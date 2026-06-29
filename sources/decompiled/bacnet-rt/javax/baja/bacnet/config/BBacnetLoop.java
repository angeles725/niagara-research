package javax.baja.bacnet.config;

import javax.baja.bacnet.BBacnetObject;
import javax.baja.bacnet.datatypes.BBacnetBitString;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.datatypes.BBacnetObjectPropertyReference;
import javax.baja.bacnet.datatypes.BBacnetSetpointReference;
import javax.baja.bacnet.datatypes.BBacnetUnsigned;
import javax.baja.bacnet.enums.BBacnetAction;
import javax.baja.bacnet.enums.BBacnetEngineeringUnits;
import javax.baja.bacnet.enums.BBacnetEventState;
import javax.baja.bacnet.enums.BBacnetObjectType;
import javax.baja.bacnet.util.BacnetBitStringUtil;
import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BDynamicEnum;
import javax.baja.sys.BEnum;
import javax.baja.sys.BEnumRange;
import javax.baja.sys.BFacets;
import javax.baja.sys.BFloat;
import javax.baja.sys.BValue;
import javax.baja.sys.Context;
import javax.baja.sys.InvalidEnumException;
import javax.baja.sys.Property;
import javax.baja.sys.Slot;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.units.BUnit;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "objectId",
      type = "BBacnetObjectIdentifier",
      defaultValue = "BBacnetObjectIdentifier.make(BBacnetObjectType.LOOP)",
      flags = 8,
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.OBJECT_IDENTIFIER, ASN_OBJECT_IDENTIFIER)")},
      override = true
   ), @NiagaraProperty(
      name = "objectType",
      type = "BEnum",
      defaultValue = "BDynamicEnum.make(BBacnetObjectType.LOOP, BEnumRange.make(BBacnetObjectType.TYPE))",
      flags = 1,
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.OBJECT_TYPE, ASN_ENUMERATED)")},
      override = true
   ), @NiagaraProperty(
      name = "presentValue",
      type = "float",
      defaultValue = "0",
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.PRESENT_VALUE, ASN_REAL)")}
   ), @NiagaraProperty(
      name = "facets",
      type = "BFacets",
      defaultValue = "BFacets.DEFAULT",
      flags = 1
   ), @NiagaraProperty(
      name = "statusFlags",
      type = "BBacnetBitString",
      defaultValue = "BBacnetBitString.emptyBitString(BacnetBitStringUtil.getBitStringLength(\"BacnetStatusFlags\"))",
      flags = 1,
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.STATUS_FLAGS, ASN_BIT_STRING, BacnetBitStringUtil.BACNET_STATUS_FLAGS_MAP)")}
   ), @NiagaraProperty(
      name = "eventState",
      type = "BEnum",
      defaultValue = "BDynamicEnum.make(BBacnetEventState.NORMAL, BEnumRange.make(BBacnetEventState.TYPE))",
      flags = 1,
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.EVENT_STATE, ASN_ENUMERATED)")}
   ), @NiagaraProperty(
      name = "outOfService",
      type = "boolean",
      defaultValue = "false",
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.OUT_OF_SERVICE, ASN_BOOLEAN)")}
   ), @NiagaraProperty(
      name = "outputUnits",
      type = "BEnum",
      defaultValue = "BDynamicEnum.make(BBacnetEngineeringUnits.NO_UNITS, BEnumRange.make(BBacnetEngineeringUnits.TYPE))",
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.OUTPUT_UNITS, ASN_ENUMERATED)")}
   ), @NiagaraProperty(
      name = "manipulatedVariableReference",
      type = "BBacnetObjectPropertyReference",
      defaultValue = "new BBacnetObjectPropertyReference()",
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.MANIPULATED_VARIABLE_REFERENCE, ASN_CONSTRUCTED_DATA)")}
   ), @NiagaraProperty(
      name = "controlledVariableReference",
      type = "BBacnetObjectPropertyReference",
      defaultValue = "new BBacnetObjectPropertyReference()",
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.CONTROLLED_VARIABLE_REFERENCE, ASN_CONSTRUCTED_DATA)")}
   ), @NiagaraProperty(
      name = "controlledVariableValue",
      type = "float",
      defaultValue = "0",
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.CONTROLLED_VARIABLE_VALUE, ASN_REAL)")}
   ), @NiagaraProperty(
      name = "controlledVariableUnits",
      type = "BEnum",
      defaultValue = "BDynamicEnum.make(BBacnetEngineeringUnits.NO_UNITS, BEnumRange.make(BBacnetEngineeringUnits.TYPE))",
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.CONTROLLED_VARIABLE_UNITS, ASN_ENUMERATED)")}
   ), @NiagaraProperty(
      name = "setpointReference",
      type = "BBacnetSetpointReference",
      defaultValue = "new BBacnetSetpointReference()",
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.SETPOINT_REFERENCE, ASN_CONSTRUCTED_DATA)")}
   ), @NiagaraProperty(
      name = "setpoint",
      type = "float",
      defaultValue = "0",
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.SETPOINT, ASN_REAL)")}
   ), @NiagaraProperty(
      name = "action",
      type = "BBacnetAction",
      defaultValue = "BBacnetAction.direct",
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.ACTION, ASN_ENUMERATED)")}
   ), @NiagaraProperty(
      name = "priorityForWriting",
      type = "BBacnetUnsigned",
      defaultValue = "BBacnetUnsigned.make(16)",
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.PRIORITY_FOR_WRITING, ASN_UNSIGNED)")}
   )})
