package com.tridium.nre.security;

import com.tridium.crypto.core.bundle.CryptographicAlgorithmBundle;

public abstract class KeyDerivationAlgorithmBundle extends CryptographicAlgorithmBundle {
   public abstract String getKeyDerivationAlgorithmName();

   public abstract int getKeyLength();
}
