package com.tridium.fox.sys;

import com.tridium.fox.session.FoxSession;
import com.tridium.util.ContextThread;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.security.AuditEvent;
import javax.baja.security.Auditor;
import javax.baja.sys.Action;
import javax.baja.sys.BAbsTime;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.user.BUser;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "lastLoginTime",
      type = "BAbsTime",
      defaultValue = "BAbsTime.NULL",
      flags = 3
   ), @NiagaraProperty(
      name = "lastLoginAddress",
      type = "String",
      defaultValue = "-",
      flags = 3
   ), @NiagaraProperty(
      name = "lastLoginUsername",
      type = "String",
      defaultValue = "-",
      flags = 3
   ), @NiagaraProperty(
      name = "lastLoginApp",
      type = "String",
      defaultValue = "-",
      flags = 3
   )})
@NiagaraAction(
   name = "forceDisconnect"
)
public class BFoxServerConnection extends BFoxConnection {
   public static final Property lastLoginTime = newProperty(3, BAbsTime.NULL, null);
   public static final Property lastLoginAddress = newProperty(3, "-", null);
   public static final Property lastLoginUsername = newProperty(3, "-", null);
   public static final Property lastLoginApp = newProperty(3, "-", null);
   public static final Action forceDisconnect = newAction(0, null);
   public static final Type TYPE = Sys.loadType(BFoxServerConnection.class);
   private boolean persistent = false;

   public BAbsTime getLastLoginTime() {
      return (BAbsTime)this.get(lastLoginTime);
   }

   public void setLastLoginTime(BAbsTime v) {
      this.set(lastLoginTime, v, null);
   }

   public String getLastLoginAddress() {
      return this.getString(lastLoginAddress);
   }

   public void setLastLoginAddress(String v) {
      this.setString(lastLoginAddress, v, null);
   }

   public String getLastLoginUsername() {
      return this.getString(lastLoginUsername);
   }

   public void setLastLoginUsername(String v) {
      this.setString(lastLoginUsername, v, null);
   }

   public String getLastLoginApp() {
      return this.getString(lastLoginApp);
   }

   public void setLastLoginApp(String v) {
      this.setString(lastLoginApp, v, null);
   }

   public void forceDisconnect() {
      this.invoke(forceDisconnect, null, null);
   }

   @Override
   public Type getType() {
      return TYPE;
   }

   public void stopped() throws Exception {
      if (this.session() != null) {
         this.session().close(null);
      }

      if (Sys.getStation() != null && Sys.getStation().isRunning()) {
         this.getConnectionTarget(NiagaraStation.class).ifPresent(station -> station.serverConnectionStopped(this));
      }
   }

   public final BUser getUser() {
      FoxSession session = this.session();
      return session != null ? session.getUser() : null;
   }

   public final Context getSessionContext() {
      FoxSession session = this.session();
      return session != null ? session.getSessionContext() : null;
   }

   @Override
   public void sessionOpened(FoxSession session) {
      if (this.session() != null) {
         this.session().close(new Exception("Reconnect while still server still connected"));
      }

      super.sessionOpened(session);
      this.getConnectionTarget(NiagaraStation.class).ifPresent(NiagaraStation::serverOpened);
   }

   @Override
   public void sessionClosed(FoxSession session, Throwable cause) {
      BFoxService foxService = (BFoxService)Sys.getService(BFoxService.TYPE);

      try {
         try {
            Auditor auditor = Sys.getAuditor();
            if (auditor != null && session.getUser() != null) {
               AuditEvent auditEvent = session.makeAuditEvent("Logout", session.getUser());
               if (auditEvent != null) {
                  auditor.audit(auditEvent);
               }
            }
         } catch (Throwable var9) {
            var9.printStackTrace();
         }

         if (this.session() == session) {
            super.sessionClosed(session, cause);
         } else {
            this.log.trace("Closing session [" + session + " doesn't match opened session [" + this.session() + "]");
         }
      } finally {
         foxService.notifyServerConnectionClosed(this, cause);
      }
   }

   @Override
   public Thread makeThread(ThreadGroup group, Runnable runnable, String name) {
      return new BFoxServerConnection.FoxServerThread(group, runnable, name);
   }

   public void setPersistent(boolean value) {
      this.persistent = value;
   }

   public boolean isPersistent() {
      return this.persistent;
   }

   public void doForceDisconnect() throws Exception {
      this.close();
   }

   class FoxServerThread extends Thread implements ContextThread {
      FoxServerThread(ThreadGroup group, Runnable runnable, String name) {
         super(group, runnable, name);
      }

      public Context getContext() {
         return BFoxServerConnection.this.getSessionContext();
      }
   }
}
