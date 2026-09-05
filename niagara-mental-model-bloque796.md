# B796 · The `-ux` write-surface seam, captured as THE kit exemplar — DashboardPan-ux walked with file:line (document-mode §20)

> **Scope** (§20 document-mode): Tridium ships no clean SPA/servlet-split `-ux` exemplar (verdict THIN, [B791]),
> so the kit cannot point at a vendor module for "how to build a secure, testable dashboard servlet." This block
> captures OUR proven seam — `com.angeles.DashboardPan.ux` — as the reference the kit cites instead: the pure
> `route() → RouteAction` seam, the thin servlet adapter, the `checkCanWrite` RBAC gate, the `X-Requested-With`
> guard, and the five-gate write-surface checklist scored against real code with file:line. This CONSOLIDATES the
> deltas already proposed in [B762]/[B763] into one citable exemplar; it adds no new rule, it anchors them in a
> module that builds and ships.
>
> **Sources**: FUENTE 1 (own module, FUENTE PROPIA, [CERT] by file:line) —
> `Cliente/Leon-Guanjuato/Dashboard/DashboardPan/DashboardPan-ux/src/com/angeles/DashboardPan/ux/`:
> `DashboardDispatch.java`, `BDashboardServlet.java`, `DashboardRbacHelper.java`, `DashboardReader.java`, and
> `srcTest/.../DashboardDispatchTest.java`. Prior blocks: [B762] (test-seam taxonomy), [B763] (write-surface
> five gates), [B791] (web-tier THIN verdict), [B752] (RBAC OPERATOR_WRITE fail-closed contrast).

---

## 796.1 — The seam in one sentence `[CERT]`
Every request under `/dashboardpan/` becomes a `RouteAction` computed by a PURE function that takes only
`Function<String,String>` header/param lookups — never a live `WebOp` — and the servlet is a thin `instanceof`
adapter over it. That single choice is what makes the whole tier unit-testable off-station.

## 796.2 — The pure routing seam `route() → RouteAction` `[CERT]`
- `DashboardDispatch` is a package-private `final` class (`DashboardDispatch.java:30`); its `route(method,
  path, headerLookup, paramLookup)` is `static` and takes `java.util.function.Function` lookups
  (`:108-111`, import `:6`) — no Baja types cross the boundary, so a test drives it with plain `HashMap`s.
- The return type is a sealed-style nested hierarchy `RouteAction` with a private ctor (`:41-43`) and fixed
  cases: `Equipment`/`Alarms`/`SetpointWrite`/`StaticResource`/`Redirect`/`NotFound`/`MethodNotAllowed`
  (`:46-90`). Callers `instanceof`-check; the routing decision lives in ONE place.
- The servlet is the adapter: `doGet`/`doPost` build the two lookups and call `DashboardDispatch.route(...)`
  (`BDashboardServlet.java:91-102`, `:132-143`), then dispatch by `instanceof` (`:104-121`, `:145-153`).

## 796.3 — Off-station tests that actually bite `[CERT]`
`DashboardDispatchTest` has **14 `@Test`** (`DashboardDispatchTest.java`, `grep -c @Test = 14`), driven with
`noHeaders()`/`noParams()` maps — no station. They pin the security-relevant branches directly: `"/../../etc/passwd"
→ NotFound` (`:46-47`), backslash path → NotFound (`:55-56`), NUL-byte path → NotFound (`:64`). The traversal
rule is proven in a unit test, not asserted in a comment. This is the [B762] DUX1 seam, shipping.

## 796.4 — The five write-surface gates, scored on real code `[CERT]`
[B763] DWS1 says a mutating `-ux` endpoint needs five gates. DashboardPan-ux today meets **4 of 5**:

