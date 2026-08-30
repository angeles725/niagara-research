# B707 — Module best practices, WB layer (MBP3): when a `-wb` part is actually needed, the Manager/View/FieldEditor patterns, and why station logic must never live in `-wb`

> Focus: **module-best-practices** · Gap **MBP3** (wb layer / Workbench Swing). Block TYPE = **DESIGN/SYNTHESIS**
> (distilled from verified blocks + jars; high [INFER] ratio expected). Feeds `docs/module-best-practices.md` §3.
> Marker `[CERT]` where re-citing verified code; `[INFER]` for guidance.

## 707.1 — The WHEN-needed decision rule

[CERT+INFER] One question: **do you need a Swing GUI element that runs only inside Workbench, operated by an
engineer — not by the station daemon?** If yes → `-wb`. Otherwise skip it.

**Needs `-wb`:** custom device/point **Manager** (`BAbstractManager extends BWbComponentView`), a Swing
**view/tool** on a component (`BWbComponentView`), a custom **field editor** (`BWbFieldEditor`), a bulk-authoring
tool (like chihuahua's `BBatchLinkEditor`), a wizard, a nav context menu, a custom sidebar — all registered via
`@AgentOn(types=…)`. ([Block 431]/[Block 432]/[Block 430]/[Block 428].)

**Does NOT need `-wb`:** a plain `BComponent` + palette (rt only); a web dashboard or bajaux widget (pure `-ux`,
`rc/` assets, 0 Java — [Block 640] §640.5); station runtime logic (NEVER in wb); standard slot editing (the
framework's `BPropertySheet` + value-type field editors already cover it).

## 707.2 — The foundational constraint: `-wb` is invisible to the daemon

[CERT] `runtimeProfile="wb"` is enforced at boot — a station daemon starts `-rp:rt,se` and the registry is
rebuilt only for supported profiles, so a `wb` jar is **simply not present** on a headless station (not
rejected — invisible). ([Block 630] §630.6, `Nre.java:265`, `NRegistry.java:253-256`.) The filename `-wb.jar` is
convention; the manifest `runtimeProfile` is what the loader reads ([Block 630] `DefaultModulesFileManager.java:350`).
**⇒ any station-needed logic in `-wb` is broken on a JACE/supervisor, and the failure is SILENT.**

## 707.3 — The framework patterns (copy these)

[CERT]
- **Manager:** `BAbstractManager` (abstract, no `@AgentOn`) → concrete subclass adds `@AgentOn(types=…)`; override
  `makeColumns()` → `MgrColumn[]` (each does `get`/`load`/`save`/`toEditor`); rows = children via `BMgrTable.reload`
  filtered by `model.accept(row) && hasOperatorRead()` (row-level permission automatic); discovery via
  `makeLearn()` → `MgrLearn` submitting a `BJob`; commit via `MgrEdit.invoke` → `Mark.moveTo` ([Block 431]).
- **View:** `BWbView` → override `getViewMenus()`/`getViewToolBar()`; for cut/copy/paste/DnD set a
  `BTransferWidget` and implement `getTransferData`/`insertTransferData`/`removeTransferData`; clipboard currency
  = `Mark` ([Block 432]).
- **Field editor:** `BWbFieldEditor` → override `doLoadValue`/`doSaveValue`, `setModified()`/`clearModified()`;
  register `@AgentOn(types="your:ValueType")` (framework picks lowest ordinal) or per-slot facet `"fieldEditor"`;
  commit = one `Transaction` per save ([Block 430]).
- **Commands + undo:** `doInvoke` returns a `CommandArtifact` (`undo()`/`redo()`) → `UndoManager.addArtifact`
  makes it undoable; return null for non-undoable (e.g. StationSave). One-shot actions invoke imperatively
  (`new LinkCommand(...).invoke()`) rather than writing the component directly ([Block 432]/[Block 429]).

## 707.4 — chihuahua-wb `BBatchLinkEditor` — the positive exemplar

[CERT] The one production `-wb` module is a single view + a `model/` helper package ([Block 654]):
- Correctly `extends BWbComponentView`, `@AgentOn(types="baja:Component", requiredPermissions="rwi")`.
- Save flow: Phase 1 re-validate all (abort on any invalid) → Phase 2 group by `getComponentSpace()` and commit
  **ONE Transaction per space** (avoids nested transactions, `:1022`) → Phase 3 `clearModified()`.
- Its `model/` classes (`PendingLink`, `PendingLinkBuilder`, `LinkSlotNameUtil`) are **pure-Java, zero Niagara
  types** → unit-testable in a plain JRE (the shop's `run-tests-wsl.sh`, [Block 637]).
- Kept SEPARATE from the rt `ChiLinkHelper` (station-side backup/restore) — no shared code across profiles.
- Version history confirms the correct order: `1.0 → 1.1 (RBAC) → 1.2 (export) → 1.3 (wb)` — **wb added last**,
  after rt+ux were stable ([Block 649]).

## 707.5 — Anti-patterns

[CERT]
- **AP1 — Station/runtime logic in `-wb`** — invisible to the daemon; silent failure ([Block 630] §630.6). Highest severity.
- **AP2 — Over-building a Swing tool when a `-ux` view suffices** — pxEditor-wb is huge because it is a real
  authoring tool with no web equivalent; a dashboard/manager for engineers should first weigh a ux approach.
- **AP3 — Duplicating rt logic in wb** — creates drift + breaks headless; keep business logic in rt ([Block 654] §654.3).
- **AP4 — Shipping empty `-wb` jars** — `interfaz1-wb` = 0 classes/0 types, still costs a boot load slot +
  signature verify ([Block 642]). Drop it.
- **AP5 — wb logic not pure-Java** — embedding Niagara types in the tool loses WSL unit-testability ([Block 654] §654.4).
- **AP6 — Nested transactions** — group by component space, one Transaction each ([Block 654] `:1022`).

## 707.6 — Top wb improvements

[INFER]
1. **(easy win)** Drop `interfaz1-wb` (the only empty wb jar) ([Block 642]).
2. Scope permissions at the `@AgentOn` level (`requiredPermissions`), not module-level `type="all"` ([Block 644]).
3. Copy chihuahua-wb's pure-Java `model/` pattern into any new wb tool ([Block 654]).
4. Default the shop template to **rt + ux only**; add `-wb` deliberately when a Manager/BatchEditor/field-editor
   need is identified ([Block 647]).
5. Keep authoring (wb) and runtime (rt) tools completely separate ([Block 654] §654.3).

## Connections

- Distills focus `workbench` [Block 427]–[Block 432], `chihuahua-source` [Block 654], `module-anatomy` [Block 630],
  `own-modules-audit` [Block 640]–[Block 647]. rt/ux siblings → [Block 705]/[Block 706]. Deliverable:
  `docs/module-best-practices.md` §3.

## Self-verify

| # | Claim | Marker | Citation | Verified |
|---|---|---|---|---|
| 1 | wb-invisible-to-daemon (runtimeProfile enforced at boot) | [CERT] | [Block 630] §630.6 | cited |
| 2 | Manager/View/FieldEditor patterns via @AgentOn | [CERT] | [Block 430]/[Block 431]/[Block 432] | cited |
| 3 | chihuahua-wb exemplar: 1 Transaction/space, pure-Java model, wb-added-last | [CERT] | [Block 654]/[Block 649] | cited |
| 4 | empty interfaz1-wb costs boot overhead | [CERT] | [Block 642] | cited |
| 5 | when-needed decision rule + improvements | [INFER] | 707.1/707.6 | reasoned |

**Tally:** [CERT] ×4 · [INFER] ×1. Block TYPE = **DESIGN/SYNTHESIS** — ratio healthy. Re-cites verified blocks.

## Open gaps (this focus)

MBP3 CLOSED (wb). Next: **MBP4** (cross-cutting — RBAC write-gate, permissions/over-permission, audit, error
handling across layers). Then MBP5 (build), MBP6 (exemplar catalog + guide).
