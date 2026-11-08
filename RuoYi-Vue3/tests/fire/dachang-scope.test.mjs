import test from 'node:test'
import assert from 'node:assert/strict'
import { isInDachang, geometryCovers } from '../../src/api/fire/dachangScope.js'
test('map scope excludes northern point accepted by older backend scope', () => {
 assert.equal(isInDachang({lng:118.777719,lat:32.265056}),false)
 assert.equal(isInDachang({lng:118.75163,lat:32.212305}),true)
 assert.equal(isInDachang({lng:null,lat:32.23}),false)
 assert.equal(isInDachang({lng:' ',lat:32.23}),false)
})
test('polygon holes, disjoint pieces and boundary points are respected', () => {
 const g={type:'MultiPolygon',coordinates:[[[[0,0],[4,0],[4,4],[0,4],[0,0]],[[1,1],[2,1],[2,2],[1,2],[1,1]]],[[[6,0],[7,0],[7,1],[6,1],[6,0]]]]}
 assert.equal(geometryCovers(g,.5,.5),true)
 assert.equal(geometryCovers(g,1.5,1.5),false)
 assert.equal(geometryCovers(g,5,.5),false)
 assert.equal(geometryCovers(g,6.5,.5),true)
 assert.equal(geometryCovers(g,0,2),true)
})
