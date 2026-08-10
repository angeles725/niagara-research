package com.tridium.program.ui;

import com.tridium.crypto.core.cert.SigningUtil;
import com.tridium.crypto.core.io.CoreCryptoManager;
import com.tridium.nre.security.SecurityInitializer;
import com.tridium.nre.util.NiagaraFiles;
import com.tridium.nsh.NShell;
import com.tridium.program.BCode;
import com.tridium.program.MacroProperties;
import com.tridium.program.ui.signing.BCertificateNotSelectedDialog;
import com.tridium.program.ui.signing.BCodeSigningOptions;
import com.tridium.sys.Nre;
import com.tridium.workbench.console.BConsole;
import com.tridium.workbench.console.BConsole.ExecCallback;
import com.tridium.workbench.shell.BNiagaraWbShell;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.PrintStream;
import java.security.AccessController;
import java.security.KeyStore;
import java.security.PrivilegedAction;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import javax.baja.naming.SlotPath;
import javax.baja.nre.platform.RuntimeProfile;
import javax.baja.registry.DependencyInfo;
import javax.baja.registry.ModuleInfo;
import javax.baja.sys.BBlob;
import javax.baja.sys.LocalizableException;
import javax.baja.sys.ModuleNotFoundException;
import javax.baja.sys.Property;
import javax.baja.sys.SlotCursor;
import javax.baja.sys.Sys;
import javax.baja.ui.BWidget;
import javax.baja.util.Lexicon;

public class Compiler implements ExecCallback {
   BWidget owner;
   public String className;
   File temp;
   File java;
   File classFile;
   File[] innerClassFiles;
   BCode code;
   boolean reportErrors = false;
   boolean signingEnabled = true;
   static final Logger log = Logger.getLogger("sys.program");
   protected static Lexicon lex = Lexicon.make("program");

   public Compiler(BWidget owner) {
      this.owner = owner;
   }

   public void compile(String className, BCode code, String source) throws Exception {
      this.className = className;
      this.temp = new File(Sys.getNiagaraUserHome(), "temp");
      this.java = new File(this.temp, className + ".java");
      this.classFile = new File(this.temp, className + ".class");
      this.innerClassFiles = null;
      this.code = code;
      File fTemp = this.temp;
      AccessController.doPrivileged((PrivilegedAction)(() -> {
         if (!fTemp.exists()) {
            fTemp.mkdirs();
         }

         return null;
      }));

      try (FileWriter out = new FileWriter(this.java)) {
         this.writeSource(out, source);
      }

      String sep = File.separator;
      String pathSep = File.pathSeparator;
      StringBuilder cp = new StringBuilder();
      File binDir = new File(Nre.getNiagaraHome(), "bin");
      File binExtDir = new File(binDir, "ext");
      String[] binJars = binExtDir.list(new Compiler.JarFilenameFilter());
      String binMbase = Nre.getNiagaraHome() + sep + "bin" + sep + "ext" + sep;

      for (String binJar : binJars) {
         cp.append(binMbase).append(binJar).append(pathSep);
      }

      String mbase = NiagaraFiles.getModulesPath().getPath() + sep;
      cp.append(mbase).append("baja.jar").append(pathSep);
      String[] depends = this.parseDependencies(code);

      for (String depend : depends) {
         if (!depend.equals("baja")) {
            cp.append(mbase).append(depend).append(".jar").append(pathSep);
         }
      }

      String profile = getCompactProfile(depends);

      try {
         String cmd = getCompileJavaCommand(profile, cp.toString(), this.temp.toString(), this.java.toString());
         this.exec(cmd);
      } catch (Exception var26) {
         if (this.reportErrors) {
            throw var26;
         }

         System.out.println("Problem loading 'javac' command.");
         var26.printStackTrace();
      }
   }

