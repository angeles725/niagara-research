<!-- kit-retro: jace8000-qnx-native focus · 2026-08-30 · scope: METHODOLOGY §5 (symbol-bearing native RE vs twin-binary offset check) + §21.2 native chain + §16 (sibling/twin focus) + §6 (entropy+binwalk encrypted discriminator) -->
<!-- review-status: pending -->

# §18 Self-Retrospective — focus: jace8000-qnx-native (2026-08-30)

**Corpus:** niagara-research · **Focus:** jace8000-qnx-native (B677–B684, 8 blocks)
**Run:** 2026-08-30 · blocks B677–B684, gaps 8/8 investigable closed (QN1–QN8), 1 blocked-live (QN6-G1)
**Retro agent:** §18 fresh-context self-retrospective (propose-only; no kit edits)

The ARM/QNX twin of the Windows `platform-native` focus: 13 ELF32 ARM binaries extracted read-only from
the JACE-8000 microSD (`local-sd-image/bin-arm/`, gitignored), analysed statically. The distinctive
run-facts: (1) the ARM ELFs **retained full symbol tables** (unlike the stripped Windows DLLs), so the whole
focus closed with `readelf`/`nm`/`strings` symbol+string inventory and Ghidra was gated but essentially not
needed for function bodies; (2) the focus was framed and driven as a per-artifact **twin** of an existing
focus; (3) QN6/B682 closed the OS-image gap with an entropy+binwalk encryption verdict.

---

## Kit files deduped against

- `research-sdd/METHODOLOGY.md` (2328 lines — §3 markers incl. line 68 symbol→[CERT] promotion, §5 preservation incl. twin-binary offset-provenance lines 321–366 + obfuscated/renamed-symbol rules, §6 tools incl. entropy test lines 528–538 + protocol-reconstruction line 458, §10 self-provisioning incl. UPDATE-IN-USE line 969, §16 multi-focus lines 1816–1859, §19 build/PoC, §21 wall/fallback-chain lines 2220–2285).
- `research-sdd/PROMPT-LOOP.md` (876 lines — HOT-core; REMITTANCE at BOOTSTRAP e, PRIOR-COVERAGE-CHECK / SCOPING-JUDGMENTS-ARE-HYPOTHESES at NORMAL CYCLE step 3, MODEL TIER).
- `research-sdd/toolbelt/tool-registry.md` (native ELF/PE triage row: `file`+`readelf`+`strings`; `radare2`/`objdump`/`nm`/`strings` in PATH; `decompile-native.sh quick`).
- Fleet grep: `twin focus|sibling focus|mirror focus|gemelo|cross-platform|per-artifact contrast` → no cross-platform focus-topology pattern (only unrelated `sibling` uses + `twin-binary` which is same-source/two-binaries); `not stripped|retain.*symbol|symbol-bearing|symbol inventory` → no native-RE branch on symbol-bearing vs stripped ELFs.
- Prior sibling retro: `retros/2026-08-30-jace8000-sd-focus-retro.md` (the physical-media focus that produced the ARM binaries; its D-note already recorded `qnx6read.py` extract-mode as §10 UPDATE-IN-USE).

---

## Summary of proposed deltas

| # | Title | Priority | Type | Kit target |
|---|---|---|---|---|
| D1 | Symbol-bearing native ELF: the `nm`/`readelf`/`strings` symbol+string inventory IS `[CERT]` identity evidence and is EXEMPT from the §5 twin-binary offset check (no offset to verify); Ghidra body-decompilation is scoped to what symbols/strings can't answer | MEDIUM | PROMOTE | METHODOLOGY.md §5 (after twin-binary offset block) + §21.2 native-chain note |
| D2 | Sibling / twin focus: open a focus that deliberately MIRRORS an existing focus for a different platform/architecture of the SAME subject — seed the backlog from the sibling's artifact inventory, drive each block as a per-artifact §14 contrast, and REMITTANCE-point back to the sibling | MEDIUM | PROMOTE | METHODOLOGY.md §16 (new bullet) |
| D4 | Firmware encryption discriminator: flat ~8.0-bit entropy AND zero binwalk signatures across the WHOLE image ⇒ encrypted (not merely compressed — legit compressed firmware retains a container/header magic), recorded as a blocked child needing the live device / device-bound key; the "encrypted" verdict stays `[INFER]`, the measurement is `[CERT]` | LOW | ABSORB | METHODOLOGY.md §6 (entropy-test paragraph) |

