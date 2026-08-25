package com.tridium.nre.syslog;

import com.tridium.nre.platform.IPlatformProvider;
import com.tridium.nre.platform.PlatformUtil;
import com.tridium.nre.subscription.SubscriptionLicenseUtil;
import com.tridium.nre.util.IPAddressUtil;
import com.tridium.nre.util.LicenseMode;
import com.tridium.nre.util.NamedThreadFactory;
import com.tridium.nre.util.NiagaraFiles;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.AccessController;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SyslogManager {
   public static final String STATION_TAG = "station";
   public static final String NRE_TAG = "niagara";
   public static final String WORKBENCH_TAG = "workbench";
   public static final String PLATFORM_TAG = "platform";
   public static final String LOG_NAME = "syslog";
   public static final Logger LOG = Logger.getLogger("syslog");
   private static final String SYSLOG_ENABLED_PROPERTY = "syslog.enabled";
   private static final String SYSLOG_SERVER_HOST_PROPERTY = "syslog.server.host";
   private static final String SYSLOG_SERVER_PORT_PROPERTY = "syslog.server.port";
   private static final String SYSLOG_SERVER_MESSAGE_TYPE_PROPERTY = "syslog.server.messageType";
   private static final String SYSLOG_SERVER_TRANSPORT_PROTOCOL_PROPERTY = "syslog.server.transportProtocol";
   private static final String SYSLOG_MANAGER_CLIENT_ALIAS_PROPERTY = "syslog.manager.clientAlias";
   private static final String SYSLOG_MANAGER_CLIENT_PASSWORD_PROPERTY = "syslog.manager.clientPassword";
   private static final String SYSLOG_NRE_LOG_ENABLED_PROPERTY = "syslog.nre.log.enabled";
   private static final String SYSLOG_PLATFORM_LOG_ENABLED_PROPERTY = "syslog.platform.log.enabled";
   private static final String SYSLOG_STATION_LOG_ENABLED_PROPERTY = "syslog.station.log.enabled";
   private static final String SYSLOG_STATION_AUDIT_ENABLED_PROPERTY = "syslog.station.audit.enabled";
   private static final String SYSLOG_SECURITY_AUDIT_ENABLED_PROPERTY = "syslog.security.audit.enabled";
   private static final String SYSLOG_MANAGER_FACILITY_PROPERTY = "syslog.manager.facility";
   private static final String SYSLOG_MANAGER_QUEUE_SIZE_PROPERTY = "syslog.manager.queueSize";
   private static final String SYSLOG_MANAGER_FORMAT_PROPERTY = "syslog.manager.format";
   private static final String SYSLOG_LOG_LEVEL_FILTER_PROPERTY = "syslog.logLevelFilter.level";
   private static final boolean DEFAULT_ENABLED = false;
   private static final String DEFAULT_SERVER_HOST = "";
   private static final int DEFAULT_SERVER_PORT = 1514;
   private static final String DEFAULT_SERVER_MESSAGE_TYPE = "bsd";
   private static final String DEFAULT_SERVER_TRANSPORT_PROTOCOL = "tcp";
   private static final String DEFAULT_CLIENT_ALIAS = "default";
   private static final boolean DEFAULT_NRE_LOG_ENABLED = true;
   private static final boolean DEFAULT_PLATFORM_LOG_ENABLED = true;
   private static final boolean DEFAULT_STATION_LOG_ENABLED = true;
   private static final boolean DEFAULT_STATION_AUDIT_ENABLED = true;
   private static final boolean DEFAULT_SECURITY_AUDIT_ENABLED = true;
   private static final String DEFAULT_FACILITY = "local0";
   private static final int DEFAULT_QUEUE_SIZE = 1000;
   private static final String DEFAULT_LOG_LEVEL_FILTER = "INFO";
   private static final int MAX_TAG_LENGTH = 32;
   private static final File LOGGING_DIRECTORY = new File(
      AccessController.doPrivileged(() -> System.getProperty("niagara.user.home")) + File.separator + "logging"
   );
   private static final File SYSLOG_PROPERTIES_FILE = new File(LOGGING_DIRECTORY, "syslog.properties");
   private static final String SYSLOG_FEATURE = "syslog";
   private static final String TRIDIUM_VENDOR = "vendor=\"Tridium\"";
   private static final Object PAUSE_STATE_MONITOR = new Object();
   private static final Pattern ENCODED_PASSWORD_PATTERN = Pattern.compile("\\[.+\\]\\=\\w+\\:\\w+");
   private static final long FILE_MONITOR_INTERVAL_MS = 10000L;
   private static final ThreadFactory FILE_MONITOR_THREAD_FACTORY = new NamedThreadFactory("SyslogManager.fileMonitor", true);
   private static boolean syslogFeatureFound;
   private static boolean licenseFileChecked;
   private final Properties syslogProperties = new Properties();
   private final List<ISyslogStatusListener> syslogStatusListenerList = new CopyOnWriteArrayList<>();
   private final AtomicReference<Optional<ScheduledExecutorService>> fileMonitor = new AtomicReference<>(Optional.empty());
   private final AtomicInteger overflowCount = new AtomicInteger(0);
   private volatile SyslogSender senderThread;
   private boolean enabled;
   private boolean isReadonly;
   private String serverHost;
   private int serverPort;
   private MessageType messageType;
   private Transport transportProtocol;
   private String clientAlias;
   private String encodedClientPassword;
   private boolean nreLogEnabled;
   private boolean platformLogEnabled;
   private boolean stationLogEnabled;
   private boolean stationAuditEnabled;
   private boolean securityAuditEnabled;
   private Facility facility;
   private int queueSize;
   private Level logLevelFilter = Level.parse("INFO");
   private boolean useTLS;
   private boolean checkPaused;
   private boolean isServerConnected;
   private boolean running;
   private long lastModified;
   private String environmentTag = "niagara";
   private String format = "%4$s [%1$tH:%1$tM:%1$tS %1$td-%1$tb-%1$ty %1$tZ][%3$s] %5$s%6$s";
   private BlockingQueue<Message> blockingQueue;
   static final List<String> VALID_ENVIRONMENT_TAGS = new ArrayList<>(Arrays.asList("platform", "station"));

   public static SyslogManager getInstance() {
      return SyslogManager.SyslogManagerInstance.INSTANCE;
   }

   private SyslogManager() {
      this.initialize();
   }

   private void initialize() {
      AccessController.doPrivileged(() -> {
         if (!LOGGING_DIRECTORY.exists() && !LOGGING_DIRECTORY.mkdirs()) {
            LOG.severe("Failed to create logging directory.");
            return null;
         }

         try {
            if (!this.isSyslogLicensed() || !SYSLOG_PROPERTIES_FILE.exists()) {
               try (FileOutputStream out = new FileOutputStream(SYSLOG_PROPERTIES_FILE)) {
                  writeDefaultSyslogConfig(out);
               }
            }

            this.loadProperties();
         } catch (Exception e) {
            LOG.log(Level.SEVERE, "Failed to configure syslog logging.", e);
         }

         return null;
      });
   }

   private static synchronized void writeDefaultSyslogConfig(OutputStream o) {
      PrintWriter out = new PrintWriter(new OutputStreamWriter(o, StandardCharsets.UTF_8));
      out.println("#Auto-generated file, do not modify.");
      out.println("syslog.enabled=false");
      out.println("syslog.server.host=");
      out.println("syslog.server.port=1514");
      out.println("syslog.server.messageType=bsd");
      out.println("syslog.server.transportProtocol=tcp");
      out.println("syslog.manager.clientAlias=default");
      out.println("syslog.nre.log.enabled=true");
      out.println("syslog.platform.log.enabled=true");
      out.println("syslog.station.log.enabled=true");
      out.println("syslog.logLevelFilter.level=INFO");
      out.println("syslog.station.audit.enabled=true");
      out.println("syslog.security.audit.enabled=true");
      out.println("syslog.manager.facility=local0");
      out.println("syslog.manager.queueSize=1000");
      out.flush();
   }

   public synchronized void writeSyslogConfig(
      String enabled,
      String serverHost,
      String serverPort,
      String messageType,
      String transportProtocol,
      String clientAlias,
      String encodedClientPassword,
      String platformLogEnabled,
      String stationLogEnabled,
      String logLevelFilter,
      String stationAuditEnabled,
      String securityAuditEnabled,
      String facility,
      String queueSize
   ) throws Exception {
      if (this.getIsReadonly()) {
         throw new Exception("Unsupported action: Cannot update readonly syslog configuration.");
      }

      getInstance().pauseFileMonitor();
      if (enabled != null) {
         if (!Boolean.parseBoolean(enabled) && !enabled.equals(Boolean.FALSE.toString())) {
            enabled = Boolean.FALSE.toString();
         }
      } else {
         enabled = Boolean.toString(this.getEnabled());
      }

      this.syslogProperties.setProperty("syslog.enabled", enabled);
      if (serverHost != null) {
         if (!IPAddressUtil.isHostname(serverHost) && !IPAddressUtil.isNumericAddr(serverHost)) {
            throw new IllegalArgumentException("invalid serverHost provided");
         }
      } else {
         serverHost = this.getServerHost();
      }

      this.syslogProperties.setProperty("syslog.server.host", serverHost);
      if (serverPort != null) {
         boolean validInteger = false;

         try {
            if (Integer.parseInt(serverPort) > 0) {
               validInteger = true;
            }
         } catch (Exception var40) {
         }

         if (!validInteger) {
            throw new IllegalArgumentException("invalid serverPort provided");
         }
      } else {
         serverPort = Integer.toString(this.getServerPort());
      }

      this.syslogProperties.setProperty("syslog.server.port", serverPort);
      if (messageType != null) {
         try {
            MessageType.valueOf(messageType);
         } catch (Exception e) {
            throw new IllegalArgumentException("invalid messageType provided");
         }
      } else {
         messageType = this.getMessageType().name();
      }

      this.syslogProperties.setProperty("syslog.server.messageType", messageType);
      if (transportProtocol != null) {
         try {
            Transport.valueOf(transportProtocol);
         } catch (Exception e) {
            throw new IllegalArgumentException("invalid transportProtocol provided");
         }
      } else {
         transportProtocol = this.getTransportProtocol().name();
      }

      this.syslogProperties.setProperty("syslog.server.transportProtocol", transportProtocol);
      if (clientAlias == null) {
         clientAlias = this.getClientAlias();
      }

      this.syslogProperties.setProperty("syslog.manager.clientAlias", clientAlias);
      if (encodedClientPassword != null) {
         Matcher matcher = ENCODED_PASSWORD_PATTERN.matcher(encodedClientPassword);
         if (!matcher.find()) {
            throw new IllegalArgumentException("invalid encodedClientPassword provided");
         }

         this.syslogProperties.setProperty("syslog.manager.clientPassword", encodedClientPassword);
      } else {
         this.syslogProperties.remove("syslog.manager.clientPassword");
      }

      if (platformLogEnabled != null) {
         if (!Boolean.parseBoolean(platformLogEnabled) && !platformLogEnabled.equals(Boolean.FALSE.toString())) {
            platformLogEnabled = Boolean.FALSE.toString();
         }
      } else {
         platformLogEnabled = Boolean.toString(this.getPlatformLogEnabled());
      }

      this.syslogProperties.setProperty("syslog.platform.log.enabled", platformLogEnabled);
      if (stationLogEnabled != null) {
         if (!Boolean.parseBoolean(stationLogEnabled) && !stationLogEnabled.equals(Boolean.FALSE.toString())) {
            stationLogEnabled = Boolean.FALSE.toString();
         }
      } else {
         stationLogEnabled = Boolean.toString(this.getStationLogEnabled());
      }

      this.syslogProperties.setProperty("syslog.station.log.enabled", stationLogEnabled);
      if (stationAuditEnabled != null) {
         if (!Boolean.parseBoolean(stationAuditEnabled) && !stationAuditEnabled.equals(Boolean.FALSE.toString())) {
            stationAuditEnabled = Boolean.FALSE.toString();
         }
      } else {
         stationAuditEnabled = Boolean.toString(this.getStationAuditEnabled());
      }

      this.syslogProperties.setProperty("syslog.station.audit.enabled", stationAuditEnabled);
      if (securityAuditEnabled != null) {
         if (!Boolean.parseBoolean(securityAuditEnabled) && !securityAuditEnabled.equals(Boolean.FALSE.toString())) {
            securityAuditEnabled = Boolean.FALSE.toString();
         }
      } else {
         securityAuditEnabled = Boolean.toString(this.getSecurityAuditEnabled());
      }

      this.syslogProperties.setProperty("syslog.security.audit.enabled", securityAuditEnabled);
      if (facility != null) {
         try {
            Facility.valueOf(facility);
         } catch (Exception e) {
            throw new IllegalArgumentException("invalid facility provided");
         }
      } else {
         facility = this.getFacility().name();
      }

      this.syslogProperties.setProperty("syslog.manager.facility", facility);
      if (queueSize != null) {
         boolean validInteger = false;

         try {
            if (Integer.parseInt(queueSize) > 0) {
               validInteger = true;
            }
         } catch (Exception var36) {
         }

         if (!validInteger) {
            throw new IllegalArgumentException("invalid queueSize provided");
         }
      } else {
         queueSize = Integer.toString(this.getQueueSize());
      }

      this.syslogProperties.setProperty("syslog.manager.queueSize", queueSize);
      if (logLevelFilter != null) {
         try {
            Level.parse(logLevelFilter.toUpperCase(Locale.ENGLISH));
         } catch (Exception e) {
            throw new IllegalArgumentException("invalid logLevelFilter provided");
         }
      } else {
         logLevelFilter = this.getLogLevelFilter().getName();
      }

      this.syslogProperties.setProperty("syslog.logLevelFilter.level", logLevelFilter);
      File dataFile = getSyslogPropertiesFile();

      try (OutputStreamWriter writer = new OutputStreamWriter(Files.newOutputStream(dataFile.toPath()), StandardCharsets.UTF_8)) {
         DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
         LocalDateTime now = LocalDateTime.now();
         this.syslogProperties.store(writer, "Auto-generated file on " + dtf.format(now) + ", do not modify.");
      } catch (Exception e) {
         if (LOG.isLoggable(Level.FINE)) {
            LOG.log(Level.SEVERE, "Failed to write properties to syslog.properties file.", e);
         } else {
            LOG.log(Level.SEVERE, "Failed to write properties to syslog.properties file: " + e.getMessage());
         }

         throw e;
      }

      this.checkLastModified();
      getInstance().resumeFileMonitor();
   }

   private static File getSyslogPropertiesFile() {
      return SYSLOG_PROPERTIES_FILE;
   }

   public boolean compareSenderThreadParameters(
      String previousHostname, int previousPort, String previousClientAlias, String previousClientPassword, Transport previousTransport
   ) {
      if (previousHostname != null && previousPort >= 1 && previousPort <= 65535 && previousClientAlias != null && this.transportProtocol != null) {
         if (previousClientPassword == null) {
            if (this.encodedClientPassword != null) {
               return true;
            }
         } else if (!previousClientPassword.equals(this.encodedClientPassword)) {
            return true;
         }

         return !previousHostname.equals(this.serverHost)
            || previousPort != this.serverPort
            || !previousClientAlias.equals(this.clientAlias)
            || previousTransport.compareTo(this.transportProtocol) != 0;
      } else {
         LOG.warning(
            "Invalid sender thread values. Previous Hostname: "
               + previousHostname
               + ", Port: "
               + previousPort
               + ", Client Alias: "
               + previousClientAlias
               + ", Transport: "
               + previousTransport
         );
         return true;
      }
   }

   public synchronized void reloadProperties() {
      boolean previousEnabled = this.enabled;
      String previousHostname = this.serverHost;
      int previousPort = this.serverPort;
      String previousClientAlias = this.clientAlias;
      String previousEncodedClientPassword = this.encodedClientPassword;
      Transport previousTransport = this.transportProtocol;
      this.loadProperties();
      if (!previousEnabled && this.enabled) {
         LOG.fine("Starting Syslog Manager");
         this.start();
      } else if (previousEnabled && !this.enabled) {
         LOG.fine("Stopping Syslog Manager");
         this.stop();
      } else if (previousEnabled
         && this.compareSenderThreadParameters(previousHostname, previousPort, previousClientAlias, previousEncodedClientPassword, previousTransport)) {
         LOG.fine("Re-initializing sender thread");
         this.reloadSenderThread();
      }
   }

   private void reloadSenderThread() {
      if (this.isValidEnvironment(this.getEnvironmentTag())) {
         if (this.senderThread != null) {
            this.senderThread.shutdown();
            this.senderThread = null;
         }

         try {
            Thread.sleep(100L);
         } catch (InterruptedException e) {
            LOG.log(Level.WARNING, "Shutdown process interrupted.", e);
         }

         this.running = false;
         if (this.blockingQueue == null) {
            this.blockingQueue = new ArrayBlockingQueue<>(this.getQueueSize());
         }

         this.initializeSenderThread();
      }
   }

   private void initializeSenderThread() {
      if (Transport.tcp != this.getTransportProtocol() && Transport.tls != this.getTransportProtocol()) {
         LOG.fine("Creating UDP sender");
         this.senderThread = new UdpSender(this, this.blockingQueue);
      } else {
         LOG.fine("Creating TCP sender");
         this.senderThread = new TcpSender(this, this.blockingQueue);
      }

      this.senderThread.setDaemon(true);
      this.senderThread.start();
      this.running = true;
   }

   boolean isSenderThread(long threadId) {
      return this.senderThread != null ? this.senderThread.getId() == threadId : false;
   }

   private void loadProperties() {
      LOG.fine("Reading syslog.properties");
      if (SYSLOG_PROPERTIES_FILE.exists()) {
         try (InputStreamReader in = new InputStreamReader(Files.newInputStream(SYSLOG_PROPERTIES_FILE.toPath()), StandardCharsets.UTF_8)) {
            this.syslogProperties.load(in);
         } catch (Exception e) {
            if (LOG.isLoggable(Level.FINE)) {
               LOG.log(Level.WARNING, "Unable to load syslog.properties.", e);
            } else {
               LOG.log(Level.WARNING, "Unable to load syslog.properties: " + e);
            }
         }
      }

      this.enabled = Boolean.parseBoolean(this.syslogProperties.getProperty("syslog.enabled", String.valueOf(false)));
      this.isReadonly = false;
      this.logLevelFilter = Level.parse(this.syslogProperties.getProperty("syslog.logLevelFilter.level", "INFO").toUpperCase(Locale.ENGLISH));
      setSyslogLogHandlerFilterLevel(this.logLevelFilter);
      if (this.enabled) {
         LOG.fine("Syslog integration is enabled");
      } else {
         LOG.fine("Syslog integration is disabled");
      }

      this.serverHost = this.syslogProperties.getProperty("syslog.server.host", "");
      if ((this.serverHost == null || "".equals(this.serverHost)) && this.enabled) {
         LOG.warning("Hostname not configured");
      }

      this.serverPort = Integer.parseInt(this.syslogProperties.getProperty("syslog.server.port", String.valueOf(1514)));

      try {
         this.transportProtocol = Transport.valueOf(this.syslogProperties.getProperty("syslog.server.transportProtocol", "tcp"));
      } catch (Exception e) {
         this.transportProtocol = Transport.valueOf("tcp");
         if (LOG.isLoggable(Level.FINE)) {
            LOG.log(Level.WARNING, "Failed to load Transport Protocol from syslog.properties. Switching to default: tcp", e);
         } else {
            LOG.log(Level.WARNING, "Failed to load Transport Protocol from syslog.properties. Switching to default: tcp");
         }
      }

      this.useTLS = this.transportProtocol.compareTo(Transport.tls) == 0;

      try {
         this.messageType = MessageType.valueOf(this.syslogProperties.getProperty("syslog.server.messageType", "bsd"));
      } catch (Exception e) {
         this.messageType = MessageType.valueOf("bsd");
         if (LOG.isLoggable(Level.FINE)) {
            LOG.log(Level.WARNING, "Failed to load Message Type from syslog.properties. Switching to default: bsd", e);
         } else {
            LOG.log(Level.WARNING, "Failed to load Message Type from syslog.properties. Switching to default: bsd");
         }
      }

      this.queueSize = Integer.parseInt(this.syslogProperties.getProperty("syslog.manager.queueSize", String.valueOf(1000)));

      try {
         this.facility = Facility.valueOf(this.syslogProperties.getProperty("syslog.manager.facility", "local0"));
      } catch (Exception e) {
         this.facility = Facility.valueOf("local0");
         if (LOG.isLoggable(Level.FINE)) {
            LOG.log(Level.WARNING, "Failed to load Facility from syslog.properties. Switching to default: local0", e);
         } else {
            LOG.log(Level.WARNING, "Failed to load Facility from syslog.properties. Switching to default: local0");
         }
      }

      this.clientAlias = this.syslogProperties.getProperty("syslog.manager.clientAlias", "default");
      this.encodedClientPassword = this.syslogProperties.getProperty("syslog.manager.clientPassword", null);
      this.nreLogEnabled = Boolean.parseBoolean(this.syslogProperties.getProperty("syslog.nre.log.enabled", String.valueOf(true)));
      this.platformLogEnabled = Boolean.parseBoolean(this.syslogProperties.getProperty("syslog.platform.log.enabled", String.valueOf(true)));
      this.stationLogEnabled = Boolean.parseBoolean(this.syslogProperties.getProperty("syslog.station.log.enabled", String.valueOf(true)));
      this.stationAuditEnabled = Boolean.parseBoolean(this.syslogProperties.getProperty("syslog.station.audit.enabled", String.valueOf(true)));
      this.securityAuditEnabled = Boolean.parseBoolean(this.syslogProperties.getProperty("syslog.security.audit.enabled", String.valueOf(true)));
      this.format = this.syslogProperties.getProperty("syslog.manager.format", "%4$s [%1$tH:%1$tM:%1$tS %1$td-%1$tb-%1$ty %1$tZ][%3$s] %5$s%6$s");
   }

   public void setEnvironmentTag(String tag) {
      if (tag.length() > 32) {
         this.environmentTag = tag.substring(0, 32);
      } else {
         this.environmentTag = tag;
      }
   }

   public void addSyslogStatusListener(ISyslogStatusListener syslogStatusListener) {
      if (syslogStatusListener != null) {
         this.syslogStatusListenerList.add(syslogStatusListener);
      }
   }

   public void removeSyslogStatusListener(ISyslogStatusListener syslogStatusListener) {
      if (syslogStatusListener != null) {
         this.syslogStatusListenerList.remove(syslogStatusListener);
      }
   }

   public String getEnvironmentTag() {
      return this.environmentTag;
   }

   public boolean getEnabled() {
      return this.enabled;
   }

   public boolean getIsReadonly() {
      return this.isReadonly;
   }

   public String getServerHost() {
      return this.serverHost;
   }

   public int getServerPort() {
      return this.serverPort;
   }

   public MessageType getMessageType() {
      return this.messageType;
   }

   public Transport getTransportProtocol() {
      return this.transportProtocol;
   }

   public String getClientAlias() {
      return this.clientAlias;
   }

   public String getClientPassword() {
      return this.encodedClientPassword;
   }

   public boolean getNreLogEnabled() {
      return this.nreLogEnabled;
   }

   public boolean getPlatformLogEnabled() {
      return this.platformLogEnabled;
   }

   public boolean getStationLogEnabled() {
      return this.stationLogEnabled;
   }

   public boolean getStationAuditEnabled() {
      return this.stationAuditEnabled;
   }

   public boolean getSecurityAuditEnabled() {
      return this.securityAuditEnabled;
   }

   public Facility getFacility() {
      return this.facility;
   }

   public int getQueueSize() {
      return this.queueSize;
   }

   public Level getLogLevelFilter() {
      return this.logLevelFilter;
   }

   public boolean getUseTLS() {
      return this.useTLS;
   }

   public String getFormat() {
      return this.format;
   }

   public synchronized void start() {
      if (this.isValidEnvironment(this.getEnvironmentTag())) {
         if (!this.isSyslogLicensed()) {
            this.enabled = false;
            LOG.fine("Syslog feature is not licensed");
         } else if (this.enabled) {
            if (this.blockingQueue == null) {
               this.blockingQueue = new ArrayBlockingQueue<>(this.getQueueSize());
            }

            this.initializeSenderThread();
         }
      }
   }

   public synchronized void stop() {
      if (this.senderThread != null) {
         this.senderThread.shutdown();
         this.senderThread = null;
      }

      if (this.blockingQueue != null) {
         this.blockingQueue.clear();
         this.blockingQueue = null;
      }

      this.running = false;
   }

   public boolean isRunning() {
      return this.running;
   }

   public void publish(Message message) {
      if (this.enabled && this.running) {
         try {
            LOG.fine("Enqueueing syslog message");
            this.checkMessageQueueSizeCallback();
            if (!this.blockingQueue.offer(message)) {
               this.overflowCount.incrementAndGet();
               LOG.severe("Syslog overflow encountered. Unable to add messages to the queue.");
            }
         } catch (Throwable t) {
            LOG.log(Level.SEVERE, "Error publishing syslog message.", t);
         }
      }
   }

   private static boolean loadSyslogFeature() {
      licenseFileChecked = true;
      File licenseDirFile;
      if (SubscriptionLicenseUtil.getLicenseMode() == LicenseMode.SUBSCRIPTION) {
         licenseDirFile = NiagaraFiles.getSubscriptionLicensePath();
      } else {
         licenseDirFile = NiagaraFiles.getPerpetualLicensePath();
      }

      if (licenseDirFile.isDirectory()) {
         File[] files = licenseDirFile.listFiles();
         if (files != null) {
            for (File file : files) {
               if (file.getName().endsWith(".license")) {
                  try (BufferedReader reader = new BufferedReader(new InputStreamReader(Files.newInputStream(file.toPath()), StandardCharsets.UTF_8))) {
                     String license = reader.lines().collect(Collectors.joining("\n"));
                     if (license.contains("vendor=\"Tridium\"")
                        && license.contains(SyslogManager.PlatformProviderHolder.PLATFORM_PROVIDER_INSTANCE.getHostId())
                        && license.contains("syslog")) {
                        syslogFeatureFound = true;
                        break;
                     }
                  } catch (IOException e) {
                     if (LOG.isLoggable(Level.FINE)) {
                        LOG.log(Level.WARNING, "Failed to read the license file.", e);
                     } else {
                        LOG.log(Level.WARNING, "Failed to read the license file: " + e);
                     }
                  }
               }
            }
         }
      }

      return syslogFeatureFound;
   }

   public boolean isSyslogLicensed() {
      return licenseFileChecked ? syslogFeatureFound : loadSyslogFeature();
   }

   public boolean isStationSyslogDisabled() {
      return !this.getEnabled() ? true : !this.getStationAuditEnabled() && !this.getSecurityAuditEnabled() && !this.getStationLogEnabled();
   }

   public boolean isPlatformSyslogDisabled() {
      return !this.getEnabled() ? true : !this.getPlatformLogEnabled();
   }

   private static void setSyslogLogHandlerFilterLevel(Level filterLevel) {
      for (Handler handler : Logger.getLogger("").getHandlers()) {
         if (handler instanceof SyslogLogHandler) {
            handler.setLevel(filterLevel);
         }
      }
   }

   private void checkMessageQueueSizeCallback() {
      for (ISyslogStatusListener syslogStatusListener : this.syslogStatusListenerList) {
         syslogStatusListener.onSyslogMessageQueueChanged(this.getQueueFullPercentage());
      }
   }

   public boolean isSyslogServerConnected() {
      return this.isServerConnected;
   }

   public int getQueueFullPercentage() {
      return this.blockingQueue == null ? 0 : this.blockingQueue.size() * 100 / (this.blockingQueue.size() + this.blockingQueue.remainingCapacity());
   }

   public void updateServerConnectionStatus(boolean isConnected, String msg) {
      this.isServerConnected = isConnected;

      for (ISyslogStatusListener syslogStatusListener : this.syslogStatusListenerList) {
         syslogStatusListener.onSyslogServerConnectionStatusChanged(isConnected, msg);
      }
   }

   public void enableFileMonitor() {
      if (this.isSyslogLicensed()) {
         this.fileMonitor.getAndUpdate(optionalExecutor -> {
            if (optionalExecutor.isPresent()) {
               return optionalExecutor;
            }

            ScheduledExecutorService result = Executors.newSingleThreadScheduledExecutor(FILE_MONITOR_THREAD_FACTORY);
            result.scheduleAtFixedRate(this::checkFileMonitor, 10000L, 10000L, TimeUnit.MILLISECONDS);
            return Optional.of(result);
         });
      }
   }

   public void disableFileMonitor() {
      this.fileMonitor.getAndUpdate(optionalExecutor -> {
         if (optionalExecutor.isPresent()) {
            ((ScheduledExecutorService)optionalExecutor.get()).shutdownNow();

            try {
               boolean timeoutElapsed = !((ScheduledExecutorService)optionalExecutor.get()).awaitTermination(15000L, TimeUnit.MILLISECONDS);
               if (timeoutElapsed) {
                  LOG.fine("Timeout elapsed while disabling Syslog properties monitor");
               }

               LOG.fine("Syslog properties monitor disabled");
            } catch (InterruptedException var2) {
            }

            return Optional.empty();
         } else {
            return optionalExecutor;
         }
      });
   }

   private void checkFileMonitor() {
      synchronized (PAUSE_STATE_MONITOR) {
         if (!this.checkPaused) {
            getInstance().checkLastModified();
         }
      }
   }

   private synchronized void checkLastModified() {
      if (SYSLOG_PROPERTIES_FILE.lastModified() != this.lastModified) {
         if (this.lastModified == 0L) {
            this.lastModified = SYSLOG_PROPERTIES_FILE.lastModified();
         } else {
            try {
               LOG.fine("Detected syslog properties file change. Reloading...");
               this.reloadProperties();
               this.lastModified = SYSLOG_PROPERTIES_FILE.lastModified();
            } catch (Exception exception) {
               LOG.log(Level.WARNING, "Exception occurred in SyslogManager checkLastModified", exception);
            }
         }
      }
   }

   public void pauseFileMonitor() {
      synchronized (PAUSE_STATE_MONITOR) {
         this.checkPaused = true;
      }
   }

   public void resumeFileMonitor() {
      synchronized (PAUSE_STATE_MONITOR) {
         this.checkPaused = false;
      }
   }

   boolean isValidEnvironment(String environmentTag) {
      if (environmentTag.toLowerCase(Locale.ENGLISH).startsWith("station")) {
         environmentTag = "station";
      }

      return VALID_ENVIRONMENT_TAGS.contains(environmentTag);
   }

   private static final class PlatformProviderHolder {
      private static final IPlatformProvider PLATFORM_PROVIDER_INSTANCE = AccessController.doPrivileged(PlatformUtil::getPlatformProvider);
   }

   private static final class SyslogManagerInstance {
      public static final SyslogManager INSTANCE = new SyslogManager();

      static {
         INSTANCE.enableFileMonitor();
      }
   }
}
