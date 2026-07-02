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

- **Covered blocks (este focus)**: 10 — B138 (módulo/service/espina HTTP-WebSocket), B139 (licensing),
  B140 (canal WebSocket: acceptor/sesiones/pub-sub/dispatch), B141 (history: cache gzip disco, threading
  privilegiado, ghost-subscribe, grouping), B142 (alarms: query read-only, doPrivileged ancho, BQL injection uuid),
  B143 (sync: config JSON-Patch multiusuario, doPrivileged config-write sin perms, favoritos por-usuario, sin locking),
  B144 (backups: path traversal por sanitización asimétrica, cero autorización, ops destructivas GET),
  B145 (config REST: read `?file=` traversal, overwrite total sin auth, delta = 2ª puerta a applyConfig),
  B146 (8 command agents: todos gatean `"r"`, ops potentes mal escaladas; REST bypassa el gate),
  B147 (taint source: `Query.method_363` URL-decode sin sanitizar; `QueryFilter` no cubre los params peligrosos).
- **Coverage metric**: 10 / 13 gaps cerrados (R13 incluido en el denominador).
- **Last iteration**: 2026-07-02 — R13 cerrado (taint source): `Query.method_363` (`http/util/Query`) es un parser mínimo que **URL-decodifica el
  query string crudo sin sanitizar** (agrava el taint: `%2F`→`/`, `%27`→`'` reconstruyen los chars del
  traversal/injection); guard asimétrico (crash sin `=`, mapComplex sí protege). `QueryFilter.make` es un filtro
  TIPADO sólo para campos de alarma que **NO cubre** los params peligrosos (`file`/`query`/`uuid`, grep-negativo)
  → esos van directo fuente→sink. **Cierra el hilo de seguridad end-to-end**: nada aguas arriba mitiga; los
  hallazgos B142/B144/B145/B146 son explotables de punta a punta. Nota cross-focus CERRADA en B147 §147.4.

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
| — | R8 · backups: `BackupManager` (daily/incremental) + `Backup*Response` | Java `-rt` | **cerrado B144** |
| — | R9 · config: `ConfigResponse`/`ConfigUpdateResponse`/`ConfigDeltaResponse` (contrato config.json + JSON Patch RFC6902 de B51) | Java `-rt` | **cerrado B145** |
| — | R10 · command agents: los 8 `BReflow*Commands` (License/File/Nav/CSV/History/Alarm/User/BQL) como superficie de comandos | Java `-rt` | **cerrado B146** |
| low | R11 · util: `RangeCalculator`/`CompareRangeCalculator`/`PointHelper`/`NavNodeSerializer`/`Json`/`StringUtils`/`BDateRangeEnum` | Java `-rt` | pending |
| low | R12 · contrato de datos frontend↔-rt: shapes JSON de los responses (parcialmente en B50/B51) | Java `-rt` + bundle | pending |
| — | R13 · taint source HTTP: `http/util/Query` (`method_363`) + `QueryFilter.make` — cómo TODO el filtrado se construye desde input crudo (feed de la BQL injection de B142 y del doPrivileged) | Java `-rt` | **cerrado B147** |

## Iteration history

| # | Fecha | Gap cerrado | Bloque | Nuevos gaps |
|---|---|---|---|---|
| 1 | 2026-07-01 | R1 esqueleto backend `-rt` | B138 | R3 (montaje servlet) formalizado desde hallazgo in-block |
| 2 | 2026-07-01 | R4 licensing | B139 | 0 (subsistema autocontenido; cruza a B75/B113/B126) |
| 3 | 2026-07-01 | R2 canal WebSocket | B140 | 0 (cerró colateralmente el mount GAP de B138 → R3 casi-cerrado) |
| 4 | 2026-07-02 | R5 history | B141 | 0 (subsistema autocontenido; abre nota de síntesis cross-focus security) |
| 5 | 2026-07-02 | R6 alarms | B142 | R13 (taint source `http/util/Query`+`QueryFilter.make`, feed de BQL injection) |
| 6 | 2026-07-02 | R7 sync | B143 | 0 (config-write surface; refuerza nota cross-focus con superficie de escritura) |
| 7 | 2026-07-02 | R8 backups | B144 | 0 (path traversal + zero-auth destructivo; pico de la superficie agregada) |
| 8 | 2026-07-02 | R9 config REST | B145 | 0 (confirma config-write sin auth por REST; agrega read-traversal `?file=`) |
| 9 | 2026-07-02 | R10 command agents | B146 | 0 (REVISA framing: gate `"r"` uniforme; REST bypassa el gate; ops mal escaladas) |
| 10 | 2026-07-02 | R13 taint source | B147 | 0 (cierra el hilo de seguridad end-to-end: URL-decode sin sanitizar, params peligrosos bypassan QueryFilter) |

## Blocked gaps (con lo que necesitan)

- (ninguno) — todo el focus es read-only-investigable sobre el JAR ya decompilado.

## Stop control (primario = read-only-investigable = 0, METHODOLOGY §8)

