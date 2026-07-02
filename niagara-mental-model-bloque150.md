# Block 150 — Síntesis cross-focus de seguridad: nmodsreflow × platform-security (bloque TERMINAL del focus)

> **Bloque de consolidación** (no de decompilado nuevo): teje la superficie de ataque agregada del backend
> `-rt` de NiagaraMods Reflow v1.7.7.75, tejiendo los hallazgos YA CITADOS en B138-B149 con el multiplicador
> de plataforma de B75/B113/B139. Cada afirmación re-cita el `file:line` de su bloque fuente (verificados en
> origen; este bloque no re-verifica por grep salvo cross-refs nuevas). READ-ONLY.
>
> Focus: **nmodsreflow** (arquitectura backend `-rt`) — bloque TERMINAL, cierra el focus. Corpus language:
> Spanish (technical EN).
>
> Fuente: los bloques B138-B149 de este focus (JAR embarcado build .75, decompile Vineflower) + B75/B113/B139.
> Markers: `[CERT]` re-cita de `file:line` ya verificado en el bloque fuente · `[INFER]` análisis de amenaza
> agregado / recomendación defensiva.
>
> Capa 26 (OEM tercero NiagaraMods). Consolida [Block 138]-[Block 149] + [Block 75]/[Block 113]/[Block 139].

---

## 150.1 — Modelo de amenaza: dónde falla la autorización `[CERT]`

Reflow expone su backend por dos superficies: el **canal WebSocket** (B140) y el **REST HTTP** (B138/B149).
La autorización se distribuye así:

- **Command agents (WS + invoke):** los 8 `BReflow*Commands` gatean uniformemente a `requiredPermissions = "r"`
  (read-level) vía `@AgentOn` `[CERT]` (B146 §146.1: BQL:29, File:23, User:18, License:27, Nav:22, CSV:26,
  History:21, Alarm:24). No hay tier `rw`/admin en ningún comando.
- **REST (`BaseServlet`):** el router es un ladder `if/else` sobre `getPathInfo()` **sin gate de auth alguno**
  — grep de permisos sobre todo el servlet = 0 `[CERT]` (B149 §149.1, `BaseServlet.java`). El REST no tiene
  ni el `"r"` de los comandos.

**El bypass estructural** `[CERT]` (B146 §146.5): el gate `"r"` cabalga la registración `@AgentOn` del
*agente*, no el *dato*. El trabajo real lo hacen statics sin check (`AlarmData.*`, `HistoryData.*`,
`BackupManager.*`, `ConfigIO.*`), y los Response REST los invocan **directo**. `[INFER]` Por lo tanto la
autorización efectiva de toda la superficie mutante es **la sesión autenticada, nada más** — cualquier usuario
que abra el WS o llegue al servlet alcanza operaciones que deberían exigir escritura/admin.

## 150.2 — Cadena de hallazgos consolidada `[CERT]`

Todos re-citan su bloque fuente (file:line ya verificado allí):

| # | Clase de defecto | Ubicación | Bloque |
|---|---|---|---|
| 1 | **Config-write sin auth, vía WS** (`sync-delta` aplica JSON-Patch del cliente bajo doPrivileged) | `BReflowSyncService.java:339,420` | B143 |
| 2 | **Config-write sin auth, vía REST overwrite total** (body → `config.json`, sólo valida Content-Length) | `ConfigUpdateResponse.java:51,64,69` | B145 |
| 3 | **Config-write sin auth, vía REST JSON-Patch** (2ª puerta a `applyConfig`) | `ConfigDeltaResponse.java:40` | B145 |
| 4 | **Traversal de escritura destructivo** (delete/apply/rename por sanitización asimétrica) | `BackupManager.java:64,174,89` | B144 |
| 5 | **Traversal de escritura por header** (`Equipment-Id` → `makeFile`) | `EquipmentNoteUpdateResponse.java:20,24` | B149 |
| 6 | **Traversal de lectura** (`?file=` override del ORD; header notes; module rc; árbol completo) | `ConfigResponse.java:28-37` · `EquipmentNoteResponse.java:20-24` · `FileResponse.java:49` · `FileTreeResponse.java:31` | B145/B149 |
| 7 | **BQL injection** (`uuid` sin escapar en cláusula BQL) | `AlarmData.java:82` | B142 |
| 8 | **BQL arbitrario read-level con Context nulo** (`ord.get(null)`, sin filtrar por ACL) | `BReflowBQLCommands.java:50,68` | B146 |
| 9 | **`doPrivileged` anchos ×4** sobre input del cliente (dispatch WS + history + alarms + sync) | `B140` · `HistoryData.java:69` · `AlarmData.java:122` · `BReflowSyncService.java:339` | B140/B141/B142/B143 |
| 10 | **Wipe de config sin token ni auth** (`reset` borra `config.json`) | `BackupResetResponse.java:20` | B144 |
| 11 | **SSRF-flavored + fuga de HostID** (`config` → URL upstream + `getHostId()`) | `WeatherMapResponse.java:82,117` | B149 |
| 12 | **Audit trail forjable** (author de headers `Client-*` spoofeables) | `ConfigUpdateResponse.java:98` · `ConfigDeltaResponse.java:40` | B145 |
| 13 | **Taint source URL-decode sin sanitizar** (agrava el traversal/injection: `%2F`→`/`, `%27`→`'`) | `Query.java:19` | B147 |
| 14 | **CSP `unsafe-inline`/`unsafe-eval` + input reflejado** en errores | `BaseServlet.java:48` | B149 |

