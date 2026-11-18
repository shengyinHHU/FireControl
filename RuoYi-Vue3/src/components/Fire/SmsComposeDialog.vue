<template>
  <el-dialog :model-value="!!smsTarget" title="发送短信" width="620px" class="fire-dialog sms-compose" append-to-body :close-on-click-modal="false" :close-on-press-escape="!sending" :show-close="!sending" :before-close="requestClose" @opened="focusBody" @closed="onClosed">
    <div v-if="loading" class="sms-loading" role="status">正在核对收件人…</div>
    <div v-if="error" class="fire-error" role="alert">{{ error }}<el-button v-if="!recipient && !sending" link @click="loadRecipient">重试</el-button></div>
    <el-form v-if="recipient" label-position="top" novalidate @submit.prevent="submit">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="收件人">{{ recipient.name || '未填写姓名' }}</el-descriptions-item>
        <el-descriptions-item label="所属场所 / 岗位">{{ recipient.context || '未填写' }}</el-descriptions-item>
        <el-descriptions-item label="登记电话">{{ recipient.rawPhone || '未填写' }}</el-descriptions-item>
      </el-descriptions>
      <el-form-item label="接收手机号" class="sms-field" :error="validation.phones">
        <el-checkbox-group v-model="selected" :disabled="sending || locked" aria-label="选择本次接收手机号" @change="changed">
          <el-checkbox v-for="phone in recipient.phones" :key="phone" :value="phone">{{ phone }}</el-checkbox>
        </el-checkbox-group>
        <span v-if="!recipient.phones.length" class="sms-muted">登记电话中没有可发送短信的手机号</span>
      </el-form-item>
      <el-form-item label="短信签名"><span>{{ recipient.signature }}（固定）</span></el-form-item>
      <el-form-item label="短信正文" required :error="validation.body">
        <el-input ref="bodyInput" v-model="body" aria-label="短信正文" :aria-invalid="!!validation.body" aria-describedby="sms-body-help" type="textarea" :rows="4" resize="none" :disabled="sending || locked" :maxlength="bodyLimit" placeholder="请输入要通知对方的内容" @input="changed" />
        <span id="sms-body-help" class="sms-muted">正文无需填写签名；含签名共 {{ fullContent.length }} / {{ recipient.maxContentLength }} 字</span>
      </el-form-item>
      <div class="sms-preview" aria-label="实际发送预览"><strong>实际发送预览</strong><p>{{ fullContent }}</p></div>
      <p class="sms-count">本次发送：{{ selected.length }} 个号码</p>
      <p v-if="!recipient.enabled" class="fire-error" role="alert">短信服务未启用，请联系管理员配置</p>
    </el-form>
    <div v-if="result" class="sms-result" role="status" aria-live="polite">
      <strong>{{ smsStatus(result.taskStatus) }}</strong>
      <p>{{ result.resultText || '请查询本次记录，核对平台受理情况。' }}</p>
      <p v-if="result.acceptedPhones">已受理号码：{{ result.acceptedPhones }}</p>
      <p v-if="result.platformTaskId">平台任务编号：{{ result.platformTaskId }}</p>
      <p v-if="['submitted', 'partial'].includes(result.taskStatus)">送达状态：尚未收到平台回执</p>
    </div>
    <template #footer>
      <el-button :disabled="sending" @click="requestClose">{{ result ? '关闭' : '取消' }}</el-button>
      <el-button v-if="requestId && (uncertain || ['pending', 'unknown'].includes(result?.taskStatus))" :loading="checking" @click="checkResult">查询提交结果</el-button>
      <el-button v-if="!locked" type="primary" :loading="sending" :disabled="loading || !recipient?.enabled || !recipient?.phones.length || checking" @click="submit">{{ result?.taskStatus === 'failed' ? '重新发送' : '发送短信' }}</el-button>
    </template>
  </el-dialog>
