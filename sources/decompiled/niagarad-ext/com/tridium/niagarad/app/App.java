package com.tridium.niagarad.app;

import com.tridium.niagarad.NiagaraDaemon;
import com.tridium.niagarad.io.PipedOutputBuffer;
import com.tridium.niagarad.log.MessageBundle;
import com.tridium.niagarad.util.KeyedList;
import com.tridium.nre.platform.IPlatformProvider;
import com.tridium.nre.platform.OperatingSystemEnum;
import com.tridium.nre.platform.PlatformUtil;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PipedOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Properties;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.nre.util.FileUtil;
import javax.baja.nre.util.TextUtil;

public abstract class App {
   public static final int APP_OVERRIDE_NONE = 0;
   public static final int APP_OVERRIDE_START = 1;
   public static final int APP_OVERRIDE_STOP = 2;
   public static final int APP_STATUS_IDLE = 0;
   public static final int APP_STATUS_STARTING = 1;
   public static final int APP_STATUS_RUNNING = 2;
   public static final int APP_STATUS_STOPPING = 3;
   public static final int APP_STATUS_FAILED = 4;
   public static final int APP_STATUS_UNKNOWN = 5;
   public static final int APP_STATUS_HALTED = 6;
   protected String appName;
   protected String displayName;
   protected String logPath;
   protected Logger filter;
   protected OutputStream hStdInput;
   protected Process hProcess;
   protected PipedOutputBuffer appOut;
   protected int restartOverride;
   protected String adminUser;
   protected EngineWatchdog watchdog;
   protected boolean isAutoStart;
   protected boolean isAutoRestart;
   protected boolean isDisabled;
   protected boolean isDirty;
   protected int logBufferFileSize;
   private int status = 5;
   public KeyedList reportData;
   protected final Object applicationMonitor = new Object();
   protected AppRegistry registry;
   protected App.ApplicationPipeThread pipeThread = null;
   private final IPlatformProvider platformProvider;
   public static final String _STATION_USER_IDENTIFIER = "suNkiNg";

   protected App(String name, AppRegistry reg, Logger mout, IPlatformProvider platformProvider) {
      this.platformProvider = platformProvider;
      this.isAutoStart = false;
      this.isAutoRestart = true;
      this.isDisabled = false;
      this.isDirty = false;
      this.filter = mout;
      this.logBufferFileSize = 262144;
      this.hStdInput = null;
      this.adminUser = null;
      this.registry = reg;
      this.reportData = null;
      this.setAppStatus(5);
      this.appName = name;
      this.displayName = this.appName;
      this.logPath = this.registry.getAppsDirPath() + File.separator + this.appName + File.separator + "console.txt";
      this.hProcess = null;
      this.restartOverride = 0;
      Logger appOutputLog = Logger.getLogger("appOut");
      if (NiagaraDaemon._FORCE) {
         appOutputLog.setLevel(Level.FINE);
      }

      this.appOut = new PipedOutputBuffer(262144, appOutputLog, this.appName);
      this.appOut.loadFile(this.logPath);
      if (NiagaraDaemon._ECHO_STATION) {
         this.appOut.tee(new BufferedWriter(new OutputStreamWriter(System.out, StandardCharsets.UTF_8), 8192));
      }

      this.appOut.start();
      this.watchdog = new EngineWatchdog(this, mout, platformProvider);
      if (this.watchdog.init() != 0) {
         this.setAppStatus(4);
         this.filter
            .severe("error creating " + this.registry.getAppType() + " " + this.appName + " watchdog, " + this.registry.getAppType() + " can not be started");
         this.watchdog = null;
      }
   }

   public String getAppName() {
      return this.appName;
   }

   public String getAppType() {
      return "app";
   }

   public String getDisplayName() {
      synchronized (this.applicationMonitor) {
         return this.displayName;
      }
   }

   public int getLogBufferFileSize() {
      return this.logBufferFileSize;
   }

   public void setLogBufferFileSize(int size) {
      this.logBufferFileSize = size;
   }

   public PipedOutputBuffer getAppOut() {
      return this.appOut;
   }

   public void sendMessage(String message) {
      this.sendMessages(new String[]{message});
   }

   public abstract void sendMessages(String[] var1);

   public abstract boolean isAcceptingMessages();

   public void clearOutput() {
      this.appOut.clear();
      if (!this.appOut.saveToFile(this.getLogPath(), this.logBufferFileSize)) {
         this.filter.severe("error logging " + this.registry.getAppType() + " " + this.appName + " output to file (" + this.getLogPath() + ")");
      }
   }

