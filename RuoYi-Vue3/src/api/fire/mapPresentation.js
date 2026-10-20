import { hasMapCoordinates } from './placeLayers.js'
// Screen-scale distance in Web Mercator pixels. Recomputed on every zoom/filter update.
export function clusterPlaces(rows, zoom) {
  const size = 256 * 2 ** zoom
  const radius = zoom < 16 ? 64 : 36
  const groups = []
  for (const row of [...rows].sort((a,b) => String(a.id).localeCompare(String(b.id)))) {
    if (!hasMapCoordinates(row.lng, row.lat)) continue
    const sin = Math.sin(Math.max(-85, Math.min(85, Number(row.lat))) * Math.PI / 180)
    const x = (Number(row.lng) + 180) / 360 * size
    const y = (0.5 - Math.log((1 + sin) / (1 - sin)) / (4 * Math.PI)) * size
    let group = groups.find(g => Math.hypot(g.x - x, g.y - y) < radius)
    if (!group) { group = { x, y, rows: [] }; groups.push(group) }
    group.rows.push(row)
  }
  return groups.map(g => ({ rows: g.rows, position: [
    g.rows.reduce((n,r) => n + Number(r.lng), 0) / g.rows.length,
    g.rows.reduce((n,r) => n + Number(r.lat), 0) / g.rows.length,
  ] }))
}
const paths = {
  industry: '<path d="M3 21V10l6 3V8l6 4V5h4v16H3Z"/><path d="M7 17h1m4 0h1m4 0h1M15 5V3h4v2"/>',
  highrise: '<path d="M6 21V3h12v18M3 21h18M9 7h1m4 0h1M9 11h1m4 0h1M9 15h1m4 0h1M10 21v-3h4v3"/>',
  commercial: '<path d="M4 10v11h16V10M3 10l2-6h14l2 6M3 10c0 3 4 3 4 0 0 3 5 3 5 0 0 3 5 3 5 0 0 3 4 3 4 0M9 21v-6h6v6"/>',
  elderly: '<path d="m3 10 9-7 9 7M5 9v12h14V9M9 14h6m-3-3v6"/>',
}
export function placeIcon(type) {
  return `<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round">${paths[type] || paths.industry}</svg>`
}
