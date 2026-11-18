<template>
  <span class="contact-actions" @click.stop @dblclick.stop>
    <ContactDialLink :name="name" />
    <el-tooltip v-if="canSms()" :content="reason || '发送短信'">
      <span class="sms-trigger-wrap">
        <el-button link type="primary" :disabled="!!reason" :aria-label="`${name || '联系人'}的发送短信${reason ? '：' + reason : ''}`" @click.stop="openSmsComposer(source, $event.currentTarget)">
          <el-icon aria-hidden="true"><Message /></el-icon><span>短信</span>
        </el-button>
      </span>
    </el-tooltip>
  </span>
</template>
<script setup>
import { computed } from 'vue'
import { Message } from '@element-plus/icons-vue'
import ContactDialLink from './ContactDialLink.vue'
import { canSms } from '@/api/fire/sms'
import { smsPhones } from '@/api/fire/smsModel'
import { openSmsComposer } from '@/api/fire/smsComposer'
const props = defineProps({ name: String, phone: String, source: Object, unavailableReason: String })
const reason = computed(() => props.unavailableReason || (!props.source?.id ? '缺少联系人编号' : !props.phone?.trim() ? '未填写联系电话' : !smsPhones(props.phone).length ? '没有可发送短信的手机号' : ''))
</script>
<style scoped>
.contact-actions { display: inline-flex; align-items: center; flex-wrap: wrap; gap: 12px; }
.sms-trigger-wrap { display: inline-flex; }
.contact-actions .el-button { gap: 4px; min-height: 28px; }
</style>
