# Block 424 — NreWin32::getHostId: the Niagara Host ID is a non-cryptographic 8-byte XOR fold of four host inputs

> Research of the **native Host ID computation** in Niagara N4's runtime launcher (`njre.dll`,
> class `NreWin32`): the actual body of `getHostId` — the four host inputs it collects, the byte
> accumulator that folds them into 8 bytes, the `Win-XXXX-XXXX-XXXX-XXXX` rendering, the vendor tag,
> and the failure/gate behavior. This is the identity a Niagara license binds to. It does NOT cover the
> Java side of licensing (`LicenseManager` DSA verification — see [Block 126], [Block 395]) nor the
> license feature-string match (`LicenseUtil::isFeaturePresent` — [Block 126] §126.6).
>
> Subject version: OptimizerSupervisor N4.14.0.162 (Honeywell OEM) — `njre.dll`
> sha256 `7007ff82e807604071a16c1e349e515beb44502a382122702d801879b79d628b`.
>
> Sources: `/mnt/c/Honeywell/OptimizerSupervisor-N4.14.0.162/bin/njre.dll` · preserved decompilation +
> disassembly in `audits/B424-njre-hostid.txt` (Part A Ghidra bodies · Part B r2 fold-loop disasm ·
> Part C r2 xref map). Method: Ghidra headless (bodies) + radare2 (live disasm/xref of the varargs the
> decompiler dropped). Markers: `[CERT]` observed in the disassembled/decompiled body (address cited) ·
> `[INFER]` deduction.
>
> Native platform layer (Capa 25). Connects [Block 124] (host-id string/import inference — this block
> UPGRADES it to CERT and CORRECTS its scope), [Block 125] (the `getHostId0` JNI shim that calls this),
> [Block 126] (licensing/crypto), [Block 380] (njre launcher — same binary), [Block 385] (nre natives).

---

## 424.1 — Ground truth: the body lives in `njre.dll`, and the twin binary caveat `[CERT]`

The readable body was decompiled by Ghidra (`audits/B424-njre-hostid.txt` Part A, lines 9–136) but the
Ghidra image's secondary function offsets (`getHostVendor@0x5090`, `getVolume@0x5fa0`) do NOT match the
shipped `njre.dll` — that layout belongs to the TWIN binary (`nre.dll` also carries all 7 host-id strings).
Identity is therefore anchored to `njre.dll` (sha256 above) with offsets re-verified live in radare2: `[CERT]`

| Function | njre.dll VMA | Verified by |
|---|---|---|
| `NreWin32::getHostId` | `0x180004a70` | debug prints `>>> hostid.debug >>>` + call sequence, Part C |
| `getVolume` (→ `GetVolumeInformationA`) | call @ `0x180006585` | Part C xref to KERNEL32 import |
| `getHostVendor` (→ `"tridium"`) | `lea` @ `0x180005517` | Part C xref |
| `getRegWinCurVerImpl` (→ registry) | `lea` @ `0x180006383` | Part C xref to `SOFTWARE\Microsoft\%s\CurrentVersion` |
| `getOrCreateHiddenKey` / `…CachedProductIdKey` | `lea` @ `0x180006094` / `0x180005f3c` | Part C xrefs to `disableHostIdGeneration` |

The same `getHostId` logic is compiled into BOTH `njre.dll` (the JVM launcher) and `nre.dll` (the runtime
DLL) — each binary carries the 7 host-id strings independently. `[CERT]` (both binaries string-match; the
Ghidra dump is one image, the r2 evidence is `njre.dll`.)

## 424.2 — The four host inputs `getHostId` collects `[CERT]`

Before folding, `getHostId` fills four buffers, each from a different source
(`audits/B424-njre-hostid.txt` Part A lines 37–63 + Part C xrefs):

| # | Input | How it is obtained | Source detail | Citation |
|---|---|---|---|---|
| 1 | **Hidden key (`lk`)** | `getOrCreateHiddenKey` — migrate, read, else generate + save a hidden local key file | fail → `exit(0xf9)` | `audits/B424-njre-hostid.txt:425` |
| 2 | **Registered owner** | `getRegWinCurVerImpl("Windows NT","RegisteredOwner")`, fallback `"Windows"` | `RegOpenKeyExA`/`RegQueryValueExA` on `HKLM\SOFTWARE\Microsoft\%s\CurrentVersion` | `audits/B424-njre-hostid.txt:561` |
| 3 | **Product ID** | `getOrCreateCachedProductIdKey` — read cached, else generate + save | fail → `exit(0xf9)` | `audits/B424-njre-hostid.txt:311` |
| 4 | **Volume serial** | `getVolume` → `GetVolumeInformationA("c:\\", …)`, takes the C: volume serial number | formatted with `%08X`-style into the buffer | `audits/B424-njre-hostid.txt:682` |

