<template>
  <el-dialog v-model="visible" title="导入重点场所" class="fire-dialog" width="min(1080px, 95vw)" append-to-body
    :close-on-click-modal="false" :close-on-press-escape="!busy" :show-close="!busy" @closed="reset">
    <p class="import-guidance">仅新增场所，同名或近似名称自动跳过，不覆盖已有档案。读取“场所类别”列：重点企业、高层建筑、综合商业体、养老机构。</p>
    <template v-if="!result">
      <el-button link type="primary" :disabled="busy" @click="downloadTemplate">下载标准模板</el-button>
      <el-upload ref="uploader" :auto-upload="false" :limit="1" accept=".xlsx" :disabled="busy" drag
        :on-change="choose" :on-remove="removeFile" :on-exceed="() => error = '请先移除已选文件，再选择新的Excel'">
        <el-icon><UploadFilled /></el-icon><div>拖入单页 Excel，或点击选择文件</div>
        <template #tip><div class="el-upload__tip">支持标准 .xlsx，最大10MB、2000条。第一行是表头；右侧地址查询说明不会作为经纬度导入。</div></template>
      </el-upload>
      <p v-if="busy" role="status">正在读取文件并检查数据库中的相似名称…</p>
    </template>
    <template v-else>
      <div class="import-summary" role="status" aria-live="polite">
        <strong>{{ complete ? '导入完成' : '预检结果' }} · {{ result.summary.total }} 条</strong>
        <span>{{ complete ? '已新增' : '可新增' }} <b>{{ complete ? result.summary.inserted : result.summary.new }}</b></span>
        <span>重复跳过 <b>{{ result.summary.duplicate }}</b></span><span>格式错误 <b>{{ result.summary.invalid }}</b></span>
      </div>
      <p class="import-guidance">{{ result.summary.withCoordinates }} 条有坐标，将在对应地图图层显示（定位待核验）；{{ result.summary.withoutCoordinates }} 条无坐标，保留在重点场所列表。{{ complete ? '' : '预检有效期2小时，确认时再次检查重复。' }}</p>
      <el-radio-group v-model="filter" aria-label="筛选导入结果" class="import-filter">
        <el-radio-button value="all">全部</el-radio-button><el-radio-button :value="complete ? 'inserted' : 'new'">{{ complete ? '已新增' : '可新增' }}</el-radio-button>
        <el-radio-button value="duplicate">重复跳过</el-radio-button><el-radio-button value="invalid">格式错误</el-radio-button>
      </el-radio-group>
      <el-table :data="pageRows" max-height="360" row-key="rowNumber" empty-text="此分类没有记录" aria-label="场所导入检查结果">
        <el-table-column prop="rowNumber" label="原表行号" width="90" />
        <el-table-column prop="name" label="对象名称" min-width="220" show-overflow-tooltip />
        <el-table-column prop="category" label="场所类别" width="120" />
        <el-table-column label="坐标" width="100"><template #default="{ row }">{{ hasCoordinates(row) ? '有坐标' : '无坐标' }}</template></el-table-column>
        <el-table-column label="处理结果" width="105"><template #default="{ row }"><el-tag :type="tagType(row.status)">{{ statusText(row.status) }}</el-tag></template></el-table-column>
        <el-table-column label="原因 / 相似名称" min-width="300"><template #default="{ row }">{{ row.reason || '校验通过，等待确认新增' }}<span v-if="row.matches?.length"> · {{ row.matches.join('、') }}</span></template></el-table-column>
      </el-table>
      <el-pagination v-model:current-page="page" :page-size="20" :total="filtered.length" layout="total, prev, pager, next" />
    </template>
    <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon role="alert" />
    <template #footer>
      <el-button :disabled="busy" @click="visible = false">{{ complete ? '关闭' : '取消' }}</el-button>
      <el-button v-if="result" :disabled="busy" @click="downloadReport">下载检查结果</el-button>
      <el-button v-if="result && !complete" :disabled="busy" @click="restart">重新选文件</el-button>
      <el-button v-if="!result" type="primary" :loading="busy" :disabled="!file || busy" @click="preview">检查文件</el-button>
      <el-button v-else-if="!complete" type="primary" :loading="busy" :disabled="busy || !result.summary.new" @click="confirm">确认新增 {{ result.summary.new }} 条</el-button>
    </template>
  </el-dialog>
