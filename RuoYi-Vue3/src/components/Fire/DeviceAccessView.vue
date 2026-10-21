<template>
  <div class="fire-page fire-theme">
    <div class="fire-page-title">
      <div><div class="fire-kicker">{{ eventOnly ? 'INCIDENT RECORDS' : 'DEVICE MONITORING' }}</div><h1>{{ eventOnly ? '消防警情记录' : '消控主机与传输装置' }}</h1>
        <p>{{ eventOnly ? '大厂街道119警情与设备上报 · 保留来源和原始状态' : '真实设备上报 · 通信、设备状态与火警分别记录' }}</p></div>
      <div class="fire-toolbar"><router-link to="/fire/dashboard">指挥大屏 ↗</router-link><el-button :loading="loading" @click="load">刷新记录</el-button></div>
    </div>
    <el-tabs v-model="tab" @tab-change="changeTab">
      <el-tab-pane v-if="!eventOnly" label="传输装置" name="gateways" />
      <el-tab-pane v-if="!eventOnly" label="消防主机" name="hosts" />
      <el-tab-pane v-if="canFire('alarms')" :label="eventOnly ? '消防警情' : '设备事件'" name="alarms" />
      <el-tab-pane v-if="!eventOnly" label="通信报文" name="protocolMessages" />
    </el-tabs>
    <div class="fire-table-card" :aria-busy="loading">
      <el-form inline novalidate @submit.prevent="search">
        <el-form-item :label="eventOnly ? '地址或内容' : '单位或装置'"><el-input v-model="keyword" clearable placeholder="输入名称后查询" @clear="search" /></el-form-item>
        <el-form-item v-if="eventOnly" label="接入来源"><el-select v-model="source" clearable placeholder="全部来源" style="width:160px" @change="changeSource">
          <el-option label="119警情" value="119" /><el-option label="消控室主机" value="device" />
        </el-select></el-form-item>
        <el-form-item v-if="eventOnly && source === '119'" label="案件类型"><el-select v-model="caseType" clearable placeholder="全部案件类型" style="width:160px" @change="search">
          <el-option label="火警扑救" value="1" /><el-option label="抢险救援" value="2" /><el-option label="社会救助" value="3" />
        </el-select></el-form-item>
        <el-form-item v-if="tab === 'alarms' && source !== '119'" label="设备事件类型"><el-select v-model="kind" clearable placeholder="全部类型" style="width:160px" @change="search">
          <el-option label="火警状态上报" value="fire" /><el-option label="手动报警操作" value="manual_alarm" /><el-option label="故障状态上报" value="fault" />
          <el-option label="状态上报" value="status" /><el-option label="操作记录" value="operation" /><el-option label="模拟量" value="measurement" />
          <el-option label="版本" value="version" /><el-option label="配置" value="configuration" /><el-option label="设备时间" value="clock" />
        </el-select></el-form-item>
        <el-form-item><el-button native-type="submit" type="primary" :loading="loading">查询</el-button></el-form-item>
      </el-form>
      <div v-if="error" class="fire-error" role="alert">{{ error }}<el-button link @click="load">重试</el-button></div>
      <p v-if="tab === 'hosts'" class="muted">仅显示实际报文识别的下接系统。传输装置在线不代表消防主机正常；未知字段显示“—”。</p>
      <p v-if="tab === 'alarms'" class="muted">119按来警时间、设备按接收时间统一排序。案件状态来自119平台；设备事件不代表现场火情仍在持续。</p>
      <p v-if="eventOnly" class="muted" role="status">{{ syncNote }}</p>
      <el-table v-loading="loading" :data="rows" row-key="id" stripe empty-text="暂无符合条件的真实记录">
        <el-table-column v-for="column in columns" :key="column.key" :prop="column.key" :label="column.label" :min-width="column.width || 160" show-overflow-tooltip>
          <template #default="{ row }"><StatusTag v-if="column.key === 'status'" :value="row.status" /><span v-else>{{ row[column.key] ?? '—' }}</span></template>
        </el-table-column>
        <el-table-column v-if="tab === 'alarms'" label="时间核验" width="125"><template #default="{row}">{{ row.sourceCode === '119' ? '源来警时间' : row.clockWarning ? '设备时间待核验' : '—' }}</template></el-table-column>
        <el-table-column label="操作" width="90" fixed="right"><template #default="{row}"><el-button link type="primary" @click="show(row)">详情</el-button></template></el-table-column>
      </el-table>
      <el-pagination v-model:current-page="page" :page-size="20" :total="total" layout="total, prev, pager, next" @current-change="load" />
    </div>
    <AlarmDetail v-model="eventOpen" :alarm-id="selectedId" />
    <el-dialog v-model="detailOpen" title="设备接入记录" class="fire-dialog" width="min(850px, 94vw)" append-to-body>
      <el-descriptions :column="1" border><el-descriptions-item v-for="column in columns" :key="column.key" :label="column.label">{{ selected?.[column.key] ?? '—' }}</el-descriptions-item></el-descriptions>
      <template v-if="tab === 'protocolMessages'"><h3>原始十六进制</h3><pre class="device-raw">{{ selected?.rawHex }}</pre><p>{{ selected?.error || '' }}</p><pre class="device-raw">{{ selected?.parsed || '' }}</pre></template>
    </el-dialog>
  </div>
