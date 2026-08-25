package com.tridium.nre.security.km;

import com.tridium.nre.platform.IPlatformProvider;
import com.tridium.nre.platform.PlatformUtil;
import java.io.File;
import java.security.AccessController;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.logging.Level;

public final class KeyMaterialFactory {
   public static final int KEY_SIZE = 32;
   private static final String LEGACY_KM_NAME = ".km";
   private File securityDir = null;
   private KeyMaterial km = null;
   private String keyName = null;
   private static HashMap<KeyMaterialFactory.KeyMaterialParameters, KeyMaterialFactory> factories = new HashMap<>();

   public static KeyMaterialFactory getInstance(File securityDir, String keyName) throws Exception {
      KeyMaterial.checkKeyMaterialPermissions();
      KeyMaterialFactory.KeyMaterialParameters parameters = new KeyMaterialFactory.KeyMaterialParameters(securityDir, keyName);
      KeyMaterialFactory factory = factories.get(parameters);
      if (factory == null) {
         factory = new KeyMaterialFactory(parameters.securityDir, parameters.kmName);
         factories.put(parameters, factory);
      }

      return factory;
   }

   private KeyMaterialFactory(File securityDir, String keyName) throws Exception {
      this.securityDir = securityDir;
      this.keyName = keyName;
   }

   public static byte[] generateNewKeyMaterial() throws Exception {
      KeyMaterial.checkKeyMaterialPermissions();
      byte[] data = new byte[32];
      new SecureRandom().nextBytes(data);
      return data;
   }

   public final KeyMaterial getKeyMaterial() {
      if (this.km == null) {
         this.km = new PlatformKeyMaterial(this.securityDir, this.keyName, false);
         boolean exists = false;

         try {
            exists = this.km.keyExists();
         } catch (Exception e) {
            KeyMaterial.LOG.log(Level.SEVERE, "Error checking for existence of key material", e);
         }

         try {
            if (!exists) {
               if (!KeyMaterialFactory.PlatformProviderHolder.PLATFORM_PROVIDER_INSTANCE.checkForKeyMaterialUpgrade(this.securityDir, this.keyName)) {
                  KeyMaterial.LOG.log(Level.SEVERE, "Check for Key Material upgrade failed");
               }

               exists = this.km.keyExists();
            }
         } catch (Throwable e) {
            KeyMaterial.LOG.log(Level.SEVERE, "Error attempting to upgrade key material", e);
         }

         try {
            if (!exists) {
               KeyMaterial.LOG.finest("Generating new key material");
               this.km.setKeyMaterial(generateNewKeyMaterial());
            }
         } catch (Exception e) {
            KeyMaterial.LOG.log(Level.SEVERE, "Error generating key material", e);
            throw new SecurityException("Could not generate key material", e);
         }
      }

      return this.km;
   }

   private static class KeyMaterialParameters {
      private File securityDir;
      private String kmName;

      KeyMaterialParameters(File securityDir, String kmName) {
         this.securityDir = securityDir;
         this.kmName = kmName;
      }

      @Override
      public boolean equals(Object o) {
         if (this == o) {
            return true;
         }

         if (o != null && this.getClass() == o.getClass()) {
            KeyMaterialFactory.KeyMaterialParameters that = (KeyMaterialFactory.KeyMaterialParameters)o;
            if (this.kmName != null ? this.kmName.equals(that.kmName) : that.kmName == null) {
               return this.securityDir != null ? this.securityDir.equals(that.securityDir) : that.securityDir == null;
            } else {
               return false;
            }
         } else {
            return false;
         }
      }

      @Override
      public int hashCode() {
         int result = this.securityDir != null ? this.securityDir.hashCode() : 0;
         return 31 * result + (this.kmName != null ? this.kmName.hashCode() : 0);
      }
   }

   private static final class PlatformProviderHolder {
      private static final IPlatformProvider PLATFORM_PROVIDER_INSTANCE = AccessController.doPrivileged(PlatformUtil::getPlatformProvider);
   }
}
