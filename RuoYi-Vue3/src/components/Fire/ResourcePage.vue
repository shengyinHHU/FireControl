<template>
  <div class="fire-page fire-theme">
    <div class="fire-page-title">
      <div>
        <div class="fire-kicker">DA CHANG / FIRE SAFETY</div>
        <h1>{{ definition.title }}</h1>
        <p>{{ definition.subtitle }}</p>
      </div>
      <div class="fire-toolbar">
        <router-link
          v-if="canFire('dashboard')"
          to="/fire/dashboard"
          class="muted"
          >指挥大屏 ↗</router-link
        ><slot name="actions" /><el-button
          v-if="!definition.readOnly && canFire(resource, 'add')"
          type="primary"
          @click="edit()"
          ><el-icon><Plus /></el-icon> 新增{{
            definition.title.replace('管理', '').replace('台账', '')
          }}</el-button
        >
      </div>
    </div>
    <slot name="before" />
    <div class="fire-table-card">
      <div class="fire-filters">
        <el-input
          v-model="query.keyword"
          placeholder="搜索名称、场所、联系人"
          clearable
          @keyup.enter="search"
          @clear="search"
          ><template #prefix
            ><el-icon><Search /></el-icon></template
        ></el-input>
        <el-select
          popper-class="fire-dialog"
          v-for="f in filters"
          :key="f.key"
          v-model="query[f.key]"
          :placeholder="f.label"
          clearable
          @change="search"
          ><el-option v-for="o in f.options" :key="o" :label="o" :value="o"
        /></el-select>
        <el-select
          popper-class="fire-dialog"
          v-if="definition.fields.some((f) => f.key === 'placeId')"
          v-model="query.placeId"
          placeholder="全部场所"
          clearable
          filterable
          @change="search"
          ><el-option
            v-for="p in options.places"
            :key="p.id"
            :label="p.name"
            :value="p.id"
        /></el-select>
        <el-date-picker
          popper-class="fire-dialog"
          v-if="definition.dateFilter"
          v-model="dates"
          type="daterange"
          value-format="YYYY-MM-DD"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          @change="changeDates"
        />
        <el-button type="primary" plain @click="search">查询</el-button
        ><el-button @click="clearQuery">重置</el-button>
      </div>
      <div v-if="error" class="fire-error">
        {{ error }}<el-button link @click="load">重新加载</el-button>
      </div>
      <el-table
        v-loading="loading"
        :data="rows"
        stripe
        row-key="id"
        empty-text="暂无符合条件的记录"
        @row-dblclick="details"
        ><el-table-column type="index" label="#" width="55" /><el-table-column
          v-for="key in definition.columns"
          :key="key"
          :prop="key"
          :label="label(key)"
          :min-width="key === 'name' ? 220 : key === 'placeName' ? 170 : 120"
          show-overflow-tooltip
          ><template #default="{ row }"
            ><StatusTag
              v-if="['status', 'risk', 'result', 'level'].includes(key)"
              :value="row[key]"
            /><span v-else-if="key === 'enabled'">{{
              row[key] ? '已启用' : '已停用'
            }}</span
            ><span v-else>{{ row[key] ?? '—' }}</span></template
          ></el-table-column
        >
        <el-table-column
          label="操作"
          fixed="right"
          :width="resource === 'cameras' ? 240 : 210"
          ><template #default="{ row }"
            ><el-button
              v-if="resource !== 'cameras' || canFire('cameras', 'preview')"
              link
              type="primary"
              @click="details(row)"
              >{{ resource === 'cameras' ? '预览 / 查岗' : '详情' }}</el-button
            ><el-button
              v-if="resource === 'grids'"
              link
              type="primary"
              @click="
                () => {
                  gridRow = row
                  gridOpen = true
                }
              "
              >网格地图</el-button
            ><el-button
              v-if="resource === 'grids' && canFire('grids', 'call')"
              link
              @click="callGrid(row)"
              >呼叫</el-button
            ><el-button
              v-if="
                resource === 'messages' &&
                row.status === '未读' &&
                canFire(resource, 'edit')
              "
              link
              type="primary"
              @click="read(row)"
              >已读</el-button
            ><el-button
              v-if="resource === 'duty' && canFire('duty', 'share')"
              link
              type="primary"
              @click="
                () => {
                  shareRow = row
                  shareOpen = true
                }
              "
              >上报码</el-button
            ><el-button
              v-if="!definition.readOnly && canFire(resource, 'edit')"
              link
              type="primary"
              @click="edit(row)"
              >编辑</el-button
            ><el-button
              v-if="!definition.readOnly && canFire(resource, 'remove')"
              link
              type="danger"
              @click="remove(row)"
              >删除</el-button
            ></template
          ></el-table-column
        > </el-table
      ><el-pagination
        v-model:current-page="query.pageNum"
        v-model:page-size="query.pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @size-change="search"
        @current-change="load"
      />
    </div>
    <el-dialog
      v-model="editOpen"
      :title="`${form.id ? '编辑' : '新增'} · ${definition.title}`"
      class="fire-dialog"
      width="760px"
      append-to-body
      :close-on-click-modal="false"
      ><el-form label-position="top" @submit.prevent="save"
        ><el-button
          v-if="definition.fields.some((f) => f.key === 'lng')"
          style="margin-bottom: 18px"
          @click="pointOpen = true"
          >在地图上选取坐标</el-button
        >
        <div class="fire-form-grid">
          <el-form-item
            v-for="f in definition.fields"
            :key="f.key"
            :label="f.label"
            :class="{ wide: f.type === 'textarea' }"
            :required="
              ['name', 'placeId', 'category', 'gridId'].includes(f.key)
            "
          >
            <el-select
              popper-class="fire-dialog"
              v-if="f.type === 'select'"
              v-model="form[f.key]"
              clearable
              ><el-option v-for="o in f.options" :key="o" :label="o" :value="o"
            /></el-select>
            <el-select
              popper-class="fire-dialog"
              v-else-if="options[f.type]"
              v-model="form[f.key]"
              clearable
              filterable
              @change="f.key === 'placeId' && clearAssociations()"
              ><el-option
                v-for="o in fieldOptions(f)"
                :key="o.id"
                :label="o.name"
                :value="o.id"
            /></el-select>
            <el-switch v-else-if="f.type === 'switch'" v-model="form[f.key]" />
            <el-input-number
              v-else-if="f.type === 'number'"
              v-model="form[f.key]"
              :precision="['lng', 'lat'].includes(f.key) ? 6 : undefined"
              :controls="false"
            />
            <el-date-picker
              popper-class="fire-dialog"
              v-else-if="f.type === 'datetime'"
              v-model="form[f.key]"
              type="datetime"
              value-format="YYYY-MM-DD HH:mm:ss"
            />
            <el-input
              v-else
              v-model="form[f.key]"
              :type="f.type === 'textarea' ? 'textarea' : 'text'"
              :rows="3"
              :maxlength="f.type === 'textarea' ? 2000 : 200"
            />
          </el-form-item></div></el-form
      ><template #footer
        ><el-button @click="editOpen = false">取消</el-button
        ><el-button type="primary" :loading="saving" @click="save"
          >保存</el-button
        ></template
      ></el-dialog
    >
    <PlaceDetail
      v-if="resource === 'places'"
      v-model="detailOpen"
      :place-id="selected?.id"
      @changed="load"
    />
    <VideoDialog
      v-else-if="resource === 'cameras'"
      v-model="detailOpen"
      :camera-id="selected?.id"
      @changed="load"
    />
    <el-dialog
      v-else
      v-model="detailOpen"
      :title="selected?.name || definition.title"
      class="fire-dialog"
      width="780px"
      append-to-body
      ><div v-if="selected" class="fire-detail-grid">
        <div v-for="f in definition.fields" :key="f.key">
          <label>{{ f.label }}</label
          ><span>{{ display(selected, f) }}</span>
        </div>
      </div>
      <img
        v-if="selected?.evidence"
        :src="selected.evidence"
        alt="查岗抓拍证据"
        style="width: 100%; border: 1px solid #25516a"
      />
      <p v-else-if="resource === 'inspections'" class="muted">
        此记录未附抓拍图片
      </p>
      <el-button
        v-if="
          resource === 'devices' &&
          selected?.cameraId &&
          canFire('cameras', 'preview')
        "
        type="primary"
        @click="
          () => {
            linkedCamera = selected.cameraId
            linkedOpen = true
          }
        "
        >查看关联视频</el-button
      >
      <p v-if="resource === 'hosts'" class="muted">
        主机监测数据以上报时间为准，离线时保留最后一次采集值。
      </p></el-dialog
    >
    <el-dialog
      v-model="gridOpen"
      :title="gridRow?.name + ' · 网格边界与辖区'"
      width="1000px"
      class="fire-dialog"
      append-to-body
      ><MapPanel
        v-if="gridOpen"
        :focus-grid="gridRow?.id"
        :editable="canFire('grids', 'edit')"
        @place="showPlace"
      />
      <p class="muted">
        点击网格内场所查看档案；编辑边界后点击保存，取消不会改变已保存边界。
      </p></el-dialog
    >
    <PlaceDetail
      v-if="resource === 'grids'"
      v-model="gridPlaceOpen"
      :place-id="gridPlaceId"
    />
    <VideoDialog v-model="linkedOpen" :camera-id="linkedCamera" />
    <el-dialog
      v-model="pointOpen"
      title="点击地图选取位置"
      width="900px"
      class="fire-dialog"
      append-to-body
      ><MapPanel v-if="pointOpen" pick-point @pick="selectPoint"
    /></el-dialog>
    <ShareCode
      v-model="shareOpen"
      kind="duty"
      :resource-id="shareRow?.placeId"
    />
  </div>
