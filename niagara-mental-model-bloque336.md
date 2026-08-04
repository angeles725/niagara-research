# Block 336 — The outbound schema model: a JSON payload IS a tree of members walked top-down into one `JSONWriter`, each bound node resolving a `BOrd` against the station and selecting slots by one of four modes

> Focus **jsonToolkit** — evidence block J2. READ-ONLY. Corpus language: ENGLISH.
>
> Scope: the OUTBOUND schema MODEL — `BJsonSchema` (root), the member tree (`BIJsonObject`/`BIJsonArray`/
> `BIJsonProperty`), `BJsonSchemaBoundMember` (ORD binding), and `BJsonSchemaBoundSlotsContainer` (slot
> selection). This is HOW a schema is shaped; the subscription/generation TRIGGER is J3, queries J4, transport
> J5, property-type detail J8.
>
> Sources (primary, decompiled N4.14.0.162 + official docs), sweep sonnet + driver re-verification of the
> load-bearing citations: `organized/jsonToolkit/jsonToolkit-rt/vineflower/com/tridiumx/jsonToolkit/outbound/schema/`
> (`BJsonSchema.java`, `BJsonSchemaMember.java`, `BJsonSchemaBoundMember.java`,
> `BJsonSchemaBoundSlotsContainer.java`, `BIJsonObject/Array/Property.java`) and `docJsonToolkit`
> (`JsonSchemaTypes-Json-70BA9870.html`, `JSONSchemaConstruction-7A8BDB05.html`, `HowSchemaGenerationWorks-Json-7147CA2C.html`).
>
> Markers: `[CERT]` local decompiled source (`file:line`) · `[CERT-doc]` official Tridium doc (basename + heading)
> · `[INFER]` deduction. Block TYPE: **evidence**. FIRST corpus block to cite `docJsonToolkit`.

---

## 336.1 — `BJsonSchema`: the tree root

`public class BJsonSchema extends BJsonSchemaMember implements LicenseLimit, IPrefixLoggable` `[CERT]`
(`BJsonSchema.java:112`) — a plain `@NiagaraType` component placed anywhere in the station tree (no `@AgentOn`)
`[CERT]`. Key slots `[CERT]` (`:56-88`): `output` (String, readonly+persistent, with a
`jsonToolkit:JsonOutputWidget` field editor — the generated JSON lands here), `enabled`, `status`, `lastUpdated`,
`config` (`BJsonSchemaConfigFolder`), `queries` (`BJsonSchemaQueryFolder`). Tuning (write-on-start, update
strategy, min/max write time) is read from `getConfig().getTuningPolicy()` `[CERT]` (`:244`, `:362`, `:399-442`)
→ detail in J3/J8.

**The one-root rule**: `checkAdd()` throws `"Only 1 root member permitted"` if a second `BIJsonSchemaMember`
child is added `[CERT]` (`:327-328`); `getRoot()` = `getChildren(BJsonSchemaMember.class)[0]` `[CERT]` (`:343-346`).
The schema's own `getJsonName()` is `""` and its `process()` just delegates to `processChildJsonMembers()` `[CERT]`
(`:334`, `:339-341`) — the schema is a transparent holder whose single child is the actual JSON root. All ORD
resolution bases on `getBaseObject()` = `Sys.getStation()` `[CERT]` (`:587-589`).

## 336.2 — The member tree: three shapes, one recursive walk

Every node implements `BIJsonSchemaMember`, whose default `processChildJsonMembers(writer, jsonKeysValid)`
iterates `getChildren(BIJsonSchemaMember.class)` and calls each child's `process()` `[CERT]`
(`BIJsonSchemaMember.java:14-20`). Three marker interfaces give the three JSON shapes, each with a default
`process()`:

- **`BIJsonObject`** — `process()` writes the key (if `jsonKeysValid`), calls `writer.object()`,
  `populateObjectContent()`, then `processChildJsonMembers(writer, true)` (children inside `{}` write KEYS), then
  `writer.endObject()` `[CERT]` (`BIJsonObject.java:20-38`).
- **`BIJsonArray`** — same but `writer.array()` / `endArray()`, and passes `jsonKeysValid=false` down (children
  inside `[]` write VALUES, no keys) `[CERT]` (`BIJsonArray.java:16-28`).
- **`BIJsonProperty<T>`** — a leaf: `getJsonValue()` → `writer.value(JsonSchemaUtil.toJsonType(getJsonValue(),
  config))`; no children (`isChildLegal()` false for a property) `[CERT]` (`BIJsonProperty.java:21-30`,
  `BJsonSchemaMember.java:63-65`). A property may not sit directly under `BJsonSchema` `[CERT]`
  (`BIJsonProperty.java:22-23`).

So the JSON structure is literally the component tree, and generation is a single top-down traversal writing to
one shared `JSONWriter` — object nodes open `{}` and propagate `keysValid=true`, array nodes open `[]` and
propagate `false`, leaves emit a typed scalar `[INFER]` (the mechanism composed from the three contracts).

## 336.3 — Binding: a node resolves a `BOrd` against the station, through a permission check

`BJsonSchemaBoundMember` adds a `binding` property of type `BOrd` (default `BOrd.DEFAULT`, field editor
`jsonToolkit:SlotOrdFE`) `[CERT]` (`BJsonSchemaBoundMember.java:40-58`). Resolution `[CERT]` (`:184-267`):
`getTarget()` returns a cached `BObject` when the node uses COV subscriptions (J3), else calls `getOrdTarget()` →
`getSchema().getOrdTarget(this, base)` → `member.getBinding().resolve(base, ctx)` `[CERT]` (`BJsonSchema.java:544-546`),
and the resolved target passes through **`JsonSchemaSecurity.permissionsCheck(schema, target)`** `[CERT]`
(`BJsonSchemaBoundMember.java:229`). So EVERY bound resolution is gated by a permission check `[INFER]` — the
`JsonSchemaSecurity` ACL surface (undocumented) is J7/J8. The JSON key of a bound node derives from
`jsonNameSource` (default `displayName`) via `JsonSchemaNameUtil` `[CERT]` (`:135`).

