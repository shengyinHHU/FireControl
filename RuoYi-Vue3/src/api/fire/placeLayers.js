// Stable database codes are the source of truth for resource layers.
export const PLACE_LAYERS = Object.freeze([
  { label: '重点企业', type: 'industry', color: '#e7bd70', glyph: '企' },
  { label: '高层建筑', type: 'highrise', color: '#73bfff', glyph: '楼' },
  { label: '综合商业体', type: 'commercial', color: '#c5a5ff', glyph: '商' },
  { label: '养老院', type: 'elderly', color: '#73dcc3', glyph: '养' },
])
export const placeLayer = (type) => PLACE_LAYERS.find(layer => layer.type === type)

export function hasMapCoordinates(lng, lat) {
  const present = value => value !== null && value !== undefined && String(value).trim() !== ''
  return present(lng) && present(lat) && Number.isFinite(Number(lng)) && Number.isFinite(Number(lat)) &&
    Math.abs(Number(lng)) <= 180 && Math.abs(Number(lat)) <= 90 && !(Number(lng) === 0 && Number(lat) === 0)
}

export function normalizePlaceRecord(row) {
  const layer = placeLayer(row.placeType)
  if ((!layer && row.placeType !== 'government') || row.placeId == null) return null
  return {
    ...row, id: String(row.placeId), gridId: row.gridId == null ? '' : String(row.gridId),
    category: layer?.label || '机关单位', lng: row.longitude == null ? null : Number(row.longitude),
    lat: row.latitude == null ? null : Number(row.latitude),
    crs: row.coordinateSystem, risk: row.riskLevel, source: 'database',
  }
}

export function normalizeMapPlace(row) {
  if (!placeLayer(row.placeType) || !hasMapCoordinates(row.longitude, row.latitude)) return null
  return normalizePlaceRecord(row)
}

export function filterMapPlaces(places, layers, keyword = '', gridId = '') {
  const enabledTypes = new Set(PLACE_LAYERS.filter(layer => layers.includes(layer.label)).map(layer => layer.type))
  const term = keyword.trim()
  return places.filter(place => {
    const type = place.placeType || PLACE_LAYERS.find(layer => layer.label === place.category)?.type
    return enabledTypes.has(type) && (!gridId || String(place.gridId) === String(gridId)) &&
      (!term || `${place.name || ''} ${place.address || ''}`.includes(term))
  })
}
