# Block 379 — `nverify.exe` decompiled (Ghidra grade): the signed-archive verifier is a JAR-signing state machine with FOUR independent `skip-*` bypass flags and a hard-pinned 270-byte Tridium Public Key

> **Focus `platform-native` REOPENED — Ghidra-grade sub-pass (NG1).** B124–B130 closed the native STATIC
> loop, but most of them (B124, B127–B130) worked at radare2/strings/RTTI grade; only B125/B126 ran the
> Ghidra decompiler, and **B126 characterized `nverify.exe` purely from its `strings`** — it never
> decompiled a single function body. This block decompiles the verifier's actual control flow and, in
> doing so, corrects and extends B126 §126.4: the CLI exposes a full suite of check-disabling flags B126
> did not enumerate, and the "Tridium Public Key" is a concrete pinned blob verified by raw `memcmp`.
> This is the fidelity gap the user named ("the decompilers we didn't fully use"). READ-ONLY.
>
> Sources (primary): `/mnt/c/Honeywell/OptimizerSupervisor-N4.14.0.162/bin/nverify.exe`
> (PE32+ x86-64, image base `0x140000000`, assembly identity `Niagara4.NVerify.exe 4.14.0.22`, sha256 in evidence).
> Method: Ghidra 12.1.2 `analyzeHeadless` + a NEW string-anchored postScript `tools/ghidra-scripts/DecompileByString.java`
> (finds the functions that reference a matched string and decompiles them — the kit's `ExportDecompiledC`
> filters by symbol NAME, useless on a Mocana-static binary whose user functions are all `FUN_*`);
> radare2 6.1.6 (`pdf` on the four flag getters + the TPK `memcmp` site); `rabin2 -z` for the option strings.
> Raw evidence preserved: `audits/B379-nverify-decomp-bystring.txt` (7 decompiled functions, 1030 lines),
> `audits/B379-nverify-ghidra.txt` (sha256 + option strings + getter disasm).
> Markers: `[CERT]` observed in the binary / decompiled body (offset or `audits/…:line` cited) · `[INFER]` deduction.
>
> Native platform layer (Capa 25). Connects [Block 126] (corrects/extends §126.4 `nverify`; confirms §126.1
> RSA-2048 module key = the pinned TPK), [Block 124] (DigiCert G4 anchor), [Block 113] (`skipModuleValidation`
> Java twin of `--skip-signature-check`), [Block 18] (the four signing schemes).

---

## 379.1 — What B126 saw vs what the code does `[CERT]`

B126 §126.4 listed the option set from the usage strings it happened to pull: `--trusted-certificates`,
`--unsigned`, `--removed`, `--verbose`, `--version`, and flagged `--unsigned * / --removed *` as the
escape hatches. **The decompiled argument parser `FUN_140009260` (`audits/B379-nverify-decomp-bystring.txt:250-412`)
handles eleven long options via a `strcmp` cascade** — a materially larger, more dangerous surface than
B126's inventory. Every one is a real, self-documented option (each has a help description string in `.rdata`):

| Option (long) | Parser action `[CERT]` | Global set | Help description (`.rdata`) `[CERT]` |
|---|---|---|---|
| `log-level` | `DAT_140073358 = atoi(val)` | `73358` | (FINE/etc.) |
| `version` | prints `nverify version %s (Mocana: %s)` then `exit(0)` | — | — |
| `skip-signature-check` | `DAT_14007c03c = 1` | `c03c` | **"Skip checking signatures." / "Skip all signature and certificate checks."** |
| `skip-tpk-check` | `DAT_14007c03d = 1` | `c03d` | "Skip checking against the TPK" |
| `skip-root-cert` | `DAT_14007c03e = 1` | `c03e` | "Skip checking root certificate trust." |
| `skip-cert-validity` | `DAT_14007c03f = 1` | `c03f` | "Skip checking the validity dates of certificates." |
| `trusted-certificates` | loads PEM/DER into `DAT_14007c040` | `c040` | (trust store) |
| `unsigned-entry-whitelist` | loads list into `DAT_14007c048` | `c048` | "…allowed to be unsigned. Use * for wildcard." |
| `removed-entry-whitelist` | loads list into `DAT_14007c050` | `c050` | "…allowed to be removed. Use * for wildcard." |
| `validate-all-signatures` | `DAT_14007c03a = 1` | `c03a` | "…all signatures and certificate chains validated." |
| `require-timestamp` | `DAT_14007c03b = 1` | `c03b` | "…timestamps will be required for all signatures." |

