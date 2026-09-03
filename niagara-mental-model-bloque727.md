# Block 727 — `access-control` RUNBOOK: exposing an N4 station as an oBIX SERVER to an external oBIX client — the operator procedure (add `ObixNetwork` → enable the `Server` slot → bind a station user to `HTTPBasicScheme` → license `export`)

> **Focus:** `access-control`, **document-mode RUNBOOK** (METHODOLOGY §20), NOT a discovery gap. Sibling of the
> other operator runbooks the STOPPED `access-control` focus hosts: [B560] cloudflared remote access, [B726]
> SSH `-L` jump host. This one captures the on-station config a technician performs so a THIRD-PARTY oBIX client
> can read/write the station over `/obix`, and the authentication that guards it.
> **Angle:** the OPERATOR procedure + its security wiring — deliberately distinct from the code-internals angle
> already covered. Cite, do NOT re-derive: [B499] obixDriver client/transport internals; [B509] the `BObixServer`
> server surface + lobby map + license gate; [B600] the typed authenticated oBIX query surface; [B494]/[B510] the
> auth-scheme implementations + SPI.
> **Sources (all consulted):**
> - FUENTE 2 (Tridium official doc): `niagara-help/guides-clean/User/baja-HTTPBasicAuthenticationScheme.txt`;
>   `guides-clean/AXtoN4Migration/cN4migratedStationObixServerConsiderations.txt`;
>   `organized/docObix/docObix-doc/extracted/doc/obixDriver-ObixNetwork.html`;
>   `.../obixDriver-ObixServer.html`.
> - FUENTE 3 (code): `organized/baja/baja/vineflower/module.palette` L18–24 (scheme tree);
>   `organized/baja/baja/vineflower/javax/baja/user/BUser.java` (user↔scheme binding);
>   `organized/obixDriver/obixDriver-rt/vineflower/module.palette` (driver tree).
> - FUENTE 1 (corpus): [B499], [B509], [B600], [B494], [B510].
> READ-ONLY over the subject; no station was mutated. Markers per METHODOLOGY §3.

## §727.1 — When this runbook applies (direction matters)

The N4 `obixDriver` is bidirectional and the `ObixNetwork` container holds BOTH halves `[CERT]`
(`organized/obixDriver/obixDriver-rt/vineflower/module.palette`: `ObixNetwork` contains the client children
`R2ObixClient`/`ObixClient`/… AND a `server` folder with `ObixExport`). Pick the half by who initiates:

| Your case | The station is the… | What you configure | Auth role of HTTP Basic |
|---|---|---|---|
| **A 3rd-party oBIX client reads/writes YOUR station** (this runbook) | oBIX **server / host** | `ObixNetwork` + its `Server` slot; a station user bound to `HTTPBasicScheme` | the INCOMING client logs in as that station user |
| Your station pulls data FROM a remote oBIX server | oBIX **client** | `ObixNetwork` + an `ObixClient` device under it | credentials the REMOTE server demands (set on the `ObixClient`) |

Both start by adding the same `ObixNetwork`; they diverge in which child you use. §727.3–§727.6 cover the
**server** case. §727.7 is the one-paragraph pointer for the client case.

## §727.2 — Prerequisite: the `export` license feature `[CERT-doc]`/`[CERT]`

