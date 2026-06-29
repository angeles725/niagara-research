package com.tridium.bacnet.datatypes;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.bacnet.datatypes.BBacnetArray;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BAction;
import javax.baja.sys.BBoolean;
import javax.baja.sys.BComponent;
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
public class BAddArrayElementAction extends BAction {
   public static final Property parameterTypeSpec = newProperty(5, BTypeSpec.NULL, null);
   public static final Type TYPE = Sys.loadType(BAddArrayElementAction.class);
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
         BTypeSpec typeSpec = this.getParameterTypeSpec();
         return typeSpec.isNull() ? null : (BValue)this.getParameterTypeSpec().getResolvedType().getInstance();
      } catch (Throwable var2) {
         logger.log(Level.SEVERE, "Failed to invoke getParameterDefault", var2);
         return null;
      }
   }

   public BValue invoke(BComponent target, BValue arg) {
      if (!(target instanceof BBacnetArray)) {
         throw new IllegalArgumentException("AddArrayElement cannot be invoked on " + target.getType());
      } else {
         BBacnetArray arr = (BBacnetArray)target;
         if (!arg.getType().is(arr.getArrayTypeSpec().getTypeInfo())) {
            throw new IllegalArgumentException("Invalid type " + arg.getType() + " for " + arr);
         } else {
            arr.doAddElement(arg);
            return BBoolean.TRUE;
         }
      }
   }

   public Type getReturnType() {
      return null;
   }
}
