'use strict';
// Cryptography adapter for Ebserver. Uses its existing ADB reset module; retains raw dumpsys logs.
const fs=require('node:fs'), path=require('node:path');
const {execFile}=require('node:child_process');
const {promisify}=require('node:util');
const exec=promisify(execFile);
const adbModule=require('./adbCommands');
const iv2201Probe=require('./iv2201-probe');
// Names as in app CryptoCase.kt: optional key size for aes/aesgcm/aesgcmbc (128/192/256), rsa (1024-4096), elgamal (768-4096).
const allowedRe=/^(?:(?:aes|aesgcm|aesgcmbc)(?:128|192|256)?|rsa(?:1024|2048|3072|4096)?|elgamal(?:768|1024|2048|3072|4096)?|chacha20|ascon|elephant|giftcofb|grain128|xoodyak)$/;
const allowed={has:x=>typeof x==='string'&&allowedRe.test(x)};
// Batched protocol (same as the desktop/Pi runs): one process per case, idle window, time-based warm-up,
// `batches` batches of ~target_batch_ms. It is used when target_batch_ms is set. Optional keys: [default,min,max]
const BATCHED={batches:[5,1,100],target_batch_ms:[3000,0,600000],warmup_ms:[5000,0,600000],
 warmup_small_power:[12,0,20],idle_ms:[5000,0,600000],block_cooldown_ms:[30000,0,600000],max_battery_temp_c:[40,20,60]};
