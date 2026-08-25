package com.tridium.nre.security;

import com.tridium.crypto.core.bundle.CryptographicAlgorithmBundle;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Objects;

public class PBEEncodingInfo extends PBEValidator {
   protected String encodingSaltHex = null;
   protected int encodingIterationCount = -1;
   public static final int DEFAULT_ENCODING_ITERATION_COUNT = 4096;
   public static final PBEEncodingInfo NULL = new PBEEncodingInfo(NullAlgorithmBundle.getInstance()) {
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

   protected PBEEncodingInfo(CryptographicAlgorithmBundle algorithmBundle) {
      super(algorithmBundle);
   }

   public PBEEncodingInfo(String encodedValidator, String encodingSaltHex, int encodingIterationCount) throws IOException {
      this(encodedValidator, encodingSaltHex, encodingIterationCount, CryptographicAlgorithmBundle.getInstance("pbkdf2-sha256.1"));
   }

   public PBEEncodingInfo(String encodedValidator, String encodingSaltHex, int encodingIterationCount, CryptographicAlgorithmBundle algorithmBundle) throws IOException {
      super(encodedValidator, algorithmBundle);
      Objects.requireNonNull(encodingSaltHex);
      this.encodingSaltHex = encodingSaltHex;
      this.encodingIterationCount = encodingIterationCount;
   }

   public PBEEncodingKey makePBEKey(SecretChars passPhrase) throws IOException {
      Objects.requireNonNull(passPhrase);
      if (this.test(passPhrase)) {
         return new PBEEncodingKey(passPhrase, this);
      } else {
         throw new SecurityException();
      }
   }

   public int getEncodingIterationCount() {
      return this.encodingIterationCount;
   }

   public String getEncodingSaltHex() {
      return this.encodingSaltHex;
   }

   @Override
   public boolean isNull() {
      return false;
   }

   public static PBEEncodingInfo readEncodingInfo(DataInput in) throws IOException {
      return readEncodingInfo(in, CryptographicAlgorithmBundle.getInstance("pbkdf2-sha256.1"));
   }

   public static PBEEncodingInfo readEncodingInfo(DataInput in, CryptographicAlgorithmBundle algorithmBundle) throws IOException {
      Objects.requireNonNull(in);
      String encodedValidator = in.readUTF();
      String encodingSaltHex = in.readUTF();
      int encodingIterationCount = in.readInt();
      return new PBEEncodingInfo(encodedValidator, encodingSaltHex, encodingIterationCount, algorithmBundle);
   }

   public void writeEncodingInfo(DataOutput out) throws IOException {
      Objects.requireNonNull(out);
      this.writeValidator(out);
      out.writeUTF(this.getEncodingSaltHex());
      out.writeInt(this.getEncodingIterationCount());
   }
}
