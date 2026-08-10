package com.tridium.devkit.wizards;

import com.tridium.gradle.plugins.templates.GradleProjectGenerator;
import com.tridium.gradle.plugins.templates.ModuleDependency;
import com.tridium.gradle.plugins.templates.ModuleRuntimeProfile;
import com.tridium.gradle.plugins.templates.NiagaraModuleDependencies;
import com.tridium.gradle.plugins.templates.NiagaraModuleGenerator;
import com.tridium.gradle.plugins.templates.NiagaraModulePartGenerator;
import com.tridium.gradle.plugins.templates.VelocityGenerator;
import com.tridium.nre.util.tuple.Pair;
import com.tridium.sys.Nre;
import com.tridium.ui.BOptionDialog;
import com.tridium.util.MapUtil;
import com.tridium.workbench.shell.BFontSize;
import com.tridium.workbench.shell.BGeneralOptions;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.baja.file.BFileSystem;
import javax.baja.file.BIFile;
import javax.baja.file.FilePath;
import javax.baja.gx.BImage;
import javax.baja.gx.BInsets;
import javax.baja.gx.Size;
import javax.baja.naming.BOrd;
import javax.baja.naming.OrdQuery;
import javax.baja.naming.SlotPath;
import javax.baja.naming.UnresolvedException;
import javax.baja.nre.platform.RuntimeProfile;
import javax.baja.registry.DependencyInfo;
import javax.baja.registry.ModuleInfo;
import javax.baja.registry.TypeInfo;
import javax.baja.sys.BModule;
import javax.baja.sys.BajaRuntimeException;
import javax.baja.sys.Sys;
import javax.baja.ui.BBorder;
import javax.baja.ui.BButton;
import javax.baja.ui.BCheckBox;
import javax.baja.ui.BDialog;
import javax.baja.ui.BLabel;
import javax.baja.ui.BRadioButton;
import javax.baja.ui.BTextField;
import javax.baja.ui.BWidget;
import javax.baja.ui.Command;
import javax.baja.ui.CommandArtifact;
import javax.baja.ui.ToggleCommand;
import javax.baja.ui.ToggleCommandGroup;
import javax.baja.ui.enums.BButtonStyle;
import javax.baja.ui.enums.BHalign;
import javax.baja.ui.enums.BValign;
import javax.baja.ui.event.BKeyEvent;
import javax.baja.ui.event.BMouseEvent;
import javax.baja.ui.file.BDirectoryChooser;
import javax.baja.ui.pane.BBorderPane;
import javax.baja.ui.pane.BConstrainedPane;
import javax.baja.ui.pane.BEdgePane;
import javax.baja.ui.pane.BGridPane;
import javax.baja.ui.table.BTable;
import javax.baja.ui.table.DefaultTableModel;
import javax.baja.ui.table.TableController;
import javax.baja.ui.table.TableModel;
import javax.baja.ui.text.TextModel;
import javax.baja.ui.util.UiLexicon;
import javax.baja.ui.wizard.BWizard;
import javax.baja.ui.wizard.BWizardHeader;
import javax.baja.ui.wizard.WizardModel;
import javax.baja.util.Lexicon;
import javax.baja.util.Version;
import javax.baja.workbench.BWbShell;
import org.apache.velocity.VelocityContext;

public class NewModuleWizard extends WizardModel {
   static BImage openIcon = BImage.make("module://icons/x16/open.png");
   static BDirectoryChooser chooser;
   static BImage moduleIcon = BImage.make("module://icons/x16/module.png");
   boolean step2initialized = false;
   String basePackage = "";
   public static UiLexicon lex = UiLexicon.makeUiLexicon(NewModuleWizard.class);
   public static BImage banner = BImage.make("module://icons/x32/newModule.png");
   public static final String lexUsePalette = lex.getText("newModule.usePalette");
   public static final String lexModuleDir = lex.getText("newModule.moduleDir");
   public static final String lexModuleName = lex.getText("newModule.moduleName");
   public static final String lexPrefSymbol = lex.getText("newModule.prefSymbol");
   public static final String lexVersion = lex.getText("newModule.vendorVersion");
   public static final String lexDescription = lex.getText("newModule.description");
   public static final String lexVendor = lex.getText("newModule.vendor");
   public static final String lexRuntimeProfiles = lex.getText("newModule.runtimeProfiles");
   private static String copyrightStatementFormat = "Copyright %d %s, All Rights Reserved.";
   BWbShell shell;
   int currentStep = 0;
   BWidget step0;
   BWidget step1;
   BWidget step2;
   BTextField moduleDir;
   BTextField moduleName;
   BTextField prefSymbol;
   BTextField version;
   BTextField description;
   BTextField vendor;
   BWidget rtpFE;
   Set<RuntimeProfile> enabledRuntimeProfiles = new HashSet<>();
   BCheckBox usePalette;
   private Map<RuntimeProfile, BCheckBox> checkBoxes = new TreeMap<>();
   BTable dependencies;
   BCheckBox useVendorVersion;
   BTable packages;
   private static final Logger log = Logger.getLogger("wbutil.modulewizard");
   public static final String DEFAULT_MODULE_VERSION = "1.0";