</template>
<script setup>
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { saveAs } from 'file-saver'
import { download } from '@/utils/request'
import { UploadFilled } from '@element-plus/icons-vue'
import { previewPlaceImport, confirmPlaceImport } from '@/api/fire/placeImport'
const emit = defineEmits(['success'])
const visible = ref(false), busy = ref(false), file = ref(null), uploader = ref(null), result = ref(null), error = ref(''), filter = ref('all'), page = ref(1)
let revision = 0
const complete = computed(() => result.value?.state === 'complete')
const filtered = computed(() => (result.value?.rows || []).filter(row => filter.value === 'all' || row.status === filter.value))
const pageRows = computed(() => filtered.value.slice((page.value - 1) * 20, page.value * 20))
watch(filter, () => page.value = 1)
const hasCoordinates = row => row.longitude !== null && row.longitude !== undefined && row.latitude !== null && row.latitude !== undefined
const statusText = s => ({ new: '可新增', duplicate: '重复跳过', invalid: '格式错误', inserted: '已新增' }[s] || s)
const tagType = s => ({ new: 'success', duplicate: 'warning', invalid: 'danger', inserted: 'success' }[s] || 'info')
async function downloadTemplate() {
  try { await download('/fire/places/import/template', {}, '重点场所标准模板.xlsx') }
  catch (e) { error.value = e.message || '模板下载失败，请重试' }
}
function downloadReport() {
  const safe = value => '"' + String(value ?? '').replace(/^[=+@\-\t\r]/, "'$&").replaceAll('"', '""') + '"'
  const lines = [['原表行号', '对象名称', '场所类别', '处理结果', '原因', '相似名称'], ...result.value.rows.map(r => [r.rowNumber, r.name, r.category, statusText(r.status), r.reason, r.matches?.join('、')])]
  saveAs(new Blob(['\ufeff' + lines.map(row => row.map(safe).join(',')).join('\r\n')], { type: 'text/csv;charset=utf-8' }), '重点场所导入检查结果.csv')
}
function reset() { revision++; file.value = null; result.value = null; error.value = ''; filter.value = 'all'; page.value = 1; uploader.value?.clearFiles() }
function open() { reset(); visible.value = true }
function removeFile() { file.value = null; error.value = '' }
function choose(selected) {
  error.value = ''
  if (!selected.name?.toLowerCase().endsWith('.xlsx') || selected.size > 10 * 1024 * 1024) { file.value = null; uploader.value?.clearFiles(); error.value = '请选择不超过10MB的标准.xlsx文件'; return }
  file.value = selected.raw
}
function restart() { reset() }
async function preview() {
  if (!file.value || busy.value) return
  const current = ++revision; busy.value = true; error.value = ''
  try { const response = await previewPlaceImport(file.value); if (current === revision) result.value = response.data }
  catch (e) { if (current === revision) error.value = e.message || '预检失败，文件已保留，请重试' }
  finally { if (current === revision) busy.value = false }
}
async function confirm() {
  if (busy.value || !result.value?.summary.new || complete.value) return
  const current = ++revision; busy.value = true; error.value = ''
  try { const response = await confirmPlaceImport(result.value.batchId); if (current === revision) { result.value = response.data; filter.value = 'all'; page.value = 1; emit('success') } }
  catch (e) { if (current === revision) error.value = e.message || '导入未完成，可重试；同一批次不会重复新增' }
  finally { if (current === revision) busy.value = false }
}
onBeforeUnmount(() => revision++)
defineExpose({ open })
</script>
<style scoped>
.import-guidance { color: var(--fire-muted, #9db3c9); line-height: 1.8; margin: 0 0 16px; }
.import-summary { display: flex; gap: 20px; align-items: center; flex-wrap: wrap; padding: 14px 0; border-bottom: 1px solid var(--fire-line, #25455b); margin-bottom: 12px; }
.import-summary b { font-variant-numeric: tabular-nums; }
.import-filter { margin-bottom: 14px; }
:deep(.el-pagination) { margin: 16px 0; }
:deep(.el-upload-dragger) { padding: 25px 12px; }
</style>
