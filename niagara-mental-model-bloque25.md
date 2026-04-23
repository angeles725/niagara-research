# Bloque 25 — Migration Framework + Bajadoc generation + Gradle build + Help system

Fecha: 2026-04-23
Fuentes empíricas: decompilados `migration-rt`, `migrator-wb`, `propMigration-wb`, driver-specific migrators; `docDeveloper-doc.jar`, `help-wb.jar`, `search-rt/wb/ux.jar`, `etc/gradle/*.gradle`; archivos niagara-help/ extraídos (~950 MB, 26k archivos).

Cubre **3 pilares operacionales** del ecosistema de build/doc/migration:
1. Migration framework (AX→N4 + N4.x→N4.x+1 + drivers específicos)
2. Pipeline Gradle + doclet Bajadoc + output docDeveloper-doc.jar
3. Help system runtime (Workbench sidebar + search + indexación local)

---

## 25.1 Migration Framework — arquitectura base

### Interfaces y registries

```
javax.baja.migration (migration-rt.jar)
 ├ BIFileMigrator                interfaz principal para migrar archivos
 │   ├ migrate() Optional<String>  (error o vacío)
 │   ├ mayContainOrds() boolean
 │   └ updateOrds()
 ├ BFileMigrator                  default impl (copia simple sin transformación)
 ├ MigratorRegistry               lazy init desde Sys.getRegistry()
 │   ├ migratorsByFile             match exacto por nombre
 │   ├ migratorsByPattern          regex
 │   ├ migratorsByExt              extensión
 │   └ migratorsByDirName          directorio
 ├ BIBogElementConverter          conversor XML elements en BOG
 │   ├ convertXElem(x, typeSpec, version, root)
 │   ├ convertComplex()
 │   └ fixOrd()
 └ ConverterRegistry              lazy init, busca BIBogElementConverter + BIPxElementConverter
     ├ registra por module:type
     ├ camina jerarquía superclases (orden de aplicación)
     └ auto-generate BModuleRemovalConverter / BPxRemovalConverter para módulos removidos
```

### Matching cascada en lookup

```
migrator = lookup(filename):
  try migratorsByFile[filename]                → match exacto
  try migratorsByPattern[patterns]              → regex
  try migratorsByExt[extension]                 → por ext
  try migratorsByDirName[parentDir]             → por directorio
  fallback → BFileMigrator (copia simple)
```

### migrator.properties

Ubicación: `${niagara_user_home}/etc/migrator.properties` (fallback a defaults).

Formato:
```properties
BogMigrator.extensions=bog,abog,bbog
AlarmDbMigrator.patterns=alarm.*\.zip
AlarmDbMigrator.extensions=adb
HistoryDbMigrator.directories=history
HistoryDbMigrator.extensions=hdb
PxMigrator.extensions=px
```

Formato general: `<MigratorClassName>.<pattern|extension|file|directory>=value1,value2`

---

## 25.2 AX → N4 migration workflow

### Tool principal

**n4mig.exe** — `bin/n4mig.exe` (Windows), Assembly `Tridium.Niagara.MigrateAXtoN4`.
```
n4mig <source.dist> [--target <output_dir>] [--logLevel FINEST]
```

Entrada: `.dist` de AX-3.8 (backup station completo).
Salida: directorio station en Workbench User Home (`C:\Users\<user>\Niagara4.x\<brand>\stations\<name>`).

### Fase 1: Pre-migration assessment

1. Crear backup AX como `.dist` desde controlador AX-3.8 (típico 50-100 MB)
2. Ubicación: `%NIAGARA_USER_HOME%/backups/`
3. Contenido: BOG, history DB, alarm DB, provisioning, LDAP keytabs, licencias
4. Análisis compat:
   - Todos drivers soportados en N4 (Software Manager check)
   - Módulos custom refactorizados para N4
   - Licencias AX incompatibles → new license request
   - Programs con APIs deprecated/removed

### Fase 2: Ejecución recursiva

```
para cada archivo/carpeta en backup .dist:
  1. lookup(archivo) en MigratorRegistry
  2. si existe migrator → init con (source, target, distManifest)
  3. migrator.migrate() → Optional<String> (error o vacío)
  4. para BOG files:
     abrir .bog, caminar árbol XML elemento a elemento
     para cada elemento:
       lookup(typeSpec) en ConverterRegistry
       si converter existe → converter.convertXElem(x, typeSpec, sourceVersion)
       si no existe:
         si módulo no existe en N4 → BModuleRemovalConverter (elimina)
         si módulo existe → pass-through re-encode
       si retorna null → remover elemento
       si retorna XElem → usar nuevo
  5. updateOrds() post-processing si migrator.mayContainOrds()
```

### Migradores por tipo de archivo

| Archivo/Patrón | Migrator | Acciones |
|---|---|---|
| `*.dist` | BackupDistMigrator | Descomprime + migra recursivo |
| `*.bog` | BogMigrator | Conversión XML, removalConverters |
| `*.px` | PxMigrator | Properties conversion, module splitting |
| `alarm*.zip` | AlarmDbMigrator | Conversión history alarmas |
| `history/` (dir) | HistoryDbMigrator | Datos históricos |
| `hdb` (ext) | HistoryDbMigrator | idem |
| `ldap/` (dir) | KeytabMigrator | LDAP keytabs |
| `provisioningNiagara/` | ProvisioningNiagaraMigrator | Provisioning data |

---

## 25.3 N4.x → N4.x+1 intra-version migration

A diferencia de AX→N4, **automática y transparente** al startup.

### Detección de versión

BOG header contiene `sourceBajaVersion`. BBogMigrator extrae del manifest. Cada converter recibe `Version` como parámetro en `convertXElem(x, typeSpec, version, root)`. Lógica condicional por version fuente.

