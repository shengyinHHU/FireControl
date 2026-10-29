<template>
  <el-dialog
    v-model="open"
    :title="mapRecord?.name || place?.name || '场所档案'"
    :width="mapRecord ? '620px' : '1000px'"
    class="fire-dialog"
    append-to-body
    ><template v-if="mapRecord"><el-descriptions :column="1" border class="place-record-details" :aria-busy="contactsLoading">
      <el-descriptions-item label="场所类别">{{ mapRecord.category }}</el-descriptions-item>
      <el-descriptions-item label="场所地址">{{ mapRecord.address || '未填写' }}</el-descriptions-item>
      <el-descriptions-item label="行业类别">{{ mapRecord.industryCategory || '未填写' }}</el-descriptions-item>
      <el-descriptions-item label="主要风险源">{{ mapRecord.mainRiskSources || '未填写' }}</el-descriptions-item>
      <el-descriptions-item label="坐标">{{ mapRecord.lng == null || mapRecord.lat == null ? '未定位' : `${mapRecord.lng}, ${mapRecord.lat}（GCJ-02）` }}</el-descriptions-item>
      <template v-for="(contact, index) in contacts" :key="contact.contactId || index">
          <el-descriptions-item label="联系人姓名">
            <div class="place-contact-name">
              <span>{{ contact.contactName || '未填写' }}</span>
              <ContactDialLink class="place-contact-dial" :name="contact.contactName" />
            </div>
          </el-descriptions-item>
          <el-descriptions-item label="联系电话">{{ contact.phone || '未填写' }}</el-descriptions-item>
      </template>
    </el-descriptions>
    <div v-if="contactsError" class="fire-error" role="alert">{{ contactsError }}<el-button link @click="load">重试</el-button></div>
    <p v-else-if="contactsLoading" class="muted" role="status">正在加载联系人…</p>
    <p v-else-if="!contacts.length" class="muted">暂无联系人信息</p>
    </template><div v-else v-loading="loading">
      <div v-if="error" class="fire-error">
        {{ error }}<el-button link @click="load">重试</el-button>
      </div>
      <template v-if="place"
        ><div class="fire-detail-grid">
          <div>
            <label>所属网格 / 场所类别</label>{{ place.gridName }} ·
            {{ place.category }}
          </div>
          <div><label>场所地址</label>{{ place.address }}</div>
          <div><label>风险等级</label><StatusTag :value="place.risk" /></div>
        </div>
        <h3 class="fire-section-title">
          人员信息
          <span class="muted"
            >（{{ people.length }}） · 当前在岗
            {{ place.onDuty?.length || 0 }} 人</span
          >
        </h3>
        <div class="fire-person-grid">
          <div v-for="p in people" :key="p.id" class="fire-person">
            <div class="person-actions">
              <el-button
                v-if="canFire('tasks', 'add')"
                size="small"
                type="primary"
                @click="assign(p)"
                >任务派发</el-button
              ><el-button
                v-if="canFire('cameras', 'preview')"
                size="small"
                @click="dispatch(p)"
                >视频调度</el-button
              ><el-button
                size="small"
                @click="
                  () => {
                    person = p
                    personOpen = true
                  }
                "
                >查看详情</el-button
              >
            </div>
            <strong>{{ p.name }}</strong>
            <p>手机号：{{ p.phone }}</p>
            <p>岗位：{{ p.role }}</p>
          </div>
        </div>
        <div v-if="!people.length" class="fire-empty">
          暂无责任人员，请在人员管理中添加
        </div>
        <el-tabs v-model="tab" style="margin-top: 20px"
          ><el-tab-pane
            :label="`物资信息（${materials.length}）`"
            name="materials"
            ><el-table :data="materials" stripe max-height="400"
              ><el-table-column
                type="index"
                label="序号"
                width="70" /><el-table-column
                prop="category"
                label="类别" /><el-table-column
                prop="name"
                label="名称"
                min-width="200" /><el-table-column
                prop="unit"
                label="单位"
                width="90" /><el-table-column
                prop="quantity"
                label="数量"
                width="90" /></el-table></el-tab-pane
          ><el-tab-pane label="安全生产" name="safety"
            ><el-table :data="hazards" stripe
              ><el-table-column
                prop="name"
                label="风险 / 隐患"
                min-width="220" /><el-table-column
                prop="category"
                label="类型" /><el-table-column
                prop="source"
                label="来源" /><el-table-column
                prop="owner"
                label="责任人" /><el-table-column label="状态"
                ><template #default="{ row }"
                  ><StatusTag
                    :value="
                      row.status
                    " /></template></el-table-column></el-table></el-tab-pane
          ><el-tab-pane label="在岗值班" name="duty"
            ><el-table :data="place.onDuty || []"
              ><el-table-column prop="name" label="姓名" /><el-table-column
                prop="phone"
                label="手机号" /><el-table-column
                prop="start"
                label="上班时间" /><el-table-column
                prop="end"
                label="下班时间" /></el-table></el-tab-pane></el-tabs
      ></template>
    </div>
    <el-dialog
      v-model="taskOpen"
      title="派发检查任务"
      width="520px"
      class="fire-dialog"
      append-to-body
      ><el-form label-position="top"
        ><el-form-item label="接收人">{{ person?.name }}</el-form-item
        ><el-form-item label="任务标题" required
          ><el-input v-model="task.name" maxlength="100" /></el-form-item
        ><el-form-item label="截止时间" required
          ><el-date-picker
            popper-class="fire-dialog"
            v-model="task.deadline"
            type="datetime"
            value-format="YYYY-MM-DD HH:mm:ss" /></el-form-item
        ><el-form-item label="任务内容"
          ><el-input
            v-model="task.description"
            type="textarea"
            :rows="3" /></el-form-item></el-form
      ><template #footer
        ><el-button @click="taskOpen = false">取消</el-button
        ><el-button type="primary" :loading="sending" @click="send"
          >派发任务</el-button
        ></template
      ></el-dialog
    >
    <el-dialog
      v-model="personOpen"
      title="人员详情"
      width="450px"
      class="fire-dialog"
      append-to-body
      ><div v-if="person">
        <h2>{{ person.name }}</h2>
        <p>{{ person.role }} · {{ place?.name }}</p>
        <p>联系电话：{{ person.phone }}</p>
        <el-button
          v-if="canFire('personnel', 'call')"
          type="primary"
          @click="call"
          >VOLTE 视频呼叫</el-button
        >
      </div></el-dialog
    >
    <el-dialog
      v-model="cameraSelect"
      title="选择场所摄像头"
      width="530px"
      class="fire-dialog"
      append-to-body
      ><el-button
        v-for="c in cameras"
        :key="c.id"
        style="display: block; margin: 8px 0; width: 100%"
        @click="
          () => {
            cameraId = c.id
            videoOpen = true
            cameraSelect = false
          }
        "
        >{{ c.name }} · {{ c.status }}</el-button
      ><el-empty
        v-if="!cameras.length"
        description="该场所暂无摄像头" /></el-dialog
    ><VideoDialog v-model="videoOpen" :camera-id="cameraId" @changed="load" />
  </el-dialog>
