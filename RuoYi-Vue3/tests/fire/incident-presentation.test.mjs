import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs'
import vm from 'node:vm'
import { parse, compileTemplate, compileScript } from '@vue/compiler-sfc'
import { transform } from 'esbuild'
import { incidentKind, incidentSummary, filterIncidents, incidentIcon } from '../../src/api/fire/incidentPresentation.js'
test('classification uses supplied codes and nature; unknown descriptions are not guessed as fires', () => {
 assert.equal(incidentKind({sourceCode:'119',caseTypeCode:'1'}),'fire')
 assert.equal(incidentKind({sourceCode:'119',caseTypeCode:'2',caseNatureName:'交通事故'}),'traffic')
 assert.equal(incidentKind({sourceCode:'119',caseTypeCode:'2'}),'rescue')
 assert.equal(incidentKind({sourceCode:'119',caseTypeCode:'3'}),'assist')
 assert.equal(incidentKind({sourceCode:'119',name:'起火'}),'unknown')
 assert.equal(new Set(['fire','traffic','rescue','assist','unknown'].map(incidentIcon)).size,5)
 assert.ok(!incidentIcon('<script>').includes('<script>'))
})
test('source switches do not mutate records; phone numbers stay out of summaries', () => {
 const rows=[{id:1,sourceCode:'119'},{id:2,source:'AI监测预警'},{id:3,source:'消控室主机'}]
 assert.deepEqual(filterIncidents(rows,['119警情']).map(r=>r.id),[1])
 assert.equal(filterIncidents(rows,[]).length,0)
 assert.equal(rows.length,3)
 assert.ok(!incidentSummary({sourceCode:'119',name:'13588830094货车冒烟'}).includes('13588830094'))
})
test('pending map component and CSS compile', async () => {
 const filename='src/components/Fire/MapPanel.vue.pending'
 const {descriptor,errors}=parse(fs.readFileSync(filename,'utf8'),{filename})
 assert.deepEqual(errors,[])
 assert.deepEqual(compileTemplate({source:descriptor.template.content,filename,id:'map'}).errors,[])
 compileScript(descriptor,{id:'map'})
 await transform(fs.readFileSync('src/api/fire/mapPresentation.css.pending','utf8'),{loader:'css'})
})
test('grid call navigates directly to the requested meeting URL', () => {
 const s=fs.readFileSync('src/components/Fire/ResourcePage.vue','utf8')
 const code=s.match(/function callGrid\(\) \{[\s\S]*?\n\}/)[0]
 const urls=[];const ctx=vm.createContext({window:{location:{assign:u=>urls.push(u)}}})
 vm.runInContext(code,ctx);ctx.callGrid()
 assert.deepEqual(urls,['https://www.jnrhtx.top/meeting/index?from=Dc'])
})
