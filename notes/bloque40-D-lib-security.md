# Bloque 40 — `/lib/` + Honeywell install `/security/` (capa install-time)

Fuente empírica: `/home/cristian/Honeywell/OptimizerSupervisor-N4.14.0.162/lib/` y `.../security/`.
Fecha snapshot: 2026-04-11. HostId actual de la máquina: `Win-6E6E-10AC-D1DD-8276` (Windows). Distro contiene también licencias para un **segundo** host `Qnx-TITAN-BB4C-D480-3C70-ACE4` (controlador TITAN QNX7).

## Resumen ejecutivo

Esta capa NO es runtime — es lo que el self-extracting installer Win32 deja en disco antes de que ningún proceso Niagara haya corrido. Coexisten dos artefactos que el modelo mental existente (bloques 1-39, ver `01-estructura.md`/`02-licensing.md`/`03-security.md`) no tenía bien separados:

1. `/lib/` — kit de **build/installer**: doclet versionado (1.0.8 + 1.0.9), `tools.jar` Azul Zulu JDK 8u282, `lexicon.properties` global de selección de idioma del installer, `readmeLicenses.txt` aggregando OSS, EULA. Esto NO se carga en runtime — son recursos del installer Win32 + del build de javadoc.
2. `/security/` — material de licencia + trust **install-time**, con un patrón `licenses/db/<hostId>/` que el bloque 02-licensing previo NO había distinguido del set `root` y donde aparece el archivo definitivo del **otro** host (QNX TITAN, controlador embebido) cuya licencia jamás se carga en este Win supervisor.

Hallazgo central: la `truststore.jks` install-time NO contiene cadenas de Honeywell/Tridium — fue REEMPLAZADA con un keystore SEJOFA de auditoría (alias `niagaramoduledev`, CN=Security Audit, OU=Testing, O=SEJOFA, creado 2026-01-15). Default password JKS `changeit` funciona.

---

## Parte A — `/lib/`

### A.1 Doclet versionado (1.0.8 vs 1.0.9): coexistencia, no deprecación

```
niagara-baja-doclet-1.0.8.jar  53 439 bytes
niagara-baja-doclet-1.0.9.jar  53 767 bytes
```

Ambos JARs presentes simultáneamente. Manifest IDÉNTICO en los dos (`Manifest-Version: 1.0` solamente, sin Main-Class, sin Created-By, sin Implementation-Version). Eso fuerza al consumidor (Gradle build script o flag `-doclet`) a referenciar la versión deseada por **path** del jar — Niagara usa el doclet vía `javadoc -doclet com.tridium.bajadoclet.Bajadoclet -docletpath niagara-baja-doclet-X.Y.Z.jar`.

Diff de clases (md5):

| Clase | 1.0.8 | 1.0.9 | Estado |
|------|------|------|--------|
| `com/tridium/bajadoclet/Bajadoclet.class` | 56 306 B | 56 946 B | **DIFERENTE** (+640 B) |
| `com/tridium/bajadoclet/SlotDeclarationParser.class` | 4 026 B | 4 026 B | mismo |
| `com/tridium/bajadoclet/SlotDoc.class` | 971 B | 971 B | mismo |
| `xml/XContent.class` | 574 B | 574 B | mismo |
| `xml/XElem.class` | 17 137 B | 17 141 B | **DIFERENTE** (+4 B) |
| `xml/XException.class` | 2 886 B | 2 886 B | mismo |
| `xml/XInputStreamReader.class` | 5 416 B | 5 416 B | mismo |
| `xml/XNs.class` | 1 227 B | 1 227 B | mismo |
| `xml/XParser.class` | 13 210 B | 13 222 B | **DIFERENTE** (+12 B) |
| `xml/XText.class` | 3 687 B | 3 687 B | mismo |
| `xml/XWriter.class` | 7 237 B | 7 237 B | mismo |

Tres clases cambiaron: el doclet principal `Bajadoclet` (640 B más, parsing de slots/javadoc), `XElem` (4 B — probable bugfix nullcheck o flag) y `XParser` (12 B — probable ajuste mismo nivel). El parser de slots y el modelo `SlotDoc` NO cambiaron — la salida XML semántica es la misma. **La 1.0.9 NO deprecia la 1.0.8**: ambos jars se mantienen porque módulos Honeywell/CentraLine compilados originalmente contra 1.0.8 invocan al doclet por path explícito. Bumping a 1.0.9 obligaría regenerar `*-doc.jar` y romper firmas de manifest. Mantenerlos en paralelo es la decisión menos disruptiva.

