package com.tridium.template.api.impl;

import com.tridium.template.api.TemplateScope;
import com.tridium.template.manifest.TemplateFileSpec;
import java.util.SortedSet;
import java.util.TreeSet;
import javax.baja.file.BDataFile;
import javax.baja.file.BDirectory;
import javax.baja.file.BIDirectory;
import javax.baja.file.types.log.BILogFile;
import javax.baja.nav.BINavNode;
import javax.baja.sys.BBoolean;
import javax.baja.sys.BComponent;
import javax.baja.sys.BStation;

public abstract class NewStationWideTemplateSource extends NewTemplateSource {
   private static BComponent transferStrategyParams = null;
   private static final String TEMPLATE_SHARED_PATH = "shared";
   private static final String TEMPLATE_ACE_PATH = "ace";

   public NewStationWideTemplateSource(BStation sourceStation, BIDirectory sourceHomeDir, BIDirectory sourceProtectedHomeDir) {
      super(sourceStation, sourceHomeDir, sourceProtectedHomeDir);
   }

   @Override
   public TemplateScope getTemplateScope() {
      return TemplateScope.STATION_WIDE;
   }

   @Override
   protected BComponent getTransferStrategyParams() {
      if (transferStrategyParams == null) {
         transferStrategyParams = new BComponent();
         transferStrategyParams.add("exactConfig", BBoolean.TRUE);
      }

      return transferStrategyParams;
   }

   @Override
   protected SortedSet<TemplateFileSpec> getFilesToStore() {
      SortedSet<TemplateFileSpec> files = super.getFilesToStore();
      boolean createdFilesHere = false;
      BIDirectory sourceHomeDir = this.getSourceHomeDir();
      if (sourceHomeDir != null) {
         files = new TreeSet<>(files);
         createdFilesHere = true;
         this.addStationFiles(files, this.getSourceHomeDir(), "shared/", "file:^");
      }

      BIDirectory sourceProtectedHomeDir = this.getSourceProtectedHomeDir();
      if (sourceProtectedHomeDir != null) {
         BINavNode ace = sourceProtectedHomeDir.getNavChild("ace");
         if (ace instanceof BIDirectory) {
            if (!createdFilesHere) {
               files = new TreeSet<>(files);
            }

            this.addStationFiles(files, (BIDirectory)ace, "ace/", "file:^^ace/");
         }
      }

      return files;
   }

   protected boolean canStoreFile(BDataFile file) {
      return file instanceof BILogFile
         ? false
         : !file.getFileName().endsWith(".ntpl") || !((BDirectory)file.getNavParent()).getFileName().equals("stationTemplates");
   }

   private void addStationFiles(SortedSet<TemplateFileSpec> files, BIDirectory directoryToAdd, String baseName, String baseOrd) {
      this.addStationFiles(files, directoryToAdd, baseName, baseOrd, 15);
   }

   private void addStationFiles(SortedSet<TemplateFileSpec> files, BIDirectory directoryToAdd, String baseName, String baseOrd, int depth) {
      for (BINavNode child : directoryToAdd.getNavChildren()) {
         String name = child.getNavName();
         if (name != null && !name.isEmpty()) {
            if (child instanceof BIDirectory && depth > 0) {
               this.addStationFiles(files, (BIDirectory)child, baseName + name + "/", baseOrd + name + "/", depth - 1);
            } else if (child instanceof BDataFile && this.canStoreFile((BDataFile)child)) {
               files.add(new TemplateFileSpec(baseName + name, "data", baseOrd + name, (BDataFile)child));
            }
         }
      }
   }
}
