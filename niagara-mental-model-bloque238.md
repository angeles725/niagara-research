# Block 238 — Reflow: usuarios, themes, favoritos y el modelo de autorización (profiles)

> **Qué documenta.** La personalización por-usuario de Reflow: identidad/roles, themes/branding, favoritos, y —lo
> central— el modelo de autorización real (`profiles`). Gap BG28 (reapertura grupo E). Extiende B143 (favoritos) y
> B146 (roles).
>
> **Alcance.** El eje usuario/autorización/personalización. El editor visual es B223; el nav es B239.
>
> **Fuentes (primarias).** SPA beautificada (`BF:`). Java RT: `commands/BReflowUserCommands.java`,
> favoritos socket-commands. Config real de disco (`profiles`, estructura). Barrido delegado (sonnet); tokens
> re-verificados.
>
> **Método / markers.** `[CERT]` = fuente primaria. `[INFER]` = deducción.

---

## 238.1 — Identidad y roles: sin capabilities propias `[CERT]`

El módulo Vuex `user` (`BF:13115`): `{username, roles, isConfig, activeProfile, favorites, clipboard*}`. La identidad
se setea en el bootstrap (`SET_USERNAME(getUserName())`, `SET_ROLES($bajaUserRoles)`, `BF:121846/121912`).
`$bajaUserRoles` viene de `BReflowUserCommands.getRoles` (`BReflowUserCommands.java:28`), un **passthrough** de
`cx.getUser().getRoles()` — la asignación de roles de la STATION es la única fuente de verdad, nada Reflow-específico.

**No hay capability flags client-side** `[CERT]` (grep negativo: `canWrite`/`isAdmin`/`hasPermission` = 0 hits). El
control de acceso NO son booleanos per-acción — es **por profile** (§238.4). (Contraste con chihuahua, que tiene un
`checkCanWrite` RBAC write-gate real, B164 — Reflow gatea por VISTA/ruta, no por acción.)

## 238.2 — Themes: branding global admin + motor de color; SIN theme per-usuario `[CERT]`

- El módulo `theme` (`BF:8767`) es un set global único (admin-set): `shortName, title, subtitle, logo, logoHeight,
  background, backgroundPosition, copyright, showWelcome, showCardHoverAnimation, useStationTimezone`. Coincide con
  las keys del `config.json.theme` real (B218).
- El módulo `colors` (`BF:4700`) es una **capa de derivación computada** (con `tinycolor`): de colores base
  (`primary, secondary, menuItem…`) deriva `primaryLight/Dark/Alpha/Readable`, `mostReadable` (contraste WCAG AAA),
  `colorPickerSwatches` (armonías splitcomplement/tetrad), + `recentColors` con lock. Es el **motor del editor de
  color/branding admin** `[INFER]` (el admin elige colores base, la paleta/contraste se auto-deriva).
- **NO hay theme per-usuario** `[CERT]` (grep negativo: `darkMode`/`userTheme`/`prefers-color-scheme` = 0 hits). El
  theme es puramente global/admin — **diferencia de producto real con chihuahua** (que tiene `userThemes`, B169).

## 238.3 — Favoritos: per-usuario, por comando WS, filename server-derived `[CERT]`

El cliente (`user` module, `BF:13169`): `loadFavorites` manda `{command:"favorites-read"}` por **WebSocket RPC** (no
HTTP); `saveFavorites` commitea optimista `SET_FAVORITES` y manda `{command:"favorites-write", favorites}`, con
rollback ante error. Servidor: comandos `favorites-read`/`favorites-write` (owner `reflowSyncService`) guardan en
`^reflow/favorites/{username}.json` donde **el username se deriva del usuario autenticado de la sesión, NO de un
parámetro del cliente** (`socket.acceptCx.getUser().getUsername()`) — reconfirma B143 (sin cross-user write posible).
En disco: `favorites/admin.json` = `[]`. Formato: array plano, un archivo por username.

## 238.4 — El modelo de autorización REAL: `profiles` `[CERT]`

El "capability model" de Reflow es **profile → lista de restricción de rutas → gating de path/nav**, no booleanos
per-usuario. Schema de un profile: `{id, name, restrictions:{routes}, navigation:{useGlobal, enabled, items[]},
weather, buildings, users[], roles[], startPage, redirectHome}`. Módulo `profiles` (`BF:8972`,
`state:{lockConfig, configUsers:["admin"], items:[…]}`).

Resolución y enforcement `[CERT]`:
- `getProfileForUser(username, roles)` (`BF:9024`): match exacto de username en `profile.users[]` → match de rol en
  `profile.roles[]` → fallback `"default-profile"`.
- `authorizeLink({link, profile, username, roles})` (`BF:9050`): allow/deny de rutas contra
  `profile.restrictions.routes`, con un toggle **`restrictNewContent`** (allowlist si true, denylist si false) que se
  computa en tiempo de edición y se hornea en la lista `routes` (no se persiste como flag).
- `isPathAvailable` (`BF:9114`): gatea primero por flags module-enabled (`alarms.enabled`, `schedules.enabled`…),
  luego por toggles per-building (`building.alarmsEnabled/schedulesEnabled/historiesEnabled`), luego delega a
  `authorizeLink`.
- `authorizedNavigationItems`/`isNavItemAvailable` (`BF:9150`): un profile puede overridear el árbol de nav entero
  (`profile.navigation.items` si `useGlobal===false`) o filtrar el global per-item.

En disco: `config.json.profiles.items` tiene 4 profiles (1 default + 3 custom), cada uno con el schema completo —
feature viva, no dead code. **Conclusión**: autorización = profile resuelto de username-o-rol al login, re-evaluado
en cada render de ruta/nav-item vía `authorizeLink`/`isPathAvailable`.

## 238.5 — Conexiones

- **[Block 143]** — favoritos server-side per-usuario; §238.3 confirma el filename server-derived + el transporte WS.
- **[Block 146]** — `BReflowUserCommands.getRoles`; §238.1 confirma que es passthrough de roles de station.
- **[Block 164]/[Block 169]** (chihuahua) — chihuahua tiene RBAC write-gate REAL + `userThemes`; Reflow gatea por
  profile/ruta (no por acción) y NO tiene theme per-usuario — dos diferencias de producto.
- **[Block 239]** (nav) — `profiles` filtra el árbol de `navigation`; §239 documenta el árbol crudo.
