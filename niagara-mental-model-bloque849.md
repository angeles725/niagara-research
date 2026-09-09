# Block 849 — Opening & running a copied station offline: the four walls (passphrase, modules, clean-folder, password-encoder)

> **Focus:** backup-licensing-ops (document-mode §20). **Scope:** the complete OPERATOR how-to for taking
> someone else's station copy and getting it to open, edit, and run in your OWN Workbench, offline, without
> touching production. Captures the **four sequential walls** hit end-to-end on the HARBOR HM_BMS copy —
> §1-6 the **passphrase** (why the Station Transfer Wizard prompts; the known-passphrase path; the official
> Tridium unknown-passphrase BOG-edit; what a forced passphrase clears; the whole-station-reload guardrail);
> §7 the **missing-module** decode failure and its module-part/`sw/` fix (+ `tools/station-modules.py`);
> §8 the **install-commit** failure from a non-clean (nested) station folder; §9 the **"SecretBytes has been
> closed"** password-encoder failure when setting a user password offline. Sibling recipe to B838 (which
> covers backup/restore/licensing); this one is the "open-a-foreign-copy" case B838-G3 gestured at.
>
> **Live context `[CERT-live]` (operator, 2026-09-09):** HARBOR / Distech-Mérida job. Operator copied the
> **HM_BMS supervisor** station backup (`HM_BMS_backup_08_09_2026`) into local Workbench to advance the
> GreenMAX lighting work offline (production is change-frozen); the Station Transfer Wizard stopped at
> *"Enter the passphrase — The passphrase that's used to protect the local copy of the station is not the
> same as the remote host's system passphrase. Please enter the local copy's passphrase."*
>
> **Sources (three consulted):**
> - **FUENTE 2 (official Tridium help, N4.14.0.162, verbatim `[CERT-doc]`):**
>   `StationSecurity/InstallingACopiedStationOntoANewPla-E8BAAE3B` (the offline BOG-edit procedure),
>   `J9Startup/InstallingSoftware-30E8011A` (the Commissioning-Wizard passphrase-mismatch prompt). Hashes in
>   `sources/SOURCES.md`.
> - **FUENTE 1 (corpus):** B838 (backup/restore/licensing + what the system passphrase encrypts + restore
>   wipes credentials); B841–B848 (the GreenMAX lighting design/matrix being built offline against this copy).
> - **FUENTE 3 (code):** none needed — this is a Workbench-tool procedure, not module internals. The BOG XML
>   shape (mostly plaintext tree, only sensitive fields encoded) is already the basis of `tools/bog-nav.py`.

---

## 1. Why the prompt appears — it is not a bug and not bypassable by "copying only the station"

In N4 the station's `config.bog` (and any `.dist`) carries its **sensitive fields encoded with the system
passphrase in force when the file was created** — user passwords, client-side credentials (station-to-station
connections, Email Service), and the like. Copying the station *already includes* those encoded fields; there
is no "copy without the passphrase", because the protection is intrinsic to the file, not a separate lock.

Workbench prompts specifically because the **local copy's passphrase differs from the remote host's system
passphrase**: *"If the passphrase for the local copy of the station is different from the remote host's system
passphrase, you are prompted to enter the local copy's passphrase. If there is no passphrase mismatch, you are
not prompted to enter one."* [CERT-doc `InstallingSoftware-30E8011A` L80–81]. To install a station from your PC
you must know its passphrase [CERT-doc same L20].

## 2. What the passphrase protects (recap)

The system passphrase encrypts the at-rest sensitive data in `config.bog` and `.dist`; it is **distinct from
the platform/station login credentials**, and a backup can only be decoded with the passphrase that was in
force when it was made [CERT-doc B838 §5/§10 → `aPlatformSystemPassword`, `RestoringFromABackup` §40–42].
The bulk of the station — control logic, wiresheets, Px, points, link graph — is **not** encoded; only the
sensitive leaf fields are.

## 3. Path A — you know (or can get) the passphrase

Type it into the wizard's Passphrase field and continue. This is the clean path: the passphrase is a
legitimate operational secret the site owner / commissioning integrator holds. For HARBOR that means asking
whoever set up the HM_BMS supervisor.

## 4. Path B — the passphrase is unknown: edit the BOG offline (official procedure)