   protected String[] parseDependencies(BCode code) throws Exception {
      Set<String> result = new HashSet<>();
      String[] deps = code.parseDependencies();

      for (String dep : deps) {
         String modulePartName = null;

         try {
            ModuleInfo mInfo = Sys.getRegistry().moduleForDependency(dep);
            modulePartName = mInfo.getModulePartName();
         } catch (ModuleNotFoundException var10) {
         }

         if (modulePartName != null) {
            this.accumDependencies(result, modulePartName);
         }
      }

      return result.toArray(new String[0]);
   }

   protected void accumDependencies(Set<String> acc, String modulePartName) throws Exception {
      if (!acc.contains(modulePartName)) {
         acc.add(modulePartName);
         DependencyInfo[] deps = Sys.getRegistry().moduleForDependency(modulePartName).getDependencies();

         for (DependencyInfo dep : deps) {
            this.accumDependencies(acc, dep.getModulePartName());
         }
      }
   }

   public void writeSource(FileWriter out, String source) throws Exception {
      out.write(source);
   }

   public void exec(String cmd) throws Exception {
      if (this.owner != null) {
         this.openConsole().exec(cmd, this);
      } else if (this.reportErrors) {
         ByteArrayOutputStream bos = new ByteArrayOutputStream();
         PrintStream out = new PrintStream(bos);
         NShell shell = new NShell(out);
         shell.exec(cmd);
         shell.execWaitUntilDone();
         String compileRes = bos.toString();
         if (compileRes.indexOf("error") > 0) {
            this.cleanup();
            throw new CompilerException(compileRes);
         }

         this.compileSuccess(null);
         this.cleanup();
      } else {
         NShell shell = new NShell(System.out);
         shell.exec(cmd);
         shell.execWaitUntilDone();
         this.compileSuccess(null);
         this.cleanup();
      }
   }

   public BConsole openConsole() {
      return this.owner.getShell() != null ? ((BNiagaraWbShell)this.owner.getShell()).openConsole() : null;
   }

   public void consoleExecDone(BConsole console, int exitCode) {
      try {
         if (exitCode == 0) {
            this.compileSuccess(console);
         } else {
            this.compileFailed(console);
         }
      } catch (Throwable var4) {
         var4.printStackTrace();
      }

      this.cleanup();
   }

   public void compileSuccess(BConsole console) throws Exception {
      BCode temp = new BCode();
      byte[] buf = new byte[(int)this.classFile.length()];

      try (DataInputStream classIn = AccessController.doPrivileged(
            (PrivilegedExceptionAction<DataInputStream>)(() -> new DataInputStream(new BufferedInputStream(new FileInputStream(this.classFile))))
         )) {
         classIn.readFully(buf);
      }

      temp.setClassName(this.className);
      temp.setClassFile(BBlob.make(buf));
      File[] tempFiles = this.classFile.getParentFile().listFiles();
      List<File> innerArray = new ArrayList<>();

      for (int i = 0; i < (tempFiles != null ? tempFiles.length : 0); i++) {
         if (tempFiles[i].getName().startsWith(this.className + '$')) {
            innerArray.add(tempFiles[i]);
            String innerClassName = tempFiles[i].getName();
            innerClassName = innerClassName.substring(0, innerClassName.length() - 6);
            buf = new byte[(int)tempFiles[i].length()];
            File file = tempFiles[i];

            try (DataInputStream tempIn = AccessController.doPrivileged(
                  (PrivilegedExceptionAction<DataInputStream>)(() -> new DataInputStream(new BufferedInputStream(new FileInputStream(file))))
               )) {
               tempIn.readFully(buf);
            }

            temp.addInnerClassFile(innerClassName, BBlob.make(buf));
         }
      }

      this.innerClassFiles = innerArray.toArray(new File[0]);

      try {
         if (this.signingEnabled) {
            signCode(temp, console);
         }
      } catch (Exception var37) {
         this.handleSigningFailed(var37, console);
         throw var37;
      }

      this.code.setClassName(temp.getClassName());
      this.code.setClassFile(temp.getClassFile());
      this.code.setSignature(temp.getSignature());
      this.code.clearInnerClassFiles();
      SlotCursor<Property> c = temp.getProperties();

      while (c.next(BBlob.class)) {
         if (c.property().isDynamic()) {
            this.code.add(c.property().getName(), c.get(), 1);
         }
      }
   }

