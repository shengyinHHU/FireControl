<template>
  <div class="command-screen" ref="screen">
    <header class="command-header">
      <div class="header-left">
        <span class="shield">消</span
        ><span>南京江北新区 <small>DA CHANG STREET</small></span>
      </div>
      <div class="command-title">
        <h1>大厂街道智慧消防监管平台</h1>
        <p>全域感知 · 智慧监管 · 协同处置</p>
      </div>
      <div class="header-right">
        <span>{{ clock }}</span>
        <div>
          <button @click="toggleSound">
            {{ sound ? '声音已开启' : '开启提醒音' }}</button
          ><button @click="fullscreen">全屏</button
          ><router-link to="/fire/places">管理工作台 ↗</router-link>
        </div>
      </div>
    </header>
    <div class="command-subbar">
      <span
        ><i class="online-dot"></i
        >{{ isFireMock ? '真实场所、主机及事件 · 其他业务为演示数据' : '街道消防安全运行态势' }}</span
      >
      <div>
        <span
          >覆盖场所 <b>{{ stats.total || 0 }}</b></span
        ><span
          >接入视频 <b>{{ stats.cameras || 0 }}</b></span
        ><span
          >未读消息 <b>{{ stats.unread || 0 }}</b></span
        >
      </div>
      <span>数据更新 {{ updated || '—' }}</span>
    </div>
    <div v-if="error" class="fire-error">
      {{ error }}<el-button link @click="refresh">重试</el-button>
    </div>
    <div class="command-grid">
      <div class="command-column left-column">
        <Panel title="数据概览"
          ><div class="overview-grid">
            <button
              v-for="(s, i) in stats.categories || []"
              :key="s.name"
              :class="{ active: category === s.name }"
              @click="category = category === s.name ? '' : s.name"
            >
              <span class="stat-icon"
                ><el-icon
                  ><component
                    :is="
                      ['OfficeBuilding', 'School', 'Shop', 'FirstAidKit'][i]
                    " /></el-icon></span
              ><strong>{{ s.count }}</strong
              ><span>{{ s.name }}</span>
            </button>
          </div></Panel
        >
        <Panel title="重点场所" class="places-panel"
          ><template #action
            ><span class="tiny">{{ filteredPlaces.length }} 处</span></template
          ><el-input
            v-model="search"
            placeholder="搜索场所名称 / 地址"
            size="small"
            clearable
            ><template #prefix
              ><el-icon><Search /></el-icon></template
          ></el-input>
          <div class="screen-scroll place-rows">
            <button
              v-for="p in filteredPlaces"
              :key="p.id"
              @click="showPlace(p.id)"
            >
              <span
                >{{ p.name }}
                <small v-if="Array.isArray(p.onDuty)">（{{ p.onDuty.length }}人）</small></span
              ><b>详情 ›</b>
            </button>
            <p v-if="!filteredPlaces.length" class="fire-empty">暂无匹配场所</p>
          </div></Panel
        >
        <Panel title="查岗管理" class="inspections-panel"
          ><template #action
            ><router-link to="/fire/inspections" class="tiny"
              >全部记录 ›</router-link
            ></template
          >
          <div class="inspection-filter">
            <el-select
              popper-class="fire-dialog"
              v-model="inspector"
              size="small"
              clearable
              placeholder="全部查岗人员"
              ><el-option
                v-for="n in [...new Set(inspections.map((i) => i.name))]"
                :key="n"
                :value="n" /></el-select
            ><span>{{ clock.slice(0, 10) }}</span>
          </div>
          <div class="screen-scroll">
            <button
              class="inspection-card"
              v-for="r in inspections.filter(
                (i) => !inspector || i.name === inspector,
              )"
              :key="r.id"
              @click="showCamera(r.cameraId)"
            >
              <div>
                <p>
                  查岗人员 <strong>{{ r.name }}</strong>
                </p>
                <p>查岗时间 {{ r.time }}</p>
                <p>查岗单位 {{ r.placeName }}</p>
                <p>查岗结果 <StatusTag :value="r.result" /></p>
              </div>
              <div class="mini-camera">
                <span>● {{ r.evidence ? '抓拍' : 'DEMO' }}</span
                ><el-icon><VideoCamera /></el-icon><small>视频查岗</small>
              </div>
            </button>
            <p v-if="!inspections.length" class="fire-empty">暂无查岗记录</p>
          </div></Panel
        >
      </div>
      <div class="command-map">
        <MapPanel
          ref="map"
          :panoramic="wideMap"
          @place="showPlace"
          @camera="showCamera"
          @alarm="showAlarm"
          @device="showDevice"
          @grid="grid = $event"
        />
        <button class="video-tools-toggle" :aria-expanded="videoToolsOpen" @click="videoToolsOpen = !videoToolsOpen">▣ 视频监控 <b v-if="!isFireMock">{{ stats.cameras || 0 }}</b> {{ videoToolsOpen ? '⌃' : '⌄' }}</button>
        <div v-show="videoToolsOpen" class="map-video-types">
          <button
            v-for="type in ['街道高点', '城中村', '无人机', '布控球']"
            :key="type"
            @click="
              () => {
                videoType = type
                videoListOpen = true
              }
            "
          >
            {{ type }}{{ type === '无人机' ? '' : '监控' }}</button
          ><button
            class="video-count"
            @click="
              () => {
                videoType = ''
                videoListOpen = true
              }
            "
          >
            <el-icon><VideoCamera /></el-icon> 视频监控
            <b v-if="!isFireMock">{{ stats.cameras || 0 }}</b>
          </button>
        </div>
        <div v-if="isFireMock" class="map-street-label">
          大厂街道 <small>消防安全网格化监管</small>
        </div>
      </div>
      <div class="command-column right-column">
        <Panel title="消防警情" class="alarm-panel"
          ><template #action
            ><router-link to="/fire/alarms" class="tiny"
              >处置中心 ›</router-link
            ></template
          >
          <div class="fire-priority-note">🔥 待核验火警上报 <b>{{ activeFires.length }}</b> <span>请立即核查处置</span></div>
          <div v-show="!selectedAlarm" class="source-tabs">
            <button
              v-for="(s, i) in sources"
              :key="s"
              :class="{ active: source === s }"
              @click="source = s"
            >
              <span :class="'source-icon source-' + i"
                ><el-icon
                  ><component
                    :is="
                      ['Bell', 'Van', 'Cpu', 'Warning'][i]
                    " /></el-icon></span
              ><span>{{ s }}</span
              ><b>{{
                s === '全部'
                  ? activeFires.length
                  : activeFires.filter((a) => a.source === s).length
              }}</b>
            </button>
          </div>
          <div v-if="selectedAlarm" class="selected-fire">
            <div class="selected-fire-head"><strong>火警上报（待核验）</strong><button @click="alarmId = ''">返回列表 ›</button></div>
            <h3>{{ selectedAlarm.placeName }}</h3><p>{{ selectedAlarm.name }}</p>
            <dl><dt>发生时间</dt><dd>{{ selectedAlarm.occurredAt }}</dd><dt>接入来源</dt><dd>{{ selectedAlarm.source }}</dd><dt>处置状态</dt><dd><StatusTag :value="selectedAlarm.status" /></dd></dl>
            <div class="selected-fire-actions"><el-button type="danger" @click="alarmOpen = true">查看事件</el-button><el-button @click="showPlace(selectedAlarm.placeId)">场所档案</el-button></div>
          </div>
          <div v-show="!selectedAlarm" class="screen-scroll alarm-list">
            <button
              v-for="a in filteredAlarms"
              :key="a.id"
              class="alarm-card"
              @click="showAlarm(a.id)"
            >
              <span
                class="alarm-symbol"
                :class="
                  a.source === 'AI监测预警'
                    ? 'ai'
                    : a.source === '119警情'
                      ? 'urgent'
                      : 'host'
                "
                ><el-icon
                  ><component
                    :is="
                      a.source === '119警情'
                        ? 'Van'
                        : a.source === 'AI监测预警'
                          ? 'Warning'
                          : 'Cpu'
                    " /></el-icon
              ></span>
              <div class="alarm-card-body">
                <div class="alarm-code">
                  {{ a.code }} <span>{{ a.source }}</span>
                </div>
                <p>{{ a.name }}</p>
                <div class="alarm-footer">
                  <small>{{ a.occurredAt }} · {{ a.placeName }}</small
                  ><StatusTag :value="a.status" />
                </div>
              </div>
            </button>
            <p v-if="!filteredAlarms.length" class="fire-empty">暂无待核验火警上报</p>
          </div></Panel
        >
        <Panel title="消控主机" class="hosts-panel"
          ><template #action
            ><router-link to="/fire/hosts" class="tiny"
              >设备台账 ›</router-link
            ></template
          >
          <div class="host-stats">
            <router-link v-for="s in hostStats" :key="s.name" :to="s.to"
              ><strong :style="{ color: s.color }">{{ s.value }}</strong
              ><span>{{ s.name }}</span></router-link
            >
          </div>
          <div class="history-head">
            <strong>设备上报（近24h）</strong
            ><el-input
              v-model="hostSearch"
              size="small"
              placeholder="单位 / 设备 / 预警"
              clearable
            />
          </div>
          <div class="screen-scroll history-list">
            <button v-for="a in history" :key="a.id" @click="showAlarm(a.id)">
              <div>
                <span>{{ a.occurredAt }}</span
                ><StatusTag :value="a.status" />
              </div>
              <strong>{{ a.placeName }}</strong>
              <p>{{ a.name }}</p>
              <small>设备：{{ a.deviceId }} · {{ a.source }}</small>
            </button>
            <p v-if="!history.length" class="fire-empty">近24小时无匹配设备事件</p>
          </div></Panel
        >
      </div>
    </div>
    <footer class="command-footer">
      <span>街道消防安全监管中心</span
      ><span>网格联动 · 资源共享 · 闭环管理</span
      ><span>{{
        isFireMock ? '真实场所 / 其他业务演示' : 'FIRE SAFETY OPERATIONS'
      }}</span>
    </footer>
    <PlaceDetail
      v-model="placeOpen"
      :place-id="placeId"
      @changed="refresh"
    /><VideoDialog
      v-model="cameraOpen"
      :camera-id="cameraId"
      @changed="refresh"
    /><AlarmDetail v-model="alarmOpen" :alarm-id="alarmId" @changed="refresh" />
    <el-dialog
      v-model="videoListOpen"
      :title="videoType ? videoType + '监控' : '视频监控资源'"
      width="800px"
      class="fire-dialog"
      append-to-body
      ><el-table
        :data="cameras.filter((c) => !videoType || c.category === videoType)"
        ><el-table-column
          prop="name"
          label="摄像头"
          min-width="240"
        /><el-table-column prop="category" label="分类" /><el-table-column
          label="状态"
          ><template #default="{ row }"
            ><StatusTag :value="row.status" /></template></el-table-column
        ><el-table-column label="操作"
          ><template #default="{ row }"
            ><el-button link type="primary" @click="showCamera(row.id)"
              >预览</el-button
            ></template
          ></el-table-column
        ></el-table
      ></el-dialog
    >
  </div>
