# B724 — Niagara N4 — web-hmi (WH1): the Honeywell WEB-HMI touchscreen panel family — models, the WEB-HMI10/CF deploy target, the HTML5/Chromium-on-Linux display model, and the responsive/kiosk implications for a served dashboard

> Focus: **web-hmi** (NEW — bootstrapped 2026-08-31). First block of the focus. HARDWARE reference for the
> physical touchscreen panel onto which the project's **DashboardPan** dashboard is being deployed
> (target = **WEB-HMI10/CF**). Distilled from the three Honeywell official datasheets, read in full.
>
> **Sources (the ONLY source — FUENTE 2, manufacturer docs):**
> - `31-00389` — WEB-HMI Touchscreen Monitor *Product Data* (specs: models, resolutions, CPU, display, touch, power). `niagara-help/docs-text/WEB-HMI_Touchscreen_Monitor_Product_Data_-_31-00389.txt`
> - `31-00456` — WEB-HMI Touchscreen IP Monitor *User Guide* (browser/URL config, toolbar, kiosk, BSP). `niagara-help/docs-text/WEB-HMI_Touchscreen_IP_Monitor_User_Guide_-_31-00456.txt`
> - `31-00390` — WEB-HMI Touchscreen Monitor *Installation Instructions* (mounting, power, connectivity). `niagara-help/docs-text/WEB-HMI_Touchscreen_Monitor_Installation_Instructions_-_31-00390.txt`
>
> **Scope:** the panel HARDWARE + its display/programming model + what a served dashboard must respect. NOT the
> DashboardPan module internals (that is the `cold-room-module`/`build-n4-module` work, referenced by name).
>
> **FUENTE-1 (corpus) = ZERO · FUENTE-3 (decompiled code) = ZERO** (this is data, not an omission). Verified
> `python3 tools/corpus-nav.py find "WEB-HMI"` → **"No matches."**; the hardware is absent from `organized/`
> (decompiled Java is the N4 software, not the OEM panel). The datasheets are therefore the SOLE legitimate
> source: every hardware fact below is **[CERT-doc]** with doc# + line; every derived claim is **[INFER]**.
> No claim without a marker.

## 724.1 — The WEB-HMI family: lineup + spec table

[CERT-doc] The WEB-HMI series are "HTML5 IP browser compatible touchscreen monitors" that integrate with CIPer
Model 10/30/50 and WEB-8000 controllers, offered in **2 tiers**: (1) *Direct controller access* (browse in the
controller) and (2) *System-wide controller access* (browse into the system) (31-00389 L7-19; 31-00390 L7-19).

