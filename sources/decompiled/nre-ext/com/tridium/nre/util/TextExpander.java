package com.tridium.nre.util;

import java.util.LinkedList;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Pattern;

public final class TextExpander {
   private final String delimStart;
   private final String delimEnd;
   private final String specialChars;
   private final Function<String, String> mapper;
   private static final char ESCAPE_CHAR = '\\';
   private static final Pattern VALID_NAME_PATTERN = Pattern.compile("[A-Za-z0-9\\._\\-\\\\/:]+");

   public TextExpander(Function<String, String> mapper) {
      this("${", "}", mapper);
   }

   public TextExpander(String delimStart, String delimEnd, Function<String, String> mapper) {
      Objects.requireNonNull(delimStart);
      Objects.requireNonNull(delimEnd);
      Objects.requireNonNull(mapper);
      this.delimStart = delimStart;
      if (delimStart.isEmpty()) {
         throw new IllegalArgumentException("Must have a start delimiter!");
      }

      this.delimEnd = delimEnd;
      if (delimEnd.isEmpty()) {
         throw new IllegalArgumentException("Must have an end delimiter!");
      }

      this.specialChars = delimStart + delimEnd;
      this.mapper = mapper;
   }

   public String expand(String src) {
      LinkedList<StringBuilder> builderQueue = new LinkedList<>();
      StringBuilder sb = new StringBuilder(src.length());

      for (int i = 0; i < src.length(); i++) {
         char c = src.charAt(i);
         if (c == '\\') {
            sb.append(src.charAt(++i));
         } else if (this.specialChars.indexOf(c) >= 0) {
            if (src.substring(i).startsWith(this.delimStart)) {
               builderQueue.push(sb);
               sb = new StringBuilder(src.length() - i);
               sb.append(this.delimStart);
               i += this.delimStart.length() - 1;
            } else {
               if (!src.substring(i).startsWith(this.delimEnd) || builderQueue.isEmpty()) {
                  throw new IllegalArgumentException("Unexpected unescaped special char " + c);
               }

               sb.append(this.delimEnd);
               String part = sb.toString();
               sb = builderQueue.pop();
               if (part.startsWith(this.delimStart) && part.endsWith(this.delimEnd)) {
                  String key = part.substring(this.delimStart.length(), part.length() - this.delimEnd.length());
                  if (!VALID_NAME_PATTERN.matcher(key).matches()) {
                     throw new IllegalArgumentException("Key name " + key + " is not valid");
                  }

                  sb.append(this.mapper.apply(key));
               } else {
                  sb.append(part);
               }
            }
         } else {
            sb.append(c);
         }
      }

      if (!builderQueue.isEmpty()) {
         throw new IllegalArgumentException("Input string has mismatched delimiters");
      } else {
         return sb.toString();
      }
   }
}
