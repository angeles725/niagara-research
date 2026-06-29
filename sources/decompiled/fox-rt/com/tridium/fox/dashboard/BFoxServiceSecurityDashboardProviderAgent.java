package com.tridium.fox.dashboard;

import com.tridium.crypto.core.cert.CertUtils;
import com.tridium.crypto.core.io.ICoreKeyStore;
import com.tridium.fox.sys.BFoxService;
import com.tridium.security.BServerCertificateHealth;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.naming.BOrd;
import javax.baja.nre.annotations.AgentOn;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.security.crypto.BSslTlsEnum;
import javax.baja.security.crypto.BTlsCipherSuiteGroup;
import javax.baja.security.crypto.CertManagerFactory;
import javax.baja.security.dashboard.BISecurityDashboardProviderAgent;
import javax.baja.security.dashboard.BSecurityItemStatus;
import javax.baja.security.dashboard.LexiconFormatInfo;
import javax.baja.security.dashboard.SecurityDashboardItem;
import javax.baja.security.dashboard.SecurityDashboardItemBuilder;
import javax.baja.sys.BBoolean;
import javax.baja.sys.BIObject;
import javax.baja.sys.BObject;
import javax.baja.sys.BRelTime;
import javax.baja.sys.Context;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType(
   agent = {@AgentOn(
      types = {"fox:FoxService"}
   )}
)
public class BFoxServiceSecurityDashboardProviderAgent extends BObject implements BISecurityDashboardProviderAgent {
   public static final Type TYPE = Sys.loadType(BFoxServiceSecurityDashboardProviderAgent.class);
   private static final String[] TRACE_PROPERTIES = new String[]{"traceMulticast", "traceReadFrame", "traceSessionStates", "traceWriteFrame"};
   private BFoxService foxService;
   private SecurityDashboardItemBuilder builder;
   private boolean legacyEnabledByDefault;
   public static final Logger LOG = Logger.getLogger("fox");
   private static final int VERSION = 4;
   private static final double CERT_EXPIRY_WARNING_MILLIS = BRelTime.makeDays(90).getMillis();
   private static final int LEGACY_DEFAULT = 0;
   private static final int LEGACY_NO = 1;
   private static final int LEGACY_YES = 2;
   private static final String CERT_EXPIRED_SUMMARY = "securityDashboard.foxCertificateExpired.summary";
   private static final String CERT_EXPIRED_DESCRIPTION = "securityDashboard.foxCertificateExpired.description";
   private static final String CERT_MISSING_SUMMARY = "securityDashboard.foxCertificateNotAvailable.summary";
   private static final String CERT_MISSING_DESCRIPTION = "securityDashboard.foxCertificateNotAvailable.description";
   private static final String CERT_NEAR_EXPIRY_SUMMARY = "securityDashboard.foxCertificateNearExpiry.summary";
   private static final String CERT_NEAR_EXPIRY_DESCRIPTION = "securityDashboard.foxCertificateNearExpiry.description";
   private static final String CERT_NOT_SELF_SIGNED_SUMMARY = "securityDashboard.foxCertificateNotSelfSigned.summary";
   private static final String CERT_NOT_SELF_SIGNED_DESCRIPTION = "securityDashboard.foxCertificateNotSelfSigned.description";
   private static final String CERT_NOT_YET_VALID_SUMMARY = "securityDashboard.foxCertificateNotYetValidException.summary";
   private static final String CERT_NOT_YET_VALID_DESCRIPTION = "securityDashboard.foxCertificateNotYetValidException.description";
   private static final String CERT_DEFAULT_SUMMARY = "securityDashboard.foxCertificateDefault.summary";
   private static final String CERT_DEFAULT_DESCRIPTION = "securityDashboard.foxCertificateDefault.description";
   private static final String CERT_SELF_SIGNED_SUMMARY = "securityDashboard.foxCertificateSelfSigned.summary";
   private static final String CERT_SELF_SIGNED_DESCRIPTION = "securityDashboard.foxCertificateSelfSigned.description";
   private static final String CERT_IN_VALIDITY_PERIOD_SUMMARY = "securityDashboard.foxCertificateValid.summary";
   private static final String CERT_IN_VALIDITY_PERIOD_DESCRIPTION = "securityDashboard.foxCertificateValid.description";
   private static final String CIPHER_SUITE_SUMMARY = "securityDashboard.cipherSuiteGroup.summary";
   private static final String CIPHER_SUITE_DESCRIPTION = "securityDashboard.cipherSuiteGroup.description";
   private static final String FORWARDING_SUMMARY = "securityDashboard.foxForwarding.summary";
   private static final String FORWARDING_DESCRIPTION = "securityDashboard.foxForwarding.description";
   private static final String LEGACY_CLIENTS_DEFAULT_YES_SUMMARY = "securityDashboard.legacyClients.defaultYes.summary";
   private static final String LEGACY_CLIENTS_DEFAULT_NO_SUMMARY = "securityDashboard.legacyClients.defaultNo.summary";
   private static final String LEGACY_CLIENTS_DISABLED_SUMMARY = "securityDashboard.legacyClients.disabled.summary";
   private static final String LEGACY_CLIENTS_ENABLED_SUMMARY = "securityDashboard.legacyClients.enabled.summary";
   private static final String LEGACY_CLIENTS_DESCRIPTION = "securityDashboard.legacyClients.description";
   private static final String NON_TLS_SUMMARY = "securityDashboard.foxNonTlsProtocol.summary";
   private static final String NON_TLS_DESCRIPTION = "securityDashboard.foxNonTlsProtocol.description";
   private static final String SECTION_HEADER = "securityDashboard.sectionHeader";
   private static final String TLS_OFF_SUMMARY = "securityDashboard.foxTlsProtocolOff.summary";
   private static final String TLS_OFF_DESCRIPTION = "securityDashboard.foxTlsProtocolOff.description";
   private static final String TLS_ON_SUMMARY = "securityDashboard.foxTlsProtocolOn.summary";
   private static final String TLS_ON_DESCRIPTION = "securityDashboard.foxTlsProtocolOn.description";
   private static final String TLS_VERSION_SUMMARY = "securityDashboard.foxTlsProtocol.summary";
   private static final String TLS_VERSION_DESCRIPTION = "securityDashboard.foxTlsProtocol.description";
   private static final String TRACE_ENABLED_SUMMARY = "securityDashboard.foxTracePropertiesEnabled.summary";
   private static final String TRACE_ENABLED_DESCRIPTION = "securityDashboard.foxTracePropertiesEnabled.description";
   private static final String GLOBAL_ENCRYPTED_CERT_SUMMARY = "securityDashboard.foxCertificateGloballyEncrypted.summary";
   private static final String UNIQUE_ENCRYPTED_CERT_SUMMARY = "securityDashboard.foxCertificateUniquelyEncrypted.summary";
   private static final String ENCRYPTED_CERT_DESCRIPTION = "securityDashboard.foxCertificateEncrypted.description";

