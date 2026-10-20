# Fire map interaction contract

The shared MapPanel is the owner of resource-layer toggles. Preserve its current two-column label arrangement and Element Plus feedback/detail primitives. Selection is local transient map state, defaults to all four place layers off, and is not a persistent database mutation.

Native buttons support Enter/Space, aria-pressed, visible keyboard focus, and independent multiple selection. Marker removal is immediate and does not depend on a new network request. The latest selection wins when data loads. Markers use class glyphs and names, not color alone.

Use a stable panel footer for loading, login, retry and displayed count. Keep other independent overlays (boundary/POI/traffic) intact when switching place types. Never show demo places as a successful response for a real-data failure. Detail dialogs reuse PlaceDetail and restore focus through Element Plus.

Verification owners: tests/fire/place-layers.test.mjs; tests/fire/map-layers.browser.cjs; docs/verification/map-place-layers-browser.json. Live deployment additionally requires the RuoYi backend, imported data/coordinates, a valid login and fire:places:list permission.