<template>
  <div class="fire-theme">
    <div class="sms-tabs">
      <el-radio-group v-model="tab"
        ><el-radio-button v-if="canFire('smsRules')" value="smsRules"
          >通知规则</el-radio-button
        ><el-radio-button v-if="canFire('sms')" value="sms"
          >发送记录</el-radio-button
        ></el-radio-group
      >
    </div>
    <ResourcePage :key="tab" :resource="tab" />
  </div>
</template>
<script setup>
import { canFire } from '@/api/fire/access'
import { ref } from 'vue'
import ResourcePage from '@/components/Fire/ResourcePage.vue'
const tab = ref(canFire('smsRules') ? 'smsRules' : 'sms')
</script>
<style scoped>
.sms-tabs {
  padding: 22px 28px 0;
}
</style>
