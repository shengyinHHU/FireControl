import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import { build } from 'esbuild'
import { PLACE_LAYERS, filterMapPlaces, hasMapCoordinates, normalizeMapPlace, normalizePlaceRecord } from '../../src/api/fire/placeLayers.js'
import { usesDatabase, canWriteResource } from '../../src/api/fire/sourcePolicy.js'

const record = (placeId, placeType, extra = {}) => ({ placeId, placeType, name: `机构${placeId}`, address: '测试路1号', longitude: '118.75', latitude: '32.23', coordinateSystem: 'GCJ02', locationStatus: 'review', ...extra })

test('all four database codes map to the existing labels and support independent add/remove', () => {
  const places = PLACE_LAYERS.map((layer, i) => normalizeMapPlace(record(String(i + 1), layer.type)))
  assert.equal(filterMapPlaces(places, []).length, 0)
  for (const layer of PLACE_LAYERS) assert.deepEqual(filterMapPlaces(places, [layer.label]).map(p => p.placeType), [layer.type])
  assert.equal(filterMapPlaces(places, PLACE_LAYERS.map(l => l.label)).length, 4)
  assert.equal(filterMapPlaces(places, ['重点企业', '养老院']).length, 2)
  assert.equal(filterMapPlaces(places, ['养老院']).length, 1)
  assert.equal(filterMapPlaces(places, []).length, 0)
})

test('pending review coordinates remain visible; unlocated rows remain in the real list but not map', () => {
  assert.equal(normalizeMapPlace(record('1', 'industry')).locationStatus, 'review')
  assert.equal(normalizeMapPlace(record('2', 'industry', { longitude: null })), null)
  assert.equal(normalizePlaceRecord(record('2', 'industry', { longitude: null })).lng, null)
  for (const [lng, lat] of [[null, 32], ['', 32], [' ', 32], [118, undefined], [NaN, 32], [181, 32], [118, 91], [0, 0]]) assert.equal(hasMapCoordinates(lng, lat), false)
  assert.equal(normalizeMapPlace(record('9007199254740993', 'highrise')).id, '9007199254740993')
})

test('database resources never merge or write demo records', () => {
  assert.equal(usesDatabase('places'), true)
  assert.equal(canWriteResource('places'), false)
  assert.equal(usesDatabase('cameras'), false)
})

async function bundled(entry, plugins = []) {
  const result = await build({ entryPoints: [entry], bundle: true, write: false, format: 'esm', platform: 'node', plugins })
  return import('data:text/javascript;base64,' + Buffer.from(result.outputFiles[0].text).toString('base64'))
}

test('even in mock mode real place reads and failures never fall back to the demo repository', async () => {
  const plugin = { name: 'transport-fixture', setup(build) {
    build.onResolve({ filter: /^(?:@\/utils\/request|@\/utils\/auth|@\/plugins\/auth|@\/mock\/fire\/repository|\.\/config)$/ }, args => ({ path: args.path, namespace: 'fixture' }))
    build.onLoad({ filter: /.*/, namespace: 'fixture' }, args => ({ contents:
      args.path === './config' ? 'export const isFireMock = true' :
      args.path === '@/plugins/auth' ? 'export default { hasPermi: () => true }' :
      args.path === '@/utils/auth' ? 'export const getToken = () => "test-token"' :
      args.path === '@/mock/fire/repository' ? 'export function createRepository(){ throw Error("Demo access forbidden") }' :
      'export default async function request(config) { if(globalThis.__placeTransportError) throw Error("backend unavailable"); return config.url.endsWith("/list") ? {rows:[{placeId:"777",placeType:"industry",name:"数据库机构",longitude:null,latitude:null}],total:1} : {data:{placeId:"777",placeType:"industry",name:"数据库机构"}} }'
    }))
  }}
  const { fire } = await bundled('src/api/fire/index.js', [plugin])
  const list = await fire.list('places')
  assert.equal(list.rows[0].id, '777')
  assert.equal(list.rows[0].source, 'database')
  assert.equal((await fire.get('places', '777')).data.name, '数据库机构')
  globalThis.__placeTransportError = true
  try { await assert.rejects(fire.list('places'), /backend unavailable/) } finally { delete globalThis.__placeTransportError }
  await assert.rejects(fire.create('places', { name: '模拟写入' }), /真实数据库/)
})

test('map adapter removes stale markers, does not duplicate after repeated toggles, and retains database coordinates', async () => {
  const plugin = { name: 'alias', setup(build) {
    build.onResolve({ filter: /^@\// }, args => ({ path: path.resolve('src', args.path.slice(2)) }))
  }}
  const { mountMap } = await bundled('src/api/fire/mapAdapter.js', [plugin])
  class Marker { constructor(options) { this.options = options } }
  class FakeMap {
    static latest
    constructor() { FakeMap.latest = this; this.overlays = new Set() }
    add(items) { for (const item of Array.isArray(items) ? items : [items]) this.overlays.add(item) }
    remove(items) { for (const item of Array.isArray(items) ? items : [items]) this.overlays.delete(item) }
    getZoom() { return 14 } on() {} setFeatures() {} destroy() { this.overlays.clear() }
  }
  class Element {
    constructor() { this.style = {}; this.attributes = {}; this.children = [] }
    setAttribute(k, v) { this.attributes[k] = v } removeAttribute(k) { delete this.attributes[k] }
    replaceChildren(...children) { this.children = children }
  }
  const beforeWindow = globalThis.window, beforeDocument = globalThis.document
  globalThis.window = { AMap: { Map: FakeMap, Marker, Pixel: class {}, InfoWindow: class { close() {} }, TileLayer: { Traffic: class {} } } }
  globalThis.document = { createElement: () => new Element() }
  try {
    const adapter = await mountMap({}, { showBoundary: false, onSelect() {}, onPoint() {} })
    const places = [normalizeMapPlace(record('1', 'industry')), normalizeMapPlace(record('2', 'elderly'))]
    const render = layers => adapter.setFeatures({ places: filterMapPlaces(places, layers), grids: [], cameras: [], devices: [], alarms: [], layers })
    const markers = () => [...FakeMap.latest.overlays].filter(x => x instanceof Marker)
    render(['重点企业']); assert.equal(markers().length, 1)
    assert.deepEqual(markers()[0].options.position, [118.75, 32.23])
    assert.match(markers()[0].options.content.children[0].innerHTML, /<svg/)
    render(['重点企业', '养老院']); assert.equal(markers().length, 1); assert.equal(markers()[0].options.content.textContent, 2)
    render(['养老院']); assert.equal(markers().length, 1)
    render([]); assert.equal(markers().length, 0)
    for (let i = 0; i < 10; i++) { render(['重点企业']); render([]) }
    assert.equal(markers().length, 0)
    adapter.destroy()
  } finally { globalThis.window = beforeWindow; globalThis.document = beforeDocument }
})