[CERT-doc] Product Data advertises **5 sizes** (31-00389 L12). The User Guide's BSP table lists **6 SKUs** — it
adds a **WEB-HMI7/CF** (7" *capacitive*, SB78) alongside the 7/C resistive (31-00456 L136-143). The `/CF`
suffix = capacitive+True-Glass tier; plain `/C` = resistive tier.

**Table — family lineup** (31-00389 Table 1 L36-58 + Table 2 L303-447; BSP col from 31-00456 L136-143):

| Model | Size | Resolution | Colors | Touch | CPU | RAM | Flash | Ethernet | BSP |
|---|---|---|---|---|---|---|---|---|---|
| WEB-HMI4/C | 4.3" TFT 16:9 | 480×272 (WVGA) | 64K | Resistive | ARM Cortex-A8 1 GHz | 512 MB | 4 GB | 1× (port0 10/100) | UN60 |
| WEB-HMI7/C | 7" TFT 15:9 | 800×480 (WVGA) | 64K | Resistive | ARM Cortex-A9 dual-core 800 MHz | 1 GB | 4 GB | 1× (port0 10/100/1000) | UN65 |
| WEB-HMI7/CF | 7" | (not in 31-00389) | — | Capacitive | — | — | — | — | SB78 |
| **WEB-HMI10/CF** | **10.1" TFT 16:10** | **1280×800 (WXGA)** | **16M** | **Projected capacitive, multi-touch** | **i.MX8M Mini Quad Cortex-A53** | **2 GB** | **4 GB** | **3× (0:10/100/1000, 1:10/100, 2:10/100)** | **SB78** |
| WEB-HMI15/CF | 15.6" TFT 16:9 | 1366×768 (HD) | 16M | Projected capacitive, multi-touch | i.MX8M Mini Quad Cortex-A53 | 2 GB | 4 GB | 3× | SB78 |
| WEB-HMI21/CF | 21.5" TFT 16:9 | 1920×1080 (full HD) | 16M | Projected capacitive, multi-touch | i.MX8M Mini Quad Cortex-A53 | 2 GB | 4 GB | 3× | SB78 |

[CERT-doc] All models run **Linux** and carry an RTC (31-00389 L327). Backup power differs by tier: resistive
4/7 use a **supercapacitor**, the `/CF` capacitive models use a **non-user-replaceable rechargeable Lithium
battery** (31-00389 L368-377). Protection is **IP66 (front) / IP20 (rear)** across the range (31-00389
L399-401). All are **CE** (EN 61000-6-x industrial) and **UL E160970** listed (31-00389 L416-446).

## 724.2 — WEB-HMI10/CF — detailed specs (the deploy target)

[CERT-doc] Display: **10.1" TFT color, 1280×800 pixel (WXGA), 16M colors, 16:10 aspect**, dimmable LED
backlight, **500 Cd/m² typ.** brightness dimmable to 0% (31-00389 L154-157, L305-314). Touch: **projected
capacitive, multi-touch, True Glass** (scratch/UV/chemical resistant) (31-00389 L158-162, L316-319). The
User Guide's BSP table confirms "WEB-HMI10/CF · 10.1 · Capacitive · SB78" (31-00456 L141).

[CERT-doc] Compute: **i.MX8M Mini Quad ARM Cortex-A53** CPU, **Linux** OS, **2 GB RAM**, **4 GB flash**
(31-00389 L321-331). RTC backed by a rechargeable Lithium battery, not user-replaceable; on first install it
must charge **48 h**, then holds date/time ~**3 months** at 25 °C (31-00389 L368-377; 31-00390 L250-259).

[CERT-doc] Connectivity: **3 Ethernet ports** — port0 10/100/1000 (Gb), port1 10/100, port2 10/100 (31-00389
L168, L334-342; 31-00390 L188-190); **2 USB** host V2.0, max 500 mA, "for maintenance only" (31-00389
L344-348; 31-00390 L186). An **SD-card slot** and **2 expansion slots for plug-in modules** are present but
marked **"not supported"** (31-00389 L183-189) — note the Install doc softens SD to "for maintenance only"
(31-00390 L198). ⚠ minor doc discrepancy (see Open gaps).

[CERT-doc] Power/environment: **24 Vdc (10–32 Vdc), 1.0 A at 24 Vdc max** (31-00389 L360-364; 31-00390 L192);
operating temp **−20 °C … +60 °C** (−4 °F … 140 °F, vertical install), storage −20 … 70 °C, humidity 5–85% RH
non-condensing (31-00389 L385-397).

[CERT-doc] Mechanical: faceplate **282 × 197 mm** (11.10" × 7.76"), cutout **271 × 186 mm** (10.67" × 7.32"),
depth 52 + 8–8.5 mm, weight **2.5 kg** (5.51 lb) (31-00389 L403-414; 31-00390 Table 3 L67-75). **Flush /
control-cabinet mounting** (31-00389 L163).

## 724.3 — Display & programming model: Linux + Chromium HTML5, "the compatible file is a URL"

[CERT-doc] The panel is **not** a Niagara Px renderer and has **no** proprietary graphics format. It runs a
**modern HTML5 browser based on Linux** — explicitly an **HTML5 Chromium Browser to access Niagara WEBs-N4**
(31-00389 L16-17, L24-26; 31-00456 L128). What it displays is a **web page reached at a URL**: the *Homepage*
setting "must include `http://` or `https://` … the IP address or hostname of the target device and the
web-page name. For example: `https://192.168.0.85/start.htm`" (31-00456 L985-992).

[CERT-doc] Chromium is a **separately updatable Application** layered over the firmware (Bootloader / MainOS /
ConfigOS). On SB78 (the 10/CF), Chromium updates via *System Settings → Applications*; it survives a Device
Restore (31-00456 Ch.6 L1052-1070, L478). The exact Chromium/engine version is **not stated** in any datasheet
→ see Open gaps (a real constraint on which HTML5/CSS/JS features are safe).

[CERT-doc] Startup behaviour is configured under *Web Browser → On Startup* (31-00456 L972-980):
- **Open homepage** → the specified Homepage loads directly; the panel's own login page does **not** appear (this
  is the kiosk mode for a deployed dashboard).
- **Open settings page** → login page first (commissioning default).
- **Continue where you left** → "Option not supported".

[INFER — links to the module work] The project's **DashboardPan** module serves an HTML5 page from a Niagara
servlet at **`/dashboardpan/`** on the station's WEB server. That is the *native* fit for this panel: it is
exactly "an HTML5 page at a URL on a Niagara WEBs host". Deployment = set the panel Homepage to
`https://<station-host>/dashboardpan/` (hostname, not IP — see §724.5 on the CA cert) and On Startup = *Open
homepage*. No Px, no PxGraph, no special packaging — the servlet's HTML/CSS/JS is what Chromium fetches.
(Derivation: 31-00456 L985-992 URL model + 31-00389 L24-26 Chromium-to-Niagara-WEBs, applied to the
DashboardPan servlet from the `cold-room-module` work; the module itself is not a block — referenced by name.)

