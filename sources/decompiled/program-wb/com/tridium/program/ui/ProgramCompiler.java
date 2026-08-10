package com.tridium.program.ui;

import com.tridium.program.BProgramCode;
import com.tridium.program.module.BProgramModule;
import com.tridium.program.ui.module.BuildException;
import com.tridium.program.ui.module.BuildHelper;
import com.tridium.program.ui.module.IBuildListener;
import com.tridium.workbench.console.BConsole;
import java.io.FileWriter;
import javax.baja.sys.BComplex;
import javax.baja.ui.BWidget;

public class ProgramCompiler extends Compiler {
   BProgramEditor editor;
   boolean saveOnSuccess;
   boolean buildOnSuccess;

   public ProgramCompiler(BProgramEditor editor) {
      super(editor);
      this.editor = editor;
   }

   public void compile(String className, boolean saveOnSuccess) throws Exception {
      this.compile(className, saveOnSuccess, false);
   }

   public void compile(String className, boolean saveOnSuccess, boolean buildOnSuccess) throws Exception {
      this.saveOnSuccess = saveOnSuccess;
      this.buildOnSuccess = buildOnSuccess;
      this.editor.saveCode();
      this.editor.imports.updateImports();
      this.editor.imports.save(this.editor.code);
      this.editor.updateSource();
      this.compile(className, this.editor.code, null);
   }

   @Override
   public void writeSource(FileWriter out, String source) throws Exception {
      SourceWriter writer = new SourceWriter(out, this.editor);
      writer.generate(this.className);
   }

   @Override
   public void compileSuccess(BConsole console) throws Exception {
      super.compileSuccess(console);
      BProgramCode code = this.editor.code;
      code.setChecksum(this.editor.computeChecksum(code.getSource()));
      this.editor.updateProgramCode();
      this.editor.updateStatusSuccess();
      if (this.saveOnSuccess) {
         this.editor.getWbShell().getSaveCommand().invoke();
      }

      if (this.buildOnSuccess) {
         this.buildProgramModule(console);
      }
   }

   @Override
   public void handleSigningFailed(Exception e, BConsole console) {
      this.editor.code.setChecksum(0);
      this.editor.updateStatusToSigningError();
      super.handleSigningFailed(e, console);
   }

   @Override
   public void compileFailed(BConsole console) throws Exception {
      this.editor.setModified();
      this.editor.code.setChecksum(0);
      this.editor.updateStatusToError();
      super.compileFailed(console);
   }

   private void buildProgramModule(BConsole console) {
      BProgramModule module = null;
      BComplex p = this.editor.program.getParent();

      while (p != null) {
         if (p instanceof BProgramModule) {
            module = (BProgramModule)p;
         } else {
            p = p.getParent();
         }
      }

      if (module != null) {
         try {
            console.appendBreak();
            BuildHelper bh = new BuildHelper(module);
            bh.buildModule(new ProgramCompiler.BuildConsoleListener(console));
         } catch (BuildException var5) {
            console.appendLine(var5.getMessage());
         }
      }
   }

   private class BuildConsoleListener implements IBuildListener {
      private BConsole console;

      public BuildConsoleListener(BConsole console) {
         this.console = console;
      }

      @Override
      public BWidget getOwner() {
         return this.console;
      }

      @Override
      public void nextStep(String desc) {
         this.console.appendLine(desc);
      }

      @Override
      public void setNumBuildSteps(int steps) {
      }

      @Override
      public void updateDesc(String desc) {
         this.console.appendLine(desc);
      }
   }
}