   public static void signCode(BCode code, BWidget owner) throws Exception {
      BCodeSigningOptions options = BCodeSigningOptions.make();
      String alias = options.getSigningCert();
      if ((alias == null || alias.isEmpty()) && AccessController.doPrivileged((PrivilegedAction<Boolean>)(() -> Boolean.getBoolean("program.requireSigning")))) {
         CoreCryptoManager ccm = AccessController.doPrivileged((PrivilegedAction<CoreCryptoManager>)(() -> {
            try {
               return CoreCryptoManager.get(SecurityInitializer.getInstance().getSecurityInfoProvider());
            } catch (Exception var1x) {
               return null;
            }
         }));
         String newAlias = BCertificateNotSelectedDialog.show(owner);
         if (newAlias != null && !newAlias.isEmpty()) {
            options.setSigningCert(newAlias);
            alias = newAlias;
         }
      }

      char[] keyPassword = null;
      if (alias != null && !alias.isEmpty()) {
         try {
            keyPassword = AccessController.doPrivileged((PrivilegedAction<char[]>)(() -> {
               try {
                  return options.getKeyPassword(owner);
               } catch (Exception var3x) {
                  throw new RuntimeException(var3x);
               }
            }));
         } catch (RuntimeException var6) {
            if (var6.getCause() instanceof Exception) {
               throw (Exception)var6.getCause();
            }

            throw var6;
         }
      }

      String tsaUrl = options.getTsaUrl();
      signCode(code, alias, keyPassword, tsaUrl);
   }

   public static void signCode(BCode code, String alias, char[] keyPassword, String tsaUrl) throws Exception {
      byte[] buf = code.getClassFile().copyBytes();
      if ((alias == null || alias.equals(""))
         && !AccessController.doPrivileged((PrivilegedAction<Boolean>)(() -> Boolean.getBoolean("program.requireSigning")))) {
         code.setSignature(BBlob.DEFAULT);
         code.clearInnerClassSignatures();
         log.warning(lex.getText("program.willNotSign"));
      } else if (alias != null && !alias.equals("")) {
         CoreCryptoManager ccm = AccessController.doPrivileged((PrivilegedAction<CoreCryptoManager>)(() -> {
            try {
               return CoreCryptoManager.get(SecurityInitializer.getInstance().getSecurityInfoProvider());
            } catch (Exception var1x) {
               return null;
            }
         }));
         BBlob signature = BBlob.make(SigningUtil.generateSignature(buf, alias, tsaUrl, (char[])keyPassword.clone(), ccm.getKeyStore().getKeyStore()));
         code.setSignature(signature);
         SlotCursor<Property> c = code.getProperties();

         while (c.next(BBlob.class)) {
            if (c.property().isDynamic() && !SlotPath.unescape(c.property().getName()).endsWith("#sig")) {
               byte[] innerBytes = ((BBlob)c.get()).copyBytes();
               signature = BBlob.make(SigningUtil.generateSignature(innerBytes, alias, tsaUrl, (char[])keyPassword.clone(), ccm.getKeyStore().getKeyStore()));
               code.addInnerClassSignature(SlotPath.unescape(c.property().getName()), signature);
            }
         }

         if (tsaUrl == null || tsaUrl.isEmpty()) {
            log.warning(lex.getText("program.willNotTimestamp"));
         }
      } else {
         throw new Exception(lex.getText("program.certNotSelected"));
      }
   }

