# RESEARCH-STATE — Focus `live-station`

> Focus dinámico (METHODOLOGY §12): validación de la station Niagara N4 **VIVA** en `127.0.0.1` (WSL mirrored).
> Sensibilidad **`live-install` → SECRETS DISCIPLINE** (cita estructura, nunca valores secretos).
> Numeración global de bloques (`niagara-mental-model-bloqueN.md`), compartida con el resto del corpus.
> Engram: `research/niagara/live-station/{progress,gaps}` (proyecto `niagara-research`).

## Modo de ejecución

**SUPERVISADO, no `/loop` ciego** (§12). Etapa A = probes read-only (rung 1). Etapa B = verificación
autenticada/con escritura → escalera de invasividad, oracle cross-protocolo, backup-before-destroy, OK del
usuario por paso. El usuario de prueba (`API`) se revoca al terminar.

## Cobertura

- **Métrica:** 4 / 19 gaps cerrados (0.21). **Etapa A CERRADA** (A1/A3/A4/A5 cerrados; A2 blocked-on-creds). Etapa B (14) pendiente.
- **Bloques del focus:** B156 (perfil pasivo), B157 (Etapa A auth: web + mount Reflow), B158 (platform + cierre Etapa A).

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

## Próxima acción

Autenticación resuelta: **HTTPBasicScheme** con el usuario `API` (Basic directo, sin SCRAM). Etapa A casi
completa: falta **A5** (platform 3011/5011, read-only, cierra A2/versión). Luego **Etapa B terminal**: los 14
defectos sobre los paths reales `/nmodsreflow/*`. Los items de LECTURA (V6, V8, V11-read, V13) son rung 1
autenticado; los de ESCRITURA/destructivos (V1-V5, V7, V9, V10, V12) son rung 2-3 → **checkpoint supervisado
por paso, con backup+oracle** antes de cada escritura (§12). Credencial `API` se revoca al terminar.
