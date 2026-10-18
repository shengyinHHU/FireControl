// AMap JS API 2.0. Browser key is public; configure serviceHost to keep the security code on the server.
let sdkPromise
export function loadMapSdk() {
  if (window.AMap) return Promise.resolve(window.AMap)
  if (sdkPromise) return sdkPromise
  const key = import.meta.env.VITE_FIRE_AMAP_KEY
  if (!key) return Promise.reject(Error('请配置 VITE_FIRE_AMAP_KEY 后重试'))
  const serviceHost = import.meta.env.VITE_FIRE_AMAP_SERVICE_HOST
  if (serviceHost) window._AMapSecurityConfig = { serviceHost }
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
export async function mountMap(container, { onSelect, onPoint }) {
  const AMap = await loadMapSdk()
  const map = new AMap.Map(container, { center: [118.753, 32.229], zoom: 14, mapStyle: 'amap://styles/darkblue', viewMode: '2D', features: ['bg', 'road', 'building', 'point'] })
  const traffic = new AMap.TileLayer.Traffic({ autoRefresh: true, interval: 180 })
  let overlays = [], draft
  map.on('click', e => onPoint([e.lnglat.getLng(), e.lnglat.getLat()]))
  return {
    setFeatures({ places, grids, cameras, devices, alarms, layers }) {
      map.remove(overlays); overlays = []
      const marker = (row, type, color) => {
        if (!Number.isFinite(Number(row.lng)) || !Number.isFinite(Number(row.lat))) return
        const element = document.createElement('button')
        element.type = 'button'; element.textContent = `${type === 'camera' ? '▣' : type === 'alarm' ? '🔥' : '●'} ${row.name}`
        element.style.cssText = `background:#082539;color:${color};border:1px solid ${color};padding:5px 7px;border-radius:3px;white-space:nowrap;cursor:pointer;font-size:11px`
        if (type === 'alarm') { element.style.cssText += ';font-size:16px;background:#542638dd;border-color:#ff7966;box-shadow:0 0 20px #ff513c99'; element.setAttribute('aria-label', `火警：${row.placeName || ''} ${row.name}`) }
        element.onclick = e => { e.stopPropagation(); onSelect(type, row.id) }
        overlays.push(new AMap.Marker({ position: [Number(row.lng), Number(row.lat)], content: element, anchor: 'bottom-center' }))
      }
      places.forEach(p => marker(p, 'place', '#e2a7ad'))
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
    reset() { map.setZoomAndCenter(14, [118.753, 32.229]) },
    convert(coordinates, from = 'gps') { return new Promise((resolve, reject) => AMap.convertFrom(coordinates, from, (status, result) => status === 'complete' ? resolve(result.locations.map(p => [p.getLng(), p.getLat()])) : reject(Error('坐标转换失败')))) },
    destroy() { map.destroy() }
  }
}
