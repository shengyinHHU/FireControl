import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import vm from 'node:vm'

const source = readFileSync(new URL('../../src/vendor/contact-livekit/index.js', import.meta.url), 'utf8')
const joinSource = source.slice(source.indexOf('  async joinRoom('), source.indexOf('  // 呼出')).replaceAll('this.#', 'this.')
function joinFixture(failure) {
  class Room {
    state = 'connected'
    remoteParticipants = new Map()
    localParticipant = { isMicrophoneEnabled: false, setCameraEnabled: async () => {}, setMicrophoneEnabled: async () => {
      if (failure === 'microphone') throw new Error('Permission denied')
      this.localParticipant.isMicrophoneEnabled = true
    } }
    async prepareConnection() {}
    async connect() { if (failure === 'connection') throw new Error('WebSocket connection failed') }
    getActiveDevice() { return 'default' }
  }
  const join = vm.runInNewContext('({' + joinSource + '}).joinRoom', { LiveKit: { Room } })
  const client = { roomMode: 'auto', emit() {}, getToken: async () => ({ roomUrl: 'wss://example.invalid', token: 'test' }), initEvents() {}, getSdkCallNum() {}, participantConnected() {} }
  return () => join.call(client, { roomNum: 'test', cameraStatus: false, microphoneStatus: true })
}
test('入会连接失败保留真实原因，不能返回空值掩盖错误', async () => {
  await assert.rejects(joinFixture('connection')(), /会议连接失败.*WebSocket connection failed/)
})
test('设备失败与连接失败可区分', async () => {
  await assert.rejects(joinFixture('microphone')(), /媒体设备启用失败.*Permission denied/)
})
test('音视频模式关闭摄像头仍可完成语音入会', async () => {
  const result = await joinFixture()()
  assert.equal(result.audioInputDevice, 'default')
})
const context = vm.createContext({ EventEmitter: class { emit() {} }, console })
vm.runInContext(source.replace(/^import .*;\r?$/gm, '').replace('export { LiveClient, LiveKit };', 'globalThis.Client = LiveClient;'), context)
function fixture() {
  const sent = [], handlers = new Map()
  const client = Object.create(context.Client.prototype)
  client.roomNum = 'test-room'
  client.socket = { send: message => sent.push(message), addMessageHandler: (name, handler) => handlers.set(name, handler) }
  return { client, sent, reply: (cmd, code = 200) => handlers.get(cmd)(JSON.stringify({ cmd, data: { code, result: 'result' } })) }
}
test('停止拉流使用本次流身份，不使用 demo 固定身份', async () => {
  const { client, sent, reply } = fixture()
  const pending = client.stopPullStream('stream-current')
  assert.equal(sent[0].data.identity, 'stream-current')
  assert.equal(sent[0].data.room, 'test-room')
  reply('StopInputStream'); await pending
})
test('推流使用配置地址，不向 demo 内网地址发送', async () => {
  const { client, sent, reply } = fixture()
  const pending = client.pushVideoStream('rtmps://example.invalid/live/test')
  assert.equal(sent[0].data.url, 'rtmps://example.invalid/live/test')
  reply('StartOutputStream'); assert.equal(await pending, 'result')
})
test('录制必须等待服务端成功响应，失败会拒绝', async () => {
  const { client, reply } = fixture()
  let settled = false
  const pending = client.startRecord().then(() => { settled = true })
  await Promise.resolve(); assert.equal(settled, false)
  reply('StartRecording'); await pending; assert.equal(settled, true)
  const stopped = client.stopRecord()
  reply('StopRecording', 500)
  await assert.rejects(stopped)
})
