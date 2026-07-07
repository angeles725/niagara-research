package com.tridium.template.ui.tag;

import com.tridium.ui.theme.Theme;
import javax.baja.gx.BImage;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.util.SortUtil;
import javax.baja.sys.BComponent;
import javax.baja.sys.BIcon;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.ui.event.BMouseEvent;
import javax.baja.ui.table.DynamicTableModel;
import javax.baja.ui.table.TableCellRenderer.Cell;
import javax.baja.ui.transfer.SimpleDragRenderer;
import javax.baja.ui.treetable.BTreeTable;
import javax.baja.ui.treetable.TreeTableCellRenderer;
import javax.baja.ui.treetable.TreeTableController;
import javax.baja.ui.treetable.TreeTableModel;
import javax.baja.ui.treetable.TreeTableNode;
import javax.baja.ui.treetable.TreeTableSelection;
import javax.baja.workbench.BWbShell;
import javax.baja.workbench.mgr.MgrColumn;
import javax.baja.workbench.mgr.MgrColumn.Name;
import javax.baja.workbench.mgr.MgrColumn.PropString;
import javax.baja.workbench.view.BWbView;

@NiagaraType
public class BTagTable extends BTreeTable {
   public static final Type TYPE = Sys.loadType(BTagTable.class);
   protected static final BIcon tagIcon = BIcon.std("tag.png");
   MgrColumn[] cols = new MgrColumn[]{new Name(), new PropString("Kind", "kind", 0), new PropString("TargetType", "targetType", 0)};
   DynamicTableModel dynamicModel;
   BTagTable.Model treeTableModel;
   BWbView view;
   Object[] roots = new Object[0];

   public Type getType() {
      return TYPE;
   }

   public BTagTable() {
   }

   public BTagTable(BWbView view) {
      this.view = view;
      this.treeTableModel = new BTagTable.Model();
      this.dynamicModel = new DynamicTableModel(this.treeTableModel);
      this.updateColumns();
      this.setModel(this.dynamicModel);
      this.setSelection(new BTagTable.Selection());
      this.setController(new BTagTable.Controller());
      this.setCellRenderer(new BTagTable.Renderer());
   }

   public void setRoots(Object[] discovery) {
      this.treeTableModel.setDiscovery(discovery);
   }

   public MgrColumn columnIndexToMgrColumn(int column) {
      return this.cols[this.dynamicModel.toRootColumnIndex(column)];
   }

   public Object getObjectAt(int row) {
      BTagTable.TagTableNode node = (BTagTable.TagTableNode)this.treeTableModel.rowToNode(row);
      return node != null ? node.discovery : null;
   }

   public Object getSelectedObject() {
      int sel = this.getSelection().getRow();
      return sel < 0 ? null : this.getObjectAt(sel);
   }

   public Object[] getSelectedObjects() {
      int[] sel = this.getSelection().getRows();
      Object[] obj = new Object[sel.length];

      for (int i = 0; i < sel.length; i++) {
         obj[i] = this.getObjectAt(sel[i]);
      }

      return obj;
   }

   public void mouseDragStarted(BMouseEvent event) {
      int[] rows = this.getSelection().getRows();
      if (rows.length != 0) {
         int rowY = this.getRowAt(event.getY());
         boolean found = false;

         for (int i = 0; i < rows.length; i++) {
            if (rowY == rows[i]) {
               found = true;
               break;
            }
         }

         if (found) {
            BImage[] icons = new BImage[rows.length];
            String[] text = new String[rows.length];

            for (int ix = 0; ix < rows.length; ix++) {
               int row = rows[ix];
               BTagTable.TagTableNode node = (BTagTable.TagTableNode)this.treeTableModel.rowToNode(row);
               icons[ix] = node.getIcon();
               text[ix] = "" + node.getValueAt(0);
            }

            SimpleDragRenderer dragRenderer = new SimpleDragRenderer(icons, text);
            dragRenderer.font = Theme.table().getCellFont();
         }
      }
   }

   void updateColumns() {
      for (int i = 0; i < this.cols.length; i++) {
         this.dynamicModel.setShowColumn(i, !this.cols[i].isUnseen());
      }
   }

   static BTagTable.TagTableNode[] sort(BTagTable.TagTableNode[] roots, int col, boolean ascending) {
      BTagTable.TagTableNode[] sorted = (BTagTable.TagTableNode[])roots.clone();
      Object[] keys = new Object[sorted.length];

      for (int i = 0; i < keys.length; i++) {
         keys[i] = sorted[i].getValueAt(col);
      }

      SortUtil.sort(keys, sorted, ascending);
      return sorted;
   }

