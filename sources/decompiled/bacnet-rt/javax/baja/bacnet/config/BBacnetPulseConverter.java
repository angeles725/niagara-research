package javax.baja.bacnet.config;

import javax.baja.bacnet.datatypes.BBacnetDateTime;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
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
      defaultValue = "BBacnetObjectIdentifier.make(BBacnetObjectType.PULSE_CONVERTER)",
      flags = 8,
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.OBJECT_IDENTIFIER, ASN_OBJECT_IDENTIFIER)")},
      override = true
   ), @NiagaraProperty(
      name = "objectType",
      type = "BEnum",
      defaultValue = "BDynamicEnum.make(BBacnetObjectType.PULSE_CONVERTER, BEnumRange.make(BBacnetObjectType.TYPE))",
      flags = 1,
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.OBJECT_TYPE, ASN_ENUMERATED)")},
      override = true
   ), @NiagaraProperty(
      name = "scaleFactor",
      type = "float",
      defaultValue = "0.0f",
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.SCALE_FACTOR, ASN_REAL)")}
   ), @NiagaraProperty(
      name = "adjustValue",
      type = "float",
      defaultValue = "0.0f",
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.ADJUST_VALUE, ASN_REAL)")}
   ), @NiagaraProperty(
      name = "count",
      type = "BBacnetUnsigned",
      defaultValue = "BBacnetUnsigned.DEFAULT",
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.COUNT, ASN_UNSIGNED)")}
   ), @NiagaraProperty(
      name = "updateTime",
      type = "BBacnetDateTime",
      defaultValue = "new BBacnetDateTime()",
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.UPDATE_TIME, ASN_CONSTRUCTED_DATA)")}
   ), @NiagaraProperty(
      name = "countChangeTime",
      type = "BBacnetDateTime",
      defaultValue = "new BBacnetDateTime()",
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.UPDATE_TIME, ASN_CONSTRUCTED_DATA)")}
   ), @NiagaraProperty(
      name = "countBeforeChange",
      type = "BBacnetUnsigned",
      defaultValue = "BBacnetUnsigned.DEFAULT",
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.COUNT_BEFORE_CHANGE, ASN_UNSIGNED)")}
   )})
public class BBacnetPulseConverter extends BBacnetAnalog {
   public static final Property objectId = newProperty(8, BBacnetObjectIdentifier.make(24), makeFacets(75, 12));
   public static final Property objectType = newProperty(1, BDynamicEnum.make(24, BEnumRange.make(BBacnetObjectType.TYPE)), makeFacets(79, 9));
   public static final Property scaleFactor = newProperty(0, 0.0F, makeFacets(188, 4));
   public static final Property adjustValue = newProperty(0, 0.0F, makeFacets(176, 4));
   public static final Property count = newProperty(0, BBacnetUnsigned.DEFAULT, makeFacets(177, 2));
   public static final Property updateTime = newProperty(0, new BBacnetDateTime(), makeFacets(189, -1));
   public static final Property countChangeTime = newProperty(0, new BBacnetDateTime(), makeFacets(189, -1));
   public static final Property countBeforeChange = newProperty(0, BBacnetUnsigned.DEFAULT, makeFacets(178, 2));
   public static final Type TYPE = Sys.loadType(BBacnetPulseConverter.class);

   public float getScaleFactor() {
      return this.getFloat(scaleFactor);
   }

   public void setScaleFactor(float v) {
      this.setFloat(scaleFactor, v, null);
   }

   public float getAdjustValue() {
      return this.getFloat(adjustValue);
   }

   public void setAdjustValue(float v) {
      this.setFloat(adjustValue, v, null);
   }

   public BBacnetUnsigned getCount() {
      return (BBacnetUnsigned)this.get(count);
   }

   public void setCount(BBacnetUnsigned v) {
      this.set(count, v, null);
   }

   public BBacnetDateTime getUpdateTime() {
      return (BBacnetDateTime)this.get(updateTime);
   }

   public void setUpdateTime(BBacnetDateTime v) {
      this.set(updateTime, v, null);
   }

   public BBacnetDateTime getCountChangeTime() {
      return (BBacnetDateTime)this.get(countChangeTime);
   }

   public void setCountChangeTime(BBacnetDateTime v) {
      this.set(countChangeTime, v, null);
   }

   public BBacnetUnsigned getCountBeforeChange() {
      return (BBacnetUnsigned)this.get(countBeforeChange);
   }

   public void setCountBeforeChange(BBacnetUnsigned v) {
      this.set(countBeforeChange, v, null);
   }

   @Override
   public Type getType() {
      return TYPE;
   }
}
