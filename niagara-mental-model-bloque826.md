# B826 · Is a `/setpoint/value` CHILD ORD routable over oBIX? — closes [B825]-G2 by read-only reasoning: the `BStatusValue` agent COLLAPSES the struct to a leaf `<real>` so the child is NEVER advertised, but the config-space resolver builds a `station:\|slot:…/setpoint/value` ORD by VERBATIM path translation, so a hand-crafted child PUT is resolvable — a simpler (bare-`<real>`, no silent-zero) write than the wrapped-`<obj>`, pending a live probe `[CERT for the code paths; INFER for the untested write]`

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

## 826.3 — IF routable, it is the SIMPLER, SAFER write — the residual `[INFER, code-grounded]`
Addressing `value` directly changes the sink from a `BComplex` to a `BSimple`, which removes both wrapped-PUT hazards:
- **No "Cannot translate"**: `value` is a `BDouble` = `BSimple`, so a bare `<real val="2.5"/>` PUT decodes via the SIMPLE
  path (`ObixDecoder` `sink.asSimple().decodeFromString`, [B822] §822.1) — no `<obj>` wrapper needed.
- **No silent-zero**: the silent-zero footgun ([B823] §823.2, [B825] §825.5) comes from an attr-only `<obj>` whose missing
  `value` child defaults to 0.0; a bare `<real>` to `value` carries the number directly, nothing to default.
- **It still PROPAGATES**: a write to the live `setpoint.value` is a nested-child set on the mounted `BStatusNumeric`,
  which bubbles `ComplexSlotMap.modified() → parent.modified(setpointProp) → knobs.propagate()` and fires the outgoing
  `BLink` synchronously ([B825] §825.3 — the bubbling path this block's write would actually exercise, vs the top-slot
  replacement the wrapped PUT uses). So it reaches control the same way.
**Residual [INFER / requires-execution]**: whether `serviceWrite` ACCEPTS a write to a `value` child that the agent
collapsed — i.e. whether `value` advertises/permits `writable` once resolved, and whether `BObixServer` serves the
collapsed child at all — is NOT verified in code and NOT probed (per the lead, no probe). The resolver builds the ORD
(§826.2); the serve/write is the open confirm. Until probed, the RECOMMENDED path stays the [B825] wrapped-`<obj>` PUT to
the slot (live-proven) or the [B823] servlet — the child-`value` ORD is a cleaner THEORETICAL alternative, not yet a
proven one.

## 826.4 — Doctrine + kit implication `[INFER, grounded]`
- **Doctrine**: *an oBIX config path is resolved by a verbatim `slot:` translation, so a slot NOT advertised in the GET
  (a `BStatusValue`'s `value` child, collapsed by its agent) may still be REACHABLE by a hand-crafted ORD — advertised ≠
  the full reachable surface.* This generalizes the [B823] §823.2 "writable-absent ≠ read-only" one level: "not-shown ≠
  not-addressable." `[ev: corpus B826]`
- **For the write-server (C9 S12 / [B822])**: the wrapped-`<obj>`-to-the-slot form ([B825]) is the LIVE-PROVEN choice;
  the child-`value` bare-`<real>` form is a candidate that, IF a probe confirms the server serves it, is simpler and
  drops the silent-zero rule — worth one read-only GET of `…/setpoint/value` on a test room to settle. `[ev: corpus B825]`
- Closes [B825]-G2's routability question at the code level; the write-acceptance is the narrowed residue (§826.5).

## Self-verify
| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | The `BStatusValue` agent renders the struct as a leaf `<real>` (value inline) and does NOT call `getKids`, so `/value` is not advertised | `[CERT]` | `BStatusValueAgent.java:29-35`; `ObixEncoder.getKids:512-529`; viewer GET §3 leaf `<real>` |
| 2 | The config resolver prepends `slot:` verbatim (no agent/tree validation), so `…/setpoint/value` → `station:\|slot:…/setpoint/value` | `[CERT]` | `BObixServer.java:197-200`; `BStationLobbyAgent.java:38,61` |
| 3 | `value` is a `BDouble` (`BSimple`) frozen slot on every `BStatusNumeric` → the resolved child is a simple, bare-`<real>`-writable target | `[CERT for the type]`+`[INFER for the write-serve]` | Baja `BStatusNumeric` value slot; [B822] simple-decode path |
| 4 | A child-`value` write would propagate the link via nested-child bubbling (not the top-slot path) | `[CERT]` | [B825] §825.3; `ComplexSlotMap.java:1468,1518` |
| 5 | Whether `serviceWrite` serves/permits the collapsed child is UNVERIFIED (no probe) | `[INFER — requires-execution]` | not in code; [B825]-G2 residue |

**Tally**: 2 `[CERT]` · 2 `[CERT]+[INFER]` · 1 `[INFER]`. The two load-bearing code paths (the agent leaf-collapse; the
verbatim `slot:` resolver) were read at the enclosing method this session. Dedupe: the wrapped-PUT mechanism + the
propagation are REMITTANCE ([B823]/[B825]); this block adds ONLY the child-ORD routability reasoning (advertised-no /
resolvable-yes) + the simpler-write implication + the narrowed residue.

## Connections
- **[B825]** (the propagation mechanism — §826.3's bubbling path is [B825] §825.3; closes its G2), **[B823]** §823.2 (the
  wrapded-PUT live result + the advertised-vs-reachable split this generalizes), **[B822]** (the simple-decode path a
  bare-`<real>` to `value` would use; the additive alternative), **[B816]** (write-path), **[B509]** (oBIX transport /
  the config lobby). Kit: a one-line note in the [B823] slot-type doctrine (`types/logic-authoring.md`) — "an unadvertised
  child slot may still be ORD-reachable; confirm by GET before relying on it".

## Open gaps
- **B826-G1** (requires-execution, read-only): a single GET of `/obix/config/Services/DashboardService/Cuarto1/setpoint/value`
  on the live station settles whether `BObixServer` serves the collapsed child (and whether it shows `writable="true"`);
  a subsequent authorized bare-`<real>` PUT would confirm the simpler write. Inherits [B825]-G2's requires-execution
  status; NO probe done here (read-only reasoning only, per scope).
