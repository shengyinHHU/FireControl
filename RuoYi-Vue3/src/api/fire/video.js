import request from '@/utils/request'
import auth from '@/plugins/auth'
export const canVideo = (action = 'list') =>
  auth.hasPermi(`fire:video:${action}`)
export const videoRegions = (regionId) =>
  request({ url: '/fire/video/regions', params: { regionId }, timeout: 40000 })
export const videoDevices = (regionId, lastId = -1) =>
  request({
    url: '/fire/video/devices',
    params: { regionId, lastId, pageSize: 20 },
    timeout: 40000,
  })
export const videoPreview = (code, protocol = 'hls') =>
  request({
    url: `/fire/video/devices/${encodeURIComponent(code)}/preview`,
    method: 'post',
    headers: { repeatSubmit: false },
    params: { protocol },
    timeout: 40000,
  })
