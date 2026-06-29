package javax.baja.bacnet.config;

import javax.baja.bacnet.BBacnetObject;
import javax.baja.bacnet.datatypes.BBacnetListOf;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.datatypes.BReadAccessResult;
import javax.baja.bacnet.datatypes.BReadAccessSpecification;
import javax.baja.bacnet.enums.BBacnetObjectType;
import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BDynamicEnum;
import javax.baja.sys.BEnumRange;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "objectId",
      type = "BBacnetObjectIdentifier",
      defaultValue = "BBacnetObjectIdentifier.make(BBacnetObjectType.GROUP)",
      flags = 72,
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.OBJECT_IDENTIFIER, ASN_OBJECT_IDENTIFIER)")},
      override = true
   ), @NiagaraProperty(
      name = "objectType",
      type = "BEnum",
      defaultValue = "BDynamicEnum.make(BBacnetObjectType.GROUP, BEnumRange.make(BBacnetObjectType.TYPE))",
      flags = 1,
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.OBJECT_TYPE, ASN_ENUMERATED)")},
      override = true
   ), @NiagaraProperty(
      name = "listOfGroupMembers",
      type = "BBacnetListOf",
      defaultValue = "new BBacnetListOf(BReadAccessSpecification.TYPE)",
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.LIST_OF_GROUP_MEMBERS, ASN_BACNET_LIST)")}
   ), @NiagaraProperty(
      name = "presentValue",
      type = "BBacnetListOf",
      defaultValue = "new BBacnetListOf(BReadAccessResult.TYPE)",
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.PRESENT_VALUE, ASN_BACNET_LIST)")}
   )})
public class BBacnetGroup extends BBacnetObject {
   public static final Property objectId = newProperty(72, BBacnetObjectIdentifier.make(11), makeFacets(75, 12));
   public static final Property objectType = newProperty(1, BDynamicEnum.make(11, BEnumRange.make(BBacnetObjectType.TYPE)), makeFacets(79, 9));
   public static final Property listOfGroupMembers = newProperty(0, new BBacnetListOf(BReadAccessSpecification.TYPE), makeFacets(53, -3));
   public static final Property presentValue = newProperty(0, new BBacnetListOf(BReadAccessResult.TYPE), makeFacets(85, -3));
   public static final Type TYPE = Sys.loadType(BBacnetGroup.class);

   public BBacnetListOf getListOfGroupMembers() {
      return (BBacnetListOf)this.get(listOfGroupMembers);
   }

   public void setListOfGroupMembers(BBacnetListOf v) {
      this.set(listOfGroupMembers, v, null);
   }

   public BBacnetListOf getPresentValue() {
      return (BBacnetListOf)this.get(presentValue);
   }

   public void setPresentValue(BBacnetListOf v) {
      this.set(presentValue, v, null);
   }

   @Override
   public Type getType() {
      return TYPE;
   }

   @Override
   public String toString(Context context) {
      return this.getObjectId().toString(context);
   }

   @Override
   public Property getPresentValueProperty() {
      return presentValue;
   }
}
