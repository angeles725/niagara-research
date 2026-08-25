package com.tridium.nre.security.io;

import com.tridium.nre.security.AesAlgorithmBundle;
import com.tridium.nre.security.PBEEncodingKey;
import com.tridium.nre.security.PBEEncryptFunction;
import com.tridium.nre.security.SecretChars;
import java.io.IOException;
import java.io.OutputStream;

public class PBEEncryptingOutputStream extends AESEncryptingOutputStream {
   public PBEEncryptingOutputStream(OutputStream outputStream, SecretChars passPhrase) throws IOException {
      this(outputStream, passPhrase, AesAlgorithmBundle.getInstance());
   }

   public PBEEncryptingOutputStream(OutputStream outputStream, SecretChars passPhrase, AesAlgorithmBundle algorithmBundle) throws IOException {
      super(outputStream, new PBEEncryptFunction(passPhrase, algorithmBundle.getAesTransformation()), algorithmBundle);
      this.init();
   }

   public PBEEncryptingOutputStream(OutputStream outputStream, PBEEncodingKey encodingKey) throws IOException {
      this(outputStream, encodingKey, AesAlgorithmBundle.getInstance());
   }

   public PBEEncryptingOutputStream(OutputStream outputStream, PBEEncodingKey encodingKey, AesAlgorithmBundle algorithmBundle) throws IOException {
      super(outputStream, new PBEEncryptFunction(encodingKey, algorithmBundle.getAesTransformation()), algorithmBundle);
      this.init();
   }

   private void init() throws IOException {
      ((PBEEncryptFunction)this.encryptFunction).getEncodingKey().writeEncodingInfo(this.dataOutput);
      this.dataOutput.flush();
   }
}
