package com.tridium.workbench.fieldeditors;

import javax.baja.nre.annotations.AgentOn;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BEnum;
import javax.baja.sys.BEnumRange;
import javax.baja.sys.BObject;
import javax.baja.sys.Context;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.ui.BListDropDown;
import javax.baja.ui.BTextDropDown;
import javax.baja.workbench.fieldeditor.BWbFieldEditor;

@NiagaraType(
   agent = {@AgentOn(
      types = {"baja:FrozenEnum"}
   )}
)
public class BFrozenEnumFE extends BWbFieldEditor {
   public static final Type TYPE = Sys.loadType(BFrozenEnumFE.class);
   private final BListDropDown combo = new BListDropDown();
   private BEnumRange range;
   private int[] ordinals;

   @Override
   public Type getType() {
      return TYPE;
   }

   public BFrozenEnumFE() {
      this.setContent(this.combo);
      this.linkTo("lk0", this.combo, BTextDropDown.valueModified, setModified);
      this.linkTo("lk1", this.combo, BTextDropDown.actionPerformed, actionPerformed);
   }

   @Override
   protected void doSetReadonly(boolean readonly) {
      this.combo.setDropDownEnabled(!readonly);
   }

   @Override
   protected void doLoadValue(BObject v, Context cx) {
      BEnum val = (BEnum)v;
      this.range = val.getRange();
      this.ordinals = this.range.getOrdinals();
      int sel = -1;
      this.combo.getList().removeAllItems();

      for (int i = 0; i < this.ordinals.length; i++) {
         int ordinal = this.ordinals[i];
         String displayTag = this.range.getDisplayTag(ordinal, null);
         if (this.isValidOrdinal(ordinal)) {
            this.combo.getList().addItem(displayTag);
         }

         if (ordinal == val.getOrdinal()) {
            sel = i;
         }
      }

      this.combo.setSelectedIndex(sel);
   }

   @Override
   protected BObject doSaveValue(BObject v, Context cx) {
      int sel = this.combo.getSelectedIndex();
      return this.range.get(this.ordinals[sel]);
   }

   protected boolean isValidOrdinal(int ordinal) {
      return this.range.isOrdinal(ordinal);
   }
}
