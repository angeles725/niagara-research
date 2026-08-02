#!/usr/bin/env python3
"""make-probes.py — the stage-by-stage probe generator (pentest tests L-3 / L-3b).

Vendor = "Tridium" so the on-disk (valid) Tridium.certificate resolves and the
5-check pipeline advances as far as each intentionally-broken field allows.
Each file varies exactly ONE thing; `nre -licenses` reports which check fired.

Live results (2026-08-01):
  probe2-hostid    (hostId of ANOTHER machine)   -> file MOVED to db/<that-hostId>/,
                                                    never loaded (host binding)
  probe2-generated (generated=2030-01-01)        -> "Current date is earlier than
                                                    license generated date"
  probe2-expired   (expiration=2020-01-01)       -> "License file is expired"
  probe2-nosig     (no <signature> element)      -> "Invalid XML: Missing signature element"
  probe2-badsig    (garbage signature)           -> "SignatureException: error decoding
                                                    signature bytes."
  probe2-goodshape (attacker-signed, DSA-224)    -> same SignatureException (format artifact)
  probe3/4         (DSA-160 padded DER)          -> decode quirk; fixed in make-forge160.py
Case-sensitivity (L-3b): vendor="tridium" (lowercase) -> "No certificate for vendor: tridium"
  even though Tridium.certificate exists+valid — NLicenseManager.getCertificate uses
  case-sensitive vendor.equals(cert.vendor).
"""
import base64
import subprocess

HOST = "Win-4D6F-169B-CEF1-8F57"
WRONG = "Win-6E6E-10AC-D1DD-8276"
KEY = "attacker_dsa.pem"      # q=224 key — good enough: format errors still prove the check order

def make(name, host, gen="2026-08-01", exp="2027-12-31", sig="ATTACKER"):
    body = (f'<license vendor="Tridium" expiration="{exp}" hostId="{host}" version="4.10" generated="{gen}">\n'
            '<feature name="nre">\n</feature>\n</license>\n')
    bp = f"probe2_{name}_body.xml"
    open(bp, "w").write(body)
    if sig == "NONE":
        lic = body
    elif sig == "BOGUS":
        lic = body.replace("</license>\n", "<signature>QUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVo=</signature>\n</license>\n")
    else:
        s = subprocess.run(["openssl", "dgst", "-sha1", "-sign", KEY, bp], capture_output=True).stdout
        lic = body.replace("</license>\n", f"<signature>{base64.b64encode(s).decode()}</signature>\n</license>\n")
    open(f"{name}.license", "w").write(lic)
    print("wrote", name)

make("probe2-hostid",    WRONG)
make("probe2-generated", HOST, gen="2030-01-01")
make("probe2-expired",   HOST, exp="2020-01-01")
make("probe2-nosig",     HOST, sig="NONE")
make("probe2-badsig",    HOST, sig="BOGUS")
make("probe2-goodshape", HOST)
