# Block 139 — nmodsreflow.77 (`-rt`): subsistema de licensing (dual Niagara/XML, firma RSA, host binding)

> Research de **NiagaraMods Reflow v1.7.7 (build .75), paquete `licensing/` del runtime `-rt`**: cómo
> Reflow decide si está licenciado, con qué límites, y contra qué host. Cubre las 6 clases del paquete
> (`License`, `LicenseValidator`, `LicenseManager`, `LicenseClient`, `Feature`, `FeatureAttribute`), el
> esquema de firma, el host binding, la detección de station-type y el fetch remoto de licencia. NO
> cubre cómo `BReflowService` aplica los límites a la UI (parcialmente en B138 §138.3) ni el bundle
> frontend.
>
> Focus: **nmodsreflow** (arquitectura backend `-rt`). Cierra el gap R4. Corpus language: Spanish (technical EN).
>
> Sources (primarias, JAR embarcado build .75):
> `LIC/` = `/home/cristian/modules/Prototipos/modulos/organized/nmodsreflow77/nmodsreflow77-rt/vineflower/com/niagaramods/nmodsreflow/licensing`
> Fuente secundaria: DIFF forense `/home/cristian/modules/Prototipos/modulos/REFLOW-175-vs-177-DIFF.md §3`
> (nota crítica: el source v1.7.5 disponible tenía el licensing DESHABILITADO con stubs; el build 77
> embarcado lo tiene íntegro/restaurado — este bloque documenta la versión enforced real).
>
> Método: decompile Vineflower del JAR embarcado + lectura directa. Markers:
> `[CERT]` fuente primaria local (`file:line`) · `[CERT-a]` secundaria/foro · `[INFER]` deducción.
>
> Capa 26 (OEM tercero NiagaraMods). Connects [Block 138] (service consume estos límites), [Block 2]
> (licensing Niagara nativo), [Block 126] (esquemas de firma del framework), [Block 75]/[Block 113]
> (code-signing y skipModuleValidation — relevante a la superficie de forja).

---

## 139.1 — Arquitectura: dos vías de licencia `[CERT]`

Reflow acepta la licencia por **dos caminos independientes**, evaluados en `LicenseValidator.validate()`
`[CERT]` `LIC/LicenseValidator.java:90-110`:

1. **Licencia Niagara nativa** — si `LicenseManager.hasNiagaraLicense()` es true, `validate()` retorna
   `true` **sin más chequeos** `[CERT]` `LIC/LicenseValidator.java:91-92`. Es la vía preferente: se
   consulta `Sys.getLicenseManager().checkFeature(vendor, "reflow")` probando 3 vendors en orden
   `NiagaraMods` → `NiagaraModsOrg` → `Tridium` `[CERT]` `LIC/LicenseManager.java:13-36` (también en
   `License.getLicenseVendor()` `LIC/License.java:75-92`). Si el feature `reflow` está en la licencia
   `.lic` de la station bajo cualquiera de esos vendors, Reflow queda licenciado.

2. **Licencia XML propietaria** — archivo `^niagaramods.license` en el station home, parseado con
   `XParser` y **verificado por firma RSA** (§139.3) `[CERT]` `LIC/License.java:145-222`,
   `LIC/LicenseValidator.java:94-108`.

`License.load()` prioriza la nativa: si `hasNiagaraLicense()`, construye el `Feature` desde el
`javax.baja.license.Feature` de Niagara y descarta firma/expiración propias `[CERT]`
`LIC/License.java:112-135`; si no, cae al XML `[CERT]` `LIC/License.java:136-230`. El gate global
`niagaraLicensing` (property del service, B138) puede desactivar la vía nativa `[CERT]`
`LIC/License.java:94-97`.

## 139.2 — El objeto `License` y el formato XML `[CERT]`

Singleton `License.INSTANCE` `[CERT]` `LIC/License.java:26`. Campos: `vendor`, `hostId`, `generated`,
`expiration`, `licensee`, `Feature[] features`, `signature` `[CERT]` `LIC/License.java:18-24`. Path fijo
`^niagaramods.license` (`^` = station home ORD) `[CERT]` `LIC/License.java:28,256,267`.

