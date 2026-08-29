# Niagara N4 — Mental Model · Bloque 12: Build System + Module Development Lifecycle

**Sesión**: 2026-04-22
**Fuentes**: devguide (build, modules, upgradingBuild, slot-o-matic, ax-to-n4-module-migration, directoryRestructure, localization, test, deployingHelp, popupEditor), source/decompilado niagara-gradle plugin, module-include.xml files, lexicon files, palette files.

---

## 12.1 Gradle + niagara-gradle plugin

### 12.1.1 Plugin overview

**Gradle 7.6+** (N4.14). Reemplaza Ant/build.xml del AX.

Plugin principal: **`com.tridium.niagara-module`** versión 7.6.17 en N4.14.0.162. Hosteado en `$NIAGARA_HOME/etc/m2/repository`. Se declara en `settings.gradle.kts`.

Plugins auxiliares:
- `com.tridium.niagara-dist`: empaqueta `.dist`.
- `com.tridium.niagara-doc`: procesa docs.
- `com.tridium.niagara-grunt`: Grunt/JS tooling para módulos ux.
- `com.tridium.bajadoc`: API docs.
- `com.tridium.bajadoc-module`: agregación docs en módulo `-doc`.

### 12.1.2 Tasks principales

- **`jar`** / **`moduleTestJar`**: compila Java + empaqueta `.jar` → copia a `$NIAGARA_HOME/modules`. moduleTestJar = JAR de tests.
- **`slotomatic`**: procesa `@NiagaraProperty|Action|Topic` en `B*.java` → genera campos/métodos. `gradlew :moduleName-rt:slotomatic [--force] [--include pattern]`.
- **`javadocJar`** / **`bajadoc`**: documentación Javadoc/Bajadoc en JARs.
- **`sign`** / **`signMods`**: firma con cert developer. Config en `niagaraSigning { ... }`.
- **`dist`**: requiere plugin `com.tridium.niagara-dist`. Empaqueta en `.dist` (ver Bloque 10.3.2).
- **`niagaraTest`** / **`jacocoNiagaraTestReport`**: ejecuta tests con cobertura JaCoCo.

### 12.1.3 Environment variables

- `NIAGARA_HOME` / `niagara_home`: instalación raíz (ej. `/opt/Niagara-4.14.0`).
- `niagara_user_home`: user data (ej. `~/.Niagara4.14/BRAND`). Dónde se instalan modules de usuario.
- `gradle.properties` en raíz del proyecto declara `niagara_home`, `niagara_user_home`, paths JDK.

Resolución: property Gradle → variable sistema → variable entorno.

### 12.1.4 `module-include.xml` (auto-generado)

Ubicación: `modulePart/module-include.xml`. Contenido: `<types>` (desde `@NiagaraType`), `<dirs>`, `<defs>`, `<lexicons>`.

Se actualiza automáticamente por annotation processor durante compilación. **No editar a mano**.

> **§14 corregido en [B631] (MA3, 2026-08-29)**: la afirmación "annotation processor durante compilación" es IMPRECISA. En el corpus decompilado NO existe ningún `javax.annotation.processing.Processor` (grep vacío). `module-include.xml` es LEÍDO (no escrito) por la herramienta **Slotomatic** (`Compiler.java:67-99,270` lee el nodo `<types>` como INPUT y escribe solo los `.java`); las entradas `<type>` se generan por el **wizard** (`NewDriverWizard`) o se editan a mano. El plugin gradle `com.tridium.niagara-module` vive fuera del corpus (`$NIAGARA_HOME/etc/m2`), así que no se puede probar que la tarea gradle no reescriba el archivo — pero el mecanismo es la herramienta Slotomatic (modelo de fuente), NO un annotation processor JSR-269. Ver [B631] §631.1-2.

Elementos `<defs>`, `<lexicons>`, `<installation>` migran ahora a `moduleManifest { }` en `build.gradle.kts`.

### 12.1.5 `niagara-module.xml`

N4 introduce `niagara-module.xml` (en carpeta módulo RAÍZ, no parts). Declara multi-part:

```xml
<niagara-module moduleName="myModule" preferredSymbol="mmod" runtimeProfiles="rt,ux,wb"/>
```

NO existe `module-parts.xml` en N4. Estructura de parts se refleja en subcarpetas Gradle.

### 12.1.6 Dependencias entre módulos

```kotlin
// build.gradle.kts
dependencies {
  api(":baja")                                    // module-to-module
  api(project(":otherModule-rt"))
  moduleTestImplementation(":control-wb")         // tests
  nre("org.bouncycastle:bcprov-jdk18on")          // $NIAGARA_HOME/bin/ext
  uberjar("org.apache.commons:commons-collections4:4.3")  // empaquetado en JAR
}
```

