package com.tridium.devkit.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.HashSet;
import java.util.Set;
import javax.baja.converters.BIBooleanToSimple;
import javax.baja.converters.BINumericToSimple;
import javax.baja.gx.BImage;
import javax.baja.naming.BOrd;
import javax.baja.registry.TypeInfo;
import javax.baja.sys.Type;
import javax.baja.ui.BValueBinding;
import javax.baja.util.BTypeSpec;

public class PaletteGenerator {
   private static final Type LABEL_TYPE = BTypeSpec.make("kitPx:BoundLabel").getResolvedType();
   String module;
   String src;
   BufferedWriter palette;
   Set<String> modules = new HashSet<>();

   private void go(String module) throws Exception {
      int n = module.lastIndexOf("\\");
      if (n == -1) {
         this.module = module;
      } else {
         this.module = module.substring(module.lastIndexOf("\\"));
      }

      this.src = module + "\\src";
      this.palette = new BufferedWriter(new FileWriter(module + "\\module.palette"));
      this.w("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
      this.w("<bajaObjectGraph version=\"1.0\">\n");
      this.w("<p m=\"b=baja\" t=\"b:UnrestrictedFolder\">\n");
      this.traverse(new File(this.src), 0);
      this.w("</p>\n");
      this.w("</bajaObjectGraph>\n");
      this.palette.close();
   }

   private void traverse(File file, int depth) throws Exception {
      if (file.isDirectory()) {
         if (depth > 0) {
            this.indent(depth);
            String folder = file.getName();
            int n = folder.lastIndexOf(92);
            folder = folder.substring(n + 1);
            this.w("<p n=\"" + folder + "\" t=\"b:UnrestrictedFolder\">\n");
         }

         File[] children = file.listFiles();

         for (int i = 0; i < children.length; i++) {
            this.traverse(children[i], depth + 1);
         }

         if (depth > 0) {
            this.indent(depth);
            this.w("</p>\n");
         }
      } else {
         this.process(file, depth + 1);
      }
   }

   private void indent(int depth) throws Exception {
      for (int i = 0; i < depth; i++) {
         this.w("  ");
      }
   }

   private void w(String str) throws Exception {
      this.palette.write(str);
   }

   private void process(File file, int depth) throws Exception {
      String abs = file.getAbsolutePath();
      if (abs.endsWith(".gif")) {
         if (abs.endsWith("True.gif")) {
            String other = abs.substring(0, abs.length() - "True.gif".length()) + "False.gif";
            if (new File(other).exists()) {
               String name = this.name(abs, "True.gif");
               String path = this.path(abs);
               String layout = this.layoutFromGif(this.gifOrd(path, name, "True.gif"));
               this.indent(depth);
               this.w("<p n=\"" + name + "\" " + this.t(LABEL_TYPE) + ">\n");
               this.indent(depth);
               this.w("  <p n=\"layout\" v=\"" + layout + "\"/>\n");
               this.indent(depth);
               this.w("  <p n=\"bnd\" " + this.t(BValueBinding.TYPE) + ">\n");
               this.indent(depth);
               this.w("     <p n=\"image\" " + this.t(BIBooleanToSimple.TYPE) + ">\n");
               this.indent(depth);
               this.w("       <p n=\"trueValue\" " + this.t(BImage.TYPE) + " ");
               this.w("v=\"" + this.gifOrd(path, name, "True.gif") + "\"/>\n");
               this.indent(depth);
               this.w("       <p n=\"falseValue\" " + this.t(BImage.TYPE) + " ");
               this.w("v=\"" + this.gifOrd(path, name, "False.gif") + "\"/>\n");
               this.indent(depth);
               this.w("     </p>\n");
               this.indent(depth);
               this.w("  </p>\n");
               this.indent(depth);
               this.w("</p>\n");
            }
         } else if (abs.endsWith("1.gif")) {
            String name = this.name(abs, "1.gif");
            String path = this.path(abs);
            String layout = this.layoutFromGif(this.gifOrd(path, name, "1.gif"));
            String root = abs.substring(0, abs.length() - "1.gif".length());
            int a = new File(root + "0.gif").exists() ? 0 : 1;
            int b = 2;
            File f = new File(root + b + ".gif");

            while (f.exists()) {
               f = new File(root + ++b + ".gif");
            }

            StringBuilder map = new StringBuilder("gx:Image ");
            int interval = (int)(100.0 / (b - a));
            int val = 0;

            for (int i = a; i < b; i++) {
               if (i == a) {
                  map.append("-inf:");
               } else {
                  map.append(val).append(":");
               }

               if (i + 1 == b) {
                  map.append("+inf=");
               } else {
                  map.append(Integer.toString(val + interval)).append("=");
               }

               map.append(this.gifOrd(path, name, new String(i + ".gif"))).append(";");
               val += interval;
            }

            map.append("default=null;");
            this.indent(depth);
            this.w("<p n=\"" + name + "\" " + this.t(LABEL_TYPE) + ">\n");
            this.indent(depth);
            this.w("  <p n=\"layout\" v=\"" + layout + "\"/>\n");
            this.indent(depth);
            this.w("  <p n=\"bnd\" " + this.t(BValueBinding.TYPE) + ">\n");
            this.indent(depth);
            this.w("     <p n=\"image\" " + this.t(BINumericToSimple.TYPE) + ">\n");
            this.indent(depth);
            this.w("       <p n=\"map\" v=\"" + map + "\"/>\n");
            this.indent(depth);
            this.w("     </p>\n");
            this.indent(depth);
            this.w("  </p>\n");
            this.indent(depth);
            this.w("</p>\n");
         } else {
            if (abs.endsWith("False.gif")) {
               return;
            }

            char ch = abs.charAt(abs.length() - 5);
            if (ch >= '0' && ch <= '9') {
               return;
            }

            String name = this.name(abs, ".gif");
            String gifOrd = this.gifOrd(this.path(abs), name, ".gif");
            this.indent(depth);
            this.w("<p n=\"" + name + "\" " + this.t(LABEL_TYPE) + ">\n");
            this.indent(depth);
            this.w("  <p n=\"layout\" v=\"" + this.layoutFromGif(gifOrd) + "\"/>\n");
            this.indent(depth);
            this.w("  <p n=\"image\" v=\"" + gifOrd + "\"/>\n");
            this.indent(depth);
            this.w("</p>\n");
         }
      }
   }

   private String name(String abs, String suffix) {
      int a = abs.indexOf("\\src\\");
      int b = abs.lastIndexOf("\\");
      return abs.substring(b + 1, abs.length() - suffix.length());
   }

   private String path(String abs) {
      int a = abs.indexOf("\\src\\");
      int b = abs.lastIndexOf("\\");
      String path = abs.substring(a + "\\src\\".length(), b);
      return path.replace('\\', '/');
   }

   private String layoutFromGif(String ord) {
      BImage img = BImage.make(BOrd.make(ord));
      img.sync();
      return "0,0," + img.getWidth() + "," + img.getHeight();
   }

   private String gifOrd(String path, String name, String suffix) {
      return "module://" + this.module + "/" + path + "/" + name + suffix;
   }

   private String t(Type type) {
      TypeInfo info = type.getTypeInfo();
      String module = info.getModuleName();
      if (this.modules.contains(module)) {
         return "t=\"" + this.id(module) + ":" + info.getTypeName() + "\"";
      } else {
         this.modules.add(module);
         return "m=\"" + this.id(module) + "=" + module + "\" t=\"" + this.id(module) + ":" + info.getTypeName() + "\"";
      }
   }

   private String id(String module) {
      if (module.equals("bajaui")) {
         return "u";
      } else if (module.equals("converters")) {
         return "c";
      } else if (module.equals("gx")) {
         return "g";
      } else if (module.equals("kitPx")) {
         return "k";
      } else {
         throw new IllegalStateException(module);
      }
   }

   private static void usage() {
      System.out.println();
      System.out.println("Usage: PaletteGenerator <moduleDir>");
      System.exit(1);
   }

   public static void main(String[] args) {
      if (args.length != 1) {
         usage();
      }

      try {
         new PaletteGenerator().go(args[0]);
      } catch (Exception var2) {
         System.err.println("Failed to generate palette: " + var2);
         var2.printStackTrace();
      }
   }
}
