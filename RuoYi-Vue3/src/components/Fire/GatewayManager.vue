<template>
 <div class="fire-page fire-theme">
  <div class="fire-page-title"><div><div class="fire-kicker">DEVICE MONITORING</div><h1>传输装置管理</h1><p>管理用户信息传输装置及下挂消防主机</p></div><div class="fire-toolbar"><el-button v-if="auth.hasRole('admin')" @click="showUnregistered">待登记报文</el-button><el-button v-if="auth.hasPermi('fire:hosts:add')" type="primary" :icon="Plus" @click="edit(null)">新增传输装置</el-button><el-button :loading="loading" @click="load">刷新记录</el-button></div></div>
  <el-tabs v-model="mainTab"><el-tab-pane label="传输装置" name="gateways"/><el-tab-pane label="全部下挂主机" name="hosts"/><el-tab-pane v-if="canFire('alarms')" label="设备事件" name="alarms"/><el-tab-pane label="通信报文" name="protocolMessages"/></el-tabs>
  <DeviceAccessView v-if="mainTab!=='gateways'" embedded :resource="mainTab" :key="mainTab"/>
  <div v-else class="fire-table-card" :aria-busy="loading">
   <el-form inline novalidate @submit.prevent="search"><el-form-item label="单位或装置"><el-input v-model="keyword" aria-label="单位或装置" clearable placeholder="输入单位、装置名称或编号" @clear="search"/></el-form-item><el-form-item><el-button type="primary" native-type="submit" :loading="loading">查询</el-button></el-form-item></el-form>
   <div v-if="error" class="fire-error" role="alert">{{ error }}<el-button link @click="load">重试</el-button></div>
   <el-table v-loading="loading" :data="rows" row-key="id" stripe empty-text="暂无传输装置，可新增装置或调整查询条件">
    <el-table-column prop="deviceCode" label="装置编号" width="100"/><el-table-column prop="name" label="装置名称" min-width="240" show-overflow-tooltip/><el-table-column prop="placeName" label="所属单位" min-width="180" show-overflow-tooltip/><el-table-column prop="model" label="型号" min-width="180"/><el-table-column prop="allowedIp" label="允许来源IP" min-width="145"/>
    <el-table-column label="接入配置" width="110"><template #default="{row}"><el-tag :type="row.serial&&row.centerAddress&&row.allowedIp?'success':'warning'">{{ row.serial&&row.centerAddress&&row.allowedIp?'已配置':'待配置' }}</el-tag></template></el-table-column>
    <el-table-column label="通信状态" width="100"><template #default="{row}"><StatusTag :value="row.status"/></template></el-table-column><el-table-column prop="deviceStatus" label="装置上报状态" min-width="220" show-overflow-tooltip/><el-table-column prop="lastSeen" label="最后有效报文" min-width="185"/>
    <el-table-column label="操作" width="230" fixed="right"><template #default="{row}"><el-button link type="primary" @click="detail(row)">详情</el-button><el-button v-if="auth.hasPermi('fire:hosts:edit')" link type="primary" @click="edit(row)">编辑</el-button><el-button v-if="auth.hasPermi('fire:hosts:edit')" link :disabled="busy" @click="toggle(row)">{{row.enabled==='0'?'停用':'启用'}}</el-button><el-button v-if="auth.hasPermi('fire:hosts:remove')" link type="danger" :disabled="busy" @click="remove('gateways',row)">删除</el-button></template></el-table-column>
   </el-table><el-pagination v-model:current-page="page" :page-size="20" :total="total" layout="total, prev, pager, next" @current-change="load"/>
  </div>
  <el-dialog v-model="unknownOpen" title="待登记报文" width="min(1000px,96vw)" class="fire-dialog" append-to-body><p class="muted">这里只展示服务器实际收到的未登记地址，不代表已验证的设备身份。核对单位和装置编号后，再配置启用。</p><div v-if="unknownError" class="fire-error" role="alert">{{unknownError}}<el-button link @click="showUnregistered">重试</el-button></div><el-table v-loading="unknownLoading" :data="unknownRows" empty-text="暂无待登记报文"><el-table-column prop="allowedIp" label="观察到的来源IP" min-width="150"/><el-table-column prop="sourceAddress" label="装置协议地址" min-width="160"/><el-table-column prop="centerAddress" label="中心协议地址" min-width="160"/><el-table-column prop="lastSeen" label="最近接收" min-width="180"/><el-table-column prop="receipts" label="帧次数" width="90"/><el-table-column label="操作" width="100"><template #default="{row}"><el-button v-if="auth.hasPermi('fire:hosts:add')" link type="primary" @click="registerUnknown(row)">新增登记</el-button></template></el-table-column></el-table></el-dialog>
  <DeviceAssetEditor v-model="editor" :kind="editKind" :record="editing" :gateway-id="editKind==='hosts'?selected?.id:undefined" @saved="saved"/>
  <el-dialog v-model="detailOpen" :title="selected?.name||'装置详情'" width="min(1160px,96vw)" class="fire-dialog" append-to-body>
   <el-tabs v-model="detailTab"><el-tab-pane label="基本资料" name="basic"/><el-tab-pane label="下挂主机" name="hosts"/><el-tab-pane v-if="canFire('alarms')" label="设备事件" name="alarms"/><el-tab-pane label="通信报文" name="protocolMessages"/></el-tabs>
   <template v-if="detailTab==='basic'"><div v-if="detailError" class="fire-error" role="alert">{{detailError}}<el-button link @click="detail(selected)">重试</el-button></div><el-descriptions v-loading="detailLoading" :column="1" border><el-descriptions-item v-for="[key,label] in detailFields" :key="key" :label="label">{{selected?.[key]??'—'}}</el-descriptions-item></el-descriptions></template>
   <template v-else-if="detailTab==='hosts'">
    <div class="fire-toolbar"><el-button v-if="auth.hasPermi('fire:hosts:add')" type="primary" :icon="Plus" @click="edit(null,'hosts')">新增下挂主机</el-button><el-button :loading="hostLoading" @click="loadHosts">刷新主机</el-button></div>
    <p class="muted">系统地址留空表示台账待核对；装置在线不代表主机正常。</p>
    <div v-if="hostError" class="fire-error" role="alert">{{hostError}}<el-button link @click="loadHosts">重试</el-button></div>
    <el-table v-loading="hostLoading" :data="hosts" row-key="id" empty-text="暂无下挂主机"><el-table-column prop="name" label="主机名称" min-width="180"/><el-table-column prop="manufacturer" label="品牌" min-width="120"/><el-table-column prop="model" label="型号" min-width="150"/><el-table-column prop="serial" label="系统类型/地址" min-width="120"/><el-table-column prop="accessMethod" label="接入方式" min-width="200" show-overflow-tooltip/><el-table-column prop="deviceStatus" label="主机上报状态" min-width="180"/><el-table-column label="操作" width="140" fixed="right"><template #default="{row}"><el-button v-if="auth.hasPermi('fire:hosts:edit')" link type="primary" @click="edit(row,'hosts')">编辑</el-button><el-button v-if="auth.hasPermi('fire:hosts:remove')" link type="danger" :disabled="busy" @click="remove('hosts',row)">删除</el-button></template></el-table-column></el-table>
    <el-pagination v-model:current-page="hostPage" :page-size="20" :total="hostTotal" layout="total, prev, pager, next" @current-change="loadHosts"/>
   </template>
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
import {getGateway,saveAsset,removeAsset,unregisteredDevices} from '@/api/fire/assets'
import DeviceAccessView from './DeviceAccessView.vue'
import DeviceAssetEditor from './DeviceAssetEditor.vue'
import StatusTag from './StatusTag.vue'
const route=useRoute(),router=useRouter()
const mainTab=ref(['hosts','alarms','protocolMessages'].includes(route.query.tab)?route.query.tab:'gateways'),keyword=ref(String(route.query.keyword||'')),page=ref(Math.max(1,Number(route.query.page)||1)),rows=ref([]),total=ref(0),loading=ref(false),error=ref(''),busy=ref(false)
const unknownOpen=ref(false),unknownRows=ref([]),unknownLoading=ref(false),unknownError=ref('')
const editor=ref(false),editKind=ref('gateways'),editing=ref(null),detailOpen=ref(false),selected=ref(null),detailTab=ref('basic'),detailLoading=ref(false),detailError=ref(''),hosts=ref([]),hostLoading=ref(false),hostError=ref(''),hostPage=ref(1),hostTotal=ref(0)
const detailFields=[['deviceCode','装置编号'],['placeName','所属单位'],['manufacturer','厂家'],['model','型号'],['serial','装置协议地址'],['centerAddress','中心协议地址'],['allowedIp','允许来源IP'],['status','通信状态'],['deviceStatus','装置上报状态'],['lastSeen','最后有效报文'],['installationStatus','安装情况'],['cameraIp','摄像头IP（台账参考）'],['remark','备注']]
let version=0,hostVersion=0,detailVersion=0,timer
function persist(){router.replace({query:{...route.query,tab:mainTab.value,keyword:keyword.value||undefined,page:String(page.value)}})}
async function load(){const current=++version;loading.value=true;error.value='';try{const r=await listDeviceRecords('gateways',{keyword:keyword.value,pageNum:page.value,pageSize:20});if(current!==version)return;if(page.value>1&&(page.value-1)*20>=r.total){page.value=Math.max(1,Math.ceil(r.total/20));return load()}rows.value=r.rows;total.value=r.total}catch(e){if(current===version){rows.value=[];total.value=0;error.value=e.message||'装置加载失败'}}finally{if(current===version)loading.value=false}}
function search(){page.value=1;persist();load()}
async function showUnregistered(){unknownOpen.value=true;unknownLoading.value=true;unknownError.value='';try{const r=await unregisteredDevices();unknownRows.value=r.data}catch(e){unknownError.value=e.message||'报文加载失败'}finally{unknownLoading.value=false}}
function registerUnknown(row){unknownOpen.value=false;edit({...row,enabled:'1'},'gateways')}
function edit(row,kind='gateways'){editing.value=row;editKind.value=kind;editor.value=true}
async function detail(row){const current=++detailVersion;selected.value=row;detailOpen.value=true;detailTab.value='basic';detailLoading.value=true;detailError.value='';hostPage.value=1;try{const r=await getGateway(row.id);if(current===detailVersion&&detailOpen.value)selected.value=r.data}catch(e){if(current===detailVersion)detailError.value=e.message||'详情加载失败'}finally{if(current===detailVersion)detailLoading.value=false}}
async function loadHosts(){const current=++hostVersion;hostLoading.value=true;hostError.value='';try{const r=await listDeviceRecords('hosts',{gatewayId:selected.value.id,pageNum:hostPage.value,pageSize:20});if(current!==hostVersion)return;if(hostPage.value>1&&(hostPage.value-1)*20>=r.total){hostPage.value=Math.max(1,Math.ceil(r.total/20));return loadHosts()}hosts.value=r.rows;hostTotal.value=r.total}catch(e){if(current===hostVersion){hosts.value=[];hostTotal.value=0;hostError.value=e.message||'主机加载失败'}}finally{if(current===hostVersion)hostLoading.value=false}}
async function saved(){load();if(detailOpen.value){if(editKind.value==='hosts')loadHosts();else detail(selected.value)}}
async function toggle(row){if(busy.value)return;try{await ElMessageBox.confirm(`${row.enabled==='0'?'停用后将停止接收此装置的新告警，历史记录保留。':'确认启用此装置接收？'}装置：${row.name}`,row.enabled==='0'?'停用传输装置':'启用传输装置',{type:'warning',confirmButtonText:'确认',cancelButtonText:'取消'})}catch{return}busy.value=true;try{await saveAsset('gateways',{...row,unitName:row.unitName||row.placeName,sourceAddress:row.serial,enabled:row.enabled==='0'?'1':'0'});ElMessage.success('接入状态已更新');load()}catch(e){error.value=e.message||'操作失败'}finally{busy.value=false}}
async function remove(kind,row){if(busy.value)return;try{await ElMessageBox.confirm(`确认删除“${row.name}”？${kind==='gateways'?'删除后停止该装置接入，下挂主机保留在历史中。':''}历史告警和通信报文保留。`,'删除确认',{type:'warning',confirmButtonText:'删除',cancelButtonText:'取消'})}catch{return}busy.value=true;try{await removeAsset(kind,row);ElMessage.success('已删除');kind==='hosts'?loadHosts():load()}catch(e){kind==='hosts'?hostError.value=e.message:error.value=e.message}finally{busy.value=false}}
watch(detailTab,v=>{if(v==='hosts')loadHosts()})
watch(detailOpen,v=>{if(!v){++hostVersion;++detailVersion;hosts.value=[]}})
watch([page,mainTab],persist)
watch(()=>route.query,q=>{const t=['hosts','alarms','protocolMessages'].includes(q.tab)?q.tab:'gateways',p=Math.max(1,Number(q.page)||1),k=String(q.keyword||'');if(t!==mainTab.value||p!==page.value||k!==keyword.value){mainTab.value=t;page.value=p;keyword.value=k;load()}})
onMounted(()=>{load();timer=setInterval(()=>{if(mainTab.value==='gateways'&&!editor.value&&!detailOpen.value&&!loading.value&&!error.value&&!busy.value)load()},5000)})
onBeforeUnmount(()=>{++version;++hostVersion;++detailVersion;clearInterval(timer)})
</script>
