package com.tridium.template.file;

import com.tridium.sys.module.BModuleFile;
import com.tridium.template.BTemplateConfig;
import com.tridium.template.TemplateConst;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.baja.file.BDirectory;
import javax.baja.file.BIFile;
import javax.baja.file.FilePath;
import javax.baja.file.zip.BZipSpace;
import javax.baja.naming.BOrd;
import javax.baja.naming.OrdQuery;
import javax.baja.nre.util.Array;
import javax.baja.space.BSpace;
import javax.baja.sys.BAbsTime;
import javax.baja.sys.BComponent;
import javax.baja.sys.Type;
import javax.baja.util.BUuid;

public class TemplateManager {
   private boolean isTemplateMapInitialized = false;
   protected BDirectory templateDir = null;
   BAbsTime tmplLastModified = BAbsTime.DEFAULT;
   protected BDirectory applicationDir = null;
   BAbsTime applLastModified = BAbsTime.DEFAULT;
   BDirectory modDir = null;
   BAbsTime modLastModified = BAbsTime.DEFAULT;
   Hashtable<FilePath, TemplateManager.TemplateInfo> tInfo = null;
   Hashtable<FilePath, TemplateManager.TemplateInfo> aInfo = null;
   Hashtable<FilePath, TemplateManager.TemplateInfo> mInfo = null;
   public static TemplateManager INSTANCE = new TemplateManager();
   public static final Logger log = Logger.getLogger("ntpl");
   private static final Pattern COMPILE = Pattern.compile("file:~templates/", 16);

   public TemplateManager.TemplateInfo getTemplate(String templateName, String vendor) {
      this.updateTemplateMap(this.getTemplateDir());
      TemplateManager.TemplateInfo thisInfo = this.findTemplate(this.tInfo, templateName, vendor);
      if (thisInfo != null) {
         return thisInfo;
      } else {
         this.updateMods();
         thisInfo = this.findTemplate(this.mInfo, templateName, vendor);
         if (thisInfo != null) {
            return thisInfo;
         } else {
            this.updateTemplateMap(this.getApplicationDir());
            thisInfo = this.findTemplate(this.aInfo, templateName, vendor);
            return thisInfo != null ? thisInfo : null;
         }
      }
   }

   public TemplateManager.TemplateInfo getTemplate(BNtplFile templateFile) {
      return templateFile == null ? null : new TemplateManager.TemplateInfo(templateFile, templateFile.getLastModified(), null);
   }

   public TemplateManager.TemplateInfo getTemplate(BUuid uID, String vendor) {
      this.updateTemplateMap(this.getTemplateDir());
      TemplateManager.TemplateInfo thisInfo = this.findTemplate(this.tInfo, uID, vendor);
      if (thisInfo != null) {
         return thisInfo;
      } else {
         this.updateMods();
         thisInfo = this.findTemplate(this.mInfo, uID, vendor);
         if (thisInfo != null) {
            return thisInfo;
         } else {
            this.updateTemplateMap(this.getApplicationDir());
            thisInfo = this.findTemplate(this.aInfo, uID, vendor);
            return thisInfo != null ? thisInfo : null;
         }
      }
   }

   public TemplateManager.TemplateInfo[] getTemplatesForType(Type typ) {
      Array<TemplateManager.TemplateInfo> a = new Array(TemplateManager.TemplateInfo.class);
      this.updateTemplateMap(this.getTemplateDir());
      if (this.tInfo != null) {
         this.addNtpls(typ, this.tInfo, a);
      }

      this.updateMods();
      if (this.mInfo != null) {
         this.addNtpls(typ, this.mInfo, a);
      }

      this.updateTemplateMap(this.getApplicationDir());
      if (this.aInfo != null) {
         this.addNtpls(typ, this.aInfo, a);
      }

      return (TemplateManager.TemplateInfo[])a.trim();
   }

   public void initTemplateMap() {
      this.updateTemplateMap(this.getTemplateDir());
      this.isTemplateMapInitialized = true;
   }

   private void updateTemplateMap(BDirectory templateDir) {
      if (!this.isTemplateMapInitialized) {
         try {
            this.updateTpls(templateDir);
         } catch (Exception var3) {
            log.log(Level.WARNING, "Cannot get template:" + var3.getLocalizedMessage(), (Throwable)var3);
         }
      }
   }

   private TemplateManager.TemplateInfo findTemplate(Hashtable<FilePath, TemplateManager.TemplateInfo> infoTable, BUuid uID, String vendor) {
      if (infoTable != null) {
         Enumeration<TemplateManager.TemplateInfo> templateInfos = infoTable.elements();

         while (templateInfos.hasMoreElements()) {
            TemplateManager.TemplateInfo thisInfo = templateInfos.nextElement();
            if (thisInfo.getUid().equals(uID) && thisInfo.getVendor().equals(vendor)) {
               return thisInfo;
            }
         }
      }

      return null;
   }

