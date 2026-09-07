# Block 835 — Commissioning a Honeywell BACnet MS/TP Spyder (Model 5/7, WEB-RxxN) on a JACE-8000: electrical wiring + Niagara N4 workflow

> **Focus:** spyder-commissioning (new). **Scope:** end-to-end how-to for adding a **BACnet MS/TP Spyder**
> (WEB-RS5N / WEB-RL6N, Model 5/7) to a **JACE-8000** — the field electrical connection (power, RS-485
> bus, terminations) AND the Workbench/N4 commissioning (network, discover, match, program, download).
> LON/Classic Spyder path noted where it diverges. Answers the operator question "cómo dar de alta un
> Spyder con un JACE-8000 + conexión eléctrica + configuración".
>
> **Sources (all three consulted):**
> - **FUENTE 2 (official Honeywell/Tridium docs, verbatim-verified this session):**
>   `JACE-8000_MtgWiringGuide.txt` (power, RS-485 ports/bias), `Spyder_Model_5_Installation_Instructions_-_31-00281.txt`
>   (Spyder power/terminals/auto-MAC), `Spyder_Model_5_Product_Data_-_31-00280.txt`,
>   `Honeywell_Advanced_Controller...Commissioning_Guide-31-00584-02.txt` (RS-485 cable/unit-loads),
>   `Spyder_Tool_for_WEBs-N4_User_Guide_-_31-00089.txt`, `NPB-8000-2X-485_InstallSheet.txt` — all under
>   `niagara-help/docs-text/`. Hashes in `sources/SOURCES.md`.
> - **FUENTE 1 (corpus):** B14 (Spyder Model 5/7 templates + BACnet commissioning + license), B17
>   (`spyderApps/Ver28`, `spyder.config`), B23 (BACnet MS/TP: `BBacnetMstpLinkLayer`, `BNetworkPort`,
>   baud enum, max-master), B25 (`honeywellBacnetSpyder`/`honeywellLonSpyder`/`honeywellSpyderTool` +
>   migrator), B32 (MS/TP needs a hardware RS-485 COM port).
> - **FUENTE 3 (code):** `organized/honeywellBacnetSpyder`, `organized/honeywellSpyderTool`,
>   `organized/honeywellLonSpyder`, `organized/docHoneywellSpyder`.
>
> **Verification note:** every load-bearing electrical fact below was read verbatim from the cited doc
> this session (`[CERT-doc]`). Exact per-terminal NUMBERS for the C1 block are given by LABEL (verified);
> confirm the printed number on the unit's terminal-assignment sticker (31-00281 Table).

---

## 1. Spyder families — pick the right bus and module

| Family | Bus | JACE hardware | Niagara module |
|--------|-----|---------------|----------------|
| **Spyder Model 5/6/7 (WEB-RS5N/RL6N)** ← this block | **BACnet MS/TP (RS-485)** | onboard RS-485 COM or NPB-8000-2X-485 | `honeywellBacnetSpyder` + `honeywellSpyderTool` |
| Spyder LON / Classic | LonWorks FTT-10 | LON option module | `honeywellLonSpyder` |
| Sylk (TR40x wall modules) | Sylk (2-wire) | — (hangs off the Spyder, not the JACE) | configured inside Spyder Tool |

`[CERT]` `honeywellBacnetSpyder` module.xml depends on `bacnet-rt`/`bacnet-wb` and provides the
`BacnetSpyder` device type + `BacnetSpyderDeviceManagerView`
(`organized/honeywellBacnetSpyder/.../META-INF/module.xml`). `[CERT]` B25 lists `honeywellLonSpyder` as
a separate module → **do not mix the BACnet and LON modules.**

## 2. Electrical — JACE-8000 side `[CERT-doc]` (JACE-8000_MtgWiringGuide)

- **Power:** UL-listed **Class 2, 24 Vac** (dedicated transformer; neither leg to earth), OR Class 2/LPS
  **24 Vdc** ≥ **1 A (24 W)**. 2-position removable power plug. (model 12977 DIN-rail unit.) (§Power, lines 21–31)
