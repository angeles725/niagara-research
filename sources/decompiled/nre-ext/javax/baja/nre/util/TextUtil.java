package javax.baja.nre.util;

import java.nio.charset.StandardCharsets;

public class TextUtil {
   private static String[] SPACES = new String[50];
   private static String[] ZEROS;

   public static byte[] stringToBytes(String string) {
      return string.getBytes(StandardCharsets.UTF_8);
   }

   public static String byteToString(int b) {
      return Integer.toString(b);
   }

   public static String bytesToHexString(byte[] bs) {
      if (bs == null) {
         return "";
      }

      StringBuilder workingBuffer = new StringBuilder();

      for (byte b : bs) {
         workingBuffer.append(byteToHexString(b));
      }

      return workingBuffer.toString();
   }

   public static String byteToHexString(int b) {
      return intToHexString(b & 0xFF, 2);
   }

   public static char byteToChar(int b, char unprintable) {
      return b >= 32 && b <= 126 ? (char)b : unprintable;
   }

   public static String intToHexString(int i) {
      return padZeros(Integer.toHexString(i), 8);
   }

   public static String intToHexString(int i, int len) {
      return padZeros(Integer.toHexString(i), len);
   }

   public static int charToInt(char c) {
      int x = c - '0';
      if (x >= 0 && x <= 9) {
         return x;
      } else {
         throw new IllegalArgumentException("'" + c + "'");
      }
   }

   public static int hexCharToInt(char c) {
      int x = c - '0';
      if (x >= 0 && x <= 9) {
         return x;
      } else {
         x = c - 'A' + 10;
         if (x >= 10 && x <= 15) {
            return x;
         } else {
            x = c - 'a' + 10;
            if (x >= 10 && x <= 15) {
               return x;
            } else {
               throw new IllegalArgumentException("'" + c + "'");
            }
         }
      }
   }

   public static boolean isHex(String str) {
      for (int i = 0; i < str.length(); i++) {
         char c = str.charAt(i);
         if ((c < '0' || c > '9') && (c < 'A' || c > 'F') && (c < 'a' || c > 'f')) {
            return false;
         }
      }

      return true;
   }

   public static String getSpaces(int num) {
      try {
         return SPACES[num];
      } catch (ArrayIndexOutOfBoundsException e) {
         if (num < 0) {
            return "";
         }

         int len = SPACES.length;
         StringBuilder buf = new StringBuilder(num);

         int rem;
         for (rem = num; rem >= len; rem -= len - 1) {
            buf.append(SPACES[len - 1]);
         }

         buf.append(SPACES[rem]);
         return buf.toString();
      }
   }

   public static String pad(String s, int width) {
      return padRight(s, width);
   }

   public static String padRight(String s, int width) {
      return s.length() >= width ? s : s + getSpaces(width - s.length());
   }

   public static String padLeft(String s, int width) {
      return s.length() >= width ? s : getSpaces(width - s.length()) + s;
   }

   public static String truncate(String str, int max) {
      return str.length() > max ? str.substring(0, max) : str;
   }

   public static String padZeros(String s, int width) {
      return s.length() >= width ? s : getZeros(width - s.length()) + s;
   }

   public static String getZeros(int num) {
      try {
         return ZEROS[num];
      } catch (ArrayIndexOutOfBoundsException e) {
         if (num < 0) {
            return "";
         }

         int len = ZEROS.length;
         StringBuilder buf = new StringBuilder(num);

         int rem;
         for (rem = num; rem >= len; rem -= len - 1) {
            buf.append(ZEROS[len - 1]);
         }

         buf.append(ZEROS[rem]);
         return buf.toString();
      }
   }

   public static char toUpperCase(char c) {
      return 'a' <= c && c <= 'z' ? (char)(c & -33) : c;
   }

   public static char toLowerCase(char c) {
      return 'A' <= c && c <= 'Z' ? (char)(c | 32) : c;
   }

   public static String toUpperCase(String s) {
      int len = s.length();
      int first = -1;

      for (int i = 0; i < len; i++) {
         char a = s.charAt(i);
         char b = toUpperCase(a);
         if (a != b) {
            first = i;
            break;
         }
      }

      if (first == -1) {
         return s;
      }

      char[] buf = new char[len];
      s.getChars(0, first, buf, 0);

      for (int i = first; i < len; i++) {
         buf[i] = toUpperCase(s.charAt(i));
      }

      return new String(buf);
   }