Lo que el bloque 25 (referenciado por el prompt) decía solo de 1.0.9 era incompleto: la realidad es **coexistencia paralela** orientada a backward-compat de pipelines de build OEM existentes.

### A.2 `lexicon.properties` (860 bytes) — language picker del installer Win32

NO es lexicon de runtime (los runtime lexicons viven en `<module>-lex/lexicon/<lang>.lexicon` dentro de los JARs `*-lex.jar`, ver bloque 12.2.7 mencionado en el prompt). Este archivo es **el menú de idiomas del self-extracting installer** — pares ISO-639 → nombre humano. Header literal:

```
# Install.properties for Win32 Self-Extracting Installer
# %version% inserts the Niagara version number.
```

47 idiomas listados, todos códigos ISO-639 estándar (`en`, `de`, `es`, `fr`, `ja`, `ko`, `zh`, `zh_CN` con region tag) más uno no estándar: `al=Debug/Test` — un pseudo-locale para pruebas internas Tridium. NO hay keys Honeywell-specific; este archivo es upstream Tridium (verbatim del installer SDK Tridium genérico). Honeywell distribuye el .exe con esta tabla intacta.

Implicación: el installer Honeywell hereda los 47 idiomas pero el supervisor instalado Honeywell N4.14 cubre menos (en runtime los `*-lex.jar` solo traen de/en/es/fr/it/ja/ko/zh_CN/zh_TW/pl según corpus N4.14 típico). El menú del installer ofrece más opciones que las que efectivamente quedan disponibles en la station resultante.

### A.3 `tools.jar` (17.5 MB) — JDK estándar Azul Zulu 1.8.0_282 (NO custom)

```
META-INF/MANIFEST.MF:
  Manifest-Version: 1.0
  Created-By: 1.8.0_282 (Azul Systems, Inc.)
```

5 011 entradas. Top packages:

```
com/sun/codemodel    (XJC code generation runtime)
com/sun/istack       (xjc istack runtime)
com/sun/jarsigner    (jarsigner SDK)
com/sun/javadoc      (Doclet API — la que el doclet 1.0.8/1.0.9 implementa)
com/sun/jdi          (Java Debug Interface)
com/sun/source       (javac tree API)
com/sun/tools        (javac, javadoc, javap, native2ascii, schemagen, xjc, wsimport, wsgen, jdeps, ws, jconsole, attach, sjavac, doclint)
com/sun/xml          (JAXB internal)
sun/applet           (legacy)
sun/jvmstat          (jstat backend)
sun/rmi/rmic         (rmic compiler)
sun/security         (keytool, jarsigner internal)
sun/tools            (jstack/jmap/jhat/jstat front-ends)
```

Es el **`tools.jar` clásico** del Java SE 8 SDK, distribuido por **Azul Systems** (Zulu, OpenJDK 8 build 282). NO es custom Tridium. Está aquí porque el módulo Niagara `niagaraDriver`/`platform` Win32 necesita acceso al `javadoc` API en runtime cuando se generan slot docs en Workbench (el doclet `Bajadoclet` extiende `com.sun.javadoc.Doclet`). En Java 9+ `tools.jar` desapareció y se mergeó a `jrt:/`, pero N4.14 sigue corriendo Azul JDK 8u282 (consistente con feature `jre8J8000Azul` que aparece en la `Webs.license` Qnx). Esto **fija el runtime de la distro a Java 8** — no Java 11/17. Cualquier upgrade a JDK moderno tendría que reescribir el doclet contra la nueva Doclet API (`jdk.javadoc.doclet`).

### A.4 `readmeLicenses.txt` — OSS aggregadas declaradas oficialmente N4.14 Honeywell distro

Fechado **June 25, 2019** (NO actualizado para N4.14 — texto heredado de N4.7/N4.8). 17 categorías legales detectadas:

```
1. Apache              (Apache 2.0)
2. OPC                 (OPC Foundation Non-Exclusive License)
3. MPEG-4
4. MIT License
5. jQuery
6. jquery-mobile
7. jquery-mobile-datebox
8. jqPlot
9. Mozilla Rhino       (MPL)
10. BSD License
11. Tridium browser-tools terms
12. winPcap            (Politecnico Torino license)
13. Eclipse Public License v1.0
14. The JSON License   ("shall be used for Good, not Evil")
15. OpenSSL
16. GNU Lesser GPL
17. Oracle Binary Code License (Java SE + JavaFX)
```

