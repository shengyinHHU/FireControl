import { videoTalkback } from './video'
import { readTalkReply } from './talkProtocol'
let loadingSdk, currentCall
function loadSdk() {
  if (!loadingSdk) loadingSdk = new Promise((resolve, reject) => {
    const script = document.createElement('script')
    script.src = `${import.meta.env.BASE_URL}vendor/official-talk/broadcastSdk.js`
    script.onload = () => {
      if (!window.initFireTalkSDK) { reject(Error('官方对讲组件不可用')); return }
      window.Recorder.TrafficImgUrl = ''
      window.Recorder.CLog = () => {}
      resolve(window.initFireTalkSDK)
    }
    script.onerror = () => { script.remove(); loadingSdk = null; reject(Error('官方对讲组件加载失败，请重试')) }
    document.head.appendChild(script)
  }).catch(error => { loadingSdk = null; throw error })
  return loadingSdk
}
// A request already processed by the platform may finish after cancellation.
// Authenticate and stop that late ticket without ever opening a microphone.
function retireTicket(ticket) {
  let socket
  try {
    socket = new WebSocket(ticket.wss)
    const timeout = setTimeout(() => { if (socket.readyState < 2) socket.close() }, 5000)
    socket.onopen = () => {
      socket.send(JSON.stringify({ method: '10000', sessionId: ticket.sessionId, token: ticket.token, level: '1' }))
      socket.send(JSON.stringify({ method: '10006', sessionId: ticket.sessionId }))
      socket.close()
    }
    socket.onerror = () => { if (socket.readyState < 2) socket.close() }
    socket.onclose = () => clearTimeout(timeout)
  } catch { if (socket?.readyState < 2) socket.close() }
}
export function createTalkback(deviceCode, { onState, onError, onDiagnostics } = {}) {
  currentCall?.stop()
  let closed = false, stream, context, sdk, deadline, sent = false
  const stats = { frames: 0, bytes: 0, power: 0, peakPower: 0, buffered: 0, gatewayConnected: false, startAck: false, deviceStatus: '未返回' }
  let lastReport = 0
  const report = (force = false) => {
    if (force || Date.now() - lastReport >= 500) { lastReport = Date.now(); onDiagnostics?.({ ...stats }) }
  }
  const phase = value => { if (!closed) onState?.(value) }
  const call = {
    stop() {
      if (closed) return
      closed = true; clearTimeout(deadline)
      const activeSdk = sdk; sdk = null
      try { activeSdk?.closeRecord() } catch { /* Always release local capture even if vendor cleanup fails. */ }
      window.removeEventListener('pagehide', call.stop)
      stream?.getTracks().forEach(track => track.stop()); stream = null
      context?.close().catch(() => {}); context = null
      if (currentCall === call) currentCall = null
      report(true)
      onState?.('stopped')
    },
    async start() {
      const fail = message => { if (!closed) { call.stop(); onError?.(message) } }
      try {
        if (!window.isSecureContext || !navigator.mediaDevices?.getUserMedia)
          throw Error('对讲需要HTTPS或localhost安全页面，并允许浏览器访问麦克风')
        phase('permission')
        deadline = setTimeout(() => fail('等待麦克风授权超时，请重试'), 45000)
        const Audio = window.AudioContext || window.webkitAudioContext
        if (!Audio) throw Error('当前浏览器不支持录音，请使用新版Chrome')
        context = new Audio()
        // Resume while still in the button click gesture, before the 20-second ticket exists.
        await context.resume()
        if (closed) return
        const obtained = await navigator.mediaDevices.getUserMedia({ audio: { channelCount: 1, echoCancellation: true, noiseSuppression: true, autoGainControl: true }, video: false })
        if (closed) { obtained.getTracks().forEach(track => track.stop()); return }
        stream = obtained
        stream.getAudioTracks().forEach(track => track.addEventListener('ended', () => fail('麦克风已断开，对讲已停止')))
        const init = await loadSdk()
        if (closed) return
        clearTimeout(deadline); phase('ticket')
        // Do not abort this HTTP request: late platform tickets must be explicitly retired.
        const { data } = await videoTalkback(deviceCode)
        if (closed) { if (data?.wss && data?.token && data?.sessionId) retireTicket(data); return }
        if (!data?.wss?.startsWith('wss://') || !data.token || !data.sessionId) throw Error('未返回有效的语音会话')
        phase('connecting')
        deadline = setTimeout(() => fail('语音网关连接超时，请检查设备支持情况及网络后重试'), 12000)
        sdk = init(data.wss.slice(6), '', {
          sessionId: data.sessionId, token: data.token,
          onOpen() {
            if (closed) return
            clearTimeout(deadline); phase('connected'); stats.gatewayConnected = true; report(true)
            deadline = setTimeout(() => fail('语音网关未确认开始，对讲已停止，请重试'), 8000)
          },
          onAudio(info) {
            if (closed) return
            stats.frames++; stats.bytes += info.bytes; stats.power = Math.max(0, Math.min(100, info.power)); stats.peakPower = Math.max(stats.peakPower, stats.power); stats.buffered = info.buffered
            report()
            if (!sent) { sent = true; clearTimeout(deadline); phase('sending') }
          },
          onClose() { fail('语音网关连接已断开，对讲已停止') },
          onError(reason) { fail(reason === 'backpressure' ? '语音发送积压，对讲已停止，请检查网络后重试' : '语音网关连接失败，请检查网络、证书或设备支持情况') },
          onMessage(message) {
            if (closed) return
            const reply = readTalkReply(message, deviceCode)
            if (!reply) return
            if (reply.kind === 'device') {
              stats.deviceStatus = reply.ok ? '成功' : '失败'; report(true)
              if (!reply.ok) fail('设备对讲失败：' + (reply.message || '设备拒绝或离线'))
            } else if (reply.kind === 'start') {
              if (!reply.ok) { fail('语音网关拒绝开始：' + (reply.message || '请检查账号、设备或是否被占用')); return }
              if (stats.startAck) return
              stats.startAck = true; report(true); clearTimeout(deadline); phase('recording')
              deadline = setTimeout(() => fail('麦克风未产生音频，对讲已停止，请检查录音设备'), 8000)
              sdk.openRecord(() => {}, () => fail('录音启动失败，请检查麦克风权限和设备'))
            }
          },
        })
        sdk.recorder.set.sourceStream = stream
        sdk.recorder.set.runningContext = context
      } catch (error) {
        const messages = { NotAllowedError: '麦克风权限被拒绝，请在浏览器设置中允许后重试', NotFoundError: '未找到麦克风，请连接录音设备', NotReadableError: '麦克风被其他程序占用或无法读取' }
        fail(messages[error.name] || error.message || '对讲启动失败')
      }
    },
  }
  currentCall = call
  window.addEventListener('pagehide', call.stop)
  return call
}
