# Manual de comisionamiento en campo — Cuartos fríos (módulo ColdRoomPan)

> Guía práctica para el técnico de controles que arma las estaciones Niagara reales.
> Documenta, cuarto por cuarto (Cuarto 1–4), cómo ensamblar los bloques del módulo
> `ColdRoomPan`, la numeración, la configuración y **todo cableado de entrada/salida**.
>
> **Documentos relacionados (no se duplican aquí):**
> - Diseño y comportamiento interno del módulo: `docs/cold-room-module-design.md`.
> - Cómo se construyó el módulo (desarrollo): `docs/how-to-create-coldroom-module.md`.
> - Flujo edición→build→firma→despliegue: `docs/module-dev-workflow.md`.
>
> Este manual asume que el módulo firmado `coldRoomPan-rt.jar` ya está instalado en
> `!modules` de la estación y que los puntos físicos (proxy points) ya existen bajo su
> dispositivo en el árbol de drivers.

---

## 1. Introducción, convención de nombres y numeración

### 1.1 Los tres tipos del módulo

El módulo aporta tres tipos de componente (el nombre en Workbench, en español, va entre paréntesis):

| Tipo | Nombre en Workbench | Rol |
|---|---|---|
| `ColdRoom` | **Cuarto frio** | Contenedor por cuarto. Maneja la lógica de enfriamiento. |
| `EvaporatorUnit` | **Evaporadora** | Una por evaporadora física. Secuencia válvula→evaporadora. |
| `DefrostController` | **Control de deshielo** | Solo Cuarto 3. Coordina el deshielo entre unidades. |

### 1.2 Numeración

- **Un `ColdRoom` por cuarto**, nombrado `ColdRoom_1`, `ColdRoom_2`, `ColdRoom_3`, `ColdRoom_4`.
- **Dentro** de cada `ColdRoom`, las evaporadoras se nombran `EvaporatorUnit_1`, `EvaporatorUnit_2`, …
  numeradas **por cuarto** (cada cuarto arranca en `_1`).
- **Cuarto 3** además lleva un `DefrostController_1` dentro del `ColdRoom_3`.

### 1.3 Modelo de CONTENCIÓN (leer antes de cablear)

Este es el punto clave del módulo y la fuente de errores más común:

- Las **evaporadoras** y el **control de deshielo** van **DENTRO** del `ColdRoom`, como hijos
  (children) del componente. No son bloques hermanos sueltos en `/Config`.
- El `ColdRoom` **maneja internamente** a sus hijos: calcula la demanda de frío por zona,
  aplica el escalonamiento y ordena a cada `EvaporatorUnit` que arranque o pare.
- Por lo tanto **NO se cablea bloque-contra-bloque**. No arrastres un link desde el
  `ColdRoom` hacia la `EvaporatorUnit`: esa relación ya existe por contención.
- Tu único trabajo de cableado (BLink) es **enlazar los puntos físicos** del árbol de drivers
  con los slots del bloque:
  - **Entradas** (sensores) → slots de entrada del bloque (enlace simple, solo lectura).
  - **Salidas** del bloque → **writables** físicos, escribiendo en el **nivel de prioridad `in8`**
    (nivel del programa; deja los niveles superiores para overrides manuales/emergencia).

### 1.4 Esquema de nombres para los puntos físicos (proxy points)

Usa un patrón consistente para poder ubicar cualquier punto de un vistazo. Convención sugerida:

- **Zonas (sensores de temperatura de zona):** `Zona1_C1`, `Zona2_C1` (zona N del cuarto N).
- **Sensor de la evaporadora (temp de serpentín):** `Coil_C1E1` (coil, cuarto 1, evaporadora 1).
- **Válvula (writable):** `Valv_C1E1`.
- **Evaporadora / ventilador (writable):** `Evap_C1E1`.
- **Resistencia de deshielo (writable, solo Cuarto 3):** `Resist_C3E1`.
- **Sensor de temp de resistencia (solo Cuarto 3, opcional):** `TempResist_C3E1`.

`C` = cuarto, `E` = evaporadora. Así `Valv_C3E2` es la válvula de la evaporadora 2 del cuarto 3.

### 1.5 Alarmas visuales (nota breve)

Las alarmas (temp evaporadora alta/baja, temp del cuarto alta, ventilador detenido) son **solo
visuales**: no intervienen en la lógica de control. Se configuran como **extensiones de punto**
(`BAlarmSourceExt`) sobre los puntos de temperatura físicos, **no** como slots de estos bloques.
Los slots `Limite alarma alta del cuarto`, `Limite alarma alta` y `Limite alarma baja` que verás
en las tablas de configuración **solo guardan el umbral**; el ruteo y la notificación de la alarma
viven en la extensión del punto. El armado completo de alarmas es un documento aparte (futuro);
aquí solo se indica dónde se cargan esos límites.