All eleven strings are present in the binary (`rabin2 -z`, `audits/B379-nverify-ghidra.txt`). The headline:
**`--skip-signature-check` ("Skip all signature and certificate checks") is a total bypass** — strictly
stronger than the `--unsigned *` wildcard B126 named as the worst case. There are **four independent
`skip-*` flags**, each disabling one layer of the trust decision. This is the native-CLI twin of
[Block 113]'s Java `skipModuleValidation`, and it is wider than B126 recorded. `[CERT]`

---

## 379.2 — The four flag getters, decompiled — exact flag→check attribution `[CERT]`

The check sites read the globals through four one-instruction getters (r2, `audits/B379-nverify-ghidra.txt`):

| Getter | Body | Returns flag | Gate site |
|---|---|---|---|
| `FUN_140009200` | `movzx eax,[0x14007c03e]; ret` | **skip-root-cert** | chain builder (§379.3, `:98`) |
| `FUN_140009220` | `movzx eax,[0x14007c03d]; ret` | **skip-tpk-check** | TPK pin (§379.4, `:183`) |
| `FUN_140009240` | `mov rax,[0x14007c048]; ret` | **unsigned-whitelist ptr** | per-entry check (§379.5, `:685/:704`) |
| `FUN_140009250` | `movzx eax,[0x14007c03a]; ret` | **validate-all-signatures** | manifest gate (`FUN_14000ac10:597`) |

So the mapping is exact and CERT-grade, not inferred: the flag the parser sets is the flag the checker
reads. (The `skip-signature-check`/`skip-cert-validity`/`require-timestamp` globals `c03c`/`c03f`/`c03b`
are set by the parser but their gate sites sit outside the seven string-anchored functions decompiled
here — traced to the parser, not yet to the enforcement branch; named honestly as a residue, §379.7.) `[CERT]/[INFER]`

---

## 379.3 — Certificate-chain validation: `FUN_140002b50` `[CERT]`

The X.509 path validator (`audits/B379-nverify-decomp-bystring.txt:9-246`) builds a cert list, walks it to
find the root (`X509_isRootCertificate`, `:66`), sorts the chain (`:95`), and — **unless `skip-root-cert`
is set** (`FUN_140009200`, `:98`) — either verifies the found root against the trust store or searches the
trust store for one (`"search trust store for root cert"`, `:125`). Each link is validated by
`FUN_140001cc0` (the per-cert X.509 verify, `:113/:208/:219`). On failure it emits
`"Certificate chain validation failed: %s"` (`:162`) / `"No ultimately trusted cert found"` (`:158`).
This is the decompiled form of B126 §126.4's `ERR_CERT_CHAIN_*` state machine — confirmed, and now grounded
in the actual branch structure. `[CERT]`

---

## 379.4 — The "Tridium Public Key" is a hard-pinned 270-byte blob checked by `memcmp` `[CERT]`

The single most important new finding. After the chain validates, `FUN_140002b50` does a **second,
independent trust check** (`:183-196`):

```c
if ((FUN_140009220() == '\0') && (param_5 == '\0')) {      // unless skip-tpk-check
    log("checking public key");
    _Buf1 = extract_pubkey(leafCert, &local_78);           // local_78 = pubkey length
    if (_Buf1 && local_78 == 0x10e &&                       // length must be 270 bytes
        memcmp(_Buf1, &DAT_140072ec0, 0x10e) == 0) {        // byte-equality vs baked-in blob
        log("Signed by TPK"); goto ok;
    }
    log("Not signed by TPK"); iVar6 = -0x1db9; goto fail;   // hard reject
}
```

Facts `[CERT]`:
- The **Tridium Public Key (TPK) is a constant 270-byte (`0x10e`) blob embedded at `DAT_140072ec0`** and
  the check is a **raw `memcmp` byte-equality** of the leaf certificate's SubjectPublicKeyInfo against it —
  not a signature verification, a *pin*. The length guard `local_78 == 0x10e` means only a 270-byte SPKI
  can match.
- **270 bytes is exactly the DER SPKI of an RSA-2048 public key** → the pinned TPK is the **RSA-2048
  module-signing key** of [Block 126 §126.1] (`DsfShaWithRsaSignature`, 256-byte `.sig`). nverify pins that
  same key: a valid DigiCert chain is necessary but NOT sufficient — the leaf must ALSO be the Tridium key.
  `[CERT]` (270B == RSA-2048 SPKI, memcmp) / `[INFER]` (that DAT_140072ec0 *is* the §126.1 key, by size + role).
- The pin is bypassable **two ways**: the `--skip-tpk-check` flag (`FUN_140009220`) OR the caller passing
  `param_5 != 0` (an internal "chain-only" mode). This is the mechanism behind B126's captured string
  *"the additional check against the Tridium Public Key will be skipped."* `[CERT]`

