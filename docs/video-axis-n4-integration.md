# Integrating a live AXIS camera into Niagara N4 — options matrix (how-to, document mode §20)

**Scope.** Bring live video from an AXIS M2025-LE (`172.16.101.31`, homelab `172.16.101.0/24`,
operator-owned) into a Niagara N4 station / Px view. Camera formats: JPEG, MJPEG, H.264.
Auth: HTTP Digest (user `root`, password by role — held in operator vault). A local relay already
re-serves MJPEG without auth at `http://localhost:8000`.

**Evidence base.** Corpus `organized/` (N4.14.0.162 decompiled), live `Webs.license`, VAPIX endpoints
verified live by the camera-side session. Markers per METHODOLOGY §3.

---

## 0. Facts established (cited)

- Native AXIS driver `naxisVideo` connects by **RTSP/H.264** for the stream and **VAPIX HTTP with
  Digest** for control. [CERT] `naxisVideo-rt`: `RtspConnection`, `RtspStream`, `RtspUsername`,
  `RtspPassword`, `AxisHttpHelper`, `Digest`.
- It ships its **own web video widget** `BNAxisVideoStream` (bajaux) plus the framework
  `BIVideoStream` — so live view renders in the **Hx/HTML5 web profile**, not only in Workbench.
  [CERT] `naxisVideo-ux/…/BNAxisVideoStream.java`, `videoDriver-ux/…/BIVideoStream.java`.
- License enforcement: `nvideo` calls `checkFeature("tridium","videoDriver")`; `naxisVideo` calls
  `getFeature("tridium","axisVideo")`. [CERT] `BVideoNetwork.java`, `BAxisVideoNetwork.java`.
