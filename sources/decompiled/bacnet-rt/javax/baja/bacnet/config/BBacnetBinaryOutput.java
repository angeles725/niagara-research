package javax.baja.bacnet.config;

import javax.baja.bacnet.datatypes.BBacnetArray;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.datatypes.BBacnetPriorityValue;
import javax.baja.bacnet.enums.BBacnetBinaryPv;
import javax.baja.bacnet.enums.BBacnetObjectType;
import javax.baja.bacnet.enums.BBacnetPolarity;
import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BDynamicEnum;
import javax.baja.sys.BEnumRange;
import javax.baja.sys.BFacets;
import javax.baja.sys.Property;
import javax.baja.sys.Slot;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "objectId",
      type = "BBacnetObjectIdentifier",
      defaultValue = "BBacnetObjectIdentifier.make(BBacnetObjectType.BINARY_OUTPUT)",
      flags = 8,
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.OBJECT_IDENTIFIER, ASN_OBJECT_IDENTIFIER)")},
      override = true
   ), @NiagaraProperty(
      name = "objectType",
      type = "BEnum",
      defaultValue = "BDynamicEnum.make(BBacnetObjectType.BINARY_OUTPUT, BEnumRange.make(BBacnetObjectType.TYPE))",
      flags = 1,
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.OBJECT_TYPE, ASN_ENUMERATED)")},
      override = true
   ), @NiagaraProperty(
      name = "polarity",
      type = "BBacnetPolarity",
      defaultValue = "BBacnetPolarity.normal",
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.POLARITY, ASN_ENUMERATED)")}
   ), @NiagaraProperty(
      name = "priorityArray",
      type = "BBacnetArray",
      defaultValue = "new BBacnetArray(BBacnetPriorityValue.TYPE, 16)",
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.PRIORITY_ARRAY, ASN_BACNET_ARRAY)")}
   ), @NiagaraProperty(
      name = "relinquishDefault",
      type = "BBacnetBinaryPv",
      defaultValue = "BBacnetBinaryPv.inactive",
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.RELINQUISH_DEFAULT, ASN_ENUMERATED)")}
   )})
public class BBacnetBinaryOutput extends BBacnetBinary {
   public static final Property objectId = newProperty(8, BBacnetObjectIdentifier.make(4), makeFacets(75, 12));
   public static final Property objectType = newProperty(1, BDynamicEnum.make(4, BEnumRange.make(BBacnetObjectType.TYPE)), makeFacets(79, 9));
   public static final Property polarity = newProperty(0, BBacnetPolarity.normal, makeFacets(84, 9));
   public static final Property priorityArray = newProperty(0, new BBacnetArray(BBacnetPriorityValue.TYPE, 16), makeFacets(87, -2));
   public static final Property relinquishDefault = newProperty(0, BBacnetBinaryPv.inactive, makeFacets(104, 9));
   public static final Type TYPE = Sys.loadType(BBacnetBinaryOutput.class);

   public BBacnetPolarity getPolarity() {
      return (BBacnetPolarity)this.get(polarity);
   }

   public void setPolarity(BBacnetPolarity v) {
      this.set(polarity, v, null);
   }

   public BBacnetArray getPriorityArray() {
      return (BBacnetArray)this.get(priorityArray);
   }

   public void setPriorityArray(BBacnetArray v) {
      this.set(priorityArray, v, null);
   }

   public BBacnetBinaryPv getRelinquishDefault() {
      return (BBacnetBinaryPv)this.get(relinquishDefault);
   }

   public void setRelinquishDefault(BBacnetBinaryPv v) {
      this.set(relinquishDefault, v, null);
   }

   @Override
   public Type getType() {
      return TYPE;
   }

   @Override
   public BFacets getSlotFacets(Slot slot) {
      if (slot == priorityArray) {
         return this.getFacets();
      } else {
         return slot == relinquishDefault ? this.getFacets() : super.getSlotFacets(slot);
      }
   }
}
