import request from '@/utils/request'
import auth from '@/plugins/auth'
export const canSms = (action = 'send') => auth.hasPermi(`fire:sms:${action}`)
export const smsRecipient = source => request({ url: '/fire/sms/recipient', params: source })
// Backend owns idempotency. Do not cache phone numbers and message bodies in request.js session storage.
export const sendSms = data => request({ url: '/fire/sms/send', method: 'post', data, timeout: 45000, headers: { repeatSubmit: false } })
export const smsRequest = id => request({ url: `/fire/sms/requests/${encodeURIComponent(id)}` })
export const listSmsRecords = params => request({ url: '/fire/sms/list', params })