Estructura XML parseada (`License.load()` `LIC/License.java:150-220`) `[CERT]`:
- Root attrs: `vendor`, `hostId`, `generated` (`yyyy-MM-dd`), `expiration` (opcional), `licensee`.
- Elemento `<signature>` (Base64).
- N elementos `<feature>` con attrs `name`, `version`, `feature-type` (default `"license"`), `feature-sku`,
  `station-type` (default `"all"`), `expiration` (opcional), + attrs libres → `FeatureAttribute[]`
  (se excluyen `name`/`version`/`expiration` del set de attrs) `[CERT]` `LIC/License.java:193-216`.

`Feature` deriva `version:int` del primer componente de `versionString` (`split("\\.",2)[0]`) `[CERT]`
`LIC/Feature.java:27-28`. Tipos de feature: `"license"` (base), `"addon"` (extensión sumable, §139.5).
`FeatureAttribute` es un par `name/value` plano `[CERT]` `LIC/FeatureAttribute.java`.

## 139.3 — Verificación de firma: RSA-SHA256 con public key empaquetada `[CERT]`

`LicenseValidator.verifySignature()` `[CERT]` `LIC/LicenseValidator.java:142-159`:
- Algoritmo **`SHA256withRSA`** (`java.security.Signature`) `[CERT]` `LIC/LicenseValidator.java:148`.
- Clave pública **X.509/PEM leída de un archivo DENTRO del módulo**: `license/public.key`, resuelto vía
  `Sys.getModuleForClass(...).findFile(...)`; se strippean los delimitadores PEM y se decodifica Base64 →
  `X509EncodedKeySpec` → `KeyFactory("RSA")` `[CERT]` `LIC/LicenseValidator.java:20,198-233`.
- Firma: `license.signature` (sin `\n`), Base64-decoded, `verify()` `[CERT]` `LIC/LicenseValidator.java:150-153`.

**`validationString()`** — la cadena firmada `[CERT]` `LIC/LicenseValidator.java:161-196`. Formato
pipe-delimited (`|`), fechas `yyyy-MM-dd`:

```
vendor | hostId.toLowerCase() | generated | licensee | [expiration]
  (por feature:)  | name | version | [feature.expiration]
                  (por attr:)  | attrName=attrValue
```

La cadena completa de `validate()` (vía XML) es: `verifyHostId()` → `verifySignature()` →
`verifyExpiration()`, cada uno logueando su fallo específico `[CERT]` `LIC/LicenseValidator.java:94-108`.
Además, por feature se chequea expiración propia en `verifyFeatureExpiration()` `[CERT]`
`LIC/LicenseValidator.java:73-88, 122-135`.

## 139.4 — Host binding y detección de station-type `[CERT]`

**Host binding** `[CERT]` `LIC/LicenseValidator.java:137-140`:
`license.hostId.toUpperCase().equals(getHostId())`, donde `getHostId()` viene de
`BSystemPlatformService.getHostId().toUpperCase()` `[CERT]` `LIC/LicenseValidator.java:235-238`. Nota de
implementación: el **check** de host usa uppercase, pero la **firma** (`validationString`) usa
`hostId.toLowerCase()` `[CERT]` `LIC/LicenseValidator.java:167` — la licencia debe traer el hostId con el
mismo casing con que fue firmada, mientras que el match de host es case-insensitive por normalización.

**Station-type** `[CERT]` `LIC/LicenseValidator.java:22-46`:

| Tipo | Regla | Cita |
|---|---|---|
| `demo` | licencia Niagara nativa con expiración finita (`getLicenseExpiration != Long.MAX_VALUE`) | `:30-34` |
| `jace` | `hostId` empieza con `QNX`/`LYX`/`WEBX`/`GC5`/`PXC` | `:36-42` |
| `supervisor` | cualquier otro (no-jace) | `:44-46` |

El gating final (feature.stationType vs station real, con excepciones supervisor/jace→demo) lo aplica
`BReflowService.hostValidates()` [Block 138 §138.3] `[CERT]` `RT/BReflowService.java:637-651`. Si no
valida, cae a `setTrialLicense()` (1 building / 1 floor / 10 equip / 3 pages) `[CERT]`
`RT/BReflowService.java:740-748`.

## 139.5 — Manager, addons y fetch remoto `[CERT]`

**`LicenseManager`** (facade singleton) `[CERT]` `LIC/LicenseManager.java:6-8`:
- `refreshLicense(debug)`: si `hasNiagaraLicense()` solo recarga; si no, `new LicenseClient().refreshLicense()`
  (fetch remoto) y recarga `[CERT]` `LIC/LicenseManager.java:50-60`.
