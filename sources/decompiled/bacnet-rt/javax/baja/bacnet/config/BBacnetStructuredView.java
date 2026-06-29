package javax.baja.bacnet.config;

import javax.baja.bacnet.BBacnetObject;
import javax.baja.bacnet.datatypes.BBacnetArray;
import javax.baja.bacnet.datatypes.BBacnetDeviceObjectReference;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.enums.BBacnetNodeType;
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
      defaultValue = "BBacnetObjectIdentifier.make(BBacnetObjectType.STRUCTURED_VIEW)",
      flags = 8,
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.OBJECT_IDENTIFIER, ASN_OBJECT_IDENTIFIER)")},
      override = true
   ), @NiagaraProperty(
      name = "objectType",
      type = "BEnum",
      defaultValue = "BDynamicEnum.make(BBacnetObjectType.STRUCTURED_VIEW, BEnumRange.make(BBacnetObjectType.TYPE))",
      flags = 1,
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.OBJECT_TYPE, ASN_ENUMERATED)")},
      override = true
   ), @NiagaraProperty(
      name = "nodeType",
      type = "BBacnetNodeType",
      defaultValue = "BBacnetNodeType.DEFAULT",
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.NODE_TYPE, ASN_ENUMERATED)")}
   ), @NiagaraProperty(
      name = "subordinateList",
      type = "BBacnetArray",
      defaultValue = "new BBacnetArray(BBacnetDeviceObjectReference.TYPE)",
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.SUBORDINATE_LIST, ASN_BACNET_ARRAY)")}
   )})
public class BBacnetStructuredView extends BBacnetObject {
   public static final Property objectId = newProperty(8, BBacnetObjectIdentifier.make(29), makeFacets(75, 12));
   public static final Property objectType = newProperty(1, BDynamicEnum.make(29, BEnumRange.make(BBacnetObjectType.TYPE)), makeFacets(79, 9));
   public static final Property nodeType = newProperty(0, BBacnetNodeType.DEFAULT, makeFacets(208, 9));
   public static final Property subordinateList = newProperty(0, new BBacnetArray(BBacnetDeviceObjectReference.TYPE), makeFacets(211, -2));
   public static final Type TYPE = Sys.loadType(BBacnetStructuredView.class);

   public BBacnetNodeType getNodeType() {
      return (BBacnetNodeType)this.get(nodeType);
   }

   public void setNodeType(BBacnetNodeType v) {
      this.set(nodeType, v, null);
   }

   public BBacnetArray getSubordinateList() {
      return (BBacnetArray)this.get(subordinateList);
   }

   public void setSubordinateList(BBacnetArray v) {
      this.set(subordinateList, v, null);
   }

   @Override
   public Type getType() {
      return TYPE;
   }
}