- **Onboard RS-485:** two ports on 3-position plugs — **COM1 = port A**, **COM2 = port B**; up to 115,200 baud (§lines 271–276).
- **More RS-485:** add **NPB-8000-2X-485** dual-RS-485 option modules (2 ports each), up to 2 → **max 6 RS-485 ports** (§lines 87–88). `[CERT-doc]` NPB-8000-2X-485_InstallSheet.
- **Bias/termination switch** (3-position, one per RS-485 port) (§lines 288–293):
  - **BIA** (default, middle) = 2.7 kΩ bias, **no** termination → use for a JACE **in the middle** of the bus.
  - **END** = 562 Ω bias **+ termination resistor** → use when the JACE is at a **physical end** of the bus.
  - MID = high-impedance bias, no termination.
- **Shield:** connect the cable shield to **earth ground at ONE end only** (§line 285) — avoids a ground loop.

## 3. Electrical — Spyder side (WEB-RS5N/RL6N) `[CERT-doc]` (31-00281)

- **Power:** **24 Vac ±20%, 50/60 Hz**, on **terminals 3 and 4** (removable plug). Max current 300 mA
  unloaded (§lines 632–646). (WEB-RL6N carries a 72 h RTC supercapacitor.)
- **BACnet MS/TP port (RS485-1) — the bus to the JACE.** Terminal numbers `[CERT-doc]` (31-00281 §213-214, strip §179-181):
  - **WEB-RS5N: 40=`C1+`, 41=`C1-`, 42=`GND`** · **WEB-RL6N: 62=`C1+`, 63=`C1-`, 64=`GND`**.
  - Wire **C1+ → JACE A+**, **C1- → JACE A-**, **GND → JACE reference/S**.
- **Power:** terminals **3, 4** (`24V~`, `24V0`) `[CERT-doc]` §229.
- **Modbus RS485-2 (`C2+ C2- GND`):** WEB-RS5N **23,24,25** · WEB-RL6N **26,27,28** — Modbus field devices, **not** the JACE bus. `[CERT-doc]` §219-221.
- **Sylk (`WM1 WM2`):** WEB-RS5N **20,21** · WEB-RL6N **30,31** — wall modules (TR40x), configured in Spyder Tool, does not go to the JACE. `[CERT-doc]` §208-210.
- **WEB-RS5N I/O map** `[CERT-doc]` (31-00281 Table 2): triacs 5-9 (TN/T~/T01/TN/T02); relays 10-19 (RO4/IN4, RN/RN, IN1/RO1, IN2/RO2, IN3/RO3); AO1-AO4 = 26/29/30/33 (24V~/GND on 27/28/31/32); UI1-UI4 = 34/36/37/39 (GND on 35/38). → 4 AO, 4 UI, small housing.
- **WEB-RL6N I/O map** `[CERT-doc]` (31-00281 Table 6): power 3,4; triacs 7-14 (T01-T04); relays 15-25 (RC4/RO4/IN4/RN, IN1/RO1, IN2/RO2, IN3/RO3); **AO1-AO6 = 32/34/36/38/40/42** (24V~/GND interleaved 33/35/37/39/41/43); **UI1-UI10 = 47/48/50/51/53/54/56/57/59/60** (GND on 49/52/55/58/61); LED out 45. → 6 AO, 10 UI, large housing. The MS/TP block is **grey**.

## 4. The MS/TP bus itself `[CERT-doc]` (31-00584 + wiring guide)

- **Cable:** **shielded twisted-pair**, per **TIA/EIA-485**, ~**22 AWG** — e.g. **Belden 9842**, J-Y-(St)-Y
  4×2×0.8, or a single pair of CAT5/6/7 (§31-00281 lines 815–817). Daisy-chain **multidrop**, polarity
  **"minus to minus, plus to plus"** (wiring guide §283).
- **Max length: 4000 ft (1200 m)** at every supported baud (9.6–76.8k), Table 10 (§31-00281 §834–837).
- **Reference (GND) wire** recommended alongside the pair unless every node is isolated (the Spyder MS/TP
  port is **non-isolated**; max ±7 V node-to-node potential, single building) (§31-00281 §873).
