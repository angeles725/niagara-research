# Block 619 — graphql-admin (SYNTHESIS / GQL-G9): reference architecture and build-vs-buy verdict for a GraphQL admin layer over Niagara N4

> **What**: The focus-closing synthesis. Consolidates GQL-G1–G8 (B611–B618) into a single reference
> architecture for a DIY GraphQL layer that administers a Niagara N4 station from a custom dashboard module,
> the consolidated security-invariant checklist, and an honest build-vs-buy verdict.
> **Scope**: synthesis only — every technical claim is REMITTANCE to the block that established it; this block
> adds the assembly, the invariant checklist, and the recommendation, not new evidence.
> **Block type**: DESIGN/APPLIED synthesis (a high `[INFER]` ratio is expected and correct — the parts are
> `[CERT]` in their own blocks). **Subject version**: Niagara N4.14.0.162 (Java 8).
> **Sources**: the focus corpus B611–B618 + the remitted framework blocks named inline. No new primary source.

---

## 619.1 — The one-line answer

**Niagara N4 has no native GraphQL, but a GraphQL admin layer is buildable today, 100% DIY, as a SEPARATE
module.** It is a translation shim over the servlet/RBAC/component APIs the station already exposes — it adds
GraphQL's schema ergonomics, not new capability. Every piece needed exists and is code-verified.

## 619.2 — Reference architecture (assembling G1–G8)

A self-contained `graphqlAdmin` module, three parts (the chihuahua [B163] shape):

**`graphqlAdmin-rt`** — the resolver host + engine:
- A `BGraphqlServlet extends BWebServlet`, `servletName="graphql-admin"` → mounts at `/graphql-admin/*`,
  live-registered ([B611] / GQL-G1). The framework hands it the session `Context` as
  `req.getAttribute("niagara.context")`; the `WebOp op` IS that `Context`.
- A bundled **graphql-java ≤ v20** engine ([B616] / GQL-G8 — Java-8 ceiling), shaded into a private package,
  loaded safely by the module's own isolated classloader ([B617] / GQL-G5).
- `DataFetcher` resolvers that, per field, run the call-site ([B614] / GQL-G4): `ord.resolve(op)` →
  `canRead()/canWrite()` gate ([B612] / GQL-G3) → BQL/slot read or `set`/`invoke`/`add` mutation → serialize
  with `com.tridium.json.JSONWriter` ([B76]). Mutations may instead dispatch to existing `@NiagaraRpc`
  methods ([B613] / GQL-G2), gaining the 4 built-in permission gates and 3-transport reach.

**`graphqlAdmin-ux`** — the dashboard front-end: a bajaux JS app (registered exactly like the native
`dashboard-ux` [B615] — a `BJsBuild`/`BCssResource`/`@AgentOn` widget, or a `BWebServlet`-served SPA) that
issues GraphQL queries/mutations to `/graphql-admin/`. A separate SIBLING module — it does NOT extend the
native `dashboard` module ([B615] / GQL-G6). The chihuahua frontend ([B170]/[B171]) is the closest working
precedent; [B47] is the headless-SPA bootstrap (SCRAM + CSRF).

**Subscriptions** — a `BGraphqlSubscriptionChannel extends BBoxChannel`, `getChannelName()="graphql"`,
auto-registered by `BBoxService` from the type registry ([B618] / GQL-G7). It rides the existing BOX
WebSocket transport ([B512]/[B554]) — no separate endpoint (Reflow's own-WS [B59] is the alternative, not a
requirement).

```
browser (graphqlAdmin-ux, bajaux JS)
   │  POST /graphql-admin/   (query/mutation)        WS: BOX "graphql" channel (subscription)
   ▼                                                    ▼
BGraphqlServlet (BWebServlet)  ── op = niagara.context ──►  BGraphqlSubscriptionChannel (BBoxChannel)
   │  graphql-java ≤v20 (shaded, own classloader)
   ▼
DataFetcher resolvers ── ord.resolve(op) ─► canRead/canWrite (OrdTarget) ─► BQL / set / invoke / @NiagaraRpc
   │                                                                         └─ all as the session user
   ▼  JSONWriter → JSON response
```

## 619.3 — Consolidated security invariants (the non-negotiables)

1. **Thread `op` into EVERY operation** ([B611]/[B614]). Never call a no-`Context` overload (`set(slot,val)`
   with null context) — it writes unattributed and skips RBAC ([B614] §614.2).
