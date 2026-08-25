package com.tridium.crypto.core.cert.ext;

import com.tridium.json.JSONObject;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.x509.Extension;

public final class NInhibitAnyPolicy extends NX509Extension {
   private final int skipCerts;

   NInhibitAnyPolicy(Extension extension) {
      super(extension);
      if (!extension.getExtnId().equals(Extension.inhibitAnyPolicy)) {
         throw new IllegalArgumentException("extension is not an InhibitAnyPolicy extension");
      }

      ASN1Integer asn1Int = ASN1Integer.getInstance(extension.getExtnValue().getOctets());
      if (asn1Int == null) {
         throw new IllegalArgumentException("extension is not an InhibitAnyPolicy extension");
      }

      this.skipCerts = asn1Int.getValue().intValue();
   }

   public int getSkipCerts() {
      return this.skipCerts;
   }

   @Override
   protected void appendJSON(JSONObject obj) {
      obj.put("skipCerts", this.skipCerts);
   }
}