---

## 2. Pasos generales (aplican a todos los cuartos)

Sigue esta secuencia en cada cuarto; el detalle específico está en las secciones 3–6.

1. **Crear el cuarto:** arrastra un `ColdRoom` (Cuarto frio) desde la paleta del módulo a
   `/Config` (por ejemplo `/Config/CuartosFrios/`). Renómbralo `ColdRoom_N`.
2. **Agregar las evaporadoras DENTRO del cuarto:** arrastra una `EvaporatorUnit` (Evaporadora)
   **sobre** el `ColdRoom_N` (queda como hijo). Renómbrala `EvaporatorUnit_1`. Repite `_2`, `_3`
   según el número de evaporadoras del cuarto.
3. **(Solo Cuarto 3) Agregar el deshielo:** arrastra un `DefrostController` (Control de deshielo)
   **dentro** del `ColdRoom_3`. Renómbralo `DefrostController_1`.
4. **Fijar el modo de etapas** del cuarto (`Modo de etapas`): `Por etapas` (Cuarto 1) o `Simple`
   (Cuartos 2, 3, 4).
5. **Configurar consigna y diferenciales** del cuarto: `Consigna`, `Diferencial arriba`,
   `Diferencial abajo`.
6. **Configurar cada evaporadora:** `Retardo de arranque` (retardo válvula→evaporadora) y
   `Tiene deshielo` (Sí solo en Cuarto 3). Cargar los límites de alarma si corresponde.
7. **(Solo Cuarto 3) Configurar el deshielo:** `Modo`, `Intervalo`/`Entrada de horario`,
   `Duracion`, `Retardo de escalonamiento`, y opcionalmente `Terminar por temp resistencia` +
   `Umbral temp resistencia`.
8. **Enlazar los puntos físicos** a los slots del bloque (tablas de ENTRADAS y SALIDAS de cada
   cuarto). Las salidas escriben en el nivel de prioridad `in8` del writable.

> **Consejo de cableado masivo:** en comisionamiento conviene usar el `BBatchLinkEditor` con
> `checkLink` (dry-run) para enlazar en lote y mantener los links estables por handle/tag, de modo
> que re-direccionar un dispositivo toque solo el proxyExt y no la lógica.

---

## 3. Cuarto 1 — 3 evaporadoras, 2 zonas, sin deshielo

### 3.1 Equipo

- **3 evaporadoras** (con 3 válvulas).
- **2 zonas** (2 sensores de temperatura de zona), consigna única compartida.
- **Sin deshielo.**

### 3.2 Estructura de bloques

```
/Config/CuartosFrios/
└── ColdRoom_1            (Cuarto frio)  — Modo de etapas = Por etapas
    ├── EvaporatorUnit_1  (Evaporadora)  — sigue Zona 1
    ├── EvaporatorUnit_2  (Evaporadora)  — sigue Zona 1 O Zona 2
    └── EvaporatorUnit_3  (Evaporadora)  — sigue Zona 2
```

### 3.3 Configuración

| Bloque | Slot | Valor |
|---|---|---|
| ColdRoom_1 | Modo de etapas | **Por etapas** |
| ColdRoom_1 | Consigna | consigna del cuarto (ej. -18 °C) |
| ColdRoom_1 | Diferencial arriba | según proyecto (ej. 1.0) |
| ColdRoom_1 | Diferencial abajo | según proyecto (ej. 1.0) |
| ColdRoom_1 | Limite alarma alta del cuarto | umbral de alarma de temp del cuarto (solo dato para la alarma visual) |
| EvaporatorUnit_1 | Retardo de arranque | **2 s** |
| EvaporatorUnit_1 | Tiene deshielo | **No** |
| EvaporatorUnit_2 | Retardo de arranque | **2 s** |
| EvaporatorUnit_2 | Tiene deshielo | **No** |
| EvaporatorUnit_3 | Retardo de arranque | **2 s** |
| EvaporatorUnit_3 | Tiene deshielo | **No** |
| EvaporatorUnit_1..3 | Limite alarma alta / Limite alarma baja | umbrales de alarma de temp de serpentín (solo dato) |

> `Salida resistencia` y `Temp resistencia` **no se usan** en este cuarto (no hay deshielo).

### 3.4 Tabla de ENTRADAS (sensor físico → slot del bloque)

