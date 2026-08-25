package com.tridium.nre.subscription;

import com.tridium.json.JSONObject;
import java.util.logging.Level;

public class DeviceCodeApi extends EntitlementApi {
   private static final int DEVICE_CODE_RETRY_COUNT = 3;

   @Override
   protected String getApiName() {
      return "deviceCode";
   }

   @Override
   protected String getApiPath() {
      return "/services/oauth2/token";
   }

   @Override
   protected String getApiVersion() {
      return "";
   }

   @Override
   protected String getConnectionUrl() {
      return SubscriptionLicenseUtil.getLicenseProperties().getProperty("license.registrationUrl", "https://www.niagara-community.com");
   }

   @Override
   protected String getConnectionPort() {
      return SubscriptionLicenseUtil.getLicenseProperties().getProperty("license.registrationPort", "443");
   }

   public EntitlementApi.EntitlementStatus getDeviceCode() {
      for (int count = 1; count <= 3; count++) {
         try {
            EntitlementApi.EntitlementStatus deviceCodeStatus = this.deviceCodeApi();
            if (!deviceCodeStatus.isSuccess()) {
               EntitlementUtil.LOG.warning(deviceCodeStatus.getMessage());
            }

            return deviceCodeStatus;
         } catch (EntitlementException e) {
            if (EntitlementUtil.LOG.isLoggable(Level.FINE)) {
               EntitlementUtil.LOG.log(Level.WARNING, "Failed to get device code.", e);
            } else {
               EntitlementUtil.LOG.log(Level.WARNING, "Failed to get device code: " + e);
            }

            EntitlementUtil.LOG.warning(String.format("Failure %d of %d attempts...", count, 3));
         }
      }

      return new EntitlementApi.EntitlementStatus(
         EntitlementApi.EntitlementState.FAILURE, 0, "Failed to register this device with the subscription licensing server."
      );
   }

   public EntitlementApi.EntitlementStatus deviceCodeApi() {
      StringBuilder requestMessage = new StringBuilder();
      requestMessage.append("client_id").append('=').append(EntitlementUtil.getDeviceRegistrationClientId());
      requestMessage.append('&');
      requestMessage.append("response_type").append('=').append("device_code");
      JSONObject response = this.sendRequest(
         requestMessage.toString(),
         EntitlementUtil.makeDeviceCodeHeader(EntitlementUtil.getDeviceRegistrationHost(), "application/x-www-form-urlencoded"),
         "application/x-www-form-urlencoded"
      );
      EntitlementApi.EntitlementStatus errorStatus = this.checkErrorResponse(response);
      if (errorStatus != null) {
         return errorStatus;
      }

      EntitlementApi.EntitlementStatus deviceCodeSuccess = new EntitlementApi.EntitlementStatus(
         EntitlementApi.EntitlementState.SUCCESS, 200, "Device code fetch successful."
      );

      try {
         deviceCodeSuccess.setDeviceCode(response.getString("device_code"));
         deviceCodeSuccess.setUserCode(response.getString("user_code"));
         deviceCodeSuccess.setVerificationUri(response.getString("verification_uri"));
         deviceCodeSuccess.setInterval(response.getInt("interval"));
         return deviceCodeSuccess;
      } catch (Exception e) {
         if (EntitlementUtil.LOG.isLoggable(Level.FINE)) {
            EntitlementUtil.LOG.log(Level.WARNING, "Failed to parse device code response.", e);
         } else {
            EntitlementUtil.LOG.log(Level.WARNING, "Failed to parse device code response: " + e);
         }

         return new EntitlementApi.EntitlementStatus(EntitlementApi.EntitlementState.FAILURE, 0, "Failed to parse device code response.");
      }
   }

   @Override
   protected EntitlementApi.EntitlementStatus doCheckEndpointErrorResponse(String type, String message) {
      return null;
   }
}
