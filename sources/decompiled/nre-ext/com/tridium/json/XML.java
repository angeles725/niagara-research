package com.tridium.json;

import java.io.Reader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Iterator;

public class XML {
   public static final Character AMP = '&';
   public static final Character APOS = '\'';
   public static final Character BANG = '!';
   public static final Character EQ = '=';
   public static final Character GT = '>';
   public static final Character LT = '<';
   public static final Character QUEST = '?';
   public static final Character QUOT = '"';
   public static final Character SLASH = '/';
   public static final String NULL_ATTR = "xsi:nil";
   public static final String TYPE_ATTR = "xsi:type";

   private static Iterable<Integer> codePointIterator(final String string) {
      return new Iterable<Integer>() {
         @Override
         public Iterator<Integer> iterator() {
            return new Iterator<Integer>() {
               private int nextIndex = 0;
               private int length = string.length();

               @Override
               public boolean hasNext() {
                  return this.nextIndex < this.length;
               }

               public Integer next() {
                  int result = string.codePointAt(this.nextIndex);
                  this.nextIndex = this.nextIndex + Character.charCount(result);
                  return result;
               }

               @Override
               public void remove() {
                  throw new UnsupportedOperationException();
               }
            };
         }
      };
   }

   public static String escape(String string) {
      StringBuilder sb = new StringBuilder(string.length());

      for (int cp : codePointIterator(string)) {
         switch (cp) {
            case 34:
               sb.append("&quot;");
               break;
            case 38:
               sb.append("&amp;");
               break;
            case 39:
               sb.append("&apos;");
               break;
            case 60:
               sb.append("&lt;");
               break;
            case 62:
               sb.append("&gt;");
               break;
            default:
               if (mustEscape(cp)) {
                  sb.append("&#x");
                  sb.append(Integer.toHexString(cp));
                  sb.append(';');
               } else {
                  sb.appendCodePoint(cp);
               }
         }
      }

      return sb.toString();
   }

   private static boolean mustEscape(int cp) {
      return Character.isISOControl(cp) && cp != 9 && cp != 10 && cp != 13
         || (cp < 32 || cp > 55295) && (cp < 57344 || cp > 65533) && (cp < 65536 || cp > 1114111);
   }

   public static String unescape(String string) {
      StringBuilder sb = new StringBuilder(string.length());
      int i = 0;

      for (int length = string.length(); i < length; i++) {
         char c = string.charAt(i);
         if (c == '&') {
            int semic = string.indexOf(59, i);
            if (semic > i) {
               String entity = string.substring(i + 1, semic);
               sb.append(XMLTokener.unescapeEntity(entity));
               i += entity.length() + 1;
            } else {
               sb.append(c);
            }
         } else {
            sb.append(c);
         }
      }

      return sb.toString();
   }

   public static void noSpace(String string) throws JSONException {
      int length = string.length();
      if (length == 0) {
         throw new JSONException("Empty string.");
      }

      for (int i = 0; i < length; i++) {
         if (Character.isWhitespace(string.charAt(i))) {
            throw new JSONException("'" + string + "' contains a space character.");
         }
      }
   }