   public String getLogPath() {
      return this.logPath;
   }

   public int getStatus() {
      return this.status;
   }

   public String getBogPath() {
      return this.registry.getAppsDirPath() + File.separator + this.appName + File.separator + "config.bog";
   }

   void updateAttribute(String attrName, String attrValue) {
      if (attrName.equalsIgnoreCase("isdisabled")) {
         this.isDisabled = Boolean.parseBoolean(attrValue);
      } else if (attrName.equalsIgnoreCase("isautorestart")) {
         this.isAutoRestart = Boolean.parseBoolean(attrValue);
      } else if (attrName.equalsIgnoreCase("isautostart")) {
         this.isAutoStart = Boolean.parseBoolean(attrValue);
      } else if (attrName.equalsIgnoreCase("logbuffersize")) {
         try {
            int logBufferSize = Integer.parseInt(attrValue);
            if (logBufferSize < 8192 || logBufferSize > 524288) {
               throw new NumberFormatException();
            }

            this.appOut.resetMemBufferSize(logBufferSize);
         } catch (NumberFormatException nfe) {
            String appType = this.registry.getAppType();
            this.filter.severe("invalid logbuffersize value '" + attrValue + "' specified for " + appType + " " + this.appName + ", ignoring value");
         }
      } else if (attrName.equalsIgnoreCase("logbufferfilesize")) {
         try {
            int logBufferFileSizeValue = Integer.parseInt(attrValue);
            if (logBufferFileSizeValue < 8192) {
               throw new NumberFormatException();
            }

            this.logBufferFileSize = logBufferFileSizeValue;
         } catch (NumberFormatException nfe) {
            String appType = this.registry.getAppType();
            this.filter.severe("invalid logbufferfilesize value '" + attrValue + "' specified for " + appType + " " + this.appName + ", ignoring value");
         }
      }
   }

   public boolean isActive() {
      boolean result = false;
      synchronized (this.applicationMonitor) {
         switch (this.status) {
            case 0:
            case 4:
            case 5:
            case 6:
               result = false;
               break;
            case 1:
            case 2:
            case 3:
               result = true;
         }

         return result;
      }
   }

   public void saveProperties(Properties props) {
      String prefix = this.registry.getAppType() + "." + this.appName + ".";
      props.setProperty(prefix + "isdisabled", this.isDisabled ? "true" : "false");
      props.setProperty(prefix + "isautorestart", this.isAutoRestart ? "true" : "false");
      props.setProperty(prefix + "isautostart", this.isAutoStart ? "true" : "false");
      props.setProperty(prefix + "logbufferfilesize", String.valueOf(this.logBufferFileSize));
      props.setProperty(prefix + "logbuffersize", String.valueOf(this.appOut.getMemBufferSize()));
   }

   public boolean start() {
      synchronized (this.applicationMonitor) {
         String appType = this.registry.getAppType();
         if (!this.registry.canStartApp()) {
            if (this.appOut != null) {
               this.appOut
                  .printf(
                     ("ERROR: Cannot start "
                           + appType
                           + " "
                           + this.appName
                           + " would exceed licensed running "
                           + appType
                           + " count ("
                           + this.registry.getMaxRunningAppCount()
                           + ")\n")
                        .getBytes(StandardCharsets.UTF_8)
                  );
               this.appOut.printf("If a license was recently installed/upgraded, restart niagarad to apply any changes\n".getBytes(StandardCharsets.UTF_8));
            }

            this.filter
               .severe(
                  "cannot start "
                     + appType
                     + " "
                     + this.appName
                     + " would exceed licensed running "
                     + appType
                     + " count ("
                     + this.registry.getMaxRunningAppCount()
                     + ")"
               );
            this.filter.severe("if a license was recently installed/upgraded, restart niagarad to apply any changes");
            return false;
         } else if (!this.canStart()) {
            if (this.appOut != null) {
               this.appOut.printf(("ERROR: cannot start " + appType + " " + this.appName + ", " + appType + " not idle\n").getBytes(StandardCharsets.UTF_8));
            }

            this.filter.severe("cannot start " + appType + " " + this.appName + ", " + appType + " not idle");
            return false;
         } else {
            this.setAppStatus(1);
            this.registry.notifyAppStarted();
            App.RunAppThread appThread = new App.RunAppThread();
            appThread.start();
            if (this.filter.isLoggable(Level.FINE)) {
               this.filter.fine(appType + " " + this.appName + " thread started [tid = " + appThread.getId() + "]");
            }

            this.restartOverride = 0;
            return true;
         }
      }
   }

