package com.tridium.niagarad.log;

import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class MulticastLogger extends Logger {
   private Logger[] loggers;

   protected MulticastLogger(String name, String resourceBundleName) {
      super(name, resourceBundleName);
   }

   public MulticastLogger(Logger[] loggers) {
      this(null, null);
      this.setLevel(Level.ALL);
      this.loggers = loggers;
   }

   @Override
   public void log(LogRecord record) {
      for (Logger log : this.loggers) {
         log.log(record.getLevel(), record.getMessage());
      }
   }
}
