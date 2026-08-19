# Block 453 — AXIS live-video runbook V1: native `naxisVideo` driver on the Supervisor (licensed path)

**Focus:** video-integration (new document-mode thread). Runbook 1 of 4 (V1). Continues the options matrix in `docs/video-axis-n4-integration.md`.

**Mode:** DOCUMENT (§20) — CAPTURE of an operator how-to, not gap discovery. Outline item = V1.

**Origin:** operator request to land the step-by-step for integrating an AXIS M2025-LE (`172.16.101.31`, operator-owned homelab) into Niagara N4 via the native driver.

**Scope:** the operator procedure + the N4 mechanics it rests on. NOT: a live install of the driver against this camera (not executed here — see Open items); PTZ/DVR tuning depth.

**Evidence base.** N4 mechanics are `[CERT]` file:line in the decompiled corpus (`organized/`, N4.14.0.162). VAPIX endpoints and the camera facts are `[CERT-live]` — verified against the live camera by the camera-side session (cross-session channel). License facts are `[CERT-live]` from the live `Webs.license`. The step SEQUENCE is `[INFER]` — a documented procedure not run against a live station in this work.

---

## 453.1 — What the driver is and what it needs

The AXIS driver ships as module `naxisVideo` ("Integrates Axis Video Cameras"). `[CERT]` `naxisVideo-rt/META-INF/module.xml`. Its network component gates on a license feature:

- `BAxisVideoNetwork` → `Sys.getLicenseManager().getFeature("tridium", "axisVideo")`. `[CERT]` `sources/decompiled/video/BAxisVideoNetwork.java:139`
- The framework layer `nvideo` gates on `checkFeature("tridium","videoDriver")`. `[CERT]` `sources/decompiled/video/BSimpleVideoCamera.java:50`

Both features are present on the **Supervisor** host: `axisVideo` `camera.limit="32"` `expiration="2027-03-31"`, plus `videoDriver`. `[CERT-live]` `Webs.license`. The **JACE** (QNX-TITAN) has neither — only `niagaraDriver`/`obixDriver`. `[CERT-live]` **→ V1 runs on the Supervisor, not the JACE.**

Transport: the driver streams **RTSP/H.264** (`RtspConnection` over `baseRtsp`, `RtspStream`) and controls the camera over **VAPIX HTTP with Digest** (`AxisHttpHelper`, `Digest`). It resolves Digest itself via `RtspUsername`/`RtspPassword`. `[CERT]` `sources/decompiled/video/BAxisVideoCamera.java:239`. The Workbench view decodes MJPEG or MPEG-4 (`AxisVideoMjpegDecoder`, `AxisFfmpegMpeg4Decoder`, view `BAxisVideoAgent`); the web profile uses the bajaux widget `BNAxisVideoStream` / framework `BIVideoStream`. `[CERT]` `naxisVideo-wb/ui/*`, `naxisVideo-ux/.../BNAxisVideoStream.java`.

**Auth aside (cross-finding).** The camera's HTTP Digest is *standard* RFC 7616 (the driver handles it). N4's OWN "Digest" login is a different protocol — **SCRAM**: `BDigestAuthenticationScheme` configures `DigestLoginModule`, which drives `ScramServer`/`ScramServerCallback`. `[CERT]` `sources/decompiled/video/BDigestAuthenticationScheme.java:44`, `sources/decompiled/video/DigestLoginModule.java:26`. So an RFC-7616 client (e.g. Python `HTTPDigestAuthHandler`) authenticates to the camera but NOT to an N4 station — confirmed empirically by the camera-side session (client inert, all paths → `/prelogin`). `[CERT-live]`. Irrelevant to V1 itself (the driver talks to the camera), but it settles how anything external could talk to the *station*.

## 453.2 — Prerequisite: network reach from the Supervisor

The Supervisor must have an L3 route to `172.16.101.31` on **RTSP 554 + HTTP 80/443**. The existing `localhost:8000` MJPEG relay does **not** help V1 — the driver speaks RTSP, which the relay does not expose. `[INFER]` (mechanism `[CERT]`, applicability inferred). Options, in order of cleanliness:

