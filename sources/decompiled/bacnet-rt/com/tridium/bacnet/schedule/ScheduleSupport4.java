package com.tridium.bacnet.schedule;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.TreeSet;
import javax.baja.bacnet.io.DuplicateEntryException;
import javax.baja.schedule.BDaySchedule;
import javax.baja.schedule.BTimeSchedule;
import javax.baja.sys.BTime;

public class ScheduleSupport4 extends ScheduleSupport0 {
   public ScheduleSupport4() {
   }

   public ScheduleSupport4(BBacnetScheduleDeviceExt ext) {
      super(ext);
   }

   @Override
   public String getVersion() {
      return "ScheduleSupport 1.4";
   }

   @Override
   protected ScheduleSupport0.TimeValue[] makeDay(BDaySchedule dsch, Object defaultValue) {
      boolean trace = log().isTraceOn();
      if (trace) {
         log().trace("makeDay(1.4): daySch=");
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
      if (trace) {
         log().trace("timesInOrder:");

         for (int i = 0; i < times.length; i++) {
            log().trace("timeSched " + i + ":");
            BBacnetScheduleDeviceExt.dump(times[i]);
         }
      }

      for (int i = 0; i < times.length; i++) {
         BTimeSchedule t = times[i];
         ScheduleSupport0.TimeValue v = new ScheduleSupport0.TimeValue(t.getStart(), this.bajaToJava(t.getEffectiveValue()));
         set.add(v);
         if (lastFinish != null && lastFinish.millis() != v.millis()) {
            set.add(lastFinish);
         }

         lastFinish = new ScheduleSupport0.TimeValue(t.getFinish(), null);
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

      if (trace) {
         log().trace("TimeValue[] return array:");

         for (int j = 0; j < ret.length; j++) {
            log().trace("ret[" + j + "]:" + ret[j]);
         }
      }

      return ret;
   }

   @Override
   protected BDaySchedule makeDay(ScheduleSupport0.TimeValue[] tvin, Object defaultValue) throws DuplicateEntryException {
      boolean trace = log().isTraceOn();
      if (trace) {
         log().trace("makeDay(1.4): tvin=");

         for (int i = 0; i < tvin.length; i++) {
            log().trace("tvin[" + i + "]:" + tvin[i]);
         }
      }

      ScheduleSupport0.TimeValue[] tvs = new ScheduleSupport0.TimeValue[tvin.length];
      System.arraycopy(tvin, 0, tvs, 0, tvin.length);
      Arrays.sort(tvs, new Comparator<Object>() {
         @Override
         public int compare(Object o1, Object o2) {
            return ((ScheduleSupport0.TimeValue)o1).compareTo(o2);
         }
      });
      if (trace) {
         log().trace("makeDay(1.4): tvs=");

         for (int i = 0; i < tvs.length; i++) {
            log().trace("tvs[" + i + "]:" + tvs[i]);
         }
      }

      BDaySchedule ret = new BDaySchedule();
      BTimeSchedule tsch = null;

      for (int i = 0; i < tvs.length; i++) {
         if (tsch == null) {
            if (tvs[i].value == null) {
               if (trace) {
                  log().trace("NULL TimeValue outside event: skip...");
               }
            } else {
               tsch = new BTimeSchedule();
               tsch.setStart(this.makeTime(tvs[i], true));
               tsch.setEffectiveValue(this.javaToBaja(tvs[i].value, defaultValue));
            }
         } else if (tvs[i].compareTo(tvs[i - 1]) == 0) {
            this.handleDuplicateTimeValue(tvs[i]);
         } else if (tvs[i].value == null) {
            tsch.setFinish(this.makeTime(tvs[i], true));
            ret.add(tsch);
            tsch = null;
         } else {
            tsch.setFinish(this.makeTime(tvs[i], true));
            ret.add(tsch);
            tsch = new BTimeSchedule();
            tsch.setStart(this.makeTime(tvs[i], true));
            tsch.setEffectiveValue(this.javaToBaja(tvs[i].value, defaultValue));
         }
      }

      if (tsch != null) {
         tsch.setFinish(BTime.make(0, 0, 0, 0));
         ret.add(tsch);
      }

      if (trace) {
         log().trace("returned daySch:");
         BBacnetScheduleDeviceExt.dump(ret);
      }

      return ret;
   }

   protected void handleDuplicateTimeValue(ScheduleSupport0.TimeValue timeValue) throws DuplicateEntryException {
      if (log().isTraceOn()) {
         log().trace("Duplicate time: skip...");
      }
   }
}
