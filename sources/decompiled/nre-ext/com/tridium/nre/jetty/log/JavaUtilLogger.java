package com.tridium.nre.jetty.log;

import java.util.logging.Level;
import java.util.logging.LogRecord;
import org.eclipse.jetty.util.log.Logger;

public class JavaUtilLogger implements Logger {
   public java.util.logging.Logger logger = java.util.logging.Logger.getLogger("jetty");
   private Level configuredLevel;
   public static final Logger INSTANCE = new JavaUtilLogger();

   private JavaUtilLogger() {
   }

   public Logger getLogger(String name) {
      return this;
   }

   public String getName() {
      return this.logger.getName();
   }

   protected void log(Level level, String msg, Throwable thrown) {
      LogRecord record = new LogRecord(level, msg);
      if (thrown != null) {
         record.setThrown(thrown);
      }

      record.setLoggerName(this.logger.getName());
      StackTraceElement[] stack = new Throwable().getStackTrace();

      for (StackTraceElement e : stack) {
         if (!e.getClassName().equals(JavaUtilLogger.class.getName())) {
            record.setSourceClassName(e.getClassName());
            record.setSourceMethodName(e.getMethodName());
            break;
         }
      }

      this.logger.log(record);
   }

   public void warn(String msg, Object... args) {
      if (this.logger.isLoggable(Level.WARNING)) {
         this.log(Level.WARNING, this.format(msg, args), null);
      }
   }

   public void warn(Throwable thrown) {
      if (this.logger.isLoggable(Level.WARNING)) {
         this.log(Level.WARNING, "", thrown);
      }
   }

   public void warn(String msg, Throwable thrown) {
      if (this.logger.isLoggable(Level.WARNING)) {
         this.log(Level.WARNING, msg, thrown);
      }
   }

   public void info(String msg, Object... args) {
      if (this.logger.isLoggable(Level.INFO)) {
         this.log(Level.INFO, this.format(msg, args), null);
      }
   }

   public void info(Throwable thrown) {
      if (this.logger.isLoggable(Level.INFO)) {
         this.log(Level.INFO, "", thrown);
      }
   }

   public void info(String msg, Throwable thrown) {
      if (this.logger.isLoggable(Level.INFO)) {
         this.log(Level.INFO, msg, thrown);
      }
   }

   public boolean isDebugEnabled() {
      return this.logger.isLoggable(Level.FINE);
   }

   public void setDebugEnabled(boolean enabled) {
      if (enabled) {
         this.configuredLevel = this.logger.getLevel();
         this.logger.setLevel(Level.FINE);
      } else {
         this.logger.setLevel(this.configuredLevel);
      }
   }

   public void debug(String msg, Object... args) {
      if (this.logger.isLoggable(Level.FINE)) {
         this.log(Level.FINE, this.format(msg, args), null);
      }
   }

   public void debug(String msg, long arg) {
      if (this.logger.isLoggable(Level.FINE)) {
         this.log(Level.FINE, this.format(msg, arg), null);
      }
   }

   public void debug(Throwable thrown) {
      if (this.logger.isLoggable(Level.FINE)) {
         this.log(Level.FINE, "", thrown);
      }
   }

   public void debug(String msg, Throwable thrown) {
      if (this.logger.isLoggable(Level.FINE)) {
         this.log(Level.FINE, msg, thrown);
      }
   }

   public void ignore(Throwable ignored) {
      if (this.logger.isLoggable(Level.ALL)) {
         this.log(Level.WARNING, "IGNORED EXCEPTION ", ignored);
      }
   }

   public static Logger getInstance() {
      return INSTANCE;
   }

   private String format(String msg, Object... args) {
      msg = String.valueOf(msg);
      String braces = "{}";
      StringBuilder builder = new StringBuilder();
      int start = 0;

      for (Object arg : args) {
         int bracesIndex = msg.indexOf(braces, start);
         if (bracesIndex < 0) {
            builder.append(msg.substring(start));
            builder.append(" ");
            builder.append(arg);
            start = msg.length();
         } else {
            builder.append(msg, start, bracesIndex);
            builder.append(arg);
            start = bracesIndex + braces.length();
         }
      }

      builder.append(msg.substring(start));
      return builder.toString();
   }
}
