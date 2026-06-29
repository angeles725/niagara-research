package javax.baja.bacnet.datatypes;

import com.tridium.bacnet.ObjectTypeList;
import com.tridium.bacnet.asn.AsnUtil;
import javax.baja.bacnet.enums.BBacnetObjectType;
import javax.baja.bacnet.enums.BBacnetPropertyIdentifier;
import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.io.AsnInput;
import javax.baja.bacnet.io.AsnOutput;
import javax.baja.bacnet.util.PropertyInfo;
import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BComponent;
import javax.baja.sys.BDynamicEnum;
import javax.baja.sys.BFacets;
import javax.baja.sys.BValue;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.units.BUnit;
import javax.baja.util.BTypeSpec;

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
   ), @NiagaraProperty(
      name = "propertyId",
      type = "BDynamicEnum",
      defaultValue = "BDynamicEnum.make(BBacnetPropertyIdentifier.presentValue)"
   ), @NiagaraProperty(
      name = "propertyArrayIndex",
      type = "int",
      defaultValue = "NOT_USED"
   ), @NiagaraProperty(
      name = "propertyValue",
      type = "BValue",
      defaultValue = "BBacnetNull.DEFAULT"
   ), @NiagaraProperty(
      name = "priority",
      type = "int",
      defaultValue = "0"
   ), @NiagaraProperty(
      name = "postDelay",
      type = "int",
      defaultValue = "0",
      facets = {@Facet(
         name = "BFacets.UNITS",
         value = "BUnit.getUnit(\"second\")"
      )}
   ), @NiagaraProperty(
      name = "quitOnFailure",
      type = "boolean",
      defaultValue = "false"
   ), @NiagaraProperty(
      name = "writeSuccessful",
      type = "boolean",
      defaultValue = "false"
   )})
public final class BBacnetActionCommand extends BComponent implements BIBacnetDataType {
   public static final Property deviceId = newProperty(0, BBacnetObjectIdentifier.DEFAULT_DEVICE, BBacnetObjectType.getObjectIdFacets(8));
   public static final Property objectId = newProperty(0, BBacnetObjectIdentifier.DEFAULT, null);
   public static final Property propertyId = newProperty(0, BDynamicEnum.make(BBacnetPropertyIdentifier.presentValue), null);
   public static final Property propertyArrayIndex = newProperty(0, -1, null);
   public static final Property propertyValue = newProperty(0, BBacnetNull.DEFAULT, null);
   public static final Property priority = newProperty(0, 0, null);
   public static final Property postDelay = newProperty(0, 0, BFacets.make("units", BUnit.getUnit("second")));
   public static final Property quitOnFailure = newProperty(0, false, null);
   public static final Property writeSuccessful = newProperty(0, false, null);
   public static final Type TYPE = Sys.loadType(BBacnetActionCommand.class);

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

   public BDynamicEnum getPropertyId() {
      return (BDynamicEnum)this.get(propertyId);
   }

   public void setPropertyId(BDynamicEnum v) {
      this.set(propertyId, v, null);
   }

   public int getPropertyArrayIndex() {
      return this.getInt(propertyArrayIndex);
   }

   public void setPropertyArrayIndex(int v) {
      this.setInt(propertyArrayIndex, v, null);
   }

   public BValue getPropertyValue() {
      return this.get(propertyValue);
   }

   public void setPropertyValue(BValue v) {
      this.set(propertyValue, v, null);
   }

   public int getPriority() {
      return this.getInt(priority);
   }

   public void setPriority(int v) {
      this.setInt(priority, v, null);
   }

   public int getPostDelay() {
      return this.getInt(postDelay);
   }

   public void setPostDelay(int v) {
      this.setInt(postDelay, v, null);
   }

   public boolean getQuitOnFailure() {
      return this.getBoolean(quitOnFailure);
   }

   public void setQuitOnFailure(boolean v) {
      this.setBoolean(quitOnFailure, v, null);
   }

   public boolean getWriteSuccessful() {
      return this.getBoolean(writeSuccessful);
   }

