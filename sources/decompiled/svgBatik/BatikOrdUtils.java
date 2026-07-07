package com.tridium.svg.batik;

import java.net.URI;
import javax.baja.naming.BOrd;
import org.apache.batik.util.ParsedURL;
import org.owasp.encoder.Encode;

public class BatikOrdUtils {
   private static final String PREFIX = "ord://svgBatik/?ord=";

   public static String toBatikUrl(ParsedURL purl) {
      String urlString = purl.toString();
      if (urlString.startsWith("ord:svgBatik")) {
         urlString = "ord://svgBatik" + urlString.substring("ord:svgBatik".length());
      }

      return urlString;
   }

   public static String toBatikUrl(BOrd ord) {
      return toBatikUrl(ord, null);
   }

   public static String toBatikUrl(BOrd ord, String ref) {
      String query = ord.encodeToString();
      String hash = ref != null && !ref.isEmpty() ? "#" + ref : "";
      return "ord://svgBatik/?ord=" + Encode.forUriComponent(query) + hash;
   }

   public static String fromBatikUrl(String url) {
      return URI.create(url).getQuery().substring("ord=".length());
   }

   public static String fromBatikUrl(ParsedURL purl) {
      return fromBatikUrl(toBatikUrl(purl));
   }

   public static String relativize(ParsedURL purl, String subUrl) {
      return relativize(toBatikUrl(purl), subUrl);
   }

   public static String relativize(String batikUrl, String subUrl) {
      if (subUrl.startsWith("ord://")) {
         return subUrl;
      } else {
         String baseOrdStr = fromBatikUrl(batikUrl);
         int hashIdx = subUrl.indexOf("#");
         String hash = null;
         if (hashIdx >= 0) {
            hash = subUrl.substring(hashIdx + 1);
            subUrl = subUrl.substring(0, hashIdx);
         }

         if (!subUrl.isEmpty()) {
            int slashIdx = baseOrdStr.lastIndexOf("/");
            if (slashIdx > 0) {
               baseOrdStr = baseOrdStr.substring(0, slashIdx + 1);
            }
         }

         return toBatikUrl(BOrd.make(baseOrdStr + subUrl), hash);
      }
   }
}
