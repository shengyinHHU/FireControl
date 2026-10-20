import test from 'node:test'
import assert from 'node:assert/strict'
import { normalizePlaceRecord, normalizeMapPlace } from '../../src/api/fire/placeLayers.js'
import { usesDatabase, canWriteResource } from '../../src/api/fire/sourcePolicy.js'
import { isActiveFire } from '../../src/api/fire/rules.js'
test('database owns device and event resources even while other business remains demo',()=>{
 for(const kind of ['places','gateways','hosts','alarms','protocolMessages']) {
  assert.equal(usesDatabase(kind),true);assert.equal(canWriteResource(kind),false)
 }
})
test('government unit remains readable without joining the four enterprise map layers',()=>{
 const row={placeId:'102',name:'大厂街道',placeType:'government',longitude:118,latitude:32}
 assert.equal(normalizePlaceRecord(row).category,'机关单位')
 assert.equal(normalizeMapPlace(row),null)
})
test('real fire reports can be reviewed while fault and operation reports never become fire',()=>{
 const fire={alarmType:'FIRE',status:'待核验',dataSource:'database'}
 assert.equal(isActiveFire(fire),true)
 assert.equal(isActiveFire({...fire,alarmType:'INFORMATION'}),false)
 assert.equal(isActiveFire({...fire,dataSource:undefined}),false)
})
