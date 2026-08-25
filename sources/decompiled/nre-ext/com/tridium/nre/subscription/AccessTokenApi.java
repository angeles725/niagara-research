package com.tridium.nre.subscription;

import com.tridium.json.JSONObject;
import java.time.Instant;
import java.util.logging.Level;

public class AccessTokenApi extends EntitlementApi {
   private static EntitlementApi.EntitlementStatus pollStatus;
   private static boolean isAccessTokenPollComplete;
   private static final int ACCESS_TOKEN_POLLING_MINUTES = 10;

   public EntitlementApi.EntitlementStatus accessTokenApi(String deviceCode) {
      StringBuilder requestBody = new StringBuilder();
      requestBody.append("client_id").append('=').append(EntitlementUtil.getDeviceRegistrationClientId());
      requestBody.append('&');
      requestBody.append("grant_type").append('=').append("device");
      requestBody.append('&');
      requestBody.append("code").append('=').append(deviceCode);
      JSONObject response = this.sendRequest(
         requestBody.toString(),
         EntitlementUtil.makeDeviceCodeHeader(EntitlementUtil.getDeviceRegistrationHost(), "application/x-www-form-urlencoded"),
         "application/x-www-form-urlencoded"
      );
      EntitlementApi.EntitlementStatus errorStatus = this.checkErrorResponse(response);
      if (errorStatus != null) {
         return errorStatus;
      }

      EntitlementApi.EntitlementStatus status = new EntitlementApi.EntitlementStatus(
         EntitlementApi.EntitlementState.SUCCESS, 200, "Access token retrieval successful."
      );
      status.setAccessToken(response.optString("access_token"));
      status.setSignature(response.optString("signature"));
      status.setScope(response.optString("scope"));
      status.setInstanceUrl(response.optString("instance_url"));
      status.setId(response.optString("id"));
      status.setTokenType(response.optString("token_type"));
      status.setIssuedAt(response.optLong("issued_at"));
      status.setAccessTokenErrorType("");
      return status;
   }

   public static EntitlementApi.EntitlementStatus getAccessTokenPollStatus() {
      return pollStatus;
   }

   public static boolean isAccessTokenPollComplete() {
      return isAccessTokenPollComplete;
   }

   @Override
   protected JSONObject handleResponseError(int statusCode, String statusType, String responseMessage) {
      if (400 == statusCode && ("authorization_pending".equals(statusType) || "too_fast".equals(statusType))) {
         if (EntitlementUtil.LOG.isLoggable(Level.FINE)) {
            EntitlementUtil.LOG.fine("Status code: 400, Response message body: " + responseMessage);
         }
      } else {
         super.handleResponseError(statusCode, statusType, responseMessage);
      }

      return new JSONObject().put("code", statusCode).put("type", statusType).put("message", responseMessage);
   }

   @Override
   protected String getApiName() {
      return "accessToken";
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

   @Override
   protected EntitlementApi.EntitlementStatus doCheckEndpointErrorResponse(String type, String message) {
      return null;
   }

   public static class Poll implements Runnable {
      String currentDeviceCode;
      AccessTokenApi currentTokenApi;
      int currentPollInterval;
      static boolean exit;
      static Thread pollThread;

      public Poll(String deviceCode, int pollInterval, AccessTokenApi api) {
         this.currentDeviceCode = deviceCode;
         this.currentPollInterval = pollInterval;
         this.currentTokenApi = api;
      }

      public Thread getThread() {
         Thread thread = new Thread(this, "Nre:PollAccessToken");
         thread.setDaemon(true);
         return thread;
      }

      @Override
      public void run() {
         exit = false;
         AccessTokenApi.isAccessTokenPollComplete = false;
         AccessTokenApi.pollStatus = new EntitlementApi.EntitlementStatus(
            EntitlementApi.EntitlementState.FAILURE, 0, "Failed to register this device with the subscription licensing server."
         );
         Instant nextPollTime = Instant.now();
         Instant lastPollTime = nextPollTime.plusSeconds(600L);
         EntitlementUtil.LOG.fine("Last Poll Time = " + lastPollTime);

         while (nextPollTime.isBefore(lastPollTime)) {
            EntitlementUtil.LOG.fine("Polling for access token at " + nextPollTime);
            if (exit) {
               AccessTokenApi.isAccessTokenPollComplete = true;
               EntitlementUtil.LOG.fine("Access token polling was cancelled.");
               break;
            }

            try {
               AccessTokenApi.pollStatus = this.currentTokenApi.accessTokenApi(this.currentDeviceCode);
               if (AccessTokenApi.pollStatus.isSuccess()) {
                  AccessTokenApi.isAccessTokenPollComplete = true;
                  EntitlementUtil.LOG.info(AccessTokenApi.pollStatus.getMessage());
                  break;
               }

               if (!"authorization_pending".equals(AccessTokenApi.pollStatus.getAccessTokenErrorType())
                  && !"too_fast".equals(AccessTokenApi.pollStatus.getAccessTokenErrorType())) {
                  AccessTokenApi.isAccessTokenPollComplete = true;
                  EntitlementUtil.LOG.warning(AccessTokenApi.pollStatus.getMessage());
                  break;
               }

               EntitlementUtil.LOG.fine(AccessTokenApi.pollStatus.getMessage());
               if ("too_fast".equals(AccessTokenApi.pollStatus.getAccessTokenErrorType())) {
                  this.currentPollInterval *= 2;
                  EntitlementUtil.LOG.info("Increasing the poll interval to " + this.currentPollInterval + " seconds");
               }

               nextPollTime = nextPollTime.plusSeconds(this.currentPollInterval);
               if (nextPollTime.isAfter(lastPollTime)) {
                  AccessTokenApi.isAccessTokenPollComplete = true;
                  break;
               }

               Thread.sleep(this.currentPollInterval * 1000L);
            } catch (Exception e) {
               if (EntitlementUtil.LOG.isLoggable(Level.FINE)) {
                  EntitlementUtil.LOG.log(Level.WARNING, "Failed to get access token.", e);
               } else {
                  EntitlementUtil.LOG.log(Level.WARNING, "Failed to get access token: " + e);
               }

               AccessTokenApi.isAccessTokenPollComplete = true;
               break;
            }
         }

         if (!AccessTokenApi.pollStatus.isSuccess()) {
            EntitlementUtil.LOG.warning("Access Token Poll finished without a device registration approval");
            AccessTokenApi.isAccessTokenPollComplete = true;
         }
      }

      public static synchronized void start(String deviceCode, int pollInterval) {
         AccessTokenApi.Poll tokenPoll = new AccessTokenApi.Poll(deviceCode, pollInterval, new AccessTokenApi());
         if (pollThread != null && pollThread.isAlive()) {
            pollThread.interrupt();

            try {
               pollThread.join(3000L);
            } catch (InterruptedException e) {
               EntitlementUtil.LOG.warning("Unable to terminate existing polling thread: " + e);
            }
         }

         EntitlementUtil.LOG.info("Polling for registration status (will poll every " + pollInterval + " seconds for " + 10 + " minutes)");
         pollThread = tokenPoll.getThread();
         pollThread.start();
      }

      public static synchronized boolean isRunning() {
         return pollThread != null && pollThread.isAlive();
      }

      public static synchronized void stop() {
         exit = true;
      }
   }
}
