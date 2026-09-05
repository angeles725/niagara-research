# B783 · Template author-side — there is no type-registration SPI; a "template type" is an `.ntpl` artifact produced by a job (MAE12, D11)

> **Scope**: how a module author defines a NEW reusable template — the AUTHOR side, vs B573's deploy/consume side.
> Headline (negative) finding: there is NO compile-time SPI where an author subclasses a `BTemplate`/`BTemplateType`;
> the template *category* is a closed enum, and a specific template is a RUNTIME `.ntpl` ZIP produced by a job from
> a component subtree marked with a `BTemplateConfig`. So the kit must document "tag + BTemplateConfig + make-job →
> `.ntpl`", not a subclass/registration recipe. Focus: `module-authoring-exemplars` (MAE12 / D11). Kit destination:
> `types/logic.md`.
>
> **Sources**: FUENTE 3 decompiled `template-rt` (`BTemplateService`, `BTemplateConfig`, `BConfigBinding`,
> `BNtplFile`, `TemplateConst`, `TemplateType`, `NiagaraTemplate`); verified this session at `organized/`.
> FUENTE 1: B573 (deploy/consume), B571-B579. READ-ONLY. English (post-B115).

---

## 783.1 — There is NO author-side type-registration SPI `[CERT]`
The template *category* is a CLOSED enum — `public enum TemplateType { UNSPECIFIED, COMPONENT, DEVICE, STATION,
APPLICATION }` (`api/TemplateType.java:8-13`) — an author cannot add a constant. And no `registerTemplateType`, no
author-supplied `BTemplate` subclass, exists (module.xml exposes only fixed framework types). "Define a new template
type" therefore = produce an `.ntpl` ARTIFACT, not register a class.

## 783.2 — `BTemplateService`: discovery by descendant scan, not registration `[CERT]`
`BTemplateService extends BAbstractService` (`BTemplateService.java:101`) tracks LIVE `BTemplateConfig` marker
components, not template classes. On `serviceStarted()` it scans the station — `CompUtil.getDescendants(station,
BTemplateConfig.class)` (:194) — and calls `register(BTemplateConfig)` (:200) per hit (adding a dynamic
`BTemplateInfo` child). The author-facing "make" actions submit jobs: `makeStationTemplate`/`makeApplicationTemplate`
→ `BMakeStationTemplateJob`/`BMakeApplicationTemplateJob` (superuser-gated). **No downstream author registration is
required** beyond placing a `BTemplateConfig` in the station.

## 783.3 — The author-side marker: `BTemplateConfig` + binding structs + tagged slots `[CERT]`
`BTemplateConfig extends BComponent` (`BTemplateConfig.java:107`) is the marker dropped INSIDE the subtree to be
templatized (its parent is the template root). The parameter/binding SCHEMA is carried by CHILD structs, not by
subclassing:
- `BConfigBinding extends BStruct` (`BConfigBinding.java:36`) with frozen slots `targetOrd`/`sourceSlot`/
  `targetSlot`/`userTip` (:35) — the exposed config parameters; `getConfigBindings() = getChildren(BConfigBinding
  .class)`. Also `BPasswordBinding` (sensitive params), `BRelationInfo` (relation params).
- Input/output params are DERIVED FROM TAGGED link slots (`getInputSlotTags(Slot)`/`getOutputSlotTags` read the tags
  off the knob/link) — the author marks a slot as a template parameter by TAGGING the link, and `BTemplateConfig`
  harvests it. **Author obligation: subclass nothing** — instantiate a `BTemplateConfig`, add `BConfigBinding`/
  `BPasswordBinding`/`BRelationInfo` children per exposed parameter, tag the input/output link slots.

