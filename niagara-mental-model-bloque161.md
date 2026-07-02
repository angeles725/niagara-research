# Block 161 — Etapa B (3/n): los destructivos — backups está AUTH-GATED en vivo (V4/V10 no reproducen)

> **Bloque de validación dinámica** (METHODOLOGY §12): verifica en la station VIVA los defectos DESTRUCTIVOS
> de [Block 150] — V4 (traversal destructivo en backups), V10 (wipe de config), V5-write (EquipmentNote), y la
> alcanzabilidad de V7/V8 (BQL). Bajo autorización rung-3 de sesión (operador presente). **Config nunca fue
> borrado** — el defecto más severo resultó auth-gated en vivo.
>
> Focus: **live-station** — Etapa B terminal. Corpus language: Spanish (technical EN).
>
> **`live-install` → SECRETS DISCIPLINE.** Solo códigos HTTP y flags; cero bodies. Los destructivos se probaron
> con el gate (403) como resultado — ninguno detonó. `config_update` de control fue no-op (POST del backup
> pristino). Config verificado pristino tras la matriz (`bf70f28f…`, 60154 B).
>
> Fuente (`[CERT-hw]`): `sources/probes/bash-20260702T200147Z.txt` + `bash-20260702T200405Z.txt`.
> Cruza [Block 150] §150.2, [Block 144] (backups, código .75), [Block 142] (alarms/BQL).
>
> Markers: `[CERT-hw]` medido en vivo · `[CERT]` re-cita `file:line` · `[INFER]` deducción. Capa 27.

---

## 161.1 — Matriz de autorización viva (usuario read-level `API`, perm `"r"`) `[CERT-hw]`

POST a cada superficie mutante (`bash-20260702T200405Z.txt`):

| Superficie mutante | Método vivo | Código (read-level) | Gate |
|---|---|---|---|
| `config_update` (overwrite total) | POST | **200** | **NO** (defecto vivo, B160) |
| `equipment-notes-update` (note write) | POST | **500** | **NO** (procesa, V5) |
| `backups/create` | POST | **403** | **SÍ** |
| `backups/destroy` | POST | **403** | **SÍ** |
| `backups/reset` (wipe de config = V10) | POST | **403** | **SÍ** |
| `backups/reset` GET-shaped (método viejo B144) | GET | **405** | método no permitido |

`[CERT-hw]` **Asimetría clave:** en el build vivo 1.7.7, la mutación de **config y notes NO está gateada**
(read-level escribe), pero **todo el subsistema de backups SÍ (403)**. Esto **refina la tesis uniforme** de
[Block 150] §150.1 ("toda la superficie mutante = sesión autenticada, nada más"): en esta station el gate NO es
uniforme — los backups exigen más permiso del que tiene `API`.

## 161.2 — V4 (traversal destructivo en backups): **NO reproducido — GATED** `[CERT-hw]` (§14 vs [Block 144])

[Block 144] documentó (`[CERT]`, build .75) que las ops de backups son **GET-shaped** y con **cero
autorización**, con `destroy`/`apply`/`rename` sin sanitizar (traversal de DELETE/READ/MOVE arbitrario). En vivo
(`bash-20260702T200147Z.txt`/`…405`):

- El método real es **POST**, no GET: GET → `405 Method Not Allowed` (Allow: POST) `[CERT-hw]`. `[INFER]` El
  vector CSRF-por-GET que B144 señaló está **mitigado** en 1.7.7 (ya no acepta GET).
- POST `create`/`destroy` → **403** para el usuario read-level `[CERT-hw]`. El traversal destructivo **no es
  alcanzable** por `API`: el gate corta antes del sink sin sanitizar.

`[INFER]` **§14 clarificación de scope (NO refuta el código):** el code-path sin sanitización de B144 sigue
existiendo en el decompilado; lo que cambia es que el build/despliegue 1.7.7 **antepone un gate de autorización**
que el análisis estático .75 no reflejaba. El defecto de traversal es real en el código pero **no explotable a
read-level** en esta station. Verdict: **NO reproducido (gated)**.

