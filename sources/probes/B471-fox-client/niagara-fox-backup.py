#!/usr/bin/env python3
"""
niagara-fox-backup.py — pull a station backup (.dist ZIP incl. config.bog) over Fox, no Workbench.

Extends the B471 login with the Fox CIRCUIT layer (B134 §134.9) and the `backup` channel (B472):
  login (SCRAM) -> open circuit on channel 'backup' cmd 'backup'
  -> writeMessage {save=f}  (save=false => READ-ONLY, no Station.saveSync)
  -> readMessage (resp)  -> read the streamed ZIP from the circuit  -> write .dist

Byte-accurate frame codec (the ZIP rides as FoxBlob chunks and contains \\n/{/} bytes).
Stdlib only. Credentials out-of-band ($N4_PW). SECRETS DISCIPLINE: the .dist is handled as a file;
its config.bog body is never printed — only sha256 + byte count + zip listing are reported.
"""
import argparse, os, sys, ssl, socket, getpass, base64, hashlib, hmac, unicodedata, zipfile, io

def _uprep(u):
    u = unicodedata.normalize("NFKC", u); return u.replace("=", "=3D").replace(",", "=2C")
def _xor(a, b): return bytes(x ^ y for x, y in zip(a, b))

class Scram:
    def __init__(self, user, pw):
        self.user=user; self.pw=pw
        self.cnonce=base64.b64encode(os.urandom(16)).decode()
        self.cfb=f"n={_uprep(user)},r={self.cnonce}"; self.client_first="n,,"+self.cfb
    def client_final(self, sf):
        m={k:v for k,v in (p.split("=",1) for p in sf.split(","))}
        salt=base64.b64decode(m["s"]); it=int(m["i"]); rn=m["r"]
        sp=hashlib.pbkdf2_hmac("sha256", unicodedata.normalize("NFKC",self.pw).encode(), salt, it, 32)
        ck=hmac.new(sp,b"Client Key",hashlib.sha256).digest(); sk=hashlib.sha256(ck).digest()
        cfwp=f"c=biws,r={rn}"; am=f"{self.cfb},{sf},{cfwp}".encode()
        proof=_xor(ck, hmac.new(sk,am,hashlib.sha256).digest())
        return f"{cfwp},p="+base64.b64encode(proof).decode()

def esc(s):
    o=[]
    for ch in s:
        c=ord(ch); o.append(ch if (0x20<=c<=0x7f and ch!='#') else "#%x;"%c)
    return "".join(o)

