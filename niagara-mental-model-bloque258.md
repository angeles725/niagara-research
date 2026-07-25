# Bloque 258 — Chart clásico (VIII): el split rt/wb — qué sabe una station de un chart que no puede dibujar

> **Qué documenta**: las **5 clases** de `chart-rt` y por qué el runtime de una station Niagara necesita
> exactamente esas y ninguna más. Cierra el gap **H8** — el último del focus `px-chart-classic`.
>
> **Fuentes** (decompilado vineflower + los `module.xml` de ambos artefactos, leídos íntegros inline; las 5
> clases suman 352 líneas):
> - `$R` = `…/chart/chart-rt/vineflower/javax/baja/chart/`
> - `$MR` = `…/chart/chart-rt/vineflower/META-INF/module.xml`
> - `$MW` = `…/chart/chart-wb/vineflower/META-INF/module.xml`
> - Raíz: `/home/cristian/modules/Prototipos/modulos/organized/`
>
> **Método**: sin sub-agente. Marcadores: `[CERT]` = fuente primaria; `[INFER]` = deducción. Bloque de
> EVIDENCIA.

---

## 258.1 — La medida exacta del split: 1 dependencia contra 14 `[CERT]`

Ambos artefactos declaran el mismo módulo lógico (`moduleName="chart"`, `description="Chart API"`,
`vendorVersion="4.14.0.162"`, `releaseDate="2024-05-28"`, `nre="true"`, `autoload="true"`,
`installable="true"`) y se distinguen solo por `runtimeProfile` `[CERT]` (`$MR:2`, `$MW:2`).

Lo que los separa de verdad es el árbol de dependencias:

| | `chart-rt` | `chart-wb` |
|---|---|---|
| Clases | **5** | 62 |
| Dependencias declaradas | **1** — `baja` `[CERT]` (`$MR:4`) | **14+** — `alarm-rt`, `baja`, `bajaScript-ux`, `bajaui-ux`, `bajaui-wb`, `bajaux-rt`, `bajaux-ux`, `box-rt`, `bql-rt`, `chart-rt`, `control-rt`, … `[CERT]` (`$MW:4-14`) |

`[INFER]` Esa asimetría **es** la respuesta al gap. `chart-rt` depende únicamente del núcleo `baja`: no
conoce `bajaui` (widgets), ni `bajaux` (web), ni `bql`, ni `control`. Por construcción **no puede dibujar
nada** — no tiene con qué. Es un módulo de *tipos*, no de comportamiento.

## 258.2 — Las 5 clases: todo lo que viaja, nada que se ejecute `[CERT]`

| Clase | Tipo base | Rol | ¿Registrada en `module.xml`? |
|---|---|---|---|
| `BAxisDimension` | `BFrozenEnum` `[CERT]` | `{x, y}` — dirección del eje | **sí**, `type name="AxisDimension"` (`$MR:9`) |
| `BAxisLocation` | `BFrozenEnum` `[CERT]` | `{top, bottom, left, right}` — borde del canvas | **sí** (`$MR:10`) |
| `BAxisBound` | `BSimple` `[CERT]` | `auto` \| `fixed,<typespec>,<valor>` — el límite de un eje | **sí** (`$MR:11`) |
| `BColumnIdentifier` | `BSimple` `[CERT]` | `null` \| `rowIndex` \| `tableColumn:<nombre>` | **sí** (`$MR:12`) |
| `TrendFlags` | **Java plano** (`public class`, sin `B`) `[CERT]` | bitmask de calidad del dato | **no** — y es correcto: no es un `BObject` |

`[INFER]` El criterio de reparto queda explícito: **al runtime va lo que se SERIALIZA**. Los cuatro tipos
registrados son exactamente los que pueden aparecer escritos dentro de un archivo `.px` o `.bog` como valor de
propiedad de un binding de chart ([Bloque 253] §253.5). Una station tiene que poder **leer, validar y volver a
escribir** esos valores — aunque jamás vaya a pintar un píxel. El quinto, `TrendFlags`, no se serializa: es la
convención de bits que ambos lados deben interpretar igual.

## 258.3 — `TrendFlags`: la única "lógica" del runtime `[CERT]`

Clase estática pura: 8 constantes y 5 predicados, sin estado ni constructor útil.

```java
   public static final int START = 1;
   public static final int OUT_OF_ORDER = 2;
   public static final int HIDDEN = 4;
   public static final int MODIFIED = 8;
   public static final int INTERPOLATED = 16;
   public static final int RESERVED_0 = 32;
   public static final int RESERVED_1 = 64;
   public static final int RESERVED_2 = 128;
```
`$R/TrendFlags.java:4-11` `[CERT]`

Los predicados `isStart` / `isOutOfOrder` / `isHidden` / `isModified` / `isInterpolated` son máscaras de una
línea (`:13-31`). **Los tres `RESERVED_*` no tienen predicado** `[CERT]` — están declarados y sin uso.

`[INFER]` Que esta clase viva en el runtime y no en el Workbench es coherente con [Bloque 251] §251.6: el
puente `BStatus → trend flags` ocurre al leer los datos (`TableSeries.StatusToInt`), y el productor de esos
datos es del lado de la station. El vocabulario de bits tiene que ser compartido; el dibujo, no.

## 258.4 — La respuesta al gap `[INFER]`

**¿Qué necesita saber una station de un chart que no puede dibujar?** Cuatro tipos serializables y una
convención de bits. Nada más:

1. **Cómo está orientado y ubicado un eje** (`BAxisDimension`, `BAxisLocation`) — para conservar la
   configuración al guardar/cargar.
2. **Cuáles son los límites de un eje** (`BAxisBound`), incluida la distinción auto/fijo.
3. **Qué columna de la tabla alimenta cada serie** (`BColumnIdentifier`).
4. **Qué significan los bits de calidad del dato** (`TrendFlags`).

`[INFER]` Es el mínimo indispensable para que un `.px` con un chart **sobreviva un round-trip** por una
station sin Workbench: se lee, se valida contra los tipos registrados, se vuelve a escribir intacto. El motor
—modelo, ejes concretos, render, interacción— vive entero en `-wb` y solo existe cuando hay un Workbench.

Esto cierra el arco del focus y explica de raíz el hallazgo de [Bloque 254] §254.8: el chart clásico **no puede
servir al browser por sí mismo** porque su motor no está en el runtime. El puente Hx de [Bloque 256] §256.3 no
es una excepción a esa regla sino su confirmación — necesita el `-wb` cargado del lado del servidor para
rasterizar, y por eso entrega una imagen muerta.

## 258.5 — Conexiones

- **[Bloque 252]** §252.4 y **[Bloque 253]** §253.5 — los cuatro tipos serializables, documentados desde el
  lado del consumidor; acá desde el lado del reparto de artefactos.
- **[Bloque 251]** §251.6 — `TrendFlags` y el puente desde `BStatus`.
- **[Bloque 254]** §254.1 y §254.8 — explica por qué los consumidores reales son todos `-wb`, y por qué el
  perfil decide el motor.
- **[Bloque 256]** §256.3 — el puente Hx confirma la regla en vez de romperla.
- **Estado del focus**: con H8 cerrado, **read-only-investigable = 0 → STOP (§8)**. Sigue el bloque de
  síntesis de cierre.
