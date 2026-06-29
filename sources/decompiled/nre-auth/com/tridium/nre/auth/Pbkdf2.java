package com.tridium.nre.auth;

import com.tridium.nre.security.KeyDerivationAlgorithmBundle;
import java.security.SecureRandom;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import javax.baja.nre.util.ByteArrayUtil;
import javax.baja.nre.util.TextUtil;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class Pbkdf2 {
   private static final Pbkdf2.EncodingCalculationCache CALC_CACHE = new Pbkdf2.EncodingCalculationCache(100);
   private static final boolean DEBUG = false;

   public static byte[] deriveKey(byte[] salt, int nof_iterations, String pass, KeyDerivationAlgorithmBundle algorithmBundle) throws Exception {
      return deriveKey(salt, nof_iterations, pass.toCharArray(), algorithmBundle);
   }

   public static byte[] deriveKey(byte[] salt, int nof_iterations, char[] pass, KeyDerivationAlgorithmBundle algorithmBundle) throws Exception {
      if (nof_iterations <= 0) {
         throw new IllegalArgumentException("invalid iteration count");
      } else if (salt.length <= 0) {
         throw new IllegalArgumentException("invalid salt length");
      } else {
         int dkLen = algorithmBundle.getKeyLength();
         if (dkLen <= 0) {
            throw new IllegalArgumentException("invalid key length");
         } else {
            byte[] cacheKey = (TextUtil.bytesToHexString(salt) + ":" + nof_iterations + ":" + new String(pass) + ":" + dkLen).getBytes();
            byte[] dk;
            synchronized (CALC_CACHE) {
               cacheKey = CALC_CACHE.hashCacheKey(cacheKey);
               dk = CALC_CACHE.get(ByteArrayUtil.toHexString(cacheKey));
            }

            try {
               if (dk != null) {
                  return dk;
               } else {
                  PBEKeySpec keySpec = new PBEKeySpec(pass, salt, nof_iterations, algorithmBundle.getKeyLength());
                  SecretKeyFactory key = SecretKeyFactory.getInstance(algorithmBundle.getKeyDerivationAlgorithmName());
                  dk = key.generateSecret(keySpec).getEncoded();
                  synchronized (CALC_CACHE) {
                     CALC_CACHE.put(ByteArrayUtil.toHexString(cacheKey), dk);
                  }

                  return dk;
               }
            } catch (Exception var12) {
               throw new SecurityException("Could not derive key: " + var12);
            }
         }
      }
   }

   public static class EncodingCalculationCache extends LinkedHashMap<String, byte[]> {
      private final int capacity;
      private static final byte[] CACHE_SALT = new byte[16];

      public EncodingCalculationCache(int capacity) {
         super(capacity + 1, 1.1F, true);
         this.capacity = capacity;
      }

      public byte[] hashCacheKey(byte[] cacheKey) throws Exception {
         byte[] saltedCacheKey = new byte[CACHE_SALT.length + cacheKey.length];
         System.arraycopy(CACHE_SALT, 0, saltedCacheKey, 0, CACHE_SALT.length);
         System.arraycopy(cacheKey, 0, saltedCacheKey, CACHE_SALT.length, cacheKey.length);
         Mac hmacSha256 = Mac.getInstance("HmacSha256");
         SecretKey macKey = new SecretKeySpec(saltedCacheKey, "HmacSHA256");
         hmacSha256.init(macKey);
         return hmacSha256.doFinal();
      }

      @Override
      protected boolean removeEldestEntry(Entry<String, byte[]> eldest) {
         return this.size() > this.capacity;
      }

      static {
         new SecureRandom().nextBytes(CACHE_SALT);
      }
   }
}
