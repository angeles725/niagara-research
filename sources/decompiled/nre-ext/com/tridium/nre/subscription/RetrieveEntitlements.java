package com.tridium.nre.subscription;

import com.tridium.json.JSONObject;
import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessController;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAmount;
import java.util.Objects;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import javax.baja.nre.util.SystemFiles;
import javax.baja.xml.XElem;
import javax.baja.xml.XParser;

public class RetrieveEntitlements extends EntitlementApi {
   private static EntitlementApi.EntitlementStatus lastEntitlementStatus;
   private final LicenseRefreshToken lrt;
   private ScheduledFuture<?> entitlementCheck;
   private TemporalAmount entitlementCheckFrequency;
   private Instant entitlementStart;
   private boolean licenseUpdated = false;
   private static final Duration RETRIEVE_ENTITLEMENTS_RETRY_TIMEOUT = Duration.ofMinutes(5L);
   public static final int RETRY_EXPIRED = 10;
   public static final int RETRY_INTERRUPTED = 20;
   static final int UNRECOGNIZED_FAILURE = 30;
   private static final String SUBSCRIPTION_LICENSE_MANAGER_CLASS_NAME = "com.tridium.sys.license.subscription.SubscriptionLicenseManager";
   private static final String SUBSCRIPTION_LICENSE_MANAGER_IS_LICENSE_SIGNATURE_VALUE_METHOD_NAME = "isLicenseSignatureValid";

   public RetrieveEntitlements(LicenseRefreshToken licenseRefreshToken) {
      this.lrt = licenseRefreshToken;
   }

   public RetrieveEntitlements(LicenseRefreshToken licenseRefreshToken, ScheduledFuture<?> check, TemporalAmount frequency) {
      this.lrt = licenseRefreshToken;
      this.entitlementCheck = check;
      this.entitlementCheckFrequency = frequency;
   }

   @Override
   protected String getApiName() {
      return "entitlements";
   }

   @Override
   protected String getApiPath() {
      return "/ncents/entitlements";
   }

   public EntitlementApi.EntitlementStatus retrieveEntitlements(long initialDelay) {
      long retryDelay = initialDelay;
      long nextEntitlementCheck = Long.MAX_VALUE;
      if (this.entitlementStart == null) {
         this.entitlementStart = Instant.now();
      }

      while (nextEntitlementCheck > retryDelay && !this.entitlementCheckExpired()) {
         if (this.lrt == null) {
            return new EntitlementApi.EntitlementStatus(
               EntitlementApi.EntitlementState.FAILURE, 0, "Subscription licensing check request failed because a LicenseRefreshToken was not found."
            );
         }

         this.lrt.updateRefreshIncrement();
         EntitlementApi.EntitlementStatus entitlementStatus = this.entitlementsApi();
         if (entitlementStatus.isSuccess()) {
            return entitlementStatus;
         }

         if (!entitlementStatus.isFailure() || !isRetryableError(entitlementStatus.getCode())) {
            if (entitlementStatus.isRestore()) {
               EntitlementUtil.LOG.info(entitlementStatus.getMessage());
               return entitlementStatus;
            }

            if (!entitlementStatus.isFailure()
               && !entitlementStatus.isInvalidRefreshToken()
               && !entitlementStatus.isLicenseExpired()
               && !entitlementStatus.isLicenseRevoked()) {
               EntitlementUtil.LOG.warning(entitlementStatus.getMessage());
               return new EntitlementApi.EntitlementStatus(
                  EntitlementApi.EntitlementState.FAILURE, 30, "Unexpected response was received from the subscription licensing server."
               );
            }

            EntitlementUtil.LOG.warning(entitlementStatus.getMessage());
            return entitlementStatus;
         }

         retryDelay *= 2L;
         String message = "Subscription licensing check failure ("
            + entitlementStatus.getCode()
            + ") \""
            + entitlementStatus.getMessage()
            + "\", retrying in "
            + retryDelay
            + "ms";
         EntitlementUtil.LOG.log(Level.WARNING, message);
         if (this.entitlementCheck != null) {
            nextEntitlementCheck = this.entitlementCheck.getDelay(TimeUnit.MILLISECONDS) + this.entitlementCheckFrequency.get(ChronoUnit.SECONDS) * 1000L;
         }

         if (nextEntitlementCheck > retryDelay) {
            try {
               Thread.sleep(retryDelay);
            } catch (InterruptedException ignore) {
               EntitlementApi.EntitlementStatus status = new EntitlementApi.EntitlementStatus(
                  EntitlementApi.EntitlementState.FAILURE, 20, "Subscription licensing check retry was interrupted."
               );
               setLastEntitlementStatus(status);
               return status;
            }
         }
      }

      EntitlementApi.EntitlementStatus status = new EntitlementApi.EntitlementStatus(
         EntitlementApi.EntitlementState.FAILURE,
         10,
         "Subscription licensing check retry expired after " + RETRIEVE_ENTITLEMENTS_RETRY_TIMEOUT.toMinutes() + " minutes."
      );
      setLastEntitlementStatus(status);
      return status;
   }

