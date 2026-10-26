# FireControl design and data context

## Product

Chinese desktop operations dashboard for Dachang street fire supervision, built on RuoYi Vue 3 and Element Plus. Preserve the existing dark navy/cyan command-screen presentation, resource-layer panel and Chinese labels. This work extends the maintained UI rather than introducing a new visual system.

## Authoritative data policy

The user requires database records to replace conflicting or overlapping demo records. Once a resource is database-backed, do not merge demo rows or silently fall back to demo data after an empty response, login failure, permission failure, or server error. Show an actionable state instead. `src/api/fire/sourcePolicy.js` registers database-owned resources; currently `places` is real across map/list/details. Other resources retain their existing data source until a real owner is implemented.

## Map layer contract

- Labels/codes: 重点企业=industry, 高层建筑=highrise, 综合商业体=commercial, 养老院=elderly.
- All four initially off. Labels are independent native toggle buttons with aria-pressed and visible focus/selection.
- Enabling adds matching real places; disabling removes them immediately. Changes use current selection even if an earlier fetch resolves later; no duplicate marker accumulation.
- Real coordinates in review status are displayed with their truthful status; do not auto-confirm them. Missing/invalid/non-GCJ02 map coordinates are excluded.
- Loading, error/retry, login, zero-results and partial-limit states belong to MapPanel's layer-panel footer.
- Data access uses RuoYi token, fire:places:list and department/user data scope. Do not add anonymous access to compensate for demo-mode login shortcuts.

## Canonical owners

- Map layers/filtering: MapPanel.vue + api/fire/placeLayers.js.
- Marker lifecycle: api/fire/mapAdapter.js; one owned overlay collection, removed before replacement.
- Details: existing PlaceDetail.vue, with a database read-only variant; no demo personnel or fake controls shown for real records.
- HTTP/auth/session/errors: utils/request.js and utils/auth.js.
- Dialogs/buttons/descriptions: existing Element Plus components and fire-dialog styling.
- Real place writes remain unavailable until actual CRUD endpoints are provided; never save edits to a shadow demo copy.

## Verification

Use tests/fire/place-layers.test.mjs for classification, coordinate handling, real-data precedence and marker lifecycle. Browser verification uses tests/fire/map-layers.browser.cjs with a real AMap SDK and explicitly mocked backend HTTP responses; it is not a live database/authentication integration test.
## Device integration

Database-owned resources now include places, gateways, hosts, alarms and protocolMessages. DeviceAccessView is the shared list owner, AlarmDetail the shared event-detail owner, and Shell the source-provenance owner. Keep existing navy/cyan fire.scss tokens and Element Plus controls; add no independent visual theme. Separate transport connectivity, reported fault/fire bits, device clock and server receipt time. Event records are read-only evidence, not an invented disposition workflow. Government unit 大厂街道 is readable but excluded from the four enterprise map layers. Runtime scope/permissions remain server-owned.

Verification: tools/device-capture/verify_project.py and tests/fire/device-access.browser.cjs. Browser HTTP fixtures are explicitly not a live business database. Actual device fire samples and production deployment remain external acceptance steps.

## 119 incident integration

Use the existing navy/cyan tokens, DeviceAccessView for the shared read-only list, AlarmDetail for source-specific details and MapPanel/mapAdapter for markers. The 119 source retains every case type, source case status/nature and alarm time; never normalize rescue or assistance to fire, or external statuses to device verification states. The minute backend synchronization is independent of browser polling. Counts and list/detail access use fire:alarms:list plus department/user scope. No fallback to demo data.

Dachang membership uses the 25 exact region polygons/names from the uploaded 大厂街道.doc. User confirmed both document and incident coordinates are GCJ02 on 2026-10-21. Region union preserves gaps and includes edges. Outside or invalid-coordinate incidents are persisted for audit but excluded from the Dachang list/map. Map 119 markers use a 119 label; device fire markers retain their existing flame. A 119 incident never borrows a linked place's map coordinate.

Verification owners: tools/119-secret-key/verify_project.py (isolated MySQL and loopback HTTP), tests/fire/incident119.test.mjs and tests/fire/incident119.browser.cjs. Browser fixtures do not prove live deployment.

## Live video integration

