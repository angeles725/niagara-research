package javax.baja.bacnet.config;

import com.tridium.bacnet.BacUtil;
import javax.baja.bacnet.datatypes.BBacnetArray;
import javax.baja.bacnet.datatypes.BBacnetBitString;
import javax.baja.bacnet.datatypes.BBacnetDeviceObjectPropertyReference;
import javax.baja.bacnet.datatypes.BBacnetEventParameter;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.datatypes.BBacnetTimeStamp;
import javax.baja.bacnet.datatypes.BBacnetUnsigned;
import javax.baja.bacnet.enums.BBacnetEventState;
import javax.baja.bacnet.enums.BBacnetEventType;
import javax.baja.bacnet.enums.BBacnetNotifyType;
import javax.baja.bacnet.enums.BBacnetObjectType;
import javax.baja.bacnet.io.PropertyValue;
import javax.baja.bacnet.util.BacnetBitStringUtil;
import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.util.Array;
import javax.baja.sys.BDynamicEnum;
import javax.baja.sys.BEnum;
import javax.baja.sys.BEnumRange;
import javax.baja.sys.BFacets;
import javax.baja.sys.BInteger;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "objectId",
      type = "BBacnetObjectIdentifier",
      defaultValue = "BBacnetObjectIdentifier.make(BBacnetObjectType.EVENT_ENROLLMENT)",
      flags = 8,
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.OBJECT_IDENTIFIER, ASN_OBJECT_IDENTIFIER)")},
      override = true
   ), @NiagaraProperty(
      name = "objectType",
      type = "BEnum",
      defaultValue = "BDynamicEnum.make(BBacnetObjectType.EVENT_ENROLLMENT, BEnumRange.make(BBacnetObjectType.TYPE))",
      flags = 1,
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.OBJECT_TYPE, ASN_ENUMERATED)")},
      override = true
   ), @NiagaraProperty(
      name = "eventType",
      type = "BEnum",
      defaultValue = "BDynamicEnum.make(BBacnetEventType.CHANGE_OF_STATE, BEnumRange.make(BBacnetEventType.TYPE))",
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.EVENT_TYPE, ASN_ENUMERATED)")}
   ), @NiagaraProperty(
      name = "notifyType",
      type = "BBacnetNotifyType",
      defaultValue = "BBacnetNotifyType.event",
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.NOTIFY_TYPE, ASN_ENUMERATED)"), @Facet("BFacets.make(BacUtil.makeBacnetNotifyTypeFacets())")}
   ), @NiagaraProperty(
      name = "eventParameters",
      type = "BBacnetEventParameter",
      defaultValue = "new BBacnetEventParameter()",
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.EVENT_PARAMETERS, ASN_CONSTRUCTED_DATA)")}
   ), @NiagaraProperty(
      name = "objectPropertyReference",
      type = "BBacnetDeviceObjectPropertyReference",
      defaultValue = "new BBacnetDeviceObjectPropertyReference()",
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.OBJECT_PROPERTY_REFERENCE, ASN_CONSTRUCTED_DATA)")}
   ), @NiagaraProperty(
      name = "eventState",
      type = "BEnum",
      defaultValue = "BDynamicEnum.make(BBacnetEventState.NORMAL, BEnumRange.make(BBacnetEventState.TYPE))",
      flags = 1,
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.EVENT_STATE, ASN_ENUMERATED)")}
   ), @NiagaraProperty(
      name = "eventEnable",
      type = "BBacnetBitString",
      defaultValue = "BBacnetBitString.emptyBitString(BacnetBitStringUtil.getBitStringLength(\"BacnetEventTransitionBits\"))",
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.EVENT_ENABLE, ASN_BIT_STRING, BacnetBitStringUtil.BACNET_EVENT_TRANSITION_BITS_MAP)")}
   ), @NiagaraProperty(
      name = "ackedTransitions",
      type = "BBacnetBitString",
      defaultValue = "BBacnetBitString.emptyBitString(BacnetBitStringUtil.getBitStringLength(\"BacnetEventTransitionBits\"))",
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.ACKED_TRANSITIONS, ASN_BIT_STRING, BacnetBitStringUtil.BACNET_EVENT_TRANSITION_BITS_MAP)")}
   ), @NiagaraProperty(
      name = "notificationClass",
      type = "BBacnetUnsigned",
      defaultValue = "BBacnetUnsigned.make(BBacnetObjectIdentifier.UNCONFIGURED_INSTANCE_NUMBER)",
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.NOTIFICATION_CLASS, ASN_UNSIGNED, new String[] { BFacets.MIN, BFacets.MAX }, new BInteger[] { BInteger.make(0), BInteger.make(BBacnetObjectIdentifier.MAX_INSTANCE_NUMBER) } )")}
   ), @NiagaraProperty(
      name = "eventTimeStamps",
      type = "BBacnetArray",
      defaultValue = "new BBacnetArray(BBacnetTimeStamp.TYPE, 3)",
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.EVENT_TIME_STAMPS, ASN_BACNET_ARRAY)")}
   )})