public class BBacnetLoop extends BBacnetObject {
   public static final Property objectId = newProperty(8, BBacnetObjectIdentifier.make(12), makeFacets(75, 12));
   public static final Property objectType = newProperty(1, BDynamicEnum.make(12, BEnumRange.make(BBacnetObjectType.TYPE)), makeFacets(79, 9));
   public static final Property presentValue = newProperty(0, 0, makeFacets(85, 4));
   public static final Property facets = newProperty(1, BFacets.DEFAULT, null);
   public static final Property statusFlags = newProperty(
      1,
      BBacnetBitString.emptyBitString(BacnetBitStringUtil.getBitStringLength("BacnetStatusFlags")),
      makeFacets(111, 8, BacnetBitStringUtil.BACNET_STATUS_FLAGS_MAP)
   );
   public static final Property eventState = newProperty(1, BDynamicEnum.make(0, BEnumRange.make(BBacnetEventState.TYPE)), makeFacets(36, 9));
   public static final Property outOfService = newProperty(0, false, makeFacets(81, 1));
   public static final Property outputUnits = newProperty(0, BDynamicEnum.make(95, BEnumRange.make(BBacnetEngineeringUnits.TYPE)), makeFacets(82, 9));
   public static final Property manipulatedVariableReference = newProperty(0, new BBacnetObjectPropertyReference(), makeFacets(60, -1));
   public static final Property controlledVariableReference = newProperty(0, new BBacnetObjectPropertyReference(), makeFacets(19, -1));
   public static final Property controlledVariableValue = newProperty(0, 0, makeFacets(21, 4));
   public static final Property controlledVariableUnits = newProperty(
      0, BDynamicEnum.make(95, BEnumRange.make(BBacnetEngineeringUnits.TYPE)), makeFacets(20, 9)
   );
   public static final Property setpointReference = newProperty(0, new BBacnetSetpointReference(), makeFacets(109, -1));
   public static final Property setpoint = newProperty(0, 0, makeFacets(108, 4));
   public static final Property action = newProperty(0, BBacnetAction.direct, makeFacets(2, 9));
   public static final Property priorityForWriting = newProperty(0, BBacnetUnsigned.make(16L), makeFacets(88, 2));
   public static final Type TYPE = Sys.loadType(BBacnetLoop.class);
   private static final String PROPORTIONAL_CONSTANT = "proportionalConstant";
   private static final String INTEGRAL_CONSTANT = "integralConstant";
   private static final String DERIVATIVE_CONSTANT = "derivativeConstant";
   private static final String PROPORTIONAL_CONSTANT_UNITS = "proportionalConstantUnits";
   private static final String INTEGRAL_CONSTANT_UNITS = "integralConstantUnits";
   private static final String DERIVATIVE_CONSTANT_UNITS = "derivativeConstantUnits";
   private BFacets proportionalFacets;
   private BFacets integralFacets;
   private BFacets derivativeFacets;