VideoMonitor owns the real CTSeeLink directory/device browser in the dashboard's adjacent 视频监控 tab and inspections page. Existing 查岗管理 content remains intact. LiveVideoDialog owns lazy mpegts.js FLV playback and teardown; no simulated thumbnails, inferred locations, or invented online statuses. API credentials and vendor tokens remain backend-only under fire.video. Account scope is administrator or explicitly configured user ids, plus fire:video:list/preview permissions. Shared styling remains fire.scss and Element Plus. Closing or switching tabs destroys playback. Cursor pagination uses lastId, with no invented total. Directory labels do not imply business-place bindings.

Video media requests use authenticated user-bound 10-minute relay sessions because vendor playlist CORS is absent. H.265 support is runtime-dependent; unsupported codecs get explicit feedback and a VLC RTSP playlist download. No fabricated successful playback or automatic background transcoding.

119 status presentation uses api/fire/incident119Status.js as the shared text owner: independent quick/full results and successful timestamps, new incident discovery time, plus runtime retry deadline. A successful quick scan must never hide failed full reconciliation. Existing Element Plus status/read-only page layout remains canonical.

Dashboard alarms have one owner: dashboard refreshAlarms, 3-second polling plus authenticated commit-triggered SSE hints. MapPanel alarmRows consumes the same snapshot without a second alarm query; standalone maps retain self-loading. Static resources refresh every 30 seconds independently. Fetch streams keep JWT in headers, coalesce in-flight hints, retain last good alarms on transient errors, and stop on unmount. No visual theme or existing alarm handling semantics change.

## Place import

Canonical importer: `components/ExcelImportDialog/PlaceImport.vue`, a business-named variant of the shared ExcelImportDialog family, reuses Element Plus upload/dialog/table/radio/pagination, request.js auth/download, and the existing navy/cyan tokens. Keep the legacy generic update-import dialog unchanged; place import has no update switch. The signature is an operational preview strip (new/duplicate/invalid plus located/unlocated), with row-numbered reasons and CSV download. No new color/type theme.

`场所类别` owns `fire_place.place_type`: 重点企业=industry, 高层建筑=highrise, 综合商业体=commercial, 养老机构=elderly. `PLACE_LAYERS` owns frontend labels; backend parser and SQL dictionary match these codes/labels. Legacy spreadsheet labels are accepted as aliases, never guessed from industry category. The user's explicit current Excel classifications take precedence over earlier inferred housing types.

Import is additive only. Header parsing excludes auxiliary coordinate candidates (AK/AL etc.); only 经度/纬度/坐标系 become place coordinates. Source cells including public-address evidence are retained in audit JSON. Existing and in-file similar names are skipped, not merged or updated. Thus a duplicate row with new coordinates does not modify an unlocated existing record. Coordinates remain review and are map-visible when valid GCJ02; absent coordinates remain pending and list-visible.

Preview/confirm require fire:places:import plus fire:places:list. Owner-bound DB batch, 2-hour expiry, global transaction mutex and commit-time recheck prevent concurrent duplicates. Completed batches return their durable original result on retry. New records inherit current department and importing user for data scope. Duplicate details outside administrator access are masked. No anonymous endpoint, no demo writes. Runtime import transaction includes contacts and source audit.

## Temporary 119 display scope 2026-10-26

The user requested all received 119 records for diagnosis, without Dachang geographic filtering. fire.incident119.dachang-only=false controls this on the server; role/data scope and the existing local handling workflow stay unchanged. Boundary classification remains stored. Shared incident119Status text reports all-scope counts, latest alarm time and latest received time separately. No claim that a newly received historical record is a new incident. List, detail and map receive the same expanded authorized scope; invalid coordinates stay list-visible without invented map points.

Live video defaults to FLV via /open/token/cloud/getDeviceMediaUrlFlv and lazy mpegts.js 1.8.0. LiveVideoDialog is the canonical player for dashboard and administration. Retain permission/account/device checks, stale-request protection, explicit reconnect, real decoded-video readiness, codec/error/timeout feedback, and explicit VLC download. No automatic HLS or WebRTC fallback. Closing or switching destroys MSE playback, aborts its fetch and deletes the owner-bound backend FLV session. The backend forwards continuous chunks without whole-stream buffering; vendor signed URLs and tokens stay server-side. FLV sessions allow one authenticated stream and expire before first use after 10 minutes; active streams close on cancellation, disconnect or 20-second upstream idle timeout. H.265 requires browser/OS decoder support; no implied transcoding. The previous WebRTC SDK and relay remain for explicit diagnostic callers only.
