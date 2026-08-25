package com.tridium.nre.security.io;

import com.tridium.nre.security.AesAlgorithmBundle;
import com.tridium.nre.security.PBEEncodingKey;
import com.tridium.nre.security.PBEEncryptFunction;
import com.tridium.nre.security.SecretChars;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;

public final class PBEEncryptingInputStream extends AESEncryptingInputStream {
   public PBEEncryptingInputStream(InputStream unencryptedContentsIn, SecretChars passPhrase) throws IOException {
      this(unencryptedContentsIn, passPhrase, AesAlgorithmBundle.getInstance());
   }

   public PBEEncryptingInputStream(InputStream unencryptedContentsIn, SecretChars passPhrase, AesAlgorithmBundle algorithmBundle) throws IOException {
      super(unencryptedContentsIn, new PBEEncryptFunction(passPhrase, algorithmBundle.getAesTransformation()), algorithmBundle);
      this.init();
   }

   public PBEEncryptingInputStream(InputStream unencryptedContentsIn, PBEEncodingKey encodingKey) throws IOException {
      this(unencryptedContentsIn, encodingKey, AesAlgorithmBundle.getInstance());
   }

   public PBEEncryptingInputStream(InputStream unencryptedContentsIn, PBEEncodingKey encodingKey, AesAlgorithmBundle algorithmBundle) throws IOException {
      super(unencryptedContentsIn, new PBEEncryptFunction(encodingKey, algorithmBundle.getAesTransformation()), algorithmBundle);
      this.init();
   }

   private void init() throws IOException {
      ByteArrayOutputStream encodingInfoBytes = new ByteArrayOutputStream();
      DataOutputStream encodingInfoData = new DataOutputStream(encodingInfoBytes);
      this.initHeader(encodingInfoData);
      ((PBEEncryptFunction)this.encryptFunction).getEncodingKey().writeEncodingInfo(encodingInfoData);
      encodingInfoData.flush();
      this.encryptedBuffer = encodingInfoBytes.toByteArray();
   }
}
