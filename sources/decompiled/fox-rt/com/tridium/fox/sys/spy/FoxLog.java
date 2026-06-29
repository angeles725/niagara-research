package com.tridium.fox.sys.spy;

import com.tridium.fox.message.FoxMessage;
import com.tridium.fox.session.Fox;
import com.tridium.fox.session.FoxSession;
import com.tridium.nre.platform.PlatformUtil;
import com.tridium.util.ThrowableUtil;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.nre.util.SecurityUtil;
import javax.baja.nre.util.SortUtil;
import javax.baja.spy.Spy;
import javax.baja.spy.SpyDir;
import javax.baja.spy.SpyWriter;
import javax.baja.sys.BRelTime;
import javax.baja.sys.Clock;
import javax.baja.sys.Sys;

public class FoxLog {
   static int nextWhoId = 0;
   static final int SERVER = 1;
   static final int ERROR = Level.SEVERE.intValue();
   static final int WARNING = Level.WARNING.intValue();
   static final int MESSAGE = Level.INFO.intValue();
   static final int TRACE = Level.FINE.intValue();
   static final int OPENED = 10;
   static final int CLOSED = 11;
   static final int REJECTED = 12;
   static DateFormat format = new SimpleDateFormat("HH:mm:ss dd-MMM-yy z");
   static Object lock = new Object();
   static final int max;
   static int size;
   static FoxLog.Rec head;
   static FoxLog.Rec tail;
   static HashMap<String, FoxLog.Who> whoHistory;
   Logger log;
   String logName;
   FoxSession session;

   public static FoxLog make(String logName) {
      return new FoxLog(Logger.getLogger(logName));
   }

   private FoxLog(Logger log) {
      this.log = log;
      this.logName = log.getName();
   }

   public Logger log() {
      return this.log;
   }

   public String getLogName() {
      return this.logName;
   }

   public boolean isTraceOn() {
      return this.log.isLoggable(Level.FINE);
   }

   public void error(String msg) {
      this.log(ERROR, msg, null);
   }

   public void error(String msg, Throwable ex) {
      this.log(ERROR, msg, ex);
   }

   public void warning(String msg) {
      this.log(WARNING, msg, null);
   }

   public void warning(String msg, Throwable ex) {
      this.log(WARNING, msg, ex);
   }

   public void message(String msg) {
      this.log(MESSAGE, msg, null);
   }

   public void message(String msg, Throwable ex) {
      this.log(MESSAGE, msg, ex);
   }

   public void trace(String msg) {
      this.log(TRACE, msg, null);
   }

   public void trace(String msg, Throwable ex) {
      this.log(TRACE, msg, ex);
   }

   public void log(int severity, String msg, Throwable ex) {
      Level level = this.severityToLevel(severity);
      if (this.log.isLoggable(level)) {
         this.log.log(level, msg, ex);
         add(new FoxLog.Rec(severity, this.session, this.logName, msg, ex));
      }
   }

   public void logRecOnly(int severity, String msg) {
      if (this.log.isLoggable(this.severityToLevel(severity))) {
         add(new FoxLog.Rec(severity, this.session, this.logName, msg, null));
      }
   }

   private Level severityToLevel(int severity) {
      if (severity == ERROR) {
         return Level.SEVERE;
      } else if (severity == WARNING) {
         return Level.WARNING;
      } else if (severity == MESSAGE) {
         return Level.INFO;
      } else {
         return severity == TRACE ? Level.FINE : Level.ALL;
      }
   }

   public FoxSession session() {
      return this.session;
   }

   public void setSession(FoxSession session) {
      this.session = session;
   }

   public void opened(FoxSession session) {
      this.session = session;
      String msg = "Opened: " + toString(session);
      if (!stationToStationConnection(session)) {
         this.log.info(msg);
      } else if (this.log.isLoggable(Level.FINE)) {
         this.log.fine(msg);
      }

      add(new FoxLog.Rec(10, session, this.logName, msg, null));
      String whoKey = toWho(session);
      synchronized (lock) {
         FoxLog.Who who = whoHistory.get(whoKey);
         if (who == null) {
            whoHistory.put(whoKey, who = new FoxLog.Who(whoKey));
         }

         who.opened(session);
      }
   }

