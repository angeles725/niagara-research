package com.tridium.niagarad;

import com.tridium.crypto.core.cert.JarSignatureRegistry;
import com.tridium.niagarad.app.App;
import com.tridium.niagarad.app.AppRegistry;
import com.tridium.niagarad.crypto.DaemonCryptoManager;
import com.tridium.niagarad.file.FileStore;
import com.tridium.niagarad.http.Authenticator;
import com.tridium.niagarad.http.Http;
import com.tridium.niagarad.http.WebServer;
import com.tridium.niagarad.io.OutputBuffer;
import com.tridium.niagarad.io.OutputBufferList;
import com.tridium.niagarad.license.Brand;
import com.tridium.niagarad.license.LicenseManager;
import com.tridium.niagarad.log.LogBufferHandler;
import com.tridium.niagarad.log.NiagaraDaemonLogSettings;
import com.tridium.niagarad.log.NullLogger;
import com.tridium.niagarad.security.policy.NoOpPermissionGroupStore;
import com.tridium.niagarad.servlet.AccountManagementServlet;
import com.tridium.niagarad.servlet.AppListServlet;
import com.tridium.niagarad.servlet.AppServlet;
import com.tridium.niagarad.servlet.AuthServlet;
import com.tridium.niagarad.servlet.BatchServlet;
import com.tridium.niagarad.servlet.CheckServlet;
import com.tridium.niagarad.servlet.ClearDaemonOutputServlet;
import com.tridium.niagarad.servlet.CryptoServlet;
import com.tridium.niagarad.servlet.DebugServlet;
import com.tridium.niagarad.servlet.DefaultServlet;
import com.tridium.niagarad.servlet.DhcpdServlet;
import com.tridium.niagarad.servlet.DiagnosticServlet;
import com.tridium.niagarad.servlet.FileServlet;
import com.tridium.niagarad.servlet.FilteredLogServlet;
import com.tridium.niagarad.servlet.GetDaemonOutputServlet;
import com.tridium.niagarad.servlet.IEEE8021XServlet;
import com.tridium.niagarad.servlet.JarServlet;
import com.tridium.niagarad.servlet.LinkServlet;
import com.tridium.niagarad.servlet.ModuleInfoServlet;
import com.tridium.niagarad.servlet.NreConfigurationServlet;
import com.tridium.niagarad.servlet.PlatformInfoServlet;
import com.tridium.niagarad.servlet.PlatformLoginFileServlet;
import com.tridium.niagarad.servlet.PlatformLoginServlet;
import com.tridium.niagarad.servlet.RebootServlet;
import com.tridium.niagarad.servlet.ReportServlet;
import com.tridium.niagarad.servlet.RequestStateServlet;
import com.tridium.niagarad.servlet.Servlet;
import com.tridium.niagarad.servlet.SharedKeyServlet;
import com.tridium.niagarad.servlet.SshdServlet;
import com.tridium.niagarad.servlet.StopServlet;
import com.tridium.niagarad.servlet.SyslogServlet;
import com.tridium.niagarad.servlet.SystemLogServlet;
import com.tridium.niagarad.servlet.SystemPasswordServlet;
import com.tridium.niagarad.servlet.TcpIpServlet;
import com.tridium.niagarad.servlet.TimeServlet;
import com.tridium.niagarad.servlet.UpdateDaemonServlet;
import com.tridium.niagarad.servlet.qnx.QnxOsUpdateServlet;
import com.tridium.niagarad.servlet.qnx.QnxServlet;
import com.tridium.niagarad.servlet.qnx.QnxUsbBackupServlet;
import com.tridium.niagarad.servlet.qnx.QnxWiFiServlet;
import com.tridium.niagarad.util.DaemonAuthUtil;
import com.tridium.niagarad.util.RequestState;
import com.tridium.nre.di.NreInstantiationException;
import com.tridium.nre.di.NreInstantiator;
import com.tridium.nre.di.SingletonSupplier;
import com.tridium.nre.di.TypeSupplier;
import com.tridium.nre.platform.IPlatformProvider;
import com.tridium.nre.platform.NJavaPlatformProvider;
import com.tridium.nre.platform.NativePlatformProviderTridium;
import com.tridium.nre.platform.OperatingSystemEnum;
import com.tridium.nre.platform.PlatformUtil;
import com.tridium.nre.security.DefaultSecurityInitializerConfig;
import com.tridium.nre.security.DeveloperSecurityManager;
import com.tridium.nre.security.ISecurityInfoProvider;
import com.tridium.nre.security.ISecurityInitializer;
import com.tridium.nre.security.KeyRing;
import com.tridium.nre.security.KeyRingFactory;
import com.tridium.nre.security.NiagaraBasicPermission;
import com.tridium.nre.security.SecurityInitializer;
import com.tridium.nre.security.TextFileSignatureVerifier;
import com.tridium.nre.security.TextFileSignatureVerifier.CommentLine;
import com.tridium.nre.security.policy.NiagaraPolicy;
import com.tridium.nre.security.policy.NiagaraPolicyUtil;
import com.tridium.nre.security.policy.NiagaraPolicy.PolicyType;
import com.tridium.nre.syslog.SyslogLogHandler;
import com.tridium.nre.syslog.SyslogManager;
import com.tridium.nre.util.IPAddressUtil;
import com.tridium.nre.util.MqUtil;
import com.tridium.nre.util.NiagaraFiles;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.AccessController;
import java.security.Policy;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.TimeZone;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.nre.util.TextUtil;
import javax.baja.xml.XElem;
import javax.baja.xml.XParser;
import sun.misc.Signal;
import sun.misc.SignalHandler;

public class NiagaraDaemon {
   public static final int _RC_OK = 0;
   public static final int _RC_IMPROPER_USAGE = 1;
   public static final int _RC_ERROR = 2;
   public static boolean _FORCE = false;
   public static boolean _CONSOLE = false;
   public static boolean _ECHO_STATION = false;
   private static final NreInstantiator instantiator = new NreInstantiator();
   private static IPlatformProvider platformProvider;
   static NiagaraDaemon _INSTANCE = null;
   public static final String FILE_ENCODING = AccessController.doPrivileged(() -> System.getProperty("file.encoding"));
   public static final String OS_NAME = AccessController.doPrivileged(() -> System.getProperty("os.name"));
   public static final String NIAGARA_HOME = AccessController.doPrivileged(() -> System.getProperty("niagara.home"));
   public static final String NIAGARA_USER_HOME = AccessController.doPrivileged(() -> System.getProperty("niagara.user.home"));
   public static final String FILESTORE_SHARED_MEMORY_PATH = AccessController.doPrivileged(() -> System.getProperty("niagara.filestore.shared.memory"));
   public static final String FILESTORE_NIAGARA_HOME_SYMLINKS = AccessController.doPrivileged(() -> System.getProperty("niagara.filestore.niagara.symlinks"));
   public static final String FILESTORE_NIAGARA_USER_HOME_SYMLINKS = AccessController.doPrivileged(
      () -> System.getProperty("niagara.filestore.niagara_user.symlinks")
   );
   public static final String NIAGARA_USER_HOME_DAEMON_PATH = NIAGARA_USER_HOME + File.separator + "daemon";
   public static final String NIAGARA_DAEMON_PROPERTIES_PATH = NIAGARA_USER_HOME_DAEMON_PATH + File.separator + "daemon.properties";
   private static final String NIAGARA_HOME_SYSTEM_PROPERTIES_PATH = NIAGARA_HOME + File.separator + "defaults" + File.separator + "system.properties";
   private static final String NIAGARA_USER_HOME_SYSTEM_PROPERTIES_PATH = NIAGARA_USER_HOME + File.separator + "etc" + File.separator + "system.properties";
   public static Properties props = initProperties();
   private static final String ROLL_KEY_MATERIAL_COOKIE_PATH = NIAGARA_USER_HOME_DAEMON_PATH + File.separator + "roll-key-material";
   private static final String LEGACY_ROLL_KEY_MATERIAL_COOKIE_PATH = "/var/cookies/roll-key-material";
   public static OutputBuffer niagaraDaemonOutputBuffer = initDaemonOutputBuffer();
   public static final String NIAGARA_DAEMON_LOGGING_PROPERTIES_PATH = NIAGARA_USER_HOME_DAEMON_PATH + File.separator + "daemonlog.properties";
   private static boolean saveLogFileAfterInitialization = false;
   public static NiagaraDaemonLogSettings niagaraDaemonLogSettings = initNiagaraDaemonLogSettings();
   private static String timeZoneId = null;
   public WebServer webServer;
   public Authenticator auth;
   public String timeStamp = null;
   public String daemonVersion;
   private final AppRegistry stationRegistry;
   private JarSignatureRegistry signatureRegistry = null;
   private static final Logger LOGGER = Logger.getLogger("niagarad");
   private static final long millisAtBootstrap = System.currentTimeMillis();
   private static long millisAtServiceStart;
   private static boolean debugSupported;
   static volatile boolean serviceStopped = false;
   public static final String NIAGARAD_APPLICATION_TYPE = "niagarad";
   public static final String NIAGARAD_APPLICATION_NAME = "niagarad";
   public static final int NIAGARAD_STATUS_IDLE = 0;
   public static final int NIAGARAD_STATUS_STARTING = 1;
   public static final int NIAGARAD_STATUS_RUNNING = 2;
   public static final int NIAGARAD_STATUS_STOPPING = 3;
   public static final int NIAGARAD_STATUS_FAILED = 4;
   public static final int NIAGARAD_STATUS_UNKNOWN = 5;
   public static final int NIAGARAD_STATUS_HALTED = 6;
   private int status = 5;
   public static final long WEBSERVER_RESTART_TIME = 3500L;
   private static NiagaraDaemon.ExternalMonitorThread externalMonitorThread = null;
   public static final String _MQUEUE_SEND = "/niagarad-to-os";
   public static final String _MQUEUE_RECEIVE = "/os-to-niagarad";
   public final Object clientMonitor = new Object();
   public boolean permanentlyLocked = false;
   public Thread owner = null;
   public int clientCount = 1;
   private static volatile ISecurityInfoProvider secInfProvider;

