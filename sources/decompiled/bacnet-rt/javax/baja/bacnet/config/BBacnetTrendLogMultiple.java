package javax.baja.bacnet.config;

import javax.baja.bacnet.datatypes.BBacnetArray;
import javax.baja.bacnet.datatypes.BBacnetDeviceObjectPropertyReference;
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
      defaultValue = "BBacnetObjectIdentifier.make(BBacnetObjectType.TREND_LOG_MULTIPLE)",
      flags = 8,
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.OBJECT_IDENTIFIER, ASN_OBJECT_IDENTIFIER)")},
      override = true
   ), @NiagaraProperty(
      name = "objectType",
      type = "BEnum",
      defaultValue = "BDynamicEnum.make(BBacnetObjectType.TREND_LOG_MULTIPLE, BEnumRange.make(BBacnetObjectType.TYPE))",
      flags = 1,
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.OBJECT_TYPE, ASN_ENUMERATED)")},
      override = true
   ), @NiagaraProperty(
      name = "logDeviceObjectProperty",
      type = "BBacnetArray",
      defaultValue = "new BBacnetArray(BBacnetDeviceObjectPropertyReference.TYPE)",
      flags = 1,
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.LOG_DEVICE_OBJECT_PROPERTY, ASN_BACNET_ARRAY)")}
   )})
public class BBacnetTrendLogMultiple extends BBacnetTrendLog {
   public static final Property objectId = newProperty(8, BBacnetObjectIdentifier.make(27), makeFacets(75, 12));
   public static final Property objectType = newProperty(1, BDynamicEnum.make(27, BEnumRange.make(BBacnetObjectType.TYPE)), makeFacets(79, 9));
   public static final Property logDeviceObjectProperty = newProperty(1, new BBacnetArray(BBacnetDeviceObjectPropertyReference.TYPE), makeFacets(132, -2));
   public static final Type TYPE = Sys.loadType(BBacnetTrendLogMultiple.class);

   public BBacnetArray getLogDeviceObjectProperty() {
      return (BBacnetArray)this.get(logDeviceObjectProperty);
   }

   public void setLogDeviceObjectProperty(BBacnetArray v) {
      this.set(logDeviceObjectProperty, v, null);
   }

   @Override
   public Type getType() {
      return TYPE;
   }
}
