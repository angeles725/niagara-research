# Block 145 — nmodsreflow.77 (`-rt`): superficie REST de config (read con `?file=` traversal, overwrite total, delta = 2ª puerta a applyConfig)

> Research de **NiagaraMods Reflow v1.7.7 (build .75), endpoints REST de config del runtime `-rt`**: cómo
> Reflow lee, reemplaza y parchea (JSON-Patch RFC6902) el `config.json` compartido por HTTP. Cubre las 3
> Response `ConfigResponse` (GET/read), `ConfigUpdateResponse` (POST/overwrite total) y `ConfigDeltaResponse`
> (POST/JSON-Patch). NO cubre `sync/ConfigIO` (persistencia, ya en B143) ni el `applyConfig` interno (B143) —
> los referencia. Es la contraparte REST del canal WS `sync-delta` de B143.
>
> Focus: **nmodsreflow** (arquitectura backend `-rt`). Cierra el gap **R9**. Corpus language: Spanish
> (technical EN).
>
> Sources (primarias, JAR embarcado build .75, decompile Vineflower):
> `RT/` = `/home/cristian/modules/Prototipos/modulos/organized/nmodsreflow77/nmodsreflow77-rt/vineflower/com/niagaramods/nmodsreflow`
> `RESP/` = `RT/http/responses`.
>
> Método: decompile Vineflower del JAR embarcado + lectura directa + grep de tokens. Markers:
> `[CERT]` fuente primaria local (`file:line`) · `[INFER]` deducción anclada a líneas `[CERT]`.
> Nota de decompilado: `method_363`=parse de query en `http/util/Query` (R13, taint source); `method_311`=`put`;
> se citan tal cual.
>
> Capa 26 (OEM tercero NiagaraMods). Connects [Block 138] (el service central tiene el `config` en memoria y
> el router de servlets), [Block 140] (los broadcasts `config-reload` van por el canal WS), [Block 143] (el
> `applyConfig` que `ConfigDeltaResponse` invoca es el mismo path privilegiado del `sync-delta`; `ConfigIO`
> persiste), [Block 144] (misma ausencia de gating + la misma clase de traversal, esta vez en lectura),
> [Block 75]/[Block 113] (skipModuleValidation), [Block 139] (licensing bypass).

---

## 145.1 — Las 3 caras REST del `config.json` `[CERT]`

Las tres son clases planas con `serve(HttpServletRequest, HttpServletResponse)` estático y **cero**
`requiredPermissions`/`doPrivileged`/permission-check (grep negativo sobre las 3) `[CERT]`.

| Response | Método | Qué hace | Persistencia | Cita |
|---|---|---|---|---|
| `ConfigResponse` | GET | lee y streamea `config.json` (o `?file=` arbitrario) | — | `ConfigResponse.java:26,37` |
| `ConfigUpdateResponse` | POST | **overwrite total** del `config.json` desde el body | síncrona directa | `ConfigUpdateResponse.java:51,69` |
| `ConfigDeltaResponse` | POST | delega el JSON-Patch a `sync.applyConfig` (B143) | vía applyConfig | `ConfigDeltaResponse.java:40` |

## 145.2 — ConfigResponse: `?file=` = lectura arbitraria de la station `[CERT]`

Por defecto lee `CONFIG_ORD = "^reflow/config.json"` `[CERT]` `ConfigResponse.java:21,25`, pero un **param de
query `file` OVERRIDE-a el ORD** (incluso quita un prefijo `file:`) y luego `findFile(new FilePath(location))`
`[CERT]` `ConfigResponse.java:26-30,37`:

```
String location = CONFIG_ORD;                                   // :25
if (req.getQueryString() != null ...) {                         // :26 (GET)
   Map query = Query.method_363(req.getQueryString());
   if (query.get("file") != null) { location = query.get("file");   // :28-29
      if (location.startsWith("file:")) { ... }                // :30
```

`[INFER]` Cualquier caller autenticado puede leer **archivos arbitrarios de la station**, no sólo el config —
misma clase de traversal que B144, esta vez de **lectura**. Además el config se streamea byte-a-byte sin
filtro/redacción `[CERT]` `:91-104`: `[INFER]` cualquier `token`/`password`/`apiKey`/`license` presente en
`config.json` se sirve en claro (exposición estructural por passthrough del archivo completo).

## 145.3 — ConfigUpdateResponse: overwrite total sin autorización `[CERT]`

Lee el body (POST) `[CERT]` `ConfigUpdateResponse.java:51`, lo streamea a un temp bajo `^reflow/cache/temp/`,
y la **única validación** es una igualdad de `Content-Length` `[CERT]` `:64-66`; luego copia el temp
**directo sobre** `^reflow/config.json` `[CERT]` `:33,69-70`. `[INFER]` Es un **reemplazo ciego total** — sin
schema, sin allow-list de claves. La escritura primaria es **síncrona directa** al filespace (`getOutputStream`
`:69`), no el writer con `Thread` de `ConfigIO`; sólo llama `ConfigIO.writeCache` después si el webCache está
on `[CERT]` `:88`. Broadcast `config-reload` al canal WS con `author` tomado de headers `Client-Username`/
`Client-Id` `[CERT]` `:98`, y `reloadConfigurationFile()` si multiUser `[CERT]` `:84`.

