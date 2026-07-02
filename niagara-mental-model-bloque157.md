# Block 157 — Etapa A autenticada: superficie web viva y mount real del servlet Reflow

> **Bloque de reconocimiento dinámico autenticado** (METHODOLOGY §12, rung 1 autenticado = SÓLO lecturas):
> primera sesión autenticada contra la station VIVA usando el usuario de prueba `API` (esquema
> **HTTPBasicScheme**, revocable). Mapea qué superficie web está realmente montada y **dónde vive el servlet
> Reflow en esta station** — resolviendo una pregunta que [Block 138] dejó abierta. Ninguna escritura.
>
> Focus: **live-station** — Etapa A (mapear el runtime), gaps A2/A3/A4. Corpus language: Spanish (technical EN).
>
> **Sensibilidad `live-install` → SECRETS DISCIPLINE.** La credencial del usuario `API` se usó SÓLO en
> tránsito (config efímero de curl en scratchpad, `mode 600`, nunca commiteado); jamás se escribió en un
> bloque, `sources/` ni engram. Se citan **códigos HTTP y content-types** (estructura), NUNCA cuerpos de
> config/datos que pudieran contener secretos. El endpoint config se probó pero su JSON **no se leyó**.
>
> Fuente (`[CERT-hw]` preservada): `sources/probes/bash-20260702T190529Z.txt` (barrido autenticado sanitizado).
> Se cruza con [Block 138] §178, [Block 149] §149.1 (decompilado `-rt` build .75).
>
> Markers: `[CERT-hw]` = medido en vivo contra la station · `[CERT]` = re-cita `file:line` de bloque previo ·
> `[INFER]` = deducción. Capa 27 (runtime vivo). Continúa [Block 156].

---

## 157.1 — Autenticación viva: HTTPBasicScheme confirmado `[CERT-hw]`

El usuario `API` está configurado con **HTTPBasicScheme** (dato del operador, confirmado por comportamiento).
Prueba definitiva de que Basic autentica — el `Location` de `/ord/` cambia con la credencial
(`bash-20260702T190529Z.txt`):

| Petición | `/ord/` → Location | Lectura |
|---|---|---|
| sin auth | `https://127.0.0.1/login` | anónimo → redirige al login form |
| con Basic `API:…` | `https://127.0.0.1/ord/station:%7Cslot:/` | autenticado → nav de la station (`station:|slot:/`) |

`[CERT-hw]` La station **no anuncia** Basic con `WWW-Authenticate` (el esquema anónimo por defecto es el form
SCRAM de `auth.min.js`), pero un usuario con HTTPBasicScheme autentica presentando la cabecera Basic de forma
proactiva. `[INFER]` Es exactamente el patrón "usuario de API": Basic directo, sin el handshake SCRAM-SHA-256
del JS de login (que sí aplica a los usuarios interactivos).

## 157.2 — Superficie web montada vs no montada `[CERT-hw]`

Con sesión autenticada, la superficie diagnóstica clásica de Niagara **no está montada** en esta station
(`bash-20260702T190529Z.txt`):

| Endpoint | Código (auth) | Estado |
|---|---|---|
| `/ord/station:|slot:/` | **200** | nav de la station — vivo |
| `/ord/module:` | **200** | espacio de módulos — vivo |
| `/spy`, `/spy/versions` | 404 | diagnóstico spy **no montado** |
| `/about`, `/nav`, `/doc/version.txt` | 404 | no montados |
| `/wb` | **403** | existe pero prohibido para este usuario |
| `/hx` | 404 | Hx (UI legacy) no montado |

`[INFER]` La superficie web está **recortada**: los endpoints de introspección (`/spy`, `/about`) que
normalmente delatan versión y módulos están ausentes, y `/wb` responde `403` (existe, pero el usuario `API`
no tiene permiso). Buena postura de minimización — y explica por qué A2 (versión exacta) no se filtra por web
(§157.5). El acceso ORD (`/ord/…`) sí funciona: es la vía autenticada de navegación.

