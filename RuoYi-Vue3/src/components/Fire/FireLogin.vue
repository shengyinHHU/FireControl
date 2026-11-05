<template>
  <main class="fire-login fire-theme">
    <header class="login-header">
      <div class="login-brand"><span class="login-brand-mark" aria-hidden="true">消</span><div><strong>大厂智慧消防</strong><span>街道消防监管平台</span></div></div>
      <span class="login-header-caption">DA CHANG <i></i> FIRE SAFETY</span>
    </header>
    <div class="login-layout">
      <section class="login-story" aria-labelledby="login-story-title">
        <div class="login-story-copy"><span class="login-eyebrow"><i></i> 城市安全，从每一处感知开始</span>
          <h2 id="login-story-title">一屏纵览<br /><span>街道消防态势</span></h2>
          <p>汇聚物联感知、视频监控与消防警情，<br class="story-line-break" />连接日常监管和现场处置。</p>
        </div>
        <LoginCity class="login-illustration" />
        <div class="login-story-footer"><span class="story-footer-line"></span><span>感知风险</span><i></i><span>协同监管</span><i></i><span>守护街道</span></div>
      </section>
      <section class="login-panel" aria-labelledby="login-title">
        <div class="login-panel-heading"><span class="login-panel-eyebrow">消防监管工作台</span><h1 id="login-title">欢迎登录</h1><p>请输入您的账号信息，进入工作台。</p></div>
        <el-form ref="loginRef" :model="loginForm" :rules="loginRules" label-position="top" class="login-form" novalidate :disabled="loading" @submit.prevent="handleLogin" @keydown.enter="guardComposition">
          <el-form-item label="账号" prop="username">
            <el-input id="login-username" v-model="loginForm.username" name="username" autocomplete="username" placeholder="请输入账号" size="large" clearable :spellcheck="false"><template #prefix><el-icon><User /></el-icon></template></el-input>
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input id="login-password" v-model="loginForm.password" name="password" :type="passwordVisible ? 'text' : 'password'" autocomplete="current-password" placeholder="请输入密码" size="large">
              <template #prefix><el-icon><Lock /></el-icon></template>
              <template #suffix><button type="button" class="password-visibility" :aria-label="passwordVisible ? '隐藏密码' : '显示密码'" :aria-pressed="passwordVisible" :disabled="loading" @click="passwordVisible = !passwordVisible"><el-icon><Hide v-if="passwordVisible" /><View v-else /></el-icon></button></template>
            </el-input>
          </el-form-item>
          <div class="captcha-stage">
            <el-form-item v-if="captchaEnabled" label="验证码" prop="code">
              <div class="captcha-controls"><el-input id="login-code" v-model="loginForm.code" name="code" autocomplete="off" placeholder="请输入验证码" size="large" :spellcheck="false" />
                <button type="button" class="captcha-refresh" :disabled="captchaLoading || loading" :aria-busy="captchaLoading" aria-label="刷新验证码" title="点击刷新验证码" @click="getCode">
                  <span v-if="captchaLoading" class="captcha-placeholder"><el-icon class="is-loading"><Loading /></el-icon> 加载中</span>
                  <img v-else-if="codeUrl" :src="codeUrl" alt="图形验证码" />
                  <span v-else class="captcha-placeholder"><el-icon><RefreshRight /></el-icon> 点击重试</span>
                </button>
              </div>
            </el-form-item>
            <p v-else class="account-guidance"><el-icon><InfoFilled /></el-icon>使用管理员为您分配的账号登录</p>
          </div>
          <div class="login-options"><el-checkbox v-model="loginForm.rememberMe">记住密码</el-checkbox><span>账号问题请联系管理员</span></div>
          <div class="login-feedback" aria-live="polite"><p v-if="captchaError || loginError" role="alert"><el-icon><WarningFilled /></el-icon>{{ captchaError || loginError }}</p></div>
          <el-button class="login-submit" type="primary" native-type="submit" :loading="loading" :disabled="loading || captchaLoading || Boolean(captchaError)"><span>{{ loading ? '正在登录' : '登录工作台' }}</span><el-icon v-if="!loading"><ArrowRight /></el-icon></el-button>
          <div v-if="register" class="login-register"><router-link to="/register">注册账号</router-link></div>
        </el-form>
        <div class="login-panel-footer"><span></span><p>大厂街道消防监管平台</p><span></span></div>
      </section>
    </div>
    <footer class="login-page-footer"><span>大厂街道 · 智慧消防</span><span>FIRE SAFETY OPERATIONS</span></footer>
  </main>
