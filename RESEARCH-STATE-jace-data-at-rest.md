# RESEARCH-STATE — focus: jace-data-at-rest (what the JACE-8000 boot microSD yields to an offline attacker: keyrings, OS creds, keystores — and whether SD possession alone decrypts the station secrets)

> Multi-focus corpus (METHODOLOGY §16). Focus **BOOTSTRAPEADO 2026-08-30** (2ª opción del operador tras cerrar
> `jace-station-config`). Fuente = los almacenes de secretos de la station `JACE_UMBRELLA` extraídos READ-ONLY
> del microSD (`local-sd-image/jace-sd.img`, P2 QNX6, via `tools/qnx6read.py`). Artefacto `live-install` →
> **SECRETS DISCIPLINE MÁXIMA**: este focus ES sobre secretos. Se cita SÓLO ESTRUCTURA (rutas, tamaños, magic
> bytes, algoritmos, longitudes de clave, nombres de cuenta de rol, Host ID enmascarado). NUNCA un valor de
> hash/clave/keyring. Se aplica la lección del retro D1: enmascarar ANTES de imprimir, `grep -c` (no `grep`),
> verificar cada máscara.
>
> **Ángulo (pregunta central):** ¿qué obtiene EXACTAMENTE quien posee físicamente el SD, offline? En particular:
> ¿los campos reversibles/`BPassword` del config.bog (encoding `reversibleEncodingKeySource="keyring"`) se
> pueden descifrar con SÓLO el SD (el keyring está en disco), o la clave efectiva está ATADA al hardware
> (ECC508 HSM / Host ID / TPM)? Profundiza B466 (machine-key vs passphrase), B684/B689 (veredicto débil-en-reposo),
> B685 §685.5 (admin PBKDF2 en el config). Data-at-rest OFFLINE — no requiere hardware vivo.

<!-- research-state.v1 -->
schema: research-state.v1
covered_blocks: 693
gaps_closed: 5
known_gaps: 7
investigable_open: 1
requires_execution_open: 1
blocked_open: 0
deferred_open: 0
undocumented_findings: 0
block_scope: shared-global
<!-- /research-state.v1 -->

focus: jace-data-at-rest
status: active (bootstrapped 2026-08-30; backlog seeded from measured P2 security-file inventory)
bootstrapped_on: 2026-08-30
block_prefix: niagara-mental-model-bloqueN.md (numeración global; próximo libre: B693)

## Coverage

- **Covered blocks**: 692 corpus-wide (this focus: B693-) (shared-global)
- **Source (out of git)**: SD P2 security files under `local-sd-image/` (gitignored, secret-bearing): keyrings
  `/.fskey/.key` (156B), `/etc/km/.km` (32B), `/home/niagara/security/.kr` (665B); OS creds `/etc/passwd`,
  `/etc/shadow` (343B), `/etc/opasswd`, `/etc/oshadow`; keystores `keystore.jceks` (4476B), `cacerts.jceks`,
  `untrusted.jceks`, `signing/signers` (33021B), `exemptions.tes`; JRE crypto `java.security`, `cacerts.bcfks`
  (BC-FIPS), policy/{limited,unlimited}; vendor certs + licenses (Host ID `Qnx-TITAN-44A2-****-****-363E`).
- **Coverage metric**: 5 / 7 gaps closed (DAR1-5; DAR2-G1 req-exec)

## Gap-backlog (prioritized)

