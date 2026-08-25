package com.tridium.niagarad.app;

import com.tridium.niagarad.NiagaraDaemon;
import com.tridium.niagarad.http.Authenticator;
import com.tridium.niagarad.http.ScramAuthenticator;
import com.tridium.niagarad.io.PipedOutputBuffer;
import com.tridium.niagarad.license.Feature;
import com.tridium.niagarad.license.LicenseManager;
import com.tridium.niagarad.log.NullLogger;
import com.tridium.niagarad.platform.PlatformInfo;
import com.tridium.niagarad.security.AuthenticationDomain;
import com.tridium.niagarad.servlet.NreConfigurationServlet;
import com.tridium.nre.platform.IPlatformProvider;
import com.tridium.nre.platform.OperatingSystemEnum;
import com.tridium.nre.platform.PlatformUtil;
import com.tridium.nre.security.Aes256PasswordManager;
import com.tridium.nre.security.SecretChars;
import com.tridium.nre.subscription.SubscriptionLicenseUtil;
import com.tridium.nre.util.IPAddressUtil;
import com.tridium.nre.util.LicenseMode;
import com.tridium.nre.util.NiagaraFiles;
import com.tridium.nre.util.Version;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Logger;
import javax.baja.nre.util.ByteArrayUtil;
import javax.baja.nre.util.SecurityUtil;
import javax.baja.nre.util.TextUtil;
import javax.baja.xml.XElem;
import javax.baja.xml.XParser;
import javax.servlet.Servlet;

public final class StationApp extends App {
   private final IPlatformProvider platformProvider;
   static final Object _NRE_PROPERTIES_MONITOR = new Object();
   private static final String NO_DEFAULT_CREDENTIALS_CHECK_COOKIE_PATH = NiagaraDaemon.NIAGARA_USER_HOME_DAEMON_PATH
      + File.separator
      + "no-default-credentials-check";
   private static final String NO_DEFAULT_SYSPW_CHECK_COOKIE_PATH = NiagaraDaemon.NIAGARA_USER_HOME_DAEMON_PATH + File.separator + "no-default-syspw-check";
   private static final String LEGACY_NO_DEFAULT_CREDENTIALS_CHECK_COOKIE_PATH = NiagaraDaemon.NIAGARA_HOME
      + File.separatorChar
      + "etc"
      + File.separatorChar
      + "no-default-credentials-check";
   private static final String LEGACY_NO_DEFAULT_SYSPW_CHECK_COOKIE_PATH = NiagaraDaemon.NIAGARA_HOME
      + File.separatorChar
      + "etc"
      + File.separatorChar
      + "no-default-syspw-check";

   public StationApp(String name, AppRegistry registry, Logger out, IPlatformProvider platformProvider) {
      super(name, registry, out, platformProvider);
      this.platformProvider = platformProvider;
      if (this.getAppStatus() != 4) {
         if (registry.hasAppStarted() && !this.allowRestart()) {
            this.setAppStatus(6);
         } else {
            this.setAppStatus(0);
         }
      }
   }

   @Override
   public String getAppType() {
      return "station";
   }

   @Override
   public void sendMessages(String[] messages) {
      if (this.isAcceptingMessages()) {
         StringBuilder messageBuilder = new StringBuilder();

         for (int i = 0; i < messages.length; i++) {
            messageBuilder.append(messages[i]);
            if (i < messages.length - 1) {
               messageBuilder.append(" ");
            }
         }

         messageBuilder.append("\n");
         if (this.writeStdInput(messageBuilder.toString()) == -1) {
            this.filter.severe("error sending message to station " + this.appName);
         }
      } else {
         this.filter.severe("attempted to send a message to station " + this.appName + ", which is not accepting messages");
      }
   }

   @Override
   public boolean isAcceptingMessages() {
      return this.hStdInput != null;
   }

   @Override
   protected boolean canStart() {
      boolean commonRequirements = super.canStart();
      return this.registry == null ? commonRequirements : this.registry.canStartApp() && commonRequirements;
   }

   private boolean isValidJreLicenseIfSupervisorHost(String hostId) {
      return true;
   }

