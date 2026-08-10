package com.tridium.workbench.slotsheet;

import com.tridium.ui.theme.Theme;
import javax.baja.gx.BFont;
import javax.baja.gx.BImage;
import javax.baja.nre.annotations.AgentOn;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.util.SortUtil;
import javax.baja.sys.BComponent;
import javax.baja.sys.BComponentEvent;
import javax.baja.sys.BIPropertyContainer;
import javax.baja.sys.BObject;
import javax.baja.sys.Context;
import javax.baja.sys.Flags;
import javax.baja.sys.Property;
import javax.baja.sys.Slot;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.ui.BMenu;
import javax.baja.ui.BSeparator;
import javax.baja.ui.BToolBar;
import javax.baja.ui.CommandArtifact;
import javax.baja.ui.event.BKeyEvent;
import javax.baja.ui.event.BMouseEvent;
import javax.baja.ui.table.BTable;
import javax.baja.ui.table.TableCellRenderer;
import javax.baja.ui.table.TableController;
import javax.baja.ui.table.TableModel;
import javax.baja.ui.table.TableSelection;
import javax.baja.ui.table.TableCellRenderer.Cell;
import javax.baja.ui.util.BTitlePane;
import javax.baja.ui.util.UiLexicon;
import javax.baja.util.BFormat;
import javax.baja.util.BNameMap;
import javax.baja.workbench.view.BWbComponentView;

@NiagaraType(
   agent = {@AgentOn(
      types = {"baja:Component", "baja:IPropertyContainer"},
      requiredPermissions = "W"
   )}
)
public class BSlotSheet extends BWbComponentView {
   public static final Type TYPE = Sys.loadType(BSlotSheet.class);
   static BFont overrideFont = BFont.make(Theme.table().getCellFont().getName(), Theme.table().getCellFont().getSize(), 1);
   final UiLexicon lex = UiLexicon.bajaui();
   final String lexProperty = this.lex.getText("slotsheet.property");
   final String lexAction = this.lex.getText("slotsheet.action");
   final String lexTopic = this.lex.getText("slotsheet.topic");
   final String lexFrozen = this.lex.getText("slotsheet.frozen");
   final String lexDynamic = this.lex.getText("slotsheet.dynamic");
   final String lexSlot = this.lex.getText("slotsheet.slot");
   final String lexIndex = this.lex.getText("slotsheet.index");
   final String lexName = this.lex.getText("slotsheet.name");
   final String lexDisplayName = this.lex.getText("slotsheet.displayName");
   final String lexDefinition = this.lex.getText("slotsheet.definition");
   final String lexFlags = this.lex.getText("slotsheet.flags");
   final String lexType = this.lex.getText("slotsheet.type");
   final String lexFacets = this.lex.getText("slotsheet.facets");
   final String lexNameMap = this.lex.getText("slotsheet.nameMap");
   public static final BImage propertyIcon = BImage.make("module://icons/x16/property.png");
   public static final BImage actionIcon = BImage.make("module://icons/x16/action.png");
   public static final BImage topicIcon = BImage.make("module://icons/x16/topic.png");
   public final SlotSheetCommands commands = new SlotSheetCommands(this);
   BSlotSheet.Model model = new BSlotSheet.Model();
   BTable table;
   BIPropertyContainer component;
   BSlotSheet.SlotEntry[] slots = new BSlotSheet.SlotEntry[0];
   BNameMap displayNames = null;

   @Override
   public Type getType() {
      return TYPE;
   }

   public BSlotSheet() {
      this.autoRegisterForComponentEvents = false;
      UiLexicon lex = UiLexicon.bajaui();
      this.table = new BTable(this.model);
      this.table.setController(new BSlotSheet.Controller());
      this.table.setCellRenderer(new BSlotSheet.Renderer());
      this.table.setSelection(new BSlotSheet.Selection());
      this.setContent(new BTitlePane("Slot Sheet", this.table));
   }

   public BMenu[] getViewMenus() {
      return this.commands.getViewMenus();
   }

