package javax.baja.bacnet.config;

import javax.baja.bacnet.BBacnetObject;
import javax.baja.bacnet.datatypes.BBacnetArray;
import javax.baja.bacnet.datatypes.BBacnetBitString;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.datatypes.BBacnetPriorityValue;
import javax.baja.bacnet.datatypes.BBacnetUnsigned;
import javax.baja.bacnet.enums.BBacnetEventState;
import javax.baja.bacnet.enums.BBacnetObjectType;
import javax.baja.bacnet.enums.BBacnetReliability;
import javax.baja.bacnet.enums.access.BBacnetDoorValue;
import javax.baja.bacnet.util.BacnetBitStringUtil;
import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BDynamicEnum;
import javax.baja.sys.BEnum;
import javax.baja.sys.BEnumRange;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "objectId",
      type = "BBacnetObjectIdentifier",
      defaultValue = "BBacnetObjectIdentifier.make(BBacnetObjectType.ACCESS_DOOR)",
      flags = 8,
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.OBJECT_IDENTIFIER, ASN_OBJECT_IDENTIFIER)")},
      override = true
   ), @NiagaraProperty(
      name = "objectType",
      type = "BEnum",
      defaultValue = "BDynamicEnum.make(BBacnetObjectType.ACCESS_DOOR, BEnumRange.make(BBacnetObjectType.TYPE))",
      flags = 1,
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.OBJECT_TYPE, ASN_ENUMERATED)")},
      override = true
   ), @NiagaraProperty(
      name = "presentValue",
      type = "BBacnetDoorValue",
      defaultValue = "BBacnetDoorValue.lock",
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.PRESENT_VALUE, ASN_ENUMERATED)")}
   ), @NiagaraProperty(
      name = "statusFlags",
      type = "BBacnetBitString",
      defaultValue = "BBacnetBitString.emptyBitString(BacnetBitStringUtil.getBitStringLength(\"BacnetStatusFlags\"))",
      flags = 1,
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.STATUS_FLAGS, ASN_BIT_STRING, BacnetBitStringUtil.BACNET_STATUS_FLAGS_MAP)")}
   ), @NiagaraProperty(
      name = "eventState",
      type = "BEnum",
      defaultValue = "BDynamicEnum.make(BBacnetEventState.NORMAL, BEnumRange.make(BBacnetEventState.TYPE))",
      flags = 1,
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.EVENT_STATE, ASN_ENUMERATED)")}
   ), @NiagaraProperty(
      name = "reliability",
      type = "BEnum",
      defaultValue = "BDynamicEnum.make(BBacnetReliability.NO_FAULT_DETECTED, BEnumRange.make(BBacnetReliability.TYPE))",
      flags = 1,
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.RELIABILITY, ASN_ENUMERATED)")}
   ), @NiagaraProperty(
      name = "outOfService",
      type = "boolean",
      defaultValue = "false",
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.OUT_OF_SERVICE, ASN_BOOLEAN)")}
   ), @NiagaraProperty(
      name = "priorityArray",
      type = "BBacnetArray",
      defaultValue = "new BBacnetArray(BBacnetPriorityValue.TYPE, 16)",
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.PRIORITY_ARRAY, ASN_BACNET_ARRAY)")}
   ), @NiagaraProperty(
      name = "relinquishDefault",
      type = "BBacnetDoorValue",
      defaultValue = "BBacnetDoorValue.lock",
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.RELINQUISH_DEFAULT, ASN_ENUMERATED)")}
   ), @NiagaraProperty(
      name = "doorPulseTime",
      type = "BBacnetUnsigned",
      defaultValue = "BBacnetUnsigned.DEFAULT",
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.DOOR_PULSE_TIME, ASN_UNSIGNED)")}
   ), @NiagaraProperty(
      name = "doorExtendedPulseTime",
      type = "BBacnetUnsigned",
      defaultValue = "BBacnetUnsigned.DEFAULT",
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.DOOR_EXTENDED_PULSE_TIME, ASN_UNSIGNED)")}
   ), @NiagaraProperty(
      name = "doorOpenTooLongTime",
      type = "BBacnetUnsigned",
      defaultValue = "BBacnetUnsigned.DEFAULT",
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.DOOR_OPEN_TOO_LONG_TIME, ASN_UNSIGNED)")}
   )})
