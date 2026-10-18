<template>
  <div class="fire-public">
    <div class="fire-public-card" style="max-width: 950px" v-loading="loading">
      <div class="fire-kicker">AUTHORIZED VIDEO</div>
      <h1>{{ share?.name || '授权视频监控' }}</h1>
      <p>大厂街道智慧消防监管平台 · 视频分享</p>
      <div v-if="error" class="fire-error">
        {{ error }}<el-button @click="load">重试</el-button>
      </div>
      <div v-if="session?.mock" class="public-video-demo">
        <el-icon size="60"><VideoCamera /></el-icon>
        <h2>演示视频授权已验证</h2>
        <p>未连接真实摄像头。正式环境由后端返回短期视频地址。</p>
      </div>
      <video
        v-else-if="session?.url"
        :src="session.url"
        controls
        autoplay
        muted
        playsinline
        style="width: 100%"
        @error="error = '播放失败，授权可能已过期或流格式不受支持'"
      />
      <p v-if="share">
        授权有效期至 {{ new Date(share.expiresAt).toLocaleString('zh-CN') }}
      </p>
    </div>
  </div>
</template>
<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { resolveShare, previewPublicVideo } from '@/api/fire'
import '@/assets/styles/fire.scss'
const route = useRoute(),
  share = ref(null),
  session = ref(null),
  loading = ref(false),
  error = ref('')
async function load() {
  loading.value = true
  error.value = ''
  try {
    const token = String(route.query.token || '')
    share.value = await resolveShare(token, 'video')
    session.value = await previewPublicVideo(token)
  } catch (e) {
    error.value = e.message
    share.value = null
    session.value = null
  } finally {
    loading.value = false
  }
}
onMounted(load)
</script>
<style scoped>
.public-video-demo {
  background: repeating-linear-gradient(
    0deg,
    #102d42,
    #102d42 3px,
    #0c273c 4px,
    #0c273c 6px
  );
  padding: 90px 20px;
  text-align: center;
  color: #5796ad;
  border: 1px solid #24516a;
}
.public-video-demo h2 {
  font-size: 20px;
}
</style>