Module version tracking:
- Cada módulo declara `moduleVersion` en gradle
- Station BOG almacena qué versión de cada módulo estaba instalado
- Startup: si módulo detecta BOG version != actual → trigger auto-migration

### Trigger on station startup

No requiere n4mig tool. Framework interno invoca ConverterRegistry si módulo expone BIBogElementConverter para rango de versión.

### Propias tipo conversiones

Ejemplo en **propMigration-wb.jar**:
```
BPropNameChangeConverter      rename simple propiedad
BNewTypeConverter             cambio nombre tipo
BPropFlagsChange              cambios flags prop
BSimpleEncodingChangeConverter  encoding change
BPropValueChange              transform valor
BTypeNewName                  nueva denominación tipo
```

Mecanismos abordados:
- Properties renombradas
- Type renamed/moved
- API removido → BModuleRemovalConverter
- Schema structure complejos

---

## 25.4 Migradores específicos per-driver

| JAR | Converters/Migradores | Propósito |
|---|---|---|
| **migration-rt.jar** | BFileMigrator, ConverterRegistry, MigratorRegistry | Core framework (interfaces + registries) |
| **migrator-wb.jar** | BBogMigrator, BBackupDistMigrator, BPxMigrator, BAlarmDbMigrator, BHistoryDbMigrator, BKeytabMigrator, BProvisioningNiagaraMigrator, MigratorOrdConverter, BUserConverter, BServiceContainerConverter, BClientPasswordConverter, BFoxServiceConverter, BLdapUserServiceConverter, BAlarmServiceConverter, BHistoryServiceConverter, BProgramConverter, BProgramModuleConverter | Implementaciones concretas + n4mig tool |
| **bacnetMigrator-wb.jar** | BBacnetLinkLayerConverter, BBacnetTrendLogAlarmSourceExtConverter, BBacnetWsToAwsConverter, BBacnetWsToAwsPxConverter | BACnet: link layer, trend log alarms, Workstation→AWS |
| **modbusTcpSlaveMigrator-wb.jar** | BModbusTcpSlaveConverter | Modbus TCP Slave config |
| **snmpMigrator-wb.jar** | (interno) | SNMP v2/v3 |
| **obixMigrator-wb.jar** | (interno) | oBIX legacy |
| **videoMigrator-wb.jar** | (interno) | Video stream config |
| **ipcMigrator-wb.jar** | (interno) | IPC module |
| **honPlantControllerMigrator-wb.jar** v1.2.5 | GUI dialog con validaciones | Honeywell Plant Controller legacy |
| **honPlantControllerEHMigrator-rt.jar** | (runtime) | Event handling migration PC |
| **spyderToIrmNxMigrator-wb.jar** | IrmNxMigrator, FunctionBlockMigrator, SylkDeviceMigrator, PhysicalPointMigrator, ScheduleBlockMigrator, SoftwarePointMigrator | Spyder (legacy HVAC) → IRMNx complex |
| **propMigration-wb.jar** | BPropNameChangeConverter, BNewTypeConverter, BPropFlagsChange, BSimpleEncodingChangeConverter | Generic property migration framework |

### Clases notables migrator-wb.jar

- `BBogMigrator` — motor principal BOG XML conversion
- `BBackupDistMigrator` — descompresión recursiva .dist
- `BPxMigrator` — properties XML conversion
- `BAlarmDbMigrator` — alarmas zip archives
- `BHistoryDbMigrator` — history database
- `BHistoryFuncConverter` — history functions deprecated
- `MigratorOrdConverter` — ORD resolution post-migration
- `BUserConverter` / `BUserConfigConverter` — usuarios y roles (AX permissions → N4 roles)
- `BServiceContainerConverter`, `BRestrictedServiceConverter` — services
- `BClientPasswordConverter` — passwords encryption
- `BFoxServiceConverter` — Fox protocol
- `BLdapUserServiceConverter` — LDAP
- `BWebProfileCfgConverter` — web profiles
- `BAlarmServiceConverter`, `BHistoryServiceConverter` — alarm/history svc
- `BProvisioningConverter` — provisioning
- `BProgramConverter`, `BProgramModuleConverter` — Programs ejecutables

---

## 25.5 Directorios de instalación related

### `conversion/`

Ubicación: `/home/cristian/Honeywell/OptimizerSupervisor-N4.14.0.162/conversion/`

Contenido: `.dist` históricos para conversiones
- `AXtoN4-tridium-qnx7-n49u1-titan-am335x-clean.dist` (70.5 MB)
- `N4toAX-qnx-jace-titan-am335x-clean.dist` (downgrade)

### `cleanDist/`

Plantillas pristine de platform para diferentes hardware:
- `clean-dist-3-honeywell-nxubc.dist` (53.3 MB) — platform genérica Honeywell
- `tridium-qnx7-n4-edge10-clean.dist` — Edge10 platform
- `tridium-atlas-clean.dist` — Atlas platform
- `nre-clean-honeywell-IPC.dist` — IPC Industrial PC

Uso: inicializar platform virgen antes migración, reset completo, baseline provisioning remoto.

### `sw/`

Versiones de software:
```
sw/
 ├ 4.14.0.142/       docAXtoN4Migration-doc.jar
 ├ 4.14.0.1.2.5/     honPlantControllerMigrator v1.2.5 versioned
 └ 4.14.0.162/       versión actual
```

Permite upgrading stepwise (saltarse migraciones intermedias no permitido). Cada versión tiene distintos schema migrators.

---

## 25.6 Downgrade / Rollback N4→AX

Ruta: `niagara-help/guides-clean/AXtoN4Migration/ID-1133-00000e67.txt`

