<template>
 <el-dialog v-model="open" :title="title" width="min(800px,94vw)" class="fire-dialog device-asset-dialog" append-to-body :close-on-click-modal="!saving" :close-on-press-escape="!saving" :show-close="!saving" @opened="focusName">
  <div v-if="error" class="fire-error" role="alert">{{ error }}</div>
  <el-form ref="formRef" :model="form" :rules="rules" label-position="top" novalidate @submit.prevent="save">
   <div class="asset-fields">
    <el-form-item label="名称" prop="name"><el-input ref="nameInput" v-model="form.name" aria-label="名称" maxlength="255" :disabled="saving" /></el-form-item>
    <template v-if="kind==='gateways'">
     <el-form-item label="所属单位" prop="unitName"><el-input v-model="form.unitName" aria-label="所属单位" maxlength="255" :disabled="saving" /></el-form-item>
     <el-form-item label="关联监管机构（可选）"><el-select v-model="form.placeId" filterable remote clearable :remote-method="searchPlaces" :loading="placesLoading" :disabled="saving||!auth.hasPermi('fire:places:list')" placeholder="可搜索已有机构，无需新增重复机构" popper-class="fire-dialog" style="width:100%"><el-option v-for="p in places" :key="p.id" :value="p.id" :label="p.name"/></el-select></el-form-item>
     <el-form-item label="装置编号"><el-input v-model="form.deviceCode" aria-label="装置编号" maxlength="64" :disabled="saving" /></el-form-item>
    </template>
    <el-form-item :label="kind==='hosts'?'主机品牌':'装置厂家'"><el-input v-model="form.manufacturer" :aria-label="kind==='hosts'?'主机品牌':'装置厂家'" maxlength="100" :disabled="saving" /></el-form-item>
    <el-form-item label="型号"><el-input v-model="form.model" aria-label="型号" maxlength="100" :disabled="saving" /></el-form-item>
    <template v-if="kind==='gateways'">
     <el-form-item label="装置协议地址" prop="sourceAddress"><el-input v-model="form.sourceAddress" aria-label="装置协议地址" maxlength="12" placeholder="报文中的12位十六进制地址" :disabled="saving" /></el-form-item>
     <el-form-item label="中心协议地址" prop="centerAddress"><el-input v-model="form.centerAddress" aria-label="中心协议地址" maxlength="12" placeholder="报文中的12位十六进制地址" :disabled="saving" /></el-form-item>
     <el-form-item label="允许来源IP" prop="allowedIp"><el-input v-model="form.allowedIp" aria-label="允许来源IP" maxlength="45" placeholder="服务器实际看到的装置IP" :disabled="saving" /></el-form-item>
     <el-form-item label="摄像头IP（台账参考）"><el-input v-model="form.cameraIp" aria-label="摄像头IP" maxlength="45" :disabled="saving" /></el-form-item>
     <el-form-item label="安装情况"><el-input v-model="form.installationStatus" aria-label="安装情况" maxlength="100" :disabled="saving" /></el-form-item>
     <el-form-item label="接入状态" prop="enabled"><el-radio-group v-model="form.enabled" :disabled="saving"><el-radio value="0">启用</el-radio><el-radio value="1">停用</el-radio></el-radio-group></el-form-item>
    </template>
    <template v-else>
     <el-form-item label="系统类型"><el-input-number v-model="form.systemType" aria-label="系统类型" :min="0" :max="255" :precision="0" :disabled="saving" /></el-form-item>
     <el-form-item label="系统地址"><el-input-number v-model="form.systemAddress" aria-label="系统地址" :min="0" :max="255" :precision="0" :disabled="saving" /></el-form-item>
    </template>
   </div>
   <el-form-item v-if="kind==='hosts'" label="接入方式"><el-input v-model="form.accessMethod" aria-label="接入方式" type="textarea" resize="vertical" maxlength="500" :disabled="saving" /></el-form-item>
   <el-form-item v-else label="备注"><el-input v-model="form.remark" aria-label="备注" type="textarea" resize="vertical" maxlength="500" :disabled="saving" /></el-form-item>
   <p class="muted">{{ kind==='hosts'?'系统类型和地址未知时请同时留空；实际报文识别出的主机可再维护名称和型号。':'编号与协议地址分别保存。协议地址或来源IP修改后，装置需重新连接；缺少接入参数时请先停用保存。' }}</p>
  </el-form>
  <template #footer><el-button :disabled="saving" @click="open=false">取消</el-button><el-button type="primary" :loading="saving" @click="save">保存</el-button></template>
 </el-dialog>
