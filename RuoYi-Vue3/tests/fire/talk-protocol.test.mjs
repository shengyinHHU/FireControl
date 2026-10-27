import test from 'node:test'
import assert from 'node:assert/strict'
import { readTalkReply } from '../../src/api/fire/talkProtocol.js'
test('real gateway 10001 requires explicit success before audio can start',()=>{
 assert.equal(readTalkReply('{"method":"10001","code":"0","msg":"success"}','device').ok,true)
 assert.equal(readTalkReply('{"method":10001,"code":7,"msg":"denied"}','device').ok,false)
 assert.equal(readTalkReply('{"method":"10001"}','device').ok,false)
})
test('10008 status is scoped to the requested device and does not prove speaker output',()=>{
 const raw=JSON.stringify({method:'10008',data:{talkStatus:[{deviceId:'other',status:'0',errorMsg:'success'},{deviceId:'device',status:'1',errorMsg:'busy'}]}})
 assert.deepEqual(readTalkReply(raw,'device'),{kind:'device',ok:false,message:'busy'})
 assert.equal(readTalkReply(raw,'absent'),null)
})
test('unknown/malformed replies are ignored and sensitive response text is masked',()=>{
 assert.equal(readTalkReply('not json','device'),null)
 assert.equal(readTalkReply('{"method":"new-event"}','device'),null)
 assert.equal(readTalkReply('x'.repeat(65537),'device'),null)
 const result=readTalkReply(JSON.stringify({method:'10001',code:1,msg:'failed wss://host/path?sign=secret token=private'}),'device')
 assert.ok(!result.message.includes('secret'));assert.ok(!result.message.includes('private'))
})
