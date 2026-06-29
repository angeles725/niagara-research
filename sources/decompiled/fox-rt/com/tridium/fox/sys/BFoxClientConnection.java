package com.tridium.fox.sys;

import com.tridium.authn.AuthenticationClient;
import com.tridium.authn.LoginFailureCause;
import com.tridium.fox.message.FoxMessage;
import com.tridium.fox.session.Fox;
import com.tridium.fox.session.FoxAuthenticationException;
import com.tridium.fox.session.FoxSession;
import com.tridium.fox.session.FoxsRedirectException;
import com.tridium.nre.security.SecretChars;
import com.tridium.sys.Nre;
import com.tridium.sys.service.ServiceManager;
import java.io.IOException;
import java.net.Socket;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.security.UnrecoverableKeyException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.baja.naming.BHost;
import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraActions;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.security.ClientTlsParameters;
import javax.baja.security.AuthenticationRealm;
import javax.baja.security.BCertificateAliasCredential;
import javax.baja.security.BClientCredentials;
import javax.baja.security.BICredentials;
import javax.baja.security.BIUserCredentials;
import javax.baja.security.BPassword;
import javax.baja.security.BUsernameCredential;
import javax.baja.security.BUsernameSchemeCredentials;
import javax.baja.security.ReportCauseAuthenticationException;
import javax.baja.security.crypto.CertManagerFactory;
import javax.baja.security.crypto.ICryptoManager;
import javax.baja.spy.SpyWriter;
import javax.baja.sys.Action;
import javax.baja.sys.BAbsTime;
import javax.baja.sys.BFacets;
import javax.baja.sys.BIObject;
import javax.baja.sys.BRelTime;
import javax.baja.sys.BString;
import javax.baja.sys.BValue;
import javax.baja.sys.Clock;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.ServiceNotFoundException;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.sys.Clock.Ticket;
import javax.baja.util.CoalesceQueue;
import javax.baja.util.IFuture;
import javax.baja.util.Invocation;
import javax.baja.util.Lexicon;
import javax.baja.util.ThreadPoolWorker;
import javax.net.SocketFactory;
import javax.net.ssl.HandshakeCompletedEvent;
import javax.net.ssl.HandshakeCompletedListener;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "port",
      type = "int",
      defaultValue = "BFoxScheme.DEFAULT_PORT"
   ), @NiagaraProperty(
      name = "useFoxs",
      type = "boolean",
      defaultValue = "false",
      flags = 5
   ), @NiagaraProperty(
      name = "lastFailureTime",
      type = "BAbsTime",
      defaultValue = "BAbsTime.DEFAULT",
      flags = 1
   ), @NiagaraProperty(
      name = "lastFailureCause",
      type = "String",
      defaultValue = "",
      flags = 1
   ), @NiagaraProperty(
      name = "retryPeriod",
      type = "BRelTime",
      defaultValue = "BRelTime.make(5*60*1000)"
   ), @NiagaraProperty(
      name = "nextAttemptTime",
      type = "BAbsTime",
      defaultValue = "BAbsTime.DEFAULT",
      flags = 1
   ), @NiagaraProperty(
      name = "credentialStore",
      type = "BClientCredentials",
      defaultValue = "new BClientCredentials()",
      facets = {@Facet("BFacets.make(BFacets.FIELD_EDITOR, BString.make(\"workbench:CredentialStoreFE\"))")}
   )})
@NiagaraActions({@NiagaraAction(
      name = "manualConnect"
   ), @NiagaraAction(
      name = "manualDisconnect"
   ), @NiagaraAction(
      name = "lingerTimeout",
      flags = 20
   )})
