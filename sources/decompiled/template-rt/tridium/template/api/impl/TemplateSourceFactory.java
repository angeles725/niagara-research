package com.tridium.template.api.impl;

import com.tridium.template.BTemplateConfig;
import com.tridium.template.UpgradeUtil;
import com.tridium.template.file.BNtplFile;
import java.io.InputStream;
import java.io.OutputStream;
import javax.baja.driver.BDevice;
import javax.baja.file.BFileSystem;
import javax.baja.file.BIDirectory;
import javax.baja.file.FilePath;
import javax.baja.nre.util.FileUtil;
import javax.baja.sys.BComponent;
import javax.baja.sys.BStation;
import javax.baja.sys.Sys;

public final class TemplateSourceFactory {
   private static final String STATION_TEMP_DIR = "^temp/";
   private static final String WB_TEMP_DIR = "~temp/";

   public static NewTemplateSource create(BComponent sourceComponent) {
      return create(sourceComponent, null, null, false);
   }

   public static NewTemplateSource create(BComponent sourceComponent, BIDirectory sourceHomeDir) {
      return create(sourceComponent, sourceHomeDir, null, false);
   }

   public static NewTemplateSource create(BStation sourceStation, boolean createAsApplication) {
      return create(sourceStation, null, null, createAsApplication);
   }

   public static NewTemplateSource create(BComponent sourceStation, BIDirectory sourceHomeDir, boolean createAsApplication) {
      return create(sourceStation, sourceHomeDir, null, createAsApplication);
   }

   public static NewTemplateSource create(
      BComponent sourceComponent, BIDirectory sourceHomeDir, BIDirectory sourceProtectedHomeDir, boolean createAsApplication
   ) {
      NewTemplateSource result = null;
      if (sourceComponent instanceof BStation) {
         if (createAsApplication) {
            result = new NewApplicationTemplateSource((BStation)sourceComponent, sourceHomeDir, sourceProtectedHomeDir);
         } else {
            result = new NewStationTemplateSource((BStation)sourceComponent, sourceHomeDir, sourceProtectedHomeDir);
         }
      } else if (sourceComponent instanceof BDevice) {
         result = new NewDeviceTemplateSource(sourceComponent, sourceHomeDir, sourceProtectedHomeDir);
      } else if (sourceComponent.getPropertyInParent() == null || sourceComponent.getPropertyInParent().isDynamic()) {
         result = new NewComponentTemplateSource(sourceComponent, sourceHomeDir, sourceProtectedHomeDir);
      }

      return result;
   }

   public static TemplateSource open(BComponent deployedTemplate) {
      DeployedTemplateSource templateSource = null;
      if (deployedTemplate instanceof BStation) {
         templateSource = InstalledApplicationTemplateSource.make((BStation)deployedTemplate);
      }

      TemplateSourceWithValue fileSource = templateSource == null ? null : loadFileSource(templateSource.getConfig());
      return (TemplateSource)(templateSource == null ? null : (fileSource == null ? templateSource : new CombinedTemplateSource(templateSource, fileSource)));
   }

   private static TemplateSourceWithValue loadFileSource(BTemplateConfig config) {
      TemplateSourceWithValue fileSource = null;

      try {
         BNtplFile tempFile = (BNtplFile)UpgradeUtil.getDeployedNtplFile(config, "^applicationTemplate/", "napl");
         FilePath temporaryFilePath;
         if (Sys.isStation()) {
            temporaryFilePath = new FilePath("^temp/" + tempFile.getFileName());
         } else {
            temporaryFilePath = new FilePath("~temp/" + tempFile.getFileName());
         }

         BNtplFile templateFile = (BNtplFile)BFileSystem.INSTANCE.makeFile(temporaryFilePath);

         try (
            InputStream in = tempFile.getInputStream();
            OutputStream out = templateFile.getOutputStream();
         ) {
            FileUtil.pipe(in, out);
         }

         fileSource = FileTemplateSource.load(templateFile);
      } catch (Exception var37) {
      }

      return fileSource;
   }

   private TemplateSourceFactory() {
   }
}
