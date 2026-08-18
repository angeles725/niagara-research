#!/usr/bin/env python3
"""
bacnet-bbmd-verify.py — BACnet/IP BBMD & Who-Is/I-Am cross-subnet reachability verifier.

Answers ONE operational question for a multi-subnet Niagara N4 BACnet/IP deployment:
"If a field device's IP changes, will Niagara re-bind it automatically?" — which reduces to
"does a broadcast Who-Is (and the device's I-Am) actually cross the router, or is the device
only reachable because its MAC is pinned as IP:port and polled by unicast?"

Why an ICMP `ping <device-ip>` does NOT answer this (corpus block B444):
  - Confirmed BACnet services (poll/COV/reads) are UNICAST to the device address
    (BBacnetDevice.java:1621,1971,2270) -> routed like ICMP -> cross a router fine.
  - Who-Is is a global BROADCAST (BBacnetDevice.java:1051; GLOBAL_BROADCAST 0xFFFF) ->
    does NOT cross a router without a BBMD or Foreign-Device registration.
  So a successful ping / working poll proves UNICAST reachability only, never that Who-Is/I-Am
  rebind will work. This tool separates the two by sending real BACnet/IP frames.

Wire constants are all [CERT] from the (Java) bacnet-rt bytecode corpus:
  BVLL function codes (BvllConst + message classes stack/link/ip/*):
    Original-Unicast-NPDU=0x0A, Original-Broadcast-NPDU=0x0B, Distribute-Broadcast-To-Network=0x09,
    Forwarded-NPDU=0x04, Write-BDT=0x01, Read-BDT=0x02, Read-BDT-Ack=0x03,
    Register-Foreign-Device=0x05, Read-FDT=0x06;  BVLC type = 0x81 (129).
  Unconfirmed service choices (BacnetUnconfirmedServiceChoice): I_AM=0, WHO_IS=8.
  Unconfirmed-Request APDU PDU-type = 0x10 (block B133, UnconfirmedRequestPdu.java:44-48).
  Niagara-as-BBMD answers Read-BDT with Read-BDT-Ack (BBacnetIpLinkLayer.java:885-895).

READ-ONLY by default: sends only Who-Is, Read-BDT and Read-FDT (queries, no state change on any
device or BBMD). Foreign-Device registration (a write) is NOT performed; it is left as a manual
Workbench step in the companion runbook BACNET-BBMD-VERIFY.md.

Usage:
  bacnet-bbmd-verify.py --device-ip 192.168.0.50 [--local-bcast 192.168.1.255]
                        [--bbmd 192.168.1.1] [--port 47808] [--src-port 47808]
                        [--timeout 3] [--json]

  --device-ip    a KNOWN field device on the OTHER subnet (e.g. a thermostat), used for the
                 directed-unicast reachability test and cross-subnet broadcast comparison.
  --local-bcast  broadcast address of the interface running this tool (e.g. 192.168.1.255).
                 Default 255.255.255.255 (limited broadcast, local wire only).
  --bbmd         IP to probe as a BBMD (Read-BDT/Read-FDT). Defaults to trying --device-ip and,
                 if given, the interface gateway. A BBMD is usually the JACE/supervisor IpPort.
  --port         BACnet/IP UDP port on the targets. Default 47808 (0xBAC0).
  --src-port     local UDP port to bind/send from. Default 47808; if busy (Niagara running on
                 THIS host), pass e.g. --src-port 0 for an ephemeral port.

Run it from a host on the SUPERVISOR's subnet (ideally the JACE/supervisor host itself) so the
broadcast test reflects what Niagara's own Who-Is would reach.
"""
import argparse
import json
import socket
import struct
import sys
import time

# ---- BVLL / BACnet constants (all [CERT], see module docstring) ----
BVLC_TYPE = 0x81
FN_ORIGINAL_UNICAST = 0x0A
FN_ORIGINAL_BROADCAST = 0x0B
FN_FORWARDED_NPDU = 0x04
FN_READ_BDT = 0x02
FN_READ_BDT_ACK = 0x03
FN_READ_FDT = 0x06
FN_READ_FDT_ACK = 0x07
FN_RESULT = 0x00
APDU_UNCONFIRMED_REQUEST = 0x10
SVC_WHO_IS = 0x08
SVC_I_AM = 0x00

C = {"crit": "\033[91m", "high": "\033[93m", "ok": "\033[92m",
     "dim": "\033[90m", "hdr": "\033[96m", "off": "\033[0m"}


