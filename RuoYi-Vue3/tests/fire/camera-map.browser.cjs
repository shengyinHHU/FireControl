// Real Vue + adapter DOM; controlled HTTP and map-SDK fixtures, no live vendor calls.
const { chromium } = require(process.env.PLAYWRIGHT_PATH || 'playwright')
const { spawn } = require('node:child_process')
const fs = require('node:fs'), assert = require('node:assert/strict')
const base = 'http://127.0.0.1:5199', output = '../outputs/camera-map'
const server = spawn(process.execPath, ['node_modules/vite/bin/vite.js', '--mode', 'production', '--host', '127.0.0.1', '--port', '5199', '--strictPort'], { windowsHide: true, stdio: 'ignore' })
;(async () => {
  let browser, page, release
  const ready = new Promise(resolve => release = resolve), checks = [], errors = []
  try {
    for (let i = 0; i < 100; i++) { try { if ((await fetch(base)).ok) break } catch {} await new Promise(r => setTimeout(r, 100)) }
    browser = await chromium.launch({ executablePath: 'C:/Program Files/Google/Chrome/Application/chrome.exe', headless: true })
    const context = await browser.newContext({ viewport: { width: 1440, height: 1000 }, reducedMotion: 'reduce' })
    await context.addCookies([{ name: 'Admin-Token', value: 'fixture-only', url: base }])
    await context.addInitScript(() => {
      class Overlay { constructor(options = {}) { this.options = options } on() {} }
      class MapFixture {
        constructor(container) { this.container = container; this.zoom = 14; this.listeners = {}; window.__map = this }
        add(items) {
          for (const item of Array.isArray(items) ? items : [items]) if (item?.options?.content instanceof HTMLElement) {
            const node = item.options.content
            node.dataset.fixtureCoordinate = item.options.position.join(',')
            node.dataset.fixtureAnchor = item.options.anchor
            Object.assign(node.style, { position: 'absolute', left: item.options.position[0] > 118.755 ? '59%' : '47%', top: '48%', transform: 'translate(-50%,-50%)', zIndex: String(item.options.zIndex) })
            this.container.append(node)
          }
        }
        remove(items) { for (const item of Array.isArray(items) ? items : [items]) item?.options?.content?.remove?.() }
        on(event, action) { (this.listeners[event] ||= []).push(action); if (event === 'complete') setTimeout(action, 0) }
        setZoom(value) { this.zoom = value; for (const listener of this.listeners.zoomend || []) listener() }
        setZoomAndCenter(value) { this.setZoom(value) }
        getZoom() { return this.zoom } setFitView() {} setFeatures() {} destroy() { this.container.replaceChildren() }
      }
      window.AMap = { Map: MapFixture, Marker: Overlay, Polygon: Overlay, Polyline: Overlay, Text: Overlay, Pixel: class {}, InfoWindow: class { close() {} open() {} setContent() {} }, TileLayer: { Traffic: Overlay } }
    })
    const camera = { id: '1', name: '大厂街道消防值班室东侧出入口超长名称摄像头 IPCamera15', deviceName: 'IPCamera15', deviceCode: '32011601021322001282', directoryPath: '大厂街道 / 消防值班室', installationLocation: '东侧出入口', locationStatus: 'review', lng: 118.753, lat: 32.229 }
    const cameras = [camera, { ...camera, id: '2', name: '消防工作站摄像头 IPCamera20', deviceName: 'IPCamera20', deviceCode: '32011601021322001287', lng: 118.758 }, { ...camera, id: 'invalid', lng: null, lat: null }]
    let previewCalls = 0, cameraFailure = false, empty = false, first = true
    await context.route('**/prod-api/**', async route => {
      const path = new URL(route.request().url()).pathname.replace('/prod-api', '')
      let body = { code: 200, data: [], rows: [], total: 0 }
      if (path === '/getInfo') body = { code: 200, user: { userId: 1, userName: 'admin', nickName: '管理员', avatar: '' }, roles: ['admin'], permissions: ['*:*:*'] }
      else if (path === '/getRouters') body.data = []
      else if (path === '/fire/video/catalog/map') {
        if (first) await ready
        body = cameraFailure ? { code: 500, msg: '监控点位暂时不可用' } : { code: 200, data: { rows: empty ? [] : cameras, truncated: false } }
      }
      else if (path === '/fire/video/catalog') body.data = { rows: cameras, total: 3 }
      else if (path === '/fire/video/devices') body.data = { list: [], lastId: -1 }
      else if (path.endsWith('/stats') || path.endsWith('/status')) body.data = {}
      else if (path.endsWith('/preview')) { previewCalls++; body = { code: 500, msg: '受控播放授权失败' } }
      else if (path === '/fire/alarms/stream') return route.fulfill({ status: 200, contentType: 'text/event-stream', body: ': fixture\n\n' })
      await route.fulfill({ json: body })
    })
    page = await context.newPage(); page.on('pageerror', error => errors.push(error.message))
    await page.goto(base + '/fire/dashboard', { waitUntil: 'domcontentloaded' })
    const panel = page.locator('.map-layer-buttons'), toggle = panel.getByRole('button', { name: '摄像头', exact: true })
    const markers = page.locator('.fire-map-camera-marker'), labels = markers.locator('.fire-map-camera-name')
    const openLayers = async () => { await page.locator('.map-layer-toggle').click(); await toggle.waitFor() }
    await openLayers(); await toggle.click()
    await panel.getByText('正在加载监控点位…', { exact: true }).waitFor()
    first = false; release(); await markers.first().waitFor(); assert.equal(await markers.count(), 2)
    await page.mouse.move(800, 150)
    for (const marker of await markers.all()) {
      const box = await marker.boundingBox(); assert.equal(box.width, 38); assert.equal(box.height, 38)
      assert.equal(await marker.locator('svg').count(), 1)
      assert.equal(await marker.locator('.fire-map-camera-name').isVisible(), false)
    }
    assert.equal(await markers.first().getAttribute('data-fixture-coordinate'), '118.753,32.229')
    assert.equal(await markers.first().getAttribute('data-fixture-anchor'), 'center')
    assert.equal(previewCalls, 0)
    checks.push('overview uses 38px camera icons at original coordinates, invalid coordinates excluded, names hidden, no automatic preview')
    fs.mkdirSync(output, { recursive: true }); await page.screenshot({ path: output + '/overview.png' })
    await markers.first().hover(); assert.equal(await labels.first().isVisible(), true)
    await page.mouse.move(800, 150); await markers.first().focus()
    await page.keyboard.press('Tab'); await page.keyboard.press('Shift+Tab')
    assert.equal(await labels.first().isVisible(), true)
    assert.match(await markers.first().getAttribute('aria-label'), /摄像头：大厂街道.*点击查看监控点位/)
    await page.keyboard.press('Enter')
    const info = page.getByRole('dialog', { name: '监控点位', exact: true })
    await info.getByText(camera.deviceCode, { exact: true }).waitFor()
    await info.getByText(camera.name, { exact: true }).waitFor()
    await info.getByRole('button', { name: '关闭', exact: true }).click(); await info.waitFor({ state: 'hidden' })
    assert.equal(previewCalls, 0)
    checks.push('hover/focus reveals full accessible name; Enter opens existing metadata dialog without starting video')
    for (let i = 0; i < 3; i++) await page.getByRole('button', { name: '放大地图', exact: true }).click()
    await page.waitForFunction(() => window.__map.getZoom() === 17)
    for (const label of await labels.all()) assert.equal(await label.isVisible(), true)
    const longLabel = await labels.first().evaluate(el => ({ width: el.getBoundingClientRect().width, scroll: el.scrollWidth, client: el.clientWidth, textOverflow: getComputedStyle(el).textOverflow }))
    assert.ok(longLabel.width <= 192 && longLabel.scroll > longLabel.client); assert.equal(longLabel.textOverflow, 'ellipsis')
    await page.screenshot({ path: output + '/zoomed.png' })
    await markers.first().click(); await info.getByText(camera.name, { exact: true }).waitFor()
    await info.getByRole('button', { name: '查看视频', exact: true }).click()
    const live = page.locator('.live-video-dialog'); await live.getByRole('alert').filter({ hasText: '受控播放授权失败' }).waitFor()
    assert.equal(previewCalls, 1)
    await live.locator('.el-dialog__headerbtn').click(); await live.waitFor({ state: 'hidden' })
    await info.getByRole('button', { name: '关闭', exact: true }).click(); await info.waitFor({ state: 'hidden' })
    await page.getByRole('button', { name: '缩小地图', exact: true }).click(); await page.mouse.move(800, 150)
    assert.equal(await labels.first().isVisible(), false)
    assert.equal(await markers.count(), 2)
    checks.push('zoom 17 shows bounded/ellipsized labels; zooming out hides names; click still reaches canonical video player and its error state')
    for (let i = 0; i < 3; i++) { await toggle.click(); assert.equal(await markers.count(), 0); await toggle.click(); await markers.first().waitFor(); assert.equal(await markers.count(), 2) }
    await page.locator('.map-search input').fill('不存在的摄像头'); await page.waitForFunction(() => document.querySelectorAll('.fire-map-camera-marker').length === 0)
    await page.locator('.map-search input').fill(''); await markers.first().waitFor()
    checks.push('layer toggles and search remove stale markers with no duplicate accumulation')
    await page.setViewportSize({ width: 390, height: 844 }); await page.locator('.map-layer-toggle').click()
    await page.evaluate(() => window.__map.setZoom(17)); await markers.first().scrollIntoViewIfNeeded(); await markers.first().click()
    await info.getByText(camera.deviceCode, { exact: true }).waitFor(); await page.waitForTimeout(350)
    const dialogBox = await info.boundingBox(); assert.ok(dialogBox.x >= 0 && dialogBox.x + dialogBox.width <= 391)
    await page.screenshot({ path: output + '/narrow-dialog.png' })
    await info.getByRole('button', { name: '关闭', exact: true }).click(); await info.waitFor({ state: 'hidden' })
    checks.push('narrow viewport retains icon click and complete metadata dialog')
    await page.setViewportSize({ width: 1440, height: 1000 }); cameraFailure = true
    await page.reload({ waitUntil: 'domcontentloaded' }); await openLayers(); await toggle.click()
    await panel.getByText('监控点位暂时不可用', { exact: true }).waitFor(); assert.equal(await markers.count(), 0)
    cameraFailure = false; await panel.getByRole('button', { name: '重试监控', exact: true }).click(); await markers.first().waitFor()
    empty = true; await page.reload({ waitUntil: 'domcontentloaded' }); await openLayers(); await toggle.click()
    await panel.getByText(/已显示 0 个监控点位/).waitFor(); assert.equal(await markers.count(), 0)
    checks.push('failed catalog displays retry with no fallback; retry restores icons; empty catalog stays empty')
    assert.deepEqual(errors, [])
    fs.writeFileSync(output + '/browser.json', JSON.stringify({ actualVue: true, productionMode: true, fixtureHTTP: true, fixtureMapSdk: true, checks, errors, previewCalls }, null, 2))
    console.log(checks.join('\n'))
  } catch (error) {
    fs.mkdirSync(output, { recursive: true }); await page?.screenshot({ path: output + '/failure.png' }).catch(() => {}); throw error
  } finally { release?.(); await browser?.close(); server.kill() }
})().catch(error => { console.error(error); process.exitCode = 1 })
