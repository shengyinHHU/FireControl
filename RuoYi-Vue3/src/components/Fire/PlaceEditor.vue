<template>
  <el-dialog :model-value="modelValue" :title="placeId ? '编辑重点场所' : '新增重点场所'" width="min(900px,94vw)" class="fire-dialog place-editor-dialog" append-to-body :close-on-click-modal="false" :close-on-press-escape="!saving" :show-close="!saving" :before-close="close" @opened="opened">
    <div v-if="error" class="fire-error" role="alert">{{ error }}<el-button v-if="loadFailed" link @click="load">重新加载</el-button></div>
    <el-form v-if="!loadFailed" ref="formRef" v-loading="loading" :model="form" :rules="rules" label-position="top" novalidate @submit.prevent="save">
      <fieldset :disabled="saving || loading" class="place-editor-fields">
        <div class="place-fields">
          <el-form-item label="场所名称" prop="name"><el-input ref="nameInput" v-model="form.name" aria-label="场所名称" maxlength="255" :disabled="saving || loading" /></el-form-item>
          <el-form-item label="场所类别" prop="placeType"><el-select v-model="form.placeType" aria-label="场所类别" popper-class="fire-dialog" :disabled="saving || loading"><el-option v-for="option in placeTypes" :key="option.type" :label="option.label" :value="option.type" /></el-select></el-form-item>
          <el-form-item class="wide" label="详细地址" prop="address"><el-input v-model="form.address" aria-label="详细地址" maxlength="500" :disabled="saving || loading" /></el-form-item>
          <el-form-item label="行业类别"><el-input v-model="form.industryCategory" aria-label="行业类别" maxlength="100" :disabled="saving || loading" /></el-form-item>
          <el-form-item label="物业或管理单位"><el-input v-model="form.managementCompany" aria-label="物业或管理单位" maxlength="255" :disabled="saving || loading" /></el-form-item>
          <el-form-item label="风险等级"><el-select v-model="form.riskLevel" aria-label="风险等级" clearable popper-class="fire-dialog" :disabled="saving || loading"><el-option v-for="risk in risks" :key="risk" :label="risk" :value="risk" /></el-select></el-form-item>
          <el-form-item class="wide" label="主要风险源"><el-input v-model="form.mainRiskSources" aria-label="主要风险源" type="textarea" resize="none" :rows="3" maxlength="10000" :disabled="saving || loading" /></el-form-item>
        </div>
        <div class="place-section-heading"><h3>联系人</h3><el-button :disabled="saving || loading || form.contacts.length >= 100" @click="addContact">添加联系人</el-button></div>
        <p v-if="!form.contacts.length" class="muted">暂无联系人，可按需添加负责人或其他联系人。</p>
        <div v-for="(contact, index) in form.contacts" :key="contact._key" class="place-contact-edit">
          <el-form-item :label="`联系人 ${index + 1} 类型`"><el-select v-if="!contact.contactId" v-model="contact.contactRole" :aria-label="`联系人 ${index + 1} 类型`" popper-class="fire-dialog" :disabled="saving || loading"><el-option label="负责人" value="import_responsible" /><el-option label="联系人" value="import_contact" /></el-select><span v-else>{{ roleLabel(contact.contactRole) }}</span></el-form-item>
          <el-form-item :label="`联系人 ${index + 1} 姓名`" :prop="`contacts.${index}.contactName`" :rules="[{validator: (_, value, cb) => validateContact(contact, cb), trigger: 'blur'}]"><el-input v-model="contact.contactName" :aria-label="`联系人 ${index + 1} 姓名`" maxlength="100" :disabled="saving || loading" /></el-form-item>
          <el-form-item :label="`联系人 ${index + 1} 电话`"><el-input v-model="contact.phone" :aria-label="`联系人 ${index + 1} 电话`" type="tel" placeholder="手机或固定电话，可保留分机" maxlength="64" :disabled="saving || loading" /></el-form-item>
          <el-button link type="danger" :aria-label="`移除联系人 ${index + 1}`" :disabled="saving || loading" @click="form.contacts.splice(index, 1)">移除</el-button>
        </div>
        <div class="place-section-heading"><h3>地图位置</h3><el-button :disabled="saving || loading" @click="pointOpen = true">在地图上选取坐标</el-button></div>
        <p class="muted">使用高德 GCJ-02 坐标；经纬度可以同时留空。修改地址或坐标后，位置会标记为待核验。</p>
        <div class="place-fields">
          <el-form-item label="经度" prop="longitude"><el-input v-model="form.longitude" aria-label="经度" inputmode="decimal" placeholder="例如 118.7500000" :disabled="saving || loading" /></el-form-item>
          <el-form-item label="纬度" prop="latitude"><el-input v-model="form.latitude" aria-label="纬度" inputmode="decimal" placeholder="例如 32.2300000" :disabled="saving || loading" /></el-form-item>
          <el-form-item class="wide" label="备注"><el-input v-model="form.remark" aria-label="备注" type="textarea" resize="none" :rows="3" maxlength="500" :disabled="saving || loading" /></el-form-item>
        </div>
      </fieldset>
    </el-form>
    <template #footer><el-button :disabled="saving" @click="close()">取消</el-button><el-button type="primary" :loading="saving" :disabled="loading || loadFailed" @click="save">保存</el-button></template>
  </el-dialog>
  <el-dialog v-model="pointOpen" title="点击地图选取场所位置" width="min(1000px,96vw)" class="fire-dialog" append-to-body><MapPanel v-if="pointOpen" pick-point @pick="selectPoint" /><template #footer><el-button @click="pointOpen = false">取消选点</el-button></template></el-dialog>
