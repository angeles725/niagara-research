package com.tridium.niagarad.app;

import com.tridium.niagarad.NiagaraDaemon;
import com.tridium.niagarad.log.MessageBundle;
import com.tridium.niagarad.platform.PlatformInfo;
import com.tridium.niagarad.util.KeyedList;
import com.tridium.nre.platform.IPlatformProvider;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.AccessController;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.baja.xml.XWriter;

public class AppRegistry {
   public static final int UNKNOWN_APP_REGISTRY = 0;
   public static final int STATION_APP_REGISTRY = 1;
   private Logger filter = null;
   private boolean appConsoleAvailable = false;
   private String appsDirPath = null;
   private String appType = null;
   private final int appTypeId;
   private ArrayList<App> apps = null;
   private long currentRunningApps = 0L;
   private final Object runningMonitor = new Object();
   private long maxRunningApps = 0L;
   private boolean appStarted = false;
   private Properties props = null;
   private final Object registryMonitor = new Object();
   private final IPlatformProvider platformProvider;
   private AppRegistry.WatchDirectoryThread appDirectoryThread;

   public AppRegistry(Logger log, boolean consoleAvailable, String appDirectory, String type, IPlatformProvider platformProvider) {
      this.platformProvider = platformProvider;
      this.filter = log;
      this.appsDirPath = appDirectory;
      this.appType = type;
      this.appStarted = false;
      this.appConsoleAvailable = consoleAvailable;
      if (this.appType.equalsIgnoreCase("station")) {
         this.appTypeId = 1;
      } else {
         this.appTypeId = 0;
      }

      if (this.appTypeId == 1) {
         this.maxRunningApps = PlatformInfo.getInstance().maxRunningStations();
      } else {
         this.maxRunningApps = 32L;
      }

      this.currentRunningApps = 0L;
      this.apps = new ArrayList<>();
      this.appDirectoryThread = null;
   }

   public void start(Properties pProps) {
      this.filter.info(this.appType + " registry starting");
      File appDirectoryFile = new File(this.appsDirPath);
      if (!appDirectoryFile.exists() && !appDirectoryFile.mkdirs()) {
         this.filter.severe("failed to create app directory at \"" + this.appsDirPath + "\", can not load apps");
      }

      synchronized (this.registryMonitor) {
         this.props = pProps;
         this.loadProperties();

         for (int i = 0; i < this.apps.size(); i++) {
            if (this.apps.get(i).getIsAutoStart()) {
               this.startApp(i);
            }
         }
      }

      this.dirWatchStart();
   }

   public void stop() {
      this.dirWatchStop();
      synchronized (this.registryMonitor) {
         this.apps.stream().filter(App::isActive).forEach(appx -> appx.stop(2));
         this.apps.stream().filter(App::isActive).forEach(App::waitForTermination);
         this.apps.forEach(appx -> appx.getAppOut().stop());

         for (App app : this.apps) {
            if (app.watchdog != null) {
               app.watchdog.stop();
               this.platformProvider.destroyWatchdog(app.appName);
               app.watchdog = null;
            }
         }

         this.apps.clear();
      }
   }

   public int stopApp(String name, int override) {
      App app = this.getApp(name);
      if (app == null) {
         return -1;
      }

      app.stop(override, false);
      return 0;
   }

   public List<App> getApps() {
      synchronized (this.registryMonitor) {
         return Collections.unmodifiableList(this.apps);
      }
   }

   public void saveAppProperties(App app, boolean writeToFile) {
      synchronized (this.registryMonitor) {
         app.saveProperties(this.props);
         app.setIsDirty(false);
         if (writeToFile) {
            NiagaraDaemon.saveProperties();
         }
      }
   }

   public void captureApp(String appName, KeyedList reportData) {
      App app = this.getApp(appName);
      if (app == null) {
         this.filter.warning(this.appType + " " + appName + " started outside of niagarad, capture is not available");
      } else {
         if (app.getStatus() == 1) {
            app.notifyRunning(reportData);
         } else if (app.getStatus() == 2) {
            app.notifyChanges(reportData);
         }
      }
   }