   private TemplateManager.TemplateInfo findTemplate(Hashtable<FilePath, TemplateManager.TemplateInfo> infoTable, String templateName, String vendor) {
      if (infoTable != null) {
         Enumeration<TemplateManager.TemplateInfo> templateInfos = infoTable.elements();

         while (templateInfos.hasMoreElements()) {
            TemplateManager.TemplateInfo thisInfo = templateInfos.nextElement();
            if (thisInfo.getName().equals(templateName) && thisInfo.getVendor().equals(vendor)) {
               return thisInfo;
            }
         }
      }

      return null;
   }

   private void addNtpls(Type typ, Hashtable<FilePath, TemplateManager.TemplateInfo> ht, Array<TemplateManager.TemplateInfo> a) {
      Iterator<FilePath> it = ht.keySet().iterator();

      while (it.hasNext()) {
         TemplateManager.TemplateInfo ti = ht.get(it.next());
         if (ti.rootType.is(typ)) {
            a.add(ti);
         }
      }
   }

   private void updateTpls(BDirectory tmplDir) {
      if (tmplDir == null) {
         this.tInfo = null;
      } else {
         BAbsTime tLstMod = tmplDir.getLastModified();
         BIFile[] templates = NtplUtil.getTemplates(tmplDir);
         Hashtable<FilePath, TemplateManager.TemplateInfo> ht = new Hashtable<>();
         if (this.getApplicationDir().equals(tmplDir)) {
            this.addNtpls(this.aInfo, ht, templates);
            this.aInfo = ht;
            this.applLastModified = tLstMod;
         } else {
            this.addNtpls(this.tInfo, ht, templates);
            this.tInfo = ht;
            this.tmplLastModified = tLstMod;
         }
      }
   }

   private void addNtpls(Hashtable<FilePath, TemplateManager.TemplateInfo> origHt, Hashtable<FilePath, TemplateManager.TemplateInfo> ht, BIFile[] files) {
      Set<String> duplicatesTemplates = findDuplicatesTemplates(Arrays.asList(files));

      for (int i = 0; i < files.length; i++) {
         try {
            if (files[i] instanceof BNtplFile) {
               BNtplFile f = (BNtplFile)files[i];
               TemplateManager.TemplateInfo ti = null;
               FilePath fp = f.getFilePath();
               if (origHt != null) {
                  ti = origHt.get(fp);
               }

               BAbsTime lstMod = f.getLastModified();
               if (ti == null || ti.lastModified == null || ti.lastModified.isBefore(lstMod)) {
                  try {
                     ti = new TemplateManager.TemplateInfo(f, lstMod, duplicatesTemplates);
                  } catch (Exception var14) {
                     log.log(Level.WARNING, "Cannot load template " + f.getFileName() + ":" + var14.getLocalizedMessage(), (Throwable)var14);
                     if (f.isOpen()) {
                        try {
                           f.close();
                        } catch (Exception var13) {
                        }
                     }
                     continue;
                  }
               }

               ht.put(fp, ti);
               if (f.isOpen()) {
                  try {
                     f.close();
                  } catch (Exception var12) {
                  }
               }
            }
         } catch (Exception var15) {
            log.log(Level.WARNING, "Error adding NTPL files:" + var15.getLocalizedMessage(), (Throwable)var15);
         }
      }
   }

   public static Set<String> findDuplicatesTemplates(List<BIFile> listContainingDuplicates) {
      Set<String> duplicateTemplate = new HashSet<>();
      Set<String> templates = new HashSet<>();

      for (BIFile file : listContainingDuplicates) {
         if (file instanceof BNtplFile) {
            String title = ((BNtplFile)file).getTitle();
            if (!templates.add(title)) {
               duplicateTemplate.add(title);
            }
         }
      }

      return duplicateTemplate;
   }

   public void modsChanged() {
      this.modLastModified = BAbsTime.DEFAULT;
   }

   private void updateMods() {
      BDirectory mdDir = this.getModuleDir();
      if (mdDir != null) {
         BAbsTime tLstMod = mdDir.getLastModified();
         if (tLstMod.isAfter(this.modLastModified)) {
            Hashtable<FilePath, TemplateManager.TemplateInfo> ht = new Hashtable<>();
            BIFile[] mods = mdDir.listFiles();

            for (BIFile mod : mods) {
               if (mod instanceof BModuleFile) {
                  BModuleFile mf = (BModuleFile)mod;

                  try {
                     BSpace space = mf.open();

                     try {
                        BIFile[] fa = ((BZipSpace)space).listFiles();
                        this.addNtpls(this.mInfo, ht, fa);
                     } finally {
                        mf.close();
                     }
                  } catch (Exception var16) {
                     log.log(Level.WARNING, "Error loading templates in module " + mf.getFileName(), (Throwable)var16);
                  }
               }
            }

            this.mInfo = ht;
            this.modLastModified = tLstMod;
         }
      }
   }

