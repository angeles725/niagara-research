# Block 455 — AXIS live-video runbook V3: PxImage snapshot refresh (lightest, license-free)

**Focus:** video-integration (document-mode thread). Runbook 3 of 4 (V3). Sibling of [B453]/[B454]/[B456].

**Mode:** DOCUMENT (§20) — CAPTURE of an operator how-to. Outline item = V3.

**Origin:** operator request for the lightest possible way to "see the camera" — a periodically refreshed still image instead of a stream.

**Scope:** the snapshot-in-Px procedure + the kitPx image-binding mechanics. NOT: real-time/audio/PTZ (V3 is deliberately a frame refresh, not video).

**Evidence base.** kitPx widget/class facts `[CERT]` file:line. VAPIX snapshot endpoint `[CERT-live]` (camera-side session). Step SEQUENCE `[INFER]`.

---

## 455.1 — Mechanism: an ORD-bound image widget

Niagara Px renders an image bound to an ORD via kitPx:

- **`BOrdToImage`** / **`BOrdToImageTypeExt`** — resolve an ORD to a displayable image. `[CERT]` `sources/decompiled/video/BOrdToImage.java:34`.
- **`BImageButton`** / **`BUxImageButton`** — an image widget variant. `[CERT]` `kitPx/.../BImageButton.java`.

The source is the AXIS **VAPIX snapshot** endpoint: `http://172.16.101.31/axis-cgi/jpg/image.cgi` — a single JPEG per request. `[CERT-live]` (camera-side session). To avoid the camera's HTTP Digest inside Px, point the widget at the **relay's** snapshot URL (auth already stripped), not the camera directly. `[INFER]` (relay behaviour `[CERT-live]`).

Like V2, these are baseline `kitPx` widgets with **no video feature gate** — V3 is license-free and works on the JACE. `[INFER-strong]`+`[CERT-live]` (JACE feature set).

## 455.2 — Weight

At `640x360` JPEG, a snapshot every N seconds is ~tens of MB/hour (vs ~0.6 GB/h for MJPEG@5fps, `[CERT-live]` camera-side). Trade-off: it is **not video** — low effective frame rate, no audio, no PTZ, no events. Best for "is it alive / periodic check", dashboards, and thumbnails. `[INFER]`.

## 455.3 — Procedure `[INFER]`

1. Confirm the relay exposes a stable snapshot URL (or use the camera's `image.cgi` only if the station can satisfy Digest — normally prefer the relay). Whitelist the origin in `UrlWhitelist` if the profile enforces it (as in [B454]).
2. In the Px editor, add an image widget bound via `BOrdToImage` to the snapshot URL as an ORD (e.g. an `ip:`/`http:` ORD to the relay page, or a local point holding the URL).
3. Give it a refresh: drive periodic re-fetch either by the widget's refresh facet or by binding the image ORD to a value that ticks every N seconds (a `NumericInterval`/`Ramp`-style trigger or a Px refresh timer). `[INFER]` — exact refresh facet not transcribed.
4. Size and save. In both Workbench and the Hx/web profile the widget shows the latest JPEG, updating each interval.

## 455.4 — Self-verify

| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | Px binds an ORD to an image via kitPx `BOrdToImage`/`BOrdToImageTypeExt` | `[CERT]` | `sources/decompiled/video/BOrdToImage.java:34` |
| 2 | AXIS snapshot endpoint = `/axis-cgi/jpg/image.cgi`, one JPEG/request | `[CERT-live]` | camera-side session |
| 3 | Use the relay's snapshot URL to avoid camera Digest inside Px | `[INFER]` | relay behaviour `[CERT-live]` |
| 4 | Baseline kitPx widgets, no video feature gate → license-free, JACE-capable | `[INFER-strong]`+`[CERT-live]` | no feature-check + JACE feature set |
| 5 | Snapshot ~tens of MB/h vs MJPEG ~0.6 GB/h @640x360x5 | `[CERT-live]`+`[INFER]` | camera-side data + arithmetic |
| 6 | Step sequence (bind ORD → set refresh → view) | `[INFER]` | not run against a live station here |

**Tally:** 6 claims — 1 `[CERT]` · 1 `[CERT-live]` · 4 `[INFER]`/mixed (labelled) · 0 unmarked. Consistent with [B454] and the matrix.

**Left out (named):** the exact Px refresh-facet/timer wiring; whether `BOrdToImage` accepts a raw http ORD or needs a point/URL indirection.

## 455.5 — Connections
- Lands **V3** of the matrix — the lightest license-free option. Shares the relay + `UrlWhitelist` mechanics with [B454]; contrasts the licensed stream [B453] and the custom module [B456].

## 455.6 — Open gaps
- **B455-G1** — live validation: confirm `BOrdToImage` binds an http snapshot ORD and the refresh cadence works in the Hx profile. Becomes `[CERT-live]` when exercised.
