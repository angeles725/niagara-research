# 02 — Detección de licencias falsificadas o manipuladas (forense defensivo)

> **Propósito**: guía para **detectar** licencias falsificadas/tampereadas en sistemas comprometidos
> o sospechosos. Documenta los verificadores que **existen** en la plataforma y las señales (IOC)
> que dejan la manipulación o la falsificación.
> **No** incluye cómo producir firmas válidas, cómo evadir los verificadores ni cómo alterar el check.

Relacionado: `niagara-n4-incident-response-playbook.md` (respuesta a incidentes completa) y el
hallazgo B75 (incidente real con módulo no firmado) en `niagara-mental-model-bloque75-security-incident.md`.

---

## 1. Por qué la falsificación es detectable por diseño

El diseño de validación hace que un archivo de licencia manipulado **no pase** sin re-firmarlo, y
re-firmar requiere la clave privada del vendor que no está en ningún lado del sistema:

1. **Validación all-or-nothing por archivo**: si cualquiera de los 5 checks falla (hostId,
   generated, expiration, certificate del vendor, firma DSA), **todo el archivo se descarta** — no
   hay carga parcial de features (`notes/02-licensing.md:60-68`). Un atacante no puede "editar un
   campo" y dejar el resto intacto.
2. **La firma cubre la totalidad**: la `<signature>` DSA (SHA-1, DER `SEQ{INTEGER r(20B),
   INTEGER s(20B)}`) se verifica contra la clave pública del `{vendor}.certificate`
   (`niagara-mental-model-bloque126.md:154`). Editar cualquier atributo del XML invalida la firma.
3. **Cadena de confianza de 2 niveles**: Tridium (root DSA, OID `1.2.840.10040.4.1`, 1024 bits,
   `generated="2003-07-16" expiration="never"`) → certificate del vendor → firma del `.license`
   (`notes/02-licensing.md:125`; `niagara-mental-model-bloque126.md:154`).
4. **Doble clave pública en `LicenseUtil`** (`masterPublicKey` + `version2PublicKey`): la verificación
   elige clave por versión del archivo (`niagara-mental-model-bloque41.md:373-388`) — una firma
   generada contra una raíz vieja no sirve para archivos nuevos y viceversa.
5. **El verificador nativo no es el límite de seguridad**: `LicenseUtil::isFeaturePresent` en
   `nre.dll` es un text-match `<license vendor="%s"` / `<feature name="%s"` **sin verificar firma**
   (`niagara-mental-model-bloque126.md:160-178`) — solo gatea el launcher (Java agents). La
   autoridad real es la capa Java `LicenseManager`. **Implicación forense**: un atacante que solo
   confía en el text-match nativo puede "ver" features que la capa Java rechaza; el desacuerdo entre
   capas es una señal.

> **Nota de madurez criptográfica**: el esquema de licencias es DSA-1024/SHA-1 (raíz de 2003,
> `expiration="never"`), el eslabón más débil de la plataforma frente a módulos (RSA-2048) y binarios
> (Authenticode RSA-4096/DigiCert G4) (`niagara-mental-model-bloque126.md:178-183`). Detectable no
> equivale a inforzable: la detección asume que nadie posee la clave privada del vendor. La
> plataforma **no valida OCSP/CRL** (certs revocados siguen aceptándose) — relevante para forense de
> certificados (`niagara-mental-model-bloque27.md:894-898`).

---

## 2. Cadena de confianza y verificadores disponibles

