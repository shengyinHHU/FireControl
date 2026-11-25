// One request at a time. Foreground changes invalidate old queries; background bursts coalesce.
export function createTableRefresh(run, { paused = () => false, delay = 250 } = {}) {
  let generation = 0, active = false, pending = null, timer, stopped = false
  async function drain() {
    clearTimeout(timer); timer = undefined
    if (stopped || active || !pending || (pending.silent && paused())) return
    const job = pending
    pending = null; active = true
    try { await run(job.silent, () => !stopped && job.generation === generation) }
    finally { active = false; if (pending && !stopped) timer = setTimeout(drain, delay) }
  }
  return {
    refresh(silent = false) {
      if (stopped) return
      if (!silent) generation++
      pending = { silent: silent && pending?.silent !== false, generation }
      if (!silent) drain()
      else if (!timer) timer = setTimeout(() => { timer = undefined; drain() }, delay)
    },
    resume() { if (pending) { clearTimeout(timer); timer = undefined; drain() } },
    stop() { stopped = true; generation++; pending = null; clearTimeout(timer) },
  }
}

// Preserve unchanged row/array identities and Element Plus's scroll/selection state.
export function reconcileTableRows(previous, incoming) {
  const byId = new Map(previous.map(row => [row.id, row]))
  const next = incoming.map(row => {
    const old = byId.get(row.id)
    if (!old) return row
    if (JSON.stringify(old) !== JSON.stringify(row)) {
      for (const key of Object.keys(old)) if (!(key in row)) delete old[key]
      Object.assign(old, row)
    }
    return old
  })
  return previous.length === next.length && next.every((row, i) => row === previous[i]) ? previous : next
}
