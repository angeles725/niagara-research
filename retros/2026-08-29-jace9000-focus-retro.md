<!-- kit-retro: jace9000 focus · 2026-08-29 · scope: DYNAMIC-SETUP §1b/§5 + METHODOLOGY §12 -->

# §18 Self-Retrospective — focus: jace9000 (2026-08-29)

**Corpus:** niagara-research · **Focus:** jace9000 (B657–B664, 8 blocks)
**Run:** 2026-08-29 · blocks B657–B664, gaps 9/13 closed, 4 blocked-live
**Retro agent:** §18 fresh-context self-retrospective (propose-only; no kit edits)

---

## Kit files deduped against

- `research-sdd/PROMPT-LOOP.md` (full text, 876 lines)
- `research-sdd/METHODOLOGY.md` (full text, 2328 lines)
- `research-sdd/toolbelt/DYNAMIC-SETUP.md` (full text, 228 lines)

---

## Summary of proposed deltas

| # | Title | Priority | Type | Kit target |
|---|---|---|---|---|
| D1 | usbipd USB-serial bridge → `/dev/ttyUSB0` with operator-coordination protocol | HIGH | PROMOTE | DYNAMIC-SETUP.md §1b or new §1c |
| D2 | VENDOR-MENU-ENUM-IS-FIRMWARE-VERSION-DEPENDENT | HIGH | PROMOTE | METHODOLOGY.md §12 + PROMPT-LOOP.md INVESTIGATE |
| D3 | PRE-INTERACTION MUTATION MAP for live menu-driven devices | MED | ABSORB | METHODOLOGY.md §12 "Read-first, write-supervised" |
| D4 | Multi-marker bootstrap fusion ([CERT-doc]+[CERT-web]+[CERT]+[CERT-live] in one block) | LOW | PROMOTE | METHODOLOGY.md §12 or PROMPT-LOOP.md NORMAL CYCLE step 4 |

Candidate NOT proposed (dedupe):
- Credential hygiene (SECRETS DISCIPLINE): already in PROMPT-LOOP SECRETS DISCIPLINE + DYNAMIC-SETUP §5/§6.

---

## D1 — usbipd USB-serial bridge → `/dev/ttyUSB0` + operator-coordination protocol

**Priority:** HIGH
**Type:** PROMOTE (new content — gap between §1b and §5)

### Evidence

- B657 §657.4 and RESEARCH-STATE-jace9000.md: "the operator bridges the FTDI to WSL via usbipd (`BIND-una-vez-ADMIN.bat` then `CONECTAR.bat` → `/dev/ttyUSB0`) and the driver reads it read-only at 115200 8N1."
- RESEARCH-STATE line 9-11: "an external FTDI FT232 USB-serial converter, `0403:6001`, bridged to WSL as `/dev/ttyUSB0` via usbipd when the operator attaches it."
- Commits B657 (bootstrap block, 2026-08-29).

### Deduplication result

DYNAMIC-SETUP.md §1b (lines 38-61) already documents usbipd-win for generic USB devices. It covers: `usbipd list` / `bind` / `attach` / `detach`, the "Windows loses the device at this point" note, and the detach-verified safe-state gate. **But** it is scoped to USB DEVICES broadly (printers, scanners) and does not cover the specific sub-case of a USB-SERIAL CONVERTER (FTDI FT232, CP210x, CH340 etc.) whose sole purpose is to give the operator a Windows COM port for their own terminal (PuTTY / TeraTerm). For that sub-case:

1. The operator ALREADY has the device attached (their PuTTY window is open on COM5). Attaching it to WSL kicks them off their own terminal — this is a forced operator-coordination step that §1b does not mention.
2. Inside WSL, the device appears as `/dev/ttyUSB0` (or `/dev/ttyACM0` for CDC-ACM type), enabling direct Linux serial tools (`minicom`, `screen`, `python -c "import serial"`) — the complement to §5's PowerShell-interop path.
3. The correct discipline: coordinate with the operator before attaching ("I will need to borrow COM5; you will lose your PuTTY connection"); detach promptly when the capture is done so the operator regains the port.

