import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import vm from 'node:vm'
const helper = readFileSync(new URL('../../src/utils/firePlatformCall.js', import.meta.url), 'utf8').replace('export function', 'function')
const component = readFileSync(new URL('../../src/components/Fire/ContactDialLink.vue', import.meta.url), 'utf8')
const handler = component.slice(component.indexOf('async function call()'), component.indexOf('</script>'))
function fixture(phone = '18052305235') {
  const destinations = [], errors = []
  const context = vm.createContext({ URL, props: { name: '张宇航', phone }, busy: { value: false }, window: { location: { assign: url => destinations.push(url) } }, ElMessage: { error: message => errors.push(message) } })
  vm.runInContext(helper + '\n' + handler, context)
  return { context, destinations, errors }
}
test('跳转原平台自动拨号入口，不创建本地会议', async () => {
  const { context, destinations } = fixture()
  await context.call()
  assert.equal(destinations[0], 'https://www.jnrhtx.top/meeting/index?from=Dc&phone=18052305235')
})
test('使用点击记录的号码并规范化国家码', async () => {
  const { context, destinations } = fixture('+86 180-5230-5235')
  await context.call()
  assert.equal(new URL(destinations[0]).searchParams.get('phone'), '18052305235')
})
test('无效或多个号码不跳转并恢复按钮', async () => {
  const { context, destinations, errors } = fixture('18052305235,18052305236')
  await context.call()
  assert.equal(destinations.length, 0); assert.equal(errors.length, 1); assert.equal(context.busy.value, false)
})
test('重复点击不会重复跳转', async () => {
  const { context, destinations } = fixture()
  await context.call(); await context.call()
  assert.equal(destinations.length, 1)
})