   public void closed(FoxSession session, Throwable ex) {
      int type;
      String msg;
      if (this.session == session) {
         type = 11;
         msg = "Closed: " + toString(session);
      } else {
         type = 12;
         msg = "Rejected: " + toString(session);
      }

      if (!stationToStationConnection(session)) {
         this.log.info(msg);
      } else if (this.log.isLoggable(Level.FINE)) {
         this.log.fine(msg);
      }

      add(new FoxLog.Rec(type, session, this.logName, msg, ex));
      String whoKey = toWho(session);
      synchronized (lock) {
         FoxLog.Who who = whoHistory.get(whoKey);
         if (who == null) {
            whoHistory.put(whoKey, who = new FoxLog.Who(whoKey));
         }

         if (type == 11) {
            who.closed(session);
         } else {
            who.rejected(session);
         }
      }

      this.session = null;
   }

   public static String toWho(FoxSession session) {
      if (session == null) {
         return "null";
      } else {
         try {
            FoxMessage hello = session.getRemoteHello();
            String host;
            if (hello == null) {
               hello = new FoxMessage();

               try {
                  host = session.getSocket().getInetAddress().toString();
               } catch (NullPointerException var7) {
                  host = "unknown";
               }
            } else {
               host = hello.getString("hostName", "unknown");
            }

            String app = hello.getString("app.name", "unknown");
            String station = hello.getString("station.name", null);
            String user = hello.getString("user", null);
            StringBuilder s = new StringBuilder();
            s.append(app);
            if (station != null) {
               s.append(" [").append(station).append("]");
            } else if (user != null) {
               s.append(" [").append(user).append("]");
            }

            s.append(" @ ").append(host);
            return s.toString();
         } catch (Exception var8) {
            var8.printStackTrace();
            return "err";
         }
      }
   }

   public static String toString(FoxSession session) {
      if (session == null) {
         return "null";
      } else {
         StringBuilder s = new StringBuilder();
         s.append(SecurityUtil.calculateSessionIdHash(session.getId()));
         s.append(session.isServer() ? " <- " : " -> ");
         s.append(SecurityUtil.calculateSessionIdHash(session.getRemoteId()));
         s.append(" :: ");
         s.append(toWho(session));
         return s.toString();
      }
   }

   static String toTime(long ticks) {
      return ticks <= 0L ? "null" : format.format(new Date(Clock.millis() - Clock.ticks() + ticks));
   }

   static String toAge(long ticks) {
      return BRelTime.toString(Clock.ticks() - ticks);
   }

   static String toDuration(long ticks) {
      return BRelTime.toString(ticks);
   }

   static boolean stationToStationConnection(FoxSession session) {
      try {
         String appName = session.getRemoteHello().getString("app.name", "");
         return appName.equals("Station") && appName.equals(Fox.appName);
      } catch (Exception var2) {
         return false;
      }
   }

   static void header(SpyWriter out) {
      out.w("\n");
      out.w(Fox.appName).w(" ").w(Fox.appVersion);
      if (Sys.getStation() != null) {
         out.w(" [" + Sys.getStation().getStationName() + "]");
      }

      out.w(" | " + toTime(Clock.ticks()));
      out.w(" | Max Log Size: " + max);
      out.w(" | <a href='/fox/log'>Log Index</a>");
      out.w(" | <a href='/fox/log/all'>All Records [" + size + "]</a>");
      out.w("<hr>\n");
   }

   static FoxLog.Who findWho(int id) {
      for (FoxLog.Who who : whoHistory.values()) {
         if (who.id == id) {
            return who;
         }
      }

      return null;
   }

   static FoxLog.Rec findRec(int id) {
      for (FoxLog.Rec rec = head; rec != null; rec = rec.next) {
         if (rec.id == id) {
            return rec;
         }
      }

      return null;
   }

   static void add(FoxLog.Rec rec) {
      synchronized (lock) {
         while (size >= max) {
            head = head.next;
            size--;
         }

         if (head == null) {
            tail = rec;
            head = rec;
         } else {
            rec.id = tail.id + 1;
            tail.next = rec;
            tail = rec;
         }

         size++;
      }
   }

   static String typeToString(int type) {
      if (type == ERROR) {
         return "Sever";
      } else if (type == WARNING) {
         return "Warning";
      } else if (type == MESSAGE) {
         return "Info";
      } else if (type == TRACE) {
         return "Fine";
      } else {
         switch (type) {
            case 10:
               return "Opened";
            case 11:
               return "Closed";
            case 12:
               return "Rejected";
            default:
               return "?" + type + "?";
         }
      }
   }

