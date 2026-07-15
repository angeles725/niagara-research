# Bloque 245 — U4 OEM analytics: `SylkActuatorAnalytics` (actuator cycle-count analytics) + `lonHoneywellAnalytics` (LON IAQ device-interface bundle)

> Empirical coverage of the OEM Honeywell **analytics** pair (coverage-audit gap U4,
> `audits/2026-07-12-coverage-audit.md`): `SylkActuatorAnalytics` (analytics/tool for Sylk-bus actuators) and
> `lonHoneywellAnalytics` (LON IAQ sensor definitions). Both SMALL (measured pre-flight §13 e2):
> `SylkActuatorAnalytics` = 4 classes (2 `-rt` + 2 `-ux`); `lonHoneywellAnalytics` = **0 classes** (resource-only
> bundle). Read inline.
>
> **Focus**: `oem-honeywell-tail`, gap U4 (MED-HIGH). Fourth block of the focus (after B242–B244).
>
> **Sources**: `organized/SylkActuatorAnalytics/SylkActuatorAnalytics-{rt,ux}/vineflower/com/honeywell/SylkActuatorAnalytics/**`
> and `organized/lonHoneywellAnalytics/lonHoneywellAnalytics-rt/extracted/**` (`.lnml` device files, module.xml,
> signature block).
>
> **Method**: read inline (small gap, no delegated sweep). `[CERT]` = observed by me at the cited `file:line`;
> `[INFER]` = deduction. Distributed artifacts (not live-install) — citing structure is in scope.
>
> Capa 22 (OEM). **Conecta fuerte**: [Bloque 88] + [Bloque 105] (Sylk bus + actuators — the hardware this
> analyzes), [Bloque 66]–[Bloque 68] (Tridium Analytics module — the analytics sibling), [Bloque 34]
> (History — the data source), [Bloque 19] + [Bloque 92] (LON framework — for the `.lnml` device files),
> [Bloque 75] (module signing — a SECOND Honeywell signer identity surfaces here).

---

## 245.1 — `SylkActuatorAnalytics`: actuator cycle-count analytics `[CERT]`

A small service + a JS tool that track **how many cycles each Sylk-bus actuator has run** — a predictive-
maintenance / wear-monitoring analytic. Four classes:

- **`BSylkActuatorService extends BAbstractService`** (`BSylkActuatorService.java:49`) — `@NiagaraProperty`s
  `sylkActuator` (a child `BSylkActuator`) + `autoConfigureNiagaraNetwork` (boolean), and `@NiagaraAction
  configureNiagaraNetwork` (`:37-52`). `[INFER]` The service auto-wires the Niagara network so actuator data is
  reachable for the analytic.
- **`BSylkActuator extends BComponent`** (`BSylkActuator.java:9`) — a child component of the service
  (`isParentLegal(parent) → parent instanceof BSylkActuatorService`, `:16-17`).
- **`BSylkActuatorToolRPC extends BComponent`** (`ux/BSylkActuatorToolRPC.java:40`) — the tool's server-side RPC:
  - `forceUpdateProxyPoint(Context)` (`:55`) — reaches `station:|slot:/Drivers/NiagaraNetwork` to refresh proxy
    points.
  - `getActuatorBarGraph(Context)` (`:75`) → `JSONArray` — the core analytic: it runs the NEQL query
    **`neql: n:history and s:ActuatorCycleCount and s:ActuatorName`** (`:87`) over `/Drivers`, i.e. it harvests
    every actuator's **cycle-count history** by tag and returns a bar-graph dataset `[CERT]`.
  - `getAllActuatorList(Context)` (`:161`) → `JSONArray` — enumerates the actuators.
- **`BSylkActuatorWidget extends BSingleton implements BIJavaScript, BIFormFactorMax, BIOffline,
  BICollectionSupport`** (`ux/BSylkActuatorWidget.java`) — the JS UI widget (offline-capable, collection-aware)
  that renders the bar graph.

`[INFER]` So the "analytics" is actuator WEAR tracking: Sylk actuators (the TR-series / SmartActuator hardware
from [Bloque 88]) log a cycle count into Niagara history; this module queries that history by tag
(`ActuatorCycleCount` + `ActuatorName`) and visualizes per-actuator cycles for maintenance planning. The data
source is the History framework ([Bloque 34]); the query language is NEQL (tag-based, [Bloque 21] tags).

---

