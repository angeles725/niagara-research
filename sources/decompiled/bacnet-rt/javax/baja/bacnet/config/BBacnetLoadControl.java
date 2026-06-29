package javax.baja.bacnet.config;

import javax.baja.bacnet.BBacnetObject;
import javax.baja.bacnet.datatypes.BBacnetArray;
import javax.baja.bacnet.datatypes.BBacnetBitString;
import javax.baja.bacnet.datatypes.BBacnetDateTime;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.datatypes.BBacnetShedLevel;
import javax.baja.bacnet.datatypes.BBacnetUnsigned;
import javax.baja.bacnet.enums.BBacnetEventState;
import javax.baja.bacnet.enums.BBacnetObjectType;
import javax.baja.bacnet.enums.BBacnetShedState;
import javax.baja.bacnet.util.BacnetBitStringUtil;
import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BDynamicEnum;
import javax.baja.sys.BEnum;
import javax.baja.sys.BEnumRange;
import javax.baja.sys.BString;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "objectId",
      type = "BBacnetObjectIdentifier",
      defaultValue = "BBacnetObjectIdentifier.make(BBacnetObjectType.LOAD_CONTROL)",
      flags = 8,
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.OBJECT_IDENTIFIER, ASN_OBJECT_IDENTIFIER)")},
      override = true
   ), @NiagaraProperty(
      name = "objectType",
      type = "BEnum",
      defaultValue = "BDynamicEnum.make(BBacnetObjectType.LOAD_CONTROL, BEnumRange.make(BBacnetObjectType.TYPE))",
      flags = 1,
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.OBJECT_TYPE, ASN_ENUMERATED)")},
      override = true
   ), @NiagaraProperty(
      name = "presentValue",
      type = "BBacnetShedState",
      defaultValue = "BBacnetShedState.shedInactive",
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
      name = "requestedShedLevel",
      type = "BBacnetShedLevel",
      defaultValue = "new BBacnetShedLevel()",
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.REQUESTED_SHED_LEVEL, ASN_CHOICE)")}
   ), @NiagaraProperty(
      name = "startTime",
      type = "BBacnetDateTime",
      defaultValue = "new BBacnetDateTime()",
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.START_TIME, ASN_CONSTRUCTED_DATA)")}
   ), @NiagaraProperty(
      name = "shedDuration",
      type = "BBacnetUnsigned",
      defaultValue = "BBacnetUnsigned.DEFAULT",
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.SHED_DURATION, ASN_UNSIGNED)")}
   ), @NiagaraProperty(
      name = "dutyWindow",
      type = "BBacnetUnsigned",
      defaultValue = "BBacnetUnsigned.DEFAULT",
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.DUTY_WINDOW, ASN_UNSIGNED)")}
   ), @NiagaraProperty(
      name = "enable",
      type = "boolean",
      defaultValue = "false",
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.ENABLE, ASN_BOOLEAN)")}
   ), @NiagaraProperty(
      name = "expectedShedLevel",
      type = "BBacnetShedLevel",
      defaultValue = "new BBacnetShedLevel()",
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.EXPECTED_SHED_LEVEL, ASN_CHOICE)")}
   ), @NiagaraProperty(
      name = "acutalShedLevel",
      type = "BBacnetShedLevel",
      defaultValue = "new BBacnetShedLevel()",
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.ACTUAL_SHED_LEVEL, ASN_CHOICE)")}
   ), @NiagaraProperty(
      name = "shedLevels",
      type = "BBacnetArray",
      defaultValue = "new BBacnetArray(BBacnetUnsigned.TYPE)",
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.SHED_LEVELS, ASN_BACNET_ARRAY)")}
   ), @NiagaraProperty(
      name = "shedLevelDescriptions",
      type = "BBacnetArray",
      defaultValue = "new BBacnetArray(BString.TYPE)",
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.SHED_LEVEL_DESCRIPTIONS, ASN_BACNET_ARRAY)")}
   )})
