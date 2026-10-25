import request from '@/utils/request'
export function previewPlaceImport(file) {
  const data = new FormData(); data.append('file', file)
  return request({ url: '/fire/places/import/preview', method: 'post', data, timeout: 120000,
    headers: { 'Content-Type': 'multipart/form-data', repeatSubmit: false } })
}
export function confirmPlaceImport(batchId) {
  return request({ url: '/fire/places/import/confirm', method: 'post', data: { batchId }, timeout: 120000,
    headers: { repeatSubmit: false } })
}
