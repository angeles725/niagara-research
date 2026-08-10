package com.tridium.program.ui.batch;

import com.tridium.program.BProgramService;
import javax.baja.nre.annotations.AgentOn;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BObject;
import javax.baja.sys.Context;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.ui.BButton;
import javax.baja.ui.BMenu;
import javax.baja.ui.BSeparator;
import javax.baja.ui.BToolBar;
import javax.baja.ui.enums.BHalign;
import javax.baja.ui.pane.BBorderPane;
import javax.baja.ui.pane.BEdgePane;
import javax.baja.ui.pane.BGridPane;
import javax.baja.ui.util.BTitlePane;
import javax.baja.workbench.view.BWbView;

@NiagaraType(
   agent = {@AgentOn(
      types = {"program:ProgramService"}
   )}
)
public class BBatchEditor extends BWbView {
   public static final Type TYPE = Sys.loadType(BBatchEditor.class);
   private static BBatchTable.Model model;
   BBatchTable table = new BBatchTable(this);
   BatchCommands commands;
   BProgramService service;
   BTitlePane titlePane;

   public Type getType() {
      return TYPE;
   }

   public BBatchEditor() {
      if (model != null) {
         this.table.setModel(model.makeCopy());
      }

      this.commands = new BatchCommands(this);
      BGridPane aux = new BGridPane(5);
      aux.setColumnAlign(BHalign.fill);
      aux.add(null, new BButton(this.commands.findObjects));
      aux.add(null, new BButton(this.commands.clearAll));
      aux.add(null, new BButton(this.commands.rename));
      aux.add(null, new BButton(this.commands.slotAdd));
      aux.add(null, new BButton(this.commands.tagAdd));
      aux.add(null, new BButton(this.commands.slotEdit));
      aux.add(null, new BButton(this.commands.slotRename));
      aux.add(null, new BButton(this.commands.slotRemove));
      aux.add(null, new BButton(this.commands.slotFlags));
      BEdgePane pane = new BEdgePane();
      this.titlePane = BTitlePane.makePane(this.getTypeDisplayName(null), this.table);
      pane.setCenter(this.titlePane);
      pane.setBottom(new BBorderPane(aux, 5.0, 0.0, 0.0, 0.0));
      this.setContent(pane);
      this.setTransferWidget(this.table);
   }

   public BMenu[] getViewMenus() {
      return new BMenu[]{this.commands.buildMenu()};
   }

   public BToolBar getViewToolBar() {
      BToolBar toolbar = new BToolBar();
      toolbar.add(null, this.commands.findObjects);
      toolbar.add(null, this.commands.clear);
      toolbar.add(null, this.commands.selectCols);
      toolbar.add(null, new BSeparator());
      toolbar.add(null, this.commands.rename);
      toolbar.add(null, this.commands.slotAdd);
      toolbar.add(null, this.commands.slotEdit);
      toolbar.add(null, this.commands.slotRename);
      toolbar.add(null, this.commands.slotRemove);
      toolbar.add(null, this.commands.slotFlags);
      return toolbar;
   }

   protected void doLoadValue(BObject value, Context cx) throws Exception {
      this.service = (BProgramService)value;
      this.table.refresh();
      this.commands.updateCommands();
   }

   public void deactivated() {
      super.deactivated();
      model = this.table.model;
   }

   public void disableCommands() {
      this.commands.findObjects.setEnabled(false);
      this.commands.clear.setEnabled(false);
      this.commands.clearAll.setEnabled(false);
      this.commands.selectCols.setEnabled(false);
      this.commands.rename.setEnabled(false);
      this.commands.slotAdd.setEnabled(false);
      this.commands.tagAdd.setEnabled(false);
      this.commands.slotEdit.setEnabled(false);
      this.commands.slotRename.setEnabled(false);
      this.commands.slotRemove.setEnabled(false);
      this.commands.slotFlags.setEnabled(false);
      this.commands.hyperlink.setEnabled(false);
   }

   public void enableCommands() {
      this.commands.updateCommands();
   }

   protected void updateObjectCount(int count) {
      this.titlePane.setCount(count);
   }

   protected void updateModel(BBatchTable.Model model) {
      BBatchEditor.model = model;
   }
}