function rng(seed){let a=seed>>>0;return()=>{a=(a+0x6D2B79F5)>>>0;let t=a;t=Math.imul(t^(t>>>15),t|1);t^=t+Math.imul(t^(t>>>7),t|61);return((t^(t>>>14))>>>0)/4294967296;};}
function shuffled(list,seed,power){const r=rng(seed*1000+power),a=[...list];for(let i=a.length-1;i>0;i--){const j=Math.floor(r()*(i+1));[a[i],a[j]]=[a[j],a[i]];}return a;}
function validate(c) {
 if(!Array.isArray(c.algorithms)||!c.algorithms.length||c.algorithms.some(x=>!allowed.has(x)))throw Error('Invalid algorithms');
 if(!Array.isArray(c.powers)||!c.powers.length||c.powers.some(x=>!Number.isInteger(x)||x<10||x>20))throw Error('Invalid powers');
 for(const [key,min,max] of [['repetitions',1,100],['inner_iterations',1,100000],['cooldown_ms',0,60000]])
  if(!Number.isInteger(c[key])||c[key]<min||c[key]>max)throw Error('Invalid '+key);
 if(c.target_batch_ms===undefined && (!Number.isInteger(c.warmup_rounds)||c.warmup_rounds<1||c.warmup_rounds>100))throw Error('Invalid warmup_rounds');
 for(const [key,[,min,max]] of Object.entries(BATCHED))
  if(c[key]!==undefined && (!Number.isInteger(c[key])||c[key]<min||c[key]>max))throw Error('Invalid '+key);
 if(c.warmup_small_power!==undefined && c.warmup_small_power!==0 && c.warmup_small_power<6)throw Error('Invalid warmup_small_power');
 if(c.shuffle_seed!==undefined && !Number.isInteger(c.shuffle_seed))throw Error('Invalid shuffle_seed');
 if(c.probe_interval_ms!==undefined && (!Number.isInteger(c.probe_interval_ms) || c.probe_interval_ms<100 || c.probe_interval_ms>1000))throw Error('Invalid probe interval');
 if(typeof c.require_energy!=='boolean')throw Error('require_energy must be boolean');
 if(c.energy_probe!==undefined && (c.energy_probe!=='iv2201' || c.require_energy))throw Error('IV2201 probe is provisional and requires require_energy=false');
 if(new Set(c.algorithms).size!==c.algorithms.length||new Set(c.powers).size!==c.powers.length)throw Error('Duplicate configurations');
 return c;
}
function csv(text, requireEnergy, batches=1) {
 const lines=text.trim().split(/\r?\n/), header=lines[0].split(',');
 if(lines.length!==batches+1)throw Error(batches===1?'Expected exactly one batch row':'Expected exactly '+batches+' batch rows');
 const rows=lines.slice(1).map(line=>{
  const values=line.split(',');
  if(values.length!==header.length)throw Error('CSV width mismatch');
  const row=Object.fromEntries(header.map((k,i)=>[k,values[i]]));
  if(!['2','3'].includes(row.schema_version)||!(Number(row.enc_ns)>0)||!(Number(row.dec_ns)>0))throw Error('Invalid timings/schema');
  if(requireEnergy && (!['VALID','VALID_DEVICE_ESTIMATE'].includes(row.energy_status)||row.batch_energy_j===''||!Number.isFinite(Number(row.batch_energy_j))||Number(row.batch_energy_j)<0))throw Error('Energy unavailable: '+row.energy_status+'; lengthen batch or verify discharge/sensors');
  return row;
 });
 return batches===1 ? rows[0] : rows;
}
async function main(args=process.argv.slice(2)) {
 const configPath=path.resolve(args[0]||path.join(__dirname,'../crypto.config.json'));
 const config=validate(JSON.parse(fs.readFileSync(configPath,'utf8')));
 const queuePath=args.find(arg=>arg.startsWith('--case-list='))?.slice(12);
 const queue=queuePath ? JSON.parse(fs.readFileSync(queuePath,'utf8')) : null;
 const caseKey=(algorithm,power,repeat)=>[algorithm,power,repeat].join(',');
 const selectedCases=queue && new Set(queue.map(r=>caseKey(r.algorithm,r.power,r.repetition)));
 if(queue && (selectedCases.size!==queue.length || queue.some(r=>!config.algorithms.includes(r.algorithm) ||
    !config.powers.includes(r.power) || !Number.isInteger(r.repetition) || r.repetition<1 || r.repetition>config.repetitions)))
  throw Error('Invalid or duplicate case-list entries');
 const batched=config.target_batch_ms!==undefined;
 const opt=key=>config[key]!==undefined?config[key]:BATCHED[key][0];
 const count=queue ? queue.length : config.algorithms.length*config.powers.length*config.repetitions;
 if(args.includes('--dry-run')) {console.log(JSON.stringify({configPath,batches_per_device:count,config},null,2));return;}
 const runAdb=async(device,...a)=>(await exec('adb',device?['-s',device,...a]:a,{timeout:600000,maxBuffer:32*1024*1024})).stdout;
 const devices=(await runAdb(null,'devices')).split(/\r?\n/).filter(l=>/\tdevice$/.test(l.trim())).map(l=>l.split('\t')[0]);
 const selected=args.find(x=>x.startsWith('--device='))?.slice(9);
 if(!selected)throw Error('Select one device explicitly with --device=SERIAL (avoids USB/Wi-Fi duplicate devices)');
 if(!devices.includes(selected))throw Error('Selected device not connected/authorized: '+selected);
 const stamp=new Date().toISOString().replace(/[^0-9]/g,'');
 const resultsRoot=path.resolve(__dirname,'../../../../results/ebserver');
 const resume=args.find(arg=>arg.startsWith('--resume='))?.slice(9);
 if(resume && !/^[A-Za-z0-9_-]+$/.test(resume))throw Error('Invalid resume directory name');
 const out=path.join(resultsRoot,resume||stamp+'_'+selected.replace(/[^a-zA-Z0-9_-]/g,'-'));
 const completed=new Set();
 const skipElephant=args.includes('--skip-elephant');
 if(resume){
  const prior=JSON.parse(fs.readFileSync(path.join(out,'manifest.json'),'utf8'));
  const extending=args.includes('--extend-sizes');
  const sameConfig=JSON.stringify(prior.config)===JSON.stringify(config);
  const oldPowers=prior.config.powers;
  const appendedSizes=extending && config.powers.length>oldPowers.length &&
    oldPowers.every((power,index)=>config.powers[index]===power) &&
    JSON.stringify({...config,powers:oldPowers})===JSON.stringify(prior.config);
  if(prior.device!==selected || (!sameConfig && !appendedSizes))throw Error('Resume configuration or device mismatch');
  if(appendedSizes){
   const extension={at_utc:new Date().toISOString(),previous_config:prior.config,previous_config_path:prior.configPath};
   const updated={...prior,config,configPath,extensions:[...(prior.extensions||[]),extension]};
   const manifest=path.join(out,'manifest.json');
   fs.writeFileSync(manifest+'.tmp',JSON.stringify(updated,null,2));
   fs.renameSync(manifest+'.tmp',manifest);
  }
  for(const line of fs.readFileSync(path.join(out,'execution.csv'),'utf8').trim().split(/\r?\n/).slice(1)){
   const [,algorithm,power,repetition,status]=line.split(',');
   if(status==='SUCCESS'||status==='SKIPPED')completed.add([algorithm,power,repetition].join(','));
  }
 }
 fs.mkdirSync(out,{recursive:true});
 const metadata={started_utc:new Date().toISOString(),device:selected,config,configPath,
  model:(await runAdb(selected,'shell','getprop','ro.product.model')).trim(),
  android:(await runAdb(selected,'shell','getprop','ro.build.version.release')).trim(),
  provider_note:'AES-CBC and ChaCha20 use BC lightweight API; AES-GCM uses Android default JCE provider. No provider-equivalence claim.',
  protocol:batched?'batched (CryptoCase, one process per case)':'legacy (one batch per process)'};
 if(config.energy_probe==='iv2201' && metadata.model!=='IV2201')throw Error('IV2201 probe is restricted to model IV2201');
 if(!resume) fs.writeFileSync(path.join(out,'manifest.json'),JSON.stringify(metadata,null,2));
 const statusFile=path.join(out,'execution.csv');
 if(!resume) fs.writeFileSync(statusFile,'run_id,algorithm,power,repetition,status\n');
 console.log('Results: '+out);
 let index=completed.size;
 const order=power=>config.shuffle_seed===undefined?config.algorithms:shuffled(config.algorithms,config.shuffle_seed,power);
 const batteryTemp=text=>Number(text.match(/temperature:\s*(\d+)/)?.[1])/10;
 let lastPower=null;
 for(let repeat=1;repeat<=config.repetitions;repeat++) for(const power of config.powers) for(const algorithm of order(power)) {
  if(batched && lastPower!==null && power!==lastPower && opt('block_cooldown_ms')>0){
   console.log('Block cooldown '+opt('block_cooldown_ms')+' ms');
   await new Promise(r=>setTimeout(r,opt('block_cooldown_ms')));
  }
  lastPower=power;
  if(selectedCases && !selectedCases.has(caseKey(algorithm,power,repeat)))continue;
  if(completed.has([algorithm,power,repeat].join(',')))continue;
  const id=stamp+'_'+String(++index).padStart(5,'0');
  if(skipElephant && algorithm==='elephant'){
   fs.appendFileSync(statusFile,[id,algorithm,power,repeat,'SKIPPED'].join(',')+'\n');
   console.log(index+'/'+count+' elephant 2^'+power+' SKIPPED');
   continue;
  }
  const dir=path.join(out,id);fs.mkdirSync(dir);
  try {
   const battery=await runAdb(selected,'shell','dumpsys','battery');
   fs.writeFileSync(path.join(dir,'battery-before.txt'),battery);
   if(config.require_energy && (!/status:\s*3\b/.test(battery)||/powered:\s*true/.test(battery)))throw Error('Formal energy runs require actual unplugged/discharging state; use Wi-Fi ADB. No battery-state spoofing.');
   const level=Number(battery.match(/level:\s*(\d+)/)?.[1]);
   if(!Number.isFinite(level)||level<25)throw Error('Battery unavailable or below 25%; recharge before continuing');
   await new Promise(r=>setTimeout(r,config.cooldown_ms));
   if(batched){ // thermal gate on the battery temperature (at most 10 min)
    const limit=opt('max_battery_temp_c'), t0=Date.now();
    let temp=batteryTemp(battery);
    while(temp>limit && Date.now()-t0<600000){
     await new Promise(r=>setTimeout(r,10000));
     temp=batteryTemp(await runAdb(selected,'shell','dumpsys','battery'));
    }
    if(Date.now()-t0>=1000)console.log('  waited '+Math.round((Date.now()-t0)/1000)+' s for battery <= '+limit+' C (now '+temp+' C)');
   }
   await adbModule.cleanBatteryStatus(selected);
   const params=batched
    ? {algorithm,contentsize:power,rounds:opt('batches'),inner_iterations:config.inner_iterations,target_ms:opt('target_batch_ms'),
       warmup_ms:opt('warmup_ms'),warmup_small_power:opt('warmup_small_power'),idle_ms:opt('idle_ms'),run_id:id}
    : {algorithm,contentsize:power,rounds:1,inner_iterations:config.inner_iterations,warmup_rounds:config.warmup_rounds,run_id:id};
   if(config.energy_probe==='iv2201')params.external_energy_probe='true';
   const opts=Object.entries(params).flatMap(([k,v])=>['-e',k,String(v)]);
   const execute=()=>runAdb(selected,'shell','am','instrument','-w',...opts,'-e','class','com.example.energy.CryptoBenchmarkInstrumentedTest#runbenchmark','com.example.energy.test/androidx.test.runner.AndroidJUnitRunner');
   let output;
   const foregroundMode=args.includes('--foreground-retry') && config.energy_probe==='iv2201';
   if(foregroundMode){
    const uid=(await runAdb(selected,'shell','cmd','package','list','packages','-U','com.example.energy'))
      .match(/^package:com\.example\.energy uid:(\d+)\s*$/m)?.[1];
    if(!uid)throw Error('Cannot resolve benchmark app UID');
    // The first 1 MiB GIFT-COFB case already froze in the previous background run.
    const maxAttempts=queue ? 3 : algorithm==='giftcofb' && power===20 && repeat===1 ? 1 : 2;
    for(let attempt=1;attempt<=maxAttempts;attempt++){
     const controller=new AbortController();
     let frozen=false;
     let checking=false;
     const foreground=setTimeout(()=>{
      runAdb(selected,'shell','am','start','-n','com.example.energy/.MainActivity')
       .then(text=>fs.writeFileSync(path.join(dir,'foreground-'+attempt+'.txt'),text))
       .catch(error=>fs.writeFileSync(path.join(dir,'foreground-'+attempt+'.txt'),error.message));
     },2500);
     const watcher=setInterval(async()=>{
      if(checking || controller.signal.aborted)return;
      checking=true;
      try{
       const state=await runAdb(selected,'shell','cat','/sys/fs/cgroup/uid_'+uid+'/cgroup.events');
       if(/^frozen 1\s*$/m.test(state)){
        frozen=true;
        fs.writeFileSync(path.join(dir,'freeze-'+attempt+'.txt'),state);
        controller.abort();
       }
      }catch{}finally{checking=false;}
     },5000);
     const retry=async()=>(await exec('adb',['-s',selected,'shell','am','instrument','-w',...opts,
       '-e','class','com.example.energy.CryptoBenchmarkInstrumentedTest#runbenchmark',
       'com.example.energy.test/androidx.test.runner.AndroidJUnitRunner'],
       {signal:controller.signal,timeout:600000,maxBuffer:32*1024*1024})).stdout;
     try{
      output=await iv2201Probe.capture(runAdb,selected,dir,battery,retry,config.probe_interval_ms);
      if(queue && !fs.readFileSync(path.join(dir,'battery-probe.csv'),'utf8').includes('\nPROVISIONAL_UNITS,')){
       fs.writeFileSync(path.join(dir,'instrumentation-attempt-'+attempt+'.txt'),output);
       output=undefined;
       const insufficient=Error('Insufficient battery probe coverage');
       insufficient.insufficient=true;
       throw insufficient;
      }
      break;
     }catch(error){
      const probeStartup=queue && /^Continuous battery probe (did not start|exited before its first sample)$/.test(error.message);
      if(!frozen && !error.killed && !error.insufficient && !probeStartup)throw error;
      const reason=probeStartup?error.message:error.insufficient?'Insufficient battery probe coverage':frozen?'OS froze the benchmark app':'Instrumentation timed out';
      const failed=path.join(dir,'failed-attempt-'+attempt);
      fs.mkdirSync(failed);
      for(const file of ['battery-probe.csv','battery-probe-raw.json','battery-after.txt']){
       const source=path.join(dir,file);
       if(fs.existsSync(source))fs.renameSync(source,path.join(failed,file));
      }
      fs.writeFileSync(path.join(failed,'reason.txt'),reason);
      await runAdb(selected,'shell','am','force-stop','com.example.energy');
      console.log(index+'/'+count+' '+algorithm+' 2^'+power+' attempt '+attempt+': '+reason);
     }finally{
      clearTimeout(foreground);
      clearInterval(watcher);
     }
    }
    if(output===undefined){
     if(args.includes('--keep-all'))throw Error('Batch failed after foreground retry; preserved for resume: '+algorithm+' 2^'+power);
     fs.appendFileSync(statusFile,[id,algorithm,power,repeat,'SKIPPED'].join(',')+'\n');
     console.log(index+'/'+count+' '+algorithm+' 2^'+power+' SKIPPED after foreground retry');
     continue;
    }
   }else{
    output=config.energy_probe==='iv2201' ? await iv2201Probe.capture(runAdb,selected,dir,battery,execute,config.probe_interval_ms) : await execute();
   }
   fs.writeFileSync(path.join(dir,'instrumentation.txt'),output);
   if(!/OK \(1 test\)/.test(output)||/FAILURES!!!|INSTRUMENTATION_FAILED|Process crashed/.test(output))throw Error('Instrumentation did not pass one test');
   for(const [service,extra] of [['batterystats',[]],['meminfo',['com.example.energy','-d']],['procstats',['--hours','1']]])
    fs.writeFileSync(path.join(dir,service+'.txt'),await runAdb(selected,'shell','dumpsys',service,...extra));
   const remote='/sdcard/Android/data/com.example.energy/files/automated/'+id+'/'+algorithm+'_'+id+'.csv';
   const local=path.join(dir,'benchmark.csv');
   await runAdb(selected,'pull',remote,local);
   if(batched) await runAdb(selected,'pull',remote+'.json',local+'.json');
   const rows=[].concat(csv(fs.readFileSync(local,'utf8'),config.require_energy,batched?opt('batches'):1));
   for(const row of rows)
    if(Number(row.size_bytes)!==2**power||(!batched&&Number(row.inner_iterations)!==config.inner_iterations)||row.algorithm!==algorithm)throw Error('Result/config mismatch');
   fs.appendFileSync(statusFile,[id,algorithm,power,repeat,'SUCCESS'].join(',')+'\n');
   console.log(index+'/'+count+' '+algorithm+' 2^'+power+' '+rows.map(r=>r.energy_status).join(' '));
  } catch(e) {
   fs.writeFileSync(path.join(dir,'failure.txt'),String(e.stack||e));
   fs.appendFileSync(statusFile,[id,algorithm,power,repeat,'FAILED'].join(',')+'\n');
   throw e;
  }
 }
 console.log('Complete: '+out);
}
module.exports={validate,csv,main};
if(require.main===module) main().catch(e=>{console.error(e.message);process.exitCode=1;});
