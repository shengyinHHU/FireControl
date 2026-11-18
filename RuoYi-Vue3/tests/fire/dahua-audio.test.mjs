import test from 'node:test'
import assert from 'node:assert/strict'
import { PcmResampler, decodeAudio } from '../../public/dahua/audio-codec.js'
test('44.1k and 48k continuous resampling keep a minute of audio within one output sample', () => {
  for (const rate of [44100, 48000]) {
    let count = 0, nonzero = false
    const converter = new PcmResampler(rate, data => { assert.equal(data.byteLength, 640); count += 320; nonzero ||= new Int16Array(data).some(n => n !== 0) })
    for (let offset = 0; offset < rate * 60; offset += 128) {
      const input = Float32Array.from({ length: Math.min(128, rate * 60 - offset) }, (_, i) => Math.sin((offset + i) * 2 * Math.PI * 440 / rate) * .5)
      converter.push(input)
    }
    assert.ok(Math.abs(count + converter.packet.length - 480000) <= 1)
    assert.ok(nonzero)
  }
})
function frame(codec, payload, bits = 16, rate = 8000) {
  const bytes = new Uint8Array(12 + payload.length), view = new DataView(bytes.buffer)
  view.setInt32(0, codec, true); view.setInt32(4, rate, true); view.setInt32(8, bits, true); bytes.set(payload, 12); return bytes.buffer
}
test('downlink PCM little endian and G.711 standard silence/sign vectors', () => {
  assert.deepEqual([...decodeAudio(frame(1, [0, 128, 0, 0, 255, 127]))], [-1, 0, 32767 / 32768])
  assert.deepEqual([...decodeAudio(frame(4, [255, 127, 128, 0]))], [0, 0, 32124 / 32768, -32124 / 32768])
  assert.deepEqual([...decodeAudio(frame(2, [213, 85]))], [8 / 32768, -8 / 32768])
  assert.throws(() => decodeAudio(frame(8, [1])))
  assert.throws(() => decodeAudio(frame(1, [1])))
  assert.throws(() => decodeAudio(frame(1, [0, 0], 16, 16000)))
})