   public static void Main(String[] args) {
      NiagaraPolicy niagaraPolicy = new NiagaraPolicy(true);
      Policy.setPolicy(niagaraPolicy);
      niagaraPolicy.bootstrap();
      NiagaraPolicyUtil.init(new NoOpPermissionGroupStore(), PolicyType.DAEMON);

      try {
         instantiator.instance(ISecurityInitializer.class);
      } catch (NreInstantiationException nie) {
         if (nie.getCause() != null) {
            System.err.println("SEVERE [" + new Date() + "][niagarad] failed to instantiate security initializer (" + nie.getCause() + ")");
         } else {
            System.err.println("SEVERE [" + new Date() + "][niagarad] failed to instantiate security initializer (" + nie + ")");
         }

         nie.printStackTrace();
         System.exit(2);
      }

      platformProvider = PlatformUtil.getPlatformProvider();
      NiagaraFiles.unpackDefaults();
      System.setProperty("NiagaraDaemon", "true");
      parseArguments(args);
      SyslogManager syslogManager = SyslogManager.getInstance();
      if (syslogManager.isSyslogLicensed()) {
         Logger.getLogger("").addHandler(new SyslogLogHandler());
         syslogManager.setEnvironmentTag("platform");
      }

      if (syslogManager.getEnabled()) {
         syslogManager.start();
      }

      if (_CONSOLE) {
         consoleMain(args);
      } else {
         if (PlatformUtil.isTridiumPlatform() && !((NativePlatformProviderTridium)platformProvider).daemonize()) {
            System.err.println("SEVERE [" + new Date() + "][niagarad] failed to daemonize process, exiting");
            System.exit(2);
         }

         serviceMain(args);
      }

      System.exit(0);
   }

   public static NiagaraDaemon getInstance() {
      return _INSTANCE;
   }

   private static Properties initProperties() {
      String nuhEtcPath = NIAGARA_USER_HOME + File.separator + "etc";
      File nuhEtcFile = new File(nuhEtcPath);
      if (!nuhEtcFile.exists() && !nuhEtcFile.mkdirs()) {
         System.err.println("SEVERE [" + new Date() + "][niagarad] unable to create '" + nuhEtcPath + "' directory, can not start");
         System.exit(2);
      }

      File niagaraHomeSystemPropertiesFile = new File(NIAGARA_HOME_SYSTEM_PROPERTIES_PATH);
      File niagaraUserHomeSystemPropertiesFile = new File(NIAGARA_USER_HOME_SYSTEM_PROPERTIES_PATH);
      File activePropertiesFile = niagaraUserHomeSystemPropertiesFile.exists() ? niagaraUserHomeSystemPropertiesFile : niagaraHomeSystemPropertiesFile;
      Properties systemProperties = System.getProperties();

      try (FileInputStream systemPropertiesIn = new FileInputStream(activePropertiesFile)) {
         Properties sysProps = new Properties();
         sysProps.load(systemPropertiesIn);

         for (Object keyObject : sysProps.keySet()) {
            String key = (String)keyObject;
            systemProperties.setProperty(key, sysProps.getProperty(key).trim());
         }
      } catch (IOException var43) {
      }

      File workingDaemonProps = new File(NIAGARA_DAEMON_PROPERTIES_PATH + ".working");
      File daemonProps = new File(NIAGARA_DAEMON_PROPERTIES_PATH);
      if (workingDaemonProps.exists() && !daemonProps.exists()) {
         try {
            Files.move(workingDaemonProps.toPath(), daemonProps.toPath(), StandardCopyOption.ATOMIC_MOVE);
         } catch (IOException ioe) {
            System.err.println("SEVERE [" + new Date() + "][niagarad] failure renaming 'daemon.properties' working file on start (" + ioe + ")");
            ioe.printStackTrace();
            System.exit(2);
         }
      }

      Properties props = new Properties();
      FileInputStream daemonInput = null;

      try {
         if (!daemonProps.exists()) {
            if (!daemonProps.getParentFile().exists() && !daemonProps.getParentFile().mkdirs()) {
               System.err
                  .println("SEVERE [" + new Date() + "][niagarad] failed to create '" + daemonProps.getParentFile().getPath() + "', can not load properties");
               System.exit(2);
            }

            if (!daemonProps.createNewFile()) {
               System.err.println("SEVERE [" + new Date() + "][niagarad] failed to create 'daemon.properties', can not load properties");
               System.exit(2);
            }
         }

         daemonInput = new FileInputStream(daemonProps);
         props.load(daemonInput);
      } catch (IOException ioException) {
         System.err.println("SEVERE [" + new Date() + "][niagarad] failed to load 'daemon.properties' (" + ioException + ")");
         ioException.printStackTrace();
         System.exit(2);
      } finally {
         if (daemonInput != null) {
            try {
               daemonInput.close();
            } catch (IOException var36) {
            }
         }
      }

      return props;
   }

   public static boolean reloadProperties() {
      return reloadProperties(NIAGARA_DAEMON_PROPERTIES_PATH);
   }

   public static boolean reloadProperties(String propertyFilePath) {
      File targetDaemonProperties = new File(propertyFilePath);
      if (!targetDaemonProperties.exists()) {
         return false;
      }

      getFilter().fine("reloading properties from " + propertyFilePath);
      Properties props = new Properties();

      try (FileInputStream daemonInput = new FileInputStream(targetDaemonProperties)) {
         props.load(daemonInput);
      } catch (IOException e) {
         if (!NIAGARA_DAEMON_PROPERTIES_PATH.equals(propertyFilePath) && !targetDaemonProperties.delete()) {
            getFilter().warning("failed to delete " + propertyFilePath);
         }

         return false;
      }

      NiagaraDaemon.props.clear();
      Enumeration<Object> keys = props.keys();

      while (keys.hasMoreElements()) {
         String key = (String)keys.nextElement();
         NiagaraDaemon.props.setProperty(key, props.getProperty(key));
      }

      if (!NIAGARA_DAEMON_PROPERTIES_PATH.equals(propertyFilePath) && !targetDaemonProperties.delete()) {
         getFilter().warning("failed to delete " + propertyFilePath);
      }

      if (!NIAGARA_DAEMON_PROPERTIES_PATH.equals(propertyFilePath)) {
         saveProperties();
      }

      return true;
   }

   public static void saveProperties() {
      File workingDaemonProps = new File(NIAGARA_DAEMON_PROPERTIES_PATH + ".working");
      File daemonProps = new File(NIAGARA_DAEMON_PROPERTIES_PATH);

      try (FileOutputStream out = new FileOutputStream(workingDaemonProps)) {
         props.store(out, "Do not modify the contents of this file");
         out.getFD().sync();
      } catch (IOException ioe) {
         getFilter().log(Level.SEVERE, "failed to save working 'daemon.properties' file", ioe);
         return;
      }

      try {
         Files.move(workingDaemonProps.toPath(), daemonProps.toPath(), StandardCopyOption.ATOMIC_MOVE);
      } catch (IOException ioe) {
         getFilter().log(Level.SEVERE, "failed to move working 'daemon.properties' file", ioe);
      }
   }