   public static void kickIt(BWbShell shell) {
      NewModuleWizard model = new NewModuleWizard(shell);
      BWizard.open(shell, model);
   }

   public NewModuleWizard(BWbShell shell) {
      this.shell = shell;
      this.enabledRuntimeProfiles.clear();
      this.enabledRuntimeProfiles.add(RuntimeProfile.rt);
      this.buildStep0();
      this.buildStep1();
      this.buildStep2();
   }

   private void buildStep0() {
      this.moduleDir = new BTextField(BFileSystem.INSTANCE.localFileToPath(Sys.getNiagaraUserHome()).getBody(), 60);
      this.moduleName = new BTextField("", 20);
      this.prefSymbol = new BTextField("", 20);
      this.version = new BTextField("1.0", 10);
      this.description = new BTextField("", 50);
      this.vendor = new BTextField("", 50);
      this.rtpFE = this.buildRtpFE();
      this.usePalette = new BCheckBox(lexUsePalette);
      this.moduleName.setModel(new NewModuleWizard.CanNextModel());
      this.prefSymbol.setModel(new NewModuleWizard.CanNextModel());
      this.vendor.setModel(new NewModuleWizard.CanNextModel());
      BButton button = new BButton(new NewModuleWizard.Browse(this.shell, this.moduleDir, this.vendor, this.version));
      button.setButtonStyle(BButtonStyle.toolBar);
      BGridPane dir = new BGridPane(2);
      dir.add(null, this.moduleDir);
      dir.add(null, button);
      BGridPane a = new BGridPane(1);
      a.add(null, new BLabel(lexModuleDir));
      a.add(null, dir);
      BGridPane b = new BGridPane(2);
      b.add(null, new BLabel(lexModuleName));
      b.add(null, this.moduleName);
      b.add(null, new BLabel(lexPrefSymbol));
      b.add(null, this.prefSymbol);
      b.add(null, new BLabel(lexVersion));
      b.add(null, this.version);
      b.add(null, new BLabel(lexDescription));
      b.add(null, this.description);
      b.add(null, new BLabel(lexVendor));
      b.add(null, this.vendor);
      BGridPane c = new BGridPane(1);
      c.add(null, new BLabel(lexRuntimeProfiles));
      c.add(null, this.rtpFE);
      c.add(null, this.usePalette);
      BGridPane d = new BGridPane(1);
      d.setRowGap(10.0);
      d.add(null, a);
      d.add(null, b);
      d.add(null, c);
      BEdgePane edge = new BEdgePane();
      edge.setTop(new BWizardHeader(banner, this.getTitle(), UiLexicon.bajaui().getText("wizard.step1") + " of 3"));
      edge.setCenter(new BBorderPane(d, 10.0, 10.0, 10.0, 10.0));
      this.step0 = edge;
   }

   public static Pair<String, String> getVendorAndVersionFromBuildGradle(FilePath rootPath) {
      String vendor = null;
      String version = null;
      BFileSystem fs = BFileSystem.INSTANCE;
      FilePath buildGradlePath = rootPath.merge("build.gradle.kts");

      try {
         BIFile buildGradleFile = fs.findFile(buildGradlePath);
         if (buildGradleFile != null) {
            try (LineNumberReader isr = new LineNumberReader(new InputStreamReader(buildGradleFile.getInputStream()))) {
               for (String str = isr.readLine(); str != null; str = isr.readLine()) {
                  if (str.trim().startsWith("defaultVendor")) {
                     String[] vendorParts = str.split("\"");
                     if (vendorParts.length >= 2) {
                        vendor = vendorParts[1];
                     }
                  } else if (str.trim().startsWith("defaultModuleVersion")) {
                     String[] versionParts = str.split("\"");
                     if (versionParts.length >= 2) {
                        version = versionParts[1];
                     }
                  }
               }
            }
         }
      } catch (IOException | UnresolvedException var20) {
      }

      return new Pair(vendor, version);
   }

   public static String getVendorFromVendorGradle(FilePath rootPath) {
      BFileSystem fs = BFileSystem.INSTANCE;
      FilePath vendorGradlePath = rootPath.merge("vendor.gradle");

      try {
         BIFile vendorGradleFile = fs.findFile(vendorGradlePath);
         if (vendorGradleFile != null) {
            try (LineNumberReader isr = new LineNumberReader(new InputStreamReader(vendorGradleFile.getInputStream()))) {
               for (String str = isr.readLine(); str != null; str = isr.readLine()) {
                  if (str.trim().startsWith("group")) {
                     String[] versionParts = str.split("'");
                     if (versionParts.length >= 2) {
                        return versionParts[1];
                     }
                  }
               }

               return null;
            }
         }
      } catch (IOException | UnresolvedException var21) {
      }

      return null;
   }