| Verificador | Qué valida | Notas forenses |
|---|---|---|
| `LicenseUtil.verify(bytes, sig, Version)` (baja.jar) | Firma DSA del `.license` contra la clave pública del version | Selecciona `masterPublicKey`/`version2PublicKey` (`niagara-mental-model-bloque41.md:373-388`) |
| `DsfSha1WithDsaSignature` (`dsfspi.dll`, Mocana DSF como JCE provider) | Motor DSA de licencias/certs | `parseDSAPublicKey`/`parseDSASignature`/`parseDERInteger` (`niagara-mental-model-bloque126.md:53,157`) |
| `DsfUtil::checkFileSignature` (`dsfspi.dll`) | Módulos `.jar.sig` (RSA-2048, detached 256 B) | Defensas de bounds (path ≤255, key ≤500 B, sig <501 B) (`niagara-mental-model-bloque126.md:170-174`) |
| `nverify.exe` CLI | Archivos firmados contra cadena DigiCert G4 | Estado `ERR_CERT_*` (revoked/expired…); `--unsigned *`/`--removed *` son wildcards que **neutralizan** el check — si un wrapper los pasa, la verificación se vacía (`niagara-mental-model-bloque126.md:105-127`) |
| Módulos en runtime | `ModuleSignatureStatusEnum`: `OK, NOT_TIMESTAMPED, UNKNOWN, SIGNER_SELF_SIGNED, TIMESTAMP_SELF_SIGNED, CERT_PATH_VALIDATION_FAILURE, CERT_PATH_VALIDATION_WARNING, UNSIGNED, INVALID_SIGNATURE` | B112 (`niagara-mental-model-bloque112.md:16-24`) |
| Truststores | `NIAGARA4.SF` del install; `cacerts.jceks`/`cacerts.bks` | Editar el truststore invalida `NIAGARA4.SF` → boot rechazado (`niagara-mental-model-bloque18.md:211-215,628`); `cacerts.jceks` corrupto → `Keystore was tampered with` (`niagara-mental-model-bloque27.md:883`) |

---

## 3. Señales de tampering (IOC)

### 3.1 Sobre archivos de licencia

| Señal | Qué sugiere | Dónde ver |
|---|---|---|
| `hostId` del archivo ≠ host actual, pero el archivo está en `db/<hostId>/` del host | Archivo de otra máquina; si apareció solo, posible exfiltración/re-uso | `notes/02-licensing.md:23`; `notes/bloque40-D-lib-security.md:319` |
| `generated` > fecha actual | Reloj manipulado o archivo re-generado con reloj atrasado | `notes/02-licensing.md:60-67` |
| La firma no verifica contra el `{vendor}.certificate` | Archivo editado o re-firmado con clave que no es la del vendor | §1; `niagara-mental-model-bloque126.md:154` |
| Feature con atributos que la plataforma **no conoce** o en vendor inexistente | Archivo "artesanal" (los atributos son string-based, typo-prone — un falsificador tiene que acertar la cadena exacta) | `niagara-mental-model-bloque41.md:363-365` |
| License con `skipModuleValidation="true"` o `smDeveloperMode="true"` activos en producción | Flags de desarrollo que **no se pueden quitar de la licencia**; solo blacklistearlos | `niagara-mental-model-bloque75.md:80,106`; `Webs.license:40,129` |
| `license.unreleasedSoftware=true` presente en producción | Reclasifica self-signed como aceptable (`SIGNER_SELF_SIGNED`) — lever de atenuación de firma | `niagara-mental-model-bloque113.md:207-214` |

### 3.2 Sobre módulos y binarios (contexto de compromiso)

| Señal | Qué sugiere | Dónde ver |
|---|---|---|
| Módulo `UNSIGNED`/`INVALID_SIGNATURE` en un sistema con `niagara.moduleVerificationMode=low` (default) | El default de fábrica **carga** módulos sin firmar con `NETWORK_COMMUNICATION` (`fail-open`) — el incidente B75 es plausible sin exploit | `niagara-mental-model-bloque75.md:18-19,60-68` |
| Warning del classloader: `No code signers for entry %s in module %s` (logger `"loader"`) | Módulo sin firma cargándose; one-shot WARNING luego FINEST | `niagara-mental-model-bloque112.md:121-127` (`ModuleClassLoader.java:73,600`) |
| `CERT_PATH_VALIDATION_FAILURE` en el SecurityDashboard | Firma presente pero cadena no valida | `niagara-mental-model-bloque112.md:16-24` |
| `program.notSigned` / `program.notTimestamped` (logger de `BCode.java`) | Programas (Fox/scripts) sin firmar | `niagara-mental-model-bloque112.md:121-127` (`BCode.java:214,208`) |
| Entradas en `security/exemptions.tes` | Puerta de exención de firma a nivel usuario; **cada alta loguea audit** | `niagara-mental-model-bloque18.md:600-622` |
| Truststore de install alterado (p. ej. `truststore.jks` con `SEJOFA` reemplazando el vanilla) | Cambio de ancla de confianza; restaurar antes de provisionar | `niagara-mental-model-bloque40.md` §40.4.6; `niagara-mental-model-bloque113.md:203-216` |

### 3.3 Sobre el runtime

