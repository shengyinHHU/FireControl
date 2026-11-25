import test from 'node:test'
import assert from 'node:assert/strict'
import { createTableRefresh, reconcileTableRows } from '../../src/api/fire/tableRefresh.js'
const sleep = ms => new Promise(resolve => setTimeout(resolve, ms))

test('background bursts serialize requests and foreground queries discard stale responses', async () => {
 const releases=[], applied=[];let query='old',active=0,maxActive=0
 const queue=createTableRefresh(async(silent,current)=>{
  const snapshot=query;active++;maxActive=Math.max(maxActive,active)
  await new Promise(resolve=>releases.push(resolve));active--
  if(current())applied.push([snapshot,silent])
 },{delay:5})
 try{
  queue.refresh();await sleep(1)
  for(let i=0;i<30;i++)queue.refresh(true)
  query='new';queue.refresh();releases.shift()();await sleep(15)
  assert.equal(releases.length,1);releases.shift()();await sleep(10)
  assert.deepEqual(applied,[['new',false]]);assert.equal(maxActive,1)
 }finally{queue.stop()}
})

test('paused background notifications resume once; stop ignores a late response', async () => {
 let paused=true,calls=0,release,applied=0
 const queue=createTableRefresh(async(_,current)=>{calls++;await new Promise(resolve=>release=resolve);if(current())applied++},{paused:()=>paused,delay:5})
 queue.refresh(true);queue.refresh(true);await sleep(15);assert.equal(calls,0)
 paused=false;queue.resume();assert.equal(calls,1);queue.stop();release();await sleep(5);assert.equal(applied,0)
})

test('row reconciliation preserves identities and applies changed, removed and new fields',()=>{
 const original=[{id:'1',status:'离线',oldField:'remove'},{id:'2',status:'在线'}]
 const same=reconcileTableRows(original,structuredClone(original));assert.equal(same,original)
 const next=reconcileTableRows(original,[{id:'1',status:'在线'},{id:'2',status:'在线'},{id:'3',status:'离线'}])
 assert.equal(next[0],original[0]);assert.equal(next[1],original[1]);assert.equal(next[0].status,'在线');assert.equal('oldField' in next[0],false)
 assert.deepEqual(reconcileTableRows(next,[{id:'3',status:'离线'}]).map(row=>row.id),['3'])
})