   public Type getType() {
      return TYPE;
   }

   public void setSecurityDashboardItemsSource(BIObject object) {
      this.foxService = (BFoxService)object;
      this.builder = new SecurityDashboardItemBuilder(this);
      this.legacyEnabledByDefault = true;
   }

   public LexiconFormatInfo getSecurityDashboardSectionHeader(Context cx) {
      return LexiconFormatInfo.make(TYPE, "securityDashboard.sectionHeader");
   }

   public BOrd getSecurityDashboardSectionHyperlinkOrd() {
      return this.foxService.getNavOrd().relativizeToSession();
   }

   public int getSecurityDashboardItemsVersion() {
      return 4;
   }

   public List<SecurityDashboardItem> getSecurityDashboardItems(Context cx) {
      List<SecurityDashboardItem> items = new ArrayList<>(this.getTlsItems());
      items.addAll(this.getCipherSuiteItems());
      items.addAll(this.getCertificateItems());
      items.addAll(this.getTraceItems());
      items.addAll(this.getLegacyConnectionItems());
      return items;
   }

   private List<SecurityDashboardItem> getTlsItems() {
      List<SecurityDashboardItem> items = new ArrayList<>();
      boolean foxEnabled = this.foxService.getFoxEnabled();
      boolean foxsEnabled = this.foxService.getFoxsEnabled();
      boolean foxsOnly = this.foxService.getFoxsOnly();
      BSslTlsEnum minProtocol = this.foxService.getFoxsMinProtocol();
      if (foxEnabled && !foxsEnabled) {
         items.add(this.builder.makeAlert("securityDashboard.foxTlsProtocolOff.summary", "securityDashboard.foxTlsProtocolOff.description"));
      }

      if (!foxEnabled && foxsEnabled) {
         items.add(this.builder.makeOk("securityDashboard.foxTlsProtocolOn.summary", "securityDashboard.foxTlsProtocolOn.description"));
      }

      if (foxEnabled && foxsEnabled) {
         if (foxsOnly) {
            items.add(this.builder.makeWarning("securityDashboard.foxForwarding.summary", "securityDashboard.foxForwarding.description"));
         } else {
            items.add(this.builder.makeAlert("securityDashboard.foxNonTlsProtocol.summary", "securityDashboard.foxNonTlsProtocol.description"));
         }
      }

      if (foxsEnabled) {
         BSecurityItemStatus status = isTlsVersionStrong(minProtocol) ? BSecurityItemStatus.securityStatusOK : BSecurityItemStatus.securityStatusWarning;
         items.add(
            this.builder
               .make(status)
               .withSummary("securityDashboard.foxTlsProtocol.summary", new Object[]{minProtocol})
               .withDescription("securityDashboard.foxTlsProtocol.description", new Object[]{BSslTlsEnum.tlsv1_2})
         );
      }

      return items;
   }

