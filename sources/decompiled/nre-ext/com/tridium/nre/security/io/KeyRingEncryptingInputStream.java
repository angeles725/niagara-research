package com.tridium.nre.security.io;

import com.tridium.nre.security.Aes256PasswordManager;
import com.tridium.nre.security.AesAlgorithmBundle;
import com.tridium.nre.security.ISecurityInfoProvider;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;

public final class KeyRingEncryptingInputStream extends AESEncryptingInputStream {
   public KeyRingEncryptingInputStream(InputStream unencryptedContentsIn, ISecurityInfoProvider provider) throws IOException {
      this(unencryptedContentsIn, provider, AesAlgorithmBundle.getInstance());
   }

   public KeyRingEncryptingInputStream(InputStream unencryptedContentsIn, ISecurityInfoProvider provider, AesAlgorithmBundle algorithmBundle) throws IOException {
      super(
         unencryptedContentsIn,
         (unencryptedData, iv) -> Aes256PasswordManager.getManager(provider.getKeyRing())
            .encrypt(unencryptedData.get(), iv, algorithmBundle.getAesTransformation()),
         algorithmBundle
      );
      ByteArrayOutputStream encodingInfoBytes = new ByteArrayOutputStream();
      DataOutputStream encodingInfoData = new DataOutputStream(encodingInfoBytes);
      this.initHeader(encodingInfoData);
      encodingInfoData.flush();
      this.encryptedBuffer = encodingInfoBytes.toByteArray();
   }
}
