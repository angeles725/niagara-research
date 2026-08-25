package com.tridium.crypto.core.exchange;

public abstract class SRP6KeyExchanger implements IKeyExchanger {
   protected final SRP6AlgorithmBundle algorithmBundle;

   protected SRP6KeyExchanger(SRP6AlgorithmBundle bundle) {
      this.algorithmBundle = bundle;
   }
}
