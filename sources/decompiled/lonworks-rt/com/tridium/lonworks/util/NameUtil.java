package com.tridium.lonworks.util;

import java.io.File;
import java.io.IOException;
import java.util.Vector;

public class NameUtil {
   public static String toJavaName(String str, boolean makeFirstUpper) {
      str = replaceNonAlphaNumerics(str, '_');
      StringBuilder sb = new StringBuilder();
      boolean mixedCase = !str.equals(str.toUpperCase());
      boolean transition = false;
      boolean lastDigit = false;
      if (Character.isDigit(str.charAt(0))) {
         str = "lon" + str;
      }

      for (int i = 0; i < str.length(); i++) {
         char c = str.charAt(i);
         if (c != '_' && c != '-' && c != ' ') {
            if (c >= '0' && c <= '9') {
               sb.append(c);
               transition = true;
               lastDigit = true;
            } else {
               if (transition) {
                  sb.append(Character.toUpperCase(c));
               } else if (mixedCase) {
                  sb.append(c);
               } else {
                  sb.append(Character.toLowerCase(c));
               }

               transition = false;
               lastDigit = false;
            }
         } else {
            if (i > 0) {
               transition = true;
            }

            if (lastDigit && i + 1 < str.length() && Character.isDigit(str.charAt(i + 1))) {
               sb.append('_');
            }

            lastDigit = false;
         }
      }

      if (makeFirstUpper) {
         sb.setCharAt(0, Character.toUpperCase(sb.charAt(0)));
      }

      return sb.toString();
   }

   public static String toConstantName(String name) {
      StringBuilder constantName = new StringBuilder();
      boolean lastCharUpperCase = false;
      boolean lastUnderBar = false;
      boolean lastDigit = false;

      for (int i = 0; i < name.length(); i++) {
         char c = name.charAt(i);
         if (Character.isLowerCase(c)) {
            if (lastDigit) {
               constantName.append("_");
            }

            constantName.append(c);
            lastCharUpperCase = false;
            lastDigit = false;
            lastUnderBar = false;
         } else if (Character.isDigit(c)) {
            if (!lastDigit && !lastUnderBar) {
               constantName.append("_");
            }

            constantName.append(c);
            lastCharUpperCase = false;
            lastDigit = true;
            lastUnderBar = false;
         } else if (c != '_' && c != ' ') {
            if (lastUnderBar) {
               lastUnderBar = false;
            } else if (lastDigit) {
               constantName.append("_");
            } else if (lastCharUpperCase) {
               if (i + 1 < name.length() && Character.isLowerCase(name.charAt(i + 1))) {
                  constantName.append("_");
               }
            } else if (i != 0) {
               constantName.append("_");
            }

            constantName.append(c);
            lastCharUpperCase = true;
            lastDigit = false;
         } else {
            constantName.append('_');
            lastCharUpperCase = false;
            lastDigit = false;
            lastUnderBar = true;
         }
      }

      return constantName.toString().toUpperCase();
   }

   private static String replaceNonAlphaNumerics(String str, char replacement) {
      StringBuilder retVal = new StringBuilder();
      if (str != null) {
         for (int i = 0; i < str.length(); i++) {
            char strI = str.charAt(i);
            if (Character.isLetterOrDigit(strI)) {
               retVal.append(strI);
            } else {
               retVal.append(replacement);
            }
         }
      }

      return retVal.toString();
   }

   public static String uniqueName(Vector<String> names, String name) {
      String n = name;
      int i = 1;

      while (names.contains(n)) {
         n = name + i++;
      }

      names.addElement(n);
      return n;
   }

   public static String getPackageName(String className) {
      if (className == null) {
         return null;
      } else {
         int i = className.lastIndexOf(46);
         return className.substring(0, i);
      }
   }

   public static String getClassName(String className) {
      if (className == null) {
         return null;
      } else {
         int i = className.lastIndexOf(46);
         return className.substring(i + 1);
      }
   }

   public static String getClassName(Class<?> c) {
      return c == null ? null : getClassName(c.getName());
   }

   public static final String capitalize(String name) {
      if (name == null) {
         return null;
      } else {
         StringBuilder buf = new StringBuilder(name);
         char c = buf.charAt(0);
         buf.setCharAt(0, Character.toUpperCase(c));
         return buf.toString();
      }
   }

   public static final String toEnumName(String prefix, String ename) {
      StringBuilder sb = new StringBuilder(prefix);
      sb.append(Character.toUpperCase(ename.charAt(0))).append(ename.substring(1));
      if (!ename.endsWith("Enum")) {
         sb.append("Enum");
      }

      return sb.toString();
   }

   public static final String toDsName(String prefix, String dname) {
      StringBuilder sb = new StringBuilder();
      sb.append("Ds").append(prefix);
      sb.append(Character.toUpperCase(dname.charAt(0))).append(dname.substring(1));
      return sb.toString();
   }

   public static String dirToClass(String dirName) {
      return dirName.replace('/', '.').replace('\\', '.');
   }

   public static String classToDir(String dirName) {
      return dirName.replace('.', '/');
   }

   public static String getParent(File file) throws IOException {
      String path = file.getCanonicalPath();
      return path.substring(0, path.lastIndexOf(System.getProperty("file.separator")));
   }

   public static String getFileNameFromPath(String pathName, boolean removeExtension) throws IOException {
      String name = pathName.substring(pathName.lastIndexOf(System.getProperty("file.separator")) + 1, pathName.length());
      if (removeExtension) {
         name = name.substring(0, name.lastIndexOf(46));
      }

      return name;
   }
}
