package com.tridium.crypto.core.cert;

import javax.baja.nre.security.ExemptionApprover;

public class DefaultExemptionApprover implements ExemptionApprover {
   @Override
   public boolean approveExemption(CertValidationResult result) {
      return false;
   }
}
