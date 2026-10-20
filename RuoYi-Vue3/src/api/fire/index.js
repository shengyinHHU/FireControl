import request from '@/utils/request'
import { listDeviceRecords, getDeviceRecord, deviceStats } from './deviceAccess'
import { validateDutyInput } from './rules'
import { isFireMock } from './config'
import { usesDatabase } from './sourcePolicy'
import { listDatabasePlaces, getDatabasePlace } from './places'
let repository
async function mock() {
  if (!repository) repository = import('@/mock/fire/repository').then(({ createRepository }) => createRepository({ storage: window.localStorage }))
  return repository
}
const resources = ['places', 'grids', 'personnel', 'materials', 'hazards', 'cameras', 'devices', 'hosts', 'tasks', 'inspections', 'alarms', 'duty', 'messages', 'smsRules', 'sms', 'shares', 'demo', 'gateways', 'protocolMessages']
const path = resource => { if (!resources.includes(resource)) throw Error('未知业务资源'); return `/fire/${resource}` }
export const fire = {
  async list(resource, query = {}) { if (resource === 'places') return listDatabasePlaces(query); if (['gateways','hosts','alarms','protocolMessages'].includes(resource)) return listDeviceRecords(resource, query); return isFireMock ? (await mock()).list(resource, query) : request({ url: `${path(resource)}/list`, params: query }) },
  async get(resource, id) { if (resource === 'places') return getDatabasePlace(id); if (['hosts','alarms'].includes(resource)) return getDeviceRecord(resource, id); if (usesDatabase(resource)) throw Error('当前资源仅提供分页查询'); return isFireMock ? (await mock()).get(resource, id) : request({ url: `${path(resource)}/${encodeURIComponent(id)}` }) },
  async create(resource, data) { if (usesDatabase(resource)) throw Error('该资源使用真实数据库，当前仅提供查询'); return isFireMock ? (await mock()).create(resource, data) : request({ url: path(resource), method: 'post', data }) },
  async update(resource, id, data) { if (usesDatabase(resource)) throw Error('该资源使用真实数据库，当前仅提供查询'); return isFireMock ? (await mock()).update(resource, id, data) : request({ url: path(resource), method: 'put', data: { ...data, id } }) },
  async remove(resource, id) { if (usesDatabase(resource)) throw Error('该资源使用真实数据库，当前仅提供查询'); return isFireMock ? (await mock()).remove(resource, id) : request({ url: `${path(resource)}/${encodeURIComponent(id)}`, method: 'delete' }) },
  async dashboard() { const base = isFireMock ? await (await mock()).dashboard() : { data: {} }; const real = await deviceStats(); return { ...base, data: { ...base.data, pending: undefined, processing: undefined, closed: undefined, ...real } } },
  async action(resource, id, action, data = {}) { if (usesDatabase(resource)) throw Error('该资源使用真实数据库，当前仅提供查询'); return isFireMock ? (await mock()).action(resource, id, action, data) : request({ url: `${path(resource)}/${encodeURIComponent(id || '_')}/${action}`, method: 'post', data }) }
}
export const transitionAlarm = (id, data) => fire.action('alarms', id, 'transition', data)
export const bindCamera = (id, cameraId) => fire.action('devices', id, 'bind-camera', { cameraId })
export const createInspection = data => fire.create('inspections', data)
export const submitDuty = data => fire.create('duty', data)
export const createShare = data => fire.action('shares', '', 'create', data)
export const requestVideo = id => fire.action('cameras', id, 'preview')
export const requestTalk = id => fire.action('cameras', id, 'talk')
export const requestCall = id => fire.action('personnel', id, 'call')
export const markMessageRead = id => fire.action('messages', id, 'read')
export async function resolveShare(token, kind) {
  if (!token) throw Error('缺少授权信息，请扫描有效二维码')
  if (isFireMock) {
    const { data } = await fire.action('shares', token, 'resolve')
    if (data.kind !== kind) throw Error('分享链接类型不匹配')
    const item = await fire.get(kind === 'duty' ? 'places' : 'cameras', data.resourceId)
    return { ...data, name: item.data.name }
  }
  return (await request({ url: `/fire/public/shares/${encodeURIComponent(token)}`, params: { kind }, headers: { isToken: false } })).data
}
export async function submitPublicDuty(token, payload) {
  validateDutyInput(payload)
  if (isFireMock) { const share = await resolveShare(token, 'duty'); return submitDuty({ ...payload, placeId: share.resourceId }) }
  return request({ url: '/fire/public/duty', method: 'post', data: { ...payload, token }, headers: { isToken: false } })
}
export async function previewPublicVideo(token) {
  if (isFireMock) { const share = await resolveShare(token, 'video'); return (await requestVideo(share.resourceId)).data }
  return (await request({ url: '/fire/public/video', method: 'post', data: { token }, headers: { isToken: false } })).data
}
