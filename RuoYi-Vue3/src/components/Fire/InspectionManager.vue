<template>
 <section :class="['inspection-manager',{compact}]" aria-label="真实查岗管理">
  <p v-if="!canInspection()" class="fire-empty">暂无查岗查看权限，请联系管理员。</p>
  <template v-else>
   <template v-if="!compact"><h1>查岗管理</h1><el-tabs v-model="tab" @tab-change="changeTab"><el-tab-pane label="岗位查岗" name="posts" /><el-tab-pane label="查岗记录" name="records" /><el-tab-pane v-if="canInspection('edit')" label="岗位配置" name="config" /></el-tabs></template>
   <div class="inspection-toolbar">
    <el-input v-model="keyword" clearable :placeholder="tab==='records'?'搜索岗位、机构或查岗人':'搜索岗位或机构'" aria-label="查岗搜索" @keyup.enter="search" @clear="search" />
    <el-select v-if="tab==='config'" v-model="status" clearable placeholder="全部状态" aria-label="岗位状态" popper-class="fire-dialog" @change="search"><el-option label="启用" value="0" /><el-option label="停用" value="1" /></el-select>
    <el-select v-if="tab==='records'" v-model="recordResult" clearable placeholder="全部结论" aria-label="查岗结果" popper-class="fire-dialog" @change="search"><el-option v-for="key in ['present','absent','unknown']" :key="key" :label="inspectionResult(key)" :value="key" /></el-select>
    <el-date-picker v-if="tab==='records'" v-model="dates" type="daterange" value-format="YYYY-MM-DD" start-placeholder="开始日期" end-placeholder="结束日期" @change="search" />
    <el-button :loading="loading" @click="search">查询</el-button>
    <el-button v-if="tab==='config'" @click="edit()">新增岗位</el-button>
   </div>
   <p v-if="error" class="fire-error" role="alert">{{ error }} <el-button link @click="load">重试</el-button></p>
   <div v-if="compact" v-loading="loading" class="inspection-cards">
    <article v-for="row in rows" :key="row.postId" class="inspection-item"><button type="button" class="inspection-open" :disabled="!canVideo('preview')" @click="inspect(row)"><strong>{{ row.postName }}</strong><span>{{ row.placeName || '未关联机构' }}</span><small>最近查岗：{{ row.lastTime || '尚无记录' }}</small><span><StatusTag v-if="row.lastResult" :value="inspectionResult(row.lastResult)" /><small v-else>尚未查岗</small> · {{ row.cameraCount }} 台摄像头 · 开始查岗 ›</span></button><InspectionContact :name="row.contactName" :phone="row.contactPhone" /></article>
    <p v-if="!loading&&!error&&!rows.length" class="fire-empty">暂无启用的查岗岗位，请在管理界面配置。</p>
   </div>
   <el-table v-else v-loading="loading" :data="rows" border empty-text="暂无真实查岗数据">
    <el-table-column prop="postName" label="岗位" min-width="180" /><el-table-column prop="placeName" label="所属机构" min-width="160" />
    <template v-if="tab==='records'"><el-table-column prop="inspectorName" label="查岗人" width="110" /><el-table-column prop="inspectTime" label="查岗时间" width="175" /><el-table-column label="结论" width="110"><template #default="{row}"><StatusTag :value="inspectionResult(row.result)" /></template></el-table-column><el-table-column prop="note" label="说明" min-width="150" show-overflow-tooltip /></template>
    <template v-else><el-table-column label="联系人及电话" min-width="240"><template #default="{row}"><InspectionContact :name="row.contactName" :phone="row.contactPhone" /></template></el-table-column><el-table-column prop="cameraCount" label="摄像头数" width="100" /><el-table-column prop="lastTime" label="最近查岗" width="175" /><el-table-column label="最近结论" width="115"><template #default="{row}"><StatusTag v-if="row.lastResult" :value="inspectionResult(row.lastResult)" /><span v-else>尚未查岗</span></template></el-table-column><el-table-column v-if="tab==='config'" label="状态" width="85"><template #default="{row}">{{ row.status==='0'?'启用':'停用' }}</template></el-table-column></template>
    <el-table-column label="操作" :width="tab==='config'?180:120" fixed="right"><template #default="{row}"><el-button v-if="tab==='records'" link @click="detail(row)">查看记录</el-button><template v-else><el-button link :disabled="row.status!=='0'||!canVideo('preview')" @click="inspect(row)">开始查岗</el-button><el-button v-if="tab==='config'" link @click="edit(row)">配置</el-button></template></template></el-table-column>
   </el-table>
   <div v-if="!compact" class="inspection-pagination"><el-pagination v-model:current-page="page" :page-size="10" :total="total" layout="total, prev, pager, next" @current-change="load" /></div>
  </template>
  <InspectionSession v-model="sessionOpen" :post-id="activePost" @saved="load" />
  <InspectionPostEditor v-model="editorOpen" :post-id="editorId" @saved="load" />
  <el-dialog v-model="recordOpen" title="历史查岗记录" width="780px" class="fire-dialog" append-to-body @closed="onRecordClosed">
   <p v-if="detailError" class="fire-error" role="alert">{{ detailError }}</p>
   <el-descriptions v-if="record" :column="1" border><el-descriptions-item label="岗位">{{ record.postName }}</el-descriptions-item><el-descriptions-item label="所属机构">{{ record.placeName || '未关联机构' }}</el-descriptions-item><el-descriptions-item label="摄像头">{{ record.deviceName }}（{{ record.deviceCode }}）</el-descriptions-item><el-descriptions-item label="查岗人">{{ record.inspectorName }}</el-descriptions-item><el-descriptions-item label="查岗时间">{{ record.inspectTime }}</el-descriptions-item><el-descriptions-item label="结论">{{ inspectionResult(record.result) }}</el-descriptions-item><el-descriptions-item label="说明">{{ record.note || '未填写' }}</el-descriptions-item></el-descriptions>
   <p v-if="record">以下为本次查岗留存的历史证据。</p><img v-if="recordImage" :src="recordImage" class="record-evidence" alt="该次查岗保存的历史抓拍" /><p v-else-if="record&&!Number(record.hasEvidence)" class="muted">该记录未留存抓拍。</p>
   <template #footer><el-button @click="recordOpen=false">关闭</el-button><el-button v-if="record&&canVideo('preview')" @click="viewCurrent">查看当前岗位视频</el-button></template>
  </el-dialog>
 </section>