   public static void checkSigningKey(String alias, char[] password) throws LocalizableException {
      CoreCryptoManager ccm = AccessController.doPrivileged((PrivilegedAction<CoreCryptoManager>)(() -> {
         try {
            return CoreCryptoManager.get(SecurityInitializer.getInstance().getSecurityInfoProvider());
         } catch (Exception var1x) {
            return null;
         }
      }));

      try {
         KeyStore keystore = ccm.getKeyStore().getKeyStore();
         SigningUtil.checkKey(keystore, alias, (char[])password.clone());
      } catch (Exception var4) {
         throw new LocalizableException("program", "program.couldNotSign", new Object[]{var4.getMessage()});
      }
   }

   public void handleSigningFailed(Exception e, BConsole console) {
      console.appendLine(lex.getText("program.couldNotSign", new Object[]{e}));
      console.showLast();
   }

   public void compileFailed(BConsole console) throws Exception {
      console.next();
   }

   public void cleanup() {
      final File[] fInnerClassFiles;
      if (this.innerClassFiles != null) {
         fInnerClassFiles = new File[this.innerClassFiles.length];
         System.arraycopy(this.innerClassFiles, 0, fInnerClassFiles, 0, this.innerClassFiles.length);
      } else {
         fInnerClassFiles = new File[0];
      }

      final File fClassFile = this.classFile;
      final File fJava = this.java;
      AccessController.doPrivileged(new PrivilegedAction<Object>() {
         @Override
         public Object run() {
            fClassFile.delete();

            for (File fInnerClassFile : fInnerClassFiles) {
               fInnerClassFile.delete();
            }

            fJava.delete();
            return null;
         }
      });
   }

   public static String getCompactProfile(String[] depends) {
      RuntimeProfile moduleProfile = RuntimeProfile.rt;

      for (String name : depends) {
         try {
            ModuleInfo mInfo = Sys.getRegistry().moduleForDependency(name);
            RuntimeProfile runtime = mInfo.getRuntimeProfile();
            if (runtime.compareTo(moduleProfile) > 0) {
               moduleProfile = runtime;
            }
         } catch (ModuleNotFoundException var8) {
         }
      }

      return moduleProfile.compareTo(RuntimeProfile.ux) >= 0 ? "" : "compact3";
   }

   public static String getCompileJavaCommand(String profile, String classPath, String destDir, String javaSrcFile) throws Exception {
      if (!javaSrcFile.startsWith("\"")) {
         javaSrcFile = "\"" + javaSrcFile;
      }

      if (!javaSrcFile.endsWith("\"")) {
         javaSrcFile = javaSrcFile + "\"";
      }

      String jdkHome = AccessController.doPrivileged((PrivilegedAction<String>)(() -> System.getProperty("java.home")));
      String javacMacro = "\""
         + jdkHome
         + File.separator
         + "bin"
         + File.separator
         + "javac\" -encoding UTF-8 %javac.profile% -Xlint:deprecation -classpath \"%javac.classpath%\" -d \"%javac.out%\" %javac.src%";
      MacroProperties javac = new MacroProperties();
      javac.put("javac.classpath", classPath);
      javac.put("javac.out", destDir);
      javac.put("javac.src", javaSrcFile);
      if (profile != null && !profile.equals("")) {
         javac.put("javac.profile", "-profile " + profile);
      } else {
         javac.put("javac.profile", "");
      }

      javac.put("javac.cmd", javacMacro);
      String cmd = javac.resolve("javac.cmd");
      if (!cmd.endsWith(javaSrcFile)) {
         int idx = cmd.lastIndexOf(javaSrcFile);
         if (idx > -1) {
            cmd = cmd.substring(0, idx);
         } else {
            cmd = cmd + " ";
         }

         cmd = cmd + javaSrcFile;
      }

      return cmd;
   }

   public void setReportErrors(boolean b) {
      this.reportErrors = b;
   }

   public void setSignignEnabled(boolean enabled) {
      this.signingEnabled = enabled;
   }

   public static class JarFilenameFilter implements FilenameFilter {
      @Override
      public boolean accept(File dir, String name) {
         return name.endsWith(".jar");
      }
   }
}