   public static String getVersionFromVendorGradle(FilePath rootPath) {
      BFileSystem fs = BFileSystem.INSTANCE;
      FilePath vendorGradlePath = rootPath.merge("vendor.gradle");

      try {
         BIFile vendorGradleFile = fs.findFile(vendorGradlePath);
         if (vendorGradleFile != null) {
            try (LineNumberReader isr = new LineNumberReader(new InputStreamReader(vendorGradleFile.getInputStream()))) {
               for (String str = isr.readLine(); str != null; str = isr.readLine()) {
                  if (str.trim().startsWith("def moduleVersion")) {
                     String[] versionParts = str.split("'");
                     if (versionParts.length >= 2) {
                        return versionParts[1];
                     }
                  }
               }

               return null;
            }
         }
      } catch (IOException | UnresolvedException var21) {
      }

      return null;
   }

   private void buildStep1() {
      BLabel label = new BLabel(lex.getText("newModule.selectDependencies"), BHalign.left);
      NewModuleWizard.DependenciesModel model = new NewModuleWizard.DependenciesModel();
      BModule m = Sys.getBajaModule();
      model.addRow(new NewModuleWizard.NreModuleInfo(m.getModuleInfo(RuntimeProfile.rt)));
      model.addRow(m.getModuleInfo(RuntimeProfile.rt));
      this.dependencies = new BTable(model);
      this.dependencies.setController(new NewModuleWizard.Controller());
      this.useVendorVersion = new BCheckBox(new NewModuleWizard.UseVendorVersion(this.shell));
      this.useVendorVersion.setSelected(true);
      BGridPane top = new BGridPane(1);
      top.setHalign(BHalign.left);
      top.add(null, label);
      top.add(null, new BBorderPane(this.useVendorVersion, 0.0, 0.0, 0.0, 5.0));
      BGridPane grid = new BGridPane(1);
      grid.setColumnAlign(BHalign.fill);
      grid.setValign(BValign.top);
      grid.add(null, new BButton(new NewModuleWizard.Add(this.shell)));
      grid.add(null, new BButton(new NewModuleWizard.Remove(this.shell)));
      BEdgePane pane = new BEdgePane();
      pane.setTop(new BBorderPane(top, 0.0, 0.0, 5.0, 0.0));
      pane.setCenter(new BBorderPane(this.dependencies, BBorder.inset, BInsets.make(0.0, 0.0, 0.0, 0.0)));
      pane.setRight(new BBorderPane(grid, 0.0, 0.0, 0.0, 5.0));
      BEdgePane edge = new BEdgePane();
      edge.setTop(new BWizardHeader(banner, this.getTitle(), UiLexicon.bajaui().getText("wizard.step2") + " of 3"));
      edge.setCenter(new BBorderPane(pane, 10.0, 10.0, 10.0, 10.0));
      this.step1 = edge;
   }

   private void initStep1() {
      this.saveRtp();
   }

   private RuntimeProfile getHighestRuntimeProfile() {
      RuntimeProfile highest = RuntimeProfile.rt;

      for (RuntimeProfile rtp : this.enabledRuntimeProfiles) {
         if (!rtp.supportsDependency(highest)) {
            highest = rtp;
         }
      }

      return highest;
   }

   private BWidget buildRtpFE() {
      BGridPane content = new BGridPane(1);
      content.setValign(BValign.top);
      content.setHalign(BHalign.left);

      for (RuntimeProfile profile : RuntimeProfile.values()) {
         if (!profile.equals(RuntimeProfile.doc)) {
            ToggleCommand cmd = new ToggleCommand(
               content,
               Lexicon.make("platDaemon", this.shell.getContext()).getText(String.format("RuntimeProfile.%s.long", profile.name())),
               null,
               null,
               Lexicon.make("platDaemon", this.shell.getContext()).getText(String.format("RuntimeProfile.%s.long", profile.name()))
            );
            BCheckBox checkBox;
            this.checkBoxes.put(profile, checkBox = new BCheckBox(cmd));
            content.add(null, checkBox);
         }
      }

      for (RuntimeProfile rtp : this.enabledRuntimeProfiles) {
         this.checkBoxes.get(rtp).setSelected(true);
      }

      return new BBorderPane(content, 0.0, 0.0, 0.0, 20.0);
   }

