# Protection-latch seam — C9 PR1 fixture (§19 build-PoC)

Pure-Java SET-DOMINANT SR latch with first-out capture (B805 §805.3 gap: kitControl ships no SR/safety latch).
- `src/com/angeles/kit/safety/ProtectionLatch.java` — the seam (no Baja deps).
- `srcTest/com/angeles/kit/safety/ProtectionLatchTest.java` — 8 biting JUnit 4 tests.

Run (JDK 8 + JUnitCore, the kit runner):
    build-n4-module-kit/toolbelt/run-pure-test.sh <this-dir> com.angeles.kit.safety.ProtectionLatchTest

Result: OK (8 tests). Mutation-proven: dropping the first-out guard fails T4 (firstOutIsNotOverwritten).
Contract: set-dominant · first-out captured once on the CLEAR->TRIPPED edge · explicit reset only, and only
with the trip condition clear · no re-trip chatter. A Bxxx wrapper drives step() from execute()/changed(),
exposes a reset ACTION (optionally B803 step-up-gated), and wires first-out to a BAlarmSourceExt.
