<template>
  <el-dialog
    v-model="open"
    :title="alarm?.sourceCode === '119' ? '119警情详情' : alarm?.dataSource === 'database' ? '设备事件详情' : '警情详情与处置'"
    width="850px"
    class="fire-dialog"
    append-to-body
    ><div v-loading="loading">
      <div v-if="error" class="fire-error">
        {{ error }}<el-button @click="load">重试</el-button>
      </div>
      <template v-if="alarm?.sourceCode === '119'">
        <h2>{{ alarm.caseTypeName || '119警情' }}</h2>
        <p class="muted">119 原始案件状态保持同步，本地处理不会修改来源系统。</p>
        <el-descriptions :column="1" border>
          <el-descriptions-item label="警情编号">{{ alarm.code }}</el-descriptions-item>
          <el-descriptions-item label="来警时间">{{ alarm.occurredAt || '未提供' }}</el-descriptions-item>
          <el-descriptions-item label="案件类型">{{ alarm.caseTypeName || '未提供' }}</el-descriptions-item>
          <el-descriptions-item label="案件状态">{{ alarm.status }}</el-descriptions-item>
          <el-descriptions-item label="案件性质">{{ alarm.caseNatureName || '未提供' }}</el-descriptions-item>
          <el-descriptions-item label="案件等级">{{ alarm.caseLevelName || '未提供' }}</el-descriptions-item>
          <el-descriptions-item label="合并状态">{{ alarm.mergeStatusName || '未提供' }}</el-descriptions-item>
          <el-descriptions-item label="案发地址">{{ alarm.address || '未提供' }}</el-descriptions-item>
          <el-descriptions-item label="警情描述">{{ alarm.description || '未提供' }}</el-descriptions-item>
          <el-descriptions-item label="所属地区">{{ incidentRegion }}</el-descriptions-item>
          <el-descriptions-item label="地图坐标">{{ alarm.coordinateStatus === 'valid' ? `${alarm.lng}, ${alarm.lat}（高德GCJ-02）` : '坐标待核验，暂不落点' }}</el-descriptions-item>
          <el-descriptions-item label="最近同步">{{ alarm.lastSyncedAt || '未提供' }}</el-descriptions-item>
        </el-descriptions>
      </template>
      <template v-else-if="alarm?.dataSource === 'database'">
        <h2>{{ alarm.name }}</h2>
        <p class="muted">设备事件记录；不代表现场仍有火情，也不代表已完成处置。</p>
        <el-descriptions :column="1" border>
          <el-descriptions-item label="所属单位">{{ alarm.placeName }}</el-descriptions-item>
          <el-descriptions-item label="传输装置">{{ alarm.gatewayName }}</el-descriptions-item>
          <el-descriptions-item label="协议对象">{{ alarm.deviceId }}</el-descriptions-item>
          <el-descriptions-item label="服务器接收时间">{{ alarm.occurredAt }}</el-descriptions-item>
          <el-descriptions-item label="设备事件时间">{{ alarm.deviceTime || '未提供' }}</el-descriptions-item>
          <el-descriptions-item label="时间核验">{{ alarm.clockWarning ? '设备时间异常或未提供，请核验' : '未发现明显偏差' }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ alarm.status }}</el-descriptions-item>
        </el-descriptions>
        <h3>协议解析结果</h3><pre style="white-space:pre-wrap;overflow-wrap:anywhere;max-height:320px;overflow:auto">{{ alarm.details }}</pre>
        <el-button @click="placeOpen = true">单位档案</el-button>
      </template>
      <template v-else-if="alarm"
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
      <section v-if="alarm?.dataSource === 'database' && canFire('alarms')" style="margin-top:24px">
        <h3>处理说明</h3>
        <el-input v-model="note" type="textarea" :rows="4" maxlength="1000" show-word-limit
          :disabled="saving" aria-label="处理说明" placeholder="请填写核查情况及处理结果（必填）" />
        <el-button type="primary" :loading="saving" :disabled="!note.trim()" @click="handleLocal">确认处理</el-button>
      </section>
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
import { handleAlarmLocally } from '@/api/fire/localAlarmHandling'
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
const incidentRegion = computed(() => alarm.value?.streetRegion?.split('；地区=')[1] || '大厂街道')
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
      if (a.dataSource !== 'database' && a.deviceId && canFire('devices')) {
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
async function handleLocal() {
  if (saving.value || !alarm.value || alarm.value.id !== props.alarmId) return
  const target = alarm.value
  saving.value = true
  try {
    const result = await attempt(async () => { handleAlarmLocally(target, note.value); return true }, '处理完成，该警情已隐藏')
    if (result) { if (props.alarmId === target.id) { note.value = ''; open.value = false }; emit('changed') }
  } finally { saving.value = false }
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
