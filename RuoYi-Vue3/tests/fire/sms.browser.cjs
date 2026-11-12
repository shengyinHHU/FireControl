// Actual Vue UI with explicitly controlled HTTP fixtures. No real recipient or SMS platform is contacted.
const { chromium } = require(process.env.PLAYWRIGHT_PATH || 'playwright')
const { spawn } = require('node:child_process')
const assert = require('node:assert/strict'), fs = require('node:fs')
const base = 'http://127.0.0.1:5197'
const server = spawn(process.execPath, ['node_modules/vite/bin/vite.js', '--mode', 'firemock', '--host', '127.0.0.1', '--port', '5197', '--strictPort'], { windowsHide: true, stdio: 'ignore' })
const checks = [], errors = [], evidence = '../docs/verification'
fs.mkdirSync(evidence, { recursive: true })
;(async () => {
  let browser
  try {
    for (let i = 0; i < 100; i++) { try { if ((await fetch(base)).ok) break } catch {} await new Promise(r => setTimeout(r, 100)) }
    browser = await chromium.launch({ executablePath: 'C:/Program Files/Google/Chrome/Application/chrome.exe', headless: true })
    const context = await browser.newContext({ viewport: { width: 1440, height: 1050 }, reducedMotion: 'reduce' })
    await context.addCookies([{ name: 'Admin-Token', value: 'fixture-only', url: base }])
    let mode = 'success', sendCount = 0, lastPayload, queryCount = 0, recipientMode = 'single', cameraRequests = 0
    const contacts = [{ contactId: '11', contactName: '测试联系人', phone: '13800000001' }, { contactId: '12', contactName: '第二联系人', phone: '13900000002;13700000003' }, { contactId: '13', contactName: '座机联系人', phone: '025-12345678' }]
    const record = { recordId: '100', requestId: 'fixture', recipientName: '测试联系人', mobiles: '13800000001', content: '【大厂街道办事处】通知检查', sourceName: '测试场所', senderName: '测试管理员', taskStatus: 'submitted', deliveryStatus: 'unconfirmed', resultText: '短信已提交平台，送达情况以手机或回执为准', platformTaskId: 'platform-fixture', acceptedPhones: '13800000001', createTime: '2026-11-11 10:00:00' }
    async function fixture(route, permissions = ['*:*:*']) {
      const req = route.request(), u = new URL(req.url()), p = u.pathname.replace('/dev-api', '')
      let response = { code: 200, data: [], rows: [], total: 0 }
      if (p === '/getInfo') response = { code: 200, user: { userId: 1, userName: 'fixture', nickName: '测试管理员', avatar: '' }, roles: ['fixture'], permissions }
      else if (p === '/getRouters') response = { code: 200, data: [] }
      else if (p === '/fire/places/list') response = { code: 200, rows: [{ placeId: '1', name: '测试场所', placeType: 'industry', address: '测试地址' }], total: 1 }
      else if (p === '/fire/places/1') response = { code: 200, data: { placeId: '1', name: '测试场所', placeType: 'industry' } }
      else if (p === '/fire/places/1/contacts') response = { code: 200, data: contacts }
      else if (p === '/fire/sms/recipient') {
        assert.equal(req.method(), 'GET'); const id = u.searchParams.get('id'), type = u.searchParams.get('type')
        assert.ok(['placeContact', 'inspectionPost'].includes(type)); if (type === 'placeContact') assert.equal(u.searchParams.get('parentId'), '1')
        if (recipientMode === 'slow' && id === '11') await new Promise(r => setTimeout(r, 650))
        const found = type === 'inspectionPost' ? { contactName: '值守联系人', phone: '13800000001' } : contacts.find(c => c.contactId === id)
        const phones = found.phone.split(';').filter(x => /^1\d{10}$/.test(x))
        response = { code: 200, data: { type, id, name: found.contactName, rawPhone: found.phone, phones, context: type === 'inspectionPost' ? '测试值守岗' : '测试场所', signature: '【大厂街道办事处】', maxContentLength: 1000, enabled: true } }
      } else if (p === '/fire/sms/send') {
        assert.equal(req.method(), 'POST'); sendCount++; lastPayload = req.postDataJSON()
        assert.match(lastPayload.requestId, /^[0-9a-f-]{36}$/); assert.equal(lastPayload.signature, '【大厂街道办事处】'); assert.equal(lastPayload.expectedPhone, contacts[0].phone)
        assert.equal(lastPayload.auth_key, undefined); assert.equal(lastPayload.sign, undefined)
        await new Promise(r => setTimeout(r, 250))
        if (mode === 'network') { await route.abort('failed'); return }
        response = { code: 200, data: { ...record, requestId: lastPayload.requestId, content: lastPayload.signature + lastPayload.body,
          ...(mode === 'failed' ? { taskStatus: 'failed', resultText: '签名未报备通过', acceptedPhones: '' } : mode === 'rejected' ? { taskStatus: 'rejected', resultText: '联系人号码已更新，请重新打开短信弹窗核对', acceptedPhones: '' } : {}) } }
      } else if (p.startsWith('/fire/sms/requests/')) { queryCount++; response = { code: 200, data: record } }
      else if (p === '/fire/sms/list') response = { code: 200, rows: [record], total: 1 }
      else if (p === '/fire/inspection/posts') response = { code: 200, rows: [{ postId: '21', postName: '测试值守岗', contactName: '值守联系人', contactPhone: '13800000001', status: '0', cameraCount: 1 }], total: 1 }
      else if (p.includes('/preview')) cameraRequests++
      await route.fulfill({ json: response })
    }
    await context.route('**/dev-api/**', route => fixture(route))
    const page = await context.newPage(); page.setDefaultTimeout(10000); page.on('pageerror', e => errors.push(e.stack))
    await page.goto(base + '/fire/places')
    await page.getByRole('row').filter({ hasText: '测试场所' }).getByRole('button', { name: '详情', exact: true }).click()
    const place = page.getByRole('dialog', { name: '测试场所', exact: true }), compose = page.getByRole('dialog', { name: '发送短信', exact: true })
    const openFirst = async () => { await place.getByRole('button', { name: '测试联系人的发送短信', exact: true }).click(); await compose.getByRole('textbox', { name: '短信正文', exact: true }).waitFor() }
    await place.getByText('测试联系人', { exact: true }).waitFor()
    assert.equal(await place.getByRole('link', { name: /的拨号页面/ }).count(), 3)
    assert.equal(await place.getByRole('button', { name: /座机联系人的发送短信/ }).isDisabled(), true)
    await openFirst(); assert.equal(await compose.getByRole('textbox', { name: '短信正文', exact: true }).inputValue(), '')
    await compose.getByRole('button', { name: '发送短信', exact: true }).click(); await compose.getByText('请输入短信正文', { exact: true }).waitFor(); assert.equal(sendCount, 0)
    const body = compose.getByRole('textbox', { name: '短信正文', exact: true })
    await body.fill('请核查消防设施，发现问题及时反馈。')
    assert.equal(await compose.locator('.sms-preview p').textContent(), '【大厂街道办事处】请核查消防设施，发现问题及时反馈。')
    await page.locator('.dialog-fade-enter-active').first().waitFor({ state: 'hidden' })
    await page.screenshot({ path: evidence + '/sms-compose-desktop.png', fullPage: true })
    await compose.getByRole('button', { name: '发送短信', exact: true }).dblclick()
    await compose.getByText('短信已提交平台，送达情况以手机或回执为准', { exact: true }).waitFor()
    assert.equal(sendCount, 1); assert.equal(lastPayload.id, '11'); assert.equal(lastPayload.parentId, '1'); assert.deepEqual(lastPayload.phones, ['13800000001'])
    assert.equal(await body.isDisabled(), true); assert.equal(await compose.getByRole('button', { name: '发送短信', exact: true }).count(), 0)
    await compose.getByRole('button', { name: '关闭', exact: true }).click(); await compose.waitFor({ state: 'hidden' })
    checks.push('phone-adjacent entry, landline disabled, empty validation, raw body/live preview, recipient identity, double-click single submit and success feedback')
    mode = 'failed'; await openFirst(); await body.fill('通知检查'); await compose.getByRole('button', { name: '发送短信', exact: true }).click(); await compose.getByText('签名未报备通过', { exact: true }).waitFor()
    assert.equal(await body.inputValue(), '通知检查'); assert.equal(await body.isDisabled(), false); await page.locator('.dialog-fade-enter-active').first().waitFor({ state: 'hidden' }); await page.screenshot({ path: evidence + '/sms-compose-failure.png', fullPage: true })
    await compose.getByRole('button', { name: '关闭', exact: true }).click(); await compose.waitFor({ state: 'hidden' })
    mode = 'rejected'; await openFirst(); await body.fill('未提交的正文'); await compose.getByRole('button', { name: '发送短信', exact: true }).click(); await compose.getByText('联系人号码已更新，请重新打开短信弹窗核对', { exact: true }).waitFor(); assert.equal(await body.isDisabled(), false)
    await compose.getByRole('button', { name: '关闭', exact: true }).click(); await compose.waitFor({ state: 'hidden' })
    checks.push('provider rejection and safe pre-dispatch validation preserve editable content with clear reasons')
    mode = 'network'; await openFirst(); await body.fill('查询结果测试'); const before = sendCount; await compose.getByRole('button', { name: '发送短信', exact: true }).click()
    await compose.getByRole('button', { name: '查询提交结果', exact: true }).waitFor(); assert.equal(sendCount, before + 1); assert.equal(await body.isDisabled(), true)
    await compose.getByRole('button', { name: '查询提交结果', exact: true }).click(); await compose.getByText('短信已提交平台，送达情况以手机或回执为准', { exact: true }).waitFor(); assert.equal(queryCount, 1); assert.equal(sendCount, before + 1)
    await compose.getByRole('button', { name: '关闭', exact: true }).click(); await compose.waitFor({ state: 'hidden' })
    checks.push('lost HTTP response locks resend and queries the original request without a second send')
    mode = 'success'; await place.getByRole('button', { name: '第二联系人的发送短信', exact: true }).click(); await body.waitFor(); await body.fill('多号码核对')
    await compose.getByRole('button', { name: '发送短信', exact: true }).click(); await compose.getByText('请选择本次接收手机号', { exact: true }).waitFor()
    await compose.locator('label.el-checkbox').filter({ hasText: '13900000002' }).click(); await compose.getByText('本次发送：1 个号码', { exact: true }).waitFor()
    await page.keyboard.press('Escape'); const confirmation = page.getByRole('dialog', { name: '放弃未发送短信' }); await confirmation.getByRole('button', { name: '放弃', exact: true }).click(); await compose.waitFor({ state: 'hidden' })
    recipientMode = 'slow'; await place.getByRole('button', { name: '测试联系人的发送短信', exact: true }).click(); await compose.getByText('正在核对收件人…', { exact: true }).waitFor(); await compose.getByRole('button', { name: '取消', exact: true }).click(); await compose.waitFor({ state: 'hidden' })
    await place.getByRole('button', { name: '第二联系人的发送短信', exact: true }).click(); await compose.getByText('第二联系人', { exact: true }).waitFor(); await page.waitForTimeout(700); assert.equal(await compose.getByText('测试联系人', { exact: true }).count(), 0)
    await compose.getByRole('button', { name: '取消', exact: true }).click(); await compose.waitFor({ state: 'hidden' })
    checks.push('explicit choice for multiple mobiles, unsent draft confirmation, keyboard Escape and stale recipient response ignored')
    await page.goto(base + '/fire/inspections'); await page.getByRole('button', { name: '值守联系人的发送短信', exact: true }).click(); await compose.getByText('测试值守岗', { exact: true }).waitFor(); assert.equal(cameraRequests, 0)
    await body.fill('请核对值守情况'); await page.setViewportSize({ width: 390, height: 844 }); await page.locator('.dialog-fade-enter-active').first().waitFor({ state: 'hidden' }); await page.screenshot({ path: evidence + '/sms-compose-mobile.png', fullPage: true })
    const bounds = await compose.boundingBox(); assert.ok(bounds.width <= 390 && bounds.x >= 0)
    const footer = await compose.getByRole('button', { name: '发送短信', exact: true }).boundingBox(); assert.ok(footer.y + footer.height <= 844)
    await page.keyboard.press('Tab'); checks.push('inspection uses the same dialog without starting video; narrow viewport keeps actions visible')
    await page.goto(base + '/fire/sms'); await page.getByRole('region', { name: '真实短信发送记录' }).getByText('通知检查', { exact: false }).first().waitFor(); assert.equal(await page.getByText('当前业务使用演示数据，操作保存在当前浏览器', { exact: true }).count(), 0); await page.getByText('短信发送记录来自真实数据库；任务提交与最终送达分别展示', { exact: true }).waitFor()
    await page.getByRole('button', { name: '详情', exact: true }).click(); const details = page.getByRole('dialog', { name: '短信发送详情' }); await details.getByText('platform-fixture', { exact: true }).waitFor(); await details.getByRole('button', { name: '关闭', exact: true }).click()
    await page.setViewportSize({ width: 1440, height: 1050 }); await page.screenshot({ path: evidence + '/sms-records-desktop.png', fullPage: true }); checks.push('real records page, source label and full result detail')
    const readOnly = await browser.newContext(); await readOnly.addCookies([{ name: 'Admin-Token', value: 'fixture-only', url: base }]); await readOnly.route('**/dev-api/**', route => fixture(route, ['fire:places:list', 'fire:sms:list']))
    const other = await readOnly.newPage(); await other.goto(base + '/fire/places'); await other.getByRole('row').filter({ hasText: '测试场所' }).getByRole('button', { name: '详情', exact: true }).click(); await other.getByText('测试联系人', { exact: true }).waitFor(); assert.equal(await other.getByRole('button', { name: /的发送短信/ }).count(), 0); await readOnly.close()
    checks.push('read-only role has no real sending control even in firemock mode')
    assert.deepEqual(errors, [])
    fs.writeFileSync(evidence + '/sms-browser.json', JSON.stringify({ actualVue: true, fixtureHTTP: true, liveSmsSent: false, businessDatabaseAccessed: false, checks, errors }, null, 2))
    console.log(checks.join('\n'))
  } finally { await browser?.close(); server.kill() }
})().catch(e => { console.error(e); process.exitCode = 1 })
