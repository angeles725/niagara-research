# B825 · Why an oBIX wrapped-`<obj>` PUT of a `BStatusNumeric` façade slot PROPAGATES to the linked control point — and why the read can lag: the write is a TOP-SLOT REPLACEMENT (decode into a detached copy → `parent.set(slot, copy)`), NOT an in-place child mutation; knob/link propagation is SYNCHRONOUS on the writing thread (<1 ms), so the tens-of-seconds "lag" is entirely the READER's poll cycle `[CERT + CERT-live]`

> **Scope**: the live fact ([CERT-live], viewer session, Cristian-authorized) — a wrapped oBIX PUT set `DashboardService/Cuarto1/setpoint`
> (a `BStatusNumeric` on `BRoomPanel`) and it PROPAGATED through the bog link to `Programacion/ColdRoom_1/setpoint`; an
> early read that showed the old value was a reader-side artifact, not a propagation failure. This block proves the
> **mechanism** ([CERT], Baja source) — correcting the initial hypothesis that the oBIX write mutates the struct child in
> place (which would not propagate): it does NOT. It settles the **timing** so "read-back after a settle" has a mechanism,
> not folklore, and states the implication for [B823] channel 1. REMITTANCE — [B816] (write-path/threading/LINK_TARGET),
> [B822]/[B823] (the setpoint write channels), [B509] (oBIX transport).
>
> **Sources**: FUENTE 3 (`[CERT]`, decompiled, crux cites confirmed at the enclosing method this session) — `com.tridium.obix.util.ObixUtils`,
> `javax.baja.obix.io.ObixDecoder`, `com.tridium.sys.schema.{ComplexSlotMap,ComponentSlotMap}`, `com.tridium.sys.engine.SlotKnobs`,
> `javax.baja.sys.BLink`, `javax.baja.sys.BComplex` (docSource), `javax.baja.driver.util.BPollScheduler`, `com.tridium.nd.point.{ServerWorker,ServerEntry}`.
> FUENTE 1 (`[CERT-live]`) — the persisted viewer record `sources/probes/2026-09-06-viewer-obix-setpoint-live-record.md`
> (propagation ×2, timing §7 measured on a monotonic clock, silent-zero hazard, per-slot writability). Markers: `[CERT]`
> code · `[CERT-live]` measured on the live station · `[INFER]` for the specific reader poll config not in this bog.

---

## 825.1 — The live fact `[CERT-live]`
The wrapped body `<obj is="/obix/def/baja:StatusNumeric"><real name="value" val="2.5"/></obj>` PUT to `Cuarto1/setpoint`
→ `200 OK`, and the value FOLLOWED through the live panel→control link (`ColdRoom_1` `Link2`: `sourceOrd`=Cuarto1,
`setpoint→setpoint`, target flags `sL`=LINK_TARGET) to `Programacion/ColdRoom_1/setpoint` — both `2.5`, then both back
to `3.0` on restore (two probes). An earlier "control stayed 3.0" report was a read taken before the reader's next
poll, corrected on a tightly-measured re-read (viewer record §1/§7). **Propagation is real and reliable; the question
this block answers is the MECHANISM and the TIMING.**