   private void saveRtp() {
      try {
         Set<RuntimeProfile> checkedProfiles = this.checkBoxes
            .entrySet()
            .stream()
            .filter(entry -> entry.getValue().isSelected())
            .map(Entry::getKey)
            .collect(Collectors.toSet());
         if (this.enabledRuntimeProfiles == null || !MapUtil.compare(checkedProfiles, this.enabledRuntimeProfiles).matchesExactly()) {
            this.enabledRuntimeProfiles = checkedProfiles;
         }
      } catch (Exception var2) {
      }
   }

   private BOptionDialog getDialog(BWidget w) {
      w = w.getParentWidget();

      while (!(w instanceof BDialog)) {
         w = w.getParentWidget();
      }

      return (BOptionDialog)w;
   }

   private void buildStep2() {
      BLabel label = new BLabel(lex.getText("newModule.addPackages"), BHalign.left);
      DefaultTableModel model = new DefaultTableModel(new String[]{lex.getText("newModule.package"), lex.getText("newModule.runtimeProfile")});
      this.packages = new BTable(model);
      this.packages.setController(new NewModuleWizard.PackageController());
      BGridPane grid = new BGridPane(1);
      grid.setColumnAlign(BHalign.fill);
      grid.setValign(BValign.top);
      grid.add(null, new BButton(new NewModuleWizard.AddPackage(this.shell)));
      grid.add(null, new BButton(new NewModuleWizard.RemovePackage(this.shell)));
      BEdgePane pane = new BEdgePane();
      pane.setTop(new BBorderPane(label, 0.0, 0.0, 5.0, 0.0));
      pane.setCenter(new BBorderPane(this.packages, BBorder.inset, BInsets.make(0.0, 0.0, 0.0, 0.0)));
      pane.setRight(new BBorderPane(grid, 0.0, 0.0, 0.0, 5.0));
      BEdgePane edge = new BEdgePane();
      edge.setTop(new BWizardHeader(banner, this.getTitle(), UiLexicon.bajaui().getText("wizard.step3") + " of 3"));
      edge.setCenter(new BBorderPane(pane, 10.0, 10.0, 10.0, 10.0));
      this.step2 = edge;
   }

   private void initStep2() {
      if (!this.step2initialized) {
         this.addDefaultPackage((DefaultTableModel)this.packages.getModel());
         this.step2initialized = true;
      }
   }

   private void addDefaultPackage(DefaultTableModel m) {
      StringBuilder sb = new StringBuilder();
      sb.append("com.").append(this.vendor.getText().trim().toLowerCase()).append(".").append(this.moduleName.getText());
      this.basePackage = sb.toString();
      m.addRow(new String[]{this.basePackage, "rt"});

      for (RuntimeProfile rtp : this.enabledRuntimeProfiles) {
         if (rtp.equals(RuntimeProfile.wb)) {
            m.addRow(new String[]{this.basePackage + ".ui", "wb"});
         } else if (rtp.equals(RuntimeProfile.ux)) {
            m.addRow(new String[]{this.basePackage + ".ux", "ux"});
         } else if (rtp.equals(RuntimeProfile.se)) {
            m.addRow(new String[]{this.basePackage + ".se", "se"});
         }
      }
   }

   public String getTitle() {
      return lex.getText("newModule.title");
   }

   public Size getPreferredSizeOfSteps() {
      int width = 540;
      int height = 440;
      return !Sys.isStation() && BGeneralOptions.make().getFontSize() == BFontSize.large ? new Size(width * 1.3, height * 1.2) : new Size(width, height);
   }

   public void init() {
      this.updateTo0();
   }

   public void back() {
      switch (this.currentStep) {
         case 1:
            this.updateTo0();
            break;
         case 2:
            this.updateTo1();
      }
   }

