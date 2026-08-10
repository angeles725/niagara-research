package com.tridium.program.ui.batch;

import com.tridium.program.batch.BAddSlotBatchRoutine;
import com.tridium.ui.BOptionDialog;
import com.tridium.ui.theme.Theme;
import com.tridium.workbench.fieldeditors.BTypeSpecFE;
import javax.baja.gx.BBrush;
import javax.baja.naming.SlotPath;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Action;
import javax.baja.sys.BBoolean;
import javax.baja.sys.BComponent;
import javax.baja.sys.BFacets;
import javax.baja.sys.BObject;
import javax.baja.sys.BValue;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.ui.BCheckBox;
import javax.baja.ui.BDialog;
import javax.baja.ui.BLabel;
import javax.baja.ui.BNullWidget;
import javax.baja.ui.BTextField;
import javax.baja.ui.enums.BHalign;
import javax.baja.ui.enums.BScrollBarPolicy;
import javax.baja.ui.enums.BValign;
import javax.baja.ui.pane.BBorderPane;
import javax.baja.ui.pane.BEdgePane;
import javax.baja.ui.pane.BGridPane;
import javax.baja.ui.pane.BScrollPane;
import javax.baja.util.BTypeSpec;
import javax.baja.util.Lexicon;
import javax.baja.workbench.fieldeditor.BWbFieldEditor;

@NiagaraType
@NiagaraAction(
   name = "typeChanged"
)
public class BAddDialog extends BOptionDialog {
   public static final Action typeChanged = newAction(0, null);
   public static final Type TYPE = Sys.loadType(BAddDialog.class);
   private static Lexicon lex = Lexicon.make("program");
   private BTextField name = new BTextField("", 30);
   private BTypeSpecFE type;
   private BWbFieldEditor editor;
   private BCheckBox setIfExists = new BCheckBox(lex.getText("batchEditor.setIfExists"));
   private BBorderPane content;

   public void typeChanged() {
      this.invoke(typeChanged, null, null);
   }

   public Type getType() {
      return TYPE;
   }

   public static BAddSlotBatchRoutine open(BBatchEditor editor) {
      BAddDialog dlg = new BAddDialog(editor);
      dlg.setBoundsCenteredOnOwner();
      dlg.open();
      if (dlg.getResult() == 1) {
         try {
            if (SlotPath.isValidName(dlg.name.getText())) {
               String name = dlg.name.getText();
               BValue value = (BValue)dlg.editor.saveValue();
               boolean set = dlg.setIfExists.isSelected();
               return BAddSlotBatchRoutine.make(name, value, set);
            }

            BDialog.error(editor, lex.getText("batchEditor.commands.slotAdd.label"), lex.getText("batchEditor.invalidName"));
         } catch (Exception var5) {
            BDialog.error(editor, "Error", "Failed", var5);
         }
      }

      return null;
   }

   private BAddDialog(BBatchEditor parent) {
      super(parent, lex.getText("batchEditor.commands.slotAdd.label"), new BNullWidget(), 3, null, null);
      BFacets facets = BFacets.make("allowNull", BBoolean.FALSE);
      facets = BFacets.make(facets, "showAbstract", BBoolean.FALSE);
      this.type = new BTypeSpecFE();
      this.linkTo("link", this.type, BTypeSpecFE.pluginModified, typeChanged);
      this.type.loadValue(BComponent.TYPE.getTypeSpec(), facets);
      this.setIfExists.setSelected(true);
      this.editor = this.make(BComponent.TYPE);
      BGridPane edGrid = new BGridPane(1);
      edGrid.setValign(BValign.top);
      edGrid.setHalign(BHalign.left);
      edGrid.add(null, this.content = new BBorderPane(this.editor, 1.0, 1.0, 1.0, 1.0));
      BGridPane grid = new BGridPane(2);
      grid.add(null, new BLabel(lex.getText("batchEditor.newName")));
      grid.add(null, this.name);
      grid.add(null, new BLabel(lex.getText("batchEditor.newType")));
      grid.add(null, this.type);
      BGridPane grid2 = new BGridPane(1);
      grid2.setHalign(BHalign.left);
      grid2.add(null, grid);
      grid2.add(null, this.setIfExists);
      BEdgePane edge = new BEdgePane();
      edge.setTop(new BBorderPane(grid2, 0.0, 0.0, 10.0, 0.0));
      BBrush bgcolor = Theme.scrollPane().getControlBackground();
      BScrollPane editorPane = new BScrollPane(new BBorderPane(edGrid, lex.getText("batchEditor.newValue")));
      editorPane.setBorderPolicy(BScrollBarPolicy.asNeeded);
      editorPane.setViewportBackground(bgcolor);
      edge.setCenter(editorPane);
      this.setContent(new BBorderPane(edge, 10.0, 10.0, 10.0, 10.0));
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

   public void doTypeChanged() {
      try {
         BObject obj = ((BTypeSpec)this.type.saveValue()).getInstance();
         this.editor = this.make(obj.getType());
         this.content.setContent(this.editor);
         this.computePreferredSize();
         this.setSize(this.getPreferredWidth(), this.getPreferredHeight());
         this.setBoundsCenteredOnOwner();
      } catch (Exception var2) {
         var2.printStackTrace();
      }
   }
}
