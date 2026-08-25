package com.tridium.nre.util;

public final class DpapiUtil {
   public static byte[] decrypt(byte[] encrypted, boolean isKeyMaterial) {
      return decrypt0(encrypted, isKeyMaterial);
   }

   public static byte[] encrypt(byte[] data, boolean isKeyMaterial, boolean localMachine) {
      return encrypt0(data, isKeyMaterial, localMachine);
   }

   private static native byte[] decrypt0(byte[] var0, boolean var1);

   private static native byte[] encrypt0(byte[] var0, boolean var1, boolean var2);
}
