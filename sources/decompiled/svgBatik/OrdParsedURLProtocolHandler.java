package com.tridium.svg.batik;

import javax.baja.naming.BOrd;
import org.apache.batik.util.ParsedURL;
import org.apache.batik.util.ParsedURLDefaultProtocolHandler;

public class OrdParsedURLProtocolHandler extends ParsedURLDefaultProtocolHandler {
   private static final String ORD = "ord";

   public String getProtocolHandled() {
      return "ord";
   }

   public OrdParsedURLData parseURL(String urlStr) {
      if (urlStr == null) {
         throw new IllegalArgumentException("URL is required");
      } else {
         int hash = urlStr.indexOf("#");
         String ref = null;
         if (hash >= 0) {
            ref = urlStr.substring(hash + 1);
            urlStr = urlStr.substring(0, hash);
         }

         return new OrdParsedURLData(BOrd.make(BatikOrdUtils.fromBatikUrl(urlStr)), ref);
      }
   }

   public OrdParsedURLData parseURL(ParsedURL basepurl, String urlStr) {
      if (basepurl != null && urlStr != null) {
         return this.parseURL(BatikOrdUtils.relativize(basepurl, urlStr));
      } else {
         throw new IllegalArgumentException("Base and sub-URLs required");
      }
   }
}
