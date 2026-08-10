package com.tridium.program.ui.batch;

import com.tridium.program.batch.BSlotFlagsBatchRoutine;
import com.tridium.ui.BOptionDialog;
import com.tridium.workbench.util.BFlagConfig;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Hashtable;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraActions;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Action;
import javax.baja.sys.BComplex;
import javax.baja.sys.BComponent;
import javax.baja.sys.Flags;
import javax.baja.sys.Property;
import javax.baja.sys.Slot;
import javax.baja.sys.SlotCursor;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.sys.Flags.Flag;
import javax.baja.ui.BCheckBox;
import javax.baja.ui.BDialog;
import javax.baja.ui.BLabel;
import javax.baja.ui.BListDropDown;
import javax.baja.ui.BNullWidget;
import javax.baja.ui.BRadioButton;
import javax.baja.ui.BWidget;
import javax.baja.ui.CommandArtifact;
import javax.baja.ui.ToggleCommand;
import javax.baja.ui.ToggleCommandGroup;
import javax.baja.ui.enums.BHalign;
import javax.baja.ui.pane.BBorderPane;
import javax.baja.ui.pane.BGridPane;
import javax.baja.util.Lexicon;

@NiagaraType
@NiagaraActions({@NiagaraAction(
      name = "propertyChanged"
   ), @NiagaraAction(
      name = "useSelectionChanged"
   )})
public class BSetFlagsDialog extends BOptionDialog {
   public static final Action propertyChanged = newAction(0, null);
   public static final Action useSelectionChanged = newAction(0, null);
   public static final Type TYPE = Sys.loadType(BSetFlagsDialog.class);
   private static Lexicon lex = Lexicon.make("program");
   private static final BSetFlagsDialog.FlagWrapper[] FLAGS;
   private static final Object USE_SELECTED_COMPONENTS;
   private BListDropDown dropDown;
   private BListDropDown flagDropDown;
   private BCheckBox useSelection = new BCheckBox(lex.getText("batchEditor.setFlagsOnSelectedObjects"));
   private BSetFlagsDialog.SetFlag setFlag;
   private Hashtable<Object, boolean[]> validFlags;

   public void propertyChanged() {
      this.invoke(propertyChanged, null, null);
   }

   public void useSelectionChanged() {
      this.invoke(useSelectionChanged, null, null);
   }

   public Type getType() {
      return TYPE;
   }

   public static BSlotFlagsBatchRoutine open(BBatchEditor editor) {
      BSetFlagsDialog dlg = new BSetFlagsDialog(editor);
      dlg.setBoundsCenteredOnOwner();
      dlg.open();
      if (dlg.getResult() == 1) {
         try {
            boolean useSelected = dlg.useSelection.isSelected();
            String slotName = useSelected ? "null" : (String)dlg.dropDown.getList().getSelectedItem();
            int f = ((BSetFlagsDialog.FlagWrapper)dlg.flagDropDown.getList().getSelectedItem()).flag.getMask();
            boolean setOrRemove = dlg.setFlag.isSelected();
            return BSlotFlagsBatchRoutine.make(useSelected, slotName, f, setOrRemove);
         } catch (Exception var6) {
            BDialog.error(editor, "Error", "Failed", var6);
         }
      }

      return null;
   }

