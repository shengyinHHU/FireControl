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
import { videoPreview } from '@/api/fire/video'
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
let player,
  version = 0,
  deadline
function release() {
  clearTimeout(deadline)
  player?.destroy()
  player = null
  if (video.value) {
    video.value.pause()
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
  if (!open.value) return
  clearTimeout(deadline)
  loading.value = false
  error.value = ''
  status.value = '播放中'
}
function onWaiting() {
  if (open.value && !error.value) status.value = '等待视频数据'
}
function onVideoError() {
  if (open.value && video.value?.getAttribute('src'))
    fail(
      '视频无法解码或连接中断，请重新连接；若持续失败，请检查设备和视频格式。',
    )
}
async function connect() {
  stop()
  const current = version
  loading.value = true
  error.value = ''
  status.value = '正在申请播放地址'
  try {
    const { data } = await videoPreview(props.device.deviceCode)
    if (current !== version || !open.value) return
    if (!data?.url) throw Error('服务未返回播放地址')
    await nextTick()
    const element = video.value
    if (!element) return
    status.value = '正在连接视频'
    deadline = setTimeout(() => {
      if (current === version)
        fail('等待画面超时，请重新连接或检查摄像头是否在线。')
    }, 35000)
    if (
      !data.requiresAuth &&
      element.canPlayType('application/vnd.apple.mpegurl')
    )
      element.src = data.url
    else {
      const { default: Hls } = await import('hls.js')
      if (current !== version || !open.value) return
      if (!Hls.isSupported())
        throw Error(
          '当前浏览器不支持此视频格式，请使用新版 Chrome、Edge 或 Safari',
        )
      player = new Hls({
        xhrSetup(xhr) {
          if (data.requiresAuth)
            xhr.setRequestHeader('Authorization', 'Bearer ' + getToken())
        },
        manifestLoadingMaxRetry: 1,
        levelLoadingMaxRetry: 1,
        fragLoadingMaxRetry: 1,
      })
      player.on(Hls.Events.ERROR, (_, info) => {
        if (info.fatal)
          console.warn(
            'Video playback diagnostic:',
            info.type,
            info.details,
            info.response?.code || '',
            info.mimeType || '',
            info.reason || '',
          )
        if (current === version && info.fatal)
          fail(
            info.details === 'bufferAddCodecError' ||
              info.details === 'manifestIncompatibleCodecsError'
              ? '当前浏览器不支持这台摄像头的视频编码，可下载 VLC 播放列表打开，或联系管理员配置兼容码流。'
              : '视频连接失败，请重新连接；若持续失败，请检查设备和网络。',
          )
      })
      player.loadSource(
        data.requiresAuth
          ? import.meta.env.VITE_APP_BASE_API + data.url
          : data.url,
      )
      player.attachMedia(element)
    }
    element.play().catch(() => {
      if (current === version) status.value = '请点击视频播放按钮'
    })
  } catch (e) {
    if (current === version) fail(e.message || '视频连接失败')
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