**Matriz de permisos** (profile → qué puede importar):
- `rt` → solo `rt`.
- `ux` → `rt` + `ux`.
- `wb` → `rt` + `ux` + `wb`.
- `se` → todas.
- `doc` → ninguna.

### 12.1.7 Multi-profile builds (rt/ux/wb)

```
myModule/
  niagara-module.xml          # Declara módulo + parts
  myModule-rt/
    myModule-rt.gradle.kts
    src/, srcTest/
    module-include.xml, module.palette, module.lexicon
  myModule-ux/
    myModule-ux.gradle.kts    # Plugin com.tridium.niagara-grunt (JS tooling)
    src/, package.json, Gruntfile.js
  myModule-wb/
    myModule-wb.gradle.kts
```

Cada `.gradle.kts` especifica `moduleManifest { runtimeProfile.set("rt"|"ux"|"wb") }`.

`settings.gradle.kts` usa `findProjects()` para auto-descubrir.

`rt` / `ux` compilan contra Java compact; `wb` contra Java SE.

### 12.1.8 Slotomatic

Herramienta Gradle que procesa anotaciones en `B*.java`:
- **`@NiagaraType`**: marker. Annotation processor estándar escribe entry en `module-include.xml`.
- **`@NiagaraProperty` / `@NiagaraAction` / `@NiagaraTopic`**: Slotomatic genera campos privados + métodos getters/setters DENTRO del mismo `.java` entre markers:
  ```
  /*+ BEGIN BAJA AUTO GENERATED CODE +*/
  ...
  /*+ END +*/
  ```

Opciones:
- `--force`: recompila todo.
- `--include pattern`: filtra clases.

Solo procesa archivos modificados (caché local).

---

## 12.2 AX → N4 migration + Lexicons/i18n

### 12.2.1 AX vs N4 breaking changes

Niagara AX (pre-2013) → N4 (2013+). No retrocompatible. Requiere refactorización.

**Cambios principales**:
- **Module parts**: AX = único `module-include.xml`. N4 = parts separados (`-rt`, `-wb`, `-doc`) en carpetas.
- **Build**: Ant (`build.xml`) → Gradle (`build.gradle.kts`).
- **Slot definitions**: AX legacy XML `<slot>` vs N4 annotations `@NiagaraProperty`. Ambos funcionan en N4 pero IDE solo resalta annotations.
- **Collections API**: AX `BICollection` NO existe en N4. Usar `BITable<BObject>` tipado.
- **Alarm/History APIs**: batch → connection-oriented (connection AutoCloseable, Bloques 8.1/8.2).

### 12.2.2 Directory restructure

**AX**:
```
mymodule/
  src/ [código mixto todos perfiles]
  build.xml
```

**N4**:
```
mymodule/
  mymodule-rt/src/ + build.gradle
  mymodule-wb/src/ + build.gradle
  mymodule-doc/ (opcional)
  build.gradle (root multi-module)
  settings.gradle
  vendor.gradle
```

**Docs**: AX `docs/` → N4 `docDeveloper/` en `META-INF/`.

**Lexicons**:
- Default: `module.lexicon` (raíz JAR).
- Locale-específico: `file:!lexicon/{lang}/moduleName.lexicon` en station.

### 12.2.3 Deprecated APIs

- `BICollection` → `BITable<T>` tipado.
- Batch Alarm/History calls → connection-oriented (statement objects).
- Old `<slot>` XML → `@NiagaraProperty` annotations.

### 12.2.4 Migration tools

**Gradle tasks**:
- `gradlew slotomatic -Dslotomatic.migrateBeforeRecompile`: convierte AX slot XML a N4 annotations, regenera `module-include.xml`.
- `gradlew build`: detecta API incompatibilidades (IDE en rojo).
- `gradlew moduleSignCheck`: valida BOG antes de upgrades.

**Workbench**: New Module Wizard auto-genera estructura N4.

**CLI**: `n4mig` migra stations AX a N4 (convierte `config.bog`).

### 12.2.5 Lexicon file format

Properties file key=value. Ejemplo (`platform-rt.lexicon`):

```properties
# Comentarios con #
help.guide.base=module://docPlatform/doc
nav.daemonFileSpace=Remote File System
validator.error.badIpAddress=Error in "{0}": improper IPv4 address format "{1}"
daemon.session.versionError=Workbench version {0} or higher. {1} reports {2}
```

