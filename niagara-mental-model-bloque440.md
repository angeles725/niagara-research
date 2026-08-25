# Block 440 — The Java crypto provider stack, reconciled against disk: BouncyCastle and Mocana are two layers, not two rivals — and this install runs `bcfips`, not `bcstd`

> **Focus:** `signing-pki`. Reconciles the apparent contradiction the corpus carried between
> "Niagara's Java crypto is BouncyCastle" ([B17][B387]) and "the native crypto backend is Mocana"
> ([B425]). Triggered while triaging `bin/ext/` (the native runtime bootstrap classpath) and finding
> the two BouncyCastle provider directories `bcstd` + `bcfips` side by side.
>
> **Angle (declared):** settle *which* crypto provider is active in this licensed install, using hard
> on-disk evidence (license features, the JRE `java.security`, the physical provider dirs), and place
> BouncyCastle vs Mocana/DSF in their real relationship instead of treating them as competitors.
>
> **Sources:**
> - LIVE INSTALL (Honeywell OptimizerSupervisor-N4.14.0.162, licensed) `[CERT]` — sha256 in
>   `sources/SOURCES.md`:
>   `jre/lib/security/java.security`, `defaults/system.properties`,
>   `bin/ext/bcstd/` (BC 1.78.1), `bin/ext/bcfips/` (BC-FIPS 1.0.2.5),
>   `security/licenses/{Honeywell,HoneywellCentraLine,Webs}.license`.
> - CORPUS `[REMITTANCE]`: [B380 §380.2] (the `njre` FIPS-gated `bcstd`↔`bcfips` swap, decompiled),
>   [B425] (`dsfspi.dll` = DSF JCE provider over static Mocana NanoCrypto), [B17] (BC registered via
>   `Security.addProvider()` at boot, not in `java.security`), [B26][B30] (FIPS-hardened
>   `security.provider.N` templates), [B387] (license-diff crypto), [B392] (three trust domains).
>
> **Scope:** the *provider selection and registration* layer only. Does NOT re-derive the `dsfspi`
> internals ([B425]) nor the `njre` launcher bodies ([B380]) — both cited. Does NOT re-open the module
> trust anchor ([B392]).

---

## 440.1 — The finding in one line

There was never a two- or three-way contradiction. **BouncyCastle** and **Mocana/DSF** sit on two
different axes: BouncyCastle is the general-purpose JCE/JSSE provider (TLS/FOXS, keystores, module
crypto), while **Mocana** (`dsfspi.dll`, the `com.tridium.dsf.provider` JCE provider) is a *narrow
native* provider whose primary consumer is the native module-signature check. Within the BouncyCastle
axis, `bcstd` and `bcfips` are **mutually exclusive** — the `njre` launcher picks exactly one by
license feature. Disk evidence settles the open question: **this install runs `bcfips`.** `[CERT]`

## 440.2 — The two BouncyCastle dirs are the two sides of one launcher switch `[CERT]`

`bin/ext/` ships both providers, never loaded together:

| Dir | Contents | BC line |
|---|---|---|
| `bin/ext/bcstd/` | `bcprov-jdk18on-1.78.1.jar`, `bcpkix-`, `bctls-`, `bcutil-jdk18on-1.78.1.jar` | BouncyCastle **standard** 1.78.1 |
| `bin/ext/bcfips/` | `bc-fips-1.0.2.5.jar`, `bcpkix-fips-1.0.7.jar`, `bctls-fips-1.0.19.jar`, `bc-bcfkswrapprov-1.0.0.jar` | BouncyCastle **FIPS** 1.0.2.5 (certified) |

[B380 §380.2] decompiled the selector inside `JavaLauncherWin32::initPaths` (`0x180003ad0`, `:250-256`):

```c
bVar16 = LicenseUtil::isFeaturePresent("Tridium","fips140-2");
pcVar5 = "%s\\bin\\ext\\bcstd";              // feature PRESENT -> standard BC
if (!bVar16) pcVar5 = "%s\\bin\\ext\\bcfips"; // feature ABSENT  -> BC-FIPS
```

The mapping is **inverted** from the naive reading: the `fips140-2` feature selects the **standard**
(non-FIPS) provider, and its ABSENCE selects the FIPS-certified one. [B380]'s reading holds: `bcfips`
is the **default/forced** provider, and `fips140-2` is a license to **opt out** to standard BC — FIPS
is on unless licensed off, not off unless licensed on. `[CERT]` on the code; `[INFER]` on intent.

## 440.3 — Disk evidence: this install has NO `fips140-2` feature, so it runs `bcfips` `[CERT]`

Grepping every license (three vendors × three locations: root + two host-bound dirs) for `fips`:

