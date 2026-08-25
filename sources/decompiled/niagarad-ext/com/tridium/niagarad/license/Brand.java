package com.tridium.niagarad.license;

import com.tridium.niagarad.util.PatternFilter;
import java.util.logging.Logger;

public final class Brand {
   private static boolean isInitialized;
   private static String brandId;
   private static Brand.AcceptList acceptWbIn;
   private static Brand.AcceptList acceptStationIn;

   private Brand() {
   }

   public static String getBrandId(Logger log) {
      init(log);
      return brandId;
   }

   public static String getAcceptWbInString(Logger log) {
      init(log);
      return acceptWbIn.patternString;
   }

   public static String getAcceptStationInString(Logger log) {
      init(log);
      return acceptStationIn.patternString;
   }

   public static boolean checkWbIn(String brandId, Logger log) {
      init(log);
      return acceptWbIn == null || acceptWbIn.check(brandId);
   }

   public static boolean checkStationIn(String brandId, Logger log) {
      init(log);
      return acceptStationIn == null || acceptStationIn.check(brandId);
   }

   public static void unload() {
      brandId = null;
      acceptWbIn = null;
      acceptStationIn = null;
      isInitialized = false;
   }

   private static void init(Logger log) {
      if (!isInitialized) {
         Feature feature = LicenseManager.getInstance(log).getFeature("tridium", "brand");
         brandId = feature == null ? null : feature.get("brandId");
         if (brandId == null) {
            isInitialized = true;
         } else {
            acceptWbIn = new Brand.AcceptList(feature, "accept.wb.in");
            acceptStationIn = new Brand.AcceptList(feature, "accept.station.in");
            isInitialized = true;
         }
      }
   }

   public static class AcceptList {
      String id;
      PatternFilter[] patterns;
      String patternString;

      public AcceptList(Feature feature, String id) {
         this.id = id;
         this.patternString = feature.get(id, "*");
         this.patterns = PatternFilter.parseList(this.patternString, ";");
      }

      public boolean check(String brandId) {
         return this.accept(brandId);
      }

      public boolean accept(String brandId) {
         if (brandId == null) {
            return true;
         }

         for (PatternFilter pattern : this.patterns) {
            if (pattern.accept(brandId)) {
               return true;
            }
         }

         return false;
      }

      @Override
      public String toString() {
         return this.id + "=" + this.patternString;
      }
   }
}
