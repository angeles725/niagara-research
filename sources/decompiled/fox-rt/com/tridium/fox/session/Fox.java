package com.tridium.fox.session;

import com.tridium.fox.message.FoxMessage;
import com.tridium.session.SessionManager;
import com.tridium.sys.Nre;
import java.io.IOException;
import java.net.Socket;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;
import javax.baja.security.BICredentials;
import javax.baja.sys.Sys;

public class Fox {
   public static final int BASIC_AUTHENTICATION = 0;
   public static final int DIGEST_AUTHENTICATION = 1;
   public static final int TRANSACTIONAL_AUTHENTICATION = 2;
   public static String MULTICAST_ADDRESS = "224.0.1.84";
   public static String IPV6_MULTICAST_ADDRESS = "FF02::137";
   public static final int MULTICAST_PORT = 1911;
   public static final String UNKNOWN_STRING = "unknown";
   public static long sessionCloseTimeout = 10000L;
   public static boolean ipv4Enabled = true;
   public static boolean ipv6Enabled = false;
   public static boolean multicastEnabled = true;
   public static int requestTimeout = 60000;
   public static int keepAliveInterval = 5000;
   public static int soTimeout = 60000;
   public static boolean tcpNoDelay = true;
   public static boolean failsafeTimeouts = true;
   public static int maxServerSessions = 100;
   public static int maxQueueSize = 1000;
   public static int circuitChunkSize = 4096;
   public static int circuitMaxReceiveBuffer = 102400;
   private static final long DEFAULT_PRE_AUTH_FRAME_SIZE_LIMIT = 65536L;
   private static long preAuthFrameSizeLimit = 65536L;
   public static int multicastTimeToLive = 4;
   public static boolean traceWriteFrame = false;
   public static boolean traceReadFrame = false;
   public static boolean traceSessionStates = false;
   public static boolean traceMulticast = false;
   public static String appName = "unknown";
   public static String appVersion = "unknown";
   public static String vmName = AccessController.doPrivileged((PrivilegedAction<String>)(() -> System.getProperty("java.vm.name", "unknown")));
   public static String vmVersion = AccessController.doPrivileged((PrivilegedAction<String>)(() -> System.getProperty("java.vm.version", "unknown")));
   public static String osName = AccessController.doPrivileged((PrivilegedAction<String>)(() -> System.getProperty("os.name", "unknown")));
   public static String osVersion = AccessController.doPrivileged((PrivilegedAction<String>)(() -> System.getProperty("os.version", "unknown")));
   public static String hostAddress = "";
   public static String hostName;
   public static Fox.ExceptionTranslator exceptionTranslator;
   public static SimpleDateFormat timestampFormatter;
   public static Fox.Clock clock;
   public static final ThreadGroup threadGroup;
   private static final int MAX_INT_LEN;
   private static int serverSessionCount;
   private static volatile int invalidFrameCount;

   public static int getInvalidFrameCount() {
      return invalidFrameCount;
   }

   public static void incrementInvalidFrameCount() {
      invalidFrameCount++;
   }

   public static long getPreAuthFrameSizeLimit() {
      return preAuthFrameSizeLimit;
   }

   public static void setPreAuthFrameSizeLimit(long preAuthFrameSizeLimit) {
      Fox.preAuthFrameSizeLimit = preAuthFrameSizeLimit;
   }

   private static boolean getBoolean(String key, boolean def) {
      String v = AccessController.doPrivileged((PrivilegedAction<String>)(() -> System.getProperty(key)));
      return v == null ? def : v.equals("true");
   }

   private static int getInt(String key, int def) {
      String v = AccessController.doPrivileged((PrivilegedAction<String>)(() -> System.getProperty(key)));
      return v == null ? def : Integer.parseInt(v);
   }

   private static Object getInstance(String key, Object def) {
      try {
         String v = AccessController.doPrivileged((PrivilegedAction<String>)(() -> System.getProperty(key)));
         return v == null ? def : Fox.class.getClassLoader().loadClass(v).getDeclaredConstructor().newInstance();
      } catch (Throwable var3) {
         var3.printStackTrace();
         return def;
      }
   }

   private static String getString(String key, String def) {
      return AccessController.doPrivileged((PrivilegedAction<String>)(() -> System.getProperty(key, def)));
   }

   public static FoxSession open(FoxConnection conn, Socket socket, String user, String password) throws Exception {
      return open(conn, socket, user, password, FoxSession.nullListeners);
   }

   public static FoxSession open(FoxConnection conn, Socket socket, String user, String password, FoxSession.IFoxSessionListener[] listeners) throws Exception {
      return Tuner.openClient(conn, socket, user, password, listeners);
   }

   public static FoxSession open(FoxConnection conn, Socket socket, BICredentials cred, FoxSession.IFoxSessionListener[] listeners) throws Exception {
      return Tuner.openClient(conn, socket, cred, listeners);
   }

   public static FoxSession getSession(String id) {
      FoxSession session = (FoxSession)SessionManager.getNiagaraSession(id, FoxSession.class);
      if (session == null && id.length() <= MAX_INT_LEN) {
         try {
            int intId = Integer.parseInt(id);

            for (FoxSession foxSession : SessionManager.getNiagaraSessions(FoxSession.class)) {
               if (foxSession.isLegacyConnection() && foxSession.getLegacyId() == intId) {
                  return foxSession;
               }
            }
         } catch (NumberFormatException var6) {
         }
      }

      return session;
   }

   public static int getServerSessionCount() {
      return SessionManager.countNiagaraSessions(FoxSession.class, FoxSession::isServer);
   }

   public static FoxSession[] getSessions() {
      Set<FoxSession> foxSessions = SessionManager.getNiagaraSessions(FoxSession.class);
      return foxSessions.toArray(new FoxSession[0]);
   }