   public void removeApp(String name) {
      App target = this.getApp(name);
      if (target != null) {
         synchronized (this.registryMonitor) {
            if (target.isActive()) {
               target.stop(2, true);
            }

            String targetPrefix = this.appType + "." + target.getAppName();
            Set<Object> keys = this.props.keySet();
            Iterator<Object> iterator = keys.iterator();
            ArrayList<String> propsToRemove = new ArrayList<>();

            while (iterator.hasNext()) {
               String key = (String)iterator.next();
               if (key.contains(targetPrefix)) {
                  propsToRemove.add(key);
               }
            }

            propsToRemove.forEach(this.props::remove);
            this.apps.remove(target);
            target.getAppOut().stop();
            if (target.watchdog != null) {
               target.watchdog.stop();
               this.platformProvider.destroyWatchdog(target.getAppName());
               target.watchdog = null;
            }

            NiagaraDaemon.saveProperties();
         }
      }
   }

   public App getApp(String name) {
      App target = null;
      synchronized (this.registryMonitor) {
         for (App app : this.apps) {
            if (app.getAppName().equalsIgnoreCase(name)) {
               target = app;
               break;
            }
         }

         return target;
      }
   }

   public void stopAllApps(int override) {
      synchronized (this.registryMonitor) {
         this.apps.stream().filter(app -> app.getStatus() != 0 && app.getStatus() != 4).forEach(app -> app.stop(override, false));
      }
   }

   public int waitForAppTermination(String appName) {
      App app = this.getApp(appName);
      if (app == null) {
         return -1;
      }

      app.waitForTermination();
      return 0;
   }

   public void loadProperties() {
      synchronized (this.registryMonitor) {
         ArrayList<String> appsToAdd = new ArrayList<>();
         ArrayList<String> appsToRemove = new ArrayList<>();
         appsToRemove.addAll(this.apps.stream().map(App::getAppName).collect(Collectors.toList()));
         File appDirectory = new File(this.appsDirPath);
         if (appDirectory.exists()) {
            File[] appDirectoryContents = appDirectory.listFiles();
            if (appDirectoryContents != null) {
               for (File candidate : appDirectoryContents) {
                  if (candidate.isDirectory()) {
                     File[] candidateContents = candidate.listFiles();
                     if (candidateContents != null) {
                        for (File candidateContent : candidateContents) {
                           if (candidateContent.getName().equalsIgnoreCase("config.bog")) {
                              appsToAdd.add(candidate.getName());
                              break;
                           }
                        }
                     }
                  }
               }
            }
         }

         appsToRemove.removeAll(appsToAdd);
         appsToRemove.forEach(this::removeApp);
         appsToAdd.forEach(this::createApp);
         boolean mustSave = false;
         String prefix = this.appType + ".";
         int prefixLen = prefix.length();
         Set<Object> keys = this.props.keySet();
         Iterator<Object> iterator = keys.iterator();
         ArrayList<String> propsToRemove = new ArrayList<>();

         while (iterator.hasNext()) {
            String propKey = (String)iterator.next();
            String propValue = this.props.getProperty(propKey);
            if (propKey.indexOf(46) != -1
               && propKey.substring(0, prefixLen).equalsIgnoreCase(prefix)
               && this.updateAppInfo(propKey.substring(prefixLen), propValue) == 0) {
               propsToRemove.add(propKey);
               mustSave = true;
            }
         }

         propsToRemove.forEach(this.props::remove);
         if (mustSave) {
            NiagaraDaemon.saveProperties();
         }
      }
   }

   public void waitForAllTerminated() {
      synchronized (this.registryMonitor) {
         this.apps.forEach(App::waitForTermination);
      }
   }

   public void listApps(XWriter content) {
      synchronized (this.registryMonitor) {
         for (App app : this.apps) {
            content.w("  <").w(this.appType).w(" ").attr("name", app.getAppName()).w(">\n");
            content.w("    <displayname ").attr("value", app.getDisplayName()).w("/>\n");
            content.w("    <isautostart ").attr("value", app.getIsAutoStart() ? "true" : "false").w("/>\n");
            content.w("    <isautorestart ").attr("value", app.getIsAutoRestart() ? "true" : "false").w("/>\n");
            content.w("    <isdisabled ").attr("value", app.getIsDisabled() ? "true" : "false").w("/>\n");
            content.w("    <status ").attr("value", String.valueOf(app.getStatus())).w("/>\n");
            content.w("    <isacceptingmessages ").attr("value", app.isAcceptingMessages() ? "true" : "false").w("/>\n");
            content.w("    <isdirty ").attr("value", app.getIsDirty() ? "true" : "false").w("/>\n");
            content.w("    <logbuffersize ").attr("value", String.valueOf(app.getAppOut().getMemBufferSize())).w("/>\n");
            content.w("    <logbufferfilesize ").attr("value", String.valueOf(app.getLogBufferFileSize())).w("/>\n");
            if (app.reportData != null) {
               for (int j = 0; j < app.reportData.size(); j++) {
                  String key = app.reportData.getKey(j);
                  String value = app.reportData.getAtIndex(j);
                  if (key != null && value != null) {
                     content.w("    <").w(key).attr(" value", value).w("/>\n");
                  }
               }
            }

            content.w("  </").w(this.appType).w(">\n");
            content.flush();
         }
      }
   }

