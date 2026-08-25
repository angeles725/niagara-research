package com.tridium.nre.subscription;

import com.tridium.json.JSONObject;
import com.tridium.json.JSONTokener;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.NoRouteToHostException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.AccessController;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import javax.baja.xml.XElem;
import okhttp3.MediaType;
import org.jose4j.jws.JsonWebSignature;

public abstract class EntitlementApi {
   public static final JSONObject EMPTY_JSON_OBJECT = new JSONObject();
   protected HttpConnectionlessTransport transport = new HttpConnectionlessTransport();
   private static final String VERSION = "";

   protected EntitlementApi() {
   }

   protected String getConnectionUrl() {
      return SubscriptionLicenseUtil.getLicenseProperties().getProperty("license.entitlementUrl", "https://www.niagaracentralapis.honeywell.com");
   }

   protected String getConnectionPort() {
      return SubscriptionLicenseUtil.getLicenseProperties().getProperty("license.entitlementPort", "443");
   }

   protected HttpRequestMessage makeRequest(JSONObject requestBody, String endpoint) {
      return this.makeRequest(requestBody, endpoint, new HashMap<>());
   }

   protected HttpRequestMessage makeRequest(JSONObject requestBody, String endpoint, Map<String, Object> authHeaderMap) {
      return this.makeRequest(requestBody.toString(), endpoint, authHeaderMap, "application/json");
   }

   protected HttpRequestMessage makeRequest(String requestBody, String endpoint, Map<String, Object> authHeaderMap, String mimeType) {
      String entitlementUrl = this.getConnectionUrl();
      String entitlementPort = this.getConnectionPort();
      entitlementPort = SubscriptionLicenseUtil.getLicenseProperties().getProperty("api." + this.getApiName() + ".port", entitlementPort);
      String entitlementBasePath = SubscriptionLicenseUtil.getLicenseProperties().getProperty("license.entitlementBasePath", "");
      String apiVersion = SubscriptionLicenseUtil.getLicenseProperties().getProperty("api." + this.getApiName() + ".apiVersion", this.getApiVersion());
      if (!"443".equals(entitlementPort)) {
         entitlementUrl = entitlementUrl + ':' + entitlementPort;
      }

      if (!"".equalsIgnoreCase(entitlementBasePath)) {
         if (entitlementBasePath.startsWith("/")) {
            entitlementUrl = entitlementUrl + entitlementBasePath;
         } else {
            entitlementUrl = entitlementUrl + '/' + entitlementBasePath;
         }
      }

      if (!apiVersion.isEmpty() && !apiVersion.startsWith("/")) {
         apiVersion = '/' + apiVersion;
      }

      if (!endpoint.startsWith("/")) {
         endpoint = '/' + endpoint;
      }

      HttpRequestMessage requestMessage = null;

      try {
         requestMessage = new HttpRequestMessage(
            HttpRequestMessage.Method.POST, new URL(entitlementUrl + apiVersion + endpoint), authHeaderMap, mimeType, requestBody
         );
      } catch (MalformedURLException urlException) {
         if (EntitlementUtil.LOG.isLoggable(Level.FINE)) {
            EntitlementUtil.LOG.log(Level.WARNING, "Request contained malformed URL.", urlException);
         } else {
            EntitlementUtil.LOG.log(Level.WARNING, "Request contained malformed URL.");
         }
      } catch (Exception e) {
         if (EntitlementUtil.LOG.isLoggable(Level.FINE)) {
            EntitlementUtil.LOG.log(Level.WARNING, "Request creation failed.", e);
         } else {
            EntitlementUtil.LOG.log(Level.WARNING, "Request creation failed. " + e.getLocalizedMessage());
         }
      }

      return requestMessage;
   }

