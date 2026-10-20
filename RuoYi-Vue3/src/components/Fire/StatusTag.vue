<template>
  <span class="fire-status" :class="tone">{{ value ?? '—' }}</span>
</template>
<script setup>
import { computed } from 'vue'
const props = defineProps({ value: [String, Boolean, Number] })
const tone = computed(() =>
  /离线|脱岗|紧急|较高|故障|火警/.test(props.value)
    ? 'danger'
    : /待|未读|整改中|处理中|执行中/.test(props.value)
      ? 'warning'
      : /在线|在岗|已|低|true/.test(props.value)
        ? 'success'
        : 'info',
)
</script>
