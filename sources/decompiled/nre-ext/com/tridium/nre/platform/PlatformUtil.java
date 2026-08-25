package com.tridium.nre.platform;

import com.tridium.nre.security.NiagaraBasicPermission;
import com.tridium.nre.security.PBEEncodingKey;
import com.tridium.nre.security.SecretChars;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.security.AccessController;
import java.util.Date;

public class PlatformUtil {
   private static IPlatformProvider loadPlatformProvider() {
      String provider = AccessController.doPrivileged(() -> System.getProperty("niagara.platform.provider"));
      Object providerInstance = null;

      try {
         if (provider == null) {
            System.err
               .println("WARNING [" + new Date() + "][nre] no 'niagara.platform.provider' system property defined, defaulting to Java Platform Provider");
            provider = "com.tridium.nre.platform.NJavaPlatformProvider";
         }

         providerInstance = Class.forName(provider).getDeclaredConstructor().newInstance();
         if (providerInstance instanceof NativePlatformProvider) {
            if (!NativePlatformProvider.load()) {
               System.err.println("SEVERE [" + new Date() + "][nre] failed to initialize native platform provider '" + provider + "'");
               System.exit(-7);
            }
         } else if (providerInstance instanceof JavaPlatformProvider) {
            JavaPlatformProvider javaPlatformProvider = (JavaPlatformProvider)providerInstance;
            if (!javaPlatformProvider.load()) {
               System.err.println("SEVERE [" + new Date() + "][nre] failed to initialize java platform provider '" + provider + "'");
               System.exit(-7);
            }
         } else {
            System.err.println("SEVERE [" + new Date() + "][nre] unsupported platform provider: " + provider);
            System.exit(-7);
         }
      } catch (ClassNotFoundException | InstantiationException | NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
         System.err.println("SEVERE [" + new Date() + "][nre] error loading provider '" + provider + "' (" + e + ")");
         e.printStackTrace();
         System.exit(-7);
      }

      return (IPlatformProvider)providerInstance;
   }

   public static IPlatformProvider getPlatformProvider() {
      NiagaraBasicPermission initPlatformPermission = new NiagaraBasicPermission("GET_PLATFORM_PROVIDER");
      SecurityManager sm = System.getSecurityManager();
      if (sm != null) {
         sm.checkPermission(initPlatformPermission);
      }

      return PlatformUtil.PlatformProviderHolder.PLATFORM_PROVIDER_INSTANCE;
   }

   public static boolean isNativePlatform() {
      return PlatformUtil.PlatformProviderHolder.PLATFORM_PROVIDER_INSTANCE instanceof NativePlatformProvider;
   }

   public static boolean isTridiumPlatform() {
      return PlatformUtil.PlatformProviderHolder.PLATFORM_PROVIDER_INSTANCE instanceof NativePlatformProviderTridium;
   }

   public static boolean isNpsdkPlatform() {
      return PlatformUtil.PlatformProviderHolder.PLATFORM_PROVIDER_INSTANCE instanceof NativePlatformProviderNpsdk;
   }

   public static PBEEncodingKey makePBEKey() throws IOException {
      try (SecretChars secretChars = PlatformUtil.PlatformProviderHolder.PLATFORM_PROVIDER_INSTANCE.getSystemPassword()) {
         return new PBEEncodingKey(secretChars);
      }
   }

   private static final class PlatformProviderHolder {
      private static final IPlatformProvider PLATFORM_PROVIDER_INSTANCE = PlatformUtil.loadPlatformProvider();
   }
}
