<template>
  <div class="fire-page fire-theme">
    <h1>视频监控</h1>
    <el-tabs v-model="tab" :before-leave="beforeTabChange" @tab-change="rememberTab">
      <el-tab-pane label="视频预览" name="preview" /><el-tab-pane label="坐标维护" name="coordinates" />
    </el-tabs>
    <VideoMonitor v-if="tab === 'preview'" directory-tree />
    <VideoCoordinateManager v-else ref="coordinates" />
  </div>
</template>
<script setup>
import VideoMonitor from '@/components/Fire/VideoMonitor.vue'
import VideoCoordinateManager from '@/components/Fire/VideoCoordinateManager.vue'
import { ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
const route = useRoute(), router = useRouter()
const tab = ref(route.query.tab === 'coordinates' ? 'coordinates' : 'preview')
const coordinates = ref()
const beforeTabChange = () => tab.value === 'coordinates' ? coordinates.value?.canLeave() ?? true : true
watch(() => route.query.tab, value => { tab.value = value === 'coordinates' ? 'coordinates' : 'preview' })
function rememberTab() { router.replace({ query: { ...route.query, tab: tab.value } }) }
</script>
