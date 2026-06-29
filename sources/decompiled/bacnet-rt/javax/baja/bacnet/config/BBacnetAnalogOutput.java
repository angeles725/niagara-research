package javax.baja.bacnet.config;

import javax.baja.bacnet.datatypes.BBacnetArray;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.datatypes.BBacnetPriorityValue;
import javax.baja.bacnet.enums.BBacnetObjectType;
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
      defaultValue = "BBacnetObjectIdentifier.make(BBacnetObjectType.ANALOG_OUTPUT)",
      flags = 8,
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.OBJECT_IDENTIFIER, ASN_OBJECT_IDENTIFIER)")},
      override = true
   ), @NiagaraProperty(
      name = "objectType",
      type = "BEnum",
      defaultValue = "BDynamicEnum.make(BBacnetObjectType.ANALOG_OUTPUT, BEnumRange.make(BBacnetObjectType.TYPE))",
      flags = 1,
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.OBJECT_TYPE, ASN_ENUMERATED)")},
      override = true
   ), @NiagaraProperty(
      name = "priorityArray",
      type = "BBacnetArray",
      defaultValue = "new BBacnetArray(BBacnetPriorityValue.TYPE, 16)",
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.PRIORITY_ARRAY, ASN_BACNET_ARRAY)")}
   ), @NiagaraProperty(
      name = "relinquishDefault",
      type = "float",
      defaultValue = "0",
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.RELINQUISH_DEFAULT, ASN_REAL)")}
   )})
public class BBacnetAnalogOutput extends BBacnetAnalog {
   public static final Property objectId = newProperty(8, BBacnetObjectIdentifier.make(1), makeFacets(75, 12));
   public static final Property objectType = newProperty(1, BDynamicEnum.make(1, BEnumRange.make(BBacnetObjectType.TYPE)), makeFacets(79, 9));
   public static final Property priorityArray = newProperty(0, new BBacnetArray(BBacnetPriorityValue.TYPE, 16), makeFacets(87, -2));
   public static final Property relinquishDefault = newProperty(0, 0, makeFacets(104, 4));
   public static final Type TYPE = Sys.loadType(BBacnetAnalogOutput.class);

   public BBacnetArray getPriorityArray() {
      return (BBacnetArray)this.get(priorityArray);
   }

   public void setPriorityArray(BBacnetArray v) {
      this.set(priorityArray, v, null);
   }

   public float getRelinquishDefault() {
      return this.getFloat(relinquishDefault);
   }

   public void setRelinquishDefault(float v) {
      this.setFloat(relinquishDefault, v, null);
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
