package com.tridium.nre.security;

import com.tridium.crypto.core.cert.NX509CertificateEntry;
import com.tridium.crypto.core.io.CoreCryptoManager;
import com.tridium.crypto.core.io.ICoreKeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.nre.security.IClientCertSelector;
import javax.baja.nre.security.IX509CertificateEntry;

public class DefaultClientCertSelector implements IClientCertSelector {
   private static final Logger LOG = Logger.getLogger("clientcert");

   @Override
   public IX509CertificateEntry selectClientCertificate(String alias, SecretChars passphrase) {
      NX509CertificateEntry entry = null;
      CoreCryptoManager cryptoManager = CoreCryptoManager.get(SecurityInitializer.getInstance().getSecurityInfoProvider());
      ICoreKeyStore ks = cryptoManager.getKeyStore();

      try {
         PrivateKey pk = (PrivateKey)ks.getKey(alias, passphrase.get());
         X509Certificate[] chain = ks.getCertificateChain(alias);
         if (chain == null || chain.length == 0 || pk == null) {
            if (LOG.isLoggable(Level.FINE)) {
               LOG.fine(String.format("keypair not found for %s", alias));
            }

            return null;
         }

         entry = NX509CertificateEntry.make(alias, chain, pk);
      } catch (Exception e) {
         if (LOG.isLoggable(Level.FINEST)) {
            LOG.log(Level.FINEST, e.getLocalizedMessage(), e);
         }

         entry = null;
      }

      return entry;
   }
}