| Señal | Qué sugiere | Dónde ver |
|---|---|---|
| Divergencia `licenseManager.pointCount` vs count del nav point manager | Inconsistencia de conteo (bug o manipulación) | `niagara-mental-model-bloque28.md:1387` |
| Feature visible para el text-match nativo pero rechazada por la capa Java | Desacuerdo entre capas (texto sin firma válida) | `niagara-mental-model-bloque126.md:169-177` |
| Alarmas de expiración que "aparecen y desaparecen" con el reloj | Manipulación de reloj (anti-reloj: `generated` check; nCloud usa su propio reloj) | `niagara-mental-model-bloque32.md:339-345`; `notes/02-licensing.md:60-67` |

---

## 4. Superficies de detección en la plataforma

- **SecurityDashboard** (station): `BSecurityDashboardModulePermissions` clasifica módulos
  `trustedModules` = `OK + SIGNER_SELF_SIGNED + TIMESTAMP_SELF_SIGNED − CERT_PATH_VALIDATION_FAILURE`;
  `BSecurityDashboardModuleSignatures` muestra buckets por `ModuleSignatureStatusEnum` y solo
  `high` obtiene OK. `getStationDashboardData` gated por `hasAdminRead`
  (`niagara-mental-model-bloque112.md:31-66,398-401`). La firma **sí discrimina** — para *señalar*.
- **PolicySpy** (`spy:/securityInfo/Policy Information`): renderiza la política efectiva por módulo:
  `type`, `purpose`, **`params`** (p. ej. `hosts=* ports=443` literal), `riskLevel`; requiere
  `NiagaraBasicPermission("VIEW_NIAGARA_POLICY")` (`niagara-mental-model-bloque112.md:131-150`).
- **Log del daemon** (OS-level, escrito por el nativo `niagarad`): no existe API Baja que lo borre →
  evidencia durable; leíble por `/systemlog`/`/getdaemonoutput`; `~logging` (`SystemFilePaths.java:32`)
  (`niagara-mental-model-bloque112.md:83-110`).
- **Audit**: el incidente B75 borró audit (y el log de history) — un audit **vacío o truncado** con
  actividad reciente es en sí una señal (`niagara-mental-model-bloque75.md:31-38`).
- **Logging del install**: pobre por diseño (solo bytes/duration en `DaemonFileUtil.java:535`,
  sin logging de auth en `BDaemonSession.acquireCredentials()`) — no esperar evidencia granular del
  cliente (`niagara-mental-model-bloque112.md:103-110`).

---

## 5. Matriz de erasabilidad (qué evidencia sobrevive)

Del análisis B112 (`niagara-mental-model-bloque112.md:154-181`):

| Señal | Erasable por un atacante de la station? |
|---|---|
| Módulo presente (estado) | **No** (state-based) |
| Modo de verificación (`niagara.moduleVerificationMode`) | **No** |
| Política efectiva (`PolicySpy`) | **No** |
| Log del daemon (`console.log`, OS-level) | **No** (nativo `niagarad`) |
| Eventos JUL (log de módulos) | Parcialmente |
| Audit | Parcialmente (B75 lo borró) |
| Alarmas/histories | Parcialmente (`HistoryDatabaseConnection`) |

Regla práctica: recoger primero lo **state-based y OS-level** (daemon log, SecurityDashboard,
PolicySpy, copias de `.license`/`.certificate`/truststores) antes de tocar nada en la station.

---

## 6. Procedimiento forense paso a paso

### Fase A — Recolección (sin alterar la evidencia)

1. Copiar (no editar) los árboles de licencias y trust:
   - `/security/licenses/` y `/security/licenses/db/<hostId>/` (todos los `.license` y `.certificate`)
     (`niagara-mental-model.md:121`).
   - Truststores: `userTrustStore`/`systemTrustStore`/`daemonTrustStore` + `userUntrustedStore`
     (4 stores runtime; `niagara-mental-model-bloque27.md` §27.4).
2. Extraer el log del daemon (`/systemlog`/`/getdaemonoutput`) y el `console.log`
   (`niagara-mental-model-bloque112.md:83-110`).
3. Capturar el SecurityDashboard (`getStationDashboardData`, `hasAdminRead`) y PolicySpy
   (`niagara-mental-model-bloque112.md:131-150,398-401`).
