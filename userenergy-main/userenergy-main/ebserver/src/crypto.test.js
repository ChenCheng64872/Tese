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
test('batched protocol: key-size names, optional keys and several batch rows',()=>{
 const b={algorithms:['rsa3072','elgamal2048','aesgcmbc128','ascon'],powers:[11,20],repetitions:1,inner_iterations:1,cooldown_ms:0,require_energy:true,target_batch_ms:3000,batches:5};
 assert.equal(validate(b),b);
 for(const bad of [{algorithms:['rsa1000']},{algorithms:['ascon128']},{batches:0},{warmup_small_power:3},{target_batch_ms:-1}])assert.throws(()=>validate({...b,...bad}));
 assert.throws(()=>validate({...b,target_batch_ms:undefined}));  // legacy protocol needs warmup_rounds
 const h='schema_version,enc_ns,dec_ns,batch_energy_j,energy_status\n';
 assert.equal(csv(h+'3,10,20,0.1,VALID\n3,11,21,0.1,VALID',true,2).length,2);
 assert.throws(()=>csv(h+'3,10,20,0.1,VALID',true,2));
});