## 783.4 — The `.ntpl` artifact: a ZIP of `.bog` + `template-manifest.xml` `[CERT]`
`.ntpl` IS the mechanism. `TemplateConst`: `TEMPLATE_EXTENSION = "ntpl"` (:17), `TEMPLATE_MANIFEST_NAME =
"template-manifest.xml"` (:16). `BNtplFile extends BZipFile` (`file/BNtplFile.java:91`) — the `.ntpl` is a ZIP
containing `template-manifest.xml` + one `.bog` (the component graph) + `px/` files + images + station files. The
manifest (`manifest/TemplateManifest.java`) carries `settings`/`links`/`bindings`/`resources`/`subtemplates`/
`dependencies`/`revisionHistory`; each parameter descriptor is a nested `Value` (slotPath, name, required,
type[num/bool/str/cfg], direction[cfg/in/out], unit/min/max) read/written by `ManifestXMLReader`/`ManifestXMLWriter`.
The author API is `api/NiagaraTemplate` (`AutoCloseable`): `createFrom(BComponent)` (:57) / `createStationTemplateFrom`
/ `createApplicationFrom`, then `save(FilePath|OutputStream)` (:132).

## 783.5 — Author obligation, concretely, and the B573 contrast `[CERT/INFER]`
To ship a NEW reusable template in a module: (1) build the component subtree (the logic); (2) drop a
`BTemplateConfig` under the root + add `BConfigBinding`/`BPasswordBinding`/`BRelationInfo` children + tag the
parameter link slots; (3) run `BTemplateService.makeStationTemplate`/`makeApplicationTemplate` (or programmatically
`NiagaraTemplate.createFrom(...).save(path)`) — the make-job emits the `.ntpl`/`.napl` ZIP; (4) ship the `.ntpl`.
**Contrast with B573 (deploy/consume)**: consuming loads an existing `.ntpl` (`NiagaraTemplate.load`/`open`,
`installApplication`) and reads `requiredProperties()`/`properties()` to fill values before instantiating — the SAME
`NiagaraTemplate`/`TemplateSource` API run in the read/deploy direction. There is no separate author-only SPI; the
"author side" is the same machinery run in the make direction.

## Self-verify

| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | Template category is a CLOSED enum; no author-side type-registration SPI exists | [CERT] | api/TemplateType.java:8-13; module.xml fixed types |
| 2 | `BTemplateService` (a BAbstractService) DISCOVERS `BTemplateConfig` by descendant scan; make = jobs | [CERT] | BTemplateService.java:101,194,200 |
| 3 | `BTemplateConfig extends BComponent`; params = `BConfigBinding extends BStruct` children + tagged link slots | [CERT] | BTemplateConfig.java:107; BConfigBinding.java:35-36 |
| 4 | `.ntpl` = `BNtplFile extends BZipFile` = `.bog` + `template-manifest.xml`; author API `NiagaraTemplate.createFrom().save()` | [CERT] | TemplateConst.java:16-17; BNtplFile.java:91; NiagaraTemplate.java:57,132 |
| 5 | Author obligation = tag + BTemplateConfig + make-job → .ntpl; consume = same API in the load direction (B573) | [CERT/INFER] | §783.5; [CERT] on the API, [INFER] on the "author does these steps" |

**Tally**: 4 [CERT], 1 [CERT/INFER]. No unmarked claims. Spine grep-verified inline this session at `organized/`.

## Connections
- **B573** (template deploy/consume — this block is its author-side mirror; same `NiagaraTemplate` API in the make
  direction). **B571-B579** (provisioning+template). **B781** (a `BTemplateConfig` carries `BRelationInfo` params —
  the same relation-definition surface B781 §781.2 documents). **B778** (`BTemplateService` is another `BAbstractService`
  registered-by-placement, per B778 §778.1).

## Open gaps
- **MAE12-G1** — the `.ntpl` `template-manifest.xml` full schema (all `Value`/`Resource`/`Subtemplate` fields) is
  summarized, not exhaustively walked; a bounded follow-up if a builder needs to hand-author a manifest.

## Kit implication (→ `types/logic.md`)
Document the template author path as ARTIFACT PRODUCTION, not type registration: a "template type" is NOT a Java class
you register — it is an `.ntpl` ZIP (a `.bog` + `template-manifest.xml` with settings/bindings/links) produced by a
make-job (`BMakeTemplateJob`/`NiagaraTemplate.save`) from a component subtree marked with a `BTemplateConfig` (+
`BConfigBinding` children + tagged parameter slots). Do NOT scaffold a `BTemplate` subclass — there is no such SPI.
