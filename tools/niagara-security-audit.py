#!/usr/bin/env python3
"""
niagara-security-audit.py — station/platform security-posture auditor for Niagara N4.

Consolidates the dispersed hardening findings of the niagara-research corpus (B75, B112,
B113, B114, B160, B316/B317, B379/B384, B392-B397) into one READ-ONLY checker. Inspects a
niagara_home on disk (+ optional live ports) and reports, per check, the observed value vs
the secure baseline, with severity and source block.

READ-ONLY. Never writes to or restarts the target. Reports secret STRUCTURE, never values
(SECRETS DISCIPLINE): "truststore opens with default password", not the key material.

Usage:
  niagara-security-audit.py <niagara_home> [--station <config.bog>] [--host 127.0.0.1] [--json]

  <niagara_home>  install dir: bin/ + defaults/system.properties + security/
  --station       path to a station config.bog (for BProgramService / BSyslogService checks)
  --host          probe live TLS/plaintext ports on this host
  --json          machine-readable output

Check IDs (SEC-01..SEC-18) follow the corpus consolidation in B398.
"""
import argparse, json, os, re, subprocess, socket, sys

SEV = {"crit": 3, "high": 2, "med": 1, "info": 0}
C = {"crit": "\033[91m", "high": "\033[93m", "med": "\033[96m", "info": "\033[90m",
     "ok": "\033[92m", "off": "\033[0m"}


class Report:
    def __init__(self): self.rows = []
    def add(self, cid, sev, title, observed, secure, verdict, block, note=""):
        self.rows.append(dict(id=cid, sev=sev, title=title, observed=observed,
                              secure=secure, verdict=verdict, block=block, note=note))
    def dump(self, as_json):
        if as_json:
            print(json.dumps(self.rows, indent=2)); return
        self.rows.sort(key=lambda r: (-SEV[r["sev"]], r["id"]))
        fails = [r for r in self.rows if r["verdict"] == "FAIL"]
        print(f"\n  Niagara N4 security posture — {len(fails)} finding(s) / {len(self.rows)} checks\n")
        for r in self.rows:
            mark = {"FAIL": "FAIL", "PASS": "PASS"}.get(r["verdict"], "MANUAL")
            col = C[r["sev"]] if r["verdict"] == "FAIL" else (C["ok"] if r["verdict"] == "PASS" else C["info"])
            print(f"  {col}[{mark:6}]{C['off']} {r['id']} ({r['sev']}) {r['title']}  [{r['block']}]")
            if r["verdict"] in ("FAIL", "MANUAL"):
                print(f"           observed: {r['observed']}")
                if r["verdict"] == "FAIL":
                    print(f"           secure  : {r['secure']}")
                if r["note"]:
                    print(f"           note    : {r['note']}")
        crit = sum(1 for r in fails if r["sev"] == "crit")
        high = sum(1 for r in fails if r["sev"] == "high")
        med = sum(1 for r in fails if r["sev"] == "med")
        print(f"\n  summary: {crit} critical · {high} high · {med} med · {len(fails)} findings\n")


def read_props(home):
    props = {}
    for rel in ("defaults/system.properties", "system.properties"):
        p = os.path.join(home, rel)
        if os.path.isfile(p):
            for line in open(p, encoding="utf-8", errors="replace"):
                m = re.match(r"\s*(#?)\s*(niagara\.[\w.]+|program\.[\w.]+)\s*=\s*(.*)", line)
                if m:
                    props[m.group(2)] = (m.group(3).strip(), m.group(1) == "#")
    return props


def keytool_default(path, pw="changeit"):
    if not os.path.isfile(path): return None
    r = subprocess.run(["keytool", "-list", "-keystore", path, "-storepass", pw],
                       capture_output=True, text=True)
    return not any(x in r.stderr.lower() for x in ("tampered", "incorrect", "invalid keystore"))


def keytool_keysizes(path, pw="changeit"):
    if not os.path.isfile(path): return []
    r = subprocess.run(["keytool", "-list", "-v", "-keystore", path, "-storepass", pw],
                       capture_output=True, text=True)
    return [int(x) for x in re.findall(r"(\d+)-bit", r.stdout)]


def tls_subject(host, port):
    try:
        r = subprocess.run(["openssl", "s_client", "-connect", f"{host}:{port}"],
                           input="", capture_output=True, text=True, timeout=15)
        c = subprocess.run(["openssl", "x509", "-noout", "-subject"],
                           input=r.stdout, capture_output=True, text=True)
        return c.stdout.strip() or None
    except Exception:
        return None


def port_open(host, port):
    try:
        with socket.create_connection((host, port), 3):
            return True
    except Exception:
        return False