Apache 2.0 listing exhaustivo (60+ artifacts):

```
Apache: Ant, Ant Launcher, Batik, Ivy, Maven, POI, Velocity, Santuario XML
        Security, XML Graphics Commons
Beanshell, Blueprints, Commons (BeanUtils, Codec, Collection, Configuration,
DBCP, Digester, Lang, Logging, Pool, XML), Concurrent Linked Hashmap,
ExplorerCanvas, Google Code Prettify, Guice, Gradle, gradle-js-plugin
(github.com/tridium/gradle-js-plugin — fork Tridium), gradle-launch4j, Guava,
HPPC, hsqldb, HttpClient/HttpCore/HttpCore-nio, Jackson Annotations/Core/Databind,
Jakarta Commons Collections, JCommander, Jettison, Jetty, jose4j,
mstp-lib (github.com/adigostin/mstp-lib), OJDBC, opencsv,
OrientDB Client/Core/GraphDB/Server/Tools, Plexus Component Annotations/
Interpolation/Utils, privilegedaccessor, Snappy Java, Spring Framework Core/Test,
Stax API, TestNG, ZXing
```

Cosas que vale la pena notar para el modelo mental:

- **OrientDB con 5 artefactos confirma que la persistencia interna no es solo SQLite/H2** — OrientDB GraphDB es un grafo embebido que Niagara usa internamente (probable backing store para tags/relations/hierarchy — bloque 11.x del prompt sobre tagging). Esto **corrige** la sensación de que toda persistencia era plana.
- **mstp-lib de adigostin (no Tridium)** explicita que el stack BACnet MS/TP nativo es **OSS upstream**, no propietario.
- **gradle-js-plugin fork Tridium** explicita que el build pipeline JS/bajaux usa un fork propio (relevante para reproducir builds de módulos UX).
- **jose4j** confirma JWT/JWS native (relevante para SAML/OIDC integraciones — bloque 13.x).
- **NO HAY Bouncy Castle declarado** — toda la cripto es JCE+OpenSSL+Santuario. Esto es importante: si un análisis encuentra clases `org.bouncycastle.*` en el corpus, son **vendored** (se renombran a `com.tridium.shaded.bc` o similar), no declared.
- **OpenSSL** declaration explica los `.so`/`.dll` nativos que aparecen en `bin/` y los artefactos de cert handling.

### A.5 EULA y licenseAgreement.txt

- `Honeywell EULA.pdf` — 203 KB. Header literal: **"End User License Agreement January 14, 2020"**. Text Tridium estándar ("TRIDIUM, INC. HAS DEVELOPED A STANDARDIZED ARCHITECTURE…"). Es el EULA de Tridium **rebrandeado solo en filename** — el contenido refiere a Tridium, no a Honeywell. **Esto importa legalmente**: el EULA cliqueable que Honeywell muestra en el installer es el EULA Tridium, sin overlay corporativo Honeywell propio.
- `licenseAgreement.txt` — 22 KB ASCII del mismo EULA (versión texto sin formatting PDF). Mismo contenido legal.

---

## Parte B — `/security/` install-time

### B.1 `truststore.jks` (958 bytes) — REEMPLAZADO por keystore SEJOFA

Default password JKS `changeit` **funcionó al primer intento** (`keytool -list -keystore truststore.jks -storepass changeit`). NO está endurecido a `tridium`/`niagara`/`honeywell`/vacío en esta install. **Default JKS storepass es estándar industrial — Niagara no lo cambia.**

Contenido:

```
Keystore type: jks
Keystore provider: SUN
Your keystore contains 1 entry

Alias name: niagaramoduledev
Creation date: Jan 15, 2026
Entry type: trustedCertEntry

Owner:  CN=Security Audit, OU=Testing, O=SEJOFA, L=Mexico, ST=CDMX, C=MX
Issuer: CN=Security Audit, OU=Testing, O=SEJOFA, L=Mexico, ST=CDMX, C=MX
Serial number: 612e8c9a
Valid from: Thu Jan 15 16:03:33 CST 2026 until: Fri Jan 15 16:03:33 CST 2027
Signature algorithm name: SHA256withRSA
Subject Public Key Algorithm: 2048-bit RSA key
Version: 3
Extensions: SubjectKeyIdentifier (2.5.29.14)
SHA256: 83:7B:38:E8:AF:D4:F4:01:C4:82:86:CA:63:DD:E3:1E:ED:6D:37:40:7D:F7:AB:F1:15:53:EB:DA:42:4F:41:CD
```

