# RESEARCH-STATE — Focus `live-station`

> Focus dinámico (METHODOLOGY §12): validación de la station Niagara N4 **VIVA** en `127.0.0.1` (WSL mirrored).
> Sensibilidad **`live-install` → SECRETS DISCIPLINE** (cita estructura, nunca valores secretos).
> Numeración global de bloques (`niagara-mental-model-bloqueN.md`), compartida con el resto del corpus.
> Engram: `research/niagara/live-station/{progress,gaps}` (proyecto `niagara-research`).
<!-- research-state.v1 -->
schema: research-state.v1
covered_blocks: 266
gaps_closed: 0
known_gaps: 0
investigable_open: 0
requires_execution_open: 0
blocked_open: 0
<!-- /research-state.v1 -->


## Modo de ejecución

**SUPERVISADO, no `/loop` ciego** (§12). Etapa A = probes read-only (rung 1). Etapa B = verificación
autenticada/con escritura → escalera de invasividad, oracle cross-protocolo, backup-before-destroy, OK del
usuario por paso. El usuario de prueba (`API`) se revoca al terminar.

## Cobertura

- **Métrica:** 17 / 19 gaps cerrados (0.89). **Etapa A CERRADA**. Etapa B: 13/14 con veredicto vivo; solo V7/V8 (BQL exacto, canal WS) diferido = requires-execution.
- **Bloques del focus:** B156 (perfil pasivo), B157 (Etapa A auth), B158 (platform + cierre A), B159 (read-side), B160 (config-write CONFIRMADO), B161 (destructivos: backups auth-gated V4/V10, V5 reachable, V7/V8 diferido).
- **Autorización rung-3:** concedida por el operador esta sesión (backup propio verificado sha256 bf70f28f…; el operador tiene el suyo). Expira al cierre de sesión / revocación del usuario `API`.

## Ground-truth vivo (§12 — re-medido, NO heredado)

Ver [Block 156] §156.7. Ancla: cert SHA-256 `C1:01:41:B2:…:E5:D2`, `hostAddress=192.168.100.100`,
`app.name=Station`, Reflow activo (unsplash en CSP). Re-confirmar antes de cada interacción de Etapa B.

## Backlog

### Etapa A — mapear el runtime vivo (read-only)

| Gap | Descripción | Estado | Clasificación |
|---|---|---|---|
| A1 | Perfil pasivo: puertos, cert, fox hello, postura HTTP/TLS | **CERRADO** (B156) | read-only ✓ |
| A2 | Versión exacta Niagara + Reflow — medir en vivo | **BLOCKED** (B158 §158.2): ausencia probada — no disclosed a read-level (spy/doc/module 404/403, platform 403); §12 NO heredada | blocked-on-platform/admin-creds |
| A3 | Superficie web montada de la station (autenticado) | **CERRADO** (B157 §157.2): ord vivo; spy/about/nav/hx no montados; wb 403 | auth read ✓ |
| A4 | Endpoints vivos de Reflow (B138-B149) | **CERRADO** (B157 §157.3-4): mount real `/nmodsreflow/`, config responde JSON read-level; resuelve B138 §178 | auth read ✓ |
| A5 | Platform daemon 3011/5011 — qué expone (identidad + versión, no login) | **CERRADO** (B158 §158.1): 3011/5011 son HTTP(S) Jetty, 403 sin creds de plataforma; sin banner pre-auth | read-only ✓ |

### Etapa B — TERMINAL: verificar los 14 defectos de [Block 150] §150.2 contra la station viva

Cada uno mapea a un item de la tabla de [Block 150]. Verificación autenticada con el usuario de prueba `API`.
Los items de ESCRITURA (config-write/traversal/wipe) son rung 2-3 (§12): backup-before + oracle + OK por paso.

| Gap | Defecto (B150 item) | `file:line` fuente | Rung probable |
|---|---|---|---|
| V1 | Config-write sin auth vía WS (`sync-delta`) | `BReflowSyncService.java:339,420` (B143) | 2-3 (write) |
| V2 | Config-write sin auth REST overwrite total | `ConfigUpdateResponse.java:51,64,69` (B145) | 2-3 (write) |
| V3 | Config-write sin auth REST JSON-Patch | `ConfigDeltaResponse.java:40` (B145) | 2-3 (write) |
| V4 | Traversal de escritura destructivo | `BackupManager.java:64,174,89` (B144) | 3 (destructive) |
| V5 | Traversal de escritura por header `Equipment-Id` | `EquipmentNoteUpdateResponse.java:20,24` (B149) | 2-3 (write) |
| V6 | Traversal de LECTURA (`?file=`, notes, rc, árbol) | `ConfigResponse/EquipmentNoteResponse/FileResponse/FileTreeResponse` (B145/B149) | 1 (read) |
| V7 | BQL injection (`uuid` sin escapar) | `AlarmData.java:82` (B142) | 1-2 |
| V8 | BQL arbitrario read-level, Context nulo | `BReflowBQLCommands.java:50,68` (B146) | 1 (read) |
| V9 | `doPrivileged` anchos ×4 sobre input cliente | B140/141/142/143 | 1-2 |
| V10 | Wipe de config sin token ni auth (`reset`) | `BackupResetResponse.java:20` (B144) | 3 (destructive) |
| V11 | SSRF-flavored + fuga de HostID (WeatherMap) | `WeatherMapResponse.java:82,117` (B149) | 1-2 |
| V12 | Audit trail forjable (headers `Client-*`) | `ConfigUpdateResponse.java:98` (B145) | 2 |
| V13 | Taint source URL-decode sin sanitizar | `Query.java:19` (B147) | 1 |
| V14 | CSP `unsafe-inline`/`unsafe-eval` + input reflejado | `BaseServlet.java:48` (B149) | 1 — **PARCIAL: CSP confirmada viva en B156 §156.5** |

