package com.tridium.program.ui.module;

import com.tridium.crypto.core.io.CoreCryptoManager;
import com.tridium.devkit.wizards.NewModuleWizard;
import com.tridium.gradle.plugins.templates.GradleProjectGenerator;
import com.tridium.gradle.plugins.templates.ModuleDependency;
import com.tridium.gradle.plugins.templates.ModuleRuntimeProfile;
import com.tridium.gradle.plugins.templates.NiagaraModuleDependencies;
import com.tridium.gradle.plugins.templates.NiagaraModuleGenerator;
import com.tridium.gradle.plugins.templates.NiagaraModulePartGenerator;
import com.tridium.gradle.plugins.templates.VelocityGenerator;
import com.tridium.nre.security.SecurityInitializer;
import com.tridium.nre.security.policy.NiagaraPolicy.PolicyType;
import com.tridium.program.BProgram;
import com.tridium.program.module.BProgramModule;
import com.tridium.program.ui.signing.BCodeSigningOptions;
import com.tridium.security.BPermissionGroupInfo;
import com.tridium.workbench.console.BConsole;
import com.tridium.workbench.console.BConsole.ExecCallback;
import com.tridium.workbench.shell.BNiagaraWbShell;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.security.AccessController;
import java.security.Key;
import java.security.KeyStore;
import java.security.PrivilegedAction;
import java.security.PrivilegedExceptionAction;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;
import javax.baja.file.BFileSystem;
import javax.baja.file.BajaFileUtil;
import javax.baja.file.FilePath;
import javax.baja.nre.util.SortUtil;
import javax.baja.sys.BAbsTime;
import javax.baja.sys.BIcon;
import javax.baja.sys.Sys;
import javax.baja.ui.BWidget;
import javax.baja.util.BNameMap;
import javax.baja.util.Lexicon;
import javax.baja.xml.XElem;
import javax.baja.xml.XWriter;

public final class BuildHelper {
   BProgramModule progMod;
   private static final BIcon icon = BIcon.make(BIcon.std("module.png"), BIcon.std("badges/script.png"));
   private static final Lexicon lex = Lexicon.make(BuildHelper.class);
   private static final String packageName = "autogen";
   private static final Logger log = Logger.getLogger("program");
   private static final String SIGNING_XML_FILE = "niagara.signing.xml";
   private static final String SIGNING_KEYSTORE = "niagara.signing.jceks";
   private static BuildStep[] buildSteps = new BuildStep[]{
      new BuildHelper.ValidateStep(),
      new BuildHelper.InitStep(),
      new BuildHelper.GenerateGradleProjectStep(),
      new BuildHelper.WriteComponentsStep(),
      new BuildHelper.WriteModuleIncludeStep(),
      new BuildHelper.WriteModulePermissionsStep(),
      new BuildHelper.WritePaletteStep(),
      new BuildHelper.WriteLexiconStep(),
      new BuildHelper.ConfigureSigningStep(),
      new BuildHelper.NiagaraBuildStep()
   };
   private File buildRoot;
   private File moduleDir;
   Map<String, String> typeMap;

   public BuildHelper(BProgramModule pm) {
      this.progMod = pm;
   }

   public BProgramModule pmod() {
      return this.progMod;
   }

   public static String toTypeName(BProgram program) {
      return program.getDisplayName(null);
   }

   public static String toComponentName(BProgram program) {
      if (program.getParent() == null) {
         throw new IllegalStateException("Must be parented.");
      } else {
         String typeName = toTypeName(program);
         if (!Character.isUpperCase(typeName.charAt(0))) {
            return null;
         } else {
            StringBuilder compName = new StringBuilder("B").append(typeName.charAt(0));

            for (int i = 1; i < typeName.length(); i++) {
               char ch = typeName.charAt(i);
               if (!Character.isLetterOrDigit(ch)) {
                  return null;
               }

               compName.append(ch);
            }

            return compName.toString();
         }
      }
   }

   public void buildModule(IBuildListener interest) throws BuildException {
      if (interest == null) {
         interest = new IBuildListener() {
            @Override
            public void updateDesc(String desc) {
            }

            @Override
            public void setNumBuildSteps(int steps) {
            }

            @Override
            public void nextStep(String desc) {
            }

            @Override
            public BWidget getOwner() {
               return null;
            }
         };
      }

      interest.setNumBuildSteps(buildSteps.length + this.progMod.listPrograms().length);
      interest.updateDesc(lex.getText("programModule.build", new Object[]{this.progMod.getModuleName()}));

      for (BuildStep buildStep : buildSteps) {
         buildStep.runStep(this, interest);
      }
   }

