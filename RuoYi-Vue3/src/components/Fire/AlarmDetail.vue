<template>
  <el-dialog
    v-model="open"
    title="警情详情与处置"
    width="850px"
    class="fire-dialog"
    append-to-body
    ><div v-loading="loading">
      <div v-if="error" class="fire-error">
        {{ error }}<el-button @click="load">重试</el-button>
      </div>
      <template v-if="alarm"
        ><div class="alarm-heading">
          <div>
            <small class="muted">{{ alarm.code }}</small>
            <h2>{{ alarm.name }}</h2>
          </div>
          <StatusTag :value="alarm.status" />
        </div>
        <div class="fire-detail-grid">
          <div><label>来源</label>{{ alarm.source }}</div>
          <div><label>发生场所</label>{{ alarm.placeName }}</div>
          <div><label>发生时间</label>{{ alarm.occurredAt }}</div>
        </div>
        <div class="fire-toolbar">
          <el-button @click="placeOpen = true">场所档案</el-button
          ><el-button
            v-if="canFire('cameras', 'preview')"
            :disabled="!device?.cameraId"
            @click="videoOpen = true"
            >联动视频</el-button
          ><el-button
            v-if="canFire('cameras', 'share')"
            :disabled="!device?.cameraId"
            @click="shareOpen = true"
            >视频二维码</el-button
          >
        </div>
        <p v-if="!device?.cameraId" class="muted">
          尚未关联摄像头，请在设备台账中完成手动绑定。
        </p>
        <h3 class="fire-section-title">处置时间线</h3>
        <el-timeline
          ><el-timeline-item
            v-for="(t, i) in alarm.timeline"
            :key="i"
            :timestamp="t.time"
            color="#29b8ce"
            ><strong>{{ t.action }} · {{ t.operator }}</strong>
            <p>{{ t.note }}</p></el-timeline-item
          ></el-timeline
        ><template v-if="alarm.status !== '已办结' && canFire('alarms', 'edit')"
          ><el-input
            v-model="note"
            type="textarea"
            :rows="3"
            maxlength="1000"
            show-word-limit
            placeholder="填写接单说明或现场核查、处置结果（必填）"
          />
          <div style="text-align: right; margin-top: 16px">
            <el-button type="primary" :loading="saving" @click="transition">{{
              alarm.status === '待处理' ? '接单处理' : '确认办结'
            }}</el-button>
          </div></template
        ></template
      >
    </div>
    <PlaceDetail v-model="placeOpen" :place-id="alarm?.placeId" /><VideoDialog
      v-model="videoOpen"
      :camera-id="device?.cameraId"
      @changed="emit('changed')" /><ShareCode
      v-model="shareOpen"
      kind="video"
      :resource-id="device?.cameraId"
  /></el-dialog>
</template>
<script setup>
import { ref, computed, watch, onBeforeUnmount } from 'vue'
import { fire, transitionAlarm } from '@/api/fire'
import { createLatestRequest } from '@/api/fire/rules'
import { canFire } from '@/api/fire/access'
import { attempt } from '@/views/fire/useFire'
import StatusTag from './StatusTag.vue'
import PlaceDetail from './PlaceDetail.vue'
import VideoDialog from './VideoDialog.vue'
import ShareCode from './ShareCode.vue'
const props = defineProps({ modelValue: Boolean, alarmId: String }),
  emit = defineEmits(['update:modelValue', 'changed'])
const open = computed({
    get: () => props.modelValue,
    set: (v) => emit('update:modelValue', v),
  }),
  alarm = ref(null),
  device = ref(null),
  note = ref(''),
  loading = ref(false),
  saving = ref(false),
  error = ref(''),
  placeOpen = ref(false),
  videoOpen = ref(false),
  shareOpen = ref(false)
const latest = createLatestRequest()
async function load() {
  if (!props.alarmId) return
  loading.value = true
  error.value = ''
  alarm.value = null
  device.value = null
  const id = props.alarmId
  await latest.run(
    async () => {
      const a = (await fire.get('alarms', id)).data
      let d = null
      if (a.deviceId && canFire('devices')) {
        try {
          d = (await fire.get('devices', a.deviceId)).data
        } catch {
          d = null
        }
      }
      return { a, d }
    },
    ({ a, d }) => {
      alarm.value = a
      device.value = d
      loading.value = false
    },
    (e) => {
      error.value = e.message
      loading.value = false
    },
  )
}
async function transition() {
  if (
    saving.value ||
    !alarm.value ||
    alarm.value.id !== props.alarmId ||
    !canFire('alarms', 'edit')
  )
    return
  const targetId = alarm.value.id
  saving.value = true
  if (
    await attempt(
      () =>
        transitionAlarm(targetId, {
          action: alarm.value.status === '待处理' ? 'accept' : 'close',
          note: note.value,
        }),
      '处置记录已保存',
    )
  ) {
    note.value = ''
    await load()
    emit('changed')
  }
  saving.value = false
}
watch(
  () => [props.modelValue, props.alarmId],
  () => {
    if (props.modelValue) {
      note.value = ''
      load()
    } else {
      latest.cancel()
      loading.value = false
      alarm.value = null
    }
  },
  { immediate: true },
)
onBeforeUnmount(() => latest.cancel())
</script>
<style scoped>
.alarm-heading {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 25px;
}
.alarm-heading h2 {
  font-size: 20px;
  margin: 10px 0;
}
.el-timeline {
  padding: 15px 0 5px 8px;
}
.el-timeline p {
  color: #8eacc0;
}
</style>
