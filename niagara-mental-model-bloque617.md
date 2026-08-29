# Block 617 — graphql-admin (GQL-G5): module classloader isolation — a standalone module can safely bundle graphql-java

> **What**: The Niagara module classloader topology, answering whether a custom module can safely bundle a
> 3rd-party library (graphql-java) without conflicting with other modules. Answer: each `-rt` module JAR gets
> its OWN `ModuleClassLoader` (parent-first to boot/NRE, then its own + embedded-JAR classes, then only its
> DECLARED dependencies). A standalone module bundling graphql-java, declared by no other module, is fully
> isolated → safe.
> **Scope**: `com.tridium.sys.module.{ModuleClassLoader, ModuleExtClassLoader}` + `ModuleManager` loader
> instantiation. The build/toolchain is REMITTANCE to [B12]/[B176]; the version ceiling (Java 8 → graphql-java
> ≤ v20) is [B616] (GQL-G8). **Correction inherited**: this block's data came from a delegated sweep that
> mis-stated the runtime as Java 11; that is CORRECTED to Java 8 per [B616] §616.1 (class-file major 52,
> measured) — all version statements here use Java 8.
> **Block type**: EVIDENCE (code) + DESIGN verdict.
> **Subject version**: Niagara N4.14.0.162 (Java 8).
> **Sources**:
> - `organized/baja/baja/vineflower/com/tridium/sys/module/ModuleClassLoader.java`
> - `organized/baja/baja/vineflower/com/tridium/sys/module/ModuleExtClassLoader.java`
> - `organized/baja/baja/vineflower/com/tridium/sys/module/ModuleManager.java`
> **Method**: vineflower; every load-bearing `file:line` driver-VERIFIED (this block's sweep mis-stated the
> Java version, so its CODE citations were independently re-grepped per §11 — the code lines matched; only
> the Java-version inference was wrong and is corrected). Markers: `[CERT]` `file:line`; `[INFER]` = verdict.

---

## 617.1 — One `ModuleClassLoader` per module JAR `[CERT]`

`ModuleManager` creates a fresh loader per `NModule`: `m.classLoader = new ModuleClassLoader(m)` (or
`SyntheticModuleClassLoader` for synthetic modules) at load (`ModuleManager.java:320`), on reload
(`:259`), and on the direct load path (`:545`) `[CERT]`. There is no shared loader pool — each module JAR
has its own class namespace.

## 617.2 — Delegation order: parent-first → own/ext → declared deps `[CERT]`

`ModuleClassLoader`'s parent is the framework loader that loaded the module class —
`super(module.getClass().getClassLoader())`, `this.parent = module.getClass().getClassLoader()`
(`:80-81`) — i.e. the NRE/boot loader holding `nre.jar`, `com.tridium.*`, `javax.baja.*`. The class-find
order is `[CERT]`:
1. **parent first**: `return this.parent.loadClass(name)` (`:194-195`) — boot/NRE classes always win.
2. **embedded extJars** (3rd-party libs bundled in the module): `extClassLoadersByResourcePath` →
   `ModuleExtClassLoader` (`:201`, built at `:109-129`).
3. **the module's own `-rt` JAR**: `this.module.moduleFile.getJarEntry(path)` → `defineExtClass`/define
   (`:206`).
4. **declared dependencies only**: iterate `this.module.depends` (`:249,267,278`).

This is **parent-first, then module-local, then declared-dependency** — NOT a flat classpath and NOT
child-first. `ModuleExtClassLoader.nfind` delegates back to its module loader (`ModuleExtClassLoader.java:165,170`),
so ext classes win over dependency-module classes but never over the boot parent.

## 617.3 — Shared vs isolated `[CERT]`

| Class origin | Visibility | Mechanism |
|---|---|---|
| Boot / `nre.jar` (`com.tridium.*`, `javax.baja.*`, incl. `com.tridium.json`) | ALL modules (shared) | `parent.loadClass` first (`:194`) |
| A module's OWN `-rt` classes | that module only | `moduleFile.getJarEntry` (`:206`) |
| A module's embedded extJar (bundled 3rd-party) classes | that module only | `ModuleExtClassLoader` (`:201`) |
| Another module's classes | only if declared in `module.xml` `<dependency>` | `this.module.depends` loop (`:249-287`) |

Cross-module visibility is **dependency-gated**: a module reaches another's classes ONLY via a `module.xml`
`<dependency>`. There is no ambient classpath; undeclared modules are invisible.

## 617.4 — Verdict: bundling graphql-java is safe for a standalone module `[INFER]`