def _color(s, k):
    return f"{C[k]}{s}{C['off']}"


def encode_unsigned(val):
    """Minimal-length big-endian unsigned, at least 1 byte (BACnet ASN.1 unsigned)."""
    if val == 0:
        return b"\x00"
    out = b""
    while val > 0:
        out = bytes([val & 0xFF]) + out
        val >>= 8
    return out


def context_unsigned(tag_number, val):
    """Context-tagged unsigned integer: [tag<<4 | 0x08 | len]. len<5 fits the initial octet."""
    data = encode_unsigned(val)
    if len(data) > 4:
        raise ValueError("instance too large for this encoder")
    return bytes([(tag_number << 4) | 0x08 | len(data)]) + data


def bvlc_wrap(function, payload):
    total = 4 + len(payload)
    return struct.pack(">BBH", BVLC_TYPE, function, total) + payload


def build_whois(low=None, high=None, broadcast=True):
    """Who-Is APDU wrapped in BVLL. Unrestricted when low/high are None."""
    apdu = bytes([APDU_UNCONFIRMED_REQUEST, SVC_WHO_IS])
    if low is not None and high is not None:
        apdu += context_unsigned(0, low) + context_unsigned(1, high)
    npdu = bytes([0x01, 0x00]) + apdu  # version 1, control 0x00
    fn = FN_ORIGINAL_BROADCAST if broadcast else FN_ORIGINAL_UNICAST
    return bvlc_wrap(fn, npdu)


def build_read_bdt():
    return bvlc_wrap(FN_READ_BDT, b"")   # 81 02 00 04


def build_read_fdt():
    return bvlc_wrap(FN_READ_FDT, b"")   # 81 06 00 04


def parse_iam(apdu):
    """Return device instance from an I-Am APDU, or None. Layout after 0x10 0x00:
    app-tag ObjectIdentifier = 0xC4 + 4 bytes (type<<22 | instance)."""
    if len(apdu) < 2 or apdu[0] != APDU_UNCONFIRMED_REQUEST or apdu[1] != SVC_I_AM:
        return None
    body = apdu[2:]
    if len(body) < 5 or body[0] != 0xC4:
        return None
    (objid,) = struct.unpack(">I", body[1:5])
    obj_type = objid >> 22
    instance = objid & 0x3FFFFF
    if obj_type != 8:  # 8 = device object
        return None
    return instance


def parse_bvlc(data):
    """Return (function, payload) or (None, None) if not a BVLC/IP frame."""
    if len(data) < 4 or data[0] != BVLC_TYPE:
        return None, None
    function = data[1]
    (length,) = struct.unpack(">H", data[2:4])
    return function, data[4:length] if length <= len(data) else data[4:]


def extract_iam(function, payload):
    """From a received BVLL payload, pull an I-Am instance if present.
    Original-*-NPDU: payload = NPDU(2)+APDU. Forwarded-NPDU: 6-byte orig addr then NPDU+APDU."""
    orig_addr = None
    if function in (FN_ORIGINAL_UNICAST, FN_ORIGINAL_BROADCAST):
        npdu = payload
    elif function == FN_FORWARDED_NPDU:
        if len(payload) < 6:
            return None, None
        orig_addr = payload[:6]
        npdu = payload[6:]
    else:
        return None, None
    if len(npdu) < 2:
        return None, orig_addr
    control = npdu[1]
    idx = 2
    # Skip optional NPDU routing fields if present (bit 5 = dest, bit 3 = src).
    if control & 0x20:  # DNET(2)+DLEN(1)+DADR(DLEN)
        if len(npdu) < idx + 3:
            return None, orig_addr
        dlen = npdu[idx + 2]
        idx += 3 + dlen
    if control & 0x08:  # SNET(2)+SLEN(1)+SADR(SLEN)
        if len(npdu) < idx + 3:
            return None, orig_addr
        slen = npdu[idx + 2]
        idx += 3 + slen
    if control & 0x20:  # hop count present when dest present
        idx += 1
    apdu = npdu[idx:]
    return parse_iam(apdu), orig_addr


def addr6_to_str(addr6):
    ip = ".".join(str(b) for b in addr6[:4])
    port = (addr6[4] << 8) | addr6[5]
    return f"{ip}:0x{port:04X}"