public class BFoxClientConnection extends BFoxConnection implements AuthenticationClient {
   public static final Property port = newProperty(0, 1911, null);
   public static final Property useFoxs = newProperty(5, false, null);
   public static final Property lastFailureTime = newProperty(1, BAbsTime.DEFAULT, null);
   public static final Property lastFailureCause = newProperty(1, "", null);
   public static final Property retryPeriod = newProperty(0, BRelTime.make(300000L), null);
   public static final Property nextAttemptTime = newProperty(1, BAbsTime.DEFAULT, null);
   public static final Property credentialStore = newProperty(
      0, new BClientCredentials(), BFacets.make("fieldEditor", BString.make("workbench:CredentialStoreFE"))
   );
   public static final Action manualConnect = newAction(0, null);
   public static final Action manualDisconnect = newAction(0, null);
   public static final Action lingerTimeout = newAction(20, null);
   public static final Type TYPE = Sys.loadType(BFoxClientConnection.class);
   public static long engageLinger = 60000L;
   private static CoalesceQueue queue;
   private static final ThreadPoolWorker threadPool;
   private static final Lexicon LEX;
   private BFoxSession foxSession;
   private AuthenticationClient authenticationClient = this;
   private String authenticationScheme;
   private BHost remoteHost;
   private final Object connectLock = new Object();
   private long lastFailureTicks;
   private Exception lastFailureException;
   private final Object retryLock = new Object();
   private final Map<BFoxClientConnection.Interest, BFoxClientConnection.InterestLogItem> interests = new HashMap<>(5);
   private List<BFoxClientConnection.InterestLogItem> interestLog = new ArrayList<>(20);
   private Ticket lingerTicket;
   private boolean checkBrandCompatibility = true;
   private boolean sslHandshakeComplete = false;
   private SSLSocketFactory socketFactory = null;

   public int getPort() {
      return this.getInt(port);
   }

   public void setPort(int v) {
      this.setInt(port, v, null);
   }

   public boolean getUseFoxs() {
      return this.getBoolean(useFoxs);
   }

   public void setUseFoxs(boolean v) {
      this.setBoolean(useFoxs, v, null);
   }

   public BAbsTime getLastFailureTime() {
      return (BAbsTime)this.get(lastFailureTime);
   }

   public void setLastFailureTime(BAbsTime v) {
      this.set(lastFailureTime, v, null);
   }

   public String getLastFailureCause() {
      return this.getString(lastFailureCause);
   }

   public void setLastFailureCause(String v) {
      this.setString(lastFailureCause, v, null);
   }

   public BRelTime getRetryPeriod() {
      return (BRelTime)this.get(retryPeriod);
   }

   public void setRetryPeriod(BRelTime v) {
      this.set(retryPeriod, v, null);
   }

   public BAbsTime getNextAttemptTime() {
      return (BAbsTime)this.get(nextAttemptTime);
   }

   public void setNextAttemptTime(BAbsTime v) {
      this.set(nextAttemptTime, v, null);
   }

   public BClientCredentials getCredentialStore() {
      return (BClientCredentials)this.get(credentialStore);
   }

   public void setCredentialStore(BClientCredentials v) {
      this.set(credentialStore, v, null);
   }

   public void manualConnect() {
      this.invoke(manualConnect, null, null);
   }

   public void manualDisconnect() {
      this.invoke(manualDisconnect, null, null);
   }

   public void lingerTimeout() {
      this.invoke(lingerTimeout, null, null);
   }

   @Override
   public Type getType() {
      return TYPE;
   }

   public BFoxClientConnection(BFoxSession foxSession) {
      this.foxSession = foxSession;
      this.setRemoteHost(foxSession.getHost());
      this.setPort(foxSession.getPort());
      this.setUseFoxs(foxSession.getUseFoxs());
      this.init();
   }

   public BFoxClientConnection() {
      this.init();
   }

   private void init() {
      if (Sys.getStation() != null) {
         try {
            AccessController.doPrivileged((PrivilegedAction<ServiceManager>)(() -> Nre.getServiceManager())).getService("platCrypto:CertManagerService");
            this.setFlags(useFoxs, this.getFlags(useFoxs) & -6);
         } catch (Exception var2) {
            this.setUseFoxs(false);
            this.setFlags(useFoxs, this.getFlags(useFoxs) | 1 | 4);
         }
      } else {
         this.setFlags(useFoxs, this.getFlags(useFoxs) & -6);
      }

      this.set(channels, BFoxChannelRegistry.getPrototype().newCopy());
   }

