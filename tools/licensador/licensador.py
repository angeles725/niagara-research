#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
REF-License — sistema de licenciamiento firmado propio (implementacion de referencia).

Esquema:  REF1.<base64url(payload JSON canonico)>.<base64url(firma RSA-PSS-SHA256)>
Dispositivo solo conoce la clave publica (raiz embebida). Pipeline de verificacion
all-or-nothing: PARSE -> SIGNATURE -> PRODUCT -> HOST -> TIME -> FEATURE -> REVOCATION.

Diseno completo: analizador-licencias/06-diseno-sistema-licenciamiento-propio.md
Dependencias: python3 + cryptography (pip install cryptography)

Uso:
  licensador.py genkeys  --out-dir DIR [--bits 3072]
  licensador.py issue    --key PRIV.pem --product P --host REF-XXXX
                         [--licensee L] [--expires ISO8601] [--feature NAME[:QTY]]...
  licensador.py verify   --root PUB.pem --file LIC.refl [--host REF-XXXX] [--state STATE.json] [--blacklist BL.refl]
  licensador.py inspect  --file LIC.refl
  licensador.py hostid   [--salt S]
  licensador.py revoke   --key PRIV.pem --serial S --out BL.refl
  licensador.py selftest --tmp DIR
Exit: 0 = verificacion OK; 1 = licencia invalida (imprime el codigo E_*); 2 = error de uso.
"""

import argparse
import base64
import hashlib
import json
import os
import platform
import secrets
import sys
import uuid
from datetime import datetime, timezone, timedelta

from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import padding, rsa

SCHEME = "REF1"
SCHEME_VER = 1
SALT_DEFAULT = b"REF-LICENSE/v1"
SKEW = timedelta(minutes=5)
PSS_SALT_LEN = 32

# Codigos de error del pipeline (documentados en 06-diseno ... §4/§6)
E_PARSE, E_SIGNATURE, E_PRODUCT, E_HOST = "E_PARSE", "E_SIGNATURE", "E_PRODUCT", "E_HOST"
E_TIME_NOT_YET, E_TIME_EXPIRED, E_CLOCK = "E_TIME_NOT_YET", "E_TIME_EXPIRED", "E_CLOCK"
E_FEATURE_LOCKED, E_FEATURE_QTY, E_REVOKED = "E_FEATURE_LOCKED", "E_FEATURE_QTY", "E_REVOKED"
OK = "OK"


# ---------------------------------------------------------------- utilidades

def _b64u(data: bytes) -> str:
    return base64.urlsafe_b64encode(data).rstrip(b"=").decode("ascii")


def _b64u_decode(s: str) -> bytes:
    return base64.urlsafe_b64decode(s + "=" * (-len(s) % 4))


def _canon_json(obj) -> bytes:
    """JSON canonico: claves ordenadas, sin espacios, ASCII. La firma cubre estos bytes exactos."""
    return json.dumps(obj, sort_keys=True, separators=(",", ":"), ensure_ascii=True).encode("ascii")


def _now_iso() -> str:
    return datetime.now(timezone.utc).replace(microsecond=0).isoformat()


def _parse_iso(s: str) -> datetime:
    return datetime.fromisoformat(s)


def _load_private(path: str, password: str = None):
    with open(path, "rb") as f:
        return serialization.load_pem_private_key(f.read(), password=password)


def _load_public(path: str):
    with open(path, "rb") as f:
        return serialization.load_pem_public_key(f.read())


def _sign(private_key, payload: bytes) -> str:
    sig = private_key.sign(
        payload,
        padding.PSS(mgf=padding.MGF1(hashes.SHA256()), salt_length=PSS_SALT_LEN),
        hashes.SHA256(),
    )
    return _b64u(sig)


def _verify_sig(public_key, payload: bytes, sig_b64: str) -> bool:
    try:
        public_key.verify(
            _b64u_decode(sig_b64),
            payload,
            padding.PSS(mgf=padding.MGF1(hashes.SHA256()), salt_length=PSS_SALT_LEN),
            hashes.SHA256(),
        )
        return True
    except Exception:
        return False


def _write_artifact(path: str, payload: dict, private_key) -> str:
    payload_b = _canon_json(payload)
    artifact = f"{SCHEME}.{_b64u(payload_b)}.{_sign(private_key, payload_b)}"
    with open(path, "w", encoding="ascii") as f:
        f.write(artifact + "\n")
    return artifact


# ---------------------------------------------------------------- hostid

def _stable_identifiers() -> list:
    """Identificadores estables de la maquina (orden fijo). En Windows usa el serial de
    volumen del disco de sistema; en Linux usa machine-id + MAC. Produccion Java: elegir
    los equivalentes estables y mantener el MISMO orden + SALT en todas las plataformas."""
    ids = []
    if platform.system() == "Windows":
        try:
            import ctypes
            buf = ctypes.create_unicode_buffer(64)
            ctypes.windll.kernel32.GetVolumeInformationW(
                "C:\\", None, 0, None, None, None, buf, 64)
            ids.append(buf.value)
        except Exception:
            pass
        ids.append(str(uuid.getnode()))
    else:
        for p in ("/etc/machine-id", "/var/lib/dbus/machine-id"):
            try:
                with open(p) as f:
                    ids.append(f.read().strip())
                break
            except OSError:
                continue
        ids.append(str(uuid.getnode()))
    return ids


def ref_host_id(salt: bytes = SALT_DEFAULT) -> str:
    """REF-<12 hex> = 'REF-' + SHA-256(ids estables || SALT)[0:6 bytes]. Deterministico;
    cambia si cambia el hardware (comportamiento documentado: re-emitir licencia)."""
    h = hashlib.sha256(salt)
    for ident in _stable_identifiers():
        h.update(ident.encode("utf-8", "replace"))
    return "REF-" + h.digest()[:6].hex().upper()


# ---------------------------------------------------------------- emision

def issue(private_key, product: str, host: str, features: dict, licensee: str = "",
          expires: str = None, serial: str = None) -> dict:
    claims = {
        "ver": SCHEME_VER,
        "product": product,
        "licensee": licensee,
        "hostId": host,
        "serial": serial or uuid.uuid4().hex.upper(),
        "issued": _now_iso(),
        "expires": expires,                      # ISO8601 UTC o None (perpetua)
        "features": features,                    # {name: {"qty": int|null, "opts": {...}}}
    }
    return claims


def sign_revocation(private_key, serials: list, issued: str = None) -> dict:
    return {
        "ver": SCHEME_VER,
        "type": "revocations",
        "revoked": sorted(serials),
        "issued": issued or _now_iso(),
    }


# ---------------------------------------------------------------- verificacion

def verify(public_key, artifact: str, product: str = None, host: str = None,
           now: datetime = None, state_path: str = None,
           blacklist_artifact: str = None):
    """Corre el pipeline all-or-nothing. Devuelve (code, claims|None).
    - state_path: archivo de estado para anti-reloj (high-water mark lastSeen).
    - blacklist_artifact: artefacto .refl de revocaciones (opcional)."""
    now = now or datetime.now(timezone.utc)
    if now.tzinfo is None:
        now = now.replace(tzinfo=timezone.utc)

    # 1 PARSE
    parts = artifact.strip().split(".")
    if len(parts) != 3 or parts[0] != SCHEME:
        return E_PARSE, None
    try:
        payload = json.loads(_b64u_decode(parts[1]).decode("utf-8"))
        sig_b64 = parts[2]
    except Exception:
        return E_PARSE, None
    if not isinstance(payload, dict) or payload.get("ver") != SCHEME_VER:
        return E_PARSE, None

    # 2 SIGNATURE (sobre los bytes canonicos exactos; se re-serializa para no confiar en el parse)
    try:
        payload_b = _canon_json(payload)
    except Exception:
        return E_PARSE, None
    if not _verify_sig(public_key, payload_b, sig_b64):
        return E_SIGNATURE, None

    # 3 PRODUCT
    if product is not None and payload.get("product") != product:
        return E_PRODUCT, payload

    # 4 HOST
    if host is not None and payload.get("hostId") != host:
        return E_HOST, payload

    # 5 TIME (con skew)
    try:
        issued = _parse_iso(payload["issued"])
        expires = _parse_iso(payload["expires"]) if payload.get("expires") else None
    except Exception:
        return E_PARSE, payload
    if issued > now + SKEW:
        return E_TIME_NOT_YET, payload
    if expires is not None and expires < now - SKEW:
        return E_TIME_EXPIRED, payload

    # 7 REVOCATION (antes de aceptar; blacklist firmada por la misma raiz)
    if blacklist_artifact:
        code, bl = verify(public_key, blacklist_artifact, product=None, host=None,
                          now=now, state_path=None)
        if code == OK and isinstance(bl, dict) and bl.get("type") == "revocations":
            if payload.get("serial") in bl.get("revoked", []):
                return E_REVOKED, payload

    # Anti-reloj (T4): high-water mark persistente
    if state_path:
        last_seen = _read_last_seen(state_path)
        if last_seen is not None and now < last_seen - SKEW:
            return E_CLOCK, payload
        _write_last_seen(state_path, max(last_seen, now) if last_seen else now)

    return OK, payload


def _read_last_seen(path: str):
    try:
        with open(path) as f:
            return _parse_iso(json.load(f)["lastSeen"])
    except Exception:
        return None


def _write_last_seen(path: str, ts: datetime):
    with open(path, "w") as f:
        json.dump({"lastSeen": ts.replace(microsecond=0).isoformat()}, f)


def check_feature(claims: dict, name: str, qty: int = 1):
    """Gating de runtime. Devuelve (code, disponible_qty)."""
    feats = claims.get("features") or {}
    if name not in feats:
        return E_FEATURE_LOCKED, 0
    f = feats[name]
    fqty = f.get("qty") if isinstance(f, dict) else None
    if fqty is None:                       # null = ilimitado
        return OK, None
    if qty > fqty:
        return E_FEATURE_QTY, fqty
    return OK, fqty - qty


# ---------------------------------------------------------------- CLI

def cmd_genkeys(args):
    os.makedirs(args.out_dir, exist_ok=True)
    key = rsa.generate_private_key(public_exponent=65537, key_size=args.bits)
    priv_path = os.path.join(args.out_dir, "root-private.pem")
    pub_path = os.path.join(args.out_dir, "root-public.pem")
    with open(priv_path, "wb") as f:
        f.write(key.private_bytes(
            serialization.Encoding.PEM,
            serialization.PrivateFormat.PKCS8,
            # Lab: sin passphrase por comodidad. Produccion: cifrar (BestAvailableEncryption)
            # y seguir la ceremonia de claves del §7 del diseno.
            serialization.NoEncryption(),
        ))
    with open(pub_path, "wb") as f:
        f.write(key.public_key().public_bytes(
            serialization.Encoding.PEM,
            serialization.PublicFormat.SubjectPublicKeyInfo,
        ))
    print(f"raiz generada: {priv_path} (PRIVADA — no distribuir) / {pub_path} (publica — embebida)")
    return 0


def _parse_features(items):
    feats = {}
    for it in items:
        name, _, qty = it.partition(":")
        feats[name] = {"qty": int(qty) if qty else None, "opts": {}}
    return feats


def cmd_issue(args):
    key = _load_private(args.key)
    claims = issue(key, args.product, args.host, _parse_features(args.feature),
                   licensee=args.licensee, expires=args.expires)
    with open(args.out, "w", encoding="ascii") as f:
        f.write(f"{SCHEME}.{_b64u(_canon_json(claims))}.{_sign(key, _canon_json(claims))}\n")
    print(f"licencia emitida: {args.out}  serial={claims['serial']}  host={claims['hostId']}  expires={claims['expires']}")
    return 0


def cmd_verify(args):
    root = _load_public(args.root)
    with open(args.file, encoding="ascii") as f:
        artifact = f.read()
    host = args.host or ref_host_id()
    code, claims = verify(root, artifact, product=args.product, host=host,
                          state_path=args.state, blacklist_artifact=args.blacklist)
    print(f"host: {host}")
    if code != OK:
        print(f"RESULTADO: {code}")
        if claims:
            print(f"claims: serial={claims.get('serial')} product={claims.get('product')} "
                  f"hostId={claims.get('hostId')} expires={claims.get('expires')}")
        return 1
    print(f"RESULTADO: {OK}  product={claims['product']} licensee={claims.get('licensee')} "
          f"serial={claims['serial']} features={sorted(claims.get('features', {}))}")
    return 0


def cmd_inspect(args):
    with open(args.file, encoding="ascii") as f:
        artifact = f.read().strip()
    parts = artifact.split(".")
    if len(parts) != 3 or parts[0] != SCHEME:
        print("no es un artefacto REF1 valido"); return 1
    try:
        claims = json.loads(_b64u_decode(parts[1]))
    except Exception as e:
        print(f"payload ilegible: {e}"); return 1
    print(json.dumps(claims, indent=2, ensure_ascii=False))
    return 0


def cmd_hostid(args):
    print(ref_host_id(args.salt.encode()))
    return 0


def cmd_revoke(args):
    key = _load_private(args.key)
    claims = sign_revocation(key, [args.serial])
    with open(args.out, "w", encoding="ascii") as f:
        f.write(f"{SCHEME}.{_b64u(_canon_json(claims))}.{_sign(key, _canon_json(claims))}\n")
    print(f"blacklist firmada: {args.out}  revoked={args.serial}")
    return 0


def cmd_selftest(args):
    """Matriz de aceptacion A1..A10 (diseno §10). Exit 0 si todas pasan."""
    tmp = args.tmp
    os.makedirs(tmp, exist_ok=True)
    key = rsa.generate_private_key(public_exponent=65537, key_size=args.bits)
    root_pub = key.public_key()
    host = "REF-0123456789AB"
    now = datetime(2026, 8, 2, 12, 0, 0, tzinfo=timezone.utc)
    state = os.path.join(tmp, "state.json")

    def art(claims):
        b = _canon_json(claims)
        return f"{SCHEME}.{_b64u(b)}.{_sign(key, b)}"

    def claims(**over):
        base = dict(ver=1, product="reflow-oven", licensee="lab", hostId=host,
                    serial=uuid.uuid4().hex.upper(), issued="2026-08-01T00:00:00+00:00",
                    expires="2027-08-01T00:00:00+00:00",
                    features={"pid": {"qty": 4, "opts": {}}, "logging": {"qty": None, "opts": {}}})
        base.update(over)
        return base

    def v(a, **kw):
        kw.setdefault("now", now)
        return verify(root_pub, a, product="reflow-oven", host=host, **kw)[0]

    results = []
    def expect(name, code, got):
        ok = code == got
        results.append((name, code, got, ok))
        return ok

    # A1 positivo
    a1 = art(claims())
    expect("A1 valida", OK, v(a1, state_path=state))
    # A2 host distinto
    expect("A2 host ajeno", E_HOST, v(art(claims(hostId="REF-FFFFFFFFFFFF"))))
    # A3 tamper payload (1 byte)
    bad = a1.split("."); payload = bytearray(_b64u_decode(bad[1])); payload[4] ^= 0x01
    bad[1] = _b64u(bytes(payload))
    expect("A3 tamper payload", E_SIGNATURE, v(".".join(bad)))
    # A4 expirada
    expect("A4 expirada", E_TIME_EXPIRED, v(art(claims(expires="2026-01-01T00:00:00+00:00"))))
    # A5 feature no incluido
    _, cl = verify(root_pub, art(claims()), product="reflow-oven", host=host, now=now)
    expect("A5 feature faltante", E_FEATURE_LOCKED, check_feature(cl, "nope")[0])
    # A6 qty insuficiente
    expect("A6 qty insuficiente", E_FEATURE_QTY, check_feature(cl, "pid", qty=9)[0])
    expect("A6b qty justa", OK, check_feature(cl, "pid", qty=4)[0])
    expect("A6c qty ilimitada", OK, check_feature(cl, "logging", qty=999)[0])
    # A7 revocada
    rev_claims = claims(serial="DEADBEEF001")
    bl = _write_artifact(os.path.join(tmp, "bl.refl"),
                         sign_revocation(key, [rev_claims["serial"]],
                                         issued="2026-08-01T00:00:00+00:00"), key)
    expect("A7 revocada", E_REVOKED, v(art(rev_claims), blacklist_artifact=bl))
    # A8 reloj atrasado (high-water mark ya avanzo)
    future = now + timedelta(days=30)
    v(art(claims()), now=future, state_path=state)          # lastSeen -> futuro
    expect("A8 rollback reloj", E_CLOCK, v(art(claims()), now=now, state_path=state))
    # A9 producto distinto
    expect("A9 producto ajeno", E_PRODUCT, v(art(claims(product="other"))))
    # A10 corrupto
    expect("A10 corrupto", E_PARSE, v("REF1.not-base64!!.sig"))
    expect("A10b formato", E_PARSE, v("garbage"))

    print(f"{'test':<22}{'esperado':<18}{'obtenido':<18}  ok")
    allok = True
    for name, code, got, ok in results:
        allok &= ok
        print(f"{name:<22}{code:<18}{got:<18}  {'PASS' if ok else 'FAIL'}")
    print(f"\n{sum(1 for r in results if r[3])}/{len(results)} PASS")
    return 0 if allok else 1


def main(argv=None):
    p = argparse.ArgumentParser(prog="licensador.py", description="REF-License — licenciamiento firmado propio (referencia)")
    sub = p.add_subparsers(dest="cmd", required=True)

    g = sub.add_parser("genkeys", help="genera raiz privada + publica")
    g.add_argument("--out-dir", required=True); g.add_argument("--bits", type=int, default=3072)
    g.set_defaults(fn=cmd_genkeys)

    i = sub.add_parser("issue", help="emite una licencia .refl")
    i.add_argument("--key", required=True); i.add_argument("--product", required=True)
    i.add_argument("--host", required=True); i.add_argument("--licensee", default="")
    i.add_argument("--expires", default=None); i.add_argument("--out", required=True)
    i.add_argument("--feature", action="append", default=[], help="NAME o NAME:QTY (repetible)")
    i.set_defaults(fn=cmd_issue)

    vv = sub.add_parser("verify", help="verifica una licencia (pipeline completo)")
    vv.add_argument("--root", required=True); vv.add_argument("--file", required=True)
    vv.add_argument("--product", default=None); vv.add_argument("--host", default=None)
    vv.add_argument("--state", default=None); vv.add_argument("--blacklist", default=None)
    vv.set_defaults(fn=cmd_verify)

    ins = sub.add_parser("inspect", help="muestra los claims sin verificar")
    ins.add_argument("--file", required=True); ins.set_defaults(fn=cmd_inspect)

    h = sub.add_parser("hostid", help="calcula el REF-... de esta maquina")
    h.add_argument("--salt", default=SALT_DEFAULT.decode()); h.set_defaults(fn=cmd_hostid)

    r = sub.add_parser("revoke", help="firma una blacklist de seriales")
    r.add_argument("--key", required=True); r.add_argument("--serial", required=True)
    r.add_argument("--out", required=True); r.set_defaults(fn=cmd_revoke)

    s = sub.add_parser("selftest", help="matriz de aceptacion A1..A10")
    s.add_argument("--tmp", required=True); s.add_argument("--bits", type=int, default=3072)
    s.set_defaults(fn=cmd_selftest)

    args = p.parse_args(argv)
    try:
        return args.fn(args)
    except Exception as e:
        print(f"error: {e}", file=sys.stderr)
        return 2


if __name__ == "__main__":
    sys.exit(main())