   protected JSONObject sendRequestMessageAndHandleResponse(HttpRequestMessage requestMessage) {
      HttpResponseMessage responseMessage = null;

      try {
         HttpMessageWrapper<HttpRequestMessage> wrapper = new HttpMessageWrapper<>(requestMessage, new CompletableFuture<>());
         this.transport.send(wrapper);
         Object result = wrapper.getTransportFuture().get();
         responseMessage = (HttpResponseMessage)result;
         return this.handleResponse(responseMessage);
      } catch (Exception e) {
         Throwable cause = e.getCause();
         if (cause instanceof HttpStatusException) {
            HttpStatusException statusException = (HttpStatusException)cause;
            return this.handleResponseError(statusException.getStatusCode(), statusException.getType(), statusException.getMessage());
         }

         if (cause instanceof SocketTimeoutException) {
            return this.handleResponseError(408, "Request timeout", e.getMessage());
         }

         if (cause instanceof NoRouteToHostException) {
            return this.handleResponseError(502, "No route to host", e.getMessage());
         }

         if (cause instanceof UnknownHostException) {
            return this.handleResponseError(502, "Unknown host", e.getMessage());
         }

         if (EntitlementUtil.LOG.isLoggable(Level.FINE)) {
            EntitlementUtil.LOG.log(Level.WARNING, "Subscription licensing transaction failed.", e);
         } else {
            EntitlementUtil.LOG.log(Level.WARNING, "Subscription licensing transaction failed. " + e.getMessage());
         }

         return this.handleResponseError(500, "Server error prevented successful request completion", e.getMessage());
      } finally {
         if (responseMessage != null) {
            try {
               responseMessage.close();
            } catch (IOException e) {
               if (EntitlementUtil.LOG.isLoggable(Level.FINE)) {
                  EntitlementUtil.LOG.log(Level.WARNING, "Error closing the connection: ", e);
               } else {
                  EntitlementUtil.LOG.log(Level.WARNING, "Error closing the connection: " + e.getMessage());
               }
            }
         }
      }
   }

   protected JSONObject sendRequest(JSONObject requestBody) {
      return this.sendRequest(requestBody, Collections.emptyMap());
   }

   protected JSONObject sendRequest(JSONObject requestBody, Map<String, Object> headers) {
      if (requestBody == null) {
         throw new EntitlementException("Unable to generate request to '" + this.getApiPath() + "' due to null request body");
      } else {
         HttpRequestMessage requestMessage = this.makeRequest(requestBody, this.getApiPath(), headers);
         if (requestMessage == null) {
            throw new EntitlementException("Unable to generate request to '" + this.getApiPath() + "' due to null request message");
         } else {
            logRequest(requestMessage.getUrl(), requestBody, headers);
            JSONObject response = this.sendRequestMessageAndHandleResponse(requestMessage);
            if (response != null && !response.isEmpty()) {
               logResponse(requestMessage.getUrl(), response);
               return response;
            } else {
               throw new EntitlementException("Unable to parse the response from '" + this.getApiPath() + '\'');
            }
         }
      }
   }

   protected JSONObject sendRequest(String requestBody, Map<String, Object> headers, String mimeType) {
      if (requestBody == null) {
         throw new EntitlementException("Unable to generate request to '" + this.getApiPath() + "' due to null request body");
      } else {
         HttpRequestMessage requestMessage = this.makeRequest(requestBody, this.getApiPath(), headers, mimeType);
         if (requestMessage == null) {
            throw new EntitlementException("Unable to generate request to '" + this.getApiPath() + "' due to null request message");
         } else {
            logRequest(requestMessage.getUrl(), requestBody, headers);
            JSONObject response = this.sendRequestMessageAndHandleResponse(requestMessage);
            if (response != null && !response.isEmpty()) {
               logResponse(requestMessage.getUrl(), response);
               return response;
            } else {
               throw new EntitlementException("Unable to parse the response from '" + this.getApiPath() + '\'');
            }
         }
      }
   }

   private static void logRequest(URL url, Object req, Map<String, Object> headers) {
      if (EntitlementUtil.LOG.isLoggable(Level.FINE)) {
         EntitlementUtil.LOG.fine("Request to: " + url);
         if (EntitlementUtil.LOG.isLoggable(Level.FINER)) {
            String loggedRequest;
            if (req instanceof JSONObject) {
               loggedRequest = sanitizeMessage((JSONObject)req).toString(2);
            } else {
               loggedRequest = req.toString();
            }

            EntitlementUtil.LOG.finer("Request message body: \n" + loggedRequest);
            if (EntitlementUtil.LOG.isLoggable(Level.FINEST)) {
               EntitlementUtil.LOG.finest("Request headers: \n" + getSanitizedRequestHeadersForLog(headers));
            }
         }
      }
   }

