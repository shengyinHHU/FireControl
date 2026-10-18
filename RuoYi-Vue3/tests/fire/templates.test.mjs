import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import { parse, compileTemplate } from '@vue/compiler-sfc'
function files(dir) {
  return fs
    .readdirSync(dir, { withFileTypes: true })
    .flatMap((e) =>
      e.isDirectory()
        ? files(path.join(dir, e.name))
        : e.name.endsWith('.vue')
          ? [path.join(dir, e.name)]
          : [],
    )
}
test('every fire Vue template compiles including inline event handlers', () => {
  const failures = []
  for (const file of [
    ...files('src/views/fire'),
    ...files('src/components/Fire'),
  ]) {
    const { descriptor, errors } = parse(fs.readFileSync(file, 'utf8'), {
      filename: file,
    })
    if (descriptor.template)
      errors.push(
        ...compileTemplate({
          source: descriptor.template.content,
          filename: file,
          id: file,
        }).errors,
      )
    if (errors.length)
      failures.push(file + ': ' + errors.map((e) => e.message || e).join('; '))
  }
  assert.deepEqual(failures, [])
})
