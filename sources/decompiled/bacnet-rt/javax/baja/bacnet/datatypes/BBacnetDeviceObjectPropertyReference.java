package javax.baja.bacnet.datatypes;

import java.io.IOException;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.bacnet.enums.BBacnetObjectType;
import javax.baja.bacnet.enums.BBacnetPropertyIdentifier;
import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.io.AsnInput;
import javax.baja.bacnet.io.AsnOutput;
import javax.baja.bacnet.io.PropertyReference;
import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BObject;
import javax.baja.sys.BStruct;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "objectId",
      type = "BBacnetObjectIdentifier",
      defaultValue = "BBacnetObjectIdentifier.DEFAULT"
   ), @NiagaraProperty(
      name = "propertyId",
      type = "int",
      defaultValue = "BBacnetPropertyIdentifier.PRESENT_VALUE"
   ), @NiagaraProperty(
      name = "propertyArrayIndex",
      type = "int",
      defaultValue = "NOT_USED"
   ), @NiagaraProperty(
      name = "deviceId",
      type = "BBacnetObjectIdentifier",
      defaultValue = "BBacnetObjectIdentifier.DEFAULT_DEVICE",
      facets = {@Facet("BBacnetObjectType.getObjectIdFacets(BBacnetObjectType.DEVICE)")}
   )})
public class BBacnetDeviceObjectPropertyReference extends BStruct implements BIBacnetDataType, PropertyReference {
   public static final Property objectId = newProperty(0, BBacnetObjectIdentifier.DEFAULT, null);
   public static final Property propertyId = newProperty(0, 85, null);
   public static final Property propertyArrayIndex = newProperty(0, -1, null);
   public static final Property deviceId = newProperty(0, BBacnetObjectIdentifier.DEFAULT_DEVICE, BBacnetObjectType.getObjectIdFacets(8));
   public static final Type TYPE = Sys.loadType(BBacnetDeviceObjectPropertyReference.class);
   public static final int MAX_ENCODED_SIZE = 16;
   private static final String SEP = "_";
   private static final BBacnetDeviceObjectPropertyReference REF = new BBacnetDeviceObjectPropertyReference();
   public static final int OBJECT_ID_TAG = 0;
   public static final int PROPERTY_ID_TAG = 1;
   public static final int PROPERTY_ARRAY_INDEX_TAG = 2;
   public static final int DEVICE_ID_TAG = 3;
   private static final Logger logger = Logger.getLogger("bacnet.datatypes");

   public BBacnetObjectIdentifier getObjectId() {
      return (BBacnetObjectIdentifier)this.get(objectId);
   }

   public void setObjectId(BBacnetObjectIdentifier v) {
      this.set(objectId, v, null);
   }

   @Override
   public int getPropertyId() {
      return this.getInt(propertyId);
   }

   public void setPropertyId(int v) {
      this.setInt(propertyId, v, null);
   }

   @Override
   public int getPropertyArrayIndex() {
      return this.getInt(propertyArrayIndex);
   }

   public void setPropertyArrayIndex(int v) {
      this.setInt(propertyArrayIndex, v, null);
   }

   public BBacnetObjectIdentifier getDeviceId() {
      return (BBacnetObjectIdentifier)this.get(deviceId);
   }

