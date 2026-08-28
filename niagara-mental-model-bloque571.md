# Block 571 — The async-action protocol: `BProvisioningStationExt`'s correlation-id completion pattern (`makeInvokeId` → `asyncActionComplete` topic → `BAsyncActionEvent` result-or-error) — a hand-rolled async RPC every provisioning station-ext inherits, distinct from BJob

**Session**: 2026-08-28
**Focus**: `provisioning` (gap PV5 — the long-running-operation pattern shared by every `BProvisioningStationExt`)
**Distribution**: Honeywell OptimizerSupervisor-N4.14.0.162
**Method**: READ-ONLY, inline bounded read of `BProvisioningStationExt` + `BAsyncActionEvent` + the subclass
sweep. Actions/topic/event token-verified inline.
**Primary sources** `[CERT]`:
- `organized/provisioningNiagara/provisioningNiagara-wb/vineflower/com/tridium/provisioningNiagara/
  {BProvisioningStationExt,BAsyncActionEvent}.java`.

**Scope**: the reusable async-completion mechanism the provisioning station-exts use for multi-minute operations
over Fox. Distinct from the `BJob` batch job ([Block 511]/[Block 567]). Does NOT re-open Fox topics/actions
([Block 4]) — applies them.

---

## 571.1 The base ext declares an async-RPC surface [CERT]

`abstract class BProvisioningStationExt extends BDeviceExt implements BIMixIn, BIStatus` `[CERT] :74` — a mixin
device-ext on a `BNiagaraStation`. It declares, via annotations `[CERT] :57-80`:
- `@NiagaraAction makeInvokeId(BString)` `[CERT] :63,79,118` — mint a unique correlation id (`asyncInvokeId`
  counter starts at 1 `[CERT] :88`).
- `@NiagaraAction cancelAsyncAction(BString)` `[CERT] :58,78,114` — cancel a running op by its invoke id.
- `@NiagaraTopic asyncActionComplete` (eventType `BAsyncActionEvent`) `[CERT] :69-80` +
  `fireAsyncActionComplete(BAsyncActionEvent)` `[CERT] :122-123` — the completion signal.

So the ext is a small async-RPC substrate: mint an id, fire a completion event carrying that id, cancel by id.

## 571.2 The completion event carries id + result OR error [CERT]

`final class BAsyncActionEvent extends BSimple` `[CERT] :18` holds a `BString invokeId` `[CERT] :21` and has
three constructors `[CERT] :36-51`:
- `(invokeId)` — completion, no return value;
- `(invokeId, BValue returnValue)` — completion with a result;
- `(invokeId, Exception e)` / `(invokeId, String msg)` — completion as a FAILURE (the exception message is
  carried).

So one event type represents success-with-value, success-without-value, and failure — the async result
envelope, keyed by the correlation id.

## 571.3 The usage pattern [CERT]

The ext itself shows the caller side `[CERT] :279-280`:
```java
BString invokeId = this.makeInvokeId(action);
AsyncActionCompleteSubscriber sub = new AsyncActionCompleteSubscriber(invokeId);
// subscribe to asyncActionComplete, then invoke the long action...
```
An `AsyncActionCompleteSubscriber` (keyed by `invokeId`) listens on the `asyncActionComplete` topic; when the
remote finishes the long operation it calls `fireAsyncActionComplete(new BAsyncActionEvent(invokeId, result))`,
the subscriber matches its own id, and resolves (or throws, if the event carries an error). `cancelAsyncAction`
lets the caller abort a still-running op. This is a **correlation-id async RPC hand-rolled on Fox topics** — it
exists because provisioning operations (install, backup, template deploy) run for minutes and cannot be
synchronous Fox calls.

## 571.4 Inherited by every provisioning station-ext [CERT]

Concrete subclasses `[CERT]` (sweep): `BSoftwareStationExt`, `BTemplateStationExt`, `BLicenseStationExt`,
`BBackupStationExt`, and `BStationProxy`. So software distribution ([Block 569]), template deploy (PV7), license
sync (PV9), and backup all share this ONE completion mechanism — each long operation on a subordinate reports
back through `asyncActionComplete` with its invoke id.

## 571.5 Two long-op mechanisms, different scopes [CERT-synthesis]

Provisioning uses TWO complementary long-running-op mechanisms: **`BJob`** ([Block 511]/[Block 567]) frames the
NETWORK-WIDE batch job (progress bar, cancel, state machine over the whole fleet), while this **async-action
topic** is the PER-STATION-EXT RPC completion for one operation on one subordinate. The batch job orchestrates;
the async-action protocol is how each individual station-ext operation signals done/failed/canceled back up.
They compose: a `BForEachStationStage` step invokes a station-ext operation and waits on its
`asyncActionComplete` while the enclosing `BJob` tracks overall progress.

## 571.6 Self-verify

| # | Claim | Marker | Citation | Verdict |
|---|-------|--------|----------|---------|
| 1 | BProvisioningStationExt = abstract BDeviceExt + BIMixIn + BIStatus; declares makeInvokeId/cancelAsyncAction actions + asyncActionComplete topic | [CERT] | BProvisioningStationExt.java:57-80 | token-checked ✓ |
| 2 | asyncInvokeId counter starts 1; makeInvokeId mints correlation id | [CERT] | :88,118 | token-checked ✓ |
| 3 | BAsyncActionEvent (BSimple) carries invokeId + result value OR exception message (3 ctors) | [CERT] | BAsyncActionEvent.java:18-51 | token-checked ✓ |
| 4 | Caller pattern: makeInvokeId → AsyncActionCompleteSubscriber(invokeId) → invoke → fireAsyncActionComplete matches | [CERT] | BProvisioningStationExt.java:122,279-280 | token-checked ✓ |
| 5 | Inherited by Software/Template/License/Backup StationExts + BStationProxy | [CERT] | subclass sweep | grep-confirmed ✓ |
| 6 | Two long-op mechanisms: BJob (network-wide) vs async-action (per-station-ext op) | [CERT-synthesis] | rows 1-5 + [B567] | reasoned ✓ |

**Marker tally**: [CERT] ×5 · [CERT-synthesis] ×1 · [INFER] ×0. Block TYPE = EVIDENCE (decompilation). 5 of 6
rows token-verified inline.

## Connections

- **[Block 567]** (PV1) — the BJob batch engine; the async-action protocol is the per-op completion inside it.
- **[Block 569]** (PV3) / PV7 / PV9 — Software/Template/License station-exts all inherit this mechanism.
- **[Block 511]** — the BJob base; the OTHER long-op mechanism, network-wide.
- **[Block 4]** — Fox topics/actions, the substrate this async-RPC is built on.

## Open gaps (this block)

- The `AsyncActionCompleteSubscriber` timeout/cleanup (what happens if the completion event never arrives) is
  named-not-traced — low value; open on demand. Focus continues at PV6 (BProvisioningRobot permission masking).
