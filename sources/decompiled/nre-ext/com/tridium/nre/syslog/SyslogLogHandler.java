package com.tridium.nre.syslog;

import com.tridium.nre.util.IPAddressUtil;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.Locale;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SyslogLogHandler extends Handler {
   private static final Pattern NEWLINE_PATTERN = Pattern.compile("/[\r\n]+/gm", 16);
   private final LogAdapter logAdapter = new LogAdapter();
   private final SyslogManager logManager;
   private final ThreadLocal<Boolean> ignoreMessages = ThreadLocal.withInitial(() -> false);

   public SyslogLogHandler() {
      this.logManager = SyslogManager.getInstance();
   }

   @Override
   public void publish(LogRecord record) {
      if (this.logManager.getEnabled()) {
         if (!this.logManager.isSenderThread(record.getThreadID())) {
            if (this.logManager.isValidEnvironment(this.logManager.getEnvironmentTag())) {
               if (!"platform".equalsIgnoreCase(this.logManager.getEnvironmentTag()) || this.logManager.getPlatformLogEnabled()) {
                  if (!this.logManager.getEnvironmentTag().toLowerCase(Locale.ENGLISH).startsWith("station") || this.logManager.getStationLogEnabled()) {
                     if (this.isLoggable(record)) {
                        if (this.ignoreMessages.get()) {
                           if (!"syslog".equals(record.getLoggerName()) && SyslogManager.LOG.isLoggable(Level.FINEST)) {
                              SyslogManager.LOG.finest("ignoring log record: " + record.getMessage());
                           }
                        } else {
                           try {
                              this.ignoreMessages.set(true);
                              SyslogManager.LOG.fine("Publishing log record");

                              try {
                                 StringBuilder msgString = new StringBuilder();
                                 String priority = this.logAdapter.adaptPriority(record, this.logManager.getFacility());
                                 msgString.append(priority);
                                 String timestamp = this.logAdapter.adaptTimeStamp(record);
                                 msgString.append(timestamp).append(' ');
                                 String host = IPAddressUtil.getHostName();
                                 msgString.append(host).append(' ');
                                 String tag = this.logManager.getEnvironmentTag();
                                 msgString.append(tag).append(": ");
                                 msgString.append(this.format(record).trim());
                                 Message message = new Message();
                                 message.print(msgString.toString());
                                 this.logManager.publish(message);
                              } catch (Throwable t) {
                                 SyslogManager.LOG.log(Level.SEVERE, "Failed to publish log record.", t);
                              }
                           } finally {
                              this.ignoreMessages.set(false);
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   public String format(LogRecord record) {
      Date date = new Date(record.getMillis());
      String source;
      if (record.getSourceClassName() != null) {
         if (record.getSourceMethodName() != null) {
            source = record.getSourceClassName() + ':' + record.getSourceMethodName();
         } else {
            source = record.getSourceClassName();
         }
      } else {
         source = record.getLoggerName();
      }

      String exception = "";
      Throwable thrown = record.getThrown();
      if (thrown != null) {
         PrintWriter writer = new PrintWriter(new StringWriter());
         thrown.printStackTrace(writer);
         exception = writer.toString();
         exception = NEWLINE_PATTERN.matcher(exception).replaceAll(Matcher.quoteReplacement("\\"));
      }

      return String.format(
         this.logManager.getFormat(), date, source, record.getLoggerName(), record.getLevel().getLocalizedName(), record.getMessage(), exception
      );
   }

   @Override
   public void flush() {
   }

   @Override
   public void close() {
   }
}
