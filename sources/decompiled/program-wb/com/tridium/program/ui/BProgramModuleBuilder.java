package com.tridium.program.ui;

import com.tridium.program.BCode;
import com.tridium.program.BProgram;
import com.tridium.program.module.BProgramModule;
import com.tridium.program.ui.module.BuildHelper;
import com.tridium.program.ui.module.IBuildListener;
import com.tridium.program.ui.module.ValidateModel;
import com.tridium.ui.theme.Theme;
import com.tridium.util.EscUtil;
import com.tridium.workbench.console.BConsole;
import com.tridium.workbench.transfer.TransferUtil;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;
import javax.baja.gx.BFont;
import javax.baja.gx.BImage;
import javax.baja.gx.BInsets;
import javax.baja.gx.Graphics;
import javax.baja.naming.BOrd;
import javax.baja.naming.ViewQuery;
import javax.baja.nre.annotations.AgentOn;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraActions;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.util.SortUtil;
import javax.baja.sys.Action;
import javax.baja.sys.BComponent;
import javax.baja.sys.BComponentEvent;
import javax.baja.sys.BFacets;
import javax.baja.sys.BIcon;
import javax.baja.sys.BObject;
import javax.baja.sys.BString;
import javax.baja.sys.BValue;
import javax.baja.sys.BVector;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.ui.BButton;
import javax.baja.ui.BDialog;
import javax.baja.ui.BLabel;
import javax.baja.ui.BNullWidget;
import javax.baja.ui.BProgressDialog;
import javax.baja.ui.BToolBar;
import javax.baja.ui.BWidget;
import javax.baja.ui.CommandArtifact;
import javax.baja.ui.BProgressDialog.Worker;
import javax.baja.ui.enums.BHalign;
import javax.baja.ui.enums.BOrientation;
import javax.baja.ui.enums.BScrollBarPolicy;
import javax.baja.ui.event.BMouseEvent;
import javax.baja.ui.pane.BBorderPane;
import javax.baja.ui.pane.BEdgePane;
import javax.baja.ui.pane.BExpandablePane;
import javax.baja.ui.pane.BGridPane;
import javax.baja.ui.pane.BLabelPane;
import javax.baja.ui.pane.BScrollPane;
import javax.baja.ui.pane.BSplitPane;
import javax.baja.ui.pane.BTabbedPane;
import javax.baja.ui.table.BTable;
import javax.baja.ui.table.TableCellRenderer;
import javax.baja.ui.table.TableCellRenderer.Cell;
import javax.baja.ui.transfer.TransferContext;
import javax.baja.util.BFolder;
import javax.baja.util.BNameMap;
import javax.baja.util.Lexicon;
import javax.baja.workbench.BWbShell;
import javax.baja.workbench.component.table.BComponentTable;
import javax.baja.workbench.component.table.ComponentTableCellRenderer;
import javax.baja.workbench.mgr.BAbstractManager;
import javax.baja.workbench.mgr.BMgrTable;
import javax.baja.workbench.mgr.MgrColumn;
import javax.baja.workbench.mgr.MgrController;
import javax.baja.workbench.mgr.MgrModel;
import javax.baja.workbench.mgr.MgrColumn.Name;
import javax.baja.workbench.mgr.MgrController.IMgrCommand;
import javax.baja.workbench.mgr.MgrController.MgrCommand;
import javax.baja.workbench.mgr.folder.BFolderManager;
import javax.baja.workbench.mgr.folder.FolderController;
import javax.baja.workbench.mgr.folder.FolderModel;

@NiagaraType(
   agent = {@AgentOn(
      types = {"program:ProgramModule"}
   )}
)
@NiagaraActions({@NiagaraAction(
      name = "handleSelection",
      flags = 4
   ), @NiagaraAction(
      name = "handleModified",
      flags = 4
   ), @NiagaraAction(
      name = "handlePermissionSelection",
      flags = 4
   )})
public class BProgramModuleBuilder extends BFolderManager {
   public static final Action handleSelection = newAction(4, null);
   public static final Action handleModified = newAction(4, null);
   public static final Action handlePermissionSelection = newAction(4, null);
   public static final Type TYPE = Sys.loadType(BProgramModuleBuilder.class);
   Lexicon lex = Lexicon.make(BProgramModuleBuilder.class);
   static final BImage CHECK = BImage.make(BIcon.std("check.png"));
   static final BImage BLANK = BImage.make(BIcon.std("blank.png"));
   BProgramModule pmod;
   BuildHelper buildHelper;
   BSplitPane split;
   BFieldSheet sheet = new BFieldSheet();
   Map<BValue, ValidateModel> verify;
   ProgramModuleBuilderCommands commands = new ProgramModuleBuilderCommands(this);
   Permissions permissions = new Permissions(this);

