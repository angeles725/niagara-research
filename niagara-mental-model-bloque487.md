# Block 487 — Silent-watcher audit: no covert/undocumented monitor in the licensing path (node-locked fully passive; subscription's only background actors are the two VISIBLE, alarmed check-ins) — plus one genuinely-silent thread that lives in the CRYPTO subsystem, not licensing

> **Focus:** `licensing`. **Question (operator):** ¿hay algo que vigile de forma SILENCIOSA que no nos demos
> cuenta? READ-ONLY, decompiled source; no binary run. Evidence-first: explicit `[CERT negative]` where a grep
> returned nothing (never assert a negative from memory). Markers §3.
>
> **Method note (doctrine):** the first grep pass produced FALSE negatives — this shell is zsh, which does not
> word-split an unquoted `$ROOTS`, so `rg` received one bogus path and errored. Re-run with literal path args
> (exit 0). A tool that errored is NOT a real zero — all results below are from the corrected runs. Paths
> searched (java-file counts): `sys/license` (21), `nre-ext/…/subscription` (23), `nre-ext/…/crypto` (110),
> `platform-rt/…/license` (12), `niagarad-ext` (105).

## §487.1 — Outbound network `[CERT / CERT negative]`

Every outbound-capable call in the license/crypto/subscription path was enumerated and classified. **No unknown
host anywhere** — only the three known ones ([B480]): `niagaracentralapis.honeywell.com` (entitlement),
`niagara-community.com` (registration OAuth); `axlicensing.tridium.com` is not even referenced in these paths.
- Subscription check-in / registration: `EntitlementApi.java:32,77`, `EntitlementUtil.java:45,47`,
  `AccessTokenApi.java:81`, `DeviceCodeApi.java:26`, `HttpConnectionlessTransport.java:75-85` — all (a) documented.
- `crypto/core/cert/SigningUtil.java:113-114` `new URL(tsaUrl)`→`HttpURLConnection` = a **jar/code-signing
  timestamp (TSA)** call, configurable, only during signing operations — NOT a license phone-home.
- `niagarad` sockets are **loopback only** (`new Socket("127.0.0.1"…)`, `NiagaraDaemon.java:992-1000`).
- **NODE-LOCKED path: `[CERT negative]` — 0 outbound calls.**

## §487.2 — Scheduled tasks / threads `[CERT / CERT negative]`

- **Node-locked (`sys/license` non-subscription + `platform-rt/license`): `[CERT negative]`** — the ONLY
  `Executor`/`Timer`/`schedule*`/`Thread` in the whole `sys/license` tree is inside the `subscription/` subfolder;
  platform license has none. `NLicenseManager.reload()` (`:188`) is an **on-demand method, not scheduled**.
- **Subscription:** `SubscriptionLicenseManager.java:55` `newScheduledThreadPool(1)` runs EXACTLY TWO tasks —
  `KeyRotationCheck` (`:170`) and `EntitlementCheck` (`:194`). No third task (matches [B480]/[B481]).
- `AccessTokenApi.java:108` `new Thread("Nre:PollAccessToken")` = OAuth polling during **operator-initiated
  registration**, not a standing watcher.

## §487.3 — Telemetry / beacon `[CERT negative]`

No hidden usage reporting: `telemetry|analytics|beacon|heartbeat|phoneHome|callHome|usageReport|metric` →
**0 hits** in any license/crypto file. All entitlement status flows are VISIBLE: failures → `logger.severe`
(`SubscriptionLicenseManager.java:285`) AND a platform alarm via `EntitlementStatusListener` →
`BLicensePlatformService.java:180-194` (`entitlementCheckinFailure/Success`).

## §487.4 — File/registry watchers over LICENSE files `[CERT negative]`

No `WatchService`/`lastModified`/polling monitor over any license file (node-locked or platform). The only
monitors are over **certificate/app** stores, not licenses (see §487.6).

## §487.5 — Listeners on license state `[CERT negative for covert]`

The only listener on license/entitlement state is `EntitlementStatusListener`, consumed by
`BLicensePlatformService` (implements it + `BIAlarmablePlatformService`) → every transition raises a **VISIBLE**
platform alarm. Node-locked: no license-state listener at all.

## §487.6 — The one genuinely-silent thread — and it is NOT licensing `[CERT]`

Honest completeness: there IS a quiet background poller in scope, but it watches **certificate stores, not
license files**, and is unrelated to license enforcement:
- `CoreCryptoManager.checkFileMonitor()` (`:930,952`) polls `checkLastModified()` every **10 s** over
  `keyStore / userTrustStore / userUntrustedStore / exemptionStore`; reload logs at **FINE only**
  (`CoreStore.java:42-43`), no alarm → effectively silent. Plus a 10-min `checkSaveRecurringCertsSchedule`.
