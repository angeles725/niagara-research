# B684 — JACE-8000 security verdict (QN8 synthesis): strong at the BOOT/FIRMWARE and PROCESS layers (CertISW secure boot + ENCRYPTED firmware payload + ECC508 secure element + FIPS Mocana crypto + de-privileged daemons that refuse root), but the QNX6 DATA partition is NOT encrypted at rest — physical possession of the microSD yields the config.bog, keyrings, `/etc/shadow` and audit history in the clear, and the factory credential sits in plaintext — so PHYSICAL ACCESS is the weak link (focus jace8000-qnx-native, QN8; §18-adjacent synthesis)

> **Focus:** `jace8000-qnx-native` (§16). **Gap closed:** QN8 (consolidated JACE-8000 security posture).
> **Type:** synthesis (no new decompilation). **Method:** consolidates this focus (B677-B683) + the JACE
> security thread (B460/B461/B466/B468/B672/B674/B676/B678) + the Windows-supervisor contrast (platform-native,
> B381/B385).
> **Bottom line:** the JACE-8000 is **hardened where an attacker comes over the wire or the boot chain, and
> weak where an attacker holds the card.** Boot/firmware and process isolation are genuinely strong; **data at
> rest on the QNX6 filesystem is not encrypted**, so the microSD is the single point of full compromise.

---

## §684.1 — STRONG: boot & firmware chain of trust `[CERT]`

- **Secure boot:** ROM→MLO→U-Boot→`go` the **CertISW**-wrapped image ([Block 672]/[Block 676]); the firmware
  is a TI GP signed image (integrity) — the payload is **also ENCRYPTED** (entropy ≈ 7.998, no binwalk
  signatures, [Block 682]) → confidentiality of the OS image, not just integrity.
- **Hardware root of trust:** an **ATECC508 secure element** (`Ecc508QnxRmEngine`, [Block 678]) + the
  machine-key domain ([Block 466]) — the firmware decryption key is device-bound, so the OS image cannot be
  unpacked off the card ([Block 682]).
- **Code signing:** the factory payload is Tridium/DigiCert-signed (RSA-2048/SHA-256, [Block 676]); deployed
  modules are re-signed to the Honeywell PKI ([Block 392]); `moduleVerificationMode` **defaults to `medium`**
  on the JACE (signed-by-trusted-cert required) — stricter than the Windows supervisor, which ran `low`
  (warnings only, B398/B519).

## §684.2 — STRONG: crypto + process isolation `[CERT]`

- **Two vetted crypto stacks:** Mocana **NanoCrypto** (static in `libdsfspi`, the DSF JCE provider — AES-256-CBC,
  NIST **CTR-DRBG**, FIPS-gated, [Block 677]/[Block 380]) + **OpenSSL `libcrypto.so.2`** (in `libcommon`, TLS/
  platform, [Block 681]).
- **De-privileged daemons:** both long-running processes **refuse to run as root** and drop to dedicated
  accounts — `niagarad`→uid 200 ([Block 679]), `station`→uid 300/`station_owners` ([Block 683]) — fail-closed
  if the drop fails. This is a **tighter posture than the Windows Supervisor**, where `plat.exe` runs as
  **LocalSystem** ([Block 381]).
- **Network hardening (live):** TLS-1.3-only :5011 platform daemon ([Block 657]/[Block 468]/[Block 474]),
  SSH/telnet/plaintext-Fox off, HSTS, 802.1X supplicant available ([Block 678]).

## §684.3 — WEAK: data at rest on the QNX6 filesystem `[CERT-hw]` (the crux)

The strong firmware crypto does **NOT** extend to the QNX6 data partition. Read directly from the microSD with
a userspace QNX6 reader ([Block 674]/[Block 682]), all of the following came out **in the clear**:
- `config.bog` — a **plain ZIP** (not encrypted); its station users are PBKDF2 hashes but fully readable.
- `/etc/shadow` + `/etc/passwd` — the QNX OS accounts (`admin`, `Luis`) with their password hashes.
- The keyring/keystores — `/etc/km/.km`, `/home/niagara/security/.kr`, `keystore.jceks`, `.fskey/.key`.
- History — `SecurityHistory.hdb`, `AuditHistory.hdb` (the audit trail).
- `fac.properties` — the **factory credential in plaintext** ([Block 672]).

