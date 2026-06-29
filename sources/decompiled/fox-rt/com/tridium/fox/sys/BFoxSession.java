package com.tridium.fox.sys;

import com.tridium.authn.LoginFailureCause;
import com.tridium.fox.message.FoxMessage;
import com.tridium.fox.session.Fox;
import com.tridium.fox.session.FoxAuthenticationException;
import com.tridium.fox.session.FoxSession;
import com.tridium.fox.session.FoxsRedirectException;
import com.tridium.fox.sys.broker.BBrokerChannel;
import com.tridium.util.ArrayUtil;
import com.tridium.util.BSessionInfo;
import com.tridium.util.CustomThemeModuleManager;
import com.tridium.util.IFoxSession;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.stream.Collectors;
import javax.baja.agent.AgentList;
import javax.baja.file.BIFile;
import javax.baja.fox.BFoxProxySession;
import javax.baja.naming.BHost;
import javax.baja.naming.BOrd;
import javax.baja.naming.SlotPath;
import javax.baja.naming.BServiceScheme.ServiceSession;
import javax.baja.nav.BINavNode;
import javax.baja.nav.BNavFileSpace;
import javax.baja.nav.NavFileDecoder;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.registry.TypeInfo;
import javax.baja.security.AuthenticationException;
import javax.baja.security.BICredentials;
import javax.baja.security.BIUserCredentials;
import javax.baja.security.BUsernameAndPassword;
import javax.baja.security.CancelledAuthenticationException;
import javax.baja.security.ChangeUserAuthenticationException;
import javax.baja.space.BSpace;
import javax.baja.sys.BAbsTime;
import javax.baja.sys.BComponent;
import javax.baja.sys.BDynamicEnum;
import javax.baja.sys.BEnumRange;
import javax.baja.sys.BFacets;
import javax.baja.sys.BIcon;
import javax.baja.sys.BRelTime;
import javax.baja.sys.BString;
import javax.baja.sys.BajaRuntimeException;
import javax.baja.sys.Context;
import javax.baja.sys.InvalidEnumException;
import javax.baja.sys.ModuleNotFoundException;
import javax.baja.sys.ServiceNotFoundException;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.timezone.BTimeZone;
import javax.baja.util.Version;

@NiagaraType
public class BFoxSession extends BFoxProxySession implements ServiceSession, IFoxSession, BFoxClientConnection.Interest {
   public static final Type TYPE = Sys.loadType(BFoxSession.class);
   private static final BIcon iconConnected = BIcon.std("fox.png");
   private static final BIcon iconDisconnected = BIcon.std("foxDisconnected.png");
   private static final BIcon secureBadgeConnected = BIcon.std("badges/lock.png");
   private static final BIcon secureBadgeDisconnected = BIcon.std("badges/lockDisconnected.png");
   private static final BIcon fipsBadgeConnected = BIcon.std("badges/fips.png");
   private static final ThreadLocal<String> threadSessionId = new ThreadLocal<>();
   private static final String validRemoteTypeInfos = "validRemoteTypeInfos";
   private static final String invalidRemoteTypeInfos = "invalidRemoteTypeInfos";
   private static final long resetTimeoutInterval = 60000L;
   private static final long timeoutNotificationInterval = 30000L;
   private final BHost host;
   private boolean useFoxs;
   private int port;
   private final BFoxClientConnection connection;
   private boolean connected;
   private BOrd absOrd;
   private BOrd ordInHost;
   private String stationName;
   private BIUserCredentials credentials;
   private BIUserCredentials usernameCredential;
   private String authenticationScheme = "Fox";
   private String[] availableSchemes;
   private String defaultScheme;
   private Context sessionContext;
   private LoginFailureCause cause;
   private ActivityMonitor monitor;
   String stationFault;
   public Object uiCache = null;

   @Override
   public Type getType() {
      return TYPE;
   }

   public static BFoxSession[] list() {
      List<BFoxSession> list = new ArrayList<>();

      for (BHost host : BHost.getAllHosts()) {
         for (BINavNode kid : host.getNavChildren()) {
            if (kid instanceof BFoxSession) {
               list.add((BFoxSession)kid);
            }
         }
      }

      return list.toArray(new BFoxSession[0]);
   }

