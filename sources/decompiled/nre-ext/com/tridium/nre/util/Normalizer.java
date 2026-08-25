package com.tridium.nre.util;

public class Normalizer {
   public static String normalize(String paramString, int paramForm) throws Exception {
      switch (paramForm) {
         case 2:
            return java.text.Normalizer.normalize(paramString, java.text.Normalizer.Form.NFD);
         case 3:
            return java.text.Normalizer.normalize(paramString, java.text.Normalizer.Form.NFKD);
         case 4:
            return java.text.Normalizer.normalize(paramString, java.text.Normalizer.Form.NFC);
         case 5:
            return java.text.Normalizer.normalize(paramString, java.text.Normalizer.Form.NFKC);
         default:
            return paramString;
      }
   }

   public interface Form {
      int NFC = 4;
      int NFD = 2;
      int NFKC = 5;
      int NFKD = 3;
   }
}