</template>
<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { ElMessage } from 'element-plus'
import { fire } from '@/api/fire'
import { PLACE_LAYERS } from '@/api/fire/placeLayers'
import { canFire } from '@/api/fire/access'
import { hostAlarmSummary, isActiveFire } from '@/api/fire/rules'
import { isFireMock } from '@/api/fire/config'
import { attempt } from '../useFire'
import Panel from '@/components/Fire/Panel.vue'
import StatusTag from '@/components/Fire/StatusTag.vue'
import MapPanel from '@/components/Fire/MapPanel.vue'
import PlaceDetail from '@/components/Fire/PlaceDetail.vue'
import VideoDialog from '@/components/Fire/VideoDialog.vue'
import AlarmDetail from '@/components/Fire/AlarmDetail.vue'
const wideMap = ref(window.innerWidth > 1100)
const videoToolsOpen = ref(false)
const resizeMap = () => { wideMap.value = window.innerWidth > 1100 }
const screen = ref(),
  map = ref(),
  stats = ref({}),
  places = ref([]),
  alarms = ref([]),
  inspections = ref([]),
  cameras = ref([]),
  search = ref(''),
  category = ref(''),
  source = ref('全部'),
  sources = ['全部', '119警情', '消控室主机', 'AI监测预警'],
  inspector = ref(''),
  hostSearch = ref(''),
  grid = ref(''),
  clock = ref(''),
  updated = ref(''),
  error = ref(''),
  sound = ref(false),
  videoType = ref(''),
  videoListOpen = ref(false)