   public static BFoxSession make(BHost host, int port) {
      return make(null, host, port, false);
   }

   public static BFoxSession make(BHost host, int port, boolean useFoxs) {
      return make(null, host, port, useFoxs);
   }

   public static BFoxSession make(String stationName, BHost host, int port, boolean useFoxs) {
      String name = buildNavName(port, useFoxs);
      BFoxSession session = (BFoxSession)host.getNavChild(name);
      if (session == null) {
         session = new BFoxSession(name, stationName, host, port, useFoxs);
         host.addNavChild(session);
      }

      return session;
   }

   protected static String buildNavName(int port, boolean useFoxs) {
      String name = useFoxs ? "foxs" : "fox";
      int defaultPort = useFoxs ? 4911 : 1911;
      if (port != defaultPort) {
         name = name + ":" + port;
      }

      return name;
   }

   private BFoxSession(String name, String stationName, BHost host, int port, boolean useFoxs) {
      super(name);
      this.stationName = stationName;
      this.host = host;
      this.port = port;
      this.useFoxs = useFoxs;
      if (useFoxs) {
         this.ordInHost = BOrd.make(new BFoxsScheme.FoxQuery(port));
      } else {
         this.ordInHost = BOrd.make(new BFoxScheme.FoxQuery(port));
      }

      this.absOrd = BOrd.make(host.getAbsoluteOrd(), this.ordInHost);
      this.connection = new BFoxClientConnection(this);
      if (Fox.appVersion.equals("unknown")) {
         try {
            Fox.appVersion = "" + Sys.getBajaVersion();
         } catch (Exception var9) {
         }

         try {
            Fox.hostName = Sys.getHostName();
         } catch (Exception var8) {
         }

         try {
            Fox.hostAddress = Sys.getLocalHost(null).getHostAddress();
         } catch (Exception var7) {
         }
      }
   }

   public BHost getHost() {
      return this.host;
   }

   @Override
   public int getPort() {
      return this.port;
   }

   public void setPort(int port) {
      this.port = port;
   }

   public boolean getUseFoxs() {
      return this.useFoxs;
   }

   public void setUseFoxs(boolean useFoxs) {
      this.useFoxs = useFoxs;
   }

   public BFoxClientConnection getConnection() {
      return this.connection;
   }

   @Override
   public String getStationName() {
      return this.stationName;
   }

   public BSessionInfo getSessionInfo() {
      FoxSession session = this.connection.session();
      BAbsTime connectedAt = BAbsTime.make(session.getConnectTime());
      Type type = BSessionInfo.TYPE;
      if (this.useFoxs) {
         type = Sys.getType("platCrypto:SslSessionInfo");
      }

      Class<?> cls = type.getTypeClass();
      String username = this.getUsername();

      BSessionInfo info;
      try {
         Method mthd = cls.getMethod("make", String.class, BAbsTime.class, Socket.class, String.class, String.class, boolean.class);
         Socket socket = session.getSocket();
         String hostName = this.connection.getRemoteHost().getHostname();
         info = (BSessionInfo)mthd.invoke(null, this.stationName, connectedAt, socket, username, hostName, session.getRemoteWelcome().getBoolean("fips", false));
      } catch (Exception var10) {
         var10.printStackTrace();
         info = null;
      }

      return info;
   }

   public boolean isConnected() {
      return this.connected;
   }

   @Override
   public synchronized void connect() throws Exception {
      if (!this.connected) {
         if (this.credentials != null) {
            this.connection.setCredentials(this.credentials);
         }

         try {
            this.connection.connect();
            BOrd tordInHost;
            if (this.useFoxs) {
               tordInHost = BOrd.make(new BFoxsScheme.FoxQuery(this.port));
            } else {
               tordInHost = BOrd.make(new BFoxScheme.FoxQuery(this.port));
            }

            if (!tordInHost.equivalent(this.ordInHost)) {
               this.ordInHost = tordInHost;
               this.absOrd = BOrd.make(this.host.getAbsoluteOrd(), this.ordInHost);
            }

            if (!this.connection.hasInterests()) {
               this.connection.engageNoRetry(this, 0L);
            }
         } catch (ChangeUserAuthenticationException var2) {
            this.authenticationScheme = "Fox";
            throw var2;
         } catch (FoxAuthenticationException var3) {
            if (var3.fatal != null) {
               throw new FatalAuthenticationException(var3.fatal);
            } else if ("Cancelled".equals(var3.getMessage())) {
               this.authenticationScheme = "Fox";
               throw new CancelledAuthenticationException();
            } else {
               if (var3.method == null) {
                  this.authenticationScheme = "Fox";
               } else {
                  this.authenticationScheme = "Fox (" + var3.method + ")";
               }

               throw new AuthenticationException(this, var3);
            }
         }
      }
   }

