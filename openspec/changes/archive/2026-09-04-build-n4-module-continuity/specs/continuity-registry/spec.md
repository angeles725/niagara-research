# Continuity Registry Specification (PR1)

## Purpose

Establish a single kit-resident session ledger so every build session can orient
from the previous one's leave-off point, and so the retro close gate has a
machine-readable state to check.

## Requirements

### Requirement: Kit-Single Build-State Ledger

The kit MUST maintain `build-n4-module-kit/BUILD-STATE.md` as the sole
authoritative session ledger for all tracked modules. It MUST use the
`<!-- build-state.v1 -->` HTML-comment envelope with the following declared
fields: `module`, `target_version`, `profiles`, `last_build`, `bytecode_major`
(fixed value = 52), `signed`, `verify_gate`, `deployed`, `target_station`,
`open_issues`, `retro_required`, `retro_pending`, `last_commit`, `last_session`.
It MUST contain an index table plus one dedicated section per tracked module.
No per-module-root state file is created.

#### Scenario: Ledger seeded at PR1 merge

- GIVEN PR1 is merged to main
- WHEN BUILD-STATE.md is first committed
- THEN it MUST contain sections for ColdRoomPan, DashboardPan, CompPan, and
  chihuahua with accurate baseline values sourced from known-verified reality
- AND one section MUST be annotated as the canonical worked example

#### Scenario: Ledger missing — no enforcement yet

- GIVEN BUILD-STATE.md does not exist (pre-PR1 state)
- WHEN any build session runs
- THEN PR1 tooling MUST NOT block the session (enforcement is PR2's concern)

### Requirement: Orient Step (BUILD-LOOP §0.a)

BUILD-LOOP MUST include a new §0.a "Orient from BUILD-STATE" step that reads
BUILD-STATE.md and prints a one-line leave-off summary before any build work
begins.

Leave-off format: `<module> · <last_build> / <verify_gate> / <deployed> · next:
<next_target> · open_issues=<N> · retro_pending=<Y|N>`

Meta-work sessions (docs-only edits, SDD artifacts, kit admin not touching
module src) are exempt from the orient print.

#### Scenario: Orient prints leave-off for known module

- GIVEN BUILD-STATE.md contains a section for module "DashboardPan"
- WHEN a new DashboardPan build session begins
- THEN the orient step MUST print the one-line summary before step 1 runs
- AND the output MUST include the retro_pending value

#### Scenario: Orient skipped for meta-work

- GIVEN the session purpose is SDD artifact authoring (no module src change)
- WHEN the session begins
- THEN the orient step MAY be skipped without violating the gate

### Requirement: Hard Close Gate at BUILD-LOOP §7

BUILD-LOOP step 7 MUST be reworded as a HARD close gate. A build session MUST
NOT be marked closed unless BUILD-STATE.md is updated in the same session AND
one of the following is true: (a) `retro_pending=Y` is set and an INDEX row is
added, OR (b) the closing commit carries a `Retro: none (trivial: …)` trailer.

#### Scenario: Gate blocks when state not updated

- GIVEN a build session changed module src/**.java
- WHEN the developer attempts step 7 without editing BUILD-STATE.md
- THEN the gate MUST surface a blocking message and refuse to mark the session
  closed

#### Scenario: Gate passes with trivial-retro trailer

- GIVEN a build session made a minor cosmetic slot rename
- WHEN the commit includes `Retro: none (trivial: slot rename only)`
- THEN the close gate MUST pass

### Requirement: SKILL.md References BUILD-STATE at Session Start

SKILL.md execution-steps MUST include an explicit reference to BUILD-STATE.md
as the first orientation action in every non-meta-work session.

#### Scenario: SKILL.md updated

- GIVEN the PR1 diff
- WHEN SKILL.md is read
- THEN BUILD-STATE.md orientation MUST appear before any build or verify step
