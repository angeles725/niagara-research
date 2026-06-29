package com.tridium.nre.auth;

import com.tridium.nre.security.KeyDerivationAlgorithmBundle;

public abstract class Pbkdf2AlgorithmBundle extends KeyDerivationAlgorithmBundle {
   public static final int SALT_INDEX = 0;
   public static final int ITERATION_COUNT_INDEX = 1;
   public static final int VALIDATION_HASH_INDEX = 2;

   public int getDataElementCount() {
      return 3;
   }
}