This **corrects [Block 124]**, which inferred the Host ID was "GetVolumeInformation-derived": the C: volume
serial is only ONE of four inputs. `[CERT]` (the other three — a generated hidden key, the registry
RegisteredOwner, and a generated product id — were not visible at string/import grade in B124). `[INFER]`
the hidden key + product id being locally *generated-and-cached* means the Host ID is stable across reboots
but re-derivable only while those files survive (their loss triggers regeneration, gated by §424.5).

## 424.3 — The fold: an 8-byte non-cryptographic XOR/shift accumulator `[CERT]`

The Ghidra decompiler dropped the variadic arguments to the `snprintf`-family call, so the actual
combination was recovered by live disassembly (`audits/B424-njre-hostid.txt` Part B, loop @ `0x180005090`).
Each byte of each of the four buffers is folded into an 8-byte state through this per-byte loop body: `[CERT]`

```
movzx eax, byte [rbp+rsi+0x180]   ; al = next input byte
xor dl, al                        ; state byte ^= al   (repeated across the 8 state regs:
xor bl, al                        ;   dl, bl, r11b, r10b, r9b, r8b, dil)
... (8 XOR-into-state ops) ...
shr cl, 1                         ; 1-bit right shift of the feedback byte
xor cl, dl                        ; feedback: cl ^= dl
inc rsi; cmp rsi,r15; jl 0x5090   ; over every byte of the buffer, then repeat for the next buffer
```

It is an 8-stage register shift-chain (`ecx←al, edi←cl, r8←dil, …`) where every input byte is XORed into all
eight state bytes and a `shr`/`xor` feedback term mixes the low lane — a home-grown LFSR-style rolling
checksum. `[CERT]` (`audits/B424-njre-hostid.txt:750` movzx, `:753` `xor dl,al`, `:766` `shr cl,1`, `:770`
loop-back). It is **NOT a cryptographic hash**: no SHA/MD5/HMAC, no
call into a crypto library — pure XOR + 1-bit shift over a 64-bit state. `[CERT]` The eight resulting bytes
are the Host ID payload.

## 424.4 — Rendering: `<vendor-prefix>-XXXX-XXXX-XXXX-XXXX` and the hardcoded vendor `[CERT]`

The 8 fold bytes are formatted as `%s-%02X%02X-%02X%02X-%02X%02X-%02X%02X`
(`njre.dll` `lea r9` @ `0x180005250`; Part A lines 124–126) — a static string prefix followed by the 8
bytes in four hex groups, i.e. `Win-XXXX-XXXX-XXXX-XXXX` in the form [Block 124] observed on disk. `[CERT]`

`NreWin32::getHostVendor` returns the hardcoded literal **`"tridium"`** (`lea r9, str.tridium` @
`0x180005517`; Part A 285–299) — even on this **Honeywell OEM** build the native host vendor is `tridium`,
not Honeywell. `[CERT]` (`getNiagaraUserHome` also embeds `"tridium"` in the user-home path — Part C
`0x5c37`/`0x5c58` — a minor corroboration that the native layer is vendor-neutral Tridium code.)

## 424.5 — Failure modes and the `disableHostIdGeneration` gate `[CERT]`

The generate-if-missing branches (`getOrCreateHiddenKey`, `getOrCreateCachedProductIdKey`) both consult a
platform property `disableHostIdGeneration` (default `"false"`, `lea rdx` @ `0x180006094` / `0x180005f3c`;
Part A lines 334/459). `[CERT]` When generation is disabled and the key is absent, the code prints
`ERROR: Host Id cannot be found/generated.` and calls **`exit(0xf9)`** (= exit 249). `[CERT]` Every
unrecoverable step in the chain (migrate/generate/save the hidden key or product id) terminates the process
with the same `exit(0xf9)` (Part A 337/346/353/444/462/471/478). `[INFER]` this is a hard fail-stop, not a
degraded mode: without a computable Host ID the runtime cannot license-check, so it aborts rather than run
unbound.

## 424.6 — Defensive-security reading `[CERT]`/`[INFER]`

