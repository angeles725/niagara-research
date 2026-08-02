# Block 318 — Reversibility runbook for the mini-PC pentest (document mode)

> **Document-mode block (METHODOLOGY §20)** — CAPTURE of how every action of the authorized pentest on the
> lab mini-PC `192.168.0.50` (`iC-Niagara-4.10.9.14`) is reversible: what was done, how each write was
> reverted (with evidence), what is planned (positive-control battery) and its revert plan, plus the
> standard protocol for any future write. Companion to **B316** (findings/verdicts) and **B317** (evasion
> tooling). The operational document lives at
> `corpus/sources/probes/B317-pentest-2026-08-01/RUNBOOK-REVERSIBILIDAD.md` (Spanish, operator-facing);
> this block is the cited corpus record (English, per corpus convention).
>
> **SECRETS DISCIPLINE**: no target secret values here — only structure, paths, PIDs, hashes and protocol.
> Sources: the actual probe/cleanup scripts and transcripts under
> `corpus/sources/probes/B317-pentest-2026-08-01/` `[CERT]`; live end-state verification `[CERT-live]`.
> Markers: `[CERT]` artifact/script cited · `[CERT-live]` measured live · `[INFER]` deduction.

---

## 318.1 — The master rule `[CERT]`

No write without: (1) verified backup, (2) independent oracle, (3) byte-identical restore, (4) a **separate**
post-cleanup verification pass. This is METHODOLOGY §12 applied to the mini-PC: backup-before-destroy,
cross-channel oracle (`nre -licenses` as the license oracle, `Get-Service`/`Get-Process` for services,
`Test-Path`/`Get-ChildItem` for residue), same-PID invariant for untouched services, and verify-removal-
with-the-same-rigour-as-placement.

## 318.2 — Actions already performed and their reversion (all reverted, evidenced) `[CERT]` / `[CERT-live]`

