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