## 825.2 — The mechanism: the oBIX write is a TOP-SLOT REPLACEMENT, not an in-place child mutation `[CERT]`
The initial hypothesis (the oBIX decoder mutates the live struct's `value` child in place, which might not propagate the
enclosing slot's link) is **WRONG**. `ObixUtils.serviceWrite()` (`ObixUtils.java:532-566`) does:
```
BObject var11 = tgt.asValue().newCopy(true);   // :543 — a DETACHED COPY of the BStatusNumeric
BValue  val   = dec.decode(var11.asValue());   // :544 — decode mutates the COPY's children
…
parent.set(pary[idx], val, ot.getUser());      // :558 — TOP-SLOT REPLACEMENT on BRoomPanel
```
The `cpx.set(valueProp, made)` inside `ObixDecoder.decode()` (`ObixDecoder.java:200-216`, the `<obj>`-children branch)
runs on `var11` — the **detached copy**, which has no `parent`/`propertyInParent` pointer yet — so **no knob fires from
that inner set**. The single triggering call is `parent.set(setpointProp, <decoded copy>, user)` at `serviceWrite:558` —
the SAME top-slot replacement the servlet does (`parent.set(prop, new BStatusNumeric(v), null)`, [B823] §823.4). So oBIX
and the servlet reach control by the identical path. `[CERT]`

## 825.3 — Why the top-slot set fires the link — SYNCHRONOUSLY `[CERT]`
`parent.set(setpointProp, …)` → `ComponentSlotMap.modified(setpointProp)` (`ComponentSlotMap.java:711` vineflower / `:714`
decompiled) → `knobs.propagate(null)` BEFORE `changed()` ([B816] §816.1). `SlotKnobs.propagate()` (`SlotKnobs.java:31-46`)
is a plain **inline loop** — for each knob, `link.getLink(); if (link!=null && link.isEnabled()) link.propagate(arg);` —
**NO queue, NO `postAsync`, NO engine-cycle deferral** (only a try/catch that logs). `BLink.propagatePropertyToProperty`
(`BLink.java:719`) then calls `target.set(tProp, value, null)` synchronously. The engine thread (`EngineManager`, 20 ms
sleep) handles only timer ticks + async ACTIONS — property→property links are NOT enqueued there. **So the whole
source→target propagation runs INLINE on the oBIX HTTP thread, before `serviceWrite` returns** (<1 ms). `[CERT]`

**The nested-child bubbling path also exists** (would matter for a `…/setpoint/value` direct write): a live `BComplex`
holds `ComplexSlotMap.parent` + `propertyInParent` (set at `ComplexSlotMap.java:1518-1519` when the value is stored), and
`ComplexSlotMap.modified()` bubbles up — `if(parent!=null) parent.modified(propertyInParent,…)` (`ComplexSlotMap.java:1468`
decompiled) → `ComponentSlotMap.modified(setpointProp)` → the same `knobs.propagate()`. So a TRUE in-place child change
would ALSO propagate — but the oBIX PUT doesn't take that path (§825.2). Either way the link fires. `[CERT]`

## 825.4 — The timing: propagation synchronous, lag is the READER's poll cycle `[CERT + CERT-live]`
- **Propagation**: synchronous, <1 ms (§825.3). Measured end-to-end (viewer record §7, monotonic clock, LAN): oBIX PUT
  round-trip 132 ms warm / 638 ms cold-TLS; the control side reflected the new value within **155 ms (restore) – 665 ms
  (write) total**, i.e. the link fires ~20-30 ms after the PUT completes, **same engine cycle**, both directions;
  propagates in <1 s every time. `[CERT-live measured]`
- **The tens-of-seconds "lag"** an early read saw is entirely READER-side, NOT deferred propagation: an oBIX POLL reader
  sees the old value until its next poll — `BPollScheduler` defaults `fastRate=1000ms`, `normalRate=5000ms`,
  `slowRate=30000ms` (`BPollScheduler.java`), so a slow-rate point reads stale for up to 30 s `[INFER — the reader's rate
  is not in this bog]`; a NiagaraDriver Fox subscription throttles by `ServerWorker.WORKER_SLEEP=1000ms` +
  `ServerEntry` `minSendTime`/`maxSendTime` (`ServerWorker.java:19`, `ServerEntry.java:126-138`) `[CERT]`. The
  Supabase/dashboard leg is ~6 s, bounded by the write-server poller's 5 s cycle — a poller artifact, not the control
  write (viewer §7). The "80+ s" first report was a single early/stale read of an already-propagated value. `[CERT-live]`

## 825.5 — Doctrine `[INFER, grounded]`
- **Corrected doctrine**: *an external oBIX write to a complex `BStatusNumeric` property IS a top-slot replacement (the
  decoder mutates a detached copy, then `serviceWrite` sets the whole slot), so it DOES propagate the slot's outgoing
  links — synchronously, on the writing thread, identically to the servlet.* (This CORRECTS the drafted hypothesis
  "external complex writes do not propagate; only a slot replacement does" — the oBIX write already IS a slot replacement.)
- **Read-back after a settle** is a READER discipline, not a propagation delay: the value is live in the station in <1 ms;
  a `~1 s` settle before reading the CONTROL side covers any poll-cycle skew; reading via the dashboard/Supabase needs
  `~6 s` (the poller). A read taken sooner than the reader's poll period is a FALSE NEGATIVE (the [B825]/retro process
  lesson: a single early read is not evidence — repeat after the reader's settle).
- **The silent-zero hazard stays** ([B823] §823.2): the attr-only `<obj … val="2.5"/>` (no `value` child) returns 200 but
  writes 0.0 — the decoded copy defaults; the body MUST carry the exact `<real name="value" val>` child. `[CERT-live]`

## 825.6 — Implication for [B823] channel 1 `[INFER, grounded]`
Channel 1 (the wrapped oBIX PUT to the RoomPanel `BStatusNumeric`) is a **viable CONTROL write**, not a display-only
desync: it lands a top-slot replacement that propagates through the bog link to the control point synchronously. The two
hazards are (a) the **silent-zero body** (attr-only → 0.0, §825.5) and (b) the **reader-side read-timing** (poll cycle;
read-back after ~1 s control / ~6 s dashboard) — NEITHER is a propagation failure. This removes the "in-place child won't
propagate" concern from [B823] §823.7's channel-1 line. **LINK_TARGET note ([B816] §816.2/§816.6)**: this write is on the
SOURCE side of the link (`Cuarto1/setpoint`, which DRIVES `ColdRoom_1/setpoint`), so it propagates and STICKS. A write to
the link-TARGET side (`ColdRoom_1/setpoint`, flags `sL`=LINK_TARGET) would instead be EPHEMERAL — overwritten on the next
source propagation ([B816] §816.2). Write the SOURCE (the façade), not the target — which is exactly what the live probe did.

## Self-verify
| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | The wrapped oBIX PUT propagates to the linked control point (both 2.5, then 3.0) | `[CERT-live]` | viewer record §1; ColdRoom_1 `Link2` source=Cuarto1 setpoint→setpoint |
| 2 | oBIX write = decode into a DETACHED copy (`newCopy(true)`) then TOP-SLOT `parent.set(slot, copy, user)` — not an in-place child mutation | `[CERT]` | `ObixUtils.java:543,544,558`; the inner `cpx.set` (`ObixDecoder.java:200-216`) is on the copy |
| 3 | The top-slot set fires `knobs.propagate()` which calls `link.propagate` SYNCHRONOUSLY (no queue/engine deferral) | `[CERT]` | `ComponentSlotMap.java:711`; `SlotKnobs.java:31-46`; `BLink.java:719` |
| 4 | A true nested-child write would also propagate (parent bubbling) | `[CERT]` | `ComplexSlotMap.java:1468,1518-1519` → `ComponentSlotMap.modified` |
| 5 | Propagation <1 ms; measured control-side <1 s (155-665 ms); Supabase leg ~6 s | `[CERT + CERT-live]` | §825.3 synchronous path; viewer record §7 (monotonic) |
| 6 | The lag is reader-side: oBIX poll (slowRate 30 s) / NiagaraDriver ServerWorker 1 s + min/maxSendTime | `[CERT for the throttles; INFER for this reader's rate]` | `BPollScheduler.java`; `ServerWorker.java:19`, `ServerEntry.java:126-138` |
| 7 | attr-only `<obj … val>` writes 0.0 (silent-zero); the write is on the SOURCE side so it sticks | `[CERT-live]`+`[CERT]` | viewer §2; [B816] §816.2 (source vs LINK_TARGET) |

**Tally**: 3 `[CERT]` · 2 `[CERT-live]` · 2 `[CERT+CERT-live]` (1 with an `[INFER]` on the reader's poll rate). The two
hypothesis-overturning cites (serviceWrite top-slot set; SlotKnobs synchronous) were confirmed at the enclosing method
this session. §825.5/§825.6 doctrine is `[INFER]` grounded in the [CERT] mechanism + the [CERT-live] measurement. Dedupe:
the set()→modified()→knobs.propagate()→changed() spine + LINK_TARGET are REMITTANCE ([B816] §816.1/816.2); this block adds
the decode-into-copy/top-slot-replacement finding, the SYNCHRONOUS propagation proof, the reader-side lag explanation, and
the corrected channel-1 doctrine.

## Connections
- **[B816]** §816.1 (set→modified→knobs.propagate→changed = the propagation spine, confirmed synchronous here) / §816.2/§816.6
  (LINK_TARGET source-vs-target — §825.6), **[B823]** §823.2/§823.7 (channel 1 — this removes the desync concern + grounds
  the read-back-after-settle), **[B822]** (the additive action alternative), **[B509]** (oBIX transport). Retro:
  `retros/2026-09-06-obix-statusnumeric-wrapped-put-retro.md` (the believed→code→live process lesson this closes with the
  mechanism). Kit: a `types/logic-authoring.md` line under §"Slot types for externally written values" — *an external
  complex-property write propagates links synchronously; the settle is the reader's poll cycle, ~1 s control / ~6 s dashboard*.

## Open gaps
- **B825-G1** (bounded, requires-execution): confirm the specific reader's poll/subscription rate on PANCCADIA (is the
  write-server's control-side read an oBIX poll at `slowRate`, a Fox subscription, or a direct GET?) — this fixes the
  exact settle margin beyond the measured <1 s. Pairs with the viewer's live session.
- **B825-G2** (bounded): whether a `…/setpoint/value` DIRECT child-path oBIX write (vs the wrapped whole-slot PUT) is even
  routable by `BObixServer`'s URI resolver — the §825.3 bubbling path predicts it would propagate, but the server may not
  expose a child ORD; untested.