   boolean hasAppStarted() {
      return this.appStarted;
   }

   void notifyAppStarted() {
      synchronized (this.runningMonitor) {
         this.currentRunningApps++;
         this.appStarted = true;
         NiagaraDaemon.getInstance().webServer.stationRunning();
      }
   }

   void notifyAppStopped() {
      synchronized (this.runningMonitor) {
         this.currentRunningApps--;
         if (this.currentRunningApps == 0L) {
            NiagaraDaemon.getInstance().webServer.noStationRunning();
         }
      }
   }

   boolean canStartApp() {
      boolean canStart = false;
      synchronized (this.runningMonitor) {
         if (this.currentRunningApps < this.maxRunningApps) {
            canStart = true;
         }

         return canStart;
      }
   }

   public boolean appRunning() {
      synchronized (this.runningMonitor) {
         return this.currentRunningApps > 0L;
      }
   }

   boolean isConsoleAvailable() {
      return this.appConsoleAvailable;
   }

   public void refreshMaxRunningAppCount() {
      synchronized (this.runningMonitor) {
         if (this.appTypeId == 1) {
            this.maxRunningApps = PlatformInfo.getInstance().maxRunningStations();
         } else {
            this.maxRunningApps = 32L;
         }
      }
   }

   long getMaxRunningAppCount() {
      synchronized (this.runningMonitor) {
         return this.maxRunningApps;
      }
   }

   public void resetDaemonSessions() {
      if (this.appTypeId == 1) {
         synchronized (this.registryMonitor) {
            this.apps.stream().filter(App::isAcceptingMessages).forEach(app -> app.sendMessage("resetdaemon"));
         }
      }
   }

   public void sendSecurityAuditEvent(String operation, String username, String message) {
      if (this.appTypeId == 1) {
         synchronized (this.registryMonitor) {
            String auditEventMessage = operation + ";" + username + ";" + message;
            auditEventMessage = Base64.getEncoder().encodeToString(auditEventMessage.getBytes(StandardCharsets.UTF_8));
            String[] securityAuditEvent = new String[]{"securityauditevent", auditEventMessage};
            this.apps.stream().filter(App::isAcceptingMessages).forEach(app -> app.sendMessages(securityAuditEvent));
         }
      }
   }

   public String getAppType() {
      return this.appType;
   }

   String getAppsDirPath() {
      return this.appsDirPath;
   }

   public void setWatchPauseState(boolean paused) {
      if (this.appDirectoryThread != null) {
         this.appDirectoryThread.watchPaused = paused;
      }
   }

   private int updateAppInfo(String propKey, String propValue) {
      int dot = propKey.indexOf(46);
      if (propKey.indexOf(46) == -1) {
         return 0;
      }

      String appName = propKey.substring(0, dot);
      String propName = propKey.substring(dot + 1, propKey.length());
      synchronized (this.registryMonitor) {
         for (App app : this.apps) {
            if (app.getAppName().equalsIgnoreCase(appName)) {
               app.updateAttribute(propName, propValue);
               return 1;
            }
         }

         return 0;
      }
   }

   private void createApp(String name) {
      App s = this.getApp(name);
      if (s == null) {
         synchronized (this.registryMonitor) {
            switch (this.appTypeId) {
               case 1:
                  this.apps.add(new StationApp(name, this, this.filter, this.platformProvider));
            }
         }
      }
   }

   private void startApp(int appIdx) {
      if (!this.apps.get(appIdx).start()) {
         String appName = this.apps.get(appIdx).getAppName();
         MessageBundle msg = new MessageBundle("platform", "AppRegistry.startError", this.appType, "start failed for " + this.appType + " " + appName);
         msg.addLexiconArgument(appName);
         this.filter.severe("start failed for " + this.appType + " " + appName);
      }
   }

   private void dirWatchStop() {
      if (this.appDirectoryThread != null) {
         this.appDirectoryThread.stopRequested = true;
         this.appDirectoryThread.interrupt();

         try {
            if (this.appDirectoryThread != null) {
               this.appDirectoryThread.join();
            }
         } catch (InterruptedException var2) {
         }

         if (this.appDirectoryThread != null) {
            this.appDirectoryThread.stopRequested = false;
            this.appDirectoryThread = null;
         }
      }
   }

