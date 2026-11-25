import request from '@/utils/request'

export const createAlarmSimulation = data => request({ url: '/fire/alarms/simulations', method: 'post', data, headers: { repeatSubmit: false } })
export const handleAlarmSimulation = (id, note) => request({ url: `/fire/alarms/simulations/${encodeURIComponent(id)}/handle`, method: 'post', data: { note }, headers: { repeatSubmit: false } })
export function simulationRequestId() {
  // crypto.randomUUID is unavailable on many HTTP intranet deployments.
  const bytes = crypto.getRandomValues(new Uint8Array(16))
  bytes[6] = (bytes[6] & 15) | 64
  bytes[8] = (bytes[8] & 63) | 128
  const hex = [...bytes].map(value => value.toString(16).padStart(2, '0')).join('')
  return `${hex.slice(0, 8)}-${hex.slice(8, 12)}-${hex.slice(12, 16)}-${hex.slice(16, 20)}-${hex.slice(20)}`
}
