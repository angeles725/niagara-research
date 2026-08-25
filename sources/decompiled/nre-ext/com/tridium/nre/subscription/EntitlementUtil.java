package com.tridium.nre.subscription;

import com.tridium.nre.security.NiagaraBasicPermission;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.security.AccessController;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;

public final class EntitlementUtil {
   public static final String KEY_ID_HEADER_VALUE = "K1";
   public static final String DEFAULT_JWT_AUDIENCE = "www.niagaracentralapis.honeywell.com";
   public static final String DEFAULT_JWT_EXPIRATION_MINUTES = "10";
   public static final String DEFAULT_CERT_VERSION = "2.0";
   public static final int HTTP_OK = 200;
   public static final int HTTP_ACCEPT = 202;
   public static final int ERROR_CODE_INVALID_PARAMETERS = 400;
   public static final int ERROR_CODE_AUTHORIZATION_NOT_PROVIDED = 401;
   public static final int ERROR_CODE_NOT_AUTHORIZED = 403;
   public static final int ERROR_CODE_REQUEST_TIMEOUT = 408;
   public static final int ERROR_CODE_ENDPOINT_SPECIFIC_ERROR = 409;
   public static final int ERROR_CODE_SERVER_ERROR = 500;
   public static final int ERROR_CODE_BAD_GATEWAY = 502;
   public static final String ERROR_TYPE_ACCESS_TOKEN_AUTH_PENDING = "authorization_pending";
   public static final String ERROR_TYPE_TOO_FAST = "too_fast";
   public static final String ERROR_TEXT_ACCESS_TOKEN_AUTH_PENDING = "The registration has not been approved on the subscription licensing system.";
   public static final String ERROR_TEXT_TOO_FAST = "Polling for access token faster than the server's expected rate.";
   public static final String ERROR_TEXT_INVALID_PARAMETERS = "Request body contained invalid parameters";
   public static final String ERROR_TEXT_AUTHORIZATION_NOT_PROVIDED = "Request authorization not provided";
   public static final String ERROR_TEXT_NOT_AUTHORIZED = "Request authorization failed on the server";
   public static final String ERROR_TEXT_REQUEST_TIMEOUT = "Request timeout";
   public static final String ERROR_TEXT_NO_ROUTE_TO_HOST = "No route to host";
   public static final String ERROR_TEXT_BAD_GATEWAY = "Bad gateway";
   public static final String ERROR_TEXT_UNKNOWN_HOST = "Unknown host";
   public static final String ERROR_TEXT_ENDPOINT_SPECIFIC_ERROR = "Server endpoint generated an error";
   public static final String ERROR_TEXT_SERVER_ERROR = "Server error prevented successful request completion";
   public static final String MIME_TYPE_APPLICATION_JSON = "application/json";
   public static final String MIME_TYPE_APPLICATION_URL_ENCODED = "application/x-www-form-urlencoded";
   public static final String DEFAULT_ENTITLEMENT_URL = "https://www.niagaracentralapis.honeywell.com";
   public static final String DEFAULT_ENTITLEMENT_PORT = "443";
   public static final String DEFAULT_REGISTRATION_URL = "https://www.niagara-community.com";
   public static final String DEFAULT_REGISTRATION_PORT = "443";
   public static final String DEFAULT_ENTITLEMENT_BASE_PATH = "";
   public static final String NCENTS_REGISTER_API_NAME = "register";
   public static final String NCENTS_REGISTER_API_PATH = "/ncents/register";
   public static final String DEVICE_REGISTRATION_HOST = "www.niagara-community.com";
   public static final String DEVICE_REGISTRATION_CLIENT_ID = "3MVG9WtWSKUDG.x4cTAX2e5oo6IfJqws2TuRetQcx7sHItlY6JwN_ukEGkqnP3w.HL_H4p.jBvbB2tMehZ6cx";
   public static final String DEVICE_REGISTRATION_RESPONSE_TYPE = "device_code";
   public static final String DEVICE_REGISTRATION_GRANT_TYPE = "device";
   public static final String DEVICE_CODE_API_NAME = "deviceCode";
   public static final String DEVICE_CODE_API_PATH = "/services/oauth2/token";
   public static final String ACCESS_TOKEN_API_NAME = "accessToken";
   public static final String ACCESS_TOKEN_API_PATH = "/services/oauth2/token";
   public static final String ROTATE_KEY_API_NAME = "rotateKeys";
   public static final String ROTATE_KEY_API_PATH = "/ncents/authn/api_key";
   public static final String ENTITLEMENTS_API_NAME = "entitlements";
   public static final String ENTITLEMENTS_API_PATH = "/ncents/entitlements";
   public static final String CERTIFICATES_API_NAME = "certificates";
   public static final String CERTIFICATES_API_PATH = "/ncents/certificates";
   public static final String STATION_PRODUCT_ID = "station";
   public static final String WORKBENCH_PRODUCT_ID = "workbench";
   public static final Logger LOG = Logger.getLogger("licensing.subscription");
   static final SecureRandom RANDOM = new SecureRandom();
   static final NiagaraBasicPermission ENTITLEMENT_WORKFLOW_PERMISSION = new NiagaraBasicPermission("ENTITLEMENT_WORKFLOW");
   static final NiagaraBasicPermission RESET_ENTITLEMENT_PERMISSION = new NiagaraBasicPermission("RESET_ENTITLEMENT");

