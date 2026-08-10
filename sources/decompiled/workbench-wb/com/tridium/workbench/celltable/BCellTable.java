package com.tridium.workbench.celltable;

import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.util.Array;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.ui.BWidget;
import javax.baja.ui.table.TableModel;
import javax.baja.workbench.celleditor.BWbCellEditor;

@NiagaraType
public class BCellTable extends BAbstractCellTable {
   public static final Type TYPE = Sys.loadType(BCellTable.class);
   private Array<BWbCellEditor[]> rows = new Array(BWbCellEditor[].class);
   private double[] prefCellWidths;

   @Override
   public Type getType() {
      return TYPE;
   }

   public BCellTable() {
      super(null, false);
      throw new IllegalStateException();
   }

   public BCellTable(final String[] colNames, double[] prefCellWidths) {
      super(colNames, false);
      if (colNames.length != prefCellWidths.length) {
         throw new IllegalArgumentException("array length mismatch");
      } else {
         this.prefCellWidths = prefCellWidths;
         this.setModel(new TableModel() {
            public int getRowCount() {
               return BCellTable.this.rows.size();
            }

            public int getColumnCount() {
               return colNames.length;
            }

            public String getColumnName(int c) {
               return colNames[c];
            }

            public Object getSubject(int r) {
               return BCellTable.this.getRowCells(r);
            }

            public Object getValueAt(int r, int c) {
               return BCellTable.this.getRowCells(r)[c];
            }
         });
      }
   }

   public void doLayout(BWidget[] children) {
      super.doLayout(children);
      double h = this.getCellRenderer().getCellHeight();
      double y = this.getHeaderRenderer().getHeaderHeight() - 1.0;

      for (int r = 0; r < this.rows.size(); r++) {
         BWbCellEditor[] cells = this.getRowCells(r);
         double x = 0.0;
         int n = 0;

         for (int c = 0; c < cells.length; c++) {
            double w = this.getCellWidth(c + n);
            cells[c].setBounds(x, y, w, h);
            x += w;
         }

         y += h;
      }
   }

   public void addRow(BWbCellEditor[] cells) {
      this.checkEnabled(cells);
      this.insertRow(this.rows.size(), cells);
   }

   public void insertRow(int idx, BWbCellEditor[] cells) {
      this.checkEnabled(cells);
      this.rows.add(idx, cells);

      for (int c = 0; c < this.colNames.length; c++) {
         String stem = this.colNames[c] + this.stemCtr++;
         this.add("ce" + stem, cells[c]);
         this.linkTo("mod" + stem, cells[c], BWbCellEditor.pluginModified, modified);
         this.rowLinks.put(cells[c], "mod" + stem);
      }

      this.resetRowIndexes();
   }

   @Override
   public void removeRow(int idx) {
      BWbCellEditor[] r = this.getRowCells(idx);
      this.rows.remove(idx);

      for (int c = 0; c < r.length; c++) {
         this.remove(r[c].getPropertyInParent());
         this.remove(this.rowLinks.get(r[c]));
         this.rowLinks.remove(r[c]);
      }

      this.resetRowIndexes();
   }

   @Override
   public void clearRows() {
      for (int r = 0; r < this.rows.size(); r++) {
         BWbCellEditor[] cells = this.getRowCells(r);

         for (int c = 0; c < cells.length; c++) {
            this.remove(cells[c].getPropertyInParent());
            this.remove(this.rowLinks.get(cells[c]));
         }
      }

      this.rows.clear();
      this.rowLinks.clear();
   }

   @Override
   public int getRowCount() {
      return this.rows.size();
   }

   @Override
   public BWbCellEditor[] getRowCells(int r) {
      return (BWbCellEditor[])this.rows.get(r);
   }

   private void resetRowIndexes() {
      for (int r = 0; r < this.rows.size(); r++) {
         BWbCellEditor[] cells = this.getRowCells(r);

         for (int c = 0; c < cells.length; c++) {
            cells[c].setRowIndex(r);
         }
      }
   }

   @Override
   protected double[] preferredCellWidths() {
      return this.prefCellWidths;
   }
}
