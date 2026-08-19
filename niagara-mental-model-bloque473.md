# B473 — J8 DEMONSTRATED END-TO-END: a hand-rolled Fox client pulled the live station's config.bog over foxs:4911 with no Workbench (read-only save=false) — plus live corrections: the JACE runs QNX 7.0, OpenJDK, Host ID Qnx-TITAN-… (focus jace8000, J8-G2 impl; §14 live-refines B459/B465)

> **Focus:** `jace8000` (§16). **Gap:** J8-G2 impl — actually retrieve the `.bog` over the Fox client.
> **Phase:** §12 dynamic — **live authenticated read** against the real JACE (`192.168.1.140:4911`). The pull
> used `save=false` (no `Station.saveSync()`, [Block 472] §472.3) → **read-only, no station mutation**.
> `live-install` → SECRETS DISCIPLINE (the `.dist` and every secret it contains stay in scratchpad; cited by
> sha256 + a structure-only manifest; NO body, key, keystore, PSK, or credential written to the corpus).
> **Block type: EVIDENCE (live, requires-execution close §19).**
> **§14 (live-refines):** the live handshake **pins the OS version and JVM** that [Block 459]/[Block 465]
> left general — **QNX 7.0.X** (they said only "QNX Neutrino") and **OpenJDK** (the doc they cited says "Oracle
> HotSpot") — noted back in both. A refinement/scope-clarify, not a refuted value.
> **Sources:** `[CERT-hw]` live backup pull this session (`sources/probes/B471-fox-client/`,
> `backup-manifest-structure.txt`) · `[CERT]` [Block 472] (mechanism), [Block 471] (login), [Block 134] (Fox).
>
> **Bottom line:** the no-Workbench `.bog` grab **works, proven live**. A ~230-line stdlib Python Fox client
> authenticated, opened the `backup` channel circuit, sent `{save=false}`, and received a **200,680-byte
> `.dist` ZIP** containing **`niagara_user_home/stations/JACE_UMBRELLA/config.bog` (7,367 bytes)** and 42
> other entries — the complete station engineering — over TLS, with station `admin`, no platform login, no
> Workbench.

## §473.1 — What was done (the implementation over B471/B472)

Added the **byte-accurate Fox circuit layer** ([Block 134] §134.9) to the B471 login client
(`sources/probes/B471-fox-client/niagara-fox-backup.py`):
1. login (SCRAM-SHA-256, [Block 471]) → authenticated;
2. read the server's **second hello** (the post-welcome identity frame — see §473.3);
3. `circuit`/`open` `{id=1, channel=backup, command=backup, metadata={}}`;
4. `circuit`/`stream` `{id=1, data=b:[ {save=z:f} ]}` — the read-only request ([Block 472] §472.3);
5. read inbound `circuit`/`stream` frames, concatenating the `data` FoxBlob bytes;
6. split the leading `resp` FoxMessage off the byte stream, take the remainder from the `PK\x03\x04` signature
   → the `.dist` ZIP.

The one non-obvious fix: the frame parser had to become **byte-accurate** and handle **FoxObject** (`o:<enc>
<len>[bytes]`) — the server's hello carries `sysInfo=o:bog …[…]` — not just FoxBlob; a line-based reader
breaks on the binary spans (exactly [Block 472] §472.5's warning).

## §473.2 — Live result (`[CERT-hw]`)

Exit 0, `FOX-BACKUP-OK`. The `.dist` (kept in scratchpad, **not** in the corpus — it carries live secrets):
- **200,680 bytes**, sha256 `805139cf23cbbae189fef0dfc9fd00e30b21eb6e5c8967db62b9f613e241e708`.
- **43 entries.** The station database is present: **`niagara_user_home/stations/JACE_UMBRELLA/config.bog`**,
  **7,367 bytes** — the actual `config.bog` pulled off the live JACE.
- Structure-only manifest (names + sizes, Host ID masked, **no bodies**) preserved as `[CERT-hw]` evidence:
  `sources/probes/B471-fox-client/backup-manifest-structure.txt` (sha256 `6df141ab…`).

The manifest confirms the on-disk layout documented earlier: `niagara_home/` (System Home) and
`niagara_user_home/` (User Home + `stations/<name>/config.bog`) — [Block 462] verified live; the security area
holds `security/licenses/db/<HostId>/{Webs,Honeywell,HoneywellCentraLine}.license`, `security/certificates/*.certificate`,
`security/keystore.jceks`, `security/cacerts.jceks`, and the `.kr` keyring — [Block 466]/[Block 467] confirmed
by structure.

## §473.3 — Incidental live identity (from the second hello) — §14 corrections

The server's post-welcome hello is a station fact-sheet (`[CERT-hw]`):

| Field | Live value | Note |
|---|---|---|
| `os.name` / `os.version` | **QNX / 7.0.X** | **§14 live-pins [Block 459]/[Block 465]** — they said only "QNX Neutrino"; live pins **QNX 7.0** |
| `vm.name` / `vm.version` | **OpenJDK Client VM / 25.412-b08** | **§14 refines [Block 459]** — the cited doc says "Oracle HotSpot"; the live VM is **OpenJDK** |
| `app.name` / `app.version` | Station / **4.14.0.162** | Niagara version, matches the supervisor corpus |
| `station.name` | **JACE_UMBRELLA** | the running station |
| `hostId` | **Qnx-TITAN-44A2-****-****-363E** | **closes J10-G1** — live Host ID; confirms the `Qnx-TITAN-XXXX-XXXX-XXXX-XXXX` format of [Block 467] (middle groups masked, SECRETS DISCIPLINE) |
| `brandId` / `timeZone` | Webs / America/Mexico_City | Honeywell WEB-8000 brand |
| `vmUuid`, `n4Id`, `n4SuperId` | (redacted) | ephemeral/session |

So the architecture answer sharpens: **QNX 7.0 + OpenJDK on the TITAN board**. The appliance identity is
now measured live, not left general or doc-only.

## §473.4 — SECRETS DISCIPLINE (how the invariant held)

- **Password** never transmitted (SCRAM) nor recorded; supplied out-of-band from scratchpad.
- **The `.dist` stays in scratchpad** — it was NOT copied to `sources/` or committed. It contains
  `keystore.jceks`, `cacerts.jceks`, the `.kr` keyring, host-bound `.license` files, and
  `wpa_supplicant_ti18xx.conf` (a WPA PSK) — all redaction-checklist items. Only its **sha256 + byte count**
  are cited.
- **The committed evidence is structure only** — a manifest of names + sizes with the Host ID masked; no file
  body, key, certificate, or PSK is in the corpus. `config.bog`'s sensitive fields are additionally
  machine-key/passphrase-encrypted at rest ([Block 466]).

## §473.5 — J8 verdict, now proven

[Block 464] predicted the station BackupService was the practical no-Workbench route "only the Fox client is
unbuilt." **Built, and executed live:** authenticated Fox session → `backup` channel → `config.bog` in hand,
read-only, with station admin alone. The two walls stand unchanged — the pulled `config.bog`'s secrets remain
**encrypted at rest** ([Block 466]); obtaining the file is not the same as reading its sealed secrets, and no
platform login or Tridium signature was needed for the *engineering* copy. J8 is demonstrated end-to-end.

## Self-verify

| # | Claim | Marker | Citation | Checked |
|---|---|---|---|---|
| 1 | Fox client pulled a .dist live (FOX-BACKUP-OK, exit 0) | [CERT-hw] | this session | ✓ |
| 2 | .dist = 200,680 B, sha256 805139cf… | [CERT-hw] | this session | ✓ sha256sum |
| 3 | config.bog present (JACE_UMBRELLA, 7,367 B) | [CERT-hw] | manifest | ✓ |
| 4 | read-only (save=false, no saveSync) | [CERT] | [Block 472] §472.3 | ✓ |
| 5 | os=QNX 7.0.X, vm=OpenJDK (live-refines B459/B465) | [CERT-hw] | 2nd hello | ✓ |
| 6 | live hostId = Qnx-TITAN-…363E (closes J10-G1) | [CERT-hw] | 2nd hello | ✓ format, masked |
| 7 | .dist kept out of corpus; only structure manifest committed | [CERT-hw] | this session | ✓ SECRETS DISCIPLINE |

Marker tally: [CERT-hw] ×5 · [CERT] ×2 (corpus) · [INFER] 0 load-bearing. **Block type: EVIDENCE (live
requires-execution close §19).** Ratio ≈ 0.

## Connections

- **[Block 471]** (login) + **[Block 472]** (mechanism) → this executes them. **[Block 462]/[Block 466]/[Block 467]**
  confirmed live by the .dist structure + 2nd hello. **[Block 459]/[Block 465]** **live-refined** (QNX 7.0/OpenJDK)
  — both carry a back-pointer here. **[Block 134]** — the Fox circuit layer implemented.

## Open gaps

**J10-G1 CLOSED** (live Host ID measured). Remaining child gaps: **J8-G3** (confirm bit-48 reachability for a
non-super role — `admin` passed; a lesser role is untested), J3-G1, J5-G1, J7-G1, J11-G1, J2-G1 (QNX mount
table — partially answered: the .dist paths confirm `niagara_home`/`niagara_user_home`). **J8 thread: DONE.**
Tool: `T: niagara-fox-backup.py · created · Fox circuit client, pulls a station .dist read-only (save=false).`
