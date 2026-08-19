# B463 — Recovering a JACE-8000 station without platform access: three hardware routes, and why credential reset needs a Tridium-signed key (focus jace8000, J7)

> **Focus:** `jace8000` (§16). **Gap:** J7 — you have a working JACE but no valid platform credentials /
> System Passphrase (ownership change, lost password). How do you recover the station?
> **Phase:** §12 dynamic + `[CERT-doc]`. Read-only (no destructive action taken on the live unit).
> **Sources:** `[CERT-doc]` niagara-help (`BackupRestore/ResettingPlatformAccountCredentials`,
> `.../JACE-8000USBBackaupAndRestoreFeatur`, `J9Startup/J8RecoveryTips`) · `[CERT]` corpus [Block 392]/[Block 395]
> (the Tridium signing root), [Block 460]/[Block 462] (this focus) · `[CERT-ref]` memory `web8000-jace-factory-commissioning`.
>
> **Bottom line:** because the JACE is an appliance, recovery does NOT go through the network platform login
> at all — it uses **hardware routes below the daemon**: (A) restore a **USB clone backup**, (B) reset to
> **factory defaults** (BACKUP button, total wipe), or (C) **Platform Account Recovery** — a serial-console
> procedure that resets the platform credentials **and** System Passphrase **while keeping station data**, but
> only with a **cryptographic authorization key issued by Tridium** for your hostId + proof of ownership. You
> cannot unilaterally reset credentials; the vendor signature is the anti-theft gate — which is also the wall
> a pure protocol-RE attack (J8) runs into.

## §463.1 — The scenario, and the feature that fits it

`[CERT-doc]` `BackupRestore/ResettingPlatformAccountCredentials-49BA99F8.txt`:
- "Occasionally a situation will arise where you have a **functional JACE-8000 controller but no valid
  credentials or system passphrase**. This could be due to a **change in building ownership or control
  contractors**. In Niagara 4.4 and later, the **Platform Account Recovery** feature provides … a secure
  method of regaining access to the JACE-8000 **without losing station data and configuration**." (§lines 11-16)

That is precisely the operator's question. Note what it preserves (**station data + configuration**) and what
it resets (**platform credentials + System Passphrase**).

## §463.2 — Route C: Platform Account Recovery (serial + Tridium signature)

`[CERT-doc]` same file — the full multi-stage procedure:

**Prerequisites:** a **USB-to-micro-USB cable** (phone-style) to the controller's Debug port; a **terminal
emulator / system shell** (e.g. **PuTTY**); ability to reach **Tridium Support by phone or email**; the
controller **powered off** to start. "This process could conceivably take several hours."

