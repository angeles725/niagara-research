# Block 163 — chihuahua MX60 (`-rt/-ux/-wb`): identidad, espina HTTP servlet y postura RBAC (esqueleto del focus)

> **Bloque inaugural del focus `chihuahua`** — primer módulo de FUENTE PRIMARIA del corpus (no decompilado):
> un dashboard Niagara N4 de autoría propia (`com.angeles.chihuahua`) para el BMS **Honeywell MX60**. Documenta
> el ESQUELETO: identidad del módulo, su estructura tri-parte, la espina HTTP (`BChiServlet` + dispatch puro) y
> el headline de su postura de seguridad (RBAC write-gate). Los subsistemas (alarms, history, thresholds,
> audit, frontend, WB) son gaps propios del focus. Método: lectura directa de la fuente.
>
> Focus: **chihuahua** (arquitectura del módulo MX60, fuente primaria). Primer bloque. Corpus language: Spanish.
>
> **Sensibilidad:** proyecto de fuente, pero despliegue de cliente real → `.env.local` (IP JACE/credenciales)
> NO se leyó ni se cita (SECRETS DISCIPLINE parcial); el código es citable.
>
> Sources (fuente primaria, base `…/Cliente/Honeywell/MX60/chihuahua/chihuahua/`):
> - `RT/` = `chihuahua-rt/src/com/angeles/chihuahua/`  ·  `UX/` = `chihuahua-ux/src/com/angeles/chihuahua/ux/`
> - `WB/` = `chihuahua-wb/src/com/angeles/chihuahua/wb/`  ·  `JS/` = `chihuahua-ux/src/rc/js/`
> - manifests: `niagara-module.xml`, `build.gradle.kts`, cada `module-permissions.xml`
>
> Markers: `[CERT]` = leído en la fuente primaria (`file:line`, spot-check §11 de 4 citas = OK) · `[INFER]`
> deducción. Capa 26 (módulo dashboard OEM, contraparte de autoría propia frente a nmodsreflow).
>
> Contraparte de comparación: [Block 138]-[Block 155] (nmodsreflow) — la comparación es un bloque POSTERIOR.

---

## 163.1 — Identidad del módulo `[CERT]`

- **Nombre / símbolo / perfiles:** `moduleName="chihuahua"`, `preferredSymbol="chihua"`,
  **`runtimeProfiles="rt,ux,wb"`** `[CERT]` `niagara-module.xml:2`. Vendor `ANGELES`, `vendorVersion` **1.3**
  `[CERT]` `build.gradle.kts:25,36` (historia en comentarios: 1.0→1.1 slot `auditLog` RBAC, 1.2
  export/importLinks, 1.3 la parte nueva `chihuahua-wb`).
- **Build cruzado intencional:** compilado contra **iC-Niagara-4.13.2.18** (PRODUCCIÓN), **Java 8**, desplegado
  a un Supervisor **4.14.0.162** `[CERT]` `CLAUDE.md:6,9`. `[INFER]` El corpus base (B1-B130) documenta ese
  mismo 4.14; chihuahua asume constraints 4.14 en runtime (p.ej. `BAlarmRecord` final en 4.14,
  `ChiAlarmHelper.java:31`).
- **Estructura tri-parte** (paralela a Reflow, distinto reparto):
  | Parte | Rol |
  |---|---|
  | `chihuahua-rt` | modelo de dominio BComponent (`BChiDashboardService`, `BPlanta`, `BChiUp/Carcamo/Datalogger` + Monitors, `ChiLinkHelper`) |
  | `chihuahua-ux` | `BChiServlet` (`BWebServlet`) + `Chi*Helper` backend + frontend ES5 IIFE en `rc/js/` |
  | `chihuahua-wb` | herramienta Workbench `BBatchLinkEditor` (**Reflow no tiene parte WB**) |
- **Dominio:** sitio de refrigeración/bombeo de **6 plantas**; 3 tipos de equipo — **UP** (Unidad Paquete,
  HVAC), **Cárcamo** (pozo/sump, `nivelCm`), **Datalogger** (presión, `pressurePsi/Bar`) `[CERT]` `CLAUDE.md:50`.
  ID canónico `<slot_lowercase>-p<planta>` (p.ej. `ud14-p6`) `[CERT]` `CLAUDE.md:50`.

## 163.2 — La espina HTTP: `BChiServlet` + dispatch puro `[CERT]`

- **Montaje:** `BChiServlet extends BWebServlet`, `getServletName()` → **`"mx60"`** → todo cuelga de **`/mx60/`**
  `[CERT]` `UX/BChiServlet.java:51,143-145`. La SPA estática se sirve del `rc/` del JAR (`RESOURCE_BASE`, `:72`).
- **Routing 100% puro (testeable sin station):** el servlet delega toda la decisión a la función pura
  `ChiServletDispatch.route(method, path, headerLookup, paramLookup)` que devuelve una jerarquía sellada
  `RouteAction`; el servlet solo hace `instanceof` y ejecuta `[CERT]` `UX/ChiServletDispatch.java:379`,
  `UX/BChiServlet.java:166,183`. `[INFER]` diseño deliberado para unit-tests WSL sin JACE.