   private boolean isValidJre8License(String javaVmVendor) {
      String[] jreFeatures;
      if (javaVmVendor.equals("Oracle Corporation")) {
         jreFeatures = new String[]{"jre8qnx"};
      } else {
         if (!javaVmVendor.equals("Azul Systems, Inc.")) {
            StringBuilder buffer = new StringBuilder();
            buffer.append("ERROR: Feature ")
               .append(javaVmVendor)
               .append(" JRE not licensed (unknown vendor), station ")
               .append(this.appName)
               .append(" cannot run\n");
            this.appOut.printf(buffer.toString().getBytes(StandardCharsets.UTF_8));
            buffer = new StringBuilder();
            buffer.append("error starting station ")
               .append(this.appName)
               .append(", feature ")
               .append(javaVmVendor)
               .append(" JRE not licensed (unknown vendor)");
            return false;
         }

         jreFeatures = new String[]{"jre8Qnx7Zulu", "jre8J8000Azul"};
      }

      if (!this.checkLicenseFeatures(jreFeatures)) {
         return false;
      } else if (!AppRegistry.LocalMetaDataHolder.JAVA_RUNTIME_VERSION.startsWith("1.8")) {
         StringBuilder buffer = new StringBuilder();
         buffer.append("ERROR: Licensed VM version or vendor does not match actual VM version/vendor ")
            .append(javaVmVendor)
            .append(" ")
            .append(AppRegistry.LocalMetaDataHolder.JAVA_RUNTIME_VERSION)
            .append(", station ")
            .append(this.appName)
            .append(" cannot run\n");
         this.appOut.printf(buffer.toString().getBytes(StandardCharsets.UTF_8));
         buffer = new StringBuilder();
         buffer.append("error starting station ")
            .append(this.appName)
            .append("Licensed VM version or vendor does not match actual VM version/vendor ")
            .append(javaVmVendor)
            .append(" ")
            .append(AppRegistry.LocalMetaDataHolder.JAVA_RUNTIME_VERSION);
         this.filter.severe(buffer.toString());
         return false;
      } else {
         return true;
      }
   }

   private boolean isValidQnx7License() {
      return this.checkLicenseFeatures(new String[]{"jre8Qnx7Zulu", "qnx7"});
   }

   private boolean checkLicenseFeatures(String[] features) {
      StringBuilder featureListBuilder = new StringBuilder();
      String prefix = "";

      for (String featureName : features) {
         Feature feature = LicenseManager.getInstance(NullLogger.getInstance()).getFeature("tridium", featureName);
         if (feature != null && !feature.isExpired()) {
            return true;
         }

         featureListBuilder.append(prefix);
         prefix = ", ";
         featureListBuilder.append(featureName);
         if (feature != null) {
            featureListBuilder.append(" (expired)");
         }
      }

      String featureListString = featureListBuilder.toString();
      String appOutMessage;
      String filterMessage;
      if (features.length > 1) {
         appOutMessage = "ERROR: Station " + this.appName + " cannot run without one of the following license features: " + featureListString + '\n';
         filterMessage = "error starting station " + this.appName + ". Station cannot run without one of the following license features: " + featureListString;
      } else {
         appOutMessage = "ERROR: Feature " + featureListString + " not licensed, station " + this.appName + "cannot run\n";
         filterMessage = "error starting station " + this.appName + ", feature " + featureListString + " not licensed";
      }

      this.appOut.printf(appOutMessage.getBytes(StandardCharsets.UTF_8));
      this.filter.severe(filterMessage);
      return false;
   }

   private static boolean checkDefaultCredentialsConditions(
      IPlatformProvider platformProvider, AuthenticationDomain authenticationDomain, PipedOutputBuffer appOut, Logger filter
   ) {
      if (platformProvider != null && authenticationDomain != null && appOut != null && filter != null) {
         String disableCheckPath;
         if (PlatformUtil.isTridiumPlatform() && OperatingSystemEnum.isOS(OperatingSystemEnum.qnx)) {
            disableCheckPath = LEGACY_NO_DEFAULT_CREDENTIALS_CHECK_COOKIE_PATH;
         } else {
            disableCheckPath = NO_DEFAULT_CREDENTIALS_CHECK_COOKIE_PATH;
         }

         File disableCheckFile = new File(disableCheckPath);
         boolean defaultAccountPresent = authenticationDomain.makeAuthInfo(platformProvider.getDefaultUsername(), platformProvider.getDefaultPassword())
            != null;
         if (!disableCheckFile.exists()) {
            if (defaultAccountPresent) {
               String message = "\n******************************************************************\n*                                                                *\n*  FACTORY DEFAULT PLATFORM CREDENTIALS DETECTED, CANNOT START!  *\n*                                                                *\n******************************************************************\n\n";
               appOut.printf(message.getBytes(StandardCharsets.UTF_8));
               return false;
            }
         } else if (!defaultAccountPresent && !disableCheckFile.delete()) {
            filter.warning("failed to delete appliance platform credentials marker, please manually delete marker");
            disableCheckFile.deleteOnExit();
         }

         return true;
      } else {
         return false;
      }
   }

