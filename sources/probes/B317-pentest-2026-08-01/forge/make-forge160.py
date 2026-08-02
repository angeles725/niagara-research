#!/usr/bin/env python3
"""make-forge160.py — forge a vendor certificate + license with the ATTACKER's own
DSA-160 keypair (the honest attacker simulation used in pentest test L-4/L-5).

This is the code that produced:
  - PentestVendor160.certificate  (self-signed with attacker_dsa160.pem)
  - PentestVendor160.license      (signed with the same key)

Live result (2026-08-01, iC-Niagara-4.10.9.14, nre -licenses):
  PentestVendor160.certificate {invalid: Invalid signature}
  -> certificate signature must verify against the EMBEDDED root key in baja.jar
     (LicenseUtil.verify(xml, sig, new Version(versionString)) ->
      masterPublicKeyData DSA / version2PublicKeyData ECDSA). NOT the on-disk store.
  -> license {invalid: Invalid certificate for vendor: PentestVendor}
  Features = none.  Forgery NOT-REPRODUCED without the private root key.

Format notes (all learned live this session):
  * Certificate XML uses the historical Tridium typo attr `algorthm` (not `algorithm`).
  * DSA-1024/q=160 (openssl 3.x default is q=224 — that yields signatures the
    platform parser rejects with "error decoding signature bytes"; q=160 makes the
    verifier RUN and fail cleanly with "Invalid signature").
  * <signature> is base64 DER SEQUENCE{INTEGER r, INTEGER s} with 20-byte INTEGERs.
"""
import base64
import subprocess
import sys

KEY = "attacker_dsa160.pem"
HOSTID = "Win-4D6F-169B-CEF1-8F57"   # live hostId of the lab mini-PC (re-measured)
VENDOR = "PentestVendor"

def sign(body_path: str) -> bytes:
    return subprocess.run(
        ["openssl", "dgst", "-sha1", "-sign", KEY, body_path],
        capture_output=True,
    ).stdout

def main() -> None:
    # 1. certificate body (no <signature> yet) — the platform re-encodes the parsed
    #    XElem via LicenseUtil.encode(root) after stripping <signature>, then verifies.
    pub = subprocess.run(
        ["openssl", "pkey", "-in", KEY, "-pubout", "-outform", "DER"],
        capture_output=True,
    ).stdout
    pub_b64 = base64.b64encode(pub).decode()
    cert_body = (
        f'<certificate version="1.0" vendor="{VENDOR}" generated="2026-08-01" expiration="never">\n'
        f' <publicKey algorthm="DSA">\n{pub_b64}\n </publicKey>\n</certificate>\n'
    )
    open("cert_body160.xml", "w").write(cert_body)
    csig = base64.b64encode(sign("cert_body160.xml")).decode()
    cert = cert_body.replace(
        "</certificate>\n", f" <signature>{csig}</signature>\n</certificate>\n"
    )
    open(f"{VENDOR}160.certificate", "w").write(cert)

    # 2. license body (no <signature>) + signature over the canonical re-encoded form
    lic_body = (
        f'<license vendor="{VENDOR}" expiration="2027-12-31" hostId="{HOSTID}" version="4.10" generated="2026-08-01">\n'
        '<feature name="station" point.limit="none">\n</feature>\n</license>\n'
    )
    open("lic_body160.xml", "w").write(lic_body)
    lsig = base64.b64encode(sign("lic_body160.xml")).decode()
    lic = lic_body.replace("</license>\n", f"<signature>{lsig}</signature>\n</license>\n")
    open(f"{VENDOR}160.license", "w").write(lic)

    print(f"wrote {VENDOR}160.certificate and {VENDOR}160.license")

if __name__ == "__main__":
    sys.exit(main())
