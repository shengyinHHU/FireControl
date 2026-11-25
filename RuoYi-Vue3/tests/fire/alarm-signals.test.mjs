import test from 'node:test'
import assert from 'node:assert/strict'
import { createAlarmSignals } from '../../src/api/fire/alarmSignals.js'
import { hostAlarmSummary } from '../../src/api/fire/rules.js'

const device = id => ({ id, sourceCode: 'device', alarmType: 'FIRE', dataSource: 'database', status: '待核验' })
const incident = id => ({ id, sourceCode: '119', alarmType: 'INFORMATION', status: '返队' })

test('initial history stays silent; device fire/manual fire and every 119 case type can trigger', () => {
  const signals = createAlarmSignals()
  const history = [device('old-device'), incident('old-119')]
  assert.deepEqual(signals.update(history), { fresh: [], pendingCount: 0 })
  const fresh = [device('new-device'), incident('new-119')]
  assert.deepEqual(signals.update([...fresh, ...history]), { fresh, pendingCount: 2 })
  assert.deepEqual(signals.update([...fresh, ...history]), { fresh: [], pendingCount: 2 })
})

test('stop acknowledges only this batch; later incidents trigger, repeated/reappearing IDs do not', () => {
  const signals = createAlarmSignals()
  signals.update([])
  signals.update([device('one')])
  signals.stop()
  assert.equal(signals.update([device('one')]).pendingCount, 0)
  signals.update([])
  assert.equal(signals.update([device('one')]).pendingCount, 0)
  const next = incident('two')
  assert.deepEqual(signals.update([device('one'), next]), { fresh: [next], pendingCount: 1 })
})

test('device faults/closed fires do not trigger; resolving/removing pending alarms stops the signal', () => {
  const signals = createAlarmSignals()
  signals.update([])
  const fault = { ...device('fault'), alarmType: 'INFORMATION', status: '设备记录' }
  const closed = { ...device('closed'), status: '已办结' }
  assert.equal(signals.update([fault, closed]).pendingCount, 0)
  assert.equal(signals.update([fault, device('active'), incident('119')]).pendingCount, 2)
  assert.equal(signals.update([fault, { ...device('active'), status: '已办结' }]).pendingCount, 0)
})

test('explicit simulation entry alerts on first snapshot but cannot replay real history or handled simulations', () => {
  const simulation = { ...device('sim:one'), isSimulation: true }
  const signals = createAlarmSignals()
  assert.deepEqual(signals.update([device('history'), simulation], simulation.id), { fresh: [simulation], pendingCount: 1 })
  assert.deepEqual(signals.update([simulation], simulation.id), { fresh: [], pendingCount: 1 })
  assert.equal(signals.update([{ ...simulation, simulationHandledAt: '2026-11-25 12:00:00' }]).pendingCount, 0)
  assert.equal(hostAlarmSummary([{ ...simulation, simulationHandledAt: '2026-11-25 12:00:00' }]).total, 0)
  assert.equal(createAlarmSignals().update([device('real')], 'real').fresh.length, 0)
  assert.equal(createAlarmSignals().update([{ ...incident('sim:119'), isSimulation: true, simulationHandledAt: 'done' }], 'sim:119').fresh.length, 0)
})

test('processing a simulation never acknowledges another pending real alarm', () => {
  const signals = createAlarmSignals()
  signals.update([])
  const simulation = { ...device('sim:one'), isSimulation: true }
  assert.equal(signals.update([device('real'), simulation]).pendingCount, 2)
  assert.equal(signals.update([device('real'), { ...simulation, simulationHandledAt: 'done' }]).pendingCount, 1)
})
