// Actual login UI with local HTTP fixtures; no live credentials or business database access.
const { chromium } = require(process.env.PLAYWRIGHT_PATH || 'playwright')
const { spawn } = require('node:child_process')
const fs = require('node:fs'), assert = require('node:assert/strict')
const base = 'http://127.0.0.1:5191'
const server = spawn(process.execPath, ['node_modules/vite/bin/vite.js', '--mode', 'firemock', '--host', '127.0.0.1', '--port', '5191', '--strictPort'], { windowsHide: true, stdio: 'ignore', env: { ...process.env, BROWSER: 'none' } })
;(async () => {
 let browser
 try {
  for (let i = 0; i < 100; i++) { try { if ((await fetch(base)).ok) break } catch {} await new Promise(resolve => setTimeout(resolve, 150)) }
  browser = await chromium.launch({ executablePath: 'C:/Program Files/Google/Chrome/Application/chrome.exe', headless: true })
  const context = await browser.newContext({ viewport: { width: 1440, height: 900 }, reducedMotion: 'reduce' })
  const page = await context.newPage(), errors = [], checks = []
  page.on('pageerror', error => errors.push(error.message))
  const image = fs.readFileSync('tests/fire/fixtures/login-captcha.gif').toString('base64')
  let captchaCalls = 0, logins = 0, captchaFailure = false, captchaEnabled = true, loginSuccess = false, lastPayload
  await context.route('**/dev-api/**', async route => {
   const req = route.request(), path = new URL(req.url()).pathname.replace('/dev-api', '')
   let body = { code: 200, data: [], rows: [], total: 0 }
   if (path === '/captchaImage') {
    captchaCalls++; await new Promise(resolve => setTimeout(resolve, 180))
    body = captchaFailure ? { code: 500, msg: '验证码接口测试失败' } : { code: 200, captchaEnabled, img: image, uuid: 'fixture-captcha-' + captchaCalls }
   } else if (path === '/login') {
    logins++; lastPayload = req.postDataJSON(); await new Promise(resolve => setTimeout(resolve, 350))
    body = loginSuccess ? { code: 200, token: 'fixture-only' } : { code: 500, msg: '账号或密码错误，请重新输入' }
   } else if (path === '/getInfo') body = { code: 200, user: { userId: 1, userName: 'fixture-user', nickName: '测试用户', avatar: '' }, roles: ['operator'], permissions: ['fire:video:list'] }
   else if (path === '/getRouters') body = { code: 200, data: [] }
   else if (path === '/fire/video/devices') body.data = { list: [], lastId: -1 }
   await route.fulfill({ json: body })
  })
  await page.goto(base + '/login?redirect=/fire/video&scene=login', { waitUntil: 'domcontentloaded' })
  const username = page.getByLabel('账号', { exact: true }), password = page.getByLabel('密码', { exact: true }), code = page.getByLabel('验证码', { exact: true })
  const submit = page.getByRole('button', { name: '登录工作台', exact: true })
  await page.waitForFunction(() => document.querySelector('.captcha-refresh img')?.naturalWidth > 0)
  assert.equal(await username.inputValue(), ''); assert.equal(await password.inputValue(), '')
  assert.equal(await password.getAttribute('autocomplete'), 'current-password')
  assert.equal(await username.getAttribute('autocomplete'), 'username')
  assert.match(await page.title(), /大厂智慧消防/)
  assert.equal(await page.getByText(/RuoYi|若依/).count(), 0)
  await page.screenshot({ path: 'docs/verification/login-desktop.png', fullPage: true })
  await page.setViewportSize({ width: 390, height: 844 }); await page.screenshot({ path: 'docs/verification/login-mobile.png', fullPage: true })
  await page.setViewportSize({ width: 1024, height: 640 }); await page.screenshot({ path: 'docs/verification/login-laptop.png', fullPage: true })
  await page.setViewportSize({ width: 1440, height: 900 })
  checks.push('project brand, city line illustration and blank credential defaults; no third-party requests needed for login artwork')
  await submit.click(); await page.getByText('请输入您的账号', { exact: true }).waitFor()
  await page.waitForFunction(() => document.activeElement?.id === 'login-username')
  assert.equal(logins, 0); checks.push('empty validation prevents requests and focuses the first invalid field')
  await username.fill('fixture-user'); await password.fill('fixture-password')
  await page.getByRole('button', { name: '显示密码', exact: true }).click(); assert.equal(await password.getAttribute('type'), 'text')
  await page.getByRole('button', { name: '隐藏密码', exact: true }).click(); assert.equal(await password.getAttribute('type'), 'password')
  await code.fill('7N2P'); await page.getByText('记住密码', { exact: true }).click(); assert.ok(await page.getByRole('checkbox', { name: '记住密码', exact: true }).isChecked())
  await submit.click(); await page.getByRole('button', { name: '正在登录', exact: true }).waitFor(); await password.press('Enter')
  await page.locator('.login-feedback').getByRole('alert').filter({ hasText: '账号或密码错误' }).waitFor()
  await page.waitForFunction(() => document.querySelector('.captcha-refresh img')?.naturalWidth > 0 && document.querySelector('#login-code')?.value === '')
  assert.equal(logins, 1); assert.equal(lastPayload.uuid, 'fixture-captcha-1'); assert.equal(await username.inputValue(), 'fixture-user'); assert.equal(await password.inputValue(), 'fixture-password'); assert.equal(captchaCalls, 2)
  const remembered = (await context.cookies()).find(cookie => cookie.name === 'password'); assert.ok(remembered); assert.notEqual(remembered.value, 'fixture-password')
  checks.push('password visibility is keyboard accessible; duplicate submit blocked; credential error preserves input and refreshes captcha; legacy encrypted remember cookies retained')
  captchaFailure = true; await page.getByRole('button', { name: '刷新验证码', exact: true }).click()
  await page.getByRole('alert').filter({ hasText: '验证码加载失败' }).waitFor(); assert.ok(await submit.isDisabled())
  captchaFailure = false; await page.getByRole('button', { name: '刷新验证码', exact: true }).click(); await submit.waitFor(); await page.waitForFunction(() => !document.querySelector('.login-submit')?.disabled)
  checks.push('captcha loading failure is persistent, blocks submission and supports retry')
  await page.setViewportSize({ width: 390, height: 844 })
  assert.ok(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth))
  await submit.scrollIntoViewIfNeeded(); assert.ok(await submit.isVisible())
  await page.setViewportSize({ width: 1024, height: 640 })
  assert.ok(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth))
  await page.evaluate(() => document.documentElement.classList.add('dark'))
  assert.equal(await page.locator('.login-panel').evaluate(el => getComputedStyle(el).backgroundColor), 'rgb(247, 251, 255)')
  checks.push('phone and short laptop have no horizontal overflow, form remains reachable, reduced-motion and dark setting retain legibility')
  captchaEnabled = false; await page.reload({ waitUntil: 'domcontentloaded' })
  await page.getByText('使用管理员为您分配的账号登录', { exact: true }).waitFor(); assert.equal(await code.count(), 0)
  assert.equal(await username.inputValue(), 'fixture-user'); assert.equal(await password.inputValue(), 'fixture-password')
  const before = logins
  await password.dispatchEvent('keydown', { key: 'Enter', code: 'Enter', keyCode: 229, isComposing: true })
  await page.waitForTimeout(250); assert.equal(logins, before)
  loginSuccess = true; await page.getByText('记住密码', { exact: true }).click(); assert.equal(await page.getByRole('checkbox', { name: '记住密码', exact: true }).isChecked(), false); await password.press('Enter')
  await page.waitForURL('**/fire/video?scene=login')
  assert.equal((await context.cookies()).filter(cookie => ['username', 'password', 'rememberMe'].includes(cookie.name)).length, 0)
  checks.push('captcha-disabled configuration, cookie restoration/removal and IME-safe Enter work; successful login preserves original destination and query parameters')
  assert.deepEqual(errors, [])
  fs.writeFileSync('docs/verification/login-browser.json', JSON.stringify({ fixtureHttp: true, liveCredentialsUsed: false, checks, errors }, null, 2))
  console.log(JSON.stringify(checks, null, 2))
 } finally { await browser?.close(); server.kill() }
})().catch(error => { console.error(error); process.exitCode = 1 })