Procedimiento:
1. Stop station N4 via Application Director
2. Distribution File Installer → seleccionar conversion `.dist` (ej `N4toAX-qnx-jace-titan-am335x-clean.dist`)
3. Install → **wipes datos/keytabs/certs/SSL keys** (IP config retenida)
4. Controller rebota a AX-3.8
5. Commissioning Wizard → restaurar desde backup anterior

**CRÍTICO**: downgrade destruye datos N4. No es "rollback" moderno — es full reset + restore.

NO hay rollback automático intra-migration. Si falla, usar backup `.dist` original.

---

## 25.7 Gotchas migration

1. **Módulos removidos** — BModuleRemovalConverter elimina silenciosamente. Log `"moduleRemoved: ax:Module"`. Dependencias huérfanas (ORDs apuntan a removed) → resolver manual post-migration.
2. **API breaking changes AX→N4** (devguide-clean/ax-to-n4-module-migration.txt):
   - `BICollection` → removido, reemplazar con `BITable<BObject>`
   - `getProgram()` deprecated AX-3.5, removed N4 → usar `getModule()` + introspection
   - `BAlarmService.getOpenAlarms()` → movido a `AlarmSpaceConnection`
   - History/Alarm DB APIs → connection-oriented en N4 (vs static AX)
   - Collections generics typing changes
3. **Programs con APIs deprecated** requieren **post-migration manual fix** — migration tool NO refactoriza Java bytecode.
4. **User.permissions removido en N4** (AX: Map<String, Boolean>; N4: roles por category). BUserConverter mapea AX permisos a N4 roles. Riesgo: degradación granularidad.
5. **Licencias AX incompatibles** — diferente hash (host ID cambia si 32→64 bit). New license request obligatorio.
6. **Custom extensions / third-party** — sin N4 port disponible → estación **no puede migrar**. Solución: mantener AX-3.8 separado + integrar vía Niagara Network.
7. **Schema incompatibilities**:
   - Type renamed sin converter → elemento eliminado
   - Property refactoring → requiere propMigration-wb con converter específico
   - Nested structure changes → converters complejos rehacen árbol

### ConverterRegistry CLI debugging

```bash
java -cp migration-rt.jar javax.baja.migration.ConverterRegistry [moduleOrType]
```
Lista converters registrados o lookup para tipo específico.

---

## 25.8 Bajadoc generation pipeline

### Inputs → Outputs

Pipeline: source Java anotado → doclet → XML estructurado → HTML + índices → JAR docDeveloper-doc.

```
Source Java (@NiagaraType, @NiagaraProperty, @NiagaraAction, @NiagaraTopic, @NiagaraFacet)
  ↓ compileJava
Compiled .class
  ↓ generateBajadoc (gradle task + niagara-baja-doclet v1.0.9)
Bajadoc XML (${buildDir}/bajadoc/doc/<module-profile>/<package>/<Class>.bajadoc)
  ↓ preJarCopy + HtmlDocAction (procesado via toc.xml)
HTML output (${buildDir}/htmldoc/doc/)
  ↓ createIndex (SearchBuilder JavaExec)
Search indices (${buildDir}/index/dat/*.dat)
  ↓ jar + niagara-signing-plugin
Signed JAR (docDeveloper-doc.jar 18.7 MB)
```

### Doclet invocation

Classpath: `com.tridium:niagara-baja-doclet:1.0.9`.
```bash
javadoc \
  -doclet com.tridium.bajadoclet.Bajadoclet \
  -bajaonly "package1;package2;..." \
  -d ${buildDir}/bajadoc/doc \
  -docletVersion 1.0.9 \
  src/**/*.java
```

Doclet extensions sobre Javadoc estándar:
- Slot-aware parsing: `@NiagaraProperty` → `<property>`
- Action indexing: `@NiagaraAction` → `<action>`
- Topic support: `<topic>`
- Facets: type facets (BStatusNumeric, etc.) → metadata
- Module context: preserva module name + runtime profile
- BajaOnly packages: flag para packages con solo Baja source

### Formato .bajadoc v2.0 XML

**Class-level** (ej `alarm-rt/com/tridium/alarm/BAlarmConsoleChannel.bajadoc`):
```xml
<?xml version="1.0" encoding="UTF-8"?>
<bajadoc version="2.0" createdBy="niagara-baja-doclet-1.0.9"
         createdAt="14-Jun-2024" createdOn="ee033fd13409">
  <class module="alarm" runtimeProfile="rt"
         qualifiedName="com.tridium.alarm.BAlarmConsoleChannel"
         name="BAlarmConsoleChannel" packageName="com.tridium.alarm" public="true">
    <description>...</description>
    <tag name="@author">...</tag>
    <tag name="@since">Baja 1.0</tag>
    <extends><type class="com.tridium.fox.sys.BFoxChannel"/></extends>
    <implements>
      <parameterizedType class="java.util.function.BiConsumer">
        <args>...</args>
      </parameterizedType>
    </implements>
    <action name="routeAlarmAcks" flags="h">
      <return><type class="void"/></return>
      <description>...</description>
    </action>
  </class>
</bajadoc>
```

Metadata embedded:
- `createdBy` — doclet version (niagara-baja-doclet-1.0.9)
- `createdAt` — fecha legible (14-Jun-2024)
- `createdOn` — hostname build machine (ee033fd13409)

Elements:
- `<class>` — clase @NiagaraType
- `<extends>`, `<implements>` — jerarquía
- `<action>` — métodos @NiagaraAction
- `<property>` — fields @NiagaraProperty
- `<topic>` — @NiagaraTopic
- `<description>` — Javadoc comment parsed
- `<tag name="@xxx">` — custom tags (@author, @since, @version, @creation)

