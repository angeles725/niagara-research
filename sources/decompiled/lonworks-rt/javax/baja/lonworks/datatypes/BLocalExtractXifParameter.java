package javax.baja.lonworks.datatypes;

import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BStruct;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperty(
   name = "fileName",
   type = "String",
   defaultValue = ""
)
public class BLocalExtractXifParameter extends BStruct {
   public static final Property fileName = newProperty(0, "", null);
   public static final Type TYPE = Sys.loadType(BLocalExtractXifParameter.class);

   public String getFileName() {
      return this.getString(fileName);
   }

   public void setFileName(String v) {
      this.setString(fileName, v, null);
   }

   public Type getType() {
      return TYPE;
   }
}