| Punto físico | → Slot destino |
|---|---|
| `Zona1_C1` | ColdRoom_1 · **Zona 1** |
| `Zona2_C1` | ColdRoom_1 · **Zona 2** |
| `Coil_C1E1` | EvaporatorUnit_1 · **Temp evaporadora** |
| `Coil_C1E2` | EvaporatorUnit_2 · **Temp evaporadora** |
| `Coil_C1E3` | EvaporatorUnit_3 · **Temp evaporadora** |

### 3.5 Tabla de SALIDAS (slot del bloque → writable físico, prioridad `in8`)

| Slot origen | → Writable físico (nivel `in8`) |
|---|---|
| EvaporatorUnit_1 · **Salida valvula** | `Valv_C1E1` |
| EvaporatorUnit_1 · **Salida evaporadora** | `Evap_C1E1` |
| EvaporatorUnit_2 · **Salida valvula** | `Valv_C1E2` |
| EvaporatorUnit_2 · **Salida evaporadora** | `Evap_C1E2` |
| EvaporatorUnit_3 · **Salida valvula** | `Valv_C1E3` |
| EvaporatorUnit_3 · **Salida evaporadora** | `Evap_C1E3` |

### 3.6 Cómo se comporta

Escalonamiento (`Modo de etapas = Por etapas`) con consigna compartida:

- **Evaporadora 1** sigue la demanda de **Zona 1**.
- **Evaporadora 3** sigue la demanda de **Zona 2**.
- **Evaporadora 2** arranca con **Zona 1 O Zona 2** (cualquiera de las dos que pida frío).

En cada evaporadora, al pedir frío se **abre la válvula primero** y, tras el `Retardo de arranque`
(2 s), **arranca la evaporadora**. Al parar, primero se detiene la evaporadora y luego cierra la
válvula.

---

## 4. Cuarto 2 — 1 evaporadora, 1 zona, sin deshielo

### 4.1 Equipo

- **1 evaporadora** (1 válvula).
- **1 zona.**
- **Sin deshielo.**

### 4.2 Estructura de bloques

```
/Config/CuartosFrios/
└── ColdRoom_2            (Cuarto frio)  — Modo de etapas = Simple
    └── EvaporatorUnit_1  (Evaporadora)
```

### 4.3 Configuración

| Bloque | Slot | Valor |
|---|---|---|
| ColdRoom_2 | Modo de etapas | **Simple** |
| ColdRoom_2 | Consigna | consigna del cuarto |
| ColdRoom_2 | Diferencial arriba | según proyecto |
| ColdRoom_2 | Diferencial abajo | según proyecto |
| ColdRoom_2 | Limite alarma alta del cuarto | umbral de alarma de temp del cuarto (solo dato) |
| EvaporatorUnit_1 | Retardo de arranque | según proyecto (ej. 2 s) |
| EvaporatorUnit_1 | Tiene deshielo | **No** |
| EvaporatorUnit_1 | Limite alarma alta / Limite alarma baja | umbrales de temp de serpentín (solo dato) |

> **Zona 2 no se usa** en este cuarto: deja el slot `Zona 2` sin enlazar.
> `Salida resistencia` y `Temp resistencia` **no se usan**.

### 4.4 Tabla de ENTRADAS

| Punto físico | → Slot destino |
|---|---|
| `Zona1_C2` | ColdRoom_2 · **Zona 1** |
| `Coil_C2E1` | EvaporatorUnit_1 · **Temp evaporadora** |

### 4.5 Tabla de SALIDAS (prioridad `in8`)

| Slot origen | → Writable físico (nivel `in8`) |
|---|---|
| EvaporatorUnit_1 · **Salida valvula** | `Valv_C2E1` |
| EvaporatorUnit_1 · **Salida evaporadora** | `Evap_C2E1` |

### 4.6 Cómo se comporta

Modo simple: cuando **Zona 1** pide frío, arranca la evaporadora (abre válvula, luego evaporadora
tras el retardo). Sin escalonamiento ni segunda zona.

---

## 5. Cuarto 3 — 2 evaporadoras, 1 zona, CON deshielo

### 5.1 Equipo

- **2 evaporadoras** (2 válvulas), ambas arrancan con la misma zona.
- **1 zona.**
- **Con deshielo por resistencias** (una resistencia por evaporadora), coordinado por el
  `DefrostController_1`.

### 5.2 Estructura de bloques

