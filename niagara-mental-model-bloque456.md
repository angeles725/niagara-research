# Block 456 — AXIS live-video runbook V5: a custom N4 module (own driver), no commercial video feature

**Focus:** video-integration (document-mode thread). Runbook 4 of 4 (V5). Sibling of [B453]/[B454]/[B455].

**Mode:** DOCUMENT (§20) — CAPTURE of an operator how-to + architecture comparison. Outline item = V5.

**Origin:** operator question — can we DEVELOP our own N4 module (Niagara SDK/Workbench, Java) that pulls the MJPEG/snapshot and exposes it in a Px/Hx view, so we depend on neither the commercial video driver nor its license feature? Legitimate own-development, not license evasion.

**Scope:** feasibility, the LICENSE reality of running a custom module, and an honest effort comparison. NOT: a full module implementation.

**Evidence base.** devkit/signing/platform facts `[CERT]` file:line + this project's prior blocks [B40]/[B400]/[B434]. Step SEQUENCE and effort `[INFER]`.

---

## 456.1 — Feasibility: yes

A custom module that HTTP-GETs the camera's snapshot/MJPEG (with Digest) and publishes it as a component property / image / points is standard Niagara module development:

- **Scaffolding present.** The Niagara Developer Kit (`devkit-wb`) ships `BNewModuleTool`, `BNewDriverTool` (`NewModuleWizard`/`NewDriverWizard`) and `Slotomatic` (the slot code generator). `[CERT]` `sources/decompiled/video/Slotomatic.java:21`; corroborates [B434] (devkit = developer tooling, `preferredSymbol="dev"`, `runtimeProfile="wb"`).
- **HTTP + Digest available.** The platform has Digest handling (`BHttpDigestCallbackHandler`, `web-rt/authn`) and plain `java.net` for outbound GET; the camera uses standard RFC 7616 Digest, which `HttpURLConnection`/an Apache client resolves directly. `[CERT]` class exists + `[INFER]` (standard Java outbound). (Note: this is the CAMERA's Digest — trivial; unrelated to N4's own SCRAM login, [B453].)
- **Publish surface.** A `BComponent` subtype exposes properties/points; a bajaux widget (like the driver's own `BNAxisVideoStream`, [B453]) renders in Px/Hx.

So a slimmed re-implementation of "fetch + expose" is clearly viable. `[INFER]` (viability from the pieces, not built here).

## 456.2 — The LICENSE reality of this path (the crux)

Running a custom module needs **no video/ONVIF license feature**. The license gates `checkFeature(...)` calls (drivers self-declare them, [B453] §453.1); a module you write that makes no such call consumes no feature. `[INFER-strong]` (no "load-any-module" feature gate found).

The real gate is **code signing**, not licensing:

- Station modules carry `META-INF/*.{RSA,DSA,EC}` and the signature sets the module's permission group. `[CERT]` [B400].
- This install already has a **developer signing keystore** — the truststore was replaced by an audit/dev keystore holding one RSA-2048 self-signed entry, alias **`niagaramoduledev`**, intent: bootstrap custom-signed modules. `[CERT]` [B40]. So custom modules can be signed and trusted **on this station today**.
- No "developer license feature" exists: building needs Workbench (already licensed) + the Developer Kit (present). `[INFER]`+`[CERT]` (devkit present).

**→ V5 is signing-gated, not feature-gated.** It runs where you can sign+trust the module — including the **JACE**, which has no video feature. `[CERT-live]` (JACE feature set) + `[CERT]` (signing model).

## 456.3 — Effort vs the alternatives (honest)

| | V2/V3 embed | **V5 custom module** | V1 native driver |
|---|---|---|---|
| Code | none | real Java + Gradle niagara plugin, sign, deploy | none |
| License | none | none (signing only) | `axisVideo` (already owned, Supervisor) |
| N4 integration | iframe/image only | native points, alarms, control | full (PTZ/DVR/events) |
| Runs on JACE | yes | yes | no (no feature) |
| Maintenance | trivial | **you own it across N4 upgrades** | Tridium owns it |

Honest read: since `axisVideo` is **already paid** on the Supervisor, V5 is mostly justified only to (a) run camera video/points on the **JACE**, or (b) stay portable to stations that never buy the feature. For "just see the camera" it is **over-engineering** versus V2's zero-code iframe. It earns its cost only when the camera's data must be **first-class N4 points/alarms/control** AND paying for / depending on the commercial driver is to be avoided. `[INFER]`.

## 456.4 — Procedure sketch `[INFER]`

1. In Workbench (Developer Kit installed), run **New Module** (`BNewModuleTool`) → then **New Driver** (`BNewDriverTool`) if you want a network/device tree; Slotomatic generates the slot boilerplate.
2. Implement a `BComponent`/device that on a poll interval does `GET image.cgi` (or opens the MJPEG multipart) against the camera with Digest, and stores the frame as a `BImage`/property, plus points (online status, last-frame time, motion if polled from VAPIX events).
3. Optionally add a bajaux widget to render the frame/stream in Px/Hx.
4. Build the module, **sign** it with the `niagaramoduledev` dev cert (jarsigner / the Niagara signing tooling), install to the station's `modules/`, and add the component from your module's palette.

## 456.5 — Self-verify

| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | Developer Kit present: `BNewModuleTool`/`BNewDriverTool`/`Slotomatic` | `[CERT]` | `sources/decompiled/video/Slotomatic.java:21`; [B434] |
| 2 | Platform has Digest + java.net for outbound camera GET | `[CERT]`+`[INFER]` | `BHttpDigestCallbackHandler` + standard Java |
| 3 | Custom module consumes NO video/ONVIF feature (no `checkFeature`) | `[INFER-strong]` | no load-gate found; feature gates are self-declared |
| 4 | Real gate = code signing; modules carry `META-INF/*.{RSA,DSA,EC}` | `[CERT]` | [B400] |
| 5 | This station has dev signing keystore `niagaramoduledev` (RSA-2048) | `[CERT]` | [B40] |
| 6 | V5 is signing-gated → can run on the JACE (no feature needed) | `[CERT]`+`[CERT-live]` | signing model + JACE feature set |
| 7 | Since `axisVideo` already owned, V5 justified mainly for JACE/portability | `[INFER]` | comparison vs [B453] |
| 8 | Procedure sketch (new module → fetch+publish → sign → install) | `[INFER]` | not built here |

**Tally:** 8 claims — 3 `[CERT]` · 1 `[CERT-live]`/mixed · 4 `[INFER]`/mixed (labelled) · 0 unmarked. Consistent with [B40]/[B400]/[B434] and the matrix.

**Left out (named):** the actual module code; the exact signing command/cert chain for deploy; whether the station's signing policy accepts self-signed dev certs in production vs a lowered policy.

## 456.6 — Connections
- Lands **V5** of the matrix and closes the four-runbook outline ([B453]–[B456]).
- Rests on the signing research: [B40] (SEJOFA dev keystore), [B400] (signed-vs-unsigned permission gating), [B434] (devkit = dev tooling). Contrast the licensed [B453] and the zero-code [B454]/[B455].

## 456.7 — Open gaps
- **B456-G1** — production signing policy: does this station accept a `niagaramoduledev` self-signed module at full permissions, or does an unsigned/self-signed module get a reduced permission group that would block outbound HTTP? Investigable against the signing policy ([B400] thread).
