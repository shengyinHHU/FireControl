<template>
  <el-dialog v-model="visible" title="导入监控坐标" class="fire-dialog" width="min(1120px, 95vw)" append-to-body
    :close-on-click-modal="false" :close-on-press-escape="!busy" :show-close="!busy" @closed="reset">
    <p>按视频来源和设备编码匹配，仅更新位置资料。空白坐标不清除已有位置；不新增设备，不修改平台名称。</p>
    <template v-if="!result">
      <el-radio-group v-model="mode" aria-label="坐标导入方式" :disabled="busy">
        <el-radio value="fill">仅补空坐标（推荐）</el-radio><el-radio value="overwrite">允许覆盖已有坐标</el-radio>
      </el-radio-group>
      <el-alert v-if="mode === 'overwrite'" title="将允许更新已有位置，请在预检中核对原坐标和新坐标。" type="warning" :closable="false" show-icon />
      <el-upload ref="uploader" :auto-upload="false" :limit="1" accept=".xlsx" :disabled="busy" drag
        :on-change="choose" :on-remove="() => file = null" :on-exceed="() => error = '请先移除已选文件'">
        <el-icon><UploadFilled /></el-icon><div>拖入调查表，或点击选择 Excel</div>
        <template #tip><div class="el-upload__tip">使用“导出未定位”或“导出当前筛选”取得调查表。仅.xlsx，最大10MB、50000条；填写真实坐标系，不接受公式。</div></template>
      </el-upload>
    </template>
    <template v-else>
      <div class="video-import-summary" role="status" aria-live="polite">
        <strong>{{ complete ? '导入完成' : '预检结果' }} · {{ result.summary.total }} 条</strong>
        <span>{{ complete ? '已更新' : '可更新' }} {{ result.summary[complete ? 'updated' : 'update'] }}</span>
        <span>跳过 {{ result.summary.skipped }} · 未变化 {{ result.summary.unchanged }}</span>
        <span>错误 {{ result.summary.invalid }} · 冲突 {{ result.summary.conflict }}</span>
      </div>
      <p>{{ complete ? '结果已保存，可下载逐行处理报告。' : '只提交“可更新”行，错误和冲突行不会写入。预检有效期2小时，提交时再次核对版本。' }}</p>
      <el-select v-model="filter" aria-label="筛选导入结果" popper-class="fire-dialog">
        <el-option label="全部结果" value="all" /><el-option label="可更新" value="update" /><el-option label="已更新" value="updated" />
        <el-option label="格式错误" value="invalid" /><el-option label="版本冲突" value="conflict" /><el-option label="跳过或未变化" value="skipped" />
      </el-select>
      <el-table :data="pageRows" row-key="rowNumber" max-height="360" empty-text="此分类没有记录" aria-label="监控坐标预检结果">
        <el-table-column prop="rowNumber" label="Excel行号" width="100" /><el-table-column prop="name" label="监控名称" min-width="170" show-overflow-tooltip />
        <el-table-column prop="deviceCode" label="设备编码" min-width="185" show-overflow-tooltip />
        <el-table-column label="原坐标 → 新坐标（GCJ-02）" min-width="285"><template #default="{ row }">{{ point(row.oldLongitude, row.oldLatitude) }} → {{ point(row.longitude, row.latitude) }}</template></el-table-column>
        <el-table-column label="处理结果" width="110"><template #default="{ row }"><el-tag :type="['invalid', 'conflict'].includes(row.status) ? 'danger' : 'info'">{{ statusText(row.status) }}</el-tag></template></el-table-column>
        <el-table-column prop="reason" label="原因" min-width="240" />
      </el-table>
      <el-pagination v-model:current-page="page" :page-size="20" :total="filtered.length" layout="total, prev, pager, next" />
    </template>
    <p v-if="busy" role="status">{{ result ? '正在写入坐标，请稍候…' : '正在读取调查表并校验设备…' }}</p>
    <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon role="alert" />
    <template #footer>
      <el-button :disabled="busy" @click="visible = false">{{ complete ? '关闭' : '取消' }}</el-button>
      <el-button v-if="result" :disabled="busy" @click="report">下载处理报告</el-button>
      <el-button v-if="result && !complete" :disabled="busy" @click="restart">重新选文件</el-button>
      <el-button v-if="!result" type="primary" :loading="busy" :disabled="!file || busy" @click="preview">检查文件</el-button>
      <el-button v-else-if="!complete" type="primary" :loading="busy" :disabled="busy || !result.summary.update" @click="confirm">确认更新 {{ result.summary.update }} 条</el-button>
    </template>
  </el-dialog>
