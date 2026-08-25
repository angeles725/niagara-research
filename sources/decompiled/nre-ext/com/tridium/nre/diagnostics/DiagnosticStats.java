package com.tridium.nre.diagnostics;

import java.security.AccessController;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

public class DiagnosticStats {
   private static DiagnosticStats.IDiagnosticListener listener;
   private double lastDuration = 0.0;
   private double totalTime = 0.0;
   private long functionCalls = 0L;
   private String functionName;
   private Map<String, DiagnosticStats.KeyStat> keyUsage;
   public static int MAX_TIMED_STACKS;
   public static int MAX_KEY_USAGES;
   public static boolean INCLUDE_WAIT_TIME;

   public DiagnosticStats(String functionName) {
      this.functionName = functionName;
      if (MAX_KEY_USAGES > 0) {
         this.keyUsage = Collections.synchronizedMap(new LinkedHashMap<String, DiagnosticStats.KeyStat>() {
            @Override
            protected boolean removeEldestEntry(Entry<String, DiagnosticStats.KeyStat> eldest) {
               return this.size() > DiagnosticStats.MAX_KEY_USAGES;
            }
         });
      }
   }

   public void complete(long duration, String keyString) {
      DiagnosticStats.KeyStat keyStat = null;
      if (this.keyUsage != null) {
         keyStat = this.keyUsage.computeIfAbsent(keyString, value -> new DiagnosticStats.KeyStat(keyString, this.functionName));
         if (keyStat.ignore) {
            return;
         }
      }

      synchronized (DiagnosticUtil.allStats) {
         this.functionCalls++;
         this.totalTime += duration;
      }

      if (keyStat != null) {
         keyStat.increment(duration);
      }

      this.lastDuration = duration;
      if (listener != null) {
         listener.changed(keyStat, this.functionName);
      }
   }

   public void ignoreKey(String keyString) {
      if (this.keyUsage != null) {
         DiagnosticStats.KeyStat keyStat = this.keyUsage.computeIfAbsent(keyString, value -> new DiagnosticStats.KeyStat(keyString, this.functionName));
         keyStat.ignore = true;
         if (keyStat.stacks != null) {
            keyStat.stacks.clear();
         }
      }
   }

   public void resetKey(String keyString) {
      if (this.keyUsage != null) {
         DiagnosticStats.KeyStat keyStat = this.keyUsage.get(keyString);
         if (keyStat != null && keyStat.stacks != null) {
            keyStat.stacks.clear();
         }
      }
   }

   public double getTotalTime() {
      return this.totalTime;
   }

   public double getLastDuration() {
      return this.lastDuration;
   }

   public long getFunctionCalls() {
      return this.functionCalls;
   }

   public String getFunctionName() {
      return this.functionName;
   }

   public Map<String, DiagnosticStats.KeyStat> getKeyUsage() {
      return this.keyUsage;
   }

   public static void setDiagnosticListener(DiagnosticStats.IDiagnosticListener l) {
      listener = l;
   }

   static {
      AccessController.doPrivileged(() -> {
         MAX_TIMED_STACKS = AccessController.doPrivileged(() -> Integer.getInteger("niagara.diagnostics.stacks", 0));
         MAX_KEY_USAGES = AccessController.doPrivileged(() -> Integer.getInteger("niagara.diagnostics.keys", 0));
         INCLUDE_WAIT_TIME = AccessController.doPrivileged(() -> Boolean.getBoolean("niagara.diagnostics.includeWaitTime"));
         return null;
      });
   }

   public static class DiagnosticException extends Exception {
   }

   public interface IDiagnosticListener {
      void changed(DiagnosticStats.KeyStat var1, String var2);
   }

   public static class KeyStat {
      private String key;
      private String functionName;
      private long callCount = 0L;
      private long totalTime = 0L;
      private long lastDuration = 0L;
      private boolean ignore = false;
      private Map<Long, DiagnosticStats.TimedStackTrace> stacks;

      public KeyStat(String key, String functionName) {
         this.key = key;
         this.functionName = functionName;
         if (DiagnosticStats.MAX_TIMED_STACKS > 0) {
            this.stacks = Collections.synchronizedMap(new LinkedHashMap<Long, DiagnosticStats.TimedStackTrace>() {
               @Override
               protected boolean removeEldestEntry(Entry<Long, DiagnosticStats.TimedStackTrace> eldest) {
                  return this.size() > DiagnosticStats.MAX_TIMED_STACKS;
               }
            });
         }
      }

      public void increment(long time) {
         synchronized (DiagnosticUtil.allStats) {
            this.callCount++;
            this.totalTime += time;
         }

         if (this.stacks != null) {
            this.stacks.put(this.callCount, new DiagnosticStats.TimedStackTrace(time, new DiagnosticStats.DiagnosticException()));
         }

         this.lastDuration = time;
      }

      public String getKey() {
         return this.key;
      }

      public String getFunctionName() {
         return this.functionName;
      }

      public long getCallCount() {
         return this.callCount;
      }

      public long getLastDuration() {
         return this.lastDuration;
      }

      public long getTotalTime() {
         return this.totalTime;
      }

      public boolean getIgnore() {
         return this.ignore;
      }

      public void setIgnore(boolean ignore) {
         this.ignore = ignore;
      }

      public Map<Long, DiagnosticStats.TimedStackTrace> getStacks() {
         return this.stacks;
      }
   }

   public static class TimedStackTrace {
      private long currentTimeMillis;
      private long timePerCall;
      private Exception stack;

      TimedStackTrace(long timePerCall, Exception stack) {
         this.timePerCall = timePerCall;
         this.stack = stack;
         this.currentTimeMillis = System.currentTimeMillis();
      }

      public long getCurrentTimeMillis() {
         return this.currentTimeMillis;
      }

      public long getTimePerCall() {
         return this.timePerCall;
      }

      public Exception getStack() {
         return this.stack;
      }
   }
}
