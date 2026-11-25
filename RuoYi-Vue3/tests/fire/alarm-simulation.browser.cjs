// Real Vue routing/components/audio; controlled backend responses, including uncertain POST outcome.
const { chromium } = require(process.env.PLAYWRIGHT_PATH || 'playwright')
const { spawn } = require('node:child_process')
const assert = require('node:assert/strict'), fs = require('node:fs')
const base = 'http://127.0.0.1:5184'
const server = spawn(process.execPath, ['node_modules/vite/bin/vite.js', '--mode', 'firemock', '--host', '127.0.0.1', '--port', '5184', '--strictPort'], { windowsHide: true, stdio: 'ignore', env: { ...process.env, BROWSER: 'none' } })
;(async () => {
  let browser
  const checks = [], errors = []
  try {
    for (let i = 0; i < 100; i++) { try { if ((await fetch(base)).ok) break } catch {} await new Promise(resolve => setTimeout(resolve, 100)) }
    browser = await chromium.launch({ executablePath: 'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe', headless: true })
    const context = await browser.newContext({ viewport: { width: 1440, height: 1000 }, reducedMotion: 'reduce' })
    await context.addCookies([{ name: 'Admin-Token', value: 'fixture-only', url: base }])
    await context.addInitScript(() => {
      // Fixture only replaces the map vendor surface; use the real map adapter/marker lifecycle.
      class Overlay { constructor(options = {}) { this.options = options } on() {} }
      class MapFixture {
        constructor(container) { this.container = container; this.zoom = 14; this.listeners = {}; window.__map = this }
        add(items) { for (const item of Array.isArray(items) ? items : [items]) if (item?.options?.content instanceof HTMLElement) {
          const node = item.options.content; node.dataset.fixtureCoordinate = item.options.position.join(',')
          Object.assign(node.style, { position: 'absolute', left: '50%', top: '48%', zIndex: String(item.options.zIndex) }); this.container.append(node)
        } }
        remove(items) { for (const item of Array.isArray(items) ? items : [items]) item?.options?.content?.remove?.() }
        on(event, action) { (this.listeners[event] ||= []).push(action); if (event === 'complete') setTimeout(action, 0) }
        setZoom(value) { this.zoom = value; for (const listener of this.listeners.zoomend || []) listener() }
        setZoomAndCenter(value, position) { this.center = position; this.setZoom(value) }
        getZoom() { return this.zoom } setFitView() {} setFeatures() {} destroy() { this.container.replaceChildren() }
      }
      window.AMap = { Map: MapFixture, Marker: Overlay, Polygon: Overlay, Polyline: Overlay, Text: Overlay, Pixel: class {}, InfoWindow: class { close() {} open() {} setContent() {} }, TileLayer: { Traffic: Overlay } }
      const NativeAudio = window.Audio, realFetch = window.fetch.bind(window)
      window.__audios = []; window.__streams = []
      window.Audio = function (...args) { const audio = new NativeAudio(...args); window.__audios.push(audio); return audio }
      window.fetch = (url, options) => {
        if (!String(url).endsWith('/stream')) return realFetch(url, options)
        const encoder = new TextEncoder()
        return Promise.resolve(new Response(new ReadableStream({ start(controller) {
          window.__streams.push(() => controller.enqueue(encoder.encode('event: alarm-change\ndata: refresh\n\n')))
          options.signal.addEventListener('abort', () => { try { controller.close() } catch {} })
        } }), { headers: { 'Content-Type': 'text/event-stream' } }))
      }
    })
    const place = { placeId: 102, name: '大厂演示场所', placeType: 'industry', address: '测试路1号', longitude: 118.75, latitude: 32.22, coordinateSystem: 'GCJ02', locationStatus: 'review' }
    let records = [], requests = [], failOnce = true, handleCalls = 0, emptyPlaces = false, failPlaces = false
    let permissions = ['*:*:*']
    await context.route('**/dev-api/**', async route => {
      const url = new URL(route.request().url()), path = decodeURIComponent(url.pathname.replace('/dev-api', ''))
      let body = { code: 200, data: [], rows: [], total: 0 }
      if (path === '/getInfo') body = { code: 200, user: { userId: 22, userName: 'fixture', nickName: '测试用户', avatar: '' }, roles: ['operator'], permissions }
      else if (path === '/getRouters') body = { code: 200, data: [] }
      else if (path === '/fire/places/list') body = failPlaces ? { code: 500, msg: '测试场所加载失败' } : { code: 200, rows: emptyPlaces ? [] : [place, { ...place, placeId: 103, name: '无坐标场所', longitude: null }], total: emptyPlaces ? 0 : 2 }
      else if (path === '/fire/alarms/simulations' && route.request().method() === 'POST') {
        const input = route.request().postDataJSON(); requests.push(input)
        let record = records.find(row => row.requestId === input.requestId)
        if (!record) {
          record = { ...input, id: 'sim:' + requests.length, code: 'sim:' + requests.length, isSimulation: true, sourceCode: input.source, source: input.source === '119' ? '119警情' : '传输装置', kind: input.source === '119' ? '1' : 'fire', name: '【模拟演示】' + input.description, placeName: place.name, placeId: '102', gatewayName: '模拟传输装置', status: '待核验', lng: 118.75, lat: 32.22, coordinateStatus: 'valid', caseTypeCode: input.source === '119' ? '1' : null, caseTypeName: input.source === '119' ? '火警扑救' : null, caseNatureName: '测试', address: place.address, occurredAt: '2026-11-25 10:30:00', details: '模拟演示，无真实设备协议报文' }
          records.unshift(record)
        }
        await new Promise(resolve => setTimeout(resolve, 350))
        body = failOnce ? { code: 500, msg: '测试提交响应丢失' } : { code: 200, data: { id: record.id, isSimulation: true } }
        failOnce = false
      } else if (path.endsWith('/handle')) {
        handleCalls++
        const record = records.find(row => path.includes('/' + row.id + '/handle'))
        const note = route.request().postDataJSON().note
        if (!note.trim()) body = { code: 500, msg: '请填写处理说明' }
        else { record.simulationHandledAt = '2026-11-25 10:31:00'; record.simulationNote = note; record.simulationOperator = '测试用户'; record.status = '已处理' }
      } else if (path === '/fire/alarms/list') {
        let visible = records.filter(row => !url.searchParams.get('source') || row.sourceCode === url.searchParams.get('source'))
        if (url.searchParams.get('keyword')) visible = visible.filter(row => row.name.includes(url.searchParams.get('keyword')))
        body = { code: 200, rows: visible, total: visible.length }
      } else if (path.startsWith('/fire/alarms/sim:')) body = { code: 200, data: records.find(row => row.id === path.split('/').pop()) }
      else if (path.endsWith('/stats')) body = { code: 200, data: {} }
      await route.fulfill({ json: body }).catch(() => {})
    })
    const page = await context.newPage(); page.on('pageerror', error => errors.push(error.message))
    await page.goto(base + '/fire/alarms?source=119&keyword=无匹配', { waitUntil: 'domcontentloaded' })
    const simulate = page.getByRole('button', { name: '模拟报警', exact: true })
    await simulate.waitFor()
    const mirror = await context.newPage(); mirror.on('pageerror', error => errors.push(error.message))
    await mirror.goto(base + '/fire/dashboard')
    await mirror.locator('.alarm-list .fire-empty').waitFor()
    await mirror.getByRole('button', { name: '开启报警声音', exact: true }).click()
    const adjacent = await simulate.evaluate(el => el.previousElementSibling?.textContent.trim())
    assert.equal(adjacent, '查询')
    await simulate.click()
    const dialog = page.getByRole('dialog', { name: '模拟报警', exact: true })
    const submit = dialog.getByRole('button', { name: '触发模拟报警', exact: true })
    await submit.click()
    await dialog.getByText('请选择演示场所', { exact: true }).waitFor()
    await page.waitForFunction(() => document.activeElement?.getAttribute('role') === 'combobox')
    await dialog.getByText('传输装置', { exact: true }).click()
    const select = dialog.getByRole('combobox')
    await select.click()
    const option = page.getByRole('option', { name: '大厂演示场所', exact: true })
    await option.waitFor()
    const triggerWidth = await select.evaluate(el => el.closest('.el-select').getBoundingClientRect().width)
    const popupWidth = await option.evaluate(el => el.closest('.el-select__popper').getBoundingClientRect().width)
    assert.ok(Math.abs(triggerWidth - popupWidth) <= 1)
    assert.equal(await page.getByRole('option', { name: '无坐标场所', exact: true }).count(), 0)
    await page.keyboard.press('ArrowDown'); await page.keyboard.press('Enter')
    if (!await dialog.getByText('地图位置：', { exact: false }).count()) { await select.click(); await option.click() }
    await dialog.getByRole('textbox', { name: '报警内容' }).fill('设备联动演示 <script>')
    await submit.evaluate(el => { el.click(); el.click() })
    await dialog.getByText('测试提交响应丢失', { exact: false }).waitFor()
    assert.equal(requests.length, 1)
    const firstId = requests[0].requestId
    await submit.click()
    await page.waitForURL('**/fire/dashboard')
    await page.locator('.alarm-light-border').waitFor()
    assert.equal(requests.length, 2); assert.equal(requests[1].requestId, firstId); assert.equal(records.length, 1)
    await page.locator('.alarm-card').filter({ hasText: '【模拟演示】设备联动演示 <script>' }).waitFor()
    await page.waitForFunction(() => window.__audios.some(audio => !audio.paused && audio.currentTime > 0))
    const mapIds = await page.locator('.fire-map').evaluate(el => el.__vueParentComponent.props.alarmRows.map(row => row.id))
    assert.deepEqual(mapIds, ['sim:1'])
    await mirror.locator('.alarm-card').filter({ hasText: '设备联动演示 <script>' }).waitFor({ timeout: 5000 })
    await mirror.locator('.alarm-light-border').waitFor()
    await page.locator('.fire-incident-marker').waitFor()
    assert.equal(await page.locator('.fire-incident-marker').getAttribute('data-fixture-coordinate'), '118.75,32.22')
    checks.push('Button beside 查询; scoped coordinate picker, keyboard selection, required validation, double-click lock and uncertain retry with stable idempotency key')
    checks.push('Record-page navigation starts first-snapshot simulation sound/light; list and map share the marked record and escaped content')
    await page.locator('.alarm-card').first().click()
    assert.deepEqual(await page.evaluate(() => window.__map.center), [118.75, 32.22])
    await page.getByRole('button', { name: '查看 / 处理' }).click()
    const detail = page.getByRole('dialog', { name: '设备事件详情', exact: true })
    await detail.getByText('模拟演示警情', { exact: true }).waitFor()
    const handle = detail.getByRole('button', { name: '确认处理', exact: true })
    assert.equal(await handle.isDisabled(), true)
    await detail.getByRole('textbox', { name: '处理说明' }).fill('演示核验完成')
    await handle.click()
    await page.waitForFunction(() => !document.querySelector('.alarm-light-border'))
    assert.equal(handleCalls, 1)
    assert.equal(await page.locator('.alarm-card').count(), 0)
    assert.equal(await page.locator('.fire-incident-marker').count(), 0)
    assert.equal(await page.evaluate(() => window.__audios.every(audio => audio.paused)), true)
    await mirror.waitForFunction(() => !document.querySelector('.alarm-light-border'), null, { timeout: 5000 })
    assert.equal(await mirror.locator('.alarm-card').count(), 0)
    assert.equal(await mirror.locator('.fire-incident-marker').count(), 0)
    await page.goto(base + '/fire/alarms')
    await page.getByRole('button', { name: '详情', exact: true }).first().click()
    await page.getByRole('dialog').getByText('演示核验完成', { exact: true }).waitFor()
    assert.equal(await page.getByRole('dialog').getByRole('button', { name: '确认处理', exact: true }).count(), 0)
    await page.keyboard.press('Escape')
    checks.push('Normal details require a note; processing saves the result, removes dashboard/map records, stops audio/light and keeps handled history readable')

    failPlaces = true; await simulate.click()
    await dialog.getByText('测试场所加载失败', { exact: false }).waitFor()
    failPlaces = false; emptyPlaces = true
    await dialog.getByRole('button', { name: '重新加载', exact: true }).click()
    await dialog.getByText('暂无可用场所', { exact: false }).waitFor()
    assert.equal(await submit.isDisabled(), true)
    await page.setViewportSize({ width: 390, height: 844 })
    await page.waitForFunction(() => document.querySelectorAll('.el-message').length === 0)
    fs.mkdirSync('../outputs/alarm-annunciator', { recursive: true })
    await page.screenshot({ path: '../outputs/alarm-annunciator/simulation-empty-narrow.png' })
    assert.equal(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth), true)
    await dialog.getByRole('button', { name: '取消', exact: true }).click()
    await page.setViewportSize({ width: 1440, height: 1000 })
    emptyPlaces = false; await simulate.click()
    await dialog.getByText('119警情', { exact: true }).click()
    await select.click(); await option.click()
    await dialog.getByText('生成后进入指挥大屏演示', { exact: true }).click()
    await submit.click(); await dialog.waitFor({ state: 'hidden' })
    assert.equal(records.length, 2)
    assert.equal(records[0].sourceCode, '119')
    await page.getByRole('link', { name: '进入大屏演示 ↗' }).click()
    await page.locator('.alarm-light-border').waitFor()
    await page.locator('.alarm-card').filter({ hasText: '【模拟演示】' }).waitFor()
    await page.locator('.fire-incident-marker').waitFor()
    await page.waitForFunction(() => document.querySelectorAll('.el-message').length === 0)
    await page.screenshot({ path: '../outputs/alarm-annunciator/simulation-dashboard.png' })
    await page.locator('.alarm-card').first().click()
    await page.getByRole('button', { name: '查看 / 处理' }).click()
    const incidentDetail = page.getByRole('dialog', { name: '119警情详情', exact: true })
    await incidentDetail.getByText('模拟演示警情', { exact: true }).waitFor()
    await incidentDetail.getByRole('textbox', { name: '处理说明' }).fill('119模拟演示核查完成')
    await incidentDetail.getByRole('button', { name: '确认处理', exact: true }).click()
    await page.waitForFunction(() => !document.querySelector('.alarm-light-border'))
    assert.equal(await page.locator('.alarm-card').count(), 0)
    checks.push('Place failure retry/empty/narrow states and separate 119 creation/handling with optional dashboard entry work')
    checks.push('An already-open second dashboard receives the simulated record and removes its card/marker/light after processing')
    await mirror.close()

    permissions = ['fire:alarms:list', 'fire:places:list']
    const restricted = await context.newPage(); await restricted.goto(base + '/fire/alarms')
    await restricted.getByRole('button', { name: '查询', exact: true }).waitFor()
    assert.equal(await restricted.getByRole('button', { name: '模拟报警', exact: true }).count(), 0)
    await restricted.close()
    checks.push('Users without fire:alarms:simulate do not see the action')
    assert.deepEqual(errors, [])
    fs.writeFileSync('../outputs/alarm-annunciator/simulation-browser.json', JSON.stringify({ actualVue: true, actualAudioPlayback: true, fixtureHTTP: true, fixtureMapVendor: true, actualMapAdapter: true, checks, errors }, null, 2))
    console.log(checks.join('\n'))
  } finally { await browser?.close(); server.kill() }
})().catch(error => { console.error(error); process.exitCode = 1 })
