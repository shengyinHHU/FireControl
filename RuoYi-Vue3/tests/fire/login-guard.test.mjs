import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs'
import vm from 'node:vm'

function setup(token) {
  let guard
  let infoCalls = 0
  const user = { roles: [], async getInfo() { infoCalls++; this.roles = ['admin'] }, async logOut() {} }
  const source = fs.readFileSync(new URL('../../src/permission.js', import.meta.url), 'utf8').replace(/^import .*$/gm, '')
  vm.runInNewContext(source, {
    isFireMock: true,
    router: { beforeEach(fn) { guard = fn }, afterEach() {}, addRoute() {} },
    NProgress: { configure() {}, start() {}, done() {} },
    getToken: () => token,
    isPathMatch: (pattern, path) => pattern === path,
    isHttp: () => false,
    isRelogin: { show: false },
    ElMessage: { error() {} },
    useUserStore: () => user,
    useLockStore: () => ({ isLock: false }),
    useSettingsStore: () => ({ setTitle() {} }),
    usePermissionStore: () => ({ async generateRoutes() { return [] } })
  })
  return { navigate: (path, fullPath = path) => guard({ path, fullPath, meta: {} }, {}), infoCalls: () => infoCalls }
}

test('demo mode requires login and preserves the dashboard destination', async () => {
  const app = setup(undefined)
  assert.equal(await app.navigate('/fire/dashboard', '/fire/dashboard?grid=1'), '/login?redirect=/fire/dashboard?grid=1')
  assert.equal(await app.navigate('/login'), true)
  assert.equal(await app.navigate('/fire-public/duty'), true)
})

test('authenticated dashboard initializes actual user roles', async () => {
  const app = setup('test-token')
  const result = await app.navigate('/fire/dashboard')
  assert.equal(result.path, '/fire/dashboard')
  assert.equal(result.replace, true)
  assert.equal(app.infoCalls(), 1)
  assert.equal(await app.navigate('/fire/dashboard'), true)
})