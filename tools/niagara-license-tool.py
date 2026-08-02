#!/usr/bin/env python3
"""
niagara-license-tool.py — OEM license tooling for Niagara N4 `.license` files.

Replicates the platform's license signing/verification (validated against a REAL
signed license, pentest B316-L12/B322):
  * signature = DSA-1024 (q=160) / SHA-1 over the canonical XML re-encoding
    WITHOUT the <signature> element (LicenseUtil.encode semantics: attributes in
    document order, ">\n", children, "</root>\n")
  * signature element = base64 DER SEQUENCE { INTEGER r(20B), INTEGER s(20B) }
  * certificate chain: license sig verified against {vendor}.certificate public
    key; the certificate's OWN sig verifies against the EMBEDDED root key in
    baja.jar (this build: single DSA root, B322)

Subcommands:
  verify  <license.xml> <certificate.xml>   -> offline check (no station needed)
  sign    <license.xml> <privkey.pem>       -> re-sign in place (new <signature>)
  rehost  <license.xml> <new-hostid> <privkey.pem> -> change hostId AND re-sign
  gen     <out.license> <vendor> <hostid> <expiration> <features.csv> <privkey.pem>
          -> build a license from scratch and sign it

The private key is the VENDOR's DSA private key matching {vendor}.certificate's
public key (the OEM holds it). NEVER store/commit private keys — they stay on the
operator's machine; this tool only reads them from the path you pass.

Dependencies: python3 + cryptography (pip install cryptography).
"""

import base64
import csv
import io
import sys
from datetime import date

from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import dsa
from lxml import etree


# ---------------------------------------------------------------------------
# Canonical re-encoding, mirroring com.tridium.sys.license.LicenseUtil.encode:
#   <qname attr1="v1" attr2="v2">\n  (attrs in DOCUMENT order)
#   <child ...>\n</child>\n        (children recursively)
#   text nodes: text + "\n"
#   </qname>\n
# The <signature> element is REMOVED before encoding (that is what the platform
# verifies: LicenseFile.java:170-181 / CertificateFile.java:73-74).
# ---------------------------------------------------------------------------
def canonical_encode(root: etree._Element) -> bytes:
    """Replicate LicenseUtil.encode EXACTLY (validated against a real signed license).

    Key semantics (discovered by brute-force validation against the real
    Honeywell.license signature, pentest B323):
      * inter-element whitespace (indentation, blank lines) is DISCARDED —
        Tridium's XParser does not keep it as XText for <feature> children
      * a self-closing element <feature/> re-encodes EXPANDED: <feature>\n</feature>\n
      * attributes in document order; children recursively; no text preservation
    """
    out = io.StringIO()

    def enc(elem: etree._Element) -> None:
        out.write("<")
        out.write(elem.tag)
        for name, value in elem.attrib.items():      # lxml preserves doc order
            out.write(f' {name}="{value}"')
        out.write(">\n")
        for child in elem:
            enc(child)
        out.write("</")
        out.write(elem.tag)
        out.write(">\n")

    enc(root)
    return out.getvalue().encode("utf-8")


def load_license_xml(path: str) -> etree._Element:
    parser = etree.XMLParser(remove_blank_text=False, resolve_entities=False)
    root = etree.parse(path, parser).getroot()
    if root.tag != "license":
        raise SystemExit(f"ERROR: root element is <{root.tag}>, expected <license>")
    return root


def load_cert_public_key(cert_path: str):
    """Extract the DSA public key (SPKI base64) from {vendor}.certificate XML."""
    parser = etree.XMLParser(resolve_entities=False)
    root = etree.parse(cert_path, parser).getroot()
    pk = root.find("publicKey")
    if pk is None or not pk.text:
        raise SystemExit("ERROR: no <publicKey> in certificate")
    spki_b64 = "".join(pk.text.split())
    algo = pk.get("algorithm") or pk.get("algorthm") or "DSA"
    pub = serialization.load_der_public_key(base64.b64decode(spki_b64))
    return pub, algo


def canonical_bytes_for_verify(root: etree._Element) -> bytes:
    """Return the bytes that the platform signs: root WITHOUT <signature>."""
    import copy
    clone = copy.deepcopy(root)
    for sig in clone.findall("signature"):
        clone.remove(sig)
    return canonical_encode(clone)


def cmd_verify(license_path: str, cert_path: str) -> None:
    root = load_license_xml(license_path)
    sig_el = root.find("signature")
    if sig_el is None or not sig_el.text:
        raise SystemExit("ERROR: no <signature> element")
    sig = base64.b64decode("".join(sig_el.text.split()))
    pub, algo = load_cert_public_key(cert_path)
    data = canonical_bytes_for_verify(root)
    try:
        pub.verify(sig, data, hashes.SHA1())
        print(f"OK   {license_path}: VALID signature (verified offline)")
        print(f"     vendor={root.get('vendor')} hostId={root.get('hostId')} "
              f"expires={root.get('expiration')} generated={root.get('generated')}")
        return 0
    except Exception as e:
        print(f"FAIL {license_path}: {type(e).__name__}: {e}")
        return 1


