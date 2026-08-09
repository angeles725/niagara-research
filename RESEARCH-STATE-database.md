# RESEARCH-STATE — focus: database (BOOTSTRAPPED, 0/10)

> Multi-focus corpus (METHODOLOGY §16). Focus **BOOTSTRAPEADO 2026-08-09** a pedido explícito del usuario
> ("documentar todo lo relacionado a la base de datos de Niagara N4"). Surge del hilo del pedido de un
> cliente (alarma→correo) al constatar que el corpus NO tiene un bloque dedicado a la capa de persistencia.
>
> **NO es terreno virgen** — backlog audit-first (no lista de deseos), sembrado por audit sweep 2026-08-09.
> Cobertura previa verificada (queda **REMITTANCE**, no se re-deriva):
> - **[Bloque 5]** — formato BOG/`config.bog` (XML, handles, links, LoadOp, rename atómico), ORD, BQL/NEQL scheme.
> - **[Bloque 21]** — motor de consulta BQL y NEQL a nivel de gramática (parsers, AST, agregados, traversal).
> - **[Bloque 33]** — formato `.hdb` (MAGIC `0xA0F61E5E`, paged, dos versiones, no SQLite, truncación).
> - **[Bloque 34]** — formato `.adb` (MAGIC `0x6010ACCD`, AlarmStore/Block/Page, FreePageMap, journal `.drr`).
> - **[Bloque 39]** — backup `.dist` + `excludeFiles` de `.hdb`/`.adb` (online vs offline).
> - **[Bloque 114]** — cifrado del BOG en reposo (EncryptionKeySource none/external/keyring, `.kr`/`.km`).
> - **[Bloque 123/290]** — decode del `config.bog` vivo (ZIP container).
> - **[Bloque 393]** — integridad: NINGÚN artefacto de datos (`.dist`/audit/history/`.bog`) lleva firma/MAC/checksum.
>
> **Ángulo declarado (§b2)**: la capa de PERSISTENCIA / base de datos de Niagara N4 como subsistema — no el
> formato de cada archivo (ya cubierto), sino la MECÁNICA VIVA que el corpus nunca abrió: el ciclo de guardado
> (trigger/dirty flag), el modelo `BComponentSpace`/`BSpace`, la ejecución de BQL contra el space, la migración
> de BOG entre versiones, y sobre todo la EXPORTACIÓN a RDBMS externo (rdb-rt, alarmOrion, HSQLDB embebido) —
> el puente entre la base interna de Niagara y una base SQL externa, hoy con CERO bloques.

<!-- research-state.v1 -->
schema: research-state.v1
block_scope: shared-global
covered_blocks: 401
gaps_closed: 5
known_gaps: 11
investigable_open: 6
requires_execution_open: 0
blocked_open: 0
deferred_open: 0
undocumented_findings: 0
<!-- /research-state.v1 -->

focus: database
status: bootstrapped
bootstrapped_on: 2026-08-09
block_prefix: niagara-mental-model-bloqueN.md (numeración global; próximo libre: B407)

## Pre-flight e2 — existencia + tamaño MEDIDO

Conteo sobre el pipeline **vineflower** (canónico), raíz
`/home/cristian/modules/Prototipos/modulos/organized/`. Todas las rutas confirmadas en disco 2026-08-09.

| Gap | Módulo/paquete fuente | Tamaño medido |
|---|---|---|
| DB1 | `baja/.../com/tridium/sys/station/BStationSaveJob.java` + `file/file-rt/vineflower/.../bog/BBogSpace.java` | < 10 archivos |
| DB2 | `rdb/rdb-rt/vineflower/` | 236 `.java` (≈102 FQN distintos) |
| DB3 | `alarmOrion/alarmOrion-rt/vineflower/` | 33 FQN distintos |
| DB4 | `migration/migration-rt/vineflower/` | 11 FQN distintos |
| DB5 | `bql/bql-rt/vineflower/com/tridium/bql/` (BLocalBqlResolver + SelectQuery) | 20 archivos |
| DB6 | `docSource/.../history-rt/javax/baja/history/db/` + `rdb-rt/.../BRdbmsHistoryExport.java` | 5 clases db/ |
| DB7 | `docSource/.../baja/javax/baja/space/` | 12 clases |
| DB8 | `rdbHsqlDb/rdbHsqlDb-rt/vineflower/` (lib HSQLDB) + `nHsqlDb-rt` (adapter, vía module-nav) | lib 3rd-party + adapter |
| DB9 | `docSource/.../history-rt/javax/baja/history/` (BCapacity/BFullPolicy/…) + history file layer | 4 clases policy |
| DB10 | `baja/.../BStationSaveJob.java` + `file-rt/.../BBogFile.java`, `BBogSpace.java` | < 10 archivos |
| DB11 | `orion/orion-rt/vineflower/com/tridium/orion/` (BOrionDatabase/BOrionSpace/OrionCursor/OrionAppSchemaManager/ISchemaUpgrader) | 136 `.java` |

## Coverage

- **Covered blocks**: 401 (corpus-wide, shared-global)
- **Coverage metric**: 5 / 11 closed
- **Last iteration**: 2026-08-09 — B406 (DB5: BLocalBqlResolver execution path, BogCursor DFS walk, TOP N, ORDER BY materialization, SKIP M absence)

