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
| mode / HOA | today `double` 0/1/2 written as `<real>`; an ENUM needs a **range facet** for oBIX to decode | `SUMMARY\|OPERATOR` | `<real val="2"/>` (double) or `<enum val="off"/>` WITH a `range` facet (ObixDecoder enum branch reads `cx.getFacets().get("range")`) | as above | a bare enum with NO range facet → oBIX cannot decode it |
| button / command | an OPERATOR `@NiagaraAction` | `Flags.OPERATOR` | oBIX `<op>` — POST → `BComponent.invoke` under `OPERATOR_INVOKE`, arg from `<real>`/`<bool>` `[ev: corpus B822]` | the Niagara invoke event (attributed) | a `HIDDEN` action (0 oBIX exposure) or a boolean "pulse" slot |

**The rule:** a slot that EXTERNAL clients write is **either a SIMPLE value or has an ACTION — never a bare complex
property.** A bare complex (`BStatusNumeric`/`BStatusBoolean`/`BStatusEnum`) either rejects the write ("Cannot
translate") or, via the wrapped-`obj` shorthand, silently writes a DEFAULT (the live silent-zero: a setpoint set to
0.0 on a 200 OK). If the status MUST be displayed (so the slot has to be complex), expose an `OPERATOR` action that
writes it, or accept the exact wrapped-`obj` contract in the client — never leave a bare complex OPERATOR property as
the write target. `[ev: corpus B823]` `[ev: retro obix-statusnumeric-wrapped-put]`

**Cleaner alternative `[INFER]`, pending a GET (B826-G1):** the child ORD `…/setpoint/value` is NOT advertised (the
agent collapses the struct to a leaf `<real>`) but IS structurally resolvable (`BStationLobbyAgent.decodeSlotPath`);
IF the façade serves it, a bare `<real>` to `/setpoint/value` would be a `BSimple` write — SIMPLER, with NO silent-zero
risk — and still propagates via the nested-child bubbling path. It stays `[INFER]` until one read-only GET confirms the
child ORD is served; until then the wrapped-`obj`-to-slot form (B825, live-proven) remains the RECOMMENDED write.
`[ev: corpus B826]`

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
