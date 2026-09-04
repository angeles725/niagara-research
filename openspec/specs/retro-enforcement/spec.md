# Retro Enforcement Gate Specification (PR2)

## Purpose

Make the retro/state-update obligation machine-enforceable via a bats-tested
toolbelt script, a retro index, and an opt-in pre-push hook.

## Requirements

### Requirement: Retro Registry Index

`retros/INDEX.md` MUST exist and contain one row per completed retro with
columns: date, module, retro filename, key lesson (one-line summary).

#### Scenario: INDEX row added per retro

- GIVEN a developer completes a new retro file `retros/B761-foo.md`
- WHEN the retro is committed
- THEN retros/INDEX.md MUST include a new row for B761 before the commit lands

### Requirement: Sweep-Build-State Script

`toolbelt/sweep-build-state.sh` MUST scan BUILD-STATE.md for all module
sections, collect those with `retro_pending=Y`, and exit non-zero while listing
the affected module names when any are found. It MUST exit 0 when none are found.
It MUST be authored test-first: `tests/build-retro-sync.bats` MUST exist and
fail before the script exists.

#### Scenario: Sweep exits 1 on pending retros

- GIVEN BUILD-STATE.md has entries for CompPan (retro_pending=Y) and chihuahua
  (retro_pending=N)
- WHEN sweep-build-state.sh runs
- THEN it MUST exit 1 and print "CompPan" as a pending module
- AND "chihuahua" MUST NOT appear in the output

#### Scenario: Sweep exits 0 when all clear

- GIVEN all BUILD-STATE.md entries have retro_pending=N
- WHEN sweep-build-state.sh runs
- THEN it MUST exit 0 with no error output

#### Scenario: Bats tests red before script lands

- GIVEN build-retro-sync.bats is committed but sweep-build-state.sh does not
  yet exist
- WHEN bats tests/build-retro-sync.bats runs
- THEN the tests for sweep behavior MUST fail (red-first TDD gate)

### Requirement: Opt-In Pre-Push Hook

An opt-in pre-push hook MUST fail when a pushed diff contains build-relevant
changes (module src/**.java, module.xml, slots, SPA, or kit files) and does NOT
include BOTH of the following: (a) a BUILD-STATE.md update in the same commit
set, AND (b) either a retro_pending=Y entry with an INDEX row OR a
`Retro: none (trivial: …)` commit trailer.

#### Scenario: Hook fails — build diff without retro

- GIVEN a developer pushes commits touching ColdRoomPan/src/**.java
- WHEN the pre-push hook runs and finds no BUILD-STATE.md change and no retro
  trailer
- THEN the hook MUST exit non-zero with a message naming the missing requirement

#### Scenario: Hook passes — trivial retro trailer present

- GIVEN a push includes a minor kit-file change
- WHEN the commit carries `Retro: none (trivial: corrected CHANGELOG typo)`
- THEN the pre-push hook MUST pass

#### Scenario: Hook passes — retro_pending + INDEX row present

- GIVEN a push changes module src and BUILD-STATE.md is updated with
  retro_pending=Y and INDEX.md gains a matching row
- WHEN the pre-push hook runs
- THEN it MUST pass

### Requirement: Existing Bats Baseline Stays Green

The 60-bats baseline (ng-deploy 33, verify-module 9, build-sh 7, mirror 5,
kit-links 3, stored-repack 3) MUST remain green after all PR2 changes land.

#### Scenario: No regression in existing suite

- GIVEN the 60 existing bats tests
- WHEN bats runs on the PR2 branch
- THEN all 60 MUST pass with exit 0
