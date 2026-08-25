package com.tridium.nre.platform;

import com.tridium.nre.auth.GroupAccount;
import com.tridium.nre.auth.NativeAccount;
import com.tridium.nre.auth.UserAccount;
import com.tridium.nre.security.SecretBytes;
import com.tridium.nre.security.SecretChars;
import com.tridium.nre.security.SecurityInitializer;
import com.tridium.nre.security.SystemPassphrase;
import com.tridium.nre.security.km.KeyMaterial;
import com.tridium.nre.util.LicenseMode;
import com.tridium.nre.util.SimpleKeyValueUtil;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import javax.baja.nre.platform.RuntimeProfile;
import javax.baja.nre.util.TextUtil;

public final class NJavaPlatformProvider extends JavaPlatformProvider {
   private static final String OS_NAME = AccessController.doPrivileged(() -> System.getProperty("os.name"));
   private static final String OS_VERSION = AccessController.doPrivileged(() -> System.getProperty("os.version"));
   private static final String OS_DESCRIPTION = OS_NAME + " " + OS_VERSION;
   private static final String OS_ARCH = AccessController.doPrivileged(() -> System.getProperty("os.version"));
   private static final String NIAGARA_HOST_ID = AccessController.doPrivileged(() -> System.getenv("NIAGARA_HOST_ID"));
   private static final String WINDOWS_ETC_HOSTS = AccessController.doPrivileged(() -> System.getenv("SystemRoot")) + "\\system32\\drivers\\etc\\hosts";
   private static String[] fsNames = null;
   private static long mountsLastModified = 0L;
   private final String SIMPLE_SYSTEM_PASSPHRASE_NAME = ".sp";
   private static final String DEFAULT_VENDOR = "tridium";
   private static final String DEFAULT_USERNAME = "tridium";
   private static final String DEFAULT_PASSWORD = "niagara";

   NJavaPlatformProvider() {
   }

   @Override
   boolean doLoad() {
      return true;
   }

   @Override
   public String getOsName() {
      String osName = OS_NAME;
      if (osName != null) {
         osName = TextUtil.split(osName, ' ')[0];
      }

      return osName;
   }

   @Override
   public String getOsVersion() {
      return OS_VERSION;
   }

   @Override
   public String getOsDescription() {
      return OS_DESCRIPTION;
   }

   @Override
   public String getOsArchitecture() {
      return OS_ARCH;
   }

   @Override
   public boolean isOsInstallable() {
      return false;
   }

   @Override
   public boolean getAllowStationRestartDefault() {
      return true;
   }

   @Override
   public boolean getAllowBrandChangeDefault() {
      return false;
   }

   @Override
   public boolean isLicenseReadonly() {
      return false;
   }

   @Override
   public boolean isSoftwareReadonly() {
      return false;
   }

   @Override
   public boolean isNiagaraHomeReadonly() {
      return false;
   }

   @Override
   public String[] getAllFileSystemNames() {
      if (OperatingSystemEnum.isOS(OperatingSystemEnum.windows)) {
         File[] roots = File.listRoots();
         ArrayList<String> readyFileSystems = new ArrayList<>();

         for (File root : roots) {
            try {
               new File(root.getPath(), "test").getCanonicalPath();
               readyFileSystems.add(root.getPath());
            } catch (Throwable var18) {
            }
         }

         fsNames = readyFileSystems.toArray(new String[0]);
      } else if (OperatingSystemEnum.isOS(OperatingSystemEnum.linux)) {
         File mounts = new File("/proc/mounts");
         long lastModified = mounts.lastModified();
         if (lastModified <= mountsLastModified) {
            return fsNames;
         }

         mountsLastModified = lastModified;

         try {
            ArrayList<String> fsList = new ArrayList<>();
            Process p = Runtime.getRuntime().exec(new String[]{"awk", "{print $2}", "/proc/mounts"});

            String line;
            try (BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
               while ((line = in.readLine()) != null) {
                  fsList.add(line);
               }
            }

            fsNames = fsList.toArray(new String[0]);
         } catch (IOException var21) {
         }
      } else if (OperatingSystemEnum.isOS(OperatingSystemEnum.qnx)) {
         fsNames = new String[]{"/"};
      }

      return fsNames;
   }

