// Load the vendor-supplied SRS player and its exact bundled dependencies on demand.
let ready
function script(name) {
  return new Promise((resolve, reject) => {
    const el = document.createElement('script')
    el.src = `${import.meta.env.BASE_URL}vendor/official-rtc/${name}`
    el.onload = resolve
    el.onerror = () => { el.remove(); reject(Error('官方播放器加载失败，请刷新后重试')) }
    document.head.appendChild(el)
  })
}
export function loadOfficialRtc() {
  if (!ready) ready = (async () => {
    await script('jquery-1.10.2.min.js')
    await script('adapter-7.4.0.min.js')
    await script('srs.sdk.js')
    return window.SrsRtcPlayerAsync
  })().catch(error => { ready = null; throw error })
  return ready
}