def bog_ascii(path):
    """Best-effort ASCII slurp of a .bog (it embeds XML-ish tokens)."""
    try:
        d = open(path, "rb").read()
        return b" ".join(re.findall(rb"[ -~]{4,}", d)).decode("ascii", "replace")
    except Exception:
        return ""


def audit(home, station_bog, host, rep):
    props = read_props(home)

    # SEC-01 crit — moduleVerificationMode
    v, com = props.get("niagara.moduleVerificationMode", (None, True))
    bad = v is None or com or v.lower() == "low"
    rep.add("SEC-01", "crit", "moduleVerificationMode", f"{v or '(unset)'}{' [commented]' if com else ''}",
            "high", "FAIL" if bad else "PASS", "B75/B397",
            "low lets an UNSIGNED module requesting NETWORK_COMMUNICATION load (the 443 incident)")

    # SEC-02 crit — truststore default password
    ts = os.path.join(home, "security", "truststore.jks")
    d = keytool_default(ts)
    if d is not None:
        rep.add("SEC-02", "crit", "truststore.jks default password",
                "opens with 'changeit'" if d else "custom password", "non-default store password",
                "FAIL" if d else "PASS", "B392/B397", "FS/daemon access -> trust-anchor injection")

    # SEC-03 crit — security/ filesystem ACLs (Windows icacls; note on Linux)
    secdir = os.path.join(home, "security")
    if os.path.isdir(secdir):
        winpath = to_winpath(secdir)
        if winpath and shutil_which("icacls.exe") or (winpath and os.path.exists("/mnt/c")):
            r = subprocess.run(["icacls.exe", winpath], capture_output=True, text=True)
            loose = bool(re.search(r"(Authenticated Users|Users|Everyone)[^\n]*\((M|F|W)", r.stdout))
            rep.add("SEC-03", "crit", "security/ filesystem ACLs",
                    "Modify/Write for Authenticated Users/Everyone" if loose else "restricted",
                    "Admin/SYSTEM only", "FAIL" if loose else "PASS", "B316/B113",
                    "any authenticated user can plant a trust anchor / license")
        else:
            rep.add("SEC-03", "crit", "security/ filesystem ACLs", "not checked (need icacls)",
                    "Admin/SYSTEM only", "MANUAL", "B316/B113", "run: icacls <home>\\security")

    # SEC-04 crit — default TLS cert (live)
    if host:
        for port, name in ((443, "station HTTPS"), (5011, "platform TLS")):
            s = tls_subject(host, port)
            if s:
                default = "ForRecoveryPurposes" in s
                rep.add(f"SEC-04:{port}", "crit", f"{name} certificate",
                        "default self-signed (ForRecoveryPurposes)" if default else s,
                        "CA-issued cert, replaced", "FAIL" if default else "PASS", "B397/B156",
                        "default cert = no real TLS trust, MITM-able on mgmt LAN")

    # SEC-05 high — commandLinePropertyBlacklist covers skip levers
    v, com = props.get("niagara.commandLinePropertyBlacklist", (None, True))
    covered = v and not com and "skipModuleValidation" in v and "ignoreVerificationMode" in v
    rep.add("SEC-05", "high", "commandLinePropertyBlacklist covers skip levers",
            "disabled/absent" if not v or com else ("partial" if not covered else "full"),
            "includes skipModuleValidation + commissioning.ignoreVerificationMode",
            "FAIL" if not covered else "PASS", "B113",
            "-Dniagara.classLoader.skipModuleValidation can disable chain validation at launch")

    # SEC-06 high — license attributes that relax signing
    lic_hits = []
    licdir = os.path.join(home, "security", "licenses")
    if os.path.isdir(licdir):
        for f in os.listdir(licdir):
            if f.endswith(".license"):
                t = open(os.path.join(licdir, f), encoding="utf-8", errors="replace").read()
                for attr in ("skipModuleValidation", "smDeveloperMode", "unreleasedSoftware"):
                    if re.search(attr + r'\s*=\s*"?true', t) or (attr in t and 'developer' in t):
                        lic_hits.append(f"{f}:{attr}")
        rep.add("SEC-06", "high", "license attributes relaxing signature",
                ", ".join(sorted(set(lic_hits))) or "none", "no developer/unreleased/smDeveloper flags",
                "FAIL" if lic_hits else "PASS", "B75/B113",
                "developer{skipModuleValidation}/unreleasedSoftware accept self-signed / disable validation")

    # SEC-07 high — program.requireSigning
    v, com = props.get("program.requireSigning", (None, True))
    bad = v is None or com or v.lower() == "false"
    rep.add("SEC-07", "high", "program.requireSigning (BProgram bytecode)",
            "false/default" if bad else v, "true", "FAIL" if bad else "PASS", "B75",
            "superuser BProgram runs arbitrary bytecode unsigned")

    # SEC-08 high — allowProgramRuntimeExec (station config)
    # SEC-10 high — syslog offload enabled (station config)
    if station_bog and os.path.isfile(station_bog):
        txt = bog_ascii(station_bog)
        exec_on = re.search(r"allowProgramRuntimeExec[^>]*true", txt) is not None
        rep.add("SEC-08", "high", "allowProgramRuntimeExec", "true" if exec_on else "false/default",
                "false", "FAIL" if exec_on else "PASS", "B75", "Runtime.exec() from programs")
        sys_on = ("SyslogService" in txt or "BSyslogSettings" in txt) and re.search(r"enabled[^>]*true", txt)
        rep.add("SEC-10", "high", "syslog offload to external SIEM",
                "enabled" if sys_on else "disabled/absent", "enabled + serverHost + TLS transport",
                "FAIL" if not sys_on else "PASS", "B75/B393/B396",
                "the ONLY tamper-resistance for the (unsigned) local audit/history record")
    else:
        for cid, title in (("SEC-08", "allowProgramRuntimeExec"), ("SEC-10", "syslog offload to SIEM")):
            rep.add(cid, "high", title, "not checked (no --station config.bog)", "-", "MANUAL",
                    "B75/B396", "pass --station <config.bog>")

    # SEC-09 high — platform plaintext 3011 (live)
    if host and port_open(host, 3011):
        loopback = host in ("127.0.0.1", "localhost", "::1")
        rep.add("SEC-09", "high" if not loopback else "med", "platform daemon plaintext port 3011",
                f"3011 open{' (loopback only, mitigated)' if loopback else ' on reachable iface'}",
                "sslOnly=true (5011 only)", "FAIL", "B75/B397", "platform creds in cleartext if used")

    # SEC-11 med — weak keys / FIPS
    sizes = keytool_keysizes(ts) if os.path.isfile(ts) else []
    ks = os.path.join(home, "security", "keystore.bks")
    weak = [s for s in sizes if s < 2048]
    fips = os.path.isfile(os.path.join(home, "jre", "lib", "security", "cacerts.bcfks"))
    rep.add("SEC-11", "med", "weak signing keys / FIPS",
            f"key sizes {sorted(set(sizes)) or 'n/a'}; FIPS keystore {'present' if fips else 'absent'}",
            "all keys >= 2048; FIPS suppresses RSA-1024 option",
            "FAIL" if weak else "PASS", "B113/B392", "RSA-1024 allowed with click-through warning")

    # SEC-12 med — .bog at-rest encryption (spot-check station config header)
    if station_bog and os.path.isfile(station_bog):
        head = open(station_bog, "rb").read(4096)
        keyring = b"keyring" in head or b"EncryptionKeySource" in head
        rep.add("SEC-12", "med", "config.bog at-rest encryption",
                "keyring/external" if keyring else "none (plaintext)", "keyring",
                "FAIL" if not keyring else "PASS", "B114",
                "EncryptionKeySource=none leaves reversible passwords in the clear")

    # SEC-14 med — Fox/HTTP plaintext ports (live)
    if host:
        for port, name, blk in ((1911, "Fox plaintext", "B397/B134"), (80, "HTTP plaintext", "B397")):
            if port_open(host, port):
                rep.add(f"SEC-14:{port}", "med", f"{name} port {port}", f"{port} open",
                        "TLS-only (4911/443)", "FAIL", blk, "station comms/UI in cleartext")

    # SEC-16 (informational) — data record is unsigned by design
    rep.add("SEC-16", "med", "local data record integrity (audit/history/backup/.bog)",
            "unsigned by design (architectural)", "syslog offload (SEC-10) = resistance, not evidence",
            "FAIL", "B393/B396", "Niagara signs code, not data; no toggle fixes this - audit via SEC-10")


def to_winpath(p):
    m = re.match(r"/mnt/([a-z])/(.*)", p)
    return f"{m.group(1).upper()}:\\" + m.group(2).replace("/", "\\") if m else None


def shutil_which(name):
    import shutil
    return shutil.which(name)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("niagara_home")
    ap.add_argument("--station", default=None)
    ap.add_argument("--host", default=None)
    ap.add_argument("--json", action="store_true")
    a = ap.parse_args()
    if not os.path.isdir(a.niagara_home):
        sys.exit(f"ERROR: not a directory: {a.niagara_home}")
    rep = Report()
    audit(a.niagara_home, a.station, a.host, rep)
    rep.dump(a.json)


if __name__ == "__main__":
    main()
