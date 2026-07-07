package com.tridium.template.ui;

import com.tridium.template.ui.file.TmplUtil;
import java.util.ArrayList;
import java.util.Collections;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BDynamicEnum;
import javax.baja.sys.BValue;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.ui.BDialog;
import javax.baja.ui.BLabel;
import javax.baja.ui.BWidget;
import javax.baja.ui.enums.BHalign;
import javax.baja.ui.event.BFocusEvent;
import javax.baja.ui.event.BMouseEvent;
import javax.baja.ui.event.BWidgetEvent;
import javax.baja.ui.event.WidgetSubscriber;
import javax.baja.ui.pane.BBorderPane;
import javax.baja.ui.pane.BGridPane;
import javax.baja.ui.table.BTable;
import javax.baja.ui.table.TableController;
import javax.baja.ui.table.TableModel;
import javax.baja.ui.table.TableSelection;
import javax.baja.util.Lexicon;
import javax.baja.workbench.fieldeditor.BWbFieldEditor;

@NiagaraType
public class BSelectOutputDialog extends BSelectFromListDialog {
   public static final Type TYPE = Sys.loadType(BSelectOutputDialog.class);
   private static Lexicon lex = Lexicon.make("template");
   private BWbFieldEditor targetSlotEditor;
   private BSelectOutputDialog.Subscriber subscriber;
   protected ArrayList<TmplUtil.TargetChoice> choices;

   @Override
   public Type getType() {
      return TYPE;
   }

   public BSelectOutputDialog() {
   }

   public BSelectOutputDialog(
      BWidget parent,
      String title,
      String subtitle,
      String info,
      boolean isInput,
      boolean allowMultiple,
      ArrayList<TmplUtil.TargetChoice> targetChoices,
      boolean showDontAsk
   ) {
      super(parent, title, subtitle, info, isInput, false, allowMultiple, targetChoices, false, showDontAsk);
   }

   @Override
   protected BSelectFromListDialog.SelectModel makeModel(ArrayList list) {
      return new BSelectOutputDialog.SelectOutputModel(list);
   }

   @Override
   protected TableController makeController() {
      return new BSelectOutputDialog.SelectOutputController();
   }

   @Override
   protected BTable makeTable(TableModel model) {
      return new BTable(model);
   }

   @Override
   protected void initHeader() {
      this.targetSlotEditor = BWbFieldEditor.makeFor(BDynamicEnum.DEFAULT);
      this.header.add(null, new BLabel(this.isInput ? lex.getText("selectOutputDialog.source.header") : lex.getText("selectOutputDialog.target.header")));
      this.header.add(null, this.targetSlotEditor);
      this.subscriber = new BSelectOutputDialog.Subscriber(this);
      this.subscriber.subscribe(this.targetSlotEditor);
   }

   @Override
   protected void searchUpdate() {
      ArrayList<TmplUtil.TargetChoice> filteredChoices = new ArrayList<>();

      for (TmplUtil.TargetChoice targetChoice : this.choiceList) {
         String slotPath = targetChoice.targetPoint.getSlotPath().toString();
         if (this.searchEntry.isEmpty() || slotPath.contains(this.searchEntry)) {
            filteredChoices.add(targetChoice);
         }
      }

      ((BSelectOutputDialog.SelectOutputModel)this.tableModel).setTargetChoices(filteredChoices);
      this.tableModel.updateTable(true);
   }

   private class SelectOutputController extends BSelectFromListDialog.SelectController {
      private SelectOutputController() {
      }

      public void focusGained(BFocusEvent event) {
         this.getTable().repaint();
      }

      protected void checkSelection(BMouseEvent event, int row) {
         super.checkSelection(event, row);
         TableSelection cacSel = BSelectOutputDialog.this.choicesTable.getSelection();
         int selRow = cacSel.getRow();
         if (selRow >= 0) {
            BSelectOutputDialog.this.targetSlotEditor.loadValue((BDynamicEnum)BSelectOutputDialog.this.tableModel.getValueAt(row, 2));
         }
      }

      protected void cellPressed(BMouseEvent event, int row, int column) {
         super.cellPressed(event, row, column);
         if (column == 1) {
            TmplUtil.TargetChoice entry = (TmplUtil.TargetChoice)BSelectOutputDialog.this.tableModel.getSubject(row);
            entry.selected = !entry.selected;
            if (entry.selected && !BSelectOutputDialog.this.allowMultiple) {
               for (TmplUtil.TargetChoice filteredChoice : BSelectOutputDialog.this.choices) {
                  if (filteredChoice != entry) {
                     filteredChoice.selected = false;
                  }
               }
            }
         }
      }

