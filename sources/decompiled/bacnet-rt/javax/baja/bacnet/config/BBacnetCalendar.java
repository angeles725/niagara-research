package javax.baja.bacnet.config;

import javax.baja.bacnet.datatypes.BBacnetCalendarEntry;
import javax.baja.bacnet.datatypes.BBacnetListOf;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.enums.BBacnetObjectType;
import javax.baja.bacnet.io.PropertyValue;
import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.util.Array;
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
      defaultValue = "BBacnetObjectIdentifier.make(BBacnetObjectType.CALENDAR)",
      flags = 8,
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.OBJECT_IDENTIFIER, ASN_OBJECT_IDENTIFIER)")},
      override = true
   ), @NiagaraProperty(
      name = "objectType",
      type = "BEnum",
      defaultValue = "BDynamicEnum.make(BBacnetObjectType.CALENDAR, BEnumRange.make(BBacnetObjectType.TYPE))",
      flags = 1,
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.OBJECT_TYPE, ASN_ENUMERATED)")},
      override = true
   ), @NiagaraProperty(
      name = "presentValue",
      type = "boolean",
      defaultValue = "false",
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.PRESENT_VALUE, ASN_BOOLEAN)")}
   ), @NiagaraProperty(
      name = "datelist",
      type = "BBacnetListOf",
      defaultValue = "new BBacnetListOf(BBacnetCalendarEntry.TYPE)",
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.DATE_LIST, ASN_BACNET_LIST)")}
   )})
public class BBacnetCalendar extends BBacnetCreatableObject {
   public static final Property objectId = newProperty(8, BBacnetObjectIdentifier.make(6), makeFacets(75, 12));
   public static final Property objectType = newProperty(1, BDynamicEnum.make(6, BEnumRange.make(BBacnetObjectType.TYPE)), makeFacets(79, 9));
   public static final Property presentValue = newProperty(0, false, makeFacets(85, 1));
   public static final Property datelist = newProperty(0, new BBacnetListOf(BBacnetCalendarEntry.TYPE), makeFacets(23, -3));
   public static final Type TYPE = Sys.loadType(BBacnetCalendar.class);

   public boolean getPresentValue() {
      return this.getBoolean(presentValue);
   }

   public void setPresentValue(boolean v) {
      this.setBoolean(presentValue, v, null);
   }

   public BBacnetListOf getDatelist() {
      return (BBacnetListOf)this.get(datelist);
   }

   public void setDatelist(BBacnetListOf v) {
      this.set(datelist, v, null);
   }

   @Override
   public Type getType() {
      return TYPE;
   }

   @Override
   public String toString(Context context) {
      StringBuilder sb = new StringBuilder();
      sb.append(this.getObjectId().toString(context)).append((char)(nameContext.equals(context) ? '_' : ':')).append(this.getPresentValue());
      return sb.toString();
   }

   @Override
   protected void addObjectInitialValues(Array<PropertyValue> listOfInitialValues) {
   }

   @Override
   public Property getPresentValueProperty() {
      return presentValue;
   }
}
