package com.tridium.crypto.core.provider;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Enumeration;
import java.util.Vector;

public class NProviderSection implements IProviderSection {
   private final String name;
   private Vector<NProviderEntry> entries = new Vector<>();

   public static NProviderSection make(String name) {
      return new NProviderSection(name);
   }

   public static NProviderSection decodeFromString(String encoded) throws Exception {
      byte[] data = Base64.getDecoder().decode(encoded);
      ByteArrayInputStream bin = new ByteArrayInputStream(data);
      DataInputStream in = new DataInputStream(bin);
      String name = in.readUTF();
      NProviderSection section = make(name);
      int size = in.readInt();

      for (int i = 0; i < size; i++) {
         int entrySize = in.readInt();
         byte[] entryBytes = new byte[entrySize];
         if (in.read(entryBytes) != entrySize) {
            throw new IllegalArgumentException("error decoding section");
         }

         NProviderEntry entry = NProviderEntry.decodeFromString(new String(entryBytes, StandardCharsets.UTF_8));
         section.add(entry);
      }

      return section;
   }

   private NProviderSection(String name) {
      this.name = name;
   }

   public void add(NProviderEntry entry) {
      this.entries.add(entry);
   }

   @Override
   public String getName() {
      return this.name;
   }

   @Override
   public Enumeration<? extends IProviderEntry> entries() {
      return this.entries.elements();
   }

   public String encodeToString() throws Exception {
      ByteArrayOutputStream bout = new ByteArrayOutputStream();
      DataOutputStream out = new DataOutputStream(bout);
      out.writeUTF(this.name);
      out.writeInt(this.entries.size());

      for (NProviderEntry entry : this.entries) {
         byte[] entryBytes = entry.encodeToString().getBytes(StandardCharsets.UTF_8);
         out.writeInt(entryBytes.length);
         out.write(entryBytes);
         out.flush();
      }

      out.close();
      bout.close();
      return Base64.getEncoder().encodeToString(bout.toByteArray());
   }

   @Override
   public int hashCode() {
      int prime = 31;
      int result = 1;
      result = 31 * result + (this.entries == null ? 0 : this.entries.hashCode());
      return 31 * result + (this.name == null ? 0 : this.name.hashCode());
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj) {
         return true;
      }

      if (obj == null) {
         return false;
      }

      if (!(obj instanceof NProviderSection)) {
         return false;
      }

      NProviderSection other = (NProviderSection)obj;
      if (this.entries == null) {
         if (other.entries != null) {
            return false;
         }
      } else if (!this.entries.equals(other.entries)) {
         return false;
      }

      if (this.name == null) {
         if (other.name != null) {
            return false;
         }
      } else if (!this.name.equals(other.name)) {
         return false;
      }

      return true;
   }
}
