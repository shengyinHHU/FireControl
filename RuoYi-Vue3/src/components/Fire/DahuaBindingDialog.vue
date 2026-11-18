<template>
  <el-dialog v-model="visible" title="摄像机对讲连接" width="520px" class="fire-dialog" append-to-body :close-on-click-modal="!saving" :close-on-press-escape="!saving" :show-close="!saving">
    <p class="muted">为当前视频绑定实际摄像机的专网地址。登录密码由服务器配置，页面不保存密码。</p>
    <div v-loading="loading">
      <el-form :model="form" label-width="100px" @submit.prevent="save">
        <el-form-item label="摄像机 IP"><el-input v-model.trim="form.host" placeholder="例如 10.5.90.150" maxlength="15" :disabled="saving" /></el-form-item>
        <el-form-item label="SDK 端口"><el-input-number v-model="form.port" :min="1" :max="65535" :disabled="saving" /></el-form-item>
        <el-form-item label="登录配置"><el-select v-model="form.credentialRef" aria-label="摄像机登录配置" popper-class="fire-dialog" :disabled="saving"><el-option v-for="name in profiles" :key="name" :label="name" :value="name" /></el-select></el-form-item>
        <el-form-item label="启用对讲"><el-switch v-model="form.enabled" :disabled="saving" /></el-form-item>
      </el-form>
      <p v-if="message" role="alert" class="fire-error">{{ message }}</p>
    </div>
    <template #footer>
      <el-button v-if="form.revision >= 0" type="danger" plain :disabled="saving || loading" @click="remove">解除绑定</el-button>
      <el-button :disabled="saving" @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="saving" :disabled="loading || !loaded" @click="save">保存</el-button>
    </template>
  </el-dialog>
</template>
<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getDahuaBinding, saveDahuaBinding, deleteDahuaBinding } from '@/api/fire/dahua'
const props = defineProps({ modelValue: Boolean, deviceCode: String })
const emit = defineEmits(['update:modelValue'])
const visible = computed({ get: () => props.modelValue, set: value => emit('update:modelValue', value) })
const form = reactive({ host: '', port: 37777, credentialRef: 'default', enabled: true, revision: -1 })
const loading = ref(false), saving = ref(false), loaded = ref(false), message = ref(''), profiles = ref([])
let generation = 0
watch(() => [props.modelValue, props.deviceCode], async () => {
  const id = ++generation
  if (!props.modelValue) return
  loading.value = true; loaded.value = false; message.value = ''
  try {
    const { data } = await getDahuaBinding(props.deviceCode)
    if (id !== generation) return
    profiles.value = data.profiles || []
    Object.assign(form, { host: '', port: 37777, credentialRef: profiles.value[0] || 'default', enabled: true, revision: -1 }, data.endpoint || {})
    loaded.value = true
  } catch (e) { if (id === generation) message.value = e.message || '连接配置加载失败，请重新打开重试' }
  finally { if (id === generation) loading.value = false }
})
async function save() {
  if (saving.value || !loaded.value) return
  if (!/^\d{1,3}(\.\d{1,3}){3}$/.test(form.host) || !form.credentialRef) { message.value = '请填写摄像机IP并选择登录配置'; return }
  saving.value = true; message.value = ''
  try { await saveDahuaBinding(props.deviceCode, { ...form }); ElMessage.success('对讲连接已保存'); visible.value = false }
  catch (e) { message.value = e.message || '保存失败，请重试' }
  finally { saving.value = false }
}
async function remove() {
  try { await ElMessageBox.confirm('解除后，该监控将无法使用大华对讲。是否继续？', '解除绑定', { type: 'warning' }) } catch { return }
  saving.value = true; message.value = ''
  try { await deleteDahuaBinding(props.deviceCode, form.revision); ElMessage.success('已解除绑定'); visible.value = false }
  catch (e) { message.value = e.message || '解除失败，请重试' }
  finally { saving.value = false }
}
</script>
