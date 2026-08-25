package com.tridium.crypto.core.cert.ext;

import com.tridium.crypto.core.util.OidMap;
import com.tridium.json.JSONArray;
import com.tridium.json.JSONObject;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.x509.CertificatePolicies;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.Extensions;
import org.bouncycastle.asn1.x509.PolicyInformation;
import org.bouncycastle.asn1.x509.PolicyQualifierInfo;

public final class NCertificatePolicies extends NX509Extension {
   private final CertificatePolicies certificatePolicies;

   NCertificatePolicies(Extension extension) {
      super(extension);
      if (!extension.getExtnId().equals(Extension.certificatePolicies)) {
         throw new IllegalArgumentException("extension is not a CertificatePolicies extension");
      }

      this.certificatePolicies = CertificatePolicies.fromExtensions(new Extensions(extension));
      if (this.certificatePolicies == null) {
         throw new IllegalArgumentException("extension is not a CertificatePolicies extension");
      }
   }

   public Set<PolicyInformation> getPolicyInformation() {
      Set<PolicyInformation> infoSet = new HashSet<>(Arrays.asList(this.certificatePolicies.getPolicyInformation()));
      return Collections.unmodifiableSet(infoSet);
   }

   @Override
   protected void appendJSON(JSONObject obj) {
      for (PolicyInformation policyInformation : this.certificatePolicies.getPolicyInformation()) {
         JSONObject piObj = new JSONObject();
         piObj.put("policyIdentifier", OidMap.get(policyInformation.getPolicyIdentifier()));
         ASN1Sequence seq = policyInformation.getPolicyQualifiers();
         if (seq != null) {
            for (Object aSeq : seq) {
               JSONArray pqsObj = new JSONArray();
               ASN1Sequence qualifiers = (ASN1Sequence)aSeq;
               if (qualifiers != null) {
                  JSONObject pqiObj = new JSONObject();
                  PolicyQualifierInfo pqi = PolicyQualifierInfo.getInstance(qualifiers);
                  pqiObj.put("oid", OidMap.get(pqi.getPolicyQualifierId()));
                  this.parsePrimitive(pqi.getQualifier().toASN1Primitive(), pqiObj);
                  pqsObj.put(pqiObj);
               }

               piObj.put("policyQualifiers", pqsObj);
            }
         }

         obj.append("policyInformation", piObj);
      }
   }
}
