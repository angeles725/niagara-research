#!/usr/bin/env python3
"""
niagara-fox-client.py — a minimal Niagara N4 Fox (foxs/TLS) client for a station you own.

Speaks the Fox wire protocol (documented in corpus B134) directly over TLS on port 4911:
  frame envelope  fox <type> <seq> <reply> <channel> <command>\n{ body };;\n
  body (FoxMessage)  { \n name=<T>:<value>\n ... }
  login = hello -> kerberos -> username -> challenge -> authMessage1/2 (SCRAM-SHA-256) -> welcome

The SCRAM-SHA-256 digest is byte-identical to the web login (corpus B457); only the transport
differs (Fox tuning frames vs the HTTP login servlet). This authenticates with YOUR OWN
credentials by the real protocol Workbench uses — it does not bypass auth.

Stdlib only. Credentials come from $N4_PW / prompt (never argv). Verbose frame logging to stderr.

Usage:
  N4_PW=... niagara-fox-client.py <host> -u <user> [--port 4911] [--login-only]
"""
import argparse, os, sys, ssl, socket, getpass, base64, hashlib, hmac, unicodedata

# ---- SCRAM-SHA-256 (identical algorithm to niagara-n4-client.py / B457) ----
def _uprep(u):
    u = unicodedata.normalize("NFKC", u)
    return u.replace("=", "=3D").replace(",", "=2C")

def _xor(a, b):
    return bytes(x ^ y for x, y in zip(a, b))

class Scram:
    def __init__(self, user, pw):
        self.user = user; self.pw = pw
        self.cnonce = base64.b64encode(os.urandom(16)).decode()
        self.cfb = f"n={_uprep(user)},r={self.cnonce}"
        self.client_first = "n,," + self.cfb
    def client_final(self, server_first):
        m = {k: v for k, v in (p.split("=", 1) for p in server_first.split(","))}
        salt = base64.b64decode(m["s"]); it = int(m["i"]); rnonce = m["r"]
        if not rnonce.startswith(self.cnonce):
            raise ValueError("server nonce does not extend client nonce")
        sp = hashlib.pbkdf2_hmac("sha256", unicodedata.normalize("NFKC", self.pw).encode(), salt, it, 32)
        ck = hmac.new(sp, b"Client Key", hashlib.sha256).digest()
        sk = hashlib.sha256(ck).digest()
        cfwp = f"c=biws,r={rnonce}"
        authmsg = f"{self.cfb},{server_first},{cfwp}".encode()
        proof = _xor(ck, hmac.new(sk, authmsg, hashlib.sha256).digest())
        self._srvkey = hmac.new(sp, b"Server Key", hashlib.sha256).digest()
        self._authmsg = authmsg
        return f"{cfwp},p=" + base64.b64encode(proof).decode()
    def verify_server(self, server_final):
        exp = base64.b64encode(hmac.new(self._srvkey, self._authmsg, hashlib.sha256).digest()).decode()
        return server_final.split("v=", 1)[-1] == exp

# ---- Fox wire codec (B134) ----
def esc(s):  # writeSafe: printable ASCII except '#' pass; else #<hex>;
    out = []
    for ch in s:
        o = ord(ch)
        if 0x20 <= o <= 0x7f and ch != '#':
            out.append(ch)
        else:
            out.append("#%x;" % o)
    return "".join(out)

def build_body(tuples):
    # tuples: list of (name, typechar, value_str)
    lines = ["{"]
    for name, t, v in tuples:
        lines.append(f"{name}={t}:{v}")
    return "\n".join(lines) + "\n}"   # {\n t1\n t2\n }

