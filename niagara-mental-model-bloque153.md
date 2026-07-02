# Block 153 — nmodsreflow.77 (`-ux`): la SPA embarcada (identidad/build, framework Vue 2.6.14, contrato de globals, router hash)

> Research de **NiagaraMods Reflow v1.7.7 (build .75), la SPA Vue embarcada** que el `-rt` sirve desde `rc/` y
> que los loaders `-ux` (B152) montan en un iframe. Cubre la identidad/build del bundle, el framework real, el
> contrato de globals `injectBaja`/`injectConfig`/`destroyApp` (que B152 dejó pendiente de verificar), el
> router, y —surfaced acá— dos hallazgos de seguridad que confirman B145. NO agota el mapa de endpoints (U4) ni
> el análisis de seguridad cliente completo (U5), que este barrido dejó mayormente pre-respondidos.
>
> Focus: **nmodsreflow-ux** (capa cliente `-ux`). Cierra el gap **U3**. Corpus language: Spanish (technical EN).
>
> Sources (primarias):
> - Bundle ORIGINAL (identidad, ground-truth en vivo): `SPA/js/app.4509efb4.js` (sha256
>   `81b82b83…`, 2 631 974 B) + `SPA/js/chunk-vendors.3fecdb47.js` (sha256 `b82c3527…`, 2 795 017 B), en
>   `/home/cristian/modules/Prototipos/modulos/organized/nmodsreflow77/nmodsreflow77-rt/vineflower/rc/`.
> - `file:line` estructural: **beautify del bundle original** (js-beautify vía `npx`, READ-ONLY, a temp en
>   scratchpad): `app.beauty.js` (123 740 líneas) / `vendors.beauty.js` (113 561 líneas). Los `file:line`
>   citados son de esos beautified-temp, que son 1:1 con el bundle minificado original.
>
> Método: beautify + grep/lectura targeteada del temp; identidad re-medida en vivo (sha256/bytes). Markers:
> `[CERT]` fuente primaria (`app.beauty.js:NNN`/`vendors.beauty.js:NNN`, o sha256 en vivo) · `[INFER]` deducción.
>
> Capa 26 (OEM tercero NiagaraMods). Connects [Block 152] (verifica el contrato de globals que dejó abierto),
> [Block 149] (`FileResponse` sirve este bundle desde `rc/`), [Block 145] (confirma el `Client-Username`
> forjable desde el lado cliente), [Block 140] (el cliente WS), [Block 50]/[Block 51] (**corrige** el framework
> a Vue 2.6.14 y da el diff .77 vs v1.7.5).

---

## 153.1 — Identidad y build `[CERT]`

Ground-truth re-medido en vivo (sha256): `app.4509efb4.js` = `81b82b83…` (2 631 974 B),
`chunk-vendors.3fecdb47.js` = `b82c3527…` (2 795 017 B) `[CERT]` (sha256sum en vivo). Build stamp embebido en
la SPA `[CERT]` `app.beauty.js:121898-121903`: `{ mode:"production", rc:"RC5", number:"75", version:"1.7.7" }`
— confirma **v1.7.7.75 RC5**, consistente con el `vendorVersion 1.7.7.75` de `module.xml` (B151).

## 153.2 — Framework real (CORRIGE B50) `[CERT]`

| Componente | Versión | Cita |
|---|---|---|
| **Vue** | **2.6.14** (la instanciada como app) | `vendors.beauty.js:7394` (`Si.version = "2.6.14"`) |
| Vue (2ª copia) | 2.6.10 — pertenece a vue-devtools embebido, NO a la app | `vendors.beauty.js:29470` |
| vue-router | 3.4.5 | `vendors.beauty.js:37147` (`xt.version = "3.4.5"`) |
| Store | Vuex (`createStore`), no Pinia | `app.beauty.js:13952` |
| HTTP | axios (`p.a.get`/`p.a.post`) | (§153.5) |

`[INFER]` **Refinamiento §14 sobre B50:** B50 §"Stack confirmado" documentó **Vue 2.7.16 / vue-router 3.6.5**,
pero eso era el **dev-tree v1.7.5** (`package.json` de `Reflow-Clean-177`). El bundle **EMBARCADO del build
.77** corre **Vue 2.6.14 / vue-router 3.4.5** (verificado como la copia instanciada vía `new o["default"]({...})`
en `app.beauty.js:121839,121906`; la copia 2.6.10 es de vue-devtools, no la app). No es una refutación —es
divergencia dev-vs-shipped / downgrade 1.7.5→1.7.7. B50 quedó anotado con la nota §14.

## 153.3 — El contrato de globals (VERIFICA B152) `[CERT]`

B152 dejó como contrato que la SPA debía exponer `injectBaja`/`injectConfig`/`destroyApp`. Confirmado `[CERT]`
`app.beauty.js:121783,121864,121928`:

- **`window.injectBaja`** `[CERT]` `:121783` — `baja!`-requiere `service:nmodsreflow:ReflowService`, llama
  `ReflowUserCommands.getRoles` (B146), y monta la app **operador** (`render`) en `#nmods-app` `[CERT]` `:121848`.
