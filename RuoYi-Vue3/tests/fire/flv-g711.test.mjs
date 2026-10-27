import test from 'node:test'
import assert from 'node:assert/strict'
import { decodeG711, FlvAudioParser } from '../../src/api/fire/flvG711.js'
function tag(type, time, payload) {
 const out=Buffer.alloc(payload.length+15)
 out[0]=type;out.writeUIntBE(payload.length,1,3);out.writeUIntBE(time&0xffffff,4,3);out[7]=time>>>24
 out.set(payload,11);out.writeUInt32BE(payload.length+11,out.length-4);return out
}
const header=Buffer.from([70,76,86,1,1,0,0,0,9,0,0,0,0])
test('G.711 A-law and mu-law known decode vectors retain signs and amplitudes',()=>{
 assert.deepEqual(Array.from(decodeG711(Uint8Array.from([0xd5,0x55,0xaa,0x2a]),7)),[8,-8,32256,-32256].map(v=>v/32768))
 assert.deepEqual(Array.from(decodeG711(Uint8Array.from([0xff,0x7f,0x80,0]),8)),[0,0,32124,-32124].map(v=>v/32768))
})
test('split FLV tags decode G.711 even if vendor header omits audio flag; retain timestamps',()=>{
 const packets=[],bases=[];const parser=new FlvAudioParser(p=>packets.push(p),t=>bases.push(t))
 const data=Buffer.concat([header,tag(9,1000,Buffer.from([0x1c,1,0,0,0])),tag(8,1040,Buffer.from([0x72,0xd5,0x55])),tag(8,1080,Buffer.from([0x82,0xff,0]))])
 for(let i=0;i<data.length;i++)parser.push(data.subarray(i,i+1))
 assert.deepEqual(bases,[1000]);assert.deepEqual(packets.map(p=>[p.time,p.format,p.channels]),[[1040,7,1],[1080,8,1]])
 assert.equal(packets[0].samples.length,2);assert.equal(parser.pending.length,0)
})
test('AAC remains owned by the native player; malformed lengths are bounded',()=>{
 const packets=[];const parser=new FlvAudioParser(p=>packets.push(p),()=>{})
 parser.push(Buffer.concat([header,tag(8,0,Buffer.from([0xaf,0,0x12,0x10]))]));assert.equal(packets.length,0)
 const invalid=Buffer.alloc(11);invalid[0]=8;invalid[1]=0xff
 assert.throws(()=>parser.push(invalid),/数据包过大/)
})
