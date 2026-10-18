import { createSeed } from './seed.js'
const copy = value => JSON.parse(JSON.stringify(value))
const ok = data => ({ code: 200, msg: '操作成功', data: copy(data) })
export function createRepository({ storage, now = () => new Date() } = {}) {
  const key = 'fire-demo-v1'
  let db = createSeed(now())
  function sync() { try { const saved = JSON.parse(storage?.getItem(key) || 'null'); if (saved?.version === 1 && Object.keys(db).every(k => k === 'version' || Array.isArray(saved[k]))) db = saved; for (const a of db.alarms) { if (!a.alarmType) a.alarmType = a.name === '三楼烟感探测器报警' ? 'FIRE' : a.name === '主机通信中断，设备离线' ? 'FAULT' : 'WARNING' } for (const a of db.alarms) for (const event of a.timeline || []) { if (event.note === '\u6a21\u62df告警接入') event.note = '演示告警接入' } for (const row of db.sms) { if (row.status === '\u6a21\u62df发送') row.status = '演示发送' } } catch { /* A corrupt demo cache is replaced by seed data. */ } }
  sync()
  const stamp = () => new Intl.DateTimeFormat('sv-SE', { timeZone: 'Asia/Shanghai', year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: false }).format(now())
  const table = name => { if (!Array.isArray(db[name])) throw Error('未知业务资源'); return db[name] }
  const find = (name, id) => { const row = table(name).find(x => x.id === id); if (!row) throw Error('记录不存在或已删除'); return row }
  const id = () => globalThis.crypto?.randomUUID?.() || `id-${Date.now()}-${Math.random().toString(36).slice(2)}`
  const commit = fn => { sync(); const before = copy(db); try { const result = fn(); try { storage?.setItem(key, JSON.stringify(db)) } catch { throw Error('保存失败：浏览器存储空间不足或不可用，请导出记录后重试') } return result } catch (e) { db = before; throw e } }
  const required = (r, keys) => keys.forEach(k => { if (r[k] === undefined || r[k] === null || String(r[k]).trim() === '') throw Error(`请填写${({ name: '名称', placeId: '场所', category: '类别', result: '查岗结论', phone: '手机号', start: '开始时间', end: '结束时间', date: '日期', personnelId: '接收人', deadline: '截止时间' })[k] || k}`) })
  function validate(name, r) {
    if (['places', 'grids', 'personnel', 'materials', 'hazards', 'cameras', 'devices', 'hosts', 'tasks', 'smsRules'].includes(name)) required(r, ['name'])
    if (name === 'places') { required(r, ['category', 'gridId']); if (!['重点企业', '高层建筑', '综合商业体', '养老院'].includes(r.category)) throw Error('场所类别无效') }
    if (r.gridId) find('grids', r.gridId)
    if (['personnel', 'materials', 'hazards', 'cameras', 'devices', 'hosts', 'tasks', 'inspections', 'duty'].includes(name)) { required(r, ['placeId']); find('places', r.placeId) }
    if (r.phone && !/^1\d{10}$/.test(r.phone)) throw Error('请输入11位手机号')
    if (r.lng !== undefined && r.lng !== '' && (!Number.isFinite(Number(r.lng)) || Number(r.lng) < -180 || Number(r.lng) > 180)) throw Error('经度范围应为-180至180')
    if (r.lat !== undefined && r.lat !== '' && (!Number.isFinite(Number(r.lat)) || Number(r.lat) < -90 || Number(r.lat) > 90)) throw Error('纬度范围应为-90至90')
    if (r.polygon && (r.polygon.length < 3 || new Set(r.polygon.map(p => p.join(','))).size < 3 || r.polygon.some(p => p.length !== 2 || !p.every(Number.isFinite) || Math.abs(p[0]) > 180 || Math.abs(p[1]) > 90))) throw Error('网格至少需要三个不同的有效坐标点')
    if (name === 'materials' && (!Number.isFinite(Number(r.quantity)) || Number(r.quantity) < 0)) throw Error('物资数量不能为负数')
    if (name === 'tasks') { required(r, ['personnelId', 'deadline']); if (find('personnel', r.personnelId).placeId !== r.placeId) throw Error('接收人不属于该场所') }
    if (name === 'inspections') { required(r, ['name', 'result', 'cameraId']); if (!['在岗', '脱岗'].includes(r.result)) throw Error('查岗结论无效'); if (find('cameras', r.cameraId).placeId !== r.placeId) throw Error('摄像头与场所不匹配') }
    for (const [field, target] of [['cameraId', 'cameras'], ['hostId', 'hosts']]) if (r[field] && find(target, r[field]).placeId !== r.placeId) throw Error('关联设备不属于同一场所')
    if (name === 'smsRules' && (!r.recipients || r.recipients.split(/[,，\s]+/).some(p => !/^1\d{10}$/.test(p)))) throw Error('短信接收人须为有效手机号，多个号码以逗号分隔')
    if (name === 'duty') {
      required(r, ['name', 'phone', 'date', 'start', 'end'])
      if (!/^\d{4}-\d{2}-\d{2}$/.test(r.date) || ![r.start, r.end].every(t => /^([01]\d|2[0-3]):[0-5]\d$/.test(t)) || r.start === r.end) throw Error('请填写有效日期和不同的起止时间')
      const start = new Date(`${r.date}T${r.start}:00+08:00`), end = new Date(`${r.date}T${r.end}:00+08:00`)
      if (!Number.isFinite(+start) || !Number.isFinite(+end)) throw Error('日期无效')
      if (new Intl.DateTimeFormat('en-CA', { timeZone: 'Asia/Shanghai' }).format(start) !== r.date) throw Error('日期无效')
      if (end < start) end.setTime(+end + 86400000)
      if (db.duty.some(d => d.id !== r.id && d.placeId === r.placeId && d.phone === r.phone && +new Date(d.startsAt) < +end && +new Date(d.endsAt) > +start)) throw Error('该人员已有重叠班次，请勿重复上报')
      r.startsAt = start.toISOString(); r.endsAt = end.toISOString(); r.status = '已上报'
    }
  }
  const enrich = (resource, row) => ({ ...copy(row), placeName: db.places.find(p => p.id === row.placeId)?.name, gridName: db.grids.find(g => g.id === row.gridId)?.name, personnelName: db.personnel.find(p => p.id === row.personnelId)?.name, ...(resource === 'places' ? { onDuty: db.duty.filter(d => d.placeId === row.id && +new Date(d.startsAt) <= +now() && +new Date(d.endsAt) > +now()) } : {}) })
  const repo = {
    async list(resource, query = {}) {
      sync()
      let rows = table(resource).map(x => enrich(resource, x))
      const { pageNum = 1, pageSize = 10, keyword, dateFrom, dateTo, ...filters } = query
      if (keyword) rows = rows.filter(r => [r.name, r.phone, r.code, r.placeName, r.owner, r.serial, r.description].some(v => String(v || '').toLowerCase().includes(String(keyword).toLowerCase())))
      for (const [k, v] of Object.entries(filters)) if (v !== '' && v !== null && v !== undefined) rows = rows.filter(r => String(r[k]) === String(v))
      if (dateFrom) rows = rows.filter(r => (r.occurredAt || r.time || r.date || '') >= dateFrom)
      if (dateTo) rows = rows.filter(r => (r.occurredAt || r.time || r.date || '') <= `${dateTo} 23:59:59`)
      return { code: 200, msg: '查询成功', total: rows.length, rows: rows.slice((Math.max(1, Number(pageNum)) - 1) * Number(pageSize), Math.max(1, Number(pageNum)) * Number(pageSize)) }
    },
    async get(resource, rowId) { sync(); return ok(enrich(resource, find(resource, rowId))) },
    async create(resource, payload) {
      return commit(() => {
        if (resource === 'duty' && payload.submissionId) { const existing = db.duty.find(d => d.submissionId === payload.submissionId); if (existing) return ok(existing) }
        const row = { ...copy(payload), id: id() }; validate(resource, row)
        if (resource === 'inspections') row.time = stamp()
        if (resource === 'tasks') row.status = '待执行'
        table(resource).unshift(row); return ok(enrich(resource, row))
      })
    },
    async update(resource, rowId, payload) { return commit(() => { const row = find(resource, rowId); const next = { ...row, ...copy(payload), id: rowId }; const foreign = { cameras: 'cameraId', hosts: 'hostId', personnel: 'personnelId', devices: 'deviceId' }[resource]; if (foreign && row.placeId !== next.placeId && Object.values(db).some(rows => Array.isArray(rows) && rows.some(r => r[foreign] === rowId))) throw Error('存在关联记录，不能修改所属场所，请先解除关联'); validate(resource, next); Object.assign(row, next); return ok(enrich(resource, row)) }) },
    async remove(resource, rowId) { return commit(() => {
      find(resource, rowId)
      const foreign = { places: 'placeId', grids: 'gridId', cameras: 'cameraId', hosts: 'hostId', devices: 'deviceId', personnel: 'personnelId' }[resource]
      if (foreign && Object.entries(db).some(([k, rows]) => k !== resource && Array.isArray(rows) && rows.some(r => r[foreign] === rowId))) throw Error('存在关联记录，不能删除；请先解除关联')
      db[resource] = table(resource).filter(r => r.id !== rowId); return ok(null)
    }) },
    async dashboard() { sync(); return ok({ total: db.places.length, categories: ['重点企业', '高层建筑', '综合商业体', '养老院'].map(name => ({ name, count: db.places.filter(p => p.category === name).length })), pending: db.alarms.filter(a => a.status === '待处理').length, processing: db.alarms.filter(a => a.status === '处理中').length, closed: db.alarms.filter(a => a.status === '已办结').length, hosts: db.hosts.length, online: db.hosts.filter(h => h.status === '在线').length, cameras: db.cameras.length, unread: db.messages.filter(m => m.status === '未读').length }) },
    async action(resource, rowId, action, payload = {}) {
      sync()
      if (resource === 'shares' && action === 'resolve') { const share = db.shares.find(s => s.token === rowId); if (!share || share.expiresAt <= +now()) throw Error('分享链接已失效，请重新生成'); return ok(share) }
      if (resource === 'cameras' && ['preview', 'talk'].includes(action)) { const camera = find(resource, rowId); if (camera.status !== '在线') throw Error('摄像头离线，无法连接'); return ok({ mock: true, cameraId: rowId, name: camera.name, url: null, message: '演示会话，未连接真实设备' }) }
      if (resource === 'personnel' && action === 'call') { find(resource, rowId); return ok({ mock: true, message: '演示视频呼叫，未拨打真实号码' }) }
      if (resource === 'grids' && action === 'call') { find(resource, rowId); return ok({ mock: true, message: '演示VOLTE呼叫，未拨打真实号码' }) }
      return commit(() => {
        if (resource === 'demo' && action === 'reset') { db = createSeed(now()); return ok(null) }
        if (resource === 'shares' && action === 'create') {
          if (!['duty', 'video'].includes(payload.kind)) throw Error('分享类型无效')
          find(payload.kind === 'duty' ? 'places' : 'cameras', payload.resourceId)
          const share = { ...payload, id: id(), token: id(), expiresAt: +now() + 86400000 }; db.shares.push(share); return ok(share)
        }
        if (resource === 'devices' && action === 'bind-camera') { const device = find(resource, rowId), camera = find('cameras', payload.cameraId); if (device.placeId !== camera.placeId) throw Error('摄像头必须属于同一场所'); device.cameraId = camera.id; return ok(device) }
        if (resource === 'messages' && action === 'read') { find(resource, rowId).status = '已读'; return ok(null) }
        if (resource === 'alarms' && action === 'transition') {
          const a = find(resource, rowId), next = { accept: ['待处理', '处理中', '接单'], close: ['处理中', '已办结', '办结'] }[payload.action]
          if (!next || a.status !== next[0]) throw Error('当前状态不允许此操作，请刷新后重试')
          if (!payload.note?.trim()) throw Error('请填写处置说明')
          a.status = next[1]; a.timeline.push({ time: stamp(), action: next[2], note: payload.note.trim(), operator: '演示值班员' })
          db.messages.unshift({ id: id(), name: `${a.name} · ${next[2]}`, alarmId: a.id, placeId: a.placeId, status: '未读', source: a.source, time: stamp() })
          for (const rule of db.smsRules.filter(r => r.enabled && r.source === a.source && r.trigger === next[2])) db.sms.unshift({ id: id(), name: a.name, alarmId: a.id, recipients: rule.recipients, status: '演示发送', time: stamp(), source: a.source })
          return ok(a)
        }
        throw Error('暂不支持此操作')
      })
    }
  }
  return repo
}
