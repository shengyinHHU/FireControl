# Official RTC assets

Source: docs/videoapi/rtc_player/rtc_player/js, supplied by the video provider.
The player uses SrsRtcPlayerAsync, SDK stream/srcObject, and SDK play/close exactly as the supplied index.html.
Local SDK changes: suppress SDP/signed-URL console logging, apply a 30-second signaling timeout, and abort pending signaling when closing. The signaling path and SDP negotiation are unchanged.
Bundled jQuery and adapter retain their upstream notices; srs.sdk.js is MIT as stated in its header.

A small optional signaling transport hook is used by the project for authenticated same-origin SDP relay because the actual provider gateway lacks browser CORS headers. The default vendor AJAX implementation remains available. SDP generation, remote description and media handling remain official.
