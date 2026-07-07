package com.tridium.template.file;

import com.tridium.file.util.FindUtil;
import java.util.ArrayList;
import java.util.List;
import javax.baja.file.BDirectory;
import javax.baja.file.BIFile;
import javax.baja.util.BUuid;

public final class TemplateFileUtil {
   private TemplateFileUtil() {
   }

   public static List<BIFile> getApplicationTemplates() {
      return getTemplates(NtplUtil.getApplicationDirectory());
   }

   public static List<BIFile> getTemplates() {
      return getTemplates(NtplUtil.getTemplateDirectory());
   }

   public static List<BIFile> getTemplates(BDirectory directory) {
      return FindUtil.files().ofType(BNtplFile.TYPE).from(directory);
   }

   public static List<BINtplFile> getApplicationTemplatesByUuid(BUuid uuid) {
      return getTemplatesByUuid(uuid, NtplUtil.getApplicationDirectory());
   }

   public static List<BINtplFile> getTemplatesByUuid(BUuid uuid) {
      return getTemplatesByUuid(uuid, NtplUtil.getTemplateDirectory());
   }

   public static List<BINtplFile> getTemplatesByUuid(BUuid uuid, BDirectory directory) {
      List<BIFile> templateFiles = getTemplates(directory);
      List<BINtplFile> matchingFiles = new ArrayList<>();

      for (BIFile file : templateFiles) {
         BINtplFile tempFile = (BINtplFile)file;
         if (tempFile.getTemplateManifest().uID.equals(uuid)) {
            matchingFiles.add(tempFile);
         }
      }

      return matchingFiles;
   }
}
