package com.tridium.nre.platform;

import com.tridium.nre.security.SecretBytes;
import com.tridium.nre.security.SecretChars;
import com.tridium.nre.security.SecurityInitializer;
import com.tridium.nre.security.SystemPassphrase;
import com.tridium.nre.security.km.KeyMaterial;
import com.tridium.nre.util.DpapiUtil;
import com.tridium.nre.util.RegistryUtil;
import com.tridium.nre.util.SimpleKeyValueUtil;
import com.tridium.nre.util.SyspwUtil;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Set;
import java.util.TimeZone;
import java.util.logging.Level;
import javax.baja.nre.util.ByteArrayUtil;
import javax.baja.nre.util.ByteBuffer;
import javax.baja.nre.util.TextUtil;
import javax.baja.xml.XElem;
import javax.baja.xml.XWriter;

public final class NativePlatformProviderTridium extends NativePlatformProvider {
   private static final String SSHD_PATH = "/etc/ssh/sshd";
   private static final String SSHD_SCRIPT = "/proc/boot/sushi sshdinit.sh";
   private String timezoneFilename = null;

   NativePlatformProviderTridium() {
   }

   @Override
   public String getHostSerialNumber() {
      String serialNumber = "";
      if (OperatingSystemEnum.isOS(OperatingSystemEnum.qnx)) {
         serialNumber = getQNXSysInfoString("/sys/info/serialnum", "N/A");
      }

      return serialNumber;
   }

   @Override
   public String getHostParts() {
      XElem extraPartsElem = new XElem("parts");
      if (OperatingSystemEnum.isOS(OperatingSystemEnum.qnx)) {
         XElem elem = new XElem("part");
         elem.setAttr("name", getQNXSysInfoString("/sys/info/imagename", "???"));
         elem.setAttr("version", getQNXSysInfoString("/sys/info/sysrev", "1.0"));
         elem.setAttr("description", getQNXSysInfoString("/sys/info/boardtype", "???") + " system board");
         extraPartsElem.addContent(elem);
         if (getQNXSysInfoString("/sys/info/maintosver", null) != null) {
            elem = new XElem("part");
            String partName = getQNXSysInfoString("/sys/info/osprefix", "???") + "-" + getQNXSysInfoString("/sys/info/imagename", "???") + "-maint";
            elem.setAttr("name", partName);
            elem.setAttr("version", getQNXSysInfoString("/sys/info/maintosver", "1.0"));
            elem.setAttr("description", getQNXSysInfoString("/sys/info/boardtype", "???") + " maintenance OS image");
            elem.setAttr("installable", "true");
            extraPartsElem.addContent(elem);
         }

         if (getQNXSysInfoString("/sys/info/factoryfsver", null) != null) {
            elem = new XElem("part");
            String partName = getQNXSysInfoString("/sys/info/osprefix", "???") + "-" + getQNXSysInfoString("/sys/info/imagename", "???") + "-factory";
            elem.setAttr("name", partName);
            elem.setAttr("version", getQNXSysInfoString("/sys/info/factoryfsver", "1.0"));
            elem.setAttr("description", getQNXSysInfoString("/sys/info/boardtype", "???") + " factory filesystem image");
            elem.setAttr("installable", "true");
            extraPartsElem.addContent(elem);
         }
      }

      String xmlExtraPartsString;
      try {
         ByteBuffer responseBuffer = new ByteBuffer();
         XWriter xmlWriter = new XWriter(responseBuffer.getOutputStream());
         extraPartsElem.write(xmlWriter);
         xmlWriter.flush();
         xmlExtraPartsString = new String(responseBuffer.toByteArray(), StandardCharsets.UTF_8);
      } catch (Exception e) {
         System.err.println("SEVERE [" + new Date() + "][nre] failed to create extra parts XML structure, returning empty parts list (" + e + ")");
         xmlExtraPartsString = "<parts/>";
      }

      return xmlExtraPartsString;
   }

