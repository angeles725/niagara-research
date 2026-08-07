# Block 383 — SYNTHESIS of the platform-native Ghidra sub-pass (B379–B382): decompiling the function bodies the strings-grade blocks read from the outside, and the security facts only the decompiler could reach

> **Focus `platform-native` — Ghidra sub-pass synthesis (NG1–NG4).** The static native loop (B124–B130,
> 2026-06-29) was declared closed, but most of it worked at radare2 / `strings` / RTTI grade; only B125/B126
> ran the Ghidra decompiler, and even B126 read `nverify.exe` and `libciper.so` from strings/symbols alone.
> This sub-pass (2026-08-07, B379–B382) decompiled the FUNCTION BODIES of the four load-bearing native
> binaries. This block consolidates what that grade change revealed and the method that made it disciplined.
> READ-ONLY. Synthesis block (type: SYNTHESIS — [INFER] expected and healthy).
>
> Consolidates [B379] (`nverify.exe`), [B380] (`njre.dll`), [B381] (`plat.exe`), [B382] (`libciper.so`);
> corrects/extends [B126], [B129]; remits to [B125]. Sources are those four blocks' cited evidence
> (`audits/B379-*`, `B380-*`, `B381-*`, `B382-*`).

---

## 383.1 — The one-line thesis

**Every one of the four native binaries carried a load-bearing security fact that its strings/RTTI-grade
block could not state — and decompilation surfaced it.** The native platform layer is not more *fragile*
than the earlier blocks implied; it is more *specified* — the exact bypass flags, privilege account,
key-derivation, and integrity model live in the instruction stream, not the string table. `[INFER]`

---

## 383.2 — What the decompiler added, per binary `[CERT]`

| Binary | Strings/RTTI grade said (prior block) | Decompilation added (this sub-pass) |
|---|---|---|
| **`nverify.exe`** [B379←B126 §126.4] | "`--unsigned */--removed *` are the escape hatches" (5 options) | **11 options incl. FOUR `skip-*` bypasses**, `--skip-signature-check` = total bypass; the **Tridium Public Key is a 270-byte RSA-2048 blob pinned by raw `memcmp`** (`DAT_140072ec0`), bypassable by `--skip-tpk-check`; exact Mocana `rel.albatross.6.5.2.u7.hf23` |
| **`njre.dll`** [B380←B125 §125.2] | `buildArgs`/`createVM`/`invokeJava` (B125 already decompiled these) | the FOUR functions B125 skipped: `java()` (Win10 gate + `isProductionBuild` banner), `initPaths` (**FIPS-license-gated `bcstd`/`bcfips` provider swap**, `NIAGARA_JRE_HOME`, classpath+`exit(0xf9)`), `loadDLL` (**GetProcAddress `JNI_CreateJavaVM`**, upgrades B125's INFER→CERT), `buildVMOptions` (`-Xmx48M`); the **`-javaagent` license gate** decompiled in full |
| **`plat.exe`** [B381←B129 §129.3] | commands + SCM/DPAPI APIs + `systempw` key path (all from strings) | the daemon runs as **LocalSystem + `SERVICE_AUTO_START`**; the System Passphrase is **passed on argv**, gated by a **native complexity policy (≥10, upper+lower+digit)**, DPAPI-sealed with **no app entropy**, stored **`REG_BINARY` under `HKLM\SOFTWARE\Niagara4`** |
| **`libciper.so`** [B382←B126 §126.5] | 257 JNI symbols, "CRC-only, `crypto false`" | the QNX-ARM **Sylk masterslave file-transfer protocol**: 496-byte records, ≤485-byte blocks with sector/block numbering, **dual integrity CRC-16-CCITT (message) + CRC-32 (file)**, synchronous `requestOneMessage`, **unauthenticated/unencrypted**, stack-protected |

Every "added" cell is `[CERT]` in its source block; none re-derives the prior block — see §383.4. `[CERT]`

---

## 383.3 — Three cross-binary threads `[CERT]/[INFER]`

1. **License features gate the native layer at three points** `[CERT]`: `nverify` pins the Tridium RSA-2048
   key (`--skip-tpk-check` off it, B379); `njre` selects the crypto provider by the `fips140-2` feature and
   blocks `-javaagent` without the `developer` feature (B380); `plat` writes the DPAPI System Passphrase that
   seeds keyring encryption (B381). The same `LicenseUtil::isFeaturePresent` decompiled in [B126] is the
   common gate — the native layer is **license-aware well below the Java `LicenseManager`**. `[INFER]` (one
   mechanism, three call sites).
2. **Everything privileged is dynamically resolved, so the import table lies** `[CERT]`: `nverify`/`njre`
   reach `JNI_CreateJavaVM` and `plat` reaches every SCM/DPAPI/registry API through
   `LoadLibrary`+`GetProcAddress` (B380 §380.3, B381 §381.1). A monitor keying on the static import table
   sees none of it — a recurring finding first inferred in B124/B129 and now `[CERT]` in the resolver bodies.
3. **Integrity ≠ authentication, twice** `[CERT]`: `nverify`'s TPK pin is a `memcmp` (identity, not a fresh
   signature check — B379), and `libciper`'s file transfer is CRC-16+CRC-32 with no cipher (B382). Both
   detect the wrong bytes; neither, by itself, stops an authorized-position adversary (a wrapper passing
   `--skip-*`, or a device on the Sylk bus). `[INFER]`

