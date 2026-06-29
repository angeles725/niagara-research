package javax.baja.bacnet.datatypes;

import javax.baja.bacnet.enums.BBacnetBinaryPv;
import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.io.AsnInput;
import javax.baja.bacnet.io.AsnOutput;
import javax.baja.bacnet.io.OutOfRangeException;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BBoolean;
import javax.baja.sys.BStruct;
import javax.baja.sys.Context;
import javax.baja.sys.InvalidEnumException;
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
      type = "BBacnetBinaryPv",
      defaultValue = "BBacnetBinaryPv.DEFAULT"
   )})
public final class BBacnetOptionalBinaryPv extends BStruct implements BIBacnetDataType {
   public static final Property isNull = newProperty(0, true, null);
   public static final Property value = newProperty(0, BBacnetBinaryPv.DEFAULT, null);
   public static final Type TYPE = Sys.loadType(BBacnetOptionalBinaryPv.class);

   public boolean getIsNull() {
      return this.getBoolean(isNull);
   }

   public void setIsNull(boolean v) {
      this.setBoolean(isNull, v, null);
   }

   public BBacnetBinaryPv getValue() {
      return (BBacnetBinaryPv)this.get(value);
   }

   public void setValue(BBacnetBinaryPv v) {
      this.set(value, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BBacnetOptionalBinaryPv() {
   }

   public BBacnetOptionalBinaryPv(BBacnetBinaryPv value) {
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
         out.writeEnumerated(this.getValue());
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
         case 9:
            int ordinal = in.readEnumerated();

            BBacnetBinaryPv binaryPv;
            try {
               binaryPv = BBacnetBinaryPv.make(ordinal);
            } catch (InvalidEnumException var6) {
               throw new OutOfRangeException("BACnetOptionalBinaryPv decoded enumerated value not valid: " + ordinal);
            }

            this.set(isNull, BBoolean.FALSE, noWrite);
            this.set(value, binaryPv, noWrite);
            break;
         default:
            throw new AsnException("Invalid tag: " + tag);
      }
   }

   public String toString(Context context) {
      return "BBacnetOptionalBinaryPv: isNull = " + this.getIsNull() + " value = " + this.getValue().toString(context);
   }
}
