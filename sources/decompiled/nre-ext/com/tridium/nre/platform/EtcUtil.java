package com.tridium.nre.platform;

import com.tridium.nre.security.SecretBytes;
import com.tridium.nre.security.SecretChars;
import com.tridium.nre.security.SystemPassphrase;
import com.tridium.nre.security.km.KeyMaterial;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public final class EtcUtil {
   static final String KEY_MATERIAL_KEY = ".km";
   static final String SYSTEM_PASSWORD_KEY = ".sp";
   public static final String ETC_NIAGARA_PATH = "/etc/niagara";
   static final String OLD_SP_KEY = "systempw";
   static final String OLD_KM_KEY = ".km";
   static final String LEGACY_NIAGARA_CONF_PATH = "/etc/niagara/niagara.conf";

   public static synchronized int set(String fileName, SecretChars value) {
      if (".sp".equals(fileName)) {
         SystemPassphrase.checkSystemPassphrasePermissions();
      } else if (".km".equals(fileName)) {
         KeyMaterial.checkKeyMaterialPermissions();
      }

      File file = new File("/etc/niagara", fileName);

      try (
         FileOutputStream out = new FileOutputStream(file);
         SecretBytes bytes = value.asSecretBytes();
      ) {
         out.write(bytes.get());
         return 0;
      } catch (IOException e) {
         return -1;
      }
   }

   public static synchronized int set(String fileName, String value) {
      try (SecretChars chars = SecretChars.fromString(value)) {
         return set(fileName, chars);
      }
   }

   public static synchronized String get(String fileName) {
      SecretChars chars = getSecretChars(fileName);
      return chars != null ? chars.asString(true) : null;
   }

   public static synchronized SecretChars getSecretChars(String fileName) {
      if (".sp".equals(fileName)) {
         SystemPassphrase.checkSystemPassphrasePermissions();
      } else if (".km".equals(fileName)) {
         KeyMaterial.checkKeyMaterialPermissions();
      }

      File file = new File("/etc/niagara", fileName);

      try (FileInputStream in = new FileInputStream(file)) {
         byte[] bytes = new byte[(int)file.length()];
         in.read(bytes);
         return SecretChars.fromSecretBytes(new SecretBytes(bytes, true), StandardCharsets.UTF_8, true);
      } catch (IOException e) {
         return null;
      }
   }

   public static synchronized int remove(String key) {
      new File("/etc/niagara", key).delete();
      return 0;
   }
}