   @Override
   public String getFileSystemDisplayName(String filePath) {
      if (filePath == null) {
         return null;
      } else {
         return filePath.length() > 4096 ? null : filePath;
      }
   }

   @Override
   public long getFreeBytes(String filePath) {
      if (filePath == null) {
         return -1L;
      }

      if (filePath.length() > 4096) {
         return -1L;
      }

      String targetFileSystemName = this.getFileSystemName(filePath);
      return targetFileSystemName == null ? -1L : AccessController.doPrivileged(() -> new File(targetFileSystemName).getUsableSpace());
   }

   @Override
   public long getTotalBytes(String filePath) {
      if (filePath == null) {
         return -1L;
      }

      if (filePath.length() > 4096) {
         return -1L;
      }

      String targetFileSystemName = this.getFileSystemName(filePath);
      return targetFileSystemName == null ? -1L : AccessController.doPrivileged(() -> new File(targetFileSystemName).getTotalSpace());
   }

   @Override
   public long getMaxFileCount(String filePath) {
      if (filePath == null) {
         return -1L;
      } else {
         return filePath.length() > 4096 ? -1L : -1L;
      }
   }

   @Override
   public long getCurrentFileCount(String filePath) {
      if (filePath == null) {
         return -1L;
      } else {
         return filePath.length() > 4096 ? -1L : -1L;
      }
   }

   @Override
   public long getMaxOpenFileDescriptorCount() {
      return -1L;
   }

   @Override
   public long getCurrentOpenFileDescriptorCount() {
      return -1L;
   }

   @Override
   public boolean isFileRegular(String filePath) {
      if (filePath == null) {
         return false;
      }

      if (filePath.length() > 4096) {
         return false;
      }

      File file = new File(filePath);
      return file.exists() && file.isFile();
   }

   @Override
   public String getFileSystemName(String filePath) {
      if (filePath == null) {
         return null;
      }

      if (filePath.length() > 4096) {
         return null;
      }

      File file = new File(filePath);

      try {
         String canonicalPath = file.getCanonicalPath();
         String fsName = "/";
         String[] fsNames = this.getAllFileSystemNames();

         for (String currentFsName : fsNames) {
            if (!"/".equals(currentFsName) && canonicalPath.startsWith(currentFsName)) {
               fsName = currentFsName;
            }
         }

         return fsName;
      } catch (IOException var10) {
         return null;
      }
   }

   @Override
   public boolean isSystemTimeReadonly() {
      return true;
   }

   @Override
   public int setNativeTimeZone(String tzId) {
      return tzId == null ? -1 : -1;
   }

   @Override
   public int setSystemTime(long millis) {
      return -1;
   }

   @Override
   public long getFreePhysicalMemoryBytes() {
      return -1L;
   }

   @Override
   public long getTotalPhysicalMemoryBytes() {
      return -1L;
   }

   @Override
   public int getCurrentCPUUtilization() {
      return -1;
   }

   @Override
   public int getOverallCPUUtilization() {
      return -1;
   }

   @Override
   public long getIdleTime(int cpuID) {
      return -1L;
   }

   @Override
   public long getCpuTime(String processName, boolean includeChildren) {
      return -1L;
   }

   @Override
   public String getHostId() {
      return NIAGARA_HOST_ID;
   }

   @Override
   public String getHostId(LicenseMode licenseModePreference) {
      return this.getHostId();
   }

   @Override
   public String getHostModel() {
      return "workstation";
   }

   @Override
   public String getHostModelVersion() {
      return "4.0";
   }

   @Override
   public String getHostProduct() {
      return "workstation";
   }

   @Override
   public String getHostVendor() {
      return "tridium";
   }

   @Override
   public String getHostSerialNumber() {
      return "12345";
   }

   @Override
   public String getHostParts() {
      return "<parts/>";
   }

