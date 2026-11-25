import { isDisplayAlarm } from './rules.js'

// The first successful snapshot is a baseline, not a batch of new incidents.
// Keep seen IDs for this dashboard session so polling/reconnection cannot replay them.
export function createAlarmSignals() {
  const seen = new Set()
  let initialized = false
  let pending = new Set()
  return {
    update(rows, simulationId = '') {
      const eligible = rows.filter(isDisplayAlarm)
      const current = new Set(eligible.map(row => row.id))
      const fresh = eligible.filter(row => !seen.has(row.id) && (initialized || (row.isSimulation && row.id === simulationId)))
      pending = new Set([...pending].filter(id => current.has(id)))
      for (const row of eligible) seen.add(row.id)
      for (const row of fresh) pending.add(row.id)
      initialized = true
      return { fresh, pendingCount: pending.size }
    },
    stop() { pending.clear() },
  }
}
