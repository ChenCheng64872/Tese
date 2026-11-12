const {test}=require('node:test');
const assert=require('node:assert/strict');
const {validate,csv}=require('./crypto');
const config={algorithms:['aesgcm'],powers:[10],repetitions:1,inner_iterations:100,warmup_rounds:3,cooldown_ms:0,require_energy:true};
test('reject dangerous or ambiguous matrix values',()=>{
 assert.equal(validate(config),config);
 for(const bad of [{powers:[9]},{algorithms:['aesgcm;bad']},{repetitions:0},{inner_iterations:0},{powers:[10,10]}])assert.throws(()=>validate({...config,...bad}));
});
test('energy must be available for formal runs; timing-only smoke may proceed',()=>{
 const h='schema_version,enc_ns,dec_ns,batch_energy_j,energy_status\n';
 assert.equal(csv(h+'2,10,20,0.001,VALID',true).batch_energy_j,'0.001');
 assert.throws(()=>csv(h+'2,10,20,,NOT_DISCHARGING',true));
 assert.equal(csv(h+'2,10,20,,NOT_DISCHARGING',false).energy_status,'NOT_DISCHARGING');
 assert.throws(()=>csv(h+'2,0,20,0.001,VALID',true));
 assert.throws(()=>csv(h+'2,10,20,0.001,VALID\n2,10,20,0.001,VALID',true));
});
