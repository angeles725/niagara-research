package com.tridium.nre.subscription;

import com.tridium.json.JSONObject;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.security.PublicKey;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class RegistrationApi extends EntitlementApi {
   private static final int REGISTRATION_RETRY_COUNT = 3;
   private static final String REREGISTRATION_CAUSE = "backup-restoration";
   static final File REGISTRATION_FILE = new File(SubscriptionLicenseUtil.getSubscriptionDirectory(), ".registered");

   @Override
   protected String getApiName() {
      return "register";
   }

   @Override
   protected String getApiPath() {
      return "/ncents/register";
   }

   @Override
   protected EntitlementApi.EntitlementStatus doCheckEndpointErrorResponse(String type, String message) {
      return null;
   }

   @Override
   protected String getApiVersion() {
      return "";
   }

   public EntitlementApi.EntitlementStatus register(
      EntitlementApi.EntitlementStatus accessTokenStatus,
      String licenseKey,
      String nreId,
      String publicKey,
      String restoreId,
      String platform,
      String type,
      Boolean isRemoteDevice
   ) {
      if (!accessTokenStatus.isSuccess()) {
         EntitlementUtil.LOG.log(Level.WARNING, "Access token claim failed. Cannot proceed with registration: " + accessTokenStatus.getMessage());
         return accessTokenStatus;
      }

      for (int count = 1; count <= 3; count++) {
         try {
            EntitlementApi.EntitlementStatus registrationStatus = this.registerApi(
               accessTokenStatus, licenseKey, nreId, publicKey, restoreId, platform, type, isRemoteDevice
            );
            if (!registrationStatus.isSuccess()) {
               EntitlementUtil.LOG.warning(registrationStatus.getMessage());
            }

            return registrationStatus;
         } catch (EntitlementException e) {
            if (EntitlementUtil.LOG.isLoggable(Level.FINE)) {
               EntitlementUtil.LOG.log(Level.WARNING, "Failed to register.", e);
            } else {
               EntitlementUtil.LOG.log(Level.WARNING, "Failed to register: " + e);
            }

            EntitlementUtil.LOG.warning(String.format("Failure %d of %d attempts...", count, 3));
         }
      }

      return new EntitlementApi.EntitlementStatus(
         EntitlementApi.EntitlementState.FAILURE, 0, "Failed to register this device with the subscription licensing server."
      );
   }

   public EntitlementApi.EntitlementStatus registerRemoteDevice(
      EntitlementApi.EntitlementStatus accessTokenStatus, String licenseKey, String nreId, String platform, String type
   ) {
      PublicKey jwtPublicKey = JwtSignatureKeys.getInstance(SubscriptionLicenseUtil.getSubscriptionDbDirectory(licenseKey)).getPublicKey();
      String publicKey = Base64.getEncoder().encodeToString(jwtPublicKey.getEncoded());
      String restoreId = RestoreId.getInstance(SubscriptionLicenseUtil.getSubscriptionDbDirectory(licenseKey)).get();
      return this.register(accessTokenStatus, licenseKey, nreId, publicKey, restoreId, platform, type, true);
   }

   public EntitlementApi.EntitlementStatus register(EntitlementApi.EntitlementStatus accessTokenStatus, String licenseKey) {
      String nreId = SubscriptionLicenseUtil.getNreId();
      PublicKey jwtPublicKey = JwtSignatureKeys.getInstance().getPublicKey();
      String publicKey = Base64.getEncoder().encodeToString(jwtPublicKey.getEncoded());
      String platform = SubscriptionLicenseUtil.LocalHostMetaDataHolder.HOST_MODEL;
      String restoreId = RestoreId.getInstance().get();
      String type = this.getNreInstanceType();
      return this.register(accessTokenStatus, licenseKey, nreId, publicKey, restoreId, platform, type, false);
   }

   public EntitlementApi.EntitlementStatus registerApi(
      EntitlementApi.EntitlementStatus accessTokenStatus,
      String licenseKey,
      String nreId,
      String publicKey,
      String restoreId,
      String platform,
      String type,
      boolean isRemoteDevice
   ) {
      JSONObject requestBody = new JSONObject()
         .put("id", accessTokenStatus.getId())
         .put("issued_at", accessTokenStatus.getIssuedAt())
         .put("signature", accessTokenStatus.getSignature());
      Map<String, String> metadataMap = new HashMap<>();
      metadataMap.put("platform", platform);
      metadataMap.put("type", type);

      try {
         requestBody.put("metadata", SubscriptionMetadataUtil.getRegistrationMetadataJson(metadataMap));
         if ("backup-restoration".equals(SubscriptionMetadataUtil.getRegistrationMetadata("reregistrationCause")) && !isRemoteDevice) {
            requestBody.put("reregistrationCause", "backup-restoration");
            requestBody.put("refreshIncrement", RefreshIncrement.getInstance().getAndIncrement());
         }
      } catch (Exception e) {
         throw new EntitlementException("Unable to retrieve metadata from license.properties file", e);
      }

      requestBody.put("nreId", nreId).put("licenseKey", licenseKey).put("publicKey", publicKey).put("restoreId", restoreId);
      JSONObject response = this.sendRequest(
         requestBody, EntitlementUtil.makeRegistrationHeader("application/json", "application/json", accessTokenStatus.getAccessToken())
      );
      EntitlementApi.EntitlementStatus errorStatus = this.checkErrorResponse(response);
      if (errorStatus != null) {
         return errorStatus;
      }

      if ("registration".equals(response.getString("type")) && "success".equals(response.getString("message"))) {
         File registrationFile = REGISTRATION_FILE;
         if (isRemoteDevice) {
            registrationFile = new File(SubscriptionLicenseUtil.getSubscriptionDbDirectory(licenseKey), ".registered");
         }

         try {
            if (!registrationFile.createNewFile()) {
               EntitlementUtil.LOG.warning("Unable to create the registration file.");
               return new EntitlementApi.EntitlementStatus(EntitlementApi.EntitlementState.FAILURE, 0, "Unable to create the registration file.");
            }

            if (SubscriptionMetadataUtil.containsRegistrationMetadata("reregistrationCause")) {
               SubscriptionMetadataUtil.removeRegistrationMetadata("reregistrationCause");
            }
         } catch (Exception e) {
            if (EntitlementUtil.LOG.isLoggable(Level.FINE)) {
               EntitlementUtil.LOG.log(Level.WARNING, "Unable to generate the registration file.", e);
            } else {
               EntitlementUtil.LOG.log(Level.WARNING, "Unable to generate the registration file. " + e.getLocalizedMessage());
            }

            return new EntitlementApi.EntitlementStatus(
               EntitlementApi.EntitlementState.FAILURE, 0, "Unable to generate the registration file: " + e.getLocalizedMessage()
            );
         }

         try (DataOutputStream out = new DataOutputStream(new FileOutputStream(registrationFile))) {
            out.writeUTF(Instant.now().toString());
         } catch (Exception e) {
            if (EntitlementUtil.LOG.isLoggable(Level.FINE)) {
               EntitlementUtil.LOG.log(Level.WARNING, "Unable to write to registration file.", e);
            } else {
               EntitlementUtil.LOG.log(Level.WARNING, "Unable to write to registration file. " + e.getLocalizedMessage());
            }

            return new EntitlementApi.EntitlementStatus(
               EntitlementApi.EntitlementState.FAILURE, 0, "Unable to write the registration file: " + e.getLocalizedMessage()
            );
         }

         EntitlementUtil.LOG.log(Level.INFO, "This device is registered with the subscription licensing system.");
         return new EntitlementApi.EntitlementStatus(EntitlementApi.EntitlementState.SUCCESS, 200, "Registration successful.");
      } else {
         return new EntitlementApi.EntitlementStatus(EntitlementApi.EntitlementState.FAILURE, 0, "Server response contained unexpected parameters.");
      }
   }

   protected String getNreInstanceType() {
      return "station";
   }
}
