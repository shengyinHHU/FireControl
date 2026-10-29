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

FLV G.711 compatibility: LiveVideoDialog detects only explicit unsupported G.711 A-law/mu-law errors and reconnects the same FLV device once with native audio parsing disabled. api/fire/flvG711.js owns incremental audio-tag parsing, bounded PCM queue and Web Audio scheduling against the video timeline, fed by the same authenticated stream fetch. AAC stays on the original native audio path. G.711 uses the existing Element Plus button/icon styling with 开启声音/关闭声音; Web Audio starts only on user interaction. Close/reconnect removes timers, sources, AudioContext and fetch. No microphone capture, HLS fallback, server transcoder or second audio request.

LiveVideoDialog uses a uniform sound UI for every codec: hide native Chromium volume/mute controls and show the same shared toolbar 开启声音/关闭声音 button only when an audio track exists. G.711 routes through Web Audio; native-supported audio toggles the video muted state. Both update from volumechange. No codec-specific visual variant.

Voice talkback: VideoTalkback is the shared control embedded in LiveVideoDialog for both dashboard and administration, using the existing Element Plus buttons/icons, danger stop action and fire-error/status presentation. Independent fire:video:talk permission plus configured account and listed-device scope are mandatory. api/fire/talkback.js owns a single foreground microphone session. Request microphone permission before the 20-second vendor ticket; lazy-load the supplied 20240704 official Recorder/A-law SDK adapter. Use domain=1, netType=0, 10000 start and 10006 stop, 8000 Hz / 320-byte G.711 A-law frames. Do not infer device capability from its code. UI reports local transmission, never an undocumented gateway result as device-confirmed success. Reject insecure capture contexts; close/cancel/reconnect/device switch/pagehide/unmount stop capture and socket. Late successful tickets are retired without recording. No automatic microphone start/reconnect, recordings, secret logs, fallback to demo audio or account credentials in the frontend. FLV remains the independent listening path; lack of a live audio track is stated.

Voice diagnostics now use confirmed live JSON replies: await method 10001/code 0 before recording, correlate method 10008 target-device status, stop on explicit rejection. VideoTalkback offers a folded diagnostic with input power/peak, locally sent frame/byte counts and gateway/device status, never credentials or recorded audio. These transport counters are not proof of speaker output. Closed-event cleanup must ignore a stale close animation if the dialog has reopened.


## Real inspection posts (2026-10-28)

InspectionManager owns the full post/record/configuration tabs and compact dashboard list. InspectionPostEditor selects a subset of the existing VideoMonitor real directory; each post binds 1–12 cameras and one default. InspectionSession embeds the canonical LiveVideoDialog, retaining FLV, uniform sound controls, VideoTalkback and teardown. There is no second player or automatic microphone start. Switching the outer video/inspection tab unmounts its active content. Keep Element Plus controls and current fire.scss navy/cyan tokens.

Posts, bindings, records and evidence are database-owned in every build mode. Show real loading/empty/error/retry and server permissions/data scope; never fall back to demo records or record an invented inspector, timestamp, screenshot or absence. Capture only decoded video pixels. Playback failure may be recorded as unknown, never automatically absent. Busy saves block close/double submit; request identifiers remain stable across identical retries and work on HTTP intranet pages. HTTPS/localhost is still required for microphone capture.

Historical names, camera identifiers, inspector and server time are snapshots; editing/disabling posts or unbinding cameras cannot rewrite history. History opens authenticated evidence, with a separate explicit action to view current video. Private evidence storage is outside the public profile directory and backed up with the tables. Ordinary roles need inspection list/edit/add and existing video permissions/account scope. SQL is additive and does not seed demo posts or records.

Verification: tools/inspection/verify_project.py uses isolated MySQL, real DataScopeAspect and transactions; tests/fire/inspection.browser.cjs uses controlled HTTP and canvas fixtures, never real cameras or the business DB. Existing video-monitor regression remains required.

Inspection post contacts: optional contactName (100 characters) and contactPhone (64 characters) belong to the post, edited in the shared InspectionPostEditor and returned by scoped post reads. Preserve phone text including landline area codes/extensions. Existing installations use SQL/fire_inspection_contact_upgrade.sql; do not infer or overwrite fire_place contacts. Reuse Element Plus input styling and stack fields on narrow screens.

