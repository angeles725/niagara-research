package com.tridium.crypto.core.cert.ext;

import com.tridium.crypto.core.cert.NGeneralName;
import com.tridium.crypto.core.util.OidMap;
import com.tridium.json.JSONObject;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.x509.CRLDistPoint;
import org.bouncycastle.asn1.x509.DistributionPoint;
import org.bouncycastle.asn1.x509.DistributionPointName;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.ReasonFlags;

public abstract class NBaseCRLDistributionPoints extends NX509Extension {
   protected final CRLDistPoint crlDistPoint;

   NBaseCRLDistributionPoints(Extension extension, ASN1ObjectIdentifier oid) {
      super(extension);
      if (!extension.getExtnId().equals(oid)) {
         throw new IllegalArgumentException("extension is not a distribution point extension: " + OidMap.get(oid));
      }

      this.crlDistPoint = CRLDistPoint.getInstance(extension.getParsedValue());
   }

   @Override
   protected final void appendJSON(JSONObject obj) {
      for (DistributionPoint point : this.crlDistPoint.getDistributionPoints()) {
         JSONObject dpObj = new JSONObject();
         GeneralNames crlIssuer = point.getCRLIssuer();
         if (crlIssuer != null) {
            for (GeneralName name : crlIssuer.getNames()) {
               NGeneralName nName = NGeneralName.make(name);
               dpObj.accumulate("crlIssuer", nName.getJSON());
            }
         }

         ReasonFlags reasonFlags = point.getReasons();
         if (reasonFlags != null) {
            int reasonFlagsValue = reasonFlags.intValue();
            if ((reasonFlagsValue & 128) != 0) {
               dpObj.append("reasons", "unused");
            }

            if ((reasonFlagsValue & 64) != 0) {
               dpObj.append("reasons", "keyCompromise");
            }

            if ((reasonFlagsValue & 32) != 0) {
               dpObj.append("reasons", "cACompromise");
            }

            if ((reasonFlagsValue & 16) != 0) {
               dpObj.append("reasons", "affiliationChanged");
            }

            if ((reasonFlagsValue & 8) != 0) {
               dpObj.append("reasons", "superseded");
            }

            if ((reasonFlagsValue & 4) != 0) {
               dpObj.append("reasons", "cessationOfOperation");
            }

            if ((reasonFlagsValue & 2) != 0) {
               dpObj.append("reasons", "certificateHold");
            }

            if ((reasonFlagsValue & 1) != 0) {
               dpObj.append("reasons", "privilegeWithdrawn");
            }

            if ((reasonFlagsValue & 32768) != 0) {
               dpObj.append("reasons", "aACompromise");
            }
         }

         DistributionPointName name = point.getDistributionPoint();
         if (name != null) {
            if (name.getType() == 0) {
               GeneralNames fullNames = GeneralNames.getInstance(name.getName());
               if (fullNames != null) {
                  for (GeneralName fullName : fullNames.getNames()) {
                     NGeneralName nFullName = NGeneralName.make(fullName);
                     dpObj.accumulate("fullName", nFullName.getJSON());
                  }
               }
            } else {
               dpObj.put("nameRelativeToCRLIssuer", name.getName().toString());
            }
         }

         obj.append("distributionPoints", dpObj);
      }
   }
}
