package com.tridium.bacnet.stack.link.sc.authentication;

import com.tridium.authn.BAuthenticationSchemeFolder;
import com.tridium.authn.BAuthenticationService;
import com.tridium.authn.NiagaraLoginConfiguration;
import com.tridium.bacnet.stack.link.sc.ScLinkLayerUtil;
import com.tridium.crypto.core.io.CRLProvider;
import com.tridium.crypto.core.io.CoreCryptoManager;
import com.tridium.crypto.core.io.TrustAnchorProvider;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.cert.CRLException;
import java.security.cert.TrustAnchor;
import java.security.cert.X509CRL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.baja.authn.BAuthenticationScheme;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.security.BAbstractAuthenticator;
import javax.baja.sys.BComponent;
import javax.baja.sys.Context;
import javax.baja.sys.IllegalChildException;
import javax.baja.sys.Property;
import javax.baja.sys.ServiceNotFoundException;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.user.BUser;
import javax.baja.user.BUserService;
import javax.baja.util.BIRestrictedComponent;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.AppConfigurationEntry.LoginModuleControlFlag;

@NiagaraType
@NiagaraProperty(
   name = "generateAlarmOnCrlExpiration",
   type = "boolean",
   defaultValue = "true"
)
public final class BBacnetScAuthenticationScheme extends BAuthenticationScheme implements BIRestrictedComponent, TrustAnchorProvider, CRLProvider {
   public static final Property generateAlarmOnCrlExpiration = newProperty(0, true, null);
   public static final Type TYPE = Sys.loadType(BBacnetScAuthenticationScheme.class);
   private Configuration configuration;
   private String trustAnchorProviderId;
   private String crlProviderId;
   public static final String SCHEME_NAME = "bacnetScAuth";

   public boolean getGenerateAlarmOnCrlExpiration() {
      return this.getBoolean(generateAlarmOnCrlExpiration);
   }

   public void setGenerateAlarmOnCrlExpiration(boolean v) {
      this.setBoolean(generateAlarmOnCrlExpiration, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public String getSchemeName() {
      return "bacnetScAuth";
   }

   public Configuration getLoginConfiguration() {
      if (this.configuration == null) {
         this.configuration = new NiagaraLoginConfiguration(BacnetScLoginModule.class.getName(), LoginModuleControlFlag.REQUIRED, new HashMap());
      }

      return this.configuration;
   }

   public BAbstractAuthenticator getDefaultAuthenticator() {
      return new BBacnetScAuthenticator();
   }

   public void checkParentForRestrictedComponent(BComponent parent, Context cx) {
      BIRestrictedComponent.checkForDuplicates(parent, this, false);
      if (!(parent instanceof BAuthenticationSchemeFolder)) {
         throw new IllegalChildException("baja", "IllegalChildException.parentAndChild", new Object[]{this.getType(), parent.getType()});
      }
   }

   public Set<TrustAnchor> getTrustAnchors() {
      List<BBacnetScAuthenticator> scAuthenticators = findScAuthenticators();
      if (scAuthenticators.isEmpty()) {
         return Collections.emptySet();
      } else {
         Set<TrustAnchor> allTrustAnchors = new HashSet<>();

         for (BBacnetScAuthenticator scAuthenticator : scAuthenticators) {
            Set<TrustAnchor> userTrustAnchors = scAuthenticator.getTrustAnchors();
            if (containsDuplicates(allTrustAnchors, userTrustAnchors)) {
               setTrustAnchorFault(scAuthenticators, ScLinkLayerUtil.LEXICON.getText("abstractConnectionManager.duplicateIssuerCerts", null));
               return Collections.emptySet();
            }

            allTrustAnchors.addAll(userTrustAnchors);
         }

         setTrustAnchorFault(scAuthenticators, null);
         return allTrustAnchors;
      }
   }

   public static List<BBacnetScAuthenticator> findScAuthenticators() {
      BUser[] users;
      try {
         users = BUserService.getService().getUsers();
      } catch (ServiceNotFoundException var7) {
         return Collections.emptyList();
      }

      List<BBacnetScAuthenticator> scAuthenticators = new ArrayList<>();

      for (BUser user : users) {
         BAbstractAuthenticator authenticator = user.getAuthenticator();
         if (authenticator.isRunning() && authenticator instanceof BBacnetScAuthenticator) {
            scAuthenticators.add((BBacnetScAuthenticator)authenticator);
         }
      }

      return scAuthenticators;
   }

   private static boolean containsDuplicates(Set<TrustAnchor> allTrustAnchors, Set<TrustAnchor> userTrustAnchors) {
      for (TrustAnchor userTrustAnchor : userTrustAnchors) {
         for (TrustAnchor trustAnchor : allTrustAnchors) {
            if (trustAnchor.getTrustedCert().equals(userTrustAnchor.getTrustedCert())) {
               return true;
            }
         }
      }

      return false;
   }

   private static void setTrustAnchorFault(List<BBacnetScAuthenticator> scAuthenticators, String faultCause) {
      for (BBacnetScAuthenticator scAuthenticator : scAuthenticators) {
         scAuthenticator.setTrustAnchorFault(faultCause);
      }
   }

   public static void trustAnchorsUpdated() {
      BAuthenticationService authService = BAuthenticationService.getService();
      BBacnetScAuthenticationScheme[] bacnetSchemes = (BBacnetScAuthenticationScheme[])authService.getAuthenticationSchemes()
         .getChildren(BBacnetScAuthenticationScheme.class);
      if (bacnetSchemes.length > 0) {
         bacnetSchemes[0].getTrustAnchors();
         AccessController.doPrivileged((PrivilegedAction<Void>)(() -> {
            CoreCryptoManager.get().trustAnchorsUpdated(bacnetSchemes[0].trustAnchorProviderId);
            return null;
         }));
      }
   }

   public Set<X509CRL> getCRLs() throws CRLException {
      List<BBacnetScAuthenticator> scAuthenticators = findScAuthenticators();
      if (scAuthenticators.isEmpty()) {
         return Collections.emptySet();
      } else {
         Set<X509CRL> crls = new HashSet<>();

         for (BBacnetScAuthenticator scAuthenticator : scAuthenticators) {
            scAuthenticator.addCRLs(crls);
         }

         return crls;
      }
   }

   public void started() throws Exception {
      super.started();
      this.getTrustAnchors();
      AccessController.doPrivileged((PrivilegedAction<Void>)(() -> {
         CoreCryptoManager manager = CoreCryptoManager.get();
         this.trustAnchorProviderId = manager.registerTrustAnchorProvider(this);
         this.crlProviderId = manager.registerCRLProvider(this);
         return null;
      }));
   }

   public void stopped() throws Exception {
      AccessController.doPrivileged((PrivilegedAction<Void>)(() -> {
         CoreCryptoManager manager = CoreCryptoManager.get();
         manager.unregisterTrustAnchorProvider(this.trustAnchorProviderId);
         manager.unregisterCRLProvider(this.crlProviderId);
         return null;
      }));
      setTrustAnchorFault(findScAuthenticators(), ScLinkLayerUtil.LEXICON.getText("abstractConnectionManager.missingScScheme", null));
      super.stopped();
   }
}