public class BBacnetAccessDoor extends BBacnetObject {
   public static final Property objectId = newProperty(8, BBacnetObjectIdentifier.make(30), makeFacets(75, 12));
   public static final Property objectType = newProperty(1, BDynamicEnum.make(30, BEnumRange.make(BBacnetObjectType.TYPE)), makeFacets(79, 9));
   public static final Property presentValue = newProperty(0, BBacnetDoorValue.lock, makeFacets(85, 9));
   public static final Property statusFlags = newProperty(
      1,
      BBacnetBitString.emptyBitString(BacnetBitStringUtil.getBitStringLength("BacnetStatusFlags")),
      makeFacets(111, 8, BacnetBitStringUtil.BACNET_STATUS_FLAGS_MAP)
   );
   public static final Property eventState = newProperty(1, BDynamicEnum.make(0, BEnumRange.make(BBacnetEventState.TYPE)), makeFacets(36, 9));
   public static final Property reliability = newProperty(1, BDynamicEnum.make(0, BEnumRange.make(BBacnetReliability.TYPE)), makeFacets(103, 9));
   public static final Property outOfService = newProperty(0, false, makeFacets(81, 1));
   public static final Property priorityArray = newProperty(0, new BBacnetArray(BBacnetPriorityValue.TYPE, 16), makeFacets(87, -2));
   public static final Property relinquishDefault = newProperty(0, BBacnetDoorValue.lock, makeFacets(104, 9));
   public static final Property doorPulseTime = newProperty(0, BBacnetUnsigned.DEFAULT, makeFacets(230, 2));
   public static final Property doorExtendedPulseTime = newProperty(0, BBacnetUnsigned.DEFAULT, makeFacets(227, 2));
   public static final Property doorOpenTooLongTime = newProperty(0, BBacnetUnsigned.DEFAULT, makeFacets(229, 2));
   public static final Type TYPE = Sys.loadType(BBacnetAccessDoor.class);

   public BBacnetDoorValue getPresentValue() {
      return (BBacnetDoorValue)this.get(presentValue);
   }

   public void setPresentValue(BBacnetDoorValue v) {
      this.set(presentValue, v, null);
   }

   public BBacnetBitString getStatusFlags() {
      return (BBacnetBitString)this.get(statusFlags);
   }

   public void setStatusFlags(BBacnetBitString v) {
      this.set(statusFlags, v, null);
   }

   public BEnum getEventState() {
      return (BEnum)this.get(eventState);
   }

   public void setEventState(BEnum v) {
      this.set(eventState, v, null);
   }

   public BEnum getReliability() {
      return (BEnum)this.get(reliability);
   }

   public void setReliability(BEnum v) {
      this.set(reliability, v, null);
   }

   public boolean getOutOfService() {
      return this.getBoolean(outOfService);
   }

   public void setOutOfService(boolean v) {
      this.setBoolean(outOfService, v, null);
   }

   public BBacnetArray getPriorityArray() {
      return (BBacnetArray)this.get(priorityArray);
   }

   public void setPriorityArray(BBacnetArray v) {
      this.set(priorityArray, v, null);
   }

   public BBacnetDoorValue getRelinquishDefault() {
      return (BBacnetDoorValue)this.get(relinquishDefault);
   }

   public void setRelinquishDefault(BBacnetDoorValue v) {
      this.set(relinquishDefault, v, null);
   }

   public BBacnetUnsigned getDoorPulseTime() {
      return (BBacnetUnsigned)this.get(doorPulseTime);
   }

   public void setDoorPulseTime(BBacnetUnsigned v) {
      this.set(doorPulseTime, v, null);
   }

   public BBacnetUnsigned getDoorExtendedPulseTime() {
      return (BBacnetUnsigned)this.get(doorExtendedPulseTime);
   }

   public void setDoorExtendedPulseTime(BBacnetUnsigned v) {
      this.set(doorExtendedPulseTime, v, null);
   }

   public BBacnetUnsigned getDoorOpenTooLongTime() {
      return (BBacnetUnsigned)this.get(doorOpenTooLongTime);
   }

   public void setDoorOpenTooLongTime(BBacnetUnsigned v) {
      this.set(doorOpenTooLongTime, v, null);
   }

   @Override
   public Type getType() {
      return TYPE;
   }
}