2. **Gate every field** with `OrdTarget.canRead()/canWrite()/canInvoke()` ([B612]).
3. **Fail-open caveat** ([B612] §612.3): an unprotected target returns `canRead()==true`. Do NOT treat that
   as "authorized" — apply an explicit resolver allowlist for non-`BIProtected` data.
4. **`@NiagaraRpc permissions="unrestricted"`** ([B613] §613.4) skips the object gate — a GraphQL bridge that
   forwards to `unrestricted` RPCs inherits their self-check assumption; verify each self-checks.
5. **Re-check `canRead()` on every subscription push** ([B618] §618.3), not only at subscribe — a mid-session
   category change must stop leaking.
6. **CSRF** on mutating POSTs ([B602]/[B58]); **SCRAM/auth** is the filter chain's ([B457]/[B510]).

## 619.4 — Build-vs-buy verdict `[INFER]`

- **No native GraphQL to buy** — the only trace in N4 is the `application/graphql` MIME constant. Everything
  is build.
- **GraphQL adds ergonomics, not capability.** N4 already exposes its data over oBIX ([B509]), BQL
  ([B514]), BOX live subscription ([B512]), Fox ([B513]), and `@NiagaraRpc` ([B507]). A GraphQL layer gives a
  typed schema, a single endpoint, and client-driven field selection — valuable for a bespoke SPA dashboard,
  but it resolves DOWN to those same primitives. It does not reach anything they cannot.
- **Cheapest working path**: for pure data-access a resolver could even loop back to the in-station oBIX
  server (`/obix/bql`, `/obix/config`) instead of the Java call-site — trading G1/G4 complexity for an HTTP
  hop and [B509]'s whole-tree-enumerable exposure.
- **Recommendation**: build the GraphQL admin layer ONLY if the team specifically wants GraphQL's schema/
  single-endpoint/subscription ergonomics for a custom dashboard SPA. Otherwise the existing oBIX + BOX +
  `@NiagaraRpc` surface administers the station with less code and no frozen-dependency risk (graphql-java
  ≤ v20 is EOL for security backports since ~mid-2024, [B616]). If built: keep it a thin, well-gated shim;
  the RBAC correctness (§619.3) is the hard part, not the GraphQL engine.
- **Effort/risk**: LOW-MEDIUM effort (all primitives exist, chihuahua [B163] is a working module precedent);
  the risks are the frozen graphql-java v20 dependency and the RBAC-threading discipline — both manageable.

## 619.5 — Connections

- Focus corpus: [B611] G1 · [B612] G3 · [B613] G2 · [B614] G4 · [B615] G6 · [B616] G8 · [B617] G5 · [B618] G7.
- Framework remittances: [B29]/[B163]/[B508] (servlet), [B457]/[B510]/[B602]/[B58] (auth/CSRF),
  [B11]/[B30]/[B561] (RBAC), [B5]/[B76]/[B408]/[B509]/[B514] (data), [B507]/[B536]/[B511] (mutations),
  [B512]/[B554]/[B59] (live), [B347]/[B12]/[B176] (build), [B216]–[B231]/[B47]/[B170]/[B171] (frontend).

---

## Self-verify

| # | Claim | Marker | Citation | Verified |
|---|---|---|---|---|
| 1 | GraphQL is buildable DIY as a separate module; adds ergonomics not capability | `[INFER]` | synthesis of B611-B618 | ✓ reasoned |
| 2 | Reference architecture rt/ux/subscription maps to G1-G8 blocks | `[INFER→CERT via B611-B618]` | each layer cited to its block | ✓ remittance |
| 3 | 6 consolidated security invariants | `[INFER→CERT via B611-B618/B602]` | each invariant cited | ✓ remittance |
| 4 | Verdict: build only for GraphQL ergonomics; oBIX/BOX/@NiagaraRpc otherwise suffice | `[INFER]` | build-vs-buy from remitted surfaces | ✓ reasoned |
| 5 | graphql-java v20 is a frozen (EOL-backport) dependency | `[CERT-web via B616]` | [B616] | ✓ remittance |

**Tally**: `[INFER]` = 3 · remittance-backed = 2. Block type = **SYNTHESIS** — a high `[INFER]` ratio is
correct here (every technical fact is `[CERT]` in its own block; this block only assembles). Focus
`graphql-admin` CLOSED: 8/8 investigable gaps closed (G1–G8), G9 synthesis delivered.
**No new primary source** — every claim remits to a focus or framework block; nothing re-derived.
