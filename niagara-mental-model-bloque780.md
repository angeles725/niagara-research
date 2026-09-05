# B780 · Per-exemplar palette / lexicon / @AgentOn conventions — copy-ready authoring templates (MAE9, D8)

> **Scope**: the CONCRETE conventions (not the mechanism) of the three "how a module is displayed" artifacts —
> `module.palette`, `<module>.lexicon`, and `@AgentOn` view/agent registration — extracted verbatim from real
> Tridium modules as copy-ready templates for a module author. The mechanism is REMITTANCE (B203 palette-BOG,
> B713/B759 lexicon, B4/B35 @AgentOn); this block captures the per-exemplar CONVENTION the kit lacked. Focus:
> `module-authoring-exemplars` (MAE9 / dimension D8). Kit destination: `types/dashboard.md` + `types/wb-widgets.md`.
>
> **Sources**: FUENTE 3 packaged/decompiled — `control-rt` (`module.palette`, `control-rt.lexicon`), `alarm-rt`
> (`alarm-rt.lexicon`), `alarm-wb` (`@AgentOn`, `META-INF/module.xml`); verified this session at `organized/`.
> READ-ONLY. English (post-B115).

---

## 780.1 — `module.palette`: draggable-type BOG `[CERT]`
A curated `bajaObjectGraph` of draggables. Attributes on each `<p>`: **`n=`** instance name (= the new component's
slot name), **`t=`** type as `symbol:TypeName`, **`m=`** module-map binding a one-letter alias to a module —
declared ONCE on the first `<p>` that uses that symbol. Verbatim (`control-rt/extracted/module.palette:1-12`):
```xml
<bajaObjectGraph version="1.0">
<p m="b=baja" t="b:UnrestrictedFolder">          <!-- root pre-declares b=baja -->
 <p n="Points" t="b:UnrestrictedFolder">          <!-- plural category folder -->
  <p n="BooleanPoint" m="c=control" t="c:BooleanPoint"/>   <!-- declares c=control once -->
  <p n="BooleanWritable" t="c:BooleanWritable"/>
```
Conventions observed: **name = bare Type minus the `B` prefix** (`BBooleanPoint` → `n="BooleanPoint"`); **folders =
plural category nouns** (`Points`, `Extensions`, `Recipients`); scale is SMALL/curated (control-rt ~13 draggables in
3 folders; alarm-rt ~14 + 3 top-level services); `h=` is unused by these real modules. **Nested `<p>` children
pre-seed sub-slots on the dropped instance** — e.g. a trigger with its mode, or an alarm ext with its algorithm.

## 780.2 — `<module>.lexicon`: flat key=value, prefixed to dodge the B759 collision `[CERT]`
Filename `<module>-<profile>.lexicon` in the jar root; a `#` header with `@author/@version`. Key conventions
(`control-rt.lexicon` / `alarm-rt.lexicon`):
- **slot display name** = bare slot key: (`override.value=Override Value`, control-rt.lexicon:12).
- **`<parent>.<child>` dotted keys** for nested/struct slots: (`alarmData.msgText=Message Text`,
  `alarmData.fromState=From State`, alarm-rt.lexicon:27-28).
- **enum-ordinal keys** flat one-per-value (`override.min1=1 Minute`, control-rt.lexicon:15; `level_1 … level_16`).
- **parameterized messages** with `{0}` args.
**The B759 collision, and how Tridium avoids it** `[CERT]`: the namespace is flat and MODULE-GLOBAL (one
`Lexicon.make("<module>")` map) — no per-type scoping, last-loaded wins on a bare-key clash. Real modules avoid it
by PREFIXING (`alarmData.status`, `alarmClass.priority`) instead of bare `status`/`priority`; a dup-key scan of
alarm-rt.lexicon returns ZERO duplicates — the prefixing discipline IS the collision-avoidance convention.

## 780.3 — `@AgentOn`: the dual-surface view/agent registration `[CERT]`
Registering a view/field-editor/wb-agent on a type is TWO synchronized surfaces — the Java annotation you write, and
the `module.xml <agent>` block Slot-o-Matic emits.
- **Java (source)** — `@NiagaraType(agent={@AgentOn(types={"mod:Type", …}, requiredPermissions="r")})` on a
  `BWbComponentView`/FE. Verbatim (`alarm-wb/.../BAlarmDbMaintenance.java:76`):
  `@NiagaraType(agent={@AgentOn(types={"alarm:AlarmService","alarm:AlarmDatabase","alarm:ArchiveAlarmProvider"},
  requiredPermissions="w")})` (import `javax.baja.nre.annotations.AgentOn`). Single-type form:
  `@AgentOn(types={"alarm:ConsoleRecipient"}, requiredPermissions="r")` (BAlarmConsole.java:255). `types` are
  `symbol:TypeName`; multiple types = one view offered on any of them; `requiredPermissions` is a BPermissions
  string (`r`/`w`).
