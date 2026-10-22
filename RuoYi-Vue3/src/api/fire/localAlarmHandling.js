import { ref } from 'vue'
export const HANDLED_ALARMS_KEY = 'fire.local-handled-alarms.v1'
const revision = ref(0)
const keyOf = alarm => `${alarm.sourceCode === '119' || alarm.source === '119警情' ? '119' : 'device'}:${String(alarm.id)}`
function read() {
  if (typeof window === 'undefined') return {}
  const data = JSON.parse(window.localStorage.getItem(HANDLED_ALARMS_KEY) || '{}')
  return data && typeof data === 'object' && !Array.isArray(data) ? data : {}
}
export function isLocallyHandled(alarm) {
  void revision.value
  try { return Object.hasOwn(read(), keyOf(alarm)) } catch { return false }
}
export const visibleLocalAlarms = rows => rows.filter(row => !isLocallyHandled(row))
export function handleAlarmLocally(alarm, note) {
  const text = String(note || '').trim()
  if (!alarm || alarm.id == null) throw Error('警情信息缺失，请重新打开详情')
  if (!text || text.length > 1000) throw Error('请填写1至1000字的处理说明')
  try {
    const data = read()
    const key = keyOf(alarm)
    if (!Object.hasOwn(data, key)) data[key] = { id:String(alarm.id), source:alarm.sourceCode || alarm.source, note:text, handledAt:new Date().toISOString() }
    window.localStorage.setItem(HANDLED_ALARMS_KEY, JSON.stringify(data))
  } catch { throw Error('无法保存处理记录，请检查浏览器存储权限后重试') }
  revision.value++
}
if (typeof window !== 'undefined') window.addEventListener?.('storage', event => {
  if (event.key === HANDLED_ALARMS_KEY || event.key === null) revision.value++
})