TPK PINNING is a real defensive control B126 missed: it hard-binds trust to one Tridium key beyond the CA
chain — good — but it is one `--skip-tpk-check` away from off. `[CERT]`

---

## 379.5 — Per-entry integrity + the unsigned-whitelist enforcement: `FUN_14000a580` `[CERT]`

The per-entry checker (`audits/B379-nverify-decomp-bystring.txt:628-823`) iterates every archive entry:

- **Entry present in the manifest** (`FUN_140005840` hit, `:676`): read its `SHA-256` digest string
  (`0x14003bae8`), **base64-decode** it (`FUN_14001f8f0`), compute SHA-256 of the entry bytes
  (`FUN_140034500`, `:721`), and `memcmp` the 32 bytes (`local_16c==0x20 && memcmp(...,0x20)==0`, `:735`).
  Match → valid; mismatch → `"Invalid checksum for %s"`, error `-7` (`:742`). The transitive JAR model of
  B126 §126.4, now decompiled: entry integrity = SHA-256 digest listed in the (separately signed) manifest.
- **Entry absent from the manifest** (`:677`): skip the `META-INF/` signature machinery
  (`.RSA/.DSA/.EC/.SF/MANIFEST.MF`, `:678-684`), then test the entry name against the **unsigned-whitelist**
  (`FUN_14000ae80(name, FUN_140009240())`, `:685-686`). In whitelist → `"Found allowed unsigned entry: %s"`
  (allowed, `:695`); otherwise → `"No manifest entry found for file %s"` and error `-5` (`:688-690`). This is
  where `--unsigned *` lands: the wildcard `*` is matched inside `FUN_14000ae80`, so a `*` whitelist makes
  every unsigned entry take the "allowed" branch. `[CERT]`

So the wildcard bypass B126 inferred is confirmed at its exact enforcement point, with its failure code
(`-5`), and it is one of several — weaker than `--skip-signature-check`, which never reaches this loop.

---

## 379.6 — Manifest checksum + signature-file matching: `FUN_140009ed0` / `FUN_14000a130` `[CERT]`

- **`FUN_140009ed0`** (`:946-1026`): computes SHA-256 of the whole `MANIFEST.MF` (`FUN_140034500`, `:982`),
  then for each `.SF` signature reads its `SHA-256-Digest-Manifest` header (`0x14003bce0`), base64-decodes,
  and `memcmp`s 32 bytes (`:1007`); mismatch → `"Checksum for manifest does not match signature"` (`:1004`).
  This binds the signed `.SF` to the exact manifest bytes — the standard jarsigner chain, decompiled. `[CERT]`