   static {
      int m;
      try {
         m = AccessController.doPrivileged((PrivilegedAction<Integer>)(() -> {
            Integer logMax = AccessController.doPrivileged((PrivilegedAction<Integer>)(() -> Integer.getInteger("niagara.fox.logMax")));
            if (logMax != null) {
               return logMax;
            } else {
               return PlatformUtil.getPlatformProvider().isEmbedded() ? 100 : 1000;
            }
         }));
      } catch (Exception var4) {
         Exception e = var4;
         m = 1000;

         try {
            Logger.getLogger("fox").log(Level.SEVERE, "Error initializing FoxLog max size. Using default max of " + m, (Throwable)e);
         } catch (Exception var3) {
            System.out.println("Error initializing FoxLog max size. Using default max of " + m);
            var4.printStackTrace();
         }
      }

      max = m;
      whoHistory = new HashMap<>();
      add(new FoxLog.Rec(MESSAGE, null, "fox", "Booted", null));
   }

   static class Index extends SpyDir {
      public Spy find(String name) {
         String title;
         HashMap<String, Object> sessions;
         if (name.equals("all")) {
            title = "All Records";
            sessions = null;
         } else if (name.startsWith("byWho-")) {
            FoxLog.Who who = FoxLog.findWho(Integer.parseInt(name.substring(6)));
            title = "Records for " + who.key;
            sessions = who.sessionsToHashMap();
         } else {
            if (!name.startsWith("bySession-")) {
               throw new IllegalStateException();
            }

            String sessionId = name.substring(10);
            title = "Records for FoxSession " + sessionId;
            sessions = new HashMap<>();
            sessions.put(sessionId, this);
         }

         return new FoxLog.SpyRecs(title, sessions);
      }

      public void write(SpyWriter out) throws Exception {
         FoxLog.header(out);
         FoxLog.Who[] who = FoxLog.whoHistory.values().toArray(new FoxLog.Who[0]);
         SortUtil.sort(who);
         out.startTable(true);
         out.trTitle("Log of who has made fox connections [" + who.length + "]", 4);
         out.w("<tr><th>Session</th><th>Opened</th><th>Closed</th><th>Duration</th></tr>\n");

         for (int i = 0; i < who.length; i++) {
            FoxLog.Who w = who[i];
            out.unsafe().trTitle("<a href='byWho-" + w.id + "'>" + w, 4);

            for (int j = w.sessionIds.length - 1; j >= 0; j--) {
               String id = w.sessionIds[j];
               if (!id.isEmpty()) {
                  String remoteId = w.remoteIds[j];
                  long open = w.openTicks[j];
                  long close = w.closeTicks[j];
                  boolean isServer = (w.flags[j] & 1) != 0;
                  String sessionStr = id + (isServer ? " <- " : " -> ") + remoteId;
                  boolean isOpen = false;
                  String closeStr;
                  String duration;
                  if (close <= 0L) {
                     closeStr = "Open";
                     duration = FoxLog.toDuration(Clock.ticks() - open);
                     isOpen = true;
                  } else if (close == Long.MAX_VALUE) {
                     closeStr = "Rejected";
                     duration = "-";
                  } else {
                     closeStr = FoxLog.toTime(close);
                     duration = FoxLog.toDuration(close - open);
                  }

                  out.w("<tr")
                     .w(isOpen ? " bgcolor='#00ff00'" : "")
                     .w(">")
                     .td()
                     .a("bySession-" + id, sessionStr)
                     .endTd()
                     .td(FoxLog.toTime(open))
                     .td(closeStr)
                     .td(duration)
                     .w("</tr>\n");
               }
            }
         }

         out.endTable();
      }
   }

   static class Rec extends Spy {
      FoxLog.Rec next;
      int id;
      long ticks = Clock.ticks();
      String sessionId;
      int type;
      String logName;
      String msg;
      Throwable ex;

      Rec(int type, FoxSession session, String logName, String msg, Throwable ex) {
         this.type = type;
         this.sessionId = session == null ? "" : SecurityUtil.calculateSessionIdHash(session.getId());
         this.logName = logName;
         this.msg = msg;
         this.ex = ex;
      }

