package javax.baja.bacnet.config;

import javax.baja.bacnet.BBacnetObject;
import javax.baja.bacnet.datatypes.BBacnetAccumulatorRecord;
import javax.baja.bacnet.datatypes.BBacnetBitString;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.datatypes.BBacnetScale;
import javax.baja.bacnet.datatypes.BBacnetUnsigned;
import javax.baja.bacnet.enums.BBacnetEngineeringUnits;
import javax.baja.bacnet.enums.BBacnetEventState;
import javax.baja.bacnet.enums.BBacnetObjectType;
import javax.baja.bacnet.util.BacnetBitStringUtil;
import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BDynamicEnum;
import javax.baja.sys.BEnum;
import javax.baja.sys.BEnumRange;
import javax.baja.sys.BFacets;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "objectId",
      type = "BBacnetObjectIdentifier",
      defaultValue = "BBacnetObjectIdentifier.make(BBacnetObjectType.ACCUMULATOR)",
      flags = 8,
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.OBJECT_IDENTIFIER, ASN_OBJECT_IDENTIFIER)")},
      override = true
   ), @NiagaraProperty(
      name = "objectType",
      type = "BEnum",
      defaultValue = "BDynamicEnum.make(BBacnetObjectType.ACCUMULATOR, BEnumRange.make(BBacnetObjectType.TYPE))",
      flags = 1,
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.OBJECT_TYPE, ASN_ENUMERATED)")},
      override = true
   ), @NiagaraProperty(
      name = "presentValue",
      type = "BBacnetUnsigned",
      defaultValue = "BBacnetUnsigned.DEFAULT",
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.PRESENT_VALUE, ASN_UNSIGNED)")}
   ), @NiagaraProperty(
      name = "facets",
      type = "BFacets",
      defaultValue = "BFacets.DEFAULT",
      flags = 1
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
      name = "outOfService",
      type = "boolean",
      defaultValue = "false",
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.OUT_OF_SERVICE, ASN_BOOLEAN)")}
   ), @NiagaraProperty(
      name = "scale",
      type = "BBacnetScale",
      defaultValue = "new BBacnetScale()",
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.SCALE, ASN_CONSTRUCTED_DATA)")}
   ), @NiagaraProperty(
      name = "units",
      type = "BEnum",
      defaultValue = "BDynamicEnum.make(BBacnetEngineeringUnits.NO_UNITS, BEnumRange.make(BBacnetEngineeringUnits.TYPE))",
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.UNITS, ASN_ENUMERATED)")}
   ), @NiagaraProperty(
      name = "maxPresValue",
      type = "BBacnetUnsigned",
      defaultValue = "BBacnetUnsigned.DEFAULT",
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.MAX_PRES_VALUE, ASN_UNSIGNED)")}
   ), @NiagaraProperty(
      name = "loggingRecord",
      type = "BBacnetAccumulatorRecord",
      defaultValue = "new BBacnetAccumulatorRecord()",
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.LOGGING_RECORD, ASN_CONSTRUCTED_DATA)")}
   )})
public class BBacnetAccumulator extends BBacnetObject {
   public static final Property objectId = newProperty(8, BBacnetObjectIdentifier.make(23), makeFacets(75, 12));
   public static final Property objectType = newProperty(1, BDynamicEnum.make(23, BEnumRange.make(BBacnetObjectType.TYPE)), makeFacets(79, 9));
   public static final Property presentValue = newProperty(0, BBacnetUnsigned.DEFAULT, makeFacets(85, 2));
   public static final Property facets = newProperty(1, BFacets.DEFAULT, null);
   public static final Property statusFlags = newProperty(
      1,
      BBacnetBitString.emptyBitString(BacnetBitStringUtil.getBitStringLength("BacnetStatusFlags")),
      makeFacets(111, 8, BacnetBitStringUtil.BACNET_STATUS_FLAGS_MAP)
   );
   public static final Property eventState = newProperty(1, BDynamicEnum.make(0, BEnumRange.make(BBacnetEventState.TYPE)), makeFacets(36, 9));
   public static final Property outOfService = newProperty(0, false, makeFacets(81, 1));
   public static final Property scale = newProperty(0, new BBacnetScale(), makeFacets(187, -1));
   public static final Property units = newProperty(0, BDynamicEnum.make(95, BEnumRange.make(BBacnetEngineeringUnits.TYPE)), makeFacets(117, 9));
   public static final Property maxPresValue = newProperty(0, BBacnetUnsigned.DEFAULT, makeFacets(65, 2));
   public static final Property loggingRecord = newProperty(0, new BBacnetAccumulatorRecord(), makeFacets(184, -1));
   public static final Type TYPE = Sys.loadType(BBacnetAccumulator.class);

   public BBacnetUnsigned getPresentValue() {
      return (BBacnetUnsigned)this.get(presentValue);
   }

   public void setPresentValue(BBacnetUnsigned v) {
      this.set(presentValue, v, null);
   }

   public BFacets getFacets() {
      return (BFacets)this.get(facets);
   }

   public void setFacets(BFacets v) {
      this.set(facets, v, null);
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

   public boolean getOutOfService() {
      return this.getBoolean(outOfService);
   }

   public void setOutOfService(boolean v) {
      this.setBoolean(outOfService, v, null);
   }

   public BBacnetScale getScale() {
      return (BBacnetScale)this.get(scale);
   }

   public void setScale(BBacnetScale v) {
      this.set(scale, v, null);
   }

   public BEnum getUnits() {
      return (BEnum)this.get(units);
   }

   public void setUnits(BEnum v) {
      this.set(units, v, null);
   }

   public BBacnetUnsigned getMaxPresValue() {
      return (BBacnetUnsigned)this.get(maxPresValue);
   }

   public void setMaxPresValue(BBacnetUnsigned v) {
      this.set(maxPresValue, v, null);
   }

   public BBacnetAccumulatorRecord getLoggingRecord() {
      return (BBacnetAccumulatorRecord)this.get(loggingRecord);
   }

   public void setLoggingRecord(BBacnetAccumulatorRecord v) {
      this.set(loggingRecord, v, null);
   }

   @Override
   public Type getType() {
      return TYPE;
   }
}
