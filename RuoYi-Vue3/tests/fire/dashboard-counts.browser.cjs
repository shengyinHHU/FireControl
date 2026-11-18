// Actual production-mode Vue with controlled HTTP; no live database writes.
const { chromium } = require(process.env.PLAYWRIGHT_PATH || 'playwright')
const { spawn } = require('node:child_process')
const fs = require('node:fs'), assert = require('node:assert/strict')
const base = 'http://127.0.0.1:5199', output = '../outputs/dashboard-counts'
const server = spawn(process.execPath, ['node_modules/vite/bin/vite.js', '--mode', 'production', '--host', '127.0.0.1', '--port', '5199', '--strictPort'], { windowsHide: true, stdio: 'ignore' })
;(async () => {
  let browser, release
  const checks = [], errors = [], requests = []
  const loading = new Promise(resolve => { release = resolve })
  let fail = false, hold = true, failSecondPage = false
  const row = (placeId, placeType) => ({ placeId, placeType, name: `真实场所 ${placeId}`, address: '大厂街道', longitude: null, latitude: null })
  let places = [...Array.from({ length: 1001 }, (_, i) => row(i + 1, 'industry')), row(1002, 'highrise'), row(1003, 'commercial'), row(1004, 'government')]
  try {
    for (let i = 0; i < 100; i++) { try { if ((await fetch(base)).ok) break } catch {} await new Promise(r => setTimeout(r, 100)) }
    browser = await chromium.launch({ executablePath: 'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe', headless: true })
    const context = await browser.newContext({ viewport: { width: 1440, height: 1000 }, reducedMotion: 'reduce' })
    await context.addCookies([{ name: 'Admin-Token', value: 'fixture-only', url: base }])
    await context.addInitScript(() => {
      const realFetch = window.fetch.bind(window)
      window.fetch = (url, options) => !String(url).endsWith('/fire/alarms/stream') ? realFetch(url, options) : Promise.resolve(new Response(new ReadableStream({ start(controller) { options.signal.addEventListener('abort', () => controller.close()) } }), { headers: { 'Content-Type': 'text/event-stream' } }))
    })
    await context.route('https://webapi.amap.com/**', route => route.abort())
    await context.route('**/prod-api/**', async route => {
      const url = new URL(route.request().url()), p = url.pathname.replace('/prod-api', '')
      let body = { code: 200, data: [], rows: [], total: 0 }
      if (p === '/getInfo') body = { code: 200, user: { userId: 1, userName: 'admin', nickName: '管理员', avatar: '' }, roles: ['admin'], permissions: ['*:*:*'] }
      else if (p === '/getRouters') body = { code: 200, data: [] }
      else if (p === '/fire/places/list') {
        const page = Number(url.searchParams.get('pageNum') || 1), size = Number(url.searchParams.get('pageSize') || 10)
        requests.push({ page, size })
        if (hold) await loading
        body = fail || (failSecondPage && page === 2) ? { code: 500, msg: '场所读取失败测试' } : { code: 200, rows: places.slice((page - 1) * size, page * size), total: places.length }
      }
      else if (p === '/fire/places/map') body = { code: 200, data: [], truncated: false }
      else if (p === '/fire/video/catalog') body = { code: 200, data: { rows: [], total: 0 } }
      else if (p === '/fire/gateways/stats' || p === '/fire/alarms/stats') body = { code: 200, data: {} }
      else if (p === '/fire/119/status') body = { code: 200, data: { enabled: false } }
      await route.fulfill({ json: body }).catch(() => {})
    })
    const page = await context.newPage()
    page.on('pageerror', e => errors.push(e.message))
    await page.clock.install()
    const counts = () => page.locator('.overview-grid strong').allTextContents()
    const assertCounts = async values => {
      await page.waitForFunction(expected => JSON.stringify([...document.querySelectorAll('.overview-grid strong')].map(el => el.textContent)) === JSON.stringify(expected), values.map(String))
    }
    const refresh = async () => { await page.clock.fastForward(30000) }
    const category = name => page.locator('.overview-grid').getByRole('button', { name, exact: false })
    await page.goto(base + '/fire/dashboard', { waitUntil: 'domcontentloaded' })
    await assertCounts(['—', '—', '—', '—'])
    fail = true; hold = false; release()
    await page.locator('.command-screen > .fire-error').waitFor()
    assert.deepEqual(await counts(), ['—', '—', '—', '—'])
    checks.push('loading and initial failure never display fixed or invented counts')
    fail = false
    await page.locator('.command-screen > .fire-error').getByRole('button', { name: '重试' }).click()
    await assertCounts([1001, 1, 1, 0])
    assert.ok(requests.some(r => r.page === 2 && r.size === 1000), 'load the second page')
    for (const [name, count] of [['重点企业', 1001], ['高层建筑', 1], ['综合商业体', 1], ['养老机构', 0]]) {
      await category(name).click()
      assert.equal(await page.locator('.place-rows button').count(), count)
      assert.equal(await page.locator('.places-panel .tiny').textContent(), `${count} 处`)
      assert.equal(await category(name).getAttribute('aria-pressed'), 'true')
    }
    assert.ok(await page.locator('.place-rows .fire-empty').isVisible())
    checks.push('all four category counts match clicked lists, including zero and unlocated records beyond row 1000')
    await category('养老机构').click()
    assert.equal(await page.locator('.place-rows button').count(), 1004, 'unfiltered list includes government')
    await category('高层建筑').focus(); await page.keyboard.press('Enter')
    assert.equal(await page.locator('.place-rows button').count(), 1)
    await page.setViewportSize({ width: 390, height: 844 })
    await page.screenshot({ path: output + '/narrow.png', fullPage: true })
    checks.push('keyboard category activation and narrow viewport preserve the four counts')
    await page.setViewportSize({ width: 1440, height: 1000 })
    await page.screenshot({ path: output + '/desktop.png', fullPage: true })
    failSecondPage = true
    await refresh()
    await page.locator('.command-screen > .fire-error').waitFor()
    assert.deepEqual(await counts(), ['1001', '1', '1', '0'])
    assert.equal(await page.locator('.place-rows button').count(), 1)
    checks.push('a failed later page retains the entire previous snapshot and matching list')
    failSecondPage = false
    places = [row(2001, 'elderly'), row(2002, 'elderly')]
    await page.locator('.command-screen > .fire-error').getByRole('button', { name: '重试' }).click()
    await assertCounts([0, 0, 0, 2])
    assert.equal(await page.locator('.place-rows button').count(), 0, 'selected category remains applied after refresh')
    await category('养老机构').click()
    assert.equal(await page.locator('.place-rows button').count(), 2)
    places = []
    await refresh()
    await assertCounts([0, 0, 0, 0])
    assert.equal(await page.locator('.place-rows button').count(), 0)
    checks.push('background refresh and empty database update counters and selected list together')
    assert.deepEqual(errors, [])
    fs.writeFileSync(output + '/browser.json', JSON.stringify({ actualVue: true, productionMode: true, fixtureHTTP: true, checks, errors }, null, 2))
    console.log(checks.join('\n'))
  } finally { release(); await browser?.close(); server.kill() }
})().catch(e => { console.error(e); process.exitCode = 1 })
