// A quick scan never replaces the last full reconciliation result.
export function format119SyncStatus(s) {
  if (!s) return '119同步状态暂不可用，可刷新重试；下表为当前已入库记录'
  if (!s.enabled) return '119同步未启用，当前显示已入库记录'
  const status = { never: '尚未运行', running: '进行中', success: '成功', partial: '部分失败', failed: '失败', cancelled: '已中止' }
  const full = status[s.lastResult] || '尚未运行'
  const quick = status[s.lastQuickResult] || '尚未运行'
  const lines = [
    `119快速扫描：每${s.intervalSeconds || 60}秒前${s.quickPages || 3}页，${quick}；最近成功：${s.lastQuickCompletedAt || '尚未完成'}`,
    `完整核对：每${s.fullIntervalSeconds || 600}秒，${full}；最近完整成功：${s.lastCompletedAt || '尚未完成首轮'}`,
    `最近发现新警情：${s.lastNewIncidentAt || '暂无记录'}`,
    s.dachangOnly === false ? `展示接口全部范围 ${s.total ?? 0} 条（不按大厂街道过滤）` : `大厂街道内 ${s.insideCount ?? 0} 条，待判定 ${s.pendingCount ?? 0} 条`,
    `最新来警时间：${s.latestAlarmTime || '暂无记录'}；最近接收时间：${s.latestReceivedAt || '暂无记录'}`,
  ]
  if (s.nextRetryAt) lines.push(`故障退避中，${s.nextRetryAt} 后允许重试`)
  if (['failed', 'partial'].includes(s.lastResult) || ['failed', 'partial'].includes(s.lastQuickResult)) lines.push('请检查后端同步日志')
  return lines.join(' · ')
}
