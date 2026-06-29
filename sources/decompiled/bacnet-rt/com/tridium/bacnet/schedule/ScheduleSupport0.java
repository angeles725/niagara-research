package com.tridium.bacnet.schedule;

import com.tridium.bacnet.asn.AsnConst;
import com.tridium.bacnet.asn.AsnUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;
import javax.baja.bacnet.BBacnetDevice;
import javax.baja.bacnet.BBacnetNetwork;
import javax.baja.bacnet.datatypes.BBacnetDate;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.datatypes.BBacnetTime;
import javax.baja.bacnet.enums.BBacnetObjectType;
import javax.baja.bacnet.export.BBacnetCalendarDescriptor;
import javax.baja.bacnet.io.AsnDataTypeNotSupportedException;
import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.io.AsnInput;
import javax.baja.bacnet.io.AsnOutput;
import javax.baja.bacnet.io.DataTypeNotSupportedException;
import javax.baja.bacnet.io.DuplicateEntryException;
import javax.baja.bacnet.io.OutOfRangeException;
import javax.baja.log.Log;
import javax.baja.naming.BOrd;
import javax.baja.nre.util.Array;
import javax.baja.schedule.BAbstractSchedule;
import javax.baja.schedule.BBooleanSchedule;
import javax.baja.schedule.BCalendarSchedule;
import javax.baja.schedule.BCompositeSchedule;
import javax.baja.schedule.BCustomSchedule;
import javax.baja.schedule.BDailySchedule;
import javax.baja.schedule.BDateRangeSchedule;
import javax.baja.schedule.BDateSchedule;
import javax.baja.schedule.BDaySchedule;
import javax.baja.schedule.BEnumSchedule;
import javax.baja.schedule.BNumericSchedule;
import javax.baja.schedule.BScheduleReference;
import javax.baja.schedule.BStringSchedule;
import javax.baja.schedule.BTimeSchedule;
import javax.baja.schedule.BWeekAndDaySchedule;
import javax.baja.status.BStatusBoolean;
import javax.baja.status.BStatusEnum;
import javax.baja.status.BStatusNumeric;
import javax.baja.status.BStatusString;
import javax.baja.status.BStatusValue;
import javax.baja.sys.BAbsTime;
import javax.baja.sys.BDouble;
import javax.baja.sys.BDynamicEnum;
import javax.baja.sys.BEnumRange;
import javax.baja.sys.BFacets;
import javax.baja.sys.BInteger;
import javax.baja.sys.BMonth;
import javax.baja.sys.BObject;
import javax.baja.sys.BTime;
import javax.baja.sys.BValue;
import javax.baja.sys.Property;
import javax.baja.sys.SlotCursor;
import javax.baja.sys.Type;

public class ScheduleSupport0 implements AsnConst {
   static final BFacets SCHEDULE_CALENDAR_OBJECT_ID_FACETS = BFacets.makeEnum(
      BEnumRange.make(new int[]{17, 6}, new String[]{BBacnetObjectType.tag(17), BBacnetObjectType.tag(6)})
   );
   private static final ScheduleSupport0.BacnetScheduleComparator scheduleOrder = new ScheduleSupport0.BacnetScheduleComparator();
   public static final String BACNET_IDX = "bacnetIdx";
   private static final BDateSchedule ALWAYS_EFFECTIVE_DATE_SCHEDULE = new BDateSchedule();
   private static final boolean CALCULATE_WEEKDAY = true;
   protected BBacnetScheduleDeviceExt devext = null;
   private static final Log logger = Log.getLog("bacnet.schedule");

   public ScheduleSupport0() {
   }

   public ScheduleSupport0(BBacnetScheduleDeviceExt ext) {
      this.devext = ext;
   }

   public static ScheduleSupport0 makeForProtocolRevision(int protocolRevision, ScheduleSupport0 current) {
      if (protocolRevision >= 16) {
         if (!current.getClass().equals(ScheduleSupport16.class)) {
            return new ScheduleSupport16();
         }
      } else if (protocolRevision >= 4) {
         if (!current.getClass().equals(ScheduleSupport4.class)) {
            return new ScheduleSupport4();
         }
      } else if (!current.getClass().equals(ScheduleSupport0.class)) {
         return new ScheduleSupport0();
      }

      return current;
   }

   public String getVersion() {
      return "ScheduleSupport 1.0";
   }

   public BAbstractSchedule decodeCalendarEntry(AsnInput in) throws AsnException {
      int tag = in.peekTag();
      if (in.isValueTag(0)) {
         return this.decodeDate(0, in);
      } else if (in.isOpeningTag(1)) {
         return this.decodeDateRange(1, in);
      } else if (in.isValueTag(2)) {
         return this.decodeWeekAndDay(2, in);
      } else {
         throw new AsnException("Invalid tag: " + tag);
      }
   }

   public BDaySchedule decodeDailySchedule(BStatusValue defaultValue, AsnInput in, int asnType) throws AsnException {
      ScheduleSupport0.TimeValue[] tvs = this.decodeTimeValues(0, in, defaultValue, asnType);
      return this.makeDay(tvs, this.bajaToJavaNullOk(defaultValue));
   }

   public BDateSchedule decodeDate(AsnInput in) throws AsnException {
      return this.decodeDate(in.readDate());
   }

   public BDateSchedule decodeDate(int contextTag, AsnInput in) throws AsnException {
      return this.decodeDate(in.readDate(contextTag));
   }

   public BDateSchedule decodeDate(BBacnetDate date) {
      BDateSchedule schedule = new BDateSchedule();
      schedule.setYear(date.getYear());
      schedule.setMonth(date.isMonthUnspecified() ? date.getMonth() : date.getMonth() - 1);
      schedule.setDay(bacnetDayOfMonthToBajaScheduleDay(date.getDayOfMonth()));
      if (!date.isDayOfWeekUnspecified()) {
         schedule.setWeekday(date.getBWeekday().getOrdinal());
      }

      return schedule;
   }

   public BCalendarSchedule decodeDateList(AsnInput in) throws AsnException {
      BCalendarSchedule ret = new BCalendarSchedule();

      while (in.peekTag() != -1) {
         ret.add(this.decodeCalendarEntry(in));
      }

      return ret;
   }

   public BDateRangeSchedule decodeDateRange(AsnInput in) throws AsnException {
      BDateRangeSchedule ret = new BDateRangeSchedule();
      ret.setStart(this.decodeDate(in));
      ret.setEnd(this.decodeDate(in));
      checkForSpecialValuesInDateRange(ret);
      return ret;
   }

   public BDateRangeSchedule decodeDateRange(int contextTag, AsnInput in) throws AsnException {
      BDateRangeSchedule ret = new BDateRangeSchedule();
      in.skipTag();
      ret.setStart(this.decodeDate(in));
      ret.setEnd(this.decodeDate(in));
      in.skipTag();
      checkForSpecialValuesInDateRange(ret);
      return ret;
   }

