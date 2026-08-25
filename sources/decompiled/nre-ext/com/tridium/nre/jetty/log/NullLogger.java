package com.tridium.nre.jetty.log;

import org.eclipse.jetty.util.log.Logger;

public final class NullLogger implements Logger {
   public static final Logger INSTANCE = new NullLogger();

   private NullLogger() {
   }

   public Logger getLogger(String name) {
      return this;
   }

   public String getName() {
      return "NullLogger";
   }

   public void debug(String msg, long value) {
   }

   public void warn(String msg, Object... args) {
   }

   public void warn(Throwable thrown) {
   }

   public void warn(String msg, Throwable thrown) {
   }

   public void info(String msg, Object... args) {
   }

   public void info(Throwable thrown) {
   }

   public void info(String msg, Throwable thrown) {
   }

   public boolean isDebugEnabled() {
      return false;
   }

   public void setDebugEnabled(boolean enabled) {
   }

   public void debug(String msg, Object... args) {
   }

   public void debug(Throwable thrown) {
   }

   public void debug(String msg, Throwable thrown) {
   }

   public void ignore(Throwable ignored) {
   }

   public static Logger getInstance() {
      return INSTANCE;
   }
}
