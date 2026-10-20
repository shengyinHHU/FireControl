import request from '@/utils/request'
import auth from '@/plugins/auth'
const urls = { gateways: '/fire/gateways/list', hosts: '/fire/hosts/list', alarms: '/fire/alarms/list', protocolMessages: '/fire/protocol/messages' }
export const normalizeDeviceRow = row => ({
  ...row, id: String(row.id), dataSource: 'database',
  alarmType: ['fire', 'manual_alarm'].includes(row.kind) ? 'FIRE' : 'INFORMATION',
  status: row.kind ? (['fire', 'manual_alarm'].includes(row.kind) ? '待核验' : '设备记录') : row.status,
  timeline: [],
})
export async function listDeviceRecords(resource, query = {}) {
  const res = await request({ url: urls[resource], params: query })
  return { ...res, rows: res.rows.map(normalizeDeviceRow) }
}
export async function getDeviceRecord(resource, id) {
  const res = await request({ url: `/fire/${resource}/${encodeURIComponent(id)}` })
  return { ...res, data: normalizeDeviceRow(res.data) }
}
export async function deviceStats() {
  const [hosts, events] = await Promise.all([
    auth.hasPermi('fire:hosts:list') ? request({ url: '/fire/hosts/stats' }) : Promise.resolve({data:{hosts:null,online:null,gateways:null}}),
    auth.hasPermi('fire:alarms:list') ? request({ url: '/fire/alarms/stats' }) : Promise.resolve({data:{eventTotal:null,fireEvents:null,faultEvents:null}}),
  ])
  return { ...hosts.data, ...events.data }
}
