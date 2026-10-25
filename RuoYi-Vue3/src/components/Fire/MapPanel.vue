<template>
  <div class="fire-map" :class="{ compact: focusGrid }">
    <div v-if="error" class="map-error">
      {{ error }}<el-button @click="retryMap">重试</el-button>
    </div>
    <div class="map-search">
      <el-input
        v-model="keyword"
        placeholder="搜索场所 / 建筑 / 消防资源"
        clearable
        ><template #prefix
          ><el-icon><Search /></el-icon></template></el-input
      ><el-button @click="resetMap">复位</el-button>
    </div>
    <div v-if="useRealMap" ref="sdkContainer" class="map-sdk"></div>
    <svg
      v-else
      ref="svg"
      class="map-svg"
      :viewBox="panoramic ? '-400 0 1800 800' : '0 0 1000 800'"
      role="group"
      aria-label="大厂街道示意地图，可选择场所、网格和资源点位"
      @wheel.prevent="wheel"
      @pointerdown="down"
      @pointermove="move"
      @pointerup="up"
      @pointercancel="drag = null"
    >
      <g :transform="`translate(${pan[0]},${pan[1]}) translate(500,400) scale(${zoom}) translate(-500,-400)`">
        <MapBackdrop :show-poi="layers.includes('地图POI')" />
        <path
          v-if="layers.includes('实时路况')"
          d="M-60 180 L1060 590 M100 -50 L720 900"
          fill="none"
          stroke="#46b88e"
          stroke-width="5"
          stroke-dasharray="60 12"
        />
        <g v-if="layers.includes('网格')">
          <g
            v-for="(g, i) in shownGrids"
            :key="g.id"
            role="button"
            tabindex="0"
            :aria-label="g.name"
            @click.stop="selectGrid(g.id)"
            @keydown.enter="selectGrid(g.id)"
          >
            <polygon
              :points="displayBoundary(g)"
              fill="#304d8a"
              :fill-opacity="selectedGrid === g.id ? 0.14 : 0.035"
              :stroke="
                selectedGrid === g.id
                  ? '#f8d87b'
                  : '#d2dcf1'
              "
              stroke-width="2"
            />
            <text
              :x="center(g)[0]"
              :y="center(g)[1] - 70"
              text-anchor="middle"
              fill="#edf3ff"
              font-size="21"
              style="paint-order:stroke;stroke:#081020;stroke-width:4px"
            >
              {{ g.name }}
            </text>
          </g>
        </g>
        <g v-if="layers.includes('地图POI')" fill="#7fa5b3" font-size="13">
          <rect x="370" y="455" width="160" height="28" rx="2" fill="#16465a" />
          <text x="380" y="474">大厂街道办事处</text>
          <rect x="460" y="585" width="190" height="28" rx="2" fill="#16465a" />
          <text x="470" y="604">大厂街道消防工作站</text>
        </g>
        <g
          v-for="p in shownPlaces"
          :key="p.id"
          class="map-point"
          role="button"
          tabindex="0"
          :aria-label="p.name"
          @click.stop="selectPlace(p.id)"
          @keydown.enter="selectPlace(p.id)"
        >
          <circle
            :cx="xy(p)[0]"
            :cy="xy(p)[1]"
            r="14"
            fill="#ee6777"
            opacity=".2"
          />
          <circle
            :cx="xy(p)[0]"
            :cy="xy(p)[1]"
            r="5"
            fill="#f28291"
            stroke="#ffdfdc"
            stroke-width="2"
          />
          <rect
            :x="xy(p)[0] - 53"
            :y="xy(p)[1] + 12"
            width="106"
            height="23"
            rx="2"
            fill="#020711aa"
            stroke="none"
          />
          <text
            :x="xy(p)[0]"
            :y="xy(p)[1] + 28"
            fill="#35c0d7"
            text-anchor="middle"
            font-size="11"
          >
            {{ p.name }}
          </text>
        </g>
        <g v-if="layers.includes('摄像头')">
          <g
            v-for="c in shownCameras"
            :key="c.id"
            role="button"
            tabindex="0"
            :aria-label="c.name"
            @click.stop="emit('camera', c.id)"
            @keydown.enter="emit('camera', c.id)"
          >
            <rect
              :x="xy(c)[0] - 7"
              :y="xy(c)[1] - 22"
              width="14"
              height="12"
              rx="2"
              fill="#28c9ae"
              stroke="#9ef3dc"
            />
            <path
              :d="`M${xy(c)[0] + 7} ${xy(c)[1] - 19} l5 -3 v12 l-5 -3`"
              fill="#28c9ae"
            />
          </g>
        </g>
        <g v-if="layers.includes('告警设备')">
          <g
            v-for="d in shownDevices"
            :key="d.id"
            role="button"
            tabindex="0"
            :aria-label="d.name"
            @click.stop="emit('device', d.id)"
            @keydown.enter="emit('device', d.id)"
          >
            <path
              :d="`M${xy(d)[0] - 17} ${xy(d)[1] - 10} l-7 13 h14 Z`"
              fill="#f0c66a"
            />
            <text
              :x="xy(d)[0] - 17"
              :y="xy(d)[1] + 1"
              text-anchor="middle"
              fill="#263a42"
              font-size="10"
            >
              !
            </text>
          </g>
        </g>
        <g class="fire-alert-layer">
          <g
            v-for="a in visibleAlarmPoints"
            :key="a.id"
            @click.stop="emit('alarm', a.id)"
            role="button"
            tabindex="0"
            @keydown.enter="emit('alarm', a.id)"
            :aria-label="(a.sourceCode === '119' ? '119警情：' : '火警上报：') + a.placeName + ' · ' + a.name"
          >
            <g :transform="`translate(${xy(a)[0]}, ${xy(a)[1] - 24})`" class="fire-alert-marker">
              <circle r="28" fill="#ff453333" stroke="#ff695a" class="fire-pulse" />
              <ellipse cy="23" rx="25" ry="8" fill="#f5454566" stroke="#ff6b4d" />
              <path v-if="a.sourceCode !== '119'" d="M0 -23 C8 -12 15 -10 12 0 C19 -5 22 6 17 15 C10 29 -12 28 -18 14 C-25 0 -8 -6 -9 -17 C-4 -13 -3 -9 -3 -6 C4 -12 3 -18 0 -23Z" fill="#ff603b" stroke="#ffc093" stroke-width="1.5" />
              <path v-if="a.sourceCode !== '119'" d="M0 -2 C6 6 11 10 6 17 C0 24 -9 17 -7 11 C-5 6 -1 5 0 -2Z" fill="#ffe9a4" />
              <text v-if="a.sourceCode === '119'" text-anchor="middle" y="6" fill="#ffffff" font-size="18">119</text>
              <title>{{ a.placeName }} · {{ a.name }} · {{ a.status }}</title>
            </g>
          </g>
        </g>
        <polyline
          v-if="editing"
          :points="points(draft)"
          fill="#1fcae733"
          stroke="#4ee5f5"
          stroke-width="3"
        />
        <circle
          v-for="(p, i) in draft"
          v-show="editing"
          :key="i"
          :cx="coord(p)[0]"
          :cy="coord(p)[1]"
          r="5"
          fill="#6ef1ef"
        />
      </g>
    </svg>
    <button class="map-layer-toggle" :aria-expanded="layersOpen" @click="layersOpen = !layersOpen">▧ 资源图层 <span>{{ layersOpen ? '收起' : '展开' }}</span></button>
    <div v-show="layersOpen" class="map-layer-buttons">
      <button
        v-for="l in layerNames"
        :key="l"
        :class="{ active: layers.includes(l) }"
        :aria-pressed="layers.includes(l)"
        :aria-label="l"
        type="button"
        @click="toggle(l)"
      >
        {{ l }}
      </button>
      <div v-if="useDatabasePlaces" class="map-place-state" role="status" aria-live="polite">
        <span v-if="databaseLoading">正在加载场所…</span>
        <template v-else-if="databaseError">
          <span>{{ databaseError }}</span>
          <el-button v-if="databaseNeedsLogin" link @click="loginForPlaces">登录</el-button>
          <el-button v-else link @click="loadDatabasePlaces">重试</el-button>
        </template>
        <template v-else>
          <span v-if="!layers.some(l => PLACE_LAYERS.some(layer => layer.label === l))">选择类别显示场所</span>
          <span v-else>已显示 {{ shownPlaces.length }} 个场所</span>
          <span v-if="databaseTruncated">点位超过上限，仅显示前 5000 个</span>
        </template>
      </div>
    </div>
    <div class="map-grid-info" v-if="selectedGrid && !editing">
      <strong>{{ grids.find((g) => g.id === selectedGrid)?.name }}</strong
      ><span>{{ shownPlaces.length }} 个场所</span
      ><el-button
        link
        @click="
          () => {
            selectedGrid = ''
            emit('grid', '')
          }
        "
        >取消下钻</el-button
      >
    </div>
    <div class="map-draw" v-if="editable">
      <template v-if="editing"
        ><span>依次点击地图绘制边界（{{ draft.length }}点）</span
        ><el-button size="small" @click="draft.pop()">撤销点</el-button
        ><el-button size="small" type="primary" :loading="saving" @click="save"
          >保存</el-button
        ><el-button
          size="small"
          @click="
            () => {
              editing = false
              draft = []
            }
          "
          >取消</el-button
        ></template
      ><el-button v-else size="small" @click="startEdit"
        >重新绘制网格边界</el-button
      >
    </div>
    <div v-if="!isFireMock" class="map-fire-legend">警情点位 {{ visibleAlarmPoints.length }} <span>点击标记查看详情</span></div>
    <div class="map-caption">
      <span class="online-dot"></span
      >{{
        useRealMap
          ? '高德地图 · 大厂街道资料边界'
          : isFireMock
            ? '示意地图 · 非测绘边界'
            : '待配置GIS · 当前为示意底图'
      }}<span>GCJ-02</span>
    </div>
    <PlaceDetail v-if="selectedMapPlace" v-model="databasePlaceOpen"
      :place-id="selectedMapPlace.id" :map-place="selectedMapPlace" />
    <div class="map-zoom">
      <button aria-label="放大地图" @click="changeZoom(1)">＋</button
      ><button aria-label="缩小地图" @click="changeZoom(-1)">－</button>
    </div>
  </div>
