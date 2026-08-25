package com.tridium.nre.subscription;

import com.tridium.json.JSONObject;
import com.tridium.nre.security.KeyRing;
import java.security.PublicKey;
import java.util.Base64;

public class RotateKeys extends EntitlementApi {
   @Override
   protected String getApiName() {
      return "rotateKeys";
   }

   @Override
   protected String getApiPath() {
      return "/ncents/authn/api_key";
   }

   public EntitlementApi.EntitlementStatus rotateKeysApi() {
      try (JwtSignatureKeys.KeyRotation keyRotation = JwtSignatureKeys.getInstance().initKeyRotation()) {
         String nreId = SubscriptionLicenseUtil.getNreId();
         PublicKey jwtPublicKey = keyRotation.getNewPublicKey();
         String publicKey = Base64.getEncoder().encodeToString(jwtPublicKey.getEncoded());
         JSONObject requestBody = new JSONObject().put("nreId", nreId).put("publicKey", publicKey);
         JSONObject response = this.sendRequest(requestBody, this.makeJwtAuthHeader());
         EntitlementApi.EntitlementStatus errorStatus = this.checkErrorResponse(response);
         if (errorStatus != null) {
            if (errorStatus.isKeyRotationFailure()) {
               throw new EntitlementException(errorStatus.getMessage());
            } else {
               return errorStatus;
            }
         } else {
            boolean keyUpdated = response.optBoolean("updated", false);
            if (!keyUpdated) {
               throw new EntitlementException("The key update was denied by the subscription licensing system.");
            }

            keyRotation.commit();

            try {
               EntitlementApi.EntitlementStatus keyMaterialRollStatus = this.checkRollKeyMaterial(SubscriptionLicenseUtil.getInstance().getKeyRing());
               if (!keyMaterialRollStatus.isSuccess()) {
                  return keyMaterialRollStatus;
               }
            } catch (Exception rollException) {
               throw new KeyRotationException("Failed to roll key material", rollException);
            }

            return new EntitlementApi.EntitlementStatus(EntitlementApi.EntitlementState.SUCCESS, 200, "Key rotation successful");
         }
      } catch (KeyRotationException | EntitlementException e) {
         throw new EntitlementException("Key rotation failed: " + e.getLocalizedMessage(), e);
      }
   }

   private EntitlementApi.EntitlementStatus checkRollKeyMaterial(KeyRing keyRing) {
      if (!keyRing.checkSupportsKeyRecovery()) {
         return new EntitlementApi.EntitlementStatus(
            EntitlementApi.EntitlementState.FAILURE, 200, "Unable to roll key material: Key recovery not supported on this platform"
         );
      }

      keyRing.rollKeyMaterial();
      return new EntitlementApi.EntitlementStatus(EntitlementApi.EntitlementState.SUCCESS, 200, "Key material rolled successfully");
   }

   @Override
   protected EntitlementApi.EntitlementStatus doCheckEndpointErrorResponse(String responseType, String responseMessage) {
      return "key".equals(responseType)
         ? new EntitlementApi.EntitlementStatus(
            EntitlementApi.EntitlementState.KEY_ROTATION_FAILURE, 409, "The key update on the subscription licensing system failed: " + responseMessage
         )
         : null;
   }
}
