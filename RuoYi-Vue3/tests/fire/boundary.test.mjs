import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs'
const data = JSON.parse(fs.readFileSync('src/assets/geo/dachang-boundary.json','utf8'))
test('street boundary preserves 25 source regions and closed valid coordinate rings', () => {
  assert.equal(data.regions.length,25)
  assert.equal(data.regions[0].properties.name,'吴家洼')
  assert.equal(data.regions[24].properties.name,'白色部分')
  assert.deepEqual(data.regions[0].geometry.coordinates[0][0],[118.759953,32.213748])
  assert.equal(data.regions.reduce((n,r) => n+r.geometry.coordinates[0].length,0),339)
  for (const region of data.regions) {
    const ring = region.geometry.coordinates[0]
    assert.deepEqual(ring[0],ring.at(-1))
    assert.ok(ring.length>=4)
    for (const [lng,lat] of ring) assert.ok(lng>=data.bounds[0] && lng<=data.bounds[2] && lat>=data.bounds[1] && lat<=data.bounds[3])
  }
})
test('union retains disjoint source segments instead of expanding to a rectangular or convex envelope', () => {
  assert.equal(data.geometry.type,'MultiPolygon')
  assert.equal(data.geometry.coordinates.length,22)
  for(const polygon of data.geometry.coordinates) for(const ring of polygon) assert.deepEqual(ring[0],ring.at(-1))
})
