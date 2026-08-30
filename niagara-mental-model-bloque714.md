# B714 — Module dev workflow, the dev loop (WF4): the edit → slotomatic → build → sign → deploy → verify loop, step by step

> Focus: **module-dev-workflow** · Gap **WF4** (the end-to-end dev loop as a runbook). Block TYPE =
> **DESIGN/RUNBOOK** (assembles the verified build steps into the ordered loop; high [INFER] ratio expected).
> Feeds `docs/module-dev-workflow.md` §4. Marker `[CERT]` where re-citing verified steps; `[INFER]` for ordering.

## 714.1 — The loop

[CERT+INFER] The ordered dev loop for one change (steps cite the block that established them):

1. **EDIT** — change the `.java` (component logic, or a `@Niagara*` annotation). If you add a NEW component type,
   also add its `<type name= class=>` to `module-include.xml` ([Block 631]/WF2). Your code goes OUTSIDE the
   `/*+ BEGIN BAJA AUTO GENERATED +*/` markers.
2. **SLOTOMATIC (conditionally)** — if a `@Niagara*` annotation changed, run `./gradlew :<part>:slotomatic` to
   regenerate the AUTO region + hash. If NO annotation changed, skip it. A stale AUTO region = compile errors /
   missing slots ([Block 637]/[Block 650]). This is the mode A (skip) vs mode B (run) decision ([Block 709]).
3. **BUILD** — `./gradlew :<part>:build` (or the whole module). The `niagara-module` plugin compiles + assembles
   the jar with `META-INF/module.xml`; the `niagara-signing` plugin auto-signs it from
   `niagara_user_home/security/keystore.jceks` ([Block 639]). The jar emerges with `META-INF/*.SF/.RSA`.
4. **DEPLOY** — copy the signed jar(s) to the station's `modules/` (`STATION_MODULES_DIR`), then restart the
   station (or the module) so the registry rebuilds and the new types load. The shop wraps this in
   `ng-deploy.sh`: `backup → gradlew (mode A/B/C) → copy → verify types vs EXPECTED_*_TYPES` (phase exit codes
   10/20/30/40/50) ([Block 637] §637.4).
5. **VERIFY** — confirm the emitted types match what you expect (the deploy script does this automatically). A
   class dropped from `module-include.xml` shows up here as a missing type, not as a build error.

## 714.2 — The commands (shop pattern)

[CERT+INFER]
```
# 1. edit .java (+ module-include.xml if a new type)
# 2. only if a @Niagara* changed:
./gradlew :<part>:slotomatic
# 3. build + auto-sign:
./gradlew :<part>:build            # or the ng-deploy wrapper, which chooses the mode
# 4-5. deploy + verify (shop wrapper):
./scripts/ng-deploy.sh             # backup -> gradlew(mode) -> copy -> verify types
```
The WSL/NTFS reality: because the signing store is on Windows, the shop bridges with **Robocopy WSL→Win→WSL**
for slotomatic + jar ([Block 639]).

## 714.3 — Where each step fails (and how you know)

[INFER]

| Step | Common failure | Symptom | Fix |
|---|---|---|---|
| slotomatic skipped | stale AUTO region | compile error / missing slot at runtime | run `:slotomatic` |
| new type not in `<type>` | class not registered | `BTypeSpec.resolve` fails; no error at build | add `<type>` |
| signing | wrong/absent key | jar unsigned or wrong signer | check `niagara_user_home/security/keystore.jceks` alias |
| deploy | station didn't restart | old type still loaded | restart station/module |
| verify | emitted types ≠ expected | ng-deploy phase exit 40/50 | reconcile `module-include.xml` / EXPECTED_*_TYPES |
| profile | logic in `-wb` | invisible on headless station | move to `-rt` ([Block 630]) |

## 714.4 — The golden rules of the loop

[INFER]
1. Slotomatic ONLY on annotation change; never hand-edit the AUTO region.
2. New type ⇒ update `module-include.xml` in the same edit.
3. The jar is signed automatically — don't add a manual sign step; ensure the right keystore/alias is active.
4. Always deploy through backup+verify (ng-deploy pattern), not a bare copy — the type-verify catches silent
   registration failures.
5. Build against the SDK home matching the target station version ([Block 638]).

## Connections

- Slotomatic/build/sign/deploy steps → focus `own-modules-audit` [Block 637]–[Block 639], `module-best-practices`
  [Block 709]. Codegen → [Block 712] (WF2); artifacts → [Block 713] (WF3); toolchain → [Block 711] (WF1).
  Deliverable: `docs/module-dev-workflow.md` §4.

## Self-verify

| # | Claim | Marker | Citation | Verified |
|---|---|---|---|---|
| 1 | loop order: edit→slotomatic(cond)→build+sign→deploy→verify | [CERT]+[INFER] | [Block 637]/[Block 639]/[Block 709] | assembled |
| 2 | slotomatic conditional on @Niagara* change | [CERT] | [Block 637]/[Block 650] | cited |
| 3 | jar auto-signed from user-home keystore | [CERT] | [Block 639] | cited |
| 4 | ng-deploy = backup→gradlew→copy→verify + WSL bridge | [CERT] | [Block 637]/[Block 639] | cited |
| 5 | failure/fix table + golden rules | [INFER] | 714.3/714.4 | reasoned |

**Tally:** [CERT] ×3 · [INFER] ×2. Block TYPE = **DESIGN/RUNBOOK** — ratio healthy. Re-cites verified blocks.

## Open gaps (this focus)

WF4 CLOSED. Next: **WF5** (testing + debugging — run-tests-wsl.sh pure-Java model testing, station debug, common
errors; + finalize `docs/module-dev-workflow.md`) — focus-closing block.
