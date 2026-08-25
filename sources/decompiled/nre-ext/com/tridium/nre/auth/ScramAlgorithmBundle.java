package com.tridium.nre.auth;

import com.tridium.nre.security.KeyDerivationAlgorithmBundle;

public abstract class ScramAlgorithmBundle extends KeyDerivationAlgorithmBundle {
   public static final int SALT_INDEX = 0;
   public static final int ITERATION_COUNT_INDEX = 1;
   public static final int VALIDATION_HASH_INDEX = 2;

   public abstract String getMacAlgorithmName();

   public abstract String getSecretKeySpecAlgorithmName();

   public abstract String getMessageDigestAlgorithmName();

   public abstract PasswordHashAlgorithm getKeyDerivationAlgorithmType();

   @Override
   public int getDataElementCount() {
      return 3;
   }
}
