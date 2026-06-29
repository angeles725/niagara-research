package javax.baja.bacnet.datatypes;

import java.util.ArrayList;
import java.util.List;
import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.io.AsnInput;
import javax.baja.bacnet.io.AsnOutput;
import javax.baja.bacnet.virtual.BBacnetVirtualProperty;
import javax.baja.bacnet.virtual.BacnetVirtualUtil;
import javax.baja.category.BCategoryMask;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraActions;
import javax.baja.nre.annotations.NiagaraTopic;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.security.BPermissions;
import javax.baja.spy.SpyWriter;
import javax.baja.sys.Action;
import javax.baja.sys.BBoolean;
import javax.baja.sys.BComponent;
import javax.baja.sys.BDouble;
import javax.baja.sys.BDynamicEnum;
import javax.baja.sys.BSimple;
import javax.baja.sys.BString;
import javax.baja.sys.BTime;
import javax.baja.sys.BValue;
import javax.baja.sys.BasicContext;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.SlotCursor;
import javax.baja.sys.Sys;
import javax.baja.sys.Topic;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraActions({@NiagaraAction(
      name = "addTimeValue",
      parameterType = "BBacnetTimeValue",
      defaultValue = "new BBacnetTimeValue()"
   ), @NiagaraAction(
      name = "removeTimeValue",
      parameterType = "BString",
      defaultValue = "BString.make(\"\")"
   )})
@NiagaraTopic(
   name = "dailyScheduleChanged"
)
public class BBacnetDailySchedule extends BComponent implements BIBacnetDataType {
   public static final Action addTimeValue = newAction(0, new BBacnetTimeValue(), null);
   public static final Action removeTimeValue = newAction(0, BString.make(""), null);
   public static final Topic dailyScheduleChanged = newTopic(0, null);
   public static final Type TYPE = Sys.loadType(BBacnetDailySchedule.class);
   public static final int DAY_SCHEDULE_TAG = 0;

   public void addTimeValue(BBacnetTimeValue parameter) {
      this.invoke(addTimeValue, parameter, null);
   }

   public void removeTimeValue(BString parameter) {
      this.invoke(removeTimeValue, parameter, null);
   }

   public void fireDailyScheduleChanged(BValue event) {
      this.fire(dailyScheduleChanged, event, null);
   }

   public Type getType() {
      return TYPE;
   }

   public final void doAddTimeValue(BBacnetTimeValue tv) {
      this.add(null, tv);
   }

   public final void doRemoveTimeValue(BString tvName) {
      Property property = this.getProperty(tvName.getString());
      if (property != null) {
         this.remove(property, null);
      }
   }

   @Override
   public final void writeAsn(AsnOutput out) {
      out.writeOpeningTag(0);
      SlotCursor<Property> c = this.getProperties();

      while (c.next(BBacnetTimeValue.class)) {
         ((BBacnetTimeValue)c.get()).writeAsn(out);
      }

      out.writeClosingTag(0);
   }

   @Override
   public final void readAsn(AsnInput in) throws AsnException {
      List<BBacnetTimeValue> timeValues = new ArrayList<>();
      in.skipOpeningTag(0);

      for (int tag = in.peekTag(); !in.isClosingTag(0); tag = in.peekTag()) {
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
               throw new AsnException("Invalid tag: " + tag);
         }

         timeValues.add(new BBacnetTimeValue(time, value));
      }

      in.skipClosingTag(0);
      this.removeAll(noWrite);
      int length = timeValues.size();

      for (int i = 0; i < length; i++) {
         this.add("BacnetTimeValue" + (i + 1), (BValue)timeValues.get(i), noWrite);
      }
   }

   public String toString(Context cx) {
      if (cx != null && cx instanceof BasicContext) {
         return "BacnetDailySchedule{" + this.getPropertyInParent() + "}";
      } else {
         this.loadSlots();
         StringBuilder sb = new StringBuilder("{");
         SlotCursor<Property> sc = this.getProperties();

         while (sc.next()) {
            sb.append(sc.get()).append(',');
         }

         if (sb.length() == 1) {
            return "{}";
         } else {
            sb.setCharAt(sb.length() - 1, '}');
            return sb.toString();
         }
      }
   }

   public void started() {
      if (!BacnetVirtualUtil.isVirtual(this) && this.getParent() instanceof BBacnetArray) {
         ((BBacnetArray)this.getParent()).linkTo(this, dailyScheduleChanged, BBacnetArray.arrayPropertyChanged);
      }
   }

   public void added(Property p, Context cx) {
      if (this.isMounted()) {
         if (this.isRunning()) {
            if (cx != noWrite) {
               this.sort();
               this.getParent().asComponent().changed(this.getPropertyInParent(), cx);
               this.fireDailyScheduleChanged(null);
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
               this.fireDailyScheduleChanged(null);
            }
         }
      }
   }

   public void changed(Property p, Context cx) {
      super.changed(p, cx);
      if (this.isMounted()) {
         if (this.isRunning()) {
            if (cx != noWrite) {
               this.sort();
               this.getParent().asComponent().changed(this.getPropertyInParent(), cx);
               this.fireDailyScheduleChanged(null);
            }
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

   public final BSimple getValue(BTime at) {
      BBacnetTimeValue tv = null;
      BBacnetTimeValue tvlast = null;
      SlotCursor<Property> c = this.getProperties();

      while (c.next(BBacnetTimeValue.class)) {
         tv = (BBacnetTimeValue)c.get();
         if (tv.getTime().toBTime().isBefore(at) && (tvlast == null || tv.isAfter(tvlast))) {
            tvlast = tv;
         }
      }

      if (tvlast == null) {
         tvlast = tv;
      }

      return (BSimple)(tvlast == null ? BBacnetNull.DEFAULT : tvlast.getValue().getAny());
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
      Property[] tvs = this.getPropertiesArray();

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

   public void spy(SpyWriter out) throws Exception {
      super.spy(out);
      out.startProps();
      out.trTitle("BacnetDailySchedule", 2);
      out.prop("virtual", BacnetVirtualUtil.isVirtual(this));
      out.endProps();
   }
}
