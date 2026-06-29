package com.tridium.bacnet.stack.link.sc.authentication;

import com.tridium.bacnet.stack.link.sc.BScLinkLayer;
import com.tridium.bacnet.stack.link.sc.ScLinkLayerUtil;
import com.tridium.bacnet.stack.network.BNetworkPort;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CRLException;
import java.security.cert.CertPathBuilder;
import java.security.cert.CertPathBuilderException;
import java.security.cert.CertStore;
import java.security.cert.Certificate;
import java.security.cert.CollectionCertStoreParameters;
import java.security.cert.PKIXBuilderParameters;
import java.security.cert.PKIXCertPathBuilderResult;
import java.security.cert.TrustAnchor;
import java.security.cert.X509CRL;
import java.security.cert.X509CertSelector;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.naming.BOrd;
import javax.baja.naming.BOrdList;
import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.security.BAbstractAuthenticator;
import javax.baja.sys.BComponent;
import javax.baja.sys.BFacets;
import javax.baja.sys.BasicContext;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.user.BUser;

@NiagaraType
@NiagaraProperty(
   name = "scPorts",
   type = "BOrdList",
   defaultValue = "BOrdList.DEFAULT",
   facets = {@Facet(
      name = "BFacets.TARGET_TYPE",
      value = "\"bacnet:NetworkPort\""
   ), @Facet(
      name = "BFacets.MULTI_LINE",
      value = "true"
   ), @Facet(
      name = "BFacets.SECURITY",
      value = "true"
   )}
)
public final class BBacnetScAuthenticator extends BAbstractAuthenticator {
   public static final Property scPorts = newProperty(
      0,
      BOrdList.DEFAULT,
      BFacets.make(BFacets.make(BFacets.make("targetType", "bacnet:NetworkPort"), BFacets.make("multiLine", true)), BFacets.make("security", true))
   );
   public static final Type TYPE = Sys.loadType(BBacnetScAuthenticator.class);
   private static final BOrd[] EMPTY_ORD_ARRAY = new BOrd[0];
   private static final BasicContext UPDATE_CONTEXT = new BasicContext();
   static final Logger logger = Logger.getLogger("bacnet.sc.auth");
   private Set<BScLinkLayer> resolvedScLinkLayers;

   public BOrdList getScPorts() {
      return (BOrdList)this.get(scPorts);
   }

