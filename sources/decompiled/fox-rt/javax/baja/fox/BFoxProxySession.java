package javax.baja.fox;

import com.tridium.fox.sys.BFoxSession;
import java.util.Optional;
import javax.baja.naming.BHost;
import javax.baja.naming.BOrd;
import javax.baja.naming.BSession;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.security.AuthenticationRealm;
import javax.baja.security.BICredentials;
import javax.baja.security.BIUserCredentials;
import javax.baja.security.BUsernameAndPassword;
import javax.baja.sys.BAbsTime;
import javax.baja.sys.BRelTime;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.util.LexiconText;

@NiagaraType
public abstract class BFoxProxySession extends BSession implements AuthenticationRealm {
   public static final Type TYPE = Sys.loadType(BFoxProxySession.class);

   public Type getType() {
      return TYPE;
   }

   @Deprecated
   public static BFoxProxySession make(BHost host, int port, String username, String password) {
      return make(host, port, false, username, password);
   }

   public static BFoxProxySession make(BHost host, int port, boolean useFoxs, String username, String password) {
      return make(host, port, useFoxs, new BUsernameAndPassword(username, password));
   }

   public static BFoxProxySession make(BHost host, int port, boolean useFoxs, BIUserCredentials credentials) {
      BFoxSession session = BFoxSession.make(null, host, port, useFoxs);
      BIUserCredentials creds = (BIUserCredentials)session.getCredentials();
      if (creds != null && session.isConnected()) {
         if (!creds.equivalent(credentials)) {
            throw new IllegalStateException("Session already connected with different credentials");
         }
      } else {
         session.setCredentials(credentials);
         session.getConnection().setCredentials(credentials);
      }

      return session;
   }

   protected BFoxProxySession(String name) {
      super(name, LexiconText.make("fox", "nav.foxSession"));
   }

   public abstract int getPort();

   public abstract String getStationName();

   public abstract String getUsername();

   public abstract BAbsTime getLastFailureTime();

   public abstract String getLastFailureCause();

   public abstract BRelTime getRetryPeriod();

   public abstract void setRetryPeriod(BRelTime var1);

   public abstract BAbsTime getNextAttemptTime();

   public <R> Optional<R> rpc(BOrd ord, String methodName, Object... args) throws Exception {
      return ((BFoxSession)this).getConnection().getChannels().getSysChannel().niagaraRpc(ord, methodName, args);
   }

   public abstract boolean isEngaged(String var1);

   public abstract void engageNoRetry(String var1) throws Exception;

   public abstract void engageNoRetry(String var1, long var2) throws Exception;

   public abstract void engageRetry(String var1) throws Exception;

   public abstract void disengage(String var1);

   public void userActivity() {
   }

   public void addNotifyListener(BFoxProxySession.NotifyListener listener) {
   }

   public void removeNotifyListener(BFoxProxySession.NotifyListener listener) {
   }

   public Object pauseActivityMonitor() {
      return null;
   }

   public void resumeActivityMonitor(Object token) {
   }

   public abstract void connect() throws Exception;

   public abstract void disconnect();

   public abstract void close();

   public String getAuthenticationRealmName() {
      throw new UnsupportedOperationException();
   }

   public String getAuthenticationScheme() {
      throw new UnsupportedOperationException();
   }

   public String[] getAvailableAuthenticationSchemes() {
      throw new UnsupportedOperationException();
   }

   public String getDefaultAuthenticationScheme() {
      throw new UnsupportedOperationException();
   }

   public BICredentials makeCredentials() {
      throw new UnsupportedOperationException();
   }

   public BICredentials getCredentials() {
      throw new UnsupportedOperationException();
   }

   public void setCredentials(BICredentials credentials) {
      throw new UnsupportedOperationException();
   }

   public interface NotifyListener {
      boolean onNotify(BFoxProxySession var1);
   }
}
