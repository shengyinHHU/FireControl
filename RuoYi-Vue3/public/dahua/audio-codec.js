// Continuous phase across worklet blocks avoids drift at 44.1 kHz.
export class PcmResampler {
  constructor(rate, emit) { this.step = rate / 8000; this.emit = emit; this.tail = []; this.pos = 0; this.packet = [] }
  push(input) {
    const samples = [...this.tail, ...input]
    while (this.pos + 1 < samples.length) {
      const i = Math.floor(this.pos), f = this.pos - i
      this.packet.push(Math.round(Math.max(-1, Math.min(1, samples[i] * (1 - f) + samples[i + 1] * f)) * 32767))
      this.pos += this.step
      if (this.packet.length === 320) {
        const data = new ArrayBuffer(640), view = new DataView(data)
        this.packet.forEach((n, j) => view.setInt16(j * 2, n, true))
        this.emit(data); this.packet = []
      }
    }
    const used = Math.min(Math.floor(this.pos), samples.length - 1)
    this.tail = samples.slice(used); this.pos -= used
  }
}
export function decodeAudio(buffer) {
  if (buffer.byteLength < 13) throw Error('无效的回传音频')
  const view = new DataView(buffer), codec = view.getInt32(0, true), rate = view.getInt32(4, true), bits = view.getInt32(8, true)
  if (rate !== 8000 || ![0, 1, 2, 4].includes(codec) || (codec <= 1 && (bits !== 16 || (buffer.byteLength - 12) % 2))) throw Error('摄像机回传音频格式暂不支持')
  const pcm = codec <= 1, count = (buffer.byteLength - 12) / (pcm ? 2 : 1), result = new Float32Array(count)
  for (let i = 0; i < count; i++) {
    if (pcm) { result[i] = view.getInt16(12 + i * 2, true) / 32768; continue }
    let a = view.getUint8(12 + i), n
    if (codec === 2) { a ^= 0x55; const seg = (a & 0x70) >> 4; n = (a & 15) << 4; n += seg ? 0x108 : 8; if (seg > 1) n <<= seg - 1; n = a & 0x80 ? n : -n }
    else { a = (~a) & 255; n = (((a & 15) << 3) + 0x84) << ((a >> 4) & 7); n = a & 0x80 ? 0x84 - n : n - 0x84 }
    result[i] = n / 32768
  }
  return result
}