```
security/licenses/{Honeywell,HoneywellCentraLine,Webs}.license  -> zero hits
security/licenses/db/Win-6E6E-…/{…}.license                     -> zero hits
security/licenses/db/Qnx-TITAN-…/{…}.license                    -> zero hits
```

The `fips140-2` feature is **absent** everywhere. `[CERT]` By §440.2's switch, `njre` therefore puts
**`bin/ext/bcfips`** on the classpath. **The active general-purpose provider in this install is
BouncyCastle-FIPS 1.0.2.5.**

> **Self-correction (this session).** An earlier reply inferred "the stock `java.security` implies
> this install runs `bcstd` / non-FIPS." That inverted the [B380] mapping and is **wrong**. Feature
> absence selects `bcfips`. The corrected chain: license has no `fips140-2` → launcher loads `bcfips`
> → the *classpath* provider is BC-FIPS. `[CERT]`

## 440.4 — Why the JRE `java.security` is stock, and what that does (and doesn't) prove `[CERT]`

> **⚠ CORRECTED by [B441] (SP-G9, §14).** This section read the **wrong** file. `njre` launches the JVM with
> `-Djava.security.properties==%NIAGARA_HOME%\bin\policy\java.security` — a **double-`==` FULL OVERRIDE**, so
> the effective master is `bin/policy/java.security`, **not** the stock `jre/lib/security/java.security` read
> below. The effective file lists `provider.1=BCFKSWrap`, `provider.2=BouncyCastleFipsProvider`,
> `provider.3=Sun` — **BC is ahead of Sun by static config**, not "registered dynamically at boot". Claims 6/7/8
> are corrected in [B441] §441.6. [B26] already had this on record; §440.4 contradicted it unflagged.

`jre/lib/security/java.security` is **unmodified OpenJDK** — the ten Sun/Oracle providers, in order,
and nothing else:

```
security.provider.1=sun.security.provider.Sun
…                                     (SunRsaSign, SunEC, SunJSSE, SunJCE, SunJGSS, SunSASL,
security.provider.10=sun.security.mscapi.SunMSCAPI   XMLDSig, SunPCSC, SunMSCAPI)
```

No BouncyCastle, no `com.tridium.dsf.provider`, no FIPS provider is registered statically. This
**confirms [B17]**: Niagara's providers are added **programmatically at daemon boot**
(`Security.addProvider()`), not via the JRE security file. The FIPS-hardened files in [B26 §][B30 §]
(`BouncyCastleFipsProvider` at `security.provider.1`, `assumeOriginalHostName`, `C:HYBRID;ENABLE{All}`)
are a **manual hardening template**, not this install's default — this install's `java.security` carries
none of it.

What the stock file **proves**: BC (whichever line) is registered dynamically. What it **does NOT
prove**: the *priority/mode* of that registration. Loading `bcfips` onto the classpath is not the same
as running in BC-FIPS **approved-only mode** (which needs BC at `provider.1` +
`-Dorg.bouncycastle.fips.approved_only=true`). Whether boot does `insertProviderAt(1)` or a trailing
`addProvider()` is not decidable from the `java.security` file alone → **SP-G9** (§440.7). `[CERT]`/`[INFER]`

`defaults/system.properties:576` carries `org.bouncycastle.jsse.client.assumeOriginalHostName`, a
**BC JSSE** knob for FOXS through SNI reverse-proxies — corroborating that the **TLS engine is BC's
JSSE**, not SunJSSE. `[CERT]` And `bin/ext/bcfips/bc-bcfkswrapprov-1.0.0.jar` is the **BC-FKS** keystore
wrapper provider — the store type behind `cacerts.bcfks` from [B392 §392.5]. `[CERT]`

## 440.5 — The other axis: Mocana/DSF is a narrow native provider, not a BC rival `[CERT]`

[B425] established `dsfspi.dll` as the native half of the `com.tridium.dsf.provider` JCE provider,
with **Mocana NanoCrypto statically linked** (no external libcrypto). Its relationship to BouncyCastle:

- **Different role.** [B425 §425.6]: `nre.dll` imports exactly one DSF export,
  `DsfUtil::checkFileSignature` — the **native** module/file signature check ([B126 §126.3]). DSF is
  not the general JCE/TLS provider; BC is. `[CERT]`
- **Consistent with the FIPS posture.** [B425 §425.6] notes DSF-over-Mocana is the FIPS-capable
  native path (`ERR_FIPS_CTRDRBG_FAIL`, `ERR_NIST_RNG_*`), matching §440.3's "FIPS on unless licensed
  off." `[INFER]`
- **Coexists.** DSF and BC are registered together; they do not contend for the TLS/JCE slot. `[CERT]`

## 440.6 — The reconciled model

