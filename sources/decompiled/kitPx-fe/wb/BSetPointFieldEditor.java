package com.tridium.kitpx;

import javax.baja.naming.OrdTarget;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.status.BIStatusValue;
import javax.baja.sys.BBoolean;
import javax.baja.sys.BComponent;
import javax.baja.sys.BDouble;
import javax.baja.sys.BFacets;
import javax.baja.sys.BIBoolean;
import javax.baja.sys.BIEnum;
import javax.baja.sys.BINumeric;
import javax.baja.sys.BValue;
import javax.baja.sys.Context;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.ui.BBinding;

@NiagaraType
public class BSetPointFieldEditor extends BGenericFieldEditor {
   public static final Type TYPE = Sys.loadType(BSetPointFieldEditor.class);

   @Override
   public Type getType() {
      return TYPE;
   }

   BSetPointBinding getSetPointBinding() {
      BBinding[] bindings = this.getBindings();

      for (int i = 0; i < bindings.length; i++) {
         if (bindings[i] instanceof BSetPointBinding) {
            return (BSetPointBinding)bindings[i];
         }
      }

      return null;
   }

   void saveSetPoint(BSetPointBinding binding, Context cx) throws Exception {
      if (binding.isBound()) {
         if (this.isModified()) {
            BValue value = (BValue)this.saveValue();
            binding.saveSetPoint(value, cx);
         }
      }
   }

   void loadSetPoint(BSetPointBinding binding) {
      this.clearModified();
      if (!this.isModified()) {
         OrdTarget target = binding.getTarget();
         BValue value = (BValue)target.get();
         BFacets facets = null;
         if (value instanceof BIBoolean) {
            BIBoolean x = (BIBoolean)target.get();
            value = BBoolean.make(x.getBoolean());
            facets = x.getBooleanFacets();
         } else if (value instanceof BINumeric) {
            BINumeric x = (BINumeric)value;
            value = BDouble.make(x.getNumeric());
            facets = x.getNumericFacets();
         } else if (value instanceof BIEnum) {
            BIEnum x = (BIEnum)value;
            value = x.getEnum();
            facets = x.getEnumFacets();
         } else if (value instanceof BIStatusValue) {
            BIStatusValue x = (BIStatusValue)value;
            value = x.getStatusValue().getValueValue();
            facets = x.getStatusValueFacets();
         } else if (value instanceof BComponent) {
            BComponent c = value.asComponent();
            c.loadSlots();
            BValue out = c.get("out");
            if (out instanceof BIStatusValue) {
               BIStatusValue x = (BIStatusValue)out;
               value = x.getStatusValue().getValueValue();
               facets = x.getStatusValueFacets();
            }
         }

         if (facets == null || facets.isNull()) {
            facets = target.getFacets();
         }

         this.loadValue(value.newCopy(), facets);
      }
   }
}
