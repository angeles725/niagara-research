package com.tridium.nre.util;

import com.tridium.nre.security.SecretChars;
import com.tridium.nre.security.SystemPassphrase;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

public final class SyspwUtil {
   private static final String DUMP_PARAMETER = "-d";
   private static final String NEW_PASSPHRASE_PARAMETER = "-p";
   private static final String OLD_PASSPHRASE_PARAMETER = "-o";
   private static final String COMMAND = "/proc/boot/syspwutil";

   public static synchronized SecretChars getSystemPassphrase() {
      SystemPassphrase.checkSystemPassphrasePermissions();
      if (!new File("/proc/boot/syspwutil").exists()) {
         return null;
      }

      String[] commandString = new String[]{"/proc/boot/syspwutil", "-d"};
      SecretChars value = null;

      try {
         Process process = Runtime.getRuntime().exec(commandString);

         try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            value = SecretChars.fromString(reader.readLine());
         }
      } catch (IOException var16) {
      }

      return value;
   }

   public static synchronized boolean setSystemPassphrase(String oldPassphrase, String newPassphrase) {
      SystemPassphrase.checkSystemPassphrasePermissions();
      if (!new File("/proc/boot/syspwutil").exists()) {
         return false;
      }

      String[] commandString = new String[]{"/proc/boot/syspwutil", "-p", newPassphrase, "-o", oldPassphrase};

      try {
         Process process = Runtime.getRuntime().exec(commandString);
         int returnCode = process.waitFor();
         return returnCode == 0;
      } catch (Exception ignored) {
         return false;
      }
   }
}
