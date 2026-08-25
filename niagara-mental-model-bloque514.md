# Block 514 — `apis` API8: the BQL/NEQL CALL contracts + over-HTTP surfaces — the `bql:`/`neql:` ORD schemes (Java call → `BITable`/cursor), the ONE query-execution-over-HTTP face (oBIX `/obix/bql`, read-only-gated), the BOX `BqlRpc` AST-only utility, and the absence of a `/bql` servlet or any NEQL wire surface

> **Focus:** `apis`, gap **API8** (last) — the CALL contracts, not the grammar or engine. READ-ONLY, decompiled;
> no run. Markers §3. **REMITTANCE-checked GENUINE:** [B5]/[B21] cover the BQL/NEQL grammar, [B406] the EXECUTOR
> internals (`BLocalBqlResolver`/`SelectQuery`/`BogCursor`), [B263] the NEQL algebra — none documents how a caller
> INITIATES a query or the over-HTTP surface.
> **Sources:** FUENTE 3 — `organized/{bql,neql}/…-rt/decompiled/…`, `obixDriver` (`BBqlLobbyAgent`), `box`
> (`BBqlRpc`). FUENTE 1 — [B406] (engine), [B5] (grammar/ORD), [B509] (oBIX server), [B512] (BOX), [B507]
> (@NiagaraRpc). Evidence delegated to a `sonnet` REMITTANCE-aware sweep; ALL load-bearing file:line RE-VERIFIED.

## §514.1 — The Java call API: the `bql:` ORD scheme `[CERT]`

BQL is invoked as an **ORD scheme**, not a direct API. `javax.baja.bql.BBqlScheme` (`@NiagaraType(ordScheme="bql")`,
`:40`): `parse(text) → BqlQuery.make(text)` (`:57`); `resolve(base, query)` (`:65-66`) extracts the `BISession`
from the base (`toSession(base)`), looks up a `BIBqlResolver` via the agent registry (default
`BLocalBqlResolver.INSTANCE` — the [B406] engine), calls `resolver.resolve(session, base, query)`, and returns an
`OrdTarget` wrapping the result. **Call pattern for a module author:**
`BOrd.make("station:|slot:/|bql:select * from control:NumericPoint").resolve(session, cx)` → `OrdTarget.get()` is a
`javax.baja.collection.BITable`. **Result consumption:** `table.cursor()` → `TableCursor.next()` → `Row.cell(col)`
(`BqlRow.java:30`). **Supply side:** a component becomes a query source by implementing `BIRelational.getRelation(
predicate, cx) → BITable` — the interface the executor calls on scope objects.

## §514.2 — BQL over HTTP: three faces, only one executes `[CERT]`

- **(A) oBIX `/obix/bql` — the ONE real query-execution-over-HTTP surface.** `BBqlLobbyAgent` (lobby name `"bql"`,
  `:46-47`) `resolve()` builds `"station:|slot:/|bql:" + uri` (`:54`) and runs it through the standard ORD path,
  encoding the `BITable` result as oBIX XML. Concrete call:
  `GET /obix/bql/select%20*%20from%20control%3ANumericPoint` → full station-side BQL execution → oBIX XML. Gated
  `requiredPermissions = "r"` (`:30`).
- **(B) BOX `BBqlRpc` — AST utility ONLY, not execution.** `toSelect(bql)` (`@NiagaraRpc unrestricted`, box+web,
  `:46-47`) parses BQL text → a `BSelect` AST → BSON; `toBqlBody(bson)` (`:54`) reverses it. `[CERT]` These do NOT
  execute a query — they parse/serialize the AST (for the Workbench query-editor round-trip). Exposed over BOX+web
  but return no rows.
- **(C) No dedicated `/bql` web servlet** `[CERT negative]` — confirms [B508]: `web-rt` has no BQL mount. The only
  HTTP execution path is via oBIX (A).

## §514.3 — NEQL call + its missing wire `[CERT]`

