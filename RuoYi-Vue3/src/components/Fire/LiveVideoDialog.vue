<template>
  <el-dialog
    v-model="open"
    :title="device?.deviceName || '实时视频'"
    width="920px"
    class="fire-dialog live-video-dialog"
    append-to-body
    @closed="stop"
  >
    <p class="muted">设备编码：{{ device?.deviceCode }}</p>
    <div class="live-stage" :aria-busy="loading">
      <video
        ref="video"
        controls
        autoplay
        muted
        playsinline
        crossorigin="anonymous"
        aria-label="摄像头实时视频"
        @playing="onPlaying"
        @waiting="onWaiting"
        @error="onVideoError"
      />
      <p v-if="loading" class="live-loading" role="status">{{ status }}</p>
    </div>
    <p v-if="error" class="fire-error" role="alert">{{ error }}</p>
    <div class="live-tools">
      <span role="status">{{ status }}</span
      ><el-button :loading="loading" @click="connect">重新连接</el-button
      ><el-button @click="fullscreen">全屏</el-button>
      <el-button :loading="downloading" @click="downloadVlc"
        >下载 VLC 播放列表</el-button
      >
    </div>
    <p class="muted">实时画面按需连接，关闭窗口后停止播放。</p>
  </el-dialog>
</template>
<script setup>
import { computed, ref, watch, nextTick, onBeforeUnmount } from 'vue'
import { videoPreview, closeVideoStream } from '@/api/fire/video'
import { getToken } from '@/utils/auth'
import { ElMessage } from 'element-plus'
const props = defineProps({ modelValue: Boolean, device: Object })
const emit = defineEmits(['update:modelValue'])
const open = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value),
})
const video = ref(),
  loading = ref(false),
  error = ref(''),
  status = ref('等待连接')
const downloading = ref(false)
let downloadUrl
let streamUrl
let player,
  version = 0,
  deadline
