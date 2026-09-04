# QA Test Gate Specification (PR2)

## Purpose

Mandate a bite-and-fast test discipline for all N4 module safety/control logic
via a toolbelt script and a METHODOLOGY.md rule, without introducing a mutation
framework.

## Requirements

### Requirement: Pure-Class Extraction Before BComponent

The kit MUST mandate that any safety or control decision (comparators, timer
arms, limit defaults) MUST reside in a ZERO-Baja pure Java class. Inline
safety/timing logic inside a BComponent MUST NOT be accepted as compliant.
This rule MUST be documented in METHODOLOGY.md as a mandatory gate.

#### Scenario: BComponent with inline safety logic fails gate

- GIVEN a BComponent contains a direct comparator or timer-arm call inline
- WHEN the QA gate is evaluated
- THEN the gate MUST mark the submission non-compliant with an explicit note

#### Scenario: Pure-class extraction satisfies gate

- GIVEN the safety logic is extracted into a plain Java class with no Niagara
  dependencies and covered by a JUnit test
- WHEN the QA gate is evaluated
- THEN the gate MUST pass

### Requirement: Fast Test Suite (<1 s per suite)

Each pure-class test suite MUST compile and run in under approximately one
second using standalone javac + JUnitCore. Suites exceeding this threshold
violate the fast-test rule.

#### Scenario: Suite completes within time budget

- GIVEN a pure-class JUnit test suite with 10 tests
- WHEN run-pure-test.sh executes javac + JUnitCore
- THEN elapsed time MUST be less than 1 second and the script MUST exit 0 on
  all-pass

### Requirement: Mutation-Sensitive Boundary Assertion

Each safety decision MUST be covered by at least one mutation-sensitive
assertion — defined as an assertion that fails when the comparator or threshold
it checks is inverted. A test suite that would survive an inverted comparator
FAILS this gate.

#### Scenario: Suite fails with zero bite assertions

- GIVEN a test suite has only presence-check assertions (assertEquals on
  constants, not boundary conditions)
- WHEN run-pure-test.sh applies the inverted-comparator probe
- THEN the script MUST exit non-zero and report that no mutation-sensitive
  assertion was found

#### Scenario: Suite passes with ≥1 bite assertion

- GIVEN a test asserts `assertTrue(controller.isSafe(threshold))` and
  `assertFalse(controller.isSafe(threshold + 1))`
- WHEN run-pure-test.sh runs
- THEN it MUST exit 0 and report the suite as bite-passing

### Requirement: run-pure-test.sh Toolbelt Script

`toolbelt/run-pure-test.sh` MUST implement the pure-class compile-and-run gate.
It MUST be authored test-first (biting bats test RED before the script exists).
It MUST be wired as a documented step in METHODOLOGY.md.

#### Scenario: Script absent — bats tests fail first

- GIVEN run-pure-test.sh does not yet exist
- WHEN bats tests covering its behavior run
- THEN those tests MUST fail (TDD red-first requirement)

### Requirement: Pre-Commit Hook Covers Pure Tests

The existing pre-commit hook (`bats && shellcheck`) MUST continue to gate
commits. No change to the pre-commit hook invocation is needed; run-pure-test.sh
is documented as a manual gate step and SHOULD be wired into the BUILD-LOOP.

## Non-Goals for This Spec

- PITest or any automated mutation framework is out of scope.
- The 67 existing pure JUnit tests in the modules are not modified by this
  campaign; they are baselines confirming the pattern already exists.
