#!/usr/bin/env bash
# forge-keys.sh — keypair generation used in the authorized pentest (B317).
# Reproduces BOTH keypairs used:
#   1. DSA-1024/q=224 (openssl 3.x default) — FIRST attempt; produced signatures the
#      platform parser REJECTED with "error decoding signature bytes" (wrong format).
#   2. DSA-1024/q=160 (platform-compatible) — the honest attacker simulation; the
#      platform verified the signature cryptographically: "{invalid: Invalid signature}".
#
# The platform expects DSA with q = 160 bits (SHA-1) and DER signatures with
# 20-byte INTEGERs (e.g. real Tridium sigs decode as 30 2e 02 15 00 ... 02 15 00 ...).

set -euo pipefail
cd "$(dirname "$0")"

# --- Keypair 1: openssl default (q=224) ---
openssl dsaparam -out dsaparam.pem 1024 2>/dev/null
openssl gendsa -out attacker_dsa.pem dsaparam.pem 2>/dev/null
openssl dsa -in attacker_dsa.pem -pubout -outform DER -out attacker_dsa_pub.der 2>/dev/null

# --- Keypair 2: force q=160 (platform-compatible) ---
openssl genpkey -genparam -algorithm DSA \
    -pkeyopt dsa_paramgen_bits:1024 -pkeyopt dsa_paramgen_q_bits:160 \
    -out dsaparam160b.pem
openssl genpkey -paramfile dsaparam160b.pem -out attacker_dsa160.pem

echo "keys written:"
ls -la attacker_dsa.pem attacker_dsa160.pem attacker_dsa_pub.der
