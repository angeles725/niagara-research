package javax.baja.bacnet.config;

import javax.baja.bacnet.BBacnetObject;
import javax.baja.bacnet.datatypes.BBacnetDeviceObjectPropertyReference;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.datatypes.BBacnetUnsigned;
import javax.baja.bacnet.enums.BBacnetObjectType;
import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BDynamicEnum;
import javax.baja.sys.BEnumRange;
import javax.baja.sys.BFacets;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Slot;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "objectId",
      type = "BBacnetObjectIdentifier",
      defaultValue = "BBacnetObjectIdentifier.make(BBacnetObjectType.AVERAGING)",
      flags = 8,
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.OBJECT_IDENTIFIER, ASN_OBJECT_IDENTIFIER)")},
      override = true
   ), @NiagaraProperty(
      name = "objectType",
      type = "BEnum",
      defaultValue = "BDynamicEnum.make(BBacnetObjectType.AVERAGING, BEnumRange.make(BBacnetObjectType.TYPE))",
      flags = 1,
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.OBJECT_TYPE, ASN_ENUMERATED)")},
      override = true
   ), @NiagaraProperty(
      name = "facets",
      type = "BFacets",
      defaultValue = "BFacets.DEFAULT",
      flags = 1
   ), @NiagaraProperty(
      name = "minimumValue",
      type = "float",
      defaultValue = "0",
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.MINIMUM_VALUE, ASN_REAL)")}
   ), @NiagaraProperty(
      name = "averageValue",
      type = "float",
      defaultValue = "0",
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.AVERAGE_VALUE, ASN_REAL)")}
   ), @NiagaraProperty(
      name = "maximumValue",
      type = "float",
      defaultValue = "0",
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.MAXIMUM_VALUE, ASN_REAL)")}
   ), @NiagaraProperty(
      name = "attemptedSamples",
      type = "BBacnetUnsigned",
      defaultValue = "BBacnetUnsigned.make(0)",
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.ATTEMPTED_SAMPLES, ASN_UNSIGNED)")}
   ), @NiagaraProperty(
      name = "validSamples",
      type = "BBacnetUnsigned",
      defaultValue = "BBacnetUnsigned.make(0)",
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.VALID_SAMPLES, ASN_UNSIGNED)")}
   ), @NiagaraProperty(
      name = "objectPropertyReference",
      type = "BBacnetDeviceObjectPropertyReference",
      defaultValue = "new BBacnetDeviceObjectPropertyReference()",
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.OBJECT_PROPERTY_REFERENCE, ASN_CONSTRUCTED_DATA)")}
   ), @NiagaraProperty(
      name = "windowInterval",
      type = "BBacnetUnsigned",
      defaultValue = "BBacnetUnsigned.make(0)",
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.WINDOW_INTERVAL, ASN_UNSIGNED)")}
   ), @NiagaraProperty(
      name = "windowSamples",
      type = "BBacnetUnsigned",
      defaultValue = "BBacnetUnsigned.make(0)",
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.WINDOW_SAMPLES, ASN_UNSIGNED)")}
   )})
public class BBacnetAveraging extends BBacnetObject {
   public static final Property objectId = newProperty(8, BBacnetObjectIdentifier.make(18), makeFacets(75, 12));
   public static final Property objectType = newProperty(1, BDynamicEnum.make(18, BEnumRange.make(BBacnetObjectType.TYPE)), makeFacets(79, 9));
   public static final Property facets = newProperty(1, BFacets.DEFAULT, null);
   public static final Property minimumValue = newProperty(0, 0, makeFacets(136, 4));
   public static final Property averageValue = newProperty(0, 0, makeFacets(125, 4));
   public static final Property maximumValue = newProperty(0, 0, makeFacets(135, 4));
   public static final Property attemptedSamples = newProperty(0, BBacnetUnsigned.make(0L), makeFacets(124, 2));
   public static final Property validSamples = newProperty(0, BBacnetUnsigned.make(0L), makeFacets(146, 2));
   public static final Property objectPropertyReference = newProperty(0, new BBacnetDeviceObjectPropertyReference(), makeFacets(78, -1));
   public static final Property windowInterval = newProperty(0, BBacnetUnsigned.make(0L), makeFacets(147, 2));
   public static final Property windowSamples = newProperty(0, BBacnetUnsigned.make(0L), makeFacets(148, 2));
   public static final Type TYPE = Sys.loadType(BBacnetAveraging.class);

   public BFacets getFacets() {
      return (BFacets)this.get(facets);
   }

   public void setFacets(BFacets v) {
      this.set(facets, v, null);
   }

   public float getMinimumValue() {
      return this.getFloat(minimumValue);
   }

   public void setMinimumValue(float v) {
      this.setFloat(minimumValue, v, null);
   }

   public float getAverageValue() {
      return this.getFloat(averageValue);
   }

   public void setAverageValue(float v) {
      this.setFloat(averageValue, v, null);
   }

   public float getMaximumValue() {
      return this.getFloat(maximumValue);
   }

   public void setMaximumValue(float v) {
      this.setFloat(maximumValue, v, null);
   }

   public BBacnetUnsigned getAttemptedSamples() {
      return (BBacnetUnsigned)this.get(attemptedSamples);
   }

   public void setAttemptedSamples(BBacnetUnsigned v) {
      this.set(attemptedSamples, v, null);
   }

   public BBacnetUnsigned getValidSamples() {
      return (BBacnetUnsigned)this.get(validSamples);
   }

   public void setValidSamples(BBacnetUnsigned v) {
      this.set(validSamples, v, null);
   }

   public BBacnetDeviceObjectPropertyReference getObjectPropertyReference() {
      return (BBacnetDeviceObjectPropertyReference)this.get(objectPropertyReference);
   }

   public void setObjectPropertyReference(BBacnetDeviceObjectPropertyReference v) {
      this.set(objectPropertyReference, v, null);
   }

   public BBacnetUnsigned getWindowInterval() {
      return (BBacnetUnsigned)this.get(windowInterval);
   }

   public void setWindowInterval(BBacnetUnsigned v) {
      this.set(windowInterval, v, null);
   }

   public BBacnetUnsigned getWindowSamples() {
      return (BBacnetUnsigned)this.get(windowSamples);
   }

   public void setWindowSamples(BBacnetUnsigned v) {
      this.set(windowSamples, v, null);
   }

   @Override
   public Type getType() {
      return TYPE;
   }

   @Override
   public String toString(Context context) {
      StringBuilder sb = new StringBuilder();
      sb.append(this.getObjectId().toString(context));
      return sb.toString();
   }

   @Override
   public BFacets getSlotFacets(Slot slot) {
      if (slot.equals(minimumValue)) {
         return this.getFacets();
      } else if (slot.equals(averageValue)) {
         return this.getFacets();
      } else if (slot.equals(maximumValue)) {
         return this.getFacets();
      } else {
         return slot.getName().equals("varianceValue") ? this.getFacets() : super.getSlotFacets(slot);
      }
   }
}
