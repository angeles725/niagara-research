# B826 · Is a `/setpoint/value` CHILD ORD routable over oBIX? — closes [B825]-G2 by read-only reasoning: the `BStatusValue` agent COLLAPSES the struct to a leaf `<real>` so the child is NEVER advertised, but the config-space resolver builds a `station:\|slot:…/setpoint/value` ORD by VERBATIM path translation, so a hand-crafted child PUT is resolvable — a simpler (bare-`<real>`, no silent-zero) write than the wrapped-`<obj>` — now LIVE-CONFIRMED END-TO-END: the child is served + `writable="true"` (GET, B826-G1) and a bare-`<real>` PUT to it writes AND propagates to control in ~1.5 s (B826-G2), so it is the PREFERRED write form; the wrapped-`<obj>`-to-parent-slot is the proven fallback `[CERT code + CERT-live end-to-end]`

> **Scope** (narrow, closes [B825]-G2 — read-only reasoning from the export encoder + the config-space resolver, NO probe
> per the lead): the wrapped-`<obj>` PUT to the `setpoint` SLOT works ([B825]); this block asks the follow-up — can an
> oBIX client instead address the struct's `value` CHILD directly (`…/Cuarto1/setpoint/value`) with a bare `<real>`?
> Settles the routability from code and states the residual requires-execution confirm. REMITTANCE — [B825] (the
> top-slot-replacement + synchronous propagation mechanism), [B823] §823.2 (the wrapped-PUT live result + the six forms),
> [B822] (the additive alternative), [B816] (write-path), [B509] (oBIX transport).
>
> **Sources**: FUENTE 3 (`[CERT]`, decompiled, read at the enclosing method this session) —
> `organized/obixDriver/obixDriver-rt/vineflower/{com/tridium/obix/server/{BStatusValueAgent,BStationLobbyAgent}.java,
> javax/baja/obix/io/ObixEncoder.java, javax/baja/obix/driver/BObixServer.java, com/tridium/obix/util/ObixUtils.java}`.
> FUENTE 1 (`[CERT-live]`) — the viewer GET showing `setpoint` as a leaf `<real>` (`sources/probes/2026-09-06-viewer-obix-setpoint-live-record.md` §3).
> `[CERT]` = code · `[CERT-live]` = the live GET shape · `[INFER]` = the untested write-acceptance.

---

## 826.1 — The child is NEVER ADVERTISED: the `BStatusValue` agent collapses the struct to a leaf `<real>` `[CERT]`
`BStatusNumeric` is a `BComplex`, so the generic encoder (`ObixEncoder.getKids`, `ObixEncoder.java:512-529`) WOULD walk
its slots and emit a `<real name="value" href="…/value">` child. But the `@AgentOn(baja:StatusValue)` agent short-circuits
that: `BStatusValueAgent.encode()` (`BStatusValueAgent.java:29-35`) calls `super.encode(...)` then
`ObixUtils.encode(out, this.val, sv.getValueValue(), target)` — it renders the value INLINE as a single leaf `<real>`
(the `val` attribute) and **never calls `getKids`**. So the GET of `setpoint` is a leaf
(`<real val="3.0" is="/obix/def/baja:StatusNumeric" unit="…celsius"/>`, viewer record §3 `[CERT-live]`) — **no `/value`
child element, no child `href`**. A conformant oBIX client navigating the advertised tree will never reach `/setpoint/value`.
`[CERT]`

## 826.2 — But the child IS RESOLVABLE: the config resolver does a VERBATIM path→`slot:` translation `[CERT]`
The oBIX URI resolution does NOT consult the agent's leaf rendering — it string-translates the href into a Baja `slot:`
ORD and lets the standard slot resolution walk the tree:
- `BObixServer.resolve(href, cx)` (`BObixServer.java:197-200`) → `getLobby().resolve(ObixUtils.resource('/'+servletName, href), cx)`.
- `BStationLobbyAgent` builds `"station:|" + decode(uri)` (`:38`); `decode` → `decodeSlotPath(uri)` which, if the path has
  no `":"`, simply **prepends `"slot:"`** (`BStationLobbyAgent.java:61`) — a purely lexical transform, no component-tree
  or agent validation. So `/config/Services/DashboardService/Cuarto1/setpoint/value` →
  `station:|slot:/Services/DashboardService/Cuarto1/setpoint/value`, passed through VERBATIM including the trailing
  `/value` segment. `[CERT]`
