# Bloque 184 — Catálogo de converters (BConverter): el motor del binding dinámico a `visible`

> Research del focus **`px-menu`** (gap G9): el catálogo de **converters** (`javax.baja.converters`), y el
> deep-dive de `BIBooleanToSimple` — la clase que, como `<IBooleanToSimple name="visible"/>`, mapea el valor
> booleano de un punto sobre la propiedad `visible` de un widget. Es el motor del patrón toggle in-place (B181
> §181.4). Responde la pregunta crítica: **¿produce un boolean usable para `visible`?** NO cubre `BValueBinding`
> (B-anterior/G3) ni el resto de bindings.
>
> Sources (preservados §5): `sources/decompiled/converters-rt/` — `BIBooleanToSimple.java`,
> `BINumericToSimple.java`, `BIStatusToSimple.java`, `BIEnumToSimple.java` (Vineflower) + `BConverter.java`
> (docSource, `javax.baja.util`). Barrido delegado (sonnet) 2026-07-06.
> Method: lectura READ-ONLY del decompilado. Markers (§3): `[CERT]` `file:line` · `[INFER]`.
>
> Capa PX (bindings). Connects [Block 181] (binding=slot hijo), [Block 183] (valores gx que un converter produce).

---

## 184.1 — El catálogo: 104 converters; la familia `BI*ToSimple` es la widget-facing `[CERT]`

El módulo `converters` tiene **104 clases** `BConverter` (`organized/converters/converters-rt/vineflower/javax/baja/converters/`),
agrupables por familia `origen→destino`: `Number/INumeric→X` (~12), `StatusNumeric→X` (~10), `Enum→X` (~10),
`StatusString→X` (~9), `String→X` (~9), `StatusEnum→X` (~8), `Boolean/StatusBoolean→X` (~14),
`Time/Date→X` (~12), misc (`BPassThrough`, `BFixedSimple`, `BObjectToString`…). `[CERT]`

El subconjunto que sirve para bindings dinámicos `name="<prop>"` sobre widgets es la familia **`BI*ToSimple`**
(6 clases: `BIBooleanToSimple`, `BIEnumToSimple`, `BINumericToSimple`, `BIStatusToSimple`, `BIBooleanToBoolean`,
`BIEnumToEnum`), porque su destino es `baja:Simple` — el tipo paraguas de cualquier propiedad de widget
(boolean, enum, color, string). `[CERT]` `[INFER]`

## 184.2 — Contrato base `BConverter.convert(from, to, ctx)` `[CERT]`

`BConverter` (un `BStruct`) define (`BConverter.java:19-26`):
```java
public void init(BObject from, BObject to) { }
public final BObject convert(BObject from, BObject to) { return this.convert(from, to, null); }
public abstract BObject convert(BObject from, BObject to, Context ctx);
```
`init` es el hook one-time (que `BIBooleanToSimple` override), y `convert(from,to,ctx)` es exactamente lo que
`BValueBinding.getOnWidget()` invoca (B-anterior/G3): `from` = valor del punto, `to` = valor actual de la
propiedad del widget. `[CERT]`

## 184.3 — `BIBooleanToSimple`: `trueValue`/`falseValue` + type-guard `[CERT]`

Declarada `@Adapter(from="baja:IBoolean", to="baja:Simple")` (`BIBooleanToSimple.java:17-22`), con dos slots
`BSimple` editables: `trueValue` (default `BBoolean.TRUE`) y `falseValue` (default `BBoolean.FALSE`)
(`BIBooleanToSimple.java:23-31`) — NO son `trueText`/`falseText` strings. `[CERT]`

`convert()` (`BIBooleanToSimple.java:62-72`):
```java
boolean bool = ((BIBoolean)from).getBoolean();
BSimple v = bool ? getTrueValue() : getFalseValue();
return (v.getType() == to.getType() ? v : to);
```
El booleano del punto selecciona `trueValue`/`falseValue`, **pero el valor solo se devuelve si su `Type`
coincide con el de la propiedad destino**; si no, devuelve `to` sin cambios (no-op, NO excepción). Ese
type-guard es lo que impide corromper la propiedad con un valor de tipo equivocado. `[CERT]`

## 184.4 — ¿Produce un boolean usable para `visible`? SÍ, sin coerción `[CERT]`

Sí, en el caso típico/default: `trueValue`/`falseValue` default a `BBoolean.TRUE`/`FALSE`, y la propiedad
`visible` de un widget es `BBoolean` (B182 §182.1 vía `BWidget`), así que el type-guard PASA y se devuelve un
`BBoolean` real, directamente asignable a `visible`. **No hace falta ningún truco de coerción** — el patrón
in-place (B181 §181.4) es sólido. `[CERT]`

La generalidad viene de que `trueValue`/`falseValue` son `BSimple` (no `BBoolean`): un autor puede reapuntar
el MISMO converter a cualquier propiedad simple (color, enum, string) editando esos dos slots al tipo del
destino. El type-guard evita el mismatch. `[INFER]`

## 184.5 — Gotcha operacional: `init()` reseed `[CERT]`

`init(from, to)` siembra AMBOS slots al valor ACTUAL de la propiedad del widget al momento de bindear:
`BIBooleanToSimple.java:57-60` — `this.setTrueValue((BSimple)to); this.setFalseValue((BSimple)to);`. `[CERT]`

Implicación `[INFER]`: un binding recién agregado en el editor puede arrancar como no-op (ambos slots iguales
al valor actual) hasta que el autor edite `trueValue`/`falseValue` en el property sheet para que difieran
(p.ej. `trueValue=true`, `falseValue=false`). En un `.px` que ya trae los slots configurados (o los defaults
`TRUE`/`FALSE` decodificados), el toggle funciona; recién-agregado a mano puede requerir ajustar los slots.
La interacción exacta entre `init()` y los defaults decodificados es un detalle de runtime — se documenta el
código; el comportamiento fino se confirmaría en la fase dinámica (§12). `[INFER]`

## 184.6 — Hermanos widget-facing `[CERT]`

- `BINumericToSimple.convert()` (`BINumericToSimple.java:46-50`): patrón LOOKUP-TABLE — busca
  `getMap().get(numeric)` en un `BNumericToSimpleMap`, devuelve el valor si existe, si no `to` (sin cambios).
  Sirve para mapear un enum/numérico de estado a distintos valores de propiedad. `[CERT]`
- `BIStatusToSimple.convert()` (`BIStatusToSimple.java:162-211`): chequea bits de status
  (disabled/fault/down/alarm/stale/overridden/null/unackedAlarm/ok) en orden de prioridad vía `check()`,
  devolviendo el primer slot `BSimple` cuyo tipo matchee `to` — MISMO type-guard que `BIBooleanToSimple`.
  Útil para, p.ej., cambiar color/visible según el status del punto. `[CERT]`

## 184.x — Connections

- **[Block 181]** — binding = slot hijo: el converter `<IBooleanToSimple name="visible"/>` ES ese slot hijo dinámico.
- **[Block 183]** — valores gx: lo que un `BI*ToSimple` devuelve (color/boolean/string) usa esos grammars.
- **[Block 182]** — `visible` es `BBoolean` de `BWidget` (por qué el type-guard pasa).
- **B185+** (`PopupBinding`, in-place, síntesis) — el in-place usa `BValueBinding` + este converter.
- **G3** (próximo tras G2) — `BValueBinding`: quién invoca `convert(from,to,ctx)` (§184.2) y arma el slot dinámico.