   public static String toLowerCase(String s) {
      int len = s.length();
      int first = -1;

      for (int i = 0; i < len; i++) {
         char a = s.charAt(i);
         char b = toLowerCase(a);
         if (a != b) {
            first = i;
            break;
         }
      }

      if (first == -1) {
         return s;
      }

      char[] buf = new char[len];
      s.getChars(0, first, buf, 0);

      for (int i = first; i < len; i++) {
         buf[i] = toLowerCase(s.charAt(i));
      }

      return new String(buf);
   }

   public static String capitalize(String s) {
      char[] c = s.toCharArray();
      c[0] = toUpperCase(c[0]);
      return new String(c);
   }

   public static String decapitalize(String s) {
      char[] c = s.toCharArray();
      c[0] = toLowerCase(c[0]);
      return new String(c);
   }

   public static String toFriendly(String s) {
      StringBuilder buf = new StringBuilder();
      buf.append(Character.toUpperCase(s.charAt(0)));
      int len = s.length();

      for (int i = 1; i < len; i++) {
         char c = s.charAt(i);
         if ((c & ' ') == 0 && i > 0) {
            buf.append(' ').append(c);
         } else {
            buf.append(c);
         }
      }

      return buf.toString();
   }

   public static String fromFriendly(String s) {
      StringBuilder buf = new StringBuilder(s.length());
      buf.append(Character.toLowerCase(s.charAt(0)));
      int len = s.length();

      int i;
      for (i = 1; i < len; i++) {
         char c = s.charAt(i);
         if (c == ' ') {
            break;
         }

         buf.append(c);
      }

      for (; i < len; i++) {
         char c = s.charAt(i);
         if (c != ' ') {
            buf.append(c);
         }
      }

      return buf.toString();
   }

   public static String getClassName(Class<?> cls) {
      return getClassName(cls.getName());
   }

   public static String getClassName(String className) {
      int x = className.lastIndexOf(46);
      if (x >= 0) {
         className = className.substring(x + 1);
      }

      if (className.charAt(className.length() - 1) == ';') {
         className = className.substring(0, className.length() - 1);
      }

      return className;
   }

   public static String getPackageName(Class<?> cls) {
      return getPackageName(cls.getName());
   }

   public static String getPackageName(String className) {
      int x = className.lastIndexOf(46);
      return x < 0 ? null : className.substring(0, x);
   }

   public static String stripMarkup(String text) {
      StringBuilder s = new StringBuilder();
      char[] buf = text.toCharArray();

      for (int i = 0; i < buf.length; i++) {
         char c = buf[i];
         if (c != '<') {
            s.append(c);
         } else {
            int end = -1;

            for (int j = i + 1; j < buf.length; j++) {
               if (buf[j] == '>') {
                  end = j;
                  break;
               }
            }

            if (end != -1) {
               i = end;
            }
         }
      }

      return s.toString();
   }

   public static String[] split(String str, char delim) {
      if (str.indexOf(delim) == -1) {
         return str.length() == 0 ? new String[0] : new String[]{str};
      }

      String[] list = new String[8];
      int a = 0;
      int b = 0;
      int n = 0;

      while (b < str.length()) {
         if (str.charAt(b) == delim) {
            list = ensureCapacity(list, n);
            list[n++] = str.substring(a, b);
            a = ++b;
         } else {
            b++;
         }
      }

      list = ensureCapacity(list, n);
      list[n++] = str.substring(a, str.length());
      if (n == list.length) {
         return list;
      }

      String[] trim = new String[n];
      System.arraycopy(list, 0, trim, 0, n);
      return trim;
   }

   public static String[] splitAndTrim(String str, char delim) {
      return trim(split(str, delim));
   }

   public static String[] ensureCapacity(String[] x, int len) {
      if (len < x.length) {
         return x;
      }

      String[] expand = new String[x.length * 2];
      System.arraycopy(x, 0, expand, 0, x.length);
      return expand;
   }

