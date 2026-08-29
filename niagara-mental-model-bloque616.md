# Block 616 — graphql-admin (GQL-G8): the Java-8 ceiling — a bundled GraphQL engine must be graphql-java ≤ v20 (or hand-rolled)

> **What**: The hard version constraint on any GraphQL engine a Niagara module can bundle. Niagara N4.14
> compiles to and runs on **Java 8**; graphql-java requires **Java 11 from v21 onward**. Therefore a bundled
> graphql-java must be **≤ v20.x**, or the library must be an alternative that still targets Java 8, or the
> resolver hand-rolls a minimal GraphQL parser. This is the "can you even ship it" feasibility gate.
> **Scope**: the Java-version constraint and the graphql-java release boundary. The bundling MECHANISM
> (classloader isolation) is GQL-G5 ([B617]); the module build/Java-8 toolchain is REMITTANCE to [B176].
> **Block type**: DESIGN/APPLIED (a feasibility constraint) grounded in one local `[CERT]` measurement + one
> `[CERT-web]` fact. Also carries a §14 correction of a delegated-sweep error.
> **Subject version**: Niagara N4.14.0.162 (Java 8). graphql-java boundary: v20.0 (Dec 2022) / v21.0 (Jul 2023).
> **Sources**:
> - LOCAL: class-file major-version of `organized/{baja/baja,web/web-rt}/extracted/.../*.class` (measured);
>   [B176] for the mandatory Java-8 toolchain.
> - WEB: `sources/web-snapshots/B616-graphql-java-java11-20260829.md` (preserved) ← GitHub discussion #3052
>   "GraphQL Java will require Java 11 going forward" (the official blog `graphql-java.com/blog/java-11-required/`
>   returned 403; the GitHub discussion is the §5 fallback with the same maintainer statement).
> **Method**: `[CERT]` = local measurement/file:line; `[CERT-web]` = the preserved web snapshot; `[INFER]` =
> the design verdict.

---

## 616.1 — Niagara N4.14 is Java 8 (class-file major 52) `[CERT]`

Directly measured on the core runtime class files (`major` field at byte offset 6-7 of the `.class`):
`javax.baja.sys.BComponent`, `javax.baja.web.BWebServlet`, and `com.tridium.sys.module.ModuleClassLoader`
are ALL **major version 52 = Java 8** `[CERT]` (measured on the `extracted/` bytecode). [B176] independently
records that the module toolchain requires Java 8 (`org.gradle.java.installations.paths → java-8-openjdk-amd64`,
`deploy.sh:33`) `[CERT]`. The station JVM is Java 8; a module JAR ships Java-8 bytecode (major 52).

**§14 correction of a delegated finding**: the GQL-G5 classloader sweep (feeding [B617]) asserted "Niagara N4
runs on Java 11 (target)". That is REFUTED here by direct class-file measurement (major 52, not 55) + [B176].
The Java version is Java **8**; [B617] is written with this corrected fact and back-points to this block.

## 616.2 — graphql-java requires Java 11 from v21 `[CERT-web]`