   public void next() {
      switch (this.currentStep) {
         case 0:
            if (this.moduleDir.getText().length() == 0) {
               throw new BajaRuntimeException("Module directory is not valid.");
            }

            new FilePath(this.moduleDir.getText());
            if (this.moduleName.getText().length() == 0) {
               throw new BajaRuntimeException("Module name is not valid.");
            }

            if (!Character.isJavaIdentifierStart(this.moduleName.getText().charAt(0))) {
               throw new BajaRuntimeException("Module name cannot start with " + this.moduleName.getText().charAt(0) + ".");
            }

            Iterator<Integer> it = this.moduleName.getText().codePoints().iterator();

            while (it.hasNext()) {
               int cp = it.next();
               if (!Character.isJavaIdentifierPart(cp)) {
                  throw new BajaRuntimeException("Module name cannot contain " + new String(Character.toChars(cp)) + ".");
               }
            }

            if (this.prefSymbol.getText().length() == 0) {
               throw new BajaRuntimeException("Preferred symbol is not valid.");
            }

            if (this.prefSymbol.getText().length() > 8) {
               throw new BajaRuntimeException("Preferred symbol is too long.");
            }

            FilePath modulePath = new FilePath(this.moduleDir.getText()).merge(this.moduleName.getText());
            BFileSystem fs = BFileSystem.INSTANCE;
            BIFile moduleFile = fs.findFile(modulePath);
            if (moduleFile != null) {
               throw new BajaRuntimeException("Module " + this.moduleName.getText() + " already exists in " + this.moduleDir.getText() + ".");
            }

            try {
               new Version(this.version.getText());
            } catch (IllegalArgumentException var7) {
               throw new BajaRuntimeException("Version is not valid.", var7);
            }

            if (this.vendor.getText().trim().length() == 0) {
               throw new BajaRuntimeException("Vendor cannot be empty.");
            }

            if (!Character.isJavaIdentifierStart(this.vendor.getText().charAt(0))) {
               throw new BajaRuntimeException("Vendor cannot start with '" + this.vendor.getText().charAt(0) + "'.");
            }

            Iterator<Integer> vendorIt = this.vendor.getText().codePoints().iterator();

            while (vendorIt.hasNext()) {
               int cp = vendorIt.next();
               if (!Character.isJavaIdentifierPart(cp)) {
                  throw new BajaRuntimeException("Vendor cannot contain '" + new String(Character.toChars(cp)) + "'.");
               }
            }

            this.updateTo1();
            break;
         case 1:
            this.updateTo2();
      }
   }

   public boolean finish() {
      this.shell.enterBusy();

      boolean var2;
      try {
         this.doit();
         return true;
      } catch (Throwable var6) {
         if (log.isLoggable(Level.FINE)) {
            log.log(Level.FINE, "Could not create new module", var6);
         }

         BDialog.error(this.shell, "Error", "Cannot create new module", var6);
         var2 = false;
      } finally {
         this.shell.exitBusy();
      }

      return var2;
   }

   private void updateTo0() {
      String name = this.moduleName.getText();
      String ps = this.prefSymbol.getText();
      String vendorName = this.vendor.getText();
      int mask = SlotPath.isValidName(name) && name.length() <= 45 && ps.length() > 0 && vendorName.length() > 0 ? 2 : 0;
      this.update(this.step0, mask);
      this.getWizard().setNextAsDefault();
      this.moduleDir.requestFocus();
      this.currentStep = 0;
   }

   private void updateTo1() {
      int mode = 2;
      if (!this.step2initialized) {
         mode |= 1;
      }

      this.update(this.step1, mode);
      this.currentStep = 1;
      this.initStep1();
   }

   private void updateTo2() {
      this.update(this.step2, 5);
      this.currentStep = 2;
      this.initStep2();
   }

   private void doit() throws Exception {
      this.saveRtp();
      FilePath dirPath = new FilePath(this.moduleDir.getText());
      BFileSystem fs = BFileSystem.INSTANCE;
      fs.makeDir(dirPath, null);
      VelocityGenerator velocityGenerator = new VelocityGenerator(fs.pathToLocalFile(dirPath));
      GradleProjectGenerator gradleProjectGenerator = new GradleProjectGenerator(velocityGenerator);
      gradleProjectGenerator.setProjectName(this.moduleName.getText());
      gradleProjectGenerator.setVendor(this.vendor.getText());
      gradleProjectGenerator.setCopyright(this.vendor.getText());
      gradleProjectGenerator.setVersion(this.version.getText());
      FilePath existingProperties = dirPath.merge("gradle.properties");
      if (fs.findFile(existingProperties) == null) {
         configureGradleProperties(gradleProjectGenerator);
      }

      NiagaraModuleGenerator niagaraModuleGenerator = NiagaraModuleGenerator.make(gradleProjectGenerator, this.moduleName.getText());
      niagaraModuleGenerator.setRuntimeProfiles(
         this.enabledRuntimeProfiles.stream().map(it -> ModuleRuntimeProfile.valueOf(it.name())).collect(Collectors.toSet())
      );
      niagaraModuleGenerator.setPreferredSymbol(this.prefSymbol.getText());
      niagaraModuleGenerator.setDescription(this.description.getText());
      NewModuleWizard.DependenciesModel model = (NewModuleWizard.DependenciesModel)this.dependencies.getModel();

      for (RuntimeProfile rtp : this.enabledRuntimeProfiles) {
         NiagaraModulePartGenerator modulePartGenerator = niagaraModuleGenerator.getModulePart(ModuleRuntimeProfile.valueOf(rtp.name()));
         NiagaraModuleDependencies dependencies = modulePartGenerator.getDependencies();

         for (int i = 0; i < model.getRowCount(); i++) {
            ModuleInfo dependency = model.getModuleInfo(i);
            if (!(dependency instanceof NewModuleWizard.NreModuleInfo) && model.getModuleInfo(i).getRuntimeProfile().supportsDependency(rtp)) {
               ModuleDependency moduleDependency = new ModuleDependency("", dependency.getModulePartName());
               dependencies.addDependency(moduleDependency);
            }
         }

         DefaultTableModel pm = (DefaultTableModel)this.packages.getModel();

         for (int ix = 0; ix < pm.getRowCount(); ix++) {
            if (rtp.name().equals(pm.getValueAt(ix, 1))) {
               modulePartGenerator.addPackage((String)pm.getValueAt(ix, 0));
            }
         }

         if (this.usePalette.isSelected()) {
            modulePartGenerator.addPalette();
         }
      }

      AccessController.doPrivileged((PrivilegedAction<Void>)(() -> {
         ClassLoader currentClassLoader = Thread.currentThread().getContextClassLoader();

         try {
            Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
            gradleProjectGenerator.generate();
            niagaraModuleGenerator.generate();
         } finally {
            Thread.currentThread().setContextClassLoader(currentClassLoader);
         }

         return null;
      }));
      this.shell.hyperlink(BOrd.make(fs.getNavOrd(), dirPath));
   }

