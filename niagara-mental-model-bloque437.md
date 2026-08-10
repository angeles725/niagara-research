# Block 437 — The driver UI framework: a reflection-driven device/point manager where a driver author only declares @AgentOn + @MgrInclude

> Research of **`driver-wb` + `ndriver-wb`** (focus `workbench`, gap WB11, LOW) — the generic driver UI
> framework every protocol driver's Workbench UI extends. Scope: the base managers, the discovery/learn base,
> the N-driver convenience layer, the config field editors, and the inheritance proof. This is the framework
> the 51-module driver-UI long tail ([Block WB12/B438]) builds on. Does NOT cover any specific driver
> (bacnet/modbus/lon are their own modules).
>
> Subject version: OptimizerSupervisor N4.14.0.162 — `driver-wb.jar`
> sha256 `89ad1fffdc21b53ef3add00f1681e26cc37ae2af36f30428585ab27f6c00687c` · `ndriver-wb.jar`
> sha256 `bc768b084062b1100e5ff4a5ed67d299fcc8a6282cfa46a0dbb0b8f4a7beeef9`.
>
> Sources: Tridium docSource (`sources/tridium-src/driver-wb/.../device/BDeviceManager.java`) + Vineflower impl
> (`sources/decompiled/{driver-wb,ndriver-wb}/`). Method: docSource for base contracts, Vineflower for impl.
> CAVEAT: `BDeviceManager.makeModel` and the config FE classes decompile with mangled tokens (`n`/`ln`); parent
> types are real, so those are cited by parent/existence. Markers: `[CERT]` (`file:line`) · `[INFER]` deduction.
>
> Workbench UI framework. Connects [Block 431] (`BAbstractManager`/`MgrLearn` — these bases extend it),
> [Block 430] (config field editors), [Block 415] (niagaraDriver device/proxy model), [Block 304] (modbus-wb
> as a concrete example).

---

## 437.1 — Two layers: driver-wb (generic) → ndriver-wb (N-driver) `[CERT]`

`driver-wb` (`runtimeProfile=wb`, "WB UI of Niagara Driver Framework") ships the abstract base managers —
`BDeviceManager`, `BPointManager`, `BDriverManager` — with NO `@AgentOn` (extension bases only). `[CERT]`
`ndriver-wb` (depends on driver-wb) is the "N-driver" convenience layer on top: `BNDeviceManager`/
`BNPointManager` + `NMgrLearn` auto-discovery + column reflection. `[CERT]` `[INFER]` most real protocol drivers
(bacnet-wb, modbus-wb) extend **ndriver-wb**, not driver-wb directly.

## 437.2 — What a driver gets for free: the device/point manager `[CERT]`

`BDeviceManager` extends `BFolderManager` (itself a `BAbstractManager` [Block 431]) and wires the standard
model/controller/state (`makeModel` → `DeviceModel`, etc. — method token mangled in docSource, return real).
`[CERT]` `DeviceModel.makeColumns` gives every device manager the columns `{Path, Name, Type, status, enabled,
health, faultCause}` and template-deploy logic for FREE — zero protocol code. `[CERT]` `BPointManager` is the
twin for `BControlPoint`. `[INFER]` a driver author inherits a working device/point table without writing a
manager.

## 437.3 — Discovery: NMgrLearn over a live BNDiscoveryJob, columns by reflection `[CERT]`

There is no `BAbstractDiscovery` class — the discovery/learn base is `MgrLearn` ([Block 431]); ndriver-wb ships
`NMgrLearn extends MgrLearn` (`sources/decompiled/ndriver-wb/com/tridium/ndriver/ui/NMgrLearn.java:37`). `[CERT]`
It subscribes LIVE to a `BNDiscoveryJob.discoveryFolder()` and `updateRoots(...)` as leaves arrive
(`NMgrLearn.java:7`,`:46`). `[CERT]` Its columns are REFLECTED: `NMgrColumnUtil.getColumnsFor` walks all
`@MgrInclude` properties on the discovery-leaf type. `[CERT]`/`[INFER]` and `toRow` maps a discovered leaf to a
target via `BINDiscoveryLeaf.updateTarget()`. `[INFER]`

## 437.4 — The minimal concrete driver: declare @AgentOn, annotate the proxy-ext `[CERT]`

