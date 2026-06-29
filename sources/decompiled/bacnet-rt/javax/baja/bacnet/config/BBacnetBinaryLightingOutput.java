package javax.baja.bacnet.config;

import javax.baja.bacnet.BBacnetObject;
import javax.baja.bacnet.datatypes.BBacnetArray;
import javax.baja.bacnet.datatypes.BBacnetBitString;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.datatypes.BBacnetPriorityValue;
import javax.baja.bacnet.datatypes.BBacnetUnsigned;
import javax.baja.bacnet.enums.BBacnetObjectType;
import javax.baja.bacnet.enums.lighting.BBacnetBinaryLightingPv;
import javax.baja.bacnet.util.BacnetBitStringUtil;
import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BDynamicEnum;
import javax.baja.sys.BEnumRange;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "objectId",
      type = "BBacnetObjectIdentifier",
      defaultValue = "BBacnetObjectIdentifier.make(BBacnetObjectType.BINARY_LIGHTING_OUTPUT)",
      flags = 8,
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.OBJECT_IDENTIFIER, ASN_OBJECT_IDENTIFIER)")},
      override = true
   ), @NiagaraProperty(
      name = "objectType",
      type = "BEnum",
      defaultValue = "BDynamicEnum.make(BBacnetObjectType.BINARY_LIGHTING_OUTPUT, BEnumRange.make(BBacnetObjectType.TYPE))",
      flags = 1,
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.OBJECT_TYPE, ASN_ENUMERATED)")},
      override = true
   ), @NiagaraProperty(
      name = "presentValue",
      type = "BBacnetBinaryLightingPv",
      defaultValue = "BBacnetBinaryLightingPv.DEFAULT",
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.PRESENT_VALUE, ASN_ENUMERATED)")}
   ), @NiagaraProperty(
      name = "statusFlags",
      type = "BBacnetBitString",
      defaultValue = "BBacnetBitString.emptyBitString(BacnetBitStringUtil.getBitStringLength(\"BacnetStatusFlags\"))",
      flags = 1,
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.STATUS_FLAGS, ASN_BIT_STRING, BacnetBitStringUtil.BACNET_STATUS_FLAGS_MAP)")}
   ), @NiagaraProperty(
      name = "outOfService",
      type = "boolean",
      defaultValue = "false",
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.OUT_OF_SERVICE, ASN_BOOLEAN)")}
   ), @NiagaraProperty(
      name = "blinkWarnEnable",
      type = "boolean",
      defaultValue = "false",
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.BLINK_WARN_ENABLE, ASN_BOOLEAN)")}
   ), @NiagaraProperty(
      name = "egressTime",
      type = "BBacnetUnsigned",
      defaultValue = "BBacnetUnsigned.DEFAULT",
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.EGRESS_TIME, ASN_UNSIGNED)")}
   ), @NiagaraProperty(
      name = "egressActive",
      type = "boolean",
      defaultValue = "false",
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.EGRESS_ACTIVE, ASN_BOOLEAN)")}
   ), @NiagaraProperty(
      name = "priorityArray",
      type = "BBacnetArray",
      defaultValue = "new BBacnetArray(BBacnetPriorityValue.TYPE, 16)",
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.PRIORITY_ARRAY, ASN_BACNET_ARRAY)")}
   ), @NiagaraProperty(
      name = "relinquishDefault",
      type = "BBacnetBinaryLightingPv",
      defaultValue = "BBacnetBinaryLightingPv.DEFAULT",
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.RELINQUISH_DEFAULT, ASN_ENUMERATED)")}
   )})
public class BBacnetBinaryLightingOutput extends BBacnetObject {
   public static final Property objectId = newProperty(8, BBacnetObjectIdentifier.make(55), makeFacets(75, 12));
   public static final Property objectType = newProperty(1, BDynamicEnum.make(55, BEnumRange.make(BBacnetObjectType.TYPE)), makeFacets(79, 9));
   public static final Property presentValue = newProperty(0, BBacnetBinaryLightingPv.DEFAULT, makeFacets(85, 9));
   public static final Property statusFlags = newProperty(
      1,
      BBacnetBitString.emptyBitString(BacnetBitStringUtil.getBitStringLength("BacnetStatusFlags")),
      makeFacets(111, 8, BacnetBitStringUtil.BACNET_STATUS_FLAGS_MAP)
   );
   public static final Property outOfService = newProperty(0, false, makeFacets(81, 1));
   public static final Property blinkWarnEnable = newProperty(0, false, makeFacets(373, 1));
   public static final Property egressTime = newProperty(0, BBacnetUnsigned.DEFAULT, makeFacets(377, 2));
   public static final Property egressActive = newProperty(0, false, makeFacets(386, 1));
   public static final Property priorityArray = newProperty(0, new BBacnetArray(BBacnetPriorityValue.TYPE, 16), makeFacets(87, -2));
   public static final Property relinquishDefault = newProperty(0, BBacnetBinaryLightingPv.DEFAULT, makeFacets(104, 9));
   public static final Type TYPE = Sys.loadType(BBacnetBinaryLightingOutput.class);

   public BBacnetBinaryLightingPv getPresentValue() {
      return (BBacnetBinaryLightingPv)this.get(presentValue);
   }

   public void setPresentValue(BBacnetBinaryLightingPv v) {
      this.set(presentValue, v, null);
   }

   public BBacnetBitString getStatusFlags() {
      return (BBacnetBitString)this.get(statusFlags);
   }

   public void setStatusFlags(BBacnetBitString v) {
      this.set(statusFlags, v, null);
   }

   public boolean getOutOfService() {
      return this.getBoolean(outOfService);
   }

   public void setOutOfService(boolean v) {
      this.setBoolean(outOfService, v, null);
   }

   public boolean getBlinkWarnEnable() {
      return this.getBoolean(blinkWarnEnable);
   }

   public void setBlinkWarnEnable(boolean v) {
      this.setBoolean(blinkWarnEnable, v, null);
   }

   public BBacnetUnsigned getEgressTime() {
      return (BBacnetUnsigned)this.get(egressTime);
   }

   public void setEgressTime(BBacnetUnsigned v) {
      this.set(egressTime, v, null);
   }

   public boolean getEgressActive() {
      return this.getBoolean(egressActive);
   }

   public void setEgressActive(boolean v) {
      this.setBoolean(egressActive, v, null);
   }

   public BBacnetArray getPriorityArray() {
      return (BBacnetArray)this.get(priorityArray);
   }

   public void setPriorityArray(BBacnetArray v) {
      this.set(priorityArray, v, null);
   }

   public BBacnetBinaryLightingPv getRelinquishDefault() {
      return (BBacnetBinaryLightingPv)this.get(relinquishDefault);
   }

   public void setRelinquishDefault(BBacnetBinaryLightingPv v) {
      this.set(relinquishDefault, v, null);
   }

   @Override
   public Type getType() {
      return TYPE;
   }
}