   static OutputBuffer initDaemonOutputBuffer() {
      String value = props.getProperty("daemon.logbuffersize", String.valueOf(262144));

      int bufferSize;
      try {
         bufferSize = Integer.parseInt(value);
         if (bufferSize < 0) {
            throw new NumberFormatException();
         }
      } catch (NumberFormatException nre) {
         System.err.println("WARNING [" + new Date() + "][niagarad] invalid value '" + value + "' specified for daemon.logbuffersize, using default");
         bufferSize = 262144;
      }

      return new OutputBuffer(bufferSize, Logger.getLogger("daemonOut"));
   }

   static NiagaraDaemonLogSettings initNiagaraDaemonLogSettings() {
      File workingLogProps = new File(NIAGARA_DAEMON_LOGGING_PROPERTIES_PATH + ".working");
      File logProps = new File(NIAGARA_DAEMON_LOGGING_PROPERTIES_PATH);
      if (!logProps.getParentFile().exists() && !logProps.getParentFile().mkdirs()) {
         System.err.println("SEVERE [" + new Date() + "][niagarad] could not configure logging, failed to create parent directory");
         return null;
      }

      if (workingLogProps.exists() && !logProps.exists()) {
         try {
            Files.move(workingLogProps.toPath(), logProps.toPath(), StandardCopyOption.ATOMIC_MOVE);
         } catch (IOException ioe) {
            System.err.println("SEVERE [" + new Date() + "][niagarad] failure renaming 'daemonlog.properties' working file on start (" + ioe + ")");
            ioe.printStackTrace();
            return null;
         }
      }

      try {
         boolean createDefaultLogFile = false;
         if (!logProps.exists()) {
            createDefaultLogFile = true;
         } else {
            try (BufferedReader reader = new BufferedReader(new FileReader(logProps))) {
               String firstLine = reader.readLine();
               if (firstLine == null || !firstLine.startsWith("# DO NOT MODIFY: ")) {
                  createDefaultLogFile = true;
               }
            }
         }

         if (createDefaultLogFile) {
            try (FileOutputStream out = new FileOutputStream(logProps)) {
               NiagaraDaemonLogSettings.writeDefaultLogConfig(out);
               saveLogFileAfterInitialization = true;
            }
         }
      } catch (Exception e) {
         System.err.println("SEVERE [" + new Date() + "][niagarad] could not configure logging (" + e + ")");
         e.printStackTrace();
         return null;
      }

      try (FileInputStream fin = new FileInputStream(logProps)) {
         NiagaraDaemonLogSettings.bootstrap(fin);
      } catch (Exception e) {
         System.err.println("SEVERE [" + new Date() + "][niagarad] could not bootstrap logging (" + e + ")");
         e.printStackTrace();
         return null;
      }

      Logger.getLogger("").addHandler(new LogBufferHandler());
      NiagaraDaemonLogSettings logSettings = NiagaraDaemonLogSettings.getInstance();
      if (!logSettings.getDeclaredLogs().contains("org.bouncycastle")) {
         logSettings.setLogLevel("org.bouncycastle", Level.SEVERE);
         saveLogFileAfterInitialization = true;
      }

      return logSettings;
   }

   private static boolean saveDeclaredLogSettings() {
      return saveLogSettings(true);
   }

   public static boolean saveLogSettings() {
      return saveLogSettings(false);
   }

   private static boolean saveLogSettings(boolean declaredOnly) {
      if (niagaraDaemonLogSettings.getForceLevel() != null) {
         return false;
      }

      File workingLoggingProps = new File(NIAGARA_DAEMON_LOGGING_PROPERTIES_PATH + ".working");
      File loggingProps = new File(NIAGARA_DAEMON_LOGGING_PROPERTIES_PATH);

      try (FileOutputStream out = new FileOutputStream(workingLoggingProps)) {
         niagaraDaemonLogSettings.save(out, declaredOnly);
         out.getFD().sync();
      } catch (Exception e) {
         getFilter().log(Level.SEVERE, "failed to save working 'daemonlog.properties' file", e);
         return false;
      }

      try {
         Files.move(workingLoggingProps.toPath(), loggingProps.toPath(), StandardCopyOption.ATOMIC_MOVE);
         return true;
      } catch (Exception e) {
         getFilter().log(Level.SEVERE, "failed to move working 'daemonlog.properties' file", e);
         return false;
      }
   }

   public static String getTimeZoneId() {
      return getTimeZoneId(false);
   }

   public static synchronized String getTimeZoneId(boolean refresh) {
      if (refresh || timeZoneId == null) {
         timeZoneId = TimeZone.getDefault().getID();
         if (getFilter().isLoggable(Level.FINE)) {
            getFilter().fine("using local timezone \"" + TimeZone.getDefault().getID() + '"');
         }
      }

      return timeZoneId;
   }

   public static void parseArguments(String[] arguments) {
      if (arguments != null && arguments.length != 0) {
         for (String argument : arguments) {
            String current = argument;
            if (current.equalsIgnoreCase("/console")) {
               _CONSOLE = true;
            } else if (current.equalsIgnoreCase("/trace")) {
               _FORCE = true;
               niagaraDaemonLogSettings.forceLevel(Level.FINE);
               getFilter().fine("forcing all filters to trace -> saving will be blocked!");
            } else if (current.equalsIgnoreCase("/echostation")) {
               _ECHO_STATION = true;
            } else {
               usage();
               System.exit(1);
            }
         }
      }
   }

   public static void usage() {
      System.out.println("Niagara Platform Daemon");
      System.out.println("Usage: niagarad  [options]");
      System.out.println();
      System.out.println("options:");
      System.out.println("\t/trace        Force all daemon output to trace");
      System.out.println("\t/console      Do not detach from the controlling console");
      System.out.println("\t/echostation  Echo station output to daemon output");
      System.out.println();
   }

   static NiagaraDaemon create(boolean consoleAvailable) {
      _INSTANCE = new NiagaraDaemon(consoleAvailable);
      return _INSTANCE;
   }

   static void consoleMain(String[] args) {
      NiagaraDaemon nd = create(true);
      nd.setDaemonStatus(1);

      try {
         nd.init(args);
      } catch (Throwable t) {
         getFilter().log(Level.SEVERE, "throwable occurred while initializing daemon, can not continue", t);
         nd.setDaemonStatus(4);
         return;
      }

      if (!nd.start()) {
         getFilter().log(Level.SEVERE, "niagara daemon failed to start properly, can not continue");
         nd.stop();
         nd.setDaemonStatus(4);
      } else {
         millisAtServiceStart = System.currentTimeMillis();
         getFilter().info("startup complete (" + (millisAtServiceStart - millisAtBootstrap) + "ms)");
         nd.setDaemonStatus(2);
         System.out.println("press enter to stop\n");

         try {
            System.in.read();
         } catch (IOException var3) {
         }

         nd.stop();
      }
   }

   static void serviceMain(String[] args) {
      NiagaraDaemon nd = create(true);
      nd.setDaemonStatus(1);

      try {
         if (platformProvider.canWriteSystemLogMessages()) {
            niagaraDaemonLogSettings.enableSystemLogging(platformProvider);
         }

         nd.init(args);
      } catch (Throwable t) {
         getFilter().log(Level.SEVERE, "throwable occurred while initializing daemon, can not continue", t);
         nd.setDaemonStatus(4);
         return;
      }

      if (nd.start()) {
         millisAtServiceStart = System.currentTimeMillis();
         getFilter().info("startup complete (" + (millisAtServiceStart - millisAtBootstrap) + "ms)");
         nd.setDaemonStatus(2);

         while (true) {
            try {
               Thread.sleep(1000L);
            } catch (InterruptedException var3) {
            }
         }
      }

      getFilter().log(Level.SEVERE, "niagara daemon failed to start properly, can not continue");
      nd.stop();
      nd.setDaemonStatus(4);
   }

   NiagaraDaemon(boolean consoleAvailable) {
      this.auth = null;
      niagaraDaemonLogSettings.enableConsoleLogging(consoleAvailable);
      Logger filter = Logger.getLogger("webserver");
      this.webServer = new WebServer(props, filter);
      File niagaradJarPath = new File(NIAGARA_HOME + File.separator + "bin" + File.separator + "ext" + File.separator + "niagarad.jar");

      try (JarFile niagaradJar = new JarFile(niagaradJarPath)) {
         JarEntry moduleXmlEntry = niagaradJar.getJarEntry("META-INF/module.xml");
         XElem moduleElement = XParser.make(new BufferedInputStream(niagaradJar.getInputStream(moduleXmlEntry))).parse();
         this.daemonVersion = moduleElement.get("vendorVersion", "4.0.0");
      } catch (Exception ioe) {
         this.daemonVersion = "4.0.0";
      }

      Logger log = Logger.getLogger("stationRegistry");
      String appDir = NIAGARA_USER_HOME + File.separator + "stations";
      this.stationRegistry = new AppRegistry(log, consoleAvailable, appDir, "station", platformProvider);
   }

