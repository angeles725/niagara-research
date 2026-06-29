package javax.baja.bacnet.datatypes.access;

import javax.baja.bacnet.datatypes.BBacnetDeviceObjectReference;
import javax.baja.bacnet.datatypes.BIBacnetDataType;
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
      name = "assignedAccessRights",
      type = "BBacnetDeviceObjectReference",
      defaultValue = "new BBacnetDeviceObjectReference ()"
   ), @NiagaraProperty(
      name = "enable",
      type = "boolean",
      defaultValue = "false"
   )})
public final class BBacnetAssignedAccessRights extends BStruct implements BIBacnetDataType {
   public static final Property assignedAccessRights = newProperty(0, new BBacnetDeviceObjectReference(), null);
   public static final Property enable = newProperty(0, false, null);
   public static final Type TYPE = Sys.loadType(BBacnetAssignedAccessRights.class);
   public static final int ASSIGNED_ACCESS_RIGHTS_TAG = 0;
   public static final int ENABLE_TAG = 1;
   public static final int MAX_ENCODED_SIZE = 16;

   public BBacnetDeviceObjectReference getAssignedAccessRights() {
      return (BBacnetDeviceObjectReference)this.get(assignedAccessRights);
   }

   public void setAssignedAccessRights(BBacnetDeviceObjectReference v) {
      this.set(assignedAccessRights, v, null);
   }

   public boolean getEnable() {
      return this.getBoolean(enable);
   }

   public void setEnable(boolean v) {
      this.setBoolean(enable, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BBacnetAssignedAccessRights() {
   }

   public BBacnetAssignedAccessRights(BBacnetDeviceObjectReference deviceObjectReference, boolean enable) {
      this.setAssignedAccessRights(deviceObjectReference);
      this.setEnable(enable);
   }

   public String toString(Context context) {
      StringBuilder sb = new StringBuilder();
      sb.append(this.getAssignedAccessRights().toString(context)).append("_?_").append(this.getEnable());
      return sb.toString();
   }

   @Override
   public void writeAsn(AsnOutput out) {
      out.writeOpeningTag(0);
      this.getAssignedAccessRights().writeAsn(out);
      out.writeClosingTag(0);
      out.writeBoolean(1, this.getEnable());
   }

   @Override
   public void readAsn(AsnInput in) throws AsnException {
      BBacnetDeviceObjectReference assignedAccessRights = new BBacnetDeviceObjectReference();
      in.skipOpeningTag(0);
      assignedAccessRights.readAsn(in);
      in.skipClosingTag(0);
      boolean enable = in.readBoolean(1);
      this.set(BBacnetAssignedAccessRights.assignedAccessRights, assignedAccessRights, noWrite);
      this.setBoolean(BBacnetAssignedAccessRights.enable, enable, noWrite);
   }
}