   private static String getQNXSysInfoString(String path, String defaultValue) {
      File file = new File(path);
      String returnValue = defaultValue;
      if (file.exists()) {
         try (BufferedReader fin = new BufferedReader(new FileReader(file))) {
            returnValue = fin.readLine();
            if (returnValue == null) {
               returnValue = defaultValue;
            } else if (returnValue.equalsIgnoreCase("unknown")) {
               returnValue = defaultValue;
            }
         } catch (IOException ioe) {
            returnValue = defaultValue;
         }
      }

      return returnValue;
   }

   @Override
   public boolean getAllowStationRestartDefault() {
      return true;
   }

   @Override
   public int setNativeTimeZone(String id) {
      if (id == null) {
         return -1;
      }

      if (OperatingSystemEnum.isOS(OperatingSystemEnum.qnx)) {
         if (this.timezoneFilename == null) {
            String javaVmVendor = AccessController.doPrivileged(() -> System.getProperty("java.vm.vendor"));
            if (javaVmVendor.equals("Azul Systems, Inc.")) {
               this.timezoneFilename = "/etc/timezone";
            } else {
               this.timezoneFilename = "/etc/JAVATIMEZONE";
            }
         }

         return !TimeZone.getTimeZone(id).getID().equals(id) ? -1 : AccessController.doPrivileged(() -> {
            try (FileWriter writer = new FileWriter(this.timezoneFilename)) {
               writer.write(id);
               writer.flush();
            } catch (IOException ioe) {
               System.err.println("SEVERE [" + new Date() + "][nre] failed to call setNativeTimeZone(" + id + ") (" + ioe + ")");
               return -1;
            }

            TimeZone.setDefault(TimeZone.getTimeZone(id));
            return 0;
         });
      } else {
         return -1;
      }
   }

   @Override
   public boolean isSSHSupported() {
      return OperatingSystemEnum.isOS(OperatingSystemEnum.qnx);
   }

   @Override
   public int getSSHPort() {
      int sshdPort = -1;

      try (BufferedReader in = new BufferedReader(new FileReader("/etc/ssh/sshd"))) {
         String value = in.readLine();
         if (value != null) {
            sshdPort = Integer.parseInt(value);
         }
      } catch (Exception var15) {
      }

      return sshdPort;
   }

