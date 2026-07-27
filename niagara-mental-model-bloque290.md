# Block 290 — Reading a live station over HTTP: why Basic auth is ignored by default, and `config.bog` as an offline diagnostic surface

> **DOCUMENT-MODE block (METHODOLOGY §20)** — procedure genre. CAPTURES a working method for reading a
> running Niagara station programmatically, established during a live session. The evidence base is the
> session itself (§20: "the SESSION is the evidence"), preserved at
> `sources/probes/live-20260727T012800Z-station-pruebas-filespace-and-obix.txt`.
>
> **Target**: LIVE station `PRUEBAS` — OptimizerSupervisor N4.14.0.162, Honeywell, reachable at
> `https://localhost` from WSL on the same host.
>
> Markers: `[CERT-live]` empirical against the running station · `[CERT]` verbatim in local artifacts ·
> `[INFER]` derived. **Corpus language: ENGLISH.**
>
> **SECRETS DISCIPLINE (live-install)** — hard invariant honoured here: no credential values, hashes,
> keystore or certificate material appear in this block or in its probe. The service account is described
> STRUCTURALLY (which scheme it must carry, which permissions), never by name or secret. Credentials for
> the session lived in a git-ignored file outside the corpus and were passed to `curl` via a sourced
> environment variable, never in `argv` of a recorded command.
>
> Access/tooling layer. Connects [Block 289] (the file space this access complements).

---

## 290.1 — Reachability: `localhost` works, the WSL gateway address does not `[CERT-live]`

The agent shell runs in WSL; the station runs on the Windows host. Only one of the two obvious addresses
answers:

```
$ curl -sk -o /dev/null -w 'http=%{http_code} time=%{time_total}s\n' --max-time 6 https://localhost/
http=302 time=0.026527s

$ curl -sk -o /dev/null -w 'http=%{http_code} time=%{time_total}s\n' --max-time 6 https://192.168.100.1/
http=000 time=0.000000s
```

`192.168.100.1` is the WSL default gateway (the Windows host as seen from the WSL NAT). It does not serve
the station. WSL's `localhost` forwarding is what carries the request. `[CERT-live]`

Identity confirmation — the 302 chain leads to a login page that names the station: `[CERT-live]`

```
Location: https://localhost/ord
<title>PRUEBAS - Acceso</title>
Niagara Framework 4 - Honeywell
```

The response also carries a strict CSP and `Strict-Transport-Security: max-age=63072000`, i.e. the web
stack is the hardened N4 default. `[CERT-live]`

## 290.2 — Why `curl -u` silently fails against a default N4 station `[CERT-live]`

A protected endpoint does NOT answer `401` with a `WWW-Authenticate` challenge. It answers `302` to the
login form:

```
$ curl -sk -D- -o /dev/null 'https://localhost/obix/'
HTTP/1.1 302 Found
Location: https://localhost/login

$ curl -sk -D- -o /dev/null 'https://localhost/ord'
HTTP/1.1 302 Found
Location: https://localhost/login
```

The login page posts `j_username`/`j_password` and loads `/login/core/auth.min.js` — the browser-side
SCRAM/digest handshake. `[CERT-live]`

**The failure mode is the important part**: because the server never OFFERS Basic, `curl -u` has nothing to
respond to. The `Authorization: Basic` header a client sends unprompted is simply ignored, and the client
sees a redirect that looks like "wrong password". It is not a wrong password and it is not a
misconfiguration — it is the default N4 posture. `[INFER]`

A user created in Workbench inherits `DigestScheme` unless told otherwise, which is what produces this. The
station already ships the scheme instances needed to change it — none has to be created: `[CERT]`

```
authenticationSchemes  b:AuthenticationSchemeFolder
DigestScheme           b:DigestAuthenticationScheme
AXDigestScheme         b:LegacyDigestAuthenticationScheme
HTTPBasicScheme        b:HTTPBasicAuthenticationScheme
HTTPBasicScheme1       b:HTTPBasicAuthenticationScheme
```
(read from `config.bog`, §290.4)

## 290.3 — With `HTTPBasicScheme`, obix answers — and the web UI still does not `[CERT-live]`

Setting a station user's **Authentication Scheme Name** to `HTTPBasicScheme` changes the picture, but not
uniformly:

| Endpoint | Default (`DigestScheme`) | With `HTTPBasicScheme` |
|---|---|---|
| `/` | 302 → `/login` | **302 → `/login`** |
| `/obix` | 403 | **200** |
| `/obix/config/` | 403 | **200** |

`/` staying at 302 is correct and must not be "fixed": Basic authenticates the **obix servlet**; the web UI
runs its own scheme. They are separate surfaces. `[INFER]` Weakening the UI's scheme to make `/` accept
Basic would trade the hardened default for nothing — obix already provides the read surface.

The authenticated payload, confirming a real read rather than a redirect: `[CERT-live]`

```xml
<obj href="https://localhost/obix/config/" is="/obix/def/baja:Station" display="Station">
 <str name="stationName" val="PRUEBAS" href="stationName/" displayName="Station Name"/>
 <ref name="Services" href="Services/" is="/obix/def/baja:ServiceContainer"/>
 <op  name="save"     href="save/"     displayName="Save"/>
 <ref name="Drivers"  href="Drivers/"  is="/obix/def/driver:DriverContainer"/>
 <ref name="Apps"     href="Apps/"     is="/obix/def/app:AppContainer"/>
</obj>
```