   public float getPresentValue() {
      return this.getFloat(presentValue);
   }

   public void setPresentValue(float v) {
      this.setFloat(presentValue, v, null);
   }

   public BFacets getFacets() {
      return (BFacets)this.get(facets);
   }

   public void setFacets(BFacets v) {
      this.set(facets, v, null);
   }

   public BBacnetBitString getStatusFlags() {
      return (BBacnetBitString)this.get(statusFlags);
   }

   public void setStatusFlags(BBacnetBitString v) {
      this.set(statusFlags, v, null);
   }

   public BEnum getEventState() {
      return (BEnum)this.get(eventState);
   }

   public void setEventState(BEnum v) {
      this.set(eventState, v, null);
   }

   public boolean getOutOfService() {
      return this.getBoolean(outOfService);
   }

   public void setOutOfService(boolean v) {
      this.setBoolean(outOfService, v, null);
   }

   public BEnum getOutputUnits() {
      return (BEnum)this.get(outputUnits);
   }

   public void setOutputUnits(BEnum v) {
      this.set(outputUnits, v, null);
   }

   public BBacnetObjectPropertyReference getManipulatedVariableReference() {
      return (BBacnetObjectPropertyReference)this.get(manipulatedVariableReference);
   }

   public void setManipulatedVariableReference(BBacnetObjectPropertyReference v) {
      this.set(manipulatedVariableReference, v, null);
   }

   public BBacnetObjectPropertyReference getControlledVariableReference() {
      return (BBacnetObjectPropertyReference)this.get(controlledVariableReference);
   }

   public void setControlledVariableReference(BBacnetObjectPropertyReference v) {
      this.set(controlledVariableReference, v, null);
   }

   public float getControlledVariableValue() {
      return this.getFloat(controlledVariableValue);
   }

   public void setControlledVariableValue(float v) {
      this.setFloat(controlledVariableValue, v, null);
   }

   public BEnum getControlledVariableUnits() {
      return (BEnum)this.get(controlledVariableUnits);
   }

   public void setControlledVariableUnits(BEnum v) {
      this.set(controlledVariableUnits, v, null);
   }

   public BBacnetSetpointReference getSetpointReference() {
      return (BBacnetSetpointReference)this.get(setpointReference);
   }

   public void setSetpointReference(BBacnetSetpointReference v) {
      this.set(setpointReference, v, null);
   }

   public float getSetpoint() {
      return this.getFloat(setpoint);
   }

   public void setSetpoint(float v) {
      this.setFloat(setpoint, v, null);
   }

   public BBacnetAction getAction() {
      return (BBacnetAction)this.get(action);
   }

   public void setAction(BBacnetAction v) {
      this.set(action, v, null);
   }

   public BBacnetUnsigned getPriorityForWriting() {
      return (BBacnetUnsigned)this.get(priorityForWriting);
   }

