package com.tridium.crypto.core.cert.ext;

import com.tridium.json.JSONObject;
import java.text.ParseException;
import java.util.Date;
import org.bouncycastle.asn1.ASN1GeneralizedTime;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.PrivateKeyUsagePeriod;

public final class NPrivateKeyUsagePeriod extends NX509Extension {
   private final PrivateKeyUsagePeriod privateKeyUsagePeriod;

   NPrivateKeyUsagePeriod(Extension extension) {
      super(extension);
      if (!extension.getExtnId().equals(Extension.privateKeyUsagePeriod)) {
         throw new IllegalArgumentException("extension is not a PrivateKeyUsagePeriod extension");
      }

      this.privateKeyUsagePeriod = PrivateKeyUsagePeriod.getInstance(extension.getParsedValue());
      if (this.privateKeyUsagePeriod == null) {
         throw new IllegalArgumentException("extension is not a PrivateKeyUsagePeriod extension");
      }
   }

   public Date getNotBefore() throws ParseException {
      ASN1GeneralizedTime time = this.privateKeyUsagePeriod.getNotBefore();
      return time != null ? time.getDate() : null;
   }

   public Date getNotAfter() throws ParseException {
      ASN1GeneralizedTime time = this.privateKeyUsagePeriod.getNotAfter();
      return time != null ? time.getDate() : null;
   }

   @Override
   protected void appendJSON(JSONObject obj) {
      try {
         if (this.getNotBefore() != null) {
            obj.put("notBefore", this.getNotBefore().toString());
         }
      } catch (Exception e) {
         obj.put("notBefore", "parse error");
      }

      try {
         if (this.getNotAfter() != null) {
            obj.put("notAfter", this.getNotAfter().toString());
         }
      } catch (Exception e) {
         obj.put("notAfter", "parse error");
      }
   }
}
