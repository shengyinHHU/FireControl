import { PcmResampler } from './audio-codec.js'
class DahuaCapture extends AudioWorkletProcessor {
  constructor() { super(); this.converter = new PcmResampler(sampleRate, data => this.port.postMessage(data, [data])) }
  process(inputs) { if (inputs[0]?.[0]) this.converter.push(inputs[0][0]); return true }
}
registerProcessor('dahua-capture', DahuaCapture)
