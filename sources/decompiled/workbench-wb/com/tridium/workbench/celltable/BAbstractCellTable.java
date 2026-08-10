package com.tridium.workbench.celltable;

import com.tridium.ui.theme.Theme;
import java.util.HashMap;
import java.util.Map;
import javax.baja.gx.BBrush;
import javax.baja.gx.BColor;
import javax.baja.gx.Graphics;
import javax.baja.nre.annotations.NiagaraTopic;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Sys;
import javax.baja.sys.Topic;
import javax.baja.sys.Type;
import javax.baja.ui.BMenu;
import javax.baja.ui.event.BMouseEvent;
import javax.baja.ui.event.BWidgetEvent;
import javax.baja.ui.table.BTable;
import javax.baja.ui.table.TableCellRenderer;
import javax.baja.ui.table.TableController;
import javax.baja.ui.table.TableHeaderRenderer;
import javax.baja.ui.table.BTable.TableSupport;
import javax.baja.ui.table.TableCellRenderer.Cell;
import javax.baja.ui.table.TableHeaderRenderer.Header;
import javax.baja.workbench.celleditor.BWbCellEditor;

@NiagaraType
@NiagaraTopic(
   name = "modified",
   eventType = "BWidgetEvent"
)
public abstract class BAbstractCellTable extends BTable implements CellEditorContainer {
   public static final Topic modified = newTopic(0, null);
   public static final Type TYPE = Sys.loadType(BAbstractCellTable.class);
   private CellController cellController;
   protected final String[] colNames;
   protected Map<BWbCellEditor, String> rowLinks = new HashMap<>();
   protected int stemCtr;
   private boolean isLabeled;
   private boolean dragging = false;
   private boolean cellsEnabled = true;

   public void fireModified(BWidgetEvent event) {
      this.fire(modified, event, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BAbstractCellTable(String[] colNames, boolean isLabeled) {
      this.colNames = colNames;
      this.isLabeled = isLabeled;
      this.setStyleClasses("cell-table");
      this.setCellRenderer(new BAbstractCellTable.CellRenderer());
      this.setHeaderRenderer(new BAbstractCellTable.HeaderRenderer());
      this.setController(new BAbstractCellTable.Controller());
      this.setMultipleSelection(false);
      this.setGridBrush(Theme.widget().getControlShadow());
      this.setHscrollBarVisible(false);
      this.setVscrollBarVisible(false);
      this.setCellController(new CellController());
   }

   @Override
   public CellController getCellController() {
      return this.cellController;
   }

   @Override
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

   protected abstract double[] preferredCellWidths();

   protected final void checkEnabled(BWbCellEditor[] cells) {
      for (int i = 0; i < cells.length; i++) {
         if (!cells[i].isReadonly()) {
            cells[i].setReadonly(!this.cellsEnabled);
         }
      }
   }

   public abstract void removeRow(int var1);

   public abstract void clearRows();

   public abstract int getRowCount();

   public abstract BWbCellEditor[] getRowCells(int var1);

   public void setCellsEnabled(boolean cellsEnabled) {
      this.cellsEnabled = cellsEnabled;
   }

   public static class CellRenderer extends TableCellRenderer {
      public final double getPreferredCellWidth(Cell cell) {
         return this.table().preferredCellWidths()[cell.column];
      }

      public final void paintCell(Graphics g, Cell cell) {
         if (this.table().isLabeled && cell.column == 0) {
            super.paintCell(g, cell);
         } else {
            this.paintCellBackground(g, cell);
         }
      }

      public BBrush getBackground(Cell cell) {
         return !this.getTable().isEnabled() ? BBrush.makeSolid(BColor.gainsboro) : null;
      }

      private BAbstractCellTable table() {
         return (BAbstractCellTable)this.getTable();
      }
   }

   public static class Controller extends TableController {
      protected void resizeHotspotPressed(BMouseEvent event, int column) {
         this.table().dragging = true;
      }

      protected void resizeHotspotDragged(BMouseEvent event, int column) {
         BAbstractCellTable table = this.table();
         table.setColumnPosition(column, event.getX());
         table.preferredCellWidths()[column] = table.getCellWidth(column);
         table.relayout();
      }

      protected void resizeHotspotReleased(BMouseEvent event, int column) {
         BAbstractCellTable table = this.table();
         table.dragging = false;
         table.preferredCellWidths()[column] = table.getCellWidth(column);
         table.relayout();
      }

      protected BMenu makeOptionsMenu() {
         BMenu menu = super.makeOptionsMenu();
         menu.keep(new String[]{"resizeColumns"});
         return menu;
      }

      private BAbstractCellTable table() {
         return (BAbstractCellTable)this.getTable();
      }
   }

   public static class HeaderRenderer extends TableHeaderRenderer {
      public double getPreferredHeaderWidth(Header header) {
         double d = super.getPreferredHeaderWidth(header);
         return Math.max(this.table().preferredCellWidths()[header.column], d);
      }

      private BAbstractCellTable table() {
         return (BAbstractCellTable)this.getTable();
      }
   }
}
