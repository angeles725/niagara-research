# HOA / output precedence seam — C9 fixture (§19 build-PoC)

Pure-Java resistance-heater command precedence: OFF > sequence > HAND > AUTO (B805 §805.11).
- `src/com/angeles/kit/hoa/HoaPrecedence.java` — `resistanceCommand(inDefrost, mode, auto)`.
- `srcTest/.../HoaPrecedenceTest.java` — 6 biting JUnit 4 tests.

Rule: an operator OFF is a LOCKOUT that dominates EVERY automation incl. a sequence that owns the output
(defrost); the sequence beats HAND/AUTO; HAND forces on outside a sequence; AUTO passes the computed value.
Maps to Niagara's priority array (BBooleanWritable emergency level 1 / manual level 8 above automation 9-16,
B810 §810.4) — this plain-mode seam emulates priority 1-2 for OFF and checks OFF on the sequence path too.

Run (JDK 8 + JUnitCore): build-n4-module-kit/toolbelt/run-pure-test.sh <this-dir> com.angeles.kit.hoa.HoaPrecedenceTest
Result: OK (6 tests). Mutation-proven: swapping the OFF/defrost order (check inDefrost first — the live
ColdRoomPan v2.0.5 bug: `if (inDefrost) return` bypasses HOA) fails T2 (offDominatesDefrost). Fixed on
fix/resistance-off-lockout (v2.0.6).
