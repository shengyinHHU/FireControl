import test from 'node:test'
import assert from 'node:assert/strict'
import { format119SyncStatus } from '../../src/api/fire/incident119Status.js'
test('quick success cannot hide failed full reconciliation or advance its displayed time', () => {
 const note = format119SyncStatus({enabled:true,lastResult:'failed',lastCompletedAt:'2026-10-22 20:00:00',lastQuickResult:'success',lastQuickCompletedAt:'2026-10-22 21:00:00',nextRetryAt:'2026-10-22 21:02:00'})
 assert.match(note,/快速扫描.*成功/)
 assert.match(note,/完整核对：每600秒，失败/)
 assert.match(note,/最近完整成功：2026-10-22 20:00:00/)
 assert.match(note,/故障退避中/)
})
test('sync status distinguishes new discovery, never run, unavailable and disabled', () => {
 assert.match(format119SyncStatus(null),/暂不可用/)
 assert.match(format119SyncStatus({enabled:false}),/未启用/)
 const note=format119SyncStatus({enabled:true,lastNewIncidentAt:'2026-10-22 21:00:00'})
 assert.match(note,/最近发现新警情：2026-10-22 21:00:00/)
 assert.match(note,/尚未完成首轮/)
})
