package com.tridium.niagarad.log;

import com.tridium.nre.platform.IPlatformProvider;
import com.tridium.nre.security.SecurityConstants;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.PlatformLoggingMXBean;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import javax.baja.nre.util.ByteBuffer;
import javax.baja.nre.util.FileUtil;

public class NiagaraDaemonLogSettings {
   private static PlatformLoggingMXBean platformLoggingMXBean;
   private static Properties logProps;
   private static Level forcedLevel = null;
   private static final HashSet<Logger> FORCED_LOGS = new HashSet<>();
   private static long saveTime = 0L;
   public static final String[] IGNORED_LOG_PREFIXES = new String[]{
      "javax.management", "jdk.event.security", "java.io.serialization", "org.bouncycastle.", "InputStreamInfo"
   };
   private static volatile NiagaraDaemonLogSettings instance;

   public static void bootstrap(FileInputStream logFileInputStream) throws Exception {
      reload(logFileInputStream);
   }

   public static void reload(FileInputStream logFileInputStream) throws Exception {
      ByteBuffer loggingProperties = new ByteBuffer();

      try (OutputStream outputStream = loggingProperties.getOutputStream()) {
         FileUtil.pipe(logFileInputStream, outputStream);
      }

      logProps = new Properties();
      logProps.load(loggingProperties.getInputStream());
      loggingProperties.seek(0);
      LogManager.getLogManager().readConfiguration(loggingProperties.getInputStream());
      platformLoggingMXBean = ManagementFactory.getPlatformMXBean(PlatformLoggingMXBean.class);
   }

   public static NiagaraDaemonLogSettings getInstance() {
      if (instance == null) {
         instance = new NiagaraDaemonLogSettings();
      }

      return instance;
   }

   private NiagaraDaemonLogSettings() {
   }

   public List<String> getRegisteredLogs() {
      List<String> registeredLogs = platformLoggingMXBean.getLoggerNames();
      Collections.sort(registeredLogs);
      return registeredLogs;
   }

   public List<String> getDeclaredLogs() {
      List<String> declared = new ArrayList<>(logProps.size());

      for (String key : logProps.stringPropertyNames()) {
         if (!key.startsWith("java.util.logging.") && key.endsWith(".level")) {
            declared.add(key.substring(0, key.lastIndexOf(46)));
         }
      }

      Collections.sort(declared);
      return declared;
   }

   public List<String> getAllLogs() {
      List<String> registeredLogs = this.getRegisteredLogs();
      List<String> declaredLogs = this.getDeclaredLogs();
      HashSet<String> allLogsSet = new HashSet<>();
      allLogsSet.addAll(registeredLogs);
      allLogsSet.addAll(declaredLogs);
      List<String> allLogsList = new ArrayList<>(allLogsSet);
      Collections.sort(allLogsList);
      return allLogsList;
   }

   public static Level getLogLevel(String logName) {
      return getLogLevel(logName, true);
   }

   private static Level getLogLevel(String logName, boolean createIfAbsent) {
      String logLevel = platformLoggingMXBean.getLoggerLevel(logName);
      if (logLevel == null) {
         if (createIfAbsent) {
            Logger.getLogger(logName);
            return getLogLevel(logName, false);
         } else {
            return Level.INFO;
         }
      } else {
         if (!logLevel.isEmpty()) {
            return Level.parse(logLevel);
         }

         String parentLogName = platformLoggingMXBean.getParentLoggerName(logName);
         String parentLogLevel = platformLoggingMXBean.getLoggerLevel(parentLogName);
         int depth = 0;

         for (int depthLimit = 10;
            !parentLogName.isEmpty() && depth++ < depthLimit && parentLogLevel.isEmpty();
            parentLogLevel = platformLoggingMXBean.getLoggerLevel(parentLogName)
         ) {
            parentLogName = platformLoggingMXBean.getParentLoggerName(parentLogName);
         }

         return !parentLogLevel.isEmpty() ? Level.parse(parentLogLevel) : Level.INFO;
      }
   }

   public void setLogLevel(String logName, Level level) {
      if (!platformLoggingMXBean.getLoggerNames().contains(logName)) {
         Logger.getLogger(logName);
      }

      try {
         platformLoggingMXBean.setLoggerLevel(logName, level.getName());
      } catch (IllegalArgumentException iae) {
         if (iae.toString().contains("does not exist")) {
            Logger.getLogger(logName);
            this.setLogLevel(logName, level);
         }
      }

      logProps.setProperty(logName + ".level", level.getName());
   }

   public void forceLevel(Level level) {
      if (level != null) {
         forcedLevel = level;

         for (String log : this.getAllLogs()) {
            if (!log.equals("jetty") && !log.startsWith("org.bouncycastle")) {
               this.setLogLevel(log, level);
               FORCED_LOGS.add(Logger.getLogger(log));
            }
         }
      }
   }

   public Level getForceLevel() {
      return forcedLevel;
   }

