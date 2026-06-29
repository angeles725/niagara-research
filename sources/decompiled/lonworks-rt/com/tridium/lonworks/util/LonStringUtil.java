package com.tridium.lonworks.util;

import java.io.IOException;
import java.util.StringTokenizer;
import javax.baja.nre.util.Array;
import javax.baja.sys.BSimple;

public final class LonStringUtil {
   private static final String delimiter = "\t";

   public static String toString(int[] a) {
      StringBuilder sb = new StringBuilder();

      for (int i = 0; i < a.length; i++) {
         if (i > 0) {
            sb.append(",");
         }

         sb.append(a[i]);
      }

      return sb.toString();
   }

   public static int[] getIntArray(String s) {
      StringTokenizer st = new StringTokenizer(s, ",");
      int tokCnt = st.countTokens();
      int[] b = new int[tokCnt];

      for (int i = 0; i < tokCnt; i++) {
         b[i] = Integer.parseInt(st.nextToken().trim());
      }

      return b;
   }

   public static String toString(String[] a) {
      StringBuilder sb = new StringBuilder();

      for (int i = 0; i < a.length; i++) {
         if (i > 0) {
            sb.append(",");
         }

         sb.append(a[i]);
      }

      return sb.toString();
   }

   public static String[] getStringArray(String s) {
      StringTokenizer st = new StringTokenizer(s, ",");
      int tokCnt = st.countTokens();
      String[] b = new String[tokCnt];

      for (int i = 0; i < tokCnt; i++) {
         b[i] = st.nextToken();
      }

      return b;
   }

   public static String toString(BSimple[] a) {
      StringBuilder sb = new StringBuilder();

      for (int i = 0; i < a.length; i++) {
         if (i > 0) {
            sb.append("\t");
         }

         try {
            sb.append(a[i].encodeToString());
         } catch (IOException var4) {
         }
      }

      return sb.toString();
   }

   public static BSimple[] getSimpleArray(String s, BSimple def) {
      StringTokenizer st = new StringTokenizer(s, "\t");
      int len = st.countTokens();
      Array a = new Array(def.getClass(), len);

      for (int i = 0; i < len; i++) {
         try {
            a.add((BSimple)def.decodeFromString(st.nextToken()));
         } catch (IOException var7) {
         }
      }

      return (BSimple[])a.trim();
   }
}