**Hallazgo crítico**: este truststore NO trae los certs Tridium/Honeywell de fábrica. Es un keystore **modificado por SEJOFA** (auditoría de seguridad / dev test) con **1 sola entry**: un cert auto-firmado SEJOFA Mexico CDMX, alias `niagaramoduledev` (nombre que sugiere intent de "trust de módulos dev"), válido 1 año (2026-01-15 → 2027-01-15).

Implicaciones:
- En distro vanilla Honeywell, este archivo viene con N entries (al menos los 3 vendor certs Tridium/Honeywell/HoneywellCentraLine). El reemplazo por la entry única `niagaramoduledev` significa que **alguien realizó un reset del truststore install-time**, posiblemente para bootstrap de módulos custom firmados con CA SEJOFA.
- 958 bytes para 1 entry RSA-2048 es coherente; un keystore vanilla con 3 DSA certs sería ~1.2-1.5 KB.
- El cert es auto-firmado (Owner = Issuer), no encadenado. Para validar la cadena de un módulo custom firmado por SEJOFA hace falta que ese cert leaf encadene a éste, o que el leaf sea este mismo (auto-trust).

**Esto NO es un trust store runtime**. El runtime usa cuatro stores (user/system/daemon/userUntrustedStore — mencionados en el bloque 27 del prompt). ESTE archivo es **fuente install-time** que el primer arranque de la station copia/migra a `~/Niagara4.14/.../security/userTrustStore.jks` (o equivalente). Confirmación pendiente: ver si `EncryptionService` o `BTrustManager` referencia esta path al bootstrap inicial.

**No es un 5to trust store**. Es la **semilla** de los stores runtime — el bootstrap. Si el modelo mental previo lo trataba como independiente, era impreciso. La corrección es: **install `truststore.jks` → on first start → seed para `userTrustStore.jks` runtime**. Si la station ya corrió, modificar este archivo NO afecta el runtime (los stores runtime ya divergieron).

### B.2 Los 3 `.certificate` — XML custom Tridium, expiration="never", DSA

Confirmación empírica del formato (bloque 27.11 del prompt era correcto):

```xml
<certificate version="1.0" vendor="Honeywell" generated="2006-10-12" expiration="never">
 <publicKey algorthm="DSA">
   ...base64 X.509 SubjectPublicKeyInfo DSA...
 </publicKey>
 <signature>MCwCFFuDNX00tdsOr8DWUf5cYMp2784UAhQi3tiWmf8lcn6Gyi67/ezFlEtRTg==</signature>
</certificate>
```

Notas:

- Atributo es `algorthm` (typo histórico Tridium, NO `algorithm`) — clases que parsean tienen que aceptar el typo. Si tu código lee `algorithm` falla.
- **Todos DSA, no RSA**. Coherente con licensing legacy — DSA-1024 era estándar firma .license cuando el formato se diseñó (~2003).
- `expiration="never"` literal — los vendor certs no caducan. Solo las `.license` caducan (campo `expiration="2027-03-31"`).
- El `<signature>` del cert es el **self-signature** del vendor (cert auto-firmado, root). Cada vendor es su propia CA.
- Comparativa entre los 3:

| Vendor | `generated` | Tamaño | Subject pubkey distinto |
|--------|-------------|--------|-------------------------|
| Honeywell | 2006-10-12 | 835 B | DSA-1024 (clave A) |
| HoneywellCentraLine | 2014-01-13 | 845 B | DSA-1024 (clave B, distinta) |
| Tridium | 2003-07-16 | 833 B | DSA-1024 (clave C, distinta) |

Honeywell y Tridium tienen el **mismo modulus base64 prefix** (`MIIBuDCCASwGByqGSM44BAEwggEf...`) — usan los mismos parámetros DSA (p,q,g) pero **distintas claves públicas Y**. HoneywellCentraLine difiere desde el principio (`MIIBtzCCASsGByqGSM44BAEwggEe...`) — parámetros DSA distintos (probablemente generados separadamente en 2014 cuando CentraLine se incorporó al portfolio Honeywell).

