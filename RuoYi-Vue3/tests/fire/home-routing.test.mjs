import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs'
import { build } from 'esbuild'

// Exercise the maintained route records with Vue Router's memory history.
// Only page components and permission/backend dependencies are replaced.
async function routerFixture(mock, allowed = true) {
  const result = await build({
    entryPoints: ['src/router/index.js'], bundle: true, write: false,
    format: 'esm', platform: 'node',
    plugins: [{ name: 'routing-fixtures', setup(builder) {
      builder.onLoad({ filter: /router[\\/]index\.js$/ }, async args => ({
        contents: fs.readFileSync(args.path, 'utf8').replaceAll('createWebHistory', 'createMemoryHistory'),
        resolveDir: args.path.replace(/[\\/][^\\/]+$/, '')
      }))
      builder.onResolve({ filter: /^@\// }, args => ({ path: args.path, namespace: 'fixture' }))
      builder.onLoad({ filter: /.*/, namespace: 'fixture' }, args => ({ contents:
        args.path === '@/api/fire/config' ? `export const isFireMock = ${mock}` :
        args.path === '@/api/fire/access' ? `export const canFire = () => ${allowed}` :
        'export default {}'
      }))
    }}]
  })
  const module = await import('data:text/javascript;base64,' + Buffer.from(result.outputFiles[0].text).toString('base64'))
  return module.default
}

for (const mock of [true, false]) {
  test(`${mock ? 'demo' : 'production'} home and legacy bookmarks open the full-screen dashboard`, async () => {
    const router = await routerFixture(mock)
    for (const path of ['/', '/index', '/fire/dashboard']) {
      await router.push(path + '?grid=1#overview')
      assert.equal(router.currentRoute.value.fullPath, '/fire/dashboard?grid=1#overview')
      assert.equal(router.currentRoute.value.name, 'FireDashboard')
    }
    assert.equal(router.getRoutes().some(route => route.meta.affix && route.name === 'Index'), false)
  })
}

test('production home retains the dashboard permission check', async () => {
  const router = await routerFixture(false, false)
  await router.push('/index')
  assert.equal(router.currentRoute.value.path, '/401')
})
