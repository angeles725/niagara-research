package com.tridium.lonworks.util.selfdoc;

import java.util.HashSet;

class MakeUniqueSupercatName {
   private HashSet<String> others = new HashSet<>();

   String getUniqueScatName(String lonMarkObjectName) {
      String nextTry = lonMarkObjectName;
      int attemptCount = 0;

      while (this.others.contains(nextTry)) {
         nextTry = getNextSuffix(lonMarkObjectName, ++attemptCount);
      }

      this.others.add(nextTry);
      return nextTry;
   }

   static String getNextSuffix(String stem, int attemptCount) {
      if (attemptCount == 0) {
         return stem;
      } else {
         boolean endsWithLetter = Character.isLetter(stem.charAt(stem.length() - 1));
         if (endsWithLetter) {
            return stem + attemptCount;
         } else {
            String suffix;
            for (suffix = "" + (char)(97 + (attemptCount - 1) % 26); (attemptCount - 1) / 26 > 0; suffix = "" + (char)(97 + (attemptCount - 1) % 26) + suffix) {
               attemptCount = (attemptCount - 1) / 26;
            }

            return stem + suffix;
         }
      }
   }
}
