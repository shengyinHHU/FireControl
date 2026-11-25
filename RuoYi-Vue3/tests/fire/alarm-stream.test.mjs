import test from 'node:test'
import assert from 'node:assert/strict'
import { createSseParser, startAlarmStream } from '../../src/api/fire/alarmStream.js'

test('SSE parser handles split chunks, CRLF and comments without inventing alarm payloads', () => {
 const received=[], parse=createSseParser(event=>received.push(event))
 parse(': keepalive\n\nevent: rea');parse('dy\r\ndata: ready\r\n\r');parse('\nevent: alarm-change\ndata: refresh\n\nevent: other\ndata: ignored\n\n')
 assert.deepEqual(received,['ready','alarm-change'])
})
test('SSE uses Authorization, stops on 403, and does not put JWT in URL', async () => {
 let changes=0,request
 const stop=startAlarmStream({url:'/fire/alarms/stream',getToken:()=> 'fixture-token',onChange:()=>changes++,fetchImpl:async(url,options)=>{request={url,options};return {status:403,ok:false}}})
 await new Promise(resolve=>setTimeout(resolve,10));stop()
 assert.equal(request.url,'/fire/alarms/stream');assert.equal(request.options.headers.Authorization,'Bearer fixture-token');assert.equal(changes,0)
})
test('disconnect cleanup aborts the SSE request and ignores late changes', async () => {
 let options,changes=0
 const stop=startAlarmStream({url:'/stream',getToken:()=> 'fixture',onChange:()=>changes++,fetchImpl:async(_,o)=>{options=o;return new Promise((resolve,reject)=>o.signal.addEventListener('abort',()=>reject(new Error('aborted'))))}})
 stop();await new Promise(resolve=>setTimeout(resolve,10))
 assert.equal(options.signal.aborted,true);assert.equal(changes,0)
})

test('device streams accept their own events and report authorization failure', async () => {
 const received=[],parse=createSseParser(event=>received.push(event),['ready','device-change'])
 parse('event: device-cha');parse('nge\ndata: refresh\n\nevent: alarm-change\ndata: ignored\n\n')
 assert.deepEqual(received,['device-change'])
 const states=[]
 const stop=startAlarmStream({url:'/fire/devices/stream?resource=gateways',getToken:()=> 'fixture',onChange:()=>{},onState:value=>states.push(value),fetchImpl:async()=>({status:401,ok:false})})
 await new Promise(resolve=>setTimeout(resolve,10));stop();assert.deepEqual(states,['connecting','denied'])
})
