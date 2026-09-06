# C8 PR19 — write-path doctrine (`feat/c8-write-path`) apply-ready draft

> For the PR19 apply worker: paste the READY blocks at the named anchors. Doctrine + matrix from B816 §816.6;
> matrix column shape is the client repo `angeles725/niagara-panccadia-leon` `docs/write-path-matrix.md` @ deed38c
> (13 pure-seam rows W1–W13). Every new line is tagged `[ev: corpus B816]`. **Grep-before-fold (K6):**
> `grep -niE 'write.path|overlap|LINK.TARGET|ephemeral' types/logic.md types/logic-authoring.md` → 0 hits — both
> sections are NEW.

---

## A — `types/logic.md`: NEW section `## Write-path & overlap`
Place after `## Staging & interlocks` (a runtime-behavior peer). The runtime FACTS an author must know:

```
## Write-path & overlap `[ev: corpus B816]`
- **`set()` runs on the CALLING thread; only the raw value store is locked:** a servlet/Workbench write and an engine `execute()`/Clock callback on the SAME component serialize ONLY for the raw store (`synchronized(this.instance)`, last-writer-wins, no torn value). Their CALLBACKS — `changed()` and link propagation — run OUTSIDE that lock, synchronously, and CAN interleave; `changed()` is re-entrant (a `changed()` that calls `set()`/`schedule()` recurses on the stack). Overlap bugs are real and live in the CALLBACKS, not the store. `[ev: corpus B816]`
- **A dashboard write to a LINK-TARGET slot LANDS, then is silently overwritten:** the `Flags.LINK_TARGET` bit is advisory metadata checked ONLY at link-CREATION in the wiresheet — never by `set()` nor `canWrite()`. A manual write to a link-driven slot sticks only until the next source propagation re-`set`s it (no rejection, no exception). A write to a NON-linked `OPERATOR` slot does stick. The UI must NOT imply a link-target write persisted. `[ev: corpus B816]`
- **A `Transaction` is NOT cross-thread atomic:** it queues ops and replays them FIFO through the same per-slot lock; there is no global lock across the batch and no cross-batch isolation — the engine thread can observe a half-written multi-slot state. Do not rely on a Transaction to hide interleaving. `[ev: corpus B816]`
```

## B — `types/logic-authoring.md`: NEW section `## Write-path test matrix`
Place after `## Minimal module (copy-start)` (the last section). The authoring/test discipline + the matrix template:

```
## Write-path test matrix `[ev: corpus B816]`
Every writable slot a dashboard/operator can hit gets a ROW: (writable slot × writer × timing) → the invariant it must hold, and the TEST that proves it. This is the template `lint-write-path.sh` enforces (columns FIXED — slot · writer · timing · invariant · test):

| Writable slot | Writer | Timing | Invariant | Test |
|---|---|---|---|---|
| `setpoint` | Dashboard / Workbench | mid-cycle (latched) | cv in new band → HOLD, no chatter; crosses → flip exactly once | `w1_setpointChangeWhileLatched` |
| `hoaMode` | Dashboard operator | mid-cycle | HAND→ON, OFF→OFF, AUTO→autoValue | `w3_hoaFlipMidCycle` |
| `defrostInterval` | Workbench | mid-cycle, shortened → overdue | new interval < elapsed → `1L`, never `Clock.schedule(0)` | `w6_intervalWriteMidCycleOverdue` |
| a LINK-TARGET slot | Dashboard | any | write is EPHEMERAL (overwritten next propagation) — UI must not imply it stuck | (row required; assert overwrite) |
| `resistanceMode` (HOA) | Dashboard operator | mid-defrost | OFF LOCKS OUT the heater even during the defrost sequence (OFF > sequence > HAND > AUTO); re-applies after exitDefrost | (client PR #4) |

**`lint-write-path.sh` (columns it requires):** slot name · writer · timing · **test name that EXISTS in `srcTest/`**. It bites when:
- **HARD** — an `OPERATOR`-writable slot a dashboard writes to has NO matrix row, OR its row's Test column names a test absent from `srcTest/`; also a `Clock.schedule`/`schedulePeriodically` reachable with a computed `≤ 0` delay (the armTrigger class — cross-ref lint-delays / B801). `[ev: corpus B816]`
- **WARN** — a dashboard write path targeting a LINK-TARGET slot (silently overridden — a footgun). `[ev: corpus B816]`
- **REVIEW** — a `changed()` that re-enters `set()`/`schedule()` on the same component (re-entrancy); reliance on a Transaction for cross-thread atomicity (there is none). `[ev: corpus B816]`
Coverage legend for the row's Test cell: a real `srcTest/` test name (lint checks it exists), `🔶` an earlier-campaign test, or `❌ C9` for an invariant that needs the rt-lifecycle seam (issue #815 — changed()-ordering, minOff/minOn, seedRestart). `[ev: corpus B816]`
```

## C — WHY the lint exists (one paragraph, for the retro/PR body)
DashboardPan can write the SAME slot the control logic and the links drive, and the three overlap silently. Two live
classes proved it: **HAND/OFF vs. a dashboard write** — an operator `hoaMode=OFF` must lock the output OFF in ANY mode
and even mid-defrost (OFF > sequence > HAND > AUTO), so a concurrent dashboard/auto write can't re-energize it (client
PR #4, resistance-off-lockout); and **the LINK_TARGET advisory-flag trap** — a dashboard write to a link-driven slot
LANDS with no error yet is silently overwritten on the next propagation, so a UI that shows it as "set" lies to the
operator (client PR #3). Neither is a race in the value store (that's last-writer-wins, atomic per slot); both live in
the CALLBACKS and the link path, which no value-store test can see. `lint-write-path.sh` forces every operator-writable
slot to declare its writer × timing × invariant and name a test that actually exists — turning "did we think about the
overlap?" from a review hope into a gate. `[ev: corpus B816; client PR #3, PR #4]`

---
## Apply-worker notes
- Both sections are NEW (grep-before-fold clean); paste under the named anchors; every line keeps `[ev: corpus B816]`.
- The matrix TEMPLATE rows above are illustrative from the client matrix (W1/W3/W6 + the two overlap classes); the kit
  doc ships the TEMPLATE + column contract, not the client's 13 rows verbatim (those live in the client repo).
- `lint-write-path.sh` is authored in a sibling PR; this doc only specifies its column contract + the HARD/WARN/REVIEW
  bite table so the tool and the doc agree (K19: name the script in BUILD-LOOP + SKILL when it lands).
- Close: PR19 is a kit change → `new-retro.sh kit c8-pr19-write-path` + `retro_pending` in the kit self-envelope,
  same push range (BUILD-LOOP §7 envelope-pairing).
