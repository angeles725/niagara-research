# Checklist — nombres de horario en GM02 (plantilla) + replicación

> Aplica el diseño de `greenmax-horario-names-spec.md`. GM02 es la **plantilla**: una vez validado en
> el navegador, se replica al resto de paneles con el mismo procedimiento (Parte E).
> Todo se hace en el **supervisor HM_BMS** (Workbench). El JACE/BACnet no se toca.

Rutas: nombres compartidos en `Config/Iluminacion/Horarios/` · control por panel en `Config/Iluminacion/GM02/`.

---

## Parte A — Control compartido (UNA sola vez, no por panel)
- [ ] En `Config/Iluminacion/Horarios/` crear **5 `BStringWritable`**: `Horario1_Nombre` … `Horario5_Nombre`.
- [ ] En cada uno, poner el slot **`fallback`** = nombre por defecto (`Horario 1` … `Horario 5`).
      (Con `fallback` seteado el `out` nunca queda nulo; el cliente lo sobreescribe desde el menú.)
- [ ] Verificar en el menú: los 5 labels de la derecha (que YA bindean a `Horario{n}_Nombre`) dejan de
      salir en null y muestran el nombre por defecto.

> Estos 5 puntos y los `Hor{n}_Eff` son **compartidos**: se crean una vez y sirven para TODOS los paneles.

---

## Parte B — Control por panel: GM02
En `Config/Iluminacion/GM02/`, por cada circuito `k` (GM02 tiene **20**: k = 1..20):
- [ ] Crear `NameMux{k}` = **`BStringSelect`** (paleta `kitControl/util`).
- [ ] `NameMux{k}.numberValues = 5`.
- [ ] `NameMux{k}.zeroBasedSelect` = **igual que el `SchedMux{k}` que ya existe** (copiar el mismo valor; no cambiar el criterio).
- [ ] Linkear entradas (índice base 1):
      `inA ← Horario1_Nombre.out` · `inB ← Horario2_Nombre.out` · `inC ← Horario3_Nombre.out` ·
      `inD ← Horario4_Nombre.out` · `inE ← Horario5_Nombre.out`.
- [ ] Linkear `NameMux{k}.select ← SelR{k}.out` (el MISMO `SelR{k}` que ya alimenta a `SchedMux{k}`).
- [ ] Verificar: `NameMux{k}.out` muestra el nombre del horario que marca `SelR{k}` (1..5).

> Truco rápido: crea `NameMux1` completo, y **duplícalo** 19 veces re-apuntando solo `select ← SelR{k}`
> (las 5 entradas de nombre son iguales para todos los circuitos del panel).

---

## Parte C — Menú (`GreenMAX Iluminacion.px`): habilitar edición del nombre
Los 5 labels de nombre YA existen (display). Solo falta el botón invisible para editarlos:
- [ ] Sobre cada label de nombre, añadir un `ImageButton` invisible con `ActionBinding` a
      `Horario{n}_Nombre/set` (mismo patrón que el selector). Coordenadas = las del label de esa fila:

```xml
<ImageButton layout="1160.0,290.0,175.0,20.0" buttonStyle="none"><ActionBinding ord="station:|slot:/Iluminacion/Horarios/Horario1_Nombre/set" widgetEvent="actionPerformed" /></ImageButton>
<ImageButton layout="1160.0,360.0,175.0,20.0" buttonStyle="none"><ActionBinding ord="station:|slot:/Iluminacion/Horarios/Horario2_Nombre/set" widgetEvent="actionPerformed" /></ImageButton>
<ImageButton layout="1160.0,430.0,175.0,20.0" buttonStyle="none"><ActionBinding ord="station:|slot:/Iluminacion/Horarios/Horario3_Nombre/set" widgetEvent="actionPerformed" /></ImageButton>
<ImageButton layout="1160.0,500.0,175.0,20.0" buttonStyle="none"><ActionBinding ord="station:|slot:/Iluminacion/Horarios/Horario4_Nombre/set" widgetEvent="actionPerformed" /></ImageButton>
<ImageButton layout="1160.0,570.0,175.0,20.0" buttonStyle="none"><ActionBinding ord="station:|slot:/Iluminacion/Horarios/Horario5_Nombre/set" widgetEvent="actionPerformed" /></ImageButton>
```

