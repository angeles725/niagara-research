package com.tridium.crypto.core.provider;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.Provider;
import java.security.Security;
import java.util.Base64;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

public class NProvider implements IProvider {
   private final String name;
   private final double version;
   private final String description;
   private Hashtable<String, NProviderSection> sections = new Hashtable<>();
   private static Vector<NProvider> nproviders;

   public static synchronized Enumeration<? extends IProvider> getProviderElements() {
      Provider[] providers = Security.getProviders();
      if (nproviders == null || nproviders.size() != providers.length) {
         nproviders = new Vector<>();

         for (Provider provider : providers) {
            nproviders.add(make(provider));
         }
      }

      return nproviders.elements();
   }

   public static synchronized NProvider[] getProviders() {
      Provider[] providers = Security.getProviders();
      if (nproviders == null || nproviders.size() != providers.length) {
         nproviders = new Vector<>();

         for (Provider provider : providers) {
            nproviders.add(make(provider));
         }
      }

      return nproviders.toArray(new NProvider[0]);
   }

   public static NProvider getProvider(String name) {
      return make(Security.getProvider(name));
   }

   public static NProvider make(Provider provider) {
      return new NProvider(provider);
   }

   public static NProvider decodeFromString(String encoded) throws Exception {
      byte[] data = Base64.getDecoder().decode(encoded);
      ByteArrayInputStream bin = new ByteArrayInputStream(data);
      DataInputStream in = new DataInputStream(bin);
      String name = in.readUTF();
      double version = in.readDouble();
      String description = in.readUTF();
      NProvider nprovider = new NProvider(name, version, description);
      int size = in.readInt();

      for (int i = 0; i < size; i++) {
         int sectionSize = in.readInt();
         byte[] sectionBytes = new byte[sectionSize];
         if (in.read(sectionBytes, 0, sectionSize) != sectionSize) {
            throw new IllegalArgumentException("unable to decode section");
         }

         NProviderSection section = NProviderSection.decodeFromString(new String(sectionBytes, StandardCharsets.UTF_8));
         nprovider.addSection(section);
      }

      return nprovider;
   }

   private NProvider(String name, double version, String description) {
      this.name = name;
      this.version = version;
      this.description = description;
   }

   private NProvider(Provider provider) {
      this.name = provider.getName();
      double tempVersion = provider.getVersion();
      this.version = tempVersion;
      this.description = provider.getInfo();

      for (Object o : provider.keySet()) {
         String entry = (String)o;
         String subentry = entry;
         if (entry.startsWith("Alg.Alias.")) {
            subentry = entry.substring("Alg.Alias.".length());
         }

         String factoryClass = subentry.substring(0, subentry.indexOf(46));
         String name = subentry.substring(factoryClass.length() + 1);
         NProviderSection section;
         if (!this.sections.containsKey(factoryClass)) {
            section = NProviderSection.make(factoryClass);
            this.sections.put(factoryClass, section);
         } else {
            section = this.sections.get(factoryClass);
         }

         section.add(NProviderEntry.make(name, provider.getProperty(entry)));
      }
   }

   private void addSection(NProviderSection section) {
      this.sections.put(section.getName(), section);
   }

   @Override
   public String getName() {
      return this.name;
   }

   @Override
   public double getVersion() {
      return this.version;
   }

   @Override
   public String getDescription() {
      return this.description;
   }

   @Override
   public Enumeration<? extends IProviderSection> sections() {
      return this.sections.elements();
   }

   public String encodeToString() throws Exception {
      ByteArrayOutputStream bout = new ByteArrayOutputStream();
      DataOutputStream out = new DataOutputStream(bout);
      out.writeUTF(this.name);
      out.writeDouble(this.version);
      out.writeUTF(this.description);
      out.writeInt(this.sections.size());
      Enumeration<NProviderSection> tsections = this.sections.elements();

      while (tsections.hasMoreElements()) {
         NProviderSection section = tsections.nextElement();
         byte[] sectionBytes = section.encodeToString().getBytes(StandardCharsets.UTF_8);
         out.writeInt(sectionBytes.length);
         out.write(sectionBytes);
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

      if (!(obj instanceof NProvider)) {
         return false;
      }

      NProvider other = (NProvider)obj;
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