   private static boolean checkDefaultSystemPassphraseConditions(IPlatformProvider platformProvider, PipedOutputBuffer appOut, Logger filter) {
      if (platformProvider != null && appOut != null && filter != null) {
         String disableCheckPath;
         if (PlatformUtil.isTridiumPlatform() && OperatingSystemEnum.isOS(OperatingSystemEnum.qnx)) {
            disableCheckPath = LEGACY_NO_DEFAULT_SYSPW_CHECK_COOKIE_PATH;
         } else {
            disableCheckPath = NO_DEFAULT_SYSPW_CHECK_COOKIE_PATH;
         }

         File disableCheckFile = new File(disableCheckPath);
         SecretChars currentChars = platformProvider.getSystemPassword();
         Throwable var7 = null;

         boolean defaultPassphrasePresent;
         try {
            char[] defaultChars = platformProvider.getDefaultPassword().toCharArray();
            defaultPassphrasePresent = SecurityUtil.equals(defaultChars, currentChars.get());
         } catch (Throwable var16) {
            var7 = var16;
            throw var16;
         } finally {
            if (currentChars != null) {
               if (var7 != null) {
                  try {
                     currentChars.close();
                  } catch (Throwable var15) {
                     var7.addSuppressed(var15);
                  }
               } else {
                  currentChars.close();
               }
            }
         }

         if (!disableCheckFile.exists()) {
            if (defaultPassphrasePresent) {
               String message = "\n*****************************************************************\n*                                                               *\n*   FACTORY DEFAULT SYSTEM PASSPHRASE DETECTED, CANNOT START!   *\n*                                                               *\n*****************************************************************\n\n";
               appOut.printf(message.getBytes(StandardCharsets.UTF_8));
               return false;
            }
         } else if (!defaultPassphrasePresent && !disableCheckFile.delete()) {
            filter.warning("failed to delete appliance system passphrase marker, please manually delete marker");
            disableCheckFile.deleteOnExit();
         }

         return true;
      } else {
         return false;
      }
   }

   private static boolean isNreCoreVersionMismatched(String nreCoreVersionString, String bajaVersionString) {
      if (nreCoreVersionString == null) {
         return false;
      }

      if (bajaVersionString == null) {
         return false;
      }

      int majorMinorUpdateBuild = 4;
      String nreCoreBuildVersionString = new Version(nreCoreVersionString).toString(majorMinorUpdateBuild);
      String bajaBuildVersionString = new Version(bajaVersionString).toString(majorMinorUpdateBuild);
      return !nreCoreBuildVersionString.equals(bajaBuildVersionString);
   }