> El menú es **uno solo**: esto se hace una vez y aplica a todo el edificio.

---

## Parte D — Tablero GM02 (`GreenMAX02 TabAL6.px`): mostrar el nombre en el selector
Único cambio: en cada uno de los 20 selectores, el **Label** deja de leer `SelR{k}` ("Hor N") y pasa a
leer `NameMux{k}` (el nombre). El botón de Set (`SelR{k}/set`) **no cambia**.
- [ ] Reemplazar el bloque completo de los 20 selectores por el del **Apéndice 1** (ya modificado,
      `NameMux1..20`, `/set` intactos). O, label por label: cambiar
      `ord=".../SelR{k}"` `format="Hor %out.value%"` → `ord=".../NameMux{k}"` `format="%out.value%"`.

---

## Parte E — Validación en el navegador (Hx) — hacer ANTES de replicar
- [ ] Menú: tocar un nombre abre el diálogo de **Set** y pide **texto**; al guardar, el nombre cambia
      en el menú **y** en el tablero (en los circuitos cuyo `SelR{k}` apunta a ese horario).
- [ ] Tablero: cada selector muestra el **nombre** (no "Hor N"); tocarlo abre el Set numérico (1..5) y
      al cambiarlo, el nombre mostrado cambia al horario correspondiente.
- [ ] **Punto a confirmar en vivo:** que el diálogo de Set de un `BStringWritable` funcione en Hx
      (por analogía con `SelR{k}/set`, que sí funciona; es el único paso no probado aún).

> Si el Set de texto NO funciona en Hx, el fallback es el gap **B853-G5** (módulo custom con servlet).
> No bloquear la replicación por esto: el **display** del nombre funciona igual; solo la edición depende de esta prueba.

---

## Parte F — Replicar a los demás tableros
Repetir SOLO Parte B (control por panel) + Parte D (rebind del selector) en cada panel. Parte A y C ya
están hechas y son compartidas.

Paneles operativos a replicar (excluye los caídos **GM01 / GM03 / GM09**):
- [ ] GM04   - [ ] GM05   - [ ] GM06   - [ ] GM07   - [ ] GM08
- [ ] GM10   - [ ] GM11   - [ ] GM12   - [ ] GM13
- [ ] GM14   - [ ] GM15   - [ ] GM16

Por cada panel `GMxx`:
1. Contar sus circuitos (varía por panel; GM02 tiene 20). Crear `NameMux{k}` para `k = 1..N` de ese panel.
2. `select ← SelR{k}` del propio panel; entradas `inA..inE ← Horario1..5_Nombre.out` (compartidos).
3. En el Px del tablero: cambiar cada Label de selector de `SelR{k}` → `NameMux{k}`
   (reemplazar `GM02` por `GMxx` en el ord). El `SelR{k}/set` no se toca.
4. Validar en el navegador (Parte E) al menos en un circuito.

> Nota: reusa el bloque del Apéndice 1 haciendo un find/replace `GM02` → `GMxx` y ajustando el número
> de circuitos (quitar los que sobren si el panel tiene menos de 20).

---

## Apéndice 1 — Bloque de selectores GM02 ya modificado (copiar/pegar)
Reemplaza el bloque de selectores actual del `GreenMAX02 TabAL6.px` por este (labels → `NameMux{k}`):

