<template>
  <div class="alarm-annunciator">
    <button type="button" :aria-pressed="soundEnabled && soundReady" @click="toggleSound">
      {{ soundEnabled && soundReady ? '关闭报警声音' : '开启报警声音' }}
    </button>
    <span v-if="active" class="alarm-signal-status" role="status">新警情报警中（{{ count }} 条）</span>
    <button v-if="active" type="button" class="alarm-signal-stop" @click="$emit('stop')">停止本次报警</button>
    <span v-if="message" class="alarm-sound-error" role="status">{{ message }}</span>
  </div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import alarmAudioUrl from '@/assets/audio/firealarm.wav'

const props = defineProps({ active: Boolean, count: Number, sequence: Number })
defineEmits(['stop'])
const soundEnabled = ref(true), soundReady = ref(false), message = ref('')
let audio, previewTimer, generation = 0, playing = false, disposed = false

function stopSound() {
  generation++
  playing = false
  clearTimeout(previewTimer)
  if (audio) { audio.pause(); audio.currentTime = 0 }
}

async function playSound(preview = false) {
  if (!audio || disposed || playing) return
  clearTimeout(previewTimer)
  const current = ++generation
  playing = true
  audio.loop = !preview
  message.value = ''
  try {
    if (audio.error) audio.load()
    await audio.play()
    if (disposed || current !== generation) return
    soundReady.value = true
    // A short preview confirms sound permission without leaving the siren running.
    if (preview) previewTimer = setTimeout(stopSound, 1500)
  } catch (error) {
    if (disposed || current !== generation) return
    playing = false
    soundReady.value = false
    message.value = error.name === 'NotAllowedError'
      ? '浏览器已拦截报警声音，请点击“开启报警声音”'
      : '报警声音播放失败，请点击“开启报警声音”重试'
  }
}

function toggleSound() {
  if (soundEnabled.value && soundReady.value) {
    soundEnabled.value = false
    message.value = ''
    stopSound()
  } else {
    soundEnabled.value = true
    // Call play directly in the gesture so browser sound permission can be granted.
    playSound(!props.active)
  }
}

watch([() => props.active, () => props.sequence], ([active]) => {
  // Promote an in-progress preview to the alarm loop when a new incident arrives.
  if (active && soundEnabled.value) {
    clearTimeout(previewTimer)
    if (audio) audio.loop = true
    playSound()
  } else {
    stopSound()
    message.value = ''
  }
})

onMounted(() => {
  audio = new Audio(alarmAudioUrl)
  audio.preload = 'auto'
  audio.onerror = () => {
    if (disposed || !playing) return
    stopSound()
    soundReady.value = false
    message.value = '报警声音加载失败，请点击“开启报警声音”重试'
  }
})
onBeforeUnmount(() => {
  disposed = true
  stopSound()
  if (audio) { audio.onerror = null; audio.removeAttribute('src'); audio.load() }
})
</script>

<style scoped>
.alarm-annunciator { display: flex; align-items: center; justify-content: flex-end; flex-wrap: wrap; gap: 6px 10px; }
.alarm-annunciator button { border: 0; background: none; color: var(--fire-muted); padding: 2px; font: inherit; cursor: pointer; }
.alarm-annunciator button:hover { color: var(--fire-text); }
.alarm-annunciator button:active { opacity: .75; }
.alarm-annunciator button:focus-visible { outline: 2px solid var(--fire-cyan); outline-offset: 3px; }
.alarm-signal-status, .alarm-sound-error, .alarm-annunciator .alarm-signal-stop { color: #ffb49c; }
.alarm-sound-error { flex-basis: 100%; line-height: 1.5; }
</style>
