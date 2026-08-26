# §18 Retro — focus: signing-pki (dynamic closure B525–B531) — 2026-08-25/26

<!-- review-status: pending -->

> Self-retrospective (METHODOLOGY §18). Proposes kit deltas only; does NOT edit the kit.
> Evidence references: blocks B525–B531, commits 1a7b229…b2cff6b, RESP-sigue
> RESEARCH-STATE-signing-pki.md (investigable=0, requires-execution=0 at close).

---

## Run summary

- **Focus:** signing-pki — the second half of the dynamic closure: after B522/B523/B524, this run
  closed the remaining backlog (SP-G10a license mirror, SP-G9a live provider order, SP-G8 OTA, SP-G6 CRL)
  with the Frida-independent `nre -@javaagent` technique discovered mid-run.
- **Session:** self-paced, operator present (steered with "dale" / "sigue" / clarifying questions).
- **Blocks:** B525 (dynamic hardening runbook, document mode), B526 (dynamic-vs-static consistency
  audit), B527 (SP-G10 session ledger), B528 (SP-G10a license mirror via `-@javaagent`), B529 (SP-G9a
  live provider order), B530 (SP-G8 OTA receive), B531 (SP-G6 CRL + the two-gates clarification).
- **Reversibility:** license tamper + force runs all restored byte-identical (sha256 == baseline);
  live PIDs unchanged (niagarad 21348, station 18524); zero install residue.
- **Notable:** one wall re-typed three times before the real cause was found (D1); a toolkit
  `--sync-state` destructively stripped a semantic marker and was reverted (D2); archive gate still
  blocked by pre-existing corpus-wide drift (D3).

---

## Proposed kit deltas

| # | Title | Evidence | Priority | Kit file / section |
|---|---|---|---|---|
| D1 | Three successive wrong tool-walls before the real path. SP-G10a was typed blocked-on-tool as "bare-bone agent" (B524 notes), then "embedded-JVM no jvm.dll" (corrected), then "agent built without Java bridge" — the real unblock was `nre -@<option>`'s JVM pass-through, found by reading the LAUNCHER's own `-help` (not a decompiler, not Frida). The kit's TOOL-BEFORE-AGENT and WALL rules push toward provisioning/replacing the SAME tool rather than asking "does the TARGET already expose an equivalent instrument?" | `nre -help` lists `-@<option> pass option to Java VM`; `-@verbose:class` + `-@javaagent:…` solved both "see Java" and "instrument Java" with zero new installs. | **MEDIUM** | `PROMPT-LOOP.md` HARD RULES / METHODOLOGY §21: when a WALL is hit, add a rung "enumerate the TARGET's own launcher/CLI options for an equivalent instrument before provisioning a replacement tool." |
| D2 | A kit `--sync-state` invocation (research-sdd-status.sh) DESTRUCTIVELY stripped the semantic `method: document-cycle-external` marker from another focus's RESEARCH-STATE while mechanically reconciling `covered_blocks`. The tool is not safe to run corpus-wide as-is; its output had to be reverted by hand. The marker loss is worse than the drift it fixed (METHODOLOGY §20 depends on that marker to distinguish external-document corpus from an abandoned loop). | `git diff` showed `-method: document-cycle-external` deleted from RESEARCH-STATE-api-access.md; reverted via checkout. | **HIGH** | kit (report only, not edit): `research-sdd-status.sh --sync-state` must PRESERVE `method:`/`block_scope:` preambles (or refuse with a WARN when it would rewrite one), and should scope to `--focus` without silently mutating 16 sibling files. |
| D3 | The archive gate's `verify-state` FAIL is corpus-wide and pre-existing: ~16 focus files have stale `covered_blocks` (drift from a 523→527-block corpus), 2 files use UPPERCASE priorities (`HIGH`/`MED`/`LOW-MED`) violating the parser grammar, and several lack `block_scope: shared-global`. None of this is signing-pki's debt, but it BLOCKS `research-sdd-archive.sh` for the whole target. The kit gives no way to archive ONE focus while the rest of the corpus carries unrelated debt. | `research-sdd-archive.sh --dry-run` → `verify-state FAIL`; the FAIL list is entirely other focuses. | **MEDIUM** | kit (report only): `research-sdd-archive.sh` should support a `--focus <slug>` scope (or a `--scope-corpus` flag) so a clean focus can archive without blocking on unrelated focuses' debt. |
| D4 | `verify-state`'s `blocked_open` derives from a `## Blocked gaps` section (`- name — needs: …`), NOT from backlog-table `Status` cells — the signing-pki file carried its blocked gaps only in the table, so the envelope read `blocked_open=0` against a declared 4. The grammar is load-bearing but only discoverable by reading verify-state.sh internals; the RESEARCH-STATE template doesn't spell it out. | RESEARCH-STATE-signing-pki.md had no `## Blocked gaps` section; adding it (with `tried:`) reconciled `blocked_open=4/4`. | **LOW** | `research-sdd` templates/RESEARCH-STATE template: document the `## Blocked gaps` grammar (`- name — needs: …; tried: …`) next to the backlog format constraint so authors don't half-record blocked gaps. |

---

## Reinforced observations (already in kit — not new deltas)

| Obs | Kit coverage | Notes |
|---|---|---|
| DISK-FIRST avoided a needless live probe | PROMPT-LOOP DISK-FIRST | SP-G8 was typed requires-execution but closed statically (`crcValid=true` hardcoded, 3 decompilers). No instrument spent on the station. |
| The `-@javaagent` path is a reimplemented observer, not relinked vendor code | METHODOLOGY §19 reimplement-do-not-relink | Agent compiled against the install's own asm-9.6.jar, instruments in a disposable process, redistributes nothing. Held. |
| `verify-block` citation WARN is cosmetic for block-reference citations | §11 verify-block | Blocks citing `[Bn]` refs (not file:line) pass exit 0 with a WARN; the real citations live in the referenced blocks. Not a correctness gap. |
| Re-measure ground truth each phase | METHODOLOGY §12 | sha256/PID invariants re-measured in B528/B529, not inherited from B518. Held. |

---

## Open gaps at close

- None investigable/executable — **focus DONE** (investigable=0, requires-execution=0).
- Blocked-on-artifact (not research walls): SP-G3a (isolated station/VM), SP-G4 (non-OEM install),
  SP-G9b (`fips140-2`-licensed install).
- Corpus-wide debt surfaced (for a separate reconciliation pass): 16 stale `covered_blocks`,
  2 UPPERCASE-priority backlogs (`oem-honeywell-tail`, `protocols`), several missing `block_scope:
  shared-global`.
