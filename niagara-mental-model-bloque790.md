# B790 · The minimal correct N4 module — the smallest exemplar-grounded skeleton (and the `scaffold-module.sh` fixture)

> **Scope**: assembles the SMALLEST correct N4 module skeleton from the campaign6 exemplar findings — every file
> cited to the block/exemplar that proves its shape. A synthesis (no new decompile): it composes B784 (module.xml),
> B780 (palette/lexicon), B776/B755 (OPERATOR property + HIDDEN action), B775/B787 (Clock ticket lifecycle), B18/B777
> (signing) into one buildable, gate-passing template. Purpose: the `types/logic.md` "minimal module" section AND the
> definable fixture for a future `scaffold-module.sh` (D1 was cut from campaign6 for lack of a biting test — this
> skeleton IS that test's RED→GREEN input). Focus: `own-modules-vs-exemplars` (synthesis addendum). Kit dest:
> `types/logic.md` + a `toolbelt/scaffold-module.sh` spec.
>
> **Sources**: FUENTE 1 the campaign6 blocks (B774-B785 + B787/B788/B789). FUENTE 3 verified exemplars this session:
> `alarm-rt/module.xml`, `control-rt/module.palette`, `CompPan-rt` (`BCompressorControl` OPERATOR prop + Clock
> ticket), `ColdRoomPan-rt` (`BDefrostController` HIDDEN action), `saml-rt/META-INF` (signing). READ-ONLY. English.

---

## 790.1 — The minimal skeleton, file by file (each cited to its proof) `[CERT unless noted]`

| File | Minimal content | Exemplar / proof |
|---|---|---|
| `<MOD>-rt/module.xml` (header) | `<module name="<MOD>-rt" bajaVersion="0" vendor="<V>" vendorVersion="X.Y.Z.B" description=… preferredSymbol="<s>" moduleName="<MOD>" runtimeProfile="rt">` | B784 · alarm-rt/module.xml:2 (attribute roster; bajaVersion const "0") |
| `<MOD>-rt/module.xml` (deps) | `<dependency name="baja" vendor="Tridium" vendorVersion="X.Y.0"/>` — the 3-part FLOOR, not the 4-part build stamp | B784 · alarm-rt/module.xml:9 |
| `<MOD>-rt/module.xml` (types) | one `<type class="…B<Comp>" name="<Comp>"/>` per exposed `@NiagaraType`; `<moduleParts>` lists sibling profiles if any | B784 · B780 |
| gradle layout | build from the GRADLE ROOT `:<MOD>-rt:slotomatic :<MOD>-rt:jar`; module version in `defaultModuleVersion` in `build.gradle.kts` `vendor{}` | build-verify.md (kit) · B784 |
| `<MOD>-rt/module.palette` | one draggable `<p>` per component under a plural folder: `<p m="b=baja" t="b:UnrestrictedFolder"><p n="<Comp>" m="x=<MOD>" t="x:<Comp>"/></p>` — NOT an empty scaffold (B5) | B780 · control-rt/module.palette |
| `<MOD>-rt/<MOD>-rt.lexicon` | a key per type + user-facing slot; PREFIX (`<Comp>.<slot>`) to dodge collisions; NOT empty (else camelCase) | B780/B759 · CompPan-rt.lexicon (56 keys, positive) |
| `<MOD>-rt/src/…/B<Comp>.java` | `public class B<Comp> extends BComponent` with ONE OPERATOR-writable property: `@NiagaraProperty(name="<x>", …, flags=Flags.SUMMARY \| Flags.OPERATOR)` | B776/B755 · `BCompressorControl.java:108` (`Flags.SUMMARY \| Flags.OPERATOR`) |
| … one engine action | ONE HIDDEN engine callback: `@NiagaraAction(name="<tick>Expired", flags=Flags.HIDDEN)` (a user command action would carry `Flags.OPERATOR` instead) | B776 · `BDefrostController.java:128` (`flags=Flags.HIDDEN`) |
| … one timer | a `private Clock.Ticket ticket;` armed in BOTH `atSteadyState()` and `started()` (guarded by `Sys.atSteadyState()`), CANCELLED in `stopped()`; re-armed in `changed()` if the interval is configurable | B775/B787/B729 · `BCompressorControl.java:1752,1786,1799` |
| built jar | Java-8 (bytecode major 52) + slotomatic + SIGNED (`META-INF/NIAGARA4.RSA` + `.SF` + sealed manifest) | B18/B777 · saml-rt/META-INF/NIAGARA4.RSA · kit build-verify.md |

## 790.2 — The assembled skeleton (annotated) `[INFER, composed from the cited shapes]`
```
<MOD>/
  build.gradle.kts        # vendor{ defaultModuleVersion("X.Y.Z") }
  <MOD>-rt/
    module.xml            # header roster + 3-part dep floor + <type> per component   [B784]
    module.palette        # one <p> per component, plural folder, m="x=<MOD>" once      [B780]
    <MOD>-rt.lexicon      # <Comp>=… + <Comp>.<slot>=… prefixed keys                    [B780]
    src/com/<v>/<MOD>/B<Comp>.java
        public class B<Comp> extends BComponent {
          @NiagaraProperty(name="setpoint", type="double", flags=Flags.SUMMARY|Flags.OPERATOR)  // operator-writable  [B776]
          @NiagaraAction(name="tickExpired", flags=Flags.HIDDEN)                                 // engine callback    [B776]
          private Clock.Ticket ticket;
          public void started(){ super.started(); if(Sys.atSteadyState()) arm(); }               // B729
          public void atSteadyState(){ arm(); }                                                  // B729
          public void stopped(){ if(ticket!=null){ticket.cancel();ticket=null;} super.stopped(); } // B775/B787
          public void changed(Property p, Context cx){ if(p==interval && isRunning()) arm(); }    // re-arm  [B775]
          private void arm(){ if(ticket!=null)ticket.cancel(); ticket=Clock.schedulePeriodically(this,getInterval(),tickExpired,null); }
        }
```
This skeleton PASSES the verify gate (major-52 + signed + types resolve + non-empty palette) AND the campaign6 biting
checks (lexicon has no dup keys; palette non-empty; the Clock.Ticket has a `stopped()`-cancel) — i.e. it is the
GREEN state for those checks by construction.

## 790.3 — Kit implication `[INFER, grounded]`
- **`types/logic.md` "minimal module" section**: the §790.1 table + the §790.2 skeleton as the copy-start for a new
  logic module — every element carries its `[ev: corpus B<n>]`.
- **`toolbelt/scaffold-module.sh` fixture spec** (D1 was cut from campaign6 for lack of a biting test): the biting test
  IS this skeleton. Spec: `scaffold-module.sh <MOD> <vendor> <symbol>` emits exactly §790.2; the RED→GREEN fixture QA
  writes = "run scaffold-module.sh, then `verify-module.sh` + the B787/B788 biting checks (ticket-without-stopped-cancel,
  lexicon-dup-keys, empty-palette) must ALL pass on the emitted skeleton, and a mutation of the template that drops the
  `stopped()`-cancel / empties the palette / dups a lexicon key must FAIL them." That makes scaffold-module.sh
  buildable AND test-anchored — the two things its campaign6 cut lacked.