   @Override
   public boolean setSSHPort(int port) {
      File sshd = new File("/etc/ssh/sshd");
      if (port == -1) {
         if (sshd.exists() && !sshd.delete()) {
            return false;
         }
      } else {
         if (!sshd.exists()) {
            try {
               if (!sshd.createNewFile()) {
                  throw new IOException("cannot create /etc/ssh/sshd");
               }
            } catch (IOException ioe) {
               return false;
            }
         }

         try (BufferedWriter writer = new BufferedWriter(new FileWriter(sshd))) {
            writer.write(String.valueOf(port));
            writer.flush();
         } catch (IOException ioe) {
            return false;
         }
      }

      String[] commandString = TextUtil.split("/proc/boot/sushi sshdinit.sh", ' ');
      ProcessBuilder builder = new ProcessBuilder(commandString);

      try {
         Process sshdinit = builder.start();
         int rc = sshdinit.waitFor();
         if (rc != 0) {
            throw new Exception("failed to launch sshdinit.sh");
         } else {
            return true;
         }
      } catch (Exception var16) {
         return false;
      }
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
         } else if (OperatingSystemEnum.isOS(OperatingSystemEnum.qnx)) {
            platformPassword = AccessController.doPrivileged(SyspwUtil::getSystemPassphrase);
         } else if (OperatingSystemEnum.isOS(OperatingSystemEnum.windows)) {
            platformPassword = AccessController.doPrivileged(() -> RegistryUtil.getEncryptedRegistryChars("systempw", false, false));
         } else if (OperatingSystemEnum.isOS(OperatingSystemEnum.linux)) {
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

      boolean success = false;

      try {
         if (SystemPassphrase.usingSimpleSystemPassphrase()) {
            success = AccessController.doPrivileged(
               () -> SimpleKeyValueUtil.getInstance(SecurityInitializer.getInstance().getSecurityInfoProvider().getSecurityDir().getPath())
                  .set(".sp", newPassword.getBytes(StandardCharsets.UTF_8))
            );
         } else if (OperatingSystemEnum.isOS(OperatingSystemEnum.qnx)) {
            success = AccessController.doPrivileged(() -> SyspwUtil.setSystemPassphrase(oldPassword, newPassword));
         } else if (OperatingSystemEnum.isOS(OperatingSystemEnum.windows)) {
            success = AccessController.doPrivileged(() -> RegistryUtil.setEncryptedRegistryString("systempw", newPassword, false));
         } else if (OperatingSystemEnum.isOS(OperatingSystemEnum.linux)) {
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

      try {
         if (OperatingSystemEnum.isOS(OperatingSystemEnum.qnx)) {
            return true;
         }

         if (OperatingSystemEnum.isOS(OperatingSystemEnum.windows)) {
            byte[] keyMaterial = AccessController.doPrivileged(() -> RegistryUtil.getEncryptedRegistryBytes(keyName, true, true));
            if (keyMaterial != null && keyMaterial.length > 0) {
               if (KeyMaterial.LOG.isLoggable(Level.FINEST)) {
                  KeyMaterial.LOG.finest("Migrating legacy key material in registry to '" + securityDirectory.getPath() + File.separator + keyName + "'");
               }

               if (!this.setKeyMaterial(securityDirectory, keyName, keyMaterial)) {
                  throw new Exception("Could not set key material during migration");
               }
            }
         } else if (OperatingSystemEnum.isOS(OperatingSystemEnum.linux)) {
            byte[] hexStringBytes = null;
            if (new File("/etc/niagara").exists()) {
               hexStringBytes = AccessController.doPrivileged(() -> SimpleKeyValueUtil.getInstance("/etc/niagara").get(keyName));
            } else if (KeyMaterial.LOG.isLoggable(Level.FINE)) {
               KeyMaterial.LOG.fine("/etc/niagara does not exist. Skipping migration check.");
            }

            if (hexStringBytes == null) {
               return super.checkForKeyMaterialUpgrade(securityDirectory, keyName);
            }

            if (KeyMaterial.LOG.isLoggable(Level.FINEST)) {
               KeyMaterial.LOG
                  .finest(
                     "Migrating legacy key material at/etc/niagara"
                        + File.separator
                        + keyName
                        + "to '"
                        + securityDirectory.getPath()
                        + File.separator
                        + keyName
                        + "'"
                  );
            }

            byte[] keyMaterial = ByteArrayUtil.hexStringToBytes(new String(hexStringBytes, StandardCharsets.UTF_8));
            if (!this.setKeyMaterial(securityDirectory, keyName, keyMaterial)) {
               throw new Exception("Could not set key material during migration");
            }
         }

         return true;
      } catch (Exception e) {
         throw new RuntimeException("Error occurred during migration", e);
      }
   }

   @Override
   public boolean supportsKeyMaterialRecovery() {
      return true;
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
      byte[] keyMaterial = null;

      try {
         if (KeyMaterial.usingSimpleKeyMaterial()) {
            keyMaterial = AccessController.doPrivileged(() -> SimpleKeyValueUtil.getInstance(securityDirectory.getPath()).get(keyName));
         } else if (OperatingSystemEnum.isOS(OperatingSystemEnum.qnx)) {
            keyMaterial = AccessController.doPrivileged(() -> SimpleKeyValueUtil.getInstance("/etc/km").get(keyName));
         } else if (OperatingSystemEnum.isOS(OperatingSystemEnum.windows)) {
            byte[] encrypted = AccessController.doPrivileged(() -> SimpleKeyValueUtil.getInstance(securityDirectory.getPath()).get(keyName));
            keyMaterial = encrypted != null ? DpapiUtil.decrypt(encrypted, true) : null;
         } else if (OperatingSystemEnum.isOS(OperatingSystemEnum.linux)) {
            keyMaterial = AccessController.doPrivileged(() -> SimpleKeyValueUtil.getInstance(securityDirectory.getPath()).get(keyName));
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
      boolean success = false;

      try {
         if (KeyMaterial.usingSimpleKeyMaterial()) {
            success = AccessController.doPrivileged(() -> SimpleKeyValueUtil.getInstance(securityDirectory.getPath()).set(keyName, keyMaterial));
         } else if (OperatingSystemEnum.isOS(OperatingSystemEnum.qnx)) {
            success = AccessController.doPrivileged(() -> SimpleKeyValueUtil.getInstance("/etc/km").set(keyName, keyMaterial));
         } else if (OperatingSystemEnum.isOS(OperatingSystemEnum.windows)) {
            byte[] encrypted = keyMaterial != null ? DpapiUtil.encrypt(keyMaterial, true, true) : null;
            success = AccessController.doPrivileged(() -> SimpleKeyValueUtil.getInstance(securityDirectory.getPath()).set(keyName, encrypted));
         } else if (OperatingSystemEnum.isOS(OperatingSystemEnum.linux)) {
            success = AccessController.doPrivileged(() -> SimpleKeyValueUtil.getInstance(securityDirectory.getPath()).set(keyName, keyMaterial));
            Path keyMaterialPath = new File(securityDirectory, keyName).toPath();
            Set<PosixFilePermission> permissions = PosixFilePermissions.fromString("rw-rw----");

            try {
               Files.setPosixFilePermissions(keyMaterialPath, permissions);
            } catch (IOException e) {
               KeyMaterial.LOG.log(Level.WARNING, String.format("Failed to restrict permissions on key material file <%s>", keyMaterialPath), e);
            }
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
         } else if (OperatingSystemEnum.isOS(OperatingSystemEnum.qnx)) {
            return AccessController.doPrivileged(() -> new File("/etc/km", keyName).lastModified());
         } else if (OperatingSystemEnum.isOS(OperatingSystemEnum.windows)) {
            return AccessController.doPrivileged(() -> new File(securityDirectory, keyName).lastModified());
         } else {
            return OperatingSystemEnum.isOS(OperatingSystemEnum.linux)
               ? AccessController.doPrivileged(() -> new File(securityDirectory, keyName).lastModified())
               : 0L;
         }
      }
   }

   private String deglobArgument(String argument) {
      FileSystem defaultFileSystem = null;

      try {
         defaultFileSystem = FileSystems.getDefault();
         PathMatcher globMatcher = defaultFileSystem.getPathMatcher("glob:" + argument);
         File globFileParent = new File(argument).getParentFile();
         if (globFileParent != null && globFileParent.exists()) {
            File[] childrenFiles = globFileParent.listFiles();
            if (childrenFiles != null) {
               for (File childFile : childrenFiles) {
                  if (globMatcher.matches(childFile.toPath())) {
                     return childFile.getPath();
                  }
               }
            }
         }
      } finally {
         if (defaultFileSystem != null) {
            try {
               defaultFileSystem.close();
            } catch (Throwable var18) {
            }
         }
      }

      return null;
   }

   @Override
   public String readSystemLog(String systemLogName) {
      if (!OperatingSystemEnum.isOS(OperatingSystemEnum.qnx)) {
         return "";
      }

      ArrayList<String> commandLine = new ArrayList<>();
      if (systemLogName == null || "".equals(systemLogName)) {
         commandLine.add("/proc/boot/slog2info");
      } else if ("log1".equals(systemLogName) || "log2".equals(systemLogName)) {
         String logArgument = this.deglobArgument("/var/slog/" + systemLogName + "_*");
         if (logArgument != null) {
            commandLine.add("/proc/boot/cat");
            commandLine.add(logArgument);
         }
      }

      if (commandLine.size() == 0) {
         return "";
      }

      StringWriter systemLogOutput = new StringWriter();
      ProcessBuilder builder = new ProcessBuilder(commandLine.toArray(new String[0]));
      builder.redirectErrorStream(true);
      Process proc = null;

      try {
         proc = builder.start();
         proc.getOutputStream().close();

         try (InputStream in = proc.getInputStream()) {
            int len = 4096;
            byte[] buf = new byte[len];

            while (true) {
               int n = in.read(buf, 0, len);
               if (n < 0) {
                  break;
               }

               systemLogOutput.write(new String(buf).toCharArray(), 0, n);
            }
         }

         proc.waitFor();
      } catch (Exception e) {
         if (proc != null) {
            proc.destroy();
         }

         systemLogOutput.write(e.getMessage() == null ? "Error launching system log command" : e.getMessage());
      }

      return systemLogOutput.toString();
   }

   public boolean daemonize() {
      return daemonize0();
   }

   private static native boolean daemonize0();
}