   public BToolBar getViewToolBar() {
      return this.commands.getViewToolBar();
   }

   @Override
   public void doLoadValue(BObject value, Context cx) {
      this.component = (BIPropertyContainer)value;
      if (this.component instanceof BComponent) {
         this.registerForComponentEvents((BComponent)this.component);
      }

      this.updateModels();
   }

   @Override
   public BObject doSaveValue(BObject value, Context cx) throws Exception {
      return value;
   }

   @Override
   public CommandArtifact invokeCommand(int id) throws Exception {
      switch (id) {
         case 1:
            return this.commands.copy.doInvoke();
         case 2:
         case 3:
         default:
            return super.invokeCommand(id);
         case 4:
            return this.commands.delete.doInvoke();
         case 5:
            return this.commands.rename.doInvoke();
      }
   }

   @Override
   public void handleComponentEvent(BComponentEvent event) {
      this.updateModels();
   }

   public Slot[] getSelectedSlots() {
      int[] rows = this.table.getSelection().getRows();
      Slot[] selected = new Slot[rows.length];

      for (int i = 0; i < selected.length; i++) {
         selected[i] = this.slots[rows[i]].slot;
      }

      return selected;
   }

   public BSlotSheet.SlotEntry[] getSelectedEntries() {
      int[] rows = this.table.getSelection().getRows();
      BSlotSheet.SlotEntry[] selected = new BSlotSheet.SlotEntry[rows.length];

      for (int i = 0; i < selected.length; i++) {
         selected[i] = this.slots[rows[i]];
      }

      return selected;
   }

   void updateModels() {
      if (this.component != null) {
         Slot[] s = this.component.getSlotsArray();
         BSlotSheet.SlotEntry[] slots = new BSlotSheet.SlotEntry[s.length];
         BNameMap displayNames = null;

         for (int i = 0; i < s.length; i++) {
            Slot slot = s[i];
            String name = slot.getName();
            slots[i] = new BSlotSheet.SlotEntry(this.component, i, slot);
            if (slot.isProperty() && name.equals("displayNames")) {
               Property prop = slot.asProperty();
               BObject val = this.component.get(prop);
               if (val instanceof BNameMap) {
                  displayNames = (BNameMap)val;
               }
            }
         }

         this.table.relayout();
         this.slots = slots;
         this.displayNames = displayNames;
         this.commands.updateCommands();
      }
   }

   class Controller extends TableController {
      protected void handleEnter(BKeyEvent event) {
         int select = this.getSelection().getRow();
         BSlotSheet.this.commands.displayName.invoke();
      }

      public void cellDoubleClicked(BMouseEvent event, int row, int column) {
         BSlotSheet.this.commands.displayName.invoke();
      }

      public void popup(BMouseEvent event, int row, int col) {
         BMenu menu = new BMenu();
         menu.add(null, BSlotSheet.this.commands.add);
         menu.add(null, BSlotSheet.this.commands.copy);
         menu.add(null, BSlotSheet.this.commands.delete);
         menu.add(null, BSlotSheet.this.commands.rename);
         menu.add(null, BSlotSheet.this.commands.flags);
         menu.add(null, BSlotSheet.this.commands.facets);
         menu.add(null, BSlotSheet.this.commands.reorder);
         menu.add(null, BSlotSheet.this.commands.displayName);
         menu.add(null, new BSeparator());
         menu.add(null, BSlotSheet.this.commands.bajadocForType);
         menu.open(event);
      }

      public int getTextSearchColumn() {
         return 3;
      }
   }

   class Model extends TableModel {
      public int getRowCount() {
         return BSlotSheet.this.slots.length;
      }

      public int getColumnCount() {
         return 8;
      }

      public String getColumnName(int col) {
         if (col == 0) {
            return BSlotSheet.this.lexSlot;
         } else if (col == 1) {
            return BSlotSheet.this.lexIndex;
         } else if (col == 2) {
            return BSlotSheet.this.lexName;
         } else if (col == 3) {
            return BSlotSheet.this.lexDisplayName;
         } else if (col == 4) {
            return BSlotSheet.this.lexDefinition;
         } else if (col == 5) {
            return BSlotSheet.this.lexFlags;
         } else if (col == 6) {
            return BSlotSheet.this.lexType;
         } else {
            return col == 7 ? BSlotSheet.this.lexFacets : "???";
         }
      }

