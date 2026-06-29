package com.tridium.lonworks.util;

import java.util.StringTokenizer;

public final class LonByteArrayUtil {
   private static final boolean LEADING_ZERO_DEFAULT = true;

   public static String toString(byte[] a) {
      return toString(a, 16, ' ', a.length, 0, true);
   }

   public static String toString(byte[] a, int len) {
      return toString(a, 16, ' ', len, 0, true);
   }

   public static String toString(byte[] a, int len, int offset) {
      return toString(a, 16, ' ', len, offset, true);
   }

   public static String toString(byte[] a, boolean leadingZero) {
      return toString(a, 16, ' ', a.length, 0, leadingZero);
   }

   public static String toString(byte[] a, char delimiter) {
      return toString(a, 16, delimiter, a.length, 0, true);
   }

   public static String toString(byte[] a, char delimiter, int len) {
      return toString(a, 16, delimiter, len, 0, true);
   }

   public static String toString(byte[] a, int radix, char delimiter, int len) {
      return toString(a, radix, delimiter, len, 0, true);
   }

   public static String toString(byte[] a, int radix, char delimiter, int len, int offset) {
      return toString(a, radix, delimiter, len, offset, true);
   }

   public static String toString(byte[] a, int radix, char delimiter, int len, int offset, boolean leadingZero) {
      StringBuilder sb = new StringBuilder();
      int dispLen = Integer.toString(255, radix).length();
      if (offset + len > a.length) {
         len = a.length - offset;
      }

      if (len == 0) {
         return "";
      } else {
         int bytes = len + offset;

         for (int i = offset; i < bytes; i++) {
            if (i > offset) {
               sb.append(delimiter);
            }

            String s = Integer.toString(a[i] & 255, radix);
            if (leadingZero) {
               for (int n = s.length(); n < dispLen; n++) {
                  sb.append('0');
               }
            }

            sb.append(s);
         }

         return sb.toString();
      }
   }

   public static byte[] getBytes(String s) {
      return getBytes(s, -1, 16);
   }

   public static byte[] getBytes(String s, String del) {
      return getBytes(s, del, -1, 16);
   }

   public static byte[] getBytes(String s, int length) {
      return getBytes(s, length, 16);
   }

   public static byte[] getBytes(String s, int length, int radix) {
      return getBytes(s, ":,. ;\\/", length, radix);
   }

   public static byte[] getBytes(String s, String del, int length, int radix) {
      StringTokenizer st = new StringTokenizer(s, del);
      int tokCnt = st.countTokens();
      byte[] b;
      if (tokCnt == 1 && radix == 16) {
         String t = st.nextToken();
         int tlen = t.length();
         if ((tlen & 1) > 0) {
            t = "0" + t;
            tlen++;
         }

         if (length < 0) {
            length = tlen / 2;
         }

         b = new byte[length];

         for (int i = 0; i < length; i++) {
            b[i] = (byte)Integer.parseInt(t.substring(i * 2, i * 2 + 2).trim(), radix);
         }
      } else {
         if (length < 0) {
            length = tokCnt;
         }

         b = new byte[length];

         for (int i = 0; i < length; i++) {
            if (st.hasMoreTokens()) {
               b[i] = (byte)Integer.parseInt(st.nextToken().trim(), radix);
            } else {
               b[i] = 0;
            }
         }
      }

      return b;
   }

   public static boolean equals(byte[] a1, byte[] a2) {
      int len = a1.length;
      if (len != a2.length) {
         return false;
      } else {
         for (int i = 0; i < len; i++) {
            if (a1[i] != a2[i]) {
               return false;
            }
         }

         return true;
      }
   }
}
