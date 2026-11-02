<template>
  <section aria-label="监控坐标维护" class="video-coordinate-manager">
    <p v-if="!canVideo()" class="fire-empty">暂无视频监控权限，请联系管理员授权。</p>
    <template v-else>
      <div class="catalog-summary" role="status" aria-live="polite">
        <span>台账 <b>{{ summary.total }}</b> 台</span><span>已填写 {{ summary.located }} 台</span><span>未填写 {{ summary.pending }} 台</span>
        <span>最近完整同步：{{ sync.lastSuccessAt || '尚未同步' }}</span>
      </div>
      <div class="catalog-layout">
        <aside class="catalog-directories" aria-label="台账目录">
          <strong>监控目录</strong>
          <el-button :type="!query.directory ? 'primary' : 'default'" :aria-pressed="!query.directory" @click="selectDirectory('')">全部监控</el-button>
          <el-tree :data="directoryTree" node-key="id" :props="{ label: 'name', children: 'children' }" highlight-current
            :current-node-key="query.directory" :expand-on-click-node="false" aria-label="台账监控目录树" @node-click="node => selectDirectory(node.id)" />
          <p v-if="!directories.length && !loading">同步监控目录后显示目录树。</p>
        </aside>
        <div class="catalog-content">
          <form class="catalog-toolbar" novalidate @submit.prevent="search" @keydown.enter.prevent="event => { if (!event.isComposing) search() }">
            <el-input v-model="draftKeyword" clearable maxlength="128" placeholder="监控名称、设备编码或安装位置" aria-label="搜索监控" @clear="search" />
            <el-select v-model="query.location" clearable placeholder="全部定位状态" aria-label="定位状态" popper-class="fire-dialog" @change="search">
              <el-option label="未填写" value="pending" /><el-option label="已填写" value="located" /><el-option label="待核验" value="review" /><el-option label="已核验" value="confirmed" />
            </el-select>
            <el-button native-type="submit" :loading="loading">查询</el-button><el-button :disabled="loading" @click="resetFilters">重置</el-button>
          </form>
          <div class="catalog-actions">
            <el-button v-if="canVideo('sync')" :loading="sync.state === 'running'" :disabled="sync.state === 'running'" @click="synchronize">同步监控目录</el-button>
            <el-button v-if="canVideo('export')" :loading="exporting" :disabled="exporting || loading" @click="exportRows(true)">导出未定位</el-button>
            <el-button v-if="canVideo('export')" :disabled="exporting || loading" @click="exportRows(false)">导出当前筛选</el-button>
            <el-button v-if="canVideo('import')" type="primary" @click="importer.open()">导入坐标</el-button>
            <el-button :loading="loading" @click="load">刷新台账</el-button>
          </div>
          <p class="catalog-sync" role="status">{{ sync.message }}<span v-if="sync.state === 'running'"> · 已发现 {{ sync.directories }} 个目录、{{ sync.devices }} 台监控</span></p>
          <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon role="alert" />
          <el-table v-loading="loading" :data="rows" row-key="id" max-height="580" :aria-busy="loading" aria-label="监控坐标台账" :empty-text="error ? '台账加载失败，请重试' : '没有符合条件的监控；首次使用请同步监控目录'">
            <el-table-column prop="name" label="监控名称" min-width="180" fixed="left" show-overflow-tooltip />
            <el-table-column prop="deviceCode" label="设备编码" min-width="200" show-overflow-tooltip />
            <el-table-column prop="directoryPath" label="完整目录" min-width="190" show-overflow-tooltip />
            <el-table-column prop="installationLocation" label="安装位置" min-width="170" show-overflow-tooltip />
            <el-table-column label="经度（GCJ-02）" width="155"><template #default="{ row }">{{ row.lng ?? '未填写' }}</template></el-table-column>
            <el-table-column label="纬度（GCJ-02）" width="150"><template #default="{ row }">{{ row.lat ?? '未填写' }}</template></el-table-column>
            <el-table-column label="定位状态" width="105"><template #default="{ row }"><el-tag :type="row.locationStatus === 'confirmed' ? 'success' : 'info'">{{ videoLocationText(row.locationStatus) }}</el-tag></template></el-table-column>
            <el-table-column prop="coordinateTime" label="坐标更新时间" width="170" />
            <el-table-column label="操作" width="160" fixed="right"><template #default="{ row }">
              <el-button v-if="canVideo('edit')" link type="primary" @click="openEditor(row)">编辑坐标</el-button>
              <el-button v-if="canVideo('preview')" link type="primary" @click="play(row)">查看视频</el-button>
            </template></el-table-column>
          </el-table>
          <el-pagination v-model:current-page="query.pageNum" v-model:page-size="query.pageSize" :page-sizes="[20, 50, 100]" :total="total" layout="total, sizes, prev, pager, next" @current-change="changePage" @size-change="changeSize" />
        </div>
      </div>
      <VideoCoordinatesImport ref="importer" @success="load" />
      <LiveVideoDialog v-model="playing" :device="activeDevice" />
      <el-dialog v-model="editor" title="编辑监控坐标" width="min(650px, 95vw)" class="fire-dialog" append-to-body
        :close-on-click-modal="false" :close-on-press-escape="!saving" :show-close="!saving" :before-close="closeEditor">
        <p>{{ activeDevice?.name }} · {{ activeDevice?.deviceCode }}</p>
        <el-form ref="formRef" :model="form" :rules="rules" :disabled="saving" label-width="110px" novalidate @submit.prevent="save">
          <el-form-item label="安装位置" prop="installationLocation"><el-input v-model="form.installationLocation" maxlength="500" /></el-form-item>
          <el-form-item label="经度" prop="sourceLongitude"><el-input v-model="form.sourceLongitude" inputmode="decimal" placeholder="例如118.753000" /></el-form-item>
          <el-form-item label="纬度" prop="sourceLatitude"><el-input v-model="form.sourceLatitude" inputmode="decimal" placeholder="例如32.229000" /></el-form-item>
          <el-form-item label="采集坐标系" prop="crs"><el-select v-model="form.crs" popper-class="fire-dialog"><el-option label="高德 GCJ-02" value="GCJ02" /><el-option label="GPS WGS84" value="WGS84" /><el-option label="百度 BD-09" value="BD09" /></el-select></el-form-item>
          <el-form-item label="定位状态"><el-radio-group v-model="form.locationStatus"><el-radio value="review">待核验</el-radio><el-radio value="confirmed">已核验</el-radio></el-radio-group></el-form-item>
          <el-form-item label="调查备注"><el-input v-model="form.remark" type="textarea" :autosize="{ minRows: 3, maxRows: 6 }" resize="none" maxlength="1000" /></el-form-item>
        </el-form>
        <p>GPS、百度坐标将转换为高德坐标，并保留原始采集值。请填写监控实际安装位置。</p>
        <el-alert v-if="editError" :title="editError" type="error" :closable="false" show-icon role="alert" />
        <template #footer><el-button :disabled="saving" @click="closeEditor(() => editor = false)">取消</el-button><el-button type="primary" :loading="saving" :disabled="saving" @click="save">保存坐标</el-button></template>
      </el-dialog>
    </template>
  </section>
