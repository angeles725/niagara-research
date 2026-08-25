package com.tridium.crypto.core.exchange;

import com.tridium.nre.security.SecretBytes;
import java.math.BigInteger;
import java.security.SecureRandom;
import org.bouncycastle.util.BigIntegers;

public final class SRP6KeyExchangerClient extends SRP6KeyExchanger {
   private final SRP6Pbkdf2Client client = new SRP6Pbkdf2Client();
   private SRP6KeyExchangerClient.State state = SRP6KeyExchangerClient.State.START;
   private BigInteger key;

   public SRP6KeyExchangerClient(SRP6AlgorithmBundle bundle) {
      super(bundle);
   }

   @Override
   public void init() {
      this.client.init(this.algorithmBundle.getParameters(), this.algorithmBundle.getTlsHash(), new SecureRandom());
      this.state = SRP6KeyExchangerClient.State.INITIALIZED;
   }

   @Override
   public byte[] doInitialStep(SecretBytes sharedSecret) {
      BigInteger A = this.client.generateClientCredentials(sharedSecret.get());
      this.state = SRP6KeyExchangerClient.State.CLIENT_CRENDENTIALS;
      return BigIntegers.asUnsignedByteArray(A);
   }

   @Override
   public byte[] doExchangeStep(byte[] input) {
      switch (this.state) {
         case START:
            throw new IllegalStateException("Not initialized");
         case INITIALIZED:
            throw new IllegalStateException("Initial step not completed");
         case CLIENT_CRENDENTIALS:
            BigInteger B = BigIntegers.fromUnsignedByteArray(input);

            try {
               this.client.calculateSecret(B);
               BigInteger M1 = this.client.calculateClientEvidenceMessage();
               this.state = SRP6KeyExchangerClient.State.MESSAGE_VERIFY;
               return BigIntegers.asUnsignedByteArray(M1);
            } catch (IllegalStateException var5) {
               return null;
            }
         case MESSAGE_VERIFY:
            BigInteger M2 = BigIntegers.fromUnsignedByteArray(input);

            try {
               if (this.client.verifyServerEvidenceMessage(M2)) {
                  this.key = this.client.calculateSessionKey();
                  this.state = SRP6KeyExchangerClient.State.COMPLETE;
               } else {
                  this.state = SRP6KeyExchangerClient.State.FAILED;
               }
            } catch (IllegalStateException e) {
               this.state = SRP6KeyExchangerClient.State.FAILED;
            }

            return null;
         default:
            return null;
      }
   }

   @Override
   public byte[] getKey() {
      return this.key != null ? BigIntegers.asUnsignedByteArray(this.key) : null;
   }

   private enum State {
      START,
      INITIALIZED,
      CLIENT_CRENDENTIALS,
      MESSAGE_VERIFY,
      COMPLETE,
      FAILED;
   }
}
