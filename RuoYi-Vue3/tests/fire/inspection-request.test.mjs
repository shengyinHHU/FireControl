import test from 'node:test'
import assert from 'node:assert/strict'
import { webcrypto } from 'node:crypto'
import { inspectionRequestId } from '../../src/utils/fireInspectionRequest.js'
test('inspection submissions work with insecure-context crypto without randomUUID', () => {
 const cryptoApi = { getRandomValues: data => webcrypto.getRandomValues(data) }
 const ids = Array.from({length:100}, () => inspectionRequestId(cryptoApi))
 assert.equal(new Set(ids).size,100)
 for(const id of ids) assert.match(id,/^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/)
})