</template>
<script setup>
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { smsTarget, closeSmsComposer, restoreSmsFocus } from '@/api/fire/smsComposer'
import { smsRecipient, sendSms, smsRequest } from '@/api/fire/sms'
import { smsRequestId, smsStatus } from '@/api/fire/smsModel'
const recipient = ref(null), selected = ref([]), body = ref(''), bodyInput = ref(), loading = ref(false), sending = ref(false), checking = ref(false), confirming = ref(false), closing = ref(false), error = ref(''), validation = ref({}), result = ref(null), requestId = ref(''), uncertain = ref(false)
let generation = 0
const locked = computed(() => uncertain.value || (result.value && !['failed', 'rejected'].includes(result.value.taskStatus)))
const fullContent = computed(() => (recipient.value?.signature || '') + body.value)
const bodyLimit = computed(() => Math.max(1, (recipient.value?.maxContentLength || 1000) - (recipient.value?.signature?.length || 0)))
function focusBody() { if (!locked.value) bodyInput.value?.focus() }
function onClosed() { if (!smsTarget.value) restoreSmsFocus() }
function changed() { validation.value = {}; error.value = ''; result.value = null; requestId.value = '' }
async function loadRecipient() {
  const current = ++generation, target = smsTarget.value
  recipient.value = null; loading.value = true; error.value = ''; selected.value = []; body.value = ''; result.value = null; validation.value = {}; requestId.value = ''; uncertain.value = false
  try {
    const { data } = await smsRecipient(target)
    if (current !== generation || !smsTarget.value) return
    recipient.value = data; selected.value = data.phones.length === 1 ? [...data.phones] : []
    await nextTick(); focusBody()
  } catch (e) { if (current === generation) error.value = e.message || '收件人核对失败，请重试' }
  finally { if (current === generation) loading.value = false }
}
async function submit() {
  if (sending.value || confirming.value || locked.value || !recipient.value) return
  validation.value = {}
  if (!selected.value.length) validation.value.phones = '请选择本次接收手机号'
  if (!body.value.trim()) validation.value.body = '请输入短信正文'
  if (fullContent.value.length > recipient.value.maxContentLength) validation.value.body = '短信超出允许长度，请缩短正文'
  if (Object.keys(validation.value).length) { if (validation.value.body) focusBody(); return }
  if (result.value?.taskStatus === 'failed') {
    confirming.value = true
    try { await ElMessageBox.confirm('将向当前勾选号码重新提交预览中的短信，是否继续？', '重新发送短信', { confirmButtonText: '重新发送', cancelButtonText: '继续编辑', type: 'warning' }) } catch { return }
    finally { confirming.value = false }
    if (sending.value || locked.value) return
    requestId.value = ''
  }
  requestId.value ||= smsRequestId()
  const current = generation
  sending.value = true; error.value = ''
  try {
    const { data } = await sendSms({ ...smsTarget.value, expectedPhone: recipient.value.rawPhone, signature: recipient.value.signature, phones: [...selected.value], body: body.value, requestId: requestId.value })
    if (current !== generation) return
    result.value = data
    if (data.taskStatus === 'submitted') ElMessage.success('短信已提交平台')
  } catch (e) {
    if (current === generation) { uncertain.value = true; error.value = '未取得本次提交结果。请先查询提交记录，避免重复发送。' }
  } finally { if (current === generation) sending.value = false }
}
async function checkResult() {
  if (checking.value) return
  const current = generation
  checking.value = true
  try {
    const { data } = await smsRequest(requestId.value)
    if (current === generation) { result.value = data; uncertain.value = false; error.value = '' }
  } catch (e) { if (current === generation) error.value = '尚未查到可靠结果，请在发送记录或平台后台核对，不要直接重发。' }
  finally { if (current === generation) checking.value = false }
}
async function requestClose() {
  if (sending.value || checking.value || closing.value || confirming.value) return
  if (body.value.trim() && !result.value && !uncertain.value) {
    closing.value = true
    try { await ElMessageBox.confirm('关闭后将放弃当前未发送的短信正文。', '放弃未发送短信', { confirmButtonText: '放弃', cancelButtonText: '继续编辑', type: 'warning' }) } catch { return }
    finally { closing.value = false }
  }
  ++generation; closeSmsComposer()
}
watch(smsTarget, value => { if (value) loadRecipient(); else ++generation })
onBeforeUnmount(() => { ++generation; closeSmsComposer() })
</script>
<style scoped>
.sms-loading { min-height: 100px; display: grid; place-items: center; }
.sms-field { margin-top: 18px; }
.sms-muted { display: block; color: var(--fire-muted); font-size: 12px; margin-top: 6px; }
.sms-preview { border: 1px solid var(--el-border-color); border-radius: var(--el-border-radius-base); padding: 12px; }
.sms-preview p { white-space: pre-wrap; overflow-wrap: anywhere; margin-bottom: 0; }
.sms-count { color: var(--fire-muted); }
.sms-result { margin-top: 16px; border-top: 1px solid var(--el-border-color); padding-top: 12px; overflow-wrap: anywhere; }
.sms-result p { margin-bottom: 0; }
:deep(textarea) { resize: none; }
</style>
<style>
.sms-compose { margin-top: 6vh; }
.sms-compose .el-dialog__body { max-height: calc(100dvh - 210px); overflow-y: auto; scrollbar-gutter: stable; }
.sms-compose .el-dialog__footer { display: flex; flex-wrap: wrap; justify-content: flex-end; gap: 8px; }
.sms-compose .el-dialog__footer .el-button + .el-button { margin-left: 0; }
.sms-compose .el-dialog__footer .el-button--primary { min-width: 100px; }
</style>
