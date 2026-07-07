package com.tridium.template.api.impl;

import com.tridium.template.TemplateConst;
import com.tridium.util.EscUtil;
import javax.baja.naming.BOrd;
import javax.baja.sys.BComponent;
import javax.baja.sys.BStation;
import javax.baja.sys.BValue;

public abstract class TemplateSourceWithBase extends TemplateSourceWithValue {
   protected abstract BComponent getBase();

   @Override
   public boolean hasComponent(String componentPath) {
      BOrd optionalOrd = BOrd.make("slot:" + componentPath);

      try {
         optionalOrd.resolve(this.getBase());
         return true;
      } catch (Exception var4) {
         return false;
      }
   }

   @Override
   public String getVendor() {
      BValue vendorValue = this.getBase().get(TemplateConst.VENDOR_TAG_NAME);
      return vendorValue != null ? vendorValue.toString(null) : null;
   }

   @Override
   public String getBaseName() {
      BComponent base = this.getBase();
      if (base instanceof BStation) {
         return ((BStation)base).getStationName();
      } else {
         String name = base.getName();
         return name == null ? EscUtil.slot.escape(base.getType().getTypeName()) : name;
      }
   }
}
