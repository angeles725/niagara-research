# B466 — The JACE-8000 System Passphrase and the two at-rest encryption domains: daemon-home secrets are sealed with a machine-only random key (not the passphrase), portable copies with a passphrase-derived key — and why that refines the .bog verdict (focus jace8000, J6; §14 refines B464)

> **Focus:** `jace8000` (§16). **Gap:** J6 — the System Passphrase, platform credential model, and how
> sensitive data is encrypted at rest.
> **Phase:** §12 + `[CERT-doc]`. Read-only. `live-install` → SECRETS DISCIPLINE.
> **§14:** this block **refines [Block 464] §464.4** — a raw copy of the *running* `config.bog` is sealed with
> a **machine-only key**, a stronger wall than the passphrase I described there (noted back in B464).
> **Sources:** `[CERT-doc]` niagara-help (`Platform/aPlatformSystemPassword`, `J8Startup/J8SpecifyingAStationDatabaseToInstall-9D4C67CE.txt`) ·
> [Block 462]/[Block 463]/[Block 464] (this focus).
>
> **Bottom line:** the **System Passphrase** encrypts sensitive values (stored client passwords in `.bog`, and
> `.dist` backups). But Niagara uses **two different keys depending on file portability**: files under the
> **daemon User Home** (`/home/niagara` — the *running* station) are sealed with "**a strong, randomly
> generated key that exists only on that system**"; **portable** files (backups, exported stations) are sealed
> with "**a key derived from the … system passphrase**." So a raw grab of the live `config.bog` is
> **un-decryptable off the box** (machine key), while a `.dist` backup is decryptable **only with the
> passphrase**. Copying with a plain file tool instead of the platform tools leaves the secrets unreadable.

## §466.1 — What the System Passphrase is

`[CERT-doc]` `Platform/aPlatformSystemPassword.txt`:
- "All Niagara 4 platforms have a **system passphrase** (password), used to **encrypt sensitive information,
  such as client passwords stored in BOG files and station databases (`config.bog` files) or station backup
  distribution (`.dist`) files**." "This system passphrase applies to the **JACE-8000 and JACE-9000**."
- It is entered **during software installation** or when changed, and you are prompted for it "when copying
  stations or restoring station backups."
- Affected areas: **Provisioning, Distribution File Installer, File Transfer Client, Station Copier, Backup,
  Commissioning, Export Tags**. Notably: "If you do not know the passphrase for a `.dist` file **you cannot
  install it**."

The passphrase is a **platform**-level secret (set at OS/platform install), distinct from **station users**
([Block 461]) — consistent with the two-credential model of [Block 460]. Resetting it is the serial +
Tridium-signed operation of [Block 463] §463.2.

## §466.2 — The load-bearing distinction: two encryption domains by portability

`[CERT-doc]` same file (verbatim):
- "Files located under the **daemon User Home** (files that belong to the system) are encrypted using **a
  strong, randomly generated key that exists only on that system**."
- "Files located under the **Niagara User Home** (that is, **portable files that can be sent to many
  systems**) are encrypted using **a key derived from the user-defined system passphrase**."
- "when transferring files between the daemon User Home and another User Home you must use the Workbench
  platform tools (Station Copier, File Transfer Client or Backup) **which convert files to use the correct
  encryption key** for the target."
- "**CAUTION: Do not use Windows Explorer to copy files** … without the proper encryption those files **may
  not be readable**."
- "If the file passphrase and system passphrase are the same, a station copy proceeds without prompting"; else
  you are prompted for the file passphrase (`J8Startup/J8SpecifyingAStationDatabaseToInstall-9D4C67CE.txt:` passphrase-mismatch prompt).

| Location | Holds | Sealed with | Decryptable off-box? |
|---|---|---|---|
| **daemon User Home** `/home/niagara` | the **running** station `config.bog` + system files | **machine-only random key** (never leaves the JACE) | **No** — the key is not portable |
| **Niagara/portable User Home** | backups (`.dist`), exported/transferred stations | **passphrase-derived key** | **Only with the System Passphrase** |

## §466.3 — Why this refines the J8 `.bog` verdict (§14 to B464)

[Block 464] §464.4 said "a copied `config.bog` carries passphrase-encrypted secrets." That is exactly right
for a **backup/exported** copy (portable domain) — decryptable with the passphrase. But it **understates** the
wall for a **raw byte copy of the *running* `config.bog`** in `/home/niagara`: that file's secrets are sealed
with the **machine-only random key**, so they are **un-decryptable anywhere else, even with the passphrase**.
This is why the platform tools (Station Copier / File Transfer / Backup) exist: they **re-encrypt** on the way
out from the machine-key domain to the passphrase domain. A raw filesystem grab (bypassing those tools — e.g.
a hypothetical serial/root copy) yields ciphertext that **no passphrase can open**.

Sharper operator takeaways:
- **BackupService `.dist` route** ([Block 464] Route 2): the tool re-encrypts to the **passphrase** key →
  secrets recoverable **iff you know the passphrase**. This is the route that actually gives usable secrets.
- **Raw daemon-home copy** (any route that skips the platform tools): structure readable, secrets sealed with
  a **non-exportable machine key** → **secrets unrecoverable off-box**, period.
- **Windows-Explorer / plain `cp`**: explicitly warned to produce unreadable files — the encryption is not a
  wrapper you can strip by copying.

## §466.4 — Where credentials live (summary)

- **Station users** → inside `config.bog` (station database), SCRAM-verified ([Block 461]); their stored
  *secrets* are sealed by the domain rules above.
- **Platform accounts + System Passphrase** → platform/OS side ([Block 460]/[Block 463]); reset needs serial +
  Tridium signature.
- **Machine-only key** → generated per system, resident only on that JACE — the reason a stolen running
  `.bog` is inert off-device.

## Self-verify

| # | Claim | Marker | Citation | Checked |
|---|---|---|---|---|
| 1 | System passphrase encrypts client passwords in .bog/config.bog and .dist | [CERT-doc] | aPlatformSystemPassword | ✓ token |
| 2 | Applies to JACE-8000/9000; set at install; prompted on copy/restore | [CERT-doc] | same | ✓ |
| 3 | daemon-home files use a machine-only random key | [CERT-doc] | same (verbatim) | ✓ token "randomly generated key that exists only on that system" |
| 4 | portable files use a passphrase-derived key | [CERT-doc] | same (verbatim) | ✓ token "derived from the user-defined system passphrase" |
| 5 | platform tools re-encrypt between domains; Windows Explorer copy unreadable | [CERT-doc] | same | ✓ |
| 6 | can't install a .dist without its passphrase | [CERT-doc] | same | ✓ token |
| 7 | passphrase-mismatch prompt on station transfer | [CERT-doc] | J8Specifying…:passphrase | ✓ |

Marker tally: [CERT-doc] ×7 · [INFER] 0 load-bearing. **Block type: EVIDENCE + §14 refinement.** Ratio ≈ 0.

## Connections

- **[Block 464] §464.4** — **refined here** (raw running-`.bog` = machine-key wall, stronger than the
  passphrase wall stated there). [Block 464] carries a back-pointer to this refinement.
- **[Block 460]/[Block 463]** — platform credential model + passphrase reset.
- **[Block 462] §462.6** — the re-encrypt-on-transfer note, now given its two-key mechanism.

## Open gaps

Queued: J10, J11, J9, child gaps (requires-execution). No new gap.