**Module-level** (`alarm-rt/module-index.bajadoc`):
```xml
<module name="alarm" runtimeProfile="rt" bajaVersion="0"
        vendor="Tridium" vendorVersion="4.14.0.162">
  <description>Niagara Alarm Module</description>
  <package name="com.tridium.alarm"/>
  <package name="javax.baja.alarm">
    <description>Core lifecycle management of alarms...</description>
  </package>
  <class packageName="javax.baja.alarm" name="BAlarmClass">
    <description>A BAlarmClass object is used to group alarms...</description>
  </class>
  <!-- 100+ class entries flattened -->
</module>
```

**Package-level** (`com/tridium/alarm/package-index.bajadoc`):
```xml
<package module="alarm" runtimeProfile="rt" name="com.tridium.alarm">
  <class packageName="com.tridium.alarm" name="BAlarmConsoleChannel">
    <description>...</description>
  </class>
</package>
```

---

## 25.9 Gradle build system

### Plugins clave (public_libraries.gradle)

```groovy
ext.libraries = [
  signing:     "com.tridium:niagara-signing-plugin:1.0.10",
  module:      "com.tridium:niagara-module-plugin:3.0.18",
  niagararjs:  "com.tridium:niagara-rjs-plugin:2.0.4",
  // BouncyCastle FIPS + 15x Eclipse JDT
]
```

### Archivos gradle principales

Path: `/home/cristian/Honeywell/OptimizerSupervisor-N4.14.0.162/etc/gradle/`
```
niagara.gradle            150+ líneas — core plugin application, sourceSet, TestNG
docmodule.gradle          304 líneas — bajadoc pipeline (BajadocSpec DSL)
public_libraries.gradle   32 líneas — versions management
eclipse.gradle, idea.gradle
```

### docmodule.gradle — BajadocSpec DSL

```groovy
buildscript {
  classpath: com.tridium:niagara-baja-doclet:1.0.8
  classpath: ${libraries.module}
}

task cleanBajadocOutputDir

class PackageSpec {
  String name
  boolean bajaOnly
}

class BajadocSpec {
  source(targetProject) { }
  includePackage(config) { }
  excludePattern(pattern) { }
  configureTask(bajadocTask) {
    bajadocTask.doclet = Class.forName("com.tridium.bajadoclet.Bajadoclet")
    bajadocTask.doFirst {
      def destDir = new File(docProject.buildDir, 'bajadoc/doc')
      destDir.mkdirs()
      options << '-d' << destDir.absolutePath
      options << '-bajaonly' << bajaOnlyPkgs
      if (docletver != null) {
        options << '-docletVersion' << docletver.substring(0, docletver.lastIndexOf('.jar'))
      }
      tree.visit { f ->
        if (!f.isDirectory() && f.path.endsWith('.java')) sources << f.file
      }
    }
  }
  createBajaOnlyPackageList() { }
}

ext.bajadoc { config }

task preJarCopy(type: Copy) { dependsOn generateBajadoc }

task createIndex(type: JavaExec) {
  main: com.tridium.help.SearchBuilder
  classpath: nre.jar, baja.jar, help-wb.jar, html-wb.jar
  args: [indexDir/src, indexDir/dat]
}

class HtmlDocAction {
  void beginPreJarCopy()
  void execute(FileCopyDetails)
  void onPreJarCopyHtmlFile(FileCopyDetails)
  HtmlDoc htmldoc = new HtmlDoc(copyright)
}

configure(tasks.jar) {
  eachFile(htmldocAction)
  from(htmldocDir/doc) { include '**/*.*' into 'doc' }
  outputs.upToDateWhen { false }
}
tasks.jar.dependsOn createIndex
```

### Task dependency graph

```
gradle clean build
 ├─→ compileJava (src/*.java)
 ├─→ cleanBajadocOutputDir
 ├─→ generateBajadoc
 │    Input: src/**/*.java + @NiagaraType
 │    Output: ${buildDir}/bajadoc/doc/{module}/{package}/{Class}.bajadoc
 ├─→ preJarCopy (type:Copy dependsOn generateBajadoc)
 │    Action: HtmlDocAction.beginPreJarCopy() + toc.xml → HTML styling
 │    Output: ${buildDir}/htmldoc/
 ├─→ createIndex (type:JavaExec dependsOn preJarCopy)
 │    Main: com.tridium.help.SearchBuilder
 │    Classpath: nre.jar, baja.jar, help-wb.jar, html-wb.jar
 │    Output: ${buildDir}/index/dat/*.dat
 └─→ jar (dependsOn createIndex)
      Include: bajadoc/** + index/dat/** + htmldoc/doc/**
      Output: {module}-doc.jar + niagara-signing-plugin firma
```

### Module project structure std

```
my-niagara-module/
 ├ build.gradle
 │   apply from: "${niagara_home}/etc/gradle/niagara.gradle"
 │   apply from: "${niagara_home}/etc/gradle/docmodule.gradle"   # si doc module
 │   niagaraModule { ... }
 ├ src/
 │   javax/baja/{module}/*.java
 │   com/tridium/{module}/*.java
 │   doc/toc.xml                        # table of contents para htmldoc
 ├ ux/
 │   src/                               # BajaScript UX
 │   rc/                                # CSS/JS resources
 ├ wb/                                  # Workbench module opcional
 ├ se/                                  # Supervisor UI opcional
 ├ test/*.java                          # TestNG tests
 └ rc/                                  # module resources (icons, images)
```

### Module.xml auto-generado

