package javax.baja.bacnet.export.extensions;

import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.control.BPointExtension;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.status.BStatus;
import javax.baja.status.BStatusValue;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "status",
      type = "BStatus",
      defaultValue = "BStatus.ok",
      flags = 1
   ), @NiagaraProperty(
      name = "faultCause",
      type = "String",
      defaultValue = "",
      flags = 1
   ), @NiagaraProperty(
      name = "objectId",
      type = "BBacnetObjectIdentifier",
      defaultValue = "BBacnetObjectIdentifier.make(BBacnetObjectType.ANALOG_INPUT)",
      flags = 1
   ), @NiagaraProperty(
      name = "propertyId",
      type = "int",
      defaultValue = "BBacnetPropertyIdentifier.PRESENT_VALUE",
      flags = 1
   )})
public class BBacnetUnsignedPropertyExt extends BPointExtension {
   public static final Property status = newProperty(1, BStatus.ok, null);
   public static final Property faultCause = newProperty(1, "", null);
   public static final Property objectId = newProperty(1, BBacnetObjectIdentifier.make(0), null);
   public static final Property propertyId = newProperty(1, 85, null);
   public static final Type TYPE = Sys.loadType(BBacnetUnsignedPropertyExt.class);

   public BStatus getStatus() {
      return (BStatus)this.get(status);
   }

   public void setStatus(BStatus v) {
      this.set(status, v, null);
   }

   public String getFaultCause() {
      return this.getString(faultCause);
   }

   public void setFaultCause(String v) {
      this.setString(faultCause, v, null);
   }

   public BBacnetObjectIdentifier getObjectId() {
      return (BBacnetObjectIdentifier)this.get(objectId);
   }

   public void setObjectId(BBacnetObjectIdentifier v) {
      this.set(objectId, v, null);
   }

   public int getPropertyId() {
      return this.getInt(propertyId);
   }

   public void setPropertyId(int v) {
      this.setInt(propertyId, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BBacnetUnsignedPropertyExt() {
   }

   public BBacnetUnsignedPropertyExt(BBacnetObjectIdentifier oid, int propertyId) {
      this();
      this.setObjectId(oid);
      this.setPropertyId(propertyId);
   }

   public void onExecute(BStatusValue bStatusValue, Context context) {
   }
}