   File getBuildRoot() {
      return this.buildRoot;
   }

   void setBuildRoot(File buildRoot) {
      this.buildRoot = buildRoot;
   }

   File getModuleDir() {
      return this.moduleDir;
   }

   void setModuleDir(File moduleDir) {
      this.moduleDir = moduleDir;
   }

   String getPreferredSymbol() {
      return this.progMod.getModuleName();
   }

   public BIcon getIcon() {
      return icon;
   }

   static class ConfigureSigningStep extends BuildStep {
      @Override
      protected synchronized void doStep() throws Exception {
         BCodeSigningOptions options = BCodeSigningOptions.make();
         String alias = options.getSigningCert();
         String tsaUrl = options.getTsaUrl();
         if ((alias == null || alias.isEmpty())
            && !AccessController.doPrivileged((PrivilegedAction<Boolean>)(() -> Boolean.getBoolean("program.requireSigning")))) {
            BuildHelper.log.warning(BuildHelper.lex.getText("programModule.willNotSign"));
            this.interest.nextStep(BuildHelper.lex.getText("programModule.nbuild"));
         } else if (alias != null && !alias.isEmpty()) {
            CoreCryptoManager ccm = AccessController.doPrivileged((PrivilegedAction<CoreCryptoManager>)(() -> {
               try {
                  return CoreCryptoManager.get(SecurityInitializer.getInstance().getSecurityInfoProvider());
               } catch (Exception var1x) {
                  return null;
               }
            }));
            char[] keyPassword = null;

            try {
               keyPassword = AccessController.doPrivileged((PrivilegedAction<char[]>)(() -> {
                  try {
                     return options.getKeyPassword(this.interest.getOwner());
                  } catch (Exception var3x) {
                     throw new RuntimeException(var3x);
                  }
               }));
            } catch (RuntimeException var42) {
               if (var42.getCause() instanceof Exception) {
                  throw (Exception)var42.getCause();
               }

               throw var42;
            }

            Key key = ccm.getKeyStore().getKey(alias, (char[])keyPassword.clone());
            X509Certificate[] certs = ccm.getKeyStore().getCertificateChain(alias);
            String keyPassString = new BigInteger(96, new SecureRandom()).toString(32);
            String storePassString = new BigInteger(96, new SecureRandom()).toString(32);
            File storeFile = new File(this.helper.buildRoot, "niagara.signing.jceks");
            KeyStore store = KeyStore.getInstance("JCEKS");
            store.load(null, null);
            AccessController.doPrivileged((PrivilegedExceptionAction)(() -> {
               store.setKeyEntry(alias, key, keyPassString.toCharArray(), certs);
               return null;
            }));

            try (FileOutputStream out = new FileOutputStream(storeFile)) {
               store.store(out, storePassString.toCharArray());
            }

            File profile = new File(this.helper.getBuildRoot(), "niagara.signing.xml");
            Properties signingProperties = new Properties();
            signingProperties.put("niagara.signing.profileType", "com.tridium.gradle.plugins.signing.profile.RestrictedSigningProfile");
            signingProperties.put("niagara.signing.storetype", "JCEKS");
            signingProperties.put("niagara.signing.storepath", storeFile.getAbsolutePath().replace("\\", "\\\\"));
            signingProperties.put("niagara.signing.storepass", storePassString);
            signingProperties.put("niagara.signing.keypass." + alias, keyPassString);
            if (tsaUrl != null && !tsaUrl.isEmpty()) {
               signingProperties.put("niagara.signing.standardtsa", tsaUrl);
            }

            try (OutputStream os = new BufferedOutputStream(new FileOutputStream(profile))) {
               signingProperties.storeToXML(os, "Signing Profile");
            }

            this.interest.nextStep(BuildHelper.lex.getText("programModule.nbuild"));
         } else {
            throw new Exception(BuildHelper.lex.getText("program.certNotSelected"));
         }
      }
   }

