package com.tridium.crypto.core.bundle;

import com.tridium.crypto.core.exchange.SRP6AlgorithmBundle;
import com.tridium.nre.auth.BCryptAlgorithmBundle;
import com.tridium.nre.auth.GlibcSha256CryptAlgorithmBundle;
import com.tridium.nre.auth.GlibcSha512CryptAlgorithmBundle;
import com.tridium.nre.auth.NiagaraStationAlgorithmBundle;
import com.tridium.nre.auth.QnxPlatformAlgorithmBundle;
import com.tridium.nre.security.AesAlgorithmBundle;
import com.tridium.nre.security.AliasedAesAlgorithmBundle;
import com.tridium.nre.security.NullAlgorithmBundle;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;

public abstract class CryptographicAlgorithmBundle {
   public static final String NAME_START = "[";
   public static final String NAME_END = "]=";
   public static final String DATA_DELIMITER = ":";
   public static final String VERSION_DELIMITER = ".";
   private static final Map<String, CryptographicAlgorithmBundle> map = new HashMap<>();

   public final String getAlgorithmName() {
      return this.getAlgorithmType() + "." + this.getAlgorithmVersion();
   }

   public abstract String getAlgorithmType();

   public abstract String getAlgorithmVersion();

   public abstract int getDataElementCount();

   public static CryptographicAlgorithmBundle getInstance(String name) {
      if (map.isEmpty()) {
         createMap();
      }

      return map.get(name);
   }

   public static CryptographicAlgorithmBundle getInstanceFor(String encoded) {
      try {
         String name = extractName(encoded);
         return getInstance(name);
      } catch (IllegalArgumentException iae) {
         return null;
      }
   }

   public static String extractName(String encoded) {
      int startEncoding = encoded.indexOf("[");
      int endEncoding = encoded.indexOf("]=");
      if (startEncoding >= 0 && endEncoding >= 0) {
         return encoded.substring(startEncoding + 1, endEncoding);
      } else {
         throw new IllegalArgumentException("Not an encoded string");
      }
   }

   public static String extractData(String encoded) {
      int startEncoding = encoded.indexOf("[");
      int endEncoding = encoded.indexOf("]=");
      if (startEncoding >= 0 && endEncoding >= 0) {
         return encoded.substring(endEncoding + "]=".length());
      } else {
         throw new IllegalArgumentException("Not an encoded string");
      }
   }

   public final String encode(String[] data) {
      if ((data != null || this.getDataElementCount() <= 0) && (data == null || data.length == this.getDataElementCount())) {
         StringBuilder sb = new StringBuilder();
         sb.append("[");
         sb.append(this.getAlgorithmName());
         sb.append("]=");
         if (data != null) {
            StringJoiner joiner = new StringJoiner(":");

            for (String s : data) {
               if (s.contains(":")) {
                  throw new IllegalArgumentException("Data elements cannot contain :");
               }

               joiner.add(s);
            }

            sb.append(joiner.toString());
         }

         return sb.toString();
      } else {
         throw new IllegalArgumentException("Incorrect data length");
      }
   }

   public final String[] decode(String encoded) {
      String encodedName = extractName(encoded);
      String encodedData = extractData(encoded);
      if (!this.getAlgorithmName().equals(encodedName)) {
         throw new IllegalArgumentException("Incorrect encoding type");
      } else if (this.getDataElementCount() > 0) {
         return this.splitData(encodedData);
      } else if (!encodedData.isEmpty()) {
         throw new IllegalArgumentException("Should not have data");
      } else {
         return null;
      }
   }

   private String[] splitData(String data) {
      Objects.requireNonNull(data);
      if (data.isEmpty()) {
         throw new IllegalArgumentException("Empty string");
      } else {
         String[] splitData = data.split(":");
         if (splitData.length != this.getDataElementCount()) {
            throw new IllegalArgumentException("Incorrect data length");
         } else {
            return splitData;
         }
      }
   }

   private static void createMap() {
      if (map.isEmpty()) {
         Arrays.asList(
               AesAlgorithmBundle.make(128, "1"),
               AesAlgorithmBundle.make(192, "1"),
               AesAlgorithmBundle.make(256, "1"),
               AesAlgorithmBundle.make(128),
               AesAlgorithmBundle.make(192),
               AesAlgorithmBundle.make(256),
               AliasedAesAlgorithmBundle.make(128, "1"),
               AliasedAesAlgorithmBundle.make(192, "1"),
               AliasedAesAlgorithmBundle.make(256, "1"),
               AliasedAesAlgorithmBundle.make(128),
               AliasedAesAlgorithmBundle.make(192),
               AliasedAesAlgorithmBundle.make(256),
               NiagaraStationAlgorithmBundle.getInstance(),
               QnxPlatformAlgorithmBundle.getInstance(),
               GlibcSha256CryptAlgorithmBundle.getInstance(),
               GlibcSha512CryptAlgorithmBundle.getInstance(),
               BCryptAlgorithmBundle.getInstance(),
               NullAlgorithmBundle.getInstance(),
               SRP6AlgorithmBundle.make(1024, "sha256"),
               SRP6AlgorithmBundle.make(2048, "sha256"),
               SRP6AlgorithmBundle.make(1024, "sha512"),
               SRP6AlgorithmBundle.make(2048, "sha512")
            )
            .stream()
            .forEach(bundle -> {
               CryptographicAlgorithmBundle var10000 = map.put(bundle.getAlgorithmName(), bundle);
            });
         map.put("SRP6", SRP6AlgorithmBundle.make(2048, "sha512"));
         map.put("srp6", SRP6AlgorithmBundle.make(2048, "sha512"));
         map.put("none", NullAlgorithmBundle.getInstance());
      }
   }
}
