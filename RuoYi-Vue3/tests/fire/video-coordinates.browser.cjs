// Actual Vue UI; controlled HTTP/vendor and map-SDK fixtures. Database behavior is verified separately.
const { chromium } = require(process.env.PLAYWRIGHT_PATH || 'playwright')
const { spawn } = require('node:child_process')
const fs = require('node:fs'), assert = require('node:assert/strict')
const base = 'http://127.0.0.1:5179'
const server = spawn(process.execPath, ['node_modules/vite/bin/vite.js', '--mode', 'firemock', '--host', '127.0.0.1', '--port', '5179', '--strictPort'], { windowsHide: true, stdio: 'ignore' })
;(async () => {
 let browser
 try {
  for (let i = 0; i < 100; i++) { try { if ((await fetch(base)).ok) break } catch {} await new Promise(r => setTimeout(r, 100)) }
  browser = await chromium.launch({ executablePath: 'C:/Program Files/Google/Chrome/Application/chrome.exe', headless: true })
  const context = await browser.newContext({ viewport: { width: 1600, height: 1000 }, reducedMotion: 'reduce', acceptDownloads: true })
  await context.addCookies([{ name: 'Admin-Token', value: 'fixture-only', url: base }])
  await context.addInitScript(() => {
   class Overlay { constructor(options = {}) { this.options = options } on() {} }
   class MapFixture {
    constructor(container) { this.container = container; this.zoom = 14 }
    add(items) { for (const item of Array.isArray(items) ? items : [items]) if (item?.options?.content instanceof HTMLElement) { item.options.content.dataset.fixtureCoordinate = item.options.position.join(','); Object.assign(item.options.content.style, { position: 'absolute', left: '50%', top: '50%', zIndex: String(item.options.zIndex) }); this.container.append(item.options.content) } }
    remove(items) { for (const item of Array.isArray(items) ? items : [items]) item?.options?.content?.remove?.() }
    on(event, action) { if (event === 'complete') setTimeout(action, 0) }
    setFitView() {} setFeatures() {} getZoom() { return this.zoom } setZoom(value) { this.zoom = value } setZoomAndCenter() {} destroy() { this.container.replaceChildren() }
   }
   window.AMap = { Map: MapFixture, Marker: Overlay, Polygon: Overlay, Polyline: Overlay, Text: Overlay, Pixel: class {}, InfoWindow: class { close() {} open() {} setContent() {} }, TileLayer: { Traffic: Overlay } }
  })
  const row = { id: '1', source: 'fixture-source', deviceCode: '000012345678901234', name: '真实接口样本监控', deviceName: '真实接口样本监控', directoryId: '2', directoryPath: '主目录 / 子目录', installationLocation: '', remark: '', lng: null, lat: null, locationStatus: 'pending', revision: 0, coordinateTime: null }
  let fail = false, editFail = false, commits = 0, previews = 0, syncCount = 0, running = false, denied = false, lastExport
  const page = await context.newPage(), errors = [], checks = []
  page.on('pageerror', e => errors.push(e.message))
  await context.route('**/dev-api/**', async route => {
   const request = route.request(), url = new URL(request.url()), path = url.pathname.replace('/dev-api', '')
   let body = { code: 200, data: {}, rows: [], total: 0 }
   if (path === '/getInfo') body = { code: 200, user: { userId: 1, userName: 'admin', nickName: '管理员', avatar: '' }, roles: ['admin'], permissions: denied ? ['fire:video:list'] : ['*:*:*'] }
   else if (path === '/getRouters') body.data = []
   else if (path === '/fire/video/catalog/directories') body.data = [{ id: '1', parentId: '', name: '主目录', path: '主目录' }, { id: '2', parentId: '1', name: '子目录', path: '主目录 / 子目录' }]
   else if (path === '/fire/video/catalog/map') body.data = { rows: row.lng === null ? [] : [row], truncated: false }
   else if (path === '/fire/video/catalog' && request.method() === 'GET') {
    if (fail) body = { code: 500, msg: '台账加载测试失败' }
    else {
     if (running && ++syncCount > 1) running = false
     const matches = (!url.searchParams.get('keyword') || row.name.includes(url.searchParams.get('keyword'))) && (url.searchParams.get('location') !== 'pending' || row.lng === null)
     body.data = { rows: matches ? [{ ...row }] : [], total: matches ? 1 : 0, summary: { total: 1, located: row.lng === null ? 0 : 1, pending: row.lng === null ? 1 : 0 }, sync: { state: running ? 'running' : 'complete', message: running ? '正在读取平台目录和全部分页' : '完整同步成功；坐标已保留', directories: 2, devices: 1, lastSuccessAt: '2026-11-02 12:00:00' } }
    }
   } else if (path === '/fire/video/catalog/sync') { running = true; syncCount = 0; body.data = { jobId: 'fixture-job', state: 'running' } }
   else if (path === '/fire/video/catalog/export') { lastExport = request.postData(); await route.fulfill({ body: Buffer.from('fixture-export'), contentType: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' }); return }
   else if (path === '/fire/video/catalog/import/preview') {
    previews++
    assert.match(request.postData(), /name="mode"/)
    body.data = { batchId: 'fixture-batch', state: 'ready', summary: { total: 2, update: 1, updated: 0, skipped: 0, invalid: 1, conflict: 0, unchanged: 0 }, rows: [{ rowNumber: 2, name: row.name, deviceCode: row.deviceCode, oldLongitude: row.lng, oldLatitude: row.lat, longitude: 118.753, latitude: 32.229, status: 'update', reason: '将补充坐标，定位待核验' }, { rowNumber: 3, name: '错误行样本', deviceCode: 'invalid', status: 'invalid', reason: '坐标系未知，请核实' }] }
   } else if (path === '/fire/video/catalog/import/confirm') {
    commits++; row.lng = 118.753; row.lat = 32.229; row.locationStatus = 'review'; row.revision++
    body.data = { batchId: 'fixture-batch', state: 'complete', summary: { total: 2, update: 0, updated: 1, skipped: 0, invalid: 1, conflict: 0, unchanged: 0 }, rows: [{ rowNumber: 2, name: row.name, deviceCode: row.deviceCode, longitude: row.lng, latitude: row.lat, status: 'updated', reason: '已写入数据库' }, { rowNumber: 3, deviceCode: 'invalid', status: 'invalid', reason: '坐标系未知，请核实' }] }
   } else if (path === '/fire/video/catalog/1/coordinates') {
    if (editFail) body = { code: 500, msg: '坐标版本冲突，请重新打开记录' }
    else { const data = request.postDataJSON(); row.lng = Number(data.sourceLongitude); row.lat = Number(data.sourceLatitude); row.locationStatus = data.locationStatus; row.installationLocation = data.installationLocation; row.revision++; body.data = {} }
   } else if (path.endsWith('/preview')) body = { code: 500, msg: '播放授权失败样本' }
   else if (path === '/fire/places/map') body.data = []
   else if (path === '/fire/video/regions') body.data = []
   else if (path === '/fire/video/devices') body.data = { list: [], lastId: -1 }
   await route.fulfill({ json: body })
  })
  await page.goto(base + '/fire/video?tab=coordinates', { waitUntil: 'domcontentloaded' })
  const manager = page.getByRole('region', { name: '监控坐标维护' })
  await manager.getByRole('cell', { name: row.deviceCode, exact: true }).waitFor()
  assert.equal(await manager.getByRole('cell', { name: '未填写', exact: true }).count(), 3)
  checks.push('blank initial coordinates and text device identity shown in real catalog')
  const downloading = page.waitForEvent('download'); await manager.getByRole('button', { name: '导出未定位', exact: true }).click(); await downloading
  assert.match(lastExport, /location=pending/); assert.doesNotMatch(lastExport, /pageNum|pageSize/)
  checks.push('export is server-filtered across all pages and defaults to unlocated')
  await manager.getByRole('button', { name: '同步监控目录', exact: true }).click()
  await manager.getByText('完整同步成功；坐标已保留', { exact: true }).waitFor()
  checks.push('async sync progress and completion displayed')
  await manager.getByRole('button', { name: '导入坐标', exact: true }).click()
  const dialog = page.getByRole('dialog', { name: '导入监控坐标' })
  await dialog.locator('input[type=file]').setInputFiles({ name: '调查.xlsx', mimeType: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', buffer: Buffer.from('HTTP fixture only; parser verified in isolated database integration') })
  await dialog.getByRole('button', { name: '检查文件', exact: true }).click()
  await dialog.getByText('预检结果 · 2 条', { exact: true }).waitFor()
  assert.equal(previews, 1); assert.equal(commits, 0)
  await dialog.getByText('坐标系未知，请核实', { exact: true }).waitFor()
  await dialog.getByRole('button', { name: '确认更新 1 条', exact: true }).click()
  await dialog.getByText('导入完成 · 2 条', { exact: true }).waitFor(); assert.equal(commits, 1)
  await dialog.getByRole('button', { name: '关闭', exact: true }).click()
  await manager.getByRole('cell', { name: '118.753', exact: true }).waitFor()
  checks.push('preview precedes commit; invalid rows and before/after coordinates visible; successful import refreshes list')
  await manager.getByRole('button', { name: '编辑坐标', exact: true }).click()
  const editor = page.getByRole('dialog', { name: '编辑监控坐标' })
  await editor.getByLabel('经度', { exact: true }).fill('118.754')
  editFail = true; await editor.getByRole('button', { name: '保存坐标', exact: true }).click()
  await editor.getByRole('alert').filter({ hasText: '坐标版本冲突' }).waitFor()
  assert.equal(await editor.getByLabel('经度', { exact: true }).inputValue(), '118.754')
  editFail = false; await editor.getByRole('button', { name: '保存坐标', exact: true }).click(); await editor.waitFor({ state: 'hidden' })
  checks.push('single edit preserves entered values after server failure and retries successfully')
  await page.locator('.el-message').waitFor({ state: 'hidden' }).catch(async () => { await page.waitForTimeout(3500) })
  await manager.locator('.el-table__body-wrapper .el-scrollbar__wrap').evaluate(el => el.scrollLeft = 0)
  await page.screenshot({ path: 'docs/verification/video-coordinates-desktop.png', fullPage: true })
  await page.setViewportSize({ width: 720, height: 900 }); await page.keyboard.press('Tab')
  await manager.getByRole('combobox', { name: '定位状态' }).focus(); await page.keyboard.press('ArrowDown'); await page.getByRole('option', { name: '未填写', exact: true }).waitFor(); await page.keyboard.press('Escape'); await page.getByRole('option', { name: '未填写', exact: true }).waitFor({ state: 'hidden' })
  await page.screenshot({ path: 'docs/verification/video-coordinates-narrow.png', fullPage: true })
  assert.ok(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth + 2))
  checks.push('keyboard focus, authored select popup and narrow layout verified')
  fail = true; await manager.getByRole('button', { name: '刷新台账', exact: true }).click(); await manager.getByRole('alert').filter({ hasText: '台账加载测试失败' }).waitFor(); assert.equal(await manager.getByRole('cell', { name: row.deviceCode, exact: true }).count(), 0)
  fail = false; await manager.getByRole('button', { name: '刷新台账', exact: true }).click(); await manager.getByRole('cell', { name: row.deviceCode, exact: true }).waitFor()
  await manager.getByRole('textbox', { name: '搜索监控' }).fill('不存在的监控'); await manager.getByRole('button', { name: '查询', exact: true }).click(); await manager.getByText('没有符合条件的监控；首次使用请同步监控目录', { exact: true }).waitFor()
  checks.push('error and no-result states have no demo fallback')
  await page.setViewportSize({ width: 1600, height: 1000 })
  await page.goto(base + '/fire/dashboard', { waitUntil: 'domcontentloaded' })
  await page.locator('.map-layer-toggle').click(); const layer = page.locator('.map-layer-buttons').getByRole('button', { name: '摄像头', exact: true })
  await layer.click(); const marker = page.locator('[data-fixture-coordinate]').filter({ hasText: row.name }); await marker.waitFor()
  assert.equal(await marker.getAttribute('data-fixture-coordinate'), '118.754,32.229')
  await marker.click(); const info = page.getByRole('dialog', { name: '监控点位', exact: true }); await info.getByText('待核验', { exact: true }).waitFor()
  await info.getByRole('button', { name: '查看视频', exact: true }).click(); await page.getByRole('dialog').filter({ hasText: '播放授权失败样本' }).waitFor()
  checks.push('database camera renders without a place binding; marker opens metadata and canonical authenticated live player')
  assert.deepEqual(errors, [])
  fs.writeFileSync('docs/verification/video-coordinates-browser.json', JSON.stringify({ fixtureHttp: true, fixtureMapSdk: true, checks, errors }, null, 2))
  console.log(JSON.stringify(checks, null, 2))
 } finally { await browser?.close(); server.kill() }
})().catch(e => { console.error(e); process.exitCode = 1 })
