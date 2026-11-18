import test from 'node:test'
import assert from 'node:assert/strict'
import { smsPhones, smsRequestId, smsStatus } from '../../src/api/fire/smsModel.js'
import { build } from 'esbuild'
import { usesDatabase, canWriteResource } from '../../src/api/fire/sourcePolicy.js'

test('SMS recipient normalization preserves only explicit mobile numbers, and deduplicates', () => {
  assert.deepEqual(smsPhones('025-12345678 转801，+8613800000001 /13900000002;13800000001'), ['13800000001', '13900000002'])
  assert.deepEqual(smsPhones('未填写 / 02512345678 / 12345678901'), [])
  assert.deepEqual(smsPhones(), [])
})
test('request IDs work in HTTP intranet contexts and remain valid UUIDs', () => {
  const previous = Object.getOwnPropertyDescriptor(globalThis, 'crypto')
  try {
    Object.defineProperty(globalThis, 'crypto', { configurable: true, value: undefined })
    const values = Array.from({ length: 100 }, () => smsRequestId())
    assert.equal(new Set(values).size, values.length)
    for (const value of values) assert.match(value, /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/)
  } finally { if (previous) Object.defineProperty(globalThis, 'crypto', previous); else delete globalThis.crypto }
})
test('submission statuses do not claim delivery', () => {
  assert.equal(smsStatus('submitted'), '已提交平台')
  assert.equal(smsStatus('partial'), '部分号码已提交')
  assert.equal(smsStatus('unknown'), '提交结果待确认')
  assert.equal(smsStatus('rejected'), '未提交')
  assert.equal(smsStatus('unexpected'), '状态待确认')
})

test('SMS reads always use real transport and propagate failures even in firemock mode', async () => {
  assert.equal(usesDatabase('sms'), true)
  assert.equal(canWriteResource('sms'), false)
  const plugin = { name: 'sms-real-source', setup(builder) {
    builder.onResolve({ filter: /^(?:@\/utils\/request|@\/utils\/auth|@\/plugins\/auth|@\/mock\/fire\/repository|\.\/config)$/ }, args => ({ path: args.path, namespace: 'fixture' }))
    builder.onLoad({ filter: /.*/, namespace: 'fixture' }, args => ({ contents:
      args.path === './config' ? 'export const isFireMock = true' :
      args.path === '@/plugins/auth' ? 'export default { hasPermi: () => true }' :
      args.path === '@/utils/auth' ? 'export const getToken = () => "fixture-only"' :
      args.path === '@/mock/fire/repository' ? 'export function createRepository(){ throw Error("Demo access forbidden") }' :
      'export async function download() {} export default async function request(config) { if(globalThis.__smsOffline) throw Error("SMS backend unavailable"); if(config.url !== "/fire/sms/list") throw Error("Unexpected URL"); return {rows:[{recordId:"100",taskStatus:"submitted"}],total:1} }'
    }))
  } }
  const bundle = await build({ entryPoints: ['src/api/fire/index.js'], bundle: true, write: false, format: 'esm', platform: 'node', plugins: [plugin] })
  const { fire } = await import('data:text/javascript;base64,' + Buffer.from(bundle.outputFiles[0].text).toString('base64'))
  assert.equal((await fire.list('sms')).rows[0].recordId, '100')
  globalThis.__smsOffline = true
  try { await assert.rejects(fire.list('sms'), /SMS backend unavailable/) } finally { delete globalThis.__smsOffline }
  await assert.rejects(fire.create('sms', { content: 'fake' }), /真实数据库/)
})