   public void handleSelection() {
      this.invoke(handleSelection, null, null);
   }

   public void handleModified() {
      this.invoke(handleModified, null, null);
   }

   public void handlePermissionSelection() {
      this.invoke(handlePermissionSelection, null, null);
   }

   public Type getType() {
      return TYPE;
   }

   public void doLoadValue(BObject obj, Context cx) {
      this.pmod = (BProgramModule)obj;
      this.buildHelper = new BuildHelper(this.pmod);
      this.verify = new HashMap<>();
      this.verify.put(this.pmod, ValidateModel.make(this.pmod));
      super.doLoadValue(obj, cx);
   }

   protected void init() {
      super.init();
      ViewQuery query = this.getWbShell().getActiveOrdTarget().getViewQuery();
      if (query != null) {
         try {
            String name = query.getParameter("program");
            int toSelect = ((BBuilderTable)this.getModel().getTable()).getRowOfProgram(name);
            this.getModel().getTable().getSelection().select(toSelect);
         } catch (Exception var4) {
         }
      }
   }

   public void handleComponentEvent(BComponentEvent event) {
      BComponent source = event.getSourceComponent();
      BValue value = event.getValue();
      if (source == this.pmod) {
         this.verify.put(this.pmod, ValidateModel.make(this.pmod));
      } else if (event.getId() == 3) {
         this.verify.put(this.getSelectedProgram(), ValidateModel.make(this.getSelectedProgram()));
         this.handleSelection();
      } else if (value instanceof BProgram) {
         switch (event.getId()) {
            case 0:
            case 1:
               this.verify.put(value, ValidateModel.make((BProgram)value));
               this.handleSelection();
               break;
            case 2:
               if (value == this.getSelectedProgram()) {
                  this.getModel().getTable().getSelection().deselectAll();
               }

               this.verify.remove(value);
         }

         this.verify.put(this.pmod, ValidateModel.make(this.pmod));
      }

      super.handleComponentEvent(event);
   }

   protected BObject doSaveValue(BObject value, Context cx) throws Exception {
      try {
         this.sheet.saveValue(this.pmod, cx);
         this.verify.put(this.pmod, ValidateModel.make(this.pmod));
      } catch (Exception var7) {
         this.verify.get(this.pmod).err(var7.getMessage());
         throw var7;
      } finally {
         this.getModel().getTable().getSelection().select(-1);
      }

      return this.pmod;
   }

   public void doHandleModified() {
      if (this.sheet.isModified()) {
         this.getController().updateCommands();
      }
   }

   protected MgrModel makeModel() {
      return new BProgramModuleBuilder.BuilderModel(this);
   }

   protected MgrController makeController() {
      return new BProgramModuleBuilder.Controller(this);
   }

   public void doHandleSelection() {
      ValidateModel m = this.verify.get(this.pmod);
      BProgram selected = this.getSelectedProgram();
      if (selected != null) {
         m = this.verify.get(selected);
      }

      if (m.getTable() != null) {
         m.setTable(null);
      }

      this.split.setWidget2(new BTable(m));
      if (this.split.getDividerPosition() > 75.0) {
         this.split.setDividerPosition(75.0);
         this.split.relayout();
      }
   }

   public void doHandlePermissionSelection() {
      int numSelected = this.permissions.table.getSelection().getRowCount();
      if (numSelected == 0) {
         this.commands.editPermissionCommand.setEnabled(false);
         this.commands.removePermissionCommand.setEnabled(false);
      } else if (numSelected == 1) {
         this.commands.editPermissionCommand.setEnabled(true);
         this.commands.removePermissionCommand.setEnabled(true);
      } else {
         this.commands.editPermissionCommand.setEnabled(false);
         this.commands.removePermissionCommand.setEnabled(true);
      }
   }

   private BProgram getSelectedProgram() {
      return (BProgram)this.getModel().getTable().getSelectedComponent();
   }

   class BuilderModel extends FolderModel {
      MgrColumn colProgramName = new BProgramModuleBuilder.BuilderModel.ProgName(1);
      MgrColumn colComp = new BProgramModuleBuilder.BuilderModel.CompName();
      MgrColumn[] columns = new MgrColumn[]{this.colProgramName, this.colComp};

      public BuilderModel(BFolderManager manager) {
         super(manager);
      }