      public void write(SpyWriter out) throws Exception {
         FoxLog.header(out);
         out.startProps("Fox Log Record");
         out.prop("Id", "" + this.id);
         out.prop("Time", FoxLog.toTime(this.ticks));
         out.prop("Age", FoxLog.toAge(this.ticks));
         out.prop("Session", this.sessionId);
         out.prop("Type", FoxLog.typeToString(this.type));
         out.prop("LogName", this.logName);
         out.prop("Message", this.msg);
         out.endProps();
         if (this.ex != null) {
            out.w("<pre>");
            ThrowableUtil.dump(out, this.ex);
            out.w("</pre>");
         }
      }
   }

   static class SpyRecs extends SpyDir {
      String title;
      HashMap<String, Object> sessions;

      SpyRecs(String title, HashMap<String, Object> sessions) {
         this.title = title;
         this.sessions = sessions;
      }

      public Spy find(String name) {
         return FoxLog.findRec(Integer.parseInt(name));
      }

      public void write(SpyWriter out) throws Exception {
         FoxLog.header(out);
         out.startTable(true);
         out.trTitle(this.title, 6);
         out.w("<tr><th>Time</th><th>Age</th><th>Sid</th><th>Log</th><th>Message</th><th>Exception</th></tr>\n");

         for (FoxLog.Rec rec = FoxLog.head; rec != null; rec = rec.next) {
            if (this.include(rec)) {
               out.w("<tr");
               if (rec.type == FoxLog.ERROR) {
                  out.w(" bgcolor='#FFAA26'");
               } else if (rec.type == FoxLog.WARNING) {
                  out.w(" bgcolor='#FFFF00'");
               }

               out.w(">")
                  .td()
                  .a(String.valueOf(rec.id), FoxLog.toTime(rec.ticks))
                  .endTd()
                  .td(FoxLog.toAge(rec.ticks))
                  .td("" + rec.sessionId)
                  .td(rec.logName)
                  .w("<td align='left' nowrap='true'>")
                  .safe(rec.msg)
                  .w("</td>")
                  .td(rec.ex == null ? "" : rec.ex.toString());
               out.w("</tr>\n");
            }
         }

         out.endTable();
      }

      boolean include(FoxLog.Rec rec) {
         return this.sessions == null ? true : this.sessions.get(rec.sessionId) != null;
      }
   }

   static class Who {
      int id;
      String key;
      String[] sessionIds;
      String[] remoteIds;
      long[] openTicks;
      long[] closeTicks;
      byte[] flags;

      Who(String key) {
         int len = 20;
         this.id = FoxLog.nextWhoId++;
         this.key = key;
         this.sessionIds = new String[20];
         this.remoteIds = new String[20];
         this.openTicks = new long[20];
         this.closeTicks = new long[20];
         this.flags = new byte[20];

         for (int i = 0; i < 20; i++) {
            this.sessionIds[i] = "";
         }
      }

      void opened(FoxSession session) {
         System.arraycopy(this.sessionIds, 0, this.sessionIds, 1, this.sessionIds.length - 1);
         System.arraycopy(this.remoteIds, 0, this.remoteIds, 1, this.remoteIds.length - 1);
         System.arraycopy(this.openTicks, 0, this.openTicks, 1, this.openTicks.length - 1);
         System.arraycopy(this.closeTicks, 0, this.closeTicks, 1, this.closeTicks.length - 1);
         System.arraycopy(this.flags, 0, this.flags, 1, this.flags.length - 1);
         this.sessionIds[0] = session.getId();
         this.remoteIds[0] = session.getRemoteId();
         this.openTicks[0] = Clock.ticks();
         this.closeTicks[0] = -1L;
         this.flags[0] = (byte)(session.isServer() ? 1 : 0);
      }

      void closed(FoxSession session) {
         String id = session.getId();

         for (int i = 0; i < this.sessionIds.length; i++) {
            if (this.sessionIds[i].equals(id)) {
               this.closeTicks[i] = Clock.ticks();
               break;
            }
         }
      }

      void rejected(FoxSession session) {
         this.opened(session);
         this.closeTicks[0] = Long.MAX_VALUE;
      }

      HashMap<String, Object> sessionsToHashMap() {
         HashMap<String, Object> map = new HashMap<>();

         for (int i = 0; i < this.sessionIds.length; i++) {
            String id = this.sessionIds[i];
            if (!id.isEmpty()) {
               map.put(id, this);
            }
         }

         return map;
      }

      @Override
      public String toString() {
         return this.key;
      }
   }
}
