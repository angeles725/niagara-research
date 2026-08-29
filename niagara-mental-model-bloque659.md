# B659 — JACE-9000 shell activation: the DEBUG console is live in normal operation (just press Enter), and the "special power-up mode" language belongs to the two recovery boots, not day-to-day access (focus jace9000, J9K-5)

> **Focus:** `jace9000` (§16). **Gap:** J9K-5 — is the ATLAS System Shell always live on the USB-C DEBUG
> port, or does it need a "special power-up mode"? **Phase:** DISK-FIRST (doc). Read-only.
> **Sources:** `[CERT-doc]` niagara-help `J9Startup/{J8SystemShell, ConnectingToTheControllerDebugSyste,
> PreparingToCommissionTheController}` · `[CERT]` corpus [Block 657] [Block 658].
>
> **Bottom line for the operator:** on the JACE-9000 the system shell is reachable over the DEBUG USB-C port
> **in normal operation** — the documented connection procedure is just "plug in USB-C, open PuTTY, and if
> you see no prompt press **Enter**." No button-hold or special boot is needed to reach the login. The
> phrase **"special power-up mode"** that appears in the generic overview is the language of the two
> *recovery* boots ([Block 658]) and the Workbench-over-USB commissioning path — not day-to-day shell login.

## §659.1 — Normal DEBUG access: always live, prompt on Enter

`[CERT-doc]` The JACE-9000 connection procedure has **no special-mode step**. You connect the USB-C cable,
open a serial terminal at 115200 8N1 ([Block 657]), and:

- "NOTE: If you do not see a login prompt, **press the Enter key** and it should display a login prompt in
  the window." — `J9Startup/ConnectingToTheControllerDebugSyste-A6400BFD.txt:71`

This matches the live behaviour observed in [Block 657] §657.4: the shell is idle until it receives a byte,
and a bare `CR` elicits the prompt. So on a running, commissioned JACE-9000 the shell is **already up** on
the DEBUG port; nothing must be "activated."

## §659.2 — Where "special power-up mode" actually comes from

`[CERT-doc]` The generic system-shell overview does use the phrase: "All controllers have a system shell
that provides low-level access to a few basic platform settings. **Using a special power-up mode** and a
serial connection via an appropriate type USB cable connected to the controller, you can access this system
shell from your PC." — `J9Startup/J8SystemShell-AA1582D0.txt:12-14`. That text is generic (`J8…`-prefixed,
inherited overview language covering the whole JACE family).

The one J9 procedure that genuinely invokes a special power-up is **Workbench-driven commissioning over
USB**, not the serial shell: "This requires a USB-to-USB-C adapter cable, VCP driver, and a **special
power-up mode** for the controller." — `J9Startup/PreparingToCommissionTheController-30E18F53.txt:52`.

`[INFER]` Reconciling the two: on the JACE-9000, "special power-up mode" describes the **commissioning /
recovery** entry paths (Workbench-over-USB, and the ESC/SHUTDOWN boots below), while ordinary **serial
system-shell login needs no special mode** — the connection procedure (§659.1) shows only connect-and-Enter.
(Marked `[INFER]` because it is a reconciliation of two doc statements, not a single verbatim claim; a live
confirmation that a normally-booted unit shows the login without any button-hold is J9K-5-follow.)

## §659.3 — The two genuinely distinct special boots (do not conflate with normal login)

`[CERT-doc]` Two real special-entry modes exist, both documented in [Block 658], and both are for RECOVERY,
not exploration:

1. **Boot Options** — press **ESC** during the boot sequence at `Press ESC to enter boot options...`
   (`J9Startup/J9ResetPlatformCredentials.txt:74`) → the 2-option credential-recovery menu.
2. **Factory-defaults mode** — press and hold the **SHUT DOWN** button while powering up
   (`J9Startup/SettingUpPlatformUsers-34CB0D4E.txt:84`: "make a serial shell connection, then press and hold
   the SHUT DOWN button as you power up the device") → the destructive factory wipe.

Neither is the normal shell: one leads to a Tridium-signed credential reset, the other wipes the station
([Block 658] §658.2–§658.3). The **normal** login (§659.1) is a third thing and needs none of this.

## §659.4 — SSH is the other access path (not a special boot)

`[CERT-doc]` The shell is "also available via **SSH** (Secure Shell) provided that SSH is enabled in the
controller." — `J9Startup/J8SystemShell-AA1582D0.txt:14`. SSH is disabled by default and delivers the same
menu, not a raw OS shell — covered in [Block 661] (J9K-7).

## Self-verify

| # | Claim | Marker | Citation | Checked |
|---|---|---|---|---|
| 1 | No prompt → press Enter to get login (normal, no special mode) | [CERT-doc] | ConnectingToTheControllerDebugSyste-A6400BFD.txt:71 | ✓ grep |
| 2 | Overview uses "special power-up mode" (generic J-family text) | [CERT-doc] | J8SystemShell-AA1582D0.txt:12-14 | ✓ grep |
| 3 | J9 "special power-up mode" applies to WB-over-USB commissioning | [CERT-doc] | PreparingToCommissionTheController-30E18F53.txt:52 | ✓ grep |
| 4 | Normal serial shell login needs no special mode (reconciliation) | [INFER] | derived from §659.1 vs §659.2 | n/a |
| 5 | ESC→Boot Options is a distinct recovery boot | [CERT-doc] | J9ResetPlatformCredentials.txt:74 | ✓ grep |
| 6 | SHUTDOWN-hold-at-power = factory mode | [CERT-doc] | SettingUpPlatformUsers-34CB0D4E.txt:84 | ✓ grep |
| 7 | Shell also via SSH if enabled | [CERT-doc] | J8SystemShell-AA1582D0.txt:14 | ✓ grep |

**Marker tally:** [CERT-doc]=6, [INFER]=1, [CERT]=0-corpus (2 corpus refs) · ratio [INFER]/[CERT-doc]=0.17 ·
**block type = EVIDENCE (doc-synthesis)**. The single [INFER] is explicitly a two-statement reconciliation,
flagged for live confirmation (J9K-5-follow).

## Connections

- [Block 657] — bootstrap: the shell is idle-until-a-byte (live-observed), 115200 8N1; §659.1 explains the
  "press Enter" that surfaces the prompt.
- [Block 658] — the ESC Boot-Options and SHUTDOWN factory modes referenced in §659.3.
- [Block 661] — J9K-7, the SSH access path (same menu, default-disabled).

## Open gaps (RESEARCH-STATE-jace9000.md)

Doc-investigable remaining: J9K-6 (non-admin account), J9K-7 (SSH), J9K-11 (microSD→Host ID), J9K-12
(COM1/COM2 vs DEBUG). Live-gated: J9K-2, J9K-3, J9K-9, J9K-10 (incl. a live confirm of §659.2's [INFER]).
