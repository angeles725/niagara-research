package com.tridium.nre.platform;

import com.tridium.nre.auth.GroupAccount;
import com.tridium.nre.auth.NativeAccount;
import com.tridium.nre.auth.QnxUserManager;
import com.tridium.nre.auth.SnapUserManager;
import com.tridium.nre.auth.UserAccount;
import com.tridium.nre.security.SecretBytes;
import com.tridium.nre.security.SecretChars;
import com.tridium.nre.security.SecurityInitializer;
import com.tridium.nre.security.SystemPassphrase;
import com.tridium.nre.security.km.KeyMaterial;
import com.tridium.nre.subscription.SubscriptionLicenseUtil;
import com.tridium.nre.util.LicenseMode;
import com.tridium.nre.util.SimpleKeyValueUtil;
import com.tridium.nre.util.SnapNetconvertManager;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import javax.baja.nre.platform.RuntimeProfile;
import javax.baja.nre.util.Array;
import javax.baja.nre.util.ByteArrayUtil;
import javax.baja.nre.util.TextUtil;
import javax.baja.xml.XElem;
import javax.baja.xml.XParser;

public abstract class NativePlatformProvider implements IPlatformProvider {
   public static boolean nativesLoaded = false;
   private static boolean errorPrinted = false;
   private static final String NIAGARA_SUPPORTED_RUNTIME_PROFILES = AccessController.doPrivileged(
      () -> System.getProperty("niagara.supported.runtime.profiles")
   );
   private static final String NIAGARA_REQUIRED_RUNTIME_PROFILES = AccessController.doPrivileged(() -> System.getProperty("niagara.required.runtime.profiles"));
   private static final String NIAGARA_ALTERNATIVE_DATABASE_PATH = AccessController.doPrivileged(() -> System.getProperty("niagara.alternative.database.path"));
   private static final String NIAGARA_ALTERNATIVE_ARCHIVE_PATH = AccessController.doPrivileged(() -> System.getProperty("niagara.alternative.archive.path"));
   private static final String NIAGARA_ALTERNATIVE_ARCHIVE_ZIP_PATH = AccessController.doPrivileged(
      () -> System.getProperty("niagara.alternative.archive.zip.path")
   );
   private static final String SNAP_NETCONVERT_PROPERTY = "niagara.use.snap.netconvert";
   private static final String QNX_USERMGR_PROPERTY = "niagara.use.qnx.usermgr";
   private static final String SNAP_USERMGR_PROPERTY = "niagara.use.snap.usermgr";
   private static final Object NATIVE_FS_MONITOR = new Object();
   private static boolean fileSystemsLoaded;
   private static String[] fileSystemNames;
   private static HashMap<String, NativePlatformProvider.FileSystemInfo> fileSystemInfoByName;
   private static boolean canWriteSystemLogMessages = false;
   private static boolean nativeLoggingEnabled = false;
   private static boolean canReadSystemLogMessages = false;
   private static boolean logSettingsLoaded = false;

   static synchronized boolean load() {
      if (!nativesLoaded) {
         try {
            System.loadLibrary("nre");
            nativesLoaded = true;
            errorPrinted = false;
         } catch (Throwable e) {
            if (!errorPrinted) {
               System.err.println("WARNING [" + new Date() + "][nre] cannot load native nre library (" + e + ")");
               e.printStackTrace();
               errorPrinted = true;
            }
         }
      }

      return nativesLoaded;
   }

   @Override
   public String getOsName() {
      return getOsName0();
   }

   @Override
   public String getOsVersion() {
      return getOsVersion0();
   }

   @Override
   public String getOsDescription() {
      return getOsDescription0();
   }

   @Override
   public String getOsArchitecture() {
      return getOsArchitecture0();
   }

   @Override
   public boolean isOsInstallable() {
      return isOsInstallable0();
   }

   @Override
   public boolean isEmbedded() {
      return isEmbedded0();
   }

   @Override
   public boolean isDaemonDebugSupported() {
      return isDaemonDebugSupported0();
   }

   @Override
   public boolean requireSecurePlatform() {
      return requireSecurePlatform0();
   }

   @Override
   public boolean requireSubscription() {
      return NativePlatformProvider.HostMetaDataHolder.HOST_ID.endsWith("#");
   }

   @Override
   public boolean getAllowStationRestartDefault() {
      return getAllowStationRestartDefault0();
   }

   @Override
   public boolean getAllowBrandChangeDefault() {
      return getAllowBrandChangeDefault0();
   }

   @Override
   public boolean isLicenseReadonly() {
      return isLicenseReadonly0();
   }

   @Override
   public boolean isSoftwareReadonly() {
      return isSoftwareReadonly0();
   }

   @Override
   public boolean isNiagaraHomeReadonly() {
      return isNiagaraHomeReadonly0();
   }

   @Override
   public String getSupportedRuntimeProfiles() {
      if (NIAGARA_SUPPORTED_RUNTIME_PROFILES != null) {
         return NIAGARA_SUPPORTED_RUNTIME_PROFILES;
      }

      Set<String> supportedProfileNames = new HashSet<>();

      for (RuntimeProfile profile : RuntimeProfile.values()) {
         supportedProfileNames.add(profile.name());
      }

      String defaultSupportedProfiles = String.join(",", supportedProfileNames);
      System.err.println("WARNING [" + new Date() + "][nre] 'niagara.supported.runtime.profiles' not defined, returning '" + defaultSupportedProfiles + "'");
      return defaultSupportedProfiles;
   }

