package com.tridium.program.ui;

import com.tridium.crypto.core.cert.NX509Certificate;
import com.tridium.crypto.core.cert.SigningUtil;
import com.tridium.crypto.core.io.ICoreTrustStore;
import com.tridium.platcrypto.fox.ChannelCryptoManager;
import com.tridium.program.BCode;
import com.tridium.program.BProgram;
import com.tridium.program.module.BProgramModule;
import com.tridium.program.ui.signing.BCertificateNotTrustedDialog;
import com.tridium.sys.Nre;
import com.tridium.workbench.fieldeditors.BTypeSpecFE;
import com.tridium.workbench.shell.BNiagaraWbShell;
import java.io.File;
import java.io.IOException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.baja.nre.util.SortUtil;
import javax.baja.nre.util.TextUtil;
import javax.baja.registry.ModuleInfo;
import javax.baja.sys.BBoolean;
import javax.baja.sys.BComplex;
import javax.baja.sys.BFacets;
import javax.baja.sys.Sys;
import javax.baja.ui.BDialog;
import javax.baja.ui.BLabel;
import javax.baja.ui.BListDropDown;
import javax.baja.ui.BMenu;
import javax.baja.ui.BSeparator;
import javax.baja.ui.BTextField;
import javax.baja.ui.BToolBar;
import javax.baja.ui.Command;
import javax.baja.ui.CommandArtifact;
import javax.baja.ui.enums.BHalign;
import javax.baja.ui.pane.BGridPane;
import javax.baja.ui.util.UiLexicon;
import javax.baja.util.BTypeSpec;
import javax.baja.util.Lexicon;

public class ProgramEditorCommands {
   static UiLexicon lex = UiLexicon.bajaui();
   static Lexicon progLex = Lexicon.make("program");
   public final BProgramEditor editor;
   public Command importType;
   public Command importPackage;
   public Command removeImport;
   public Command saveAndCompile;
   public Command compile;
   public Command build;
   public Command installCertificate;
   static String[] BIN_EXT_MODULE_NAMES;

   public ProgramEditorCommands(BProgramEditor editor) {
      this.editor = editor;
      this.importType = new ProgramEditorCommands.ImportTypeCommand();
      this.importPackage = new ProgramEditorCommands.ImportPackageCommand();
      this.removeImport = new ProgramEditorCommands.RemoveImportCommand();
      this.saveAndCompile = new ProgramEditorCommands.SaveAndCompileCommand();
      this.compile = new ProgramEditorCommands.CompileCommand();
      this.build = new ProgramEditorCommands.BuildCommand();
      this.installCertificate = new ProgramEditorCommands.InstallCertificateCommand();
   }

   public BMenu[] getViewMenus() {
      BMenu menu = UiLexicon.bajaui().buildMenu("programEditor.menu.label");
      menu.add("importType", this.importType);
      menu.add("importPackage", this.importPackage);
      menu.add("importRemove", this.removeImport);
      menu.add("sep0", new BSeparator());
      menu.add("addSlot", this.editor.slotSheet.commands.add);
      menu.add("deleteSlot", this.editor.slotSheet.commands.delete);
      menu.add("renameSlot", this.editor.slotSheet.commands.rename);
      menu.add("reorderSlots", this.editor.slotSheet.commands.reorder);
      menu.add("sep1", new BSeparator());
      menu.add("saveAndCompile", this.saveAndCompile);
      menu.add("compile", this.compile);
      menu.add("installCertificate", this.installCertificate);
      return new BMenu[]{menu};
   }

   public BToolBar getViewToolBar() {
      BNiagaraWbShell shell = (BNiagaraWbShell)this.editor.getShell();
      BToolBar bar = new BToolBar();
      if (shell != null) {
         bar.add("find", shell.commands.find);
         bar.add("replace", shell.commands.replace);
      }

      bar.add("sep2", new BSeparator());
      BProgramModule module = null;

      for (BComplex p = this.editor.program.getParent(); p != null; p = p.getParent()) {
         if (p instanceof BProgramModule) {
            module = (BProgramModule)p;
            break;
         }
      }

      if (module != null) {
         bar.add("build", this.build);
      }

      bar.add("saveAndCompile", this.saveAndCompile);
      bar.add("compile", this.compile);
      bar.add("installCertificate", this.installCertificate);
      if (shell != null) {
         bar.add("consolePrev", shell.commands.consolePrev);
         bar.add("consoleNext", shell.commands.consoleNext);
      }

      return bar;
   }

