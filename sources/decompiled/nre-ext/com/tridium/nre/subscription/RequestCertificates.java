package com.tridium.nre.subscription;

import com.tridium.json.JSONObject;
import com.tridium.nre.util.Version;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import javax.baja.nre.util.ByteBuffer;
import javax.baja.xml.XElem;
import javax.baja.xml.XParser;

public class RequestCertificates extends EntitlementApi {
   private boolean certificateUpdated = false;
   private static final int CERTIFICATE_REQUEST_COUNT = 3;

   @Override
   protected String getApiName() {
      return "certificates";
   }

   @Override
   protected String getApiPath() {
      return "/ncents/certificates";
   }

   public EntitlementApi.EntitlementStatus getCertificates(String nreId, String[] vendors, String version) {
      for (int i = 1; i <= 3; i++) {
         try {
            EntitlementApi.EntitlementStatus requestStatus = this.getCertificatesApi(nreId, vendors, version, true);
            if (requestStatus.isInvalidVendor() || requestStatus.isFailure()) {
               EntitlementUtil.LOG.warning(requestStatus.getMessage());
               return requestStatus;
            }

            if (requestStatus.isSuccess()) {
               EntitlementUtil.LOG.info(requestStatus.getMessage());
               return requestStatus;
            }
         } catch (EntitlementException e) {
            if (EntitlementUtil.LOG.isLoggable(Level.FINE)) {
               EntitlementUtil.LOG.log(Level.WARNING, "Failed to retrieve certificates from the subscription licensing system.", e);
            } else {
               EntitlementUtil.LOG.log(Level.WARNING, "Failed to retrieve certificates from the subscription licensing system: " + e.getLocalizedMessage());
            }

            EntitlementUtil.LOG.warning(String.format("Failure %d of %d attempts...", i, 3));
         }
      }

      return new EntitlementApi.EntitlementStatus(
         EntitlementApi.EntitlementState.FAILURE, 0, "Failed to request certificates from the subscription licensing server."
      );
   }

   public EntitlementApi.EntitlementStatus getCertificatesApi(String nreId, String[] vendors, String version, boolean writeFile) {
      this.certificateUpdated = false;
      JSONObject requestBody = new JSONObject().put("version", version).put("vendors", vendors).put("nreId", nreId);
      JSONObject response = this.sendRequest(requestBody, this.makeJwtAuthHeader());
      EntitlementApi.EntitlementStatus errorStatus = this.checkErrorResponse(response);
      if (errorStatus != null) {
         return errorStatus;
      }

      EntitlementApi.EntitlementStatus entitlementCertificates = new EntitlementApi.EntitlementStatus(
         EntitlementApi.EntitlementState.SUCCESS, 200, "Certificate request completed successfully."
      );

      for (String vendor : vendors) {
         ByteBuffer xmlBuffer = new ByteBuffer(((JSONObject)response.get("certificates")).optString(vendor + ".certificate").getBytes(StandardCharsets.UTF_8));

         XElem certificateElem;
         try {
            certificateElem = XParser.make(xmlBuffer.getInputStream()).parse();
         } catch (Exception e) {
            throw new EntitlementException("Unable to parse incoming certificate to XElem object.", e);
         }

         try {
            entitlementCertificates.addCertificate(certificateElem);
            if (writeFile) {
               SubscriptionLicenseUtil slu = SubscriptionLicenseUtil.getInstance();
               this.certificateUpdated = slu.writeCertificate(certificateElem);
            }
         } catch (Exception e) {
            throw new EntitlementException("Unable to write to certificate file.", e);
         }
      }

      return entitlementCertificates;
   }

   @Override
   protected EntitlementApi.EntitlementStatus doCheckEndpointErrorResponse(String responseType, String responseMessage) {
      return EntitlementApi.EntitlementState.INVALID_VENDOR.toString().equals(responseType)
         ? new EntitlementApi.EntitlementStatus(EntitlementApi.EntitlementState.INVALID_VENDOR, 409, "Invalid vendors: " + responseMessage)
         : null;
   }

   public String getCertificateVersion() {
      String certificateVersion = SubscriptionLicenseUtil.getLicenseProperties().getProperty("license.certificateVersion", "2.0");

      try {
         new Version(certificateVersion);
         return certificateVersion;
      } catch (IllegalArgumentException e) {
         EntitlementUtil.LOG
            .warning(
               String.format("Illegal certificate version defined in license.properties file '%s'. Using default version '%s'", certificateVersion, "2.0")
            );
         return "2.0";
      }
   }

   public boolean isCertificateUpdated() {
      return this.certificateUpdated;
   }
}