   public String getUsername() {
      return this.getCredentials().getUsername();
   }

   public final BFoxSession getFoxSession() {
      return this.foxSession;
   }

   public final BHost getRemoteHost() {
      Optional<NiagaraStation> station = this.getConnectionTarget(NiagaraStation.class);
      return station.isPresent() ? station.get().getRemoteHost() : this.remoteHost;
   }

   public void setRemoteHost(BHost remoteHost) {
      this.remoteHost = remoteHost;
   }

   public void setRemoteHost(BHost remoteHost, int port) {
      this.setRemoteHost(remoteHost);
      this.setPort(port);
   }

   public void setCredentials(BIUserCredentials credentials) {
      this.getCredentialStore().setCredentials(credentials);
   }

   public void setSSLSocketFactory(SSLSocketFactory socketFactory) {
      this.socketFactory = socketFactory;
   }

   public SSLSocketFactory getSSLSocketFactory() {
      return this.socketFactory;
   }

   public BIUserCredentials getCredentials() {
      return this.getCredentialStore().getCredentials();
   }

   public void setAuthenticationClient(AuthenticationClient client) {
      this.authenticationClient = client;
   }

   public AuthenticationClient getAuthenticationClient() {
      return this.authenticationClient;
   }

   public void setAuthenticationScheme(String scheme) {
      this.authenticationScheme = scheme;
   }

   public String getAuthenticationScheme() {
      return this.authenticationScheme;
   }

   public boolean isSslHandshakeComplete() {
      return this.sslHandshakeComplete;
   }

   @Override
   public void sessionOpened(FoxSession session) {
      if (this.checkBrandCompatibility) {
         Acceptor.accept(session);
      }

      super.sessionOpened(session);
      if (this.foxSession != null) {
         this.foxSession.sessionOpened();
      }

      this.getConnectionTarget(NiagaraStation.class).ifPresent(NiagaraStation::clientOpened);
   }

   @Override
   public void sessionClosed(FoxSession session, Throwable cause) {
      this.sessionClosed(session, cause, null);
   }

   @Override
   public void sessionClosed(FoxSession session, Throwable cause, LoginFailureCause failureCause) {
      super.sessionClosed(session, cause);
      if (this.foxSession != null) {
         this.foxSession.sessionClosed(failureCause);
      }

      this.getConnectionTarget(NiagaraStation.class).ifPresent(NiagaraStation::clientClosed);
   }

   public void connect() throws Exception {
      try {
         AccessController.doPrivileged(new BFoxClientConnection.ConnectPrivilegedAction());
      } catch (PrivilegedActionException var2) {
         throw var2.getException();
      }
   }

   private boolean isClientCertAuthUnrecoverableKeyException(BFoxSession foxSession, BIUserCredentials cred, Exception e) {
      return foxSession != null
         && cred instanceof BCertificateAliasCredential
         && e.getCause() != null
         && e.getCause().getCause() != null
         && e.getCause().getCause() instanceof UnrecoverableKeyException;
   }

   private void listenForHandshake(final SSLSocket socket) {
      HandshakeCompletedListener listener = new HandshakeCompletedListener() {
         @Override
         public void handshakeCompleted(HandshakeCompletedEvent handshakeCompletedEvent) {
            BFoxClientConnection.this.sslHandshakeComplete = true;
            socket.removeHandshakeCompletedListener(this);
         }
      };
      socket.addHandshakeCompletedListener(listener);
   }

   public BFoxClientConnection.Interest[] getInterests() {
      synchronized (this.interests) {
         return this.interests.keySet().toArray(new BFoxClientConnection.Interest[0]);
      }
   }

   boolean hasInterests() {
      synchronized (this.interests) {
         return !this.interests.isEmpty();
      }
   }

