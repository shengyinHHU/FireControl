// getRandomValues also works in browsers without randomUUID support.
export function createCallId() {
  const cryptoApi = globalThis.crypto
  if (typeof cryptoApi?.randomUUID === 'function') return cryptoApi.randomUUID()
  if (typeof cryptoApi?.getRandomValues !== 'function') throw new Error('浏览器不支持安全随机数，请使用新版浏览器')
  const bytes = cryptoApi.getRandomValues(new Uint8Array(16))
  return Array.from(bytes, value => value.toString(16).padStart(2, '0')).join('')
}
