import { ref, onMounted, onBeforeUnmount } from 'vue'
import { startAlarmStream } from '@/api/fire/alarmStream'
import { getToken } from '@/utils/auth'

// A manager and its embedded tables share a single authenticated connection per permission scope.
const streams = new Map()
export function useDeviceUpdates(onChange, resource = 'gateways', calibrationMs = 60000) {
  const state = ref('connecting')
  let entry, timer
  const listener = () => onChange()
  onMounted(() => {
    entry = streams.get(resource)
    if (!entry) {
      entry = { listeners: new Set(), states: new Set(), state: 'connecting' }
      streams.set(resource, entry)
      const current = entry
      entry.stop = startAlarmStream({
        url: `${import.meta.env.VITE_APP_BASE_API}/fire/devices/stream?resource=${resource}`,
        getToken, events: ['ready', 'device-change'], reconnectDelay: 1000,
        onChange: () => current.listeners.forEach(callback => callback()),
        onState: value => { current.state = value; current.states.forEach(target => { target.value = value }) },
      })
    }
    state.value = entry.state
    entry.listeners.add(listener); entry.states.add(state)
    // Event delivery is best effort. Reconcile after gaps, and when a background tab becomes visible.
    timer = setInterval(listener, calibrationMs)
    document.addEventListener('visibilitychange', visible)
  })
  function visible() { if (!document.hidden) listener() }
  onBeforeUnmount(() => {
    clearInterval(timer); document.removeEventListener('visibilitychange', visible)
    if (!entry) return
    entry.listeners.delete(listener); entry.states.delete(state)
    if (!entry.listeners.size) { entry.stop(); streams.delete(resource) }
  })
  return state
}