   @Override
   protected boolean prepareForLaunch() {
      this.appOut.clear();
      if (PlatformUtil.isTridiumPlatform() && OperatingSystemEnum.isOS(OperatingSystemEnum.qnx)) {
         if (SubscriptionLicenseUtil.getLicenseMode() == LicenseMode.SUBSCRIPTION) {
            if (!SubscriptionLicenseUtil.getHostIdStatus().equals("ok")) {
               String message = "Platform is in subscription mode, but license is not yet registered.";
               this.appOut.printf(message.getBytes(StandardCharsets.UTF_8));
               this.filter.info(message);
               return true;
            }

            LicenseManager.getInstance(NullLogger.getInstance()).reload(NiagaraDaemon.getFilter());
         }

         boolean jreLicensed = this.isValidJre8License(AppRegistry.LocalMetaDataHolder.JAVA_VM_VENDOR);
         boolean qnxLicensed = this.isValidQnx7License();
         if (!jreLicensed || !qnxLicensed) {
            return false;
         }
      }

      IPlatformProvider platformProvider = PlatformUtil.getPlatformProvider();
      if (!this.isValidJreLicenseIfSupervisorHost(platformProvider.getHostId())) {
         return false;
      }

      if (!platformProvider.isEmbedded()) {
         Feature capacityFeature = LicenseManager.getInstance(NullLogger.getInstance()).getFeature("tridium", "globalCapacity");
         if (capacityFeature != null && !capacityFeature.isExpired()) {
            String heapLimitString = capacityFeature.get("heap.limit", "-1");
            int heapLimit = 0;

            try {
               heapLimit = Integer.parseInt(heapLimitString);
            } catch (Exception var42) {
            }

            if (heapLimit > 0) {
               synchronized (_NRE_PROPERTIES_MONITOR) {
                  try {
                     File nrePropertiesFile = new File(NiagaraDaemon.NIAGARA_USER_HOME + File.separator + "etc" + File.separator + "nre.properties");
                     File parentFile = nrePropertiesFile.getParentFile();
                     if (!parentFile.exists() && !parentFile.mkdirs()) {
                        StringBuilder buffer = new StringBuilder();
                        buffer.append("WARNING: Failed to create directory '")
                           .append(parentFile.getPath())
                           .append("', heap licensing errors for station ")
                           .append(this.appName)
                           .append(" may occur.\n");
                        this.appOut.printf(buffer.toString().getBytes(StandardCharsets.UTF_8));
                        buffer = new StringBuilder();
                        buffer.append("failed to create directory '")
                           .append(parentFile.getPath())
                           .append("', heap licensing errors for station ")
                           .append(this.appName)
                           .append(" may occur.\n");
                        this.filter.severe(buffer.toString());
                     }

                     String[] fileContents;
                     if (nrePropertiesFile.exists()) {
                        ArrayList<String> nrePropertiesContents = new ArrayList<>();

                        try (BufferedReader reader = new BufferedReader(new FileReader(nrePropertiesFile))) {
                           for (String currentLine = reader.readLine(); currentLine != null; currentLine = reader.readLine()) {
                              nrePropertiesContents.add(currentLine);
                           }
                        }

                        boolean foundOptions = false;

                        for (int i = 0; i < nrePropertiesContents.size(); i++) {
                           String currentLine = nrePropertiesContents.get(i);
                           if (currentLine.startsWith("station.java.options")) {
                              foundOptions = true;
                              StringBuilder currentContentsLessXmx = new StringBuilder("station.java.options=");
                              StringTokenizer tokenizer = new StringTokenizer(currentLine.substring(currentLine.indexOf("=") + 1));

                              while (tokenizer.hasMoreTokens()) {
                                 String token = tokenizer.nextToken();
                                 if (!token.startsWith("-Xmx")) {
                                    currentContentsLessXmx.append(token).append(" ");
                                 }
                              }

                              nrePropertiesContents.set(i, currentContentsLessXmx.append("-Xmx").append(heapLimit).append("M").toString());
                           }
                        }

                        if (!foundOptions) {
                           nrePropertiesContents.add("station.java.options=-Xmx" + heapLimit + "M");
                        }

                        fileContents = nrePropertiesContents.toArray(new String[0]);
                     } else {
                        fileContents = new String[]{"station.java.options=-Xmx" + heapLimit + "M"};
                     }

                     try (BufferedWriter writer = new BufferedWriter(new FileWriter(nrePropertiesFile))) {
                        for (String line : fileContents) {
                           writer.write(line + "\n");
                        }

                        writer.flush();
                     }
                  } catch (Exception e) {
                     StringBuilder buffer = new StringBuilder();
                     buffer.append("failed to validate heap settings (").append(e).append("), station").append(this.appName).append(" may not start properly");
                     this.filter.warning(buffer.toString());
                     buffer = new StringBuilder();
                     buffer.append("WARNING: Failed to validate heap settings (")
                        .append(e)
                        .append("), station")
                        .append(this.appName)
                        .append(" may not start properly\n");
                     this.appOut.printf(buffer.toString().getBytes(StandardCharsets.UTF_8));
                  }
               }
            }
         }
      }

      return true;
   }