## 245.2 — `lonHoneywellAnalytics`: LON IAQ device-interface bundle (no code) `[CERT]`

A **resource-only** module (`fase1-recon: class_count=0`; `module.xml description="HoneywellAnalytics"`,
`vendorVersion="4.14.0.162"`, dep `baja` only, `<types/>` empty). Its payload is **LON device-interface
definitions** for Indoor-Air-Quality sensors:

- `extracted/IAQCo2.lnml` and `extracted/IAQMulti.lnml` `[CERT]` — XML `type="XLonInterfaceFile"` / `XLonDevice`
  definitions. `IAQCo2.lnml` header: `programID v="80 00 0c 0a 46 04 04 02"`, `majorVersion 4` / `minorVersion
  400`, `numNvDeclarations v="7"`, `addressTableEntries v="15"`, plus the full LON buffer configuration
  (network/application in/out buffer counts + sizes). `[INFER]` These are LonMark-style device profiles (the
  `.lnml` = LON node markup) describing the CO2 and multi-gas IAQ sensors' network-variable interface — the LON
  equivalent of the XIF device profiles seen in [Bloque 19]/[Bloque 92], scoped to Honeywell IAQ hardware. No
  code — the "analytics" here is device DEFINITIONS, not logic.

---

## 245.3 — Signing note: a SECOND Honeywell code-signing identity `[CERT]`

`lonHoneywellAnalytics` is a signed jar, but its signer differs from the firmware module of [Bloque 243]:
`extracted/META-INF/NIAGARA4.RSA` leaf subject = **`CN=Honeywell Product PKI RSA, OU=ACS, O=Honeywell
International Inc., C=US`** — an INTERNAL Honeywell PKI ("Honeywell Product PKI", OU=ACS = Automation & Control
Solutions), NOT the external DigiCert-G4 code-signing chain that signed `honFirmwarePackage` ([Bloque 243]
§243.3). `[INFER]` So Honeywell signs different module classes with different roots — a commercial DigiCert
code-signing cert for firmware-bearing modules and an internal Product-PKI cert for others. Worth tracking as a
supply-chain observation (two trust anchors in one OEM stack).

---

## 245.4 — Conexiones

- **[Bloque 88] / [Bloque 105]** (Sylk): `SylkActuatorAnalytics` analyzes the Sylk-bus actuators those blocks
  modelled (`honeywellSylkDevice` wall modules + `honIrmControl` Sylk FBs).
- **[Bloque 66]–[Bloque 68]** (Tridium Analytics): the analytics-module family; this is the OEM actuator-wear
  analytic counterpart.
- **[Bloque 34]** (History) + **[Bloque 21]** (Tag/NEQL): the data source and query language of the cycle-count
  analytic (`neql: n:history and s:ActuatorCycleCount`).
- **[Bloque 19] / [Bloque 92]** (LON): `lonHoneywellAnalytics`'s `.lnml` IAQ device profiles are LON
  device-interface definitions, kin to the XIF profiles those blocks covered.
- **[Bloque 75] / [Bloque 243]** (signing): introduces a SECOND Honeywell signer — internal "Honeywell Product
  PKI RSA / OU=ACS" — alongside B243's external DigiCert-G4 code-signing chain.

---

## 245.5 — Self-verify

- **Claims observed by me** (`[CERT]`): `SylkActuatorAnalytics` class declarations + service slots/action
  (`BSylkActuatorService.java:37-52`), the NEQL cycle-count query (`ux/BSylkActuatorToolRPC.java:87`) + RPC
  methods, the widget interfaces; `lonHoneywellAnalytics` module.xml (0 types) + the `.lnml` XLonDevice headers
  + the `Honeywell Product PKI RSA` signer (`openssl pkcs7`). `[INFER]` = the wear-analytic semantics, the LON
  profile purpose, and the dual-signer observation.
- **Block TYPE**: EVIDENCE (small modules, one resource-only). U4 covered; `SylkActuatorAnalytics` is fully read
  (4 classes), `lonHoneywellAnalytics` is proven-absence on the code axis (LON definitions only).
- **New gaps queued**: none net-new from U4. Next per RESEARCH-STATE-oem-honeywell-tail: U5 (Honeywell utility
  modules — `honBacnetHelper`/`honUtilityBacRestore`/`honLonsockClient`/`honDescriptionUtility`, MED), or
  U1b/U1c.
