package javax.baja.bacnet.config;

import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
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
      defaultValue = "BBacnetObjectIdentifier.make(BBacnetObjectType.ANALOG_VALUE)",
      flags = 8,
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.OBJECT_IDENTIFIER, ASN_OBJECT_IDENTIFIER)")},
      override = true
   ), @NiagaraProperty(
      name = "objectType",
      type = "BEnum",
      defaultValue = "BDynamicEnum.make(BBacnetObjectType.ANALOG_VALUE, BEnumRange.make(BBacnetObjectType.TYPE))",
      flags = 1,
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.OBJECT_TYPE, ASN_ENUMERATED)")},
      override = true
   )})
public class BBacnetAnalogValue extends BBacnetAnalog {
   public static final Property objectId = newProperty(8, BBacnetObjectIdentifier.make(2), makeFacets(75, 12));
   public static final Property objectType = newProperty(1, BDynamicEnum.make(2, BEnumRange.make(BBacnetObjectType.TYPE)), makeFacets(79, 9));
   public static final Type TYPE = Sys.loadType(BBacnetAnalogValue.class);

   @Override
   public Type getType() {
      return TYPE;
   }
}
