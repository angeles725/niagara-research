# Block 863 — Native Path B build recipe: minimal `BLoginTemplate` module (B834-G2)

> **Focus:** custom-login. **Closes:** B834-G2 — the native Path B build recipe: how to build
> a minimal, self-contained Niagara module that registers a custom `web:LoginTemplate` type,
> serves its own login assets via the module scheme, and activates in a station with zero
> `file:^` dependency. All evidence from framework sources and our own module tree; build
> knowledge from B12/B25/B834/B862 — not re-derived.
>
> **Sources consulted (all three, METHODOLOGY §6):**
> - **FUENTE 1 (corpus):** B834 §3.B / §4 (Path B, module-scheme best practice, 4.10u3 caveat);
>   B862 §2.5 (`LoginFileServlet` → `resourceToOrd()` → module:// pipeline); B12 (build system,
>   slotomatic, `niagara-module.xml`, `module-include.xml`); B25 (module.xml generation).
> - **FUENTE 2 (framework source):**
>   `organized/web/web-rt/vineflower/javax/baja/web/BLoginTemplate.java` · `BLoginTemplate.java`
>   `@NiagaraType` annotation · `organized/web/web-rt/extracted/META-INF/module.xml:20`
>   (type registration format, no `<agent>`) · `docSource/.../IStateLoginTemplate.java` ·
>   `docSource/.../ILoginTemplateEx.java` ·
>   `organized/docDeveloper/docDeveloper-doc/vineflower/doc/releaseNotes.txt` (min version
>   "Resolved In: 3.2.2") · `LoginServlet.java` (no IStateLoginTemplate check in N4.14).
> - **FUENTE 3 (our own modules):**
>   `sources/decompiled/nmxdomo-rt/com/niagaramods/nmxdomo/BDomoFloatingCard.java` (INSTANCE/TYPE
>   pattern) · `BDomoLoginTemplate.java:145-147` (`resourceToOrd` module:// pattern) ·
>   `Cliente/Leon-Guanjuato/Compresores/CompPan/niagara-module.xml` · `CompPan-rt/module-include.xml`
>   · `CompPan-rt/CompPan-rt.gradle.kts` · `Compresores/build.gradle.kts` ·
>   `DashboardPan-ux/DashboardPan-ux.gradle.kts:52` (`api(":web-rt")`) ·
>   `DashboardPan-ux/src/rc/` (resource directory layout) ·
>   `DashboardPan-ux/build/manifest/writeModuleXml/module.xml` (generated module.xml shape).
> - **niagara-help:** `niagara_help.py guide-search "login template"` → 0 results (guide available
>   only as docSource bajadoc, not a help-file route in this corpus). `find "web ord scheme"` → 0.
>   Zero = data, recorded here.

---

## 1. What the recipe produces

A signed, installable `MyLogin-rt.jar` that:
- Registers `MyLogin:MyLoginTemplate` as a `web:LoginTemplate` subtype (Workbench picker finds
  it automatically in the `WebService.loginTemplate` drop-down).
- Serves its static web assets (CSS, JS, images) via the module scheme
  (`module://MyLogin/rc/login/<file>`), meaning the browser never fetches `file:^` directly.
- Works on all N4 versions including 4.10u3+, where `file:^` login resources are forbidden for
  direct browser access (B834 §4 / the 4.10u3 lockdown does **not** affect module:// ORDs).

One `rt`-only part is sufficient; no `ux` or `wb` part is needed because the login page is plain
HTML rendered by the station servlet, not a Workbench view.

---

## 2. Module directory skeleton

```
MyLogin/                           ← Gradle project root
├── build.gradle.kts               ← vendor + signing plugins
├── settings.gradle.kts            ← subproject include
├── gradle.properties              ← niagara_home path
├── niagara-module.xml             ← module root descriptor (one part: rt)
└── MyLogin-rt/                    ← rt part
    ├── MyLogin-rt.gradle.kts      ← part build (plugins + deps)
    ├── module-include.xml         ← type list (Slotomatic input)
    └── src/
        ├── com/example/mylogin/
        │   └── BMyLoginTemplate.java   ← BLoginTemplate subclass
        └── rc/
            └── login/
                ├── login.html     ← the full login page (written by write())
                ├── login.css      ← custom stylesheet
                └── logo.svg       ← assets served at /login/login/...
```

`[CERT]` `src/rc/` layout mirrors `DashboardPan-ux/src/rc/` (confirmed: type
`DashboardPan-ux/src/rc/` exists in our module tree); `[CERT]` module:// path
`module://nmxdomo/rc/...` pattern from `BDomoLoginTemplate.java:145-147`.

---

## 3. `niagara-module.xml` (project root)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<niagara-module moduleName="MyLogin" preferredSymbol="MYLOG" runtimeProfiles="rt"/>
```

`[CERT]` Direct copy of the `CompPan/niagara-module.xml` pattern
(`Cliente/Leon-Guanjuato/Compresores/CompPan/niagara-module.xml`):
`<niagara-module moduleName="CompPan" preferredSymbol="COMPAN" runtimeProfiles="rt"/>`.
Single profile means one jar: `MyLogin-rt.jar`.

> **`preferredSymbol`** becomes the module namespace. Choose it to match the first half of
> the `BTypeSpec` that goes into `WebService.loginTemplate`, e.g. `MYLOG:MyLoginTemplate`.

---

## 4. `module-include.xml` (inside `MyLogin-rt/`)

```xml
<types>
  <!--com.example.mylogin-->
  <type class="com.example.mylogin.BMyLoginTemplate" name="MyLoginTemplate"/>
</types>
```

`[CERT]` Format mirrors `CompPan-rt/module-include.xml` exactly:
```xml
<types>
  <!--com.angeles.CompPan-->
  <type class="com.angeles.CompPan.BCompressorControl" name="CompressorControl"/>
</types>
```

There is **no `<agent>` block** needed on this type entry. `[CERT]` Evidence: the stock
`BLoginTemplate` entry in `web-rt/extracted/META-INF/module.xml:20` has no agent either:
```xml
<type class="javax.baja.web.BLoginTemplate" name="LoginTemplate"/>
```
Workbench's `BTypeSpec` picker for `WebService.loginTemplate` (target type `web:LoginTemplate`,
B834 §1) resolves all registered subtypes automatically — no agent registration is required.

From B12 (corrected by B631): `module-include.xml` is **read** by Slotomatic; the `<type>`
entries are created manually (or by the NewDriverWizard) and are not generated by an annotation
processor. `[CERT-doc]` B12 §12.1.8.

---

## 5. Java source: `BMyLoginTemplate.java`

```java
package com.example.mylogin;

import java.io.IOException;
import java.io.PrintWriter;
import javax.baja.naming.BOrd;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.web.BLoginTemplate;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

// @NiagaraType annotation drives Slotomatic to generate INSTANCE + TYPE into this file.
// [CERT] Pattern: BLoginTemplate.java uses @NiagaraType; Domo output shows INSTANCE + TYPE
// fields in each concrete subclass (BDomoFloatingCard.java).
@NiagaraType
public final class BMyLoginTemplate extends BLoginTemplate {

  /*+ ----- BEGIN SLOTOMATIC GENERATED (will be filled in by `gradlew slotomatic`) --------- +*/
  public static final BMyLoginTemplate INSTANCE = new BMyLoginTemplate();
  /*+ TYPE is the baja type metadata, required by the Niagara type system                     +*/
  public static final Type TYPE = Sys.loadType(BMyLoginTemplate.class);
  /*+ ----- END SLOTOMATIC GENERATED -------------------------------------------------------- +*/

  @Override
  public Type getType() { return TYPE; }

  /**
   * Emit the full HTML login page.
   * [CERT] Contract from BLoginTemplate.java javadoc: the form must contain exactly:
   *   <form method='post' action='/j_security_check'>
   *     <input type='text' name='j_username' />
   *     <input type='text' name='j_password' />
   *     <input type='submit' name='submit' />
   *   </form>
   * Attributes may be added (style, class), but names / method / action must not change.
   * CSRF token is injected by auth.min.js; do not add it manually.
   */
  @Override
  public void write(HttpServletRequest req, HttpServletResponse resp)
      throws IOException, ServletException {
    resp.setContentType("text/html;charset=UTF-8");
    PrintWriter out = resp.getWriter();
    // Recommended: read login.html from the module jar and stream it, rather than
    // hard-coding HTML here. [INFER]
    BOrd template = BOrd.make("module://MYLOG/rc/login/login.html");
    // ... resolve and stream template bytes, OR write inline HTML:
    out.println("<!DOCTYPE html><html><head>");
    out.println("<link rel='stylesheet' href='/login/login/login.css'>");
    out.println("</head><body>");
    out.println("<form method='post' action='/j_security_check'>");
    out.println("<input type='text' name='j_username'/>");
    out.println("<input type='text' name='j_password'/>");
    out.println("<input type='submit' name='submit' value='Login'/>");
    out.println("</form>");
    out.println("<script src='/login/core/auth.min.js'></script>");
    out.println("</body></html>");
  }

  /**
   * Map /login/<path> → a module:// ORD served by LoginFileServlet via
   * UnauthenticatedCache. [CERT] Pattern from BDomoLoginTemplate.java:145-147.
   *
   * Example: path="/login/login.css" → "module://MYLOG/rc/login/login.css"
   * which is served from the jar at rc/login/login.css.
   *
   * LoginFileServlet calls resourceToOrd(pathInfo) where pathInfo has already
   * had "/login" stripped, so path arrives as "/login/login.css". [CERT] B862 §2.5.
   */
  @Override
  public BOrd resourceToOrd(String path) {
    return BOrd.make("module://MYLOG/rc" + path);
  }
}
```

**Optional interfaces** (`IStateLoginTemplate` and `ILoginTemplateEx`, both added in 2015):
- `IStateLoginTemplate.write(service, req, resp, LoginState state)` — renders per login state.
- `ILoginTemplateEx.processLoginGet/Post(...)` — intercepts GET render and POST submit.

`[CERT]` Neither interface is checked by `LoginServlet.java` in N4.14 (grep finds no reference
to `IStateLoginTemplate`/`ILoginTemplateEx` there). `[CERT]` Domo's three subclasses do NOT
implement either interface and work fully in N4.14 (B862 §1.1). They are opt-in enhancements, not
required for a functional login. Implement `IStateLoginTemplate` if you need per-state rendering
(error / force-reset / 2FA token) without session-attribute inspection.

> **`NCCB-4026` note:** The bug entry "Custom BLoginTemplates must implement IStateLoginTemplate"
> appears in the AX 3.8 release changes (`docDeveloper/.../doc/changes.txt`) — an AX-era
> enforcement. In N4.14, `LoginServlet` does not enforce this. `[CERT]` grep of
> `LoginServlet.java` for `IStateLoginTemplate` → 0 hits.

---

## 6. Root `build.gradle.kts`

```kotlin
plugins {
  id("com.tridium.niagara")
  id("com.tridium.vendor")
  id("com.tridium.niagara-signing")
  // If niagara_home repos convention plugin is available in your plugin set:
  // id("com.tridium.convention.niagara-home-repositories")
}

vendor {
  defaultVendor("MyOrg")
  defaultModuleVersion("1.0")   // becomes vendorVersion in module.xml [CERT] B12/memory key
}

subprojects {
  repositories { mavenCentral() }
}
```

`[CERT]` Plugin set and `vendor {}` block pattern from
`Cliente/Leon-Guanjuato/Compresores/build.gradle.kts`.

The `defaultModuleVersion()` call sets the `vendorVersion` attribute in the signed module.xml
artifact — the key used for upgrade detection and dependency pinning (B12; memory:
`client-module-version-key-location`).

---

## 7. `MyLogin-rt/MyLogin-rt.gradle.kts` (part build script)

```kotlin
import com.tridium.gradle.plugins.bajadoc.task.Bajadoc
import com.tridium.gradle.plugins.module.util.ModulePart.RuntimeProfile.*

plugins {
  id("com.tridium.niagara-module")
  id("com.tridium.niagara-signing")
  id("com.tridium.bajadoc")
  id("com.tridium.niagara-jacoco")
  id("com.tridium.niagara-annotation-processors")
  id("com.tridium.convention.niagara-home-repositories")
}

description = "Custom login template for MyOrg"

moduleManifest {
  moduleName.set("MyLogin")
  runtimeProfile.set(rt)
}

dependencies {
  nre(":nre")
  api(":baja")
  api(":web-rt")                                           // BLoginTemplate, BOrd, BWebService
  compileOnly("javax.servlet:javax.servlet-api:3.1.0")    // HttpServletRequest/Response
}

tasks.named<Bajadoc>("bajadoc") {
  includePackage("com.example.mylogin")
}
```

`[CERT]` `api(":web-rt")` and `compileOnly("javax.servlet:javax.servlet-api:3.1.0")` from
`DashboardPan-ux/DashboardPan-ux.gradle.kts:52`. Part plugin set from
`CompPan-rt/CompPan-rt.gradle.kts`.

> **Dependency version in generated module.xml:** `api(":web-rt")` resolves the version from
> whatever is installed in `niagara_home`. Building against N4.14 produces
> `<dependency name="web-rt" vendorVersion="4.14"/>`. If backward compat to N4.10 is required,
> build against the 4.10 installation. `[INFER]`

---

## 8. Build and sign steps

```bash
# From the MyLogin/ project root:
./gradlew :MyLogin-rt:slotomatic   # Slotomatic: reads module-include.xml + @NiagaraType
                                   # annotations; generates INSTANCE/TYPE boilerplate inside
                                   # BMyLoginTemplate.java (between SLOTOMATIC markers).
                                   # [CERT-doc] B12 §12.1.8

./gradlew :MyLogin-rt:jar          # Compiles + bundles: classes + src/rc/* → MyLogin-rt.jar
                                   # rc/ resources land at the jar root as rc/login/*.
                                   # Generated META-INF/module.xml included.

./gradlew :MyLogin-rt:sign         # Signs the jar (PKCS7, com.tridium.niagara-signing).
                                   # Produces MyLogin-rt.jar (signed in-place or dist/).
                                   # [CERT-doc] B18 §18.4 (module signing mechanics)

# Shortcut: all three together (sign depends on jar, jar depends on slotomatic):
./gradlew :MyLogin-rt:sign
```

`[INFER]` Task sequence (Slotomatic → jar → sign) derived from Gradle dependency graph described
in B12; the sign task's jar-dependency is the standard `com.tridium.niagara-signing` behavior
(`[CERT-doc]` B18).

> **Force-slotomatic after source changes:** `./gradlew :MyLogin-rt:slotomatic --rerun-tasks` if
> you edit `module-include.xml` and the incremental build does not re-trigger. `[CERT-doc]` B12
> §12.1.8 (`--force` flag equivalence).

---

## 9. Deploy: install the jar

```bash
# Option A — local station (same machine):
cp build/libs/MyLogin-rt.jar  $NIAGARA_HOME/modules/
# restart the station (or the platform daemon if autoload=true).

# Option B — remote station (JACE / supervisor):
# Use Workbench > Software Manager to push MyLogin-rt.jar via the fox tunnel.
# [CERT-doc] B834 §5 (Domo install procedure mirrors this: "copy to modules dir
# local / push via Software Manager remote; relaunch Workbench; restart station").
```

`[INFER]` "Relaunch Workbench" is needed so that Workbench loads the new type into its class
loader before attempting to set `WebService.loginTemplate`. `[CERT-doc]` Domo README states the
same requirement (B834 §5).

---

## 10. Select the template in the station

1. In Workbench, navigate to **Config > Services > WebService** (or wherever WebService lives in
   the station hierarchy).
2. Open the **Property Sheet** for WebService.
3. Find the **Login Template** property (`loginTemplate`, type `BTypeSpec`, default null =
   `DefaultLoginTemplate`). `[CERT]` B834 §1 / `BWebService.java:243-249`.
4. Uncheck "null" / click the type picker. Two drop-downs appear:
   - First drop-down: module → select **MyLogin** (the `preferredSymbol` / module name).
   - Second drop-down: type → select **MyLoginTemplate**.
5. Save. The station immediately uses your `write()` method for subsequent `GET /login` requests.
   No station restart required. `[INFER]` (consistent with how BTypeSpec works in a live station —
   `BLoginTemplate.getLoginTemplate()` resolves the spec on each request; B834 §1).

The resulting `BTypeSpec` value is `MYLOG:MyLoginTemplate` (module symbol + `:` + type name).
To revert, set the property back to null / "Any".

---

## 11. How assets reach the browser (`module://` pipeline)

```
Browser GET /login/login/login.css
    └─► LoginFileServlet.doGet("/login/login.css")    [CERT] B862 §2.5
            └─► BLoginTemplate.getLoginTemplate()
                    .resourceToOrd("/login/login.css")
                    = BOrd.make("module://MYLOG/rc/login/login.css")
            └─► UnauthenticatedCache.allowOrd() whitelist check
            └─► resolve & stream bytes from MyLogin-rt.jar!/rc/login/login.css
```

`[CERT]` `LoginFileServlet.java:68-75` (B862 §2.5) and `BDomoLoginTemplate.java:145-147` (module
scheme pattern). This pipeline does NOT touch `file:^`, so the 4.10u3+ lockdown is irrelevant.

The stock auth engine is available at `/login/core/auth.min.js` (served by
`LoginFileServlet` from NRE jetty rc; path starts `/core/...` which routes to `nre` rc — B862
§2.5). Include it in your HTML to get digest/SCRAM authentication without re-implementing the
auth protocol.

---

## 12. Version and compatibility notes

| Item | Detail | Marker |
|------|--------|--------|
| `BLoginTemplate` API first available | AX 3.2.2 ("Resolved In: 3.2.2", release notes 9065) | [CERT] |
| N4 availability | Inherited from AX; present in all N4 versions including 4.10 and 4.14 | [CERT] |
| `IStateLoginTemplate` / `ILoginTemplateEx` | Added 2015 (copyright date in source); optional — not checked by N4.14 `LoginServlet` | [CERT] |
| 4.10u3 `file:^` restriction | Does NOT affect module:// resources — served server-side by LoginFileServlet | [CERT] B834 §4 / B862 §2.5 |
| Minimum N4 for building with our Gradle stack | N4.10 (nmxdomo deps web-rt 4.10; B834 §5) — build against 4.10 for widest compat | [CERT] / [INFER] |
| Client version in this corpus | N4.14 (PANCCADIA station); building against 4.14 produces tighter dep in module.xml | [CERT] |
| `BSingleton` concrete subclass | Must have `INSTANCE = new ...` and `TYPE = Sys.loadType(...)` | [CERT] Domo pattern |

---

## Self-verify

| # | Claim | Marker | Evidence |
|---|-------|--------|----------|
| 1 | `BLoginTemplate` is abstract `BSingleton`; two abstract methods: `write(req,resp)` + `resourceToOrd(path)` | [CERT] | `organized/web/web-rt/vineflower/javax/baja/web/BLoginTemplate.java` |
| 2 | `@NiagaraType` annotation present on `BLoginTemplate`; drives Slotomatic INSTANCE/TYPE generation | [CERT] | Same file, line 14 |
| 3 | Concrete singleton pattern: `INSTANCE = new BMyLoginTemplate(); TYPE = Sys.loadType(...)` | [CERT] | `sources/decompiled/nmxdomo-rt/.../BDomoFloatingCard.java` |
| 4 | Type registration in `module-include.xml`: plain `<type class="..." name="..."/>`, no `<agent>` block | [CERT] | `CompPan-rt/module-include.xml`; `web-rt/extracted/META-INF/module.xml:20` |
| 5 | `module-include.xml` is read (not written) by Slotomatic; `<type>` entries are added manually | [CERT-doc] | B12 §12.1.8 (B631 corrected) |
| 6 | `niagara-module.xml` root format for a single-rt-part module | [CERT] | `CompPan/niagara-module.xml` |
| 7 | Part `.gradle.kts`: `api(":web-rt")` + `compileOnly("javax.servlet:javax.servlet-api:3.1.0")` | [CERT] | `DashboardPan-ux/DashboardPan-ux.gradle.kts:52` |
| 8 | Web assets in `src/rc/`; land at jar root as `rc/<path>` | [CERT] | `DashboardPan-ux/src/rc/` directory confirmed |
| 9 | `resourceToOrd(path)` returns `BOrd.make("module://<symbol>/rc" + path)` | [CERT] | `BDomoLoginTemplate.java:145-147` |
| 10 | `LoginFileServlet` routes `resourceToOrd()` result through UnauthenticatedCache; module:// works unauthenticated | [CERT] | B862 §2.5 / `LoginFileServlet.java:68-75` |
| 11 | No agent registration needed; BTypeSpec picker for target type `web:LoginTemplate` auto-discovers subtypes | [CERT] | `BWebService.java:243-249` (B834 §1); `web-rt/module.xml:20` |
| 12 | `IStateLoginTemplate` / `ILoginTemplateEx` are optional in N4.14 (LoginServlet does not check) | [CERT] | `LoginServlet.java` grep → 0 hits |
| 13 | NCCB-4026 "must implement IStateLoginTemplate" is an AX 3.8 bug, not a N4 requirement | [CERT] | `changes.txt` context (AX-era list) |
| 14 | `BLoginTemplate` API introduced in AX 3.2.2 (present in all N4 versions) | [CERT] | `releaseNotes.txt` "Resolved In: 3.2.2" |
| 15 | `module://` resources not affected by 4.10u3+ `file:^` restriction (browser never fetches `file:^`) | [CERT] | B834 §4 / B862 §1.3 |
| 16 | Build task sequence: slotomatic → jar → sign; sign depends on jar | [CERT-doc] + [INFER] | B12 §12.1.8; B18 §18.4 |
| 17 | Activating: `WebService.loginTemplate` BTypeSpec → module:type picker; no station restart needed | [CERT] / [INFER] | B834 §1; `BLoginTemplate.getLoginTemplate()` per-request resolution |

**Tally:** 17 claims — 13 [CERT], 2 [CERT-doc], 1 [CERT-doc]+[INFER], 1 [CERT]/[INFER].
No unmarked assertions.

---

## Connections

- **B834** — Path B description + `BLoginTemplate` contract + 4.10u3 caveat + the `loginTemplate`
  control point. This block is the concrete build realization of B834 §3.B.
- **B862** — `LoginFileServlet` pipeline (§2.5); `resourceToOrd` module:// pattern (§1.3);
  `IStateLoginTemplate` not checked in N4.14 (§2.3); Domo INSTANCE/TYPE pattern (§1.1).
- **B12** — Slotomatic mechanics, `niagara-module.xml`, `module-include.xml`, Gradle task graph.
- **B18** — Module signing (PKCS7 / `com.tridium.niagara-signing`).
- **B25** — module.xml generation at build time.
- **B9** — `BILoginHTMLForm` / SCRAM auth engine; the `/login/core/auth.min.js` endpoint the
  recipe references.
- **Build-n4-module skill** — covers the full build/sign/dist pipeline; this recipe uses its
  rt-only module variant. See skill checklist before a first build.

---

## Open gaps (after this block)

- **B834-G3** — Live-station probe (operator-authorized): read `WebService.loginTemplate` value
  and `shared/domo/` contents. Remains open; requires station access.
- **B862-G1** — `LoginState` integer constants: no named constants found in corpus; minor.