   private EntitlementUtil() {
   }

   public static String makeJwtHeaderString(String hostid) throws Exception {
      SecurityManager sm = System.getSecurityManager();
      if (sm != null) {
         sm.checkPermission(ENTITLEMENT_WORKFLOW_PERMISSION);
      }

      String jwtAudience = SubscriptionLicenseUtil.getLicenseProperties().getProperty("license.entitlementServerName", "www.niagaracentralapis.honeywell.com");
      String jwtExpiration = SubscriptionLicenseUtil.getLicenseProperties().getProperty("license.entitlementApiExpiration", "10");
      float expiration = Float.parseFloat("10");

      try {
         expiration = Float.parseFloat(jwtExpiration);
      } catch (NumberFormatException e) {
         if (LOG.isLoggable(Level.FINE)) {
            LOG.log(Level.WARNING, "JWT expiration override failed.", e);
         } else {
            LOG.log(Level.WARNING, "JWT expiration override failed. " + e.getLocalizedMessage());
         }
      }

      JwtClaims claims = new JwtClaims();
      claims.setSubject(hostid);
      claims.setAudience(jwtAudience);
      claims.setExpirationTimeMinutesInTheFuture(expiration);
      claims.setIssuedAtToNow();
      JsonWebSignature jws = new JsonWebSignature();
      jws.setPayload(claims.toJson());
      AccessController.doPrivileged(() -> {
         jws.setKey(JwtSignatureKeys.getInstance().getPrivateKey());
         return null;
      });
      jws.setKeyIdHeaderValue("K1");
      jws.setAlgorithmHeaderValue("ES256");
      return jws.getCompactSerialization();
   }

   private static Instant readRegistrationTime() {
      try (DataInputStream in = new DataInputStream(new FileInputStream(RegistrationApi.REGISTRATION_FILE))) {
         return Instant.parse(in.readUTF());
      } catch (Exception e) {
         if (LOG.isLoggable(Level.FINE)) {
            LOG.log(Level.WARNING, "Registration file was corrupted. ", e);
         } else {
            LOG.log(Level.WARNING, "Registration file was corrupted. " + e.getLocalizedMessage());
         }

         if (!RegistrationApi.REGISTRATION_FILE.delete()) {
            LOG.log(Level.WARNING, "Failed to delete corrupted registration file: " + RegistrationApi.REGISTRATION_FILE);
         }

         return Instant.EPOCH;
      }
   }

   public static boolean isRegistered() {
      return RegistrationApi.REGISTRATION_FILE.exists() ? !readRegistrationTime().equals(Instant.EPOCH) : false;
   }

   public static Instant getRegistrationTime() {
      return RegistrationApi.REGISTRATION_FILE.exists() ? readRegistrationTime() : Instant.EPOCH;
   }

   public static String getDeviceRegistrationClientId() {
      return SubscriptionLicenseUtil.getLicenseProperties()
         .getProperty("license.clientId", "3MVG9WtWSKUDG.x4cTAX2e5oo6IfJqws2TuRetQcx7sHItlY6JwN_ukEGkqnP3w.HL_H4p.jBvbB2tMehZ6cx");
   }

   public static String getDeviceRegistrationHost() {
      return SubscriptionLicenseUtil.getLicenseProperties().getProperty("license.deviceRegistrationHost", "www.niagara-community.com");
   }

   public static Map<String, Object> makeDeviceCodeHeader(String host, String contentType) {
      Map<String, Object> headers = new HashMap<>();
      headers.put("Host", host);
      headers.put("Content-Type", contentType);
      return headers;
   }

   public static Map<String, Object> makeRegistrationHeader(String contentType, String accept, String authorization) {
      Map<String, Object> headers = new HashMap<>();
      headers.put("Authorization", "Bearer " + authorization);
      headers.put("Content-Type", contentType);
      headers.put("Accept", accept);
      return headers;
   }
}
