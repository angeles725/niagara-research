# Bloque 249 — U8 Centraline residue: `clStationUpgradeTool` (CentraLine→Honeywell rebrand migration) + `clPrintout` (station documentation/PDF) + `clExtensions`/`clProfile` + 4 resource bundles

> Empirical coverage of gap U8 (coverage-audit `audits/2026-07-12-coverage-audit.md`): the CentraLine residue —
> 8 modules. Measured pre-flight (§13 e2): `clPrintout` 24 cls, `clStationUpgradeTool` 11, `clExtensions` 2,
> `clProfile` 1 (code); `CentralineAhuPx`, `CentralineHtgPx`, `CentralineLONIOr5`, `DINsymbol` = 0-code resource
> bundles.
>
> **KEY FINDING**: U8 is **direct evidence of an in-progress CentraLine→Honeywell rebrand/migration** — the
> upgrade tool literally rewrites persisted `clBACnetUtilities:*` (CentraLine) types to `hon:*`/
> `honBACnetUtilities:*` (Honeywell), and the CentraLine brand PROFILE steers to the Honeywell device-manager
> stack. This EXPLAINS the recurring multi-brand OEM pattern seen across B242/B244/B246/B248: CentraLine (and
> the other brands) are being absorbed into the Honeywell stack, and these are the rebrand machinery.
>
> **Focus**: `oem-honeywell-tail`, gap U8 (MED). Block after B242–B248.
>
> **Sources**: `organized/{clPrintout,clStationUpgradeTool,clExtensions,clProfile}/<m>-<rt|wb>/vineflower/**` +
> 4 resource module.xml. **Method**: 4 code modules via delegated `sonnet` sweep; 4 load-bearing claims
> re-verified by me. `[CERT]` = re-verified at cited `file:line`; `[CERT-a]` = sweep citation; `[INFER]` =
> deduction.
>
> Capa 22 (OEM). **Conecta fuerte**: [Bloque 87] (`clHVAC*` Centraline — the covered CentraLine stack),
> [Bloque 242]/[Bloque 244]/[Bloque 246]/[Bloque 248] (the multi-brand pattern, now EXPLAINED as rebrand),
> [Bloque 7] (BACnet), PX blocks (clPrintout PX→PDF), [Bloque 75] (licensing).

---

## 249.1 — `clStationUpgradeTool`: the CentraLine→Honeywell rebrand migration `[CERT]` / `[CERT-a]`

`BClStationUpgradeTool extends BWbTool` (a Workbench wizard) migrates a legacy CentraLine-branded station to the
Honeywell stack. The mechanism is a hybrid: it drives Niagara's own migration binary + custom converters.

- **Native migration driver** `[CERT]`: `Runtime.getRuntime().exec` (`.../upgrade/ui/BClStationUpgradeTool.java:334`)
  runs `<niagara_home>\bin\n4mig.exe` (`:336`) with a **hardcoded `filePassPhrase = "Centraline"`** (`:335`,
  appended `-filePassPhrase:` `:342`) + a template selector (Controller=1 / Supervisor=2).
- **BOG type rewrite** `[CERT]`: `BClBogElementConverter implements BIBogElementConverter` rewrites persisted
  BACnet type specs — `clBACnetUtilities` → `hon=honBACnetUtilities` (`utils/BClBogElementConverter.java:57-59`),
  e.g. `BacnetClDevice`/`BacnetAwsClDevice` → `hon:HonBacnetDevice`; ~29 CentraLine→Honeywell type renames total
  `[CERT-a]`.
- **PX + NAV migrators** `[CERT-a]`: `BClPxFileMigrator extends BPxMigrator` rewrites `.px` (module import
  `clBACnetUtilities`→`honBACnetUtilities`, `HonScheduler`→`WebWidget` js, strips now-missing widget types);
  `BClNavFileMigrator extends BFileMigrator` applies the same substitution to `.nav`.
- **Post-upgrade repair injectors** `[CERT-a]`: `BClStatusFlagsUpdater`/`BClTrendLogsUpdater` inject one-shot
  fixer actions on the parent `BBacnetDevice`/`BBacnetNetwork` at `atSteadyState`; `BClUpdateStatusFlagsAction`
  runs a BQL query over descendant points and repairs `statusFlags` facets on legacy `ClBacnet*Point` — literal
  CentraLine-residue cleanup.

`[INFER]` This module IS the rebrand: it transforms a shipped CentraLine station (types, `.px`, `.nav`, BOG XML,
point flags) into a Honeywell-`hon:` station. Its existence is the strongest evidence that the multi-brand OEM
set (B242/B244/B246/B248) is a rebrand-in-progress, not parallel independent products.

---

## 249.2 — `clPrintout`: station documentation / PDF report subsystem `[CERT]` / `[CERT-a]`

"Honeywell Printout" (24 cls) generates printable station documentation.

- **Trigger + annotation** `[CERT-a]`: `BPrintoutStationExt extends BComponent` (`printDocumentation` action →
  `BPrintStationNotification`); `BStatementExt` auto-adds typed shadow props (`BStatementPropertyType`:
  ignore/input/output/setting) to annotate components for print.
