# nmodsreflow (NiagaraMods Reflow v1.7.7 `-rt`) — Research State

> Focus: **arquitectura backend del módulo `nmodsreflow` runtime `-rt`, build 1.7.7.75** — reconstruir
> el modelo mental del módulo OEM de tercero: service central, superficies HTTP/WebSocket, y cómo los
> subsistemas (history/alarms/sync/backups/licensing/config) cuelgan del `BReflowService`. Vista de
> ARQUITECTURA/estructura, no wire-level de protocolos Niagara (eso es el focus `protocols`).
> READ-ONLY. Corpus language: Spanish (technical EN).
>
> Source root (primaria, JAR embarcado — bytecode que corre):
> `/home/cristian/modules/Prototipos/modulos/organized/nmodsreflow77/` (`-rt` + `-ux`, decompile Vineflower).
> Fuente relacionada NO autoritativa (dev tree divergente, nombres desofuscados):
> `/home/cristian/modules/Prototipos/Reflow-Clean-177/`. DIFF forense 1.7.5↔1.7.7:
> `/home/cristian/modules/Prototipos/modulos/REFLOW-175-vs-177-DIFF.md`.
> Tools: `research-sdd/toolbelt` (Vineflower ya aplicado) + lectura directa + CodeGraph + grep.
> Mirrored in engram (project `niagara-research`): `research/niagara/nmodsreflow/gaps`, `.../nmodsreflow/progress`.

## Why this focus exists

El corpus ya auditó Reflow v1.7.5 cross-stack (B50/B51, Capa 17) a nivel de descubrimiento (frontend↔-ux,
bundle deobfuscado, bugs). Este focus dedicado reconstruye el **backend `-rt` del build 77** con rigor
`file:line` sobre el JAR embarcado: el esqueleto del módulo primero, luego cada subsistema. El usuario
eligió el ángulo "Arquitectura backend -rt" (2026-07-01).

## Coverage

- **Covered blocks (este focus)**: 6 — B138 (módulo/service/espina HTTP-WebSocket), B139 (licensing),
  B140 (canal WebSocket: acceptor/sesiones/pub-sub/dispatch), B141 (history: cache gzip disco, threading
  privilegiado, ghost-subscribe, grouping), B142 (alarms: query read-only, doPrivileged ancho, BQL injection uuid),
  B143 (sync: config JSON-Patch multiusuario, doPrivileged config-write sin perms, favoritos por-usuario, sin locking).
- **Coverage metric**: 6 / 12 gaps cerrados (+ 1 sub-gap nuevo R13 descubierto).
- **Last iteration**: 2026-07-02 — R7 cerrado (sync): `BReflowSyncService` (BAbstractService bajo BReflowService)
  = colaboración realtime multiusuario sobre `config.json` compartido vía deltas JSON-Patch (zjsonpatch). El
  comando `sync-delta` **aplica y persiste un JSON-Patch del cliente bajo `doPrivileged` ancho SIN
  `requiredPermissions`** (4to subsistema con el patrón, único que escribe estado); `ConfigIO` sin locking
  (threads crudos); favoritos por-usuario self-scoped por username server-side (sin cross-user write ni path
  traversal alcanzable). Nota cross-focus REFORZADA en B143 §143.7 (agrega la superficie config-write).

## Gap-backlog (priorizado)

