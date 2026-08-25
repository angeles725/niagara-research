# Block 515 — `apis` SYNTHESIS: the Niagara N4 API map as one system — four transports (web/Fox/BOX/oBIX) over one ORD address space and one auth spine, the `niagaraRpc` verb that spans three of them, and the security asymmetry between the closed RPC layer and the broad oBIX read surface

> **Focus:** `apis` — focus-closing synthesis (METHODOLOGY §8). Consolidates the 8 gap blocks [B507]–[B514]
> (API1–API8) and the REMITTANCE map that framed them. READ-ONLY. Block TYPE = **SYNTHESIS/DESIGN** (high
> `[INFER]` is expected — it cross-references cited evidence, adds no new source claims; every fact traces to a
> numbered block). Bootstrapped by a 2-agent AUDIT-FIRST (§13) from the request "investigate every N4 API".

## §515.1 — What the focus found `[CERT]`

The request was "all APIs"; the AUDIT-FIRST finding was that **the corpus already had a dedicated block for
almost every major surface** — so this focus documented only the 8 genuinely uncovered ones and mapped the rest
as REMITTANCE. The 8 new blocks:

| # | API | Block | One-line |
|---|---|---|---|
| API1 | `@NiagaraRpc` HTTP RPC | [B507] | annotation → `/rpc` servlet, JSON, 4-layer auth, closed-by-default |
| API2 | web-tier routing + auth | [B508] | servlet mount table + ORD→view dispatch + **N4 "Digest"=SCRAM** WWW-Authenticate |
| API3 | oBIX server | [B509] | N4 as oBIX host; `/obix/config`+`/obix/ord` = whole-tree read surface |
| API5 | `BAuthenticationScheme` SPI | [B510] | the framework contract to author a custom auth scheme |
| API7 | `BJob`/`BJobService` | [B511] | async jobs as live components on a ForkJoinPool |
| API4 | BOX wire | [B512] | JSON (not binary) live-subscription substrate; HTTP-poll v1 + WS-push v2 |
| API6 | Fox client API | [B513] | `BFoxProxySession` — open a Fox session programmatically |
| API8 | BQL/NEQL call + over-HTTP | [B514] | `bql:`/`neql:` ORD schemes; oBIX `/obix/bql` the only HTTP execute |

## §515.2 — Axis 1: four transports over one address space `[INFER]`

N4 has **four client transports**, and every one of them addresses the same station component tree by **ORD**:
- **Web/HTTP** ([B508]) — `/ord` (view render), `/rpc` (RPC [B507]), `/file`, `/obix` (oBIX [B509]); ORD carried
  in the URL path.
- **Fox** ([B513]/[B134]) — `BFoxProxySession` over TCP :1911 (TLS = Foxs); named channels (sys/file/user).
- **BOX** ([B512]) — the live-subscription substrate over HTTP-poll or WebSocket; `sub` takes ORDs.
- **oBIX** ([B509]) — REST/XML+SOAP; `/obix/ord/<ord>` resolves any ORD.

`[INFER]` **The ORD is the universal API address** — `bql:`/`neql:` schemes ([B514]), `/obix/ord`, the RPC
`{method, ord}` pair, BOX `sub` ORDs, and Fox `BOrd.resolve(session)` all name targets the same way. There is no
per-resource REST schema; there is one component space, four ways onto it.

## §515.3 — Axis 2: the `niagaraRpc` verb spans three transports `[CERT]`

The focus's unifying discovery: **one RPC verb, one server annotation, three client faces** —
`@NiagaraRpc(transports={web, box, fox})` ([B507]) declares which apply, and each maps to a concrete client:
- `web` → the `/rpc` servlet ([B507]);
- `box` → BOX `serverSideCall` ([B512]);
- `fox` → `BFoxProxySession.rpc()` → the Fox sys-channel `niagaraRpc` ([B513]).

`[INFER]` So `@NiagaraRpc` is a **transport-abstract RPC**: a module declares a method once and it is reachable
from the browser (web/box) and from another station (fox) without per-transport code. This is the modern
module-RPC path; the older per-mechanism RPC survives only in a fox-only legacy whitelist ([B507]).

## §515.4 — Axis 3: one auth spine `[CERT]`

