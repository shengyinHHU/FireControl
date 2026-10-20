import test from 'node:test'
import assert from 'node:assert/strict'
import { clusterPlaces, placeIcon } from '../../src/api/fire/mapPresentation.js'
test('clusters nearby places at street scale and separates them when zoomed in', () => {
 const rows=[{id:'1',lng:118.75,lat:32.23},{id:'2',lng:118.7508,lat:32.23},{id:'3',lng:118.79,lat:32.25}]
 assert.deepEqual(clusterPlaces(rows,14).map(g=>g.rows.length),[2,1])
 assert.equal(clusterPlaces(rows,18).length,3)
 assert.deepEqual(clusterPlaces([...rows].reverse(),14),clusterPlaces(rows,14))
})
test('coincident places remain accessible as a group and invalid coordinates are excluded', () => {
 const rows=[{id:'1',lng:118.75,lat:32.23},{id:'2',lng:118.75,lat:32.23},{id:'3',lng:null,lat:null}]
 assert.equal(clusterPlaces(rows,18)[0].rows.length,2)
 assert.equal(clusterPlaces([],14).length,0)
 assert.equal(new Set(['industry','highrise','commercial','elderly'].map(placeIcon)).size,4)
 assert.ok(!placeIcon('<script>').includes('<script>'))
})
