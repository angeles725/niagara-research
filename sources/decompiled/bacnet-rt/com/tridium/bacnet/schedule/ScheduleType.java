package com.tridium.bacnet.schedule;

public class ScheduleType {
   private String scheduleType;
   private String dataType;

   public ScheduleType() {
   }

   public ScheduleType(String s) {
      this.scheduleType = s;
      this.dataType = "";
   }

   public ScheduleType(String s, String d) {
      this.scheduleType = s;
      this.dataType = d;
   }

   public String getScheduleType() {
      return this.scheduleType;
   }

   public String getDataType() {
      return this.dataType;
   }

   @Override
   public String toString() {
      return this.scheduleType + ":" + this.dataType;
   }
}