   class Controller extends TreeTableController {
      public void cellDoubleClicked(BMouseEvent event, int row, int col) {
      }
   }

   class Model extends TreeTableModel {
      BTagTable.TagTableNode[] roots = new BTagTable.TagTableNode[0];
      Object[] discovery;

      public void setDiscovery(Object[] nodes) {
         this.discovery = (Object[])nodes.clone();
      }

      public void updateTreeTable(boolean resize) {
         BWbShell shell = BTagTable.this.view.getWbShell();
         shell.enterBusy();

         try {
            BTagTable.TagTableNode[] roots = new BTagTable.TagTableNode[this.discovery.length];

            for (int i = 0; i < roots.length; i++) {
               BTagTable.TagTableNode root = this.discoveryToRoot(this.discovery[i]);
               if (root == null) {
                  root = BTagTable.this.new TagTableNode(this, this.discovery[i]);
               }

               roots[i] = root;
            }

            int sortCol = BTagTable.this.dynamicModel.toRootColumnIndex(BTagTable.this.getSortColumn());
            if (sortCol >= 0) {
               roots = BTagTable.sort(roots, sortCol, BTagTable.this.isSortAscending());
            }

            this.roots = roots;
            super.updateTreeTable(resize);
         } finally {
            shell.exitBusy();
         }
      }

      public int getRootCount() {
         return this.roots.length;
      }

      public TreeTableNode getRoot(int index) {
         return this.roots[index];
      }

      public boolean isDepthExpandable(int depth) {
         return true;
      }

      public int getColumnCount() {
         return BTagTable.this.cols.length;
      }

      public String getColumnName(int col) {
         return BTagTable.this.cols[col].getDisplayName();
      }

      public boolean isColumnSortable(int col) {
         return true;
      }

      public void sortByColumn(int col, boolean ascending) {
         this.getSelection().deselectAll();
         this.roots = BTagTable.sort(this.roots, col, ascending);
         super.updateTreeTable(false);
      }

      BTagTable.TagTableNode discoveryToRoot(Object discovery) {
         BTagTable.TagTableNode[] roots = this.roots;

         for (int i = 0; i < roots.length; i++) {
            if (roots[i].discovery == discovery) {
               return roots[i];
            }
         }

         return null;
      }
   }

   class Renderer extends TreeTableCellRenderer {
      public String getCellText(Cell cell) {
         try {
            Object obj = BTagTable.this.getObjectAt(cell.row);
            MgrColumn col = BTagTable.this.columnIndexToMgrColumn(cell.column);
            return col.toDisplayString(obj, cell.value, BTagTable.this.view.getCurrentContext());
         } catch (Exception var4) {
            return "";
         }
      }
   }

   class Selection extends TreeTableSelection {
      public void updateTable() {
         super.updateTable();
      }
   }

   class TagTableNode extends TreeTableNode {
      Object discovery;
      BTagTable.TagTableNode[] children;
      BImage icon;

      TagTableNode(BTagTable.Model model, Object discovery) {
         super(model);
         this.discovery = discovery;
      }

      TagTableNode(BTagTable.TagTableNode parent, Object discovery) {
         super(parent);
         this.discovery = discovery;
      }

      public Object getSubject() {
         return this.discovery;
      }

      public boolean hasChildren() {
         return this.discovery instanceof BComponent ? ((BComponent)this.discovery).getChildComponents().length > 0 : false;
      }

      public int getChildCount() {
         return this.getChildren().length;
      }

      public TreeTableNode getChild(int index) {
         return this.getChildren()[index];
      }

      TreeTableNode[] getChildren() {
         if (this.children == null) {
            Object[] kidDis = ((BComponent)this.discovery).getChildComponents();
            BTagTable.TagTableNode[] kidNodes = new BTagTable.TagTableNode[kidDis.length];

            for (int i = 0; i < kidNodes.length; i++) {
               kidNodes[i] = BTagTable.this.new TagTableNode(this, kidDis[i]);
            }

            this.children = kidNodes;
         }

         return this.children;
      }

      public Object getValueAt(int col) {
         return BTagTable.this.cols[col].get(this.discovery);
      }

      public BImage getIcon() {
         if (this.icon == null) {
            this.icon = BImage.make(BTagTable.tagIcon);
         }

         return this.icon;
      }
   }
}