   @Override
   public String getRequiredRuntimeProfiles() {
      if (NIAGARA_REQUIRED_RUNTIME_PROFILES != null) {
         return NIAGARA_REQUIRED_RUNTIME_PROFILES;
      }

      String defaultRequiredProfiles = RuntimeProfile.rt.name();
      System.err.println("WARNING [" + new Date() + "][nre] 'niagara.required.runtime.profiles' not defined, returning '" + defaultRequiredProfiles + "'");
      return defaultRequiredProfiles;
   }

   @Override
   public boolean notifyApplicationStatus(String applicationType, String applicationName, int applicationStatus) {
      return notifyApplicationStatus0(applicationType, applicationName, applicationStatus);
   }

   @Override
   public boolean isSSHSupported() {
      return isSSHSupported0();
   }

   @Override
   public int getSSHPort() {
      return getSSHPort0();
   }

   @Override
   public boolean setSSHPort(int port) {
      return setSSHPort0(port);
   }

   @Override
   public boolean supportsNativeDiagnostics() {
      return supportsNativeDiagnostics0();
   }

   @Override
   public String[][] getNativeDiagnosticsCommands() {
      return getNativeDiagnosticsCommands0();
   }

   @Override
   public String executeNativeDiagnosticsCommand(int commandIndex) {
      return executeNativeDiagnosticsCommand0(commandIndex);
   }

   @Override
   public String[] getAllFileSystemNames() {
      synchronized (NATIVE_FS_MONITOR) {
         return !loadFileSystems() ? null : fileSystemNames;
      }
   }

   @Override
   public String getFileSystemName(String filePath) {
      if (filePath == null) {
         return null;
      }

      synchronized (NATIVE_FS_MONITOR) {
         if (!loadFileSystems()) {
            return null;
         }

         if (fileSystemInfoByName.containsKey(filePath)) {
            return filePath;
         }

         Path path;
         try {
            path = AccessController.doPrivileged(() -> Paths.get(new File(filePath).getCanonicalFile().toURI()));
         } catch (PrivilegedActionException pae) {
            return null;
         }

         String bestMatch = null;

         for (String fileSystemName : fileSystemNames) {
            if (path.startsWith(fileSystemName) && (bestMatch == null || bestMatch.length() < fileSystemName.length())) {
               bestMatch = fileSystemName;
            }
         }

         return bestMatch;
      }
   }

   @Override
   public String getFileSystemDisplayName(String filePath) {
      if (filePath == null) {
         return null;
      }

      synchronized (NATIVE_FS_MONITOR) {
         if (!loadFileSystems()) {
            return null;
         }

         String fileSystem = this.getFileSystemName(filePath);
         if (fileSystem == null) {
            return null;
         }

         NativePlatformProvider.FileSystemInfo fileSystemInfo = fileSystemInfoByName.get(fileSystem);
         return fileSystemInfo == null ? null : fileSystemInfo.fileSystemDisplayName;
      }
   }

   @Override
   public long getFreeBytes(String filePath) {
      if (filePath == null) {
         return -1L;
      }

      synchronized (NATIVE_FS_MONITOR) {
         if (!loadFileSystems()) {
            return -1L;
         }

         String fileSystem = this.getFileSystemName(filePath);
         if (fileSystem == null) {
            return -1L;
         }

         NativePlatformProvider.FileSystemInfo fileSystemInfo = fileSystemInfoByName.get(fileSystem);
         return fileSystemInfo == null ? -1L : AccessController.doPrivileged(fileSystemInfo.fileSystemFile::getUsableSpace);
      }
   }

   @Override
   public long getTotalBytes(String filePath) {
      if (filePath == null) {
         return -1L;
      }

      synchronized (NATIVE_FS_MONITOR) {
         if (!loadFileSystems()) {
            return -1L;
         }

         String fileSystem = this.getFileSystemName(filePath);
         if (fileSystem == null) {
            return -1L;
         }

         NativePlatformProvider.FileSystemInfo fileSystemInfo = fileSystemInfoByName.get(fileSystem);
         return fileSystemInfo == null ? -1L : fileSystemInfo.totalBytes;
      }
   }

   @Override
   public long getMaxFileCount(String filePath) {
      if (filePath == null) {
         return -1L;
      }

      synchronized (NATIVE_FS_MONITOR) {
         if (!loadFileSystems()) {
            return -1L;
         }

         String fileSystem = this.getFileSystemName(filePath);
         if (fileSystem == null) {
            return -1L;
         }

         NativePlatformProvider.FileSystemInfo fileSystemInfo = fileSystemInfoByName.get(fileSystem);
         return fileSystemInfo == null ? -1L : fileSystemInfo.maxFileCount;
      }
   }

   @Override
   public long getCurrentFileCount(String filePath) {
      if (filePath == null) {
         return -1L;
      }

      synchronized (NATIVE_FS_MONITOR) {
         if (!loadFileSystems()) {
            return -1L;
         }

         String fileSystem = this.getFileSystemName(filePath);
         return fileSystem == null ? -1L : getCurrentFileCount0(fileSystem);
      }
   }

   @Override
   public long getMaxOpenFileDescriptorCount() {
      return getMaxOpenFileDescriptorCount0();
   }

   @Override
   public long getCurrentOpenFileDescriptorCount() {
      return getCurrentOpenFileDescriptorCount0();
   }

   @Override
   public boolean isFileRegular(String filePath) {
      if (filePath == null) {
         return false;
      }

      synchronized (NATIVE_FS_MONITOR) {
         if (!loadFileSystems()) {
            return false;
         }

         File targetFile = new File(filePath);
         if (!targetFile.exists()) {
            return false;
         }

         boolean isRegular = false;

         try {
            Path path = Paths.get(targetFile.toURI());
            BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
            isRegular = attrs.isRegularFile();
         } catch (Exception var8) {
         }

         return isRegular;
      }
   }

   @Override
   public boolean isSystemTimeReadonly() {
      return isSystemTimeReadonly0();
   }