DYNAMIC-SETUP.md §5 (Serial / COM console acquisition) covers only the PowerShell-interop path, framing WSL as having "no `/dev/ttyS` for a host COM port." That framing is correct for built-in COM ports but misleading for USB-serial converters: usbipd provides the alternative path to `/dev/ttyUSB0` that §5 does not document.

### Proposed landing

**DYNAMIC-SETUP.md §1b** — add a sub-section after the USB/IP common gotchas:

> **§1c. USB-serial converter bridge (FTDI / CP210x / CH340 → `/dev/ttyUSB0`)**
>
> A USB-serial converter that enumerates on Windows as `COM<n>` (e.g. FTDI FT232, `0403:6001`) can be bridged to WSL via the same `usbipd` workflow above, giving Linux serial tools direct access as `/dev/ttyUSB0` (or `/dev/ttyACM0` for CDC-ACM). This is an ALTERNATIVE to §5's PowerShell-interop path and is preferable when the gap requires Linux-native serial tooling (e.g., `minicom`, `python pyserial`, `picocom`).
>
> **Operator-coordination obligation (distinct from the generic §1b device case).** The operator is likely already connected to the same port through a Windows terminal (PuTTY, TeraTerm). Attaching via usbipd kicks them off their own terminal session. Before attaching:
> 1. Confirm with the operator that they can afford to lose the port temporarily.
> 2. After the read-only capture is preserved in `sources/probes/`, detach immediately: `usbipd detach --busid <X-Y>`.
> 3. Confirm the operator regained their COM<n> device before ending the interaction.
>
> This is the detach-verified safe-state gate (§1b) applied to a shared-serial-port context: "safe-state" = the operator's terminal is back.
>
> **Evidence:** niagara-research jace9000 B657 — FTDI FT232 (`0403:6001`) bridged from COM5 to `/dev/ttyUSB0 @115200 8N1`; operator pre-authorized the bridge; captures preserved in `sources/probes/B657-jace9000-serial/`; detached after capture.

---

## D2 — VENDOR-MENU-ENUM-IS-FIRMWARE-VERSION-DEPENDENT

**Priority:** HIGH
**Type:** PROMOTE (new named rule — no equivalent in kit)

### Evidence

- B657 §657.3 (`[CERT-live]`): "The live unit's main-menu NUMBERING does not match the doc example. In this session the operator selected the main-menu key documented as `3 Ping Host` and instead reached the Network Configuration Utility (`Enter new value, '.' to clear the field or '<cr>' to keep existing value` / `Hostname < atlashost > :`) — a **mutating** path."
- B657 §657.3: "the doc's per-number mapping is firmware-version-dependent and must be re-confirmed live on each unit; `4 → System Diagnostic` is confirmed here, but `3 = Ping = safe` is not trustworthy for this build."
- Gap J9K-10 opened to re-capture the live main-menu numbering for this firmware.
- Commit 9e76c9977 (B657 block, 2026-08-29).

### Deduplication result

Searched kit for analogues:
- "SCOPING JUDGMENTS ARE HYPOTHESES" (PROMPT-LOOP §3): scoped to prior-block conclusions about investigation scope, not doc-enumeration positions.
- "GAP PREMISES ARE HYPOTHESES" (PROMPT-LOOP BOOTSTRAP step e): scoped to backlog gap descriptions, not doc key-mappings.
- "RE-MEASURE A DRAMATIC NEGATIVE" (PROMPT-LOOP §3): scoped to absence findings.
- "ANNOTATION-BEFORE-DERIVATION" (PROMPT-LOOP §3): scoped to derived quantities on labeled sources.
- §12 "Read-first, write-supervised": covers read-before-write discipline but does not name the enumeration-position failure mode.