So **data-at-rest protection on the JACE-8000 is per-FIELD, not whole-partition**: only *reversible* `BPassword`
fields (SMTP, subordinate creds) are AES-wrapped by the machine key ([Block 562]/[Block 466]) — and this
station stores none. The **partition itself is cleartext**. (Contrast: the JACE-9000/ATLAS reports full-disk
encryption, [Block 665]; the JACE-8000/QNX does not.) **Physical possession of the SD = full disclosure of
the controller's stored data.**

## §684.4 — Credentials `[CERT-hw]`

- **All login credentials are PBKDF2-HMAC-SHA256, 10,000 iterations** — OS (`/etc/shadow`) and station users
  alike (same scheme as the Supervisor, [Block 562]). One-way (no decrypt). **10k is low** by modern guidance
  ([Block 562]) → a wordlist attack is feasible for human-chosen passwords, though `admin`/`Luis` were **not**
  a common/default password (targeted + ~35-candidate checks failed; rockyou attempt in progress).
- **No reversible service secrets** in this station (no SMTP/subordinate/driver passwords) → nothing to
  decrypt via the keyring here.
- **Recovery of access** (owner) is therefore by **reset** (Platform Account Recovery [Block 463], or editing
  the SD), not by decryption; cracking is the only path to the *existing* password and is low-probability.

## §684.5 — Weak spots & operator actions `[CERT]/[INFER]`

| Weakness | Evidence | Action |
|---|---|---|
| QNX6 data partition unencrypted; SD = full data disclosure | [Block 674]/§684.3 | Physically secure the card; treat SD extraction as full compromise |
| Factory credential in plaintext on P1 + P3 | [Block 672] | Remove/rotate factory defaults on fielded cards |
| Exposed `admin` credential (live) + default/expired TLS certs | [Block 468] | Rotate admin creds; replace default certs |
| PBKDF2 iteration count fixed at 10k (low) | [Block 562] | Not operator-configurable; use strong passwords |
| Two crypto stacks (Mocana + OpenSSL) = double patch surface; OpenSSL `libcrypto.so.2` version untracked | [Block 681] | Track OpenSSL CVEs at firmware-update time (QN5-G1) |

## §684.6 — One-line verdict

**The JACE-8000 protects "who can boot/run what" and "who reaches it over the network" strongly (secure boot +
encrypted firmware + ECC508 + FIPS crypto + de-privileged, root-refusing daemons), but it does NOT protect
"what is stored on the card" — the QNX6 filesystem is cleartext, so the microSD is the weak link.** This is the
JACE-8000 instance of the corpus thesis ([Block 392]/[Block 468]): vendor-grade protection of code/identity,
weaker protection of data/evidence, and physical media as the soft underbelly.

## §684.7 — Self-verify

| # | Claim | Marker | Cite |
|---|---|---|---|
| 1 | Secure boot CertISW + encrypted firmware payload + ECC508 device-bound key | [CERT] | [Block 676]/[Block 682]/[Block 678] |
| 2 | Dual crypto (Mocana FIPS + OpenSSL); moduleVerificationMode default medium (> supervisor low) | [CERT] | [Block 677]/[Block 681]; B398/B519 |
| 3 | niagarad + station de-privileged, refuse root (vs Windows LocalSystem) | [CERT] | [Block 679]/[Block 683]/[Block 381] |
| 4 | QNX6 data partition NOT encrypted; config.bog/shadow/keyrings/audit read in clear from SD | [CERT-hw] | [Block 674]/§684.3 |
| 5 | All login creds PBKDF2-HMAC-SHA256 10k (OS + station); 10k low; no reversible secrets here | [CERT-hw] | [Block 562]/§684.4 |
| 6 | Physical SD possession = full data compromise (the weak link) | [CERT-hw] + [INFER] | §684.3/§684.6 |

**Tally:** 6 claims — 5 [CERT]/[CERT-hw], 1 [CERT-hw]+[INFER] synthesis. 0 unmarked.

## §684.8 — Connections

- **[Block 677]-[Block 683]** — the QN1-QN7 native findings this consolidates.
- **[Block 468]** — live security posture (the network-hardening + exposed-cred half).
- **[Block 672]/[Block 674]/[Block 676]/[Block 682]** — SD anatomy, tree, signing, firmware encryption.
- **[Block 466]/[Block 562]** — key domains + the password-encoder chain (PBKDF2 vs reversible).
- **[Block 392]** — the corpus signing thesis this instantiates; **[Block 381]** — the Windows-daemon contrast.