   void verifyPolicyFiles() {
      String securityFileName = new File(NiagaraFiles.getSecurityPolicyPath(), "java.security").getAbsolutePath();

      try {
         TextFileSignatureVerifier.verifyFile(securityFileName, CommentLine.SECURITY, getSecurityInfoProvider());
      } catch (Exception e) {
         getFilter().log(Level.SEVERE, "security file verification failed. Security files may have been tampered with (" + e + ")", e);
         throw new SecurityException("security file verification failed", e);
      }

      String policyFileName = new File(NiagaraFiles.getSecurityPolicyPath(), "java.policy").getAbsolutePath();

      try {
         TextFileSignatureVerifier.verifyFile(policyFileName, CommentLine.POLICY, getSecurityInfoProvider());
      } catch (Exception e) {
         getFilter().log(Level.SEVERE, "policy file verification failed. Policy files may have been tampered with (" + e + ")", e);
         throw new SecurityException("policy file verification failed", e);
      }
   }

   void validateSystemTime() {
      long rightNowMillis = System.currentTimeMillis();
      if (rightNowMillis < 1420070400000L) {
         Date rightNowDate = new Date(rightNowMillis);
         SimpleDateFormat format = new SimpleDateFormat("yyyy-MMM-dd HH:mm:ss");
         if (platformProvider.isEmbedded() && !platformProvider.isSystemTimeReadonly()) {
            long licenseGeneratedMillis = LicenseManager.getInstance(NullLogger.getInstance()).tridiumGeneratedDate;
            long signingPropertiesMillis = 0L;

            try {
               File signingPropertiesFile = new File(NiagaraFiles.getSecurityPolicyPath(), "signing.properties");

               try (InputStream inputStream = new FileInputStream(signingPropertiesFile)) {
                  Properties signingProperties = new Properties();
                  signingProperties.load(inputStream);
                  signingPropertiesMillis = Long.parseLong(signingProperties.getProperty("notBefore"));
               }
            } catch (Exception var23) {
            }

            if (signingPropertiesMillis <= 0L && licenseGeneratedMillis <= 0L) {
               getFilter().warning("system clock is set to past date \"" + format.format(rightNowDate) + "\", errors may occur!");
            } else {
               long newMillis = Math.max(signingPropertiesMillis, licenseGeneratedMillis);
               if (platformProvider.setSystemTime(newMillis) == 0) {
                  getFilter()
                     .warning(
                        "system clock was set to past date \""
                           + format.format(rightNowDate)
                           + "\" and clock was adjusted to \""
                           + format.format(new Date(newMillis))
                     );
               } else {
                  getFilter()
                     .warning("system clock was set to past date \"" + format.format(rightNowDate) + "\" but clock adjustment failed, errors may occur!");
               }
            }
         } else {
            getFilter().warning("system clock is set to past date \"" + format.format(rightNowDate) + "\", errors may occur!");
         }
      }
   }

   void registerPlatformServlets() {
      if (!(platformProvider instanceof NJavaPlatformProvider)) {
         if (PlatformUtil.isTridiumPlatform() && OperatingSystemEnum.isOS(OperatingSystemEnum.qnx)) {
            Servlet s;
            if (!this.webServer.registerServlet(s = new FileServlet("files", "/", this, false, platformProvider))) {
               getFilter().severe("servlet <" + s.getName() + "> failed to initialize");
            }

            if (!this.webServer.registerServlet(s = new QnxOsUpdateServlet())) {
               getFilter().severe("servlet <" + s.getName() + "> failed to initialize");
            }

            QnxServlet var10;
            if (!this.webServer.registerServlet(var10 = new QnxServlet())) {
               getFilter().severe("servlet <" + var10.getName() + "> failed to initialize");
            }

            if ("TITAN".equalsIgnoreCase(platformProvider.getHostModel())) {
               QnxUsbBackupServlet var11;
               if (!this.webServer.registerServlet(var11 = new QnxUsbBackupServlet())) {
                  getFilter().severe("servlet <" + var11.getName() + "> failed to initialize");
               }

               QnxWiFiServlet var12;
               if (!this.webServer.registerServlet(var12 = new QnxWiFiServlet())) {
                  getFilter().severe("servlet <" + var12.getName() + "> failed to initialize");
               }
            }
         }

         Servlet s;
         if (platformProvider.isEmbedded() && !this.webServer.registerServlet(s = new RebootServlet())) {
            getFilter().severe("servlet <" + s.getName() + "> failed to initialize");
         }

         debugSupported = platformProvider.isDaemonDebugSupported();
         if (debugSupported) {
            if (!this.webServer.registerServlet(s = new DebugServlet())) {
               getFilter().severe("servlet <" + s.getName() + "> failed to initialize");
            }

            String supportAuthTypes = platformProvider.getSupportedAuthenticationTypes();
            String[] supportedAuthTypesArray = TextUtil.split(supportAuthTypes, ',');
            boolean foundScramScheme = false;

            for (String authType : supportedAuthTypesArray) {
               if (DaemonAuthUtil.isScramScheme(authType)) {
                  foundScramScheme = true;
                  break;
               }
            }

            if (foundScramScheme) {
               if (!this.webServer.registerServlet(s = new PlatformLoginServlet())) {
                  getFilter().severe("servlet <" + s.getName() + "> failed to initialize");
               }

               if (!this.webServer.registerServlet(s = new PlatformLoginFileServlet())) {
                  getFilter().severe("servlet <" + s.getName() + "> failed to initialize");
               }
            }
         }

         if (platformProvider.providesAccountManagement()) {
            String supportAuthTypes = platformProvider.getSupportedAuthenticationTypes();
            String[] supportedAuthTypesArray = TextUtil.split(supportAuthTypes, ',');
            boolean foundNativeScheme = false;

            for (String authType : supportedAuthTypesArray) {
               if (DaemonAuthUtil.isNativeScheme(authType)) {
                  foundNativeScheme = true;
                  break;
               }
            }

            if (foundNativeScheme) {
               if (!this.webServer.registerServlet(s = new AccountManagementServlet(platformProvider))) {
                  getFilter().severe("servlet <" + s.getName() + "> failed to initialize");
               }
            } else {
               getFilter().warning("file based authenticator in use, disabling account management servlet");
            }
         }

         if (platformProvider.supportsNREConfiguration() && !this.webServer.registerServlet(s = new NreConfigurationServlet(platformProvider))) {
            getFilter().severe("servlet <" + s.getName() + "> failed to initialize");
         }

         if (platformProvider.isSSHSupported() && !this.webServer.registerServlet(s = new SshdServlet(platformProvider))) {
            getFilter().severe("servlet <" + s.getName() + "> failed to initialize");
         }

         if (AccessController.doPrivileged(() -> Boolean.getBoolean("niagara.dhcpd.supported")) && !this.webServer.registerServlet(s = new DhcpdServlet())) {
            getFilter().severe("servlet <" + s.getName() + "> failed to initialize");
         }

         if (AccessController.doPrivileged(() -> Boolean.getBoolean("niagara.ieee8021x.supported"))
            && !this.webServer.registerServlet(s = new IEEE8021XServlet())) {
            getFilter().severe("servlet <" + s.getName() + "> failed to initialize");
         }

         if (AccessController.doPrivileged(() -> Boolean.getBoolean("niagara.link.supported")) && !this.webServer.registerServlet(s = new LinkServlet())) {
            getFilter().severe("servlet <" + s.getName() + "> failed to initialize");
         }

         if (platformProvider.canReadSystemLogMessages() && !this.webServer.registerServlet(s = new SystemLogServlet(platformProvider))) {
            getFilter().severe("servlet <" + s.getName() + "> failed to initialize");
         }

         if (platformProvider.supportsNativeDiagnostics() && !this.webServer.registerServlet(s = new DiagnosticServlet(platformProvider))) {
            getFilter().severe("servlet <" + s.getName() + "> failed to initialize");
         }
      }
   }