   static class GenerateGradleProjectStep extends BuildStep {
      @Override
      protected void doStep() throws Exception {
         BFileSystem fs = BFileSystem.INSTANCE;
         FilePath rootPath = fs.localFileToPath(this.helper.getBuildRoot());
         fs.makeDir(rootPath, null);
         VelocityGenerator velocityGenerator = new VelocityGenerator(fs.pathToLocalFile(rootPath));
         GradleProjectGenerator gradleProjectGenerator = new GradleProjectGenerator(velocityGenerator);
         gradleProjectGenerator.setProjectName(this.helper.pmod().getModuleName());
         gradleProjectGenerator.setVendor(this.helper.pmod().getVendor());
         gradleProjectGenerator.setVersion(this.helper.pmod().getVendorVersion());
         gradleProjectGenerator.setCopyright(this.helper.pmod().getVendor());
         NewModuleWizard.configureGradleProperties(gradleProjectGenerator);
         ModuleRuntimeProfile rtp = ModuleRuntimeProfile.valueOf(this.helper.pmod().getRuntimeProfile().name());
         NiagaraModuleGenerator niagaraModuleGenerator = NiagaraModuleGenerator.make(gradleProjectGenerator, this.helper.pmod().getModuleName());
         niagaraModuleGenerator.setRuntimeProfiles(new ModuleRuntimeProfile[]{rtp});
         niagaraModuleGenerator.setPreferredSymbol(this.helper.getPreferredSymbol());
         niagaraModuleGenerator.setDescription(this.helper.pmod().getDescription());
         NiagaraModulePartGenerator modulePartGenerator = niagaraModuleGenerator.getModulePart(rtp);
         modulePartGenerator.addPackage("autogen");
         NiagaraModuleDependencies dependencies = modulePartGenerator.getDependencies();
         BNameMap depends = this.helper.pmod().getDependencies();
         String[] modules = depends.list();
         SortUtil.sort(modules);

         for (String name : modules) {
            ModuleDependency moduleDependency = new ModuleDependency("Tridium", name);
            dependencies.addDependency(moduleDependency);
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
         BCodeSigningOptions options = BCodeSigningOptions.make();
         String certAlias = options.getSigningCert();
         if (certAlias != null && !certAlias.isEmpty()) {
            File buildGradleKts = fs.pathToLocalFile(rootPath.merge("build.gradle.kts"));

            try (PrintWriter writer = new PrintWriter(new FileOutputStream(buildGradleKts, true))) {
               writer.println("signingServices {");
               writer.println("  signingProfileFactory {");
               writer.println("    allowDefaultProfile.set(false)");
               writer.println("  }");
               writer.println("}");
               writer.println();
               writer.println("niagaraSigning {");
               writer.println("  aliases.set(listOf(\"" + certAlias + "\"))");
               writer.println("  signingProfileFile.set(project.layout.projectDirectory.file(\"niagara.signing.xml\"))");
               writer.println("}");
            }
         }
      }
   }

   static class InitStep extends BuildStep {
      @Override
      public void doStep() throws Exception {
         File temp = new File(Sys.getNiagaraUserHome(), "temp");
         BNameMap depends = BProgramModule.rationalizeDependencies(this.helper.pmod());
         this.helper.pmod().setDependencies(depends);
         File buildDir = new File(temp, this.helper.pmod().getModuleName());
         BajaFileUtil.delete(buildDir);
         File moduleDir = new File(
            buildDir, this.helper.pmod().getModuleName() + '/' + this.helper.pmod().getModuleName() + "-" + this.helper.pmod().getRuntimeProfile().toString()
         );
         moduleDir.mkdirs();
         this.helper.setBuildRoot(buildDir);
         this.helper.setModuleDir(moduleDir);
         this.helper.typeMap = new HashMap<>();
         this.interest.nextStep(BuildHelper.lex.getText("programModule.convert"));
      }
   }

   static class NiagaraBuildStep extends BuildStep implements ExecCallback {
      String cmd;
      int exitCode = -1;

      @Override
      protected synchronized void doStep() throws Exception {
         File gradlewFile = new File(this.helper.getBuildRoot(), "gradlew");
         this.cmd = "\"" + gradlewFile.getAbsolutePath() + "\" -p \"" + this.helper.getBuildRoot() + "\" jar --no-daemon --console=plain";
         BWidget owner = this.interest.getOwner();
         if (owner != null && owner.getShell() != null) {
            ((BNiagaraWbShell)owner.getShell()).openConsole().exec(this.cmd, this);
            this.wait();
            if (this.exitCode != 0) {
               throw new BuildException(BuildHelper.lex.getText("programModule.nbuild.fail"));
            } else {
               this.interest.nextStep(BuildHelper.lex.getText("programModule.complete"));
            }
         } else {
            throw new IllegalStateException("must be in workbench.");
         }
      }