   public static BFoxSession connect(BFoxSession session) throws Exception {
      try {
         session.connect();
      } catch (FoxsRedirectException var2) {
         session = recreateSession(session, var2.getPort(), true);
         session.connect();
      }

      return session;
   }

   private static BFoxSession recreateSession(BFoxSession old, int port, boolean useFoxs) {
      BFoxSession session = make(old.getStationName(), old.getHost(), port, useFoxs);
      session.getConnection().setAuthenticationClient(old.getConnection().getAuthenticationClient());
      if (old.getCredentials() != null) {
         session.setCredentials(old.getCredentials());
      }

      old.close();
      return session;
   }

   void postConnect() throws Exception {
      FoxMessage hello = this.connection.session().getRemoteHello();
      this.stationName = null;

      try {
         this.stationName = hello.getString("station.name");
      } catch (Exception var5) {
      }

      this.connection.getChannels().getUserChannel().fetchPrefs();
      this.connection.getChannels().getSysChannel().subscribeNavEvents();
      this.loadSubSpaces();
      this.loadNavFileSpace();
      BTimeZone timeZone = BTimeZone.getLocal();

      try {
         timeZone = (BTimeZone)BTimeZone.DEFAULT.decodeFromString(hello.getString("timeZone"));
      } catch (Exception var4) {
      }

      this.sessionContext = BFacets.make("TimeZone", timeZone, "username", BString.make(this.getUsername()));
      this.connected = true;
      if (this.monitor != null) {
         this.monitor.start();
      }
   }

   @Override
   public synchronized void disconnect() {
      this.authenticationScheme = "Fox";
      this.connection.disengage(this);
      this.connection.close();
   }

   @Override
   public void close() {
      this.disconnect();
      this.host.removeNavChild(this);
   }

   public String getStationFault() {
      return this.stationFault;
   }

   public Context getSessionContext() {
      return this.sessionContext;
   }

   public LoginFailureCause getFailureCause() {
      return this.cause;
   }

   @Override
   public String getUsername() {
      if (this.credentials != null) {
         return this.credentials.getUsername();
      } else {
         return this.usernameCredential != null ? this.usernameCredential.getUsername() : this.connection.getCredentials().getUsername();
      }
   }

   @Override
   public BAbsTime getLastFailureTime() {
      return this.connection.getLastFailureTime();
   }

   @Override
   public String getLastFailureCause() {
      return this.connection.getLastFailureCause();
   }

   @Override
   public BRelTime getRetryPeriod() {
      return this.connection.getRetryPeriod();
   }

   @Override
   public void setRetryPeriod(BRelTime period) {
      this.connection.setRetryPeriod(period);
   }

   @Override
   public BAbsTime getNextAttemptTime() {
      return this.connection.getNextAttemptTime();
   }

   @Override
   public boolean isEngaged(String interest) {
      return this.connection.isEngaged(this.toInterest(interest));
   }

   @Override
   public void engageNoRetry(String interest) throws Exception {
      this.connection.engageNoRetry(this.toInterest(interest));
   }

   public static BFoxSession engageNoRetry(BFoxSession session, String interest) throws Exception {
      try {
         session.engageNoRetry(interest);
      } catch (FoxsRedirectException var3) {
         session = recreateSession(session, var3.getPort(), true);
         session.engageNoRetry(interest);
      }

      return session;
   }

   @Override
   public void engageNoRetry(String interest, long failFastPeriod) throws Exception {
      this.connection.engageNoRetry(this.toInterest(interest), failFastPeriod);
   }