**Convenciones**:
- Keys dot-separated: `module.component.aspect=value`.
- Interpolation: `{0}`, `{1}` placeholders runtime.
- Escapes: `\"`, `\\`, `\n`.

**Ubicaciones**:
- Module default: `module.lexicon` en raíz JAR (`module://moduleName/moduleName.lexicon`).
- Station locale-specific: `file:!lexicon/{lang}/moduleName.lexicon` (ej. `file:!lexicon/es_ES/platform.lexicon`).

### 12.2.6 BLexicon runtime API

```java
// Opción 1: Lexicon (simple)
Lexicon lex = Lexicon.get("platform");
String msg = lex.get("nav.daemonFileSpace");  // "Remote File System"

// Opción 2: LexiconText (N4.8+, context-aware)
LexiconText text = new LexiconText("platform", "nav.daemonFileSpace");
String msg = text.getText(context);  // locale del context

// Opción 3: LexiconModule (N4.8+, reusable)
LexiconModule lex = LexiconModule.get("platform");
String msg = lex.getText("nav.daemonFileSpace", context);
```

**Context param**: `WebOp`, `HttpServletRequest`, `OrdTarget`. Proporciona locale del user actual. Workbench típicamente usa `null` (VM default).

**JavaScript**:
```javascript
Lex.get("platform", "nav.daemonFileSpace")
```

### 12.2.7 Fallback chain + locales

Resolución en orden:
1. Exact match: `file:!lexicon/es_ES/platform.lexicon`.
2. Generic locale: `file:!lexicon/es/platform.lexicon`.
3. Station default: `file:!lexicon/{stationLocale}/platform.lexicon`.
4. Module bundled: `module://platform/platform.lexicon`.

**Locales soportados** en Honeywell OptimizerSupervisor: `en_US` (default), `es`/`es_ES`, `fr`/`fr_FR`, `de`/`de_DE`, `ja_JP`, `zh_CN`, otros según distribución.

### 12.2.8 `%lexicon(key)%` placeholder

Sintaxis en BFormat (properties tipo BFormat):
```xml
<p n="toFaultText" t="b:Format" v="%lexicon(platform:SystemPlatformService.stationSaveFailure)%"/>
<p n="toNormalText" t="b:Format" v="%lexicon(platform:SystemPlatformService.stationSaveSuccess)%"/>
```

Runtime: `BFormat.toString(Context)` resuelve placeholder.

**En Px/BajaScript** (RequireJS `lex!` plugin):
```javascript
require(['lex!platform'], function(lex) {
  var msg = lex.get('nav.daemonFileSpace');
});
```

**En Velocity**: `$util.lexicon()` helper.

---

## 12.3 Testing + palettes + help + dev tools

### 12.3.1 Testing framework (TestNG)

Niagara usa **TestNG**. Tests co-localizados en `srcTest/` dentro del módulo.

**Tipos**:
- **Unit tests** (`BTestNg`): extends `javax.baja.test.BTestNg`, declara Type en Baja. Métodos `@Test` = test cases.
- **Station-based tests** (`BTestNgStation`): station corriendo con servicios básicos (UserService, NiagaraNetwork, etc.).
- **Assertions**: TestNG `Assert.assertEquals()`, `Assert.assertTrue()`, etc. `javax.baja.test.TestHelper` para async y acceso privado.

**Annotations**:
- `@BeforeMethod` / `@AfterMethod`: per test method.
- `@BeforeClass` / `@AfterClass`: one-time per clase.
- `@DataProvider`: parameterized tests.
- `@Test(groups={"ci"}, dependsOnGroups=..., priority=...)`.
- `expectedExceptions={ClassName.class}`.

**Async**: `TestHelper.waitFor(() -> condition)` con timeout default 5000ms.

**Custom station** (`configureTestStation`): override para añadir components, services, users/roles, load BOG/XML files, Fox client `connect("user", "password")` → `BProxyFoxSession`.

**Build tasks**:
- `moduleTestJar`: compila + empaqueta.
- `niagaraTest`: ejecuta con opciones (grupos, verbosity, output).
- `jacocoNiagaraTestReport`: coverage HTML.

**Reportes**: XML + HTML en `<niagara.user.home>/reports/testng`. Opciones: `-output:<path>`, `-benchmark` (top 50 tests lentos).

### 12.3.2 Module palettes (`.palette`)

ZIP-comprimido con componentes/devices/points/lógica pre-configurados para Workbench.

**Formato**:
- Extensión `.palette` (ZIP).
- Contiene `file.xml` (raíz): BOG XML serializado (Bloque 5.2).
- Elementos `<p>`, `<a>`, `<n>` (nombre), `<t>` (tipo), `<h>` (handle).
- Módulos con prefijo `m="alias=modName"`.

