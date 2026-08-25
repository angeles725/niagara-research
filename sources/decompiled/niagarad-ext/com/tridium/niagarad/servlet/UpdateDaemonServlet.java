package com.tridium.niagarad.servlet;

import com.tridium.crypto.core.cert.JarSignatureRegistry;
import com.tridium.crypto.core.io.CoreCryptoManager;
import com.tridium.crypto.core.io.CoreTrustStore;
import com.tridium.crypto.core.io.CryptoSupport;
import com.tridium.crypto.core.io.ICoreKeyStore;
import com.tridium.json.JSONObject;
import com.tridium.niagarad.NiagaraDaemon;
import com.tridium.niagarad.crypto.DaemonCryptoManager;
import com.tridium.niagarad.license.LicenseManager;
import com.tridium.niagarad.log.ErrorHandler;
import com.tridium.niagarad.log.MessageBundle;
import com.tridium.niagarad.log.NullLogger;
import com.tridium.niagarad.platform.PlatformInfo;
import com.tridium.niagarad.util.CsrfTokenUtil;
import com.tridium.niagarad.util.KeyedList;
import com.tridium.nre.platform.IPlatformProvider;
import com.tridium.nre.security.Aes256PasswordEncoderUtil;
import com.tridium.nre.security.KeyRing;
import com.tridium.nre.security.SecretChars;
import com.tridium.nre.security.SecurityInitializer;
import com.tridium.nre.subscription.AccessTokenApi;
import com.tridium.nre.subscription.DeviceCodeApi;
import com.tridium.nre.subscription.RegistrationApi;
import com.tridium.nre.subscription.SubscriptionLicenseUtil;
import com.tridium.nre.subscription.SubscriptionMetadataUtil;
import com.tridium.nre.subscription.AccessTokenApi.Poll;
import com.tridium.nre.subscription.EntitlementApi.EntitlementStatus;
import com.tridium.nre.util.LicenseMode;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.security.AccessController;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.nre.security.SharedSecretKey;
import javax.baja.nre.security.TlsCipherSuiteGroup;
import javax.baja.xml.XWriter;
import javax.servlet.http.HttpServletRequest;

public class UpdateDaemonServlet extends DaemonServlet {
   private Logger filter;
   private volatile EntitlementStatus accessTokenStatus;
   private final Object statusLock = new Object();
   private final IPlatformProvider platformProvider;

   public UpdateDaemonServlet(IPlatformProvider platformProvider) {
      super("updatedaemon");
      this.platformProvider = platformProvider;
   }

   @Override
   public boolean doStart() {
      this.filter = Logger.getLogger("updatedaemon");
      return true;
   }

