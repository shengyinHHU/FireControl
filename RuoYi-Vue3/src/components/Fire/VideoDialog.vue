<template>
  <el-dialog
    v-model="open"
    :title="camera?.name || '视频监控'"
    width="920px"
    class="fire-dialog"
    append-to-body
    @closed="cleanup"
    ><div v-loading="loading">
      <div v-if="error" class="fire-error">
        {{ error }}<el-button link @click="connect">重试连接</el-button>
      </div>
      <div v-if="session" class="video-stage">
        <canvas
          v-if="session.mock"
          ref="canvas"
          width="960"
          height="480"
          aria-label="演示消控室监控画面"
        /><video
          v-else-if="session.url"
          ref="video"
          :src="session.url"
          controls
          autoplay
          muted
          playsinline
          crossorigin="anonymous"
          @error="error = '视频无法播放，请检查授权和浏览器支持的流协议'"
        />
        <div v-else class="fire-empty">服务未返回可播放地址</div>
        <span v-if="session.mock" class="video-label"
          >演示监控画面 · 未连接真实摄像头</span
        >
      </div>
      <div v-if="camera" class="video-tools">
        <StatusTag :value="camera.status" /><span class="muted">{{
          camera.placeName
        }}</span
        ><el-button
          v-if="canFire('cameras', 'talk')"
          :disabled="!session"
          @click="talk"
          >{{ talking ? '结束喊话' : '人工喊话' }}</el-button
        ><el-button
          v-if="canFire('cameras', 'share')"
          :disabled="!session"
          @click="shareOpen = true"
          >视频二维码</el-button
        >
      </div>
      <p class="muted">真实查岗请从“查岗管理”的岗位入口操作。</p>
    </div>
    <ShareCode v-model="shareOpen" kind="video" :resource-id="cameraId"
  /></el-dialog>
</template>
<script setup>
import { ref, computed, watch, nextTick, onBeforeUnmount } from 'vue'
import { ElMessage } from 'element-plus'
import { fire, requestVideo, requestTalk } from '@/api/fire'
import { canFire } from '@/api/fire/access'
import { attempt } from '@/views/fire/useFire'
import StatusTag from './StatusTag.vue'
import ShareCode from './ShareCode.vue'
const props = defineProps({ modelValue: Boolean, cameraId: String }),
  emit = defineEmits(['update:modelValue'])
const open = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v),
})
const camera = ref(null),
  session = ref(null),
  canvas = ref(),
  video = ref(),
  loading = ref(false),
  error = ref(''),
  talking = ref(false),
  shareOpen = ref(false)
let timer,
  stream,
  sessionVersion = 0
function cleanup() {
  sessionVersion++
  clearInterval(timer)
  stream?.getTracks().forEach((t) => t.stop())
  stream = null
  talking.value = false
  if (video.value) {
    video.value.pause()
    video.value.removeAttribute('src')
    video.value.load()
  }
}
function draw() {
  const c = canvas.value
  if (!c) return
  const x = c.getContext('2d')
  x.fillStyle = '#233137'
  x.fillRect(0, 0, 960, 480)
  x.fillStyle = '#3b4c51'
  x.beginPath()
  x.moveTo(0, 0)
  x.lineTo(960, 0)
  x.lineTo(800, 300)
  x.lineTo(180, 300)
  x.fill()
  x.fillStyle = '#111d25'
  x.fillRect(120, 70, 730, 240)
  for (let i = 0; i < 8; i++) {
    x.fillStyle = '#253f4b'
    x.fillRect(140 + (i % 4) * 174, 90 + Math.floor(i / 4) * 105, 150, 85)
    x.fillStyle = '#558a87'
    x.fillRect(152 + (i % 4) * 174, 102 + Math.floor(i / 4) * 105, 125, 60)
    x.fillStyle = '#244f56'
    for (let j = 0; j < 6; j++)
      x.fillRect(
        160 + (i % 4) * 174 + j * 19,
        118 + Math.floor(i / 4) * 105,
        12,
        24,
      )
  }
  x.fillStyle = '#424c4b'
  x.fillRect(90, 330, 790, 38)
  x.fillStyle = '#17252b'
  x.fillRect(380, 340, 90, 120)
  x.fillRect(580, 340, 90, 120)
  x.fillStyle = '#b2d5cf'
  x.font = '18px monospace'
  x.fillText('DEMO CAM / ' + (camera.value?.id || ''), 24, 32)
  x.fillText(
    new Date().toLocaleString('zh-CN', { timeZone: 'Asia/Shanghai' }),
    650,
    32,
  )
  x.fillStyle = '#e08181'
  x.fillText('● REC', 24, 452)
  x.fillStyle = '#b2d5cf'
  x.fillText('演示消控室画面', 700, 452)
}
async function connect() {
  if (!canFire('cameras', 'preview')) {
    error.value = '无视频预览权限'
    return
  }
  cleanup()
  const version = sessionVersion
  session.value = null
  camera.value = null
  error.value = ''
  loading.value = true
  try {
    const c = await fire.get('cameras', props.cameraId),
      s = await requestVideo(props.cameraId)
    if (version !== sessionVersion) return
    camera.value = c.data
    session.value = s.data
    await nextTick()
    if (s.data.mock) {
      draw()
      timer = setInterval(draw, 1000)
    }
  } catch (e) {
    if (version === sessionVersion) error.value = e.message
  } finally {
    if (version === sessionVersion) loading.value = false
  }
}
async function talk() {
  if (talking.value) {
    cleanupTalk()
    return
  }
  const s = await attempt(() => requestTalk(props.cameraId))
  if (!s) return
  if (s.data.mock) {
    talking.value = true
    ElMessage.info('演示喊话已开始，不会向真实设备发送声音')
    return
  }
  if (!s.data.talkUrl) {
    ElMessage.warning('服务未返回可用的喊话通道')
    return
  }
  ElMessage.info('已取得喊话授权，需配置设备厂商语音适配器')
}
function cleanupTalk() {
  stream?.getTracks().forEach((t) => t.stop())
  stream = null
  talking.value = false
}
watch(
  () => [props.modelValue, props.cameraId],
  () => {
    if (props.modelValue && props.cameraId) connect()
    else cleanup()
  },
  { immediate: true },
)
onBeforeUnmount(cleanup)
</script>
<style scoped>
.video-stage {
  position: relative;
  background: #020b13;
  min-height: 240px;
}
.video-stage canvas,
.video-stage video {
  width: 100%;
  display: block;
  max-height: 460px;
}
.video-label {
  position: absolute;
  bottom: 12px;
  left: 50%;
  transform: translateX(-50%);
  background: #0a1b29d9;
  padding: 6px 12px;
  color: #d6b978;
  font-size: 11px;
  white-space: nowrap;
}
.video-tools {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  margin-top: 15px;
}
.video-tools .muted {
  flex: 1;
  font-size: 12px;
}

</style>