</template>
<script setup>
import {ref,computed,watch,nextTick} from 'vue'
import {ElMessage} from 'element-plus'
import {saveAsset} from '@/api/fire/assets'
import {listDatabasePlaces} from '@/api/fire/places'
import auth from '@/plugins/auth'
const props=defineProps({modelValue:Boolean,kind:{type:String,default:'gateways'},record:Object,gatewayId:[String,Number]})
const emit=defineEmits(['update:modelValue','saved'])
const places=ref([]),placesLoading=ref(false);let placeVersion=0
async function searchPlaces(keyword){const current=++placeVersion;placesLoading.value=true;try{const r=await listDatabasePlaces({keyword,pageNum:1,pageSize:30});if(current===placeVersion)places.value=r.rows.map(p=>({id:String(p.id),name:p.name}))}catch{if(current===placeVersion)places.value=[]}finally{if(current===placeVersion)placesLoading.value=false}}
const saving=ref(false),error=ref(''),form=ref({}),formRef=ref(),nameInput=ref()
const open=computed({get:()=>props.modelValue,set:v=>{if(!saving.value)emit('update:modelValue',v)}})
const title=computed(()=>`${form.value.id?'编辑':'新增'}${props.kind==='hosts'?'下挂主机':'传输装置'}`)
const required=label=>({required:true,whitespace:true,message:'请填写'+label,trigger:'blur'})
const hexRule={validator:(_,v,cb)=>v&&!/^[a-fA-F0-9]{12}$/.test(v)?cb(new Error('请输入12位十六进制地址')):cb(),trigger:'blur'}
const rules={name:[required('名称')],unitName:[required('所属单位')],sourceAddress:[hexRule],centerAddress:[hexRule]}
function focusName(){nameInput.value?.focus()}
async function save(){if(saving.value)return;error.value='';try{await formRef.value.validate()}catch{await nextTick();formRef.value?.$el.querySelector('.is-error input')?.focus();return}
 if(props.kind==='gateways'&&form.value.enabled==='0'&&(!form.value.sourceAddress||!form.value.centerAddress||!form.value.allowedIp)){error.value='启用前请填写装置协议地址、中心协议地址和允许来源IP';return}
 if(props.kind==='hosts'&&((form.value.systemType==null)!=(form.value.systemAddress==null))){error.value='系统类型和系统地址需同时填写或同时留空';return}
 saving.value=true;try{await saveAsset(props.kind,{...form.value,placeId:form.value.placeId||null});ElMessage.success('已保存');emit('saved');saving.value=false;open.value=false}catch(e){error.value=e.message||'保存失败，请重试'}finally{saving.value=false}}
watch(()=>props.modelValue,v=>{++placeVersion;placesLoading.value=false;if(!v)return;places.value=props.record?.placeId?[{id:String(props.record.placeId),name:props.record.placeName}]:[];error.value='';form.value={name:'',unitName:'',manufacturer:props.kind==='gateways'?'浙江大华':'',model:props.kind==='gateways'?'DH-HY-FAM-1000':'',enabled:'1',...props.record,placeId:props.record?.placeId?String(props.record.placeId):'',sourceAddress:props.record?.serial||props.record?.sourceAddress||'',gatewayId:props.gatewayId||props.record?.gatewayId,systemType:props.record?.systemType??undefined,systemAddress:props.record?.systemAddress??undefined};nextTick(()=>formRef.value?.clearValidate())})
</script>
<style scoped>
.asset-fields{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:0 20px}.muted{font-size:12px}@media(max-width:600px){.asset-fields{grid-template-columns:1fr}}
</style>

<style>
.device-asset-dialog.el-dialog{display:flex;flex-direction:column;max-height:90vh;margin-top:5vh}
.device-asset-dialog .el-dialog__body{min-height:0;overflow-y:auto;flex:1}
.device-asset-dialog .el-dialog__header,.device-asset-dialog .el-dialog__footer{flex-shrink:0}
</style>