   public BFoxClientConnection.InterestLogItem[] getInterestLog() {
      BFoxClientConnection.InterestLogItem[] active = this.interests.values().toArray(new BFoxClientConnection.InterestLogItem[0]);
      BFoxClientConnection.InterestLogItem[] historical = this.interestLog.toArray(new BFoxClientConnection.InterestLogItem[0]);
      BFoxClientConnection.InterestLogItem[] total = new BFoxClientConnection.InterestLogItem[active.length + historical.length];
      System.arraycopy(active, 0, total, 0, active.length);
      System.arraycopy(historical, 0, total, active.length, historical.length);
      return total;
   }

   public boolean isEngaged(BFoxClientConnection.Interest interest) {
      synchronized (this.interests) {
         return this.interests.get(interest) != null;
      }
   }

   public void engageNoRetry(BFoxClientConnection.Interest interest) throws Exception {
      this.engageNoRetry(interest, 5000L);
   }

   public void engageNoRetry(BFoxClientConnection.Interest interest, long failFastPeriod) throws Exception {
      this.engageNoRetry(interest, failFastPeriod, false);
   }

   public void engageNoRetry(BFoxClientConnection.Interest interest, long failFastPeriod, boolean updateState) throws Exception {
      Exception ex = this.lastFailureException;
      if (ex != null && Clock.ticks() - this.lastFailureTicks < failFastPeriod) {
         throw ex;
      } else {
         try {
            this.engage(interest, false);
         } catch (Exception var9) {
            if (updateState) {
               this.transition("Waiting for retry...");
               long retryPeriod = this.getRetryPeriod().getMillis();
               this.setNextAttemptTime(BAbsTime.make(Clock.millis() + retryPeriod));
            }

            throw var9;
         }
      }
   }

   public void engageRetry(BFoxClientConnection.Interest interest) throws Exception {
      this.engage(interest, true);
   }

   private void engage(BFoxClientConnection.Interest interest, boolean retry) throws Exception {
      synchronized (this.interests) {
         if (this.interests.get(interest) == null) {
            BFoxClientConnection.InterestLogItem log = this.logEngaged(interest);
            this.interests.put(interest, log);
         }

         if (this.lingerTicket != null) {
            this.lingerTicket.cancel();
         }
      }

      while (true) {
         try {
            this.connect();
            this.interruptRetries();
            return;
         } catch (Exception var9) {
            if (!retry) {
               throw var9;
            }

            this.transition("Waiting for retry...");
            long retryPeriod = this.getRetryPeriod().getMillis();
            this.setNextAttemptTime(BAbsTime.make(Clock.millis() + retryPeriod));
            synchronized (this.retryLock) {
               this.retryLock.wait(retryPeriod);
            }
         }
      }
   }

   public void disengage(BFoxClientConnection.Interest interest) {
      synchronized (this.interests) {
         int oldSize = this.interests.size();
         if (interest != null) {
            BFoxClientConnection.InterestLogItem log = this.interests.get(interest);
            if (log != null) {
               this.logDisengaged(log);
            }

            this.interests.remove(interest);
         }

         if (oldSize > 0 && this.interests.size() == 0) {
            if (this.isRunning()) {
               this.lingerTicket = Clock.schedule(this, BRelTime.make(engageLinger), lingerTimeout, null);
            } else {
               this.close();
            }
         }
      }

      this.sslHandshakeComplete = false;
   }

   public void interruptRetries() {
      if (this.retryLock != null) {
         synchronized (this.retryLock) {
            this.retryLock.notifyAll();
         }
      }
   }

   private void transition(String newState) {
      if (this.log.isTraceOn()) {
         this.log.trace(this.getState() + " -> " + newState);
      }

      this.setState(newState);
   }

   public void doLingerTimeout() {
      synchronized (this.interests) {
         this.lingerTicket = null;
         if (this.interests.size() == 0) {
            this.close();
         }
      }
   }

   public boolean getCheckBrandCompatibility() {
      return this.checkBrandCompatibility;
   }

   public void setCheckBrandCompatibility(boolean check) {
      this.checkBrandCompatibility = check;
   }

