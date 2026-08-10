package com.tridium.workbench.console;

import com.tridium.nsh.NShell;
import com.tridium.ui.theme.Theme;
import com.tridium.workbench.shell.BNiagaraWbShell;
import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Action;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.ui.BDialog;
import javax.baja.ui.BLabel;
import javax.baja.ui.BScrollBar;
import javax.baja.ui.event.BMouseWheelEvent;
import javax.baja.ui.event.BScrollEvent;
import javax.baja.ui.pane.BEdgePane;
import javax.baja.ui.util.UiLexicon;
import javax.baja.workbench.view.BWbView;

@NiagaraType
@NiagaraAction(
   name = "scrolled",
   parameterType = "BScrollEvent",
   defaultValue = "new BScrollEvent()"
)
public class BConsole extends BEdgePane {
   public static final Action scrolled = newAction(0, new BScrollEvent(), null);
   public static final Type TYPE = Sys.loadType(BConsole.class);
   public static final int BUFFER_SIZE = 500;
   BNiagaraWbShell shell;
   BLabel prompt;
   BConsoleEntry entry;
   BScrollBar scrollBar;
   BConsoleBuffer buffer;
   BConsole.ExecCallback execCallback;
   NShell nsh;
   ArrayList<String> history = new ArrayList<>();
   int historyPosition = -1;