function release() {
  clearTimeout(deadline)
  if (player) {
    player.unload()
    player.detachMediaElement()
    player.destroy()
  }
  if (streamUrl) {
    closeVideoStream(streamUrl).catch(() => {})
    streamUrl = null
  }
  player = null
  if (video.value) {
    video.value.pause()
    video.value.srcObject = null
    video.value.removeAttribute('src')
    video.value.load()
  }
}
function stop() {
  if (downloadUrl) {
    URL.revokeObjectURL(downloadUrl)
    downloadUrl = null
  }
  version++
  release()
  loading.value = false
}
function fail(message) {
  stop()
  error.value = message
  status.value = '播放失败'
}
function onPlaying() {
  if (!open.value || !video.value?.videoWidth) return
  clearTimeout(deadline)
  loading.value = false
  error.value = ''
  status.value = '播放中'
}
function onWaiting() {
  if (open.value && player && !error.value) {
    status.value = '等待视频数据'
    loading.value = true
    armTimeout(version)
  }
}
function onVideoError() {
  if (open.value && player)
    fail(
      '视频无法解码或连接中断，请重新连接；若持续失败，请检查设备和视频格式。',
    )
}
function armTimeout(current) {
  clearTimeout(deadline)
  deadline = setTimeout(() => {
    if (current === version) fail('等待FLV画面超时，请重新连接或检查摄像头网络。')
  }, 40000)
}
async function connect() {
  stop()
  const current = version
  loading.value = true
  error.value = ''
  status.value = '正在申请FLV播放地址'
  try {
    const { data } = await videoPreview(props.device.deviceCode, 'flv')
    if (!data?.url?.startsWith('/fire/video/flv/')) throw Error('未返回FLV播放会话，请重启最新后端')
    if (current !== version || !open.value) {
      closeVideoStream(data.url).catch(() => {})
      return
    }
    streamUrl = data.url
    const { default: mpegts } = await import('mpegts.js')
    if (current !== version || !open.value) return
    mpegts.LoggingControl.enableAll = false
    const features = mpegts.getFeatureList()
    if (!features.mseLivePlayback) throw Error('当前浏览器不支持FLV直播，请使用新版Chrome')
    const token = getToken()
    if (!token) throw Error('登录已过期，请重新登录后播放')
    await nextTick()
    if (current !== version || !open.value || !video.value) return
    status.value = '正在连接FLV视频'
    const sdk = mpegts.createPlayer({
      type: 'flv', isLive: true,
      url: import.meta.env.VITE_APP_BASE_API + data.url,
    }, {
      headers: { Authorization: 'Bearer ' + token },
      enableWorker: false,
      enableStashBuffer: false,
      lazyLoad: false,
      autoCleanupSourceBuffer: true,
      autoCleanupMaxBackwardDuration: 30,
      autoCleanupMinBackwardDuration: 10,
      liveBufferLatencyChasing: true,
      liveBufferLatencyMaxLatency: 3,
      liveBufferLatencyMinRemain: 1,
    })
    player = sdk
    sdk.on(mpegts.Events.ERROR, (type, detail, info) => {
      if (current !== version) return
      const code = info?.code
      fail(type === mpegts.ErrorTypes.NETWORK_ERROR
        ? code === 401 || code === 403
          ? '视频访问授权已失效，请重新登录或检查权限。'
          : 'FLV视频连接中断，请重新连接；若持续失败，请检查设备和网络。'
        : 'FLV视频解码失败，请检查浏览器对该视频编码的支持。')
    })
    sdk.on(mpegts.Events.MEDIA_INFO, info => {
      if (current !== version) return
      if (/hvc1|hev1|hevc|h265/i.test(info.videoCodec || '') && !features.mseH265Playback)
        fail('该摄像头为H.265编码，当前浏览器环境无法解码，请使用支持HEVC的新版Chrome。')
    })
    sdk.on(mpegts.Events.LOADING_COMPLETE, () => {
      if (current === version) {
        status.value = '视频流已结束'
        armTimeout(current)
      }
    })
    sdk.attachMediaElement(video.value)
    armTimeout(current)
    sdk.load()
    sdk.play()?.catch(() => {
      if (current === version) status.value = '请点击视频播放按钮'
    })
  } catch (e) {
    if (current === version) fail(e.message || 'FLV视频连接失败，请重新连接')
  }
}
async function downloadVlc() {
  downloading.value = true
  try {
    const { data } = await videoPreview(props.device.deviceCode, 'rtsp')
    if (!open.value) return
    if (!data?.url?.startsWith('rtsp://')) throw Error('未返回有效 RTSP 地址')
    if (downloadUrl) URL.revokeObjectURL(downloadUrl)
    downloadUrl = URL.createObjectURL(
      new Blob(['#EXTM3U\n#EXTINF:-1,Camera\n' + data.url + '\n'], {
        type: 'audio/x-mpegurl',
      }),
    )
    const anchor = document.createElement('a')
    anchor.href = downloadUrl
    anchor.download = 'camera-live.m3u'
    anchor.click()
    ElMessage.success('播放列表已下载，请立即用 VLC 打开；地址具有时效')
  } catch (e) {
    ElMessage.error(e.message || '下载播放列表失败')
  } finally {
    downloading.value = false
  }
}
async function fullscreen() {
  try {
    await video.value?.requestFullscreen()
  } catch {
    error.value = '无法进入全屏，请使用播放器内的全屏按钮'
  }
}
watch(
  () => [props.modelValue, props.device?.deviceCode],
  async () => {
    if (props.modelValue && props.device?.deviceCode) {
      await nextTick()
      connect()
    } else stop()
  },
  { immediate: true },
)
onBeforeUnmount(stop)
</script>
<style scoped>
.live-stage {
  position: relative;
  background: var(--fire-bg);
  aspect-ratio: 16/9;
  margin-top: 12px;
}
.live-stage video {
  width: 100%;
  height: 100%;
  object-fit: contain;
}
.live-loading {
  position: absolute;
  inset: 40% 0 auto;
  text-align: center;
  pointer-events: none;
}
.live-tools {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
  margin-top: 12px;
}
.live-tools span {
  flex: 1;
}
.muted {
  font-size: 12px;
}
</style>