   private void dirWatchStart() {
      if (this.appDirectoryThread == null) {
         if (AppRegistry.LocalMetaDataHolder.WATCH_STATION_DIRECTORY != null) {
            if (!Boolean.parseBoolean(AppRegistry.LocalMetaDataHolder.WATCH_STATION_DIRECTORY)) {
               return;
            }
         } else if (this.platformProvider.isEmbedded()) {
            return;
         }

         this.appDirectoryThread = new AppRegistry.WatchDirectoryThread();
         this.appDirectoryThread.start();
      }
   }

   protected static final class LocalMetaDataHolder {
      protected static final String JAVA_RUNTIME_VERSION = AccessController.doPrivileged(() -> System.getProperty("java.runtime.version", "unknown"));
      protected static final String JAVA_VM_VENDOR = AccessController.doPrivileged(() -> System.getProperty("java.vm.vendor", "unknown"));
      private static final String WATCH_STATION_DIRECTORY = AccessController.doPrivileged(() -> System.getProperty("niagarad.watchStationDir"));
   }

   public class WatchDirectoryThread extends Thread {
      volatile boolean stopRequested = false;
      volatile boolean watchPaused = false;

      public WatchDirectoryThread() {
         super("Niagarad:WatchDirectory");
      }

      @Override
      public void run() {
         this.watchAppsDirectory();
      }

      private void watchAppsDirectory() {
         boolean dirChanged = false;
         File applicationsDirectory = new File(AppRegistry.this.appsDirPath);
         if (!applicationsDirectory.exists()) {
            AppRegistry.this.filter.severe("requested directory does not exist, can not watch, returning");
         } else {
            AppRegistry.this.appDirectoryThread.stopRequested = false;
            long lastModification = applicationsDirectory.lastModified();

            while (true) {
               try {
                  Thread.sleep(5000L);
               } catch (InterruptedException var18) {
               }

               if (AppRegistry.this.appDirectoryThread.stopRequested) {
                  AppRegistry.this.appDirectoryThread.stopRequested = false;
                  break;
               }

               if (AppRegistry.this.appDirectoryThread.watchPaused && AppRegistry.this.filter.isLoggable(Level.FINE)) {
                  AppRegistry.this.filter.fine("watch apps directory thread paused, temporarily ignoring directory events");
               } else {
                  if (lastModification != applicationsDirectory.lastModified()) {
                     dirChanged = true;
                     lastModification = applicationsDirectory.lastModified();
                  }

                  if (dirChanged) {
                     if (AppRegistry.this.filter.isLoggable(Level.FINE)) {
                        AppRegistry.this.filter.fine("watch apps directory thread detected content change, refreshing app list");
                     }

                     synchronized (AppRegistry.this.registryMonitor) {
                        dirChanged = false;
                        ArrayList<App> toRemove = new ArrayList<>();
                        ArrayList<String> toAdd = new ArrayList<>();
                        AppRegistry.this.apps
                           .stream()
                           .filter(current -> !current.isActive())
                           .forEach(
                              current -> {
                                 File applicationDirectory = new File(applicationsDirectory, current.getAppName());
                                 if (!applicationDirectory.exists()) {
                                    if (AppRegistry.this.filter.isLoggable(Level.FINE)) {
                                       AppRegistry.this.filter
                                          .fine("watch apps directory thread removing stale " + current.getAppType() + " " + current.getAppName());
                                    }

                                    toRemove.add(current);
                                 }
                              }
                           );
                        toRemove.forEach(app -> AppRegistry.this.removeApp(app.getAppName()));
                        File[] children = applicationsDirectory.listFiles();
                        if (children != null) {
                           for (File child : children) {
                              if (child.isDirectory() && AppRegistry.this.getApp(child.getName()) == null) {
                                 for (File childFiles : child.listFiles()) {
                                    if (childFiles.getName().equalsIgnoreCase("config.bog")) {
                                       if (AppRegistry.this.filter.isLoggable(Level.FINE)) {
                                          AppRegistry.this.filter
                                             .fine("watch apps directory thread adding new " + AppRegistry.this.getAppType() + " " + child.getName());
                                       }

                                       toAdd.add(child.getName());
                                       break;
                                    }
                                 }
                              }
                           }
                        }

                        toAdd.forEach(app -> AppRegistry.this.createApp(app));
                     }
                  }

                  if (AppRegistry.this.appDirectoryThread.stopRequested) {
                     AppRegistry.this.appDirectoryThread.stopRequested = false;
                     break;
                  }
               }
            }
         }
      }
   }
}
