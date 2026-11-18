<template>
  <div class="meeting-console">
    <div class="toolbar">
      <button :disabled="busy" @click="run(() => client.changeCameraStatus())">{{ camera ? '关闭摄像头' : '开启摄像头' }}</button>
      <button :disabled="busy" @click="run(() => client.changeScreenShareStatus())">{{ sharing ? '停止共享' : '共享屏幕' }}</button>
      <button :disabled="busy" :class="{ recording }" @click="run(record)">{{ recording ? '■ 停止录制' : '● 开始录制' }}</button>
      <button @click="resume">播放会议声音</button>
      <button @click="refreshDevices">刷新设备</button>
    </div>
    <p role="status">{{ notice }}</p>
    <p v-if="failure" role="alert" class="failure">{{ failure }}</p>
    <div ref="stage" class="video-grid"></div>
    <div class="devices">
      <label v-for="kind in deviceKinds" :key="kind.value">{{ kind.label }}
        <select :disabled="busy" :value="selected[kind.value] || ''" @change="switchDevice(kind.value, $event.target.value)">
          <option value="" disabled>选择设备</option>
          <option v-for="(d, i) in devices.filter(d => d.kind === kind.value)" :key="d.deviceId" :value="d.deviceId">{{ d.label || `${kind.label} ${i + 1}` }}</option>
        </select>
      </label>
    </div>
    <details><summary>邀请参会与媒体流</summary>
      <form @submit.prevent="run(invite)"><input v-model="inviteName" aria-label="受邀姓名" placeholder="联系人姓名" required><input v-model="invitePhone" aria-label="受邀手机号码" placeholder="手机号码" required><button :disabled="busy">呼叫加入会议</button></form>
      <form @submit.prevent="run(() => client.inviteThirdParty(internetIdentity.trim(), client.org))"><input v-model="internetIdentity" aria-label="通信平台用户身份" placeholder="通信平台用户身份" required><button :disabled="busy">邀请平台用户</button></form>
      <form @submit.prevent="run(togglePull)"><input v-model="pullUrl" aria-label="拉流地址" placeholder="视频流地址（https / rtsp / rtmp）" :disabled="pulling" required><button :disabled="busy">{{ pulling ? '停止拉流' : '拉流入会' }}</button></form>
      <form @submit.prevent="run(togglePush)"><input v-model="pushUrl" aria-label="推流地址" placeholder="服务商提供的 RTMP 推流地址" :disabled="pushing" required><button :disabled="busy">{{ pushing ? '停止推流' : '开始推流' }}</button></form>
      <p v-if="pushResult">推流返回：{{ pushResult }}</p>
    </details>
    <p class="hint">手机电话端提供语音；网页参会端可提供视频和屏幕共享。录制由通信服务器保存。</p>
  </div>
