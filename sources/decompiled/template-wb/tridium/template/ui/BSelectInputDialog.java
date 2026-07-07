package com.tridium.template.ui;

import java.util.ArrayList;
import java.util.Collections;
import javax.baja.naming.SlotPath;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BComponent;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.ui.BLabel;
import javax.baja.ui.BWidget;
import javax.baja.ui.enums.BHalign;
import javax.baja.ui.event.BFocusEvent;
import javax.baja.ui.event.BMouseEvent;
import javax.baja.ui.table.BTable;
import javax.baja.ui.table.TableController;
import javax.baja.ui.table.TableModel;
import javax.baja.ui.table.TableSelection;
import javax.baja.util.Lexicon;

@NiagaraType
public class BSelectInputDialog extends BSelectFromListDialog {
   public static final Type TYPE = Sys.loadType(BSelectInputDialog.class);
   private static Lexicon lex = Lexicon.make("template");
   private ArrayList<BSelectInputDialog.ComponentEntry> filteredChoices;

   @Override
   public Type getType() {
      return TYPE;
   }

   public BSelectInputDialog() {
   }

   public BSelectInputDialog(
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
      return new BSelectInputDialog.SelectInputModel(list);
   }

   @Override
   protected TableController makeController() {
      return new BSelectInputDialog.SelectInputController();
   }

   @Override
   protected BTable makeTable(TableModel model) {
      return new BTable(model);
   }

   @Override
   protected void initHeader() {
      this.header.add(null, new BLabel("  " + lex.getText("selectTagGroupDialog.clickToSelect")));
   }

   @Override
   protected ArrayList makeChoiceArray(ArrayList components) {
      ArrayList list = new ArrayList();
      boolean onlyOne = components.size() == 1;

      for (Object comp : components) {
         if (comp instanceof BComponent) {
            list.add(new BSelectInputDialog.ComponentEntry((BComponent)comp, onlyOne));
         }
      }

      return list;
   }

   private void updateFilteredChoices(String filter) {
      if (this.filteredChoices == null) {
         this.filteredChoices = new ArrayList<>();
      }

      this.filteredChoices.clear();

      for (BSelectInputDialog.ComponentEntry entry : this.choiceList) {
         if (filter.isEmpty() || entry.getComponent().getSlotPath().toString().contains(filter)) {
            this.filteredChoices.add(entry);
         }
      }
   }

   @Override
   public ArrayList<BComponent> getSelectedResults() {
      ArrayList<BComponent> list = new ArrayList<>();

      for (BSelectInputDialog.ComponentEntry filteredChoice : this.filteredChoices) {
         if (filteredChoice.isSelected()) {
            list.add(filteredChoice.getComponent());
         }
      }

      return list;
   }

   @Override
   protected void searchUpdate() {
      this.updateFilteredChoices(this.searchEntry);
      this.tableModel.updateTable(true);
   }

   public class ComponentEntry implements Comparable<BSelectInputDialog.ComponentEntry> {
      BComponent component;
      boolean selected;

      ComponentEntry(BComponent component, boolean selected) {
         this.component = component;
         this.selected = selected;
      }

      BComponent getComponent() {
         return this.component;
      }

      boolean isSelected() {
         return this.selected;
      }

      void setComponent(BComponent component) {
         this.component = component;
      }

      void setSelected(boolean selected) {
         this.selected = selected;
      }

      String getSelectString() {
         return SlotPath.unescape(this.component.getSlotPath().toString());
      }

      public int compareTo(BSelectInputDialog.ComponentEntry componentEntry) {
         return this.getSelectString().compareTo(componentEntry.getSelectString());
      }
   }

   private class SelectInputController extends BSelectFromListDialog.SelectController {
      private SelectInputController() {
      }

      public void focusGained(BFocusEvent event) {
         this.getTable().repaint();
      }

      protected void checkSelection(BMouseEvent event, int row) {
         super.checkSelection(event, row);
         TableSelection cacSel = BSelectInputDialog.this.tableModel.getSelection();
         cacSel.deselectAll();
      }

      protected void cellPressed(BMouseEvent event, int row, int column) {
         super.cellPressed(event, row, column);
         BSelectInputDialog.ComponentEntry entry = (BSelectInputDialog.ComponentEntry)BSelectInputDialog.this.tableModel.getSubject(row);
         entry.selected = !entry.selected;
         if (entry.selected && !BSelectInputDialog.this.allowMultiple) {
            for (BSelectInputDialog.ComponentEntry filteredChoice : BSelectInputDialog.this.filteredChoices) {
               if (filteredChoice != entry) {
                  filteredChoice.selected = false;
               }
            }
         }

         BSelectInputDialog.this.choicesTable.getSelection().deselectAll();
      }
   }

   private class SelectInputModel extends BSelectFromListDialog.SelectModel {
      public SelectInputModel() {
      }

      public SelectInputModel(ArrayList list) {
         BSelectInputDialog.this.filteredChoices = (ArrayList<BSelectInputDialog.ComponentEntry>)list.clone();
      }

      @Override
      public Object getSubject(int row) {
         return BSelectInputDialog.this.filteredChoices.get(row);
      }

      public BHalign getColumnAlignment(int col) {
         return col == 1 ? BHalign.center : BHalign.left;
      }

      @Override
      public int getRowCount() {
         return BSelectInputDialog.this.filteredChoices.size();
      }

      @Override
      public int getColumnCount() {
         return 3;
      }

      @Override
      public String getColumnName(int index) {
         switch (index) {
            case 0:
               return BSelectInputDialog.this.isInput
                  ? BSelectInputDialog.lex.getText("selectInputDialog.source.column")
                  : BSelectInputDialog.lex.getText("selectInputDialog.target.column");
            case 1:
               return BSelectInputDialog.lex.getText("selectInputDialog.select.column");
            default:
               return "";
         }
      }

      @Override
      public Object getValueAt(int row, int col) {
         BSelectInputDialog.ComponentEntry entry = BSelectInputDialog.this.filteredChoices.get(row);
         switch (col) {
            case 0:
               return entry.getSelectString();
            case 1:
               return entry.selected ? "X" : "";
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
         if (BSelectInputDialog.this.filteredChoices != null) {
            if (ascending) {
               Collections.sort(BSelectInputDialog.this.filteredChoices);
            } else {
               Collections.reverse(BSelectInputDialog.this.filteredChoices);
            }
         }
      }

      @Override
      public ArrayList getChoices() {
         ArrayList<BComponent> selectedComps = new ArrayList<>();

         for (BSelectInputDialog.ComponentEntry filteredChoice : BSelectInputDialog.this.filteredChoices) {
            if (filteredChoice.selected) {
               selectedComps.add(filteredChoice.component);
            }
         }

         return selectedComps;
      }
   }
}
