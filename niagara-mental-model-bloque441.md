# Block 441 — SP-G9 settled: BouncyCastle is registered **statically** at `provider.1/.2` ahead of Sun via an **overriding** `bin/policy/java.security` (`==` full override), not a runtime `insertProviderAt(1)`/`addProvider()` — the FIPS provider is primary, but approved-only strict mode is **off**. Corrects [B440].

> **Focus:** `signing-pki` · **Gap:** SP-G9 (opened by [B440] §440.7) · **Iteration:** static binary/code exploration.
>
> **Question (as posed):** does the daemon boot register BouncyCastle via `insertProviderAt(1)` (BC ahead of
> Sun → BC-FIPS approved-only precondition met) or via a trailing `addProvider()` (Sun-first, BC merely
> available)? This decides whether "FIPS on unless licensed off" is **enforced** or merely **shipped**.
>
> **Sources:**
> - `bin/njre.dll` Ghidra decompile (`tools/ghidra-scripts/decomp-njre-bystring.txt`, sha `725180bd…`) —
>   the JVM launcher's argv construction. `[CERT]`
> - `Honeywell/OptimizerSupervisor-N4.14.0.162/bin/policy/java.security` (sha `f5d4f882…`) — the **effective**
>   master security properties file. `[CERT]`
> - `jre/lib/security/java.security` (same install) — the **stock** JRE master, documenting `=` vs `==`. `[CERT-doc]`
> - Prior blocks: [B440] (provider reconciliation — the block this corrects), [B26] (raw `==` + provider
>   ordering, already on record), [B30] (FIPS *migration* workflow), [B17] (boot provider registration),
>   [B380] (njre bcstd↔bcfips license swap), [B425] (dsfspi/Mocana).
>
> **Scope:** the *provider registration mechanism and ordering* only. Does NOT re-derive the bcstd/bcfips
> classpath swap ([B380]/[B440] §440.3) nor the Mocana/DSF native axis ([B425]). READ-ONLY.

---

## 441.1 — The gap posed a false dichotomy

[B440] §440.4 read `jre/lib/security/java.security`, found the ten stock Sun providers with **no
BouncyCastle**, and concluded that "Niagara providers are registered **dynamically** at boot
(`Security.addProvider()`), not via `java.security`" ([B440] claim 7, citing [B17]). From that premise it
framed SP-G9 as a binary choice between two **runtime** calls:

- `Security.insertProviderAt(bc, 1)` — BC lands at position 1, **ahead of** Sun (approved-only precondition), or
- `Security.addProvider(bc)` — BC is appended **after** Sun (Sun-first; BC merely present).

The decompiled corpus even *looked* like it could answer this by call **arity**: the Niagara-side decompilers
mangle both JDK method names to `l(...)`, but `insertProviderAt(Provider,int)` is a 2-arg call and
`addProvider(Provider)` is a 1-arg call, so the two are distinguishable even mangled.

**That whole approach is moot.** The premise is wrong: BouncyCastle is **not** registered by a runtime call
at all. It is registered **statically and declaratively**, by an `java.security` file — just not the one
[B440] read.

## 441.2 — njre overrides the master security file with a **double-equals** (`==`) argument `[CERT]`

The native launcher `njre.dll` builds the JVM argv with, verbatim from the decompile:

```
tools/ghidra-scripts/decomp-njre-bystring.txt:536
    "-Djava.security.properties==%s\\bin\\policy\\java.security"
tools/ghidra-scripts/decomp-njre-bystring.txt:324
    anchor @180003053 -> "-Djava.security.properties==%s\bin\policy\java.security"
```

`%s` is `%NIAGARA_HOME%`. The delivered argument is therefore
`-Djava.security.properties==C:\...\bin\policy\java.security` — note the **two** equals signs. The JVM the
daemon launches does **not** run on `jre/lib/security/java.security`; it runs on `bin/policy/java.security`.

## 441.3 — `==` means **full override**, per the install's own master file `[CERT-doc]`

The stock `jre/lib/security/java.security` documents the exact semantics of the flag njre uses:

```
jre/lib/security/java.security:7    #    -Djava.security.properties=<URL>
                              :9    # This properties file appends to the master security properties file.
                              :16   #    -Djava.security.properties==<URL> (2 equals),
                              :18   # then that properties file completely overrides the master security …
                              :291  security.overridePropertiesFile=true
```

- **one** `=` → the file **appends** to (supplements) the master.
- **two** `==` → the file **completely overrides** the master.
- Either form is gated by `security.overridePropertiesFile=true`, which the stock master sets (line 291).

njre uses `==`. So `bin/policy/java.security` **wholly replaces** the stock JRE file — it is a complete
1323-line master (all standard sections present: `SecureRandom` seed, `jdk.tls.disabledAlgorithms`, cert-path
constraints), not a fragment. The stock `jre/lib/security/java.security` — the file [B440] §440.4 read — is
**inert at runtime**: correct bytes, wrong (overridden) file.

