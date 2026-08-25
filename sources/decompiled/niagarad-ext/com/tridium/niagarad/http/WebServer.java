package com.tridium.niagarad.http;

import com.tridium.crypto.core.io.CoreCryptoManager;
import com.tridium.crypto.core.io.CryptoSupport;
import com.tridium.crypto.core.io.ServerCertificateHealth;
import com.tridium.niagarad.NiagaraDaemon;
import com.tridium.niagarad.app.AppRegistry;
import com.tridium.niagarad.crypto.DaemonCryptoManager;
import com.tridium.niagarad.license.Brand;
import com.tridium.niagarad.log.ErrorHandler;
import com.tridium.niagarad.log.MessageBundle;
import com.tridium.niagarad.log.NullLogger;
import com.tridium.niagarad.log.SimpleErrorHandler;
import com.tridium.niagarad.security.AuthenticationInfo;
import com.tridium.niagarad.security.SimpleAuthenticationInfo;
import com.tridium.niagarad.servlet.DebugServlet;
import com.tridium.niagarad.servlet.OutputServlet;
import com.tridium.niagarad.servlet.Servlet;
import com.tridium.nre.jetty.JettyThreadUtil;
import com.tridium.nre.jetty.JettyUtil;
import com.tridium.nre.jetty.log.JavaUtilLogger;
import com.tridium.nre.platform.IPlatformProvider;
import com.tridium.nre.platform.OperatingSystemEnum;
import com.tridium.nre.platform.PlatformUtil;
import com.tridium.nre.security.Aes256PasswordEncoderUtil;
import com.tridium.nre.security.KeyRing;
import com.tridium.nre.security.SecretBytes;
import com.tridium.nre.security.SecretChars;
import com.tridium.nre.security.SecurityInitializer;
import com.tridium.nre.security.SessionKey;
import com.tridium.nre.util.IPAddressUtil;
import com.tridium.nre.util.InterfaceNetworkSettings;
import com.tridium.nre.util.Version;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.AccessController;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.nre.security.ServerTlsParameters;
import javax.baja.nre.security.TlsCipherSuiteGroup;
import javax.baja.nre.util.ByteArrayUtil;
import javax.baja.nre.util.SecurityUtil;
import javax.baja.nre.util.TextUtil;
import javax.servlet.DispatcherType;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.eclipse.jetty.http.CookieCompliance;
import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.io.EndPoint;
import org.eclipse.jetty.io.ManagedSelector;
import org.eclipse.jetty.io.SelectorManager;
import org.eclipse.jetty.server.ConnectionFactory;
import org.eclipse.jetty.server.ConnectionLimit;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpChannel;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.MultiPartFormDataCompliance;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.ServerConnector.ServerConnectorManager;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.InetAccessHandler;
import org.eclipse.jetty.server.handler.SizeLimitHandler;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlets.DoSFilter;
import org.eclipse.jetty.servlets.QoSFilter;
import org.eclipse.jetty.util.IO;
import org.eclipse.jetty.util.MultiException;
import org.eclipse.jetty.util.annotation.Name;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.util.thread.Scheduler;

public class WebServer {
   public static final int WS_STATE_CREATED = 0;
   public static final int WS_STATE_RUNNING = 1;
   public static final int WS_STATE_STOPPING = 2;
   public static final int WS_STATE_STOPPED = 3;
   private int httpPort = 80;
   private int httpsPort = 443;
   private String httpAddress;
   private String httpsAddress;
   private final Logger filter;
   private int state = 0;
   private boolean sslEnabled = false;
   private boolean sslOnly = false;
   private boolean sslMisconfigured = false;
   private static final String OUTPUT_BUFFER_SIZE = "outputBufferSize";
   private int outputBufferSize = 0;
   private static final String OUTPUT_AGGREGATION_SIZE = "outputAggregationSize";
   private int outputAggregationSize = 0;
   private static final Version minimumRequiredClientVersion = new Version("4.4");
   public static final String HTTP_PORT = "port";
   public static final String DEFAULT_HTTP_PORT = "3011";
   private String keyAlias;
   private SecretChars keyPassphrase;
   private String sslAlgType;
   private TlsCipherSuiteGroup tlsCipherSuiteGroup;
   private Authenticator auth = null;
   private Authenticator oldAuth = null;
   private final Object authMonitor = new Object();
   private Servlet defServlet = null;
   private ArrayList<Servlet> servlets = null;
   private static final ArrayList<HttpField> constantHttpHeaders = new ArrayList<>();
   private Server server;
   private ServerConnector httpConnector;
   private ServerConnector httpsConnector;
   private boolean ignoreHttpsSelectFailure = false;
   private static final String HTTP_CONNECTOR_NAME = "httpConnector";
   private static final String HTTPS_CONNECTOR_NAME = "httpsConnector";
   private SessionHandler sessionHandler;
   private WebServer.NiagaraInetAccessHandler inetAccessHandler;
   private SizeLimitHandler sizeLimitHandler;
   private boolean addLocalExemptions = false;
   private DoSFilter dosFilter;
   private QoSFilter qosFilter;
   private ServerCertificateHealth certHealth = new ServerCertificateHealth();
   public static final String LOGIN = "Login";
   public static final String LOGOUT = "Logout";
   public static final String LOGIN_FAILURE = "Login Failure";
   public static final String TIMEOUT = "Logout (Timeout)";
   protected static final int THREAD_IDLE_TIMEOUT_MS = 180000;
   protected static final int AUTHENTICATED_SESSION_IDLE_TIMEOUT_MS = 180000;
   protected static final int UNAUTHENTICATED_SESSION_IDLE_TIMEOUT_MS = 10000;
   public static final long JETTY_STOP_TIMEOUT = 5000L;
   public static final String REFRESH_DAEMON_SESSION_KEY_HEADER = "RefreshDaemonSessionKey";
   private static final int JETTY_ACCEPTOR_THREADS;
   private static final int JETTY_ACCEPTOR_THREAD_DELTA;
   private static final int JETTY_SELECTOR_THREADS;
   private static final int JETTY_MINIMUM_THREADS;
   private static final int JETTY_MAXIMUM_THREADS;
   private static final int JETTY_MAX_FORM_CONTENT_SIZE;
   private static final int JETTY_MAX_FORM_KEYS;
   private static final int JETTY_MAX_HTTP_REQUEST_HEADER_SIZE;
   private static final int JETTY_MAX_HTTP_RESPONSE_HEADER_SIZE;
   private static final int JETTY_HEADER_CACHE_SIZE;
   private static final int JETTY_MAX_HTTP_SESSIONS;
   private static final int JETTY_MIN_REQUEST_DATA_RATE;
   private static final boolean JETTY_MAX_CONNECTIONS_ENABLED = AccessController.doPrivileged(
      () -> Boolean.valueOf(System.getProperty("niagara.daemon.webserver.max.connections.enabled", "true"))
   );
   private static final int JETTY_MAX_CONNECTIONS;
   private static final long JETTY_MAX_CONNECTIONS_IDLE_TIMEOUT;
   private static final long HSTS_MAX_AGE = AccessController.doPrivileged(() -> Long.getLong("hstsMaxAge", 63072000L));
   private static boolean REJECTION_FILTER_ENABLED = AccessController.doPrivileged(
      () -> Boolean.valueOf(System.getProperty("niagara.daemon.webserver.dos.enabled", "true"))
   );
   private static final int REJECTION_FILTER_MAX_REQUESTS_PER_SEC_NO_STATION;
   private static final int REJECTION_FILTER_MAX_REQUESTS_PER_SEC_STATION;
   private static final long REJECTION_FILTER_DELAY_MS;
   private static final String REJECTION_FILTER_IP_WHITELIST = AccessController.doPrivileged(
      () -> System.getProperty("niagara.daemon.webserver.dos.whitelist", "127.0.0.1,::1")
   );
   private static final boolean REJECTION_FILTER_INSERT_HEADERS = AccessController.doPrivileged(
      () -> Boolean.getBoolean("niagara.daemon.webserver.dos.headers")
   );
   private static final long REJECTION_FILTER_MAX_REQUEST_MS;
   private static final long REJECTION_FILTER_MAX_REQUEST_MS_TRIDIUM_QNX;
   private static boolean QOS_FILTER_ENABLED = AccessController.doPrivileged(
      () -> Boolean.valueOf(System.getProperty("niagara.daemon.webserver.qos.enabled", "true"))
   );
   private static final int QOS_FILTER_MAX_REQUESTS_NO_STATION;
   private static final int QOS_FILTER_MAX_REQUESTS_STATION;
   private static String INET_ACCESS_HANDLER_EXCLUDE_LIST = AccessController.doPrivileged(
      () -> System.getProperty("niagara.daemon.webserver.inet.access.exclude")
   );
   private static String INET_ACCESS_HANDLER_INCLUDE_LIST = AccessController.doPrivileged(
      () -> System.getProperty("niagara.daemon.webserver.inet.access.include")
   );
   private static boolean SIZE_LIMIT_HANDLER_ENABLED = AccessController.doPrivileged(
      () -> Boolean.valueOf(System.getProperty("niagara.daemon.webserver.size.limit.enabled", "true"))
   );
   private static final long SIZE_LIMIT_HANDLER_REQUEST_LIMIT;
   private static final long SIZE_LIMIT_HANDLER_RESPONSE_LIMIT;
   private static boolean STATION_HTTPS_SUSTAINED_DOS_RESTART_ENABLED = AccessController.doPrivileged(
      () -> Boolean.valueOf(System.getProperty("niagara.daemon.webserver.dos.sustained.restart.enabled", "true"))
   );
   private static final int STATION_HTTPS_SUSTAINED_DOS_LIMIT;
   private static final long STATION_HTTPS_SUSTAINED_DOS_DURATION_MS;
   private static final long STATION_HTTPS_SUSTAINED_DOS_RESTART_DELAY_MS;
   private static volatile Timer STATION_HTTPS_SUSTAINED_DOS_RESTART_TIMER;
   private static final LinkedList<Long> STATION_HTTPS_SUSTAINED_DOS_TIMESTAMPS = new LinkedList<>();
   private static final Object STATION_HTTPS_SUSTAINED_DOS_LOCK = new Object();
   private static final boolean STATION_HTTPS_NONNIAGARA_DOS_RESTART_ENABLED = AccessController.doPrivileged(
      () -> Boolean.valueOf(System.getProperty("niagara.daemon.webserver.dos.nonniagara.restart.enabled", "true"))
   );
   private static final int STATION_HTTPS_NONNIAGARA_DOS_LIMIT;
   private static final long STATION_HTTPS_NONNIAGARA_DOS_DURATION_MS;
   private static final long STATION_HTTPS_NONNIAGARA_DOS_RESTART_DELAY_MS;
   private static volatile Timer STATION_HTTPS_NONNIAGARA_DOS_RESTART_TIMER;
   private static final LinkedList<Long> STATION_HTTPS_NONNIAGARA_DOS_TIMESTAMPS = new LinkedList<>();
   private static final Object STATION_HTTPS_NONNIAGARA_DOS_LOCK = new Object();
   private static final boolean STATION_HTTPS_NONNIAGARA_DOS_THROTTLE_ENABLED = AccessController.doPrivileged(
      () -> Boolean.valueOf(System.getProperty("niagara.daemon.webserver.dos.nonniagara.throttle.enabled", "true"))
   );
   private static final long STATION_HTTPS_NONNIAGARA_DECAY_MIN = 250L;
   private static final long STATION_HTTPS_NONNIAGARA_DECAY_MAX = 10000L;
   private static volatile long STATION_HTTPS_NONNIAGARA_DECAY_CURRENT = 250L;
   private static final double STATION_HTTPS_NONNIAGARA_DECAY_MULTIPLIER = 1.25;
   private static final Object STATION_HTTPS_NONNIAGARA_DECAY_LOCK = new Object();
   private static volatile WebServer.DecayResetThread STATION_HTTPS_NONNIAGARA_DECAY_RESET_THREAD;
   public static final long MINIMUM_FREE_SPACE_BYTES = 262144L;
   private static final long MILLIS_IN_HOUR = 3600000L;
   private static volatile Timer QNX_ACCEPTOR_THREAD_GC_TIMER = null;