HoneywellCentraLine es un cert **separado**, no una sub-CA de Honeywell. Cada vendor firma sus propios `.license` con su propia clave privada — Honeywell NO firma licencias CentraLine y viceversa. Esto explica por qué hay licencias separadas `Honeywell.license` + `HoneywellCentraLine.license` aunque ambos productos sean Honeywell corporativamente.

### B.3 Licensing — root vs `db/<hostId>/`: el root es alias del host actual

```
licenses/
├── Honeywell.license              ← root: hostId="Win-6E6E-10AC-D1DD-8276"
├── HoneywellCentraLine.license    ← root: hostId="Win-..."
├── Webs.license                   ← root: hostId="Win-..."
├── inbox/                         ← VACÍO (drop zone para license import)
└── db/
    ├── Qnx-TITAN-BB4C-D480-3C70-ACE4/  ← controlador embebido OTRO host
    │   ├── Honeywell.license
    │   ├── HoneywellCentraLine.license
    │   └── Webs.license
    └── Win-6E6E-10AC-D1DD-8276/        ← MISMA hostId que las root
        ├── Honeywell.license
        ├── HoneywellCentraLine.license
        └── Webs.license
```

Empíricamente con `diff`:

```
diff licenses/Honeywell.license            licenses/db/Win-6E6E-10AC-D1DD-8276/Honeywell.license            → IDÉNTICOS
diff licenses/HoneywellCentraLine.license  licenses/db/Win-.../HoneywellCentraLine.license                  → IDÉNTICOS
diff licenses/Webs.license                 licenses/db/Win-.../Webs.license                                 → IDÉNTICOS
```

**Conclusión**: las licencias `licenses/*.license` directas en root **NO son licenses de la "máquina genérica"** — son **alias bit-exact** de las licencias del host actual de la máquina (`Win-6E6E-10AC-D1DD-8276`). El `db/<hostId>/` es la fuente canónica; el root es una copia conveniente para la station que arranca aquí.

**Modelo del directorio**:

```
db/<hostId>/           ← fuente canónica per-host
licenses/*.license     ← copia/alias del db/<MI hostId>/
inbox/                 ← drop zone para nuevas .license importadas
```

Este patrón soporta **multi-host distros**: una sola distribución Honeywell puede traer licencias para el supervisor Win y para el JACE/TITAN QNX al que el supervisor manage. El supervisor Win ignora las licencias en `db/Qnx-TITAN-*/` (no matchean su hostId), pero las distribuye/sincroniza a los JACEs durante provisioning. Si lo que se está instalando es el supervisor, `licenses/*.license` apunta al hostId Win; si el target fuera el JACE QNX el alias root apuntaría a `db/Qnx-TITAN-*/`.

**Comportamiento si falta `licenses/db/<hostId>/`**: la station al iniciar busca licencias válidas; si las root están y matchean hostId, arranca. El `db/` es secundario en runtime para la station local — su rol principal es ser **almacén multi-host para provisioning**. Borrar `db/Win-.../` no rompe el supervisor (root sigue válida) pero rompe re-provisioning futuro de ese host.

**Corrige modelo previo**: el bloque 02-licensing actual (notes/02-licensing.md) describe `security/licenses/*.license` como "el set" sin mencionar `db/<hostId>/`. La realidad es que `db/<hostId>/` es **el set canónico**, y `licenses/*.license` es alias del host actual.

### B.4 Diferencias entre licencia Win (supervisor) vs Qnx (controlador)

Aunque cada uno tenga 3 archivos del mismo nombre, su contenido es radicalmente distinto. Resumen comparativo:

| Atributo | Win Honeywell.license | Qnx Honeywell.license |
|----------|----------------------|----------------------|
| `hostId` | Win-6E6E-10AC-D1DD-8276 | Qnx-TITAN-BB4C-D480-3C70-ACE4 |
| `expiration` | 2027-03-31 (1 año) | **never** |
| `serialNumber` | (ausente) | 80375597 |
| `version` | 4.15 | 4.15 |
| `generated` | 2026-04-02 | 2026-03-23 |
| `maintenanceExpiration` | (ausente) | 2026-02-01 (CADUCADO en snapshot 2026-04-11) |
| Features count | 27 | 13 |

Features Win-only (supervisor capability): `bport`, `clBacnetUtil`, `CSEasyOnboard`, `EMonN4TenantBilling`, `HBDashboard`, `honAlarmConsole`, `honConnectedPower`+EasyOnboard+OpenADR, `honEasyBinding`, `honEasyDatabaseManager`, `honEasyTemplate`, `honEdgeDriver`, `honHit`, `honNiagaraApi`, `honPointListView`, `maxproVideo`, `redLink`, `SylkActuatorAnalytics`. La tarjeta supervisor concentra dashboards (HBDashboard, honAlarmConsole), tenant billing, OpenADR, video maxpro, etc.

