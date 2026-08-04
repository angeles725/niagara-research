# Block 347 — The UI layers: bajaux field editors + one unrestricted-but-benign preview RPC (ux), and a Swing output widget that pretty-prints via bundled Gson with NO typed deserialization (wb)

> Focus **jsonToolkit** — evidence block J13 (low priority). READ-ONLY. Corpus language: ENGLISH.
>
> Scope: the two UI layers — `jsonToolkit-ux` (bajaux field editors + `BJsonToolkitRpcUtil`) and `jsonToolkit-wb`
> (Swing field editors + `BJsonOutputWidget` + `FormattedJsonParser`). The bundled `com.google.gson.*` (~65
> classes, Gson 2.9.0) is DISMISSED per the census — only THAT it ships and WHO uses it is documented.
>
> Sources (primary, decompiled N4.14.0.162 + doc), sweep sonnet + driver re-verification of the two security
> claims: `organized/jsonToolkit/jsonToolkit-ux/vineflower/com/tridiumx/jsonToolkit/ux/*`,
> `jsonToolkit-wb/vineflower/com/tridiumx/jsonToolkit/ui/*`, and `docJsonToolkit`
> (`VisualizationJson-9A09FB0F.html`).
>
> Markers: `[CERT]` local decompiled source (`file:line`) · `[CERT-doc]` official doc · `[INFER]` deduction.
> Block TYPE: **evidence**.

---

## 347.1 — Two UI layers over the same properties

The ux layer is bajaux (`BIJavaScript` singletons, each a `JsInfo` → `module://jsonToolkit/rc/fe/*.js`, bundled by
`BJsonToolkitJsBuild` which depends on `webEditors` + a CSS resource) `[CERT]` (`BJsonToolkitJsBuild.java:22`) — the
same webEditors-consumer pattern as email-ux [Block 333] `[INFER]`. The wb layer is Swing (`BWbFieldEditor`
dropdowns + a text widget). Both edit the SAME schema/query properties (query pick, query style, slot ORD, slot
selection, tag namespace) `[CERT]`. `jsonToolkit-wb` also SHIPS Gson 2.9.0 (bundled, DISMISSED) `[CERT]`, used
only by §347.3.

## 347.2 — `BJsonToolkitRpcUtil`: one RPC, unrestricted but benign

The ux RPC singleton (module.xml `JsonToolkitRpcUtil`) exposes exactly ONE method `[CERT]`
(`BJsonToolkitRpcUtil.java:34-57`):

```java
@NiagaraRpc(permissions = "unrestricted", transports = {@Transport(type = box)})
public static String retrievePreviewText(String queryResultWriterTypeSpec, Context cx) {
   Type t = Sys.getType(queryResultWriterTypeSpec);
   BObject o = t.getInstance();
   if (!(o instanceof BQueryResultWriter)) throw new BajaRuntimeException("Not a QueryResultWriter…");
   return ((BQueryResultWriter)o).previewText().toString(cx);
}
```

**`permissions = "unrestricted"` — no license/service/permission gate**; I grep-confirmed zero
license/permission checks in the ux own code `[CERT]`. So any authenticated session (box transport) can call it.
**But the operation is BENIGN** `[CERT]`: it instantiates the named type ONLY if it `instanceof BQueryResultWriter`
(the query-style writers [Block 342] §342.2 — harmless components), and returns that style's `previewText()`
(an abstract method returning a BString) `[CERT]` (`BQueryResultWriter.java:42`). `previewText()` is a static
SAMPLE of the style's SHAPE, not live station data `[INFER]`. So unlike the inbound export-registration [Block 341]
§341.4 (a real ACL gap), this RPC is a low-consequence preview helper: no station data, no arbitrary
instantiation beyond the `BQueryResultWriter` hierarchy `[INFER]`. (Framework-semantic caution applied: the
`unrestricted` flag alone is not a finding — the ACTION it guards is what matters, and here it is trivial.)

## 347.3 — Gson is pretty-print only — NO typed deserialization (verified negative)

