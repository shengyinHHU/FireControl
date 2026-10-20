import { hasMapCoordinates, placeLayer } from './placeLayers'
import dachangBoundary from '@/assets/geo/dachang-boundary.json'
// AMap JS API 2.0. Browser key is public; configure serviceHost to keep the security code on the server.
let sdkPromise
export function loadMapSdk() {
  if (window.AMap) return Promise.resolve(window.AMap)
  if (sdkPromise) return sdkPromise
  const key = import.meta.env.VITE_FIRE_AMAP_KEY
  if (!key) return Promise.reject(Error('请配置 VITE_FIRE_AMAP_KEY 后重试'))
  const serviceHost = import.meta.env.VITE_FIRE_AMAP_SERVICE_HOST
  if (serviceHost) window._AMapSecurityConfig = { serviceHost: new URL(serviceHost, window.location.origin).href }
  sdkPromise = new Promise((resolve, reject) => {
    const script = document.createElement('script')
    const timer = setTimeout(() => { script.remove(); sdkPromise = null; reject(Error('地图服务加载超时，请检查网络与域名配置')) }, 15000)
    script.src = `https://webapi.amap.com/maps?v=2.0&key=${encodeURIComponent(key)}`
    script.onload = () => { clearTimeout(timer); if (window.AMap) resolve(window.AMap); else { sdkPromise = null; reject(Error('地图SDK未加载成功')) } }
    script.onerror = () => { clearTimeout(timer); script.remove(); sdkPromise = null; reject(Error('地图服务连接失败')) }
    document.head.appendChild(script)
  })
  return sdkPromise
}
export async function mountMap(container, { onSelect, onPoint, showBoundary = true }) {
  const AMap = await loadMapSdk()
  const map = new AMap.Map(container, { center: [118.753, 32.229], zoom: 14, mapStyle: 'amap://styles/darkblue', viewMode: '2D', features: ['bg', 'road', 'building', 'point'] })
  const boundaryOverlays = []
  const boundaryLabels = []
  const boundaryPieces = dachangBoundary.geometry.type === 'Polygon'
    ? [dachangBoundary.geometry.coordinates] : dachangBoundary.geometry.coordinates
  const padding = () => {
    const screen = container.closest('.command-screen')
    if (!screen || window.innerWidth <= 1100) return [110, 70, 35, 35]
    return [120, 65,
      (screen.querySelector('.left-column')?.getBoundingClientRect().width || 0) + 40,
      (screen.querySelector('.right-column')?.getBoundingClientRect().width || 0) + 40]
  }
  const fitBoundary = () => {
    if (showBoundary && boundaryOverlays.length) map.setFitView(boundaryOverlays, true, padding(), 15)
    else map.setZoomAndCenter(14, [118.753, 32.229])
  }
  if (showBoundary) {
    // Exact geometric union of supplied polygons. Preserve gaps, never use a convex hull.
    const outside = new AMap.Polygon({
      path: [[[70, 0], [140, 0], [140, 60], [70, 60]], ...boundaryPieces.map(p => p[0])],
      fillColor: '#020817', fillOpacity: 0.72, strokeWeight: 0, zIndex: 40, bubble: true
    })
    map.add(outside)
    for (const path of boundaryPieces) {
      const polygon = new AMap.Polygon({ path, fillColor: '#20a7ce', fillOpacity: 0.07,
        strokeColor: '#72e4ff', strokeOpacity: 0.9, strokeWeight: 2, zIndex: 45, bubble: true })
      boundaryOverlays.push(polygon)
    }
    map.add(boundaryOverlays)
    const info = new AMap.InfoWindow({ offset: new AMap.Pixel(0, -12) })
    for (const region of dachangBoundary.regions) {
      const ring = region.geometry.coordinates[0]
      const polygon = new AMap.Polygon({ path: ring, fillOpacity: 0, strokeColor: '#a8d1ed',
        strokeOpacity: 0.3, strokeWeight: 1, zIndex: 46 })
      polygon.on('click', e => {
        const content = document.createElement('div')
        content.textContent = `大厂街道 · ${region.properties.name === '白色部分' ? '产业区域（资料白色部分）' : region.properties.name}`
        content.style.cssText = 'color:#153049;padding:6px 10px;font-size:14px'
        info.setContent(content); info.open(map, e.lnglat)
      })
      map.add(polygon)
      const position = region.properties.labelPoint
      if (position) boundaryLabels.push(new AMap.Text({ position,
        text: region.properties.name === '白色部分' ? '产业区域' : region.properties.name,
        zIndex: 60, style: { color: '#d2f4ff', background:'transparent', border:'none',
          fontSize:'12px', textShadow:'0 1px 3px #000, 0 0 4px #000', padding:'0' } }))
    }
    const updateLabels = () => {
      if (map.getZoom() >= 14) map.add(boundaryLabels)
      else map.remove(boundaryLabels)
    }
    map.on('zoomend', updateLabels)
    map.on('complete', () => { fitBoundary(); updateLabels() })
    fitBoundary()
  }
  const traffic = new AMap.TileLayer.Traffic({ autoRefresh: true, interval: 180 })
  let overlays = [], draft
  map.on('click', e => onPoint([e.lnglat.getLng(), e.lnglat.getLat()]))
  return {
    setFeatures({ places, grids, cameras, devices, alarms, layers }) {
      map.remove(overlays); overlays = []
      const marker = (row, type, color) => {
        if (!hasMapCoordinates(row.lng, row.lat)) return
        const element = document.createElement('button')
        element.type = 'button'; element.textContent = `${type === 'camera' ? '▣' : type === 'alarm' ? '🔥' : '●'} ${row.name}`
        element.style.cssText = `background:#082539;color:${color};border:1px solid ${color};padding:5px 7px;border-radius:3px;white-space:nowrap;cursor:pointer;font-size:11px`
        if (type === 'place') {
          const layer = placeLayer(row.placeType)
          const icon = document.createElement('span')
          icon.textContent = layer?.glyph || '场'
          icon.setAttribute('aria-hidden', 'true')
          icon.style.cssText = `display:inline-flex;align-items:center;justify-content:center;width:24px;height:24px;margin-right:0;border-radius:3px;background:${color};color:#071728;font-size:14px;font-weight:700`
          const label = document.createElement('span')
          label.textContent = row.name
          label.hidden = true
          label.style.cssText = 'display:inline-block;max-width:190px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;vertical-align:middle'
          // Keep dense map labels readable: show the name on pointer or keyboard focus.
          const showLabel = () => { label.hidden = false; label.style.display = 'inline-block' }
          const hideLabel = () => { label.hidden = true; label.style.display = 'none' }
          hideLabel()
          element.onmouseenter = showLabel
          element.onmouseleave = hideLabel
          element.onfocus = showLabel
          element.onblur = hideLabel
          element.replaceChildren(icon, label)
          element.className = 'fire-map-place-marker'
          element.title = `${layer?.label || row.category || '场所'}：${row.name}`
          element.setAttribute('aria-label', element.title)
          element.style.padding = '3px 7px 3px 3px'
        }
        if (type === 'alarm') { element.style.cssText += ';font-size:16px;background:#542638dd;border-color:#ff7966;box-shadow:0 0 20px #ff513c99'; element.setAttribute('aria-label', `火警：${row.placeName || ''} ${row.name}`) }
        element.onclick = e => { e.stopPropagation(); onSelect(type, row.id) }
        overlays.push(new AMap.Marker({ position: [Number(row.lng), Number(row.lat)], content: element, anchor: 'bottom-center' }))
      }
      places.forEach(p => marker(p, 'place', placeLayer(p.placeType)?.color || '#e2a7ad'))
      if (layers.includes('摄像头')) cameras.forEach(p => marker(p, 'camera', '#73dcc3'))
      if (layers.includes('告警设备')) devices.forEach(p => marker(p, 'device', '#e7bd70'))
      alarms.forEach(p => marker(p, 'alarm', '#fc788b'))
      if (layers.includes('网格')) grids.forEach((g, i) => {
        if (!g.polygon?.length) return
        const polygon = new AMap.Polygon({ path: g.polygon, strokeColor: ['#ceb45f', '#3cba9d', '#49a4cc'][i % 3], fillColor: ['#ceb45f', '#3cba9d', '#49a4cc'][i % 3], fillOpacity: .18, strokeWeight: 2 })
        polygon.on('click', () => onSelect('grid', g.id)); overlays.push(polygon)
      })
      map.add(overlays)
      if (layers.includes('实时路况')) map.add(traffic); else map.remove(traffic)
      map.setFeatures(layers.includes('地图POI') ? ['bg', 'road', 'building', 'point'] : ['bg', 'road', 'building'])
    },
    setDraft(path) { if (draft) map.remove(draft); draft = new AMap.Polyline({ path, strokeColor: '#66e6ef', strokeWeight: 3 }); map.add(draft) },
    focus(position) { map.setZoomAndCenter(16, position) },
    zoom(delta) { map.setZoom(map.getZoom() + delta) },
    reset() { fitBoundary() },
    convert(coordinates, from = 'gps') { return new Promise((resolve, reject) => AMap.convertFrom(coordinates, from, (status, result) => status === 'complete' ? resolve(result.locations.map(p => [p.getLng(), p.getLat()])) : reject(Error('坐标转换失败')))) },
    destroy() { map.destroy() }
  }
}