```xml
<module name="alarm-rt" bajaVersion="0" vendor="Tridium"
        vendorVersion="4.14.0.162"
        description="Niagara Alarm Module"
        preferredSymbol="a" nre="true" autoload="true"
        installable="true"
        buildMillis="1718362695611"           <!-- Unix ms timestamp -->
        buildHost="ee033fd13409"
        moduleName="alarm" runtimeProfile="rt"
        releaseDate="2024-05-28">
  <dependencies>
    <dependency name="baja" vendor="Tridium" vendorVersion="4.14.0"/>
    <dependency name="bql-rt" vendor="Tridium" vendorVersion="4.14.0"/>
    ...
  </dependencies>
  <dirs/>
  <installation/>
  <types>
    <type class="javax.baja.alarm.BAlarmDatabase" name="AlarmDatabase"/>
    <type class="javax.baja.alarm.BAlarmService" name="AlarmService"/>
    ...
  </types>
  <permissions>
    <java-permissions type="workbench">
      <java-permission class="java.lang.RuntimePermission" name="loadLibrary.alarmDialog"/>
    </java-permissions>
    <java-permissions type="station">
      <java-permission action="read,write,delete" class="java.io.FilePermission"
                       name="${protected.station.home}${/}alarm${/}-"/>
    </java-permissions>
  </permissions>
  <moduleParts>
    <modulePart name="alarm-ux" runtimeProfile="ux"/>
    <modulePart name="alarm-wb" runtimeProfile="wb"/>
    <modulePart name="alarm-se" runtimeProfile="se"/>
  </moduleParts>
</module>
```

Auto-generado desde `@NiagaraType` annotations via reflection en build time.

---

## 25.10 docDeveloper-doc.jar structure (18.7 MB)

```
docDeveloper-doc.jar
 ├ META-INF/
 │   MANIFEST.MF (989 KB, Niagara module manifest firmado)
 │   NIAGARA4.SF (989 KB, signature file)
 │   NIAGARA4.RSA (11 KB, RSA cert)
 │   module.xml (517 bytes)
 ├ doc/
 │   registry.html (9 KB)
 │   alarm.html (22.5 KB — guide)
 │   search.html (9.7 KB — search UI)
 │   constant-values.html (2.1 MB)
 │   index-all.html (17.4 MB)
 │   overview-summary.html, overview-tree.html (1 MB)
 │   alarm-rt/
 │     com/tridium/alarm/*.bajadoc (XML)
 │     javax/baja/alarm/*.bajadoc
 │     module-index.bajadoc
 │   [169 module folders]
 │   jsdoc/
 │     bajaScript-ux/ (26 MB, 50+ HTML)
 │     bajaui-ux/
 │   servlets/, themes/, architectures/
 │   stylesheet.css, script.js
 └ rc/ (resources)
```

Module.xml:
```xml
<module name="docDeveloper-doc" bajaVersion="0" vendor="Tridium"
        vendorVersion="4.14.0.162"
        description="Niagara Software Developer Documentation"
        preferredSymbol="docdev" nre="true" autoload="true"
        installable="true" buildMillis="1718363554681"
        moduleName="docDeveloper" runtimeProfile="doc"
        releaseDate="2024-05-28">
  <dependencies>
    <dependency name="baja" vendor="Tridium" vendorVersion="4.0"/>
  </dependencies>
</module>
```

---

## 25.11 Help system runtime — help-wb.jar (215 KB)

### Arquitectura clases

```
com/tridium/help/
 ├ HelpSystem (7.6 KB)             core orchestrator
 ├ SearchLoader (15 KB)             lazy loader índices
 ├ Searcher (14.7 KB)               full-text searcher
 ├ SearchBuilder (12.9 KB)          index builder (JavaExec en gradle)
 ├ SearchResult (1.8 KB)            DTO
 ├ HierarchyBuilder (5.5 KB)        inheritance tree
 ├ BajadocIndex (7.4 KB)            bajadoc cache
 ├ BajadocFinder (3.3 KB)           locate HTML por clase
 ├ HelpVerifier (3.3 KB)            integrity checks
 ├ bajadoc/
 │   ClassDoc (10.9 KB)             clase metadata
 │   MemberDoc/FieldDoc/MethodDoc   miembros
 │   PropertyDoc (1.2 KB)           @NiagaraProperty slots
 │   ActionDoc (1.4 KB)             @NiagaraAction slots
 │   SlotDoc (1 KB)                 base slot metadata
 │   ConstructorDoc (1.6 KB)
 │   Annotation, AnnotationElementDoc, AnnotationValue
 │   Parameter, Throws, DescriptionPart, Returns
 │   JavaType (5.1 KB), JavaTypeVariable, WildcardJavaType
 │   FrameworkDoc, ClassSummary
 │   parser/BajadocParser (31 KB)   HTML parser con Javadoc extraction
 │   ui/BBajadocViewer (5.7 KB)     Workbench panel viewer
 │   ui/BajadocCommands (5.9 KB)    toolbar/menu
 │   ui/BBajadocOptions (2.7 KB)    preferences
 │   ui/BFormatFE (4.1 KB)          Format editor
 │   util/TypeNameFormatter (3.2 KB)
 ├ ui/
 │   BHelpSideBar (12.3 KB)         Workbench sidebar
 │   BHelpProfile (5.0 KB)          view configuration
 │   HelpTreeController, HelpTreeNode, HelpSyncTreeCommand (7.7 KB)
 │   DocTreeModel, DocModuleNode (6.6 KB), DocClassNode (7.8 KB), DocPackageNode, DocGroupingNode, DocClassMemberNode
 │   TocRootNode (4.4 KB), TocNode, TocTreeModel
 │   SearchNode, SearchTreeModel (6.5 KB)
 ├ web/
 │   BBajadocServletView (2.2 KB)   servlet render
 ├ HtmlChecker (4.1 KB)             valida HTML
 └ rc/bajadoc.css (4.3 KB)
```

### 35 dependencias de help-wb

