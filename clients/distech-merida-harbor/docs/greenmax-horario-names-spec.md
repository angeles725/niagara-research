# HARBOR GreenMAX — nombres de horario editables y reflejados en todo

> Cierra el pendiente **#5** del `greenmax-redesign-status.md` y una parte del gap **B853-G2**
> (mostrar el nombre en el tablero) sin usar EnumWritable de etiquetas fijas.
> Evidencia de control: `kitControl-rt` — `BStringSelect`, `BMuxSwitch`, `BSwitch` (docSource original) **[CERT]**.

## Objetivo
El cliente escribe el **nombre** de cada uno de los 5 horarios desde el menú, y ese nombre se ve
**en todas las pantallas** (menú + tableros), incluido el selector de cada circuito. Un solo cambio
se propaga a todo.

## Principio de propagación
Los 5 nombres viven **una sola vez**. Cada pantalla **bindea al mismo ord**. Cambiar el nombre
una vez actualiza todas las pantallas, porque todas leen el mismo punto.

---

## 1. Control a crear

### 1.1 Nombres (compartidos, una sola vez) — en `Config/Iluminacion/Horarios/`
Crear **5 `Horario{n}_Nombre` = `BStringWritable`** (`n = 1..5`).
- Poner el valor por defecto en el slot **`fallback`** de cada uno: `Horario 1` … `Horario 5`
  (con `fallback` seteado, el `out` nunca queda nulo aunque nadie escriba).
- No se les linkea nada de entrada: el cliente los escribe desde el menú (§3).

### 1.2 Multiplexor de nombre por circuito — en cada panel `Config/Iluminacion/GMxx/`
Por cada circuito `k`, crear **`NameMux{k}` = `BStringSelect`** (paleta `kitControl/util`).
Se configura **idéntico al `SchedMux{k}` (BooleanSelect) que ya está desplegado**, solo que de String:

- `NameMux{k}.numberValues = 5`  · `zeroBasedSelect` = **igual que el SchedMux{k} existente** (no cambiar el criterio; copiar el mismo).
- Entradas (índice base 1, verificado en `BStringSelect.getInStatusValue`: `case 1→inA … case 5→inE`):
  - `inA` ← `Horario1_Nombre.out`
  - `inB` ← `Horario2_Nombre.out`
  - `inC` ← `Horario3_Nombre.out`
  - `inD` ← `Horario4_Nombre.out`
  - `inE` ← `Horario5_Nombre.out`
- `select` ← `SelR{k}.out`  ← **el MISMO selector numérico 1..5 que ya existe** (el que alimenta a `SchedMux{k}`).
- `NameMux{k}.out` (BStatusString) = el nombre del horario actualmente elegido en ese circuito.

Así el `out` refleja **las dos cosas**: si el cliente renombra el horario, cambia el string; si el
operador cambia la selección (1..5), el mux toma otra entrada. Ambos, dinámicos.

> El `select` de `BSwitch` es un `BStatusEnum` que usa `getOrdinal()`; el link desde `SelR{k}` (numérico)
> ya está probado en el `SchedMux{k}` desplegado, así que se replica sin cambios. **[CERT]** `BSwitch.java`.

---

## 2. Regla de escritura en el navegador (Hx) — por qué "Set", igual que el selector
La estación corre perfil **Hx**. El camino probado para escribir desde el Px es el **diálogo de Set**
(`ActionBinding` a `.../set`, sin argumento fijo): el operador **teclea** el valor. Para un
`BStringWritable`, ese mismo diálogo pide **texto** en vez de número. Es el mismo patrón que ya
funciona con `SelR{k}/set` (ver §3 del status doc y B853).

**No** intentar botones con valor fijo (`set("...")`): en Hx fallan silencioso, igual que el numérico.

---

## 3. Menú (`GreenMAX Iluminacion.px`) — mostrar y editar el nombre
El menú **ya muestra** el nombre (los 5 labels ya bindean a `Horario{n}_Nombre` con
`ObjectToString %out.value%`). Solo faltan dos cosas:
- Que los 5 `Horario{n}_Nombre` **existan** en la estación (si no, los labels salen en null).
- **Editar**: superponer sobre cada label un `ImageButton` invisible con `ActionBinding` a
  `Horario{n}_Nombre/set` (mismo patrón exacto del selector `SelR{k}/set` del tablero → abre el
  diálogo de Set, que para un String pide texto).

```xml
<!-- Label del nombre (YA existe en el menú) -->
<Label layout="1160.0,290.0,175.0,20.0" font="bold 12.0pt Arial" halign="left">
  <ValueBinding ord="station:|slot:/Iluminacion/Horarios/Horario1_Nombre">
    <ObjectToString name="text" format="%out.value%"/>
  </ValueBinding>
</Label>
<!-- AÑADIR encima: botón invisible que abre el Set de texto -->
<ImageButton layout="1160.0,290.0,175.0,20.0" buttonStyle="none">
  <ActionBinding ord="station:|slot:/Iluminacion/Horarios/Horario1_Nombre/set"
                 widgetEvent="actionPerformed"/>
</ImageButton>
```

Repetir para `Horario2..5_Nombre` (y en las coordenadas de cada fila).

---

## 4. Tablero (`GreenMAXxx TabALy.px`) — el selector muestra el NOMBRE
Hoy cada selector es: `Label` con `ObjectToString format="Hor %out.value%"` sobre `SelR{k}` (número)
+ `ImageButton` invisible con `ActionBinding` a `SelR{k}/set`. Solo se cambia **el ord del Label** a
`NameMux{k}` y el `format` a `%out.value%`. El botón de Set numérico (`SelR{k}/set`) **no se toca**.

```xml
<!-- ANTES: muestra el numero -->
<ValueBinding ord="station:|slot:/Iluminacion/GM02/SelR1">
  <ObjectToString name="text" format="Hor %out.value%"/>
</ValueBinding>

<!-- DESPUES: muestra el nombre del horario elegido -->
<ValueBinding ord="station:|slot:/Iluminacion/GM02/NameMux1">
  <ObjectToString name="text" format="%out.value%"/>
</ValueBinding>
```

> Tradeoff aceptado: el operador **ve el nombre** pero para cambiarlo **teclea el número 1..5**
> (restricción de Hx). El dropdown de nombres real es el gap **B853-G2** (EnumWritable + Hx), que
> queda para después. Este diseño ya cumple "ver el nombre reflejado en todo".

---

## 5. Orden de aplicación (Workbench)
1. Crear los 5 `Horario{n}_Nombre` con su `fallback` por defecto (una vez).
2. En GM02 (piloto): crear los `NameMux{k}` y linkear entradas + `select ← SelR{k}`.
3. Editar el Px del menú (bindings §3) y el Px del tablero GM02 (bindings §4).
4. Validar en el navegador Hx: el nombre se muestra, el diálogo de Set (texto) escribe, y el cambio
   aparece a la vez en menú y tablero.
5. Replicar `NameMux{k}` + bindings de tablero al resto de paneles (junto con el pendiente #4).

## 6. Verificación
- Renombrar `Horario3_Nombre` desde el menú → el menú y **cada tablero** cuyo `SelR{k}=3` muestran el
  nuevo nombre sin re-linkear nada.
- Cambiar `SelR{k}` de 3 a 5 → la etiqueta del tablero pasa a mostrar `Horario5_Nombre`.