Candidate evaluated and NOT proposed (dedupe → already covered):
- **D3 — in-run tool EXTENSION (growing an existing `tools/` tool mid-run).** ALREADY fully encoded: METHODOLOGY §10 **UPDATE-IN-USE** case (line 969 — "a tool already in `tools/` is changed mid-run (parameter added, bug fixed, threshold tuned). Record the change at the iteration where it happens"). Moreover the extension in question (`tools/qnx6read.py` gaining an extract-by-path mode) happened in the PRIOR focus (`jace8000-sd` B675) — NOT in this focus (B677–B684 consumed already-extracted ARM binaries) — and the prior retro already recorded it verbatim as "UPDATE-IN-USE case, §10". No new rule needed.

---

## D1 — Symbol-bearing native ELF: symbol/string inventory is `[CERT]`, exempt from the twin-binary offset check, Ghidra reserved

**Priority:** MEDIUM
**Type:** PROMOTE (new native-RE branch; the underlying symbol→[CERT] principle exists, the workflow + exemption do not)

### Evidence

- RESEARCH-STATE header: "13 ELF32 ARM little-endian, **CON tabla de símbolos** (no stripped, a diferencia del lado Windows)… Símbolos → RE mucho más tractable." Per-binary FUNC counts listed (libdsfspi 2115 FUNC, libcommon 1885, …).
- Iteration history: 7 of 8 blocks recorded `no · inline (readelf/nm/strings)` or `(symbol/string inventory readelf/nm)`; Ghidra 12.1.3 + r2 were in scope (`detect-tools` gate passed) but not used for function bodies.
- B677 §677.1–§677.4 (`[CERT]`): the entire DSF crypto engine identity (JNI SPI class map, Mocana NanoCrypto static link via `readelf -d NEEDED`, AES-256-CBC + NIST CTR-DRBG from `strings`, DSA/RSA keygen) is derived from `nm -D`/`readelf -d`/`strings` — no decompiled offsets.
- B677 §677.5 tally (the load-bearing methodological line): "**Symbols/strings are direct evidence (not decompiled offsets), so no twin-binary corroboration needed for identity;** parameter-level claims are deferred, not asserted." The block also correctly draws the LINE symbols can't cross: §677.4 leaves the KDF salt/iteration parameters as a decompile follow-up (QN1-G2) — "the symbols name the primitives, not the parameter values."

### Deduplication result

- §3 line 68 already says a statistical-only assignment is promoted to `[CERT]` "only by a **symbol**, a spec, or a documented anchor" — so a symbol IS `[CERT]`-grade. That is the PRINCIPLE; it is present.
- §5 twin-binary offset-provenance (lines 321–366) requires verifying a decompiler dump's OFFSETS (RVA/VMA) against the sha256-anchored binary before citing, because two twin binaries sit at different addresses. This is precisely what a symbol-NAME citation does NOT need — a symbol names the function regardless of where it sits — but §5 never says so, leaving a reviewer to demand a twin-binary check on a citation that has no offset to check.
- §5 obfuscated/renamed-symbol rules ("Never cite a renamed symbol as identity — `a.b(c)` is a position, not a name") cover the STRIPPED/obfuscated case (the Windows-DLL side); the kit has no complementary rule for the RETAINED-symbol case.
- §21.2 native chain is `ghidra → r2 → quick` with "never bare strings — TOOL-BEFORE-AGENT." Read literally this points at Ghidra-decompile-first and warns off `strings` — which under-serves a symbol-bearing ELF where a `nm`/`readelf`/`strings` INVENTORY (not "bare strings" over a stripped blob, but structured symbol-table reading) is the correct, higher-value first move. The tool-registry's "Native ELF/PE — triage" row (`file`+`readelf`+`strings`) is a first-look-before-decompile step, not a "symbols can close identity, decompile is reserved" methodology.

Genuinely new on two specific axes: (1) a symbol-bearing binary's symbol/string inventory is `[CERT]` identity evidence **EXEMPT from the §5 twin-binary offset check** (there is no offset — the exemption is the new content); (2) a native-RE workflow branch — when the ELF retains its symbol table, do symbol inventory first and RESERVE Ghidra body-decompilation for what symbols/strings cannot answer (parameter values, control flow). The symbol→[CERT] principle underpins it but neither the exemption nor the branch is stated.

### Proposed landing

**METHODOLOGY.md §5** — add after the twin-binary offset-provenance block (line ~366):