- **`window.injectConfig`** `[CERT]` `:121864` — monta la app **config** en `#nmods-config` `[CERT]` `:121912`,
  y en ese mount commitea a Vuex: `user/SET_USERNAME (e.getUserName())`, `user/SET_ROLES`, `SET_IS_CONFIG`,
  `SET_IS_MULTI_USER (r.getMultiUserConfig())`, `SET_SOCKET_TIMEOUT (r.getSocketTimeout())` `[CERT]` `:121912`.
- **`window.destroyApp`** = `window.vueApp.$destroy()` `[CERT]` `:121928`.

`[INFER]` Confirma el modelo de B152: el host bajaux (Workbench) inyecta el puente y la SPA monta el Vue app en
el iframe, tomando username/roles/flags del `ReflowService` vía el objeto Baja inyectado — **no re-autentica**.
(Color: un flag `SET_VILLAIN_MODE` se activa con `a1("headless")` `[CERT]` `:121912` — modo debug/headless.)

## 153.4 — Router hash `[CERT]`

El router no declara `mode` explícito `[CERT]` `app.beauty.js:55148` → default **hash mode**, confirmado por el
fallback hardcodeado `window.location.href = "/nmodsreflow/#".concat(a)` `[CERT]` `:121825`. `[INFER]` coincide
exactamente con el `src='/nmodsreflow/#'+$reflowPath` que arma `loader.js` (B152 §152.2). Rutas top-level
`[CERT]` `app.beauty.js:55155-55400`: `/`, `/alarms`, `/schedules`, `/equipment`, `/buildings`, `/histories`,
`/floors`, `/pages/:id`, `/embed/:ord(.*)`.

## 153.5 — Wiring backend y seguridad (surfaced; detalle en U4/U5) `[CERT]`

Todas las llamadas REST son relativas (same-origin al JACE, sin `baseURL`): `/nmodsreflow/{demos,
station/history-*, config, config_update, config_delta, station/alarms/query, station/backups[/reset],
station/equipment-notes[-update], ws}` `[INFER]` (endpoints grep-confirmados; mapa completo → U4).

**Confirmación del `Client-Username` forjable (cara cliente de B145) `[CERT]`:** los headers se setean
por-llamada (sin interceptor global) `app.beauty.js:14159-14160,14214-14215,87134-87135`:
- `Client-Id` = `socketInfo.clientId` — **asignado por el server** vía el mensaje WS `client-info` `[CERT]` `:4237`.
- `Client-Username` = `store.state.user.username` — **estado Vuex mutable del cliente**, seteado en el mount
  desde `getUserName()` `[CERT]` `:121912`, pero enviado como header que el server **no cross-checkea** (B145
  §145.3). `[INFER]` confirma desde ambos lados que el atributo de autoría/audit es forjable en el HTTP.

**Secreto hardcodeado `[CERT]`:** token Mapbox público (cuenta "gbodigital") embebido en cada station
desplegada `[CERT]` `app.beauty.js:118864` (`pk.eyJ1IjoiZ2JvZGlnaXRhbCI…`). `[INFER]` es publishable-class pero
real y facturable; el `apiKey` del módulo weather default `""` (config de usuario, no leak). Sin otros secretos
en ninguno de los dos bundles `[CERT]` (grep negativo `AIza`/`Bearer`/`sk_live`/`password`).

## 153.6 — Cliente WebSocket (cross-ref B140/B143) `[CERT]`

Abre `new WebSocket("wss://"|"ws://" + location.host + "/nmodsreflow/ws")` `[CERT]` `app.beauty.js:4087`, con
envelope ticket-based. El único canal joineado es **`"reflow"`** `[CERT]` `:4213,4216`; `[INFER]` no hay canal
`"reflow-config"` separado del lado cliente — `"sync-delta"` es un **comando** enviado dentro del canal
`reflow` `[CERT]` `:14202-14206` (coherente con B143: `sync-delta` es un `IReflowCommand`, no un canal).

## 153.7 — Connections

- **[Block 152]** — verifica el contrato de globals `injectBaja`/`injectConfig`/`destroyApp` y el hash router
  (`/nmodsreflow/#`) que B152 dejó pendientes; todo confirmado.
- **[Block 149]** — `FileResponse` sirve este bundle desde `rc/` (mismo `module://nmodsreflow/rc`); los
  endpoints relativos consumen el contrato de datos de B149.
- **[Block 145]** — confirma desde el cliente que `Client-Username` es estado Vuex mutable → audit forjable.
- **[Block 140]/[Block 143]** — el cliente WS abre `/nmodsreflow/ws`, canal `reflow`, `sync-delta` como comando.
- **[Block 50]/[Block 51]** — **corregidos §14**: el framework es Vue 2.6.14 (no 2.7); este bloque es el bundle
  `.77` con identidad re-medida (vs el `app-readable.js` v1.7.5 de B51).

`[INFER]` U3 cierra la identidad de la SPA. El barrido dejó U4 (mapa completo de endpoints/wiring) y U5
(construcción exacta de params `file`/`query`/`config` + seguridad cliente) **mayormente pre-respondidos** —
las próximas iteraciones los finalizan sobre el mismo beautified-temp.
