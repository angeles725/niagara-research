package com.tridium.crypto.core.cert.ext;

import com.tridium.crypto.core.util.OidMap;
import com.tridium.json.JSONArray;
import com.tridium.json.JSONObject;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.Extensions;
import org.bouncycastle.asn1.x509.KeyPurposeId;

public final class NExtendedKeyUsage extends NX509Extension {
   private static final KeyPurposeId[] EMPTY_PURPOSES_LIST = new KeyPurposeId[0];
   private final ExtendedKeyUsage extendedKeyUsage;

   public static NExtendedKeyUsage make(boolean isCritical, Collection<KeyPurposeId> purposes) throws IOException {
      return make(isCritical, purposes.toArray(EMPTY_PURPOSES_LIST));
   }

   public static NExtendedKeyUsage make(boolean isCritical, KeyPurposeId... id) throws IOException {
      ExtendedKeyUsage eku = new ExtendedKeyUsage(id);
      Extension ekuExt = new Extension(Extension.extendedKeyUsage, isCritical, eku.toASN1Primitive().getEncoded("DER"));
      return new NExtendedKeyUsage(ekuExt);
   }

   NExtendedKeyUsage(Extension extension) {
      super(extension);
      if (!extension.getExtnId().equals(Extension.extendedKeyUsage)) {
         throw new IllegalArgumentException("extension is not an ExtendedKeyUsage extension");
      }

      this.extendedKeyUsage = ExtendedKeyUsage.fromExtensions(new Extensions(extension));
      if (this.extendedKeyUsage == null) {
         throw new IllegalArgumentException("extension is not an ExtendedKeyUsage extension");
      }
   }

   public boolean hasPurpose(KeyPurposeId keyPurposeId) {
      return this.extendedKeyUsage.hasKeyPurposeId(keyPurposeId);
   }

   public Set<KeyPurposeId> getPurposes() {
      Set<KeyPurposeId> purposes = new HashSet<>(Arrays.asList(this.extendedKeyUsage.getUsages()));
      return Collections.unmodifiableSet(purposes);
   }

   public Set<String> getPurposeStrings() {
      Set<String> purposeStrings = new HashSet<>();

      for (KeyPurposeId purposeId : this.extendedKeyUsage.getUsages()) {
         purposeStrings.add(OidMap.get(purposeId.toOID()));
      }

      return Collections.unmodifiableSet(purposeStrings);
   }

   @Override
   protected void appendJSON(JSONObject obj) {
      for (KeyPurposeId id : this.extendedKeyUsage.getUsages()) {
         obj.append("purposes", OidMap.get(id.toOID()));
      }
   }

   @Override
   public String encodeToString() {
      JSONObject obj = new JSONObject();
      obj.put("oid", this.getOid().getId());
      obj.put("isCritical", this.isCritical());
      JSONObject valObj = new JSONObject();

      for (KeyPurposeId id : this.extendedKeyUsage.getUsages()) {
         valObj.append("purposes", id.toOID().getId());
      }

      obj.put("value", valObj);
      return obj.toString();
   }

   static NExtendedKeyUsage doDecodeFromString(String val) throws IOException {
      try {
         boolean isCritical = false;
         JSONObject obj = new JSONObject(val);
         if (obj.has("oid") && new ASN1ObjectIdentifier(obj.getString("oid")).equals(Extension.extendedKeyUsage)) {
            if (obj.has("isCritical")) {
               isCritical = obj.getBoolean("isCritical");
            }

            JSONObject valObj = obj.getJSONObject("value");
            Set<KeyPurposeId> purposes = new HashSet<>();
            JSONArray usagesArr = valObj.getJSONArray("purposes");

            for (int i = 0; i < usagesArr.length(); i++) {
               ASN1ObjectIdentifier id = new ASN1ObjectIdentifier(usagesArr.getString(i));
               purposes.add(KeyPurposeId.getInstance(id));
            }

            return make(isCritical, purposes);
         }
      } catch (Exception e) {
         if (e instanceof IOException) {
            throw e;
         }

         throw new IOException("error decoding NExtendedKeyUsage from string", e);
      }

      throw new IOException("error decoding NExtendedKeyUsage from string");
   }
}
