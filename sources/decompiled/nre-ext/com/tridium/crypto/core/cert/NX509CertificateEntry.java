package com.tridium.crypto.core.cert;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Base64;
import javax.baja.nre.security.IX509Certificate;
import javax.baja.nre.security.IX509CertificateEntry;

public class NX509CertificateEntry implements IX509CertificateEntry {
   private String alias = null;
   private IX509Certificate[] certs = null;
   private PrivateKey key = null;

   public static NX509CertificateEntry make(String alias, X509Certificate[] certs, PrivateKey key) {
      return new NX509CertificateEntry(alias, certs, key);
   }

   public static NX509CertificateEntry make(String alias, IX509Certificate[] certs, PrivateKey key) {
      return new NX509CertificateEntry(alias, certs, key);
   }

   public static NX509CertificateEntry make(String encoded) throws Exception {
      return decodeFromString(encoded);
   }

   public NX509CertificateEntry(String alias, X509Certificate[] certs, PrivateKey key) {
      this.alias = alias;
      this.key = key;
      NX509Certificate[] ncerts = new NX509Certificate[certs.length];

      for (int i = 0; i < certs.length; i++) {
         ncerts[i] = NX509Certificate.make(certs[i]);
      }

      this.certs = ncerts;
   }

   public NX509CertificateEntry(String alias, IX509Certificate[] certs, PrivateKey key) {
      this.alias = alias;
      this.certs = certs;
      this.key = key;
   }

   @Override
   public IX509Certificate getCertificate(int i) {
      return this.certs[i];
   }

   @Override
   public X509Certificate[] getCertificates() {
      if (this.certs != null && this.certs.length > 0) {
         X509Certificate[] certs = new X509Certificate[this.certs.length];

         for (int i = 0; i < this.certs.length; i++) {
            certs[i] = this.certs[i].getCertificate();
         }

         return certs;
      } else {
         return new X509Certificate[0];
      }
   }

   @Override
   public String getAlias() {
      return this.alias;
   }

   @Override
   public PrivateKey getPrivateKey() {
      return this.key;
   }

   @Override
   public String encodeToString() throws Exception {
      return encodeToString(this);
   }

   @Override
   public int hashCode() {
      int prime = 31;
      int result = 1;
      result = 31 * result + (this.alias == null ? 0 : this.alias.hashCode());
      result = 31 * result + Arrays.hashCode(this.certs);
      return 31 * result + (this.key == null ? 0 : this.key.hashCode());
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj) {
         return true;
      }

      if (obj == null) {
         return false;
      }

      if (!(obj instanceof NX509CertificateEntry)) {
         return false;
      }

      NX509CertificateEntry other = (NX509CertificateEntry)obj;
      if (this.alias == null) {
         if (other.alias != null) {
            return false;
         }
      } else if (!this.alias.equals(other.alias)) {
         return false;
      }

      if (!Arrays.equals(this.certs, other.certs)) {
         return false;
      }

      if (this.key == null) {
         if (other.key != null) {
            return false;
         }
      } else if (!this.key.equals(other.key)) {
         return false;
      }

      return true;
   }

   public static String encodeToString(NX509CertificateEntry entry) throws Exception {
      ByteArrayOutputStream bout = new ByteArrayOutputStream();
      ObjectOutputStream out = new ObjectOutputStream(bout);
      if (entry.alias != null) {
         out.writeBoolean(true);
         out.writeUTF(entry.getAlias());
      } else {
         out.writeBoolean(false);
      }

      if (entry.certs != null) {
         out.writeInt(entry.certs.length);

         for (int i = 0; i < entry.certs.length; i++) {
            out.writeUTF(entry.certs[i].encodeToString());
         }
      } else {
         out.writeInt(0);
      }

      if (entry.key != null) {
         out.writeBoolean(true);
         out.writeUTF(NKey.encodeToString(entry.getPrivateKey()));
      } else {
         out.writeBoolean(false);
      }

      out.close();
      bout.close();
      return Base64.getEncoder().encodeToString(bout.toByteArray());
   }

   public static NX509CertificateEntry decodeFromString(String encoded) throws Exception {
      byte[] data = Base64.getDecoder().decode(encoded);
      ByteArrayInputStream bin = new ByteArrayInputStream(data);
      ObjectInputStream in = new ObjectInputStream(bin);
      String alias = null;
      NX509Certificate[] certs = null;
      PrivateKey key = null;
      if (in.readBoolean()) {
         alias = in.readUTF();
      }

      int count = in.readInt();
      if (count > 0) {
         certs = new NX509Certificate[count];

         for (int i = 0; i < count; i++) {
            certs[i] = NX509Certificate.make(in.readUTF());
         }
      }

      if (in.readBoolean()) {
         key = (PrivateKey)NKey.decodeFromString(in.readUTF());
      }

      return make(alias, certs, key);
   }
}
