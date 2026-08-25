package com.tridium.nre.security.io;

import com.tridium.nre.security.Aes256PasswordManager;
import com.tridium.nre.security.ISecurityInfoProvider;
import java.io.IOException;
import java.io.InputStream;

public final class KeyRingDecryptingInputStream extends AESDecryptingInputStream {
   public KeyRingDecryptingInputStream(InputStream encryptedContentsIn, ISecurityInfoProvider provider) throws IOException {
      super(
         encryptedContentsIn,
         (cipher, iv, aesTransformation) -> Aes256PasswordManager.getManager(provider.getKeyRing()).decryptSecret(cipher, iv, aesTransformation)
      );
   }
}
