# PROBE — Native interposition/shim surface of the N4 license & signature checks

Question (from peer, Java side covers provider-order/moduleVerificationMode live): does nre.dll/dsfspi
expose an obvious native hook point, and can the license verify be shim-ed WITHOUT touching signed baja.jar?
Method: READ-ONLY rabin2 -i/-E/-I + PE cert-table parse. Binaries in .../N4.14.0.162/bin. Re-anchored (B424).

## Native crypto import surface (rabin2 -i) — [CERT]
- ONLY two native modules import from dsfspi.dll, and each imports EXACTLY ONE symbol:
    njre.dll -> ?checkFileSignature@DsfUtil@@SAHPEAEHPEBDH@Z   (RSA module/dist signature verify)
    nre.dll  -> ?checkFileSignature@DsfUtil@@SAHPEAEHPEBDH@Z
- nre.dll does NOT import DsfSha1WithDsaSignature (the DSA-1024 license/vendor-cert verify). [CERT]
- isFeaturePresent (license text-match gate, B126 §126.6) calls neither — verifies nothing. [CERT, prior probe]

## How the DSA license verify is actually reached — [CERT]
- dsfspi.dll exports 54 JNI natives `Java_com_tridium_dsf_provider_*` (DsfSecurityProvider,
  DsfAes256CipherSpi, DsfDsaKeyPairGeneratorSpi, ...). The DSA license/cert verify path is therefore:
  Java LicenseManager (baja.jar) -> JCE provider DsfSecurityProvider -> JNI -> dsfspi. NO native caller.

## dsfspi.dll integrity — [CERT]
- Authenticode-signed: PE Certificate Table present, size 10520 bytes. A file-swap breaks this signature
  IFF a loader enforces it; standard Windows DLL load does NOT enforce Authenticode by default. [CERT/INFER]

## Interposition verdict — two native surfaces, one chokepoint (dsfspi)
A. checkFileSignature (RSA, modules/dist): dynamically imported by nre.dll AND njre.dll. This IS the
   obvious native hook point — a patched/proxy dsfspi.dll or DLL search-order redirection could make it
   return "valid" without editing signed baja.jar. Gated by: dsfspi being Authenticode-signed (only if
   enforced) + moduleVerificationMode=low live (B398, Java side) already relaxing the module check. [INFER on CERT]
B. License DSA verify (DsfSha1WithDsaSignature): NOT native-imported; only via the 54 JNI exports from
   baja.jar's DsfSecurityProvider. To bypass LICENSE validation via native interposition WITHOUT touching
   baja.jar you would either (1) patch/proxy dsfspi.dll (single crypto chokepoint, dynamically loaded), or
   (2) register a rogue JCE provider ahead of DsfSecurityProvider (provider-order, B441/SP-G9 — Java side). [INFER on CERT]

## Answer to the peer's question
- The text-match gate needs no shim (it checks nothing).
- The real verify (dsfspi DSA over JNI) CAN be interposed natively without editing baja.jar, via a
  patched/proxy dsfspi.dll or a provider-order attack. Both are conditioned on: dsfspi Authenticode
  enforcement (off by default at DLL load) + module/provider verification posture (moduleVerificationMode
  live=low B398; provider order B441). dsfspi.dll is the single native crypto chokepoint for BOTH the RSA
  module check and the DSA license check — the highest-value interposition target on the native side.

## NOT done
- No dynamic proof (no proxy DLL built, no run) — that is a heavier, execution/patching step left to the
  operator's explicit call. This probe is the static shim-SURFACE map only.