---

## 383.4 — The method that kept it honest (PRIOR-COVERAGE → REMIT → DEEPEN) `[CERT]`

The sub-pass's premise — "these were only read at strings grade" — was itself a hypothesis, and it was
**wrong twice**, caught by the PROMPT-LOOP PRIOR-COVERAGE CHECK before any re-derivation:
- **B380**: B125 §125.2 had ALREADY decompiled `buildArgs`/`createVM`/`invokeJava` at Ghidra grade →
  REMITTED to B125 (§14), block re-scoped to the four functions B125 skipped.
- **B381**: B129 §129.7 had explicitly judged decompilation "not load-bearing" → tested that judgment,
  found it too conservative (LocalSystem/argv-passphrase/complexity-policy ARE load-bearing), REMITTED the
  command-set fact, kept only the new security facts.
In both, the discipline was: read the prior block first, remit what it covered, deepen only the genuine gap.
A tool was created to make this possible on symbol-stripped binaries: **`DecompileByString.java`** (anchors
decompilation on referenced strings, since `ExportDecompiledC` filters by symbol name — useless on
Mocana-static `nverify.exe`). `[CERT]`

---

## 383.5 — What remains (and why it is diminishing returns)

- **NG1-G1** (investigable): place the 3 untraced `nverify` `skip-*` gate sites via a call-graph trace from
  `main`. Incremental — the flags' EFFECT is already documented (B379 §379.1); this only pins their branch.
- **NG2b** (investigable, low value): decompile the ~104 remaining `nre.dll` `NativePlatformProvider` native
  bodies beyond B125's 3 samples. B125 documented the JNI-binding PATTERN and 3 representative natives; the
  rest are largely repetitive JNI shims — bulk, not depth. Carries the `nre.dll`-isolation-import gotcha.
- **Dynamic (requires-execution, unchanged)**: N6 live 3011/5011 framing, N4 live field-bus, N5 live
  alarm-UI — need a running daemon / field hardware / GUI session.

The four high-value native binaries are now at body grade; NG1-G1/NG2b are optional detail. `[CERT]`

---

## 383.6 — Self-verify

This is a SYNTHESIS block; its claims are traceable to the four evidence blocks, not to fresh tool output.
Spot re-checks against the source blocks:
1. B379: 4 `skip-*` + 270-byte TPK `memcmp` pin — ✓ ([B379] §379.1/§379.4).
2. B380: FIPS-gated `bcstd`/`bcfips` + `-javaagent` `developer`-gate + `loadDLL` GetProcAddress — ✓ ([B380] §380.2/§380.3/§380.5).
3. B381: LocalSystem + auto-start + argv passphrase + complexity policy + `REG_BINARY`/HKLM + DPAPI-no-entropy — ✓ ([B381] §381.2/§381.3).
4. B382: 496B record, ≤485B blocks, CRC-16-CCITT + CRC-32, `crypto false` — ✓ ([B382] §382.2/§382.3/§382.4).
5. Method: B380 remits to B125, B381 remits to B129 (§14) — ✓ ([B380] §380.x, [B381] §381.x).

**Marker tally**: `[CERT]` ≈ 14 · `[INFER]` 4 (the thesis, the three cross-binary threads' generalizations).
Ratio ≈ 0.29 — EXPECTED and healthy for a SYNTHESIS block (§11): the [INFER]s are cross-block generalizations,
not unbacked claims; each rests on `[CERT]` evidence in B379–B382.

---

## 383.x — Connections

- **[B379]/[B380]/[B381]/[B382]** — the four evidence blocks this synthesizes.
- **[B126]** — corrected/extended by B379 (§126.4) and B382 (§126.5); its `LicenseUtil::isFeaturePresent`
  is the common license gate of §383.3 thread 1.
- **[B129]** — refined by B381 (§129.3); its "decompilation not load-bearing" judgment is the §383.4 lesson.
- **[B125]** — the sub-pass's REMITTANCE anchor: B380 completes its §125.2 launcher trace without re-deriving it.
- **[B124]** — the boot path all four binaries sit on.
- **Terminal**: focus high-value targets closed at body grade; §18 self-retrospective follows; NG1-G1/NG2b
  remain investigable but diminishing-returns.