None is a match. This is a genuinely new failure class: a vendor-supplied doc example shows a SPECIFIC NUMBERED/ENUMERATED position (menu key, table row, field index, option position) as mapping to a given function. That mapping is CAPTURED FROM A SPECIFIC FIRMWARE VERSION. On a different firmware build, the position maps to a DIFFERENT function — which can be mutating where the doc example implied safe.

This is distinct from GAP PREMISES ARE HYPOTHESES (which applies to the researcher's claims) because here the VENDOR DOCUMENT is the source of the false certainty: the doc shows "3 = Ping Host" as if it were a fixed truth. The failure mode is trusting a doc's enumerated position as version-independent when it is not.

### Proposed landing

**METHODOLOGY.md §12** (dynamic phase discipline), after the "Read-first, write-supervised" bullet — add:

> **VENDOR-MENU-ENUM-IS-FIRMWARE-VERSION-DEPENDENT.** On a menu-driven shell (serial console, telnet/SSH menu, web admin menu), a vendor doc's per-key / per-number mapping (e.g. "`3 = Ping Host`", "`4 = System Diagnostic`") is a SNAPSHOT of a specific firmware version's menu layout. A later firmware build may reorder, add, or remove options, shifting every subsequent key's position. Treat the doc's enumerated mapping as a HYPOTHESIS, not a specification: before pressing any key on a live unit, confirm the live menu matches the doc's positions — a doc entry "`N = safe-read-option`" may reach a mutating path on this firmware.
>
> **Evidence:** niagara-research jace9000 B657 §657.3 — doc "`3 = Ping Host`" (safe) reached the Network Configuration Utility (mutating) on the live JACE-9000; only "`4 = System Diagnostic`" was confirmed by live observation.
>
> **Corollary (back-pointer rule):** when a live finding refutes a doc's enumerated position, open a gap to re-capture the live numbering map (J9K-10 pattern) rather than patching the doc example — the live map IS the finding.

**PROMPT-LOOP.md INVESTIGATE section** — add a cross-reference line after SCOPING JUDGMENTS ARE HYPOTHESES:

> - VENDOR-MENU-ENUM-IS-FIRMWARE-VERSION-DEPENDENT (METHODOLOGY §12): a doc's per-key enumeration on a menu-driven shell is firmware-version-dependent — confirm live before trusting any key's position. Sibling of GAP PREMISES ARE HYPOTHESES, scoped to enumerated doc positions.

---

## D3 — PRE-INTERACTION MUTATION MAP for live menu-driven devices

**Priority:** MED
**Type:** ABSORB into existing §12 "Read-first, write-supervised"

### Evidence

- B657 §657.3: the block explicitly authored a mutation safety map from docs BEFORE any keystroke was sent. The map classified each main-menu option as non-mutating (Ping Host, System Diagnostic) vs. mutating (network, time, credentials, passphrase, backup/restore, reboot).
- This was done pre-interaction — before the operator pressed any key — based on official doc analysis alone.
- The live finding (D2 above) then INVALIDATED one safe entry in the map, showing the map is a hypothesis, not a guarantee.

### Deduplication result

METHODOLOGY.md §12 "Read-first, write-supervised" (line 1326-1328): "Start with READ-ONLY probes (safe on a running system — confirm read-only in code first). WRITE/modify (load programs, change config) only step-by-step with explicit user OK."

DYNAMIC-SETUP.md §5: "A config-changing/reboot command over serial can brick the device just like a bad network write — explicit user OK only, never in an autonomous loop, and label a mutation ⚠ CONFIG MUTATION."

Neither names the PRE-INTERACTION step of authoring a mutation map BEFORE entering the menu. The current kit assumes you know what is read-only before you start; it does not tell you to DERIVE THAT KNOWLEDGE from docs as a formal step preceding any interaction.

### Proposed absorption

**METHODOLOGY.md §12 "Read-first, write-supervised"** — extend the existing bullet:

> **Read-first, write-supervised.** Start with READ-ONLY probes (safe on a running system — confirm read-only in code first). WRITE/modify (load programs, change config) only step-by-step with explicit user OK; a bad write can brick the device.
> **For a menu-driven shell (serial console, telnet/SSH menu)** — before sending any keystroke beyond authentication, author a PRE-INTERACTION MUTATION MAP from official docs: enumerate every menu option and classify each as `read-only` (display-only, trace, log) or `mutating` (network config, credential change, backup/restore, reboot, factory wipe). The map tells you which keys are safe to press unsupervised. Treat it as a hypothesis (VENDOR-MENU-ENUM-IS-FIRMWARE-VERSION-DEPENDENT applies — confirm the live menu layout matches your doc-derived map before acting on it). Evidence: jace9000 B657 §657.3 — mutation map authored from J9 docs before any keypress; live observation refuted one "safe" entry.

---

## D4 — Multi-marker bootstrap fusion in one block

**Priority:** LOW
**Type:** PROMOTE (clarification / naming a valid pattern)

### Evidence

- B657 bootstrap block fused four marker types in one block: `[CERT-doc]` (J9-specific Tridium guides), `[CERT-web]` (Tridium datasheet/FAQ), `[CERT]` (decompiled `BSystemPlatformServiceAtlas.java`), `[CERT-live]` (serial probes this session).
- Marker tally B657: `[CERT-doc]=6, [CERT]=2, [CERT-web]=1, [CERT-live]=3, [INFER]=0`.
- This is efficient: a single bootstrap block established identity at maximum confidence without requiring separate blocks per source type.

### Deduplication result

METHODOLOGY.md §3 documents all markers and their hierarchy. PROMPT-LOOP.md NORMAL CYCLE step 4 ("WRITE ONE BLOCK") allows all markers. §12 covers live probes. Nothing NAMES the multi-source bootstrap fusion as a deliberate efficiency pattern.

### Assessment

This is a LOW-priority observation because the kit already PERMITS all markers in one block; it just doesn't name this as a recommended pattern for bootstrap blocks on live targets. A brief note is worth proposing but not urgent.

### Proposed landing

**METHODOLOGY.md §12**, near the end of the phase-entry description — add:

> **Multi-source bootstrap efficiency.** A §12 bootstrap block over a live target MAY — and SHOULD when sources are available — fuse all applicable marker types ([CERT-doc], [CERT-web], [CERT], [CERT-live]) in ONE block rather than requiring separate blocks per source type. The identity question (what is this device, what does this port speak?) is most efficiently answered by maximum-confidence triangulation: docs for the spec, web for the vendor's public hardware facts, code for the platform class, live capture for confirmation. A bootstrap block that carries all four tiers with zero [INFER] is a quality signal, not overreach. Evidence: jace9000 B657 — SoC/OS identity from [CERT-web] + platform class from [CERT] + doc spec from [CERT-doc] + live banner from [CERT-live] = zero [INFER] bootstrap.

---

## Tools used / acquired this focus

No new tools acquired in this focus. The focus used:
- `serial-console.sh` (existing kit §5 wrapper) — NOT used directly; operator-held console and usbipd bridge were the paths.
- `usbipd-win` (Windows side, pre-existing on operator machine) — the new pattern is the SERIAL CONVERTER use of it (D1 above).
- Standard `grep`/`fd`/`cat` for doc extraction.

## FOCUSES.md / TARGETS.md status

- jace9000 is registered in FOCUSES.md (sibling of jace8000).
- TARGETS.md row for niagara-research should be refreshed to reflect 8 new blocks (B657–B664) when the focus is committed.

## Proposed kit delta verdict (§18 propose-never-apply)

All proposed deltas above are PROPOSALS for the kit maintainer's review. This file does not edit any kit file. The human reviewer should:
1. Accept / reject / modify each delta.
2. If accepted, apply the landing to the target kit file and section.
3. D1 (HIGH) and D2 (HIGH) are the most valuable — they name previously unnamed patterns with clear evidence from this run.
4. D3 (MED) is an absorption into existing content — low editorial risk.
5. D4 (LOW) is optional.
