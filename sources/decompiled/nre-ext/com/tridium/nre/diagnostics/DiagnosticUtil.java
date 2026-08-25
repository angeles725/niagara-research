package com.tridium.nre.diagnostics;

import com.tridium.nre.platform.IPlatformProvider;
import com.tridium.nre.platform.PlatformUtil;
import com.tridium.nre.security.NiagaraBasicPermission;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.security.AccessController;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class DiagnosticUtil {
   static final Map<String, DiagnosticStats> allStats = Collections.synchronizedMap(new HashMap<>());
   private static long diagnosticStart = System.currentTimeMillis();
   private static long diagnosticStop = 0L;
   private static HashSet<String> ignoreFunctionList = new HashSet<>();
   private static ThreadMXBean threadBean;
   private static final Logger LOG = Logger.getLogger("diagnostics");

   public static void checkDiagnosticSecurityManager() {
      boolean enabled = AccessController.doPrivileged(() -> Boolean.getBoolean("niagara.security.manager.diagnostic"));
      if (enabled) {
         enableDiagnosticSecurityManager();
      }
   }

   public static void enableDiagnosticSecurityManager() {
      NiagaraBasicPermission permission = new NiagaraBasicPermission("DIAGNOSTIC_SECURITY_MANAGER");
      SecurityManager sm = System.getSecurityManager();
      if (sm != null) {
         sm.checkPermission(permission);
      }

      if (!(sm instanceof DiagnosticSecurityManager) && sm != null) {
         LOG.warning("Security Manager diagnostic mode enabled.");
         new DiagnosticStats("none");
         new DiagnosticStats.KeyStat("none", "none");
         new DiagnosticStats.TimedStackTrace(0L, null);
         nanoTime();
         AccessController.doPrivileged(() -> {
            System.setSecurityManager(new DiagnosticSecurityManager());
            return null;
         });
      }
   }

   public static long startIfLoggable(String functionName) {
      return functionName != null && Logger.getLogger("diagnostics." + functionName).isLoggable(Level.FINE) ? nanoTime() : -1L;
   }

   public static void diagnose(String functionName, Runnable r) {
      diagnose(functionName, null, r);
   }

   public static void diagnose(String functionName, Object objectKey, Runnable r) {
      long start = nanoTime();

      try {
         r.run();
      } finally {
         complete(start, functionName, objectKey);
      }
   }

   public static <T> T diagnose(String functionName, Supplier<T> supplier) {
      return diagnose(functionName, null, supplier);
   }

   public static <T> T diagnose(String functionName, Object objectKey, Supplier<T> supplier) {
      long start = nanoTime();

      try {
         return supplier.get();
      } finally {
         complete(start, functionName, objectKey);
      }
   }

   public static void diagnoseIfLoggable(String functionName, Runnable r) {
      diagnoseIfLoggable(functionName, null, r);
   }

   public static void diagnoseIfLoggable(String functionName, Object objectKey, Runnable r) {
      long start = startIfLoggable(functionName);

      try {
         r.run();
      } finally {
         complete(start, functionName, objectKey);
      }
   }

   public static <T> T diagnoseIfLoggable(String functionName, Supplier<T> supplier) {
      return diagnoseIfLoggable(functionName, null, supplier);
   }

   public static <T> T diagnoseIfLoggable(String functionName, Object objectKey, Supplier<T> supplier) {
      long start = startIfLoggable(functionName);

      try {
         return supplier.get();
      } finally {
         complete(start, functionName, objectKey);
      }
   }

   public static void complete(long start, String functionName) {
      complete(start, functionName, null);
   }

   public static void complete(long start, String functionName, Object objectKey) {
      if (diagnosticStop == 0L && start != -1L) {
         long duration = nanoTime() - start;
         completeDuration(duration, functionName, objectKey);
      }
   }

   public static void completeDuration(long duration, String functionName, Object objectKey) {
      if (diagnosticStop == 0L) {
         String key = "";
         if (objectKey != null) {
            key = objectKey.toString();
         }

         if (!ignoreFunctionList.contains(functionName)) {
            DiagnosticStats stats = allStats.computeIfAbsent(functionName, value -> new DiagnosticStats(functionName));
            stats.complete(duration, key);
         }
      }
   }

   public static void ignoreFunction(String functionName) {
      ignoreFunctionList.add(functionName);
      resetFunction(functionName);
   }

   public static void resetFunction(String functionName) {
      allStats.remove(functionName);
   }

   public static void ignoreKey(String functionName, String key) {
      DiagnosticStats stats = allStats.computeIfAbsent(functionName, value -> new DiagnosticStats(functionName));
      stats.ignoreKey(key);
   }

   public static void resetKey(String functionName, String key) {
      DiagnosticStats stats = allStats.computeIfAbsent(functionName, value -> new DiagnosticStats(functionName));
      stats.resetKey(key);
   }

   public static Map<String, DiagnosticStats> getAllStats() {
      return allStats;
   }

   public static long getDiagnosticDuration() {
      return diagnosticStop == 0L ? System.currentTimeMillis() - diagnosticStart : diagnosticStop - diagnosticStart;
   }

   public static long getDiagnosticStart() {
      return diagnosticStart;
   }

   public static long getDiagnosticStop() {
      return diagnosticStop;
   }

   public static void reset() {
      allStats.clear();
      diagnosticStart = System.currentTimeMillis();
      diagnosticStop = 0L;
   }

   public static long nanoTime() {
      if (threadBean == null) {
         if (DiagnosticStats.INCLUDE_WAIT_TIME) {
            return DiagnosticUtil.PlatformProviderHolder.PLATFORM_PROVIDER_INSTANCE.getNanoCount();
         }

         threadBean = ManagementFactory.getThreadMXBean();
         if (!threadBean.isThreadCpuTimeEnabled()) {
            DiagnosticStats.INCLUDE_WAIT_TIME = true;
            threadBean = null;
            LOG.warning("JVM implementation doesn't support thread cpu time");
            return DiagnosticUtil.PlatformProviderHolder.PLATFORM_PROVIDER_INSTANCE.getNanoCount();
         }
      }

      return threadBean.getCurrentThreadCpuTime();
   }

   public static void pause() {
      diagnosticStop = System.currentTimeMillis();
   }

   private static final class PlatformProviderHolder {
      private static final IPlatformProvider PLATFORM_PROVIDER_INSTANCE = AccessController.doPrivileged(PlatformUtil::getPlatformProvider);
   }
}