   private static void checkForSpecialValuesInDateRange(BDateRangeSchedule dateRange) throws OutOfRangeException {
      BDateSchedule start = dateRange.getStart();
      BDateSchedule end = dateRange.getEnd();
      if ((!allValuesInDateScheduleAreUnspecified(start) || !noneInDateScheduleAreSpecialValues(end))
         && (!noneInDateScheduleAreSpecialValues(start) || !allValuesInDateScheduleAreUnspecified(end))
         && (!allValuesInDateScheduleAreUnspecified(start) || !allValuesInDateScheduleAreUnspecified(end))) {
         if (start.getYear() == -1
            || end.getYear() == -1
            || monthHasSpecialValue(start.getMonth())
            || monthHasSpecialValue(end.getMonth())
            || dayHasSpecialValue(start.getDay())
            || dayHasSpecialValue(end.getDay())
            || start.getWeekday() == -1
            || end.getWeekday() == -1) {
            throw new OutOfRangeException("Date contains Special Values.");
         }
      }
   }

   private static boolean monthHasSpecialValue(int month) {
      return month == -1 || month == 12 || month == 13;
   }

   private static boolean dayHasSpecialValue(int day) {
      return day == -1 || day == 32 || day == 33 || day == 34 || day == 35;
   }

   private static boolean allValuesInDateScheduleAreUnspecified(BDateSchedule dateSchedule) {
      return dateSchedule.getYear() == -1 && dateSchedule.getMonth() == -1 && dateSchedule.getDay() == -1 && dateSchedule.getWeekday() == -1;
   }

   private static boolean noneInDateScheduleAreSpecialValues(BDateSchedule dateSchedule) {
      return dateSchedule.getYear() != -1
         && !monthHasSpecialValue(dateSchedule.getMonth())
         && !dayHasSpecialValue(dateSchedule.getDay())
         && dateSchedule.getWeekday() != -1;
   }

   private static boolean allValuesInTimeAreUnspecified(BBacnetTime t) {
      return t.isHourUnspecified() && t.isMinuteUnspecified() && t.isSecondUnspecified() && t.isHundredthUnspecified();
   }

   public BCompositeSchedule decodeExceptionSchedule(BStatusValue defaultValue, AsnInput in, BBacnetObjectIdentifier deviceId, int asnType) throws AsnException {
      BCompositeSchedule ret = new BCompositeSchedule();
      Array<BDailySchedule> arr = new Array(BDailySchedule.class);
      int tag = in.peekTag();

      for (int ndx = 0; tag != -1; tag = in.peekTag()) {
         BDailySchedule mdsch = this.decodeSpecialEvent(defaultValue, in, deviceId, asnType, ndx++);
         int eventPriority = ((BInteger)mdsch.get("priority")).getInt();
         boolean inserted = false;

         for (int i = 0; i < arr.size(); i++) {
            BDailySchedule existing = (BDailySchedule)arr.get(i);
            Property p = existing.getProperty("priority");
            int existingPriority = 1;
            if (p != null) {
               existingPriority = existing.getInt(p);
            }

            if (eventPriority < existingPriority) {
               arr.add(i, mdsch);
               inserted = true;
               break;
            }
         }

         if (!inserted) {
            arr.add(mdsch);
         }
      }

      Iterator<BDailySchedule> it = arr.iterator();

      while (it.hasNext()) {
         ret.add(null, (BValue)it.next());
      }

      return ret;
   }

   public BAbstractSchedule decodePeriod(AsnInput in, BBacnetObjectIdentifier deviceId) throws AsnException {
      int tag = in.peekTag();
      if (in.isOpeningTag(0)) {
         in.skipTag();
         BAbstractSchedule ret = this.decodeCalendarEntry(in);
         in.skipTag();
         return ret;
      } else if (in.isValueTag(1)) {
         BScheduleReference ret = new BScheduleReference();
         BBacnetObjectIdentifier objectId = in.readObjectIdentifier(1);

         try {
            BBacnetDevice device = BBacnetNetwork.bacnet().doLookupDeviceById(deviceId);
            if (device == null) {
               if (BBacnetNetwork.localDevice().getObjectId().equals(deviceId)) {
                  BBacnetCalendarDescriptor calXport = (BBacnetCalendarDescriptor)BBacnetNetwork.localDevice().lookupBacnetObject(objectId);
                  if (calXport != null) {
                     ret.setRef(calXport.getObject().asComponent().getSlotPathOrd());
                  }
               } else {
                  log().message("Unable to find BACnet device for deviceId " + deviceId + "!!! Calendar Reference could fail...");
                  ret.setRef((BOrd)BOrd.NULL.decodeFromString("bac:" + deviceId.toString() + ";" + objectId.toString() + "{" + "schedule" + "}"));
               }
            } else {
               BAbstractSchedule cal = (BAbstractSchedule)device.lookupBacnetObject(objectId, -1, -1, "schedule");
               if (cal != null) {
                  ret.setRef(cal.getSlotPathOrd());
               } else {
                  log().message("Unable to find BACnet calendar " + objectId + " in device " + deviceId + "!!! Calendar Reference could fail...");
                  ret.setRef((BOrd)BOrd.NULL.decodeFromString("bac:" + deviceId.toString() + ";" + objectId.toString() + "{" + "schedule" + "}"));
               }
            }

            return ret;
         } catch (Exception var8) {
            log().error("Exception decoding schedule reference:" + objectId, var8);
            throw new RuntimeException(var8.getMessage());
         }
      } else {
         throw new AsnException("Invalid tag: " + tag);
      }
   }

   public BStatusValue decodeScheduleDefault(BStatusValue defaultValue, AsnInput in, int asnType) throws AsnException {
      Type t = defaultValue.getType();
      if (t.is(BStatusBoolean.TYPE)) {
         return this.decodeScheduleDefaultBoolean((BStatusBoolean)defaultValue, in);
      } else if (t.is(BStatusNumeric.TYPE)) {
         return this.decodeScheduleDefaultNumeric((BStatusNumeric)defaultValue, in);
      } else if (t.is(BStatusEnum.TYPE)) {
         return this.decodeScheduleDefaultEnum((BStatusEnum)defaultValue, in, asnType);
      } else if (t.is(BStatusString.TYPE)) {
         return this.decodeScheduleDefaultString((BStatusString)defaultValue, in);
      } else {
         throw new IllegalArgumentException(defaultValue.getClass().toString());
      }
   }

