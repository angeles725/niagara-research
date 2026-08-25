package com.tridium.niagarad.log;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class NiagaraDaemonFormatter extends Formatter {
   private final Date date = new Date();

   private NiagaraDaemonFormatter() {
   }

   public static NiagaraDaemonFormatter getInstance() {
      return NiagaraDaemonFormatter.NiagaraDaemonFormatterHolder._FORMATTER_INSTANCE;
   }

   @Override
   public synchronized String format(LogRecord record) {
      this.date.setTime(record.getMillis());
      String source;
      if (record.getSourceClassName() != null) {
         source = record.getSourceClassName();
         if (record.getSourceMethodName() != null) {
            source = source + " " + record.getSourceMethodName();
         }
      } else {
         source = record.getLoggerName();
      }

      String message = this.formatMessage(record);
      String throwable = "";
      if (record.getThrown() != null) {
         StringWriter sw = new StringWriter();
         PrintWriter pw = new PrintWriter(sw);
         pw.println();
         record.getThrown().printStackTrace(pw);
         pw.close();
         throwable = sw.toString();
      }

      String format = "%4$s [%1$tH:%1$tM:%1$tS %1$td-%1$tb-%1$ty %1$tZ][%3$s] %5$s%6$s%n";
      return String.format(format, this.date, source, record.getLoggerName(), record.getLevel(), message, throwable);
   }

   private static class NiagaraDaemonFormatterHolder {
      public static final NiagaraDaemonFormatter _FORMATTER_INSTANCE = new NiagaraDaemonFormatter();
   }
}
