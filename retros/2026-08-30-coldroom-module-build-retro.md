<!-- review-status: applied 2026-09-05 · kit dd552ea · shipped: #1 (§16 APPLIED/BUILD-ALONG focus), #2 + #5 (§18 triggers, PR #430), #3 (PROMPT-LOOP, PR #445); DEFERRED: #4 (§3 [CERT-live] vs [CERT-hw] boundary) -->
<!-- Marker lifecycle: the maintainer flips 'pending' above to 'applied <date> · kit <sha>' once this retro's proposed deltas are reviewed and applied (or 'dismissed') in the kit; sweep-retros.sh reads this marker to report which retros are still open (METHODOLOGY §18). -->
<!-- kit-retro -->
<!--
  target: niagara-research
  focus: module-dev-workflow (post-close addenda B721–B723)
  blocks: B721, B722, B723 (3 addendum blocks on an already-STOPPED focus)
  date: 2026-08-30
  review-status: pending
  propose-never-apply: true
-->

# Retro — niagara-research · module-dev-workflow (applied build, addenda B721–B723) · 2026-08-30 · Research-SDD self-retrospective

> Run reviewed: the **applied ColdRoomPan module build** session — a real N4 module built, signed, and deployed
> by the operator in live tools, then documented, producing addenda B721–B723 to the already-closed
> `module-dev-workflow` focus plus a set of `docs/` guides and LaTeX manuals. Trigger: focus-completion /
> post-close-addendum (deferred from the session summary's "Next Steps: §18 retro for the B721-723 addenda").
> Method: a FRESH-CONTEXT agent read the current kit (`PROMPT-LOOP.md` + `METHODOLOGY.md`) FIRST, then the run's
> blocks, RESEARCH-STATE, and engram summary, and proposes kit deltas. READ-ONLY on the kit — this report only
> PROPOSES; kit changes are human-reviewed and human-committed (METHODOLOGY §18, propose-never-apply).

## What this run was (and why it does not fit the normal focus shape)

This was NOT a discovery focus and NOT a pure synthesis-guide focus. It was an **applied build-along**: the
operator drove a real deliverable in live external tools — a `ColdRoomPan` cold-room controller module, built
against Niagara 4.15.3.28 in WSL2, signed with the operator's own `luissigner` cert in the Workbench GUI, and
deployed onto a live JACE (192.168.1.140) that was made to trust the operator's `luisCA`. The research loop ran
*alongside* that work: it captured the live-verified procedure as it happened and produced the operator-facing
artifacts (`docs/how-to-create-coldroom-module.md`, `docs/cold-room-module-design.md`,
`docs/commissioning-cuartos-frios.md`, and the EN/ES `docs/manuals/new-module-creation/` and
`docs/manuals/commissioning-cuartos-frios/` LaTeX/PDF manuals).

Three corpus addenda came out of operator questions raised *while building*, and all three landed on a focus
that had **already reached its 5/5 STOP**:

- **B721** `module-permissions.xml` — the focus's file table had only *catalogued* it; an operator question
  ("is it how the module looks, or what has permissions?") forced it open. Java Security Manager permission-
  request manifest.
- **B722** the real WSL2 build loop from the `chihuahua` project's living docs; carries a **§14 clarification**
  of a corpus imprecision (slotomatic runs in WSL; the Robocopy bridge is a signing-store concern of one
  wrapper, not a slotomatic requirement).
- **B723** the self-signed code-signing chain + JACE trust, **[CERT-live]** operator-verified in the Workbench
  GUI this session, plus a cross-version-compatibility finding (dep pinned at minor `4.15` → runs on any
  4.15.x; built .28, ran on .20 and .28).

## Proposed kit deltas

> Only genuinely NEW items — anything the kit already encodes is under "Already covered", not here.

| # | Proposed change | Target (file · §/section) | Evidence | Type | Priority |
|---|---|---|---|---|---|
| 1 | Name an **APPLIED / BUILD-ALONG session** run mode: the operator executes in a live external tool and the loop's job flips from *probe unknowns* to *capture the live-verified procedure + emit operator-facing deliverables* | `METHODOLOGY.md §16` (run modes) + `PROMPT-LOOP.md` step 1 (CHOOSE) | Whole session; B723 `[CERT-live]` GUI walkthrough; B722 real chihuahua build; deliverables `docs/how-to-*`, `docs/manuals/*` | new | MED |
| 2 | Name a **POST-CLOSE ADDENDUM** lane: a standalone block that closes a *catalogued-but-unexplained* item on a STOPPED focus, cited from that focus's RESEARCH-STATE, that does **not** re-arm the STOP or open a budget — lighter than §8's "Reopening a STOPPED loop" | `METHODOLOGY.md §8`, immediately after "Reopening a STOPPED loop for a bounded experiment" | B721/B722/B723 headers ("ADDENDUM block … focus closed 5/5"); RESEARCH-STATE-module-dev-workflow.md Coverage ("Addendum B72x (post-close) … Does not change the 5/5 STOP") | new | MED |
| 3 | For **operator-facing manual deliverables**: anchor to the operator's EXISTING mental model before introducing a new abstraction, and do not switch between competing models mid-explanation | `PROMPT-LOOP.md` (deliverable-authoring note, near the SYNTHESIS-GUIDE deliverable guidance) | Session summary: operator "found the custom-Java-module approach HARD to follow … use their existing wire-sheet mental model"; explanation flip-flopped between Model A (containment) and Model B (visible wires) | new | MED |
| 4 | Refine the **`[CERT-live]` vs `[CERT-hw]` boundary** to explicitly place "operator verifies a procedure by executing it in a LOCAL, administratively-owned GUI tool" on the `[CERT-hw]` side (owned-local), not `[CERT-live]` (remote/unowned) | `METHODOLOGY.md §12b/§12c` (`~L1549`) | B723 tags the operator's LOCAL Workbench GUI + local JACE walkthrough `[CERT-live]`, but the CERT taxonomy reserves `[CERT-live]` for remote services you do not own and assigns owned devices to `[CERT-hw]` | refinement | LOW |
| 5 | Extend the **§18 retro TRIGGER** so it also fires at the natural close of an APPLIED / build-along session and whenever a session produces POST-CLOSE ADDENDA — not only on a focus STOP | `METHODOLOGY.md §18` (trigger conditions) | This very retro nearly did not happen: no focus STOP fired (applied session + addenda on an already-STOPPED focus), so the §18 trigger did not fire; the retro ran only because the operator explicitly asked for it | new | MED |

Rationale (WHY it matters · cost · impact):

- **#1 (MED)** — The kit's run-mode vocabulary is discovery-shaped (probe an unknown, cite `[CERT]`, close a
  gap). This session had almost no unknowns to probe — the operator already knew the destination; the loop's
  value was *documenting the path as it was walked live* and *shaping deliverables for a specific operator*.
  Without a named mode, a future agent may mis-model an applied session as a discovery focus and burn effort
  "investigating" a procedure the operator is executing in front of it, or fail to pre-declare the deliverable
  shape. Cost: a short §16 sub-section. Impact: correct expectations (high `[CERT-live]`/`[CERT-hw]` ratio,
  deliverable-first, gaps arrive from the operator's live questions, not from a backlog sweep).

- **#2 (MED)** — All three addenda landed on a focus at STOP without going through §8's reopen (no additive
  budget, no re-arm). The kit today offers only two lanes: heavy "Reopening a STOPPED loop … its OWN fresh
  budget cap" (for NEW work) and "Live backlog injection" (while a focus is still RUNNING). Neither fits "the
  focus is closed, but a question surfaced about something it only *catalogued* — write a standalone addendum,
  cite it from the closed RESEARCH-STATE, and leave the STOP count untouched." The run improvised this cleanly
  (each addendum block self-labels "post-close … does not change the 5/5 STOP"), which is exactly the signal
  §18 says to codify. Cost: one paragraph in §8. Impact: future agents get a legitimate lightweight lane
  instead of either forcing a full reopen (over-ceremony) or silently editing a closed focus (audit loss).

