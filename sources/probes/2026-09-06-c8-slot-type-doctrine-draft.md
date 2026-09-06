# C8 slot-type doctrine — which slot type for an EXTERNALLY-written value (apply-ready)

> For the apply worker (lands via wave-2 **PR15** — added as a PR15 block in the PR13/14/15 draft so it applies THIS
> campaign). Target: `types/logic-authoring.md` NEW `##` section "Slot types for externally written values" + ONE
> line in `types/dashboard.md`. Grounded in the LIVE oBIX probe (B823 §823.2, 2026-09-06 `[CERT-live]`; verbatim
> record `sources/probes/2026-09-06-viewer-obix-setpoint-live-record.md`), B822 (the
> OPERATOR-action path), B816 (write overlap). The enforcing lint is C9 seed **S19** (`ext-writable-shape`).
>
> **Grep-before (K6):** `grep -niE 'slot type.*external|externally written|wrapped.obj|silent.zero' types/logic-authoring.md`
> → 0 hits — NEW section.

=== BEGIN types/logic-authoring.md §"Slot types for externally written values" ===

## Slot types for externally written values `[ev: corpus B823]`
When a value is written by an EXTERNAL client (oBIX/write-server, the -ux servlet, a fox/BajaScript client), the slot
TYPE decides whether the write is even possible and whether it lands safely. Pick by value class:

| Value class | Recommended slot | Flags | How it is written externally | Audit path | Anti-pattern |
|---|---|---|---|---|---|
| numeric setpoint / config | plain `double` (if oBIX writes it) — or `BStatusNumeric` ONLY when the status must display | `SUMMARY\|OPERATOR` | `double`: oBIX bare `<real val="..">`. `BStatusNumeric`: the WRAPPED body `<obj is="…:StatusNumeric"><real name="value" val=".."/></obj>` (LIVE-verified) — NEVER attr-only `<obj … val>` (200 but writes 0.0); OR the servlet `POST /api/setpoint`; OR an OPERATOR action | servlet `auditLog` / write-server audit / a Niagara event when via an action | a bare `BStatusNumeric` written by external clients → the silent-zero footgun `[ev: retro obix-statusnumeric-wrapped-put]` |
| timing / delay | `BRelTime` | `SUMMARY\|OPERATOR` | oBIX `<reltime val="PT..S"/>` | as above | a `double` seconds field that skips the reltime unit `[ev: corpus B823]` |
| switch / on-off | `boolean` | `SUMMARY\|OPERATOR` | oBIX `<bool val="true">` | as above | a `BStatusBoolean` (complex) written bare → "Cannot translate" `[ev: corpus B823]` |
| mode / HOA | today `double` 0/1/2 written as `<real>`; for future modules a **FROZEN enum** (`BFrozenEnum` via `@NiagaraEnum`/`@Range`, e.g. a `BHoaMode` auto/hand/off = 0/1/2) carries its range INTRINSICALLY — no explicit facet needed | `SUMMARY\|OPERATOR` | `double`: `<real val="2"/>`. FROZEN enum: renders `<enum val="hand" display=… range=…/>` and decodes `<enum val="hand"/>` with NO explicit `BFacets.RANGE` — the encoder (`ObixUtils:358`) and decoder (`ObixDecoder:184/245/333`, `setFromVal`) fall back to the value's `getRange()`. A DYNAMIC enum is the only case that needs an explicit range facet. The `@Range` tags need `module.lexicon` keys (SP6 known set). `[ev: corpus B828]` | as above | a `double`→enum switch is a LOSSY retype (OUTAGE) → future modules only; existing RoomPanel modes stay `double` 0/1/2 `[ev: corpus B828]` |
| button / command | an OPERATOR `@NiagaraAction` | `Flags.OPERATOR` | oBIX `<op>` — POST → `BComponent.invoke` under `OPERATOR_INVOKE`, arg from `<real>`/`<bool>` `[ev: corpus B822]` | the Niagara invoke event (attributed) | a `HIDDEN` action (0 oBIX exposure) or a boolean "pulse" slot |