```
alarm-rt, baja, bajaScript-ux, bajaui-wb, bajaux-rt/ux,
box-rt, bql-rt, chart-rt, control-rt, converters-rt,
driver-rt, entityIo-rt, export-rt/wb, file-rt, fox-rt,
gx-rt/wb, history-rt, html-wb, jetty-rt, js-ux, neql-rt,
net-rt, nsh-wb, pdf-wb, platform-rt, query-rt, schedule-rt,
search-rt, tagdictionary-rt, template-rt, web-rt, workbench-wb
```

### Permissions (security sandbox)

```java
// Workbench runtime
java.io.FilePermission: ${niagara.user.home}/help/ (read,write,execute,delete)
java.lang.RuntimePermission: getenv.*, exitVM.*, accessClassInPackage.sun.util.logging.*
com.tridium.nre.security.NiagaraSocketPermission: *:1-100000 (connect,resolve)
java.awt.AWTPermission: accessClipboard, showWindowWithoutWarningBanner
com.tridium.nre.security.NiagaraBasicPermission: GET_MANAGER

// Station runtime (web server)
java.io.FilePermission: ${niagara.user.home}/help/ (read only)
com.tridium.nre.security.NiagaraSocketPermission: *:1-100000 (accept,connect,listen)
```

---

## 25.12 Search subsystem (search-rt/wb/ux.jar)

```
search-rt.jar       106 KB   runtime service, full-text indexer
search-wb.jar       34 KB    Workbench UI sidebar
search-ux.jar       N/A

javax.baja.search/
 BSearchService (27 KB)      CORE: índice full-text, thread pool
 BSearchParams (8.8 KB)      query, scope, max results
 BSearchResult (9.0 KB)      {title, snippet, rank, uri, module}
 BResultsRequest (3.1 KB)    async request/response
 BSearchScope (3.4 KB)       {GLOBAL, MODULE, CLASS, HELP}
 BSearchResultSet (11 KB)    paginated
 BSearchTask (16 KB)         async execution
 BBqlSearchProvider (6.3 KB) integración BQL
 BISearchProvider (0.7 KB)   plugin interface
```

**Características**:
- NO usa Lucene (ausente en classpath) — implementación custom
- Indexación **lazy** — HelpSystem#SearchLoader on-demand durante boot
- Scope: HELP (docs only), MODULE (clase específica), GLOBAL (todo)
- Async thread pool (no bloquea UI Workbench)
- Ranking: snippet extraction + keyword highlighting

---

## 25.13 niagara-help/ directory structure (~950 MB extracción)

```
niagara-help/
 ├ bajadoc/ (141 MB, 3,586 HTML)         API Tridium (javax.baja.*)
 │   allclasses-noframe.html (2,712 clases)
 │   index-all.html, overview-tree.html
 │   javax/baja/{package}/*.html
 ├ bajadoc-clean/ (13 MB, 2,759 .txt)    HTML → plain text
 │   CLASS/PACKAGE/EXTENDS/IMPLEMENTS/METHODS format
 ├ devguide/ (115 MB, 472 HTML)          Framework guides
 │   index.html (TOC 6 secciones)
 │   architecture.html, componentModel.html, basicDriver.html, etc.
 │   jsdoc/{module}/ (180+ carpetas)
 │   arch-*.html / arch-*.png (7 diagramas)
 ├ devguide-clean/ (5.5 MB, 463 .txt)    Plain text devguide
 ├ guides/ (491 MB, 8,971 HTML in 98 folders)  User guides
 │   Alarms/ (133), Bacnet/ (316), Histories/ (204), Scheduling/ (118)
 │   Drivers/ (145), Container/ (156), Graphics/ (198)
 │   Honeywell/ (250+), HoneywellSpyder/ (145), HoneywellSylkDevice/ (120+)
 │   AXtoN4Migration/ (105)
 ├ guides-clean/ (31 MB, 6,864 .txt)     Plain text guides
 ├ docs-text/ (44 MB, 364 .txt)          PDF → text
 ├ source/ (35 MB, 2,603 .java, 53 modules)  Código público
 ├ jdk/ (59 MB, 4,120 .bajadoc)          JDK stdlib metadata
 ├ indexes/ (17 MB, 10 JSON)
 │   class-index (404 KB, 2,712 clases)
 │   inheritance (547 KB, 2,759 entries)
 │   method-index (7.7 MB, 19,527 nombres, 39,987 entries)
 │   devguide-toc (23 KB, 109 guides en 6 sections)
 │   guides-index (86 KB, 98 folders)
 │   pdf-index (94 KB, 364 PDFs)
 │   source-index (470 KB, 2,603 clases)
 │   xref-index (1.1 MB, 3,000 clases, 17,690 edges)
 │   slots-index (623 KB, 721 clases, 3,269 props, 404 actions, 68 topics)
 │   call-index (5.4 MB, 12,462 métodos, 138,875 call entries)
 └ tools/ (852 KB, 13 Python scripts)
     niagara_help.py (44 KB, 53+ CLI commands)
     niagara_help_lib/ (11 library modules)
     bajadoc_parser.py (14 KB HTML→text)
     guides_parser.py, devguide_parser.py
     pdf_extractor.py (pdftotext wrapper)
     inheritance_builder.py, method_indexer.py
     source_indexer.py, xref_builder.py
     slots_indexer.py, build_call_index.py
```

### Estadísticas

| Métrica | Valor |
|---|---|
| Tamaño total niagara-help/ | 952 MB |
| Archivos texto | 13,053 |
| Archivos HTML | 12,029 |
| JSON indexes | 10 (17 MB) |
| PDFs fuente | 364 (1.2 GB en `/docs/`) |
| Java source lines | 679,503 |
| Clases mapeadas | 2,712 |
| Métodos indexados | 19,527 nombres / 39,987 entries |
| Módulos documentación | 198 JARs (-doc.jar) |
| Carpetas guías | 98 subsistemas |

---

## 25.14 Índices JSON detalle

### class-index.json (404 KB, 2,712 clases)