obix mirrors the component tree, so `/obix/config/Drivers/` enumerates the driver containers directly —
observed here: `NiagaraNetwork`, `BacnetNetwork`, `SnmpNetwork`, `ObixNetwork`,
`AbstractMqttDriverNetwork`, plus two `baja:Folder`s (`CODIGOS`, `PRUEBAS`). `[CERT-live]`

**Minimal-privilege note** (METHODOLOGY §12 / SECRETS DISCIPLINE): the account driving this should be a
dedicated service user with only the permissions the reads require, never a reused admin, and Basic is
acceptable ONLY over the station's existing HTTPS. `[INFER]`

## 290.4 — `config.bog` is a ZIP: station structure without Workbench `[CERT]`

The station database is a ZIP wrapping a single `file.xml`:

```
$ file <station>/config.bog
Zip archive data, at least v2.0 to extract, compression method=deflate
$ unzip -o -q config.bog -d cfg_x    ->    cfg_x/file.xml
```

That makes the component tree greppable offline — useful precisely when HTTP access is what is broken.
Structure observed under `t="b:UserService"` (names of principals only, no credential material):
`defaultPrototype` (prototype), `admin`, `BACnet`, `test`. `[CERT]` The scheme folder quoted in §290.2 comes
from the same file.

**Caveat that cost a wrong conclusion in-session**: `config.bog` is the ON-DISK state. A user created in
Workbench without a station **Save** is absent from it while the RUNTIME already honours it. That was
observed directly — the service account authenticated over HTTP while not appearing in the file, whose
mtime was a day old. `[CERT-live]` So `config.bog` proves what the station was last SAVED as, never what it
currently IS. For the running state, ask the station (§290.3).

A second in-session error worth recording: a first pass read only the leading segment of the `UserService`
subtree and reported "only `admin` exists". Partial extraction of a large XML is a silent-truncation trap —
scan the whole subtree or state the bound. `[CERT-live]`

## 290.5 — Self-verify

| Claim | Evidence | Marker |
|---|---|---|
| `https://localhost/` answers 302 from WSL | `http=302 time=0.026527s` | `[CERT-live]` |
| WSL gateway `192.168.100.1` does not answer | `http=000` | `[CERT-live]` |
| The responder is station `PRUEBAS`, N4 Honeywell | login `<title>` + footer | `[CERT-live]` |
| Protected endpoints 302 to `/login`, no `WWW-Authenticate` | headers of `/obix/`, `/ord` | `[CERT-live]` |
| Login flow is form + `auth.min.js` (SCRAM/digest) | `j_username`, `j_password`, `/login/core/auth.min.js` | `[CERT-live]` |
| ⇒ an unprompted Basic header is ignored | derived from the two above | `[INFER]` |
| Four scheme instances already exist in the station | `config.bog` `AuthenticationSchemeFolder` | `[CERT]` |
| With `HTTPBasicScheme`: `/obix` and `/obix/config/` = 200 | before/after table | `[CERT-live]` |
| `/` remains 302 under Basic | same table | `[CERT-live]` |
| ⇒ obix servlet and web UI are separate auth surfaces | derived | `[INFER]` |
| obix returns the station object with `stationName=PRUEBAS` | XML body | `[CERT-live]` |
| `/obix/config/Drivers/` enumerates 5 networks + 2 folders | XML body | `[CERT-live]` |
| `config.bog` is a ZIP containing `file.xml` | `file` output + `unzip` | `[CERT]` |
| Principals on disk: `admin`, `BACnet`, `test` (+ prototype) | `t="b:User"` scan | `[CERT]` |
| A Workbench-created user absent from `config.bog` still authenticates | the service account did | `[CERT-live]` |
| ⇒ `config.bog` = last SAVED state, not running state | derived | `[INFER]` |
| Partial XML extraction produced a wrong user list in-session | first pass reported "only admin" | `[CERT-live]` |
| Minimal-privilege service account, Basic only over HTTPS | practice, not measurement | `[INFER]` |

Tally: **[CERT] 3 / [CERT-live] 10 / [INFER] 5.** Zero secret values recorded.

---

## 290.x — Connections and open gaps

- **[Block 289]** — the file space; this block is its access-side companion. Together they cover "reach the
  station" and "know where its files are".
- **[Block 289] §289.4** — identifying the running station by `config.bog.lock` uses the same on-disk
  reasoning as §290.4, with the same disk-vs-runtime caveat.
- **B289-G1** (the pathbar `UnknownSchemeException`) remains open and is naming, not access: obix reached
  the same station fine.

### Open gaps

| ID | Gap | Class |
|---|---|---|
| **B290-G1** | Whether the obix servlet can be reached with the DIGEST scheme from a non-browser client (implementing the SCRAM handshake), avoiding a Basic-scheme account altogether. Not attempted. | DYNAMIC |
| **B290-G2** | The `obix` write surface (`op name="save"`, writable points) was NOT exercised — reads only. Any write against a live station falls under the §12 LIVE-WRITE recipe and the `⚠ CONFIG MUTATION` block label. | DYNAMIC |