   public static boolean isRetryableError(int statusCode) {
      return statusCode != 200 && statusCode != 409;
   }

   protected boolean entitlementCheckExpired() {
      return Duration.between(this.entitlementStart, Instant.now()).compareTo(RETRIEVE_ENTITLEMENTS_RETRY_TIMEOUT) > 0;
   }

   public EntitlementApi.EntitlementStatus entitlementsApi() {
      JSONObject requestBody = new JSONObject()
         .put("nreId", this.lrt.getNreId())
         .put("productId", this.lrt.getProductId())
         .put("refreshIncrement", this.lrt.getRefreshIncrement())
         .put("restoreId", this.lrt.getRestoreId())
         .put("nonce", this.lrt.getNonce());
      return this.entitlementsApi(requestBody, true);
   }

   public EntitlementApi.EntitlementStatus entitlementsApi(JSONObject requestBody, boolean writeFile) {
      this.licenseUpdated = false;
      JSONObject response = this.sendRequest(requestBody, this.makeJwtAuthHeader());
      EntitlementApi.EntitlementStatus errorStatus = this.checkErrorResponse(response);
      if (errorStatus != null) {
         setLastEntitlementStatus(errorStatus);
         return errorStatus;
      }

      if (!response.has("entitlements")) {
         EntitlementApi.EntitlementStatus status = new EntitlementApi.EntitlementStatus(
            EntitlementApi.EntitlementState.FAILURE, 200, "Subscription licensing request failed to return the set of licenses."
         );
         setLastEntitlementStatus(status);
         return status;
      }

      JSONObject entitlements = response.optJSONObject("entitlements");
      if (entitlements != null && !entitlements.isEmpty()) {
         EntitlementApi.EntitlementStatus entitlementLicenses = new EntitlementApi.EntitlementStatus(
            EntitlementApi.EntitlementState.SUCCESS, 200, "Subscription licensing request is complete."
         );

         for (String licenseName : Objects.requireNonNull(JSONObject.getNames(entitlements))) {
            String license = entitlements.optString(licenseName, "");

            try {
               XElem licenseXml = XParser.make(license).parse(true);
               if (writeFile) {
                  if (!this.isLicenseValid(licenseXml)) {
                     EntitlementApi.EntitlementStatus status = new EntitlementApi.EntitlementStatus(
                        EntitlementApi.EntitlementState.FAILURE,
                        200,
                        "Subscription licensing request failed to validate returned license, so no license file was generated."
                     );
                     setLastEntitlementStatus(status);
                     return status;
                  }

                  SubscriptionLicenseUtil slu = SubscriptionLicenseUtil.getInstance();
                  this.licenseUpdated = slu.writeLicense(licenseXml);
               }

               entitlementLicenses.addEntitlement(licenseXml);
            } catch (Exception e) {
               EntitlementApi.EntitlementStatus status = new EntitlementApi.EntitlementStatus(
                  EntitlementApi.EntitlementState.FAILURE, 200, "Subscription licensing request failed to generate license files: " + e.getLocalizedMessage()
               );
               setLastEntitlementStatus(status);
               return status;
            }
         }

         setLastEntitlementStatus(entitlementLicenses);
         return entitlementLicenses;
      } else {
         EntitlementApi.EntitlementStatus status = new EntitlementApi.EntitlementStatus(
            EntitlementApi.EntitlementState.FAILURE, 200, "Subscription licensing request failed to return any licenses."
         );
         setLastEntitlementStatus(status);
         return status;
      }
   }

