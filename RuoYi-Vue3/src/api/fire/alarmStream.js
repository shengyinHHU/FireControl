// SSE uses fetch so the RuoYi JWT stays in the Authorization header, never the URL.
export function createSseParser(onEvent) {
  let pending = ''
  return chunk => {
    pending += chunk
    if (pending.length > 65536) throw Error('Invalid event stream')
    let boundary
    while ((boundary = /\r?\n\r?\n/.exec(pending))) {
      const block = pending.slice(0, boundary.index)
      pending = pending.slice(boundary.index + boundary[0].length)
      const event = block.split(/\r?\n/).find(line => line.startsWith('event:'))?.slice(6).trim()
      if (event === 'ready' || event === 'alarm-change') onEvent(event)
    }
  }
}
export function startAlarmStream({ url, getToken, onChange, fetchImpl = globalThis.fetch }) {
  let stopped = false, active, retryTimer, lifetime, failures = 0
  async function connect() {
    if (stopped || !getToken()) return
    active = new AbortController()
    lifetime = setTimeout(() => active?.abort(), 65000)
    let retry = true
    try {
      const response = await fetchImpl(url, { headers: { Accept: 'text/event-stream', Authorization: 'Bearer ' + getToken() }, signal: active.signal, cache: 'no-store' })
      if (response.status === 401 || response.status === 403) { retry = false; return }
      if (!response.ok || !response.headers.get('content-type')?.includes('text/event-stream') || !response.body) throw Error('Stream unavailable')
      failures = 0
      const decoder = new TextDecoder(), reader = response.body.getReader()
      const parse = createSseParser(() => { if (!stopped) onChange() })
      try {
        while (!stopped) { const { value, done } = await reader.read(); if (done) break; parse(decoder.decode(value, { stream: true })) }
      } finally { await reader.cancel().catch(() => {}) }
    } catch { /* Polling remains active; reconnect silently without repeated error toasts. */ }
    finally {
      clearTimeout(lifetime)
      if (!stopped && retry) retryTimer = setTimeout(connect, Math.min(30000, 5000 * 2 ** Math.min(failures++, 3)))
    }
  }
  connect()
  return () => { stopped = true; clearTimeout(retryTimer); clearTimeout(lifetime); active?.abort() }
}
