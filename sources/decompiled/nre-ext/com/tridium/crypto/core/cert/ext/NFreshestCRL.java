package com.tridium.crypto.core.cert.ext;

import org.bouncycastle.asn1.x509.Extension;

public final class NFreshestCRL extends NBaseCRLDistributionPoints {
   NFreshestCRL(Extension extension) {
      super(extension, Extension.freshestCRL);
   }
}
