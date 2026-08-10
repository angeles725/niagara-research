package com.tridium.program.ui;

import com.tridium.security.BPermissionGroupInfo;
import javax.baja.ui.BMenu;
import javax.baja.ui.event.BMouseEvent;
import javax.baja.ui.table.BTable;
import javax.baja.ui.table.TableController;
import javax.baja.ui.table.TableModel;
import javax.baja.ui.table.TableSelection;
import javax.baja.util.Lexicon;

public class Permissions extends TableModel {
   BProgramModuleBuilder builder;
   BTable table;
   private Lexicon bajaLex = null;

   Permissions(BProgramModuleBuilder builder) {
      this.builder = builder;
      this.table = new BTable(this);
      this.table.setController(new Permissions.Controller());
      builder.linkTo(this.table, BTable.selectionModified, BProgramModuleBuilder.handlePermissionSelection);
   }

   public int getRowCount() {
      return ((BPermissionGroupInfo[])this.builder.pmod.getPermissions().getChildren(BPermissionGroupInfo.class)).length;
   }

   public int getColumnCount() {
      return 5;
   }

   public String getColumnName(int col) {
      if (this.bajaLex == null) {
         this.bajaLex = Lexicon.make("baja");
      }

      switch (col) {
         case 0:
            return this.bajaLex.get("permissions.type", "Type");
         case 1:
            return this.bajaLex.get("permissions.policyType", "Policy Type");
         case 2:
            return this.bajaLex.get("permissions.purpose", "Purpose");
         case 3:
            return this.bajaLex.get("permissions.params", "Parameters");
         case 4:
            return this.bajaLex.get("permissions.required", "Required");
         default:
            return "Error: unknown column name";
      }
   }

   public Object getValueAt(int row, int col) {
      BPermissionGroupInfo[] permissionGroups = (BPermissionGroupInfo[])this.builder.pmod.getPermissions().getChildren(BPermissionGroupInfo.class);
      BPermissionGroupInfo permissionGroup = permissionGroups[row];
      switch (col) {
         case 0:
            return permissionGroup.getGroupType();
         case 1:
            return permissionGroup.getPolicyType();
         case 2:
            return permissionGroup.getPurpose();
         case 3:
            return permissionGroup.getParameters();
         case 4:
            return permissionGroup.getRequired();
         default:
            return "Error: unknown column";
      }
   }

   void add(BPermissionGroupInfo permissionGroup) {
      this.builder.pmod.getPermissions().add("permissionGroup?", permissionGroup);
      this.updateTable();
   }

   BPermissionGroupInfo getSelectedPermission() {
      TableSelection selection = this.builder.permissions.table.getSelection();
      int row = selection.getRow();
      return row >= 0 ? ((BPermissionGroupInfo[])this.builder.pmod.getPermissions().getChildren(BPermissionGroupInfo.class))[row] : null;
   }

   class Controller extends TableController {
      public void cellPopup(BMouseEvent event, int row, int column) {
         this.popup(event);
      }

      public void backgroundPopup(BMouseEvent event) {
         this.popup(event);
      }

      public void popup(BMouseEvent event) {
         BMenu menu = new BMenu();
         menu.add(null, Permissions.this.builder.commands.addPermissionCommand);
         menu.add(null, Permissions.this.builder.commands.editPermissionCommand);
         menu.add(null, Permissions.this.builder.commands.removePermissionCommand);
         menu.open(Permissions.this.table, event.getX(), event.getY());
      }
   }
}
