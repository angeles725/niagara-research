# B781 · Author-side declaration surfaces — categories (none), relations (a definition, not a subclass), and hierarchy (level-def composition) (MAE10, D9)

> **Scope**: how a module AUTHORS three "grouping/relating" surfaces — categories, relations, hierarchies. The key
> result is that all three are authored DIFFERENTLY from what a naive builder expects, and one (categories) has NO
> author-side surface at all. Runtime models are REMITTANCE (B11/B48 categories, B758 BCustomRelation, B584-B586
> hierarchy, B260-B270 tags); this block is the AUTHOR side. Focus: `module-authoring-exemplars` (MAE10 / D9). Kit
> destination: `types/logic.md`.
>
> **Sources**: FUENTE 3 decompiled — `baja` (`BComponent`, `BCategory`, `BCategoryService`, `BRelation`,
> `Relation`), `hierarchy-rt` (`BLevelDef`, `BHierarchy`, `BHierarchyService`, level-def types, module.xml),
> `tagdictionary`; verified this session at `organized/`. READ-ONLY. English (post-B115).

---

## 781.1 — CATEGORIES: there is NO author-side declaration (runtime-only) `[CERT]`
A module author declares NOTHING for categories — no `module.xml <category>`, no `BCategory` subclass, no
per-component author markup. Every component is categorizable for free: `BComponent … implements … BICategorizable`
(`baja/.../BComponent.java:84`), and `getCategoryMask()` reads the mask from the runtime slotMap
(`BComponent.java:1934`), not from source. `BCategory extends BAbstractCategory` (`.../category/BCategory.java:20`)
is a plain component INSTANCE created at runtime under `BCategoryService`; assignment is a runtime ORD→mask table
`BOrdToCategoryMap` held by the service (B11/B48). **Author obligation: none.** Kit consequence: emit NO
category-authoring scaffold — categories are an operator-runtime concern, not a build-time one.

## 781.2 — RELATIONS: `BRelation` is a CONCRETE carrier; a relation TYPE is a `relationId` + a `RelationInfo` `[CERT]`
`public class BRelation extends BStruct implements Relation` (`baja/.../BRelation.java:44`) is **not abstract** — it
is the universal persisted carrier (a knob on a component pair), with author-visible props `relationId` (`:20,45`),
`inbound` (direction, `:31`), `sourceOrd`, `relationTags`. So you do NOT author a relation by subclassing BRelation.
Two author moves instead:
- **Emit a relation instance**: construct `BRelation`/`BasicRelation` with a `relationId` (`namespace:name`), an
  endpoint ORD/component, and a direction (`INBOUND`/`OUTBOUND`, `Relation.java`).
- **Define a new relation TYPE**: implement/register a `RelationInfo` (`tag/RelationInfo.java` — `getRelationId()` →
  `Id.newId(dictionary.namespace, name)`, `addRelations(...)`/`getRelation(...)`) in a tag dictionary — the covered
  `BCustomRelation extends BRelationInfo` (B758) is exactly this. **Contrast**: `BRelation` = the carrier;
  `BRelationInfo`/`BCustomRelation` = the DEFINITION that computes/materializes relations (often BQL-backed) — the
  author-side "type" lives here, never in a BRelation subclass.

## 781.3 — HIERARCHY: composed from `BLevelDef` variants under `BHierarchyService` `[CERT]`
The hierarchy author composes framework components — no Java subclassing for a standard hierarchy. The abstract unit
is `public abstract class BLevelDef extends BComponent` (`hierarchy-rt/.../BLevelDef.java:46`) with the single
contract `abstract BLevelElem[] getElements(BLevelElem parent, Context cx)` (`:80`). The author drops a `BHierarchy`
root (itself a `BLevelDef`) into `BHierarchyService extends BAbstractService` (`.../BHierarchyService.java:86`) and
adds an ordered sequence of level defs, each either:
- **an ENTITY level** (real components) — `BQueryLevelDef` (a NEQL `query`) or `BRelationLevelDef`
  (`inboundRelationIds`/`outboundRelationIds`, follows §781.2 relations to reach entities);
- **a GROUPING level** (synthetic folders) — `BGroupLevelDef` (`groupBy`) or `BListLevelDef` (+`BNamedGroupDef`).
All registered in `hierarchy-rt/.../META-INF/module.xml` (`BQueryLevelDef`:49, `BRelationLevelDef`:50), which also
exposes the `hierarchy:` ORD space via `<type … BHierarchyScheme … ordScheme="hierarchy"/>` (`:36`, the §778.2 ORD-
scheme pattern). A CUSTOM level = subclass `BLevelDef` and implement `getElements`.

## Self-verify

| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | Categories have no author-side declaration; every BComponent is BICategorizable at runtime | [CERT] | BComponent.java:84,1934; BCategory.java:20; (B11/B48 runtime service) |
| 2 | `BRelation` is CONCRETE (`extends BStruct implements Relation`), a carrier — not a base to subclass | [CERT] | BRelation.java:44; props relationId :20,45 / inbound :31 |
| 3 | A relation TYPE is authored as a `relationId` + a `RelationInfo`/`BCustomRelation` definition (B758), not a BRelation subclass | [CERT/INFER] | RelationInfo.java; B758 (BCustomRelation); [CERT] on BRelation concreteness |
| 4 | Hierarchy: `BLevelDef` abstract + `getElements`; author composes BHierarchy root + level-def sequence under BHierarchyService | [CERT] | BLevelDef.java:46,80; BHierarchyService.java:86; module.xml:49,50 |
| 5 | Entity levels = BQueryLevelDef(NEQL)/BRelationLevelDef; grouping = BGroupLevelDef/BListLevelDef; hierarchy: ORD scheme | [CERT] | hierarchy-rt module.xml:36,49,50 |

**Tally**: 4 [CERT], 1 [CERT/INFER]. No unmarked claims. Spine grep-verified inline this session at `organized/`.

## Connections
- **B11/B48** (BCategoryService runtime — §781.1 confirms there is no author side above it). **B758**
  (BCustomRelation/BRelationInfo — §781.2's relation-type definition surface). **B584-B586** (hierarchy runtime model
  — §781.3 is its author side). **B778** (the `hierarchy:` scheme is a live instance of the ORD-scheme pattern B778
  documents). **B260-B270** (tags — relations are `Taggable`).

## Open gaps
- **MAE10-G1** — a fully custom `BLevelDef` subclass (its own `getElements` computing a non-query/non-relation
  level) is named but not walked; bounded follow-up if a builder needs a bespoke level.

## Kit implication (→ `types/logic.md`)
Add a "grouping/relating declaration surfaces" note with THREE distinct postures: (1) **categories** — author
NOTHING; every component is `BICategorizable`, categories are operator-runtime via `BCategoryService` — the kit must
emit NO category scaffold; (2) **relations** — never subclass `BRelation` (it is a concrete carrier); emit a
`BRelation`/`BasicRelation` instance (relationId + endpoint + direction), or define a relation TYPE by registering a
`RelationInfo`/`BCustomRelation` in a tag dictionary (B758); (3) **hierarchy** — compose a `BHierarchy` root +
ordered `BLevelDef` children (`BQueryLevelDef`/`BRelationLevelDef` entity levels, `BGroupLevelDef`/`BListLevelDef`
grouping levels) under `BHierarchyService`; subclass `BLevelDef`+`getElements` only for a bespoke level.