| # | Gate | Status | Evidence (file:line) |
|---|---|---|---|
| 1 | `checkCanWrite` is the FIRST line of the write handler; `OPERATOR_WRITE` fail-closed (deny on no-user / no-service / any exception) | ✅ met | `BDashboardServlet.java:198`; `DashboardRbacHelper.checkCanWrite:33`, no-user reject `:40-46`, 403 `:55-61`, `perms.has(OPERATOR_WRITE)` `:98`, `catch→return false` `:100-104` |
| 2 | Hand-rolled `X-Requested-With` guard INSIDE the pure `route()` (framework CSRF filter covers `/rpc/*` only) | ✅ met | `DashboardDispatch.java:123-126` (POST), `:144-147` (/api GET) → `Redirect(REDIRECT_HOME)` |
| 3 | Pin the write ORD under a `SERVICE_ORD` facade + reject traversal (no raw client ORD) | ✅ met | traversal reject `BDashboardServlet.java:222-223`; parent pinned `SERVICE_ORD + "/" + parentRel` `:241`, resolved+typechecked `:247-248` |
| 4 | Mutate under a per-Ord lock → HTTP **423** on contention | ❌ ABSENT | no `423`/lock in `BDashboardServlet.java` (targeted grep 0 hits) — the open punch-list item ([B763], issue #49) |
| 5 | Audit every mutation (who/what/when/old→new), fire-and-forget, audit-failure-never-fails-the-write | ✅ met | `BDashboardServlet.java:286-301` — one JSON-lines entry `:296`, `catch…(ignored)` `:299-301` |

The 4/5 score is the point: the exemplar is honest — it demonstrates the four gates the kit should REQUIRE and
names gate 4 as the residue (client punch-list, tracked in #49), so the kit teaches all five while pointing at a
real implementation of four.

## 796.5 — The anti-pattern the same module carries (teach the boundary) `[CERT]`
`DashboardReader` is `public final` (`DashboardReader.java:66`) but impure: 15+ `javax.baja.*` imports (`:6-20`)
and `buildEquipmentResponse(BComponent context)` takes a LIVE component (`:143`). It is the [B762] DUX2
anti-pattern — a data-shaper that could have injected the Baja touch as a `Function` but instead binds to the
station. The exemplar shows both: the testable seam (`DashboardDispatch`) and the un-testable shaper
(`DashboardReader`) side by side, so the kit can point at the contrast, not just the ideal.

## 796.6 — Kit implication `[INFER]`
`types/dashboard.md`'s "-ux testable seam" and "write-surface" sections should POINT AT THIS BLOCK
(DashboardPan-ux, file:line) as the worked exemplar, replacing the missing Tridium reference ([B791] THIN). The
kit rule stays [B763] DWS1 (five gates) + [B762] DUX1 (pure route seam); B796 is the "here is one that does it,
with tests" citation. Gate 4 (per-Ord lock/423) is documented as REQUIRED-but-not-yet-in-the-exemplar so the
kit does not teach a 4-gate ceiling.

## Self-verify

| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | `route()` is a pure static fn over `Function` lookups returning a `RouteAction` hierarchy; servlet is a thin instanceof adapter | [CERT] | DashboardDispatch.java:30,41-90,108-111; BDashboardServlet.java:91-102,132-153 |
| 2 | 14 off-station @Test pin traversal/backslash/NUL → NotFound | [CERT] | DashboardDispatchTest.java (@Test×14; :46-47,:55-56,:64) |
| 3 | Gate 1 met: checkCanWrite first line, OPERATOR_WRITE fail-closed on no-user/exception | [CERT] | BDashboardServlet.java:198; DashboardRbacHelper.java:33,40-46,55-61,98,100-104 |
| 4 | Gate 2 met: X-Requested-With guard inside pure route() | [CERT] | DashboardDispatch.java:123-126,144-147 |
| 5 | Gate 3 met: ORD pinned under SERVICE_ORD + traversal reject | [CERT] | BDashboardServlet.java:222-223,241,247-248 |
| 6 | Gate 5 met: audit fire-and-forget, failure never fails the write | [CERT] | BDashboardServlet.java:286-301 |
| 7 | Gate 4 (per-Ord lock/423) ABSENT — the open punch-list residue | [CERT — negative] | no 423/lock in BDashboardServlet.java (grep 0); B763; issue #49 |
| 8 | DashboardReader is the impure anti-pattern (live BComponent, 15+ baja imports) | [CERT] | DashboardReader.java:6-20,66,143 |

**Tally**: 7 [CERT] + 1 [CERT-negative]. No unmarked claims. §796.6 kit implication is [INFER].

## Connections
- **B762** (test-seam taxonomy — DUX1/DUX2 seams this exemplifies), **B763** (the five write gates this scores),
  **B791** (web-tier THIN — the reason the kit needs an OWN exemplar), **B752** (OPERATOR_WRITE fail-closed
  contrast vs vendor unrestricted), **B58**/**B507** (framework CSRF `/rpc/*` only — why gate 2 is hand-rolled).
  Kit: `types/dashboard.md`; client residue: issue #49 (gate 4).

## Open gaps
- **B796-G1** (requires-execution): gate 4 (per-Ord lock + HTTP 423) is unimplemented; the exemplar becomes 5/5
  only after the DashboardPan-ux change lands (issue #49). Until then the kit cites a 4/5 exemplar with gate 4
  marked REQUIRED.