**Ejemplo real** (hvfd.palette):
```xml
<bajaObjectGraph version="1.0">
<p h="1" m="b=baja" t="b:Folder">
  <p n="HFVD$20Devices" h="2" t="b:UnrestrictedFolder">
    <p n="HVFD_BACnet" h="3" m="bac=bacnet" t="bac:BacnetDevice">
      <p n="points" h="5" t="bac:BacnetPointDeviceExt">
        <p n="Frequency$20Setpoint" h="1c" t="c:NumericPoint">
          <p n="facets" v="units=u:hertz;Hz;(s-1);;"/>
          <p n="proxyExt" h="1d" t="bac:BacnetNumericProxyExt">
            <p n="objectId" v="analogValue:0"/>
          </p>
        </p>
      </p>
    </p>
  </p>
</p>
</bajaObjectGraph>
```

**Carga Workbench**: Insert from Palette en wiresheet. Workbench descomprime, deserializa BOG, coloca componentes. Valida tipos contra modules instalados.

**Palette vs Module**:
- Module = JAR/ZIP con bytecode + resources. Registra tipos en Type system.
- Palette = archivo de configuración (datos) que usa tipos de modules existentes. Template/snapshot.

### 12.3.3 Deploying help docs

Help = módulo con runtime profile `doc`.

**Creación (3 pasos)**:

1. **Autor escribe HTML** en `src/doc/`. Convenciones:
   - Type guide: `src/doc/yourModule-CustomTypeName.html`.
   - View guide: `src/doc/yourModule-ViewTypeName.html`.
   - Índice: `src/doc/toc.xml` (table of contents).

2. **Build script** (Gradle):
   ```kotlin
   plugins { id("com.tridium.niagara-doc") }
   dependencies {
     indexJars(":nre")
     indexJars(":baja")
     indexJars(":html-wb")
     indexJars(":help-wb")
   }
   ```
   Task `docCopy` copia HTML desde `src/doc/**/*`. Inserta stylesheet, copyright, navigation (Index/Prev/Next).

3. **Aggregation**:
   - Bajadoc (API docs): plugin `com.tridium.bajadoc` para modules con profile `rt`/`ux`/`wb`.
   - Agregación: módulo `-doc` con `id("com.tridium.bajadoc-module")` + `dependencies { bajadocs(project(...)) }`.

**TOC (toc.xml)**:
```xml
<toc version="1.0">
  <tocitem text="Index" target="index.html" image="module://myModule/rc/icons/index.png"/>
  <tocitem text="User Guide" target="userGuide.html"/>
  <tocitem text="Developer Guide" target="devGuide.html">
    <tocitem text="Architecture" target="arch.html"/>
  </tocitem>
</toc>
```

**Help viewer en Workbench**: Tabs TOC / API (Bajadoc) / Search (full-text).
- Help → Guide on Target busca `src/doc/<module>-<TypeName>.html`.
- Help → On View busca `src/doc/<module>-<ViewTypeName>.html`.

**CSS clases built-in**: `<p class="note">`, `class="tip"`, `class="warning"`, `class="important"`, `class="caution"`.

**JSDoc (JavaScript)**: `grunt-niagara` + `jsdoc` task → HTML en `build/src/jsdoc`.

### 12.3.4 Popup editors + property editors

**Popup Editor** (`BIPopupEditor`):
- Clase extends `BWbEditor` (abstract) + implementa `BIPopupEditor`.
- Se instancia cuando user hace click en componente en wiresheet.
- Editor controla renderizado ENTERO del componente (no por slots).
- UI coherente, validación centralizada.

**Property Editors** (registrado como agent):
- Sistema pluggable per tipo. Module declara servicios que implementan `BIEditorProvider` o similar.
- Property Sheet renderiza cada slot con su editor asignado.
- Custom editor para tipo específico: Property Sheet lo usa automáticamente.

**Workflow**:
1. Crear `MyCustomEditor extends BWbEditor` + implementar `BIPopupEditor`.
2. Registrar en `moduleManifest.xml` o vía code como agent.
3. Wiresheet detecta tipo → busca popup editor → instancia + pinta.
4. Alternativa: Property Sheet con property editors individuales.

**Diferencia**:
- Popup editor = editor del COMPONENTE completo (vista custom).
- Property editor = editor de un SLOT individual (string, number, etc.).

---

## Síntesis del bloque

### Modelo mental