   @Override
   public long getTickCount() {
      return System.nanoTime() / 1000000L;
   }

   @Override
   public long getNanoCount() {
      return System.nanoTime() / 1000L;
   }

   @Override
   public int createWatchdog(String name) {
      return name == null ? -1 : -1;
   }

   @Override
   public int destroyWatchdog(String name) {
      return name == null ? -1 : -1;
   }

   @Override
   public int getWatchdogCycles(String name) {
      return name == null ? -1 : -1;
   }

   @Override
   public int getWatchdogPolicy(String name) {
      return name == null ? -1 : -1;
   }

   @Override
   public int getWatchdogTimeout(String name) {
      return name == null ? -1 : -1;
   }

   @Override
   public int updateWatchdog(String name, int cycles, int policy, int timeout) {
      return -1;
   }

   @Override
   public boolean canWriteSystemLogMessages() {
      return false;
   }

   @Override
   public boolean enableSystemLogging() {
      return false;
   }

   @Override
   public void log(int level, String message) {
   }

   @Override
   public boolean canReadSystemLogMessages() {
      return false;
   }

   @Override
   public String readSystemLog(String log) {
      return "";
   }

   @Override
   public String getHostFileName() {
      return OperatingSystemEnum.isOS(OperatingSystemEnum.windows) ? WINDOWS_ETC_HOSTS : "/etc/hosts";
   }

   @Override
   public String getNetworkSettingsXML() {
      return "<tcpIpSettings hostname=\""
         + this.getComputerName()
         + "\" isReadonly=\"true\" isIPv6Readonly=\"true\" usesAdapterLevelSettings=\"false\" domain=\""
         + this.getComputerDomain(true)
         + "\" defaultGateway=\"0.0.0.0\"><dnsHosts limit=\"2\"></dnsHosts><adapters></adapters></tcpIpSettings>";
   }

   @Override
   public int setNetworkSettingsXML(String networkSettingsXML) {
      return networkSettingsXML == null ? -1 : -1;
   }

   @Override
   public String[] getAdapterNames() {
      try {
         Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
         ArrayList<String> interfaceList = new ArrayList<>();

         while (interfaces.hasMoreElements()) {
            NetworkInterface networkInterface = interfaces.nextElement();
            if (!networkInterface.isLoopback() && networkInterface.getInterfaceAddresses().size() != 0) {
               interfaceList.add(networkInterface.getName());
            }
         }

         return interfaceList.toArray(new String[0]);
      } catch (Throwable var4) {
         return new String[0];
      }
   }

   @Override
   public boolean usesPosixSockets() {
      return !OperatingSystemEnum.isOS(OperatingSystemEnum.windows);
   }

   @Override
   public void reboot() {
   }

   @Override
   public String getTempDirPath() {
      return AccessController.doPrivileged(() -> System.getProperty("java.io.tmpdir"));
   }

   @Override
   public String getSupportedRuntimeProfiles() {
      Set<String> supportedProfileNames = new HashSet<>();

      for (RuntimeProfile profile : RuntimeProfile.values()) {
         supportedProfileNames.add(profile.name());
      }

      return String.join(",", supportedProfileNames);
   }

   @Override
   public String getRequiredRuntimeProfiles() {
      return "rt";
   }

   @Override
   public boolean isStationPlatformReadonly() {
      return true;
   }

   @Override
   public boolean isEmbedded() {
      return false;
   }

   @Override
   public boolean isDaemonDebugSupported() {
      return false;
   }

   @Override
   public boolean requireSecurePlatform() {
      return true;
   }

   @Override
   public boolean requireSubscription() {
      return false;
   }

   @Override
   public boolean notifyApplicationStatus(String applicationType, String applicationName, int applicationStatus) {
      if (applicationType == null) {
         return false;
      } else if (applicationType.length() > 128) {
         return false;
      } else {
         return applicationName == null ? false : applicationName.length() <= 128;
      }
   }

   @Override
   public boolean isSSHSupported() {
      return false;
   }