## 724.4 — Power / mounting / connectivity essentials (from the Install doc)

[CERT-doc] Mounting: flush cut-out **271 × 186 mm**, fixed with the fixing-bracket kit — **9 pieces** for the
WEB-HMI10/CF (31-00390 L145, Table 3). Capacitive-panel tightening torque **130 Ncm** (or until the bezel
corner contacts the panel) (31-00390 L106-107). The unit is an **open-type device** (except the front) and must
sit in an enclosure of at least IP54 (31-00390 L90-95, L134-139).

[CERT-doc] Power: DC connector, AWG24 wire, female terminal-block pitch 5.08 mm, torque 50 Ncm; input
**10–32 Vdc**, an **extra-low-voltage / limited power source**; all ports are **SELV / Class 2** (31-00390
L166-168, L192, L209-223). The unit **must be grounded to earth** via the screw/faston near the power block,
and the supply must have **double or reinforced insulation** (31-00390 L203-241).

[CERT-doc] Ethernet layout (capacitive rear, 31-00390 L183-198): callout-4 **eth0 = 10/100/1000**, eth1/eth2 =
10/100. **eth0 is the WAN port**, located next to the power connector, and "should be used to provide the
connection to the required web server" (31-00456 L721-724). By default all interfaces are **DHCP**; static IP
is configurable (31-00456 L721-755). The panel has an **internal firewall that blocks all incoming traffic
except ICMP ping** (31-00456 L726-728) — i.e. it is a pure *client* to the Niagara WEB server. An optional
**bridge/switch service** (SB78 only) can share eth0's address across eth1/eth2 (31-00456 L777-793).

## 724.5 — Responsive / kiosk implications for a served dashboard at 1280×800 capacitive

Design constraints the DashboardPan page must respect (facts [CERT-doc]; design consequences [INFER]):

- [CERT-doc]+[INFER] **Fixed native resolution 1280×800, 16:10.** Design/lay out the dashboard for a 1280×800
  viewport; there is no window to resize. (31-00389 L154-155, L309.)
- [CERT-doc] **Capacitive multi-touch, no pointer.** The panel supports pinch/spread **zoom**, two/five-finger
  **scroll (flick)**, and **drag** gestures (31-00456 L270-289). [INFER] There is **no mouse hover** → any
  `:hover`-only menu, tooltip, or affordance is unreachable; make all controls tap-activated. Size touch targets
  for a fingertip (comfortable ≥ ~9–10 mm), not a cursor. (Note: the resistive 4/7 models support **no** gestures,
  31-00456 L275 — irrelevant to 10/CF but relevant if the dashboard must also target those.)
- [CERT-doc] **Kiosk mode.** *On Startup = Open homepage* suppresses the panel login/chrome; an optional
  **toolbar** can be enabled (recommended during commissioning) with "show only on error"; *history buttons* and
  *loading controls* are "Not supported" (31-00456 L974-1013). [INFER] The served page owns 100% of the screen —
  provide in-page navigation; do not rely on browser back/reload chrome.
- [CERT-doc] **Fonts.** The panel does **best-match font substitution** from its own library; web pages may not
  render as intended, but custom **TTF fonts can be uploaded** via USB (31-00456 L859-877). [INFER] Prefer
  system-safe/web-safe font stacks, or pre-install the dashboard's TTF on the panel.
- [CERT-doc] **User-Agent override** available (e.g. "Android") to make a page render correctly (31-00456
  L1022-1023); **Restart-browser-daily** option exists (31-00456 L1030). [INFER] a long-lived kiosk benefits
  from the daily restart to shed any Chromium memory drift.
- [CERT-doc] **HTTPS + certificates.** HTTP is discouraged; the panel has a certificate manager and you should
  **import the Supervisor/station root CA** into the panel and set the Homepage to the controller's **hostname,
  not its IP** (31-00456 L1326-1338, L1430-1431). [INFER] For DashboardPan over Niagara's HTTPS WEB server,
  import the station's TLS root so Chromium trusts it without a warning interstitial (which would break kiosk).
- [CERT-doc] **Virtual keyboard** appears on input focus (layouts EN/IT/DE/FR/ES) (31-00456 L259-268). [INFER]
  If the dashboard has text inputs, expect the on-screen keyboard to cover the lower half — keep inputs in the
  upper region or scroll-into-view on focus.

## Connections

- **DashboardPan servlet / build-n4-module & cold-room-module work** — referenced by name (not blocks); this
  block is the hardware target those produce a page for. §724.3 is the seam.
- **[Block 9] UI Stack** — where the Niagara WEB / servlet / HTML rendering layer sits; the panel is the *client*
  end of that stack (a remote Chromium hitting a station servlet).
