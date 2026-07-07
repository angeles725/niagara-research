package com.tridium.px.editor.sidebars.layersheet;

import com.tridium.px.editor.BPxEditorPane;
import com.tridium.px.editor.util.LayerManager;
import com.tridium.workbench.celleditors.BEnumCE;
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
import javax.baja.px.editor.event.PxLayerEvent;
import javax.baja.px.editor.event.PxListener;
import javax.baja.px.editor.event.PxSelectionEvent;
import javax.baja.sys.Action;
import javax.baja.sys.BLink;
import javax.baja.sys.BajaRuntimeException;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.ui.BAbstractButton;
import javax.baja.ui.BMenu;
import javax.baja.ui.BSeparator;
import javax.baja.ui.BToolBar;
import javax.baja.ui.CommandArtifact;
import javax.baja.ui.event.BMouseEvent;
import javax.baja.ui.event.BWidgetEvent;
import javax.baja.ui.pane.BEdgePane;
import javax.baja.ui.px.BLayerStatus;
import javax.baja.ui.px.PxLayer;
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
public class BPxLayerSheet extends BPxSideBar implements PxListener {
   public static final Action tableSelectionModified = newAction(0, null);
   public static final Action cellModified = newAction(0, new BWidgetEvent(), null);
   public static final Type TYPE = Sys.loadType(BPxLayerSheet.class);
   private static Lexicon LEX = Lexicon.make("pxEditor");
   private static String DESC = LEX.getText("pxLayerSheet.label");
   private static BImage ICON = BImage.make(LEX.getText("pxLayerSheet.icon"));
   private static String STATUS = Lexicon.make("bajaui").getText("status");
   BPxEditorPane editorPane;
   BPxEditor editor;
   LayerManager layerMgr;
   private BAbstractButton btnAdd;
   private BAbstractButton btnRemove;
   BLabeledCellTable table;
   boolean readonly = false;
   private List<BPxLayerSheet.Row> rows = new ArrayList<>();
   private Map<BWbCellEditor, BPxLayerSheet.Row> ceToRow = new HashMap<>();

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

   public BPxLayerSheet(BPxEditor editor) {
      super(editor);
      this.editor = editor;
      this.editorPane = (BPxEditorPane)editor.getContent();
      this.layerMgr = this.editorPane.getLayerManager();
      editor.addPxListener(this);
      this.table = new BLabeledCellTable(new String[]{STATUS});
      this.table.setController(new Controller() {
         protected BMenu makePopup(TableSubject subject) {
            return subject.getActiveRow() == -1 ? null : BPxLayerSheet.this.makeMenu();
         }
      });
      this.table.setCellController(new CellController() {
         public void showCellPopup(BWbCellEditor ce, BMouseEvent event) {
            Point pnt = ce.translateToAncestor(BPxLayerSheet.this, new Point(event.getX(), event.getY()));
            BPxLayerSheet.this.makeMenu().open(BPxLayerSheet.this, pnt.x, pnt.y);
         }
      });
      BToolBar toolbar = new BToolBar();
      toolbar.add(null, this.btnAdd = this.newButton(new AddPxLayer(this)));
      toolbar.add(null, this.btnRemove = this.newButton(new RemovePxLayer(this)));
      this.btnRemove.setEnabled(false);
      BEdgePane edge = new BEdgePane();
      edge.setRight(toolbar);
      BEdgePane contents = new BEdgePane();
      contents.setTop(edge);
      contents.setCenter(this.table);
      this.setContent(contents);
      this.linkTo("linkTable", this.table, BLabeledCellTable.selectionModified, tableSelectionModified);
   }

   private BMenu makeMenu() {
      BMenu menu = new BMenu();
      menu.add(null, new RenamePxLayer(this.editorPane, this));
      menu.add(null, new RemovePxLayer(this));
      menu.add(null, new BSeparator());
      this.editorPane.getStudio().populatePopupMenu(menu);
      return menu;
   }

   @Override
   public void pxEvent(PxEvent event) {
      switch (event.getEventType()) {
         case 0:
            PxEditorEvent ee = (PxEditorEvent)event;
            switch (ee.getEventId()) {
               case 0:
                  this.setReadonly(this.editor.isReadonly());
                  this.loadLayers();
                  break;
               case 3:
                  BDrawingTool tool = (BDrawingTool)ee.getEventValue();
                  this.setReadonly(!tool.isNormal());
                  this.loadLayers();
            }
      }
   }

   private void loadLayers() {
      PxLayer[] layers = this.editor.getPxLayers();
      this.table.clearRows();

      for (int i = 0; i < layers.length; i++) {
         PxLayer lay = layers[i];
         this.table.addRow(lay.getName(), new BPxLayerSheet.Row(lay).toArray());
      }
   }

   public void doTableSelectionModified() {
      this.editorPane.getSelectedWidgets().deselectAll();
      PxLayer[] layers = this.editor.getPxLayers();
      int n = this.table.getSelection().getRow();
      this.btnRemove.setEnabled(n != -1 && !this.readonly);
      if (n != -1) {
         this.editorPane.getSelectedWidgets().setWidgets(this.editorPane.getLayerManager().getLayerWidgets(layers[n]));
      }

      this.editor.firePxEvent(new PxSelectionEvent(this.editorPane.getSelectedWidgets().getWidgets()));
   }

   public void doCellModified(BWidgetEvent event) {
      try {
         BWbCellEditor ce = (BWbCellEditor)event.getWidget();
         BLayerStatus status = (BLayerStatus)ce.saveValue();
         BPxLayerSheet.Row row = this.ceToRow.get(ce);
         new ChangePxLayerValue(this.editorPane, this, ce, row.layer, status).invoke();
      } catch (Exception var5) {
         throw new BajaRuntimeException(var5);
      }
   }

   void insertLayer(int index, PxLayer layer, CommandArtifact removal) {
      this.layerMgr.insert(index, layer, removal);
      this.table.insertRow(index, layer.getName(), new BPxLayerSheet.Row(layer).toArray());
      this.table.getSelection().deselectAll();
      this.table.getSelection().select(index);
      this.table.relayout();
      this.updateUI(layer, new PxLayerEvent(0, layer));
   }

   CommandArtifact removeLayer(int index) {
      PxLayer[] layers = this.editor.getPxLayers();
      PxLayer layer = layers[index];
      CommandArtifact removal = this.layerMgr.remove(this.editorPane, layer);
      BPxLayerSheet.Row row = this.rows.get(index);
      this.remove(row.link);
      this.ceToRow.remove(row.ce);
      this.rows.remove(index);
      this.table.removeRow(index);
      this.table.getSelection().deselectAll();
      this.table.relayout();
      this.updateUI(layer, new PxLayerEvent(1, layer));
      return removal;
   }

   void updateUI(PxLayer layer, PxEvent event) {
      this.editor.firePxEvent(event);
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
      PxLayer layer;
      BWbCellEditor ce;
      BLink link;

      Row(PxLayer layer) {
         this.layer = layer;
         this.ce = new BEnumCE();
         this.ce.loadValue(layer.getStatus());
         this.link = BPxLayerSheet.this.linkTo(this.ce, BWbCellEditor.pluginModified, BPxLayerSheet.cellModified);
         BPxLayerSheet.this.rows.add(this);
         BPxLayerSheet.this.ceToRow.put(this.ce, this);
      }

      BWbCellEditor[] toArray() {
         return new BWbCellEditor[]{this.ce};
      }
   }
}
