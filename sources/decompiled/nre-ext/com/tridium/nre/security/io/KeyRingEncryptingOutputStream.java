package com.tridium.nre.security.io;

import com.tridium.nre.security.Aes256PasswordManager;
import com.tridium.nre.security.AesAlgorithmBundle;
import com.tridium.nre.security.ISecurityInfoProvider;
import java.io.IOException;
import java.io.OutputStream;

public class KeyRingEncryptingOutputStream extends AESEncryptingOutputStream {
   public KeyRingEncryptingOutputStream(OutputStream outputStream, ISecurityInfoProvider provider) throws IOException {
      this(outputStream, provider, AesAlgorithmBundle.getInstance());
   }

   public KeyRingEncryptingOutputStream(OutputStream outputStream, ISecurityInfoProvider provider, AesAlgorithmBundle algorithmBundle) throws IOException {
      super(
         outputStream,
         (unencryptedData, iv) -> Aes256PasswordManager.getManager(provider.getKeyRing())
            .encrypt(unencryptedData.get(), iv, algorithmBundle.getAesTransformation()),
         algorithmBundle
      );
   }
}
