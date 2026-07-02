# Block 154 — nmodsreflow.77 (`-ux`): wiring cliente↔backend (mapa de endpoints REST + comandos WS de la SPA)

> Research de **NiagaraMods Reflow v1.7.7 (build .75), la capa de comunicación de la SPA**: qué endpoints REST
> llama (axios) y qué comandos manda por el WebSocket, es decir la **cara cliente** del contrato que B149
> documentó del lado servidor y del canal WS de B140. Cubre el mapa endpoint→método→uso y el set de comandos
> WS. NO cubre la construcción de seguridad de cada param (U5) ni el detalle redirect/config (U6/U7).
>
> Focus: **nmodsreflow-ux** (capa cliente `-ux`). Cierra el gap **U4**. Corpus language: Spanish (technical EN).
>
> Sources (primarias):
> - `file:line` estructural: **beautify** de `app.4509efb4.js` (sha256 `81b82b83…`, B153) →
>   `scratchpad/app.beauty.js` (READ-ONLY, temp; 1:1 con el bundle minificado original).
>
> Método: grep dirigido + lectura de ventanas del beautified-temp. Markers: `[CERT]` (`app.beauty.js:NNN`) ·
> `[INFER]` deducción.
>
> Capa 26 (OEM tercero NiagaraMods). Connects [Block 149] (contrato servidor de cada `*Response`), [Block 140]
> (canal WS), [Block 143] (comandos WS sync/favorites/config-control), [Block 144] (backups GET-shaped),
> [Block 145] (config POST + headers `Client-*`), [Block 153] (identidad SPA).

---

## 154.1 — Mapa de endpoints REST (axios) `[CERT]`

Todas las llamadas son **relativas same-origin** (sin `baseURL`), al JACE que sirve la SPA `[INFER]`:

| Endpoint | Método | Uso cliente | Backend | Cita |
|---|---|---|---|---|
| `/nmodsreflow/config` | GET | leer config.json | ConfigResponse (B145) | `app.beauty.js:14408` |
| `/nmodsreflow/config_update` | **POST** | overwrite total del config | ConfigUpdateResponse (B145) | `:14157,14212` |
| `/nmodsreflow/config_delta` | **POST** | JSON-Patch del config | ConfigDeltaResponse (B145) | `:14228` |
| `/nmodsreflow/station/history-groups` | GET | árbol de grupos | HistoryGroupsResponse (B141) | `:13636` |
| `/nmodsreflow/station/histories` | GET | lista de histories | HistoryListResponse (B141) | `:13657` |
| `/nmodsreflow/station/history-data` | GET | datos de trend | HistoryDataResponse (B141) | `:13714` |
| `/nmodsreflow/station/alarms/query` | **POST** | query de alarmas | AlarmQueryResponse (B142) | `:14745` |
| `/nmodsreflow/station/alarms/csv?type=source&…` | GET | export CSV sources | AlarmCSVResponse (B142) | `:32227` |
| `/nmodsreflow/station/alarms/csv?type=alarms&…` | GET | export CSV alarmas | AlarmCSVResponse (B142) | `:34488` |
| `/nmodsreflow/station/backups` | GET | listar backups | BackupListResponse (B144) | `:3942` |
| `/nmodsreflow/station/backups/create?file=` | **GET** | crear backup | BackupCreateResponse (B144) | `:3963` |
| `/nmodsreflow/station/backups/rename?file=` | **GET** | renombrar | BackupRenameResponse (B144) | `:3984` |
| `/nmodsreflow/station/backups/apply?file=` | **GET** | restaurar (overwrite config) | BackupApplyResponse (B144) | `:4006` |
| `/nmodsreflow/station/backups/destroy?file=` | **GET** | borrar backup | BackupDestroyResponse (B144) | `:4027` |
| `/nmodsreflow/station/backups/reset` | **GET** | wipe de config | BackupResetResponse (B144) | `:4048` |
| `/nmodsreflow/station/equipment-notes` | GET | leer notas (header `Equipment-Id`) | EquipmentNoteResponse (B149) | `:47815` |
| `/nmodsreflow/station/equipment-notes-update` | **POST** | escribir notas | EquipmentNoteUpdateResponse (B149) | `:47840` |
| `/nmodsreflow/demos` | GET | demo.json | DemoResponse (B149) | `:13219` |
| `/nmodsreflow/image-library`, `/sound-library` | GET | árboles de assets | Image*/File (B149) | `:3732,15138` |
| `/nmodsreflow/point-matrix.json`, `/icon-categories.json`, `/icon-search.json` | GET | lookups de assets del módulo | FileResponse (`rc/`, B149) | `:4925,61779,61781` |

