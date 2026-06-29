package javax.baja.bacnet.datatypes;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.io.AsnInput;
import javax.baja.bacnet.io.AsnOutput;
import javax.baja.bacnet.virtual.BBacnetVirtualProperty;
import javax.baja.bacnet.virtual.BacnetVirtualUtil;
import javax.baja.category.BCategoryMask;
import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraTopic;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.security.BPermissions;
import javax.baja.spy.SpyWriter;
import javax.baja.sys.Action;
import javax.baja.sys.BAbsTime;
import javax.baja.sys.BBoolean;
import javax.baja.sys.BComponent;
import javax.baja.sys.BDouble;
import javax.baja.sys.BDynamicEnum;
import javax.baja.sys.BFacets;
import javax.baja.sys.BSimple;
import javax.baja.sys.BString;
import javax.baja.sys.BTime;
import javax.baja.sys.BValue;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.SlotCursor;
import javax.baja.sys.Sys;
import javax.baja.sys.Topic;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "eventName",
      type = "String",
      defaultValue = "event"
   ), @NiagaraProperty(
      name = "periodChoice",
      type = "int",
      defaultValue = "0",
      facets = {@Facet("BFacets.makeInt(0,1)")}
   ), @NiagaraProperty(
      name = "period",
      type = "BValue",
      defaultValue = "new BBacnetCalendarEntry()"
   ), @NiagaraProperty(
      name = "eventPriority",
      type = "int",
      defaultValue = "16",
      facets = {@Facet("BFacets.makeInt(1, 16)")}
   )})
@NiagaraAction(
   name = "addTimeValue",
   parameterType = "BBacnetTimeValue",
   defaultValue = "new BBacnetTimeValue()"
)
@NiagaraTopic(
   name = "specialEventChanged"
)
public class BBacnetSpecialEvent extends BComponent implements BIBacnetDataType {
   public static final Property eventName = newProperty(0, "event", null);
   public static final Property periodChoice = newProperty(0, 0, BFacets.makeInt(0, 1));
   public static final Property period = newProperty(0, new BBacnetCalendarEntry(), null);
   public static final Property eventPriority = newProperty(0, 16, BFacets.makeInt(1, 16));
   public static final Action addTimeValue = newAction(0, new BBacnetTimeValue(), null);
   public static final Topic specialEventChanged = newTopic(0, null);
   public static final Type TYPE = Sys.loadType(BBacnetSpecialEvent.class);
   private static final Logger logger = Logger.getLogger("bacnet.debug");
   public static final int CALENDAR_ENTRY_TAG = 0;
   public static final int CALENDAR_REFERENCE_TAG = 1;
   public static final int LIST_OF_TIME_VALUES_TAG = 2;
   public static final int EVENT_PRIORITY_TAG = 3;

   public String getEventName() {
      return this.getString(eventName);
   }

   public void setEventName(String v) {
      this.setString(eventName, v, null);
   }

   public int getPeriodChoice() {
      return this.getInt(periodChoice);
   }

   public void setPeriodChoice(int v) {
      this.setInt(periodChoice, v, null);
   }

   public BValue getPeriod() {
      return this.get(period);
   }

   public void setPeriod(BValue v) {
      this.set(period, v, null);
   }

   public int getEventPriority() {
      return this.getInt(eventPriority);
   }

   public void setEventPriority(int v) {
      this.setInt(eventPriority, v, null);
   }

   public void addTimeValue(BBacnetTimeValue parameter) {
      this.invoke(addTimeValue, parameter, null);
   }

   public void fireSpecialEventChanged(BValue event) {
      this.fire(specialEventChanged, event, null);
   }

   public Type getType() {
      return TYPE;
   }

   public String toString(Context context) {
      StringBuilder sb = new StringBuilder();
      sb.append(this.getEventName()).append("[" + this.getPeriodChoice() + "]: ").append(this.getPeriod()).append(" pri" + this.getEventPriority() + ":");
      SlotCursor<Property> c = this.getProperties();

      while (c.next(BBacnetTimeValue.class)) {
         sb.append(" ").append(c.get().toString(context));
      }

      return sb.toString();
   }

   public void started() {
      if (!BacnetVirtualUtil.isVirtual(this) && this.getParent() instanceof BBacnetArray) {
         ((BBacnetArray)this.getParent()).linkTo(this, specialEventChanged, BBacnetArray.arrayPropertyChanged);
      }

      this.syncPeriod(true);
   }

   public void added(Property p, Context cx) {
      if (this.isMounted()) {
         if (this.isRunning()) {
            if (cx != noWrite) {
               this.sort();
               this.getParent().asComponent().changed(this.getPropertyInParent(), cx);
               this.fireSpecialEventChanged(null);
            }
         }
      }
   }

