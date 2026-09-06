# Client issue draft — repo hygiene (tracked build artifacts) + a lint-timers false positive

Author: companero (Fable), 2026-09-06. For the lead to hand to Cristian to file on `niagara-panccadia-leon` when he says.
Two independent parts; both verified read-only at the a109249 worktree (`Cliente/Leon-Guanjuato-worktrees/main-a109249`).
**Part (b)'s framing changed under verification: the lint FAIL is a FALSE POSITIVE, not a client defect — the fix is a KIT
lint refinement, not client code.** `[ev: git ls-files @ a109249]` `[ev: lint-timers.sh @ kit main]` `[ev: git blame BDefrostController.java:718]`

---
## Part A — tracked gradle build artifacts (repo hygiene) — REAL, client-side fix
`git ls-files` at a109249 tracks 44 files under `**/build/`: **classes 28 · tmp 8 · libs 4 · manifest 4**.
- `**/build/classes/` (28 `.class`) and `**/build/tmp/` (8, incl. `previous-compilation-data.bin` and `jar/MANIFEST.MF`) are
  gradle's incremental-compile CACHE — they churn on EVERY build, so PR6/PR8 had to commit them alongside real changes
  (noise in every diff, false conflicts).
- `**/build/libs/*.jar` (4) and `**/build/manifest/writeModuleXml/module.xml` (4) are the DEPLOY convention — the station
  RARs consume these committed jars + module.xml. **KEEP them tracked.**
There are NO build rules in `.gitignore` today.

### Fix (exact)
Add to `.gitignore` (root):
```gitignore
# Gradle incremental-compile cache — churns every build; NOT the deploy artifacts
**/build/tmp/
**/build/classes/
```
One-time untrack (keeps the working-tree files, stops tracking them):
```bash
cd <client-root>
git rm -r --cached $(git ls-files '**/build/tmp' '**/build/classes')
git commit -m "chore: stop tracking gradle build cache (build/tmp, build/classes); keep libs+manifest deploy artifacts"
```
Leaves `build/libs/*.jar` + `build/manifest/**/module.xml` tracked (unchanged deploy flow).
### Acceptance
- `git ls-files | grep -E '/build/(tmp|classes)/'` → empty; `… /build/(libs|manifest)/` → still 4 + 4.
- A clean rebuild produces no `git status` churn under `build/tmp` or `build/classes`.
### Impact
Low risk, high signal: every future PR diff stops carrying `.class`/`.bin` noise; no deploy change.

---
## Part B — `lint-timers.sh companion-flag` FAIL on `BDefrostController.java` — a FALSE POSITIVE (kit fix, not client)
`lint-timers.sh` on `Paccadia/ColdRoomPan/ColdRoomPan-rt/src` exits **1** with:
```
FAIL  companion-flag  …/BDefrostController.java: flag 'anyNoHardware' set beside Clock.schedule* not cleared in stopped()/started()
```
**But this is a false positive on two counts (verified):**
1. `anyNoHardware` is a **method-LOCAL**, not a field: `boolean anyNoHardware = false;` at **:718**, inside
   `private void requestDefrostCycle()` (**:713**); set `true` at :726, read at :740. Its ONLY declaration is that local
   (`grep '^\s*(private\|boolean) .*anyNoHardware'` → just :718). A per-call local resets to `false` on every call — it
   CANNOT leak across a lifecycle, so there is nothing to "clear in stopped()/started()". The companion-flag rule targets a
   class-level FIELD flag guarding a live timer (kit `lint-timers.sh` header: "A boolean/int flag assigned true beside a
   Clock.schedule*… not assigned false inside stopped() or started()").
2. `requestDefrostCycle()` (:713-~750) contains **no Clock.schedule** at all (the schedules are in other methods: :808,
   :810, :850). The rule paired the flag with a Clock.schedule across a method boundary.
`anyNoHardware` is a defrost-ELIGIBILITY flag ("some unit has no defrost hardware"), unrelated to any `Clock.Ticket`.
Introduced at **14443c2** (2026-09-05, v2.0.7); **not touched by C9**. The real timer discipline in this file is CORRECT —
`lint-timers.sh` also prints `PASS timer-ticket … cancelled in stopped()` for the same file.

### Fix (KIT, not client) — refine `companion-flag` so it stops false-positiving
The rule must only fire when BOTH hold: (1) the flag is a **class FIELD** (declared at class scope, not a `type name = …;`
inside a method body), and (2) the paired `Clock.schedule*` is in the **same method body** as the flag assignment
(brace-scoped, not file-wide). Either guard alone kills this case. This is a `niagara-tools` kit change (a lint-timers-ext
follow-up), tracked with a bats fixture reproducing the local-flag + cross-method-schedule shape → PASS.
### Client-side (optional)
No client CODE change is warranted — there is no timer-leak defect. If the client wants a clean `report-module.sh` before
the kit lint is fixed, a one-line documented waiver is acceptable, but the correct home for the fix is the kit lint.
### Acceptance
- Kit: after the refinement, `lint-timers.sh` on ColdRoomPan-rt/src at a109249 → exit 0 (companion-flag no longer fires on
  `anyNoHardware`), and a new bats case pins the local-flag/cross-method shape as PASS while a true field-flag case still FAILs.

## Self-verify
| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | 28 classes / 8 tmp / 4 libs / 4 manifest tracked; no .gitignore build rule | [CERT] | `git ls-files` @ a109249 |
| 2 | libs/manifest are the deploy artifacts | [CERT] | tracked `build/libs/*.jar` + `build/manifest/writeModuleXml/module.xml` |
| 3 | `anyNoHardware` is a method-local, only decl at :718, in requestDefrostCycle :713 | [CERT] | grep/awk @ a109249 |
| 4 | requestDefrostCycle has no Clock.schedule | [CERT] | awk :713-750 (0 hits); schedules at :808/:810/:850 |
| 5 | origin 14443c2 v2.0.7, not C9 | [CERT] | `git blame -L 718` |
| 6 | companion-flag rule targets field flags | [CERT] | lint-timers.sh header lines 14-15 |
