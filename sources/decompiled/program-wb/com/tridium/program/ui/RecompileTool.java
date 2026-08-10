package com.tridium.program.ui;

import com.tridium.file.types.bog.BBogFile;
import com.tridium.program.BCode;
import com.tridium.program.BProgram;
import com.tridium.program.BProgramCode;
import com.tridium.program.ui.signing.BCodeSigningOptions;
import java.io.Console;
import java.io.File;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.baja.file.BFileSystem;
import javax.baja.file.BLocalFileStore;
import javax.baja.space.BComponentSpace;
import javax.baja.sys.BComponent;
import javax.baja.sys.LocalizableException;
import javax.baja.sys.Property;
import javax.baja.util.Lexicon;

public class RecompileTool {
   private final String alias;
   private final String tsaUrl;
   private final char[] password;
   protected static Lexicon lex = Lexicon.make("program");
   static final Logger log = Logger.getLogger("program");

   public static void main(String[] args) {
      List<String> argList = new ArrayList<>();
      Map<String, String> optionsMap = new HashMap<>();

      for (String arg : args) {
         if (arg.startsWith("-")) {
            String key = arg.substring(1);
            String value = null;
            if (arg.contains(":")) {
               int colon = key.indexOf(58);
               value = key.substring(colon + 1);
               key = key.substring(0, colon);
            }

            optionsMap.put(key.toLowerCase(), value);
         } else {
            argList.add(arg);
         }
      }

      if (argList.size() < 1) {
         usage();
      } else {
         File f = new File(argList.get(0));
         if (!f.exists()) {
            System.out.println("File not found: " + f);
         } else {
            String alias = optionsMap.get("alias");
            String tsaUrl = optionsMap.get("tsaurl");
            char[] password = null;
            if (optionsMap.get("password") != null) {
               password = optionsMap.get("password").toCharArray();
            }

            BCodeSigningOptions options = BCodeSigningOptions.make();
            if (alias == null || alias.isEmpty()) {
               alias = options.getSigningCert();
            }

            if (tsaUrl == null) {
               tsaUrl = options.getTsaUrl();
            }

            if (password == null && alias != null && !alias.isEmpty()) {
               Console console = AccessController.doPrivileged((PrivilegedAction<Console>)(() -> System.console()));
               password = console.readPassword(lex.getText("program.certPassword.description", new Object[]{alias}));
            }

            if (alias != null && !alias.isEmpty()) {
               try {
                  Compiler.checkSigningKey(alias, password);
               } catch (LocalizableException var10) {
                  log.severe(var10.getLocalizedMessage());
                  return;
               }
            } else {
               log.warning(lex.getText("program.willNotSign"));
            }

            new RecompileTool(alias, tsaUrl, password).process(f);
            System.exit(0);
         }
      }
   }

   public static void usage() {
      System.out.println();
      System.out.println("usage:");
      System.out.println("  RecompileTool <dir|bogfile> [flags]");
      System.out.println();
      System.out.println("parameters:");
      System.out.println("  dir|bogfile        Directory containing bog file(s) or individual bog file.");
      System.out.println();
      System.out.println("optional flags:");
      System.out.println("  -alias:<arg>       Alias of a code signing certificate in user key store to");
      System.out.println("                     sign programs with. Defaults to workbench code signing");
      System.out.println("                     options if not provided.");
      System.out.println();
      System.out.println("  -password:<arg>    Private key password for the signing certificate. Will be");
      System.out.println("                     prompted if not provided.");
      System.out.println();
      System.out.println("  -tsaUrl:<arg>      Time stamp authority url to use for timestamping program");
      System.out.println("                     signatures. Defaults to workbench code signing options");
      System.out.println("                     if not provided.");
   }

   public RecompileTool(String alias, String tsaUrl, char[] password) {
      this.alias = alias;
      this.tsaUrl = tsaUrl;
      this.password = password;
   }

   public void process(File f) {
      if (!f.isDirectory()) {
         if (f.getName().endsWith(".bog")) {
            this.processBog(f);
         }
      } else {
         File[] kids = f.listFiles();

         for (int i = 0; kids != null && i < kids.length; i++) {
            this.process(kids[i]);
         }
      }
   }

   public void processBog(File f) {
      System.out.println("--- Processing " + f + "...");

      try {
         BBogFile bog = new BBogFile(new BLocalFileStore(BFileSystem.INSTANCE, BFileSystem.INSTANCE.localFileToPath(f), f));
         BComponentSpace space = (BComponentSpace)bog.open();

         try {
            BComponent root = space.getRootComponent();
            this.process(root);
         } finally {
            if (bog.isOpen()) {
               bog.save();
               bog.close();
            }
         }

         System.out.println("--- Processed " + f + "!");
      } catch (Exception var9) {
         System.out.println("ERROR: Cannot process " + f);
         var9.printStackTrace();
      }
   }

   public void process(BComponent c) throws Exception {
      if (c instanceof BProgram) {
         System.out.println("  Recompiling " + toString(c));

         try {
            this.recompile((BProgram)c);
         } catch (Exception var7) {
            System.out.println("ERROR Recompiling " + toString(c));
            if (var7 instanceof CompilerException) {
               System.out.println(var7.getMessage());
            } else {
               var7.printStackTrace();
            }
         }
      }

      BComponent[] kids = c.getChildComponents();

      for (BComponent kid : kids) {
         this.process(kid);
      }
   }

   public void recompile(BProgram program) throws Exception {
      String className = BCode.generateClassName();
      BProgramEditor editor = new BProgramEditor();
      editor.loadValue(program);
      char[] src = SourceWriter.generateToCharArray(editor, className);
      BProgramCode code = (BProgramCode)program.getCode().newCopy();
      Compiler compiler = new Compiler(null);
      compiler.setReportErrors(true);
      compiler.setSignignEnabled(false);
      compiler.compile(className, code, new String(src));
      if (this.alias != null && !this.alias.isEmpty()) {
         Compiler.signCode(code, this.alias, this.password, this.tsaUrl);
      }

      program.setCode(code);
   }

   static String toString(BComponent c) {
      List<String> path = new ArrayList<>();

      while (true) {
         Property prop = c.getPropertyInParent();
         if (prop == null) {
            StringBuilder s = new StringBuilder();

            for (String aPath : path) {
               s.append("/").append(aPath);
            }

            return s.toString();
         }

         path.add(0, prop.getName());
         c = (BComponent)c.getParent();
      }
   }
}
