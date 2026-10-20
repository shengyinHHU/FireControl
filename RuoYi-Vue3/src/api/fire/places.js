import request from '@/utils/request'
import { getToken } from '@/utils/auth'
import { PLACE_LAYERS, normalizePlaceRecord } from './placeLayers'

function requireLogin() {
  if (!getToken()) throw Error('请先登录后加载真实场所')
}
// Always query the real backend, even while unrelated dashboard business is in demo mode.
export function listMapPlaces() {
  requireLogin()
  return request({ url: '/fire/places/map', method: 'get' })
}
export async function listDatabasePlaces(query = {}) {
  requireLogin()
  const { category, keyword, ...params } = query
  if (category) params.placeType = PLACE_LAYERS.find(layer => layer.label === category)?.type || category
  if (keyword) params.name = keyword
  const response = await request({ url: '/fire/places/list', method: 'get', params })
  return { ...response, rows: (response.rows || []).map(normalizePlaceRecord).filter(Boolean) }
}
export async function getDatabasePlace(id) {
  requireLogin()
  const response = await request({ url: `/fire/places/${encodeURIComponent(id)}`, method: 'get' })
  return { ...response, data: normalizePlaceRecord(response.data) }
}
