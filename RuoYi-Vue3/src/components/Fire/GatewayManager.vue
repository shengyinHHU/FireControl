<template>
 <div class="fire-page fire-theme">
  <div class="fire-page-title"><div><div class="fire-kicker">DEVICE MONITORING</div><h1>传输装置管理</h1><p>管理传输装置，接收消防警情并查看通信报文</p></div><div class="fire-toolbar"><el-button v-if="auth.hasRole('admin')" @click="showUnregistered">待登记报文</el-button><el-button v-if="auth.hasPermi('fire:gateways:add')" type="primary" :icon="Plus" @click="edit(null)">新增传输装置</el-button><el-button :loading="loading" @click="load">刷新记录</el-button></div></div>
  <el-tabs v-model="mainTab"><el-tab-pane label="传输装置" name="gateways"/><el-tab-pane v-if="canFire('alarms')" label="设备事件" name="alarms"/><el-tab-pane label="通信报文" name="protocolMessages"/></el-tabs>
  <p class="muted device-update-status" role="status">{{ updateState === 'connected' ? '实时更新已连接' : updateState === 'denied' ? '实时更新不可用，请检查登录和权限' : '正在连接实时更新，定期校准' }}</p>
  <DeviceAccessView v-if="mainTab!=='gateways'" embedded :resource="mainTab" :key="mainTab"/>
  <div v-else class="fire-table-card" :aria-busy="loading">
   <el-form inline novalidate @submit.prevent="search"><el-form-item label="单位或装置"><el-input v-model="keyword" aria-label="单位或装置" clearable placeholder="输入单位、装置名称或编号" @clear="search"/></el-form-item><el-form-item><el-button type="primary" native-type="submit" :loading="loading">查询</el-button></el-form-item></el-form>
   <div v-if="error" class="fire-error" role="alert">{{ error }}<el-button link @click="load">重试</el-button></div>
   <el-table v-loading="loading" :data="rows" row-key="id" stripe empty-text="暂无传输装置，可新增装置或调整查询条件">
    <el-table-column prop="deviceCode" label="装置编号" width="100"/><el-table-column prop="name" label="装置名称" min-width="240" show-overflow-tooltip/><el-table-column prop="placeName" label="所属单位" min-width="180" show-overflow-tooltip/><el-table-column prop="model" label="型号" min-width="180"/><el-table-column prop="allowedIp" label="来源IP" min-width="145"/>
    <el-table-column label="来源识别" width="110"><template #default="{row}"><el-tag :type="row.allowedIp?'success':'warning'">{{ row.allowedIp?'已关联IP':'待接收' }}</el-tag></template></el-table-column>
    <el-table-column label="通信状态" width="100"><template #default="{row}"><StatusTag :value="row.status"/></template></el-table-column><el-table-column prop="deviceStatus" label="装置上报状态" min-width="220" show-overflow-tooltip/><el-table-column prop="lastSeen" label="最后有效报文" min-width="185"/>
    <el-table-column label="操作" width="230" fixed="right"><template #default="{row}"><el-button link type="primary" @click="detail(row)">详情</el-button><el-button v-if="auth.hasPermi('fire:gateways:edit')" link type="primary" @click="edit(row)">编辑</el-button><el-button v-if="auth.hasPermi('fire:gateways:edit')" link :disabled="busy" @click="toggle(row)">{{row.enabled==='0'?'停用':'启用'}}</el-button><el-button v-if="auth.hasPermi('fire:gateways:remove')" link type="danger" :disabled="busy" @click="remove(row)">删除</el-button></template></el-table-column>
   </el-table><el-pagination v-model:current-page="page" :page-size="20" :total="total" layout="total, prev, pager, next" @current-change="load"/>
  </div>
  <el-dialog v-model="unknownOpen" title="待登记报文" width="min(1000px,96vw)" class="fire-dialog" append-to-body><p class="muted">此处保留历史未登记报文。新装置发送有效报文后会自动接入，请在装置列表中补充所属单位。</p><div v-if="unknownError" class="fire-error" role="alert">{{unknownError}}<el-button link @click="showUnregistered">重试</el-button></div><el-table v-loading="unknownLoading" :data="unknownRows" empty-text="暂无待登记报文"><el-table-column prop="allowedIp" label="观察到的来源IP" min-width="150"/><el-table-column prop="sourceAddress" label="装置协议地址" min-width="160"/><el-table-column prop="centerAddress" label="中心协议地址" min-width="160"/><el-table-column prop="lastSeen" label="最近接收" min-width="180"/><el-table-column prop="receipts" label="帧次数" width="90"/><el-table-column label="操作" width="100"><template #default="{row}"><el-button v-if="auth.hasPermi('fire:gateways:add')" link type="primary" @click="registerUnknown(row)">新增登记</el-button></template></el-table-column></el-table></el-dialog>
  <DeviceAssetEditor v-model="editor" :record="editing" @saved="saved"/>
  <el-dialog v-model="detailOpen" :title="selected?.name||'装置详情'" width="min(1160px,96vw)" class="fire-dialog" append-to-body>
   <el-tabs v-model="detailTab"><el-tab-pane label="基本资料" name="basic"/><el-tab-pane v-if="canFire('alarms')" label="设备事件" name="alarms"/><el-tab-pane label="通信报文" name="protocolMessages"/></el-tabs>
   <template v-if="detailTab==='basic'"><div v-if="detailError" class="fire-error" role="alert">{{detailError}}<el-button link @click="detail(selected)">重试</el-button></div><el-descriptions v-loading="detailLoading" :column="1" border><el-descriptions-item v-for="[key,label] in detailFields" :key="key" :label="label">{{selected?.[key]??'—'}}</el-descriptions-item></el-descriptions></template>
   <DeviceAccessView v-else-if="selected" :key="selected.id+detailTab" embedded :resource="detailTab" :gateway-id="selected.id"/>
  </el-dialog>
 </div>
