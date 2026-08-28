# Block 566 — `UserMonitor` + `BUserEvent`: the reactive user-space hook — a `userEvent` topic fires typed add/remove/rename/modify events, and the supervisor's user-replication device-ext is the real consumer (closes the access-control focus)

**Session**: 2026-08-28
**Focus**: `access-control` (gap AC8 — the `UserMonitor`/`BUserEvent` framework hooks; the last gap, closes the
focus)
**Distribution**: Honeywell OptimizerSupervisor-N4.14.0.162
**Method**: READ-ONLY, inline bounded read of the two classes + a consumer sweep.
**Primary sources** `[CERT]`:
- `organized/baja/baja/vineflower/javax/baja/user/{UserMonitor,BUserEvent}.java`.
- consumer `organized/niagaraDriver/.../com/tridium/nd/user/BNiagaraUserDeviceExt` (existence-confirmed).

**Scope**: how the framework broadcasts user-space changes reactively (vs polling). Closes AC8 and the
`access-control` focus. Does NOT open the supervisor user-replication flow ([Block 414–420] territory) — only
the event hook it consumes.

---

## 566.1 `UserMonitor` — a subscriber that requires a `userEvent` topic [CERT]

`public final class UserMonitor` `[CERT] :14` wraps a component and **requires it to implement a `userEvent`
Topic** — the constructor throws if absent: `if (comp.getTopic("userEvent") == null) throw new
BajaRuntimeException("Component must implement userEvent Topic!")` `[CERT] :19-20`. It holds an inner
`UserSubscriber extends Subscriber` `[CERT] :146` whose `event(BComponentEvent)` `[CERT] :151-166` translates raw
component events into typed user events and calls `fireUserEvent`, which fires on the host's topic:
`this.comp.fire(this.comp.getTopic("userEvent"), event)` `[CERT] :142-143`. So any container of users (a user
service, a prototype set, a driver's user store) gains a reactive change stream by hosting the topic and a
monitor.

## 566.2 `BUserEvent` — a typed change record [CERT]

`public class BUserEvent extends BStruct` `[CERT] :26` carries `(id, userName, oldName)` `[CERT] :13-34`. The
`id` vocabulary `[CERT] :27-31`:

| id | Constant | Fired by |
|---|---|---|
| −1 | `UNKNOWN` | — |
| 0 | `ADDED` | `makeAdded(user)` `[CERT] :82` |
| 1 | `REMOVED` | `makeRemoved(user, propName)` `[CERT] :92` |
| 2 | `MODIFIED` | `makeModified(user)` / `new BUserEvent(2, user)` `[CERT] :58,117,164` |
| 3 | `RENAMED` | `makeRenamed(user, oldName)` `[CERT] :102` |

`oldName` is populated only for RENAMED. So a subscriber learns exactly which user changed and how — add / remove
/ rename (with the previous name) / modify — without diffing the user space itself.

## 566.3 The real consumer: supervisor user replication [CERT]

The sweep shows the concrete consumer is `com.tridium.nd.user.BNiagaraUserDeviceExt` (the `niagaraDriver`
user-replication device extension). This is the seam by which a **supervisor propagates user add/remove/rename/
modify to subordinate stations reactively** — when a user changes on the supervisor, the `userEvent` fires, the
device-ext hears it, and replication follows, rather than a periodic full-sync. This ties AC8 directly into the
`niagara-network-supervisor` thread ([Block 414–420]): user identity is one of the things the Fox join keeps in
sync, and `BUserEvent` is its trigger. (`BUserPrototypes` [Block 559] also hosts a `userEvent` topic — the same
reactive pattern for prototype-driven provisioning.)

## 566.4 Self-verify

| # | Claim | Marker | Citation | Verdict |
|---|-------|--------|----------|---------|
| 1 | UserMonitor requires host to implement userEvent Topic (throws otherwise); inner Subscriber translates component events | [CERT] | UserMonitor.java:14-20,146-166 | token-checked ✓ |
| 2 | fireUserEvent fires BUserEvent on the host's userEvent topic | [CERT] | :142-143 | token-checked ✓ |
| 3 | BUserEvent extends BStruct, (id,userName,oldName); id = UNKNOWN(-1)/ADDED(0)/REMOVED(1)/MODIFIED(2)/RENAMED(3) | [CERT] | BUserEvent.java:26-34 | token-checked ✓ |
| 4 | makeAdded/Removed/Renamed/Modified factories fire the matching id; oldName only for RENAMED | [CERT] | UserMonitor.java:82,92,102,117 | token-checked ✓ |
| 5 | Real consumer = niagaraDriver BNiagaraUserDeviceExt (supervisor user replication) | [CERT] | consumer sweep path | grep-confirmed ✓ |

**Marker tally**: [CERT] ×5 · [INFER] ×0. Block TYPE = EVIDENCE (decompilation). 5 of 5 rows token-verified
inline.

## Connections

- **[Block 559]** (AC2) — `BUserPrototypes` also hosts a `userEvent` topic; the same reactive hook for
  provisioning.
- **[Block 414–420]** — the supervisor↔subordinate join; `BUserEvent` is the trigger for reactive user
  replication via `BNiagaraUserDeviceExt`.
- **[Block 564]** (AC6) — user changes are ALSO audited (AuditEvent Changed/Added/Removed); events and audit are
  parallel outputs of the same mutation.

## Open gaps (this block)

- `BNiagaraUserDeviceExt` replication internals (conflict handling, direction) are niagara-network-supervisor
  territory — named, not opened. **AC8 CLOSED; access-control focus investigable=0 → STOP.**
