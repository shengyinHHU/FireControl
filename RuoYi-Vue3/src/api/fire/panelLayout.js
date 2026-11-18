// All sizes are pixels; a column always retains its full available height.
export function panelMinimums(available, minimums) {
  const room = Math.max(0, Number(available) || 0)
  const values = minimums.map(value => Math.max(0, Number(value) || 0))
  const total = values.reduce((sum, value) => sum + value, 0)
  return total > room ? values.map(value => value * room / total) : values
}

export function fitPanelSizes(available, preferred, minimums) {
  let room = Math.max(0, Number(available) || 0)
  const limits = panelMinimums(room, minimums)
  const weights = minimums.map((_, index) => Number.isFinite(preferred?.[index]) && preferred[index] > 0 ? preferred[index] : 0)
  const result = minimums.map(() => 0)
  let remaining = minimums.map((_, index) => index)
  while (remaining.length) {
    const total = remaining.reduce((sum, index) => sum + weights[index], 0)
    const share = index => total > 0 ? room * weights[index] / total : room / remaining.length
    const constrained = remaining.filter(index => share(index) < limits[index])
    if (!constrained.length) { for (const index of remaining) result[index] = share(index); break }
    for (const index of constrained) { result[index] = limits[index]; room = Math.max(0, room - limits[index]) }
    remaining = remaining.filter(index => !constrained.includes(index))
  }
  return result
}

export function movePanelBoundary(sizes, minimums, index, delta) {
  const result = [...sizes]
  if (index < 0 || index >= sizes.length - 1 || !Number.isFinite(delta)) return result
  const pair = sizes[index] + sizes[index + 1]
  const upper = Math.max(minimums[index], Math.min(pair - minimums[index + 1], sizes[index] + delta))
  result[index] = upper; result[index + 1] = Math.max(0, pair - upper)
  return result
}

export function readPanelShares(value, count) {
  try {
    const stored = JSON.parse(value)
    if (stored?.version !== 1 || !Array.isArray(stored.shares) || stored.shares.length !== count || stored.shares.some(size => typeof size !== 'number' || !Number.isFinite(size) || size <= 0 || size > 1)) return null
    const total = stored.shares.reduce((sum, size) => sum + size, 0)
    return Math.abs(total - 1) < .001 ? stored.shares.map(size => size / total) : null
  } catch { return null }
}