const placeOpen = ref(false),
  placeId = ref(''),
  cameraOpen = ref(false),
  cameraId = ref(''),
  alarmOpen = ref(false),
  alarmId = ref('')
const selectedAlarm = computed(() => alarms.value.find(a => a.id === alarmId.value && isActiveFire(a)))
const filteredPlaces = computed(() =>
  places.value.filter(
    (p) =>
      (!category.value || p.category === category.value) &&
      (!grid.value || p.gridId === grid.value) &&
      (!search.value ||
        `${p.name || ''}${p.address || ''}`.includes(
          search.value,
        )),
  ),
)
const activeFires = computed(() => alarms.value.filter(isActiveFire))
const filteredAlarms = computed(() =>
  activeFires.value.filter(
    (a) => source.value === '全部' || a.source === source.value,
  ),
)
const hostAlarmData = computed(() => hostAlarmSummary(alarms.value))
const history = computed(() =>
  hostAlarmData.value.rows.filter(
    (a) =>
      Date.now() -
        new Date(a.occurredAt.replace(' ', 'T') + '+08:00').getTime() <=
        86400000 &&
      `${a.name}${a.placeName}${a.deviceId}`.includes(hostSearch.value),
  ),
)
const hostStats = computed(() => [
  { name: '装置总量', value: stats.value.gateways ?? '—', color: '#4ed9ee', to: '/fire/hosts' },
  { name: '主机总量', value: stats.value.hosts ?? '—', color: '#4ed9ee', to: '/fire/hosts?tab=hosts' },
  { name: '在线主机', value: stats.value.online ?? '—', color: '#45d6aa', to: '/fire/hosts?tab=hosts' },
  { name: '事件（24h）', value: stats.value.eventTotal ?? '—', color: '#edbd5f', to: '/fire/alarms' },
  { name: '火警（24h）', value: stats.value.fireEvents ?? '—', color: '#ed7e93', to: '/fire/alarms?kind=fire' },
  { name: '故障（24h）', value: stats.value.faultEvents ?? '—', color: '#edbd5f', to: '/fire/alarms?kind=fault' },
])

let polling,
  ticking,
  audioContext,
  seen = new Set(),
  initialized = false,
  busy = false,
  disposed = false
