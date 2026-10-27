# Official voice demo adapter

Source: docs/talkapi/语音对讲demo-20240704/src/utils/broadcastSdk.js
Original SHA256: adf703266d2f2e23d40d301e9cd4325ee9837bec277473e8361eb671b212a24a

Keep the bundled Recorder implementation, its PCM resampling and the exact vendor A-law encoder. Global export renamed initFireTalkSDK. The connection adapter uses the demo's exact 10000/10006 JSON and 8000 Hz / 320-byte A-law frames. Use a valid power-of-two 2048 capture buffer for ScriptProcessor fallback. Add lifecycle callbacks, complete-frame draining, bounded socket backlog, idempotent teardown, and release consumed recorder buffers. Remove the original send-after-close and optional HTTP heartbeat/login/device helpers (not used in the official demo). Recorder telemetry is disabled before recorder construction. No account secret is embedded here. Gateway status payloads are not assigned guessed success meanings.
