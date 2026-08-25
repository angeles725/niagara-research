package com.tridium.nre.security;

import java.io.IOException;

public abstract class RecoverableKeyData {
   protected boolean supportsKeyRecovery() {
      return true;
   }

   protected abstract boolean recoveryKeyExists();

   protected abstract void createRecoveryKey() throws IOException;

   protected abstract RecoverableKeyData getRecoveryKey(KeyParameters var1);

   protected abstract void rollBackRecoveryKey() throws IOException;

   protected abstract void deleteRecoveryKey() throws IOException;
}