Official Tridium recipe *"Installing a station copy on another platform"* for exactly the case where "the BOG
file passphrase is not known" — before you can run it you must edit the BOG offline
[CERT-doc `InstallingACopiedStationOntoANewPla-E8BAAE3B` L14–15,33]:

1. Copy the station folder into your User Home stations folder
   (`C:\Users\<you>\Niagara4.x\<brand>\stations`) [L45].
2. In the Workbench NavTree, navigate to the folder and **double-click `config.bog` to open it** — opening
   the file for editing does **not** require the passphrase [L49].
3. On the toolbar click the **Bog File Protection tool** (enter/change/add a BOG passphrase) [L52–53].
4. Choose **"Force the file to start using a different passphrase that you specify"**, enter the new
   Passphrase + confirm, click **Update**, then Close [L60].
5. Expand `config.bog > Services`, double-click **UserService**, open the `admin` user, and set a new
   Password + confirm under Authenticator (repeat for other users as needed) [L62–65].
6. Right-click `config.bog` → **Save** [L67].
7. Open the **Station Copier** and copy the station from your User Home to the target platform [L69]. It
   typically autostarts; otherwise start it from Application Director [L72+].

## 5. What is cleared vs what survives — the CAUTION

Forcing a new passphrase in step 4 **clears every password value that was encoded with the old passphrase**:
you must re-enter the passwords the station uses as a client — station-to-station connections, Email Service,
and by extension driver/field-device credentials [CERT-doc L55]. **Your control logic, wiresheets, Px, points
and links are untouched** — only the encoded secrets are lost. So for offline *design* work (the GreenMAX
lighting logic + Px of B841–B848) this path costs you nothing structural; it costs you the field credentials,
which you'd re-supply at commissioning.

Note also: if you only need to **edit** the copy offline (build Px, wiresheets) and never run it locally, step
2 alone is enough — you open `config.bog` and edit it as a file without the passphrase, and never touch steps
3–7.

## 6. Guardrail — do NOT ship the whole offline copy back over live production `[INFER, operator-context]`

The operator's stated end-goal was "just arrive and load the modified station" wholesale onto production. Two
concrete hazards make that wrong, both grounded in B838 [CERT-doc]:

- **Credentials.** After a forced passphrase (§4) the copy has **no** field/client credentials. Copying it
  whole over production drops the live connections to the field controllers/BACnet.
- **Drift.** A live station accumulates state and operator changes (histories, alarms, tuned setpoints) that a
  months-old offline copy does not have; a wholesale copy **overwrites** them. (B838 §7 already records that
  restore/whole-station operations wipe live-only data.)

Correct pattern for HARBOR: develop the lighting logic/Px offline against this copy, then carry **only the new
components** to production — export/import the specific `.bog`/`.px`, or rebuild the wiresheet live — never a
whole-station replace. This is derived operator guidance, not a doc verbatim; marked `[INFER]`.

## 7. Second wall: missing MODULES — the copy opens only if your Workbench has every module the station uses `[CERT-live]`

After the passphrase, the next wall on the HARBOR HM_BMS copy was a decode failure — **not** a passphrase
issue:

```
CannotLoadBogException ... Cannot load module 'dsb=dashboard' [973:80]
ModuleException: Cannot resolve dependency dashboard-rt-Tridium-4.14.0 for dashboard-wb-Tridium-4.14.0.162
ModuleNotFoundException: dashboard-rt
```

Line 973 of the BOG is `<p n="DashboardService" ... t="dsb:DashboardService"/>`. The station's `config.bog`
references component TYPES that live in module PARTS (`<module>-rt/-wb/-ux`); to **decode** the BOG, every
referenced part **and its declared `<dependencies>`** must be installed in the Workbench install's `modules/`.
The install had `dashboard-wb.jar` but **not** `dashboard-rt.jar` (a partial install) → the exact error. The
missing jar was already staged in the same install under `sw/4.14.0.162/dashboard-rt.jar`; copying it into
`modules/` and restarting Workbench let the BOG open. HM_BMS references 23 modules; `dashboard` was the only
one whose `-rt` part was missing. `[CERT-live]` 2026-09-09.