   public void notifyChanges(KeyedList data) {
      synchronized (this.applicationMonitor) {
         String appType = this.registry.getAppType();
         if (!this.isAcceptingMessages()) {
            this.filter.warning(appType + " " + this.appName + " started outside of niagarad, output is not available");
         } else if (this.status != 2) {
            this.filter.warning("invalid state in notifyChanges, " + appType + " " + this.appName + " is not 'running'");
         } else {
            if (data != null) {
               this.displayName = data.get("displayname", this.appName);
               if (this.reportData != null) {
                  for (int i = 0; i < data.size(); i++) {
                     this.reportData.set(data.getKey(i), data.getAtIndex(i));
                  }
               } else {
                  this.reportData = data;
               }
            }
         }
      }
   }

   public void notifyRunning(KeyedList data) {
      synchronized (this.applicationMonitor) {
         String appType = this.registry.getAppType();
         if (!this.isAcceptingMessages()) {
            this.filter.warning(appType + " " + this.appName + " is not accepting messages, can not set status to running");
         } else if (this.status != 1) {
            this.filter.warning("invalid state in notifyRunning, " + appType + " " + this.appName + " is not 'starting'");
         } else {
            this.reportData = data;
            if (this.reportData != null) {
               String newDisplayName = this.reportData.get("displayname", this.appName);
               this.displayName = newDisplayName;
               this.filter.info(appType + " " + newDisplayName + " startup complete");
            }

            this.setAppStatus(2);
         }
      }
   }

   public boolean stop(int restartOverride) {
      return this.stop(restartOverride, false);
   }

   public boolean stop(int restartOverride, boolean block) {
      String appType = this.registry.getAppType();
      if (this.filter.isLoggable(Level.FINE)) {
         this.filter.fine("stop requested for " + appType + " " + this.appName + " (restartOverride = " + restartOverride + ", block = " + block + ")");
      }

      if (restartOverride == 1 && !this.allowRestart()) {
         this.filter.warning("restart not allowed for " + appType + " " + this.appName);
         return false;
      }

      if (!block) {
         try {
            App.StopAppThread thread = new App.StopAppThread(new App.StopKillParameters(this, restartOverride));
            thread.start();
            return true;
         } catch (Throwable t) {
            this.filter.log(Level.SEVERE, "throwable occurred creating the stop request, can not stop " + appType + " " + this.appName, t);
            return false;
         }
      } else {
         synchronized (this.applicationMonitor) {
            if (this.status != 2 && this.status != 1) {
               if (this.status != 3) {
                  StringBuilder buffer = new StringBuilder();
                  switch (this.status) {
                     case 0:
                     case 6:
                        break;
                     case 1:
                     case 2:
                     case 3:
                     default:
                        buffer.append("invalid status (").append(this.status).append(") for ").append(appType).append(" ").append(this.appName);
                        this.filter.warning(buffer.toString());
                        break;
                     case 4:
                     case 5:
                        buffer.append("stopping failed or unknown ").append(appType).append(" ").append(this.appName);
                        this.filter.warning(buffer.toString());
                  }

                  return true;
               }
            } else {
               this.restartOverride = restartOverride;
               StringBuilder buffer = new StringBuilder();
               buffer.append(appType).append(" ").append(this.appName).append(" stopping");
               this.filter.info(buffer.toString());
               if (this.isAcceptingMessages() && this.status == 2) {
                  this.setAppStatus(3);
                  this.sendMessage("quit");
                  if (this.hStdInput != null) {
                     try {
                        this.hStdInput.close();
                     } catch (IOException var9) {
                     }

                     this.hStdInput = null;
                  }
               } else {
                  this.setAppStatus(3);
                  buffer = new StringBuilder();
                  buffer.append("Killing ").append(appType).append(" ").append(this.appName).append(" from daemon\n\n");
                  this.appOut.printf(buffer.toString().getBytes(StandardCharsets.UTF_8));
                  destroyProcess(this.hProcess);
               }
            }
         }

         this.waitForTermination();
         return true;
      }
   }

   public boolean kill(int restartOverride) {
      return this.kill(restartOverride, false);
   }