- `hasFeature("reflow")` corta-circuito a `true` si hay licencia nativa `[CERT]` `LIC/LicenseManager.java:67-74`.
- **Addons**: features `type="addon"` con el mismo `name`; `getAddonsAttributeCount()` **suma** los
  valores enteros de un attr a través de todos los addons `[CERT]` `LIC/LicenseManager.java:101-122` —
  así se apilan límites (p.ej. `BReflowService.licenseRefreshed()` hace `buildings + addonBuildings`,
  B138) `[CERT]` `RT/BReflowService.java:673-674`.

**`LicenseClient`** — fetch remoto `[CERT]` `LIC/LicenseClient.java:20-47`:
- URL: **`http://api.niagaramodules.com/license/<hostId>`** — HTTP plano (no TLS) `[CERT]` `LIC/LicenseClient.java:21,26`.
- Headers: `Content-Type: application/json`, `Accept: application/xml`,
  `User-Agent: nmodsreflow/1.0.0 (Niagara/<hostId>)` `[CERT]` `LIC/LicenseClient.java:28-30`.
- Sobre HTTP 200, escribe el body a `^niagaramods.license` en el station home `[CERT]` `LIC/LicenseClient.java:33-37`.
- hostId vía `BSystemPlatformService.getHostId()` (sin normalizar) `[CERT]` `LIC/LicenseClient.java:49-52`.

Es el permiso `NETWORK_COMMUNICATION hosts="*"` declarado en `module.xml` [Block 138 §138.2] `[CERT]`.

## 139.6 — Observaciones de seguridad

- **La vía nativa NO verifica firma** `[CERT]` `LIC/LicenseValidator.java:91-92`: si el feature `reflow`
  aparece en la licencia Niagara de la station (vendor NiagaraMods/NiagaraModsOrg/Tridium), Reflow queda
  licenciado sin ninguna validación criptográfica propia. La confianza se delega enteramente al
  `LicenseManager` de Niagara (cuya autenticidad DSA-1024 valida el framework, [Block 126]) `[INFER]`.
- **La public key vive DENTRO del módulo** (`license/public.key`) `[CERT]` `LIC/LicenseValidator.java:20`.
  El JAR está firmado+sealed [Block 138 §138.1], así que reemplazar la key exige romper el module-signing
  de Niagara — exactamente el vector que el corpus documentó como viable vía `skipModuleValidation` /
  truststore comprometido [Block 75]/[Block 113] `[INFER]`. Es la debilidad estructural clásica del
  licensing OEM: la raíz de confianza del esquema propio es tan fuerte como el code-signing del host.
- **Fetch de licencia sobre HTTP plano** `[CERT]` `LIC/LicenseClient.java:21`: el hostId viaja en claro
  (URL path + User-Agent) y la licencia se descarga sin TLS. Un MITM no puede **forjar** (la XML
  descargada igual debe pasar `verifySignature()` con host binding), pero sí puede **denegar** el refresh
  o **fingerprint-ear** hosts NiagaraMods en la red `[INFER]`.
- **Downgrade silencioso a trial**: cualquier fallo de validación (host, firma, expiración, station-type)
  NO bloquea el arranque — degrada a límites trial `[CERT]` `RT/BReflowService.java:653-748`. Fail-open
  hacia funcionalidad reducida, no fail-closed.

## 139.7 — Connections

- **[Block 138]** — `BReflowService` consume este subsistema: llama `doRefreshLicense()` en `started()`,
  timer de refresh 24h, y `licenseRefreshed()` traduce features/attrs a los límites (`buildingLimit`, etc.)
  y `setTrialLicense()` en fallo. Este bloque documenta la máquina de licencia que alimenta esos límites.
- **[Block 2]** — licensing Niagara nativo (HostId, Feature, LicenseManager Tridium): Reflow monta su
  esquema propio ENCIMA del de Tridium y además acepta directamente un feature `reflow` en la licencia
  nativa como bypass.
- **[Block 126]** — esquemas de firma del framework (módulos SHAwithRSA-2048, licencias/certs SHA1withDSA-1024):
  Reflow usa **SHA256withRSA** para su XML propietario, distinto de los 4 esquemas del framework.
- **[Block 75]/[Block 113]** — code-signing y `skipModuleValidation`: definen la superficie por la que la
  public key empaquetada (raíz de confianza del licensing XML) podría ser sustituida.