class Fox:
    def __init__(self, host, port, verbose):
        self.verbose=verbose
        raw=socket.create_connection((host,port),timeout=20)
        ctx=ssl.create_default_context(); ctx.check_hostname=False; ctx.verify_mode=ssl.CERT_NONE
        self.s=ctx.wrap_socket(raw, server_hostname=host)
        self.rf=self.s.makefile("rb"); self.seq=1
    def log(self,m):
        if self.verbose: sys.stderr.write(m+"\n")
    # ---- writing ----
    def _tuple_bytes(self, name, t, v):
        if t=='b':   # blob: name=b:<len>[<rawbytes>]
            return name.encode()+b"=b:"+str(len(v)).encode()+b"["+v+b"]"
        if t=='m':   # nested message bytes already built
            return name.encode()+b"=m:"+v
        return (f"{name}={t}:{v}").encode()
    def send(self, ftype, channel, command, tuples):
        body=b"{\n"
        for (n,t,v) in tuples:
            body+=self._tuple_bytes(n,t,v)+b"\n"
        body+=b"}"
        frame=f"fox {ftype} {self.seq} -1 {channel} {command}\n".encode()+body+b";;\n"
        self.log(f"  >> fox {ftype} {self.seq} -1 {channel} {command}  ({len(body)}B body)")
        self.s.sendall(frame); self.seq+=1
    def send_tuning(self, cmd, tuples): self.send('a','fox',cmd,tuples)
    # ---- reading (byte-accurate) ----
    def _readline(self):
        return self.rf.readline()
    def _read_exact(self, n):
        buf=b""
        while len(buf)<n:
            c=self.rf.read(n-len(buf))
            if not c: raise EOFError("eof")
            buf+=c
        return buf
    def read_frame(self):
        # header line
        while True:
            line=self._readline()
            if not line: raise EOFError("connection closed")
            if line.startswith(b"fox "): break
        parts=line.rstrip(b"\n").split(b" ")
        ftype=parts[1].decode(); seq=parts[2].decode(); reply=parts[3].decode()
        channel=parts[4].decode(); command=(parts[5].decode() if len(parts)>5 else "")
        if ftype=='k':
            return ('k',seq,reply,channel,command,{})
        body=self._read_message()   # parse {...}
        # footer ;;\n
        foot=self._read_exact(3)
        while foot!=b";;\n":
            foot=foot[1:]+self._read_exact(1)
        self.log(f"  << fox {ftype} {seq} {reply} {channel} {command}  ({len(body)} tuples)")
        return (ftype,seq,reply,channel,command,body)
    def _read_message(self):
        # expects '{' then \n then tuples then '}'
        b=self._read_exact(1)
        while b in (b"\n",b" "): b=self._read_exact(1)
        assert b==b"{", f"expected {{ got {b!r}"
        # consume optional \n
        body={}
        while True:
            # peek next non-newline byte
            ch=self._read_exact(1)
            if ch==b"\n": continue
            if ch==b"}": break
            # read name until '='
            name=ch
            while True:
                c=self._read_exact(1)
                if c==b"=": break
                name+=c
            t=self._read_exact(1)   # type char
            colon=self._read_exact(1)  # ':'
            assert colon==b":", f"expected : got {colon!r}"
            if t==b"b":
                # <len>[<bytes>]
                num=b""
                c=self._read_exact(1)
                while c!=b"[": num+=c; c=self._read_exact(1)
                ln=int(num); raw=self._read_exact(ln)
                assert self._read_exact(1)==b"]"
                body[name.decode()]=("b",raw)
                # consume trailing \n
                nl=self._read_exact(1)
                if nl!=b"\n": pass
            elif t==b"o":
                # FoxObject: <encoding> <len>[<rawbytes>]
                enc=b""
                c=self._read_exact(1)
                while c!=b" ": enc+=c; c=self._read_exact(1)
                num=b""; c=self._read_exact(1)
                while c!=b"[": num+=c; c=self._read_exact(1)
                ln=int(num); raw=self._read_exact(ln)
                assert self._read_exact(1)==b"]"
                body[name.decode()]=("o",(enc.decode(),raw))
                nl=self._read_exact(1)
            elif t==b"m":
                sub=self._read_message()   # recursive; leading '{' already? we consumed name=..:  -> next is '{'
                body[name.decode()]=("m",sub)
                nl=self._read_exact(1)
            else:
                val=b""
                c=self._read_exact(1)
                while c!=b"\n": val+=c; c=self._read_exact(1)
                body[name.decode()]=(t.decode(), val.decode("utf-8","replace"))
        return body

