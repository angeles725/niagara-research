# Block 313 — `modbusTcpSlaveMigrator`: 61 lines that rename `httpPort` into a typed `BServerPort`, and nothing else

> Focus **modbus**, gap **M10**. What the one-class migrator module does, what it migrates *from*, and why
> only the TCP slave has one. [Block 25] listed it in the AX→N4 migrator inventory; this block opens it.
> READ-ONLY. Corpus language: ENGLISH.
>
> Sources (primary): `organized/modbusTcpSlaveMigrator/modbusTcpSlaveMigrator-wb/vineflower/…/BModbusTcpSlaveConverter.java`
> (61 lines — the module's only class).
>
> Markers: `[CERT]` local primary source (`file:line`) · `[INFER]` deduction.
>
> Layer 26 (Communication protocols — driver focus). Connects [Block 25] (the migrator inventory —
> **remitted, not re-derived**), [Block 294] (the `port` property this produces), [Block 298] (the TCP slave
> network).

---

## 313.1 — What [Block 25] already established — remitted `[CERT]`

[Block 25] §(migrator inventory) already recorded the module's existence, its single class and its one-line
purpose `[CERT]` `niagara-mental-model-bloque25.md:170`:

> `**modbusTcpSlaveMigrator-wb.jar**` | `BModbusTcpSlaveConverter` | *Modbus TCP Slave config*

and placed it among the driver-specific migrators alongside `bacnetMigrator-wb`, `snmpMigrator-wb`,
`obixMigrator-wb` and the Honeywell ones `[CERT]` `:1050`. **That is not re-derived here.** This block adds
only what [Block 25] did not open: what the conversion actually does.

## 313.2 — The whole module is one converter, for one type `[CERT]`

`BModbusTcpSlaveConverter extends BObject implements BIBogElementConverter` `[CERT]`
`…/BModbusTcpSlaveConverter.java` (61 lines total), and its static initialiser registers exactly one type:

```java
static { convertTypes.add("modbusTcpSlave:ModbusTcpSlaveNetwork"); }
```

`[INFER]` so the migrator touches **only** the TCP slave network — not devices, not points, not the serial
slave (`modbusSlave`), and nothing on the client side. Whatever changed, it changed in one property of one
component type.

The module is **`-wb` only** ([Block 294]'s inventory has no `modbusTcpSlaveMigrator-rt`) `[CERT]`.
`[INFER]` migration runs in Workbench against a station database being upgraded, not in the running station.

## 313.3 — The migration: `httpPort` (a bare int) → `port` (a `BServerPort`) `[CERT]`

Two methods do the work. `convertXElem` operates on the raw bog XML before typing `[CERT]`:

```java
for (XElem kid : x.elems()) {
   String name = kid.get("n", "-");
   if (name.equals("httpPort")) {
      if (kid.get("t", "null").endsWith(":ServerPort")) this.port = -1;      // already migrated
      else { this.port = kid.geti("v", 502); x.removeContent(kid); }         // capture + delete
   }
}
```

then `convertComplex` applies it to the typed component `[CERT]`:

```java
if (toConvert instanceof BModbusTcpSlaveNetwork) {
   BModbusTcpSlaveNetwork network = (BModbusTcpSlaveNetwork) toConvert;
   if (this.port != -1) network.getPort().setPublicServerPort(this.port);
}
```

with the field initialised `private int port = 502` `[CERT]`.

So the old schema stored the listening port as a property literally named **`httpPort`**, holding a plain
integer; the current schema is the `port` property [Block 294] §294.4 measured as
`new BServerPort(502, IpProtocol.TCP)`. The converter lifts the integer out of the old element, deletes the
element, and pushes the value into the new typed property's *public* server port.

Three details worth naming `[INFER]`:

- **the old name is `httpPort`**, which has nothing to do with HTTP. The Modbus TCP slave listener was
  evidently declared by reusing a property name from the web/server infrastructure — a naming artefact the
  migration quietly retires;
- **it is idempotent by sentinel**: if the element's type attribute already ends in `:ServerPort`, `port` is
  set to `-1` and `convertComplex` skips the assignment. Re-running the migration on an already-migrated
  station is a no-op;
- **502 appears twice as a default** — as the field initialiser and as the `geti` fallback — so a malformed
  or missing value lands on the standard Modbus port rather than zero.

## 313.4 — Why only this one `[INFER]`

`[INFER]` the serial slave (`modbusSlave`) needs no migrator because it has no port property at all — its
transport is `serialPortConfig`, a `BSerialHelper` ([Block 309] §309.1), which did not change type. The
client modules have no listening port either. The TCP slave is the only Modbus component that ever exposed a
socket-level port as a bare integer, so it is the only one whose schema had to be lifted to the typed
`BServerPort`. That is consistent with [Block 294] §294.4, where `port` on `BModbusTcpSlaveNetwork` is the
sole `BServerPort` in the whole driver.

## 313.5 — Self-verify

`verify-block.sh` tally (COMPUTED — `adj` strips the header legend):

| Marker | raw | adj |
|---|---|---|
| `[CERT]` | 21 | 20 |
| `[CERT-doc]` | 1 | 1 |
| `[CERT-hw]` / `[CERT-live]` / `[CERT-web]` / `[CERT-a]` | 0 | 0 |
| `[INFER]` | 7 | 6 |
| **[INFER]/[CERT*] ratio** | | **6/21 = 0.29** |

Script exit 0. (The `[CERT-doc]` counted is the sentence below naming the marker, not a citation.)

**Block type: EVIDENCE.**

Load-bearing claims:

| # | Claim | Marker | Verified how |
|---|---|---|---|
| 1 | [Block 25] already listed the module + its single class | `[CERT]` | `niagara-mental-model-bloque25.md:170, 1050` verbatim |
| 2 | The module has exactly one class, 61 lines | `[CERT]` | directory enumeration + `wc -l` |
| 3 | `implements BIBogElementConverter` | `[CERT]` | class declaration |
| 4 | `convertTypes` holds one entry, `modbusTcpSlave:ModbusTcpSlaveNetwork` | `[CERT]` | static initialiser read in full |
| 5 | `convertXElem` matches the element named `httpPort` | `[CERT]` | method read in full |
| 6 | `:ServerPort` type suffix → `port = -1` (idempotence sentinel) | `[CERT]` | same method |
| 7 | Otherwise capture `v` (default 502) and `removeContent(kid)` | `[CERT]` | same method |
| 8 | `convertComplex` calls `getPort().setPublicServerPort(port)` when `port != -1` | `[CERT]` | method read in full |
| 9 | Field initialiser is `private int port = 502` | `[CERT]` | field declaration |
| 10 | No `-rt` profile for this module | `[CERT]` | module inventory ([Block 294] §294.2 measured 1 class, `-wb` only) |

Tokens grep-confirmed in their cited source: **10 / 10** — the entire class was read, so every claim is a
direct reading rather than a grep hit. §313.4 is wholly `[INFER]`, argued from the absence of a comparable
property elsewhere in the driver rather than from any statement in the migrator.

`[CERT-doc]`: none — the guide has no migration topic. No new sources preserved. Model tier:
**no delegation — inline**.

## 313.x — Connections

- **[Block 25]** — the migrator inventory this block deepens; §313.1 remits rather than repeats it.
- **[Block 294]** — §294.4's `port = BServerPort(502, TCP)` is the target schema of this migration.
- **[Block 298]** — the TCP slave network whose config is migrated.
- **[Block 309]** — the serial slave's `BSerialHelper`, the reason no serial migrator exists.

**Gaps opened by this block**: none.