   private static boolean parse(XMLTokener x, JSONObject context, String name, XMLParserConfiguration config, int currentNestingDepth) throws JSONException {
      JSONObject jsonObject = null;
      Object token = x.nextToken();
      if (token == BANG) {
         char c = x.next();
         if (c == '-') {
            if (x.next() == '-') {
               x.skipPast("-->");
               return false;
            }

            x.back();
         } else if (c == '[') {
            token = x.nextToken();
            if ("CDATA".equals(token) && x.next() == '[') {
               String string = x.nextCDATA();
               if (string.length() > 0) {
                  context.accumulate(config.getcDataTagName(), string);
               }

               return false;
            }

            throw x.syntaxError("Expected 'CDATA['");
         }

         int i = 1;

         do {
            token = x.nextMeta();
            if (token == null) {
               throw x.syntaxError("Missing '>' after '<!'.");
            }

            if (token == LT) {
               i++;
            } else if (token == GT) {
               i--;
            }
         } while (i > 0);

         return false;
      } else {
         if (token == QUEST) {
            x.skipPast("?>");
            return false;
         }

         if (token == SLASH) {
            token = x.nextToken();
            if (name == null) {
               throw x.syntaxError("Mismatched close tag " + token);
            } else if (!token.equals(name)) {
               throw x.syntaxError("Mismatched " + name + " and " + token);
            } else if (x.nextToken() != GT) {
               throw x.syntaxError("Misshaped close tag");
            } else {
               return true;
            }
         } else {
            if (token instanceof Character) {
               throw x.syntaxError("Misshaped tag");
            }

            String tagName = (String)token;
            token = null;
            jsonObject = new JSONObject();
            boolean nilAttributeFound = false;
            XMLXsiTypeConverter<?> xmlXsiTypeConverter = null;

            while (true) {
               if (token == null) {
                  token = x.nextToken();
               }

               if (!(token instanceof String)) {
                  if (token == SLASH) {
                     if (x.nextToken() != GT) {
                        throw x.syntaxError("Misshaped tag");
                     }

                     if (config.getForceList().contains(tagName)) {
                        if (nilAttributeFound) {
                           context.append(tagName, JSONObject.NULL);
                        } else if (jsonObject.length() > 0) {
                           context.append(tagName, jsonObject);
                        } else {
                           context.put(tagName, new JSONArray());
                        }
                     } else if (nilAttributeFound) {
                        context.accumulate(tagName, JSONObject.NULL);
                     } else if (jsonObject.length() > 0) {
                        context.accumulate(tagName, jsonObject);
                     } else {
                        context.accumulate(tagName, "");
                     }

                     return false;
                  }

                  if (token != GT) {
                     throw x.syntaxError("Misshaped tag");
                  }

                  while (true) {
                     token = x.nextContent();
                     if (token == null) {
                        if (tagName != null) {
                           throw x.syntaxError("Unclosed tag " + tagName);
                        }

                        return false;
                     }

                     if (token instanceof String) {
                        String string = (String)token;
                        if (string.length() > 0) {
                           if (xmlXsiTypeConverter != null) {
                              jsonObject.accumulate(config.getcDataTagName(), stringToValue(string, xmlXsiTypeConverter));
                           } else {
                              jsonObject.accumulate(config.getcDataTagName(), config.isKeepStrings() ? string : stringToValue(string));
                           }
                        }
                     } else if (token == LT) {
                        if (currentNestingDepth == config.getMaxNestingDepth()) {
                           throw x.syntaxError("Maximum nesting depth of " + config.getMaxNestingDepth() + " reached");
                        }

                        if (parse(x, jsonObject, tagName, config, currentNestingDepth + 1)) {
                           if (config.getForceList().contains(tagName)) {
                              if (jsonObject.length() == 0) {
                                 context.put(tagName, new JSONArray());
                              } else if (jsonObject.length() == 1 && jsonObject.opt(config.getcDataTagName()) != null) {
                                 context.append(tagName, jsonObject.opt(config.getcDataTagName()));
                              } else {
                                 context.append(tagName, jsonObject);
                              }
                           } else if (jsonObject.length() == 0) {
                              context.accumulate(tagName, "");
                           } else if (jsonObject.length() == 1 && jsonObject.opt(config.getcDataTagName()) != null) {
                              context.accumulate(tagName, jsonObject.opt(config.getcDataTagName()));
                           } else {
                              context.accumulate(tagName, jsonObject);
                           }

                           return false;
                        }
                     }
                  }
               }

               String string = (String)token;
               token = x.nextToken();
               if (token == EQ) {
                  token = x.nextToken();
                  if (!(token instanceof String)) {
                     throw x.syntaxError("Missing value");
                  }

                  if (config.isConvertNilAttributeToNull() && "xsi:nil".equals(string) && Boolean.parseBoolean((String)token)) {
                     nilAttributeFound = true;
                  } else if (config.getXsiTypeMap() != null && !config.getXsiTypeMap().isEmpty() && "xsi:type".equals(string)) {
                     xmlXsiTypeConverter = config.getXsiTypeMap().get(token);
                  } else if (!nilAttributeFound) {
                     jsonObject.accumulate(string, config.isKeepStrings() ? (String)token : stringToValue((String)token));
                  }

                  token = null;
               } else {
                  jsonObject.accumulate(string, "");
               }
            }
         }
      }
   }

