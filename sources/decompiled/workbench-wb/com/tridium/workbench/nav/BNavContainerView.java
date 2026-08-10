package com.tridium.workbench.nav;

import javax.baja.gx.BImage;
import javax.baja.nav.BINavNode;
import javax.baja.nre.annotations.AgentOn;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.util.SortUtil;
import javax.baja.sys.BObject;
import javax.baja.sys.Context;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.ui.BMenu;
import javax.baja.ui.HyperlinkInfo;
import javax.baja.ui.event.BMouseEvent;
import javax.baja.ui.table.BTable;
import javax.baja.ui.table.TableController;
import javax.baja.ui.table.TableModel;
import javax.baja.ui.table.TableSubject;
import javax.baja.ui.util.BTitlePane;
import javax.baja.ui.util.UiLexicon;
import javax.baja.workbench.nav.menu.NavMenuUtil;
import javax.baja.workbench.view.BIExportableTableView;
import javax.baja.workbench.view.BWbView;

@NiagaraType(
   agent = {@AgentOn(
      types = {"baja:NavContainer"},
      requiredPermissions = "r"
   )}
)
public class BNavContainerView extends BWbView implements BIExportableTableView {
   public static final Type TYPE = Sys.loadType(BNavContainerView.class);
   static final int COL_NAME = 0;
   static final int COL_DESCRIPTION = 1;
   final UiLexicon lex = UiLexicon.bajaui();
   final String LEX_NAME = this.lex.getText("nav.name");
   final String LEX_DESCRIPTION = this.lex.getText("nav.description");
   BTable table;
   protected BNavContainerView.Row[] rows = new BNavContainerView.Row[0];

   @Override
   public Type getType() {
      return TYPE;
   }

   @Override
   public BTable getExportTable() {
      return this.table;
   }

   @Override
   protected void doLoadValue(BObject value, Context cx) throws Exception {
      BINavNode container = (BINavNode)value;
      String title = container.getNavDisplayName(cx);
      this.table = this.makeNavTable(container);
      this.setContent(BTitlePane.makePane(title, this.table));
   }

   @Override
   protected BObject doSaveValue(BObject value, Context cx) {
      throw new IllegalStateException();
   }

   protected BTable makeNavTable(BINavNode container) {
      BINavNode[] kids = container.getNavChildren();
      this.rows = new BNavContainerView.Row[kids.length];

      for (int i = 0; i < kids.length; i++) {
         this.rows[i] = new BNavContainerView.Row(kids[i]);
      }

      return new BTable(new BNavContainerView.Model(), new BNavContainerView.Controller());
   }

   protected class Controller extends TableController {
      public Controller() {
      }

      public void cellDoubleClicked(BMouseEvent event, int row, int col) {
         BNavContainerView.this.getWbShell().hyperlink(new HyperlinkInfo(BNavContainerView.this.rows[row].node.getNavOrd(), event));
      }

      protected BMenu makePopup(TableSubject subject) {
         return NavMenuUtil.makeMenu(this.getTable(), subject);
      }
   }

   protected class Model extends TableModel {
      public Model() {
      }

      public int getRowCount() {
         return BNavContainerView.this.rows.length;
      }

      public int getColumnCount() {
         return 2;
      }

      public String getColumnName(int col) {
         switch (col) {
            case 0:
               return BNavContainerView.this.LEX_NAME;
            case 1:
               return BNavContainerView.this.LEX_DESCRIPTION;
            default:
               return "?";
         }
      }

      public Object getSubject(int row) {
         return BNavContainerView.this.rows[row].node;
      }

      public Object getValueAt(int row, int col) {
         return BNavContainerView.this.rows[row].getValueAt(col);
      }

      public boolean isColumnSortable(int col) {
         return true;
      }

      public void sortByColumn(int col, boolean ascending) {
         Object[] keys = new Object[BNavContainerView.this.rows.length];

         for (int i = 0; i < keys.length; i++) {
            keys[i] = BNavContainerView.this.rows[i].getSortKey(col);
         }

         SortUtil.sort(keys, BNavContainerView.this.rows, ascending);
      }

      public BImage getRowIcon(int row) {
         return BNavContainerView.this.rows[row].icon;
      }
   }

   protected static class Row {
      BINavNode node;
      String name;
      String description;
      BImage icon;

      public Row(BINavNode node) {
         this.node = node;
         this.name = node.getNavDisplayName(null);
         this.description = node.getNavDescription(null);
         if (this.description == null) {
            this.description = "";
         }

         this.icon = BImage.make(node.getNavIcon());
      }

      public Object getValueAt(int col) {
         switch (col) {
            case 0:
               return this.name;
            case 1:
               return this.description;
            default:
               return "?";
         }
      }

      public Object getSortKey(int col) {
         return this.getValueAt(col);
      }
   }
}
