package com.tridium.nre.security;

import java.security.SecureRandom;
import javax.baja.nre.util.ByteArrayUtil;

public final class Aes256PasswordEncoderUtil {
   public static final String DEFAULT_PASSWORD = "[" + NullAlgorithmBundle.getInstance().getAlgorithmName() + "]=";
   public static final AesAlgorithmBundle ALGORITHM_BUNDLE = AesAlgorithmBundle.make(256);
   public static final int IV_SIZE = 16;
   private static final SecureRandom RANDOM = new SecureRandom();
   public static final String SSL_KEY_PASS_KEY = "com.tridium.niagarad.web.sslKeyPass";

   private Aes256PasswordEncoderUtil() {
   }

   public static String encodePassword(KeyRing keyRing, String keyRingAlias, SecretChars password) throws Exception {
      Aes256PasswordManager pwdMgr = Aes256PasswordManager.getManager(keyRing, keyRingAlias);
      byte[] ivBytes = new byte[16];
      RANDOM.nextBytes(ivBytes);

      byte[] encrypted;
      try (SecretBytes passwordChars = password.asSecretBytes()) {
         encrypted = pwdMgr.encrypt(passwordChars.get(), ivBytes, ALGORITHM_BUNDLE.getAesTransformation());
      }

      String[] data = new String[ALGORITHM_BUNDLE.getDataElementCount()];
      data[1] = ByteArrayUtil.toHexString(encrypted);
      data[0] = ByteArrayUtil.toHexString(ivBytes);
      return ALGORITHM_BUNDLE.encode(data);
   }

   public static SecretChars decodePassword(KeyRing keyRing, String keyRingAlias, String encodedEntry) throws Exception {
      Aes256PasswordManager pwdMgr = Aes256PasswordManager.getManager(keyRing, keyRingAlias);
      String[] data = ALGORITHM_BUNDLE.decode(encodedEntry);
      return SecretChars.fromSecretBytes(pwdMgr.decryptSecret(data[1], data[0]));
   }

   public static boolean isDefault(String password) {
      return DEFAULT_PASSWORD.equals(password);
   }
}