   public static Object stringToValue(String string, XMLXsiTypeConverter<?> typeConverter) {
      return typeConverter != null ? typeConverter.convert(string) : stringToValue(string);
   }

   public static Object stringToValue(String string) {
      if ("".equals(string)) {
         return string;
      }

      if ("true".equalsIgnoreCase(string)) {
         return Boolean.TRUE;
      }

      if ("false".equalsIgnoreCase(string)) {
         return Boolean.FALSE;
      }

      if ("null".equalsIgnoreCase(string)) {
         return JSONObject.NULL;
      }

      char initial = string.charAt(0);
      if (initial >= '0' && initial <= '9' || initial == '-') {
         try {
            return stringToNumber(string);
         } catch (Exception var3) {
         }
      }

      return string;
   }

   private static Number stringToNumber(String val) throws NumberFormatException {
      char initial = val.charAt(0);
      if ((initial < '0' || initial > '9') && initial != '-') {
         throw new NumberFormatException("val [" + val + "] is not a valid number.");
      }

      if (isDecimalNotation(val)) {
         try {
            BigDecimal bd = new BigDecimal(val);
            return initial == '-' && BigDecimal.ZERO.compareTo(bd) == 0 ? -0.0 : bd;
         } catch (NumberFormatException retryAsDouble) {
            try {
               Double d = Double.valueOf(val);
               if (!d.isNaN() && !d.isInfinite()) {
                  return d;
               } else {
                  throw new NumberFormatException("val [" + val + "] is not a valid number.");
               }
            } catch (NumberFormatException ignore) {
               throw new NumberFormatException("val [" + val + "] is not a valid number.");
            }
         }
      } else {
         if (initial == '0' && val.length() > 1) {
            char at1 = val.charAt(1);
            if (at1 >= '0' && at1 <= '9') {
               throw new NumberFormatException("val [" + val + "] is not a valid number.");
            }
         } else if (initial == '-' && val.length() > 2) {
            char at1 = val.charAt(1);
            char at2 = val.charAt(2);
            if (at1 == '0' && at2 >= '0' && at2 <= '9') {
               throw new NumberFormatException("val [" + val + "] is not a valid number.");
            }
         }

         BigInteger bi = new BigInteger(val);
         if (bi.bitLength() <= 31) {
            return bi.intValue();
         } else {
            return bi.bitLength() <= 63 ? bi.longValue() : bi;
         }
      }
   }

   private static boolean isDecimalNotation(String val) {
      return val.indexOf(46) > -1 || val.indexOf(101) > -1 || val.indexOf(69) > -1 || "-0".equals(val);
   }

   public static JSONObject toJSONObject(String string) throws JSONException {
      return toJSONObject(string, XMLParserConfiguration.ORIGINAL);
   }

   public static JSONObject toJSONObject(Reader reader) throws JSONException {
      return toJSONObject(reader, XMLParserConfiguration.ORIGINAL);
   }

   public static JSONObject toJSONObject(Reader reader, boolean keepStrings) throws JSONException {
      return keepStrings ? toJSONObject(reader, XMLParserConfiguration.KEEP_STRINGS) : toJSONObject(reader, XMLParserConfiguration.ORIGINAL);
   }

   public static JSONObject toJSONObject(Reader reader, XMLParserConfiguration config) throws JSONException {
      JSONObject jo = new JSONObject();
      XMLTokener x = new XMLTokener(reader);

      while (x.more()) {
         x.skipPast("<");
         if (x.more()) {
            parse(x, jo, null, config, 0);
         }
      }

      return jo;
   }

   public static JSONObject toJSONObject(String string, boolean keepStrings) throws JSONException {
      return toJSONObject(new StringReader(string), keepStrings);
   }

   public static JSONObject toJSONObject(String string, XMLParserConfiguration config) throws JSONException {
      return toJSONObject(new StringReader(string), config);
   }

   public static String toString(Object object) throws JSONException {
      return toString(object, null, XMLParserConfiguration.ORIGINAL);
   }

