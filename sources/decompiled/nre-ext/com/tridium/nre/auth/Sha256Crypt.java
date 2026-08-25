package com.tridium.nre.auth;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

public final class Sha256Crypt {
   private static final String sha256_salt_prefix = "$5$";
   private static final String sha256_rounds_prefix = "rounds=";
   private static final int SALT_LEN_MAX = 16;
   private static final int ROUNDS_DEFAULT = 5000;
   private static final int ROUNDS_MIN = 1000;
   private static final int ROUNDS_MAX = 999999999;
   private static final String SALTCHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
   private static final String itoa64 = "./0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

   private static MessageDigest getSHA256() {
      try {
         return MessageDigest.getInstance("SHA-256");
      } catch (NoSuchAlgorithmException ex) {
         throw new RuntimeException(ex);
      }
   }

   public static final String Sha256_crypt(String keyStr, String saltStr, int roundsCount) {
      MessageDigest ctx = getSHA256();
      MessageDigest alt_ctx = getSHA256();
      byte[] p_bytes = null;
      byte[] s_bytes = null;
      int rounds = 5000;
      boolean include_round_count = false;
      if (saltStr != null) {
         if (saltStr.startsWith("$5$")) {
            saltStr = saltStr.substring("$5$".length());
         }

         if (saltStr.startsWith("rounds=")) {
            String num = saltStr.substring("rounds=".length(), saltStr.indexOf(36));
            int srounds = Integer.valueOf(num);
            saltStr = saltStr.substring(saltStr.indexOf(36) + 1);
            rounds = Math.max(1000, Math.min(srounds, 999999999));
            include_round_count = true;
         }

         if (saltStr.length() > 16) {
            saltStr = saltStr.substring(0, 16);
         }

         if (saltStr.endsWith("$")) {
            saltStr = saltStr.substring(0, saltStr.length() - 1);
         } else if (saltStr.indexOf("$") != -1) {
            saltStr = saltStr.substring(0, saltStr.indexOf("$"));
         }
      } else {
         Random randgen = new Random();
         StringBuilder saltBuf = new StringBuilder();

         while (saltBuf.length() < 16) {
            int index = (int)(randgen.nextFloat() * "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890".length());
            saltBuf.append("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890".substring(index, index + 1));
         }

         saltStr = saltBuf.toString();
      }

      if (roundsCount != 0) {
         rounds = Math.max(1000, Math.min(roundsCount, 999999999));
      }

      byte[] key = keyStr.getBytes();
      byte[] salt = saltStr.getBytes();
      ctx.reset();
      ctx.update(key, 0, key.length);
      ctx.update(salt, 0, salt.length);
      alt_ctx.reset();
      alt_ctx.update(key, 0, key.length);
      alt_ctx.update(salt, 0, salt.length);
      alt_ctx.update(key, 0, key.length);
      byte[] alt_result = alt_ctx.digest();

      int cnt;
      for (cnt = key.length; cnt > 32; cnt -= 32) {
         ctx.update(alt_result, 0, 32);
      }

      ctx.update(alt_result, 0, cnt);

      for (int var21 = key.length; var21 > 0; var21 >>= 1) {
         if ((var21 & 1) != 0) {
            ctx.update(alt_result, 0, 32);
         } else {
            ctx.update(key, 0, key.length);
         }
      }

      alt_result = ctx.digest();
      alt_ctx.reset();

      for (int var22 = 0; var22 < key.length; var22++) {
         alt_ctx.update(key, 0, key.length);
      }

      byte[] temp_result = alt_ctx.digest();
      p_bytes = new byte[key.length];
      int cnt2 = 0;

      for (cnt = p_bytes.length; cnt >= 32; cnt -= 32) {
         System.arraycopy(temp_result, 0, p_bytes, cnt2, 32);
         cnt2 += 32;
      }

      System.arraycopy(temp_result, 0, p_bytes, cnt2, cnt);
      alt_ctx.reset();

      for (int var24 = 0; var24 < 16 + (alt_result[0] & 255); var24++) {
         alt_ctx.update(salt, 0, salt.length);
      }

      temp_result = alt_ctx.digest();
      s_bytes = new byte[salt.length];
      int var27 = 0;

      for (cnt = s_bytes.length; cnt >= 32; cnt -= 32) {
         System.arraycopy(temp_result, 0, s_bytes, var27, 32);
         var27 += 32;
      }

      System.arraycopy(temp_result, 0, s_bytes, var27, cnt);

      for (int var26 = 0; var26 < rounds; var26++) {
         ctx.reset();
         if ((var26 & 1) != 0) {
            ctx.update(p_bytes, 0, key.length);
         } else {
            ctx.update(alt_result, 0, 32);
         }

         if (var26 % 3 != 0) {
            ctx.update(s_bytes, 0, salt.length);
         }

         if (var26 % 7 != 0) {
            ctx.update(p_bytes, 0, key.length);
         }

         if ((var26 & 1) != 0) {
            ctx.update(alt_result, 0, 32);
         } else {
            ctx.update(p_bytes, 0, key.length);
         }

         alt_result = ctx.digest();
      }

      StringBuilder buffer = new StringBuilder("$5$");
      if (include_round_count || rounds != 5000) {
         buffer.append("rounds=");
         buffer.append(rounds);
         buffer.append("$");
      }

      buffer.append(saltStr);
      buffer.append("$");
      buffer.append(b64_from_24bit(alt_result[0], alt_result[10], alt_result[20], 4));
      buffer.append(b64_from_24bit(alt_result[21], alt_result[1], alt_result[11], 4));
      buffer.append(b64_from_24bit(alt_result[12], alt_result[22], alt_result[2], 4));
      buffer.append(b64_from_24bit(alt_result[3], alt_result[13], alt_result[23], 4));
      buffer.append(b64_from_24bit(alt_result[24], alt_result[4], alt_result[14], 4));
      buffer.append(b64_from_24bit(alt_result[15], alt_result[25], alt_result[5], 4));
      buffer.append(b64_from_24bit(alt_result[6], alt_result[16], alt_result[26], 4));
      buffer.append(b64_from_24bit(alt_result[27], alt_result[7], alt_result[17], 4));
      buffer.append(b64_from_24bit(alt_result[18], alt_result[28], alt_result[8], 4));
      buffer.append(b64_from_24bit(alt_result[9], alt_result[19], alt_result[29], 4));
      buffer.append(b64_from_24bit((byte)0, alt_result[31], alt_result[30], 3));
      ctx.reset();
      return buffer.toString();
   }