**Autorización — ausente:** ningún `requiredPermissions` ni check `[CERT]` (grep negativo). `[INFER]` Cualquier
usuario bare-authenticated reemplaza el config compartido entero por HTTP. El `author` de headers del cliente
es **spoofeable** → el audit trail es forjable `[INFER]`.

## 145.4 — ConfigDeltaResponse: segunda puerta a la ruta privilegiada de B143 `[CERT]`

No llama `JsonPatch.apply` localmente: lee el body, `mapper.readTree(in)` y **delega a
`sync.applyConfig(Client-Username, Client-Id, data)`** en `BReflowSyncService` `[CERT]`
`ConfigDeltaResponse.java:38-40`:

```
InputStream in = req.getInputStream();                                              // :38 (POST)
JsonNode data = mapper.readTree(in);                                                // :39
HashMap response = sync.applyConfig(req.getHeader("Client-Username"), req.getHeader("Client-Id"), data);  // :40
```

`[INFER]` `applyConfig` es **el mismo** que usa el comando WS `sync-delta` (B143): aplica el JSON-Patch del
cliente al config bajo `AccessController.doPrivileged` ancho, sin `requiredPermissions`. Es decir, este
endpoint HTTP es una **segunda puerta bare-authenticated** hacia ese path privilegiado. Identidad otra vez de
headers spoofeables `[CERT]` `:40`. Resultado del patch sólo por header `Config-Patched` `[CERT]` `:42`;
`setStatus(200)` en el path feliz `[CERT]` `:43`; `[INFER]` en el catch sólo hace `println` sin setear status
de error → un patch fallido no da señal 4xx/5xx clara al caller.

## 145.5 — Cross-cutting `[CERT]`

- **doPrivileged en estas Response:** ninguno (grep negativo). La elevación de privilegio vive dentro de
  `applyConfig` (B143), no acá.
- **Validación:** Update valida SÓLO `Content-Length` `[CERT]` `:64`; Delta no valida acá (pasa el node crudo
  a applyConfig).
- **Mutación GET-shaped/CSRF:** Update y Delta leen el body (POST) → no son mutaciones por query string. Pero
  el `?file=` de ConfigResponse (GET) sí es una **lectura** arbitraria por query param `[CERT]` `:28-29`.
- **Secretos:** ConfigResponse streamea el `config.json` completo sin redacción `[CERT]` `:91-104`.

## 145.6 — Connections

- **[Block 138]** — el `config` en memoria (`BReflowService.config`) y el router de servlets que mapea estos
  `serve`.
- **[Block 140]** — el broadcast `config-reload` de Update va por el canal WS de B140.
- **[Block 143]** — `ConfigDeltaResponse` invoca el `applyConfig` privilegiado de B143; `ConfigUpdateResponse`
  sobrescribe el mismo `config.json` que sync colabora, y `ConfigIO` (B143) persiste el cache.
- **[Block 144]** — misma ausencia total de gating de permisos; misma clase de traversal (B144 en
  delete/apply/rename de escritura; B145 en `?file=` de lectura). Juntos: traversal **de lectura y de
  escritura** sobre la station.
- **[Block 75]/[Block 113]** — `skipModuleValidation`. **[Block 139]** — licensing bypass.

**Nota de seguridad cross-focus (REFORZADA, R9 completa el patrón):** R9 confirma que el `config.json` se muta
**también por REST** sin autorización, y agrega un traversal de **lectura** (`?file=`) con passthrough de
secretos. Cuadro agregado (ya maduro para la síntesis NEXT-ACTION nmodsreflow × platform-security): (1)
history/alarms/sync (B141/B142/B143) corren `doPrivileged` anchos sobre input del cliente; (2) el `config.json`
es mutable sin perms por **tres** vías —`sync-delta` WS (B143), `ConfigUpdateResponse` overwrite total (B145),
`ConfigDeltaResponse` JSON-Patch (B145, 2ª puerta a la ruta privilegiada de B143); (3) traversal de escritura
destructivo en backups (B144) y de lectura arbitraria en config (B145); (4) BQL injection a read-level (B142);
(5) audit trail forjable (author de headers `Client-*` spoofeables); (6) todo descansa en la firma/validación
del módulo, que B75/B113 mostraron desactivable vía `skipModuleValidation`, con licensing RSA bypaseable
(B139). `[INFER]` La superficie agregada está completamente caracterizada: el NEXT-ACTION natural al agotar el
focus es un **bloque de síntesis cross-focus** (nmodsreflow × platform-security). El sub-gap R13
(`http/util/Query.method_363`) sigue siendo el taint source común a los params `file`/`query` de B142/B144/B145.
