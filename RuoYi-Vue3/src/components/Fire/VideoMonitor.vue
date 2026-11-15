<template>
  <section class="video-monitor" :class="{ compact }" aria-label="真实视频监控">
    <p v-if="!canVideo()" class="fire-empty">
      暂无视频监控权限，请联系管理员授权。
    </p>
    <template v-else>
      <div v-if="compactTree" class="monitor-compact-tree">
        <div class="monitor-toolbar">
          <strong class="monitor-heading">监控目录</strong>
          <el-button :loading="compactPaging" @click="reloadTree">刷新设备</el-button>
        </div>
        <div class="monitor-devices monitor-tree-scroll">
          <p v-if="treeError" class="fire-error" role="alert">{{ treeError }} <el-button link @click="reloadTree">重试目录</el-button></p>
          <el-tree :key="treeEpoch" ref="tree" node-key="id" lazy :load="loadTree" :props="treeProps"
            :default-expanded-keys="['all']" :expand-on-click-node="true" highlight-current aria-label="视频监控目录树">
            <template #default="{ node, data }">
              <span v-if="data.kind === 'directory'" class="monitor-tree-directory" :title="data.name">
                <el-icon><FolderOpened v-if="node.expanded" /><Folder v-else /></el-icon><span>{{ data.name }}</span>
              </span>
              <button v-else-if="data.kind === 'camera'" type="button" class="monitor-device monitor-tree-camera"
                :title="data.name" :disabled="!canVideo('preview')" :aria-label="`查看视频：${data.name}`" @click.stop="play(data.device)">
                <el-icon><VideoCamera /></el-icon><span>{{ data.name }}</span>
              </button>
              <el-button v-else-if="data.kind === 'more'" link :loading="compactPaging" @click.stop="loadTreeMore(node)">加载更多</el-button>
              <span v-else class="monitor-tree-empty">{{ data.name }}</span>
            </template>
          </el-tree>
        </div>
        <div class="monitor-footer" role="status">展开目录，点击摄像头查看视频</div>
      </div>
      <div v-else class="monitor-layout" :class="{ 'with-tree': directoryTree }">
      <aside v-if="directoryTree" class="monitor-tree-panel" aria-label="监控目录">
        <h2>监控目录</h2>
        <el-button class="monitor-all" :type="selected ? 'default' : 'primary'" :aria-pressed="!selected" @click="goRoot">全部视频</el-button>
        <p v-if="treeError" class="fire-error" role="alert">{{ treeError }} <el-button link @click="reloadTree">重试目录</el-button></p>
        <el-tree :key="treeEpoch" ref="tree" node-key="id" lazy :load="loadTree" :props="treeProps"
          :expand-on-click-node="false" highlight-current :current-node-key="selected || undefined"
          aria-label="视频监控目录树" @node-click="selectTree" />
      </aside>
      <div class="monitor-content">
      <div class="monitor-toolbar">
        <strong v-if="directoryTree" class="monitor-heading">{{ selectedName || '全部视频' }}</strong>
        <el-select v-if="!directoryTree"
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
      <div v-if="!directoryTree && trail.length" class="monitor-path">
        <el-button link @click="goRoot">首层目录</el-button>
        <span v-for="r in trail" :key="r.id"> / {{ r.name }}</span>
      </div>
      <div v-if="!directoryTree && children.length" class="monitor-directories">
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
          :disabled="!canVideo(selectable ? 'list' : 'preview')"
          :class="{ selected: selectable && selectedCodes.includes(device.deviceCode) }"
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
          <span>{{ selectable ? (selectedCodes.includes(device.deviceCode) ? '已选 ✓' : '选择摄像头') : '查看视频 ›' }}</span>
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
      </div></div>
      <LiveVideoDialog v-model="playing" :device="activeDevice" />
    </template>
  </section>