   @Override
   public int getSSHPort() {
      return -1;
   }

   @Override
   public boolean setSSHPort(int port) {
      return false;
   }

   @Override
   public boolean supportsNativeDiagnostics() {
      return false;
   }

   @Override
   public String[][] getNativeDiagnosticsCommands() {
      return (String[][])null;
   }

   @Override
   public String executeNativeDiagnosticsCommand(int commandIndex) {
      return null;
   }

   @Override
   public long getProcessId() {
      String processId = ManagementFactory.getRuntimeMXBean().getName();
      if (processId.contains("@")) {
         try {
            return Long.parseLong(processId.substring(0, processId.indexOf("@")));
         } catch (NumberFormatException var3) {
         }
      }

      return -1L;
   }

   @Override
   public void dumpThreads() {
   }

   @Override
   public int lockFile(String filePath) {
      return FileLockUtil.lockFile(filePath);
   }

   @Override
   public int unlockFile(String filePath, int lockId) {
      return FileLockUtil.unlockFile(filePath, lockId);
   }

   @Override
   public boolean allowPlatformDaemonRestart() {
      return false;
   }

   @Override
   public int restartPlatformDaemon() {
      return -1;
   }

   @Override
   public boolean platformDaemonShutdownRequested() {
      return false;
   }

   @Override
   public String getSupportedAuthenticationTypes() {
      return "scram-glibc-sha512/file";
   }

   @Override
   public boolean isAuthenticationReadonly() {
      return false;
   }

   @Override
   public String getDefaultUsername() {
      return "tridium";
   }

   @Override
   public String getDefaultPassword() {
      return "niagara";
   }

   @Override
   public boolean synchronizeUsers(String userName, String password) {
      if (userName == null) {
         return false;
      } else {
         return password == null ? false : false;
      }
   }

   @Override
   public String getComputerName() {
      try {
         return InetAddress.getLocalHost().getHostName();
      } catch (Throwable var2) {
         return "";
      }
   }

   @Override
   public String getComputerDomain(boolean fullyQualified) {
      try {
         String canonicalHostname = InetAddress.getLocalHost().getCanonicalHostName();
         if (canonicalHostname.contains(".")) {
            return canonicalHostname.substring(canonicalHostname.indexOf(".") + 1);
         }
      } catch (Throwable var3) {
      }

      return "";
   }

   @Override
   public boolean isGroupMember(String userId, String groupId) {
      if (userId == null) {
         return false;
      } else {
         return groupId == null ? false : false;
      }
   }

   @Override
   public String getDefaultAdminGroupName() {
      return null;
   }

   @Override
   public boolean isPasswordValid(String id, String password) {
      if (id == null) {
         return false;
      } else {
         return password == null ? false : false;
      }
   }

   @Override
   public String getDomainGroupsXml(String id) {
      return id == null ? null : null;
   }

   @Override
   public String getIdFromName(String name, boolean userAccount) {
      return name == null ? null : null;
   }

   @Override
   public String getNameFromId(String id, boolean userAccount) {
      return id == null ? null : null;
   }

   @Override
   public NativeAccount getAccountFromName(String name, String defaultDomain, boolean userAccount) {
      if (name == null) {
         return null;
      } else {
         return defaultDomain == null ? null : null;
      }
   }

   @Override
   public NativeAccount getAccountFromId(String id, boolean userAccount) {
      return id == null ? null : null;
   }

   @Override
   public GroupAccount getDefaultAdminGroup() {
      return null;
   }

   @Override
   public GroupAccount[] getAccounts(String accountIdList, char deliminator) {
      return accountIdList == null ? null : null;
   }

   @Override
   public UserAccount getAccountFromCredentials(String username, String password, boolean defaultLocal) {
      if (username == null) {
         return null;
      } else {
         return password == null ? null : null;
      }
   }

   @Override
   public String getPasswordHash(String username) {
      return username == null ? null : null;
   }

   @Override
   public boolean providesAccountManagement() {
      return false;
   }