```xml
  <Picture layout="50.0,190.0,60.0,22.0" image="file:^Imagenes/The Harbor/Buttons/BotonNormal.png" scale="fitWidth" /><Label layout="50.0,190.0,60.0,20.0" halign="center" font="bold 10.0pt Arial"><ValueBinding ord="station:|slot:/Iluminacion/GM02/NameMux1"><ObjectToString name="text" format="%out.value%" /></ValueBinding></Label><ImageButton layout="50.0,190.0,60.0,22.0" buttonStyle="none"><ActionBinding ord="station:|slot:/Iluminacion/GM02/SelR1/set" widgetEvent="actionPerformed" /></ImageButton><Picture layout="50.0,250.0,60.0,22.0" image="file:^Imagenes/The Harbor/Buttons/BotonNormal.png" scale="fitWidth" /><Label layout="50.0,250.0,60.0,20.0" halign="center" font="bold 10.0pt Arial"><ValueBinding ord="station:|slot:/Iluminacion/GM02/NameMux2"><ObjectToString name="text" format="%out.value%" /></ValueBinding></Label><ImageButton layout="50.0,250.0,60.0,22.0" buttonStyle="none"><ActionBinding ord="station:|slot:/Iluminacion/GM02/SelR2/set" widgetEvent="actionPerformed" /></ImageButton><Picture layout="50.0,310.0,60.0,22.0" image="file:^Imagenes/The Harbor/Buttons/BotonNormal.png" scale="fitWidth" /><Label layout="50.0,310.0,60.0,20.0" halign="center" font="bold 10.0pt Arial"><ValueBinding ord="station:|slot:/Iluminacion/GM02/NameMux3"><ObjectToString name="text" format="%out.value%" /></ValueBinding></Label><ImageButton layout="50.0,310.0,60.0,22.0" buttonStyle="none"><ActionBinding ord="station:|slot:/Iluminacion/GM02/SelR3/set" widgetEvent="actionPerformed" /></ImageButton><Picture layout="50.0,370.0,60.0,22.0" image="file:^Imagenes/The Harbor/Buttons/BotonNormal.png" scale="fitWidth" /><Label layout="50.0,370.0,60.0,20.0" halign="center" font="bold 10.0pt Arial"><ValueBinding ord="station:|slot:/Iluminacion/GM02/NameMux4"><ObjectToString name="text" format="%out.value%" /></ValueBinding></Label><ImageButton layout="50.0,370.0,60.0,22.0" buttonStyle="none"><ActionBinding ord="station:|slot:/Iluminacion/GM02/SelR4/set" widgetEvent="actionPerformed" /></ImageButton><Picture layout="50.0,430.0,60.0,22.0" image="file:^Imagenes/The Harbor/Buttons/BotonNormal.png" scale="fitWidth" /><Label layout="50.0,430.0,60.0,20.0" halign="center" font="bold 10.0pt Arial"><ValueBinding ord="station:|slot:/Iluminacion/GM02/NameMux5"><ObjectToString name="text" format="%out.value%" /></ValueBinding></Label><ImageButton layout="50.0,430.0,60.0,22.0" buttonStyle="none"><ActionBinding ord="station:|slot:/Iluminacion/GM02/SelR5/set" widgetEvent="actionPerformed" /></ImageButton><Picture layout="50.0,490.0,60.0,22.0" image="file:^Imagenes/The Harbor/Buttons/BotonNormal.png" scale="fitWidth" /><Label layout="50.0,490.0,60.0,20.0" halign="center" font="bold 10.0pt Arial"><ValueBinding ord="station:|slot:/Iluminacion/GM02/NameMux6"><ObjectToString name="text" format="%out.value%" /></ValueBinding></Label><ImageButton layout="50.0,490.0,60.0,22.0" buttonStyle="none"><ActionBinding ord="station:|slot:/Iluminacion/GM02/SelR6/set" widgetEvent="actionPerformed" /></ImageButton><Picture layout="50.0,550.0,60.0,22.0" image="file:^Imagenes/The Harbor/Buttons/BotonNormal.png" scale="fitWidth" /><Label layout="50.0,550.0,60.0,20.0" halign="center" font="bold 10.0pt Arial"><ValueBinding ord="station:|slot:/Iluminacion/GM02/NameMux7"><ObjectToString name="text" format="%out.value%" /></ValueBinding></Label><ImageButton layout="50.0,550.0,60.0,22.0" buttonStyle="none"><ActionBinding ord="station:|slot:/Iluminacion/GM02/SelR7/set" widgetEvent="actionPerformed" /></ImageButton><Picture layout="50.0,610.0,60.0,22.0" image="file:^Imagenes/The Harbor/Buttons/BotonNormal.png" scale="fitWidth" /><Label layout="50.0,610.0,60.0,20.0" halign="center" font="bold 10.0pt Arial"><ValueBinding ord="station:|slot:/Iluminacion/GM02/NameMux8"><ObjectToString name="text" format="%out.value%" /></ValueBinding></Label><ImageButton layout="50.0,610.0,60.0,22.0" buttonStyle="none"><ActionBinding ord="station:|slot:/Iluminacion/GM02/SelR8/set" widgetEvent="actionPerformed" /></ImageButton><Picture layout="240.0,190.0,60.0,22.0" image="file:^Imagenes/The Harbor/Buttons/BotonNormal.png" scale="fitWidth" /><Label layout="240.0,190.0,60.0,20.0" halign="center" font="bold 10.0pt Arial"><ValueBinding ord="station:|slot:/Iluminacion/GM02/NameMux9"><ObjectToString name="text" format="%out.value%" /></ValueBinding></Label><ImageButton layout="240.0,190.0,60.0,22.0" buttonStyle="none"><ActionBinding ord="station:|slot:/Iluminacion/GM02/SelR9/set" widgetEvent="actionPerformed" /></ImageButton><Picture layout="240.0,250.0,60.0,22.0" image="file:^Imagenes/The Harbor/Buttons/BotonNormal.png" scale="fitWidth" /><Label layout="240.0,250.0,60.0,20.0" halign="center" font="bold 10.0pt Arial"><ValueBinding ord="station:|slot:/Iluminacion/GM02/NameMux10"><ObjectToString name="text" format="%out.value%" /></ValueBinding></Label><ImageButton layout="240.0,250.0,60.0,22.0" buttonStyle="none"><ActionBinding ord="station:|slot:/Iluminacion/GM02/SelR10/set" widgetEvent="actionPerformed" /></ImageButton><Picture layout="240.0,310.0,60.0,22.0" image="file:^Imagenes/The Harbor/Buttons/BotonNormal.png" scale="fitWidth" /><Label layout="240.0,310.0,60.0,20.0" halign="center" font="bold 10.0pt Arial"><ValueBinding ord="station:|slot:/Iluminacion/GM02/NameMux11"><ObjectToString name="text" format="%out.value%" /></ValueBinding></Label><ImageButton layout="240.0,310.0,60.0,22.0" buttonStyle="none"><ActionBinding ord="station:|slot:/Iluminacion/GM02/SelR11/set" widgetEvent="actionPerformed" /></ImageButton><Picture layout="240.0,370.0,60.0,22.0" image="file:^Imagenes/The Harbor/Buttons/BotonNormal.png" scale="fitWidth" /><Label layout="240.0,370.0,60.0,20.0" halign="center" font="bold 10.0pt Arial"><ValueBinding ord="station:|slot:/Iluminacion/GM02/NameMux12"><ObjectToString name="text" format="%out.value%" /></ValueBinding></Label><ImageButton layout="240.0,370.0,60.0,22.0" buttonStyle="none"><ActionBinding ord="station:|slot:/Iluminacion/GM02/SelR12/set" widgetEvent="actionPerformed" /></ImageButton><Picture layout="240.0,430.0,60.0,22.0" image="file:^Imagenes/The Harbor/Buttons/BotonNormal.png" scale="fitWidth" /><Label layout="240.0,430.0,60.0,20.0" halign="center" font="bold 10.0pt Arial"><ValueBinding ord="station:|slot:/Iluminacion/GM02/NameMux13"><ObjectToString name="text" format="%out.value%" /></ValueBinding></Label><ImageButton layout="240.0,430.0,60.0,22.0" buttonStyle="none"><ActionBinding ord="station:|slot:/Iluminacion/GM02/SelR13/set" widgetEvent="actionPerformed" /></ImageButton><Picture layout="240.0,490.0,60.0,22.0" image="file:^Imagenes/The Harbor/Buttons/BotonNormal.png" scale="fitWidth" /><Label layout="240.0,490.0,60.0,20.0" halign="center" font="bold 10.0pt Arial"><ValueBinding ord="station:|slot:/Iluminacion/GM02/NameMux14"><ObjectToString name="text" format="%out.value%" /></ValueBinding></Label><ImageButton layout="240.0,490.0,60.0,22.0" buttonStyle="none"><ActionBinding ord="station:|slot:/Iluminacion/GM02/SelR14/set" widgetEvent="actionPerformed" /></ImageButton><Picture layout="240.0,550.0,60.0,22.0" image="file:^Imagenes/The Harbor/Buttons/BotonNormal.png" scale="fitWidth" /><Label layout="240.0,550.0,60.0,20.0" halign="center" font="bold 10.0pt Arial"><ValueBinding ord="station:|slot:/Iluminacion/GM02/NameMux15"><ObjectToString name="text" format="%out.value%" /></ValueBinding></Label><ImageButton layout="240.0,550.0,60.0,22.0" buttonStyle="none"><ActionBinding ord="station:|slot:/Iluminacion/GM02/SelR15/set" widgetEvent="actionPerformed" /></ImageButton><Picture layout="240.0,610.0,60.0,22.0" image="file:^Imagenes/The Harbor/Buttons/BotonNormal.png" scale="fitWidth" /><Label layout="240.0,610.0,60.0,20.0" halign="center" font="bold 10.0pt Arial"><ValueBinding ord="station:|slot:/Iluminacion/GM02/NameMux16"><ObjectToString name="text" format="%out.value%" /></ValueBinding></Label><ImageButton layout="240.0,610.0,60.0,22.0" buttonStyle="none"><ActionBinding ord="station:|slot:/Iluminacion/GM02/SelR16/set" widgetEvent="actionPerformed" /></ImageButton><Picture layout="430.0,190.0,60.0,22.0" image="file:^Imagenes/The Harbor/Buttons/BotonNormal.png" scale="fitWidth" /><Label layout="430.0,190.0,60.0,20.0" halign="center" font="bold 10.0pt Arial"><ValueBinding ord="station:|slot:/Iluminacion/GM02/NameMux17"><ObjectToString name="text" format="%out.value%" /></ValueBinding></Label><ImageButton layout="430.0,190.0,60.0,22.0" buttonStyle="none"><ActionBinding ord="station:|slot:/Iluminacion/GM02/SelR17/set" widgetEvent="actionPerformed" /></ImageButton><Picture layout="430.0,250.0,60.0,22.0" image="file:^Imagenes/The Harbor/Buttons/BotonNormal.png" scale="fitWidth" /><Label layout="430.0,250.0,60.0,20.0" halign="center" font="bold 10.0pt Arial"><ValueBinding ord="station:|slot:/Iluminacion/GM02/NameMux18"><ObjectToString name="text" format="%out.value%" /></ValueBinding></Label><ImageButton layout="430.0,250.0,60.0,22.0" buttonStyle="none"><ActionBinding ord="station:|slot:/Iluminacion/GM02/SelR18/set" widgetEvent="actionPerformed" /></ImageButton><Picture layout="430.0,310.0,60.0,22.0" image="file:^Imagenes/The Harbor/Buttons/BotonNormal.png" scale="fitWidth" /><Label layout="430.0,310.0,60.0,20.0" halign="center" font="bold 10.0pt Arial"><ValueBinding ord="station:|slot:/Iluminacion/GM02/NameMux19"><ObjectToString name="text" format="%out.value%" /></ValueBinding></Label><ImageButton layout="430.0,310.0,60.0,22.0" buttonStyle="none"><ActionBinding ord="station:|slot:/Iluminacion/GM02/SelR19/set" widgetEvent="actionPerformed" /></ImageButton><Picture layout="430.0,370.0,60.0,22.0" image="file:^Imagenes/The Harbor/Buttons/BotonNormal.png" scale="fitWidth" /><Label layout="430.0,370.0,60.0,20.0" halign="center" font="bold 10.0pt Arial"><ValueBinding ord="station:|slot:/Iluminacion/GM02/NameMux20"><ObjectToString name="text" format="%out.value%" /></ValueBinding></Label><ImageButton layout="430.0,370.0,60.0,22.0" buttonStyle="none"><ActionBinding ord="station:|slot:/Iluminacion/GM02/SelR20/set" widgetEvent="actionPerformed" /></ImageButton></CanvasPane>```