**The rule:** a slot that EXTERNAL clients write is **either a SIMPLE value or has an ACTION — never a bare complex
property.** A bare complex (`BStatusNumeric`/`BStatusBoolean`/`BStatusEnum`) either rejects the write ("Cannot
translate") or, via the wrapped-`obj` shorthand, silently writes a DEFAULT (the live silent-zero: a setpoint set to
0.0 on a 200 OK). If the status MUST be displayed (so the slot has to be complex), expose an `OPERATOR` action that
writes it, or accept the exact wrapped-`obj` contract in the client — never leave a bare complex OPERATOR property as
the write target. `[ev: corpus B823]` `[ev: retro obix-statusnumeric-wrapped-put]`

**Cleaner alternative — now PREFERRED `[CERT-live]` (B826-G1/G2 CLOSED):** the child ORD `…/setpoint/value` is NOT
advertised (the agent collapses the struct to a leaf `<real>`) but IS structurally resolvable
(`BStationLobbyAgent.decodeSlotPath`) — and the façade DOES serve it: a GET returns `200 <real … writable="true"/>` on
BOTH the RoomPanel and the ColdRoom (B826-G1, record §8 `b4e6d8a4f`), and a bare `<real val="N"/>` PUT to it writes AND
propagates to control in ~1.5 s (B826-G2, record §9 `f99f2e45b`), via the nested-child bubbling path (B825 §825.3, now
live-proven end-to-end). So the child bare-`<real>` is a `BSimple` write with NO silent-zero hazard, and is the
PREFERRED external write for a `BStatusNumeric`; the wrapped-`obj`-to-parent-slot form (B825) is the proven FALLBACK.
Rule: **a complex property is writable externally through its child leaf ORD (bare `<real>`); the parent slot needs the
wrapped `<obj>` and carries the silent-zero hazard.** `[ev: corpus B826]` `[ev: corpus B825]`

**Propagates through links? YES, synchronously (mechanism settled by [Block 825]):** an external write that lands as a
TOP-SLOT REPLACEMENT (an oBIX wrapped `<obj>` PUT, the servlet, or fox — all decode into a detached copy then
`parent.set(slot, copy)`, `ObixUtils.java:543/:558`) fires the slot's outgoing links SYNCHRONOUSLY on the writing
thread (`SlotKnobs.propagate:31-46`, <1 ms). So a write to the façade SOURCE (`Cuarto1/setpoint`) propagates to the
control TARGET (`ColdRoom_1/setpoint`) in <1 ms — the read-back "settle" is the READER's poll cadence (~1 s a
control-slot poll / ~6 s the dashboard poller), NOT a propagation delay. Rule: an external write must land on the slot
the control READS (or its link SOURCE), or on an action — a write to a display-only mirror with no link, or to the
link-TARGET side (which the next source propagation overwrites, B816 §816.2), does not move the plant.
`[ev: corpus B823]` `[ev: corpus B825]` `[ev: retro obix-statusnumeric-wrapped-put]`

**Overlap caveat:** if the written slot is a link TARGET (driven BY a link, not a source), the external write is
EPHEMERAL — the next propagation overwrites it (B816 §816.2). Confirm write-source vs write-target before relying on
the write sticking. `[ev: corpus B816]`

=== END types/logic-authoring.md section ===

=== BEGIN types/dashboard.md one-line (add under the write-surface / servlet section) ===
- **A dashboard-written value's slot type is load-bearing:** a `double`/`BRelTime`/`boolean` is oBIX-writable directly; a `BStatusNumeric` needs the wrapped `<obj><real name="value">` body (never attr-only → silent 0.0) or the servlet/action path — see `types/logic-authoring.md` §"Slot types for externally written values". `[ev: corpus B823]` `[ev: retro obix-statusnumeric-wrapped-put]`
=== END types/dashboard.md line ===

## Apply notes
- Place the section VERBATIM; every row keeps its `[ev:]` token(s). Route it: name the new section in the
  `types/logic-authoring.md` audience header if it lists sub-topics.
- The enforcing lint is NOT built here — it is C9 seed **S19** (`ext-writable-shape`: WARN on an `OPERATOR` complex
  property with no writing action). This draft is doctrine only.
- The `obix-statusnumeric-wrapped-put` retro (`retros/2026-09-06-…`) is this doctrine's source; its token credits the
  fold.