- **#3 (MED)** — This is the run's most honest friction. The deliverables are operator-facing manuals, so how
  the loop *explains* is part of the deliverable, not a courtesy. The operator repeatedly could not follow the
  custom-Java-module abstraction (invisible containment, block-to-block relations not shown as wires) because
  it did not map onto their wire-sheet mental model, and the explanation made it worse by oscillating between a
  containment model and a visible-wire model mid-thread. Cost: a short authoring note. Impact: manual
  deliverables that start from the reader's existing model and commit to ONE model per explanation — the
  cheapest, highest-leverage fix for the single biggest source of confusion this session.

- **#4 (LOW)** — A marker-hygiene edge, not a ranking change. The live-wins ordering is already correct in the
  kit; the only gap is that an *applied* session naturally verifies against a LOCAL owned tool (Workbench GUI,
  a JACE the operator administers), and the current wording does not explicitly say that case is `[CERT-hw]`,
  so B723 reached for `[CERT-live]`. Low cost, low blast radius; worth one clarifying clause so the next
  applied session tags owned-local verification consistently.

- **#5 (MED)** — The retro almost did not happen at all. §18's trigger is written around a focus STOP; an applied
  build-along session and post-close addenda have no STOP, so the trigger silently does not fire and the retro
  drops off the "done" checklist. Here it was flagged "pending" in the session summary but was executed only when
  the operator explicitly asked ("¿realizaste las retros?") — an honest miss: the natural close point should have
  prompted it without the operator having to. The completeness of the retro discipline itself is at risk exactly
  in the session shapes Deltas #1 and #2 name. Fix: make §18 also fire at the natural close of an applied session
  and for any post-close addenda, so the retro no longer depends on the operator remembering to ask.

