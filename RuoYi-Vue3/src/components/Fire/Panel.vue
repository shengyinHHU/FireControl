<template>
  <section class="fire-panel">
    <header>
      <span class="panel-line"></span>
      <h2>{{ title }}</h2>
      <slot name="action" />
    </header>
    <div class="panel-body"><slot /></div>
  </section>
</template>
<script setup>
defineProps({ title: String })
</script>
