package com.tridium.program.ui;

import com.tridium.crypto.core.cert.SigningUtil;
import com.tridium.program.BProgram;
import com.tridium.program.BProgramCode;
import com.tridium.program.module.BProgramModule;
import com.tridium.workbench.console.BConsole.HyperlinkInterceptor;
import com.tridium.workbench.slotsheet.BSlotSheet;
import java.io.File;
import javax.baja.gx.BImage;
import javax.baja.naming.BOrd;
import javax.baja.naming.SlotPath;
import javax.baja.nre.annotations.AgentOn;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraActions;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Action;
import javax.baja.sys.BBlob;
import javax.baja.sys.BComponentEvent;
import javax.baja.sys.BObject;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.ui.BButton;
import javax.baja.ui.BLabel;
import javax.baja.ui.BMenu;
import javax.baja.ui.BToolBar;
import javax.baja.ui.BWidget;
import javax.baja.ui.CommandArtifact;
import javax.baja.ui.enums.BHalign;
import javax.baja.ui.pane.BBorderPane;
import javax.baja.ui.pane.BEdgePane;
import javax.baja.ui.pane.BGridPane;
import javax.baja.ui.pane.BTabbedPane;
import javax.baja.ui.pane.BTextEditorPane;
import javax.baja.ui.text.BTextEditor;
import javax.baja.ui.text.Position;
import javax.baja.ui.text.parsers.JavaParser;
import javax.baja.ui.util.UiLexicon;
import javax.baja.workbench.view.BWbComponentView;

@NiagaraType(
   agent = {@AgentOn(
      types = {"program:Program"}
   )}
)
@NiagaraActions({@NiagaraAction(
      name = "codeTextModified"
   ), @NiagaraAction(
      name = "tabSelectionModified"
   )})
public class BProgramEditor extends BWbComponentView implements HyperlinkInterceptor {
   public static final Action codeTextModified = newAction(0, null);
   public static final Action tabSelectionModified = newAction(0, null);
   public static final Type TYPE = Sys.loadType(BProgramEditor.class);
   static UiLexicon lex = UiLexicon.bajaui();
   static String statusOk = lex.getText("programEditor.status.ok");
   static String statusNeedsCompile = lex.getText("programEditor.status.needsCompile");
   static String statusError = lex.getText("programEditor.status.error");
   static String statusSigningError = lex.getText("programEditor.status.signingError");
   static String statusNotSigned = lex.getText("programEditor.status.notSigned");
   static String statusNotTimestamped = lex.getText("programEditor.status.notTimestamped");
   static BImage statusOkIcon = BImage.make("module://icons/x16/shapes/circleGreen.png");
   static BImage statusWarningIcon = BImage.make("module://icons/x16/shapes/circleGold.png");
   static BImage statusErrorIcon = BImage.make("module://icons/x16/shapes/circleRed.png");
   static BImage editIcon = BImage.make("module://icons/x16/edit.png");
   static BImage slotsIcon = BImage.make("module://icons/x16/page.png");
   static BImage importsIcon = BImage.make("module://icons/x16/module.png");
   static BImage sourceIcon = BImage.make("module://icons/x16/script.png");
   static BImage addIcon = BImage.make("module://icons/x16/add.png");
   static BImage removeIcon = BImage.make("module://icons/x16/delete.png");
   static final String boilerPlate = "public void onStart() throws Exception\n{\n  // start up code here\n}\n\npublic void onExecute() throws Exception\n{\n  // execute code (set executeOnChange flag on inputs)\n}\n\npublic void onStop() throws Exception\n{\n  // shutdown code here\n}\n\n";
   ProgramEditorCommands commands = new ProgramEditorCommands(this);
   ProgramCompiler compiler = new ProgramCompiler(this);
   int sourceLine;
   BProgram program;
   BProgramCode code;
   BTabbedPane tabs;
   boolean modified = false;
   boolean compileError = false;
   BWidget statusPane;
   BLabel[] statuses;
   BWidget editTab;
   BTextEditor editText;
   BWidget slotTab;
   BSlotSheet slotSheet;
   Imports imports = new Imports(this);
   BWidget importsTab;
   BWidget sourceTab;
   BTextEditor sourceText;

   public void codeTextModified() {
      this.invoke(codeTextModified, null, null);
   }

