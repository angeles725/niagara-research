package javax.baja.bacnet.config;

import javax.baja.bacnet.datatypes.BBacnetAny;
import javax.baja.bacnet.datatypes.BBacnetBitString;
import javax.baja.bacnet.datatypes.BBacnetDateRange;
import javax.baja.bacnet.datatypes.BBacnetDeviceObjectPropertyReference;
import javax.baja.bacnet.datatypes.BBacnetListOf;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.datatypes.BBacnetUnsigned;
import javax.baja.bacnet.enums.BBacnetObjectType;
import javax.baja.bacnet.enums.BBacnetPropertyIdentifier;
import javax.baja.bacnet.enums.BBacnetReliability;
import javax.baja.bacnet.io.PropertyValue;
import javax.baja.bacnet.util.BacnetBitStringUtil;
import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.util.Array;
import javax.baja.sys.BBoolean;
import javax.baja.sys.BDynamicEnum;
import javax.baja.sys.BEnumRange;
import javax.baja.sys.BFacets;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Slot;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "objectId",
      type = "BBacnetObjectIdentifier",
      defaultValue = "BBacnetObjectIdentifier.make(BBacnetObjectType.SCHEDULE)",
      flags = 8,
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.OBJECT_IDENTIFIER, ASN_OBJECT_IDENTIFIER)")},
      override = true
   ), @NiagaraProperty(
      name = "objectType",
      type = "BEnum",
      defaultValue = "BDynamicEnum.make(BBacnetObjectType.SCHEDULE, BEnumRange.make(BBacnetObjectType.TYPE))",
      flags = 1,
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.OBJECT_TYPE, ASN_ENUMERATED)")},
      override = true
   ), @NiagaraProperty(
      name = "presentValue",
      type = "BBacnetAny",
      defaultValue = "new BBacnetAny()",
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.PRESENT_VALUE, ASN_ANY)")}
   ), @NiagaraProperty(
      name = "facets",
      type = "BFacets",
      defaultValue = "BFacets.DEFAULT"
   ), @NiagaraProperty(
      name = "effectivePeriod",
      type = "BBacnetDateRange",
      defaultValue = "new BBacnetDateRange()",
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.EFFECTIVE_PERIOD, ASN_CONSTRUCTED_DATA)")}
   ), @NiagaraProperty(
      name = "listOfObjectPropertyReferences",
      type = "BBacnetListOf",
      defaultValue = "new BBacnetListOf(BBacnetDeviceObjectPropertyReference.TYPE)",
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.LIST_OF_OBJECT_PROPERTY_REFERENCES, ASN_BACNET_LIST)")}
   ), @NiagaraProperty(
      name = "priorityForWriting",
      type = "BBacnetUnsigned",
      defaultValue = "BBacnetUnsigned.make(16)",
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.PRIORITY_FOR_WRITING, ASN_UNSIGNED)")}
   )})
public class BBacnetSchedule extends BBacnetCreatableObject {
   public static final Property objectId = newProperty(8, BBacnetObjectIdentifier.make(17), makeFacets(75, 12));
   public static final Property objectType = newProperty(1, BDynamicEnum.make(17, BEnumRange.make(BBacnetObjectType.TYPE)), makeFacets(79, 9));
   public static final Property presentValue = newProperty(0, new BBacnetAny(), makeFacets(85, -4));
   public static final Property facets = newProperty(0, BFacets.DEFAULT, null);
   public static final Property effectivePeriod = newProperty(0, new BBacnetDateRange(), makeFacets(32, -1));
   public static final Property listOfObjectPropertyReferences = newProperty(
      0, new BBacnetListOf(BBacnetDeviceObjectPropertyReference.TYPE), makeFacets(54, -3)
   );
   public static final Property priorityForWriting = newProperty(0, BBacnetUnsigned.make(16L), makeFacets(88, 2));
   public static final Type TYPE = Sys.loadType(BBacnetSchedule.class);

   public BBacnetAny getPresentValue() {
      return (BBacnetAny)this.get(presentValue);
   }

   public void setPresentValue(BBacnetAny v) {
      this.set(presentValue, v, null);
   }

   public BFacets getFacets() {
      return (BFacets)this.get(facets);
   }

   public void setFacets(BFacets v) {
      this.set(facets, v, null);
   }

   public BBacnetDateRange getEffectivePeriod() {
      return (BBacnetDateRange)this.get(effectivePeriod);
   }

   public void setEffectivePeriod(BBacnetDateRange v) {
      this.set(effectivePeriod, v, null);
   }

   public BBacnetListOf getListOfObjectPropertyReferences() {
      return (BBacnetListOf)this.get(listOfObjectPropertyReferences);
   }

   public void setListOfObjectPropertyReferences(BBacnetListOf v) {
      this.set(listOfObjectPropertyReferences, v, null);
   }

   public BBacnetUnsigned getPriorityForWriting() {
      return (BBacnetUnsigned)this.get(priorityForWriting);
   }

   public void setPriorityForWriting(BBacnetUnsigned v) {
      this.set(priorityForWriting, v, null);
   }

   @Override
   public Type getType() {
      return TYPE;
   }

   @Override
   public String toString(Context context) {
      StringBuilder sb = new StringBuilder();
      sb.append(this.getObjectId().toString(context)).append((char)(nameContext.equals(context) ? '_' : ':')).append(this.getPresentValue().toString(context));
      return sb.toString();
   }

   @Override
   public void started() throws Exception {
      super.started();
      if (this.device().getProtocolRevision() >= 4) {
         if (this.get(BBacnetPropertyIdentifier.scheduleDefault.getTag()) == null) {
            this.add(BBacnetPropertyIdentifier.scheduleDefault.getTag(), new BBacnetAny(), 0, makeFacets(174, -4), null);
         }

         if (this.get(BBacnetPropertyIdentifier.statusFlags.getTag()) == null) {
            this.add(
               BBacnetPropertyIdentifier.statusFlags.getTag(),
               BBacnetBitString.emptyBitString(BacnetBitStringUtil.getBitStringLength("BacnetStatusFlags")),
               0,
               makeFacets(111, 8, BacnetBitStringUtil.BACNET_STATUS_FLAGS_MAP),
               null
            );
         }

         if (this.get(BBacnetPropertyIdentifier.reliability.getTag()) == null) {
            this.add(
               BBacnetPropertyIdentifier.reliability.getTag(), BDynamicEnum.make(0, BEnumRange.make(BBacnetReliability.TYPE)), 0, makeFacets(103, 9), null
            );
         }

         if (this.get(BBacnetPropertyIdentifier.outOfService.getTag()) == null) {
            this.add(BBacnetPropertyIdentifier.outOfService.getTag(), BBoolean.FALSE, 0, makeFacets(81, 1), null);
         }

         this.buildPolledProperties();
      }
   }

   @Override
   public BFacets getSlotFacets(Slot slot) {
      return slot == presentValue ? this.getFacets() : super.getSlotFacets(slot);
   }

   @Override
   public Property getPresentValueProperty() {
      return presentValue;
   }

   @Override
   protected void addObjectInitialValues(Array<PropertyValue> listOfInitialValues) {
      this.addPriorityForWriting(this.getPriorityForWriting(), listOfInitialValues);
      Property scheduleDefault = this.getProperty("scheduleDefault");
      if (scheduleDefault != null) {
         this.addScheduleDefault(scheduleDefault, listOfInitialValues);
      }

      this.addListOfObjectPropertyReferences(listOfObjectPropertyReferences, listOfInitialValues);
   }
}
