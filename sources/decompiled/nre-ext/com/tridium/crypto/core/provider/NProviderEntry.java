package com.tridium.crypto.core.provider;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Base64;

public class NProviderEntry implements IProviderEntry {
   private String key;
   private String value;

   public static NProviderEntry make(String key, String value) {
      return new NProviderEntry(key, value);
   }

   public static NProviderEntry decodeFromString(String encoded) throws IOException {
      byte[] data = Base64.getDecoder().decode(encoded);
      ByteArrayInputStream bin = new ByteArrayInputStream(data);
      ObjectInputStream in = new ObjectInputStream(bin);
      String key = in.readUTF();
      String value = in.readUTF();
      return new NProviderEntry(key, value);
   }

   private NProviderEntry(String key, String value) {
      this.key = key;
      this.value = value;
   }

   @Override
   public String getKey() {
      return this.key;
   }

   @Override
   public String getValue() {
      return this.value;
   }

   public String encodeToString() throws Exception {
      ByteArrayOutputStream bout = new ByteArrayOutputStream();
      ObjectOutputStream out = new ObjectOutputStream(bout);
      out.writeUTF(this.key);
      out.writeUTF(this.value);
      out.close();
      bout.close();
      return Base64.getEncoder().encodeToString(bout.toByteArray());
   }

   @Override
   public int hashCode() {
      int prime = 31;
      int result = 1;
      result = 31 * result + (this.key == null ? 0 : this.key.hashCode());
      return 31 * result + (this.value == null ? 0 : this.value.hashCode());
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj) {
         return true;
      }

      if (obj == null) {
         return false;
      }

      if (!(obj instanceof NProviderEntry)) {
         return false;
      }

      NProviderEntry other = (NProviderEntry)obj;
      if (this.key == null) {
         if (other.key != null) {
            return false;
         }
      } else if (!this.key.equals(other.key)) {
         return false;
      }

      if (this.value == null) {
         if (other.value != null) {
            return false;
         }
      } else if (!this.value.equals(other.value)) {
         return false;
      }

      return true;
   }
}
