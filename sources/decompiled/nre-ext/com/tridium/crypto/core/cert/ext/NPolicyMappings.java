package com.tridium.crypto.core.cert.ext;

import com.tridium.crypto.core.util.OidMap;
import com.tridium.json.JSONObject;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.PolicyMappings;

public final class NPolicyMappings extends NX509Extension {
   private final PolicyMappings policyMappings;

   NPolicyMappings(Extension extension) {
      super(extension);
      if (!extension.getExtnId().equals(Extension.policyMappings)) {
         throw new IllegalArgumentException("extension is not a PolicyMappings extension");
      }

      this.policyMappings = PolicyMappings.getInstance(extension.getParsedValue());
      if (this.policyMappings == null) {
         throw new IllegalArgumentException("extension is not an PolicyMappings extension");
      }
   }

   @Override
   protected void appendJSON(JSONObject obj) {
      for (Object aSeq : (ASN1Sequence)this.policyMappings.toASN1Primitive()) {
         JSONObject innerObj = new JSONObject();
         ASN1Sequence inner = (ASN1Sequence)aSeq;
         ASN1ObjectIdentifier issuerDomainPolicy = (ASN1ObjectIdentifier)inner.getObjectAt(0);
         innerObj.put("issuerDomainPolicy", OidMap.get(issuerDomainPolicy.getId()));
         ASN1ObjectIdentifier subjectDomainPolicy = (ASN1ObjectIdentifier)inner.getObjectAt(1);
         innerObj.put("subjectDomainPolicy", OidMap.get(subjectDomainPolicy.getId()));
         obj.append("policyMappings", innerObj);
      }
   }
}
