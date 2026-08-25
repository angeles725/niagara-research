package com.tridium.crypto.core.exchange;

import com.tridium.nre.platform.PlatformUtil;
import com.tridium.nre.security.KeyDerivationAlgorithmBundle;
import com.tridium.nre.security.NullAlgorithmBundle;
import java.security.AccessController;
import java.util.StringJoiner;

public final class KeyExchange {
   public static final String KEY_EXCHANGE_STATUS_KEY = "keyExchange";
   public static final String KEY_EXCHANGE_STATUS_CONFIRMATION_KEY = "keyExchangeConfirmation";
   public static final String KEY_EXCHANGE_STATUS_VALUE_NONE = "none";
   public static final String KEY_EXCHANGE_STATUS_VALUE_NONE_PENDING_USER = "nonePendingUser";
   public static final String KEY_EXCHANGE_STATUS_VALUE_NONE_PENDING_CONFIRMATION = "nonePendingConfirmation";
   public static final String KEY_EXCHANGE_STATUS_VALUE_SRP6 = "srp6";
   public static final String KEY_EXCHANGE_METHOD_KEY = "keyExchangeMethod";
   public static final String KEY_EXCHANGE_METHODS_KEY = "keyExchangeMethods";
   public static final String KEY_EXCHANGE_CIPHER_KEY = "keyExchangeCipher";
   public static final String KEY_EXCHANGE_CIPHERS_KEY = "keyExchangeCiphers";
   public static final String KEY_EXCHANGE_CIPHER_VALUE_AES_128_1 = "aes-128.1";
   public static final String KEY_EXCHANGE_CIPHER_VALUE_AES_128_2 = "aes-128.2";
   public static final String KEY_EXCHANGE_CIPHER_VALUE_AES_256_1 = "aes-256.1";
   public static final String KEY_EXCHANGE_CIPHER_VALUE_AES_256_2 = "aes-256.2";

   private KeyExchange() {
   }

   public static IKeyExchanger makeClient(KeyDerivationAlgorithmBundle algorithmBundle) {
      return make(algorithmBundle, false);
   }

   public static IKeyExchanger makeServer(KeyDerivationAlgorithmBundle algorithmBundle) {
      return make(algorithmBundle, true);
   }

   private static IKeyExchanger make(KeyDerivationAlgorithmBundle algorithmBundle, boolean isServer) {
      switch (algorithmBundle.getKeyDerivationAlgorithmName()) {
         case "srp6":
            if (algorithmBundle instanceof SRP6AlgorithmBundle) {
               if (isServer) {
                  return new SRP6KeyExchangerServer((SRP6AlgorithmBundle)algorithmBundle);
               }

               return new SRP6KeyExchangerClient((SRP6AlgorithmBundle)algorithmBundle);
            }
         default:
            throw new IllegalArgumentException("Invalid key exchange algorithm name");
      }
   }

   public static String getPreferredKeyExchangeMethods() {
      String preferredMethod = KeyExchange.LocalMetaDataHolder.NIAGARA_KEY_EXCHANGE_PREFERRED_METHODS;
      if (preferredMethod == null) {
         String[] preferredMethods;
         if (KeyExchange.LocalMetaDataHolder.IS_EMBEDDED) {
            preferredMethods = new String[]{
               SRP6AlgorithmBundle.make(1024, "sha512").getAlgorithmName(),
               SRP6AlgorithmBundle.make(2048, "sha512").getAlgorithmName(),
               NullAlgorithmBundle.getInstance().getAlgorithmName()
            };
         } else {
            preferredMethods = new String[]{
               SRP6AlgorithmBundle.make(2048, "sha512").getAlgorithmName(),
               SRP6AlgorithmBundle.make(1024, "sha512").getAlgorithmName(),
               NullAlgorithmBundle.getInstance().getAlgorithmName()
            };
         }

         StringJoiner joiner = new StringJoiner(":");

         for (String method : preferredMethods) {
            joiner.add(method);
         }

         preferredMethod = joiner.toString();
      }

      return preferredMethod;
   }

   public static String getPreferredKeyExchangeCiphers() {
      return "aes-128.2:aes-128.1";
   }

   public static String getPreferredKeyExchangeCiphers256() {
      return "aes-256.2:aes-256.1";
   }

   private static final class LocalMetaDataHolder {
      public static final String NIAGARA_KEY_EXCHANGE_PREFERRED_METHODS = AccessController.doPrivileged(
         () -> System.getProperty("niagara.keyExchange.preferredMethods")
      );
      private static final boolean IS_EMBEDDED = AccessController.doPrivileged(() -> PlatformUtil.getPlatformProvider().isEmbedded());
   }
}