   public void setPriorityForWriting(BBacnetUnsigned v) {
      this.set(priorityForWriting, v, null);
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
   public void started() throws Exception {
      super.started();
      this.proportionalFacets = this.getFacetsFromUnits("proportionalConstantUnits", 2);
      this.integralFacets = this.getFacetsFromUnits("integralConstantUnits", 2);
      this.derivativeFacets = this.getFacetsFromUnits("derivativeConstantUnits", 2);
   }

   @Override
   public void stopped() throws Exception {
      super.stopped();
      this.proportionalFacets = null;
      this.integralFacets = null;
      this.derivativeFacets = null;
   }

   @Override
   public void changed(Property p, Context cx) {
      super.changed(p, cx);
      if (this.isRunning()) {
         if (p.getName().equals("proportionalConstantUnits")) {
            this.proportionalFacets = this.getFacetsFromUnits("proportionalConstantUnits", 2);
         } else if (p.getName().equals("integralConstantUnits")) {
            this.integralFacets = this.getFacetsFromUnits("integralConstantUnits", 2);
         } else if (p.getName().equals("derivativeConstantUnits")) {
            this.derivativeFacets = this.getFacetsFromUnits("derivativeConstantUnits", 2);
         }
      }
   }

   public void added(Property p, Context cx) {
      super.added(p, cx);
      if (this.isRunning()) {
         if (p.getName().equals("proportionalConstantUnits")) {
            this.proportionalFacets = this.getFacetsFromUnits("proportionalConstantUnits", 2);
         } else if (p.getName().equals("integralConstantUnits")) {
            this.integralFacets = this.getFacetsFromUnits("integralConstantUnits", 2);
         } else if (p.getName().equals("derivativeConstantUnits")) {
            this.derivativeFacets = this.getFacetsFromUnits("derivativeConstantUnits", 2);
         }
      }
   }

   public void removed(Property p, BValue oldValue, Context cx) {
      super.removed(p, oldValue, cx);
      if (this.isRunning()) {
         if (p.getName().equals("proportionalConstantUnits")) {
            this.proportionalFacets = null;
         } else if (p.getName().equals("integralConstantUnits")) {
            this.integralFacets = null;
         } else if (p.getName().equals("derivativeConstantUnits")) {
            this.derivativeFacets = null;
         }
      }
   }

   @Override
   public BFacets getSlotFacets(Slot s) {
      if (s.equals(presentValue)) {
         return this.getFacets();
      } else if (s.getName().equals("proportionalConstant")) {
         return this.proportionalFacets != null ? this.proportionalFacets : BFacets.DEFAULT;
      } else if (s.getName().equals("integralConstant")) {
         return this.integralFacets != null ? this.integralFacets : BFacets.DEFAULT;
      } else if (s.getName().equals("derivativeConstant")) {
         return this.derivativeFacets != null ? this.derivativeFacets : BFacets.DEFAULT;
      } else {
         return super.getSlotFacets(s);
      }
   }

   @Override
   public void setOutputFacets() {
      BUnit u = null;

      try {
         u = BBacnetEngineeringUnits.make(this.getOutputUnits().getOrdinal()).getNiagaraUnits();
      } catch (InvalidEnumException var5) {
         log.warning("Can't make BUnits from BacnetEngineeringUnits:" + this.getOutputUnits());
      }

      BFloat minPV = (BFloat)this.get("minimumOutput");
      BFloat maxPV = (BFloat)this.get("maximumOutput");
      if (minPV == null) {
         minPV = BFloat.make(Float.NEGATIVE_INFINITY);
      }

      if (maxPV == null) {
         maxPV = BFloat.make(Float.POSITIVE_INFINITY);
      }

      if (minPV.getFloat() == -Float.MAX_VALUE) {
         minPV = BFloat.make(Float.NEGATIVE_INFINITY);
      }

      if (maxPV.getFloat() == Float.MAX_VALUE) {
         maxPV = BFloat.make(Float.POSITIVE_INFINITY);
      }

      BFacets f = BFacets.makeNumeric(u, minPV, maxPV);
      this.setFacets(f);
   }

   @Override
   public Property getPresentValueProperty() {
      return presentValue;
   }

   private BFacets getFacetsFromUnits(String units, int precision) {
      BEnum u = (BEnum)this.get(units);
      return u != null ? BFacets.makeNumeric(BBacnetEngineeringUnits.getNiagaraUnits(u.getOrdinal()), precision) : null;
   }
}