- That `station:|slot:` ORD then resolves through the ordinary Baja slot tree: `Cuarto1` (`BRoomPanel`) → `setpoint`
  (`BStatusNumeric`) → `value` (a frozen `BDouble` property on every `BStatusNumeric`). `value` IS a real slot, so the
  path resolves to a valid `OrdTarget` on the `value` property. `[CERT — the resolver is agent-agnostic; value is a known BStatusNumeric slot]`

**So the child ORD is NOT advertised (§826.1) but IS structurally routable (§826.2)** — the same advertised-vs-reachable
split as the wrapped-`<obj>` PUT itself ([B823] §823.2: `writable` never advertised, yet a hand-crafted PUT writes).

**LIVE-CONFIRMED (viewer GET, no PUT — record §8, `b4e6d8a4f`; closes B826-G1) `[CERT-live]`:** `GET
/obix/config/Services/DashboardService/Cuarto1/setpoint/value` → `200 <real val="3.0" … writable="true"/>`, and
`GET /obix/config/Programacion/ColdRoom_1/setpoint/value` → `200 <real val="3.0" … writable="true"/>`. So the child ORD is
not only resolvable but **SERVED and advertised `writable="true"`** on BOTH the RoomPanel and the ColdRoom — even though
the PARENT `setpoint` GET carries no `writable` (the leaf-collapse, §826.1). The child is a `BSimple` (`BDouble`), so
`ObixUtils.encode:241-243` DOES advertise `writable` once the path is resolved directly. This is the read-only reasoning
(§826.1-2) confirmed empirically — the reasoning predicted it exactly.

## 826.3 — Served + writable (LIVE), the SIMPLER SAFER write — only the PUT-propagation is the residual `[CERT-live + INFER]`
Addressing `value` directly changes the sink from a `BComplex` to a `BSimple`, which removes both wrapped-PUT hazards
(the child is now [CERT-live] served + `writable="true"`, §826.2):
- **No "Cannot translate"**: `value` is a `BDouble` = `BSimple`, so a bare `<real val="2.5"/>` PUT decodes via the SIMPLE
  path (`ObixDecoder` `sink.asSimple().decodeFromString`, [B822] §822.1) — no `<obj>` wrapper needed.
- **No silent-zero**: the silent-zero footgun ([B823] §823.2, [B825] §825.5) comes from an attr-only `<obj>` whose missing
  `value` child defaults to 0.0; a bare `<real>` to `value` carries the number directly, nothing to default.
- **It still PROPAGATES**: a write to the live `setpoint.value` is a nested-child set on the mounted `BStatusNumeric`,
  which bubbles `ComplexSlotMap.modified() → parent.modified(setpointProp) → knobs.propagate()` and fires the outgoing
  `BLink` synchronously ([B825] §825.3 — the bubbling path this block's write would actually exercise, vs the top-slot
  replacement the wrapped PUT uses). So it reaches control the same way.
