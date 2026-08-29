# Block 614 — graphql-admin (GQL-G4): the concrete resolver call-site — read (BQL/slot), mutate (set/invoke/add), and serialize to JSON, all as the session Context

> **What**: The end-to-end Java call-site a GraphQL resolver runs inside a `doPost(WebOp)` handler (or an
> `@NiagaraRpc` method body): resolve an ORD as the session user, gate it, read a value (a BQL table or a
> control-point slot), or mutate it (`set` a property / `invoke` an action / `add`/`remove` a component),
> then serialize the result to JSON. This assembles the primitives from GQL-G1/G2/G3 into the actual code a
> resolver executes.
> **Scope**: `javax.baja.sys.BComplex`/`BComponent` mutation+read API (the `Context`-taking overloads) and
> the `javax.baja.collection.BITable`/`TableCursor` iteration. The BQL query CONTRACT (`BBqlScheme`, resolve
> → `BITable`) is REMITTANCE to [B514]; the control-point read (`getOutStatusValue`) to [B76] §76.2; the
> `com.tridium.json.JSONWriter` API to [B76] §76.1; the writable-point priority-array write to [B536]. The
> Context provenance is [B611]/[B613]; the per-field gate is [B612].
> **Block type**: DESIGN/APPLIED synthesis (a call-site recipe) grounded in EVIDENCE signatures — a higher
> `[INFER]` ratio is EXPECTED and healthy here; the signatures are `[CERT]`, the assembly is `[INFER]`.
> **Subject version**: Niagara N4.14.0.162.
> **Sources**:
> - `organized/docSource/docSource-doc/extracted/baja/javax/baja/sys/BComplex.java` (Tridium original source)
> - `organized/docSource/docSource-doc/extracted/baja/javax/baja/sys/BComponent.java` (Tridium original source)
> - `organized/docSource/docSource-doc/extracted/baja/javax/baja/collection/{BITable,TableCursor}.java`
> **Method**: docSource-first (real signatures + javadoc). Markers: `[CERT]` = verbatim `file:line`;
> `[INFER]` = the call-site assembly / design guidance. Remitted primitives are cited to their owning block.

---

## 614.1 — Every mutating/reading API takes a `Context` — thread `op` into all of them `[CERT]`

The Baja component API exposes a `Context`-carrying overload for every state-changing and identity-sensitive
operation. Threading the session `Context` (`op` from [B611], or the injected `SecurableContext` from
[B613]) into that parameter is what makes the operation run AS the session user and inherit RBAC.

Property-value write (`BComplex`, `BComponent extends BComplex`) `[CERT]`:
- `set(Property property, BValue value, Context context)` (`BComplex.java:845`)
- `set(String name, BValue value, Context context)` (`:895`)
- `set(Property[] properties, BValue[] values, Context context)` (`:821`, batch)

Component structural mutations (`BComponent`) `[CERT]`:
- `add(String name, BValue value, int flags, BFacets facets, Context context)` (`:874`) + overloads (`:882,899`)
- `remove(String name, Context context)` (`:925`) · `remove(Property slot, Context context)` (`:941`) · `removeAll(Context)` (`:976`)
- `rename(Property, String, Context)` (`:1022`) · `reorder(Property[], Context)` (`:1118`) · `setDisplayName(Property, BFormat, Context)` (`:1062`)
- `setCategoryMask(BCategoryMask mask, Context cx)` (`:1944` — the RBAC-category admin op itself)

Action invocation (`BComponent`) `[CERT]`:
- `invoke(Action action, BValue argument, Context context)` (`:1229`, synchronous) · `post(Action, BValue, Context cx)` (`:1255`, async → `IFuture`)

## 614.2 — The footgun: the no-`Context` overloads bypass attribution `[CERT]`

`BComplex.set(Property property, BValue value)` exists and delegates to `set(property, value, null)`
`[CERT]` (`BComplex.java:854`). A `null` context means the write is NOT attributed to the session user and
the RBAC/audit path sees no principal. Several structural overloads have the same no-context form.

**Rule for a resolver** `[INFER]`: NEVER call the no-`Context` overload from request-handling code. Always
pass `op`. A resolver that calls `set(slot, value)` (no context) writes as the framework, silently
sidestepping the per-field `canWrite()` gate ([B612]) — the exact silent-escalation class named in
[B611] §611.3, now at the mutation call-site.

## 614.3 — Read call-site: BQL table + control-point slot `[CERT]`/remittance

**BQL query result** is a `BITable`, iterated by a `TableCursor`:
- `BITable.cursor()` returns a `TableCursor<T>` `[CERT]` (`BITable.java:44`).
- `TableCursor.row()` gives the current `Row`; `cell(Column column)` returns the cell value
  (`return row().cell(column)`) `[CERT]` (`TableCursor.java:26,31-33`). Iteration advances with the
  inherited `Cursor.next()`.

The resolve→`BITable` step (a `bql:` ORD resolved to a table) and the cursor lifecycle are REMITTANCE to
[B514]; the important point for G4 is that the ORD must be resolved with `op` so the table — and every row
`canRead()` decision inside it — is scoped to the session user.

**Control-point value read**: `((BControlPoint)ord.resolve(op).getComponent()).getOutStatusValue()` →
`BStatusValue`, then `getValueValue()` — REMITTANCE to [B76] §76.2.

## 614.4 — Serialize with `com.tridium.json.JSONWriter` (remittance) `[INFER]`