   @Override
   public int setSystemTime(long millis) {
      return setSystemTime0(millis);
   }

   @Override
   public int setNativeTimeZone(String id) {
      return setNativeTimeZone0(id);
   }

   @Override
   public String getHostId() {
      return this.getHostId(SubscriptionLicenseUtil.getLicenseMode());
   }

   @Override
   public String getHostId(LicenseMode licenseModePreference) {
      return !licenseModePreference.equals(LicenseMode.SUBSCRIPTION) && !this.requireSubscription()
         ? NativePlatformProvider.HostMetaDataHolder.HOST_ID
         : SubscriptionLicenseUtil.getNreId();
   }

   @Override
   public String getHostModel() {
      String model = NativePlatformProvider.HostMetaDataHolder.HOST_MODEL;
      return model != null && model.length() != 0 ? model : "Workstation";
   }

   @Override
   public String getHostModelVersion() {
      return NativePlatformProvider.HostMetaDataHolder.HOST_MODEL_VERSION;
   }

   @Override
   public String getHostProduct() {
      return NativePlatformProvider.HostMetaDataHolder.HOST_PRODUCT;
   }

   @Override
   public String getHostVendor() {
      return NativePlatformProvider.HostMetaDataHolder.HOST_VENDOR;
   }

   @Override
   public String getHostSerialNumber() {
      return NativePlatformProvider.HostMetaDataHolder.HOST_SERIAL_NUMBER;
   }

   @Override
   public String getHostParts() {
      return NativePlatformProvider.HostMetaDataHolder.HOST_PARTS;
   }

   @Override
   public long getTickCount() {
      return getTickCount0();
   }

   @Override
   public long getNanoCount() {
      return getNanoCount0();
   }

   @Override
   public int createWatchdog(String name) {
      return createWatchdog0(name);
   }

   @Override
   public int destroyWatchdog(String name) {
      return destroyWatchdog0(name);
   }

   @Override
   public int getWatchdogCycles(String name) {
      return getWatchdogCycles0(name);
   }

   @Override
   public int getWatchdogPolicy(String name) {
      return getWatchdogPolicy0(name);
   }

   @Override
   public int getWatchdogTimeout(String name) {
      return getWatchdogTimeout0(name);
   }

   @Override
   public int updateWatchdog(String name, int cycles, int policy, int timeout) {
      return updateWatchdog0(name, cycles, policy, timeout);
   }

   @Override
   public boolean canWriteSystemLogMessages() {
      return !loadNativeLogSettings() ? false : canWriteSystemLogMessages;
   }

   @Override
   public boolean enableSystemLogging() {
      if (!loadNativeLogSettings()) {
         return false;
      }

      if (!canWriteSystemLogMessages) {
         return false;
      }

      if (!nativeLoggingEnabled) {
         nativeLoggingEnabled = enableSystemLogging0();
      }

      return nativeLoggingEnabled;
   }

   @Override
   public void log(int level, String message) {
      if (loadNativeLogSettings()) {
         if (canWriteSystemLogMessages) {
            if (nativeLoggingEnabled) {
               log0(level, message);
            }
         }
      }
   }

   @Override
   public boolean canReadSystemLogMessages() {
      return !loadNativeLogSettings() ? false : canReadSystemLogMessages;
   }

   @Override
   public String readSystemLog(String systemLogName) {
      if (!loadNativeLogSettings()) {
         return "";
      } else {
         return !canReadSystemLogMessages ? "" : readSystemLog0();
      }
   }

   @Override
   public long getFreePhysicalMemoryBytes() {
      return getFreePhysicalMemoryBytes0();
   }

   @Override
   public long getTotalPhysicalMemoryBytes() {
      return getTotalPhysicalMemoryBytes0();
   }

   @Override
   public int getCurrentCPUUtilization() {
      return getCurrentCPUUtilization0();
   }

   @Override
   public int getOverallCPUUtilization() {
      return getOverallCPUUtilization0();
   }

   @Override
   public long getIdleTime(int cpuID) {
      return getIdleTime0(cpuID);
   }

   @Override
   public long getCpuTime(String processName, boolean includeChildren) {
      return getCpuTime0(processName, includeChildren);
   }

   @Override
   public String getHostFileName() {
      return NativePlatformProvider.NetworkManagerPropertyHolder.USE_SNAP_NETCONVERT ? SnapNetconvertManager.getHostFileName() : getHostFileName0();
   }

   @Override
   public String getNetworkSettingsXML() {
      return NativePlatformProvider.NetworkManagerPropertyHolder.USE_SNAP_NETCONVERT ? SnapNetconvertManager.getNetworkSettingsXML() : getNetworkSettingsXML0();
   }

   @Override
   public int setNetworkSettingsXML(String networkSettingsXML) {
      return NativePlatformProvider.NetworkManagerPropertyHolder.USE_SNAP_NETCONVERT
         ? SnapNetconvertManager.setNetworkSettingsXML(networkSettingsXML)
         : setNetworkSettingsXML0(networkSettingsXML);
   }

   @Override
   public String[] getAdapterNames() {
      Array<String> adapterNamesArray = new Array<>(String.class);
      String hostSettingsXML = this.getNetworkSettingsXML();
      if (hostSettingsXML != null && !hostSettingsXML.trim().isEmpty()) {
         XElem hostSettings;
         try {
            hostSettings = XParser.make(hostSettingsXML).parse(true);
         } catch (Exception e) {
            return adapterNamesArray.trim();
         }

         if (hostSettings != null) {
            XElem adapterSettingsElem = hostSettings.elem("adapters");
            if (adapterSettingsElem != null) {
               XElem[] adapters = adapterSettingsElem.elems("adapter");
               if (adapters != null) {
                  for (XElem adapter : adapters) {
                     String name = adapter.get("id", null);
                     if (name != null) {
                        adapterNamesArray.add(name);
                     }
                  }
               }
            }
         }
      }

      return adapterNamesArray.trim();
   }

