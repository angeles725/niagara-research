<!-- review-status: pending -->
<!-- Marker lifecycle: maintainer flips 'pending' → 'applied <date> · kit <sha>' (or 'dismissed') once folded; sweep-retros.sh reads this (METHODOLOGY §18). -->
# Retro — a decoder-path finding is not "closed" until the live probe: the oBIX wrapped-`obj` PUT of a `BStatusNumeric` · 2026-09-06

> §18 research retro from B823 (can the existing `BRoomPanel.setpoint`, a `BStatusNumeric`, be written over oBIX with
> no module change). PROPOSES only. The finding flipped between the code read and the live probe — the process lesson
> is the point.

## What we BELIEVED (before the probe) `[the starting hypothesis]`
The setpoint is effectively READ-ONLY over oBIX. The GET carries no `writable="true"` and no `<op>`, and the
write-server's earlier bare-`<real>` PUT returned "Cannot translate". First reading: oBIX cannot write it; the only
paths are the module servlet (channel 3) or an additive code slot (B822).

## What the CODE said (the decoder trace) `[CERT — static]`
Reading `ObixDecoder`/`ObixUtils`/the agents refined it to "closed to CONFORMANT clients, with an unverified escape
hatch": a bare `<real>` into a `BComplex` sink throws "Cannot translate" (`ObixDecoder.java:197/:346`) and `writable`
is advertised only for a `BSimple` (`ObixUtils.encode:241-243`), so no standard client attempts it — BUT the
`name=="obj"` branch (`:200-216`) walks children, and `setFromVal` does `((BStatusNumeric)cpx).setValue(BDouble.decode)`
(`:569`). So a hand-crafted `<obj is="…:StatusNumeric"><real name="value" val/></obj>` SHOULD reach `parent.set()`.
Whether the server accepted it was left `[INFER]` — a decode-level possibility, not a proven write.

## What the LIVE PROBE showed (Cristian-authorized, read-only first, Cuarto 1) `[CERT-live]`
The wrapped body WRITES and REACHES CONTROL: `<obj is="/obix/def/baja:StatusNumeric"><real name="value" val="2.5"/></obj>`
→ `200 OK`, value `2.5 {ok}`, held 80+ s, and it PROPAGATED through the live panel→control link to
`Programacion/ColdRoom_1/setpoint` (both `2.5`, Supabase `latest` `2.5`). Three things the code alone did not give:
1. **`writable` absent ≠ read-only.** The GET never advertised `writable="true"`, yet the wrapped PUT wrote. The
   `writable` attribute governs CONFORMANT clients, not what a hand-crafted PUT can do. (Refines [B509]'s reading.)
2. **The attr-only `<obj>` is a SILENT-ZERO footgun.** `<obj is="…:StatusNumeric" val="2.5"/>` (the value as an
   ATTRIBUTE on the obj, no `value` CHILD) returns `200 OK` but writes `0.0` — the missing child defaults the property.
   A write that looks successful and sets the setpoint to zero. Only the exact `value`-child body is safe.
3. **The link propagation takes a SETTLE.** A first control read moments after the PUT showed `3.0` (link not yet
   executed) → a premature "did not propagate" report that a re-read after a settle corrected to `2.5`. The
   propagation-timing mechanism (struct-child mutation vs slot replacement) is **pending [B825]**.

## Proven Lessons
1. **A decoder-path finding is a HYPOTHESIS until the live probe.** "The decoder can set it" (`setFromVal`) was
   correct but incomplete — it did not tell us `writable`-absent still accepts the write, nor that the attr-only shape
   silently zeroes. A `[CERT]` static trace + an `[INFER]` on the server-accept is not "closed"; the read-only-first
   live probe is what closes it. Mark such gaps requires-execution and DON'T rank the channel until probed.
2. **An attribute-only `<obj>` is a footgun class:** a partial oBIX body that the server accepts (200) but decodes to
   a default is worse than a rejection — it writes a wrong value silently. Any oBIX-write doctrine MUST require the
   exact typed child, never the shorthand attribute form, and any tool that emits such a PUT must be tested against
   the silent-zero case.
3. **A single early read is NOT evidence of no-effect — re-read after a settle before concluding.** This session
   produced TWO contradictory live reports within minutes: "the write moved only the display mirror, control stayed
   3.0" then, on a clean re-test, "it propagated, both 2.5". The first read was taken before the panel→control link
   executed. For any write whose effect crosses a link/propagation, the read-back must wait a settle; a premature read
   is a false negative that nearly shipped a wrong conclusion. (Pairs with lesson 1 — the live probe closes it, but
   only a CORRECTLY-TIMED live probe.)
4. **Read-only-first + one test room + explicit user authorization** is the right shape for a live protocol probe —
   the GET and the six PUT forms were exercised before anything load-bearing, and only after Cristian authorized it.

## Proposed kit deltas (propose-never-apply)
| Δ | Delta | Target file / § | Token |
|---|---|---|---|
| Δ1 | "a slot external clients write is either simple or has an action; never a bare complex property" + the wrapped-obj contract + the silent-zero warning | `types/logic-authoring.md` §"Slot types for externally written values" | `[ev: retro obix-statusnumeric-wrapped-put]` |
| Δ2 | the value-class → slot-type table (setpoint/timing/switch/mode/button) | `types/logic-authoring.md` + one line `types/dashboard.md` | `[ev: corpus B823]` `[ev: corpus B822]` |
| Δ3 | `ext-writable-shape` lint (WARN on an OPERATOR complex property with no action) | C9 seed **S19** | `[ev: retro obix-statusnumeric-wrapped-put]` |

## Self-verify
| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | Wrapped `<obj><real name="value">` PUT writes a BStatusNumeric live (200, 2.5{ok}, 80+s) | `[CERT-live]` | B823 §823.2 probe 2026-09-06 |
| 2 | attr-only `<obj … val>` → 200 but silent 0.0 | `[CERT-live]` | same probe |
| 3 | Decoder path predicts it (`setFromVal:569`) but not the writable-absent/silent-zero facts | `[CERT]` | `ObixDecoder.java:200-216,569` |
| 4 | writable-absent ≠ read-only (corrects the code-only reading + B509) | `[CERT-live]` | GET had no writable=, PUT still wrote |

**Tally:** `[CERT-live]` ×3 · `[CERT]` ×1. The process lesson (decode-finding needs the live probe) is the fold target.

## Connections
- [B823] (the channel enumeration this closes channel 1 of), [B822] (the additive alternative), [B816] (write-path
  overlap — does the write STICK vs a link overwrite), [B509] (oBIX transport — the `writable` reading corrected),
  [B800] §800.8 (the retype OUTAGE that keeps this a no-retype question).
