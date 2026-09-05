# B788 · Conformance audit — palette/lexicon: DashboardPan-wb empty-palette footgun, partial lexicons, and a corrected "CompPan lexicon empty" claim (OMV4)

> **Scope**: CONFORMANCE AUDIT of our three modules' `module.palette` + `module.lexicon` against the idiom (B780:
> one `<p>` per exposed `@NiagaraType`, prefix lexicon keys; B759: missing key → raw camelCase; B5: empty/scaffold
> palette passes the gate but has nothing to drag). Several real findings + a CORRECTION to a stale "empty lexicon"
> claim. Focus: `own-modules-vs-exemplars` (OMV4). Routes to a client punch-list + candidate biting checks.
>
> **Sources**: FUENTE 3 our real `module.palette`/`module.lexicon` files (verified this session): ColdRoomPan,
> CompPan, DashboardPan (rt/ux/wb); reference chihuahua (rt/ux). FUENTE 1: B780 (palette/lexicon conventions), B759
> (lexicon collision), B5 (empty palette). READ-ONLY. English (post-B115).

---

## 788.1 — Palette: rt/ux conform; DashboardPan-wb is an empty-palette footgun `[CERT]`
The rt/ux palettes conform (one `<p>` per exposed `@NiagaraType`, enums/editors correctly excluded): ColdRoomPan-rt
3/3, CompPan-rt 1/1, DashboardPan-rt 2/2 (nested in Services/Rooms folders), DashboardPan-ux 1/1.
**FINDING — DashboardPan-wb ships an EMPTY scaffold palette**: `module.palette` = `<p m="b=baja" t="b:Folder"></p>`
with ZERO draggable entries (and its lexicon is comment-only). The wb part has no `@NiagaraType`, so nothing is owed
— but the empty scaffold PASSES the verify gate while offering nothing to drag (the exact B5 footgun). Sev LOW (a wb
part with no components legitimately has nothing; the footgun is that the gate can't tell "empty because nothing to
expose" from "empty because the author forgot").

## 788.2 — Lexicon: one CONFORMS, two partial, and a CORRECTED claim `[CERT]`
- **CompPan-rt CONFORMS — 56 keys** (`CompPan-rt/module.lexicon`, verified count 56; e.g. `CompressorControl=Control
  de compresores`, `room1Calling=Cuarto 1 llamando`). **CORRECTION: the prior "CompPan-rt.lexicon empty (T8)" claim
  (build-kit BUILD-STATE + memory) is FALSE** — it is fully populated. Drop that flag.
- **ColdRoomPan-rt PARTIAL — 32 keys**: type names + core slots covered, but operator-facing slots MISSING
  (`fanMode`, `valveMode`, `resistanceMode`, `freezeSetpoint`, `freezeProtect`, `freezeDiffStop/Restart` — grep=0) +
  the BFanMode enum options → those render raw camelCase (B759). Sev MED (operator-visible).
- **DashboardPan-rt PARTIAL — ~25% slot coverage**: 21 keys vs ~55 missing operator/config slots
  (`evap{1,2,3}{FanMode,ValveMode,FreezeSetpoint,…}`, `zone/evap High/LowLimit`, `comp{1,2}Mode`, all `*Label`,
  `Cuarto4/5`, `startDelay`). The weakest of the three. Sev MED.
- **No DUP bare keys in ANY lexicon** (uniq -d clean across all files) — the B759 dup-collision risk is theoretical
  here, not present.

## 788.3 — Prefix discipline: flat bare keys, safe today `[CERT/INFER]`
All our lexicons use FLAT bare slot-name keys (`setpoint=`, `condenser1=`), not the `Type.slot` prefix B780 recommends.
Safe today because each bare slot name maps to one intended label per module JAR (`setpoint`=Consigna in ColdRoomPan
vs Setpoint in DashboardPan live in separate jars → no cross-module collision). The one live within-module risk is
DashboardPan if `BRoomPanel` and `BDashboardService` ever wanted different labels for the same bare slot name — not
currently the case. [INFER] low risk; prefixing is a hardening, not an active defect.

## 788.4 — chihuahua reference: palette-complete, lexicons EMPTY BY DESIGN `[CERT]`
chihuahua-rt palette = 7 entries (grouped Services/Monitors/Equipment); chihuahua-ux = 1/1. Its lexicons (rt AND ux)
are EMPTY (comment-only, 0 keys) — every WB slot label falls back to toFriendly camelCase. It gets away with it
because the operator UI is the servlet-rendered HTML dashboard (BChiServlet), NOT the WB property sheet. So the
reference baseline is "palette-complete, lexicon-absent"; ALL THREE of our modules are MORE lexicon-conformant than
the reference (DashboardPan-rt the weakest). `BPlanta` is intentionally palette-OMITTED (auto-seeded as fixed
Planta1..Oficinas children; dragging it would break the fixed naming) — a legitimate exception to "one `<p>` per type".

## 788.5 — Two routings `[INFER, grounded]`
1. **Client punch-list**: DashboardPan-wb — delete the empty scaffold palette/lexicon or populate it (B5);
   DashboardPan-rt — add the ~55 missing `BRoomPanel` operator/config lexicon keys (B759); ColdRoomPan-rt — add
   `fanMode`/`valveMode`/`freezeSetpoint`/`freezeProtect` + BFanMode enum-option keys; CompPan-rt — clean.
2. **Kit implication — candidate BITING CHECKS for `verify-module.sh` (QA RED-first)**:
   - **B (ship as FAIL, fully deterministic)**: lexicon DUP bare keys — `grep -v '^#' | cut -d= -f1 | sort | uniq -d`.
     A repeated key is always a last-wins bug; near-zero FP. (Clean on our corpus today → a good regression guard.)
   - **A (WARN)**: empty-palette — a `module.palette` whose `<p>` tree has zero non-Folder entries WHILE the module
     ships ≥1 non-enum/non-editor `@NiagaraType`. Catches DashboardPan-wb-style scaffolds; scope to "has components"
     to avoid FP on legit editor/servlet-only parts.
   - **C (WARN with threshold)**: exposed-type/slot lexicon coverage % — a key per `@NiagaraType` (bare type name) +
     per user-facing slot (flags SUMMARY|OPERATOR and NOT TRANSIENT/HIDDEN); report %, WARN below ~80%. Needs the flag
     filter to exclude internal timers (`*Expired`, `tick`) — else noisy; and chihuahua-style servlet UIs legitimately
     run at 0%, so WARN not FAIL.

## Self-verify

| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | rt/ux palettes conform (one `<p>` per exposed type); DashboardPan-wb palette is an empty `b:Folder` scaffold | [CERT] | DashboardPan-wb/module.palette (Folder only, 0 entries) |
| 2 | CORRECTION: CompPan-rt lexicon is NOT empty — 56 populated keys | [CERT] | CompPan-rt/module.lexicon (count 56) |
| 3 | ColdRoomPan-rt lexicon partial (32 keys; fanMode/freezeSetpoint missing → camelCase) | [CERT] | ColdRoomPan-rt/module.lexicon (grep fanMode/freezeSetpoint = 0) |
| 4 | DashboardPan-rt lexicon ~25% coverage (~55 operator/config slots missing) | [CERT] | DashboardPan-rt/module.lexicon vs src slot scan |
| 5 | No DUP bare keys in any lexicon (B759 dup-collision theoretical here) | [CERT] | uniq -d clean across all files |
| 6 | chihuahua reference: palette-complete, lexicons EMPTY by design (servlet UI); BPlanta palette-omitted intentionally | [CERT] | chihuahua rt/ux lexicon 0 keys; BChiDashboardService auto-seed |

**Tally**: 5 [CERT], 1 [CERT/INFER]. No unmarked claims. Findings + the CompPan correction grep-verified inline this session.

## Connections
- **B780** (palette/lexicon conventions — the conformance standard), **B759** (lexicon collision/camelCase — the
  partial-lexicon consequence), **B5** (empty palette footgun — DashboardPan-wb). **B760** (punch-list — adds items).
  **build-n4-module kit T8** — §788.2 CORRECTS its "CompPan-rt.lexicon empty" claim (a kit-side delta for campaign6).

## Open gaps
- **OMV4-G1** — the exact set of missing DashboardPan-rt slots is enumerated read-only; whether each renders visibly
  camelCase in Workbench vs is masked by the servlet UI is a live-check (requires-execution), like chihuahua's empty
  lexicon being invisible behind its servlet.

## Kit implication (→ `verify-module.sh` biting checks + a kit correction + client punch-list)
Propose 3 checks: (B) lexicon DUP-bare-keys as a deterministic FAIL (QA RED-first, clean regression guard today);
(A) empty-palette WARN scoped to modules shipping components; (C) lexicon type/slot coverage-% WARN below threshold
(flag-filtered). KIT CORRECTION: the build kit records "CompPan-rt.lexicon empty (T8)" — it is now FALSE (56 keys);
fix or drop that note. Client punch-list per §788.5.
