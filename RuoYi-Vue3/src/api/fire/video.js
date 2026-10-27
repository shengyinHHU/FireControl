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
export const videoPreview = (code, protocol = 'flv') =>
  request({
    url: `/fire/video/devices/${encodeURIComponent(code)}/preview`,
    method: 'post',
    headers: { repeatSubmit: false },
    params: { protocol },
    timeout: 40000,
  })

export const closeVideoStream = (url) =>
  request({ url, method: 'delete', timeout: 5000, headers: { repeatSubmit: false } })

export const videoTalkback = (code, signal) =>
  request({
    url: `/fire/video/devices/${encodeURIComponent(code)}/talkback`,
    method: 'post', signal, timeout: 35000,
    headers: { repeatSubmit: false },
  })