def main():
    ap=argparse.ArgumentParser()
    ap.add_argument("host"); ap.add_argument("-u","--user",required=True)
    ap.add_argument("--port",type=int,default=4911)
    ap.add_argument("-o","--output",required=True,help="output .dist path")
    ap.add_argument("-q","--quiet",action="store_true")
    args=ap.parse_args()
    pw=os.environ.get("N4_PW") or getpass.getpass(f"Password for {args.user}: ")
    fx=Fox(args.host,args.port,not args.quiet)
    sys.stderr.write(f"[fox] TLS connected {args.host}:{args.port}\n")

    # ---- login ----
    fx.send_tuning("hello",[("fox.version","s","1.0.2"),("id","i","0"),("n4Id","s","0"),
                            ("hostName","s","research"),("hostAddress","s","127.0.0.1"),("app.name","s","FoxProbe")])
    fx.read_frame()                      # server hello
    fx.read_frame()                      # kerberos
    fx.send_tuning("username",[("username","s",esc(args.user)),("kerbKey","z","f")])
    *_,ch=fx.read_frame()                # challenge
    if "keyExchangeMethods" in ch:
        fx.send_tuning("clientKeyExchangeMethod",[("keyExchangeMethod","s",ch["keyExchangeMethods"][1].split(":")[0])])
    sc=Scram(args.user,pw)
    fx.send_tuning("authMessage1",[("authInput","s","authInputScram"),("authHandshake1","s",esc(sc.client_first))])
    *_,am1=fx.read_frame(); server_first=am1["authHandshake1"][1]
    fx.send_tuning("authMessage2",[("authHandshake2","s",esc(sc.client_final(server_first)))])
    fx.read_frame()                      # authMessage2 reply
    # read welcome
    ft,_,_,_,cmd,_=fx.read_frame()
    if cmd!="welcome":
        sys.stderr.write(f"[fox] login not welcome: {cmd}\n"); return 5
    sys.stderr.write("[fox] AUTHENTICATED\n")

    # ---- open backup circuit ----
    cid="1"   # first client circuit (odd)
    fx.send('a','circuit','open',[("id","i",cid),("channel","s","backup"),("command","s","backup"),("metadata","m",b"{\n}")])
    # write request message {save=f} into the circuit as a stream frame (blob = serialized msg bytes)
    reqmsg=b"{\nsave=z:f\n}"
    fx.send('a','circuit','stream',[("id","i",cid),("data","b",reqmsg)])
    sys.stderr.write("[fox] backup circuit opened, sent save=false request; reading stream...\n")

    # ---- read streamed bytes for our circuit ----
    stream=bytearray(); got_close=False; frames=0
    while not got_close:
        try:
            ftype,seq,reply,channel,command,body=fx.read_frame()
        except EOFError:
            break
        if channel=="circuit" and command=="stream" and body.get("id",("i",""))[1]==cid:
            data=body.get("data",("b",b""))[1]
            stream+=data; frames+=1
        elif channel=="circuit" and command=="close" and body.get("id",("i",""))[1]==cid:
            got_close=True
        elif channel=="circuit" and command=="open":
            pass
        # ignore fox/hello, keepalive, etc.
        if frames>0 and len(stream)>0 and frames%20==0:
            sys.stderr.write(f"[fox] ...{len(stream)} bytes in {frames} chunks\n")
        if len(stream)>80_000_000:  # safety cap
            sys.stderr.write("[fox] size cap hit\n"); break

    sys.stderr.write(f"[fox] circuit stream ended: {len(stream)} bytes, {frames} chunks, close={got_close}\n")

    # ---- split resp FoxMessage off the front, remainder = ZIP ----
    # resp is a small {...} FoxMessage; find its terminating '}' at depth 0
    buf=bytes(stream)
    start=buf.find(b"{")
    if start<0:
        sys.stderr.write("[fox] no response message found\n"); return 6
    depth=0; end=-1
    for i in range(start,len(buf)):
        if buf[i:i+1]==b"{": depth+=1
        elif buf[i:i+1]==b"}":
            depth-=1
            if depth==0: end=i; break
    resp=buf[start:end+1] if end>0 else b""
    sys.stderr.write(f"[fox] resp message: {resp[:120]!r}\n")
    zip_bytes=buf[end+1:] if end>0 else b""
    # a ZIP starts with 'PK'
    pk=zip_bytes.find(b"PK\x03\x04")
    if pk<0:
        sys.stderr.write(f"[fox] no ZIP signature in stream (first bytes {zip_bytes[:16]!r})\n")
        open(args.output+".raw","wb").write(buf)
        return 7
    zip_bytes=zip_bytes[pk:]
    open(args.output,"wb").write(zip_bytes)
    h=hashlib.sha256(zip_bytes).hexdigest()
    sys.stderr.write(f"[fox] WROTE {args.output} : {len(zip_bytes)} bytes sha256={h}\n")
    # list contents (names + sizes only, never bodies) — SECRETS DISCIPLINE
    try:
        z=zipfile.ZipFile(io.BytesIO(zip_bytes))
        print(f"ZIP OK: {len(z.namelist())} entries")
        for n in z.namelist()[:40]:
            info=z.getinfo(n)
            print(f"  {info.file_size:>10}  {n}")
        print("FOX-BACKUP-OK")
    except Exception as e:
        sys.stderr.write(f"[fox] zip parse issue: {e}\n")
    return 0

if __name__=="__main__":
    sys.exit(main())
