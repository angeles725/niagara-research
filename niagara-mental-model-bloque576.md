# Block 576 — The provisioning ux/BOX RPC surface: five `permissions="unrestricted"` `@NiagaraRpc` methods that SELF-GATE on object-level `hasOperatorRead` — "unrestricted" means invocable, not ungated — closing the provisioning focus (and correcting the audit's ux class count)

**Session**: 2026-08-28
**Focus**: `provisioning` (gap PV10 — the browser-facing RPC surface; the final gap, closes the focus)
**Distribution**: Honeywell OptimizerSupervisor-N4.14.0.162
**Method**: READ-ONLY, inline bounded read of the `-ux` package + the RPC self-gate. Corrects an AUDIT-FIRST
class-count assumption.
**Primary sources** `[CERT]`:
- `organized/provisioningNiagara/provisioningNiagara-ux/vineflower/com/tridium/provisioningNiagara/ux/
  {BProvisioningNiagaraRpcUtil,BProvisioningNiagaraCssResource,BProvisioningNiagaraJsBuild}.java`.

**Scope**: the browser↔station RPC contract the provisioning UI uses. Applies the [Block 507] `@NiagaraRpc` /
[Block 512] BOX model. Closes PV10 and the provisioning focus.

---

## 576.1 The ux package is THREE classes — audit correction [CERT]

The AUDIT-FIRST seed estimated the `-ux` layer at ~48 classes with "40+ `BUx*Factory` step builders". Measured:
the `com.tridium.provisioningNiagara.ux` package is **3 classes** `[CERT]` — `BProvisioningNiagaraRpcUtil`
(the RPC surface), `BProvisioningNiagaraCssResource` + `BProvisioningNiagaraJsBuild` (the bajaux CSS/JS bundle
resources). There is **no `*Factory` class** in the package (glob returns 0). The audit's 48 conflated the whole
`-ux` jar with this package, and the "step-builder factory" pattern it imagined is not here — the UI is a bajaux
JS app served by the JS/CSS build resources, talking to the station through the RPC util. (§14 correction to the
PV10 seed.)

## 576.2 Five BOX RPCs, all declared "unrestricted" [CERT]

`BProvisioningNiagaraRpcUtil extends BSingleton` (`INSTANCE`) `[CERT] :40-41` exposes five `@NiagaraRpc` methods,
**every one `permissions = "unrestricted"`, transport `TransportType.box`** `[CERT]`:
`getDeviceId(nwOrd, deviceOrd)`, `getDevices(jobOrd)`, `getNtpServers(serverString)`, `getCleanDistFiles()`,
`getInstallable(navOrd)` `[CERT] :50-162`. At face value this contrasts sharply with [Block 507]'s finding that
`@NiagaraRpc` defaults to closed (`"I"` = Invoke) — `"unrestricted"` explicitly OPENS the method to any
authenticated caller over BOX.

## 576.3 …but the object-touching ones SELF-GATE [CERT]

"Unrestricted" is not "ungated". The methods that reach real objects perform their OWN permission check and throw
`PermissionException`. `getDeviceId` `[CERT] :56-64`:
```java
if (!nw.getPermissions(cx).hasOperatorRead())
   throw new PermissionException("Insufficient privileges to Device Network");
...
if (!device.getPermissions(cx).hasOperatorRead())
   throw new PermissionException("Insufficient privileges to Device");
```
So the RPC is reachable by any authenticated user, but it enforces **object-level `hasOperatorRead`** on the
target network and device using the caller's `Context` — the standard RBAC gate ([Block 561]/AC3, [Block 30]),
just applied INSIDE the method instead of by the RPC permission attribute. `getDevices`/`getInstallable` likewise
`throws PermissionException`/`Exception`. The two that do NOT (`getNtpServers`, `getCleanDistFiles` `[CERT]
:108,129`) read supervisor-LOCAL config (NTP server list, clean-dist file names) with no per-object target — low
sensitivity, so no per-object gate. This is a deliberate, consistent pattern: transport-open + in-method
object-RBAC, not an unauthenticated hole.

## 576.4 Self-verify

| # | Claim | Marker | Citation | Verdict |
|---|-------|--------|----------|---------|
| 1 | ux package = 3 classes (RpcUtil + CssResource + JsBuild); NO *Factory (corrects audit's 48/40-factory) | [CERT] | ls(ux) = 3; glob *Factory = 0 | measured ✓ |
| 2 | BProvisioningNiagaraRpcUtil = BSingleton with 5 @NiagaraRpc methods, all permissions="unrestricted", BOX transport | [CERT] | BProvisioningNiagaraRpcUtil.java:40-162 | token-checked ✓ |
| 3 | getDeviceId self-gates: nw/device.getPermissions(cx).hasOperatorRead() → PermissionException | [CERT] | :56-64 | token-checked ✓ |
| 4 | getDevices/getInstallable throw PermissionException/Exception; getNtpServers/getCleanDistFiles read local config, no per-object gate | [CERT] | :77,108,129,162 | token-checked ✓ |
| 5 | "unrestricted" = invocable, not ungated (object RBAC enforced in-method) | [CERT-synthesis] | rows 2-4 + [B507] | reasoned ✓ |

**Marker tally**: [CERT] ×4 · [CERT-synthesis] ×1 · [INFER] ×0. Block TYPE = EVIDENCE (decompilation). 4 of 5
rows token-verified inline. Corrects one AUDIT-FIRST assumption (ux class count).

## Connections

- **[Block 507]** — `@NiagaraRpc` (closed-by-default "I"); here overridden to "unrestricted" + in-method gate.
- **[Block 512]** — BOX transport, the wire these RPCs ride.
- **[Block 561]** (AC3) / **[Block 30]** — the object-permission model (`hasOperatorRead`) the methods enforce.
- **[Block 568]** (PV2) — the server-side channel; the ux RPCs are the browser front for the same operations.

## Open gaps (this block)

- The bajaux JS app itself (`rc/…` sources bundled by `BProvisioningNiagaraJsBuild`) is not decompiled — UI
  layer, low value for the provisioning model. **PV10 CLOSED; provisioning focus investigable=0 → STOP.**
