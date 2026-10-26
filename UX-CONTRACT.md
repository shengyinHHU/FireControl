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

DeviceAccessView event-only mode is 消防警情记录, with source and case filters committed to the URL; host mode keeps device-only event filtering. All status, empty, loading, error/retry and keyboard contracts reuse the shared owner. Read-only 119 details show source case fields, region, coordinate status and last synchronization without simulated disposition controls. Source filters default to all; 119 source defaults to all case types. The incident-record page omits sorting/source explanatory paragraphs and verbose synchronization diagnostics at the user’s request; query, sorting, and background synchronization remain unchanged. Source metadata, host/device statistics and history remain separated. Canonical verification: tests/fire/incident119.browser.cjs.

## Place contact detail variant

PlaceDetail owns the database contact section for both map popups and place archives. Load active contacts only from /fire/places/{id}/contacts with existing fire:places:list and place data scope. Preserve the stored phone text, show loading/empty/error with retry, and ignore superseded place requests. Place fields and all contacts share one bordered descriptions table with a fixed 140px label column, without a separate 联系人 heading. Each contact has one Element Plus ElLink immediately after their name, with the maintained Phone icon and 拨号 label. This link opens the fixed dialing portal in a new tab with noopener/noreferrer; it does not send contact data or initiate a call. Verification: tests/fire/place-contacts.browser.cjs and tools/place-contacts/verify_project.py.

## Live video variant

VideoMonitor + api/fire/video.js own real directory selection, nested directory navigation, cursor pagination, loading/empty/error/retry and superseded-query protection. Element Plus ElTabs/ElSelect/ElDialog remain canonical. LiveVideoDialog owns lazy mpegts.js FLV playback, status, manual reconnect and player destruction. Separate tabs preserve original inspection behavior. No fake camera fallback, no inferred online state, no calls to vendor APIs from browser. Backend grants account access only to administrator or explicitly configured user ids with video permissions.

Video media requests use authenticated user-bound 10-minute relay sessions because vendor playlist CORS is absent. H.265 support is runtime-dependent; unsupported codecs get explicit feedback and a VLC RTSP playlist download. No fabricated successful playback or automatic background transcoding.

119 status presentation uses api/fire/incident119Status.js as the shared text owner: independent quick/full results and successful timestamps, new incident discovery time, plus runtime retry deadline. A successful quick scan must never hide failed full reconciliation. Existing Element Plus status/read-only page layout remains canonical.

Dashboard alarms have one owner: dashboard refreshAlarms, 3-second polling plus authenticated commit-triggered SSE hints. MapPanel alarmRows consumes the same snapshot without a second alarm query; standalone maps retain self-loading. Static resources refresh every 30 seconds independently. Fetch streams keep JWT in headers, coalesce in-flight hints, retain last good alarms on transient errors, and stop on unmount. No visual theme or existing alarm handling semantics change.

## Place import canonical UI map

| Capability | Canonical owner | Contract | Verification |
|---|---|---|---|
| Form / Upload | ExcelImportDialog/PlaceImport.vue + Element Plus | single .xlsx; 10MB/2000 rows; preflight before confirmation | place-import.browser.cjs |
| Select/Listbox | Element Plus radio group | all/new/duplicate/invalid, page reset on filter | place-import.browser.cjs |
| Scrollbar / Table | existing fire theme + Element Plus | table owns horizontal/vertical scroll; responsive modal | place-import.browser.cjs |
| Toast / Failure | request.js + persistent inline ElAlert | keep selected file on preview failure and batch on commit failure | place-import.browser.cjs |
| CRUD | FirePlaceImportService | only new records, no overwrite; idempotent owner batch; transaction recheck | tools/place-import/verify_project.py |
| Date | server preview clock | 2-hour preview TTL; source dates retained as text, not create_time | PlaceImportIntegrationTest.java |

ResourcePage remains the shared list/detail owner; places exposes its refresh after successful import. Confirming a partial valid set skips invalid and duplicate rows, with exact counts visible before confirmation; there is no hidden selection or override. Busy confirmation cannot be dismissed or double-submitted. Success keeps the result dialog open, refreshes the list, and opening again clears prior results. Downloadable CSV prefixes formula-looking values, and all table names/reasons are escaped text. New records inherit current department/importing user, and readers without import permission see no import action. Valid review coordinates display on the corresponding enabled map layer; no-coordinate rows stay in the list. This replaces the former rule that all place writes were unavailable; arbitrary edit/delete remains unavailable.

Both the dashboard 119 panel and full incident-record page omit verbose synchronization diagnostic paragraphs at the user’s request; background synchronization and alarm polling are unchanged.

Dashboard alarm cards hide the incident code while retaining internal ids for selection. Content uses the existing light text, addresses cyan, times pale gold, sources mauve, and status the shared StatusTag semantic colors. Address and time remain separate readable lines and wrap within the card.

Live video defaults to FLV via /open/token/cloud/getDeviceMediaUrlFlv and lazy mpegts.js 1.8.0. LiveVideoDialog is the canonical player for dashboard and administration. Retain permission/account/device checks, stale-request protection, explicit reconnect, real decoded-video readiness, codec/error/timeout feedback, and explicit VLC download. No automatic HLS or WebRTC fallback. Closing or switching destroys MSE playback, aborts its fetch and deletes the owner-bound backend FLV session. The backend forwards continuous chunks without whole-stream buffering; vendor signed URLs and tokens stay server-side. FLV sessions allow one authenticated stream and expire before first use after 10 minutes; active streams close on cancellation, disconnect or 20-second upstream idle timeout. H.265 requires browser/OS decoder support; no implied transcoding. The previous WebRTC SDK and relay remain for explicit diagnostic callers only.