> **Symbol-bearing native binary — symbol/string inventory is `[CERT]`, and it is EXEMPT from the twin-binary offset check.** The offset-provenance check above guards a decompiler dump's ADDRESSES; it does not apply to a claim anchored to a SYMBOL NAME. When a native ELF/PE **retains its symbol table** (not stripped — check `readelf -h`/`nm`), the `nm`/`readelf -d`/`strings` inventory of exported symbols, `NEEDED` libraries, and string constants is direct `[CERT]` identity evidence: a symbol names its function regardless of where the linker placed it, so there is no offset to verify and no twin-binary corroboration is required for identity. This is the complement of the renamed/obfuscated-symbol rule above (a stripped or ProGuard-renamed symbol is a POSITION, never identity). The line symbols CANNOT cross: parameter VALUES, control flow, and constants computed at runtime are not in the symbol table — those stay `[INFER]` or become a scoped decompile follow-up (B677 §677.4 deferred the KDF salt/iteration params to a Ghidra pass). **Evidence:** jace8000-qnx-native B677 — the entire `libdsfspi.so` crypto-engine identity (JNI SPI map, static Mocana NanoCrypto, AES-256-CBC, NIST CTR-DRBG) closed from `nm -D`/`readelf -d`/`strings` with no decompiled offsets; 7 of 8 blocks closed the same way against 13 symbol-bearing ARM ELFs.

**METHODOLOGY.md §21.2** — refine the native rung so symbol-bearing binaries are not pushed straight to decompile:

> - Native ELF/PE/firmware: **if the binary retains symbols (`readelf -h`/`nm`), read the symbol+string inventory FIRST** — it is `[CERT]` identity evidence (§5) and often closes the identity/architecture question without a decompile; RESERVE `ghidra → r2` body-decompilation for what symbols/strings cannot answer (parameter values, control flow, stripped regions). For a STRIPPED binary the chain is `ghidra → r2 → quick` as before (never bare `strings` — TOOL-BEFORE-AGENT).

---

## D2 — Sibling / twin focus: mirror an existing focus onto a different platform/architecture of the same subject

**Priority:** MEDIUM
**Type:** PROMOTE (new named focus-topology pattern; §16 multi-focus + REMITTANCE + §14 are the building blocks, the composite is not named)

### Evidence

- RESEARCH-STATE header: "Es el **gemelo QNX/ARM del focus `platform-native`** (que hizo los binarios de WINDOWS, B124-130/B379-385/B424-425)." The angle is explicitly "cómo el JACE… sobre QNX… la cripto de reposo en ARM… un veredicto de seguridad consolidado."
- Backlog seeded by MIRRORING the sibling's artifact inventory: every gap names its Windows sibling — QN1 "sibling of B425 dsfspi.dll", QN2 "sibling of B124/B380", QN3 "sibling of plat.exe B381", QN7 station launcher. The 13 ARM binaries were profiled AUDIT-FIRST and mapped onto the Windows focus's structure, not hand-guessed.
- Per-artifact §14 contrasts drove the blocks: B677 = "the exact ARM twin of the Windows DSF stack ([Block 425])" with a §14 refinement (ARM adds a HW-RNG path vs Windows "timers only"); B679 issued a §14 vs B381 — "drops privileges to niagarad user, refuses root" contrasted against Windows `plat.exe` LocalSystem; B683 station de-privileged (uid300, refuses root). The cross-platform DIFFERENCE is itself the finding (B684 security verdict: strong boot/process, weak data-at-rest).
- REMITTANCE table points every already-covered subject back to its owning focus: Windows native → `platform-native`; live station → `jace8000`; SD filesystem → `jace8000-sd`; wire protocols → protocols/modbus/bacnet. This kept the twin focus from re-inflating the backlog with subjects a sibling block already answered.

### Deduplication result

