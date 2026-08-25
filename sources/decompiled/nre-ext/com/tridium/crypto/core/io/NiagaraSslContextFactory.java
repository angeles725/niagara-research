package com.tridium.crypto.core.io;

import java.security.cert.CRL;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.eclipse.jetty.util.ssl.SslContextFactory.Server;

public class NiagaraSslContextFactory extends Server {
   private final Set<CRL> crls = new HashSet<>();

   protected Collection<? extends CRL> loadCRL(String crlPath) throws Exception {
      return this.crls;
   }

   public void addCRLs(Set<? extends CRL> crls) {
      this.crls.addAll(crls);
   }
}