   public final boolean isPasswordResetRequired() {
      FoxSession session = this.session();
      if (session != null) {
         FoxMessage remoteWelcome = session.getRemoteWelcome();
         if (remoteWelcome != null) {
            return remoteWelcome.getBoolean("forceReset", false);
         }
      }

      return false;
   }

   public void pingOk() {
      this.getConnectionTarget(NiagaraStation.class).ifPresent(NiagaraStation::pingOk);
   }

   public void pingFail(String cause) {
      this.getConnectionTarget(NiagaraStation.class).ifPresent(station -> station.pingFail(cause));
   }

   public void doManualConnect() throws Exception {
      this.connect();
   }

   public void doManualDisconnect() throws Exception {
      this.close();
   }

   public IFuture post(Action action, BValue argument, Context cx) {
      if (action.equals(lingerTimeout)) {
         try {
            if (!threadPool.isRunning()) {
               synchronized (threadPool) {
                  if (!threadPool.isRunning()) {
                     threadPool.start("foxLingerTimeout");
                  }
               }
            }

            queue.enqueue(new Invocation(this, action, argument, cx));
         } catch (Exception var7) {
            this.log.error("Could not invoke lingerTimeout action on fox client connection " + this.toDebugString(), var7);
         }

         return null;
      } else {
         return super.post(action, argument, cx);
      }
   }

   public void changed(Property prop, Context cx) {
      super.changed(prop, cx);
      if (prop.isFrozen() && (prop.getDefaultFlags() & 1) == 0) {
         this.interruptRetries();
      }
   }

   private BFoxClientConnection.InterestLogItem logEngaged(BFoxClientConnection.Interest interest) {
      BFoxClientConnection.InterestLogItem log = new BFoxClientConnection.InterestLogItem();
      log.interest = interest.toString();
      log.startTicks = Clock.ticks();
      return log;
   }

   private void logDisengaged(BFoxClientConnection.InterestLogItem log) {
      log.endTicks = Clock.ticks();

      while (this.interestLog.size() >= 20) {
         this.interestLog.remove(0);
      }

      this.interestLog.add(log);
   }

   public static void spyThreadPoolWorker(SpyWriter out) {
      try {
         threadPool.spy(out);
      } catch (Exception var2) {
      }
   }

   public BUsernameCredential requestUsername(AuthenticationRealm realm) {
      BValue authScheme = this.get("authenticationScheme");
      return (BUsernameCredential)(authScheme instanceof BString && !((BString)authScheme).getString().isEmpty()
         ? new BUsernameSchemeCredentials(this.getCredentials().getUsername(), ((BString)authScheme).getString())
         : new BUsernameCredential(this.getCredentials().getUsername()));
   }

   public BICredentials requestInformation(AuthenticationRealm realm, String schemeName, int step, BIObject seedInfo) {
      return this.getCredentials();
   }

   static {
      try {
         engageLinger = Long.parseLong(
            AccessController.doPrivileged((PrivilegedAction<String>)(() -> System.getProperty("niagara.fox.engageLinger", "" + engageLinger))).trim()
         );
      } catch (Exception var1) {
         var1.printStackTrace();
      }

      queue = new CoalesceQueue(1000);
      threadPool = new ThreadPoolWorker(queue);
      threadPool.setMaxThreads(1000);
      LEX = Lexicon.make("fox");
   }

   private class ConnectPrivilegedAction implements PrivilegedExceptionAction<Object> {
      private ConnectPrivilegedAction() {
      }