```json
{
  "BWebServlet": {
    "path": "javax/baja/web/BWebServlet.html",
    "package": "javax.baja.web",
    "type": "class"
  }
}
```
Lookup O(1) nombre → HTML bajadoc.

### inheritance.json (547 KB)

```json
{
  "BComponent": {
    "extends": "BComplex",
    "implements": ["BIPropertyContainer", "BIObject"],
    "subclasses": ["BAbstractService", "BFolder", "BDevice"],
    "package": "javax.baja.sys"
  }
}
```

### method-index.json (7.7 MB)

```json
{
  "doGet": [
    {
      "class": "BWebServlet",
      "package": "javax.baja.web",
      "section": "METHODS",
      "signature": "void doGet(WebOp c)"
    }
  ]
}
```
Búsqueda glob: `set*Name`, `do*`.

### xref-index.json (1.1 MB, 17,690 edges)

```json
{
  "BAlarmService": {
    "uses": ["BAlarmClass", "BAlarmRecord", ...],
    "used_by": ["BAlarmDatabase", "BAlarmServiceExt", ...],
    "package": "javax.baja.alarm"
  }
}
```

### slots-index.json (623 KB)

```json
{
  "BAlarmClass": {
    "module": "alarm-rt",
    "properties": [{"name":"faultCause","type":"BString","flags":["required","summary"]}],
    "actions": [{"name":"acknowledge","parameters":[]}],
    "topics": [{"name":"changed","description":"Fired when..."}]
  }
}
```

### call-index.json (5.4 MB, 138,875 call entries)

```json
{
  "BAlarmService.acknowledge": {
    "callers": [
      {"caller":"AlarmSupport.ackAlarm","file":"...","line":456}
    ]
  }
}
```

---

## 25.15 Context-sensitive help workflow

```
1. User F1 / help button en Workbench view
 → HelpSystem#getContextHelp(currentComponent)
   - tipo componente en foco
   - class-index.json → ruta HTML

2. BajadocFinder#find(className)
   - busca en allclasses-noframe.html
   - resolve path: javax/baja/alarm/BAlarmService.html

3. BBajadocViewer (Workbench View)
   - HTML parser (embedded, no external lib)
   - render con CSS (bajadoc.css)
   - extrae: inheritance, methods, fields, properties

4. BHelpSideBar (Workbench Sidebar)
   - árbol navegable: inheritance chain, methods/fields, related (xref)
   - guides relacionados (guides-index.json por keyword)

5. Searcher#find(query)
   - full-text en devguide-clean/, guides-clean/, docs-text/
   - ranking por frecuencia + relevancia
```

### URL schemes

Dentro Workbench:
```
nire://help/{module}/{className}          → BBajadocViewer
nire://guide/{guideFolder}/{topic}        → HTML viewer
nire://devguide/{file}                    → devguide-clean/{file}.txt
nire://search?query={q}&scope={HELP|MODULE}
```

Web servlet (BBajadocServletView):
```
http://station:8080/help/bajadoc/{package}/{className}.html
http://station:8080/help/guides/{guideFolder}/{topic}.html
http://station:8080/help/devguide/{file}
http://station:8080/help/search?q={query}
```

Render interno: **BWebBrowser** (wrapper Jetty, NO navegador externo). CSS embedding para estilo offline.

---

## 25.16 Doc distribution — 198 JARs

### Nombres convención

- `cl*` Honeywell commercial: clHVAC, clCBus, clPrintout, clHVACNordicAirCondition
- `doc*` Tridium base: docAlarms, docBacnet, docHistories, docScheduling, docPlatform
- `doc*` Honeywell: docHoneywellSpyder, docHoneywellSylkDevice
- Migración: docAXtoN4Migration, docAapup
- Hardware: docCIPerModel10/30/50, docTR50, docTR100

### Ubicaciones

```
/modules/ (~50 JARs principales)
  help-wb.jar, search-rt/wb/ux.jar
  {module}-doc.jar (per module base)
  clCBus-doc.jar (8.4 MB), clHVAC-doc.jar (4.3 MB)
  clPrintout-doc.jar (811 KB)

/sw/4.14.0.162/ (~150 JARs adicionales)
  docAlarms/Bacnet/Histories/Scheduling/Platform/Modbus/MQTT-doc.jar
  docObix/StationSecurity/Lexicon/Videoframework-doc.jar
  docAnalyticsAPI/BaaS/HoneywellSpyder/SylkDevice-doc.jar
  docJavaWebClients/N4Install/LdapN4-doc.jar
```

---

## 25.17 Categorización documentación

### API Reference (Bajadoc) — /bajadoc/ 141 MB

- Scope: javax.baja.* + com.tridium.*
- Audience: Java/BajaScript module devs
- Contenido: class → fields, methods, properties, inheritance, constructors
- Indexación: class-index, method-index, inheritance, xref-index, slots-index

### User Guides — /guides/ 491 MB

- Scope: operators/integrators (install, config, troubleshooting)
- Audience: system engineers, technicians
- Contenido: step-by-step, screenshots, dialogs, checklists
- Ejemplos: "Adding BACnet Network", "Configuring Alarm Responses"
- Indexación: guides-index.json, guides-clean/ full-text

### Installation & Hardware — /docs/ 1.2 GB (364 PDFs)

- Scope: install, commissioning, hardware, release notes, security
- Audience: site engineers, integrators, operations
- Contenido: install guides N4.14, hardware datasheets (CIPer, TR50/100), release bulletins (security, fixes, compat), regulatory (CE, FCC)
- Indexación: pdf-index.json, docs-text/ full-text

### Developer Guide — /devguide/ 115 MB

