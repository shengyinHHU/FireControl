<template>
  <div class="inspection-contact">
<<<<<<< HEAD
    <div class="contact-line"><span>联系人：{{ name || '未填写' }}</span><ContactDialLink v-if="phone && phone.trim()" :name="name" :phone="phone" /></div>
=======
    <div class="contact-line"><span>联系人：{{ name || '未填写' }}</span><ContactActions v-if="phone && phone.trim()" :name="name" :phone="phone" :source="{ type: 'inspectionPost', id: postId }" :unavailable-reason="inactive ? '岗位已停用' : ''" /></div>
>>>>>>> 8d2b32494d80c8ded538436c8b2260f8aad513f1
    <div class="contact-phone">联系电话：{{ phone || '未填写' }}</div>
  </div>
</template>
<script setup>
import ContactActions from './ContactActions.vue'
defineProps({ name: String, phone: String, postId: [String, Number], inactive: Boolean })
</script>
<style scoped>
.inspection-contact { font-size: 13px; line-height: 1.6; overflow-wrap: anywhere; }
.contact-line { display: flex; align-items: center; flex-wrap: wrap; gap: 4px 16px; }
.contact-phone { color: var(--fire-muted); }
</style>
