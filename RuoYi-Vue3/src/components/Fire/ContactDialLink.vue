<template>
  <el-link class="contact-dial" type="primary" :underline="false"
    href="https://www.jnrhtx.top/meeting/index?from=Dc" target="_blank" rel="noopener noreferrer"
    :aria-label="`${name || '联系人'}的拨号页面（新窗口打开）`">
    <el-icon aria-hidden="true"><Phone /></el-icon><span>拨号</span>
  </el-link>
</template>
<script setup>
import { Phone } from '@element-plus/icons-vue'
defineProps({ name: String })
</script>
<style scoped>
.contact-dial :deep(.el-link__inner) { gap: 4px; }
</style>
