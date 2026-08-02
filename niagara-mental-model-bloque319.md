# Block 319 — Native licensing gate verified with Ghidra on the real build + LIVE bypass of the `-javaagent` gate (L-11)

> **Dynamic-phase block (METHODOLOGY §12 + document-mode capture)** — re-verified the native licensing layer
> of the ACTUAL lab build (`iC-Niagara-4.10.9.14`, OEM iSMA CONTROLLI) against the corpus findings
> (B124/B125/B126 were built on a DIFFERENT build), using Ghidra headless, and then **proved live** that the
> native `-javaagent` gate is bypassable with a signature-less license file. This is a new security finding
> for the OEM report (test **L-11**), plus a build-delta note vs B126.
>
> **⚠ CONFIG MUTATION** — one rung-2 reversible write (planted `javaagent-developer.license`), executed with
> the backup→oracle→restore protocol of B318; end state verified pristine (sha256/PIDs unchanged).
> **SECRETS DISCIPLINE**: no target secret values; the attacker DSA keypair remains the pentest's own
> disposable test key. Decompilation is of the OEM's own shipped binaries (structure only).
>
> Sources: Ghidra 12.1.2 headless decompilation of `bin/nre.dll` (sha256
> `ba9f71c49d64f5ea608d2ff35dbf3c4aee9a0fc700aff019ba564b3fb87d4f4a`) preserved under
> `corpus/sources/probes/B317-pentest-2026-08-01/native/ghidra-nre-decomp.txt` + `ghidra-nre-callers.txt`
> `[CERT]`; live launcher behavior `[CERT-live]`; corpus B124/B125/B126 as the comparative baseline `[CERT]`.
> Markers: `[CERT]` decompiled/artifact · `[CERT-live]` measured live · `[INFER]` deduction.

---

## 319.1 — Toolchain: Ghidra headless on this host (reusable note) `[CERT]`

Ghidra 12.1.2 (linuxbrew, `/home/linuxbrew/.linuxbrew/opt/ghidra`) headless fails with
`Unable to prompt user for JDK path, no TTY detected` unless three environment pieces are set:

```bash
export JAVA_HOME=/home/linuxbrew/.linuxbrew/opt/openjdk@21
export JAVA_TOOL_OPTIONS="-Duser.home=<persistent-workdir>/ghidra-home -Djava.io.tmpdir=<persistent-workdir>/tmp"
# ghidra-home must contain .config/ghidra/ghidra_12.1.2_PUBLIC (pre-created) so LaunchSupport can
# persist java_home.save; a /tmp user.home is wiped between commands and the JDK prompt recurs.
analyzeHeadless <proj> <name> -import <bin> -overwrite [-scriptPath <dir> -postScript X.java]
```

Script gotchas hit live: Ghidra 12 API has no `GhidraScript.getSymbol(String,null)` / no `decompile(Function)`
— use `symbolTable.getAllSymbols(true)` + `functionManager.getFunctionAt(addr)` +
`DecompInterface.decompileFunction(f, timeout, monitor)`; script dir path must not break OSGi (keep it
dot-free).

## 319.2 — Build delta vs corpus (why re-verification was necessary) `[CERT]`

| Binary | Corpus build (B125/B126) | This build (iC-Niagara-4.10.9.14) |
|---|---|---|
| `nre.dll` | sha256 `606ff1c6…` | sha256 `ba9f71c49d64f5ea608d2ff35dbf3c4aee9a0fc700aff019ba564b3fb87d4f4a` |
| `njre.dll` | sha256 `7007ff82…` | sha256 `803f9db56249a803fdf2287d4866e5c07fc715bd281f7e0b3bedd2a27d38a06d` |
| `isFeaturePresent` | formats TWO needles internally (`<license vendor="%s"`, `<feature name="%s"`) — B126 §126.6 | receives the **plain feature name** as `arg2`; single `strstr(fileContent, arg2)` |
| Needle strings | present at `0x18000ee98`/`0x18000eeb0` | **absent** — only `"%s\security\licenses"` and `".license"` exist in this DLL |

So the text-match semantics of B126 §126.6 **hold**, but the implementation differs: the feature name is
matched as a bare literal anywhere in the file (even `developer` alone would match). The security
consequence is the same and now **stronger**: no XML-shape requirement at all.

## 319.3 — Ghidra decompilation of the real build `[CERT]`

**`LicenseUtil::isFeaturePresent` @ `0x180004ac0`** (see `ghidra-nre-decomp.txt`):
1. `snprintf(path, "%s\\security\\licenses", arg1)` — arg1 is the install home (`this+0xc` at call sites);
2. `DirectoryListing::make(path)` → iterate entries, skip `.`/`..`, filter `strcmp(ext, ".license") == 0`;
3. for each file: `fopen`/`fseek`/`ftell`/`fread` whole content; **`strstr(content, arg2)`** → true on first hit;
4. **no call into any DSA/RSA verifier** — no signature check in this function (same conclusion as B126).