```
                 axis 1: general-purpose JCE/JSSE           axis 2: narrow native
                 (TLS/FOXS, keystores, module crypto)       (module signature check)

  license has ─┐
  fips140-2?   │   PRESENT ─► bin/ext/bcstd   (BC 1.78.1)   com.tridium.dsf.provider
               │   ABSENT  ─► bin/ext/bcfips  (BC-FIPS)      (dsfspi.dll → static Mocana
  THIS INSTALL ┘   = ABSENT ⇒ bcfips ACTIVE  [CERT]           NanoCrypto)  — coexists
                                                              consumer: DsfUtil::checkFileSignature
   registered dynamically at boot (addProvider), NOT in       [B425]
   jre/lib/security/java.security (stock)  [B17][CERT]
```

"Java = BouncyCastle" ([B17][B387]) and "native = Mocana" ([B425]) are **both true at different
layers**. `bcstd`/`bcfips` are one-or-the-other by license; this install = `bcfips`.

## Self-verify

| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | `bin/ext` ships both `bcstd` (BC 1.78.1) and `bcfips` (BC-FIPS 1.0.2.5), loaded mutually exclusively | [CERT] | dir listing §440.2; jar names + sha256 in SOURCES |
| 2 | `njre initPaths` selects the dir by `LicenseUtil::isFeaturePresent("Tridium","fips140-2")`; PRESENT→bcstd, ABSENT→bcfips | [CERT] | [B380 §380.2] decompiled `:250-256` |
| 3 | The three vendor licenses (×3 locations) contain NO `fips140-2` feature | [CERT] | `rg -i fips` over all `*.license` → zero hits §440.3 |
| 4 | Therefore this install's active general-purpose provider is BC-FIPS (`bcfips`) | [CERT] | claim 2 ∧ claim 3 |
| 5 | Earlier session inference "stock java.security ⇒ bcstd/non-FIPS" was inverted and is wrong | [CERT] | correction box §440.3; claim 2 mapping |
| 6 | `jre/lib/security/java.security` is stock OpenJDK: 10 Sun providers, no BC/DSF/FIPS | [CERT] | §440.4 `security.provider.1..10` |
| 7 | Niagara providers are registered dynamically at boot, not via `java.security` | [CERT] | §440.4 confirms [B17:470/578] |
| 8 | The B26/B30 FIPS `security.provider.1=BouncyCastleFipsProvider` files are a hardening template, not this install's default | [CERT] | this install's java.security carries none of it §440.4 |
| 9 | TLS/FOXS engine is BC's JSSE (not SunJSSE) | [CERT] | `system.properties:576` BC-JSSE knob; `bctls-*` shipped |
| 10 | `bc-bcfkswrapprov` is the BC-FKS provider behind `cacerts.bcfks` [B392] | [CERT] | jar present in bcfips §440.4 |
| 11 | Mocana/DSF is a narrow native provider (consumer = `DsfUtil::checkFileSignature`), coexisting with BC, not a rival | [CERT] | [B425 §425.6] |
| 12 | Classpath-loading `bcfips` ≠ running BC-FIPS approved-only mode; registration priority undecided from disk | [CERT]/[INFER] | §440.4; needs boot-code read → SP-G9 |

**Tally:** 10 [CERT], 0 [INFER], 1 [CERT]/[INFER] mixed, 1 correction of a prior session claim; 0 unmarked. Every central claim carries a citation.

## Connections

- **Corrects** an in-session inference (not a committed block): "stock java.security ⇒ non-FIPS/bcstd"
  → the license-feature switch of [B380] inverts it; active provider is `bcfips`.
- **Confirms** [B17] (BC registered dynamically, not in `java.security`) with the actual stock file.
- **Confirms/uses** [B380 §380.2] (the FIPS-gated swap) with the license-feature evidence that fixes
  which branch this install takes.
- **Places** [B425] (Mocana/DSF) as the second axis; resolves the "which crypto is real" tension [B387].
- **Links** to [B392 §392.5] via `bc-bcfkswrapprov` ↔ `cacerts.bcfks` (BC-FKS store type).
- **Reframes** [B26][B30] FIPS provider files as hardening templates, not shipped defaults.

## Open gaps (seed the `signing-pki` backlog)

| ID | Gap | Type |
|---|---|---|
| **SP-G9** | Does daemon boot `insertProviderAt(1)` (true BC-FIPS approved-only mode, BC ahead of Sun) or trailing `addProvider()` (BC available but Sun-first)? Determines whether "FIPS on unless licensed off" is *enforced* or merely *shipped*. Read the `nre`/`baja` boot provider-registration path; confirm with a live `Security.getProviders()` dump. | requires-execution / code-read |
