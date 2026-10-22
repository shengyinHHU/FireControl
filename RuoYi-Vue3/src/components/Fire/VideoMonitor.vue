<template>
  <section class="video-monitor" :class="{ compact }" aria-label="真实视频监控">
    <p v-if="!canVideo()" class="fire-empty">
      暂无视频监控权限，请联系管理员授权。
    </p>
    <template v-else>
      <div class="monitor-toolbar">
        <el-select
          v-model="selected"
          placeholder="全部目录"
          clearable
          filterable
          popper-class="fire-dialog"
          aria-label="选择监控目录"
          @change="changeRegion"
        >
          <el-option
            v-for="r in regions"
            :key="r.id"
            :label="r.name"
            :value="String(r.id)"
          />
        </el-select>
        <el-button :loading="loading" @click="refresh">刷新设备</el-button>
      </div>
      <div v-if="trail.length" class="monitor-path">
        <el-button link @click="goRoot">首层目录</el-button>
        <span v-for="r in trail" :key="r.id"> / {{ r.name }}</span>
      </div>
      <div v-if="children.length" class="monitor-directories">
        <el-button
          v-for="r in children"
          :key="r.id"
          size="small"
          @click="enterRegion(r)"
          >{{ r.name }} ›</el-button
        >
      </div>
      <p v-if="error" class="fire-error" role="alert">
        {{ error }} <el-button link @click="refresh">重试</el-button>
      </p>
      <div v-loading="loading" class="monitor-devices" :aria-busy="loading">
        <button
          v-for="device in devices"
          :key="device.deviceCode"
          type="button"
          class="monitor-device"
          :disabled="!canVideo('preview')"
          @click="play(device)"
        >
          <el-icon class="monitor-icon"><VideoCamera /></el-icon>
          <span class="monitor-device-body"
            ><strong>{{ device.deviceName || '未命名摄像头' }}</strong
            ><small>{{ device.deviceCode }}</small
            ><small>{{
              device.status === undefined
                ? '状态未知'
                : '平台状态：' + device.status
            }}</small></span
          >
          <span>查看视频 ›</span>
        </button>
        <p v-if="!loading && !error && !devices.length" class="fire-empty">
          当前目录暂无摄像头
        </p>
      </div>
      <div class="monitor-footer" role="status">
        <span>天翼视联 · 已加载 {{ devices.length }} 台</span
        ><el-button
          v-if="nextId !== null && nextId !== '-1'"
          :loading="loading"
          @click="loadMore"
          >加载更多</el-button
        >
      </div>
      <LiveVideoDialog v-model="playing" :device="activeDevice" />
    </template>
  </section>
</template>
<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { canVideo, videoRegions, videoDevices } from '@/api/fire/video'
import LiveVideoDialog from './LiveVideoDialog.vue'
defineProps({ compact: Boolean })
const selected = ref(''),
  regions = ref([]),
  children = ref([]),
  trail = ref([]),
  devices = ref([])
const loading = ref(false),
  error = ref(''),
  nextId = ref(null),
  playing = ref(false),
  activeDevice = ref(null)
let version = 0
function play(device) {
  activeDevice.value = device
  playing.value = true
}
async function refresh() {
  const current = ++version
  loading.value = true
  error.value = ''
  devices.value = []
  nextId.value = null
  try {
    const [root, nested, page] = await Promise.all([
      videoRegions(),
      selected.value
        ? videoRegions(selected.value)
        : Promise.resolve({ data: [] }),
      videoDevices(selected.value || undefined),
    ])
    if (current !== version) return
    regions.value = root.data || []
    children.value = nested.data || []
    devices.value = page.data?.list || []
    nextId.value = String(page.data?.lastId ?? -1)
  } catch (e) {
    if (current === version) error.value = e.message || '监控设备加载失败'
  } finally {
    if (current === version) loading.value = false
  }
}
function changeRegion() {
  const region = regions.value.find((r) => String(r.id) === selected.value)
  trail.value = region ? [region] : []
  refresh()
}
function enterRegion(region) {
  selected.value = String(region.id)
  trail.value.push(region)
  refresh()
}
function goRoot() {
  selected.value = ''
  trail.value = []
  refresh()
}
async function loadMore() {
  if (loading.value || nextId.value === '-1' || nextId.value === null) return
  const current = version,
    cursor = nextId.value
  loading.value = true
  error.value = ''
  try {
    const { data } = await videoDevices(selected.value || undefined, cursor)
    if (current !== version) return
    const seen = new Set(devices.value.map((d) => d.deviceCode))
    devices.value.push(
      ...(data?.list || []).filter((d) => !seen.has(d.deviceCode)),
    )
    nextId.value = String(data?.lastId ?? -1)
    if (nextId.value === cursor) {
      nextId.value = '-1'
      error.value = '平台分页游标未更新，请刷新设备后重试'
    }
  } catch (e) {
    if (current === version) error.value = e.message || '加载更多失败'
  } finally {
    if (current === version) loading.value = false
  }
}
onMounted(() => {
  if (canVideo()) refresh()
})
onBeforeUnmount(() => {
  version++
  playing.value = false
})
</script>
<style scoped>
.video-monitor {
  min-width: 0;
}
.monitor-toolbar {
  display: flex;
  gap: 10px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}
.monitor-toolbar .el-select {
  flex: 1;
  min-width: 140px;
  max-width: 320px;
}
.monitor-path {
  margin: 8px 0;
}
.monitor-directories {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
  margin-bottom: 10px;
}
.monitor-directories .el-button {
  margin-left: 0;
}
.monitor-devices {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 10px;
  min-height: 80px;
}
.monitor-device {
  display: flex;
  align-items: center;
  gap: 12px;
  text-align: left;
  padding: 14px;
  color: inherit;
  background: transparent;
  border: 1px solid var(--el-border-color);
  border-radius: 4px;
  cursor: pointer;
  min-width: 0;
}
.monitor-device:hover,
.monitor-device:focus-visible {
  border-color: var(--el-color-primary);
  outline: 2px solid var(--el-color-primary);
  outline-offset: 2px;
}
.monitor-device:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.monitor-device-body {
  display: flex;
  flex-direction: column;
  gap: 6px;
  flex: 1;
  min-width: 0;
  overflow-wrap: anywhere;
}
.monitor-device small {
  font-size: 12px;
}
.monitor-icon {
  font-size: 28px;
}
.monitor-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
  margin-top: 12px;
  font-size: 12px;
}
.compact {
  display: flex;
  flex-direction: column;
  min-height: 0;
  flex: 1;
  overflow: hidden;
}
.compact .monitor-toolbar,
.compact .monitor-footer {
  flex-shrink: 0;
}
.compact .monitor-directories {
  max-height: 64px;
  overflow: auto;
  flex-shrink: 0;
}
.compact .monitor-devices {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  max-height: none;
  overflow: auto;
}
.compact .monitor-device {
  padding: 10px;
}
.compact .monitor-icon {
  font-size: 22px;
}
@media (max-width: 600px) {
  .monitor-devices {
    grid-template-columns: 1fr;
  }
  .monitor-device {
    flex-wrap: wrap;
  }
}
</style>
