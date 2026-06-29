# Block 136 — Sox/Soxs presence audit in N4 (negative finding): the legacy NiagaraAX Sox protocol is ABSENT from this OptimizerSupervisor install — only vestigial Sedona-management UI labels and orphan icons survive; the station role Sox once shared is carried by Fox

> Research of whether the **legacy NiagaraAX Sox protocol** (the lightweight binary sibling of Fox,
> used by Sedona Framework device tooling / `SoxClient` over UDP) exists in the shipped **Niagara
> N4.14.0.162 OptimizerSupervisor** module set, and — since this is a **rigorous NEGATIVE finding** —
> what carries the role Sox played. This is the FINAL static gap (P6) of the `protocols` focus and it
> CLOSES the static loop. Scope: the 973 module jars of this Supervisor install (an x64-Windows
> Supervisor, NOT a JACE) + the candidate transport/provisioning jars; it does NOT cover a JACE's own
> Sedona runtime (a JACE may carry the optional N4 Sedona Driver — out of this install's scope).
> READ-ONLY. Corpus language: ENGLISH.
>
> Sources (primary, from the live install):
> `/mnt/c/Honeywell/OptimizerSupervisor-N4.14.0.162/modules/` (973 module jars — the directory listing
> itself is the negative evidence) + extracted class/lexicon/resource entries of the candidate jars
> `baja.jar`, `platform-rt.jar`, `fox-rt.jar`, `niagaraDriver-rt.jar`/`-wb.jar`,
> `provisioningNiagara-wb.jar`/`-ux.jar`, `platDaemon-rt.jar`/`-wb.jar`, `platCrypto-rt.jar`,
> `icons-ux.jar`, `themeLucid-ux.jar`, `bajaui-wb.jar`, `workbench-wb.jar`, `platHwScan-rt.jar`.
> Method: `unzip -l` (class/resource name listing) + extract + `grep -a` over class constant pools and
> `.lexicon` files + a raw-byte `LC_ALL=C grep` backstop across all 973 jars + `javap -p`. External
> corroboration preserved: `sources/manuals/ccontrols-sedona-sox-whitepaper.pdf` (sha256 649fcc52…),
> registered in `SOURCES.md`.
> Markers:
> `[CERT]` local primary source (`file:line` / listing) · `[CERT-doc]` downloaded doc (`sources/...pdf §`) ·
> `[CERT-web]` official/secondary web (URL + date) · `[CERT-a]` secondary source/forum · `[INFER]` deduction.
>
> Layer 26 (Communication protocols — wire-level focus). Connects [Block 134] (Fox — Sox's sibling /
> the protocol that carries Sox's station role), [Block 129] (platform.fox daemon — the surface that
> now manages Sedona on a JACE), [Block 27] / [Block 13] (Niagara Network / session muxing over Fox),
> and [Block 131] / [Block 132] / [Block 133] / [Block 135] (sibling wire-level protocol blocks).

---

## 136.1 — What Sox is, and the hypothesis under test `[CERT-doc]` `[CERT-web]`

**Sox** is Tridium's lightweight protocol for talking to **Sedona Framework** devices — the small
embedded C controller framework (a separate runtime from the Java Niagara station). The Sedona tooling
(**SAE — Sedona Application Editor**) "connects to the Sedona device using the **SOX protocol**" over an
IP connection to read/write points and edit the device's wiresheet `[CERT-doc]`
`sources/manuals/ccontrols-sedona-sox-whitepaper.pdf` lines 163, 289-290. In NiagaraAX, Sox was the
**lightweight sibling of Fox** (B134): where Fox is the heavyweight station/Workbench RPC+streaming
protocol (TCP 1911/4911, full SCRAM auth, named channels + numbered circuits — B134), Sox was the thin
binary protocol for Sedona devices. Niagara reaches Sedona devices via an optional **N4 Sedona Driver**
installed on a Workbench or JACE, which "can be used to communicate via Sedona (**SOX**) to Sedona
devices" `[CERT-web]` ccontrols.com material (accessed 2026-06-29).

**Hypothesis (from the protocol audit):** Sox is ABSENT or replaced in N4.14. This block PROVES the
presence/absence from the install's evidence and documents what carries Sox's role. The audit-level
claim "there is no `sox-rt.jar` in this install" is verified below at §136.2 and bounded at §136.4.

> Note on the unrelated "SOX": Sarbanes-Oxley ("SOX compliance") is a financial-controls term with no
> relation to the Tridium Sox protocol. No such mention was found here; this block is strictly the
> Sedona protocol.

## 136.2 — Negative evidence #1: NO Sox module, NO Sox class anywhere in the 973 jars `[CERT]`

The Sox protocol would ship as a `sox` module (`sox-rt.jar`, with a `BSoxService` and a `SoxClient`),
exactly as Fox ships as `fox-rt.jar` with `BFoxService`/`FoxSession` (B134 §134.1). It does not exist
here:

| Search | Result | Evidence |
|---|---|---|
| `ls modules/ \| grep -i sox` (any sox jar) | **none** (grep exit 1) | directory listing of 973 jars — no `sox-rt.jar`/`soxs-rt.jar`/any `*sox*.jar` `[CERT]` |
| `ls modules/ \| grep -i sedona` (any sedona jar) | **none** | no `sedona-rt.jar` either — the optional N4 Sedona Driver is NOT installed on this Supervisor `[CERT]` |
| `unzip -l <each jar> \| grep` for a `*sox*.class` / `*sedona*.class`, all 973 jars | **only** `javax/baja/ui/text/parsers/SedonaParser.class` (bajaui-wb.jar) | the sole "sedona" class is a **Workbench text-editor syntax highlighter** (see §136.3) `[CERT]` |
| `grep -a "Sox\|SoxClient\|BSoxService\|SoxService\|BSox"` across extracted candidate jars (baja, platform-rt, fox-rt, niagaraDriver-rt/wb, provisioningNiagara-wb/ux, platDaemon-rt/wb, platCrypto-rt) | **zero hits** | no Sox class reference, import, or string constant in any transport/provisioning jar `[CERT]` |
| Raw-byte backstop `LC_ALL=C grep -aoE "BSoxService\|SoxClient\|SoxService\|BSox[A-Za-z]+\|com/tridium/sox\|sox-rt"` across **all 973 jars** | **ZERO** | catches stored/uncompressed entries too — no Sox protocol class token exists in the install `[CERT]` |

**There is no Sox protocol implementation in this install.** Not a module, not a class, not a class
reference — confirmed at three independent levels (jar listing, per-jar class scan, raw-byte sweep).

## 136.3 — What DOES survive: vestigial Sedona-management UI, not the protocol `[CERT]`

The absence is **bounded, not total silence** — the *management* surface for Sedona-on-a-JACE survives
as platform-admin labels and orphan icon resources. None of it is the Sox protocol codec; it is the
Supervisor/Workbench UI that drives a *remote* JACE's Sedona daemon through the **platform** channel:

| Residual artifact | Where | What it is | Cite |
|---|---|---|---|
| `x16/sox.png`, `soxDisconnected.png`, `soxTunnel.png`, `soxTunnelDisconnected.png` | `icons-ux.jar`, overrides in `themeLucid-ux.jar` | **orphan 16px icons** — the AX Sox + "Sox tunnel" iconography; the tunnel family also has `foxTunnel*`/`platformTunnel*` siblings | `icons-ux.jar` listing `[CERT]` |
| `PlatformAdministration.command.enableSedona.icon = module://icons/x16/sox.png` / `disableSedona.icon = …/sox.png` | `platDaemon-wb.lexicon:103,108` | the Platform Administration **enable/disable Sedona Support** commands reuse the sox icon | `[CERT]` |
| `SedonaSurrogateView.extraDetails = sox={0},http={1}` | `platDaemon-wb.lexicon:1312` | a surrogate view that reports the managed JACE's **Sedona daemon sox port and http port** — UI display string, not a connection | `[CERT]` |
| `PlatformAdministration.sedonaSupport = Sedona Support` + `.enabled/.disabled/.unsupported` | `platDaemon-rt.lexicon:50-53` | platform daemon reports whether the connected platform supports Sedona | `[CERT]` |
| `AppSurrogate.sedona.display = sedona application {0}` / `Sedona.notAcceptingMessages = …sedona app…` | `platform-rt.lexicon:258,1072` | platform-side surrogate for a Sedona *application* on the device | `[CERT]` |
| `sedonaWireless485OptionCard = Sedona Wireless RS-485 Option Card` | `platHwScan-rt.lexicon:56` | a hardware option-card label | `[CERT]` |
| `DeviceCnxType.icon = module://icons/x16/sox.png` | `workbench-wb.lexicon:447` | a device-connection-type enum that still points at the sox icon | `[CERT]` |
| `javax.baja.ui.text.parsers.SedonaParser` extends `javax.baja.ui.text.TextParser` (`parseIdentifier`/`parseNumberLiteral`/`isKeyword`) | `bajaui-wb.jar` | a **syntax highlighter for the Sedona programming language** in the Workbench text editor — NOT the Sox wire protocol | `javap -p` `[CERT]` |

**Interpretation:** N4 keeps the ability to *manage* Sedona support on a remote JACE (enable/disable,
show the JACE's sox/http ports) through the **platform daemon** (B129), and to *edit* Sedona source in
Workbench — but the **Sox protocol stack itself is not present** in this Supervisor. The icons and the
`DeviceCnxType` enum are leftovers carried forward from the AX heritage `[INFER]` (orphan resources
with no implementing class behind them in this install).

## 136.4 — Negative evidence #2: bounding the search (exactly which jars, so the negative is not a blanket claim) `[CERT]`

The absence claim is bounded to a **named, reproducible search set**, per methodology (an absence is
evidenced by (a) no module + (b) no residual implementation classes across explicitly-searched jars):

- **All 973 module jars** — raw-byte token sweep for `BSoxService|SoxClient|SoxService|BSox*|com/tridium/sox|sox-rt` → **0** `[CERT]`; and per-jar `*sox*.class`/`*sedona*.class` name scan → only `SedonaParser` (a UI highlighter) `[CERT]`.
- **Constant-pool (extracted + `grep -a`) on the candidate transport/provisioning/platform jars**: `baja.jar`, `platform-rt.jar`, `fox-rt.jar`, `niagaraDriver-rt.jar`, `niagaraDriver-wb.jar`, `provisioningNiagara-wb.jar`, `provisioningNiagara-ux.jar`, `platDaemon-rt.jar`, `platDaemon-wb.jar`, `platCrypto-rt.jar` → **no `Sox`/`SoxClient`/`BSoxService` string constant** in any `[CERT]`.

Port note: the historical AX Sox UDP port is commonly cited as **1876** `[CERT-a]` (community/integration
docs). A text search for the literal `1876` across all jars' lexicons/xml/txt hit only **unrelated**
contexts (`docDeveloper-doc.jar`, `honeywellLonSpyder.jar`, `opcUaCore-rt.jar`, `weather-rt.jar`) — **none
is a Sox port reference** (no Sox class binds them). So port 1876 is **not confirmed in this install's
code**; it is reported here only as the documented AX value `[CERT-a]`/`[INFER]`, deliberately NOT
escalated to `[CERT]`.

## 136.5 — What carries Sox's role in N4: Fox + platform.fox `[CERT]` `[INFER]`

Sox in AX served two roles; in N4 both are carried by the **Fox family** documented in B134/B129:

**(1) Station-to-station / Niagara-Network comms, discovery and provisioning → Fox (B134).** The Niagara
Network driver `com.tridium.nd.BNiagaraStation` connects over **Fox**, not Sox — its constant pool
references `com.tridium.fox.sys.BFoxClientConnection`/`BFoxServerConnection`, `FoxSession`, `FoxMessage`,
and a `configureFoxClientConnection` method `[CERT]` `BNiagaraStation.class` (string constants). Station
**discovery** runs over Fox too: `com.tridium.nd.BStationDiscoveryJob` references
`com.tridium.fox.session.MulticastServer` + `BFoxService` + `BLearnStation` `[CERT]`
`BStationDiscoveryJob.class` — i.e. it uses the Fox multicast group (224.0.1.84:1911, B134 §134.10) where
AX would have used Sox's own UDP. The Niagara Network scheme `BNSpaceScheme` is wired as
`service:niagaraDriver:NiagaraNetwork` over `BStationScheme` `[CERT]` `BNSpaceScheme.class`. **No Sox
connection/tunnel class exists** (`grep -ai soxtunnel|BSoxTunnel|SoxConnection` over all extracted jars →
none `[CERT]`).

**(2) Sedona-device management → the platform daemon (platform.fox, B129).** The enable/disable Sedona
commands and the `SedonaSurrogateView` (§136.3) live in `platDaemon` and run over the **platform**
protocol surface B129 documented (`platform.fox` chunked Fox, daemon 3011/5011) — i.e. N4 manages a
JACE's Sedona daemon *through the platform channel*, not through a station-resident Sox driver. The Sox
*device* protocol itself, when used at all in N4, is an **optional add-on `N4 Sedona Driver`** installed
on the JACE/Workbench that actually talks to Sedona devices `[CERT-web]` — and it is **not installed in
this OptimizerSupervisor** `[CERT]` (§136.2).

**Conclusion `[INFER]`:** Sox is **absent from this N4.14 Supervisor** because (a) Sedona is a
JACE/embedded-controller feature that a Supervisor does not host, and (b) the cross-station role Sox
shared in AX is, in N4, the job of **Fox** (B134) for the Niagara Network and of the **platform daemon**
(B129) for device management. The hypothesis is CONFIRMED with the refinement that Sox is not "removed
from N4 the platform" but rather "an optional driver, simply not present in this install," with only
vestigial management-UI residue surviving here.

## 136.6 — Self-verify

- **Token check (presence claims)**: grep-confirmed every load-bearing `[CERT]` token in its cited
  source — `platDaemon-wb.lexicon:103,108,1312` (`enableSedona…sox.png`, `SedonaSurrogateView…sox={0},http={1}`),
  `platDaemon-rt.lexicon:50-53` (`sedonaSupport`), `platform-rt.lexicon:258,1072`, `platHwScan-rt.lexicon:56`,
  `workbench-wb.lexicon:447`, `icons-ux.jar` (`x16/sox.png`+tunnel family), `SedonaParser.class` (javap
  signature), `BNiagaraStation.class`/`BStationDiscoveryJob.class`/`BNSpaceScheme.class` (Fox/`BFox*`/
  `MulticastServer` constants). The `[CERT-doc]` Sox lines verified in
  `sources/manuals/ccontrols-sedona-sox-whitepaper.pdf` (lines 163, 289-290).
- **Negative finding bound (the core deliverable)**: the absence is bounded, not blanket —
  **jars searched at constant-pool depth**: `baja`, `platform-rt`, `fox-rt`, `niagaraDriver-rt`,
  `niagaraDriver-wb`, `provisioningNiagara-wb`, `provisioningNiagara-ux`, `platDaemon-rt`,
  `platDaemon-wb`, `platCrypto-rt`; **plus a raw-byte token sweep across all 973 jars**. Result: **0**
  Sox protocol classes / references (`sox-rt`, `BSoxService`, `SoxClient`, `SoxService`, `BSox*`,
  `com/tridium/sox`) and **0** `sox`/`sedona` jars; the only `*sedona*` class is a UI text highlighter.
- **Marker tally**: ~30 `[CERT]` · 2 `[CERT-doc]` (whitepaper Sox lines) · 1 `[CERT-web]` (N4 Sedona
  Driver/Sox) · 1 `[CERT-a]` (port 1876, deliberately un-escalated) · 4 `[INFER]` (orphan-resource
  reading; Supervisor-doesn't-host-Sedona; "Fox carries the role" deduction; overall conclusion).
  **[INFER]/[CERT] ratio ≈ 0.13** — low: the absence and the surviving residue are source-confirmed; the
  inference is confined to the *interpretation* of why Sox is gone and what replaced it, exactly as a
  rigorous negative finding should be.
- **Artifacts**: block file written; `sources/manuals/ccontrols-sedona-sox-whitepaper.pdf` preserved +
  registered in `SOURCES.md`; `INDEX.md` Layer 26 + `RESEARCH-STATE-protocols.md` updated (P6 covered,
  static loop STOPPED); CATALOG regenerated.

## 136.x — Connections

- **[Block 134]** — **Fox is Sox's sibling and its N4 successor for the station role.** B134 documented
  the full Fox wire (text frame envelope, 7 opcodes, FoxMessage codec, channel/circuit muxing,
  SCRAM-SHA-256, 1911/4911, multicast 224.0.1.84:1911). B136 shows the *lightweight* sibling Sox is
  absent here and that the Niagara Network (`BNiagaraStation`/`BStationDiscoveryJob`) speaks Fox —
  `BFoxClientConnection`/`FoxSession`/`MulticastServer` — for the comms+discovery role AX split between
  Fox and Sox.
- **[Block 129]** — **platform.fox daemon now manages Sedona.** The surviving Sedona surface
  (enable/disable Sedona Support, `SedonaSurrogateView` sox/http ports) lives in `platDaemon` and runs
  over the platform daemon B129 documented (3011/5011, chunked `platform.fox`) — i.e. N4 manages a JACE's
  Sedona daemon via the platform channel, not a station Sox driver.
- **[Block 27] / [Block 13]** — **Niagara Network / session muxing.** B27/B13 described the Niagara
  Network and Fox session multiplexing at the architecture level; B136 confirms at the code level that
  the network driver's transport is Fox (`BNSpaceScheme` → `service:niagaraDriver:NiagaraNetwork` over
  `BStationScheme`, connections via `BFoxClientConnection`), with no Sox path.
- **[Block 131] / [Block 132] / [Block 133] / [Block 135]** — sibling wire-level protocol blocks
  (Modbus, OPC, BACnet, LON). B136 is the focus's **negative finding**: where those four documented a
  present protocol's bytes, B136 documents a protocol's **absence** — the disciplined complement that
  closes the static `protocols` loop.
</content>
