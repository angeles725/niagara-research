# §18 Self-Retrospective — ports focus

> **Focus**: `ports` — per-port reference (purpose · config · auth gate · reachability) for every N4 listening port. Consolidation + gap-fill over a mature corpus.
> **Run date**: 2026-08-29
> **Blocks**: B620–B627 (8 blocks; 7 investigable gaps PO-G1..G7 closed + PO-G8 synthesis; PO-G7w requires-execution deferred)
> **Review status**: pending

---

## NOT PROPOSED — already covered by the kit

The following candidate observations were deduped against `METHODOLOGY.md` and `PROMPT-LOOP.md`; they are well-covered and the run demonstrated them working correctly.

| Observation | Where covered | Evidence from this run |
|---|---|---|
| Sub-agent sweep findings are hypotheses, not facts — verify before writing a block | PROMPT-LOOP §3 **VERIFY BEFORE ACTING** ("A delegated finding is a hypothesis with citation, not a fact. Before writing a block… resolve at least the `file:line` citations") | The audit sweep's claims (PO-G4: "SC bypasses session via TLS-cert"; PO-G5: "TWO OPC-UA endpoints") were each investigated and code-measured before being written as blocks |
| Gap premises formed from a sweep are hypotheses | PROMPT-LOOP BOOTSTRAP step e **GAP PREMISES ARE HYPOTHESES** ("the initial research plan is a best guess from outside the code") | Both PO-G4 and PO-G5 were formed as questions ("does SC bypass?", "does :52443 listen?"), not as assertions |
| A dramatic negative (proven absence) requires a second measurement before asserting it | PROMPT-LOOP §3 **RE-MEASURE A DRAMATIC NEGATIVE** | B624 explicitly cites the RE-MEASURE: "0 usages → no :52443 listener" was confirmed by a full grep across opcUaServer-rt, not a single file check |
| Pre-declaring remittances before the audit sweep avoids inflating the backlog | PROMPT-LOOP BOOTSTRAP step e **PRE-DECLARE REMITTANCES FIRST** | RESEARCH-STATE carries 14 remittance rows pre-declared before the sweep; the sweep seeded only the 7 genuinely new gaps |

---

## Proposed deltas

---

### DELTA-1 — Name the "audit-sweep security/existence surprise" as a specific hypothesis subclass

**WHAT**: When the AUDIT-FIRST sweep (a delegated general-purpose sub-agent) asserts a SECURITY mechanism or an EXISTENCE claim — e.g. "this endpoint bypasses session auth" or "this port has TWO listeners" — that assertion should be explicitly labeled in the gap description as a SWEEP HYPOTHESIS (not embedded as a known fact), distinct from a structural or behavioral premise.

**WHY (block/commit evidence)**: B622 §622.3 titles a section "Correction of the audit-sweep hypothesis + characterization" — the sweep stated the SC hub "bypasses the Niagara session" via TLS-cert admission; B622 refuted it by code-reading BJettyScWebSocketAcceptor.java:81-91. B624 §624.3 does the same for ":52443 has TWO endpoints" — refuted by a full-module grep. In both cases the gap description in RESEARCH-STATE embedded the sweep's phrasing ("Niagara session/RBAC vs TLS-cert-only at the Jetty layer (bypassing session)") as if partially accepted, rather than marking it "(sweep hypothesis — measure)". A future researcher reading the gap could mistake the phrasing for a pre-confirmed assertion.

**WHERE in the kit**: The existing rules cover this through the union of VERIFY BEFORE ACTING + GAP PREMISES ARE HYPOTHESES + RE-MEASURE A DRAMATIC NEGATIVE. What is missing is a NAMED subclass specific to the audit sweep: under PROMPT-LOOP BOOTSTRAP step e (AUDIT-FIRST BACKLOG), after "GAP PREMISES ARE HYPOTHESES", add a sentence explicitly calling out the high-risk subclass: security-bypass and existence surprises from the sweep output must be labeled "(sweep hypothesis — measure first)" in the gap description, never embedded as partial assertions.

**PRIORITY**: MEDIUM — the protocol worked correctly (blocks refuted both hypotheses by measurement), but the gap descriptions leaked the sweep's framing. A junior researcher could miscite the gap description as pre-confirmed.

**Partially covered**: YES — VERIFY BEFORE ACTING + GAP PREMISES ARE HYPOTHESES cover the general case. The proposed delta is a NAMED emphasis / subclass within those existing rules, not a new rule.

---

### DELTA-2 — Name the "consolidation focus" as a recognized focus type

**WHAT**: A focus whose primary work is REMITTANCE (most gaps cite existing blocks) + a small gap-fill + a closing reference-table synthesis block. The deliverable is a consolidated reference document, not new knowledge. This is a distinct focus TYPE separate from "new-territory investigation" or "audit-first backlog building".

**WHY (block/commit evidence)**: The ports focus had 14 remittance rows in RESEARCH-STATE (covering protocol internals, TLS posture, outbound-only ports) before the audit sweep, and 7 investigable gaps — all of which were narrow gap-fills over known subsystems (auth model of a specific port, proof of a type being unwired, etc.). B627 (PO-G8) is a master reference table, the focus deliverable. The pattern: mature corpus → consolidation focus → mostly remittances + a few gap-fills → master synthesis. The kit names "new-territory", "audit-first", and "DESIGN/APPLIED" focus types; a "consolidation focus" is unnamed, making it hard to recognize at bootstrap.

