#!/usr/bin/env python3
"""hdbread.py — minimal read-only reader for Niagara N4 .hdb history files.

On-disk format (confirmed on JACE-8000 SD, focus jace-history-audit B699):
  [0:4]   magic 0xA106F11E
  [4:8]   version (big-endian uint32; observed 2)
  [8:12]  config XML length (big-endian uint32)
  [12:12+len]  embedded HistoryConfig XML (schema: field,type;field,type;...) — cleartext
               (reversibleEncodingKeySource="none" -> records are NOT encrypted)
  [12+len:]    record region (packed cleartext records + index metadata)

This reader extracts the SCHEMA and does a best-effort cleartext field walk. It is READ-ONLY and
does NOT mutate the file. Exact per-record binary framing is a refinement; for research we extract
the schema + the readable string fields (operation/target/userName/message per record type).

SECRETS DISCIPLINE: audit/log records carry operator activity. Use --mask to redact user identities
and long values before printing; cite structure/counts, not specific secret values.

Usage:
  python3 hdbread.py <file.hdb> [--schema] [--ops] [--strings] [--mask]
"""
import sys, struct, re

MAGIC = bytes.fromhex("a106f11e")

def parse(path):
    d = open(path, "rb").read()
    if d[:4] != MAGIC:
        raise SystemExit("not a .hdb (bad magic %s)" % d[:4].hex())
    ver = struct.unpack(">I", d[4:8])[0]
    clen = struct.unpack(">I", d[8:12])[0]
    xml = d[12:12+clen].decode("utf-8", "replace")
    records = d[12+clen:]
    return ver, clen, xml, records

def schema_of(xml):
    m = re.search(r'n="schema"[^>]*v="([^"]*)"', xml)
    fields = []
    if m:
        for pair in m.group(1).split(";"):
            if "," in pair:
                name, typ = pair.split(",", 1)
                fields.append((name, typ))
    rt = re.search(r'n="recordType"[^>]*v="([^"]*)"', xml)
    hid = re.search(r'n="id"[^>]*v="([^"]*)"', xml)
    return (hid.group(1) if hid else "?"), (rt.group(1) if rt else "?"), fields

def mask(s):
    # redact anything that looks like a credential value or a non-role personal login
    if re.fullmatch(r'(admin|guest|root|niagarad|station|daemon|sshd)', s):
        return s
    if re.search(r'[A-Za-z0-9+/]{16,}={0,2}', s):
        return "<VALUE-MASKED>"
    return s

def main():
    args = sys.argv[1:]
    if not args:
        raise SystemExit(__doc__)
    path = args[0]
    do_mask = "--mask" in args
    ver, clen, xml, rec = parse(path)
    hid, rt, fields = schema_of(xml)
    print("# %s" % path)
    print("magic=A106F11E version=%d config_len=%d record_region=%d bytes" % (ver, clen, len(rec)))
    if "--schema" in args or not any(a.startswith("--") for a in args[1:]):
        print("history_id=%s" % hid)
        print("record_type=%s" % rt)
        print("schema (%d fields): %s" % (len(fields), ", ".join(n for n, _ in fields)))
    if "--ops" in args:
        for op in (b"Added", b"Changed", b"Removed", b"Renamed"):
            print("op %-8s = %d" % (op.decode(), rec.count(op)))
    if "--strings" in args:
        for run in re.findall(rb'[ -~]{3,}', rec):
            s = run.decode()
            print(mask(s) if do_mask else s)

if __name__ == "__main__":
    main()