   public WebServer(Properties props, Logger filter) {
      this.filter = filter;
      this.readProperties(props);
      Log.setLog(JavaUtilLogger.getInstance());
   }

   public boolean start(Authenticator auth) {
      this.sslMisconfigured = false;
      this.setAuthenticator(auth);
      if (this.getFilter().isLoggable(Level.FINER)) {
         this.getFilter()
            .finer("creating QueuedThreadPool with min worker threads = " + JETTY_MINIMUM_THREADS + ", max worker threads = " + JETTY_MAXIMUM_THREADS);
      }

      QueuedThreadPool threadPool = new QueuedThreadPool();
      threadPool.setIdleTimeout(180000);
      threadPool.setName("Daemon:WS");
      this.server = new Server(threadPool);
      if (this.getFilter().isLoggable(Level.FINER)) {
         this.getFilter().finer("creating Server with max form content size = " + JETTY_MAX_FORM_CONTENT_SIZE + ", max form keys = " + JETTY_MAX_FORM_KEYS);
      }

      this.server.setAttribute("org.eclipse.jetty.server.Request.maxFormContentSize", JETTY_MAX_FORM_CONTENT_SIZE);
      this.server.setAttribute("org.eclipse.jetty.server.Request.maxFormKeys", JETTY_MAX_FORM_KEYS);
      HttpConfiguration config = JettyUtil.makeBasicHttpConfiguration();
      if (this.outputBufferSize > 0) {
         config.setOutputBufferSize(this.outputBufferSize);
      }

      if (this.outputAggregationSize > 0) {
         config.setOutputAggregationSize(this.outputAggregationSize);
      }

      config.setFormEncodedMethods(new String[0]);
      config.setRequestCookieCompliance(CookieCompliance.RFC6265);
      config.setMultiPartFormDataCompliance(MultiPartFormDataCompliance.RFC7578);
      config.setNotifyRemoteAsyncErrors(false);
      boolean persistentConnectionRequired = true;
      if (this.getFilter().isLoggable(Level.FINER)) {
         this.getFilter().finer("creating Server with persistent connections enabled = " + persistentConnectionRequired);
      }

      config.setPersistentConnectionsEnabled(persistentConnectionRequired);
      if (this.getFilter().isLoggable(Level.FINER)) {
         this.getFilter().finer("creating Server with minimum request data rate = " + JETTY_MIN_REQUEST_DATA_RATE + " byte/sec");
      }

      config.setMinRequestDataRate(JETTY_MIN_REQUEST_DATA_RATE);
      if (this.getFilter().isLoggable(Level.FINER)) {
         this.getFilter()
            .finer(
               "creating Server with max request header size = "
                  + JETTY_MAX_HTTP_REQUEST_HEADER_SIZE
                  + ", max response header size = "
                  + JETTY_MAX_HTTP_RESPONSE_HEADER_SIZE
            );
      }

      config.setRequestHeaderSize(JETTY_MAX_HTTP_REQUEST_HEADER_SIZE);
      config.setResponseHeaderSize(JETTY_MAX_HTTP_RESPONSE_HEADER_SIZE);
      if (this.getFilter().isLoggable(Level.FINER)) {
         this.getFilter().finer("creating Server with header cache size = " + JETTY_HEADER_CACHE_SIZE);
      }

      config.setHeaderCacheSize(JETTY_HEADER_CACHE_SIZE);
      if (this.getFilter().isLoggable(Level.FINER)) {
         this.getFilter()
            .finer(
               "creating ServerConnector(s) with acceptor threads argument = "
                  + JETTY_ACCEPTOR_THREADS
                  + ", selector threads argument = "
                  + JETTY_SELECTOR_THREADS
            );
      }

      if (this.isSSLEnabled()) {
         try {
            if (this.filter.isLoggable(Level.FINE)) {
               this.filter.fine("creating TLS socket factory (algType:" + this.sslAlgType + ", sslOnly:" + this.isSSLOnly() + ")");
            }

            config.setSecureScheme("https");
            config.setSecurePort(this.httpsPort);
            String protocol;
            if (SecurityInitializer.getInstance().isFips()) {
               if (!DaemonCryptoManager.isTlsAlgFipsApproved(this.sslAlgType)) {
                  this.filter.warning("forcing ssl socket to use " + (String)CryptoSupport.TYPES.get("tlsv1_3") + " due to FIPS mode enabled");
               }

               protocol = "tlsv1_3";
            } else {
               protocol = this.sslAlgType.toLowerCase();
            }

            CoreCryptoManager ccm = CoreCryptoManager.get(NiagaraDaemon.getSecurityInfoProvider());
            String resolvedAlias = null;
            SecretChars certPasswordChars = null;
            if (this.keyPassphrase != null) {
               if (this.filter.isLoggable(Level.FINE)) {
                  this.filter.fine("using cert password to retrieve certificate " + this.keyAlias);
               }

               this.certHealth = ccm.checkServerCertificateStatus(this.keyAlias, this.keyPassphrase, this.filter);
               resolvedAlias = this.certHealth.getReturnedCert();
               if (this.keyAlias.equals(resolvedAlias)) {
                  certPasswordChars = this.keyPassphrase.newCopy();
               }

               if ("default".equals(resolvedAlias)) {
                  certPasswordChars = null;
               }
            } else {
               if ("tridium".equals(this.keyAlias) && !ccm.getKeyStore().containsAlias("tridium")) {
                  this.filter.info("legacy alias 'tridium' not found, switching to use 'default'");
                  this.keyAlias = "default";
                  NiagaraDaemon.props.setProperty("keyAlias", "default");
                  NiagaraDaemon.saveProperties();
               }

               this.certHealth = ccm.checkServerCertificateStatus(this.keyAlias, null, this.filter);
               resolvedAlias = this.certHealth.getReturnedCert();
            }

            if ("tridium".equals(resolvedAlias) || "default".equals(resolvedAlias)) {
               this.filter.warning("using default TLS server certificate '" + resolvedAlias + "' is not recommended");
            }

            ServerTlsParameters tlsParams = new ServerTlsParameters(protocol, resolvedAlias, this.tlsCipherSuiteGroup);
            if (certPasswordChars != null) {
               tlsParams.setKeyPassphrase(certPasswordChars.get());
            }

            if (this.tlsCipherSuiteGroup != TlsCipherSuiteGroup.recommended) {
               this.filter.warning("not using recommended tls cipher suite group, using " + tlsParams);
            } else if (this.filter.isLoggable(Level.FINE)) {
               this.filter.fine("using " + tlsParams);
            }

            SslContextFactory contextFactory = ccm.getSslContextFactory(tlsParams);
            HttpConfiguration configs = new HttpConfiguration(config);
            SecureRequestCustomizer customizer = new SecureRequestCustomizer();
            customizer.setStsMaxAge(HSTS_MAX_AGE);
            configs.addCustomizer(customizer);
            if (JETTY_ACCEPTOR_THREADS > 0 && OperatingSystemEnum.isOS(OperatingSystemEnum.qnx) && QNX_ACCEPTOR_THREAD_GC_TIMER == null) {
               if (this.filter.isLoggable(Level.FINER)) {
                  this.filter.finer("Starting GC task for acceptor thread usage");
               }

               QNX_ACCEPTOR_THREAD_GC_TIMER = new Timer("Daemon:WS-acceptor-gc", true);
               QNX_ACCEPTOR_THREAD_GC_TIMER.scheduleAtFixedRate(new WebServer.GCTimerTask(), 43200000L, 43200000L);
            }

            this.httpsConnector = new WebServer.NiagaraCustomConnector(
               this.server,
               JETTY_ACCEPTOR_THREADS,
               JETTY_SELECTOR_THREADS,
               this.httpsPort,
               true,
               new SslConnectionFactory(contextFactory, HttpVersion.HTTP_1_1.toString()),
               new HttpConnectionFactory(configs)
            );
            this.httpsConnector.setPort(this.httpsPort);
            this.httpsConnector.setIdleTimeout(180000L);
            this.httpsConnector.setAcceptorPriorityDelta(JETTY_ACCEPTOR_THREAD_DELTA);
            this.httpsConnector.setName("httpsConnector");
            if (this.getFilter().isLoggable(Level.FINER)) {
               this.getFilter()
                  .finer(
                     "creating secure ServerConnector with acceptor threads = "
                        + this.httpsConnector.getAcceptors()
                        + " (delta = "
                        + this.httpsConnector.getAcceptorPriorityDelta()
                        + "), selector threads = "
                        + this.httpsConnector.getSelectorManager().getSelectorCount()
                  );
            }
         } catch (Exception e) {
            this.filter.log(Level.SEVERE, "TLS connector could not be started", e);
            this.sslMisconfigured = true;
            if (this.isSSLEnabled() && this.isSSLOnly()) {
               this.filter.severe("TLS misconfiguration for TLS only platform, remote platform connections will be disabled until TLS error is corrected");
            }
         }
      }

      if (JETTY_ACCEPTOR_THREADS > 0 && OperatingSystemEnum.isOS(OperatingSystemEnum.qnx) && QNX_ACCEPTOR_THREAD_GC_TIMER == null) {
         if (this.filter.isLoggable(Level.FINER)) {
            this.filter.finer("Starting GC task for acceptor thread usage");
         }

         QNX_ACCEPTOR_THREAD_GC_TIMER = new Timer("Daemon:WS-acceptor-gc", true);
         QNX_ACCEPTOR_THREAD_GC_TIMER.scheduleAtFixedRate(new WebServer.GCTimerTask(), 43200000L, 43200000L);
      }

      this.httpConnector = new WebServer.NiagaraCustomConnector(
         this.server, JETTY_ACCEPTOR_THREADS, JETTY_SELECTOR_THREADS, this.httpPort, false, new HttpConnectionFactory(config)
      );
      this.httpConnector.setPort(this.httpPort);
      this.httpConnector.setIdleTimeout(180000L);
      this.httpConnector.setAcceptorPriorityDelta(JETTY_ACCEPTOR_THREAD_DELTA);
      this.httpConnector.setName("httpConnector");
      if (this.getFilter().isLoggable(Level.FINER)) {
         this.getFilter()
            .finer(
               "creating ServerConnector with accept threads = "
                  + this.httpConnector.getAcceptors()
                  + " (delta = "
                  + this.httpConnector.getAcceptorPriorityDelta()
                  + "), selector threads = "
                  + this.httpConnector.getSelectorManager().getSelectorCount()
            );
      }

      if (this.isSSLEnabled() && this.isSSLOnly()) {
         this.httpConnector.setHost("localhost");
      }

      this.startServlets();
      ServletContextHandler outputServletsContextHandler = new ServletContextHandler();
      outputServletsContextHandler.setContextPath("/");
      ServletHolder daemonOutputHolder = new ServletHolder("getdaemonoutput", OutputServlet.class);
      outputServletsContextHandler.addServlet(daemonOutputHolder, "/getdaemonoutput");
      if (NiagaraDaemon.getInstance().getStationRegistry() != null) {
         String appType = NiagaraDaemon.getInstance().getStationRegistry().getAppType();
         ServletHolder stationOutputHolder = new ServletHolder(appType, OutputServlet.class);
         outputServletsContextHandler.addServlet(stationOutputHolder, "/" + appType);
      }

      ServletContextHandler filterContextHandler = new ServletContextHandler();
      filterContextHandler.setContextPath("/");
      WebServer.PrerequisiteServletHandler prerequisiteServletHandler = new WebServer.PrerequisiteServletHandler();
      prerequisiteServletHandler.addServletWithMapping(WebServer.PrerequisiteServlet.class, "/");
      filterContextHandler.setServletHandler(prerequisiteServletHandler);
      boolean anyApplicationRunning = false;
      AppRegistry registry = NiagaraDaemon.getInstance().getStationRegistry();
      if (registry != null && registry.appRunning()) {
         anyApplicationRunning = true;
      }

      FilterHolder dosHolder = null;

      try {
         if (REJECTION_FILTER_ENABLED) {
            FilterHolder temporaryDoSHolder = new FilterHolder(DoSFilter.class);
            temporaryDoSHolder.setInitParameter(
               "maxRequestsPerSec",
               anyApplicationRunning
                  ? String.valueOf(REJECTION_FILTER_MAX_REQUESTS_PER_SEC_STATION)
                  : String.valueOf(REJECTION_FILTER_MAX_REQUESTS_PER_SEC_NO_STATION)
            );
            temporaryDoSHolder.setInitParameter("delayMs", String.valueOf(REJECTION_FILTER_DELAY_MS));
            temporaryDoSHolder.setInitParameter("ipWhitelist", REJECTION_FILTER_IP_WHITELIST);
            temporaryDoSHolder.setInitParameter("insertHeaders", String.valueOf(REJECTION_FILTER_INSERT_HEADERS));
            long maxRequestsMs = PlatformUtil.isTridiumPlatform() && OperatingSystemEnum.isOS(OperatingSystemEnum.qnx)
               ? REJECTION_FILTER_MAX_REQUEST_MS_TRIDIUM_QNX
               : REJECTION_FILTER_MAX_REQUEST_MS;
            temporaryDoSHolder.setInitParameter("maxRequestMs", String.valueOf(maxRequestsMs));
            if (this.getFilter().isLoggable(Level.FINER)) {
               this.getFilter()
                  .finer(
                     "creating DoSFilter with maxRequestsPerSec = "
                        + REJECTION_FILTER_MAX_REQUESTS_PER_SEC_NO_STATION
                        + " ("
                        + REJECTION_FILTER_MAX_REQUESTS_PER_SEC_STATION
                        + "), delayMs = "
                        + REJECTION_FILTER_DELAY_MS
                        + ", ipWhitelist = '"
                        + REJECTION_FILTER_IP_WHITELIST
                        + "', insetHeaders = "
                        + REJECTION_FILTER_INSERT_HEADERS
                        + ", maxRequestMs = "
                        + maxRequestsMs
                  );
            }

            if (STATION_HTTPS_SUSTAINED_DOS_RESTART_ENABLED && this.getFilter().isLoggable(Level.FINER)) {
               this.getFilter()
                  .finer(
                     "enforcing sustained HTTPS DoS restart handler with limit = "
                        + STATION_HTTPS_SUSTAINED_DOS_LIMIT
                        + ", duration = "
                        + STATION_HTTPS_SUSTAINED_DOS_DURATION_MS
                        + "ms, restartDelay = "
                        + STATION_HTTPS_SUSTAINED_DOS_RESTART_DELAY_MS
                        + "ms"
                  );
            }

            filterContextHandler.addFilter(temporaryDoSHolder, "/*", EnumSet.of(DispatcherType.REQUEST));
            dosHolder = temporaryDoSHolder;
         }
      } catch (Exception exception) {
         REJECTION_FILTER_ENABLED = false;
         STATION_HTTPS_SUSTAINED_DOS_RESTART_ENABLED = false;
         this.getFilter().log(Level.WARNING, "Exception occurred during RejectionFilter initialization, ignoring RejectionFilter usage", exception);
      }

      if (STATION_HTTPS_NONNIAGARA_DOS_RESTART_ENABLED) {
         if (this.getFilter().isLoggable(Level.FINER)) {
            this.getFilter()
               .finer(
                  "enforcing non-Niagara HTTPS DoS restart handler with limit = "
                     + STATION_HTTPS_NONNIAGARA_DOS_LIMIT
                     + ", duration = "
                     + STATION_HTTPS_NONNIAGARA_DOS_DURATION_MS
                     + "ms, restartDelay = "
                     + STATION_HTTPS_NONNIAGARA_DOS_RESTART_DELAY_MS
                     + "ms"
               );
         }
      } else if (STATION_HTTPS_NONNIAGARA_DOS_THROTTLE_ENABLED && this.getFilter().isLoggable(Level.FINER)) {
         this.getFilter().finer("enforcing non-Niagara HTTPS DoS throttle handler with delayMultiplier = 1.25, maxDelay = 10000ms");
      }

      FilterHolder qosHolder = null;

      try {
         if (QOS_FILTER_ENABLED) {
            FilterHolder temporaryQoSHolder = new FilterHolder(WebServer.NiagaraQoSFilter.class);
            temporaryQoSHolder.setInitParameter(
               "maxRequests", anyApplicationRunning ? String.valueOf(QOS_FILTER_MAX_REQUESTS_STATION) : String.valueOf(QOS_FILTER_MAX_REQUESTS_NO_STATION)
            );
            if (this.getFilter().isLoggable(Level.FINER)) {
               this.getFilter()
                  .finer("creating QoSFilter with maxRequests = " + QOS_FILTER_MAX_REQUESTS_NO_STATION + " (" + QOS_FILTER_MAX_REQUESTS_STATION + ")");
            }

            filterContextHandler.addFilter(temporaryQoSHolder, "/*", EnumSet.of(DispatcherType.REQUEST));
            qosHolder = temporaryQoSHolder;
         }
      } catch (Exception exception) {
         QOS_FILTER_ENABLED = false;
         this.getFilter().log(Level.WARNING, "Exception occurred during QoSFilter initialization, ignoring QoSFilter usage", exception);
      }

      this.sessionHandler = new SessionHandler();
      this.sessionHandler.setMaxInactiveInterval(10);
      this.sessionHandler.getSessionCookieConfig().setName("NIAGARA_DAEMON_SESSION_ID");
      this.sessionHandler.getSessionCookieConfig().setComment("__SAME_SITE_STRICT__");
      if (!this.getServlet("login").getName().equals("login")) {
         this.sessionHandler.getSessionCookieConfig().setHttpOnly(true);
      } else {
         this.sessionHandler.getSessionCookieConfig().setHttpOnly(!(this.getAuthenticator() instanceof ScramAuthenticator) || !DebugServlet.debugEnabled);
      }

      this.sessionHandler.setSessionIdManager(new PlatformSessionIdManager(this.server));
      this.sessionHandler.addEventListener(this.getAuthenticator());

      try {
         if (INET_ACCESS_HANDLER_INCLUDE_LIST != null && !INET_ACCESS_HANDLER_INCLUDE_LIST.trim().isEmpty()
            || INET_ACCESS_HANDLER_EXCLUDE_LIST != null && !INET_ACCESS_HANDLER_EXCLUDE_LIST.trim().isEmpty()) {
            String includeListOrEmpty = Optional.ofNullable(INET_ACCESS_HANDLER_INCLUDE_LIST).orElse("").trim();
            String excludeListOrEmpty = Optional.ofNullable(INET_ACCESS_HANDLER_EXCLUDE_LIST).orElse("").trim();
            if (this.getFilter().isLoggable(Level.FINER)) {
               this.getFilter()
                  .finer(
                     "creating InetAccessHandler with include list argument = '"
                        + includeListOrEmpty
                        + "' exclude list argument = '"
                        + excludeListOrEmpty
                        + "'"
                  );
            }

            WebServer.NiagaraInetAccessHandler temporaryInetAddressHandler = new WebServer.NiagaraInetAccessHandler();
            Set<String> excludes = new LinkedHashSet<>(Arrays.asList(TextUtil.split(excludeListOrEmpty, ',')));
            Set<String> includes = new LinkedHashSet<>(Arrays.asList(TextUtil.split(includeListOrEmpty, ',')));
            IPAddressUtil.clearLocalHostCache();
            IPAddressUtil.getLocalHost();
            IPAddressUtil.getLocalHost(null);
            Set<String> localAdapters = new LinkedHashSet<>(Arrays.asList("127.0.0.1", "::1"));

            for (InterfaceNetworkSettings localAdapter : IPAddressUtil.getLocalHostInterfaces()) {
               String localAddress = localAdapter.getInetAddress().getHostAddress();
               localAdapters.add(localAddress);
            }

            excludes.removeAll(localAdapters);
            includes.removeAll(localAdapters);
            this.addLocalExemptions = false;
            if (includes.isEmpty() && !excludes.isEmpty()) {
               this.addLocalExemptions = false;
            } else if (!includes.isEmpty() && excludes.isEmpty()) {
               excludes.addAll(Arrays.asList("0.0.0.0", "::"));
               this.addLocalExemptions = true;
            } else if (includes.isEmpty() && excludes.isEmpty()) {
               this.addLocalExemptions = false;
            } else {
               this.addLocalExemptions = true;
            }

            if (this.addLocalExemptions) {
               includes.addAll(localAdapters);
            }

            if (this.getFilter().isLoggable(Level.FINER)) {
               this.getFilter()
                  .finer("creating InetAccessHandler with include list = '" + (includes.isEmpty() ? "[*]" : includes) + "', exclude list = '" + excludes + "'");
            }

            if (!includes.isEmpty()) {
               temporaryInetAddressHandler.include(includes.toArray(new String[0]));
            }

            if (!excludes.isEmpty()) {
               temporaryInetAddressHandler.exclude(excludes.toArray(new String[0]));
            }

            temporaryInetAddressHandler.includeConnector(this.httpsConnector.getName());
            if (!"localhost".equals(this.httpConnector.getHost())) {
               temporaryInetAddressHandler.includeConnector(this.httpConnector.getName());
            }

            this.inetAccessHandler = temporaryInetAddressHandler;
         }
      } catch (Exception exception) {
         INET_ACCESS_HANDLER_INCLUDE_LIST = null;
         INET_ACCESS_HANDLER_EXCLUDE_LIST = null;
         this.getFilter().log(Level.WARNING, "Exception occurred during InetAccessHandler initialization, ignoring InetAccessHandler usage", exception);
      }

      try {
         if (SIZE_LIMIT_HANDLER_ENABLED) {
            if (this.getFilter().isLoggable(Level.FINER)) {
               this.getFilter()
                  .finer(
                     "creating SizeLimitHandler with request limit = "
                        + SIZE_LIMIT_HANDLER_REQUEST_LIMIT
                        + ", response limit = "
                        + SIZE_LIMIT_HANDLER_RESPONSE_LIMIT
                  );
            }

            this.sizeLimitHandler = new SizeLimitHandler(SIZE_LIMIT_HANDLER_REQUEST_LIMIT, SIZE_LIMIT_HANDLER_RESPONSE_LIMIT);
         }
      } catch (Exception exception) {
         SIZE_LIMIT_HANDLER_ENABLED = false;
         this.getFilter().log(Level.WARNING, "Exception occurred during SizeLimitHandler initialization, ignoring SizeLimitHandler usage", exception);
      }

      ArrayList<Handler> handlerList = new ArrayList<>();
      if (this.inetAccessHandler != null) {
         handlerList.add(this.inetAccessHandler);
      }

      if (this.sizeLimitHandler != null) {
         handlerList.add(this.sizeLimitHandler);
      }

      filterContextHandler.setSessionHandler(this.sessionHandler);
      handlerList.add(filterContextHandler);
      handlerList.add(new WebServer.NiagaraRequestHandler());
      handlerList.add(outputServletsContextHandler);
      HandlerList handlers = new HandlerList();
      handlers.setHandlers(handlerList.toArray(new Handler[0]));
      this.server.setHandler(handlers);
      this.server.setStopTimeout(5000L);

      try {
         this.server.start();
         if (dosHolder != null) {
            this.dosFilter = (DoSFilter)dosHolder.getFilter();
         }

         if (qosHolder != null) {
            this.qosFilter = (QoSFilter)qosHolder.getFilter();
         }

         this.server.addConnector(this.httpConnector);
         if (JETTY_MAX_CONNECTIONS_ENABLED && !"localhost".equals(this.httpConnector.getHost())) {
            if (this.getFilter().isLoggable(Level.FINER)) {
               this.getFilter()
                  .finer(
                     "creating HTTP ConnectionLimit with maxConnections = " + JETTY_MAX_CONNECTIONS + ", idleTimeout = " + JETTY_MAX_CONNECTIONS_IDLE_TIMEOUT
                  );
            }

            ConnectionLimit httpConnectionLimit = new WebServer.NiagaraConnectionLimit(JETTY_MAX_CONNECTIONS, this.httpConnector);
            httpConnectionLimit.setIdleTimeout(JETTY_MAX_CONNECTIONS_IDLE_TIMEOUT);
            this.server.addBean(httpConnectionLimit);
            httpConnectionLimit.start();
         }

         if (this.filter.isLoggable(Level.FINE)) {
            this.filter.fine("starting HTTP connector on port " + this.httpConnector.getPort());
         }

         this.httpConnector.start();
         String host = this.httpConnector.getHost();
         int port = this.httpConnector.getPort();
         InetSocketAddress bindAddress = host == null ? new InetSocketAddress(port) : new InetSocketAddress(host, port);
         this.httpAddress = bindAddress.getAddress().getHostAddress();
         if (this.filter.isLoggable(Level.FINE)) {
            this.filter.fine("HTTP connector listening on address " + this.httpAddress);
         }

         if (this.httpsConnector != null && this.isSSLEnabled() && !this.isSSLMisconfigured()) {
            this.server.addConnector(this.httpsConnector);
            if (JETTY_MAX_CONNECTIONS_ENABLED) {
               if (this.getFilter().isLoggable(Level.FINER)) {
                  this.getFilter()
                     .finer(
                        "creating HTTPS ConnectionLimit with maxConnections = "
                           + JETTY_MAX_CONNECTIONS
                           + ", idleTimeout = "
                           + JETTY_MAX_CONNECTIONS_IDLE_TIMEOUT
                     );
               }

               ConnectionLimit httpsConnectionLimit = new WebServer.NiagaraConnectionLimit(JETTY_MAX_CONNECTIONS, this.httpsConnector);
               httpsConnectionLimit.setIdleTimeout(JETTY_MAX_CONNECTIONS_IDLE_TIMEOUT);
               this.server.addBean(httpsConnectionLimit);
               httpsConnectionLimit.start();
            }

            if (this.filter.isLoggable(Level.FINE)) {
               this.filter.fine("starting HTTPS connector on port " + this.httpsConnector.getPort());
            }

            this.httpsConnector.start();
            host = this.httpsConnector.getHost();
            port = this.httpsConnector.getPort();
            bindAddress = host == null ? new InetSocketAddress(port) : new InetSocketAddress(host, port);
            this.httpsAddress = bindAddress.getAddress().getHostAddress();
            if (this.filter.isLoggable(Level.FINE)) {
               this.filter.fine("HTTPS connector listening on address " + this.httpsAddress);
            }
         }
      } catch (Exception e) {
         if (e instanceof MultiException) {
            MultiException me = (MultiException)e;

            for (Throwable throwable : me.getThrowables()) {
               this.filter.severe("error starting the webserver (" + throwable + ")");
               if (this.filter.isLoggable(Level.FINE)) {
                  this.filter.log(Level.SEVERE, "Stack trace: ", throwable);
               }
            }
         } else {
            this.filter.severe("error starting the webserver (" + e + ")");
            if (this.filter.isLoggable(Level.FINE)) {
               this.filter.log(Level.SEVERE, "Stack trace: ", e);
            }
         }

         return false;
      }

      int connectorThreadBaseline = this.httpConnector.getAcceptors()
         + this.httpConnector.getSelectorManager().getSelectorCount()
         + (this.httpsConnector != null ? this.httpsConnector.getAcceptors() : 0)
         + (this.httpsConnector != null ? this.httpsConnector.getSelectorManager().getSelectorCount() : 0);
      threadPool.setMinThreads(JETTY_MINIMUM_THREADS + connectorThreadBaseline);
      threadPool.setMaxThreads(JETTY_MAXIMUM_THREADS + connectorThreadBaseline);
      int numberOfThreads = threadPool.getThreads();
      this.filter
         .info("web server thread" + (numberOfThreads > 0 ? "s " : " ") + "started [threadCount = " + (numberOfThreads - connectorThreadBaseline) + "]");
      return true;
   }

