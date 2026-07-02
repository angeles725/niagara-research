# Block 155 — nmodsreflow.77 (`-ux`): postura de seguridad cliente (cara SPA de la cadena B150; por qué el traversal B144 pasa inadvertido)

> Research de **NiagaraMods Reflow v1.7.7 (build .75), la postura de seguridad del lado cliente**: cómo la SPA
> construye los params que alimentan los sinks del backend, qué "defensa" cliente existe, y cómo eso se cruza
> con la cadena de 14 defectos de B150. Es el bloque de cierre del focus `nmodsreflow-ux`: **cierra U5** y **por
> remisión U6** (redirect/hyperlink, ya en B152) **y U7** (config cliente, ya en B153/B154).
>
> Focus: **nmodsreflow-ux** (capa cliente `-ux`) — bloque de cierre. Corpus language: Spanish (technical EN).
>
> Sources (primarias):
> - `file:line` estructural: **beautify** de `app.4509efb4.js` (sha256 `81b82b83…`, B153) →
>   `scratchpad/app.beauty.js` (READ-ONLY, temp; 1:1 con el minificado original).
>
> Método: grep dirigido + lectura de ventanas del beautified-temp. Markers: `[CERT]` (`app.beauty.js:NNN`) ·
> `[INFER]` análisis de seguridad.
>
> Capa 26 (OEM tercero NiagaraMods). Connects [Block 150] (la cadena de seguridad agregada que la SPA
> alimenta), [Block 144] (traversal de backups — explica por qué pasa inadvertido), [Block 145] (config-write +
> `Client-Username`), [Block 147] (URL-decode del server que deshace el encoding cliente), [Block 152]/[Block 153]/
> [Block 154] (redirect/hyperlink, identidad, wiring — cierre por remisión de U6/U7).

---

## 155.1 — `encodeName`: el cliente sanitiza lo que el server no `[CERT]`

El param `file=` de TODAS las operaciones de backup se arma con una función cliente `encodeName(t)` `[CERT]`
`app.beauty.js:3963` (create), `:3984` (rename, `file`+`name`), `:4006` (apply), `:4027` (destroy). Su
definición `[CERT]` `app.beauty.js:3933-3935`:

```
encodeName: function(t) {
    return encodeURI(t.replace(/(<|>|:|"|\/|\\|\||\?|\*|#)/g, ""))
}
```