   public BDailySchedule decodeSpecialEvent(BStatusValue defaultValue, AsnInput in, BBacnetObjectIdentifier deviceId, int asnType, int ndx) throws AsnException {
      BDailySchedule ret = new BDailySchedule(this.decodePeriod(in, deviceId));
      ScheduleSupport0.TimeValue[] tvs = this.decodeTimeValues(2, in, defaultValue, asnType);
      ret.setDay(this.makeDay(tvs, this.bajaToJavaNullOk(defaultValue)));
      ret.add("priority", BInteger.make(in.readUnsignedInt(3)));
      ret.add("bacnetIdx", BInteger.make(ndx));
      return ret;
   }

   public BWeekAndDaySchedule decodeWeekAndDay(int contextTag, AsnInput in) throws AsnException {
      BWeekAndDaySchedule ret = new BWeekAndDaySchedule();
      byte[] wnd = in.readOctetString(contextTag);
      int i = wnd[0];
      ret.setMonth(i > 0 && i <= 14 ? i - 1 : -1);
      int var6 = wnd[1];
      ret.setWeek(var6);
      var6 = wnd[2];
      switch (var6) {
         case 1:
         case 2:
         case 3:
         case 4:
         case 5:
         case 6:
            ret.setWeekday(var6);
            break;
         case 7:
            ret.setWeekday(0);
            break;
         default:
            ret.setWeekday(-1);
      }

      return ret;
   }

   public void encodeCalendarEntry(BAbstractSchedule sch, AsnOutput out) {
      if (sch instanceof BDateSchedule) {
         this.encodeDate((BDateSchedule)sch, 0, out);
      } else if (sch instanceof BDateRangeSchedule) {
         this.encodeDateRange((BDateRangeSchedule)sch, 1, out);
      } else if (sch instanceof BWeekAndDaySchedule) {
         this.encodeWeekAndDay((BWeekAndDaySchedule)sch, 2, out);
      } else {
         if (!(sch instanceof BCustomSchedule)) {
            throw new IllegalArgumentException("Invalid calendar entry: " + sch.getClass());
         }

         BAbsTime start = BAbsTime.make();
         BAbsTime finish = start.nextYear();
         int numEvents = 0;

         for (BAbsTime next = sch.nextEvent(start); numEvents < 10 && next.isBefore(finish); next = sch.nextEvent(next)) {
            if (sch.isEffective(next)) {
               this.encodeDate(next, 0, out);
               numEvents++;
            }
         }
      }
   }

   public void encodeDailySchedule(BDaySchedule dsch, BStatusValue defaultValue, AsnOutput out, int asnType) {
      ScheduleSupport0.TimeValue[] tvs = this.makeDay(dsch, this.bajaToJava(defaultValue));
      this.encodeTimeValues(tvs, 0, out, asnType);
   }

   public void encodeDate(BDateSchedule dsch, AsnOutput out) {
      this.encodeDate(dsch, out, false);
   }

   public void encodeDate(BDateSchedule dsch, AsnOutput out, boolean calculateWeekDay) {
      int year = dsch.getYear();
      int month = dsch.getMonth();
      int day = bajaScheduleDayToBacnetDayOfMonth(dsch.getDay());
      int weekDay = dsch.getWeekday();
      if (calculateWeekDay && month >= 0 && year >= 0 && day >= 0) {
         weekDay = BAbsTime.getWeekday(year, BMonth.make(month), day).getOrdinal();
      }

      out.writeDate(year < 0 ? 255 : year - 1900, month < 0 ? month : month + 1, day, weekDay == 0 ? 7 : weekDay);
   }

   public void encodeDate(BDateSchedule dsch, int contextTag, AsnOutput out) {
      int y = dsch.getYear();
      int m = dsch.getMonth();
      int d = bajaScheduleDayToBacnetDayOfMonth(dsch.getDay());
      int weekDay = dsch.getWeekday();
      out.writeDate(contextTag, y < 0 ? 255 : y - 1900, m < 0 ? m : m + 1, d, weekDay == 0 ? 7 : weekDay);
   }

   public void encodeDate(BAbsTime date, int contextTag, AsnOutput out) {
      int y = date.getYear();
      int m = date.getMonth().getOrdinal();
      int d = date.getDay();
      int w = date.getWeekday().getOrdinal();
      out.writeDate(contextTag, y - 1900, m + 1, d, w == 0 ? 7 : w);
   }

   public void encodeDateList(BCalendarSchedule cal, AsnOutput out) {
      SlotCursor<Property> c = cal.getProperties();

      while (c.next(BAbstractSchedule.class)) {
         this.encodeCalendarEntry((BAbstractSchedule)c.get(), out);
      }
   }

   public void encodeDateRange(BDateRangeSchedule drsch, AsnOutput out) {
      if (drsch.getAlwaysEffective()) {
         this.encodeDate(ALWAYS_EFFECTIVE_DATE_SCHEDULE, out);
         this.encodeDate(ALWAYS_EFFECTIVE_DATE_SCHEDULE, out);
      } else {
         this.encodeDate(drsch.getStart(), out, true);
         this.encodeDate(drsch.getEnd(), out, true);
      }
   }

   public void encodeDateRange(BDateRangeSchedule drsch, int contextTag, AsnOutput out) {
      out.writeOpeningTag(contextTag);
      if (drsch.getAlwaysEffective()) {
         this.encodeDate(ALWAYS_EFFECTIVE_DATE_SCHEDULE, out);
         this.encodeDate(ALWAYS_EFFECTIVE_DATE_SCHEDULE, out);
      } else {
         this.encodeDate(drsch.getStart(), out, true);
         this.encodeDate(drsch.getEnd(), out, true);
      }

      out.writeClosingTag(contextTag);
   }

   public void encodeEntry(BAbstractSchedule sch, AsnOutput out, BBacnetObjectIdentifier deviceId) {
      if (sch instanceof BScheduleReference) {
         BBacnetObjectIdentifier defObjectId = BBacnetObjectIdentifier.make(6);
         BOrd ord = ((BScheduleReference)sch).getRef();
         if (BBacnetNetwork.localDevice().getObjectId().equals(deviceId)) {
            BBacnetObjectIdentifier objectId = BBacnetNetwork.localDevice().lookupBacnetObjectId(ord);
            out.writeObjectIdentifier(1, objectId != null ? objectId : defObjectId);
         } else {
            BObject o = ord.get(sch.getComponentSpace());
            if (o == null) {
               throw new IllegalArgumentException("Invalid calendar reference: ref=" + ord + "; unresolved!");
            }

            if (!(o instanceof BCalendarSchedule)) {
               throw new IllegalArgumentException("Invalid calendar reference: ref=" + ord + "; o=" + o + " [" + o.getType() + "]");
            }

            BCalendarSchedule cal = (BCalendarSchedule)o;
            BBacnetScheduleImportExt importExt = BBacnetScheduleDeviceExt.getBacnetImportExt(cal);
            if (importExt != null) {
               out.writeObjectIdentifier(1, importExt.getObjectId());
            } else {
               if (this.devext == null) {
                  throw new IllegalArgumentException("ScheduleSupport device extension reference not set!");
               }

               BBacnetScheduleExport export = this.devext.getBacnetExportExt(cal);
               if (export == null) {
                  throw new IllegalArgumentException("No schedule import or export is available for calendar:" + ord);
               }

               out.writeObjectIdentifier(1, export.getObjectId());
            }
         }
      } else {
         out.writeOpeningTag(0);
         this.encodeCalendarEntry(sch, out);
         out.writeClosingTag(0);
      }
   }