   public void setScPorts(BOrdList v) {
      this.set(scPorts, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   PKIXCertPathBuilderResult verify(List<? extends Certificate> sortedClientCertChain) {
      Set<TrustAnchor> trustAnchors = this.getTrustAnchors();
      if (trustAnchors.isEmpty()) {
         if (logger.isLoggable(Level.FINE)) {
            logger.fine("Could not verify cert chain. The SC link layers associated with " + this.getParentUserName() + " have no configured certificates.");
         }

         return null;
      } else {
         try {
            PKIXBuilderParameters params = new PKIXBuilderParameters(trustAnchors, new X509CertSelector());
            params.setRevocationEnabled(false);
            params.addCertStore(CertStore.getInstance("Collection", new CollectionCertStoreParameters(sortedClientCertChain)));
            CertPathBuilder builder = CertPathBuilder.getInstance("PKIX");
            PKIXCertPathBuilderResult result = (PKIXCertPathBuilderResult)builder.build(params);
            if (logger.isLoggable(Level.FINEST)) {
               X509Certificate issuerCertificate = result.getTrustAnchor().getTrustedCert();
               logger.finest(
                  "Certificate "
                     + ((X509Certificate)sortedClientCertChain.get(0)).getSubjectX500Principal()
                     + " verified against issuer certificate "
                     + issuerCertificate.getSubjectX500Principal()
               );
            }

            return result;
         } catch (NoSuchAlgorithmException | CertPathBuilderException | InvalidAlgorithmParameterException var7) {
            if (logger.isLoggable(Level.FINE)) {
               StringJoiner dns = new StringJoiner(",");

               for (Certificate certificate : sortedClientCertChain) {
                  dns.add("<" + ((X509Certificate)certificate).getSubjectX500Principal() + '>');
               }

               logger.fine("Certificates " + dns + " did not verify for user " + this.getParentUserName());
            }

            return null;
         }
      }
   }

   Set<TrustAnchor> getTrustAnchors() {
      Set<BScLinkLayer> scLinkLayers = this.getResolvedScLinkLayers();
      if (scLinkLayers.isEmpty()) {
         return Collections.emptySet();
      } else {
         Set<TrustAnchor> trustAnchors = new HashSet<>();

         for (BScLinkLayer scLinkLayer : scLinkLayers) {
            scLinkLayer.addTrustAnchors(trustAnchors);
         }

         return trustAnchors;
      }
   }

   void addCRLs(Set<X509CRL> crls) throws CRLException {
      for (BScLinkLayer scLinkLayer : this.getResolvedScLinkLayers()) {
         scLinkLayer.addCRLs(crls);
      }
   }

   public boolean isAssociatedWithLinkLayer(BScLinkLayer scLinkLayer) {
      return this.getResolvedScLinkLayers().contains(scLinkLayer);
   }

   void setTrustAnchorFault(String faultCause) {
      for (BScLinkLayer scLinkLayer : this.getResolvedScLinkLayers()) {
         scLinkLayer.setTrustAnchorFault(faultCause);
      }
   }

   private Set<BScLinkLayer> getResolvedScLinkLayers() {
      Set<BScLinkLayer> scLinkLayers = this.resolvedScLinkLayers;
      if (scLinkLayers == null) {
         scLinkLayers = this.resolveScLinkLayers();
      }

      return scLinkLayers;
   }

   public boolean isParentLegal(BComponent parent) {
      return parent instanceof BUser;
   }

   public void started() throws Exception {
      super.started();
      if (Sys.isStationStarted()) {
         this.resolveScLinkLayers();
         if (!this.resolvedScLinkLayers.isEmpty()) {
            BBacnetScAuthenticationScheme.trustAnchorsUpdated();
         }
      }
   }

   public void stopped() throws Exception {
      boolean isEmpty = this.resolvedScLinkLayers.isEmpty();
      updateConnectionManagersStatus(this.resolvedScLinkLayers);
      this.resolvedScLinkLayers.clear();
      if (!isEmpty) {
         BBacnetScAuthenticationScheme.trustAnchorsUpdated();
      }

      super.stopped();
   }

   private static void updateConnectionManagersStatus(Set<BScLinkLayer> scLinkLayers) {
      if (scLinkLayers != null) {
         for (BScLinkLayer scLinkLayer : scLinkLayers) {
            scLinkLayer.updateConnectionManagersStatus();
         }
      }
   }

   public void changed(Property property, Context context) {
      super.changed(property, context);
      if (this.isRunning() && property.equals(scPorts) && context != UPDATE_CONTEXT) {
         this.resolveScLinkLayers();
         BBacnetScAuthenticationScheme.trustAnchorsUpdated();
      }
   }

   public void removeLinkLayer(BScLinkLayer linkLayer) {
      if (linkLayer != null) {
         List<BOrd> newScPorts = new ArrayList<>(this.getScPorts().size());

         for (BOrd scPortOrd : this.getScPorts()) {
            try {
               BNetworkPort port = (BNetworkPort)scPortOrd.get(this);
               BScLinkLayer portLinkLayer = (BScLinkLayer)port.getLink();
               if (!portLinkLayer.equals(linkLayer)) {
                  newScPorts.add(scPortOrd);
               }
            } catch (Exception var7) {
               newScPorts.add(scPortOrd);
            }
         }

         this.setScPorts(BOrdList.make(newScPorts.toArray(EMPTY_ORD_ARRAY)));
      }
   }

   private Set<BScLinkLayer> resolveScLinkLayers() {
      Set<BScLinkLayer> linkLayers = new HashSet<>();
      List<BOrd> newScPortOrds = new ArrayList<>();
      List<BBacnetScAuthenticator> scAuthenticators = BBacnetScAuthenticationScheme.findScAuthenticators();

      for (BOrd scPortOrd : this.getScPorts()) {
         if (this.isOrdValid(scPortOrd)) {
            try {
               BNetworkPort networkPort = (BNetworkPort)scPortOrd.get(this);
               BScLinkLayer linkLayer = (BScLinkLayer)networkPort.getLink();
               if (!this.isAssociatedWithOtherUser(scAuthenticators, scPortOrd, linkLayer)) {
                  if (linkLayers.add(linkLayer)) {
                     newScPortOrds.add(scPortOrd);
                  } else if (logger.isLoggable(Level.WARNING)) {
                     logger.warning(
                        "The SC network port ord "
                           + scPortOrd
                           + " on the authenticator for user "
                           + this.getParentUserName()
                           + " has been removed because it resolves to a network port already on the list."
                     );
                  }
               }
            } catch (Exception var8) {
               ScLinkLayerUtil.logException(logger, new StringBuilder("could not resolve Ord to ScLinkLayer: ").append(scPortOrd), var8);
            }
         }
      }

      Set<BScLinkLayer> oldLinkLayers = this.resolvedScLinkLayers;
      this.resolvedScLinkLayers = linkLayers;
      this.set(scPorts, BOrdList.make(newScPortOrds.toArray(EMPTY_ORD_ARRAY)), UPDATE_CONTEXT);
      updateConnectionManagersStatus(oldLinkLayers);
      return linkLayers;
   }

   private boolean isOrdValid(BOrd scPortOrd) {
      if (scPortOrd.isNull()) {
         return false;
      } else if (!ScLinkLayerUtil.areOrdSchemesValid(scPortOrd)) {
         if (logger.isLoggable(Level.WARNING)) {
            logger.warning(
               "The authenticator for user "
                  + this.getParentUserName()
                  + " has an SC network port ord \""
                  + scPortOrd
                  + "\" that contains a disallowed ord query scheme; allowed schemes: "
                  + String.join(", ", ScLinkLayerUtil.ALLOWED_ORD_QUERY_SCHEMES)
            );
         }

         return false;
      } else {
         return true;
      }
   }

   private boolean isAssociatedWithOtherUser(List<BBacnetScAuthenticator> scAuthenticators, BOrd scPortOrd, BScLinkLayer linkLayer) {
      for (BBacnetScAuthenticator scAuthenticator : scAuthenticators) {
         if (scAuthenticator != this && scAuthenticator.resolvedScLinkLayers != null && scAuthenticator.resolvedScLinkLayers.contains(linkLayer)) {
            if (logger.isLoggable(Level.WARNING)) {
               logger.warning(
                  "The SC network port ord "
                     + scPortOrd
                     + " on the authenticator for user "
                     + this.getParentUserName()
                     + " has been removed because the network port is already associated with another user: "
                     + scAuthenticator.getParentUserName()
               );
            }

            return true;
         }
      }

      return false;
   }

   private String getParentUserName() {
      return this.getParent() != null ? this.getParent().getName() : "null";
   }
}