`[INFER]` **Confirmación cliente de B144:** las 5 operaciones mutantes/destructivas de backups
(create/rename/apply/destroy/reset) las llama la SPA con **GET** y `?file=` `[CERT]` `:3963-4048` — exactamente
la mutación destructiva GET-shaped (CSRF-friendly) que B144 §144.3 marcó del lado servidor. El cliente pasa el
nombre en `file=` sin más (el traversal de B144 es alcanzable con lo que la propia SPA arma). `config_update`/
`config_delta`/`alarms/query`/`equipment-notes-update` sí son POST `[CERT]`.

## 154.2 — Comandos WebSocket sobre el canal `reflow` `[CERT]`

El cliente abre `/nmodsreflow/ws` (B153 §153.6) y sobre el canal `reflow` manda comandos con envelope
ticket-based `[CERT]`:

| Comando (cliente→server) | Uso | Backend | Cita |
|---|---|---|---|
| `ping` | keep-alive | — | `app.beauty.js:4180` |
| `join` | unirse al canal | acceptor (B140) | `:4260` |
| `favorites-read` | leer favoritos del usuario | ReflowOrdTreeFavoritesRead (B143) | `:13176` |
| `favorites-write` | escribir favoritos | ReflowOrdTreeFavoritesWrite (B143) | `:13199` |
| `sync-delta` | aplicar JSON-Patch al config compartido | ConfigSyncCommand (B143) | `:14203` |
| `config-control` | protocolo single-writer lock | RequestControlCommand (B143) | `:14339,14358,14376,14487` |
| `config-route` / `route` | navegación / ruteo | — | `:118307,56983` |

`[INFER]` Confirma B143 desde el cliente: `favorites-read/write`, `sync-delta` y `config-control` son
exactamente los comandos que B143 documentó del lado servidor. Mensajes server→client (tipados): `client-info`
(asigna el `clientId`, `:4237`), `config-reload`/`config-refresh` (`:14466`), y una familia de forms/lists de
UI (`navigationItemList`, `historyList`, `alarmConsoleList`, `licenseComponent`, `*Form`) `[CERT]` `:59044-113912`.

## 154.3 — Headers de request `[CERT]`

El `config_update` (y las demás mutaciones) mandan 3 headers `[CERT]` `app.beauty.js:14158-14161`:

- `Client-Id` = `socketInfo.clientId` — asignado por el server vía `client-info` (B153, no forjable).
- `Client-Username` = `user.username` — **estado Vuex mutable del cliente** (B153/B145, forjable).
- `Client-Migration` = flag de migración de config entre versiones `[CERT]` `:14161` `[INFER]` (nuevo; indica
  al backend que el config viene de una versión previa y requiere migración de shape).

## 154.4 — Connections

- **[Block 149]** — cada endpoint de §154.1 mapea a su `*Response`; este bloque es la cara cliente de ese
  contrato (métodos + params confirmados desde la SPA).
- **[Block 140]/[Block 143]** — los comandos WS de §154.2 son los `IReflowCommand` del canal `reflow` de B140;
  `favorites`/`sync-delta`/`config-control` confirman B143.
- **[Block 144]** — confirma que backups create/rename/apply/destroy/reset son GET con `?file=` desde el
  cliente (la mutación destructiva GET-shaped / CSRF-friendly).
- **[Block 145]** — `config_update`/`config_delta` POST con headers `Client-*`; `Client-Username` mutable.
- **[Block 153]** — identidad de la SPA que emite estas llamadas.

`[INFER]` U4 cierra el mapa de comunicación. Quedan U5 (postura de seguridad cliente: construcción de los
params `file`/`query`/`config`, ya con el `Client-Username` mutable y el token Mapbox de B153), U6
(redirect/hyperlink, mayormente en B152 §152.4-152.5), U7 (config cliente). Varios se solapan con lo ya
documentado — se cerrarán con ese criterio.