</template>
<script setup>
import { ref, computed, onMounted } from 'vue'
import '@/assets/styles/fire.scss'
import { useRoute } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { fire, markMessageRead } from '@/api/fire'
import { canFire } from '@/api/fire/access'
import { validateFields } from '@/api/fire/rules'
import { catalog, labels } from '@/views/fire/catalog'
import { useList, attempt } from '@/views/fire/useFire'
import StatusTag from './StatusTag.vue'
import PlaceDetail from './PlaceDetail.vue'
import VideoDialog from './VideoDialog.vue'
import MapPanel from './MapPanel.vue'
import ShareCode from './ShareCode.vue'
const props = defineProps({ resource: { type: String, required: true } })
const definition = computed(() => catalog[props.resource]),
  route = useRoute()
const { rows, total, loading, error, query, load, search } = useList(
  props.resource,
  { placeId: route.query.placeId || '', gridId: route.query.gridId || '' },
)
const pointOpen = ref(false)
function selectPoint([lng, lat]) {
  form.value.lng = lng
  form.value.lat = lat
  form.value.crs = 'GCJ02'
  pointOpen.value = false
}
const dates = ref([]),
  options = ref({
    places: [],
    grids: [],
    hosts: [],
    cameras: [],
    personnel: [],
  }),
  form = ref({}),
  editOpen = ref(false),
  saving = ref(false),
  selected = ref(null),
  detailOpen = ref(false),
  gridOpen = ref(false),
  gridRow = ref(null),
  gridPlaceOpen = ref(false),
  gridPlaceId = ref(''),
  linkedOpen = ref(false),
  linkedCamera = ref(''),
  shareOpen = ref(false),
  shareRow = ref(null)
