package com.tridium.crypto.core.cert.ext;

import com.tridium.json.JSONObject;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import javax.baja.nre.util.ByteArrayUtil;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.Extensions;
import org.bouncycastle.asn1.x509.SubjectKeyIdentifier;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;

public final class NSubjectKeyIdentifier extends NX509Extension {
   private final SubjectKeyIdentifier subjectKeyIdentifier;

   public static NSubjectKeyIdentifier make(boolean isCritical, PublicKey key) throws IOException, NoSuchAlgorithmException {
      SubjectKeyIdentifier ski = new JcaX509ExtensionUtils().createSubjectKeyIdentifier(key);
      Extension akiExt = new Extension(Extension.subjectKeyIdentifier, isCritical, ski.toASN1Primitive().getEncoded("DER"));
      return new NSubjectKeyIdentifier(akiExt);
   }

   NSubjectKeyIdentifier(Extension extension) {
      super(extension);
      if (!extension.getExtnId().equals(Extension.subjectKeyIdentifier)) {
         throw new IllegalArgumentException("extension is not a SubjectKeyIdentifier extension");
      }

      this.subjectKeyIdentifier = SubjectKeyIdentifier.fromExtensions(new Extensions(extension));
      if (this.subjectKeyIdentifier == null) {
         throw new IllegalArgumentException("extension is not a SubjectKeyIdentifier extension");
      }
   }

   public byte[] getKeyIdentifier() {
      return this.subjectKeyIdentifier.getKeyIdentifier();
   }

   @Override
   protected void appendJSON(JSONObject obj) {
      obj.putOpt("keyIdentifier", ByteArrayUtil.toHexString(this.subjectKeyIdentifier.getKeyIdentifier()));
   }

   @Override
   public String encodeToString() {
      JSONObject obj = new JSONObject();
      obj.put("oid", this.getOid().getId());
      obj.put("isCritical", this.isCritical());
      JSONObject valObj = new JSONObject();
      if (this.subjectKeyIdentifier.getKeyIdentifier() != null) {
         valObj.put("keyIdentifier", ByteArrayUtil.toHexString(this.subjectKeyIdentifier.getKeyIdentifier()));
      }

      obj.put("value", valObj);
      return obj.toString();
   }

   static NSubjectKeyIdentifier doDecodeFromString(String val) throws IOException {
      try {
         boolean isCritical = false;
         byte[] keyIdentifier = null;
         JSONObject obj = new JSONObject(val);
         if (obj.has("oid") && new ASN1ObjectIdentifier(obj.getString("oid")).equals(Extension.subjectKeyIdentifier)) {
            if (obj.has("isCritical")) {
               isCritical = obj.getBoolean("isCritical");
            }

            JSONObject valObj = obj.getJSONObject("value");
            if (valObj.has("keyIdentifier")) {
               keyIdentifier = ByteArrayUtil.hexStringToBytes(valObj.getString("keyIdentifier"));
            }

            SubjectKeyIdentifier ski = new SubjectKeyIdentifier(keyIdentifier);
            Extension skiExt = new Extension(Extension.subjectKeyIdentifier, isCritical, ski.toASN1Primitive().getEncoded("DER"));
            return new NSubjectKeyIdentifier(skiExt);
         }
      } catch (Exception e) {
         if (e instanceof IOException) {
            throw e;
         }

         throw new IOException("error decoding NAuthorityKeyIdentifier from string", e);
      }

      throw new IOException("error decoding NAuthorityKeyIdentifier from string");
   }
}
