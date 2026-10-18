import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs'
import { transform } from 'esbuild'
test('mock requires both development build and explicit true flag', async () => {
  const source = fs.readFileSync('src/api/fire/config.js', 'utf8')
  for (const [dev, flag, expected] of [
    [true, 'true', true],
    [false, 'true', false],
    [true, 'false', false],
    [false, 'false', false],
  ]) {
    const { code } = await transform(source, {
      format: 'esm',
      define: {
        'import.meta.env.DEV': String(dev),
        'import.meta.env.VITE_FIRE_MOCK': JSON.stringify(flag),
      },
    })
    const module = await import(
      'data:text/javascript;base64,' + Buffer.from(code).toString('base64')
    )
    assert.equal(module.isFireMock, expected)
  }
})
