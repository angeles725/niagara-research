package javax.baja.bacnet.datatypes;

import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.io.AsnInput;
import javax.baja.bacnet.io.AsnOutput;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BBoolean;
import javax.baja.sys.BFloat;
import javax.baja.sys.BStruct;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "isNull",
      type = "boolean",
      defaultValue = "true"
   ), @NiagaraProperty(
      name = "value",
      type = "float",
      defaultValue = "0.0f"
   )})
public final class BBacnetOptionalReal extends BStruct implements BIBacnetDataType {
   public static final Property isNull = newProperty(0, true, null);
   public static final Property value = newProperty(0, 0.0F, null);
   public static final Type TYPE = Sys.loadType(BBacnetOptionalReal.class);

   public boolean getIsNull() {
      return this.getBoolean(isNull);
   }

   public void setIsNull(boolean v) {
      this.setBoolean(isNull, v, null);
   }

   public float getValue() {
      return this.getFloat(value);
   }

   public void setValue(float v) {
      this.setFloat(value, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BBacnetOptionalReal() {
   }

   public BBacnetOptionalReal(float value) {
      this.setIsNull(false);
      this.setValue(value);
   }

   public boolean isNull() {
      return this.getIsNull();
   }

   @Override
   public void writeAsn(AsnOutput out) {
      if (this.getIsNull()) {
         out.writeNull();
      } else {
         out.writeReal(this.getValue());
      }
   }

   @Override
   public void readAsn(AsnInput in) throws AsnException {
      int tag = in.peekTag();
      switch (tag) {
         case 0:
            in.readNull();
            this.set(isNull, BBoolean.TRUE, noWrite);
            break;
         case 4:
            float real = in.readReal();
            this.set(isNull, BBoolean.FALSE, noWrite);
            this.setFloat(value, real, noWrite);
            break;
         default:
            throw new AsnException("Invalid tag: " + tag);
      }
   }

   public String toString(Context context) {
      return "BACnetOptionalReal: isNull = " + this.getIsNull() + " value = " + BFloat.toString(this.getValue(), context);
   }
}