**A `sw/1.0/<part>.jar` is a v1.0 stub, not the real module** — verify the jar's real manifest
`vendorVersion` before copying (confirmed: `sw/1.0/dashboard-rt.jar` = vendorVersion "1.0";
`sw/4.14.0.162/dashboard-rt.jar` = "4.14.0.162"). Tool: `tools/station-modules.py check <config.bog>` lists
referenced modules, flags missing parts with the reason, locates the version-matched staged jar, and (with
`--fix`) copies it; `doctor` reports any installed part with an unresolved dependency.

## 8. Third wall: installing the copy to a platform — the folder must be ONE clean station `[CERT-live]`

Running the copy on a platform (Station Copier → localhost) failed at the final commit:

```
DaemonResponseException: Request to platform daemon at localhost failed: bad return code to commit file instance
  at InstallScenario.commit ... FinalStep.run (Station Transfer Wizard)
```

Cause on HM_BMS: the downloaded backup folder was **not a clean single station** — it wrapped a config.bog +
`shared/` + `userdata/` AND a **nested second station** `HM_BMS/` (its own config.bog/shared/history, 2018
console logs, **0 GreenMAX** — an unrelated old sub-backup). The transfer shipped both levels and the daemon
could not commit a station-within-a-station. Disk (299 GB free) and license were not the cause. A valid
station directory is ONE folder with exactly one root `config.bog` + `shared/`/`userdata/`, no nested station.
Fix: move the stray nested station out of the directory, then re-run Station Copier. `[CERT-live]` 2026-09-09.
(Distinguish the real station by content: the HARBOR one has 48 GreenMAX refs / 652 BooleanSchedules vs the
intruder's 0/33.) For pure offline DESIGN work this whole install step is unnecessary — edit the root
`config.bog` in place.

## 9. Fourth wall: "SecretBytes has been closed" when setting a user password offline `[CERT / CERT-live]`

After forcing a new passphrase (§4), setting the `admin` password and clicking Save threw:

```
BajaRuntimeException → java.io.IOException: java.lang.IllegalStateException:
  Cannot perform operation on a SecretBytes that has been closed
  at BogPasswordObjectEncoder.passPhraseToKey(BogPasswordObjectEncoder.java:551)
  at BBogSpace.getEncodingContext / newTransaction / Transaction.start / doSaveValue
```

**Mechanism `[CERT]`** — `BBogSpace.getEncodingContext` derives the write key from the passphrase before
opening the save Transaction [`organized/.../file/types/bog/BBogSpace.java:173-207`]:

```java
if (bogPasswordObjectEncoder.getKeySource().equals(EncryptionKeySource.external)
        || reversibleEncryptionPassPhrase.isPresent()) {
    try (SecretChars passPhrase = reversibleEncryptionPassPhrase.get().getSecretChars()) {
        pContext.setEncryptionAndDecryptionKey(external,
            Optional.of(bogPasswordObjectEncoder.passPhraseToKey(passPhrase)));  // ← throws
    }
}
```

The bog is passphrase-protected (`external` key source). Writing a password requires deriving the key via
`passPhraseToKey`, but the **encoder's internal `SecretBytes` was already closed** after the force-passphrase
operation — the encoder instance is reused with disposed key material, so the key derivation aborts and the
save fails. It is a stale-session-state problem, not a wrong password.

**Fix (confirmed working `[CERT-live]` 2026-09-09), in order:**

1. **Close `config.bog` entirely** (close it in the NavTree, not just the tab) and **reopen it** — a fresh
   open rebuilds the encoder with a valid `SecretBytes`. Often this alone is enough.
2. Bog File Protection tool → **"Enter the file's passphrase"** → type the passphrase you set → **Update**
   (this puts a LIVE key in the session).
3. **Without closing anything**, `Services > UserService > admin > Authenticator` → set Password + Confirm →
   **Save**. The save now has a live key and does not re-close the secret.

If step 3 still fails, re-run "Force the file to start using a different passphrase" → Update → then edit the
password and Save immediately, all in ONE session without closing the bog between the operations.

## Self-verify

| # | Claim | Marker | Evidence |
|---|-------|--------|----------|
| 1 | Prompt fires only on a passphrase MISMATCH between local copy and remote host system passphrase | [CERT-doc] | `InstallingSoftware-30E8011A` L80–81 |
| 2 | To install a station from your PC you must know its passphrase | [CERT-doc] | `InstallingSoftware-30E8011A` L20 |
| 3 | Sensitive fields are encoded with the passphrase in force when the file was made; distinct from login creds | [CERT-doc] | B838 §5/§10 (`RestoringFromABackup` §40–42; `aPlatformSystemPassword`) |
| 4 | Unknown-passphrase copy requires editing the BOG offline before it can run | [CERT-doc] | `InstallingACopiedStationOntoANewPla` L14–15,33 |
| 5 | `config.bog` can be opened for editing without the passphrase | [CERT-doc] | `InstallingACopiedStationOntoANewPla` L49 |
| 6 | Bog File Protection tool → "Force … different passphrase" → Update sets a new passphrase | [CERT-doc] | `InstallingACopiedStationOntoANewPla` L52–53,60 |
| 7 | Forcing a new passphrase CLEARS all password values encoded with the old passphrase | [CERT-doc] | `InstallingACopiedStationOntoANewPla` L55 |
| 8 | Reset admin (and other) user passwords via UserService, then Save, then Station Copier to target | [CERT-doc] | `InstallingACopiedStationOntoANewPla` L62–65,67,69 |
| 9 | Control logic / Px / points / links are not encoded, so they survive the forced passphrase | [INFER] | derived from §7 (only password values are cleared) + BOG XML shape (`tools/bog-nav.py`) |
| 10 | Whole-station reload over production drops field credentials and overwrites live drift | [INFER] | operator-context, grounded in B838 §7 restore-wipes-creds [CERT-doc] |
| 11 | HARBOR HM_BMS copy hit the mismatch prompt in local Workbench 2026-09-09 | [CERT-live] | operator Station Transfer Wizard screenshot 2026-09-09 |
| 12 | BOG decode needs every referenced module PART + its `<dependencies>` in `modules/`; dashboard-wb present but dashboard-rt missing → ModuleNotFoundException dashboard-rt | [CERT-live] | operator stack trace 2026-09-09; line 973 `t="dsb:DashboardService"`; fixed by copying sw/4.14.0.162/dashboard-rt.jar |
| 13 | `sw/1.0/<part>.jar` is a v1.0 stub; real module in `sw/<ver>/`; verify manifest vendorVersion | [CERT-live] | dashboard-rt.jar: sw/1.0="1.0", sw/4.14.0.162="4.14.0.162" |
| 14 | Station Copier commit fails ("bad return code to commit file instance") when the installed folder is not one clean station (nested second station inside) | [CERT-live] | operator DaemonResponseException 2026-09-09; HM_BMS_backup wrapper held a nested HM_BMS/ station (0 GreenMAX, 2018 logs) |
| 15 | Saving a user password offline needs a live key derived from the passphrase; a reused encoder with a closed SecretBytes throws "SecretBytes has been closed" | [CERT] | BBogSpace.java:173-207 getEncodingContext → passPhraseToKey(passPhrase) |
| 16 | Fix: close+reopen the bog (rebuilds the encoder), re-enter the passphrase, then edit+Save the password in one session | [CERT-live] | operator confirmed working 2026-09-09 |

**Tally:** 16 claims — 6 [CERT-doc], 3 [INFER] (2 derived guidance + 1 structural inference), 6 [CERT-live],
1 [CERT] (code). No unmarked assertions.

## Connections
- **B838** — backup/restore/licensing + what the system passphrase encrypts + restore wipes credentials; this
  block is the "unknown-passphrase copy" sibling recipe (see B838-G3).
- **B841–B848** — the GreenMAX lighting design/matrix/wiresheet being built offline against this very HM_BMS
  copy; §6 guardrail governs how that work reaches production.
- **tools/bog-nav.py** — reads the `config.bog` XML tree directly (only sensitive leaves are encoded),
  corroborating that logic/Px survive a passphrase reset.
- **tools/station-modules.py** — resolves a station's module-part dependencies against a local install and
  locates the missing jar in `sw/` (§7). Built this session; documented in `tools/README.md`.

## Open gaps (RESEARCH-STATE-backup-licensing-ops)
- **B849-G1** *(investigable)* — the Station Copier's own passphrase prompts on the copy-to-target leg (does
  the target re-encode with *its* system passphrase automatically, or prompt again), as a step-anchored recipe.
- Relationship to **B838-G3** — that gap named the `.dist` Distribution-File-Installer-vs-Station-Copier
  decision; this block covers the BOG-edit path only, so G3 stays open for the `.dist` side.
