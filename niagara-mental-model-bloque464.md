# B464 — Obtaining the station .bog without Workbench: two routes (platform File Transfer vs station BackupService), their RE cost, and the passphrase wall that limits what a copy yields (focus jace8000, J8)

> **Focus:** `jace8000` (§16). **Gap:** J8 — the operator's explicit ask: can the platform protocol be
> reverse-engineered to enter the platform and pull a copy of the station `.bog` without Workbench?
> **Phase:** §12 dynamic (live at `192.168.1.140`, admin) + analysis. Read-only (no backup triggered / no
> mutation this iteration). `live-install` → SECRETS DISCIPLINE.
> **Block type: DESIGN/ANALYSIS** — a feasibility synthesis; a higher [INFER] ratio is expected and healthy
> (§11), not an exhaustion signal.
> **Sources:** `[CERT-live]` service-tree probe this session · `[CERT]` corpus [Block 16]/[Block 17]/[Block 114]
> (`.dist`/`.bog` format), [Block 457] (ported SCRAM login), [Block 460]/[Block 461]/[Block 462]/[Block 463]
> (this focus) · `[CERT-doc]` niagara-help (`AXtoN4Migration` Station Copier).
>
> **Bottom line:** yes, a copy is obtainable **without Workbench**, and the *cheaper* route is not RE-ing the
> platform daemon at all — it is the **station BackupService** over the already-ported authenticated channel,
> because **BackupService is live-confirmed present and you hold station `admin`**. RE-ing the *platform*
> protocol (Station Copier / File Transfer on :5011) is the harder route and only adds value when you lack
> station admin. **Either route hits the same wall:** protected fields in `config.bog` are **System-Passphrase
> encrypted at rest**, so a copy yields the full *structure/config* but not the sealed *secrets* without the
> passphrase.

## §464.1 — Two routes to the same file

| | Route 1: platform File Transfer / Station Copier | Route 2: station BackupService |
|---|---|---|
| Channel | platform daemon :5011 ([Block 460]) | station Fox :4911 / web :443 ([Block 461]) |
| Credential | **platform** login | **station admin** (you have it) |
| Protocol to port | the niagarad platform protocol (unported) | Fox (invoke `BackupService.backup()`) |
| Output | direct `config.bog` from `/home/niagara/stations/<n>/` | a `.dist` (zip) containing `config.bog` + files |
| Live status here | daemon reachable, 403-to-GET (handshake needed) | **BackupService present (confirmed)** |

- `[CERT-live]` `Services/BackupService`, `Services/FoxService`, `Services/PlatformServices`,
  `Services/UserService`, `Services/WebService` all resolve (200, titled) via the ORD navigator — **the station
  backup machinery exists on this JACE.** No `~/backups` folder is present yet (`/file/!backups/` → 404): no
  backup has been taken.
- `[CERT-doc]` The platform **Station Copier** "transfers the station … to the stations folder" of the daemon
  user home (`AXtoN4Migration/CopyingANewStationToTheDaemonUserHo.txt:30,43`) — the platform-login route that
  reaches `config.bog` directly.

## §464.2 — Route 2 is the practical answer (least RE, you already have the credential)

Because `api-access` already ported the **SCRAM web login** ([Block 457]) and you hold `admin`, the missing
piece for Route 2 is a **Fox client** (port 4911, TLS) that invokes the `BackupService` backup action; the
service writes a `.dist` into the daemon user home `backups/` folder, which is then downloadable (via
`/file`/`/ord file:` if the station serves that path, or via platform File Transfer). Concretely:

1. Authenticate the station (SCRAM, [Block 457]) — done, live-confirmed.
2. Open a Fox session on :4911 (TLS) and invoke `station:|slot:/Services/BackupService` → `backup()`.
3. Retrieve the produced `.dist` and open it — it is a standard zip whose `config.bog` is the documented
   `.bog` XML tree ([Block 16]/[Block 17]/[Block 114]).

**RE cost:** porting Fox is more work than SCRAM — Fox is a **stateful, framed** protocol (the corpus touched
its wire form in the `protocols` focus, [Block 131]) rather than a 3-message handshake — but it is a
known-shape protocol, not a black box, and it is the same channel Workbench uses. This is `requires-execution`
(build a Fox client), tracked as **J8-G1**, but the path is unobstructed and needs no vendor cooperation.

## §464.3 — Route 1 (platform RE) and why it is the harder, narrower option

To pull `config.bog` through the **platform daemon** you must reproduce the niagarad platform protocol: the
live daemon **403s every plain HTTP request with no auth challenge** ([Block 460] §460.1), so the exact
digest/nonce handshake and the File-Transfer/Station-Copier request framing must be recovered (**J3-G1**). The
corpus already decompiled the daemon binary on the Windows side ([Block 381] `plat`/`niagarad`), which is a
head start, but the JACE runs the **QNX** build and the *wire* protocol was never captured. Route 1 only wins
when you have **platform** credentials but **not** station admin — otherwise Route 2 dominates. This is the
reverse-engineering the operator asked about; the honest assessment is that it is **feasible but strictly
harder than Route 2**, with no unique payoff when station admin is in hand.