## 441.4 — The effective provider order: BouncyCastle at `1`/`2`, **ahead of** Sun at `3` `[CERT]`

`bin/policy/java.security` declares, statically:

```
bin/policy/java.security:69   # For Niagara we add BouncyCastleFipsProvider and remove the
                        :70   # com.sun.net.ssl.internal.ssl.Provider
                        :72   security.provider.1=org.bouncycastle.jcajce.bcfkswrapprovider.BouncyCastleBCFKSWrapProvider
                        :73   security.provider.2=org.bouncycastle.jcajce.provider.BouncyCastleFipsProvider
                        :74   security.provider.3=sun.security.provider.Sun
                        :75   security.provider.4=sun.security.rsa.SunRsaSign
                        :76   security.provider.5=sun.security.ec.SunEC
                        :77   security.provider.6=com.sun.crypto.provider.SunJCE
                        :78   security.provider.7=sun.security.jgss.SunProvider
                        :79   security.provider.8=com.sun.security.sasl.Provider
                        :80   security.provider.9=org.jcp.xml.dsig.internal.dom.XMLDSigRI
                        :81   security.provider.10=sun.security.smartcardio.SunPCSC
```

The BC-FKS keystore-wrapper provider is `security.provider.1`; **BC-FIPS is `security.provider.2`**; the Sun
provider is `security.provider.3`. **BouncyCastle is ahead of Sun by shipped configuration.** This is the
*static/declarative equivalent* of `insertProviderAt(1)` — achieved through preference ordering in the
overriding master file, **not** a runtime `insertProviderAt`/`addProvider` call.

**SP-G9 answer:** neither runtime call. BC precedes Sun because the overriding master lists it first. The
"is BC ahead of Sun?" precondition is **YES, and enforced** by shipped config — but see §441.5 for the
part of "FIPS on" that is *not* enforced.

## 441.5 — approved-only **strict** mode is NOT enabled → FIPS provider runs in **general** mode `[CERT]`/`[INFER]`

Being `security.provider.1/2` makes BC-FIPS the **primary** provider (`[CERT]`). It does **not** by itself put
BC-FIPS into *approved-only* mode, which additionally requires the system property
`-Dorg.bouncycastle.fips.approved_only=true`. That property appears **nowhere** in the examined evidence:

- not in the njre argv construction (`decomp-njre-bystring.txt` — njre sets only the fips *dir selection*,
  §441-adjacent [B380]; the only fips token it emits is the diagnostic `java> fips = %s` at line 310), `[CERT]`
- not in `bin/policy/java.security`, `[CERT]`
- not in `bin/`, `etc/`, or `defaults/` searched for `approved_only|bouncycastle.fips`. `[CERT]` (zero hits;
  literal query `rg -rn 'approved_only|bouncycastle.fips' bin etc defaults`)

Therefore this install runs the FIPS-certified provider as its top-priority provider, but in **general
(non-approved-only) mode**: non-FIPS-approved algorithms are still reachable through it. `[INFER]` on the
runtime behaviour (from BC-FIPS documented semantics: approved-only is opt-in via that flag), `[CERT]` on the
flag's absence. njre L250 confirms the dir selection this install lands on:
`LicenseUtil::isFeaturePresent("Tridium","fips140-2")` is **absent** → `bin\ext\bcfips` on the classpath
([B380]/[B440]), so `BouncyCastleFipsProvider` at `provider.2` resolves to a class that actually exists on
the classpath — the config and the classpath are consistent. `[CERT]`

## 441.6 — §14 corrections to [B440], and reconciliation with [B26]/[B30]/[B17]

[B26] (line 120) **already had** the `-Djava.security.properties==%NIAGARA_HOME%\bin\policy\java.security`
argument and (lines 451-452) **already had** `provider.1=BouncyCastleBCFKSWrapProvider` /
`provider.2=BouncyCastleFipsProvider`. [B440] §440.4 therefore **contradicted an on-record block** without
flagging it (§14 miss). Corrections, carried into [B440] as a pointer:

| [B440] claim | Status | Correction |
|---|---|---|
| 6 — `jre/lib/security/java.security` is stock OpenJDK, 10 Sun providers, no BC/DSF/FIPS | **bytes true, conclusion void** | That file is **overridden** by `==` (§441.2-3). It is not the effective config; its stock order does not describe the running JVM. |
| 7 — Niagara providers are registered **dynamically** at boot (`addProvider`), not via `java.security` | **CORRECTED** | The **primary** BC providers are registered **statically**, via the overriding `bin/policy/java.security` (§441.4). [B17]'s `addProvider` reading may still hold for *supplementary* runtime registration, but the BC-ahead-of-Sun ordering is declarative, not a runtime insert. |
| 8 — the [B26]/[B30] `provider.1=BouncyCastleFipsProvider` files are a **hardening template, not this install's default** | **CORRECTED** | The FIPS-first ordering **is** this install's shipped default — it lives in the active `bin/policy/java.security` ([B26]). What [B30] describes on top (editing `jre/lib/security/java.security`, adding the `C:HYBRID;ENABLE{All}` provider attribute, provider self-tests, approved-only) is a *further strict-FIPS migration*, not the baseline. [B440] conflated [B30]'s migration template with [B26]'s shipped baseline. |

