import { isActiveFire } from './rules.js'
export const INCIDENT_LAYERS = ['119警情', '消控主机', 'AI预警']
export function incidentSource(row) {
  if (row.sourceCode === '119' || row.source === '119警情') return '119警情'
  if (row.sourceCode === 'AI' || /AI/.test(row.source || '')) return 'AI预警'
  return '消控主机'
}
export function incidentKind(row) {
  if (incidentSource(row) === '119警情') {
    if (String(row.caseTypeCode) === '1') return 'fire'
    if (/交通|车辆事故/.test(row.caseNatureName || '')) return 'traffic'
    if (String(row.caseTypeCode) === '2') return 'rescue'
    if (String(row.caseTypeCode) === '3') return 'assist'
    return 'unknown'
  }
  return row.alarmType === 'FIRE' ? 'fire' : 'unknown'
}
export const incidentUrgent = row => incidentKind(row) === 'fire' && isActiveFire(row)
export const filterIncidents = (rows, layers) => rows.filter(row => layers.includes(incidentSource(row)))
export function incidentSummary(row) {
  const title = String(row.name || row.caseTypeName || '警情详情').replace(/\d{7,}/g, '…')
  return `${incidentSource(row)} · ${title.length > 18 ? title.slice(0,18) + '…' : title}`
}
const paths = {
 fire: '<path d="M13 2c1 5-5 6-3 11-3-1-3-4-3-4-6 7-1 13 5 13s10-6 6-12c0 4-3 4-3 4s3-7-2-12Z"/>',
 traffic: '<path d="m4 10 2-6h12l2 6M3 10h18v8H3zM5 18v3m14-3v3M6 14h2m8 0h2"/>',
 rescue: '<circle cx="12" cy="12" r="9"/><circle cx="12" cy="12" r="4"/><path d="m6 5 3 4m6 6 3 4M5 18l4-3m6-6 4-3"/>',
 assist: '<path d="M3 16h4l3-3h7c3 0 3 3 0 3h-5m-5 0 4 5h7l4-6M3 14v8M12 9C3 4 10 0 12 4c2-4 9 0 0 5Z"/>',
 unknown: '<path d="M5 17h14l-2-3V9a5 5 0 0 0-10 0v5l-2 3Zm5 3h4M12 2v2"/>',
}
export function incidentIcon(kind) {
 return `<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">${paths[kind] || paths.unknown}</svg>`
}
