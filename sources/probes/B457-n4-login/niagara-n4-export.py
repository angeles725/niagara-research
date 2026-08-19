#!/usr/bin/env python3
"""
niagara-n4-export.py — bulk data extraction from a Niagara N4 station you own.

Logs in ONCE (SCRAM-SHA-256 + acceptEula, via niagara-n4-client.py's N4Client)
and then pulls data out over oBIX:

  --histories DIR   export every trend history to a CSV per history under DIR
  --config FILE     dump the config point tree (name, href, value) to CSV

Uses your OWN valid credentials over the real N4 protocol (like Workbench) —
normal authenticated access, not a bypass. Standard library only.

Examples:
  N4_PW='...' niagara-n4-export.py -u API2 -k https://localhost --histories ./out
  N4_PW='...' niagara-n4-export.py -u API2 -k https://localhost \
        --histories ./out --start 2026-08-01T00:00:00 --limit 50000
  N4_PW='...' niagara-n4-export.py -u API2 -k https://localhost --config ./points.csv

  # incremental: only fetch records newer than the last run, appending to CSVs
  # (state kept in <DIR>/.export-state.json — ideal for a daily/weekly cron):
  N4_PW='...' niagara-n4-export.py -u API2 -k https://localhost --histories ./out --incremental
"""
import argparse, os, sys, csv, json, getpass, importlib.util
import xml.etree.ElementTree as ET
from datetime import datetime, timedelta
from urllib.parse import quote

# reuse the login client (hyphenated filename -> load via importlib)
_here = os.path.dirname(os.path.abspath(__file__))
_spec = importlib.util.spec_from_file_location("n4client", os.path.join(_here, "niagara-n4-client.py"))
_n4 = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(_n4)
N4Client = _n4.N4Client

OBIX_NS = "http://obix.org/ns/schema/1.0"


def _tag(el):
    return el.tag.split("}", 1)[-1]  # strip namespace


def _parse(body):
    try:
        return ET.fromstring(body)
    except ET.ParseError as e:
        sys.stderr.write(f"[warn] XML parse error: {e}\n")
        return None


def _refs(root):
    """Return [(name, href), ...] for every <ref> child."""
    out = []
    for el in root:
        if _tag(el) == "ref":
            out.append((el.get("name"), el.get("href")))
    return out


def _safe(name):
    return "".join(c if c.isalnum() or c in "-_." else "_" for c in (name or "unnamed"))


def _records_from(root):
    """Parse a HistoryQueryOut root -> (rows, columns)."""
    rows, cols = [], ["timestamp"]
    if root is None:
        return rows, cols
    for lst in root:
        if _tag(lst) != "list":
            continue
        for rec in lst:
            row = {}
            for field in rec:
                n = field.get("name")
                if n:
                    row[n] = field.get("val", "")
                    if n not in cols:
                        cols.append(n)
            if row:
                rows.append(row)
    return rows, cols


def _advance(ts):
    """Given an ISO-8601 timestamp, return ts + 1ms as ISO (next query start)."""
    try:
        return (datetime.fromisoformat(ts) + timedelta(milliseconds=1)).isoformat(timespec="milliseconds")
    except Exception:
        return None


def _query_history(c, base_href, start, end, page_size, max_records):
    """Paginated history query. When a page fills to page_size, re-query from the
    last timestamp + 1ms (Niagara's pagination contract). Dedupes by timestamp."""
    all_rows, cols, seen = [], ["timestamp"], set()
    cur_start, pages = start, 0
    while True:
        params = []
        if cur_start:
            params.append("start=" + quote(cur_start, safe=""))
        if end:
            params.append("end=" + quote(end, safe=""))
        params.append("limit=%d" % page_size)
        _, _, body = c.request(base_href + "?" + "&".join(params))
        rows, pcols = _records_from(_parse(body))
        for cc in pcols:
            if cc not in cols:
                cols.append(cc)
        new, last_ts = 0, None
        for r in rows:
            ts = r.get("timestamp")
            last_ts = ts or last_ts
            if ts and ts in seen:
                continue
            if ts:
                seen.add(ts)
            all_rows.append(r); new += 1
        pages += 1
        if new == 0 or len(rows) < page_size or len(all_rows) >= max_records or not last_ts:
            break
        nxt = _advance(last_ts)
        if not nxt or nxt == cur_start:
            break
        cur_start = nxt
    return all_rows[:max_records], cols, pages


def _csv_write(fn, cols, rows, append):
    """Write rows to CSV. append=True adds to an existing file using its header."""
    if append and os.path.exists(fn):
        with open(fn, newline="") as f:
            header = next(csv.reader(f), None)
        if header:
            cols = header
        mode, write_header = "a", False
    else:
        mode, write_header = "w", True
    with open(fn, mode, newline="") as f:
        w = csv.DictWriter(f, fieldnames=cols, extrasaction="ignore")
        if write_header:
            w.writeheader()
        for r in rows:
            w.writerow(r)


