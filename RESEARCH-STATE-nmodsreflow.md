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

- **Covered blocks (este focus)**: 1 — B138 (módulo/service/espina HTTP-WebSocket).
- **Coverage metric**: 1 / 12 gaps cerrados.
- **Last iteration**: 2026-07-01 — R1 cerrado (esqueleto backend `-rt`).

## Gap-backlog (priorizado)

| Prioridad | Gap | Tipo/fuente | Estado |
|---|---|---|---|
| — | R1 · esqueleto: módulo, `BReflowService`, ORD scheme, `BaseServlet`/`SocketServlet` | Java `-rt` | **cerrado B138** |
| high | R2 · canal WebSocket: `BReflowChannelService` + `BReflowWebSocketAcceptor` + `IReflowCommand` (pub/sub, dispatch de comandos, sesiones) | Java `-rt` | pending |
| high | R4 · licensing: `License`/`LicenseValidator`/`LicenseManager`/`LicenseClient` (RSA-SHA256, host binding, `api.niagaramodules.com`, station-type gating) | Java `-rt` | pending |
| high | R5 · history: `HistoryIO`/`HistoryData`/`HistoryGhostSubscriber`/`HistoryGroups` (cache GZIP, threading privilegiado, lookup por id) | Java `-rt` | pending |
| medium | R3 · montaje del servlet: cómo `BaseServlet`/`SocketServlet` reciben path en Jetty (cross-ref `web-rt`, B9) | Java `-rt` + framework | pending |
| medium | R6 · alarms: `ReflowAlarmSource`/`AlarmData`/`QueryFilter`/`AlarmSourceCollection` + `AlarmQueryResponse` (POST) | Java `-rt` | pending |
| medium | R7 · sync: `BReflowSyncService`/`ConfigIO`/`ReflowSyncResponse` + favoritos ORD-tree (multiusuario) | Java `-rt` | pending |
| medium | R8 · backups: `BackupManager` (daily/incremental) + `Backup*Response` | Java `-rt` | pending |
| medium | R9 · config: `ConfigResponse`/`ConfigUpdateResponse`/`ConfigDeltaResponse` (contrato config.json + JSON Patch RFC6902 de B51) | Java `-rt` | pending |
| medium | R10 · command agents: los 8 `BReflow*Commands` (License/File/Nav/CSV/History/Alarm/User/BQL) como superficie de comandos | Java `-rt` | pending |
| low | R11 · util: `RangeCalculator`/`CompareRangeCalculator`/`PointHelper`/`NavNodeSerializer`/`Json`/`StringUtils`/`BDateRangeEnum` | Java `-rt` | pending |
| low | R12 · contrato de datos frontend↔-rt: shapes JSON de los responses (parcialmente en B50/B51) | Java `-rt` + bundle | pending |

## Iteration history

| # | Fecha | Gap cerrado | Bloque | Nuevos gaps |
|---|---|---|---|---|
| 1 | 2026-07-01 | R1 esqueleto backend `-rt` | B138 | R3 (montaje servlet) formalizado desde hallazgo in-block |

## Blocked gaps (con lo que necesitan)

- (ninguno) — todo el focus es read-only-investigable sobre el JAR ya decompilado.

## Stop control (primario = read-only-investigable = 0, METHODOLOGY §8)

- **Open gaps — read-only investigable**: 11 (R2–R12)   ← el loop STATIC para cuando llegue a 0
- **Open gaps — requires-execution**: 0
- **Open gaps — blocked** (hardware/live/NDA): 0
- Iteraciones consecutivas con backlog vacío (secundario): 0/2
- Budget cap: none

## Self-verify (B138)

- Tokens `[CERT]` chequeados vía grep: 20/20 presentes (incl. T20=0 confirmando ausencia de `doPut/doDelete`
  en build 77 → divergencia de Clean-177 sostenida). Cero citas alucinadas.
- Marker tally B138: `[CERT]` ~28 · `[CERT-a]` 1 (DIFF forense) · `[INFER]` 1 (hipótesis montaje servlet).
  Ratio `[INFER]/[CERT]` ≈ 0.04 — evidencia investigable abundante, lejos del umbral 0.5.