      protected BMgrTable makeTable() {
         BBuilderTable table = new BBuilderTable(this);
         BProgramModuleBuilder.this.linkTo(table, BTable.selectionModified, BProgramModuleBuilder.handleSelection);
         return table;
      }

      public Type getFolderType() {
         return BFolder.TYPE;
      }

      public Type[] getIncludeTypes() {
         return new Type[]{BProgram.TYPE};
      }

      public boolean accept(BComponent component) {
         if (component.getParent().equals(BProgramModuleBuilder.this.pmod.getPrograms())) {
            BProgramModuleBuilder.this.verify.put(component, ValidateModel.make((BProgram)component));
            return true;
         } else {
            return false;
         }
      }

      public int getSubscribeDepth() {
         return 2;
      }

      protected String makeTableTitle() {
         return BProgramModuleBuilder.this.lex.getText("modBuilder.title");
      }

      public BWidget makePane() {
         BProgramModuleBuilder.this.sheet = new BFieldSheet();
         BProgramModuleBuilder.this.sheet.loadValue(BProgramModuleBuilder.this.pmod, BProgramModuleBuilder.this.getCurrentContext());
         BProgramModuleBuilder.this.linkTo(BProgramModuleBuilder.this.sheet, BFieldSheet.pluginModified, BAbstractManager.setModified);
         BProgramModuleBuilder.this.linkTo(BProgramModuleBuilder.this.sheet, BFieldSheet.pluginModified, BProgramModuleBuilder.handleModified);
         BExpandablePane expand = new BExpandablePane();
         BEdgePane editorPane = new BEdgePane();
         editorPane.setCenter(BProgramModuleBuilder.this.sheet);
         expand.setSummary(new BLabel(BProgramModuleBuilder.this.lex.getText("modBuilder.settings")));
         expand.setExpansion(new BBorderPane(editorPane, 0.0, 0.0, 5.0, 0.0));
         expand.setExpanded(true);
         BWidget managerPane = this.makeManagerPane();
         BProgramModuleBuilder.this.split = new BSplitPane(BOrientation.vertical, 75.0);
         BProgramModuleBuilder.this.split.setWidget1(managerPane);
         BProgramModuleBuilder.this.split.setWidget2(new BNullWidget());
         BEdgePane edge = new BEdgePane();
         edge.setTop(expand);
         edge.setCenter(BProgramModuleBuilder.this.split);
         return edge;
      }

      private BWidget makeManagerPane() {
         super.initTable();
         BWidget table = super.getTable();
         BWidget requestTab = this.makeRequestsPane();
         BTabbedPane tabbedPane = new BTabbedPane();
         tabbedPane.add(this.makeTableTitle(), new BLabelPane(this.makeTableTitle(), table));
         tabbedPane.add("permissionRequests", new BLabelPane(BProgramModuleBuilder.this.lex.getText("modBuilder.permissions.title"), requestTab));
         return tabbedPane;
      }

      private BWidget makeRequestsPane() {
         BGridPane buttons = new BGridPane(3);
         buttons.add(null, new BButton(BProgramModuleBuilder.this.commands.addPermissionCommand));
         buttons.add(null, new BButton(BProgramModuleBuilder.this.commands.editPermissionCommand));
         buttons.add(null, new BButton(BProgramModuleBuilder.this.commands.removePermissionCommand));
         buttons.setUniformColumnWidth(true);
         buttons.setColumnAlign(BHalign.fill);
         BEdgePane edge = new BEdgePane();
         edge.setCenter(BProgramModuleBuilder.this.permissions.table);
         edge.setBottom(new BBorderPane(buttons));
         return edge;
      }

      protected MgrColumn[] makeColumns() {
         return this.columns;
      }

      protected class CompName extends MgrColumn {
         public CompName() {
            super("BComponent Name", 6);
         }

         public Object get(Object row) {
            return BuildHelper.toComponentName((BProgram)row);
         }
      }

      protected class ProgName extends Name {
         private BProgramModuleBuilder.BuilderModel.ProgName.CellRenderer rend = new BProgramModuleBuilder.BuilderModel.ProgName.CellRenderer();

         public ProgName(int flags) {
            super(flags);
         }

         public TableCellRenderer getCellRenderer() {
            return this.rend;
         }

         private class CellRenderer extends ComponentTableCellRenderer {
            private CellRenderer() {
            }

            public double getPreferredCellWidth(Cell s) {
               return 20.0 + this.getFont(s).width("VeryLongBProgramNameExample");
            }