   @Override
   public String getAccountXml(String id, boolean userAccount) {
      return id == null ? null : null;
   }

   @Override
   public String addUserAccount(String name, String password, String comment, boolean passwordHashed) {
      if (name == null) {
         return null;
      } else if (password == null) {
         return null;
      } else {
         return comment == null ? null : null;
      }
   }

   @Override
   public boolean removeUserAccount(String id) {
      return id == null ? false : false;
   }

   @Override
   public boolean addUserToGroup(String userId, String groupId) {
      if (userId == null) {
         return false;
      } else {
         return groupId == null ? false : false;
      }
   }

   @Override
   public boolean removeUserFromGroup(String userId, String groupId) {
      if (userId == null) {
         return false;
      } else {
         return groupId == null ? false : false;
      }
   }

   @Override
   public boolean changeUserPassword(String id, String oldPassword, String newPassword) {
      if (id == null) {
         return false;
      } else if (oldPassword == null) {
         return false;
      } else {
         return newPassword == null ? false : false;
      }
   }

   @Override
   public boolean isSystemPasswordReadonly() {
      return false;
   }

   @Override
   public SecretChars getSystemPassword() {
      SystemPassphrase.checkSystemPassphrasePermissions();
      SecretChars platformPassword = null;

      try {
         byte[] platformPasswordBytes = AccessController.doPrivileged(
            () -> SimpleKeyValueUtil.getInstance(SecurityInitializer.getInstance().getSecurityInfoProvider().getSecurityDir().getPath()).get(".sp")
         );
         if (platformPasswordBytes != null && platformPasswordBytes.length > 0) {
            platformPassword = SecretChars.fromSecretBytes(new SecretBytes(platformPasswordBytes, true));
         }
      } catch (PrivilegedActionException pae) {
         SystemPassphrase.LOG.log(Level.SEVERE, "Permission failure when getting system passphrase", pae);
         if (pae.getCause() instanceof RuntimeException) {
            throw (RuntimeException)pae.getCause();
         }

         throw new RuntimeException(pae.getCause());
      }

      return platformPassword == null ? SecretChars.fromString(this.getDefaultPassword()) : platformPassword;
   }

   @Override
   public boolean setSystemPassword(String oldPassword, String newPassword) {
      SystemPassphrase.checkSystemPassphrasePermissions();
      if (this.isSystemPasswordReadonly()) {
         return false;
      }

      if (oldPassword == null) {
         return false;
      }

      if (oldPassword.length() > 128) {
         return false;
      }

      if (newPassword == null) {
         return false;
      }

      if (newPassword.length() > 128) {
         return false;
      }

      if (!SystemPassphrase.checkSystemPassphrase(oldPassword)) {
         return false;
      }

      try {
         return AccessController.doPrivileged(
            () -> SimpleKeyValueUtil.getInstance(SecurityInitializer.getInstance().getSecurityInfoProvider().getSecurityDir().getPath())
               .set(".sp", newPassword.getBytes(StandardCharsets.UTF_8))
         );
      } catch (PrivilegedActionException pae) {
         SystemPassphrase.LOG.log(Level.SEVERE, "Permission failure when setting system passphrase", pae);
         if (pae.getCause() instanceof RuntimeException) {
            throw (RuntimeException)pae.getCause();
         } else {
            throw new RuntimeException(pae.getCause());
         }
      }
   }

   @Override
   public boolean checkForKeyMaterialUpgrade(File securityDirectory, String keyName) {
      if (securityDirectory == null) {
         return false;
      }

      if (securityDirectory.getPath().length() > 4096) {
         return false;
      }

      if (keyName == null) {
         return false;
      }

      if (keyName.length() > 4096) {
         return false;
      }

      KeyMaterial.checkKeyMaterialPermissions();
      if (OperatingSystemEnum.isOS(OperatingSystemEnum.windows)) {
         try {
            byte[] simpleKeyMaterial = SimpleKeyValueUtil.getInstance(securityDirectory.getPath()).get(".km");
            if (simpleKeyMaterial != null && simpleKeyMaterial.length > 0) {
               KeyMaterial.LOG.finest("Migrating legacy key material at '" + securityDirectory.getPath() + File.separator + keyName + "' to new location");
               if (!this.setKeyMaterial(securityDirectory, keyName, simpleKeyMaterial)) {
                  throw new Exception("Could not set key material during migration");
               }
            }
         } catch (Exception e) {
            throw new RuntimeException("Error occurred during migration", e);
         }
      }

      return true;
   }

