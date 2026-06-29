package com.tridium.lonworks.resource;

import com.tridium.lonworks.util.LonFileUtil;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.security.AccessController;
import java.security.PrivilegedAction;
import javax.baja.file.BDirectory;
import javax.baja.file.BIFile;
import javax.baja.naming.BOrd;
import javax.baja.naming.UnresolvedException;
import javax.baja.nre.util.Array;
import javax.baja.nre.util.ByteArrayUtil;
import javax.baja.nre.util.DefaultFileCopy;
import javax.baja.units.BUnit;

public class CrossReference {
   Conversion.UnitPrompt unitPrompt = null;
   boolean[] marks = new boolean[]{false, false, false, false, false, false, false};
   CrossReference.Resource resources = null;
   CrossReference.Resource standard = null;
   PrintStream out;

   public CrossReference(boolean createStandard, String dir) {
      this.out = System.out;
      String fname = "standard";
      CrossReference.Resource res = new CrossReference.Resource();
      res.fileName = fname;

      try {
         BIFile f = LonFileUtil.getFile(dir + "/STANDARD.typ");
         res.typFile = (TypeFile)ResourceFileUtil.getResourceFile(f);
         f = LonFileUtil.getFile(dir + "/STANDARD.fpt");
         res.fptFile = (FptFile)ResourceFileUtil.getResourceFile(f);
         f = LonFileUtil.getFile(dir + "/STANDARD.enu");
         res.lanFile = (LanguageFile)ResourceFileUtil.getResourceFile(f);
         File loc = new File(".");
         f = LonFileUtil.getFile(loc.getCanonicalPath() + "/StandardConversion.xml");
         res.conversion = new Conversion(f);
      } catch (Throwable var7) {
         System.out.println(var7);
      }

      this.standard = res;
      res.standard = true;
      res.next = this.resources;
      this.resources = res;
   }

   public CrossReference() {
      this.out = System.out;
      this.addStandard();
   }

   public CrossReference(PrintStream out) {
      this.out = System.out;
      this.out = out;
      AccessController.doPrivileged((PrivilegedAction<Void>)(() -> {
         this.addStandard();
         return null;
      }));
   }

   private void addStandard() {
      String fname = "standard";
      CrossReference.Resource res = new CrossReference.Resource();
      res.fileName = fname;
      BIFile f = (BIFile)BOrd.make("module://lonworks/type/Standard.typ").get();

      try {
         res.typFile = (TypeFile)ResourceFileUtil.getResourceFile(f);
      } catch (IOException var8) {
         this.out.println(var8);
      }

      f = (BIFile)BOrd.make("module://lonworks/type/Standard.fpt").get();

      try {
         res.fptFile = (FptFile)ResourceFileUtil.getResourceFile(f);
      } catch (IOException var7) {
         this.out.println("No \".fpt\" file for " + fname);
      }

      f = (BIFile)BOrd.make("module://lonworks/type/Standard.enu").get();

      try {
         res.lanFile = (LanguageFile)ResourceFileUtil.getResourceFile(f);
      } catch (IOException var6) {
         this.out.println("No \".enu\" file for " + fname);
      }

      try {
         DefaultFileCopy.copyFile("lonStandardConversion.xml");
         f = (BIFile)BOrd.make("file:!defaults/lonStandardConversion.xml").get();
      } catch (IOException | UnresolvedException var5) {
         f = (BIFile)BOrd.make("module://lonworks/type/StandardConversion.xml").get();
      }

      res.conversion = new Conversion(f);
      this.standard = res;
      res.standard = true;
      this.addResource(res);
   }

   private void addResource(CrossReference.Resource nres) {
      if (this.resources != null && this.resources.typFile.scope > nres.typFile.scope) {
         CrossReference.Resource res = this.resources;

         for (CrossReference.Resource nxt = res.next; nxt != null; nxt = nxt.next) {
            if (nxt.typFile.scope < nres.typFile.scope) {
               nres.next = nxt;
               res.next = nres;
               return;
            }

            res = nxt;
         }

         res.next = nres;
      } else {
         nres.next = this.resources;
         this.resources = nres;
      }
   }

