package com.tridium.crypto.core.io;

import java.security.Principal;
import java.util.Collection;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLSession;
import org.eclipse.jetty.util.ssl.X509;
import org.eclipse.jetty.util.ssl.SniX509ExtendedKeyManager.SniSelector;
import org.eclipse.jetty.util.ssl.SslContextFactory.Server;

public class JettySniSelector implements SniSelector {
   private final Server sslContextFactory;
   private final String defaultKeyType;
   private final String defaultAlias;

   public JettySniSelector(Server sslContextFactory, String defaultKeyType, String defaultAlias) {
      this.sslContextFactory = sslContextFactory;
      this.defaultKeyType = defaultKeyType;
      this.defaultAlias = defaultAlias;
   }

   public String sniSelect(String keyType, Principal[] issuers, SSLSession session, String sniHost, Collection<X509> certificates) throws SSLHandshakeException {
      String alias = this.sslContextFactory.sniSelect(keyType, issuers, session, sniHost, certificates);
      if (alias != null) {
         return alias;
      } else {
         return keyType.equals(this.defaultKeyType) ? this.defaultAlias : null;
      }
   }
}