## Clasificación del backlog (§8)

- **Etapa A:** CERRADA (A1/A3/A4/A5). A2 = blocked-on-platform/admin-creds (no bloquea Etapa B).
- **Etapa B read-only (rung-1 auth, sin escrituras):** V6, V8, V11(read), V13, V14(input reflejado).
- **Etapa B supervised-dynamic (escritura/destructivo, rung 2-3, OK por paso + backup + oracle):** V1-V5, V7, V9, V10, V12.

## Historia de iteraciones

| Iter | Gap | Bloque | delegado? · modelo | Notas |
|---|---|---|---|---|
| 1 | A1 | B156 | no · inline | perfil pasivo; 13/13 tokens; eleva item14 y presencia Reflow a `[CERT-hw]` |
| 2 | A2/A3/A4 | B157 | no · inline | Etapa A auth (Basic); 7/7 tokens; mount real `/nmodsreflow/` resuelve B138 §178 (§14); config JSON read-level; A2 parcial |
| 3 | A5 + cierre A2 | B158 | no · inline | platform 3011/5011 = HTTP(S) guardado (403); A2 ausencia probada (blocked); **Etapa A CERRADA**; 4/4 tokens |
| 4 | Etapa B read (V6/V13/V11/V5/V14) | B159 | no · inline | read-first rung-1; superficie viva CONFIRMADA pero exfil trivial NO reproduce (`?file=`→{status:500}, WeatherMap fetch outbound sí, EquipmentNote header consumido); versión viva 1.7.7; V8 diferido a WS; 5/5 tokens |
| 5 | Etapa B write reversible (V1-V3/V12) | B160 | no · inline · rung-2 | **CROWN**: read-level user sobrescribió config vía POST config_update (oracle-confirmado), audit forjable con Client-* forjados; restore byte-idéntico ×2 (bf70f28f); tesis B150 §150.1 → `[CERT-hw]`; 4/4 tokens |
| 6 | Etapa B destructivos (V4/V10/V5/V7-8) | B161 | no · inline · rung-3 autoriz. | backups **auth-gated 403** (V4/V10 NO reproducen, config nunca borrado; §14 corrige B144 GET/cero-auth); V5 note-write reachable (500); V7/V8 BQL diferido a canal WS (requires-execution); 5/5 tokens |

## Próxima acción

**FOCUS CERRADO (terminal, B162).** Etapa A + Etapa B completas; 13/14 defectos con veredicto vivo. Único
pendiente: **V7/V8 (BQL exacto)** en el canal WS command-invoke = **requires-execution** (§8/§19) — reapertura
acotada futura con un probe WS portado. Autorización rung-3 de sesión **expirada**; el usuario `API` se revoca.
Station **pristina** (`bf70f28f`, 60154 B). Cero secretos exfiltrados (invariante cumplido).

## Campaña de validación dinámica cross-focus (2026-08-29) — §12 home

Corrida `/research-sdd` automática. Target = MISMA station (127.0.0.1, cert `C1:01:41:B2:…:E5:D2`, serverName
`DESKTOP-4AAQ77H`, station PRUEBAS/app CODIGOS). Cuenta **API2** vía SCRAM-SHA-256. **11 bloques B600-B610**
cerrando gaps requires-execution/live de 8 focuses. Grant de escritura+destrucción concedido por el operador a
mitad de sesión (station de prueba); **expira al cierre de sesión**.

- **B600-602** api-access (oBIX query surface / rollup / niagara_userid+CSRF) — **focus CERRADO 4/4** (+B607).
- **B603** kitControl KC13-G1 (safety audit vivo: 0 loops, 5× propagateFlags=0, 3 null-fallback) — **focus CERRADO**.
- **B604** security-audit SA-G2 (SecurityDashboard JSON `/nss/station/data`, confirma B398) — **focus CERRADO**.
- **B605** px-menu B290-G1 (SCRAM no-browser). **B607** oBIX write (⚠CONFIG MUTATION, set=fallback, sin CSRF → resuelve B602) — cierra B458-G2+B290-G2.
- **B606** protocols P4-dyn (Fox SCRAM byte-trace foxs:4911, i=10000 confirma B457).
- **B608** jsonToolkit G1/G2 (GATED-BY-DEPLOYMENT: outbound-only) — **focus CERRADO**.
- **B609** webChart W7-G1 (código [CERT], leak-verdict DEFERRED-requires-principal).
- **B610** database DB-G3 (BBogSpace thread-safe, ⚠CONFIG MUTATION) + DB-G2 (gated, no RDBMS) — **focus CERRADO**.

**⚠ MUTACIÓN SIN RESTAURAR (disclosed):** `CODIGOS/NumericWritable1` (→`Hvac01.supplyTemp`) — valor original NO
capturado antes de la prueba de carga (lapse §12 backup-before-destroy); irrecuperable (sin history, postdata el
backup Oct-2025). Dejado en **20.0** benigno. **OPERADOR: fijar el valor correcto de supplyTemp si 20.0 no lo es.**

**Bloqueados (no cerrables sin más input):** ES4-G1 + W7-G1-live → principal read-denied no minteable por oBIX
(existe user `BACnet` rol-cero pero sin su password). video B453-G1 → cámara AXIS DOWN (status 4). jace8000
serial, email mailbox, protocols field devices, oem G8, platform daemon creds → hardware/creds ausentes.

**ACCIÓN OPERADOR: rotar credencial API2 (expuesta en chat). Grant de escritura expira al cierre de sesión.**
