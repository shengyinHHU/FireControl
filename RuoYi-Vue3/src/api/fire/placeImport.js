import request from '@/utils/request'
import { normalizePlaceCategory, placeLayer } from './placeLayers'

function normalizeImportResponse(response) {
  if (!Array.isArray(response.data?.rows)) return response
  return { ...response, data: { ...response.data, rows: response.data.rows.map(row => ({
    ...row, category: placeLayer(row.type)?.label || normalizePlaceCategory(row.category),
  })) } }
}

export async function previewPlaceImport(file) {
  const data = new FormData(); data.append('file', file)
  return normalizeImportResponse(await request({ url: '/fire/places/import/preview', method: 'post', data, timeout: 120000,
    headers: { 'Content-Type': 'multipart/form-data', repeatSubmit: false } }))
}
export async function confirmPlaceImport(batchId) {
  return normalizeImportResponse(await request({ url: '/fire/places/import/confirm', method: 'post', data: { batchId }, timeout: 120000,
    headers: { repeatSubmit: false } }))
}