## 157.3 — El servlet Reflow vive en `/nmodsreflow/`, no `/reflow/` — resuelve [Block 138] §178 `[CERT-hw]`

[Block 138] §178 dejó ABIERTO que el mount `/reflow/...` **no aparecía** en el módulo `-rt` decompilado (grep
vacío) `[CERT]`. La station viva **resuelve** el hueco (`bash-20260702T190529Z.txt`):

| Path | Código · content-type | Lectura |
|---|---|---|
| `/reflow`, `/reflow/config`, `/reflow/file` | **404** | confirma B138 §178: NO monta en `/reflow/` |
| `/nmodsreflow` | **200** text/html | **base real del servlet** |
| `/nmodsreflow/config` | **200 `application/json`** | Response de config (B149) — viva |
| `/nmodsreflow/file` | **200** text/html | FileResponse (B149) — viva |
| `/nmodsreflow/reflow` | **200** | subpath del ladder `getPathInfo` |
| `/nmodsreflow/rc` | **200** | espacio de recursos del módulo (SPA "Reflow") |

`[CERT-hw]` El servlet base es **`/nmodsreflow/`**; los subpaths del ladder `req.getPathInfo()` que [Block 149]
§149.1 documentó (`config`, `file`, …) cuelgan de ahí, no de `/reflow/`. Esto **eleva a `[CERT-hw]`** la
existencia viva del router `BaseServlet` de B149 y su superficie de datos. Es una **clarificación de scope**
(§14), no una refutación: B138 ya había señalado que `/reflow/` no era el mount; el valor vivo lo confirma y
nombra el correcto.

## 157.4 — El config endpoint responde JSON a nivel-lectura `[CERT-hw]`

`/nmodsreflow/config` devuelve **`200 application/json`** a un usuario **read-level** con HTTPBasicScheme
`[CERT-hw]`. `[INFER]` Esto es el preludio empírico de la tesis de [Block 150] §150.1 (el REST no gatea auth de
escritura y el gate `"r"` de los comandos no cubre el dato): la superficie config de Reflow está viva y
responde a un usuario mínimo. **No se leyó el cuerpo** (SECRETS DISCIPLINE) — sólo se cita que responde. La
verificación de si además ESCRIBE sin auth (items 1-3 de B150) es Etapa B, rung 2-3 (supervisada).

## 157.5 — A2 (versión exacta): no disclosed por web montada — pendiente `[CERT-hw]`/gap honesto

La versión Niagara **no se pudo re-medir por HTTP**: `/spy/versions`, `/doc/version.txt`, `/about` → 404
(§157.2) `[CERT-hw]`. El contexto de proyecto la infiere como **N4.14.0.162**, pero §12 prohíbe heredarla:
queda **NO re-medida en vivo**. `[INFER]` La vía viva restante es el **platform daemon** (3011/5011, gap A5),
que reporta versión/build en su handshake — se medirá ahí. A2 queda parcial: web-gated confirmado, versión
pendiente de A5.

## 157.6 — Connections

- **[Block 138]** §178 — pregunta abierta del mount `/reflow/` (grep vacío en `-rt`): **RESUELTA** aquí
  (§157.3) → mount real `/nmodsreflow/` `[CERT-hw]`. Clarificación de scope §14.
- **[Block 149]** §149.1 — router `BaseServlet` + ladder `getPathInfo` + subpaths (config/file): sus paths
  quedan **vivos y alcanzables** bajo `/nmodsreflow/` (§157.3-4), elevando su existencia a `[CERT-hw]`.
- **[Block 150]** §150.1 — la tesis "REST sin gate de auth" gana su primer indicio vivo: config responde a
  read-level (§157.4). Los 14 items se verifican en Etapa B sobre estos paths reales.
- **[Block 156]** — continúa el perfil: aquella CSP delató Reflow; aquí se localiza su servlet vivo.
- **Focus `live-station`** — cierra A4 (mount Reflow), avanza A3 (superficie montada) y deja A2 parcial
  (pendiente A5/platform).