   private void init(String[] args) {
      checkSecurityManagerDisable();
      _CONSOLE = _CONSOLE | Boolean.parseBoolean(props.getProperty("console", "false"));
      _ECHO_STATION = _ECHO_STATION | Boolean.parseBoolean(props.getProperty("echoStation", "false"));
      getTimeZoneId();
      if (getFilter().isLoggable(Level.FINE)) {
         getFilter().fine("using default file encoding \"" + FILE_ENCODING + "\"");
      }

      LicenseManager.getInstance(NullLogger.getInstance()).reload(getFilter());
      Http.setDefaultMimeType("text/plain");
      WebServer.addConstantHttpHeader("Niagara-Platform", OS_NAME);
      WebServer.addConstantHttpHeader("Server", "Niagara Web Server/" + this.daemonVersion);
      String hostId = platformProvider.getHostId();
      if (hostId != null) {
         WebServer.addConstantHttpHeader("Niagara-HostId", hostId);
      }

      String brandId = Brand.getBrandId(getFilter());
      if (brandId != null) {
         WebServer.addConstantHttpHeader("Baja-Station-Brand", brandId);
      }

      Calendar now = Calendar.getInstance();
      this.timeStamp = now.get(1) + "-" + now.get(2) + "-" + now.get(5) + "-" + now.get(11) + "-" + now.get(12) + "-" + now.get(13);
      WebServer.addConstantHttpHeader("Niagara-Started", this.timeStamp);
      this.validateSystemTime();
      Http.loadMimeTable(getFilter(), NIAGARA_HOME + "/etc/extensions.properties");
      ISecurityInitializer initializer = (ISecurityInitializer)instantiator.instance(ISecurityInitializer.class);
      if (initializer.isFips() && !DaemonCryptoManager.isTlsAlgFipsApproved(props.getProperty("sslAlgType"))) {
         props.setProperty("sslAlgType", "tlsv1_3");
         saveProperties();
      }

      if (props.getProperty("tlsUseExtendedMasterSecret") == null) {
         props.setProperty("tlsUseExtendedMasterSecret", "true");
      }

      System.setProperty("jdk.tls.useExtendedMasterSecret", props.getProperty("tlsUseExtendedMasterSecret"));

      try {
         if (secInfProvider == null) {
            secInfProvider = initializer.getSecurityInfoProvider();
            File securityBasePath = new File(NIAGARA_USER_HOME, "security");
            KeyRing kr = KeyRingFactory.getInstance(securityBasePath, ".kr", ".km").getKeyRing();
            String rollCookiePath;
            if (PlatformUtil.isTridiumPlatform() && OperatingSystemEnum.isOS(OperatingSystemEnum.qnx)) {
               rollCookiePath = "/var/cookies/roll-key-material";
            } else {
               rollCookiePath = ROLL_KEY_MATERIAL_COOKIE_PATH;
            }

            File keyRollCookie = new File(rollCookiePath);
            if (keyRollCookie.exists()) {
               getFilter().log(Level.INFO, "found roll key material request, rolling key material...");
               kr.rollKeyMaterial();
               if (!keyRollCookie.delete()) {
                  getFilter().log(Level.WARNING, "failed to delete cookie '" + kr + "', key material may roll again on next boot");
                  keyRollCookie.deleteOnExit();
               }
            } else {
               kr.checkRollKeyMaterial(KeyRing.KEY_MATERIAL_ROLL_INTERVAL);
            }
         }
      } catch (Exception e) {
         getFilter().log(Level.SEVERE, "unable to initialize key ring (" + e + ")", e);
         throw new SecurityException("unable to initialize key ring", e);
      }

      this.verifyPolicyFiles();
      if (!AccessController.doPrivileged(() -> Boolean.getBoolean("niagara.disableSignatureRegistry"))) {
         this.signatureRegistry = JarSignatureRegistry.buildSignatureRegistry();
      }

      this.registerServlets(this.webServer);
      if (_FORCE) {
         niagaraDaemonLogSettings.forceLevel(Level.FINE);
      }
   }

   private void registerServlets(WebServer webServer) {
      webServer.setDefaultServlet(new DefaultServlet());
      Servlet s;
      if (!webServer.registerServlet(s = new CheckServlet())) {
         getFilter().severe("servlet <" + s.getName() + "> failed to initialize");
      }

      if (!webServer.registerServlet(s = new AuthServlet(platformProvider))) {
         getFilter().severe("servlet <" + s.getName() + "> failed to initialize");
      }

      RequestStateServlet var7;
      if (!webServer.registerServlet(var7 = new RequestStateServlet())) {
         getFilter().severe("servlet <" + var7.getName() + "> failed to initialize");
      }

      BatchServlet var8;
      if (!webServer.registerServlet(var8 = new BatchServlet())) {
         getFilter().severe("servlet <" + var8.getName() + "> failed to initialize");
      }

      FilteredLogServlet var9;
      if (!webServer.registerServlet(var9 = new FilteredLogServlet())) {
         getFilter().severe("servlet <" + var9.getName() + "> failed to initialize");
      }

      ReportServlet var10;
      if (!webServer.registerServlet(var10 = new ReportServlet())) {
         getFilter().severe("servlet <" + var10.getName() + "> failed to initialize");
      }

      StopServlet var11;
      if (!webServer.registerServlet(var11 = new StopServlet())) {
         getFilter().severe("servlet <" + var11.getName() + "> failed to initialize");
      }

      GetDaemonOutputServlet var12;
      if (!webServer.registerServlet(var12 = new GetDaemonOutputServlet())) {
         getFilter().severe("servlet <" + var12.getName() + "> failed to initialize");
      }

      ClearDaemonOutputServlet var13;
      if (!webServer.registerServlet(var13 = new ClearDaemonOutputServlet())) {
         getFilter().severe("servlet <" + var13.getName() + "> failed to initialize");
      }

      UpdateDaemonServlet var14;
      if (!webServer.registerServlet(var14 = new UpdateDaemonServlet(platformProvider))) {
         getFilter().severe("servlet <" + var14.getName() + "> failed to initialize");
      }

      ModuleInfoServlet var15;
      if (!webServer.registerServlet(var15 = new ModuleInfoServlet(platformProvider))) {
         getFilter().severe("servlet <" + var15.getName() + "> failed to initialize");
      }

      JarServlet var16;
      if (!webServer.registerServlet(var16 = new JarServlet())) {
         getFilter().severe("servlet <" + var16.getName() + "> failed to initialize");
      }

      PlatformInfoServlet var17;
      if (!webServer.registerServlet(var17 = new PlatformInfoServlet(platformProvider))) {
         getFilter().severe("servlet <" + var17.getName() + "> failed to initialize");
      }

      AppListServlet var18;
      if (!webServer.registerServlet(var18 = new AppListServlet())) {
         getFilter().severe("servlet <" + var18.getName() + "> failed to initialize");
      }

      TcpIpServlet var19;
      if (!webServer.registerServlet(var19 = new TcpIpServlet(platformProvider))) {
         getFilter().severe("servlet <" + var19.getName() + "> failed to initialize");
      }

      TimeServlet var20;
      if (!webServer.registerServlet(var20 = new TimeServlet(platformProvider))) {
         getFilter().severe("servlet <" + var20.getName() + "> failed to initialize");
      }

      SystemPasswordServlet var21;
      if (!webServer.registerServlet(var21 = new SystemPasswordServlet(platformProvider))) {
         getFilter().severe("servlet <" + var21.getName() + "> failed to initialize");
      }

      SharedKeyServlet var22;
      if (!webServer.registerServlet(var22 = new SharedKeyServlet())) {
         getFilter().severe("servlet <" + var22.getName() + "> failed to initialize");
      }

      CryptoServlet var23;
      if (!webServer.registerServlet(var23 = new CryptoServlet())) {
         getFilter().severe("servlet <" + var23.getName() + "> failed to initialize");
      }

      SyslogServlet var24;
      if (!webServer.registerServlet(var24 = new SyslogServlet())) {
         getFilter().severe("servlet <" + var24.getName() + "> failed to initialize");
      }

      AppServlet var25;
      if (this.stationRegistry != null && !webServer.registerServlet(var25 = new AppServlet("station", this.stationRegistry))) {
         getFilter().severe("servlet <" + var25.getName() + "> failed to initialize");
      }

      FileServlet niagaraHomeFileServlet = new FileServlet("niagara", NIAGARA_HOME, this, platformProvider.isNiagaraHomeReadonly(), platformProvider);
      if (!webServer.registerServlet(niagaraHomeFileServlet)) {
         getFilter().severe("servlet <" + niagaraHomeFileServlet.getName() + "> failed to initialize");
      }

      FileServlet niagaraUserHomeFileServlet = new FileServlet("niagara_user", NIAGARA_USER_HOME, this, false, platformProvider);
      if (!webServer.registerServlet(niagaraUserHomeFileServlet)) {
         getFilter().severe("servlet <" + niagaraUserHomeFileServlet.getName() + "> failed to initialize");
      }

      this.registerPlatformServlets();
      String hostFile = platformProvider.getHostFileName();
      niagaraUserHomeFileServlet.addUriMapping("/etc/hosts", hostFile);
      FileStore.initializeProvider(platformProvider);
      FileStore.getTempDirPath();
   }

