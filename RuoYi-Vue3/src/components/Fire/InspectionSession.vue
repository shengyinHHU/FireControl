<template>
 <LiveVideoDialog v-model="open" :device="device" :preview="preview" :title="post?.postName || '岗位查岗'" :busy="saving">
  <template #default="{ ready, capture, error: playError }">
   <p v-if="loadError" class="fire-error" role="alert">{{ loadError }} <el-button link @click="load">重试岗位</el-button></p>
   <p v-if="loading" role="status">正在加载岗位摄像头…</p>
   <template v-if="post">
    <p>{{ post.placeName || '未关联机构' }}</p>
    <InspectionContact class="session-contact" :name="post.contactName" :phone="post.contactPhone" />
    <el-select v-model="binding" :disabled="saving" aria-label="选择岗位摄像头" popper-class="fire-dialog" style="width:100%" @change="clearEvidence">
     <el-option v-for="c in post.cameras" :key="c.bindingId" :label="c.deviceName + (c.isPrimary ? '（默认）' : '')" :value="c.bindingId" />
    </el-select>
    <el-form v-if="canInspection('add')" label-position="top" class="inspection-form" @submit.prevent>
     <p>查岗人：{{ user.nickName || user.name }} · 查岗时间由服务器记录</p>
     <el-form-item label="查岗结论" required><el-select aria-label="查岗结论" v-model="result" :disabled="saving" placeholder="请选择结论" popper-class="fire-dialog"><el-option v-for="(label,key) in results" :key="key" :label="label" :value="key" /></el-select></el-form-item>
     <el-form-item label="查岗说明"><el-input aria-label="查岗说明" v-model="note" type="textarea" :rows="2" maxlength="1000" show-word-limit :disabled="saving" /></el-form-item>
     <p v-if="!ready" class="muted">画面尚未就绪，不能据此认定脱岗；可选择“无法判断”并说明原因。</p>
     <div class="capture-tools"><el-button :disabled="!ready || saving" @click="grab(capture)">抓拍取证</el-button><el-button v-if="imageUrl" :disabled="saving" @click="clearEvidence">移除抓拍</el-button><el-button type="primary" :loading="saving" :disabled="!result || !binding || (!ready && result !== 'unknown')" @click="save">保存查岗记录</el-button></div>
     <img v-if="imageUrl" :src="imageUrl" class="inspection-evidence" alt="本次查岗待保存的抓拍" />
     <p v-if="submitError" class="fire-error" role="alert">{{ submitError }}</p>
    </el-form>
   </template>
  </template>
 </LiveVideoDialog>
</template>
<script setup>
import { computed, ref, watch, onBeforeUnmount } from 'vue'
import { ElMessage } from 'element-plus'
import LiveVideoDialog from './LiveVideoDialog.vue'
import InspectionContact from './InspectionContact.vue'
import { inspectionRequestId } from '@/utils/fireInspectionRequest'
import useUserStore from '@/store/modules/user'
import { canInspection, inspectionPost, previewInspection, saveInspectionRecord } from '@/api/fire/inspection'
const props = defineProps({ modelValue: Boolean, postId: [String,Number] })
const emit = defineEmits(['update:modelValue','saved'])
const user = useUserStore()
const open = computed({ get:()=>props.modelValue, set:v=>{if(!saving.value)emit('update:modelValue',v)} })
const post=ref(),binding=ref(''),loading=ref(false),loadError=ref(''),saving=ref(false),submitError=ref(''),result=ref(''),note=ref(''),imageUrl=ref('')
const results={present:'在岗',absent:'脱岗',unknown:'无法判断'}
const device=computed(()=>post.value?.cameras?.find(c=>c.bindingId===binding.value))
let generation=0,image,requestId=inspectionRequestId()
function clearEvidence(){if(imageUrl.value)URL.revokeObjectURL(imageUrl.value);imageUrl.value='';image=null;requestId=inspectionRequestId()}
async function load(){const current=++generation;post.value=null;binding.value='';loadError.value='';loading.value=true;clearEvidence();result.value='';note.value='';submitError.value='';try{const {data}=await inspectionPost(props.postId);if(current!==generation||!open.value)return;if(data.status!=='0')throw Error('岗位已停用，请刷新岗位列表');if(!data.cameras?.length)throw Error('岗位尚未绑定可用摄像头');post.value=data;binding.value=data.cameras[0].bindingId}catch(e){if(current===generation)loadError.value=e.message||'岗位加载失败'}finally{if(current===generation)loading.value=false}}
const preview=async(code,protocol)=>{const selected=binding.value;return previewInspection(props.postId,selected,protocol)}
async function grab(capture){const current=generation,selected=binding.value;try{const blob=await capture();if(current!==generation||!open.value||selected!==binding.value)return;clearEvidence();image=blob;imageUrl.value=URL.createObjectURL(blob)}catch(e){ElMessage.error(e.message||'抓拍失败')}}
async function save(){if(saving.value)return;saving.value=true;submitError.value='';try{await saveInspectionRecord({requestId,postId:props.postId,bindingId:binding.value,result:result.value,note:note.value},image);ElMessage.success('查岗记录已保存');emit('saved');saving.value=false;open.value=false}catch(e){submitError.value=e.message||'保存失败，可重试'}finally{saving.value=false}}
watch([result,note],()=>{requestId=inspectionRequestId()})
watch(()=>[props.modelValue,props.postId],()=>{if(props.modelValue&&props.postId)load();else{generation++;clearEvidence() }},{immediate:true})
onBeforeUnmount(()=>{generation++;clearEvidence()})
</script>
<style scoped>
.session-contact{margin-bottom:12px}.inspection-form{margin-top:16px}.capture-tools{display:flex;gap:8px;flex-wrap:wrap}.capture-tools .el-button{margin-left:0}.inspection-evidence{display:block;max-width:100%;max-height:240px;margin-top:12px}.muted{font-size:12px}
</style>