   static {
      File binDir = new File(Nre.getNiagaraHome(), "bin");
      File binExtDir = new File(binDir, "ext");
      BIN_EXT_MODULE_NAMES = binExtDir.list(new ProgramEditorCommands.BinExtModuleFilter());
   }

   public static class BinExtModuleFilter extends Compiler.JarFilenameFilter {
      @Override
      public boolean accept(File dir, String name) {
         if (!super.accept(dir, name)) {
            return false;
         } else {
            File file = new File(dir, name);
            if (!file.exists()) {
               return false;
            } else {
               try (ZipFile zip = new ZipFile(file)) {
                  ZipEntry entry = zip.getEntry("META-INF/module.xml");
                  if (entry == null) {
                     entry = zip.getEntry("meta-inf/module.xml");
                  }

                  return entry != null;
               } catch (IOException var19) {
                  return false;
               }
            }
         }
      }
   }

   public class BuildCommand extends Command {
      public BuildCommand() {
         super(ProgramEditorCommands.this.editor, Lexicon.make(BProgramEditor.class), "programEditor.build");
      }

      public CommandArtifact doInvoke() throws Exception {
         ProgramEditorCommands.this.editor.compiler.compile(BCode.generateClassName(), true, true);
         return null;
      }
   }

   public class CompileCommand extends ProgramEditorCommands.ProgramEditorCommand {
      public CompileCommand() {
         super("programEditor.compile");
      }

      @Override
      public CommandArtifact doInvoke() throws Exception {
         ProgramEditorCommands.this.editor.compiler.compile(BCode.generateClassName(), false);
         return null;
      }
   }

   public class ImportPackageCommand extends ProgramEditorCommands.ProgramEditorCommand {
      public ImportPackageCommand() {
         super("programEditor.importPackage");
      }

      @Override
      public CommandArtifact doInvoke() throws Exception {
         BListDropDown moduleBox = new BListDropDown();
         ModuleInfo[] modulesList = Sys.getRegistry().getModules();
         String[] moduleNames = new String[modulesList.length + ProgramEditorCommands.BIN_EXT_MODULE_NAMES.length];

         for (int i = 0; i < modulesList.length; i++) {
            moduleNames[i] = modulesList[i].getModulePartName();
         }

         for (int i = 0; i < ProgramEditorCommands.BIN_EXT_MODULE_NAMES.length; i++) {
            moduleNames[modulesList.length + i] = ProgramEditorCommands.BIN_EXT_MODULE_NAMES[i]
               .substring(0, ProgramEditorCommands.BIN_EXT_MODULE_NAMES[i].length() - 4);
         }

         SortUtil.sort(moduleNames);
         moduleBox.getList().addItem("java");

         for (String moduleName : moduleNames) {
            moduleBox.getList().addItem(moduleName);
         }

         moduleBox.setSelectedItem("baja-rt");
         BTextField packageBox = new BTextField("", 25);
         BGridPane pane = new BGridPane(2);
         pane.setColumnAlign(BHalign.fill);
         pane.add(null, new BLabel(ProgramEditorCommands.lex.getText("programEditor.module")));
         pane.add(null, moduleBox);
         pane.add(null, new BLabel(ProgramEditorCommands.lex.getText("programEditor.package")));
         pane.add(null, packageBox);
         int r = BDialog.open(ProgramEditorCommands.this.editor, this.getLabel(), pane, 3, BDialog.QUESTION_ICON);
         if (r == 2) {
            return null;
         } else {
            String module = (String)moduleBox.getSelectedItem();
            String pack = packageBox.getText();
            if (pack.equals("")) {
               return null;
            } else {
               ProgramEditorCommands.this.editor.imports.add(new Imports.Import(module, pack, 1));
               return null;
            }
         }
      }
   }