</template>
<script setup>
import { computed, ref, onMounted, onBeforeUnmount } from 'vue'
import { canVideo, videoRegions, videoDevices } from '@/api/fire/video'
import LiveVideoDialog from './LiveVideoDialog.vue'
const props = defineProps({ compact: Boolean, directoryTree: Boolean, selectable: Boolean, selectedCodes: { type: Array, default: () => [] } })
const emit = defineEmits(['select'])
const compactTree = computed(() => props.compact && props.directoryTree)
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
const tree = ref(), treeError = ref(''), treeEpoch = ref(0), selectedName = ref('')
const compactPaging = ref(false)
const treeProps = { label: 'name', isLeaf: 'isLeaf' }
let treeVersion = 0
async function loadTree(node, resolve, reject) {
  const current = treeVersion
  if (compactTree.value && node.level === 0) {
    resolve([{ id: 'all', kind: 'directory', name: '全部视频', isLeaf: false }])
    return
  }
  try {
    const regionId = compactTree.value ? node.data.regionId : node.level === 0 ? undefined : node.data.id
    const [{ data }, page] = await Promise.all([
      videoRegions(regionId),
      compactTree.value ? videoDevices(regionId) : Promise.resolve(null),
    ])
    if (current !== treeVersion) { resolve([]); return }
    if (!Array.isArray(data)) throw Error('监控目录返回格式异常')
    if (compactTree.value) {
      const folders = data.map(row => ({ id: `${node.key}:directory:${row.id}`, regionId: String(row.id), kind: 'directory', name: row.name, isLeaf: false }))
      const children = [...folders, ...compactPageNodes(page.data, node.key, regionId)]
      treeError.value = ''
      resolve(children.length ? children : [{ id: `${node.key}:empty`, kind: 'empty', name: '当前目录暂无摄像头', isLeaf: true }])
      return
    }
    treeError.value = ''
    resolve(data.map(row => ({ ...row, id: String(row.id), isLeaf: row.hasChildren === 0 || row.hasChildren === '0' || row.hasChildren === false })))
  } catch (e) {
    if (current === treeVersion) treeError.value = e.message || '监控目录加载失败'
    reject()
  }
}
function compactPageNodes(page, parentKey, regionId, existing = new Set()) {
  if (!Array.isArray(page?.list)) throw Error('监控设备返回格式异常')
  const nodes = []
  for (const device of page.list) {
    const id = `${parentKey}:camera:${device.deviceCode}`
    if (existing.has(id)) continue
    existing.add(id)
    nodes.push({ id, kind: 'camera', name: device.deviceName || '未命名摄像头', device, isLeaf: true })
  }
  const cursor = String(page.lastId ?? -1)
  if (cursor !== '-1') nodes.push({ id: `${parentKey}:more`, kind: 'more', name: '加载更多', regionId, cursor, isLeaf: true })
  return nodes
}
async function loadTreeMore(node) {
  if (compactPaging.value) return
  const current = treeVersion, parent = node.parent, { regionId, cursor } = node.data
  compactPaging.value = true
  treeError.value = ''
  try {
    const { data } = await videoDevices(regionId, cursor)
    if (current !== treeVersion) return
    if (String(data?.lastId ?? -1) === cursor) throw Error('平台分页游标未更新，请刷新设备后重试')
    const additions = compactPageNodes(data, parent.key, regionId, new Set(parent.childNodes.map(child => child.key)))
    tree.value.remove(node.data)
    for (const item of additions) tree.value.append(item, parent.data)
    treeError.value = ''
  } catch (e) {
    if (current === treeVersion) treeError.value = e.message || '加载更多失败，请重试'
  } finally {
    if (current === treeVersion) compactPaging.value = false
  }
}
function selectTree(region) {
  selected.value = String(region.id)
  selectedName.value = region.name
  refresh()
}
function reloadTree() {
  treeVersion++; treeEpoch.value++; treeError.value = ''
  compactPaging.value = false
  if (compactTree.value) return
  goRoot()
}
let version = 0
function play(device) {
  if (props.selectable) { emit('select', device); return }
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
    if (props.directoryTree) {
      const { data } = await videoDevices(selected.value || undefined)
      if (current !== version) return
      devices.value = data?.list || []
      nextId.value = String(data?.lastId ?? -1)
      return
    }
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
  selectedName.value = ''
  tree.value?.setCurrentKey(undefined)
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
  if (canVideo() && !compactTree.value) refresh()
})
onBeforeUnmount(() => {
  treeVersion++
  version++
  playing.value = false
})
</script>
<style scoped>
.monitor-layout.with-tree { display:grid; grid-template-columns:250px minmax(0,1fr); gap:20px; align-items:start; }
.monitor-content { min-width:0; }
.monitor-tree-panel { background:var(--fire-panel);color:var(--fire-text);border:1px solid var(--fire-border,var(--el-border-color)); border-radius:4px; padding:12px; min-width:0; }
.monitor-tree-panel h2 { font-size:15px; margin:0 0 12px; }
.monitor-all { width:100%; margin-bottom:12px; }
.monitor-tree-panel :deep(.el-tree) { max-height:calc(100vh - 300px); overflow:auto; background:transparent; color:inherit; --el-tree-node-hover-bg-color:var(--fire-hover,#123950); }
.monitor-tree-panel :deep(.el-tree-node__content) { min-height:34px; }
.monitor-tree-panel :deep(.el-tree-node.is-current > .el-tree-node__content) { background:var(--fire-hover,#123950); color:var(--el-color-primary); }
.monitor-heading { flex:1; align-self:center; overflow-wrap:anywhere; }
.compact .monitor-layout,.compact .monitor-content { display:flex;flex-direction:column;flex:1;min-height:0;overflow:hidden; }
@media(max-width:900px) { .monitor-layout.with-tree { grid-template-columns:210px minmax(0,1fr); gap:12px; }.with-tree .monitor-devices{grid-template-columns:1fr;} }
@media(max-width:600px) { .monitor-layout.with-tree { grid-template-columns:1fr; }.monitor-tree-panel :deep(.el-tree){max-height:220px;} }

.monitor-device.selected { border-color: var(--fire-accent); }
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
.monitor-compact-tree { display:flex; flex-direction:column; flex:1; min-height:0; overflow:hidden; }
.monitor-compact-tree .monitor-toolbar { margin-bottom:6px; }
.monitor-compact-tree .monitor-tree-scroll { display:block; overflow:auto; }
.monitor-compact-tree :deep(.el-tree) { background:transparent; color:inherit; min-width:100%; --el-tree-node-hover-bg-color:var(--el-fill-color-light); }
.monitor-compact-tree :deep(.el-tree-node__content) { height:auto; min-height:34px; align-items:flex-start; }
.monitor-compact-tree :deep(.el-tree-node.is-current > .el-tree-node__content),.monitor-compact-tree :deep(.el-tree-node:focus > .el-tree-node__content) { background:var(--el-fill-color-light); color:var(--fire-cyan); }
.monitor-compact-tree :deep(.el-tree-node:focus-visible > .el-tree-node__content) { outline:1px solid var(--fire-cyan); outline-offset:-1px; }
.monitor-compact-tree :deep(.el-tree-node__expand-icon) { margin-top:5px; }
.monitor-tree-directory,.compact .monitor-tree-camera { display:flex; flex:1; min-width:0; align-items:flex-start; gap:7px; padding:7px 4px 7px 0; text-align:left; white-space:normal; overflow-wrap:anywhere; line-height:1.45; }
.monitor-tree-directory > span,.monitor-tree-camera > span { min-width:0; }
.monitor-tree-directory .el-icon,.monitor-tree-camera .el-icon { flex-shrink:0; margin-top:2px; color:var(--fire-cyan); }
.compact .monitor-tree-camera { border:0; border-radius:2px; background:transparent; font:inherit; color:inherit; flex-wrap:nowrap; }
.compact .monitor-tree-camera:hover,.compact .monitor-tree-camera:focus-visible { color:var(--fire-cyan); outline:1px solid var(--fire-cyan); outline-offset:-1px; }
.monitor-tree-empty { padding:8px 4px 8px 0; color:var(--fire-muted); white-space:normal; }
.monitor-compact-tree .monitor-footer { margin-top:6px; color:var(--fire-muted); }
@media (max-width: 600px) {
  .monitor-devices {
    grid-template-columns: 1fr;
  }
  .monitor-device {
    flex-wrap: wrap;
  }
}
</style>