ContactDialLink is the canonical Element Plus Phone-icon/拨号 link for PlaceDetail and inspection contacts. It opens the fixed https://www.jnrhtx.top/meeting/index?from=Dc in a new tab with noopener/noreferrer, passing no personal data. InspectionContact displays current post name/phone in post/config tables, compact dashboard cards and the active inspection dialog. Missing values use 未填写; no phone means no dial action. Compact cards separate the video button from the dial link, so dialing never starts playback. Do not add current contacts to historical snapshots.

Video route migration (2026-10-28): /fire/video is the dedicated real VideoMonitor page, replacing the simulated cameras ResourcePage. /fire/inspections contains only InspectionManager. Old /fire/inspections?tab=video links redirect with replace to /fire/video, retaining other query/hash values before destination authorization. Navigation and route permission use fire:video:list in every mode; preview/talk retain existing permissions. Dashboard full-monitor links point to /fire/video; its compact monitor tab still reuses VideoMonitor. Leaving the dedicated route unmounts playback/talk. No database migration is needed. This supersedes earlier references to the real-monitor tab living inside the inspection management page.

Dedicated video directory variant: /fire/video uses VideoMonitor directory-tree with Element Plus lazy ElTree in a left directory pane and existing camera cards/pagination on the right. Directory expansion loads real child directories on demand; selecting a node filters by its region id, resets paging and ignores stale responses. 全部视频 resets scope. Directory failures have retry; video requests/errors remain independent. No automatic stream opening or invented child nodes. Compact dashboard and inspection camera picker retain the existing compact/directory selector UI. At small phone widths the tree stacks above the list; at desktop/tablet widths the panes remain side by side.


119 geographic filtering (2026-10-28, supersedes previous source/25-region/all-scope assumptions): user confirmed the 119 source is WGS84 and supplied a 33-vertex GCJ02 scope. Fire119Coordinates owns the locally validated WGS84->GCJ02 formula; Fire119Boundary owns fire/dachang-119-scope.json. Keep original jd/wd/raw payload and separate GCJ02 map coordinates. Inclusion uses the closed 33-point polygon, including edges, never the previous 25-area union or an expanded rectangle/address guess. Other map/community geometry remains separate.

geofence_status/version and boundary_distance_m are derived metadata independent of preserved manual street attribution. Server-owned dachangOnly=true and current rule version constrain the shared list/detail/map query and scope counts; client parameters cannot bypass them. Outside/missing/invalid records remain stored. Old-version records remain pending/hidden until rebuilt from original raw_json. The existing single sync worker rebuilds metadata transactionally in batches of 200 before polling, at most 20 batches per full run/one per quick run. No new incident/history/discovery or alarm/receipt timestamps are manufactured by rebuilding. Guard writes with payload_hash, resume on subsequent rounds, no blocking unbounded startup task.

AlarmDetail uses the existing read-only descriptions table to show a review hint at <=1 metre from the boundary, without changing inclusion. Conversion and this diagnostic are offline; no AMap requests/keys are required for runtime 119 sync. Previously removed verbose sync paragraphs stay removed. Deployment requires SQL/fire_119_gcj02_scope_upgrade.sql before backend restart. Verification: tools/119-secret-key/verify_project.py (isolated SQL, 225 formula vectors, filters, manual separation, bounded reclassification, rollback), tests/fire/incident119.browser.cjs.

119 scope expansion (2026-10-29 user override): include a record when either its current-version converted coordinates are inside the 33-point polygon OR its source address/description literally contains 大厂. Keyword inclusion applies even with missing/invalid/outside/old-version coordinates; never invent map positions. The shared MyBatis dachangIncluded fragment owns list/detail/map and effective scope counts, preserving permission filters and deduplicating both matches. Geometric metadata remains unchanged for audit. Existing rows are immediately included by the read rule, without backfill or a new schema migration. This supersedes the prior geographic-only inclusion rule; other case types/status/clock semantics stay unchanged.