   public static void configureGradleProperties(GradleProjectGenerator gradleProjectGenerator) {
      configureGradleProperties(gradleProjectGenerator, Nre.getNiagaraHome().getAbsolutePath(), Sys.getNiagaraUserHome().getAbsolutePath(), File.separator);
   }

   public static void configureGradleProperties(GradleProjectGenerator gradleProjectGenerator, String niagaraHome, String niagaraUserHome, String fileSeparator) {
      VelocityContext context = gradleProjectGenerator.getContext();
      context.put("niagara_home", niagaraHome.replace("\\", "\\\\"));
      context.put("niagara_user_home", niagaraUserHome.replace("\\", "\\\\"));
      context.put("fileSeparator", fileSeparator.replace("\\", "\\\\"));
      gradleProjectGenerator.addTemplateWrite("rc/gradle.properties.vm", "gradle.properties");
   }

   class Add extends Command {
      Add(BWidget owner) {
         super(owner, NewModuleWizard.lex.getText("newModule.add"));
      }

      public CommandArtifact doInvoke() {
         NewModuleWizard.DependenciesModel childDependenciesModel = NewModuleWizard.this.new DependenciesModel();
         BModule bajaModule = Sys.getBajaModule();
         childDependenciesModel.addRow(NewModuleWizard.this.new NreModuleInfo(bajaModule.getModuleInfo(RuntimeProfile.rt)));
         ModuleInfo[] m = Sys.getRegistry().getModules();
         RuntimeProfile rtp = NewModuleWizard.this.getHighestRuntimeProfile();

         for (int i = 0; i < m.length; i++) {
            if (m[i].getRuntimeProfile().supportsDependency(rtp)) {
               childDependenciesModel.addRow(m[i]);
            }
         }

         childDependenciesModel.sortByColumn(0, true);
         BTable childTable = new BTable(childDependenciesModel);
         childTable.setController(NewModuleWizard.this.new ChildController());
         BConstrainedPane pane = new BConstrainedPane(new BBorderPane(childTable, BBorder.inset, BInsets.make(0.0, 0.0, 0.0, 0.0)));
         pane.setMinWidth(400.0);
         pane.setMaxHeight(300.0);
         BWidget c = new BBorderPane(pane, 10.0, 10.0, 10.0, 10.0);
         if (1 == BDialog.open(this.getOwner(), NewModuleWizard.lex.getText("newModule.selectDependencies"), c, 3)) {
            int[] rows = childTable.getSelection().getRows();
            if (rows.length == 0) {
               return null;
            }

            NewModuleWizard.DependenciesModel model = (NewModuleWizard.DependenciesModel)NewModuleWizard.this.dependencies.getModel();

            for (int ix = 0; ix < rows.length; ix++) {
               model.addRow(childDependenciesModel.getModuleInfo(rows[ix]));
            }

            NewModuleWizard.this.dependencies.relayout();
         }

         return null;
      }
   }

   class AddPackage extends Command {
      AddPackage(BWidget owner) {
         super(owner, NewModuleWizard.lex.getText("newModule.add"));
      }