   @Override
   public boolean usesPosixSockets() {
      return usesPosixSockets0();
   }

   @Override
   public boolean isStationPlatformReadonly() {
      return isStationPlatformReadonly0();
   }

   @Override
   public void reboot() {
      reboot0();
   }

   @Override
   public String getTempDirPath() {
      return AccessController.doPrivileged(() -> System.getProperty("java.io.tmpdir"));
   }

   @Override
   public long getProcessId() {
      return getProcessId0();
   }

   @Override
   public void dumpThreads() {
      dumpThreads0();
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
      return allowPlatformDaemonRestart0();
   }

   @Override
   public int restartPlatformDaemon() {
      return !this.allowPlatformDaemonRestart() ? -1 : restartPlatformDaemon0();
   }

   @Override
   public boolean platformDaemonShutdownRequested() {
      return platformDaemonShutdownRequested0();
   }

   @Override
   public String getSupportedAuthenticationTypes() {
      return getSupportedAuthenticationTypes0();
   }

   @Override
   public boolean isAuthenticationReadonly() {
      return isAuthenticationReadonly0();
   }

   @Override
   public String getDefaultUsername() {
      return getDefaultUsername0();
   }

   @Override
   public String getDefaultPassword() {
      return getDefaultPassword0();
   }

   @Override
   public boolean synchronizeUsers(String userName, String password) {
      return synchronizeUsers0(userName, password);
   }

   @Override
   public String getComputerName() {
      return getComputerName0();
   }

   @Override
   public String getComputerDomain(boolean fullyQualified) {
      return getComputerDomain0(fullyQualified);
   }

   @Override
   public boolean isGroupMember(String userId, String groupId) {
      return isGroupMember0(userId, groupId);
   }

   @Override
   public String getDefaultAdminGroupName() {
      return getDefaultAdminGroupName0();
   }

   @Override
   public boolean isPasswordValid(String userId, String password) {
      return isPasswordValid0(userId, password);
   }

   @Override
   public String getDomainGroupsXml(String idString) {
      return getDomainGroupsXml0(idString);
   }

   @Override
   public String getAccountXml(String idString, boolean userAccount) {
      return getAccountXml0(idString, userAccount);
   }

   @Override
   public String getIdFromName(String name, boolean userAccount) {
      return getIdFromName0(name, userAccount);
   }

   @Override
   public String getNameFromId(String id, boolean userAccount) {
      return getNameFromId0(id, userAccount);
   }

   @Override
   public NativeAccount getAccountFromName(String accountName, String defaultDomain, boolean userAccount) {
      if (!NativeAccount.isAccountQualifierValid(accountName)) {
         return null;
      }

      String fullyQualifiedName;
      if (!NativeAccount.isAccountNameFullyQualified(accountName)) {
         if (!NativeAccount.isAccountQualifierValid(defaultDomain)) {
            return null;
         }

         fullyQualifiedName = defaultDomain + '\\' + accountName;
      } else {
         fullyQualifiedName = accountName;
      }

      String platformIdentifier = getIdFromName0(fullyQualifiedName, userAccount);
      if (platformIdentifier == null) {
         return null;
      } else {
         return userAccount ? new UserAccount(fullyQualifiedName, platformIdentifier) : new GroupAccount(fullyQualifiedName, platformIdentifier);
      }
   }

   @Override
   public NativeAccount getAccountFromId(String platformIdentifier, boolean userAccount) {
      if (platformIdentifier == null) {
         return null;
      } else if (platformIdentifier.isEmpty()) {
         return null;
      } else {
         String accountName = getNameFromId0(platformIdentifier, userAccount);
         if (!NativeAccount.isAccountQualifierValid(accountName)) {
            return null;
         } else {
            return userAccount ? new UserAccount(accountName, platformIdentifier) : new GroupAccount(accountName, platformIdentifier);
         }
      }
   }

   @Override
   public GroupAccount[] getAccounts(String accountIdList, char delimiter) {
      if (accountIdList == null) {
         return null;
      }

      Array<GroupAccount> accounts = new Array<>(GroupAccount.class);
      String[] idList = TextUtil.split(accountIdList, delimiter);

      for (String id : idList) {
         GroupAccount current = (GroupAccount)this.getAccountFromId(id, false);
         if (current != null) {
            accounts.add(current);
         }
      }

      return accounts.trim();
   }

   @Override
   public GroupAccount getDefaultAdminGroup() {
      String defaultAdminGroupName = this.getDefaultAdminGroupName();
      if (!NativeAccount.isAccountQualifierValid(defaultAdminGroupName)) {
         return null;
      }

      String defaultDomain = this.getComputerDomain(true) == null ? this.getComputerName() : this.getComputerDomain(true);
      return (GroupAccount)this.getAccountFromName(defaultAdminGroupName, defaultDomain, false);
   }

