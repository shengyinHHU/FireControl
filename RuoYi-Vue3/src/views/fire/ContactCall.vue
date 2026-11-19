<template>
  <main class="call-page"><section>
    <p>大厂街道 · 会议电话 <span v-if="meetingNumber">｜会议号 {{ meetingNumber }}</span></p><h1>{{ contact?.name || '联系人呼叫' }}</h1>
    <h2>{{ contact?.phone }}</h2><p role="status">{{ status }}</p>
    <p v-if="error" role="alert" class="error">{{ error }}</p>
    <MeetingConsole v-if="meetingClient && active" ref="consoleRef" :client="meetingClient" />
    <div v-if="meetingNumber && !active" class="meeting-stage">
      <article><div class="avatar">我</div><h3>本地调度席</h3><p>{{ muted ? '麦克风已关闭' : '麦克风已开启' }}</p></article>
      <article v-for="p in participants" :key="p.identity"><div class="avatar" :class="{ speaking: p.isSpeaking }">☎</div><h3>{{ p.name }}</h3><p>{{ p.isSpeaking ? '正在发言' : '已进入语音会议' }}</p></article>
      <article v-if="!participants.length"><div class="avatar">☎</div><h3>{{ contact?.name }}</h3><p>等待电话端加入</p></article>
    </div>
    <button v-if="meetingNumber && active" :disabled="deviceBusy" @click="toggleMic">{{ muted ? '开启麦克风' : '静音' }}</button>
    <button v-if="active" @click="stop">挂断并退出会议</button>
    <button v-if="audioBlocked" @click="enableAudio">播放通话声音</button>
    <button @click="back">返回平台</button>
    <div ref="audioHost"></div>
  </section></main>
