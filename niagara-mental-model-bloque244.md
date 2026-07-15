# Bloque 244 — U3 OEM alarm layer: `honAlarmExt` (alarm-delay / transient suppression) + `honAlarmConsole` (brand-aware JS console + BQL RPC)

> Empirical coverage of the OEM Honeywell **alarm extension** pair (coverage-audit gap U3,
> `audits/2026-07-12-coverage-audit.md`): `honAlarmExt` (Honeywell extensions to the Niagara alarm framework)
> and `honAlarmConsole` (a branded alarm-summary console UI). Both SMALL (measured pre-flight §13 e2):
> `honAlarmExt-rt` = 5 classes, `honAlarmConsole` = 5 classes (3 `-rt` + 2 `-ux`). Real code — read inline.
>
> **Focus**: `oem-honeywell-tail`, gap U3 (HIGH). Third block of the focus (after B242, B243).
>
> **Sources**: `organized/honAlarmExt/honAlarmExt-rt/vineflower/com/honeywell/honAlarmExt/**` and
> `organized/honAlarmConsole/honAlarmConsole-{rt,ux}/vineflower/com/honeywell/honAlarmConsole/**` + both
> `module.xml`.
>
> **Method**: read inline (small gap, no delegated sweep). `[CERT]` = observed by me at the cited `file:line`;
> `[CERT-a]` = read from an obfuscation-degraded decompile (one `honAlarmConsole` method failed to decompile —
> vineflower NPE, obfuscated method names `a/b/c/…`); `[INFER]` = deduction. **ZKM caveat**: `honAlarmConsole`
> shows Zelix obfuscation artifacts (single-letter method names, a decompiler failure), so its RPC internals
> are lower-confidence. `honAlarmExt` decompiled cleanly.
>
> Capa 22 (OEM). **Conecta fuerte**: [Bloque 8] (Alarm framework core — `BAlarmClass`, `BConsoleRecipient`,
> `BAlarmRecord`, routing), [Bloque 34] (Alarm/History deep), [Bloque 242] (`honIrmConfig` — the SAME 7-brand
> OEM set), [Bloque 75] (license gating).

---

## 244.1 — Scope: two small alarm modules, one OEM story `[CERT]`

`honAlarmExt` (`module.xml`: `description="Honeywell Alarm Extensions"`, `vendorVersion="4.8.0.0.0.5"` — an
OLDER 4.8-era module) registers 3 Niagara types: `HonAlarmClass`, `HonConsoleRecipient`, `DelayFilterState`.
`honAlarmConsole` registers `HonAlarmConsoleRpc` + the UX console. Neither is large; together they are the
**Honeywell alarm-delay + branded-console layer** over the Tridium alarm framework ([Bloque 8]).

The split: `honAlarmExt` = the RUNTIME behavior (delay/suppress alarms before routing); `honAlarmConsole` = the
OPERATOR UI (a branded JS console that queries and summarizes alarms).

---

## 244.2 — `honAlarmExt`: alarm-delay / transient-suppression `[CERT]`

The core feature is **alarm debouncing**: buffer an alarm for a delay window and only route it if it is STILL
active when the window expires — suppressing transient/chattering alarms. It is implemented twice, at two
points in the alarm pipeline:

- **`BHonAlarmClass extends BAlarmClass`** (`BHonAlarmClass.java:50`) — the alarm-class level. Adds
  `@NiagaraProperty`s (`:26-49`): `enableAlarmDelay` (boolean), `delayTime` (`BRelTime`, `BFacets.MIN` bound),
  `sendDelayBufferOnShutdown` (boolean), and a `checkBuffer` action. It OVERRIDES the routing hook
  `doRouteAlarm(BAlarmRecord record)` (`:94`) to intercept-and-buffer, plus `doCheckBuffer()` (`:138`) and
  `removeFromBufferIfRecStateIsNormal(BAlarmRecord rec)` (`:156`) — i.e. when a buffered alarm's record state
  returns to NORMAL before the delay expires, it is removed from the buffer and never routed. Lifecycle
  `started()`/`stopped()`/`changed()` manage a timer thread (`run()` at `:279`).
- **`BHonConsoleRecipient extends BConsoleRecipient`** (`BHonConsoleRecipient.java`) — the recipient level, with
  the IDENTICAL delay properties (`enableAlarmDelay`, `delayTime` with MIN+MAX facets, `sendDelayBufferOnShutdown`,
  `checkBuffer`) and overrides `handleAlarm(BAlarmRecord record)` (`:102`) + the same `doCheckBuffer`/
  `removeFromBufferIfRecStateIsNormal` buffer logic. `[INFER]` So a deployment can apply the delay at the
  alarm SOURCE (alarm class) or at the DELIVERY point (console recipient), depending on topology.
- **`BDelayFilterState extends BFrozenEnum`** (`BDelayFilterState.java:11-18`) — the per-alarm buffer state
  machine: `UNKNOWN=0`, `DELAYED=1`, `SENT=2`, `IGNORED=3`. `[INFER]` An alarm enters `DELAYED`, then resolves
  to either `SENT` (still active after delay → routed) or `IGNORED` (returned to normal within delay →
  suppressed). `AlarmRecordContainer` holds the buffered records.

