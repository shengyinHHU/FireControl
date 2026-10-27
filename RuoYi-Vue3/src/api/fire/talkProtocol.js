// Confirmed against the real gateway on 2026-10-27: explicit code/status 0
// accompanies success. Never treat a socket open or a locally queued frame as device playback.
export function readTalkReply(text, deviceCode) {
  if (typeof text !== 'string' || text.length > 65536) return null
  let reply
  try { reply = JSON.parse(text) } catch {
    return text.startsWith('OFFLINE:') ? { kind: 'device', ok: false, message: '设备返回离线通知' } : null
  }
  if (!reply || typeof reply !== 'object') return null
  const method = String(reply.method)
  if (method === '10001') return { kind: 'start', ok: String(reply.code) === '0', message: safeMessage(reply.msg) }
  if (method === '10008' && Array.isArray(reply.data?.talkStatus)) {
    const item = reply.data.talkStatus.find(row => String(row?.deviceId) === deviceCode)
    if (item) return { kind: 'device', ok: String(item.status) === '0', message: safeMessage(item.errorMsg) }
  }
  return null
}
function safeMessage(value) {
  if (typeof value !== 'string') return ''
  return value.replace(/(?:https?|wss?):\/\/\S+/gi, '[地址已隐藏]').replace(/(?:token|sign|authorization)\s*[:=]\s*[^ ,;]+/gi, '[凭证已隐藏]').replace(/[\x00-\x1f]/g, '').slice(0, 160)
}
