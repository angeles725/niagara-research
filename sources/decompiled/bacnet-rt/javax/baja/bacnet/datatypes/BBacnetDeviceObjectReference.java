package javax.baja.bacnet.datatypes;

import javax.baja.bacnet.enums.BBacnetObjectType;
import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.io.AsnInput;
import javax.baja.bacnet.io.AsnOutput;
import javax.baja.nre.annotations.Facet;
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
      name = "deviceId",
      type = "BBacnetObjectIdentifier",
      defaultValue = "BBacnetObjectIdentifier.DEFAULT_DEVICE",
      facets = {@Facet("BBacnetObjectType.getObjectIdFacets(BBacnetObjectType.DEVICE)")}
   ), @NiagaraProperty(
      name = "objectId",
      type = "BBacnetObjectIdentifier",
      defaultValue = "BBacnetObjectIdentifier.DEFAULT"
   )})
public class BBacnetDeviceObjectReference extends BStruct implements BIBacnetDataType {
   public static final Property deviceId = newProperty(0, BBacnetObjectIdentifier.DEFAULT_DEVICE, BBacnetObjectType.getObjectIdFacets(8));
   public static final Property objectId = newProperty(0, BBacnetObjectIdentifier.DEFAULT, null);
   public static final Type TYPE = Sys.loadType(BBacnetDeviceObjectReference.class);
   public static final int MAX_ENCODED_SIZE = 10;
   public static final int DEVICE_ID_TAG = 0;
   public static final int OBJECT_ID_TAG = 1;

   public BBacnetObjectIdentifier getDeviceId() {
      return (BBacnetObjectIdentifier)this.get(deviceId);
   }

   public void setDeviceId(BBacnetObjectIdentifier v) {
      this.set(deviceId, v, null);
   }

   public BBacnetObjectIdentifier getObjectId() {
      return (BBacnetObjectIdentifier)this.get(objectId);
   }

   public void setObjectId(BBacnetObjectIdentifier v) {
      this.set(objectId, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BBacnetDeviceObjectReference() {
   }

   public BBacnetDeviceObjectReference(BBacnetObjectIdentifier objectId) {
      this.setObjectId(objectId);
   }

   public BBacnetDeviceObjectReference(BBacnetObjectIdentifier deviceId, BBacnetObjectIdentifier objectId) {
      this.setDeviceId(deviceId);
      this.setObjectId(objectId);
   }

   public final boolean isDeviceIdUsed() {
      return !deviceId.isEquivalentToDefaultValue(this.get(deviceId));
   }

   @Override
   public final void writeAsn(AsnOutput out) {
      if (this.isDeviceIdUsed()) {
         out.writeObjectIdentifier(0, this.getDeviceId());
      }

      out.writeObjectIdentifier(1, this.getObjectId());
   }

   @Override
   public final void readAsn(AsnInput in) throws AsnException {
      in.peekTag();
      BBacnetObjectIdentifier deviceId = in.isValueTag(0) ? in.readObjectIdentifier(0) : BBacnetObjectIdentifier.DEFAULT_DEVICE;
      BBacnetObjectIdentifier objectId = in.readObjectIdentifier(1);
      this.set(BBacnetDeviceObjectReference.deviceId, deviceId, noWrite);
      this.set(BBacnetDeviceObjectReference.objectId, objectId, noWrite);
   }

   public String toString(Context cx) {
      StringBuilder sb = new StringBuilder();
      sb.append(this.getObjectId().toString(cx));
      if (this.isDeviceIdUsed()) {
         if (cx != null && cx.equals(nameContext)) {
            sb.append('_').append(this.getDeviceId().toString(cx));
         } else {
            sb.append(" in ").append(this.getDeviceId().toString(cx));
         }
      }

      return sb.toString();
   }

   public final String toDebugString() {
      StringBuilder sb = new StringBuilder(32);
      if (this.isDeviceIdUsed()) {
         sb.append("\n  " + this.getDeviceId().toString());
      }

      sb.append("\n  " + this.getObjectId().toString());
      return sb.toString();
   }
}
