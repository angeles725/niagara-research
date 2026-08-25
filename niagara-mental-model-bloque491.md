# Block 491 — secrets-at-rest: the three-layer KeyMaterial(`.km`)→KeyRing(`.kr`)→secret AES-256-GCM chain, and how the root `.km` is protected per-OS (Windows DPAPI machine-scope vs QNX/Linux plaintext) — closing B482-G2 and answering the "can a copied station's secrets be read off-box" question

> **Focus:** `secrets-at-rest` (new; bootstrap block). READ-ONLY, decompiled `nre-ext`; no binary run.
> SECRETS DISCIPLINE: key structure/derivation only. Closes **B482-G2**. Native DPAPI-flag corroboration by
> sibling session Segundo (in-flight). Markers §3, file:line.

## §491.1 — The three-layer chain `[CERT]`

1. **KeyMaterial (`.km`)** = a raw **32-byte AES-256 key**, `SecureRandom` random-per-install
   (`security/km/KeyMaterialFactory.java:12,36-41`; auto-generate `:66-74`). The *master* key. **NOT
   password/passphrase-derived** (no KDF).
2. **KeyRing (`.kr`)** = per-alias key entries, the blob AES-encrypted under the `.km` bytes:
   `version=5, magic=357109530, iv[16], AES/GCM/NoPadding, GCMParameterSpec(128,iv),
   SecretKeySpec(km.getKeyMaterial(),"AES")` (`security/SimpleKeyRing.java:379-393,161-168`; legacy v≤3 = CBC).
   Each entry itself also AES-GCM under `.km` (`SimpleKeyRing$SimpleKeyRingEntry:521-561`).
3. **Secret file** = encrypted under a per-alias key drawn from the KeyRing: `Aes256PasswordManager.getKey(alias)`
   → `kr.getKey(alias)` → AES-256-GCM over the payload (`Aes256PasswordManager.java:193-206,37-46`).

Wiring: `KeyRingFactory.getInstance(dir,".kr",".km")` → `KeyMaterialFactory.getInstance(dir,".km").getKeyMaterial()`
→ `new SimpleKeyRing(dir,".kr",km)` (`security/KeyRingFactory.java:63-67`).

## §491.2 — How the root `.km` is protected (the crux, closes B482-G2) `[CERT]`

The `.km` is random-per-install; its OWN at-rest protection is delegated to `IPlatformProvider.get/setKeyMaterial`
(`security/km/PlatformKeyMaterial.java:46,52`) and is **PER-OS**:
- **Windows → machine-bound via DPAPI.** `NativePlatformProviderTridium.setKeyMaterial` wraps with
  `DpapiUtil.encrypt(km, isKeyMaterial=true, localMachine=true)`; read `DpapiUtil.decrypt(enc, true)`
  (`platform/NativePlatformProviderTridium.java:482,435`). `DpapiUtil.encrypt/decrypt` are `native`
  (`util/DpapiUtil.java:8-14`) → **Windows DPAPI `CryptProtectData`/`CryptUnprotectData`, LOCAL_MACHINE scope**.
- **QNX / JACE → PLAINTEXT** at `/etc/km` via `SimpleKeyValueUtil` (plain `FileInputStream/OutputStream`,
  `util/SimpleKeyValueUtil.java:56,95`; provider `:431-432,479-480`). Secrecy = OS file protection only. = [B466]'s
  "machine-only random key" domain.
- **Linux native → PLAINTEXT** in the security dir, POSIX `rw-rw----` (`NativePlatformProviderTridium.java:436-437,
  484-493`).
- **Pure-Java/workstation** → plaintext via `SimpleKeyValueUtil`; `supportsKeyMaterialRecovery()=false`.
- **`SimpleKeyMaterial`** (dev/test, env `NIAGARA_USE_SIMPLE_KM`) → plaintext, "NOT FOR PRODUCTION USE".

So the Java AES layer protects `.kr` + the payloads; the **root `.km` is protected by an OS primitive** — DPAPI
(machine scope) on Windows, **plain file permissions** on QNX/JACE and Linux (no crypto wrapping in Java).

## §491.3 — Inventory of protected secrets `[CERT]`

All resolve through `KeyRingFactory` → `Aes256PasswordManager`:
- Subscription: `baja.licensing.subscription.{ecKeyPair,refreshIncrement,restoreId}` ([B480]).
- Default station password/secret encoder: `javax.baja.security.BAes256PasswordEncoder.key`
  (`Aes256PasswordManager.java:16`).
- Daemon/platform: the *system* keyring (rolled by `NiagaraDaemon.java:849`) — syslog TLS, UpdateDaemonServlet,
  FileServlet, etc.