</template>
<script setup>
import { ref, shallowRef, onMounted, onBeforeUnmount } from 'vue'
import MeetingConsole from '@/components/Fire/MeetingConsole.vue'
import { useRouter, useRoute } from 'vue-router'
import { LiveClient } from '@/vendor/contact-livekit/index'
const router = useRouter(), route = useRoute()
const contact = ref(null), status = ref('正在准备'), error = ref(''), active = ref(false), audioBlocked = ref(false), audioHost = ref(null)
const base = 'https://dict.jnrhtx.top:18880'
const meetingNumber = ref(''), participants = ref([]), muted = ref(false), deviceBusy = ref(false)
const meetingClient = shallowRef(null), consoleRef = ref(null)
async function toggleMic() {
  if (deviceBusy.value || !active.value) return
  deviceBusy.value = true
  try {
    await client.room.localParticipant.setMicrophoneEnabled(muted.value)
    muted.value = !client.room.localParticipant.isMicrophoneEnabled
  } catch (e) { error.value = e.message || '麦克风切换失败' }
  finally { deviceBusy.value = false }
}
let client, cancelled = false, roomId, identity, media
const controller = new AbortController()
function check() { if (cancelled) throw new Error('呼叫已取消') }
function deadline(promise, ms = 20000) {
  return new Promise((resolve, reject) => {
    const timer = setTimeout(() => reject(new Error('通信服务响应超时，请返回后重新发起')), ms)
    Promise.resolve(promise).then(resolve, reject).finally(() => clearTimeout(timer))
  })
}
async function json(path, token) {
  const response = await fetch(base + '/prod-rh' + path, { headers: token ? { Authorization: `Bearer ${token}` } : {}, signal: controller.signal })
  if (!response.ok) throw new Error(`认证接口异常：${response.status}`)
  const result = await response.json()
  if (result.code !== 200) throw new Error(result.msg || '通信认证失败')
  return result
}
async function cleanup() {
  media?.getTracks().forEach(t => t.stop())
  if (!client) return
  try { if (roomId) await deadline(client.deleteRoom(identity), 3000) } catch { /* Disconnect even if remote cleanup fails. */ }
  try { await client.leaveRoom() } finally { client.socket?.close(); audioHost.value?.replaceChildren() }
}
async function stop() {
  if (cancelled) return
  cancelled = true; controller.abort(); status.value = '正在结束会议'
  await consoleRef.value?.finish()
  active.value = false; status.value = '呼叫已结束'
  await cleanup()
}
function back() { void stop(); router.back() }
async function enableAudio() {
  try { await client?.room?.startAudio(); for (const a of audioHost.value?.querySelectorAll('audio') || []) await a.play(); audioBlocked.value = false } catch { audioBlocked.value = true }
}
onMounted(async () => {
  const key = 'fire-call:' + String(route.query.ticket || '')
  let raw
  try { raw = sessionStorage.getItem(key); sessionStorage.removeItem(key); contact.value = raw ? JSON.parse(raw) : null } catch { /* Invalid one-use request. */ }
  if (!contact.value || Date.now() - contact.value.createdAt > 60000 || !/^1[3-9]\d{9}$/.test(contact.value.phone)) {
    status.value = '请从联系人旁重新点击拨号'; return
  }
  active.value = true
  try {
    if (!navigator.mediaDevices?.getUserMedia) throw new Error('语音呼叫需要 HTTPS 或 localhost 环境')
    status.value = '请允许使用麦克风'
    media = await navigator.mediaDevices.getUserMedia({audio:true, video:false}); check()
    media.getTracks().forEach(t => t.stop())
    status.value = '正在认证通信身份'
    const auth = await deadline(json('/comm/com/getDCToken')); check()
    if (!auth.token) throw new Error('通信服务未返回登录令牌')
    const info = await deadline(json('/auth/getInfo', auth.token)); check()
    identity = info.user?.phonenumber
    if (!identity) throw new Error('通信账号未配置呼叫身份')
    client = new LiveClient({ userId: identity, org:'default', roomMode:'auto', roomName:`联系人呼叫-${Date.now()}`, baseUrl:base })
    client.on('videoCallRender', item => {
      if (cancelled || item.isLocal) return
      participants.value = participants.value.filter(p => p.identity !== item.identity)
      if (!item.remove) participants.value.push({ identity: item.identity, name: item.name || contact.value.name, isSpeaking: item.isSpeaking })
      if (!item.remove && item.audioTrack) status.value = '电话端音频已接入会议'
      else if (item.remove && !participants.value.length) status.value = '电话端已离开会议'
    })
    status.value = '正在准备会议'
    roomId = await deadline(client.createRoom().then(async id => { roomId = id; if (cancelled) await cleanup(); return id })); check()
    if (!roomId) throw new Error('会议创建失败')
    status.value = '正在连接语音'
    const joined = await deadline(client.joinRoom({roomNum:roomId, cameraStatus:false, microphoneStatus:true, echoCancellation:true, noiseSuppression:true})); check()
    if (!joined || client.room?.state !== 'connected') throw new Error('会议连接未就绪，未发起外呼')
    if (!client.room.localParticipant.isMicrophoneEnabled) throw new Error('会议已连接，但麦克风未开启，未发起外呼。请检查浏览器麦克风权限和输入设备。')
    status.value = '正在提交外呼'
    meetingNumber.value = roomId
    meetingClient.value = client
    await deadline(client.makeCall(contact.value.name, contact.value.phone)); check()
    if (!participants.value.length) status.value = '外呼请求已提交，等待对方接听'
  } catch (e) {
    if (!cancelled) { error.value = e?.message || '通信请求失败'; status.value = '呼叫未完成'; active.value = false }
    cancelled = true; controller.abort(); await cleanup()
  }
})
onBeforeUnmount(() => { void stop() })
</script>
<style scoped>
.call-page{min-height:100vh;display:grid;place-items:center;background:#061629;color:#d7efff;font-family:inherit}.call-page section{width:min(560px,90vw);padding:36px;background:#0c263c;border:1px solid #24627b;border-radius:12px}.call-page h1{font-size:28px}.call-page h2{color:#4ed7ed}.call-page button{margin:16px 12px 0 0;padding:10px 20px;border:1px solid #32b3cc;background:#123e58;color:#e5faff;border-radius:6px;cursor:pointer}.error{color:#ff9a9a}
</style>
<style scoped>
.call-page section { width:min(1000px,90vw); }
.meeting-stage { display:grid; grid-template-columns:repeat(auto-fit,minmax(220px,1fr)); gap:20px; margin:24px 0; }
.meeting-stage article { padding:30px 16px; text-align:center; background:#081b30; border:1px solid #28536c; border-radius:10px; }
.avatar { display:grid; place-items:center; width:76px; height:76px; margin:auto; border-radius:50%; background:#164661; color:#59dcf2; font-size:30px; }
.speaking { box-shadow:0 0 0 4px #31c9a7; }
button:disabled { opacity:.5; }
</style>
