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
// Tabler Icons (MIT); see src/assets/icons/tabler/LICENSE.
const paths = {
  "fire": "<path d=\"M12 10.941c2.333 -3.308 .167 -7.823 -1 -8.941c0 3.395 -2.235 5.299 -3.667 6.706c-1.43 1.408 -2.333 3.294 -2.333 5.588c0 3.704 3.134 6.706 7 6.706c3.866 0 7 -3.002 7 -6.706c0 -1.712 -1.232 -4.403 -2.333 -5.588c-2.084 3.353 -3.257 3.353 -4.667 2.235\" />",
  "traffic": "<path d=\"M5 17a2 2 0 1 0 4 0a2 2 0 1 0 -4 0\" />\n  <path d=\"M15 17a2 2 0 1 0 4 0a2 2 0 1 0 -4 0\" />\n  <path d=\"M5 17h-2v-6l2 -5h9l4 5h1a2 2 0 0 1 2 2v4h-2m-4 0h-6m-6 -6h15m-6 0v-5\" />",
  "rescue": "<path d=\"M8 12a4 4 0 1 0 8 0a4 4 0 1 0 -8 0\" />\n  <path d=\"M3 12a9 9 0 1 0 18 0a9 9 0 1 0 -18 0\" />\n  <path d=\"M15 15l3.35 3.35\" />\n  <path d=\"M9 15l-3.35 3.35\" />\n  <path d=\"M5.65 5.65l3.35 3.35\" />\n  <path d=\"M18.35 5.65l-3.35 3.35\" />",
  "assist": "<path d=\"M19.5 12.572l-7.5 7.428l-7.5 -7.428a5 5 0 1 1 7.5 -6.566a5 5 0 1 1 7.5 6.572\" />\n  <path d=\"M12 6l-3.293 3.293a1 1 0 0 0 0 1.414l.543 .543c.69 .69 1.81 .69 2.5 0l1 -1a3.182 3.182 0 0 1 4.5 0l2.25 2.25\" />\n  <path d=\"M12.5 15.5l2 2\" />\n  <path d=\"M15 13l2 2\" />",
  "unknown": "<path d=\"M10 5a2 2 0 1 1 4 0a7 7 0 0 1 4 6v3a4 4 0 0 0 2 3h-16a4 4 0 0 0 2 -3v-3a7 7 0 0 1 4 -6\" />\n  <path d=\"M9 17v1a3 3 0 0 0 6 0v-1\" />",
  "mixed": "<path d=\"M8 6a2 2 0 0 1 2 -2h8a2 2 0 0 1 2 2v8a2 2 0 0 1 -2 2h-8a2 2 0 0 1 -2 -2l0 -8\" />\n  <path d=\"M4 10a2 2 0 0 1 2 -2h8a2 2 0 0 1 2 2v8a2 2 0 0 1 -2 2h-8a2 2 0 0 1 -2 -2l0 -8\" />"
}
export function incidentIcon(kind) {
 return `<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">${paths[kind] || paths.unknown}</svg>`
}
export const incidentEnded = row => /返队|归档|已结束|已结案|已处理|处置结束|已办结/.test(row.status || row.caseStatusName || "")
export function incidentClusterKind(rows) {
 const kinds = new Set(rows.map(incidentKind))
 return kinds.size === 1 ? [...kinds][0] : "mixed"
}
