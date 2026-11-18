<template>
  <section class="fire-page fire-theme sms-records" aria-label="真实短信发送记录">
    <div class="fire-page-title"><div><h1>短信发送记录</h1><p>核对收件人、短信内容及平台提交结果</p></div></div>
    <div class="fire-table-card">
      <div class="fire-filters">
        <el-input v-model="keyword" aria-label="搜索短信收件人、号码或场所" clearable placeholder="收件人、号码或场所" @keyup.enter="search" @clear="search" />
        <el-select v-model="status" aria-label="短信提交状态" clearable placeholder="全部提交状态" popper-class="fire-dialog" @change="search"><el-option v-for="(label, value) in smsStatuses" :key="value" :label="label" :value="value" /></el-select>
        <el-date-picker v-model="dates" aria-label="短信发送日期" type="daterange" value-format="YYYY-MM-DD" start-placeholder="开始日期" end-placeholder="结束日期" popper-class="fire-dialog" @change="search" />
        <el-button type="primary" @click="search">查询</el-button><el-button @click="reset">重置</el-button>
      </div>
      <div v-if="error" class="fire-error" role="alert">{{ error }}<el-button link @click="load">重试</el-button></div>
      <el-table v-loading="loading" :data="rows" row-key="recordId" stripe empty-text="暂无短信发送记录">
        <el-table-column prop="createTime" label="提交时间" width="180" />
        <el-table-column prop="recipientName" label="收件人" min-width="110" />
        <el-table-column prop="mobiles" label="接收号码" min-width="160" show-overflow-tooltip />
        <el-table-column prop="sourceName" label="场所 / 岗位" min-width="160" show-overflow-tooltip />
        <el-table-column prop="content" label="短信内容" min-width="230" show-overflow-tooltip />
        <el-table-column label="提交状态" width="150"><template #default="{ row }"><el-tag :type="row.taskStatus === 'failed' ? 'danger' : row.taskStatus === 'submitted' ? 'success' : 'warning'">{{ smsStatus(row.taskStatus) }}</el-tag></template></el-table-column>
        <el-table-column label="送达状态" width="120"><template #default>未收到回执</template></el-table-column>
        <el-table-column prop="senderName" label="发送人" min-width="110" />
        <el-table-column label="操作" fixed="right" width="80"><template #default="{ row }"><el-button link type="primary" @click="detail = row">详情</el-button></template></el-table-column>
      </el-table>
      <el-pagination v-model:current-page="page" :page-size="10" :total="total" layout="total, prev, pager, next" @current-change="commit" />
    </div>
    <el-dialog :model-value="!!detail" title="短信发送详情" width="620px" class="fire-dialog sms-record-detail" append-to-body @update:model-value="detail = null">
      <el-descriptions v-if="detail" :column="1" border>
        <el-descriptions-item label="收件人">{{ detail.recipientName }}</el-descriptions-item><el-descriptions-item label="接收号码">{{ detail.mobiles }}</el-descriptions-item><el-descriptions-item label="场所 / 岗位">{{ detail.sourceName }}</el-descriptions-item>
        <el-descriptions-item label="短信内容"><span class="sms-record-content">{{ detail.content }}</span></el-descriptions-item>
        <el-descriptions-item label="提交结果">{{ smsStatus(detail.taskStatus) }}<p>{{ detail.resultText }}</p></el-descriptions-item>
        <el-descriptions-item label="已受理号码">{{ detail.acceptedPhones || '暂无' }}</el-descriptions-item><el-descriptions-item label="送达状态">未收到平台回执</el-descriptions-item>
        <el-descriptions-item label="平台任务编号">{{ detail.platformTaskId || '暂无' }}</el-descriptions-item><el-descriptions-item label="发送人 / 时间">{{ detail.senderName }} / {{ detail.createTime }}</el-descriptions-item>
      </el-descriptions>
      <template #footer><el-button @click="detail = null">关闭</el-button></template>
    </el-dialog>
  </section>
</template>
<script setup>
import { onBeforeUnmount, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { listSmsRecords } from '@/api/fire/sms'
import { smsStatuses, smsStatus } from '@/api/fire/smsModel'
const route = useRoute(), router = useRouter()
const keyword = ref(''), status = ref(''), dates = ref([]), page = ref(1), rows = ref([]), total = ref(0), loading = ref(false), error = ref(''), detail = ref(null)
let generation = 0
async function load() {
  const current = ++generation; loading.value = true; error.value = ''; rows.value = []
  try { const response = await listSmsRecords({ keyword: keyword.value, taskStatus: status.value, beginTime: dates.value?.[0], endTime: dates.value?.[1], pageNum: page.value, pageSize: 10 }); if (current === generation) { rows.value = response.rows || []; total.value = response.total || 0 } }
  catch (e) { if (current === generation) error.value = e.message || '短信记录加载失败，请重试' }
  finally { if (current === generation) loading.value = false }
}
function commit() {
  const query = { ...route.query, keyword: keyword.value, taskStatus: status.value, beginTime: dates.value?.[0], endTime: dates.value?.[1], pageNum: page.value > 1 ? String(page.value) : undefined }
  for (const key of Object.keys(query)) if (!query[key]) delete query[key]
  const stable = value => JSON.stringify(Object.entries(value).sort(([a], [b]) => a.localeCompare(b)))
  if (stable(query) === stable(route.query)) load(); else router.replace({ query })
}
function search() { page.value = 1; commit() }
function reset() { keyword.value = ''; status.value = ''; dates.value = []; search() }
watch(() => route.query, q => { keyword.value = String(q.keyword || ''); status.value = String(q.taskStatus || ''); dates.value = q.beginTime && q.endTime ? [q.beginTime, q.endTime] : []; page.value = Math.max(1, Number(q.pageNum) || 1); load() }, { immediate: true })
onBeforeUnmount(() => ++generation)
</script>
<style scoped>
.sms-records :deep(.el-pagination) { margin-top: 16px; justify-content: flex-end; }
.sms-record-content { white-space: pre-wrap; overflow-wrap: anywhere; }
</style>
