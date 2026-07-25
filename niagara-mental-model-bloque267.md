# Bloque 267 — Tags (VIII): la UI de Export Tags y el puente `BPxViewTag` hacia el subsistema PX

> **Qué documenta**: la capa Workbench de `exportTags` — `ui/` (13 clases) + `tags/px/` (3). Cierra el gap
> **T7** del focus `tags`.
>
> **Lo interesante no es la UI**: es que `tags/px/` **conecta el join de estaciones con el subsistema PX** que
> el corpus documentó en cuatro focuses (B179-B215). Ver §267.2.
>
> **Recordatorio de encuadre** ([Bloque 266] §266.1): `exportTags` **no pertenece al subsistema de tags**. Se
> documenta acá porque el gap estaba abierto; conceptualmente es Niagara Network.
>
> **Fuentes** (decompilado vineflower):
> - `$W` = `…/exportTags/exportTags-wb/vineflower/com/tridium/exporttags/`
> - Raíz: `/home/cristian/modules/Prototipos/modulos/organized/`
>
> **Método**: barrido delegado (tier `sonnet`, 65 llamadas, cubrió T7 y T8) + verificación inline: **6 tokens**
> re-verificados. Marcadores: `[CERT]` = fuente primaria; `[INFER]` = deducción. Bloque de EVIDENCIA.

---

## 267.1 — La UI: nada nuevo, todo reutilizado `[CERT]`

Las 13 clases de `ui/` no inventan mecanismos: usan los que el corpus ya documentó.

| Mecanismo (documentado en) | Clases que lo usan |
|---|---|
| **Vista Workbench por `@AgentOn`** ([Bloque 211]) | `BCategoryFilterManager` (sobre `CategoryFilterExt`), `BExportTagSummaryManager` y `BJoinProfileManager` (sobre `SubordinateExportTagNetworkExt`), `BJobNotificationHandler` |
| **`BWbFieldEditor`** ([Bloque 214], [Bloque 202]) | `BOrdVariableFE`, `BSubstituteOrdsFE`, `BStationCompSelFE` (directos); `BMultiStationNamePickerFE`, `BPropertyFiltersFE` (vía `BMultiRowFE`); `BStationNamePickerFE`, `BUserNamePickerFE` (vía `BComponentNamePickerFE`) |
| **Command de toolbar** | `BStationJoinMgrCommand` sobre `BStationMgrCommand` |
| Utilidad plana (sin tipo Niagara) | `JobTracker` — abre `BProgressDialog` y luego un `BDialog` con el log del join |

`[INFER]` Es una confirmación útil de la tesis de [Bloque 215]: la infraestructura de UI de Workbench es
uniforme, y un módulo funcionalmente ajeno al PX editor **la reutiliza tal cual**. Los field editors con
sustitución de variables `$(stationName)` / `$(currentLocation)` son lo único específico del dominio.

## 267.2 — `BPxViewTag`: el join también distribuye VISTAS PX `[CERT]`

Éste es el hallazgo del gap. `BPxViewTag extends BNiagaraExportTag` (`$W/tags/px/BPxViewTag.java:125`)
`[CERT]` es un tipo de export tag más — pero lo que provisiona no es un punto ni una history: es **una vista
PX completa**, propagada del supervisor a la subordinada.

Su `doJoin()` hace, en orden `[CERT]`:

1. **Habilita los virtuales** de la estación destino si no lo estaban (`:264`).
2. **Recorre el archivo `.px`** con un `IPxOrdVisitor` (`:272-273`) para descubrir todas las ORDs que
   referencia.
3. **Crea un `BNiagaraFileImport`** en el espacio de archivos del destino con la tabla de ORDs mapeadas
   (`:286-302`) — así el `.px` viaja como archivo.
4. **Aplica el `BCategoryMask`** a las ORDs de archivo vía `BCategoryService` (`:292-298`).
5. **Crea un `BSubstitutePxView`** en el espacio virtual con el mapa `substituteOrds` calculado (`:321-329`),
   propagando también el `requiredPermissions` configurado por el ingeniero (`:325`).

