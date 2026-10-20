from pathlib import Path
p=Path('src/api/fire/mapAdapter.js')
s=p.read_text(encoding='utf-8')
s="import { clusterPlaces, placeIcon } from './mapPresentation'\nimport './mapPresentation.css'\n"+s
s=s.replace("strokeOpacity: 0.9, strokeWeight: 2", "strokeOpacity: 0.45, strokeWeight: 1")
s=s.replace("strokeOpacity: 0.3, strokeWeight: 1", "strokeOpacity: 0.18, strokeWeight: 1")
s=s.replace("color: '#d2f4ff'", "color: '#a5bbc9'")
s=s.replace("map.getZoom() >= 14", "map.getZoom() >= 15")
start=s.index("          const icon = document.createElement('span')")
end=s.index("        if (type === 'alarm')",start)
s=s[:start]+'''          const icon = document.createElement('span')
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
''' + s[end:]
s=s.replace("element.onclick = e => { e.stopPropagation(); onSelect(type, row.id) }", "element.onclick = e => { e.stopPropagation(); if (type === 'place') { selectedPlace = row.id; redraw() }; onSelect(type, row.id) }")
s=s.replace("anchor: 'bottom-center'", "anchor: type === 'place' ? 'center' : 'bottom-center', zIndex: type === 'alarm' ? 220 : 120")
s=s.replace("      places.forEach(p => marker(p, 'place', placeLayer(p.placeType)?.color || '#e2a7ad'))",'''      clusterPlaces(places, map.getZoom()).forEach(group => {
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
      })''')
s=s.replace("  let overlays = [], draft", "  let overlays = [], draft, features, selectedPlace\n  const clusterInfo = new AMap.InfoWindow({ offset: new AMap.Pixel(0, -20) })\n  const redraw = () => { if (features) renderFeatures(features) }\n  map.on('zoomend', redraw)")
s=s.replace("  return {\n    setFeatures({ places, grids, cameras, devices, alarms, layers }) {", "  function renderFeatures({ places, grids, cameras, devices, alarms, layers }) {")
s=s.replace("    },\n    setDraft(path)", "    }\n  return {\n    setFeatures(value) { features = value; clusterInfo.close(); redraw() },\n    setDraft(path)")
p.write_text(s,encoding='utf-8')
p=Path('src/components/Fire/MapPanel.vue');s=p.read_text(encoding='utf-8');s=s.replace("!['实时路况', '摄像头', '告警设备',", "!['地图POI', '实时路况', '摄像头', '告警设备',");p.write_text(s,encoding='utf-8')