- The **live** `Webs.license` (`C:\Honeywell\OptimizerSupervisor-N4.14.0.162\security\licenses\`)
  grants `axisVideo` (`camera.limit="32"`, `expiration="2027-03-31"`), plus `videoDriver`,
  `milestoneVideo`, `maxpro`, `remoteVideo`. [CERT-live]. The JACE host (QNX-TITAN) has **no** video
  feature — only `niagaraDriver`, `obixDriver`. [CERT-live] → the native driver runs on the
  **Supervisor**, not the JACE.
- **No generic ONVIF module** exists in this install; there is `baseRtsp` plus per-manufacturer
  drivers (`naxisVideo`, `milestoneVideo`, `maxpro`, `remoteVideo`). [CERT] module listing.
- Px can embed an external URL via the Hx web widgets `BHxPxWebBrowser` / `BHxWebWidget` (web profile
  only). External URLs may be subject to `com.tridium.security.UrlWhitelist`. [CERT] classes exist.
- AXIS model M2025-LE compatibility with `naxisVideo` is **not hardcoded** in a model table; the
  driver is VAPIX/RTSP-generic, so it should bind, but this is **not verified live**. [INFER]

---

## 1. Options matrix

### V1 — Native `naxisVideo` driver (AxisVideoNetwork)  ·  `tried: not-executed (capability+license CERT)`
Add `AxisVideoNetwork` under the station → add `BAxisVideoCamera` (host `172.16.101.31`,
`RtspUsername=root`, `RtspPassword`=vault). Driver opens RTSP for the stream and VAPIX/Digest for
control/PTZ. Drop the camera's video widget on a Px view; it renders in Hx/web via `BNAxisVideoStream`.

- **Pros:** first-class N4 integration — PTZ, events, alarms, recording/DVR, one pane in the station;
  driver handles Digest itself; licensed (`axisVideo`, 32 cameras, valid to 2027-03-31); native web
  widget (no iframe hacks); survives as a real component (bindable, historizable events).
- **Cons:** the **Supervisor** must have an L3 route to the camera on **RTSP 554 + HTTP 80** — the
  existing `localhost:8000` MJPEG relay does **not** satisfy this (driver speaks RTSP, not the relay's
  MJPEG). Runs only where the feature is (Supervisor, not JACE). M2025-LE binding unverified live.

### V2 — Embed the MJPEG relay in a Px Web Browser widget  ·  `tried: not-executed (widgets CERT)`
In a Px view use `BHxPxWebBrowser` / `BHxWebWidget` pointing at the relay (`http://<relay-host>:8000`).
Renders in the Hx/HTML5 web profile.

- **Pros:** reuses the relay as-is; **no driver, no license**; works for the JACE too (it is just the
  browser rendering an iframe); relay already strips Digest (auth-free source).
- **Cons:** web profile only (not the Workbench Px canvas); it is an **iframe of an external app**, not
  a native N4 component — no PTZ/events/alarms/recording; must whitelist the URL (`UrlWhitelist`); relay
  must stay reachable and running.

### V3 — PxImage / image binding refreshing the snapshot  ·  `tried: not-executed`
A Px image widget bound to the snapshot URL (via relay to avoid Digest:
`http://<relay>/…/jpg/image.cgi`), refreshed every N seconds.

- **Pros:** ultra-light (~40 MB/h), simplest; works in both Workbench Px and web; works on the JACE;
  no license.
- **Cons:** not real video (frame refresh, low fps); no audio/PTZ/events.

### V4 — RTSP → gateway (go2rtc / MediaMTX) → embed WebRTC/HLS  ·  `tried: not-executed`
Camera-side session's transport option. Gateway repackages RTSP to WebRTC/HLS/MJPEG; N4 embeds it via
`BHxPxWebBrowser`.

- **Pros:** efficient H.264 (10–20× less data), low latency (WebRTC), multi-viewer, still no N4 video
  license.
- **Cons:** extra infra (the gateway); inside N4 it is still an embedded iframe (no native integration).

### V5 — Custom N4 module (own Java module/driver, no commercial video feature)  ·  `tried: not-executed`
Build a small module: a `BComponent` (or minimal driver) that HTTP-GETs the snapshot/MJPEG with Digest
and publishes it as a property / `BImage` and/or as points; optionally a custom bajaux Px widget for the
view. Scaffolded with the Niagara Developer Kit (`devkit-wb`: `BNewModuleTool`/`BNewDriverTool`,
Slotomatic). [CERT] devkit present (`B434`). Platform has Digest handling (`BHttpDigestCallbackHandler`)
and outbound HTTP via `java.net`. [CERT]

- **License:** running a custom module needs **no video/ONVIF feature** — the license gates
  `checkFeature(...)` calls, and your module makes none. The real gate is **code signing**: station
  modules carry `META-INF/*.{RSA,DSA,EC}` and signing sets the permission group (`B400`); this station
  already has a **dev signing keystore** (`SEJOFA / niagaramoduledev`, RSA-2048) set up to trust
  custom-signed modules (`B40`). No "developer license feature" — you need Workbench (licensed, present)
  + the Developer Kit tooling (present). [CERT]/[INFER]
- **Pros:** first-class N4 without buying the commercial driver — camera data as native points (online
  status, motion), alarms, control; no external relay/gateway process; can run on the **JACE** (which
  lacks the `axisVideo` feature); portable to stations without the video feature.
- **Cons:** real development — Java + Gradle niagara plugin, sign, deploy, and **maintain across N4
  upgrades**; you re-implement a slice of what the licensed driver already does.

---

## 1b. Licensing split — baseline (free) vs licensed feature

| Path | N4 components used | Needs a licensed feature? |
|------|--------------------|---------------------------|
| V1 native driver | `naxisVideo` / `nvideo` / `videoDriver` | **YES** — `tridium:axisVideo` (+ `videoDriver`). Present on Supervisor, absent on JACE. [CERT-live] |
| V2 embed relay | Px + Hx `BHxPxWebBrowser` / `BHxWebWidget` | **NO** — baseline (ships with every station; no `checkFeature`) [INFER-strong] |
| V3 snapshot refresh | Px image widget | **NO** — baseline |
| V4 gateway + embed | Hx web widget (N4 side) | **NO** — baseline (cost is external gateway, not an N4 feature) |
| V5 custom module | own module + Px/bajaux + devkit | **NO** feature — gated by **code signing** (dev cert already present), not by a license feature |

License-free route to "the same thing" (see the picture without paying for the video driver):
**V2/V3** (embed the relay / refresh the snapshot) for zero code, or **V5** (custom module) when native
points/alarms are wanted without the commercial feature. All are baseline or signing-gated, not
feature-gated.

---

## 2. Answers to the integration questions

1. **N4 mechanism for live view.** Native path → the driver's `BNAxisVideoStream` widget on a Px view
   (Hx/web). Non-native → `BHxPxWebBrowser`/`BHxWebWidget` iframe to the relay (V2), or a Px image
   widget refreshing the snapshot (V3).
2. **Native ONVIF/RTSP driver worth it vs embedding MJPEG?** No generic ONVIF driver here. For AXIS
   specifically the native `axisVideo` driver *is* RTSP under the hood and is licensed — worth it when
   full integration (PTZ/events/recording) is wanted. If the goal is just "see the picture", embedding
   the relay (V2/V3) is lighter and license-free.
3. **Digest + network reach.** Native driver resolves Digest itself (`RtspUsername`/`RtspPassword`) but
   needs an L3 route from the **Supervisor** to the camera (RTSP 554 + HTTP 80) — VPN/subnet-router
   (Tailscale/WireGuard on the bridge) or a tunnel that forwards both ports; the MJPEG relay does not
   help the native driver. Embed path: the relay already handles Digest, so the station/browser only
   needs to reach the relay; watch `UrlWhitelist`.
4. **Snapshot vs continuous stream in Px.** Snapshot refresh (V3) = lightest, for monitoring/"is it
   alive". Continuous stream = real live view via the native widget (V1) or the relay iframe (V2).

---

## 3. Recommendation

- **Want real Niagara integration** (PTZ, events, alarms, recording; consumed on the Supervisor):
  **V1 native driver.** It is the licensed, first-class N4 way; the one cost is giving the Supervisor an
  L3 route to the camera (RTSP+HTTP). Prefer a VPN/subnet-router over the current MJPEG relay because
  the driver needs RTSP, which the relay does not expose.
- **Want "just show it now", possibly on the JACE, reusing the working relay:** **V2** (embed relay in
  `PxWebBrowser`) for a continuous stream, or **V3** (snapshot refresh) for the lightest footprint.
  No license, no RTSP routing.
- **V4** only if efficient multi-viewer H.264 without the native driver becomes a requirement later.
- **V5 (custom module)** honest take: since `axisVideo` is **already licensed** on the Supervisor, a
  custom module is mostly justified only to (a) run video/points on the **JACE** (no feature there), or
  (b) stay portable to stations that never buy the feature. For "just see the camera" it is
  over-engineering versus V2's zero-code iframe; it earns its cost only when the camera's data must be
  first-class N4 points/alarms/control **and** paying for the commercial driver is to be avoided.

**Not verified live (open items):** M2025-LE binding to `naxisVideo`; RTSP reachability from the
Supervisor; `UrlWhitelist` behavior for the embed. Each becomes a `tried:` entry when exercised.

---

## Detailed step-by-step runbooks (corpus blocks)

Each option has a dedicated, cited how-to block (document-mode capture, §20):

- **V1 — native driver:** `niagara-mental-model-bloque453.md`
- **V2 — embed relay in Px Web Widget:** `niagara-mental-model-bloque454.md`
- **V3 — snapshot refresh:** `niagara-mental-model-bloque455.md`
- **V5 — custom module:** `niagara-mental-model-bloque456.md`

State: `RESEARCH-STATE-video.md`. Cross-finding: N4's own "Digest" login is **SCRAM**, not RFC 7616 —
see B453 §453.1 auth aside.