   public boolean kill(int restartOverride, boolean block) {
      String appType = this.registry.getAppType();
      if (this.filter.isLoggable(Level.FINE)) {
         this.filter.fine("kill requested for " + appType + " " + this.appName + " (restartOverride = " + restartOverride + ", block = " + block + ")");
      }

      if (restartOverride == 1 && !this.allowRestart()) {
         this.filter.warning("restart not allowed for " + appType + " " + this.appName);
         return false;
      }

      if (!block) {
         App.KillAppThread thread = new App.KillAppThread(new App.StopKillParameters(this, restartOverride));
         thread.start();
         return true;
      }

      synchronized (this.applicationMonitor) {
         if (this.status != 2 && this.status != 1 && this.status != 3) {
            this.filter.warning("cannot kill " + appType + " " + this.appName + ", " + appType + " not starting or idle");
            return false;
         }

         this.setAppStatus(3);
         this.restartOverride = restartOverride;
         this.filter.info("killing " + appType + " " + this.appName);
         destroyProcess(this.hProcess);
      }

      if (this.watchdog != null) {
         this.watchdog.stop();
      }

      return true;
   }

   public abstract void generateStackDump();

   public void waitForTermination() {
      while (this.isActive()) {
         try {
            Thread.sleep(1000L);
         } catch (InterruptedException var2) {
         }
      }
   }

   public void run() {
      String appType = this.registry.getAppType();
      synchronized (this.applicationMonitor) {
         if (!this.prepareForLaunch()) {
            this.registry.notifyAppStopped();
            this.setAppStatus(4);
            return;
         }

         if (this.watchdog == null) {
            this.filter.severe("engine watchdog for " + appType + " " + this.appName + " not initialized, can not run");
            this.registry.notifyAppStopped();
            this.setAppStatus(4);
            return;
         }

         if (this.filter.isLoggable(Level.FINE)) {
            this.filter.fine("starting engine watchdog for " + appType + " " + this.appName);
         }

         this.watchdog.start();
         if (!this.watchdog.isAlive()) {
            this.filter.severe("engine watchdog for " + appType + " " + this.appName + " failed to start, can not run");
            this.registry.notifyAppStopped();
            this.setAppStatus(4);
            return;
         }

         if (!this.launch()) {
            if (this.watchdog != null) {
               this.watchdog.stop();
            }

            this.registry.notifyAppStopped();
            this.setAppStatus(4);
            return;
         }
      }

      int exitCode = this.waitForAppExit(this.hProcess);
      synchronized (this.applicationMonitor) {
         if (this.watchdog != null) {
            this.watchdog.stop();
         }

         if (exitCode > 127) {
            exitCode -= 256;
         }

         StringBuilder buffer = new StringBuilder();
         buffer.append(appType).append(" ").append(this.appName).append(" stopped");
         if (this.filter.isLoggable(Level.FINE)) {
            this.filter.fine(buffer.toString());
         }

         this.cleanup();
         if (exitCode == 0) {
            if (this.allowRestart()) {
               this.setAppStatus(0);
            } else {
               this.setAppStatus(6);
            }
         } else {
            if (exitCode == -99) {
               buffer = new StringBuilder();
               buffer.append(appType).append(" ").append(this.appName).append(" requested reboot on exit");
               this.filter.info(buffer.toString());
               this.setAppStatus(6);
               if (!NiagaraDaemon.getInstance().lockClientPermanent()) {
                  this.filter.severe("multiple client access conflict (app)");
                  return;
               }

               NiagaraDaemon.getInstance().queueReboot();
               return;
            }

            buffer = new StringBuilder();
            buffer.append(appType).append(" ").append(this.appName).append(" failed, rc = ").append(exitCode);
            MessageBundle msg = new MessageBundle("platform", "App.failed", appType, buffer.toString());
            msg.addLexiconArgument(this.appName);
            msg.addLexiconArgument(exitCode);
            this.filter.severe(buffer.toString());
            this.appOut.printf("\nApp Failed\n".getBytes(StandardCharsets.UTF_8));
            this.setAppStatus(4);
         }

         if (this.restartOverride == 2) {
            if (this.filter.isLoggable(Level.FINE)) {
               buffer = new StringBuilder();
               buffer.append("preventing ").append(appType).append(" ").append(this.appName).append(" restart, stopped manually");
               this.filter.fine(buffer.toString());
            }
         } else if (this.isAutoRestart && this.status == 4) {
            if (this.filter.isLoggable(Level.FINE)) {
               buffer = new StringBuilder();
               buffer.append(appType).append(" ").append(this.appName).append(" restart on failure is enabled, requesting restart");
               this.filter.fine(buffer.toString());
            }

            if (this.isRecoverableError(exitCode)) {
               if (this.allowRestart()) {
                  buffer = new StringBuilder();
                  buffer.append(appType).append(" ").append(this.appName).append(" restarting");
                  this.filter.info(buffer.toString());
                  this.start();
               } else {
                  NiagaraDaemon.getInstance().failureReboot();
               }
            } else if (this.filter.isLoggable(Level.FINE)) {
               buffer = new StringBuilder();
               buffer.append("preventing ").append(appType).append(" ").append(this.appName).append(" restart, error is not recoverable");
               this.filter.fine(buffer.toString());
            }
         } else if (this.restartOverride == 1 && (this.status == 0 || this.status == 4)) {
            if (this.filter.isLoggable(Level.FINE)) {
               buffer = new StringBuilder();
               buffer.append(appType).append(" ").append(this.appName).append(" restart override is enabled, requesting restart");
               this.filter.fine(buffer.toString());
            }

            try {
               this.applicationMonitor.wait(1500L);
            } catch (InterruptedException var7) {
            }

            buffer = new StringBuilder();
            buffer.append(appType).append(" ").append(this.appName).append(" restarting");
            this.filter.info(buffer.toString());
            this.start();
         }
      }
   }