## 336.4 — Slot selection: four modes, independent of the COV blacklist

A `BJsonSchemaBoundSlotsContainer` (a bound object whose properties are the target's slots) decides WHICH slots
become JSON via `slotsToInclude` (`BSlotSelectionType`, default `allVisibleSlots`) `[CERT]`
(`BJsonSchemaBoundSlotsContainer.java:27-38`). `getPropertiesToIncludeInJson(target)` switches on the ordinal
`[CERT]` (`:73-100`):

| Ordinal | Mode | Rule |
|---|---|---|
| 0 | allSlots | `target.getPropertiesArray()` — everything |
| 1 | **allVisibleSlots** (default) | exclude hidden: `(flags & 4) == 0` |
| 2 | summary | only summary: `(flags & 8) != 0` |
| 3 | selectedSlots | the manually-listed `slotList` names |

Each selected slot is read and converted by `JsonSchemaUtil.toJsonType()` — the Baja→JSON type boundary `[CERT]`
(`:113-120`). **This slot selection is INDEPENDENT of the service's `globalCovSlotFilter`** [Block 335] §335.5:
that blacklist governs which slot CHANGES trigger regeneration (J3, subscription level); this selection governs
which slots APPEAR in the output at generation time — I grep-confirmed no COV-filter reference in this class
`[CERT]`. Two different filters, two different stages `[INFER]`.

## 336.5 — The official mental model `[CERT-doc]`

Tridium's own description matches the code exactly:
> "All components that contribute to the string output … are called **members** and are nested under the schema.
> During generation, the system processes each member **recursively (top down)**, appending each member's result
> to a JSON writer." — `JsonSchemaTypes-Json-70BA9870.html` §"JSON schema types" `[CERT-doc]` (tokens `processChildJsonMembers`,
> "recursively call process", "nested schema", "JSON writer" verified in the file).
> "Three interfaces represent three structural types … **Property (key/value), Object, Array**." — same doc `[CERT-doc]`.
> "Setting up a schema involves **binding station data to JSON entities** … using the current values of an ord
> target." — `JSONSchemaConstruction-7A8BDB05.html` `[CERT-doc]`. Its slot-selection list (All / All visible / Summary /
> Selected) aligns 1:1 with the four ordinals of §336.4 `[CERT-doc]`.
> "Binding ords resolve against the current base item of the schema … this is the station." —
> `HowSchemaGenerationWorks-Json-7147CA2C.html` `[CERT-doc]`, confirmed by `getBaseObject()`=`Sys.getStation()` (§336.1).

So doc and code agree on the whole model `[INFER]`: schema = tree, generation = recursive top-down `process()`
into one writer, bound nodes pull live station data by ORD.

## 336.6 — What this block does NOT resolve

- WHEN generation runs (write-on-start, COV subscriptions, update strategy) and where `output` is pushed → **J3**.
- Queries feeding array/object content (`BJsonSchemaQueryFolder`) → **J4**; the exporter/transport → **J5**.
- The 15 concrete property TYPES and `JsonSchemaUtil.toJsonType` mapping detail → **J8**.
- `JsonSchemaSecurity.permissionsCheck` internals (the ACL) → **J7/J8**.

## 336.7 — Connections

- [Block 335] §335.5 — the `globalCovSlotFilter` this block distinguishes from schema-level slot selection
  (§336.4); the service is the license/identity root above every schema.
- [Block 76] — used jsonToolkit's `InlineJsonWriter` as inspiration for a chihuahua JSON generator; that inline
  path is the programmatic cousin of this declarative tree (J10).
- J3/J4/J5 — the trigger, the queries, and the transport that turn this static model into emitted JSON.

## 336.8 — Self-verify

Block TYPE: **evidence** (code + first `[CERT-doc]`). Delegated sweep **sonnet**; driver re-verified the
load-bearing claims verbatim: one-root constraint (`BJsonSchema.java:327`), `getBaseObject`=station (`:587`), the
four slot-selection ordinals (`BJsonSchemaBoundSlotsContainer.java:73-100`), the `BOrd` binding + permission
check (`BJsonSchemaBoundMember.java:54`, `:229`), and token-checked the `[CERT-doc]` quote against
`JsonSchemaTypes-Json-70BA9870.html` (4 tokens present). Extern anchors: `BJsonSchema.java:112`, `:327`, `:587`,
`BJsonSchemaBoundMember.java:229`, `BJsonSchemaBoundSlotsContainer.java:73`, `BIJsonObject.java:20`. Doc
registered in `sources/SOURCES.md`.

`verify-block.sh` marker tally (computed):

| Marker | count (adj) |
|---|---|
| CERT (extern file:line) | 23 |
| CERT-doc | 9 |
| CERT-hw / CERT-live / CERT-web / CERT-a | 0 |
| INFER | 5 |
| INFER/CERT ratio | 0.16 |

`verify-block.sh` exit 0. `verify-sources.sh`: docJsonToolkit files registered + preserved under
`sources/manuals/jsonToolkit-docs/`, no FABRICATED-CITE for B336.

Evidence block: `[INFER]`s are the two-filters-two-stages reading and the doc↔code agreement, each anchored to a
cited `[CERT]`/`[CERT-doc]`.