   public class ImportTypeCommand extends ProgramEditorCommands.ProgramEditorCommand {
      public ImportTypeCommand() {
         super("programEditor.importType");
      }

      @Override
      public CommandArtifact doInvoke() throws Exception {
         BTypeSpecFE plugin = new BTypeSpecFE();
         plugin.loadValue(
            BTypeSpec.make("baja", "String"),
            BFacets.make(
               BFacets.make(BFacets.make("allowNull", BBoolean.FALSE), BFacets.make("showAbstract", BBoolean.TRUE)),
               BFacets.make("showInterface", BBoolean.TRUE)
            )
         );
         int r = BDialog.open(ProgramEditorCommands.this.editor, this.getLabel(), plugin, 3, BDialog.QUESTION_ICON);
         if (r == 2) {
            return null;
         } else {
            BTypeSpec spec = (BTypeSpec)plugin.saveValue();
            if (spec.getTypeName().equals("")) {
               return null;
            } else {
               String className = spec.getResolvedType().getTypeClass().getName();
               String pack = TextUtil.getPackageName(className);
               ProgramEditorCommands.this.editor
                  .imports
                  .add(new Imports.Import(spec.getResolvedType().getModule().getModulePartName(spec.getResolvedType().getRuntimeProfile()), pack, 1));
               return null;
            }
         }
      }
   }

   public class InstallCertificateCommand extends ProgramEditorCommands.ProgramEditorCommand {
      public InstallCertificateCommand() {
         super("programEditor.installCertificate");
      }

      @Override
      public CommandArtifact doInvoke() {
         BProgram program = ProgramEditorCommands.this.editor.program;
         byte[] signature = program.getCode().getSignature().copyBytes();
         byte[] classBytes = program.getCode().getClassFile().copyBytes();

         try {
            Certificate cert = SigningUtil.getRootCertificate(classBytes, signature);
            if (!(cert instanceof X509Certificate)) {
               BDialog.error(ProgramEditorCommands.this.editor, ProgramEditorCommands.progLex.getText("program.notX509"));
               return null;
            }

            NX509Certificate ncert = NX509Certificate.make((X509Certificate)cert);
            ChannelCryptoManager ccm = new ChannelCryptoManager(program);
            ICoreTrustStore userTrustStore = ccm.getUserTrustStore();
            ICoreTrustStore systemTrustStore = ccm.getSystemTrustStore();
            if (userTrustStore.getCertificateAlias(ncert.getCertificate()) != null || systemTrustStore.getCertificateAlias(ncert.getCertificate()) != null) {
               BDialog.info(ProgramEditorCommands.this.editor, ProgramEditorCommands.progLex.getText("program.certAlreadyTrusted"));
               return null;
            }

            BCertificateNotTrustedDialog.installUntrustedCertificate(ncert, ProgramEditorCommands.this.editor, program);
         } catch (Exception var9) {
            BDialog.error(ProgramEditorCommands.this.editor, BDialog.TITLE_ERROR, ProgramEditorCommands.progLex.getText("program.couldNotInstallCert"), var9);
         }

         return null;
      }
   }

   public class ProgramEditorCommand extends Command {
      public ProgramEditorCommand(String keyBase) {
         super(ProgramEditorCommands.this.editor, UiLexicon.bajaui().module, keyBase);
      }

      public CommandArtifact doInvoke() throws Exception {
         BDialog.message(ProgramEditorCommands.this.editor, "Incomplete: " + this.getLabel());
         return null;
      }
   }

   public class RemoveImportCommand extends ProgramEditorCommands.ProgramEditorCommand {
      public RemoveImportCommand() {
         super("programEditor.removeImport");
      }

      @Override
      public CommandArtifact doInvoke() throws Exception {
         ProgramEditorCommands.this.editor.imports.removeSelection();
         return null;
      }
   }

   public class SaveAndCompileCommand extends ProgramEditorCommands.ProgramEditorCommand {
      public SaveAndCompileCommand() {
         super("programEditor.saveAndCompile");
      }

      @Override
      public CommandArtifact doInvoke() throws Exception {
         ProgramEditorCommands.this.editor.compiler.compile(BCode.generateClassName(), true);
         return null;
      }
   }
}