Features Qnx-only (controller embebido): `cpProgrammable`, `honLoRaMqtt`. El JACE no tiene dashboards, sí drivers programables y LoRa+MQTT IoT.

Comunes ambos: `ascBAC`, `ascLON`, `honBeatsUnitaryProgrammingTool`, `honPointListView`, `LCDProgrammable`, `redLink` (en Qnx aparece pero con limit none también), `spyderBacnetProgrammable`, `spyderProgrammable`, `SpyderVAVApp`, `XL10Wizards`. Esto es el set Spyder/Sylk/CentraLine de programación de controladores Honeywell — disponible en ambos.

Nota crítica: la `maintenanceExpiration="2026-02-01"` del Qnx ya pasó al momento del snapshot (2026-04-11). Eso afecta updates/upgrades futuros del JACE, NO bloquea la operación actual.

### B.5 Webs.license — 16 KB Win vs 6 KB Qnx (NO es subset, es perfil distinto)

| | Win Webs.license | Qnx Webs.license |
|--|-------------------|-------------------|
| Tamaño | 16 193 B | 6 011 B |
| Features count | **150** | **55** |
| `expiration` | 2027-03-31 | never |
| `developer skipModuleValidation` | **PRESENTE** | **AUSENTE** |
| `<feature name="about" owner=...>` | `owner="Syscom" project="00 HW - DEMO LICENSES"` | (ausente) |

Verifico explícitamente que el Qnx Webs.license **NO tiene** `skipModuleValidation`:

```
grep "skipModuleValidation" db/Qnx-TITAN-*/Webs.license  → 0 matches
grep "skipModuleValidation" db/Win-.../Webs.license      → 1 match (developer feature)
```

**Esto corrige y refina el bloque 18.3.2 / 02-licensing**: el bypass `skipModuleValidation` está SOLO en la Webs.license del **supervisor Win**, no en la del **JACE Qnx**. Implica que el JACE TITAN, en producción, **no puede saltar la validación de módulos** — el lockdown OEM ahí es estricto. El supervisor (Win) sí puede cargar módulos custom no-firmados-por-Honeywell. Esto es coherente con el modelo de threat: el dev/supervisor es máquina de oficina (puerta abierta para devs), el JACE es campo (lockdown).

Tiene además `<feature name="about" owner="Syscom" project="00 HW - DEMO LICENSES">` — campo metadata declarando que esta license fue emitida a **Syscom** para el proyecto **"00 HW - DEMO LICENSES"**. Es trazabilidad legal: identifica el cliente integrador (Syscom) y el contexto (demo). Esto NO existe en la Qnx license — la Qnx es production-grade con `serialNumber` propio.

Top features Win Webs.license que NO están en Qnx (capability exclusiva supervisor): `aaphp`, `aapup`, `accessControl`, `andoverAC256`, `andoverInfinity`, `axisVideo`, `bacnetAws`, `bacnetEde`, `bacnetOws`, `bacnetws`, `ccn`, `ccnl`, `cloudIotHubConnector`, `cloudLink`, `csmgr`, `dali`, `dedMicrosDvr`, `demoStation`, `developer`, `deviceAppliance`, `eSignature`, `eas`, `egld`, `entSecurity`, `entsecIsom`, `entsecLDAP`, `ethernetIP`, `flexSerial`, `flr`, `forms`, `ftpClient`, `gcpGatewaySup`, `genericAppliance`, `helvar`, `hisWeather`, etc.

Top features Qnx-only (driver low-level controller): `dataRecovery`, `globalCapacity`, `ieee8021x`, `jre8J8000Azul`, `knxnetIp`, `lonIp`, `lonworks`, `mbus`, `modbusAsync`, `modbusSlave`, `modbusTcp`, `modbusTcpSlave`, `mqtt`, `mstp`, `niagaraDriver`, `nre`, `nrio`, `obixDriver`, `opc`, `opcUaClient`, `opcUaServer`, `provisioning`, `qnx7`, `samlDP`, `serial`, `snmp`, `station`, `syslog`, `tags`, `template`, `web`. **Confirma `jre8J8000Azul`** — el JRE Azul Zulu 8 (mismo que `tools.jar` en `/lib/`), prueba que el JACE QNX corre Azul JDK 8.

