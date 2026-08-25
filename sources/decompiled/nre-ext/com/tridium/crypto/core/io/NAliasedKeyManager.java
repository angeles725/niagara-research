package com.tridium.crypto.core.io;

import java.net.Socket;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.X509KeyManager;

public class NAliasedKeyManager implements X509KeyManager {
   private ICoreKeyStore keyStore;
   private String keyAlias;
   protected static Logger logger = Logger.getLogger("crypto");

   public NAliasedKeyManager(ICoreKeyStore keyStore, String keyAlias) {
      this.keyStore = keyStore;
      this.keyAlias = keyAlias;
   }

   @Override
   public String chooseClientAlias(String[] keyType, Principal[] principal, Socket socket) {
      return this.keyAlias;
   }

   @Override
   public String chooseServerAlias(String keyType, Principal[] principal, Socket socket) {
      return this.keyAlias;
   }

   @Override
   public X509Certificate[] getCertificateChain(String alias) {
      try {
         Certificate[] chain = this.keyStore.getCertificateChain(alias);
         if (chain != null && chain.length != 0) {
            X509Certificate[] x509chain = new X509Certificate[chain.length];

            for (int i = 0; i < chain.length; i++) {
               x509chain[i] = (X509Certificate)chain[i];
            }

            return x509chain;
         } else {
            return null;
         }
      } catch (Exception e) {
         return null;
      }
   }

   @Override
   public String[] getClientAliases(String keyType, Principal[] issuers) {
      return new String[]{this.keyAlias};
   }

   @Override
   public PrivateKey getPrivateKey(String alias) {
      try {
         return (PrivateKey)this.keyStore.getKey(this.keyAlias, null);
      } catch (Exception e) {
         logger.log(Level.SEVERE, "error reading client aliases", e);
         return null;
      }
   }

   @Override
   public String[] getServerAliases(String keyType, Principal[] issuers) {
      return new String[]{this.keyAlias};
   }
}
