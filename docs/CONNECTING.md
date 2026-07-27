# Connecting to the local Niagara station (PRUEBAS)

How an agent session reaches the local Honeywell OptimizerSupervisor station, what is
verified to work, and what does not. Credentials live in `.secrets` at the repo root
(git-ignored, mode 600) — never inline them in a command, a file, or a commit.

## The environment

| Item | Value | How it was established |
|---|---|---|
| Niagara home | `C:\Honeywell\OptimizerSupervisor-N4.14.0.162` | on disk |
| Station home | `C:\ProgramData\Niagara4.14\OptimizerSupervisor\stations\PRUEBAS` | on disk |
| Station URL | `https://localhost/` | verified |
| Station name | `PRUEBAS` | login page title: `PRUEBAS — Acceso` |
| Product | Niagara Framework 4 · Honeywell | login page footer |

Other Niagara installs exist on this machine (`Niagara4.15/Mercato`, `Niagara4.13`) — they
are NOT the target. The target is the 4.14 OptimizerSupervisor above.

## What the agent can actually reach

The agent runs shell commands inside WSL, not on Windows directly. That matters for
networking, and it was tested rather than assumed:

- `https://localhost/` from WSL → **HTTP 302 → `/login`**. Reachable. WSL localhost
  forwarding to the Windows host works here.
- `https://192.168.100.1/` (the WSL default-gateway address for the Windows host) → **no
  response**. Do not use the gateway IP; use `localhost`.

So: the agent can issue HTTP requests against the station. It cannot run Workbench, and it
has no native Fox client — anything requiring a Fox session or the Workbench UI has to be
done by the user.

## The authentication catch

Requesting a protected endpoint returns a redirect to the login form, **not** a
`WWW-Authenticate` header:

```
$ curl -sk -D- -o /dev/null https://localhost/obix/
HTTP/1.1 302 Found
Location: https://localhost/login
```

The login form posts `j_username` / `j_password` and loads `/login/core/auth.min.js`, which
means the active authentication scheme is the browser/SCRAM digest flow, not HTTP Basic.

**Consequence: a plain `curl -u user:pass` will NOT authenticate.** The station never offers
Basic, so curl has nothing to answer. This is the default N4 posture and it is correct
security-wise — it is not a misconfiguration to "fix" casually.

Two ways forward, in order of preference:

1. **Dedicated service user with HTTP Basic scheme.** In Workbench, create a user whose
   Authentication Scheme is `HTTPBasicScheme`, scoped to only the permissions the task needs.
   Basic over the existing HTTPS is acceptable; over plain HTTP it is not. Put that user's
   credentials in `.secrets` as `NIAGARA_USER` / `NIAGARA_PASS`. Do not reuse an admin account.
2. **User runs the command.** Prefix it with `!` in the session (e.g. `! curl ...`) so the
   output lands in the conversation. Nothing is stored, nothing is scripted.

For read-only data pulls, the `obix` driver is usually the cleanest surface once a Basic user
exists — it speaks plain XML over HTTP and needs no Fox session.

## Usage pattern

```bash
set -a; . ./.secrets; set +a
curl -sk -u "$NIAGARA_USER:$NIAGARA_PASS" "$NIAGARA_URL/obix/config/"
```

`set -a` exports the values so child processes inherit them; `set +a` stops that. Passing the
password as a curl argument puts it in the process list and in shell history — prefer
`--netrc-file` or `-u "$USER:$PASS"` from a sourced variable, as above, and never echo it.

## Deploying the PX menu to this station

The `deliverables/px-menu/` files are consumed by Workbench, not by HTTP. Copy `menu.px` to:

```
C:\ProgramData\Niagara4.14\OptimizerSupervisor\stations\PRUEBAS\px\menu.px
```

That path is what `ord="file:^px/menu.px"` resolves to — `^` means station home. Create the
`px` folder if it does not exist. Then open the host graphic in the PX editor and add the
trigger button. See `deliverables/px-menu/README.md`.

## Verified working

With a station user whose Authentication Scheme Name is `HTTPBasicScheme`:

```
/                302   (web UI still uses the form/digest flow — expected, not a fault)
/obix            200
/obix/config/    200
```

`GET /obix/config/` returns the station object: `stationName=PRUEBAS`, plus refs to
`Services/`, `Drivers/`, `Apps/` and a `save` op. So obix is the working read surface.

Note the split: HTTP Basic authenticates the **obix** servlet, but `/` keeps redirecting to
the login form because the web UI runs its own scheme. That is normal — do not "fix" it.

The station ships these schemes already (no need to create one):

```
DigestScheme       b:DigestAuthenticationScheme
AXDigestScheme     b:LegacyDigestAuthenticationScheme
HTTPBasicScheme    b:HTTPBasicAuthenticationScheme
HTTPBasicScheme1   b:HTTPBasicAuthenticationScheme
```

A new user defaults to `DigestScheme`; leaving it there is what produces a 302 on every
Basic request. Set it to `HTTPBasicScheme` explicitly.

## What `.secrets` does and does not protect

`.secrets` being git-ignored keeps credentials out of the repository history. That is its
only job. It does **not** hide the file from an agent session — the agent can read any file
in the working directory, and the harness surfaces file edits automatically.

If a credential must stay unseen by the agent, do not put it in a file under the project.
Run the command yourself (prefix `!` in the session) and share only the output.

## Not verified

- Whether the platform daemon is listening on 5011/3011 locally — not tested. The
  `NIAGARA_PLATFORM_*` entries in `.secrets` are untested placeholders.
