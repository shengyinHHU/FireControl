import test from 'node:test'
import assert from 'node:assert/strict'
import { fitPanelSizes, movePanelBoundary, panelMinimums, readPanelShares } from '../../src/api/fire/panelLayout.js'

const near = (actual, expected) => assert.ok(Math.abs(actual - expected) < .00001, `${actual} != ${expected}`)
test('layout distributes space proportionally and preserves each minimum', () => {
  assert.deepEqual(fitPanelSizes(800, [100, 400, 300], [100, 150, 240]), [100, 400, 300])
  const sizes = fitPanelSizes(600, [.05, .75, .2], [106, 150, 240])
  near(sizes.reduce((a, b) => a + b), 600)
  assert.deepEqual(sizes, [106, 254, 240])
  assert.deepEqual(fitPanelSizes(600, [], [100, 150, 240]), [180, 180, 240])
})
test('undersized viewport scales minimums and never overflows the column', () => {
  const minimums = panelMinimums(300, [100, 150, 250])
  assert.deepEqual(minimums, [60, 90, 150])
  const sizes = fitPanelSizes(300, [1, 100, 2], [100, 150, 250])
  sizes.forEach((size, i) => near(size, minimums[i]))
  near(sizes.reduce((a, b) => a + b), 300)
  assert.deepEqual(fitPanelSizes(0, [1, 2], [100, 200]), [0, 0])
})
test('dragging moves only the adjacent pair and clamps either direction', () => {
  const sizes = [106, 400, 300], mins = [106, 150, 240]
  assert.deepEqual(movePanelBoundary(sizes, mins, 1, -120), [106, 280, 420])
  assert.deepEqual(movePanelBoundary(sizes, mins, 1, -9999), [106, 150, 550])
  assert.deepEqual(movePanelBoundary(sizes, mins, 1, 9999), [106, 460, 240])
  assert.deepEqual(movePanelBoundary(sizes, mins, -1, 20), sizes)
  assert.deepEqual(movePanelBoundary(sizes, mins, 0, NaN), sizes)
  assert.deepEqual(sizes, [106, 400, 300])
})
test('only valid versioned layout preferences can be restored', () => {
  assert.deepEqual(readPanelShares('{"version":1,"shares":[0.2,0.3,0.5]}', 3), [.2, .3, .5])
  for (const value of [null, '', '{', '{}', '{"version":2,"shares":[0.5,0.5]}', '{"version":1,"shares":[0.5]}', '{"version":1,"shares":[0,1]}', '{"version":1,"shares":[-0.5,1.5]}', '{"version":1,"shares":["0.5",0.5]}', '{"version":1,"shares":[0.6,0.6]}']) assert.equal(readPanelShares(value, 2), null)
})