4. Copiar backup de station y audit (`~audits`) (`SystemFilePaths.java:32`).
5. Anotar `Sys.getHostId()` y los relojes (station + OS) para el check anti-reloj.

### Fase B — Verificación offline (fuera de la station)

1. **Integridad de cada `.license`**: verificar la `<signature>` DSA contra la clave pública del
   `{vendor}.certificate` del mismo árbol (herramientas estándar de crypto/ASN.1; el formato es
   `SEQUENCE{INTEGER r(20B), INTEGER s(20B)}` con SPKI DSA-1024 OID `1.2.840.10040.4.1`)
   (`niagara-mental-model-bloque126.md:154`).
2. **Coherencia de campos**: `hostId` == host de origen; `generated` ≤ fecha real del archivo;
   `expiration` coherente (`notes/02-licensing.md:60-67`).
3. **Módulos**: verificar `.jar.sig` (RSA-2048, 256 B detached) con `DsfUtil::checkFileSignature`
   equivalentes, o `nverify.exe` — **sin** wildcards `--unsigned *`/`--removed *`
   (`niagara-mental-model-bloque126.md:105-127,170-174`).
4. **Truststores**: comparar contra un baseline conocido (p. ej. el vanilla vs `SEJOFA` en
   `truststore.jks`; `niagara-mental-model-bloque40.md` §40.4.6). Verificar que el ancla **Angeles**
   esté en `cacerts.bks` y no haya entradas residuales (B113 checklist H4-H8,
   `niagara-mental-model-bloque113.md:203-216`).
5. **Flags de licencia**: buscar `skipModuleValidation`, `smDeveloperMode`,
   `license.unreleasedSoftware=true`, `exemptions.tes` poblado — en producción son señales
   (`niagara-mental-model-bloque75.md:80,106`; `niagara-mental-model-bloque113.md:207-214`).

### Fase C — Correlación

- Cruzar: ¿el módulo sospechoso aparece como `UNSIGNED`/`INVALID_SIGNATURE` en el SecurityDashboard
  y como warning `"No code signers..."` en el classloader?
  (`niagara-mental-model-bloque112.md:121-127`).
- ¿El daemon log tiene el `ERROR: Host Id cannot be found/generated.` o marcadores
  `>>> hostid.debug >>>`? (síntoma de ambiente clonado; `niagara-mental-model-bloque124.md:68,103,173`).
- ¿Hay desacuerdo entre el text-match nativo y la capa Java?
  (`niagara-mental-model-bloque126.md:169-177`).
- ¿El audit está vacío/truncado con actividad reciente? (patrón B75;
  `niagara-mental-model-bloque75.md:31-38`).

---

## 7. Limitaciones del enfoque (ser honestos)

- DSA-1024/SHA-1 de 2003: si el atacante obtiene una clave privada de vendor o un firmador interno,
  la detección por firma no alcanza — se cae a correlación de comportamiento
  (`niagara-mental-model-bloque126.md:178-183`).
- Sin OCSP/CRL: un cert revocado sigue siendo aceptado (`niagara-mental-model-bloque27.md:894-898`).
- El text-match nativo es manipulable por diseño (no verifica firma) — es una señal de desacuerdo,
  no un verificador (`niagara-mental-model-bloque126.md:169-177`).
- Los flags de licencia (`skipModuleValidation`, `smDeveloperMode`) no se pueden quitar de la
  licencia; la mitigación es blacklist de sysprops en el launcher, no forense
  (`niagara-mental-model-bloque75.md:98-106`).

---

## Fuentes

- `niagara-mental-model-bloque126.md` — esquemas de firma, `dsfspi.dll`, `nverify.exe`, `isFeaturePresent`
- `niagara-mental-model-bloque112.md` — SecurityDashboard, PolicySpy, daemon log, IOC, erasabilidad
- `niagara-mental-model-bloque75-security-incident.md` — incidente B75, fail-open/fail-closed, gaps
- `niagara-mental-model-bloque113.md` — hardening del code-signing, checklist trust (H4-H8)
- `niagara-mental-model-bloque18.md` / `bloque27.md` — permission groups, truststores, certs
- `niagara-mental-model-bloque40.md` §40.4.x — truststore.jks, layout de licencias
- `notes/02-licensing.md` — pipeline de validación
- `niagara-n4-incident-response-playbook.md` — respuesta a incidentes completa
