import test from 'node:test'
import assert from 'node:assert/strict'
import {
  createLatestRequest,
  validateFields,
  validateDutyInput,
  hostAlarmSummary,
  isActiveFire,
  fireAlarmPoints,
} from '../../src/api/fire/rules.js'
test('latest request ignores slow stale response and canceled response', async () => {
  const gate = createLatestRequest()
  let resolveA
  const a = new Promise((r) => (resolveA = r))
  let state = ''
  const first = gate.run(
    () => a,
    (v) => (state = v),
  )
  await gate.run(
    () => Promise.resolve('B'),
    (v) => (state = v),
  )
  resolveA('A')
  await first
  assert.equal(state, 'B')
  let resolveC
  const c = new Promise((r) => (resolveC = r))
  const pending = gate.run(
    () => c,
    (v) => (state = v),
  )
  gate.cancel()
  resolveC('C')
  await pending
  assert.equal(state, 'B')
})
test('frontend validates required fields and real-mode duty inputs', () => {
  assert.throws(() =>
    validateFields([{ key: 'name', label: '名称' }], { name: '  ' }),
  )
  assert.throws(() =>
    validateFields([{ key: 'lng', label: '经度' }], { lng: 181 }),
  )
  assert.throws(() =>
    validateDutyInput({
      name: '张三',
      phone: '1',
      date: '2026-10-18',
      start: '08:00',
      end: '20:00',
    }),
  )
  assert.throws(() =>
    validateDutyInput({
      name: '张三',
      phone: '13800009999',
      date: '2026-02-31',
      start: '08:00',
      end: '20:00',
    }),
  )
})
test('host panel includes all alarms regardless of source', () => {
  const r = hostAlarmSummary([
    { source: '119警情', status: '待处理' },
    { source: '消控室主机', status: '处理中' },
    { source: 'AI监测预警', status: '已办结' },
    { source: '消控室主机', status: '已办结' },
  ])
  assert.equal(r.total, 4)
  assert.equal(r.closed, 2)
  assert.equal(r.pending, 2)
})

test('active fire classification excludes faults and closed fires independent of source', () => {
  assert.equal(isActiveFire({ alarmType: 'FIRE', source: '消控室主机', status: '待处理' }), true)
  assert.equal(isActiveFire({ alarmType: 'FIRE', status: '处理中' }), true)
  assert.equal(isActiveFire({ alarmType: 'FIRE', status: '已办结' }), false)
  assert.equal(isActiveFire({ alarmType: 'FAULT', source: '119警情', status: '待处理' }), false)
  assert.equal(isActiveFire({ name: '火警', status: '待处理' }), false)
})
test('fire coordinates prefer alarm then same-site device then place and reject missing positions', () => {
  const a = { id:'a', placeId:'p', deviceId:'d', alarmType:'FIRE', status:'待处理' }
  const places = [{id:'p',name:'测试场所',lng:118,lat:32}]
  const devices = [{id:'d',placeId:'p',lng:119,lat:33}]
  assert.equal(fireAlarmPoints([a],places,devices)[0].lng,119)
  assert.equal(fireAlarmPoints([{...a,lng:120,lat:34}],places,devices)[0].lng,120)
  assert.equal(fireAlarmPoints([a],places,[])[0].lng,118)
  assert.equal(fireAlarmPoints([a],[],[]).length,0)
  assert.equal(fireAlarmPoints([{...a,status:'已办结'}],places,devices).length,0)
})