   private static boolean isTlsVersionStrong(BSslTlsEnum tlsVersion) {
      return tlsVersion.equals(BSslTlsEnum.tlsv1_2) || tlsVersion.equals(BSslTlsEnum.tlsv1_3);
   }

   private List<SecurityDashboardItem> getCipherSuiteItems() {
      if (!this.foxService.getFoxsEnabled()) {
         return Collections.emptyList();
      } else {
         BTlsCipherSuiteGroup cipherSuite = this.foxService.getCipherSuiteGroup();
         BSecurityItemStatus status = cipherSuite.equals(BTlsCipherSuiteGroup.recommended)
            ? BSecurityItemStatus.securityStatusOK
            : BSecurityItemStatus.securityStatusWarning;
         return Collections.singletonList(
            this.builder
               .make(status)
               .withSummary("securityDashboard.cipherSuiteGroup.summary", new Object[]{cipherSuite})
               .withDescription("securityDashboard.cipherSuiteGroup.description", new Object[0])
         );
      }
   }

   private List<SecurityDashboardItem> getCertificateItems() {
      if (!this.foxService.getFoxsEnabled()) {
         return Collections.emptyList();
      } else {
         List<SecurityDashboardItem> items = new ArrayList<>();
         String foxsCert = this.foxService.getCertAliasAndPassword().getAlias();
         BServerCertificateHealth health = this.foxService.getServerCertificateHealth();
         switch (health.getCertStatus().getOrdinal()) {
            case 0:
               items.add(
                  this.builder
                     .makeOk()
                     .withSummary("securityDashboard.certHealth.ok", new Object[]{health.getReturnedCert()})
                     .withDescription("securityDashboard.certHealth.description", new Object[0])
               );
               break;
            case 1:
               items.add(
                  this.builder
                     .makeAlert()
                     .withSummary("securityDashboard.certHealth.badPassword", new Object[]{health.getRequestedCert(), health.getReturnedCert()})
                     .withDescription("securityDashboard.certHealth.description", new Object[0])
               );
               break;
            case 2:
               items.add(
                  this.builder
                     .makeAlert()
                     .withSummary("securityDashboard.certHealth.badKey", new Object[]{health.getRequestedCert(), health.getReturnedCert()})
                     .withDescription("securityDashboard.certHealth.description", new Object[0])
               );
               break;
            case 3:
               items.add(
                  this.builder
                     .makeAlert()
                     .withSummary("securityDashboard.certHealth.missingKey", new Object[]{health.getRequestedCert(), health.getReturnedCert()})
                     .withDescription("securityDashboard.certHealth.description", new Object[0])
               );
               break;
            case 4:
               items.add(
                  this.builder
                     .makeAlert()
                     .withSummary("securityDashboard.certHealth.badDefault", new Object[]{health.getRequestedCert(), health.getReturnedCert()})
                     .withDescription("securityDashboard.certHealth.description", new Object[0])
               );
         }

         try {
            X509Certificate certificate = CertManagerFactory.getInstance().getKeyStore().getCertificate(foxsCert);

            try {
               certificate.checkValidity();
               if (isNearExpiration(certificate)) {
                  items.add(
                     this.builder
                        .makeWarning()
                        .withSummary("securityDashboard.foxCertificateNearExpiry.summary", new Object[]{foxsCert, certificate.getNotAfter()})
                        .withDescription("securityDashboard.foxCertificateNearExpiry.description", new Object[0])
                  );
               } else {
                  items.add(
                     this.builder
                        .makeOk()
                        .withSummary("securityDashboard.foxCertificateValid.summary", new Object[]{foxsCert})
                        .withDescription("securityDashboard.foxCertificateValid.description", new Object[0])
                  );
               }
            } catch (CertificateExpiredException var7) {
               items.add(
                  this.builder
                     .makeAlert()
                     .withSummary("securityDashboard.foxCertificateExpired.summary", new Object[]{foxsCert})
                     .withDescription("securityDashboard.foxCertificateExpired.description", new Object[0])
               );
            } catch (CertificateNotYetValidException var8) {
               items.add(
                  this.builder
                     .makeAlert()
                     .withSummary("securityDashboard.foxCertificateNotYetValidException.summary", new Object[]{foxsCert, certificate.getNotBefore()})
                     .withDescription("securityDashboard.foxCertificateNotYetValidException.description", new Object[0])
               );
            }

            if (isSelfSigned(certificate)) {
               if ("default".equalsIgnoreCase(foxsCert)) {
                  items.add(
                     this.builder
                        .makeWarning()
                        .withSummary("securityDashboard.foxCertificateDefault.summary", new Object[0])
                        .withDescription("securityDashboard.foxCertificateDefault.description", new Object[0])
                  );
               } else {
                  items.add(
                     this.builder
                        .makeWarning()
                        .withSummary("securityDashboard.foxCertificateSelfSigned.summary", new Object[]{foxsCert})
                        .withDescription("securityDashboard.foxCertificateSelfSigned.description", new Object[0])
                  );
               }
            } else {
               items.add(
                  this.builder
                     .makeOk()
                     .withSummary("securityDashboard.foxCertificateNotSelfSigned.summary", new Object[]{foxsCert})
                     .withDescription("securityDashboard.foxCertificateNotSelfSigned.description", new Object[0])
               );
            }

            try {
               ICoreKeyStore keyStore = (ICoreKeyStore)CertManagerFactory.getInstance().getKeyStore();
               if (CertUtils.isPrivateKeyGloballyEncrypted(foxsCert, keyStore)) {
                  items.add(
                     this.builder
                        .makeWarning()
                        .withSummary("securityDashboard.foxCertificateGloballyEncrypted.summary", new Object[]{foxsCert})
                        .withDescription("securityDashboard.foxCertificateEncrypted.description", new Object[0])
                  );
               } else {
                  items.add(
                     this.builder
                        .makeOk()
                        .withSummary("securityDashboard.foxCertificateUniquelyEncrypted.summary", new Object[]{foxsCert})
                        .withDescription("securityDashboard.foxCertificateEncrypted.description", new Object[0])
                  );
               }
            } catch (Exception var6) {
               LOG.log(Level.WARNING, "unable to check private key for password", (Throwable)var6);
            }
         } catch (Exception var9) {
         }

         return items;
      }
   }