</template>
<script setup>
import '@/api/fire/mapPresentation.css'
import { INCIDENT_LAYERS, filterIncidents, incidentSource } from '@/api/fire/incidentPresentation'
import { ref, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import { ElMessage } from 'element-plus'
import MapBackdrop from './MapBackdrop.vue'
import PlaceDetail from './PlaceDetail.vue'
import { useRouter } from 'vue-router'
import { getToken } from '@/utils/auth'
import { listMapPlaces } from '@/api/fire/places'
import { PLACE_LAYERS, filterMapPlaces, normalizeMapPlace } from '@/api/fire/placeLayers'
import { fireAlarmPoints } from '@/api/fire/rules'
import { mountMap } from '@/api/fire/mapAdapter'
import { fire } from '@/api/fire'
import { canFire } from '@/api/fire/access'
import { isFireMock } from '@/api/fire/config'
import { attempt } from '@/views/fire/useFire'
const props = defineProps({
    panoramic: Boolean,
    alarmRows: { type: Array, default: undefined },
    focusGrid: String,
    editable: Boolean,
    pickPoint: Boolean,
  }),
  emit = defineEmits(['place', 'camera', 'device', 'alarm', 'grid', 'pick'])
const useRealMap = import.meta.env.VITE_FIRE_MAP_PROVIDER === 'amap',
  sdkContainer = ref()
const router = useRouter()
const useDatabasePlaces = useRealMap && !props.editable
const databasePlaces = ref([]), databaseLoading = ref(false), databaseError = ref(''),
  databaseNeedsLogin = ref(false), databaseTruncated = ref(false),
  databasePlaceOpen = ref(false), selectedMapPlace = ref(null)
let databaseLoadVersion = 0
let adapter,
  disposed = false
const svg = ref(),
  places = ref([]),
  grids = ref([]),
  cameras = ref([]),
  devices = ref([]),
  alarms = ref([]),
  keyword = ref(''),
  layersOpen = ref(false),
  error = ref(''),
  selectedGrid = ref(props.focusGrid || ''),
  zoom = ref(1),
  pan = ref([0, 0]),
  editing = ref(false),
  draft = ref([]),
  saving = ref(false)
const layerNames = [
    '重点企业',
    '高层建筑',
    '综合商业体',
    '养老机构',
    '地图POI',
    '网格',
    '摄像头',
    '告警设备',
    '实时路况',
    ...INCIDENT_LAYERS,
    '显示警情名称',
  ],
  layers = ref(layerNames.filter((l) => !['显示警情名称', '地图POI', '实时路况', '摄像头', '告警设备', ...PLACE_LAYERS.map(layer => layer.label)].includes(l)))
const coord = ([lng, lat]) => [
    ((lng - 118.715) / 0.08) * 1000,
    800 - ((lat - 32.2) / 0.06) * 800,
  ],
  xy = (p) => coord([Number(p.lng), Number(p.lat)]),
  points = (ps) =>
    (ps || [])
      .map(coord)
      .map((p) => p.join(','))
      .join(' ')
const displayBoundary = (g) => {
  if (props.editable || g.polygon?.length !== 4 || !/^g[123]$/.test(g.id) || g.polygon[0]?.[1] !== 32.207) return points(g.polygon)
  const boundaries = {
    g1: [[190,650],[205,608],[226,590],[212,555],[245,528],[251,487],[270,475],[255,430],[275,409],[263,370],[298,346],[315,315],[343,327],[365,303],[397,324],[408,356],[436,363],[425,399],[449,420],[437,458],[457,484],[439,516],[460,542],[434,571],[413,581],[396,615],[370,625],[355,651],[328,642],[312,671],[285,659],[260,680],[237,663],[214,671]],
    g2: [[343,327],[359,279],[388,270],[404,242],[439,250],[455,220],[483,232],[509,212],[535,231],[562,220],[584,247],[611,239],[635,266],[622,294],[650,317],[635,344],[662,370],[650,403],[672,425],[651,450],[671,480],[646,506],[617,493],[594,512],[570,493],[543,517],[515,498],[490,526],[460,542],[439,516],[457,484],[437,458],[449,420],[425,399],[436,363],[408,356],[397,324],[365,303]],
    g3: [[662,370],[688,348],[713,358],[740,337],[765,352],[791,339],[819,363],[842,357],[863,391],[849,416],[872,439],[857,473],[878,500],[859,526],[870,559],[846,579],[835,612],[808,607],[786,636],[759,628],[734,654],[710,640],[684,665],[658,644],[631,651],[612,620],[583,611],[574,582],[549,572],[543,542],[515,530],[543,517],[570,493],[594,512],[617,493],[646,506],[671,480],[651,450],[672,425],[650,403]]
  }
  return boundaries[g.id].map(p => p.join(',')).join(' ')

}
const center = (g) => {
  const p = g.polygon || []
  return p.length
    ? coord([
        p.reduce((s, v) => s + v[0], 0) / p.length,
        p.reduce((s, v) => s + v[1], 0) / p.length,
      ])
    : [0, 0]
}
const shownGrids = computed(() =>
  grids.value.filter((g) => !props.focusGrid || g.id === props.focusGrid),
)
const shownPlaces = computed(() => filterMapPlaces(
  useDatabasePlaces ? databasePlaces.value : places.value,
  layers.value, keyword.value, selectedGrid.value,
))
const shownCameras = computed(() =>
  cameras.value.filter((c) =>
    shownPlaces.value.some((p) => p.id === c.placeId),
  ),
)
const shownDevices = computed(() =>
  devices.value.filter((c) =>
    shownPlaces.value.some((p) => p.id === c.placeId),
  ),
)
// Active fire markers are never hidden by ordinary resource/category filters.
const alarmPoints = computed(() => fireAlarmPoints(props.alarmRows === undefined ? alarms.value : props.alarmRows, useDatabasePlaces ? databasePlaces.value : places.value, devices.value))
const visibleAlarmPoints = computed(() => filterIncidents(alarmPoints.value, layers.value))
const toggle = (l) => {
  layers.value = layers.value.includes(l)
    ? layers.value.filter((x) => x !== l)
    : [...layers.value, l]
}
function selectGrid(id) {
  if (editing.value) return
  selectedGrid.value = id
  emit('grid', id)
}
function loginForPlaces() {
  router.push({ path: '/login', query: { redirect: router.currentRoute.value.fullPath } })
}
function selectPlace(id) {
  const row = useDatabasePlaces && databasePlaces.value.find(place => place.id === String(id))
  if (row) {
    selectedMapPlace.value = row
    databasePlaceOpen.value = true
  } else emit('place', id)
}
async function loadDatabasePlaces() {
  const version = ++databaseLoadVersion
  databaseError.value = ''
  databaseNeedsLogin.value = !getToken()
  if (databaseNeedsLogin.value) {
    databasePlaces.value = []
    databaseLoading.value = false
    databaseError.value = '请登录后加载场所'
    return
  }
  databaseLoading.value = true
  try {
    const response = await listMapPlaces()
    if (disposed || version !== databaseLoadVersion) return
    if (!Array.isArray(response.data)) throw Error('场所数据格式异常')
    databasePlaces.value = response.data.map(normalizeMapPlace).filter(Boolean)
    databaseTruncated.value = Boolean(response.truncated)
  } catch (e) {
    if (disposed || version !== databaseLoadVersion) return
    databasePlaces.value = []
    databaseError.value = e?.message || '场所加载失败，请重试'
  } finally {
    if (!disposed && version === databaseLoadVersion) databaseLoading.value = false
  }
}
async function load() {
  error.value = ''
  const databaseRequest = useDatabasePlaces ? loadDatabasePlaces() : Promise.resolve()
  // Keep demo business records off display maps; real API data remains enabled.
  if (isFireMock && !props.editable) {
    places.value = []
    grids.value = []
    cameras.value = []
    devices.value = []
    alarms.value = []
    try {
      if (props.alarmRows === undefined && canFire('alarms')) alarms.value = (await fire.list('alarms', { pageSize: 1000 })).rows
    } catch(e) { error.value = e.message || '真实设备事件加载失败' }
    await databaseRequest
    return
  }
  try {
    const [p, g, c, d, a] = await Promise.all(
      ['places', 'grids', 'cameras', 'devices', 'alarms'].map((r) =>
        canFire(r) && !(r === 'alarms' && props.alarmRows !== undefined) && !(useDatabasePlaces && r === 'places')
          ? fire.list(r, { pageSize: 10000 })
          : Promise.resolve({ rows: [] }),
      ),
    )
    places.value = p.rows
    grids.value = g.rows
    cameras.value = c.rows
    devices.value = d.rows
    alarms.value = a.rows
  } catch (e) {
    error.value = e.message
  } finally {
    await databaseRequest
  }
}
let drag = null
function local(e) {
  const p = svg.value.createSVGPoint()
  p.x = e.clientX
  p.y = e.clientY
  return p.matrixTransform(svg.value.getScreenCTM().inverse())
}
function down(e) {
  if (e.button !== 0) return
  const p = local(e)
  drag = { x: p.x, y: p.y, pan: [...pan.value], moved: false }
  if (e.target === svg.value || e.target.tagName === 'rect')
    svg.value.setPointerCapture(e.pointerId)
}
function move(e) {
  if (!drag || editing.value || props.pickPoint) return
  const p = local(e)
  if (Math.abs(p.x - drag.x) + Math.abs(p.y - drag.y) > 4) {
    drag.moved = true
    pan.value = [drag.pan[0] + p.x - drag.x, drag.pan[1] + p.y - drag.y]
  }
}
function up(e) {
  if ((editing.value || props.pickPoint) && drag) {
    const p = local(e),
      x = (p.x - pan.value[0] - 500) / zoom.value + 500,
      y = (p.y - pan.value[1] - 400) / zoom.value + 400
    const coordinate = [
      +(118.715 + (x / 1000) * 0.08).toFixed(6),
      +(32.2 + ((800 - y) / 800) * 0.06).toFixed(6),
    ]
    if (props.pickPoint) emit('pick', coordinate)
    else draft.value.push(coordinate)
  }
  drag = null
}
function wheel(e) {
  zoom.value = Math.min(
    2.5,
    Math.max(0.6, zoom.value + (e.deltaY < 0 ? 0.1 : -0.1)),
  )
}
function startEdit() {
  if (!selectedGrid.value) {
    ElMessage.info('请先选择需要编辑的网格')
    return
  }
  draft.value = []
  editing.value = true
}
async function save() {
  saving.value = true
  if (
    await attempt(
      () => fire.update('grids', selectedGrid.value, { polygon: draft.value }),
      '网格边界已保存',
    )
  ) {
    editing.value = false
    draft.value = []
    load()
  }
  saving.value = false
}
function resetMap() {
  zoom.value = 1
  pan.value = [0, 0]
  adapter?.reset()
}
function changeZoom(direction) {
  if (adapter) adapter.zoom(direction)
  else zoom.value = Math.min(2.5, Math.max(0.6, zoom.value + direction * 0.2))
}
function renderMap() {
  adapter?.setFeatures({
    places: shownPlaces.value,
    // The initial demo rectangles are not surveyed administrative boundaries.
    // Retain user-drawn geometry and the grid editor, but never overlay seed boxes on the real map.
    grids: shownGrids.value.filter(g => props.editable || !isFireMock || !(
      /^g[123]$/.test(g.id) && g.polygon?.length === 4 &&
      g.polygon[0]?.[1] === 32.207 && g.polygon[1]?.[1] === 32.211 &&
      g.polygon[2]?.[1] === 32.248 && g.polygon[3]?.[1] === 32.244
    )),
    cameras: shownCameras.value,
    devices: shownDevices.value,
    alarms: visibleAlarmPoints.value,
    showAlarmNames: layers.value.includes('显示警情名称'),
    layers: layers.value,
  })
}
watch(
  [shownPlaces, shownGrids, shownCameras, shownDevices, visibleAlarmPoints, layers],
  renderMap,
  { deep: true },
)
watch(draft, (v) => adapter?.setDraft(editing.value ? v : []), { deep: true })
watch(
  () => props.focusGrid,
  (v) => {
    selectedGrid.value = v || ''
  },
)
async function retryMap() {
  await load()
  if (useRealMap && !adapter) {
    try {
      const instance = await mountMap(sdkContainer.value, {
        showBoundary: !props.editable && !props.pickPoint,
        onSelect: (type, id) =>
          type === 'grid' ? selectGrid(id) : type === 'place' ? selectPlace(id) : emit(type, id),
        onPoint: (point) => {
          if (props.pickPoint) emit('pick', point)
          else if (editing.value) draft.value.push(point)
        },
      })
      if (disposed) {
        instance.destroy()
        return
      }
      adapter = instance
      renderMap()
    } catch (e) {
      error.value = e.message
    }
  }
}
onMounted(retryMap)
onBeforeUnmount(() => {
  disposed = true
  databaseLoadVersion++
  adapter?.destroy()
})
function focusAlarm(id) {
  const point = alarmPoints.value.find(a => a.id === id)
  if (!point) return
  const source = incidentSource(point)
  if (!layers.value.includes(source)) layers.value = [...layers.value, source]
  if (adapter) adapter.focus([point.lng, point.lat])
  else {
    const [x,y] = xy(point)
    zoom.value = 1.15
    pan.value = [(500-x)*zoom.value, (400-y)*zoom.value]
  }
}
defineExpose({ refresh: load, focusAlarm })
</script>
<style scoped>
.fire-map {
  height: 100%;
  min-height: 560px;
  position: relative;
  background: #020711;
  overflow: hidden;
  border: 1px solid #225367;
}
.fire-map.compact {
  height: 520px;
  min-height: 400px;
}
.map-sdk {
  width: 100%;
  height: 100%;
  min-height: 500px;
}
.map-svg {
  width: 100%;
  height: 100%;
  display: block;
  touch-action: none;
  cursor: grab;
}
.map-svg:active {
  cursor: grabbing;
}
.map-point {
  cursor: pointer;
}
.map-point:hover circle {
  fill: #f9ce76;
}
.map-search {
  position: absolute;
  z-index: 2;
  top: 14px;
  left: 14px;
  display: flex;
  gap: 8px;
  width: min(420px, 70%);
}
.map-layer-buttons {
  position: absolute;
  left: 14px;
  right: auto;
  bottom: 95px;
  display: flex;
  flex-direction: column;
  gap: 5px;
}
.map-layer-buttons button,
.map-zoom button {
  background: #082539e8;
  border: 1px solid #31546a;
  color: #91b1c2;
  padding: 8px 12px;
  cursor: pointer;
  border-radius: 3px;
  font-size: 12px;
}
.map-layer-buttons button.active {
  background: #164b5f;
  color: #a9e7ee;
  border-color: #337f94;
}
.map-caption {
  position: absolute;
  bottom: 12px;
  left: 12px;
  background: #082236d9;
  padding: 7px;
  color: #7fa9b6;
  font-size: 10px;
}
.map-caption > span:last-child {
  margin-left: 12px;
}
.map-zoom {
  position: absolute;
  bottom: 14px;
  right: 14px;
  display: flex;
  gap: 4px;
}
.map-grid-info {
  position: absolute;
  top: 65px;
  left: 15px;
  background: #0b2d43e8;
  padding: 10px 15px;
  display: flex;
  align-items: center;
  gap: 12px;
  border: 1px solid #236179;
  font-size: 12px;
}
.map-grid-info span {
  color: #69c8d0;
}
.map-draw {
  position: absolute;
  bottom: 55px;
  left: 12px;
  background: #0c2b43;
  padding: 8px;
  font-size: 12px;
  max-width: 80%;
}
.map-draw span {
  display: block;
  margin-bottom: 6px;
}
.map-error {
  position: absolute;
  top: 110px;
  left: 10%;
  z-index: 5;
  background: #422738;
  padding: 20px;
}
@media (max-width: 1100px) {
  .map-layer-buttons {
    left: 12px;
    right: 12px;
    bottom: 50px;
    flex-direction: row;
    overflow: auto;
    max-width: calc(100% - 24px);
  }
  .map-layer-buttons button {
    flex-shrink: 0;
  }
  .map-draw {
    bottom: 95px;
  }
}
.fire-alert-marker { cursor:pointer; filter:drop-shadow(0 0 8px #ff503a); }
.fire-pulse { animation:fire-ring 1.8s ease-out infinite; transform-box:fill-box; transform-origin:center; }
@keyframes fire-ring { from { opacity:.9; transform:scale(.75) } to { opacity:0; transform:scale(1.5) } }
.map-fire-legend { position:absolute; left:14px; bottom:48px; padding:7px 10px; background:#111e3ddd; border:1px solid #975046; color:#ffc4ad; font-size:12px; pointer-events:none; }
.map-fire-legend span { color:#8ca6c6; font-size:10px; margin-left:10px; }
@media(max-width:1100px) { .map-fire-legend { bottom:95px; } }
@media(prefers-reduced-motion:reduce) { .fire-pulse { animation:none; } }
.map-layer-toggle { position:absolute; left:14px; top:65px; z-index:3; padding:10px 14px; color:#d1e7f3; background:#102348ee; border:1px solid #35618a; cursor:pointer; border-radius:4px; font-size:14px; }
.map-layer-toggle span { color:#7da8bc; font-size:12px; margin-left:14px; }
.map-layer-buttons { top:110px; bottom:auto; z-index:4; background:#0a1733f5; padding:10px; border:1px solid #315c84; border-radius:4px; display:grid; grid-template-columns:repeat(2,1fr); max-width:260px; max-height:calc(100% - 180px); overflow-y:auto; }
.map-point circle { fill:#49b7d8; stroke:#b1e7ef; }
@media(max-width:1100px) { .map-layer-buttons { left:14px; right:auto; bottom:auto; top:110px; } }
.map-place-state { grid-column:1 / -1; min-height:28px; display:flex; flex-wrap:wrap; align-items:center; gap:6px; color:#b8d7e8; font-size:12px; line-height:1.5; }
.map-place-state .el-button { color:#75d9f0; }
.map-layer-buttons > button[aria-pressed="true"]::before { content:'✓ '; }
.map-layer-buttons > button:focus-visible, .map-layer-toggle:focus-visible, .map-zoom button:focus-visible { outline:2px solid #a9e7ee; outline-offset:2px; }
</style>
