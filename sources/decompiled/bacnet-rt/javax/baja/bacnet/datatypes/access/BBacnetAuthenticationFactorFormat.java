package javax.baja.bacnet.datatypes.access;

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
      name = "vendorId",
      type = "int",
      defaultValue = "0"
   ), @NiagaraProperty(
      name = "vendorFormat",
      type = "int",
      defaultValue = "0"
   )})
public final class BBacnetAuthenticationFactorFormat extends BStruct implements BIBacnetDataType {
   public static final Property formatType = newProperty(0, BBacnetAuthenticationFactorType.DEFAULT, null);
   public static final Property vendorId = newProperty(0, 0, null);
   public static final Property vendorFormat = newProperty(0, 0, null);
   public static final Type TYPE = Sys.loadType(BBacnetAuthenticationFactorFormat.class);
   public static final int FORMAT_TYPE_TAG = 0;
   public static final int VENDOR_ID_TAG = 1;
   public static final int VENDOR_FORMAT_TAG = 2;

   public BBacnetAuthenticationFactorType getFormatType() {
      return (BBacnetAuthenticationFactorType)this.get(formatType);
   }

   public void setFormatType(BBacnetAuthenticationFactorType v) {
      this.set(formatType, v, null);
   }

   public int getVendorId() {
      return this.getInt(vendorId);
   }

   public void setVendorId(int v) {
      this.setInt(vendorId, v, null);
   }

   public int getVendorFormat() {
      return this.getInt(vendorFormat);
   }

   public void setVendorFormat(int v) {
      this.setInt(vendorFormat, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BBacnetAuthenticationFactorFormat() {
   }

   public BBacnetAuthenticationFactorFormat(BBacnetAuthenticationFactorType formatType, int vendorId, int vendorFormat) {
      this.setFormatType(formatType);
      this.setVendorId(vendorId);
      this.setVendorFormat(vendorFormat);
   }

   public String toString(Context context) {
      StringBuilder sb = new StringBuilder("BacnetAuthFactorFmt:");
      sb.append(this.getFormatType()).append(":").append(this.getVendorId()).append(":").append(this.getVendorFormat());
      return sb.toString();
   }

   @Override
   public void writeAsn(AsnOutput out) {
      out.writeEnumerated(0, this.getFormatType());
      int vendorId = this.getVendorId();
      if (vendorId >= 0 && vendorId <= 65535) {
         out.writeUnsignedInteger(1, vendorId);
      }

      int vendorFormat = this.getVendorFormat();
      if (vendorFormat >= 0 && vendorFormat <= 65535) {
         out.writeUnsignedInteger(2, vendorFormat);
      }
   }

   @Override
   public void readAsn(AsnInput in) throws AsnException {
      BBacnetAuthenticationFactorType formatType = BBacnetAuthenticationFactorType.make(in.readEnumerated(0));
      in.peekTag();
      int vendorId = in.isContextTag(1) ? in.readUnsignedInt(1) : -1;
      in.peekTag();
      int vendorFormat = in.isContextTag(2) ? in.readUnsignedInt(2) : -1;
      this.set(BBacnetAuthenticationFactorFormat.formatType, formatType, noWrite);
      this.setInt(BBacnetAuthenticationFactorFormat.vendorId, vendorId, noWrite);
      this.setInt(BBacnetAuthenticationFactorFormat.vendorFormat, vendorFormat, noWrite);
   }
}
