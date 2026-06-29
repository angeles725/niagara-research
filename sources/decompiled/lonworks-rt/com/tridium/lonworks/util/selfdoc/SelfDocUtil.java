package com.tridium.lonworks.util.selfdoc;

import java.util.StringTokenizer;

public class SelfDocUtil {
   public static int getFirstIndex(String range) {
      if (range.length() <= 0) {
         return -1;
      } else {
         int ndx = 0;

         while (ndx < range.length() && Character.isDigit(range.charAt(ndx))) {
            ndx++;
         }

         return ndx == range.length() ? Integer.parseInt(range) : Integer.parseInt(range.substring(0, ndx));
      }
   }

   public static int getLastIndex(String range) {
      if (range.length() <= 0) {
         return -1;
      } else {
         int ndx = range.length() - 1;

         while (ndx >= 0 && Character.isDigit(range.charAt(ndx))) {
            ndx--;
         }

         return ndx == -1 ? Integer.parseInt(range) : Integer.parseInt(range.substring(ndx + 1));
      }
   }

   public static int getObjectCount(String range) {
      return getLastIndex(range) - getFirstIndex(range) + 1;
   }

   public static int[] selectToIntArray(String select) {
      if (select != null && select.length() != 0) {
         try {
            boolean range = select.indexOf(45) > 0;
            StringTokenizer st = new StringTokenizer(select, "-.");
            int n = st.countTokens();
            if (!range) {
               int[] sels = new int[n];

               for (int i = 0; i < sels.length; i++) {
                  sels[i] = Integer.decode(st.nextToken());
               }

               return sels;
            } else if (n != 2) {
               throw new RuntimeException("Invalid select " + select);
            } else {
               int firstNdx = Integer.decode(st.nextToken());
               int lastNdx = Integer.decode(st.nextToken());
               int[] sels = new int[lastNdx - firstNdx + 1];

               for (int i = 0; i < sels.length; i++) {
                  sels[i] = firstNdx + i;
               }

               return sels;
            }
         } catch (Throwable var9) {
            return new int[0];
         }
      } else {
         return new int[0];
      }
   }
}