Net reconciliation of the three trust-adjacent readings: **[B26] = shipped baseline** (`==` override + BC at
1/2), **[B30] = optional strict-FIPS migration** (approved-only + hybrid attribute), **[B440] = the block that
mislabeled the baseline as a template**, now corrected here.

## 441.7 — Verdict: **enforced vs shipped**, split cleanly

The gap asked one yes/no; the honest answer is two axes:

- **Provider priority (BC ahead of Sun):** **ENFORCED** — shipped, declarative, in the overriding master.
  The daemon cannot come up with Sun ahead of BC-FIPS without editing a signed-adjacent policy file.
- **FIPS approved-only strict enforcement:** **NOT enabled** — the FIPS-certified module is primary but runs
  in general mode; the `approved_only` flag is absent. "FIPS on unless licensed off" describes *which BC jar*
  and *which provider is primary*, **not** algorithm-level approved-only enforcement. That stricter posture is
  a deliberate, unshipped migration ([B30]).

---

## Self-verify

| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | njre builds `-Djava.security.properties==%s\bin\policy\java.security` with **two** equals signs | [CERT] | `decomp-njre-bystring.txt:536`, `:324` |
| 2 | `==` (two equals) = **full override** of the master; `=` (one) = append | [CERT-doc] | `jre/lib/security/java.security:7,9,16,18` |
| 3 | override is gated by `security.overridePropertiesFile=true`, set in the stock master | [CERT-doc] | `jre/lib/security/java.security:291` |
| 4 | the stock `jre/lib/security/java.security` has `provider.1=sun…Sun` (Sun first, no BC) | [CERT] | `jre/lib/security/java.security:68` |
| 5 | the **effective** `bin/policy/java.security` has `provider.1=BCFKSWrap`, `provider.2=BCFips`, `provider.3=Sun` | [CERT] | `bin/policy/java.security:72-74` |
| 6 | therefore BouncyCastle precedes Sun by static config, not a runtime `insertProviderAt`/`addProvider` | [CERT] | claim 1 ∧ claim 2 ∧ claim 5 |
| 7 | `-Dorg.bouncycastle.fips.approved_only=true` appears nowhere in njre, `bin/policy/java.security`, `bin/`, `etc/`, `defaults/` | [CERT] | zero-hit `rg 'approved_only\|bouncycastle.fips'` over those paths + njre decomp |
| 8 | FIPS provider is primary but runs in **general** (non-approved-only) mode | [INFER] | claim 7 + BC-FIPS documented approved-only-is-opt-in semantics |
| 9 | this install selects `bcfips` (no `fips140-2` feature), so `provider.2`'s class exists on classpath | [CERT] | `decomp-njre-bystring.txt:250,253` + [B380]/[B440] |
| 10 | [B26] already recorded both the `==` argument and the BC-1/2 ordering; [B440] §440.4 contradicted it | [CERT] | `bloque26.md:120,451-452` vs [B440] claim 7 |
| 11 | [B30] is a strict-FIPS *migration* (edits jre file, `C:HYBRID;ENABLE{All}`, approved-only), not the baseline | [CERT] | `bloque30.md:443-476` |

**Tally:** 11 claims — 9 [CERT], 1 [CERT-doc] counted under CERT-family, 1 [INFER]. No unmarked assertions.
INFER ratio 1/11 = 0.09.

## Connections

- **Corrects [B440]** §440.4 claims 6/7/8 and closes its spawned gap **SP-G9** (§440.7). Pointer added to B440.
- **Confirms/promotes [B26]** — B26's raw `==` + provider ordering was correct and is now the reconciled baseline.
- **Reframes [B30]** — its FIPS provider edits are the *strict-migration* delta over the shipped baseline, not the default.
- **Consistent with [B380]/[B440]** — the bcstd↔bcfips classpath swap and this static provider ordering are two
  independent layers that agree for this install (no `fips140-2` → bcfips → `BouncyCastleFipsProvider` resolvable).
- **Independent of [B425]** — Mocana/DSF (`dsfspi.dll`) is the narrow native provider; it is not in this
  provider list and is registered by its own path.

## Gaps

- **SP-G9 — CLOSED** (this block).
- **SP-G9a (new, low, requires-execution):** live confirmation via `Security.getProviders()` on the running
  station that the effective order matches `bin/policy/java.security` (the `==` override is a launcher argv
  claim; a live read would upgrade §441.4 to `[CERT-live]`). Deferred — same live-station dependency as SP-G3/G6/G8.
- **SP-G9b (new, low):** the *licensed-bcstd* branch names `provider.2=BouncyCastleFipsProvider`, a class that
  does **not** exist in the standard BC jar. Is there a second `bin/policy/java.security` variant, or does the
  daemon rewrite the provider line when `fips140-2` is present? Not investigable on this install (bcfips-only).
