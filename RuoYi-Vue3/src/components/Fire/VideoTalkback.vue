<template>
  <div v-if="canVideo('talk') || canVideo('talk-config')" class="video-talkback">
    <div class="talk-tools">
      <el-button v-if="!running && canVideo('talk')" :disabled="!deviceCode || bindingOpen" @click="start">
        <el-icon><Microphone /></el-icon>开始对讲
      </el-button>
      <el-button v-if="running" type="danger" plain @click="stop">
        <el-icon><Close /></el-icon>{{ transmitting ? '结束对讲' : '取消对讲' }}
      </el-button>
      <span v-if="running" role="status">{{ phaseText }}</span>
      <el-button v-if="canVideo('talk-config')" :disabled="running || !deviceCode" @click="bindingOpen = true"><el-icon><Setting /></el-icon>连接配置</el-button>
    </div>
    <details v-if="running || diagnostics.frames" class="talk-diagnostics">
      <summary>对讲诊断</summary>
      <p>输入音量：{{ diagnostics.power || 0 }}/100 · 已发送 {{ diagnostics.frames || 0 }} 包 / {{ diagnostics.bytes || 0 }} 字节 · 已接收 {{ diagnostics.received || 0 }} 字节</p>
      <p>SDK 已提交 {{ diagnostics.submitted || 0 }} 字节（每5秒更新）</p>
      <p>发送计数表示浏览器已提交音频；现场是否播出需由现场人员确认。服务器记录 SDK 提交结果。</p>
    </details>
    <p v-if="message" class="fire-error" role="alert">{{ message }}</p>
    <p v-if="transmitting" class="muted">麦克风已开启，请佩戴耳机；摄像机声音通过大华对讲通道收听。</p>
    <DahuaBindingDialog v-model="bindingOpen" :device-code="deviceCode" />
  </div>
</template>
<script setup>
import { computed, ref, watch, onBeforeUnmount } from 'vue'
import { Microphone, Close, Setting } from '@element-plus/icons-vue'
import { canVideo } from '@/api/fire/video'
import { createDahuaTalkback as createTalkback } from '@/api/fire/dahuaTalkback'
import DahuaBindingDialog from './DahuaBindingDialog.vue'
const props = defineProps({ open: Boolean, deviceCode: String, ready: Boolean, hasAudio: Boolean })
const emit = defineEmits(['starting', 'ended'])
const bindingOpen = ref(false)
const phase = ref('stopped'), message = ref(''), diagnostics = ref({})
const running = computed(() => phase.value !== 'stopped')
const transmitting = computed(() => phase.value === 'sending')
const phaseText = computed(() => ({ permission: '等待麦克风授权', ticket: '正在申请对讲', connecting: '正在连接摄像机', sending: '正在双向对讲' })[phase.value] || '')
let call, generation = 0
let listening = true
function toggleListening() { listening = !listening; call?.setListening(listening); return listening }
function stop() { const active = call; call = null; active?.stop(); generation++; phase.value = 'stopped'; emit('ended') }
function start() {
  if (!props.open || !props.deviceCode || running.value) return
  stop(); message.value = ''; diagnostics.value = {}
  const current = generation
  listening = true
  emit('starting')
  call = createTalkback(props.deviceCode, {
    onState(value) { if (current === generation) { phase.value = value; if (value === 'stopped') emit('ended') } },
    onDiagnostics(value) { if (current === generation) diagnostics.value = value },
    onError(value) { if (current === generation) { message.value = value; phase.value = 'stopped' } },
  })
  call.start()
}
watch(() => [props.open, props.deviceCode], () => { stop(); bindingOpen.value = false; message.value = '' })
onBeforeUnmount(stop)
defineExpose({ stop, toggleListening })
</script>
<style scoped>
.video-talkback { margin-top: 12px; }
.talk-tools { display: flex; align-items: center; flex-wrap: wrap; gap: 12px; }
.muted { font-size: 12px; }
.talk-diagnostics { margin-top: 8px; font-size: 12px; overflow-wrap: anywhere; }
.talk-diagnostics summary { cursor: pointer; }
</style>
