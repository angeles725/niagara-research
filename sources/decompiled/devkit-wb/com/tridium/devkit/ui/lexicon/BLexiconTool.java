package com.tridium.devkit.ui.lexicon;

import java.io.File;
import java.util.HashSet;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BIcon;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.ui.util.UiLexicon;
import javax.baja.workbench.tool.BWbNavNodeTool;

@NiagaraType
public class BLexiconTool extends BWbNavNodeTool {
   public static final Type TYPE = Sys.loadType(BLexiconTool.class);
   public String lexiconName;
   public String moduleName;
   public String path;
   public String key;
   public String buildModuleName;
   public String buildModuleSymbol;
   public String buildModuleDescription;
   public String buildModuleVendor;
   public String buildModuleVersion;
   public String buildModuleBrand;
   public boolean buildModuleDeleteLexiconFiles;
   public int[] buildModuleSelectedRows;
   public File selectedSourceModuleFile;
   public String migrateModuleDestination;
   public String migrateModuleVersion;
   HashSet<File> selectedFiles = new HashSet<>();
   public File migrateModuleSourceFolder;
   static UiLexicon lexicon = UiLexicon.makeUiLexicon(BLexiconEditor.class);
   private static final BIcon icon = BIcon.std("text.png");

   public Type getType() {
      return TYPE;
   }

   public BIcon getIcon() {
      return icon;
   }

   static {
      BLexiconEditorOptions.getDefault();
   }
}
