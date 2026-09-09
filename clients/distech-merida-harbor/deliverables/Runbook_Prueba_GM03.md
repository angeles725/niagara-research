# Runbook de prueba — rediseño de horarios en 1 tablero (GM03)

Objetivo: validar en un solo circuito real la lógica **4 horarios + selector + HOA** (template kitControl)
antes de replicarla en los 16 tableros. Tablero elegido: **GM03** (el más chico, 7 circuitos).

> **Estación:** Supervisor **HM_BMS**, EC-Net 4 **4.3.58.18**. Workbench = `Distech Controls EC-Net 4 v4.3.58.18.4 Setup.exe`.
> **Ruta base GM03:** `station:|slot:/Drivers/NiagaraNetwork/HM_Central/points/GM03ilum/`
> **Circuito de prueba:** **R1** (relé físico `Relay[1].BO`).

## GM03 tal como está hoy (verificado del backup)
- Circuitos con índices **1, 2, 3, 4, 5, 6, 17** (no es 1–7; ojo con el 17).
- Cada circuito: `R{k}` (BooleanSchedule) → `HorR{k}` (BooleanWritable) `.in10` → el JACE lo baja a BACnet `Relay[k].BO.in10`.
- Descripciones `labelR{k}` = *"Pendiente"* (este tablero no se terminó de etiquetar).

---

## Paso 0 — Preparación (no saltar)
1. **Respaldo primero**: `Backup Station` del HM_BMS (ver procedimiento B838). Es la red de seguridad.
2. Conecta Workbench 4.3 a la estación. Abre `GM03ilum` en **Wire Sheet**.
3. **Anota el estado actual** de R1: abre `HorR1` (Property Sheet) y confirma que `in10` viene de `R1.out`
   y que `in16` es el fallback. Anota el valor actual de `Relay[1].BO`.

## Paso 1 — Crear los 4 horarios maestros (una sola vez)
En `station:|slot:/Config/` crea un folder `Iluminacion/Horarios/` y dentro 4 `BooleanSchedule`:
- **Horario1** — todo el día ON (para ver ON claro).
- **Horario2** — todo el día OFF.
- **Horario3** — 08:00–18:00 L–V.
- **Horario4** — 18:00–24:00 L–D.

(Patrones distintos a propósito, para distinguir cuál está mandando.)

## Paso 2 — Importar el template en R1
1. `File ▸ Open ▸ Open Bog` → `GreenMAX_Circuit_Template.bog`.
2. Copia la carpeta `GreenMAX_Circuit_Template` y **pégala dentro de `GM03ilum`**. Renómbrala `Test_R1` (opcional).
3. Verás: `SchedMux`, `HoaMux`, `ConstON`, `ConstOFF`, `SelR`, `HoaR` con sus 5 links internos ya hechos.

## Paso 3 — Cablear los 4 enlaces externos + cutover
En el Wire Sheet, arrastra estos links:
1. `Horario1.out → SchedMux.inA`
2. `Horario2.out → SchedMux.inB`
3. `Horario3.out → SchedMux.inC`
4. `Horario4.out → SchedMux.inD`
5. `HoaMux.out → HorR1.in10`
6. **Cutover:** **borra** el link viejo `R1.out → HorR1.in10` (para que `in10` tenga un solo escritor).
   **No toques** `in16`.

## Paso 4 — Valores iniciales
- `SelR` = **1** (sigue Horario1).
- `HoaR` = **1** (Auto).

## Paso 5 — Matriz de prueba (observa `HorR1.out` y `Relay[1].BO`)

| # | Acción | Esperado en `HorR1` / `Relay[1].BO` |
|---|--------|-------------------------------------|
| 1 | `HoaR=1` (Auto), `SelR=1` | Sigue **Horario1** → ON |
| 2 | `SelR=2` | Sigue **Horario2** → OFF |
| 3 | `SelR=3` | Sigue **Horario3** (ON solo 08–18 L–V) |
| 4 | `SelR=4` | Sigue **Horario4** (ON solo 18–24) |
| 5 | `HoaR=2` (Hand) | **ON forzado**, sin importar `SelR` ni el horario |
| 6 | `HoaR=3` (Off) | **OFF forzado** |
| 7 | `HoaR=1` (Auto) | Vuelve a obedecer el horario seleccionado |
| 8 | En cada paso | `Relay[1].BO` (estado del relé físico) **espeja** a `HorR1` |

Para forzar valores usa **right-click ▸ Actions ▸ Set** en `SelR`/`HoaR`, o el Property Sheet.

## Paso 6 — Validar el punto de riesgo (gap B847-G1)
Al pegar/cablear, confirma que el link **`SelR.out` (Numeric) → `SchedMux.select` (Enum)** fue aceptado y que
`select` cambia con `SelR`. **Si Workbench lo rechaza** (o `select` no se mueve):
- Borra los 2 links a `select` (`SelR→SchedMux.select` y `HoaR→HoaMux.select`).
- Re-enlaza con un bloque de conversión Numeric→Enum, **o** cambia `SelR`/`HoaR` a `EnumWritable` con rango 1–4 / 1–3.
- Anota cuál funcionó (define el patrón para los otros 348 circuitos).

## Paso 7 — Rollback (si algo falla)
1. Borra el link `HoaMux.out → HorR1.in10`.
2. Vuelve a cablear `R1.out → HorR1.in10` (el original).
3. Borra la carpeta `Test_R1`.
4. `HorR1` queda como estaba. (Si dudas, restaura el backup del Paso 0.)

## Criterios de aceptación (para dar por buena la lógica)
- [ ] Los 4 estados de `SelR` cambian el horario que obedece `HorR1`.
- [ ] `Hand` fuerza ON y `Off` fuerza OFF por encima del horario; `Auto` lo devuelve.
- [ ] `Relay[1].BO` sigue a `HorR1` en todos los casos.
- [ ] El link `Numeric→Enum` del `select` quedó resuelto (y anotado cómo).
- [ ] Rollback probado: R1 vuelve a su schedule original sin residuos.

Con esto validado en R1, replicas el patrón en R2–R6 y R17 de GM03, y luego a los demás tableros
(GM02–GM16 con `HorR{k}`; GM01 con `SchdlGM01 R{k}`).