   static void register(FoxSession session) {
      SessionManager.addSession(session);
   }

   static void unregister(FoxSession session) {
      session.invalidate();
   }

   static void debugSessions() {
      SessionManager.printSessions();
   }

   static {
      try {
         hostAddress = Sys.getLocalHost(null).getHostAddress();
      } catch (Exception var4) {
      }

      hostName = "";

      try {
         hostName = Sys.getHostName();
      } catch (Exception var3) {
      }

      exceptionTranslator = new Fox.ExceptionTranslator();

      try {
         clock = new Fox.DefaultClock();
      } catch (Throwable var2) {
         var2.printStackTrace();
      }

      threadGroup = AccessController.doPrivileged(new PrivilegedAction<ThreadGroup>() {
         public ThreadGroup run() {
            return new ThreadGroup(Nre.findMainThreadGroup(Thread.currentThread().getThreadGroup()), "fox");
         }
      });

      try {
         soTimeout = getInt("niagara.fox.soTimeout", soTimeout);
         tcpNoDelay = getBoolean("niagara.fox.tcpNoDelay", tcpNoDelay);
         keepAliveInterval = getInt("niagara.fox.keepAliveInterval", keepAliveInterval);
         requestTimeout = getInt("niagara.fox.requestTimeout", requestTimeout);
         maxServerSessions = getInt("niagara.fox.maxServerSessions", maxServerSessions);
         maxQueueSize = getInt("niagara.fox.maxQueueSize", maxQueueSize);
         circuitChunkSize = getInt("niagara.fox.circuitChunkSize", circuitChunkSize);
         circuitMaxReceiveBuffer = getInt("niagara.fox.circuitMaxReceiveBuffer", circuitMaxReceiveBuffer);
         failsafeTimeouts = getBoolean("niagara.fox.failsafeTimeouts", failsafeTimeouts);
         multicastTimeToLive = getInt("niagara.fox.multicastTimeToLive", multicastTimeToLive);
         exceptionTranslator = (Fox.ExceptionTranslator)getInstance("niagara.fox.exceptionTranslator", exceptionTranslator);
         traceWriteFrame = getBoolean("niagara.fox.traceWriteFrame", traceWriteFrame);
         traceReadFrame = getBoolean("niagara.fox.traceReadFrame", traceReadFrame);
         traceSessionStates = getBoolean("niagara.fox.traceSessionStates", traceSessionStates);
         traceMulticast = getBoolean("niagara.fox.traceMulticast", traceMulticast);
         ipv6Enabled = getBoolean("niagara.ipv6Enabled", ipv6Enabled);
         multicastEnabled = getBoolean("niagara.fox.multicastEnabled", multicastEnabled);
         sessionCloseTimeout = AccessController.doPrivileged(
            (PrivilegedAction<Long>)(() -> Long.getLong("niagara.fox.sessionCloseTimeout", sessionCloseTimeout))
         );
         preAuthFrameSizeLimit = AccessController.doPrivileged(
            (PrivilegedAction<Long>)(() -> Long.getLong("niagara.fox.preAuthFrameSizeLimit", preAuthFrameSizeLimit))
         );
         timestampFormatter = new SimpleDateFormat(getString("niagara.fox.errorDateFormatString", "HH:mm:ss dd-MMM-yy z"));
      } catch (Throwable var1) {
         var1.printStackTrace();
      }

      Fox.FailsafeTimeoutThread thread = new Fox.FailsafeTimeoutThread();
      thread.setDaemon(true);
      thread.start();
      MAX_INT_LEN = Integer.toString(Integer.MAX_VALUE).length();
   }

   public abstract static class Clock {
      public abstract long ticks();
   }

   private static class DefaultClock extends Fox.Clock {
      private DefaultClock() {
      }

      @Override
      public long ticks() {
         return javax.baja.sys.Clock.ticks();
      }
   }

   public static class ExceptionTranslator {
      public FoxMessage exceptionToMessage(Throwable e) {
         FoxMessage msg = new FoxMessage();
         msg.add("exception", e.toString());
         return msg;
      }

      public Exception messageToException(FoxMessage msg) throws IOException {
         return new ServerException(msg.getString("exception"));
      }
   }

   static class FailsafeTimeoutThread extends Thread {
      FailsafeTimeoutThread() {
         super(Fox.threadGroup, "Fox:FailsafeTimeout");
      }

      @Override
      public void run() {
         while (true) {
            try {
               Thread.sleep(10000L);
               if (Fox.failsafeTimeouts) {
                  FoxSession[] sessions = Fox.getSessions();

                  for (int i = 0; i < sessions.length; i++) {
                     FoxSession session = sessions[i];
                     long readLag = Fox.clock.ticks() - session.getLastReadTicks();
                     long writeLag = Fox.clock.ticks() - session.getLastWriteTicks();
                     if (readLag > Fox.soTimeout) {
                        IOException ex = new IOException("Failsafe timeout on read " + readLag + "ms > " + Fox.soTimeout + "ms");
                        System.out.println("SEVERE [" + Fox.timestampFormatter.format(new Date()) + "][fox] " + ex.getMessage());
                        session.close(ex);
                     } else if (writeLag > Fox.soTimeout) {
                        IOException ex = new IOException("Failsafe timeout on write " + writeLag + "ms > " + Fox.soTimeout + "ms");
                        System.out.println("SEVERE [" + Fox.timestampFormatter.format(new Date()) + "][fox] " + ex.getMessage());
                        session.close(ex);
                     }
                  }
               }
            } catch (Exception var9) {
               System.out.println("SEVERE [" + Fox.timestampFormatter.format(new Date()) + "][fox] Exception occurred during FailsafeTimeout check");
               var9.printStackTrace();
            }
         }
      }
   }
}
