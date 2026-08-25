# §18 Retro — focus: framework-drivers — 2026-08-25

<!-- review-status: pending -->

> Self-retrospective (METHODOLOGY §18). Produced by a fresh-context retro agent. Proposes kit
> deltas only; does NOT edit the kit. Evidence references: blocks B496–B506, commits 2026-08-25,
> RESEARCH-STATE-framework-drivers.md.

---

## Run summary

- **Focus:** framework-drivers (bootstrapped from oem-honeywell-tail gap U12)
- **Session:** 2026-08-25, niagara-research corpus
- **Mode:** Orchestrated AUTO — 10 driver blocks B496–B505 + focus synthesis B506 (11 blocks total)
- **Gaps:** 10/10 FD1–FD10, all closed (10 sonnet sweeps + inline synthesis); 2 gaps (FD9, FD10) surfaced BEYOND the seed premise via AUDIT-FIRST
- **Execution pattern:** each iteration = one delegated `sonnet` sub-agent sweep → driver re-verifies ALL load-bearing file:line inline → one block per commit → pushed at focus stop
- **Notable:** 10/10 sweep line numbers were offset-wrong (cumulative offsets from concatenated file context); driver re-grepped every citation. FD8 weather: sweep asserted endpoint decommission → FALSIFY-BEFORE-REPORTING via WebSearch refuted it → DE-ESCALATION.

---

## Proposed kit deltas

| # | Title | Evidence | Priority | Kit file / section |
|---|---|---|---|---|
| D1 | Warn that delegated sweeps over decompiled Java trees produce systematically wrong line numbers — strengthen VERIFY BEFORE ACTING | 10/10 blocks B496–B505: every sweep-provided `file:line` was cumulative-offset-wrong (sweep saw concatenated output; real file lines were different, e.g. `BOpcHttpsSecurityPolicies.java:79` vs real `:20`). Current kit language says "resolve at least the `file:line` citations that support a key claim" — this implies selective verification, but for concatenated-output sweeps ALL citations are wrong. | **HIGH** | `PROMPT-LOOP.md` — VERIFY BEFORE ACTING paragraph (INVESTIGATE section) |
| D2 | Note that delegated decompilation sweeps are high-falsification-priority sources for external/operational status claims | B505 (FD8 weather): sweep asserted "NWS endpoints decommissioned 2023, forecast broken on every live N4.14" — a strong operational conclusion derived purely from hardcoded URL strings in decompiled code. WebSearch (FALSIFY-BEFORE-REPORTING) refuted it. Decompilation cannot observe live endpoint state; sweep-generated operational claims about external services are structurally unreliable. FALSIFY-BEFORE-REPORTING already fires on this, but the kit has no explicit note calling out delegated sweeps as a specifically high-risk source for this failure mode. | MEDIUM | `PROMPT-LOOP.md` — FALSIFY BEFORE REPORTING paragraph (INVESTIGATE section) |

---

## Reinforced observations (already in kit — not new deltas)

| Obs | Kit coverage | Notes |
|---|---|---|
| (c) Gap class counts wrong in audit (64→53, 325→189, 1978→1975, etc.); RE-MEASURE rule fired correctly | BOOTSTRAP e / e2: "GAP NUMBERS ARE ALSO HYPOTHESES — re-derive it from the source before using it." RE-MEASURE rule. | Worked as designed across 3+ iterations. Reinforced. |
| (d) AUDIT-FIRST surfaced 2 modules beyond the seed premise (knxnetIp FD9, abstractMqttDriver FD10) | BOOTSTRAP e: "AUDIT-FIRST BACKLOG … Derive the prioritized backlog from that matrix." + "GAP PREMISES ARE HYPOTHESES." | Reinforced. |
| (e) SDK-bundling axis (3-pole: 1-SDK-AS-IS / no-SDK / N-SDK-shaded) visible only at synthesis B506 | PROMPT-LOOP §8 terminal trigger: synthesis block is a valid terminal artifact that consolidates the focus. No kit gap here; axis discovery at synthesis is expected. | Minor observation; not worth a new rule. |
| (f) Python heredoc state bookkeeping caused 2 transient ordering errors (iteration rows out of order, missed row) | No kit rule prescribes implementation tooling for iteration history; errors were minor and corrected manually. | Tooling note, not a kit-level insight. No delta. |

