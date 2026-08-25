package com.tridium.nre.security;

import com.tridium.nre.security.io.AESDecryptingInputStream;
import java.io.DataInput;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

public final class PBEDecryptingInputStream extends AESDecryptingInputStream {
   private PBEEncodingInfo fileEncodingInfo;

   public PBEDecryptingInputStream(InputStream encryptedContentsIn, SecretChars passPhrase) throws IOException {
      super(encryptedContentsIn, null);
      Objects.requireNonNull(passPhrase);

      try (PBEEncodingKey key = this.fileEncodingInfo.makePBEKey(passPhrase)) {
         this.decryptFunction = new PBEDecryptFunction(key);
      }
   }

   public PBEDecryptingInputStream(InputStream encryptedContentsIn, PBEEncodingKey encodingKey) throws IOException {
      super(encryptedContentsIn, new PBEDecryptFunction(encodingKey));
      if (this.fileEncodingInfo.getEncodingIterationCount() != encodingKey.getEncodingIterationCount()
         || !this.fileEncodingInfo.getEncodedValidator().equals(encodingKey.getEncodedValidator())
         || !this.fileEncodingInfo.getEncodingSaltHex().equals(encodingKey.getEncodingSaltHex())) {
         throw new SecurityException();
      }
   }

   @Override
   protected void init(DataInput in) throws IOException {
      this.fileEncodingInfo = PBEEncodingInfo.readEncodingInfo(in);
   }
}
