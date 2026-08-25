package com.tridium.nre.security;

import com.tridium.nre.security.policy.NiagaraPolicy;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.security.AccessControlException;
import java.security.AccessController;
import java.security.AllPermission;
import java.security.Permission;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DeveloperSecurityManager extends SecurityManager {
   private FileWriter logFileWriter;
   private final Set<Integer> uniqueStackTraces = new HashSet<>();
   private final Set<Permission> missingPermission = new HashSet<>();
   private final NiagaraPolicy.PolicyType policyType;
   private static DeveloperSecurityManager DEV_SM;
   private static final Logger LOG = Logger.getLogger("security.developerSecurityManager");

   private DeveloperSecurityManager(NiagaraPolicy.PolicyType policyType) {
      this.policyType = policyType;
   }

   public static String enableDeveloperSecurityManager(NiagaraPolicy.PolicyType policyType) {
      NiagaraBasicPermission permission = new NiagaraBasicPermission("DISABLE_SECURITY_MANAGER");
      SecurityManager sm = System.getSecurityManager();
      if (sm != null) {
         sm.checkPermission(permission);
      }

      return AccessController.doPrivileged(() -> {
         if (DEV_SM == null) {
            DEV_SM = new DeveloperSecurityManager(policyType);
            System.setSecurityManager(DEV_SM);
            return DEV_SM.intiDebugLogFile();
         } else {
            throw new RuntimeException("DeveloperSecurityManager already initialized. Cannot re-enable.");
         }
      });
   }

   private String intiDebugLogFile() {
      String niagaraUserHome = AccessController.doPrivileged(() -> System.getProperty("niagara.user.home"));
      String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
      File logFile = new File(niagaraUserHome + File.separator + "developerSecurityManagerLog-" + this.policyType + "-" + timestamp + ".txt");

      try {
         this.logFileWriter = new FileWriter(logFile);
      } catch (Exception e) {
         LOG.log(Level.SEVERE, "Unable to to create file " + logFile + ".  Writing to stderr.", e);
         this.logFileWriter = new FileWriter(FileDescriptor.err);
      }

      AccessController.doPrivileged(
         () -> {
            Runtime.getRuntime()
               .addShutdownHook(
                  new Thread(
                     () -> {
                        try {
                           this.closeLogFile();
                        } catch (Throwable t) {
                           System.err
                              .println(
                                 "WARNING ["
                                    + new Date()
                                    + "][security.developerSecurityManager] Failed to properly close developer security manager log file "
                                    + t.getMessage()
                              );
                           t.printStackTrace();
                        }
                     }
                  )
               );
            return null;
         }
      );
      return logFile.getAbsolutePath();
   }

   private void closeLogFile() throws IOException {
      if (this.logFileWriter == null) {
         throw new IOException("Unable to write log summary, log file not created.");
      }

      this.logFileWriter.write("Missing Permission Summary:\n");

      for (Permission p : this.missingPermission) {
         this.logFileWriter.write("  " + p + "\n");
      }

      this.logFileWriter.close();
   }

   private boolean isWbAllPermissions(AccessControlException ace) {
      if (!(ace.getPermission() instanceof AllPermission) && this.policyType != NiagaraPolicy.PolicyType.WORKBENCH) {
         return false;
      }

      StackTraceElement[] stackTraceElements = ace.getStackTrace();

      for (StackTraceElement elem : stackTraceElements) {
         String className = elem.getClassName();
         if (!className.startsWith("java.lang.")
            && !className.startsWith("java.security.")
            && !className.startsWith("com.sun.glass.")
            && !className.startsWith("com.sun.javafx.")
            && !className.equals(this.getClass().getName())) {
            return false;
         }
      }

      return true;
   }

   private void logStackTrace(StackTraceElement[] stackTrace) throws IOException {
      if (this.logFileWriter == null) {
         throw new IOException("Unable to write stack trace, log file not created.");
      }

      this.logFileWriter.write("StackTrace: ");

      for (StackTraceElement elem : stackTrace) {
         this.logFileWriter.write(elem.toString() + "\n  ");
      }
   }

   private void logException(Exception e) {
      try {
         if (e instanceof AccessControlException) {
            this.logAccessControlException((AccessControlException)e);
         } else {
            if (this.logFileWriter == null) {
               throw new IOException("Unable to write exception, log file not created.");
            }

            this.logFileWriter.write("Exception: " + e.getLocalizedMessage() + "\n");
            this.logStackTrace(e.getStackTrace());
            this.logFileWriter.write("\n\n");
         }
      } catch (Throwable t) {
         LOG.log(Level.SEVERE, "Failed to write missing permission to log file", t);
      }
   }

   private void logAccessControlException(AccessControlException ace) throws Exception {
      if (!this.isWbAllPermissions(ace)) {
         this.missingPermission.add(ace.getPermission());
         StackTraceElement[] stackTrace = ace.getStackTrace();
         int stackTraceHash = Arrays.hashCode(stackTrace);
         if (!this.uniqueStackTraces.contains(stackTraceHash)) {
            this.uniqueStackTraces.add(stackTraceHash);
            LOG.log(Level.SEVERE, ace.getLocalizedMessage());
            if (this.logFileWriter == null) {
               throw new IOException("Unable to write access control exception, log file not created.");
            }

            this.logFileWriter.write("Missing Permission: " + ace.getPermission().toString() + "\n");
            this.logStackTrace(ace.getStackTrace());
            this.logFileWriter.write("\n\n");
         }
      }
   }

   @Override
   public void checkPermission(Permission perm) {
      try {
         super.checkPermission(perm);
      } catch (SecurityException | NullPointerException e) {
         this.logException(e);
      }
   }

   @Override
   public void checkPermission(Permission perm, Object context) {
      try {
         super.checkPermission(perm, context);
      } catch (SecurityException | NullPointerException e) {
         this.logException(e);
      }
   }

   @Override
   public void checkAccept(String host, int port) {
      try {
         super.checkAccept(host, port);
      } catch (SecurityException | NullPointerException e) {
         this.logException(e);
      }
   }

   @Override
   public void checkAccess(Thread t) {
      try {
         super.checkAccess(t);
      } catch (SecurityException | NullPointerException e) {
         this.logException(e);
      }
   }

   @Override
   public void checkAccess(ThreadGroup g) {
      try {
         super.checkAccess(g);
      } catch (SecurityException | NullPointerException e) {
         this.logException(e);
      }
   }

   @Override
   public void checkConnect(String host, int port) {
      try {
         super.checkConnect(host, port);
      } catch (SecurityException | NullPointerException e) {
         this.logException(e);
      }
   }

   @Override
   public void checkConnect(String host, int port, Object context) {
      try {
         super.checkConnect(host, port, context);
      } catch (SecurityException | NullPointerException e) {
         this.logException(e);
      }
   }

   @Override
   public void checkCreateClassLoader() {
      try {
         super.checkCreateClassLoader();
      } catch (SecurityException | NullPointerException e) {
         this.logException(e);
      }
   }

   @Override
   public void checkDelete(String file) {
      try {
         super.checkDelete(file);
      } catch (SecurityException | NullPointerException e) {
         this.logException(e);
      }
   }

   @Override
   public void checkExec(String cmd) {
      try {
         super.checkExec(cmd);
      } catch (SecurityException | NullPointerException e) {
         this.logException(e);
      }
   }

   @Override
   public void checkExit(int status) {
      try {
         super.checkExit(status);
      } catch (SecurityException | NullPointerException e) {
         this.logException(e);
      }
   }

   @Override
   public void checkLink(String lib) {
      try {
         super.checkLink(lib);
      } catch (SecurityException | NullPointerException e) {
         this.logException(e);
      }
   }

   @Override
   public void checkListen(int port) {
      try {
         super.checkListen(port);
      } catch (SecurityException | NullPointerException e) {
         this.logException(e);
      }
   }

   @Override
   public void checkMulticast(InetAddress maddr) {
      try {
         super.checkMulticast(maddr);
      } catch (SecurityException | NullPointerException e) {
         this.logException(e);
      }
   }

   @Override
   public void checkPackageAccess(String pkg) {
      try {
         super.checkPackageAccess(pkg);
      } catch (SecurityException | NullPointerException e) {
         this.logException(e);
      }
   }

   @Override
   public void checkPackageDefinition(String pkg) {
      try {
         super.checkPackageDefinition(pkg);
      } catch (SecurityException | NullPointerException e) {
         this.logException(e);
      }
   }

   @Override
   public void checkPrintJobAccess() {
      try {
         super.checkPrintJobAccess();
      } catch (SecurityException | NullPointerException e) {
         this.logException(e);
      }
   }

   @Override
   public void checkPropertiesAccess() {
      try {
         super.checkPropertiesAccess();
      } catch (SecurityException | NullPointerException e) {
         this.logException(e);
      }
   }

   @Override
   public void checkPropertyAccess(String key) {
      try {
         super.checkPropertyAccess(key);
      } catch (SecurityException | NullPointerException e) {
         this.logException(e);
      }
   }

   @Override
   public void checkRead(FileDescriptor fd) {
      try {
         super.checkRead(fd);
      } catch (SecurityException | NullPointerException e) {
         this.logException(e);
      }
   }

   @Override
   public void checkSecurityAccess(String target) {
      try {
         super.checkSecurityAccess(target);
      } catch (SecurityException | NullPointerException e) {
         this.logException(e);
      }
   }

   @Override
   public void checkSetFactory() {
      try {
         super.checkSetFactory();
      } catch (SecurityException | NullPointerException e) {
         this.logException(e);
      }
   }

   @Override
   public void checkWrite(FileDescriptor fd) {
      try {
         super.checkWrite(fd);
      } catch (SecurityException | NullPointerException e) {
         this.logException(e);
      }
   }

   @Override
   public void checkWrite(String file) {
      try {
         super.checkWrite(file);
      } catch (SecurityException | NullPointerException e) {
         this.logException(e);
      }
   }

   public void checkAwtEventQueueAccess() {
      LOG.severe("Calls to checkAwtEventQueueAccess() are deprecated");

      try {
         super.checkAwtEventQueueAccess();
      } catch (SecurityException | NullPointerException e) {
         this.logException(e);
      }
   }

   public void checkMemberAccess(Class<?> clazz, int which) {
      LOG.severe("Calls to checkMemberAccess(Class<?> clazz, int which) are deprecated");

      try {
         super.checkMemberAccess(clazz, which);
      } catch (SecurityException | NullPointerException e) {
         this.logException(e);
      }
   }

   @Override
   public void checkMulticast(InetAddress maddr, byte ttl) {
      LOG.severe("Calls to checkMulticast(InetAddress maddr, byte ttl) are deprecated");

      try {
         super.checkMulticast(maddr, ttl);
      } catch (SecurityException | NullPointerException e) {
         this.logException(e);
      }
   }

   public void checkSystemClipboardAccess() {
      LOG.severe("Calls to checkSystemClipboardAccess() are deprecated");

      try {
         super.checkSystemClipboardAccess();
      } catch (SecurityException | NullPointerException e) {
         this.logException(e);
      }
   }

   public boolean checkTopLevelWindow(Object window) {
      LOG.severe("Calls to checkTopLevelWindow() are deprecated");

      try {
         return super.checkTopLevelWindow(window);
      } catch (SecurityException | NullPointerException e) {
         this.logException(e);
         return true;
      }
   }

   protected ClassLoader currentClassLoader() {
      LOG.severe("Calls to currentClassLoader() are deprecated");
      return super.currentClassLoader();
   }

   protected Class<?> currentLoadedClass() {
      LOG.severe("Calls to currentLoadedClass() are deprecated");
      return super.currentLoadedClass();
   }

   public boolean getInCheck() {
      LOG.severe("Calls to getInCheck() are deprecated");
      return super.getInCheck();
   }

   protected boolean inClass(String name) {
      LOG.severe("Calls to inClass(String name) are deprecated");
      return super.inClass(name);
   }

   protected boolean inClassLoader() {
      LOG.severe("Calls to checkMemberAccess() are deprecated");

      try {
         super.inClassLoader();
      } catch (SecurityException | NullPointerException e) {
         this.logException(e);
      }

      return false;
   }
}