## 161.3 — V10 (wipe de `config.json`): **NO reproducido — GATED**, config intacto `[CERT-hw]`

`backups/reset` (el wipe, `BackupResetResponse`, B144) → POST **403** para `API` `[CERT-hw]`. El oracle GET
confirmó el config **sin tocar** (`bf70f28f…`, 60154 B) antes y después `[CERT-hw]`. `[INFER]` El defecto más
severo del corpus (borrar config sin token ni auth) **no es alcanzable** por el usuario read-level en vivo — el
mismo gate del subsistema backups (§161.2) lo protege. Verdict: **NO reproducido (gated); config nunca borrado.**

## 161.4 — V5-write (EquipmentNote): **superficie de escritura alcanzable** `[CERT-hw]`

POST `equipment-notes-update` con `Equipment-Id: ../../x` → **500** (no 403) `[CERT-hw]`. `[INFER]` A diferencia
de backups, la escritura de notas **NO está gateada**: el 500 indica que el sink (`makeFile` sobre el path del
header, B149) se **ejecutó** y erró con el traversal benigno (path no resoluble), no que se rechazó por auth.
Verdict: **mecanismo de escritura CONFIRMADO alcanzable a read-level** (consistente con config_update, §161.1);
escritura exitosa a un path controlado no forzada (payload benigno).

## 161.5 — V7/V8 (BQL): motor de query alcanzable; inyección precisa en canal WS — **DIFERIDO** `[CERT-hw]`/`[INFER]`

`AlarmQueryResponse` POST `/station/alarms/query` → **500** con body vacío/`{uuid:"x'"}` (procesa, no 403), y el
export CSV GET → **200** `[CERT-hw]`: el motor de alarmas/BQL es **alcanzable a read-level**. Pero la inyección
BQL precisa de [Block 142] §142.1 (`getAlarmByUuid`: `"…uuid = '" + uuid + "'"` con comilla cruda) y el BQL
arbitrario de [Block 146] (`BReflowBQLCommands`) viven en el **canal WS de comandos** (`@AgentOn`,
command-invoke), no en el POST REST. `[INFER]` Verificar la inyección exige portar el protocolo WS
command-invoke de Niagara (Fox/box sobre wss:4911/443) → clasificado **requires-execution** (§8/§19), diferido a
una iteración de canal WS. Verdict: **superficie de query CONFIRMADA a read-level; inyección BQL exacta diferida
al canal WS.**

## 161.6 — Tesis viva refinada y Connections

`[INFER]` **Mapa de explotabilidad vivo (usuario read-level, station 1.7.7):**
- **Explotable/confirmado:** config-write ×3 (V1-V3), audit-forge (V12), note-write reachable (V5), CSP inseg.
  (V14), WeatherMap outbound (V11).
- **Superficie existe, exfil trivial no reproduce:** read-traversal `?file=` (V6/V13).
- **NO reproducible (auth-gated 403):** backups traversal destructivo (V4) y wipe de config (V10).
- **Diferido (canal WS):** BQL injection/arbitrario (V7/V8).

La lección `[CERT-hw]` central: el corpus mapeó los code-paths correctamente (`[CERT]`), pero la station viva
impone un **gate de autorización NO uniforme** que el análisis estático no podía ver — backups protegido,
config abierto. Esto es exactamente lo que §12 existe para capturar.

- **[Block 150]** §150.1 — tesis "toda la superficie mutante a read-level": **refinada** — cierto para
  config/notes, FALSO para backups (gated) en 1.7.7.
- **[Block 144]** — ops de backups: §14 clarificación viva (método POST no GET; auth-gated, no "cero auth").
- **[Block 142]/[Block 146]** — BQL: motor alcanzable; inyección precisa diferida a WS.
- **[Block 160]** — contraste directo: config_update 200 vs backups 403 (misma sesión, mismo usuario).
- **Focus `live-station`** — 13/14 defectos con veredicto vivo; solo V7/V8 (WS) diferido. Próximo: síntesis
  terminal de Etapa B + retro §18.