- **End-of-line termination at BOTH physical ends only:** a **120 Ω, 0.25–0.5 W** resistor (= cable
  impedance, not shipped). At a **JACE/plant end → set the 3-position slide switch to END** (built-in);
  at the **far Spyder end → insert a 120 Ω resistor directly across C1+/C1-** (Spyder has no term switch).
  `[CERT-doc]` (§31-00281 §826–829, §856–862).
- **Loading:** UL limits each RS-485 interface to **32 unit loads**; Honeywell devices draw ~¼ unit load
  each → up to **128** on a segment (§31-00281 §818–820). Power each controller from **separate transformers**.
- **Baud:** Spyder Model 5 supports 9.6/19.2/38.4/57.6/**76.8 kbps** (default **76.8**). `[CERT]` Niagara
  `BBacnetMstpBaudRate` enum (B23 §23.27): 9600=0 … 76800=4, 115200=5. **Set the JACE BACnet port to
  76 800** to match the Spyder default.

## 5. Addressing `[CERT-doc]` (31-00281 §Automatic MAC Addressing) + `[CERT]` B23

- **MS/TP MAC:** 0–255. **MAC 0 is reserved** (the token master / JACE). WEB-RxxN Spyders do **automatic
  MAC addressing** (self-assign within a configured range) — the range must start **≥ 1** (§lines 632–700).
  - **No DIP/rotary address switches** on the WEB-RxxN — the MAC is self-assigned between `min MAC`/`max MAC`
    (proprietary props **1028**/**1029**); MAC 0 reserved; all units are MS/TP **masters**; `maxMaster`
    default **127**. Writeable by software: `MAC address`, `maxMaster`, `min MAC`, `max MAC`. `[CERT-doc]`
    31-00281 §630–677. (Other/ancillary MSTP controllers may use MANUAL MAC addressing — Fig. 12.)
- **BACnet device instance:** must be **unique across the whole BACnet internetwork**; set in Niagara when
  the device is added.
- **Network number + baud + max-master must match** the segment. `[CERT]` B23: tune `max-master` down to
  the highest actual MAC, or every token cycle wastes scan time.

## 6. Niagara N4 commissioning (Workbench) `[CERT-doc]` (31-00089) + `[CERT]` B14

1. **Add the driver network:** Palette → `bacnet` → drag **`BacnetNetwork`** onto `Station/Drivers`.
2. **Bind the physical port + baud:** on the network's MS/TP port (`BNetworkPort`, B23 §23.28) set the
   COM port (COM1/COM2/COM3…), `mstpBaudRate = 76800`, and the JACE as MS/TP master (device address 0).
3. **Load the Spyder palette:** Palette → `honeywellSpyderTool`.
4. **Add the device:** drag **`BACnetSpyder`** onto the BacnetNetwork; name it by zone/equipment.
5. **Pick the model:** open the device → **Controller Summary View** → select the exact model
   (WEB-RS5N/RL6N / PVBxxxx) — wrong model hides I/O.
6. **Build the app:** Engineering Mode → wiresheet → drag function blocks (PID, StageDriver, setpoint calc,
   VAV) — or drop a ready **Spyder Model 5/7 VAV template** from `Palettes_and_Misc/Spyder Model 5|7/templates/` (B14 §14).
7. **Discover:** right-click BacnetNetwork → Views → **Bacnet Device Manager** → **Discover**.
8. **Match:** select the discovered physical controller → **Match** (binds MAC/instance).
9. **Download:** right-click the device → **Spyder Download → Full Download** (first time; Quick Download
   for later edits). Watch: Validation → Compilation → Download → **Successful**. Batch ops exist for many
   devices at once.
10. **License:** a BACnet network with proxy points needs an active `bacnet`/`bport` license on the JACE
    (B14 §14.10) — install it in the JACE startup/commissioning wizard.

## 7. Gotchas (each a real failure mode)

