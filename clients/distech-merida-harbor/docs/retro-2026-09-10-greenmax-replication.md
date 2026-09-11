# Retro — HARBOR GreenMAX: nombres de horario + replicación a todos los tableros (2026-09-10)

## Qué se logró
- **Nombres de horario editables** (`Horario{n}_Nombre` StringWritable) + edición desde el menú (botón invisible → Set de texto) + `NameMux{k}` (BStringSelect) para que el tablero muestre el nombre del horario elegido.
- **Replicación del rediseño a los 13 tableros operativos** (GM02, GM04–GM08, GM10–GM16; GM09 caído): quitar HOA + popups de calendario, conservar pill (`Relay[k].BO` valor) y LED (`Relay[k].BO` status), agregar selector `NameMux/SelR`.
- **Generador reusable** `gen_tablero.py` (auto-detecta posiciones de pills y número de circuitos, aplica el transform, valida XML, ajusta viewSize).
- **Leyenda "Horarios"** (solo lectura) en los 13 tableros para orientar qué número = qué nombre.

## Lecciones (qué costó y por qué)
1. **El bog manda sobre los nombres de archivo.** `GreenMAX07 TabAL8.px` NO es un segundo tablero de GM07: es la PxView de **GM08** (vive dentro de `GM08ilum`, 32 relays). Verificar `device → PxView → relays` en `config.bog` con `bog-nav slot`, no inferir del nombre.
2. **Evidencia en vivo del usuario > backup viejo.** Inferí de un backup (Sep 8) que los `SelR/SchedMux` iban relativos dentro de `GM0Xilum` y cambié la ruta; el usuario confirmó que la ruta **absoluta** `station:|slot:/Iluminacion/GM0X/` ya funcionaba. El backup precedía al build del piloto. Regla: `[CERT-live]` gana; no "corregir" algo que el usuario reporta funcionando sin verificarlo contra la estación real.
3. **Nombre de carpeta de control ≠ nombre del device.** El device puede traer cero (`GM010ilum`), pero la carpeta de control es **plana** (`Iluminacion/GM10`). Usar el cero rompió GM10.
4. **`file:^` para imágenes; `local:|foxs:|station:|slot:` solo para datos en vivo.** Prefijé una imagen con `local:|foxs:|` y estaba de más; todas las imágenes del Px usan `file:^`.
5. **LED = status del relé físico (`Relay[k].BO`), no del comando (`HorR{k}`).** Se revirtió al binding original porque refleja caídas de comunicación / override de campo; el comando no.
6. **Convención de bindings del Px del cliente:** `ValueBinding` + `ObjectToString format="%out.value%"` (no `BFormat`); botón invisible `ImageButton buttonStyle="none"` + `ActionBinding .../set` para abrir el diálogo de Set (patrón Hx-safe probado).

## Pendientes / parqueados
- **Construir el control por panel** en Workbench (`SelR/SchedMux/NameMux` + cutover) y validar en el navegador. Los `HorR{k}` ya existen en cada `GM0Xilum`; masters/HOA/nombres son compartidos.
- **Highlight verde del menú (Nivel 2):** el diseño (StatusDemux→isOn/isOff + `BotonVerde.png`) es correcto en lógica, pero el verde **no renderizó** en el navegador — depurar en vivo (¿asset no cargó? ¿estilo del botón toolBar tapa el fondo?). Gap relacionado: B853-G5.
- **GM07 TabAL8:** confirmado que es GM08; no hay segundo tablero pendiente para GM07.
- Ajuste cosmético: la leyenda "Horarios" (y≈140–250) puede rozar el borde superior del mapa en algunos paneles.

## Herramientas nuevas de la sesión
- `gen_tablero.py` (scratchpad): generador de tableros parametrizado por device/tag.
- `BotonVerde.png` (81×33, en `…/Imagenes/The Harbor/Buttons/`): fondo verde para highlight (pendiente de validar en Hx).
