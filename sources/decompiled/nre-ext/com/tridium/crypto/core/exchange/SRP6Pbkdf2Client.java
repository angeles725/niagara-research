package com.tridium.crypto.core.exchange;

import java.math.BigInteger;
import org.bouncycastle.tls.crypto.impl.jcajce.srp.SRP6Client;
import org.bouncycastle.util.BigIntegers;

public class SRP6Pbkdf2Client extends SRP6Client {
   public BigInteger generateClientCredentials(byte[] saltedPassword) {
      this.x = BigIntegers.fromUnsignedByteArray(saltedPassword);
      this.a = this.selectPrivateValue();
      this.A = this.g.modPow(this.a, this.N);
      return this.A;
   }

   public BigInteger calculateSecret(BigInteger B) {
      super.calculateSecret(B);
      this.x = null;
      return this.S;
   }
}
