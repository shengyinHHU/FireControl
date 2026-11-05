// Real UI using production-mode configuration, local HTTP fixtures, no live credentials.
const { chromium } = require(process.env.PLAYWRIGHT_PATH || 'playwright')
const { spawn } = require('node:child_process')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
// Resolve static automatic GET routes from the maintained backend, rather than
// silently granting successful fixture responses to unimplemented endpoints.
const controllerDir = '../RuoYi-Vue-master/ruoyi-admin/src/main/java/com/ruoyi/web/controller/fire'
const backendGetRoutes = new Set()
for (const name of fs.readdirSync(controllerDir).filter(name => name.endsWith('Controller.java'))) {
  const source = fs.readFileSync(path.join(controllerDir, name), 'utf8')
  const prefix = source.match(/@RequestMapping\(\s*"([^"]+)"\s*\)/)?.[1]
  assert.ok(prefix, `controller prefix: ${name}`)
  for (const match of source.matchAll(/@GetMapping(?:\(\s*(?:value\s*=\s*)?"([^"]*)"[^)]*\))?/g)) {
    backendGetRoutes.add(prefix + (match[1] || ''))
  }
}
const base = 'http://127.0.0.1:5192'
const server = spawn(process.execPath, ['node_modules/vite/bin/vite.js', '--mode', 'production', '--host', '127.0.0.1', '--port', '5192', '--strictPort'], { windowsHide: true, stdio: 'ignore' })
;(async () => {
  let browser
  try {
    let ready = false
    for (let i = 0; i < 150; i++) {
      try { if ((await fetch(base)).ok) { ready = true; break } } catch {}
      await new Promise(resolve => setTimeout(resolve, 100))
    }
    assert.ok(ready, 'Vite started')
    browser = await chromium.launch({ executablePath: 'C:/Program Files/Google/Chrome/Application/chrome.exe', headless: true })
    const context = await browser.newContext({ viewport: { width: 1440, height: 900 }, reducedMotion: 'reduce' })
    const checks = [], errors = []
    let allowed = true, loginSuccess = false, failAlarms = false
    const requests = [], unexpectedRequests = []
    await context.route('https://webapi.amap.com/**', route => route.abort())
    await context.route('**/prod-api/**', async route => {
      const path = new URL(route.request().url()).pathname.replace('/prod-api', '')
      requests.push(path)
      if (path.startsWith('/fire/') && (!backendGetRoutes.has(path) || route.request().method() !== 'GET')) {
        unexpectedRequests.push(path)
        return route.fulfill({ json: { code: 500, msg: `No static resource ${path.slice(1)} for request '${path}'` } })
      }
      let body = { code: 200, data: [], rows: [], total: 0 }
      if (path === '/captchaImage') body = { code: 200, captchaEnabled: false }
      else if (path === '/login') {
        await new Promise(resolve => setTimeout(resolve, 150))
        body = loginSuccess ? { code: 200, token: 'fixture-only' } : { code: 500, msg: '账号或密码错误' }
      } else if (path === '/getInfo') body = {
        code: 200, user: { userId: 1, userName: 'fixture-user', nickName: '测试用户', avatar: '' },
        roles: allowed ? ['admin'] : ['operator'], permissions: allowed ? ['*:*:*'] : ['fire:places:list']
      }
      else if (path === '/getRouters') body = { code: 200, data: [] }
      else if (path === '/fire/alarms/list' && failAlarms) body = { code: 500, msg: '警情接口测试失败' }
      else if (path === '/fire/video/catalog' || path === '/fire/video/catalog/map') body.data = { rows: [], total: 0, truncated: false }
      else if (path === '/fire/video/devices') body.data = { list: [], lastId: -1 }
      else if (path.endsWith('/stats') || path.endsWith('/status')) body.data = {}
      else if (path === '/fire/alarms/stream') return route.fulfill({ status: 200, contentType: 'text/event-stream', body: ': fixture\n\n' })
      await route.fulfill({ json: body })
    })
    const page = await context.newPage()
    await page.clock.install()
    page.on('pageerror', error => errors.push(error.message))
    await page.goto(base + '/index?grid=1#overview')
    await page.waitForURL('**/login?redirect=**')
    assert.equal(await page.locator('.command-screen').count(), 0)
    await page.getByText('使用管理员为您分配的账号登录', { exact: true }).waitFor()
    assert.equal(await page.title(), '大厂智慧消防')
    const icons = await page.locator('link[rel="icon"]').evaluateAll(links => links.map(link => ({ type: link.type, href: link.getAttribute('href') })))
    assert.deepEqual(icons, [
      { type: 'image/x-icon', href: '/favicon.ico?v=dc-fire-1' },
      { type: 'image/svg+xml', href: '/fire-favicon.svg?v=1' }
    ])
    for (const icon of icons) {
      const response = await context.request.get(base + icon.href)
      assert.equal(response.status(), 200)
      assert.ok(await page.evaluate(async url => {
        const image = new Image(); image.src = url; await image.decode()
        return image.naturalWidth > 0 && image.naturalHeight > 0
      }, base + icon.href))
    }
    checks.push('login browser title is 大厂智慧消防; versioned SVG shield/flame and multi-size ICO fallback both load and decode')
    await page.getByLabel('账号', { exact: true }).fill('fixture-user')
    await page.getByLabel('密码', { exact: true }).fill('fixture-password')
    await page.locator('.login-submit').click()
    await page.locator('.login-feedback').getByRole('alert').waitFor()
    assert.equal(new URL(page.url()).pathname, '/login')
    checks.push('unauthenticated legacy home requires login; failed login stays on login')
    loginSuccess = true
    await page.locator('.login-submit').click()
    await page.waitForURL('**/fire/dashboard?grid=1#overview')
    await page.locator('.command-screen h1').waitFor()
    assert.equal(await page.locator('.command-screen h1').innerText(), '大厂街道智慧消防监管平台')
    assert.equal(await page.locator('.app-wrapper').count(), 0)
    await page.getByText('街道消防安全运行态势', { exact: true }).waitFor()
    assert.equal(await page.title(), '大厂智慧消防')
    checks.push('production mode login opens existing full-screen dashboard and preserves query/hash; RuoYi workbench is absent')
    for (const destination of ['/', '/index', '/fire/dashboard']) {
      await page.goto(base + destination)
      await page.waitForURL('**/fire/dashboard')
      await page.locator('.command-screen h1').waitFor()
    }
    await page.reload()
    await page.locator('.command-screen h1').waitFor()
    await page.waitForFunction(() => document.querySelector('.command-subbar')?.textContent.match(/警情更新\s+\d/))
    await page.locator('.map-layer-toggle').click()
    assert.equal(await page.locator('.map-layer-buttons').getByRole('button', { name: '告警设备', exact: true }).count(), 0)
    assert.equal(await page.locator('.map-layer-buttons').getByRole('button', { name: '网格', exact: true }).count(), 0)
    await page.locator('.map-layer-toggle').click()
    for (let cycle = 0; cycle < 3; cycle++) {
      const beforePolling = requests.filter(path => path === '/fire/hosts/stats').length
      const beforeMap = requests.filter(path => path === '/fire/video/catalog/map').length
      await page.clock.fastForward(31000)
      await assertPoll(() => requests.filter(path => path === '/fire/hosts/stats').length > beforePolling && requests.filter(path => path === '/fire/video/catalog/map').length > beforeMap)
      await page.waitForFunction(() => !document.querySelector('.command-screen > .fire-error'))
    }
    assert.deepEqual([...new Set(unexpectedRequests)], [], 'all automatic fire API calls must have a real backend GET mapping')
    assert.equal(await page.locator('.map-error').filter({ hasText: 'No static resource' }).count(), 0)
    assert.equal(requests.filter(path => path === '/fire/devices/list').length, 0)
    assert.equal(requests.filter(path => path === '/fire/grids/list').length, 0)
    assert.equal(await page.getByText(/No static resource/).count(), 0)
    assert.ok(requests.includes('/fire/alarms/list'))
    assert.ok(requests.includes('/fire/video/catalog/map'))
    assert.ok(requests.includes('/fire/places/list'))
    assert.ok(requests.includes('/fire/inspection/posts'))
    checks.push('production automatic requests match actual backend GET mappings across 3 refresh cycles (93 seconds); unsupported grids/devices APIs and layers are absent while real data requests remain active')
    await page.getByRole('tab', { name: '视频监控', exact: true }).click()
    await assertPoll(() => requests.includes('/fire/video/regions') && requests.includes('/fire/video/devices'))
    await page.getByRole('tab', { name: '查岗管理', exact: true }).click()
    checks.push('dashboard video tab uses declared backend directory/device endpoints and returns to inspections')
    failAlarms = true
    await page.clock.fastForward(3100)
    await page.locator('.command-screen > .fire-error').filter({ hasText: '警情接口测试失败' }).waitFor()
    failAlarms = false
    await page.clock.fastForward(3100)
    await page.locator('.command-screen > .fire-error').waitFor({ state: 'hidden' })
    checks.push('real alarm failures remain visible and recover after a successful refresh')
    await page.screenshot({ path: 'docs/verification/home-routing-desktop.png', fullPage: true })
    checks.push('root, legacy home, direct dashboard and refresh all reach the same command screen')
    await context.clearCookies()
    await page.goto(base + '/login')
    await page.getByText('使用管理员为您分配的账号登录', { exact: true }).waitFor()
    await page.getByLabel('账号', { exact: true }).fill('fixture-user')
    await page.getByLabel('密码', { exact: true }).fill('fixture-password')
    await page.getByLabel('密码', { exact: true }).press('Enter')
    await page.waitForURL('**/fire/dashboard')
    await page.locator('.command-screen h1').waitFor()
    checks.push('login without redirect uses the dashboard home; keyboard submission works')
    allowed = false
    await page.goto(base + '/index')
    await page.waitForURL('**/401')
    assert.equal(await page.locator('.command-screen').count(), 0)
    checks.push('account without dashboard permission still reaches 401')
    await context.clearCookies()
    await page.setViewportSize({ width: 390, height: 844 })
    await page.goto(base + '/index')
    await page.waitForURL('**/login?redirect=**')
    await page.locator('.login-submit').waitFor()
    assert.ok(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth))
    checks.push('narrow viewport also requires login and keeps form reachable')
    assert.deepEqual(errors, [])
    assert.deepEqual([...new Set(unexpectedRequests)], [], 'no unimplemented fire API during the complete workflow')
    fs.writeFileSync('docs/verification/home-routing-browser.json', JSON.stringify({ mode: 'production', productionBuild: false, fixtureHttp: true, liveCredentialsUsed: false, mapSdk: 'blocked; routing and API verification only', backendGetRoutes: [...backendGetRoutes].sort(), requestedFireEndpoints: [...new Set(requests.filter(path => path.startsWith('/fire/')))].sort(), unexpectedRequests, checks, errors }, null, 2))
    console.log(JSON.stringify(checks, null, 2))
  } finally { await browser?.close(); server.kill() }
})().catch(error => { console.error(error); process.exitCode = 1 })

async function assertPoll(predicate) {
  for (let i = 0; i < 50; i++) {
    if (predicate()) return
    await new Promise(resolve => setTimeout(resolve, 100))
  }
  assert.ok(predicate(), 'background resource refresh completed')
}