</template>
<script setup>
import { computed, ref, watch, onBeforeUnmount } from 'vue'
import { UploadFilled } from '@element-plus/icons-vue'
import { saveAs } from 'file-saver'
import { previewVideoCoordinates, confirmVideoCoordinates } from '@/api/fire/videoCatalog'
const emit = defineEmits(['success'])
const visible = ref(false), busy = ref(false), file = ref(null), uploader = ref(), result = ref(null), error = ref(''), mode = ref('fill'), filter = ref('all'), page = ref(1)
let version = 0
const complete = computed(() => result.value?.state === 'complete')
const filtered = computed(() => (result.value?.rows || []).filter(r => filter.value === 'all' || (filter.value === 'skipped' ? ['skipped', 'unchanged'].includes(r.status) : r.status === filter.value)))
const pageRows = computed(() => filtered.value.slice((page.value - 1) * 20, page.value * 20))
watch(filter, () => page.value = 1)
const statusText = s => ({ update: '可更新', updated: '已更新', skipped: '已跳过', unchanged: '未变化', invalid: '格式错误', conflict: '版本冲突' }[s] || s)
const point = (lng, lat) => lng === null || lng === undefined ? '未填写' : `${lng}, ${lat}`
function choose(upload) {
  error.value = ''
  if (!upload.name.toLowerCase().endsWith('.xlsx') || upload.size > 10 * 1024 * 1024) { error.value = '请选择不超过10MB的.xlsx文件'; file.value = null; uploader.value?.clearFiles(); return }
  file.value = upload.raw
}
async function preview() {
  const current = ++version; busy.value = true; error.value = ''
  try { const r = await previewVideoCoordinates(file.value, mode.value); if (current === version) result.value = r.data }
  catch (e) { if (current === version) error.value = e.message || '预检失败，请重试' }
  finally { if (current === version) busy.value = false }
}
async function confirm() {
  const current = ++version; busy.value = true; error.value = ''
  try { const r = await confirmVideoCoordinates(result.value.batchId); if (current === version) { result.value = r.data; filter.value = 'all'; page.value = 1; emit('success') } }
  catch (e) { if (current === version) error.value = e.message || '提交失败，请重试；版本冲突请重新预检' }
  finally { if (current === version) busy.value = false }
}
function report() {
  const cell = value => '"' + String(value ?? '').replace(/^[=+@\-\t\r]/, "'$&").replaceAll('"', '""') + '"'
  const rows = [['Excel行号', '设备编码', '监控名称', '原经度', '原纬度', '新经度GCJ02', '新纬度GCJ02', '处理结果', '原因'], ...result.value.rows.map(r => [r.rowNumber, r.deviceCode, r.name, r.oldLongitude, r.oldLatitude, r.longitude, r.latitude, statusText(r.status), r.reason])]
  saveAs(new Blob(['\ufeff' + rows.map(r => r.map(cell).join(',')).join('\r\n')], { type: 'text/csv;charset=utf-8' }), '监控坐标处理报告.csv')
}
function reset() { version++; file.value = null; result.value = null; error.value = ''; page.value = 1; filter.value = 'all'; mode.value = 'fill'; uploader.value?.clearFiles() }
function restart() { reset() }
function open() { reset(); visible.value = true }
onBeforeUnmount(() => version++)
defineExpose({ open, isBusy: () => busy.value })
</script>
<style scoped>
.video-import-summary { display:flex; flex-wrap:wrap; gap:12px; padding:14px 0; color:var(--fire-text); }
.el-select { width:220px; margin-bottom:12px; }
.el-upload { margin-top:16px; }
p { color:var(--el-text-color-regular); line-height:1.6; }
</style>