`javax.baja.neql.BNeqlScheme` (`@NiagaraType(ordScheme="neql")`, `:22`) **extends `BQueryScheme`** (a different
superclass than BQL's `BOrdScheme`), `parse(body) → new NeqlQuery(body)` (`:38`); resolution dispatches through
`BQueryScheme` → the registered `BINeqlQueryHandler`/`BIQueryHandler`, yielding an entity set (relation traversal),
not a BQL `BITable`. Call pattern: `BOrd.make("station:|slot:/|neql:<predicate>").resolve(...)`. `[CERT negative]`
**NEQL has NO over-HTTP surface** — no oBIX lobby agent, no BOX RPC, no servlet; the deepest wire-reachable layer
is still the [B263] algebra. (Note: `@NiagaraRpc` on the tag-dictionary `BNeqlizeRpc` [B507 §507.5] exposes
tag-relation data, not a NEQL query endpoint.)

## §514.4 — Permissions `[CERT]`/`[INFER]`

- **oBIX BQL:** `requiredPermissions = "r"` (`BBqlLobbyAgent.java:30`) — `[INFER]` **a read-only account can
  execute arbitrary BQL over HTTP**, and there is no finer per-query gate. **This compounds [B509]:** oBIX's
  `/obix/config`+`/obix/ord` already expose the whole tree to a read-capable account; `/obix/bql` adds arbitrary
  *querying* of it — a read-only user can run `select … from …` station-wide. A sharp SEC feed to [B398]/[B490].
- **BOX `BqlRpc`:** `permissions="unrestricted"` — benign, consistent with its non-executing (AST-only) nature.
- **ORD/Java path:** no `BBqlScheme`-level RBAC; enforcement is delegated to the `BISession`/`Context` passed in
  and to each `BIBqlResolver` — a scheme-level open surface, gated only by the session's own permissions.

## §514.5 — Self-verify

| # | Claim | Marker | Citation | Verdict |
|---|---|---|---|---|
| 1 | BQL is called via the `bql:` ORD scheme (BBqlScheme parse/resolve→BIBqlResolver); result=BITable+cursor | `[CERT]` | `BBqlScheme.java:40,57,65-66`; `BqlRow.java:30` | PASS |
| 2 | oBIX /obix/bql = full BQL execution over HTTP (`station:|slot:/|bql:`+uri), gated "r" | `[CERT]` | `BBqlLobbyAgent.java:30,46-54` | PASS |
| 3 | BOX BqlRpc toSelect/toBqlBody = AST parse/serialize ONLY, unrestricted, not execution | `[CERT]` | `BBqlRpc.java:46-56` | PASS |
| 4 | no /bql web servlet (confirms B508) | `[CERT negative]` | web-rt grep = 0 | PASS |
| 5 | NEQL via `neql:` scheme (BQueryScheme/NeqlQuery); NO over-HTTP surface | `[CERT]`/`[CERT neg]` | `BNeqlScheme.java:22,38` | PASS |
| 6 | oBIX BQL read-only-gated → arbitrary station-wide query by a read account (compounds B509) | `[CERT]`+`[INFER]` | `BBqlLobbyAgent.java:30` | PASS |

**Tally:** 6 claims — all `[CERT]`/`[CERT negative]` load-bearing + `[INFER]` (read-only query exposure). Block
TYPE = **EVIDENCE**; API8 CLOSED. REMITTANCE-checked genuine vs B406/B5/B21/B263. All load-bearing tokens
re-verified inline.

## §514.6 — Connections & focus status

- **Delta over [B406]** (engine) / [B5]/[B21] (grammar): this is the caller's contract + the wire surface.
- **The over-HTTP query face is oBIX** ([B509]) — BQL execution is a lobby agent, not a first-class endpoint; the
  BOX face ([B512]) is only the editor's AST utility. Ties BQL into the API map: ORD-scheme (Java) → oBIX (HTTP
  execute) → BOX (AST edit).
- **SEC:** the read-only oBIX BQL gate compounds [B509]'s whole-tree exposure — feed to [B398]/[B490].
- **Focus status:** `apis` **8/8 — ALL investigable gaps closed.** NEXT = focus-closing SYNTHESIS + §18 retro + push.