async function refresh() {
  if (busy) return
  busy = true
  try {
    const [s, p, a, i, c] = await Promise.all([
      fire.dashboard(),
      ...['places', 'alarms', 'inspections', 'cameras'].map((r) =>
        r === 'alarms' && !canFire('alarms') ? Promise.resolve({rows: [], total: 0}) : fire.list(r, { pageSize: 1000 }),
      ),
    ])
    if (disposed) return
    // Authoritative database places replace the demo place list and its category counts.
    stats.value = { ...s.data, total: p.total,
      categories: PLACE_LAYERS.map(layer => ({ name: layer.label, count: p.rows.filter(row => row.placeType === layer.type).length })),
    }
    places.value = p.rows
    alarms.value = a.rows
    if (a.total > a.rows.length) error.value = '设备事件仅展示最近1000条，完整记录请打开设备事件页面'
    inspections.value = i.rows
    cameras.value = c.rows
    const fresh = a.rows.filter((x) => isActiveFire(x) && !seen.has(x.id))
    if (initialized && fresh.length) {
      ElMessage.warning(`新增 ${fresh.length} 条消防警情，请及时核查`)
      if (sound.value) beep()
    }
    seen = new Set(a.rows.filter(isActiveFire).map((x) => x.id))
    initialized = true
    updated.value = new Date().toLocaleTimeString('zh-CN')
    if (a.total <= a.rows.length) error.value = ''
    map.value?.refresh()
  } catch (e) {
    if (!disposed) error.value = e.message
  } finally {
    busy = false
  }
}
function beep() {
  if (!audioContext) return
  const o = audioContext.createOscillator(),
    g = audioContext.createGain()
  o.connect(g)
  g.connect(audioContext.destination)
  o.frequency.value = 660
  g.gain.value = 0.08
  o.start()
  o.stop(audioContext.currentTime + 0.4)
}
async function toggleSound() {
  if (!sound.value) {
    try {
      audioContext ||= new (window.AudioContext || window.webkitAudioContext)()
      await audioContext.resume()
      sound.value = true
      beep()
    } catch {
      ElMessage.warning('浏览器无法开启提示音')
    }
  } else sound.value = false
}
function showPlace(id) {
  placeId.value = id
  placeOpen.value = true
}
function showCamera(id) {
  if (!canFire('cameras', 'preview')) {
    ElMessage.warning('无视频预览权限')
    return
  }
  cameraId.value = id
  cameraOpen.value = true
}
function showAlarm(id) {
  alarmId.value = id
  if (selectedAlarm.value) map.value?.focusAlarm(id)
  else alarmOpen.value = true
}
async function showDevice(id) {
  const r = await attempt(() => fire.get('devices', id))
  if (r?.data.cameraId) showCamera(r.data.cameraId)
  else ElMessage.info('该设备尚未绑定摄像头')
}
async function fullscreen() {
  try {
    if (document.fullscreenElement) await document.exitFullscreen()
    // Include dialogs and messages teleported to body in the fullscreen subtree.
    else await document.documentElement.requestFullscreen()
  } catch {
    ElMessage.info('浏览器未允许全屏，请使用 F11')
  }
}
const tick = () =>
  (clock.value = new Date().toLocaleString('zh-CN', {
    timeZone: 'Asia/Shanghai',
    hour12: false,
  }))
