import { dahuaTicket, cancelDahuaTicket } from './dahua'
import { decodeAudio } from '../../../public/dahua/audio-codec.js'
let current
export function createDahuaTalkback(code, { onState, onError, onDiagnostics } = {}) {
  current?.stop()
  let closed = false, context, stream, source, capture, sink, receiveGain, socket, ticket, ping, timeout, playback = 0, ready = false, listening = true, lastPong = Date.now()
  const playing = new Set(), stats = { frames: 0, bytes: 0, received: 0, power: 0 }
  const state = s => { if (!closed) onState?.(s) }
  const fail = message => { if (!closed) { call.stop(); onError?.(message) } }
  const call = {
    setListening(enabled) { listening = enabled; if (receiveGain) receiveGain.gain.value = listening ? 1 : 0 },
    stop() {
      if (closed) return
      closed = true; clearInterval(ping); clearTimeout(timeout)
      window.removeEventListener('pagehide', call.stop)
      try { if (socket?.readyState === 1) socket.send(JSON.stringify({ type: 'stop' })); socket?.close() } catch { /* cleanup continues */ }
      if (ticket) cancelDahuaTicket(ticket).catch(() => {})
      stream?.getTracks().forEach(t => t.stop()); capture?.disconnect(); source?.disconnect(); sink?.disconnect(); receiveGain?.disconnect()
      playing.forEach(s => { try { s.stop() } catch { /* already ended */ } }); playing.clear()
      context?.close().catch(() => {}); onState?.('stopped')
      if (current === call) current = null
    },
    async start() {
      try {
        if (!window.isSecureContext || !navigator.mediaDevices?.getUserMedia) throw Error('对讲需要HTTPS或localhost，并允许麦克风权限')
        state('permission'); timeout = setTimeout(() => fail('等待麦克风授权超时'), 45000)
        context = new AudioContext(); receiveGain = context.createGain(); receiveGain.gain.value = listening ? 1 : 0; receiveGain.connect(context.destination)
        await context.resume(); if (closed) return
        const mic = await navigator.mediaDevices.getUserMedia({ audio: { channelCount: 1, echoCancellation: true, noiseSuppression: true, autoGainControl: true } })
        if (closed) { mic.getTracks().forEach(t => t.stop()); return }
        stream = mic; stream.getTracks().forEach(t => t.addEventListener('ended', () => fail('麦克风已断开')))
        await context.audioWorklet.addModule(`${import.meta.env.BASE_URL}dahua/capture-worklet.js`)
        if (closed) return
        clearTimeout(timeout); state('ticket')
        const { data } = await dahuaTicket(code)
        if (closed) { if (data?.ticket) cancelDahuaTicket(data.ticket).catch(() => {}); return }
        ticket = data.ticket
        const url = new URL(import.meta.env.VITE_APP_BASE_API + data.path, location.origin)
        url.protocol = url.protocol === 'https:' ? 'wss:' : 'ws:'
        socket = new WebSocket(url); socket.binaryType = 'arraybuffer'; state('connecting')
        timeout = setTimeout(() => fail('摄像机对讲连接超时，请检查专网、密码及设备占用'), 20000)
        socket.onopen = () => socket.send(JSON.stringify({ type: 'auth', ticket }))
        socket.onerror = () => fail('对讲连接失败，请检查WebSocket代理及服务器日志')
        socket.onclose = () => fail('对讲连接已关闭')
        socket.onmessage = event => {
          if (closed) return
          try {
            if (typeof event.data === 'string') {
              const message = JSON.parse(event.data)
              if (message.type === 'error') { fail(message.message); return }
              if (message.type === 'pong') { lastPong = Date.now(); stats.submitted = message.submittedBytes || 0; onDiagnostics?.({ ...stats }) }
              if (message.type === 'ready' && !ready) {
                ready = true; clearTimeout(timeout); lastPong = Date.now(); state('sending')
                source = context.createMediaStreamSource(stream); capture = new AudioWorkletNode(context, 'dahua-capture'); sink = context.createGain(); sink.gain.value = 0
                source.connect(capture); capture.connect(sink); sink.connect(context.destination)
                capture.port.onmessage = ({ data: pcm }) => {
                  if (closed || socket.readyState !== 1) return
                  if (socket.bufferedAmount > 3200) { fail('语音发送积压，对讲已停止'); return }
                  socket.send(pcm); stats.frames++; stats.bytes += pcm.byteLength
                  const samples = new Int16Array(pcm); let peak = 0; for (const n of samples) peak = Math.max(peak, Math.abs(n)); stats.power = Math.round(peak / 32768 * 100)
                  if (stats.frames % 12 === 0) onDiagnostics?.({ ...stats })
                }
                ping = setInterval(() => { if (Date.now() - lastPong > 15000) fail('对讲心跳超时'); else if (socket.readyState === 1) socket.send('{"type":"ping"}') }, 5000)
              }
            } else {
              const samples = decodeAudio(event.data)
              if (playback > context.currentTime + .25) { fail('回传声音积压，对讲已停止'); return }
              const buffer = context.createBuffer(1, samples.length, 8000); buffer.copyToChannel(samples, 0)
              const output = context.createBufferSource(); output.buffer = buffer; output.connect(receiveGain)
              playing.add(output); output.onended = () => { playing.delete(output); output.disconnect() }
              playback = Math.max(playback, context.currentTime + .02); output.start(playback); playback += samples.length / 8000
              stats.received += event.data.byteLength - 12; onDiagnostics?.({ ...stats })
            }
          } catch (e) { fail(e.message || '音频处理失败') }
        }
      } catch (e) {
        const messages = { NotAllowedError: '麦克风权限被拒绝，请允许后重试', NotFoundError: '未找到麦克风', NotReadableError: '麦克风无法读取或正被占用' }
        fail(messages[e.name] || e.message || '对讲启动失败')
      }
    },
  }
  current = call; window.addEventListener('pagehide', call.stop); return call
}
