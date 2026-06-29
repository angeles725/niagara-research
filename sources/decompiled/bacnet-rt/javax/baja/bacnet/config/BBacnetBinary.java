package javax.baja.bacnet.config;

import javax.baja.bacnet.BBacnetObject;
import javax.baja.bacnet.datatypes.BBacnetBitString;
import javax.baja.bacnet.enums.BBacnetBinaryPv;
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
import javax.baja.sys.BString;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Slot;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "presentValue",
      type = "BBacnetBinaryPv",
      defaultValue = "BBacnetBinaryPv.inactive",
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.PRESENT_VALUE, ASN_ENUMERATED)")}
   ), @NiagaraProperty(
      name = "facets",
      type = "BFacets",
      defaultValue = "BFacets.DEFAULT",
      flags = 5
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
   )})
public abstract class BBacnetBinary extends BBacnetObject {
   public static final Property presentValue = newProperty(0, BBacnetBinaryPv.inactive, makeFacets(85, 9));
   public static final Property facets = newProperty(5, BFacets.DEFAULT, null);
   public static final Property statusFlags = newProperty(
      1,
      BBacnetBitString.emptyBitString(BacnetBitStringUtil.getBitStringLength("BacnetStatusFlags")),
      makeFacets(111, 8, BacnetBitStringUtil.BACNET_STATUS_FLAGS_MAP)
   );
   public static final Property eventState = newProperty(1, BDynamicEnum.make(0, BEnumRange.make(BBacnetEventState.TYPE)), makeFacets(36, 9));
   public static final Property outOfService = newProperty(0, false, makeFacets(81, 1));
   public static final Type TYPE = Sys.loadType(BBacnetBinary.class);

   public BBacnetBinaryPv getPresentValue() {
      return (BBacnetBinaryPv)this.get(presentValue);
   }

   public void setPresentValue(BBacnetBinaryPv v) {
      this.set(presentValue, v, null);
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

   @Override
   public Type getType() {
      return TYPE;
   }

   @Override
   public String toString(Context context) {
      StringBuilder sb = new StringBuilder();
      sb.append(this.getObjectId().toString(context)).append(" = " + this.getPresentValue().toString(context));
      return sb.toString();
   }

   @Override
   public BFacets getSlotFacets(Slot slot) {
      if (slot == presentValue) {
         return this.getFacets();
      } else if (slot.getName().equals("alarmValue")) {
         return this.getFacets();
      } else if (slot.getName().equals("feedbackValue")) {
         return this.getFacets();
      } else {
         return slot.getName().equals("relinquishDefault") ? this.getFacets() : super.getSlotFacets(slot);
      }
   }

   @Override
   public void setOutputFacets() {
      BString activeText = (BString)this.get("activeText");
      BString inactiveText = (BString)this.get("inactiveText");
      BFacets f = BFacets.makeBoolean(activeText != null ? activeText : BString.make("true"), inactiveText != null ? inactiveText : BString.make("false"));
      this.setFacets(f);
   }

   @Override
   public Property getPresentValueProperty() {
      return presentValue;
   }
}