1. **Baud mismatch** → the Spyder never appears in Discover. JACE port must equal the Spyder's 76.8 kbps.
2. **MAC 0 on a Spyder** → token-master conflict; keep auto-MAC range ≥ 1.
3. **Missing/extra EOL termination** → reflections (worst at 76.8k). Exactly the two physical ends.
4. **Grounding the shield at both ends** → ground loop on the non-isolated Spyder port; ground at ONE end.
5. **Wrong model in Controller Summary View** → I/O points/blocks disappear.
6. **Quick Download on first commission** → incomplete; use **Full Download** first.
7. **max-master too high** → wasted token scans; tune to the highest MAC.
8. **LON Spyder ≠ this path** → needs the LON option module + `honeywellLonSpyder`, not RS-485 + `honeywellBacnetSpyder`.

---

## Self-verify

| # | Claim | Marker | Evidence |
|---|-------|--------|----------|
| 1 | JACE-8000 power = Class-2 24 Vac / 24 Vdc ≥1 A (24 W) | [CERT-doc] | JACE-8000_MtgWiringGuide §21–31 |
| 2 | Onboard RS-485 COM1=port A, COM2=port B; up to 6 ports via 2× NPB-8000-2X-485 | [CERT-doc] | wiring guide §87–88,271–276; NPB sheet |
| 3 | Bias switch BIA=2.7 kΩ no-term; END=562 Ω + termination | [CERT-doc] | wiring guide §291–293 |
| 4 | Shield to earth at ONE end only | [CERT-doc] | wiring guide §285 |
| 5 | Spyder power 24 Vac ±20% on terminals 3,4; 300 mA unloaded | [CERT-doc] | 31-00281 §632–646 |
| 6 | Spyder MS/TP port = C1+/C1-/GND; Modbus = C2+/C2-/GND; Sylk = WM1/WM2 | [CERT-doc] | 31-00281 terminal maps §84/116/179 |
| 7 | Cable = shielded TP, TIA/EIA-485, ~22 AWG; 32 unit loads UL | [CERT-doc] | 31-00584 §812–830,1422 |
| 8 | Spyder baud incl. 76.8k default; Niagara enum 76800=4 | [CERT-doc]+[CERT] | 31-00280; B23 §23.27 |
| 9 | MAC 0 reserved; WEB-RxxN auto-MAC (range ≥1) | [CERT-doc] | 31-00281 §632–700 |
| 10 | Commission: BacnetNetwork→port/baud→BACnetSpyder→model→app→Discover→Match→Full Download | [CERT-doc] | 31-00089; honeywellBacnetSpyder guides |
| 11 | BACnet proxy points require a license on the JACE | [CERT] | B14 §14.10 |
| 12 | `honeywellBacnetSpyder` provides BacnetSpyder + DeviceManagerView; LON uses honeywellLonSpyder | [CERT] | module.xml; B25 |
| 13 | Exact terminals: MS/TP RS5N 40/41/42, RL6N 62/63/64; power 3,4; Sylk 20,21/30,31; Modbus 23-25/26-28 | [CERT-doc] | 31-00281 §208-221,179-181,229; Table 2 |

**Tally:** 13 claims — 10 [CERT-doc], 2 [CERT], 1 mixed. No unmarked assertions. (B835-G1 closed for MS/TP + power + bus terminals + RS5N I/O; only the full WEB-RL6N I/O row-table residual remains.)

## Connections
- **B14** Spyder templates + BACnet commissioning + license — the palette/template source for step 6.
- **B23** BACnet MS/TP internals (`BNetworkPort`, baud enum, max-master) — the Niagara-side config.
- **B32** MS/TP requires a hardware RS-485 COM port — why the option module matters.
- **B25** the Honeywell Spyder module set + migrator.
- **jace8000 focus (B459/B460)** — the JACE as an embedded QNX controller (software/platform side).

## Open gaps (RESEARCH-STATE-spyder-commissioning)
- **B835-G1** *(CLOSED)* — full terminal maps for BOTH models (WEB-RS5N Table 2 + WEB-RL6N Table 6),
  termination (120 Ω both ends), cable/length/loading — all [CERT-doc] in §3/§4.
- **B835-G2** *(next)* — LON/Classic Spyder path end-to-end (NPB-8000-LON + honeywellLonSpyder) as its own recipe.
- **B835-G3** — Spyder Tool function-block reference (PID/StageDriver/VAV) for building the application.
- **B835-G4** — Live commissioning probe on the operator's JACE (authorized) to record real values [CERT-live].