## §464.4 — The wall both routes share: passphrase-encrypted secrets

`[CERT-doc]` platform tools "**convert files to use the correct encryption key**" ([Block 462] §462.6,
`aPlatformSystemPassword:52`). The **System Passphrase** encrypts sensitive `config.bog` fields at rest, so:

- A copied/backed-up `config.bog` is **readable as structure** — components, links, config values, the wire
  graph — matching the documented `.bog` format ([Block 114]).
- Its **protected fields** (stored credentials, keyring material, some service secrets) are **ciphertext**
  sealed to the source JACE's passphrase. Without that passphrase you cannot decrypt them, and restoring the
  `.bog` onto a JACE with a *different* passphrase triggers the **passphrase-mismatch** condition ([Block 463]
  §463.5 / J6).

So "a copy of the `.bog`" ≠ "the station's secrets." You get the engineering (which is what an integrator
usually wants), not the sealed credential material — a deliberate at-rest protection, consistent with the
corpus thesis that Niagara encrypts *who-can-do-what* strongly ([Block 392] signing/at-rest).

> **§14 refined in [Block 466]:** this section says "passphrase-encrypted," which is exact for a
> **backup/exported** copy (portable domain, decryptable with the passphrase). But a **raw copy of the
> *running* `config.bog`** in the daemon User Home is sealed with a **machine-only random key that never
> leaves the JACE** — a *stronger* wall than the passphrase, un-decryptable off-box even with the passphrase.
> See [Block 466] §466.3. Net: the **BackupService route re-encrypts to the passphrase key** (recoverable with
> the passphrase); a raw daemon-home grab is not.

## §464.5 — Verdict

- **Without Workbench, with station admin:** obtainable via **Route 2** (station BackupService over a ported
  Fox client). Live-confirmed the service exists; only the Fox client is unbuilt (J8-G1).
- **Without station admin, with platform login:** obtainable via **Route 1** (platform File Transfer / Station
  Copier), after porting the platform handshake (J3-G1) — harder.
- **With neither credential:** **not** obtainable by protocol RE. Serial access lets you *wipe* or *restore
  your own image* ([Block 463]) but not read the existing station's sealed data; credential reset needs a
  **Tridium-signed key** ([Block 463] §463.2). RE does not defeat the passphrase or the vendor signature.
- **Any route:** the copy yields structure/config, **not** passphrase-sealed secrets.

## Self-verify

| # | Claim | Marker | Citation | Checked |
|---|---|---|---|---|
| 1 | BackupService + Fox/Platform/User/Web services present live | [CERT-live] | this session | ✓ titles resolve 200 |
| 2 | No ~/backups folder yet (404) | [CERT-live] | this session | ✓ |
| 3 | Station Copier transfers station to daemon stations folder (platform route) | [CERT-doc] | CopyingANewStationToTheDaemonUserHo:30,43 | ✓ token |
| 4 | Platform daemon 403-to-GET, handshake unported | [CERT] | [Block 460] | ✓ corpus |
| 5 | .dist/.bog format already documented | [CERT] | [Block 16]/[Block 17]/[Block 114] | ✓ corpus |
| 6 | SCRAM login already ported (Route 2 auth ready) | [CERT] | [Block 457] | ✓ corpus |
| 7 | Protected .bog fields are passphrase-encrypted at rest | [CERT-doc] | [Block 462] §462.6 / aPlatformSystemPassword:52 | ✓ |
| 8 | Route feasibility comparison / verdict | [INFER] | this analysis | design synthesis |

Marker tally: [CERT-live] ×2 · [CERT-doc] ×2 · [CERT] ×3 (corpus) · [INFER] = the route-feasibility judgments
(expected in a DESIGN block). Load-bearing *facts* (service presence, format, gates) are all CERT; the
*verdict* is reasoned [INFER] resting on them. Ratio read per §11 as DESIGN.

## Connections

- **[Block 460]/[Block 462]** — the platform daemon gate and the filesystem routes Route 1 depends on.
- **[Block 461]/[Block 457]** — the station channel + ported SCRAM login Route 2 depends on.
- **[Block 463]** — recovery routes; the "neither credential" branch and the Tridium signature wall.
- **[Block 16]/[Block 17]/[Block 114]** — `.dist`/`.bog` format (REMITTANCE, not re-derived).

## Open gaps

New child **J8-G1** (requires-execution): build a minimal **Fox client** to invoke `BackupService.backup()`
and retrieve the `.dist` — the concrete no-Workbench `.bog` grab; the SCRAM half is ported, the Fox half is
not. **J3-G1** stays (platform-handshake bytes for Route 1). Queued: J2, J6, J9, J10, J11.