Result serialization uses `com.tridium.json.JSONWriter` (org.json repackaged in `nre.jar`, free on any
module classpath) — `new JSONWriter(StringWriter)` → `object()/key(k)/value(v)/endObject()` →
`toString()`, REMITTANCE to [B76] §76.1. A resolver walks its GraphQL field selection and, for each
`canRead()`-approved node, writes `key(fieldName).value(cellOrSlotValue)`. (jsonToolkit [B335]–[B349] is the
heavier bidirectional marshaller alternative; for a resolver the thin `JSONWriter` is enough.)

## 614.5 — The assembled resolver call-site `[INFER]`

```java
// inside doPost(WebOp op)  — op IS the session Context (B611)
// QUERY field:
OrdTarget t = someOrd.resolve(op);                 // scoped to session user (B612 §612.1)
if (!t.canRead()) { /* omit/null */ }              // per-field gate (B612)
BITable table = (BITable) t.get();                 // bql: ORD → table (B514)
TableCursor c = table.cursor();
JSONWriter w = new JSONWriter(sw);                 // B76 §76.1
w.array();
while (c.next()) { w.object().key("v").value(c.cell(col)); w.endObject(); }
w.endArray();

// MUTATION field:
OrdTarget mt = targetOrd.resolve(op);
if (!mt.canWrite()) { /* 403 */ }                  // B612
BComponent comp = mt.getComponent();
comp.set(slot, newValue, op);                      // BComplex.set(...,Context) — NEVER the null overload
// or: comp.invoke(action, arg, op);               // BComponent.invoke(...,Context)
// or: parent.add(name, value, flags, op);         // structural admin mutation
```

For a **writable point** the mutation is not a raw `set` but the priority-array write path
(`override`/`set`/`emergencyOverride` actions at a level) — REMITTANCE to [B536]/[B544]. For **async**
long-running admin ops use `post(action, arg, op)` → `IFuture` or a `BJob` ([B511]); recall the
[B613] §613.5 thread-local caveat — off-request-thread work must carry an explicit context, not the
request thread-local.

## 614.6 — Connections

- **[B611] (G1)** / **[B613] (G2)** — supply the `Context` (`op` / injected `SecurableContext`) this
  call-site threads into every `set`/`invoke`/`add`/`resolve`.
- **[B612] (G3)** — the `canRead()/canWrite()` gate that MUST bracket every read/mutation here.
- **[B514]** — BQL `bql:` ORD → `BITable` resolution + cursor lifecycle (the query primitive §614.3 uses).
- **[B76]** — §76.1 `JSONWriter` serialization; §76.2 control-point value read.
- **[B536]/[B544]** — the writable-point priority-array write (the correct mutation for a control point,
  not a raw `set`).
- **[B509]** — oBIX is the alternative "call-site": a resolver could loopback to `/obix/bql` instead of
  these Java APIs, trading G4's complexity for B509's whole-tree-enumerable exposure and an HTTP hop.
- Forward: **GQL-G5** (can a module bundle graphql-java to host these resolvers?), **GQL-G8** (Java-8
  library viability). The DATA-access half of the focus is now fully specified by G1–G4.

---

## Self-verify

| # | Claim | Marker | Citation | Verified |
|---|---|---|---|---|
| 1 | Property write: `BComplex.set(Property,BValue,Context)` (+ String/batch overloads) | `[CERT]` | BComplex.java:821,845,895 | ✓ token |
| 2 | No-Context `set(Property,BValue)` delegates to `set(...,null)` — no attribution | `[CERT]` | BComplex.java:854 | ✓ token |
| 3 | Structural mutations `add/remove/rename/reorder/setDisplayName` take Context | `[CERT]` | BComponent.java:874,925,941,1022,1118,1062 | ✓ token |
| 4 | `invoke(Action,BValue,Context)` sync; `post(...,Context)` async | `[CERT]` | BComponent.java:1229,1255 | ✓ token |
| 5 | `setCategoryMask(BCategoryMask,Context)` (RBAC-category admin op) | `[CERT]` | BComponent.java:1944 | ✓ token |
| 6 | `BITable.cursor()` → `TableCursor`; `cell(Column)` = `row().cell(column)` | `[CERT]` | BITable.java:44; TableCursor.java:26,31-33 | ✓ token |
| 7 | Resolver must thread `op` into every op; never the null-context overload | `[INFER]` | design rule from #1-#4 + [B611]/[B612] | ✓ reasoned |
| 8 | Assembled call-site: resolve(op)→canRead/canWrite→read/mutate→JSONWriter | `[INFER]` | synthesis of #1-#6 + remittances | ✓ reasoned |
| 9 | Writable-point mutation is the priority-array path, not a raw set | `[INFER→CERT via B536]` | [B536]/[B544] remittance | ✓ remittance |

**Tally**: `[CERT]` = 6 · `[INFER]` = 3 (one remittance-backed) · others = 0. **[INFER]/[CERT] ratio** ≈ 0.5
— block type = **DESIGN/APPLIED synthesis**, so this ratio is EXPECTED and healthy (§11), NOT an
exhaustion signal: the signatures are all `[CERT]`, the call-site assembly is the design contribution.
**Tokens checked**: 6 `[CERT]` signature groups read-confirmed in docSource (BComplex/BComponent/BITable/
TableCursor). Remitted primitives (BQL table, JSONWriter, control-point read, writable write) cited to
[B514]/[B76]/[B536], not re-derived.