def _sign_bytes(data: bytes, privkey_path: str) -> str:
    """Sign with openssl (DSA-1024/SHA-1) — produces the exact DER the platform
    accepts (proven in pentest L-4: attacker DSA-160 signature parsed fine and
    failed only cryptographically). cryptography's DSA.sign() returns r||s plus
    extra bytes, so openssl is the reliable path here."""
    import subprocess
    import tempfile
    with tempfile.NamedTemporaryFile(suffix=".xml", delete=False) as tf:
        tf.write(data)
        tf.flush()
        name = tf.name
    try:
        sig = subprocess.run(
            ["openssl", "dgst", "-sha1", "-sign", privkey_path, name],
            capture_output=True, check=True,
        ).stdout
        # Platform DER requirement (discovered by live testing, B323): Sun JDK8's DSA
        # decodeSignature rejects INTEGERs whose top bit (bit 159) is set — it reads
        # them as NEGATIVE and raises "error decoding signature bytes". The real
        # Tridium signer therefore only ships signatures where r and s both have
        # bit159 = 0 (retrying with a fresh k until satisfied; ~25% per attempt).
        # Emit SEQUENCE { INTEGER r(20B), INTEGER s(20B) } with bit159 clear.
        i = 2
        def rd_int():
            nonlocal i
            assert sig[i] == 2
            ln = sig[i+1]
            v = int.from_bytes(sig[i+2:i+2+ln], "big")
            i += 2 + ln
            return v
        for _ in range(40):
            r = rd_int(); s = rd_int()
            rb = r.to_bytes(20, "big"); sb = s.to_bytes(20, "big")
            if not (rb[0] & 0x80) and not (sb[0] & 0x80):
                body = b"\x02\x14" + rb + b"\x02\x14" + sb
                der = b"\x30" + bytes([len(body)]) + body
                return base64.b64encode(der).decode()
            # re-sign with a fresh k
            sig = subprocess.run(
                ["openssl", "dgst", "-sha1", "-sign", privkey_path, name],
                capture_output=True, check=True,
            ).stdout
            i = 2
        raise SystemExit("ERROR: could not produce a bit159-clear DSA signature after 40 tries")
    finally:
        import os
        os.unlink(name)


def _set_signature(root: etree._Element, sig_b64: str) -> None:
    for sig in root.findall("signature"):
        root.remove(sig)
    sig_el = etree.SubElement(root, "signature")
    sig_el.text = sig_b64


def _write_license(root: etree._Element, path: str) -> None:
    tree = etree.ElementTree(root)
    tree.write(path, xml_declaration=False, encoding="utf-8", pretty_print=False)


def cmd_sign(license_path: str, privkey_path: str) -> None:
    root = load_license_xml(license_path)
    data = canonical_bytes_for_verify(root)
    sig_b64 = _sign_bytes(data, privkey_path)
    _set_signature(root, sig_b64)
    _write_license(root, license_path)
    print(f"OK   re-signed {license_path}")


def cmd_rehost(license_path: str, new_hostid: str, privkey_path: str) -> None:
    root = load_license_xml(license_path)
    root.set("hostId", new_hostid)
    data = canonical_bytes_for_verify(root)
    sig_b64 = _sign_bytes(data, privkey_path)
    _set_signature(root, sig_b64)
    _write_license(root, license_path)
    print(f"OK   re-hosted to {new_hostid} and re-signed: {license_path}")


def cmd_gen(out_path: str, vendor: str, hostid: str, expiration: str,
            features_csv: str, privkey_path: str) -> None:
    root = etree.Element("license")
    root.set("vendor", vendor)
    root.set("expiration", expiration)
    root.set("hostId", hostid)
    root.set("version", "4.10")
    root.set("generated", date.today().isoformat())
    for row in csv.reader(io.StringIO(features_csv)):
        # row = [name, attr=val, attr=val, ...]  (CSV separates cells on commas;
        # within a cell, attributes are ";"-separated and joined with ",")
        if not row or not row[0].strip():
            continue
        name = row[0].strip()
        feat = etree.SubElement(root, "feature")
        feat.set("name", name)
        for cell in row[1:]:
            for pair in cell.split(";"):
                pair = pair.strip()
                if not pair:
                    continue
                k, _, v = pair.partition("=")
                if k:
                    feat.set(k, v)
    data = canonical_bytes_for_verify(root)
    sig_b64 = _sign_bytes(data, privkey_path)
    _set_signature(root, sig_b64)
    _write_license(root, out_path)
    print(f"OK   generated {out_path}")


USAGE = __doc__

if __name__ == "__main__":
    args = sys.argv[1:]
    if not args or args[0] in ("-h", "--help"):
        print(USAGE)
        raise SystemExit(0)
    cmd, rest = args[0], args[1:]
    if cmd == "verify" and len(rest) == 2:
        raise SystemExit(cmd_verify(*rest))
    if cmd == "sign" and len(rest) == 2:
        cmd_sign(*rest)
    elif cmd == "rehost" and len(rest) == 3:
        cmd_rehost(*rest)
    elif cmd == "gen" and len(rest) == 6:
        cmd_gen(*rest)
    else:
        print(USAGE)
        raise SystemExit(2)
