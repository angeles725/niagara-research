# Plantilla de expediente técnico por prueba — Acuerdo OEM Tridium §6.3

> **Propósito.** Marco reproducible vacío que cumple, campo por campo, la estructura mínima que el Acuerdo
> exige (§6.3, incisos a–k) para cada prueba de evasión autorizada. Se llena **una copia por prueba**, y
> **solo después** de que exista la autorización firmada auténtica y el alcance del Anexo I/J aprobado.
>
> **Regla del Acuerdo (§6.3, cierre):** la ausencia de evidencia suficiente marca la prueba como
> **"no verificada"** o **"no reproducible"**, y NO autoriza despliegue, reutilización ni presentación como
> hallazgo confirmado.
>
> **Convención de nombre de archivo:** `EXP-<YYYYMMDD>-<slug-corto>.md` (un archivo por prueba).

---

## 0. Estado de autorización (pre-requisito — bloquea la ejecución)

- [ ] Acuerdo firmado de manera auténtica por representante **real e identificable** de Tridium con facultades.
- [ ] Alcance de esta prueba incluido en **Anexo I** o en un **Acta del Anexo J** (producto, versión, host, cuenta, entorno).
- [ ] Ventana autorizada, responsables (técnico + contractual) y **criterio de aborto** definidos.
- [ ] Entorno = **laboratorio aislado**; sin producción, sin datos reales, sin sistemas de terceros.

> Si alguna casilla está sin marcar, **no se ejecuta**. El expediente queda en estado `BORRADOR / NO AUTORIZADO`.

---

## a. Identificación (§6.3.a)

| Campo | Valor |
|---|---|
| ID único de prueba | `EXP-________-________` |
| Fecha | |
| Investigador(es) | |
| Aprobador(es) | |
| Entorno (lab/host/red) | |

## b. Objeto evaluado (§6.3.b)

| Campo | Valor |
|---|---|
| Producto / familia | |
| Módulo / componente | |
| Archivo / binario | |
| Versión / build | |
| Ruta | |
| **Hash SHA-256 (original)** | |

## c. Hipótesis y criterio (§6.3.c)

- **Control que se evalúa:** _(host-id / firma de módulo / entitlement / expiración / certificado / integridad)_
- **Hipótesis:** _(qué se cree que se puede evitar/alterar y por qué)_
- **Criterio objetivo de ÉXITO:** _(observable que confirma la evasión)_
- **Criterio objetivo de FRACASO:** _(observable que confirma que el control resiste)_

## d. Línea base legítima (§6.3.d)

_Comportamiento observado **antes de cualquier cambio**, con el control operando normalmente. Incluir salida/logs de referencia._

## e. Procedimiento paso a paso (§6.3.e)

| # | Acción | Comando / herramienta | Parámetros | Entrada | Salida observada |
|---|---|---|---|---|---|
| 1 | | | | | |
| 2 | | | | | |
| … | | | | | |

## f. Código mínimo para reproducir (§6.3.f)

```
# fuente / pseudocódigo / script / fragmento mínimo necesario
```

## g. Tabla completa de cambios (§6.3.g)

| Archivo | Función | Clase | Método | Offset | Parámetro | Valor anterior | Valor posterior | Justificación |
|---|---|---|---|---|---|---|---|---|
| | | | | | | | | |

## h. Inventario de binarios y restauración (§6.3.h)

| Binario | SHA-256 **antes** | SHA-256 **después** | Mecanismo de restauración |
|---|---|---|---|
| | | | |

> Todo binario modificado es una **copia de laboratorio**. Documentar cómo se revierte al estado original.

## i. Evidencia primaria (§6.3.i)

| Tipo | Ruta / referencia | Hash | Nota |
|---|---|---|---|
| log | | | |
| captura / video | | | |
| traza / pcap | | | |
| volcado (dump) | | | |
| manifest / diff / parche | | | |
| reporte de herramienta | | | |

## j. Resultado y cierre (§6.3.j)

- **Resultado observado:** _(¿se cumplió el criterio de éxito o el de fracaso?)_
- **Impacto:** 
- **Limitaciones:** 
- **Causa raíz propuesta:** 
- **Mitigación / control compensatorio sugerido:** 
- **Prueba de regresión** _(confirmar que, tras la remediación, la técnica ya NO funciona)_: 

## k. Aprobación (§6.3.k)

| Rol | Nombre | Fecha | Firma/aprobación electrónica |
|---|---|---|---|
| Investigador | | | |
| Aprobador Tridium | | | |
| Aprobador OEM/Distribuidor | | | |

---

## Veredicto de suficiencia (§6.3, cierre — obligatorio)

- [ ] Evidencia **suficiente y reproducible** → puede presentarse como hallazgo.
- [ ] Evidencia **insuficiente** → marcar **`NO VERIFICADA`** / **`NO REPRODUCIBLE`**; prohibido despliegue,
      reutilización o presentación como hallazgo confirmado.

**Manejo de la evidencia:** confidencial; sin publicación de métodos de evasión; sin uso fuera de los fines de
seguridad acordados (§6.1, §6.4). Retención según §8.4 del Acuerdo.