   public static BFoxSession engageNoRetry(BFoxSession session, String interest, long failFastPeriod) throws Exception {
      try {
         session.engageNoRetry(interest, failFastPeriod);
      } catch (FoxsRedirectException var5) {
         session.engageNoRetry(interest, failFastPeriod);
         session.connect();
      }

      return session;
   }

   @Override
   public void engageRetry(String interest) throws Exception {
      this.connection.engageRetry(this.toInterest(interest));
   }

   public static BFoxSession engageRetry(BFoxSession session, String interest) throws Exception {
      try {
         session.engageRetry(interest);
      } catch (FoxsRedirectException var3) {
         session = recreateSession(session, var3.getPort(), true);
         session.engageRetry(interest);
      }

      return session;
   }

   @Override
   public void disengage(String interest) {
      this.connection.disengage(this.toInterest(interest));
   }

   private BFoxClientConnection.Interest toInterest(String s) {
      return new BFoxClientConnection.StringInterest(s);
   }

   synchronized void sessionOpened() {
   }

   synchronized void sessionClosed() {
      this.sessionClosed(null);
   }

   synchronized void sessionClosed(LoginFailureCause cause) {
      if (this.monitor != null) {
         this.monitor.stop();
      }

      this.connected = false;
      this.stationFault = null;
      this.cause = cause;
      this.unloadSubSpaces();
      this.credentials = null;
      this.connection.setCredentials(new BUsernameAndPassword());
   }

   public long resetSessionTimeout() throws Exception {
      return this.resetSessionTimeout(0L);
   }

   public long resetSessionTimeout(long offset) throws Exception {
      return this.getConnection().getChannels().getUserChannel().resetSessionTimeout(offset);
   }

   public long getSessionTimeRemaining() throws Exception {
      return this.getConnection().getChannels().getUserChannel().getSessionTimeRemaining();
   }

   @Override
   public void userActivity() {
      if (this.isConnected()) {
         if (this.monitor == null) {
            this.monitor = new ActivityMonitor(60000L, 30000L, this, BFoxSession.ExecutorServiceHolder.EXECUTOR);
            this.monitor.start();
         }

         this.monitor.activity();
      }
   }

   @Override
   public void addNotifyListener(BFoxProxySession.NotifyListener listener) {
      if (this.isConnected()) {
         if (this.monitor == null) {
            this.monitor = new ActivityMonitor(60000L, 30000L, this, BFoxSession.ExecutorServiceHolder.EXECUTOR);
            this.monitor.start();
         }

         this.monitor.addNotifyListener(listener);
      }
   }

   @Override
   public void removeNotifyListener(BFoxProxySession.NotifyListener listener) {
      if (this.isConnected()) {
         if (this.monitor == null) {
            this.monitor = new ActivityMonitor(60000L, 30000L, this, BFoxSession.ExecutorServiceHolder.EXECUTOR);
            this.monitor.start();
         }

         this.monitor.removeNotifyListener(listener);
      }
   }

   @Override
   public Object pauseActivityMonitor() {
      if (this.isConnected()) {
         if (this.monitor == null) {
            this.monitor = new ActivityMonitor(60000L, 30000L, this, BFoxSession.ExecutorServiceHolder.EXECUTOR);
            this.monitor.start();
         }

         return this.monitor.pause();
      } else {
         return null;
      }
   }

   @Override
   public void resumeActivityMonitor(Object token) {
      if (this.isConnected()) {
         if (this.monitor == null) {
            this.monitor = new ActivityMonitor(60000L, 30000L, this, BFoxSession.ExecutorServiceHolder.EXECUTOR);
            this.monitor.start();
         }

         this.monitor.resume(token);
      }
   }

   public ActivityMonitor getActivityMonitor() {
      if (this.isConnected() && this.monitor == null) {
         this.monitor = new ActivityMonitor(60000L, 30000L, this, BFoxSession.ExecutorServiceHolder.EXECUTOR);
         this.monitor.start();
      }

      return this.monitor;
   }

   @Override
   public String getAuthenticationRealmName() {
      return this.absOrd.toString();
   }

   @Override
   public String getAuthenticationScheme() {
      return this.authenticationScheme;
   }

