package com.tridium.bacnet.datatypes;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.bacnet.datatypes.BBacnetArray;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.util.Array;
import javax.baja.sys.BAction;
import javax.baja.sys.BBoolean;
import javax.baja.sys.BComponent;
import javax.baja.sys.BDynamicEnum;
import javax.baja.sys.BEnum;
import javax.baja.sys.BEnumRange;
import javax.baja.sys.BInteger;
import javax.baja.sys.BValue;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.util.BTypeSpec;

@NiagaraType
@NiagaraProperty(
   name = "parameterTypeSpec",
   type = "BTypeSpec",
   defaultValue = "BTypeSpec.NULL",
   flags = 5
)
public class BRemoveArrayElementAction extends BAction {
   public static final Property parameterTypeSpec = newProperty(5, BTypeSpec.NULL, null);
   public static final Type TYPE = Sys.loadType(BRemoveArrayElementAction.class);
   private static final Logger logger = Logger.getLogger("bacnet.datatypes");

   public BTypeSpec getParameterTypeSpec() {
      return (BTypeSpec)this.get(parameterTypeSpec);
   }

   public void setParameterTypeSpec(BTypeSpec v) {
      this.set(parameterTypeSpec, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public Type getParameterType() {
      try {
         BTypeSpec typeSpec = this.getParameterTypeSpec();
         return typeSpec.isNull() ? null : this.getParameterTypeSpec().getResolvedType();
      } catch (Exception var2) {
         logger.log(Level.SEVERE, "Failed to invoke getParameterType", (Throwable)var2);
         return null;
      }
   }

   public BValue getParameterDefault() {
      try {
         BBacnetArray ba = (BBacnetArray)this.getParent();
         if (ba == null) {
            return null;
         } else {
            Array<String> a = new Array(String.class);
            Property[] props = ba.getDynamicPropertiesArray();

            for (int i = 0; i < props.length; i++) {
               String n = props[i].getName();
               if (n.indexOf("element") == 0) {
                  a.add(n);
               }
            }

            String[] tags = (String[])a.trim();
            if (tags.length == 0) {
               return null;
            } else {
               BEnumRange range = BEnumRange.make(tags);
               return BDynamicEnum.make(0, range);
            }
         }
      } catch (Throwable var6) {
         logger.log(Level.SEVERE, "Failed to invoke getParameterDefault", var6);
         return null;
      }
   }

   public BValue invoke(BComponent target, BValue arg) {
      if (!(target instanceof BBacnetArray)) {
         throw new IllegalArgumentException("RemoveArrayElement cannot be invoked on " + target.getType());
      } else {
         BBacnetArray arr = (BBacnetArray)target;
         int ndx = Integer.parseInt(((BEnum)arg).getTag().substring(7));
         arr.doRemoveElement(BInteger.make(ndx - 1));
         return BBoolean.FALSE;
      }
   }

   public Type getReturnType() {
      return null;
   }
}
