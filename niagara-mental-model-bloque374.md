# Niagara N4 — Bloque 374: `webChart` (W7) — the rt server: three query routes gated by `OrdTarget.canRead()` running as the session user, an RPC surface for cross-station metadata (Fox+web), a write-only file servlet; and a permission-guard DEFECT — `/schedule` and `/boxTable` call `sendError(404)` without `return`, falling through to encode the body (only `/data` hard-throws)

> **Focus**: `webChart`, gap **W7** (the rt server layer in depth). **Remittance**: [B199] §199.2 (the 3 servlet
> routes + `{t,v,r,s}` feed + version contract), [B367] (the `/data` feed carries no limit fields), [B369]
> (sampling is client-side — this block confirms the server does none).
>
> Subject: `webChart-rt` `WebChartQueryServlet`, `BWebChartQueryRpc`, `WebChartFileServlet`, `WebChartUtil`.
> **Method**: delegated sweep + **inline driver verify of the security fall-through and the no-server-sampling
> claims** (both read directly — the security claim is a framework-semantic + severity claim). Block type:
> **EVIDENCE**.

---

## §374.1 — Reads run as the session user, gated by `OrdTarget.canRead()` `[CERT]`

`WebChartQueryServlet` pulls the authenticated context from `req.getAttribute("niagara.context")` `[CERT]` `:104`
and every ORD is `.relativizeToSession().normalize()` `:123,132,146`, resolving under that user. Access control is
**`OrdTarget.canRead()`** (operator-read), not a servlet-level `requiredPermissions` declaration. `[CERT]`.

## §374.2 — The guard DEFECT: two of three routes fall through after `sendError` `[CERT]`

The `/data` (history) route enforces correctly — `if (!target.canRead()) throw new PermissionException();`
`[CERT]` `WebChartQueryServlet.java:176-178` (in `encodeHistoryData`). But `/schedule` and `/boxTable` do **not**:

```java
// /schedule (:138-141) and /boxTable (:152-155), identical shape:
if (!target.canRead()) {
    resp.sendError(404);          // sets status — but NO return / else
}
BControlSchedule schedule = (BControlSchedule) target.get();   // executes anyway
... encodeSchedule(writer, schedule, ...)                      // ... and streams the body
```

`[CERT]` `WebChartQueryServlet.java:138-143,152-157`. The `!canRead()` branch sets a 404 status but omits the
`return`, so control falls through to `target.get()` and the payload is encoded regardless. **The code-level guard
is structurally incomplete** `[CERT]`. Whether the body actually reaches an unauthorized client depends on servlet
**container commit semantics**: `sendError` typically commits and flushes the error response, after which the
later `getWriter()`/writes may throw `IllegalStateException` or be discarded — OR, if the buffer is large enough
and not yet committed, leak the schedule/table data. This is **not determinable statically** → child gap
**W7-G1 (requires-execution)**: reproduce a `/schedule` or `/boxTable` read as a user lacking read on the target
and observe whether the response body carries data. `[CERT]` (the omission) + `[INFER]` (the exploitability). Note
the blast radius is bounded: only schedule transitions and box/analytics tables — the bulk **history** feed
(`/data`) is correctly gated.

## §374.3 — The RPC surface: cross-station metadata over Fox+web `[CERT]`

`BWebChartQueryRpc` is a parallel path carrying **metadata/browse/permissions/settings — not the bulk sample
feed**. Every method is `@NiagaraRpc(permissions="unrestricted", transports={box, web})` `[CERT]` `:81+` —
`unrestricted` to *invoke*, but each does its own per-ORD `hasOperatorRead()` internally `[CERT]` `:96,112,149`.
`box`=Fox station-to-station (Supervisor→JACE), `web`=browser; the RPCs serve `getSourceList` (browse the
`history:` device tree), `getInfo` (capacities/facets/recordTypes/timezones), `getPermissions`, `getDisplayPath`,
and `getChartSettings` (read a `.chart`'s JSON). The point history itself stays on the servlet (web only).
`[CERT]`.

## §374.4 — `WebChartFileServlet` is write-only; no server sampling `[CERT]`

`WebChartFileServlet` is **`doPost` only** `[CERT]` `:54` — it SAVES chart definitions and CSV exports, gated by
`hasOperatorWrite()` up front + per-file on overwrite `:60-63,95-98`, with a `|`/`..` traversal filter
`:159-162`; accepts `.chart` (JSON) or `.csv`, else 406. Reading a `.chart` back is the `getChartSettings` RPC, not
here. `[CERT]`.

**No server-side sampling** `[CERT]` — confirming [B369]: `WebChartUtil` does no downsampling/decimation/averaging;
the only server reduction is a **boundary time-filter** (`encodeMinifiedGenericValueRecord` skips rows outside
`[start,end]`, `WebChartUtil.java:95-100`), applied on `/boxTable`; the `/data` history route streams the full
cursor with no cap `WebChartQueryServlet.java:199-207`. Values encode as `{t,v,r,s}` (`r`/`s` only when non-default)
under `Version("1")` ↔ `application/vnd.tridium.webChart-v1+json`. `[CERT]`.

---

## Self-verify

| # | Claim | Marker | Citation | Verified |
|---|---|---|---|---|
| 1 | Servlet runs as session user (`niagara.context`, relativizeToSession); gate = `OrdTarget.canRead()` | [CERT] | `WebChartQueryServlet.java:104,123,132` | ✅ read |
| 2 | `/data` hard-throws PermissionException on `!canRead()` | [CERT] | `WebChartQueryServlet.java:176-178` | ✅ read |
| 3 | `/schedule` + `/boxTable` call `sendError(404)` WITHOUT return → fall through to `target.get()` + encode | [CERT] | `WebChartQueryServlet.java:138-143,152-157` | ✅ read |
| 4 | Whether body actually leaks depends on container commit semantics of `sendError` → W7-G1 requires-execution | [INFER] | §374.2 reasoning | ✅ reasoned |
| 5 | RPC = cross-station metadata over Fox+web; `unrestricted` invoke + internal `hasOperatorRead` | [CERT] | `BWebChartQueryRpc.java:81,96,112,149` | ✅ sweep-cited |
| 6 | FileServlet write-only (doPost, hasOperatorWrite, traversal filter) | [CERT] | `WebChartFileServlet.java:54,60-63,159-162` | ✅ sweep-cited |
| 7 | No server-side sampling; only boundary time-filter; full cursor on `/data` | [CERT] | `WebChartUtil.java:95-100`; `WebChartQueryServlet.java:199-207` | ✅ read |

**Marker tally**: [CERT] ×6 · [INFER] ×1. Ratio ≈ 0.14. Block type = **EVIDENCE** (with a security finding).
Load-bearing re-resolved to disk: the `/data` hard-throw vs the `/schedule`+`/boxTable` fall-through (claims 2-3,
read directly — the security claim), and the no-server-sampling (claim 7). One child gap opened (W7-G1,
requires-execution). No §14.

## Connections

- [B199] §199.2 — the 3 routes + feed; §374 adds the permission enforcement + the guard defect.
- [B367]/[B369] — no limit fields in the feed, no server sampling — both confirmed here.
- [B360] — the alarm DB's operator-read gate; webChart's `canRead()` is the same posture (where enforced).

## Gaps opened / queued

**W7 closed** with one child gap: **W7-G1 (requires-execution)** — reproduce whether the `/schedule` or `/boxTable`
`sendError(404)`-without-return actually leaks the body to an unauthorized user (container-dependent). Logged in
RESEARCH-STATE as requires_execution.