`JsonWbStringUtil.indent(String)` is the sole Gson user `[CERT]` (`JsonWbStringUtil.java:9`, `:15-16`):
`JsonParser.parseString(json)` → a `JsonElement` tree, then `new GsonBuilder().setPrettyPrinting().create()
.toJson(element)` → pretty string. I grep-confirmed **NO `gson.fromJson(...)` anywhere in the wb own code** (0
hits) `[CERT]`. So Gson is used ONLY to tree-parse + re-serialize a station-generated string for display —
there is **no reflection-based typed deserialization**, so the classic Gson deserialization risk (gadget chains,
type confusion) does NOT apply here `[CERT]`. `FormattedJsonParser` (extends `TextParser`) touches no Gson at all
— it is a pure character-scan tokenizer that colors `{`/`[`/`"`/`:` in the Workbench text widget `[CERT]`
(`FormattedJsonParser.java`). Input to `indent()` is the station-sourced `output` `BString`, not user-pasted JSON
`[CERT]` (`BJsonOutputWidget.java:70`).

## 347.4 — The editor/widget catalog

**ux** (bajaux, each a `JsInfo` → `module://jsonToolkit/rc/fe/*.js`) `[CERT]`: `BJsonOutputEditor` (read-only
`output` display), `BQueryPicker`, `BQueryStylePicker`, `BSlotOrdEditor`, `BSlotSelectionTypeEditor`.
**wb** (Swing `BWbFieldEditor`) `[CERT]`: `BQueryPickerFE` (query-name dropdown), `BQueryStylePickerFE` (style
dropdown + hover preview — the RPC's consumer), `BSelectedSlotsFE` (slot-selection enum + manual list),
`BSlotOrdFE` (ORD text+browse), `BTagNamespaceFE` (queries `TagDictionaryService`), and `BJsonOutputWidget` (a
non-editable text view with `FormattedJsonParser` coloring + a toolbar: Generate / Copy / Clear / History /
Metrics / Pretty-toggle; Generate calls `schema.forceGenerateJson()` with a 30 s lease then polls) `[CERT]`
(`BJsonOutputWidget.java`). Doc: `VisualizationJson-9A09FB0F.html` §"Visualization" frames the ux/wb as the way
to view/visualize JSON query output `[CERT-doc]`.

## 347.5 — What this block does NOT resolve

- `com.google.gson.*` internals (bundled OSS, DISMISSED) — only Gson 2.9.0's presence + the single `indent()`
  use is documented.
- The `webEditors` framework the ux bundle depends on — the px-tail focus (planned), cross-referenced only.

## 347.6 — Connections

- [Block 333] (email-ux) — the same bajaux-over-`webEditors` shell pattern.
- [Block 342] §342.2 — the query styles whose `previewText()` the RPC returns.
- [Block 341] §341.4 — the inbound export-registration (a REAL ACL gap); §347.2's unrestricted RPC is the
  contrasting BENIGN case — the driver measured the action, not just the flag.
- [Block 336]/[Block 339] — the `output` slot the widget displays.

## 347.7 — Self-verify

Block TYPE: **evidence** (low priority; code + `[CERT-doc]`). Delegated sweep **sonnet**; driver re-verified the
two security claims directly (framework-semantic-check): the RPC `retrievePreviewText` body + `unrestricted` +
`instanceof BQueryResultWriter` guard + benign `previewText()` (`BJsonToolkitRpcUtil.java:34-57`,
`BQueryResultWriter.java:42`), and the Gson pretty-print-only usage with grep-confirmed ZERO `fromJson`
(`JsonWbStringUtil.java:15-16`). `[CERT-doc]` `VisualizationJson` registered + preserved under
`sources/manuals/jsonToolkit-docs/`.

`verify-block.sh` marker tally (computed):

| Marker | count (adj) |
|---|---|
| CERT (extern file:line) | 16 |
| CERT-doc | 3 |
| CERT-hw / CERT-live / CERT-web / CERT-a | 0 |
| INFER | 4 |
| INFER/CERT ratio | 0.21 |

`verify-block.sh` exit 0; `verify-sources.sh` no FABRICATED-CITE for B347.

Evidence block: `[INFER]`s are the benign-RPC and preview-is-sample readings — each anchored to a driver-verified
`[CERT]`. The two security claims are DE-escalations (unrestricted-but-benign RPC; Gson-but-no-fromJson), the
correct measured posture.
