# Bloque 418 — Niagara Network Supervisor (VI): tipos no resueltos en el BOG propio del JACE — REMITTANCE a B405 + delta N6

> **Qué documenta**: el gap **N6** — qué hace Niagara cuando una station PROPIA carga su BOG
> con un tipo no registrado (p.ej. `exportTags:PxViewTag` en un JACE sin el módulo `-wb`).
> Contexto: arranque de estación / deserialización BOG normal (`BogTypeResolver`), no
> migración (BBogMigrator/ConverterRegistry).
>
> **REMITTANCE** — el mecanismo central lo documentó [Bloque 405] §405.10 (`[CERT]`):
> `TypeNotFoundException` → `warningAndSkip` → `null` → propiedad descartada. Este bloque
> NO re-documenta ese mecanismo; agrega el delta específico para N6:
> (a) ruta exacta cuando el módulo SÍ carga pero el tipo falta en `-rt`;
> (b) confirmación de que `warningAndSkip` **no es silencioso** (log WARNING con line:col);
> (c) distinción con el path supervisor-side de [Bloque 414] §414.4 (`BlacklistTypeResolver`).
>
> **Subject version**: Niagara N4 4.14.0.162 · build 2024-05-28
>
> **Fuentes** (Vineflower; alias de ruta):
> - `$VDD` = `/home/cristian/modules/Prototipos/modulos/organized/baja/baja/vineflower/javax/baja/io/ValueDocDecoder.java`
>
> **Método**: lectura inline de `$VDD` (5 rangos verificados por número de línea); cross-check
> con fuente niagara-help (misma lógica, numering difiere). 6 tokens `[CERT]` de peso.
> Marcadores: `[CERT]` = fuente primaria (file:line); `[INFER]` = deducción.
>
> Deserialización BOG / arranque de estación. Conecta [Bloque 405] (mecanismo general de carga
> BOG + migración), [Bloque 414] (BlacklistTypeResolver supervisor-side).

---

## 418.1 — Prior coverage: B405 §405.10 responde la pregunta núcleo `[CERT]`

[Bloque 405] §405.10 verificó directamente contra `ValueDocDecoder.java` que en la ruta de
carga normal de una station N4 (no migración):

> `TypeNotFoundException` → `warningAndSkip("Type 'X' not found: propName")` → retorna `null`
> → la propiedad queda **descartada** del árbol de componentes.

Esto responde la pregunta central de N6: **Niagara no aborta, no inserta stub/placeholder —
descarta la propiedad**. Lo que B405 no cubrió (y constituye el delta de N6):

1. La ruta interna exacta de `BogTypeResolver.newInstance()` cuando el módulo SÍ carga pero
   el tipo falta dentro del módulo instalado.
2. Qué hace exactamente `warningAndSkip` — el nombre sugiere que hay un log, pero B405 no
   verificó la implementación (la describió como "silently dropped").
3. La distinción JACE-side vs supervisor-side (B414 §414.4 usa `BlacklistTypeResolver`).

---

## 418.2 — Ruta exacta: módulo carga, tipo ausente del perfil `-rt` `[CERT]`

`BogTypeResolver` (clase interna estática de `ValueDocDecoder`) es el resolver usado durante
el arranque normal de cualquier station N4 `[CERT]` `$VDD:1036`.

En el JACE, el módulo `exportTags` sí está instalado (como parte `-rt`). Al cargar el BOG:

**Paso 1** — `loadModule()` resuelve `exportTags` vía `Nre.getModuleManager().loadModuleParts("exportTags")`.
Retorna los parts disponibles; en el JACE solo existe `-rt` → no hay `ModuleException`. El
mapa `byProfile` se pobla con el módulo `-rt`.

**Paso 2** — `newInstance()` busca `PxViewTag` en cada `NModule` cargado:

```java
// $VDD:1079-1087
for (NModule module : byProfile.values()) {
    if (module.hasType(tname)) {            // exportTags-rt: PxViewTag ausente → false
        return ValueDocDecoder.typeResolverNewInstance(module, tname);
    }
}
// ningún módulo tiene el tipo → cae al fallback:
Iterator<NModule> i = getModuleMap(decoder).get(tkey).values().iterator();
if (i.hasNext()) {
    return ValueDocDecoder.newSwapInstance(i.next().getModuleName(), tname);
}
```

`[CERT]` `$VDD:1079-1087`

**Paso 3** — `newSwapInstance("exportTags", "PxViewTag")` consulta el `typeSwapMap`:

```java
// $VDD:592-604
String typeString = "exportTags:PxViewTag";
String typeSwap = typeSwapMap.get(typeString);   // → null (mapa solo tiene NiagaraVirtualGateway)
if (typeSwap != null) { ... }
else { throw new TypeNotFoundException(typeString); }
```

`[CERT]` `$VDD:592-604` + mapa con entrada única `[CERT]` `$VDD:608`

**Paso 4** — `TypeNotFoundException` capturada en `newInstance()`:

```java
// $VDD:1097-1099
} catch (TypeNotFoundException var13) {
    decoder.plugin.warningAndSkip("Type \"" + var13.getMessage() + "\" not found: " + propName);
    return null;
}
```

`[CERT]` `$VDD:1097-1099`

Resultado: `null` retornado → propiedad `BPxViewTag` descartada del árbol de componentes del JACE.
La station **continúa arrancando** — el `TypeNotFoundException` es un error *recoverable* por diseño.

---

## 418.3 — `warningAndSkip` NO es silencioso: WARNING con line:col `[CERT]`