public class BBacnetEventEnrollment extends BBacnetCreatableObject {
   public static final Property objectId = newProperty(8, BBacnetObjectIdentifier.make(9), makeFacets(75, 12));
   public static final Property objectType = newProperty(1, BDynamicEnum.make(9, BEnumRange.make(BBacnetObjectType.TYPE)), makeFacets(79, 9));
   public static final Property eventType = newProperty(0, BDynamicEnum.make(1, BEnumRange.make(BBacnetEventType.TYPE)), makeFacets(37, 9));
   public static final Property notifyType = newProperty(
      0, BBacnetNotifyType.event, BFacets.make(makeFacets(72, 9), BFacets.make(BacUtil.makeBacnetNotifyTypeFacets()))
   );
   public static final Property eventParameters = newProperty(0, new BBacnetEventParameter(), makeFacets(83, -1));
   public static final Property objectPropertyReference = newProperty(0, new BBacnetDeviceObjectPropertyReference(), makeFacets(78, -1));
   public static final Property eventState = newProperty(1, BDynamicEnum.make(0, BEnumRange.make(BBacnetEventState.TYPE)), makeFacets(36, 9));
   public static final Property eventEnable = newProperty(
      0,
      BBacnetBitString.emptyBitString(BacnetBitStringUtil.getBitStringLength("BacnetEventTransitionBits")),
      makeFacets(35, 8, BacnetBitStringUtil.BACNET_EVENT_TRANSITION_BITS_MAP)
   );
   public static final Property ackedTransitions = newProperty(
      0,
      BBacnetBitString.emptyBitString(BacnetBitStringUtil.getBitStringLength("BacnetEventTransitionBits")),
      makeFacets(0, 8, BacnetBitStringUtil.BACNET_EVENT_TRANSITION_BITS_MAP)
   );
   public static final Property notificationClass = newProperty(
      0, BBacnetUnsigned.make(4194303L), makeFacets(17, 2, new String[]{"min", "max"}, new BInteger[]{BInteger.make(0), BInteger.make(4194302)})
   );
   public static final Property eventTimeStamps = newProperty(0, new BBacnetArray(BBacnetTimeStamp.TYPE, 3), makeFacets(130, -2));
   public static final Type TYPE = Sys.loadType(BBacnetEventEnrollment.class);

   public BEnum getEventType() {
      return (BEnum)this.get(eventType);
   }

   public void setEventType(BEnum v) {
      this.set(eventType, v, null);
   }

   public BBacnetNotifyType getNotifyType() {
      return (BBacnetNotifyType)this.get(notifyType);
   }

   public void setNotifyType(BBacnetNotifyType v) {
      this.set(notifyType, v, null);
   }

   public BBacnetEventParameter getEventParameters() {
      return (BBacnetEventParameter)this.get(eventParameters);
   }

   public void setEventParameters(BBacnetEventParameter v) {
      this.set(eventParameters, v, null);
   }

   public BBacnetDeviceObjectPropertyReference getObjectPropertyReference() {
      return (BBacnetDeviceObjectPropertyReference)this.get(objectPropertyReference);
   }

   public void setObjectPropertyReference(BBacnetDeviceObjectPropertyReference v) {
      this.set(objectPropertyReference, v, null);
   }

   public BEnum getEventState() {
      return (BEnum)this.get(eventState);
   }

   public void setEventState(BEnum v) {
      this.set(eventState, v, null);
   }

   public BBacnetBitString getEventEnable() {
      return (BBacnetBitString)this.get(eventEnable);
   }

   public void setEventEnable(BBacnetBitString v) {
      this.set(eventEnable, v, null);
   }

   public BBacnetBitString getAckedTransitions() {
      return (BBacnetBitString)this.get(ackedTransitions);
   }

   public void setAckedTransitions(BBacnetBitString v) {
      this.set(ackedTransitions, v, null);
   }

   public BBacnetUnsigned getNotificationClass() {
      return (BBacnetUnsigned)this.get(notificationClass);
   }

   public void setNotificationClass(BBacnetUnsigned v) {
      this.set(notificationClass, v, null);
   }

   public BBacnetArray getEventTimeStamps() {
      return (BBacnetArray)this.get(eventTimeStamps);
   }

   public void setEventTimeStamps(BBacnetArray v) {
      this.set(eventTimeStamps, v, null);
   }

   @Override
   public Type getType() {
      return TYPE;
   }

   @Override
   public String toString(Context context) {
      StringBuilder sb = new StringBuilder();
      sb.append(this.getObjectId().toString(context))
         .append(' ')
         .append(this.getEventType())
         .append(" monitoring ")
         .append(this.getObjectPropertyReference().toString(context));
      return sb.toString();
   }

   @Override
   protected void addObjectInitialValues(Array<PropertyValue> listOfInitialValues) {
      this.addNotifyType(this.getNotifyType(), listOfInitialValues);
      this.addObjectPropertyReference(this.getObjectPropertyReference(), listOfInitialValues);
      this.addEventEnable(this.getEventEnable(), listOfInitialValues);
      this.addEventParameter(this.getEventParameters(), listOfInitialValues);
      this.addNotificationClass(this.getNotificationClass(), listOfInitialValues);
   }
}