</template>
<script setup>
import { ref, watch, computed } from 'vue'
import { ElMessageBox } from 'element-plus'
import ContactDialLink from './ContactDialLink.vue'
import { fire, requestCall } from '@/api/fire'
import { listDatabasePlaceContacts } from '@/api/fire/places'
import { canFire } from '@/api/fire/access'
import { attempt } from '@/views/fire/useFire'
import StatusTag from './StatusTag.vue'
import VideoDialog from './VideoDialog.vue'
const props = defineProps({ modelValue: Boolean, placeId: String, mapPlace: { type: Object, default: null } }),
  emit = defineEmits(['update:modelValue', 'changed'])
const open = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v),
})
const place = ref(null),
  people = ref([]),
  materials = ref([]),
  hazards = ref([]),
  loading = ref(false),
  error = ref(''),
  tab = ref('materials'),
  person = ref(null),
  personOpen = ref(false),
  taskOpen = ref(false),
  task = ref({}),
  sending = ref(false),
  cameras = ref([]),
  cameraSelect = ref(false),
  videoOpen = ref(false),
  cameraId = ref('')
const mapRecord = computed(() => props.mapPlace || (place.value?.source === 'database' ? place.value : null))
const contacts = ref([]), contactsLoading = ref(false), contactsError = ref('')
let loadVersion = 0
async function loadContacts(id, version) {
  if (!id) { contactsError.value = '缺少场所编号，无法加载联系人'; return }
  contactsLoading.value = true
  try {
    const rows = await listDatabasePlaceContacts(id)
    if (version === loadVersion) contacts.value = rows
  } catch (e) {
    if (version === loadVersion) contactsError.value = e.message || '联系人加载失败，请重试'
  } finally {
    if (version === loadVersion) contactsLoading.value = false
  }
}
async function load() {
  const version = ++loadVersion
  contacts.value = []
  contactsError.value = ''
  contactsLoading.value = false
  if (props.mapPlace) {
    loading.value = false
    error.value = ''
    await loadContacts(props.mapPlace.id || props.mapPlace.placeId || props.placeId, version)
    return
  }
  if (!props.placeId) return
  loading.value = true
  error.value = ''
  place.value = null
  try {
    const p = await fire.get('places', props.placeId)
    if (version !== loadVersion) return
    if (p.data?.source === 'database') {
      place.value = p.data
      await loadContacts(props.placeId, version)
      return
    }
    const [u, m, h, c] = await Promise.all(
      ['personnel', 'materials', 'hazards', 'cameras'].map(async (r) => {
        if (!canFire(r)) return { rows: [] }
        try {
          return await fire.list(r, { placeId: props.placeId, pageSize: 1000 })
        } catch (e) {
          if (version === loadVersion)
            error.value = `部分关联信息加载失败：${e.message}`
          return { rows: [] }
        }
      }),
    )
    if (version !== loadVersion) return
    place.value = p.data
    people.value = u.rows
    materials.value = m.rows
    hazards.value = h.rows
    cameras.value = c.rows
  } catch (e) {
    if (version === loadVersion) error.value = e.message
  } finally {
    if (version === loadVersion) loading.value = false
  }
}
function assign(p) {
  person.value = p
  task.value = {
    name: '',
    deadline: '',
    description: '',
    placeId: props.placeId,
    personnelId: p.id,
  }
  taskOpen.value = true
}
async function send() {
  sending.value = true
  if (await attempt(() => fire.create('tasks', task.value), '任务已派发')) {
    taskOpen.value = false
    emit('changed')
  }
  sending.value = false
}
function dispatch(p) {
  person.value = p
  cameraSelect.value = true
}
async function call() {
  const res = await attempt(() => requestCall(person.value.id))
  if (res) ElMessageBox.alert(res.data.message || '呼叫请求已提交', '视频呼叫')
}
watch(
  () => [props.modelValue, props.placeId, props.mapPlace],
  () => {
    if (props.modelValue) load()
    else loadVersion++
  },
  { immediate: true },
)
</script>

<style scoped>
.place-record-details :deep(.el-descriptions__table) { table-layout: fixed; }
.place-record-details :deep(.el-descriptions__label.el-descriptions__cell) { width: 140px; }
.place-record-details :deep(.el-descriptions__content) { overflow-wrap: anywhere; }
.place-contact-name { display: flex; align-items: center; flex-wrap: wrap; gap: 8px 16px; }
.place-contact-dial :deep(.el-link__inner) { gap: 4px; }
</style>