**Steps (verbatim shape):**
1. Power off the controller.
2. Open a **serial connection** with the serial-shell program.
3. Power up; during boot **press ESC** to enter **recovery mode → Alternate Boot Options**.
4. In the menu, enter **`8`** → "**Reset Platform Credentials**"; confirm **`Y`** ("reset platform credentials
   **and system passphrase**").
5. The **Platform Access Recovery** screen shows the controller's **hostId**, **OS version**, a **randomly
   generated token**, and instructions.
6. Contact your Support channel; request a credential/passphrase reset **for that hostId**; provide the
   required **"proof of ownership."**
7. Enter the **customer name** in the screen.
8. Contact **Tridium** with the **token + hostId + customer name**.
9. Tridium validates identity **via Niagara Licensing** and generates a **"Signature" containing a Reset
   Authorization Key**, sent to you.
10. Enter the key — **valid only 24 hours** from generation, else restart from step 1.

**The load-bearing fact:** the reset is gated by a **Tridium-signed authorization key** bound to the hostId.
This is the same asymmetric-signature root the corpus documented — the hidden vendor signing key in `baja.jar`
([Block 395] `masterPublicKeyData`; [Block 392] the DSA/RSA vendor roots). The controller verifies the
Signature offline against that embedded root, so **no one — including an attacker with full serial access —
can reset credentials without Tridium issuing the key.** Recovery ≠ bypass.

## §463.3 — Route A: restore a USB clone backup (if you have one)

`[CERT-doc]` `BackupRestore/JACE-8000USBBackaupAndRestoreFeatur.txt`:
- A **clone backup** made via the USB button "**contains a complete image of the platform and station**"
  (:12-13) — made **without Workbench** (:12).
- Restore it with a **USB flash drive + USB-to-microUSB cable + terminal emulator** (:52). USB media must be
  **FAT32/FAT32X** (:43), flash sticks **≤128 GB**, no bus-powered external HDDs (:47-49).
- A backup made by the *Workbench* BackupService instead is restored with the **Distribution File Installer**
  (:34) — a platform tool, so that path needs platform access; the USB clone path does not.

If a clone image exists, this is the fastest full recovery and needs **no platform login and no Tridium call**
— you own the image.

## §463.4 — Route B: recover factory defaults (total wipe)

`[CERT-doc]` same USB file:
- "**Recover the factory default image.** This feature **does not require a USB flash drive, special cable or
  terminal emulator.** The system **pulls the factory image from non-volatile, read-only memory.**" (:54-55)
- `[CERT-doc]` Triggered by **holding the BACKUP button during power-up**: "**while holding the Backup button
  during power up/boot up … initiates a factory recovery image, it restores the controller to its factory ship
  state**" (`BackupRestore/DocumentChangeLogUSBBackup-7F731B6A.txt:101-102`). `[INFER]` operational detail
  (memory `web8000-jace-factory-commissioning`, not a citable primary source): BACKUP LED fast-blink → ~10 s
  countdown → cycle power → ~20-40 min to "niagarad startup complete".
- Safety interlock (N4.7U1+): if **any USB device is inserted** in the backup/restore port, factory recovery
  is **skipped** (:58-61) — so remove USB media first when you actually want a factory wipe.

This **erases the station** back to ship state — use only when the data is expendable or unrecoverable. It is
the last resort, and unlike Route C it needs neither Tridium nor a passphrase.

## §463.5 — Decision tree (lost platform access)

```
Do you have a clone/backup image you trust?
├─ YES → Route A: USB clone restore (or Distribution File Installer for a WB backup). No Tridium, no wipe.
└─ NO
   ├─ Need the station data/config kept? → Route C: Platform Account Recovery (serial, option 8).
   │     Resets creds + passphrase; KEEPS the station. Requires a Tridium-signed key (hostId + proof of
   │     ownership; 24 h validity). Caveat: fields sealed by the OLD passphrase become unreadable
   │     (passphrase mismatch — see J6) — the .bog structure survives, its encrypted secrets do not.
   └─ Data expendable / box must be reclaimed clean → Route B: Factory Defaults (BACKUP button, no USB).
         Total wipe to ship state. No Tridium, no passphrase.
```

## §463.6 — What this says about J8 (RE to grab a .bog)

Two walls are now explicit: (1) the network `.bog` route is **platform-login-gated** ([Block 462] Route 1);
(2) the credential-reset escape hatch is **Tridium-signature-gated** (§463.2). So a purely offline attacker
with serial access can **factory-wipe** the box (Route B) or **restore their own image** (Route A), but cannot
**read the existing station's secrets** without either the platform passphrase or a Tridium-issued key. RE of
the platform protocol (J8) can at best reach the *platform-login-gated* file transfer — it does not defeat the
passphrase encryption of protected `.bog` fields or the Tridium signature. That boundary is the J8 verdict in
advance; J8 will test how far the RE actually gets.

## Self-verify

| # | Claim | Marker | Citation | Checked |
|---|---|---|---|---|
| 1 | Platform Account Recovery (N4.4+) regains access without losing station data | [CERT-doc] | ResettingPlatformAccountCredentials:11-16 | ✓ token |
| 2 | Serial + ESC→Alternate Boot Options→option 8 resets creds AND passphrase | [CERT-doc] | same file (steps) | ✓ tokens "press ESC","enter 8","Reset Platform Credentials" |
| 3 | Screen shows hostId + token; Tridium generates a signed Reset Authorization Key | [CERT-doc] | same file | ✓ tokens "Reset Authorization Key","Signature" |
| 4 | Key valid only 24 h | [CERT-doc] | same file | ✓ token "24-hour" |
| 5 | Signature verified against the embedded Tridium root | [CERT] | [Block 392]/[Block 395] | ✓ corpus |
| 6 | USB clone backup = complete platform+station image, made without Workbench | [CERT-doc] | JACE-8000USBBackaupAndRestoreFeatur:12-13 | ✓ |
| 7 | Restore clone via USB+microUSB+terminal; FAT32; ≤128 GB | [CERT-doc] | same:52,43,47 | ✓ |
| 8 | Factory defaults pulled from read-only NVRAM; no cable; USB-insert interlock skips it | [CERT-doc] | same:54-55,58-61 | ✓ |
| 9 | Factory recovery = BACKUP button hold during power-up | [CERT-doc] | DocumentChangeLogUSBBackup:101-102 | ✓ token |
| 10 | LED/timing of factory recovery | [INFER] | memory (not primary) | ⚠ non-CERT, flagged |

Marker tally: [CERT-doc] ×8 · [CERT] ×1 (corpus) · [INFER] ×1 (quarantined LED/timing detail). Load-bearing
claims are all [CERT-doc]/[CERT]. **Block type: EVIDENCE (procedure).** Ratio ≈ 0.03.

## Connections

- **[Block 462]** — the four filesystem routes; Routes A/B/C here are the *recovery* subset that bypass the
  platform login. **[Block 460]** — why the network route is platform-gated.
- **[Block 392]/[Block 395]** (`signing-pki`) — the embedded Tridium signing root that validates the Reset
  Authorization Key; recovery reuses the vendor-PKI anti-theft design.
- Forward: **J6** (System Passphrase / passphrase mismatch after reset), **J8** (RE verdict pre-stated here),
  **J10** (hostId — the identity the recovery key is bound to).

## Open gaps

Queued: J8, J2, J6, J9, J10, J11, J3-G1, J5-G1. New child **J7-G1**: the exact Alternate Boot Options menu of
the *JACE-8000* (vs the Edge/JACE-9000 wording) — a live serial capture, requires physical Debug-port access
(requires-execution / hardware).
