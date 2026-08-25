package com.tridium.nre.security;

import java.io.IOException;

public class PBEEncryptFunction implements AESEncryptFunction {
   private final PBEEncodingKey encodingKey;
   private final String aesTransformation;

   public PBEEncryptFunction(PBEEncodingKey encodingKey, String aesTransformation) throws IOException {
      this.encodingKey = (PBEEncodingKey)encodingKey.newCopy();
      this.aesTransformation = aesTransformation;
   }

   public PBEEncryptFunction(SecretChars passPhrase, String aesTransformation) throws IOException {
      this.encodingKey = new PBEEncodingKey(passPhrase);
      this.aesTransformation = aesTransformation;
   }

   @Override
   public void close() {
      this.encodingKey.close();
   }

   @Override
   public byte[] encrypt(SecretBytes unencryptedData, byte[] iv) throws Exception {
      return Aes256PasswordManager.encrypt(unencryptedData.get(), iv, this.encodingKey.get().get(), this.aesTransformation);
   }

   public PBEEncodingKey getEncodingKey() {
      return this.encodingKey;
   }
}
