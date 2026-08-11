# Block 386 — license-diff L1: what a Niagara license changes on disk is the ENTIRE `security/` subtree — an unlicensed installed instance has no `security/licenses`, `security/certificates`, or `truststore.jks` at all

> **CORRECTED by [B442] (2026-08-11):** this is a true observation for the sampled unlicensed 4.13.2.18
> install, not a release-independent invariant. The live unlicensed 4.10.9.14 host has a baseline
> `security/` tree (valid Tridium certificate, empty `licenses/{db,inbox}`, policy, custom signer registry).
> Across versions, use validated `.license` records / `nre -licenses`, not parent-directory presence.

> **Focus `license-diff` (bootstrapped 2026-08-07) — L1 inventory + the license-axis answer.** The user asked
> to diff a LICENSED vs an UNLICENSED install with `diffoscope` to document what a license changes on disk.
> This first block builds the inventory, corrects the artifact framing (the user-selected "unlicensed" B is
> an INSTALLER package, not an installed instance), re-pairs against a genuinely installed-but-unlicensed
> instance, and lands the categorical answer. READ-ONLY over all installs. `live-install` → SECRETS
> DISCIPLINE (HostId FORMAT and license STRUCTURE only; the `.license`/`.certificate` files carry only public
> keys+signatures per [B126]).
>
> Installs compared:
> - **A = LICENSED**: `/mnt/c/Honeywell/OptimizerSupervisor-N4.14.0.162` (Honeywell OEM, N4.14.0.162; the
>   install RE'd in B124-B385/B2/B126). 66,559 files.
> - **B = user-picked "unlicensed"**: `…/Downloads/Tridium_EMEA_N4_Supervisor-4.15.3.28.2` (N4.15.3.28.2).
>   908 files. **Finding: an INSTALLER package, not an install** (§386.2).
> - **iC = re-pair, genuinely installed + unlicensed**: `/mnt/c/Niagara/iC-Niagara-4.13.2.18` (base
>   Niagara, N4.13.2.18; has `bin/`+`defaults/`+742 modules, 0 licenses). Used for the license-axis (§386.3).
>
> **THREE-AXIS attribution** (the focus's governing caveat): findings are tagged LICENSE / VERSION /
> ARTIFACT-TYPE. Only LICENSE-axis findings answer the focus question.
> Tools: SHA-256 (attempted) / path+size inventory, `diffoscope 327` (kit venv). Evidence:
> `audits/B386-license-diff-inventory.txt`, `audits/B386-only-in-B.txt`, `audits/B386-diffoscope-sysprops.txt`.
> Markers: `[CERT]` observed on disk (path/count cited) · `[INFER]` deduction.

---

## 386.1 — Method: full-tree SHA-256 was infeasible; path+size inventory + targeted diffoscope `[CERT]`

Hashing both full trees over the WSL `/mnt/c` mount repeatedly hit the harness timeout (SHA-256 of a ~full
install is I/O-bound over the 9p mount; two attempts were SIGTERM'd mid-run). **Tool wall, registered:** a
full-tree byte hash is not the right instrument here. The method pivoted to a **path+size inventory** (fast —
no byte reads: `find -printf '%s\t%p'`), which classifies the changed set (only-in-A / only-in-B /
same-path-same-size / same-path-diff-size) without hashing, and reserves `diffoscope` for the small
license-axis subset. Same-size-same-path is a strong identity proxy; diff-size is a definite change. `[CERT]`

---

## 386.2 — ARTIFACT-TYPE finding: the user-selected B is an installer, not an install `[CERT]`

The A-vs-B inventory (`audits/B386-license-diff-inventory.txt`): `[CERT]`

| Class | Count | Meaning |
|---|---|---|
| A files | 66,559 | full installed instance (bin, defaults, security, users, docs, palettes, modules) |
| B files | 908 | — |
| only-in-A | 65,844 | A's install-time + OEM + docs tree B lacks |
| only-in-B | 193 | B's installer/SDK-only files |
| common paths | 715 | present in both (ALL under `./modules`) |
| common, same size | **1** | essentially nothing is byte-identical |
| common, diff size | **714** | 714 same-named module jars differ |

B's top level is `Installer_x64.exe`, `Uninstaller_x64.exe`, `dist/`, `modules/` (776 jars), `dev/` (SDK
examples: `niagaraModulesExample`, `authClientExample`), `install-data/`, `overlay/` — and **zero `bin/`,
`security/`, or `defaults/`**. So B is the **pre-install distribution package**, never installed or licensed;
its "0 licenses" is because it was never commissioned, not because it is an unlicensed running instance.
`[CERT]` **ARTIFACT-TYPE axis.** Consequence: the only bilaterally-comparable surface is the 715 common
module jars, and 714/715 differ purely because A is 4.14 and B is 4.15 — a **VERSION-axis** delta, not a
license one. B cannot answer the license question by a two-sided diff. `[CERT]`

---

## 386.3 — LICENSE-axis answer (re-paired): a license MATERIALIZES the whole `security/` subtree `[CERT]`

Re-pairing A against a genuinely installed-but-unlicensed instance — `iC-Niagara-4.13.2.18` (base Niagara,
`bin/` 28 files, `defaults/` 11, `modules/` 742, **0 licenses**) — gives the categorical result: `[CERT]`

- **The unlicensed installed instance has NO `security/` directory at all** (`[ -d iC/security ]` → absent). `[CERT]`
- **The licensed install's `security/` is 13 files** (`audits/B386-…-inventory.txt`): `[CERT]`
  ```
  security/licenses/Honeywell.license
  security/licenses/HoneywellCentraLine.license
  security/licenses/Webs.license
  security/licenses/db/<Qnx-JACE-HostId>/{Honeywell,HoneywellCentraLine,Webs}.license
  security/licenses/db/<Win-supervisor-HostId>/{Honeywell,HoneywellCentraLine,Webs}.license
  security/certificates/{Honeywell,HoneywellCentraLine,Tridium}.certificate
  security/truststore.jks
  ```
  So licensing materializes: **per-vendor licenses** (Honeywell / HoneywellCentraLine / Webs), a **per-host
  license DB** `licenses/db/<HostId>/` keyed by HostId — TWO hosts here, a **Qnx-TITAN JACE** and a
  **Win supervisor** (HostId format `<platform>-<4×4-hex>`, the Win one is [B126]'s `Win-6E6E-…`) — the
  **vendor certificate chain** (Honeywell → HoneywellCentraLine → Tridium root), and the **`truststore.jks`**.
  `[CERT]` **LICENSE axis** (SECRETS DISCIPLINE: HostId format + structure cited, license internals remitted
  to [B126 §126.1/§126.6] and [B2] — DSA-1024/SHA-1 sig, `hostId=` binding, `<feature>` gates).

**Corrected scope [B442]:** in this 4.13.2.18 sample, licensing correlates with creation of
`security/{licenses,certificates}` + `truststore.jks`; the comparison remains valid for this pair. It is not
a cross-version invariant: unlicensed 4.10.9.14 retains baseline security/certificate/signing material.
Nothing in `modules/`, `bin/`, or `defaults/` is added or removed by licensing; the portable indicator is a
validated HostId-bound `.license` / loaded feature set, not existence of the parent `security/` directory.
`[CERT]/[INFER]` (CERT: this pair's subtree split; INFER: cross-version mechanism, corrected by B442 live data).

---

## 386.4 — `diffoscope` validated `[CERT]`

`diffoscope 327` (kit venv) on `defaults/system.properties` (A vs iC — both installed) produced a clean
unified diff (`audits/B386-diffoscope-sysprops.txt`), confirming the tool is operational for the follow-up
per-file license/config diffs. The system.properties delta itself is VERSION+VENDOR-axis (4.13 base vs 4.14
Honeywell), not license. `[CERT]`

---

## 386.5 — Defensive / operational note `[CERT]`

1. **A license is a HostId-bound artifact set, not a code change** `[CERT]` (§386.3): moving/copying a
   licensed install to a different machine leaves `security/licenses/db/<HostId>/` pointing at the OLD host
   ids; the license only validates on the hosts it names (ties to [B2]/[B126] HostId binding). [B442] corrects
   the filesystem diagnostic: an unlicensed instance may retain baseline `security/`; authoritative state is
   the validated HostId-bound license/feature set, recoverable only by legitimate licensing for its HostId.
2. **The `.license`/`.certificate` files are public** (`[B126]`): public keys + DSA/RSA signatures — safe to
   inventory by structure; no private key material is in the licensed `security/` tree read here (the private
   signing keys live at Tridium, not on the install). `[CERT]`

No secret VALUES extracted; HostIds cited by format/role; read-only over all three installs.

---

## 386.6 — Self-verify

**Token re-checks** (`audits/B386-license-diff-inventory.txt`):
1. A=66,559 files, B=908, only-A=65,844, only-B=193, common=715, common-diff-size=714, same-size=1 — ✓ (inventory).
2. B top-level = Installer_x64.exe/dist/modules/dev, no bin/security/defaults — ✓ (`find -maxdepth 1`).
3. A `security/` = 13 files incl. `licenses/db/<2 HostIds>/` + 3 certificates + `truststore.jks` — ✓ (`find`).
4. iC-Niagara-4.13.2.18 `security/` dir ABSENT; has bin(28)/defaults(11)/modules(742), 0 licenses — ✓.
5. diffoscope 327 produced a unified diff on system.properties — ✓ (`audits/B386-diffoscope-sysprops.txt`).

**5/5 tokens re-verified.**

**Marker tally**: `[CERT]` ≈ 17 · `[INFER]` 2 (no on-disk module is license-gated; the moved-install
corollary). Ratio ≈ 0.12 — low; EVIDENCE block (inventory). This L1 also ABSORBS most of L2 (the
`security/` diff) — re-scope L2 to the per-file STRUCTURE of the licensed `security/` vs the corpus's
[B126]/[B2], not an A-vs-B byte diff (B has no `security/`).

---

## 386.x — Connections

- **[B126]** — the `.license`/`.certificate` internals (DSA-1024/SHA-1, HostId binding, feature gates) this
  block inventories from the outside; §386.3 is the on-disk container of B126's format findings.
- **[B2]** — licensing/HostId model: L1 shows its on-disk footprint is the `security/licenses/db/<HostId>/` tree.
- **[B380]/[B381]** — runtime license enforcement (`fips140-2`/`developer` gates, DPAPI systempw) reads license
  records; [B442] shows an unlicensed 4.10 install can still retain non-license security baseline files.
- **Forward (this focus)**: L2 (licensed `security/` structure vs corpus — mostly absorbed here), L3 (module
  version delta 4.14↔4.15, VERSION axis — needs the `japicmp` tool for API-level), L5 (config/defaults diff),
  L6 (feature-gate map — what a license ENABLES at runtime). **Re-pair note:** for a same-version license
  diff, the 4.13.2.18 pair (installed Optimizer-licensed vs iC-unlicensed) is the cleanest available.