   public void stop() {
      this.state = 2;
      if (STATION_HTTPS_SUSTAINED_DOS_RESTART_ENABLED && STATION_HTTPS_SUSTAINED_DOS_RESTART_TIMER != null) {
         STATION_HTTPS_SUSTAINED_DOS_RESTART_TIMER.cancel();
         STATION_HTTPS_SUSTAINED_DOS_RESTART_TIMER = null;
      }

      if (STATION_HTTPS_NONNIAGARA_DOS_RESTART_ENABLED && STATION_HTTPS_NONNIAGARA_DOS_RESTART_TIMER != null) {
         STATION_HTTPS_NONNIAGARA_DOS_RESTART_TIMER.cancel();
         STATION_HTTPS_NONNIAGARA_DOS_RESTART_TIMER = null;
      }

      if (QNX_ACCEPTOR_THREAD_GC_TIMER != null) {
         QNX_ACCEPTOR_THREAD_GC_TIMER.cancel();
         QNX_ACCEPTOR_THREAD_GC_TIMER = null;
      }

      this.stopServlets();

      try {
         if (this.server != null) {
            try {
               for (Connector connector : this.server.getConnectors()) {
                  if (connector instanceof ServerConnector) {
                     ServerConnector serverConnector = (ServerConnector)connector;
                     serverConnector.getSelectorManager().stop();
                  }
               }
            } catch (Exception var6) {
            }

            this.server.stop();
            this.server.join();
         }
      } catch (Exception e) {
         this.filter.log(Level.SEVERE, "error stopping the webserver (" + e + ")", e);
      }

      this.sessionHandler = null;
      this.dosFilter = null;
      this.qosFilter = null;
      this.inetAccessHandler = null;
      this.sizeLimitHandler = null;
      this.addLocalExemptions = false;
      this.httpConnector = null;
      this.httpsConnector = null;
      STATION_HTTPS_SUSTAINED_DOS_TIMESTAMPS.clear();
      STATION_HTTPS_NONNIAGARA_DOS_TIMESTAMPS.clear();
      this.state = 3;
   }

