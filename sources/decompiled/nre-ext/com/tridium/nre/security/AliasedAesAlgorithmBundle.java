package com.tridium.nre.security;

import java.util.HashMap;
import java.util.Map;

public class AliasedAesAlgorithmBundle extends AbstractAesAlgorithmBundle {
   private static final String NAME = "aliased-aes";
   private static final String VERSION = "2";
   public static final int ALIAS_INDEX = 0;
   public static final int IV_INDEX = 1;
   public static final int CIPHER_INDEX = 2;
   private final int keySize;
   private final String version;
   private static final Map<String, Map<Integer, AliasedAesAlgorithmBundle>> ALGORITHM_BUNDLES = new HashMap<>();

   private AliasedAesAlgorithmBundle(int keySize, String version) {
      this.keySize = keySize;
      this.version = version;
   }

   public static AliasedAesAlgorithmBundle make(int keySize) {
      return make(keySize, "2");
   }

   public static AliasedAesAlgorithmBundle make(int keySize, String version) {
      Map<Integer, AliasedAesAlgorithmBundle> versionMap = ALGORITHM_BUNDLES.get(version);
      if (versionMap == null) {
         throw new IllegalArgumentException("Invalid version for AliasedAesAlgorithmBundle: " + version);
      } else {
         AliasedAesAlgorithmBundle bundle = versionMap.get(keySize);
         if (bundle == null) {
            throw new IllegalArgumentException("Invalid key size for AES: " + keySize);
         } else {
            return bundle;
         }
      }
   }

   @Override
   public String getAlgorithmType() {
      return "aliased-aes-" + this.getKeySize();
   }

   @Override
   public String getAlgorithmVersion() {
      return this.version;
   }

   @Override
   public int getDataElementCount() {
      return 3;
   }

   @Override
   public int getKeySize() {
      return this.keySize;
   }

   @Override
   public int getIvIndex() {
      return 1;
   }

   @Override
   public int getCipherIndex() {
      return 2;
   }

   @Override
   protected String getName() {
      return "aliased-aes";
   }

   @Override
   public String getAesTransformation() {
      return this.getAlgorithmVersion().equals("1") ? "AES/CBC/PKCS5Padding" : "AES/GCM/NoPadding";
   }

   public static AliasedAesAlgorithmBundle getInstance() {
      return make(256);
   }

   static {
      Map<Integer, AliasedAesAlgorithmBundle> version1Map = new HashMap<>();
      version1Map.put(128, new AliasedAesAlgorithmBundle(128, "1"));
      version1Map.put(192, new AliasedAesAlgorithmBundle(192, "1"));
      version1Map.put(256, new AliasedAesAlgorithmBundle(256, "1"));
      Map<Integer, AliasedAesAlgorithmBundle> version2Map = new HashMap<>();
      version2Map.put(128, new AliasedAesAlgorithmBundle(128, "2"));
      version2Map.put(192, new AliasedAesAlgorithmBundle(192, "2"));
      version2Map.put(256, new AliasedAesAlgorithmBundle(256, "2"));
      ALGORITHM_BUNDLES.put("1", version1Map);
      ALGORITHM_BUNDLES.put("2", version2Map);
   }
}