   public void encodeExceptionScheduleWithIdx(BCompositeSchedule csch, BStatusValue defaultValue, AsnOutput out, int asnType, BBacnetObjectIdentifier deviceId) {
      List<BDailySchedule> specialEvents = sortSpecialEvents(csch);

      for (int i = 0; i < specialEvents.size(); i++) {
         this.encodeSpecialEvent(specialEvents.get(i), defaultValue, out, asnType, deviceId);
      }
   }

   public void encodeExceptionSchedule(BCompositeSchedule csch, BStatusValue defaultValue, AsnOutput out, int asnType, BBacnetObjectIdentifier deviceId) {
      SlotCursor<Property> c = csch.getProperties();

      while (c.next(BDailySchedule.class)) {
         BDailySchedule mdsch = (BDailySchedule)c.get();
         this.encodeSpecialEvent(mdsch, defaultValue, out, asnType, deviceId);
      }
   }

   public void encodeScheduleDefault(BStatusValue defaultOutput, AsnOutput out, int asnType) {
      Type t = defaultOutput.getType();
      if (t.is(BStatusBoolean.TYPE)) {
         this.encodeScheduleDefault((BStatusBoolean)defaultOutput, out, asnType);
      } else if (t.is(BStatusNumeric.TYPE)) {
         this.encodeScheduleDefault((BStatusNumeric)defaultOutput, out, asnType);
      } else if (t.is(BStatusEnum.TYPE)) {
         this.encodeScheduleDefault((BStatusEnum)defaultOutput, out, asnType);
      } else if (t.is(BStatusString.TYPE)) {
         this.encodeScheduleDefault((BStatusString)defaultOutput, out, asnType);
      } else {
         throw new IllegalArgumentException(defaultOutput.getClass().toString());
      }
   }

   public void encodeSpecialEvent(BDailySchedule mdsch, BStatusValue defaultValue, AsnOutput out, int asnType, BBacnetObjectIdentifier deviceId) {
      this.encodeEntry(mdsch.getDays(), out, deviceId);
      ScheduleSupport0.TimeValue[] tvs = this.makeDay(mdsch.getDay(), this.bajaToJava(defaultValue));
      this.encodeTimeValues(tvs, 2, out, asnType);
      Property p = mdsch.getProperty("priority");
      out.writeUnsignedInteger(3, p != null ? mdsch.getInt(p) : 16L);
   }

   public void encodeSpecialEvent(int index, BCompositeSchedule csch, BStatusValue defaultValue, AsnOutput out, int asnType, BBacnetObjectIdentifier deviceId) {
      if (index <= 0) {
         throw new ArrayIndexOutOfBoundsException();
      } else {
         SlotCursor<Property> c = csch.getProperties();

         while (index-- > 0) {
            if (!c.next(BDailySchedule.class)) {
               throw new ArrayIndexOutOfBoundsException();
            }
         }

         this.encodeSpecialEvent((BDailySchedule)c.get(), defaultValue, out, asnType, deviceId);
      }
   }

   public void encodeWeekAndDay(BWeekAndDaySchedule wndsch, int contextTag, AsnOutput out) {
      byte[] octetString = new byte[3];
      int m = wndsch.getMonth();
      int w = wndsch.getWeek();
      int d = wndsch.getWeekday();
      if (m == -1) {
         octetString[0] = -1;
      } else {
         octetString[0] = (byte)(m + 1);
      }

      octetString[1] = (byte)w;
      switch (d) {
         case -1:
            octetString[2] = -1;
            break;
         case 0:
            octetString[2] = 7;
            break;
         default:
            octetString[2] = (byte)d;
      }

      out.writeOctetString(contextTag, octetString);
   }

   protected ScheduleSupport0.TimeValue[] makeDay(BDaySchedule dsch, Object defaultValue) {
      if (log().isTraceOn()) {
         log().trace("makeDay(1.0): daySch=");
         BBacnetScheduleDeviceExt.dump(dsch);
      }

      TreeSet<ScheduleSupport0.TimeValue> set = new TreeSet<>(new Comparator<Object>() {
         @Override
         public int compare(Object o1, Object o2) {
            return ((ScheduleSupport0.TimeValue)o1).compareTo(o2);
         }
      });
      ScheduleSupport0.TimeValue lastFinish = null;
      BTimeSchedule[] times = dsch.getTimesInOrder();
      if (times.length > 0) {
         BTimeSchedule t = times[0];
         ScheduleSupport0.TimeValue v = new ScheduleSupport0.TimeValue(t.getStart(), this.bajaToJava(t.getEffectiveValue()));
         set.add(v);
         if (v.millis() > 0) {
            v = new ScheduleSupport0.TimeValue(BTime.make(0, 0, 0, 0), defaultValue);
            set.add(v);
         }

         lastFinish = new ScheduleSupport0.TimeValue(t.getFinish(), defaultValue);
      } else {
         set.add(new ScheduleSupport0.TimeValue(BTime.make(0, 0, 0, 0), defaultValue));
      }

      for (int i = 1; i < times.length; i++) {
         BTimeSchedule t = times[i];
         ScheduleSupport0.TimeValue v = new ScheduleSupport0.TimeValue(t.getStart(), this.bajaToJava(t.getEffectiveValue()));
         set.add(v);
         if (lastFinish.millis() != v.millis()) {
            set.add(lastFinish);
         }

         lastFinish = new ScheduleSupport0.TimeValue(t.getFinish(), defaultValue);
      }

      if (lastFinish != null && lastFinish.millis() > 0) {
         set.add(lastFinish);
      }

      ScheduleSupport0.TimeValue[] ret = new ScheduleSupport0.TimeValue[set.size()];
      Iterator<ScheduleSupport0.TimeValue> it = set.iterator();
      int i = 0;

      while (it.hasNext()) {
         ret[i++] = it.next();
      }

      if (log().isTraceOn()) {
         log().trace("TimeValue[] return array:");

         for (int j = 0; j < ret.length; j++) {
            log().trace("ret[" + j + "]:" + ret[j]);
         }
      }

      return ret;
   }