- Scope: framework architecture + coding patterns
- Audience: devs building Niagara modules
- Contenido: component model, module structure, driver dev, web services, BQL, licensing
- Indexación: devguide-toc.json (109 guías en 6 secciones: Framework, UI, Drivers, Web Services, Services, Tools/Protocol), devguide-clean/

### Source Code — /source/ 35 MB (2,603 .java, 53 modules)

- Scope: public Niagara API implementation
- Audience: advanced devs, framework contributors
- Indexación: source-index, call-index, xref-index

---

## 25.18 Honeywell documentation layer

### Documentación específica Honeywell

PDFs:
```
/docs/Honeywell Optimizer Supervisor N4/
 ├ NA/
 │   31-00263 datasheet
 │   31-00551 install guide N4.14
 │   software release bulletin
 ├ EU/
 │   31-00263EU datasheet
 └ CE/FCC compliance
```

Guides:
```
/guides/Honeywell/                    250+ HTML
/guides/HoneywellFunctionBlocks/      180+ HTML (FB library)
/guides/HoneywellSpyder/              145+ HTML (HVAC)
/guides/HoneywellSylkDevice/          120+ HTML (SYLK integration)
```

Módulos:
```
clHVAC-doc.jar (4.3 MB)
clHVACAirConditioning-doc.jar (5.0 MB)
clHVACChiller-doc.jar (1.3 MB)
clHVACEnergyManagement-doc.jar (543 KB)
clHVACGeneral-doc.jar (811 KB)
clHVACHeating-doc.jar (3.3 MB)
clHVACNordicAirCondition-doc.jar (1.5 MB)
clHVACNordicGeneral-doc.jar (180 KB)
clHVACRoomControl-doc.jar (553 KB)
clCBus-doc.jar (8.4 MB, CBus protocol)
docHoneywellSpyder-doc.jar
docHoneywellSylkDevice-doc.jar
```

---

## 25.19 Gotchas cross-bloque

### Migration

1. Module removed → BModuleRemovalConverter silent removal; ORDs huérfanos manual fix.
2. API deprecated (BICollection, getProgram, BAlarmService.getOpenAlarms) requiere post-migration Java refactoring.
3. User.permissions Map AX → N4 role system mapping: degradación granularidad.
4. Licencias AX incompatibles (host ID hash diff): new request obligatorio.
5. Custom extensions sin N4 port → no migra; alternativa Niagara Network bridge.
6. Downgrade destruye datos: no es rollback moderno.

### Bajadoc

7. Doclet version (1.0.9) embedded en cada .bajadoc via `createdBy` attribute.
8. `createdAt` y `createdOn` embedded permite track en qué build nació la doc.
9. `-bajaonly` flag para packages que SOLO tienen Baja source (no Java público).
10. BuildMillis en module.xml = Unix millisecond timestamp (1718363554681 = 14-Jun-2024).

### Gradle

11. docmodule.gradle se aplica condicionalmente (solo si doc module).
12. HtmlDocAction procesa toc.xml → custom styling.
13. tasks.jar.dependsOn createIndex → outputs.upToDateWhen false (siempre rebuild).
14. SearchBuilder JavaExec requiere classpath con nre.jar, baja.jar, help-wb.jar, html-wb.jar.

### Help system

15. NO usa Lucene — full-text custom implementation.
16. 3 niveles cache: index JSON + EntityTagIndex equivalente + HTML parse cache.
17. Search loader lazy — índices cargan on-demand en boot.
18. Context-help F1 mapea componente.type → class-index → bajadoc HTML.
19. Render interno BWebBrowser (Jetty wrapper), no navegador externo.

---

## Fuentes primarias leídas

1. `modules/migration-rt.jar` — core framework interfaces
2. `modules/migrator-wb.jar` — n4mig tool, BBogMigrator, BBackupDistMigrator, BPxMigrator, etc
3. `modules/propMigration-wb.jar` — property migration converters
4. `modules/bacnetMigrator-wb.jar`, `modbusTcpSlaveMigrator-wb.jar`, `snmpMigrator-wb.jar`, `obixMigrator-wb.jar`, `videoMigrator-wb.jar`, `ipcMigrator-wb.jar`, `honPlantControllerMigrator-wb.jar`, `honPlantControllerEHMigrator-rt.jar`, `spyderToIrmNxMigrator-wb.jar` — driver-specific migrators
5. `modules/docDeveloper-doc.jar` (18.7 MB, 169 module folders + jsdoc + HTML guides)
6. `modules/help-wb.jar` (215 KB, HelpSystem + BBajadocViewer + BHelpSideBar)
7. `modules/search-rt.jar` (106 KB), `search-wb.jar` (34 KB), `search-ux.jar`
8. `etc/gradle/niagara.gradle`, `docmodule.gradle` (304 líneas), `public_libraries.gradle` — build system
9. `conversion/*.dist` (70 MB AX→N4 migration .dist files)
10. `cleanDist/*.dist` (53 MB platform pristine templates)
11. `sw/{4.14.0.142, 4.14.0.1.2.5, 4.14.0.162}/` (versioned modules)
12. `defaults/migrator.properties` — configuración migradores
13. `niagara-help/` (~950 MB extracción: bajadoc/ 141MB, bajadoc-clean/ 13MB, devguide/ 115MB, devguide-clean/ 5.5MB, guides/ 491MB, guides-clean/ 31MB, docs-text/ 44MB, source/ 35MB, jdk/ 59MB, indexes/ 17MB 10 JSONs, tools/ 852KB 13 Python scripts)
14. `docs/` (1.2 GB, 364 PDFs en 126 folders)
15. `niagara-help/devguide-clean/{ax-to-n4-module-migration,build,modules,bog}.txt`

Total: ≈3500 clases decompiladas, 3 pipelines (migration workflow, bajadoc generation, help runtime), 10 JSON indexes documentados, 198 -doc.jar catalogados.