B405 §405.10 describió el drop como "silently dropped". La implementación de `warningAndSkip`
muestra que **no es silenciosa** — emite un log a nivel WARNING:

```java
// $VDD:897-912
public void warningAndSkip(String msg) throws RuntimeException {
    this.warning(msg);   // ← log.warning primero
    this.skip();
}

public void warning(String msg) throws RuntimeException {
    this.log.warning(msg + " [" + this.parser.line() + ':' + this.parser.column() + "]");
    this.warningCount++;
}
```

`[CERT]` `$VDD:897-912`

Para el escenario N6, el mensaje exacto en el log de Niagara es:

```
WARNING  Type "exportTags:PxViewTag" not found: <propName> [<line>:<col>]
```

Este WARNING es visible en el log de la station (archivo de log Niagara, nivel WARNING).
La station NO produce excepción ni aborta; el `warningCount` se incrementa internamente. `[CERT]`

`[INFER]` Corrección al §405.10: "silently dropped" es impreciso — la propiedad se descarta
**con un WARNING en el log**. "Silently" aplica solo en el sentido de que no se lanza excepción
ni se aborta el arranque, no en el sentido de que no hay log.

---

## 418.4 — Distinción con `BlacklistTypeResolver` (B414 §414.4): dos rutas, mismo resultado `[CERT]`

B414 §414.4 documentó que `BSupervisorJoinJob` usa un `BlacklistTypeResolver` propio al
decodificar el BOG de la **subordinada** (ruta supervisor↔JACE). Ese resolver:

```java
// BSupervisorJoinJob.java inner class (B414 §414.4)
BValue result = super.newInstance(decoder, parent, propName, prop, typeStr);
if (result == null) { return null; }   // propaga el null de BogTypeResolver
if (BFoxChannel.isBlacklistedLegacyType(...)) { decoder.skip(); return null; }
return result;
```

Diferencias clave entre las dos rutas:

| Aspecto | JACE cargando SU PROPIO BOG (N6) | Supervisor decodificando BOG subordinado (B414) |
|---|---|---|
| Resolver | `BogTypeResolver` nativo | `BlacklistTypeResolver` (subclase) |
| Origen | `ValueDocDecoder` estándar en arranque | `BSupervisorJoinJob` en join |
| Resultado tipo no resuelto | `warningAndSkip` → null | `super.newInstance()` → null (misma ruta base) |
| Capa de blacklist adicional | No | Sí — tipos legacy Fox también omitidos |
| Efecto final | Propiedad ausente + WARNING en log | Propiedad ausente (misma ausencia) |

`[CERT]` — ambas rutas verificadas en sus respectivas fuentes (B414 §414.4 para `BlacklistTypeResolver`;
`$VDD:1036-1099` para `BogTypeResolver`).

**Conclusión para R2 de B414 §414.5**: el JACE con `BPxViewTag` en su BOG propio simplemente
lo descarta con un WARNING al arrancar — sin stub, sin abort, con la entrada en el log.
El árbol de componentes runtime del JACE **no tiene** el slot `BPxViewTag`; la station opera
con la propiedad ausente del modelo. `[INFER]` Implicación operacional: si Workbench agregó
`BPxViewTag` al JACE y el JACE reinicia, ese tag desaparece silenciosamente del árbol runtime
(solo WARNING), pero la station arranca sin error fatal.

---

## 418.5 — Self-verify (§11)

| Claim | Marcador | Cita |
|---|---|---|
| `BogTypeResolver` clase interna de `ValueDocDecoder` | `[CERT]` | `$VDD:1036` |
| Walk `byProfile` → `hasType(tname)` → false para PxViewTag en `-rt` | `[CERT]` | `$VDD:1079-1082` |
| Fallback `newSwapInstance(moduleName, tname)` cuando ningún módulo tiene el tipo | `[CERT]` | `$VDD:1085-1087` |
| `typeSwapMap` tiene sólo 1 entrada (NiagaraVirtualGateway) → PxViewTag → TypeNotFoundException | `[CERT]` | `$VDD:608` |
| `TypeNotFoundException` → `warningAndSkip("Type \"X\" not found: propName")` → null | `[CERT]` | `$VDD:1097-1099` |
| `warningAndSkip` = `warning()` + `skip()` | `[CERT]` | `$VDD:897-907` |
| `warning()` llama `log.warning(msg + " [line:col]")` | `[CERT]` | `$VDD:910-912` |

**Tally**: 7 tokens `[CERT]`; 2 `[INFER]` (corrección a B405 "silently" + implicación operacional). 0 `[CERT-a]`. Tipo: **REMITTANCE**.

---

## 418.6 — Conexiones

- **[Bloque 405] §405.10** — documentó el mecanismo base: TypeNotFoundException → warningAndSkip → null → drop. B418 remite a ese bloque para el mecanismo y agrega el delta específico de N6 (ruta `newSwapInstance`, implementación de `warningAndSkip`, distinción JACE-side). CORRECCIÓN §14: B405 §405.10 describe el drop como "silently dropped" — precisión: hay un log WARNING en el Niagara log (no silent). El bloque B405 puede agregar la nota: *"warningAndSkip emite WARNING al log antes de skip — ver B418 §418.3 para la implementación verificada."*
- **[Bloque 414] §414.4** — documentó `BlacklistTypeResolver` (supervisor-side); B418 diferencia el path JACE-own-BOG del path supervisor↔subordinada, mostrando que ambos descartan el elemento pero por rutas de código distintas.
- **[Bloque 414] §414.5 (R2)** — B418 cierra R2: JACE con `BPxViewTag` en su BOG propio → WARNING + drop → station no aborta. El riesgo operacional está confirmado.