def make_socket(src_port, timeout):
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    s.setsockopt(socket.SOL_SOCKET, socket.SO_BROADCAST, 1)
    try:
        s.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    except OSError:
        pass
    try:
        s.bind(("0.0.0.0", src_port))
    except OSError as e:
        print(_color(f"  ! could not bind local UDP :{src_port} ({e}). "
                     f"Retry with --src-port 0 (ephemeral).", "high"))
        sys.exit(2)
    s.settimeout(timeout)
    return s


def collect(sock, deadline, want_iam=True):
    """Drain responses until deadline. Returns list of (src_ip, src_port, function, iam_instance,
    forwarded_addr, raw)."""
    out = []
    while True:
        remaining = deadline - time.monotonic()
        if remaining <= 0:
            break
        sock.settimeout(remaining)
        try:
            data, (rip, rport) = sock.recvfrom(1500)
        except socket.timeout:
            break
        except OSError:
            break
        fn, payload = parse_bvlc(data)
        if fn is None:
            continue
        inst, fwd = (None, None)
        if want_iam:
            inst, fwd = extract_iam(fn, payload)
        out.append((rip, rport, fn, inst, fwd, data))
    return out


def parse_bdt_ack(data):
    """Read-BDT-Ack: entries of 10 bytes = 6-byte BACnet/IP addr + 4-byte broadcast mask."""
    fn, payload = parse_bvlc(data)
    if fn != FN_READ_BDT_ACK:
        return None
    entries = []
    for i in range(0, len(payload) - 9, 10):
        addr = payload[i:i + 6]
        mask = payload[i + 6:i + 10]
        entries.append((addr6_to_str(addr), ".".join(str(b) for b in mask)))
    return entries


def parse_fdt_ack(data):
    """Read-FDT-Ack: entries of 10 bytes = 6-byte addr + 2-byte TTL + 2-byte remaining."""
    fn, payload = parse_bvlc(data)
    if fn != FN_READ_FDT_ACK:
        return None
    entries = []
    for i in range(0, len(payload) - 9, 10):
        addr = payload[i:i + 6]
        ttl = (payload[i + 6] << 8) | payload[i + 7]
        rem = (payload[i + 8] << 8) | payload[i + 9]
        entries.append((addr6_to_str(addr), ttl, rem))
    return entries


def same_subnet_24(a, b):
    return a.split(".")[:3] == b.split(".")[:3]


