package com.tridium.px.editor.sidebars.propsheet;

import com.tridium.px.editor.BPxEditorPane;
import com.tridium.px.editor.property.PxPropertyUtil;
import com.tridium.ui.px.PxPropertyComponentArray;
import com.tridium.workbench.celltable.BLabeledCellTable;
import com.tridium.workbench.celltable.CellController;
import com.tridium.workbench.celltable.BAbstractCellTable.Controller;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.baja.gx.BImage;
import javax.baja.gx.Point;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraActions;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.px.editor.BDrawingTool;
import javax.baja.px.editor.BPxEditor;
import javax.baja.px.editor.BPxSideBar;
import javax.baja.px.editor.event.PxEditorEvent;
import javax.baja.px.editor.event.PxEvent;
import javax.baja.px.editor.event.PxListener;
import javax.baja.px.editor.event.PxPropertyEvent;
import javax.baja.px.editor.event.PxSelectionEvent;
import javax.baja.sys.Action;
import javax.baja.sys.BLink;
import javax.baja.sys.BValue;
import javax.baja.sys.BajaRuntimeException;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.ui.BAbstractButton;
import javax.baja.ui.BMenu;
import javax.baja.ui.BToolBar;
import javax.baja.ui.CommandArtifact;
import javax.baja.ui.event.BMouseEvent;
import javax.baja.ui.event.BWidgetEvent;
import javax.baja.ui.pane.BEdgePane;
import javax.baja.ui.px.PxProperty;
import javax.baja.ui.table.TableSubject;
import javax.baja.util.Lexicon;
import javax.baja.workbench.celleditor.BWbCellEditor;

@NiagaraType
@NiagaraActions({@NiagaraAction(
      name = "tableSelectionModified"
   ), @NiagaraAction(
      name = "cellModified",
      parameterType = "BWidgetEvent",
      defaultValue = "new BWidgetEvent()"
   )})
public class BPxPropSheet extends BPxSideBar implements PxListener {
   public static final Action tableSelectionModified = newAction(0, null);
   public static final Action cellModified = newAction(0, new BWidgetEvent(), null);
   public static final Type TYPE = Sys.loadType(BPxPropSheet.class);
   private static Lexicon LEX = Lexicon.make("pxEditor");
   private static String DESC = LEX.getText("pxPropSheet.label");
   private static BImage ICON = BImage.make(LEX.getText("pxPropSheet.icon"));
   private static String VALUE = Lexicon.make("bajaui").getText("value");
   BPxEditorPane editorPane;
   BPxEditor editor;
   BLabeledCellTable table;
   boolean readonly = false;
   private PxPropertyComponentArray gcomps;
   private BAbstractButton btnAdd;
   private BAbstractButton btnRemove;
   private List<BPxPropSheet.Row> rows = new ArrayList<>();
   private Map<BWbCellEditor, BPxPropSheet.Row> ceToRow = new HashMap<>();

   public void tableSelectionModified() {
      this.invoke(tableSelectionModified, null, null);
   }

   public void cellModified(BWidgetEvent parameter) {
      this.invoke(cellModified, parameter, null);
   }

   @Override
   public Type getType() {
      return TYPE;
   }

   public BPxPropSheet(BPxEditor editor) {
      super(editor);
      this.editor = editor;
      this.editorPane = (BPxEditorPane)editor.getContent();
      editor.addPxListener(this);
      this.table = new BLabeledCellTable(new String[]{VALUE});
      this.table.setController(new Controller() {
         protected BMenu makePopup(TableSubject subject) {
            return subject.getActiveRow() == -1 ? null : BPxPropSheet.this.makeMenu();
         }
      });
      this.table.setCellController(new CellController() {
         public void showCellPopup(BWbCellEditor ce, BMouseEvent event) {
            Point pnt = ce.translateToAncestor(BPxPropSheet.this, new Point(event.getX(), event.getY()));
            BPxPropSheet.this.makeMenu().open(BPxPropSheet.this, pnt.x, pnt.y);
         }
      });
      BToolBar toolbar = new BToolBar();
      toolbar.add(null, this.btnAdd = this.newButton(new AddPxProperty(this)));
      toolbar.add(null, this.btnRemove = this.newButton(new RemovePxProperty(this)));
      this.btnRemove.setEnabled(false);
      BEdgePane edge = new BEdgePane();
      edge.setRight(toolbar);
      BEdgePane contents = new BEdgePane();
      contents.setTop(edge);
      contents.setCenter(this.table);
      this.setContent(contents);
      this.linkTo("linkTable", this.table, BLabeledCellTable.selectionModified, tableSelectionModified);
   }

