# B663 — JACE-9000 serial port map: COM1/COM2 are top-side RS-485 field-bus ports (screw terminals, 3-position bias/termination switch) — a separate thing from the front USB-C DEBUG console, even though both can run at 115200 (focus jace9000, J9K-12)

> **Focus:** `jace9000` (§16). **Gap:** J9K-12 — confirm the RS-485 field ports (COM1/COM2) are physically
> and functionally separate from the USB-C DEBUG serial console (a 115200-baud collision trap).
> **Phase:** DISK-FIRST (doc). Read-only. Closes the last doc-investigable gap of the focus.
> **Sources:** `[CERT-doc]` niagara-help `J9MtgWrg/{RS485Wiring, RS485BiasSwitches, ShutdownAndDebug}` ·
> `[CERT]` corpus [Block 657] (DEBUG console) [Block 448] (JACE-8000 RS-485 field bus).
>
> **Bottom line for the operator:** the JACE-9000 has **three serial interfaces that are easy to confuse
> because two of them can run at 115200**: **COM1** and **COM2** are **RS-485 field-bus** ports on the
> controller's **top side** (screw-terminal, each with a bias/termination switch) for wiring field devices
> (Modbus, etc.); the **DEBUG** port is a **USB-C** port behind the front and speaks **only** the ATLAS
> System Shell. Same possible baud, completely different purpose and connector — do not cross them.

## §663.1 — COM1/COM2: the RS-485 field-bus ports

`[CERT-doc]` "On the controller's **top side**, two RS485 ports operate as **COM1** and **COM2**. Each port
is capable of **up to 115,200 baud**, and uses a **three-position screw terminal connector**."
— `J9MtgWrg/RS485Wiring-0B48A1ED.txt:12-13`.

These are the multidrop field-wiring ports (RS-485 differential pair + common), used to reach field devices
over serial field protocols. Their "up to 115,200 baud" ceiling is exactly the number that collides with the
DEBUG console default — but these carry a **wire protocol to field devices**, not a login shell.

## §663.2 — Each RS-485 port has a 3-position bias/termination switch

`[CERT-doc]` "Each RS485 port has an adjacent **three-position biasing switch**."
— `J9MtgWrg/RS485BiasSwitches-0B48DC84.txt:12`. The three positions and their resistor networks:

| Switch | Bias | Termination | Use |
|---|---|---|---|
| **BIA** (default, middle) | 2.7 kΩ bias resistors | none | trunk needs biasing, controller NOT at the end of the line |
| **END** | 562 Ω bias resistors | 150 Ω termination | controller AT the end of the RS-485 trunk |
| **MID** | 47.5 kΩ bias resistors | none | light/no biasing needed |

`[CERT-doc]` values verbatim: BIA "2.7K Ohm bias resistors with no termination resistor" (`:23`); END "562 Ohm
bias resistors and 150 Ohm termination resistor" (`:25`); MID "47.5K bias resistors with no termination
resistor" (`:27`); and "BIA … is often best if the RS485 trunk needs biasing when the controller is not
installed at the end of the [trunk]" (`:35`). Termination (END) is the one that adds the 150 Ω line
terminator — set it only when the JACE sits at a physical end of the bus.

## §663.3 — DEBUG: the odd one out

`[CERT-doc]` "DEBUG: The DEBUG port is a **USB-C** port for serial debug communications to the controller
**only**." (`J9MtgWrg/ShutdownAndDebug-0B4C4735.txt:40`); "You can use a serial terminal program (for
example: PuTTY) with the DEBUG port to access the controller's **system shell menu** … Default DEBUG port
settings are: 115200, 8, N, 1 … Login requires admin-level platform credentials." (`:46-49`).

So the disambiguation is clean:

| Interface | Connector / location | Carries | Speaks |
|---|---|---|---|
| **COM1 / COM2** | 3-pos screw terminal, top side | RS-485 field bus (differential) | field protocol (Modbus, etc.) |
| **DEBUG** | USB-C, front | point-to-point serial to PC | ATLAS System Shell (platform admin) |

The operator's COM5 ([Block 657]) is a Windows COM-port *enumeration* of the **DEBUG** USB-C link — it is not
COM1 or COM2 on the controller. A field device wired to COM1/COM2 will never present the shell, and the DEBUG
port will never carry Modbus.

## §663.4 — Relationship to the JACE-8000 field-bus block

`[CERT]` [Block 448]/[Block 449] documented the JACE-8000's 115200-baud RS-485 traffic for the **nrio** I/O
expansion bus (`actrld`, `/dev/ser2`) — a *driver/field* concern, the same family as COM1/COM2 here, and
firmly distinct from a debug console. This block confirms the JACE-9000 keeps the COM1/COM2 RS-485 field
ports and adds the concrete bias/termination switch spec, which the corpus did not previously hold.

## Self-verify

| # | Claim | Marker | Citation | Checked |
|---|---|---|---|---|
| 1 | COM1/COM2 = top-side RS-485, ≤115200, 3-pos screw terminal | [CERT-doc] | RS485Wiring-0B48A1ED.txt:12-13 | ✓ grep |
| 2 | Each RS-485 port has a 3-position bias switch | [CERT-doc] | RS485BiasSwitches-0B48DC84.txt:12 | ✓ grep |
| 3 | BIA = 2.7K bias, no termination (default) | [CERT-doc] | …RS485BiasSwitches:23 | ✓ grep |
| 4 | END = 562Ω bias + 150Ω termination | [CERT-doc] | …RS485BiasSwitches:25 | ✓ grep |
| 5 | MID = 47.5K bias, no termination | [CERT-doc] | …RS485BiasSwitches:27 | ✓ grep |
| 6 | DEBUG = USB-C, controller only, 115200 8N1, shell menu, admin login | [CERT-doc] | ShutdownAndDebug-0B4C4735.txt:40,46-49 | ✓ grep (B657) |
| 7 | JACE-8000 nrio RS-485 field bus is the sibling field-serial concern | [CERT] | [Block 448]/[Block 449] | ✓ corpus |

**Marker tally:** [CERT-doc]=6, [CERT]=1 (corpus), [INFER]=0 · ratio [INFER]/[CERT*]=0 · **block type =
EVIDENCE (doc, hardware reference)**. No unmarked claims.

## Connections

- [Block 657] — the DEBUG/ATLAS-shell side; COM5 = a PC-side enumeration of DEBUG, not COM1/COM2.
- [Block 448]/[Block 449] — the JACE-8000 RS-485 field bus (nrio/actrld): the field-serial family COM1/COM2
  belongs to, distinct from a console.

## Open gaps (RESEARCH-STATE-jace9000.md)

**investigable_open → 0.** All doc-investigable gaps closed (J9K-0/1/4/5/6/7/8/11/12). Remaining are
**live-gated** and need the operator's serial session: J9K-2 (System Diagnostic outputs), J9K-3 (pre-login
exposure), J9K-9 (passphrase-on-serial), J9K-10 (this firmware's live main-menu numbering). → STOP: write
the focus synthesis + §18 retro.