## Self-verify

| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | module.xml header roster + 3-part dep FLOOR (not the 4-part build stamp) | [CERT] | B784 · alarm-rt/module.xml:2,9 |
| 2 | Non-empty palette = one `<p>` per component (vs the B5 empty-scaffold footgun) | [CERT] | B780/B788 · control-rt/module.palette |
| 3 | Prefixed non-empty lexicon (one key per type+slot) | [CERT] | B780/B759 · CompPan-rt.lexicon |
| 4 | One OPERATOR-writable property = `Flags.SUMMARY \| Flags.OPERATOR` | [CERT] | B776/B755 · BCompressorControl.java:108 |
| 5 | One HIDDEN engine action = `@NiagaraAction(flags=Flags.HIDDEN)` | [CERT] | B776 · BDefrostController.java:128 |
| 6 | Clock.Ticket armed in started()+atSteadyState(), cancelled in stopped() | [CERT] | B775/B787/B729 · BCompressorControl.java:1752,1786,1799 |
| 7 | Signed jar (NIAGARA4.RSA/SF + sealed) is mandatory | [CERT] | B18/B777 · saml-rt/META-INF/NIAGARA4.RSA |
| 8 | The skeleton is GREEN for verify-gate + campaign6 biting checks by construction | [INFER] | composition of rows 1-7 |

**Tally**: 7 [CERT], 1 [INFER]. No unmarked claims. Each shape grep-verified this session at its exemplar.

## Connections
- **B784** (module.xml conventions), **B780** (palette/lexicon), **B776/B755** (action/flags), **B775/B787/B729**
  (timer lifecycle), **B18/B777** (signing), **B788/B789** (the biting checks this skeleton passes by construction),
  **B760** (punch-list). The Author-side-SPI idiom (B778/B782/B785) extends this skeleton when a module needs a
  service/scheme/provider.

## Open gaps
- **B790-G1** (requires-execution): actually run `scaffold-module.sh` (once written) + build+deploy the emitted
  skeleton to a station to prove it boots — the read-only skeleton is gate-green by construction but unproven live.

## Kit implication (→ `types/logic.md` "minimal module" + a `toolbelt/scaffold-module.sh` spec)
Add the §790.1 table + §790.2 skeleton as the "minimal module" copy-start (each element `[ev: corpus B<n>]`), and
specify `scaffold-module.sh` with THIS skeleton as its output and the "gate + biting-checks pass on the emitted
skeleton, mutations fail" RED→GREEN fixture — resolving the two gaps (buildable + test-anchored) that cut D1 from
campaign6.
