// Real dashboard in production mode, with controlled HTTP data (no live cameras).
const { chromium } = require(process.env.PLAYWRIGHT_PATH || 'playwright')
const { spawn } = require('node:child_process')
const fs = require('node:fs'), assert = require('node:assert/strict')
const base = 'http://127.0.0.1:5198', output = '../outputs/dashboard-layout'
const server = spawn(process.execPath, ['node_modules/vite/bin/vite.js', '--mode', 'production', '--host', '127.0.0.1', '--port', '5198', '--strictPort'], { windowsHide: true, stdio: 'ignore' })
const leftKey = 'fire:dashboard:panels:v1:left', rightKey = 'fire:dashboard:panels:v1:right'
const close = (a, b, message) => assert.ok(Math.abs(a - b) < 2, `${message}: ${a} != ${b}`)
;(async () => {
  let browser, activePage
  const checks = [], errors = []
  let previewCalls = 0
  try {
    for (let i = 0; i < 100; i++) { try { if ((await fetch(base)).ok) break } catch {} await new Promise(r => setTimeout(r, 100)) }
    browser = await chromium.launch({ executablePath: 'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe', headless: true })
    const context = await browser.newContext({ viewport: { width: 1440, height: 1000 }, reducedMotion: 'reduce' })
    await context.addCookies([{ name: 'Admin-Token', value: 'fixture-only', url: base }])
    await context.addInitScript(() => {
      const realFetch = window.fetch.bind(window)
      window.fetch = (url, options) => {
        if (!String(url).endsWith('/fire/alarms/stream')) return realFetch(url, options)
        return Promise.resolve(new Response(new ReadableStream({ start(controller) { options.signal.addEventListener('abort', () => controller.close()) } }), { headers: { 'Content-Type': 'text/event-stream' } }))
      }
    })
    await context.route('https://webapi.amap.com/**', route => route.abort())
    const places = Array.from({ length: 40 }, (_, i) => ({ placeId: i + 1, name: `大厂街道重点场所 ${i + 1}`, address: `大厂街道测试路 ${i + 1} 号`, placeType: 'industry', longitude: 118.75, latitude: 32.22, coordinateSystem: 'GCJ02' }))
    const alarms = Array.from({ length: 12 }, (_, i) => ({ id: `event-${i}`, sourceCode: 'device', source: '传输装置', kind: 'fire', name: `传输装置火警上报 ${i + 1}`, placeName: places[i].name, placeId: String(i + 1), lng: 118.75, lat: 32.22, occurredAt: new Date().toISOString().slice(0, 19).replace('T', ' '), status: '待核验', deviceId: 'gateway-1' }))
    await context.route('**/prod-api/**', async route => {
      const p = new URL(route.request().url()).pathname.replace('/prod-api', '')
      let body = { code: 200, data: [], rows: [], total: 0 }
      if (p === '/getInfo') body = { code: 200, user: { userId: 1, userName: 'admin', nickName: '管理员', avatar: '' }, roles: ['admin'], permissions: ['*:*:*'] }
      else if (p === '/getRouters') body = { code: 200, data: [] }
      else if (p === '/fire/gateways/stats') body = { code: 200, data: { gateways: 37, online: 20, gatewayOnline: 20, offline: 17 } }
      else if (p === '/fire/alarms/stats') body = { code: 200, data: { eventTotal: 12, fireEvents: 12, faultEvents: 0 } }
      else if (p === '/fire/alarms/list') body = { code: 200, rows: alarms, total: alarms.length }
      else if (p === '/fire/119/status') body = { code: 200, data: { enabled: false } }
      else if (p === '/fire/places/list') body = { code: 200, rows: places, total: places.length }
      else if (p === '/fire/places/map') body = { code: 200, data: places, truncated: false }
      else if (p === '/fire/video/catalog') body = { code: 200, data: { rows: [], total: 20 } }
      else if (p === '/fire/inspection/posts') body = { code: 200, rows: places.slice(0, 8).map((place, i) => ({ postId: i + 1, postName: `消防值班室 ${i + 1}`, placeName: place.name, status: '0', cameraCount: 2 })), total: 8 }
      else if (p === '/fire/video/devices') body = { code: 200, data: { list: places.slice(0, 20).map((_, i) => ({ deviceCode: `320116010213220012${String(i).padStart(2, '0')}`, deviceName: `IPCamera${i + 1}` })), lastId: '-1' } }
      if (p.includes('/preview')) previewCalls++
      await route.fulfill({ json: body })
    })
    const page = await context.newPage()
    activePage = page
    page.on('pageerror', e => errors.push(e.message))
    const sizes = () => page.locator('.panel-stack-pane').evaluateAll(elements => Object.fromEntries(elements.map(el => [el.dataset.panel, el.getBoundingClientRect().height])))
    const handles = page.getByRole('separator', { name: /^调整/ })
    const leftHandle = page.getByRole('separator', { name: '调整重点场所与视频监控/查岗管理的高度', exact: true })
    const overviewHandle = page.getByRole('separator', { name: '调整数据概览与重点场所的高度', exact: true })
    const rightHandle = page.getByRole('separator', { name: '调整消防警情与传输装置的高度', exact: true })
    const drag = async (handle, dy, cancel = false) => {
      const box = await handle.boundingBox(), x = box.x + box.width / 2, y = box.y + box.height / 2
      await page.mouse.move(x, y); await page.mouse.down(); await page.mouse.move(x, y + dy, { steps: 8 })
      if (cancel) await page.keyboard.press('Escape')
      await page.mouse.up(); await page.waitForTimeout(80)
    }
    const bounded = async () => {
      const violations = await page.locator('.panel-stack-enabled').evaluateAll(columns => columns.flatMap(column => {
        const outer = column.getBoundingClientRect()
        return [...column.querySelectorAll('.panel-stack-pane')].flatMap(pane => {
          const panel = pane.querySelector('.fire-panel'), body = panel.querySelector('.panel-body'), box = pane.getBoundingClientRect(), panelBox = panel.getBoundingClientRect(), bodyBox = body.getBoundingClientRect()
          return box.top < outer.top - 1 || box.bottom > outer.bottom + 1 || panelBox.height > box.height + 1 || bodyBox.bottom > panelBox.bottom + 1 ? [pane.dataset.panel] : []
        })
      }))
      assert.deepEqual(violations, [], 'panels and their contents must remain in the column')
    }
    await page.goto(base + '/fire/dashboard', { waitUntil: 'domcontentloaded' })
    await page.locator('.place-rows button').first().waitFor()
    await page.locator('.inspection-item').first().waitFor()
    await handles.nth(2).waitFor(); await page.waitForTimeout(150)
    await bounded()
    assert.equal(await handles.count(), 3)
    const initial = await sizes()
    const inspectionCards = await page.locator('.inspection-cards').evaluate(el => ({ height: el.clientHeight, scrollHeight: el.scrollHeight, bottom: el.getBoundingClientRect().bottom, ownerBottom: el.closest('.panel-body').getBoundingClientRect().bottom }))
    assert.ok(inspectionCards.height > 0 && inspectionCards.scrollHeight > inspectionCards.height && inspectionCards.bottom <= inspectionCards.ownerBottom + 1, 'inspection tab scrolls within the allocated panel')
    await page.getByRole('tab', { name: '视频监控', exact: true }).click()
    await page.locator('.monitor-device').first().waitFor()
    const videoBefore = await page.locator('.monitor-devices').evaluate(el => el.clientHeight)
    await drag(leftHandle, -120)
    const enlarged = await sizes()
    close(enlarged.monitor, initial.monitor + 120, 'drag expands video')
    close(enlarged.places, initial.places - 120, 'drag reduces places')
    close(enlarged.overview, initial.overview, 'overview stays fixed')
    close(await page.locator('.monitor-devices').evaluate(el => el.clientHeight), videoBefore + 120, 'camera list receives the extra space')
    await bounded()
    const footer = await page.locator('.monitor-footer').boundingBox(), monitor = await page.locator('[data-panel="monitor"]').boundingBox()
    assert.ok(footer.y + footer.height <= monitor.y + monitor.height)
    checks.push('left drag expands video list by 120px while retaining toolbar/footer and reducing only places')
    await drag(overviewHandle, 40)
    const overviewAdjusted = await sizes()
    close(overviewAdjusted.overview, initial.overview + 40, 'overview adjustable')
    close(overviewAdjusted.monitor, enlarged.monitor, 'video unchanged when moving overview boundary')
    await drag(rightHandle, 70)
    const adjusted = await sizes()
    close(adjusted.alarms, initial.alarms + 70, 'right drag expands alarms')
    close(adjusted.gateways, initial.gateways - 70, 'right drag reduces gateways')
    checks.push('all three boundaries independently adjust their adjacent pair')
    const saved = await page.evaluate(keys => keys.map(k => localStorage.getItem(k)), [leftKey, rightKey])
    assert.ok(saved.every(Boolean))
    await page.reload({ waitUntil: 'domcontentloaded' }); await handles.nth(2).waitFor(); await page.waitForTimeout(150)
    const restored = await sizes()
    for (const name of Object.keys(adjusted)) close(restored[name], adjusted[name], `restored ${name}`)
    await drag(leftHandle, 50, true)
    const canceled = await sizes()
    for (const name of Object.keys(restored)) close(canceled[name], restored[name], `Escape cancels ${name}`)
    assert.deepEqual(await page.evaluate(keys => keys.map(k => localStorage.getItem(k)), [leftKey, rightKey]), saved)
    checks.push('reload restores both columns; Escape cancels a drag without storing it')
    await leftHandle.focus(); await page.keyboard.press('ArrowUp')
    let keyboard = await sizes(); close(keyboard.monitor, restored.monitor + 20, 'keyboard step')
    await page.keyboard.press('Shift+ArrowDown')
    keyboard = await sizes(); close(keyboard.monitor, restored.monitor - 30, 'keyboard accelerated step')
    await page.keyboard.press('Home'); keyboard = await sizes(); close(keyboard.places, 150, 'minimum places height')
    await page.keyboard.press('End'); keyboard = await sizes(); close(keyboard.monitor, 240, 'minimum monitor height')
    await rightHandle.focus(); await page.keyboard.press('Home'); close((await sizes()).alarms, 240, 'minimum alarms height')
    await page.keyboard.press('End'); close((await sizes()).gateways, 220, 'minimum gateways height')
    await bounded()
    checks.push('arrow/Shift/Home/End work and minimum heights prevent panels from disappearing')
    await page.getByRole('button', { name: '还原布局', exact: true }).click()
    const reset = await sizes()
    for (const name of Object.keys(initial)) close(reset[name], initial[name], `reset ${name}`)
    assert.deepEqual(await page.evaluate(keys => keys.map(k => localStorage.getItem(k)), [leftKey, rightKey]), [null, null])
    await drag(leftHandle, -130)
    await page.getByRole('tab', { name: '视频监控', exact: true }).click(); await page.locator('.monitor-device').first().waitFor()
    fs.mkdirSync(output, { recursive: true })
    await page.screenshot({ path: output + '/desktop.png' })
    const persisted = await page.evaluate(key => localStorage.getItem(key), leftKey)
    for (const viewport of [{ width: 1920, height: 1080 }, { width: 1440, height: 720 }]) {
      await page.setViewportSize(viewport); await page.waitForTimeout(200); await bounded()
      assert.equal(await page.evaluate(key => localStorage.getItem(key), leftKey), persisted, 'viewport changes do not overwrite stored preference')
      await page.screenshot({ path: `${output}/${viewport.width}x${viewport.height}.png` })
    }
    await page.getByRole('button', { name: '全屏', exact: true }).click()
    await page.waitForFunction(() => !!document.fullscreenElement); await page.waitForTimeout(100); await bounded()
    await page.getByRole('button', { name: '全屏', exact: true }).click()
    await page.waitForFunction(() => !document.fullscreenElement)
    checks.push('reset restores defaults; 1920x1080, 1440x720 and fullscreen keep panels bounded without overwriting preference')
    await page.setViewportSize({ width: 390, height: 844 }); await page.waitForTimeout(200)
    assert.equal(await handles.count(), 0)
    assert.equal(await page.getByRole('button', { name: '还原布局', exact: true }).count(), 0)
    assert.equal(await page.evaluate(key => localStorage.getItem(key), leftKey), persisted)
    assert.ok(await page.evaluate(() => document.documentElement.scrollHeight > innerHeight), 'narrow screen uses natural page scroll')
    await page.locator('.gateways-panel').scrollIntoViewIfNeeded(); await page.screenshot({ path: output + '/narrow.png', fullPage: true })
    await page.setViewportSize({ width: 1440, height: 1000 }); await page.waitForTimeout(150); await bounded()
    await page.evaluate(key => localStorage.setItem(key, '{bad data'), leftKey)
    await page.reload({ waitUntil: 'domcontentloaded' }); await handles.nth(2).waitFor(); await page.waitForTimeout(150)
    close((await sizes()).overview, initial.overview, 'invalid saved layout falls back to defaults')
    await overviewHandle.dblclick(); assert.equal(await page.evaluate(key => localStorage.getItem(key), leftKey), null)
    checks.push('narrow screens naturally scroll and retain desktop preferences; corrupt storage recovers; double click resets a column')
    // Browser privacy settings may block just the layout preference; adjusting still works.
    await page.addInitScript(() => {
      for (const name of ['getItem', 'setItem', 'removeItem']) {
        const original = Storage.prototype[name]
        Storage.prototype[name] = function(key, ...args) {
          if (String(key).startsWith('fire:dashboard:panels:')) throw new DOMException('Storage unavailable', 'SecurityError')
          return original.call(this, key, ...args)
        }
      }
    })
    await page.reload({ waitUntil: 'domcontentloaded' }); await handles.nth(2).waitFor(); await page.waitForTimeout(150)
    const blockedStorageBefore = await sizes()
    await drag(leftHandle, -50)
    close((await sizes()).monitor, blockedStorageBefore.monitor + 50, 'storage failure does not block dragging')
    await page.getByRole('button', { name: '还原布局', exact: true }).click(); await bounded()
    checks.push('blocked browser preference storage leaves dragging and reset usable; inspection tab has bounded internal scrolling')
    assert.equal(previewCalls, 0, 'layout changes must never initiate a camera stream')
    assert.deepEqual(errors, [])
    fs.writeFileSync(output + '/browser.json', JSON.stringify({ actualVue: true, productionMode: true, fixtureHTTP: true, checks, previewCalls, errors }, null, 2))
    console.log(checks.join('\n'))
  } catch (error) {
    fs.mkdirSync(output, { recursive: true })
    await activePage?.screenshot({ path: output + '/failure.png', fullPage: true }).catch(() => {})
    console.error(await activePage?.locator('.panel-stack-pane').evaluateAll(nodes => nodes.map(node => ({ panel: node.dataset.panel, style: node.getAttribute('style'), height: node.clientHeight }))).catch(() => []))
    throw error
  } finally { await browser?.close(); server.kill() }
})().catch(e => { console.error(e); process.exitCode = 1 })
