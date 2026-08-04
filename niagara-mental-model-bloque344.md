# Block 344 — The programmatic escape hatch: `BInlineJsonWriter` drops a `BProgram` into the schema tree and hands it the shared `JSONWriter`, and `BTypeOverride` lets a Program rewrite how a Baja value serializes — the native facility [Block 76] reused

> Focus **jsonToolkit** — evidence block J10. READ-ONLY. Corpus language: ENGLISH.
>
> Scope: the `program/` package — `BInlineJsonWriter` (a schema member that emits JSON via an embedded Program)
> and `BTypeOverride` (a Program that customizes a value's JSON encoding). The escape hatch for JSON the
> declarative tree [Block 336]/[Block 342] cannot express. This is the mechanism [Block 76] drew on when it
> ported a Program-based JSON generator into `chihuahua-rt`.
>
> Sources (primary, decompiled N4.14.0.162 + docs), read in full inline:
> `organized/jsonToolkit/jsonToolkit-rt/vineflower/com/tridiumx/jsonToolkit/outbound/schema/program/`
> (`BAbstractInlineJsonWriter.java`, `BInlineJsonWriter.java`, `BTypeOverride.java`) and `docJsonToolkit`
> (`InlineJSONWriter-94E2D15D.html`, `TypeOverrideExample-94E2114D.html`, `jsonToolkit-InlineJsonWriter.html`).
>
> Markers: `[CERT]` local decompiled source (`file:line`) · `[CERT-doc]` official doc · `[INFER]` deduction.
> Block TYPE: **evidence**.

---

## 344.1 — `BInlineJsonWriter`: a schema member backed by a Program

`BInlineJsonWriter extends BAbstractInlineJsonWriter extends BJsonSchemaMember` `[CERT]`
(`BInlineJsonWriter.java:23`, `BAbstractInlineJsonWriter.java:12`) — so it is a NODE in the schema tree
[Block 336] §336.2, but instead of bound slots it holds a `program` property of type `com.tridium.program.BProgram`
`[CERT]` (`BInlineJsonWriter.java:19-24`). The base carries a `jsonWriter` field
(`com.tridium.json.JSONWriter`) and exposes it via `getJsonWriter()` `[CERT]`
(`BAbstractInlineJsonWriter.java:14`, `:35-36`); it has NO child processing (`processChildJsonMembers` is empty —
the Program IS the content) `[CERT]` (`:23-24`), and `getCurrentBase()` returns `schema.getBaseObject()` so the
Program can see the current base (including a relative schema's per-row base [Block 343]) `[CERT]`.

## 344.2 — The mechanism: `process()` invokes the Program with the writer

At generation time [Block 337] §337.1, `process(jsonWriter, keysValid)` stores the writer, looks up the Program's
`"override"` action, and invokes it passing `this` (the writer node) as the argument `[CERT]`
(`BInlineJsonWriter.java:45-52`):

```java
this.jsonWriter = jsonWriter;
Action a = this.getProgram().getAction("override");
BObject result = this.getProgram().invoke(a, this);     // Program receives the writer node
```

So the embedded Program runs with access to the shared `JSONWriter` (via the argument's `getJsonWriter()`) and
the current base, and writes ARBITRARY JSON straight into the stream `[INFER]`. A `JSONException` from the Program
is caught and logged as a WARNING (the generation continues) `[CERT]` (`:54-60`). This is the declarative-tree
escape hatch: whatever shape the property/object/array leaves cannot produce, a Program can `[INFER]`. Doc:
`InlineJSONWriter-94E2D15D.html` centers on giving a Program the `JsonWriter`/`jsonWriter` to emit inline
`[CERT-doc]`.

## 344.3 — `BTypeOverride`: a Program rewrites a value's JSON encoding

`BTypeOverride extends BComponent` with an `override(BValue)` action `[CERT]` (`BTypeOverride.java:25-31`). Its
`overrideValueEncoding(value)` → `considerPrograms(value)` runs the first child `BProgram` to transform how that
value is encoded `[CERT]` (`:42`, `:47-49`). So where `BInlineJsonWriter` overrides a whole SUBTREE's emission,
`BTypeOverride` overrides how a single VALUE TYPE serializes to JSON `[INFER]` — the finer-grained hook. Doc:
`TypeOverrideExample-94E2114D.html` `[CERT-doc]`.

## 344.4 — The facility [Block 76] reused (REMITTANCE)

[Block 76] documented porting "a JSON generator from a Program (jsonToolkit `InlineJsonWriter`) to a custom module
`chihuahua-rt`" — a `String output` slot regenerated periodically and exposed over obix. That block is the APPLIED
/reuse case; THIS block is the native SOURCE mechanism it drew from `[CERT]` (cross-block). [Block 76] §53 also
named `com.tridium.json.JSONWriter(Appendable)` as the closest primitive — exactly the `JSONWriter` this member
hands the Program `[CERT]` (`BAbstractInlineJsonWriter.java:3`, `:14`). So the corpus already had the consumer;
J10 closes the loop with the producer.

## 344.5 — What this block does NOT resolve

- `com.tridium.program.BProgram` internals (the `program-rt` module) — a module dependency [Block 335] header,
  not the jsonToolkit focus.
- `com.tridium.json.JSONWriter` / `JSONStringer` internals (bundled Tridium JSON) — the emitter primitive,
  adjacent to J12.

## 344.6 — Connections

- [Block 336] §336.2 — the tree this member plugs into; unlike the typed leaves, it emits freely.
- [Block 337] §337.1 — `generateAndOutputJson` walks the tree, invoking this member's `process` and thus the Program.
- [Block 343] §343.1 — `getCurrentBase()` exposes the relative-schema per-row base to the Program.
- [Block 76] — the applied reuse (a Program→JSON→obix generator in chihuahua-rt); §344.4 remits to it.

## 344.7 — Self-verify

Block TYPE: **evidence** (small; code + `[CERT-doc]`). Inline full read of all three files (200 lines).
Load-bearing citations token-checked verbatim: `extends BAbstractInlineJsonWriter`/`extends BJsonSchemaMember`
(`BInlineJsonWriter.java:23`, `BAbstractInlineJsonWriter.java:12`), the `program` BProgram property
(`BInlineJsonWriter.java:19-24`), the `process`→`getProgram().invoke("override", this)` (`:45-52`), the empty
`processChildJsonMembers` (`BAbstractInlineJsonWriter.java:23-24`), and `BTypeOverride`'s child-Program run
(`BTypeOverride.java:47-49`). `[CERT-doc]` docs registered + preserved under `sources/manuals/jsonToolkit-docs/`.

`verify-block.sh` marker tally (computed):

| Marker | count (adj) |
|---|---|
| CERT (extern file:line) | 12 |
| CERT-doc | 4 |
| CERT-hw / CERT-live / CERT-web / CERT-a | 0 |
| INFER | 4 |
| INFER/CERT ratio | 0.25 |

`verify-block.sh` exit 0; `verify-sources.sh` no FABRICATED-CITE for B344.

Evidence block: `[INFER]`s are the escape-hatch reading and the whole-subtree-vs-single-value distinction, each
anchored to a cited `[CERT]`.
