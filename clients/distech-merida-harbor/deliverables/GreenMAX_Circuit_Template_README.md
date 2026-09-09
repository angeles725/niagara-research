# GreenMAX — Plantilla de 1 circuito (kitControl, sin módulo)

Archivo: `GreenMAX_Circuit_Template.bog`
Objetivo: la lógica de **1 circuito** del rediseño (4 horarios maestros + selector 1‑de‑4 + HOA Auto/Hand/Off),
lista para **copiar‑pegar** en cada circuito de los 16 tableros GreenMAX del Supervisor **HM_BMS**.

> Estación destino: EC‑Net 4 **4.3.58.18** (Distech). Workbench debe ser esa versión —
> instalador: `Distech Controls EC-Net 4 v4.3.58.18.4 Setup.exe`.

## Qué trae el template (8 componentes)
- **SchedMux** — `kitControl:BooleanSelect` (`numberValues=4`): el MUX 1‑de‑4 de horarios.
- **HoaMux** — `kitControl:BooleanSelect` (`numberValues=3`): Auto / Hand / Off.
- **ConstON / ConstOFF** — `kitControl:BooleanConst` (true / false), para Hand y Off.
- **SelR** — `NumericWritable` (facets 0–4): qué horario sigue el circuito. `0`=sigue al tablero, `1‑4`=horario fijo.
- **HoaR** — `NumericWritable` (facets 1–3): `1`=Auto, `2`=Hand (forzar ON), `3`=Off.
- **_README** — un StringConst con el recordatorio de cableado.

**Links internos ya cableados:** `SelR.out→SchedMux.select`, `SchedMux.out→HoaMux.inA`,
`ConstON.out→HoaMux.inB`, `ConstOFF.out→HoaMux.inC`, `HoaR.out→HoaMux.select`.

## Cómo usarlo en Workbench
1. **Crear una sola vez** los 4 horarios maestros: 4 `BooleanSchedule` en `Config/Iluminacion/Horarios/Horario1..4`.
2. **Abrir el template:** `File ▸ Open ▸ Open Bog` y selecciona `GreenMAX_Circuit_Template.bog` (o arrástralo).
   Copia la carpeta `GreenMAX_Circuit_Template` al `Palette`/portapapeles.
3. **Pegar** el contenido dentro del folder del circuito (junto a su `HorR{k}` / `Relay[k].BO`).
4. **Cablear los 4 enlaces EXTERNOS** que faltan (no vienen en el template porque apuntan a componentes de fuera):
   - `Horario1.out → SchedMux.inA`
   - `Horario2.out → SchedMux.inB`
   - `Horario3.out → SchedMux.inC`
   - `Horario4.out → SchedMux.inD`
   - `HoaMux.out → HorR{k}.in10`   ← el writable que ya existe del circuito
5. **Cutover:** al conectar `HoaMux.out → HorR{k}.in10`, **quita el link viejo** `R{k}.out → HorR{k}.in10`
   en el mismo paso (que `in10` tenga un solo escritor). No toques `in16` (fallback de comunicación).
6. Ajusta `SelR` y `HoaR` (valores por defecto) y verifica que el relé responde.

## Selección de dos niveles (opcional)
Si quieres "el circuito sigue al tablero salvo override": deja `SelR` en `0` y agrega, una vez por tablero,
un `SelPanel` (NumericWritable 1‑4) + `Equal(SelR,0)` + `NumericSelect`/`EnumSwitch` que entregue `SelPanel`
cuando `SelR=0`, o `SelR` cuando no; su salida va a `SchedMux.select` en lugar del link directo de `SelR`.

## Aviso (importante)
Este `.bog` está **construido y validado estructuralmente** (parseo + links OK con `bog-nav`), pero **no se
ha probado la importación en un Workbench real** (yo no tengo Workbench). Punto a verificar (gap B847‑G1):
el link `SelR.out (Numeric) → SchedMux.select (Enum)`. Si Workbench rechaza esa conversión al importar,
**borra los 2 links a `select`** y re‑enlaza con un bloque de conversión Numeric→Enum, o usa `EnumWritable`
para `SelR`/`HoaR`. Todo lo demás (los links booleanos y los bloques) es estándar.