   private static String getSanitizedRequestHeadersForLog(Map<String, Object> headers) {
      StringBuilder loggedHeaders = new StringBuilder();

      for (Entry<String, Object> header : headers.entrySet()) {
         Object value = header.getValue();
         if ("AUTHORIZATION".equalsIgnoreCase(header.getKey())) {
            try {
               JsonWebSignature jws = new JsonWebSignature();
               jws.setCompactSerialization(value.toString().substring("Bearer ".length()));
               AccessController.doPrivileged(() -> {
                  jws.setKey(JwtSignatureKeys.getInstance().getPublicKey());
                  return null;
               });
               value = jws.getPayload();
            } catch (Exception ignore) {
               value = "********";
            }
         }

         loggedHeaders.append(header.getKey()).append('=').append(value).append('\n');
      }

      return loggedHeaders.toString();
   }

   private static void logResponse(URL url, JSONObject resp) {
      if (EntitlementUtil.LOG.isLoggable(Level.FINE)) {
         EntitlementUtil.LOG.fine("Response from: " + url);
      }

      if (EntitlementUtil.LOG.isLoggable(Level.FINER)) {
         EntitlementUtil.LOG.finer("Response message body: \n" + sanitizeMessage(resp).toString(2));
      }
   }

   protected abstract String getApiName();

   protected abstract String getApiPath();

   protected String getApiVersion() {
      return "";
   }

   private static JSONObject sanitizeMessage(JSONObject msg) {
      JSONObject sanitized = new JSONObject();
      Iterator keys = msg.keys();

      while (keys.hasNext()) {
         String key = (String)keys.next();
         if ("publicKey".equals(key)) {
            sanitized.put("publicKey", "********");
         } else if ("refreshIncrement".equals(key)) {
            sanitized.put("refreshIncrement", "********");
         } else if ("restoreId".equals(key)) {
            sanitized.put("restoreId", "********");
         } else {
            sanitized.put(key, msg.get(key));
         }
      }

      return sanitized;
   }

   protected JSONObject handleResponseError(int statusCode, String statusType, String responseMessage) {
      String warningMessage;
      switch (statusCode) {
         case 400:
            warningMessage = "Request body contained invalid parameters";
            break;
         case 401:
            warningMessage = "Request authorization not provided";
            break;
         case 403:
            warningMessage = "Request authorization failed on the server";
            break;
         case 408:
            warningMessage = "Request timeout";
            break;
         case 409:
            warningMessage = "Server endpoint generated an error";
            break;
         case 500:
            warningMessage = "Server error prevented successful request completion";
            break;
         case 502:
            warningMessage = "Bad gateway";
            break;
         default:
            warningMessage = "Unrecognized response code: " + statusCode;
      }

      EntitlementUtil.LOG.warning(warningMessage);
      if (EntitlementUtil.LOG.isLoggable(Level.FINE)) {
         EntitlementUtil.LOG.fine("Status code: " + statusCode + ", Response message body: " + responseMessage);
      }

      return new JSONObject().put("code", statusCode).put("type", statusType).put("message", responseMessage);
   }

   protected JSONObject handleResponse(HttpResponseMessage responseMessage) {
      if (responseMessage.getStatusCode() == 200) {
         try {
            MediaType mediaType = responseMessage.getContentType();
            if (mediaType == null) {
               throw new EntitlementException("Response does not contain a content type");
            }

            String responseType = responseMessage.getContentType().subtype();
            if (responseType.toLowerCase().contains("json")) {
               return new JSONObject(new JSONTokener(responseMessage.getBodyAsString()));
            }
         } catch (Exception e) {
            if (EntitlementUtil.LOG.isLoggable(Level.FINE)) {
               EntitlementUtil.LOG.log(Level.WARNING, "Error parsing response message body JSON: ", e);
            } else {
               EntitlementUtil.LOG.log(Level.WARNING, "Error parsing response message body JSON: " + e.getMessage());
            }
         }
      }

      return EMPTY_JSON_OBJECT;
   }

   protected Map<String, Object> makeJwtAuthHeader() {
      return this.makeJwtAuthHeader(SubscriptionLicenseUtil.getNreId());
   }

   protected Map<String, Object> makeJwtAuthHeader(String hostId) {
      try {
         return Collections.singletonMap("Authorization", "Bearer " + EntitlementUtil.makeJwtHeaderString(hostId));
      } catch (Exception e) {
         throw new EntitlementException("Authorization header creation failed.", e);
      }
   }

