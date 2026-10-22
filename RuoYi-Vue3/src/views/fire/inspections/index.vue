<template>
  <div class="fire-theme">
    <el-tabs v-model="tab" class="inspection-tabs">
      <el-tab-pane label="查岗管理" name="inspections"
        ><ResourcePage resource="inspections"
      /></el-tab-pane>
      <el-tab-pane label="视频监控" name="video" lazy
        ><div class="fire-page">
          <h1>视频监控</h1>
          <VideoMonitor v-if="tab === 'video'" /></div
      ></el-tab-pane>
    </el-tabs>
  </div>
</template>
<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import ResourcePage from '@/components/Fire/ResourcePage.vue'
import VideoMonitor from '@/components/Fire/VideoMonitor.vue'
const route = useRoute(),
  router = useRouter()
const tab = computed({
  get: () => (route.query.tab === 'video' ? 'video' : 'inspections'),
  set: (value) =>
    router.replace({
      query: { ...route.query, tab: value === 'video' ? 'video' : undefined },
    }),
})
</script>
<style scoped>
.inspection-tabs {
  padding: 0 20px;
}
.inspection-tabs :deep(.el-tabs__header) {
  margin-bottom: 0;
}
</style>
