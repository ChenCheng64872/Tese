'use strict';

const fs = require('node:fs');
const path = require('node:path');
const { spawn } = require('node:child_process');
const { performance } = require('node:perf_hooks');

function unplugged(battery) {
  return ['AC', 'USB', 'Wireless'].every(source =>
    new RegExp('^\\s*' + source + ' powered: false\\s*$', 'm').test(battery));
}

function continuousSamples(device, intervalMs, samples) {
  const delay=(intervalMs/1000).toFixed(3);
  const command='while true; do cat /proc/uptime /sys/class/power_supply/battery/uevent || exit; printf "\\n__PROBE_END__\\n" || exit; sleep '+delay+'; done';
  const child=spawn('adb',['-s',device,'shell',command],{windowsHide:true,stdio:['ignore','pipe','pipe']});
  child.stderr.resume();
  let buffer='';
  let origin;
  let ready;
  let fail;
  const first=new Promise((resolve,reject)=>{ready=resolve;fail=reject;});
  const timeout=setTimeout(()=>fail(Error('Continuous battery probe did not start')),10000);
  child.stdout.setEncoding('utf8');
  child.stdout.on('data',chunk=>{
    buffer+=chunk;
    const parts=buffer.split('__PROBE_END__');
    buffer=parts.pop();
    for(const part of parts){
      const raw=part.trim();
      const uptime=Number(raw.match(/^([\d.]+)\s+[\d.]+\s*$/m)?.[1]);
      if(!Number.isFinite(uptime))continue;
      if(origin===undefined)origin=uptime;
      const voltage=Number(raw.match(/^POWER_SUPPLY_VOLTAGE_NOW=(\d+)\s*$/m)?.[1]);
      const current=Number(raw.match(/^POWER_SUPPLY_CURRENT_NOW=(-?\d+)\s*$/m)?.[1]);
      const status=raw.match(/^POWER_SUPPLY_STATUS=([^\r\n]+)/m)?.[1];
      samples.push({elapsed:(uptime-origin)*1000,voltage,current,status,raw});
      ready();
    }
  });
  child.on('error',fail);
  child.on('exit',()=>fail(Error('Continuous battery probe exited before its first sample')));
  return {first,stop(){clearTimeout(timeout);child.kill();}};
}

async function capture(runAdb, device, dir, batteryBefore, execute, intervalMs = 500) {
  if (!unplugged(batteryBefore)) throw Error('IV2201 probe requires all power sources disconnected');
  const samples = [];
  let done = false;
  let error;
  const start = performance.now();
  const continuous=intervalMs<500 ? continuousSamples(device,intervalMs,samples) : null;
  const sampling = continuous ? Promise.resolve() : (async () => {
    while (!done) {
      try {
        const raw = await runAdb(device, 'shell', 'cat', '/sys/class/power_supply/battery/uevent');
        const elapsed = performance.now() - start;
        const voltage = Number(raw.match(/^POWER_SUPPLY_VOLTAGE_NOW=(\d+)\s*$/m)?.[1]);
        const current = Number(raw.match(/^POWER_SUPPLY_CURRENT_NOW=(-?\d+)\s*$/m)?.[1]);
        const status = raw.match(/^POWER_SUPPLY_STATUS=([^\r\n]+)/m)?.[1];
        samples.push({ elapsed, voltage, current, status, raw });
      } catch (e) {
        error = String(e.message);
        break;
      }
      await new Promise(resolve => setTimeout(resolve, intervalMs));
    }
  })();
  let result;
  try {
    if(continuous)await continuous.first;
    result = await execute();
  } finally {
    done = true;
    if(continuous)continuous.stop();
    await sampling;
    const end = performance.now() - start;
    const after = await runAdb(device, 'shell', 'dumpsys', 'battery').catch(() => '');
    fs.writeFileSync(path.join(dir, 'battery-after.txt'), after);
    fs.writeFileSync(path.join(dir, 'battery-probe-raw.json'), JSON.stringify(samples, null, 2));
    const usable = unplugged(after) && !error && samples.length >= 4 && samples.every(s =>
      ['Not charging', 'Discharging'].includes(s.status) &&
      s.voltage >= 2500 && s.voltage <= 5000 && s.current > 0 && s.current <= 10000);
    const window = samples.length > 1 ? (samples.at(-1).elapsed - samples[0].elapsed) / 1000 : 0;
    const estimate = usable ? samples.slice(1).reduce((joules, sample, index) => {
      const previous = samples[index];
      const averagePower = (previous.voltage * previous.current + sample.voltage * sample.current) / 2e6;
      return joules + averagePower * (sample.elapsed - previous.elapsed) / 1000;
    }, 0) : null;
    fs.writeFileSync(path.join(dir, 'battery-probe.csv'), [
      'status,scope,samples,sampled_window_s,command_and_sampling_s,provisional_energy_j',
      [usable ? 'PROVISIONAL_UNITS' : 'UNUSABLE', 'ADB_INSTRUMENTATION_WINDOW', samples.length,
        window.toFixed(3), (end / 1000).toFixed(3), estimate?.toFixed(6) ?? ''].join(',')
    ].join('\n') + '\n');
  }
  return result;
}

module.exports = { capture };