   public boolean isLicenseValid(XElem licenseElem) {
      XElem root = licenseElem.copy();
      String hostId = root.get("hostId");
      if (!hostId.equals(SubscriptionLicenseUtil.getNreId())) {
         EntitlementUtil.LOG.finest("license validation: hostId mismatch");
         return false;
      }

      XElem sigElem = root.elem("signature");
      if (sigElem != null && sigElem.string() != null) {
         String vendor = root.get("vendor");
         File certificateFile = new File(SubscriptionLicenseUtil.getSubscriptionCertificateDirectory(), vendor + ".certificate");
         if (!certificateFile.exists()) {
            RequestCertificates rc = new RequestCertificates();
            boolean certFound = this.updateCertificates(rc, vendor);
            if (!certFound || !rc.isCertificateUpdated()) {
               if (EntitlementUtil.LOG.isLoggable(Level.FINEST)) {
                  EntitlementUtil.LOG.finest("license validation: certificate for " + vendor + " not found");
               }

               return false;
            }
         }

         if (!certificateFile.exists()) {
            if (EntitlementUtil.LOG.isLoggable(Level.FINEST)) {
               EntitlementUtil.LOG.finest("license validation: no certificate file for: " + vendor);
            }

            return false;
         } else {
            boolean isValid = true;

            try {
               boolean isNiagaraDaemon = AccessController.doPrivileged(() -> Boolean.getBoolean("NiagaraDaemon"));
               if (!isNiagaraDaemon) {
                  isValid = this.isLicenseSignatureValid(root, certificateFile);
               } else if (EntitlementUtil.LOG.isLoggable(Level.FINE)) {
                  EntitlementUtil.LOG.log(Level.FINE, "running as niagara daemon, skipping license signature validation");
               }
            } catch (Exception e) {
               EntitlementUtil.LOG.log(Level.FINE, "failed to validate license signature", e);
               isValid = false;
            }

            return isValid;
         }
      } else {
         EntitlementUtil.LOG.finest("license validation: missing signature");
         return false;
      }
   }

   private boolean isLicenseSignatureValid(XElem root, File certificateFile) throws Exception {
      File bajaModuleFile = new File(SystemFiles.getModulesDirectory(), "baja.jar");
      URL bajaUrl = bajaModuleFile.toURI().toURL();
      return AccessController.doPrivileged(() -> {
         try (URLClassLoader bajaClassLoader = new URLClassLoader(new URL[]{bajaUrl}, this.getClass().getClassLoader())) {
            Class<?> subscriptionLicenseManagerClass = bajaClassLoader.loadClass("com.tridium.sys.license.subscription.SubscriptionLicenseManager");
            Method isLicenseSignatureValid = subscriptionLicenseManagerClass.getDeclaredMethod("isLicenseSignatureValid", XElem.class, File.class);
            return (Boolean)isLicenseSignatureValid.invoke(null, root, certificateFile);
         }
      });
   }

   public boolean updateCertificates(RequestCertificates rc, String vendor) {
      String[] certificateVendors = new String[]{vendor};
      String certificateVersion = rc.getCertificateVersion();
      EntitlementApi.EntitlementStatus status = rc.getCertificates(this.lrt.getNreId(), certificateVendors, certificateVersion);
      return status.isSuccess();
   }

   @Override
   protected EntitlementApi.EntitlementStatus doCheckEndpointErrorResponse(String responseType, String responseMessage) {
      if (EntitlementApi.EntitlementState.INVALID_REFRESH_TOKEN.toString().equals(responseType)) {
         return new EntitlementApi.EntitlementStatus(
            EntitlementApi.EntitlementState.INVALID_REFRESH_TOKEN, 409, "The refresh token is invalid: " + responseMessage
         );
      } else if (EntitlementApi.EntitlementState.LICENSE_EXPIRED.toString().equals(responseType)) {
         return new EntitlementApi.EntitlementStatus(EntitlementApi.EntitlementState.LICENSE_EXPIRED, 409, "The license is expired: " + responseMessage);
      } else if (EntitlementApi.EntitlementState.LICENSE_REVOKED.toString().equals(responseType)) {
         return new EntitlementApi.EntitlementStatus(EntitlementApi.EntitlementState.LICENSE_REVOKED, 409, "The license is revoked: " + responseMessage);
      } else {
         return EntitlementApi.EntitlementState.RESTORE.toString().equals(responseType)
            ? new EntitlementApi.EntitlementStatus(
               EntitlementApi.EntitlementState.RESTORE, 409, "The restore workflow was initiated by the subscription licensing system: " + responseMessage
            )
            : null;
      }
   }

   public boolean isLicenseUpdated() {
      return this.licenseUpdated;
   }

   public static EntitlementApi.EntitlementStatus getLastEntitlementStatus() {
      return lastEntitlementStatus;
   }

   private static void setLastEntitlementStatus(EntitlementApi.EntitlementStatus status) {
      lastEntitlementStatus = status;
   }
}