Because each module loads its bundled extJar into its OWN namespace, a **standalone `graphql-admin` module
that bundles graphql-java (≤ v20 per [B616]) and is declared as a dependency by no other module is fully
isolated** — no `LinkageError`/`ClassCastException` is possible; two different modules could even bundle
different graphql-java versions with no clash.

Failure mode (only if violated): if module A `<dependency>`-declares module B and both bundle graphql-java,
A's loader may resolve B's copy; casting a `graphql.schema.*` object created by A's own bundled version
against B's loaded class then throws `ClassCastException` (different `Class` objects, different loaders).
Avoided by NOT sharing graphql types across a module boundary. A self-contained resolver module never hits this.

## 617.5 — Gotchas for bundling a 3rd-party JAR `[CERT]`/`[INFER]`

- **Parent shadowing** `[INFER]`: the boot parent is tried first (`:194`), so if Tridium ships a class in
  `nre.jar` that collides with a graphql-java transitive dep (e.g. a Guava/commons version), the platform
  copy wins. graphql-java itself is not a Tridium dep; the risk is its transitives. **Shade** graphql-java
  and its deps into a private package (`com.myfirm.graphql.shaded.*`) to eliminate this and split-package
  sealing errors.
- **Signed-JAR check** `[CERT]`: `verifyJarEntrySignature` is enforced (`:226,374`; `ModuleExtClassLoader.java:165`)
  — for entries under `com/tridium/` or `javax/baja/` and for elevated-permission modules. Ordinary
  3rd-party extJar entries (other packages) are not subjected to the Tridium PKI chain unless TPK checking
  is requested. Bundling graphql-java's own classes is accepted; do not place them under a `com/tridium`
  or `javax/baja` path.
- **Class-file version** `[CERT via B616]`: the JVM is Java 8 (major 52) — a graphql-java v21+ (major 55)
  extJar throws `UnsupportedClassVersionError` at define time. Bundle ≤ v20 ([B616]).

## 617.6 — Connections

- **[B616] (GQL-G8)** — the Java-8 version ceiling (graphql-java ≤ v20); corrects this block's source-sweep
  Java-version claim (§14). §617.5's class-version gotcha depends on it.
- **[B347]** — the confirmed precedent that a Niagara `-wb` module bundles a 3rd-party JAR (Gson 2.9.0);
  B617 supplies the classloader model that makes it safe.
- **[B12]/[B176]** — the module build pipeline + `module.xml` dependency declaration (the `<dependency>`
  entries §617.3 gates on).
- Forward: **GQL-G7** (subscriptions) and **GQL-G9** (synthesis).

---

## Self-verify

| # | Claim | Marker | Citation | Verified |
|---|---|---|---|---|
| 1 | One `ModuleClassLoader` per NModule (`new ModuleClassLoader(m)`) | `[CERT]` | ModuleManager.java:259,320,545 | ✓ grep |
| 2 | Parent = boot/NRE loader (`module.getClass().getClassLoader()`) | `[CERT]` | ModuleClassLoader.java:80-81 | ✓ grep |
| 3 | Delegation: parent first → ext → own jar → declared deps | `[CERT]` | ModuleClassLoader.java:194,201,206,249-287 | ✓ grep |
| 4 | ext loader delegates back to module loader (not child-first over parent) | `[CERT]` | ModuleExtClassLoader.java:165,170 | ✓ grep |
| 5 | Cross-module visibility is `module.xml` dependency-gated | `[CERT]` | ModuleClassLoader.java:249-287 | ✓ grep |
| 6 | `verifyJarEntrySignature` enforced for com/tridium & javax/baja entries | `[CERT]` | ModuleClassLoader.java:226,374 | ✓ grep |
| 7 | Standalone module bundling graphql-java is isolated → safe | `[INFER]` | verdict from #1-#5 | ✓ reasoned |
| 8 | Shade to private package to avoid parent shadowing / split-package | `[INFER]` | deduction from #2/#3 | ✓ reasoned |
| 9 | Runtime is Java 8 (bundle ≤ v20), NOT Java 11 as the sweep assumed | `[CERT via B616]` | [B616] §616.1 (class major 52) | ✓ corrected |

**Tally**: `[CERT]` = 6 · `[INFER]` = 2 · `[CERT via B616]` = 1. **Ratio** ≈ 0.3. Block type = EVIDENCE. G5 closed.
**§11 note**: this block's data came from a delegated sweep that mis-stated the JVM as Java 11 (an inference,
not a miscited line). All CODE citations were independently re-grepped and CONFIRMED; the Java-version claim
was DE-ESCALATED and corrected to Java 8 via [B616] (direct class-file measurement). No other sweep claim
required correction.
**Tokens checked**: 6 `[CERT]` groups re-grepped against ModuleClassLoader/ModuleExtClassLoader/ModuleManager.