def export_histories(c, host, outdir, start, end, limit, page_size,
                     incremental=False, state_path=None):
    os.makedirs(outdir, exist_ok=True)
    state = {}
    if incremental:
        state_path = state_path or os.path.join(outdir, ".export-state.json")
        if os.path.exists(state_path):
            try:
                state = json.load(open(state_path))
            except Exception:
                state = {}
        sys.stderr.write(f"[incremental] state: {state_path} ({len(state)} tracked)\n")
    st, _, body = c.request("/obix/histories/")
    root = _parse(body)
    if root is None:
        sys.stderr.write("[error] cannot read /obix/histories/\n"); return
    devices = _refs(root)
    sys.stderr.write(f"[histories] {len(devices)} device(s)\n")
    total = 0
    for dname, dhref in devices:
        dpath = "/obix/histories/" + dhref
        st, _, dbody = c.request(dpath)
        droot = _parse(dbody)
        if droot is None:
            continue
        hists = _refs(droot)
        sys.stderr.write(f"[device {dname}] {len(hists)} histories\n")
        for hname, hhref in hists:
            key = "%s__%s" % (_safe(dname), _safe(hname))
            eff_start = start
            if incremental and state.get(key):
                eff_start = _advance(state[key]) or start
            base = "%s%s~historyQuery" % (dpath, hhref)
            rows, cols, pages = _query_history(c, base, eff_start, end, page_size, limit)
            fn = os.path.join(outdir, key + ".csv")
            _csv_write(fn, cols, rows, append=incremental)
            if rows:
                last_ts = next((r["timestamp"] for r in reversed(rows) if r.get("timestamp")), None)
                if last_ts:
                    state[key] = last_ts
            total += len(rows)
            pg = f" ({pages} pages)" if pages > 1 else ""
            tag = " +new" if (incremental and rows) else ""
            sys.stderr.write(f"   {hname}: {len(rows)} rows{tag} -> {os.path.basename(fn)}{pg}\n")
    if incremental:
        json.dump(state, open(state_path, "w"), indent=1)
        sys.stderr.write(f"[incremental] state saved ({len(state)} tracked)\n")
    sys.stderr.write(f"[done] {total} rows across all histories -> {outdir}\n")


def dump_config(c, outfile, max_depth):
    """Walk /obix/config/ following refs; write every object that carries a val."""
    seen = set()
    rows = []

    def walk(path, depth):
        if depth > max_depth or path in seen:
            return
        seen.add(path)
        st, _, body = c.request(path)
        root = _parse(body)
        if root is None:
            return
        # record this object's own value if it has one
        val = root.get("val")
        if val is not None:
            rows.append({"href": root.get("href", path), "name": root.get("display") or root.get("name") or "",
                         "is": root.get("is", ""), "val": val})
        # descend into child refs
        for el in root:
            if _tag(el) == "ref":
                href = el.get("href")
                if not href or href.startswith("~") or href.startswith("#"):
                    continue
                child = href if href.startswith("/") else path.rstrip("/") + "/" + href
                walk(child, depth + 1)

    sys.stderr.write(f"[config] walking /obix/config/ (max depth {max_depth})...\n")
    walk("/obix/config/", 0)
    with open(outfile, "w", newline="") as f:
        w = csv.DictWriter(f, fieldnames=["href", "name", "is", "val"])
        w.writeheader()
        for r in rows:
            w.writerow(r)
    sys.stderr.write(f"[done] {len(rows)} points with values -> {outfile}\n")


def main():
    ap = argparse.ArgumentParser(description="Bulk oBIX data export from a Niagara N4 station you own.")
    ap.add_argument("host", help="base URL, e.g. https://localhost")
    ap.add_argument("-u", "--user", required=True)
    ap.add_argument("-p", "--password", help="password (else $N4_PW, else prompt)")
    ap.add_argument("-k", "--insecure", action="store_true")
    ap.add_argument("--histories", metavar="DIR", help="export all histories to CSV files under DIR")
    ap.add_argument("--config", metavar="FILE", help="dump config point tree to this CSV")
    ap.add_argument("--start", help="history range start (ISO, e.g. 2026-08-01T00:00:00)")
    ap.add_argument("--end", help="history range end (ISO)")
    ap.add_argument("--limit", type=int, default=1000000, help="max TOTAL records per history (safety cap, default 1e6)")
    ap.add_argument("--page-size", type=int, default=5000, help="records per page request (pagination, default 5000)")
    ap.add_argument("--max-depth", type=int, default=6, help="config walk depth (default 6)")
    ap.add_argument("--incremental", action="store_true",
                    help="only fetch records newer than the last run (delta); appends to existing CSVs")
    ap.add_argument("--state", metavar="FILE",
                    help="incremental state file (default <DIR>/.export-state.json)")
    ap.add_argument("--sha512", action="store_true")
    args = ap.parse_args()

    if not args.histories and not args.config:
        ap.error("choose at least one: --histories DIR and/or --config FILE")

    pw = args.password or os.environ.get("N4_PW") or getpass.getpass(f"Password for {args.user}: ")
    c = N4Client(args.host, insecure=args.insecure)
    ok, msg = c.login(args.user, pw, algo=("sha512" if args.sha512 else "sha256"))
    sys.stderr.write(f"[login] {msg}\n")
    if not ok:
        return 1

    if args.histories:
        export_histories(c, args.host, args.histories, args.start, args.end, args.limit,
                         args.page_size, incremental=args.incremental, state_path=args.state)
    if args.config:
        dump_config(c, args.config, args.max_depth)
    return 0


if __name__ == "__main__":
    sys.exit(main())
