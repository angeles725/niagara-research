package com.tridium.niagarad.log;

import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class NullLogger extends Logger {
   private static NullLogger instance = null;

   protected NullLogger(String name, String resourceBundleName) {
      super(name, resourceBundleName);
   }

   @Override
   public void log(LogRecord record) {
   }

   public static NullLogger getInstance() {
      if (instance == null) {
         instance = new NullLogger(null, null);
      }

      return instance;
   }

   public void unload() {
      if (instance != null) {
         instance = null;
      }
   }
}