      public CommandArtifact doInvoke() {
         ToggleCommandGroup<ToggleCommand> modulePart = new ToggleCommandGroup();
         BGridPane rtpRadioBtns = new BGridPane(8);

         for (RuntimeProfile profile : RuntimeProfile.values()) {
            if (NewModuleWizard.this.enabledRuntimeProfiles.contains(profile)) {
               rtpRadioBtns.add("radio?", new BRadioButton(modulePart, profile.name()));
            }
         }

         BTextField pkg = new BTextField(NewModuleWizard.this.basePackage, NewModuleWizard.this.basePackage.length() + 12);
         BGridPane addPackageContent = new BGridPane(2);
         addPackageContent.add("lbl?", new BLabel(NewModuleWizard.lex.getText("newModule.package")));
         addPackageContent.add("widget?", pkg);
         addPackageContent.add("lbl?", new BLabel(NewModuleWizard.lex.getText("newModule.runtimeProfile")));
         addPackageContent.add("widget?", rtpRadioBtns);
         int retVal = BDialog.open(this.getOwner(), NewModuleWizard.lex.getText("newModule.add"), addPackageContent, 3);
         if (retVal != 1) {
            return null;
         } else {
            String p = pkg.getText();
            if (p != null) {
               String[] packageParts = p.split("\\.");

               for (String packageName : packageParts) {
                  if (!Character.isJavaIdentifierStart(packageName.charAt(0))) {
                     throw new BajaRuntimeException("Package name cannot start with '" + packageName.charAt(0) + "'.");
                  }

                  Iterator<Integer> it = packageName.codePoints().iterator();

                  while (it.hasNext()) {
                     int cp = it.next();
                     if (!Character.isJavaIdentifierPart(cp)) {
                        throw new BajaRuntimeException("Package name cannot contain '" + new String(Character.toChars(cp)) + "'.");
                     }
                  }
               }

               DefaultTableModel m = (DefaultTableModel)NewModuleWizard.this.packages.getModel();
               boolean packageExists = false;

               for (int row = 0; row < m.getRowCount(); row++) {
                  if (m.getValueAt(row, 0).equals(p)) {
                     packageExists = true;
                     BDialog.error(this.getOwner(), NewModuleWizard.lex.getText("newModule.duplicatePackage", new Object[]{p, m.getValueAt(row, 1).toString()}));
                     break;
                  }
               }

               if (!packageExists) {
                  m.addRow(new String[]{p, modulePart.getSelected().getLabel()});
                  NewModuleWizard.this.packages.relayout();
                  NewModuleWizard.this.packages.sizeColumnsToFit();
               }
            }

            return null;
         }
      }
   }

   public static class Browse extends Command {
      BTextField moduleDir;
      BTextField vendor;
      BTextField version;

      public Browse(BWidget owner, BTextField moduleDir, BTextField vendor, BTextField version) {
         super(owner, "");
         this.moduleDir = moduleDir;
         this.vendor = vendor;
         this.version = version;
         this.updateVendorAndVersion();
      }

      public String getText() {
         return null;
      }

      public BImage getIcon() {
         return NewModuleWizard.openIcon;
      }

      public CommandArtifact doInvoke() {
         if (NewModuleWizard.chooser == null) {
            NewModuleWizard.chooser = BDirectoryChooser.make(this.getOwner());
         }

         BOrd file = NewModuleWizard.chooser.show();
         if (file != null) {
            OrdQuery[] q = file.parse();
            this.moduleDir.setText(q[q.length - 1].getBody());
            this.moduleDir.repaint();
            this.updateVendorAndVersion();
         }

         return null;
      }

      private void updateVendorAndVersion() {
         Pair<String, String> vendorAndVersion = NewModuleWizard.getVendorAndVersionFromBuildGradle(new FilePath(this.moduleDir.getText()));
         String vendorStr = (String)vendorAndVersion.getFirst();
         String versionStr = (String)vendorAndVersion.getSecond();
         if (vendorStr != null) {
            this.vendor.setText(vendorStr);
            this.vendor.setEditable(false);
         } else {
            this.vendor.setText("");
            this.vendor.setEditable(true);
         }

         if (versionStr != null) {
            this.version.setText(versionStr);
            this.version.setEditable(false);
         } else {
            this.version.setText("1.0");
            this.version.setEditable(true);
         }
      }
   }

   class CanNextModel extends TextModel {
      protected void textModified() {
         String text = NewModuleWizard.this.moduleName.getText();
         if (NewModuleWizard.this.getWizard() != null) {
            NewModuleWizard.this.getWizard()
               .setNextEnabled(
                  SlotPath.isValidName(text)
                     && text.length() <= 45
                     && NewModuleWizard.this.prefSymbol.getText().length() > 0
                     && NewModuleWizard.this.vendor.getText().length() > 0
               );
         }
      }
   }

   class ChildController extends TableController {
      protected void cellDoubleClicked(BMouseEvent event, int row, int column) {
         NewModuleWizard.this.getDialog(this.getTable()).getOkButton().invokeAction();
      }
   }

   class Controller extends TableController {
      public void keyReleased(BKeyEvent event) {
         super.keyReleased(event);
         if (event.getKeyCode() == 127) {
            NewModuleWizard.this.new Remove(NewModuleWizard.this.shell).invoke();
         }
      }
   }

   class DependenciesModel extends TableModel {
      private ArrayList<ModuleInfo> moduleInfos = new ArrayList<>();
      private String[] cols = new String[]{
         NewModuleWizard.lex.getText("newModule.moduleName"),
         NewModuleWizard.lex.getText("newModule.vendor"),
         NewModuleWizard.lex.getText("newModule.vendorVersion")
      };
      public boolean vendor = true;

