package javax.baja.bacnet.datatypes.access;

import javax.baja.bacnet.datatypes.BBacnetOctetString;
import javax.baja.bacnet.datatypes.BIBacnetDataType;
import javax.baja.bacnet.enums.access.BBacnetAuthenticationFactorType;
import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.io.AsnInput;
import javax.baja.bacnet.io.AsnOutput;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BStruct;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "formatType",
      type = "BBacnetAuthenticationFactorType",
      defaultValue = "BBacnetAuthenticationFactorType.DEFAULT"
   ), @NiagaraProperty(
      name = "formatClass",
      type = "int",
      defaultValue = "0"
   ), @NiagaraProperty(
      name = "value",
      type = "BBacnetOctetString",
      defaultValue = "BBacnetOctetString.DEFAULT"
   )})
public final class BBacnetAuthenticationFactor extends BStruct implements BIBacnetDataType {
   public static final Property formatType = newProperty(0, BBacnetAuthenticationFactorType.DEFAULT, null);
   public static final Property formatClass = newProperty(0, 0, null);
   public static final Property value = newProperty(0, BBacnetOctetString.DEFAULT, null);
   public static final Type TYPE = Sys.loadType(BBacnetAuthenticationFactor.class);
   public static final int FORMAT_TYPE_TAG = 0;
   public static final int FORMAT_CLASS_TAG = 1;
   public static final int VALUE_TAG = 2;

   public BBacnetAuthenticationFactorType getFormatType() {
      return (BBacnetAuthenticationFactorType)this.get(formatType);
   }

   public void setFormatType(BBacnetAuthenticationFactorType v) {
      this.set(formatType, v, null);
   }

   public int getFormatClass() {
      return this.getInt(formatClass);
   }

   public void setFormatClass(int v) {
      this.setInt(formatClass, v, null);
   }

   public BBacnetOctetString getValue() {
      return (BBacnetOctetString)this.get(value);
   }

   public void setValue(BBacnetOctetString v) {
      this.set(value, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BBacnetAuthenticationFactor() {
   }

   public BBacnetAuthenticationFactor(BBacnetAuthenticationFactorType formatType, int formatClass, BBacnetOctetString value) {
      this.setFormatType(formatType);
      this.setFormatClass(formatClass);
      this.setValue(value);
   }

   public String toString(Context context) {
      StringBuilder sb = new StringBuilder("BacnetAuthFactor:");
      sb.append(this.getFormatType())
         .append(":")
         .append(this.getFormatClass())
         .append(":")
         .append(this.getValue().getBytes() != null ? this.getValue().length() : "null");
      return sb.toString();
   }

   @Override
   public void writeAsn(AsnOutput out) {
      out.writeEnumerated(0, this.getFormatType());
      out.writeUnsignedInteger(1, this.getFormatClass() & 4294967295L);
      out.writeOctetString(2, this.getValue());
   }

   @Override
   public void readAsn(AsnInput in) throws AsnException {
      BBacnetAuthenticationFactorType formatType = BBacnetAuthenticationFactorType.make(in.readEnumerated(0));
      int formatClass = in.readUnsignedInt(1);
      BBacnetOctetString value = in.readBacnetOctetString(2);
      this.set(BBacnetAuthenticationFactor.formatType, formatType, noWrite);
      this.setInt(BBacnetAuthenticationFactor.formatClass, formatClass, noWrite);
      this.set(BBacnetAuthenticationFactor.value, value, noWrite);
   }
}