   private void readProperties(Properties props) {
      SimpleErrorHandler simpleErrorHandler = new SimpleErrorHandler();
      if (Boolean.parseBoolean(DaemonCryptoManager.SSL_ENABLED_READONLY_VALUE)) {
         this.sslEnabled = Boolean.parseBoolean("true");
         this.sslOnly = Boolean.parseBoolean(DaemonCryptoManager.DEFAULT_SSL_ONLY);
         String tempValue = props.getProperty("sslEnabled", "true");
         if (!tempValue.equals("true")) {
            String logMessage = "invalid \"sslEnabled\" value \"" + tempValue + "\" when TLS readonly, using \"" + "true" + "\"";
            simpleErrorHandler.error(logMessage);
            props.setProperty("sslEnabled", "true");
         }

         tempValue = props.getProperty("sslOnly", DaemonCryptoManager.DEFAULT_SSL_ONLY);
         if (!tempValue.equals(DaemonCryptoManager.DEFAULT_SSL_ONLY)) {
            String logMessage = "invalid \"sslOnly\" value \"" + tempValue + "\" when TLS readonly, using \"" + DaemonCryptoManager.DEFAULT_SSL_ONLY + "\"";
            simpleErrorHandler.error(logMessage);
            props.setProperty("sslOnly", DaemonCryptoManager.DEFAULT_SSL_ONLY);
         }
      } else {
         this.sslEnabled = Boolean.parseBoolean(props.getProperty("sslEnabled", "true"));
         this.sslOnly = Boolean.parseBoolean(props.getProperty("sslOnly", DaemonCryptoManager.DEFAULT_SSL_ONLY));
         if (props.getProperty("sslOnly") == null) {
            props.setProperty("sslOnly", String.valueOf(this.sslOnly));
         }
      }

      if (this.isSSLEnabled()) {
         this.keyAlias = checkAlias(props, "keyAlias", "tridium", this.filter, simpleErrorHandler);
         this.keyPassphrase = checkPassphrase(props, "keyPassphrase", this.keyAlias, this.filter, simpleErrorHandler);
         this.sslAlgType = checkAlgorithm(props, "sslAlgType", "tlsv1_3", this.filter, simpleErrorHandler);
         String requireStrongCipherSuites = props.getProperty("requireStrongCipherSuites");
         if (requireStrongCipherSuites != null) {
            props.remove("requireStrongCipherSuites");
            if ("false".equalsIgnoreCase(requireStrongCipherSuites)) {
               this.tlsCipherSuiteGroup = TlsCipherSuiteGroup.supported;
               props.setProperty("tlsCipherSuiteGroup", this.tlsCipherSuiteGroup.name());
            } else {
               this.tlsCipherSuiteGroup = TlsCipherSuiteGroup.getEnum(DaemonCryptoManager.DEFAULT_TLS_CIPHER_SUITE_GROUP);
            }

            String logMessage = "legacy \"requireStrongCipherSuites\" value \""
               + requireStrongCipherSuites
               + "\" found, migrated to key \""
               + "tlsCipherSuiteGroup"
               + "\"";
            simpleErrorHandler.error(logMessage);
         } else {
            this.tlsCipherSuiteGroup = TlsCipherSuiteGroup.getEnum(props.getProperty("tlsCipherSuiteGroup", DaemonCryptoManager.DEFAULT_TLS_CIPHER_SUITE_GROUP));
         }
      }

      this.outputBufferSize = Integer.parseInt(props.getProperty("outputBufferSize", "0"));
      this.outputAggregationSize = Integer.parseInt(props.getProperty("outputAggregationSize", "0"));
      this.httpPort = checkPort(props, "port", "3011", this.filter, simpleErrorHandler);
      this.httpsPort = checkPort(props, "sslPort", "5011", this.filter, simpleErrorHandler);
      if (simpleErrorHandler.getLastError() != null) {
         NiagaraDaemon.saveProperties();
      }
   }

   public static int checkPort(Properties props, String key, String defaultPort, Logger filter, ErrorHandler errorHandler) {
      String portStringValue = null;

      int portIntValue;
      try {
         portStringValue = props.getProperty(key, defaultPort);
         portIntValue = Integer.parseInt(portStringValue);
         if (portIntValue <= 1024 || portIntValue > 65535) {
            throw new NumberFormatException();
         }
      } catch (NumberFormatException nfe) {
         String logMessage = "invalid \"" + key + "\" value \"" + portStringValue + "\" specified, using default \"" + defaultPort + "\"";
         filter.severe(logMessage);
         errorHandler.error(logMessage);
         portIntValue = Integer.parseInt(defaultPort);
         props.setProperty(key, defaultPort);
      }

      return portIntValue;
   }

   public static String checkAlias(Properties props, String key, String defaultAlias, Logger filter, ErrorHandler errorHandler) {
      String aliasValue = props.getProperty(key, defaultAlias);
      String lowerCaseAlias = aliasValue.toLowerCase(Locale.ENGLISH);
      if (!aliasValue.equals(lowerCaseAlias)) {
         String logMessage = "invalid \"" + key + "\" case value \"" + aliasValue + "\" specified, using \"" + lowerCaseAlias + "\"";
         filter.severe(logMessage);
         errorHandler.error(logMessage);
         aliasValue = lowerCaseAlias;
         props.setProperty(key, aliasValue);
      }

      return aliasValue;
   }

   public static SecretChars checkPassphrase(Properties props, String key, String alias, Logger filter, ErrorHandler errorHandler) {
      try {
         String keyPassValue = props.getProperty(key, null);
         if (keyPassValue == null) {
            return null;
         }

         KeyRing keyRing = SecurityInitializer.getInstance().getSecurityInfoProvider().getKeyRing();
         return Aes256PasswordEncoderUtil.decodePassword(keyRing, "com.tridium.niagarad.web.sslKeyPass", keyPassValue);
      } catch (Exception e) {
         filter.log(Level.SEVERE, "unable to decode the password from the properties bundle", e);
         errorHandler.error("unable to decode the password from the properties bundle");
         return null;
      }
   }

   public static String checkAlgorithm(Properties props, String key, String defaultAlgorithm, Logger filter, ErrorHandler errorHandler) {
      String algorithmValue = props.getProperty(key, defaultAlgorithm);
      if (CryptoSupport.TYPES.get(algorithmValue) == null) {
         String logMessage = "invalid \"" + key + "\" algorithm value \"" + algorithmValue + "\" specified, using \"" + defaultAlgorithm + "\"";
         filter.severe(logMessage);
         errorHandler.error(logMessage);
         algorithmValue = defaultAlgorithm;
         props.setProperty(key, algorithmValue);
      }

      return algorithmValue;
   }

   public boolean restart(Authenticator auth, Properties props) {
      this.stop();
      this.readProperties(props);
      boolean success = this.start(auth);
      if (NiagaraDaemon.getInstance().getStationRegistry() != null) {
         NiagaraDaemon.getInstance().getStationRegistry().resetDaemonSessions();
      }

      return success;
   }

   public int getState() {
      return this.state;
   }