   public static boolean serverPortAvailable(int port) {
      try (Socket localHost = new Socket("127.0.0.1", port)) {
         localHost.setReuseAddress(true);
         return false;
      } catch (Exception var64) {
         try (Socket localHost = new Socket("localhost", port)) {
            localHost.setReuseAddress(true);
            return false;
         } catch (Exception var62) {
            try (Socket publicHost = new Socket(IPAddressUtil.getLocalHost(), port)) {
               publicHost.setReuseAddress(true);
               return false;
            } catch (Exception var60) {
               return true;
            }
         }
      }
   }

   public void updateHttpsPort(int port) {
      props.setProperty("sslPort", String.valueOf(port));
   }

   public void updateHttpPort(int port) {
      props.setProperty("port", String.valueOf(port));
   }

   public void updateAuthenticator() {
      this.webServer.updateAuthenticator(getFilter(), props, platformProvider);
      this.auth = this.webServer.getAuthenticator();
   }

   public AppRegistry getStationRegistry() {
      return this.stationRegistry;
   }

   public JarSignatureRegistry getSignatureRegistry() {
      return this.signatureRegistry;
   }

   public void stopApps() {
      if (this.stationRegistry != null) {
         this.stationRegistry.stop();
      }
   }

   public int queueRestartWeb() {
      NiagaraDaemon.QueueRestartWebserverThread thread = new NiagaraDaemon.QueueRestartWebserverThread(this);
      thread.start();
      if (getFilter().isLoggable(Level.FINE)) {
         getFilter().fine("queueRestartWeb thread started [tid = " + thread.getId() + "]");
      }

      return 1;
   }

   public boolean start() {
      getFilter().info("starting, niagara_user_home=" + NIAGARA_USER_HOME);
      OutputBufferList.getInstance().start();
      readDaemonUsrProps(true);
      Authenticator newAuth = Authenticator.make(getFilter(), props, null, null, platformProvider);
      if (newAuth == null) {
         getFilter().severe("failed to create to daemon authenticator, can not start");
         return false;
      }

      if (!this.webServer.start(newAuth)) {
         return false;
      }

      this.auth = newAuth;
      if (this.stationRegistry != null) {
         this.stationRegistry.start(props);
      }

      Signal.handle(new Signal("INT"), new NiagaraDaemon.ShutdownSignalHandler());
      if (PlatformUtil.isTridiumPlatform() && OperatingSystemEnum.isOS(OperatingSystemEnum.qnx)) {
         Signal.handle(new Signal("USR2"), new NiagaraDaemon.SigUsr2SignalHandler());
      }

      externalMonitorThread = NiagaraDaemon.ExternalMonitorThread.createExternalMonitorThread();
      if (saveLogFileAfterInitialization) {
         saveDeclaredLogSettings();
      }

      return true;
   }

   public int queueStop() {
      return this.queueStop(true);
   }

   public int queueStop(boolean exitOnStop) {
      NiagaraDaemon.QueueStopThread thread = new NiagaraDaemon.QueueStopThread(exitOnStop);
      thread.start();
      if (getFilter().isLoggable(Level.FINE)) {
         getFilter().fine("queueStop thread started [tid = " + thread.getId() + "]");
      }

      return 1;
   }

   private int stop() {
      return this.stop(false);
   }

   private int stop(boolean force) {
      if (serviceStopped) {
         return 1;
      }

      getFilter().info("stopping");
      this.setDaemonStatus(3);
      if (!force && this.stationRegistry != null) {
         this.stationRegistry.stop();
      }

      this.webServer.stop();
      OutputBufferList.getInstance().stop();
      if (externalMonitorThread != null) {
         externalMonitorThread.stopThread();
      }

      serviceStopped = true;
      return 1;
   }

   public int queueReboot() {
      return this.queueReboot(false);
   }

   public int queueReboot(boolean force) {
      if (!platformProvider.isEmbedded()) {
         getFilter().warning("ignoring queue reboot request, platform does not support reboot");
         return -1;
      }

      NiagaraDaemon.QueueRebootThread thread = new NiagaraDaemon.QueueRebootThread(force);
      thread.start();
      if (getFilter().isLoggable(Level.FINE)) {
         getFilter().fine("queueReboot thread started [tid = " + thread.getId() + "]");
      }

      return 1;
   }

   private int reboot(boolean force) {
      if (!platformProvider.isEmbedded()) {
         getFilter().warning("ignoring reboot request, platform does not support reboot");
         return -1;
      } else {
         this.stop(force);
         platformProvider.reboot();
         return -1;
      }
   }

   public int failureReboot() {
      StringBuilder failureRebootList = new StringBuilder();
      String value = null;

      int limit;
      try {
         value = props.getProperty("failureRebootLimit", "3");
         limit = Integer.parseInt(value);
         if (limit < 0) {
            throw new NumberFormatException();
         }
      } catch (NumberFormatException nfe) {
         getFilter().warning("invalid failureRebootLimit value '" + value + "' specified, using default");
         limit = 3;
         props.setProperty("failureRebootLimit", "3");
      }

      long limitPeriod;
      try {
         value = props.getProperty("failureRebootLimitPeriod", "600000");
         limitPeriod = Long.parseLong(value);
         if (limitPeriod < 0L) {
            throw new NumberFormatException();
         }
      } catch (NumberFormatException nfe) {
         getFilter().warning("invalid failureRebootLimitPeriod value '" + value + "' specified, using default");
         limitPeriod = 600000L;
         props.setProperty("failureRebootLimitPeriod", "600000");
      }

      long now = System.currentTimeMillis();
      long thresh = now - limitPeriod;
      String oldFailureRebootList = props.getProperty("failureReboot", null);
      long count = 0L;
      if (oldFailureRebootList != null) {
         String[] reboots = TextUtil.split(oldFailureRebootList, ',');

         for (String reboot : reboots) {
            try {
               long timestamp = Long.parseLong(reboot);
               if (timestamp >= thresh) {
                  count++;
                  failureRebootList.append(timestamp).append(',');
               }
            } catch (NumberFormatException nfe) {
               getFilter().warning("bad reboot limit timestamp '" + this.timeStamp + "' found in properties file, ignoring");
            }
         }
      }

      failureRebootList.append(now);
      props.setProperty("failureReboot", failureRebootList.toString());
      saveProperties();
      if (count >= limit) {
         getFilter().warning("reboot limit reached");
         return 0;
      } else {
         return this.queueReboot(true);
      }
   }

   public static Logger getFilter() {
      return LOGGER;
   }

   public static long getMillisAtBootstrap() {
      return millisAtBootstrap;
   }

   public static long getMillisAtStart() {
      return millisAtServiceStart;
   }

   public static long getJarSignatureBuildMillis() {
      return JarSignatureRegistry.getJarSignatureBuildMillis();
   }

   public static boolean getDebugSupported() {
      return debugSupported;
   }

   private static String statusValueToString(int status) {
      String statusString;
      switch (status) {
         case 0:
            statusString = "idle";
            break;
         case 1:
            statusString = "starting";
            break;
         case 2:
            statusString = "running";
            break;
         case 3:
            statusString = "stopping";
            break;
         case 4:
            statusString = "failed";
            break;
         case 5:
         default:
            statusString = "unknown";
            break;
         case 6:
            statusString = "halted";
      }

      return statusString;
   }

   private void setDaemonStatus(int newStatus) {
      if (this.status != newStatus) {
         if (getFilter().isLoggable(Level.FINEST)) {
            getFilter().finest("notify application status change for niagarad: " + statusValueToString(this.status) + " -> " + statusValueToString(newStatus));
         }

         int oldStatus = this.status;
         this.status = newStatus;
         if (!platformProvider.notifyApplicationStatus("niagarad", "niagarad", this.status)) {
            getFilter()
               .warning("failed to notify application status change for niagarad: " + statusValueToString(oldStatus) + " -> " + statusValueToString(newStatus));
         }
      }
   }

   private int getDaemonStatus() {
      return this.status;
   }

   private static void readDaemonUsrProps(boolean startup) {
      if (PlatformUtil.isTridiumPlatform() && OperatingSystemEnum.isOS(OperatingSystemEnum.qnx)) {
         File tempDaemonProps = new File("/tmp/daemon_usr1.properties");
         if (tempDaemonProps.exists()) {
            reloadProperties("/tmp/daemon_usr1.properties");
            if (!startup) {
               NiagaraDaemon.QueueRestartWebserverThread thread = new NiagaraDaemon.QueueRestartWebserverThread(getInstance());
               thread.run();
            }
         }
      }
   }

   public int queueRefreshSoftware() {
      if (!platformProvider.allowPlatformDaemonRestart()) {
         return -1;
      } else if (_CONSOLE) {
         getFilter().severe("cannot reload platform daemon binaries when it is running from the console, binaries must be moved manually");
         return -1;
      } else {
         return platformProvider.restartPlatformDaemon();
      }
   }

