import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs'
import vm from 'node:vm'

const source = fs.readFileSync('src/views/fire/dashboard/index.vue', 'utf8')
const handler = source.match(/async function fullscreen\(\) \{[\s\S]*?\n\}/)?.[0]
assert.ok(handler, 'dashboard fullscreen handler must exist')

test('fullscreen includes body-mounted dialogs and exits on the next toggle', async () => {
  const calls = []
  const document = {
    fullscreenElement: null,
    documentElement: {
      async requestFullscreen() {
        calls.push('page')
        document.fullscreenElement = this
      },
    },
    async exitFullscreen() {
      calls.push('exit')
      document.fullscreenElement = null
    },
  }
  const context = vm.createContext({
    document,
    screen: { value: { requestFullscreen: () => calls.push('dashboard-only') } },
    ElMessage: { info: () => calls.push('error') },
  })
  vm.runInContext(handler, context)
  await context.fullscreen()
  await context.fullscreen()
  assert.deepEqual(calls, ['page', 'exit'])
})

test('fullscreen rejection displays the keyboard fallback', async () => {
  const messages = []
  const context = vm.createContext({
    document: {
      fullscreenElement: null,
      documentElement: { requestFullscreen: async () => { throw new Error('denied') } },
    },
    ElMessage: { info: (message) => messages.push(message) },
  })
  vm.runInContext(handler, context)
  await context.fullscreen()
  assert.deepEqual(messages, ['浏览器未允许全屏，请使用 F11'])
})
