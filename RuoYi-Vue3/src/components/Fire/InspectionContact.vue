<template>
  <div class="inspection-contact">
    <div class="contact-line"><span>联系人：{{ name || '未填写' }}</span><ContactDialLink v-if="phone && phone.trim()" :name="name" /></div>
    <div class="contact-phone">联系电话：{{ phone || '未填写' }}</div>
  </div>
</template>
<script setup>
import ContactDialLink from './ContactDialLink.vue'
defineProps({ name: String, phone: String })
</script>
<style scoped>
.inspection-contact { font-size: 13px; line-height: 1.6; overflow-wrap: anywhere; }
.contact-line { display: flex; align-items: center; flex-wrap: wrap; gap: 4px 16px; }
.contact-phone { color: var(--fire-muted); }
</style>
