# Block 454 — AXIS live-video runbook V2: embed the MJPEG relay in a Px Web Widget (license-free path)

**Focus:** video-integration (document-mode thread). Runbook 2 of 4 (V2). Sibling of [B453]/[B455]/[B456].

**Mode:** DOCUMENT (§20) — CAPTURE of an operator how-to. Outline item = V2.

**Origin:** operator request for a route that needs **no** N4 video feature — reuse the existing MJPEG relay by embedding it in a Px view.

**Scope:** the embed procedure + the N4 widgets and the URL-whitelist gate. NOT: hardening the relay; the relay's internals (owned by the camera-side session).

**Evidence base.** N4 widget/class facts `[CERT]` file:line (`organized/`). Relay behaviour (`localhost:8000`, Digest stripped, MJPEG re-served) is `[CERT-live]` from the camera-side session. Step SEQUENCE `[INFER]`.

---

## 454.1 — The mechanism and why it is license-free

A Px view can host an embedded browser / web widget pointing at an arbitrary URL. The relevant N4 components:

- **`BHxPxWebBrowser`** and **`BHxWebWidget`** — the Px web widgets rendered in the **Hx/HTML5 web profile**. `[CERT]` `sources/decompiled/video/BHxPxWebBrowser.java:37`, `sources/decompiled/video/BHxWebWidget.java`.
- **`BUxWebBrowser`** — the bajaux web-browser widget. `[CERT]` `bajaui-ux/.../BUxWebBrowser.java`.
- **`BJxWebBrowserImpl`** — the Workbench-embedded Chromium (JxBrowser) for viewing inside Workbench. `[CERT]` `jxBrowser-wb/.../BJxWebBrowserImpl.java`.

None of these call `checkFeature` for a video feature — they belong to baseline `hx`/`bajaui`/`jxBrowser` modules that ship with every station. `[INFER-strong]` (no feature-gate found on them). **→ V2 needs no `axisVideo`/`videoDriver` license and therefore also works on the JACE**, which lacks those features. `[CERT-live]` (JACE feature set) + `[INFER]` (baseline widgets).

The relay does the auth heavy-lifting: it terminates the camera's RFC 7616 HTTP Digest and re-serves MJPEG **without auth** on `http://localhost:8000`. `[CERT-live]` (camera-side session). So the Px widget only needs to reach the relay — no Digest, no RTSP. (N.B. the camera's Digest is *standard* HTTP Digest; N4's own "Digest" login is SCRAM — unrelated here since the widget consumes the relay, not an N4 login. See [B453]/the auth note.)

## 454.2 — The gate to watch: URL whitelist

External URLs embedded in the web profile are subject to **`com.tridium.security.UrlWhitelist`**. `[CERT]` class exists in `baja`. If the relay URL is not allowed, the iframe is blocked. The relay host/port must be added to the station's URL whitelist (WebService / allowed-hosts configuration). `[INFER]` (mechanism `[CERT]`, exact config path not transcribed).

## 454.3 — Procedure `[INFER]`

1. Ensure the relay is reachable from where the Px page is rendered: for the **Hx web profile**, that is the browser client's host + the station; for **Workbench** (`BJxWebBrowserImpl`/`BUxWebBrowser`), the Workbench host. Use a relay URL both can resolve (not `localhost` if the viewer is on another machine — bind the relay to a routable address or tunnel it).
2. Whitelist the relay origin in the station's `UrlWhitelist` (WebService config). Without this the embed is blocked.
3. In the Px editor, add a **Web Widget / PxWebBrowser** (from the `hx` palette) and set its URL to the relay stream page (e.g. `http://<relay-host>:8000/`). For a bare MJPEG endpoint, point at the relay's HTML page that wraps `<img src="/stream">` rather than the raw multipart stream.
4. Size the widget on the canvas; save. View the Px in the **Hx/HTML5 profile** (web browser). The MJPEG renders as a continuous image via `multipart/x-mixed-replace`.
5. For Workbench-only viewing, the same URL in the JxBrowser widget works without the Hx profile.

## 454.4 — Self-verify

| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | Px can embed a URL via `BHxPxWebBrowser`/`BHxWebWidget` (Hx web profile) | `[CERT]` | `sources/decompiled/video/BHxPxWebBrowser.java:37` |
| 2 | bajaux `BUxWebBrowser` + Workbench `BJxWebBrowserImpl` also embed URLs | `[CERT]` | `bajaui-ux/*`, `jxBrowser-wb/*` |
| 3 | These widgets are baseline; no video feature gate → works on the JACE too | `[INFER]`+`[CERT-live]` | no feature-check found + JACE feature set |
| 4 | External URLs gated by `UrlWhitelist`; relay origin must be whitelisted | `[CERT]` | `com.tridium.security.UrlWhitelist` |
| 5 | Relay strips camera Digest, re-serves MJPEG unauth at localhost:8000 | `[CERT-live]` | camera-side session |
| 6 | Step sequence (whitelist → add widget → set URL → Hx view) | `[INFER]` | not run against a live station here |

**Tally:** 6 claims — 3 `[CERT]` · 1 `[CERT-live]` · 2 `[INFER]`/mixed (labelled) · 0 unmarked. Consistent with [B453] and `docs/video-axis-n4-integration.md`.

**Left out (named):** exact `UrlWhitelist` config path; whether the Hx profile enforces mixed-content/TLS rules against an http relay; relay hardening.

## 454.5 — Connections
- Lands **V2** of the matrix. License-free sibling of the licensed [B453]. Contrast [B455] (snapshot, lighter) and [B456] (custom module, native points).
- Auth distinction (camera Digest vs N4 SCRAM) cross-refs the auth finding in [B453] §453.1 and the SCRAM note.

## 454.6 — Open gaps
- **B454-G1** — live validation: does the Hx profile embed the http relay without mixed-content blocking, and what is the exact `UrlWhitelist` entry? Becomes `[CERT-live]` when exercised.