   public static void addConstantHttpHeader(String name, String value) {
      if (value != null && name != null) {
         constantHttpHeaders.add(new HttpField(name, value));
      }
   }

   public boolean authenticate(HttpServletRequest req, HttpServletResponse resp) {
      Authenticator auth = this.getAuthenticator();
      if (auth == null) {
         return false;
      }

      AuthenticationInfo result = auth.makeAuthInfo(req, resp);
      if (result != null) {
         HttpSession requestSession = req.getSession(false);
         if (requestSession == null) {
            if (this.getFilter().isLoggable(Level.FINE)) {
               this.getFilter().fine("failed to generate new session key, request does not have a session");
            }

            return false;
         }

         if (auth.supportsSecureKeyExchange() && auth.keyExchangeEnabled(requestSession)) {
            byte[] extractedSessionKey = auth.extractSessionKey(requestSession);
            if (extractedSessionKey != null) {
               SecretBytes secret = new SecretBytes(extractedSessionKey, true);
               Throwable var22 = null;

               try {
                  requestSession.setAttribute("sessionKey", new SessionKey(secret, auth.getEncryptionAlgorithmBundle(requestSession)));
               } catch (Throwable var17) {
                  var22 = var17;
                  throw var17;
               } finally {
                  if (secret != null) {
                     if (var22 != null) {
                        try {
                           secret.close();
                        } catch (Throwable var16) {
                           var22.addSuppressed(var16);
                        }
                     } else {
                        secret.close();
                     }
                  }
               }
            }
         } else {
            SessionKey sessionKey = (SessionKey)requestSession.getAttribute("sessionKey");
            if (sessionKey == null) {
               String requestUserName = auth.getRequestUserName(req);
               if (requestUserName == null) {
                  if (this.getFilter().isLoggable(Level.FINE)) {
                     this.getFilter().fine("failed to generate new session key, missing request user name");
                  }

                  return false;
               }

               String requestSessionId = requestSession.getId();
               if (requestSessionId == null) {
                  if (this.getFilter().isLoggable(Level.FINE)) {
                     this.getFilter().fine("failed to generate new session key, missing request session id");
                  }

                  return false;
               }

               sessionKey = SessionKey.make(auth.getRequestUserName(req).getBytes(StandardCharsets.UTF_8), ByteArrayUtil.hexStringToBytes(requestSessionId));
               requestSession.setAttribute("sessionKey", sessionKey);
               resp.setHeader("RefreshDaemonSessionKey", "true");
               if (this.getFilter().isLoggable(Level.FINEST)) {
                  this.getFilter()
                     .finest(
                        "server added RefreshDaemonSessionKey header on response to request \""
                           + TextUtil.truncate(req.getRequestURI(), 25)
                           + "\", client should refresh session key"
                     );
               }
            }
         }
      }

      req.setAttribute("AuthenticationInfo", result);
      return result != null;
   }

   public Authenticator getAuthenticator() {
      return this.auth;
   }

   public void setAuthenticator(Authenticator newAuth) {
      synchronized (this.authMonitor) {
         this.auth = newAuth;
      }
   }

   public void updateAuthenticator(Logger handler, Properties props, IPlatformProvider platformProvider) {
      synchronized (this.authMonitor) {
         if (this.oldAuth != null) {
            this.oldAuth = null;
         }

         this.oldAuth = this.getAuthenticator();
         Authenticator newAuth = Authenticator.make(
            handler,
            props,
            this.oldAuth == null ? null : this.oldAuth.getAuthDomain().getExtraUsers(),
            this.oldAuth == null ? null : this.oldAuth.getAuthDomain().getExtraAdminUsers(),
            platformProvider
         );
         if (newAuth == null) {
            this.getFilter().severe("failed to create new daemon authenticator, can not update authenticator");
         } else {
            this.setAuthenticator(newAuth);
            if (NiagaraDaemon.getInstance().getStationRegistry() != null) {
               NiagaraDaemon.getInstance().getStationRegistry().resetDaemonSessions();
            }

            this.updateSessionCookieHttpOnlyConfig();
         }
      }
   }

   public void setDefaultServlet(Servlet s) {
      if (s.init(this)) {
         if (this.state != 1 || s.start()) {
            this.defServlet = s;
         }
      }
   }

   public boolean registerServlet(Servlet s) {
      if (s.getName() == null) {
         return false;
      }

      if (!s.init(this)) {
         StringBuilder buffer = new StringBuilder();
         buffer.append("servlet <").append(s.getName() != null ? s.getName() : "").append("> init failed");
         this.filter.warning(buffer.toString());
         return false;
      }

      if (this.state == 1 && !s.start()) {
         StringBuilder buffer = new StringBuilder();
         buffer.append("servlet <").append(s.getName() != null ? s.getName() : "").append("> start failed");
         this.filter.warning(buffer.toString());
         return false;
      }

      if (this.servlets == null) {
         this.servlets = new ArrayList<>();
      }

      this.servlets.add(s);
      if (this.filter.isLoggable(Level.FINE)) {
         StringBuilder buffer = new StringBuilder();
         buffer.append("servlet <").append(s.getName() != null ? s.getName() : "").append("> started");
         this.filter.fine(buffer.toString());
      }

      return true;
   }

   public void unregisterServlet(Servlet s) {
      if (s != null && this.servlets != null) {
         this.servlets.remove(s);
      }
   }

   public Servlet getServlet(String name) {
      if (name != null && this.servlets != null) {
         for (Servlet servlet : this.servlets) {
            if (servlet.getName().equalsIgnoreCase(name)) {
               return servlet;
            }
         }

         return this.defServlet;
      } else {
         return this.defServlet;
      }
   }

   public List<Servlet> getServlets() {
      return Collections.unmodifiableList(this.servlets);
   }

   private void startServlets() {
      if (this.defServlet != null && !this.defServlet.start()) {
         this.filter.severe("could not start default servlet");
         this.defServlet = null;
      }

      if (this.servlets != null) {
         ArrayList<Servlet> badServlets = new ArrayList<>();

         for (Servlet current : this.servlets) {
            if (!current.start()) {
               this.filter.severe("could not start " + current.getName() + " servlet, the servlet will be disabled");
               badServlets.add(current);
            }
         }

         this.servlets.removeAll(badServlets);
      }
   }

   private void stopServlets() {
      if (this.servlets != null) {
         this.servlets.forEach(Servlet::stop);
      }
   }

   public void updateSessionCookieHttpOnlyConfig() {
      if (this.server != null && this.server.getHandler() instanceof HandlerList) {
         HandlerList handlerList = (HandlerList)this.server.getHandler();
         Handler[] handlers = handlerList.getHandlers();
         if (handlers != null) {
            for (Handler handler : handlers) {
               if (handler instanceof SessionHandler) {
                  SessionHandler sessionHandler = (SessionHandler)handler;
                  sessionHandler.getSessionCookieConfig().setHttpOnly(!(this.getAuthenticator() instanceof ScramAuthenticator) || !DebugServlet.debugEnabled);
               }
            }
         }
      }
   }

   public void touchRequestSession(HttpServletRequest req) {
      if (this.server != null && this.sessionHandler != null && req != null) {
         HttpSession session = req.getSession(false);
         if (session != null) {
            this.sessionHandler.access(session, req.isSecure());
         }
      }
   }

   public int getHttpPort() {
      return this.httpPort;
   }

   public int getHttpsPort() {
      return this.httpsPort;
   }

   public String getHttpAddress() {
      return this.httpAddress;
   }

   public String getHttpsAddress() {
      return this.httpsAddress;
   }

   public Logger getFilter() {
      return this.filter;
   }

   public boolean isSSLEnabled() {
      return this.sslEnabled;
   }

   public boolean isSSLOnly() {
      return this.sslOnly;
   }

   public boolean isSSLMisconfigured() {
      return this.sslMisconfigured;
   }

   public ServerCertificateHealth getCertHealth() {
      return this.certHealth;
   }

   public void refreshAccessHandlerLocalAdapters() {
      if (this.inetAccessHandler != null && this.addLocalExemptions) {
         for (InterfaceNetworkSettings localAdapter : IPAddressUtil.getLocalHostInterfaces()) {
            this.inetAccessHandler.include(localAdapter.getInetAddress().getHostAddress());
         }
      }
   }

   public void stationRunning() {
      if (REJECTION_FILTER_ENABLED && this.dosFilter != null) {
         this.dosFilter.setMaxRequestsPerSec(REJECTION_FILTER_MAX_REQUESTS_PER_SEC_STATION);
         STATION_HTTPS_SUSTAINED_DOS_TIMESTAMPS.clear();
         STATION_HTTPS_NONNIAGARA_DOS_TIMESTAMPS.clear();
      }

      if (QOS_FILTER_ENABLED && this.qosFilter != null) {
         this.qosFilter.setMaxRequests(QOS_FILTER_MAX_REQUESTS_STATION);
      }
   }

   public void noStationRunning() {
      if (REJECTION_FILTER_ENABLED && this.dosFilter != null) {
         this.dosFilter.setMaxRequestsPerSec(REJECTION_FILTER_MAX_REQUESTS_PER_SEC_NO_STATION);
         STATION_HTTPS_SUSTAINED_DOS_TIMESTAMPS.clear();
         STATION_HTTPS_NONNIAGARA_DOS_TIMESTAMPS.clear();
      }

      if (QOS_FILTER_ENABLED && this.qosFilter != null) {
         this.qosFilter.setMaxRequests(QOS_FILTER_MAX_REQUESTS_NO_STATION);
      }
   }

   public void sessionAuthenticated(HttpSession session, AuthenticationInfo authenticationInfo, HttpServletRequest request) {
      if (!Boolean.parseBoolean((String)session.getAttribute("sessionAudited")) && authenticationInfo instanceof SimpleAuthenticationInfo) {
         session.setAttribute("sessionAudited", "true");
         SimpleAuthenticationInfo simpleAuthenticationInfo = (SimpleAuthenticationInfo)authenticationInfo;
         if (!simpleAuthenticationInfo.isExtra()) {
            this.filter.info("Opened: " + SecurityUtil.calculateSessionIdHash(session.getId()) + " @ " + request.getRemoteAddr());
            if (NiagaraDaemon.getInstance().getStationRegistry() != null) {
               String message = "Platform | " + request.getRemoteAddr() + " | " + request.getHeader("user-agent");
               NiagaraDaemon.getInstance().getStationRegistry().sendSecurityAuditEvent("Login", authenticationInfo.getUsername(), message);
            }
         }
      }

      session.setMaxInactiveInterval(180);
      if (REJECTION_FILTER_ENABLED && this.dosFilter != null) {
         this.dosFilter.removeFromRateTracker(session.getId());
      }
   }

   public void sessionRejected(HttpSession session, String username, HttpServletRequest request) {
      if (username != null && !"".equals(username)) {
         if (this.filter.isLoggable(Level.FINE)) {
            this.filter
               .fine("Rejected: " + (session != null ? SecurityUtil.calculateSessionIdHash(session.getId()) : "null") + " @ " + request.getRemoteAddr());
         }

         if (NiagaraDaemon.getInstance().getStationRegistry() != null) {
            String message = "Platform | " + request.getRemoteAddr() + " | " + request.getHeader("user-agent");
            NiagaraDaemon.getInstance().getStationRegistry().sendSecurityAuditEvent("Login Failure", username, message);
         }
      }
   }

   public void sessionInvalidated(HttpSession session) {
      if (this.sessionHandler != null) {
         this.sessionHandler.removeSession(session.getId(), true);
      }
   }

   public static int getJettyMaxHttpRequestHeaderSize() {
      return JETTY_MAX_HTTP_REQUEST_HEADER_SIZE;
   }

   public static int getJettyMaxHttpSessions() {
      return JETTY_MAX_HTTP_SESSIONS;
   }

   public static boolean getJettyMaxConnectionsEnabled() {
      return JETTY_MAX_CONNECTIONS_ENABLED;
   }

   public static int getJettyMaxConnections() {
      return JETTY_MAX_CONNECTIONS;
   }

   public static long getJettyMaxConnectionsIdleTimeout() {
      return JETTY_MAX_CONNECTIONS_IDLE_TIMEOUT;
   }

   public static boolean getRejectionFilterEnabled() {
      return REJECTION_FILTER_ENABLED;
   }