</template>
<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter, onBeforeRouteLeave, onBeforeRouteUpdate } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { canVideo } from '@/api/fire/video'
import { listVideoCameras, videoCatalogDirectories, syncVideoCatalog, exportVideoCoordinates, editVideoCoordinates, videoLocationText } from '@/api/fire/videoCatalog'
import VideoCoordinatesImport from '@/components/ExcelImportDialog/VideoCoordinates.vue'
import LiveVideoDialog from './LiveVideoDialog.vue'
const route = useRoute(), router = useRouter()
const scalar = value => typeof value === 'string' ? value : ''
const query = reactive({ keyword: scalar(route.query.vc_q), directory: scalar(route.query.vc_dir), location: scalar(route.query.vc_loc), pageNum: Math.max(1, Number(route.query.vc_page) || 1), pageSize: [20, 50, 100].includes(Number(route.query.vc_size)) ? Number(route.query.vc_size) : 20 })
const draftKeyword = ref(query.keyword), rows = ref([]), directories = ref([]), total = ref(0), summary = ref({ total: 0, located: 0, pending: 0 }), sync = ref({ state: 'idle' })
const loading = ref(false), exporting = ref(false), error = ref(''), importer = ref(), playing = ref(false), activeDevice = ref(null), editor = ref(false), saving = ref(false), editError = ref(''), formRef = ref()
const form = reactive({ sourceLongitude: '', sourceLatitude: '', crs: 'GCJ02', locationStatus: 'review', installationLocation: '', remark: '', revision: 0 })
const rules = { sourceLongitude: [{ required: true, message: '请填写经度', trigger: 'blur' }], sourceLatitude: [{ required: true, message: '请填写纬度', trigger: 'blur' }], crs: [{ required: true, message: '请选择实际采集坐标系', trigger: 'change' }] }
let version = 0, disposed = false, timer, original = ''
const directoryTree = computed(() => {
  const map = new Map(directories.value.map(d => [d.id, { ...d, children: [] }]))
  const root = []
  for (const node of map.values()) { const parent = map.get(node.parentId); if (parent && parent !== node) parent.children.push(node); else root.push(node) }
  return root
})
function remember() {
  router.replace({ query: { ...route.query, vc_q: query.keyword || undefined, vc_dir: query.directory || undefined, vc_loc: query.location || undefined, vc_page: String(query.pageNum), vc_size: String(query.pageSize) } })
}
async function loadDirectories() { const response = await videoCatalogDirectories(); if (!disposed) directories.value = response.data }
async function load() {
  const current = ++version; loading.value = true; error.value = ''; clearTimeout(timer)
  try {
    const response = await listVideoCameras(query)
    if (disposed || current !== version) return
    rows.value = response.rows; total.value = response.total; summary.value = response.summary; sync.value = response.sync
    const lastPage = Math.max(1, Math.ceil(total.value / query.pageSize))
    if (query.pageNum > lastPage) { query.pageNum = lastPage; remember(); return load() }
    if (sync.value.state === 'running') timer = setTimeout(load, 2000)
    else await loadDirectories()
  } catch (e) { if (!disposed && current === version) { rows.value = []; error.value = e.message || '台账加载失败'; timer = setTimeout(load, 10000) } }
  finally { if (!disposed && current === version) loading.value = false }
}
function search() { query.keyword = draftKeyword.value.trim(); query.pageNum = 1; remember(); load() }
function selectDirectory(id) { query.directory = id; search() }
function changePage() { remember(); load() }
function changeSize() { query.pageNum = 1; remember(); load() }
function resetFilters() { draftKeyword.value = ''; query.directory = ''; query.location = ''; search() }
async function synchronize() {
  error.value = ''
  try { await syncVideoCatalog(); ElMessage.success('已开始同步监控目录'); await load() }
  catch (e) { error.value = e.message || '同步启动失败' }
}
async function exportRows(unlocated) {
  exporting.value = true; error.value = ''
  try { await exportVideoCoordinates({ keyword: query.keyword, directory: query.directory, location: unlocated ? 'pending' : query.location }) }
  catch (e) { error.value = e.message || '导出失败，请重试' }
  finally { exporting.value = false }
}
function play(row) { activeDevice.value = row; playing.value = true }
function openEditor(row) {
  activeDevice.value = row
  Object.assign(form, { sourceLongitude: row.lng === null ? '' : String(row.lng), sourceLatitude: row.lat === null ? '' : String(row.lat), crs: 'GCJ02', locationStatus: row.locationStatus === 'confirmed' ? 'confirmed' : 'review', installationLocation: row.installationLocation, remark: row.remark, revision: row.revision })
  original = JSON.stringify(form); editError.value = ''; editor.value = true
}
async function canLeave() {
  if (saving.value || importer.value?.isBusy()) { ElMessage.warning('正在保存或导入坐标，请等待完成'); return false }
  if (editor.value && JSON.stringify(form) !== original) {
    try { await ElMessageBox.confirm('尚未保存坐标，关闭将丢弃本次填写。', '放弃修改', { confirmButtonText: '放弃修改', cancelButtonText: '继续填写', type: 'warning' }); original = JSON.stringify(form); editor.value = false }
    catch { return false }
  }
  return true
}
async function closeEditor(done) { if (await canLeave()) done() }
async function save() {
  if (!await formRef.value.validate().catch(() => false)) return
  if (![form.sourceLongitude, form.sourceLatitude].every(v => /^[+-]?\d{1,3}(\.\d{1,10})?$/.test(v))) { editError.value = '经纬度须填写十进制度数'; return }
  saving.value = true; editError.value = ''
  try { await editVideoCoordinates(activeDevice.value.id, { ...form }); editor.value = false; ElMessage.success('监控坐标已保存'); await load() }
  catch (e) { editError.value = e.message || '保存失败，请重试' }
  finally { saving.value = false }
}
onMounted(() => { if (canVideo()) load() })
onBeforeRouteLeave(canLeave)
onBeforeRouteUpdate(to => to.query.tab === 'coordinates' ? true : canLeave())
onBeforeUnmount(() => { disposed = true; version++; clearTimeout(timer) })
defineExpose({ canLeave })
</script>
<style scoped>
.catalog-summary { display:flex; flex-wrap:wrap; gap:12px 24px; padding:12px 0 18px; color:var(--fire-text); }
.catalog-summary b { color:var(--fire-cyan); font-variant-numeric:tabular-nums; }
.catalog-layout { display:grid; grid-template-columns:250px minmax(0, 1fr); gap:20px; align-items:start; }
.catalog-directories { padding:12px; color:var(--fire-text); background:var(--fire-panel); border:1px solid var(--el-border-color); border-radius:4px; }
.catalog-directories > .el-button { display:block; width:100%; margin:12px 0; }
.catalog-directories :deep(.el-tree) { overflow:auto; max-height:580px; background:transparent; color:var(--fire-text); --el-tree-node-hover-bg-color:var(--el-fill-color-light); }
.catalog-content { min-width:0; }
.catalog-toolbar, .catalog-actions { display:flex; flex-wrap:wrap; align-items:center; gap:10px; margin-bottom:14px; }
.catalog-toolbar .el-input { width:310px; max-width:100%; }
.catalog-toolbar .el-select { width:170px; }
.catalog-actions .el-button, .catalog-toolbar .el-button { margin-left:0; }
.catalog-sync { min-height:24px; color:var(--fire-muted); }
.el-pagination { margin-top:16px; overflow:auto; padding-bottom:8px; }
@media(max-width:900px) { .catalog-layout { grid-template-columns:minmax(0, 1fr); } .catalog-directories :deep(.el-tree) { max-height:180px; } }
</style>
