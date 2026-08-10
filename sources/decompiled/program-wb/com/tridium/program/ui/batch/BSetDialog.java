package com.tridium.program.ui.batch;

import com.tridium.program.batch.BEditSlotBatchRoutine;
import com.tridium.ui.BOptionDialog;
import com.tridium.ui.theme.Theme;
import javax.baja.gx.BBrush;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Action;
import javax.baja.sys.BComponent;
import javax.baja.sys.BObject;
import javax.baja.sys.BValue;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.ui.BCheckBox;
import javax.baja.ui.BDialog;
import javax.baja.ui.BLabel;
import javax.baja.ui.BListDropDown;
import javax.baja.ui.BNullWidget;
import javax.baja.ui.enums.BHalign;
import javax.baja.ui.enums.BScrollBarPolicy;
import javax.baja.ui.enums.BValign;
import javax.baja.ui.pane.BBorderPane;
import javax.baja.ui.pane.BEdgePane;
import javax.baja.ui.pane.BGridPane;
import javax.baja.ui.pane.BScrollPane;
import javax.baja.util.Lexicon;
import javax.baja.workbench.fieldeditor.BWbFieldEditor;

@NiagaraType
@NiagaraAction(
   name = "propertyChanged"
)
public class BSetDialog extends BOptionDialog {
   public static final Action propertyChanged = newAction(0, null);
   public static final Type TYPE = Sys.loadType(BSetDialog.class);
   private static Lexicon lex = Lexicon.make("program");
   private BBatchEditor parent;
   private BListDropDown dropDown;
   private BWbFieldEditor editor;
   private BCheckBox add;
   private BBorderPane content;

   public void propertyChanged() {
      this.invoke(propertyChanged, null, null);
   }

   public Type getType() {
      return TYPE;
   }

   public static BEditSlotBatchRoutine open(BBatchEditor editor) {
      BSetDialog dlg = new BSetDialog(editor);
      dlg.setBoundsCenteredOnOwner();
      dlg.open();
      if (dlg.getResult() == 1) {
         try {
            String name = (String)dlg.dropDown.getList().getSelectedItem();
            BValue value = (BValue)dlg.editor.saveValue();
            return BEditSlotBatchRoutine.make(name, value);
         } catch (Exception var4) {
            BDialog.error(editor, "Error", "Failed", var4);
         }
      }

      return null;
   }

   private BSetDialog(BBatchEditor parent) {
      super(parent, lex.getText("batchEditor.commands.slotEdit.label"), new BNullWidget(), 3, null, null);
      this.parent = parent;
      this.editor = this.make(BComponent.TYPE);
      this.add = new BCheckBox(lex.getText("batchEditor.addIfNotExist"));
      this.add.setSelected(true);
      this.dropDown = new BListDropDown();
      String[] cols = parent.table.model.getAllColumns();

      for (String col : cols) {
         this.dropDown.getList().addItem(col);
      }

      this.linkTo(this.dropDown, BListDropDown.listActionPerformed, propertyChanged);
      BGridPane grid = new BGridPane(2);
      grid.setHalign(BHalign.left);
      grid.add(null, new BLabel(lex.getText("batchEditor.property")));
      grid.add(null, this.dropDown);
      BGridPane edGrid = new BGridPane(1);
      edGrid.setValign(BValign.top);
      edGrid.setHalign(BHalign.left);
      edGrid.add(null, this.content = new BBorderPane(this.editor, 1.0, 1.0, 1.0, 1.0));
      BEdgePane edge = new BEdgePane();
      edge.setTop(new BBorderPane(grid, 0.0, 0.0, 10.0, 0.0));
      BBrush bgcolor = Theme.scrollPane().getControlBackground();
      BScrollPane editorPane = new BScrollPane(new BBorderPane(edGrid, lex.getText("batchEditor.newValue")));
      editorPane.setBorderPolicy(BScrollBarPolicy.asNeeded);
      editorPane.setViewportBackground(bgcolor);
      edge.setCenter(editorPane);
      this.setContent(new BBorderPane(edge, 10.0, 10.0, 10.0, 10.0));
      this.dropDown.getList().setSelectedIndex(0);
      this.doPropertyChanged();
   }

   private BWbFieldEditor make(Type type) {
      BObject prop = type.getInstance();
      BWbFieldEditor fe = BWbFieldEditor.makeFor(prop);
      fe.loadValue(prop);
      return fe;
   }

   public void computePreferredSize() {
      super.computePreferredSize();
      double pw = Math.max(500.0, this.getPreferredWidth());
      double ph = Math.max(300.0, this.getPreferredHeight());
      this.setPreferredSize(pw, ph);
   }

   public void doPropertyChanged() {
      try {
         String col = (String)this.dropDown.getList().getSelectedItem();
         this.editor = this.parent.table.model.getColumnEditor(col);
         this.content.setContent(this.editor);
         this.editor.loadValue(this.editor.getCurrentValue(), this.editor.getCurrentContext());
         this.computePreferredSize();
         this.setSize(this.getPreferredWidth(), this.getPreferredHeight());
         this.setBoundsCenteredOnOwner();
      } catch (Exception var2) {
         var2.printStackTrace();
      }
   }
}
