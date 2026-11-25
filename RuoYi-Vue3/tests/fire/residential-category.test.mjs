import test from 'node:test'
import assert from 'node:assert/strict'
import { build } from 'esbuild'
import { placeLayer, normalizePlaceCategory, normalizePlaceRecord, filterMapPlaces } from '../../src/api/fire/placeLayers.js'
import { createRepository } from '../../src/mock/fire/repository.js'
import { createSeed } from '../../src/mock/fire/seed.js'

test('residential display, filter and database identity use the same highrise code', () => {
  assert.equal(placeLayer('highrise').label, '居民小区')
  for (const value of ['高层建筑', '高层住宅', '居民小区', 'highrise']) assert.equal(normalizePlaceCategory(value), '居民小区')
  const row = normalizePlaceRecord({ placeId: '101', placeType: 'highrise', name: '扬子2村', longitude: 118.76, latitude: 32.23 })
  assert.equal(row.category, '居民小区')
  assert.equal(row.placeType, 'highrise')
  assert.deepEqual(filterMapPlaces([row], ['居民小区']), [row])
  assert.deepEqual(filterMapPlaces([row], ['重点企业']), [])
})

test('old demo storage keeps its records, filters and counts after the display name changes', async () => {
  const seed = createSeed()
  for (const row of seed.places) if (row.category === '居民小区') row.category = '高层建筑'
  let saved = JSON.stringify(seed)
  const repo = createRepository({ storage: { getItem: () => saved, setItem: (_, value) => { saved = value } } })
  const rows = await repo.list('places', { category: '居民小区' })
  assert.equal(rows.total, 2)
  assert.ok(rows.rows.every(row => row.category === '居民小区'))
  const counts = (await repo.dashboard()).data.categories
  assert.equal(counts.find(row => row.name === '居民小区').count, 2)
  assert.ok(!counts.some(row => row.name === '高层建筑'))
  const added = await repo.create('places', { name: '居民小区兼容测试', category: '高层建筑', gridId: 'g1' })
  assert.equal(added.data.category, '居民小区')
  assert.equal((await repo.list('places', { category: '居民小区' })).total, 3)
})

async function transportModule(entry) {
  const result = await build({ entryPoints: [entry], bundle: true, write: false, format: 'esm', platform: 'node', plugins: [{
    name: 'category-transport-fixture', setup(builder) {
      builder.onResolve({ filter: /^@\/utils\/(?:request|auth)$/ }, args => ({ path: args.path, namespace: 'fixture' }))
      builder.onLoad({ filter: /.*/, namespace: 'fixture' }, args => ({ contents: args.path.endsWith('/auth')
        ? 'export const getToken = () => "fixture"'
        : `export default async function request(config) {
          globalThis.__categoryRequest = config;
          if (config.url.endsWith('/list')) return { rows: [], total: 0 };
          return { code: 200, data: { batchId: 'old-batch', rows: [{ type: 'highrise', category: '高层建筑' }, { category: '高层建筑' }] } };
        }` }))
    },
  }] })
  return import('data:text/javascript;base64,' + Buffer.from(result.outputFiles[0].text).toString('base64'))
}

test('both category names query highrise and old import batches display the current label', async () => {
  try {
    const places = await transportModule('src/api/fire/places.js')
    for (const category of ['高层建筑', '居民小区']) {
      await places.listDatabasePlaces({ category })
      assert.equal(globalThis.__categoryRequest.params.placeType, 'highrise')
    }
    const importer = await transportModule('src/api/fire/placeImport.js')
    const file = new Blob(['fixture'])
    for (const response of [await importer.previewPlaceImport(file), await importer.confirmPlaceImport('old-batch')]) {
      assert.ok(response.data.rows.every(row => row.category === '居民小区'))
      assert.equal(response.data.rows[0].type, 'highrise')
      assert.equal(response.data.batchId, 'old-batch')
    }
  } finally { delete globalThis.__categoryRequest }
})
