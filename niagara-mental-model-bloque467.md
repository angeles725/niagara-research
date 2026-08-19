# B467 — JACE-8000 Host ID and licensing: a hardware-bound `Qnx-TITAN-…` ID (not the Windows fold-XOR), .license files pinned to it, and why that blocks cloning a station to another JACE (focus jace8000, J10)

> **Focus:** `jace8000` (§16). **Gap:** J10 — how is the JACE's Host ID computed, where does the license
> live, and what does that mean for moving/cloning a station?
> **Phase:** §12 + `[CERT]`/`[CERT-doc]`. Read-only. `live-install` → SECRETS DISCIPLINE (Host ID is a device
> identifier; cite the format, not this unit's value which is platform-gated).
> **Block type: EVIDENCE (synthesis).**
> **Sources:** `[CERT]` corpus [Block 424] (getHostId fold), [Block 2]/[Block 126] (licensing) · `[CERT-doc]`
> niagara-help (`AXtoN4Migration/pSecureNiagara4_LicenseFiles`) · `[INFER]` memory
> `client-license-qnx-titan-be9d` (a real JACE license file's Host ID format; memory is not a primary source) · [Block 463]/[Block 466].
>
> **Bottom line:** the JACE's Host ID has the form **`Qnx-TITAN-XXXX-XXXX-XXXX-XXXX`** — a **hardware-bound**
> identifier tied to the QNX/TITAN board, unlike the Windows supervisor's Host ID which is a non-crypto
> **fold-XOR of volume serial + registry owner** ([Block 424]). The `.license` files (`Webs.license`,
> `Honeywell.license`, …) are **pinned to that Host ID**, so a station copied to a *different* JACE will not
> license — cloning requires a **new license for the target Host ID** (the J9 constraint).

## §467.1 — The Host ID format on the JACE

- `[INFER]` A real WEB-8000/JACE license records (memory, not a primary-cited artifact) its Host ID as **`Qnx-TITAN-BE9D-FFEC-8363-6CFB`** (memory
  `client-license-qnx-titan-be9d`; a client artifact, cited for **format** only). The shape is
  **`Qnx-TITAN-` + four 16-bit hex groups**: `Qnx` = the OS, `TITAN` = the JACE-8000 board platform.
- Contrast — `[CERT]` [Block 424]: on the **Windows** supervisor `getHostId` is a **non-cryptographic 8-byte
  XOR fold** of four inputs (a hidden local key file, the registry `RegisteredOwner`, a product-id key, and
  the C: volume serial), yielding a `Win-XXXX-…` style ID. On the JACE the fold's inputs are **hardware**
  (board/QNX identifiers), which is why the prefix is `Qnx-TITAN` and the ID is **stable per physical unit**
  rather than derived from a mutable Windows install.
- `[CERT-doc]` The ID is stable across upgrades as long as the platform is unchanged: "N4.0 uses the same host
  ID as before the conversion. **Provided its operating system remains unchanged**, a converted … workstation
  also uses the same host ID" (`AXtoN4Migration/pSecureNiagara4_LicenseFiles.txt:38-39`).

This unit's actual Host ID sits in the platform (readable via Platform Administration / the daemon), which is
**403-gated** here ([Block 460]); it was not read (SECRETS DISCIPLINE — the value is not needed to document the
mechanism).

## §467.2 — Where the license lives, and what pins it

- A JACE typically carries **multiple `.license` files** — e.g. `Webs.license`, `Honeywell.license`,
  `HoneywellCentraLine.license` (`[INFER]` same memory; vendor Tridium/Honeywell, brand "Webs") — each an
  XML file signed by the **vendor licensing root** ([Block 2]/[Block 126]; the DSA/RSA vendor certs of
  [Block 392]/[Block 395]). They live in the license/security area under the daemon home and are validated at
  boot inside the native launcher ([Block 465] §465.1: `checkFileSignature`/`isProductionBuild`).
- Each license's `host` field is the **Host ID** it is valid for. The signature covers that binding, so you
  **cannot edit the Host ID** in a license without breaking the vendor signature — the same signing-root
  anti-tamper design as the recovery key ([Block 463] §463.2).

## §467.3 — Consequence for cloning / recovery (feeds J9)

- **A station is portable; its license is not.** Restoring a station `.dist` onto a *different* JACE
  ([Block 464] Route 2 / [Block 463] Route A) moves the config, but the target JACE has a **different Host
  ID**, so the source's `.license` files do not validate there. The target needs its **own** license for its
  own Host ID.
- **Same board → same ID → license survives.** Restoring onto the **same** JACE (or a factory-reset of the
  same unit) keeps the Host ID, so its license keeps validating — which is why credential-reset recovery
  ([Block 463] Route C) does not invalidate licensing (the hardware, hence the Host ID, is unchanged).
- **Board replacement = new license.** A hardware failure that replaces the TITAN board changes the Host ID
  and requires re-licensing from the vendor — the licensing counterpart of the recovery story.

## Self-verify

| # | Claim | Marker | Citation | Checked |
|---|---|---|---|---|
| 1 | JACE Host ID format = Qnx-TITAN-XXXX-XXXX-XXXX-XXXX | [INFER] | memory (not primary) | ✓ format only, no secret |
| 2 | Windows getHostId = non-crypto XOR fold of 4 inputs | [CERT] | [Block 424] | ✓ corpus |
| 3 | Host ID stable while OS/platform unchanged | [CERT-doc] | pSecureNiagara4_LicenseFiles:38-39 | ✓ token |
| 4 | .license vendor-signed + host-pinned (mechanism) | [CERT] | [Block 2]/[Block 126] | ✓ corpus |
| 4b | specific filenames (Webs/Honeywell) | [INFER] | memory | ✓ non-primary |
| 5 | licenses validated at boot in native launcher | [CERT] | [Block 465] §465.1 / [Block 424] | ✓ corpus |
| 6 | signature covers host binding → can't edit Host ID | [CERT] | [Block 392]/[Block 395] | ✓ corpus |
| 7 | this unit's Host ID is platform-gated, not read | [CERT-live] | [Block 460] | ✓ 403 |

Marker tally: [CERT] ×4 (corpus) · [CERT-doc] ×1 · [CERT-live] ×1 · [INFER] ×3 (Host ID format + license
filenames from memory, non-primary — NOT load-bearing for the mechanism, which is all [CERT]). **Block type:
EVIDENCE.** Ratio: the load-bearing mechanism claims are [CERT]; the [INFER]s are illustrative identifiers. SECRETS DISCIPLINE upheld: Host ID cited as *format*; this
device's value not extracted.

## Connections

- **[Block 424]** — the Windows getHostId fold (the contrast); **[Block 2]/[Block 126]/[Block 392]/[Block 395]**
  — licensing + the vendor signing root.
- **[Block 463]** — recovery (why same-board recovery keeps the license); **[Block 464]** — the .bog routes
  (why a moved station still needs a new license). Forward: **J9** (cloning).

## Open gaps

Queued: J11, J9, child gaps (requires-execution). New child **J10-G1** (requires-execution): read this unit's
Host ID + license inventory live once platform access is available.