## Gap-backlog (prioritized)

Formato canónico de 4 columnas exigido por `research-sdd-status.sh`.

| Priority | Gap | Type | Status |
|---|---|---|---|
| high | DB1 BStationSaveJob — qué dispara el guardado del BOG y cómo se propaga el dirty flag desde un write de property hasta un flush programado | decompiled-java | closed (B402) |
| high | DB2 rdb-rt — pipeline completo de export a RDBMS externo: BRdbmsHistoryExport lee .hdb y escribe SQL, dialectos MS-SQL/MySQL/Oracle, selección de driver JDBC, esquema de columnas | decompiled-java | closed (B403) |
| high | DB3 alarmOrion — backend RDB de alarmas: esquema SQL de BOrionAlarmDatabase, paginación de OrionAlarmCursor, BArchiveAlarmProvider.execute() moviendo alarmas cleared de .adb a Orion | decompiled-java | closed (B404) |
| medium | DB4 migration-rt — migración de BOG entre versiones: BIBogElementConverter/MigratorRegistry/BFileMigrator transformando un .bog viejo al cargar (rename/removal de tipos) | decompiled-java | closed (B405) |
| medium | DB5 BLocalBqlResolver — ejecución de una consulta BQL contra el component space: hay índice o walk lineal, cómo se implementan TOP N SKIP M y ORDER BY a nivel cursor | decompiled-java | closed (B406) |
| medium | DB6 BArchiveHistoryProvider — cadena de archival de history (local .hdb → RDB): qué dispara el archival (cron/capacidad), batching, fallo/retry, integración con BRdbmsHistoryExport | decompiled-java | pending |
| medium | DB7 BComponentSpace — ciclo de vida interno: LoadCallbacks/SubscribeCallbacks/TrapCallbacks durante deserialización BOG y operación viva, qué agrega AuditableSpace, cuándo se consulta BHandleScheme | decompiled-java | pending |
| low | DB8 HSQLDB embebido — rol de rdbHsqlDb-rt/nHsqlDb-rt: backend del driver rdb-rt, servidor SQL embebido para uso de station, o solo features opcionales | decompiled-java | pending |
| low | DB9 .hdb retención/rollover — a nivel formato: al llegar a BCapacity, FullPolicy borra del page más viejo (trimToCapacity) o rota archivo; comportamiento al cambiar collection-interval con records existentes | decompiled-java | pending |
| low | DB10 BOG crash-recovery — path bog.tmp→bog.bak→bog en Windows vs POSIX: si el NRE verifica .bog.bak al boot y recupera, edge case MoveFileEx en NTFS | decompiled-java | pending |
| medium | DB11 orion-rt ORM — mapeo objeto-relacional genérico de Niagara sobre RDBMS: BOrionDatabase/BOrionSpace, generación de esquema desde @NiagaraOrionType, OrionCursor, versionado/upgrade (ISchemaUpgrader/BSchemaVersion), sesión/transacción. SURGIÓ en B404 (alarmOrion y lonOrion se apoyan acá) | decompiled-java | pending |

### Remittance (no son gaps — ya cubiertos)

- Formato BOG/serialización XML → **[Bloque 5]**. Cifrado BOG → **[Bloque 114]**. Formato `.adb` → **[Bloque 34]**.
  Formato `.hdb` → **[Bloque 33]**. Gramática BQL/NEQL → **[Bloque 21]**. Backup `.dist` → **[Bloque 39]**.
  Integridad de datos (sin firma) → **[Bloque 393]**. BQL en report → **[Bloque 338/358/360]**.

## Iteration history

| # | Date | Gap closed | Block | Delegated? · model tier | New gaps uncovered |
|---|---|---|---|---|---|
| — | 2026-08-09 | (bootstrap — audit-first) | — | yes · sonnet (audit sweep) | 10 seeded |
| 1 | 2026-08-09 | DB1 | B402 | yes · sonnet (delegated iteration) | 0 |
| 2 | 2026-08-09 | DB2 | B403 | yes · sonnet (structural) + haiku (mechanical enumeration) | 0 |
| 3 | 2026-08-09 | DB3 | B404 | yes · sonnet (structural) + haiku (mechanical enumeration) | 0 |
| 4 | 2026-08-09 | DB4 | B405 | yes · sonnet (inline; ~13 files; no delegation needed) | 0 |
| 5 | 2026-08-09 | DB5 | B406 | no · sonnet-4.6 (inline; 16 files; no delegation) | 0 |

## Blocked gaps (each tagged with what it needs)

- none — los 11 gaps son read-only investigable (fuente confirmada en disco, e2 2026-08-09).

## Stop control (primary = read-only-investigable exhaustion, METHODOLOGY §8)

- **Open gaps — read-only investigable**: 6   ← el loop ESTÁTICO para cuando esto llega a 0
- **Open gaps — requires-execution**: 0
- **Open gaps — blocked**: 0
- Consecutive iterations with empty backlog (secondary): 0/2
- Budget cap: none

## Dismissed file types

- none (focus por-subsistema, no por censo de extensiones)