   public void removed(Property p, BValue v, Context cx) {
      if (this.isMounted()) {
         if (this.isRunning()) {
            if (cx != noWrite) {
               this.sort();
               this.getParent().asComponent().changed(this.getPropertyInParent(), cx);
               this.fireSpecialEventChanged(null);
            }
         }
      }
   }

   public void changed(Property p, Context cx) {
      super.changed(p, cx);
      if (this.isMounted()) {
         if (this.isRunning()) {
            if (p.equals(periodChoice)) {
               if (cx != noWrite) {
                  this.syncPeriod(false);
               }
            } else if (p.equals(period) && cx != noWrite) {
               this.syncPeriod(true);
            }

            if (cx != noWrite) {
               this.sort();
               this.getParent().asComponent().changed(this.getPropertyInParent(), cx);
               this.fireSpecialEventChanged(null);
            }
         }
      }
   }

   private void syncPeriod(boolean fromPeriod) {
      if (fromPeriod) {
         BValue per = this.getPeriod();
         if (per instanceof BBacnetCalendarEntry) {
            this.setInt(periodChoice, 0, noWrite);
         } else if (per instanceof BBacnetObjectIdentifier) {
            this.setInt(periodChoice, 1, noWrite);
         }
      } else {
         int perch = this.getPeriodChoice();
         if (perch == 0) {
            this.set(period, new BBacnetCalendarEntry(), noWrite);
         } else if (perch == 1) {
            this.set(period, BBacnetObjectIdentifier.make(6), noWrite);
         }
      }
   }

   public final void subscribed() {
      BBacnetVirtualProperty vp = BacnetVirtualUtil.getVirtualProperty(this);
      if (vp != null) {
         vp.childSubscribed(this);
      }
   }

   public final void unsubscribed() {
      BBacnetVirtualProperty vp = BacnetVirtualUtil.getVirtualProperty(this);
      if (vp != null) {
         vp.childUnsubscribed(this);
      }
   }

   public BCategoryMask getAppliedCategoryMask() {
      return BacnetVirtualUtil.isVirtual(this) ? this.getParent().asComponent().getAppliedCategoryMask() : super.getAppliedCategoryMask();
   }

   public BCategoryMask getCategoryMask() {
      return BacnetVirtualUtil.isVirtual(this) ? this.getParent().asComponent().getCategoryMask() : super.getCategoryMask();
   }

   public BPermissions getPermissions(Context cx) {
      return BacnetVirtualUtil.isVirtual(this) ? this.getParent().asComponent().getPermissions(cx) : super.getPermissions(cx);
   }

   public final void doAddTimeValue(BBacnetTimeValue tv) {
      this.add(null, tv);
   }

   @Override
   public final void writeAsn(AsnOutput out) {
      switch (this.getPeriodChoice()) {
         case 0:
            out.writeOpeningTag(0);
            ((BBacnetCalendarEntry)this.getPeriod()).writeAsn(out);
            out.writeClosingTag(0);
            break;
         case 1:
            out.writeObjectIdentifier(1, (BBacnetObjectIdentifier)this.getPeriod());
      }

      out.writeOpeningTag(2);
      SlotCursor<Property> c = this.getProperties();

      while (c.next(BBacnetTimeValue.class)) {
         ((BBacnetTimeValue)c.get()).writeAsn(out);
      }

      out.writeClosingTag(2);
      out.writeUnsignedInteger(3, this.getEventPriority());
   }

   @Override
   public final void readAsn(AsnInput in) throws AsnException {
      int periodChoice = in.peekTag();
      BValue period;
      switch (periodChoice) {
         case 0:
            in.skipOpeningTag(0);
            period = new BBacnetCalendarEntry();
            ((BBacnetCalendarEntry)period).readAsn(in);
            in.skipClosingTag(0);
            break;
         case 1:
            period = in.readObjectIdentifier(1);
            break;
         default:
            throw new AsnException("Invalid tag: " + periodChoice);
      }

      List<BBacnetTimeValue> timeValues = new ArrayList<>();
      in.skipOpeningTag(2);

      for (int tag = in.peekTag(); !in.isClosingTag(2); tag = in.peekTag()) {
         if (tag == -1) {
            throw new AsnException("Invalid tag: " + tag);
         }

         BBacnetTime time = in.readTime();
         int asnType = in.peekApplicationTag();
         BSimple value;
         switch (asnType) {
            case 0:
               value = in.readNull();
               break;
            case 1:
               value = BBoolean.make(in.readBoolean());
               break;
            case 2:
               value = in.readUnsigned();
               break;
            case 3:
               value = in.readSigned();
               break;
            case 4:
               value = in.readFloat();
               break;
            case 5:
               value = BDouble.make(in.readDouble());
               break;
            case 6:
               value = in.readBacnetOctetString();
               break;
            case 7:
               value = BString.make(in.readCharacterString());
               break;
            case 8:
               value = in.readBitString();
               break;
            case 9:
               value = BDynamicEnum.make(in.readEnumerated());
               break;
            case 10:
               value = in.readDate();
               break;
            case 11:
               value = in.readTime();
               break;
            case 12:
               value = in.readObjectIdentifier();
               break;
            default:
               throw new AsnException("Invalid tag: " + asnType);
         }

         timeValues.add(new BBacnetTimeValue(time, value));
      }

      in.skipClosingTag(2);
      int eventPriority = in.readUnsignedInt(3);
      this.setInt(BBacnetSpecialEvent.periodChoice, periodChoice, noWrite);
      this.set(BBacnetSpecialEvent.period, period, noWrite);
      this.removeAll(noWrite);
      int length = timeValues.size();

      for (int i = 0; i < length; i++) {
         this.add("timeValue" + (i + 1), (BValue)timeValues.get(i), noWrite);
      }

      this.setInt(BBacnetSpecialEvent.eventPriority, eventPriority, noWrite);
   }

