// Execute the shipped Postman scripts in a deterministic sandbox. No external requests.
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const assert = require('node:assert/strict');
const dir = path.resolve(__dirname, '../../docs/sms-api');
const collection = JSON.parse(fs.readFileSync(path.join(dir, 'FireControl-SMS.postman_collection.json'), 'utf8'));
const initial = JSON.parse(fs.readFileSync(path.join(dir, 'FireControl-SMS.postman_environment.json'), 'utf8'));
const env = new Map(initial.values.map(x => [x.key, x.value]));
const locals = new Map();
let skipped = false, checks = 0;
const uuid = '5b93fa88-e117-4ff6-a43c-970a517f35aa';
const pm = {
  environment: {get: k => env.get(k), set: (k,v) => env.set(k,v), unset: k => env.delete(k)},
  variables: {get: k => locals.get(k), set: (k,v) => locals.set(k,v), replaceIn: s => {assert.equal(s,'{{$guid}}'); return uuid;}},
  execution: {skipRequest: () => { skipped = true; }},
};
function run(index, kind) {
  skipped = false;
  const code = collection.item[index].event.find(x => x.listen === kind).script.exec.join('\n');
  return vm.runInNewContext(code, {pm});
}
function check(pass) {assert.ok(pass); checks++;}
function blocked(index) {assert.throws(() => run(index, 'prerequest')); check(skipped);}
blocked(0); // Empty password stops login.
env.set('password', 'quoted"password\\827!');
run(0, 'prerequest');
check(JSON.parse(locals.get('tokenPayload')).password === env.get('password'));
env.set('token', 'fixture-token');
blocked(1); // Empty recipient prevents a real SMS by default.
env.set('mobiles','13800000001,13900000002');
env.set('content','中文 "引号" + & {{literal}}\n换行');
run(1, 'prerequest');
let first = JSON.parse(locals.get('smsPayload'));
check(first.requestId === uuid && env.get('requestId') === uuid);
check(first.content === env.get('content') && first.mobiles.length === 2);
run(1, 'prerequest');
check(locals.get('smsPayload') === JSON.stringify(first));
env.set('mobiles','13800000001,13800000001');blocked(1);
env.set('mobiles','02512345678');blocked(1);
env.set('mobiles','13800000001');env.set('requestId','bad');blocked(1);blocked(2);
env.set('requestId',uuid);run(2,'prerequest');check(locals.get('requestId')===uuid);
env.delete('token');blocked(2);
env.set('token','previous-token');
pm.response={code:401,json:()=>({code:401})};
// Failed pm.test records a failure and continues, matching Postman sandbox semantics.
pm.test=(name,f)=>{try {f();} catch {}};
pm.response.to={have:{status:s=>assert.equal(pm.response.code,s)}};
pm.expect=v=>({to:{eql:expected=>assert.equal(v,expected),be:{a:type=>{assert.equal(typeof v,type);return {and:{not:{empty:true}}};}},include:x=>assert.ok(v.includes(x))}});
run(0,'test');check(!env.has('token'));
pm.response={code:200,json:()=>({code:200,token:'new-token',expiresIn:1800}),to:{have:{status:s=>assert.equal(s,200)}}};
run(0,'test');check(env.get('token')==='new-token'&&env.get('expiresIn')==='1800');
pm.test=(name,f)=>f();
pm.response={code:200,json:()=>({code:200,data:{requestId:uuid,taskStatus:'submitted'}}),to:{have:{status:s=>assert.equal(s,200)}}};
run(1,'test');run(2,'test');checks+=2;
const openapi=JSON.parse(fs.readFileSync(path.join(dir,'openapi.json'),'utf8'));
check(openapi.openapi==='3.0.3'&&Object.keys(openapi.paths).length===3);
check(openapi.components.schemas.Send.additionalProperties===false);
check(collection.item.length===3&&collection.item[0].request.auth.type==='noauth');
check(initial.values.filter(x=>['password','token','mobiles','requestId'].includes(x.key)).every(x=>x.value===''));
const report={passed:checks,postmanScriptsExecutedInSandbox:true,liveSmsSent:false};
fs.writeFileSync(path.resolve(dir,'../verification/sms-api-bundle.json'),JSON.stringify(report,null,2)+'\n');
console.log(JSON.stringify(report));
