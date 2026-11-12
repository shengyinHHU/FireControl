<template>
  <div class="fire-theme">
    <div class="sms-tabs">
      <el-radio-group v-model="tab" @change="changeTab"
        ><el-radio-button v-if="canFire('smsRules')" value="smsRules"
          >通知规则</el-radio-button
        ><el-radio-button v-if="canFire('sms')" value="sms"
          >发送记录</el-radio-button
        ></el-radio-group
      >
    </div>
    <SmsRecords v-if="tab === 'sms'" />
    <ResourcePage v-else :key="tab" :resource="tab" />
  </div>
</template>
<script setup>
import { canFire } from '@/api/fire/access'
import { ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import ResourcePage from '@/components/Fire/ResourcePage.vue'
import SmsRecords from '@/components/Fire/SmsRecords.vue'
const route = useRoute(), router = useRouter()
const tab = ref(canFire('sms') ? 'sms' : 'smsRules')
function changeTab() { router.replace({ query: tab.value === 'smsRules' ? { tab: 'rules' } : {} }) }
watch(() => route.query.tab, value => { tab.value = value === 'rules' && canFire('smsRules') ? 'smsRules' : canFire('sms') ? 'sms' : 'smsRules' }, { immediate: true })
</script>
<style scoped>
.sms-tabs {
  padding: 22px 28px 0;
}
</style>