   @Override
   public int doGet(HttpServletRequest request, ErrorHandler handler, KeyedList query, XWriter content) {
      if (query == null) {
         MessageBundle msg = new MessageBundle("invalid query provided");
         handler.error(msg);
         this.filter.severe("invalid query provided");
         return 400;
      }

      if (!DebugServlet.debugEnabled && query.size() > 0 && !CsrfTokenUtil.verifyCsrfToken(request, query.get("csrfToken", null))) {
         MessageBundle msg = new MessageBundle("invalid CSRF token in request");
         handler.error(msg);
         this.filter.severe("invalid CSRF token in request");
         return 403;
      }

      boolean anyUpdates = false;
      boolean restartWeb = false;
      if (query.containsKey("runtimeProfilesEnabled")) {
         String runtimeProfilesEnabled = query.get("runtimeProfilesEnabled", "");
         if (runtimeProfilesEnabled.trim().isEmpty()) {
            NiagaraDaemon.props.remove("runtimeProfilesEnabled");
         } else {
            runtimeProfilesEnabled = runtimeProfilesEnabled.trim();
            String[] candidateProfiles = runtimeProfilesEnabled.split(",");
            Set<String> supportedProfiles = new HashSet<>();
            Collections.addAll(supportedProfiles, this.platformProvider.getSupportedRuntimeProfiles().split(","));

            for (String candidate : candidateProfiles) {
               if (!supportedProfiles.contains(candidate)) {
                  MessageBundle msg = new MessageBundle("Invalid runtime profile \"" + candidate + "\" specified");
                  handler.error(msg);
                  this.filter.severe("invalid runtime profile \"" + candidate + "\" specified");
                  return 400;
               }
            }

            NiagaraDaemon.props.setProperty("runtimeProfilesEnabled", runtimeProfilesEnabled);
         }

         anyUpdates = true;
      }

      if (query.containsKey("allowStationRestart")) {
         String value = query.get("allowStationRestart", "false");
         NiagaraDaemon.props.setProperty("allowStationRestart", value);
         anyUpdates = true;
      }

      if (query.containsKey("failureRebootLimit")) {
         String value = query.get("failureRebootLimit", "3");

         try {
            int rebootValue = Integer.parseInt(value);
            if (rebootValue < 0 || rebootValue > 32) {
               throw new NumberFormatException();
            }
         } catch (NumberFormatException nfe) {
            MessageBundle msg = new MessageBundle("Invalid failure reboot limit \"" + value + "\" specified");
            handler.error(msg);
            this.filter.severe("invalid failure reboot limit \"" + value + "\" specified");
            return 400;
         }

         NiagaraDaemon.props.setProperty("failureRebootLimit", value);
         anyUpdates = true;
      }

      if (query.containsKey("failureRebootLimitPeriod")) {
         String value = query.get("failureRebootLimitPeriod", "600000");

         try {
            long limitValue = Long.parseLong(value);
            if (limitValue < 0L) {
               throw new NumberFormatException();
            }
         } catch (NumberFormatException nfe) {
            MessageBundle msg = new MessageBundle("Invalid failure reboot limit period \"" + value + "\" specified");
            handler.error(msg);
            this.filter.severe("invalid failure reboot limit period \"" + value + "\" specified");
            return 400;
         }

         NiagaraDaemon.props.setProperty("failureRebootLimitPeriod", value);
         anyUpdates = true;
      }

      if (query.containsKey("port")) {
         String value = query.get("port", "3011");

         int portValue;
         try {
            portValue = Integer.parseInt(value);
            if (portValue <= 1024 || portValue > 65535) {
               throw new NumberFormatException();
            }
         } catch (NumberFormatException nfe) {
            MessageBundle msg = new MessageBundle("Invalid daemon port number \"" + value + "\" specified");
            handler.error(msg);
            this.filter.severe("invalid daemon port number \"" + value + "\" specified");
            return 400;
         }

         if (NiagaraDaemon.getInstance().webServer.getHttpPort() != portValue) {
            if (!NiagaraDaemon.serverPortAvailable(portValue)) {
               MessageBundle msg = new MessageBundle("Specified daemon port number " + portValue + " conflicts with port in use");
               handler.error(msg);
               this.filter.severe("specified daemon port number " + portValue + " conflicts with port in use");
               return 400;
            }

            NiagaraDaemon.getInstance().updateHttpPort(portValue);
            anyUpdates = true;
            restartWeb = true;
         }
      }

      boolean sslEnabledStateReadonly = Boolean.parseBoolean(DaemonCryptoManager.SSL_ENABLED_READONLY_VALUE);
      if (!sslEnabledStateReadonly && query.containsKey("sslEnabled")) {
         restartWeb = true;
         anyUpdates = true;
         String value = query.get("sslEnabled", "true");
         if (value.equals("true")) {
            NiagaraDaemon.props.setProperty("sslEnabled", "true");
         } else {
            NiagaraDaemon.props.setProperty("sslEnabled", "false");
         }
      }

      if (!sslEnabledStateReadonly && query.containsKey("sslOnly")) {
         restartWeb = true;
         anyUpdates = true;
         String value = query.get("sslOnly", DaemonCryptoManager.DEFAULT_SSL_ONLY);
         if (value.equals("true")) {
            NiagaraDaemon.props.setProperty("sslOnly", "true");
         } else {
            NiagaraDaemon.props.setProperty("sslOnly", "false");
         }
      }

      if (query.containsKey("sslPort")) {
         String value = query.get("sslPort", "5011");

         int portValue;
         try {
            portValue = Integer.parseInt(value);
            if (portValue <= 1024 || portValue > 65535) {
               throw new NumberFormatException();
            }
         } catch (NumberFormatException nfe) {
            MessageBundle msg = new MessageBundle("Invalid daemon secure port number \"" + value + "\" specified");
            handler.error(msg);
            this.filter.severe("invalid daemon secure port number \"" + value + "\" specified");
            return 400;
         }

         if (NiagaraDaemon.getInstance().webServer.getHttpsPort() != portValue) {
            if (!NiagaraDaemon.serverPortAvailable(portValue)) {
               MessageBundle msg = new MessageBundle("Specified daemon secure port number " + portValue + " conflicts with port in use");
               handler.error(msg);
               this.filter.severe("specified daemon secure port number " + portValue + " conflicts with port in use");
               return 400;
            }

            NiagaraDaemon.getInstance().updateHttpsPort(portValue);
            anyUpdates = true;
            restartWeb = true;
         }
      }

      if (query.containsKey("keyAlias")) {
         restartWeb = true;
         anyUpdates = true;
         String keyAlias = query.get("keyAlias", "default");

         try {
            CoreCryptoManager ccm = CoreCryptoManager.get(NiagaraDaemon.getSecurityInfoProvider());
            ICoreKeyStore keyStore = ccm.getKeyStore();
            if (keyStore instanceof CoreTrustStore) {
               ((CoreTrustStore)keyStore).checkLastModified();
            }

            X509Certificate sslServerCertificate = keyStore.getCertificate(keyAlias);
            if (sslServerCertificate == null) {
               throw new Exception("Certificate for key alias \"" + keyAlias + "\" not found");
            }

            String keyPassphrase = null;
            String encodedKeyEntry = null;
            if (query.containsKey("keyPassphrase")) {
               String sharedKeyInQuery = query.get("sharedKeyName", null);
               if (sharedKeyInQuery == null) {
                  throw new Exception("No shared key found in query");
               }

               String encodedKeyPasswordValue = query.get("keyPassphrase", null);
               String sharedKeyAttributeName = "sharedKey_" + query.get("sharedKeyName", null);
               SharedSecretKey sharedKey = (SharedSecretKey)request.getSession(false).getAttribute(sharedKeyAttributeName);

               try {
                  keyPassphrase = sharedKey.decrypt(Base64.getDecoder().decode(encodedKeyPasswordValue)).asString(true, StandardCharsets.UTF_8);
                  KeyRing keyRing = SecurityInitializer.getInstance().getSecurityInfoProvider().getKeyRing();
                  encodedKeyEntry = Aes256PasswordEncoderUtil.encodePassword(
                     keyRing, "com.tridium.niagarad.web.sslKeyPass", new SecretChars(keyPassphrase.toCharArray(), true)
                  );
               } catch (Exception e) {
                  throw new Exception("Error decrypting key password(s) sent from client", e);
               }
            }

            PrivateKey privateKey = (PrivateKey)ccm.getKeyStore().getKey(keyAlias, keyPassphrase == null ? null : keyPassphrase.toCharArray());
            if (privateKey == null) {
               throw new Exception("Certificate for key alias \"" + keyAlias + "\" not found or not retrievable");
            }

            NiagaraDaemon.props.setProperty("keyAlias", keyAlias);
            if (encodedKeyEntry != null) {
               NiagaraDaemon.props.setProperty("keyPassphrase", encodedKeyEntry);
            } else {
               NiagaraDaemon.props.remove("keyPassphrase");
            }
         } catch (Exception e) {
            MessageBundle msg = new MessageBundle("certificate exception occurred handling key alias/password request (" + e + ")");
            handler.error(msg);
            this.filter.log(Level.SEVERE, "certificate exception occurred handling key alias/password request (" + e + ")");
            if (this.filter.isLoggable(Level.FINE)) {
               this.filter.log(Level.SEVERE, "Stack trace: ", e);
            }

            return 400;
         }
      }

      if (query.containsKey("sslAlgType")) {
         restartWeb = true;
         anyUpdates = true;
         String value = query.get("sslAlgType", "tlsv1_3");
         boolean invalidType;
         if (SecurityInitializer.getInstance().isFips()) {
            invalidType = !DaemonCryptoManager.isTlsAlgFipsApproved(value);
         } else {
            invalidType = CryptoSupport.TYPES.get(value) == null;
         }

         if (invalidType) {
            MessageBundle msg = new MessageBundle("Invalid daemon ssl algorithm \"" + value + "\" specified");
            handler.error(msg);
            this.filter.severe("invalid daemon ssl algorithm \"" + value + "\" specified");
            return 400;
         }

         NiagaraDaemon.props.setProperty("sslAlgType", value);
      }

      if (query.containsKey("tlsCipherSuiteGroup")) {
         restartWeb = true;
         anyUpdates = true;
         String value = query.get("tlsCipherSuiteGroup", DaemonCryptoManager.DEFAULT_TLS_CIPHER_SUITE_GROUP);

         try {
            TlsCipherSuiteGroup type = TlsCipherSuiteGroup.valueOf(value);
            NiagaraDaemon.props.setProperty("tlsCipherSuiteGroup", type.toString());
         } catch (Exception e) {
            MessageBundle msg = new MessageBundle("Invalid daemon tls cipher suite \"" + value + "\" specified");
            handler.error(msg);
            this.filter.severe("invalid daemon tls cipher suite \"" + value + "\" specified");
            return 400;
         }
      }

      if (query.containsKey("tlsUseExtendedMasterSecret")) {
         anyUpdates = true;
         String value = query.get("tlsUseExtendedMasterSecret", "true");
         if (!"true".equals(value) && !"false".equals(value)) {
            MessageBundle msg = new MessageBundle("Invalid option \"" + value + "\" specified for \"useExtendedMasterSecret\"");
            handler.error(msg);
            this.filter.severe("invalid option \"" + value + "\" specified for \"useExtendedMasterSecret\"");
            return 400;
         }

         NiagaraDaemon.props.setProperty("tlsUseExtendedMasterSecret", value);
      }

      if (anyUpdates) {
         NiagaraDaemon.saveProperties();
      }

      if (query.containsKey("reloadLicenses")) {
         LicenseManager.getInstance(NullLogger.getInstance()).reload(NullLogger.getInstance());
         PlatformInfo.getInstance().refreshMaxRunningAppCounts();
         if (NiagaraDaemon.getInstance().getStationRegistry() != null) {
            NiagaraDaemon.getInstance().getStationRegistry().refreshMaxRunningAppCount();
         }
      }

      if (query.containsKey("reloadLicenseMode")) {
         SubscriptionLicenseUtil.reinitializeLicenseMode();
         SubscriptionLicenseUtil.createSubscriptionLicCertDirectory();
      }

      if (query.containsKey("subscriptionMode")) {
         String licenseMode = query.get("subscriptionMode", "PERPETUAL");

         try {
            String value = "false";
            if (LicenseMode.valueOf(licenseMode) == LicenseMode.SUBSCRIPTION) {
               value = "true";
            }

            SubscriptionMetadataUtil.addMetadata("license.subscriptionMode", value);
         } catch (IOException e) {
            MessageBundle msg = new MessageBundle("Exception occurred while writing license.properties key (" + e + ')');
            handler.error(msg);
            this.filter.log(Level.SEVERE, "exception occurred while writing license.properties key (" + e + ')');
            if (this.filter.isLoggable(Level.FINE)) {
               this.filter.log(Level.SEVERE, "Stack trace: ", e);
            }

            return 500;
         }
      }

      if (query.containsKey("updateSubscriptionLicense")) {
         if (SubscriptionLicenseUtil.getLicenseMode() != LicenseMode.SUBSCRIPTION) {
            MessageBundle msg = new MessageBundle("Received \"updateSubscriptionLicense\" message while not in subscription mode");
            handler.error(msg);
            this.filter.log(Level.SEVERE, "received \"updateSubscriptionLicense\" message while not in subscription mode");
            return 400;
         } else {
            return AccessController.doPrivileged(
               () -> {
                  try {
                     SubscriptionLicenseUtil slu = this.getSubscriptionLicenseUtil();
                     EntitlementStatus entitlementStatus = slu.getLicenseUpdate();
                     if (!entitlementStatus.isSuccess()) {
                        String errorMessage = "Host ID license request failed [Code = '"
                           + entitlementStatus.getCode()
                           + "' Message = '"
                           + entitlementStatus.getMessage()
                           + "']";
                        MessageBundle msgx = new MessageBundle(errorMessage);
                        handler.error(msgx);
                        this.filter.log(Level.WARNING, "Host ID license request failed, " + entitlementStatus.getMessage());
                        return 500;
                     } else {
                        entitlementStatus.getLicenses().write(content, 2);
                        entitlementStatus.getCertificates().write(content, 2);
                        this.filter.info("license request successful");
                        return 200;
                     }
                  } catch (Exception e) {
                     StringWriter output = new StringWriter();
                     output.write("Subscription license update failed");
                     if (ex.getMessage() != null) {
                        output.write(": " + ex.getMessage());
                     }

                     if (this.filter.isLoggable(Level.FINE)) {
                        for (StackTraceElement ste : ex.getStackTrace()) {
                           output.write('\n' + ste.toString());
                        }
                     }

                     String outputString = output.toString();
                     MessageBundle msgxx = new MessageBundle(outputString);
                     handler.error(msgxx);
                     this.filter.log(Level.WARNING, outputString);
                     return 400;
                  }
               }
            );
         }
      } else {
         if (query.containsKey("regenerateNreId")) {
            if (SubscriptionLicenseUtil.getLicenseMode() != LicenseMode.SUBSCRIPTION) {
               MessageBundle msg = new MessageBundle("Received \"regenerateNreId\" message while not in subscription mode");
               handler.error(msg);
               this.filter.log(Level.SEVERE, "received \"regenerateNreId\" message while not in subscription mode");
               return 400;
            }

            try {
               SubscriptionLicenseUtil.regenerateNreId();
            } catch (IOException e) {
               MessageBundle msg = new MessageBundle("Exception occurred while regenerating Nre ID (" + e + ')');
               handler.error(msg);
               this.filter.log(Level.SEVERE, "exception occurred while regenerating Nre ID (" + e + ')');
               if (this.filter.isLoggable(Level.FINE)) {
                  this.filter.log(Level.SEVERE, "Stack trace: ", e);
               }

               return 500;
            }
         }

         if (query.containsKey("startAccessTokenPoll")) {
            if (SubscriptionLicenseUtil.getLicenseMode() != LicenseMode.SUBSCRIPTION) {
               MessageBundle msg = new MessageBundle("Received \"startAccessTokenPoll\" message while not in subscription mode");
               handler.error(msg);
               this.filter.log(Level.SEVERE, "received \"startAccessTokenPoll\" message while not in subscription mode");
               return 400;
            }

            synchronized (this.statusLock) {
               this.accessTokenStatus = null;
            }

            DeviceCodeApi api = this.getDeviceCodeApi();
            EntitlementStatus deviceCodeStatus = api.getDeviceCode();
            if (!deviceCodeStatus.isSuccess()) {
               MessageBundle msg = new MessageBundle("Device Code fetch failed. " + deviceCodeStatus.getMessage());
               handler.error(msg);
               this.filter.log(Level.WARNING, "Host ID registration failed, " + deviceCodeStatus.getMessage());
               return 500;
            } else {
               this.response.setHeader("Content-Type", "application/json");
               JSONObject deviceCodeResponseJson = new JSONObject();
               deviceCodeResponseJson.put("device_code", deviceCodeStatus.getDeviceCode());
               deviceCodeResponseJson.put("user_code", deviceCodeStatus.getUserCode());
               deviceCodeResponseJson.put("verification_uri", deviceCodeStatus.getVerificationUri());
               deviceCodeResponseJson.put("interval", deviceCodeStatus.getInterval());
               deviceCodeResponseJson.write(content, 2, 2);
               this.filter.log(Level.FINE, "device code fetched successfully");
               this.startAccessTokenPoll(deviceCodeStatus.getDeviceCode(), deviceCodeStatus.getInterval());
               this.filter.log(Level.INFO, "access token poll started, poll interval " + deviceCodeStatus.getInterval() + " seconds");
               return 200;
            }
         } else if (query.containsKey("getAccessTokenPollStatus")) {
            if (SubscriptionLicenseUtil.getLicenseMode() != LicenseMode.SUBSCRIPTION) {
               MessageBundle msg = new MessageBundle("Received \"getAccessTokenPollStatus\" message while not in subscription mode");
               handler.error(msg);
               this.filter.log(Level.SEVERE, "received \"getAccessTokenPollStatus\" message while not in subscription mode");
               return 400;
            }

            JSONObject accessTokenResponseJson = new JSONObject();
            if (AccessTokenApi.isAccessTokenPollComplete()) {
               accessTokenResponseJson.put("isComplete", "true");
               synchronized (this.statusLock) {
                  this.accessTokenStatus = AccessTokenApi.getAccessTokenPollStatus();
                  accessTokenResponseJson.put("isFailure", String.valueOf(this.accessTokenStatus.isFailure()));
                  accessTokenResponseJson.put("failureMessage", this.accessTokenStatus.getMessage());
               }
            } else {
               accessTokenResponseJson.put("isComplete", "false");
               accessTokenResponseJson.put("isFailure", "false");
               accessTokenResponseJson.put("failureMessage", "");
            }

            this.response.setHeader("Content-Type", "application/json");
            accessTokenResponseJson.write(content, 2, 2);
            this.filter.log(Level.FINE, "sending access token poll status");
            return 200;
         } else if (query.containsKey("stopAccessTokenPoll")) {
            if (SubscriptionLicenseUtil.getLicenseMode() != LicenseMode.SUBSCRIPTION) {
               MessageBundle msg = new MessageBundle("Received \"stopAccessTokenPoll\" message while not in subscription mode");
               handler.error(msg);
               this.filter.log(Level.SEVERE, "received \"stopAccessTokenPoll\" message while not in subscription mode");
               return 400;
            }

            this.stopAccessTokenPoll();
            synchronized (this.statusLock) {
               this.accessTokenStatus = null;
               return 200;
            }
         } else if (query.containsKey("registerDevice")) {
            if (SubscriptionLicenseUtil.getLicenseMode() != LicenseMode.SUBSCRIPTION) {
               MessageBundle msg = new MessageBundle("Received \"registerDevice\" message while not in subscription mode");
               handler.error(msg);
               this.filter.log(Level.SEVERE, "received \"registerDevice\" message while not in subscription mode");
               return 400;
            }

            String licenseKey = query.get("licenseKey", "XXXX-XXXX-XXXX-XXXX");
            if ("XXXX-XXXX-XXXX-XXXX".equals(licenseKey)) {
               MessageBundle msg = new MessageBundle("Registration request failed [Message = 'Unable to parse license key']");
               handler.error(msg);
               this.filter.log(Level.WARNING, "Host ID registration failed, unable to parse license key");
               return 400;
            }

            EntitlementStatus registrationStatus;
            synchronized (this.statusLock) {
               if (this.accessTokenStatus == null) {
                  MessageBundle msg = new MessageBundle("Registration request failed [Message = 'Access token not found']");
                  handler.error(msg);
                  this.filter.log(Level.WARNING, "Host ID registration failed, access token not found");
                  return 400;
               }

               if (this.accessTokenStatus.isFailure()) {
                  MessageBundle msg = new MessageBundle(
                     "Registration request failed because access token poll had failed. Access token poll error [Code = '"
                        + this.accessTokenStatus.getCode()
                        + "' Message = '"
                        + this.accessTokenStatus.getMessage()
                        + "']"
                  );
                  handler.error(msg);
                  this.filter.log(Level.WARNING, "Host ID registration failed, " + this.accessTokenStatus.getMessage());
                  return 400;
               }

               registrationStatus = this.getRegistrationApi().register(this.accessTokenStatus, licenseKey);
            }

            if (!registrationStatus.isSuccess()) {
               MessageBundle msg = new MessageBundle(
                  "Registration request failed [Code = '" + registrationStatus.getCode() + "' Message = '" + registrationStatus.getMessage() + "']"
               );
               handler.error(msg);
               this.filter.log(Level.WARNING, "Host ID registration failed, " + registrationStatus.getMessage());
               return 500;
            }

            if (this.filter.isLoggable(Level.FINE)) {
               this.filter.log(Level.FINE, "Host ID registered successfully with license key: " + licenseKey);
            }

            return 200;
         } else {
            if (query.containsKey("resetRestoreParameters")) {
               if (SubscriptionLicenseUtil.getLicenseMode() != LicenseMode.SUBSCRIPTION) {
                  MessageBundle msg = new MessageBundle("Received \"resetRestoreParameters\" message while not in subscription mode");
                  handler.error(msg);
                  this.filter.log(Level.SEVERE, "received \"resetRestoreParameters\" message while not in subscription mode");
                  return 400;
               }

               try {
                  SubscriptionLicenseUtil.removeRestoreParameters();
                  SubscriptionMetadataUtil.addRegistrationMetadata("reregistrationCause", "backup-restoration");
                  SubscriptionMetadataUtil.addRegistrationMetadata("previousNreId", this.platformProvider.getHostId());
               } catch (IOException e) {
                  MessageBundle msg = new MessageBundle("Exception occurred while deleting restore parameters (" + e + ")");
                  handler.error(msg);
                  this.filter.log(Level.SEVERE, "exception occurred while deleting restore parameters (" + e + ")");
                  if (this.filter.isLoggable(Level.FINE)) {
                     this.filter.log(Level.SEVERE, "Stack trace: ", e);
                  }

                  return 400;
               }
            }

            if (query.containsKey("rebuildSignatureRegistry")) {
               this.filter.fine("requesting signature registry rebuild");
               JarSignatureRegistry.buildSignatureRegistry();
            }

            if (query.containsKey("dumpThreads")) {
               this.filter.fine("requesting thread dump");
               this.platformProvider.dumpThreads();
            }

            if (query.containsKey("reloadTz")) {
               if (this.filter.isLoggable(Level.FINE)) {
                  this.filter
                     .fine(
                        "refreshing daemon time zone [defaultTimeZone='"
                           + TimeZone.getDefault().getID()
                           + "',user.timezone='"
                           + AccessController.doPrivileged(() -> System.getProperty("user.timezone"))
                           + "']"
                     );
               }

               System.clearProperty("user.timezone");
               TimeZone.setDefault(null);
               NiagaraDaemon.getTimeZoneId(true);
               if (this.filter.isLoggable(Level.FINE)) {
                  this.filter
                     .fine(
                        "daemon time zone refreshed [defaultTimeZone='"
                           + TimeZone.getDefault().getID()
                           + "',user.timezone='"
                           + AccessController.doPrivileged(() -> System.getProperty("user.timezone"))
                           + "']"
                     );
               }
            }

            if (query.containsKey("garbageCollection")) {
               this.filter.fine("requesting garbage collection");
               System.gc();
            }

            if (query.containsKey("restartWeb")) {
               this.filter.fine("requesting webserver restart");
               restartWeb = true;
            }

            if (query.containsKey("reloadProperties")) {
               this.filter.fine("requesting properties reload");
               NiagaraDaemon.reloadProperties(NiagaraDaemon.NIAGARA_DAEMON_PROPERTIES_PATH);
               restartWeb = true;
            }

            if (query.containsKey("refreshSoftware")) {
               if (this.platformProvider.allowPlatformDaemonRestart()) {
                  try {
                     if (!NiagaraDaemon.getInstance().lockClient()) {
                        MessageBundle msg = new MessageBundle("platform", "NiagaraDaemon.multipleClient", "multiple client access conflict (refreshSoftware)");
                        this.filter.severe("multiple client access conflict (refreshSoftware)");
                        handler.error(msg);
                        return 409;
                     }

                     if (NiagaraDaemon.getInstance().queueRefreshSoftware() == 0) {
                        NiagaraDaemon.getInstance().lockClientPermanent();
                     }
                  } finally {
                     NiagaraDaemon.getInstance().unlockClient();
                  }
               }
            } else if (restartWeb) {
               NiagaraDaemon.getInstance().queueRestartWeb();
            }

            return 200;
         }
      }
   }

   protected SubscriptionLicenseUtil getSubscriptionLicenseUtil() {
      return SubscriptionLicenseUtil.getInstance();
   }

   protected DeviceCodeApi getDeviceCodeApi() {
      return new DeviceCodeApi();
   }

   protected RegistrationApi getRegistrationApi() {
      return new RegistrationApi();
   }

   protected void startAccessTokenPoll(String deviceCode, int interval) {
      Poll.start(deviceCode, interval);
   }

   protected void stopAccessTokenPoll() {
      if (Poll.isRunning()) {
         Poll.stop();
         this.filter.log(Level.INFO, "access token poll stopped");
      } else {
         this.filter.log(Level.FINE, "no active polls to stop");
      }
   }
}
