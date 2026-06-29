package javax.baja.bacnet.datatypes;

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
      name = "deviceObjectId",
      type = "BBacnetObjectIdentifier",
      defaultValue = "BBacnetObjectIdentifier.make(BBacnetObjectType.DEVICE)"
   ), @NiagaraProperty(
      name = "deviceAddress",
      type = "BBacnetAddress",
      defaultValue = "new BBacnetAddress()"
   )})
public final class BBacnetAddressBinding extends BStruct implements BIBacnetDataType {
   public static final Property deviceObjectId = newProperty(0, BBacnetObjectIdentifier.make(8), null);
   public static final Property deviceAddress = newProperty(0, new BBacnetAddress(), null);
   public static final Type TYPE = Sys.loadType(BBacnetAddressBinding.class);
   public static final int MAX_ENCODED_SIZE = 16;

   public BBacnetObjectIdentifier getDeviceObjectId() {
      return (BBacnetObjectIdentifier)this.get(deviceObjectId);
   }

   public void setDeviceObjectId(BBacnetObjectIdentifier v) {
      this.set(deviceObjectId, v, null);
   }

   public BBacnetAddress getDeviceAddress() {
      return (BBacnetAddress)this.get(deviceAddress);
   }

   public void setDeviceAddress(BBacnetAddress v) {
      this.set(deviceAddress, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BBacnetAddressBinding() {
   }

   public BBacnetAddressBinding(BBacnetObjectIdentifier deviceObjectId, BBacnetAddress deviceAddress) {
      this.setDeviceObjectId(deviceObjectId);
      this.getDeviceAddress().copyFrom(deviceAddress);
   }

   public String toString(Context context) {
      StringBuilder sb = new StringBuilder();
      sb.append(this.getDeviceObjectId().toString(context)).append("_to_").append(this.getDeviceAddress().toString(context));
      return sb.toString();
   }

   @Override
   public void writeAsn(AsnOutput out) {
      out.writeObjectIdentifier(this.getDeviceObjectId());
      this.getDeviceAddress().writeAsn(out);
   }

   @Override
   public void readAsn(AsnInput in) throws AsnException {
      BBacnetObjectIdentifier deviceObjectId = in.readObjectIdentifier();
      this.getDeviceAddress().readAsn(in);
      this.set(BBacnetAddressBinding.deviceObjectId, deviceObjectId, noWrite);
   }
}