   public void setIsAutoStart(boolean newIsAutoStartValue) {
      this.isAutoStart = newIsAutoStartValue;
   }

   public void setIsAutoRestart(boolean newIsAutoRestartValue) {
      this.isAutoRestart = newIsAutoRestartValue;
   }

   public void setIsDisabled(boolean newIsDisabledValue) {
      this.isDisabled = newIsDisabledValue;
   }

   public void setIsDirty(boolean newIsDirtyValue) {
      this.isDirty = newIsDirtyValue;
   }

   public boolean getIsAutoStart() {
      return this.isAutoStart;
   }

   public boolean getIsAutoRestart() {
      return this.isAutoRestart;
   }

   public boolean getIsDisabled() {
      return this.isDisabled;
   }

   public boolean getIsDirty() {
      return this.isDirty;
   }

   protected abstract boolean prepareForLaunch();

   protected abstract boolean launch();

   protected abstract boolean allowRestart();

   protected boolean canStart() {
      return !this.allowRestart() ? this.status == 0 : this.status == 0 || this.status == 4;
   }

   protected abstract boolean isRecoverableError(int var1);

   protected int writeStdInput(String message) {
      try {
         this.hStdInput.write(message.getBytes(StandardCharsets.UTF_8));
         this.hStdInput.flush();
         return 0;
      } catch (IOException e) {
         return -1;
      }
   }

   protected static void makeAdminCredentials(StringBuilder user, StringBuilder password) throws IOException, NoSuchAlgorithmException {
      Random random = new SecureRandom();
      byte[] tempUsername = new byte[16];
      random.nextBytes(tempUsername);
      byte[] tempPassword = new byte[16];
      random.nextBytes(tempPassword);
      user.append(TextUtil.bytesToHexString(tempUsername)).append("suNkiNg");
      password.append(TextUtil.bytesToHexString(tempPassword));
   }

   protected void cleanup() {
      this.hProcess = null;
      if (this.pipeThread != null && this.pipeThread.isAlive()) {
         this.pipeThread.stopRequested = true;
         this.pipeThread.interrupt();

         try {
            this.pipeThread.input.close();
         } catch (Exception var5) {
         }

         try {
            this.pipeThread.output.close();
         } catch (Exception var4) {
         }

         try {
            this.pipeThread.join();
         } catch (InterruptedException var3) {
         }

         this.pipeThread.stopRequested = false;
         this.pipeThread = null;
      }

      if (this.hStdInput != null) {
         try {
            this.hStdInput.close();
         } catch (IOException var2) {
         }

         this.hStdInput = null;
      }

      if (this.adminUser != null) {
         NiagaraDaemon.getInstance().auth.getAuthDomain().getExtraUsers().remove(this.adminUser);
         this.adminUser = null;
      }

      this.appOut.printf("\n\n".getBytes(StandardCharsets.UTF_8));
      String appType = this.registry.getAppType();
      if (new File(this.registry.getAppsDirPath(), this.appName).exists()) {
         FileUtil.renameToBackup(new File(this.getLogPath()), 10);
         if (!this.appOut.saveToFile(this.getLogPath(), this.logBufferFileSize)) {
            this.filter.severe("error logging " + appType + " " + this.appName + " output to file (" + this.getLogPath() + ")");
         }
      }
   }

