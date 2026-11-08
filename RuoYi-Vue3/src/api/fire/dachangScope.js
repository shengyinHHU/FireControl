import boundary from '../../assets/geo/dachang-boundary.json' with { type: 'json' }
// All inputs are already GCJ-02. Use the exact geometry drawn by mapAdapter.
function ringLocation(ring, x, y) {
 let inside = false
 for (let i = 0, j = ring.length - 1; i < ring.length; j = i++) {
  const [ax, ay] = ring[j], [bx, by] = ring[i]
  const cross = (x-ax)*(by-ay)-(y-ay)*(bx-ax)
  if (Math.abs(cross) < 1e-12 && x >= Math.min(ax,bx)-1e-10 && x <= Math.max(ax,bx)+1e-10 && y >= Math.min(ay,by)-1e-10 && y <= Math.max(ay,by)+1e-10) return 0
  if ((ay > y) !== (by > y) && x < (bx-ax)*(y-ay)/(by-ay)+ax) inside = !inside
 }
 return inside ? 1 : -1
}
export function geometryCovers(geometry, x, y) {
 const polygons = geometry.type === 'Polygon' ? [geometry.coordinates] : geometry.coordinates
 return polygons.some(([outer, ...holes]) => ringLocation(outer,x,y) >= 0 && !holes.some(r => ringLocation(r,x,y) === 1))
}
export function isInDachang(row) {
 if (!row || [row.lng,row.lat].some(v => v == null || String(v).trim() === '' || !Number.isFinite(Number(v)))) return false
 return geometryCovers(boundary.geometry, Number(row.lng), Number(row.lat))
}
