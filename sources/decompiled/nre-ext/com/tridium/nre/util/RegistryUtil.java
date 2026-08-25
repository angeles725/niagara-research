package com.tridium.nre.util;

import com.tridium.nre.security.SecretBytes;
import com.tridium.nre.security.SecretChars;
import java.nio.charset.StandardCharsets;

public final class RegistryUtil {
   public static byte[] getEncryptedRegistryBytes(String keyName, boolean considerLegacy, boolean isKeyMaterial) throws SecurityException {
      return getEncryptedRegistryString0(keyName, considerLegacy, isKeyMaterial);
   }

   public static String getEncryptedRegistryString(String keyName, boolean considerLegacy, boolean isKeyMaterial) throws SecurityException {
      byte[] bytes = getEncryptedRegistryString0(keyName, considerLegacy, isKeyMaterial);
      return bytes != null ? new String(bytes, StandardCharsets.UTF_8) : null;
   }

   public static SecretChars getEncryptedRegistryChars(String keyName, boolean considerLegacy, boolean isKeyMaterial) throws SecurityException {
      byte[] bytes = getEncryptedRegistryString0(keyName, considerLegacy, isKeyMaterial);
      if (bytes != null) {
         try (SecretBytes secretBytes = new SecretBytes(bytes, true)) {
            return SecretChars.fromSecretBytes(secretBytes, StandardCharsets.UTF_8, true);
         }
      } else {
         return null;
      }
   }

   public static boolean setEncryptedRegistryBytes(String keyName, byte[] newValue, boolean isKeyMaterial) throws SecurityException {
      return setEncryptedRegistryString0(keyName, newValue, isKeyMaterial);
   }

   public static boolean setEncryptedRegistryString(String keyName, String newValue, boolean isKeyMaterial) throws SecurityException {
      return setEncryptedRegistryString0(keyName, newValue == null ? null : newValue.getBytes(StandardCharsets.UTF_8), isKeyMaterial);
   }

   private static native byte[] getEncryptedRegistryString0(String var0, boolean var1, boolean var2);

   private static native boolean setEncryptedRegistryString0(String var0, byte[] var1, boolean var2);
}