- **module.xml (packaged mirror)** — `alarm-wb/extracted/META-INF/module.xml:52-59`:
  ```xml
  <type class="…BAlarmConsole" name="AlarmConsole">
   <agent requiredPermissions="r"><on type="alarm:AlarmConsole"/><on type="alarm:ConsoleRecipient"/></agent>
  </type>
  ```
  (A type with no agent is a bare `<type class=… name=…/>`.)

## 780.4 — Proposed toolbelt extractor (a kit-tooling delta) `[INFER]`
All three artifacts are structured, per-module, and today require manual find/grep+read. A small read-only
`module_nav palette-lexicon-agents <module>` subcommand that dumps (i) every `<p n= t= m=>`, (ii) all `key=value`
grouped by prefix WITH a **duplicate-bare-key collision report** (operationalizing B759 — a check no human should do
by eye), and (iii) all `<type>/<agent>/<on>` cross-checked against `@AgentOn` in source, would be low-effort,
high-reuse for any author using the build kit. (Also: `module_nav resources` expects `modules/<name>.jar`; the
packaged files live under `organized/<mod>/<sub>/extracted/` + `/vineflower/` — the navigator should learn that layout.)

## Self-verify

| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | palette `<p>` uses n=/t=/m=; m="alias=module" declared once on first use; root pre-declares b=baja | [CERT] | control-rt/module.palette:1-12 |
| 2 | palette naming = bare-Type-minus-B; folders = plural nouns; nested `<p>` pre-seeds child slots | [CERT] | control-rt/module.palette (Points/BooleanPoint; nested trigger/ext) |
| 3 | lexicon keys: bare slot, `<parent>.<child>` dotted, enum-ordinal, `{0}` params | [CERT] | control-rt.lexicon:12,15; alarm-rt.lexicon:27-28 |
| 4 | lexicon is flat module-global (B759); Tridium avoids clash by prefixing — alarm-rt has 0 dup keys | [CERT] | alarm-rt.lexicon (prefixed keys; dup-scan = 0) |
| 5 | @AgentOn dual-surface: Java `@NiagaraType(agent={@AgentOn(types=…,requiredPermissions=…)})` + module.xml `<agent><on>` | [CERT] | BAlarmDbMaintenance.java:76; BAlarmConsole.java:255; alarm-wb module.xml:52-59 |

**Tally**: 5 [CERT], 1 [INFER] (the proposed extractor). No unmarked claims. Load-bearing cites grep-verified inline
this session; the sweep's @AgentOn line numbers were corrected on verification (decompiled BAlarmDbMaintenance:76,
BAlarmConsole:255).

## Connections
- **B203** (palette BOG mechanism), **B713/B759** (lexicon mechanism + the module-global-key hazard §780.2
  operationalizes), **B4/B35** (@AgentOn/views), **B751-B753** (wb/ux authoring). **B778/B779** (the same modules'
  service + child-tree authoring — this block is their display surface).

## Open gaps
- **MAE9-G1** — the `-se`/`-doc` profile packaging of these artifacts (which profile ships the palette vs lexicon vs
  agents) is touched in MAE13 (D1 profile matrix), not here.

## Kit implication (→ `types/dashboard.md` + `types/wb-widgets.md`)
- `types/dashboard.md`: add the `module.palette` convention (bare-Type-minus-B names, plural category folders,
  `m="alias=module"` once, nested `<p>` to pre-seed ext/config slots) + the copy-ready template; and the lexicon rule
  — flat/module-global, so PREFIX keys (`parent.child`, `Type.slot`) to dodge the B759 collision.
- `types/wb-widgets.md`: document the dual-surface `@AgentOn` registration (write the annotation, Slot-o-Matic emits
  `<type><agent><on type=…/></agent></type>`; multi-type `types={…}` = one view over several source types).
- Toolbelt (optional): propose `module_nav palette-lexicon-agents <module>` with the dup-key collision report (§780.4).