      public synchronized void consoleExecDone(BConsole console, int exitCode) {
         File profile = new File(this.helper.getBuildRoot(), "niagara.signing.xml");
         File storeFile = new File(this.helper.buildRoot, "niagara.signing.jceks");
         profile.delete();
         storeFile.delete();
         this.exitCode = exitCode;
         this.notify();
      }
   }

   static class ValidateStep extends BuildStep {
      private String modOrd;

      @Override
      public void doStep() throws Exception {
         this.interest.updateDesc(BuildHelper.lex.getText("programModule.validate"));
         this.modOrd = this.helper.pmod().getNavOrd().toString();
         boolean err = false;
         ValidateModel m = ValidateModel.make(this.helper.pmod());
         if (m.getErrCount() > 0) {
            err = true;
            this.appendErrOrdLines(null, m);
         }

         BProgram[] programs = this.helper.pmod().listPrograms();

         for (BProgram program : programs) {
            m = ValidateModel.make(program);
            if (m.getErrCount() > 0) {
               err = true;
               this.appendErrOrdLines(program, m);
            }
         }

         if (err) {
            throw new BuildException(BuildHelper.lex.getText("programModule.validate.failed"));
         } else {
            this.interest.nextStep(BuildHelper.lex.getText("programModule.init"));
         }
      }

      private void appendErrOrdLines(BProgram p, ValidateModel m) {
         StringBuilder ord = new StringBuilder().append(this.modOrd).append("|view:program:ProgramModuleBuilder");
         if (p != null) {
            ord.append("?program=").append(BuildHelper.toTypeName(p));
         }

         int numErrs = m.getErrCount();
         String ordStr = ord.toString();

         for (int i = 0; i < numErrs; i++) {
            this.interest.updateDesc(ordStr + "|| " + m.getValueAt(i, 0));
         }
      }
   }

   static class WriteComponentsStep extends BuildStep {
      File packageDir;

      @Override
      public void doStep() throws Exception {
         File srcDir = new File(this.helper.getModuleDir(), "src");
         this.packageDir = new File(srcDir, "autogen");
         if (!this.packageDir.exists() && !this.packageDir.mkdirs()) {
            throw new BuildException(BuildHelper.lex.getText("programModule.convert.failPkg", new Object[]{this.packageDir}));
         } else {
            BProgram[] programs = this.helper.pmod().listPrograms();

            for (BProgram program : programs) {
               String compName = BuildHelper.toComponentName(program);
               String compClass = compName + ".java";
               this.interest.nextStep(BuildHelper.lex.getText("programModule.convert.write", new Object[]{compName}));
               this.helper.typeMap.put(BuildHelper.toTypeName(program), "autogen." + compName);
               FileWriter javaOut = new FileWriter(new File(this.packageDir, compClass));
               new ComponentWriter(javaOut, program, "autogen").generate();
            }

            this.interest.nextStep(BuildHelper.lex.getText("programModule.rootGradle"));
         }
      }
   }

   static class WriteLexiconStep extends BuildStep {
      @Override
      protected void doStep() throws Exception {
         File lexicon = new File(this.helper.getModuleDir(), "module.lexicon");

         try (PrintStream out = new PrintStream(new FileOutputStream(lexicon))) {
            out.println("#");
            out.println("# Lexicon auto-generated by the Program Module-Builder");
            out.println("#");
         }

         this.interest.nextStep(BuildHelper.lex.getText("programModule.configureSigning"));
      }
   }

   static class WriteModuleIncludeStep extends BuildStep {
      @Override
      public void doStep() throws Exception {
         File miFile = new File(this.helper.getModuleDir(), "module-include.xml");

         try {
            XWriter xmi = new XWriter(miFile);
            this.writeComments(xmi);
            this.writeTypes(xmi);
            xmi.close();
         } catch (Exception var3) {
            throw new BuildException(BuildHelper.lex.getText("programModule.include.fail"), var3);
         }

         this.interest.nextStep(BuildHelper.lex.getText("programModule.permissions"));
      }

      private void writeComments(XWriter xmi) throws IOException {
         StringBuilder comments = new StringBuilder()
            .append("<!--")
            .append("\n")
            .append("  - Module Include File")
            .append("\n")
            .append("  - Auto-generated by the Program Module-Builder.")
            .append("\n")
            .append("  - Date: ")
            .append(BAbsTime.now())
            .append("\n")
            .append("  -->")
            .append("\n\n");
         xmi.write(comments.toString());
      }