**WHERE in the kit**: METHODOLOGY §16 (multi-focus corpus) and PROMPT-LOOP BOOTSTRAP step b2 (ANGLE declaration). Add a named focus type: "Consolidation focus — the deliverable is a reference table or master synthesis. Most gaps are REMITTANCE (pre-declared before the sweep). The audit sweep targets what is NOT yet consolidated, not what is not yet investigated. The closing block is a synthesis/reference block, not a new evidence block." This guides the angle declaration and sets correct expectations for [INFER]/[CERT] ratios in the synthesis block (expected high).

**PRIORITY**: MEDIUM — the run navigated this correctly by experience, but naming it makes the type replicable without improvisation.

**Partially covered**: YES — PRE-DECLARE REMITTANCES FIRST covers the backlog-discipline half. The proposed delta names the FOCUS TYPE (what it is, what the deliverable is, how to declare it at bootstrap) rather than just the backlog rule.

---

### DELTA-3 — Document the "gap-split" discipline for a gap with both read-only and live halves

**WHAT**: When a single research question has a read-only (static) component AND a live (dynamic) component, proactively split it into TWO gap entries at backlog-seeding time: one investigable statically (closes in the current loop), one deferred as `requires-execution → §19`. Do NOT leave the whole gap as `blocked-on-live` just because one half needs execution.

**WHY (block/commit evidence)**: PO-G7 ("platform daemon auth MODEL from code") and PO-G7w ("platform daemon on-the-wire auth digest — the live frame") demonstrate the split. PO-G7 was investigated as a static code-read (B626, 2 files) and closed [CERT]. PO-G7w was deferred as `requires-execution` from the start. Without the split, the entire "platform daemon auth" question would have been a single `blocked-on-live` gap and the static code model would never have been written. The split captured the recoverable half while being honest about the ceiling of the other.

**WHERE in the kit**: METHODOLOGY §8 (Stopping criterion) classifies gaps as read-only / requires-execution / blocked but does not describe the proactive split as a discipline. Add a brief note under the "Each iteration MUST classify every open gap into THREE buckets" paragraph: "When a gap has BOTH a static-readable component AND a live component, split it at seeding time into two entries: one per the static half (investigable now), one per the live half (requires-execution → §19). A single unsplit gap left as `blocked-on-live` loses the static work permanently."

**PRIORITY**: MEDIUM-HIGH — genuinely new guidance absent from the kit. The discipline was improvised successfully in this run but is not documented.

**Partially covered**: NO — §8 describes the classification buckets but does not name the split discipline.

---

### DELTA-4 — Loop-mechanic friction: deferred+requires-execution gap produces triple-count in state envelope

**WHAT**: A gap whose Priority is `deferred` AND whose Status starts with `requires-execution` AND is listed in the "Blocked gaps" section is counted by `research-sdd-status.sh` in THREE envelope counters simultaneously: `deferred_open`, `requires_execution_open`, and `blocked_open`. The result: the envelope shows all three as 1, though only one gap exists.

**WHY (state-file evidence)**: RESEARCH-STATE-ports.md envelope shows `requires_execution_open: 1`, `blocked_open: 1`, `deferred_open: 1` — all three map to PO-G7w alone. PO-G7w has `Priority: deferred`, `Status: requires-execution → §19`, and appears in the "Blocked gaps" section. A reader parsing the envelope infers three distinct unresolved items; there is one.

**WHERE in the kit**: Not in METHODOLOGY.md or PROMPT-LOOP.md (a scripts-lane concern, primarily in `research-sdd-status.sh`). The documentation could note the overlap: "A gap with `Priority: deferred` + `Status: requires-execution` + membership in the `## Blocked gaps` section will be counted in all three counters; this is correct (one gap satisfies all three conditions) but may appear to inflate the open counts."

**PRIORITY**: LOW — scripts-lane primarily. The envelope is not wrong (PO-G7w genuinely satisfies all three conditions), but documentation of the overlap in the gap-status prose would prevent misreading. Propose as a clarification note in METHODOLOGY §7 (State and memory) or §8, not as a rule change.

**Partially covered**: NO — the overlap is undocumented, but it is a tooling behavior issue more than a methodology gap.

---

## Summary table

| Delta | Kind | Priority | Partially covered? | Where |
|---|---|---|---|---|
| DELTA-1: audit-sweep security/existence surprise as named hypothesis subclass | naming/emphasis | MEDIUM | YES (VERIFY BEFORE ACTING + GAP PREMISES) | PROMPT-LOOP BOOTSTRAP step e, AUDIT-FIRST BACKLOG paragraph |
| DELTA-2: "consolidation focus" as a named focus type | naming/new type | MEDIUM | YES (PRE-DECLARE REMITTANCES covers the backlog half) | METHODOLOGY §16 + PROMPT-LOOP step b2 |
| DELTA-3: gap-split discipline for read-only + live halves | new rule | MEDIUM-HIGH | NO | METHODOLOGY §8, under the "THREE buckets" paragraph |
| DELTA-4: deferred+requires-execution triple-count (scripts-lane note) | clarification | LOW | NO | METHODOLOGY §7/§8 prose note; scripts in `research-sdd-status.sh` |
