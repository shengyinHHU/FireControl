// Actual Vue components and controlled HTTP/navigation. Never contacts the real dial/SMS platform.
const { chromium } = require(process.env.PLAYWRIGHT_PATH || 'playwright')
const { spawn } = require('node:child_process')
const assert = require('node:assert/strict'), fs = require('node:fs')
const base = 'http://127.0.0.1:5207', output = '../outputs/contact-dial'
const server = spawn(process.execPath, ['node_modules/vite/bin/vite.js', '--mode', 'firemock', '--host', '127.0.0.1', '--port', '5207', '--strictPort'], { windowsHide: true, stdio: 'ignore' })
;(async () => {
  let browser
  const checks = [], errors = [], destinations = []
  let previewCalls = 0, smsSends = 0
  try {
    for (let i = 0; i < 100; i++) { try { if ((await fetch(base)).ok) break } catch {} await new Promise(r => setTimeout(r, 100)) }
    browser = await chromium.launch({ executablePath: 'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe', headless: true })
    const context = await browser.newContext({ viewport: { width: 1440, height: 1000 }, reducedMotion: 'reduce' })
    await context.addCookies([{ name: 'Admin-Token', value: 'fixture-only', url: base }])
    const place = { placeId: '1', name: '拨号验证场所', placeType: 'elderly' }
    const contacts = [
      { contactId: '11', contactName: '测试张先生', phone: '13800000001' },
      { contactId: '12', contactName: '国家码联系人', phone: '+86 139-0000-0002' },
      { contactId: '13', contactName: '未填电话联系人', phone: '' },
      { contactId: '14', contactName: '多号码联系人', phone: '13800000001;13900000002' },
      { contactId: '15', contactName: '座机联系人', phone: '025-12345678' },
    ]
    await context.route('**/dev-api/**', async route => {
      const request = route.request(), p = new URL(request.url()).pathname.replace('/dev-api', '')
      let body = { code: 200, data: [], rows: [], total: 0 }
      if (p === '/getInfo') body = { code: 200, user: { userId: 1, userName: 'fixture', nickName: '测试管理员', avatar: '' }, roles: ['admin'], permissions: ['*:*:*'] }
      else if (p === '/getRouters') body = { code: 200, data: [] }
      else if (p === '/fire/places/list') body = { code: 200, rows: [place], total: 1 }
      else if (p === '/fire/places/1') body = { code: 200, data: place }
      else if (p === '/fire/places/1/contacts') body = { code: 200, data: contacts }
      else if (p === '/fire/inspection/posts') body = { code: 200, rows: [{ postId: '21', postName: '拨号验证值守岗', contactName: '值守联系人', contactPhone: '13700000003', status: '0', cameraCount: 1 }], total: 1 }
      else if (p === '/fire/sms/recipient') body = { code: 200, data: { type: 'placeContact', id: '11', name: contacts[0].contactName, rawPhone: contacts[0].phone, phones: [contacts[0].phone], context: place.name, signature: '【大厂街道办事处】', maxContentLength: 1000, enabled: true } }
      else if (p === '/fire/sms/send') smsSends++
      if (p.includes('/preview')) previewCalls++
      await route.fulfill({ json: body })
    })
    await context.route('https://www.jnrhtx.top/**', async route => {
      destinations.push(route.request().url())
      await route.fulfill({ contentType: 'text/html', body: '<!doctype html><title>Dial navigation fixture</title>' })
    })
    const page = await context.newPage(); page.setDefaultTimeout(8000); page.on('pageerror', e => errors.push(e.message))
    async function openPlace() {
      await page.goto(base + '/fire/places', { waitUntil: 'domcontentloaded' })
      await page.getByRole('row').filter({ hasText: place.name }).getByRole('button', { name: '详情', exact: true }).click()
      const dialog = page.getByRole('dialog', { name: place.name, exact: true })
      await dialog.getByText(contacts[0].contactName, { exact: true }).waitFor()
      return dialog
    }
    function action(dialog, name) { return dialog.locator('.contact-actions').filter({ has: dialog.page().locator(`.contact-dial[title^="${name}："]`) }).locator('.contact-dial') }
    async function openDial(trigger, expectedPhone) {
      const originalUrl = page.url()
      const [popup] = await Promise.all([page.waitForEvent('popup'), trigger()])
      await popup.waitForLoadState('domcontentloaded')
      assert.equal(popup.url(), 'https://www.jnrhtx.top/meeting/index?from=Dc&phone=' + expectedPhone)
      assert.equal(await popup.evaluate(() => window.opener), null)
      assert.equal(page.url(), originalUrl, 'dialing must preserve the current page')
      await popup.close()
    }
    let dialog = await openPlace()
    assert.equal(await action(dialog, contacts[0].contactName).evaluate(el => el.classList.contains('is-disabled')), false, 'a persisted phone must reach the shared dial component')
    fs.mkdirSync(output, { recursive: true })
    await page.screenshot({ path: output + '/desktop.png', fullPage: true })
    await openDial(() => action(dialog, contacts[0].contactName).click(), '13800000001')
    assert.equal(await dialog.isVisible(), true)
    checks.push('place dialing opens a separate tab with no opener and preserves the original page and dialog')
    await openDial(() => action(dialog, contacts[1].contactName).click(), '13900000002')
    checks.push('country code and separators normalize without reusing the previous contact phone')
    assert.equal(await action(dialog, contacts[2].contactName).evaluate(el => el.classList.contains('is-disabled')), true)
    for (const contact of [contacts[3], contacts[4]]) {
      await action(dialog, contact.contactName).click()
      await page.getByText('请配置有效的手机号码，不能将多个号码合并拨打', { exact: true }).first().waitFor()
      assert.ok(page.url().startsWith(base)); assert.equal(destinations.length, 2)
      assert.equal(await action(dialog, contact.contactName).evaluate(el => el.classList.contains('is-disabled')), false)
    }
    await dialog.getByRole('button', { name: '测试张先生的发送短信', exact: true }).click()
    const composer = page.getByRole('dialog', { name: '发送短信', exact: true })
    await composer.getByRole('textbox', { name: '短信正文', exact: true }).waitFor()
    assert.equal(smsSends, 0); assert.equal(destinations.length, 2)
    checks.push('missing phone disables dialing; multiple numbers/landlines recover with feedback; SMS still opens independently')
    await page.goto(base + '/fire/inspections', { waitUntil: 'domcontentloaded' })
    const inspection = page.getByRole('region', { name: '真实查岗管理' })
    const dial = inspection.locator('.contact-dial').first(); await dial.waitFor()
    assert.equal(await dial.evaluate(el => el.classList.contains('is-disabled')), false)
    await page.setViewportSize({ width: 390, height: 844 })
    await dial.focus(); await page.screenshot({ path: output + '/narrow.png', fullPage: true })
    await openDial(() => dial.press('Enter'), '13700000003')
    assert.equal(previewCalls, 0); assert.equal(smsSends, 0)
    checks.push('inspection reuses the fix, keyboard activation works at 390px and dialing does not start video')
    assert.deepEqual(errors, [])
    fs.writeFileSync(output + '/browser.json', JSON.stringify({ actualVue: true, fixtureHttp: true, externalNavigationStubbed: true, realCallsMade: false, checks, destinations, errors }, null, 2))
    console.log(checks.join('\n'))
  } finally { await browser?.close(); server.kill() }
})().catch(e => { console.error(e); process.exitCode = 1 })