   public static void externalMonitorEntry(String message) {
      if (message != null) {
         NiagaraDaemon daemonInstance = getInstance();
         if (getFilter().isLoggable(Level.FINE)) {
            getFilter().fine("external event \"" + message + "\" encountered");
         }

         if (!message.equalsIgnoreCase("backup")) {
            if (message.equalsIgnoreCase("shutdown")) {
               getFilter().info("shutdown button pressed, stopping niagarad");
               daemonInstance.queueStop(false);
               double secondsToWait = 300.0;

               while (!serviceStopped && secondsToWait > 0.0) {
                  secondsToWait--;

                  try {
                     Thread.sleep(1000L);
                  } catch (Exception var8) {
                  }
               }

               if (secondsToWait <= 0.0) {
                  getFilter().severe("daemon failed to stop in a timely fashion");
                  if (!MqUtil.sendMessage("/niagarad-to-os", "fail")) {
                     getFilter().severe("failed to send io \"fail\" message, can not notify shutdown service");
                  }

                  return;
               }

               getFilter().info("niagarad daemon stopped, notifying shutdown service");
               if (!MqUtil.sendMessage("/niagarad-to-os", "success")) {
                  getFilter().severe("failed to send io \"success\" message, can not notify shutdown service");
                  return;
               }

               System.exit(0);
            } else if (message.startsWith("network_interface_event")) {
               if (message.length() > "network_interface_event".length()) {
                  String event = message.substring("network_interface_event".length()).trim();
                  getFilter().info("network interface event \"" + event + "\" encountered, refreshing local interfaces...");
               } else {
                  getFilter().info("network interface event encountered, refreshing local interfaces...");
               }

               IPAddressUtil.clearLocalHostCache();
               IPAddressUtil.getLocalHost();
               IPAddressUtil.getLocalHost(null);
               if (daemonInstance.webServer != null && daemonInstance.webServer.getState() == 1) {
                  daemonInstance.webServer.refreshAccessHandlerLocalAdapters();
               }

               getFilter().info("network interface refresh complete");
            } else {
               getFilter().info("ignoring unhandled external event \"" + message + "\"");
            }
         } else {
            getFilter().info("backup button pressed, locking daemon and saving stations");

            for (boolean lockObtained = daemonInstance.lockClient(); !lockObtained; lockObtained = daemonInstance.lockClient()) {
               if (daemonInstance.permanentlyLocked) {
                  getFilter().severe("client sessions are permanently locked, can not continue");
                  if (!MqUtil.sendMessage("/niagarad-to-os", "fail")) {
                     getFilter().severe("failed to send io \"fail\" message, can not notify backup service");
                  }

                  return;
               }

               getFilter().warning("failed to obtain client session lock, retrying");

               try {
                  Thread.sleep(3000L);
               } catch (InterruptedException var9) {
               }
            }

            try {
               if (daemonInstance.getStationRegistry().appRunning()) {
                  List<App> applications = daemonInstance.getStationRegistry().getApps();
                  applications.stream()
                     .filter(App::isActive)
                     .forEach(
                        current -> {
                           if (current.isAcceptingMessages()) {
                              int secondsToWait = 300;

                              while (current.getStatus() == 1 && secondsToWait > 0) {
                                 secondsToWait--;

                                 try {
                                    Thread.sleep(1000L);
                                 } catch (Exception var7) {
                                 }
                              }

                              if (secondsToWait <= 0) {
                                 getFilter().severe("station " + current.getAppName() + " failed to start in a timely fashion");
                                 if (!MqUtil.sendMessage("/niagarad-to-os", "fail")) {
                                    getFilter().severe("failed to send io \"fail\" message, can not notify backup service");
                                 }

                                 return;
                              }

                              String requestId = "baja:StationSaveJob@" + current.getAppName();
                              String initialState = RequestState.getInstance().getRequestState(requestId);
                              current.sendMessage("save");
                              String newState = RequestState.getInstance().getRequestState(requestId);

                              for (secondsToWait = 300;
                                 initialState.equalsIgnoreCase(newState) && secondsToWait > 0;
                                 newState = RequestState.getInstance().getRequestState(requestId)
                              ) {
                                 secondsToWait--;

                                 try {
                                    Thread.sleep(1000L);
                                 } catch (Exception var6) {
                                 }
                              }

                              if (secondsToWait <= 0) {
                                 getFilter().severe("station " + current.getAppName() + " failed to save in a timely fashion");
                                 if (!MqUtil.sendMessage("/niagarad-to-os", "fail")) {
                                    getFilter().severe("failed to send io \"fail\" message, can not notify backup service");
                                 }
                              } else if (newState.contains("success")) {
                                 getFilter().info("station " + current.getAppName() + " save successful");
                              } else if (newState.contains("fail")) {
                                 getFilter().severe("station " + current.getAppName() + " save failed");
                                 if (!MqUtil.sendMessage("/niagarad-to-os", "fail")) {
                                    getFilter().severe("failed to send io \"fail\" message, can not notify backup service");
                                 }
                              }
                           } else {
                              getFilter().severe("station " + current.getAppName() + " not currently accepting messages, can not save");
                              if (!MqUtil.sendMessage("/niagarad-to-os", "fail")) {
                                 getFilter().severe("failed to send io \"fail\" message, can not notify backup service");
                              }
                           }
                        }
                     );
                  getFilter().info("station save sequence complete, notifying backup service to proceed");
               } else {
                  getFilter().info("no stations running, notifying backup service to proceed");
               }

               if (MqUtil.sendMessage("/niagarad-to-os", "success")) {
                  getFilter().info("waiting for backup service completion");
                  StringBuilder messageBuffer = new StringBuilder();
                  if (MqUtil.receiveMessage("/os-to-niagarad", messageBuffer)) {
                     message = messageBuffer.toString();
                     if (message.equals("fail")) {
                        getFilter().warning("received io message \"" + messageBuffer + "\", backup failed, unlocking daemon");
                     } else {
                        getFilter().info("received io message \"" + messageBuffer + "\", backup ok, unlocking daemon");
                     }

                     return;
                  } else {
                     getFilter().severe("error receiving message from backup service, unlocking daemon");
                     return;
                  }
               }

               getFilter().severe("failed to send io \"success\" message, can not notify backup service");
            } finally {
               daemonInstance.unlockClient();
            }

            return;
         }
      }
   }

   public boolean lockClient() {
      return this.lockClient(-1L);
   }

   public boolean lockClient(long timeout) {
      if (this.permanentlyLocked) {
         return true;
      }

      synchronized (this.clientMonitor) {
         Thread currentThread = Thread.currentThread();
         if (this.clientCount == 0 && this.owner != null && this.owner != currentThread) {
            if (timeout < 0L) {
               return false;
            }

            try {
               this.clientMonitor.wait(timeout);
            } catch (InterruptedException var7) {
            }

            if (this.clientCount == 0 && this.owner != null && this.owner != currentThread) {
               return false;
            }
         }

         if (this.owner != currentThread) {
            this.clientCount--;
            this.owner = Thread.currentThread();
         }

         return true;
      }
   }

   public void unlockClient() {
      if (!this.permanentlyLocked) {
         synchronized (this.clientMonitor) {
            if (this.clientCount == 0 && this.owner == Thread.currentThread()) {
               this.clientCount++;
               this.owner = null;
            }

            this.clientMonitor.notify();
         }
      }
   }

   public boolean lockClientPermanent() {
      if (this.permanentlyLocked) {
         return false;
      }

      boolean locked = this.lockClient();
      if (locked) {
         this.permanentlyLocked = true;
      }

      return locked;
   }

   public static ISecurityInfoProvider getSecurityInfoProvider() {
      NiagaraBasicPermission securityInfoPermission = new NiagaraBasicPermission("GET_SECURITY_INFO_PROVIDER");
      SecurityManager sm = System.getSecurityManager();
      if (sm != null) {
         sm.checkPermission(securityInfoPermission);
      }

      return secInfProvider;
   }

   private static void checkSecurityManagerDisable() {
      String disable = AccessController.doPrivileged(() -> System.getProperty("niagara.security.manager.disable"));
      if (disable != null) {
         if (LicenseManager.getInstance(NullLogger.getInstance()).checkFeature("tridium", "smDeveloperMode") != null) {
            String logFileName = DeveloperSecurityManager.enableDeveloperSecurityManager(PolicyType.DAEMON);
            getFilter().severe("*******************************************************************************");
            getFilter().severe("Security Manager developer mode enabled. It is recommended to switch off this mode as soon as possible.");
            getFilter().severe("Security Manager Exceptions are being written to " + logFileName);
            getFilter().severe("*******************************************************************************");
         } else {
            getFilter().warning("A request was made to run in Security Manager developer mode, but the system is not licensed for it.");
         }
      }
   }

