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

test('unfiltered scope and receiving time stay distinct from the original alarm time', () => {
 const note=format119SyncStatus({enabled:true,dachangOnly:false,total:2265,insideCount:5,latestAlarmTime:'2026-10-21 13:14:16',latestReceivedAt:'2026-10-26 10:00:00'})
 assert.match(note,/展示接口全部范围 2265 条（不按大厂街道过滤）/)
 assert.match(note,/最新来警时间：2026-10-21 13:14:16/)
 assert.match(note,/最近接收时间：2026-10-26 10:00:00/)
 assert.doesNotMatch(note,/大厂街道内 5 条/)
})