---

## Delta details

### D1 — Systematic offset accumulation in delegated sweeps over concatenated decompiled output

**Evidence:** Every single delegated `sonnet` sweep in this focus (10/10 blocks B496–B505) returned file:line citations that were offset-wrong. The mechanism: when a sub-agent is given decompiled Java source as a concatenated context (multiple `.java` files piped or pasted together), it produces line numbers relative to the concatenated stream, not relative to the individual file. The file itself has a different and correct line numbering. The driver discovered this consistently — every "re-verify inline" step discarded the sweep's line numbers and re-grepped the actual file.

**Gap in current kit:** VERIFY BEFORE ACTING (PROMPT-LOOP, INVESTIGATE section) says:

> "Before writing a block or correcting a document on that basis: (a) resolve at least the `file:line` citations that support a key claim"

The phrase "at least the … key claim" implies the driver may trust non-key sweep citations. For concatenated-output sweeps, no citation is trustworthy — the offset is cumulative across however many files preceded the one being cited. A driver that takes the sweep's line number for a non-key claim is planting a wrong `[CERT]` citation.

**Proposed addition (under VERIFY BEFORE ACTING, after the "key claim" sentence):**

> SYSTEMATIC-OFFSET CAVEAT: when the delegated sweep received decompiled or concatenated multi-file output as context, ALL line numbers it reports are offset relative to the concatenated stream, not the individual file — a citation of `Foo.java:79` may be line `:20` in the real file. In this case treat ALL sweep-provided file:line as HYPOTHESES and re-grep every load-bearing citation from the actual on-disk file before writing it as `[CERT]`. This is not selective — a concatenated-stream sweep cannot yield a correct line for any file except the first.

**Priority:** HIGH — this failure was 100% consistent across all 10 blocks. A driver that trusts even one sweep-provided line from a concatenated-output sweep will write a wrong `[CERT]` citation.

---

### D2 — Delegated decompilation sweeps over-assert external/operational status

**Evidence:** B505 (FD8 weather). The delegated `sonnet` sweep, reading decompiled code that contained hardcoded URLs for NOAA/NWS XML feeds, concluded "NWS endpoints decommissioned 2023, forecast broken on every live N4.14." This is an operational claim about live external infrastructure. The sweep derived it from what the code pointed at plus whatever training knowledge it had about NWS endpoint changes. The driver applied FALSIFY-BEFORE-REPORTING (WebSearch), found the NDFD XML service had migrated to AWS but remained active, and DE-ESCALATED the finding from "broken by default" to "unverified durability risk."

**Gap in current kit:** FALSIFY-BEFORE-REPORTING already fires on this pattern and worked correctly. However, the kit has no note calling out delegated decompilation sweeps as a structurally unreliable source for external/operational status:

- A decompilation sweep can read what URL strings are hardcoded.
- It cannot probe whether those endpoints are alive.
- It can guess from training knowledge, but that knowledge is static and often outdated.
- Result: sweeps systematically over-assert that external endpoints are broken/decommissioned when those endpoints were deprecated at some point in the sweep's training window, even if they later migrated.

**Proposed addition (at the end of FALSIFY BEFORE REPORTING paragraph or as a callout):**

> HIGH-RISK SOURCE: delegated decompilation sweeps. A sweep can read hardcoded URLs from source code but cannot probe live external services — its operational claim about an external endpoint ("decommissioned", "broken by default", "no longer active") is derived from decompiled strings plus static training knowledge, both of which may be outdated. Treat any sweep-generated operational conclusion about an EXTERNAL service or endpoint as high-falsification-priority before reporting it: cost is typically one WebSearch; the de-escalation from "broken by default" to "unverified risk" is the correct outcome and prevents a wrong operational recommendation from propagating. (Evidence: B505 FD8 weather, 2026-08-25.)

**Priority:** MEDIUM — FALSIFY-BEFORE-REPORTING already covers this; the proposed addition makes the sweep-specific pattern explicit so future drivers don't need to discover it from experience.