   @Override
   public UserAccount getAccountFromCredentials(String username, String password, boolean defaultLocal) {
      if (!NativeAccount.isAccountQualifierValid(username)) {
         return null;
      }

      if (password == null) {
         return null;
      }

      UserAccount result = null;
      if (NativeAccount.isAccountNameFullyQualified(username)) {
         UserAccount candidate = (UserAccount)this.getAccountFromName(username, null, true);
         if (candidate != null && candidate.isPasswordValid(this, password)) {
            result = candidate;
         }
      } else {
         String domain1;
         String domain2;
         String domain3;
         if (defaultLocal) {
            domain1 = this.getComputerName();
            domain2 = this.getComputerDomain(true);
            domain3 = this.getComputerDomain(false);
         } else {
            domain1 = this.getComputerDomain(true);
            domain2 = this.getComputerDomain(false);
            domain3 = this.getComputerName();
         }

         if (domain1 != null) {
            UserAccount candidate = (UserAccount)this.getAccountFromName(username, domain1, true);
            if (candidate != null && candidate.isPasswordValid(this, password)) {
               result = candidate;
            }
         }

         if (result == null && domain2 != null && !domain2.equalsIgnoreCase(domain1)) {
            UserAccount candidate = (UserAccount)this.getAccountFromName(username, domain2, true);
            if (candidate != null && candidate.isPasswordValid(this, password)) {
               result = candidate;
            }
         }

         if (result == null && domain3 != null && !domain3.equalsIgnoreCase(domain1) && !domain3.equalsIgnoreCase(domain2)) {
            UserAccount candidate = (UserAccount)this.getAccountFromName(username, domain3, true);
            if (candidate != null && candidate.isPasswordValid(this, password)) {
               result = candidate;
            }
         }
      }

      return result;
   }

   @Override
   public String getPasswordHash(String userName) {
      return getPasswordHash0(userName);
   }

   @Override
   public boolean providesAccountManagement() {
      return !NativePlatformProvider.UserManagerPropertyHolder.USE_QNX_USERMGR && !NativePlatformProvider.UserManagerPropertyHolder.USE_SNAP_USERMGR
         ? providesAccountManagement0()
         : true;
   }

   @Override
   public String addUserAccount(String username, String password, String comment, boolean passwordHashed) {
      if (NativePlatformProvider.UserManagerPropertyHolder.USE_QNX_USERMGR) {
         return QnxUserManager.addUserAccount(username, password, comment, passwordHashed);
      } else {
         return NativePlatformProvider.UserManagerPropertyHolder.USE_SNAP_USERMGR
            ? SnapUserManager.addUserAccount(username, password, comment, passwordHashed)
            : addUserAccount0(username, password, comment, passwordHashed);
      }
   }

   @Override
   public boolean removeUserAccount(String userId) {
      if (NativePlatformProvider.UserManagerPropertyHolder.USE_QNX_USERMGR) {
         return QnxUserManager.removeUserAccount(userId);
      } else {
         return NativePlatformProvider.UserManagerPropertyHolder.USE_SNAP_USERMGR ? SnapUserManager.removeUserAccount(userId) : removeUserAccount0(userId);
      }
   }

   @Override
   public boolean addUserToGroup(String userId, String groupId) {
      if (NativePlatformProvider.UserManagerPropertyHolder.USE_QNX_USERMGR) {
         return QnxUserManager.addUserToGroup(userId, groupId);
      } else {
         return NativePlatformProvider.UserManagerPropertyHolder.USE_SNAP_USERMGR
            ? SnapUserManager.addUserToGroup(userId, groupId)
            : addUserToGroup0(userId, groupId);
      }
   }

   @Override
   public boolean removeUserFromGroup(String userId, String groupId) {
      if (NativePlatformProvider.UserManagerPropertyHolder.USE_QNX_USERMGR) {
         return QnxUserManager.removeUserFromGroup(userId, groupId);
      } else {
         return NativePlatformProvider.UserManagerPropertyHolder.USE_SNAP_USERMGR
            ? SnapUserManager.removeUserFromGroup(userId, groupId)
            : removeUserFromGroup0(userId, groupId);
      }
   }

   @Override
   public boolean changeUserPassword(String userId, String oldPassword, String newPassword) {
      if (NativePlatformProvider.UserManagerPropertyHolder.USE_QNX_USERMGR) {
         return QnxUserManager.changeUserPassword(userId, oldPassword, newPassword);
      } else {
         return NativePlatformProvider.UserManagerPropertyHolder.USE_SNAP_USERMGR
            ? SnapUserManager.changeUserPassword(userId, oldPassword, newPassword)
            : changeUserPassword0(userId, oldPassword, newPassword);
      }
   }

   @Override
   public String getAltDatabasePath() {
      return NIAGARA_ALTERNATIVE_DATABASE_PATH;
   }

   @Override
   public String getAltArchivePath() {
      return NIAGARA_ALTERNATIVE_ARCHIVE_PATH;
   }

   @Override
   public String getAltArchiveZipPath() {
      return NIAGARA_ALTERNATIVE_ARCHIVE_ZIP_PATH;
   }

   @Override
   public int getArchiveBackupCount() {
      return getArchiveBackupCount0();
   }

   @Override
   public boolean supportsNREConfiguration() {
      return NativePlatformProvider.NREConfigurationDataHolder.SUPPORTED;
   }

   @Override
   public NREMemoryPool getNRESystemReserveMemoryPool() {
      return NativePlatformProvider.NREConfigurationDataHolder.SYSTEM_RESERVE;
   }

   @Override
   public NREMemoryPool getNREHeapMemoryPool() {
      return NativePlatformProvider.NREConfigurationDataHolder.HEAP_SPACE;
   }

   @Override
   public NREMemoryPool getNRECodeCacheMemoryPool() {
      return NativePlatformProvider.NREConfigurationDataHolder.CODE_CACHE;
   }

   @Override
   public NREMemoryPool getNREMetaSpaceMemoryPool() {
      return NativePlatformProvider.NREConfigurationDataHolder.META_SPACE;
   }

   @Override
   public NREMemoryPool getNRERamDiskMemoryPool() {
      return NativePlatformProvider.NREConfigurationDataHolder.RAM_DISK;
   }

   @Override
   public boolean isSystemPasswordReadonly() {
      return isSystemPasswordReadonly0();
   }

