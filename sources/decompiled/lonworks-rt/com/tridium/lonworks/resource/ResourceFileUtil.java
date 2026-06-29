package com.tridium.lonworks.resource;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.file.BIFile;
import javax.baja.naming.BOrd;

public final class ResourceFileUtil {
   private static final Logger LOG = Logger.getLogger("lonworks.file");

   private ResourceFileUtil() {
   }

   public static ResourceFile getResourceFile(String filename) throws IOException {
      String fname = filename.replace('\\', '/');
      BIFile f = (BIFile)BOrd.make("file:/" + fname).resolve().get();
      return getResourceFile(f);
   }

   public static ResourceFile getResourceFile(File file) throws IOException {
      ResourceFile var3;
      try (FileInputStream inputStream = new FileInputStream(file)) {
         var3 = getResourceFile(inputStream, file.getName());
      }

      return var3;
   }

   public static ResourceFile getResourceFile(BIFile f) throws IOException {
      ResourceFile var3;
      try (InputStream inputStream = f.getInputStream()) {
         var3 = getResourceFile(inputStream, f.getFileName());
      }

      return var3;
   }

   private static ResourceFile getResourceFile(InputStream in, String fileName) throws IOException {
      int len = in.available();
      byte[] a = new byte[len];

      for (int i = 0; i < len; i++) {
         a[i] = (byte)in.read();
      }

      return getResourceFile(new ResFileInputStream(a), fileName);
   }

   private static ResourceFile getResourceFile(ResFileInputStream in, String fileName) throws IOException {
      in.readCharacter();
      int fileType = typeStringToInt(in.readString(3));
      in.fileType = fileType;
      in.seek(0L);
      ResourceFile rf;
      switch (fileType) {
         case 1:
            rf = new CatalogFile();
            break;
         case 2:
            rf = new LanguageFile();
            break;
         case 3:
            rf = new TypeFile();
            break;
         case 4:
            rf = new FptFile();
            break;
         default:
            throw new RuntimeException("Unsupported type " + fileType);
      }

      rf.fileName = fileName;

      try {
         rf.parse(in);
      } catch (Throwable var5) {
         LOG.log(Level.SEVERE, "Failed to get resource file '" + fileName + "'", var5);
      }

      return rf;
   }

   public static int typeStringToInt(String s) {
      if (s.equalsIgnoreCase("CAT")) {
         return 1;
      } else if (s.equalsIgnoreCase("RES")) {
         return 2;
      } else if (s.equalsIgnoreCase("TYP")) {
         return 3;
      } else if (s.equalsIgnoreCase("FPT")) {
         return 4;
      } else {
         return s.equalsIgnoreCase("FMT") ? 5 : 0;
      }
   }

   public static String typeIntToString(int t) {
      switch (t) {
         case 1:
            return "Catalog";
         case 2:
            return "Lanquage Resource";
         case 3:
            return "Type";
         case 4:
            return "FPT";
         case 5:
            return "Format";
         default:
            return "Unknown file type";
      }
   }
}
