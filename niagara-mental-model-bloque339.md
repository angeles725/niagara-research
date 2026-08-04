# Block 339 — There is NO autonomous transport: jsonToolkit is a JSON marshaller, not a JSON pusher — the `output` slot is the product, read by consumers or saved to a file via the Workbench `BExporter`

> Focus **jsonToolkit** — evidence block J5. READ-ONLY. Corpus language: ENGLISH.
>
> Scope: how the generated JSON LEAVES the station. The audit flagged this as unknown ("BJsonExporter is an
> agent on BJsonSchema but no HTTP servlet / file writer / fox endpoint is visible"). This block RESOLVES it: the
> module ships NO autonomous transport; delivery is consumer-driven.
>
> Sources (primary, decompiled N4.14.0.162 + docs), read + two independent absence-grep passes by the driver:
> `organized/jsonToolkit/jsonToolkit-rt/vineflower/com/tridiumx/jsonToolkit/outbound/schema/support/exporter/BJsonExporter.java`
> (read in full) and `docJsonToolkit` (`Exporting-Json-6DCD8BED.html`, `ExportingJsonOutput-6EADCB50.html`).
>
> Markers: `[CERT]` local decompiled source (`file:line`) · `[CERT-doc]` official doc · `[INFER]` deduction.
> Block TYPE: **evidence** (includes a proven-absence finding).

---

## 339.1 — The finding: no autonomous transport (proven absence)

The module has **no HTTP servlet, no web-service, no fox push, no socket, no scheduled/autonomous file write**.
Two independent grep passes over the whole module (`rt` + `ux` + `wb`) returned nothing `[CERT]`:
- **tried** `BWebServlet` · `extends BServlet` · `IWebOp`/`doGet`/`doPost` · `HttpURLConnection` · `FileOutputStream`
  · `BFoxSession` · `sendRequest`/`postTo`/`uploadTo` → 0 hits (only `BJsonExporter`'s own `OutputStreamWriter`).
- **tried** `obix` · `BWebService`/`BIWeb` · `java.net.Socket` · `BHttp` · `scheduleExport`/`autoExport`/`pushJson`
  + `module.xml` servlet/web-service registration → 0 hits (the sole `javax.baja.web` import is a type-check in
  `JsonSchemaUtil.toJsonType`, not a transport).

So the JSON string produced by generation [Block 337] §337.1 sits in the schema's `output` String property
([Block 336] §336.1) and goes nowhere on its own `[INFER]`. There are exactly TWO exits, both PULL /
user-initiated.

## 339.2 — Exit 1: the `output` slot, read by any Niagara consumer

`output` is a plain public String slot with `getOutput()` `[CERT]` (`BJsonSchema.java:113`, `:143`). Anything
that can read a station slot can read the JSON: an obix client (`/obix/…/output`), a BQL read, a fox
subscription, or a browser bajascript app subscribing to the slot `[INFER]`. The doc points exactly here:
> "…allowing **subscription only to the output slot**, which can fetch the required data from the station." —
> `SubscriptionExamples-Json` [Block 337] §337.6 `[CERT-doc]`; and `Exporting-Json-6DCD8BED.html` frames output
> consumption around "subscription" + "output slot" + "bql" `[CERT-doc]`.

This is the SAME pattern [Block 76] used when it ported jsonToolkit's inline writer into `chihuahua-rt`: a
`String output` slot regenerated periodically and exposed over obix at `/obix/config/.../output` `[INFER]`
(cross-block) — jsonToolkit's native design IS that slot-plus-consumer model.

## 339.3 — Exit 2: `BJsonExporter` — the Workbench "Export" to a `.json` file

`BJsonExporter extends BExporter`, `@AgentOn(jsonToolkit:JsonSchema)` `[CERT]` (`BJsonExporter.java:20-25`) —
i.e. it plugs into Niagara's standard file-export framework (the right-click "Export" on a schema). It declares
file type `BJsonFile` and extension `json` `[CERT]` (`:33-39`), and `export(ExportOp op)` simply writes
`schema.getOutput()` to the op's output stream as UTF-8 `[CERT]` (`:41-48`):

```java
public void export(ExportOp op) {
   BJsonSchema schema = (BJsonSchema) op.get();
   try (OutputStreamWriter osw = new OutputStreamWriter(op.getOutputStream(), UTF_8)) {
      osw.write(schema.getOutput());
   } catch (IOException e) { log.warning(...); }
}
```

So this is a **user-initiated, one-shot** save of the current `output` to a file — not a scheduled or networked
push `[CERT]`. `ExportingJsonOutput-6EADCB50.html` is entirely about this "Export"→"file" workflow `[CERT-doc]`
("Export" ×14, "file" ×4).

## 339.4 — Architectural consequence: marshaller, not transport

jsonToolkit's module description is literally "**Utilities to help marshal niagara components to/from JSON**"
[Block 335] header — and the code matches: it GENERATES and HOLDS JSON, but the last mile (get it to an external
system) is someone else's job `[INFER]`. To actually deliver, you pair the `output` slot with a transport the
station already has: an **obix** read, a **BQL-over-fox** pull, a **bajascript** web app, or a **Program** that
reads `output` and POSTs it. This mirrors the email focus's "no test-send" shape [Block 331] §331.1 — the module
does the domain work, transport is external `[INFER]`. The INBOUND direction (J6/J7) is symmetric: it RECEIVES a
JSON string via an action, it does not itself listen on a socket `[INFER]`.

## 339.5 — What this block does NOT resolve

- How the INBOUND side receives JSON (the `route(BString)` action, selectors, handlers) → **J6/J7**.
- The `export marker` mechanism (a different concept — inbound registering ORDs for export tracking) → **J7**.
- The `ux` layer's `BJsonToolkitRpcUtil` RPC (a browser-facing read path, not an autonomous push) → **J13**.

## 339.6 — Connections

- [Block 337] §337.1 — generation writes `output`; this block shows `output` is the terminal product.
- [Block 336] §336.1 — the `output` slot declaration.
- [Block 76] — chihuahua's obix-exposed `output` slot: the consumer-pull pattern jsonToolkit natively uses.
- [Block 331] §331.1 (email) — the "module produces, transport is external" shape, recurring.

## 339.7 — Self-verify

Block TYPE: **evidence** with a PROVEN-ABSENCE finding (§339.1). The absence was established by TWO independent
grep passes with distinct term sets over all three artifacts + `module.xml`, each returning zero transport
mechanisms (the `tried:` clause is in §339.1). The positive facts were read verbatim: `BJsonExporter` full class
(`BJsonExporter.java:20-48`) and `output`/`getOutput()` (`BJsonSchema.java:113`, `:143`). `[CERT-doc]` from the
two Exporting docs (registered + preserved under `sources/manuals/jsonToolkit-docs/`).

`verify-block.sh` marker tally (computed):

| Marker | count (adj) |
|---|---|
| CERT (extern file:line) | 6 |
| CERT-doc | 4 |
| CERT-hw / CERT-live / CERT-web / CERT-a | 0 |
| INFER | 7 |
| INFER/CERT ratio | 0.70 |

`verify-block.sh` exit 0; `verify-sources.sh` no FABRICATED-CITE for B339. The 0.70 ratio is EXPECTED for a
PROVEN-ABSENCE block: there is no transport to document, so the positive evidence is small (the `BExporter` +
the `output` slot) and the value is the absence itself plus its architectural consequence — the investigable
evidence for "how JSON leaves" is exhausted by this block. `[INFER]`s are the consumer-pull enumeration and the
marshaller-not-transport reading, each anchored to the proven absence + the `BExporter` code + doc.
