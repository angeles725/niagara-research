# B532 — signing-pki: the licensing watch-map — which processes verify HostId/licenses/modules, when and where they fire, who calls them, and the three interposition ("mirror") points for a complete self-consistent view

**Focus:** `signing-pki` · **Mode:** synthesis (consolidates B481/B487/B518/B519/B521/B524/B528 + fresh `Nre.java` boot-order trace) · **Language:** English.

**Scope.** Answer the operator's full question set in one map: what processes work, who watches what, when
and whether continuously, what code path calls it, who is "al pendiente", how you know it is "bien", and —
defensively — where each of the three gates (HostId, license, module) could be mirrored so the station
shows a self-consistent "everything valid" view. No bypass procedure is described; the interposition
points are the same three the hardening actions (H1–H4/H6) defend. SECRETS DISCIPLINE observed.

**Evidence.** Cited blocks + `organized/baja/baja/{decompiled,vineflower}/com/tridium/sys/Nre.java`
(boot order), `NodeLockedLicenseManager.java` (HostId compare), `ModuleManager.java` (per-JAR verify).

---

## 1. The processes (who works) — `[CERT-live]` [B518]/[B478]

| Process | Role | Consumes / verifies |
|---|---|---|
| `niagarad.exe` (Windows service `Niagara`) | platform daemon + station lifecycle controller | its OWN platform-feature license manager + supervises `station.exe` |
| `station.exe` | the station JVM (drivers, web, Fox) | `security/licenses` + per-JAR module signatures |
| `nre.exe`/`njre` | on-demand launcher/oracle | verifies at launch; NOT a standing watcher |

Active proof: service `Running` + both in `tasklist` + `https://localhost/`=302 + daemon `:5011`=403.

## 2. Who watches what, WHEN — event-triggered, not continuous — `[CERT-live]` [B519]/[B481]/[B487]

| Gate | Fires at | Post-boot re-watch? |
|---|---|---|
| **License** (signature + HostId + dates) | station BOOT only (`licenseManager.postInit()` → `load()` → in-memory feature map) | ❌ none (node-locked). Live-proven: a swap-and-restore was invisible to the running `station.exe` |
| **Module** (per-JAR signature) | add/class-load (`moduleManager.postInit()` → `checkFileSignature` one JAR at a time) | ❌ no global re-scan; a never-loaded class is never verified |
| **HostId** | inside the same boot, within license validation (`isLicenseHostIdValid()`) | ❌ not recomputed after boot |

**Only continuous watcher** = the SUBSCRIPTION entitlement watchdog (6h check-in + clone detection +
operator alarm); this install is node-locked/perpetual ⇒ **no live licensing watcher exists at all**.

## 3. The boot call chain (who calls whom, in order) — `[CERT]` `Nre.java:728-746`

```
verifyPolicyFiles()                        // signature check of java.security / java.policy
moduleManager.initSystemJars()
licenseManager.postInit()                  // GATE 1+2: signature + HostId + dates
moduleManager.postInit()                   // GATE 3: checkFileSignature per JAR
engineManager / serviceManager / stationManager / resourceManager.postInit()
verifyFipsLicense()                        // feature check "fips140-2"
(moduleVerificationMode applied to the above)
```

Every gate is a **direct call from `Nre`** (the station main). There is no separate daemon watching
licenses; `niagarad` only watches the **process-exit code** (`-3`/`-6` = non-recoverable).

## 4. Who is "al pendiente" and how you know it is "bien" — `[CERT]`/`[CERT-live]`

- **Al pendiente = `niagarad` only**, and it watches the station's *life* (exit codes), NOT the license
  content. There is no runtime actor re-reading `security/licenses`.
- **Who says it is "bien":** at boot, the `nre -licenses` oracle (`{valid}` per license + features). In
  runtime, **nobody** — the only report was the boot-time one. This is the structural reason a mirror
  works: between triggers the gates are dormant.

## 5. The three interposition points — a complete, self-consistent "mirror" — `[CERT-live]` on 2 of 3

| Want to show | Exact point | Technique | Status |
|---|---|---|---|
| Licenses `{valid}` | `LicenseUtil.verify(...)` → `return true` | `-@javaagent` + ASM rewrite | ✅ **executed live** [B528] |
| HostId matches | `NodeLockedLicenseManager.isLicenseHostIdValid()` (`this.hostId.equals(Nre.getHostId())`) → `return true` | same agent, one more method | mapped, not executed |
| Modules valid | `SignatureUtil::checkFileSignature` → return `0` | native `Interceptor` hook | ✅ **executed live** [B524] |

A single combined boot agent that rewrites `LicenseUtil.verify` + `isLicenseHostIdValid` (both Java, both
in the `licenseManager.postInit()` path) plus the existing native `checkFileSignature` return — all in the
ONE `station.exe` JVM — would present the self-consistent "everything valid, correct host" view. All three
converge in the same process and the same boot sequence, which is why the technique transfers.

## 6. The defensive read (what actually protects it) — `[CERT]`/`[INFER]`

The gates are interposable because they are **local boot-time code paths**, not remote controls. The real
defenses are therefore: **H4/WDAC** (block loading the agent/hook binary at all), **H1–H3** (strict
posture — a `highSecurity` module-mode raises the failure cost), **H6/FIM** (detect the injected agent or a
swapped/cloned license on disk), and protecting the **HostId fold inputs** ([B424]: hidden key `hid3`,
RegisteredOwner, product id, C: serial). "Siempre" is answered honestly: enforcement is boot-triggered
here; the only always-on component is the *subscription* watchdog, absent on this node-locked install.

## 7. Self-verify

| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | Two long-lived processes + on-demand oracle | `[CERT-live]` | tasklist; [B518] §3c |
| 2 | Boot-only license gate; no node-locked watcher | `[CERT-live]`/`[CERT]` | [B519] §2; [B481]; [B487] |
| 3 | Boot call order `postInit()` chain | `[CERT]` | Nre.java:728-746 |
| 4 | HostId compare is one line `equals(getHostId())` | `[CERT]` | NodeLockedLicenseManager.java:61-63 |
| 5 | 2 of 3 interposition points executed; 1 mapped | `[CERT-live]`/`[CERT]` | [B528] (license), [B524] (module), mapped HostId |

**Tally:** 3 `[CERT-live]` (shared), 3 `[CERT]`, 1 `[INFER]` (explicit). No unmarked claims.

## 8. Connections

- Consolidates [B481]/[B487] (watchers), [B518]/[B519] (gates live), [B521] (per-JAR), [B524]/[B528]
  (the two executed mirrors), [B424] (HostId fold).
- Feeds `docs/niagara-signing-hardening-guide.md` §8 (the three-gate map) as its process/completeness
  companion.
- No new gap. Open items: only blocked-on-artifact SP-G3a / SP-G4 / SP-G9b remain.
