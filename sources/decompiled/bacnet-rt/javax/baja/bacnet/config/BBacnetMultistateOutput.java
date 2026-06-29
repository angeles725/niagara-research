package javax.baja.bacnet.config;

import javax.baja.bacnet.datatypes.BBacnetArray;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.datatypes.BBacnetPriorityValue;
import javax.baja.bacnet.datatypes.BBacnetUnsigned;
import javax.baja.bacnet.enums.BBacnetObjectType;
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
      defaultValue = "BBacnetObjectIdentifier.make(BBacnetObjectType.MULTI_STATE_OUTPUT)",
      flags = 8,
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.OBJECT_IDENTIFIER, ASN_OBJECT_IDENTIFIER)")},
      override = true
   ), @NiagaraProperty(
      name = "objectType",
      type = "BEnum",
      defaultValue = "BDynamicEnum.make(BBacnetObjectType.MULTI_STATE_OUTPUT, BEnumRange.make(BBacnetObjectType.TYPE))",
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
      type = "BBacnetUnsigned",
      defaultValue = "new BBacnetUnsigned(0)",
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.RELINQUISH_DEFAULT, ASN_UNSIGNED)")}
   )})
public class BBacnetMultistateOutput extends BBacnetMultistate {
   public static final Property objectId = newProperty(8, BBacnetObjectIdentifier.make(14), makeFacets(75, 12));
   public static final Property objectType = newProperty(1, BDynamicEnum.make(14, BEnumRange.make(BBacnetObjectType.TYPE)), makeFacets(79, 9));
   public static final Property priorityArray = newProperty(0, new BBacnetArray(BBacnetPriorityValue.TYPE, 16), makeFacets(87, -2));
   public static final Property relinquishDefault = newProperty(0, new BBacnetUnsigned(0L), makeFacets(104, 2));
   public static final Type TYPE = Sys.loadType(BBacnetMultistateOutput.class);

   public BBacnetArray getPriorityArray() {
      return (BBacnetArray)this.get(priorityArray);
   }

   public void setPriorityArray(BBacnetArray v) {
      this.set(priorityArray, v, null);
   }

   public BBacnetUnsigned getRelinquishDefault() {
      return (BBacnetUnsigned)this.get(relinquishDefault);
   }

   public void setRelinquishDefault(BBacnetUnsigned v) {
      this.set(relinquishDefault, v, null);
   }

   @Override
   public Type getType() {
      return TYPE;
   }
}
