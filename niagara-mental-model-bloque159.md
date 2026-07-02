# Block 159 — Etapa B (1/n): verificación viva de los defectos de LECTURA de [Block 150]

> **Bloque de validación dinámica** (METHODOLOGY §12, rung-1 autenticado = solo GET, cero mutación): verifica
> contra la station VIVA los items de LECTURA de la tabla de 14 defectos de [Block 150] §150.2 — V6 (traversal
> read `?file=`), V13 (taint URL-decode), V11 (WeatherMap SSRF/HostID), V5-read (EquipmentNote header), V14
> (input reflejado). Usuario de prueba `API` (HTTPBasicScheme), paths reales `/nmodsreflow/*` (B157). Autoriz.
> rung-3 de sesión concedida por el operador; este bloque no ejerce escritura (read-first).
>
> Focus: **live-station** — Etapa B terminal. Corpus language: Spanish (technical EN).
>
> **`live-install` → SECRETS DISCIPLINE.** Se citan códigos HTTP, longitudes y **envelopes de error**; el body
> del config (60 KB) NUNCA se capturó a corpus (solo a un backup en scratchpad, citado por hash bf70f28f…). El
> valor del HostID NO se capturó. Payloads de traversal fueron benignos (nunca `/etc/passwd` ni keystores).
>
> Fuente (`[CERT-hw]`): `sources/probes/bash-20260702T194602Z.txt` + `bash-20260702T194752Z.txt`.
> Cruza [Block 150] §150.2, [Block 145]/[Block 147]/[Block 149] (código decompilado 1.7.7 `-rt`).
>
> Markers: `[CERT-hw]` medido en vivo · `[CERT]` re-cita `file:line` decompilado · `[INFER]` deducción. Capa 27.

---

## 159.1 — Versión viva del módulo — cierra A2 (lado Reflow) `[CERT-hw]`

El SPA embebido delata su build en un comentario HTML servido a un usuario autenticado
(`bash-20260702T194752Z.txt`): **`Build version: 1.7.7 - Thursday, August 14th, 2025`** `[CERT-hw]`. El campo
de datos del config (backup propio) trae `version=14`, `reflowVersion=1.7.5-43` `[CERT-hw]` — éste último es la
versión que **guardó** el config (metadato de datos), NO el módulo corriendo. `[INFER]` El módulo vivo es de la
línea **1.7.7** (Ago 2025), coherente con el corpus decompilado (1.7.7 build .75, B138-B155): las
verificaciones de Etapa B aplican al mismo tren de versión — las divergencias son de comportamiento vivo, no de
build distinto.

## 159.2 — V6 + V13: traversal de LECTURA `?file=` — **NO reproducido en vivo** `[CERT-hw]`

[Block 145] §145.2 documentó (código 1.7.7 `[CERT]` `ConfigResponse.java:28,37`) que el query `?file=`
override-a el ORD y hace `findFile(new FilePath(location))` → lectura arbitraria. En vivo el comportamiento
diverge (`bash-20260702T194752Z.txt`):

| Petición | Código · len | Body |
|---|---|---|
| `/nmodsreflow/config` (baseline) | 200 · 60154 | config.json completo |
| `?file=config.json` | 200 · 17 | **`{ "status": 500 }`** |
| `?file=..%2f..%2f..%2fconfig.json` (V13 url-decoded) | 200 · 17 | **`{ "status": 500 }`** |
| `?path=` / `?ord=` / `?f=` (otros nombres) | 200 · 60154 | ignora el param → config default |

`[CERT-hw]` `file` **es** el parámetro que dispara la rama de lectura (los otros nombres se ignoran y devuelven
el config default), pero **cualquier valor de `?file=` produce `{status:500}`**, no el contenido del archivo.
`[INFER]` La superficie existe (el param se consume) pero el traversal trivial de exfiltración **no reproduce**:
el sink 500-ea con mis payloads benignos (probablemente espera un ORD/formato exacto, no un path crudo). V13:
el URL-decode ocurre (`%2e%2e%2f` se acepta) pero la lectura igual 500-ea. **Veredicto: superficie confirmada,
explotación trivial NO reproducida** (§14 clarificación de comportamiento vivo, NO refuta el code-path de B145).

## 159.3 — V11: WeatherMap SSRF-flavored — **mecanismo de fetch outbound CONFIRMADO**, fuga de HostID no observada `[CERT-hw]`

