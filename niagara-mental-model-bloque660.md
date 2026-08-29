# B660 — JACE-9000 platform accounts over serial: every platform user has full equal privileges — there is no read-only or non-admin platform role, so serial-shell read-only-ness comes only from menu choice, never from a limited account (focus jace9000, J9K-6)

> **Focus:** `jace9000` (§16). **Gap:** J9K-6 — the serial shell requires "admin-level platform
> credentials"; can a lower-privilege platform account exist that logs in to view without mutating?
> **Phase:** DISK-FIRST (doc). Read-only.
> **Sources:** `[CERT-doc]` niagara-help `J9Startup/{Commissioning, SettingUpPlatformUsers,
> ShutdownAndDebug}` · `[CERT]` corpus [Block 657] [Block 658].
>
> **Bottom line for the operator:** the JACE-9000 platform layer has **no tiered accounts**. Every platform
> daemon user has **"full equal privileges"** — the platform daemon is, by Tridium's own words, "the
> highest-level of access to the controller." There is **no read-only / viewer platform account**. So on the
> serial shell, "read-only" is a property of **which menu option you pick** ([Block 657] §657.3), never of a
> restricted login. Anyone who can authenticate to the DEBUG console can reset credentials, change the
> network, or reboot the box.

## §660.1 — The auth gate: admin-level, and that is the only level

`[CERT-doc]` The serial shell gate is fixed at admin: "NOTE: Login requires **admin-level platform
credentials**." — `J9Startup/ShutdownAndDebug-0B4C4735.txt:49` (as recorded in [Block 657] §657.2). The
question J9K-6 asks is whether a *lower* level exists below that gate. It does not.

## §660.2 — Additional accounts are additional ADMINS, not lesser roles

`[CERT-doc]` The commissioning wizard's option to add platform users is explicit that the extras are
co-equal admins, not a separate privilege class:

- "Configure additional platform daemon users — recommended option if you require additional platform admin
  user accounts, with unique user names and passwords (**all have full equal privileges**)."
  — `J9Startup/Commissioning-30E1CE0B.txt:51-52`

So multiplicity of accounts buys **accountability** (distinct names/passwords, so actions attribute to a
person) — not **least privilege**. There is no doc-described way to mint a platform account that can view but
not change.

## §660.3 — Why: the platform daemon is the top of the trust stack

`[CERT-doc]` Tridium frames the platform account as the maximum-authority credential, which is consistent
with there being no role beneath it:

- "…guard them closely — as they provide the **highest security level access** to any Niagara platform."
  — `J9Startup/SettingUpPlatformUsers-34CB0D4E.txt:58-59`
- "Consider the platform daemon as the **highest-level of access to the controller**."
  — `J9Startup/SettingUpPlatformUsers-34CB0D4E.txt:80`

This is the platform-vs-station distinction from the sibling focus ([Block 460]/[Block 461]): the STATION
layer has full RBAC — roles, per-category permissions, read-only users — but that governs the *station*
(points, logic), reached over Fox/HTTP, not the serial shell. The **platform** layer (what the DEBUG console
speaks) is binary: you are a platform admin or you are not in.

## §660.4 — Operational consequence for read-only work

Because there is no viewer account, the ONLY safe way to use the serial console read-only is to log in as the
(necessarily admin) platform user and **restrict yourself to non-mutating menu options** — on this firmware,
the confirmed-read-only surface is the **System Diagnostic submenu** ([Block 657] §657.4). The login itself
grants full power; discipline, not the account, is what keeps a session read-only. This also sharpens the
credential-exposure risk: the leaked `admin1` credential ([Block 657]) is not a limited account — it is
full platform control. **Rotate it.**

## Self-verify

| # | Claim | Marker | Citation | Checked |
|---|---|---|---|---|
| 1 | Serial shell login requires admin-level platform credentials | [CERT-doc] | ShutdownAndDebug-0B4C4735.txt:49 | ✓ grep (B657) |
| 2 | Additional platform users "all have full equal privileges" | [CERT-doc] | Commissioning-30E1CE0B.txt:51-52 | ✓ grep |
| 3 | Platform creds = highest security level access | [CERT-doc] | SettingUpPlatformUsers-34CB0D4E.txt:58-59 | ✓ grep |
| 4 | Platform daemon = highest-level access to the controller | [CERT-doc] | SettingUpPlatformUsers-34CB0D4E.txt:80 | ✓ grep |
| 5 | Station RBAC (roles/read-only users) is a separate layer | [CERT] | [Block 461] (station SCRAM/navigator) | ✓ corpus |

**Marker tally:** [CERT-doc]=4, [CERT]=1 (corpus), [INFER]=0 · ratio [INFER]/[CERT*]=0 · **block type =
EVIDENCE (doc-synthesis)**. No unmarked claims. Proven-absence discipline: "no non-admin platform role" is
supported by the positive statement "all have full equal privileges" (Commissioning:52), not merely by not
finding one — but the sweep also found no lesser-role topic across J9Startup/Platform/StationSecurity.

## Connections

- [Block 657] — read-only-ness is a menu-choice property, not an account property (§657.3); the leaked
  `admin1` is full platform control.
- [Block 658] — the mutating power an authenticated serial user holds (credential reset, factory wipe).
- [Block 460]/[Block 461] — the platform-vs-station split: station has RBAC, the platform layer does not.

## Open gaps (RESEARCH-STATE-jace9000.md)

Doc-investigable remaining: J9K-7 (SSH), J9K-11 (microSD→Host ID), J9K-12 (COM1/COM2 vs DEBUG). Live-gated:
J9K-2, J9K-3, J9K-9, J9K-10.
