# Block 604 — SA-G2: the native Security Dashboard JSON is a live, consumable security-posture source at `GET /nss/station/data?ord=<dashboard-component>` — 14 module-contributed sections, 64 items (30 OK / 16 Info / 14 Warning / 4 Alert), and its live verdicts independently CONFIRM B398's static hardening findings from Niagara's own instrument

**Session**: 2026-08-29
**Focus**: `security-audit` (gap SA-G2 — consume the native SecurityDashboard JSON as a live source). §12 DYNAMIC.
**Distribution / live target**: OptimizerSupervisor-N4.14.0.162, `127.0.0.1`, station `PRUEBAS`, `live-install`
→ SECRETS DISCIPLINE (finding lexicon-KEYS cited; every `arguments[].value` — cert names, user names, paths —
REDACTED and NOT preserved).
**Method**: READ-ONLY (§12 rung-1) `API2`/SCRAM GET, `no·inline`. Endpoint located by reading
`SecurityDashboardServlet.doGet` `[CERT]`, then consumed live `[CERT-live]`.
**Primary sources**:
- `[CERT]` `organized/nss/nss-rt/vineflower/com/tridium/nss/dashboard/SecurityDashboardServlet.java:54-140`.
- `[CERT-live]` `sources/probes/B604-security-dashboard/live-findings-sanitized.txt`.
**Scope**: prove the dashboard is HTTP-consumable and catalog its live structure + verdicts. Cross-validates
[Block 398]'s static/tool audit. Does NOT replace the `tools/niagara-security-audit.py` ([B398]) — it identifies
the NATIVE source that tool can now ingest.

---

## 604.1 The endpoint — servlet routing [CERT] + live [CERT-live]

`SecurityDashboardServlet` (an `HttpServlet`, nss-rt) routes by request URI `[CERT] :88-113`:

| URI | method → data |
|---|---|
| `/nss/dashboardstatus?ord=<c>` | `getDashboardStatus` — enabled/disabled status of a dashboard component |
| **`/nss/station/data?ord=<c>`** | **`getStationDashboardData`** — the full station posture JSON (SA-G2 target) |
| `/nss/system/data` | `getSystemDataAll` — multi-station (system dashboard), chunked |
| `/nss/system/data/station…` | `getSystemDataSingle` |

Each requires an `ord` param resolving to a `BIStationSecurityDashboard` / `BISystemSecurityDashboard`; a
missing/invalid ord → `sendError(404)` (the cause of a naive no-param GET). A `Referer` containing `bajaux`
flips a `workbenchBrowser` facet `[CERT] :58-63` — the JSON is the SAME data the Workbench/bajaux
`BSecurityDashboardView` renders, so this endpoint IS the programmatic feed of that view.

Live call `[CERT-live]`:
`GET /nss/station/data?ord=station:|slot:/Services/SecurityService` → **HTTP 200, 22 694 bytes JSON**,
`{stationName:"PRUEBAS", version:1, timestamp, sections:[14]}`. The `nss:SecurityService` component IS the
station dashboard (`BIStationSecurityDashboard`).

## 604.2 Structure — 14 module-contributed sections, item = {summary, description, status} [CERT-live]

The dashboard is a plug-in aggregation (`BISecurityDashboardProvider` SPI): each section's header is a
lexicon-keyed contribution from a different module `[CERT-live]`:
`baja` (userService · authenticationService · loggingService) · `fox` · `web` ×2 · `signingService` ·
`abstractMqttDriver` (azure) · `program` · `nss` (fileSystem · moduleSignatures · modulePermissionGroup) ·
`platform` (systemPlatformService · syslogPlatformService) · `platCrypto`. Each `dashboardItem` has exactly
three fields — `summary` (a `{lexiconKey, arguments[]}`), `description`, and `status`. **64 items total.**

## 604.3 Live severity distribution [CERT-live]

| status | count |
|---|---|
| `securityStatusOK` | 30 |
| `securityStatusInfo` | 16 |
| `securityStatusWarning` | **14** |
| `securityStatusAlert` | **4** |

## 604.4 The live verdicts — and they CONFIRM B398 from Niagara's own instrument [CERT-live]