- **Export engine** `[CERT-a]`: `export/Exporter` walks the `BStation` component tree, serializes to
  `printout.xml`, and renders each `BPxView` to PDF in-process via `com.tridium.pdf.BPxViewToPdf` +
  `javax.baja.pdf.PdfOp` — a station-tree dump + PX-graphics-to-PDF report generator.
- **External native tool** `[CERT]`: `PrintoutConfig` (`.../printout/PrintoutConfig.java:11-13`) pins the on-disk
  contract `!printout/printout.xml` + **`!printout/clPrintout.exe`**; `BPrintoutDialog` shell-execs
  `clPrintout.exe printout.xml` for final document assembly.
- **License + brand + expiry gate** `[CERT]` (`util/Helper.java`): `getFeature("Tridium", "brand")` → `brandId`
  allowed only in `{CentraLine, ComfortPoint, ComfortAndEnergy, Webs, WebsOpen, SBC, Trend, HoneywellMVC,
  HoneywellBMS}` (`:124-135`, a **9-brand** superset of the B242/B244 7-brand list), fallback
  `getFeature("Honeywell", "honPrintOut")` (`:142`); plus an EXPIRY gate on feature
  `("HoneywellCentraLine"/"Honeywell", "honLibPrt")` with a local anti-rollback `.exp` stash and a `HostId`
  `"win"` check. `[INFER]` A time-limited, brand-gated commercial add-on.

---

## 249.3 — `clExtensions`, `clProfile`, and the 4 resource bundles `[CERT]` / `[CERT-a]`

- **`clExtensions`** (2 cls) `[CERT-a]`: `BClBacnetOffsetPointExt extends BPointExtension` — a numeric-point
  **offset/trim** ext (adds the offset's `out` onto the point output unless overridden; restricted to
  `BNumericPoint`), backed by `BClBacnetOffsetPoint extends BNumericWritable` (a stripped writable with all 16
  `inX` slots hidden). Point-level only, despite the broad module name.
- **`clProfile`** (1 cls) `[CERT]`: `BCentralineProfile extends BWbProfile` — the CentraLine Workbench brand skin
  (icon `module://clProfile/res/centraLine.png` `:66`, lexicon `clAppName`/`clWelcomeTitle`). But
  `getAgents()` **steers device-manager agent selection toward the Honeywell BACnet stack** —
  `honBACnetUtilities:HonBacnetDeviceManager`/`HonBacnetOwsDeviceManager` (`:36-37`), auto-starts
  `honBACnetUtilities:HonWbService` — another rebrand-in-progress signal (CentraLine skin, Honeywell engine).
- **4 resource-only bundles** `[CERT]` (0 code, e2): `CentralineAhuPx` / `CentralineHtgPx` (AHU + Heating Px
  graphics), `CentralineLONIOr5` (LON IO/Smart-IO device defs — `module.xml`: "CLIOL82n LON IO and Smart IO.
  Rev05 Mar 2014. OK with Hawk 600E series"), `DINsymbol` ("Graphical Symbols based on DIN"). Pure graphic/
  device-definition assets, no logic — closed by proven-absence on the code axis.

---

## 249.4 — Conexiones

- **[Bloque 87]** (`clHVAC*` Centraline): the covered CentraLine control stack; U8 is its residue + the tool that
  MIGRATES it to Honeywell.
- **[Bloque 242]/[Bloque 244]/[Bloque 246]/[Bloque 248]** (multi-brand OEM): `clStationUpgradeTool`'s
  `clBACnetUtilities`→`honBACnetUtilities` rewrite + `clProfile`'s Honeywell-agent steering EXPLAIN the recurring
  7/9-brand set — CentraLine (and ComfortPoint/Webs/SBC/Trend/…) are being folded into the Honeywell stack.
- **[Bloque 7]** (BACnet): the migration operates on BACnet device/point types; `clExtensions` is a BACnet-point
  offset ext.
- **PX blocks**: `clPrintout` renders `BPxView`→PDF (`com.tridium.pdf.BPxViewToPdf`) — a consumer of the PX
  graphics stack.
- **[Bloque 75]** (licensing): the 9-brand + expiry + HostId gate in `clPrintout` is the licensing pattern at its
  most elaborate (brand ∪ feature ∪ expiry ∪ host).

---

## 249.5 — Self-verify

- **Re-verified by me** (`[CERT]`): `n4mig.exe` + `filePassPhrase="Centraline"`
  (`BClStationUpgradeTool.java:334-342`), `clBACnetUtilities`→`honBACnetUtilities` rewrite
  (`BClBogElementConverter.java:57-59`), `clPrintout.exe` path (`PrintoutConfig.java:13`) + the 9-brand license
  gate (`Helper.java:124-142`), `BCentralineProfile extends BWbProfile` + Honeywell-agent steering
  (`BCentralineProfile.java:31,36-37,66`). `[CERT-a]` = the sweep's structural citations (Exporter/migrators/
  injectors). `[INFER]` = the rebrand thesis.
- **Block TYPE**: EVIDENCE. U8 covered; the rebrand-migration finding is the highest-value cross-block insight
  of the focus so far.
- **New gaps queued**: none. Next per RESEARCH-STATE-oem-honeywell-tail: U9 (Modbus smart-sensor +
  plantController migrators), or U1b/U1c.
