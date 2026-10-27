// G.711 is not supported by MSE. Decode only this audio track through Web Audio;
// mpegts.js keeps ownership of the original video and its media timeline.
export function decodeG711(bytes, format) {
  const out = new Float32Array(bytes.length)
  for (let i = 0; i < bytes.length; i++) {
    let value = bytes[i], sample
    if (format === 7) {
      value ^= 0x55
      const segment = (value & 0x70) >> 4
      sample = (value & 15) << 4
      sample += segment === 0 ? 8 : 0x108
      if (segment > 1) sample <<= segment - 1
      if (!(value & 0x80)) sample = -sample
    } else if (format === 8) {
      value = (~value) & 255
      sample = (((value & 15) << 3) + 132) << ((value & 0x70) >> 4)
      sample = value & 0x80 ? 132 - sample : sample - 132
    } else throw Error('不支持的G.711音频格式')
    out[i] = sample / 32768
  }
  return out
}

// Incremental FLV parser: TCP/fetch chunk boundaries are unrelated to FLV tags.
export class FlvAudioParser {
  constructor(onAudio, onVideoBase) {
    this.audio = onAudio
    this.videoBase = onVideoBase
    this.pending = new Uint8Array(0)
    this.header = false
    this.base = false
  }
  push(chunk) {
    const input = new Uint8Array(this.pending.length + chunk.length)
    input.set(this.pending); input.set(chunk, this.pending.length)
    let offset = 0
    if (!this.header) {
      if (input.length < 9) { this.pending = input; return }
      if (input[0] !== 70 || input[1] !== 76 || input[2] !== 86) throw Error('无效FLV数据')
      const size = new DataView(input.buffer).getUint32(5)
      if (size < 9 || size > 1024) throw Error('无效FLV文件头')
      if (input.length < size + 4) { this.pending = input; return }
      offset = size + 4; this.header = true
    }
    while (offset + 11 <= input.length) {
      const size = (input[offset + 1] << 16) | (input[offset + 2] << 8) | input[offset + 3]
      if (size > 8 * 1024 * 1024) throw Error('FLV数据包过大')
      if (offset + size + 15 > input.length) break
      const time = (input[offset + 7] * 0x1000000) + (input[offset + 4] << 16) + (input[offset + 5] << 8) + input[offset + 6]
      const type = input[offset] & 31, start = offset + 11
      if (type === 9 && size > 1 && !this.base && input[start + 1] === 1) {
        this.base = true; this.videoBase(time)
      }
      if (type === 8 && size > 1) {
        const format = input[start] >> 4
        if (format === 7 || format === 8)
          this.audio({ time, format, channels: (input[start] & 1) + 1, samples: decodeG711(input.subarray(start + 1, start + size), format) })
      }
      offset += size + 15
    }
    this.pending = input.slice(offset)
  }
}

export function createG711Audio(video, onAvailable) {
  let context, gain, base, closed = false
  let queue = []
  const sources = new Set()
  const halt = () => { for (const source of sources) { try { source.stop() } catch {} } sources.clear() }
  const pump = () => {
    if (!context || context.state !== 'running' || video.paused || video.seeking || video.readyState < 3 || base === undefined) { halt(); return }
    gain.gain.value = video.muted ? 0 : video.volume
    const rate = video.playbackRate || 1
    while (queue.length) {
      const packet = queue[0], time = (packet.time - base) / 1000
      const duration = packet.samples.length / packet.channels / 8000
      if (time > video.currentTime + 0.15) break
      queue.shift()
      const late = Math.max(0, video.currentTime - time)
      if (late >= duration) continue
      const buffer = context.createBuffer(packet.channels, packet.samples.length / packet.channels, 8000)
      for (let channel = 0; channel < packet.channels; channel++) {
        const output = buffer.getChannelData(channel)
        for (let i = 0; i < output.length; i++) output[i] = packet.samples[i * packet.channels + channel]
      }
      const source = context.createBufferSource()
      source.buffer = buffer; source.playbackRate.value = rate; source.connect(gain)
      source.onended = () => { sources.delete(source); source.disconnect() }
      sources.add(source)
      source.start(context.currentTime + Math.max(0, time - video.currentTime) / rate, late)
    }
  }
  const parser = new FlvAudioParser(packet => {
    if (closed) return
    queue.push(packet)
    // Bound memory even while muted/paused or before the first video frame.
    if (queue.length > 400) queue.shift()
    onAvailable()
  }, time => { base = time })
  const timer = setInterval(pump, 20)
  video.addEventListener('pause', halt)
  video.addEventListener('waiting', halt)
  video.addEventListener('seeking', halt)
  return {
    feed: chunk => { if (!closed) parser.push(chunk) },
    async toggle() {
      if (closed) return false
      if (!context) {
        const Audio = window.AudioContext || window.webkitAudioContext
        if (!Audio) throw Error('当前浏览器不支持音频播放')
        context = new Audio(); gain = context.createGain(); gain.connect(context.destination)
      }
      // Invoked directly by the sound button's user gesture, never by autoplay.
      await context.resume()
      if (closed) return false
      video.muted = !video.muted
      gain.gain.value = video.muted ? 0 : video.volume
      pump()
      return !video.muted
    },
    close() {
      closed = true; clearInterval(timer); halt(); queue = []
      video.removeEventListener('pause', halt); video.removeEventListener('waiting', halt); video.removeEventListener('seeking', halt)
      gain?.disconnect(); context?.close().catch(() => {})
    },
  }
}

// One authenticated fetch feeds both parsers. No second stream, credentials in URL,
// background microphone, external player, or unbounded whole-stream buffer.
export function createG711Loader(mpegts, audio) {
  return class extends mpegts.BaseLoader {
    constructor(seekHandler, config) { super('g711-fetch'); this.config = config; this.controller = null; this.reader = null; this.stopped = false }
    open(source) {
      this.stopped = false; this.controller = new AbortController(); this._status = mpegts.LoaderStatus.kConnecting
      this.read(source).catch(() => {
        if (!this.stopped) { this._status = mpegts.LoaderStatus.kError; this._onError?.(mpegts.LoaderErrors.EXCEPTION, { code: -1, msg: 'FLV音视频连接中断' }) }
      })
    }
    async read(source) {
      const response = await fetch(source.url, { headers: this.config.headers, signal: this.controller.signal, cache: 'no-store', redirect: 'error' })
      if (!response.ok) { this._status = mpegts.LoaderStatus.kError; this._onError?.(mpegts.LoaderErrors.HTTP_STATUS_CODE_INVALID, { code: response.status, msg: 'FLV连接失败' }); return }
      this.reader = response.body.getReader(); this._status = mpegts.LoaderStatus.kBuffering
      let received = 0
      try {
        while (!this.stopped) {
          const { done, value } = await this.reader.read()
          if (done) break
          audio.feed(value)
          const start = received; received += value.byteLength
          const chunk = value.buffer.slice(value.byteOffset, value.byteOffset + value.byteLength)
          this._onDataArrival?.(chunk, start, received)
        }
        if (!this.stopped) { this._status = mpegts.LoaderStatus.kComplete; this._onComplete?.(0, received - 1) }
      } finally { this.reader.releaseLock(); this.reader = null }
    }
    abort() { this.stopped = true; this.controller?.abort(); this.reader?.cancel().catch(() => {}); this._status = mpegts.LoaderStatus.kIdle }
    destroy() { this.abort(); super.destroy() }
  }
}