      private void writeTypes(XWriter xmi) throws IOException {
         XElem xtypes = new XElem("types");

         for (String typeName : this.helper.typeMap.keySet()) {
            String typeClass = this.helper.typeMap.get(typeName);
            XElem type = new XElem("type").addAttr("name", typeName).addAttr("class", typeClass);
            xtypes.addContent(type);
         }

         xtypes.write(xmi);
      }
   }

   static class WriteModulePermissionsStep extends BuildStep {
      @Override
      public void doStep() throws Exception {
         File mpFile = new File(this.helper.getModuleDir(), "module-permissions.xml");

         try {
            XWriter xmp = new XWriter(mpFile);
            Throwable var3 = null;

            try {
               this.writeComments(xmp);
               this.writePermissions(xmp);
            } catch (Throwable var13) {
               var3 = var13;
               throw var13;
            } finally {
               if (xmp != null) {
                  if (var3 != null) {
                     try {
                        xmp.close();
                     } catch (Throwable var12) {
                        var3.addSuppressed(var12);
                     }
                  } else {
                     xmp.close();
                  }
               }
            }
         } catch (Exception var15) {
            throw new BuildException(BuildHelper.lex.getText("programModule.permissions.fail"), var15);
         }

         this.interest.nextStep(BuildHelper.lex.getText("programModule.palette"));
      }

      private void writeComments(XWriter xmi) throws IOException {
         StringBuilder comments = new StringBuilder()
            .append("<!--")
            .append("\n")
            .append("  - Module Permissions File")
            .append("\n")
            .append("  - Auto-generated by the Program Module-Builder.")
            .append("\n")
            .append("  - Date: ")
            .append(BAbsTime.now())
            .append("\n")
            .append("  -->")
            .append("\n\n");
         xmi.write(comments.toString());
      }

      private void writePermissions(XWriter xmi) throws IOException {
         BPermissionGroupInfo[] permissionInfos = (BPermissionGroupInfo[])this.helper.pmod().getPermissions().getChildren(BPermissionGroupInfo.class);
         if (permissionInfos.length > 0) {
            XElem[] xNiagaraPermissionGroups = new XElem[PolicyType.values().length];

            for (BPermissionGroupInfo permissionInfo : permissionInfos) {
               PolicyType policyType = PolicyType.valueOf(permissionInfo.getPolicyType());
               if (xNiagaraPermissionGroups[policyType.ordinal()] == null) {
                  xNiagaraPermissionGroups[policyType.ordinal()] = new XElem("niagara-permission-groups");
                  xNiagaraPermissionGroups[policyType.ordinal()].addAttr("type", policyType.toString());
               }

               XElem xPermission = permissionInfo.generatePermissionGroupXElem();
               xNiagaraPermissionGroups[policyType.ordinal()].addContent(xPermission);
            }

            XElem xPermissions = new XElem("permissions");

            for (XElem typeElem : xNiagaraPermissionGroups) {
               if (typeElem != null) {
                  xPermissions.addContent(typeElem);
               }
            }

            xPermissions.write(xmi);
         }
      }
   }

   static class WritePaletteStep extends BuildStep {
      @Override
      protected void doStep() throws Exception {
         File palette = new File(this.helper.getModuleDir(), "module.palette");
         XWriter xout = new XWriter(palette);
         xout.prolog();
         XElem graph = new XElem("bajaObjectGraph").addAttr("version", "1.0");
         XElem folder = new XElem("p").addAttr("m", "b=baja").addAttr("t", "b:UnrestrictedFolder");
         BProgram[] programs = this.helper.pmod().listPrograms();
         SortUtil.sort(programs, programs, (o1, o2) -> {
            String n1 = BuildHelper.toTypeName(o1);
            String n2 = BuildHelper.toTypeName(o2);
            return n1.compareTo(n2);
         });

         for (BProgram program : programs) {
            folder.addContent(
               new XElem("p")
                  .addAttr("n", BuildHelper.toTypeName(program))
                  .addAttr("m", this.helper.getPreferredSymbol() + "=" + this.helper.pmod().getModuleName())
                  .addAttr("t", this.helper.getPreferredSymbol() + ":" + BuildHelper.toTypeName(program))
            );
         }

         graph.addContent(folder);
         graph.write(xout);
         xout.close();
         this.interest.nextStep(BuildHelper.lex.getText("programModule.lexicon"));
      }
   }
}