public class BBacnetLoadControl extends BBacnetObject {
   public static final Property objectId = newProperty(8, BBacnetObjectIdentifier.make(28), makeFacets(75, 12));
   public static final Property objectType = newProperty(1, BDynamicEnum.make(28, BEnumRange.make(BBacnetObjectType.TYPE)), makeFacets(79, 9));
   public static final Property presentValue = newProperty(0, BBacnetShedState.shedInactive, makeFacets(85, 9));
   public static final Property statusFlags = newProperty(
      1,
      BBacnetBitString.emptyBitString(BacnetBitStringUtil.getBitStringLength("BacnetStatusFlags")),
      makeFacets(111, 8, BacnetBitStringUtil.BACNET_STATUS_FLAGS_MAP)
   );
   public static final Property eventState = newProperty(1, BDynamicEnum.make(0, BEnumRange.make(BBacnetEventState.TYPE)), makeFacets(36, 9));
   public static final Property requestedShedLevel = newProperty(0, new BBacnetShedLevel(), makeFacets(218, -5));
   public static final Property startTime = newProperty(0, new BBacnetDateTime(), makeFacets(142, -1));
   public static final Property shedDuration = newProperty(0, BBacnetUnsigned.DEFAULT, makeFacets(219, 2));
   public static final Property dutyWindow = newProperty(0, BBacnetUnsigned.DEFAULT, makeFacets(213, 2));
   public static final Property enable = newProperty(0, false, makeFacets(133, 1));
   public static final Property expectedShedLevel = newProperty(0, new BBacnetShedLevel(), makeFacets(214, -5));
   public static final Property acutalShedLevel = newProperty(0, new BBacnetShedLevel(), makeFacets(212, -5));
   public static final Property shedLevels = newProperty(0, new BBacnetArray(BBacnetUnsigned.TYPE), makeFacets(221, -2));
   public static final Property shedLevelDescriptions = newProperty(0, new BBacnetArray(BString.TYPE), makeFacets(220, -2));
   public static final Type TYPE = Sys.loadType(BBacnetLoadControl.class);

   public BBacnetShedState getPresentValue() {
      return (BBacnetShedState)this.get(presentValue);
   }

   public void setPresentValue(BBacnetShedState v) {
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

   public BBacnetShedLevel getRequestedShedLevel() {
      return (BBacnetShedLevel)this.get(requestedShedLevel);
   }

   public void setRequestedShedLevel(BBacnetShedLevel v) {
      this.set(requestedShedLevel, v, null);
   }

   public BBacnetDateTime getStartTime() {
      return (BBacnetDateTime)this.get(startTime);
   }

   public void setStartTime(BBacnetDateTime v) {
      this.set(startTime, v, null);
   }

   public BBacnetUnsigned getShedDuration() {
      return (BBacnetUnsigned)this.get(shedDuration);
   }

   public void setShedDuration(BBacnetUnsigned v) {
      this.set(shedDuration, v, null);
   }

   public BBacnetUnsigned getDutyWindow() {
      return (BBacnetUnsigned)this.get(dutyWindow);
   }

   public void setDutyWindow(BBacnetUnsigned v) {
      this.set(dutyWindow, v, null);
   }

   public boolean getEnable() {
      return this.getBoolean(enable);
   }

   public void setEnable(boolean v) {
      this.setBoolean(enable, v, null);
   }

   public BBacnetShedLevel getExpectedShedLevel() {
      return (BBacnetShedLevel)this.get(expectedShedLevel);
   }

   public void setExpectedShedLevel(BBacnetShedLevel v) {
      this.set(expectedShedLevel, v, null);
   }

   public BBacnetShedLevel getAcutalShedLevel() {
      return (BBacnetShedLevel)this.get(acutalShedLevel);
   }

   public void setAcutalShedLevel(BBacnetShedLevel v) {
      this.set(acutalShedLevel, v, null);
   }

   public BBacnetArray getShedLevels() {
      return (BBacnetArray)this.get(shedLevels);
   }

   public void setShedLevels(BBacnetArray v) {
      this.set(shedLevels, v, null);
   }

   public BBacnetArray getShedLevelDescriptions() {
      return (BBacnetArray)this.get(shedLevelDescriptions);
   }

   public void setShedLevelDescriptions(BBacnetArray v) {
      this.set(shedLevelDescriptions, v, null);
   }

   @Override
   public Type getType() {
      return TYPE;
   }
}
