package javax.baja.bacnet.config;

import com.tridium.bacnet.asn.AsnUtil;
import javax.baja.bacnet.BBacnetObject;
import javax.baja.bacnet.datatypes.BBacnetArray;
import javax.baja.bacnet.datatypes.BBacnetBitString;
import javax.baja.bacnet.datatypes.BBacnetUnsigned;
import javax.baja.bacnet.enums.BBacnetEventState;
import javax.baja.bacnet.enums.BBacnetPropertyIdentifier;
import javax.baja.bacnet.util.BacnetBitStringUtil;
import javax.baja.naming.SlotPath;
import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BDynamicEnum;
import javax.baja.sys.BEnum;
import javax.baja.sys.BEnumRange;
import javax.baja.sys.BFacets;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Slot;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "presentValue",
      type = "BEnum",
      defaultValue = "BDynamicEnum.make(0)",
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.PRESENT_VALUE, ASN_UNSIGNED)")}
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
      name = "numberOfStates",
      type = "BBacnetUnsigned",
      defaultValue = "BBacnetUnsigned.DEFAULT",
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.NUMBER_OF_STATES, ASN_UNSIGNED)")}
   )})
public abstract class BBacnetMultistate extends BBacnetObject {
   public static final Property presentValue = newProperty(0, BDynamicEnum.make(0), makeFacets(85, 2));
   public static final Property facets = newProperty(1, BFacets.DEFAULT, null);
   public static final Property statusFlags = newProperty(
      1,
      BBacnetBitString.emptyBitString(BacnetBitStringUtil.getBitStringLength("BacnetStatusFlags")),
      makeFacets(111, 8, BacnetBitStringUtil.BACNET_STATUS_FLAGS_MAP)
   );
   public static final Property eventState = newProperty(1, BDynamicEnum.make(0, BEnumRange.make(BBacnetEventState.TYPE)), makeFacets(36, 9));
   public static final Property outOfService = newProperty(0, false, makeFacets(81, 1));
   public static final Property numberOfStates = newProperty(0, BBacnetUnsigned.DEFAULT, makeFacets(74, 2));
   public static final Type TYPE = Sys.loadType(BBacnetMultistate.class);

   public BEnum getPresentValue() {
      return (BEnum)this.get(presentValue);
   }

   public void setPresentValue(BEnum v) {
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

   public BBacnetUnsigned getNumberOfStates() {
      return (BBacnetUnsigned)this.get(numberOfStates);
   }

   public void setNumberOfStates(BBacnetUnsigned v) {
      this.set(numberOfStates, v, null);
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
      return slot == presentValue ? this.getFacets() : super.getSlotFacets(slot);
   }

   @Override
   public void setOutputFacets() {
      BBacnetArray stateText = (BBacnetArray)this.get(BBacnetPropertyIdentifier.stateText.getTag());
      if (stateText != null) {
         int[] ords = new int[stateText.getSize()];

         for (int i = 0; i < ords.length; i++) {
            ords[i] = i + 1;
         }

         String[] tags = new String[ords.length];

         for (int i = 0; i < tags.length; i++) {
            tags[i] = SlotPath.escape(stateText.getElement(i + 1).toString());
         }

         this.setFacets(BFacets.makeEnum(BEnumRange.make(ords, tags)));
      }
   }

   @Override
   public Property getPresentValueProperty() {
      return presentValue;
   }

   @Override
   protected byte[] toEncodedValue(BBacnetObject.BacnetPropertyData d, Property p) {
      return d.getPropertyId() == 85 ? AsnUtil.toAsnUnsigned(((BEnum)this.get(p)).getOrdinal()) : AsnUtil.toAsn(d.getAsnType(), this.get(p));
   }
}
