package javax.baja.bacnet.config;

import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.enums.BBacnetObjectType;
import javax.baja.bacnet.enums.BBacnetPolarity;
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
      defaultValue = "BBacnetObjectIdentifier.make(BBacnetObjectType.BINARY_INPUT)",
      flags = 8,
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.OBJECT_IDENTIFIER, ASN_OBJECT_IDENTIFIER)")},
      override = true
   ), @NiagaraProperty(
      name = "objectType",
      type = "BEnum",
      defaultValue = "BDynamicEnum.make(BBacnetObjectType.BINARY_INPUT, BEnumRange.make(BBacnetObjectType.TYPE))",
      flags = 1,
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.OBJECT_TYPE, ASN_ENUMERATED)")},
      override = true
   ), @NiagaraProperty(
      name = "polarity",
      type = "BBacnetPolarity",
      defaultValue = "BBacnetPolarity.normal",
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.POLARITY, ASN_ENUMERATED)")}
   )})
public class BBacnetBinaryInput extends BBacnetBinary {
   public static final Property objectId = newProperty(8, BBacnetObjectIdentifier.make(3), makeFacets(75, 12));
   public static final Property objectType = newProperty(1, BDynamicEnum.make(3, BEnumRange.make(BBacnetObjectType.TYPE)), makeFacets(79, 9));
   public static final Property polarity = newProperty(0, BBacnetPolarity.normal, makeFacets(84, 9));
   public static final Type TYPE = Sys.loadType(BBacnetBinaryInput.class);

   public BBacnetPolarity getPolarity() {
      return (BBacnetPolarity)this.get(polarity);
   }

   public void setPolarity(BBacnetPolarity v) {
      this.set(polarity, v, null);
   }

   @Override
   public Type getType() {
      return TYPE;
   }
}