- **[Block 657] JACE-9000** and **[Block 672]–[Block 683] JACE-8000 hardware** — sibling *controller* hardware
  blocks; WEB-HMI is the complementary *operator-panel* hardware that browses those controllers' WEBs.
- FUENTE-1/FUENTE-3 = ZERO for this hardware (recorded above) — future sessions should not re-search the corpus
  for WEB-HMI; it is datasheet-only.

## Self-verify

| # | Claim | Marker | Citation | Verified |
|---|---|---|---|---|
| 1 | Family = HTML5 IP-browser monitors, 5 sizes (6 SKUs incl. 7/CF), 2 access tiers | [CERT-doc] | 31-00389 L7-19, L12; 31-00456 L136-143 | verified |
| 2 | WEB-HMI10/CF = 10.1" 1280×800 WXGA 16:10, 16M colors, projected-capacitive multi-touch | [CERT-doc] | 31-00389 L154-162, L305-319; 31-00456 L141 | verified |
| 3 | 10/CF compute: i.MX8M Mini Quad Cortex-A53, Linux, 2 GB RAM, 4 GB flash | [CERT-doc] | 31-00389 L321-331 | verified |
| 4 | 10/CF I/O: 3 Ethernet (eth0 Gb WAN), 2 USB maint-only; SD + expansion "not supported" | [CERT-doc] | 31-00389 L168, L334-348, L183-189; 31-00456 L721-724 | verified |
| 5 | 10/CF power 24 Vdc (10–32), 1.0 A; IP66 front/IP20 rear; cutout 271×186 mm; 2.5 kg | [CERT-doc] | 31-00389 L360-364, L399-407, L412; 31-00390 L67-75 | verified |
| 6 | Panel = Linux + HTML5 Chromium browsing Niagara WEBs-N4; no Px/proprietary format | [CERT-doc] | 31-00389 L16-17, L24-26; 31-00456 L128 | verified |
| 7 | "Compatible file" = a URL: Homepage must be http(s)://host/page (e.g. .../start.htm) | [CERT-doc] | 31-00456 L985-992 | verified |
| 8 | Kiosk: On Startup=Open homepage hides login/chrome; toolbar optional; back/reload not supported | [CERT-doc] | 31-00456 L972-1013 | verified |
| 9 | eth0 WAN is the web-server link; internal firewall blocks all incoming except ICMP | [CERT-doc] | 31-00456 L721-728 | verified |
| 10 | Import root CA + use hostname (not IP) for HTTPS homepage; cert manager present | [CERT-doc] | 31-00456 L1326-1338, L1430-1431 | verified |
| 11 | Capacitive supports zoom/scroll/drag gestures; resistive models support none | [CERT-doc] | 31-00456 L270-289 | verified |
| 12 | DashboardPan servlet at /dashboardpan/ is the native fit → Homepage = https://host/dashboardpan/ | [INFER] | derived from #6+#7 applied to cold-room-module servlet | reasoned |
| 13 | No hover on capacitive → tap-only controls, finger-sized targets; virtual keyboard covers lower half | [INFER] | derived from #11 + 31-00456 L259-268 | reasoned |
| 14 | Prefer web-safe fonts / pre-install TTF; daily browser restart for a long-lived kiosk | [INFER] | derived from 31-00456 L859-877, L1030 | reasoned |

**Tally:** **[CERT-doc] ×11 · [INFER] ×3** (self-verify rows). Body prose carries the same split — every
hardware/spec/behaviour statement is [CERT-doc] with doc#+line; the three derived statements (DashboardPan fit,
no-hover/keyboard design, font/restart advice) are [INFER]. Block TYPE = hardware reference (FUENTE-2 only;
FUENTE-1 = FUENTE-3 = ZERO, recorded).

## Open gaps

- **WH1-G1 (datasheet-limited):** the exact **Chromium/engine version** on SB78 firmware is not stated in any of
  the three docs — it gates which HTML5/CSS/JS features the DashboardPan page can safely use. Needs the panel's
  *System Settings → System → Info* (kernel/build/compatibility code) or the Applications Chromium version on a
  live unit. (31-00456 L602-626, L1220-1228 describe where the version lives, not the value.)
- **WH1-G2 (datasheet-limited):** kiosk/toolbar **lockdown specifics** — how fully the operator UI can be locked
  (press-and-hold-5s and Tap-Tap always reach System Settings; 31-00456 L332-361) — a security/kiosk-hardening
  question, not answered as a policy. Note the tap-hold corner + Tap-Tap recovery paths always exist by design.
- **WH1-G3 (doc inconsistency, minor):** SD slot is "**not supported**" (31-00389 L188-189) vs "**for
  maintenance only**" (31-00390 L198); and USB for WEB-HMI7/C reads "1 USB host port" in ordering info
  (31-00389 L51) but "**No**" in Table 2 (31-00389 L335). Recorded, not blocking the 10/CF deploy.
