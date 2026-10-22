import { visibleLocalAlarms } from './localAlarmHandling.js'
import request from '@/utils/request'
import auth from '@/plugins/auth'
const urls = { gateways: '/fire/gateways/list', hosts: '/fire/hosts/list', alarms: '/fire/alarms/list', protocolMessages: '/fire/protocol/messages' }
export const normalizeDeviceRow = row => {
  const incident119 = row.sourceCode === '119'
  return {
    ...row, id: String(row.id), dataSource: 'database',
    alarmType: incident119 ? (row.caseTypeCode === '1' ? 'FIRE' : 'INFORMATION') : ['fire', 'manual_alarm'].includes(row.kind) ? 'FIRE' : 'INFORMATION',
    status: incident119 ? (row.status || '未提供') : row.kind ? (['fire', 'manual_alarm'].includes(row.kind) ? '待核验' : '设备记录') : row.status,
    timeline: [],
  }
}
export async function incident119Status() {
  return (await request({ url: '/fire/119/status' })).data
}
export async function listDeviceRecords(resource, query = {}) {
  const res = await request({ url: urls[resource], params: query })
  const rows = res.rows.map(normalizeDeviceRow)
  return { ...res, rows: resource === 'alarms' ? visibleLocalAlarms(rows) : rows }
}
export async function getDeviceRecord(resource, id) {
  const res = await request({ url: `/fire/${resource}/${encodeURIComponent(id)}` })
  return { ...res, data: normalizeDeviceRow(res.data) }
}
export async function deviceStats() {
  const [hosts, events, incident119] = await Promise.all([
    auth.hasPermi('fire:hosts:list') ? request({ url: '/fire/hosts/stats' }) : Promise.resolve({data:{hosts:null,online:null,gateways:null}}),
    auth.hasPermi('fire:alarms:list') ? request({ url: '/fire/alarms/stats' }) : Promise.resolve({data:{eventTotal:null,fireEvents:null,faultEvents:null}}),
    auth.hasPermi('fire:alarms:list') ? incident119Status() : Promise.resolve(null),
  ])
  return { ...hosts.data, ...events.data, incident119 }
}
