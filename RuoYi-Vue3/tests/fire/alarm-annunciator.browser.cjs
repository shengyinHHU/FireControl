// Actual dashboard, controlled HTTP/SSE, and real uploaded WAV playback in Edge.
const { chromium } = require(process.env.PLAYWRIGHT_PATH || 'playwright')
const { spawn } = require('node:child_process')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const base = 'http://127.0.0.1:5182'
const server = spawn(process.execPath, ['node_modules/vite/bin/vite.js', '--mode', 'firemock', '--host', '127.0.0.1', '--port', '5182', '--strictPort'], { windowsHide: true, stdio: 'ignore', env: { ...process.env, BROWSER: 'none' } })
;(async () => {
  let browser
  const checks = [], errors = []
  try {
    for (let i = 0; i < 100; i++) {
      try { if ((await fetch(base)).ok) break } catch {}
      await new Promise(resolve => setTimeout(resolve, 100))
    }
    browser = await chromium.launch({ executablePath: 'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe', headless: true })
    const context = await browser.newContext({ viewport: { width: 1440, height: 1000 } })
    await context.addCookies([{ name: 'Admin-Token', value: 'fixture-only', url: base }])
    await context.addInitScript(() => {
      const NativeAudio = window.Audio, realFetch = window.fetch.bind(window)
      window.__alarmAudios = []; window.__alarmPlays = 0; window.__denyAlarm = true
      window.Audio = function (...args) {
        const audio = new NativeAudio(...args), play = audio.play.bind(audio)
        window.__alarmAudios.push(audio)
        audio.play = () => {
          window.__alarmPlays++
          if (window.__denyAlarm) return Promise.reject(new DOMException('fixture policy rejection', 'NotAllowedError'))
          if (window.__failAlarm) return Promise.reject(new DOMException('fixture playback failure', 'NotSupportedError'))
          return play()
        }
        return audio
      }
      window.fetch = (url, options) => {
        if (!String(url).endsWith('/fire/alarms/stream')) return realFetch(url, options)
        const encoder = new TextEncoder()
        return Promise.resolve(new Response(new ReadableStream({ start(controller) {
          window.__emitAlarm = () => controller.enqueue(encoder.encode('event: alarm-change\ndata: refresh\n\n'))
          options.signal.addEventListener('abort', () => { try { controller.close() } catch {} })
        } }), { headers: { 'Content-Type': 'text/event-stream' } }))
      }
    })
    const device = id => ({ id, sourceCode: 'device', source: '传输装置', kind: 'fire', name: '声光测试' + id, placeName: '测试场所', occurredAt: '2026-11-25 10:00:00', status: '待核验', lng: 118.75, lat: 32.22 })
    const incident = id => ({ ...device(id), sourceCode: '119', source: '119警情', caseTypeCode: '2', kind: '2', caseTypeName: '抢险救援', status: '返队' })
    const history = device('history')
    let rows = [history], fail = false, calls = 0
    await context.route('**/dev-api/**', async route => {
      const p = new URL(route.request().url()).pathname.replace('/dev-api', '')
      let body = { code: 200, data: [], rows: [], total: 0 }
      if (p === '/getInfo') body = { code: 200, user: { userId: 1, userName: 'admin', nickName: '管理员', avatar: '' }, roles: ['admin'], permissions: ['*:*:*'] }
      else if (p === '/getRouters') body = { code: 200, data: [] }
      else if (p === '/fire/alarms/list') { calls++; body = fail ? { code: 500, msg: '测试刷新失败' } : { code: 200, rows, total: rows.length } }
      else if (p.endsWith('/stats')) body = { code: 200, data: {} }
      await route.fulfill({ json: body }).catch(() => {})
    })
    const page = await context.newPage()
    page.on('pageerror', error => errors.push(error.message))
    await page.goto(base + '/fire/dashboard', { waitUntil: 'domcontentloaded' })
    await page.locator('.alarm-card').filter({ hasText: '声光测试history' }).waitFor()
    await page.waitForFunction(() => window.__emitAlarm && window.__alarmAudios.length === 1)
    const signal = page.locator('.alarm-annunciator')
    const light = page.locator('.alarm-light-border')
    const refresh = async () => {
      const before = calls
      await page.evaluate(() => window.__emitAlarm())
      await page.waitForFunction(() => !document.querySelector('.fire-error'))
      while (calls === before) await page.waitForTimeout(20)
      await page.waitForTimeout(100)
    }
    assert.equal(await light.count(), 0)
    assert.equal(await page.evaluate(() => window.__alarmPlays), 0)
    const decoded = await page.evaluate(async () => {
      const ctx = new AudioContext()
      try {
        const bytes = await (await fetch(window.__alarmAudios[0].src)).arrayBuffer()
        const byteLength = bytes.byteLength
        const buffer = await ctx.decodeAudioData(bytes)
        return { duration: buffer.duration, sampleRate: buffer.sampleRate, bytes: byteLength }
      } finally { await ctx.close() }
    })
    assert.equal(decoded.bytes, 3214178)
    assert.ok(decoded.duration > 1)
    checks.push('Initial history stays silent; original WAV fetches and decodes in Edge')

    rows = [device('device-1'), history]; await refresh()
    await signal.getByText('浏览器已拦截报警声音，请点击“开启报警声音”').waitFor()
    assert.equal(await light.count(), 1)
    assert.equal(await light.evaluate(el => getComputedStyle(el).pointerEvents), 'none')
    await page.evaluate(() => { window.__denyAlarm = false })
    await signal.getByRole('button', { name: '开启报警声音' }).click()
    await page.waitForFunction(() => !window.__alarmAudios[0].paused && window.__alarmAudios[0].currentTime > 0)
    assert.equal(await page.evaluate(() => window.__alarmAudios[0].loop), true)
    assert.equal(await signal.locator('.alarm-sound-error').count(), 0)
    const plays = await page.evaluate(() => window.__alarmPlays)
    await refresh()
    assert.equal(await page.evaluate(() => window.__alarmPlays), plays)
    checks.push('Device report starts red edges; autoplay rejection stays visible and explicit enable plays the real looping WAV; polling does not replay')

    await signal.getByRole('button', { name: '停止本次报警' }).focus()
    await page.keyboard.press('Enter')
    await page.waitForFunction(() => window.__alarmAudios[0].paused)
    assert.equal(await light.count(), 0)
    await refresh(); assert.equal(await light.count(), 0)
    await page.locator('.source-tabs button').filter({ hasText: '传输装置' }).click()
    rows = [incident('119-rescue'), ...rows]; await refresh()
    await light.waitFor()
    await page.waitForFunction(() => !window.__alarmAudios[0].paused)
    assert.match(await signal.innerText(), /新警情报警中（1 条）/)
    assert.equal(await page.locator('.alarm-card').filter({ hasText: '119-rescue' }).count(), 0)
    await page.locator('.source-tabs button').filter({ hasText: '全部' }).click()
    checks.push('Keyboard stop silences only this batch; a later 119 rescue incident triggers both channels')

    fail = true; await page.evaluate(() => window.__emitAlarm())
    await page.locator('.fire-error').filter({ hasText: '测试刷新失败' }).waitFor()
    assert.equal(await light.count(), 1)
    assert.equal(await page.evaluate(() => window.__alarmAudios[0].paused), false)
    fail = false; await refresh()
    await signal.getByRole('button', { name: '关闭报警声音' }).click()
    assert.equal(await light.count(), 1)
    assert.equal(await page.evaluate(() => window.__alarmAudios[0].paused), true)
    await page.evaluate(() => { window.__failAlarm = true })
    await signal.getByRole('button', { name: '开启报警声音' }).click()
    await signal.getByText('报警声音播放失败，请点击“开启报警声音”重试').waitFor()
    await page.evaluate(() => { window.__failAlarm = false })
    await signal.getByRole('button', { name: '开启报警声音' }).click()
    await page.waitForFunction(() => !window.__alarmAudios[0].paused)
    checks.push('Refresh failure retains the active alarm; mute preserves light; playback failure exposes retry and recovers')

    await page.waitForFunction(() => document.querySelectorAll('.el-message').length === 0)
    const edge = await light.evaluate(el => {
      const animation = el.getAnimations()[0]
      animation.pause(); animation.currentTime = 600
      return { animation: getComputedStyle(el).animationName, opacity: getComputedStyle(el).opacity }
    })
    assert.match(edge.animation, /^alarm-border-pulse/)
    await page.waitForTimeout(100)
    fs.mkdirSync('../outputs/alarm-annunciator', { recursive: true })
    await page.screenshot({ path: '../outputs/alarm-annunciator/desktop.png' })
    await page.emulateMedia({ reducedMotion: 'reduce' })
    assert.equal(await light.evaluate(el => getComputedStyle(el).animationName), 'none')
    assert.equal(await light.evaluate(el => getComputedStyle(el).opacity), '1')
    await page.setViewportSize({ width: 390, height: 844 })
    assert.ok(await page.locator('.command-title h1').evaluate(el => el.offsetHeight < 50), 'narrow title must remain readable')
    await page.screenshot({ path: '../outputs/alarm-annunciator/narrow.png' })
    assert.equal(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth), true)
    await signal.getByRole('button', { name: '停止本次报警' }).click()
    const stopCalls = await page.evaluate(() => window.__alarmPlays)
    rows = [...rows, { ...device('fault'), kind: 'fault' }, { ...device('outside'), lng: 118.777719, lat: 32.265056 }]
    await refresh()
    assert.equal(await light.count(), 0)
    assert.equal(await page.evaluate(() => window.__alarmPlays), stopCalls)
    rows = [{ ...device('manual'), kind: 'manual_alarm' }, ...rows]; await refresh()
    await light.waitFor()
    rows = [history]; await refresh()
    assert.equal(await light.count(), 0)
    assert.equal(await page.evaluate(() => window.__alarmAudios[0].paused), true)
    checks.push('Desktop pulse, steady reduced-motion border, narrow layout, fault/outside exclusion, manual alarm and resolved-batch teardown verified')

    await signal.getByRole('button', { name: '关闭报警声音' }).click()
    await signal.getByRole('button', { name: '开启报警声音' }).click()
    await page.waitForFunction(() => !window.__alarmAudios[0].paused)
    await page.waitForFunction(() => window.__alarmAudios[0].paused, { timeout: 3000 })
    rows = [device('leaving'), history]; await refresh(); await light.waitFor()
    await page.waitForFunction(() => !window.__alarmAudios[0].paused)
    await page.getByRole('link', { name: '管理工作台 ↗' }).click()
    await page.waitForFunction(() => window.__alarmAudios[0].paused && !window.__alarmAudios[0].getAttribute('src'))
    checks.push('Idle enable previews only 1.5 seconds; navigating away releases active media')
    assert.deepEqual(errors, [])
    fs.writeFileSync('../outputs/alarm-annunciator/browser.json', JSON.stringify({ actualVue: true, fixtureHTTP: true, fixtureSSE: true, actualWavPlayback: true, decoded, checks, errors }, null, 2))
    console.log(checks.join('\n'))
  } finally { await browser?.close(); server.kill() }
})().catch(error => { console.error(error); process.exitCode = 1 })
