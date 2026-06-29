package javax.baja.bacnet.datatypes;

import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.io.AsnInput;
import javax.baja.bacnet.io.AsnOutput;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BBoolean;
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
      type = "BBacnetUnsigned",
      defaultValue = "BBacnetUnsigned.DEFAULT"
   )})
public final class BBacnetOptionalUnsigned extends BStruct implements BIBacnetDataType {
   public static final Property isNull = newProperty(0, true, null);
   public static final Property value = newProperty(0, BBacnetUnsigned.DEFAULT, null);
   public static final Type TYPE = Sys.loadType(BBacnetOptionalUnsigned.class);

   public boolean getIsNull() {
      return this.getBoolean(isNull);
   }

   public void setIsNull(boolean v) {
      this.setBoolean(isNull, v, null);
   }

   public BBacnetUnsigned getValue() {
      return (BBacnetUnsigned)this.get(value);
   }

   public void setValue(BBacnetUnsigned v) {
      this.set(value, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BBacnetOptionalUnsigned() {
   }

   public BBacnetOptionalUnsigned(BBacnetUnsigned value) {
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
         out.writeUnsigned(this.getValue());
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
         case 2:
            BBacnetUnsigned unsigned = in.readUnsigned();
            this.set(isNull, BBoolean.FALSE, noWrite);
            this.set(value, unsigned, noWrite);
            break;
         default:
            throw new AsnException("Invalid tag: " + tag);
      }
   }

   public String toString(Context context) {
      return "BBacnetOptionalUnsigned: isNull = " + this.getIsNull() + " value = " + this.getValue().toString(context);
   }
}
