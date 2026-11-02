import request, { download } from '@/utils/request'
import { hasMapCoordinates } from './placeLayers'

export async function listVideoCameras(params = {}) {
  const response = await request({ url: '/fire/video/catalog', params })
  return response.data
}
export const videoCatalogDirectories = () => request({ url: '/fire/video/catalog/directories' })
export const videoCatalogMap = () => request({ url: '/fire/video/catalog/map' })
export const getVideoCamera = id => request({ url: `/fire/video/catalog/${encodeURIComponent(id)}` })
export const syncVideoCatalog = () => request({ url: '/fire/video/catalog/sync', method: 'post' })
export const exportVideoCoordinates = params => download('/fire/video/catalog/export', params, '监控坐标调查表.xlsx')
export function previewVideoCoordinates(file, mode) {
  const data = new FormData()
  data.append('file', file)
  data.append('mode', mode)
  return request({ url: '/fire/video/catalog/import/preview', method: 'post', data, timeout: 120000, headers: { 'Content-Type': 'multipart/form-data' } })
}
export const confirmVideoCoordinates = batchId => request({ url: '/fire/video/catalog/import/confirm', method: 'post', data: { batchId }, timeout: 120000, headers: { repeatSubmit: false } })
export const editVideoCoordinates = (id, data) => request({ url: `/fire/video/catalog/${encodeURIComponent(id)}/coordinates`, method: 'put', data, headers: { repeatSubmit: false } })

export const videoLocationText = status => ({ pending: '未填写', review: '待核验', confirmed: '已核验' }[status] || '待核验')
export function validVideoPoint(row) {
  return hasMapCoordinates(row.lng, row.lat)
}
