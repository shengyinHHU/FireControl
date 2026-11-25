<template>
  <el-dialog v-model="open" title="模拟报警" class="fire-dialog alarm-simulation-dialog" width="min(580px, 94vw)" append-to-body :close-on-click-modal="!saving" :close-on-press-escape="!saving" :show-close="!saving" :before-close="beforeClose">
    <p class="muted">生成带“模拟演示”标记的警情，演示大屏记录、地图定位、声光提醒和处理流程。</p>
    <div v-if="error" class="fire-error" role="alert">{{ error }}<el-button v-if="!loaded" link :disabled="saving" @click="loadPlaces">重新加载</el-button></div>
    <el-form ref="form" :model="values" :rules="rules" label-position="top" novalidate @submit.prevent="submit">
      <el-form-item label="报警来源" prop="source"><el-radio-group v-model="values.source" :disabled="saving"><el-radio-button value="device">传输装置</el-radio-button><el-radio-button value="119">119警情</el-radio-button></el-radio-group></el-form-item>
      <el-form-item label="演示场所" prop="placeId">
        <el-select v-model="values.placeId" filterable clearable :loading="loading" :disabled="saving || loading" placeholder="选择大厂范围内有坐标的场所" style="width:100%" aria-label="演示场所" popper-class="fire-dialog">
          <el-option v-for="place in places" :key="place.id" :value="place.id" :label="place.name" />
        </el-select>
        <p v-if="loaded && !places.length" class="muted" role="status">暂无可用场所，请先在场所档案中补充大厂范围内的高德坐标。</p>
      </el-form-item>
      <p v-if="selected" class="simulation-location">{{ selected.address || selected.name }}<br />地图位置：{{ selected.lng }}, {{ selected.lat }}（GCJ-02）</p>
      <el-form-item label="报警内容" prop="description"><el-input v-model="values.description" type="textarea" :rows="3" resize="none" maxlength="500" show-word-limit :disabled="saving" /></el-form-item>
      <el-checkbox v-if="canFire('dashboard')" v-model="enterDashboard" :disabled="saving">生成后进入指挥大屏演示</el-checkbox>
      <p class="muted">在详情中填写说明并确认处理后，本条模拟警情会从大屏和地图移除。若仍有其他未确认的新警情，声光提醒会继续。</p>
    </el-form>
    <template #footer><el-button :disabled="saving" @click="open = false">取消</el-button><el-button type="primary" :loading="saving" :disabled="loading || !loaded || !places.length" @click="submit">触发模拟报警</el-button></template>
  </el-dialog>
</template>
<script setup>
import { computed, nextTick, onBeforeUnmount, reactive, ref, watch } from 'vue'
import { listDashboardPlaces } from '@/api/fire/places'
import { isInDachang } from '@/api/fire/dachangScope'
import { createAlarmSimulation, simulationRequestId } from '@/api/fire/alarmSimulation'
import { canFire } from '@/api/fire/access'
const props = defineProps({ modelValue: Boolean, source: String })
const emit = defineEmits(['update:modelValue', 'created'])
const open = computed({ get: () => props.modelValue, set: value => { if (!saving.value) emit('update:modelValue', value) } })
const form = ref(), loading = ref(false), loaded = ref(false), saving = ref(false), error = ref(''), places = ref([]), enterDashboard = ref(true)
const values = reactive({ source: 'device', placeId: '', description: '' })
const selected = computed(() => places.value.find(place => place.id === values.placeId))
const rules = {
  source: [{ required: true, message: '请选择报警来源', trigger: 'change' }],
  placeId: [{ required: true, message: '请选择演示场所', trigger: 'change' }],
  description: [{ validator: (_, value, done) => done(value?.trim() ? undefined : new Error('请填写报警内容')), trigger: 'blur' }],
}
let generation = 0, requestId = '', disposed = false
function beforeClose(done) { if (!saving.value) done() }
async function loadPlaces() {
  const current = ++generation
  loading.value = true; loaded.value = false; error.value = ''
  try {
    const result = await listDashboardPlaces()
    if (current !== generation || disposed) return
    places.value = result.rows.filter(place => place.crs === 'GCJ02' && isInDachang(place))
    loaded.value = true
  } catch (e) { if (current === generation && !disposed) error.value = e.message || '场所加载失败，请重试' }
  finally { if (current === generation && !disposed) loading.value = false }
}
async function submit() {
  if (saving.value || loading.value || !loaded.value) return
  saving.value = true; error.value = ''
  try {
    if (!await form.value.validate().catch(() => false)) {
      saving.value = false
      await nextTick()
      form.value.$el.querySelector('.is-error input:not([disabled]), .is-error textarea:not([disabled])')?.focus()
      return
    }
    if (!selected.value) { error.value = '请选择可用的演示场所'; return }
    const result = await createAlarmSimulation({ ...values, description: values.description.trim(), requestId })
    if (disposed) return
    saving.value = false
    emit('created', { id: result.data.id, enterDashboard: enterDashboard.value })
    open.value = false
  } catch (e) { if (!disposed) error.value = (e.message || '模拟报警生成失败') + '；可重试，同一次请求不会重复生成记录' }
  finally { saving.value = false }
}
watch(() => [values.source, values.placeId, values.description], () => { if (!saving.value) requestId = simulationRequestId() }, { flush: 'sync' })
watch(() => props.modelValue, value => {
  if (value) {
    values.source = props.source === '119' ? '119' : 'device'; values.placeId = ''; values.description = '消防声光报警联动演示，请按流程核查处理'
    requestId = simulationRequestId(); enterDashboard.value = canFire('dashboard'); places.value = []; form.value?.clearValidate(); loadPlaces()
  } else { generation++; loading.value = false }
})
onBeforeUnmount(() => { disposed = true; generation++ })
</script>
<style scoped>
.simulation-location { color: var(--fire-muted); font-size: 13px; line-height: 1.6; overflow-wrap: anywhere; }
.alarm-simulation-dialog .muted { line-height: 1.6; }
</style>
