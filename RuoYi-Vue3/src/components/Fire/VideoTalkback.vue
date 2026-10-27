<template>
  <div v-if="canVideo('talk')" class="video-talkback">
    <div class="talk-tools">
      <el-button v-if="!running" :disabled="!ready" @click="start">
        <el-icon><Microphone /></el-icon>开始对讲
      </el-button>
      <el-button v-else type="danger" plain @click="stop">
        <el-icon><Close /></el-icon>{{ transmitting ? '结束对讲' : '取消对讲' }}
      </el-button>
      <span v-if="running" role="status">{{ phaseText }}</span>
    </div>
    <details v-if="running || diagnostics.frames" class="talk-diagnostics">
      <summary>对讲诊断</summary>
      <p>输入音量：{{ diagnostics.power || 0 }}/100（峰值 {{ diagnostics.peakPower || 0 }}） · 已发送 {{ diagnostics.frames || 0 }} 包 / {{ diagnostics.bytes || 0 }} 字节</p>
      <p>网关连接：{{ diagnostics.gatewayConnected ? '已连接' : '等待' }} · 开始确认：{{ diagnostics.startAck ? '已确认' : '等待' }} · 设备通道：{{ diagnostics.deviceStatus || '未返回' }}</p>
    </details>
    <p v-if="message" class="fire-error" role="alert">{{ message }}</p>
    <p v-if="transmitting" class="muted">麦克风已开启，请佩戴耳机；设备声音通过直播收听{{ hasAudio ? '。' : '，当前直播暂无音轨，仅能喊话。' }}</p>
  </div>
</template>
<script setup>
import { computed, ref, watch, onBeforeUnmount } from 'vue'
import { Microphone, Close } from '@element-plus/icons-vue'
import { canVideo } from '@/api/fire/video'
import { createTalkback } from '@/api/fire/talkback'
const props = defineProps({ open: Boolean, deviceCode: String, ready: Boolean, hasAudio: Boolean })
const emit = defineEmits(['starting'])
const phase = ref('stopped'), message = ref(''), diagnostics = ref({})
const running = computed(() => phase.value !== 'stopped')
const transmitting = computed(() => phase.value === 'sending')
const phaseText = computed(() => ({ permission: '等待麦克风授权', ticket: '正在申请对讲', connecting: '正在连接语音网关', connected: '通道已连接，等待开始确认', recording: '正在启动录音', sending: '正在发送语音' })[phase.value] || '')
let call, generation = 0
function stop() { const active = call; call = null; active?.stop(); generation++; phase.value = 'stopped' }
function start() {
  if (!props.open || !props.ready || running.value) return
  stop(); message.value = ''; diagnostics.value = {}
  const current = generation
  emit('starting')
  call = createTalkback(props.deviceCode, {
    onState(value) { if (current === generation) phase.value = value },
    onDiagnostics(value) { if (current === generation) diagnostics.value = value },
    onError(value) { if (current === generation) { message.value = value; phase.value = 'stopped' } },
  })
  call.start()
}
watch(() => [props.open, props.deviceCode], () => { stop(); message.value = '' })
onBeforeUnmount(stop)
defineExpose({ stop })
</script>
<style scoped>
.video-talkback { margin-top: 12px; }
.talk-tools { display: flex; align-items: center; flex-wrap: wrap; gap: 12px; }
.muted { font-size: 12px; }
.talk-diagnostics { margin-top: 8px; font-size: 12px; overflow-wrap: anywhere; }
.talk-diagnostics summary { cursor: pointer; }
</style>