1. **The license-binding identity is not cryptographically bound** `[CERT]` (§424.3): the Host ID is a
   64-bit XOR/shift fold, not a signed or hashed value. The *license file* over it is DSA-signed
   ([Block 126] §126.6, [Block 395]), but the identity input itself carries no integrity primitive — it is,
   in principle, forgeable/collidable at the fold level. `[INFER]` an attacker who controls the four inputs
   (volume serial via a cloned disk image, RegisteredOwner via registry, and the two generated key files)
   can reproduce a target Host ID; the barrier is obtaining those four values, not breaking crypto.
2. **Two of four inputs are attacker-writable local state** `[CERT]` (§424.2): the hidden key and product id
   are generated-and-saved *by this code*, so they live in files an admin can copy between machines —
   consistent with the corpus theme that Niagara protects "who may run" cryptographically but binds identity
   to soft host facts.
3. **`disableHostIdGeneration` is a supply-chain knob** `[INFER]` (§424.5): a platform that ships with
   generation disabled and pre-seeded key files pins the Host ID to a provisioning image — useful for
   cloned appliances, and a place where a fixed Host ID could be baked in.
4. **Vendor is `tridium` at the native layer regardless of OEM** `[CERT]` (§424.4): the Honeywell rebrand is
   a Java/packaging concern; the host identity and vendor tag are Tridium-native. Aligns with [Block 392]'s
   finding that OEM re-signing sits above a shared Tridium native base.

## 424.7 — Self-verify

| # | Claim | Marker | Source |
|---|---|---|---|
| 1 | `getHostId` body is in `njre.dll` @ `0x180004a70` (twin copy in `nre.dll`) | `[CERT]` | `audits/B424-njre-hostid.txt:9` |
| 2 | Four inputs: hidden key, RegisteredOwner (registry), product id, C: volume serial | `[CERT]` | `audits/B424-njre-hostid.txt:37` |
| 3 | Volume serial via `GetVolumeInformationA("c:\\")` — one of four, correcting B124 | `[CERT]` | `audits/B424-njre-hostid.txt:682` |
| 4 | 8-byte fold = per-byte XOR-into-8-state + `shr`/`xor` feedback, no crypto primitive | `[CERT]` | `audits/B424-njre-hostid.txt:750` |
| 5 | Format `%s-%02X%02X-%02X%02X-%02X%02X-%02X%02X` @ `0x180005250` | `[CERT]` | `audits/B424-njre-hostid.txt:124` |
| 6 | `getHostVendor` returns hardcoded `"tridium"` on Honeywell OEM | `[CERT]` | `audits/B424-njre-hostid.txt:285` |
| 7 | `disableHostIdGeneration` gate; unrecoverable → `exit(0xf9)` | `[CERT]` | `audits/B424-njre-hostid.txt:334` |
| 8 | Identity input carries no integrity primitive → forgeable at fold level | `[INFER]` | §424.6 (from claim 4) |

**Marker tally**: `[CERT]` ≈ 20 · `[INFER]` 5 ([INFER]/[CERT] ≈ 0.25). Type: **EVIDENCE block**
(decompilation/disassembly) — ratio is healthy, this gap's investigable static evidence is now captured at
body grade. Token-checked load-bearing strings against `njre.dll`: `RegisteredOwner`, `GetVolumeInformationA`,
`%s-%02X%02X-…`, `disableHostIdGeneration`, `tridium`, `SOFTWARE\Microsoft\%s\CurrentVersion` — 6/6 present
(izz/xref, Part C). Fold ops re-verified live in r2 (Part B), independent of the Ghidra dump.

## 424.8 — Connections

- **[Block 124]** — UPGRADED to `[CERT]` and CORRECTED: volume serial is 1 of 4 inputs, not the whole
  derivation; the `Win-` prefix is a static format prefix, the payload is the 8-byte fold.
- **[Block 125]** — `getHostId0` is the JNI shim (`Nre::getHostId(buf,0x40)` → `NewStringUTF`) that calls
  THIS native body; B125 saw the shim, this block opens the computation.
- **[Block 126]** / **[Block 395]** — the license *file* over this Host ID is DSA-signed by hidden embedded
  roots; §424.6 contrasts the signed wrapper with the unsigned identity input.
- **[Block 380]** — same `njre.dll`; B380 opened the JVM-launch path, this opens the host-identity path in
  the same binary.

<!-- research-block: platform-native focus, gap NG5 (getHostId computation body) — CLOSED at body grade -->