1. VPN / subnet-router (Tailscale/WireGuard on the mini-PC bridge) so the Supervisor reaches `172.16.101.0/24` directly at L3. Best fit — full RTSP+HTTP.
2. A tunnel/port-forward that carries **both** 554 and 80 to the camera (SSH `-L` for each, or a Cloudflare ingress) — heavier, must map RTSP data ports too.

## 453.3 — Procedure `[INFER]`

1. In Workbench, open the Supervisor station. Confirm the license: **Platform → license** shows `axisVideo` (or `Config → Services → LicenseService`). If absent, stop — this host cannot run V1.
2. Open the `naxisVideo` palette. Drag **`AxisVideoNetwork`** under `Config/Drivers` (`DriverContainer`).
3. Under the network, add an **`AxisVideoCamera`** device. Set its properties: `hostName=172.16.101.31`, `controlPort`/`dataPort` (defaults per palette), `useTcpTransport` (true if RTSP-over-TCP is needed through the tunnel), and credentials `RtspUsername=root` + `RtspPassword` (from the operator vault — enter by value in the secure field, never commit it). `[CERT]` prop names from `BAxisVideoCamera`.
4. Ping/learn: the device should come `{ok}`. Use its **`BAxisVideoAgent`** view to confirm live decode in Workbench.
5. For a web/Px live view: place the camera's video component on a Px view; it renders through `BNAxisVideoStream` in the Hx/HTML5 profile. PTZ/resolution use `BAxisVideoPanTiltZoomSettings` / `BAxisVideoResolutionSettings`. `[CERT]` class names.

## 453.4 — Self-verify

| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | AXIS driver = module `naxisVideo`, gates on `getFeature("tridium","axisVideo")` | `[CERT]` | `sources/decompiled/video/BAxisVideoNetwork.java:139` |
| 2 | Framework `nvideo` gates on `checkFeature("tridium","videoDriver")` | `[CERT]` | `sources/decompiled/video/BSimpleVideoCamera.java:50` |
| 3 | Supervisor has `axisVideo` (32 cam, exp 2027-03-31) + `videoDriver`; JACE has neither | `[CERT-live]` | `Webs.license` |
| 4 | Driver streams RTSP/H.264 + VAPIX Digest; handles Digest itself | `[CERT]` | `sources/decompiled/video/BAxisVideoCamera.java:239` (`RtspUsername`) |
| 5 | Workbench decodes MJPEG/MPEG4 (`BAxisVideoAgent`); web via `BNAxisVideoStream` | `[CERT]` | `naxisVideo-wb/ui/*`, `naxisVideo-ux/*` |
| 6 | Supervisor needs L3 RTSP 554 + HTTP route; relay does not satisfy it | `[INFER]` | mechanism claim 4 + relay is MJPEG-only |
| 7 | Step sequence (add network→camera→creds→view) | `[INFER]` | not run against a live station here |

**Tally:** 7 claims — 3 `[CERT]` · 1 `[CERT-live]` (claim 3; claim 4 also code) · 2 `[INFER]` · 0 unmarked. Consistent with `docs/video-axis-n4-integration.md`.

**Left out (named):** M2025-LE model-binding confirmation; PTZ/DVR/event-historization setup; exact default `controlPort`/`dataPort` values.

## 453.5 — Connections
- Lands **V1** of the four-option matrix (`docs/video-axis-n4-integration.md`). Siblings: [B454] (V2 embed), [B455] (V3 snapshot), [B456] (V5 custom module).
- License facts corroborate `[[video-axis-licensing]]` and the QNX-TITAN/Win split.

## 453.6 — Open gaps
- **B453-G1** — live validation: does `naxisVideo` bind the M2025-LE, and is RTSP reachable from the Supervisor through the chosen transport? Becomes `[CERT-live]` when exercised (moves this runbook from `[INFER]` to tried).