   public final BAbsTime nextEvent(BAbsTime time) {
      switch (this.getPeriodChoice()) {
         case 0:
            BBacnetCalendarEntry e = (BBacnetCalendarEntry)this.getPeriod();
            BAbsTime date = e.nextDate(time);
            if (date == null) {
               return null;
            }

            BTime tod = BTime.make(time);
            BBacnetTimeValue tvnext = null;
            SlotCursor<Property> c = this.getProperties();

            while (c.next(BBacnetTimeValue.class)) {
               BBacnetTimeValue tv = (BBacnetTimeValue)c.get();
               if (tv.getTime().isAfter(tod) && (tvnext == null || tv.isBefore(tvnext))) {
                  tvnext = tv;
               }
            }

            return tvnext == null ? null : BBacnetDateTime.makeBAbsTime(date, tvnext.getTime());
         case 1:
            logger.severe("Calendar reference choice not supported!");
            return null;
         default:
            logger.severe("Calendar tag choice not supported!");
            return null;
      }
   }

   public final BSimple getValue(BAbsTime time) {
      if (!this.isActive(time)) {
         return null;
      } else {
         BTime tod = BTime.make(time);
         BBacnetTimeValue tv = null;
         BBacnetTimeValue tvlast = null;
         SlotCursor<Property> c = this.getProperties();

         while (c.next(BBacnetTimeValue.class)) {
            tv = (BBacnetTimeValue)c.get();
            if (tv.getTime().isNotAfter(tod) && (tvlast == null || tv.isNotBefore(tvlast))) {
               tvlast = tv;
            }
         }

         if (tvlast == null) {
            tvlast = tv;
         }

         return (BSimple)(tvlast == null ? BBacnetNull.DEFAULT : tvlast.getValue().getAny());
      }
   }

   private boolean isActive(BAbsTime time) {
      switch (this.getPeriodChoice()) {
         case 0:
            return ((BBacnetCalendarEntry)this.getPeriod()).isActive(time);
         case 1:
            logger.severe("Calendar reference choice not supported!");
            return false;
         default:
            logger.severe("Calendar tag choice not supported!");
            return false;
      }
   }

   private BBacnetTimeValue getTV(BBacnetTime t) {
      SlotCursor<Property> c = this.getProperties();

      while (c.next(BBacnetTimeValue.class)) {
         if (((BBacnetTimeValue)c.get()).getTime().equals(t)) {
            return (BBacnetTimeValue)c.get();
         }
      }

      return null;
   }

   private void sort() {
      Property[] props = this.getPropertiesArray();
      if (props.length > 4) {
         Property[] tvs = new Property[props.length - 4];
         System.arraycopy(props, 4, tvs, 0, tvs.length);

         for (int i = 0; i < tvs.length - 1; i++) {
            int small = i;

            for (int j = i + 1; j < tvs.length; j++) {
               BBacnetTimeValue tvj = (BBacnetTimeValue)this.get(tvs[j]);
               BBacnetTimeValue tvsmall = (BBacnetTimeValue)this.get(tvs[small]);
               if (tvj.isBefore(tvsmall)) {
                  small = j;
               }
            }

            Property temp = tvs[i];
            tvs[i] = tvs[small];
            tvs[small] = temp;
         }

         this.reorder(tvs, null);
      }
   }

   public void spy(SpyWriter out) throws Exception {
      super.spy(out);
      out.startProps();
      out.trTitle("BacnetSpecialEvent", 2);
      out.prop("virtual", BacnetVirtualUtil.isVirtual(this));
      out.endProps();
   }
}
