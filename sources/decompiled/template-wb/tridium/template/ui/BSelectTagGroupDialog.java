package com.tridium.template.ui;

import com.tridium.tagdictionary.util.TagDictionaryUtil.ComponentTagGroupChoices;
import java.util.ArrayList;
import java.util.Collections;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BComponent;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.ui.BWidget;
import javax.baja.ui.enums.BHalign;
import javax.baja.ui.event.BFocusEvent;
import javax.baja.ui.event.BMouseEvent;
import javax.baja.ui.table.BTable;
import javax.baja.ui.table.TableController;
import javax.baja.ui.table.TableModel;
import javax.baja.util.Lexicon;

@NiagaraType
public class BSelectTagGroupDialog extends BSelectFromListDialog {
   public static final Type TYPE = Sys.loadType(BSelectTagGroupDialog.class);
   private static Lexicon lex = Lexicon.make("template");
   protected int COL_CONVERT = 3;
   protected int COL_REMOVE_TAG = 4;
   private ArrayList<ComponentTagGroupChoices> filteredChoices;

   @Override
   public Type getType() {
      return TYPE;
   }

   public BSelectTagGroupDialog() {
   }

   public BSelectTagGroupDialog(
      BWidget parent,
      String title,
      String subtitle,
      String info,
      boolean isInput,
      boolean isRelation,
      boolean allowMultiple,
      ArrayList choiceList,
      boolean allowRemember
   ) {
      super(parent, title, subtitle, info, isInput, isRelation, allowMultiple, choiceList, allowRemember, allowRemember);
   }

   @Override
   protected BSelectFromListDialog.SelectModel makeModel(ArrayList list) {
      return new BSelectTagGroupDialog.SelectTagGroupModel(list);
   }

   @Override
   protected TableController makeController() {
      return new BSelectTagGroupDialog.SelectTagGroupController();
   }

   @Override
   protected BTable makeTable(TableModel model) {
      BTable table = new BTable(model);
      table.setMultipleSelection(true);
      return table;
   }

   @Override
   protected void initHeader() {
   }

   @Override
   protected boolean showSearch() {
      return false;
   }

   @Override
   protected ArrayList makeChoiceArray(ArrayList list) {
      if (list.size() == 1) {
         for (Object tgc : list) {
            if (tgc instanceof ComponentTagGroupChoices) {
               ((ComponentTagGroupChoices)tgc).setSelected(true);
            }
         }
      }

      return list;
   }

   private void updateFilteredChoices(String filter) {
      if (this.filteredChoices == null) {
         this.filteredChoices = new ArrayList<>();
      }

      this.filteredChoices.clear();

      for (ComponentTagGroupChoices entry : this.choiceList) {
         if (filter.isEmpty() || entry.getTagGroupInfo().getSlotPath().toString().contains(filter)) {
            this.filteredChoices.add(entry);
         }
      }
   }

   @Override
   public ArrayList<BComponent> getSelectedResults() {
      ArrayList<BComponent> list = new ArrayList<>();

      for (ComponentTagGroupChoices filteredChoice : this.filteredChoices) {
         if (filteredChoice.isSelected()) {
            list.add(filteredChoice.getTagGroupInfo());
         }
      }

      return list;
   }

   @Override
   protected void searchUpdate() {
      this.updateFilteredChoices(this.searchEntry);
      this.tableModel.updateTable(true);
   }

   private class SelectTagGroupController extends BSelectFromListDialog.SelectController {
      boolean actionCol = false;
      int[] selRows = null;

      private SelectTagGroupController() {
      }

      public void focusGained(BFocusEvent event) {
         this.getTable().repaint();
      }

      protected void checkSelection(BMouseEvent event, int row) {
         super.checkSelection(event, row);
         if (this.actionCol && this.selRows != null) {
            if (this.selRows.length == 1 && row == this.selRows[0]) {
               this.getSelection().deselect(row);
            } else {
               for (int selRow : this.selRows) {
                  if (selRow == row) {
                     this.getSelection().select(this.selRows);
                     break;
                  }
               }
            }
         }
      }

