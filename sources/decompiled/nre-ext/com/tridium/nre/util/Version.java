package com.tridium.nre.util;

import java.util.Arrays;
import java.util.StringTokenizer;

public class Version implements Comparable<Object> {
   public static Version ZERO = new Version("0");
   public int[] versions;

   public Version(String s) {
      try {
         int[] buf = new int[16];
         int c = 0;
         StringTokenizer st = new StringTokenizer(s, "._");

         while (st.hasMoreTokens()) {
            int x = Integer.parseInt(st.nextToken());
            if (x < 0) {
               throw new IllegalArgumentException();
            }

            buf[c++] = x;
         }

         this.versions = new int[c];
         System.arraycopy(buf, 0, this.versions, 0, c);
      } catch (Exception e) {
         throw new IllegalArgumentException("Invalid version string \"" + s + "\"");
      }
   }

   @Override
   public int compareTo(Object ver) {
      Version v;
      if (ver instanceof String) {
         v = new Version((String)ver);
      } else {
         v = (Version)ver;
      }

      int len = this.versions.length;
      int vLen = v.versions.length;

      for (int i = 0; i < len && i < vLen; i++) {
         if (this.versions[i] > v.versions[i]) {
            return 1;
         }

         if (this.versions[i] < v.versions[i]) {
            return -1;
         }
      }

      if (len == vLen) {
         return 0;
      } else {
         return len > vLen ? 1 : -1;
      }
   }

   @Override
   public boolean equals(Object obj) {
      if (obj instanceof Version) {
         Version v = (Version)obj;
         if (this.versions.length != v.versions.length) {
            return false;
         }

         for (int i = 0; i < this.versions.length; i++) {
            if (this.versions[i] != v.versions[i]) {
               return false;
            }
         }

         return true;
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      return Arrays.hashCode(this.versions);
   }

   @Override
   public String toString() {
      return this.toString(this.versions.length);
   }

   public String toString(int len) {
      StringBuilder s = new StringBuilder();

      for (int i = 0; i < len && i < this.versions.length; i++) {
         if (i > 0) {
            s.append('.');
         }

         s.append(this.versions[i]);
      }

      return s.toString();
   }
}
