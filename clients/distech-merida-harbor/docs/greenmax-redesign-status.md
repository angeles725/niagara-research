# HARBOR GreenMAX — rediseño de iluminación: estado, lógica nueva y pendientes

> Estado al 2026-09-10 (~5:30 a.m.). Estación **HM_BMS** (supervisor Windows, EC-Net 4.3.58.18 ≈ N4.4) +
> JACE **HM_Central** (192.168.1.102). Corpus: bloques **B841–B848** (diseño), **B851** (cadena real + pilot),
> **B852** (diseño UI), **B853** (comportamiento en el navegador / Hx). Este doc es el handoff operativo.

---

## 1. Qué pidió el cliente
Reemplazar el control actual (un schedule dedicado por circuito, ~349) por:
- **5 horarios maestros**, editables desde el menú de tableros (incluye poder **escribir el nombre** de cada horario).
- Cada horario con su **Auto / ON / OFF** (HOA) — el ON/OFF vive **a nivel de horario**, no por interruptor.
- Cada **interruptor** solo **elige qué horario sigue** (selector).
- LED del menú **verde/rojo** (verde = encendido, rojo = apagado).
- En el tablero: el **LED = status de `HorR{k}`** (el comando), el **pill = valor del relé físico `Relay[k].BO`**.
- El selector debe abrir **solo "Set"** para escribir 1–5 (no el menú completo de acciones).

## 2. La lógica nueva (control)
Cadena verificada, por circuito (uniforme GM02–GM16):
```
5 masters compartidos (una vez, en Config/Iluminacion/Horarios/):
   Horario{n} (BooleanSchedule) → Hor{n}_Eff (BooleanWritable) .in10   (Auto = sigue el programa)
   botones menú: ON→Hor{n}_Eff/active · OFF→/inactive · Auto→/auto     (sin argumento; ver §3)
   [passthrough] Hor{n}_Eff.out → Hor{n}_HOA.inA ; Hor{n}_Mode=1        (para NO re-linkear circuitos)

por circuito k (en Config/Iluminacion/GM02/):
   Hor1..5_HOA.out → SchedMux{k} (BooleanSelect n=5) ← SelR{k} (NumericWritable 1..5)
   SchedMux{k}.out → HorR{k}.in10  (cutover; se quitó R{k}.out → HorR{k}.in10)
   → (proxy) JACE HorGM02/HorR{k} → BcpBacnetNetwork/GM02ilum/Relay[k].BO.in10 → relé físico
```
Todo vive en el **supervisor**; el JACE/BACnet no se toca.

## 3. Por qué el HOA es BooleanWritable y el selector abre "Set" (clave del navegador)
La estación corre el **perfil Hx** en el navegador (no bajaux). En Hx, escribir un **valor fijo** a un
NumericWritable desde un botón (`set(2)`) **falla silencioso** (no convierte el string a `BDouble`). Por eso:
- El HOA del horario es **BooleanWritable** con `active`/`inactive`/`auto` (**sin argumento**, sí funcionan),
  cada botón con `<Override name="actionArg"/>` vacío para no pedir "Override Duration".
- El **selector** por circuito bindea a **`SelR{k}/set`** (ActionBinding, sin actionArg) → abre **solo el diálogo
  de Set** donde el operador **teclea** 1–5 (ese camino sí funciona en el navegador).
Detalle completo y evidencia en **B853**.

## 4. Qué se hizo y dónde (GM02 + menú)
- **Control GM02:** 5 `Hor{n}_Eff` (BooleanWritable) + passthrough; 20 `SchedMux{k}`+`SelR{k}` (creados con
  Paste Special "Keep all links"), cutover a `HorR{k}.in10`. `Hor{n}_Mode` a NumericWritable=1.
- **Px menú** (`GreenMAX Iluminacion.px`): 5 filas de Horarios a la derecha (nombre + LED verde/rojo +
  Auto/ON/OFF + editar schedule). Ords absolutos `station:|slot:/Iluminacion/Horarios/…`. Imports agregados:
  `control`, `converters`.
- **Px tablero** (`GreenMAX02 TabAL6.px`): por circuito, pill=`Relay[k].BO` (valor), LED=`HorR{k}` (status),
  selector "Hor N" (Set dialog); se quitaron los botones Auto/taps y el iconito de calendario.
- Código Px completo entregado como `.md` al operador (scratchpad de la sesión).

## 5. LO QUE FALTA (pendientes)
**Ejecución (cuando se pueda):**
1. **Confirmar GM02 en el navegador de punta a punta** (botones sin popup, selector Set, LEDs correctos).
2. **Contenido de los 5 horarios** — el programa semanal de cada uno (lo define el cliente).
3. **Cuadro de cargas** — 19 circuitos sin descripción (GM05×13, GM16×6).
4. **Replicar a los otros 12 paneles** (GM03–GM16, menos los caídos GM01/GM03/GM09) — control + Px por panel.
   Para cada panel: los `Hor{n}_Eff` son compartidos (una vez); por panel van `SelR{k}` + `SchedMux{k}` + el Px.
5. **Nombres de horario** — crear los `Horario{n}_Nombre` (StringWritable) si se quieren nombres.
6. **Decomisionar** los ~349 schedules viejos (`R{k}`) tras validar; conservar los 5 masters.
7. **Comercial** — confirmar la extensión vs la cotización de 2 estados de SEJOFA.

**Investigación registrada para después (gaps en RESEARCH-STATE, NO hacer ahora):**
- **B853-G2** — selector más limpio con **EnumWritable** (nombres de horario) + dropdown en Hx.
- **B853-G3** — limpieza del passthrough (re-apuntar circuitos a `Hor{n}_Eff`, quitar el BooleanSelect redundante).
- **B853-G4** — diferencias Hx vs bajaux por versión N4 (referencia de qué hace/no hace el Px stock en cada motor).
- **B853-G5** — opción de **módulo custom** (JS+servlet, patrón DashboardPan) si el cliente quiere escritura
  numérica sin diálogo de Set.

## 6. Riesgos / notas
- El pill del tablero es solo lectura (ya no controla) — el manual se fue al horario. Confirmar que al cliente le sirve.
- Al aplicar cutovers con horarios vacíos + modos en ON, los circuitos quedan encendidos hasta cargar los schedules reales.
- El corpus `organized/` es N4.14; el módulo real del cliente (`kitPx-wb` 4.3.58.18) se trajo a
  `organized/_client-ecnet-4.3.58.18-n4.4/` — usar ESE para dudas de comportamiento en la estación del cliente.
