<template>
 <el-dialog v-model="open" :title="postId ? '配置查岗岗位' : '新增查岗岗位'" width="800px" class="fire-dialog" append-to-body :close-on-click-modal="!saving" :close-on-press-escape="!saving" :show-close="!saving">
  <p v-if="error" class="fire-error" role="alert">{{ error }}</p>
  <el-form v-loading="loading" label-position="top" @submit.prevent>
   <el-form-item label="岗位名称" required><el-input aria-label="岗位名称" v-model="form.postName" maxlength="160" :disabled="saving" /></el-form-item>
   <el-form-item label="所属机构（可选）"><el-select v-model="form.placeId" filterable remote clearable :remote-method="searchPlaces" :loading="placesLoading" :disabled="saving" placeholder="搜索真实机构" popper-class="fire-dialog" style="width:100%"><el-option v-for="p in places" :key="p.id" :value="p.id" :label="p.name" /></el-select></el-form-item>
   <div class="post-contacts"><el-form-item label="联系人姓名"><el-input aria-label="联系人姓名" v-model="form.contactName" maxlength="100" :disabled="saving" placeholder="请输入联系人姓名" /></el-form-item><el-form-item label="联系电话"><el-input aria-label="联系电话" v-model="form.contactPhone" maxlength="64" :disabled="saving" placeholder="手机或固定电话" type="tel" /></el-form-item></div>
   <div class="post-options"><el-form-item label="岗位状态"><el-radio-group v-model="form.status" :disabled="saving"><el-radio value="0">启用</el-radio><el-radio value="1">停用</el-radio></el-radio-group></el-form-item><el-form-item label="排序"><el-input-number v-model="form.sortOrder" :min="0" :max="100000" :disabled="saving" /></el-form-item></div>
   <el-form-item label="岗位说明"><el-input aria-label="岗位说明" v-model="form.remark" type="textarea" maxlength="500" :disabled="saving" /></el-form-item>
   <div class="camera-heading"><strong>绑定摄像头（{{ form.cameras.length }}/12）</strong><el-button :disabled="saving" @click="picker=true">从视频监控选择</el-button></div>
   <p class="muted">默认摄像头用于打开岗位；更换、解绑不改变历史查岗记录。</p>
   <el-table :data="form.cameras" border><el-table-column label="默认" width="70"><template #default="{row}"><el-radio v-model="primary" :value="row.deviceCode" :disabled="saving" :aria-label="'设为默认：'+row.deviceName"><span /></el-radio></template></el-table-column><el-table-column prop="deviceName" label="摄像头" min-width="180" /><el-table-column prop="deviceCode" label="设备编码" min-width="180" /><el-table-column label="操作" width="170"><template #default="{row,$index}"><el-button link :disabled="saving||!$index" @click="move($index,-1)">上移</el-button><el-button link :disabled="saving||$index===form.cameras.length-1" @click="move($index,1)">下移</el-button><el-button link type="danger" :disabled="saving" @click="remove(row)">移除</el-button></template></el-table-column></el-table>
  </el-form>
  <template #footer><el-button :disabled="saving" @click="open=false">取消</el-button><el-button type="primary" :loading="saving" :disabled="loading||!form.postName.trim()||!form.cameras.length||!primary" @click="save">保存岗位</el-button></template>
 </el-dialog>
 <el-dialog v-model="picker" title="从视频监控选择摄像头" width="960px" class="fire-dialog" append-to-body><VideoMonitor v-if="picker" selectable :selected-codes="form.cameras.map(c=>c.deviceCode)" @select="select" /><template #footer><el-button @click="picker=false">完成选择</el-button></template></el-dialog>
</template>
<script setup>
import {computed,ref,watch} from 'vue'
import {ElMessage} from 'element-plus'
import VideoMonitor from './VideoMonitor.vue'
import {inspectionPost,saveInspectionPost} from '@/api/fire/inspection'
import {listDatabasePlaces} from '@/api/fire/places'
const props=defineProps({modelValue:Boolean,postId:[String,Number]});const emit=defineEmits(['update:modelValue','saved'])
const open=computed({get:()=>props.modelValue,set:v=>{if(!saving.value)emit('update:modelValue',v)}})
const form=ref({postName:'',contactName:'',contactPhone:'',placeId:'',status:'0',sortOrder:0,remark:'',cameras:[]}),primary=ref(''),picker=ref(false),loading=ref(false),saving=ref(false),error=ref(''),places=ref([]),placesLoading=ref(false)
let version=0,placeVersion=0
async function searchPlaces(keyword){const current=++placeVersion;placesLoading.value=true;try{const data=await listDatabasePlaces({keyword,pageSize:30,pageNum:1});if(current===placeVersion)places.value=(data.rows||[]).map(p=>({id:String(p.id),name:p.name}))}catch{if(current===placeVersion)places.value=[]}finally{if(current===placeVersion)placesLoading.value=false}}
function select(camera){if(form.value.cameras.some(c=>c.deviceCode===camera.deviceCode)){remove(camera);return}if(form.value.cameras.length>=12){ElMessage.warning('每个岗位最多绑定12台摄像头');return}form.value.cameras.push({deviceCode:camera.deviceCode,deviceName:camera.deviceName||camera.deviceCode});if(!primary.value)primary.value=camera.deviceCode}
function remove(camera){form.value.cameras=form.value.cameras.filter(c=>c.deviceCode!==camera.deviceCode);if(primary.value===camera.deviceCode)primary.value=form.value.cameras[0]?.deviceCode||''}
function move(i,d){const a=form.value.cameras;[a[i],a[i+d]]=[a[i+d],a[i]]}
async function save(){if(saving.value)return;saving.value=true;error.value='';try{await saveInspectionPost({...form.value,placeId:form.value.placeId||null,cameras:form.value.cameras.map(c=>({deviceCode:c.deviceCode,isPrimary:c.deviceCode===primary.value}))});ElMessage.success('岗位配置已保存');emit('saved');saving.value=false;open.value=false}catch(e){error.value=e.message||'保存失败'}finally{saving.value=false}}
watch(()=>[props.modelValue,props.postId],async()=>{const current=++version;++placeVersion;loading.value=false;error.value='';picker.value=false;if(!props.modelValue)return;form.value={postName:'',contactName:'',contactPhone:'',placeId:'',status:'0',sortOrder:0,remark:'',cameras:[]};primary.value='';places.value=[];if(!props.postId)return;loading.value=true;try{const {data}=await inspectionPost(props.postId);if(current!==version)return;form.value={...data,placeId:data.placeId||'',cameras:data.cameras||[]};primary.value=form.value.cameras.find(c=>c.isPrimary)?.deviceCode||form.value.cameras[0]?.deviceCode||'';if(data.placeId)places.value=[{id:data.placeId,name:data.placeName}]}catch(e){if(current===version)error.value=e.message||'岗位加载失败'}finally{if(current===version)loading.value=false}},{immediate:true})
</script>
<style scoped>
.post-options,.camera-heading{display:flex;align-items:center;justify-content:space-between;gap:16px;flex-wrap:wrap}.muted{font-size:12px}
.post-contacts{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:16px}@media(max-width:600px){.post-contacts{grid-template-columns:1fr;gap:0}}
</style>