   private static final String b64_from_24bit(byte B2, byte B1, byte B0, int size) {
      int v = (B2 & 255) << 16 | (B1 & 255) << 8 | B0 & 255;
      StringBuilder result = new StringBuilder();

      while (--size >= 0) {
         result.append("./0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".charAt(v & 63));
         v >>>= 6;
      }

      return result.toString();
   }

   public static final boolean verifyPassword(String plaintextPass, String sha256CryptText) {
      if (sha256CryptText.startsWith("$5$")) {
         return sha256CryptText.equals(Sha256_crypt(plaintextPass, sha256CryptText, 0));
      } else {
         throw new RuntimeException("Bad sha256CryptText");
      }
   }

   public static final boolean verifyHashTextFormat(String sha256CryptText) {
      if (!sha256CryptText.startsWith("$5$")) {
         return false;
      }

      sha256CryptText = sha256CryptText.substring("$5$".length());
      if (sha256CryptText.startsWith("rounds=")) {
         String num = sha256CryptText.substring("rounds=".length(), sha256CryptText.indexOf(36));

         try {
            int ex = Integer.valueOf(num);
         } catch (NumberFormatException ex) {
            return false;
         }

         sha256CryptText = sha256CryptText.substring(sha256CryptText.indexOf(36) + 1);
      }

      if (sha256CryptText.indexOf(36) > 17) {
         return false;
      }

      sha256CryptText = sha256CryptText.substring(sha256CryptText.indexOf(36) + 1);

      for (int i = 0; i < sha256CryptText.length(); i++) {
         if ("./0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".indexOf(sha256CryptText.charAt(i)) == -1) {
            return false;
         }
      }

      return true;
   }

   private static void selfTest() {
      String[] msgs = new String[]{
         "$5$saltstring",
         "Hello world!",
         "$5$saltstring$5B8vYYiY.CVt1RlTTf8KbXBH3hsxY/GNooZaBBGWEc5",
         "$5$rounds=10000$saltstringsaltstring",
         "Hello world!",
         "$5$rounds=10000$saltstringsaltst$3xv.VbSHBb41AL9AvLeujZkZRBAwqFMz2.opqey6IcA",
         "$5$rounds=5000$toolongsaltstring",
         "This is just a test",
         "$5$rounds=5000$toolongsaltstrin$Un/5jzAHMgOGZ5.mWJpuVolil07guHPvOW8mGRcvxa5",
         "$5$rounds=1400$anotherlongsaltstring",
         "a very much longer text to encrypt.  This one even stretches over morethan one line.",
         "$5$rounds=1400$anotherlongsalts$Rx.j8H.h8HjEDGomFU8bDkXm3XIUnzyxf12oP84Bnq1",
         "$5$rounds=77777$short",
         "we have a short salt string but not a short password",
         "$5$rounds=77777$short$JiO1O3ZpDAxGJeaDIuqCoEFysAe1mZNJRs3pw0KQRd/",
         "$5$rounds=123456$asaltof16chars..",
         "a short string",
         "$5$rounds=123456$asaltof16chars..$gP3VQ/6X7UUEW3HkBn2w1/Ptq2jxPyzV/cZKmF/wJvD",
         "$5$rounds=10$roundstoolow",
         "the minimum number is still observed",
         "$5$rounds=1000$roundstoolow$yfvwcWrQ8l/K0DAWyuPMDNHpIVlTQebY9l/gL972bIC"
      };
      System.out.println("Starting Sha256Crypt tests now...");

      for (int t = 0; t < msgs.length / 3; t++) {
         String saltPrefix = msgs[t * 3];
         String plainText = msgs[t * 3 + 1];
         String cryptText = msgs[t * 3 + 2];
         String result = Sha256_crypt(plainText, saltPrefix, 0);
         System.out.println("test " + t + " result is:" + result);
         System.out.println("test " + t + " should be:" + cryptText);
         if (result.equals(cryptText)) {
            System.out.println("Passed crypt well");
         } else {
            System.out.println("Failed Crypt Badly");
         }

         if (verifyPassword(plainText, cryptText)) {
            System.out.println("Passed verifyPassword well");
         } else {
            System.out.println("Failed verifyPassword Badly");
         }
      }
   }

   public static void main(String[] arg) {
      selfTest();
   }
}
