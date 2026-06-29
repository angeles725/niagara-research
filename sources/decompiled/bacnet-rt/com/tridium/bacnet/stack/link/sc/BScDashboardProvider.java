package com.tridium.bacnet.stack.link.sc;

import com.tridium.bacnet.stack.link.BBacnetLinkLayer;
import com.tridium.bacnet.stack.link.sc.authentication.BIssuerCertAndCrl;
import com.tridium.bacnet.stack.network.BNetworkPort;
import com.tridium.crypto.core.cert.NX509Certificate;
import java.security.cert.CRLException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import javax.baja.naming.BOrd;
import javax.baja.nre.annotations.AgentOn;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.security.BX509Certificate;
import javax.baja.security.crypto.CertManagerFactory;
import javax.baja.security.dashboard.BISecurityDashboardProviderAgent;
import javax.baja.security.dashboard.LexiconFormatInfo;
import javax.baja.security.dashboard.SecurityDashboardItem;
import javax.baja.security.dashboard.SecurityDashboardItemBuilder;
import javax.baja.sys.BAbsTime;
import javax.baja.sys.BComponent;
import javax.baja.sys.BIObject;
import javax.baja.sys.BObject;
import javax.baja.sys.Context;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType(
   agent = {@AgentOn(
      types = {"bacnet:ScLinkLayer"}
   )}
)
public class BScDashboardProvider extends BObject implements BISecurityDashboardProviderAgent {
   public static final Type TYPE = Sys.loadType(BScDashboardProvider.class);
   private static final String CRL_DESCRIPTION = "securityDashboard.scLinkLayer.crl.description";
   private static final String CERT_VALIDITY_DESCRIPTION = "securityDashboard.scLinkLayer.certificateValidity.description";
   private static final String CERT_SIGNATURE_DESCRIPTION = "securityDashboard.scLinkLayer.certificateSigned.description";
   private static final String OPERATIONAL_CERT_NAME = "Operational Certificate";
   private static final String ISSUER_CERT_NAME = "Issuer Certificate #";
   private static final int VERSION = 1;
   private static final double MILLIS_IN_NINETY_DAYS = 7.776E9;
   private static final SecurityDashboardItemBuilder builder = new SecurityDashboardItemBuilder(TYPE);
   private BScLinkLayer scLinkLayer;

   public Type getType() {
      return TYPE;
   }

   public void setSecurityDashboardItemsSource(BIObject object) {
      this.scLinkLayer = (BScLinkLayer)object;
   }

   public LexiconFormatInfo getSecurityDashboardSectionHeader(Context cx) {
      return LexiconFormatInfo.make(TYPE, "securityDashboard.scLinkLayer.sectionHeader", this.scLinkLayer.getParent().getName());
   }

   public BOrd getSecurityDashboardSectionHyperlinkOrd() {
      return this.scLinkLayer.getNavOrd().relativizeToSession();
   }

   public int getSecurityDashboardItemsVersion() {
      return 1;
   }

   public List<SecurityDashboardItem> getSecurityDashboardItems(Context cx) {
      List<SecurityDashboardItem> items = new ArrayList<>();
      this.addOperationalCertItems(items);
      boolean areCrlsUsed = this.areCrlsUsed();
      addIssuerItems(items, areCrlsUsed, this.scLinkLayer.getCredentials().getIssuerCertificate1(), 1);
      addIssuerItems(items, areCrlsUsed, this.scLinkLayer.getCredentials().getIssuerCertificate2(), 2);
      if (!areCrlsUsed) {
         items.add(
            builder.makeInfo()
               .withSummary("securityDashboard.scLinkLayer.crl.notInUse", new Object[0])
               .withDescription("securityDashboard.scLinkLayer.crl.description", new Object[0])
         );
      }

      return items;
   }

   private void addOperationalCertItems(List<SecurityDashboardItem> items) {
      String operationalCertAlias = this.scLinkLayer.getCredentials().getOperationalCertificateAliasAndPassword().getAlias();

      try {
         X509Certificate certificate = CertManagerFactory.getInstance().getKeyStore().getCertificate(operationalCertAlias);
         items.add(makeCertificateValidityItem(certificate, "Operational Certificate"));
         if (NX509Certificate.make(certificate).isSelfSigned()) {
            items.add(
               builder.makeAlert()
                  .withSummary("securityDashboard.scLinkLayer.certificateSelfSigned.summary", new Object[]{"Operational Certificate"})
                  .withDescription("securityDashboard.scLinkLayer.certificateSigned.description", new Object[0])
            );
         } else {
            items.add(
               builder.makeOk()
                  .withSummary("securityDashboard.scLinkLayer.certificateNotSelfSigned.summary", new Object[]{"Operational Certificate"})
                  .withDescription("securityDashboard.scLinkLayer.certificateSigned.description", new Object[0])
            );
         }
      } catch (Exception var4) {
         items.add(
            builder.makeAlert()
               .withSummary("securityDashboard.scLinkLayer.certificateNotAvailable.summary", new Object[0])
               .withDescription("securityDashboard.scLinkLayer.certificateNotAvailable.description", new Object[0])
         );
      }
   }

   private static void addIssuerItems(List<SecurityDashboardItem> items, boolean areAnyCrlsUsed, BIssuerCertAndCrl certAndCrl, int issuerNumber) {
      if (!certAndCrl.isNull() && !certAndCrl.getIssuerCertificate().isNull()) {
         addIssuerCertItems(items, certAndCrl.getIssuerCertificate(), issuerNumber);
         if (areAnyCrlsUsed) {
            items.add(makeCrlItem(certAndCrl, issuerNumber));
         }
      }
   }

