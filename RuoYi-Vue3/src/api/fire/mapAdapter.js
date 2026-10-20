import { clusterPlaces, placeIcon } from './mapPresentation'
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
        strokeColor: '#72e4ff', strokeOpacity: 0.45, strokeWeight: 1, zIndex: 45, bubble: true })
      boundaryOverlays.push(polygon)
    }
    map.add(boundaryOverlays)
    const info = new AMap.InfoWindow({ offset: new AMap.Pixel(0, -12) })
    for (const region of dachangBoundary.regions) {
      const ring = region.geometry.coordinates[0]
      const polygon = new AMap.Polygon({ path: ring, fillOpacity: 0, strokeColor: '#a8d1ed',
        strokeOpacity: 0.18, strokeWeight: 1, zIndex: 46 })
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
        zIndex: 60, style: { color: '#a5bbc9', background:'transparent', border:'none',
          fontSize:'12px', textShadow:'0 1px 3px #000, 0 0 4px #000', padding:'0' } }))
    }
    const updateLabels = () => {
      if (map.getZoom() >= 15) map.add(boundaryLabels)
      else map.remove(boundaryLabels)
    }
    map.on('zoomend', updateLabels)
    map.on('complete', () => { fitBoundary(); updateLabels() })
    fitBoundary()
  }
  const traffic = new AMap.TileLayer.Traffic({ autoRefresh: true, interval: 180 })
  let overlays = [], draft, features, selectedPlace
  const clusterInfo = new AMap.InfoWindow({ offset: new AMap.Pixel(0, -20) })
  const redraw = () => { if (features) renderFeatures(features) }
  map.on('zoomend', redraw)
  map.on('click', e => onPoint([e.lnglat.getLng(), e.lnglat.getLat()]))
  function renderFeatures({ places, grids, cameras, devices, alarms, layers }) {
      map.remove(overlays); overlays = []
      const marker = (row, type, color) => {
        if (!hasMapCoordinates(row.lng, row.lat)) return
        const element = document.createElement('button')
        element.type = 'button'; element.textContent = `${type === 'camera' ? '▣' : type === 'alarm' ? '🔥' : '●'} ${row.name}`
        element.style.cssText = `background:#082539;color:${color};border:1px solid ${color};padding:5px 7px;border-radius:3px;white-space:nowrap;cursor:pointer;font-size:11px`
        if (type === 'place') {
          const layer = placeLayer(row.placeType)
          const icon = document.createElement('span')
          icon.innerHTML = placeIcon(row.placeType)
          icon.setAttribute('aria-hidden', 'true')
          const label = document.createElement('span')
          label.className = 'fire-map-place-name'
          label.textContent = row.name
          element.replaceChildren(icon, label)
          element.className = 'fire-map-place-marker'
          element.title = `${layer?.label || '场所'}：${row.name}`
          element.setAttribute('aria-label', element.title)
          element.removeAttribute('style')
          if (selectedPlace === row.id) element.classList.add('is-selected')
        }
        if (type === 'alarm') { element.style.cssText += ';font-size:16px;background:#542638dd;border-color:#ff7966;box-shadow:0 0 20px #ff513c99'; element.setAttribute('aria-label', `火警：${row.placeName || ''} ${row.name}`) }
        element.onclick = e => { e.stopPropagation(); if (type === 'place') { selectedPlace = row.id; redraw() }; onSelect(type, row.id) }
        overlays.push(new AMap.Marker({ position: [Number(row.lng), Number(row.lat)], content: element, anchor: type === 'place' ? 'center' : 'bottom-center', zIndex: type === 'alarm' ? 220 : 120 }))
      }
      clusterPlaces(places, map.getZoom()).forEach(group => {
        if (group.rows.length === 1) { marker(group.rows[0], 'place', '#64cfe8'); return }
        const button = document.createElement('button')
        button.type = 'button'
        button.className = 'fire-map-cluster'
        button.textContent = group.rows.length
        button.setAttribute('aria-label', `${group.rows.length}个场所，点击展开`)
        button.onclick = e => {
          e.stopPropagation()
          if (map.getZoom() < 18) { map.setZoomAndCenter(Math.min(18, map.getZoom() + 2), group.position); return }
          const list = document.createElement('div')
          list.className = 'fire-map-cluster-list'
          for (const row of group.rows) {
            const item = document.createElement('button')
            item.type = 'button'; item.textContent = `${placeLayer(row.placeType)?.label || '场所'} · ${row.name}`
            item.onclick = () => { selectedPlace = row.id; clusterInfo.close(); onSelect('place', row.id) }
            list.appendChild(item)
          }
          clusterInfo.setContent(list); clusterInfo.open(map, group.position)
        }
        overlays.push(new AMap.Marker({position: group.position, content: button, anchor: 'center', zIndex: 130}))
      })
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
    }
  return {
    setFeatures(value) { features = value; clusterInfo.close(); redraw() },
    setDraft(path) { if (draft) map.remove(draft); draft = new AMap.Polyline({ path, strokeColor: '#66e6ef', strokeWeight: 3 }); map.add(draft) },
    focus(position) { map.setZoomAndCenter(16, position) },
    zoom(delta) { map.setZoom(map.getZoom() + delta) },
    reset() { fitBoundary() },
    convert(coordinates, from = 'gps') { return new Promise((resolve, reject) => AMap.convertFrom(coordinates, from, (status, result) => status === 'complete' ? resolve(result.locations.map(p => [p.getLng(), p.getLat()])) : reject(Error('坐标转换失败')))) },
    destroy() { map.destroy() }
  }
}