   protected EntitlementApi.EntitlementStatus checkErrorResponse(JSONObject response) {
      if (response.has("code")) {
         int httpStatusCode = response.optInt("code");
         if (httpStatusCode == 409) {
            return this.checkEndpointErrorResponse(response);
         }

         if (httpStatusCode >= 400 && httpStatusCode < 600) {
            String errorType = response.optString("type");
            if ("authorization_pending".equals(errorType)) {
               EntitlementApi.EntitlementStatus errorStatus = new EntitlementApi.EntitlementStatus(
                  EntitlementApi.EntitlementState.FAILURE, httpStatusCode, "The registration has not been approved on the subscription licensing system."
               );
               errorStatus.setAccessTokenErrorType("authorization_pending");
               return errorStatus;
            }

            if ("too_fast".equals(errorType)) {
               EntitlementApi.EntitlementStatus errorStatus = new EntitlementApi.EntitlementStatus(
                  EntitlementApi.EntitlementState.FAILURE, httpStatusCode, "Polling for access token faster than the server's expected rate."
               );
               errorStatus.setAccessTokenErrorType("too_fast");
               return errorStatus;
            }

            return new EntitlementApi.EntitlementStatus(
               EntitlementApi.EntitlementState.FAILURE,
               httpStatusCode,
               response.optString("message", "Subscription licensing API request failed due to an HTTP return error.")
            );
         }

         if (httpStatusCode != 200) {
            return new EntitlementApi.EntitlementStatus(
               EntitlementApi.EntitlementState.FAILURE, httpStatusCode, "Subscription licensing API request failed due to an unexpected HTTP response."
            );
         }
      }

      return null;
   }

   private EntitlementApi.EntitlementStatus checkEndpointErrorResponse(JSONObject response) {
      String responseType = response.optString("type");
      String responseMessage = response.optString("message", "");
      EntitlementApi.EntitlementStatus status = this.doCheckEndpointErrorResponse(responseType, responseMessage);
      if (status != null) {
         return status;
      }

      if (EntitlementUtil.LOG.isLoggable(Level.FINE)) {
         EntitlementUtil.LOG.fine("Unknown endpoint error: " + responseType + ": " + responseMessage);
      }

      return new EntitlementApi.EntitlementStatus(EntitlementApi.EntitlementState.FAILURE, 409, responseMessage);
   }

   protected abstract EntitlementApi.EntitlementStatus doCheckEndpointErrorResponse(String var1, String var2);

   public enum EntitlementState {
      SUCCESS {
         @Override
         public String toString() {
            return "success";
         }
      },
      FAILURE {
         @Override
         public String toString() {
            return "failure";
         }
      },
      INVALID_VENDOR {
         @Override
         public String toString() {
            return "invalid vendor";
         }
      },
      INVALID_REFRESH_TOKEN {
         @Override
         public String toString() {
            return "invalid refresh token";
         }
      },
      LICENSE_EXPIRED {
         @Override
         public String toString() {
            return "license expired";
         }
      },
      LICENSE_REVOKED {
         @Override
         public String toString() {
            return "license revoked";
         }
      },
      REGISTERED {
         @Override
         public String toString() {
            return "registered";
         }
      },
      RESTORE {
         @Override
         public String toString() {
            return "restore";
         }
      },
      KEY_ROTATION_FAILURE {
         @Override
         public String toString() {
            return "key rotation failure";
         }
      };

      EntitlementState() {
      }
   }

   public static class EntitlementStatus {
      private final String message;
      private final EntitlementApi.EntitlementState state;
      private final XElem entitlements = new XElem("licenses");
      private final XElem certificates = new XElem("certificates");
      private final int code;
      private String user_code;
      private String device_code;
      private String verification_uri;
      private int interval;
      private String accessToken;
      private String signature;
      private String scope;
      private String id;
      private String instanceUrl;
      private String tokenType;
      private long issuedAt;
      private String accessTokenErrorType;

      public EntitlementStatus(EntitlementApi.EntitlementState entitlementState, int statusCode, String entitlementMessage) {
         this.state = entitlementState;
         this.message = entitlementMessage;
         this.code = statusCode;
      }

      public EntitlementStatus(
         EntitlementApi.EntitlementState entitlementState,
         int statusCode,
         String entitlementMessage,
         String userCode,
         String verificationUri,
         String deviceCode,
         int val
      ) {
         this.state = entitlementState;
         this.message = entitlementMessage;
         this.code = statusCode;
         this.user_code = userCode;
         this.verification_uri = verificationUri;
         this.device_code = deviceCode;
         this.interval = val;
      }

