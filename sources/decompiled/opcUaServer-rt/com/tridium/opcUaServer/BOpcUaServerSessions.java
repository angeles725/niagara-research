package com.tridium.opcUaServer;

import com.prosysopc.ua.server.Session;
import com.prosysopc.ua.server.SessionManager;
import com.prosysopc.ua.server.UaServer;
import java.util.Collection;
import java.util.logging.Logger;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Action;
import javax.baja.sys.BComplex;
import javax.baja.sys.BValue;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.util.BFolder;
import javax.baja.util.IFuture;
import javax.baja.util.Invocation;

@NiagaraType
@NiagaraAction(
   name = "update",
   flags = 16
)
public final class BOpcUaServerSessions extends BFolder {
   public static final Action update = newAction(16, null);
   public static final Type TYPE = Sys.loadType(BOpcUaServerSessions.class);
   private BOpcUaServer network = null;
   private static final Logger logger = Logger.getLogger("opcUaServer.opcUaServerSession");

   public void update() {
      this.invoke(update, null, null);
   }

   public Type getType() {
      return TYPE;
   }

   public void started() throws Exception {
      BComplex complex = this.getParent();
      if (complex instanceof BOpcUaServer) {
         this.network = (BOpcUaServer)complex;
      }
   }

   public boolean removeSession(Session session) {
      BOpcUaServerSession[] sessions = (BOpcUaServerSession[])this.getChildren(BOpcUaServerSession.class);

      for (BOpcUaServerSession thisSession : sessions) {
         if (thisSession.getSessionId().equals(session.getSessionId().toString())) {
            this.remove(thisSession);
            thisSession.invalidate();
            return true;
         }
      }

      return false;
   }

   public boolean addSession(Session session) {
      try {
         Property p = this.add("s?", BOpcUaServerSession.make(session), 3);
         return p != null;
      } catch (Exception var3) {
         logger.fine("Exception while adding the session : " + var3);
         return false;
      }
   }

   public IFuture post(Action action, BValue argument, Context cx) {
      return this.network != null ? this.network.postAsync(new Invocation(this, action, argument, cx)) : super.post(action, argument, cx);
   }

   public void subscribed() {
      if (this.network != null && this.network.getStatus().isValid()) {
         this.update();
      }
   }

   public void doUpdate() {
      if (this.network != null && this.network.getStatus().isValid() && this.network.server != null) {
         UaServer uaServer = this.network.server;
         SessionManager sessionManager = uaServer.getSessionManager();
         Collection<Session> sessions = sessionManager.getSessions();
         this.removeAll();

         for (Session session : sessions) {
            BOpcUaServerSession var6 = (BOpcUaServerSession)this.get(this.add("s?", BOpcUaServerSession.make(session), 3));
         }
      }
   }

   public String toString(Context cx) {
      BOpcUaServerSession[] sessions = (BOpcUaServerSession[])this.getChildren(BOpcUaServerSession.class);
      return sessions.length == 0 ? "No active sessions" : sessions.length + " active sessions";
   }
}