      protected void cellDoubleClicked(BMouseEvent event, int row, int column) {
         if (column == 2) {
            TmplUtil.TargetChoice choice = (TmplUtil.TargetChoice)BSelectOutputDialog.this.tableModel.getSubject(row);
            BDynamicEnum targetSlotEnum = choice.targetSlotEnum;
            BWbFieldEditor editor = BWbFieldEditor.makeFor(targetSlotEnum);
            editor.loadValue(targetSlotEnum);
            BGridPane grid = new BGridPane(1);
            grid.add(null, new BLabel(BSelectOutputDialog.lex.getText("selectOutputDialog.slot")));
            grid.add(null, editor);
            BBorderPane pane = new BBorderPane(grid, 10.0, 10.0, 10.0, 10.0);
            String message = BSelectOutputDialog.this.isInput
               ? BSelectOutputDialog.lex.getText("selectOutputDialog.selectSourceSlot")
               : BSelectOutputDialog.lex.getText("selectOutputDialog.selectTargetSlot");
            if (1 != BDialog.open(BSelectOutputDialog.this.getOwner(), message, pane, 3)) {
               return;
            }

            BValue newValue = null;

            try {
               newValue = editor.saveValue().asValue();
            } catch (Exception var12) {
               var12.printStackTrace();
            }

            if (newValue == null) {
               return;
            }

            choice.targetSlotEnum = (BDynamicEnum)newValue;
            BSelectOutputDialog.this.targetSlotEditor.loadValue((BDynamicEnum)BSelectOutputDialog.this.tableModel.getValueAt(row, 2));
         }
      }
   }

   private class SelectOutputModel extends BSelectFromListDialog.SelectModel {
      public SelectOutputModel() {
      }

      public SelectOutputModel(ArrayList list) {
         BSelectOutputDialog.this.choices = (ArrayList<TmplUtil.TargetChoice>)list.clone();
      }

      @Override
      public Object getSubject(int row) {
         return BSelectOutputDialog.this.choices.get(row);
      }

      @Override
      public ArrayList getChoices() {
         ArrayList<TmplUtil.TargetChoice> selectedComps = new ArrayList<>();

         for (TmplUtil.TargetChoice filteredChoice : BSelectOutputDialog.this.choices) {
            if (filteredChoice.selected) {
               selectedComps.add(filteredChoice);
            }
         }

         return selectedComps;
      }

      public void setTargetChoices(ArrayList<TmplUtil.TargetChoice> newChoices) {
         BSelectOutputDialog.this.choices = newChoices;
      }

      public BHalign getColumnAlignment(int col) {
         return col == 1 ? BHalign.center : BHalign.left;
      }

      @Override
      public int getRowCount() {
         return BSelectOutputDialog.this.choices.size();
      }

      @Override
      public int getColumnCount() {
         return 3;
      }

      @Override
      public String getColumnName(int index) {
         switch (index) {
            case 0:
               return BSelectOutputDialog.this.isInput
                  ? BSelectOutputDialog.lex.getText("selectOutputDialog.source.column")
                  : BSelectOutputDialog.lex.getText("selectOutputDialog.target.column");
            case 1:
               return BSelectOutputDialog.lex.getText("selectInputDialog.select.column");
            case 2:
               return BSelectOutputDialog.this.isInput
                  ? BSelectOutputDialog.lex.getText("selectOutputDialog.sourceSlot.column")
                  : BSelectOutputDialog.lex.getText("selectOutputDialog.targetSlot.column");
            default:
               return "";
         }
      }

      @Override
      public Object getValueAt(int row, int col) {
         TmplUtil.TargetChoice targetChoice = BSelectOutputDialog.this.choices.get(row);
         switch (col) {
            case 0:
               return targetChoice.getSelectString();
            case 1:
               return targetChoice.selected ? "X" : "";
            case 2:
               return targetChoice.targetSlotEnum;
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
         if (BSelectOutputDialog.this.choices != null) {
            if (ascending) {
               Collections.sort(BSelectOutputDialog.this.choices);
            } else {
               Collections.reverse(BSelectOutputDialog.this.choices);
            }
         }
      }
   }

   class Subscriber extends WidgetSubscriber {
      BWidget owner;

      Subscriber(BWidget owner) {
         this.owner = owner;
      }

      public void modified(BWidgetEvent event) {
         if (event.getWidget().equals(BSelectOutputDialog.this.targetSlotEditor)) {
            try {
               BDynamicEnum saveValue = (BDynamicEnum)BSelectOutputDialog.this.targetSlotEditor.saveValue();
               int rowCount = BSelectOutputDialog.this.tableModel.getRowCount();
               TableSelection selection = BSelectOutputDialog.this.choicesTable.getSelection();
               int row = selection.getRow();
               if (row >= 0) {
                  ((TmplUtil.TargetChoice)BSelectOutputDialog.this.choiceList.get(row)).targetSlotEnum = saveValue;
                  BSelectOutputDialog.this.btnSave.setEnabled(true);
               }
            } catch (Exception var6) {
               var6.printStackTrace();
            }
         }
      }
   }
}