   protected BDaySchedule makeDay(ScheduleSupport0.TimeValue[] tvs, Object defaultValue) throws DuplicateEntryException {
      if (log().isTraceOn()) {
         log().trace("makeDay(1.0): tvs=");

         for (int i = 0; i < tvs.length; i++) {
            log().trace("tvs[" + i + "]:" + tvs[i]);
         }
      }

      BDaySchedule ret = new BDaySchedule();

      for (int i = 0; i < tvs.length; i++) {
         BTimeSchedule tsch = new BTimeSchedule();
         tsch.setStart(this.makeTime(tvs[i], true));
         if (i + 1 >= tvs.length) {
            tsch.setFinish(BTime.make(0, 0, 0, 0));
         } else {
            tsch.setFinish(this.makeTime(tvs[i + 1], true));
         }

         tsch.setEffectiveValue(this.javaToBaja(tvs[i].value, defaultValue));
         ret.add(tsch);
      }

      if (log().isTraceOn()) {
         log().trace("ret daySch=");
         BBacnetScheduleDeviceExt.dump(ret);
      }

      return ret;
   }

   public void setDeviceExt(BBacnetScheduleDeviceExt ext) {
      this.devext = ext;
   }

   public ScheduleType getScheduleType(int contextTag, AsnInput in) throws AsnException {
      int tag = in.peekTag();
      if (!in.isOpeningTag(contextTag)) {
         throw new AsnException("Invalid tag: " + tag);
      } else {
         in.skipTag();

         for (tag = in.peekTag(); !in.isClosingTag(contextTag); tag = in.peekTag()) {
            if (tag == -1) {
               throw new AsnException("Invalid tag: " + tag);
            }

            in.readTime();
            tag = in.peekApplicationTag();
            switch (tag) {
               case 0:
               default:
               case 1:
                  return new ScheduleType(BBooleanSchedule.TYPE.toString(), AsnUtil.getAsnTypeName(tag));
               case 2:
                  return new ScheduleType(BEnumSchedule.TYPE.toString(), AsnUtil.getAsnTypeName(tag));
               case 3:
                  return new ScheduleType(BEnumSchedule.TYPE.toString(), AsnUtil.getAsnTypeName(tag));
               case 4:
                  return new ScheduleType(BNumericSchedule.TYPE.toString(), AsnUtil.getAsnTypeName(tag));
               case 5:
                  return new ScheduleType(BNumericSchedule.TYPE.toString(), AsnUtil.getAsnTypeName(tag));
               case 6:
                  return new ScheduleType(BStringSchedule.TYPE.toString(), AsnUtil.getAsnTypeName(tag));
               case 7:
                  return new ScheduleType(BStringSchedule.TYPE.toString(), AsnUtil.getAsnTypeName(tag));
               case 8:
                  return new ScheduleType(BStringSchedule.TYPE.toString(), AsnUtil.getAsnTypeName(tag));
               case 9:
                  return new ScheduleType(BEnumSchedule.TYPE.toString(), AsnUtil.getAsnTypeName(tag));
               case 10:
                  return new ScheduleType(BStringSchedule.TYPE.toString(), AsnUtil.getAsnTypeName(tag));
               case 11:
                  return new ScheduleType(BStringSchedule.TYPE.toString(), AsnUtil.getAsnTypeName(tag));
               case 12:
                  return new ScheduleType(BStringSchedule.TYPE.toString(), AsnUtil.getAsnTypeName(tag));
            }
         }

         tag = in.peekTag();
         return null;
      }
   }

   protected final BTime makeTime(ScheduleSupport0.TimeValue t, boolean zero) {
      return BTime.make(
         t.hour >= 0 && t.hour <= 23 ? t.hour : (zero ? 0 : 23),
         t.minute >= 0 && t.minute <= 59 ? t.minute : (zero ? 0 : 59),
         t.second >= 0 && t.second <= 59 ? t.second : (zero ? 0 : 59),
         t.millis >= 0 && t.millis <= 999 ? t.millis : (zero ? 0 : 999)
      );
   }

   protected Object bajaToJava(BStatusValue s) {
      if (s == null || s.getStatus().isNull()) {
         return null;
      } else if (s instanceof BStatusBoolean) {
         return ((BStatusBoolean)s).getValue() ? Boolean.TRUE : Boolean.FALSE;
      } else if (s instanceof BStatusNumeric) {
         return ((BStatusNumeric)s).getValue();
      } else if (s instanceof BStatusEnum) {
         return ((BStatusEnum)s).getValue().getOrdinal();
      } else if (s instanceof BStatusString) {
         return ((BStatusString)s).getValue();
      } else {
         throw new IllegalArgumentException("Invalid BStatusValue: " + s.getClass());
      }
   }

   protected Object bajaToJavaNullOk(BStatusValue sv) {
      if (sv == null) {
         return null;
      } else if (sv instanceof BStatusBoolean) {
         return ((BStatusBoolean)sv).getValue() ? Boolean.TRUE : Boolean.FALSE;
      } else if (sv instanceof BStatusNumeric) {
         return ((BStatusNumeric)sv).getValue();
      } else if (sv instanceof BStatusEnum) {
         return ((BStatusEnum)sv).getValue().getOrdinal();
      } else if (sv instanceof BStatusString) {
         return ((BStatusString)sv).getValue();
      } else {
         throw new IllegalArgumentException("Invalid BStatusValue:" + sv + " {" + sv.getType() + "}");
      }
   }

   protected BStatusValue javaToBaja(Object src, Object def) {
      BStatusValue ret = null;
      Object o = src;
      if (src == null) {
         o = def;
      }

      if (o instanceof Boolean) {
         ret = new BStatusBoolean((Boolean)o);
      } else if (o instanceof Double) {
         ret = new BStatusNumeric((Double)o);
      } else if (o instanceof Integer) {
         ret = new BStatusEnum(BDynamicEnum.make((Integer)o));
      } else {
         if (!(o instanceof String)) {
            throw new IllegalStateException("Unexpected output type: " + o.getClass());
         }

         ret = new BStatusString((String)o);
      }

      if (src == null) {
         ret.setStatusNull(true);
      }

      return ret;
   }

   protected BStatusValue javaToBaja(Object src, BStatusValue def) {
      BStatusValue ret = null;
      if (src == null) {
         return def;
      } else {
         if (src instanceof Boolean) {
            ret = new BStatusBoolean((Boolean)src);
         } else if (src instanceof Double) {
            ret = new BStatusNumeric((Double)src);
         } else if (src instanceof Integer) {
            ret = new BStatusEnum(BDynamicEnum.make((Integer)src));
         } else {
            if (!(src instanceof String)) {
               throw new IllegalStateException("Unexpected output type: " + src.getClass());
            }

            ret = new BStatusString((String)src);
         }

         if (src == null) {
            ret.setStatusNull(true);
         }

         return ret;
      }
   }

