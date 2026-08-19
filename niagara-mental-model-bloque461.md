# B461 — Accessing the JACE-8000 station: SCRAM login → the bajaux ORD navigator browses the whole component tree; the module set is minimal (no oBIX/Hx/help) (focus jace8000, J4)

> **Focus:** `jace8000` (§16). **Gap:** J4 — how do you actually get into and read the station on a JACE?
> **Phase:** §12 dynamic (live at `192.168.1.140`, admin). Read-only. `live-install` → SECRETS DISCIPLINE.
> **Sources:** `[CERT-live]` authenticated station probes this session · `[CERT]` corpus [Block 457]/[Block 458]
> (SCRAM login + tooling) · [Block 459]/[Block 460] (this focus).
>
> **Bottom line:** with valid station credentials you log in with **SCRAM-SHA-256** (not HTTP Digest), and the
> live web UI is the **bajaux ORD navigator** at `/ord/station:|slot:/…`. As `admin` that navigator walks the
> **entire station component tree** — `Config → Services → PlatformServices → …` — so you can *read the whole
> station over HTTPS* without Workbench. This JACE runs a **minimal module set**: no oBIX, no Hx, no help
> (`/obix`, `/hx`, `/help` → 404), and the raw file/Workbench servlets are gated (`/file`, `/bajaux`, `/wb`
> → 403).

## §461.1 — Getting in: SCRAM-SHA-256, confirmed live

- `[CERT-live]` Login with `admin` over `https://192.168.1.140` succeeds via the N4 SCRAM handshake — the
  `api-access` tool reported `authenticated (scram-sha256)`. N4's "Digest" scheme is **SCRAM-SHA-256 over the
  login servlet**, NOT RFC-7616 HTTP Digest ([Block 457]); a plain Basic/Digest client cannot log in.
- The reusable client is `sources/probes/B457-n4-login/niagara-n4-client.py` (stdlib only). Credentials are
  supplied out-of-band (SECRETS DISCIPLINE); this block records the *method*, never the password.

## §461.2 — The live web surface (measured as `admin`)

| Path | Code | Meaning |
|---|---|---|
| `/`, `/ord`, `/login/`, `/prelogin` | **200** → `/ord/station:%7Cslot:/` | all land on the bajaux ORD navigator |
| `/ord/station:\|slot:/` | 200, title **"Config"** | station root component = `Config` |
| `/ord/station:\|slot:/Services` | 200, title "Services" | the Services container browses |
| `/ord/station:\|slot:/Services/PlatformServices` | 200, title "PlatformServices" | platform-service node visible **inside** the station |
| `/file/`, `/file/^` | **403** | file servlet present but directory access gated (J5) |
| `/bajaux/`, `/wb/` | **403** | Workbench-profile servlets not served to a browser |
| `/obix/`, `/obix/about` | **404** | **oBIX module not installed** (differs from B457/B458's station) |
| `/hx/` | 404 | no Hx (legacy HTML) profile module |
| `/help/`, `/about`, `/system` | 404 | no help module; no bare system servlet |

Two things stand out:

1. **The ORD navigator is a full read channel.** `station:|slot:/…` is the Niagara ORD (Object Resolution
   Descriptor) addressing scheme; the bajaux navigator resolves any slot path an admin may see. Walking
   `Config → Services → PlatformServices` proves the whole component tree is browsable over HTTPS — this is
   how you *read* a JACE with no Workbench and no oBIX.
2. **The module set is minimal.** oBIX, Hx, and help are absent. So the machine-friendly data API this focus's
   sibling `api-access` used (`/obix/...`) is **not available here** — data extraction on this JACE must go
   through the ORD/bajaux navigator or the Fox channel, not oBIX. (If oBIX is wanted, the `obix` module must
   be installed via the platform Software Manager — a J3/J8 platform action.)

## §461.3 — The engineering channel: Fox on :4911 (TLS)

- `[CERT-live]` **:4911 open, :1911 (plaintext Fox) closed** ([Block 459]) — Fox is available only TLS-wrapped
  (`foxs`), presenting the `ForRecoveryPurposes` cert. Fox is the protocol **Workbench** uses to engineer the
  station (component CRUD, subscriptions, BQL, station copier). The web ORD navigator and Fox reach the same
  component space; Fox is the richer, bidirectional one.
- Plaintext Fox being off is good posture: engineering traffic is not exposed in the clear.

## §461.4 — The bridge that matters for J8: PlatformServices inside the station

`Services/PlatformServices` resolving over the *station* channel (200) is the load-bearing find: **some
platform information and actions are reachable from inside the station**, not only through the separate
platform daemon (:3011/:5011, [Block 460]). This is why an admin-authenticated *station* session — not a
platform login — may be enough to trigger a **station backup** and read platform metadata, which is one of the
two candidate routes to obtain a `.bog` without Workbench (J8). It does not grant OS-level file transfer
(that is the platform daemon's job), but it is a second, station-side lever.

## §461.5 — Access summary (how to read a JACE station, no Workbench)

1. **Authenticate** — SCRAM-SHA-256 via the login servlet ([Block 457] tool).
2. **Navigate** — `GET /ord/station:|slot:/<path>` walks the component tree (bajaux).
3. **Read data** — here, via ORD/bajaux (oBIX is absent); on an oBIX-enabled station, `/obix/...` ([Block 458]).
4. **Engineer** — Fox on :4911 (TLS) for full component CRUD / BQL / backup (Workbench or a Fox client).
5. **Platform-level** — the separate daemon (:3011/:5011, [Block 460]); some platform nodes are also visible
   read-side inside the station (`Services/PlatformServices`).

## Self-verify

| # | Claim | Marker | Citation | Checked |
|---|---|---|---|---|
| 1 | admin logs in via SCRAM-SHA-256 live | [CERT-live] | this session | ✓ "authenticated (scram-sha256)" |
| 2 | `/` and `/ord` → 200 to `/ord/station:\|slot:/` (bajaux navigator) | [CERT-live] | this session | ✓ |
| 3 | station root component titled "Config"; Services/PlatformServices browse (200) | [CERT-live] | this session | ✓ titles |
| 4 | oBIX/Hx/help absent (404); /file, /bajaux, /wb gated (403) | [CERT-live] | this session | ✓ |
| 5 | Fox :4911 TLS open, :1911 plaintext closed | [CERT-live] | [Block 459] | ✓ |
| 6 | N4 "Digest" = SCRAM-SHA-256, not RFC-7616 | [CERT] | [Block 457] | ✓ corpus |

Marker tally: [CERT-live] ×5 · [CERT] ×3 (corpus) · [INFER] 0 load-bearing. **Block type: EVIDENCE (live).**
Ratio ≈ 0.

## Connections

- **[Block 457]/[Block 458]** (`api-access`) — the SCRAM login + oBIX tooling; J4 shows the oBIX half does not
  apply here (module absent) while the login half transfers directly.
- **[Block 459]** — architecture/ports; **[Block 460]** — the platform daemon (the *other* front door).
- Forward: **J5** (why `/file` is 403 and the real filesystem routes), **J8** (station-side backup as a
  `.bog` route).

## Open gaps

Queued: J5, J7, J8, J2, J6, J9, J10, J11, J3-G1. New child: **J4-G1** — enumerate what `admin` can *write*
via Fox/ORD (component set, station backup trigger) — folded into J8.
