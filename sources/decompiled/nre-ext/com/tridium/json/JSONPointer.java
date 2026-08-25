package com.tridium.json;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class JSONPointer {
   private static final String ENCODING = "utf-8";
   private final List<String> refTokens;

   public static JSONPointer.Builder builder() {
      return new JSONPointer.Builder();
   }

   public JSONPointer(String pointer) {
      if (pointer == null) {
         throw new NullPointerException("pointer cannot be null");
      }

      if (!pointer.isEmpty() && !pointer.equals("#")) {
         String refs;
         if (pointer.startsWith("#/")) {
            refs = pointer.substring(2);

            try {
               refs = URLDecoder.decode(refs, "utf-8");
            } catch (UnsupportedEncodingException e) {
               throw new RuntimeException(e);
            }
         } else {
            if (!pointer.startsWith("/")) {
               throw new IllegalArgumentException("a JSON pointer should start with '/' or '#/'");
            }

            refs = pointer.substring(1);
         }

         this.refTokens = new ArrayList<>();
         int slashIdx = -1;
         int prevSlashIdx = 0;

         do {
            prevSlashIdx = slashIdx + 1;
            slashIdx = refs.indexOf(47, prevSlashIdx);
            if (prevSlashIdx == slashIdx || prevSlashIdx == refs.length()) {
               this.refTokens.add("");
            } else if (slashIdx >= 0) {
               String token = refs.substring(prevSlashIdx, slashIdx);
               this.refTokens.add(unescape(token));
            } else {
               String token = refs.substring(prevSlashIdx);
               this.refTokens.add(unescape(token));
            }
         } while (slashIdx >= 0);
      } else {
         this.refTokens = Collections.emptyList();
      }
   }

   public JSONPointer(List<String> refTokens) {
      this.refTokens = new ArrayList<>(refTokens);
   }

   private static String unescape(String token) {
      return token.replace("~1", "/").replace("~0", "~");
   }

   public Object queryFrom(Object document) throws JSONPointerException {
      if (this.refTokens.isEmpty()) {
         return document;
      }

      Object current = document;

      for (String token : this.refTokens) {
         if (current instanceof JSONObject) {
            current = ((JSONObject)current).opt(unescape(token));
         } else {
            if (!(current instanceof JSONArray)) {
               throw new JSONPointerException(String.format("value [%s] is not an array or object therefore its key %s cannot be resolved", current, token));
            }

            current = readByIndexToken(current, token);
         }
      }

      return current;
   }

   private static Object readByIndexToken(Object current, String indexToken) throws JSONPointerException {
      try {
         int index = Integer.parseInt(indexToken);
         JSONArray currentArr = (JSONArray)current;
         if (index >= currentArr.length()) {
            throw new JSONPointerException(String.format("index %s is out of bounds - the array has %d elements", indexToken, currentArr.length()));
         }

         try {
            return currentArr.get(index);
         } catch (JSONException e) {
            throw new JSONPointerException("Error reading value at index position " + index, e);
         }
      } catch (NumberFormatException e) {
         throw new JSONPointerException(String.format("%s is not an array index", indexToken), e);
      }
   }

   @Override
   public String toString() {
      StringBuilder rval = new StringBuilder("");

      for (String token : this.refTokens) {
         rval.append('/').append(escape(token));
      }

      return rval.toString();
   }

   private static String escape(String token) {
      return token.replace("~", "~0").replace("/", "~1");
   }

   public String toURIFragment() {
      try {
         StringBuilder rval = new StringBuilder("#");

         for (String token : this.refTokens) {
            rval.append('/').append(URLEncoder.encode(token, "utf-8"));
         }

         return rval.toString();
      } catch (UnsupportedEncodingException e) {
         throw new RuntimeException(e);
      }
   }

   public static class Builder {
      private final List<String> refTokens = new ArrayList<>();

      public JSONPointer build() {
         return new JSONPointer(this.refTokens);
      }

      public JSONPointer.Builder append(String token) {
         if (token == null) {
            throw new NullPointerException("token cannot be null");
         }

         this.refTokens.add(token);
         return this;
      }

      public JSONPointer.Builder append(int arrayIndex) {
         this.refTokens.add(String.valueOf(arrayIndex));
         return this;
      }
   }
}