- BOG/file transcoding: `KeyRingEncrypting/DecryptingStream` + `BogTranscoderInputStream`.
- Crypto core stores: user truststore password + the `signers` registry ([B482]) via the same KeyRing path.
- **Separate chain (NOT `.km`-wrapped):** the System Passphrase `.sp` — Windows encrypted Registry `systempw`,
  QNX native `SyspwUtil`, Linux `/etc/niagara/.sp` plaintext (`NativePlatformProviderTridium.java:239-326`). Flag
  as adjacent.

## §491.4 — Portability: can a copied station's secrets be read off-box? `[CERT]/[INFER]`

**Not by copying files alone — but the binding is machine-scoped, not user/password-scoped, and uneven:**
- **Windows:** `.kr`/`.km` are useless off-host — `.km` is DPAPI LOCAL_MACHINE-wrapped; `CryptUnprotectData`
  only succeeds on the same machine. Caveat: LOCAL_MACHINE scope means **any account/process on that host** can
  decrypt (no user/password gate). `[CERT + INFER scope]`
- **QNX/JACE & Linux:** `.km` is **plaintext** — anyone who can read those bytes (root, backup, disk image, or a
  station copy that includes `/etc/km`) decrypts `.kr` and every secret **offline**. "Machine-bound" only in that
  the random key lives on that box; NOT cryptographically hardware-bound. `[CERT]` — matches [B466].
- General: `.km` is symmetric random (never passphrase-derived) → **possession of `.km` = decryption everywhere.**
  Windows raises the bar to "run code on the same machine"; QNX/Linux lower it to "read the key file."

## §491.5 — Key roll & recovery `[CERT]`

- Interval `niagara.keyMaterialRollInterval` default **365 d** (`KeyRing.java:23-25`); driven by daemon
  `checkRollKeyMaterial` (`NiagaraDaemon.java:849`) + subscription `RotateKeys` (`:42,57`).
- `rollKeyMaterial` (`KeyRing.java:168-238`) is atomic/crash-safe: decrypt all → write `.km.rec`+`.kr.rec` →
  new random `.km` → re-encrypt entries → delete `.rec`. Recovery on next load rolls back from `.rec` twins
  (`KeyRingFactory.java:66-100`); neither `.rec` + load fail → `SecurityException("Key ring is corrupt and
  unrecoverable.")`. No-op on pure-Java (recovery unsupported).

## §491.6 — Self-verify

| # | Claim | Marker | Citation | Verdict |
|---|---|---|---|---|
| 1 | 3-layer: .km(random 32B) → .kr entries (AES-GCM) → secret (AES-256-GCM per-alias) | `[CERT]` | `KeyMaterialFactory.java:36-41`; `SimpleKeyRing.java:379-393`; `Aes256PasswordManager.java:193-206` | PASS |
| 2 | .km is random-per-install, NOT password-derived | `[CERT]` | `KeyMaterialFactory.java:36-41,66-74` | PASS |
| 3 | .km protection per-OS: Windows DPAPI LOCAL_MACHINE; QNX/Linux plaintext (file perms) | `[CERT]` | `NativePlatformProviderTridium.java:435,482,431-432,436-437`; `DpapiUtil.java:8-14` | PASS (closes B482-G2) |
| 4 | Windows off-box useless (DPAPI machine) but any local account decrypts; QNX/Linux .km plaintext = offline-readable | `[CERT]`/`[INFER]` | `:482`; `SimpleKeyValueUtil.java:56,95` | PASS |
| 5 | System passphrase .sp is a SEPARATE chain (not .km-wrapped) | `[CERT]` | `NativePlatformProviderTridium.java:239-326` | PASS |
| 6 | Roll 365d + crash-safe .rec recovery | `[CERT]` | `KeyRing.java:23-25,168-238`; `KeyRingFactory.java:66-100` | PASS |

**Tally:** 6 claims, all `[CERT]` (1 `[INFER]` sub-part = DPAPI scope semantics; Segundo confirming native flags).

## §491.7 — Connections & open gaps

- Closes [B482-G2]; extends [B480] (subscription secrets), [B466] (JACE two domains), [B424] (Windows hidden key).
- **B491-G1** native DPAPI flag confirmation (`CryptProtectData` `CRYPTPROTECT_LOCAL_MACHINE`, the wrapped blob) —
  Segundo's native pass. **B491-G2** the `.sp` System-Passphrase chain in depth (its own focus-worthy surface).
- Security tie-in: on QNX/JACE and Linux the at-rest secret protection is **file-permission-deep, not
  crypto-deep** — a disk image / backup / root read yields all station secrets ([B490] SEC-12 scoping).
