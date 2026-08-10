package com.tridium.program.ui;

import com.tridium.crypto.core.cert.NX509Certificate;
import com.tridium.program.BCode;
import com.tridium.program.BProgram;
import com.tridium.program.BProgramService;
import com.tridium.program.BRobotCode;
import com.tridium.program.BRobotResult;
import com.tridium.program.ui.signing.BCertificateNotTrustedDialog;
import com.tridium.workbench.console.BConsole;
import com.tridium.workbench.console.BConsole.HyperlinkInterceptor;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;
import javax.baja.nre.annotations.AgentOn;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.platform.RuntimeProfile;
import javax.baja.nre.util.TextUtil;
import javax.baja.registry.DependencyInfo;
import javax.baja.sys.BBlob;
import javax.baja.sys.BObject;
import javax.baja.sys.Context;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.ui.BDialog;
import javax.baja.ui.BMenu;
import javax.baja.ui.BToolBar;
import javax.baja.ui.CommandArtifact;
import javax.baja.ui.pane.BTextEditorPane;
import javax.baja.ui.text.BTextEditor;
import javax.baja.ui.text.Position;
import javax.baja.ui.text.parsers.JavaParser;
import javax.baja.util.Lexicon;
import javax.baja.workbench.view.BWbComponentView;

@NiagaraType(
   agent = {@AgentOn(
      types = {"program:ProgramService"}
   )}
)
public class BRobotEditor extends BWbComponentView implements HyperlinkInterceptor {
   public static final Type TYPE = Sys.loadType(BRobotEditor.class);
   static String defaultDepends = "baja; program";
   static String defaultCode = "// depends: "
      + defaultDepends
      + "\n\nimport javax.baja.sys.*;\nimport com.tridium.program.*;\n\npublic class RobotImpl\n  extends Robot\n{\n\n  public void run()\n    throws Exception\n  {\n    process(Sys.getStation());\n  }\n\n  public void process(BComponent c)\n    throws Exception\n  {\n    // do something here\n    log.println(c.toPathString() + \" [\" + c.getType() + \"]\");\n    \n    // recurse\n    BComponent[] kids = c.getChildComponents();\n    for(int i=0; i<kids.length; ++i)\n      process(kids[i]);\n  }\n\n}\n";
   static Lexicon lex = Lexicon.make("program");
   RobotEditorCommands commands = new RobotEditorCommands(this);
   BProgramService service;
   BTextEditor editor;
   BRobotEditor.RobotCompiler compiler = new BRobotEditor.RobotCompiler(this);
   BRobotCode robotCode = new BRobotCode();

   public Type getType() {
      return TYPE;
   }

   public BRobotEditor() {
      this.editor = new BTextEditor();
      this.editor.setParser(new JavaParser());
      this.editor.setText(defaultCode);
      this.setTransferWidget(this.editor);
      this.setCommandEnabled(6, true);
      this.setCommandEnabled(8, true);
      this.setCommandEnabled(7, true);
      this.setCommandEnabled(9, true);
      this.setCommandEnabled(10, true);
      BTextEditorPane editorPane = new BTextEditorPane(this.editor);
      this.setContent(editorPane);
   }

   public BMenu[] getViewMenus() {
      return this.commands.getViewMenus();
   }

   public BToolBar getViewToolBar() {
      return this.commands.getViewToolBar();
   }

   public void doLoadValue(BObject value, Context cx) {
      this.service = (BProgramService)value;
   }

   public void deactivated() {
      super.deactivated();
      defaultCode = this.editor.getText();
   }

   public CommandArtifact invokeCommand(int id) throws Exception {
      switch (id) {
         case 6:
            return this.editor.doFind();
         case 7:
            return this.editor.doFindPrev();
         case 8:
            return this.editor.doFindNext();
         case 9:
            return this.editor.doReplace();
         case 10:
            return this.editor.doGoto();
         default:
            return super.invokeCommand(id);
      }
   }

   public boolean consoleHyperlink(File file, int line1, int col1, int line2, int col2) {
      if (!file.equals(this.compiler.java)) {
         return false;
      } else {
         this.highlight(line1, col1, line2, col2);
         return true;
      }
   }