   public void tabSelectionModified() {
      this.invoke(tabSelectionModified, null, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BProgramEditor() {
      this.autoRegisterForComponentEvents = false;
      this.setContent(new BLabel("ProgramEditor"));
      this.initStatusPane();
      this.initEditPane();
      this.initSlotPane();
      this.initImportsPane();
      this.initSourcePane();
      this.tabs = new BTabbedPane();
      this.tabs.addPane(lex.getText("programEditor.edit"), editIcon, this.editTab);
      this.tabs.addPane(lex.getText("programEditor.slots"), slotsIcon, this.slotTab);
      this.tabs.addPane(lex.getText("programEditor.imports"), importsIcon, this.importsTab);
      this.tabs.addPane(lex.getText("programEditor.source"), sourceIcon, this.sourceTab);
      this.linkTo(null, this.editText, BTextEditor.textModified, codeTextModified);
      this.linkTo(null, this.tabs, BTabbedPane.selectionModified, tabSelectionModified);
      BEdgePane edge = new BEdgePane();
      edge.setTop(this.statusPane);
      edge.setCenter(this.tabs);
      this.setContent(edge);
   }

   private void initStatusPane() {
      this.statuses = new BLabel[3];
      this.statuses[0] = new BLabel("status not initialized");
      this.statuses[1] = new BLabel("");
      this.statuses[2] = new BLabel("");
      BGridPane gridPane = new BGridPane(1);
      gridPane.setHalign(BHalign.left);
      gridPane.setColumnGap(10.0);
      gridPane.add(null, this.statuses[0]);
      gridPane.add(null, this.statuses[1]);
      gridPane.add(null, this.statuses[2]);
      this.statusPane = new BBorderPane(gridPane, 5.0, 0.0, 10.0, 0.0);
   }

   private void initEditPane() {
      this.editText = new BTextEditor();
      this.editText.setParser(new JavaParser());
      this.editTab = new BTextEditorPane(this.editText);
   }

   private void initSlotPane() {
      this.slotSheet = new BSlotSheet();
      this.slotTab = this.slotSheet;
   }

   private void initImportsPane() {
      BGridPane buttons = new BGridPane(3);
      buttons.add(null, new BButton(this.commands.importType));
      buttons.add(null, new BButton(this.commands.importPackage));
      buttons.add(null, new BButton(this.commands.removeImport));
      buttons.setUniformColumnWidth(true);
      buttons.setColumnAlign(BHalign.fill);
      BEdgePane edge = new BEdgePane();
      edge.setCenter(this.imports.table);
      edge.setBottom(new BBorderPane(buttons));
      this.importsTab = edge;
   }

   private void initSourcePane() {
      this.sourceText = new BTextEditor();
      this.sourceText.setEditable(false);
      this.sourceText.setParser(new JavaParser());
      this.sourceTab = new BTextEditorPane(this.sourceText);
   }

   public BMenu[] getViewMenus() {
      return this.commands.getViewMenus();
   }

   public BToolBar getViewToolBar() {
      return this.commands.getViewToolBar();
   }

   public void doLoadValue(BObject value, Context cx) {
      this.program = (BProgram)value;
      this.registerForComponentEvents(this.program, 1);
      this.code = (BProgramCode)this.program.getCode().newCopy();
      this.loadCode();
      this.imports.load(this.code);
      this.slotSheet.doLoadValue(this.program, cx);
      this.updateStatus();
      this.tabs.selectPane(this.editTab);
   }

   public BObject doSaveValue(BObject value, Context cx) throws Exception {
      this.saveCode();
      this.imports.save(this.code);
      this.program.setCode(this.code);
      this.registerForComponentEvents(this.program, 1);
      this.program.updateActionParameters();
      this.modified = false;
      this.updateStatus();
      return this.program;
   }

   public CommandArtifact invokeCommand(int id) throws Exception {
      BTextEditor text = this.getActiveTextEditor();
      if (text == null) {
         return null;
      } else {
         switch (id) {
            case 6:
               return text.doFind();
            case 7:
               return text.doFindPrev();
            case 8:
               return text.doFindNext();
            case 9:
               return text.doReplace();
            case 10:
               return text.doGoto();
            default:
               return super.invokeCommand(id);
         }
      }
   }

   public BTextEditor getActiveTextEditor() {
      BWidget activeTab = this.tabs.getSelectedPane();
      if (activeTab == this.editTab) {
         return this.editText;
      } else {
         return activeTab == this.sourceTab ? this.sourceText : null;
      }
   }

   public void updateEditorCommands() {
      BTextEditor text = this.getActiveTextEditor();
      if (text != null) {
         text.updateEnableStates();
      }

      this.setTransferWidget(text);
      boolean search = text != null;
      this.setCommandEnabled(6, search);
      this.setCommandEnabled(8, search);
      this.setCommandEnabled(7, search);
      this.setCommandEnabled(9, search && text.isEditable());
      this.setCommandEnabled(10, search);
   }

   public boolean consoleHyperlink(File file, int line1, int col1, int line2, int col2) {
      if (!file.equals(this.compiler.java)) {
         BOrd ord = BOrd.make(SlotPath.unescape(file.getName()));
         BObject target = ord.get();
         if (!(target instanceof BProgramModule) && !(target instanceof BProgram)) {
            return false;
         } else {
            this.getWbShell().hyperlink(ord);
            return true;
         }
      } else {
         this.highlight(line1, col1, line2, col2);
         return true;
      }
   }

   public void highlight(int line1, int col1, int line2, int col2) {
      this.tabs.selectPane(this.editTab);
      line1 -= this.sourceLine;
      line2 -= this.sourceLine;
      col1 -= 2;
      col2 -= 2;
      this.highlight(this.editText, line1, col1, line2, col2);
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

   public void updateStatus() {
      int i = 0;
      if (!this.modified && (this.computeChecksum(this.editText.getText()) == this.program.getCode().getChecksum() || this.compileError)) {
         if (!this.compileError) {
            this.statuses[i].setText(statusOk);
            this.statuses[i].setImage(statusOkIcon);
         }
      } else {
         this.compileError = false;
         this.statuses[i].setText(statusNeedsCompile);
         this.statuses[i].setImage(statusWarningIcon);
      }

      i++;
      BBlob signature = this.code.getSignature();
      if (signature == BBlob.DEFAULT) {
         this.statuses[i].setText(statusNotSigned);
         this.statuses[i].setImage(statusWarningIcon);
         this.commands.installCertificate.setEnabled(false);
         i++;
      } else {
         if (this.program.getCode().getStatus().isFault()) {
            this.statuses[i].setText(this.program.getCode().getFaultCause());
            this.statuses[i].setImage(statusErrorIcon);
            this.commands.installCertificate.setEnabled(true);
            i++;
         } else {
            this.commands.installCertificate.setEnabled(false);
         }

         byte[] sigBytes = signature.copyBytes();
         byte[] classBytes = this.code.getClassFile().copyBytes();
         if (!SigningUtil.isTimestamped(classBytes, sigBytes)) {
            this.statuses[i].setText(statusNotTimestamped);
            this.statuses[i].setImage(statusWarningIcon);
            i++;
         }
      }

      while (i < this.statuses.length) {
         this.statuses[i].setText("");
         this.statuses[i].setImage(BImage.NULL);
         i++;
      }
   }

   public void updateProgramCode() {
      this.program.setCode(this.code);
      this.registerForComponentEvents(this.program, 1);
   }

   public void updateStatusToError() {
      this.statuses[0].setText(statusError);
      this.statuses[0].setImage(statusErrorIcon);
      this.compileError = true;
   }

   public void updateStatusToSigningError() {
      this.statuses[0].setText(statusSigningError);
      this.statuses[0].setImage(statusErrorIcon);
      this.compileError = true;
   }

   public void updateStatusSuccess() {
      this.compileError = false;
      this.updateStatus();
   }

   public void updateSource() {
      char[] text = SourceWriter.generateToCharArray(this, "ProgramImpl");
      this.sourceText.getModel().read(text);
   }

   public void saveCode() {
      String source = this.editText.getText();
      this.code.setSource(source);
      this.imports.updateImports();
   }

   public void loadCode() {
      String text = this.code.getSource();
      if (text.length() == 0) {
         text = "public void onStart() throws Exception\n{\n  // start up code here\n}\n\npublic void onExecute() throws Exception\n{\n  // execute code (set executeOnChange flag on inputs)\n}\n\npublic void onStop() throws Exception\n{\n  // shutdown code here\n}\n\n";
      }

      this.editText.setText(text);
   }

   public Imports getImports() {
      return this.imports;
   }

   public int computeChecksum(String source) {
      int checksum = source.hashCode();
      Property[] props = this.program.getPropertiesArray();

      for (Property prop : props) {
         if (!prop.isFrozen()) {
            checksum ^= prop.getName().hashCode();
            checksum ^= prop.getType().getTypeName().hashCode();
         }
      }

      return checksum;
   }

   public void doCodeTextModified() {
      if (!this.isModifiedStateLocked()) {
         this.setModified();
         this.modified = true;
         this.updateStatus();
      }
   }

   public void doTabSelectionModified() {
      this.updateEditorCommands();
      this.saveCode();
      this.updateSource();
   }

   public void handleComponentEvent(BComponentEvent event) {
      this.slotSheet.handleComponentEvent(event);
      this.imports.updateImports();
      switch (event.getId()) {
         case 0:
         case 1:
         case 2:
         case 3:
            this.updateStatus();
      }
   }
}