- §16 (lines 1816–1859) formalises multi-focus: one RESEARCH-STATE per focus, `FOCUSES.md`, focus-aware block prefix, angle confirmation, concurrent loops. It describes INDEPENDENT parallel focuses; it does not describe a focus DERIVED from another as its cross-platform mirror.
- PROMPT-LOOP BOOTSTRAP e "PRE-DECLARE REMITTANCES" and NORMAL-CYCLE step 3 "PRIOR COVERAGE CHECK" cover pointing at / not re-doing prior coverage — the machinery this pattern USES — but neither names the twin-focus move (seed backlog by mirroring a sibling focus's artifact list; require a per-block cross-platform contrast).
- §5 "twin-binary" is a different animal (same C source compiled to two binaries, an offset-drift citation hazard); this is the same SUBJECT/SYSTEM implemented on two PLATFORMS, investigated as two focuses — a focus-topology pattern, not a citation-provenance one. Fleet grep confirms no `twin focus`/`sibling focus`/`gemelo`/`cross-platform` focus pattern exists.
- §14 (cross-block consistency) supplies the correction mechanism each contrast uses, but §14 is block-to-block within a focus; it does not prescribe cross-platform sibling contrast as a focus METHOD.

Genuinely new as a NAMED composite: a repeatable focus-open recipe = (a) seed the backlog by mirroring an existing focus's confirmed artifact inventory onto the new platform/architecture (shortcuts the AUDIT-FIRST sweep — the sibling already found the subjects); (b) drive each block as a per-artifact §14 contrast with its sibling, where the platform DIFFERENCE is a first-class finding; (c) REMITTANCE-point every non-twin subject back to its owning focus. Proven end-to-end across 8 blocks.

### Proposed landing

**METHODOLOGY.md §16** — add a bullet after the "State the active focus" bullet (line ~1834):

> - **Sibling / twin focus (mirror an existing focus onto another platform/architecture of the SAME subject).** When a subject already has a focus for one implementation (e.g. its Windows binaries) and you now hold the SAME subject on a different platform (its ARM/QNX binaries), open a TWIN focus rather than re-bootstrapping from zero: (1) **seed the backlog by mirroring the sibling focus's confirmed artifact inventory** — the sibling already discovered the subjects, so each gap opens as "sibling of [Block N]" and the AUDIT-FIRST sweep only confirms the artifacts exist on the new platform and measures them; (2) **drive each block as a per-artifact cross-platform contrast** — the DIFFERENCE from the sibling is a first-class finding, and where the twin refutes or refines a sibling block, issue a §14 correction with a back-pointer (B679 refined B381's Windows-LocalSystem finding: the QNX `niagarad` drops privileges and refuses root); (3) **REMITTANCE-point every non-twin subject** back to its owning focus (the sibling platform, live access, wire protocols) so the twin backlog is not re-inflated with already-covered subjects (PROMPT-LOOP BOOTSTRAP e). Distinct from §5's "twin-binary" (same source, two binaries — a citation-offset hazard); this is one subject on two platforms, investigated as two focuses. **Evidence:** jace8000-qnx-native (B677–B684) opened as the ARM/QNX twin of `platform-native` (Windows) — 8 blocks, each a sibling contrast (B677↔B425, B678↔B124/B380, B679↔B381, B683 station), REMITTANCE table pointing Windows/live/wire back to their focuses.

---

## D4 — Firmware encryption discriminator: flat entropy + zero binwalk signatures ⇒ encrypted (blocked child), verdict stays `[INFER]`

