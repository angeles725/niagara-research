# B661 — JACE-9000 SSH access to the system shell: same menu-driven shell (not a raw OS prompt), platform login still required, and SSH/SFTP are disabled by default on TCP 22 with a standing "keep it off" caution (focus jace9000, J9K-7)

> **Focus:** `jace9000` (§16). **Gap:** J9K-7 — SSH is a documented alternative to the serial DEBUG console;
> does it deliver the same platform-login shell menu or a raw OS prompt, and is it enabled by default?
> **Phase:** DISK-FIRST (doc). Read-only.
> **Sources:** `[CERT-doc]` niagara-help `J9Startup/{J8SystemShell, J8AboutTheNcSystemShellMenu,
> J8PerformingPlatformAdministration}` · `[CERT]` corpus [Block 657] [Block 468].
>
> **Bottom line for the operator:** SSH reaches the **same ATLAS System Shell menu** as the serial DEBUG
> console — it is **not** a Linux root shell — and it **still demands platform login**. But SSH (and SFTP)
> are **disabled by default** (TCP port 22), and Tridium's own guidance is to **keep them disabled** and
> turn them off again immediately after any use. So on a factory/hardened JACE-9000 the network path to the
> shell is closed; the serial DEBUG port is the expected way in.

## §661.1 — SSH gives the SAME shell, not a raw OS prompt

`[CERT-doc]` The shell is reachable two ways and both present the identical menu surface:

- "All controllers have a system shell… System shell is **also available via SSH** (Secure Shell) provided
  that SSH is enabled in the controller." — `J9Startup/J8SystemShell-AA1582D0.txt:14`
- "If SSH is enabled in the controller, you can also access the controller's **system shell** using a remote
  terminal session using SSH. **Platform login is still required** (just as with the controller powered up
  in serial shell mode)." — `J9Startup/J8AboutTheNcSystemShellMenu-AA1F5AC0.txt:25-26`
- The menu is the same object regardless of transport: the doc labels its figure "System shell menu (**serial
  shell or Telnet access**)" — `…J8AboutTheNcSystemShellMenu-AA1F5AC0.txt:34`.

So SSH does not escalate you past the menu — you land in the same menu-driven platform interface ([Block 657]
§657.2), gated by the same admin-only platform login ([Block 660]). There is no documented raw-OS / root
shell exposed over SSH.

## §661.2 — Disabled by default, TCP 22, and a standing caution to keep it off

`[CERT-doc]` Remote shell access is off out of the box:

- "Enable or disable SFTP (Secure File Transfer Protocol) and SSH (Secure Shell) access to the JACE
  controller. **By default, such access is disabled**, where both protocols use **TCP port 22**."
  — `J9Startup/J8PerformingPlatformAdministration-A89886C8.txt:48`
- "CAUTION: Although SFTP and SSH are more secure than FTP and Telnet access, enabling still poses security
  risks. **We strongly recommend you keep this access disabled**, unless otherwise directed by Systems
  Engineering. Upon completion of any use, **such access should be disabled once again**."
  — `…J8PerformingPlatformAdministration-A89886C8.txt:49-51`

The enable/disable control lives **inside the shell itself** — the shell provides "enabling/disabling
SFTP/SSH and Telnet" among its basic settings (`…J8AboutTheNcSystemShellMenu-AA1F5AC0.txt:13-16`). So turning
SSH on is a deliberate, in-band act, and the doc frames leaving it on as a security risk.

This matches the JACE-8000 hardened posture ([Block 468]: SSH/Telnet off by default). For the JACE-9000 it
means the **serial DEBUG port is the primary, always-available shell path**, and SSH is an opt-in that a
security-conscious operator turns off again after use.

## §661.3 — What is NOT resolved here

The **default-disabled** state is documented; whether SSH is enabled on *this particular* commissioned unit
is a live-only check (part of J9K-3/J9K-2 posture). And Telnet appears in the figure caption alongside serial
— its exact JACE-9000 availability (many N4 controllers ship Telnet permanently off) is a live/hardening
follow, not asserted here beyond the doc's own "enabling/disabling … Telnet" wording.

## Self-verify

| # | Claim | Marker | Citation | Checked |
|---|---|---|---|---|
| 1 | Shell also available via SSH if enabled | [CERT-doc] | J8SystemShell-AA1582D0.txt:14 | ✓ grep |
| 2 | SSH delivers the same system shell; platform login still required | [CERT-doc] | J8AboutTheNcSystemShellMenu-AA1F5AC0.txt:25-26 | ✓ grep |
| 3 | Figure: same menu for serial shell or Telnet | [CERT-doc] | …AA1F5AC0.txt:34 | ✓ grep |
| 4 | SFTP/SSH disabled by default, TCP port 22 | [CERT-doc] | J8PerformingPlatformAdministration-A89886C8.txt:48 | ✓ grep |
| 5 | Caution: keep disabled; disable again after use | [CERT-doc] | …A89886C8.txt:49-51 | ✓ grep |
| 6 | Enable/disable SFTP/SSH/Telnet is a shell menu function | [CERT-doc] | …AA1F5AC0.txt:13-16 | ✓ grep |
| 7 | Matches JACE-8000 hardened default (SSH/Telnet off) | [CERT] | [Block 468] | ✓ corpus |

**Marker tally:** [CERT-doc]=6, [CERT]=1 (corpus), [INFER]=0 · ratio [INFER]/[CERT*]=0 · **block type =
EVIDENCE (doc-synthesis)**. No unmarked claims.

## Connections

- [Block 657] — the ATLAS System Shell menu SSH lands you in; same 115200-serial menu, other transport.
- [Block 660] — "platform login still required": that login is admin-only, no viewer role.
- [Block 468] — JACE-8000 live posture: SSH/telnet off by default — the same hardening baseline.

## Open gaps (RESEARCH-STATE-jace9000.md)

Doc-investigable remaining: J9K-11 (microSD→Host ID), J9K-12 (COM1/COM2 vs DEBUG). Live-gated: J9K-2, J9K-3
(incl. whether SSH is enabled on this unit), J9K-9, J9K-10.
