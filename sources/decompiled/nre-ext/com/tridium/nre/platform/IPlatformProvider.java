package com.tridium.nre.platform;

import com.tridium.nre.auth.GroupAccount;
import com.tridium.nre.auth.NativeAccount;
import com.tridium.nre.auth.UserAccount;
import com.tridium.nre.security.SecretChars;
import com.tridium.nre.util.LicenseMode;
import java.io.File;

public interface IPlatformProvider {
   String getOsName();

   String getOsVersion();

   String getOsDescription();

   String getOsArchitecture();

   boolean isOsInstallable();

   String getHostId();

   String getHostId(LicenseMode var1);

   String getHostModel();

   String getHostModelVersion();

   String getHostProduct();

   String getHostVendor();

   String getHostSerialNumber();

   String getHostParts();

   boolean getAllowStationRestartDefault();

   boolean allowPlatformDaemonRestart();

   int restartPlatformDaemon();

   boolean platformDaemonShutdownRequested();

   boolean getAllowBrandChangeDefault();

   boolean isLicenseReadonly();

   String getTempDirPath();

   boolean isSoftwareReadonly();

   boolean isNiagaraHomeReadonly();

   String getSupportedRuntimeProfiles();

   String getRequiredRuntimeProfiles();

   boolean isStationPlatformReadonly();

   boolean isEmbedded();

   boolean isDaemonDebugSupported();

   boolean requireSecurePlatform();

   boolean requireSubscription();

   boolean notifyApplicationStatus(String var1, String var2, int var3);

   boolean isSSHSupported();

   int getSSHPort();

   boolean setSSHPort(int var1);

   boolean supportsNativeDiagnostics();

   String[][] getNativeDiagnosticsCommands();

   String executeNativeDiagnosticsCommand(int var1);

   String[] getAllFileSystemNames();

   String getFileSystemName(String var1);

   String getFileSystemDisplayName(String var1);

   long getFreeBytes(String var1);

   long getTotalBytes(String var1);

   long getMaxFileCount(String var1);

   long getCurrentFileCount(String var1);

   long getMaxOpenFileDescriptorCount();

   long getCurrentOpenFileDescriptorCount();

   boolean isFileRegular(String var1);

   int lockFile(String var1);

   int unlockFile(String var1, int var2);

   boolean isSystemTimeReadonly();

   int setNativeTimeZone(String var1);

   int setSystemTime(long var1);

   long getFreePhysicalMemoryBytes();

   long getTotalPhysicalMemoryBytes();

   int getCurrentCPUUtilization();

   int getOverallCPUUtilization();

   long getIdleTime(int var1);

   long getCpuTime(String var1, boolean var2);

   long getTickCount();

   long getNanoCount();

   int createWatchdog(String var1);

   int destroyWatchdog(String var1);

   int getWatchdogCycles(String var1);

   int getWatchdogPolicy(String var1);

   int getWatchdogTimeout(String var1);

   int updateWatchdog(String var1, int var2, int var3, int var4);

   boolean canWriteSystemLogMessages();

   boolean enableSystemLogging();

   void log(int var1, String var2);

   boolean canReadSystemLogMessages();

   String readSystemLog(String var1);

   String getHostFileName();

   String getNetworkSettingsXML();

   int setNetworkSettingsXML(String var1);

   String[] getAdapterNames();

   boolean usesPosixSockets();

   void reboot();

   long getProcessId();

   void dumpThreads();

   String getSupportedAuthenticationTypes();

   boolean isAuthenticationReadonly();

   String getDefaultUsername();

   String getDefaultPassword();

   boolean synchronizeUsers(String var1, String var2);

   String getComputerName();

   String getComputerDomain(boolean var1);

   String getIdFromName(String var1, boolean var2);

   String getNameFromId(String var1, boolean var2);

   String getDefaultAdminGroupName();

   boolean isGroupMember(String var1, String var2);

   boolean isPasswordValid(String var1, String var2);

   String getDomainGroupsXml(String var1);

   String getPasswordHash(String var1);

   NativeAccount getAccountFromName(String var1, String var2, boolean var3);

   NativeAccount getAccountFromId(String var1, boolean var2);

   GroupAccount getDefaultAdminGroup();

   GroupAccount[] getAccounts(String var1, char var2);

   UserAccount getAccountFromCredentials(String var1, String var2, boolean var3);

   boolean providesAccountManagement();

   String getAccountXml(String var1, boolean var2);

   String addUserAccount(String var1, String var2, String var3, boolean var4);

   boolean removeUserAccount(String var1);

   boolean addUserToGroup(String var1, String var2);

   boolean removeUserFromGroup(String var1, String var2);

   boolean changeUserPassword(String var1, String var2, String var3);

   boolean isSystemPasswordReadonly();

   SecretChars getSystemPassword();

   boolean setSystemPassword(String var1, String var2);

   boolean checkForKeyMaterialUpgrade(File var1, String var2);

   boolean supportsKeyMaterialRecovery();

   byte[] getKeyMaterial(File var1, String var2);

   boolean setKeyMaterial(File var1, String var2, byte[] var3);

   long getKeyMaterialLastModified(File var1, String var2);

   String getAltDatabasePath();

   String getAltArchivePath();

   String getAltArchiveZipPath();

   int getArchiveBackupCount();

   boolean supportsNREConfiguration();

   NREMemoryPool getNRESystemReserveMemoryPool();

   NREMemoryPool getNREHeapMemoryPool();

   NREMemoryPool getNRECodeCacheMemoryPool();

   NREMemoryPool getNREMetaSpaceMemoryPool();

   NREMemoryPool getNRERamDiskMemoryPool();
}
