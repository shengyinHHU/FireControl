# Fire map interaction contract

The shared MapPanel is the owner of resource-layer toggles. Preserve its current two-column label arrangement and Element Plus feedback/detail primitives. Selection is local transient map state, defaults to all four place layers off, and is not a persistent database mutation.

Native buttons support Enter/Space, aria-pressed, visible keyboard focus, and independent multiple selection. Marker removal is immediate and does not depend on a new network request. The latest selection wins when data loads. Markers use class glyphs and names, not color alone.

Use a stable panel footer for loading, login, retry and displayed count. Keep other independent overlays (boundary/POI/traffic) intact when switching place types. Never show demo places as a successful response for a real-data failure. Detail dialogs reuse PlaceDetail and restore focus through Element Plus.

Verification owners: tests/fire/place-layers.test.mjs; tests/fire/map-layers.browser.cjs; docs/verification/map-place-layers-browser.json. Live deployment additionally requires the RuoYi backend, imported data/coordinates, a valid login and fire:places:list permission.
## Device access canonical UI map

| Capability | Canonical owner | Source of truth | Allowed variants | Verification |
|---|---|---|---|---|
| Select/Listbox | Element Plus ElSelect | UX-CONTRACT.md | device event kind filter | device-access.browser.cjs |
| Form | Element Plus ElForm/ElInput | UX-CONTRACT.md | read-only device query toolbar | device-access.browser.cjs |
| Scrollbar | assets/styles/fire.scss and Element Plus table scroll owner | DESIGN.md | table internal horizontal scroll, bounded raw-data pre | device-access.browser.cjs |
| Toast | Element Plus ElMessage via utils/request.js | UX-CONTRACT.md | request failure plus persistent inline retry | device-access.browser.cjs |
| CRUD | DeviceAccessView + deviceAccess.js | domain API | read-only; no fake writes or simulated transitions | device-access.test.mjs |
| Date | Server Asia/Shanghai formatting | domain protocol | read-only receive/device time with clock warning | FireAccessIntegrationTest.java |
| Table Selection | Not applicable | domain API | no batch actions | device-access.browser.cjs |

DeviceAccessView owns the repeated gateway/host/event/message table, URL committed query state, 5-second refresh, loading/error/empty states and stale-request protection. Event details reuse AlarmDetail's explicit database read-only variant. No database record may load demo cameras, personnel or disposition timelines. Real page provenance belongs to Shell; hide demo-reset controls on database-owned pages. Raw protocol strings are plain text; Vue interpolation must remain escaped. Tables own horizontal overflow; shared page/ancestor scrolling is unchanged. Tokens remain in assets/styles/fire.scss; this feature adds no new palette or typography system.