class Fox:
    TYPES = {'s':115,'a':97,'r':114,'e':101,'n':110,'k':107,'c':99}
    def __init__(self, host, port=4911, verbose=True):
        self.verbose = verbose
        raw = socket.create_connection((host, port), timeout=15)
        ctx = ssl.create_default_context()
        ctx.check_hostname = False; ctx.verify_mode = ssl.CERT_NONE
        self.s = ctx.wrap_socket(raw, server_hostname=host)
        self.f = self.s.makefile("rwb")
        self.seq = 1
    def log(self, d, txt):
        if self.verbose:
            for ln in txt.splitlines():
                sys.stderr.write(f"  {d} {ln}\n")
    def send(self, ftype, channel, command, tuples):
        body = build_body(tuples)
        frame = f"fox {ftype} {self.seq} -1 {channel} {command}\n{body};;\n"
        self.log(">>", frame.rstrip("\n"))
        self.f.write(frame.encode()); self.f.flush()
        self.seq += 1
    def send_tuning(self, command, tuples):
        self.send('a', 'fox', command, tuples)
    def read_frame(self):
        # returns (ftype, seq, reply, channel, command, {name:(type,value)})
        while True:
            header = self.f.readline().decode("utf-8", "replace").rstrip("\n")
            if header == "":
                raise EOFError("connection closed")
            if not header.startswith("fox "):
                # could be leftover; skip
                continue
            parts = header.split(" ")
            ftype, seq, reply, channel, command = parts[1], parts[2], parts[3], parts[4], (parts[5] if len(parts) > 5 else "")
            if ftype == 'k':   # keepalive: no body, skip
                self.log("<<", header + "  (keepalive skipped)")
                continue
            break
        body = {}
        line = self.f.readline().decode("utf-8", "replace").rstrip("\n")  # expect '{'
        collected = [header, line]
        if line.strip() == "{" or line.strip().startswith("{"):
            while True:
                line = self.f.readline().decode("utf-8", "replace").rstrip("\n")
                collected.append(line)
                if line.startswith("}"):   # '};;' terminator
                    break
                if "=" in line and ":" in line:
                    name, rest = line.split("=", 1)
                    t, v = rest.split(":", 1)
                    body[name] = (t, v)
        self.log("<<", "\n".join(collected))
        return ftype, seq, reply, channel, command, body

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("host")
    ap.add_argument("-u", "--user", required=True)
    ap.add_argument("--port", type=int, default=4911)
    ap.add_argument("--login-only", action="store_true")
    ap.add_argument("-q", "--quiet", action="store_true")
    args = ap.parse_args()
    pw = os.environ.get("N4_PW") or getpass.getpass(f"Password for {args.user}: ")

    fx = Fox(args.host, args.port, verbose=not args.quiet)
    sys.stderr.write(f"[fox] TLS connected {args.host}:{args.port}\n")

    # 1. hello
    fx.send_tuning("hello", [
        ("fox.version", "s", "1.0.2"),
        ("id", "i", "0"),
        ("n4Id", "s", "0"),
        ("hostName", "s", esc("research")),
        ("hostAddress", "s", "127.0.0.1"),
        ("app.name", "s", "FoxProbe"),
    ])
    # 2. server hello
    _,_,_,_,cmd,hello = fx.read_frame()
    sys.stderr.write(f"[fox] server hello: fox.version={hello.get('fox.version')}\n")
    # 3. kerberos
    _,_,_,_,cmd,kb = fx.read_frame()
    sys.stderr.write(f"[fox] kerberos frame: {cmd} useKerberos={kb.get('useKerberos')}\n")
    # 4. username
    fx.send_tuning("username", [("username", "s", esc(args.user)), ("kerbKey", "z", "f")])
    # 5. challenge (loop once)
    _,_,_,_,cmd,ch = fx.read_frame()
    if cmd != "challenge":
        sys.stderr.write(f"[fox] expected challenge, got {cmd}: {ch}\n"); return 2
    method = ch.get("method", ("s",""))[1]
    sys.stderr.write(f"[fox] challenge method={method} keyExchangeMethods={ch.get('keyExchangeMethods')}\n")
    # 6. keyExchange response if the server offered methods (on TLS = null bundle)
    if "keyExchangeMethods" in ch:
        kem = ch["keyExchangeMethods"][1]
        # echo the null bundle name (first token); on TLS no KDF bundle is chosen
        null_name = kem.split(":")[0]
        fx.send_tuning("clientKeyExchangeMethod", [("keyExchangeMethod", "s", null_name)])
        sys.stderr.write(f"[fox] sent clientKeyExchangeMethod={null_name}\n")
    # 7. SCRAM authMessage1
    sc = Scram(args.user, pw)
    fx.send_tuning("authMessage1", [("authInput", "s", "authInputScram"),
                                    ("authHandshake1", "s", esc(sc.client_first))])
    _,_,_,_,_,am1 = fx.read_frame()
    server_first = am1.get("authHandshake1", ("s",""))[1]
    if not server_first or "r=" not in server_first:
        sys.stderr.write(f"[fox] authMessage1 reply missing serverFirst: {am1}\n"); return 3
    # 8. SCRAM authMessage2
    client_final = sc.client_final(server_first)
    fx.send_tuning("authMessage2", [("authHandshake2", "s", esc(client_final))])
    _,_,_,_,_,am2 = fx.read_frame()
    server_final = am2.get("authHandshake2", ("s",""))[1]
    ok_srv = sc.verify_server(server_final) if server_final else False
    # 9. welcome
    _,_,_,_,cmd,wel = fx.read_frame()
    if cmd == "welcome":
        sys.stderr.write(f"[fox] *** AUTHENTICATED (welcome) *** server-signature-verified={ok_srv}\n")
        print("FOX-LOGIN-OK")
        return 0
    elif cmd == "retry":
        sys.stderr.write("[fox] server said retry (try next scheme)\n"); return 4
    else:
        sys.stderr.write(f"[fox] REJECTED: {cmd} {wel}\n"); return 5

if __name__ == "__main__":
    sys.exit(main())
