#!/usr/bin/env python3
"""
http-digest.py — a small reusable HTTP client that authenticates with
HTTP Digest (RFC 7616 / 2617) OR HTTP Basic, using VALID credentials.

Purpose: talk to embedded/IoT devices that use HTTP Digest auth — network
cameras (AXIS, etc.), PDUs/UPS, and similar gear — to READ data (snapshots,
status, config) with an account you own. It is a convenience wrapper around
Python's standard-library Digest/Basic auth (the same thing `curl --digest`
does); it authenticates normally, it does not bypass anything.

NOTE: this only works against servers that actually challenge with HTTP
Digest/Basic. It does NOT work against Niagara N4 (that uses SCRAM), nor any
service that doesn't send a WWW-Authenticate: Digest/Basic challenge.

Standard library only — no pip install.

Examples:
  # GET a camera snapshot, save to file (password prompted, not in argv):
  http-digest.py -u root -k -o snap.jpg https://172.16.101.31/axis-cgi/jpg/image.cgi

  # pass the password via env (avoids argv exposure):
  HTTP_PW='secret' http-digest.py -u admin -k https://192.168.1.50/status

  # POST with a body and show response headers:
  http-digest.py -u admin -X POST -d 'k=v' -i http://host/api
"""
import argparse, os, sys, ssl, getpass, urllib.request, urllib.error


def build_opener(url, user, pw, insecure, force_basic):
    pmgr = urllib.request.HTTPPasswordMgrWithDefaultRealm()
    # associate the credentials with the target URL (scheme+host+path prefix);
    # realm=None matches any realm the server challenges with.
    pmgr.add_password(None, url, user, pw)
    handlers = []
    if force_basic:
        handlers.append(urllib.request.HTTPBasicAuthHandler(pmgr))
    else:
        # both, so the client answers whatever the server challenges with
        handlers.append(urllib.request.HTTPDigestAuthHandler(pmgr))
        handlers.append(urllib.request.HTTPBasicAuthHandler(pmgr))
    if insecure:
        ctx = ssl.create_default_context()
        ctx.check_hostname = False
        ctx.verify_mode = ssl.CERT_NONE
        handlers.append(urllib.request.HTTPSHandler(context=ctx))
    return urllib.request.build_opener(*handlers)


def main():
    ap = argparse.ArgumentParser(description="HTTP Digest/Basic client for owned devices.")
    ap.add_argument("url")
    ap.add_argument("-u", "--user", required=True)
    ap.add_argument("-p", "--password", help="password (else $HTTP_PW, else prompt)")
    ap.add_argument("-X", "--method", default=None, help="HTTP method (default GET, or POST if -d)")
    ap.add_argument("-d", "--data", default=None, help="request body")
    ap.add_argument("-H", "--header", action="append", default=[], help="extra header 'Name: value' (repeatable)")
    ap.add_argument("-k", "--insecure", action="store_true", help="skip TLS certificate verification")
    ap.add_argument("-o", "--output", help="write body to file instead of stdout")
    ap.add_argument("-i", "--include", action="store_true", help="print response status + headers to stderr")
    ap.add_argument("--basic", action="store_true", help="force HTTP Basic instead of auto Digest+Basic")
    ap.add_argument("--timeout", type=float, default=20.0)
    args = ap.parse_args()

    pw = args.password or os.environ.get("HTTP_PW")
    if pw is None:
        pw = getpass.getpass(f"Password for {args.user}: ")

    method = args.method or ("POST" if args.data is not None else "GET")
    headers = {}
    for h in args.header:
        if ":" in h:
            k, v = h.split(":", 1)
            headers[k.strip()] = v.strip()

    opener = build_opener(args.url, args.user, pw, args.insecure, args.basic)
    data = args.data.encode() if args.data is not None else None
    req = urllib.request.Request(args.url, data=data, method=method, headers=headers)

    try:
        resp = opener.open(req, timeout=args.timeout)
    except urllib.error.HTTPError as e:
        sys.stderr.write(f"HTTP {e.code} {e.reason}\n")
        wa = e.headers.get("WWW-Authenticate")
        if wa:
            sys.stderr.write(f"WWW-Authenticate: {wa}\n")
        body = e.read()
        if args.output:
            open(args.output, "wb").write(body)
        else:
            sys.stdout.buffer.write(body)
        return 1
    except urllib.error.URLError as e:
        sys.stderr.write(f"connection error: {e.reason}\n")
        return 2

    if args.include:
        sys.stderr.write(f"HTTP {resp.status} {resp.reason}\n")
        for k, v in resp.getheaders():
            sys.stderr.write(f"{k}: {v}\n")
        sys.stderr.write("\n")

    body = resp.read()
    if args.output:
        open(args.output, "wb").write(body)
        sys.stderr.write(f"[{len(body)} bytes -> {args.output}]  HTTP {resp.status} {resp.headers.get('Content-Type','')}\n")
    else:
        sys.stdout.buffer.write(body)
    return 0


if __name__ == "__main__":
    sys.exit(main())
