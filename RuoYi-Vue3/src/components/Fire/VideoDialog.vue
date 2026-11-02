<template>
  <LiveVideoDialog v-if="camera" v-model="open" :device="camera" />
  <el-dialog v-else v-model="open" title="视频监控" class="fire-dialog" width="min(600px, 95vw)" append-to-body>
    <p v-if="loading" role="status">正在加载真实监控资料…</p>
    <p v-else class="fire-error" role="alert">{{ error }}</p>
    <template #footer><el-button @click="open = false">关闭</el-button><el-button v-if="!loading" type="primary" @click="load">重试</el-button></template>
  </el-dialog>
</template>
<script setup>
import { ref, computed, watch, onBeforeUnmount } from 'vue'
import { getVideoCamera } from '@/api/fire/videoCatalog'
import LiveVideoDialog from './LiveVideoDialog.vue'
const props = defineProps({ modelValue: Boolean, cameraId: String }), emit = defineEmits(['update:modelValue'])
const open = computed({ get: () => props.modelValue, set: value => emit('update:modelValue', value) })
const camera = ref(null), loading = ref(false), error = ref('')
let version = 0
async function load() {
  const current = ++version; camera.value = null; loading.value = true; error.value = ''
  try { const response = await getVideoCamera(props.cameraId); if (current === version && open.value) camera.value = response.data }
  catch (e) { if (current === version) error.value = e.message || '监控不存在或无权访问' }
  finally { if (current === version) loading.value = false }
}
watch(() => [props.modelValue, props.cameraId], ([visible]) => { if (visible && props.cameraId) load(); else { version++; camera.value = null } }, { immediate: true })
onBeforeUnmount(() => version++)
</script>