      public Object getSubject(int row) {
         return BSlotSheet.this.slots[row];
      }

      public Object getValueAt(int row, int col) {
         BSlotSheet.SlotEntry se = BSlotSheet.this.slots[row];
         if (col == 0) {
            return se.summary;
         } else if (col == 1) {
            return String.valueOf(se.index);
         } else if (col == 2) {
            return se.slot.getName();
         } else if (col == 3) {
            return se.toDisplayName();
         } else if (col == 4) {
            return se.definition;
         } else if (col == 5) {
            return Flags.encodeToString(se.flags);
         } else if (col == 6) {
            return se.type;
         } else {
            return col == 7 ? se.facetsString : "???";
         }
      }

      public BImage getRowIcon(int row) {
         return BSlotSheet.this.slots[row].icon;
      }

      public boolean isColumnSortable(int col) {
         return true;
      }

      public void sortByColumn(int col, boolean ascending) {
         Object[] keys = new Object[BSlotSheet.this.slots.length];

         for (int i = 0; i < keys.length; i++) {
            switch (col) {
               case 1:
                  keys[i] = BSlotSheet.this.slots[i].index;
                  break;
               default:
                  keys[i] = this.getValueAt(i, col).toString();
            }
         }

         SortUtil.sort(keys, BSlotSheet.this.slots, ascending);
      }
   }

   class Renderer extends TableCellRenderer {
      public BFont getFont(Cell cell) {
         if (cell.column == 3) {
            BSlotSheet.SlotEntry se = BSlotSheet.this.slots[cell.row];
            if (se.isDisplayNameOverridden()) {
               return BSlotSheet.overrideFont;
            }
         }

         return super.getFont(cell);
      }
   }

   class Selection extends TableSelection {
      public void updateTable() {
         super.updateTable();
         BSlotSheet.this.commands.updateCommands();
      }
   }

   class SlotEntry {
      String summary;
      int index;
      BImage icon;
      Slot slot;
      String definition;
      int flags;
      String type;
      String facetsString;

      SlotEntry(BIPropertyContainer component, int index, Slot slot) {
         this.slot = slot;
         this.index = index;
         this.flags = component.getFlags(slot);
         this.facetsString = component.getSlotFacets(slot).toString();
         this.definition = slot.isFrozen() ? BSlotSheet.this.lexFrozen : BSlotSheet.this.lexDynamic;
         if (slot.isAction()) {
            this.summary = BSlotSheet.this.lexAction;
            this.icon = BSlotSheet.actionIcon;
            this.type = this.typestr(slot.asAction().getReturnType()) + " (" + this.typestr(slot.asAction().getParameterType()) + ")";
         } else if (slot.isTopic()) {
            this.summary = BSlotSheet.this.lexTopic;
            this.icon = BSlotSheet.topicIcon;
            this.type = this.typestr(slot.asTopic().getEventType());
         } else {
            this.summary = BSlotSheet.this.lexProperty;
            this.icon = BSlotSheet.propertyIcon;
            if (slot.isFrozen()) {
               this.type = this.typestr(slot.asProperty().getType());
            } else {
               this.type = component.get(slot.asProperty()).getType().toString();
            }
         }
      }

      boolean isDisplayNameOverridden() {
         return BSlotSheet.this.displayNames != null && BSlotSheet.this.displayNames.get(this.slot.getName()) != null;
      }

      String toDisplayName() {
         if (BSlotSheet.this.displayNames != null) {
            BFormat format = BSlotSheet.this.displayNames.get(this.slot.getName());
            if (format != null) {
               return format.getFormat();
            }
         }

         return BSlotSheet.this.component.getDisplayName(this.slot, null);
      }

      String typestr(Type type) {
         return type == null ? "void" : type.toString();
      }
   }
}