onMounted(() => {
  window.addEventListener('resize', resizeMap)
  tick()
  refresh()
  ticking = setInterval(tick, 1000)
  polling = setInterval(refresh, 30000)
})
onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeMap)
  disposed = true
  clearInterval(ticking)
  clearInterval(polling)
  audioContext?.close()
})
</script>
<style scoped>
.command-screen {
  height: 100vh;
  min-height: 700px;
  display: flex;
  flex-direction: column;
  background: radial-gradient(ellipse at 50% 0, #103b56 0, #04152a 60%);
  padding: 0 18px;
  color: #c7dfed;
  overflow: hidden;
}
.command-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  min-height: 86px;
  position: relative;
  border-bottom: 1px solid #25516a;
}
.command-header:after {
  content: '';
  position: absolute;
  bottom: -1px;
  left: 32%;
  width: 36%;
  height: 2px;
  background: #37cde1;
  box-shadow: 0 0 12px #32bbda;
}
.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 24%;
  font-size: 14px;
  letter-spacing: 2px;
}
.header-left small {
  display: block;
  font-size: 9px;
  color: #648da6;
  letter-spacing: 2px;
  margin-top: 8px;
}
.shield {
  border: 1px solid #2d9bb7;
  color: #4ed4e7;
  padding: 9px 10px;
  font-size: 20px;
  clip-path: polygon(0 0, 100% 0, 100% 80%, 50% 100%, 0 80%);
  background: #104256;
}
.command-title {
  text-align: center;
  flex: 1;
}
.command-title h1 {
  font-size: clamp(22px, 1.8vw, 34px);
  letter-spacing: 4px;
  margin: 0;
  color: #dbf7ff;
  text-shadow: 0 0 20px #3991b066;
  font-weight: 600;
}
.command-title p {
  font-size: 10px;
  letter-spacing: 7px;
  color: #77acc3;
  margin: 8px 0 0;
}
.header-right {
  width: 24%;
  text-align: right;
  font-family: Consolas, 'Microsoft YaHei', sans-serif;
  font-size: 14px;
  color: #aed2e3;
}
.header-right > div {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 10px;
  font-size: 11px;
}
.header-right button {
  border: 0;
  background: none;
  color: #7ba6ba;
  cursor: pointer;
  padding: 0;
}
.header-right a {
  color: #47c7de;
}
.command-subbar {
  height: 34px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 10px;
  color: #779db5;
  letter-spacing: 0.5px;
}
.command-subbar > div {
  display: flex;
  gap: 28px;
}
.command-subbar b {
  color: #5cd5df;
  margin-left: 7px;
}
.command-grid {
  display: grid;
  grid-template-columns: minmax(255px, 22%) minmax(400px, 1fr) minmax(
      300px,
      24%
    );
  gap: 12px;
  flex: 1;
  min-height: 0;
}
.command-column {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-height: 0;
}
.left-column > .fire-panel:first-child {
  flex: 0 0 26%;
}
.places-panel {
  flex: 1;
  min-height: 0;
}
.inspections-panel {
  flex: 0 0 33%;
  min-height: 0;
}
.command-column :deep(.panel-body) {
  height: calc(100% - 44px);
  display: flex;
  flex-direction: column;
}
.overview-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 7px;
  height: 100%;
}
.overview-grid button {
  border: 1px solid #1a3f59;
  background: #0b2c4980;
  color: #8fb9d0;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  font-size: 11px;
  border-radius: 3px;
}
.overview-grid button.active {
  border-color: #42bed4;
  background: #15455c;
}
.overview-grid strong {
  font-size: clamp(22px, 2vw, 34px);
  font-family: Consolas, monospace;
  line-height: 1.1;
  color: #e0f8ff;
  text-shadow: 0 0 10px #43cfe055;
}
.stat-icon {
  border: 1px solid #23617c;
  border-radius: 50%;
  width: 30px;
  height: 30px;
  display: grid;
  place-items: center;
  background: #14465e;
  color: #43bad5;
  font-size: 17px;
}
.screen-scroll {
  overflow: auto;
  min-height: 0;
  scrollbar-width: thin;
  scrollbar-color: #24657f transparent;
}
.place-rows {
  margin-top: 9px;
  flex: 1;
}
.place-rows > button {
  display: flex;
  justify-content: space-between;
  gap: 5px;
  width: 100%;
  text-align: left;
  padding: 11px 10px;
  background: #0a2b46;
  border: 1px solid #11364f;
  color: #bbd7e7;
  margin-bottom: 7px;
  font-size: 12px;
  cursor: pointer;
}
.place-rows b {
  font-size: 10px;
  color: #35b9d3;
  white-space: nowrap;
}
.place-rows small {
  color: #93b8c9;
}
.tiny {
  font-size: 10px;
  color: #5899b4;
}
.inspection-filter {
  display: flex;
  gap: 7px;
  align-items: center;
  margin-bottom: 9px;
}
.inspection-filter > .el-select {
  flex: 1;
  min-width: 0;
}
.inspection-filter > span {
  color: #4baccc;
  font-size: 10px;
  white-space: nowrap;
}
.inspection-card {
  width: 100%;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border: 1px solid #12364e;
  padding: 10px;
  background: #08243d;
  text-align: left;
  margin-bottom: 8px;
  color: #86aec7;
  cursor: pointer;
}
.inspection-card p {
  margin: 0 0 5px;
  font-size: 10px;
}
.inspection-card strong {
  font-weight: 400;
  color: #bed9e8;
}
.inspection-card :deep(.fire-status) {
  font-size: 9px;
  padding: 0 8px;
  line-height: 17px;
}
.mini-camera {
  width: 70px;
  height: 62px;
  background: linear-gradient(145deg, #102b32, #030d17);
  border: 1px solid #1a3e4d;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: space-around;
  color: #426c77;
  flex-shrink: 0;
}
.mini-camera span {
  color: #ca656c;
  font-size: 8px;
}
.mini-camera small {
  font-size: 8px;
}
.command-map {
  position: relative;
  min-height: 0;
  border: 1px solid #1c6680;
}
.command-map :deep(.fire-map) {
  min-height: 100%;
  border: 0;
}
.map-video-types {
  position: absolute;
  top: 60px;
  right: 14px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.map-video-types button {
  background: #0a2b3fec;
  border: 1px solid #376573;
  border-radius: 3px;
  color: #acd0db;
  font-size: 11px;
  padding: 7px 13px;
  cursor: pointer;
}
.map-video-types button.video-count {
  background: #114337;
  color: #91d8bc;
  border-color: #2d7260;
  display: flex;
  gap: 8px;
  align-items: center;
  border-radius: 24px;
  margin-top: 3px;
}
.map-street-label {
  position: absolute;
  bottom: 80px;
  left: 35px;
  font-size: 22px;
  letter-spacing: 6px;
  color: #8caeb2;
  pointer-events: none;
  opacity: 0.65;
}
.map-street-label small {
  display: block;
  font-size: 9px;
  letter-spacing: 3px;
  margin-top: 10px;
}
.alarm-panel {
  flex: 1.08;
  min-height: 0;
}
.hosts-panel {
  flex: 1;
  min-height: 0;
}
.source-tabs {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 4px;
  margin-bottom: 10px;
}
.source-tabs button {
  background: #08223a;
  border: 1px solid #123b56;
  border-radius: 3px;
  color: #7da7be;
  font-size: 10px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 7px 0;
  cursor: pointer;
}
.source-tabs button.active {
  background: #104462;
  border-color: #218aaa;
  color: #c8ebf6;
}
.source-tabs b {
  font-size: 12px;
  font-family: Consolas, monospace;
}
.source-icon {
  width: 25px;
  height: 25px;
  border: 1px solid #3c728a;
  border-radius: 50%;
  display: grid;
  place-items: center;
  font-size: 15px;
  color: #72c7e1;
}
.source-1 {
  color: #f78c97;
  border-color: #805260;
}
.source-2 {
  color: #d9c67d;
  border-color: #756a43;
}
.source-3 {
  color: #b092ed;
  border-color: #64538f;
}
.alarm-list {
  flex: 1;
}
.alarm-card {
  width: 100%;
  border: 1px solid #11334d;
  border-radius: 4px;
  background: #08243d;
  padding: 10px 9px;
  display: flex;
  gap: 9px;
  color: #c5dfec;
  text-align: left;
  margin-bottom: 8px;
  cursor: pointer;
}
.alarm-symbol {
  width: 33px;
  height: 35px;
  display: grid;
  place-items: center;
  border: 1px solid #664052;
  border-radius: 8px;
  font-size: 22px;
  color: #ed839a;
  background: #2d253a;
  flex-shrink: 0;
}
.alarm-symbol.ai {
  color: #b28ae2;
  border-color: #594b7c;
  background: #272746;
}
.alarm-symbol.host {
  color: #d2bf80;
  border-color: #756848;
  background: #2d3031;
}
.alarm-card-body {
  flex: 1;
  min-width: 0;
}
.alarm-code {
  font-size: 10px;
  color: #78a6c0;
  display: flex;
  justify-content: space-between;
  gap: 5px;
}
.alarm-code span {
  font-size: 9px;
  color: #c290b6;
  white-space: nowrap;
}
.alarm-card p {
  font-size: 12px;
  line-height: 1.5;
  margin: 7px 0;
}
.alarm-footer {
  display: flex;
  align-items: center;
  gap: 5px;
  justify-content: space-between;
}
.alarm-footer small {
  font-size: 10px;
  color: #729eb8;
  line-height: 1.6;
}
.alarm-footer :deep(.fire-status) {
  font-size: 9px;
  padding: 1px 5px;
  line-height: 16px;
  max-width: 47px;
  white-space: normal;
  flex-shrink: 0;
}
.host-stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 6px;
  margin-bottom: 12px;
}
.host-stats > a {
  display: flex;
  flex-direction: column;
  align-items: center;
  background: #0a2a45;
  border: 1px solid #143a54;
  padding: 8px 0;
}
.host-stats strong {
  font-size: 26px;
  font-family: Consolas, monospace;
  line-height: 1.1;
  text-shadow: 0 0 8px currentColor;
}
.host-stats span {
  font-size: 10px;
  color: #7aa1b8;
  margin-top: 5px;
}
.history-head {
  display: flex;
  gap: 9px;
  align-items: center;
  margin-bottom: 8px;
}
.history-head strong {
  color: #55bbd1;
  font-size: 11px;
  white-space: nowrap;
}
.history-head .el-input {
  min-width: 0;
}
.history-list {
  flex: 1;
}
.history-list > button {
  width: 100%;
  background: #08243b;
  border: 1px solid #13354d;
  text-align: left;
  padding: 10px;
  color: #accbde;
  margin-bottom: 7px;
  cursor: pointer;
}
.history-list > button > div {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 10px;
  color: #6e96ae;
}
.history-list strong {
  display: block;
  color: #51bed3;
  font-size: 11px;
  margin-top: 6px;
}
.history-list p {
  font-size: 11px;
  margin: 5px 0;
}
.history-list small {
  font-size: 9px;
  color: #6e96ae;
}
.history-list :deep(.fire-status) {
  font-size: 9px;
  line-height: 16px;
  padding: 0 6px;
}
.command-footer {
  display: flex;
  justify-content: space-between;
  height: 24px;
  align-items: center;
  color: #426c85;
  font-size: 9px;
  letter-spacing: 1px;
}
@media (max-height: 850px) and (min-width: 1100px) {
  .command-header {
    min-height: 72px;
  }
  .command-subbar {
    height: 28px;
  }
  .stat-icon {
    display: none;
  }
  .inspection-card {
    padding: 7px;
  }
  .alarm-card {
    padding: 7px;
  }
  .host-stats strong {
    font-size: 23px;
  }
}
@media (max-width: 1100px) {
  .command-screen {
    height: auto;
    overflow: visible;
    min-height: 100vh;
  }
  .command-grid {
    grid-template-columns: 1fr 1fr;
  }
  .command-map {
    grid-column: 1/-1;
    grid-row: 1;
    height: 550px;
  }
  .command-column {
    min-height: 850px;
  }
  .header-left {
    display: none;
  }
  .header-right {
    width: auto;
  }
  .command-title h1 {
    font-size: 22px;
  }
  .command-title p {
    letter-spacing: 3px;
  }
  .header-right > span {
    font-size: 11px;
  }
  .command-subbar > div {
    display: none;
  }
  .command-header {
    gap: 10px;
  }
  .command-column :deep(.panel-body) {
    max-height: 450px;
  }
  .command-footer {
    margin-top: 15px;
  }
}
@media (max-width: 650px) {
  .command-screen {
    padding: 0 8px;
  }
  .command-grid {
    display: flex;
    flex-direction: column;
  }
  .command-map {
    order: -1;
    height: 500px;
  }
  .command-title h1 {
    font-size: 17px;
    letter-spacing: 1px;
  }
  .command-title p {
    font-size: 8px;
    letter-spacing: 1px;
  }
  .header-right > span {
    display: none;
  }
  .header-right > div {
    flex-direction: column;
    gap: 6px;
  }
  .command-column {
    min-height: auto;
  }
  .command-column > .fire-panel {
    min-height: 260px;
  }
  .overview-grid {
    min-height: 180px;
  }
  .command-column :deep(.panel-body) {
    max-height: 420px;
  }
  .command-footer > span:nth-child(2) {
    display: none;
  }
}

/* Unified command-center styling, matching the supplied blue GIS reference. */
.command-screen { background: radial-gradient(ellipse at 50% 0, #122b61 0, #030b20 65%); border: 1px solid #2861aa; }
.command-header { background: linear-gradient(180deg, #173f8caa, #0b1d4766); border-bottom: 2px solid #337cdd; }
.command-title { padding: 12px 24px; background: linear-gradient(180deg, #2156b988, #14376c33); clip-path: polygon(0 0,100% 0,94% 100%,6% 100%); border-bottom: 3px solid #36a6f3; }
.command-title h1 { color:#eef5ff; text-shadow:0 0 14px #408aff; }
.command-column :deep(.fire-panel) { background: linear-gradient(135deg,#122653ed,#0c1b3cdd); border:1px solid #265ca0; box-shadow:inset 0 0 30px #14367544; }
.command-column :deep(.fire-panel > header) { background:linear-gradient(90deg,#19418288,transparent); border-bottom:1px solid #26569a; }
.command-column :deep(.panel-line) { width:10px; height:10px; transform:rotate(45deg); background:#1bbdf4; border:2px solid #307dce; box-shadow:0 0 10px #137cdd; }
.command-map { border-color:#2256a0; box-shadow:inset 0 0 45px #163366; }
.fire-priority-note { padding:9px 10px; margin-bottom:8px; background:linear-gradient(90deg,#742c3e88,#142956); border-left:3px solid #ff7667; color:#ffbdab; font-size:12px; }
.fire-priority-note b { font-size:20px; margin:0 6px; color:#fff1df; }
.fire-priority-note span { float:right; font-size:10px; margin-top:5px; }
.alarm-card { background:linear-gradient(110deg,#34253eaa,#122449); border-color:#514361; }
.host-stats > a, .overview-grid button, .history-list > button, .inspection-card, .place-rows > button { background:#10244bcc; border-color:#254576; }
.map-video-types button { background:linear-gradient(180deg,#154b91,#0c2451); border-color:#2873b7; }


/* Map-first command layout: floating readable panels, quiet chrome. */
.command-screen { --left-panel:clamp(280px,23vw,370px); --right-panel:clamp(320px,26vw,420px); padding:0 14px; background:#050b18; }
.command-header { min-height:72px; background:linear-gradient(180deg,#102954,#081427); }
.command-title { padding:8px 18px; }
.command-title h1 { font-size:clamp(23px,1.8vw,34px); letter-spacing:3px; text-shadow:none; }
.command-title p { margin-top:6px; }
.command-subbar { height:28px; font-size:12px; }
.command-column :deep(.fire-panel) { background:linear-gradient(135deg,#102348f5,#0c1b35ed); border:1px solid #294b72; box-shadow:0 8px 26px #0005; backdrop-filter:blur(5px); }
.command-column :deep(.fire-panel > header) { height:38px; background:#15305588; }
.command-column :deep(.fire-panel h2) { font-size:16px; color:#d3ecf7; }
.command-column :deep(.panel-body) { height:calc(100% - 38px); padding:10px; }
.command-column :deep(.panel-line) { box-shadow:none; width:7px; height:7px; }
.tiny { font-size:12px; color:#8ab5ce; }
.overview-grid { grid-template-columns:repeat(4,1fr); gap:5px; }
.overview-grid button { border:0; background:transparent; font-size:12px; gap:6px; }
.overview-grid strong { font-size:28px; text-shadow:none; }
.stat-icon { display:none; }
.place-rows > button { padding:11px 9px; font-size:14px; border:0; border-bottom:1px solid #28415f; background:transparent; margin-bottom:3px; }
.place-rows b, .place-rows small { font-size:12px; }
.inspection-card { background:#10243b; padding:10px; }
.inspection-card p { font-size:12px; line-height:1.4; }
.inspection-filter > span { font-size:12px; }
.fire-priority-note { font-size:14px; padding:7px 10px; margin-bottom:6px; }
.fire-priority-note span { font-size:12px; }
.fire-priority-note b { font-size:22px; }
.source-tabs { gap:4px; margin-bottom:8px; }
.source-tabs button { flex-direction:row; justify-content:center; flex-wrap:wrap; font-size:11px; padding:6px 2px; border:0; background:#173253; }
.source-icon { display:none; }
.source-tabs button.active { background:#275078; color:#fff; }
.source-tabs b { font-size:13px; }
.alarm-card { padding:9px; gap:6px; margin-bottom:6px; background:#20243be6; border:1px solid #573f4b; border-left:2px solid #ec795e; }
.alarm-symbol { display:none; }
.alarm-code { font-size:11px; color:#9cb7ce; }
.alarm-code span { font-size:11px; color:#abbdd4; }
.alarm-card p { font-size:14px; font-weight:500; margin:5px 0; line-height:1.4; }
.alarm-footer small { font-size:12px; line-height:1.4; color:#a6bfd1; }
.alarm-footer :deep(.fire-status) { font-size:11px; max-width:none; white-space:nowrap; }
.host-stats { gap:4px; margin-bottom:8px; }
.host-stats > a { padding:5px 0; border:0; background:#17304a77; }
.host-stats strong { font-size:23px; text-shadow:none; }
.host-stats span { font-size:12px; margin-top:3px; }
.history-head strong { font-size:12px; }
.history-list > button { padding:8px; }
.history-list strong, .history-list p { font-size:13px; }
.history-list small, .history-list > button > div { font-size:11px; }
.video-tools-toggle { position:absolute; top:65px; right:14px; background:#102348ee; color:#d1e7f3; border:1px solid #35618a; border-radius:4px; padding:10px 14px; cursor:pointer; font-size:14px; z-index:4; }
.video-tools-toggle b { color:#51d2de; margin:0 6px; }
.map-video-types { top:111px; z-index:5; background:#0a1733f5; padding:10px; border:1px solid #315c84; border-radius:4px; }
.map-video-types button { font-size:13px; background:#13304c; padding:10px 13px; }
.map-street-label { display:none; }
@media(min-width:1101px) {
  .command-grid { position:relative; display:block; }
  .command-map { position:absolute; inset:0 -14px; border:0; box-shadow:none; }
  .command-map :deep(.fire-map) { min-height:100%; }
  .command-column { position:absolute; top:8px; bottom:8px; z-index:2; gap:10px; }
  .left-column { left:0; width:var(--left-panel); }
  .right-column { right:0; width:var(--right-panel); }
  .left-column > .fire-panel:first-child { flex:0 0 106px; }
  .places-panel { flex:1; }
  .inspections-panel { flex:0 0 38%; }
  .alarm-panel { flex:1.35; }
  .hosts-panel { flex:1; }
  .command-map :deep(.map-search) { left:calc(var(--left-panel) + 32px); width:calc(100% - var(--left-panel) - var(--right-panel) - 64px); top:14px; }
  .command-map :deep(.map-layer-toggle), .command-map :deep(.map-layer-buttons) { left:calc(var(--left-panel) + 32px); }
  .video-tools-toggle, .map-video-types { right:calc(var(--right-panel) + 32px); }
  .command-map :deep(.map-caption), .command-map :deep(.map-fire-legend) { left:calc(var(--left-panel) + 32px); }
  .command-map :deep(.map-zoom) { right:calc(var(--right-panel) + 32px); }
  .command-map :deep(.map-grid-info) { top:120px; left:calc(var(--left-panel) + 32px); }
}
@media(min-width:1101px) and (max-height:800px) {
  .command-header { min-height:64px; }
  .command-title p { display:none; }
  .alarm-panel { flex:0 0 382px; }
  .alarm-card { padding:6px 8px; }
  .alarm-footer small { font-size:11px; }
  .fire-priority-note { padding:4px 8px; }
  .command-column :deep(.panel-body) { padding:8px; }
  .host-stats { grid-template-columns:repeat(6,1fr); gap:2px; }
  .host-stats strong { font-size:20px; }
  .host-stats span { font-size:10px; white-space:nowrap; }
}
@media(max-width:1100px) {
  .command-column { min-height:1000px; }
  .left-column > .fire-panel:first-child { flex:0 0 125px; }
  .overview-grid { min-height:75px; }
  .alarm-panel { min-height:430px; }
}
@media(max-width:650px) {
  .command-title h1 { font-size:17px; letter-spacing:1px; }
  .command-column { min-height:0; }
  .command-column > .fire-panel:first-child { min-height:130px; }
  .right-column > .alarm-panel:first-child { min-height:430px; }
  .map-video-types { right:14px; }
}
.selected-fire { overflow:auto; min-height:0; padding:4px 2px; }
.selected-fire-head { display:flex; justify-content:space-between; align-items:center; font-size:13px; color:#ffb49c; }
.selected-fire-head button { border:0; background:none; color:#8fcbe2; cursor:pointer; }
.selected-fire h3 { margin:12px 0 6px; font-size:18px; color:#edf5ff; }
.selected-fire p { font-size:14px; margin:0 0 12px; }
.selected-fire dl { display:grid; grid-template-columns:70px 1fr; gap:9px; font-size:12px; }
.selected-fire dt { color:#8ba8bf; } .selected-fire dd { margin:0; }
.selected-fire-actions { display:flex; gap:8px; margin-top:12px; }
.selected-fire-actions .el-button + .el-button { margin-left:0; }
</style>
