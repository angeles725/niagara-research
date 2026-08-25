package com.tridium.nre.util;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.baja.nre.util.ByteBuffer;

public class LegacyStorageUtil {
   private static final byte[] secret = new byte[]{
      -112,
      44,
      -73,
      64,
      -68,
      -111,
      38,
      103,
      -127,
      102,
      78,
      -83,
      -28,
      98,
      -73,
      55,
      106,
      -45,
      76,
      -88,
      -45,
      28,
      -101,
      15,
      99,
      5,
      53,
      111,
      -55,
      -95,
      -73,
      70,
      -111,
      -125,
      -24,
      -67,
      -1,
      64,
      -34,
      -43,
      -88,
      100,
      37,
      38,
      -14,
      27,
      -13,
      -103,
      -74,
      -82,
      -97,
      -32,
      120,
      -69,
      -90,
      -6,
      -118,
      -91,
      -109,
      -75,
      77,
      24,
      99,
      93,
      43,
      -58,
      35,
      123,
      -21,
      102,
      53,
      26,
      68,
      -27,
      -29,
      71,
      -11,
      124,
      101,
      118,
      113,
      -113,
      36,
      105,
      122,
      54,
      -96,
      -1,
      -59,
      -75,
      -116,
      -126,
      -40,
      -21,
      -53,
      12,
      15,
      -82,
      -86,
      -123,
      7,
      93,
      -93,
      -128,
      -95,
      -19,
      27,
      50,
      -103,
      -123,
      -83,
      104,
      -91,
      118,
      -80,
      -12,
      -78,
      36,
      -57,
      15,
      71,
      3,
      -92,
      -87,
      -72,
      118,
      -41,
      -1,
      64,
      95,
      12,
      80,
      -97,
      58,
      -115,
      -30,
      121,
      103,
      -57,
      -122,
      -16,
      -78,
      -42,
      28,
      -18,
      -96,
      33,
      -3,
      -4,
      114,
      64,
      107,
      -8,
      -110,
      -6,
      100,
      -32,
      -126,
      87,
      68,
      -107,
      -34,
      0,
      -5,
      66,
      73,
      46,
      22,
      126,
      -69,
      -20,
      78,
      -19,
      -57,
      61,
      -6,
      0,
      -27,
      -21,
      -24,
      -70,
      -96,
      -63,
      -40,
      -16,
      99,
      -110,
      -89,
      102,
      26,
      -93,
      -128,
      58,
      -40,
      -52,
      -88,
      66,
      -119,
      106,
      18,
      -45,
      59,
      76,
      -124,
      -24,
      37,
      22,
      -66,
      31,
      98,
      98,
      57,
      80,
      12,
      -73,
      107,
      0,
      2,
      88,
      106,
      3,
      -24,
      72,
      104,
      47,
      -74,
      -25,
      101,
      55,
      70,
      -67,
      -13,
      -109,
      94,
      96,
      106,
      29,
      -75,
      17,
      -125,
      -91,
      126,
      24,
      -2,
      -87,
      79,
      55,
      103,
      41,
      -24,
      -37,
      56,
      -106,
      39,
      125,
      3
   };
   private static SecureRandom random = new SecureRandom();
   private static int randomCount = 0;
   public static final int SCHEME_0 = 0;
   public static final int SCHEME_1 = 1;

   public static String encode(char[] s, int scheme) throws IOException {
      byte[] data;
      switch (scheme) {
         case 0:
            data = encodeScheme0(s);
            break;
         case 1:
            try {
               data = encodeScheme1(s);
               break;
            } catch (Exception e) {
               throw new IOException("unrecognized char type");
            }
         default:
            throw new IOException("Bad source data");
      }

      byte[] wrapper = new byte[data.length + 1];
      wrapper[0] = (byte)scheme;
      System.arraycopy(data, 0, wrapper, 1, data.length);
      return Base64.getEncoder().encodeToString(wrapper);
   }

   public static String encode(String s, int scheme) throws IOException {
      return encode(s.toCharArray(), scheme);
   }

   public static String decode(String s) throws IOException {
      ByteBuffer buf = new ByteBuffer(Base64.getDecoder().decode(s));
      int scheme = buf.read();
      byte[] data = new byte[buf.getLength() - 1];
      buf.read(data);
      switch (scheme) {
         case 0:
            return decodeScheme0(data);
         case 1:
            try {
               return decodeScheme1(data);
            } catch (Exception e) {
               throw new IOException("unrecognized char type");
            }
         default:
            throw new IOException("Bad source data");
      }
   }

   public static byte[] encodeScheme0(char[] s) {
      int rawLength = s.length * 2;
      int len = 4 + rawLength + rawLength % 16;
      byte[] data = new byte[len];
      int slen = secret.length;
      int si = Math.abs(random.nextInt() + randomCount) % slen;
      randomCount *= 11;
      int esi = ((si << 17) + 374573) * 3 ^ 2124067810;
      data[0] = (byte)(esi >>> 24 & 0xFF);
      data[1] = (byte)(esi >>> 16 & 0xFF);
      data[2] = (byte)(esi >>> 8 & 0xFF);
      data[3] = (byte)(esi >>> 0 & 0xFF);
      int n = 4;

      for (int i = s.length - 1; i >= 0; i--) {
         char c = s[i];
         if (c == 0) {
            throw new IllegalStateException("Illegal string");
         }

         data[n++] = (byte)(c >>> '\b' & 0xFF);
         data[n++] = (byte)(c >>> 0 & 0xFF);
      }

      int pad = randomCount;
      randomCount += 3;
      n += 2;

      while (n < data.length) {
         data[n++] = (byte)(pad ^ secret[n % secret.length]);
      }

      for (int i = 4; i < data.length; i++) {
         data[i] ^= secret[si++ % slen];
      }

      return data;
   }

   public static byte[] encodeScheme0(String s) {
      return encodeScheme0(s.toCharArray());
   }

   public static String decodeScheme0(byte[] data) {
      int slen = secret.length;
      int esi = ((data[0] & 255) << 24) + ((data[1] & 255) << 16) + ((data[2] & 255) << 8) + (data[3] & 255);
      int si = (esi ^ 2124067810) / 3 - 374573 >> 17;

      for (int i = 4; i < data.length; i++) {
         data[i] ^= secret[si++ % slen];
      }

      int n = 4;

      while (n < data.length && (data[n] != 0 || data[n + 1] != 0)) {
         n++;
      }

      int len = (n - 4) / 2;
      char[] chars = new char[len];

      for (int i = 0; i < len; i++) {
         int x = i * 2 + 4;
         char c = (char)(((data[x] & 255) << 8) + (data[x + 1] & 255));
         chars[len - i - 1] = c;
      }

      return new String(chars);
   }

   public static byte[] encodeScheme1(String s) throws Exception {
      return s.getBytes("UTF-8");
   }

   public static byte[] encodeScheme1(char[] s) throws Exception {
      return encodeScheme1(new String(s));
   }

   public static String decodeScheme1(byte[] data) throws Exception {
      return new String(data, "UTF-8");
   }
}