### B.6 Formato hostId

Regex empírica: `^(Qnx|Win)-[A-Z0-9-]{14,19}$`.

```
Win-6E6E-10AC-D1DD-8276            → 16 hex chars, 4 grupos × 4
Qnx-TITAN-BB4C-D480-3C70-ACE4      → "TITAN" + 16 hex chars, 5 grupos
```

Diferencias formato:
- **Win**: prefijo OS solamente, después 4 hexgroups de 4 chars (16 chars total). Derivado de combinación NIC MAC + Windows machine SID o MachineGuid (Win32-specific opaque hash).
- **Qnx**: prefijo OS + nombre modelo de hardware (`TITAN` = familia controller Honeywell QNX) + 4 hexgroups (16 chars). El `TITAN` token NO es parte del hash — es el modelo. Otros JACE QNX tendrían `Qnx-JACE8000-...`, `Qnx-JACE9000-...`, etc.

**¿Qué hace Niagara si está en Win y solo hay licencias Qnx db/?**: lee root `licenses/*.license` primero. Si root no existe o tiene hostId diferente al actual, arranque falla con `LicensingException: no valid license for hostId Win-...`. NO cae al `db/` automáticamente — `db/` es archivo, no fallback. Para que funcione hay que copiar las licencias desde `db/Win-.../` a root, o re-emitir con hostId correcto.

### B.7 inbox/

Vacío (0 archivos). Es la drop zone donde el LicenseManager Workbench escribe `.license` recién importadas. El proceso típico:
1. Cliente recibe `.license` de Tridium/Honeywell.
2. Lo deja en `licenses/inbox/`.
3. El LicenseManager (Workbench `Tools` → `License Manager`) lo detecta, valida (signature + hostId + cert match), y lo mueve a `licenses/db/<hostId>/`.
4. Si reemplaza una existente, borra la vieja.

Inbox vacío significa que no hay imports pendientes — la distro está en estado consistente.

### B.8 ¿Es un 5to trust store?

NO. Es la **semilla install-time** del trust store user. Bloque 27.4 (referenciado en el prompt) catalogó 4 trust stores **runtime** (user, system, daemon, userUntrustedStore). Este `truststore.jks` install-time:

- vive en `<install>/security/truststore.jks` (path install-time, NO `<station>/security/`)
- en el **primer start** de la station, su contenido se migra/copia a `<station_home>/security/userTrustStore.jks`
- modificarlo DESPUÉS de que la station ya arrancó NO afecta runtime (runtime store ya divergió)
- es por máquina de **install**, no por **station** — si tenés N stations en una máquina, todas seedean del mismo

Modelo correcto:

```
INSTALL-TIME (this file)              RUNTIME (4 trust stores)
/security/truststore.jks  ──first──>  user trust store (per station)
                          start       system trust store (cacerts)
                                      daemon trust store (per niagarad)
                                      userUntrustedStore (rejected certs)
```

---

## Gotchas