[Block 149] §149 (`[CERT]` `WeatherMapResponse.java:24,82,117`) documentó que `config` del cliente se concatena
en `http://weather.niagaramodules.com/maps + config + "?host=" + getHostId()`. En vivo
(`bash-20260702T194752Z.txt`): `/nmodsreflow/weather-map?config=/TEST` → **`500: Error fetching image from
provider`** `[CERT-hw]`. `[INFER]` El endpoint **sí realiza un fetch outbound incorporando el input del
cliente** (el error es del *provider fetch*, no de parseo) → el núcleo SSRF-flavored (petición saliente con
input controlado) **está vivo**. El fetch falló (sin salida a internet / provider inalcanzable en este entorno),
por lo que la **fuga de HostID no se pudo observar** (el error precede al retorno). **Veredicto: mecanismo
outbound CONFIRMADO; fuga de HostID no observable sin un fetch exitoso** (requeriría upstream alcanzable).

## 159.4 — V5-read: EquipmentNote traversal por header — **mecanismo CONFIRMADO** `[CERT-hw]`

[Block 149] §149 (`[CERT]` `EquipmentNoteResponse.java:20-24`): el header `Equipment-Id` compone
`NOTE_FOLDER + fileName + ".json"` → `findFile(new FilePath(location))`. En vivo: sin header → 200; con
`Equipment-Id: ../../x` → **`500: Internal Error`** que además refleja `/station/equipment-notes`
(`bash-20260702T194752Z.txt`) `[CERT-hw]`. `[INFER]` El header **se consume y alimenta la resolución de path**
(200→500 con el payload de traversal) → la superficie del sink está viva; el path traversado no resolvió a un
archivo legible (500) con mi payload benigno. **Veredicto: mecanismo CONFIRMADO (header alcanza el file-resolve);
lectura exitosa no lograda con payload benigno.**

## 159.5 — V14: input reflejado en errores — **PARCIAL** `[CERT-hw]`

Dos señales (`bash-20260702T194602Z.txt`/`…4752Z`): (a) el 500 de equipment-notes **refleja** el path
`/station/equipment-notes` en el body `[CERT-hw]` → hay reflexión de input en algunas ramas de error; (b) un
subpath aleatorio (`/nmodsreflow/ZZQ7391XREFLECT`) devuelve **200 con el SPA** (no error) y **no** refleja el
marcador `[CERT-hw]`. El componente CSP `unsafe-inline`/`unsafe-eval` del item 14 ya quedó confirmado vivo en
[Block 156] §156.5. **Veredicto: reflexión PARCIAL (presente en errores estructurados, ausente en subpaths
desconocidos); CSP inseg. ya confirmada.**

## 159.6 — V8 (BQL arbitrario): **DIFERIDO al canal de comandos** `[INFER]`

[Block 146] documentó `BReflowBQLCommands` como **command agent** (`@AgentOn`, gate `"r"`), invocable por el
canal WS/command-invoke, no por un GET REST plano. No es alcanzable con los GET de este bloque → se difiere a
una iteración de canal WS de Etapa B. **No verificado aquí** (honesto, no cerrado).

## 159.7 — Síntesis read-side y Connections

`[INFER]` **Patrón vivo:** los endpoints de lectura de [Block 150] **existen y consumen el input tainteado**
(confirmando la superficie de ataque que el corpus mapeó por decompilado), pero con payloads benignos naive
**devuelven 500 en vez de exfiltrar** archivos/HostID. Es la distinción clave entre `[CERT]` "el code-path
existe" y `[CERT-hw]` "es trivialmente explotable en vivo": superficie CONFIRMADA, exfiltración trivial NO
reproducida (V6/V13), mecanismos outbound/header CONFIRMADOS (V11/V5). Ninguna refuta el corpus (§14: refinan
comportamiento vivo del mismo build 1.7.7).

- **[Block 150]** §150.2 — items 6, 13, 11, 5, 14: verificados vivos (veredictos arriba). Items de escritura
  (1-5, 10, 12) y BQL (7-8) pendientes de las próximas iteraciones de Etapa B.
- **[Block 145]/[Block 147]/[Block 149]** — sus code-paths (`?file=`, URL-decode, WeatherMap, EquipmentNote)
  quedan con superficie viva confirmada pero exfiltración no trivial en 1.7.7.
- **[Block 156]** — la versión viva (§159.1) refina el perfil; CSP del item 14 ya confirmada allí.
- **Focus `live-station`** — Etapa B arrancada (read-first); próximo: rung-2 escrituras reversibles (V1-V3, V12)
  con backup+oracle+restore, bajo la autorización de sesión concedida.
