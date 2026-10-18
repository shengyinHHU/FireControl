import { isFireMock } from '@/api/fire/config'
import { canFire } from '@/api/fire/access'
const definitions = [
  {
    path: 'dashboard',
    name: 'FireDashboard',
    component: () => import('@/views/fire/dashboard/index.vue'),
    meta: { title: '指挥大屏', icon: 'monitor', fireResource: 'dashboard' },
    beforeEnter: () => canFire('dashboard') || '/401',
  },
  {
    path: 'places',
    name: 'FirePlaces',
    component: () => import('@/views/fire/places/index.vue'),
    meta: { title: '重点场所', icon: 'monitor', fireResource: 'places' },
    beforeEnter: () => canFire('places') || '/401',
  },
  {
    path: 'grids',
    name: 'FireGrids',
    component: () => import('@/views/fire/grids/index.vue'),
    meta: { title: '网格管理', icon: 'monitor', fireResource: 'grids' },
    beforeEnter: () => canFire('grids') || '/401',
  },
  {
    path: 'video',
    name: 'FireVideo',
    component: () => import('@/views/fire/video/index.vue'),
    meta: { title: '视频监控', icon: 'monitor', fireResource: 'cameras' },
    beforeEnter: () => canFire('cameras') || '/401',
  },
  {
    path: 'inspections',
    name: 'FireInspections',
    component: () => import('@/views/fire/inspections/index.vue'),
    meta: { title: '查岗管理', icon: 'monitor', fireResource: 'inspections' },
    beforeEnter: () => canFire('inspections') || '/401',
  },
  {
    path: 'alarms',
    name: 'FireAlarms',
    component: () => import('@/views/fire/alarms/index.vue'),
    meta: { title: '警情处置', icon: 'monitor', fireResource: 'alarms' },
    beforeEnter: () => canFire('alarms') || '/401',
  },
  {
    path: 'hosts',
    name: 'FireHosts',
    component: () => import('@/views/fire/hosts/index.vue'),
    meta: { title: '消防主机', icon: 'monitor', fireResource: 'hosts' },
    beforeEnter: () => canFire('hosts') || '/401',
  },
  {
    path: 'devices',
    name: 'FireDevices',
    component: () => import('@/views/fire/devices/index.vue'),
    meta: { title: '设备台账', icon: 'monitor', fireResource: 'devices' },
    beforeEnter: () => canFire('devices') || '/401',
  },
  {
    path: 'personnel',
    name: 'FirePersonnel',
    component: () => import('@/views/fire/personnel/index.vue'),
    meta: { title: '责任人员', icon: 'monitor', fireResource: 'personnel' },
    beforeEnter: () => canFire('personnel') || '/401',
  },
  {
    path: 'materials',
    name: 'FireMaterials',
    component: () => import('@/views/fire/materials/index.vue'),
    meta: { title: '消防物资', icon: 'monitor', fireResource: 'materials' },
    beforeEnter: () => canFire('materials') || '/401',
  },
  {
    path: 'hazards',
    name: 'FireHazards',
    component: () => import('@/views/fire/hazards/index.vue'),
    meta: { title: '安全生产', icon: 'monitor', fireResource: 'hazards' },
    beforeEnter: () => canFire('hazards') || '/401',
  },
  {
    path: 'tasks',
    name: 'FireTasks',
    component: () => import('@/views/fire/tasks/index.vue'),
    meta: { title: '任务派发', icon: 'monitor', fireResource: 'tasks' },
    beforeEnter: () => canFire('tasks') || '/401',
  },
  {
    path: 'duty',
    name: 'FireDuty',
    component: () => import('@/views/fire/duty/index.vue'),
    meta: { title: '值班上报', icon: 'monitor', fireResource: 'duty' },
    beforeEnter: () => canFire('duty') || '/401',
  },
  {
    path: 'messages',
    name: 'FireMessages',
    component: () => import('@/views/fire/messages/index.vue'),
    meta: { title: '消息中心', icon: 'monitor', fireResource: 'messages' },
    beforeEnter: () => canFire('messages') || '/401',
  },
  {
    path: 'sms',
    name: 'FireSms',
    component: () => import('@/views/fire/sms/index.vue'),
    meta: { title: '短信通知', icon: 'monitor', fireResource: 'sms' },
    beforeEnter: () => canFire('sms') || canFire('smsRules') || '/401',
  },
]
// The full-screen board uses the fire shell. Production management uses RuoYi Layout.
const dashboard = definitions.filter((r) => r.path === 'dashboard')
const management = definitions.filter((r) => r.path !== 'dashboard')
export const fireRoutes = isFireMock
  ? [
      {
        path: '/fire',
        component: () => import('@/views/fire/Shell.vue'),
        redirect: '/fire/dashboard',
        children: definitions,
      },
    ]
  : [
      {
        path: '/fire',
        component: () => import('@/views/fire/AdminLayout.vue'),
        redirect: '/fire/places',
        alwaysShow: true,
        meta: { title: '智慧消防', icon: 'monitor' },
        children: management,
      },
      {
        path: '/fire/dashboard',
        component: () => import('@/views/fire/Shell.vue'),
        hidden: true,
        children: dashboard.map((route) => ({ ...route, path: '' })),
      },
    ]
export const firePublicRoutes = [
  {
    path: '/fire-public/duty',
    component: () => import('@/views/fire/duty/report.vue'),
    hidden: true,
    meta: { title: '每日值班上报' },
  },
  {
    path: '/fire-public/video',
    component: () => import('@/views/fire/video/share.vue'),
    hidden: true,
    meta: { title: '授权视频' },
  },
]