   public void setAuthenticationScheme(String schemeName) {
      this.authenticationScheme = "Fox (" + schemeName + ")";
   }

   @Override
   public String[] getAvailableAuthenticationSchemes() {
      return this.availableSchemes == null ? null : (String[])this.availableSchemes.clone();
   }

   public void setAvailableAuthenticationSchemes(String[] availableSchemes) {
      if (availableSchemes == null) {
         this.availableSchemes = null;
      } else {
         this.availableSchemes = (String[])availableSchemes.clone();
      }
   }

   @Override
   public String getDefaultAuthenticationScheme() {
      return this.defaultScheme;
   }

   public void setDefaultAuthenticationScheme(String scheme) {
      this.defaultScheme = scheme;
   }

   @Override
   public BICredentials makeCredentials() {
      return new BUsernameAndPassword();
   }

   @Override
   public BICredentials getCredentials() {
      return this.credentials;
   }

   @Override
   public void setCredentials(BICredentials credentials) {
      if (credentials instanceof BIUserCredentials) {
         this.credentials = (BIUserCredentials)credentials;
      } else {
         throw new IllegalArgumentException();
      }
   }

   public void setUsernameCredential(BIUserCredentials credentials) {
      this.usernameCredential = credentials;
   }

   public void loadSubSpaces() {
      try {
         BSysChannel sysChannel = this.getConnection().getChannels().getSysChannel();
         HashMap<String, String> schemeIds = sysChannel.listLocalSpaces();

         for (TypeInfo type : Sys.getRegistry().getTypes(BIFoxProxySpace.TYPE.getTypeInfo())) {
            if (!type.isInterface() && !type.isAbstract()) {
               try {
                  BSpace space = (BSpace)type.getInstance();
                  if (schemeIds.containsKey(space.getNavName()) && this.getNavChild(space.getNavName()) == null) {
                     ((BIFoxProxySpace)space).init(this);
                     this.addNavChild(space);
                  }
               } catch (Exception var12) {
                  try {
                     Class<?> cls = type.getClass();
                     Constructor<?> cons = cls.getConstructor();
                     if (!Modifier.isPublic(cons.getModifiers())) {
                        continue;
                     }
                  } catch (NoSuchMethodException var10) {
                     continue;
                  } catch (Exception var11) {
                  }

                  var12.printStackTrace();
               }
            }
         }
      } catch (LocalizableServerException var13) {
         if (var13.getLexiconModule().equals("fox") && var13.getLexiconKey().equals("error.NoPermissionForStation")) {
            throw var13;
         }

         var13.printStackTrace();
      } catch (Throwable var14) {
         var14.printStackTrace();
      }
   }

   public void loadNavFileSpace() {
      BOrd navFileOrd = null;

      try {
         for (BINavNode kid : this.getNavChildren()) {
            if (kid instanceof BNavFileSpace) {
               this.removeNavChild(kid);
               break;
            }
         }

         navFileOrd = this.connection.getChannels().getUserChannel().getNavFile();
         if (navFileOrd.isNull()) {
            return;
         }

         BIFile file = (BIFile)navFileOrd.get(this);
         BNavFileSpace space = new NavFileDecoder(file).decodeDocument();
         this.addNavChild(space);
         BINavNode[] kids = this.getNavChildren();
         kids = (BINavNode[])ArrayUtil.toTop(kids, kids.length - 1);
         this.reorderNavChildren(kids);
      } catch (Throwable var6) {
         System.out.println("ERROR:  Cannot load nav file \"" + navFileOrd + "\": " + var6);
      }
   }

   public void unloadSubSpaces() {
      try {
         for (BINavNode sub : this.getNavChildren()) {
            try {
               if (sub instanceof BIFoxProxySpace) {
                  BIFoxProxySpace foxSpace = (BIFoxProxySpace)sub;
                  foxSpace.cleanup(this);
               }

               this.removeNavChild(sub);
            } catch (Exception var6) {
               var6.printStackTrace();
            }
         }
      } catch (Exception var7) {
         var7.printStackTrace();
      }
   }

   public BOrd getAbsoluteOrd() {
      return this.absOrd;
   }

   public BOrd getOrdInHost() {
      return this.ordInHost;
   }

