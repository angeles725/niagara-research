package com.tridium.crypto.core.cert.ext;

import com.tridium.json.JSONObject;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.Extensions;
import org.bouncycastle.asn1.x509.KeyUsage;

public final class NKeyUsage extends NX509Extension {
   private final KeyUsage keyUsage;

   public static NKeyUsage make(boolean isCritical, int usage) throws IOException {
      KeyUsage ku = new KeyUsage(usage);
      Extension kuExt = new Extension(Extension.keyUsage, isCritical, ku.toASN1Primitive().getEncoded("DER"));
      return new NKeyUsage(kuExt);
   }

   NKeyUsage(Extension extension) {
      super(extension);
      if (!extension.getExtnId().equals(Extension.keyUsage)) {
         throw new IllegalArgumentException("extension is not a KeyUsage extension");
      }

      this.keyUsage = KeyUsage.fromExtensions(new Extensions(extension));
      if (this.keyUsage == null) {
         throw new IllegalArgumentException("extension is not a KeyUsage extension");
      }
   }

   public Set<String> getFlags() {
      Set<String> flags = new HashSet<>();
      if (this.keyUsage.hasUsages(128)) {
         flags.add("digitalSignature");
      }

      if (this.keyUsage.hasUsages(64)) {
         flags.add("nonRepudiation");
      }

      if (this.keyUsage.hasUsages(32)) {
         flags.add("keyEncipherment");
      }

      if (this.keyUsage.hasUsages(16)) {
         flags.add("dataEncipherment");
      }

      if (this.keyUsage.hasUsages(8)) {
         flags.add("keyAgreement");
      }

      if (this.keyUsage.hasUsages(4)) {
         flags.add("keyCertSign");
      }

      if (this.keyUsage.hasUsages(2)) {
         flags.add("cRLSign");
      }

      if (this.keyUsage.hasUsages(1)) {
         flags.add("encipherOnly");
      }

      if (this.keyUsage.hasUsages(32768)) {
         flags.add("decipherOnly");
      }

      return Collections.unmodifiableSet(flags);
   }

   public int getKeyUsageValue() {
      int keyUsageValue = 0;
      if (this.keyUsage.hasUsages(128)) {
         keyUsageValue |= 128;
      }

      if (this.keyUsage.hasUsages(64)) {
         keyUsageValue |= 64;
      }

      if (this.keyUsage.hasUsages(32)) {
         keyUsageValue |= 32;
      }

      if (this.keyUsage.hasUsages(16)) {
         keyUsageValue |= 16;
      }

      if (this.keyUsage.hasUsages(8)) {
         keyUsageValue |= 8;
      }

      if (this.keyUsage.hasUsages(4)) {
         keyUsageValue |= 4;
      }

      if (this.keyUsage.hasUsages(2)) {
         keyUsageValue |= 2;
      }

      if (this.keyUsage.hasUsages(1)) {
         keyUsageValue |= 1;
      }

      if (this.keyUsage.hasUsages(32768)) {
         keyUsageValue |= 32768;
      }

      return keyUsageValue;
   }

   public boolean hasFlag(int bit) {
      return this.keyUsage.hasUsages(bit);
   }

   @Override
   protected void appendJSON(JSONObject obj) {
      if (this.keyUsage.hasUsages(128)) {
         obj.append("flags", "digitalSignature");
      }

      if (this.keyUsage.hasUsages(64)) {
         obj.append("flags", "nonRepudiation");
      }

      if (this.keyUsage.hasUsages(32)) {
         obj.append("flags", "keyEncipherment");
      }

      if (this.keyUsage.hasUsages(16)) {
         obj.append("flags", "dataEncipherment");
      }

      if (this.keyUsage.hasUsages(8)) {
         obj.append("flags", "keyAgreement");
      }

      if (this.keyUsage.hasUsages(4)) {
         obj.append("flags", "keyCertSign");
      }

      if (this.keyUsage.hasUsages(2)) {
         obj.append("flags", "cRLSign");
      }

      if (this.keyUsage.hasUsages(1)) {
         obj.append("flags", "encipherOnly");
      }

      if (this.keyUsage.hasUsages(32768)) {
         obj.append("flags", "decipherOnly");
      }
   }

   @Override
   public String encodeToString() {
      JSONObject obj = new JSONObject();
      obj.put("oid", this.getOid().getId());
      obj.put("isCritical", this.isCritical());
      JSONObject valObj = new JSONObject();
      valObj.put("flags", this.getKeyUsageValue());
      obj.put("value", valObj);
      return obj.toString();
   }

   static NKeyUsage doDecodeFromString(String val) throws IOException {
      try {
         boolean isCritical = false;
         JSONObject obj = new JSONObject(val);
         if (obj.has("oid") && new ASN1ObjectIdentifier(obj.getString("oid")).equals(Extension.keyUsage)) {
            if (obj.has("isCritical")) {
               isCritical = obj.getBoolean("isCritical");
            }

            JSONObject valObj = obj.getJSONObject("value");
            int usage = valObj.getInt("flags");
            return make(isCritical, usage);
         }
      } catch (Exception e) {
         if (e instanceof IOException) {
            throw e;
         }

         throw new IOException("error decoding NKeyUsage from string", e);
      }

      throw new IOException("error decoding NKeyUsage from string");
   }
}