</template>
<script setup>
import {ref,onMounted,onBeforeUnmount} from 'vue'
import StatusTag from './StatusTag.vue'
import InspectionContact from './InspectionContact.vue'
import InspectionSession from './InspectionSession.vue'
import InspectionPostEditor from './InspectionPostEditor.vue'
import {canVideo} from '@/api/fire/video'
import {canInspection,inspectionPosts,inspectionRecords,inspectionRecord,inspectionEvidence,inspectionResult} from '@/api/fire/inspection'
const props=defineProps({compact:Boolean})
const tab=ref('posts'),keyword=ref(''),status=ref(''),recordResult=ref(''),dates=ref([]),page=ref(1),rows=ref([]),total=ref(0),loading=ref(false),error=ref('')
const sessionOpen=ref(false),activePost=ref(),editorOpen=ref(false),editorId=ref(),recordOpen=ref(false),record=ref(),recordImage=ref(''),detailError=ref('')
let generation=0,detailVersion=0,timer
async function load(){if(!canInspection())return;const current=++generation;loading.value=true;error.value='';rows.value=[];try{const response=await(tab.value==='records'?inspectionRecords({pageNum:page.value,pageSize:10,keyword:keyword.value,result:recordResult.value,beginTime:dates.value?.[0],endTime:dates.value?.[1]}):inspectionPosts({pageNum:page.value,pageSize:props.compact?8:10,keyword:keyword.value,status:tab.value==='config'?status.value:'0'}));if(current!==generation)return;rows.value=response.rows||[];total.value=response.total||0}catch(e){if(current===generation)error.value=e.message||'查岗数据加载失败'}finally{if(current===generation)loading.value=false}}
function search(){page.value=1;load()}
function changeTab(){keyword.value='';status.value='';recordResult.value='';dates.value=[];search()}
function inspect(row){activePost.value=row.postId;sessionOpen.value=true}
function edit(row){editorId.value=row?.postId;editorOpen.value=true}
function onRecordClosed(){if(!recordOpen.value)clearImage()}
function clearImage(){detailVersion++;if(recordImage.value)URL.revokeObjectURL(recordImage.value);recordImage.value=''}
async function detail(row){clearImage();const current=detailVersion;record.value=null;detailError.value='';recordOpen.value=true;try{const {data}=await inspectionRecord(row.recordId);if(current!==detailVersion||!recordOpen.value)return;record.value=data;if(Number(data.hasEvidence)){const blob=await inspectionEvidence(data.recordId);if(current===detailVersion&&recordOpen.value)recordImage.value=URL.createObjectURL(blob)}}catch(e){if(current===detailVersion)detailError.value=e.message||'记录或证据加载失败'}}
function viewCurrent(){const id=record.value.postId;recordOpen.value=false;activePost.value=id;sessionOpen.value=true}
onMounted(()=>{load();if(props.compact)timer=setInterval(()=>{if(!sessionOpen.value&&!editorOpen.value&&!loading.value)load()},30000)})
onBeforeUnmount(()=>{generation++;clearImage();clearInterval(timer)})
defineExpose({refresh:load})
</script>
<style scoped>
.inspection-manager:not(.compact){padding:20px}.inspection-toolbar{display:flex;gap:8px;flex-wrap:wrap;margin-bottom:12px}.inspection-toolbar .el-input{max-width:240px}.inspection-toolbar .el-select{width:140px}.compact .inspection-toolbar .el-input{max-width:none;flex:1;min-width:130px}.inspection-cards{display:grid;gap:10px;max-height:420px;overflow:auto}.inspection-item{display:flex;flex-direction:column;gap:7px;align-items:flex-start;border:1px solid var(--fire-border);background:transparent;color:inherit;padding:12px;text-align:left;cursor:pointer}.inspection-open{display:flex;flex-direction:column;align-items:flex-start;gap:7px;width:100%;border:0;padding:0;background:transparent;color:inherit;text-align:left;cursor:pointer;font:inherit}.inspection-open:focus-visible{outline:2px solid var(--fire-accent,#25c5db);outline-offset:3px}.inspection-open:disabled{opacity:.6;cursor:not-allowed}.inspection-item small{color:var(--fire-muted)}.inspection-pagination{margin-top:16px;display:flex;justify-content:flex-end}.record-evidence{max-width:100%;max-height:480px;margin-top:12px}.muted{font-size:12px}
</style>