   public void highlight(int line1, int col1, int line2, int col2) {
      this.highlight(this.editor, line1, col1, line2, col2);
   }

   void highlight(BTextEditor text, int line1, int col1, int line2, int col2) {
      Position end = text.getModel().getEndPosition();
      Position p1 = new Position(line1 - 1, col1 - 1);
      Position p2 = new Position(line2 - 1, col2);
      if (p1.compareTo(end) > 0) {
         p1 = end;
      }

      if (p2.compareTo(end) > 0) {
         p2 = end;
      }

      text.getSelection().select(p1, p2);
      text.moveCaretPosition(p2);
      text.requestFocus();
   }

   public void run() {
      BRobotResult r = this.service.runRobot(this.robotCode);
      BBlob certBlob = r.getUntrustedCertificate();
      if (certBlob.length() > 0) {
         try {
            byte[] certBytes = certBlob.copyBytes();

            try (InputStream inputBytes = new ByteArrayInputStream(certBytes)) {
               CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
               X509Certificate x509Certificate = (X509Certificate)certFactory.generateCertificate(inputBytes);
               NX509Certificate cert = NX509Certificate.make(x509Certificate);
               if (BCertificateNotTrustedDialog.installUntrustedCertificate(cert, this, this.service)) {
                  this.run();
               } else {
                  BDialog.error(this, lex.getText("robot.certNotTrusted"));
               }
            }
         } catch (Exception var19) {
            BDialog.error(this, BDialog.TITLE_ERROR, lex.getText("program.couldNotInstallCert"), var19);
         }
      } else {
         BTextEditor out = new BTextEditor(r.getLog(), false);
         BTextEditorPane pane = new BTextEditorPane(out, 20, 80);
         BDialog.open(this, "Log Output", pane, 1);
      }
   }

   public String parseDepends(String text) throws Exception {
      if (text.startsWith("// depends:")) {
         int colon = text.indexOf(58);
         int nl = text.indexOf(10);
         if (colon >= 0 && nl >= 0) {
            Set<String> deps = new HashSet<>();
            StringTokenizer st = new StringTokenizer(text.trim().substring(colon + 1, nl), ";");

            while (st.hasMoreTokens()) {
               String name = st.nextToken().trim();
               if (name.length() > 0 && !BProgram.isSpecialModule(name)) {
                  this.accumDepends(deps, Sys.getRegistry().getModule(name, RuntimeProfile.rt).getModulePartName());
               }
            }

            StringBuilder result = new StringBuilder();

            for (String dep : deps) {
               if (result.length() > 0) {
                  result.append("; ");
               }

               result.append(dep);
            }

            return result.toString();
         }
      }

      return defaultDepends;
   }

   protected void accumDepends(Set<String> acc, String modulePartName) throws Exception {
      if (!acc.contains(modulePartName)) {
         acc.add(modulePartName);
         DependencyInfo[] deps = Sys.getRegistry().moduleForDependency(modulePartName).getDependencies();

         for (DependencyInfo dep : deps) {
            this.accumDepends(acc, dep.getModulePartName());
         }
      }
   }

   class RobotCompiler extends Compiler {
      boolean runOnSuccess;

      RobotCompiler(BRobotEditor editor) {
         super(editor);
      }

      public void compile(boolean runOnSuccess) throws Exception {
         this.runOnSuccess = runOnSuccess;
         String source = BRobotEditor.this.editor.getText();
         String className = BCode.generateClassName();
         int x = source.indexOf("RobotImpl");
         if (x < 0) {
            throw new Exception("Class name must be RobotImpl");
         } else {
            source = TextUtil.replace(source, "RobotImpl", className);
            BRobotEditor.this.robotCode.setDependencies(BRobotEditor.this.parseDepends(source));
            this.compile(className, BRobotEditor.this.robotCode, source);
         }
      }

      @Override
      public void compileSuccess(BConsole console) throws Exception {
         super.compileSuccess(console);
         if (this.runOnSuccess) {
            BRobotEditor.this.run();
         }
      }
   }
}