```
/Config/CuartosFrios/
└── ColdRoom_3               (Cuarto frio)  — Modo de etapas = Simple
    ├── EvaporatorUnit_1     (Evaporadora)  — Tiene deshielo = Sí
    ├── EvaporatorUnit_2     (Evaporadora)  — Tiene deshielo = Sí
    └── DefrostController_1  (Control de deshielo)
```

### 5.3 Configuración

| Bloque | Slot | Valor |
|---|---|---|
| ColdRoom_3 | Modo de etapas | **Simple** |
| ColdRoom_3 | Consigna | consigna del cuarto |
| ColdRoom_3 | Diferencial arriba | según proyecto |
| ColdRoom_3 | Diferencial abajo | según proyecto |
| ColdRoom_3 | Limite alarma alta del cuarto | umbral de alarma de temp del cuarto (solo dato) |
| EvaporatorUnit_1 | Retardo de arranque | según proyecto |
| EvaporatorUnit_1 | **Tiene deshielo** | **Sí** |
| EvaporatorUnit_2 | Retardo de arranque | según proyecto |
| EvaporatorUnit_2 | **Tiene deshielo** | **Sí** |
| EvaporatorUnit_1..2 | Limite alarma alta / Limite alarma baja | umbrales de temp de serpentín (solo dato) |

Parámetros del deshielo (`DefrostController_1`):

| Slot | Valor |
|---|---|
| **Modo** | `Intervalo` u `Horario` (según proyecto) |
| **Intervalo** | periodo entre deshielos (solo si Modo = Intervalo), ej. cada 6 h |
| **Entrada de horario** | (solo si Modo = Horario) enlazar un `BooleanSchedule` — ver 5.6 |
| **Duracion** | duración máxima del deshielo |
| **Retardo de escalonamiento** | **4 min** (evita que ambas unidades entren en deshielo a la vez) |
| **Terminar por temp resistencia** | opcional: `Sí` para terminar también por temperatura |
| **Umbral temp resistencia** | temperatura de fin de deshielo (si el anterior = Sí) |

### 5.4 Tabla de ENTRADAS

| Punto físico | → Slot destino |
|---|---|
| `Zona1_C3` | ColdRoom_3 · **Zona 1** |
| `Coil_C3E1` | EvaporatorUnit_1 · **Temp evaporadora** |
| `Coil_C3E2` | EvaporatorUnit_2 · **Temp evaporadora** |
| `TempResist_C3E1` | EvaporatorUnit_1 · **Temp resistencia** (solo si se usa terminación por temp) |
| `TempResist_C3E2` | EvaporatorUnit_2 · **Temp resistencia** (solo si se usa terminación por temp) |

> Los sensores de temp de resistencia solo son necesarios si `Terminar por temp resistencia = Sí`.
> Si el deshielo termina solo por `Duracion`, no hace falta enlazar `Temp resistencia`.

### 5.5 Tabla de SALIDAS (prioridad `in8`)

| Slot origen | → Writable físico (nivel `in8`) |
|---|---|
| EvaporatorUnit_1 · **Salida valvula** | `Valv_C3E1` |
| EvaporatorUnit_1 · **Salida evaporadora** | `Evap_C3E1` |
| EvaporatorUnit_1 · **Salida resistencia** | `Resist_C3E1` |
| EvaporatorUnit_2 · **Salida valvula** | `Valv_C3E2` |
| EvaporatorUnit_2 · **Salida evaporadora** | `Evap_C3E2` |
| EvaporatorUnit_2 · **Salida resistencia** | `Resist_C3E2` |

### 5.6 Si el Modo del deshielo es Horario

Crea un `BooleanSchedule` (WebScheduler nativo) en la estación con los horarios de deshielo del
cuarto, y **enlázalo** al slot `Entrada de horario` del `DefrostController_1`. El operador edita
esos horarios desde la vista de agenda; no se tocan los bloques.

### 5.7 Cómo se comporta

- En operación normal, ambas evaporadoras siguen la demanda de **Zona 1** (modo Simple).
- El `DefrostController_1` **coordina el deshielo**: cuando toca deshelar (por Intervalo o por
  Horario), la secuencia por unidad es **cerrar válvula → parar evaporadora → energizar
  resistencia**. Termina por `Duracion` o, si está activo, por `Temp resistencia ≥ Umbral`.
- **Interbloqueo:** nunca hay dos unidades en deshielo a la vez. Si una unidad está deshelando y
  la otra ya toca, la segunda **espera**; cuando la primera vuelve a operación normal, arranca el
  `Retardo de escalonamiento` (4 min) y recién entonces la segunda inicia su deshielo. La unidad
  que no está deshelando sigue enfriando normalmente.