   private static int bajaScheduleDayToBacnetDayOfMonth(int bajaDay) {
      if (bajaDay >= 1 && bajaDay <= 31) {
         return bajaDay;
      } else {
         switch (bajaDay) {
            case -1:
               return -1;
            case 32:
               return 32;
            case 33:
               throw new IllegalArgumentException("Baja special value 'last 7 days' cannot be converted to a BACnet day-of-month value.");
            case 34:
               return 33;
            case 35:
               return 34;
            default:
               throw new IllegalArgumentException("Invalid baja day-of-month value: " + bajaDay);
         }
      }
   }

   private static int bacnetDayOfMonthToBajaScheduleDay(int bacnetDay) {
      if (bacnetDay >= 1 && bacnetDay <= 31) {
         return bacnetDay;
      } else {
         switch (bacnetDay) {
            case -1:
               return -1;
            case 32:
               return 32;
            case 33:
               return 34;
            case 34:
               return 35;
            default:
               throw new IllegalArgumentException("Invalid BACnet day value: " + bacnetDay);
         }
      }
   }

   public static List<BDailySchedule> sortSpecialEvents(BCompositeSchedule csch) {
      List<BDailySchedule> specialEvents = new ArrayList<>();
      SlotCursor<Property> c = csch.getProperties();

      while (c.next(BDailySchedule.class)) {
         BDailySchedule mdsch = (BDailySchedule)c.get();
         specialEvents.add((BDailySchedule)mdsch.newCopy());
      }

      if (specialEvents.size() > 0) {
         Collections.sort(specialEvents, scheduleOrder);
      }

      return specialEvents;
   }

   private BStatusValue decodeScheduleDefaultBoolean(BStatusBoolean defaultValue, AsnInput in) throws AsnException {
      int tag = in.peekApplicationTag();
      switch (tag) {
         case 0:
            return defaultValue;
         case 1:
            return new BStatusBoolean(in.readBoolean());
         default:
            throw new DataTypeNotSupportedException("Invalid tag: " + tag);
      }
   }

   private BStatusValue decodeScheduleDefaultNumeric(BStatusNumeric defaultValue, AsnInput in) throws AsnException {
      int tag = in.peekApplicationTag();
      switch (tag) {
         case 0:
            return defaultValue;
         case 4:
            return new BStatusNumeric(in.readReal());
         default:
            throw new DataTypeNotSupportedException("Invalid tag: " + tag);
      }
   }

   private BStatusValue decodeScheduleDefaultEnum(BStatusEnum defaultValue, AsnInput in, int asnType) throws AsnException {
      BEnumRange r = defaultValue.getValue().getRange();
      int tag = in.peekApplicationTag();
      switch (tag) {
         case 0:
            return defaultValue;
         case 1:
         case 4:
         case 5:
         case 6:
         case 7:
         case 8:
         default:
            throw new DataTypeNotSupportedException("Invalid tag: " + tag);
         case 2:
            if (asnType >= 0 && asnType != tag) {
               throw new AsnException("Invalid tag: " + tag);
            }

            return new BStatusEnum(r.get(in.readUnsignedInt()));
         case 3:
            if (asnType >= 0 && asnType != tag) {
               throw new AsnException("Invalid tag: " + tag);
            }

            return new BStatusEnum(r.get(in.readSignedInteger()));
         case 9:
            if (asnType >= 0 && asnType != tag) {
               throw new AsnException("Invalid tag: " + tag);
            } else {
               return new BStatusEnum(r.get(in.readEnumerated()));
            }
      }
   }

   private BStatusValue decodeScheduleDefaultString(BStatusString defaultValue, AsnInput in) throws AsnException {
      int tag = in.peekApplicationTag();
      switch (tag) {
         case 0:
            return defaultValue;
         case 7:
            return new BStatusString(in.readCharacterString());
         default:
            throw new DataTypeNotSupportedException("Invalid tag: " + tag);
      }
   }

   private ScheduleSupport0.TimeValue decodeTimeValue(AsnInput in, BStatusValue defaultValue, int asnType) throws AsnException {
      Type t = defaultValue.getType();
      if (t.is(BStatusBoolean.TYPE)) {
         return this.decodeTimeValueBoolean(in, (BStatusBoolean)defaultValue, asnType);
      } else if (t.is(BStatusNumeric.TYPE)) {
         return this.decodeTimeValueNumeric(in, (BStatusNumeric)defaultValue);
      } else if (t.is(BStatusEnum.TYPE)) {
         return this.decodeTimeValueEnum(in, (BStatusEnum)defaultValue, asnType);
      } else if (t.is(BStatusString.TYPE)) {
         return this.decodeTimeValueString(in, (BStatusString)defaultValue);
      } else {
         throw new IllegalArgumentException(defaultValue.getClass().toString());
      }
   }

   private void checkForUnspecified(BBacnetTime t) throws AsnException {
      if ((t.isHourUnspecified() || t.isMinuteUnspecified() || t.isSecondUnspecified() || t.isHundredthUnspecified()) && !allValuesInTimeAreUnspecified(t)) {
         throw new OutOfRangeException("Time contains Special Values.");
      }
   }

   private ScheduleSupport0.TimeValue decodeTimeValueBoolean(AsnInput in, BStatusBoolean defaultValue, int expAsnType) throws AsnException {
      BBacnetTime t = in.readTime();
      int tag = in.peekApplicationTag();
      boolean b = false;
      this.checkForUnspecified(t);
      switch (tag) {
         case 0:
            in.readNull();
            return new ScheduleSupport0.TimeValue(t, null);
         case 1:
            if (expAsnType != -1 && expAsnType != 1) {
               throw new DataTypeNotSupportedException("Invalid tag: " + tag);
            }

            b = in.readBoolean();
            break;
         case 9:
            if (expAsnType != -1 && expAsnType != 9) {
               throw new DataTypeNotSupportedException("Invalid tag: " + tag);
            }

            int value = in.readEnumerated();
            b = value != 0;
            if (value > 1) {
               throw new OutOfRangeException("Invalid BacnetBinaryPv value" + value);
            }
            break;
         default:
            throw new AsnDataTypeNotSupportedException(tag, "Invalid tag: " + tag);
      }

      return new ScheduleSupport0.TimeValue(t, b ? Boolean.TRUE : Boolean.FALSE);
   }

   private ScheduleSupport0.TimeValue decodeTimeValueNumeric(AsnInput in, BStatusNumeric defaultValue) throws AsnException {
      BBacnetTime t = in.readTime();
      this.checkForUnspecified(t);
      int tag = in.peekApplicationTag();
      double d = 0.0;
      switch (tag) {
         case 0:
            in.readNull();
            return new ScheduleSupport0.TimeValue(t, null);
         case 4:
            d = in.readReal();
            return new ScheduleSupport0.TimeValue(t, d);
         default:
            throw new AsnDataTypeNotSupportedException(tag, "Invalid tag: " + tag);
      }
   }