   private BSetFlagsDialog(BBatchEditor parent) {
      super(parent, lex.getText("batchEditor.commands.slotFlags.label"), new BNullWidget(), 3, null, null);
      this.useSelection.setSelected(false);
      this.linkTo(this.useSelection, BCheckBox.actionPerformed, useSelectionChanged);
      int len = parent.table.model.kids.size();
      this.validFlags = new Hashtable<>();
      String firstSlotName = null;

      for (int i = 0; i < len; i++) {
         BComponent comp = parent.table.model.kids.get(i);
         BComplex p = comp.getParent();
         if (p instanceof BComponent) {
            Property prop = comp.getPropertyInParent();
            if (prop != null) {
               BFlagConfig flagConfig = new BFlagConfig((BComponent)p, new Slot[]{prop});
               boolean[] flagEnabled = this.validFlags.get(USE_SELECTED_COMPONENTS);
               if (flagEnabled == null) {
                  flagEnabled = new boolean[FLAGS.length];

                  for (int j = 0; j < FLAGS.length; j++) {
                     flagEnabled[j] = flagConfig.isEnabled(FLAGS[j].flag);
                  }
               } else {
                  for (int j = 0; j < FLAGS.length; j++) {
                     flagEnabled[j] = flagEnabled[j] && flagConfig.isEnabled(FLAGS[j].flag);
                  }
               }

               this.validFlags.put(USE_SELECTED_COMPONENTS, flagEnabled);
            }
         }

         SlotCursor<Slot> c = comp.getSlots();

         while (c.next()) {
            Slot s = c.slot();
            String slotName = s.getName();
            if (firstSlotName == null) {
               firstSlotName = slotName;
            }

            BFlagConfig flagConfig = new BFlagConfig(comp, new Slot[]{s});
            boolean[] flagEnabled = this.validFlags.get(slotName);
            if (flagEnabled == null) {
               flagEnabled = new boolean[FLAGS.length];

               for (int j = 0; j < FLAGS.length; j++) {
                  flagEnabled[j] = flagConfig.isEnabled(FLAGS[j].flag);
               }
            } else {
               for (int j = 0; j < FLAGS.length; j++) {
                  flagEnabled[j] = flagEnabled[j] && flagConfig.isEnabled(FLAGS[j].flag);
               }
            }

            this.validFlags.put(slotName, flagEnabled);
         }
      }

      this.dropDown = new BListDropDown();
      ArrayList<String> list = new ArrayList<>();
      Enumeration<Object> e = this.validFlags.keys();

      while (e.hasMoreElements()) {
         Object key = e.nextElement();
         if (key != USE_SELECTED_COMPONENTS) {
            list.add((String)key);
         }
      }

      String[] cols = list.toArray(new String[0]);
      Arrays.sort((Object[])cols);

      for (String col : cols) {
         this.dropDown.getList().addItem(col);
      }

      if (cols.length > 0) {
         this.dropDown.getList().setSelectedIndex(0);
      }

      this.linkTo(this.dropDown, BListDropDown.listActionPerformed, propertyChanged);
      this.flagDropDown = new BListDropDown();
      if (firstSlotName != null) {
         boolean[] flagEnabled = this.validFlags.get(firstSlotName);
         boolean anyEnabled = false;

         for (int i = 0; i < FLAGS.length; i++) {
            if (flagEnabled[i]) {
               this.flagDropDown.getList().addItem(FLAGS[i]);
               anyEnabled = true;
            }
         }

         if (anyEnabled) {
            this.flagDropDown.getList().setSelectedIndex(0);
         }
      }

      BGridPane grid1 = new BGridPane(1);
      grid1.setHalign(BHalign.left);
      grid1.add(null, this.useSelection);
      BGridPane grid2 = new BGridPane(2);
      grid2.setHalign(BHalign.left);
      grid2.add(null, new BLabel(lex.getText("batchEditor.slot")));
      grid2.add(null, this.dropDown);
      grid2.add(null, new BLabel(lex.getText("batchEditor.flag")));
      grid2.add(null, this.flagDropDown);
      ToggleCommandGroup<ToggleCommand> group = new ToggleCommandGroup();
      group.add(this.setFlag = new BSetFlagsDialog.SetFlag(this));
      BSetFlagsDialog.RemoveFlag removeFlag;
      group.add(removeFlag = new BSetFlagsDialog.RemoveFlag(this));
      this.setFlag.setSelected(true);
      BGridPane grid3 = new BGridPane(2);
      grid3.setHalign(BHalign.left);
      grid3.add(null, new BRadioButton(this.setFlag));
      grid3.add(null, new BRadioButton(removeFlag));
      BGridPane grid = new BGridPane(1);
      grid.setHalign(BHalign.left);
      grid.add(null, grid1);
      grid.add(null, grid2);
      grid.add(null, grid3);
      this.setContent(new BBorderPane(grid, 10.0, 10.0, 10.0, 10.0));
   }

   public void doPropertyChanged() {
      try {
         String slotName = (String)this.dropDown.getList().getSelectedItem();
         if (slotName == null) {
            this.flagDropDown.getList().removeAllItems();
            this.flagDropDown.getList().setSelectedIndex(0);
            return;
         }

         boolean[] flagEnabled = this.validFlags.get(slotName);
         Object oldSelection = this.flagDropDown.getSelectedItem();
         Object newSelection = null;
         this.flagDropDown.getList().removeAllItems();

         for (int i = 0; i < FLAGS.length; i++) {
            if (flagEnabled[i]) {
               this.flagDropDown.getList().addItem(FLAGS[i]);
               if (FLAGS[i] == oldSelection) {
                  newSelection = FLAGS[i];
               }
            }
         }

         if (newSelection != null) {
            this.flagDropDown.getList().setSelectedItem(newSelection);
         } else {
            this.flagDropDown.getList().setSelectedIndex(0);
         }
      } catch (Exception var6) {
         var6.printStackTrace();
      }
   }

   public void doUseSelectionChanged() {
      boolean enabled = !this.useSelection.isSelected();
      this.dropDown.setEnabled(enabled);
      if (!enabled) {
         boolean[] flagEnabled = this.validFlags.get(USE_SELECTED_COMPONENTS);
         Object oldSelection = this.flagDropDown.getSelectedItem();
         Object newSelection = null;
         this.flagDropDown.getList().removeAllItems();

         for (int i = 0; i < FLAGS.length; i++) {
            if (flagEnabled[i]) {
               this.flagDropDown.getList().addItem(FLAGS[i]);
               if (FLAGS[i] == oldSelection) {
                  newSelection = FLAGS[i];
               }
            }
         }

         if (newSelection != null) {
            this.flagDropDown.getList().setSelectedItem(newSelection);
         } else {
            this.flagDropDown.getList().setSelectedIndex(0);
         }
      } else {
         this.doPropertyChanged();
      }
   }

   static {
      Flag[] flags = Flags.getFlags();
      FLAGS = new BSetFlagsDialog.FlagWrapper[flags.length];

      for (int i = 0; i < flags.length; i++) {
         FLAGS[i] = new BSetFlagsDialog.FlagWrapper(flags[i]);
      }

      USE_SELECTED_COMPONENTS = new Object();
   }

   static class FlagWrapper {
      Flag flag;

      public FlagWrapper(Flag flag) {
         this.flag = flag;
      }

      @Override
      public String toString() {
         return this.flag.getDisplayName(null);
      }
   }

   static class RemoveFlag extends ToggleCommand {
      RemoveFlag(BWidget owner) {
         super(owner, BSetFlagsDialog.lex.getText("batchEditor.removeFlag"));
      }

      public CommandArtifact doInvoke() {
         return null;
      }
   }

   static class SetFlag extends ToggleCommand {
      SetFlag(BWidget owner) {
         super(owner, BSetFlagsDialog.lex.getText("batchEditor.setFlag"));
      }

      public CommandArtifact doInvoke() {
         return null;
      }
   }
}