`[INFER]` **El punto de cierre end-to-end (B147 §147.4):** entre la fuente (query string / header crudo) y
cada sink (FilePath/BOrd/BQL) el único transform es `URLDecoder.decode` — que **ayuda** al atacante. Nada
aguas arriba sanitiza. Por eso los items 4-8 y 11 son explotables de punta a punta, no teóricos.

## 150.3 — El multiplicador de plataforma `[CERT]`

Todo el privilegio de §150.2 (los `doPrivileged` que corren BQL/config-write del cliente con privilegio
elevado) descansa en el supuesto de que el JAR OEM está **firmado y validado** por la plataforma Niagara. Ese
supuesto es desactivable:

- **Validación de módulo apagable:** B75/B113 documentaron que la validación de módulo/code-signing puede
  desactivarse vía **`skipModuleValidation`** `[CERT]` (B75/B113). `[INFER]` Con eso, un JAR modificado (o no
  firmado) corre igual, y sus bloques privilegiados también.
- **Licensing con bypass:** B139 documentó un **bypass en el licensing RSA** de Reflow `[CERT]` (B139). `[INFER]`
  el gating por tipo de station / features no es una barrera dura.
- **El HostID cierra el círculo:** el `getHostId()` que `WeatherMapResponse` **fuga off-box** `[CERT]`
  (`WeatherMapResponse.java:117`, B149) es el **mismo** identificador que ancla el binding del licensing
  `[CERT]` (B139). `[INFER]` la exfil del HostID alimenta directamente el análisis del bypass de licencia.

`[INFER]` **Superficie agregada:** config mutable sin auth por 3 vías + traversal destructivo lectura/escritura
+ BQL arbitrario/injection + 4 bloques privilegiados anchos + audit forjable + SSRF/HostID leak, **sobre** una
plataforma donde la validación de módulo puede apagarse y el licensing tiene bypass. La composición es
materialmente peor que cualquier hallazgo aislado.

## 150.4 — Postura defensiva (recomendaciones READ-ONLY) `[INFER]`

No implementadas (research read-only); ordenadas por impacto/esfuerzo:

1. **Gatear las Response/commands mutantes a `rw`/admin, no `"r"`** — hoy config-write, backup-destroy y
   note-write son alcanzables a read-level (§150.1). El fix natural es un check de permiso en el **dato**
   (los statics), no sólo en el `@AgentOn`, para que el REST no lo bypasse.
2. **Sanitizar el path en el taint source** — normalizar/rechazar `..` y separadores DESPUÉS del URL-decode en
   `Query.method_363` (o en cada sink), cerrando items 4-6 (B147).
3. **Escapar/parametrizar el `uuid` en la BQL** (`AlarmData.java:82`) y evitar BQL por concatenación en general
   (items 7-8).
4. **Angostar el `doPrivileged`** a la mínima llamada API que lo necesite, y correr el BQL con el Context del
   caller (no `null`), para que el ACL del usuario filtre (item 8-9).
5. **No fugar el HostID** en `WeatherMap` y validar/whitelistear el param `config` de la URL upstream (item 11).
6. **Token anti-CSRF + método correcto** en las mutaciones (backup reset/apply/destroy son GET-shaped; config
   author viene de headers) — items 4, 10, 12.
7. **CSP estricta** (quitar `unsafe-inline`/`unsafe-eval`, acotar `connect-src`) y escapar el `path` reflejado
   en errores (item 14).

## 150.5 — Connections (bloques consolidados)

- **[Block 138]** — módulo/service/espina HTTP-WebSocket (la base de toda la superficie).
- **[Block 139]** — licensing RSA + bypass + HostID binding (multiplicador de plataforma).
- **[Block 140]** — canal WebSocket + dispatch bajo `doPrivileged` (item 9).
- **[Block 141]** — history (doPrivileged ancho, item 9).
- **[Block 142]** — alarms (BQL injection uuid, item 7).
- **[Block 143]** — sync (config-write JSON-Patch sin auth, items 1, 9).
- **[Block 144]** — backups (traversal de escritura destructivo + wipe, items 4, 10).
- **[Block 145]** — config REST (overwrite + delta + `?file=` read + audit forjable, items 2, 3, 6, 12).
- **[Block 146]** — command agents (gate `"r"` mal escalado + bypass REST + BQL arbitrario, items 1, 8).
- **[Block 147]** — taint source (URL-decode sin sanitizar, item 13; cierre end-to-end).
- **[Block 148]** — util (corrobora "sin sanitización en util/").
- **[Block 149]** — contrato de datos + router sin auth-gate + sinks EquipmentNote/WeatherMap (items 5, 6, 11, 14).
- **[Block 75]/[Block 113]** — `skipModuleValidation` (multiplicador de plataforma).

`[INFER]` **Cierre del focus:** la superficie backend `-rt` de nmodsreflow está completamente mapeada
(subsistemas: service, WS, licensing, history, alarms, sync, backups, config, commands, taint source, util,
contrato de datos). El hilo de seguridad está cerrado end-to-end y consolidado aquí. Este es el bloque
terminal del focus.