      public EntitlementStatus(
         EntitlementApi.EntitlementState entitlementState,
         int statusCode,
         String entitlementMessage,
         String accessToken,
         String signature,
         String scope,
         String id,
         String instanceUrl,
         String tokenType,
         long issuedAt,
         String accessTokenErrorType
      ) {
         this.state = entitlementState;
         this.message = entitlementMessage;
         this.code = statusCode;
         this.accessToken = accessToken;
         this.signature = signature;
         this.scope = scope;
         this.id = id;
         this.instanceUrl = instanceUrl;
         this.tokenType = tokenType;
         this.issuedAt = issuedAt;
         this.accessTokenErrorType = accessTokenErrorType;
      }

      public EntitlementApi.EntitlementState getState() {
         return this.state;
      }

      public boolean isSuccess() {
         return this.state == EntitlementApi.EntitlementState.SUCCESS;
      }

      public boolean isFailure() {
         return this.state == EntitlementApi.EntitlementState.FAILURE;
      }

      public boolean isInvalidVendor() {
         return this.state == EntitlementApi.EntitlementState.INVALID_VENDOR;
      }

      public boolean isInvalidRefreshToken() {
         return this.state == EntitlementApi.EntitlementState.INVALID_REFRESH_TOKEN;
      }

      public boolean isLicenseExpired() {
         return this.state == EntitlementApi.EntitlementState.LICENSE_EXPIRED;
      }

      public boolean isLicenseRevoked() {
         return this.state == EntitlementApi.EntitlementState.LICENSE_REVOKED;
      }

      public boolean isAlreadyRegistered() {
         return this.state == EntitlementApi.EntitlementState.REGISTERED;
      }

      public boolean isRestore() {
         return this.state == EntitlementApi.EntitlementState.RESTORE;
      }

      public boolean isKeyRotationFailure() {
         return this.state == EntitlementApi.EntitlementState.KEY_ROTATION_FAILURE;
      }

      public String getMessage() {
         return this.message;
      }

      public int getCode() {
         return this.code;
      }

      public void addEntitlement(XElem licenseContents) {
         this.entitlements.addContent(licenseContents);
      }

      public String getUserCode() {
         return this.user_code;
      }

      public String getDeviceCode() {
         return this.device_code;
      }

      public String getVerificationUri() {
         return this.verification_uri;
      }

      public int getInterval() {
         return this.interval;
      }

      protected void setUserCode(String userCode) {
         this.user_code = userCode;
      }

      protected void setDeviceCode(String deviceCode) {
         this.device_code = deviceCode;
      }

      protected void setVerificationUri(String verificationUri) {
         this.verification_uri = verificationUri;
      }

      protected void setInterval(int val) {
         this.interval = val;
      }

      void addCertificate(XElem certificateContents) {
         this.certificates.addContent(certificateContents);
      }

      public XElem getLicenses() {
         return this.entitlements.deepcopy();
      }

      public XElem getCertificates() {
         return this.certificates.deepcopy();
      }

      public String getAccessToken() {
         return this.accessToken;
      }

      public void setAccessToken(String token) {
         this.accessToken = token;
      }

      public String getSignature() {
         return this.signature;
      }

      public void setSignature(String entitlementStatusSignature) {
         this.signature = entitlementStatusSignature;
      }

      public String getScope() {
         return this.scope;
      }

      public void setScope(String entitlementStatusScope) {
         this.scope = entitlementStatusScope;
      }

      public String getInstanceUrl() {
         return this.instanceUrl;
      }

      public void setInstanceUrl(String url) {
         this.instanceUrl = url;
      }

      public String getTokenType() {
         return this.tokenType;
      }

      public void setTokenType(String type) {
         this.tokenType = type;
      }

      public long getIssuedAt() {
         return this.issuedAt;
      }

      public void setIssuedAt(long issued) {
         this.issuedAt = issued;
      }

      public String getId() {
         return this.id;
      }

      public void setId(String statusId) {
         this.id = statusId;
      }

      public String getAccessTokenErrorType() {
         return this.accessTokenErrorType;
      }

      public void setAccessTokenErrorType(String errorType) {
         this.accessTokenErrorType = errorType;
      }
   }
}