const filters = computed(() =>
  (definition.value.filter || []).map((k) =>
    definition.value.fields.find((f) => f.key === k),
  ),
)
const label = (k) =>
  definition.value.fields.find((f) => f.key === k)?.label || labels[k] || k
async function loadOptions() {
  const needed = [
    ...new Set(
      definition.value.fields
        .map((f) => f.type)
        .filter((t) => options.value[t]),
    ),
  ]
  await Promise.all(
    needed.map(async (r) => {
      if (!canFire(r)) return
      try {
        options.value[r] = (await fire.list(r, { pageSize: 10000 })).rows
      } catch (e) {
        error.value = `关联选项加载失败：${e.message}`
      }
    }),
  )
}
const fieldOptions = (f) =>
  (options.value[f.type] || []).filter(
    (o) =>
      !['hosts', 'cameras', 'personnel'].includes(f.type) ||
      o.placeId === form.value.placeId,
  )
const clearAssociations = () => {
  form.value.hostId = ''
  form.value.cameraId = ''
  form.value.personnelId = ''
}
const display = (row, f) =>
  options.value[f.type]
    ? options.value[f.type].find((o) => o.id === row[f.key])?.name || '—'
    : typeof row[f.key] === 'boolean'
      ? row[f.key]
        ? '是'
        : '否'
      : (row[f.key] ?? '—')
const changeDates = () => {
  query.dateFrom = dates.value?.[0] || ''
  query.dateTo = dates.value?.[1] || ''
  search()
}
function clearQuery() {
  Object.keys(query).forEach((k) => {
    if (!['pageNum', 'pageSize'].includes(k)) query[k] = ''
  })
  dates.value = []
  search()
}
async function edit(row) {
  await attempt(loadOptions)
  form.value = row
    ? JSON.parse(JSON.stringify(row))
    : { ...definition.value.defaults, placeId: query.placeId || undefined }
  editOpen.value = true
}
async function save() {
  saving.value = true
  const result = await attempt(() => {
    validateFields(definition.value.fields, form.value)
    const payload = Object.fromEntries(
      definition.value.fields.map((f) => [f.key, form.value[f.key]]),
    )
    if (definition.value.fields.some((f) => f.key === 'lng'))
      payload.crs = 'GCJ02'
    return form.value.id
      ? fire.update(props.resource, form.value.id, payload)
      : fire.create(props.resource, payload)
  }, '保存成功')
  saving.value = false
  if (result) {
    editOpen.value = false
    load()
  }
}
async function remove(row) {
  await attempt(async () => {
    await ElMessageBox.confirm(
      `确认删除“${row.name}”？关联数据存在时将阻止删除。`,
      '删除确认',
      { type: 'warning' },
    )
    await fire.remove(props.resource, row.id)
    if (rows.value.length === 1 && query.pageNum > 1) query.pageNum--
    await load()
  }, '已删除')
}
async function details(row) {
  if (props.resource === 'cameras' && !canFire('cameras', 'preview')) return
  const res = await attempt(() => fire.get(props.resource, row.id))
  if (res) {
    selected.value = res.data
    detailOpen.value = true
  }
}
async function read(row) {
  if (await attempt(() => markMessageRead(row.id), '已标记为已读')) load()
}
async function callGrid(row) {
  const res = await attempt(() => fire.action('grids', row.id, 'call'))
  if (res)
    await ElMessageBox.alert(res.data.message || '呼叫请求已提交', '视频呼叫')
}
function showPlace(id) {
  gridPlaceId.value = id
  gridPlaceOpen.value = true
}
onMounted(() => {
  load()
  attempt(loadOptions)
})
</script>