   @Override
   public void pxEvent(PxEvent event) {
      switch (event.getEventType()) {
         case 0:
            PxEditorEvent ee = (PxEditorEvent)event;
            switch (ee.getEventId()) {
               case 0:
                  this.setReadonly(this.editor.isReadonly());
                  this.loadProps();
                  break;
               case 3:
                  BDrawingTool tool = (BDrawingTool)ee.getEventValue();
                  this.setReadonly(!tool.isNormal());
                  this.loadProps();
            }
      }
   }

   private void loadProps() {
      this.gcomps = this.editorPane.getPxPropertyComponents();
      PxProperty[] props = this.editor.getPxProperties();
      this.table.clearRows();

      for (int i = 0; i < props.length; i++) {
         PxProperty p = props[i];
         this.table.addRow(p.getName(), new BPxPropSheet.Row(p).toArray());
      }
   }

   public void doTableSelectionModified() {
      this.editorPane.getSelectedWidgets().deselectAll();
      PxProperty[] props = this.editor.getPxProperties();
      int n = this.table.getSelection().getRow();
      this.btnRemove.setEnabled(n != -1 && !this.readonly);
      if (n != -1) {
         this.editorPane.getSelectedWidgets().setWidgets(this.gcomps.getWidgets(props[n]));
      }

      this.editor.firePxEvent(new PxSelectionEvent(this.editorPane.getSelectedWidgets().getWidgets()));
   }

   public void doCellModified(BWidgetEvent event) {
      try {
         BWbCellEditor ce = (BWbCellEditor)event.getWidget();
         BValue value = (BValue)ce.saveValue();
         BPxPropSheet.Row row = this.ceToRow.get(ce);
         new ChangePxPropertyValue(this.editorPane, this, ce, row.prop, value).invoke();
      } catch (Exception var5) {
         throw new BajaRuntimeException(var5);
      }
   }

   void insertProperty(int n, PxProperty prop, CommandArtifact removal) {
      PxPropertyUtil.insert(this.editor, n, prop, removal);
      this.gcomps.setValue(prop);
      this.table.insertRow(n, prop.getName(), new BPxPropSheet.Row(prop).toArray());
      this.table.getSelection().deselectAll();
      this.table.getSelection().select(n);
      this.table.relayout();
      this.updateUI(prop, new PxPropertyEvent(0, prop));
   }

   CommandArtifact removeProperty(int n) {
      PxProperty[] props = this.editor.getPxProperties();
      PxProperty prop = props[n];
      this.gcomps.setDefaultValue(prop);
      CommandArtifact removal = PxPropertyUtil.remove(this.editor, prop, this.gcomps);
      BPxPropSheet.Row row = this.rows.get(n);
      this.remove(row.link);
      this.ceToRow.remove(row.ce);
      this.rows.remove(n);
      this.table.removeRow(n);
      this.table.getSelection().deselectAll();
      this.table.relayout();
      this.updateUI(prop, new PxPropertyEvent(1, prop));
      return removal;
   }

   void updateUI(PxProperty prop, PxEvent event) {
      switch (event.getEventType()) {
         case 1:
            switch (((PxPropertyEvent)event).getEventId()) {
               case 1:
                  this.editorPane.getSelectedWidgets().setWidgets(this.gcomps.getWidgets(prop));
            }
         default:
            this.editor.firePxEvent(event);
      }
   }

   private BMenu makeMenu() {
      BMenu menu = new BMenu();
      menu.add(null, new RenamePxProperty(this));
      menu.add(null, new RemovePxProperty(this));
      return menu;
   }

   public void setReadonly(boolean readonly) {
      this.readonly = readonly;
      this.table.setCellsEnabled(!readonly);
      this.btnAdd.setEnabled(!readonly);
      int n = this.table.getSelection().getRow();
      this.btnRemove.setEnabled(n != -1 && !readonly);
   }

   @Override
   public BImage getSideBarIcon() {
      return ICON;
   }

   @Override
   public String getSideBarDescription() {
      return DESC;
   }

   class Row {
      PxProperty prop;
      BWbCellEditor ce;
      BLink link;

      Row(PxProperty prop) {
         this.prop = prop;
         this.ce = BWbCellEditor.makeFor(prop.getValue());
         this.ce.loadValue(prop.getValue());
         this.link = BPxPropSheet.this.linkTo(this.ce, BWbCellEditor.pluginModified, BPxPropSheet.cellModified);
         BPxPropSheet.this.rows.add(this);
         BPxPropSheet.this.ceToRow.put(this.ce, this);
      }

      BWbCellEditor[] toArray() {
         return new BWbCellEditor[]{this.ce};
      }
   }
}
