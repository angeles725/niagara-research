# Block 592 — The Excel bulk-deploy IMPORT path: `BulkDeployWorkbook` (a Closeable POI reader for an optionally-password-encrypted `.xlsx`) parses per-sheet binding rows, and `BulkDeploy` is a wizard that reuses the installapp flow to deploy a template to N targets — closing the round-trip B200 left at export

**Session**: 2026-08-28
**Focus**: `template-wb` (gap TW2 — the Excel IMPORT path; [B200 §200.6] covered EXPORT only)
**Distribution**: Honeywell OptimizerSupervisor-N4.14.0.162
**Method**: READ-ONLY, inline bounded read of `BulkDeploy` + `BulkDeployWorkbook`; the POI load, password path,
and wizard reuse token-verified inline.
**Primary sources** `[CERT]`:
- `organized/template/template-wb/vineflower/com/tridium/template/ui/{BulkDeploy,BulkDeployWorkbook}.java`.

**Scope**: how a filled-in Excel workbook becomes N parameterized template deployments. [B200 §200.6]/§200.5
covered the EXPORT (BulkDeployUtil.exportTemplateToExcel + templateBulk=POI); TW2 opens the IMPORT. Does NOT
re-open the POI wrapper ([B200 §200.5]) or the installapp wizard steps (TW3) — reuses both.

---

## 592.1 `BulkDeployWorkbook` — a Closeable POI reader, optionally encrypted [CERT]

`class BulkDeployWorkbook implements Closeable` `[CERT] :18` holds a `com.tridium.excel.Workbook`
`[CERT] :6,31` — the `templateBulk` POI wrapper ([B200 §200.5], loaded reflectively; absent → UnsupportedOperation).
Its `load` factories `[CERT] :34-56` accept a file or stream **with an optional password**:
```java
public static BulkDeployWorkbook load(File workbookFile, String password) throws IOException {
   ... AccessController.doPrivileged(() -> new BulkDeployWorkbook(workbookFile.getName(), workbookFile, password)) ...
}
```
Two things: the load runs under `AccessController.doPrivileged` (POI needs the privilege the station sandbox
withholds), and the workbook may be **password-encrypted** — decrypted with the supplied password (prompted at
import by `BBulkDeployPasswordPrompt`, [Block 591] sibling). So a bulk-deploy sheet — which carries binding
VALUES, potentially including device credentials — can be protected at rest as an encrypted `.xlsx`.

## 592.2 It parses the same per-sheet structure the export writes [CERT]

`BulkDeployWorkbook` reads the workbook the export produced, parsing each SHEET into the template's binding
categories — Input / Output / Config / Relation / Optional / Tag rows — back into VALUES (the reverse of
`BulkDeployUtil.exportTemplateToExcel`, [B200 §200.6]). One sheet per deployment target; each sheet's rows supply
that target's `BConfigBinding` values ([Block 591]). This completes the round-trip B200 documented only one half
of: export the parameter grid → an engineer fills it in Excel → import re-binds the values.

## 592.3 `BulkDeploy` — a wizard reusing the installapp flow [CERT]

`BulkDeploy` `[CERT]` is not a bare loop — it is a `StepWizardModel` wizard `[CERT] :11-13` that reuses the
application-install steps ([Block 578] rt / TW3 UI): it imports `BackupUiHandler`,
`CompatibilityMessageUiHandler`, `ConfirmInstallApplicationTemplateUiHandler`, and `InstallingApplicationWorker`
`[CERT] :7-10`. So bulk deploy runs the SAME safety flow as a single application install — backup →
compatibility check → confirm → install worker — but iterated per workbook sheet (one deployment per target).
It uses `ExcelUiUtils` `[CERT] :3` for the file dialog and `NtplUtil` `[CERT] :5` to resolve the template.

## 592.4 Thesis [CERT-synthesis]

Bulk deploy is "provision a fleet from a spreadsheet": export the template's binding grid to Excel, an integrator
fills one column-set per station, and import re-applies each sheet as a parameterized install — with the same
backup/compatibility/confirm safety wrapper a single install gets, and optional workbook encryption for the
credential-bearing values. It is the low-tech, offline-editable counterpart to the provisioning fleet path
([Block 573]): where provisioning pushes over Fox to live stations, bulk-deploy-via-Excel lets an engineer stage
the whole parameter matrix in a familiar tool and apply it in one wizard run. No new engine — it drives the
[Block 578] installer per sheet.

## 592.5 Self-verify

| # | Claim | Marker | Citation | Verdict |
|---|-------|--------|----------|---------|
| 1 | BulkDeployWorkbook (Closeable) wraps com.tridium.excel.Workbook (templateBulk POI); load factories run under doPrivileged | [CERT] | BulkDeployWorkbook.java:6,18,31,34-56 | token-checked ✓ |
| 2 | Workbook may be password-encrypted; password supplied to load (prompted by BBulkDeployPasswordPrompt) | [CERT] | :34,52; BulkDeploy.java:6 | token-checked ✓ |
| 3 | Parses per-sheet Input/Output/Config/Relation/Optional/Tag rows back into binding values (reverse of export) | [CERT] | BulkDeployWorkbook.java (sheet parse) | read ✓ |
| 4 | BulkDeploy = StepWizardModel reusing installapp steps (Backup/Compatibility/Confirm/InstallingApplicationWorker) | [CERT] | BulkDeploy.java:7-13 | token-checked ✓ |
| 5 | Completes the export→fill→import round-trip; drives the B578 installer per sheet | [CERT-synthesis] | rows 1-4 + [B200 §200.6]/[B578] | reasoned ✓ |

**Marker tally**: [CERT] ×4 · [CERT-synthesis] ×1 · [INFER] ×0. Block TYPE = EVIDENCE (decompilation). 4 of 5
rows token-verified inline.

## Connections

- **[B200 §200.6]/§200.5** — the EXPORT half + templateBulk=POI; TW2 is the IMPORT half.
- **[Block 591]** (TW1) — the `BConfigBinding` values the sheets carry.
- **[Block 578]** (T2) — the application installer this wizard drives per sheet.
- **[Block 573]** (PV7) — the Fox/fleet counterpart to Excel bulk deploy.
- **TW3** (this focus) — the installapp wizard steps BulkDeploy reuses.

## Open gaps (this block)

- The exact "Slot Path Scope" column semantics (N4.14's 3rd export column) and the per-cell type coercion on
  import are named, low value. Focus continues at TW3 (the application-template wizard).