   public void enableConsoleLogging(boolean enable) {
      Handler existingHandler = null;
      Logger rootLogger = Logger.getLogger("");

      for (Handler handler : rootLogger.getHandlers()) {
         if (handler instanceof ConsoleHandler) {
            existingHandler = handler;
            break;
         }
      }

      if (!enable && existingHandler != null) {
         Logger.getLogger("").removeHandler(existingHandler);
      } else {
         Handler targetHandler;
         if (existingHandler == null) {
            targetHandler = new ConsoleHandler();
         } else {
            targetHandler = existingHandler;
         }

         targetHandler.setLevel(Level.ALL);
         targetHandler.setFilter(null);
         targetHandler.setFormatter(NiagaraDaemonFormatter.getInstance());
         if (existingHandler == null) {
            Logger.getLogger("").addHandler(targetHandler);
         }
      }
   }

   public void enableSystemLogging(IPlatformProvider platformProvider) throws Exception {
      boolean foundSystemHandler = false;
      Logger rootLogger = Logger.getLogger("");

      for (Handler handler : rootLogger.getHandlers()) {
         if (handler instanceof SystemLogHandler) {
            foundSystemHandler = true;
            break;
         }
      }

      if (!foundSystemHandler) {
         if (!platformProvider.enableSystemLogging()) {
            throw new Exception("Failed to enable system logging");
         }

         SystemLogHandler systemLogHandler = new SystemLogHandler(platformProvider);
         systemLogHandler.setLevel(Level.ALL);
         systemLogHandler.setFilter(null);
         systemLogHandler.setFormatter(NiagaraDaemonFormatter.getInstance());
         Logger.getLogger("").addHandler(systemLogHandler);
      }
   }

   public boolean save(FileOutputStream logFileOutputStream) {
      return this.save(logFileOutputStream, false);
   }

   public boolean save(FileOutputStream logFileOutputStream, boolean declaredOnly) {
      if (forcedLevel != null) {
         return false;
      }

      try {
         this.writeTo(logFileOutputStream, declaredOnly);
      } catch (Exception e) {
         return false;
      }

      saveTime = System.currentTimeMillis();
      return true;
   }

   public long getSaveTime() {
      return saveTime;
   }

   public static void writeDefaultLogConfig(OutputStream o) {
      PrintWriter out = new PrintWriter(o);
      out.println("# DO NOT MODIFY: Auto-generated by Niagara on " + new Date());
      out.println(".level = INFO");
      out.println();
      String logLevel = SecurityConstants.canCheckTpk() ? "SEVERE" : "WARNING";
      out.println("jetty.level = " + logLevel);
      out.println("org.bouncycastle.level = SEVERE");
      out.flush();
   }

   private void writeTo(OutputStream out, boolean declaredOnly) {
      PrintWriter p = new PrintWriter(out);
      Map<String, String> working = new HashMap<>();

      for (String key : logProps.stringPropertyNames()) {
         working.put(key, logProps.getProperty(key));
      }

      writeHeader(p);
      writeRootSettings(p, working);
      this.writeLogs(p, working, declaredOnly);
      writeRemaining(p, working);
      if (!working.isEmpty()) {
         throw new IllegalStateException("Error: Not all logging properties written");
      }

      p.flush();
   }

   private static void writeHeader(PrintWriter p) {
      p.println("# DO NOT MODIFY: Auto-generated by Niagara on " + new Date());
   }

   private static void writeRootSettings(PrintWriter p, Map<String, String> props) {
      String level = props.remove(".level");
      if (level == null) {
         level = "INFO";
      }

      p.println(".level = " + level);
      p.println();
   }

   private void writeLogs(PrintWriter p, Map<String, String> props, boolean declaredOnly) {
      for (String log : declaredOnly ? this.getDeclaredLogs() : this.getAllLogs()) {
         if (!log.isEmpty() && !log.equalsIgnoreCase("global")) {
            boolean containsIgnoredPrefix = false;

            for (String ignoredLogPrefix : IGNORED_LOG_PREFIXES) {
               containsIgnoredPrefix = log.startsWith(ignoredLogPrefix);
               if (containsIgnoredPrefix) {
                  break;
               }
            }

            if (!containsIgnoredPrefix) {
               String level = props.remove(log + ".level");
               String useParent = props.remove(log + ".useParentHandlers");
               String handlers = props.remove(log + ".handlers");
               if (level == null) {
                  level = "INFO";
               }

               p.println(log + ".level = " + level);
               if (useParent != null) {
                  p.println(log + ".useParentHandlers = " + useParent);
               }

               if (handlers != null) {
                  p.println(log + ".handlers = " + handlers);
               }
            }
         }
      }

      p.println();
   }

   private static void writeRemaining(PrintWriter p, Map<String, String> props) {
      List<String> keys = new ArrayList<>(props.keySet());
      Collections.sort(keys);

      for (String key : keys) {
         p.println(key + " = " + props.remove(key));
      }

      p.println();
   }
}