- This is the crypto/trust subsystem (cert rotation/reload), NOT the licensing path. Worth telling an operator
  "a silent background thread exists in the crypto subsystem," while being clear it does not watch licenses.

## §487.7 — Verdicts

- **NODE-LOCKED (perpetual): NO undocumented silent watcher.** Fully passive — 0 outbound, 0 scheduled tasks,
  0 file watchers over licenses, 0 license-state listeners. `reload()` on-demand only. Corroborates [B481].
- **SUBSCRIPTION: NO undocumented silent watcher.** Only the two documented tasks (`EntitlementCheck` ~6h,
  `KeyRotationCheck` daily), both VISIBLE (log.severe + platform alarm). All hosts are the known three; no
  telemetry/usage beacon beyond entitlement.
- **Bottom line:** no hidden phone-home, no covert beacon, no extra licensing task, no silent watcher over
  license files, for either model. The only truly silent poller in the searched code is the crypto cert-store
  monitor (out of the licensing enforcement path). Native-side corroboration (no hidden WinINet/WinHTTP/ws2_32
  in the DLLs) is being confirmed separately (Segundo + capa/FLOSS) → any refinement lands as a child gap.

## §487.8 — Self-verify

| # | Claim | Marker | Citation | Verdict |
|---|---|---|---|---|
| 1 | Only 3 known hosts in the license path; node-locked 0 outbound | `[CERT]`/`[CERT neg]` | EntitlementApi/Util, AccessToken/DeviceCode; grep | PASS |
| 2 | Subscription has exactly 2 scheduled tasks; node-locked/platform none | `[CERT]`/`[CERT neg]` | `SubscriptionLicenseManager.java:55,170,194` | PASS |
| 3 | No telemetry/beacon; entitlement status is logged + alarmed (visible) | `[CERT neg]`/`[CERT]` | grep; `BLicensePlatformService.java:180-194` | PASS |
| 4 | No file watcher over license files | `[CERT negative]` | grep `sys/license`+`platform/license` | PASS |
| 5 | Silent 10s poller exists but over CERT stores, not licenses (FINE-only, no alarm) | `[CERT]` | `CoreCryptoManager.java:930,952`; `CoreStore.java:42-43` | PASS |
| 6 | TSA URL (SigningUtil) is code-signing timestamp, not a license phone-home | `[CERT]` | `SigningUtil.java:113-114` | PASS |

**Tally:** 6 claims, all `[CERT]`/`[CERT negative]`, 0 unmarked. Method: false-negative from zsh word-split caught and re-run (tool-failure ≠ zero).

## §487.9 — Native corroboration (no hidden native beacon) `[CERT negative]` — by sibling session Segundo

Native import/string audit (imports via `il`/`ii`, delay-load via `objdump -p`, string scan `izz` for
wininet/winhttp/ws2_32/socket/http) over `nre.dll`, `njre.dll`, `dsfspi.dll`, and `common.dll` (delay-loaded by
nre): **VERDICT — NO hidden native phone-home/beacon.**
- `nre.dll`/`njre.dll`: imports = advapi32, **crypt32 (ONLY `CryptProtectData`/`CryptUnprotectData` = DPAPI
  local secret encryption)**, kernel32, ole32, shell32, vcruntime, dsfspi(+common). **ZERO ws2_32/wininet/
  winhttp**, no `connect`/`send`/`socket`/`Internet*`/`WinHttp*` (static, delay, or as a dynamic-resolution
  string).
- `dsfspi.dll`: only kernel32/vcruntime/crt — **zero network capability.**
- ALL `http://` URLs in the four binaries = **DigiCert OCSP/CRL/AIA/CPS DATA embedded inside the Authenticode
  signing certs** (Trusted Root G4), NOT beacon destinations, and no code fetches them (no net API).
- `common.dll` (delay-loaded): the ONLY native binary with ws2_32, but **name-resolution only** (`getaddrinfo`/
  `getnameinfo`/`freeaddrinfo`/`WSACreateEvent`) — **no `socket`/`connect`/`send`/`recv`/`bind` → cannot open a
  connection or transmit.** `netapi32` = OS user/group/domain enumeration (auth OS-user mapping + hostname
  canonicalization for Host ID), not a watcher.
- **Net:** the only outbound path in the product is the KNOWN Java entitlement/subscription path ([B480]); the
  native side is clean. Confirms **B487-G1 CLOSED**.

## §487.10 — Connections

- Answers the operator's covert-monitoring question with evidence (Java §487.1-8 + native §487.9); corroborates
  [B481] (node-locked passive) and [B480] (the two subscription watchers).