**`NreLauncherWin32::createVM` @ `0x1800066e3`** (see `ghidra-nre-callers.txt`):
```c
for each javaOption:
    if strstr(opt, "javaagent") || strstr(opt, "agentpath"):
        if (!LicenseUtil::isFeaturePresent(home, "developer"))
            fprintf(stderr, "FATAL: Can not use Java agent argument '%s' without a '%s' feature in license.\n",
                    opt, "developer");
            return -1;
```
`initFips` @ `0x1800069db` similarly gates on `isFeaturePresent(home, "fips140-2")`.

## 319.4 — LIVE bypass test (L-11) `[CERT-live]`

Artifact planted: `javaagent-developer.license` — a `vendor="Tridium"` license containing
`<feature name="developer">` with an **EMPTY `<signature></signature>`** (the signing step in the generator
silently failed; the file shipped without any signature — which only strengthens the result). Launcher
invocation: `nre '-@-javaagent:whatever' -version`.

| State | Launcher output | Verdict |
|---|---|---|
| Baseline (no license file) | `FATAL: Can not use Java agent argument '--javaagent:whatever' without a 'developer' feature in license.` | gate CLOSED — VM refused |
| **After planting the signature-less file** | **no FATAL** → proceeds to JVM (`Unrecognized option: --javaagent:whatever` / `CreateJavaVM failed -1`, i.e. failed only because the test option is not a real JVM option) | **gate BYPASSED** |
| Java-layer verdict on the same file (`nre -licenses`) | `javaagent-developer.license {invalid: Invalid XML: Missing signature element [line 1]}` | Java layer REJECTS it |

**Finding L-11 (CONFIRMED):** the native `-javaagent`/`-agentpath` gate is a **signature-less text-match on
the literal feature name** in any `.license` file under `security\licenses\`. A file with **no valid
signature at all** satisfies it. Combined with **L-6** (`Authenticated Users` have `Modify` on
`security\licenses`), a **standard user can plant such a file and enable JVM agents** (`-javaagent` /
`-agentpath`, i.e. arbitrary instrumentation code in the launcher JVM) **without any valid license**.
The Java `LicenseManager` remains the authority for features, but the native fast-path — the one the
launcher trusts for the agent gate — is evadable. This is the live, exploitable form of the B126 §126.6
layer-divergence note.

Scope note (honest): the attacker still needs to influence the launcher's command line
(`-@-javaagent:...`) — this is not a remote RCE by itself; it removes the LICENSE barrier that the
`developer` feature was supposed to impose on agent loading, for any local user who can write the licenses
directory. For the OEM: the ACL fix (B316 rec. 1) also closes this path.

## 319.5 — Evidence preservation

- `sources/probes/B317-pentest-2026-08-01/native/bin/{nre,njre,dsfspi,niagarad,plat}.{dll,exe}` — pulled
  binaries (sha256 recorded in §319.2 and file names).
- `native/ghidra-nre-decomp.txt`, `native/ghidra-nre-callers.txt` — decompilation outputs + analysis notes.
- `native/DecompileLicense.java`, `native/DecompileCallers.java` — the Ghidra scripts.
- `native/javaagent-gate{,-test,-2,-3}.ps1`, `native/post-ja-verify.ps1` — live test + cleanup transcripts.
- `forge/javaagent-developer.license`, `forge/make-javaagent-lic.py` — the planted artifact + generator.
- End state re-verified: `certDir=Tridium.certificate`, `licTree=\db,\inbox`, tcert sha256
  `9E1D3F6D9E66DE4020171FA9D3DFA66F0B75036DDA5B1732A49F7973A4965211`, `pentest-dirs=0`,
  PIDs `niagarad=2556`/`sshd=11144` (unchanged).

## 319.6 — Self-verify

- `verify-block.sh niagara-mental-model-bloque319.md` — exit 0 (verified above).
- Marker tally (whole block, incl. legend): `[CERT-live]` 4 · `[CERT]` 7 · `[INFER]` 2 (legend + §319.4 scope note phrasing; no load-bearing inference). Load-bearing tokens re-verified: decompiled bodies in `ghidra-nre-decomp.txt`/`ghidra-nre-callers.txt` (grep for `isFeaturePresent`, `strstr`, `developer`, `FATAL`), live outputs in `recon-2026-08-01.txt` and the gate-test transcripts, sha256 of `nre.dll` recomputed (matches `ba9f71c4…`).
- RE-MEASURE rule: binaries pulled and hashed live this session; corpus hashes were NOT inherited.