   public static int getRejectionFilterMaxRequestsNoStation() {
      return REJECTION_FILTER_MAX_REQUESTS_PER_SEC_NO_STATION;
   }

   public static int getRejectionFilterMaxRequestsStation() {
      return REJECTION_FILTER_MAX_REQUESTS_PER_SEC_STATION;
   }

   public static boolean getQoSFilterEnabled() {
      return QOS_FILTER_ENABLED;
   }

   public static int getQoSFilterMaxRequestsNoStation() {
      return QOS_FILTER_MAX_REQUESTS_NO_STATION;
   }

   public static int getQoSFilterMaxRequestsStation() {
      return QOS_FILTER_MAX_REQUESTS_STATION;
   }

   public static boolean getSizeLimitHandlerEnabled() {
      return SIZE_LIMIT_HANDLER_ENABLED;
   }

   public static long getSizeLimitHandlerRequestLimit() {
      return SIZE_LIMIT_HANDLER_REQUEST_LIMIT;
   }

   public static long getSizeLimitHandlerResponseLimit() {
      return SIZE_LIMIT_HANDLER_RESPONSE_LIMIT;
   }

   static {
      int temporaryProperty = AccessController.doPrivileged(() -> Integer.getInteger("niagara.daemon.webserver.acceptor.threads", -1));
      JETTY_ACCEPTOR_THREADS = Integer.max(-1, Integer.min(16, temporaryProperty));
      temporaryProperty = AccessController.doPrivileged(() -> Integer.getInteger("niagara.daemon.webserver.acceptor.threads.delta", 0));
      JETTY_ACCEPTOR_THREAD_DELTA = Integer.max(-5, Integer.min(5, temporaryProperty));
      temporaryProperty = AccessController.doPrivileged(() -> Integer.getInteger("niagara.daemon.webserver.selector.threads", -1));
      temporaryProperty = JettyThreadUtil.calcSelectorThreads(temporaryProperty, Runtime.getRuntime().availableProcessors());
      JETTY_SELECTOR_THREADS = Integer.min(8, temporaryProperty);
      temporaryProperty = AccessController.doPrivileged(() -> Integer.getInteger("niagara.daemon.webserver.min.threads", 8));
      JETTY_MINIMUM_THREADS = Integer.max(5, Integer.min(15, temporaryProperty));
      temporaryProperty = AccessController.doPrivileged(() -> Integer.getInteger("niagara.daemon.webserver.max.threads", 25));
      JETTY_MAXIMUM_THREADS = Integer.max(25, Integer.min(50, temporaryProperty));
      temporaryProperty = AccessController.doPrivileged(() -> Integer.getInteger("niagara.daemon.webserver.max.form.content.size", 0));
      JETTY_MAX_FORM_CONTENT_SIZE = Integer.max(0, Integer.min(536870912, temporaryProperty));
      temporaryProperty = AccessController.doPrivileged(() -> Integer.getInteger("niagara.daemon.webserver.max.form.keys", 0));
      JETTY_MAX_FORM_KEYS = Integer.max(0, Integer.min(1024, temporaryProperty));
      temporaryProperty = AccessController.doPrivileged(() -> Integer.getInteger("niagara.daemon.webserver.max.http.request.header.size", 2048));
      JETTY_MAX_HTTP_REQUEST_HEADER_SIZE = Integer.max(2048, Integer.min(8192, temporaryProperty));
      temporaryProperty = AccessController.doPrivileged(() -> Integer.getInteger("niagara.daemon.webserver.max.http.response.header.size", 2048));
      JETTY_MAX_HTTP_RESPONSE_HEADER_SIZE = Integer.max(2048, Integer.min(8192, temporaryProperty));
      temporaryProperty = AccessController.doPrivileged(() -> Integer.getInteger("niagara.daemon.webserver.header.cache.size", 0));
      JETTY_HEADER_CACHE_SIZE = Integer.max(0, Integer.min(4096, temporaryProperty));
      temporaryProperty = AccessController.doPrivileged(() -> Integer.getInteger("niagara.daemon.webserver.max.sessions", 32));
      JETTY_MAX_HTTP_SESSIONS = Integer.max(32, Integer.min(64, temporaryProperty));
      temporaryProperty = AccessController.doPrivileged(() -> Integer.getInteger("niagara.daemon.webserver.min.request.data.rate", 1));
      JETTY_MIN_REQUEST_DATA_RATE = Integer.max(1, Integer.min(1048576, temporaryProperty));
      temporaryProperty = AccessController.doPrivileged(() -> Integer.getInteger("niagara.daemon.webserver.max.connections", 32));
      JETTY_MAX_CONNECTIONS = Integer.max(32, Integer.min(128, temporaryProperty));
      long temporaryLongProperty = AccessController.doPrivileged(() -> Long.getLong("niagara.daemon.webserver.max.connections.idle.timeout", 500L));
      JETTY_MAX_CONNECTIONS_IDLE_TIMEOUT = Long.max(500L, Long.min(10000L, temporaryLongProperty));
      temporaryProperty = AccessController.doPrivileged(() -> Integer.getInteger("niagara.daemon.webserver.dos.max.requests.no.station", 25));
      REJECTION_FILTER_MAX_REQUESTS_PER_SEC_NO_STATION = Integer.max(25, Integer.min(200, temporaryProperty));
      temporaryProperty = AccessController.doPrivileged(() -> Integer.getInteger("niagara.daemon.webserver.dos.max.requests.station", 16));
      REJECTION_FILTER_MAX_REQUESTS_PER_SEC_STATION = Integer.max(16, Integer.min(200, temporaryProperty));
      temporaryProperty = AccessController.doPrivileged(() -> Integer.getInteger("niagara.daemon.webserver.dos.delay", -1));
      REJECTION_FILTER_DELAY_MS = Integer.max(-1, Integer.min(1000, temporaryProperty));
      temporaryLongProperty = AccessController.doPrivileged(() -> Long.getLong("niagara.daemon.webserver.dos.max.request.ms", 30000L));
      REJECTION_FILTER_MAX_REQUEST_MS = Long.max(30000L, Long.min(250000L, temporaryLongProperty));
      temporaryLongProperty = AccessController.doPrivileged(() -> Long.getLong("niagara.daemon.webserver.dos.max.request.ms.tridium.qnx", 40000L));
      REJECTION_FILTER_MAX_REQUEST_MS_TRIDIUM_QNX = Long.max(40000L, Long.min(250000L, temporaryLongProperty));
      temporaryProperty = AccessController.doPrivileged(() -> Integer.getInteger("niagara.daemon.webserver.qos.max.request.no.station", 10));
      QOS_FILTER_MAX_REQUESTS_NO_STATION = Integer.max(10, Integer.min(50, temporaryProperty));
      temporaryProperty = AccessController.doPrivileged(() -> Integer.getInteger("niagara.daemon.webserver.qos.max.request.station", 6));
      QOS_FILTER_MAX_REQUESTS_STATION = Integer.max(6, Integer.min(50, temporaryProperty));
      temporaryLongProperty = AccessController.doPrivileged(() -> Long.getLong("niagara.daemon.webserver.size.limit.request", 536870912L));
      SIZE_LIMIT_HANDLER_REQUEST_LIMIT = temporaryLongProperty == -1L ? -1L : Long.max(16384L, temporaryLongProperty);
      temporaryLongProperty = AccessController.doPrivileged(() -> Long.getLong("niagara.daemon.webserver.size.limit.response", -1L));
      SIZE_LIMIT_HANDLER_RESPONSE_LIMIT = temporaryLongProperty == -1L ? -1L : Long.max(16384L, temporaryLongProperty);
      temporaryProperty = AccessController.doPrivileged(() -> Integer.getInteger("niagara.daemon.webserver.dos.sustained.limit", 30));
      STATION_HTTPS_SUSTAINED_DOS_LIMIT = Integer.max(10, Integer.min(100, temporaryProperty));
      temporaryLongProperty = AccessController.doPrivileged(() -> Long.getLong("niagara.daemon.webserver.dos.sustained.duration.ms", 30000L));
      STATION_HTTPS_SUSTAINED_DOS_DURATION_MS = Long.max(10000L, Long.min(60000L, temporaryLongProperty));
      temporaryLongProperty = AccessController.doPrivileged(() -> Long.getLong("niagara.daemon.webserver.dos.sustained.restart.delay.ms", 30000L));
      STATION_HTTPS_SUSTAINED_DOS_RESTART_DELAY_MS = Long.max(5000L, Long.min(60000L, temporaryLongProperty));
      temporaryProperty = AccessController.doPrivileged(() -> Integer.getInteger("niagara.daemon.webserver.dos.nonniagara.limit", 8));
      STATION_HTTPS_NONNIAGARA_DOS_LIMIT = Integer.max(5, Integer.min(100, temporaryProperty));
      temporaryLongProperty = AccessController.doPrivileged(() -> Long.getLong("niagara.daemon.webserver.dos.nonniagara.duration.ms", 60000L));
      STATION_HTTPS_NONNIAGARA_DOS_DURATION_MS = Long.max(10000L, Long.min(60000L, temporaryLongProperty));
      temporaryLongProperty = AccessController.doPrivileged(() -> Long.getLong("niagara.daemon.webserver.dos.nonniagara.restart.delay.ms", 30000L));
      STATION_HTTPS_NONNIAGARA_DOS_RESTART_DELAY_MS = Long.max(5000L, Long.min(60000L, temporaryLongProperty));
   }

   private class DecayResetThread extends Thread {
      volatile long lastThrottleMs = System.currentTimeMillis();

      public DecayResetThread() {
         super("Daemon:WS-DecayResetThread");
      }

      @Override
      public void run() {
         while (this.lastThrottleMs + 30000L > System.currentTimeMillis()) {
            try {
               Thread.sleep(10000L);
            } catch (InterruptedException var2) {
            }
         }

         if (WebServer.this.filter.isLoggable(Level.FINE)) {
            WebServer.this.filter.fine("resetting the non-Niagara request delay to 250ms");
         }

         WebServer.STATION_HTTPS_NONNIAGARA_DECAY_CURRENT = 250L;
         WebServer.STATION_HTTPS_NONNIAGARA_DECAY_RESET_THREAD = null;
      }
   }

   private class GCTimerTask extends TimerTask {
      private GCTimerTask() {
      }

      @Override
      public void run() {
         if (WebServer.this.filter != null && WebServer.this.filter.isLoggable(Level.FINER)) {
            WebServer.this.filter.finer("Requesting GC for acceptor thread usage");
         }

         System.gc();
      }
   }

   private class NiagaraConnectionLimit extends ConnectionLimit {
      public NiagaraConnectionLimit(@Name("maxConnections") int maxConnections, @Name("connectors") Connector... connectors) {
         super(maxConnections, connectors);
      }

      protected void limit() {
         super.limit();
         WebServer.this.getFilter().warning("Webserver maximum connection limit " + this.getMaxConnections() + " reached, limiting new connections");
      }

      protected void unlimit() {
         super.unlimit();
         WebServer.this.getFilter().info("Webserver maximum connection limit cleared");
      }
   }

   private class NiagaraCustomConnector extends ServerConnector {
      private final Consumer<WebServer.NiagaraManagedSelector> onSelectFailConsumer = WebServer.this.new RestartSelectorTask();
      int port;
      boolean secure;

      public NiagaraCustomConnector(Server server, int acceptors, int selectors, int port, boolean secure, ConnectionFactory... factories) {
         super(server, acceptors, selectors, factories);
         this.port = port;
         this.secure = secure;
      }

      protected SelectorManager newSelectorManager(Executor executor, Scheduler scheduler, int selectors) {
         return new ServerConnectorManager(executor, scheduler, selectors) {
            protected ManagedSelector newSelector(int id) {
               return WebServer.this.new NiagaraManagedSelector(
                  this, id, NiagaraCustomConnector.this.port, NiagaraCustomConnector.this.secure, NiagaraCustomConnector.this.onSelectFailConsumer
               );
            }
         };
      }
   }

   private class NiagaraInetAccessHandler extends InetAccessHandler {
      private NiagaraInetAccessHandler() {
      }