| Prioridad | Gap | Tipo/fuente | Estado |
|---|---|---|---|
| — | R1 · esqueleto: módulo, `BReflowService`, ORD scheme, `BaseServlet`/`SocketServlet` | Java `-rt` | **cerrado B138** |
| — | R2 · canal WebSocket: `BReflowChannelService` + `BReflowWebSocketAcceptor` + `IReflowCommand` (pub/sub, dispatch de comandos, sesiones) | Java `-rt` | **cerrado B140** |
| — | R4 · licensing: `License`/`LicenseValidator`/`LicenseManager`/`LicenseClient` (RSA-SHA256, host binding, `api.niagaramodules.com`, station-type gating) | Java `-rt` | **cerrado B139** |
| — | R5 · history: `HistoryIO`/`HistoryData`/`HistoryGhostSubscriber`/`HistoryGroups` (cache GZIP, threading privilegiado, lookup por id) | Java `-rt` | **cerrado B141** |
| low | R3 · montaje del servlet: cómo `BaseServlet`/`SocketServlet` reciben path en Jetty (cross-ref `web-rt`, B9) | Java `-rt` + framework | casi-cerrado B140 (web.xml `/ws` + `/*`); resta base `/module/<name>/` |
| — | R6 · alarms: `ReflowAlarmSource`/`AlarmData`/`QueryFilter`/`AlarmSourceCollection` + `AlarmQueryResponse` (POST) | Java `-rt` | **cerrado B142** |
| — | R7 · sync: `BReflowSyncService`/`ConfigIO`/`ReflowSyncResponse` + favoritos ORD-tree (multiusuario) | Java `-rt` | **cerrado B143** |
| medium | R8 · backups: `BackupManager` (daily/incremental) + `Backup*Response` | Java `-rt` | pending |
| medium | R9 · config: `ConfigResponse`/`ConfigUpdateResponse`/`ConfigDeltaResponse` (contrato config.json + JSON Patch RFC6902 de B51) | Java `-rt` | pending |
| medium | R10 · command agents: los 8 `BReflow*Commands` (License/File/Nav/CSV/History/Alarm/User/BQL) como superficie de comandos | Java `-rt` | pending |
| low | R11 · util: `RangeCalculator`/`CompareRangeCalculator`/`PointHelper`/`NavNodeSerializer`/`Json`/`StringUtils`/`BDateRangeEnum` | Java `-rt` | pending |
| low | R12 · contrato de datos frontend↔-rt: shapes JSON de los responses (parcialmente en B50/B51) | Java `-rt` + bundle | pending |
| medium | R13 · taint source HTTP: `http/util/Query` (`method_363`) + `QueryFilter.make` — cómo TODO el filtrado se construye desde input crudo (feed de la BQL injection de B142 y del doPrivileged) | Java `-rt` | pending (descubierto en B142) |

## Iteration history

| # | Fecha | Gap cerrado | Bloque | Nuevos gaps |
|---|---|---|---|---|
| 1 | 2026-07-01 | R1 esqueleto backend `-rt` | B138 | R3 (montaje servlet) formalizado desde hallazgo in-block |
| 2 | 2026-07-01 | R4 licensing | B139 | 0 (subsistema autocontenido; cruza a B75/B113/B126) |
| 3 | 2026-07-01 | R2 canal WebSocket | B140 | 0 (cerró colateralmente el mount GAP de B138 → R3 casi-cerrado) |
| 4 | 2026-07-02 | R5 history | B141 | 0 (subsistema autocontenido; abre nota de síntesis cross-focus security) |
| 5 | 2026-07-02 | R6 alarms | B142 | R13 (taint source `http/util/Query`+`QueryFilter.make`, feed de BQL injection) |
| 6 | 2026-07-02 | R7 sync | B143 | 0 (config-write surface; refuerza nota cross-focus con superficie de escritura) |

## Blocked gaps (con lo que necesitan)

- (ninguno) — todo el focus es read-only-investigable sobre el JAR ya decompilado.

## Stop control (primario = read-only-investigable = 0, METHODOLOGY §8)

- **Open gaps — read-only investigable**: 7 (R8–R13; R3 casi-cerrado)   ← el loop STATIC para cuando llegue a 0
- **Open gaps — requires-execution**: 0
- **Open gaps — blocked** (hardware/live/NDA): 0
- Iteraciones consecutivas con backlog vacío (secundario): 0/2
- Próximo gap (según prioridad): **R8 · backups** (`BackupManager` daily/incremental + `Backup*Response`). Alternativa de alta señal: R13 (taint source `http/util/Query`, cierra el análisis de la BQL injection de B142).
- Budget cap: none

