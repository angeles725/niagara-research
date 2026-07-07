package com.tridium.px.editor.sidebars.cellsheet.celleditors;

import com.tridium.workbench.celltable.BAbstractCellTable;
import com.tridium.workbench.celltable.BLabeledCellTable;
import com.tridium.workbench.celltable.BAbstractCellTable.CellRenderer;
import javax.baja.gx.BBrush;
import javax.baja.naming.BOrd;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.util.Array;
import javax.baja.nre.util.IFilter;
import javax.baja.sys.Action;
import javax.baja.sys.BFacets;
import javax.baja.sys.BString;
import javax.baja.sys.BajaRuntimeException;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.ui.BDialog;
import javax.baja.ui.BMenu;
import javax.baja.ui.BWidget;
import javax.baja.ui.pane.BBorderPane;
import javax.baja.ui.pane.BConstrainedPane;
import javax.baja.ui.pane.BEdgePane;
import javax.baja.ui.pane.BScrollPane;
import javax.baja.ui.table.BTable;
import javax.baja.ui.table.TableCellRenderer;
import javax.baja.ui.table.TableController;
import javax.baja.ui.table.TableModel;
import javax.baja.ui.table.TableCellRenderer.Cell;
import javax.baja.util.Lexicon;
import javax.baja.workbench.celleditor.BWbCellEditor;

@NiagaraType
@NiagaraAction(
   name = "cellTableModified"
)
public class BVariablesEditor extends BEdgePane {
   public static final Action cellTableModified = newAction(0, null);
   public static final Type TYPE = Sys.loadType(BVariablesEditor.class);
   private static Lexicon WB = Lexicon.make("wbutil");
   private static String VALUE = WB.getText("facetsFE.value");
   private static Lexicon LEX = Lexicon.make("pxEditor");
   private BLabeledCellTable cellTable = new BLabeledCellTable(new String[]{VALUE});
   private String[] keys;
   private BTable beforeAfter;
   BOrd[] before;
   BOrd[] after;

   public void cellTableModified() {
      this.invoke(cellTableModified, null, null);
   }

   public Type getType() {
      return TYPE;
   }

   public static BFacets open(BWidget owner, String title, BFacets facets, BOrd[] origOrds, boolean readonly) throws Exception {
      BVariablesEditor editorPane = new BVariablesEditor(facets, origOrds, readonly);
      int r = BDialog.open(owner, title, editorPane, 3);
      return r != 1 ? null : editorPane.save();
   }

   public BVariablesEditor(BFacets facets, BOrd[] origOrds, boolean readonly) {
      this.cellTable.setCellsEnabled(!readonly);
      this.cellTable.setCellRenderer(new CellRenderer() {
         public BBrush getSelectionForeground(Cell cell) {
            return this.getForeground(cell);
         }

         public BBrush getSelectionBackground(Cell cell) {
            return this.getBackground(cell);
         }
      });
      this.keys = facets.list();

      for (int i = 0; i < this.keys.length; i++) {
         BWbCellEditor ce = new BVariableTextCE();
         ce.loadValue(facets.getFacet(this.keys[i]));
         this.cellTable.addRow(this.keys[i], new BWbCellEditor[]{ce});
      }

      IFilter filter = new IFilter() {
         public boolean accept(Object obj) {
            return !obj.equals(BOrd.NULL);
         }
      };
      this.before = (BOrd[])new Array(origOrds).filter(filter).trim();
      this.after = new BOrd[this.before.length];
      this.setAfterOrds();
      this.beforeAfter = new BTable();
      this.beforeAfter.setModel(new BVariablesEditor.BeforeAfterModel());
      this.beforeAfter.setController(new TableController() {
         protected BMenu makeOptionsMenu() {
            BMenu menu = super.makeOptionsMenu();
            menu.keep(new String[]{"resizeColumns"});
            return menu;
         }
      });
      this.beforeAfter.setCellRenderer(new TableCellRenderer() {
         public BBrush getSelectionForeground(Cell cell) {
            return this.getForeground(cell);
         }

         public BBrush getSelectionBackground(Cell cell) {
            return this.getBackground(cell);
         }
      });
      BConstrainedPane cons1 = new BConstrainedPane(new BScrollPane(this.cellTable));
      cons1.setMinHeight(150.0);
      cons1.setMaxHeight(300.0);
      cons1.setMinWidth(300.0);
      BConstrainedPane cons2 = new BConstrainedPane(new BScrollPane(this.beforeAfter));
      cons2.setMaxHeight(300.0);
      cons2.setMinWidth(300.0);
      this.setTop(new BBorderPane(cons1, 0.0, 0.0, 10.0, 0.0));
      this.setBottom(cons2);
      this.linkTo(this.cellTable, BAbstractCellTable.modified, cellTableModified);
   }

   public void doCellTableModified() {
      this.setAfterOrds();
      this.repaint();
   }

   private void setAfterOrds() {
      try {
         BFacets fac = this.save();

         for (int i = 0; i < this.before.length; i++) {
            this.after[i] = this.before[i].substitute(fac);
         }
      } catch (Exception var3) {
         throw new BajaRuntimeException(var3);
      }
   }

   public BFacets save() throws Exception {
      BString[] values = new BString[this.keys.length];

      for (int i = 0; i < this.keys.length; i++) {
         values[i] = (BString)this.cellTable.getRowCells(i)[0].saveValue();
      }

      return BFacets.make(this.keys, values);
   }

   private static String text(String s) {
      return LEX.getText(s);
   }

   class BeforeAfterModel extends TableModel {
      public int getRowCount() {
         return BVariablesEditor.this.before.length;
      }

      public int getColumnCount() {
         return 2;
      }

      public String getColumnName(int col) {
         switch (col) {
            case 0:
               return BVariablesEditor.text("boundOrds.relativize.before");
            case 1:
               return BVariablesEditor.text("boundOrds.relativize.after");
            default:
               throw new IllegalStateException();
         }
      }

      public Object getValueAt(int row, int col) {
         switch (col) {
            case 0:
               return BVariablesEditor.this.before[row];
            case 1:
               return BVariablesEditor.this.after[row];
            default:
               throw new IllegalStateException();
         }
      }
   }
}
