export function createLatestRequest() {
  let version = 0
  return {
    cancel() { version++ },
    async run(load, apply, failed = () => {}) {
      const current = ++version
      try { const value = await load(); if (current === version) apply(value) }
      catch (error) { if (current === version) failed(error) }
    }
  }
}
export function validateFields(fields, values) {
  for (const f of fields) {
    const v = values[f.key]
    if (['name', 'placeId', 'category', 'gridId', 'personnelId', 'deadline'].includes(f.key) && (v === undefined || v === null || !String(v).trim())) throw Error(`请填写${f.label}`)
    if (f.type === 'select' && v && !f.options.includes(v)) throw Error(`${f.label}无效`)
    if (f.key === 'phone' && v && !/^1\d{10}$/.test(v)) throw Error('请输入11位手机号')
    if (['lng', 'lat'].includes(f.key) && v !== undefined && v !== null && v !== '' && (!Number.isFinite(Number(v)) || Math.abs(Number(v)) > (f.key === 'lng' ? 180 : 90))) throw Error(`${f.label}超出有效范围`)
    if (f.key === 'quantity' && (!Number.isFinite(Number(v)) || Number(v) < 0)) throw Error('物资数量必须为非负数')
  }
}
export function validateDutyInput(r) {
  if (!r.name?.trim()) throw Error('请填写值班人员姓名')
  if (!/^1\d{10}$/.test(r.phone)) throw Error('请输入11位手机号')
  if (!/^\d{4}-\d{2}-\d{2}$/.test(r.date || '') || ![r.start, r.end].every(t => /^([01]\d|2[0-3]):[0-5]\d$/.test(t || '')) || r.start === r.end) throw Error('请填写有效日期和不同的起止时间')
  const start = new Date(`${r.date}T${r.start}:00+08:00`)
  if (!Number.isFinite(+start) || new Intl.DateTimeFormat('en-CA', { timeZone: 'Asia/Shanghai' }).format(start) !== r.date) throw Error('日期无效')
}
export function hostAlarmSummary(alarms) {
  const rows = alarms
  return { rows, total: rows.length, closed: rows.filter(a => a.status === '已办结').length, pending: rows.filter(a => a.status !== '已办结').length }
}

// Alarm classification is independent of the reporting source.
export const isActiveFire = a => a.alarmType === 'FIRE' && ['待处理', '处理中'].includes(a.status)
export function fireAlarmPoints(alarms, places, devices = []) {
  const valid = p => p && [p.lng, p.lat].every(v => v !== null && v !== undefined && v !== '' && Number.isFinite(Number(v))) && Math.abs(Number(p.lng)) <= 180 && Math.abs(Number(p.lat)) <= 90
  return alarms.filter(isActiveFire).flatMap(a => {
    const place = places.find(p => p.id === a.placeId)
    const device = devices.find(d => d.id === a.deviceId && d.placeId === a.placeId)
    const point = [a, device, place].find(valid)
    return point ? [{ ...a, lng: Number(point.lng), lat: Number(point.lat), placeName: place?.name || a.placeName }] : []
  })
}
