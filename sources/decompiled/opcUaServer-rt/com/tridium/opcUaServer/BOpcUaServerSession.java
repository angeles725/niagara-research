package com.tridium.opcUaServer;

import com.prosysopc.ua.ApplicationIdentity;
import com.prosysopc.ua.server.ServerUserIdentity;
import com.prosysopc.ua.server.Session;
import com.prosysopc.ua.stack.builtintypes.DateTime;
import com.tridium.authn.LoginFailureCause;
import com.tridium.nre.security.NiagaraBasicPermission;
import com.tridium.session.NiagaraSession;
import com.tridium.session.SessionManager;
import java.util.HashMap;
import java.util.Map;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BComponent;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.user.BUser;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "sessionId",
      type = "String",
      defaultValue = "",
      flags = 3
   ), @NiagaraProperty(
      name = "sessionName",
      type = "String",
      defaultValue = "",
      flags = 3
   ), @NiagaraProperty(
      name = "sessionTimeout",
      type = "String",
      defaultValue = "",
      flags = 3
   ), @NiagaraProperty(
      name = "clientLastContactTime",
      type = "String",
      defaultValue = "",
      flags = 3
   ), @NiagaraProperty(
      name = "isActive",
      type = "boolean",
      defaultValue = "false",
      flags = 3
   ), @NiagaraProperty(
      name = "userIdentity",
      type = "String",
      defaultValue = "",
      flags = 3
   ), @NiagaraProperty(
      name = "subscriptionCount",
      type = "int",
      defaultValue = "0",
      flags = 3
   ), @NiagaraProperty(
      name = "sessionPath",
      type = "String",
      defaultValue = "",
      flags = 5
   )})
public final class BOpcUaServerSession extends BComponent implements NiagaraSession {
   public static final Property sessionId = newProperty(3, "", null);
   public static final Property sessionName = newProperty(3, "", null);
   public static final Property sessionTimeout = newProperty(3, "", null);
   public static final Property clientLastContactTime = newProperty(3, "", null);
   public static final Property isActive = newProperty(3, false, null);
   public static final Property userIdentity = newProperty(3, "", null);
   public static final Property subscriptionCount = newProperty(3, 0, null);
   public static final Property sessionPath = newProperty(5, "", null);
   public static final Type TYPE = Sys.loadType(BOpcUaServerSession.class);
   private Session session = null;
   private String id;
   private String superId;
   private final long creationTime = System.currentTimeMillis();

   public String getSessionId() {
      return this.getString(sessionId);
   }

   public void setSessionId(String v) {
      this.setString(sessionId, v, null);
   }

   public String getSessionName() {
      return this.getString(sessionName);
   }

   public void setSessionName(String v) {
      this.setString(sessionName, v, null);
   }

   public String getSessionTimeout() {
      return this.getString(sessionTimeout);
   }

   public void setSessionTimeout(String v) {
      this.setString(sessionTimeout, v, null);
   }

   public String getClientLastContactTime() {
      return this.getString(clientLastContactTime);
   }

   public void setClientLastContactTime(String v) {
      this.setString(clientLastContactTime, v, null);
   }

   public boolean getIsActive() {
      return this.getBoolean(isActive);
   }

   public void setIsActive(boolean v) {
      this.setBoolean(isActive, v, null);
   }

   public String getUserIdentity() {
      return this.getString(userIdentity);
   }

   public void setUserIdentity(String v) {
      this.setString(userIdentity, v, null);
   }

   public int getSubscriptionCount() {
      return this.getInt(subscriptionCount);
   }

   public void setSubscriptionCount(int v) {
      this.setInt(subscriptionCount, v, null);
   }

   public String getSessionPath() {
      return this.getString(sessionPath);
   }

   public void setSessionPath(String v) {
      this.setString(sessionPath, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public static BOpcUaServerSession make(Session session) {
      BOpcUaServerSession newSession = new BOpcUaServerSession();
      newSession.session = session;
      newSession.setSessionId(session.getSessionId().toString());
      newSession.setSessionName(session.getSessionName());
      newSession.setSessionTimeout("" + (int)session.getSessionTimeout());
      newSession.setIsActive(session.isActive());
      DateTime lastTime = session.getClientLastContactTime();
      newSession.setClientLastContactTime(lastTime == null ? "" : lastTime.toString());
      ServerUserIdentity identity = session.getUserIdentity();
      newSession.setUserIdentity(identity == null ? "" : identity.toString());
      newSession.setSubscriptionCount(session.getSubscriptionCount());
      newSession.id = SessionManager.generateSessionId(BOpcUaServerSession.class, 25);
      newSession.setSessionPath(newSession.toPathString());
      return newSession;
   }

   public Session getUaSession() {
      return this.session;
   }

   public String toString(Context cx) {
      return this.getSessionName();
   }

   public Map<String, String> getSessionInfo() {
      return new HashMap<>();
   }

   public long getCreationTime() {
      return this.creationTime;
   }

   public String getId() {
      return this.id;
   }

   public String getSuperId() {
      return this.superId;
   }

   public void setSuperId(String superId) {
      NiagaraBasicPermission initPermission = new NiagaraBasicPermission("MODIFY_SESSION_IDS");
      SecurityManager sm = System.getSecurityManager();
      if (sm != null) {
         sm.checkPermission(initPermission);
      }

      this.superId = superId;
   }

   public void doSetAuthenticated(BUser user) {
   }

   public void invalidate() {
      this.invalidate(null);
   }

   public void invalidate(LoginFailureCause cause) {
      SessionManager.removeSession(this);
   }

   public String getAuditTarget() {
      return this.getSessionPath();
   }

   public String getRemoteHost() {
      return ApplicationIdentity.getActualHostName();
   }
}