   protected int waitForAppExit(Process processToWaitFor) {
      String appType = this.registry.getAppType();

      int exitCode;
      try {
         exitCode = processToWaitFor.waitFor();
         if (exitCode > 127) {
            exitCode -= 256;
         }

         this.filter.info(this.getAppType() + " " + this.appName + " exited with status " + exitCode);
      } catch (InterruptedException e) {
         this.filter.severe("error waiting for " + appType + " " + this.appName + " to terminate: " + e);
         exitCode = -1;
      }

      this.registry.notifyAppStopped();
      return exitCode;
   }

   private static void destroyProcess(Process stationProcess) {
      if (PlatformUtil.isTridiumPlatform() && OperatingSystemEnum.isOS(OperatingSystemEnum.qnx)) {
         try {
            ProcessBuilder builder = new ProcessBuilder(TextUtil.split("/proc/boot/slay -f -s SIGKILL station", ' '));
            Process slayStationProcess = builder.start();
            slayStationProcess.waitFor();
         } catch (Exception var3) {
         }
      }

      stationProcess.destroyForcibly();
   }

   public static String statusValueToString(int status) {
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

   protected void setAppStatus(int newStatus) {
      if (this.status != newStatus) {
         if (this.filter.isLoggable(Level.FINEST)) {
            this.filter
               .finest(
                  "notify application status change for "
                     + this.getAppType()
                     + " "
                     + this.getAppName()
                     + ": "
                     + statusValueToString(this.status)
                     + " -> "
                     + statusValueToString(newStatus)
               );
         }

         int oldStatus = this.status;
         this.status = newStatus;
         if (!this.platformProvider.notifyApplicationStatus(this.getAppType(), this.getAppName(), this.status)) {
            this.filter
               .warning(
                  "failed to notify application status change for "
                     + this.getAppType()
                     + " "
                     + this.getAppName()
                     + ": "
                     + statusValueToString(oldStatus)
                     + " -> "
                     + statusValueToString(newStatus)
               );
         }
      }
   }

   protected int getAppStatus() {
      return this.status;
   }

   public class ApplicationPipeThread extends Thread {
      InputStream input;
      PipedOutputStream output;
      boolean stopRequested = false;

      public ApplicationPipeThread(InputStream input, PipedOutputStream output) {
         super("Niagarad:ApplicationPipeThread(" + App.this.getAppName() + ")");
         this.input = input;
         this.output = output;
      }

      @Override
      public void run() {
         this.stopRequested = false;
         byte[] buffer = new byte[8192];

         while (!this.stopRequested) {
            try {
               if (!interrupted()) {
                  int bytesRead;
                  if ((bytesRead = this.input.read(buffer, 0, 8192)) == -1) {
                     break;
                  }

                  this.output.write(buffer, 0, bytesRead);
               }
            } catch (IOException ioe) {
               break;
            }
         }

         try {
            this.output.close();
         } catch (IOException var4) {
         }
      }
   }

   private static class KillAppThread extends Thread {
      App.StopKillParameters parameters;

      public KillAppThread(App.StopKillParameters parms) {
         super("Niagarad:KillAppThread(" + parms.applicationToStopKill.getAppName() + ")");
         this.parameters = parms;
      }

      @Override
      public void run() {
         this.parameters.applicationToStopKill.kill(this.parameters.restartOverride, true);
      }
   }

   private class RunAppThread extends Thread {
      RunAppThread() {
         super("Niagarad:RunAppThread(" + App.this.getAppName() + ")");
      }

      @Override
      public void run() {
         App.this.run();
      }
   }

   private static class StopAppThread extends Thread {
      App.StopKillParameters parameters;

      StopAppThread(App.StopKillParameters parms) {
         super("Niagarad:StopAppThread(" + parms.applicationToStopKill.getAppName() + ")");
         this.parameters = parms;
      }

      @Override
      public void run() {
         this.parameters.applicationToStopKill.stop(this.parameters.restartOverride, true);
      }
   }

   private static class StopKillParameters {
      App applicationToStopKill;
      int restartOverride;

      StopKillParameters(App appToStopKill, int override) {
         this.applicationToStopKill = appToStopKill;
         this.restartOverride = override;
      }
   }
}
