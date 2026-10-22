import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import { isActiveFire, isDisplayAlarm, fireAlarmPoints, hostAlarmSummary } from '../../src/api/fire/rules.js'

const source = (await readFile(new URL('../../src/api/fire/deviceAccess.js', import.meta.url), 'utf8'))
 .replace("'./localAlarmHandling.js'", JSON.stringify(new URL('../../src/api/fire/localAlarmHandling.js', import.meta.url).href))
 .replace("import request from '@/utils/request'", "const request = () => { throw Error('Unexpected HTTP') }")
 .replace("import auth from '@/plugins/auth'", "const auth = {hasPermi: () => false}")
const { normalizeDeviceRow } = await import('data:text/javascript;base64,' + Buffer.from(source).toString('base64'))

test('119 preserves all incident types and external status without inventing verification workflow', () => {
 for (const type of ['1','2','3','99']) {
  const row=normalizeDeviceRow({id:'119:1',sourceCode:'119',caseTypeCode:type,status:'返队',kind:type})
  assert.equal(row.status,'返队');assert.equal(isDisplayAlarm(row),true);assert.equal(isActiveFire(row),false)
  assert.deepEqual(row.timeline,[])
 }
})
test('119 uses only its own validated map position and never borrows a linked place coordinate', () => {
 const incident={id:'119:1',sourceCode:'119',placeId:'p'}
 assert.deepEqual(fireAlarmPoints([incident],[{id:'p',lng:118,lat:32}]),[])
 assert.equal(fireAlarmPoints([{...incident,lng:118.75,lat:32.21}],[]).length,1)
 assert.equal(fireAlarmPoints([{...incident,lng:181,lat:32}],[]).length,0)
})
test('host history excludes the independent 119 source', () => {
 assert.deepEqual(hostAlarmSummary([{id:'119:1',sourceCode:'119'},{id:'device',sourceCode:'device'}]).rows.map(x=>x.id),['device'])
})