## Already covered (dedupe — proof the retro read the kit first)

- **Reopening a STOPPED focus for NEW work with its own additive budget** → already covered, `METHODOLOGY §8`
  ("Reopening a STOPPED loop for a bounded experiment", L769–785). Delta #2 is deliberately a *lighter* sibling
  of this rule, not a duplicate — it names the no-budget, no-re-arm addendum case that §8 does not.
- **Live backlog injection while a focus is RUNNING** → already covered, `METHODOLOGY §8` (L785). Not this run:
  the focus was already at STOP when the addenda arrived, which is precisely why #2 is needed.
- **Delegating a writer for 2+ non-trivial documents** → already covered by the orchestrator Delegation Rules
  (write rule: delegate one writer for 2+ non-trivial files). The big docs/manuals were produced with delegated
  writers exactly as prescribed — NOT re-proposed.
- **§14 cross-block correction of a corpus imprecision** → already covered, `METHODOLOGY §14`. B722's slotomatic
  clarification (empirical `BUILD_WORKFLOW.md` beats the `[B639]`-cited runbook framing; protocol
  empirical > doc) was executed correctly under the existing rule — NOT re-proposed.
- **`[CERT-live]`/`[CERT-hw]` outrank `[CERT]`/`[CERT-doc]` (the live system wins)** → already covered,
  `METHODOLOGY §3` (L74–84). Delta #4 refines only the owned-local *boundary*, not the ranking.
- **SYNTHESIS-GUIDE FOCUS pattern + focus pair (rules-guide vs process-runbook over shared evidence)** →
  already covered by this session's sibling retros (`module-best-practices`, `module-dev-workflow`, 2026-08-30).
  Delta #1 is orthogonal: applied build-along is not distillation of existing corpus, it is capture of a live
  procedure being executed now.

## Anti-patterns observed

- Explanation oscillated between two competing mental models (containment vs visible wires) mid-thread, which
  compounded the operator's confusion → the delta that prevents it: **#3**.
- Three addenda mutated a STOPPED focus's coverage record with an improvised, unnamed convention (correct in
  practice, but relying on ad-hoc "post-close" labels rather than a kit-sanctioned lane) → **#2**.

## Tools built, adapted, or outgrown

