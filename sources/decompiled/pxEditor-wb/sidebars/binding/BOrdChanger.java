package com.tridium.px.editor.sidebars.binding;

import com.tridium.px.editor.util.Reflector;
import com.tridium.ui.theme.Theme;
import java.util.HashSet;
import java.util.Set;
import javax.baja.gx.BBrush;
import javax.baja.gx.BImage;
import javax.baja.gx.Graphics;
import javax.baja.naming.BOrd;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.px.editor.BPxEditor;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.ui.BDialog;
import javax.baja.ui.BMenu;
import javax.baja.ui.event.BMouseEvent;
import javax.baja.ui.pane.BConstrainedPane;
import javax.baja.ui.pane.BEdgePane;
import javax.baja.ui.pane.BScrollPane;
import javax.baja.ui.table.BTable;
import javax.baja.ui.table.TableCellRenderer;
import javax.baja.ui.table.TableController;
import javax.baja.ui.table.TableModel;
import javax.baja.ui.table.TableCellRenderer.Cell;
import javax.baja.util.Lexicon;

@NiagaraType
public abstract class BOrdChanger extends BDialog {
   public static final Type TYPE = Sys.loadType(BOrdChanger.class);
   protected static Lexicon lexicon = Lexicon.make("pxEditor");
   protected int checkBoxColumn = 0;
   protected int beforeColumn = 1;
   protected int afterColumn = 2;
   protected int totalColumns = 3;
   private static final BImage OK = icon("boundOrds.ok");
   private static final BImage STOP = icon("boundOrds.stop");
   BPxEditor editor;
   protected BEdgePane edge = new BEdgePane();
   BOrd[] before;
   BOrd[] after;
   boolean[] selected;
   BTable table = new BTable();

   public Type getType() {
      return TYPE;
   }

   public BOrdChanger(BPxEditor editor, BOrd[] before) {
      this.editor = editor;
      this.before = before;
      this.after = new BOrd[before.length];
      this.selected = new boolean[before.length];
      BConstrainedPane cons = new BConstrainedPane(new BScrollPane(this.table));
      cons.setMaxHeight(400.0);
      this.edge.setCenter(cons);
      this.setContent(this.edge);
   }

   protected abstract BOrd after(BOrd var1);

   protected abstract boolean selectable(int var1);

   protected final void selectIfSelectable(int row, boolean isSelected) {
      this.selected[row] = isSelected && this.selectable(row);
   }

   protected void init() {
      for (int i = 0; i < this.before.length; i++) {
         this.after[i] = this.after(this.before[i]);
         this.selected[i] = true;
      }

      this.table.setModel(new BOrdChanger.Model());
      this.table.setController(new BOrdChanger.Controller());
      this.table.setCellRenderer(new BOrdChanger.Renderer());
   }

   Set<BOrd> selected() {
      Set<BOrd> set = new HashSet<>();

      for (int i = 0; i < this.before.length; i++) {
         if (this.selected[i]) {
            set.add(this.before[i]);
         }
      }

      return set;
   }

   protected static String text(String s) {
      return lexicon.getText(s);
   }

   private static BImage icon(String s) {
      return BImage.make(lexicon.getText(s));
   }

   class Controller extends TableController {
      protected void cellPressed(BMouseEvent event, int row, int column) {
         if (BOrdChanger.this.selectable(row)) {
            if (column != 0) {
               super.cellPressed(event, row, column);
            } else {
               this.toggleRow(row);
            }
         }
      }

      protected void cellDoubleClicked(BMouseEvent event, int row, int column) {
         this.toggleRow(row);
      }

      protected void toggleRow(int row) {
         BOrdChanger.this.selected[row] = !BOrdChanger.this.selected[row];
         BOrdChanger.this.after[row] = BOrdChanger.this.selected[row] ? BOrdChanger.this.after(BOrdChanger.this.before[row]) : BOrdChanger.this.before[row];
         BOrdChanger.this.repaint();
      }

      protected BMenu makeOptionsMenu() {
         return Reflector.optionsMenu(this.getTable());
      }
   }

   class Model extends TableModel {
      public int getRowCount() {
         return BOrdChanger.this.selected.length;
      }

      public int getColumnCount() {
         return BOrdChanger.this.totalColumns;
      }

      public String getColumnName(int col) {
         if (col == BOrdChanger.this.checkBoxColumn) {
            return "  ";
         } else if (col == BOrdChanger.this.beforeColumn) {
            return BOrdChanger.text("boundOrds.relativize.before");
         } else if (col == BOrdChanger.this.afterColumn) {
            return BOrdChanger.text("boundOrds.relativize.after");
         } else {
            throw new IllegalStateException();
         }
      }

      public Object getValueAt(int row, int col) {
         if (col == BOrdChanger.this.checkBoxColumn) {
            return "";
         } else if (col == BOrdChanger.this.beforeColumn) {
            return BOrdChanger.this.before[row];
         } else if (col == BOrdChanger.this.afterColumn) {
            return BOrdChanger.this.after[row];
         } else {
            throw new IllegalStateException();
         }
      }
   }

   class Renderer extends TableCellRenderer {
      public BBrush getSelectionForeground(Cell cell) {
         return this.getForeground(cell);
      }

      public BBrush getSelectionBackground(Cell cell) {
         return this.getBackground(cell);
      }

      public BBrush getForeground(Cell cell) {
         return BOrdChanger.this.selectable(cell.row) && BOrdChanger.this.selected[cell.row] ? super.getForeground(cell) : Theme.widget().getControlShadow();
      }

      public void paintCell(Graphics g, Cell cell) {
         if (cell.column == BOrdChanger.this.checkBoxColumn) {
            this.paintCellBackground(g, cell);
            if (BOrdChanger.this.selectable(cell.row)) {
               if (BOrdChanger.this.selected[cell.row]) {
                  double x = 2.0;
                  double y = (cell.height - 16.0) / 2.0;
                  g.drawImage(BOrdChanger.OK, x, y);
               }
            } else {
               double x = 2.0;
               double y = (cell.height - 16.0) / 2.0;
               g.drawImage(BOrdChanger.STOP, x, y);
            }
         } else {
            super.paintCell(g, cell);
         }
      }

      public double getPreferredCellWidth(Cell cell) {
         if (cell.column == 0) {
            return super.getPreferredCellWidth(cell);
         } else {
            String s = BOrdChanger.this.before[cell.row].toString();
            return Theme.table().getCellFont().width(s) + 12.0;
         }
      }
   }
}