`[INFER]` **Hallazgo central:** ese regex `(<|>|:|"|/|\|||?|*|#)` es **idéntico** al que `BackupManager.create`
aplica del lado servidor (B144 §144.2: `replaceAll("(<|>|:|\"|\\/|\\\\|\\||\\?|\\*|#)","")`). Es decir, la SPA
quita los separadores de path (`/ \` + los demás) del nombre **para las 4 operaciones** (create/rename/apply/
destroy) antes de mandarlo. **Consecuencia:**

- **En el happy-path (UI de la SPA), el traversal de B144 NO se dispara** — porque el cliente sanitiza el
  nombre en `encodeName` para destroy/apply/rename también, aunque el server NO lo haga en esas 3 (B144).
- **Pero la sanitización es SÓLO cliente.** B144 §144.2 probó que el server no sanitiza en destroy/apply/rename.
  Un request HTTP directo (que B144/B149 mostraron alcanzable con sólo la sesión, sin `requiredPermissions`)
  puede mandar `file=../../x` **saltándose `encodeName`** y pegar el traversal. `[INFER]`

`[INFER]` **Esto explica por qué el bug de B144 pasa inadvertido y confirma que es real:** el desarrollador
SABE que el nombre necesita sanitizarse (lo hace en el cliente `encodeName` Y en el server `create`), pero lo
OLVIDÓ en el server en destroy/apply/rename. La defensa cliente enmascara el bug en uso normal; el bug latente
persiste para cualquiera que hable HTTP directo. Es la confirmación cross-layer de la asimetría de B144.

## 155.2 — El resto de "defensa" cliente es URL-encoding, que el server deshace `[CERT]`

La SPA usa `encodeURIComponent`/`encodeURI`/`escape` en varios armados de URL `[CERT]`
`app.beauty.js:3772,15057,121282` (entre otros). `[INFER]` pero eso es **URL-encoding, no sanitización**: B147
§147.1 probó que el taint source del server (`Query.method_363`) hace `URLDecoder.decode` sin sanitizar, así
que cualquier `encodeURIComponent` del cliente se **deshace** en el server antes de llegar al sink. No aporta
defensa real; sólo transporte. (El único filtro con efecto de seguridad es `encodeName` §155.1, y sólo en el
happy-path.)

## 155.3 — Los otros aportes cliente a la cadena B150 `[CERT]`

- **`Client-Username` mutable** — header desde estado Vuex `user.username` (B153/B154), enviado en config_update/
  delta `[CERT]` `app.beauty.js:14160,14234`. `[INFER]` alimenta el "audit trail forjable" (defecto #12 de B150)
  desde el cliente.
- **Token Mapbox hardcodeado** — `app.beauty.js:118864` (B153). `[INFER]` secreto embebido en cada station.
- **config_delta** manda `{delta: s}` con el JSON-Patch armado cliente `[CERT]` `app.beauty.js:14228` —
  alimenta el config-write sin auth (defectos #1/#3 de B150).
- **Manejo de error** — los catches loguean a consola y devuelven `[]` `[CERT]` `app.beauty.js:3967,4009`,
  espejando el patrón de swallow del backend (B144/B143). `[INFER]` fallas invisibles al usuario.

## 155.4 — Mapa cliente → cadena de defectos de B150 `[INFER]`

Qué defectos de B150 alimenta directamente la SPA (todos `[INFER]` sobre citas `[CERT]` ya establecidas):

| Defecto B150 | ¿La SPA lo alimenta? | Vía cliente |
|---|---|---|
| #1/#2/#3 config-write sin auth | sí | `config_update`/`config_delta` POST (B154 §154.1) |
| #4 traversal escritura backups | happy-path NO (encodeName), directo SÍ | `?file=` GET (§155.1) |
| #6 traversal lectura | parcial | `?file=`/header `Equipment-Id` (B154) |
| #10 wipe config | sí | `backups/reset` GET (B154) |
| #12 audit forjable | sí | `Client-Username` mutable (§155.3) |
| #13 URL-decode taint | n/a (server) | el encoding cliente se deshace (§155.2) |

`[INFER]` **Conclusión de seguridad cliente:** la SPA es un consumidor "confiado" del backend — no re-valida
autorización (hereda la sesión, B152/B153) y su única defensa con efecto (`encodeName`) es client-side y
bypasseable. No AGREGA defectos nuevos a B150; los **alimenta** y, en el caso del traversal de backups,
**enmascara** el bug del server en el happy-path (lo que explica por qué no se detectó antes). El veredicto de
B150 no cambia; se completa con la cara cliente.

## 155.5 — Cierre por remisión de U6 y U7 `[CERT]`

- **U6 (redirect/hyperlink)** — cubierto en **B152 §152.4-152.5**: el Proxy de `niagara.env` en `hyperlink.js`
  y el redirect browser→`/nmodsreflow`; la navegación `unescape(path)`→`location.href` está scopeada al hash
  `/nmodsreflow/#` (open-redirect de riesgo bajo). No hay sustancia nueva → **U6 cerrado por remisión** a B152.
- **U7 (config cliente)** — cubierto en **B153** (`injectConfig` + los commits Vuex `SET_IS_CONFIG`/
  `SET_IS_MULTI_USER`/`SET_SOCKET_TIMEOUT`) y **B154** (endpoints `config`/`config_update`/`config_delta` +
  comandos WS `config-control`/`sync-delta`). El contrato de config cliente es exactamente el de B143/B145 visto
  desde la SPA. No hay sustancia nueva → **U7 cerrado por remisión** a B153/B154.

## 155.6 — Connections

- **[Block 150]** — este bloque es la cara cliente de la cadena agregada; mapea qué defectos alimenta la SPA
  (§155.4) sin agregar nuevos.
- **[Block 144]** — `encodeName` (§155.1) usa el mismo regex que `BackupManager.create`; confirma la asimetría
  de B144 desde el cliente y explica por qué el traversal pasa inadvertido.
- **[Block 145]** — `Client-Username` mutable + config-write; **[Block 147]** — el URL-encoding cliente se
  deshace en el taint source del server.
- **[Block 152]/[Block 153]/[Block 154]** — cierre por remisión de U6 (redirect/hyperlink) y U7 (config cliente).

`[INFER]` **Fin del focus `nmodsreflow-ux`.** Con U5 cerrado y U6/U7 por remisión, la superficie cliente está
completamente mapeada (5 bloques B151-B155): registro de vistas → loaders/iframe → SPA (identidad, Vue 2.6.14) →
wiring REST/WS → postura de seguridad. El read-only-investigable del focus llega a 0.
