package com.tridium.crypto.core.cert;

import java.security.CodeSigner;

public class NoOpCertificateChainValidator extends CertificateChainValidator {
   @Override
   public void validateCertChain(CodeSigner signer, boolean checkTpk) throws ValidationException {
   }
}
