package com.tridium.crypto.core.io;

import com.tridium.nre.security.ISecurityInfoProvider;
import java.security.cert.CertStore;
import java.security.cert.CertStoreParameters;
import java.security.cert.CollectionCertStoreParameters;
import java.security.cert.PKIXBuilderParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509CRL;
import java.security.cert.X509CertSelector;
import java.util.Set;
import javax.net.ssl.CertPathTrustManagerParameters;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

public class TrustManagerBuilder {
   private final ISecurityInfoProvider securityInfoProvider;

   public TrustManagerBuilder(ISecurityInfoProvider securityInfoProvider) {
      if (securityInfoProvider == null) {
         throw new IllegalArgumentException("securityInfoProvider cannot be null.");
      }

      this.securityInfoProvider = securityInfoProvider;
   }

   public static TrustManager[] getTrustManagers(Set<TrustAnchor> trustAnchors) {
      return getTrustManagers(trustAnchors, null);
   }

   public static TrustManager[] getTrustManagers(Set<TrustAnchor> trustAnchors, Set<X509CRL> crls) {
      try {
         TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("PKIX");
         PKIXBuilderParameters pkixParameters = getPKIXBuildParameters(trustAnchors, crls);
         CertPathTrustManagerParameters ctmp = new CertPathTrustManagerParameters(pkixParameters);
         trustManagerFactory.init(ctmp);
         return trustManagerFactory.getTrustManagers();
      } catch (Exception e) {
         throw new SecurityException("Could not build TrustManagers.", e);
      }
   }

   private static PKIXBuilderParameters getPKIXBuildParameters(Set<TrustAnchor> trustAnchors, Set<X509CRL> crls) throws Exception {
      return crls != null && !crls.isEmpty() ? getPKIXBuildParametersWithCRLs(trustAnchors, crls) : getPKIXBuildParametersWithoutCRLs(trustAnchors);
   }

   private static PKIXBuilderParameters getPKIXBuildParametersWithoutCRLs(Set<TrustAnchor> trustAnchors) throws Exception {
      PKIXBuilderParameters pkixParameters = new PKIXBuilderParameters(trustAnchors, null);
      pkixParameters.setRevocationEnabled(false);
      return pkixParameters;
   }

   private static PKIXBuilderParameters getPKIXBuildParametersWithCRLs(Set<TrustAnchor> trustAnchors, Set<X509CRL> crls) throws Exception {
      PKIXBuilderParameters pkixParameters = new PKIXBuilderParameters(trustAnchors, new X509CertSelector());
      CertStoreParameters certStoreParameters = new CollectionCertStoreParameters(crls);
      CertStore certStore = CertStore.getInstance("Collection", certStoreParameters);
      pkixParameters.addCertStore(certStore);
      pkixParameters.setRevocationEnabled(true);
      return pkixParameters;
   }
}