   @Override
   public SecretChars getSystemPassword() {
      SystemPassphrase.checkSystemPassphrasePermissions();
      SecretChars platformPassword = null;

      try {
         if (SystemPassphrase.usingSimpleSystemPassphrase()) {
            byte[] platformPasswordBytes = AccessController.doPrivileged(
               () -> SimpleKeyValueUtil.getInstance(SecurityInitializer.getInstance().getSecurityInfoProvider().getSecurityDir().getPath()).get(".sp")
            );
            if (platformPasswordBytes != null && platformPasswordBytes.length > 0) {
               platformPassword = SecretChars.fromSecretBytes(new SecretBytes(platformPasswordBytes, true));
            }
         } else if (SystemPassphrase.usingNativeSystemPassphrase()) {
            String platformPasswordString = AccessController.doPrivileged(NativePlatformProvider::getSystemPassword0);
            if (platformPasswordString != null && platformPasswordString.length() > 0) {
               platformPassword = SecretChars.fromString(platformPasswordString);
            }
         } else {
            byte[] platformPasswordBytes = AccessController.doPrivileged(() -> SimpleKeyValueUtil.getInstance("/etc/niagara").get(".sp"));
            if (platformPasswordBytes != null && platformPasswordBytes.length > 0) {
               platformPassword = SecretChars.fromSecretBytes(new SecretBytes(platformPasswordBytes, true));
            }
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
         boolean success;
         if (SystemPassphrase.usingSimpleSystemPassphrase()) {
            success = AccessController.doPrivileged(
               () -> SimpleKeyValueUtil.getInstance(SecurityInitializer.getInstance().getSecurityInfoProvider().getSecurityDir().getPath())
                  .set(".sp", newPassword.getBytes(StandardCharsets.UTF_8))
            );
         } else if (SystemPassphrase.usingNativeSystemPassphrase()) {
            success = AccessController.doPrivileged(() -> setSystemPassword0(oldPassword, newPassword));
         } else {
            byte[] newPasswordBytes = newPassword.getBytes(StandardCharsets.UTF_8);
            success = AccessController.doPrivileged(() -> SimpleKeyValueUtil.getInstance("/etc/niagara").set(".sp", newPasswordBytes));
         }

         return success;
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
      boolean useNativeKeyMaterial = KeyMaterial.usingNativeKeyMaterial();
      boolean useNativeSystemPassphrase = SystemPassphrase.usingNativeSystemPassphrase();

      try {
         File oldConfigurationFile = new File("/etc/niagara/niagara.conf");
         if ((!useNativeKeyMaterial || !useNativeSystemPassphrase) && oldConfigurationFile.exists()) {
            Properties niagaraConfProperties = new Properties();

            try (BufferedReader reader = new BufferedReader(new FileReader(oldConfigurationFile))) {
               niagaraConfProperties.load(reader);
            }

            if (niagaraConfProperties.size() == 0) {
               oldConfigurationFile.deleteOnExit();
            } else {
               if (!useNativeKeyMaterial) {
                  String kmValue = niagaraConfProperties.getProperty(".km", null);
                  if (kmValue != null) {
                     byte[] existingKeyMaterial = this.getKeyMaterial(securityDirectory, keyName);
                     if ((existingKeyMaterial == null || existingKeyMaterial.length == 0)
                        && !this.setKeyMaterial(securityDirectory, keyName, kmValue.getBytes(StandardCharsets.UTF_8))) {
                        throw new Exception("Could not set key material during migration");
                     }
                  }
               }

               if (!useNativeSystemPassphrase) {
                  String systemPwValue = niagaraConfProperties.getProperty("systempw", null);
                  if (systemPwValue != null) {
                     String existingSystemPassphrase = this.getSystemPassword().asString(true);
                     if ((existingSystemPassphrase == null || this.getDefaultPassword().equals(existingSystemPassphrase))
                        && !this.setSystemPassword(existingSystemPassphrase, systemPwValue)) {
                        throw new Exception("Could not set system passphrase during migration");
                     }
                  }
               }

               if (PlatformUtil.isNpsdkPlatform()) {
                  oldConfigurationFile.deleteOnExit();
               }
            }
         }

         return !useNativeKeyMaterial && !useNativeSystemPassphrase ? true : checkForKeyMaterialUpgrade0();
      } catch (Exception e) {
         throw new RuntimeException("Error occurred during migration", e);
      }
   }

   @Override
   public boolean supportsKeyMaterialRecovery() {
      if (KeyMaterial.usingSimpleKeyMaterial()) {
         return false;
      } else {
         return KeyMaterial.usingNativeKeyMaterial() ? supportsKeyMaterialRecovery0() : true;
      }
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
         byte[] keyMaterial;
         if (KeyMaterial.usingSimpleKeyMaterial()) {
            keyMaterial = AccessController.doPrivileged(() -> SimpleKeyValueUtil.getInstance(securityDirectory.getPath()).get(keyName));
         } else if (KeyMaterial.usingNativeKeyMaterial()) {
            keyMaterial = AccessController.doPrivileged(() -> getKeyMaterial0(keyName));
         } else {
            byte[] hexStringBytes = AccessController.doPrivileged(() -> SimpleKeyValueUtil.getInstance("/etc/niagara").get(keyName));
            keyMaterial = hexStringBytes != null ? ByteArrayUtil.hexStringToBytes(new String(hexStringBytes, StandardCharsets.UTF_8)) : null;
         }

         return keyMaterial;
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
         boolean success;
         if (KeyMaterial.usingSimpleKeyMaterial()) {
            success = AccessController.doPrivileged(() -> SimpleKeyValueUtil.getInstance(securityDirectory.getPath()).set(keyName, keyMaterial));
         } else if (KeyMaterial.usingNativeKeyMaterial()) {
            success = AccessController.doPrivileged(() -> setKeyMaterial0(keyName, keyMaterial));
         } else {
            byte[] keyMaterialBytes = keyMaterial == null ? null : ByteArrayUtil.toHexString(keyMaterial).getBytes(StandardCharsets.UTF_8);
            success = AccessController.doPrivileged(() -> SimpleKeyValueUtil.getInstance("/etc/niagara").set(keyName, keyMaterialBytes));
         }

         return success;
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
      } else if (securityDirectory.getPath().length() > 4096) {
         return 0L;
      } else if (keyName == null) {
         return 0L;
      } else if (keyName.length() > 4096) {
         return 0L;
      } else {
         KeyMaterial.checkKeyMaterialPermissions();
         if (KeyMaterial.usingSimpleKeyMaterial()) {
            return AccessController.doPrivileged(() -> new File(securityDirectory, keyName).lastModified());
         } else {
            return KeyMaterial.usingNativeKeyMaterial()
               ? getKeyMaterialLastModified0(keyName)
               : AccessController.doPrivileged(() -> new File("/etc/niagara", keyName).lastModified());
         }
      }
   }

   private static boolean loadFileSystems() {
      if (!fileSystemsLoaded) {
         fileSystemNames = getAllFileSystemNames0();
         fileSystemInfoByName = new HashMap<>();

         for (String fileSystemName : fileSystemNames) {
            File fileSystemFile = new File(fileSystemName);
            String fileSystemDisplayName = getFileSystemDisplayName0(fileSystemName);
            long totalBytes = AccessController.doPrivileged(fileSystemFile::getTotalSpace);
            long maxFileCount = getMaxFileCount0(fileSystemName);
            NativePlatformProvider.FileSystemInfo fileSystemInfo = new NativePlatformProvider.FileSystemInfo(
               fileSystemDisplayName, fileSystemFile, totalBytes, maxFileCount
            );
            fileSystemInfoByName.put(fileSystemName, fileSystemInfo);
         }

         fileSystemsLoaded = true;
      }

      return true;
   }

   private static boolean loadNativeLogSettings() {
      if (!logSettingsLoaded) {
         canWriteSystemLogMessages = canWriteSystemLogMessages0();
         canReadSystemLogMessages = canReadSystemLogMessages0();
         logSettingsLoaded = true;
      }

      return true;
   }

   private static native String getOsName0();

   private static native String getOsVersion0();

   private static native String getOsDescription0();

   private static native String getOsArchitecture0();

   private static native boolean isOsInstallable0();

   private static native String getHostId0();

   private static native String getHostModel0();

   private static native String getHostModelVersion0();

   private static native String getHostProduct0();

   private static native String getHostVendor0();

   private static native String getHostSerialNumber0();

   private static native String getHostParts0();

   private static native long getTickCount0();

   private static native long getNanoCount0();

   private static native boolean getAllowStationRestartDefault0();

   private static native boolean allowPlatformDaemonRestart0();

   private static native boolean getAllowBrandChangeDefault0();

   private static native boolean isLicenseReadonly0();

   private static native boolean isNiagaraHomeReadonly0();

   private static native int restartPlatformDaemon0();

   private static native boolean platformDaemonShutdownRequested0();

   private static native boolean isSoftwareReadonly0();

   private static native boolean isStationPlatformReadonly0();

   private static native boolean isEmbedded0();

   private static native boolean isDaemonDebugSupported0();

   private static native boolean requireSecurePlatform0();

   private static native boolean notifyApplicationStatus0(String var0, String var1, int var2);

   private static native boolean isSSHSupported0();

   private static native int getSSHPort0();

   private static native boolean setSSHPort0(int var0);

   private static native boolean supportsNativeDiagnostics0();

   private static native String[][] getNativeDiagnosticsCommands0();

   private static native String executeNativeDiagnosticsCommand0(int var0);

   private static native String[] getAllFileSystemNames0();

   private static native String getFileSystemDisplayName0(String var0);

   private static native long getMaxFileCount0(String var0);

   private static native long getCurrentFileCount0(String var0);

   private static native long getMaxOpenFileDescriptorCount0();

   private static native long getCurrentOpenFileDescriptorCount0();

   private static native boolean isSystemTimeReadonly0();

   private static native int setSystemTime0(long var0);

   private static native int setNativeTimeZone0(String var0);

   private static native long getFreePhysicalMemoryBytes0();

   private static native long getTotalPhysicalMemoryBytes0();

   private static native int getCurrentCPUUtilization0();

   private static native int getOverallCPUUtilization0();

   private static native long getIdleTime0(int var0);

   private static native long getCpuTime0(String var0, boolean var1);

   private static native int createWatchdog0(String var0);

   private static native int destroyWatchdog0(String var0);

   private static native int getWatchdogCycles0(String var0);

   private static native int getWatchdogPolicy0(String var0);

   private static native int getWatchdogTimeout0(String var0);

   private static native int updateWatchdog0(String var0, int var1, int var2, int var3);

   private static native boolean canWriteSystemLogMessages0();

   private static native boolean enableSystemLogging0();

   private static native void log0(int var0, String var1);

   private static native boolean canReadSystemLogMessages0();

   private static native String readSystemLog0();

   private static native String getHostFileName0();

   private static native int setNetworkSettingsXML0(String var0);

   private static native String getNetworkSettingsXML0();

   private static native boolean usesPosixSockets0();

   private static native void reboot0();

   private static native long getProcessId0();

   private static native void dumpThreads0();

   private static native String getSupportedAuthenticationTypes0();

   private static native boolean isAuthenticationReadonly0();

   private static native String getDefaultUsername0();

   private static native String getDefaultPassword0();

   private static native boolean synchronizeUsers0(String var0, String var1);

   private static native String getComputerName0();

   private static native String getComputerDomain0(boolean var0);

   private static native boolean isGroupMember0(String var0, String var1);

   private static native String getDefaultAdminGroupName0();

   private static native boolean isPasswordValid0(String var0, String var1);

   private static native String getNameFromId0(String var0, boolean var1);

   private static native String getIdFromName0(String var0, boolean var1);

   private static native String getDomainGroupsXml0(String var0);

   private static native String getPasswordHash0(String var0);

   private static native boolean providesAccountManagement0();

   private static native String getAccountXml0(String var0, boolean var1);

   private static native String addUserAccount0(String var0, String var1, String var2, boolean var3);

   private static native boolean removeUserAccount0(String var0);

   private static native boolean addUserToGroup0(String var0, String var1);

   private static native boolean removeUserFromGroup0(String var0, String var1);

   private static native boolean changeUserPassword0(String var0, String var1, String var2);

   private static native int getArchiveBackupCount0();

   private static native boolean supportsNREConfiguration0();

   private static native int[] getNRESystemReserveMemoryPool0();

   private static native int[] getNREHeapMemoryPool0();

   private static native int[] getNRECodeCacheMemoryPool0();

   private static native int[] getNREMetaSpaceMemoryPool0();

   private static native int[] getNRERamDiskMemoryPool0();

   private static native boolean isSystemPasswordReadonly0();

   private static native String getSystemPassword0();

   private static native boolean setSystemPassword0(String var0, String var1);

   private static native boolean checkForKeyMaterialUpgrade0();

   private static native boolean supportsKeyMaterialRecovery0();

   private static native byte[] getKeyMaterial0(String var0);

   private static native boolean setKeyMaterial0(String var0, byte[] var1);

   private static native long getKeyMaterialLastModified0(String var0);

   private static final class FileSystemInfo {
      private final String fileSystemDisplayName;
      private final File fileSystemFile;
      private final long totalBytes;
      private final long maxFileCount;

      private FileSystemInfo(String newFileSystemDisplayName, File newFileSystemFile, long newTotalBytes, long newMaxFileCount) {
         this.fileSystemDisplayName = newFileSystemDisplayName;
         this.fileSystemFile = newFileSystemFile;
         this.totalBytes = newTotalBytes;
         this.maxFileCount = newMaxFileCount;
      }
   }

   private static class HostMetaDataHolder {
      private static final String HOST_ID;
      private static final String HOST_MODEL;
      private static final String HOST_MODEL_VERSION;
      private static final String HOST_PRODUCT;
      private static final String HOST_VENDOR;
      private static final String HOST_SERIAL_NUMBER;
      private static final String HOST_PARTS;

      static {
         try {
            HOST_ID = NativePlatformProvider.getHostId0();
            HOST_MODEL = NativePlatformProvider.getHostModel0();
            HOST_MODEL_VERSION = NativePlatformProvider.getHostModelVersion0();
            HOST_PRODUCT = NativePlatformProvider.getHostProduct0();
            HOST_VENDOR = NativePlatformProvider.getHostVendor0();
            HOST_SERIAL_NUMBER = NativePlatformProvider.getHostSerialNumber0();
            HOST_PARTS = NativePlatformProvider.getHostParts0();
         } catch (Throwable t) {
            System.err.println("SEVERE [" + new Date() + "][nre] failed to initialize host metadata: " + t);
            throw t;
         }
      }
   }

   private static class NREConfigurationDataHolder {
      private static final boolean SUPPORTED;
      private static final NREMemoryPool SYSTEM_RESERVE;
      private static final NREMemoryPool HEAP_SPACE;
      private static final NREMemoryPool CODE_CACHE;
      private static final NREMemoryPool META_SPACE;
      private static final NREMemoryPool RAM_DISK;

      static {
         try {
            SUPPORTED = NativePlatformProvider.supportsNREConfiguration0();
            int[] systemReserveSizes = NativePlatformProvider.getNRESystemReserveMemoryPool0();
            SYSTEM_RESERVE = new NREMemoryPool(systemReserveSizes[0], systemReserveSizes[1], systemReserveSizes[2]);
            int[] heapSpaceSizes = NativePlatformProvider.getNREHeapMemoryPool0();
            HEAP_SPACE = new NREMemoryPool(heapSpaceSizes[0], heapSpaceSizes[1], heapSpaceSizes[2]);
            int[] codeCacheSizes = NativePlatformProvider.getNRECodeCacheMemoryPool0();
            CODE_CACHE = new NREMemoryPool(codeCacheSizes[0], codeCacheSizes[1], codeCacheSizes[2]);
            int[] metaSpaceSizes = NativePlatformProvider.getNREMetaSpaceMemoryPool0();
            META_SPACE = new NREMemoryPool(metaSpaceSizes[0], metaSpaceSizes[1], metaSpaceSizes[2]);
            int[] ramDiskSizes = NativePlatformProvider.getNRERamDiskMemoryPool0();
            RAM_DISK = new NREMemoryPool(ramDiskSizes[0], ramDiskSizes[1], ramDiskSizes[2]);
         } catch (Throwable t) {
            System.err.println("SEVERE [" + new Date() + "][nre] failed to initialize nreconfig: " + t);
            throw t;
         }
      }
   }

   private static class NetworkManagerPropertyHolder {
      private static final boolean USE_SNAP_NETCONVERT = AccessController.doPrivileged(() -> Boolean.getBoolean("niagara.use.snap.netconvert"));
   }

   private static class UserManagerPropertyHolder {
      private static final boolean USE_QNX_USERMGR = AccessController.doPrivileged(() -> Boolean.getBoolean("niagara.use.qnx.usermgr"));
      private static final boolean USE_SNAP_USERMGR = AccessController.doPrivileged(() -> Boolean.getBoolean("niagara.use.snap.usermgr"));
   }
}
