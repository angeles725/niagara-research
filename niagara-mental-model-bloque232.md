# Block 232 — Reflow: licensing como producto (límites, tiers, activación)

> **Qué documenta.** El licensing de Reflow desde el ángulo de PRODUCTO: qué gatea cada límite, los tiers
> (trial/demo/pago), y cómo se activa (host-ID, cloud). Gap BG16 (reapertura grupo B). Reencuadra B139 (que vio
> el licensing por SEGURIDAD: firma RSA, host binding).
>
> **Alcance.** Los límites de producto, el modelo de tiers y el flujo de activación. La criptografía/firma está
> en B139; aquí el comportamiento de negocio.
>
> **Fuentes (primarias).** SPA beautificada (1:1 con `app.4509efb4.js` sha256 `81b82b83…` del ORIGINAL minificado —
> el temp beautificado tiene su propio hash pero es 1:1, 123 740 líneas): `scratchpad/reflow-app.beauty.js`, `BF:`.
> Java RT: `licensing/{LicenseClient,LicenseManager,Feature}.java`, `BReflowService.java`,
> `commands/BReflowLicenseCommands.java`. Barrido delegado (sonnet); tokens re-verificados por el driver.
>
> **Método / markers.** `[CERT]` = fuente primaria. `[INFER]` = deducción.

---

## 232.1 — Los 5 límites y qué gatea cada uno `[CERT]`

Módulo Vuex `license` (`BF:10376`), `state.limits = {buildings, floors, pages, equipment, maps}`. Enforcement en
dos capas: **bloqueo duro al agregar** (`canAddComponent`, `BF:10456`) + **banner "sobre el límite"** (getters
`*Error`) si un config existente ya excede un límite reducido.

| Límite | Gatea | Enforcement |
|---|---|---|
| `buildings` | máx. edificios | `canAddComponent("building")`: `components.buildings < limits.buildings` bloquea el "add" |
| `floors` | máx. floorplans | mismo patrón |
| `pages` | máx. páginas de dashboard | mismo patrón |
| `equipment` | máx. equipos/devices | mismo patrón + check dedicado en el wizard bulk-add (`BF:83191`, "Your license is limited to a maximum of N devices") |
| `maps` | **NO es un conteo** — toggle booleano del widget `weather-map` | `"weather-map"==type && license.limits.maps` (`BF:2090`); clase `disabled-display` si `!maps` |

Cuando se alcanza un límite (buildings/floors/pages/equipment), un modal **"Component Limit Reached"** ("Unable to
add {resource}. You have reached the maximum…permitted in your license") con botón **"Upgrade License"** — o
**"Purchase a License"** si es trial — abre `niagaramods.com/products/reflow?…&key=component-limit&host={hostId}`
(`BF:118278`) `[CERT]`. `9999` = "Unlimited" (sentinela).

## 232.2 — Tiers: trial, demo, pago `[CERT]`

Estado `license` (`BF:10376`): `{type, hostId, isTrial, isOfficeDemo, stationType, expires, limits, trialLimits}`.
- **Trial** (`type:"trial"`, `isTrial:true`): caps hardcodeados `trialLimits = {buildings:1, floors:3, pages:3,
  equipment:10, maps:false}` (`BF:10397`). Banner persistente "…operating in free trial mode" + CTA "Purchase a
  License".
- **Integrator/Office Demo** (`isOfficeDemo`, `type:"demo"`): todos los límites a `9999`/unlimited
  incondicionalmente.
- **Pago**: distinguido por station type — `licenseTypeDescription` (`BF:10510`) mapea `jace`→"JACE",
  `jacepro`→"JACE Pro", `supervisor`→"Supervisor", `site`→"Site", `enterprise`→"Enterprise", `standard`→"Standard".
  Cada uno trae sus límites del license file.

`licenseStatusText` (`BF:103578`): `invalidStationType`→"Invalid License", `isTrial`→"Unlicensed", else→"Active".
Un license firmado pero de station-type equivocado se RECHAZA (`licensed:false`, `invalidStationType:true`) — la
licencia debe coincidir con el tipo de station `[CERT]`.

## 232.3 — Activación: automática, por host-ID, zero-touch `[CERT]`

**No hay campo para tipear una license key** — la activación es automática y keyed por host-ID:
- `LicenseClient.refreshLicense` (`LicenseClient.java:20`): `GET http://api.niagaramodules.com/license/{hostId}`
  (`:21`) y en 200 escribe la respuesta a `^niagaramods.license` en el station home. **No manda token/credencial —
  el host-ID ES el identificador** `[CERT]`. La provisión ocurre out-of-band (el cliente compra en el sitio
  niagaramodules.com, keyed al hostId que la app muestra en los links de compra).
- `LicenseManager.refreshLicense`: si ya hay un license nativo Niagara (`checkFeature`) recarga; si no, delega en
  `LicenseClient` para bajar el XML y recarga `License.INSTANCE`.
- **Automático en cada arranque + timer 24h**: `BReflowService` en `stationStarted()` llama `doRefreshLicense()` +
  arma un timer periódico (`ticketExpired` cada 24h, `BF`/`BReflowService.java:484`) que re-baja el license `[CERT]`.
- **Refresh manual**: `BReflowLicenseCommands.refreshLicense` (comando WS/REST, user-triggerable) fuerza una bajada
  fresca; el botón "Refresh" del settings dispatcha `license/refreshLicense` (`BF:103593`).

## 232.4 — Cómo los límites llegan del license file `[CERT]`

`LicenseManager.getFeatureAttr("reflow", attr)` (`LicenseManager.java:96`) lee atributos de la feature "reflow".
`Feature.java:49` parsea las keys nativas Niagara (`reflow.buildings.limit`, `reflow.floors.limit`,
`reflow.devices.limit`, `reflow.pages.limit`, `reflow.maps.limit`, `reflow.station-type`, `reflow.type`) quitando
prefijo/sufijo: `name.replaceAll("reflow\\.","").replaceAll("\\.limit","")` — así los nombres crudos del license
file SON los nombres de los límites. `BReflowLicenseCommands.licenseData()` los convierte al JSON del cliente:
- El valor literal `"none"` = unlimited → emitido como `9999` `[CERT]`.
- **`buildings` es el ÚNICO límite aumentable por add-ons** (`getAddonsAttributeCount("reflow","buildings")` suma
  add-ons); floors/pages/equipment/maps no tienen suma de add-ons `[CERT]`.
- Nota de naming: el atributo del license file es `devices`, pero el campo cliente/Vuex es `equipment` (split
  license-file vs producto).

## 232.5 — Conexiones

- **[Block 139]** — el licensing por SEGURIDAD (dual Niagara/XML, RSA-SHA256, host binding, api.niagaramodules.com);
  §232 es el mismo subsistema como negocio (límites/tiers/activación).
- **[Block 218]/[Block 222]** — `license.limits.maps` gatea `weather-map` (el único límite-toggle).
- **[Block 228]** — el límite `equipment` se chequea en el wizard bulk-add de equipos (auto-binding).
- **[Block 217]** §217.8 — el `hostId` que ancla el license es el mismo que fuga WeatherMap (B150) — nexo
  seguridad↔licensing.
- **Hacia BG18**: la migración (siguiente) es el otro mecanismo de ciclo de vida; ambos corren en el arranque/load.
