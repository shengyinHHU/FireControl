import { ref, reactive, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { fire } from '@/api/fire'
export function useList(resource, initial = {}) {
  const rows = ref([]),
    total = ref(0),
    loading = ref(false),
    error = ref('')
  const query = reactive({ pageNum: 1, pageSize: 10, keyword: '', ...initial })
  let revision = 0
  async function load() {
    const current = ++revision
    loading.value = true
    error.value = ''
    try {
      const res = await fire.list(resource, { ...query })
      if (current === revision) {
        rows.value = res.rows
        total.value = res.total
      }
    } catch (e) {
      if (current === revision) error.value = e.message || '加载失败，请重试'
    } finally {
      if (current === revision) loading.value = false
    }
  }
  const search = () => {
    query.pageNum = 1
    return load()
  }
  return { rows, total, loading, error, query, load, search }
}
export async function attempt(action, success) {
  try {
    const result = await action()
    if (success) ElMessage.success(success)
    return result
  } catch (e) {
    if (e !== 'cancel' && e !== 'close')
      ElMessage.error(e.message || '操作失败，请重试')
    return null
  }
}
