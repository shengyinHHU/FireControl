<template>
  <el-dialog
    v-model="open"
    :title="kind === 'duty' ? '值班表上报二维码' : '告警视频分享'"
    width="480px"
    class="fire-dialog"
    append-to-body
    ><div v-loading="loading" style="text-align: center">
      <div v-if="error" class="fire-error">
        {{ error }}<el-button @click="generate">重试</el-button>
      </div>
      <template v-if="url"
        ><img
          :src="image"
          alt="扫码打开授权页面"
          width="240"
          height="240"
          style="
            background: white;
            border: 12px solid white;
            border-radius: 5px;
          "
        />
        <p class="muted">
          {{
            kind === 'duty'
              ? '手机扫码填写每日值班信息'
              : '扫码查看关联位置的授权视频'
          }}
        </p>
        <p v-if="isFireMock" class="muted">
          演示授权保存在当前浏览器。跨手机演示需部署后端授权服务。
        </p>
        <p class="muted">有效期至 {{ expires }}</p>
        <el-input :model-value="url" readonly /><el-button
          style="margin-top: 15px"
          @click="copy"
          >复制链接</el-button
        ><el-button @click="visit">打开页面</el-button></template
      >
    </div></el-dialog
  >
</template>
<script setup>
import { ref, computed, watch, onBeforeUnmount } from 'vue'
import QRCode from 'qrcode'
import { ElMessage } from 'element-plus'
import { createLatestRequest } from '@/api/fire/rules'
import { createShare } from '@/api/fire'
import { canFire } from '@/api/fire/access'
import { isFireMock } from '@/api/fire/config'
const props = defineProps({
    modelValue: Boolean,
    kind: String,
    resourceId: String,
  }),
  emit = defineEmits(['update:modelValue'])
const open = computed({
    get: () => props.modelValue,
    set: (v) => emit('update:modelValue', v),
  }),
  url = ref(''),
  image = ref(''),
  expires = ref(''),
  error = ref(''),
  loading = ref(false)
const latest = createLatestRequest()
async function generate() {
  if (!canFire(props.kind === 'duty' ? 'duty' : 'cameras', 'share')) {
    error.value = '无分享权限'
    return
  }
  if (!props.resourceId) return
  loading.value = true
  error.value = ''
  url.value = ''
  const kind = props.kind,
    resourceId = props.resourceId
  await latest.run(
    async () => {
      const { data } = await createShare({ kind, resourceId })
      const base = import.meta.env.VITE_FIRE_PUBLIC_ORIGIN || location.origin
      const link =
        data.url ||
        `${base}/fire-public/${kind}?token=${encodeURIComponent(data.token)}`
      const qr = await QRCode.toDataURL(link, {
        width: 240,
        margin: 2,
        errorCorrectionLevel: 'M',
      })
      return { link, qr, expiry: data.expiresAt }
    },
    ({ link, qr, expiry }) => {
      url.value = link
      image.value = qr
      expires.value = new Date(expiry).toLocaleString('zh-CN')
      loading.value = false
    },
    (e) => {
      error.value = e.message
      loading.value = false
    },
  )
}
onBeforeUnmount(() => latest.cancel())
async function copy() {
  try {
    await navigator.clipboard.writeText(url.value)
    ElMessage.success('链接已复制')
  } catch {
    ElMessage.info('请从链接框手动复制')
  }
}
function visit() {
  window.open(url.value, '_blank', 'noopener,noreferrer')
}
watch(
  () => [props.modelValue, props.resourceId],
  () => {
    if (props.modelValue) generate()
    else {
      latest.cancel()
      url.value = ''
      loading.value = false
    }
  },
  { immediate: true },
)
</script>
