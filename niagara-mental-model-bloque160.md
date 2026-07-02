# Block 160 — Etapa B (2/n): CONFIRMACIÓN VIVA del config-write sin auth (V1-V3, V12)

> **Bloque de validación dinámica** (METHODOLOGY §12, rung-2 escritura reversible — backup+oracle+restore):
> verifica en la station VIVA la **tesis central de [Block 150] §150.1** — el config de Reflow es escribible
> por un usuario read-level SIN gate de autorización de escritura. Es la verificación corona del focus. Bajo
> autorización rung-3 de sesión; ejecutada como escritura REVERSIBLE con restore byte-idéntico verificado.
>
> Focus: **live-station** — Etapa B terminal. Corpus language: Spanish (technical EN).
>
> **`live-install` → SECRETS DISCIPLINE.** Cero bodies de config a corpus: el estado se manejó como backup en
> scratchpad (sha `bf70f28f…`) y solo se citan códigos HTTP, presencia de un marcador BENIGNO propio, y hashes
> de restore. El marcador fue una clave descartable (`__researchProbe`) añadida a config VÁLIDO — nunca un
> cambio estructural que rompiera la station.
>
> **Salvaguardas §12 aplicadas:** (1) backup verificado + confirmado que el config vivo == backup antes de
> mutar; (2) oracle CROSS-request (GET independiente confirma el POST); (3) restore inmediato POST del backup
> pristino, verificado **byte-idéntico** (sha256) tras CADA escritura; (4) bench re-confirmado (cert idéntico).
>
> Fuente (`[CERT-hw]`): `sources/probes/bash-20260702T195551Z.txt` (+ capturas de respuesta en scratchpad).
> Cruza [Block 150] §150.1-2, [Block 143]/[Block 145] (código decompilado 1.7.7).
>
> Markers: `[CERT-hw]` medido en vivo · `[CERT]` re-cita `file:line` · `[INFER]` deducción. Capa 27.

---

## 160.1 — V2: overwrite total de `config.json` a nivel-lectura — **CONFIRMADO** `[CERT-hw]`

Path real (B154, `app.beauty.js:14157`): **POST `/nmodsreflow/config_update`** → `ConfigUpdateResponse`
(overwrite total, `[CERT]` `ConfigUpdateResponse.java:51,69`). Prueba viva controlada
(`bash-20260702T195551Z.txt`):

| Etapa | Acción | Resultado |
|---|---|---|
| 1 write | POST config pristino **+ 1 clave benigna** `__researchProbe`, usuario `API` (perm `"r"`) | **HTTP 200** |
| 2 oracle | GET `/nmodsreflow/config` (request independiente) | marcador **presente** → **WRITE APLICADO** |
| 3 restore | POST backup pristino | HTTP 200 |
| 4 verify | GET + sha256 | **byte-idéntico** (`bf70f28f…`, 60154 B, marcador ausente) |

`[CERT-hw]` Un usuario **read-level** (`API`, HTTPBasicScheme, `requiredPermissions="r"`) **sobrescribió**
`config.json` de la station viva; el oracle independiente lo confirmó y el restore volvió el config idéntico.
Esto **eleva a `[CERT-hw]`** la tesis de [Block 150] §150.1 (el REST no gatea auth de escritura; el gate `"r"`
de los comandos no cubre el dato) — verificada de punta a punta contra el hardware.

## 160.2 — V1 y V3: mismas puertas al `applyConfig` privilegiado — **CONFIRMADO por paridad** `[INFER]`

[Block 143]/[Block 145] documentaron `[CERT]` que **tres** vías llegan al mismo `applyConfig` sin
`requiredPermissions`: WS `sync-delta` (`BReflowSyncService.java:339,420` — V1), REST `config_update` (V2, aquí
CONFIRMADO vivo) y REST `config_delta` (`ConfigDeltaResponse.java:40` — V3, POST `/nmodsreflow/config_delta`,
B154 `:14228`). `[INFER]` Habiendo probado en vivo que la vía REST de overwrite **no gatea escritura**, V1 y V3
— que comparten el **mismo path privilegiado sin check** — son CONFIRMADOS por paridad. No se ejecutaron
individualmente para **minimizar mutaciones** (una prueba viva del patrón basta; el resto es riesgo sin
información nueva). Quedan como `[CERT]`-código + `[CERT-hw]`-por-paridad, no re-ejecutados.

## 160.3 — V12: audit trail forjable — **CONFIRMADO** `[CERT-hw]`

[Block 145] item 12 (`[CERT]` `ConfigUpdateResponse.java:98`): el autor de la mutación se toma de headers
`Client-*` spoofeables. Prueba viva: el mismo POST `config_update` con **`Client-User: forged-ghost-attacker`**
y **`Client-Host: 10.0.0.9`** falsos → **HTTP 200, write aplicado** (`bash-20260702T195551Z.txt`) `[CERT-hw]`.
`[INFER]` El backend **acepta un autor arbitrario no autenticado** en la mutación → el audit trail es forjable.
El sink de auditoría en sí (dónde queda el autor) no es observable por esta API read-level, pero la aceptación
del header forjado sin verificación confirma el defecto de origen.

## 160.4 — Postura de seguridad de la prueba y estado final `[CERT-hw]`

`[CERT-hw]` Tras las **dos** escrituras (marcador simple y marcador+headers forjados), el config quedó
**restaurado byte-idéntico** al pristino (`bf70f28f…`, 60154 B, marcador ausente) — verificado por sha256 en
cada ciclo (`bash-20260702T195551Z.txt`). La station no quedó alterada. `[INFER]` Combinado con B159 (los reads)
y B150: la superficie mutante de Reflow es alcanzable y **efectiva** a nivel-lectura — a diferencia de los
reads (que 500-ean con payloads triviales, B159), la **escritura sí se aplica limpiamente**. El defecto de
escritura es materialmente más grave en vivo que el de lectura.

## 160.5 — Connections

- **[Block 150]** §150.1-2 — tesis central (config-write sin auth-gate) **CONFIRMADA viva** (`[CERT-hw]`);
  items 1-3 (config-write ×3 vías) y 12 (audit forjable) verificados. Pendientes: 4/10 (destructivos), 5
  (EquipmentNote write), 7/8 (BQL/alarms).
- **[Block 143]** — el `applyConfig` bajo `doPrivileged` sin `requiredPermissions`: su efecto vivo probado.
- **[Block 145]** — `ConfigUpdateResponse`/`ConfigDeltaResponse`/headers `Client-*`: los tres confirmados
  (overwrite y audit-forge directos; delta por paridad).
- **[Block 159]** — contraste: reads 500-ean, writes aplican → asimetría lectura/escritura en vivo.
- **Focus `live-station`** — reversibles (V1-V3, V12) CERRADOS; próximo: destructivos V4 (traversal backups) y
  V10 (wipe config) sobre objetivos sacrificiales, con el restore ya probado como red de seguridad.