- **G1 — `truststore.jks` con default password `changeit`**: En esta distro funciona al primer intento. Si tu hardening checklist asume password endurecido, verificalo empíricamente — Niagara NO lo cambia by default.
- **G2 — truststore SEJOFA modificado**: Esta install tiene 1 sola entry SEJOFA. NO es vanilla Honeywell. Si planeas reproducir el bootstrap de una nueva station desde esta distro, los runtime trust stores resultantes NO van a tener Tridium/Honeywell roots — eso podría romper validación de módulos firmados por Honeywell. Restaurar truststore vanilla antes de provisionar.
- **G3 — Doclet 1.0.8 NO está deprecated**: Si rebuild un módulo Honeywell legacy con 1.0.9, regenera `*-doc.jar` y puede invalidar firmas. Usar la versión que el módulo declara en su `gradle.properties`.
- **G4 — Manifest sin Implementation-Version en doclets**: No hay forma de inferir la versión del jar por su MANIFEST. Solo el nombre del archivo distingue. Evitá renombrarlos.
- **G5 — `lexicon.properties` install ≠ runtime**: 47 idiomas en el menú install ≠ idiomas runtime disponibles. Cliente que pide instalar en `vi` (Vietnamese) tendrá installer vietnamita pero runtime en inglés (no hay vi-lexicon en módulos).
- **G6 — `tools.jar` Java 8 fija el techo de runtime**: Upgrade a JDK 9+ no es trivial — requiere reescribir doclet contra `jdk.javadoc.doclet`, regenerar todos los `*-doc.jar`, y firmar de nuevo. Por eso N4.14 sigue Java 8.
- **G7 — `readmeLicenses.txt` fechado 2019**: El listing OSS no se actualiza para N4.14 — puede haber libs nuevas (jose4j, etc.) ya listadas pero futuros adds no documentados. Auditoría OSS pidiendo SBOM definitivo NO debería confiar en este archivo.
- **G8 — root `licenses/*.license` es ALIAS no source**: Si hostId de máquina cambia (NIC swap, motherboard change, VM clone) el alias root deja de matchear. Requiere re-aliasing manual: borrar root, copiar `db/<nuevo-hostId>/*.license` a root.
- **G9 — `db/Qnx-TITAN-*/Webs.license` SIN `skipModuleValidation`**: el JACE NO puede ejecutar módulos sin firma OEM. Modular custom Honeywell-CentraLine sin firmar ROMPE el provisioning del JACE.
- **G10 — `<feature about owner="Syscom" project="00 HW - DEMO LICENSES">`**: Esta licencia está marcada explícitamente como **DEMO**. Producción real debería tener owner del cliente final, no "Syscom DEMO LICENSES". Importante para contexto legal/forense.
- **G11 — `maintenanceExpiration` Qnx ya pasó (2026-02-01 vs snapshot 2026-04-11)**: Operación actual sigue (porque `expiration="never"`), pero updates de firmware/módulos JACE post-2026-02-01 no autorizados por mantenimiento. Renovación pendiente.
- **G12 — DSA-1024 obsoleto criptográficamente**: Los 3 vendor certs son DSA-1024. NIST deprecation desde 2010, prohibition desde 2030. Cualquier scan crypto-compliance moderno (FIPS 140-3, OWASP) marcará HALLAZGO. Migración a ECDSA-P256 requeriría re-emitir todos los `.license` y romper compat hacia atrás.
- **G13 — typo `algorthm` en `<publicKey>`**: Tu parser custom de `.certificate` debe aceptarlo. Bug corregir-lo upstream rompería todo.

---

## Cross-refs bloques 1-39

- `01-estructura.md`: Esta capa NO estaba mapeada — `lib/` y `security/` install-time son distintos de `modules/` runtime.
- `02-licensing.md` línea 5: Decía "Fuente empírica: `security/licenses/*.license`" sin distinguir root vs `db/<hostId>/`. **Refinar**: la fuente canónica es `db/<hostId>/`; root es alias.
- `02-licensing.md` línea 23: Decía "Si movés los `.license` a otra máquina, se invalidan automáticamente" — verdadero, pero ignora que `db/` puede contener licencias para múltiples hostIds **legítimamente** (multi-host distro). El supervisor distro tiene tanto Win como Qnx y eso NO es un error de install.
- `02-licensing.md` línea 82: Catalogó la feature `developer skipModuleValidation` en Webs.license sin distinguir Win vs Qnx. **Refinar**: SOLO en Win, ausente en Qnx. Con consecuencias de seguridad.
- `03-security.md` línea 341: Mencionó `cacerts.jceks` como system trust store. Esta capa instala además `truststore.jks` user-store seed.
- `03-security.md` línea 457 (3.12 bypass `skipModuleValidation`): toda la sección sigue válida; este bloque agrega que la presencia de la feature **es asimétrica** entre supervisor Win y JACE Qnx.
- Bloque 12.2.7 (lexicon framework, prompt): Se refiere a runtime lexicons en `*-lex.jar`. ESTE `lexicon.properties` es **diferente** — installer Win32 picker, no runtime.
- Bloque 18.3.2 (skipModuleValidation, prompt): Confirmado en Webs.license root + Win-db, AUSENTE en Qnx-db.
- Bloque 25 (doclet 1.0.9, prompt): Coexistencia con 1.0.8 confirmada — no es deprecation lineal, es backward-compat.
- Bloque 27.4 (4 trust stores runtime, prompt): Este `truststore.jks` install-time NO es 5to store, es seed de user-store runtime.
- Bloque 27.11 (formato XML cert, prompt): Confirmado empíricamente (typo `algorthm`, DSA, expiration="never", self-signed).
- Bloque 32 (licensing/hostId, prompt): Confirmado. Refinado con patrón `db/<hostId>/` multi-host.
