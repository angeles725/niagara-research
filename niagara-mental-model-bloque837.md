# Block 837 — The LON / Classic Spyder path on a JACE-8000: FTT-10 wiring + LonWorks commissioning, and how it differs from BACnet MS/TP (B835-G2)

> **Focus:** spyder-commissioning. **Gap:** B835-G2 — the LON sibling of the BACnet MS/TP recipe in B835.
> When the Spyder is a **LON/Classic** unit (not MS/TP), the option module, the bus, the addressing and the
> commissioning verbs all change. Read B835 for MS/TP; this block is the LON counterpart.
>
> **Sources (all three):** FUENTE 2 — `niagara-help/docs-text/`: `12997(LON)-C.txt` +
> `NPB-8000-LON_InstallSheet.txt` (the LON option module), `JACE-8000_MtgWiringGuide_-TRI.txt` (4 option
> slots), `docLonworks.txt` (Niagara LonWorks Driver Guide), `Spyder_Tool_for_WEBs-N4_User_Guide_-_31-00089.txt`,
> `Spyder_PUL-PUB-PVB-PVL_Controllers_User_Guide_-_63-2662.txt`; `guides-clean/HoneywellSpyder/honeywellLonSpyder-*`.
> FUENTE 3 — `organized/honeywellLonSpyder/.../module.xml` (`BLonSpyder`, deps `lonworks-rt/wb`). FUENTE 1
> — B77 (`BLonSpyder extends BDynamicDevice`), B135 (TP/FT-10 78.125 kbaud, service-pin). Hashes in SOURCES.

---

## 1. LON option module + JACE hardware `[CERT-doc]`

- **Module:** **NPB-8000-LON, part 12978** — an **FTT-10A LonWorks** adapter with a **2-position** removable
  screw-terminal plug (`12997(LON)-C.txt` L12-15).
- **How many:** the JACE-8000 has **4 option-module slots total** (`JACE-8000_MtgWiringGuide_-TRI` L39,196,629)
  → up to **4 LON modules** ("two LonWorks FTT-10 (2 LON modules)" example L105). Mixing rule: 2 dual-RS-485
  modules leaves room for only 1 non-RS-485 (LON/232), 3 total.
- **Port naming:** one LON module = **LON1** regardless of slot; multiples number from the base outward
  (`12997(LON)-C.txt` L28-29). Each LON port = one `LonNetwork`, its `Lon Comm Config > Device Name = LONn`.
- **Install:** **power down the JACE** before seating/removing an option module.
- **License:** the host license must include the **`lonworks`** feature (`docLonworks.txt`).

## 2. Electrical — FTT-10 bus `[CERT-doc]` (+ `[INFER]` where noted)

- Wire the **FTT-10A** network to the module's **2-position** connector, **26–12 AWG** (`12997(LON)-C.txt` L114-118).
- **Polarity-insensitive** — "polarity is not a factor in FTT-10A wiring" (a fundamental FTT-10 property).
- **Free topology** (FTT = *Free Topology* Transceiver): bus, star, loop or mixed. Echelon's *FTT-10A
  Free Topology Transceiver User's Guide* (078-0156-01F) governs the exact distance/junction rules — that
  Echelon doc is **not** in our sources, so treat max node/length as unconfirmed here.
- **Signal rate: 78.125 kbaud**, differential-Manchester (TP/FT-10 physical layer) — **fixed, no baud
  setting** (B135).
- **Termination:** `[INFER]` FTT-10 free topology does **not** use end-of-line terminators the way RS-485
  does (one network-level terminator is used per Echelon guidance) — **not** cited from our three sources;
  confirm in the Echelon FTT-10 guide. This is a real divergence from the MS/TP "both ends 120 Ω".
- **Module LEDs:** TX (yellow, JACE transmitting), RX (green, another node), PWR (green).

## 3. Commissioning in Niagara `[CERT-doc]` (docLonworks + Spyder Tool guide) + `[CERT]` module.xml

1. **Modules + license:** station has `lonworks`(+`kitLon`) and **`honeywellLonSpyder`**; license `lonworks`.
2. **Add LonNetwork:** `Config > Drivers` → New → **Lon Network**; if several LON ports, give each a unique
   **Device Name (LON1/LON2…)**.
3. **Add the device:** from the **`honeywellSpyderTool`** palette drag **`LonSpyder`** onto the LonNetwork
   (`BLonSpyder`, `LonSpyderDeviceManagerView` registered on `lonworks:LonNetwork` `[CERT]` module.xml).