   public void setDeviceId(BBacnetObjectIdentifier v) {
      this.set(deviceId, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BBacnetDeviceObjectPropertyReference() {
   }

   public BBacnetDeviceObjectPropertyReference(BBacnetObjectIdentifier objectId) {
      this.setObjectId(objectId);
   }

   public BBacnetDeviceObjectPropertyReference(BBacnetObjectIdentifier objectId, int propertyId) {
      this.setObjectId(objectId);
      this.setPropertyId(propertyId);
   }

   public BBacnetDeviceObjectPropertyReference(BBacnetObjectIdentifier objectId, int propertyId, int propertyArrayIndex) {
      this.setObjectId(objectId);
      this.setPropertyId(propertyId);
      this.setPropertyArrayIndex(propertyArrayIndex);
   }

   public BBacnetDeviceObjectPropertyReference(BBacnetObjectIdentifier objectId, int propertyId, int propertyArrayIndex, BBacnetObjectIdentifier deviceId) {
      this.setObjectId(objectId);
      this.setPropertyId(propertyId);
      this.setPropertyArrayIndex(propertyArrayIndex);
      this.setDeviceId(deviceId);
   }

   public final boolean isPropertyArrayIndexUsed() {
      return this.getPropertyArrayIndex() != -1;
   }

   public final boolean isDeviceIdUsed() {
      return !deviceId.isEquivalentToDefaultValue(this.get(deviceId));
   }

   @Override
   public final void writeAsn(AsnOutput out) {
      out.writeObjectIdentifier(0, this.getObjectId());
      out.writeEnumerated(1, this.getPropertyId());
      if (this.isPropertyArrayIndexUsed()) {
         out.writeUnsignedInteger(2, this.getPropertyArrayIndex());
      }

      if (this.isDeviceIdUsed()) {
         out.writeObjectIdentifier(3, this.getDeviceId());
      }
   }

   @Override
   public final void readAsn(AsnInput in) throws AsnException {
      BBacnetObjectIdentifier objectId = in.readObjectIdentifier(0);
      int propertyId = in.readEnumerated(1);
      in.peekTag();
      int propertyArrayIndex = in.isValueTag(2) ? in.readUnsignedInt(2) : -1;
      in.peekTag();
      BBacnetObjectIdentifier deviceId = in.isValueTag(3) ? in.readObjectIdentifier(3) : BBacnetObjectIdentifier.DEFAULT_DEVICE;
      this.set(BBacnetDeviceObjectPropertyReference.objectId, objectId, noWrite);
      this.setInt(BBacnetDeviceObjectPropertyReference.propertyId, propertyId, noWrite);
      this.setInt(BBacnetDeviceObjectPropertyReference.propertyArrayIndex, propertyArrayIndex, noWrite);
      this.set(BBacnetDeviceObjectPropertyReference.deviceId, deviceId, noWrite);
   }

   public String toString(Context cx) {
      StringBuilder sb = new StringBuilder();
      sb.append(this.getObjectId().toString(cx)).append('_').append(BBacnetPropertyIdentifier.tag(this.getPropertyId()));
      if (cx != null && cx.equals(nameContext)) {
         if (this.isPropertyArrayIndexUsed()) {
            sb.append('_').append(this.getPropertyArrayIndex());
         }

         if (this.isDeviceIdUsed()) {
            sb.append('_').append(this.getDeviceId().toString(cx));
         }
      } else {
         sb.append('[').append(this.getPropertyArrayIndex()).append(']');
         if (this.isDeviceIdUsed()) {
            sb.append(" in ").append(this.getDeviceId().toString(cx));
         }
      }

      return sb.toString();
   }

   public final String toDebugString() {
      StringBuilder sb = new StringBuilder(32);
      sb.append("\n  " + this.getObjectId().toString());
      sb.append("\n  " + BBacnetPropertyIdentifier.tag(this.getPropertyId()));
      if (this.isPropertyArrayIndexUsed()) {
         sb.append("[" + this.getPropertyArrayIndex() + "]");
      }

      if (this.isDeviceIdUsed()) {
         sb.append("\n  " + this.getDeviceId().toString());
      }

      return sb.toString();
   }

   public final String encodeToString() throws IOException {
      StringBuilder sb = new StringBuilder();
      sb.append(this.getObjectId().getObjectType()).append("_").append(this.getObjectId().getInstanceNumber()).append("_").append(this.getPropertyId());
      if (this.isPropertyArrayIndexUsed()) {
         sb.append("_").append(this.getPropertyArrayIndex());
      }

      if (this.isDeviceIdUsed()) {
         sb.append("_").append(this.getDeviceId().getObjectType()).append("_").append(this.getDeviceId().getInstanceNumber());
      }

      return sb.toString();
   }

   public final BObject decodeFromString(String s) throws IOException {
      try {
         StringTokenizer st = new StringTokenizer(s, "_");
         int numTokens = st.countTokens();
         if (numTokens < 3) {
            throw new IOException("Incomplete BacnetDeviceObjectPropretyReference:" + s);
         } else {
            BBacnetObjectIdentifier objId = BBacnetObjectIdentifier.make(Integer.parseInt(st.nextToken()), Integer.parseInt(st.nextToken()));
            int propId = Integer.parseInt(st.nextToken());
            int index = -1;
            BBacnetObjectIdentifier devId = BBacnetObjectIdentifier.DEFAULT_DEVICE;
            if (numTokens == 4 || numTokens == 6) {
               index = Integer.parseInt(st.nextToken());
            }

            if (numTokens > 4) {
               devId = BBacnetObjectIdentifier.make(Integer.parseInt(st.nextToken()), Integer.parseInt(st.nextToken()));
            }

            return new BBacnetDeviceObjectPropertyReference(objId, propId, index, devId);
         }
      } catch (Exception var8) {
         throw new IOException("Error decoding BBacnetObjectPropertyReference " + s);
      }
   }

   public static final BBacnetDeviceObjectPropertyReference fromString(String s) {
      try {
         return (BBacnetDeviceObjectPropertyReference)REF.decodeFromString(s);
      } catch (IOException var2) {
         logger.log(Level.SEVERE, "BBacnetDeviceObjectPropertyReference fromString failure", (Throwable)var2);
         return null;
      }
   }
}
