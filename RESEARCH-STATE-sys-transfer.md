# RESEARCH-STATE — focus: sys-transfer (ACTIVE)

> Multi-focus corpus (METHODOLOGY §16). SEEDED directly (structure self-evident from class names + prior-coverage
> reconciliation) on 2026-08-28 — no delegated sweep needed for a 16-class, clearly-factored package.
>
> **Angle (§b2):** the general `com.tridium.sys.transfer` TRANSFER-STRATEGY framework — the engine behind
> Workbench cut/copy/paste/move/delete, drag-drop, and programmatic component/file transfer, plus the
> `DeployToComp`/`ReplacingContext` primitives the template subsystem consumes. Genuinely unopened: B200/B578/
> B579/B583/B593 mention `DeployToComp`/`ReplacingContext` only as CONSUMERS (template deploy). Read-only,
> decompiled-Java (`organized/baja/…`). Corpus language = **English**.
>
> **Scope:** the transfer-strategy engine. Does NOT re-derive the template deploy/upgrade that USES it ([B578]/
> [B579]), the component-space model ([B408]), or the Fox wire ([B134]).

<!-- research-state.v1 -->
schema: research-state.v1
block_scope: shared-global
covered_blocks: 4
gaps_closed: 4
known_gaps: 5
investigable_open: 1
requires_execution_open: 0
blocked_open: 0
<!-- /research-state.v1 -->

focus: sys-transfer
status: active (4/5; ST1→B595 … ST4→B598 DONE; NEXT ST5 remote transfer + WB consumer)
seeded_from: direct seed (structure self-evident) + prior-coverage reconciliation 2026-08-28
seeded_on: 2026-08-28
gaps_total: 5 investigable (ST1–ST5)
gaps_closed: 0
block_prefix: niagara-mental-model-bloqueN.md (shared global numbering)

## Surface (16 classes, `organized/baja/baja/vineflower/com/tridium/sys/transfer/`)

`TransferStrategy` (abstract factory+base), `Mark` (source marker — actually in `javax.baja.space`), concrete
strategies by source→dest: `CompToComp`, `CompToBog`, `FileToComp`, `FileToFile`, `DeployToComp`,
`IntraCompSpaceMove`, `IntraFileSpaceMove`, `ToNavFolder`, `DeleteOp`, `RemoteIntraSpace`, `RemoteTransferSpace`;
`ReplacingContext` (BasicContext, replace mode); `TransferResult`/`CompTransferResult`/`TransferListener`;
`TransferStrategy` support. **Consumers (verified):** `workbench-wb/com/tridium/workbench/transfer/{TransferUtil,
TransferArtifact}` (WB cut/copy/paste), `box/util/PasteRecord`, `fox/sys/broker/TransferCodec` (remote wire),
`template-wb/…/file/{BWbDeployableNtplFile,TmplUtil}`.

## REMITTANCE (cite, do NOT re-derive)

- `DeployToComp`/`ReplacingContext` as USED by template deploy/upgrade → **[B578]/[B579]/[B583]/[B593]** (consumer)
- Component-space model (BComponentSpace/rootComponent) → **[B408]**
- Fox remote transport → **[B134]** (foundation)

## Gap-backlog (prioritized)

| Priority | Gap | Scope | Where (`organized/baja/…/com/tridium/sys/transfer/`) | Status |
|---|---|---|---|---|
| high | ~~**ST1 strategy factory + dispatch**~~ | make(action=COPY16/MOVE32, Mark[values+names], target); makeImpl dispatches by target-type then source-space×action; same-space MOVE→IntraCompSpaceMove(fw113), remote→RemoteIntraSpace, cross→CompToComp, BIDeployable→DeployToComp; one router for all transfer | — | **CLOSED → B595** |
| high | ~~**ST2 DeployToComp + ReplacingContext**~~ | deploy is POLYMORPHIC (BIDeployable.getSteps supplies steps, strategy drives them; exact=TRUE for .ntpl); ReplacingContext IS the handle-preservation primitive (addAllHandles→restoreHandles), the mechanism B578/B579 relied on | — | **CLOSED → B596** |
| medium | ~~**ST3 component strategies**~~ | CompToComp (copy/move by copySource flag, cross-space); IntraCompSpaceMove (same-space re-parent, identity-preserving); CompToBog (export to fresh in-memory bog); ToNavFolder (nav bookmarks); DeleteOp (niagaraRemoveLinks facet) | — | **CLOSED → B597** |
| medium | ~~**ST4 file strategies + results**~~ | FileToFile (stream copy)/IntraFileSpaceMove/FileToComp(→CompToComp); TransferResult abstract undo() = every transfer is an UNDO token; CompTransferResult undoCopy/undoMove (makes WB paste undoable, B432); TransferListener progress hook | — | **CLOSED → B598** |
| medium | **ST5 remote transfer + WB consumer** | RemoteIntraSpace/RemoteTransferSpace (cross-station cut/paste), fox TransferCodec wire, and the Workbench consumer (TransferUtil/TransferArtifact) that turns clipboard/drag-drop into a strategy | `Remote*.java` + `fox/…/TransferCodec.java` + `workbench-wb/…/transfer/{TransferUtil,TransferArtifact}.java` | **NEXT** |

## Proven-absent / notes

- No existing block on the transfer framework (B200/B578/B579/B583/B593 = consumers only, reconciled).
- `Mark` lives in `javax.baja.space` (the source marker), not the transfer package.

## Stop control (METHODOLOGY §8)

- **Open gaps — read-only investigable**: 1 (ST5). Focus ACTIVE.
- **Gaps closed**: 4 (ST1→B595 … ST4→B598).
- **Coverage metric**: 0 / 5.
