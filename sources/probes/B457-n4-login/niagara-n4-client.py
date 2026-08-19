#!/usr/bin/env python3
"""
niagara-n4-client.py — authenticated API client for a Niagara N4 station you own.

Niagara N4's default "Digest" auth scheme is NOT HTTP Digest (RFC 7616) — it is
SCRAM-SHA-256 over the web login servlet. A plain HTTP Digest/Basic client cannot
log in. This tool implements N4's real login handshake with VALID credentials
(the same protocol Workbench and the browser use), then lets you GET/POST any
station endpoint (oBIX, BQL, etc.) to READ your data.

It authenticates normally with your own credentials; it does not bypass auth.

Login flow (reverse-engineered from /login/core/auth.min.js, verified live on 4.14):
  1. GET  /prelogin                         (seed the session cookie)
  2. POST /prelogin/j_security_check/  action=sendClientFirstMessage   -> server-first (r,s,i)
  3. POST /prelogin/j_security_check/  action=sendClientFinalMessage   -> server-final (v=), verified
  4. POST /prelogin/j_security_check/  action=acceptEula               (commit past the EULA gate)
  -> session authenticated (JSESSIONID + niagara_userid); request any path.

Content-Type for the handshake posts: application/x-niagara-login-support
Hash: SCRAM-SHA-256 by default (the "scram-sha512" label in the JS is a misnomer;
sjcl defaults to sha256). Use --sha512 if a given station actually uses SHA-512.

Standard library only — no pip install.

Examples:
  # verify + read station identity (password prompted, self-signed TLS):
  niagara-n4-client.py -u API2 -k https://localhost /obix/about

  # password via env, save output:
  N4_PW='secret' niagara-n4-client.py -u API2 -k -o about.xml https://localhost /obix/about
"""
import argparse, os, sys, ssl, getpass, base64, hashlib, hmac, unicodedata
import urllib.request, urllib.error, http.cookiejar

LOGIN_ENDPOINT = "/prelogin/j_security_check/"
LOGIN_CT = "application/x-niagara-login-support"


def _uprep(u):
    u = unicodedata.normalize("NFKC", u)
    return u.replace("=", "=3D").replace(",", "=2C")


def _xor(a, b):
    return bytes(x ^ y for x, y in zip(a, b))


