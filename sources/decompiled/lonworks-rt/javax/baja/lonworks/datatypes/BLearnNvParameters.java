package javax.baja.lonworks.datatypes;

import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BStruct;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperty(
   name = "useLonObjects",
   type = "boolean",
   defaultValue = "false"
)
public class BLearnNvParameters extends BStruct {
   public static final Property useLonObjects = newProperty(0, false, null);
   public static final Type TYPE = Sys.loadType(BLearnNvParameters.class);

   public boolean getUseLonObjects() {
      return this.getBoolean(useLonObjects);
   }

   public void setUseLonObjects(boolean v) {
      this.setBoolean(useLonObjects, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BLearnNvParameters() {
   }

   public BLearnNvParameters(boolean useLo) {
      this.setUseLonObjects(useLo);
   }
}