   private static void addIssuerCertItems(List<SecurityDashboardItem> items, BX509Certificate issuerCert, int certNumber) {
      if (!issuerCert.isNull()) {
         items.add(makeCertificateValidityItem(issuerCert.getX509Certificate(), "Issuer Certificate #" + certNumber));
      }
   }

   private static SecurityDashboardItem makeCrlItem(BIssuerCertAndCrl certAndCrl, int issuerNumber) {
      if (certAndCrl.getCrlDistributionPointUrls().isEmpty()) {
         return builder.makeAlert()
            .withSummary("securityDashboard.scLinkLayer.crl.notConfigured", new Object[]{issuerNumber})
            .withDescription("securityDashboard.scLinkLayer.crl.description", new Object[0]);
      } else if (!certAndCrl.hasValidCrlFilename()) {
         return builder.makeAlert()
            .withSummary("securityDashboard.scLinkLayer.crl.fileLocationError", new Object[]{issuerNumber})
            .withDescription("securityDashboard.scLinkLayer.crl.description", new Object[0]);
      } else {
         try {
            Optional<X509CRL> crl = certAndCrl.getCrl();
            if (!crl.isPresent()) {
               return builder.makeAlert()
                  .withSummary("securityDashboard.scLinkLayer.crl.missing", new Object[]{issuerNumber})
                  .withDescription("securityDashboard.scLinkLayer.crl.description", new Object[0]);
            } else if (Instant.now().isAfter(crl.get().getNextUpdate().toInstant())) {
               return builder.makeAlert()
                  .withSummary("securityDashboard.scLinkLayer.crl.expired", new Object[]{issuerNumber})
                  .withDescription("securityDashboard.scLinkLayer.crl.description", new Object[0]);
            } else {
               Instant nextUpdate = crl.get().getNextUpdate().toInstant();
               return nextUpdate.isBefore(Instant.now().plus(1L, ChronoUnit.DAYS))
                  ? builder.makeWarning()
                     .withSummary("securityDashboard.scLinkLayer.crl.aboutToExpire", new Object[]{issuerNumber, BAbsTime.make(nextUpdate.toEpochMilli())})
                     .withDescription("securityDashboard.scLinkLayer.crl.description", new Object[0])
                  : builder.makeOk()
                     .withSummary("securityDashboard.scLinkLayer.crl.ok", new Object[]{issuerNumber})
                     .withDescription("securityDashboard.scLinkLayer.crl.description", new Object[0]);
            }
         } catch (CRLException var4) {
            return builder.makeAlert()
               .withSummary("securityDashboard.scLinkLayer.crl.unableToRetrieve", new Object[]{issuerNumber})
               .withDescription("securityDashboard.scLinkLayer.crl.description", new Object[0]);
         }
      }
   }

   private static SecurityDashboardItem makeCertificateValidityItem(X509Certificate certificate, String certName) {
      try {
         certificate.checkValidity();
         return certificate.getNotAfter().getTime() - new Date().getTime() < 7.776E9
            ? builder.makeWarning()
               .withSummary(
                  "securityDashboard.scLinkLayer.certificateNearExpiry.summary", new Object[]{certName, BAbsTime.make(certificate.getNotAfter().getTime())}
               )
               .withDescription("securityDashboard.scLinkLayer.certificateValidity.description", new Object[0])
            : builder.makeOk()
               .withSummary("securityDashboard.scLinkLayer.certificateValid.summary", new Object[]{certName})
               .withDescription("securityDashboard.scLinkLayer.certificateValidity.description", new Object[0]);
      } catch (CertificateExpiredException var3) {
         return builder.makeAlert()
            .withSummary("securityDashboard.scLinkLayer.certificateExpired.summary", new Object[]{certName})
            .withDescription("securityDashboard.scLinkLayer.certificateValidity.description", new Object[0]);
      } catch (CertificateNotYetValidException var4) {
         return builder.makeAlert()
            .withSummary(
               "securityDashboard.scLinkLayer.certificateNotYetValidException.summary",
               new Object[]{certName, BAbsTime.make(certificate.getNotBefore().getTime())}
            )
            .withDescription("securityDashboard.scLinkLayer.certificateValidity.description", new Object[0]);
      }
   }

   private boolean areCrlsUsed() {
      BComponent networkLayer = this.scLinkLayer.getParent().getParent().asComponent();

      for (BNetworkPort networkPort : (BNetworkPort[])networkLayer.getChildren(BNetworkPort.class)) {
         BBacnetLinkLayer linkLayer = networkPort.getLink();
         if (linkLayer instanceof BScLinkLayer
            && (
               doesIssuerUseCrl(((BScLinkLayer)linkLayer).getCredentials().getIssuerCertificate1())
                  || doesIssuerUseCrl(((BScLinkLayer)linkLayer).getCredentials().getIssuerCertificate2())
            )) {
            return true;
         }
      }

      return false;
   }

   private static boolean doesIssuerUseCrl(BIssuerCertAndCrl certAndCrl) {
      return !certAndCrl.isNull() && !certAndCrl.getIssuerCertificate().isNull() ? !certAndCrl.getCrlDistributionPointUrls().isEmpty() : false;
   }
}
