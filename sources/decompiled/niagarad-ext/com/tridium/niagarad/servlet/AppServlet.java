package com.tridium.niagarad.servlet;

import com.tridium.crypto.core.io.CryptoSupport;
import com.tridium.niagarad.app.App;
import com.tridium.niagarad.app.AppRegistry;
import com.tridium.niagarad.log.ErrorHandler;
import com.tridium.niagarad.log.MessageBundle;
import com.tridium.niagarad.util.CsrfTokenUtil;
import com.tridium.niagarad.util.DaemonAuthUtil;
import com.tridium.niagarad.util.KeyedList;
import com.tridium.nre.util.BogModUtil;
import com.tridium.nre.util.BogModUtil.SlotInfo;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import javax.baja.nre.util.TextUtil;
import javax.baja.xml.XWriter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class AppServlet extends DaemonServlet {
   Map<String, String> upgradeRequestTokens;
   public static final int MAX_UPGRADE_TOKENS = 10;
   public static final SlotInfo WEB_TLS_SLOT = new SlotInfo("web", "WebService", "baja", "SslTlsEnum", "httpsMinProtocol");
   public static final SlotInfo FOX_TLS_SLOT = new SlotInfo("fox", "FoxService", "baja", "SslTlsEnum", "foxsMinProtocol");
   private final AppRegistry registry;

   public AppServlet(String servletName, AppRegistry registry) {
      super(servletName);
      this.registry = registry;
      this.upgradeRequestTokens = Collections.synchronizedMap(new LinkedHashMap<String, String>() {
         @Override
         protected boolean removeEldestEntry(Entry<String, String> eldest) {
            return this.size() > 10;
         }
      });
   }

   @Override
   public boolean useDefaultAuthentication() {
      return false;
   }

   @Override
   public boolean authenticate(HttpServletRequest request, HttpServletResponse response) {
      if (request.getHeader("Upgrade") != null) {
         String providedToken = request.getHeader("UpgradeRequestToken");
         if (providedToken == null) {
            return false;
         }

         String tokenKey = request.getRemoteAddr() + providedToken;
         String expectedToken = this.upgradeRequestTokens.get(tokenKey);
         if (expectedToken == null) {
            return false;
         }

         String[] tokenAttributes = TextUtil.split(expectedToken, ':');
         String expectedTokenValue = tokenAttributes[0];

         try {
            long generation = Long.valueOf(tokenAttributes[1]);
            long expiration = Long.valueOf(tokenAttributes[2]);
            long now = System.currentTimeMillis();
            if (now < generation || now > expiration) {
               this.upgradeRequestTokens.remove(tokenKey);
               return false;
            }

            if (providedToken.equals(expectedTokenValue)) {
               this.upgradeRequestTokens.remove(tokenKey);
               return true;
            }
         } catch (NumberFormatException var14) {
         }

         return false;
      } else {
         return DaemonAuthUtil.authUser(this.getServer().getAuthenticator(), request, response);
      }
   }

   @Override
   public int doGet(HttpServletRequest request, ErrorHandler handler, KeyedList query, XWriter content) {
      String appName = query.get(this.registry.getAppType(), "");
      String action = query.get("action", "");
      if (action != null && !action.isEmpty()) {
         boolean update = false;
         switch (action.toLowerCase()) {
            case "clearoutput":
            case "delete":
            case "stop":
            case "kill":
            case "start":
            case "update":
            case "tell":
            case "threads":
            case "setalias":
            case "setpausestate":
               update = true;
            default:
               if (update && !DebugServlet.debugEnabled && !CsrfTokenUtil.verifyCsrfToken(request, query.get("csrfToken", null))) {
                  MessageBundle msg = new MessageBundle("invalid CSRF token in request");
                  handler.error(msg);
                  return 403;
               } else {
                  int result;
                  if (action.equalsIgnoreCase("clearoutput")) {
                     result = this.clearAppOutput(appName, handler);
                  } else if (action.equalsIgnoreCase("delete")) {
                     result = this.deleteApp(appName, handler);
                  } else if (action.equalsIgnoreCase("list")) {
                     result = this.listApps(content);
                  } else if (action.equalsIgnoreCase("stop")) {
                     result = this.stopApp(appName, query, handler);
                  } else if (action.equalsIgnoreCase("kill")) {
                     result = this.killApp(appName, query, handler);
                  } else if (action.equalsIgnoreCase("wait")) {
                     result = this.waitAppTermination(appName, handler);
                  } else if (action.equalsIgnoreCase("start")) {
                     result = this.startApp(appName, handler);
                  } else if (action.equalsIgnoreCase("tell")) {
                     result = this.tellApp(appName, query, handler);
                  } else if (action.equalsIgnoreCase("update")) {
                     result = this.updateApp(appName, query, handler);
                  } else if (action.equalsIgnoreCase("threads")) {
                     result = this.dumpThreads(appName, handler);
                  } else if (action.equalsIgnoreCase("getoutput")) {
                     result = this.getAppOutput(request, appName, handler, content);
                  } else if (action.equalsIgnoreCase("setalias")) {
                     result = this.setStationCertificateAlias(handler, query);
                  } else if (action.equalsIgnoreCase("setTlsVersion")) {
                     result = this.setStationTlsVersion(handler, query);
                  } else if (action.equalsIgnoreCase("setPauseState")) {
                     result = this.setWatchPauseState(handler, query);
                  } else {
                     handler.error(new MessageBundle("AppServlet: Unrecognized action \"" + action + "\""));
                     result = 400;
                  }

                  return result;
               }
         }
      } else {
         MessageBundle msg = new MessageBundle("platform", "Servlet.missingParam", "action", "AppServlet: Missing action argument");
         handler.error(msg);
         return 400;
      }
   }

   public int getAppOutput(HttpServletRequest request, String appName, ErrorHandler handler, XWriter content) {
      if (appName != null && !appName.isEmpty()) {
         App app = this.registry.getApp(appName);
         if (app == null) {
            MessageBundle msg = new MessageBundle("platform", "DaemonServlet.stationNotFound", "AppServlet: App not found");
            handler.error(msg);
            return 400;
         } else {
            byte[] token = new byte[16];
            new SecureRandom().nextBytes(token);
            long tokenTimeout = request.isSecure() ? 60000L : 10000L;
            long generated = System.currentTimeMillis();
            long expires = generated + tokenTimeout;
            String tokenString = TextUtil.bytesToHexString(token);
            this.upgradeRequestTokens.put(request.getRemoteAddr() + tokenString, tokenString + ":" + generated + ":" + expires);
            content.w("<upgraderequest token='" + tokenString + "'/>");
            return 200;
         }
      } else {
         MessageBundle msg = new MessageBundle("platform", "Servlet.missingParam", "station", "AppServlet: Missing station argument");
         handler.error(msg);
         return 400;
      }
   }

   public int clearAppOutput(String appName, ErrorHandler handler) {
      if (appName != null && !appName.isEmpty()) {
         App app = this.registry.getApp(appName);
         if (app != null) {
            app.clearOutput();
            return 200;
         } else {
            MessageBundle msg = new MessageBundle("platform", "DaemonServlet.stationNotFound", "AppServlet: App not found");
            handler.error(msg);
            return 400;
         }
      } else {
         MessageBundle msg = new MessageBundle("platform", "Servlet.missingParam", "station", "AppServlet: Missing station argument");
         handler.error(msg);
         return 400;
      }
   }

   public int deleteApp(String appName, ErrorHandler handler) {
      if (appName != null && !appName.isEmpty()) {
         this.registry.removeApp(appName);
         return 200;
      } else {
         MessageBundle msg = new MessageBundle("platform", "Servlet.missingParam", "station", "AppServlet: Missing station argument");
         handler.error(msg);
         return 400;
      }
   }

   public int listApps(XWriter content) {
      content.w("<").w(this.registry.getAppType()).w("s>\n");
      this.registry.listApps(content);
      content.w("</").w(this.registry.getAppType()).w("s>\n");
      return 200;
   }

   public int stopApp(String appName, KeyedList query, ErrorHandler handler) {
      if (appName != null && !appName.isEmpty()) {
         String overrideString = query.get("restartOverride", String.valueOf(0));

         int overrideValue;
         try {
            overrideValue = Integer.valueOf(overrideString);
            if (overrideValue < 0 || overrideValue > 2) {
               throw new NumberFormatException();
            }
         } catch (NumberFormatException nfe) {
            MessageBundle msg = new MessageBundle(
               "platform", "Servlet.invalidOverride", overrideString, "AppServlet: Invalid app restart override value " + overrideString + " specified"
            );
            handler.error(msg);
            return 400;
         }

         if (appName.equals("all")) {
            this.registry.stopAllApps(overrideValue);
         } else {
            this.registry.stopApp(appName, overrideValue);
         }

         return 200;
      } else {
         MessageBundle msg = new MessageBundle("platform", "Servlet.missingParam", "station", "AppServlet: No station specified");
         handler.error(msg);
         return 400;
      }
   }

   public int killApp(String appName, KeyedList query, ErrorHandler handler) {
      if (appName != null && !appName.isEmpty()) {
         App app = this.registry.getApp(appName);
         if (app != null) {
            boolean block = Boolean.valueOf(query.get("block", "false"));
            String overrideString = query.get("restartOverride", String.valueOf(0));

            int overrideValue;
            try {
               overrideValue = Integer.valueOf(overrideString);
               if (overrideValue < 0 || overrideValue > 2) {
                  throw new NumberFormatException();
               }
            } catch (NumberFormatException nfe) {
               MessageBundle msg = new MessageBundle(
                  "platform", "Servlet.invalidOverride", overrideString, "AppServlet: Invalid app restart override value " + overrideString + " specified"
               );
               handler.error(msg);
               return 400;
            }

            app.kill(overrideValue, block);
            return 200;
         } else {
            MessageBundle msg = new MessageBundle("platform", "DaemonServlet.stationNotFound", "AppServlet: App not found");
            handler.error(msg);
            return 400;
         }
      } else {
         MessageBundle msg = new MessageBundle("platform", "Servlet.missingParam", "app", "AppServlet: No app specified");
         handler.error(msg);
         return 400;
      }
   }

   public int startApp(String appName, ErrorHandler handler) {
      if (appName != null && !appName.isEmpty()) {
         App app = this.registry.getApp(appName);
         if (app != null) {
            app.start();
            return 200;
         } else {
            MessageBundle msg = new MessageBundle("platform", "DaemonServlet.stationNotFound", "AppServlet: App not found");
            handler.error(msg);
            return 400;
         }
      } else {
         MessageBundle msg = new MessageBundle("platform", "Servlet.missingParam", "station", "AppServlet: Missing station argument");
         handler.error(msg);
         return 400;
      }
   }

   public int tellApp(String appName, KeyedList query, ErrorHandler handler) {
      if (appName != null && !appName.isEmpty()) {
         App app = this.registry.getApp(appName);
         if (app != null) {
            String message = query.get("message", "");
            if (!message.isEmpty()) {
               app.sendMessage(message);
               return 200;
            } else {
               MessageBundle msg = new MessageBundle("platform", "Servlet.missingParam", "message", "AppServlet: Missing message argument");
               handler.error(msg);
               return 400;
            }
         } else {
            MessageBundle msg = new MessageBundle("platform", "DaemonServlet.stationNotFound", "AppServlet: App not found");
            handler.error(msg);
            return 400;
         }
      } else {
         MessageBundle msg = new MessageBundle("platform", "Servlet.missingParam", "station", "AppServlet: Missing station argument");
         handler.error(msg.toString());
         return 400;
      }
   }

   public int dumpThreads(String appName, ErrorHandler handler) {
      if (appName != null && !appName.isEmpty()) {
         App app = this.registry.getApp(appName);
         if (app != null) {
            app.generateStackDump();
            return 200;
         } else {
            MessageBundle msg = new MessageBundle("platform", "DaemonServlet.stationNotFound", "AppServlet: App not found");
            handler.error(msg);
            return 400;
         }
      } else {
         MessageBundle msg = new MessageBundle("platform", "Servlet.missingParam", "station", "AppServlet: Missing station argument");
         handler.error(msg);
         return 400;
      }
   }

   public int updateApp(String appName, KeyedList query, ErrorHandler handler) {
      int result = 200;
      App app = null;
      if (appName != null) {
         app = this.registry.getApp(appName);
      }

      if (app == null) {
         this.registry.loadProperties();
      } else {
         if (query.containsKey("isdisabled")) {
            app.setIsDisabled(Boolean.valueOf(query.get("isdisabled", "false")));
         }

         if (query.containsKey("isautorestart")) {
            app.setIsAutoRestart(Boolean.valueOf(query.get("isautorestart", "true")));
         }

         if (query.containsKey("isautostart")) {
            app.setIsAutoStart(Boolean.valueOf(query.get("isautostart", "true")));
         }

         if (query.containsKey("logbuffersize")) {
            String logBufferString = query.get("logbuffersize", String.valueOf(262144));

            int logBufferValue;
            try {
               logBufferValue = Integer.valueOf(logBufferString);
               if (logBufferValue < 8192 || logBufferValue > 524288) {
                  throw new NumberFormatException();
               }
            } catch (NumberFormatException nfe) {
               MessageBundle msg = new MessageBundle(
                  "platform", "Servlet.invalidLogBufferSize", logBufferString, "AppServlet: Invalid log buffer size " + logBufferString + " specified"
               );
               handler.error(msg);
               return 400;
            }

            int previousValue = app.getAppOut().getMemBufferSize();
            if (app.getAppOut().resetMemBufferSize(logBufferValue) != 0) {
               app.getAppOut().resetMemBufferSize(previousValue);
               result = 400;
            }
         }

         if (query.containsKey("logbufferfilesize")) {
            String logBufferFileString = query.get("logbufferfilesize", String.valueOf(262144));

            int logBufferFileValue;
            try {
               logBufferFileValue = Integer.valueOf(logBufferFileString);
               if (logBufferFileValue < 8192) {
                  throw new NumberFormatException();
               }
            } catch (NumberFormatException nfe) {
               MessageBundle msg = new MessageBundle(
                  "platform",
                  "Servlet.invalidLogBufferSize",
                  logBufferFileString,
                  "AppServlet: Invalid log buffer file size " + logBufferFileString + " specified"
               );
               handler.error(msg);
               return 400;
            }

            app.setLogBufferFileSize(logBufferFileValue);
         }

         app.setIsDirty(true);
         if (query.containsKey("save")) {
            this.registry.saveAppProperties(app, true);
         }
      }

      return result;
   }

   public int waitAppTermination(String appName, ErrorHandler handler) {
      if (appName != null && !appName.isEmpty()) {
         if (appName.equals("all")) {
            this.registry.waitForAllTerminated();
         } else {
            int rc = this.registry.waitForAppTermination(appName);
            if (rc != 0) {
               MessageBundle msg = new MessageBundle("platform", "DaemonServlet.stationNotFound", "AppServlet: App not found");
               handler.error(msg);
               return 400;
            }
         }

         return 200;
      } else {
         MessageBundle msg = new MessageBundle("platform", "Servlet.missingParam", "station", "AppServlet: No station specified");
         handler.error(msg);
         return 400;
      }
   }

   private int setStationCertificateAlias(ErrorHandler handler, KeyedList query) {
      int result = this.checkMandatoryParams(handler, query, "alias", this.registry.getAppType());
      if (result != 200) {
         return result;
      } else {
         String alias = query.get("alias", null);
         String stationName = query.get(this.registry.getAppType(), null);
         App station = this.registry.getApp(stationName);
         if (station == null) {
            MessageBundle msg = new MessageBundle("platform", "DaemonServlet.stationNotFound", "AppServlet: App not found");
            handler.error(msg);
            return 400;
         } else if (station.isActive()) {
            MessageBundle msg = new MessageBundle(
               "platform", "AppServlet.setAliasOnActiveStation", "AppServlet: Cannot update certificate alias on active station"
            );
            handler.error(msg);
            return 400;
         } else {
            MessageBundle msg = new MessageBundle(
               "platform",
               "AppServlet.setCertAliasNotSupported",
               "AppServlet: Setting Fox and Web Service TLS certificate alias on an offline station not supported in this version"
            );
            handler.error(msg);
            return 400;
         }
      }
   }

   private int setStationTlsVersion(ErrorHandler handler, KeyedList query) {
      int result = this.checkMandatoryParams(handler, query, "tlsVersion", this.registry.getAppType());
      if (result != 200) {
         return result;
      } else {
         String tlsVersion = query.get("tlsVersion", null);
         String stationName = query.get(this.registry.getAppType(), null);
         if (!CryptoSupport.VALID_TLS_TAGS.contains(tlsVersion)) {
            MessageBundle msg = new MessageBundle(
               "platform", "AppServlet.unrecognizedTlsVersion", tlsVersion, "AppServlet: Unrecognized TLS version " + tlsVersion
            );
            handler.error(msg);
            return 400;
         } else {
            App station = this.registry.getApp(stationName);
            if (station == null) {
               MessageBundle msg = new MessageBundle("platform", "DaemonServlet.stationNotFound", "AppServlet: App not found");
               handler.error(msg);
               return 400;
            } else if (station.isActive()) {
               MessageBundle msg = new MessageBundle("platform", "AppServlet.setTlsOnActiveStation", "AppServlet: Cannot update TLS version on active station");
               handler.error(msg);
               return 400;
            } else {
               Map<SlotInfo, String> replacements = new HashMap<>();
               replacements.put(FOX_TLS_SLOT, tlsVersion);
               replacements.put(WEB_TLS_SLOT, tlsVersion);
               BogModUtil.replaceValues(station.getBogPath(), replacements);
               return 200;
            }
         }
      }
   }

   private int setWatchPauseState(ErrorHandler handler, KeyedList query) {
      int result = this.checkMandatoryParams(handler, query, "pauseState");
      if (result != 200) {
         return result;
      }

      String pauseWatch = query.get("pauseState", "false");
      this.registry.setWatchPauseState(Boolean.valueOf(pauseWatch));
      return 200;
   }
}