---

## 6. Cuarto 4 — 1 evaporadora, 1 zona, sin deshielo

### 6.1 Equipo

- **1 evaporadora** (1 válvula).
- **1 zona.**
- **Sin deshielo.** Igual que el Cuarto 2 pero con un retardo válvula→evaporadora mayor.

### 6.2 Estructura de bloques

```
/Config/CuartosFrios/
└── ColdRoom_4            (Cuarto frio)  — Modo de etapas = Simple
    └── EvaporatorUnit_1  (Evaporadora)  — Retardo de arranque = 5 s
```

### 6.3 Configuración

| Bloque | Slot | Valor |
|---|---|---|
| ColdRoom_4 | Modo de etapas | **Simple** |
| ColdRoom_4 | Consigna | consigna del cuarto |
| ColdRoom_4 | Diferencial arriba | según proyecto |
| ColdRoom_4 | Diferencial abajo | según proyecto |
| ColdRoom_4 | Limite alarma alta del cuarto | umbral de alarma de temp del cuarto (solo dato) |
| EvaporatorUnit_1 | **Retardo de arranque** | **5 s** |
| EvaporatorUnit_1 | Tiene deshielo | **No** |
| EvaporatorUnit_1 | Limite alarma alta / Limite alarma baja | umbrales de temp de serpentín (solo dato) |

> `Salida resistencia` y `Temp resistencia` **no se usan**. Zona 2 no se usa.

### 6.4 Tabla de ENTRADAS

| Punto físico | → Slot destino |
|---|---|
| `Zona1_C4` | ColdRoom_4 · **Zona 1** |
| `Coil_C4E1` | EvaporatorUnit_1 · **Temp evaporadora** |

### 6.5 Tabla de SALIDAS (prioridad `in8`)

| Slot origen | → Writable físico (nivel `in8`) |
|---|---|
| EvaporatorUnit_1 · **Salida valvula** | `Valv_C4E1` |
| EvaporatorUnit_1 · **Salida evaporadora** | `Evap_C4E1` |

### 6.6 Cómo se comporta

Modo simple con retardo mayor: cuando **Zona 1** pide frío, **abre la válvula** y **5 s después
arranca la evaporadora**. Al parar, primero para la evaporadora y luego cierra la válvula.

---

## 7. Lista de verificación de comisionamiento (por cuarto)

- [ ] `ColdRoom_N` creado en `/Config` y renombrado.
- [ ] Evaporadoras arrastradas **dentro** del `ColdRoom_N` y numeradas `_1.._N`.
- [ ] (Cuarto 3) `DefrostController_1` dentro del `ColdRoom_3`.
- [ ] `Modo de etapas` fijado (Cuarto 1 = Por etapas; 2/3/4 = Simple).
- [ ] `Consigna`, `Diferencial arriba`, `Diferencial abajo` cargados.
- [ ] `Retardo de arranque` por evaporadora (Cuarto 1 = 2 s; Cuarto 4 = 5 s).
- [ ] `Tiene deshielo` = Sí solo en las evaporadoras del Cuarto 3.
- [ ] (Cuarto 3) parámetros de deshielo cargados; `Retardo de escalonamiento` = 4 min.
- [ ] Todas las ENTRADAS enlazadas (sensor → slot).
- [ ] Todas las SALIDAS enlazadas al writable en nivel `in8`.
- [ ] (Cuarto 3, si aplica) `Entrada de horario` ← `BooleanSchedule`.
- [ ] Límites de alarma cargados donde corresponde (recordar: la alarma visual se arma como
      extensión del punto, documento aparte).
- [ ] Prueba funcional: forzar demanda de zona y verificar orden válvula→evaporadora y, en el
      Cuarto 3, la secuencia e interbloqueo de deshielo.

---

## 8. Resumen de I/O por cuarto

| Cuarto | Entradas numéricas | Salidas booleanas | Deshielo |
|---|---|---|---|
| 1 | 2 zona + 3 serpentín = 5 | 3 válvula + 3 evaporadora = 6 | No |
| 2 | 1 zona + 1 serpentín = 2 | 1 válvula + 1 evaporadora = 2 | No |
| 3 | 1 zona + 2 serpentín (+ 2 temp resistencia*) | 2 válvula + 2 evaporadora + 2 resistencia = 6 | Sí |
| 4 | 1 zona + 1 serpentín = 2 | 1 válvula + 1 evaporadora = 2 | No |

\* Las 2 entradas de temp de resistencia del Cuarto 3 solo si se usa `Terminar por temp resistencia`.
