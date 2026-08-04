# Block 342 — The outbound detail catalog: 15 property types in five groups, 8 query-result JSON shapes selected per-result, and a three-stage key-naming pipeline (source → spacing → casing)

> Focus **jsonToolkit** — evidence block J8. READ-ONLY. Corpus language: ENGLISH.
>
> Scope: the CATALOG of the outbound schema — the 15 leaf `property/` types (what each emits), the 8 query-result
> STYLES in `query/style/` (what JSON shape each renders), and the `config/` naming enums. Fills in the detail
> the model block [Block 336] J2 and the query block [Block 338] J4 deferred.
>
> Sources (primary, decompiled N4.14.0.162 + docs), sweep sonnet + driver re-verification of every enum list:
> `organized/jsonToolkit/jsonToolkit-rt/vineflower/com/tridiumx/jsonToolkit/outbound/schema/`
> (`property/*`, `query/style/*`, `config/*`) and `docJsonToolkit` (`Components-0567A524.html`,
> `CustomQueryStyle-Json-70E179E6.html`, `jsonToolkit-JsonSchemaFacetProperty.html`).
>
> Markers: `[CERT]` local decompiled source (`file:line`) · `[CERT-doc]` official doc · `[INFER]` deduction.
> Block TYPE: **evidence** (catalog).

---

## 342.1 — The property palette: 15 leaf types in five groups

All are `BIJsonProperty` leaves [Block 336] §336.2 whose `getJsonValue()` supplies the scalar. Five groups `[CERT]`:

| Group | Types | Emits |
|---|---|---|
| **Literal/constant** | `BJsonSchemaStringProperty`, `BJsonSchemaNumericProperty`, `BJsonSchemaBooleanProperty`, `BJsonSchemaCountProperty` | a fixed slot value; **Count** auto-increments an `AtomicInteger` **per generation** (`getJsonValue` calls `incrementAndGet`) `[CERT]` (`BJsonSchemaCountProperty.java:47`, `:50-51`) |
| **Bound-value** | `BJsonSchemaBoundProperty`, `BJsonSchemaBoundCsvProperty` | the live `BOrd` target value; CSV variant flattens child slots to a comma-joined string with NO `[]`/`{}` `[CERT]` (`BJsonSchemaBoundCsvProperty.java:37-41`) |
| **Time** | `BJsonSchemaCurrentTimeProperty` (`BAbsTime.now()`), `BJsonSchemaUnixTimeProperty` (`System.currentTimeMillis()` epoch ms) | a timestamp `[CERT]` |
| **Metadata/facet/tag** | `BJsonSchemaMetadataProperty`, `BJsonSchemaFacetProperty`, `BJsonSchemaTagProperty` | one system field / one facet value / one tag value of the bound component `[CERT]` |
| **Aggregate/list** | `BJsonSchemaFacetList`, `BJsonSchemaTagList` (both extend `BJsonSchemaPropertyList`) | N key-value pairs — all-or-CSV-filtered facets / tags of the bound component `[CERT]` (`BJsonSchemaFacetList.java:63-101`, `BJsonSchemaTagList.java:103-133`) |

Doc palette confirms: Count "increments by one each time the schema generates"; BoundCSV "renders child slots as
a … comma separated list (with no surrounding [] or {})" — `Components-0567A524.html` `[CERT-doc]`.

## 342.2 — Query styles: 8 JSON shapes, discovered globally, chosen per-result

`BQueryResultWriter` (abstract, `BObject`+`BIAgent`) declares `appendJson(JSONWriter, QueryResultHolder)` `[CERT]`.
`QueryStyleManager` discovers ALL writer agents once from the registry
(`Sys.getRegistry().getAgents(BJsonSchemaBoundQueryResult.TYPE)`) and caches them; the actual style is chosen by
the user at the `BJsonSchemaBoundQueryResult` (the manager only enumerates) `[CERT]` (`QueryStyleManager.java:29-36`).
The 8 shapes turn a cached result [Block 338] §338.4 into `[CERT]`:

| Style | JSON shape |
|---|---|
| `BObjectsArray` | `[{col:v,…}, …]` — array of objects keyed by column name |
| `BNamedObjects` | `{firstColVal:{…}, …}` — object keyed by first-column value |
| `BRowArray` | `[[v,v,…], …]` — array of value-arrays (no names) |
| `BRowArrayWithHeader` | same, first element is the header row |
| `BColumnArray` | `[[colA…],[colB…]]` — transposed (one inner array per column) |
| `BColumnArrayWithHeader` | same, first element of each inner array is the column name |
| `BSingleColumnArray` | `[v,v,v]` — flat, first column only |
| `BKeyValuePairObject` | `{col1:col2, …}` — object, col1=key col2=value (needs ≥2 columns) `[CERT]` (`BKeyValuePairObject.java:26`, `:32`) |

