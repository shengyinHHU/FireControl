// Verified platform flow: from + phone -> create meeting -> native call page.
export function buildPlatformCallUrl(contact) {
  const phone = String(contact.phone || '').replace(/[\s-]/g, '').replace(/^\+86/, '')
  if (!/^1[3-9]\d{9}$/.test(phone)) throw new Error('请配置有效的手机号码，不能将多个号码合并拨打')
  const url = new URL('https://www.jnrhtx.top/meeting/index')
  url.searchParams.set('from', 'Dc')
  url.searchParams.set('phone', phone)
  return url.href
}