   private List<SecurityDashboardItem> getTraceItems() {
      List<String> tracePropertiesList = new ArrayList<>();

      for (String prop : TRACE_PROPERTIES) {
         if (BBoolean.TRUE.equals(this.foxService.get(prop))) {
            tracePropertiesList.add(prop);
         }
      }

      return tracePropertiesList.isEmpty()
         ? Collections.emptyList()
         : Collections.singletonList(
            this.builder
               .makeWarning()
               .withSummary("securityDashboard.foxTracePropertiesEnabled.summary", new Object[]{String.join(", ", tracePropertiesList)})
               .withDescription("securityDashboard.foxTracePropertiesEnabled.description", new Object[0])
         );
   }

   private List<SecurityDashboardItem> getLegacyConnectionItems() {
      String summary = null;
      BSecurityItemStatus status = null;
      switch (this.foxService.getSupportLegacyClients().getOrdinal()) {
         case 0:
            if (this.legacyEnabledByDefault) {
               summary = "securityDashboard.legacyClients.defaultYes.summary";
               status = BSecurityItemStatus.securityStatusWarning;
            } else {
               summary = "securityDashboard.legacyClients.defaultNo.summary";
               status = BSecurityItemStatus.securityStatusOK;
            }
            break;
         case 1:
            summary = "securityDashboard.legacyClients.disabled.summary";
            status = BSecurityItemStatus.securityStatusOK;
            break;
         case 2:
            summary = "securityDashboard.legacyClients.enabled.summary";
            status = BSecurityItemStatus.securityStatusAlert;
      }

      return Collections.singletonList(this.builder.make(status, summary, "securityDashboard.legacyClients.description"));
   }

   private static boolean isNearExpiration(X509Certificate certificate) {
      return certificate.getNotAfter().getTime() - new Date().getTime() < CERT_EXPIRY_WARNING_MILLIS;
   }

   private static boolean isSelfSigned(X509Certificate certificate) {
      return CertUtils.checkDnEquality(certificate.getSubjectX500Principal(), certificate.getIssuerX500Principal());
   }
}
