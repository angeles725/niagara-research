package javax.baja.bacnet.config;

import java.util.logging.Level;
import javax.baja.bacnet.BBacnetObject;
import javax.baja.bacnet.datatypes.BBacnetBitString;
import javax.baja.bacnet.enums.BBacnetEngineeringUnits;
import javax.baja.bacnet.enums.BBacnetEventState;
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
import javax.baja.sys.BInteger;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Slot;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.units.BUnit;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
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
      name = "units",
      type = "BEnum",
      defaultValue = "BDynamicEnum.make(BBacnetEngineeringUnits.NO_UNITS, BEnumRange.make(BBacnetEngineeringUnits.TYPE))",
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.UNITS, ASN_ENUMERATED)")}
   )})
public abstract class BBacnetAnalog extends BBacnetObject {
   public static final Property presentValue = newProperty(0, 0, makeFacets(85, 4));
   public static final Property facets = newProperty(1, BFacets.DEFAULT, null);
   public static final Property statusFlags = newProperty(
      1,
      BBacnetBitString.emptyBitString(BacnetBitStringUtil.getBitStringLength("BacnetStatusFlags")),
      makeFacets(111, 8, BacnetBitStringUtil.BACNET_STATUS_FLAGS_MAP)
   );
   public static final Property eventState = newProperty(1, BDynamicEnum.make(0, BEnumRange.make(BBacnetEventState.TYPE)), makeFacets(36, 9));
   public static final Property outOfService = newProperty(0, false, makeFacets(81, 1));
   public static final Property units = newProperty(0, BDynamicEnum.make(95, BEnumRange.make(BBacnetEngineeringUnits.TYPE)), makeFacets(117, 9));
   public static final Type TYPE = Sys.loadType(BBacnetAnalog.class);
   private static final double LN_10 = Math.log(10.0);

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

   public BEnum getUnits() {
      return (BEnum)this.get(units);
   }

   public void setUnits(BEnum v) {
      this.set(units, v, null);
   }

   @Override
   public Type getType() {
      return TYPE;
   }

   @Override
   public String toString(Context context) {
      StringBuilder sb = new StringBuilder();
      sb.append(this.getObjectId().toString(context)).append(" = " + this.getPresentValue());
      return sb.toString();
   }

   @Override
   public BFacets getSlotFacets(Slot slot) {
      if (slot.equals(presentValue)) {
         return this.getFacets();
      } else if (slot.getName().equals("highLimit")) {
         return this.getFacets();
      } else if (slot.getName().equals("lowLimit")) {
         return this.getFacets();
      } else if (slot.getName().equals("deadband")) {
         return this.getFacets();
      } else if (slot.getName().equals("resolution")) {
         return this.getFacets();
      } else {
         return slot.getName().equals("covIncrement") ? this.getFacets() : super.getSlotFacets(slot);
      }
   }

   @Override
   public void setOutputFacets() {
      try {
         BUnit u = null;

         try {
            u = BBacnetEngineeringUnits.getNiagaraUnits(this.getUnits().getOrdinal());
         } catch (Exception var7) {
            log.info(this + ":Can't make BUnits from BacnetEngineeringUnits:" + this.getUnits());
         }

         BFloat res = (BFloat)this.get("resolution");
         BInteger precision = BInteger.make(2);
         if (res != null && res.getFloat() > 0.0F) {
            precision = BInteger.make((int)Math.ceil(-(Math.log(res.getFloat()) / LN_10)));
         }

         BFloat minPV = (BFloat)this.get("minPresValue");
         BFloat maxPV = (BFloat)this.get("maxPresValue");
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

         BFacets f = BFacets.makeNumeric(u, precision, minPV, maxPV);
         this.setFacets(f);
      } catch (Exception var8) {
         log.log(Level.INFO, this + ":Exception in setOutputFacets()", (Throwable)var8);
      }
   }

   @Override
   public Property getPresentValueProperty() {
      return presentValue;
   }
}
