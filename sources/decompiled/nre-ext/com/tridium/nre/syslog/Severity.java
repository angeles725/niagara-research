package com.tridium.nre.syslog;

public enum Severity {
   EMERGENCY(0),
   ALERT(1),
   CRITICAL(2),
   ERROR(3),
   WARNING(4),
   NOTICE(5),
   INFO(6),
   DEBUG(7);

   private final int level;

   Severity(int level) {
      this.level = level;
   }

   public int getLevel() {
      return this.level;
   }
}
