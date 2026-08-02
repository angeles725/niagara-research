#!/usr/bin/env python3
"""make-probes36h.py — the 36-hour `generated`-grace boundary probe (pentest test L-8).

LicenseFile.java:110-113:  if (now < generated - 129600000L /* 36h */) reject.
So a license whose `generated` is up to ~36h in the FUTURE passes the anti-clock-
rollback check; beyond 36h it is rejected.

Live results (2026-08-01, machine clock 2026-08-01 ~20:12 CST, parseDate startOfDay):
  generated=2026-08-02  (~28h ahead) -> PASSED generated, failed at signature
                                        {invalid: Invalid signature}
  generated=2026-08-04  (~52h ahead) -> "Current date is earlier than license generated date"

Date arithmetic trap (learned live): parseDate(..., startOfDay=true) parses the date
as 00:00:00 local, so "+2 days" from a 20:xx clock is only ~28h ahead — still inside
the grace. Use "+3 days" to exceed the 36h window.
"""
import base64
import subprocess

HOST = "Win-4D6F-169B-CEF1-8F57"
KEY = "attacker_dsa160.pem"

def make(name, gen_date):
    body = (f'<license vendor="Tridium" expiration="2030-12-31" hostId="{HOST}" version="4.10" generated="{gen_date}">\n'
            '<feature name="nre">\n</feature>\n</license>\n')
    bp = f"probe7_{name}_body.xml"
    open(bp, "w").write(body)
    raw = subprocess.run(["openssl", "dgst", "-sha1", "-sign", KEY, bp], capture_output=True).stdout
    lic = body.replace("</license>\n", f"<signature>{base64.b64encode(raw).decode()}</signature>\n</license>\n")
    open(f"{name}.license", "w").write(lic)
    print("wrote", name, "generated=", gen_date)

# beyond the 36h grace (>= ~2026-08-03 08:13 CST): must be rejected by the generated check
make("probe7-gen-aug4", "2026-08-04")