   private ScheduleSupport0.TimeValue decodeTimeValueEnum(AsnInput in, BStatusEnum defaultValue, int asnType) throws AsnException {
      BBacnetTime t = in.readTime();
      this.checkForUnspecified(t);
      int tag = in.peekApplicationTag();
      int i = 0;
      switch (tag) {
         case 0:
            in.readNull();
            return new ScheduleSupport0.TimeValue(t, null);
         case 1:
         case 4:
         case 5:
         case 6:
         case 7:
         case 8:
         default:
            throw new AsnDataTypeNotSupportedException(tag, "Invalid tag: " + tag);
         case 2:
            if (asnType != -1 && asnType != tag) {
               throw new AsnException("Invalid tag: " + tag);
            }

            i = in.readUnsignedInt();
            break;
         case 3:
            if (asnType != -1 && asnType != tag) {
               throw new AsnException("Invalid tag: " + tag);
            }

            i = in.readSignedInteger();
            break;
         case 9:
            if (asnType != -1 && asnType != tag) {
               throw new AsnException("Invalid tag: " + tag);
            }

            i = in.readEnumerated();
      }

      return new ScheduleSupport0.TimeValue(t, i);
   }

   private ScheduleSupport0.TimeValue decodeTimeValueString(AsnInput in, BStatusString defaultValue) throws AsnException {
      BBacnetTime t = in.readTime();
      this.checkForUnspecified(t);
      int tag = in.peekApplicationTag();
      switch (tag) {
         case 0:
            in.readNull();
            return new ScheduleSupport0.TimeValue(t, null);
         case 7:
            String s = in.readCharacterString();
            return new ScheduleSupport0.TimeValue(t, s);
         default:
            throw new AsnDataTypeNotSupportedException(tag, "Invalid tag: " + tag);
      }
   }

   private ScheduleSupport0.TimeValue[] decodeTimeValues(int contextTag, AsnInput in, BStatusValue defaultValue, int asnType) throws AsnException {
      int tag = in.peekTag();
      if (!in.isOpeningTag(contextTag)) {
         throw new AsnException("Invalid tag: " + tag);
      } else {
         in.skipTag();
         tag = in.peekTag();

         ArrayList<ScheduleSupport0.TimeValue> v;
         for (v = new ArrayList<>(); !in.isClosingTag(contextTag); tag = in.peekTag()) {
            if (tag == -1) {
               throw new AsnException("Invalid tag: " + tag);
            }

            v.add(this.decodeTimeValue(in, defaultValue, asnType));
         }

         in.skipTag();
         return v.toArray(new ScheduleSupport0.TimeValue[0]);
      }
   }

   private void encodeScheduleDefault(BStatusBoolean defaultOutput, AsnOutput out, int asnType) {
      if (defaultOutput != null && !defaultOutput.getStatus().isNull()) {
         boolean b = defaultOutput.getValue();
         switch (asnType) {
            case 1:
               out.writeBoolean(b);
               break;
            case 2:
               out.writeUnsignedInteger(b ? 1L : 0L);
               break;
            case 3:
               out.writeSignedInteger(b ? 1 : 0);
               break;
            case 4:
               out.writeReal(b ? 1.0 : 0.0);
               break;
            case 5:
               out.writeDouble(b ? 1.0 : 0.0);
               break;
            case 6:
               out.writeOctetString(new byte[]{(byte)(b ? 1 : 0)});
               break;
            case 7:
               out.writeCharacterString(String.valueOf(b));
               break;
            case 8:
            default:
               out.writeNull();
               break;
            case 9:
               out.writeEnumerated(b ? 1 : 0);
         }
      } else {
         out.writeNull();
      }
   }

   private void encodeScheduleDefault(BStatusNumeric defaultOutput, AsnOutput out, int asnType) {
      if (defaultOutput != null && !defaultOutput.getStatus().isNull()) {
         double d = defaultOutput.getValue();
         switch (asnType) {
            case 1:
               out.writeBoolean(BDouble.equals(d, 0.0));
               break;
            case 2:
               out.writeUnsignedInteger((int)d & 2147483647);
               break;
            case 3:
               out.writeSignedInteger((int)d);
               break;
            case 4:
               out.writeReal((float)d);
               break;
            case 5:
               out.writeDouble(d);
               break;
            case 6:
               out.writeOctetString(new byte[]{(byte)d});
               break;
            case 7:
               out.writeCharacterString(String.valueOf(d));
               break;
            case 8:
            default:
               out.writeNull();
               break;
            case 9:
               out.writeEnumerated((int)d);
         }
      } else {
         out.writeNull();
      }
   }

   private void encodeScheduleDefault(BStatusEnum defaultOutput, AsnOutput out, int asnType) {
      if (defaultOutput != null && !defaultOutput.getStatus().isNull()) {
         int i = defaultOutput.getValue().getOrdinal();
         BEnumRange r = defaultOutput.getValue().getRange();
         switch (asnType) {
            case 1:
               out.writeBoolean(i != 0);
               break;
            case 2:
               out.writeUnsignedInteger(i & 2147483647);
               break;
            case 3:
               out.writeSignedInteger(i);
               break;
            case 4:
               out.writeReal(i);
               break;
            case 5:
               out.writeDouble(i);
               break;
            case 6:
               out.writeOctetString(new byte[]{(byte)i});
               break;
            case 7:
               out.writeCharacterString(r.getTag(i));
               break;
            case 8:
            default:
               out.writeNull();
               break;
            case 9:
               out.writeEnumerated(i);
         }
      } else {
         out.writeNull();
      }
   }

   private void encodeScheduleDefault(BStatusString defaultOutput, AsnOutput out, int asnType) {
      if (defaultOutput != null && !defaultOutput.getStatus().isNull()) {
         String s = defaultOutput.getValue();
         switch (asnType) {
            case 1:
               out.writeBoolean(s.equalsIgnoreCase("true"));
               break;
            case 2:
               out.writeUnsignedInteger(Long.parseLong(s) & 2147483647L);
               break;
            case 3:
               out.writeSignedInteger(Integer.parseInt(s));
               break;
            case 4:
               out.writeReal(Float.parseFloat(s));
               break;
            case 5:
               out.writeDouble(Double.parseDouble(s));
               break;
            case 6:
               out.writeOctetString(new byte[]{Byte.parseByte(s)});
               break;
            case 7:
               out.writeCharacterString(s);
               break;
            case 8:
            default:
               out.writeNull();
               break;
            case 9:
               out.writeEnumerated(Integer.parseInt(s));
         }
      } else {
         out.writeNull();
      }
   }

