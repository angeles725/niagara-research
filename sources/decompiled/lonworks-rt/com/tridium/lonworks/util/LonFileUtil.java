package com.tridium.lonworks.util;

import javax.baja.file.BAbstractFile;
import javax.baja.file.BDirectory;
import javax.baja.file.BIFile;
import javax.baja.file.FilePath;
import javax.baja.naming.BOrd;
import javax.baja.sys.BajaRuntimeException;

public class LonFileUtil {
   public static BIFile getFile(String filename) {
      try {
         return (BIFile)BOrd.make(fixFilename(filename)).resolve().get();
      } catch (Throwable var2) {
         throw new BajaRuntimeException("Unable to access file " + filename, var2);
      }
   }

   public static BIFile getFile(BDirectory dir, String filename) {
      try {
         return dir.getFileSpace().findFile(getFilePath(dir, filename));
      } catch (Throwable var3) {
         throw new BajaRuntimeException("Unable to access file " + filename + " in " + dir.getFilePath().getBody(), var3);
      }
   }

   private static FilePath getFilePath(BDirectory dir, String filename) {
      return new FilePath(dir.getFilePath().getBody() + "/" + filename);
   }

   public static BDirectory getDirectory(String filename) {
      try {
         BAbstractFile f = (BAbstractFile)BOrd.make(fixFilename(filename)).resolve().get();
         if (!f.isDirectory()) {
            throw new RuntimeException(filename + " is not a directory.");
         } else {
            return (BDirectory)f;
         }
      } catch (Throwable var2) {
         throw new BajaRuntimeException("Unable to access directory " + filename, var2);
      }
   }

   public static BIFile getOrMakeFile(BDirectory dir, String filename) {
      try {
         FilePath fp = getFilePath(dir, filename);
         return dir.getFileSpace().makeFile(fp);
      } catch (Throwable var4) {
         throw new BajaRuntimeException("Unable to create directory " + filename, var4);
      }
   }

   private static String fixFilename(String filename) {
      String fname = filename.replace('\\', '/');
      return "file:/" + fname;
   }
}
