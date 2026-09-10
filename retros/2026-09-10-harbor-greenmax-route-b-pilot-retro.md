# Retro — HARBOR GreenMAX Route B: el pilot en vivo y dos falsos negativos que casi tuercen la sesión

> Sesión 2026-09-10, focus harbor-greenmax-lighting. Se armó y se hizo cutover en vivo de un circuito
> (GM02 c1) del rediseño de iluminación Route B, operador-en-el-loop (él ejecuta en Workbench, yo guío y
> capturo). Resultado en el bloque B851. Este retro es el §18 de esa iteración.

## Por qué esta retro
El trabajo salió bien y quedó `[CERT-live]`, pero dos veces afirmé algo con seguridad basándome en un
resultado vacío de una herramienta, y las dos veces estaba mal. Vale más registrar eso que el éxito.

## Qué se produjo / qué funcionó
- **Cadena real verificada + pilot end-to-end:** `Rk (schedule por circuito) → HorRk.in10 → proxy JACE →
  Relay[k].BO.in10 → relé`, cutover sin parpadeo. Confirmó la tesis de B841 y corrigió el modelo de B847.
- **Verify-before-claim cuando lo hice:** antes de decir "sí se puede escribir el nombre" verifiqué
  `BStringWritable` en el código; antes de mandar bloques de la paleta verifiqué `kitControl` real. Ahí bien.
- **Ejecutar UNO antes de replicar 288:** el pilot destapó que el modelo del cliente es HOA-por-horario
  (no por circuito) y 5 horarios (no 4). Descubrirlo en el circuito 1 y no después de 288 fue la ganancia.
- **Patrón operador-en-el-loop:** guiar paso a paso + pedir la lectura concreta (Relation Sheet, valor de
  slot) y convertirla en evidencia citada. Funcionó limpio para un gap requires-execution.

## Trampas y disciplina (para la próxima)
1. **Un resultado vacío de una herramienta NO es ausencia.** `bog-nav links --to/--from <handle>` devolvió
   `(no matching links)` para links que SÍ existían (bug: matcheaba el needle solo contra rutas, no contra
   handles). Con eso concluí y le dije al operador "la cadena no existe, párate". Lo corrigió su Relation
   Sheet en vivo + el dump global `--slot-any` (que sí los mostró). La metodología ya lo dice —
   FALLA DE HERRAMIENTA ≠ CERO REAL, y `live > lectura de herramienta` — y aun así volvió a pasar. Regla
   reforzada: **antes de afirmar "no existe" o de decir STOP, corroborá con un segundo método.**
2. **No declares una capacidad "no disponible" al primer intento fallido.** Llamé `mem_save` a secas, falló,
   y dije "engram no está disponible". El tool real es `mcp__plugin_engram_engram__mem_save`, deferred (se
   carga con ToolSearch). Era mi nombre equivocado, no engram caído. Regla: si un tool "no existe", revisá
   el nombre completo / que sea deferred antes de reportar que el subsistema está caído.
3. **El diseño `[INFER]` es hipótesis hasta que corre.** B841/B846/B847 eran diseño; el pilot los volvió
   `[CERT-live]` pero también los corrigió. Bien haber marcado esos claims como `[INFER]` desde el principio.

## La lección
Los dos errores fueron el mismo error: **tratar un cero de herramienta como un hecho.** El antídoto ya está
en el protocolo (corroborar, `live > tool`), pero la presión de ir rápido en vivo lo saltó. Cuando la
afirmación va a **frenar al operador** o negar la existencia de algo, el costo de un segundo método es cero
comparado con el de mandarlo por el camino equivocado.

## Deltas propuestos (para revisión humana, no aplico al kit)
- **Ya aplicado (tool del proyecto):** `bog-nav.py` — `--to/--from` aceptan `h:handle`, con caso de
  regresión en `selftest`. Falta commit.
- **Kit/entorno (nota):** dejar registrado que en este harness los tools de engram son deferred y llevan
  prefijo `mcp__plugin_engram_engram__` — para no volver a concluir "engram caído" por un nombre corto.

## Connections
- **B851** — el bloque que este retro cierra (pilot + cadena verificada + fix de bog-nav).
- **B847 / B841 / B846** — diseño corregido/confirmado por el pilot.
- `retros/2026-09-05-bog-nav-module-find-tools-retro.md` — origen de bog-nav; este retro le suma el bug de handle.
