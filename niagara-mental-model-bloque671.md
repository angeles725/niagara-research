# B671 — External oBIX alarm-ACK: an HTTP client POSTs to `/obix/alarm/<uuid>/ack` with a tiny `<obj><str name="ackUser" .../></obj>` (contract `obix:AckAlarmIn`, only field `ackUser`, NO `ackData`); the record UUID is the primary key (same uuid the webhook emits), reached read-level with BASIC auth — the ack invoke path has NO write gate, only force-clear needs admin-write (focus alarm-webhook, AW6; builds on B509/B600/B499)

> **Focus:** `alarm-webhook` (§16, re-opened). **Gap closed:** AW6 (how a Node backend acks a Niagara alarm
> via oBIX). **Phase:** static, READ-ONLY. Server code delegated to an Explore sweep; all load-bearing lines
> RE-VERIFIED by the driver (grep) before sealing.
> **Sources** (all `[CERT]`, `obixDriver-rt` + `obix-rt` vineflower):
> - `organized/obixDriver/obixDriver-rt/vineflower/com/tridium/obix/server/{BAlarmServiceAgent,BAlarmLobbyAgent,BAlarmsLobbyAgent,BAlarmWrapper,BAlarmServiceQuery}.java`
> - `organized/obixDriver/obixDriver-rt/vineflower/com/tridium/obix/util/ObixUtils.java`
> - `organized/obix/obix-rt/vineflower/obix/contracts/{AckAlarm,AckAlarmIn,AckAlarmOut,Alarm,AlarmFilter,AlarmQueryOut}.java`
> - `organized/obixDriver/obixDriver-rt/vineflower/com/tridium/obix/import/BObixAlarmImport.java` (Niagara acting as an oBIX ack CLIENT — the canonical request example)
> - `[CERT]` corpus [Block 509] (BObixServer /obix, verbs, RBAC), [Block 600] (typed oBIX AlarmSubject query), [Block 499] (obixDriver, BASIC auth), [Block 345] §345.4 (uuid is a BAlarmRecord frozen slot).
>
> **Bottom line for the PoC:** the ack is a single `POST /obix/alarm/<uuid>/ack` (BASIC auth, `Content-Type:
> text/xml`) with body `<obj is="obix:AlarmAckIn"><str name="ackUser" val="…"/></obj>`. The **`<uuid>` is the
> exact same BAlarmRecord uuid your webhook already sends** — no query-then-match needed. The only declared
> input field is **`ackUser`** (there is **no `ackData`** in this build's contract), and even that is
> overwritten by the authenticated login for a normal ack. Reaching the alarm needs only **read**-level oBIX
> access; the ack invoke itself has **no additional permission check** (unlike a write, and unlike force-clear
> which requires admin-write on the alarm class).

---

## §671.1 — The endpoint: `/obix/alarm/<uuid>` and its `ack` op `[CERT]`

There are **two** alarm lobbies under `/obix` (both `@AgentOn ObixLobby requiredPermissions="r"`):
- **`alarms`** (plural) → the `BAlarmService` subject/query surface (`BAlarmsLobbyAgent`), i.e. the
  `/obix/config/Services/AlarmService/` typed query from [Block 600] §600.4.
- **`alarm`** (singular) → the per-record resolver used for ack: `BAlarmLobbyAgent.lobbyName = "alarm"`
  `[CERT BAlarmLobbyAgent.java:29]`.

A single alarm is encoded with `href = makeAlarmUri(out, rec.getUuid().encodeToString())`
`[CERT BAlarmServiceAgent.java:65]`, where `makeAlarmUri(out, uuid) = concat(lobbyPath,"alarm",uuid)`
`[CERT ObixUtils.java:451]` — i.e. **`/obix/alarm/<uuid>`**. Its ack op is attached as
`obj.initOp("ack", "obix:AlarmAckIn", "obix:AlarmAckOut"); obj.setHref(concat(href,"ack"))`
`[CERT BAlarmServiceAgent.java:74-75]` — i.e. **`/obix/alarm/<uuid>/ack`**. The alarm `<obj>` carries
`is="obix:Alarm obix:AckAlarm"` `[CERT:47]` (+ `obix:PointAlarm`/`obix:StatefulAlarm` variants) and an explicit
`<str name="niagara-uuid" val="<uuid>"/>` `[CERT:122]`.

**Contract-name quirk to be aware of `[CERT]`:** the op *advertises* `in="obix:AlarmAckIn"
out="obix:AlarmAckOut"` `[:74]`, but the **registered** contracts are `obix:AckAlarm` / `obix:AckAlarmIn` /
`obix:AckAlarmOut` (`ContractInit`); `obix:AlarmAckIn`/`AlarmAckOut` are NOT in the registry. The server does
not resolve the posted `is=` against the registry — `BAlarmWrapper.invoke` reads the posted complex
positionally by child name (`get("ackUser")`) — so the `is=` string on your request body is effectively
cosmetic. The response is written `is="obix:AckAlarmOut"` `[CERT BAlarmWrapper.java:~144]`.

## §671.2 — Referencing ONE alarm: the UUID is the primary key `[CERT]`

`BAlarmLobbyAgent.resolve` `[CERT:49-74]` parses `/obix/alarm/<uuid>[/ack]`:
```java
ack = uri.contains("ack");                         // :55
BUuid uid = (BUuid)BUuid.DEFAULT.decodeFromString(uuid);   // :60
BAlarmRecord rec = conn.getRecord(uid);            // :67  ← direct DB lookup by UUID
if (rec == null) throw new BadUriErr("UUID not found: " + uuid);   // :69
… new ObixTarget(ot, new BAlarmWrapper(rec, ack));  // :74
```
So the client references the alarm **by its Niagara record UUID**, carried in the href. **This is the same
`BAlarmRecord.getUuid()` your webhook emits** ([Block 670] §670.1; a frozen slot per [Block 345] §345.4) — you
do **not** need to query-then-match. `POST /obix/alarm/<that-uuid>/ack` hits the exact record.

**Fallback discovery (only if you didn't keep the uuid):** `GET /obix/config/Services/AlarmService/` returns an
`obix:AlarmSubject` with `<op name="query" in="obix:AlarmFilter" out="obix:AlarmQueryOut">` and a `feed`
([Block 600]). But **`AlarmFilter` has NO uuid/source field** — only `limit`(int)/`start`/`end`(abstime)
`[CERT AlarmFilter.java:8-13]` (server → `bql: … from openAlarms where lastUpdate between start,end`). So the
fallback is: query a time window → each returned `<obj>` carries `href="/obix/alarm/<uuid>"` and
`<str name="niagara-uuid">` → match client-side on `niagara-uuid`/`source`/`timestamp` → use its `ack` href.
Keeping the uuid from the webhook is strictly simpler.

## §671.3 — The exact ack request `[CERT]` (modeled on Niagara's own oBIX ack client)

```
POST /obix/alarm/<uuid>/ack HTTP/1.1
Host: <station>
Authorization: Basic base64(obixUser:password)
Content-Type: text/xml

<obj is="obix:AlarmAckIn">
  <str name="ackUser" val="telegram-bot"/>
</obj>
```
Evidence — `BObixAlarmImport.doAckAlarm` (Niagara AS the client) builds exactly this:
`arg.setIs(new Contract("obix:AlarmAckIn"))` + child `Str name="ackUser"`, then `obixInvoke(op, arg)` = POST
`text/xml` `[CERT BObixAlarmImport.java:~199-212; verbs from Block 499 §499.2]`. Server routing:
`BObixServer.service` POST → `ObixUtils.serviceInvoke` → for a `BIObixInvocable` target,
`((BIObixInvocable)tgt).invoke(dec,enc)` `[CERT ObixUtils.java:494-497]` → `BAlarmWrapper.invoke` reads
`get("ackUser")` `[CERT BAlarmWrapper.java:62]`, sets `AckState.ackPending`, `conn.update(rec)`,
`service.ackAlarm(rec)`, and returns `<obj is="obix:AckAlarmOut"><obj name="alarm" is="obix:Alarm obix:AckAlarm"…/></obj>`.

- **`ackUser` is the ONLY declared field** — `AckAlarmIn` contract = `<str name='ackUser' val='' null='true'/>`
  and the sole accessor `Str ackUser()` `[CERT AckAlarmIn.java:7,9]`. **There is no `ackData` field** in this
  build (contrary to the generic oBIX spec). A minimal `<obj><str name="ackUser" val=""/></obj>` also works.
- **Attribution `[INFER — decompile-ambiguous]`:** for a normal ack the record's user is set from the
  **authenticated** BASIC-auth user (`user.getFullName()`/username), so the `ackUser` you POST is largely
  cosmetic `[CERT BAlarmWrapper.java:66]`. The exact precedence when the posted `ackUser` is non-empty is
  obscured by a decompiled inverted null-check (`:62-64`); treat "the ack is attributed to the oBIX login" as
  the reliable statement and confirm the posted-value precedence live if it matters.

## §671.4 — Auth & permissions `[CERT paths] / [INFER net effect]`

- **BASIC auth over `/obix`** — confirmed ([Block 499] §499.2, [Block 509] §509.5). `BObixServer` reads the
  `Authorization` header; sessions are invalidated per-request unless `allowSessionReuse`.
- **Normal ack = read-level, no write gate.** The alarm lobbies register `requiredPermissions="r"`
  `[CERT BAlarmLobbyAgent.java:24]`, and the invoke path `ObixUtils.serviceInvoke` calls `invoke()` with **no
  permission check** `[CERT:494-497]` — in contrast to `serviceWrite`, which throws
  `PermissionErr(... cannot write this object)` `[CERT:564]`. So a user who can *resolve* `/obix/alarm/<uuid>`
  (read on the lobby) can ack it; there is no operator/write gate in the ack code path. `[INFER — net effect:
  whether a low-privilege user is rejected at lobby resolution before invoke is a live-station residual gap.]`
- **Force-clear IS gated.** Posting an extra `<str name="forceCleared" val="…"/>` triggers the privileged
  branch: `alarmClass.getPermissions(cx).hasAdminWrite()` and, if absent, `throw new SecurityException("A
  force clear was attempted on an alarm via oBIX without admin write permission on its associated alarm
  class.")` `[CERT BAlarmWrapper.java:78-84]`. It then sets `acked` + `normal` and audits via
  `BAlarmService.auditForceClear` and adds an `addAlarmFacet("forceCleared", …)` `[CERT:87-91]`.

**Security note for the operator:** because a plain ack needs only read-level oBIX access with no invoke-time
gate, the oBIX user handed to the Node backend should be scoped tightly (a dedicated low-privilege account),
and — since acking is state-changing yet ungated — treated as a write-capable credential despite the
`requiredPermissions="r"` label. This is consistent with [Block 509]'s finding that `/obix` writes are RBAC-gated
but there is no per-object read ACL.

## §671.5 — uuid mapping (the answer to "can I ack by our uuid?") `[CERT]`

**Yes, directly.** The oBIX per-alarm href is literally `.../alarm/<rec.getUuid().encodeToString()>`
`[CERT BAlarmServiceAgent.java:65 + ObixUtils.java:451]`, round-tripped server-side via
`BUuid.decodeFromString(uuid)` → `conn.getRecord(uid)` `[CERT BAlarmLobbyAgent.java:60,67]`. The uuid is also
re-exposed as `<str name="niagara-uuid">` `[CERT:122]` for matching inside query/feed results. The abstract
`obix:Alarm`/`StatefulAlarm` contracts only declare `source`/`timestamp`/`normalTimestamp` — uuid is a Niagara
extension surfaced as the href + the `niagara-uuid` element, not part of the portable contract.

**One residual encoding caveat `[INFER]`:** `makeAlarmUri` does a raw `concat` with no explicit URL-encoding
of the uuid. Confirm on a live station that `BUuid.encodeToString()`'s output survives URL path encoding
(percent-encode on the client if it contains reserved chars).

## §671.6 — Self-verify

| # | Claim | Marker | Cite |
|---|---|---|---|
| 1 | Ack endpoint = `POST /obix/alarm/<uuid>/ack`; single alarm at `/obix/alarm/<uuid>` | [CERT] | BAlarmServiceAgent.java:65,74-75; ObixUtils.java:451; BAlarmLobbyAgent.java:29,55 |
| 2 | Client references the alarm by BAlarmRecord UUID; server does `decodeFromString`→`getRecord` | [CERT] | BAlarmLobbyAgent.java:60,67,69 |
| 3 | Body = `<obj is="obix:AlarmAckIn"><str name="ackUser".../></obj>`, POST text/xml, BASIC auth | [CERT] | BObixAlarmImport.java:199-212; ObixUtils.java:494-497 |
| 4 | `AckAlarmIn` has ONLY `ackUser` (no `ackData`); posted `is=` not registry-validated | [CERT] | AckAlarmIn.java:7,9; BAlarmServiceAgent.java:74 |
| 5 | ackUser overwritten by authenticated user (attribution = oBIX login) | [CERT] + [INFER precedence] | BAlarmWrapper.java:62-66 |
| 6 | Normal ack has no invoke-time permission check; write does throw PermissionErr | [CERT paths] + [INFER net] | ObixUtils.java:494-497,564; BAlarmLobbyAgent.java:24 |
| 7 | force-clear requires `hasAdminWrite`, else SecurityException + audit | [CERT] | BAlarmWrapper.java:78-91 |
| 8 | Same webhook uuid is the ack key; also re-exposed as `niagara-uuid`; AlarmFilter has no uuid/source filter | [CERT] | BAlarmServiceAgent.java:65,122; AlarmFilter.java:8-13 |
| 9 | URL-encoding of the uuid path segment unverified statically | [INFER] | §671.5 |

**Tally:** 9 claims — 7 [CERT], 2 mixed [CERT+INFER] (#5,#6), 1 [INFER] (#9). 0 unmarked.

## §671.7 — Connections

- **[Block 509]** — BObixServer `/obix` mount, GET/PUT/POST verbs, RBAC-gated writes / no per-object read ACL
  (this block extends it with the alarm ack invoke path having no gate).
- **[Block 600]** — the typed `AlarmSubject` query op (the `alarms` plural lobby / fallback discovery).
- **[Block 499]** — obixDriver verbs + BASIC auth (`invoke = POST text/xml`).
- **[Block 670]** — the webhook `toJson` that emits the uuid this block acks by; `getUuid()`/`getAckState()`.
- **[Block 34] §34.1.3** — the station-side ack path (`doAckAlarm`/`doRouteToSource`); oBIX ack ultimately
  calls `service.ackAlarm(rec)`.
- **[Block 345] §345.4** — uuid as a BAlarmRecord frozen slot.