Every transport authenticates through the **same primitive**: **N4 "Digest" = SCRAM-SHA-256** ([B508] found the
`WWW-Authenticate: SCRAM` handshake; [B134] the Fox side; [B510] the scheme framework). The `BAuthenticationScheme`
SPI ([B510]) is the single pluggable point — `getSchemeName()` becomes the on-wire token, the same scheme drives
web (`BHttpDigestCallbackHandler`), Fox, and (transitively) BOX/oBIX which ride the web tier. HTTP Basic exists but
is scheme-gated off by default ([B508] §14-refines [B290]). `[INFER]`: auth is transport-uniform — reason about it
once (SCRAM + RBAC), not per-API.

## §515.5 — Axis 4: the four functional roles `[INFER]`

The APIs partition cleanly by what they DO:
- **method call** → `niagaraRpc` (web/box/fox) ([B507]/[B512]/[B513]);
- **live subscription** → BOX (`ProxyBroker`, [B512]) — the only push channel;
- **REST data read/write** → oBIX ([B509]);
- **query** → `bql:`/`neql:` ORD schemes ([B514]), executable over HTTP only via oBIX `/obix/bql`;
- **async work** → `BJob` ([B511]), observed over Fox at its ORD.

## §515.6 — Axis 5: the security asymmetry (SEC feed) `[INFER]`

The focus surfaced a clear split, fed to [B398]/[B490]:
- **CLOSED-by-default:** the web `/rpc` RPC ([B507]) — session + CSRF + HTTPS + RBAC `Invoke`; a method opens only
  by declaring `permissions="unrestricted"`.
- **BROAD read surface:** the oBIX server ([B509]) — `/obix/config` (whole tree) + `/obix/ord` (any ORD) +
  `/obix/bql` (arbitrary query, [B514]) are gated only by `requiredPermissions="r"`, with no per-object export
  allowlist (only `/obix/continuousControl` is curated). `[INFER]` **A read-capable account can enumerate AND
  query the entire station over oBIX HTTP** — the surface [B458] used legitimately; mitigation is RBAC read-scoping
  + not licensing oBIX `export`. Cross-user isolation is enforced on BOX/oBIX watches ([B512]/[B509]) and Fox
  sessions ([B513]).

## §515.7 — Self-verify

| # | Claim | Marker | Citation | Verdict |
|---|---|---|---|---|
| 1 | 8 API surfaces catalogued (API1–API8), rest REMITTANCE | `[CERT]` | [B507]–[B514] (§515.1) | PASS |
| 2 | four transports (web/Fox/BOX/oBIX), all ORD-addressed | `[INFER]` | [B508],[B513],[B512],[B509] | PASS |
| 3 | niagaraRpc verb spans web/box/fox client faces under one @NiagaraRpc | `[CERT]` | [B507],[B512],[B513] | PASS |
| 4 | one auth spine: SCRAM (=N4 Digest) + BAuthenticationScheme SPI | `[CERT]` | [B508],[B510] | PASS |
| 5 | functional roles: rpc/subscribe(BOX)/rest(oBIX)/query(BQL)/async(BJob) | `[INFER]` | per-block | PASS |
| 6 | SEC asymmetry: closed RPC vs broad oBIX read/query surface | `[INFER]` | [B507],[B509],[B514] | PASS |

**Tally:** 6 claims — 3 rest on `[CERT]` block citations, 3 are `[INFER]` cross-cutting patterns (healthy for a
synthesis; no new source claims). This CLOSES the `apis` focus.

## §515.8 — Focus closure & open threads

- **`apis` STOPPED — 8/8 investigable gaps closed** (B507–B514 + this synthesis B515). REMITTANCE held: the ~30
  already-covered surfaces (Fox wire [B134], servlets/CSRF [B58], hx [B433], WebSocket [B59], SCRAM [B457],
  BajaScript/BOX-client [B36]/[B42], Baja SDK [B4], ORD [B5], api-access [B457]-[B458], …) were cited, not
  re-derived. nHaystack REST + BACnet-WS Annex-Q = proven-absent (not installed).
- **Recorded-not-seeded** (each in its block, none blocking): `BSysChannel` remote read/write internals ([B513]);
  the call-site-cookbook PARTIALs (Control/Point, History, Tag Java call APIs) that risked being REMITTANCE
  re-hashes.
- **Cross-block corrections this run:** §14 refine of [B290] (Basic scheme-gated, [B508]); the "BOX=binary" myth
  corrected ([B512]); [B494] extended with the auth-SPI author contract ([B510]).
- **NEXT:** §18 self-retrospective, then push. No read-only-investigable API surface remains.
