package javax.baja.bacnet.config;

import com.tridium.bacnet.BacUtil;
import javax.baja.bacnet.BBacnetObject;
import javax.baja.bacnet.datatypes.BBacnetDeviceObjectPropertyReference;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.datatypes.BBacnetUnsigned;
import javax.baja.bacnet.enums.BBacnetEventState;
import javax.baja.bacnet.enums.BBacnetNotifyType;
import javax.baja.bacnet.enums.BBacnetObjectType;
import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BDynamicEnum;
import javax.baja.sys.BEnum;
import javax.baja.sys.BEnumRange;
import javax.baja.sys.BFacets;
import javax.baja.sys.BInteger;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Slot;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "objectId",
      type = "BBacnetObjectIdentifier",
      defaultValue = "BBacnetObjectIdentifier.make(BBacnetObjectType.EVENT_LOG)",
      flags = 8,
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.OBJECT_IDENTIFIER, ASN_OBJECT_IDENTIFIER)")},
      override = true
   ), @NiagaraProperty(
      name = "objectType",
      type = "BEnum",
      defaultValue = "BDynamicEnum.make(BBacnetObjectType.EVENT_LOG, BEnumRange.make(BBacnetObjectType.TYPE))",
      flags = 1,
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.OBJECT_TYPE, ASN_ENUMERATED)")},
      override = true
   ), @NiagaraProperty(
      name = "description",
      type = "String",
      defaultValue = "",
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.DESCRIPTION, BacnetConst.ASN_CHARACTER_STRING)")}
   ), @NiagaraProperty(
      name = "eventState",
      type = "BEnum",
      defaultValue = "BDynamicEnum.make(BBacnetEventState.NORMAL, BEnumRange.make(BBacnetEventState.TYPE))",
      flags = 1,
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.EVENT_STATE, ASN_ENUMERATED)")}
   ), @NiagaraProperty(
      name = "logEnable",
      type = "boolean",
      defaultValue = "false",
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.ENABLE, ASN_BOOLEAN)")}
   ), @NiagaraProperty(
      name = "stopWhenFull",
      type = "boolean",
      defaultValue = "false",
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.STOP_WHEN_FULL, ASN_BOOLEAN)")}
   ), @NiagaraProperty(
      name = "bufferSize",
      type = "BBacnetUnsigned",
      defaultValue = "BBacnetUnsigned.make(60)",
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.BUFFER_SIZE, ASN_UNSIGNED)")}
   ), @NiagaraProperty(
      name = "recordCount",
      type = "BBacnetUnsigned",
      defaultValue = "BBacnetUnsigned.make(0)",
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.RECORD_COUNT, ASN_UNSIGNED)")}
   ), @NiagaraProperty(
      name = "totalRecordCount",
      type = "BBacnetUnsigned",
      defaultValue = "BBacnetUnsigned.make(0)",
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.TOTAL_RECORD_COUNT, ASN_UNSIGNED)")}
   ), @NiagaraProperty(
      name = "notifyType",
      type = "BBacnetNotifyType",
      defaultValue = "BBacnetNotifyType.event",
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.NOTIFY_TYPE, ASN_ENUMERATED)"), @Facet("BFacets.make(BacUtil.makeBacnetNotifyTypeFacets())")}
   ), @NiagaraProperty(
      name = "notificationClass",
      type = "BBacnetUnsigned",
      defaultValue = "BBacnetUnsigned.make(BBacnetObjectIdentifier.UNCONFIGURED_INSTANCE_NUMBER)",
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.NOTIFICATION_CLASS, ASN_UNSIGNED, new String[] { BFacets.MIN, BFacets.MAX }, new BInteger[] { BInteger.make(1), BInteger.make(BBacnetObjectIdentifier.MAX_INSTANCE_NUMBER) } )")}
   ), @NiagaraProperty(
      name = "objectPropertyReference",
      type = "BBacnetDeviceObjectPropertyReference",
      defaultValue = "new BBacnetDeviceObjectPropertyReference()",
      flags = 4,
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.LOG_DEVICE_OBJECT_PROPERTY, ASN_CONSTRUCTED_DATA)")}
   )})
