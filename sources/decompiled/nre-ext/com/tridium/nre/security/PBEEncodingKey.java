package com.tridium.nre.security;

import com.tridium.crypto.core.bundle.CryptographicAlgorithmBundle;
import com.tridium.nre.auth.Pbkdf2;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Objects;
import javax.baja.nre.util.ByteArrayUtil;
import javax.baja.nre.util.TextUtil;

public final class PBEEncodingKey extends PBEEncodingInfo implements ISecretBytesSupplier {
   private final SecretBytes encodingKey;
   private final SecretChars passPhrase;

   public PBEEncodingKey(SecretChars passPhrase) {
      this(passPhrase, CryptographicAlgorithmBundle.getInstance("pbkdf2-sha256.1"));
   }

   public PBEEncodingKey(SecretChars passPhrase, CryptographicAlgorithmBundle algorithmBundle) {
      super(algorithmBundle);
      Objects.requireNonNull(passPhrase);
      this.passPhrase = passPhrase.newCopy();
      byte[] validationSalt = new byte[16];
      new SecureRandom().nextBytes(validationSalt);
      this.validationSaltHex = ByteArrayUtil.toHexString(validationSalt);
      byte[] encodingSalt = new byte[16];
      new SecureRandom().nextBytes(encodingSalt);
      this.encodingSaltHex = ByteArrayUtil.toHexString(encodingSalt);
      this.validationIterationCount = 10000;
      this.encodingIterationCount = 4096;

      byte[] validationHash;
      try {
         validationHash = Pbkdf2.deriveKey(validationSalt, this.validationIterationCount, passPhrase.get(), (KeyDerivationAlgorithmBundle)algorithmBundle);
      } catch (Exception e) {
         throw new SecurityException();
      }

      this.validationHashHex = TextUtil.bytesToHexString(validationHash);

      try {
         this.encodingKey = new SecretBytes(
            Pbkdf2.deriveKey(encodingSalt, this.encodingIterationCount, passPhrase.get(), (KeyDerivationAlgorithmBundle)algorithmBundle), false
         );
      } catch (Exception e) {
         throw new SecurityException();
      }
   }

   protected PBEEncodingKey(SecretChars passPhrase, PBEEncodingInfo validator) throws IOException {
      super(validator.getEncodedValidator(), validator.getEncodingSaltHex(), validator.getEncodingIterationCount(), validator.getAlgorithmBundle());
      Objects.requireNonNull(passPhrase);
      this.passPhrase = passPhrase.newCopy();
      if (validator instanceof PBEEncodingKey && ((PBEEncodingKey)validator).encodingKey != null) {
         this.encodingKey = ((PBEEncodingKey)validator).encodingKey.newCopy();
      } else {
         byte[] encodingSalt = ByteArrayUtil.hexStringToBytes(this.encodingSaltHex);

         try {
            this.encodingKey = new SecretBytes(
               Pbkdf2.deriveKey(encodingSalt, this.encodingIterationCount, passPhrase.get(), (KeyDerivationAlgorithmBundle)validator.getAlgorithmBundle()),
               false
            );
         } catch (Exception e) {
            throw new SecurityException();
         }
      }
   }

   public static PBEEncodingKey random() throws IOException {
      try (
         SecretBytes bytes = SecretBytes.random(64);
         SecretChars secretChars = SecretChars.fromString(Base64.getEncoder().encodeToString(bytes.get()));
      ) {
         return new PBEEncodingKey(secretChars);
      }
   }

   public SecretBytes get() {
      this.checkClosed();
      return this.encodingKey;
   }

   @Override
   public ISecretBytesSupplier newCopy() {
      this.checkClosed();

      try {
         return new PBEEncodingKey(this.passPhrase, this);
      } catch (IOException e) {
         throw new RuntimeException(e);
      }
   }

   public SecretChars getPassPhrase() {
      return this.passPhrase;
   }

   public boolean isClosed() {
      return this.passPhrase.isClosed();
   }

   @Override
   public void close() {
      if (this.encodingKey != null) {
         this.encodingKey.close();
      }

      this.passPhrase.close();
   }

   private void checkClosed() {
      if (this.isClosed()) {
         throw new IllegalStateException("Cannot perform operation on a PBEEncodingKey that has been closed");
      }
   }
}
