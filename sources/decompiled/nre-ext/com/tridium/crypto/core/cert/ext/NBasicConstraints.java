package com.tridium.crypto.core.cert.ext;

import com.tridium.json.JSONObject;
import java.io.IOException;
import java.math.BigInteger;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.Extensions;

public final class NBasicConstraints extends NX509Extension {
   private final BasicConstraints basicConstraints;

   public static NBasicConstraints make(boolean isCritical, boolean isCA) throws IOException {
      BasicConstraints bc = new BasicConstraints(isCA);
      Extension bcExt = new Extension(Extension.basicConstraints, isCritical, bc.toASN1Primitive().getEncoded("DER"));
      return new NBasicConstraints(bcExt);
   }

   public static NBasicConstraints make(boolean isCritical, int pathLen) throws IOException {
      BasicConstraints bc = new BasicConstraints(pathLen);
      Extension bcExt = new Extension(Extension.basicConstraints, isCritical, bc.toASN1Primitive().getEncoded("DER"));
      return new NBasicConstraints(bcExt);
   }

   NBasicConstraints(Extension extension) {
      super(extension);
      if (!extension.getExtnId().equals(Extension.basicConstraints)) {
         throw new IllegalArgumentException("extension is not a BasicConstraints extension");
      }

      this.basicConstraints = BasicConstraints.fromExtensions(new Extensions(extension));
      if (this.basicConstraints == null) {
         throw new IllegalArgumentException("extension is not a BasicConstraints extension");
      }
   }

   public boolean isCA() {
      return this.basicConstraints.isCA();
   }

   public BigInteger getPathLenConstraint() {
      return this.basicConstraints.getPathLenConstraint();
   }

   @Override
   protected void appendJSON(JSONObject obj) {
      obj.put("isCA", this.isCA());
      if (this.basicConstraints.getPathLenConstraint() != null) {
         obj.put("pathLenConstraint", this.basicConstraints.getPathLenConstraint().intValue());
      }
   }

   @Override
   public String encodeToString() {
      JSONObject obj = new JSONObject();
      obj.put("oid", this.getOid().getId());
      obj.put("isCritical", this.isCritical());
      JSONObject valObj = new JSONObject();
      valObj.put("isCA", this.isCA());
      if (this.getPathLenConstraint() != null) {
         valObj.put("pathLenConstraint", this.getPathLenConstraint().intValue());
      }

      obj.put("value", valObj);
      return obj.toString();
   }

   static NBasicConstraints doDecodeFromString(String val) throws IOException {
      try {
         boolean isCritical = false;
         boolean isCA = false;
         int pathLen = -1;
         JSONObject obj = new JSONObject(val);
         if (obj.has("oid") && new ASN1ObjectIdentifier(obj.getString("oid")).equals(Extension.basicConstraints)) {
            if (obj.has("isCritical")) {
               isCritical = obj.getBoolean("isCritical");
            }

            JSONObject valObj = obj.getJSONObject("value");
            if (valObj.has("isCA")) {
               isCA = valObj.getBoolean("isCA");
            }

            if (valObj.has("pathLenConstraint")) {
               pathLen = valObj.getInt("pathLenConstraint");
            }

            if (pathLen >= 0) {
               return make(isCritical, pathLen);
            }

            return make(isCritical, isCA);
         }
      } catch (Exception e) {
         if (e instanceof IOException) {
            throw e;
         }

         throw new IOException("error decoding NBasicConstraints from string", e);
      }

      throw new IOException("error decoding NBasicConstraints from string");
   }
}
