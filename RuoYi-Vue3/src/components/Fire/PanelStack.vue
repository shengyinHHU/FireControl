<template>
  <div ref="root" class="panel-stack" :class="{ 'panel-stack-enabled': enabled, 'panel-stack-dragging': !!drag }" :style="{gap: enabled ? '0px' : undefined}">
    <template v-for="(panel, index) in panels" :key="panel.id">
      <div :id="paneId(panel.id)" class="panel-stack-pane" :data-panel="panel.id" :style="enabled && sizes.length ? {flex: `0 0 ${sizes[index]}px`} : undefined">
        <slot :name="panel.id" />
      </div>
      <div v-if="enabled && index < panels.length - 1" class="panel-stack-handle" role="separator" tabindex="0" aria-orientation="horizontal" :aria-label="`调整${panel.label}与${panels[index + 1].label}的高度`" :aria-controls="`${paneId(panel.id)} ${paneId(panels[index + 1].id)}`" :aria-valuenow="boundaryValue(index)" :aria-valuemin="boundaryLimit(index, false)" :aria-valuemax="boundaryLimit(index, true)" :aria-valuetext="`${panel.label} ${Math.round(sizes[index] || 0)} 像素，${panels[index + 1].label} ${Math.round(sizes[index + 1] || 0)} 像素`" title="上下拖动调整高度；可用上下方向键调整，Home / End 调至最小 / 最大；双击恢复本列默认布局" @pointerdown="start($event, index)" @pointermove="move" @pointerup="endPointer($event, true)" @pointercancel="endPointer($event, false)" @lostpointercapture="endPointer($event, false)" @keydown="key($event, index)" @dblclick="reset">
        <span aria-hidden="true" />
      </div>
    </template>
  </div>
</template>
<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, useId, watch } from 'vue'
import { fitPanelSizes, movePanelBoundary, panelMinimums, readPanelShares } from '@/api/fire/panelLayout'
const props = defineProps({ panels: { type: Array, required: true }, enabled: { type: Boolean, default: true }, storageKey: { type: String, required: true }, initialSizes: { type: Function, required: true } })
const root = ref(), sizes = ref([]), available = ref(0), shares = ref(null), drag = ref(null)
const id = useId(), handleHeight = 10
const minimums = computed(() => panelMinimums(available.value, props.panels.map(panel => panel.minSize)))
const paneId = name => `panel-stack-${id}-${name}`
let observer
function restore() { try { shares.value = readPanelShares(localStorage.getItem(props.storageKey), props.panels.length) } catch { shares.value = null } }
function measure() {
  if (!props.enabled || !root.value) return
  const room = Math.max(0, root.value.clientHeight - (props.panels.length - 1) * handleHeight)
  if (drag.value && Math.abs(room - available.value) > .5) finish(false)
  available.value = room
  if (room > 0) sizes.value = fitPanelSizes(room, shares.value || props.initialSizes(room), props.panels.map(panel => panel.minSize))
}
function persist() {
  const total = sizes.value.reduce((sum, size) => sum + size, 0)
  if (!total) return
  shares.value = sizes.value.map(size => size / total)
  try { localStorage.setItem(props.storageKey, JSON.stringify({ version: 1, shares: shares.value })) } catch { /* Layout remains adjustable when browser storage is unavailable. */ }
}
function start(event, index) {
  if (event.button !== 0 || event.isPrimary === false || drag.value || !available.value) return
  event.preventDefault(); event.currentTarget.focus({ preventScroll: true })
  drag.value = { index, y: event.clientY, sizes: [...sizes.value], pointerId: event.pointerId, target: event.currentTarget }
  event.currentTarget.setPointerCapture(event.pointerId)
}
function move(event) {
  const current = drag.value
  if (!current || event.pointerId !== current.pointerId) return
  sizes.value = movePanelBoundary(current.sizes, minimums.value, current.index, event.clientY - current.y)
}
function finish(commit) {
  const current = drag.value
  if (!current) return
  drag.value = null
  if (current.target.hasPointerCapture(current.pointerId)) current.target.releasePointerCapture(current.pointerId)
  if (commit) persist()
  else { sizes.value = current.sizes; nextTick(measure) }
}
function endPointer(event, commit) { if (event.pointerId === drag.value?.pointerId) finish(commit) }
function boundaryValue(index) { return Math.round((sizes.value[index] || 0) / ((sizes.value[index] || 0) + (sizes.value[index + 1] || 0) || 1) * 100) }
function boundaryLimit(index, maximum) {
  const pair = (sizes.value[index] || 0) + (sizes.value[index + 1] || 0)
  if (!pair) return maximum ? 100 : 0
  return Math.round((maximum ? pair - minimums.value[index + 1] : minimums.value[index]) / pair * 100)
}
function key(event, index) {
  if (event.key === 'Escape' && drag.value) { event.preventDefault(); finish(false); return }
  if (drag.value || !['ArrowUp', 'ArrowDown', 'Home', 'End'].includes(event.key)) return
  event.preventDefault()
  const step = event.shiftKey ? 50 : 20
  const delta = event.key === 'Home' ? -available.value : event.key === 'End' ? available.value : event.key === 'ArrowUp' ? -step : step
  sizes.value = movePanelBoundary(sizes.value, minimums.value, index, delta); persist()
}
function reset() { finish(false); shares.value = null; try { localStorage.removeItem(props.storageKey) } catch { /* No persistent state to remove. */ } measure() }
function cancel() { finish(false) }
watch(() => props.enabled, () => { cancel(); nextTick(measure) })
onMounted(() => { restore(); measure(); observer = new ResizeObserver(measure); observer.observe(root.value); window.addEventListener('blur', cancel); document.addEventListener('visibilitychange', cancel) })
onBeforeUnmount(() => { observer?.disconnect(); cancel(); window.removeEventListener('blur', cancel); document.removeEventListener('visibilitychange', cancel) })
defineExpose({ reset })
</script>
<style scoped>
.panel-stack{display:flex;flex-direction:column;min-height:0}.panel-stack-pane{display:flex;flex-direction:column;min-height:0;min-width:0;flex:1 0 auto}.panel-stack-enabled>.panel-stack-pane{flex:1 1 0;overflow:hidden}.panel-stack-pane :deep(.fire-panel){flex:1;min-height:0;min-width:0}.panel-stack-enabled .panel-stack-handle{box-sizing:border-box;display:flex;align-items:center;justify-content:center;flex:0 0 10px;height:10px;min-height:10px;cursor:ns-resize;touch-action:none;user-select:none;position:relative;z-index:3;border-radius:2px;outline-offset:-1px}.panel-stack-handle>span{width:56px;height:3px;border-radius:2px;background:var(--fire-muted,#88a7bf);opacity:.65}.panel-stack-handle:hover,.panel-stack-handle:focus-visible,.panel-stack-dragging .panel-stack-handle{background:var(--el-fill-color-light,#153650)}.panel-stack-handle:hover>span,.panel-stack-handle:focus-visible>span,.panel-stack-dragging .panel-stack-handle>span{background:var(--fire-cyan,#22c6e8);opacity:1}.panel-stack-handle:focus-visible{outline:2px solid var(--fire-cyan,#22c6e8)}.panel-stack-dragging{user-select:none;cursor:ns-resize}
</style>
