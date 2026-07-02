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

- **Métrica:** 1 / 15 gaps cerrados (0.07). Etapa A arrancada; Etapa B (14 verificaciones) pendiente.
- **Bloques del focus:** B156 (perfil pasivo).

## Ground-truth vivo (§12 — re-medido, NO heredado)

Ver [Block 156] §156.7. Ancla: cert SHA-256 `C1:01:41:B2:…:E5:D2`, `hostAddress=192.168.100.100`,
`app.name=Station`, Reflow activo (unsplash en CSP). Re-confirmar antes de cada interacción de Etapa B.

## Backlog

### Etapa A — mapear el runtime vivo (read-only)

| Gap | Descripción | Estado | Clasificación |
|---|---|---|---|
| A1 | Perfil pasivo: puertos, cert, fox hello, postura HTTP/TLS | **CERRADO** (B156) | read-only ✓ |
| A2 | Versión exacta Niagara + Reflow (gated tras login §156.4) — medir en vivo | abierto | requiere test-user (auth) |
| A3 | Árbol de componentes / módulos instalados de la station (autenticado) | abierto | requiere test-user (auth) |
| A4 | Endpoints REST/WS vivos de Reflow (B138-B149) presentes y respondiendo | abierto | requiere test-user (auth) |
| A5 | Platform daemon 3011/5011 — qué expone (identidad, no login) | abierto | read-only (probe platform) |

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

- **read-only-investigable ahora (sin auth):** A5 (platform probe). [A1 cerrado]
- **investigable con test-user (auth, read):** A2, A3, A4, V6, V8, V11(read), V13.
- **supervised-dynamic (escritura, rung 2-3):** V1-V5, V7, V9, V10, V12 — requieren OK por paso + oracle.
- **parcial:** V14 (CSP ya confirmada viva; falta el input reflejado en error).

## Historia de iteraciones

| Iter | Gap | Bloque | delegado? · modelo | Notas |
|---|---|---|---|---|
| 1 | A1 | B156 | no · inline | perfil pasivo; 13/13 tokens; eleva item14 y presencia Reflow a `[CERT-hw]` |

## Próxima acción

Checkpoint SUPERVISADO. La continuación de Etapa A (A2/A3/A4) y toda Etapa B requieren **autenticar con el
usuario de prueba `API`** — transición a interacción autenticada. Confirmar con el usuario antes de loguear
(SECRETS DISCIPLINE + rung ≥1 autenticado). A5 (platform 3011/5011) es read-only y puede correr sin auth.