</template>
<script setup>
import { ref, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { fire } from '@/api/fire'
import { incident119Status } from '@/api/fire/deviceAccess'
import { canFire } from '@/api/fire/access'
import StatusTag from './StatusTag.vue'
import AlarmDetail from './AlarmDetail.vue'
const props = defineProps({ eventOnly: Boolean })
const route = useRoute(), router = useRouter()
const allowed = props.eventOnly ? ['alarms'] : ['gateways', 'hosts', 'alarms', 'protocolMessages']
const tab = ref(allowed.includes(route.query.tab) ? route.query.tab : allowed[0])
const keyword = ref(String(route.query.keyword || '')), kind = ref(String(route.query.kind || ''))
const source = ref(props.eventOnly ? String(route.query.source || '') : 'device'), caseType = ref(String(route.query.caseType || '')), syncStatus = ref(null)
const page = ref(Math.max(1, Number(route.query.page) || 1)), rows = ref([]), total = ref(0), loading = ref(false), error = ref('')
const detailOpen = ref(false), eventOpen = ref(false), selected = ref(null), selectedId = ref('')
let revision = 0, timer
const definitions = {
 gateways: [['name','装置名称',230],['placeName','所属单位'],['model','型号'],['serial','协议地址'],['status','通信状态'],['deviceStatus','装置上报状态',260],['lastSeen','最后有效报文',190]],
 hosts: [['name','主机/系统',220],['placeName','所属单位'],['gatewayName','传输装置',220],['serial','系统类型/地址'],['status','装置通信状态'],['deviceStatus','主机上报状态',260],['lastSeen','最后接收时间',190]],
 alarms: [['name','事件内容',300],['placeName','所属单位'],['gatewayName','上报装置',220],['occurredAt','服务器接收时间',190],['deviceTime','设备事件时间',190],['status','记录状态']],
 protocolMessages: [['direction','方向'],['sessionId','连接编号',220],['peerIp','设备IP'],['sequenceNo','流水号'],['commandNo','命令'],['receivedAt','记录时间',190],['parseStatus','解析结果'],['transmissionStatus','发送结果']],
}
const columns = computed(() => (props.eventOnly ? [['source','来源',140],['name','警情内容',280],['caseTypeName','案件类型',130],['caseNatureName','案件性质',120],['placeName','案发地址或单位',260],['occurredAt','来警或接收时间',190],['status','案件或记录状态',150]] : definitions[tab.value]).map(([key,label,width]) => ({key,label,width})))
const syncNote = computed(() => {
 const s=syncStatus.value
 if(!s) return '119同步状态暂不可用，可刷新重试；下表为当前已入库记录'
 if(!s.enabled) return '119同步未启用，当前显示已入库记录'
 if(['failed','partial'].includes(s.lastResult)) return `119最近同步未完整成功；最近完整成功：${s.lastCompletedAt || '尚未完成'}。请检查后端同步日志。`
 return `119每${s.intervalSeconds || 60}秒同步 · 大厂街道内 ${s.insideCount ?? 0} 条 · 待判定 ${s.pendingCount ?? 0} 条 · 最近完整成功：${s.lastCompletedAt || '尚未完成首轮'}`
})
async function load() {
 const current = ++revision
 loading.value = true; error.value = ''
 try {
  const [res, state] = await Promise.all([fire.list(tab.value, { pageNum: page.value, pageSize:20, keyword:keyword.value, kind:tab.value==='alarms'?kind.value:'', source:tab.value==='alarms'?source.value:'', caseType:caseType.value }), props.eventOnly ? incident119Status().catch(() => null) : Promise.resolve(null)])
  if(current!==revision) return
  syncStatus.value=state
  if(page.value>1 && (page.value-1)*20>=res.total){page.value=Math.max(1,Math.ceil(res.total/20));persist();load();return}
  rows.value=res.rows;total.value=res.total
 } catch(e) { if(current===revision) { rows.value=[];total.value=0;error.value=e.message || '设备记录加载失败' } }
 finally {if(current===revision)loading.value=false}
}
function persist() { router.replace({ query:{...route.query,tab:tab.value,keyword:keyword.value || undefined,kind:kind.value || undefined,source:source.value || undefined,caseType:caseType.value || undefined,page:String(page.value)} }) }
function search() { page.value=1;persist();load() }
function changeSource() { kind.value='';caseType.value='';search() }
function changeTab() { detailOpen.value=false;eventOpen.value=false;rows.value=[];total.value=0;search() }
function show(row) { if(tab.value==='alarms') {selectedId.value=row.id;eventOpen.value=true} else {selected.value=row;detailOpen.value=true} }
watch(page, persist)
watch(() => route.query, q => {
 const nextTab=allowed.includes(q.tab)?q.tab:allowed[0], nextPage=Math.max(1,Number(q.page)||1)
 const nextKeyword=String(q.keyword||''), nextKind=String(q.kind||''), nextSource=props.eventOnly?String(q.source||''):'device', nextCase=String(q.caseType||'')
 if(tab.value===nextTab && page.value===nextPage && keyword.value===nextKeyword && kind.value===nextKind && source.value===nextSource && caseType.value===nextCase)return
 tab.value=nextTab;page.value=nextPage;keyword.value=nextKeyword;kind.value=nextKind;source.value=nextSource;caseType.value=nextCase
 detailOpen.value=false;eventOpen.value=false;load()
})
onMounted(() => {load();timer=setInterval(() => { if(!loading.value && !error.value && !detailOpen.value && !eventOpen.value)load() },5000)})
onBeforeUnmount(() => { revision++;clearInterval(timer) })
</script>
<style scoped>
.device-raw { white-space:pre-wrap;overflow-wrap:anywhere;max-height:320px;overflow:auto; }
</style>