- **Guards del router** `[CERT]` `UX/ChiServletDispatch.java`: traversal (`..`,`\`,`\0`) → 404 (`:390,454`);
  **`/api/*` exige header `X-Requested-With: XMLHttpRequest`** o redirige 302 a `/mx60/#home` (`:395-401`) —
  CSRF-lite; `/api/csrf-probe` exento por read-only (`:462`).
- **Mapa de endpoints** `[CERT]` (`UX/ChiServletDispatch.java:402-620`):
  - **GET:** `/api/config`, `/api/equipment`, `/api/alarms`, `/api/alarms/summary`, `/api/alarms/sources`,
    `/api/alarms/source`, `/api/alarms/notes/{uuid}`, `/api/alarms/hyperlink`, `/api/historyList`,
    `/api/historyData`, `/api/equipment-histories`, `/api/schedules`, `/api/csrf-probe`, `/api/user/theme`,
    `/api/user/capability`, `/api/audit`, `/api/{up|carcamo|datalogger}/{ord}/thresholds`.
  - **POST (mutaciones):** `/api/setpoint`, `/api/alarms/latch`, `/api/alarms/unlatch`, `/api/alarms/notes`,
    `/api/alarms/ackAll`, `/api/user/theme`, `/api/{up|carcamo|datalogger}/{ord}/threshold`. POST desconocido → 405.
- **Contrato de datos:** JSON armado a mano con `StringBuilder`/`PrintWriter` (sin librería JSON,
  `UX/ChiJsonUtil.java:9`); cada handler en try/catch → 500 `{"error":…}`; headers `application/json` + no-cache
  `[CERT]` `UX/BChiServlet.java:1272`.

## 163.3 — Headline de postura: RBAC write-gate en CADA mutación `[CERT]` (el contraste con Reflow)

`ChiRbacHelper.checkCanWrite(req, resp)` es el **único gate de escritura server-side**, y se invoca como
**primera sentencia de todo handler de POST** antes de parsear el body `[CERT]` `UX/ChiRbacHelper.java:20`
(ADR D2), p.ej. `handleSetpointWrite` `UX/BChiServlet.java:680`, `handleAlarmLatch` `:1321`.

- **Decisión por el bit built-in `OPERATOR_WRITE`** (no por nombre de rol): `canWrite = BPermissions.has(OPERATOR_WRITE)`,
  `role = canWrite ? "operador" : "viewer"` `[CERT]` `UX/ChiRbacHelper.java:15-18` (ADR D1).
- Sin usuario → **401**; usuario sin write → **403** `{"error":"forbidden","role":"viewer"}`; **falla CERRADO**
  (deny ante cualquier excepción) `[CERT]` `UX/ChiRbacHelper.java` (§5 del barrido).
- **Permisos custom:** ambos `module-permissions.xml` son **stubs vacíos** `[CERT]` — el módulo NO declara
  permiso Niagara propio; reutiliza el `OPERATOR_WRITE` de plataforma.

`[INFER]` **Éste es el eje de contraste más agudo con Reflow:** [Block 150] §150.1 probó (y B160 confirmó en
vivo) que Reflow **no gatea la escritura** (config-write a nivel-lectura). chihuahua **gatea cada endpoint
mutante** con `checkCanWrite` fail-closed. La comparación formal es un bloque posterior; aquí queda anclado el
headline.

## 163.4 — Mapa de subsistemas (roadmap de gaps) `[CERT]`

Cada uno con su bloque propio pendiente (backlog C2-C14):

| Subsistema | Clase clave | Gap |
|---|---|---|
| RBAC (deep) | `UX/ChiRbacHelper.java` | C2 |
| Alarms | `UX/ChiAlarmHelper.java` / `ChiAlarmQueryHelper.java` | C4 |
| Audit | `UX/ChiAuditHelper.java` + `RT/…/BChiDashboardService.auditLog` | C5 |
| Threshold/protección | `RT/…/BChiUp.java` (49 slots) + control-tick 10s + `ChiThresholdHelper` | C6 |
| Equipment/config-state | `UX/ChiEquipmentReader.java`, monitors auto-provisioning | C7 |
| Frontend (topología/subs) | `JS/app/SubscriptionPool.js`, `EquipmentData.js` | C8-C9 |
| Workbench tool | `WB/BBatchLinkEditor.java` (chihuahua-only) | C10 |
| Link export/import | `RT/…/ChiLinkHelper.java` → `chih-links.json` | C11 |
| History / Schedule | `UX/ChiHistoryHelper.java` / `ChiScheduleHelper.java` | C12-C13 |
| Build/deploy/tests | `deploy.sh`, gradle multi-módulo, `HANDOFF.md` | C14 |

## 163.5 — Connections

- **[Block 138]** (nmodsreflow `-rt` esqueleto) — la CONTRAPARTE directa: mismo patrón tri-parte + servlet HTTP;
  la comparación chihuahua↔Reflow (dimensión por dimensión) es un bloque de síntesis posterior de este focus.
- **[Block 150]/[Block 160]** — Reflow sin write-auth gate (confirmado en vivo); chihuahua **sí** gatea
  (§163.3) → el eje de contraste de seguridad ya anclado.
- **Backlog C2-C14** — los subsistemas mapeados en §163.4, cada uno un bloque futuro.
- **Focus `chihuahua`** — bloque inaugural; sienta identidad + espina + postura para el loop por subsistema.
