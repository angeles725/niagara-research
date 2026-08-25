package com.tridium.crypto.core.exchange;

import com.tridium.nre.security.SecretBytes;
import java.math.BigInteger;
import java.security.SecureRandom;
import org.bouncycastle.tls.crypto.SRP6Group;
import org.bouncycastle.tls.crypto.impl.jcajce.srp.SRP6Server;
import org.bouncycastle.util.BigIntegers;

public final class SRP6KeyExchangerServer extends SRP6KeyExchanger {
   private SRP6KeyExchangerServer.State state = SRP6KeyExchangerServer.State.START;
   private BigInteger key;
   private final SRP6Server server = new SRP6Pbkdf2Server();

   public SRP6KeyExchangerServer(SRP6AlgorithmBundle bundle) {
      super(bundle);
   }

   @Override
   public void init() {
   }

   @Override
   public byte[] doInitialStep(SecretBytes sharedSecret) {
      SRP6Group defaultParameters = this.algorithmBundle.getParameters();
      BigInteger x = BigIntegers.fromUnsignedByteArray(sharedSecret.get());
      BigInteger g = defaultParameters.getG();
      BigInteger N = defaultParameters.getN();
      BigInteger v = g.modPow(x, N);
      this.server.init(defaultParameters, v, this.algorithmBundle.getTlsHash(), new SecureRandom());
      this.state = SRP6KeyExchangerServer.State.CLIENT_CREDENTIALS;
      return null;
   }

   @Override
   public byte[] doExchangeStep(byte[] input) {
      switch (this.state) {
         case START:
            throw new IllegalStateException("Not initialized");
         case CLIENT_CREDENTIALS:
            BigInteger A = BigIntegers.fromUnsignedByteArray(input);
            BigInteger B = this.server.generateServerCredentials();

            try {
               this.server.calculateSecret(A);
               this.state = SRP6KeyExchangerServer.State.VERIFY_M1;
               return BigIntegers.asUnsignedByteArray(B);
            } catch (IllegalStateException e) {
               this.state = SRP6KeyExchangerServer.State.FAILED;
               return null;
            }
         case VERIFY_M1:
            BigInteger M1 = BigIntegers.fromUnsignedByteArray(input);

            try {
               if (this.server.verifyClientEvidenceMessage(M1)) {
                  BigInteger M2 = this.server.calculateServerEvidenceMessage();
                  this.key = this.server.calculateSessionKey();
                  this.state = SRP6KeyExchangerServer.State.COMPLETE;
                  return BigIntegers.asUnsignedByteArray(M2);
               }

               this.state = SRP6KeyExchangerServer.State.FAILED;
            } catch (IllegalStateException e) {
               this.state = SRP6KeyExchangerServer.State.FAILED;
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
      CLIENT_CREDENTIALS,
      VERIFY_M1,
      COMPLETE,
      FAILED;
   }
}
