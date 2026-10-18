# SDD ledger — plan: outputs/消防平台前端实施计划.md
Ruling: source is a non-Git extracted project; use targeted file backups and manifest, not worktrees or commits, as approved in plan.
Pre-flight: tasks 2–7 consume task 1 service and stable entity IDs; consistent.
Pre-flight: tasks 3–7 share placeId/gridId/deviceId/cameraId; use single repository for all aggregation.
Pre-flight: task 8 production authentication must remain active; mock requires DEV and explicit flag.
Ruling: consolidate domain rule tests up front to exercise cross-module invariants before implementing shared repository. UI tasks remain sequential.
Task 1: in progress.
Task 1: complete — service split, mock-mode gate and persistent repository; domain tests green.
Task 2: complete — place/person/material/hazard/task CRUD and shared place dialog.
Task 3: implemented — schematic GIS, grids, drawing, map picking; AMap adapter added, awaiting credentials for external validation.
Task 4: complete for mock scope — device binding, preview, capture and inspection verified in browser; production talk/vendor transport needs integration.
Task 5: complete — alarm accept/close verified in browser, timeline and counts update; messages/SMS rules available.
Task 6: complete for mock scope — QR opened H5, submitted test duty and verified in management list. Cross-browser public shares require backend.
Task 7: implemented — responsive dashboard; browser narrow-layout checked.
Review: five findings accepted. Added latest-request guard, reverse-reference checks, permission-aware relation fetches, action permission guards, host-only statistics. Regression suite: 12 passed.
Ruling: QR graph installed in isolated work dependency directory after existing Yarn remote tarballs were rejected by npm; copied QR dependency graph nested under qrcode to avoid overwriting existing transitive dependencies; Yarn lock appended with registry resolutions.
Task 8: in progress — documentation, production auth check and final build.
Task 8: complete — 16 tests passed, production build passed, mock excluded, production unauthenticated route redirected to login. Documentation and handoff prepared.
