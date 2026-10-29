import request from '@/utils/request'
import auth from '@/plugins/auth'
export const canInspection = (action = 'list') => auth.hasPermi(`fire:inspections:${action}`)
export const inspectionPosts = params => request({ url: '/fire/inspection/posts', params })
export const inspectionPost = id => request({ url: `/fire/inspection/posts/${id}` })
export const saveInspectionPost = data => request({ url: '/fire/inspection/posts' + (data.postId ? '/' + data.postId : ''), method: data.postId ? 'put' : 'post', data, timeout: 90000, headers: { repeatSubmit: false } })
export const inspectionRecords = params => request({ url: '/fire/inspection/records', params })
export const inspectionRecord = id => request({ url: `/fire/inspection/records/${id}` })
export const inspectionEvidence = async id => { const blob=await request({ url: `/fire/inspection/records/${id}/evidence`, responseType: 'blob' }); if(!blob.type.startsWith('image/')) throw Error('证据加载失败或无权访问'); return blob }
export const previewInspection = (post, binding, protocol) => request({ url: `/fire/inspection/posts/${post}/cameras/${binding}/preview`, method: 'post', params: { protocol }, timeout: 90000, headers: { repeatSubmit: false } })
export function saveInspectionRecord(data, evidence) {
 const body = new FormData();body.append('data', JSON.stringify(data));if (evidence) body.append('evidence', evidence, 'capture.jpg')
 return request({ url: '/fire/inspection/records', method: 'post', data: body, timeout: 60000, headers: { 'Content-Type': 'multipart/form-data', repeatSubmit: false } })
}
export const inspectionResult = value => ({ present: '在岗', absent: '脱岗', unknown: '无法判断' })[value] || '尚未查岗'