## Self-verify

- **B138**: 20/20 tokens `[CERT]` grep-confirmados (incl. T20=0 → divergencia Clean-177). `[CERT]` ~28 ·
  `[CERT-a]` 1 · `[INFER]` 1. Ratio `[INFER]/[CERT]` ≈ 0.04.
- **B139**: 16/16 tokens `[CERT]` grep-confirmados. `[CERT]` ~32 · `[CERT-a]` 1 (DIFF forense §3) ·
  `[INFER]` 4 (implicaciones de seguridad, claramente etiquetadas). Ratio `[INFER]/[CERT]` ≈ 0.13 —
  evidencia abundante; los `[INFER]` son análisis de seguridad derivado, no huecos de evidencia.
- **B140**: `[CERT]` ~58 (todos `file:line` sobre el JAR embarcado build .75; registro de comandos
  grep-confirmado) · `[INFER]` 8 (comportamiento/seguridad derivado, cada uno anclado en líneas `[CERT]`).
  Ratio `[INFER]/[CERT]` ≈ 0.14. Hallazgos notables: montaje real vía `web.xml` `/ws`; dispatch bajo
  `AccessController.doPrivileged`; bug de fan-out en `ReflowChannel.broadcast(except)` (`return` en vez de
  `continue`); WS atado a la sesión HTTP Niagara.
- **B141**: 11/11 tokens load-bearing `[CERT]` grep-confirmados en su `file:line` exacto (T1–T9 + streaming
  gzip + `getBit(4)`/`BOrd("history:")`). `[CERT]` ~40 · `[INFER]` 9 (todos anclados a líneas `[CERT]`
  verificadas). Ratio `[INFER]/[CERT]` ≈ 0.22 — los `[INFER]` son análisis de riesgo/seguridad derivado, no
  huecos de evidencia. Hallazgos notables: la "cache GZIP" son 2 blobs en disco (no memoria) con TTL
  wall-clock; query bajo `doPrivileged` ancho alimentado por query-string HTTP; ghost-subscribe fire-once
  con posible leak; BQL por concat; page-count roto (int-divide antes de `ceil`).
- **B142**: 9 grupos de tokens load-bearing `[CERT]` grep-confirmados en su `file:line` exacto (uuid BQL
  `:82` · doPrivileged `:122-126` · AlarmDbConnection/timeQuery `:250,254` · perms `="r"` `:24` ·
  POST parse `:31,35,38` · String `==` `:340,343` + CSV `:28` · catch vacío `:132` · HashMap collection).
  `[CERT]` ~42 · `[INFER]` 11 (análisis de seguridad/correctitud, todos anclados). Ratio `[INFER]/[CERT]`
  ≈ 0.26. Hallazgo de mayor señal: **BQL injection vía `uuid` sin escapar alcanzable a read-permission**;
  3er subsistema con `doPrivileged` ancho; subsistema read-only (sin ack). Refuerza la nota cross-focus.
- **B143**: 7 grupos de tokens load-bearing `[CERT]` grep-confirmados en su `file:line` exacto (BAbstractService
  `:35` + isParentLegal `:65-66` · doPrivileged `:339`/`JsonPatch.apply(delta)` `:420` · Timer debounce
  `:21-22,266` · ConfigIO paths `:18-20` + threads crudos `:25,48,54,88` · favoritos user server-side
  `Read:31-32`/`Write:26-27` · info-leak `Read:63` · **ausencia de `requiredPermissions` bajo `SYNC/`**
  grep-negativo). `[CERT]` ~40 · `[INFER]` 12 (análisis de seguridad/concurrencia, todos anclados). Ratio
  `[INFER]/[CERT]` ≈ 0.30. Hallazgo de mayor señal: **config-write de un JSON-Patch del cliente bajo
  `doPrivileged` ancho SIN permission check** (4to subsistema, único que escribe estado); `ConfigIO` sin
  locking; favoritos self-scoped por username server-side (sin cross-user write ni path traversal alcanzable).