      public void addRow(ModuleInfo moduleInfo) {
         this.moduleInfos.add(moduleInfo);
      }

      public void removeRow(int row) {
         this.moduleInfos.remove(row);
      }

      public int getRowCount() {
         return this.moduleInfos.size();
      }

      public int getColumnCount() {
         return this.cols.length;
      }

      public String getColumnName(int col) {
         return this.cols[col];
      }

      public BImage getRowIcon(int row) {
         return NewModuleWizard.moduleIcon;
      }

      public Object getSubject(int row) {
         return this.moduleInfos.get(row);
      }

      public Object getValueAt(int row, int col) {
         ModuleInfo moduleInfo = this.moduleInfos.get(row);
         switch (col) {
            case 0:
               return moduleInfo.getModulePartName();
            case 1:
               return this.vendor ? moduleInfo.getVendor() : "-";
            case 2:
               return this.vendor ? moduleInfo.getVendorVersion().toString(2) : "-";
            default:
               return "";
         }
      }

      public ModuleInfo getModuleInfo(int row) {
         return this.moduleInfos.get(row);
      }

      public void sortByColumn(int col, boolean ascending) {
         if (col == 0) {
            Collections.sort(this.moduleInfos, new NewModuleWizard.DependenciesModel.ModuleNameComparator());
         } else {
            super.sortByColumn(col, ascending);
         }
      }

      private class ModuleNameComparator implements Comparator<ModuleInfo> {
         private ModuleNameComparator() {
         }

         public int compare(ModuleInfo o1, ModuleInfo o2) {
            return o1.getModulePartName().compareTo(o2.getModulePartName());
         }
      }
   }

   private class NreModuleInfo implements ModuleInfo {
      ModuleInfo bajaModuleInfo;

      NreModuleInfo(ModuleInfo bajaModuleInfo) {
         this.bajaModuleInfo = bajaModuleInfo;
      }

      public RuntimeProfile getRuntimeProfile() {
         return RuntimeProfile.rt;
      }

      public String getModuleName() {
         return "nre";
      }

      public String getModulePartName() {
         return "nre";
      }

      public Version getBajaVersion() {
         return this.bajaModuleInfo.getBajaVersion();
      }

      public String getVendor() {
         return this.bajaModuleInfo.getVendor();
      }

      public Version getVendorVersion() {
         return this.bajaModuleInfo.getVendorVersion();
      }

      public String getDescription() {
         return this.bajaModuleInfo.getDescription();
      }

      public DependencyInfo[] getDependencies() {
         return new DependencyInfo[0];
      }

      public TypeInfo[] getTypes() {
         return new TypeInfo[0];
      }

      public boolean isTransient() {
         return false;
      }
   }

   class PackageController extends TableController {
      public void keyReleased(BKeyEvent event) {
         super.keyReleased(event);
         if (event.getKeyCode() == 127) {
            NewModuleWizard.this.new RemovePackage(NewModuleWizard.this.shell).invoke();
         }
      }
   }

   class Remove extends Command {
      Remove(BWidget owner) {
         super(owner, NewModuleWizard.lex.getText("newModule.remove"));
      }

      public CommandArtifact doInvoke() {
         NewModuleWizard.DependenciesModel model = (NewModuleWizard.DependenciesModel)NewModuleWizard.this.dependencies.getModel();
         int[] rows = NewModuleWizard.this.dependencies.getSelection().getRows();

         for (int i = 0; i < rows.length; i++) {
            model.removeRow(rows[i] - i);
         }

         NewModuleWizard.this.dependencies.relayout();
         return null;
      }
   }

   class RemovePackage extends Command {
      RemovePackage(BWidget owner) {
         super(owner, NewModuleWizard.lex.getText("newModule.remove"));
      }

      public CommandArtifact doInvoke() {
         DefaultTableModel model = (DefaultTableModel)NewModuleWizard.this.packages.getModel();
         int[] rows = NewModuleWizard.this.packages.getSelection().getRows();

         for (int i = 0; i < rows.length; i++) {
            model.removeRow(rows[i] - i);
         }

         NewModuleWizard.this.packages.relayout();
         return null;
      }
   }

   class UseVendorVersion extends ToggleCommand {
      UseVendorVersion(BWidget owner) {
         super(owner, NewModuleWizard.lex.getText("newModule.vendorVersion"));
      }

      public CommandArtifact doInvoke() {
         NewModuleWizard.DependenciesModel model = (NewModuleWizard.DependenciesModel)NewModuleWizard.this.dependencies.getModel();
         model.vendor = this.isSelected();
         NewModuleWizard.this.dependencies.relayout();
         return null;
      }
   }
}