| Action | What it touched | How reverted | Evidence script |
|---|---|---|---|
| Read-only recon (SSH, firewall, ACLs, sockets, KMSpico, `nre -licenses`, `plat.exe`, `station.exe`, `wb.exe -help`) | nothing on disk | nothing to revert (reads only) | `recon-2026-08-01.txt` |
| L-1 unsigned forged license in `security\licenses\` | 1 file + canonicalized copy in `db\<hostId>\` | `Remove-Item` + `db\` cleanup (canonicalization renames to `<vendor>.license` — cleanup must look in `db/`, not only the root) | `run-test.ps1`, `verify-clean.ps1` |
| L-2/L-4 forged cert+license in `security\certificates\` + `security\licenses\` | 2 files + `db\` copy | backup of `Tridium.certificate`+`db`/`inbox` → remove → `db\` cleanup → sha256 verify | `run-test.ps1`, `run-probes5.ps1`, `verify-clean.ps1` |
| L-3 six stage-by-stage probes (`vendor="Tridium"`) | 6 files + canonicalization | same protocol | `run-probes2.ps1`, `clean-residue.ps1` |
| L-8 36h-grace probes (future `generated`) | 3 files + canonicalization | same protocol | `run-probes5/6/7.ps1` |
| L-10 `wb.exe` GUI launch | 1 process + JVM child | `Stop-Process` + verify 0 `wb/java/javaw` procs | `wb-cleanup.ps1` |
| `pentest-staging` + backups under `C:\Users\ASUS\` | temp dirs | `Remove-Item -Recurse -Force` + **independent re-check** (first pass left residue — §318.4) | `residue-cleanup.ps1`, `final-verify.ps1` |

**End-state proof (2026-08-01, independent `final-verify.ps1`)** `[CERT-live]`:
- `certificates\` = only `Tridium.certificate`, sha256 `9E1D3F6D9E66DE4020171FA9D3DFA66F0B75036DDA5B1732A49F7973A4965211` (identical to initial);
- `licenses\` = only `db` + `inbox` (empty); `nre -licenses` = HostId + `Tridium.certificate {valid}` + Licenses `none` + Features `none`;
- PIDs unchanged (`niagarad`=2556, `sshd`=11144); 0 `java/wb` processes; 0 `pentest-*` dirs on the host.

## 318.3 — Planned actions (positive control) and their revert plans `[CERT]`

Once the OEM provides a signed license for `Win-4D6F-169B-CEF1-8F57`:

| Planned test | What it would touch | Revert plan |
|---|---|---|
| Legit license import via `licenses\inbox\` | the `.license` (LicenseManager validates and moves it to `db\<hostId>\`) | backup ALL of `security\licenses\` (root+db+inbox) first → after test: remove imported `.license` + restore tree from backup → verify `nre -licenses` = none/none and `Tridium.certificate` sha256 |
| Station boot with license (`station.exe`/service) | Java station process + station `db\` + station home under `C:\Users\ASUS\Niagara4.10\iSMA CONTROLLI\` | backup station home before creating station → stop station process/service → delete created station dirs → restore from backup → verify `niagarad`/`sshd` PIDs intact and `nre -licenses` |
| Tampering a valid license (flip 1 byte in signed XML) | 1 `.license` | byte-identical copy first (sha256) → restore copy → verify sha256 + oracle |
| System clock (ahead/back for SMA/expiration) | `Set-Date`/`w32tm` | record exact time + NTP source before → `w32tm /resync` or `Set-Date` to original → verify with `Get-Date` + external NTP → confirm `nre -licenses`/logs unaffected. ⚠ Clock changes affect logs/timestamps and KMS activation — only with explicit authorization, at the END of a test window |
| HostId spoofing (volume serial / MachineGuid change) | volume `C:` / registry | ⚠ **NOT recommended without a full image backup**: changing the volume serial changes the derived HostId (`getHostId0` → `GetVolumeInformationA`), can invalidate existing licenses and may trigger `CLONED_FILE`. If done: image backup + record original serial (`D2DE8C94`) + restore serial + reboot + re-verify HostId |

## 318.4 — Standard protocol for any future write `[CERT]`

```
1. BACKUP   : copy everything to be touched to host staging; sha256 each file.
2. PLANT    : write/plant the test artifact.
3. ORACLE   : confirm effect via an independent channel (nre -licenses, Get-Service, Test-Path).
4. RESTORE  : revert file by file + clean db/ (canonicalization) + staging.
5. VERIFY   : a SEPARATE later pass (never the same command): sha256, dir tree, PIDs, oracle,
              0 pentest-* residue on the host.
```

## 318.5 — Live lesson: cleanup must be re-verified in a separate pass `[CERT-live]`

The `final-state.ps1` cleanup pass reported `staging-gone=True`; a later independent check
(`state-proof.ps1`) found `pentest-staging` still present with 5 probe files (`probe5/6/7-*.license`) and
1 backup dir. Probable cause: cleanup ran in the same window as the last staging `scp` and was not
re-checked afterwards. **Adopted fix**: every cleanup ends with an independent check in a separate command
(`residue-cleanup.ps1` → `final-verify.ps1`), and the "pristine" criterion includes
`Test-Path pentest-staging` + `pentest-*` count = 0 + invariant PIDs. This incident is deliberately kept in
the record: a single-pass "clean" report is not evidence of a clean host.

## 318.6 — Self-verify

- `verify-block.sh niagara-mental-model-bloque318.md` — exit 0 (verified above).
- Marker tally (whole block, incl. legend): `[CERT-live]` 6 · `[CERT]` 7 · `[INFER]` 2 (legend + §318.1 phrasing; no load-bearing inference). Load-bearing tokens re-verified: all cited scripts exist under `sources/probes/B317-pentest-2026-08-01/` (ls-confirmed); end-state values match the live `final-verify.ps1` output (sha256, PIDs 2556/11144, empty trees, oracle none/none).
- The residue incident (§318.5) is cross-checked against both transcripts (`state-proof.ps1` finding the
  residue; `residue-cleanup.ps1`+`final-verify.ps1` proving its removal).