public class BBacnetEventLog extends BBacnetObject {
   public static final Property objectId = newProperty(8, BBacnetObjectIdentifier.make(25), makeFacets(75, 12));
   public static final Property objectType = newProperty(1, BDynamicEnum.make(25, BEnumRange.make(BBacnetObjectType.TYPE)), makeFacets(79, 9));
   public static final Property description = newProperty(0, "", makeFacets(28, 7));
   public static final Property eventState = newProperty(1, BDynamicEnum.make(0, BEnumRange.make(BBacnetEventState.TYPE)), makeFacets(36, 9));
   public static final Property logEnable = newProperty(0, false, makeFacets(133, 1));
   public static final Property stopWhenFull = newProperty(0, false, makeFacets(144, 1));
   public static final Property bufferSize = newProperty(0, BBacnetUnsigned.make(60L), makeFacets(126, 2));
   public static final Property recordCount = newProperty(0, BBacnetUnsigned.make(0L), makeFacets(141, 2));
   public static final Property totalRecordCount = newProperty(0, BBacnetUnsigned.make(0L), makeFacets(145, 2));
   public static final Property notifyType = newProperty(
      0, BBacnetNotifyType.event, BFacets.make(makeFacets(72, 9), BFacets.make(BacUtil.makeBacnetNotifyTypeFacets()))
   );
   public static final Property notificationClass = newProperty(
      0, BBacnetUnsigned.make(4194303L), makeFacets(17, 2, new String[]{"min", "max"}, new BInteger[]{BInteger.make(1), BInteger.make(4194302)})
   );
   public static final Property objectPropertyReference = newProperty(4, new BBacnetDeviceObjectPropertyReference(), makeFacets(132, -1));
   public static final Type TYPE = Sys.loadType(BBacnetEventLog.class);

   public String getDescription() {
      return this.getString(description);
   }

   public void setDescription(String v) {
      this.setString(description, v, null);
   }

   public BEnum getEventState() {
      return (BEnum)this.get(eventState);
   }

   public void setEventState(BEnum v) {
      this.set(eventState, v, null);
   }

   public boolean getLogEnable() {
      return this.getBoolean(logEnable);
   }

   public void setLogEnable(boolean v) {
      this.setBoolean(logEnable, v, null);
   }

   public boolean getStopWhenFull() {
      return this.getBoolean(stopWhenFull);
   }

   public void setStopWhenFull(boolean v) {
      this.setBoolean(stopWhenFull, v, null);
   }

   public BBacnetUnsigned getBufferSize() {
      return (BBacnetUnsigned)this.get(bufferSize);
   }

   public void setBufferSize(BBacnetUnsigned v) {
      this.set(bufferSize, v, null);
   }

   public BBacnetUnsigned getRecordCount() {
      return (BBacnetUnsigned)this.get(recordCount);
   }

   public void setRecordCount(BBacnetUnsigned v) {
      this.set(recordCount, v, null);
   }

   public BBacnetUnsigned getTotalRecordCount() {
      return (BBacnetUnsigned)this.get(totalRecordCount);
   }

   public void setTotalRecordCount(BBacnetUnsigned v) {
      this.set(totalRecordCount, v, null);
   }

   public BBacnetNotifyType getNotifyType() {
      return (BBacnetNotifyType)this.get(notifyType);
   }

   public void setNotifyType(BBacnetNotifyType v) {
      this.set(notifyType, v, null);
   }

   public BBacnetUnsigned getNotificationClass() {
      return (BBacnetUnsigned)this.get(notificationClass);
   }

   public void setNotificationClass(BBacnetUnsigned v) {
      this.set(notificationClass, v, null);
   }

   public BBacnetDeviceObjectPropertyReference getObjectPropertyReference() {
      return (BBacnetDeviceObjectPropertyReference)this.get(objectPropertyReference);
   }

   public void setObjectPropertyReference(BBacnetDeviceObjectPropertyReference v) {
      this.set(objectPropertyReference, v, null);
   }

   @Override
   public Type getType() {
      return TYPE;
   }

   @Override
   protected boolean shouldPoll(int propertyId) {
      return propertyId != 131;
   }

   public String getDisplayName(Slot slot, Context cx) {
      if (slot.equals(logEnable)) {
         return lex.getText("bacnet.trendlog.log.enable.display.name");
      } else {
         return slot.equals(totalRecordCount) ? lex.getText("bacnet.trendlog.log.buffer.display.name") : super.getDisplayName(slot, cx);
      }
   }
}
