# Niagara N4 — Bloque 17: Filesystem forensics completo + native binaries + JRE embebido

**Parte del mental model.** Ver [INDEX.md](INDEX.md) para el mapa completo.

Bloque 10.2.3 cubrió la semántica ORD de los 4 file roots (`!config` / `!sys` / `!fox` / `!file`). Este bloque profundiza en **paths físicos reales** del distribution Honeywell `OptimizerSupervisor-N4.14.0.162`, los binarios nativos, los 3 Homes (Install / User / Daemon) y el JRE embebido. Investigación empírica READ-ONLY sobre el install real en `/home/cristian/Honeywell/OptimizerSupervisor-N4.14.0.162/` (WSL) y el User Home nativo en `/home/cristian/Niagara4.14/OptimizerSupervisor/`.

---

## 17.1 Install Home — layout completo

Ruta: `/home/cristian/Honeywell/OptimizerSupervisor-N4.14.0.162/` (WSL, equivalente Windows `C:\Honeywell\OptimizerSupervisor-N4.14.0.162\`).

READ-ONLY en producción. Propiedad del installer Honeywell. Firma digital vía `MANIFEST.MF` + `NIAGARA4.RSA` + `NIAGARA4.SF` en `bin/META-INF/`. Modificar rompe integridad.

### 17.1.1 `bin/` — executables

**Runtimes principales**:

| Binario | Función | Fase |
|---------|---------|------|
| `niagarad.exe` (23 K) | Daemon nativo C/C++. Puerto 5011 HTTPS. Spawn y supervisa JVMs de stations via IPC local | Runtime Windows Service |
| `console.exe` (96 K) | Terminal headless/CLI attacheable al daemon | Debug remoto |
| `wb.exe` (108 K) | Workbench GUI main (Java Swing) | Development |
| `wb_w.exe` (108 K) | Workbench windowed variant (no console) | Deploy target |
| `nre.exe` (23 K) | Niagara Runtime Engine launcher (legacy, pre-N4.14 compat) | Runtime legacy |
| `station.exe` (24 K) | Station launcher directo (bypass daemon, raro en prod) | Debug |
| `plat.exe` (50 K) | Platform inspection CLI (analog a Workbench Platform view) | Admin |
| `test.exe` (50 K) | Test runner TestNG (Bloque 12.3) | CI/QA |
| `hdbt.exe` (35 K) | History Database Tool — export/import/repair de history files binarios | Data ops |
| `n4mig.exe` (98 K) | Migration utility AX → N4 + upgrades cross-N4 | Install/upgrade |
| `nverify.exe` (517 K) | Crypto verify — valida signatures de módulos, certs, `.dist` archives (Bloque 3.9) | Security audit |
| `dataExportTool.exe` (75 MB) | History export masivo + conversion formatos (CSV, XML, BIT) | Data ops |
| `uninstall.exe` (392 K) | MSI uninstall wrapper | Deinstall |

**DLLs nativas Windows x64** en `bin/`:

| DLL | Tamaño | Categoría |
|-----|--------|-----------|
| `nre.dll` | 115 K | Runtime core nativo (JNI bridge para `nre.exe`) |
| `njre.dll` | 69 K | JNI wrapper adicional |
| `common.dll` | 189 K | Shared utilities inter-binarios |
| `trayIcon.dll` | 194 K | Systray Windows (Workbench running indicator) |
| `alarmDialog.dll` | 24 K | Native popup alarm dialogs |
| `cppunit.dll` | 200 K | C++ Unit test framework (debug) |
| `dsfspi.dll` | 359 K | DataStore File SPI (storage driver layer) |
| `honImport.dll` | 59 K | Honeywell legacy data importer |
| `lon.dll` | 35 K | LonTalk protocol nativo |
| `opc.dll` | 176 K | OPC DA client nativo |
| `opccomn_ps.dll` | 61 K | OPC Common Proxy/Stub |
| `opcproxy.dll` | 105 K | OPC DCOM proxy |
| `pcapBacEther.dll` | 27 K | BACnet Ethernet packet capture (diagnóstico) |
| `msvcp140.dll`, `msvcr120.dll`, `vcruntime140.dll`, `vcruntime140_1.dll` | 549 K / 932 K / 92 K / 34 K | Microsoft Visual C++ Runtime deps |

**Native POSIX**:
- `libciper.so` (123 K) — **ELF ARM 32-bit EABI5 con debug info**. Cipher ops hardware-accelerated. Target: QNX Neutrino on JACE controllers (ARM AM335x — ver `cleanDist/tridium-qnx7-n49u1-titan-am335x-clean.dist`). NO es Linux x86/x64.
- `libciper.so.sig` (71 bytes) — PKCS7 DER signature (header `30 45 02 20` = ASN.1 SEQUENCE/INTEGER). Validada en runtime QNX por signature loader de Niagara.

### 17.1.2 `bin/ext/` — JAR extensions

**13 MB total**, organizados en subdirs + JARs raíz:

| Subdir | Contenido | Tamaño |
|--------|-----------|--------|
| `bcfips/` | **Bouncy Castle FIPS** (activado cuando `licensingFIPS=true`): `bc-fips-1.0.2.5.jar` (3.8 MB), `bctls-fips-1.0.19.jar`, `bcpkix-fips-1.0.7.jar`, `bc-bcfkswrapprov-1.0.0.jar` | 6.4 MB |
| `bcstd/` | **Bouncy Castle Standard** (default no-FIPS): `bcprov-jdk18on-1.78.1.jar` (8.2 MB), `bctls-jdk18on-1.78.1.jar`, `bcpkix-jdk18on-1.78.1.jar`, `bcutil-jdk18on-1.78.1.jar` | 12 MB |
| `jxbrowser/` | **JxBrowser 7.39.0** (118 MB con binarios Chromium): JAR principal (15 MB) + `win64` binary (103 MB) + `javafx/swing/swt` bindings. Usado en Workbench para HTML views embebidos | 118 MB |
| `system/` | JARs JVM shared: ASM (bytecode), JAXB, JFFI/JNA/JNR (FFI native), **OrientDB Core 3.2.23** (5.6 MB — embedded DB usado por History service Bloque 8.2), LZ4, commons-* | 17 MB |

**JARs raíz de `bin/ext/`**:
- `jetty-all-compact3-9.4.54.v20240208.jar` (3.2 MB) — HTTP server embebido (Bloque 9.3)
- `kotlin-stdlib-1.9.10.jar` (1.8 MB) — Kotlin runtime (build scripts `.gradle.kts`)
- `okhttp-4.12.0.jar` (813 K) — HTTP client
- `niagarad.jar`, `nre.jar` — Java-side helpers para los binarios nativos homónimos (cargados via `-jar` ó classpath)
- `slf4j-api-2.0.9.jar`, `slf4j-jdk14-2.0.9.jar`, `logback.xml` — logging facade

**Todos los JARs en `bin/ext/` tienen `.sig` sidecar** (256 bytes, RSA signature). Validación obligatoria en load.

**BC NO vive en `jre/lib/ext/`**. Solo en `bin/ext/bcstd|bcfips/`. Cargados vía manifest/classpath dinámico del daemon. Importante: el provider SunJCE nativo del JDK permanece en `java.security` pero BC se añade como provider adicional via `Security.addProvider()` en boot.

### 17.1.3 `bin/policy/` — Java security policy (Bloque 3.2)

3 archivos cross-comprobados con Bloque 3:

| Archivo | Tamaño | Función |
|---------|--------|---------|
| `java.policy` | 21 K | Policy base — FilePermission, SocketPermission, ReflectPermission. Extendido por module-permissions.xml (Bloque 3.3) |
| `java.security` | 65 K | Security providers list + algorithm defaults + disabled algorithms |
| `signing.properties` | 330 bytes | Path al cert público Honeywell code signing (hardcoded) |

Este último es **central a la saga httpapi**: define qué cert valida la firma de módulos. Hardcoded → bypass vía `skipModuleValidation` license-gated (Bloque 3.10) ó `-Dniagara.classLoader.skipModuleValidation=true`.

### 17.1.4 `bin/install-data/`, `bin/x86/`, `bin/META-INF/`

**`install-data/`** (260 K) — assets del installer Windows:
- `install.properties` (14 K) i18n labels
- `version.properties` (565 bytes) — release/build/date
- `licenseAgreement.txt` (23 K) — EULA plaintext
- `Powered_by_Niagara_4.bmp`, `titleImage.bmp`, `sidebarImage.bmp` — wizard UI art

**`x86/`** (544 K) — side-by-side 32-bit natives para legacy integrations:
- `ldvProxy.exe` (29 K) — Legacy Device proxy 32-bit
- `msvcp140.dll` (425 K) + `vcruntime140.dll` (73 K) — MSVC runtime x86

**`META-INF/`** — manifest del install home como si fuera un JAR:
- `MANIFEST.MF` (16 K) — classpath + main class + version del install bundle
- `NIAGARA4.RSA` (11 K) — X.509 cert Honeywell Code Signing
- `NIAGARA4.SF` (17 K) — signature file (SHA256 hashes de todos los JARs)

Esto es **la raíz de trust del install**: modificar cualquier JAR invalida `NIAGARA4.SF` → `nverify.exe` falla → daemon rehúsa boot.

### 17.1.5 `lib/` + `modules/` + `defaults/` + `etc/`

**`lib/`** (19 MB) — tooling compartido cross-binario:
- `tools.jar` (18 MB) — Javadoc, compiler, JDK tooling shared
- `niagara-baja-doclet-1.0.8.jar` + `1.0.9.jar` — Baja source annotation processor usado por slotomatic (Bloque 12.1.4)
- `Honeywell EULA.pdf` (204 K), `readmeLicenses.txt` (184 K), `lexicon.properties` (860 bytes)

**`modules/`** (1.018 GB, 969 JARs descomprimidos):
- Naming: `{moduleName}-{profile}.jar` donde profile = `rt` | `ux` | `wb` | `doc` (Bloque 1.1)
- Distribución por vendor:
  - Honeywell-prefixed (`hon*`, `alerton*`, `centraline*`): ~150+
  - Tridium core (`niagara*`, `baja*`, `control*`, `alarm*`, `history*`, `schedule*`): ~100+
  - Drivers (`bacnet*`, `lon*`, `knx*`, `modbus*`, `mqtt*`, `cbus*`, `ccn*`, `opc*`): ~80+
  - Vendor/deployment-specific (`angeles*`, `casino*`, `sanluis*`, `sejofa*`): ~50+
- **Cache**: `modules.rar` (737 MB comprimido RAR5) — backup redundante del corpus para recovery si `modules/` corrupto.

**`defaults/`** (240 K) — templates copiados en first run a User/Daemon homes:

| Archivo | Propósito |
|---------|-----------|
| `platform.bog` (3.4 K) | Platform config BOG snapshot inicial |
| `system.properties` (31 K) | **Properties globales críticos** (ver 17.6) |
| `nre.properties` (1.8 K) | JVM flags per contexto (station, wb) |
| `bacnetObjectTypes.xml` (121 K) | BACnet object type table |
| `units.xml` (30 K), `unitConversion.xml`, `unitDifferentialConversion.xml` | Sistema de unidades |
| `lonStandardConversion.xml` (8 K), `niagaraAxBacnetObjectTypes.xml` (2.5 K) | Legacy type coercion |
| `migrator.properties`, `colorCoding.properties` | Migration AX→N4 + UI colors |
| `workbench/` | Subdir con templates nuevos (newStation, newModule, newFile) |

**`etc/`** (48 K) — config post-install:
- `brand.properties` (1.3 K) — Honeywell branding overrides
- `extensions.properties` (2.9 K) — module enablement registry
- `gradle/` — build configs base + plugins de Niagara
- `m2/repository/` — Maven/Gradle local cache (deps de Niagara plugins, NO cambia per user)

### 17.1.6 `security/` install-level + `sw/` versioned signer cache

**`security/`** (install-level, distinto de user/daemon):

| Dir/File | Contenido |
|----------|-----------|
| `certificates/` | `Honeywell.certificate`, `HoneywellCentraLine.certificate`, `Tridium.certificate` — X.509 PEM para verify de module signatures |
| `truststore.jks` (958 bytes) | Truststore CA roots — muy pequeño, solo CAs Honeywell/Tridium (NO Mozilla root set) |
| `licenses/*.license` | `Honeywell.license`, `HoneywellCentraLine.license`, `Webs.license` — licenses a nivel distribution (Bloque 2) |
| `licenses/db/Win-6E6E-10AC-D1DD-8276/`, `licenses/db/Qnx-TITAN-BB4C-D480-3C70-ACE4/` | Per-hostid license cache (hostid hash → license fingerprint) |

**`sw/`** (464 K, 108 directorios versionados) — NO es "signers registry" como inicialmente inferí. Son **snapshots de JARs firmados por versión histórica**:

```
sw/1.0/      ~ 980 JARs firmados de todos los módulos en versión 1.0
sw/1.1.0/
sw/1.4.105/
sw/4.14.0.162/
...
```

Cada subdir = signing snapshot de una versión. Uso: **rollback fallback** — si un módulo nuevo falla validación, el daemon puede fallar-over a la versión firmada anterior. Evita re-sign completo cuando solo cambia 1 módulo.

### 17.1.7 Documentación — 3 directorios distintos

| Dir | Tamaño | Naturaleza | Contenido |
|-----|--------|-----------|-----------|
| `niagara-help/` | 128 K + `.git` | **Git repo** (source + HTML generado) | bajadoc (Baja API), devguide (tutorials), guides/ (100 dirs temáticos), indexes/. 82 topics HTML del devguide investigados en bloques previos |
| `javadoc/` | 18 MB | **1 JAR** | `niagaraJavadoc.jar` — HTML Javadoc precompilado del API Baja |
| `docs/` | 128 dirs | **PDFs Honeywell vendor** | Vendor-specific: `docBACnet/`, `docAnalyticsReference/`, `docCBus/`, `docCIPerModel{10,30,50}/`, `docDriversN4/`, `docEasyFeatures/`, `docEasyTemplating/` |

**Diferencia clave**: `niagara-help` es open-source dev docs Tridium (con historia git), `javadoc` es API reference generado, `docs` es closed Honeywell vendor documentation.

### 17.1.8 Otros directorios

| Dir | Tamaño | Propósito |
|-----|--------|-----------|
| `Palettes_and_Misc/palettes/` | 616 K | 3 `.palette` files: `HoneywellSubmeter.palette` (13 K), `hvfd.palette` (13 K), `XL15C.palette` (573 K) — drag-drop BOG templates para Workbench |
| `Palettes_and_Misc/{BACnetFFT_N4_Reflash, CIPer Model 30, Optimizer Unitary, TC300, TR100, Spyder Classic/Model 5/Model 7}/` | — | Equipment-specific config packages (BOG snapshots + CAD) |
| `knx/` | 8 K (2 empty .bog) | `cache.bog` + `datadefs.bog` — KNX driver runtime caches (populados al add KNX network) |
| `spyderApps/Ver28/` | — | Honeywell Spyder 2.8 pre-built control programs |
| `printout/` | 3 MB | Legacy wiring diagram generator (`clPrintout.exe` + Word COM interop DLLs) para AX→N4 diagram export |
| `px/` | empty | Reservado `.px` files (Niagara web components) |
| `JxBrowser/7.39.0/` | 12 K | Metadata only. Binarios reales en `bin/ext/jxbrowser/` (118 MB) |
| `conversion/` | 108 MB | `AXtoN4-*.dist` (68 MB) + `N4toAX-*.dist` (41 MB) — migration distributions encrypted |
| `cleanDist/` | 119 MB | Clean firmware images: QNX Neutrino ARM Titan AM335x (68 MB), Honeywell NXUBC controller (51 MB), Atlas, edge10. Usados por `n4mig.exe` para commissioning/reflash |
| `module-navigator/` | 280 K | Herramienta CLI interna (SEJOFA-specific) para decompilar e indexar los 926 JARs. Referenciada en `NAVIGATORS_MANUAL.md` |

**`4.14.0.162/` nested** (44 K) — UN solo archivo: `4.14.0.162.9.csv` (33 K). Manifest con formato `Name,Version` de TODOS los módulos en esta distribución con versiones exactas. Usado para verificación de integridad de distribución + reproducibilidad de builds.

**`modules.rar`** (737 MB) + **`NAVIGATORS_MANUAL.md`** (32 KB) — backup comprimido del corpus + doc de tooling custom.

---

## 17.2 User Home (Workbench)

Ruta real en WSL (install nativo Linux): `/home/cristian/Niagara4.14/OptimizerSupervisor/`
Equivalente Windows: `C:\Users\equipo\Niagara4.14\OptimizerSupervisor\`

**Propietario**: usuario logeado (ej. `cristian`). Workbench corre como usuario, no como SYSTEM. Aislamiento por-usuario — otro usuario tiene su propio User Home.

**Creación**: en primer run del Workbench, templates copiados desde `install/defaults/workbench/`.

### 17.2.1 Estructura top-level (verificada empírica)

```
/home/cristian/Niagara4.14/OptimizerSupervisor/
├── EasyTemplates/            # Templates Honeywell user-level
├── IPC_EULA/                 # EULA aceptado IPC
├── Spyder_EULA/              # EULA aceptado Spyder
├── TestExport.bog            # BOG test file ad-hoc usuario
├── applicationTemplates/     # Templates de aplicación per-vendor
├── audits/                   # Audit logs del Workbench
├── backups/                  # Backups manuales de stations (snapshot bajo demanda)
├── build.gradle.kts          # Build script raíz user-level (1.4 K)
├── certManagement/           # Certs PEM para firma dev
├── etc/                      # Config user-level
│   ├── options/              # 26 .options binarios (UI prefs per-plugin)
│   ├── credentials/          # credentials.xml + users.xml cached
│   ├── nre.properties        # JVM flags user-level override
│   ├── navTree.xml           # Estado persistente nav tree
│   ├── recentOrds.xml        # MRU de ORDs
│   └── wb-*Profile.xml       # Profiles UI (Galileo, Help)
├── gradle/wrapper/           # gradle-wrapper.jar + properties (Gradle 7.6)
├── gradle.properties         # niagara_home + niagara_user_home paths
├── gradlew, gradlew.bat      # Wrapper scripts
├── help/                     # 102 subdirs — offline docs user-level
├── logging/                  # Workbench session logs
├── pxEditor.properties       # Config editor PX (fonts, grid, snap)
├── registry/                 # Type registry cache Workbench (registry.db + .chk)
├── security/                 # Keystores user
├── settings.gradle.kts       # Gradle settings
├── shared/                   # Assets compartidos entre stations del usuario
├── spyder.config             # Config Spyder integration
├── stationTemplates/         # Templates para crear nuevas stations
├── stations/                 # 15 stations del usuario (local dev)
├── sw/                       # Signed module cache user-level
├── temp/                     # Scratch dir Workbench
├── templates/                # Templates genéricos (override defaults)
└── trash/                    # Workbench trash (undo support para deletes)
```

### 17.2.2 `certManagement/` + `security/`

**`certManagement/`** — PEM certificates para firma de módulos dev:
- `angelesca.pem` — cert personal del desarrollador
- `default.pem` — cert default para nuevos módulos
- `devmodulesigning.pem` — cert específico signing dev
- `sejofa_codesigningcs.pem` — cert custom deployment SEJOFA
- `sejofa_codesigningcs.csr` — CSR pendiente de firma por CA

**`security/`** (verificado empírico):
- `keystore.jceks` (14 K) — JCEKS keystore privado del usuario. Contiene clave privada firma módulos + certs custom
- `cacerts.jceks` (3.2 K) — JCEKS truststore CAs adicionales del usuario (extiende `install/security/truststore.jks`)
- `untrusted.jceks` (32 bytes) — Certs explícitamente no confiables (revocados manualmente)
- `.km` (262 bytes) — Master key encriptada del keystore
- `.kr` (1.6 K) — Keyring con claves auxiliares
- `signing/` — Metadata de signers usados + signer cache de módulos firmados por este user
- `exemptions.tes` (16 K) — Trustability Exemption Service — módulos/certs explícitamente eximidos de validación (usado con cuidado; raíz de bypass flows del Bloque 18)

**Aislamiento**: si usuario modifica su `keystore.jceks`, solo afecta su propio Workbench. Daemon (otro `security/` en `ProgramData`) es inalcanzable.

### 17.2.3 `EasyTemplates/` (Honeywell-specific)

Framework de **template parametrizado** (distinto del `BComponentTemplate` core del Bloque 14, que ese será el que cubra este template framework) — **aplicación específica Honeywell**.

Estructura:
- `easytemplating.properties` — última librería usada, tipo objeto (Simple vs Complex)
- `SimpleObjects/Default_lib/version.xml` — versioning de librería default
- `ComplexObjects/` — templates complejos (controladores, servicios, UX)

Workflow: usuario selecciona template en Workbench → EasyTemplates UI parametriza → genera subtree instanciado → drop en station. NO usa `BComponentTemplate` directamente; es layer encima.

Investigación deep de formato y workflow va en Bloque 14.

### 17.2.4 `gradle/` + wrapper

- **Gradle 7.6** (coherente con Bloque 12.1.1): `gradle-wrapper.jar` + `gradle-wrapper.properties` con `distributionUrl=.../gradle-7.6-bin.zip`
- `gradle.properties`:
  ```
  niagara_home=C:\\Honeywell\\OptimizerSupervisor-N4.14.0.162
  niagara_user_home=C:\\Users\\equipo\\Niagara4.14\\OptimizerSupervisor
  ```
- `build.gradle.kts` raíz (1.4 K):
  ```kotlin
  plugins {
    id("com.tridium.niagara")
    id("com.tridium.vendor")
    id("com.tridium.niagara-signing")
    id("com.tridium.convention.niagara-home-repositories")
  }
  vendor { defaultVendor("Angeles4657"); defaultModuleVersion("1.0") }
  ```

`settings.gradle.kts` (4.6 K) — incluye projects (módulos dev user). Signing automático: cada módulo compilado firmado con cert `security/keystore.jceks`.

### 17.2.5 `etc/` + `help/` + `stations/`

**`etc/nre.properties`** user-level (confirmado empírico):
```
station.java.options=-Dfile.encoding=UTF-8 -Xss512K -Xmx1024M
wb.java.options=-Dfile.encoding=UTF-8 -Xss512K -Xmx1024M
softjace=false
```

**`etc/options/`** — 26 archivos `.options` binarios (preferencias UI persistidas):
- `alarm-AlarmConsoleOptions.options`
- `bajaui-TextEditorOptions.options`
- `workbench-GeneralOptions.options`
- `paletteSideBar.options`
- `mru.options` — recent files/projects

**`etc/credentials/credentials.xml`** — creds cached para conexiones a stations (encriptado con master key del user keystore).

**`help/`** (102 subdirs + `bajadoc.dat` 872 KB) — docs offline developer. `bajadoc.dat` = índice full-text search binario.

**`stations/`** — 15 stations dev del usuario. Cada una `{name}/config.bog` + subtree propio. Estas son stations **locales de desarrollo**; stations de producción viven en Daemon Home.

**`sw/`** user-level — cache de módulos firmados por/para este user. Separado del `sw/` install-level.

**`trash/`** — Workbench undo dir: deletes de componentes mueven a trash con timestamp, no borran inmediato. Vacío en GC periódico.

---

## 17.3 Daemon Home (ProgramData)

Ruta Windows: `C:\ProgramData\Niagara4.14\OptimizerSupervisor\`
**En este sistema WSL el daemon no corre** — no hay materialización. La estructura descrita es según documentación + inferencia desde `defaults/` del install + consistencia con Bloque 10.1.

**Propietario**: `NT AUTHORITY\SYSTEM` (Windows) ó root/niagara user (Linux JACE). Workbench nunca accede directo — solo via Fox/platform protocol (5011).

### 17.3.1 Estructura esperada

```
C:\ProgramData\Niagara4.14\OptimizerSupervisor\
├── security/
│   ├── master.jceks         # Master keyring — referenciado Bloque 13.2.4
│   ├── tls-server.jceks     # TLS cert para HTTPS 5011
│   └── truststore.jks       # CAs del daemon (certs de stations remotas)
├── daemon/
│   ├── daemon.properties    # Config daemon (puerto 5011, pools, timeouts)
│   ├── log/                 # Rolling logs daemon.log.1, .2, ...
│   └── platformLock         # PID lock file
├── etc/                     # Config daemon-level
├── registry/
│   ├── registry.db          # Type registry cache con hash de module set
│   └── registry.chk         # Checksum para invalidation
└── stations/{name}/
    ├── config.bog           # Config real de la station (BOG serialized)
    ├── console_backup_*.txt # Console output backups
    ├── hs_err_pid*.log      # JVM crash dumps
    ├── replay_pid*.log      # Replay logs debug
    ├── public/              # Assets publicados via Jetty (Bloque 9.3)
    ├── shared/              # Shared: lib/, config/, navs/, px/, rc/, reflow/, icons/, image/
    ├── userdata/{role}/     # Per-role: admin/, operator/
    ├── history/             # History database files (.hdb — Bloque 8.2)
    ├── alarm/               # Alarm database files (.adb — Bloque 8.1)
    └── file/                # Station `file:` scheme root
```

### 17.3.2 `security/` daemon

- **`master.jceks`**: contiene master password + clave global de encryption. Usado para desencriptar BPassword reversibles de `config.bog` (Bloque 13.2.4). Inaccesible para usuarios — solo SYSTEM/daemon. **Gotcha crítico Bloque 13.2.4**: si `master.jceks` no accesible, BPassword reversibles devuelven empty **silencioso** (debug opaco).
- **`tls-server.jceks`**: cert TLS para Workbench ↔ Daemon HTTPS 5011. Se genera en commissioning.
- **`truststore.jks`**: CAs del daemon — usado para validar certs de stations remotas (Supervisor→Subordinate FOX TLS).

### 17.3.3 `daemon/`

- **`daemon.properties`**: puerto 5011 HTTPS (Bloque 10.1.4), thread pool sizes, queue depths, timeouts, logging levels.
- **`log/daemon.log*`**: rolling logs (size-based), startup/shutdown, station spawn, health checks.
- **`platformLock`**: PID lock. Previene 2 daemons concurrentes escribiendo `registry/` + `stations/`. Deleted en shutdown normal; stale lock ≠ active process indica crash previo.

### 17.3.4 `registry/` type cache

- **`registry.db`**: cache binario del type registry con hash del module set en `install/modules/`. Acelera boot — evita re-scan de 969 JARs en cada startup.
- **`registry.chk`**: hash del module set. Invalidación automática si cambia (nuevo módulo, upgrade).

Nota: User Home tiene su **propio** `registry/` (para tipos de módulos dev no deployados al daemon). Registries pueden diferir.

### 17.3.5 `stations/{name}/`

Cada station ocupa su subtree. El `config.bog` es el **estado autoritativo** — Workbench lo edita via Fox y daemon lo persiste aquí.

Separación crítica:
- **`public/`** — servido por Jetty en HTTP puerto 80/443 (Bloque 9.3). Dashboards HTML5, imágenes, JS del usuario.
- **`shared/`** — accesible solo por station + Workbench (NO public web). Librerías, navs, PX files.
- **`userdata/{role}/`** — per-role data. Separación de filesystem entre admin/operator/guest.
- **`history/*.hdb`** — history binary DBs (Bloque 8.2). Gotcha del Bloque 10.3.3: online backup **excluye** `.hdb`/`.adb` — necesitás offline backup para integridad completa.
- **`alarm/*.adb`** — alarm DBs (Bloque 8.1).

---

## 17.4 Comparativa Install vs User vs Daemon

| Concepto | Install Home | User Home | Daemon Home |
|----------|--------------|-----------|-------------|
| Ruta WSL | `/home/cristian/Honeywell/OptimizerSupervisor-N4.14.0.162/` | `/home/cristian/Niagara4.14/OptimizerSupervisor/` | No existe en WSL |
| Ruta Windows | `C:\Honeywell\OptimizerSupervisor-N4.14.0.162\` | `%USERPROFILE%\Niagara4.14\OptimizerSupervisor\` | `C:\ProgramData\Niagara4.14\OptimizerSupervisor\` |
| Propietario | Installer (Honeywell) | Usuario logeado | NT AUTHORITY\SYSTEM |
| Permisos | r-x | rwx (user) | rwx (SYSTEM only) |
| Mutabilidad | IMMUTABLE (firma integridad) | Mutable per-user | Mutable runtime |
| Licenses | `security/licenses/*.license` | N/A (lee desde install) | N/A (lee desde install) |
| Trusted CAs | `security/truststore.jks` | `security/cacerts.jceks` | `security/truststore.jks` |
| Module signing key | N/A | `security/keystore.jceks` + `certManagement/*.pem` | N/A (no firma) |
| Master keyring | N/A | N/A | `security/master.jceks` |
| TLS server cert | N/A | N/A | `security/tls-server.jceks` |
| Gradle runtime | `etc/gradle/` + plugins | `gradle/wrapper/` + `gradle.properties` | N/A |
| Maven repo | `etc/m2/repository/` | N/A (ref a install) | N/A |
| Type registry | N/A (source types en JARs) | `registry/` Workbench types | `registry/` Daemon types |
| Stations | Templates en `defaults/workbench/newStations/` | 15 stations dev locales | `stations/{name}/config.bog` (prod) |
| Logs | N/A | `logging/` Workbench | `daemon/log/daemon.log*` + `stations/{name}/` |
| Histories | N/A | N/A | `stations/{name}/history/*.hdb` |
| Alarms | N/A | N/A | `stations/{name}/alarm/*.adb` |
| EasyTemplates | Defaults en `defaults/workbench/` | `EasyTemplates/` (custom) | N/A |
| Platform lock | N/A | N/A | `daemon/platformLock` |
| Trash/undo | N/A | `trash/` | N/A |

**¿Por qué 3 Homes?** Boundary de confianza:
1. **Install** = codebase firmado Honeywell. Read-only evita tampering.
2. **User** = workspace personal. Aislamiento entre usuarios del mismo host.
3. **Daemon** = runtime state privilegiado. SYSTEM only — master password + TLS keys + production stations no deben ser leíbles por usuarios (incluso admins).

Usuario malicioso que firme módulo custom con cert de su User Home → solo carga en **su** Workbench. Deploy al daemon requiere pasar validación server-side (firma válida + permiso RWI + auth).

---

## 17.5 JRE embebido

### 17.5.1 Identificación

**Vendor**: Azul Systems — Zulu Runtime Environment
**Versión**: **1.8.0_412 revision 20** (Java 8u412)
**Plataforma**: Windows x64
**Fuente**: `jre/jreVersion.xml`

Java 8 — **pre-modular (sin JPMS)**. Monolítico `rt.jar`. LTS soportado hasta diciembre 2030. Decisión consistente con hardware legacy (JACE ARM AM335x QNX7).

**Implicación mayor**: el sandbox JVM del Bloque 3 se implementa via `SecurityManager` + `java.policy` + PolicyFile extension, NO via JPMS module boundaries (que son JDK 9+). Esto explica el uso de 19 permission groups textuales (Bloque 3.4) y la dependencia de módulos firmados para `checkPermission()`.

### 17.5.2 Layout JRE

Java 8 classical:
- `bin/` (89 MB) — executables (`java.exe`, `javac.exe`, `keytool.exe`, `kinit.exe` Kerberos, `klist.exe`, `jfr.exe`) + DLLs (`jvm.dll`, `jsse.dll`, `sunec.dll`, `j2gss.dll` Kerberos, `jaas_nt.dll` SASL, `j2pkcs11.dll` PKCS#11)
- `lib/` (~88 MB) — `rt.jar`, `jsse.jar`, `jce.jar`, `charsets.jar`, `tools.jar`, `jfr.jar`, `jfxswt.jar`. Security: `security/` con `cacerts`, `java.security`, `java.policy`. Extensions: `lib/ext/` con Zulu/OpenJDK runtime extras.
- **NO hay `jmods/`** — JDK 9+ only.
- `legal/`, `ASSEMBLY_EXCEPTION`, `LICENSE`, `THIRD_PARTY_README` — GPLv2 + Classpath Exception + Azul terms.

### 17.5.3 Security providers (orden de precedencia)

De `jre/lib/security/java.security` líneas 68-77:

| # | Provider | Clase | Algoritmos |
|---|----------|-------|------------|
| 1 | Sun | `sun.security.provider.Sun` | MD5, SHA-1/256/384/512, RSA, DSA, SecureRandom |
| 2 | SunRsaSign | `sun.security.rsa.SunRsaSign` | SHA*withRSA |
| 3 | SunEC | `sun.security.ec.SunEC` | ECDSA, EC keygen |
| 4 | SunJSSE | `com.sun.net.ssl.internal.ssl.Provider` | TLS/SSL |
| 5 | SunJCE | `com.sun.crypto.provider.SunJCE` | AES, 3DES, PBE (deshabilitado en FIPS) |
| 6 | SunJGSS | `sun.security.jgss.SunProvider` | Kerberos GSSAPI |
| 7 | SunSASL | `com.sun.security.sasl.Provider` | SCRAM-SHA256, DIGEST-MD5, PLAIN |
| 8 | XMLDSigRI | `org.jcp.xml.dsig.internal.dom.XMLDSigRI` | XML Signature W3C |
| 9 | SunPCSC | `sun.security.smartcardio.SunPCSC` | Smartcard PC/SC |
| 10 | SunMSCAPI | `sun.security.mscapi.SunMSCAPI` | Windows CryptoAPI |

**BouncyCastle NO aparece en `java.security`**. Se carga dinámicamente via `Security.addProvider()` en boot del daemon (desde `bin/ext/bcstd/` o `bin/ext/bcfips/`). Cuando `licensingFIPS=true`:
- SunJCE deshabilitado
- `bc-fips-1.0.2.5.jar` insertado como provider #2-3
- Truststore switch a formato **BCFKS** (`jre/lib/security/cacerts.bcfks`, 191 K)

**Algoritmos deshabilitados** (`jdk.tls.disabledAlgorithms`): SSLv3, TLSv1, TLSv1.1, RC4, DES, 3DES_EDE_CBC, MD5withRSA, NULL, anon.
**`jdk.certpath.disabledAlgorithms`**: MD2, MD5, SHA1 (restricciones), RSA < 1024, DSA < 1024, EC < 224.

### 17.5.4 Truststore JRE vs Niagara

**`jre/lib/security/cacerts`** (187 KB JKS):
- ~100-150 CAs Mozilla root set (default JDK)
- Password default `changeit`
- **Fallback** — usado para outbound TLS genérico (ej. conectar a HTTPS público no-Niagara)
- Override via `-Djavax.net.ssl.trustStore=...`

**Niagara usa SUS propios trust stores** (override):
- Install: `security/truststore.jks` (958 B, solo Honeywell/Tridium CAs)
- User: `security/cacerts.jceks` (3.2 K)
- Daemon: `security/truststore.jks` (CAs para validar subordinate stations)

Diseño: Niagara prefiere PKI interno (self-signed Tridium CA ó Honeywell CA) sobre CAs públicas. cacerts del JRE es último recurso.

### 17.5.5 JVM flags default (`defaults/nre.properties`)

Confirmado empírico:
```
station.java.options=-Dfile.encoding=UTF-8 -Xss512K -Xmx1024M
wb.java.options=-Dfile.encoding=UTF-8 -Xss512K -Xmx1024M
```

Interpretación:
- `-Dfile.encoding=UTF-8` — encoding default (crítico para BajaScript, .properties, BOG XML parsing)
- `-Xss512K` — thread stack 512 KB (default JDK 8 Windows x64 es 1 MB). Niagara reduce para ahorrar memoria — impacta si hay recursión profunda en links (Bloque 6.1.6 gotcha).
- `-Xmx1024M` — heap max 1 GB. Conservador. **Explica Bloque 13.1.7** (Supervisor bottleneck ~50 subordinados): con 1 GB el Supervisor no puede mantener más stations sin bump manual de `-Xmx`.
- **Sin `-Xms`** — initial heap auto (típicamente 1/4 de Xmx).
- **Sin GC explícito** — ParallelGC default JDK 8 server (NO G1GC, NO ZGC).

**Flags de producción NO presentes por default** (deben agregarse manual):
- `-XX:+UseG1GC` (mejor para large heaps)
- `-XX:+HeapDumpOnOutOfMemoryError`
- `-XX:HeapDumpPath=<path>`
- `-XX:MaxGCPauseMillis=200`

### 17.5.6 `defaults/system.properties` — properties críticos

Confirmado empírico. Valores default:

| Property | Valor | Impacto |
|----------|-------|---------|
| `niagara.moduleVerificationMode` | `low` | **LOW = permite módulos no firmados en algunos contextos**. Gotcha enorme del Bloque 18 — explica parcialmente la saga httpapi (modo default permite self-signed en dev) |
| `program.requireSigning` | `false` | Program objects pueden ejecutar sin firma |
| `niagara.fox.circuitMaxReceiveBuffer` | `10240000` (10 MB) | Explica Bloque 13.2.5 channel exhaustion ~1000 per session — con 10 MB buffer, leak agota heap |
| `niagara.license.subscriptionLicenseAllowed` | `true` | Permite subscription licensing (Bloque 13.1.1) |
| `niagara.ipv6Enabled` | `false` | **IPv6 OFF por default**. Si habilitan IPv6 en infra, deben override aquí |
| `jdk.lang.Process.allowAmbiguousCommands` | `true` | ProcessBuilder/Runtime.exec sin restricciones — security concern lateral |
| `jdk.tls.rejectClientInitiatedRenegotiation` | `true` | Mitigación DoS TLS renegotiation |
| `sun.java2d.noddraw` | `true` | Deshabilita DirectDraw (workaround VM bugs Windows) |
| `bajaui.hasKeyboard` | `true` | Workbench UI assume teclado (no kiosk/touch) |

### 17.5.7 BouncyCastle FIPS

Activación cuando `licensingFIPS=true` (feature license):
1. SunJCE (provider #5) desactivado via `java.security` override
2. `bc-fips-1.0.2.5.jar` insertado como provider #2-3
3. BCTLS-FIPS `bctls-fips-1.0.19.jar` reemplaza SunJSSE
4. BCPKIX-FIPS `bcpkix-fips-1.0.7.jar` para X.509 cert path
5. `bc-bcfkswrapprov-1.0.0.jar` wrapper para BCFKS keystore format
6. Truststore switch → `cacerts.bcfks` (191 K BCFKS)

Diferencia BC Std vs FIPS:
- **bcstd** (`bcprov-jdk18on-1.78.1.jar`, 8.2 MB) — full algorithm set, mejor performance, no FIPS
- **bcfips** (`bc-fips-1.0.2.5.jar`, 3.8 MB) — FIPS 140-2 Level 1 validado, subset restringido: SHA-256/384/512 only, AES-CBC/GCM, ECDSA P-256/384/521, RSA PSS

N4.14 default = **bcstd**. FIPS mode requiere license + reconfig manual.

### 17.5.8 Relación con sandbox (Bloque 3) y signing (Bloque 18)

**Sandbox JVM** (Bloque 3):
- `jre/lib/security/java.policy` = policy base (reads general)
- Niagara extiende con `install/bin/policy/java.policy` (firmado PKCS7)
- 19 permission groups (Bloque 3.4) enforced por `SecurityManager.checkPermission()`
- `niagara.moduleVerificationMode=low` default permite módulos sin firma en contextos limitados

**Module signing** (Bloque 18 próximo):
- BCPKIX valida X.509 cert chain en module load
- `XMLDSigRI` provider (#8) para XML signatures en policy files
- Todos JARs en `bin/ext/` tienen `.sig` (256 B RSA signatures)
- Validator usa cert Honeywell en `install/bin/policy/signing.properties`

---

## 17.6 Descubrimientos críticos del bloque

1. **JRE = Azul Zulu Java 8u412 x64 Windows** — Pre-modular, NO JPMS. Sandbox via SecurityManager + policy files + PKCS7-signed bin/policy/. Esto es coherente con la arquitectura de 19 permission groups textuales (Bloque 3).

2. **`niagara.moduleVerificationMode=low` default** — Modo default del distribution Honeywell **permite módulos no firmados en contextos limitados**. Hallazgo enorme para la investigación de Bloque 18 — la saga httpapi puede haber fallado porque ese contexto específico **sí** requería firma (workbench deploy vs runtime load differ).

3. **`libciper.so` es ARM 32-bit EABI5 QNX** — nativo para JACE controllers (ARM AM335x), NO Linux x86/x64. Firma PKCS7 DER (71 B sidecar) validada en QNX.

4. **`sw/` install-level = snapshots por versión, NO signer registry** — 108 dirs con JARs firmados históricos. Fallback rollback en validation failure.

5. **Heap 1 GB default** en `defaults/nre.properties` explica Bloque 13.1.7 Supervisor bottleneck — sin bump manual, ~50 subordinados saturan.

6. **`niagara.fox.circuitMaxReceiveBuffer=10485760` (10 MB)** explica Bloque 13.2.5 channel exhaustion — leak de channels con buffer grande agota heap.

7. **3 Homes = 3 boundary de trust**: Install inmutable firmado, User per-user isolation, Daemon SYSTEM-only con master.jceks. Workbench NUNCA accede directo a Daemon Home — solo via 5011 HTTPS.

8. **BC Standard 1.78.1 en `bin/ext/bcstd/`, BC FIPS 1.0.2.5 en `bcfips/`** — NO en `jre/lib/ext/`. Ambos se cargan via `Security.addProvider()` dinámico.

9. **JxBrowser 7.39.0 ocupa 118 MB** de los `bin/ext/` — uso: Workbench HTML views embebidos (Chromium).

10. **OrientDB 3.2.23 embebido** (`bin/ext/system/orientdb-core-*.jar`, 5.6 MB) — usado por History service (Bloque 8.2) como storage backend alternativo.

11. **`truststore.jks` install 958 B** — solo Honeywell/Tridium CAs, NO Mozilla root set. JRE `cacerts` (187 KB Mozilla) es fallback.

12. **`jdk.lang.Process.allowAmbiguousCommands=true`** default — ProcessBuilder/Runtime.exec irrestricto. Security concern lateral no mencionado en Bloque 3.

---

## 17.7 Conexiones con otros bloques

- **Bloque 2 (Licensing)**: `install/security/licenses/db/{hostid}/` per-host cache; `licensingFIPS` feature activa BC FIPS providers.
- **Bloque 3 (Security sandbox)**: `bin/policy/` PKCS7-signed triples coherente con Bloque 3.2; `niagara.moduleVerificationMode=low` es override de los controles Bloque 3.3.
- **Bloque 8 (History/Alarm)**: `stations/{name}/history/*.hdb` + `alarm/*.adb` en Daemon Home; OrientDB como backend embedded.
- **Bloque 10.2.3 (file system semantics)**: los paths físicos `install/` → `!sys`, `daemon/stations/{name}/` → `!config` + `!file`.
- **Bloque 12 (Build)**: Gradle 7.6 wrapper en User Home; `etc/m2/repository/` install-level provee plugins `com.tridium.niagara*`.
- **Bloque 13.1 (Niagara Network)**: per-station license cache en `licenses/db/{hostid}/` — hash del hostid define fingerprint.
- **Bloque 13.2.4 (keyring)**: `master.jceks` en Daemon Home — inaccesible silencioso explica gotcha.
- **Bloque 13.2.5 (Fox channels)**: `circuitMaxReceiveBuffer=10 MB` explica exhaustion.
- **Bloque 18 (próximo, signing)**: `bin/policy/signing.properties` hardcoded Honeywell CA + `moduleVerificationMode` son los puntos de bypass a explorar.

---

## Engram topic keys

- `niagara/filesystem/install-home-layout`
- `niagara/filesystem/user-daemon-homes`
- `niagara/platform/jre-embebido-azul-zulu-jdk8`