   static {
      TypeSupplier<ISecurityInitializer> secIntSupplier = new SingletonSupplier(
         ISecurityInitializer.class, SecurityInitializer.class, new DefaultSecurityInitializerConfig()
      );
      instantiator.addSupplier(secIntSupplier);
   }

   private abstract static class ExternalMonitorThread extends Thread {
      protected boolean threadStopRequested = false;

      public static NiagaraDaemon.ExternalMonitorThread createExternalMonitorThread() {
         NiagaraDaemon.ExternalMonitorThread thread = null;
         if (PlatformUtil.isNpsdkPlatform()) {
            thread = new NiagaraDaemon.ExternalMonitorThreadNpsdk();
         } else if (PlatformUtil.isTridiumPlatform()) {
            if (!OperatingSystemEnum.isOS(OperatingSystemEnum.qnx)) {
               return null;
            }

            thread = new NiagaraDaemon.ExternalMonitorThreadTridium();
         }

         if (thread != null) {
            thread.start();
         }

         return thread;
      }

      protected ExternalMonitorThread() {
         super("Niagarad:ExternalMonitor");
         this.setDaemon(true);
      }

      public void stopThread() {
         this.threadStopRequested = true;
      }
   }

   private static class ExternalMonitorThreadNpsdk extends NiagaraDaemon.ExternalMonitorThread {
      private static final boolean CHECK_NETWORK_INTERFACE_EVENT_PROPERTY_VALUE = AccessController.doPrivileged(
         () -> Boolean.getBoolean("niagara.enable.network.interface.events")
      );
      private static final int NETWORK_INTERFACE_CHECK_TICKS = 10000;
      private long lastNetworkInterfaceCheckTicks = NiagaraDaemon.platformProvider.getTickCount();

      private ExternalMonitorThreadNpsdk() {
      }

      @Override
      public void run() {
         boolean externalStopRequested = NiagaraDaemon.platformProvider.platformDaemonShutdownRequested();
         if (CHECK_NETWORK_INTERFACE_EVENT_PROPERTY_VALUE) {
            NiagaraDaemon.getFilter()
               .fine(
                  "Monitoring external network events at "
                     + NiagaraDaemon.ExternalMonitorThreadNpsdk.NetworkInterfaceEventFileHolder.NETWORK_INTERFACE_EVENT_FILE
               );
         }

         while (!externalStopRequested && !this.threadStopRequested) {
            try {
               Thread.sleep(1000L);
            } catch (InterruptedException var7) {
            }

            externalStopRequested = NiagaraDaemon.platformProvider.platformDaemonShutdownRequested();
            if (!externalStopRequested && CHECK_NETWORK_INTERFACE_EVENT_PROPERTY_VALUE) {
               long currentTicks = NiagaraDaemon.platformProvider.getTickCount();
               if (Math.abs(currentTicks - this.lastNetworkInterfaceCheckTicks) > 10000L) {
                  this.lastNetworkInterfaceCheckTicks = currentTicks;
                  long tempNetworkInterfaceEventLastModified = NiagaraDaemon.ExternalMonitorThreadNpsdk.NetworkInterfaceEventFileHolder.NETWORK_INTERFACE_EVENT_FILE
                     .lastModified();
                  if (tempNetworkInterfaceEventLastModified
                     != NiagaraDaemon.ExternalMonitorThreadNpsdk.NetworkInterfaceEventFileHolder.networkInterfaceEventLastModified) {
                     if (NiagaraDaemon.ExternalMonitorThreadNpsdk.NetworkInterfaceEventFileHolder.networkInterfaceEventLastModified == 0L
                        || tempNetworkInterfaceEventLastModified != 0L) {
                        NiagaraDaemon.getFilter().info("network interface event encountered, refreshing local host network interfaces...");
                        IPAddressUtil.clearLocalHostCache();
                        IPAddressUtil.getLocalHost();
                        IPAddressUtil.getLocalHost(null);
                        NiagaraDaemon daemonInstance = NiagaraDaemon.getInstance();
                        if (daemonInstance.webServer != null && daemonInstance.webServer.getState() == 1) {
                           daemonInstance.webServer.refreshAccessHandlerLocalAdapters();
                        }

                        NiagaraDaemon.getFilter().info("network interface refresh complete");
                     }

                     NiagaraDaemon.ExternalMonitorThreadNpsdk.NetworkInterfaceEventFileHolder.networkInterfaceEventLastModified = tempNetworkInterfaceEventLastModified;
                  }
               }
            }
         }

         if (externalStopRequested) {
            NiagaraDaemon.getFilter().info("received external stop signal, stopping niagarad");
            NiagaraDaemon.getInstance().queueStop(true);
         }
      }

      private static class NetworkInterfaceEventFileHolder {
         private static final File NETWORK_INTERFACE_EVENT_FILE = AccessController.doPrivileged(
            () -> new File(NiagaraDaemon.platformProvider.getTempDirPath() + File.separatorChar + "niagara_network_interface_event")
         );
         private static long networkInterfaceEventLastModified = NETWORK_INTERFACE_EVENT_FILE.lastModified();
      }
   }

   private static class ExternalMonitorThreadTridium extends NiagaraDaemon.ExternalMonitorThread {
      private ExternalMonitorThreadTridium() {
      }

      @Override
      public void run() {
         if (!MqUtil.queueExists("/os-to-niagarad")) {
            NiagaraDaemon.getFilter().severe("/os-to-niagarad message queue missing, skipping external monitor thread");
         } else if (!MqUtil.queueExists("/niagarad-to-os")) {
            NiagaraDaemon.getFilter().severe("/niagarad-to-os message queue missing, skipping external monitor thread");
         } else {
            while (!this.threadStopRequested) {
               StringBuilder messageBuffer = new StringBuilder();
               if (MqUtil.receiveMessage("/os-to-niagarad", messageBuffer)) {
                  NiagaraDaemon.externalMonitorEntry(messageBuffer.toString());
               } else if (!NiagaraDaemon.serviceStopped && !this.threadStopRequested) {
                  NiagaraDaemon.getFilter().severe("failed to receive external notification");
               }

               try {
                  Thread.sleep(3000L);
               } catch (InterruptedException var3) {
               }
            }
         }
      }
   }

   private class QueueRebootThread extends Thread {
      boolean force;

      QueueRebootThread(boolean force) {
         super("Niagarad:QueueReboot");
         this.force = force;
      }

      @Override
      public void run() {
         try {
            Thread.sleep(3000L);
         } catch (InterruptedException var2) {
         }

         NiagaraDaemon.this.reboot(this.force);
      }
   }

   private static class QueueRestartWebserverThread extends Thread {
      NiagaraDaemon nd;

      QueueRestartWebserverThread(NiagaraDaemon daemon) {
         super("Niagarad:QueueRestartWebserver");
         this.nd = daemon;
      }

      @Override
      public void run() {
         try {
            Thread.sleep(1000L);
         } catch (InterruptedException var2) {
         }

         Authenticator newAuth = Authenticator.make(
            NiagaraDaemon.getFilter(),
            NiagaraDaemon.props,
            this.nd.webServer.getAuthenticator().getAuthDomain().getExtraUsers(),
            this.nd.webServer.getAuthenticator().getAuthDomain().getExtraAdminUsers(),
            NiagaraDaemon.platformProvider
         );
         if (newAuth == null) {
            NiagaraDaemon.getFilter().severe("failed to create to daemon authenticator, can not restart web server");
         } else {
            this.nd.webServer.restart(newAuth, NiagaraDaemon.props);
            this.nd.auth = newAuth;
         }
      }
   }

   private class QueueStopThread extends Thread {
      boolean exitOnStop;

      QueueStopThread(boolean exitOnStop) {
         super("Niagarad:QueueStop");
         this.exitOnStop = exitOnStop;
      }

      @Override
      public void run() {
         try {
            Thread.sleep(3000L);
         } catch (InterruptedException var2) {
         }

         NiagaraDaemon.this.stop();
         if (this.exitOnStop) {
            System.exit(0);
         }
      }
   }

   private static class ShutdownSignalHandler implements SignalHandler {
      boolean stopSignaled = false;

      private ShutdownSignalHandler() {
      }

      @Override
      public void handle(Signal sig) {
         if (!this.stopSignaled) {
            this.stopSignaled = true;
            NiagaraDaemon.getFilter().info("received termination signal " + sig + ", shutting down");
            NiagaraDaemon.getInstance().stop(false);
            System.exit(0);
         }
      }
   }

   private static class SigUsr2SignalHandler implements SignalHandler {
      private SigUsr2SignalHandler() {
      }

      @Override
      public void handle(Signal sig) {
         NiagaraDaemon.getFilter().fine("<< SIGUSR2 >> encountered, reading daemon usr file");
         NiagaraDaemon.readDaemonUsrProps(false);
      }
   }
}
