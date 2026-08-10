package com.tridium.program.ui;

import com.tridium.workbench.shell.BNiagaraWbShell;
import javax.baja.ui.BDialog;
import javax.baja.ui.BMenu;
import javax.baja.ui.BSeparator;
import javax.baja.ui.BToolBar;
import javax.baja.ui.Command;
import javax.baja.ui.CommandArtifact;
import javax.baja.ui.util.UiLexicon;

public class RobotEditorCommands {
   static UiLexicon lex = UiLexicon.bajaui();
   public final BRobotEditor editor;
   public final Command compile;
   public final Command compileAndRun;

   public RobotEditorCommands(BRobotEditor editor) {
      this.editor = editor;
      this.compileAndRun = new RobotEditorCommands.CompileAndRunCommand();
      this.compile = new RobotEditorCommands.CompileCommand();
   }

   public BMenu[] getViewMenus() {
      BMenu menu = UiLexicon.bajaui().buildMenu("robotEditor.menu.label");
      menu.add("compile", this.compile);
      menu.add("run", this.compileAndRun);
      return new BMenu[]{menu};
   }

   public BToolBar getViewToolBar() {
      BNiagaraWbShell shell = (BNiagaraWbShell)this.editor.getShell();
      BToolBar bar = new BToolBar();
      if (shell != null) {
         bar.add("find", shell.commands.find);
         bar.add("replace", shell.commands.replace);
      }

      bar.add("sep1", new BSeparator());
      bar.add("compile", this.compile);
      bar.add("run", this.compileAndRun);
      bar.add("sep2", new BSeparator());
      if (shell != null) {
         bar.add("consolePrev", shell.commands.consolePrev);
         bar.add("consoleNext", shell.commands.consoleNext);
      }

      return bar;
   }

   public class CompileAndRunCommand extends RobotEditorCommands.RobotEditorCommand {
      public CompileAndRunCommand() {
         super("robotEditor.compileAndRun");
      }

      @Override
      public CommandArtifact doInvoke() throws Exception {
         RobotEditorCommands.this.editor.compiler.compile(true);
         return null;
      }
   }

   public class CompileCommand extends RobotEditorCommands.RobotEditorCommand {
      public CompileCommand() {
         super("robotEditor.compile");
      }

      @Override
      public CommandArtifact doInvoke() throws Exception {
         RobotEditorCommands.this.editor.compiler.compile(false);
         return null;
      }
   }

   public class RobotEditorCommand extends Command {
      public RobotEditorCommand(String keyBase) {
         super(RobotEditorCommands.this.editor, UiLexicon.bajaui().module, keyBase);
      }

      public CommandArtifact doInvoke() throws Exception {
         BDialog.message(RobotEditorCommands.this.editor, "Incomplete: " + this.getLabel());
         return null;
      }
   }
}