Custom styles are a documented extension point: extend `BQueryResultWriter`, `@AgentOn(jsonToolkit:
JsonSchemaBoundQueryResult)`, implement `appendJson`+`previewText` — `CustomQueryStyle-Json-70E179E6.html`
`[CERT-doc]` (with a `QueryResultHolder.getResultList()`/`getColumnNames()` example).

## 342.3 — The key-naming pipeline: source → spacing → casing

A bound member's JSON KEY is derived by a three-stage, per-schema pipeline applied at emit time
(`JsonSchemaNameUtil`), all enums driver-verified `[CERT]`:

1. **`BJsonSchemaNameSource`** (5, default `displayName`) — the RAW string:
   `displayName` / `targetName` / `targetDisplayName` / `targetParentName` / `targetPath` `[CERT]`
   (`BJsonSchemaNameSource.java:12`).
2. **`BJsonSchemaNameSpacing`** (6, default `remove`) — space handling:
   `remove` / `keep` / `add` / `hyphenate` / `underscore` / `urlEncode` `[CERT]` (`BJsonSchemaNameSpacing.java:12`).
3. **`BJsonSchemaNameCasing`** (5, default `camel`) — letter case:
   `camel` / `pascal` / `upper` / `lower` / `preserve` `[CERT]` (`BJsonSchemaNameCasing.java:12`).

Two more config enums `[CERT]`: `BJsonSchemaPropertyNameSource` (2: `displayName`/`name`) for auto-discovered
slots, and `BMetadataField` (12: `name`, `displayName`, `slotPath`, `handle`, `typeName`, `typeSpec`,
`parentsName`, `deviceName`, `nearestFolderName`, `historyName`, `historyId`, `toString`) — the field
`BJsonSchemaMetadataProperty` extracts, via a 12-case switch `[CERT]` (`BMetadataField.java:12`,
`BJsonSchemaMetadataProperty.java:113-154`). `BSlotSelectionType` (J2 §336.4), `BJsonSchemaTuningPolicy` +
`BJsonSchemaUpdateStrategy` (J3) live here too — named, not re-derived.

## 342.4 — What this block does NOT resolve

- The relative-schema variant of properties/queries (cross-station) → **J9**.
- The programmatic `BInlineJsonWriter` (a non-tree emitter) → **J10**.
- The alarm-recipient outbound (`BJsonAlarmRecipient`) → **J11**.
- `JsonSchemaUtil.toJsonType` (the Baja→JSON primitive mapping the leaves call) — a small utility, folded into J12.

## 342.5 — Connections

- [Block 336] §336.2 — the object/array/property tree; this block enumerates the property leaves + the bound
  slot-container key naming.
- [Block 338] §338.4 — `BJsonSchemaBoundQueryResult` selects one of these §342.2 styles for its cached rows.
- [Block 337] §337.1 — Count's per-generation increment ties to the generation trigger cadence.

## 342.6 — Self-verify

Block TYPE: **evidence** (catalog; code + `[CERT-doc]`). Delegated sweep **sonnet**; driver re-verified every
load-bearing ENUM list verbatim (the counts are denominators): `BJsonSchemaNameCasing` 5 (`:12`),
`BJsonSchemaNameSource` 5 (`:12`), `BJsonSchemaNameSpacing` 6 incl. `urlEncode` (`:12`), `BMetadataField` 12
(`:12`), plus Count's `incrementAndGet` (`BJsonSchemaCountProperty.java:50-51`) and `BKeyValuePairObject`'s
2-column minimum (`:26`, `:32`). `[CERT-doc]` token-checked; `Components`/`CustomQueryStyle`/`FacetProperty` docs
registered + preserved under `sources/manuals/jsonToolkit-docs/`.

`verify-block.sh` marker tally (computed):

| Marker | count (adj) |
|---|---|
| CERT (extern file:line) | 17 |
| CERT-doc | 5 |
| CERT-hw / CERT-live / CERT-web / CERT-a | 0 |
| INFER | 2 |
| INFER/CERT ratio | 0.09 |

`verify-block.sh` exit 0; `verify-sources.sh` no FABRICATED-CITE for B342.

Evidence block (catalog): almost all `[CERT]`/`[CERT-doc]` — a low `[INFER]` ratio is expected for an enumeration
block; the few `[INFER]`s are group categorizations.