            public void paintCell(Graphics g, Cell cell) {
               this.paintCellBackground(g, cell);
               double x = 2.0;
               double y = (cell.height - 16.0) / 2.0;
               ValidateModel v = BProgramModuleBuilder.this.verify.get(this.getComponentTable().getComponentAt(cell.row));
               g.drawImage(v.getProgramStatusIcon(), x, y);
               x += 20.0;
               String s = (String)cell.value;
               BFont font = this.getFont(cell);
               g.setFont(font);
               g.drawString(s, x, (cell.height + font.getAscent() - font.getDescent()) / 2.0);
            }

            protected BComponentTable getComponentTable() {
               return ProgName.this.getManager().getModel().getTable();
            }
         }
      }
   }

   class Controller extends FolderController {
      BProgramModuleBuilder.Controller.CompileCommand compileCmd;
      BProgramModuleBuilder.Controller.BuildCommand buildCmd;
      BProgramModuleBuilder.Controller.EditDepends dependsCmd;

      public Controller(BFolderManager manager) {
         super(manager);
         this.compileCmd = new BProgramModuleBuilder.Controller.CompileCommand(manager);
         this.buildCmd = new BProgramModuleBuilder.Controller.BuildCommand(manager);
         this.dependsCmd = new BProgramModuleBuilder.Controller.EditDepends(manager);
      }

      protected IMgrCommand[] makeCommands() {
         this.allDescendants.setSelected(true);
         return new IMgrCommand[]{this.edit, this.compileCmd, this.buildCmd, this.dependsCmd};
      }

      public void cellDoubleClicked(BMgrTable table, BMouseEvent event, int row, int col) {
         BProgram p = (BProgram)table.getComponentAt(row);
         BWbShell shell = table.getManager().getWbShell();
         shell.hyperlink(BOrd.make(p.getNavOrd(), "view:program:ProgramEditor"));
      }

      public BToolBar makeToolBar() {
         return super.makeToolBar();
      }

      public void updateCommands() {
         super.updateCommands();
         boolean buildEnabled = true;

         for (ValidateModel validateModel : BProgramModuleBuilder.this.verify.values()) {
            if (validateModel.getErrCount() > 0) {
               buildEnabled = false;
               break;
            }
         }

         if (BProgramModuleBuilder.this.sheet != null && BProgramModuleBuilder.this.sheet.isModified()) {
            buildEnabled = false;
         }

         this.buildCmd.setEnabled(buildEnabled);
         this.dependsCmd.setEnabled(buildEnabled);
         this.compileCmd.setEnabled(!buildEnabled && !BProgramModuleBuilder.this.sheet.isModified());
      }

      public CommandArtifact drop(BMgrTable table, TransferContext cx) throws Exception {
         return TransferUtil.insert(BProgramModuleBuilder.this, cx, BProgramModuleBuilder.this.pmod.getPrograms(), null);
      }

      protected class BuildCommand extends MgrCommand {
         public BuildCommand(BWidget owner) {
            super(owner, BProgramModuleBuilder.this.lex, "modBuilder.cmd.build");
            this.setFlags(7);
         }

         public CommandArtifact doInvoke() throws Exception {
            BProgramModuleBuilder.this.saveValue();
            BProgressDialog.open(
               this.getOwner(),
               BProgramModuleBuilder.this.lex.getText("modBuilder.cmd.build.title"),
               new BProgramModuleBuilder.Controller.BuildCommand.BuildWorker()
            );
            return null;
         }

         class BuildWorker extends Worker implements IBuildListener {
            int curStep = 0;

            @Override
            public void setNumBuildSteps(int steps) {
               this.getDialog().getProgressBar().setMax(steps);
            }

            @Override
            public void nextStep(String desc) {
               this.getDialog().getProgressBar().setValue(++this.curStep);
               this.updateDesc(desc);
            }

            @Override
            public void updateDesc(String desc) {
               this.getDialog().getMessageLabel().setText(desc);
            }

            @Override
            public BWidget getOwner() {
               return BProgramModuleBuilder.this;
            }

            public void doCancel() throws Exception {
            }

            public void doRun() throws Exception {
               try {
                  BProgramModuleBuilder.this.buildHelper.buildModule(this);
               } catch (Exception var2) {
                  BDialog.error(this.getDialog(), BProgramModuleBuilder.this.lex.getText("programModule.nbuild.fail"), var2.getMessage(), var2);
               }
            }
         }
      }