   public void setWriteSuccessful(boolean v) {
      this.setBoolean(writeSuccessful, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public void started() throws Exception {
      super.started();
      this.updatePropertyValueType();
   }

   public void changed(Property property, Context context) {
      super.changed(property, context);
      if (this.isRunning()) {
         if (property.equals(objectId) || property.equals(propertyId) || property.equals(propertyArrayIndex)) {
            this.updatePropertyValueType();
         }
      }
   }

   private void updatePropertyValueType() {
      PropertyInfo propertyInfo = findPropertyInfo(this.getObjectId(), this.getPropertyId().getOrdinal());
      Type newType = this.getPropertyType(propertyInfo);
      Type newElementType = getElementType(newType, propertyInfo);
      BValue propertyValue = this.getPropertyValue();
      Type oldType = propertyValue.getType();
      if (oldType.equals(newType)) {
         if (newType == BBacnetListOf.TYPE) {
            Type oldElementType = ((BBacnetListOf)propertyValue).getListType();
            if (oldElementType.equals(newElementType)) {
               return;
            }
         } else {
            if (newType != BBacnetArray.TYPE) {
               return;
            }

            BBacnetArray oldArray = (BBacnetArray)propertyValue;
            Type oldElementType = oldArray.getArrayTypeSpec().getResolvedType();
            int newArraySize = propertyInfo.getSize();
            if (oldElementType.equals(newElementType)) {
               if (oldArray.getFixedSize()) {
                  if (oldArray.getSize() == newArraySize) {
                     return;
                  }
               } else if (newArraySize == -1) {
                  return;
               }
            }
         }
      }

      if (newType == BBacnetListOf.TYPE) {
         this.setPropertyValue(new BBacnetListOf(newElementType));
      } else if (newType == BBacnetArray.TYPE) {
         int newSize = propertyInfo.getSize();
         if (newSize != -1) {
            this.setPropertyValue(new BBacnetArray(newElementType, newSize));
         } else {
            this.setPropertyValue(new BBacnetArray(newElementType));
         }
      } else {
         this.setPropertyValue((BValue)newType.getInstance());
      }
   }

   private static PropertyInfo findPropertyInfo(BBacnetObjectIdentifier objectId, int propertyId) {
      return ObjectTypeList.getInstance().getPropertyInfo(objectId.getObjectType(), propertyId);
   }

   private Type getPropertyType(PropertyInfo propertyInfo) {
      if (propertyInfo == null) {
         return BComponent.TYPE;
      } else if (propertyInfo.isList()) {
         return BBacnetListOf.TYPE;
      } else if (propertyInfo.isArray()) {
         int index = this.getPropertyArrayIndex();
         if (index == 0) {
            return BBacnetUnsigned.TYPE;
         } else {
            return index > 0 ? Sys.getType(propertyInfo.getType()) : BBacnetArray.TYPE;
         }
      } else {
         return Sys.getType(propertyInfo.getType());
      }
   }

   private static Type getElementType(Type type, PropertyInfo propertyInfo) {
      return type != BBacnetListOf.TYPE && type != BBacnetArray.TYPE ? null : Sys.getType(propertyInfo.getType());
   }

   @Override
   public void readAsn(AsnInput in) throws AsnException {
      BBacnetObjectIdentifier deviceId = BBacnetObjectIdentifier.DEFAULT_DEVICE;
      in.peekTag();
      if (in.isValueTag(0)) {
         deviceId = in.readObjectIdentifier(0);
      }

      BBacnetObjectIdentifier objectId = in.readObjectIdentifier(1);
      int propertyId = in.readEnumerated(2);
      int propertyArrayIndex = -1;
      in.peekTag();
      if (in.isValueTag(3)) {
         propertyArrayIndex = in.readUnsignedInt(3);
      }

      PropertyInfo propertyInfo = findPropertyInfo(objectId, propertyId);
      BValue propertyValue = readPropertyValue(propertyInfo, propertyArrayIndex, in);
      int priority = 0;
      in.peekTag();
      if (in.isValueTag(5)) {
         priority = in.readUnsignedInt(5);
      }

      int postDelay = 0;
      in.peekTag();
      if (in.isValueTag(6)) {
         postDelay = in.readUnsignedInt(6);
      }

      boolean quitOnFailure = in.readBoolean(7);
      boolean writeSuccessful = in.readBoolean(8);
      this.set(BBacnetActionCommand.deviceId, deviceId, noWrite);
      this.set(BBacnetActionCommand.objectId, objectId, noWrite);
      this.set(BBacnetActionCommand.propertyId, BDynamicEnum.make(propertyId, BBacnetPropertyIdentifier.DEFAULT.getRange()), noWrite);
      this.setInt(BBacnetActionCommand.propertyArrayIndex, propertyArrayIndex, noWrite);
      this.set(BBacnetActionCommand.propertyValue, propertyValue, noWrite);
      this.setInt(BBacnetActionCommand.priority, priority, noWrite);
      this.setInt(BBacnetActionCommand.postDelay, postDelay, noWrite);
      this.setBoolean(BBacnetActionCommand.quitOnFailure, quitOnFailure, noWrite);
      this.setBoolean(BBacnetActionCommand.writeSuccessful, writeSuccessful, noWrite);
   }

   private static BValue readPropertyValue(PropertyInfo propertyInfo, int propertyArrayIndex, AsnInput in) throws AsnException {
      if (propertyArrayIndex == -1) {
         return AsnUtil.asnToValue(propertyInfo, in.readEncodedValue(4));
      } else if (!propertyInfo.isArray()) {
         throw new AsnException("Index specified for BACnetActionCommand when property is not an array");
      } else {
         int size = propertyInfo.getSize();
         if (propertyArrayIndex == 0) {
            if (size > -1) {
               throw new AsnException("BACnetActionCommand index may not be zero fixed size for a fixed size array");
            } else {
               return AsnUtil.fromAsnUnsigned(in.readEncodedValue(4));
            }
         } else if (size > -1 && propertyArrayIndex > size) {
            throw new AsnException("Index specified for BACnetActionCommand exceeds array's fixed size");
         } else {
            BTypeSpec elementType = BTypeSpec.make(propertyInfo.getType());
            return AsnUtil.fromAsn(in.readEncodedValue(4), (BValue)elementType.getInstance());
         }
      }
   }

   @Override
   public void writeAsn(AsnOutput out) {
      if (this.isDeviceIdUsed()) {
         out.writeObjectIdentifier(0, this.getDeviceId());
      }

      out.writeObjectIdentifier(1, this.getObjectId());
      out.writeEnumerated(2, this.getPropertyId());
      if (this.isPropertyArrayIndexUsed()) {
         out.writeUnsignedInteger(3, this.getPropertyArrayIndex());
      }

      out.writeEncodedValue(4, AsnUtil.toAsn(this.getPropertyValue()));
      if (this.isPriorityUsed()) {
         out.writeUnsignedInteger(5, this.getPriority());
      }

      if (this.isPostDelayUsed()) {
         out.writeUnsignedInteger(6, this.getPostDelay());
      }

      out.writeBoolean(7, this.getQuitOnFailure());
      out.writeBoolean(8, this.getWriteSuccessful());
   }

   private boolean isDeviceIdUsed() {
      return !deviceId.isEquivalentToDefaultValue(this.getDeviceId());
   }

   private boolean isPropertyArrayIndexUsed() {
      return this.getPropertyArrayIndex() != -1;
   }

   private boolean isPriorityUsed() {
      return this.getPriority() > 0;
   }

   private boolean isPostDelayUsed() {
      return this.getPostDelay() > 0;
   }

   public String toString(Context context) {
      StringBuilder sb = new StringBuilder();
      if (this.isDeviceIdUsed()) {
         sb.append(this.getDeviceId().toString(context)).append(' ');
      }

      sb.append(this.getObjectId().toString(context)).append(' ');
      sb.append(BBacnetPropertyIdentifier.tag(this.getPropertyId().getOrdinal()));
      if (this.isPropertyArrayIndexUsed()) {
         sb.append('[').append(this.getPropertyArrayIndex()).append(']');
      }

      sb.append(" := ").append(this.getPropertyValue().toString(context));
      if (this.isPriorityUsed()) {
         sb.append(" @").append(this.getPriority());
      }

      if (context != null) {
         return sb.toString();
      } else {
         if (this.isPostDelayUsed()) {
            sb.append("; postDelay=").append(this.getPostDelay()).append('s');
         }

         sb.append("; quitOnFailure=").append(this.getQuitOnFailure());
         sb.append("; writeSuccessful=").append(this.getWriteSuccessful());
         return sb.toString();
      }
   }
}
