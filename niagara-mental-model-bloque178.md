# Block 178 — nmodsreflow: cómo mapea puntos↔equipos (por CONTENCIÓN de driver, no por link) vs chihuahua

> **Reapertura acotada del focus `nmodsreflow`** (METHODOLOGY §8): responde una pregunta puntual del usuario —
> ¿cómo asocia Reflow los puntos de la station a sus equipos, dado que chihuahua debe linkearlos a mano con
> link mark? Decompilado build .75 + corroboración con el config vivo (estructura, no valores). READ-ONLY.
>
> Focus: **nmodsreflow** (reabierto para esta pregunta) — cruza con [Block 172]/[Block 173] (link mark de
> chihuahua). Corpus language: Spanish.
>
> Sources: `RT/util/PointHelper.java` (decompilado Vineflower, base
> `…/organized/nmodsreflow77/nmodsreflow77-rt/vineflower/com/niagaramods/nmodsreflow/`) + observación de la
> ESTRUCTURA del `config.json` vivo de Reflow (backup en scratchpad, `sha256 bf70f28f…`; se cita schema/conteos,
> nunca valores — SECRETS DISCIPLINE).
> Markers: `[CERT]` `file:line` decompilado · `[CERT-hw]` estructura observada en el config vivo · `[INFER]`.
> Capa 26.

---

## 178.1 — La pregunta

chihuahua asocia puntos a equipos con **wire-sheet links explícitos** (link mark / `BatchLinkEditor`, [Block 172];
serializados por `ChiLinkHelper`, [Block 173]): cada punto se linkea a un slot de `BChiUp`/`BChiCarcamo`/
`BChiDatalogger`. Es manual y por-punto. Reflow no hace eso. ¿Cómo obtiene los puntos de un equipo?

## 178.2 — La respuesta: `PointHelper` lee los puntos por CONTENCIÓN del driver `[CERT]`

Reflow apoya todo en el **modelo de driver estándar de Niagara**. La clase entera es `PointHelper` (50 líneas):

- `getPointsForDevice(BDevice device)` → `parseDeviceExtsForPoints(device.getDeviceExts())` `[CERT]`
  `RT/util/PointHelper.java:12-14`.
- `parseDeviceExtsForPoints(...)` recorre las **device extensions** del device y, para cada una: recolecta los
  `BControlPoint` hijos directos, y **recursa dentro de `BPointDeviceExt`** (la extensión "Points" que todo
  device de driver tiene) **y `BPointFolder`** (subcarpetas de puntos) `[CERT]` `RT/util/PointHelper.java:16-29`.
- Imports que lo delatan: `javax.baja.driver.BDevice`, `javax.baja.driver.point.BPointDeviceExt`,
  `javax.baja.driver.point.BPointFolder`, `javax.baja.control.BControlPoint` `[CERT]` `:5-9`.
- Variante genérica: `getPointsForComponent(BComponent)` recolecta `BControlPoint` hijos (shallow/deep) de
  cualquier componente `[CERT]` `:31-49`.

`[INFER]` **Traducción:** cuando en la station se crea un **device** (BACnet/Modbus/N2/…) bajo una network y se
le agregan puntos (por discovery/learn o a mano), esos puntos viven —por el modelo de driver— DENTRO del device,
en su extensión `Points` (`BPointDeviceExt` → `BPointFolder` → `BControlPoint`). Reflow simplemente **camina ese
árbol y toma todos los puntos**. Los puntos YA son hijos del device; no hay nada que linkear.

## 178.3 — Corolario en el config: el binding NO se guarda, se descubre `[CERT-hw]`

El `config.json` vivo de Reflow lo confirma por su ESTRUCTURA: `equipment` es un dict de **`{id: bool}`** (32
entradas, flags de habilitación), igual que `floorplans`/`histories`/`buildings` `[CERT-hw]` (schema del backup
`bf70f28f…`). En TODO el config hay solo **7 strings `slot:` / 7 `station:`** `[CERT-hw]`. `[INFER]` Es decir: el
config **no almacena** el mapa punto→equipo (no hay cientos de ORDs); solo dice qué equipos están habilitados/
posicionados. La membresía de puntos **se resuelve en vivo** desde el árbol de la station vía `PointHelper`
(§178.2). El binding vive en la **estructura de la station** (el device y sus puntos), no en Reflow.

## 178.4 — El contraste con chihuahua `[CERT]`

La diferencia es de MODELO DE COMPONENTE:

| | Reflow | chihuahua |
|---|---|---|
| Qué es el "equipo" | un **`BDevice`** de driver (o un componente contenedor) | un **componente custom** `BChiUp`/`BChiCarcamo`/`BChiDatalogger` (NO es `BDevice`) |
| Dónde viven los puntos | **dentro** del device (extensión `Points`, por contención) | fuera, en cualquier parte de la station |
| Cómo se asocian | **tree-walk** de la extensión Points (`PointHelper`) — automático | **wire-sheet BLinks** (link mark / `BatchLinkEditor`) — manual, por-punto ([Block 172]) |
| Qué se persiste | nada del binding (se descubre en vivo) | cada link, exportable a `chih-links.json` ([Block 173]) |

`[INFER]` **La raíz:** Reflow asume que el "equipo" ES un device de driver que **contiene** sus puntos, así que
los lee gratis. chihuahua modela el equipo como un **componente de dashboard propio** cuyos slots (`temp`,
`ampsX`, setpoints, protecciones — [Block 168]) se **alimentan por link** desde los puntos reales del driver.
Por eso chihuahua necesita link mark: sus `BChiUp` no son los devices, son una capa de dominio ENCIMA, y hay
que cablear los puntos hacia sus slots.

## 178.5 — Implicación práctica `[INFER]`

- **Para "ser como Reflow"** (evitar el link manual): chihuahua tendría que, o bien (a) hacer que sus equipos
  extiendan/envuelvan el `BDevice` y lean puntos por contención como `PointHelper`, o (b) resolver puntos por
  **convención de nombre/ORD** en `changed()`/`started()` en vez de por BLink — un "auto-bind" que busca el
  punto por path relativo al device y puebla los slots. Ambos eliminan el link mark a cambio de acoplar el
  modelo de dashboard al árbol de driver.
- **Trade-off:** el approach de Reflow es cero-configuración pero **plano** (toma TODOS los puntos del device,
  sin semántica de rol); el de chihuahua es explícito y **tipado** (cada slot sabe qué es: `tempSuministro`,
  `amps1`, `spEnfriamiento`), lo que habilita su lógica de protección/latch ([Block 168]) que Reflow no tiene.
  El link manual es el precio de la semántica de dominio.

## 178.6 — Connections

- **[Block 172]** — `BatchLinkEditor`: la herramienta que hace el link mark masivo en chihuahua (el trabajo que
  Reflow evita por contención).
- **[Block 173]** — `ChiLinkHelper`: persiste esos links (`chih-links.json`); Reflow no persiste binding.
- **[Block 168]** — los slots tipados de `BChiUp` que el link alimenta y que habilitan el control de dominio.
- **[Block 148]** — capa util de Reflow (donde vive `PointHelper`, antes no detallado).
- **[Block 177]** — comparación general chihuahua↔Reflow; este bloque profundiza el eje "mapeo de puntos".
- **Focus `nmodsreflow`** — reabierto para esta pregunta (§8), re-STOP tras responderla.