   private void verifyUniqueResourceId(TypeFile tFile, String fName) throws CrossReference.DuplicateResourceException {
      for (CrossReference.Resource res = this.resources; res != null; res = res.next) {
         if (res.typFile.scope == tFile.scope && this.compareArray(res.typFile.refId, tFile.refId, 8)) {
            throw new CrossReference.DuplicateResourceException(
               "Duplicate resource sets: "
                  + fName
                  + " & "
                  + this.stripFilename(res.fileName)
                  + " for scope "
                  + res.typFile.scope
                  + " and refId: "
                  + ByteArrayUtil.toHexString(res.typFile.refId)
            );
         }
      }
   }

   private boolean compareArray(byte[] a1, byte[] a2, int lenToComp) {
      if (lenToComp == 0) {
         return true;
      } else {
         if (a1[0] != 15) {
            if ((a1[0] & 15) != (a2[0] & 15)) {
               return false;
            }

            int a = a1[0] >> 4 & 15;
            int b = a2[0] >> 4 & 15;
            if (a != 8 && a != 9 || b != 8 && b != 9) {
               return false;
            }
         }

         for (int i = 1; i < lenToComp; i++) {
            if (a1[i] != a2[i]) {
               return false;
            }
         }

         return true;
      }
   }

   public void addResource(BDirectory dir, String fileName, TypeFile tFile) throws CrossReference.DuplicateResourceException {
      String fname = this.stripFilename(fileName);
      CrossReference.Resource res = new CrossReference.Resource();
      res.fileName = fname;
      res.typFile = tFile;
      if (res.typFile == null) {
         res.typFile = (TypeFile)this.getResourceFile(dir, fname, ".typ");
      }

      res.fptFile = (FptFile)this.getResourceFile(dir, fname, ".fpt");
      res.lanFile = this.getLanguageFile(dir, fname);
      this.verifyUniqueResourceId(res.typFile, fname);
      BIFile f = LonFileUtil.getOrMakeFile(dir, fname + "Conversion.xml");
      if (f != null) {
         res.conversion = new Conversion(f);
      }

      res.conversion.setUnitPrompt(this.unitPrompt);
      this.addResource(res);
   }

   private LanguageFile getLanguageFile(BDirectory resDir, String typName) {
      BIFile[] files = resDir.listFiles();
      LanguageFile lf = null;

      for (int i = 0; i < files.length; i++) {
         if (this.fileNameNoExt(files[i].getFileName()).equals(typName)) {
            ResourceFile rf = null;

            try {
               rf = ResourceFileUtil.getResourceFile(files[i]);
            } catch (Throwable var8) {
            }

            if (rf != null && rf.fileType == 2) {
               if (lf == null) {
                  lf = (LanguageFile)rf;
                  this.out.println("Using language file: " + files[i].getFileName());
               } else {
                  this.out.println("Detected duplicate language file: " + files[i].getFileName());
               }
            }
         }
      }

      return null;
   }

   private String fileNameNoExt(String fName) {
      int n = fName.indexOf(".");
      return n > 0 ? fName.substring(0, n) : fName;
   }

   private String stripFilename(String fileName) {
      return fileName.lastIndexOf(46) > 0 ? fileName.substring(0, fileName.lastIndexOf(46)) : fileName;
   }

   private ResourceFile getResourceFile(BDirectory dir, String fname, String ext) {
      try {
         BIFile lf = LonFileUtil.getFile(dir, fname + ext);
         if (lf != null) {
            return ResourceFileUtil.getResourceFile(lf);
         }
      } catch (Exception var5) {
      }

      this.out.println("Error opening " + fname + ext);
      return null;
   }

   public Type findNvType(int scope, int index) {
      if (index <= 0) {
         throw new RuntimeException("Not a valid index (" + index + ")");
      } else {
         CrossReference.Resource res = this.getResouce(scope);
         TypeFile tf = res.typFile;
         if (tf == null) {
            Thread.dumpStack();
         }

         if (tf == null) {
            throw new RuntimeException("No type file for scope " + scope);
         } else {
            Type[] ta = tf.nvTypes;
            if (index > ta.length) {
               throw new RuntimeException("No nvType index (" + index + ") found in " + tf.fileName);
            } else {
               return ta[index - 1];
            }
         }
      }
   }