class N4Client:
    def __init__(self, host, insecure=False, timeout=20.0):
        self.host = host.rstrip("/")
        self.timeout = timeout
        self.cj = http.cookiejar.CookieJar()
        handlers = [urllib.request.HTTPCookieProcessor(self.cj)]
        if insecure:
            ctx = ssl.create_default_context()
            ctx.check_hostname = False
            ctx.verify_mode = ssl.CERT_NONE
            handlers.append(urllib.request.HTTPSHandler(context=ctx))
        self.op = urllib.request.build_opener(*handlers)

    def _req(self, path, method="GET", body=None, ct=None):
        headers = {"Content-Type": ct} if ct else {}
        req = urllib.request.Request(
            self.host + path,
            data=(body.encode() if body is not None else None),
            method=method, headers=headers)
        try:
            r = self.op.open(req, timeout=self.timeout)
            return r.status, r.geturl(), r.read()
        except urllib.error.HTTPError as e:
            return e.code, self.host + path, e.read()

    def _post_login(self, body):
        return self._req(LOGIN_ENDPOINT, "POST", body, LOGIN_CT)

    def _scram(self, user, pw, algo):
        digestmod = algo
        H = lambda m: hashlib.new(digestmod, m).digest()
        HM = lambda k, m: hmac.new(k, m, digestmod).digest()
        cnonce = base64.b64encode(os.urandom(18)).decode().replace(",", "")
        cfb = f"n={_uprep(user)},r={cnonce}"
        _, _, sf = self._post_login(
            "action=sendClientFirstMessage&clientFirstMessage=n,," + cfb)
        sf = sf.decode("utf-8", "replace").strip()
        if "s=" not in sf or "r=" not in sf:
            return False, f"client-first rejected: {sf[:80]}"
        m = {k: v for k, v in (p.split("=", 1) for p in sf.split(","))}
        salt = base64.b64decode(m["s"]); it = int(m["i"]); rnonce = m["r"]
        if not rnonce.startswith(cnonce):
            return False, "server nonce does not extend client nonce"
        sp = hashlib.pbkdf2_hmac(digestmod, unicodedata.normalize("NFKC", pw).encode(),
                                 salt, it, hashlib.new(digestmod).digest_size)
        ck = HM(sp, b"Client Key"); sk = H(ck)
        cfwp = f"c=biws,r={rnonce}"
        authmsg = f"{cfb},{sf},{cfwp}".encode()
        proof = base64.b64encode(_xor(ck, HM(sk, authmsg))).decode()
        _, _, fin = self._post_login(
            "action=sendClientFinalMessage&clientFinalMessage=" + cfwp + ",p=" + proof)
        fin = fin.decode("utf-8", "replace").strip()
        if not fin.startswith("v="):
            return False, f"client-final rejected (bad password?): {fin[:80]}"
        # verify the server signature (mutual auth)
        serverkey = HM(sp, b"Server Key")
        expected = base64.b64encode(HM(serverkey, authmsg)).decode()
        if fin[2:] != expected:
            return False, "server signature mismatch"
        return True, "ok"

    def login(self, user, pw, algo="sha256", accept_eula=True):
        self._req("/prelogin")  # seed session
        ok, msg = self._scram(user, pw, algo)
        if not ok and algo == "sha256":
            # some stations use the sha512 variant; retry once on a fresh session
            self.cj.clear()
            self._req("/prelogin")
            ok, msg = self._scram(user, pw, "sha512")
            if ok:
                algo = "sha512"
        if not ok:
            return False, msg
        if accept_eula:
            self._post_login("action=acceptEula")
        return True, f"authenticated (scram-{algo})"

    def request(self, path, method="GET", body=None, ct=None):
        return self._req(path, method, body, ct)


def main():
    ap = argparse.ArgumentParser(description="Authenticated Niagara N4 (SCRAM) API client for a station you own.")
    ap.add_argument("host", help="base URL, e.g. https://localhost")
    ap.add_argument("path", nargs="?", default="/obix/about", help="path to request after login (default /obix/about)")
    ap.add_argument("-u", "--user", required=True)
    ap.add_argument("-p", "--password", help="password (else $N4_PW, else prompt)")
    ap.add_argument("-X", "--method", default=None)
    ap.add_argument("-d", "--data", default=None, help="request body")
    ap.add_argument("--ct", default=None, help="Content-Type for -d")
    ap.add_argument("-k", "--insecure", action="store_true", help="skip TLS verification")
    ap.add_argument("-o", "--output", help="write body to file")
    ap.add_argument("--sha512", action="store_true", help="force SCRAM-SHA-512")
    ap.add_argument("--no-eula", action="store_true", help="skip the acceptEula commit step")
    ap.add_argument("--timeout", type=float, default=20.0)
    args = ap.parse_args()

    pw = args.password or os.environ.get("N4_PW")
    if pw is None:
        pw = getpass.getpass(f"Password for {args.user}: ")

    c = N4Client(args.host, insecure=args.insecure, timeout=args.timeout)
    ok, msg = c.login(args.user, pw, algo=("sha512" if args.sha512 else "sha256"),
                      accept_eula=not args.no_eula)
    sys.stderr.write(f"[login] {msg}\n")
    if not ok:
        return 1

    method = args.method or ("POST" if args.data is not None else "GET")
    status, final, body = c.request(args.path, method, args.data, args.ct)
    sys.stderr.write(f"[{method} {args.path}] HTTP {status}  final={final}\n")
    if "prelogin" in final:
        sys.stderr.write("[warn] redirected to login — not authorized for this path or session lost\n")
    if args.output:
        open(args.output, "wb").write(body)
        sys.stderr.write(f"[{len(body)} bytes -> {args.output}]\n")
    else:
        sys.stdout.buffer.write(body)
    return 0


if __name__ == "__main__":
    sys.exit(main())
