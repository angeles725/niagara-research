# Block 234 — Reflow: qué módulos Niagara usa y para qué (dependencias + tipos)

> **Qué documenta.** El árbol de dependencias de módulos Niagara de Reflow (`-rt` y `-ux`), para qué usa cada uno,
> y los tipos que registra. Gap BG22 (reapertura grupo D, pedido del usuario). Da la capa "de abajo" del stack
> (los módulos del framework, complementando B216 que cubrió las libs de terceros).
>
> **Alcance.** Las dependencias declaradas y su propósito + los tipos registrados. El detalle de cada subsistema ya
> está en sus bloques (se remite).
>
> **Fuentes (primarias).** `META-INF/module.xml` de `nmodsreflow-rt` y `nmodsreflow-ux` (leídos directo). Cross-ref
> a bloques del corpus para el "cómo se usa cada módulo".
>
> **Método / markers.** `[CERT]` = leído en `module.xml`. `[CERT]`+remisión = el uso confirmado en un bloque previo.
> `[INFER]` = deducción del propósito.

---

## 234.1 — Dos módulos, un producto `[CERT]`

Reflow se distribuye en dos módulos Niagara (ambos `vendorVersion 1.7.7.75`, `vendor NiagaraMods`,
`preferredSymbol nmflow`, `moduleName nmodsreflow`, `bajaVersion 0`, Tridium 4.6):
- **`nmodsreflow-rt`** (`runtimeProfile="rt"`): el backend — corre en la station (JACE/Supervisor).
- **`nmodsreflow-ux`** (`runtimeProfile="ux"`): la capa de vistas — registro de las 3 vistas + loaders JS.

## 234.2 — Dependencias del `-rt` (12 módulos Tridium) y para qué `[CERT]`

`nmodsreflow-rt` declara 12 dependencias Tridium 4.6:

| Módulo | Para qué lo usa Reflow | Evidencia |
|---|---|---|
| **baja** | Framework core (BComponent, slots, ORD, BajaScript) — todo | todo el corpus |
| **web-rt** | El servlet HTTP (`BaseServlet`/`SocketServlet`) que sirve la SPA + REST + WebSocket | B138/B149 |
| **history-rt** | `BHistory` para los charts de historia (`historyChart`, sparkline) + cache GZIP | B141, B224 |
| **alarm-rt** | `BAlarmService` + clases de alarma para el widget `alarm` y la consola | B142 |
| **control-rt** | `BControlPoint` (numeric/boolean writable) — setpoints, toggle, valores de punto | B146/B171-adj |
| **schedule-rt** | `BSchedule` para el widget `schedule-list` + WebScheduler | B175-adj |
| **bql-rt** | Queries BQL (alarmas, history, nav — los command agents) | B142/B146 |
| **bacnet-rt** | Puntos BACnet (el protocolo de campo dominante en los equipos) | `[INFER]` (driver de campo) |
| **driver-rt** | Framework de drivers (`BDevice`, enumerar puntos de equipos — el auto-binding) | B228 |
| **net-rt** | NiagaraNetwork — historias/puntos CROSS-STATION (el `HistoryGhostSubscriber` fix) | B230 |
| **box-rt** | Protocolo Fox/box (comunicación station↔station, subscripción baja remota) | `[INFER]` (Fox) |
| **platform-rt** | `BSystemPlatformService.getHostId()` — el host-ID que ancla el licensing | B232, B139 |

**Lectura** `[INFER]`: la lista revela que Reflow es un **agregador transversal** — toca casi todos los subsistemas
de una station (control, alarm, history, schedule, bacnet, net) porque el dashboard visualiza TODO lo de la station,
no un dominio acotado.

## 234.3 — Dependencias del `-ux` (13, +nmodsreflow-rt) `[CERT]`

`nmodsreflow-ux` declara las mismas 12 + **`nmodsreflow-rt` (NiagaraMods 1.7)** — el `-ux` depende del `-rt` (para
`BReflowService`, el tipo sobre el que registra sus vistas). No declara `bajaux`/`workbench` como dependencia de
módulo explícita, pero sí declara permission-groups `workbench` (§234.5) — las 3 vistas son view-agents que corren
en el entorno web/WB de Niagara `[CERT]`.

## 234.4 — Tipos registrados `[CERT]`

**`-rt`** registra:
- `BReflowService` (`ReflowService`) — el servicio central; todo lo demás cuelga de él (`@AgentOn`).
- `BReflowScheme` (`ReflowScheme`, `ordScheme="reflow"`) — el ORD scheme propio `reflow:` (deep-linking, B152).
- `BDateRangeEnum` (`DateRangeEnum`) — vocabulario de rangos de tiempo (history).
- **8 command agents** (`ReflowLicenseCommands`, `ReflowFileCommands`, `ReflowNavCommands`, `ReflowCSVCommands`,
  `ReflowHistoryCommands`, `ReflowAlarmCommands`, `ReflowUserCommands`, `ReflowBQLCommands`) — todos
  `@AgentOn(ReflowService)` con `requiredPermissions="r"` (B146).
- `BReflowChannelService`, `BReflowWebSocketAcceptor`, `BReflowSyncService` — la infra de WebSocket + sync (B140/B221).

**`-ux`** registra las 3 vistas, todas `@AgentOn(ReflowService)`:
- `BReflow` (`Reflow`) — agent `requiredPermissions="r"` (el dashboard viewer).
- `BReflowConfig` (`ReflowConfig`) — agent **`requiredPermissions="rw"`** (el editor — gate de VISTA, B151/B223).
- `BReflowRedirect` (`ReflowRedirect`) — agent `"r"` (redirect/deep-link).

## 234.5 — ORD scheme propio + permisos `[CERT]`

Reflow registra su **propio ORD scheme `reflow:`** (`BReflowScheme`, `ordScheme="reflow"`) — permite ORDs como
`|reflow:` que el loader traduce al hash del router (B152 §152.x). Las permission-groups del `-ux` son `all`,
`workbench`, `station` `[CERT]` — la vista puede abrirse en los tres contextos (de ahí que corra en Workbench Y en
el navegador, BG23/B235).

## 234.6 — Conexiones

- **[Block 216]** — cubrió las libs de TERCEROS embarcadas (jackson, Vue, d3…); §234 cubre la capa de abajo: los
  MÓDULOS del framework Niagara de los que Reflow depende.
- **[Block 138]** — el `BReflowService` y la espina; §234 lista todos los tipos que registra.
- **[Block 228]** — `driver-rt` es lo que enumera los puntos de un equipo para el auto-binding.
- **[Block 230]** — `net-rt` habilita las historias cross-station (el `HistoryGhostSubscriber`).
- **Hacia BG23 (B235)**: las 3 vistas registradas aquí son el objeto del sistema de vistas (cómo se sirven en
  Workbench vs navegador vs perfiles).
