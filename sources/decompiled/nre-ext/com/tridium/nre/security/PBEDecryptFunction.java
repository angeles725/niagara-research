package com.tridium.nre.security;

import java.io.IOException;

public class PBEDecryptFunction implements AESDecryptFunction {
   private final PBEEncodingKey encodingKey;

   public PBEDecryptFunction(PBEEncodingKey encodingKey) throws IOException {
      this.encodingKey = (PBEEncodingKey)encodingKey.newCopy();
   }

   public PBEDecryptFunction(SecretChars passPhrase) throws IOException {
      this.encodingKey = new PBEEncodingKey(passPhrase);
   }

   @Override
   public SecretBytes decrypt(byte[] cipher, byte[] iv, String aesTransformation) throws Exception {
      return Aes256PasswordManager.decryptSecret(this.encodingKey.get().get(), cipher, iv, aesTransformation);
   }

   @Override
   public void close() {
      this.encodingKey.close();
   }

   PBEEncodingKey getEncodingKey() {
      return this.encodingKey;
   }
}