   public CpType findCpType(int scope, int index) {
      CrossReference.Resource res = this.getResouce(scope);
      TypeFile tf = res.typFile;
      if (tf == null) {
         Thread.dumpStack();
      }

      if (tf == null) {
         throw new RuntimeException("No type file for scope " + scope);
      } else {
         CpType[] ta = tf.cpTypes;
         if (index >= 0 && index <= ta.length) {
            return ta[index - 1];
         } else {
            throw new RuntimeException("No cpType index (" + index + ") found in " + tf.fileName);
         }
      }
   }

   public EnumSet findEnum(int scope, int index) {
      CrossReference.Resource res = this.getResouce(scope);
      TypeFile tf = res.typFile;
      if (tf == null) {
         Thread.dumpStack();
      }

      if (tf == null) {
         throw new RuntimeException("No type file for scope " + scope);
      } else {
         EnumSet[] ea = tf.enumSets;
         if (index > ea.length) {
            throw new RuntimeException("No enum index " + index + " found in " + tf.fileName);
         } else {
            EnumSet es = ea[index - 1];
            if (res.conversion != null && res.conversion.isBooleanEnum(es.enumTag)) {
               es.isBoolean = true;
            }

            return es;
         }
      }
   }

   public Type findNvTypeByObject(int objType, int memNdx, boolean mfgMember) {
      CrossReference.Resource res = this.resources;

      while (res != null) {
         if (res.standard == mfgMember) {
            res = res.next;
         } else {
            FptFile f = res.fptFile;
            Fpt fpt = f.getFptByKey(objType);
            if (fpt != null) {
               Fpt.Nv nv = fpt.getMemberNv(memNdx);
               if (nv == null) {
                  throw new RuntimeException("No member " + memNdx + " for object type " + objType + " in file " + f.fileName);
               }

               if (nv.nvTIndex <= 0) {
                  throw new RuntimeException("Member nv " + memNdx + " for object type " + objType + " in file " + f.fileName + " has no type specified.");
               }

               Type typ = this.findNvType(nv.nvTScope, nv.nvTIndex);
               if (typ == null) {
                  throw new RuntimeException("Could not find nv type " + nv.nvTScope + "," + nv.nvTIndex);
               }

               return typ;
            }

            res = res.next;
         }
      }

      throw new RuntimeException("Could not find object type " + objType);
   }

   public String getCpName(int objType, int scope, int index) {
      CrossReference.Resource res = this.resources;

      while (res != null) {
         if (res.standard) {
            res = res.next;
         } else {
            FptFile f = res.fptFile;
            Fpt fpt = f.getFptByKey(objType);
            if (fpt != null) {
               Fpt.Cp cp = fpt.getMemberCp(scope, index);
               if (cp == null && fpt.inheritMark && this.standard != null) {
                  Fpt sfpt = this.standard.fptFile.getFptByKey(objType);
                  if (sfpt != null) {
                     cp = sfpt.getMemberCp(scope, index);
                  }
               }

               if (cp == null) {
                  throw new RuntimeException("No member cp with scope/index " + scope + "/" + index + " in " + fpt.key + " " + f.fileName);
               }

               return cp.cpName;
            }

            res = res.next;
         }
      }

      if (this.standard != null) {
         Fpt fpt = this.standard.fptFile.getFptByKey(objType);
         if (fpt != null) {
            Fpt.Cp cpx = fpt.getMemberCp(scope, index);
            if (cpx == null) {
               throw new RuntimeException("No member cp with scope/index " + scope + "/" + index + " in " + fpt.key + " " + this.standard.fileName);
            }

            return cpx.cpName;
         }
      }

      throw new RuntimeException("Could not find object type " + objType);
   }

