package com.tridium.fox.sys.spy;

import com.tridium.fox.session.Fox;
import com.tridium.fox.session.FoxConnection;
import com.tridium.fox.session.FoxSession;
import com.tridium.fox.sys.BFoxClientConnection;
import javax.baja.spy.Spy;
import javax.baja.spy.SpyDir;
import javax.baja.spy.SpyWriter;
import javax.baja.sys.BObject;

public class FoxIndexSpy extends SpyDir {
   public Spy find(String name) {
      if (name.equals("log")) {
         return new FoxLog.Index();
      } else {
         String id = name.substring("session-".length());
         FoxSession session = Fox.getSession(id);
         return new FoxSessionSpy(session);
      }
   }

   public void write(SpyWriter out) {
      FoxSession[] sessions = Fox.getSessions();
      out.startProps();
      out.trTitle("Fox", 2);
      out.prop("requestTimeout", Fox.requestTimeout);
      out.prop("keepAliveInterval", Fox.keepAliveInterval);
      out.prop("soTimeout", Fox.soTimeout);
      out.prop("tcpNoDelay", Fox.tcpNoDelay);
      out.prop("failsafeTimeouts", Fox.failsafeTimeouts);
      out.prop("maxServerSessions", Fox.maxServerSessions);
      out.prop("maxQueueSize", Fox.maxQueueSize);
      out.prop("circuitChunkSize", Fox.circuitChunkSize);
      out.prop("circuitMaxReceiveBuffer", Fox.circuitMaxReceiveBuffer);
      out.prop("hostName", Fox.hostName);
      out.prop("hostAddress", Fox.hostAddress);
      out.prop("app", Fox.appName + " " + Fox.appVersion);
      out.prop("vm", Fox.vmName + " " + Fox.vmVersion);
      out.prop("os", Fox.osName + " " + Fox.osVersion);
      out.prop("exceptionTranslator", Fox.exceptionTranslator.getClass().getName());
      out.prop("engageLinger", "" + BFoxClientConnection.engageLinger);
      out.prop("preAuthFrameSizeLimit", "" + Fox.getPreAuthFrameSizeLimit());
      out.prop("invalidFrameCount", "" + Fox.getInvalidFrameCount());
      out.trTitle("Fox Log", 2);
      out.propNameLink("log", "Fox Log Index", "size=" + FoxLog.size);
      out.trTitle("Client Sessions", 2);

      for (int i = 0; i < sessions.length; i++) {
         if (!sessions[i].isServer()) {
            this.prop(out, sessions[i]);
         }
      }

      out.trTitle("Server Sessions [cached count=" + Fox.getServerSessionCount() + "]", 2);

      for (int ix = 0; ix < sessions.length; ix++) {
         if (sessions[ix].isServer()) {
            this.prop(out, sessions[ix]);
         }
      }

      out.trTitle("Fox Thread Pool Worker", 2);
      out.endProps();
      BFoxClientConnection.spyThreadPoolWorker(out);
   }

   private void prop(SpyWriter out, FoxSession s) {
      String n = "session-" + s.getId();
      FoxConnection conn = s.conn();
      int fwConnType = -1;
      if (conn instanceof BObject) {
         Object val = ((BObject)conn).fw(803, null, null, null, null);
         if (val instanceof Integer) {
            fwConnType = (Integer)val;
         }
      }

      String displayName = fwConnType != -1 ? n + "-fw" : n;
      out.propNameLink(n, displayName, s);
   }
}
