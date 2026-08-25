package com.tridium.nre.security;

import java.util.HashMap;
import java.util.Map;

public class AesAlgorithmBundle extends AbstractAesAlgorithmBundle {
   private static final String NAME = "aes";
   private static final String VERSION = "2";
   public static final int IV_INDEX = 0;
   public static final int CIPHER_INDEX = 1;
   private final int keySize;
   private final String version;
   private static final Map<String, Map<Integer, AesAlgorithmBundle>> ALGORITHM_BUNDLES = new HashMap<>();

   private AesAlgorithmBundle(int keySize, String version) {
      this.keySize = keySize;
      this.version = version;
   }

   public static AesAlgorithmBundle make(int keySize) {
      return make(keySize, "2");
   }

   public static AesAlgorithmBundle make(int keySize, String version) {
      Map<Integer, AesAlgorithmBundle> versionMap = ALGORITHM_BUNDLES.get(version);
      if (versionMap == null) {
         throw new IllegalArgumentException("Invalid version for AesAlgorithmBundle: " + version);
      } else {
         AesAlgorithmBundle bundle = versionMap.get(keySize);
         if (bundle == null) {
            throw new IllegalArgumentException("Invalid key size for AES: " + keySize);
         } else {
            return bundle;
         }
      }
   }

   @Override
   public String getAlgorithmVersion() {
      return this.version;
   }

   @Override
   public int getDataElementCount() {
      return 2;
   }

   @Override
   public int getKeySize() {
      return this.keySize;
   }

   @Override
   public int getIvIndex() {
      return 0;
   }

   @Override
   public int getCipherIndex() {
      return 1;
   }

   @Override
   protected String getName() {
      return "aes";
   }

   @Override
   public String getAesTransformation() {
      return this.getAlgorithmVersion().equals("1") ? "AES/CBC/PKCS5Padding" : "AES/GCM/NoPadding";
   }

   public static AesAlgorithmBundle getInstance() {
      return make(256);
   }

   static {
      Map<Integer, AesAlgorithmBundle> version1Map = new HashMap<>();
      version1Map.put(128, new AesAlgorithmBundle(128, "1"));
      version1Map.put(192, new AesAlgorithmBundle(192, "1"));
      version1Map.put(256, new AesAlgorithmBundle(256, "1"));
      Map<Integer, AesAlgorithmBundle> version2Map = new HashMap<>();
      version2Map.put(128, new AesAlgorithmBundle(128, "2"));
      version2Map.put(192, new AesAlgorithmBundle(192, "2"));
      version2Map.put(256, new AesAlgorithmBundle(256, "2"));
      ALGORITHM_BUNDLES.put("1", version1Map);
      ALGORITHM_BUNDLES.put("2", version2Map);
   }
}
