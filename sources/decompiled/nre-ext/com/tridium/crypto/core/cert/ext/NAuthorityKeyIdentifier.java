package com.tridium.crypto.core.cert.ext;

import com.tridium.crypto.core.cert.CertificateParseException;
import com.tridium.crypto.core.cert.NGeneralName;
import com.tridium.json.JSONArray;
import com.tridium.json.JSONObject;
import com.tridium.json.JSONUtil;
import java.io.IOException;
import java.math.BigInteger;
import javax.baja.nre.security.IX509Certificate;
import javax.baja.nre.util.ByteArrayUtil;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.x509.AuthorityKeyIdentifier;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.Extensions;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.GeneralNamesBuilder;

public final class NAuthorityKeyIdentifier extends NX509Extension {
   private final AuthorityKeyIdentifier authorityKeyIdentifier;

   public static NAuthorityKeyIdentifier make(boolean isCritical, IX509Certificate authorityCert) throws IOException {
      GeneralName authorityName = new GeneralName(authorityCert.getIssuerDN());
      BigInteger serialNumber = authorityCert.getSerialNumber();
      NSubjectKeyIdentifier subjectKeyIdentifier = (NSubjectKeyIdentifier)authorityCert.getExtension(Extension.subjectKeyIdentifier);
      AuthorityKeyIdentifier aki;
      if (subjectKeyIdentifier != null) {
         aki = new AuthorityKeyIdentifier(subjectKeyIdentifier.getKeyIdentifier(), new GeneralNames(authorityName), serialNumber);
      } else {
         aki = new AuthorityKeyIdentifier(new GeneralNames(authorityName), serialNumber);
      }

      Extension akiExt = new Extension(Extension.authorityKeyIdentifier, isCritical, aki.toASN1Primitive().getEncoded("DER"));
      return new NAuthorityKeyIdentifier(akiExt);
   }

   NAuthorityKeyIdentifier(Extension extension) {
      super(extension);
      if (!extension.getExtnId().equals(Extension.authorityKeyIdentifier)) {
         throw new IllegalArgumentException("extension is not an AuthorityKeyIdentifier");
      }

      this.authorityKeyIdentifier = AuthorityKeyIdentifier.fromExtensions(new Extensions(extension));
      if (this.authorityKeyIdentifier == null) {
         throw new IllegalArgumentException("extension is not an AuthorityKeyIdentifier");
      }
   }

   public byte[] getKeyIdentifier() {
      return this.authorityKeyIdentifier.getKeyIdentifier();
   }

   public BigInteger getAuthorityCertSerialNumber() {
      return this.authorityKeyIdentifier.getAuthorityCertSerialNumber();
   }

   public GeneralNames getAuthorityCertIssuer() {
      return this.authorityKeyIdentifier.getAuthorityCertIssuer();
   }

   @Override
   protected void appendJSON(JSONObject obj) {
      if (this.authorityKeyIdentifier.getKeyIdentifier() != null) {
         obj.put("keyIdentifier", ByteArrayUtil.toHexString(this.authorityKeyIdentifier.getKeyIdentifier()));
      }

      if (this.authorityKeyIdentifier.getAuthorityCertSerialNumber() != null) {
         obj.putOpt("authorityCertSerialNumber", this.authorityKeyIdentifier.getAuthorityCertSerialNumber());
      }

      if (this.authorityKeyIdentifier.getAuthorityCertIssuer() != null) {
         GeneralName[] names = this.authorityKeyIdentifier.getAuthorityCertIssuer().getNames();

         for (GeneralName name : names) {
            obj.accumulate("authorityCertIssuer", NGeneralName.make(name).getJSON());
         }
      }
   }

   @Override
   public String encodeToString() {
      JSONObject obj = new JSONObject();
      obj.put("oid", this.getOid().getId());
      obj.put("isCritical", this.isCritical());
      JSONObject valObj = new JSONObject();
      if (this.authorityKeyIdentifier.getKeyIdentifier() != null) {
         valObj.put("keyIdentifier", ByteArrayUtil.toHexString(this.authorityKeyIdentifier.getKeyIdentifier()));
      }

      valObj.putOpt("authorityCertSerialNumber", this.authorityKeyIdentifier.getAuthorityCertSerialNumber().toString());
      GeneralName[] names = this.authorityKeyIdentifier.getAuthorityCertIssuer().getNames();

      for (GeneralName name : names) {
         valObj.append("authorityCertIssuer", new JSONObject(NGeneralName.make(name).encodeToString()));
      }

      obj.put("value", valObj);
      return obj.toString();
   }

   static NAuthorityKeyIdentifier doDecodeFromString(String val) throws IOException, CertificateParseException {
      try {
         boolean isCritical = false;
         byte[] keyIdentifier = null;
         BigInteger authorityCertSerialNumber = null;
         GeneralNamesBuilder namesBuilder = new GeneralNamesBuilder();
         JSONObject obj = new JSONObject(val);
         if (obj.has("oid") && new ASN1ObjectIdentifier(obj.getString("oid")).equals(Extension.authorityKeyIdentifier)) {
            if (obj.has("isCritical")) {
               isCritical = obj.getBoolean("isCritical");
            }

            JSONObject valObj = obj.getJSONObject("value");
            if (valObj.has("keyIdentifier")) {
               keyIdentifier = ByteArrayUtil.hexStringToBytes(valObj.getString("keyIdentifier"));
            }

            if (valObj.has("authorityCertSerialNumber")) {
               authorityCertSerialNumber = new BigInteger(valObj.getString("authorityCertSerialNumber"));
            }

            JSONArray namesArr = valObj.getJSONArray("authorityCertIssuer");

            for (int i = 0; i < namesArr.length(); i++) {
               try {
                  namesBuilder.addName(NGeneralName.decodeFromString(JSONUtil.getString(namesArr, i)).getName());
               } catch (Exception e) {
                  throw new CertificateParseException("authorityCertIssuer", JSONUtil.getString(namesArr, i));
               }
            }

            AuthorityKeyIdentifier aki = new AuthorityKeyIdentifier(keyIdentifier, namesBuilder.build(), authorityCertSerialNumber);
            Extension akiExt = new Extension(Extension.authorityKeyIdentifier, isCritical, aki.toASN1Primitive().getEncoded("DER"));
            return new NAuthorityKeyIdentifier(akiExt);
         }
      } catch (Exception e) {
         if (!(e instanceof IOException) && !(e instanceof CertificateParseException)) {
            throw new IOException("error decoding NAuthorityKeyIdentifier from string", e);
         }

         throw e;
      }

      throw new IOException("error decoding NAuthorityKeyIdentifier from string");
   }
}
