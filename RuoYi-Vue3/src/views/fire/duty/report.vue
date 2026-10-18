<template>
  <div class="fire-public">
    <div class="fire-public-card" v-loading="loading">
      <div class="fire-kicker">DA CHANG / FIRE SAFETY</div>
      <h1>每日值班上报</h1>
      <p>大厂街道智慧消防监管平台</p>
      <div v-if="error" class="fire-error">
        {{ error }}<el-button @click="load">重试</el-button>
      </div>
      <template v-if="share"
        ><el-alert
          v-if="isFireMock"
          title="演示上报环境，数据仅保存在此浏览器"
          type="info"
          :closable="false"
        />
        <h3 class="fire-section-title">{{ share.name }}</h3>
        <el-result
          v-if="submitted"
          icon="success"
          title="值班表已上报"
          sub-title="请按班次履行值守职责，可关闭此页面。"
        /><el-form v-else label-position="top" @submit.prevent="submit"
          ><el-form-item label="值班人员姓名" required
            ><el-input
              v-model="form.name"
              placeholder="请输入真实姓名"
              maxlength="30" /></el-form-item
          ><el-form-item label="手机号码" required
            ><el-input
              v-model="form.phone"
              placeholder="请输入11位手机号"
              maxlength="11"
              inputmode="tel" /></el-form-item
          ><el-form-item label="值班日期" required
            ><el-date-picker
              popper-class="fire-dialog"
              v-model="form.date"
              type="date"
              value-format="YYYY-MM-DD"
              :clearable="false"
          /></el-form-item>
          <div class="fire-form-grid">
            <el-form-item label="上班时间" required
              ><el-time-select
                popper-class="fire-dialog"
                v-model="form.start"
                start="00:00"
                step="00:30"
                end="23:30" /></el-form-item
            ><el-form-item label="下班时间" required
              ><el-time-select
                popper-class="fire-dialog"
                v-model="form.end"
                start="00:00"
                step="00:30"
                end="23:30"
            /></el-form-item>
          </div>
          <p>下班时间早于上班时间时，按次日下班计算。</p>
          <el-button
            type="primary"
            size="large"
            native-type="submit"
            :loading="saving"
            style="width: 100%"
            >提交值班表</el-button
          ></el-form
        ></template
      >
    </div>
  </div>
</template>
<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { resolveShare, submitPublicDuty } from '@/api/fire'
import { isFireMock } from '@/api/fire/config'
import { attempt } from '../useFire'
import '@/assets/styles/fire.scss'
const route = useRoute(),
  share = ref(null),
  error = ref(''),
  loading = ref(false),
  saving = ref(false),
  submitted = ref(false)
const form = ref({
  name: '',
  phone: '',
  date: new Intl.DateTimeFormat('en-CA', { timeZone: 'Asia/Shanghai' }).format(
    new Date(),
  ),
  start: '08:00',
  end: '20:00',
  submissionId: crypto.randomUUID(),
})
async function load() {
  loading.value = true
  error.value = ''
  share.value = null
  try {
    share.value = await resolveShare(String(route.query.token || ''), 'duty')
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}
async function submit() {
  if (saving.value) return
  saving.value = true
  const r = await attempt(() =>
    submitPublicDuty(String(route.query.token || ''), form.value),
  )
  if (r) submitted.value = true
  saving.value = false
}
onMounted(load)
</script>