      @Override
      public Object run() throws Exception {
         Optional<NiagaraStation> parent = BFoxClientConnection.this.getConnectionTarget(NiagaraStation.class);
         long startTicks = Clock.ticks();
         synchronized (BFoxClientConnection.this.connectLock) {
            if (BFoxClientConnection.this.isConnected()) {
               return null;
            } else if (BFoxClientConnection.this.lastFailureTicks > startTicks) {
               throw BFoxClientConnection.this.lastFailureException;
            } else {
               String startState = BFoxClientConnection.this.getState();
               FoxSession.IFoxSessionListener[] listeners = FoxSession.createListeners(BFoxClientConnection.this);

               try {
                  if (parent.isPresent() && parent.get().isFatalFault()) {
                     throw new Exception(parent.get().getFaultCause());
                  }

                  BFoxClientConnection.this.transition("Connecting...");
                  BFoxClientConnection.this.setNextAttemptTime(BAbsTime.NULL);
                  BHost remoteHost = BFoxClientConnection.this.getRemoteHost();
                  if (remoteHost == null) {
                     throw new IllegalStateException("Remote host not set");
                  }

                  BIUserCredentials cred = BFoxClientConnection.this.getCredentials();
                  if ((cred.getUsername() == null || cred.getUsername().isEmpty())
                     && BFoxClientConnection.this.authenticationClient.getPreconnectCredentials().isPresent()) {
                     cred = (BIUserCredentials)BFoxClientConnection.this.authenticationClient.getPreconnectCredentials().get();
                  }

                  Socket socket = null;
                  boolean redirect = false;
                  if (!BFoxClientConnection.this.getUseFoxs()) {
                     try {
                        socket = remoteHost.openSocket(BFoxClientConnection.this.getPort());
                        Fox.open(BFoxClientConnection.this, socket, cred, listeners);
                     } catch (FoxsRedirectException var37) {
                        if (BFoxClientConnection.this.foxSession != null) {
                           throw var37;
                        }

                        socket.close();
                        redirect = true;
                        int redirectPort = var37.getPort();
                        BFoxClientConnection.this.setPort(var37.getPort());
                        BFoxClientConnection.this.setUseFoxs(true);
                        BFoxClientConnection.this.log.message("received foxs redirect to port " + redirectPort);
                     }
                  }

                  if (BFoxClientConnection.this.getUseFoxs() || redirect) {
                     try {
                        if (BFoxClientConnection.this.socketFactory == null) {
                           ICryptoManager cryptoFactory = CertManagerFactory.getInstance();
                           ClientTlsParameters tlsParameters;
                           if (cred instanceof BCertificateAliasCredential) {
                              tlsParameters = new ClientTlsParameters(
                                 ClientTlsParameters.DEFAULT.getMinTlsProtocol(), ((BCertificateAliasCredential)cred).getCertificateAlias()
                              );
                              if (!((BCertificateAliasCredential)cred).getCertificatePassword().equals(BPassword.DEFAULT)) {
                                 try {
                                    SecretChars certPasswordChars = ((BCertificateAliasCredential)cred).getCertificatePassword().getSecretChars();
                                    Throwable var15 = null;

                                    try {
                                       tlsParameters.setKeyPassphrase(certPasswordChars.get());
                                    } catch (Throwable var32) {
                                       var15 = var32;
                                       throw var32;
                                    } finally {
                                       if (certPasswordChars != null) {
                                          if (var15 != null) {
                                             try {
                                                certPasswordChars.close();
                                             } catch (Throwable var31) {
                                                var15.addSuppressed(var31);
                                             }
                                          } else {
                                             certPasswordChars.close();
                                          }
                                       }
                                    }
                                 } catch (Exception var35) {
                                    BFoxClientConnection.this.log.message("error decoding password for cert", var35);
                                 }
                              }
                           } else {
                              tlsParameters = ClientTlsParameters.DEFAULT;
                           }

                           try {
                              SocketFactory fact = cryptoFactory.getClientSocketFactory(tlsParameters);
                              socket = remoteHost.openSocket(BFoxClientConnection.this.getPort(), fact);
                           } catch (SecurityException var33) {
                              if (BFoxClientConnection.this.isClientCertAuthUnrecoverableKeyException(BFoxClientConnection.this.foxSession, cred, var33)) {
                                 BFoxClientConnection.this.foxSession.getConnection().getAuthenticationClient().setPreconnectCredentials(null);
                                 throw new FoxAuthenticationException(
                                    Lexicon.make("clientCertAuth").getText("authnHandler.unsupportedCertificateCredentials"),
                                    BFoxClientConnection.this.foxSession.getAuthenticationScheme(),
                                    null,
                                    BFoxClientConnection.this.foxSession.getConnection().session()
                                 );
                              }

                              throw var33;
                           }
                        } else {
                           socket = remoteHost.openSocket(BFoxClientConnection.this.getPort(), BFoxClientConnection.this.socketFactory);
                        }

                        if (socket instanceof SSLSocket) {
                           BFoxClientConnection.this.listenForHandshake((SSLSocket)socket);
                        }

                        if (BFoxClientConnection.this.foxSession != null) {
                           BFoxClientConnection.this.foxSession.setUseFoxs(true);
                           BFoxClientConnection.this.foxSession.setPort(BFoxClientConnection.this.getPort());
                        }

                        Fox.open(BFoxClientConnection.this, socket, cred, listeners);
                     } catch (ServiceNotFoundException var36) {
                        throw new IOException("CryptoFactory not found.");
                     }
                  }

                  BFoxClientConnection.this.lastFailureTicks = 0L;
                  BFoxClientConnection.this.lastFailureException = null;
                  BFoxClientConnection.this.setLastFailureTime(BAbsTime.NULL);
                  BFoxClientConnection.this.setLastFailureCause("");
                  if (BFoxClientConnection.this.isPasswordResetRequired()) {
                     BFoxClientConnection.this.pingFail(BFoxClientConnection.LEX.getText("ping.fail.passwordResetRequired"));
                  } else {
                     BFoxClientConnection.this.pingOk();
                  }

                  if (BFoxClientConnection.this.foxSession != null) {
                     BFoxClientConnection.this.foxSession.postConnect();
                  }
               } catch (FoxsRedirectException var38) {
                  throw var38;
               } catch (Exception var39) {
                  Exception e = var39;
                  String cause = var39.toString();
                  if (var39 instanceof FoxAuthenticationException) {
                     FoxAuthenticationException ex = (FoxAuthenticationException)var39;
                     cause = "Authentication Failed";
                     if (var39 instanceof ReportCauseAuthenticationException) {
                        cause = cause + ": " + ((ReportCauseAuthenticationException)var39).getCauseMessage();
                     }

                     if (ex.fatal != null) {
                        cause = cause + ": " + ex.fatal;
                     }
                  }

                  BFoxClientConnection.this.transition(startState);
                  BFoxClientConnection.this.lastFailureTicks = Clock.ticks();
                  BFoxClientConnection.this.lastFailureException = var39;
                  BFoxClientConnection.this.setLastFailureTime(Clock.time());
                  BFoxClientConnection.this.setLastFailureCause(cause);
                  BFoxClientConnection.this.pingFail(cause);
                  if (listeners != FoxSession.nullListeners) {
                     for (FoxSession.IFoxSessionListener listener : listeners) {
                        listener.connectionAborted(cause, e);
                     }
                  }

                  if (listeners != FoxSession.nullListeners) {
                     for (FoxSession.IFoxSessionListener listener : listeners) {
                        listener.connectionAborted(cause, e);
                     }
                  }

                  BFoxClientConnection.this.sslHandshakeComplete = false;
                  throw e;
               }

               return null;
            }
         }
      }
   }

   public interface Interest {
      @Override
      int hashCode();

      @Override
      boolean equals(Object var1);

      @Override
      String toString();
   }

   public static class InterestLogItem {
      public String interest;
      public long startTicks;
      public long endTicks;
   }

   public static class StringInterest implements BFoxClientConnection.Interest {
      private String string;

      public StringInterest(String s) {
         if (s == null) {
            throw new IllegalArgumentException("interest string is null");
         } else {
            this.string = s;
         }
      }

      @Override
      public int hashCode() {
         return this.string.hashCode();
      }

      @Override
      public boolean equals(Object o) {
         return o instanceof BFoxClientConnection.StringInterest && ((BFoxClientConnection.StringInterest)o).string.equals(this.string);
      }

      @Override
      public String toString() {
         return this.string;
      }
   }
}
