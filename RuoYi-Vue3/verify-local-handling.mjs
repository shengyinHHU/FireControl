import assert from 'node:assert/strict'
import fs from 'node:fs'
import { computed } from 'vue'
import { parse, compileScript, compileTemplate } from '@vue/compiler-sfc'
const store = new Map()
globalThis.window = {localStorage:{getItem:k=>store.get(k)??null,setItem:(k,v)=>store.set(k,v)},addEventListener(){}}
const local = await import('./src/api/fire/localAlarmHandling.js')
const a={id:'test',sourceCode:'119'}, b={id:'test',sourceCode:'device'}
const visible=computed(()=>local.visibleLocalAlarms([a,b]))
assert.equal(visible.value.length,2)
assert.throws(()=>local.handleAlarmLocally(a,'   '))
local.handleAlarmLocally(a,' 已核查处理 ')
assert.deepEqual(visible.value,[b])
assert.equal(JSON.parse(store.get(local.HANDLED_ALARMS_KEY))['119:test'].note,'已核查处理')
const reloaded=await import('./src/api/fire/localAlarmHandling.js?reload=1')
assert.equal(reloaded.isLocallyHandled(a),true)
window.localStorage.setItem=()=>{throw Error('quota')}
assert.throws(()=>local.handleAlarmLocally(b,'已处理'),/无法保存/)
assert.equal(local.isLocallyHandled(b),false)
const config=await import('./src/api/fire/dashboardDisplay.js')
assert.equal(config.USE_FIXED_PLACE_COUNTS,true)
assert.deepEqual(config.FIXED_PLACE_COUNTS.map(x=>x.count),[223,81,12,9])
for(const file of ['src/components/Fire/AlarmDetail.vue','src/components/Fire/DeviceAccessView.vue','src/views/fire/dashboard/index.vue']){
 const source=fs.readFileSync(file+'.local-handle.pending','utf8')
 const {descriptor,errors}=parse(source,{filename:file})
 assert.deepEqual(errors,[])
 compileScript(descriptor,{id:file})
 const compiled=compileTemplate({source:descriptor.template.content,filename:file,id:file})
 assert.deepEqual(compiled.errors,[])
 console.log('Vue compile passed:',file)
}
console.log('PASS: validation, immediate reactive hide, source isolation, notes persistence, reload, storage failure, fixed counts')
