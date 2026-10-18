import test from 'node:test'
import assert from 'node:assert/strict'
import { createRepository } from '../../src/mock/fire/repository.js'
const storage = () => {
  let value
  return {
    getItem: () => value,
    setItem: (_, v) => {
      value = v
    },
  }
}
const make = (s = storage()) =>
  createRepository({ storage: s, now: () => new Date('2026-10-18T04:00:00Z') })
test('pagination, filtering, persistence and rollback on quota failure', async () => {
  const s = storage(),
    r = make(s)
  const a = await r.list('places', { pageNum: 1, pageSize: 2 }),
    b = await r.list('places', { pageNum: 2, pageSize: 2 })
  assert.equal(a.rows.length, 2)
  assert.ok(!a.rows.some((x) => b.rows.some((y) => x.id === y.id)))
  const filtered = await r.list('places', {
    category: '重点企业',
    keyword: '扬子',
  })
  assert.ok(
    filtered.rows.every(
      (x) => x.category === '重点企业' && x.name.includes('扬子'),
    ),
  )
  const row = await r.create('places', {
    name: '测试企业',
    category: '重点企业',
    gridId: 'g1',
    address: '测试地址',
  })
  assert.equal((await make(s).get('places', row.data.id)).data.name, '测试企业')
  const broken = make({ getItem: () => '{', setItem: () => {} })
  assert.ok((await broken.list('places', {})).total > 0)
  const quota = make({
    getItem: () => null,
    setItem: () => {
      throw Error('quota')
    },
  })
  const before = (await quota.list('places', {})).total
  await assert.rejects(
    quota.create('places', {
      name: '不能保存',
      category: '重点企业',
      gridId: 'g1',
    }),
    /保存失败/,
  )
  assert.equal((await quota.list('places', {})).total, before)
})
test('validation and referenced entities cannot be removed', async () => {
  const r = make()
  await assert.rejects(r.create('places', { name: '', category: '重点企业' }))
  await assert.rejects(r.remove('places', 'p1'), /关联/)
  await assert.rejects(
    r.create('materials', { name: '水带', placeId: 'p1', quantity: -1 }),
  )
  await assert.rejects(
    r.create('tasks', { name: '检查', personnelId: 'missing', placeId: 'p1' }),
  )
})
test('grid polygon and coordinates validate; filtering uses gridId', async () => {
  const r = make()
  await assert.rejects(
    r.update('grids', 'g1', {
      polygon: [
        [118, 32],
        [118.1, 32.1],
      ],
    }),
  )
  await assert.rejects(r.update('devices', 'd1', { lng: 999, lat: 32 }))
  const rows = (await r.list('places', { gridId: 'g1' })).rows
  assert.ok(rows.length && rows.every((x) => x.gridId === 'g1'))
})
test('camera binding checks site and offline playback fails', async () => {
  const r = make()
  await assert.rejects(
    r.action('devices', 'd1', 'bind-camera', { cameraId: 'c3' }),
    /场所/,
  )
  await r.action('devices', 'd1', 'bind-camera', { cameraId: 'c1' })
  assert.equal((await r.get('devices', 'd1')).data.cameraId, 'c1')
  await assert.rejects(r.action('cameras', 'c4', 'preview', {}), /离线/)
  await assert.rejects(
    r.create('inspections', { placeId: 'p1', cameraId: 'c1', name: '王强' }),
  )
})
test('alarm transitions, timeline, dashboard and notification dedup', async () => {
  const r = make()
  const before = (await r.dashboard()).data.pending
  await r.action('alarms', 'a1', 'transition', {
    action: 'accept',
    note: '接单',
  })
  await assert.rejects(
    r.action('alarms', 'a1', 'transition', { action: 'accept', note: '重复' }),
  )
  await assert.rejects(
    r.action('alarms', 'a1', 'transition', { action: 'close', note: '' }),
  )
  await r.action('alarms', 'a1', 'transition', {
    action: 'close',
    note: '现场核查完成',
  })
  const a = (await r.get('alarms', 'a1')).data
  assert.equal(a.status, '已办结')
  assert.equal(a.timeline.length, 3)
  assert.equal((await r.dashboard()).data.pending, before - 1)
  await assert.rejects(
    r.action('alarms', 'a1', 'transition', {
      action: 'accept',
      note: '重新接单',
    }),
  )
})
test('duty validates, handles overnight, deduplicates and rejects expired shares', async () => {
  const r = make()
  await assert.rejects(r.create('duty', { name: '王强', phone: '1' }))
  const payload = {
    name: '测试值班员',
    phone: '13800009999',
    placeId: 'p1',
    date: '2026-10-18',
    start: '08:00',
    end: '20:00',
    submissionId: 'once',
  }
  const a = await r.create('duty', payload),
    b = await r.create('duty', payload)
  assert.equal(a.data.id, b.data.id)
  assert.ok(
    (await r.get('places', 'p1')).data.onDuty.some(
      (x) => x.phone === payload.phone,
    ),
  )
  const overnight = await r.create('duty', {
    ...payload,
    submissionId: 'night',
    date: '2026-10-17',
    start: '20:00',
    end: '08:00',
  })
  assert.ok(new Date(overnight.data.endsAt) > new Date(overnight.data.startsAt))
  await assert.rejects(r.action('shares', 'missing', 'resolve', {}), /失效/)
})
test('separate tabs observe saved duty and do not overwrite each other', async () => {
  const s = storage(),
    a = make(s),
    b = make(s)
  await b.create('duty', {
    name: '跨页值班',
    phone: '13800008888',
    placeId: 'p1',
    date: '2026-10-18',
    start: '08:00',
    end: '20:00',
    submissionId: 'cross-tab',
  })
  assert.ok(
    (await a.get('places', 'p1')).data.onDuty.some(
      (p) => p.name === '跨页值班',
    ),
  )
  await a.create('materials', { name: '新水带', placeId: 'p1', quantity: 3 })
  assert.ok((await b.list('materials', { keyword: '新水带' })).total === 1)
})
test('invalid calendar day and overlapping duty are rejected', async () => {
  const r = make()
  await assert.rejects(
    r.create('duty', {
      name: '值班员',
      phone: '13800009999',
      placeId: 'p1',
      date: '2026-02-31',
      start: '08:00',
      end: '20:00',
    }),
    /日期/,
  )
  const payload = {
    name: '值班员',
    phone: '13800009999',
    placeId: 'p1',
    date: '2026-10-18',
    start: '08:00',
    end: '20:00',
  }
  await r.create('duty', payload)
  await assert.rejects(
    r.create('duty', { ...payload, start: '09:00', end: '21:00' }),
    /重复|重叠/,
  )
})
test('moving a referenced camera, host or person cannot break site associations', async () => {
  const r = make()
  await assert.rejects(r.update('cameras', 'c1', { placeId: 'p2' }), /关联/)
  await assert.rejects(r.update('hosts', 'h1', { placeId: 'p2' }), /关联/)
  await assert.rejects(r.update('personnel', 'u1', { placeId: 'p2' }), /关联/)
})
test('share tokens expire and cannot resolve after deadline', async () => {
  let time = new Date('2026-10-18T04:00:00Z')
  const r = createRepository({ storage: storage(), now: () => time })
  const share = (
    await r.action('shares', '', 'create', { kind: 'duty', resourceId: 'p1' })
  ).data
  assert.equal(
    (await r.action('shares', share.token, 'resolve')).data.resourceId,
    'p1',
  )
  time = new Date('2026-10-19T04:00:01Z')
  await assert.rejects(r.action('shares', share.token, 'resolve'), /失效/)
})
test('matching SMS rule emits one simulated record on accept', async () => {
  const r = make()
  await r.action('alarms', 'a1', 'transition', {
    action: 'accept',
    note: '演示接单',
  })
  assert.equal((await r.list('sms', {})).total, 1)
  await assert.rejects(
    r.action('alarms', 'a1', 'transition', { action: 'accept', note: '重复' }),
  )
  assert.equal((await r.list('sms', {})).total, 1)
})