| Priority | Gap | Type | Status |
|---|---|---|---|
| high | DAR1 the keyring trio (/home/niagara/security/.kr, /etc/km/.km, /.fskey/.key) — format, magic, size, what each holds, how they relate; the reversible-encoding key store | keyring binary disk | closed (B693 — .kr=serialized Java KeyRing, .km=32B cleartext master key, .fskey=156B fs key; .km-in-clear seeds DAR2) |
| high | DAR2 THE question: can config.bog reversible/BPassword fields be decrypted from the SD ALONE, or is the effective key hardware-bound (ECC508/Host ID/TPM)? — bound it from disk+corpus | analysis + corpus | closed (B694 — verdict H1: software keyring on disk, .km cleartext, NOT ECC508; SD=all key material offline; §14 refines B466 threat model) |
| high | DAR3 /etc/shadow + /etc/passwd — the QNX OS accounts, hash algorithms, account inventory (structure; hashes MASKED) | os-cred disk | closed (B695 — 7 accounts, 5 no-login; only admin+operator log in; PBKDF2-HMAC-SHA256 ~10k = same primitive as config.bog; no aging) |
| medium | DAR4 station keystores (keystore.jceks 4476B, cacerts.jceks, untrusted.jceks, signing/signers) — what THIS unit holds (TLS default-cert private key? signing keys?), JCEKS format | keystore disk | closed (B696 — keystore.jceks=TLS keypair alias 'default'=factory ForRecoveryPurposes self-signed; cacerts/untrusted empty; signers=module store REMITTANCE B392) |
| medium | DAR5 JRE crypto policy deployed (java.security, policy limited vs unlimited, cacerts.bcfks BC-FIPS) — the crypto configuration on this unit | jre-config disk | closed (B697 — standard non-FIPS Sun stack, unlimited default, weak TLS/algos disabled; FIPS shipped-not-engaged across every layer) |
| medium | DAR2-G1 the actual decryption PoC (KeyRing unwrap w/ .km -> derive reversible key -> decrypt a config.bog BPassword field via Mocana AES-256-CBC) | requires-execution | requires-execution (implement Niagara keyring unwrap + Mocana AES-256-CBC) |
| low | DAR6 SYNTHESIS — the recoverable-from-SD-alone verdict: exactly what SD possession yields vs what needs live hardware (extends B466/B684/B689) | synthesis | pending |

`tried:` (none blocked yet — all sources confirmed present on SD P2 and extractable via qnx6read.py; SOURCE-BEFORE-AGENT passes).

## Remittance (ya cubiertos — NO son gaps de este focus)

- PKI model, cacerts/cacerts.bcfks BC-FKS, vendor `.certificate` (DSA-1024 root), keystore chains, `.sig` → focus `signing-pki` [Block 392]–[Block 396].
- Licenses (`.license`, DSA-signed, Host ID binding) + licensed-vs-unlicensed on disk → focus `license-diff` [Block 386]–[Block 391]/[Block 442].
- Machine-key vs passphrase encryption DOMAINS (daemon-home indescifrable off-box vs portable/.dist) → focus `jace8000` [Block 466].
- Per-field `BPassword` GCM (tamper-evident only that field) → [Block 393].
- ECC508 HSM engine + Mocana NanoCrypto (AES-256-CBC, CTR-DRBG) + OpenSSL 2nd stack → focus `jace8000-qnx-native` [Block 677]/[Block 681]/[Block 684].
- Weak-data-at-rest VERDICT (QNX6 cleartext, SD=full compromise) → [Block 684]; audit trails local-only no egress → [Block 689]; admin PBKDF2-10k in config → [Block 685] §685.5.
- The /etc/shadow OS-hash CRACK attempt (hashcat -m 10900, rockyou, 0/3 recovered, EXHAUSTED) → engram (2026-08-30). This focus documents STRUCTURE, does not re-crack.
- Keyring framework internals (.km/.kr DPAPI-equivalent, KeyRing API) → base corpus / [Block 466]; this focus opens the ON-DISK FILES.

## Iteration history

| # | Date | Gap closed | Block | Delegated? · model tier | New gaps uncovered |
|---|---|---|---|---|---|
| — | 2026-08-30 | (bootstrap — P2 security-file inventory) | — | no · inline (p2-tree scan + qnx6read source-confirm) | DAR1–DAR6 seeded |
| 1 | 2026-08-30 | DAR1 keyring trio structure | B693 | no · inline (secrets-sensitive: magic/entropy only, no key bytes) | 0 new (.km-cleartext seeds DAR2) |
| 2 | 2026-08-30 | DAR2 SD-alone-decrypt verdict | B694 | no · inline (analysis + §14 refine B466) | DAR2-G1 (requires-execution: decryption PoC) |
| 3 | 2026-08-30 | DAR3 OS accounts (passwd/shadow) | B695 | no · inline (secrets-sensitive: skeleton only, hashes masked) | 0 new |
| 4 | 2026-08-30 | DAR4 station keystores | B696 | no · inline (keystore magic/aliases, no private keys) | 0 new |
| 5 | 2026-08-30 | DAR5 JRE crypto policy | B697 | no · inline (java.security config read) | 0 new |

## Blocked gaps (each tagged with what it needs)

(none yet — all read-only investigable from on-disk artifacts.)

## Stop control (primary = read-only-investigable exhaustion, METHODOLOGY §8)

- **Open gaps — read-only investigable**: 1
- **Open gaps — requires-execution**: 1 (DAR2-G1)
- **Open gaps — blocked**: 0
- Budget cap: none

## Dismissed file types

- (to be filled by the coverage pass as gaps close.)