**PUT + propagation now LIVE-CONFIRMED (Cristian-authorized, record §9, `f99f2e45b`; closes B826-G2) `[CERT-live]`:** a bare
`<real val="2.5"/>` PUT to `…/Cuarto1/setpoint/value` → `200` echoing the value, and `ColdRoom_1/setpoint` = `2.5` after
**~1.5 s** (restored to `3.0`). So (a) `serviceWrite` ACCEPTS the bare-`<real>` write to the child, and (b) it PROPAGATES
through the link to control — proving the nested-child bubbling path of [B825] §825.3 END-TO-END (the ~1.5 s is well
inside the reader's poll settle, consistent with the synchronous propagation of [B825] §825.3). **So the child-`value`
bare-`<real>` is now the PREFERRED write form** (simple decode, no `<obj>` wrapper, no silent-zero hazard, one authorized
live round-trip proven); the [B825] wrapped-`<obj>`-to-the-parent-slot is the PROVEN FALLBACK (still needed for any client
that cannot address the child ORD). Both reach control; the child form is cleaner.

## 826.4 — Doctrine + kit implication `[INFER, grounded]`
- **Doctrine**: *an oBIX config path is resolved by a verbatim `slot:` translation, so a slot NOT advertised in the GET
  (a `BStatusValue`'s `value` child, collapsed by its agent) may still be REACHABLE by a hand-crafted ORD — advertised ≠
  the full reachable surface.* This generalizes the [B823] §823.2 "writable-absent ≠ read-only" one level: "not-shown ≠
  not-addressable." `[ev: corpus B826]`
- **For the write-server (C9 S12 / [B822])**: the child-`value` bare-`<real>` form is now the PREFERRED write (LIVE-proven
  end-to-end §826.3 — simple decode, no silent-zero rule to enforce); the [B825] wrapped-`<obj>`-to-the-slot is the proven
  FALLBACK. The write-server's NUM builder (which already emits a bare `<real>`, viewer record §4) can target
  `…/setpoint/value` directly — no new wrapped-obj type needed. `[ev: corpus B825, B826]`
- Fully closes [B825]-G2: routability reasoned from code (§826.1-2) then confirmed live end-to-end (GET §826.2 + PUT §826.3).

## Self-verify
| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | The `BStatusValue` agent renders the struct as a leaf `<real>` (value inline) and does NOT call `getKids`, so `/value` is not advertised | `[CERT]` | `BStatusValueAgent.java:29-35`; `ObixEncoder.getKids:512-529`; viewer GET §3 leaf `<real>` |
| 2 | The config resolver prepends `slot:` verbatim (no agent/tree validation), so `…/setpoint/value` → `station:\|slot:…/setpoint/value` | `[CERT]` | `BObixServer.java:197-200`; `BStationLobbyAgent.java:38,61` |
| 3 | The child `…/setpoint/value` is SERVED + advertised `writable="true"` (a `BSimple` `BDouble` leaf) on BOTH RoomPanel and ColdRoom, though the parent carries no `writable` | `[CERT-live]` | viewer record §8 (`b4e6d8a4f`): two `200 <real … writable="true"/>` GETs |
| 4 | A child-`value` write would propagate the link via nested-child bubbling (not the top-slot path) | `[CERT]` | [B825] §825.3; `ComplexSlotMap.java:1468,1518` |
| 5 | A bare-`<real>` PUT to the child is ACCEPTED (200) and PROPAGATES to control (~1.5 s) — [B825] §825.3 nested-child bubbling proven END-TO-END | `[CERT-live]` | viewer record §9 (`f99f2e45b`): PUT 2.5 → ColdRoom_1 2.5, restored 3.0 |

**Tally**: 3 `[CERT]` · 2 `[CERT-live]` (the child GET §826.2 and the PUT+propagation §826.3). The two load-bearing code
paths (the agent leaf-collapse; the verbatim `slot:` resolver) were read at the enclosing method, and BOTH the read-only
prediction (child served + `writable="true"`) AND the write+propagation were then confirmed live end-to-end. Dedupe: the
wrapped-PUT mechanism + the propagation are REMITTANCE ([B823]/[B825]); this block adds ONLY the child-ORD routability
(reasoned from code, then live-proven) + the preferred-write finding (bare-`<real>` to the child, no silent-zero).

## Connections
- **[B825]** (the propagation mechanism — §826.3's bubbling path IS [B825] §825.3, now proven live end-to-end by B826-G2),
  **[B823]** §823.2 (the wrapped-PUT live result + the advertised-vs-reachable split this generalizes), **[B822]** (the
  simple-decode path the bare-`<real>` to `value` uses; the additive alternative), **[B816]** (write-path), **[B509]**
  (oBIX transport / the config lobby). Kit: the [B823] slot-type doctrine (`types/logic-authoring.md`) — the child-`value`
  bare-`<real>` is the PREFERRED external write for a `BStatusNumeric` (live-proven, no silent-zero), the wrapped-`<obj>`
  the fallback; and the general rule "an unadvertised child slot may still be ORD-reachable — confirm by GET".

## Open gaps
- **B826-G1** — CLOSED `[CERT-live]` (viewer GET, record §8 `b4e6d8a4f`): `…/setpoint/value` is served + `writable="true"`
  on both the RoomPanel and the ColdRoom.
- **B826-G2** — CLOSED `[CERT-live]` (Cristian-authorized PUT, record §9 `f99f2e45b`): a bare `<real val="2.5"/>` PUT to
  the child writes AND propagates to control in ~1.5 s. Both gaps closed; the block is complete end-to-end. (No open
  requires-execution residue.)