</template>
<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { LiveKit } from '@/vendor/contact-livekit/index'
import { createCallId } from '@/utils/fireCallId'
const props = defineProps({ client: { type: Object, required: true } })
const stage = ref(null), devices = ref([]), selected = ref({}), camera = ref(false), sharing = ref(false)
const busy = ref(false), recording = ref(false), notice = ref(''), failure = ref('')
const inviteName = ref(''), invitePhone = ref(''), internetIdentity = ref('')
const pullUrl = ref(''), pushUrl = ref(''), pulling = ref(false), pushing = ref(false), pushResult = ref('')
const pullIdentity = 'stream_' + createCallId()
const deviceKinds = [{ value:'audioinput', label:'麦克风' }, { value:'videoinput', label:'摄像头' }, { value:'audiooutput', label:'扬声器' }]
const nodes = new Map(), listeners = [], sdkListeners = []
let disposed = false
function bounded(promise) {
  return new Promise((resolve, reject) => {
    const timer = setTimeout(() => reject(new Error('服务响应超时，操作结果未知，请勿重复提交')), 20000)
    Promise.resolve(promise).then(resolve, reject).finally(() => clearTimeout(timer))
  })
}
async function run(action) {
  if (busy.value || disposed) return
  busy.value = true; failure.value = ''; notice.value = ''
  try { await bounded(action()); if (!disposed) { sync(); await refreshDevices() } }
  catch (e) { failure.value = e?.message || String(e || '操作失败') }
  finally { busy.value = false }
}
async function record() {
  if (recording.value) { await props.client.stopRecord(); recording.value = false; notice.value = '服务器已停止录制；文件保存位置请以通信服务配置为准。' }
  else { await props.client.startRecord(); recording.value = true; notice.value = '服务器已开始录制会议' }
}
async function invite() {
  const phone = invitePhone.value.replace(/[\s-]/g, '').replace(/^\+86/, '')
  if (!inviteName.value.trim() || !/^1[3-9]\d{9}$/.test(phone)) throw new Error('请填写联系人姓名和有效手机号码')
  await props.client.makeCall(inviteName.value.trim(), phone)
  notice.value = '已提交邀请，等待对方接听'
}
function validUrl(value, protocols) {
  const url = new URL(value)
  if (!protocols.includes(url.protocol)) throw new Error('不支持该媒体地址协议')
  return url.href
}
async function togglePull() {
  if (pulling.value) { await props.client.stopPullStream(pullIdentity); pulling.value = false }
  else { await props.client.pullStream(validUrl(pullUrl.value, ['https:', 'http:', 'rtsp:', 'rtmp:']), pullIdentity); pulling.value = true }
}
async function togglePush() {
  if (pushing.value) { await props.client.stopPushVideoStream(); pushing.value = false }
  else { const result = await props.client.pushVideoStream(validUrl(pushUrl.value, ['rtmp:', 'rtmps:'])); pushing.value = true; pushResult.value = typeof result === 'string' ? result : JSON.stringify(result) }
}
async function refreshDevices() {
  try { devices.value = await navigator.mediaDevices.enumerateDevices() }
  catch (e) { failure.value = e.message || '无法读取设备' }
}
function switchDevice(kind, id) {
  const methods = { audioinput:'changeAudioDevice', videoinput:'changeVideoDevice', audiooutput:'changeOutputDevice' }
  return run(async () => { await props.client[methods[kind]](id); selected.value[kind] = id })
}
async function resume() {
  try { await props.client.room.startAudio(); for (const node of nodes.values()) if (node.media) await node.media.play(); failure.value = '' }
  catch { failure.value = '浏览器阻止播放，请检查声音权限后重试' }
}
function removeNode(key) {
  const node = nodes.get(key)
  if (!node) return
  if (node.track && node.media) node.track.detach(node.media)
  node.box.remove(); nodes.delete(key)
}
function snapshot(video) {
  try {
    if (!video.videoWidth) throw new Error('尚无视频画面')
    const canvas = document.createElement('canvas'); canvas.width = video.videoWidth; canvas.height = video.videoHeight
    canvas.getContext('2d').drawImage(video, 0, 0)
    const link = document.createElement('a'); link.download = `会议截图-${Date.now()}.png`; link.href = canvas.toDataURL('image/png'); link.click()
  } catch (e) { failure.value = e.message || '截图失败' }
}
function sync() {
  const room = props.client.room
  if (disposed || !room || !stage.value) return
  camera.value = room.localParticipant.isCameraEnabled; sharing.value = room.localParticipant.isScreenShareEnabled
  const keep = new Set()
  for (const participant of [room.localParticipant, ...room.remoteParticipants.values()]) {
    const label = participant.isLocal ? '本地调度席' : (participant.name || participant.identity)
    const key = participant.identity + ':participant'; keep.add(key)
    if (!nodes.has(key)) { const box = document.createElement('article'); const title = document.createElement('p'); box.append(title); stage.value.append(box); nodes.set(key, { box, title }) }
    nodes.get(key).title.textContent = `${label} · ${participant.isSpeaking ? '正在发言' : participant.isMicrophoneEnabled ? '麦克风开启' : '已静音'}`
    for (const pub of participant.trackPublications.values()) {
      const track = pub.track
      if (!track || pub.isMuted || (participant.isLocal && track.kind === 'audio')) continue
      const trackKey = participant.identity + ':' + pub.trackSid; keep.add(trackKey)
      if (nodes.get(trackKey)?.track === track) continue
      removeNode(trackKey)
      const box = document.createElement('article'), title = document.createElement('p')
      title.textContent = `${label} · ${pub.source === 'screen_share' ? '屏幕共享' : track.kind === 'video' ? '视频' : '音频'}`
      const media = track.attach(); media.autoplay = true; media.playsInline = true
      if (track.kind === 'video') media.muted = true
      box.append(title, media)
      if (track.kind === 'video') { const button = document.createElement('button'); button.textContent = '保存截图'; button.onclick = () => snapshot(media); box.append(button) }
      else box.hidden = true
      stage.value.append(box); nodes.set(trackKey, { box, media, track })
      media.play().catch(() => { failure.value = '请点击“播放会议声音”允许浏览器播放' })
    }
  }
  for (const key of nodes.keys()) if (!keep.has(key)) removeNode(key)
}
async function finish() {
  const work = []
  if (recording.value) work.push(props.client.stopRecord())
  if (pulling.value) work.push(props.client.stopPullStream(pullIdentity))
  if (pushing.value) work.push(props.client.stopPushVideoStream())
  await Promise.allSettled(work.map(bounded))
}
defineExpose({ finish })
onMounted(() => {
  const room = props.client.room
  const callbacks = {
    startRecording: () => { recording.value = true },
    stopRecording: () => { recording.value = false },
    startPushing: () => { pushing.value = true },
    stopPushing: () => { pushing.value = false },
    stopPulling: () => { pulling.value = false },
    errorPush: event => { failure.value = event.message || '通信服务操作失败' }
  }
  for (const [event, handler] of Object.entries(callbacks)) { props.client.on(event, handler); sdkListeners.push([event, handler]) }
  for (const event of ['ParticipantConnected', 'ParticipantDisconnected', 'TrackSubscribed', 'TrackUnsubscribed', 'TrackMuted', 'TrackUnmuted', 'LocalTrackPublished', 'LocalTrackUnpublished', 'ActiveSpeakersChanged']) {
    const name = LiveKit.RoomEvent[event]; room.on(name, sync); listeners.push([name, sync])
  }
  navigator.mediaDevices.addEventListener('devicechange', refreshDevices)
  sync(); void refreshDevices()
})
onBeforeUnmount(() => {
  disposed = true
  for (const [event, handler] of listeners) props.client.room?.off(event, handler)
  for (const [event, handler] of sdkListeners) props.client.off(event, handler)
  navigator.mediaDevices.removeEventListener('devicechange', refreshDevices)
  for (const key of nodes.keys()) removeNode(key)
})
</script>
<style scoped>
.toolbar,.devices{display:flex;gap:10px;flex-wrap:wrap}.meeting-console button{padding:9px 15px;color:#dcf7ff;background:#153e58;border:1px solid #328ba5;border-radius:6px;cursor:pointer}.meeting-console button:disabled{opacity:.5;cursor:wait}.recording{background:#8b293b!important}.failure{color:#ff9b9b}.video-grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(260px,1fr));gap:12px;margin:16px 0}.video-grid :deep(article){background:#071b2d;border:1px solid #27536b;padding:12px;border-radius:8px}.video-grid :deep(video){width:100%;max-height:420px;background:#020b14}.devices label{display:grid;gap:6px;flex:1;min-width:180px}select,input{background:#081d30;color:#d9f3ff;border:1px solid #28647f;padding:10px;border-radius:5px;min-width:0}details{margin-top:20px}summary{cursor:pointer;color:#58d6ec}form{display:flex;flex-wrap:wrap;gap:10px;margin:12px 0}form input{flex:1}.hint{font-size:12px;color:#93b3c8}
</style>