      public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
         try {
            HttpChannel channel = baseRequest.getHttpChannel();
            if (channel != null) {
               EndPoint endp = channel.getEndPoint();
               if (endp != null) {
                  InetSocketAddress address = endp.getRemoteAddress();
                  if (address != null && !this.isAllowed(address.getAddress(), baseRequest, request)) {
                     WebServer.this.filter
                        .warning(
                           "Rejected excluded address from source '"
                              + baseRequest.getRemoteAddr()
                              + ":"
                              + baseRequest.getRemotePort()
                              + "' with request '"
                              + baseRequest.getMethod()
                              + " "
                              + baseRequest.getRequestURI()
                              + "'"
                        );
                     if (Http.isNiagaraClient(request)) {
                        response.setHeader("Server", "Protocol Schema/" + NiagaraDaemon.getInstance().daemonVersion.charAt(0));
                     }

                     Http.sendError(request, response, 403);
                     Http.flushResponseBuffer(WebServer.this.filter, request, response);
                     baseRequest.setHandled(true);
                     return;
                  }
               }
            }
         } catch (Throwable t) {
            if (WebServer.this.filter.isLoggable(Level.FINEST)) {
               WebServer.this.filter.log(Level.FINEST, "Ignored Jetty Exception in NiagaraInetAccessHandler", t);
            }
         }