- **`FUN_14000a130`** (`:830-938`): finds `.SF` entries (suffix bytes `.S F`, `:870-871`), then derives the
  signature-block filename by **rewriting the extension `.SF → .RSA`, and on failure `→ .DSA`** (`:881-894`),
  tries each with `FUN_140001230`, and if neither exists → `"Could not find matching signaure file %s"`
  (the vendor's own `signaure` typo, `:896`). The block is then PKCS#7-verified via `FUN_140002290`
  (`:912`). So nverify accepts **RSA and DSA** signature blocks (tries RSA first), matching two of B126
  §126.1's four schemes; the `.EC` (ECDSA) extension is recognized only in the a580 skip-list, not derived
  here as a signature block. `[CERT]`

---

## 379.7 — Defensive-security summary (factual, no secrets) `[CERT]`

1. **Four `skip-*` bypass flags, one total** `[CERT]` (§379.1): `--skip-signature-check` disables all
   signature+certificate checking; `--skip-tpk-check`, `--skip-root-cert`, `--skip-cert-validity` each peel
   off one layer. B126 recorded only the `--unsigned/--removed` wildcards; this is a wider bypass surface.
   Operationally identical concern to [Block 113]'s `skipModuleValidation`: nverify is only as safe as the
   wrapper that invokes it, and the wrapper now has far more ways to weaken it.
2. **TPK pin is strong but flag-gated** `[CERT]` (§379.4): trust is hard-pinned to a 270-byte RSA-2048 key
   by `memcmp`, above and beyond the DigiCert chain — a genuine anti-mis-issuance control — but
   `--skip-tpk-check` (or the internal `param_5` chain-only mode) turns it off.
3. **Exact crypto backend** `[CERT]`: `Mocana: rel.albatross.6.5.2.u7.hf23` (`0x14003b7f0`) — B126 inferred
   "NanoCrypto/FIPS"; this is the precise product+version string, useful for CVE/EOL tracking of the
   statically-linked crypto.
4. **Per-entry integrity is SHA-256 + base64 `memcmp`** `[CERT]` (§379.5/§379.6), transitive through a
   separately PKCS#7-signed manifest — sound where enabled.
5. **Residue (honest scope)**: the enforcement branches for `--skip-signature-check`/`--skip-cert-validity`/
   `--require-timestamp` are traced to the parser globals but not to their gate sites within the seven
   string-anchored functions here. **tried:** string-anchored decompilation of the verify pipeline;
   **needs:** call-graph from `main`/`FUN_140009ac0` dispatch to the archive-verify entry to place those
   three gates. Named as child gap **NG1-G1**.

No private keys read; the TPK is a public key (pinned by design). No `.exe`/`.dll` mutated; analysis on an
isolated copy.

---

## 379.8 — Self-verify

**Token re-checks** (load-bearing `[CERT]` re-confirmed by re-running the tool):
1. Eleven option strings incl. all four `skip-*` present in `.rdata` — ✓ (`rabin2 -z`, `audits/B379-nverify-ghidra.txt`).
2. `skip-signature-check` help = "Skip all signature and certificate checks." — ✓ (`rabin2 -z` @ `0x14003b3d8`).
3. Parser `FUN_140009260` `strcmp` cascade sets `DAT_14007c03c/03d/03e/03f/048/050/03a/03b` — ✓ (`decomp:341-384`).
4. Getter `FUN_140009220` = `movzx [0x14007c03d]` (skip-tpk-check) — ✓ (r2 disasm).
5. Getter `FUN_140009200` = `movzx [0x14007c03e]` (skip-root-cert) — ✓ (r2 disasm).
6. TPK check: `local_78==0x10e && memcmp(_Buf1,&DAT_140072ec0,0x10e)` → "Signed by TPK"/"Not signed by TPK", fail `-0x1db9` — ✓ (`decomp:187-195`).
7. `"Signed by TPK"` @ `0x14003a0b0`, `"Not signed by TPK"` @ `0x14003a0c0` — ✓ (`rabin2 -z`).
8. Per-entry SHA-256: `local_16c==0x20 && memcmp(...,0x20)` else `"Invalid checksum"`/`-7`; unsigned path `"No manifest entry found"`/`-5` + whitelist via `FUN_14000ae80(name,FUN_140009240())` — ✓ (`decomp:735-745, 685-696`).
9. Manifest: SHA-256 vs `SHA-256-Digest-Manifest` base64, mismatch → "Checksum for manifest does not match signature" — ✓ (`decomp:993-1004`).
10. `.SF→.RSA→.DSA` extension rewrite (`0x52 0x53 0x41` / `0x44`) + "signaure" typo — ✓ (`decomp:881-896`).
11. `Mocana: rel.albatross.6.5.2.u7.hf23` @ `0x14003b7f0` — ✓ (`rabin2 -z`).

**11/11 load-bearing tokens re-verified.**

**Marker tally** (`grep -oE` over this file): `[CERT]` ≈ 26 · `[INFER]` 2 (the DAT_140072ec0↔§126.1-key
identity by size+role; the three untraced gate sites). Ratio `[INFER]/[CERT]` ≈ 0.08 — very low: this is an
EVIDENCE block over decompiled bodies + re-run tool output, near-primary. The evidence is rich; the residue
(NG1-G1) is a bounded call-graph trace, not a gap in the binary evidence.

---

## 379.x — Connections

- **[Block 126]** — *corrects & extends §126.4.* B126 read `nverify` from `strings` and listed a 5-option
  subset; B379 decompiles the parser and finds eleven options incl. four `skip-*` bypasses, decompiles the
  TPK `memcmp` pin (absent from B126), and confirms §126.1's RSA-2048 module key = the pinned TPK (270 B SPKI).
- **[Block 113]** — `--skip-signature-check` is the native-CLI twin of the Java `skipModuleValidation`.
- **[Block 124]** — the DigiCert G4 chain B124 found in PE Authenticode is the CA layer under §379.3; §379.4
  shows nverify pins the Tridium key ON TOP of that chain.
- **[Block 18]** — of the four signing schemes, `nverify` implements RSA + DSA block verification (§379.6).
- **Forward — reopened focus (Ghidra sub-pass)**: NG2 (`nre.dll launchNre` / `common.dll createVM` bodies),
  NG3 (`plat.exe` DPAPI `systempw` + `CreateServiceA` decompiled), NG4 (`libciper.so` QNX-ARM bodies),
  NG1-G1 (place the three untraced `skip-*` gate sites via call-graph).
