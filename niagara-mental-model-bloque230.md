# Block 230 — Reflow: evolución de producto v1.7.5 → v1.7.7 (qué cambió realmente)

> **Qué documenta.** Los cambios reales entre Reflow v1.7.5 y v1.7.7 (backend `-rt`), separando cambios genuinos
> del ruido de decompilador, y qué significan para la capacidad de producto. Gap BG19 (reapertura grupo B).
>
> **Alcance.** El diff del BACKEND `-rt` (el análisis forense cubre las clases Java, no el bundle frontend). Los
> subsistemas ya documentados por fuente primaria se remiten a sus bloques.
>
> **Fuentes.** Análisis forense preservado `sources/REFLOW-175-vs-177-DIFF.md` (diff Vineflower 1.7.5 vs 1.7.7,
> fecha 2026-04-03), citado `[CERT-a]` (análisis secundario). Cross-ref a bloques del corpus que verificaron los
> hechos por fuente primaria: B139 (licensing), B141 (history), B142 (alarms), B217/B218 (schema config).
>
> **Método / markers.** `[CERT-a]` = el análisis forense preservado. `[CERT]` = confirmado por fuente primaria en
> un bloque del corpus (remite). `[INFER]` = deducción.

---

## 230.1 — Advertencia metodológica: v1.7.5 estaba "unlicensed" `[CERT-a]`

El código v1.7.5 disponible fue **modificado manualmente para deshabilitar el licenciamiento** (comentarios
`// licensing disabled - unlicensed build`, `sources/REFLOW-175-vs-177-DIFF.md` §nota-fuente). Por eso, casi todo el
"diff" en las clases de licensing (`License.java` ~249 líneas, `LicenseValidator.java` ~221, `LicenseManager.java`,
`LicenseClient.java`, `BReflowLicenseCommands.java`) es **restauración del original**, NO un cambio de v1.5→v1.7. Es
crítico no leer esas diferencias como evolución de producto.

Además, gran parte del diff bruto es **ruido de decompilador** `[CERT-a]`: renombres `method_311`/`method_312`,
`lambda → clase anónima`, `var5 → var7`. El análisis los marca "NO - Solo ruido decompilador"
(`BReflowChannelService`, `BReflowWebSocketAcceptor`, `BReflowSyncService`, `ConfigIO`, **`BackupManager` ~51 líneas
= ruido**, `Query`, `IReflowCommand`, etc.).

## 230.2 — Los cambios REALES v1.7.5 → v1.7.7 `[CERT-a]`/`[CERT]`

| Área | Cambio real | Impacto | Verificado en |
|---|---|---|---|
| **History** | Threading con `PrivilegedAction` (`FromComponentTask`) — la query corre en thread privilegiado separado | Evita timeouts/bloqueo del servlet | B141 `[CERT]` |
| **History** | **`HistoryGhostSubscriber` (clase NUEVA)** — "touch subscription" que despierta historias remotas | **Bug fix**: historias cross-station ya no devuelven vacío | §230.3, B141 |
| **History** | `HistoryIO` cache de grupos en archivo GZIP | Rendimiento | B141 `[CERT]` |
| **Alarms** | `AlarmQuery` movido de **GET (query string) a POST (JSON body)** | **Breaking change**: el frontend debe mandar POST | §230.4, B142 |
| **Alarms** | Campo `ackTime` en `AlarmData`; método `canAcknowledgeAlarms` nuevo | Info de ack | B142 `[CERT]` |
| **Routing** | Ruta `/demo/.*` → redirige a `index.html` | Deep-linking en demos (SPA routing) | B149 (BaseServlet) |
| **Routing** | `pathFound=true` tras servir history-data | Bug fix: evita doble-serve como FileResponse | `[CERT-a]` |
| Licensing | (restauración, no cambio — §230.1) | — | B139 |
| Backups/Sync/WS | Solo ruido decompilador | Sin cambio funcional | `[CERT-a]` |

## 230.3 — El cambio más impactante: `HistoryGhostSubscriber` `[CERT-a]`

Clase nueva `history/HistoryGhostSubscriber.java`: un subscriber efímero que se suscribe a un `BHistory` y se
**auto-desuscribe al primer evento** (`event()` → `history.unsubscribe(this)`). Es el patrón Niagara "touch
subscription": suscribir+desuscribir fuerza al sistema de historia a inicializar su estado. Se usa en
`HistoryData.subscribeToHistory()` para "despertar" historias que NO son de la station local antes de leerlas —
resuelve un bug donde historias remotas (vía NiagaraNetwork) devolvían datos vacíos si nunca habían sido suscritas.
El análisis lo marca "el cambio más impactante de la versión" `[CERT-a]`.

## 230.4 — El único breaking change: `AlarmQuery` GET → POST `[CERT-a]`

v1.7.5 servía `/station/alarms/query` en `doGet()` con parámetros en query-string; v1.7.7 lo movió a `doPost()` con
body JSON (`AlarmQueryResponse` lee `req.getReader()` → parse JSON). Razón probable: los query-strings tienen límite
de longitud y los filtros de alarmas complejos necesitan JSON. **Requiere que el frontend UX se actualice
simultáneamente** para mandar POST. Es el único cambio que rompe compatibilidad de API. Coherente con B142/B149
(que documentaron el POST en el build .77) `[CERT]`.

## 230.5 — Qué NO cambió: el modelo de dashboard / la capacidad builder `[INFER]`

El diff es del BACKEND `-rt`. Notablemente, los cambios reales son de **history/alarms/routing**, NO del modelo de
dashboard ni del motor de sync/config: `BReflowSyncService` y `ConfigIO` figuran como "solo ruido decompilador". Es
decir, **la capacidad de producto (el modelo `config.json`, el JSON-Patch, el catálogo de widgets, los floorplans)
es ESTABLE entre 1.5 y 1.7** — el schema `config.json` es `version:14` en ambas (confirmado live en 1.7.5 B217 §217.8
y en el corpus static 1.7.7 B218). Las 3 clases UX (`BReflow`/`BReflowConfig`/`BReflowRedirect`) tienen **0 cambios**
`[CERT-a]`. La evolución 1.5→1.7 fue de robustez (history cross-station, threading) y una migración de API (alarm
POST), no de nuevas capacidades de builder. (El bundle frontend no está en este diff; B153 notó por separado que el
dev-tree 1.7.5 declaraba Vue 2.7.16 vs el shipped 2.6.14.)

## 230.6 — Conexiones

- **[Block 141]/[Block 142]** — verificaron por fuente primaria (build .77) el threading de history, el ghost
  subscriber y el POST de alarms; §230 los ubica como la EVOLUCIÓN desde 1.5.
- **[Block 139]** — el licensing cuyo diff es restauración, no cambio (§230.1).
- **[Block 217]/[Block 218]** — el schema `config.json v14` estable que prueba que el builder no cambió.
- **Hacia BG16/BG18** (licensing-producto, migración): la migración de config (`Client-Migration`) es el mecanismo
  que maneja cambios de schema ENTRE versiones — relevante dado que el schema fue estable en este salto.