</template>
<script setup>
import {ref,watch,onMounted,onBeforeUnmount} from 'vue'
import {useRoute,useRouter} from 'vue-router'
import {ElMessage,ElMessageBox} from 'element-plus'
import {Plus} from '@element-plus/icons-vue'
import auth from '@/plugins/auth'
import {canFire} from '@/api/fire/access'
import {listDeviceRecords} from '@/api/fire/deviceAccess'
import {createTableRefresh,reconcileTableRows} from '@/api/fire/tableRefresh'
import {useDeviceUpdates} from '@/views/fire/useDeviceUpdates'
import {getGateway,saveAsset,removeAsset,unregisteredDevices} from '@/api/fire/assets'
import DeviceAccessView from './DeviceAccessView.vue'
import DeviceAssetEditor from './DeviceAssetEditor.vue'
import StatusTag from './StatusTag.vue'
const route=useRoute(),router=useRouter()
const mainTab=ref(['alarms','protocolMessages'].includes(route.query.tab)?route.query.tab:'gateways'),keyword=ref(String(route.query.keyword||'')),page=ref(Math.max(1,Number(route.query.page)||1)),rows=ref([]),total=ref(0),loading=ref(false),error=ref(''),busy=ref(false)
const unknownOpen=ref(false),unknownRows=ref([]),unknownLoading=ref(false),unknownError=ref('')
const editor=ref(false),editing=ref(null),detailOpen=ref(false),selected=ref(null),detailTab=ref('basic'),detailLoading=ref(false),detailError=ref('')
const detailFields=[['deviceCode','装置编号'],['placeName','所属单位'],['manufacturer','厂家'],['model','型号'],['serial','装置协议地址'],['centerAddress','中心协议地址'],['allowedIp','来源IP'],['status','通信状态'],['deviceStatus','装置上报状态'],['lastSeen','最后有效报文'],['installationStatus','安装情况'],['cameraIp','摄像头IP（台账参考）'],['remark','备注']]
let detailVersion=0
const refreshQueue=createTableRefresh(async(silent,isCurrent)=>{
 if(!silent)loading.value=true
 try{
  const r=await listDeviceRecords('gateways',{keyword:keyword.value,pageNum:page.value,pageSize:20},{silentError:silent})
  if(!isCurrent())return
  if(page.value>1&&(page.value-1)*20>=r.total){page.value=Math.max(1,Math.ceil(r.total/20));load();return}
  rows.value=reconcileTableRows(rows.value,r.rows);total.value=r.total;error.value=''
  if(detailOpen.value&&detailTab.value==='basic'){
   const fresh=rows.value.find(row=>row.id===selected.value?.id)
   if(fresh)selected.value={...selected.value,...fresh}
  }
 }catch(e){if(isCurrent()){if(!silent){rows.value=[];total.value=0}error.value=(e.message||'装置加载失败')+(silent?'；当前保留上次成功记录，将自动重试':'')}}
 finally{if(isCurrent())loading.value=false}
},{paused:()=>document.hidden||mainTab.value!=='gateways'||editor.value||busy.value||(detailOpen.value&&detailTab.value!=='basic')})
const updateState=useDeviceUpdates(()=>refreshQueue.refresh(true))
function persist(){router.replace({query:{...route.query,tab:mainTab.value,keyword:keyword.value||undefined,page:String(page.value)}})}
function load(){refreshQueue.refresh()}
function search(){page.value=1;persist();load()}
async function showUnregistered(){unknownOpen.value=true;unknownLoading.value=true;unknownError.value='';try{const r=await unregisteredDevices();unknownRows.value=r.data}catch(e){unknownError.value=e.message||'报文加载失败'}finally{unknownLoading.value=false}}
function registerUnknown(row){unknownOpen.value=false;edit({...row,enabled:'1'})}
function edit(row){editing.value=row;editor.value=true}
async function detail(row){const current=++detailVersion;selected.value=row;detailOpen.value=true;detailTab.value='basic';detailLoading.value=true;detailError.value='';try{const r=await getGateway(row.id);if(current===detailVersion&&detailOpen.value)selected.value=r.data}catch(e){if(current===detailVersion)detailError.value=e.message||'详情加载失败'}finally{if(current===detailVersion)detailLoading.value=false}}
async function saved(){load();if(detailOpen.value)detail(selected.value)}
async function toggle(row){if(busy.value)return;try{await ElMessageBox.confirm(`${row.enabled==='0'?'停用后将停止接收此装置的新告警，历史记录保留。':'确认启用此装置接收？'}装置：${row.name}`,row.enabled==='0'?'停用传输装置':'启用传输装置',{type:'warning',confirmButtonText:'确认',cancelButtonText:'取消'})}catch{return}busy.value=true;try{await saveAsset('gateways',{...row,unitName:row.unitName||row.placeName,sourceAddress:row.serial,enabled:row.enabled==='0'?'1':'0'});ElMessage.success('接入状态已更新');load()}catch(e){error.value=e.message||'操作失败'}finally{busy.value=false}}
async function remove(row){if(busy.value)return;try{await ElMessageBox.confirm(`确认删除“${row.name}”？删除后停止该装置接入，历史告警和通信报文保留。`,'删除确认',{type:'warning',confirmButtonText:'删除',cancelButtonText:'取消'})}catch{return}busy.value=true;try{await removeAsset('gateways',row);ElMessage.success('已删除');load()}catch(e){error.value=e.message||'删除失败'}finally{busy.value=false}}
watch(detailOpen,v=>{if(!v){++detailVersion}})
watch([mainTab,editor,busy,detailOpen,detailTab],()=>refreshQueue.resume())
watch([page,mainTab],persist)
watch(()=>route.query,q=>{const t=['alarms','protocolMessages'].includes(q.tab)?q.tab:'gateways',p=Math.max(1,Number(q.page)||1),k=String(q.keyword||'');if(t!==mainTab.value||p!==page.value||k!==keyword.value){mainTab.value=t;page.value=p;keyword.value=k;load()}})
onMounted(load)
onBeforeUnmount(()=>{refreshQueue.stop();++detailVersion})
</script>
<style scoped>.device-update-status { min-height: 20px; margin: 0 0 8px; font-size: 12px; }</style>