Per the preserved maintainer announcement (`sources/web-snapshots/B616-graphql-java-java11-20260829.md`,
GitHub discussion #3052, accessed 2026-08-29):
- **v20.0 (Dec 2022)** = the LAST release line supporting Java 8 as its minimum `[CERT-web]`.
- **v21.0 (Jul 2023)** = the FIRST version REQUIRING Java 11 `[CERT-web]`.
- Security backports run ~18 months post-release (v20 → into mid-2024) `[CERT-web]`.

A graphql-java v21+ JAR is Java-11 bytecode (class major 55); loading it under N4's Java 8 JVM throws
`UnsupportedClassVersionError` at class-define time (the same load-time failure [B617] §6 names for any
over-version library). So v20 is the ceiling `[INFER]`.

## 616.3 — Feasibility verdict `[INFER]`

Three viable options for the GraphQL engine inside a Niagara module, best-first:
1. **Bundle graphql-java ≤ v20.x** (recommended). v20 is a mature, full-featured line — schema definition
   (SDL + programmatic), query/mutation execution, `DataFetcher` resolvers, and subscriptions are all
   present. It is Java-8 bytecode → loads cleanly. Shade it into a private package to avoid split-package
   clashes with any Tridium-shipped transitive dep ([B617] §6 gotcha). Security backports ended ~mid-2024,
   so this is a FROZEN dependency — acceptable for an on-prem BMS module, but note the no-upstream-fixes risk.
2. **A Java-8-targeting alternative** — e.g. a GraphQL parser/executor that still publishes Java-8 artifacts,
   or `graphql-java-kickstart` pinned to a graphql-java ≤ v20 core. Same Java-8 constraint cascades to every
   transitive dependency.
3. **Hand-roll a minimal GraphQL layer** — parse a restricted GraphQL query subset and dispatch to the
   resolver call-site ([B614]). Viable because the admin surface is small and the resolver already maps
   fields → `OrdTarget`/`@NiagaraRpc`; a full engine may be overkill. Trades library risk for maintenance.

The Java-8 ceiling does NOT block the focus's thesis — it only pins the engine version. GraphQL-over-N4 is
buildable today with graphql-java v20.

## 616.4 — Connections

- **[B176]** — the module build pipeline + mandatory Java-8 toolchain (the constraint's origin).
- **[B617] (GQL-G5)** — the classloader isolation that makes bundling a specific graphql-java version safe;
  §616.1 corrects that block's Java-version claim (§14).
- **[B347]** — the confirmed precedent that a Niagara module CAN bundle a 3rd-party JAR (Gson 2.9.0 in
  jsonToolkit-wb); B616 adds the version-ceiling rule for graphql-java specifically.
- **[B614] (GQL-G4)** — the resolver call-site the hand-rolled option (3) would dispatch into.
- Forward: **GQL-G7** (subscriptions) and **GQL-G9** (synthesis).

---

## Self-verify

| # | Claim | Marker | Citation | Verified |
|---|---|---|---|---|
| 1 | Core N4 runtime classes are class-file major 52 (Java 8) | `[CERT]` | measured: BComponent/BWebServlet/ModuleClassLoader.class | ✓ od major=52 |
| 2 | Module toolchain requires Java 8 (`java-8-openjdk-amd64`) | `[CERT]` | [B176] §176 (deploy.sh:33) | ✓ remittance/read |
| 3 | graphql-java v20.0 (Dec 2022) = last Java-8 line | `[CERT-web]` | sources/web-snapshots/B616-…md (GitHub disc. #3052) | ✓ preserved |
| 4 | graphql-java v21.0 (Jul 2023) = first requiring Java 11 | `[CERT-web]` | same snapshot | ✓ preserved |
| 5 | v21+ (major 55) → UnsupportedClassVersionError under N4 Java 8 | `[INFER]` | deduction from #1/#4 | ✓ reasoned |
| 6 | Verdict: bundle graphql-java ≤ v20, or Java-8 alt, or hand-roll | `[INFER]` | design from #1-#5 + [B617]/[B347] | ✓ reasoned |
| 7 | §14: refutes the G5 sweep's "N4 runs Java 11" claim | `[CERT]` | #1 measurement (major 52) | ✓ measured |

**Tally**: `[CERT]` = 3 · `[CERT-web]` = 2 · `[INFER]` = 2. **Ratio** [INFER]/[CERT*] ≈ 0.4 — block type =
DESIGN/APPLIED (feasibility constraint), ratio expected. G8 closed.
**Tokens/evidence checked**: class-file major version measured directly (=52) on 3 core classes; web fact
preserved to `sources/web-snapshots/` and cited (load-bearing `[CERT-web]` per §5). §14 correction of [B617]
recorded; back-pointer added to that block.
**MCP/web snapshot gate (§5)**: the load-bearing `[CERT-web]` IS snapshotted to `sources/web-snapshots/` and
registered in SOURCES.md (Y).
