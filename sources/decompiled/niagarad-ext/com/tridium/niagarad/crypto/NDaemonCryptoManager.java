package com.tridium.niagarad.crypto;

import com.tridium.crypto.core.io.CoreCryptoManager;
import com.tridium.crypto.core.io.CryptoCoreServerSocketFactory;
import com.tridium.niagarad.NiagaraDaemon;
import java.security.KeyStore;
import javax.baja.nre.security.ServerTlsParameters;
import javax.net.ServerSocketFactory;

public class NDaemonCryptoManager extends DaemonCryptoManager {
   public NDaemonCryptoManager() throws IllegalArgumentException {
      if (NiagaraDaemon.NIAGARA_USER_HOME == null) {
         throw new IllegalArgumentException("niagara.user.home not defined");
      }
   }

   @Override
   public KeyStore getKeyStore() throws Exception {
      if (NiagaraDaemon.NIAGARA_USER_HOME == null) {
         throw new IllegalArgumentException("niagara.user.home not defined");
      }

      CoreCryptoManager coreCryptoManager = CoreCryptoManager.get(NiagaraDaemon.getSecurityInfoProvider());
      return coreCryptoManager.getKeyStore().getKeyStore();
   }

   @Override
   public ServerSocketFactory getServerSocketFactory(String type, ServerTlsParameters tlsParams) throws Exception {
      if (NiagaraDaemon.NIAGARA_USER_HOME == null) {
         throw new IllegalArgumentException("niagara.user.home not defined");
      } else {
         return new CryptoCoreServerSocketFactory(NiagaraDaemon.getSecurityInfoProvider(), tlsParams);
      }
   }
}