   public static String[] trim(String[] list) {
      if (list == null) {
         return null;
      }

      for (int i = 0; i < list.length; i++) {
         if (list[i] != null) {
            list[i] = list[i].trim();
         }
      }

      return list;
   }

   public static String join(String[] v, char delim) {
      if (v.length == 0) {
         return "";
      }

      StringBuilder sb = new StringBuilder();
      int n = v.length - 1;

      for (int i = 0; i < n; i++) {
         sb.append(v[i]).append(delim);
      }

      sb.append(v[n]);
      return sb.toString();
   }

   public static String trimLeft(String s) {
      StringBuilder sb = new StringBuilder(s);

      while (sb.length() > 0 && Character.isWhitespace(sb.charAt(0))) {
         sb.deleteCharAt(0);
      }

      return sb.toString();
   }

   public static String trimRight(String s) {
      StringBuilder sb = new StringBuilder(s);

      for (int len = sb.length(); len > 0 && Character.isWhitespace(sb.charAt(len - 1)); len--) {
         sb.deleteCharAt(len - 1);
      }

      return sb.toString();
   }

   public static String replace(String text, String oldStr, String newStr) {
      if (text == null) {
         throw new NullPointerException();
      }

      if (oldStr == null) {
         throw new NullPointerException();
      }

      if (newStr == null) {
         throw new NullPointerException();
      }

      int b = text.indexOf(oldStr, 0);
      return b == -1 ? text : doReplace(new StringBuffer(text), oldStr, newStr, b).toString();
   }

   public static StringBuffer replace(StringBuffer text, String oldStr, String newStr) {
      if (text == null) {
         throw new NullPointerException();
      }

      if (oldStr == null) {
         throw new NullPointerException();
      }

      if (newStr == null) {
         throw new NullPointerException();
      }

      int b = indexOf(text, oldStr, 0);
      return b == -1 ? text : doReplace(text, oldStr, newStr, b);
   }

   private static StringBuffer doReplace(StringBuffer text, String oldStr, String newStr, int b) {
      int n1 = oldStr.length();
      int n2 = newStr.length();

      while (b != -1) {
         text.replace(b, b + n1, newStr);
         b = indexOf(text, oldStr, b + n2);
      }

      return text;
   }

   public static int indexOf(StringBuffer buffer, String pattern) {
      return indexOf(buffer, pattern, 0);
   }

   public static int indexOf(StringBuffer buffer, String pattern, int fromIndex) {
      if (buffer == null) {
         throw new NullPointerException();
      }

      if (pattern == null) {
         throw new NullPointerException();
      }

      int[] overlap = computeOverlap(pattern);
      int j = 0;
      int n = buffer.length();
      int m = pattern.length();
      int z = fromIndex < 0 ? 0 : fromIndex;

      label37:
      for (int i = z; i < n; i++) {
         while (buffer.charAt(i) != pattern.charAt(j)) {
            if (j == 0) {
               continue label37;
            }

            j = overlap[j];
         }

         if (++j == m) {
            return i - m + 1;
         }
      }

      return -1;
   }

   private static int[] computeOverlap(String pattern) {
      int m = pattern.length();
      int[] overlap = new int[m + 1];
      overlap[0] = -1;

      for (int i = 0; i < m; i++) {
         overlap[i + 1] = overlap[i] + 1;

         while (overlap[i + 1] > 0 && pattern.charAt(i) != pattern.charAt(overlap[i + 1] - 1)) {
            overlap[i + 1] = overlap[overlap[i + 1] - 1] + 1;
         }
      }

      return overlap;
   }

   public static String unquote(String value) {
      if (value != null && value.length() >= 2 && value.charAt(0) == '"' && value.charAt(value.length() - 1) == '"') {
         value = value.substring(1, value.length() - 1);
      }

      return value;
   }

   static {
      SPACES[0] = "";

      for (int i = 1; i < 50; i++) {
         SPACES[i] = SPACES[i - 1] + " ";
      }

      ZEROS = new String[16];
      ZEROS[0] = "";

      for (int i = 1; i < 16; i++) {
         ZEROS[i] = ZEROS[i - 1] + "0";
      }
   }
}