`BNDeviceManager extends BDeviceManager`, `@AgentOn(types={"ndriver:NNetwork","ndriver:NDeviceFolder"})`
(`sources/decompiled/ndriver-wb/com/tridium/ndriver/ui/BNDeviceManager.java:17`,`:21`). `[CERT]` Its `makeLearn`
returns an `NMgrLearn` ONLY if `NMgrUtil.getDiscoveryLeafType(this)` is non-null — so learn is silently disabled
for networks with no discovery host, no override needed. `[CERT]`/`[INFER]` `NDeviceModel.makeColumns` delegates
to `NMgrModelUtil.makeColumns`, which appends all `@MgrInclude`-reflected properties from the protocol's
device/proxy-ext type. `[INFER]` **So a concrete driver's minimum is: declare `@AgentOn` for its network/device
types and annotate its proxy-ext properties with `@MgrInclude` — it then gets a full device+point manager with
discover-and-add for free.** `[CERT]`/`[INFER]`

## 437.5 — Config field editors `[CERT]`

Three reusable driver FEs (class tokens mangled `ln`; parents real): `BIpPortFE extends BWbFieldEditor` (comm
port, with an "unspecified" = -1 checkbox), `BTuningPolicyNameFE extends BComponentNamePickerFE` (per-point
poll-rate picker — leases the ancestor `BDeviceNetwork.tuningPolicies` and lists `BTuningPolicy` children), and
`BProxyConversionFE extends BWbFieldEditor` (point-scaling dropdown over registered `BProxyConversion` subtypes).
`[CERT]` (by parent/existence). `[INFER]` these are the standard comm-config / poll-config / scaling editors
every driver point/network reuses via [Block 430]'s field-editor dispatch.

## 437.6 — Inheritance proof `[CERT]`

Spot-check: the built-in `BFileDeviceManager extends BDeviceManager`
(`sources/decompiled/driver-wb/.../device/BFileDeviceManager.java:24`), overriding only `makeModel` to add two
protocol columns (`DeviceExtsColumn` + `baseOrd`). `[CERT]` This is the exact pattern [Block 304] documented for
modbus-wb — a driver extends `BNDeviceManager`/`BDeviceManager` and adds only its protocol-specific columns.
`[INFER]`

## 437.7 — Self-verify

| # | Claim | Marker | Source |
|---|---|---|---|
| 1 | driver-wb = generic base managers (no @AgentOn); ndriver-wb = N-driver layer (depends on driver-wb) | `[CERT]` | §437.1; module.xml |
| 2 | `BDeviceManager extends BFolderManager` (a BAbstractManager); free columns + template deploy | `[CERT]` | `BDeviceManager.java` (docSource) |
| 3 | `NMgrLearn extends MgrLearn`, live `BNDiscoveryJob` subscription, `@MgrInclude` column reflection | `[CERT]` | `NMgrLearn.java:37`,`:7` |
| 4 | `BNDeviceManager extends BDeviceManager @AgentOn(ndriver:NNetwork)`; learn auto-enabled via discovery-leaf type | `[CERT]` | `BNDeviceManager.java:17`,`:21` |
| 5 | Minimal driver = declare @AgentOn + annotate proxy-ext `@MgrInclude` → free device+point manager | `[CERT]`/`[INFER]` | §437.4 |
| 6 | Config FEs: `BIpPortFE`/`BTuningPolicyNameFE`/`BProxyConversionFE` (parents real, tokens mangled) | `[CERT]` | §437.5 |
| 7 | Inheritance proof: `BFileDeviceManager extends BDeviceManager` (same pattern as modbus B304) | `[CERT]` | `BFileDeviceManager.java:24` |

**Marker tally**: `[CERT]` ≈ 18 · `[INFER]` 8 ([INFER]/[CERT] ≈ 0.44). Type: **EVIDENCE block** (LOW survey) —
ratio healthy. VERIFY-BEFORE-ACTING: the load-bearing inheritance chain (`NMgrLearn extends MgrLearn`,
`BNDeviceManager extends BDeviceManager`, `BFileDeviceManager extends BDeviceManager`) was re-verified live and
is CLEAN; only the config-FE class tokens and `makeModel` were mangled and are cited by parent/existence.
Tokens confirmed: `NMgrLearn extends MgrLearn`, `BNDiscoveryJob`, `@AgentOn ndriver:NNetwork`,
`extends BDeviceManager` (×2), `DeviceExtsColumn`.

## 437.8 — Connections

- **[Block 431]** — `BDeviceManager`→`BFolderManager`→`BAbstractManager`; `NMgrLearn`→`MgrLearn`. The driver
  framework is a specialization of the generic manager/learn framework.
- **[Block 430]** — the config FEs (`BIpPortFE`, tuning-policy, proxy-conversion) plug into the field-editor
  dispatch.
- **[Block 415]** — the niagaraDriver device/proxy model these managers present.
- **[Block 304] / WB12 (B438)** — modbus-wb and the 51-driver long tail all extend these bases; B438 documents
  that the tail is pattern-repetition over THIS framework.

<!-- research-block: focus workbench, gap WB11 (driver-wb + ndriver-wb framework) — CLOSED at body grade (LOW) -->