   @Override
   public boolean supportsKeyMaterialRecovery() {
      return false;
   }

   @Override
   public byte[] getKeyMaterial(File securityDirectory, String keyName) {
      if (securityDirectory == null) {
         return null;
      }

      if (securityDirectory.getPath().length() > 4096) {
         return null;
      }

      if (keyName == null) {
         return null;
      }

      if (keyName.length() > 4096) {
         return null;
      }

      KeyMaterial.checkKeyMaterialPermissions();

      try {
         return AccessController.doPrivileged(() -> SimpleKeyValueUtil.getInstance(securityDirectory.getPath()).get(keyName));
      } catch (PrivilegedActionException pae) {
         KeyMaterial.LOG.log(Level.SEVERE, "Permission failure when getting key material", pae);
         if (pae.getCause() instanceof RuntimeException) {
            throw (RuntimeException)pae.getCause();
         } else {
            throw new RuntimeException(pae.getCause());
         }
      }
   }

   @Override
   public boolean setKeyMaterial(File securityDirectory, String keyName, byte[] keyMaterial) {
      if (securityDirectory == null) {
         return false;
      }

      if (securityDirectory.getPath().length() > 4096) {
         return false;
      }

      if (keyName == null) {
         return false;
      }

      if (keyName.length() > 4096) {
         return false;
      }

      if (keyMaterial != null && keyMaterial.length > 32) {
         return false;
      }

      KeyMaterial.checkKeyMaterialPermissions();

      try {
         return AccessController.doPrivileged(() -> SimpleKeyValueUtil.getInstance(securityDirectory.getPath()).set(keyName, keyMaterial));
      } catch (PrivilegedActionException pae) {
         KeyMaterial.LOG.log(Level.SEVERE, "Permission failure when setting key material", pae);
         if (pae.getCause() instanceof RuntimeException) {
            throw (RuntimeException)pae.getCause();
         } else {
            throw new RuntimeException(pae.getCause());
         }
      }
   }

   @Override
   public long getKeyMaterialLastModified(File securityDirectory, String keyName) {
      if (securityDirectory == null) {
         return 0L;
      }

      if (securityDirectory.getPath().length() > 4096) {
         return 0L;
      }

      if (keyName == null) {
         return 0L;
      }

      if (keyName.length() > 4096) {
         return 0L;
      }

      KeyMaterial.checkKeyMaterialPermissions();
      return AccessController.doPrivileged(() -> new File(securityDirectory, keyName).lastModified());
   }

   @Override
   public String getAltDatabasePath() {
      return null;
   }

   @Override
   public String getAltArchivePath() {
      return null;
   }

   @Override
   public String getAltArchiveZipPath() {
      return null;
   }

   @Override
   public int getArchiveBackupCount() {
      return 1;
   }

   @Override
   public boolean supportsNREConfiguration() {
      return false;
   }

   @Override
   public NREMemoryPool getNRESystemReserveMemoryPool() {
      return new NREMemoryPool(0, 0, 0);
   }

   @Override
   public NREMemoryPool getNREHeapMemoryPool() {
      return new NREMemoryPool(0, 0, 0);
   }

   @Override
   public NREMemoryPool getNRECodeCacheMemoryPool() {
      return new NREMemoryPool(0, 0, 0);
   }

   @Override
   public NREMemoryPool getNREMetaSpaceMemoryPool() {
      return new NREMemoryPool(0, 0, 0);
   }

   @Override
   public NREMemoryPool getNRERamDiskMemoryPool() {
      return new NREMemoryPool(0, 0, 0);
   }
}
