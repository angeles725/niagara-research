package com.tridium.bacnet.schedule;

import javax.baja.bacnet.io.DuplicateEntryException;

public class ScheduleSupport16 extends ScheduleSupport4 {
   public ScheduleSupport16() {
   }

   public ScheduleSupport16(BBacnetScheduleDeviceExt ext) {
      super(ext);
   }

   @Override
   public String getVersion() {
      return "ScheduleSupport 1.16";
   }

   @Override
   protected void handleDuplicateTimeValue(ScheduleSupport0.TimeValue timeValue) throws DuplicateEntryException {
      if (log().isTraceOn()) {
         log().trace("Duplicate time: " + timeValue);
      }

      throw new DuplicateEntryException("Duplicate time value within a single day: " + timeValue);
   }
}