   public String getNavDisplayName(Context cx) {
      String s = super.getNavDisplayName(cx);
      if (!this.useFoxs) {
         if (this.connection.getPort() != 1911) {
            s = s + ":" + this.connection.getPort();
         }
      } else if (this.connection.getPort() != 4911) {
         s = s + ":" + this.connection.getPort();
      }

      return this.stationName != null ? s + " (" + this.stationName + ")" : s;
   }

   public BOrd getNavOrd() {
      return this.absOrd;
   }

   public BComponent getService(Type type) {
      try {
         BBrokerChannel broker = (BBrokerChannel)this.connection.getChannels().get("station", BBrokerChannel.TYPE);
         SlotPath path = broker.serviceToPath(type.toString());
         BOrd ord = BOrd.make("station:|" + path);
         return (BComponent)ord.get(this);
      } catch (Exception var5) {
         throw new ServiceNotFoundException(type.toString(), var5);
      }
   }

   public AgentList getAgents(Context cx) {
      AgentList agents = super.getAgents(cx);
      agents.toTop("workbench:FoxSessionAgent");
      agents.toTop("workbench:StationSummary");
      agents.remove("workbench:NavContainerView");
      return agents;
   }

   public BIcon getIcon() {
      BIcon icon;
      if (this.isConnected()) {
         icon = iconConnected;
         if (this.useFoxs) {
            icon = BIcon.make(icon, secureBadgeConnected);
         }

         try {
            if (this.getConnection().session().getRemoteWelcome().getBoolean("fips", false)) {
               icon = BIcon.make(icon, fipsBadgeConnected);
            }
         } catch (Exception var3) {
         }
      } else {
         icon = iconDisconnected;
         if (this.useFoxs) {
            icon = BIcon.make(icon, secureBadgeDisconnected);
         }
      }

      return icon;
   }

   public static RuntimeException toException(Throwable e) {
      if (e instanceof ModuleNotFoundLocalException) {
         return (RuntimeException)e;
      } else if (e instanceof ModuleNotFoundRemoteException) {
         return (RuntimeException)e;
      } else {
         ModuleNotFoundException x = LocalizableExceptionTranslator.getModuleNotFoundException(e);
         if (x != null) {
            return new ModuleNotFoundLocalException(x.getModuleName(), e);
         } else {
            return (RuntimeException)(e instanceof RuntimeException ? (RuntimeException)e : new BajaRuntimeException(e));
         }
      }
   }

   public void setThreadLocalSessionId() {
      try {
         threadSessionId.set(this.getConnection().session().getId());
      } catch (Exception var2) {
      }
   }

   public void clearThreadLocalSessionId() {
      try {
         threadSessionId.remove();
      } catch (Exception var2) {
      }
   }

   public static BDynamicEnum getDefaultThemeEnumForSession() {
      BDynamicEnum themeEnum = CustomThemeModuleManager.getDefaultThemeEnum();

      try {
         String sessionId = threadSessionId.get();
         if (sessionId != null) {
            FoxSession session = Fox.getSession(sessionId);
            BFoxSession foxSession = ((BFoxClientConnection)session.conn()).getFoxSession();
            BEnumRange range = themeEnum.getRange();
            int[] ordinals = range.getOrdinals();
            String selectedTheme = themeEnum.getTag();
            ArrayList<Integer> newOrdinals = new ArrayList<>(ordinals.length);
            ArrayList<String> newTags = new ArrayList<>(ordinals.length);
            boolean missingTheme = false;

            for (int ordinal : ordinals) {
               String tag = range.getTag(ordinal);
               String themeModuleName = "theme" + tag + ":ux";
               Version themeModuleVersion = session.getFromCache(themeModuleName, key -> (Version)foxSession.fw(404, themeModuleName, null, null, null));
               if (themeModuleVersion != null && !themeModuleVersion.isNull()) {
                  newOrdinals.add(ordinal);
                  newTags.add(tag);
               } else {
                  missingTheme = true;
               }
            }

            if (missingTheme) {
               BEnumRange newRange = BEnumRange.make(newOrdinals.stream().mapToInt(i -> i).toArray(), newTags.toArray(new String[0]));

               try {
                  return BDynamicEnum.make(newRange.get(selectedTheme));
               } catch (InvalidEnumException var17) {
                  if (newOrdinals.size() > 0) {
                     return BDynamicEnum.make(newRange.get(ordinals[0]));
                  }

                  return BDynamicEnum.DEFAULT;
               }
            }
         }
      } catch (Throwable var18) {
      }

      return themeEnum;
   }

