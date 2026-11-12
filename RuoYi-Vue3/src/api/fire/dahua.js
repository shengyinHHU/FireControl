import request from '@/utils/request'
const devicePath = code => `/fire/video/dahua/devices/${encodeURIComponent(code)}`
export const getDahuaBinding = code => request({ url: devicePath(code) + '/binding' })
export const saveDahuaBinding = (code, data) => request({ url: devicePath(code) + '/binding', method: 'put', data })
export const deleteDahuaBinding = (code, revision) => request({ url: devicePath(code) + '/binding', method: 'delete', params: { revision } })
export const dahuaTicket = code => request({ url: devicePath(code) + '/sessions', method: 'post', headers: { repeatSubmit: false } })
export const cancelDahuaTicket = ticket => request({ url: '/fire/video/dahua/sessions', method: 'delete', data: { ticket }, headers: { repeatSubmit: false }, timeout: 5000 })