   public void scrolled(BScrollEvent parameter) {
      this.invoke(scrolled, parameter, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BConsole() {
      throw new IllegalStateException();
   }

   public BConsole(BNiagaraWbShell shell) {
      this.shell = shell;
      this.entry = new BConsoleEntry(this);
      this.buffer = new BConsoleBuffer(this);
      this.nsh = new BConsole.ConsoleShell(new PrintStream(new ConsoleWriter(this)));
      this.scrollBar = new BScrollBar();
      this.prompt = new BLabel(">", Theme.textEditor().getFont());
      this.scrollBar.setMin(0);
      this.scrollBar.setMax(499);
      this.scrollBar.setExtent(10);
      this.scrollBar.setPosition(489);
      this.scrollBar.setUnitIncrement(1);
      this.linkTo("scrollLink", this.scrollBar, BScrollBar.positionChanged, scrolled);
      this.updatePrompt();
      this.setCenter(new BConsolePane(this.buffer, this.prompt, this.entry));
      this.setRight(this.scrollBar);
   }

   public String getStyleSelector() {
      return "console";
   }

   public void appendBreak() {
      this.buffer.append("", true);
   }

   public void appendLine(String line) {
      this.buffer.append(line, false);
   }

   public void show(int index) {
      String status = null;
      if (index == -1) {
         status = UiLexicon.bajaui().getText("noMore");
         if (this.buffer.highlight == -1) {
            this.showStatus(status);
            return;
         }

         index = this.buffer.highlight;
      }

      this.buffer.highlight = index;
      this.buffer.repaint();
      this.scrollToVisible(index);
      if (this.buffer.lines[index].isOrd()) {
         if (this.shell != null) {
            this.shell.hyperlink(((BConsoleBuffer.OrdLine)this.buffer.lines[index]).getHyperlinkInfo());
         }
      } else {
         BConsoleBuffer.Line line = this.buffer.lines[index];
         if (this.shell != null && line instanceof BConsoleBuffer.FileLine) {
            BConsoleBuffer.FileLine fileLine = (BConsoleBuffer.FileLine)line;
            BWbView view = this.shell.getActiveView();
            if (view instanceof BConsole.HyperlinkInterceptor) {
               BConsole.HyperlinkInterceptor interceptor = (BConsole.HyperlinkInterceptor)view;
               if (interceptor.consoleHyperlink(fileLine.file, fileLine.line1, fileLine.col1, fileLine.line2, fileLine.col2)) {
                  return;
               }
            }

            this.shell.hyperlink(fileLine.file, fileLine.line1, fileLine.col1, fileLine.line2, fileLine.col2);
         }

         if (status == null) {
            status = line.text;
         }

         this.showStatus(status);
      }
   }

   public void showLast() {
      this.show(this.buffer.lines.length - 1);
   }

   public void next() {
      this.show(this.buffer.next());
   }

   public void prev() {
      this.show(this.buffer.prev());
   }

   public void prime() {
      this.scrollToEnd();
      this.entry.requestFocus();
   }

   public void doScrolled(BScrollEvent event) {
      this.buffer.repaint();
   }

   public void scrollToEnd() {
      this.scrollBar.setPosition(500 - this.scrollBar.getExtent() - 1);
   }

   public void mouseWheel(BMouseWheelEvent event) {
      BScrollBar sb = this.scrollBar;
      if (sb.isVisible() && sb.getWidth() != 0.0) {
         sb.scrollByUnits(event.getPreciseWheelRotation());
         event.consume();
      }
   }

   public void scrollToVisible(int index) {
      int extent = this.scrollBar.getExtent();
      int start = this.scrollBar.getPosition();
      int end = start + extent;
      int count = this.scrollBar.getMax();
      if (index <= start) {
         int pos = index - 1;
         if (pos < 0) {
            pos = 0;
         }

         this.scrollBar.setPosition(pos);
      } else if (index >= end) {
         int pos = index - extent;
         if (pos >= count) {
            pos = count - 1;
         }

         if (pos < 0) {
            pos = 0;
         }

         this.scrollBar.setPosition(pos);
      }
   }

   public void nextCommand() {
      if (this.historyPosition < this.history.size() - 1) {
         this.historyPosition++;
         this.entry.setCommand(this.history.get(this.historyPosition));
      }
   }

   public void prevCommand() {
      if (this.historyPosition > 0) {
         this.historyPosition--;
         this.entry.setCommand(this.history.get(this.historyPosition));
      }
   }

   private void updateHistory(String cmd) {
      for (int i = 0; i < this.history.size(); i++) {
         if (cmd.equals(this.history.get(i))) {
            this.history.remove(i);
            break;
         }
      }

      this.history.add(cmd);
      this.historyPosition = this.history.size();
   }

   public void exec(String cmd) {
      this.exec(cmd, null);
   }

   public void exec(String cmd, BConsole.ExecCallback execCallback) {
      this.showStatus(null);
      this.updateHistory(cmd);
      this.entry.setCommand("");
      this.scrollToEnd();
      this.entry.getUndoManager().discardAllArtifacts();
      this.buffer.append(this.prompt.getText() + cmd, true);
      if (cmd.equalsIgnoreCase("cls")) {
         this.cls();
      } else {
         try {
            this.execCallback = execCallback;
            this.nsh.exec(cmd);
         } catch (Exception var4) {
            var4.printStackTrace();
            this.nsh.out().println("Command failed: " + var4);
         }

         this.shell.updateCommandStates(this.shell.getActiveView());
         this.updatePrompt();
      }
   }

   public void kill() {
      if (this.nsh.inExec()) {
         if (4 == BDialog.confirm(this, this.getLexicon().get("killConsoleCommand.warning.message"))) {
            this.nsh.execKill();
         }
      }
   }

   public boolean inExec() {
      boolean running = false;
      if (this.nsh != null) {
         running = this.nsh.inExec();
      }

      return running;
   }

   public void updatePrompt() {
      this.prompt.setText(this.nsh.cd() + ">");
   }

   public void cls() {
      for (int i = 0; i < this.buffer.lines.length; i++) {
         this.buffer.lines[i] = null;
      }

      this.buffer.repaint();
   }

   public void dump() {
      System.out.println("Console");
      System.out.println("  historyPosition = " + this.historyPosition);

      for (int i = 0; i < this.history.size(); i++) {
         System.out.println("    history[" + i + "] = " + this.history.get(i));
      }
   }

   public void showStatus(String msg) {
      if (this.shell != null) {
         this.shell.showStatus(msg);
      } else {
         System.out.println("showStatus: " + msg);
      }
   }

   class ConsoleShell extends NShell {
      ConsoleShell(PrintStream out) {
         super(out);
      }

      public void execWaitForDone() {
      }

      public void execDone(int exitCode) {
         super.execDone(exitCode);
         if (BConsole.this.execCallback != null) {
            try {
               BConsole.this.execCallback.consoleExecDone(BConsole.this, exitCode);
            } catch (Throwable var3) {
               var3.printStackTrace();
            }

            BConsole.this.execCallback = null;
         }

         BConsole.this.shell.updateCommandStates(BConsole.this.shell.getActiveView());
         BConsole.this.repaint();
      }
   }

   public interface ExecCallback {
      void consoleExecDone(BConsole var1, int var2);
   }

   public interface HyperlinkInterceptor {
      boolean consoleHyperlink(File var1, int var2, int var3, int var4, int var5);
   }
}
