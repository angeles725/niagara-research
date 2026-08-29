# B662 — JACE-9000 Host ID and the microSD card: two ID formats (ATLAS-SD-… vs ATLAS-…), a Tridium-secret CID validated at boot, license portability tied to the card, and a hard fail — a non-Tridium card means Niagara will not run (focus jace9000, J9K-11)

> **Focus:** `jace9000` (§16). **Gap:** J9K-11 — the JACE-9000 Host ID depends on the microSD card; what are
> the two formats and the licensing/identity impact of inserting, removing, or replacing the card?
> **Phase:** DISK-FIRST (doc). Read-only. Visible over the serial DEBUG console ([Block 657] banner shows
> `hostid: ATLAS-SD-…`).
> **Sources:** `[CERT-doc]` niagara-help `J9BackupRestore/{AboutHostID, BackupAndRestore, GeneratingHostId}`
> · `[CERT]` corpus [Block 467] (JACE-8000 Host ID) [Block 657].
>
> **Bottom line for the operator:** the JACE-9000 Host ID has **two forms** — `ATLAS-SD-…` when a
> Tridium-programmed microSD card is present (derived from the card), and `ATLAS-…` when it is not (derived
> from the CPU). The card carries a **Tridium secret** checked at every boot via the card's **CID**. Three
> operational hard facts follow: (1) **inserting or removing the card CHANGES the Host ID** — which breaks a
> license pinned to the old ID; (2) the SD-based ID makes the license **portable** to another JACE-9000 by
> moving the card; (3) a **non-Tridium card makes the controller not generate a Host ID at all — Niagara
> will not run**.

## §662.1 — Two Host ID formats

`[CERT-doc]` (`J9BackupRestore/AboutHostID-4869323B.txt`):

- Without a card — CPU-derived: "For a JACE-9000 **without an MicroSD card**, the Host ID is derived from the
  **CPU ID**. It takes the format: **ATLAS-1B22-B800-1CD8-A54B**." (`:26`)
- With a card — card-derived: "For a JACE-9000 **with a MicroSD card**, the Host ID is derived from **data on
  the MicroSD card**. It takes the format: **ATLAS-SD-F93E-14C2-6345-D321**." (`:28`)

So the `-SD-` infix is the tell: `ATLAS-SD-<hex>` = card-based, `ATLAS-<hex>` = CPU-based. The live unit in
[Block 657] showed `ATLAS-SD-…`, i.e. it is running with a Tridium microSD installed.

## §662.2 — The card carries a Tridium secret, validated at boot via CID

`[CERT-doc]` (`J9BackupRestore/GeneratingHostId-342EA243.txt`):

- "All MicroSD cards are programmed with the **Tridium secret**, which validates the authenticity of the
  MicroSD card-based Host ID established using **Card Identification (CID)** values. The CID number is a
  unique identifier or serial number created on the MicroSD card at the time of manufacturing." (`:14`)
- "If no MicroSD card is present, the device uses a CPU-based Host ID. When the MicroSD is present at boot
  time, the MicroSD card is checked for authenticity using the Tridium secret. The Host ID's authenticity
  will be verified. **If the MicroSD card fails authentication, the Host ID is considered invalid**." (`:28-30`)

So the SD Host ID is not merely "read off the card" — it is a **cryptographically-anchored identity**: the
card's manufacturer CID plus a Tridium-programmed secret, re-validated on every boot.

## §662.3 — Portability, and the three hard operational facts

`[CERT-doc]`:

- **Portability:** "To support portability between JACE-9000 devices, the Host ID of a JACE-9000 will change
  to reflect presence of MicroSD card. This ties the license file to the Host ID on the SD card and makes it
  portable." — `AboutHostID-4869323B.txt:24`. Move the Tridium card to another JACE-9000 and the license
  (pinned to `ATLAS-SD-…`) travels with it.
- **Insert/remove changes identity:** "If you insert or remove a microSD card from a JACE-9000 the **Host ID
  will change** for that controller." — `BackupAndRestore-36AF4489.txt:63`. Any license pinned to the prior
  ID stops matching.
- **Non-Tridium card = dead controller:** "Only **Tridium-configured** microSD cards are supported"
  (`BackupAndRestore-36AF4489.txt:56`); "If you install a non-Tridium microSD card in the unit, the system
  **will not generate a Host ID and Niagara will not run**." (`:60`).

## §662.4 — Contrast with the JACE-8000

`[CERT]` The JACE-8000 Host ID was `Qnx-TITAN-…`, hardware-bound to the SoC and **not** portable — cloning a
station to another JACE required a new license ([Block 467]). The JACE-9000 **inverts the portability
model**: by moving the *card* you move the identity (and thus the license) to new hardware — a deliberate
serviceability feature — at the cost of a new failure mode (wrong/absent/non-Tridium card ⇒ invalid Host ID
⇒ Niagara down). Both remain host-pinned in the sense that the license follows an identity you cannot forge;
what changed is *what* the identity is bound to (portable card vs fixed CPU).

## Self-verify

| # | Claim | Marker | Citation | Checked |
|---|---|---|---|---|
| 1 | Without card: CPU-derived Host ID, format ATLAS-… | [CERT-doc] | AboutHostID-4869323B.txt:26 | ✓ grep |
| 2 | With card: card-derived Host ID, format ATLAS-SD-… | [CERT-doc] | AboutHostID-4869323B.txt:28 | ✓ grep |
| 3 | Card has Tridium secret; CID = manufacture serial | [CERT-doc] | GeneratingHostId-342EA243.txt:14 | ✓ grep |
| 4 | Boot-time authenticity check; fail → Host ID invalid | [CERT-doc] | GeneratingHostId-342EA243.txt:28-30 | ✓ grep |
| 5 | Card ties license to Host ID → portable between JACE-9000s | [CERT-doc] | AboutHostID-4869323B.txt:24 | ✓ grep |
| 6 | Insert/remove card changes the Host ID | [CERT-doc] | BackupAndRestore-36AF4489.txt:63 | ✓ grep |
| 7 | Only Tridium cards supported; non-Tridium → Niagara won't run | [CERT-doc] | BackupAndRestore-36AF4489.txt:56,60 | ✓ grep |
| 8 | JACE-8000 Host ID Qnx-TITAN-…, hardware-bound, not portable | [CERT] | [Block 467] | ✓ corpus |

**Marker tally:** [CERT-doc]=7, [CERT]=1 (corpus), [INFER]=0 · ratio [INFER]/[CERT*]=0 · **block type =
EVIDENCE (doc)**. No unmarked claims. Example Host ID strings are Tridium doc placeholders, not this unit's
live value (SECRETS: the live `ATLAS-SD-…` is an identifier, not a secret, but not reproduced here).

## Connections

- [Block 657] — the serial banner exposes `hostid: ATLAS-SD-…`; this block explains what that string is.
- [Block 658] — Platform Access Recovery binds its Tridium-signed reset to the Host id; the card-based ID is
  what that signature targets.
- [Block 467] — JACE-8000 `Qnx-TITAN-…` Host ID: the non-portable predecessor model this inverts.
- [Block 466]/signing-pki [Block 392] — the Tridium-secret/CID anchor is another instance of the
  Tridium-rooted trust chain.

## Open gaps (RESEARCH-STATE-jace9000.md)

Doc-investigable remaining: J9K-12 (COM1/COM2 vs DEBUG). Live-gated: J9K-2, J9K-3, J9K-9, J9K-10. After
J9K-12, investigable_open → 0 (STOP → synthesis + §18 retro).
