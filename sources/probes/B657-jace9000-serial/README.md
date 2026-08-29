# B657 — JACE-9000 serial DEBUG console (ATLAS System Shell) live captures

Read-only §12 probes, 2026-08-29, /dev/ttyUSB0 @ 115200 8N1 (FTDI FT232 bridged from COM5 via usbipd).
Operator authorized read; NO credentials typed by the driver (operator's prior session had already
authenticated and then idle-timed-out). SECRETS DISCIPLINE: no secret VALUES captured.

- 01-menu-prompt.txt        — 1x CR → `Enter Choice :` (ATLAS System Shell main menu prompt)
- 02-idle-timeout-reauth.txt— option `4` sent → "Timed out waiting for input, please re-authenticate"
- 03-logout-to-login.txt    — CR → "Logging out current user" + ANSI clear → `login :`
- 04-atlas-system-diagnostic-menu.png — operator screenshot: main-menu opt 4 → ATLAS System Diagnostic
                              Menu (8 read-only Display/Trace options + X Exit)
- 05-network-config-utility.png — operator screenshot: a main-menu key → Network Configuration Utility
                              ("Hostname < atlashost > :", field-edit prompt) = MUTATING path