         baseRequest.setHandled(false);
      }
   }

   private class NiagaraManagedSelector extends ManagedSelector {
      private final Set<EndPoint> endpoints = ConcurrentHashMap.newKeySet();
      private final Consumer<WebServer.NiagaraManagedSelector> onSelectFailConsumer;
      int port;
      boolean secure;

      public NiagaraManagedSelector(
         SelectorManager selectorManager, int id, int port, boolean secure, Consumer<WebServer.NiagaraManagedSelector> onSelectFailConsumer
      ) {
         super(selectorManager, id);
         this.onSelectFailConsumer = onSelectFailConsumer;
         this.port = port;
         this.secure = secure;
      }

      protected void endPointOpened(EndPoint endPoint) {
         super.endPointOpened(endPoint);
         this.endpoints.add(endPoint);
      }

      protected void endPointClosed(EndPoint endPoint) {
         super.endPointClosed(endPoint);
         this.endpoints.remove(endPoint);
      }

      protected void onSelectFailed(Throwable cause) {
         if (!WebServer.this.ignoreHttpsSelectFailure || !this.secure) {
            if (WebServer.this.filter.isLoggable(Level.FINE)) {
               WebServer.this.filter
                  .fine(
                     "Selector thread '"
                        + Thread.currentThread().getName()
                        + "' for connector port "
                        + this.port
                        + " failed ("
                        + cause
                        + "), restarting selector"
                  );
               if (WebServer.this.filter.isLoggable(Level.FINEST)) {
                  WebServer.this.filter.log(Level.FINE, "Stack trace: ", cause);
               }
            }

            this.endpoints.forEach(endpoint -> {
               if (endpoint.getConnection() != null) {
                  try {
                     IO.close(endpoint.getConnection());
                  } catch (Throwable var3) {
                  }
               }

               try {
                  IO.close(endpoint);
               } catch (Throwable var2) {
               }
            });
            this.endpoints.clear();
            new Thread(() -> this.onSelectFailConsumer.accept(this), "OnSelectFailedTask").start();
         }
      }
   }

   public static class NiagaraQoSFilter extends QoSFilter {
      protected int getPriority(ServletRequest request) {
         HttpServletRequest baseRequest = (HttpServletRequest)request;
         if (NiagaraDaemon.getInstance().webServer.getHttpPort() != request.getLocalPort()
            || !"127.0.0.1".equals(request.getLocalAddr()) && !"0:0:0:0:0:0:0:1".equals(request.getLocalAddr())) {
            HttpSession session = baseRequest.getSession(false);
            return session != null && !session.isNew() ? 1 : 0;
         } else {
            return 2;
         }
      }
   }

   private class NiagaraRequestHandler extends AbstractHandler {
      private NiagaraRequestHandler() {
      }

      public void handle(String pathInContext, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
         try {
            response.setStatus(200);
            if (request.getLocalPort() == WebServer.this.httpsPort) {
               baseRequest.setSecure(true);
            }

            baseRequest.setHandled(true);
            Servlet servlet = WebServer.this.getServlet(Http.getServletName(pathInContext));
            if (servlet == null) {
               Http.sendError(request, response, 404);
               Http.flushResponseBuffer(WebServer.this.filter, request, response);
               return;
            }

            response.setHeader("Server", "Protocol Schema/" + NiagaraDaemon.getInstance().daemonVersion.charAt(0));

            try {
               if (servlet.useDefaultAuthentication()) {
                  if (!WebServer.this.authenticate(request, response)) {
                     Http.sendError(request, response, 401);
                     Http.flushResponseBuffer(WebServer.this.filter, request, response);
                     return;
                  }
               } else if (servlet.requiresAuthentication() && !servlet.authenticate(request, response)) {
                  Http.sendError(request, response, 401);
                  Http.flushResponseBuffer(WebServer.this.filter, request, response);
                  return;
               }
            } catch (Throwable throwable) {
               WebServer.this.filter
                  .log(
                     Level.SEVERE,
                     "throwable encountered when authenticating "
                        + request.getMethod()
                        + " request for URI '"
                        + TextUtil.truncate(request.getRequestURI(), 30)
                        + "...', closing connection",
                     throwable
                  );
               Http.sendError(request, response, 401);
               Http.flushResponseBuffer(WebServer.this.filter, request, response);
               return;
            }

            if (servlet.requiresAuthentication()) {
               for (HttpField constantHttpHeader : WebServer.constantHttpHeaders) {
                  response.setHeader(constantHttpHeader.getName(), constantHttpHeader.getValue());
               }
            }

            String brandHeader = request.getHeader("Baja-Station-Brand");
            if (brandHeader == null) {
               brandHeader = request.getHeader("Baja-Wb-Brand");
               if (brandHeader != null && !Brand.checkWbIn(brandHeader, NullLogger.getInstance())) {
                  Http.sendError(request, response, 403);
                  Http.flushResponseBuffer(WebServer.this.filter, request, response);
                  return;
               }
            } else if (!Brand.checkStationIn(brandHeader, NullLogger.getInstance())) {
               Http.sendError(request, response, 403);
               Http.flushResponseBuffer(WebServer.this.filter, request, response);
               return;
            }

            if (WebServer.this.filter.isLoggable(Level.FINE)) {
               StringBuilder buffer = new StringBuilder();
               buffer.append("handle ").append(request.getMethod()).append(": ").append(request.getRequestURI());
               if (request.getQueryString() != null) {
                  buffer.append(" ").append(request.getQueryString());
               }

               WebServer.this.filter.fine(buffer.toString());
            }

            if (request.getHeader("Upgrade") != null) {
               baseRequest.setHandled(false);
               return;
            }

            String method = request.getMethod();
            if (method.equalsIgnoreCase("GET")) {
               response.setHeader("Pragma", "no-cache");
               response.setHeader("Cache-Control", "max-age=0, no-cache, no-store");
               servlet.doGet(request, response);
            } else if (method.equalsIgnoreCase("POST")) {
               servlet.doPost(request, response);
            } else if (method.equalsIgnoreCase("HEAD")) {
               servlet.doHead(request, response);
            } else if (method.equalsIgnoreCase("DELETE")) {
               servlet.doDelete(request, response);
            } else {
               response.setStatus(405);
               response.setHeader("Connection", "close");
               response.setHeader("Allow", "GET,POST,HEAD,DELETE");
            }
         } catch (Throwable throwable) {
            WebServer.this.filter
               .log(
                  Level.SEVERE,
                  "throwable encountered when handling "
                     + request.getMethod()
                     + " request for URI '"
                     + TextUtil.truncate(request.getRequestURI(), 30)
                     + "...', closing connection",
                  throwable
               );
            response.setStatus(400);
         }

         try {
            Http.flushResponseBuffer(WebServer.this.filter, request, response);
         } catch (Throwable throwable) {
            WebServer.this.filter
               .log(
                  Level.SEVERE,
                  "throwable encountered when writing "
                     + response.getStatus()
                     + " response to request for URI '"
                     + TextUtil.truncate(request.getRequestURI(), 30)
                     + "...', closing connection",
                  throwable
               );
         }
      }
   }

   public static class PrerequisiteServlet extends HttpServlet {
      public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
      }
   }

   private class PrerequisiteServletHandler extends ServletHandler {
      private PrerequisiteServletHandler() {
      }

      public void doHandle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) {
         if (WebServer.REJECTION_FILTER_ENABLED || WebServer.QOS_FILTER_ENABLED) {
            try {
               super.doHandle(target, baseRequest, request, response);
               if (response.getStatus() == 429) {
                  synchronized (WebServer.STATION_HTTPS_SUSTAINED_DOS_LOCK) {
                     if (WebServer.STATION_HTTPS_SUSTAINED_DOS_RESTART_TIMER == null) {
                        WebServer.this.filter
                           .warning(
                              "Rejected suspected DOS attempt from source '"
                                 + baseRequest.getRemoteAddr()
                                 + ":"
                                 + baseRequest.getRemotePort()
                                 + "' on port "
                                 + baseRequest.getLocalPort()
                                 + " with request '"
                                 + baseRequest.getMethod()
                                 + " "
                                 + baseRequest.getRequestURI()
                                 + "'"
                           );
                     }
                  }

                  if (!response.isCommitted()) {
                     response.reset();
                     if (Http.isNiagaraClient(request)) {
                        response.setHeader("Server", "Protocol Schema/" + NiagaraDaemon.getInstance().daemonVersion.charAt(0));
                     }

                     Http.sendError(request, response, 429);
                     Http.flushResponseBuffer(WebServer.this.filter, request, response);
                  }

                  if (WebServer.STATION_HTTPS_SUSTAINED_DOS_RESTART_ENABLED) {
                     AppRegistry registry = NiagaraDaemon.getInstance().getStationRegistry();
                     if (registry != null && registry.appRunning() && baseRequest.getLocalPort() == WebServer.this.httpsPort) {
                        synchronized (WebServer.STATION_HTTPS_SUSTAINED_DOS_LOCK) {
                           if (WebServer.this.httpsConnector != null
                              && !WebServer.this.httpsConnector.isStopped()
                              && WebServer.STATION_HTTPS_SUSTAINED_DOS_RESTART_TIMER == null) {
                              long last = System.currentTimeMillis();
                              WebServer.STATION_HTTPS_SUSTAINED_DOS_TIMESTAMPS.add(last);
                              if (WebServer.STATION_HTTPS_SUSTAINED_DOS_TIMESTAMPS.size() >= WebServer.STATION_HTTPS_SUSTAINED_DOS_LIMIT) {
                                 long first = WebServer.STATION_HTTPS_SUSTAINED_DOS_TIMESTAMPS.pollFirst();
                                 long delta = last - first;
                                 if (delta >= 0L && delta <= WebServer.STATION_HTTPS_SUSTAINED_DOS_DURATION_MS) {
                                    WebServer.STATION_HTTPS_SUSTAINED_DOS_TIMESTAMPS.clear();
                                    WebServer.this.filter
                                       .severe(
                                          "Sustained DOS condition triggered from source '"
                                             + baseRequest.getRemoteAddr()
                                             + ":"
                                             + baseRequest.getRemotePort()
                                             + "' on port "
                                             + baseRequest.getLocalPort()
                                             + ", temporarily disabling "
                                             + WebServer.this.httpsPort
                                             + " HTTPS connector ("
                                             + WebServer.STATION_HTTPS_NONNIAGARA_DOS_RESTART_DELAY_MS
                                             + "ms)"
                                       );

                                    try {
                                       if (WebServer.this.httpsConnector != null && !WebServer.this.httpsConnector.isStopped()) {
                                          WebServer.this.ignoreHttpsSelectFailure = true;

                                          for (EndPoint endPoint : WebServer.this.httpsConnector.getConnectedEndPoints()) {
                                             if (endPoint.getConnection() != null) {
                                                try {
                                                   IO.close(endPoint.getConnection());
                                                } catch (Throwable var45) {
                                                }
                                             }

                                             try {
                                                IO.close(endPoint);
                                             } catch (Throwable var44) {
                                             }
                                          }

                                          WebServer.this.httpsConnector.stop();
                                       }
                                    } catch (Exception exception) {
                                       WebServer.this.filter
                                          .log(Level.SEVERE, "Exception occurred stopping the HTTPS connector during sustained DOS handling", exception);
                                    }

                                    WebServer.STATION_HTTPS_SUSTAINED_DOS_RESTART_TIMER = new Timer("StationDoSConnectorRestart", true);
                                    WebServer.STATION_HTTPS_SUSTAINED_DOS_RESTART_TIMER
                                       .schedule(
                                          new TimerTask() {
                                             @Override
                                             public void run() {
                                                try {
                                                   WebServer.this.filter
                                                      .severe("Sustained DOS timeout elapsed, restarting " + WebServer.this.httpsPort + " HTTPS connector");
                                                   if (WebServer.this.httpsConnector != null && !WebServer.this.httpsConnector.isRunning()) {
                                                      WebServer.this.httpsConnector.start();
                                                   }

                                                   WebServer.this.ignoreHttpsSelectFailure = false;
                                                } catch (Exception exception) {
                                                   WebServer.this.filter
                                                      .log(
                                                         Level.SEVERE,
                                                         "Exception occurred restarting the HTTPS connector during sustained DOS handling, restarting webserver",
                                                         exception
                                                      );
                                                   WebServer.this.restart(WebServer.this.getAuthenticator(), NiagaraDaemon.props);
                                                } finally {
                                                   if (WebServer.STATION_HTTPS_SUSTAINED_DOS_RESTART_TIMER != null) {
                                                      WebServer.STATION_HTTPS_SUSTAINED_DOS_RESTART_TIMER.cancel();
                                                      WebServer.STATION_HTTPS_SUSTAINED_DOS_RESTART_TIMER = null;
                                                   }
                                                }
                                             }
                                          },
                                          WebServer.STATION_HTTPS_SUSTAINED_DOS_RESTART_DELAY_MS
                                       );
                                 }
                              }
                           }
                        }
                     }
                  }

                  return;
               }
            } catch (Throwable t) {
               if (WebServer.this.filter.isLoggable(Level.FINEST)) {
                  WebServer.this.filter.log(Level.FINEST, "Ignored Jetty Exception in PrerequisiteHandler", t);
               }
            }
         }

         try {
            response.setHeader("x-frame-options", "deny");
            boolean forbidden = true;
            MessageBundle forbiddenMessage = null;
            if (!request.getProtocol().equalsIgnoreCase(HttpVersion.HTTP_1_0.asString())) {
               if (Http.isNiagaraClient(request)) {
                  Version clientVersion = Http.getNiagaraClientVersion(request);
                  if (clientVersion != null && clientVersion.compareTo(WebServer.minimumRequiredClientVersion) >= 0) {
                     forbidden = false;
                  } else {
                     forbiddenMessage = new MessageBundle(
                        "Niagara daemon can only communicate with Workbench " + WebServer.minimumRequiredClientVersion + " or higher"
                     );
                  }
               } else if (request.getHeader("Upgrade") == null && NiagaraDaemon.getDebugSupported() && DebugServlet.debugEnabled) {
                  forbidden = false;
               }
            }

            if (forbidden) {
               WebServer.this.getAuthenticator().invalidateSession(request);
               AppRegistry registry = NiagaraDaemon.getInstance().getStationRegistry();
               if (registry != null && registry.appRunning() && baseRequest.getLocalPort() == WebServer.this.httpsPort) {
                  if (WebServer.STATION_HTTPS_NONNIAGARA_DOS_RESTART_ENABLED) {
                     synchronized (WebServer.STATION_HTTPS_NONNIAGARA_DOS_LOCK) {
                        if (WebServer.this.httpsConnector != null
                           && !WebServer.this.httpsConnector.isStopped()
                           && WebServer.STATION_HTTPS_NONNIAGARA_DOS_RESTART_TIMER == null) {
                           if (WebServer.this.filter.isLoggable(Level.FINE)) {
                              WebServer.this.filter
                                 .fine(
                                    "Non-Niagara HTTPS traffic while Niagara Station running from source '"
                                       + baseRequest.getRemoteAddr()
                                       + ":"
                                       + baseRequest.getRemotePort()
                                       + "' on port "
                                       + baseRequest.getLocalPort()
                                       + " with request '"
                                       + baseRequest.getMethod()
                                       + " "
                                       + baseRequest.getRequestURI()
                                       + "'"
                                 );
                           }

                           long last = System.currentTimeMillis();
                           WebServer.STATION_HTTPS_NONNIAGARA_DOS_TIMESTAMPS.add(last);
                           if (WebServer.STATION_HTTPS_NONNIAGARA_DOS_TIMESTAMPS.size() >= WebServer.STATION_HTTPS_NONNIAGARA_DOS_LIMIT) {
                              long first = WebServer.STATION_HTTPS_NONNIAGARA_DOS_TIMESTAMPS.pollFirst();
                              long delta = last - first;
                              if (delta >= 0L && delta <= WebServer.STATION_HTTPS_NONNIAGARA_DOS_DURATION_MS) {
                                 WebServer.STATION_HTTPS_NONNIAGARA_DOS_TIMESTAMPS.clear();
                                 WebServer.this.filter
                                    .severe(
                                       "Non-Niagara DOS condition triggered from source '"
                                          + baseRequest.getRemoteAddr()
                                          + ":"
                                          + baseRequest.getRemotePort()
                                          + "' on port "
                                          + baseRequest.getLocalPort()
                                          + ", temporarily disabling "
                                          + WebServer.this.httpsPort
                                          + " HTTPS connector ("
                                          + WebServer.STATION_HTTPS_NONNIAGARA_DOS_RESTART_DELAY_MS
                                          + "ms)"
                                    );

                                 try {
                                    if (WebServer.this.httpsConnector != null && !WebServer.this.httpsConnector.isStopped()) {
                                       WebServer.this.ignoreHttpsSelectFailure = true;

                                       for (EndPoint endPoint : WebServer.this.httpsConnector.getConnectedEndPoints()) {
                                          if (endPoint.getConnection() != null) {
                                             try {
                                                IO.close(endPoint.getConnection());
                                             } catch (Throwable var43) {
                                             }
                                          }

                                          try {
                                             IO.close(endPoint);
                                          } catch (Throwable var42) {
                                          }
                                       }

                                       WebServer.this.httpsConnector.stop();
                                    }
                                 } catch (Exception exception) {
                                    WebServer.this.filter
                                       .log(Level.SEVERE, "Exception occurred stopping the HTTPS connector during non-Niagara DOS handling", exception);
                                 }

                                 WebServer.STATION_HTTPS_NONNIAGARA_DOS_RESTART_TIMER = new Timer("StationDoSConnectorRestart", true);
                                 WebServer.STATION_HTTPS_NONNIAGARA_DOS_RESTART_TIMER
                                    .schedule(
                                       new TimerTask() {
                                          @Override
                                          public void run() {
                                             try {
                                                WebServer.this.filter
                                                   .severe("Non-Niagara DOS timeout elapsed, restarting " + WebServer.this.httpsPort + " HTTPS connector");
                                                if (WebServer.this.httpsConnector != null && !WebServer.this.httpsConnector.isRunning()) {
                                                   WebServer.this.httpsConnector.start();
                                                }

                                                WebServer.this.ignoreHttpsSelectFailure = false;
                                             } catch (Exception exception) {
                                                WebServer.this.filter
                                                   .log(
                                                      Level.SEVERE,
                                                      "Exception occurred restarting the HTTPS connector during non-Niagara DOS handling, restarting webserver",
                                                      exception
                                                   );
                                                WebServer.this.restart(WebServer.this.getAuthenticator(), NiagaraDaemon.props);
                                             } finally {
                                                if (WebServer.STATION_HTTPS_NONNIAGARA_DOS_RESTART_TIMER != null) {
                                                   WebServer.STATION_HTTPS_NONNIAGARA_DOS_RESTART_TIMER.cancel();
                                                   WebServer.STATION_HTTPS_NONNIAGARA_DOS_RESTART_TIMER = null;
                                                }
                                             }
                                          }
                                       },
                                       WebServer.STATION_HTTPS_NONNIAGARA_DOS_RESTART_DELAY_MS
                                    );
                              }
                           }
                        }
                     }
                  } else if (WebServer.STATION_HTTPS_NONNIAGARA_DOS_THROTTLE_ENABLED) {
                     try {
                        synchronized (WebServer.STATION_HTTPS_NONNIAGARA_DECAY_LOCK) {
                           WebServer.this.httpsConnector.setAccepting(false);

                           try {
                              Thread.sleep(WebServer.STATION_HTTPS_NONNIAGARA_DECAY_CURRENT);
                              WebServer.STATION_HTTPS_NONNIAGARA_DECAY_CURRENT = (long)(WebServer.STATION_HTTPS_NONNIAGARA_DECAY_CURRENT * 1.25);
                              WebServer.STATION_HTTPS_NONNIAGARA_DECAY_CURRENT = Long.min(WebServer.STATION_HTTPS_NONNIAGARA_DECAY_CURRENT, 10000L);
                              if (WebServer.this.filter.isLoggable(Level.FINE)) {
                                 WebServer.this.filter
                                    .fine("non-Niagara request delay incremented to " + WebServer.STATION_HTTPS_NONNIAGARA_DECAY_CURRENT + "ms");
                              }
                           } catch (Exception var38) {
                           } finally {
                              WebServer.this.httpsConnector.setAccepting(true);
                           }

                           if (WebServer.STATION_HTTPS_NONNIAGARA_DECAY_RESET_THREAD == null) {
                              WebServer.STATION_HTTPS_NONNIAGARA_DECAY_RESET_THREAD = WebServer.this.new DecayResetThread();
                              WebServer.STATION_HTTPS_NONNIAGARA_DECAY_RESET_THREAD.start();
                           } else {
                              WebServer.STATION_HTTPS_NONNIAGARA_DECAY_RESET_THREAD.lastThrottleMs = System.currentTimeMillis();
                           }
                        }
                     } catch (Exception exception) {
                        WebServer.this.filter.log(Level.SEVERE, "Exception occurred while limiting non-Niagara request, restarting webserver", exception);
                        WebServer.this.restart(WebServer.this.getAuthenticator(), NiagaraDaemon.props);
                        return;
                     }
                  }
               }

               if (forbiddenMessage != null) {
                  Http.sendErrorXML(request, response, 403, forbiddenMessage);
               } else {
                  Http.sendError(request, response, 403);
               }

               Http.flushResponseBuffer(WebServer.this.filter, request, response);
               return;
            }
         } catch (Throwable throwable) {
            WebServer.this.filter
               .log(
                  Level.SEVERE,
                  "throwable encountered when handling "
                     + request.getMethod()
                     + " request for URI '"
                     + TextUtil.truncate(request.getRequestURI(), 30)
                     + "...', closing connection",
                  throwable
               );
            response.setStatus(400);

            try {
               Http.flushResponseBuffer(WebServer.this.filter, request, response);
            } catch (Throwable flushThrowable) {
               WebServer.this.filter
                  .log(
                     Level.SEVERE,
                     "throwable encountered when writing "
                        + response.getStatus()
                        + " response to request for URI '"
                        + TextUtil.truncate(request.getRequestURI(), 30)
                        + "...', closing connection",
                     throwable
                  );
            }

            return;
         }

         baseRequest.setHandled(false);
      }
   }

   private class RestartSelectorTask implements Consumer<WebServer.NiagaraManagedSelector> {
      private RestartSelectorTask() {
      }

      public void accept(WebServer.NiagaraManagedSelector customManagedSelector) {
         try {
            customManagedSelector.stop();
            if (WebServer.this.filter.isLoggable(Level.FINE)) {
               WebServer.this.filter.fine("RestartSelectorTask stopped selector for connector port " + customManagedSelector.port);
            }

            customManagedSelector.start();
            if (WebServer.this.filter.isLoggable(Level.FINE)) {
               WebServer.this.filter.fine("RestartSelectorTask started selector for connector port " + customManagedSelector.port);
            }
         } catch (Exception exception) {
            WebServer.this.filter
               .log(
                  Level.SEVERE,
                  "RestartSelectorTask failed to restart the webserver selector on port " + customManagedSelector.port + ", restarting webserver",
                  exception
               );
            WebServer.this.restart(WebServer.this.getAuthenticator(), NiagaraDaemon.props);
         }
      }
   }
}
