# Niagara N4 — chihuahua-source (CS7): the wb `BBatchLinkEditor` is a proper Workbench authoring view (`BWbComponentView`, `@AgentOn baja:Component requiredPermissions="rwi"`) for bulk-creating links with per-space transactional commit — distinct from the rt-side `ChiLinkHelper` (backup/restore)

**Focus**: chihuahua-source · **Gap**: CS7 (wb Batch Link Editor) · **Session**: 2026-08-29 · **Block**: B654
**Sources** (`[CERT]` real source): `chihuahua-wb/src/com/angeles/chihuahua/wb/BBatchLinkEditor.java` + `model/{PendingLink,PendingLinkBuilder,LinkSlotNameUtil,DirectionButtonUtil,DirectionLabelUtil,SearchResultUtil}.java`.

**Scope**: the Workbench tooling profile of the production module. WB view framework = [B427]-[B432] (REMIT); the rt link helper = [B650] (ChiLinkHelper).

---

## 654.1 A real Workbench view, correctly agent-registered

`[CERT]` `BBatchLinkEditor.java:69` — `public final class BBatchLinkEditor extends BWbComponentView` (the Workbench component-view framework, [B427]/[B431]). `[CERT]` `:67` — `agent = @AgentOn(types = "baja:Component", requiredPermissions = "rwi")`: it registers as a right-click View on ANY `baja:Component`, gated on read+write+invoke permissions. Slotomatic ([B631]) generates the `_BBatchLinkEditor` companion + emits the `<agent>` block into `META-INF/module.xml` ([B632]: this is the `<agent><on type="baja:Component"/></agent>` seen in the jar). So the editor is a properly-declared WB agent — matching [B12] §12.3.4's popup/agent registration model.

---

## 654.2 The batch-link workflow (accumulate → validate → transactional commit)

`[CERT]` — the editor lets an engineer accumulate heterogeneous From/To links across multiple Accept actions, then commit atomically:
1. `doLoadValue` (`:205`, ~195 L) builds a split-pane UI (equip slot list · BNavTree + search + remote slot picker · pending-links list).
2. From/To toggle (`:219-244`) sets link direction (FROM = equip fed from remote; TO = equip feeds remote).
3. Accept (`:563`) confirms via `BDialog.OK_CANCEL`, builds a `PendingLink` (`PendingLinkBuilder.fromDirection`), **dry-runs `checkLink`**, tags `[OK]`/`[!]`, appends to parallel pending lists.
4. Find (`:683`) DFS's the component space (handles `Sys.getStation()==null` in the WB client process, `:696`), case-insensitive name match.
5. Save All (`:921`): **Phase 1** re-validate ALL with `checkLink`, abort on any invalid; **Phase 2** group by `target.getComponentSpace()`, **one `Transaction` per space** (avoids nested transactions, `:1022`), `target.makeLink` + `target.add(uniqueSlotName, link, tx)` + `tx.commit()`, slot names from `LinkSlotNameUtil.generate()` with a per-target reservation set; **Phase 3** `clearModified()` (not `setModified()` — the button bypasses `saveValue()`, `:1087`).

This is careful WB engineering: dry-run validation before commit, all-or-nothing per space, unique-slot-name collision avoidance ([B432] transfer/transaction model).

---

## 654.3 Distinct from the rt `ChiLinkHelper`

`[CERT]` — two different link tools, no shared code:
- **`BBatchLinkEditor`** (wb) — a design-time AUTHORING tool: an engineer interactively bulk-creates NEW links in Workbench.
- **`ChiLinkHelper`** (rt, [B650]) — a runtime BACKUP/RESTORE utility: the station snapshots existing links to `chih-links.json` and restores them across restarts.

They address complementary needs (author vs persist) and correctly live in different profiles (wb authoring / rt runtime) — the [B630]/[B12] profile split done right.

## 654.4 Model classes — pure-Java, testable

`[CERT]` `model/` — `PendingLink` (immutable 6-String DTO, no Niagara types), `PendingLinkBuilder.fromDirection` (FROM/TO→source/target, pure-Java), `LinkSlotNameUtil` (unique "Link"/"Link1"/… with a collision predicate), `DirectionButtonUtil`/`DirectionLabelUtil`/`SearchResultUtil` (UI text + `[N] name — path` format/parse). All pure-Java = the WSL-testable seam (the shop pattern: keep logic in Niagara-free helpers, cf. [B637] tests). One `[INFER]` comment (`:910`, a smoke-test note on `setModified`), no `System.out`, no TODO/FIXME.

---

## 654.5 Grade

`BBatchLinkEditor` is a well-built Workbench view: correctly agent-registered (`rwi`-gated), dry-run-validated, transactional per space, with pure-Java testable model classes. It is a genuine engineering-productivity tool (bulk linking) not present in the shop's other dashboards — a strength of the production module. Correct profile placement (wb) and cleanly separated from the rt link helper.

---

## Self-verify

| # | Claim | Marker | Citation | Checked |
|---|---|---|---|---|
| 1 | BBatchLinkEditor extends BWbComponentView; @AgentOn baja:Component requiredPermissions="rwi" | [CERT] | BBatchLinkEditor.java:67,69 | ✅ read verbatim |
| 2 | workflow: accumulate From/To → Accept dry-runs checkLink → Save All re-validates + per-space Transaction + unique slot names | [CERT] | BBatchLinkEditor.java:563,921,1022 | ✅ read |
| 3 | distinct from rt ChiLinkHelper (author vs backup/restore), no shared code, different profiles | [CERT] | BBatchLinkEditor (wb) vs ChiLinkHelper (rt, [B650]) | ✅ read+cross-ref |
| 4 | model/ classes pure-Java testable (PendingLink DTO, LinkSlotNameUtil, etc.); no System.out/TODO | [CERT] | model/*.java | ✅ read |

**Tally**: [CERT] ×4 · [INFER] ×0 · real-source block. Agent registration + transactional commit token-checked verbatim.

## Connections

- **[B427]-[B432]** — WB view/manager/transaction framework (REMIT). **[B631]/[B632]** — slotomatic-generated agent block in module.xml. **[B12]** §12.3.4 — popup/agent registration. **[B650]** — the rt ChiLinkHelper (complementary). **[B637]** — pure-Java testable seam pattern.
- Forward: CS8 (verdict: wb tooling is a strength).

## Gaps uncovered

- None. The wb layer is clean.