   protected BDirectory getTemplateDir() {
      if (this.templateDir == null) {
         this.templateDir = AccessController.doPrivileged((PrivilegedAction<BDirectory>)(() -> NtplUtil.getTemplateDirectory()));
      }

      return this.templateDir;
   }

   private BDirectory getModuleDir() {
      if (this.modDir == null) {
         BOrd modOrd = BOrd.make(TemplateConst.TEMPLATE_MODULE_DIR);

         try {
            this.modDir = (BDirectory)modOrd.resolve().get();
         } catch (Exception var3) {
            this.modDir = null;
         }
      }

      return this.modDir;
   }

   protected BDirectory getApplicationDir() {
      if (this.applicationDir == null) {
         this.applicationDir = AccessController.doPrivileged((PrivilegedAction<BDirectory>)(() -> NtplUtil.getApplicationDirectory()));
      }

      return this.applicationDir;
   }

   public static class TemplateInfo implements Comparable<TemplateManager.TemplateInfo> {
      FilePath fp;
      BOrd ntpFileOrd;
      BAbsTime lastModified;
      BAbsTime versionDate;
      Type rootType;
      String name;
      String vendor;
      String version;
      BUuid uID;
      String description;

      public TemplateInfo(BNtplFile file, BAbsTime lastModified, Set<String> duplicatesTemplates) {
         BComponent base = file.getBaseComponent();
         BTemplateConfig config = BTemplateConfig.getConfigForRoot(base);
         this.setFilePath(file.getFilePath());
         this.setNtpFileOrd(file.getOrdInHost());
         this.setLastModified(config != null && !BAbsTime.NULL.equals(config.getVersionDate()) ? config.getVersionDate() : lastModified);
         this.setRootType(base.getType());
         this.setVendor(file.getVendor());
         this.setVersion(file.getVersion());
         this.setDescription(file.getDescription());
         this.setUID(file.getUID());
         String pickName = file.getTitle();
         OrdQuery[] a = file.getOrdInSpace().parse();
         if (a.length > 1) {
            OrdQuery q = a[a.length - 2];
            if (q instanceof FilePath) {
               String[] names = ((FilePath)q).getNames();
               if (names.length > 1 && names[0].equals("modules")) {
                  String modNm = names[1];
                  pickName = modNm.substring(0, modNm.indexOf(".")) + "/" + pickName;
               }
            }
         }

         if (duplicatesTemplates != null && duplicatesTemplates.contains(file.getTitle())) {
            String filePath = file.getFilePath().toString();
            this.setName(
               file.getTitle()
                  + " ("
                  + TemplateManager.COMPILE.matcher(filePath.substring(0, filePath.lastIndexOf(46))).replaceAll(Matcher.quoteReplacement(""))
                  + ")"
            );
         } else {
            this.setName(pickName);
         }
      }

      public void setFilePath(FilePath fp) {
         this.fp = fp;
      }

      public void setNtpFileOrd(BOrd ntpFileOrd) {
         this.ntpFileOrd = ntpFileOrd;
      }

      public void setLastModified(BAbsTime lastModified) {
         this.lastModified = lastModified;
      }

      public void setVersionDate(BAbsTime versionDate) {
         this.versionDate = versionDate;
      }

      public void setRootType(Type rootType) {
         this.rootType = rootType;
      }

      public void setVendor(String vendor) {
         this.vendor = vendor;
      }

      public void setVersion(String version) {
         this.version = version;
      }

      public void setDescription(String description) {
         this.description = description;
      }

      public void setName(String name) {
         this.name = name;
      }

      public void setUID(BUuid uID) {
         this.uID = uID;
      }

      public FilePath getFilePath() {
         return this.fp;
      }

      public BOrd getNtpFileOrd() {
         return this.ntpFileOrd;
      }

      public BAbsTime getLastModified() {
         return this.lastModified;
      }

      public BAbsTime getVersionDate() {
         return this.versionDate;
      }

      public Type getRootType() {
         return this.rootType;
      }

      public String getName() {
         return this.name;
      }

      public String getVendor() {
         return this.vendor;
      }

      public String getVersion() {
         return this.version;
      }

      public String getDescription() {
         return this.description;
      }

      public BUuid getUid() {
         return this.uID;
      }

      public BNtplFile getNtplFile() {
         return (BNtplFile)this.getNtpFileOrd().resolve().get();
      }

      public int compareTo(TemplateManager.TemplateInfo other) {
         if (this != other && !this.getFilePath().equals(other.getFilePath())) {
            int result = this.getName().compareToIgnoreCase(other.getName());
            if (result != 0) {
               return result;
            } else {
               result = this.getName().compareTo(other.getName());
               return result != 0 ? result : this.getFilePath().compareTo(other.getFilePath());
            }
         } else {
            return 0;
         }
      }
   }
}