**Build system Niagara N4** es Gradle-first con plugin `com.tridium.niagara-module` que orquesta 8+ tasks clave (jar, slotomatic, sign, dist, bajadoc, niagaraTest, etc.). Multi-profile (rt/ux/wb) via `niagara-module.xml` raíz + subcarpetas per-part. Matriz de permisos estricta limita qué puede importar cada profile.

**Slotomatic** es el codegen que conecta Bloque 4 (slots) con la fase de build. Annotation processor escribe `module-include.xml`; Slotomatic escribe getter/setter + static slot fields DENTRO del `.java`.

**AX → N4 migration**: refactor real. BICollection → BITable, connection-oriented APIs, Gradle en vez de Ant, parts en carpetas separadas, annotations en vez de XML slots. Tools de migración ayudan (slotomatic --migrateBeforeRecompile, n4mig CLI).

**Lexicons**: properties file con interpolation `{N}` placeholders. Fallback chain 4-niveles (exact locale → generic → station default → module bundled). `%lexicon(key)%` placeholder en BOG/BFormat, `lex!` plugin en BajaScript, `$util.lexicon()` en Velocity.

**Testing (TestNG)**: unit + station-based con `BTestNg` y `BTestNgStation`. Async via `TestHelper.waitFor()`. Coverage JaCoCo. Reports XML+HTML.

**Palettes** (.palette): templates BOG zippeados. Datos, no código.

**Help deployment**: módulo con profile `doc`. Plugin `niagara-doc` + `bajadoc` + TOC.xml.

**Popup editors vs property editors**: componente completo vs slot individual. Extensión vía `BWbEditor` + `BIPopupEditor`.

### Conexiones

- **Bloque 1**: modules tienen profiles rt/ux/wb — el build system lo materializa.
- **Bloque 3**: `sign`/`signMods` firma con cert developer del keyring (Bloque 3.8 signing profiles).
- **Bloque 4**: Slotomatic genera código desde annotations del Bloque 4.5.
- **Bloque 5**: palettes son BOG files (5.2). Lexicon file references via ORD `file:!lexicon/...`.
- **Bloque 9**: `lex!` plugin RequireJS (9.2.4). Velocity `$util.lexicon()` (9.1.5).
- **Bloque 10**: `.dist` build (10.3.2) requiere plugin `com.tridium.niagara-dist`.

### Gotchas críticos

1. **Matriz de permisos profile** estricta — módulo `rt` NO puede importar de `wb` nunca (gotcha del Bloque 1.1 pero relevante build-time).
2. **Slotomatic solo toca archivos modificados** por default — si corrompió caché, usar `--force`.
3. **`module-include.xml` NO editable a mano** — regenerado cada build.
4. **`niagara-module.xml` vs `module-parts.xml`** — último NO existe en N4.
5. **Gradle 7.6+ requerido** para N4.13+. Upgrade rompe build scripts viejos.
6. **AX→N4 collections**: cast de `BICollection` a `BITable<T>` requiere type parameter explícito.
7. **Lexicon fallback silent**: si falta key en locale target, cae a default sin warning. Logging recomendado para missing keys.
8. **Lexicon interpolation**: `{0}` `{1}` — si se olvida un param, se imprime literal `{0}`.
9. **TestNG station tests son lentos** — cada uno spin up station completa. Preferir unit tests cuando sea posible.
10. **Palettes no validan tipos al guardar** — solo al load en Workbench. Un palette con typos `m:TypoTypeName` falla en insert.
11. **Help doc module `-doc` sin docCopy explícito** falla silenciosamente — no hay tarea default.

### Qué habilita

Con Bloques 1-12 podés:
- Escribir un módulo custom end-to-end: build.gradle.kts, annotations, slotomatic, test, sign, dist.
- Localizar un módulo a español sin tocar código.
- Migrar un módulo AX legacy a N4 con las herramientas correctas.
- Integrar CI/CD con `niagaraTest` + `jacocoNiagaraTestReport`.
- Publicar un palette con dispositivos BACnet pre-configurados.

**Próximo**: Bloque 13 — Gaps profundos (subscription licensing, Niagara Network, Fox wire, sensitive data, virtual components, NiagaraRPC).

---

## Engram topic keys

- `niagara/build/gradle-plugin` — niagara-module plugin, tasks, multi-profile, dependencias, slotomatic.
- `niagara/build/ax-n4-migration-lexicons` — breaking changes AX→N4, directory restructure, deprecated APIs, migration tools, lexicon format, BLexicon API, fallback chain.
- `niagara/build/testing-palettes-help-editors` — TestNG, .palette format, deploying help, popup/property editors.

---

**Sesión cerrada**: 2026-04-22 — Bloque 12 consolidado.