- **Open gaps — read-only investigable**: 3 (R11 util, R12 contrato de datos; R3 casi-cerrado)   ← el loop STATIC para cuando llegue a 0
- **Open gaps — requires-execution**: 0
- **Open gaps — blocked** (hardware/live/NDA): 0
- Iteraciones consecutivas con backlog vacío (secundario): 0/2
- Próximo gap (según prioridad): **R11 · util** (`RangeCalculator`/`CompareRangeCalculator`/`PointHelper`/`NavNodeSerializer`/`Json`/`StringUtils`/`BDateRangeEnum`). Luego R12 (contrato de datos frontend↔-rt). Al llegar a investigable=0, NEXT-ACTION = **bloque de síntesis cross-focus de seguridad** (nmodsreflow × platform-security), ya maduro y con el hilo cerrado end-to-end (B147 §147.4).
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
- **B144**: tokens load-bearing `[CERT]` grep-confirmados en `file:line` exacto — sanitize regex create `:215`
  vs concats sin sanitizar destroy `:64`/apply `:174`/rename `:89` (newName scrub `:83`); `delete()` `:69`;
  ausencia total de `requiredPermissions`/`doPrivileged`/permission-checks en `backups/`+`Backup*Response`
  (grep-negativo); las 6 Response = `serve` GET-shaped `getQueryString`; reset delete `config.json` `:20`.
  `[CERT]` ~34 · `[INFER]` 14 (análisis de explotabilidad; el escape real del `..` marcado `[INFER]` porque
  depende de la semántica de `FilePath`, anclado al concat `[CERT]`). Ratio `[INFER]/[CERT]` ≈ 0.41 — alto
  porque el gap es de seguridad (mucha deducción de impacto sobre pocas líneas load-bearing), no por falta de
  evidencia. **Superficie más grave del focus**: path traversal delete/overwrite/move de `.json` arbitrario +
  wipe de config, todo sin autorización y GET-triggerable (CSRF). Único subsistema sin `doPrivileged`.
- **B145**: tokens load-bearing `[CERT]` grep-confirmados en `file:line` exacto — `ConfigResponse` `?file=`
  override `:28-29` + `findFile` `:37` (GET `:26`) · `ConfigUpdateResponse` body `:51` + Content-Length-only
  `:64` + overwrite directo `:69` · `ConfigDeltaResponse` `applyConfig(...)` `:40` (delega a B143) · ausencia
  total de `requiredPermissions`/`doPrivileged` en las 3 (grep-negativo). `[CERT]` ~30 · `[INFER]` 12 (impacto
  de seguridad, todos anclados). Ratio `[INFER]/[CERT]` ≈ 0.40. Confirma la tesis: config mutable sin auth por
  REST (3 vías) + traversal de LECTURA arbitraria (`?file=`) con passthrough de secretos + audit trail forjable
  (author de headers `Client-*`). Patrón de seguridad agregado totalmente caracterizado → síntesis cross-focus
  madura como NEXT-ACTION.
- **B146**: tokens load-bearing `[CERT]` grep-confirmados en `file:line` exacto — `requiredPermissions="r"`
  en los 8 (`@AgentOn`: BQL:29 · File:23 · User:18 · License:27 · Nav:22 · CSV:26 · History:21 · Alarm:24) ·
  BQL `BOrd.make(query)` `:50`/`:64` + `ord.get(null)` `:68` (Context nulo) · license refresh `:169` ·
  File sólo `listFiles` `:33`+`findFile` `:64` (sin write) · User sólo `getRoles`/`getAllRoles` `:28-35`
  (sin CRUD) · Alarm `canAcknowledgeAlarms` `:101` (informativo). `[CERT]` ~34 · `[INFER]` 10 (todos
  anclados). Ratio `[INFER]/[CERT]` ≈ 0.29. **REVISE-and-CONFIRM**: los 8 gatean `"r"` (corrige "cero
  autorización"), pero mal escalado + REST bypassa el gate (cabalga `@AgentOn`, no el dato). Negativos: sin
  file-write/delete, sin user-CRUD (colapsan 2 sub-claims de la tesis).
- **B147**: lectura directa completa de las 2 clases (203 líneas) + grep-confirmación de negativos. Tokens
  load-bearing `[CERT]`: `URLDecoder.decode` `Q:19` (URL-decode del input crudo) · split `&`/`indexOf("=")`
  `Q:15,18` · guard asimétrico `Q:18-19` vs `mapComplex Q:32-33,38` · `QueryFilter` campos sólo de alarma
  `QF:16-24,26-112` · **`QueryFilter` no referencia `file`/`query`/`uuid`** (grep-negativo) · sinks directos
  `BackupDestroyResponse:13`/`ConfigResponse:27`/`BackupApplyResponse:18`. `[CERT]` ~22 · `[INFER]` 6 (todos
  anclados). Ratio `[INFER]/[CERT]` ≈ 0.27. **Cierra el hilo end-to-end**: el único transform fuente→sink es
  URL-decode (agrava el taint), y los params peligrosos no pasan por el filtro tipado → explotabilidad
  confirmada, no teórica.
