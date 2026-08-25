package com.tridium.nre.security;

import com.tridium.crypto.core.bundle.CryptographicAlgorithmBundle;
import com.tridium.nre.auth.Pbkdf2;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.EnumSet;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.nre.util.ByteArrayUtil;
import javax.baja.nre.util.TextUtil;

public class PBEValidator implements Predicate<SecretChars> {
   protected String validationSaltHex = null;
   protected String validationHashHex = null;
   protected int validationIterationCount = -1;
   protected final CryptographicAlgorithmBundle algorithmBundle;
   private static final Logger LOGGER = Logger.getLogger("crypto");
   public static final int DEFAULT_VALIDATION_ITERATION_COUNT = 10000;
   public static final PBEValidator NULL = new PBEValidator(NullAlgorithmBundle.getInstance()) {
      @Override
      public String getEncodedValidator() {
         return this.algorithmBundle.encode(null);
      }

      @Override
      public boolean test(SecretChars passPhrase) {
         return false;
      }

      @Override
      public boolean isNull() {
         return true;
      }
   };

   protected PBEValidator(CryptographicAlgorithmBundle algorithmBundle) {
      Objects.requireNonNull(algorithmBundle);
      this.algorithmBundle = algorithmBundle;
   }

   public PBEValidator(String encodedValidator) throws IOException {
      this(encodedValidator, CryptographicAlgorithmBundle.getInstance(CryptographicAlgorithmBundle.extractName(encodedValidator)));
   }

   public PBEValidator(String encodedValidator, CryptographicAlgorithmBundle algorithmBundle) throws IOException {
      this(algorithmBundle);
      Objects.requireNonNull(encodedValidator);
      String validatorEncodingName = CryptographicAlgorithmBundle.extractName(encodedValidator);
      if (!algorithmBundle.getAlgorithmName().equals(validatorEncodingName)) {
         throw new IOException("Unsupported validator encoding type");
      }

      try {
         String[] validatorPbkData = algorithmBundle.decode(encodedValidator);
         this.validationSaltHex = validatorPbkData[0];
         this.validationIterationCount = Integer.parseInt(validatorPbkData[1]);
         this.validationHashHex = validatorPbkData[2];
      } catch (IllegalArgumentException iae) {
         throw new IOException("Invaild encoding");
      }
   }

   public String getEncodedValidator() {
      String[] data = new String[this.algorithmBundle.getDataElementCount()];
      data[0] = this.validationSaltHex;
      data[1] = String.valueOf(this.validationIterationCount);
      data[2] = this.validationHashHex;
      return this.algorithmBundle.encode(data);
   }

   public CryptographicAlgorithmBundle getAlgorithmBundle() {
      return this.algorithmBundle;
   }

   public boolean isNull() {
      return false;
   }

   public static PBEValidator readValidator(DataInput in) throws IOException {
      return readValidator(in, CryptographicAlgorithmBundle.getInstance("pbkdf2-sha256.1"));
   }

   public static PBEValidator readValidator(DataInput in, CryptographicAlgorithmBundle algorithmBundle) throws IOException {
      Objects.requireNonNull(in);
      return new PBEValidator(in.readUTF(), algorithmBundle);
   }

   public void writeValidator(DataOutput out) throws IOException {
      Objects.requireNonNull(out);
      out.writeUTF(this.getEncodedValidator());
   }

   public boolean test(SecretChars passPhrase) {
      try {
         byte[] validationSalt = ByteArrayUtil.hexStringToBytes(this.validationSaltHex);
         byte[] validationHash = Pbkdf2.deriveKey(
            validationSalt, this.validationIterationCount, passPhrase.get(), (KeyDerivationAlgorithmBundle)this.algorithmBundle
         );
         return this.validationHashHex.equals(TextUtil.bytesToHexString(validationHash));
      } catch (AssertionError e) {
         if (CryptoProvider.CryptoError.FIPS_PASSWORD_LENGTH == SecurityInitializer.getInstance().getCryptoProvider().parseThrowable(e)) {
            FipsInformation fipsInfo = SecurityInitializer.getInstance().getFipsInformation();
            LOGGER.warning(
               String.format(
                  "Password does not meet FIPS 140-%d (version %d) requirements for PBKDF2 derivation.",
                  fipsInfo.getFipsVersion(),
                  fipsInfo.getNiagaraVersion()
               )
            );
            LOGGER.log(Level.FINE, "Call that failed: ", e);
            return false;
         } else {
            throw e;
         }
      } catch (Exception e) {
         return false;
      }
   }

   public static EnumSet<PBEValidator.ValidationFault> checkPassPhraseValidity(SecretChars passPhrase) {
      EnumSet<PBEValidator.ValidationFault> errorKeys = EnumSet.noneOf(PBEValidator.ValidationFault.class);
      if (SecurityInitializer.getInstance().isFips()) {
         int minLength = PasswordStrength.DEFAULT.getMinimumLength();
         if (passPhrase.size() < minLength) {
            errorKeys.add(PBEValidator.ValidationFault.FIPS_MIN_LENGTH);
         }
      }

      return errorKeys;
   }

   public enum ValidationFault {
      FIPS_MIN_LENGTH;
   }
}
