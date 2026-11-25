<template>
  <el-link class="contact-dial" type="primary" :underline="false" :disabled="busy || !phone?.trim()"
    role="button" :tabindex="busy || !phone?.trim() ? -1 : 0" :aria-disabled="busy || !phone?.trim()"
    :aria-label="`${name || '联系人'}的拨号（新标签页打开）`" :title="`${name || '联系人'}：${phone || '未填写电话'}`"
    @click="call" @keydown.enter.prevent="call" @keydown.space.prevent="call">
    <el-icon aria-hidden="true"><Phone /></el-icon><span>拨号</span>
  </el-link>
</template>
<script setup>
import { ref, onBeforeUnmount } from 'vue'
import { buildPlatformCallUrl } from '@/utils/firePlatformCall'
import { ElMessage } from 'element-plus'
import { Phone } from '@element-plus/icons-vue'
const props = defineProps({ name: String, phone: String })
const busy = ref(false)
let unlockTimer
onBeforeUnmount(() => clearTimeout(unlockTimer))
async function call() {
  if (busy.value || !props.phone?.trim()) return
  busy.value = true
  try {
    window.open(buildPlatformCallUrl({ name: props.name, phone: props.phone }), '_blank', 'noopener,noreferrer')
    unlockTimer = setTimeout(() => { busy.value = false }, 1000)
  } catch (e) {
    ElMessage.error(e?.message || '无法打开呼叫页面')
    busy.value = false
  }
}
</script>
<style scoped>.contact-dial :deep(.el-link__inner){gap:4px}</style>