The 4 ALERTS (by lexicon key, values redacted) `[CERT-live]`:
1. `baja:…autoLogoffDisabled` — session auto-logoff is OFF.
2. `baja:…authenticationScheme.withHttpEnabled` — an auth scheme is usable over PLAIN HTTP.
3. `web:…certHealth.missingKey` — a configured cert is missing its private key.
4. `web:…hostHeader.validationOff` — HTTP Host-header validation is OFF.

The 14 WARNINGS include (redacted): `baja:…superUserWithDefaultName` · `fox:…foxCertificateNearExpiry` /
`…Default` / `…GloballyEncrypted` / `…legacyClients.defaultYes` · `web:…webForwarding` (HTTP port live) ·
`program:…programObjects.signingNotRequired` · `nss:…moduleSignatures.verificationMode` /
`…NOT_TIMESTAMPED` / `…SIGNER_SELF_SIGNED` / `…CERT_PATH_VALIDATION_FAILURE` ·
`platform:…systemPlatformService.certificateExpiry` / `…certificate.signature.tls…` / `…globallyEncrypted`.

**Independent confirmation of [Block 398]** — the static/tool audit found `moduleVerificationMode=low`, HTTP 80
open, `program.requireSigning` off, default/`ForRecoveryPurposes` TLS cert, default super user. The native
dashboard, computed by the station itself, raises the SAME items live: `moduleSignatures.verificationMode`
(warn), `webForwarding` (HTTP live), `programObjects.signingNotRequired`, `certHealth`/`certificate.*`,
`superUserWithDefaultName`. Two independent instruments (custom tool + native SPI) agreeing is a strong
`[CERT-live]` corroboration of the supervisor's hardening posture — and SA-G2's answer is YES: the dashboard is
a machine-consumable live source `tools/niagara-security-audit.py` can ingest instead of re-deriving.

## 604.5 What this does NOT resolve

- Whether the endpoint is read-gated below admin (API2 is a privileged account) — a low-privilege principal was
  not tested; the CSRF/permission gate on `/nss/system/data` (returned 400 here for a plain GET) is untested.
  Scoped-out (belongs with W7-G1 / a low-priv principal).
- Per-item `arguments` (concrete cert names, expiry dates, user names) — deliberately NOT captured (SECRETS
  DISCIPLINE); the finding TYPES suffice for the posture verdict.

---

## Self-verify

| # | Claim | Marker | Citation | Verified |
|---|---|---|---|---|
| 1 | Servlet routes /nss/station/data → getStationDashboardData, needs ord | [CERT] | SecurityDashboardServlet:88-113 | ✓ token |
| 2 | Live GET returned HTTP 200, 22694B JSON, 14 sections | [CERT-live] | live-findings-sanitized.txt | ✓ live |
| 3 | 64 items, shape {summary,description,status}, 14 module sections | [CERT-live] | live-findings-sanitized.txt | ✓ live |
| 4 | Severity dist OK30/Info16/Warn14/Alert4 | [CERT-live] | live-findings-sanitized.txt | ✓ live |
| 5 | 4 alerts (autoLogoff, http-auth, missingKey, hostHeader) | [CERT-live] | live-findings-sanitized.txt | ✓ live |
| 6 | Warnings confirm B398 (moduleVerificationMode, http, signing, cert, default super user) | [CERT-live]+[CERT] | live + [B398] | ✓ cross |

**Marker tally**: [CERT-live] ×5, [CERT] ×1. [INFER] 0. Ratio 0. **Block type: EVIDENCE (§12 live).** CLOSES
SA-G2. **§12 verdict: CONFIRMED** — dashboard is a live consumable source AND independently corroborates B398.
Zero secrets exfiltrated (values redacted, raw JSON not preserved). Read-only.

## Connections

- [Block 398] — the security-audit bootstrap (custom tool); this block finds the NATIVE source it can ingest and
  independently confirms its findings.
- [Block 392] — trust-anchor domains; the dashboard's cert warnings (default/`globallyEncrypted`/expiry) are the
  live view of that analysis.
- [Block 600] — the oBIX read session; here a non-oBIX servlet (`/nss/…`) is the feed.
- security-audit focus: SA-G2 closed; SA-G4 (client-facing threat model) remains.