   private void encodeTimeValue(ScheduleSupport0.TimeValue tv, AsnOutput out, int asnType) {
      out.writeTime(tv.hour, tv.minute, tv.second, tv.hund());
      if (tv.value == null) {
         out.writeNull();
      } else if (tv.value instanceof Boolean) {
         this.encodeValue((Boolean)tv.value, out, asnType);
      } else if (tv.value instanceof Double) {
         this.encodeValue((Double)tv.value, out, asnType);
      } else if (tv.value instanceof Integer) {
         this.encodeValue((Integer)tv.value, out, asnType);
      } else if (tv.value instanceof String) {
         this.encodeValue((String)tv.value, out, asnType);
      } else if (log().isTraceOn()) {
         log().trace("tv value=" + tv.value + (tv.value != null ? " " + tv.value.getClass() : ""));
      }
   }

   private void encodeTimeValues(ScheduleSupport0.TimeValue[] tvs, int contextTag, AsnOutput out, int asnType) {
      if (tvs != null) {
         out.writeOpeningTag(contextTag);

         for (int i = 0; i < tvs.length; i++) {
            this.encodeTimeValue(tvs[i], out, asnType);
         }

         out.writeClosingTag(contextTag);
      }
   }

   private void encodeValue(Boolean b, AsnOutput out, int asnType) {
      switch (asnType) {
         case 1:
            out.writeBoolean(b);
            break;
         case 2:
            out.writeUnsignedInteger(b ? 1L : 0L);
            break;
         case 3:
            out.writeSignedInteger(b ? 1 : 0);
            break;
         case 4:
            out.writeReal(b ? 1.0 : 0.0);
            break;
         case 5:
            out.writeDouble(b ? 1.0 : 0.0);
            break;
         case 6:
            out.writeOctetString(new byte[]{(byte)(b ? 1 : 0)});
            break;
         case 7:
            out.writeCharacterString(b.toString());
            break;
         case 8:
         default:
            out.writeBoolean(b);
            break;
         case 9:
            out.writeEnumerated(b ? 1 : 0);
      }
   }

   private void encodeValue(Double d, AsnOutput out, int asnType) {
      switch (asnType) {
         case 1:
            out.writeBoolean(!BDouble.equals(d, 0.0));
            break;
         case 2:
            out.writeUnsignedInteger((int)d.doubleValue());
            break;
         case 3:
            out.writeSignedInteger((int)d.doubleValue());
            break;
         case 4:
            out.writeReal((float)d.doubleValue());
            break;
         case 5:
            out.writeDouble(d);
            break;
         case 6:
            out.writeOctetString(new byte[]{(byte)d.doubleValue()});
            break;
         case 7:
            out.writeCharacterString(d.toString());
            break;
         case 8:
         default:
            out.writeReal((float)d.doubleValue());
            break;
         case 9:
            out.writeEnumerated((int)d.doubleValue());
      }
   }

   private void encodeValue(Integer i, AsnOutput out, int asnType) {
      switch (asnType) {
         case 1:
            out.writeBoolean(i != 0);
            break;
         case 2:
            out.writeUnsignedInteger(i & 2147483647);
            break;
         case 3:
            out.writeSignedInteger(i);
            break;
         case 4:
            out.writeReal(i.intValue());
            break;
         case 5:
            out.writeDouble(i.intValue());
            break;
         case 6:
            out.writeOctetString(new byte[]{(byte)i.intValue()});
            break;
         case 7:
            out.writeCharacterString(i.toString());
            break;
         case 8:
         default:
            out.writeUnsignedInteger(i & 2147483647);
            break;
         case 9:
            out.writeEnumerated(i);
      }
   }

   private void encodeValue(String s, AsnOutput out, int asnType) {
      switch (asnType) {
         case 1:
            out.writeBoolean(s.equalsIgnoreCase("true"));
            break;
         case 2:
            out.writeUnsignedInteger(Long.parseLong(s) & 2147483647L);
            break;
         case 3:
            out.writeSignedInteger(Integer.parseInt(s));
            break;
         case 4:
            out.writeReal(Float.parseFloat(s));
            break;
         case 5:
            out.writeDouble(Double.parseDouble(s));
            break;
         case 6:
            out.writeOctetString(new byte[]{Byte.parseByte(s)});
            break;
         case 7:
            out.writeCharacterString(s);
            break;
         case 8:
         default:
            out.writeCharacterString(s);
            break;
         case 9:
            out.writeEnumerated(Integer.parseInt(s));
      }
   }

   static Log log() {
      return logger;
   }

   private static class BacnetScheduleComparator implements Comparator<Object> {
      private BacnetScheduleComparator() {
      }

      @Override
      public int compare(Object o1, Object o2) {
         if (o1 != null && o2 != null && o1 instanceof BDailySchedule && o2 instanceof BDailySchedule) {
            BDailySchedule dailySchedule1 = (BDailySchedule)o1;
            BDailySchedule dailySchedule2 = (BDailySchedule)o2;
            BValue idx1 = dailySchedule1.get("bacnetIdx");
            BValue idx2 = dailySchedule2.get("bacnetIdx");
            if (idx1 != null && idx2 != null && idx1 instanceof BInteger && idx2 instanceof BInteger) {
               BInteger int1 = (BInteger)idx1;
               BInteger int2 = (BInteger)idx2;
               return int1.getInt() - int2.getInt();
            }
         }

         return 0;
      }
   }

   static class TimeValue {
      public int hour;
      public int minute;
      public int second;
      public int millis;
      public Object value;

      public TimeValue() {
      }

      public TimeValue(BTime t, Object v) {
         this.hour = t.getHour();
         this.minute = t.getMinute();
         this.second = t.getSecond();
         this.millis = t.getMillisecond();
         this.value = v;
      }

      public TimeValue(BBacnetTime t, Object v) {
         this.hour = t.getHour();
         this.minute = t.getMinute();
         this.second = t.getSecond();
         this.millis = t.getHundredth();
         if (this.millis > 0) {
            this.millis *= 10;
         }

         this.value = v;
      }

      public int compareTo(Object o) {
         int me = this.millis();
         int him = ((ScheduleSupport0.TimeValue)o).millis();
         if (me < him) {
            return -1;
         } else {
            return me > him ? 1 : 0;
         }
      }

      @Override
      public boolean equals(Object o) {
         return o instanceof ScheduleSupport0.TimeValue ? this.millis() == ((ScheduleSupport0.TimeValue)o).millis() : false;
      }

      @Override
      public int hashCode() {
         return this.hour << 24 | this.minute << 16 | this.second << 8 | this.millis;
      }

      public int millis() {
         int ret = this.second * 1000;
         ret += this.minute * 60000;
         ret += this.hour * 3600000;
         return ret + this.millis;
      }

      public int hund() {
         return this.millis < 0 ? this.millis : this.millis / 10;
      }

      @Override
      public String toString() {
         return this.hour + ":" + this.minute + ":" + this.second + "." + this.millis + ", " + this.value;
      }
   }
}
