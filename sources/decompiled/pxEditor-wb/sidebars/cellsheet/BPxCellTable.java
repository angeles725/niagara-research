package com.tridium.px.editor.sidebars.cellsheet;

import com.tridium.px.editor.BPxEditorPane;
import com.tridium.ui.theme.Theme;
import com.tridium.workbench.celltable.CellController;
import com.tridium.workbench.celltable.CellEditorContainer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.baja.gx.Graphics;
import javax.baja.gx.Point;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Action;
import javax.baja.sys.BComponent;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.ui.BMenu;
import javax.baja.ui.BWidget;
import javax.baja.ui.event.BMouseEvent;
import javax.baja.ui.table.BTable;
import javax.baja.ui.table.TableCellRenderer;
import javax.baja.ui.table.TableController;
import javax.baja.ui.table.TableModel;
import javax.baja.ui.table.BTable.TableSupport;
import javax.baja.ui.table.TableCellRenderer.Cell;
import javax.baja.workbench.celleditor.BWbCellEditor;

@NiagaraType
@NiagaraAction(
   name = "handleSelectionModified"
)
public class BPxCellTable extends BTable implements CellEditorContainer {
   public static final Action handleSelectionModified = newAction(0, null);
   public static final Type TYPE = Sys.loadType(BPxCellTable.class);
   protected BPxEditorPane editorPane;
   protected final BPxCellSheet sheet;
   private BPxCellTable.Model model = new BPxCellTable.Model();
   private boolean dragging = false;
   private boolean ignoreModified = false;
   private CellController cellController;

   public void handleSelectionModified() {
      this.invoke(handleSelectionModified, null, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BPxCellTable(BPxEditorPane editorPane, BPxCellSheet sheet) {
      this.editorPane = editorPane;
      this.sheet = sheet;
      this.setMultipleSelection(false);
      this.setHeaderVisible(false);
      this.setExtendedResize(true);
      this.setHscrollBarVisible(false);
      this.setVscrollBarVisible(false);
      this.setGridBrush(Theme.widget().getControlShadow());
      this.setCellRenderer(new BPxCellTable.CellRenderer());
      this.setModel(this.model);
      this.setController(new BPxCellTable.Controller());
      this.setCellController(new BPxCellTable.PxCellController());
      this.linkTo("lnkHandleMod", this, BTable.selectionModified, handleSelectionModified);
   }

   public void doHandleSelectionModified() {
      if (!this.ignoreModified) {
         BPxCellTable[] tables = this.sheet.getCellTables();

         for (int i = 0; i < tables.length; i++) {
            if (tables[i] != this) {
               tables[i].ignoreModified = true;
               tables[i].getSelection().deselectAll();
               tables[i].ignoreModified = false;
            }
         }
      }
   }

   public void doLayout(BWidget[] children) {
      super.doLayout(children);

      for (int i = 0; i < this.model.cellEditors.size(); i++) {
         BWbCellEditor ce = this.model.cellEditors.get(i);
         ce.setBounds(
            this.getCellWidth(0), this.getCellRenderer().getCellHeight() * ce.getRowIndex(), this.getCellWidth(1), this.getCellRenderer().getCellHeight()
         );
      }
   }

   public BWidget childAt(Point pt) {
      return this.dragging ? null : super.childAt(pt);
   }

   void addCellEditor(BWbCellEditor ce) {
      this.model.cellEditors.add(ce);
      this.add(null, ce);
   }

   public CellController getCellController() {
      return this.cellController;
   }

   public void setCellController(CellController c) {
      this.installSupport(this.cellController, c);
      this.cellController = c;
   }

   private void installSupport(TableSupport old, TableSupport support) {
      if (support == null) {
         throw new NullPointerException();
      } else if (old != support) {
         if (support.getTable() != null) {
            throw new IllegalArgumentException("Already installed on another table");
         } else {
            if (old != null) {
               old.setTable(null);
            }

            support.setTable(this);
         }
      }
   }

   protected CellTableContext makeContext(final BWbCellEditor ce) {
      return new CellTableContext() {
         @Override
         public boolean allowAnimate() {
            return true;
         }

         @Override
         public BComponent[] getGroupableComponents() {
            List<BComponent> arr = new ArrayList<>();
            arr.addAll(new ArrayList<>(Arrays.asList(BPxCellTable.this.sheet.getAllWidgets())));
            return arr.toArray(new BComponent[0]);
         }

         @Override
         public void doPaste() {
            BPxCellTable.this.sheet.getCellContext().cellModified(ce);
         }
      };
   }

   private class CellRenderer extends TableCellRenderer {
      private CellRenderer() {
      }

      public double getPreferredCellWidth(Cell cell) {
         return BPxCellTable.this.sheet.getColumnWidth();
      }

      public void paintCell(Graphics g, Cell cell) {
         if (cell.column == 0) {
            super.paintCell(g, cell);
         }
      }
   }

   private class Controller extends TableController {
      private Controller() {
      }

      protected void resizeHotspotPressed(BMouseEvent event, int column) {
         BPxCellTable.this.dragging = true;
      }

      protected void resizeHotspotDragged(BMouseEvent event, int column) {
         BPxCellTable[] tables = BPxCellTable.this.sheet.getCellTables();

         for (int i = 0; i < tables.length; i++) {
            tables[i].setColumnPosition(column, event.getX());
            tables[i].relayoutSync();
         }
      }

      protected void resizeHotspotReleased(BMouseEvent event, int column) {
         BPxCellTable.this.dragging = false;
         BPxCellTable.this.sheet.setColumnWidth(BPxCellTable.this.getCellWidth(0));
         BPxCellTable.this.relayoutSync();
      }

      protected void cellPopup(BMouseEvent event, int row, int column) {
         BWbCellEditor ce = BPxCellTable.this.model.cellEditors.get(row);
         BMenu menu = BPxCellTable.this.sheet.makePopup(ce, BPxCellTable.this.makeContext(ce));
         Point pnt = BPxCellTable.this.translateToAncestor(BPxCellTable.this.sheet, new Point(event.getX(), event.getY()));
         menu.open(BPxCellTable.this.sheet, pnt.x, pnt.y);
      }
   }

   private class Model extends TableModel {
      private List<BWbCellEditor> cellEditors = new ArrayList<>();

      private Model() {
      }

      public int getRowCount() {
         return this.cellEditors.size();
      }

      public int getColumnCount() {
         return 2;
      }

      public String getColumnName(int col) {
         return "";
      }

      public Object getValueAt(int row, int col) {
         switch (col) {
            case 0:
               return this.cellEditors.get(row).getPropertyName();
            case 1:
               return this.cellEditors.get(row);
            default:
               throw new IllegalStateException();
         }
      }
   }

   private class PxCellController extends CellController {
      private PxCellController() {
      }

      public void showCellPopup(BWbCellEditor ce, BMouseEvent event) {
         BMenu menu = BPxCellTable.this.sheet.makePopup(ce, BPxCellTable.this.makeContext(ce));
         Point pnt = ce.translateToAncestor(BPxCellTable.this.sheet, new Point(event.getX(), event.getY()));
         menu.open(BPxCellTable.this.sheet, pnt.x, pnt.y);
      }
   }
}
