package com.tridium.nre.security;

import com.tridium.crypto.core.bundle.CryptographicAlgorithmBundle;

public abstract class EncryptionAlgorithmBundle extends CryptographicAlgorithmBundle {
   public abstract String getEncryptionAlgorithmName();

   public abstract int getKeySize();
}