</template>
<script setup>
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getPlaceEditor, saveDatabasePlace } from '@/api/fire/places'
import { PLACE_LAYERS, hasMapCoordinates } from '@/api/fire/placeLayers'
import MapPanel from './MapPanel.vue'

const props = defineProps({ modelValue: Boolean, placeId: [String, Number] })
const emit = defineEmits(['update:modelValue', 'saved'])
const placeTypes = [...PLACE_LAYERS, { type: 'government', label: '机关单位' }]
const formRef = ref(), nameInput = ref(), form = ref(blank()), loading = ref(false), saving = ref(false), loadFailed = ref(false), error = ref(''), pointOpen = ref(false)
let version = 0, contactKey = 0, baseline = '', closing = false
function blank() { return { name: '', placeType: 'industry', address: '', industryCategory: '', managementCompany: '', riskLevel: '', mainRiskSources: '', longitude: '', latitude: '', coordinateSystem: 'GCJ02', remark: '', contacts: [] } }
const risks = computed(() => [...new Set(['低', '一般', '较高', '高', form.value.riskLevel].filter(Boolean))])
const roleLabel = role => ({ import_responsible: '负责人', principal: '主要负责人', legal_representative: '法定代表人', onsite_manager: '现场负责人', fire_safety_manager: '消防安全管理人', import_contact: '联系人' }[role] || '联系人')
function validateContact(contact, cb) { contact.contactName?.trim() || contact.phone?.trim() ? cb() : cb(new Error('请填写联系人姓名或电话，或移除此行')) }
function coordinates(_, value, cb) {
  const lng = String(form.value.longitude ?? '').trim(), lat = String(form.value.latitude ?? '').trim()
  if (!lng && !lat) return cb()
  if (!hasMapCoordinates(lng, lat)) return cb(new Error('请成对填写有效经纬度，经度为 -180～180，纬度为 -90～90'))
  if ([lng, lat].some(v => !/^-?\d+(?:\.\d{1,7})?$/.test(v))) return cb(new Error('经纬度最多保留7位小数'))
  cb()
}
const rules = { name: [{ required: true, whitespace: true, message: '请填写场所名称', trigger: 'blur' }], placeType: [{ required: true, message: '请选择场所类别', trigger: 'change' }], longitude: [{ validator: coordinates, trigger: 'blur' }], latitude: [{ validator: coordinates, trigger: 'blur' }] }
function opened() { formRef.value?.clearValidate(); nameInput.value?.focus() }
function addContact() { form.value.contacts.push({ _key: ++contactKey, contactName: '', phone: '', contactRole: form.value.contacts.length ? 'import_contact' : 'import_responsible' }) }
function selectPoint([lng, lat]) { form.value.longitude = Number(lng).toFixed(7); form.value.latitude = Number(lat).toFixed(7); pointOpen.value = false }
async function close(done) {
  if (saving.value || closing) return
  closing = true
  try {
    if (!loading.value && !loadFailed.value && JSON.stringify(form.value) !== baseline) await ElMessageBox.confirm('尚有未保存的场所资料，确认放弃修改？', '放弃修改', { type: 'warning', confirmButtonText: '放弃修改', cancelButtonText: '继续编辑' })
    ++version; pointOpen.value = false; emit('update:modelValue', false); if (typeof done === 'function') done()
  } catch { /* Keep the user's draft. */ } finally { closing = false }
}
async function load() {
  const current = ++version; loading.value = !!props.placeId; error.value = ''; loadFailed.value = false; form.value = blank(); baseline = JSON.stringify(form.value)
  try {
    if (props.placeId) {
      const result = await getPlaceEditor(props.placeId)
      if (current !== version) return
      if (!result.place || !Array.isArray(result.contacts)) throw Error('场所资料返回格式异常，请重新加载')
      const place = result.place
      form.value = { ...blank(), placeId: place.id, revision: place.revision, name: place.name, placeType: place.placeType, address: place.address || '', industryCategory: place.industryCategory || '', managementCompany: place.managementCompany || '', riskLevel: place.riskLevel || '', mainRiskSources: place.mainRiskSources || '', longitude: place.longitude ?? '', latitude: place.latitude ?? '', coordinateSystem: place.coordinateSystem || 'GCJ02', remark: place.remark || '', contacts: result.contacts.map(c => ({ ...c, contactName: c.contactName || '', phone: c.phone || '', _key: ++contactKey })) }
    }
    baseline = JSON.stringify(form.value); await nextTick(); if (current === version) { formRef.value?.clearValidate(); nameInput.value?.focus() }
  } catch (e) { if (current === version) { loadFailed.value = true; error.value = e.message || '场所资料加载失败' } } finally { if (current === version) loading.value = false }
}
async function save() {
  if (saving.value || loading.value || loadFailed.value) return
  error.value = ''
  saving.value = true
  try { await formRef.value.validate() } catch { saving.value = false; await nextTick(); formRef.value?.$el.querySelector('.is-error input')?.focus(); return }
  try {
    const value = { ...form.value, longitude: String(form.value.longitude ?? '').trim() ? Number(form.value.longitude) : null, latitude: String(form.value.latitude ?? '').trim() ? Number(form.value.latitude) : null, contacts: form.value.contacts.map(({ _key, ...c }) => c) }
    await saveDatabasePlace(value); baseline = JSON.stringify(form.value); ElMessage.success(props.placeId ? '场所资料已更新' : '场所已新增'); emit('saved'); emit('update:modelValue', false)
  } catch (e) { error.value = e.message || '保存失败，请重试' } finally { saving.value = false }
}
watch(() => [props.modelValue, props.placeId], () => { pointOpen.value = false; if (props.modelValue) load(); else ++version }, { immediate: true })
onBeforeUnmount(() => ++version)
</script>
<style scoped>
.place-editor-fields{border:0;padding:0;margin:0;min-width:0}.place-fields{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:0 20px}.wide{grid-column:1/-1}.place-fields .el-select,.place-contact-edit .el-select{width:100%}.place-section-heading{display:flex;justify-content:space-between;align-items:center;gap:16px;margin:8px 0 14px}.place-section-heading h3{font-size:16px;margin:0}.muted{font-size:12px;line-height:1.7;color:var(--fire-muted)}.place-contact-edit{display:grid;grid-template-columns:1fr 1fr 1.5fr auto;gap:12px;align-items:center;border-bottom:1px solid var(--el-border-color-lighter);margin-bottom:16px}.place-contact-edit>.el-button{align-self:center}@media(max-width:640px){.place-fields,.place-contact-edit{grid-template-columns:1fr}.wide{grid-column:auto}.place-contact-edit{gap:0;padding-bottom:12px}.place-contact-edit>.el-button{justify-self:end}}
</style>
<style>
.place-editor-dialog.el-dialog{display:flex;flex-direction:column;max-height:90vh;margin-top:5vh}.place-editor-dialog .el-dialog__body{overflow-y:auto;min-height:0;flex:1}.place-editor-dialog .el-dialog__header,.place-editor-dialog .el-dialog__footer{flex-shrink:0}
</style>
