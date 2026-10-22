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

## 119 source variant

DeviceAccessView event-only mode is 消防警情记录, with source and case filters committed to the URL; host mode keeps device-only event filtering. All status, empty, loading, error/retry and keyboard contracts reuse the shared owner. Read-only 119 details show source case fields, region, coordinate status and last synchronization without simulated disposition controls. Source filters default to all; 119 source defaults to all case types. Synchronization status remains a persistent inline message, not only a toast. Source metadata, host/device statistics and history remain separated. Canonical verification: tests/fire/incident119.browser.cjs.

## Place contact detail variant

PlaceDetail owns the database contact section for both map popups and place archives. Load active contacts only from /fire/places/{id}/contacts with existing fire:places:list and place data scope. Preserve the stored phone text, show loading/empty/error with retry, and ignore superseded place requests. Place fields and all contacts share one bordered descriptions table with a fixed 140px label column, without a separate 联系人 heading. Each contact has one Element Plus ElLink immediately after their name, with the maintained Phone icon and 拨号 label. This link opens the fixed dialing portal in a new tab with noopener/noreferrer; it does not send contact data or initiate a call. Verification: tests/fire/place-contacts.browser.cjs and tools/place-contacts/verify_project.py.

## Live video variant

VideoMonitor + api/fire/video.js own real directory selection, nested directory navigation, cursor pagination, loading/empty/error/retry and superseded-query protection. Element Plus ElTabs/ElSelect/ElDialog remain canonical. LiveVideoDialog owns native-HLS or lazy hls.js playback, status, manual reconnect and player destruction. Separate tabs preserve original inspection behavior. No fake camera fallback, no inferred online state, no calls to vendor APIs from browser. Backend grants account access only to administrator or explicitly configured user ids with video permissions.

Video media requests use authenticated user-bound 10-minute relay sessions because vendor playlist CORS is absent. H.265 support is runtime-dependent; unsupported codecs get explicit feedback and a VLC RTSP playlist download. No fabricated successful playback or automatic background transcoding.

119 status presentation uses api/fire/incident119Status.js as the shared text owner: independent quick/full results and successful timestamps, new incident discovery time, plus runtime retry deadline. A successful quick scan must never hide failed full reconciliation. Existing Element Plus status/read-only page layout remains canonical.

Dashboard alarms have one owner: dashboard refreshAlarms, 3-second polling plus authenticated commit-triggered SSE hints. MapPanel alarmRows consumes the same snapshot without a second alarm query; standalone maps retain self-loading. Static resources refresh every 30 seconds independently. Fetch streams keep JWT in headers, coalesce in-flight hints, retain last good alarms on transient errors, and stop on unmount. No visual theme or existing alarm handling semantics change.