def main():
    ap = argparse.ArgumentParser(
        description="Verify BACnet/IP BBMD + Who-Is/I-Am cross-subnet reachability (read-only).")
    ap.add_argument("--device-ip", required=True,
                    help="a known field device on the OTHER subnet (e.g. 192.168.0.50)")
    ap.add_argument("--local-bcast", default="255.255.255.255",
                    help="broadcast address of THIS host's interface (default limited broadcast)")
    ap.add_argument("--bbmd", default=None, help="IP to probe as a BBMD (Read-BDT/Read-FDT)")
    ap.add_argument("--port", type=int, default=47808, help="BACnet/IP UDP port (default 47808)")
    ap.add_argument("--src-port", type=int, default=47808,
                    help="local UDP bind port (use 0 if Niagara holds 47808 on this host)")
    ap.add_argument("--timeout", type=float, default=3.0, help="per-probe listen window (s)")
    ap.add_argument("--json", action="store_true", help="machine-readable output")
    args = ap.parse_args()

    result = {"device_ip": args.device_ip, "port": args.port, "tests": {}}
    sock = make_socket(args.src_port, args.timeout)

    if not args.json:
        print(_color("\n  BACnet/IP BBMD & Who-Is reachability verifier "
                     "(read-only) — corpus B444\n", "hdr"))

    # Test 1 — directed UNICAST Who-Is to the device (the pinned-MAC / ICMP-equivalent path).
    sock.sendto(build_whois(broadcast=False), (args.device_ip, args.port))
    r1 = collect(sock, time.monotonic() + args.timeout)
    uni_hit = [x for x in r1 if x[0] == args.device_ip and x[3] is not None]
    result["tests"]["unicast_device"] = {
        "reachable": bool(uni_hit),
        "instance": uni_hit[0][3] if uni_hit else None,
    }

    # Test 2 — local BROADCAST Who-Is (what Niagara's own Who-Is emits).
    sock.sendto(build_whois(broadcast=True), (args.local_bcast, args.port))
    r2 = collect(sock, time.monotonic() + args.timeout)
    responders = {}
    for rip, rport, fn, inst, fwd, _ in r2:
        if inst is not None:
            responders.setdefault(rip, set()).add(inst)
    cross = {ip: sorted(v) for ip, v in responders.items()
             if same_subnet_24(ip, args.device_ip)}
    local = {ip: sorted(v) for ip, v in responders.items()
             if not same_subnet_24(ip, args.device_ip)}
    result["tests"]["broadcast"] = {
        "responders": {ip: sorted(v) for ip, v in responders.items()},
        "crossed_from_device_subnet": cross,
        "same_as_local": local,
    }

    # Test 3 — Read-BDT / Read-FDT against candidate BBMDs.
    bbmd_targets = [t for t in [args.bbmd, args.device_ip] if t]
    bdt_results = {}
    for tgt in dict.fromkeys(bbmd_targets):
        sock.sendto(build_read_bdt(), (tgt, args.port))
        rb = collect(sock, time.monotonic() + args.timeout, want_iam=False)
        entry = {"is_bbmd": False, "bdt": None, "fdt": None}
        for _, _, fn, _, _, raw in rb:
            bdt = parse_bdt_ack(raw)
            if bdt is not None:
                entry["is_bbmd"] = True
                entry["bdt"] = bdt
        if entry["is_bbmd"]:
            sock.sendto(build_read_fdt(), (tgt, args.port))
            rf = collect(sock, time.monotonic() + args.timeout, want_iam=False)
            for _, _, fn, _, _, raw in rf:
                fdt = parse_fdt_ack(raw)
                if fdt is not None:
                    entry["fdt"] = fdt
        bdt_results[tgt] = entry
    result["tests"]["bbmd_probe"] = bdt_results
    sock.close()

    # ---- Verdict ----
    device_answers_unicast = result["tests"]["unicast_device"]["reachable"]
    device_answers_broadcast = bool(cross)
    any_bbmd = any(v["is_bbmd"] for v in bdt_results.values())

    if device_answers_broadcast or any_bbmd:
        verdict = ("REBIND OK — a broadcast Who-Is reaches the device subnet"
                   + (" (BBMD confirmed)" if any_bbmd else "")
                   + ". Instance-based re-binding will work on IP change.")
        vk = "ok"
    elif device_answers_unicast:
        verdict = ("REBIND WILL FAIL — the device answers UNICAST (pinned IP:port works, like "
                   "ICMP) but NOT the broadcast Who-Is, and no BBMD answered. On an IP change "
                   "Niagara cannot re-resolve it. Add a BBMD/Foreign-Device path or use a static "
                   "IP / DHCP reservation.")
        vk = "crit"
    else:
        verdict = ("INCONCLUSIVE — the device did not answer even the directed unicast Who-Is. "
                   "Check --device-ip, --port, firewall, and that you run this on a routed host.")
        vk = "high"
    result["verdict"] = verdict

    if args.json:
        print(json.dumps(result, indent=2))
        return 0 if vk == "ok" else 1

    u = result["tests"]["unicast_device"]
    print(f"  1) Directed UNICAST Who-Is -> {args.device_ip}:0x{args.port:04X}")
    if u["reachable"]:
        print(_color(f"     I-Am received (instance {u['instance']}). "
                     f"Unicast/routing works — same as ICMP.", "ok"))
    else:
        print(_color("     no I-Am. Device unreachable even by unicast.", "high"))

    print(f"\n  2) Local BROADCAST Who-Is -> {args.local_bcast}:0x{args.port:04X}")
    if not responders:
        print(_color("     no responders at all.", "high"))
    else:
        for ip, insts in sorted(responders.items()):
            tag = _color("[device subnet — CROSSED]", "ok") if same_subnet_24(ip, args.device_ip) \
                else _color("[local subnet]", "dim")
            print(f"     {ip:<16} instances {insts} {tag}")

    print("\n  3) BBMD probe (Read-BDT / Read-FDT)")
    for tgt, e in bdt_results.items():
        if e["is_bbmd"]:
            print(_color(f"     {tgt} IS a BBMD.", "ok"))
            print("       BDT (subnets it distributes broadcasts to):")
            for addr, mask in e["bdt"] or []:
                print(f"         peer {addr}  mask {mask}")
            if e["fdt"]:
                print("       FDT (registered foreign devices):")
                for addr, ttl, rem in e["fdt"]:
                    print(f"         {addr}  ttl {ttl}s  remaining {rem}s")
        else:
            print(_color(f"     {tgt} did not answer Read-BDT (not a BBMD, or blocked).", "dim"))

    print("\n  " + _color("VERDICT:", "hdr") + " " + _color(verdict, vk) + "\n")
    return 0 if vk == "ok" else 1


if __name__ == "__main__":
    sys.exit(main())
