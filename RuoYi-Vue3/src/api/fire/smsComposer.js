import { shallowRef } from 'vue'
export const smsTarget = shallowRef(null)
let opener
export function openSmsComposer(source, element) { opener = element; smsTarget.value = { ...source } }
export function closeSmsComposer() { smsTarget.value = null }
export function restoreSmsFocus() { if (opener?.isConnected) opener.focus({ preventScroll: true }); opener = null }
