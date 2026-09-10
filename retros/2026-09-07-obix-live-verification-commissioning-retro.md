# Retro — oBIX como herramienta de verificación EN VIVO durante commissioning

Date: 2026-09-07
Author: Opus 4.8 (research session, con Cristian, equipo multi-sesión con @conexion)
Scope: usar oBIX (lectura/escritura de slots de la station viva) como instrumento de
investigación y verificación mientras el operador cablea en Workbench. Se construyó
`tools/obix-nav.py` y se corrió un loop "tú cableas / yo leo y te digo qué falta" sobre el
JACE-9000 de PANCCADIA. Complementa el toolbelt del kit research-sdd (las TRES fuentes +
disciplina de evidencia [CERT]).

## Por qué esta retro

El protocolo research-sdd está pensado para investigar el CORPUS/CÓDIGO. Esta sesión abrió un
modo nuevo: **verificar el sistema VIVO** (una station corriendo) como fuente de evidencia de
primer nivel, en paralelo al operador. Salieron técnicas y trampas que conviene dejar escritas
para reusarlas.

## Qué se produjo / qué funcionó

- **`tools/obix-nav.py`** (commiteado, niagara-research 76244929c): lee la fachada
  (`Services/DashboardService`) vs el rt (`Programacion/…`) por **oBIX Batch** en una sola
  llamada; auto-detecta el mapeo evap→unidad por `coilTemp`; reporta OK/MISSING/DIFF por slot.
  Convirtió un loop de decenas de `curl` sueltos en una lectura estructurada de ~1s.
- **oBIX SÍ expone los BLinks.** Un componente exporta cada link como
  `<obj is="baja:Link"> display="Indirect: <origen>.<slot> → slot:/<destino>.<slot>"`. Eso
  permite leer el CABLEADO real (no solo valores) y decir "esta entrada viene de X" —
  crucial para "¿está bien conectado?".
- **Cruce de evidencia vivo ↔ código.** Se confirmó por valor (termostato `no_EffTempMode`
  ordinal Cool=1) y en paralelo por código [CERT] (`applyFanRunMode`/`enterDefrost`/
  `exitDefrost`: el modo continuous NO pisa el deshielo). La afirmación al operador solo se
  dio cuando VIVO y CÓDIGO coincidieron.
- **Detección de bugs por lectura de valor.** El `valveMode` (0/1/2) entrando crudo a un `Or`
  booleano (→ `!=0`=true) se cazó leyendo `Or.inA` vs `valveMode` en vivo, antes de que el
  operador presionara "Apagar" y abriera la válvula.

## Trampas y disciplina (para la próxima)

- **FALLA DE HERRAMIENTA ≠ CERO / DATO.** `bog-nav.py links --src` devolvía vacío (bug del
  flag); el `links` plano sí traía 309 links. Estuve a punto de concluir "no hay links" por un
  flag roto. Regla del protocolo confirmada: una fuente que no se pudo consultar bloquea el
  hallazgo negativo — verificar con otra vía antes de afirmar ausencia.
- **El handle no siempre resuelve por oBIX.** oBIX navega por ruta, no por `h:xxxx`. Para
  ubicar bloques nuevos hubo que listar carpetas (estaban en `io34_5_2/points` como Or5-9),
  no adivinar. El display del link da el destino por ruta y el origen por handle+slot; el
  nombre del slot origen (`evap1Setpoint`, `valveMode`) suele bastar para identificar.
- **Escribir por oBIX es acción de control.** oBIX puede hacer PUT de valores (setpoints,
  modos), pero sobre una station VIVA es una mutación de control: se ofreció, no se ejecutó
  sin OK explícito. Los LINKS (BLinks) NO se pueden crear por oBIX — eso es Workbench/Fox.
- **Túnel de vida corta.** El cert cloudflared dura ~4 min; el túnel se reconecta on-demand
  antes de cada verificación en vez de mantenerlo colgado (más estable). El secreto
  (`OBIX_PASS`) se leyó del `config.env` de la mini-PC a un archivo del scratchpad aislado,
  nunca al repo ni a memoria.

## La lección

**El sistema vivo es una cuarta fuente de evidencia, y oBIX es la lente — pero con la misma
disciplina que el corpus:** lee el cableado real (no solo valores), cruza VIVO ↔ CÓDIGO [CERT]
antes de afirmar, trata una herramienta rota como "no verificado" (no como cero), y separa
LEER (libre) de ESCRIBIR (acción de control que se ofrece y se confirma). `obix-nav.py` queda
como el instrumento reusable para auditar fachada↔rt de cualquier station PANCCADIA.

## Connections

- [[panccadia-jace-live-obix-audit]] · [[panccadia-viewer-3d-contract-build-4-3]] ·
  el bloque de acceso oBIX/túnel de @conexion (tunnel/clientes/Leon-Guanajuato/Pancaddia).