   public static Collection<TypeInfo> getRemoteTypesForSession(Collection<TypeInfo> localTypes) {
      try {
         String sessionId = threadSessionId.get();
         if (sessionId != null) {
            FoxSession session = Fox.getSession(sessionId);
            BFoxSession foxSession = ((BFoxClientConnection)session.conn()).getFoxSession();
            BFoxClientConnection connection = foxSession.getConnection();
            if (connection != null && connection.isConnected() && connection.getRemoteVersion().compareTo(FoxSession.VERSION_4) >= 0) {
               BBrokerChannel brokerChannel = (BBrokerChannel)connection.getChannels().get("station");
               if (brokerChannel != null && session != null) {
                  Map<String, Version> validCache = session.getFromCache("validRemoteTypeInfos", key -> new HashMap<>());
                  Collection<String> invalidCache = session.getFromCache("invalidRemoteTypeInfos", key -> new HashSet<>());
                  Map<String, Version> filteredTypeInfos = brokerChannel.checkTypes(
                     localTypes.stream()
                        .filter(type -> !validCache.containsKey(type.toString()) && !invalidCache.contains(type.toString()))
                        .collect(Collectors.toList())
                  );
                  filteredTypeInfos.forEach((type, version) -> {
                     Version var10000 = validCache.merge(type, version, (t, v) -> (Version)v);
                  });
                  invalidCache.addAll(
                     localTypes.stream().filter(type -> !filteredTypeInfos.containsKey(type.toString())).map(TypeInfo::toString).collect(Collectors.toList())
                  );
                  return localTypes.stream().filter(type -> validCache.containsKey(type.toString())).collect(Collectors.toList());
               }
            }
         }
      } catch (Throwable var9) {
      }

      return localTypes;
   }

   public final Object fw(int x, Object a, Object b, Object c, Object d) {
      boolean useSecureFox = false;
      switch (x) {
         case 404:
            if (a != null && !((String)a).equalsIgnoreCase("baja")) {
               if (this.getConnection().getRemoteVersion().compareTo(new Version(new int[]{3, 6, 13})) != -1) {
                  BSysChannel sysChannel = this.getConnection().getChannels().getSysChannel();

                  try {
                     String key = b == null ? (String)a : String.format("%s:%s", a, b);
                     byte[] versionBytes = sysChannel.stationCall("module.version", key.getBytes());
                     return new Version(new String(versionBytes));
                  } catch (Exception var14) {
                     return null;
                  }
               }

               return null;
            }

            return this.getConnection().getRemoteVersion();
         case 805:
         case 806:
            useSecureFox = true;
         case 801:
         case 802:
            String stationName = (String)a;
            BHost host = (BHost)b;
            String name = useSecureFox ? "fwFoxs-" + host.getHostname() : "fwFox-" + host.getHostname();
            int defaultPort = useSecureFox ? 4911 : 1911;
            int port = c != null ? (Integer)c : defaultPort;
            if (port != defaultPort) {
               name = name + ":" + port;
            }

            BFoxSession result = new BFoxSession(name, stationName, host, port, useSecureFox);
            Integer fwConnType = x != 801 && x != 805 ? FoxSession.FW_STATION_FOX_SESSION : FoxSession.FW_FOX_SESSION;
            result.connection.fw(803, fwConnType, null, null, null);
            return result;
         default:
            return super.fw(x, a, b, c, d);
      }
   }

   private static class ExecutorServiceHolder {
      private static final ThreadFactory daemonThreadFactory = new ThreadFactory() {
         @Override
         public Thread newThread(Runnable r) {
            Thread t = Executors.defaultThreadFactory().newThread(r);
            t.setDaemon(true);
            t.setName("ActivityMonitor");
            return t;
         }
      };
      private static final ScheduledExecutorService EXECUTOR = Executors.newSingleThreadScheduledExecutor(daemonThreadFactory);
   }
}
