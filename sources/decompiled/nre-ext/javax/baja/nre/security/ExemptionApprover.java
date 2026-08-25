package javax.baja.nre.security;

import com.tridium.crypto.core.cert.CertValidationResult;

public interface ExemptionApprover {
   boolean approveExemption(CertValidationResult var1);

   default boolean isTransientApproval() {
      return false;
   }
}