   @Override
   protected boolean launch() {
      this.appOut.clear();
      StringBuilder buffer = new StringBuilder();
      buffer.append("station ").append(this.appName).append(" starting");
      this.filter.info(buffer.toString());
      if (this.watchdog == null) {
         buffer = new StringBuilder();
         buffer.append("ERROR: Station watchdog unavailable, station ").append(this.appName).append(" cannot run\n");
         this.appOut.printf(buffer.toString().getBytes(StandardCharsets.UTF_8));
         buffer = new StringBuilder();
         buffer.append("error starting station ").append(this.appName).append(", watchdog is unavailable");
         this.filter.severe(buffer.toString());
         return false;
      }

      StringBuilder adminUser = new StringBuilder();
      StringBuilder adminPassword = new StringBuilder();

      try {
         makeAdminCredentials(adminUser, adminPassword);
      } catch (NoSuchAlgorithmException | IOException exception) {
         buffer = new StringBuilder();
         buffer.append("error starting station ").append(this.appName).append(": ").append(exception);
         this.filter.severe(buffer.toString());
         return false;
      }

      NiagaraDaemon niagaraDaemon = NiagaraDaemon.getInstance();
      Authenticator authenticator = niagaraDaemon.webServer.getAuthenticator();
      if (authenticator instanceof ScramAuthenticator) {
         ScramAuthenticator scramAuthenticator = (ScramAuthenticator)authenticator;
         Random r = new SecureRandom();
         byte[] salt = new byte[16];
         r.nextBytes(salt);

         try {
            authenticator.getAuthDomain()
               .addExtraUser(
                  adminUser.toString(),
                  scramAuthenticator.getPasswordHash(adminPassword.toString(), TextUtil.bytesToHexString(salt).getBytes(StandardCharsets.UTF_8))
               );
         } catch (Exception e) {
            buffer = new StringBuilder();
            buffer.append("failed to generate local session password hash, station")
               .append(this.appName)
               .append(" may not start properly (")
               .append(e)
               .append(")");
            this.filter.warning(buffer.toString());
            buffer = new StringBuilder();
            buffer.append("WARNING: Failed to generate local session password hash, station")
               .append(this.appName)
               .append(" may not start properly (")
               .append(e)
               .append(")\n");
            this.appOut.printf(buffer.toString().getBytes(StandardCharsets.UTF_8));
         }
      } else {
         authenticator.getAuthDomain().addExtraUser(adminUser.toString(), adminPassword.toString());
      }

      String localHttpAddress = IPAddressUtil.isIpv6Address(niagaraDaemon.webServer.getHttpAddress()) ? "::1" : "127.0.0.1";
      int localHttpPort = niagaraDaemon.webServer.getHttpPort();
      int localHttpsPort = niagaraDaemon.webServer.getHttpsPort();
      StringBuilder daemonSpawnArgumentStage = new StringBuilder();
      String daemonSpawnArgument = "";

      try {
         byte[] ivBytes = new byte[16];
         new SecureRandom().nextBytes(ivBytes);
         String iv = ByteArrayUtil.toHexString(ivBytes);
         Aes256PasswordManager manager = Aes256PasswordManager.getManager(NiagaraDaemon.getSecurityInfoProvider().getKeyRing());
         String unencryptedString = adminUser + "-" + adminPassword + "-" + localHttpAddress + "-" + localHttpPort + "-" + localHttpsPort;
         daemonSpawnArgumentStage.append(ByteArrayUtil.toHexString(manager.encrypt(unencryptedString, iv)));
         daemonSpawnArgumentStage.append(":").append(iv);
         daemonSpawnArgument = daemonSpawnArgumentStage.toString();
      } catch (Exception e) {
         buffer = new StringBuilder();
         buffer.append("failed to encrypt local session credentials, station").append(this.appName).append(" may not start properly");
         this.filter.warning(buffer.toString());
         buffer = new StringBuilder();
         buffer.append("WARNING: Failed to encrypt local session credentials, station").append(this.appName).append(" may not start properly\n");
         this.appOut.printf(buffer.toString().getBytes(StandardCharsets.UTF_8));
      }

      if (checkDefaultCredentialsConditions(this.platformProvider, authenticator.getAuthDomain(), this.appOut, this.filter)
         && checkDefaultSystemPassphraseConditions(this.platformProvider, this.appOut, this.filter)) {
         String nreCoreVersionString = NiagaraDaemon.getInstance().daemonVersion;
         String bajaVersionString = null;
         File bajaModule = new File(NiagaraFiles.getModulesPath(), "baja.jar");
         if (bajaModule.exists()) {
            try (JarFile niagaradJar = new JarFile(bajaModule)) {
               JarEntry moduleXmlEntry = niagaradJar.getJarEntry("META-INF/module.xml");
               XElem moduleElement = XParser.make(new BufferedInputStream(niagaradJar.getInputStream(moduleXmlEntry))).parse();
               bajaVersionString = moduleElement.get("vendorVersion", null);
            } catch (Exception var40) {
            }
         }

         if (isNreCoreVersionMismatched(nreCoreVersionString, bajaVersionString)) {
            buffer = new StringBuilder();
            buffer.append("\n")
               .append("********************************************************************************\n")
               .append("*\n")
               .append("*   NRE CORE VERSION '")
               .append(nreCoreVersionString)
               .append("' != BAJA VERSION '")
               .append(bajaVersionString)
               .append("', CANNOT START!\n")
               .append("*\n")
               .append("********************************************************************************\n")
               .append("\n");
            this.appOut.printf(buffer.toString().getBytes(StandardCharsets.UTF_8));
            return false;
         }

         ArrayList<String> warningDescriptions = PlatformInfo.getInstance().warningDescriptions();
         if (warningDescriptions != null && warningDescriptions.size() > 0) {
            buffer = new StringBuilder();
            buffer.append("###########################################################################\n");
            buffer.append("#\n");
            buffer.append("# WARNING: Platform contains ").append(warningDescriptions.size()).append(" warnings\n");
            buffer.append("#\n");

            for (String warningDescription : warningDescriptions) {
               buffer.append("#   ").append(warningDescription).append("\n");
            }

            buffer.append("#\n");
            buffer.append("###########################################################################\n");
            this.appOut.printf(buffer.toString().getBytes(StandardCharsets.UTF_8));
         }

         Servlet servlet = NiagaraDaemon.getInstance().webServer.getServlet("nreconfig");
         if (servlet instanceof NreConfigurationServlet) {
            NreConfigurationServlet nreConfigurationServlet = (NreConfigurationServlet)servlet;
            StringBuilder nreConfigurationSettings = new StringBuilder();
            if (!nreConfigurationServlet.printSettings(nreConfigurationSettings)) {
               buffer = new StringBuilder();
               buffer.append("using custom NRE configuration (").append(nreConfigurationSettings).append(")");
               this.filter.info(buffer.toString());
               buffer = new StringBuilder();
               buffer.append("NOTICE: Using custom NRE configuration (").append(nreConfigurationSettings).append(")\n");
               this.appOut.printf(buffer.toString().getBytes(StandardCharsets.UTF_8));
            }
         }

         Set<String> enabledProfiles = new HashSet<>();
         Collections.addAll(enabledProfiles, this.platformProvider.getRequiredRuntimeProfiles().split(","));
         Collections.addAll(
            enabledProfiles, NiagaraDaemon.props.getProperty("runtimeProfilesEnabled", this.platformProvider.getRequiredRuntimeProfiles()).split(",")
         );
         String enableProfilesString = String.join(",", enabledProfiles);
         String stationExecutablePath = NiagaraDaemon.NIAGARA_HOME + File.separator + "bin" + File.separator + "station";
         String daemonspawn = "-daemonspawn:" + daemonSpawnArgument;
         String rp = "-rp:" + enableProfilesString;
         String[] commandString = new String[]{stationExecutablePath, this.appName, daemonspawn, rp};
         ProcessBuilder builder = new ProcessBuilder(commandString);
         builder.directory(new File(this.registry.getAppsDirPath() + File.separator + this.appName));
         builder.redirectErrorStream(true);

         try {
            this.hProcess = builder.start();
            this.hStdInput = this.hProcess.getOutputStream();
            this.pipeThread = new App.ApplicationPipeThread(this.hProcess.getInputStream(), this.appOut.getWriteHandle());
            this.pipeThread.start();
            return true;
         } catch (IOException e) {
            buffer = new StringBuilder();
            buffer.append("ERROR: Error starting station ").append(this.appName).append(", unable to create station process: ").append(e).append("\n");
            this.appOut.printf(buffer.toString().getBytes(StandardCharsets.UTF_8));
            buffer = new StringBuilder();
            buffer.append("error starting station ").append(this.appName).append(", unable to create station process: ").append(e);
            this.filter.severe(buffer.toString());
            return false;
         }
      } else {
         return false;
      }
   }

   @Override
   protected boolean allowRestart() {
      boolean defaultValue = PlatformInfo.getInstance().allowStationRestartDefault();
      String value = NiagaraDaemon.props.getProperty("allowStationRestart", String.valueOf(defaultValue));
      return Boolean.parseBoolean(value);
   }

   @Override
   protected boolean isRecoverableError(int exitCode) {
      switch (exitCode) {
         case -7:
         case -6:
         case -5:
         case -4:
         case -3:
         case -2:
            return false;
         default:
            return true;
      }
   }

   @Override
   public void generateStackDump() {
      String appType = this.registry.getAppType();
      if (this.hProcess == null) {
         this.filter.severe("attempt to get stack dump for " + appType + " " + this.appName + " that is not running");
      } else {
         this.sendMessage("threads");
      }
   }
}