| # | CREATED (path · purpose) | ADAPTED | OUTGREW | ORACLE | VERDICT |
|---|---|---|---|---|---|
| T1 | `docs/manuals/*/main.tex` (+`main-es.tex`) · pandoc/pdflatex LaTeX pipeline for the EN/ES commissioning & module-creation manuals | — | — | — | `keep-local` · target-specific doc build; the only reusable lesson is the LaTeX-hygiene note below, which is content, not a kit tool |
| T2 | — | — | — | — | `no` · the ASCII-fy step (box-drawing/≥/arrows → ASCII; `underscore` package for `_`) is a one-off pdflatex workaround, superseded once captured as a doc note; no reuse value as a kit tool |

No ORACLE was built or needed — this run's correctness was established by the operator executing the procedure
live (`[CERT-live]`/`[CERT-hw]`), not by any tool that re-observes a result.

## Tools used (session toolchain)

The tools this session ran **on** (distinct from the "Tools built/adapted/outgrown" table above, which is about
tools the session *created*):

**Module build, sign & deploy — operator's Windows/WSL2 environment:**
- **Niagara Workbench** — New Module wizard; palette; property sheets; Certificate Management (User Key Store /
  User Trust Store); Certificate Signer Tool; Jar Signer Tool; Platform (importing the CA into a JACE's trust store).
- **IntelliJ IDEA** — open project; Project Structure (JDK 1.8 / language level 8); editing `gradle.properties`.
- **gradle-niagara** (`com.tridium.*` plugins, 7.6.22 for 4.15 / 7.6.17 for 4.14) via `gradlew`: tasks
  `clean` / `slotomatic` / `jar` / `compileModuleTestJava`, with `-Pniagara_home` + `-Porg.gradle.java.installations.paths` overrides.
- **Slot-o-Matic** (`slotomatic` task) — AUTO-region codegen from `@Niagara*` annotations.
- **WSL2 (Ubuntu)** — the build host; **JDK 8** (`/usr/lib/jvm/java-8-openjdk-amd64`, openjdk 1.8.0_502).
- **keytool / Certificate Management GUI** — self-signed CA + code-signing chain (`luisCA` / `luissigner`).

**Corpus & documentation side — this repo:**
- **module-navigator** (`module_nav.py`) and **niagara-help** (`niagara_help.py`) — grounding facts in the corpus.
- **Delegated writer agents** (Explore / general-purpose) — the large docs, the manuals, and this retro.
- **Engram** (`mem_save` / `mem_search` / `mem_session_summary`) — cross-session persistence.
- **pandoc + pdflatex (texlive)** — Markdown → LaTeX/PDF for the manuals (with the ASCII-fy step for
  box-drawing/≥/arrows and the `underscore` package to keep `_` literal under pdflatex).
- **`tools/gen-catalog.py`** — CATALOG regeneration; **git** — commits pushed to `origin/main`.

## Metrics

- **Blocks reviewed**: 3 (B721, B722, B723) · **§14 cross-block corrections in this run**: 1 (B722 slotomatic
  vs Robocopy bridge) · **Rules skipped in practice**: 1 (the three addenda bypassed §8's reopen ceremony — the
  gap Delta #2 names, not a violation)
- **Deltas proposed (new)**: 5 (4 MED, 1 LOW) · **Already-covered lessons**: 6

## Honest verdict

This run genuinely surfaced new signal, because it did not fit the kit's discovery/synthesis shapes: it was an
operator-driven **applied build** whose deliverables were operator-facing manuals, and whose corpus additions
were **post-close addenda** to a STOPPED focus. Those two shapes (Delta #1, Delta #2) are the substantive
proposals — the kit currently has no vocabulary for either. Delta #3 codifies the run's biggest real friction
(mental-model mismatch + mid-explanation model flip-flop), and Delta #4 is a small `[CERT]`-marker boundary
cleanup. The mechanics of module development themselves needed almost no new research — the corpus already
covered them, which is why every gap here arrived from a live operator question rather than a backlog sweep, and
why the honest story of this session is about *run mode and communication*, not about discovering new facts.
