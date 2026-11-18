export function smsPhones(raw = '') {
  return [...new Set(String(raw).split(/[,，;；/、\s]+/).map(p => p.replace(/^(?:\+86|0086)/, '')).filter(p => /^1[3-9]\d{9}$/.test(p)))]
}
export const smsStatuses = Object.freeze({ pending: '正在提交', submitted: '已提交平台', partial: '部分号码已提交', failed: '提交失败', unknown: '提交结果待确认' })
export const smsStatus = value => value === 'rejected' ? '未提交' : smsStatuses[value] || '状态待确认'
export function smsRequestId() {
  if (globalThis.crypto?.randomUUID) return globalThis.crypto.randomUUID()
  return '10000000-1000-4000-8000-100000000000'.replace(/[018]/g, c => {
    const random = globalThis.crypto?.getRandomValues ? globalThis.crypto.getRandomValues(new Uint8Array(1))[0] : Math.floor(Math.random() * 256)
    return (Number(c) ^ random & 15 >> Number(c) / 4).toString(16)
  })
}