      protected class CompileCommand extends MgrCommand {
         public CompileCommand(BWidget owner) {
            super(owner, BProgramModuleBuilder.this.lex, "modBuilder.cmd.compile");
            this.setFlags(7);
         }

         public CommandArtifact doInvoke() throws Exception {
            BProgram[] programs = BProgramModuleBuilder.this.pmod.listPrograms();

            for (BProgram program : programs) {
               if (BProgramModuleBuilder.this.verify.get(program).getErrCount() != 0) {
                  try {
                     BProgramModuleBuilder.Controller.CompileCommand.PCompiler compiler = new BProgramModuleBuilder.Controller.CompileCommand.PCompiler(
                        this.getOwner(), program
                     );
                     compiler.compile(BCode.generateClassName(), program.getCode(), null);
                  } catch (Exception var7) {
                     var7.printStackTrace();
                  }
               }
            }

            return null;
         }

         private class PCompiler extends Compiler {
            BProgram program;
            BProgramEditor pe;

            public PCompiler(BWidget owner, BProgram program) {
               super(owner);
               this.program = program;
               this.pe = new BProgramEditor();
               this.pe.loadValue(program);
            }

            @Override
            public void writeSource(FileWriter out, String source) throws Exception {
               SourceWriter writer = new SourceWriter(out, this.pe);
               writer.generate(this.className);
            }

            @Override
            public synchronized void compile(String className, BCode code, String source) throws Exception {
               super.compile(className, code, source);
            }

            @Override
            public synchronized void consoleExecDone(BConsole console, int exitCode) {
               super.consoleExecDone(console, exitCode);
            }

            @Override
            public void compileFailed(BConsole console) throws Exception {
               console.cls();
               BProgramModuleBuilder.this.verify.get(this.program).err(lex.getText("vprogram.err.compile"));
            }

            @Override
            public void compileSuccess(BConsole console) throws Exception {
               super.compileSuccess(console);
               int old = this.program.getCode().getChecksum();
               int now = this.pe.computeChecksum(this.program.getCode().getSource());
               if (old != now) {
                  this.program.getCode().setChecksum(now);
                  BProgramModuleBuilder.this.handleComponentEvent(new BComponentEvent(0, BProgramModuleBuilder.this.pmod.getPrograms(), null, this.program));
               }
            }
         }
      }

      protected class EditDepends extends MgrCommand {
         public EditDepends(BWidget owner) {
            super(owner, BProgramModuleBuilder.this.lex, "modBuilder.cmd.editDepends");
            this.setFlags(3);
         }

         public CommandArtifact doInvoke() throws Exception {
            BVector vector = new BVector();
            BFieldSheet sheet = this.makeDependsSheet(vector);
            BScrollPane scroll = new BScrollPane(new BBorderPane(sheet, BInsets.make(10.0)));
            scroll.setVpolicy(BScrollBarPolicy.always);
            scroll.setViewportBackground(Theme.widget().getControlBackground());

            while (2 != BDialog.open(BProgramModuleBuilder.this, BProgramModuleBuilder.this.lex.getText("modBuilder.cmd.editDepends.title"), scroll, 3)) {
               try {
                  sheet.saveValue(vector, null);
               } catch (Exception var12) {
                  BDialog.error(BProgramModuleBuilder.this, "Error", var12.getMessage(), var12);
                  continue;
               }

               BNameMap depends = BNameMap.DEFAULT;
               Property[] props = vector.getPropertiesArray();

               for (Property prop : props) {
                  String module = EscUtil.slot.unescape(prop.getName());
                  String version = vector.getString(prop);
                  depends = BNameMap.make(depends, module, version);
               }

               BProgramModuleBuilder.this.pmod.setDependencies(depends);
               return null;
            }

            return null;
         }

         private BFieldSheet makeDependsSheet(BVector vector) {
            BFieldSheet sheet = new BFieldSheet(true);
            BFacets facets = BFacets.make("validator", "baja:VersionValidator");
            facets = BFacets.make(facets, BFacets.make("fieldSheetShow", true));
            facets = BFacets.make(facets, BFacets.make("iconOverride", "module://icons/x16/module.png"));
            BNameMap fullDepends = BProgramModule.rationalizeDependencies(BProgramModuleBuilder.this.pmod);
            String[] names = fullDepends.list();
            SortUtil.sort(names);

            for (String name : names) {
               Property p = vector.add(EscUtil.slot.escape(name), BString.make(fullDepends.get(name).getFormat()));
               vector.setFacets(p, facets);
            }

            sheet.loadValue(vector);
            return sheet;
         }
      }
   }
}
