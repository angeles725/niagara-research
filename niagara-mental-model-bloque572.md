# Block 572 — `BProvisioningRobot`: the provisioning escape hatch — arbitrary Program code run on every subordinate via `BProgramService.runRobot(code)` — gated by a `getPermissions` mask that strips non-super-users to READ-ONLY, with fleet risk bounded by each station's program-signing posture

**Session**: 2026-08-28
**Focus**: `provisioning` (gap PV6 — the robot step, the arbitrary-code escape hatch, and its permission masking)
**Distribution**: Honeywell OptimizerSupervisor-N4.14.0.162
**Method**: READ-ONLY, inline bounded read of `BProvisioningRobot` + `BRobotJobStep`; the permission mask and the
program-execution call token-verified inline.
**Primary sources** `[CERT]`:
- `organized/provisioningNiagara/provisioningNiagara-wb/vineflower/com/tridium/provisioningNiagara/program/
  {BProvisioningRobot,BRobotJobStep}.java`.

**Scope**: the one provisioning step that runs arbitrary code, and how it is gated. Connects to the `program`
module runtime/sandbox ([Block 541]) and the hardening posture ([Block 398]) — does NOT re-open either.

---

## 572.1 A robot is a Program run on each subordinate [CERT]

`BRobotJobStep extends BDeviceJobStep implements BIEncodable` `[CERT] :42` carries a `robotOrd` (→ a
`BProvisioningRobot`) `[CERT] :37-52`. Its `doRun` `[CERT] :73-107` opens a session to the target device, gets
that station's program service, and runs the robot:
```java
BProgramService programService = (BProgramService) BOrd.make("service:program:ProgramService").get(session);
BRobotResult result = programService.runRobot(code);
```
So a "robot" is **arbitrary Program code** ([Block 541]) executed on EACH target subordinate via
`BProgramService.runRobot(code)`. It is the provisioning **escape hatch**: when the built-in steps (install,
backup, rename, license…) are not enough, a robot runs custom logic across the fleet — one program, executed on
N stations by the batch engine ([Block 567]).

## 572.2 The permission mask: non-super-users see READ-ONLY [CERT]

`BProvisioningRobot extends BComponent` `[CERT] :32` overrides `getPermissions(Context)` `[CERT] :67-82`:
```java
BPermissions permissions = super.getPermissions(cx);
if (cx != null && cx.getUser() != null && !cx.getUser().getPermissions().isSuperUser()) {
   int mask = 0;
   if (permissions.hasOperatorRead()) mask |= 1;    // keep operator READ
   if (permissions.hasAdminRead())    mask |= 16;   // keep admin READ
   permissions = BPermissions.make(mask);           // everything else stripped
}
return permissions;
```
For any **non-super-user**, the robot's effective permissions are masked down to at most `operatorRead |
adminRead` — **all write/invoke/admin-write/admin-invoke bits are stripped**. Only a **super-user** retains full
permissions. So a non-super-user can at most SEE that a robot exists; only a super-user can author, modify, or
invoke one. This is a deliberate hardening of the arbitrary-code capability: the escape hatch is super-user-only
by construction, not by ACL configuration.

## 572.3 The residual risk is each station's program posture [CERT-synthesis]

The masking gates who can AUTHOR a robot on the supervisor. But `runRobot(code)` executes on the SUBORDINATE via
its `BProgramService` `[CERT] :105-107`, so the code runs under **that station's** program controls: the
`program` SecurityManager sandbox and the `program.requireSigning` flag ([Block 541]). [Block 398] found that
production supervisors frequently ship with `program.requireSigning` OFF — where that holds on a subordinate, a
robot runs **unsigned arbitrary code** on it. So the fleet-wide risk of the robot escape hatch is bounded by two
independent controls: (1) super-user-only authoring (this block), and (2) each subordinate's program-signing/
sandbox posture ([Block 541]/[Block 398]). Both matter; neither alone is sufficient. This is a clean instance of
the [Block 392] cross-cut — strong control over "who may run code", weaker only where an operator disabled signing.

## 572.4 Self-verify

| # | Claim | Marker | Citation | Verdict |
|---|-------|--------|----------|---------|
| 1 | BRobotJobStep (BDeviceJobStep) carries robotOrd → BProvisioningRobot; doRun runs it on each target station | [CERT] | BRobotJobStep.java:42,73 | token-checked ✓ |
| 2 | Execution = BProgramService.runRobot(code) on the target session (arbitrary Program code, B541) | [CERT] | BRobotJobStep.java:105-107 | token-checked ✓ |
| 3 | BProvisioningRobot.getPermissions masks non-super-users to operatorRead(1)\|adminRead(16), stripping all write/invoke | [CERT] | BProvisioningRobot.java:67-82 | token-checked ✓ |
| 4 | Super-users retain full permissions (mask only applies to non-super) | [CERT] | :69 | token-checked ✓ |
| 5 | Residual risk = subordinate's program.requireSigning/sandbox posture (B541/B398); robot code runs under it | [CERT-synthesis] | :105-107 + [B541]/[B398] | reasoned ✓ |

**Marker tally**: [CERT] ×4 · [CERT-synthesis] ×1 · [INFER] ×0. Block TYPE = EVIDENCE (decompilation). 4 of 5
rows token-verified inline.

## Connections

- **[Block 541]** — the `program` module runtime + SecurityManager sandbox + requireSigning; a robot IS a program.
- **[Block 398]** — hardening: `program.requireSigning` often OFF on supervisors → unsigned robot code risk.
- **[Block 567]** (PV1) — the batch engine that runs the robot step across the fleet.
- **[Block 392]** — the cross-cut: strong "who may run code" control (super-user-only authoring), weaker where
  signing is disabled.
- **[Block 18]** — module/program signing, the other half of the code-trust story.

## Open gaps (this block)

- `BRobotResult` fields and whether a robot failure aborts the enclosing job or is per-station tolerated are
  named-not-traced — low value; open on demand. Focus continues at PV7 (template deployment pipeline).