`[INFER]` This is a classic transient-alarm filter (nuisance-alarm suppression) layered on the Niagara alarm
routing — the OEM value-add over the stock `BAlarmClass`/`BConsoleRecipient` of [Bloque 8], which route
immediately with no delay/debounce.

---

## 244.3 — `honAlarmConsole`: brand-aware JS console + BQL RPC summary API `[CERT]` / `[CERT-a]`

- **`BHonAlarmConsole extends BSingleton implements BIJavaScript, BIFormFactorMax`**
  (`ux/BHonAlarmConsole.java:22`) — the console UI component: a JS-backed (`BIJavaScript`) singleton at max form
  factor (`BIFormFactorMax` = full-viewport responsive). Its compiled bundle is
  `BHonAlarmConsoleBuiltJS extends BJsBuild`. `[INFER]` A modern single-page alarm console rendered client-side,
  the alarm-domain sibling of the branded dashboards elsewhere in the OEM stack.
- **`BHonAlarmConsoleRpc extends BObject`** (`BHonAlarmConsoleRpc.java`) — the server-side RPC the JS calls.
  Observed static methods `[CERT]`: `checkFeatureLicense(Context)` (`:61`) + `checkHonAlarmConsoleFeature()`
  (`:85`, returns a `javax.baja.license.Feature` — **license-gated**), `checkEdgeController(Context)` /
  `isConnectedToEdgeController()` (`:204/:226` — edge-controller awareness), `getBrandFromLicenseFile()`
  (`:236` — multi-brand), and the two data endpoints `getMultiSourceSummary(Map, Context)` (`:250`) +
  `getSingleSourceSummary(Map, Context)` (`:309`), both returning `com.tridium.json.JSONObject`.
  `[CERT-a]` One method failed to decompile (vineflower NPE, ZKM-obfuscated); its bytecode trace shows it builds
  a `BDynamicTimeRange` + `BFilterSet` from the request JSON and resolves a `javax.baja.alarm.BAlarmRecipient`
  — i.e. the summary is a **BQL query over alarms** (time-range + filter-set + recipient), returned as JSON.
- **`HonAlarmConsoleConstants`** (`common/HonAlarmConsoleConstants.java:4-15`) `[CERT]` — brand constants
  `TREND`, `CENTRALINE`, `SBC`, `WEBS`, `ALERTON`, `HBS`, `HONEYWELLBMS` + per-brand `*_VENDOR` strings. This is
  the **SAME 7-brand OEM set** as `honIrmConfig`'s `BrandHandler` ([Bloque 242] §242.6: Alerton, Centraline,
  HBS, SBC, Trend, WEBS, Honeywell) — the alarm console is rebadged per OEM via `getBrandFromLicenseFile()`.

---

## 244.4 — Conexiones

- **[Bloque 8]** (Alarm framework): the base this extends — `BHonAlarmClass extends BAlarmClass`,
  `BHonConsoleRecipient extends BConsoleRecipient`, both operating on `BAlarmRecord` and the routing pipeline.
  honAlarmExt's value-add is the delay/suppression the stock framework lacks.
- **[Bloque 34]** (Alarm/History deep): the deeper alarm-record/console-recipient machinery the delay logic
  hooks into.
- **[Bloque 242]** (`honIrmConfig`): the SAME 7-brand OEM licensing set appears here in
  `HonAlarmConsoleConstants` + `getBrandFromLicenseFile()` — the multi-brand rebadging is a cross-module
  Honeywell pattern (config, alarm console, …), all keyed off the Niagara license file.
- **[Bloque 75]** (seguridad / licensing): the console is license-gated (`checkHonAlarmConsoleFeature`) and the
  brand is read from the license file — same license-as-gate pattern.

---

## 244.5 — Self-verify

- **Claims observed by me** (`[CERT]`): `honAlarmExt` class declarations + `@NiagaraProperty` delay slots +
  routing overrides (`BHonAlarmClass.java:26-156`), `BDelayFilterState` ordinals (`:11-18`),
  `BHonConsoleRecipient` delay slots + `handleAlarm` (`:26-102`), `honAlarmConsole` RPC method signatures
  (`BHonAlarmConsoleRpc.java:61-309`), the 7 brand constants (`HonAlarmConsoleConstants.java:4-15`), and the
  `BHonAlarmConsole` UI interfaces (`ux/BHonAlarmConsole.java:22`). `[CERT-a]` = the one obfuscation-degraded RPC
  method (BQL time-range/filter-set, from bytecode trace). `[INFER]` = the debounce semantics + brand/console
  deductions.
- **Block TYPE**: EVIDENCE (small modules). Moderate `[INFER]` share — the modules are small and the behavior
  (buffer→SENT/IGNORED) is partly deduced from method names + the state enum, not a line-by-line trace of the
  timer body. U3 is covered; a deeper per-line trace of the delay timer is possible but low-value.
- **New gaps queued**: none net-new from U3. Next per RESEARCH-STATE-oem-honeywell-tail: U4
  `SylkActuatorAnalytics` + `lonHoneywellAnalytics` (MED-HIGH), or U1b/U1c to finish `honIrmConfig`.