`[INFER]` **Lo que resuelve**: una vista PX escrita en el supervisor apunta a ORDs del supervisor. Para que
esa misma vista sirva en 30 subordinadas, cada ORD tiene que reapuntar a los componentes locales de cada
estación. `BSubstitutePxView` es la pieza que hace esa reescritura **en runtime**, y `BPxViewTag` la que la
provisiona automáticamente durante el join. Es distribución de gráficos a escala de flota, sin editar 30
copias del `.px`.

Los field editors del property sheet reflejan ese flujo: `pxView` usa el `PxViewSelectFE` de `wbutil`,
los directorios usan el `OrdVariableFE` con tokens, y `substituteOverrideOrds` abre una tabla de pares
ORD-origen → ORD-destino para los casos que el mapeo automático no resuelve.

`BPxViewTagValidationJob` valida las ORDs del `.px` **antes** de comprometer el join, contando fallos y
abortando si hay alguno.

`[INFER]` Este es el eslabón que faltaba entre dos mundos que el corpus documentó por separado: los cuatro
focuses PX (B179-B215) describieron cómo se **construye** una vista; esto describe cómo se **distribuye**.

## 267.3 — El acoplamiento que puede romper en un JACE `[CERT]`

`BSubstitutePxView` y `BPxViewTag` viven **en `exportTags-wb.jar`** `[CERT]` (ruta de las fuentes: todo bajo
`exportTags-wb/`). Pero `doJoin()` **persiste instancias de `BSubstitutePxView` en el espacio virtual de la
estación destino** (§267.2, paso 5).

`[INFER]` Si la estación destino no carga el jar `-wb` —y un JACE típicamente **no** lo hace, porque `-wb` es
el perfil de Workbench— no puede resolver el tipo `exportTags:SubstitutePxView` al levantar ese slot virtual.
El mecanismo asume implícitamente que el destino es una estación de clase supervisor.

**No se pudo confirmar el fallo real** (requiere una station viva sin el jar): queda como **riesgo estructural
verificado en la ubicación de las clases**, no como incidente observado. Es exactamente el tipo de detalle que
[Bloque 258] §258.1 hizo explícito para el chart —qué vive en `-rt` y qué en `-wb`— aplicado acá a un caso
donde la separación parece violada.

## 267.4 — Gotchas de la UI `[CERT]`

- **Contraseña como columna de tabla**: `BJoinProfileManager` declara
  `colDefaultUserPassword = new Prop(BJoinProfile.defaultUserPassword, 1)` (`$W/ui/BJoinProfileManager.java:47`)
  `[CERT]`. El flag `1` la deja oculta por defecto en el selector de columnas, pero **el enmascarado no se
  impone en esta capa**: depende de que la propiedad tenga su propio field editor de password.
  `[INFER]` Un operador que active esa columna podría ver la credencial de join en una tabla.
- **Dos managers compiten por el mismo tipo**: `BExportTagSummaryManager` y `BJoinProfileManager` son ambos
  agentes sobre `SubordinateExportTagNetworkExt` — el usuario elige cuál abrir desde el selector de vistas.
- `JobTracker` ofrece exportar el log del join a archivo tras completarse.

## 267.5 — Conexiones

- **[Bloque 266]** — el runtime del join que esta UI maneja; y el recordatorio de que `exportTags` no es
  tagging.
- **[Bloque 211]**, **[Bloque 214]**, **[Bloque 202]** — los mecanismos de UI reutilizados sin variación
  (§267.1).
- **Focuses PX (B179-B215)** — §267.2 es el puente que faltaba: aquellos documentaron cómo se **construye**
  una vista PX; éste, cómo se **distribuye** a una flota con reescritura de ORDs.
- **[Bloque 258]** §258.1 — el criterio `-rt`/`-wb` que hace visible el riesgo de §267.3.
- **Gaps abiertos**: T8 (UI del diccionario de tags), T9 (doc oficial preservada).
