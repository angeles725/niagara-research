package com.tridium.niagarad.log;

import com.tridium.nre.platform.IPlatformProvider;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

class SystemLogHandler extends Handler {
   public static final int _SYSTEMLOG_TRACE = 0;
   public static final int _SYSTEMLOG_MESSAGE = 1;
   public static final int _SYSTEMLOG_WARNING = 2;
   public static final int _SYSTEMLOG_ERROR = 3;
   private final IPlatformProvider platformProvider;

   SystemLogHandler(IPlatformProvider platformProvider) {
      this.platformProvider = platformProvider;
   }

   @Override
   public void publish(LogRecord record) {
      int nativeLogLevel = 1;
      Level level = record.getLevel();
      if (level == Level.ALL || level == Level.FINE || level == Level.FINER || level == Level.FINEST) {
         nativeLogLevel = 0;
      } else if (level == Level.INFO || level == Level.CONFIG) {
         nativeLogLevel = 1;
      } else if (level == Level.WARNING) {
         nativeLogLevel = 2;
      } else if (level == Level.SEVERE) {
         nativeLogLevel = 3;
      }

      this.platformProvider.log(nativeLogLevel, NiagaraDaemonFormatter.getInstance().format(record));
   }

   @Override
   public void flush() {
   }

   @Override
   public void close() throws SecurityException {
   }
}