The oBIX **server** half is license-gated on the `export` sub-key of the `tridium:obixDriver` feature.
- Doc: enabling `ObixNetwork` lets "remote oBIX clients access the lobby of the station following login using
  station user credentials **(provided that the host platform's license has `export="true"`)**" `[CERT-doc]`
  (`obixDriver-ObixNetwork.html`, Enabled property).
- Code confirms the gate `[CERT]` (via [B509 §509.1]): `BObixServer.serviceStarted()` sets
  `licensed = getLicenseFeature().getb("export", false)` (`BObixServer.java:372`); an unlicensed server answers
  **HTTP 403 "Unlicensed oBIX Server"** (`:227`).

**Check first:** `Platform > License Manager` (or the running-install license file) → confirm the `obixDriver`
feature carries `export="true"`. Without it the network installs and enables, but every `/obix` request is
rejected 403. (The CLIENT half is gated by a separate `import` sub-key — not needed for this runbook.)

## §727.3 — Add the `ObixNetwork` to Drivers `[CERT]`/`[CERT-doc]`

`ObixNetwork` is the top-level driver container; "This network object is a Framework convention, and has no
physical correspondence to any oBIX system" `[CERT-doc]` (`obixDriver-ObixNetwork.html`). Type `od:ObixNetwork`
`[CERT]` (`obixDriver-rt/module.palette` L4). Add it with either method:

- **Palette (recommended):** `Window > Sidebars > Palette` → load **`obixDriver`** → drag **`ObixNetwork`** into
  `Config > Drivers`. Name it, OK.
- **New button:** double-click `Drivers` (Driver Manager view) → **New** → *Obix Network* → OK.

## §727.4 — Enable the network + verify the `Server` slot `[CERT-doc]`

1. Select the `ObixNetwork` → **`Enabled = true`** (default). Per the doc this is exactly what lets remote clients
   in `[CERT-doc]` (`obixDriver-ObixNetwork.html`). `Status {ok}` means licensed and polling; `{fault}` → read
   `Fault Cause` (a `{fault}` with the license missing is the §727.2 case).
2. The `Server` is a **frozen container slot UNDER the ObixNetwork** — you do not add it separately `[CERT-doc]`
   (`obixDriver-ObixServer.html`). Right-click `ObixNetwork` → `Views > AX Property Sheet` → expand `Server`.
   - `Servlet Name` is **read-only, fixed at `obix`** `[CERT-doc]`. The station therefore serves at:

     ```
     https://<station-host>/obix
     ```

     `GET /obix` returns the oBIX Lobby; `/obix/config/*` exposes the station tree; `/obix/watchService` the
     per-user live-value watches (surface detail in [B509 §509.2/§509.6], typed ops in [B600]).
   - `Debug` (default false) prints incoming/outgoing oBIX traffic to station stdout — use only while
     troubleshooting.

## §727.5 — Add `HTTPBasicScheme` and bind the station user (the auth core) `[CERT-doc]`/`[CERT]`

`BObixServer.service()` performs NO credential check itself — authentication is done by the N4 web tier upstream
and the authenticated `BUser` is handed in ([B509 §509.5] `[CERT]`). So guarding `/obix` = giving the login user
the right web-facing scheme. oBIX clients that cannot hold cookies need **HTTP Basic** `[CERT-doc]`
(`cN4migratedStationObixServerConsiderations.txt`: "must be authenticated using the HTTP Basic Scheme").

1. **Ensure the scheme exists.** Default location `[CERT-doc]` (`baja-HTTPBasicAuthenticationScheme.txt` L23):

   ```
   Config > Services > AuthenticationService > AuthenticationSchemes > WebServicesSchemes > HTTPBasicScheme
   ```

   Component tree `[CERT]` (`baja/module.palette` L18–24): `AuthenticationSchemes` (`b:UnrestrictedFolder`) →
   `WebServicesSchemes` (`b:AuthenticationSchemeFolder`) → `HTTPBasicScheme` (`b:HTTPBasicAuthenticationScheme`).
   If it was deleted: load the **`baja`** palette → drag *HTTP Basic Authentication Scheme* into
   `WebServicesSchemes`.

2. **Bind it to the client's login user, BY NAME.** In `Config > Services > UserService > <user>`, set the
   **`Authentication Scheme Name`** property to the scheme's component name, e.g. `HTTPBasicScheme`.
   This is per-user `[CERT-doc]` ("assigned on a per-user basis",
   `cN4migratedStationObixServerConsiderations.txt` L21) and resolved by NAME in code `[CERT]`:
   `BUser.authenticationSchemeName` (`BUser.java:225`) is looked up via
   `authnService.getAuthenticationScheme(getAuthenticationSchemeName())` (`BUser.java:606`). The string MUST match
   the component name under `WebServicesSchemes` exactly, or the login fails.
   - Give this user only the roles/categories the client actually needs — reads over the wide lobbies
     (`/obix/config`, `/obix/ord`) have **no per-object read ACL** ([B509 §509.5], `[CERT]`+`[INFER]`); the user's
     RBAC scope IS the export boundary. Writes are separately RBAC-gated.

## §727.6 — HTTPS is not optional for this scheme `[CERT-doc]`

HTTP Basic sends the username and password over the connection base64-encoded — encoding, **not** encryption
`[CERT-doc]` (`baja-HTTPBasicAuthenticationScheme.txt` L12–13: "the user name and password are sent over the
connection"). Corpus corroboration: the obixDriver's default lobby is `http://` and credentials are
"base64-in-the-clear unless the operator opts into HTTPS" ([B499] headline). Therefore:

- Serve `/obix` over **HTTPS/TLS only** (`WebService` → Https enabled, Http disabled or redirected). Point the
  external client at `https://<host>/obix`.
- Do NOT move interactive Workbench/browser users to HTTP Basic — leave them on Digest. HTTP Basic is only for the
  cookie-less machine client.

## §727.7 — Reverse direction (station AS oBIX client), one paragraph `[CERT]`

If instead your station must READ from a remote oBIX server: add the same `ObixNetwork`, then under it add an
**`ObixClient`** device (`od:ObixClient`, `[CERT]` `obixDriver-rt/module.palette` L10), set its URI + the
credentials the remote server requires, and pull points via `ObixProxyExt`. This half is gated by the license
`import` sub-key, not `export`. Client/transport internals are [B499]; not expanded here.

## Self-verify (METHODOLOGY §11)

| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | `ObixNetwork` (type `od:ObixNetwork`) is the top-level driver container holding both client children and a `server` folder | `[CERT]` | `obixDriver-rt/vineflower/module.palette` L4,42–44 |
| 2 | The oBIX server half needs license `export="true"`; unlicensed → HTTP 403 "Unlicensed oBIX Server" | `[CERT-doc]`+`[CERT]` | `obixDriver-ObixNetwork.html` Enabled prop; `BObixServer.java:372,227` (via [B509 §509.1]) |
| 3 | Enabling `ObixNetwork` lets remote clients access the lobby using station user credentials | `[CERT-doc]` | `obixDriver-ObixNetwork.html`, Enabled property text |
| 4 | `Server` is a frozen slot under `ObixNetwork`; its Servlet Name is read-only, fixed at `obix` → endpoint `/obix` | `[CERT-doc]` | `obixDriver-ObixServer.html` |
| 5 | `HTTPBasicScheme` default path is `AuthenticationService > AuthenticationSchemes > WebServicesSchemes`; tree types as stated | `[CERT-doc]`+`[CERT]` | `baja-HTTPBasicAuthenticationScheme.txt` L23; `baja/module.palette` L18–24 |
| 6 | HTTP Basic is assigned per-user via the user's `Authentication Scheme Name`, resolved by name in code | `[CERT-doc]`+`[CERT]` | `cN4migratedStationObixServerConsiderations.txt` L21; `BUser.java:225,606` |
| 7 | `BObixServer` does no credential check itself; auth is enforced by the upstream web tier | `[CERT]` | [B509 §509.5] |
| 8 | HTTP Basic transmits credentials base64 (not encrypted) → HTTPS required | `[CERT-doc]` | `baja-HTTPBasicAuthenticationScheme.txt` L12–13; corrob. [B499] |
| 9 | Wide oBIX lobbies have no per-object read ACL; the login user's RBAC scope is the export boundary | `[CERT]`+`[INFER]` | [B509 §509.5] |
| 10 | Reverse direction uses an `ObixClient` under the same network, gated by `import` | `[CERT]` | `obixDriver-rt/module.palette` L10; [B499] |

**Tally:** 10 claims — 4 `[CERT-doc]`, 2 `[CERT]`, 3 mixed `[CERT-doc]+[CERT]`/`[CERT]+[INFER]`, 0 unmarked. No
claim without a citation. Scope left OUT (named, per §8): the exact `GlobalPasswordConfiguration` sub-properties
of the scheme (see [B558] password policy); SOAP-variant mounts `/obix/soap` ([B509 §509.3]); the full lobby-agent
map and Watch lease mechanics ([B509 §509.2/§509.4]); TLS certificate provisioning for the HTTPS listener.

## Connections

- **[B509]** — the `BObixServer` internals this runbook drives: license gate, lobby map, no read ACL, per-user watches.
- **[B499]** — obixDriver client + shared transport; the `http://`-default / base64 credential warning.
- **[B600]** — the typed authenticated oBIX ops an external client can actually call over `/obix`.
- **[B494]/[B510]** — the auth-scheme implementations and the `BAuthenticationScheme` SPI `HTTPBasicScheme` is an instance of.
- **[B560]/[B726]** — sibling `access-control` operator runbooks (remote access via cloudflared / SSH `-L`).
- **[B558]** — password policy that `GlobalPasswordConfiguration` on this scheme feeds into.

## Gaps opened

- **B727-G1** (low) — the exact `GlobalPasswordConfiguration` additional properties surfaced ON the `HTTPBasicScheme`
  component (min length, complexity, expiry) are documented generically in `baja-GlobalPasswordConfiguration`; not
  transcribed here. Fold into [B558] if a per-scheme override matters.
- **B727-G2** (low) — HTTPS listener + server-certificate provisioning for the `/obix` endpoint (WebService TLS,
  cert store) is referenced but not proceduralized; candidate for a `signing-pki`/TLS runbook.