**Priority:** LOW
**Type:** ABSORB (one-line refinement resolving §6's own compressor-vs-ciphertext caveat)

### Evidence

- B682 §682.2 (`[CERT]` measurement + `[INFER]` encryption): payload of `n4-titan-am335x.signed` (27,149,316 B) measured at 7.997–7.998 bits/byte at 1 MB / 13 MB (header 0x0–0x400 = 5.158, structured); `binwalk` reports 0 signatures / 0 embedded files across the whole image.
- The discriminator reasoning (the new nugget): "Flat ~8.0-bit/byte entropy with no gzip/lzo/uImage/IFS magic anywhere ⇒ encrypted, or maximally compressed with a stripped header; **a normal (even compressed) QNX IFS leaves detectable structure/headers, so encryption is the strong reading** `[INFER]`." — i.e. binwalk-ZERO across the whole image is what tips flat entropy toward *encrypted* rather than *compressed*, because legit compressed firmware still carries a container/header magic binwalk would catch.
- Marker discipline: B682 §682.4 keeps "encrypted" at `[INFER]` (claims 4–5) while the entropy/binwalk measurements are `[CERT]` (claims 2–3). Closed QN6-G1 as a blocked child needing the live device or the device-bound key (`tried:` static entropy + binwalk → opaque, no offline unpack).

### Deduplication result

§6 (lines 528–538) already encodes the entropy test: "~7.99–8.0 bits/byte with a near-flat histogram ⇒ ciphertext or maximally-compressed data" and explicitly caveats "This is a first-pass check, not cipher identification: **a strong compressor is indistinguishable from ciphertext at this level.**" §21.2 firmware chain ends at "a non-extracting binwalk signature + entropy map" and notes "Encrypted inner archives are a blocker WITH an attack path (bkcrack), not a wall."

So the entropy test AND the binwalk+entropy firmware rung both exist. What is NOT stated — and is the genuinely-new nuance — is the DISCRIMINATOR that partly resolves §6's own compressor-vs-ciphertext caveat FOR A FIRMWARE IMAGE: legitimately-compressed firmware retains a container/header magic, so flat entropy + **zero binwalk signatures across the whole image** tips the reading toward encrypted rather than compressed. Plus the marker discipline (the "encrypted" verdict stays `[INFER]`; only the measurement is `[CERT]`) and the outcome (record as a blocked child needing the device-bound key). This is an application/refinement of the existing test, hence ABSORB, not a new rule — low editorial risk.

### Proposed absorption

**METHODOLOGY.md §6** — extend the entropy-test paragraph (after the "strong compressor is indistinguishable from ciphertext" caveat, line ~535):

> For a FIRMWARE image the compressor-vs-ciphertext ambiguity is partly resolvable: legitimately-compressed firmware (uImage/IFS/gzip/lzo) still carries a container/header MAGIC, so **flat ~8.0-bit entropy PLUS zero binwalk signatures across the whole image ⇒ encrypted is the strong reading** (a compressed-with-header image would surface that header to binwalk). Keep the verdict honest: the entropy/binwalk numbers are `[CERT]`, but "encrypted" remains `[INFER]` (this is a first-pass distinction, not cipher identification), and the gap is recorded as a blocked child needing the running device or the device-bound key — not a wall. **Evidence:** jace8000-qnx-native B682 — `n4-titan-am335x.signed` payload 7.997–7.998 bits/byte + binwalk 0 signatures ⇒ QNX IFS not statically extractable from the SD; QN6-G1 blocked-on-live-device / device-bound key.

---

## Tools used / acquired this focus

| Tool | Path | Case (§10) | Verdict | WHY |
|---|---|---|---|---|
| `readelf` / `nm` / `strings` / `objdump` | system (in PATH) | INSTALL (pre-existing) | keep-local | The symbol+string inventory channel for the 13 symbol-bearing ARM ELFs — this focus's primary evidence tool (D1). No wrapper needed; tool-registry already lists them. |
| Ghidra 12.1.3 / r2 | (per detect-tools cache) | INSTALL (pre-existing) | keep-local | In scope (detect-tools gate passed) and available, but reserved — the retained symbols answered identity, so body-decompilation was not needed for B677–B684 (deferred to QN1-G2 KDF-parameter follow-up). |
| entropy one-liner + `binwalk` | system / tool-registry | INSTALL (pre-existing) | keep-local (refinement → D4) | The encryption discriminator for the QNX firmware payload (B682). Reusable knowledge is the D4 discriminator, not a script. |
| `tools/qnx6read.py` | `niagara-research/tools/qnx6read.py` | (not touched this focus) | — | The ARM binaries were already extracted in the prior focus (jace8000-sd B675, where qnx6read gained its extract mode — §10 UPDATE-IN-USE, recorded in that focus's retro). Not modified here. |

No `install-tool.sh` recipe was needed; no external tool was downloaded.

## FOCUSES.md / TARGETS.md status

- `jace8000-qnx-native` is a NEW focus bootstrapped 2026-08-30, the ARM/QNX twin of `platform-native` (D2). Confirm it is registered in `FOCUSES.md` (active → stopped) and refresh the niagara-research row in the kit's `TARGETS.md` to reflect the 8 new blocks (B677–B684) at focus-commit time (living-mirror rule).
- `block_scope: shared-global` is correctly declared in RESEARCH-STATE-jace8000-qnx-native.md (the corpus shares one global `niagara-mental-model-bloqueN` prefix).
- STOP is honored correctly: 8/8 investigable closed, 1 blocked-live (QN6-G1, encrypted payload) with a populated `tried:` clause.

## Proposed kit delta verdict (§18 propose-never-apply)

All deltas above are PROPOSALS for the kit maintainer's review. This file edits no kit file. The reviewer should:
1. Accept / reject / modify each delta.
2. D1 (MED) and D2 (MED) are the highest value — both name real gaps: the kit has a symbol→[CERT] principle but no native-RE branch for symbol-bearing ELFs (and no twin-binary-check exemption for symbol citations), and it has multi-focus + REMITTANCE but no named cross-platform sibling-focus pattern.
3. D4 (LOW) is a one-line refinement resolving §6's own compressor-vs-ciphertext caveat for firmware — low editorial risk; the marker use (measurement `[CERT]`, "encrypted" `[INFER]`) was already correct in B682.
4. D3 was evaluated and REJECTED as already-covered (§10 UPDATE-IN-USE; and the extension actually occurred in the prior jace8000-sd focus, already retro'd).
