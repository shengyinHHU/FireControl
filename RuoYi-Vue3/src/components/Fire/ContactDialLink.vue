<template>
  <el-link class="contact-dial" type="primary" :underline="false" :disabled="busy || !phone?.trim()" :title="`${name || '联系人'}：${phone || '未填写电话'}`" @click="call">
    <el-icon aria-hidden="true"><Phone /></el-icon><span>拨号</span>
  </el-link>
</template>
<script setup>
import { ref } from 'vue'
import { buildPlatformCallUrl } from '@/utils/firePlatformCall'
import { ElMessage } from 'element-plus'
import { Phone } from '@element-plus/icons-vue'
const props = defineProps({ name: String, phone: String })
const busy = ref(false)
async function call() {
  if (busy.value) return
  busy.value = true
  try {
    window.location.assign(buildPlatformCallUrl({ name: props.name, phone: props.phone }))
  } catch (e) {
    ElMessage.error(e?.message || '无法打开呼叫页面')
    busy.value = false
  }
}
</script>
<style scoped>.contact-dial :deep(.el-link__inner){gap:4px}</style>
