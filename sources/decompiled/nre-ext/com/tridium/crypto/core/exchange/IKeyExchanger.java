package com.tridium.crypto.core.exchange;

import com.tridium.nre.security.SecretBytes;

public interface IKeyExchanger {
   void init();

   byte[] getKey();

   byte[] doInitialStep(SecretBytes var1);

   byte[] doExchangeStep(byte[] var1);
}