4. **Discover / service pin:** right-click LonNetwork → **Lon Device Manager** → **Discover**. If a node
   doesn't show (different domain), press its physical **service pin** — the manager shows "Waiting on
   service pin" (up to **300 s**) then lists it. Each node carries a unique **48-bit Neuron ID** (burned in).
5. **Match:** pair the discovered node with the database `LonSpyder` (binds subnet/node + Neuron ID).
6. **Commission:** the **Commission** action writes the device's **domain table (domain 0), subnet/node
   address, auth key, address table, NV config** → state "Configured, online". (This step has no MS/TP
   equivalent — the station acts as the LonWorks **network manager**.)
7. **Download the app:** right-click → **Spyder Download** (Full/Quick) — transfers the compiled program
   via **LonWorks file transfer** (Echelon ShortStack image) (B77/B120).
8. **NV binding:** `LonSpyder > ControlProgram > NV Configuration View` — configure **NVI/NVO/NCI**, use
   **Generate NVs**, bind through the **Lon Link Manager** (the station owns bindings). Spyder actions:
   Generate NVs / Generate XIF / Generate Lnml / Compile / Spyder Download / Set Mode to Auto. Batch ops
   under LonNetwork → Spyder Batch Operations.

## 4. LON vs BACnet MS/TP — the differences that matter

| Aspect | **LON (this block)** | **BACnet MS/TP (B835)** |
|--------|----------------------|--------------------------|
| Option module | NPB-8000-LON (12978), 2-pos | NPB-8000-2X-485, 3-pos |
| Physical bus | FTT-10A twisted pair, **free topology** | RS-485 **linear multidrop** |
| Polarity | **insensitive** | **sensitive** (−/−, +/+) |
| Termination | free-topology (no both-ends 120 Ω) `[INFER]` | **120 Ω at both ends**; JACE = END switch |
| Baud | **fixed 78.125 kbaud** | configurable (set 76 800 for Spyder) |
| Addressing | **48-bit Neuron ID** + subnet/node (NM-assigned) | **MAC** (auto) + BACnet **device instance** |
| Network mgmt | station is the **LonWorks network manager** (domains, bindings) | none central; each device self-identifies |
| Module / device | `honeywellLonSpyder` / `BLonSpyder` | `honeywellBacnetSpyder` / `BBacnetSpyder` |
| Download | LonWorks file transfer + ShortStack | (per B835) Spyder Download over BACnet |
| License | `lonworks` | `bacnet` |

The **Spyder Tool wiresheet + function blocks are the same** for both (B836) — only the network layer differs.

---

## Self-verify

| # | Claim | Marker | Evidence |
|---|-------|--------|----------|
| 1 | NPB-8000-LON = part 12978, FTT-10A, 2-position plug | [CERT-doc] | 12997(LON)-C.txt L12-15 |
| 2 | JACE-8000 = 4 option slots total; up to 4 LON modules; LON1 naming | [CERT-doc] | JACE-8000_MtgWiringGuide_-TRI L39,105,196,629; 12997 L28 |
| 3 | FTT-10A, 26–12 AWG, polarity-insensitive, free topology | [CERT-doc] | 12997(LON)-C.txt L114-118 |
| 4 | 78.125 kbaud fixed (TP/FT-10), no baud setting | [CERT] | B135 |
| 5 | FTT-10 termination differs from RS-485 both-ends 120 Ω (Echelon-governed) | [INFER] | not in our 3 sources; flagged |
| 6 | Commission workflow: LonNetwork → LonSpyder → Discover/service-pin(300s)/Neuron ID → Match → Commission → Download → NV bind | [CERT-doc] | docLonworks; Spyder Tool 31-00089; 63-2662 |
| 7 | `BLonSpyder` + LonSpyderDeviceManagerView on lonworks:LonNetwork; deps lonworks-rt/wb | [CERT] | honeywellLonSpyder module.xml |
| 8 | 48-bit Neuron ID; station = LonWorks network manager (domain/subnet-node/NV) | [CERT-doc]+[CERT] | docLonworks; B77 |
| 9 | License = lonworks | [CERT-doc] | docLonworks.txt |

**Tally:** 9 claims — 6 [CERT-doc], 2 [CERT], 1 [INFER] (termination, explicitly flagged). No unmarked assertions.

## Connections
- **B835** — the BACnet MS/TP path (the default); this block is its LON sibling.
- **B836** — the shared Spyder Tool function blocks (same for LON and BACnet).
- **B77 / B135 / B25.4** — `honeywellLonSpyder` internals, TP/FT-10 encoding, the migrator.

## Open gaps
- **B837-G1** — FTT-10 termination + max node/length exact rules (needs the Echelon FTT-10A guide, outside
  our 3 sources) → close claim 5.