</template>
<script setup>
import { ref, watch, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { User, Lock, View, Hide, ArrowRight, Loading, RefreshRight, InfoFilled, WarningFilled } from '@element-plus/icons-vue'
import Cookies from 'js-cookie'
import { getCodeImg } from '@/api/login'
import { encrypt, decrypt } from '@/utils/jsencrypt'
import useUserStore from '@/store/modules/user'
import LoginCity from './LoginCity.vue'

const userStore = useUserStore(), route = useRoute(), router = useRouter(), loginRef = ref()
const loginForm = ref({ username: '', password: '', rememberMe: false, code: '', uuid: '' })
const loginRules = { username: [{ required: true, trigger: 'blur', message: '请输入您的账号' }], password: [{ required: true, trigger: 'blur', message: '请输入您的密码' }], code: [{ required: true, trigger: 'blur', message: '请输入验证码' }] }
const codeUrl = ref(''), loading = ref(false), captchaEnabled = ref(true), captchaLoading = ref(false), captchaError = ref(''), loginError = ref(''), passwordVisible = ref(false), register = ref(false), redirect = ref()
let codeVersion = 0, disposed = false, previousTitle = ''
const pageTitle = import.meta.env.VITE_APP_TITLE
watch(() => route.query.redirect, value => { redirect.value = value }, { immediate: true })
function guardComposition(event) { if (event.isComposing || event.keyCode === 229) event.preventDefault() }
async function handleLogin() {
  if (loading.value || captchaLoading.value || captchaError.value) return
  loading.value = true; loginError.value = ''
  let invalid = false
  try {
    const valid = await loginRef.value.validate().catch(() => false)
    if (!valid) { invalid = true; return }
    if (loginForm.value.rememberMe) {
      Cookies.set('username', loginForm.value.username, { expires: 30 })
      Cookies.set('password', encrypt(loginForm.value.password), { expires: 30 })
      Cookies.set('rememberMe', 'true', { expires: 30 })
    } else { Cookies.remove('username'); Cookies.remove('password'); Cookies.remove('rememberMe') }
    await userStore.login(loginForm.value)
    if (disposed) return
    const otherQueryParams = Object.fromEntries(Object.entries(route.query).filter(([key]) => key !== 'redirect'))
    await router.push({ path: redirect.value || '/', query: otherQueryParams })
  } catch (error) {
    if (disposed) return
    loginError.value = error?.message || '登录未成功，请检查账号信息或网络后重试'
    if (captchaEnabled.value) await getCode()
  } finally {
    if (!disposed) { loading.value = false; if (invalid) { await nextTick(); loginRef.value?.$el.querySelector('.is-error input')?.focus() } }
  }
}
async function getCode() {
  const version = ++codeVersion; captchaLoading.value = true; captchaError.value = ''; loginForm.value.code = ''
  try {
    const response = await getCodeImg()
    if (disposed || version !== codeVersion) return
    captchaEnabled.value = response.captchaEnabled === undefined ? true : response.captchaEnabled
    if (captchaEnabled.value) {
      if (!response.img || !response.uuid) throw Error('验证码加载异常，请点击图片区域重试')
      codeUrl.value = 'data:image/gif;base64,' + response.img; loginForm.value.uuid = response.uuid
    }
  } catch {
    if (!disposed && version === codeVersion) { codeUrl.value = ''; loginForm.value.uuid = ''; captchaError.value = '验证码加载失败，请点击验证码区域重试' }
  } finally { if (!disposed && version === codeVersion) captchaLoading.value = false }
}
function getCookie() {
  const username = Cookies.get('username'), password = Cookies.get('password')
  loginForm.value.username = username || ''
  try { loginForm.value.password = password ? decrypt(password) || '' : '' } catch { loginForm.value.password = '' }
  loginForm.value.rememberMe = Cookies.get('rememberMe') === 'true'
}
onMounted(() => { previousTitle = document.title; document.title = pageTitle; getCookie(); getCode() })
onBeforeUnmount(() => { disposed = true; codeVersion++; if (document.title === pageTitle) document.title = previousTitle })
</script>
<style lang="scss" scoped>
.fire-login {
  min-height:100svh; box-sizing:border-box; display:grid; grid-template-rows:auto 1fr auto; gap:30px; padding:32px 5vw 24px; overflow-x:clip;
  color:var(--fire-text); background:radial-gradient(ellipse at 24% 59%, #123c53 0, transparent 50%), var(--fire-bg);
  --login-ink:#18394f; --login-muted:#597487; --login-paper:#f7fbff;
}
.login-header, .login-layout, .login-page-footer { width:100%; max-width:1420px; margin:0 auto; }
.login-header { display:flex; justify-content:space-between; align-items:center; gap:20px; }
.login-brand { display:flex; align-items:center; gap:14px; }
.login-brand-mark { display:grid; place-items:center; width:46px; height:50px; background:#103d53; border:1px solid #28718a; color:var(--fire-cyan); font-size:26px; font-weight:700; border-radius:4px; }
.login-brand strong { display:block; color:#eef8ff; font-size:21px; letter-spacing:2px; line-height:1.4; }
.login-brand div > span { display:block; margin-top:4px; color:#91afc5; font-size:11px; letter-spacing:3px; }
.login-header-caption { display:flex; align-items:center; gap:16px; font-family:Bahnschrift, Arial, sans-serif; font-size:10px; color:#87aabd; letter-spacing:2px; }
.login-header-caption i { width:26px; height:1px; background:#3b6a80; }
.login-layout { display:grid; grid-template-columns:minmax(0, 1fr) 430px; gap:clamp(35px, 6vw, 100px); align-items:center; padding:12px 0 4px; }
.login-story { min-width:0; position:relative; align-self:stretch; display:flex; flex-direction:column; justify-content:center; padding-top:30px; }
.login-story-copy { position:relative; z-index:1; padding-left:12px; }
.login-eyebrow { display:flex; gap:12px; align-items:center; color:#a7c9d9; font-size:12px; letter-spacing:2px; }
.login-eyebrow i { display:block; width:25px; height:2px; background:var(--fire-cyan); }
.login-story h2 { margin:25px 0 20px; font-size:clamp(40px, 4.2vw, 66px); font-weight:700; letter-spacing:2px; line-height:1.26; color:#eef8ff; }
.login-story h2 span { color:#74dce9; }
.login-story p { color:#9cb9ca; font-size:14px; line-height:1.95; letter-spacing:.6px; margin:0; }
.login-illustration { display:block; width:calc(100% + 80px); max-width:860px; margin:-8px -40px -34px; min-height:240px; }
.login-story-footer { display:flex; align-items:center; gap:19px; padding:0 12px 12px; color:#9cb9ca; font-size:12px; letter-spacing:2px; }
.story-footer-line { width:34px; height:1px; background:#47798c; }
.login-story-footer i { height:3px; width:3px; background:#41839b; }
.login-panel {
  position:relative; padding:36px 36px 25px; border:1px solid #e0ebf3; border-radius:12px; color:var(--login-ink); background:var(--login-paper); box-shadow:0 30px 90px #020b1c66, 0 0 0 7px #d2f3ff08; color-scheme:light;
  --el-bg-color:var(--login-paper); --el-bg-color-overlay:#fff; --el-fill-color-blank:#fff; --el-fill-color-light:#edf5f9;
  --el-text-color-primary:#18394f; --el-text-color-regular:#345166; --el-text-color-secondary:#597487; --el-text-color-placeholder:#597487;
  --el-border-color:#cad9e3; --el-border-color-light:#dce6ed; --el-color-primary:#116f88; --el-color-danger:#b64145;
  --el-disabled-bg-color:#eaf0f4; --el-disabled-text-color:#738b9c; --el-disabled-border-color:#d5e1e8;
}
.login-panel::before { content:''; position:absolute; top:-1px; left:36px; width:42px; height:3px; background:var(--fire-cyan); }
.login-panel-eyebrow { color:#5b768a; font-size:11px; letter-spacing:2px; }
.login-panel h1 { margin:13px 0 10px; font-size:29px; letter-spacing:2px; font-weight:650; color:var(--login-ink); }
.login-panel-heading > p { margin:0; color:var(--login-muted); font-size:12px; line-height:1.7; }
.login-form { margin-top:28px; }
.login-form :deep(.el-form-item) { margin-bottom:21px; }
.login-form :deep(.el-form-item__label) { color:#345166; font-size:13px; font-weight:500; padding-bottom:8px; height:auto; line-height:20px; }
.login-form :deep(.el-input__wrapper) { min-height:49px; border-radius:5px; padding:0 14px; background:#fff; box-shadow:0 0 0 1px #cad9e3 inset; }
.login-form :deep(.el-input__wrapper:hover) { box-shadow:0 0 0 1px #7eabbc inset; }
.login-form :deep(.el-input__wrapper.is-focus) { box-shadow:0 0 0 1px #18758f inset, 0 0 0 3px #22b8d719; }
.login-form :deep(.el-input__inner) { height:49px; color:var(--login-ink); font-size:13px; }
.login-form :deep(.el-input__prefix) { margin-right:8px; color:#7294a6; font-size:17px; }
.login-form :deep(.el-form-item__error) { padding-top:4px; font-size:11px; }
.password-visibility { display:grid; place-items:center; width:30px; height:36px; padding:0; border:0; background:transparent; color:#6c899d; cursor:pointer; border-radius:4px; font-size:17px; }
.password-visibility:hover { color:#146f87; background:#edf6fa; }
.password-visibility:disabled { cursor:default; opacity:.5; }
.captcha-stage { min-height:87px; }
.captcha-controls { display:flex; width:100%; gap:12px; }
.captcha-controls > .el-input { flex:1; min-width:0; }
.captcha-refresh { flex:0 0 116px; width:116px; height:49px; border:1px solid #cad9e3; background:#edf5f9; border-radius:5px; padding:0; overflow:hidden; cursor:pointer; }
.captcha-refresh img { display:block; width:100%; height:100%; object-fit:contain; }
.captcha-refresh:hover { border-color:#18758f; }
.captcha-refresh:disabled { cursor:default; }
.captcha-placeholder { display:flex; justify-content:center; align-items:center; gap:5px; color:#526f83; font-size:11px; }
.account-guidance { display:flex; align-items:center; gap:8px; margin:0; padding:15px 12px; font-size:12px; line-height:1.6; color:#526f83; background:#edf4f8; border-radius:4px; }
.account-guidance .el-icon { flex-shrink:0; }
.login-options { display:flex; justify-content:space-between; gap:10px; align-items:center; }
.login-options > span { font-size:11px; color:#597487; }
.login-options :deep(.el-checkbox__label) { font-size:12px; color:#456276; }
.login-options :deep(.el-checkbox__inner) { background:#fff; border-color:#b4c8d6; }
.login-options :deep(.is-checked .el-checkbox__inner) { background:#116f88; border-color:#116f88; }
.login-feedback { min-height:32px; padding:4px 0 7px; }
.login-feedback p { display:flex; gap:6px; align-items:flex-start; font-size:12px; line-height:1.6; color:#b64145; margin:0; }
.login-feedback .el-icon { flex-shrink:0; margin-top:3px; }
.login-submit.el-button { display:flex; justify-content:center; gap:16px; width:100%; height:50px; border:1px solid #0b2742; border-radius:5px; background:#0b2742; color:#f7fbff; font-size:14px; font-weight:600; letter-spacing:2px; }
.login-submit.el-button:hover { background:#154664; border-color:#154664; box-shadow:0 5px 18px #0b27421a; }
.login-submit.el-button:active { background:#06192e; }
.login-submit.el-button:disabled { opacity:.65; cursor:default; }
.login-submit > .el-icon { color:#75ddeb; }
.login-panel :deep(button:focus-visible), .login-panel :deep(input:focus-visible), .login-panel :deep(a:focus-visible), .login-panel :deep(.el-checkbox__original:focus-visible + .el-checkbox__inner) { outline:2px solid #126c85; outline-offset:4px; }
.login-panel-footer { display:flex; align-items:center; justify-content:center; gap:12px; margin-top:25px; }
.login-panel-footer p { color:#597487; font-size:10px; letter-spacing:1px; margin:0; white-space:nowrap; }
.login-panel-footer > span { height:1px; flex:1; background:#dde7ee; }
.login-register { text-align:right; margin-top:14px; font-size:12px; color:#116f88; }
.login-page-footer { display:flex; justify-content:space-between; align-items:center; gap:12px; color:#7394ab; font-size:10px; letter-spacing:2px; line-height:1.8; padding-top:10px; }
.login-page-footer > span:last-child { font-family:Bahnschrift, Arial, sans-serif; font-size:9px; letter-spacing:3px; }
@media(min-width:1600px) { .login-layout { grid-template-columns:minmax(0,1fr) 450px; gap:130px; } .login-panel { padding:42px 42px 30px; } }
@media(max-height:800px) and (min-width:801px) { .fire-login { gap:20px; padding-top:22px; padding-bottom:18px; } .login-story { padding-top:10px; } .login-story h2 { font-size:44px; margin:20px 0 14px; } .login-illustration { height:280px; min-height:200px; margin-bottom:-20px; } .login-panel { padding:26px 30px 22px; } .login-panel h1 { font-size:26px; margin-top:10px; } .login-form { margin-top:20px; } .login-form :deep(.el-form-item) { margin-bottom:17px; } .login-form :deep(.el-input__wrapper), .login-form :deep(.el-input__inner) { min-height:45px; height:45px; } .captcha-refresh { height:45px; } .captcha-stage { min-height:83px; } .login-feedback { min-height:28px; } .login-submit.el-button { height:46px; } .login-panel-footer { margin-top:20px; } }
@media(max-width:1100px) { .fire-login { padding:26px 4vw 20px; } .login-layout { grid-template-columns:minmax(0,1fr) 390px; gap:28px; } .login-story h2 { font-size:42px; } .login-panel { padding:30px 28px 24px; } .login-story-footer { gap:12px; letter-spacing:1px; } .login-eyebrow { letter-spacing:.5px; font-size:11px; } .login-illustration { width:calc(100% + 50px); margin-left:-25px; margin-right:-25px; } }
@media(max-width:800px) { .fire-login { gap:28px; padding:24px 24px 20px; } .login-layout { display:block; max-width:440px; padding:0; } .login-story { display:none; } .login-header-caption { display:none; } .login-header { max-width:440px; } .login-panel { padding:32px 30px 26px; } .login-page-footer { max-width:440px; flex-wrap:wrap; gap:2px; } }
@media(max-width:420px) { .fire-login { padding:20px 16px 16px; gap:24px; } .login-brand strong { font-size:18px; } .login-brand-mark { width:40px; height:44px; font-size:23px; } .login-panel { padding:28px 22px 24px; } .login-panel h1 { font-size:26px; } .login-options { gap:5px; } .login-options > span { font-size:10px; } .captcha-refresh { flex-basis:106px; width:106px; } .login-page-footer { letter-spacing:1px; } }
@media(prefers-reduced-motion:reduce) { .fire-login :deep(*) { scroll-behavior:auto; transition:none !important; animation:none !important; } }
</style>