   public String getResourceString(int scope, int index) {
      CrossReference.Resource res = this.getResouce(scope);
      LanguageFile lf = res.lanFile;
      if (lf == null) {
         throw new RuntimeException("No language file for scope " + scope);
      } else {
         LanguageFile.LangRec[] recs = lf.langRecs;
         if (index > recs.length) {
            throw new RuntimeException("No index " + scope + "/" + index + " found in " + lf.fileName);
         } else {
            String str = recs[index - 1].resStr;

            try {
               String ls = str.toLowerCase();
               BUnit.getUnit(ls);
               str = ls;
            } catch (Throwable var9) {
               Conversion sc = res.conversion;
               if (sc != null) {
                  str = sc.getConversionString(scope, index, str);
               }
            }

            return str;
         }
      }
   }

   public String getPrincipalNv(int objType) {
      CrossReference.Resource res = this.resources;

      while (res != null) {
         if (res.standard) {
            res = res.next;
         } else {
            boolean mfg = true;
            FptFile f = res.fptFile;
            Fpt fpt = f.getFptByKey(objType);
            if (fpt != null) {
               if (fpt.principalNV == 0 && fpt.inheritMark && this.standard != null) {
                  fpt = this.standard.fptFile.getFptByKey(objType);
                  mfg = false;
               }

               if (fpt.principalNV <= 0) {
                  throw new RuntimeException("No principalNV for objType " + objType);
               }

               return (mfg ? "#" : "|") + fpt.principalNV;
            }

            res = res.next;
         }
      }

      if (this.standard != null) {
         Fpt fpt = this.standard.fptFile.getFptByKey(objType);
         if (fpt.principalNV <= 0) {
            throw new RuntimeException("No principalNV for objType " + objType);
         } else {
            return "|" + fpt.principalNV;
         }
      } else {
         throw new RuntimeException("Could not find object type " + objType);
      }
   }

   public void setUnitPrompt(Conversion.UnitPrompt u) {
      this.unitPrompt = u;

      for (CrossReference.Resource res = this.resources; res != null; res = res.next) {
         if (res.conversion != null) {
            res.conversion.setUnitPrompt(u);
         }
      }
   }

   public Conversion getConversion(String fName) {
      String fileName = this.stripFilename(fName);
      CrossReference.Resource res = this.resources;

      while (res != null && !res.fileName.equals(fileName)) {
         res = res.next;
      }

      return res == null ? null : res.conversion;
   }

   public TypeFile[] getTypeFiles(boolean marked) {
      Array<TypeFile> a = new Array(TypeFile.class);

      for (CrossReference.Resource res = this.resources; res != null; res = res.next) {
         if (!res.standard && (!marked || this.marks[res.typFile.scope])) {
            a.add(res.typFile);
         }
      }

      return (TypeFile[])a.trim();
   }

   public void flush() {
      for (CrossReference.Resource res = this.resources; res != null; res = res.next) {
         if (res.conversion != null) {
            res.conversion.flush();
         }
      }
   }

   CrossReference.Resource getResouce(int scope) {
      CrossReference.Resource res = this.resources;

      while (res != null && res.typFile != null && res.typFile.scope != scope) {
         res = res.next;
      }

      if (res == null) {
         throw new RuntimeException("No resource file for scope " + scope);
      } else {
         return res;
      }
   }

   public LanguageFile getLanguageFile(int scope) {
      CrossReference.Resource res = this.getResouce(scope);
      LanguageFile lf = res.lanFile;
      if (lf == null) {
         throw new RuntimeException("No language file for scope " + scope);
      } else {
         return lf;
      }
   }

   public void mark(int scope) {
      if (scope >= 0 && scope < this.marks.length) {
         this.marks[scope] = true;
      }
   }

   public void clearMarks() {
      this.marks = new boolean[]{false, false, false, false, false, false, false};
   }

   public static class DuplicateResourceException extends Exception {
      final String msg;

      DuplicateResourceException(String reason) {
         this.msg = reason;
      }

      @Override
      public String toString() {
         return this.msg;
      }
   }

   static class Resource {
      String fileName;
      TypeFile typFile = null;
      FptFile fptFile = null;
      LanguageFile lanFile = null;
      Conversion conversion = null;
      CrossReference.Resource next = null;
      boolean standard = false;
   }
}