      protected void cellPressed(BMouseEvent event, int row, int column) {
         super.cellPressed(event, row, column);
         ComponentTagGroupChoices entry = (ComponentTagGroupChoices)BSelectTagGroupDialog.this.tableModel.getSubject(row);
         this.selRows = this.getSelection().getRows();
         if (this.selRows.length == 0) {
            this.selRows = new int[1];
            this.selRows[0] = row;
         }

         if (column == BSelectTagGroupDialog.this.COL_REMOVE_TAG) {
            for (int i = 0; i < this.selRows.length; i++) {
               ComponentTagGroupChoices selEntry0 = (ComponentTagGroupChoices)BSelectTagGroupDialog.this.tableModel.getSubject(this.selRows[0]);
               ComponentTagGroupChoices selEntry = (ComponentTagGroupChoices)BSelectTagGroupDialog.this.tableModel.getSubject(this.selRows[i]);
               if (selEntry.isSelected()) {
                  selEntry.setRemoveTags(true);
               } else if (i == 0) {
                  selEntry.setRemoveTags(!selEntry.isRemoveTags());
               } else {
                  selEntry.setRemoveTags(selEntry0.isRemoveTags());
               }
            }
         } else if (column == BSelectTagGroupDialog.this.COL_CONVERT) {
            for (int ix = 0; ix < this.selRows.length; ix++) {
               ComponentTagGroupChoices selEntry0 = (ComponentTagGroupChoices)BSelectTagGroupDialog.this.tableModel.getSubject(this.selRows[0]);
               ComponentTagGroupChoices selEntry = (ComponentTagGroupChoices)BSelectTagGroupDialog.this.tableModel.getSubject(this.selRows[ix]);
               if (ix == 0) {
                  selEntry.setSelected(!selEntry0.isSelected());
               } else {
                  selEntry.setSelected(selEntry0.isSelected());
               }

               selEntry.setRemoveTags(selEntry.isSelected());
            }
         }

         if (column < BSelectTagGroupDialog.this.COL_CONVERT && this.selRows.length == 1) {
            this.actionCol = false;
         } else {
            if (this.selRows.length == 1) {
               this.getSelection().deselect(this.selRows[0]);
            }

            this.actionCol = true;
         }
      }
   }

   private class SelectTagGroupModel extends BSelectFromListDialog.SelectModel {
      public SelectTagGroupModel() {
      }

      public SelectTagGroupModel(ArrayList list) {
         BSelectTagGroupDialog.this.filteredChoices = (ArrayList<ComponentTagGroupChoices>)list.clone();
      }

      @Override
      public Object getSubject(int row) {
         return BSelectTagGroupDialog.this.filteredChoices.get(row);
      }

      public BHalign getColumnAlignment(int col) {
         switch (col) {
            case 1:
               return BHalign.right;
            case 2:
            default:
               return BHalign.left;
            case 3:
            case 4:
               return BHalign.center;
         }
      }

      @Override
      public int getRowCount() {
         return BSelectTagGroupDialog.this.filteredChoices.size();
      }

      @Override
      public int getColumnCount() {
         return 7;
      }

      @Override
      public String getColumnName(int index) {
         switch (index) {
            case 0:
               return BSelectTagGroupDialog.lex.getText("selectTagGroupDialog.component.column");
            case 1:
               return BSelectTagGroupDialog.lex.getText("selectTagGroupDialog.directTags.column");
            case 2:
               return BSelectTagGroupDialog.lex.getText("selectTagGroupDialog.tagGroup.column");
            case 3:
               return BSelectTagGroupDialog.lex.getText("selectTagGroupDialog.convert.column");
            case 4:
               return BSelectTagGroupDialog.lex.getText("selectTagGroupDialog.removeTags.column");
            default:
               return "";
         }
      }

      @Override
      public Object getValueAt(int row, int col) {
         ComponentTagGroupChoices entry = BSelectTagGroupDialog.this.filteredChoices.get(row);
         switch (col) {
            case 0:
               if (row != 0 && BSelectTagGroupDialog.this.filteredChoices.get(row - 1).getComponent().equals(entry.getComponent())) {
                  return "";
               }

               return entry.getComponent().getSlotPath();
            case 1:
               return entry.getTagsString();
            case 2:
               return entry.getTagGroupInfo().getGroupId();
            case 3:
               return entry.isSelected() ? "X" : "";
            case 4:
               return entry.isRemoveTags() ? "X" : "";
            default:
               return "";
         }
      }

      @Override
      public boolean isColumnSortable(int col) {
         return col == 0;
      }

      @Override
      public synchronized void sortByColumn(int col, boolean ascending) {
         if (BSelectTagGroupDialog.this.filteredChoices != null) {
            if (ascending) {
               Collections.sort(BSelectTagGroupDialog.this.filteredChoices);
            } else {
               Collections.reverse(BSelectTagGroupDialog.this.filteredChoices);
            }
         }
      }

      @Override
      public ArrayList getChoices() {
         ArrayList<ComponentTagGroupChoices> selectedComps = new ArrayList<>();

         for (ComponentTagGroupChoices filteredChoice : BSelectTagGroupDialog.this.filteredChoices) {
            if (filteredChoice.isSelected()) {
               selectedComps.add(filteredChoice);
            }
         }

         return selectedComps;
      }
   }
}