   public static String toString(Object object, String tagName) {
      return toString(object, tagName, XMLParserConfiguration.ORIGINAL);
   }

   public static String toString(Object object, String tagName, XMLParserConfiguration config) throws JSONException {
      return toString(object, tagName, config, 0, 0);
   }

   private static String toString(Object object, String tagName, XMLParserConfiguration config, int indentFactor, int indent) throws JSONException {
      StringBuilder sb = new StringBuilder();
      if (object instanceof JSONObject) {
         if (tagName != null) {
            sb.append(indent(indent));
            sb.append('<');
            sb.append(tagName);
            sb.append('>');
            if (indentFactor > 0) {
               sb.append("\n");
               indent += indentFactor;
            }
         }

         JSONObject jo = (JSONObject)object;

         for (String key : jo.keySet()) {
            Object value = jo.opt(key);
            if (value == null) {
               value = "";
            } else if (value.getClass().isArray()) {
               value = new JSONArray(value);
            }

            if (key.equals(config.getcDataTagName())) {
               if (value instanceof JSONArray) {
                  JSONArray ja = (JSONArray)value;
                  int jaLength = ja.length();

                  for (int i = 0; i < jaLength; i++) {
                     if (i > 0) {
                        sb.append('\n');
                     }

                     Object val = ja.opt(i);
                     sb.append(escape(val.toString()));
                  }
               } else {
                  sb.append(escape(value.toString()));
               }
            } else if (value instanceof JSONArray) {
               JSONArray ja = (JSONArray)value;
               int jaLength = ja.length();

               for (int i = 0; i < jaLength; i++) {
                  Object val = ja.opt(i);
                  if (val instanceof JSONArray) {
                     sb.append('<');
                     sb.append(key);
                     sb.append('>');
                     sb.append(toString(val, null, config, indentFactor, indent));
                     sb.append("</");
                     sb.append(key);
                     sb.append('>');
                  } else {
                     sb.append(toString(val, key, config, indentFactor, indent));
                  }
               }
            } else if ("".equals(value)) {
               sb.append(indent(indent));
               sb.append('<');
               sb.append(key);
               sb.append("/>");
               if (indentFactor > 0) {
                  sb.append("\n");
               }
            } else {
               sb.append(toString(value, key, config, indentFactor, indent));
            }
         }

         if (tagName != null) {
            sb.append(indent(indent - indentFactor));
            sb.append("</");
            sb.append(tagName);
            sb.append('>');
            if (indentFactor > 0) {
               sb.append("\n");
            }
         }

         return sb.toString();
      } else if (object == null || !(object instanceof JSONArray) && !object.getClass().isArray()) {
         String string = object == null ? "null" : escape(object.toString());
         if (tagName == null) {
            return indent(indent) + "\"" + string + "\"" + (indentFactor > 0 ? "\n" : "");
         } else {
            return string.length() == 0
               ? indent(indent) + "<" + tagName + "/>" + (indentFactor > 0 ? "\n" : "")
               : indent(indent) + "<" + tagName + ">" + string + "</" + tagName + ">" + (indentFactor > 0 ? "\n" : "");
         }
      } else {
         JSONArray ja;
         if (object.getClass().isArray()) {
            ja = new JSONArray(object);
         } else {
            ja = (JSONArray)object;
         }

         int jaLength = ja.length();

         for (int i = 0; i < jaLength; i++) {
            Object val = ja.opt(i);
            sb.append(toString(val, tagName == null ? "array" : tagName, config, indentFactor, indent));
         }

         return sb.toString();
      }
   }

   public static String toString(Object object, int indentFactor) {
      return toString(object, null, XMLParserConfiguration.ORIGINAL, indentFactor);
   }

   public static String toString(Object object, String tagName, int indentFactor) {
      return toString(object, tagName, XMLParserConfiguration.ORIGINAL, indentFactor);
   }

   public static String toString(Object object, String tagName, XMLParserConfiguration config, int indentFactor) throws JSONException {
      return toString(object, tagName, config, indentFactor, 0);
   }

   private static final String indent(int indent) {
      StringBuilder sb = new StringBuilder();

      for (int i = 0; i < indent; i++) {
         sb.append(' ');
      }

      return sb.toString();
   }
}
