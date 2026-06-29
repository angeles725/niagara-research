package com.tridium.bacnet.stack.link.sc.authentication;

import com.tridium.authn.BAuthenticationService;
import com.tridium.bacnet.stack.link.sc.BScCredentials;
import com.tridium.bacnet.stack.link.sc.BScLinkLayer;
import com.tridium.bacnet.stack.link.sc.ScLinkLayerUtil;
import com.tridium.crypto.core.cert.CertUtils;
import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.AccessController;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.security.cert.CRLException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import javax.baja.alarm.AlarmSupport;
import javax.baja.alarm.BAlarmRecord;
import javax.baja.alarm.BAlarmSourceInfo;
import javax.baja.alarm.BIAlarmSource;
import javax.baja.alarm.BSourceState;
import javax.baja.naming.BOrdList;
import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraActions;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.util.ByteArrayUtil;
import javax.baja.security.BX509Certificate;
import javax.baja.status.BIStatus;
import javax.baja.status.BStatus;
import javax.baja.sys.Action;
import javax.baja.sys.BAbsTime;
import javax.baja.sys.BBoolean;
import javax.baja.sys.BComponent;
import javax.baja.sys.BFacets;
import javax.baja.sys.BString;
import javax.baja.sys.BasicContext;
import javax.baja.sys.Clock;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.sys.Clock.Ticket;
import javax.baja.util.BFormat;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "status",
      type = "BStatus",
      defaultValue = "BStatus.ok",
      flags = 3
   ), @NiagaraProperty(
      name = "issuerCertificate",
      type = "BX509Certificate",
      defaultValue = "BX509Certificate.DEFAULT",
      facets = {@Facet(
         name = "BFacets.SECURITY",
         value = "true"
      ), @Facet("BFacets.make(\"warningText\", \"%lexicon(bacnet:issuerCertEditor.warningText)%\")")}
   ), @NiagaraProperty(
      name = "useCrlDistributionPointInIssuer",
      type = "boolean",
      defaultValue = "true",
      facets = {@Facet(
         name = "BFacets.SECURITY",
         value = "true"
      )}
   ), @NiagaraProperty(
      name = "crlDistributionPointUrls",
      type = "String",
      defaultValue = "",
      flags = 1,
      facets = {@Facet(
         name = "BFacets.SECURITY",
         value = "true"
      ), @Facet(
         name = "BFacets.MULTI_LINE",
         value = "true"
      )}
   ), @NiagaraProperty(
      name = "crlDescriptor",
      type = "BCrlDescriptor",
      defaultValue = "new BCrlDescriptor()"
   ), @NiagaraProperty(
      name = "alarmSourceInfo",
      type = "BAlarmSourceInfo",
      defaultValue = "initAlarmSourceInfo()"
   )})
@NiagaraActions({@NiagaraAction(
      name = "ackAlarm",
      parameterType = "BAlarmRecord",
      defaultValue = "new BAlarmRecord()",
      returnType = "BBoolean",
      flags = 4
   ), @NiagaraAction(
      name = "checkCrlExpiration",
      flags = 4
   )})
public final class BIssuerCertAndCrl extends BComponent implements BIAlarmSource, BIStatus {
   public static final Property status = newProperty(3, BStatus.ok, null);
   public static final Property issuerCertificate = newProperty(
      0, BX509Certificate.DEFAULT, BFacets.make(BFacets.make("security", true), BFacets.make("warningText", "%lexicon(bacnet:issuerCertEditor.warningText)%"))
   );
   public static final Property useCrlDistributionPointInIssuer = newProperty(0, true, BFacets.make("security", true));
   public static final Property crlDistributionPointUrls = newProperty(1, "", BFacets.make(BFacets.make("security", true), BFacets.make("multiLine", true)));
   public static final Property crlDescriptor = newProperty(0, new BCrlDescriptor(), null);
   public static final Property alarmSourceInfo = newProperty(0, initAlarmSourceInfo(), null);
   public static final Action ackAlarm = newAction(4, new BAlarmRecord(), null);
   public static final Action checkCrlExpiration = newAction(4, null);
   public static final Type TYPE = Sys.loadType(BIssuerCertAndCrl.class);
   private static final BasicContext UPDATE_CONTEXT = new BasicContext();
   private static final String CRL_EXTENSION = ".crl";
   private String cachedUrls = "";
   private String crlFilename;
   private AlarmSupport alarmSupport;
   private final AtomicReference<Ticket> expirationCheckTicket = new AtomicReference<>();
   private Instant crlExpirationInstant;

   public BStatus getStatus() {
      return (BStatus)this.get(status);
   }

   public void setStatus(BStatus v) {
      this.set(status, v, null);
   }

   public BX509Certificate getIssuerCertificate() {
      return (BX509Certificate)this.get(issuerCertificate);
   }

   public void setIssuerCertificate(BX509Certificate v) {
      this.set(issuerCertificate, v, null);
   }

   public boolean getUseCrlDistributionPointInIssuer() {
      return this.getBoolean(useCrlDistributionPointInIssuer);
   }

   public void setUseCrlDistributionPointInIssuer(boolean v) {
      this.setBoolean(useCrlDistributionPointInIssuer, v, null);
   }

   public String getCrlDistributionPointUrls() {
      return this.getString(crlDistributionPointUrls);
   }

   public void setCrlDistributionPointUrls(String v) {
      this.setString(crlDistributionPointUrls, v, null);
   }

   public BCrlDescriptor getCrlDescriptor() {
      return (BCrlDescriptor)this.get(crlDescriptor);
   }

   public void setCrlDescriptor(BCrlDescriptor v) {
      this.set(crlDescriptor, v, null);
   }

   public BAlarmSourceInfo getAlarmSourceInfo() {
      return (BAlarmSourceInfo)this.get(alarmSourceInfo);
   }

   public void setAlarmSourceInfo(BAlarmSourceInfo v) {
      this.set(alarmSourceInfo, v, null);
   }

   public BBoolean ackAlarm(BAlarmRecord parameter) {
      return (BBoolean)this.invoke(ackAlarm, parameter, null);
   }

   public void checkCrlExpiration() {
      this.invoke(checkCrlExpiration, null, null);
   }

   public Type getType() {
      return TYPE;
   }

   public boolean isParentLegal(BComponent parent) {
      return parent instanceof BScCredentials;
   }

   public void started() throws Exception {
      super.started();
      this.alarmSupport = new AlarmSupport(this, this.getAlarmSourceInfo());
      this.crlExpirationInstant = this.generateCrlExpirationInstant();
      if (this.crlExpirationInstant != null) {
         this.generateExpirationTicket(this.crlExpirationInstant);
      }

      this.updateAlarm();
   }

   public void changed(Property property, Context context) {
      if (this.isRunning()) {
         if (property.equals(issuerCertificate)) {
            if (this.getUseCrlDistributionPointInIssuer()) {
               this.updateCrlDistributionPointFromCertificate();
               this.getCrlDescriptor().updateStatus();
            }

            this.deleteCrl();
            this.crlFilename = this.generateCrlFilename();
            BScLinkLayer linkLayer = ScLinkLayerUtil.getScLinkLayer(this);
            if (BBacnetScAuthenticator.logger.isLoggable(Level.FINE)) {
               BBacnetScAuthenticator.logger
                  .fine(
                     "issuerCertificate property has been changed in IssuerCertAndCrl "
                        + this.getName()
                        + " of SC port "
                        + linkLayer.getParent().getName()
                        + ": CRL has been deleted"
                  );
            }

            linkLayer.trustAnchorsUpdated();
            this.getCrlDescriptor().updateStatus();
         } else if (property.equals(useCrlDistributionPointInIssuer)) {
            if (this.getUseCrlDistributionPointInIssuer()) {
               this.setFlags(crlDistributionPointUrls, this.getFlags(crlDistributionPointUrls) | 1);
               this.cachedUrls = this.getCrlDistributionPointUrls();
               this.updateCrlDistributionPointFromCertificate();
            } else {
               this.setFlags(crlDistributionPointUrls, this.getFlags(crlDistributionPointUrls) & -2);
               this.set(crlDistributionPointUrls, BString.make(this.cachedUrls), UPDATE_CONTEXT);
            }

            this.getCrlDescriptor().updateStatus();
         } else if (property.equals(crlDistributionPointUrls) && context != UPDATE_CONTEXT) {
            if (this.getUseCrlDistributionPointInIssuer()) {
               BBacnetScAuthenticator.logger
                  .info(
                     "CRL Distribution Point URLs was modified, but is configured to use values from the issuer certificate. Reverting to CRL distributions points found in the issuer certificate."
                  );
               this.updateCrlDistributionPointFromCertificate();
            }

            this.getCrlDescriptor().updateStatus();
         }

         super.changed(property, context);
      }
   }

   private void updateCrlDistributionPointFromCertificate() {
      X509Certificate configuredIssuer = this.getIssuerCertificate().getX509Certificate();
      if (configuredIssuer == null) {
         BBacnetScAuthenticator.logger.info("Issuer is configured to use CRL distribution points from the certificate, but no Issuer is configured");
      } else {
         List<String> crlDistributionPoints = CertUtils.getCrlDistributionPointsFromCertificate(configuredIssuer);
         if (crlDistributionPoints.isEmpty()) {
            BBacnetScAuthenticator.logger
               .info(
                  String.format(
                     "Issuer <%s> is configured to use CRL distribution points from the certificate, but the certificate has no CRL distribution points",
                     configuredIssuer.getSubjectX500Principal().toString()
                  )
               );
            this.set(crlDistributionPointUrls, BString.make(""), UPDATE_CONTEXT);
         } else {
            this.set(crlDistributionPointUrls, BString.make(String.join("\n", crlDistributionPoints)), UPDATE_CONTEXT);
         }
      }
   }

   void validateCrl(X509CRL crl) throws Exception {
      X509Certificate crlIssuer = this.getIssuerCertificate().getX509Certificate();
      if (crlIssuer == null) {
         throw new Exception("CRL cannot be validated. Issuer Certificate is not set.");
      } else {
         crl.verify(crlIssuer.getPublicKey());
      }
   }

   void saveCrl(X509CRL crl) throws Exception {
      File crlDir = getCrlDir();
      if (crlDir == null) {
         throw new Exception(
            "Could not save CRL for "
               + this.getName()
               + " of SC port "
               + ScLinkLayerUtil.getScLinkLayer(this).getParent().getName()
               + ": Directory does not exist and could not be created."
         );
      } else {
         String crlFilename = this.getCrlFilename();
         if (crlFilename.isEmpty()) {
            throw new Exception(
               "Could not save CRL for "
                  + this.getName()
                  + " of SC port "
                  + ScLinkLayerUtil.getScLinkLayer(this).getParent().getName()
                  + ": Filename not generated"
            );
         } else {
            Path crlPath = new File(crlDir, crlFilename).toPath();

            try {
               AccessController.doPrivileged((PrivilegedExceptionAction<Void>)(() -> {
                  Files.write(crlPath, crl.getEncoded());
                  return null;
               }));
               if (BBacnetScAuthenticator.logger.isLoggable(Level.FINE)) {
                  BBacnetScAuthenticator.logger
                     .fine(
                        "CRL was saved for "
                           + this.getName()
                           + " of SC port "
                           + ScLinkLayerUtil.getScLinkLayer(this).getParent().getName()
                           + "; directory: "
                           + crlDir
                           + ", filename: "
                           + crlFilename
                     );
               }
            } catch (PrivilegedActionException var6) {
               throw new Exception(
                  "Could not write CRL for " + this.getName() + " of SC port " + ScLinkLayerUtil.getScLinkLayer(this).getParent().getName(),
                  var6.getException()
               );
            }

            this.crlExpirationInstant = crl.getNextUpdate().toInstant();
            this.generateExpirationTicket(this.crlExpirationInstant);
            this.updateAlarm();
            BScLinkLayer linkLayer = ScLinkLayerUtil.getScLinkLayer(this);
            if (BBacnetScAuthenticator.logger.isLoggable(Level.FINE)) {
               BBacnetScAuthenticator.logger
                  .fine(
                     "CRL has been saved for property "
                        + this.getName()
                        + " of SC port "
                        + linkLayer.getParent().getName()
                        + ": signaling that trust anchors have been updated"
                  );
            }

            linkLayer.trustAnchorsUpdated();
         }
      }
   }

   private void deleteCrl() {
      File crlDir = getCrlDir();
      if (crlDir == null) {
         BBacnetScAuthenticator.logger
            .warning(
               "Could not delete CRL for "
                  + this.getName()
                  + " of SC port "
                  + ScLinkLayerUtil.getScLinkLayer(this).getParent().getName()
                  + ": Directory does not exist and could not be created"
            );
      }

      String crlFilename = this.getCrlFilename();
      if (crlFilename.isEmpty()) {
         if (BBacnetScAuthenticator.logger.isLoggable(Level.FINE)) {
            BBacnetScAuthenticator.logger
               .fine(
                  "Filename has not yet been generated for "
                     + this.getName()
                     + " of SC port "
                     + ScLinkLayerUtil.getScLinkLayer(this).getParent().getName()
                     + ": skipping delete"
               );
         }
      } else {
         Path crlPath = new File(crlDir, crlFilename).toPath();

         try {
            boolean wasDeleted = AccessController.doPrivileged((PrivilegedExceptionAction<Boolean>)(() -> Files.deleteIfExists(crlPath)));
            if (BBacnetScAuthenticator.logger.isLoggable(Level.FINE)) {
               if (wasDeleted) {
                  BBacnetScAuthenticator.logger
                     .fine(
                        "CRL was deleted for "
                           + this.getName()
                           + " of SC port "
                           + ScLinkLayerUtil.getScLinkLayer(this).getParent().getName()
                           + "; directory: "
                           + crlDir
                           + ", filename: "
                           + crlFilename
                     );
               } else {
                  BBacnetScAuthenticator.logger
                     .fine(
                        "CRL was not deleted because it did not exist for "
                           + this.getName()
                           + " of SC port "
                           + ScLinkLayerUtil.getScLinkLayer(this).getParent().getName()
                           + "; directory: "
                           + crlDir
                           + ", filename: "
                           + crlFilename
                     );
               }
            }
         } catch (PrivilegedActionException var5) {
            ScLinkLayerUtil.logException(
               BBacnetScAuthenticator.logger,
               new StringBuilder("Could not delete CRL for ")
                  .append(this.getName())
                  .append(" of SC port ")
                  .append(ScLinkLayerUtil.getScLinkLayer(this).getParent().getName())
                  .append("; directory: ")
                  .append(crlDir)
                  .append(", filename: ")
                  .append(crlFilename),
               var5.getException()
            );
         }

         Ticket ticket = this.expirationCheckTicket.getAndSet(null);
         if (ticket != null) {
            ticket.cancel();
         }

         this.crlExpirationInstant = null;
         this.updateAlarm();
      }
   }

   private String generateCrlFilename() {
      X509Certificate issuerCert = this.getIssuerCertificate().getX509Certificate();
      if (issuerCert == null) {
         return "";
      } else {
         try {
            MessageDigest md = MessageDigest.getInstance("SHA256");
            byte[] digest = md.digest(issuerCert.getSubjectX500Principal().getName().getBytes());
            return ByteArrayUtil.toHexString(digest) + ".crl";
         } catch (NoSuchAlgorithmException var4) {
            ScLinkLayerUtil.logException(
               BBacnetScAuthenticator.logger, Level.SEVERE, new StringBuilder("Could not generate CRL filename for ").append(this.getName()), var4
            );
            return "";
         }
      }
   }

   static File getCrlDir() {
      return BIssuerCertAndCrl.CrlDirHolder.CRL_DIR;
   }

   String getCrlFilename() {
      if (this.crlFilename == null) {
         this.crlFilename = this.generateCrlFilename();
      }

      return this.crlFilename;
   }

   public Optional<X509CRL> getCrl() throws CRLException {
      if (!this.isConfiguredForCrls()) {
         return Optional.empty();
      } else {
         File crlDir = getCrlDir();
         String crlFilename = this.getCrlFilename();
         if (crlDir != null && !crlFilename.isEmpty()) {
            File crlFile = new File(crlDir, crlFilename);
            if (!AccessController.doPrivileged(crlFile::exists)) {
               this.getCrlDescriptor().execute();
               throw new CRLException("CRLs are configured for " + this.getName() + " but could not find CRL.");
            } else {
               try (FileInputStream fileInputStream = AccessController.doPrivileged(
                     (PrivilegedExceptionAction<FileInputStream>)(() -> new FileInputStream(crlFile))
                  )) {
                  CertificateFactory cf = CertificateFactory.getInstance("X.509");
                  X509CRL crl = (X509CRL)cf.generateCRL(fileInputStream);
                  this.validateCrl(crl);
                  return Optional.of(crl);
               } catch (Exception var20) {
                  Exception e = var20;
                  if (var20 instanceof PrivilegedActionException) {
                     e = ((PrivilegedActionException)var20).getException();
                  }

                  throw new CRLException("CRLs are configured for " + this.getName() + " but could not retrieve CRL from file.", e);
               }
            }
         } else {
            return Optional.empty();
         }
      }
   }

   private Instant generateCrlExpirationInstant() {
      if (!this.isConfiguredForCrls()) {
         return null;
      } else {
         File crlDir = getCrlDir();
         String crlFilename = this.getCrlFilename();
         if (crlDir != null && !crlFilename.isEmpty()) {
            File crlFile = new File(crlDir, crlFilename);
            if (AccessController.doPrivileged(crlFile::exists)) {
               try (FileInputStream inputStream = AccessController.doPrivileged(
                     (PrivilegedExceptionAction<FileInputStream>)(() -> new FileInputStream(crlFile))
                  )) {
                  CertificateFactory cf = CertificateFactory.getInstance("X.509");
                  X509CRL crl = (X509CRL)cf.generateCRL(inputStream);
                  return crl.getNextUpdate().toInstant();
               } catch (Exception var20) {
                  Exception e = var20;
                  if (var20 instanceof PrivilegedActionException) {
                     e = ((PrivilegedActionException)var20).getException();
                  }

                  ScLinkLayerUtil.logException(
                     BBacnetScAuthenticator.logger, new StringBuilder("Could not load CRL for ").append(this.getName()).append(" to get expiration date"), e
                  );
               }
            }

            return null;
         } else {
            return null;
         }
      }
   }

   public boolean hasValidCrlFilename() {
      return getCrlDir() != null && !this.getCrlFilename().isEmpty();
   }

   private boolean isConfiguredForCrls() {
      return !this.getCrlDistributionPointUrls().isEmpty() && !this.getIssuerCertificate().isNull();
   }

   private void generateExpirationTicket(Instant expirationInstant) {
      Ticket ticket = this.expirationCheckTicket.getAndSet(null);
      if (ticket != null) {
         ticket.cancel();
      }

      if (expirationInstant != null) {
         this.expirationCheckTicket.set(Clock.schedule(this, BAbsTime.make(expirationInstant.plusSeconds(5L).toEpochMilli()), checkCrlExpiration, null));
      }
   }

   public void doCheckCrlExpiration() {
      this.updateAlarm();
   }

   private void updateAlarm() {
      if (this.crlExpirationInstant != null && Instant.now().isAfter(this.crlExpirationInstant)) {
         this.alarmOffNormal();
      } else {
         this.alarmNormal();
      }
   }

   public BBoolean doAckAlarm(BAlarmRecord ackRequest) {
      try {
         boolean alarmAck = this.alarmSupport.ackAlarm(ackRequest);
         if (alarmAck) {
            this.setStatus(BStatus.make(this.getStatus(), 128, false));
         }

         return BBoolean.make(alarmAck);
      } catch (Exception var3) {
         ScLinkLayerUtil.logException(
            BBacnetScAuthenticator.logger, new StringBuilder("Failed to ack alarm for ").append(BOrdList.make(this.getNavOrd())), var3
         );
         return BBoolean.FALSE;
      }
   }

   private void alarmOffNormal() {
      if (!this.getStatus().isAlarm() && shouldGenerateAlarmOnExpiration() && this.isRunning()) {
         try {
            boolean ackRequired = this.alarmSupport.isAckRequired(BSourceState.offnormal);
            this.alarmSupport.newOffnormalAlarm();
            int statusBits = this.getStatus().getBits();
            statusBits |= 8;
            if (ackRequired) {
               statusBits |= 128;
            }

            this.setStatus(BStatus.make(statusBits));
         } catch (Exception var3) {
            ScLinkLayerUtil.logException(
               BBacnetScAuthenticator.logger, new StringBuilder("Failed to send offNormal alarm for ").append(this.alarmSupport.getSourceOrd()), var3
            );
         }
      }
   }

   private void alarmNormal() {
      if (this.getStatus().isAlarm() && this.isRunning()) {
         try {
            this.alarmSupport.toNormal(null);
            this.setStatus(BStatus.make(this.getStatus(), 8, false));
         } catch (Exception var2) {
            ScLinkLayerUtil.logException(
               BBacnetScAuthenticator.logger, new StringBuilder("Failed to send toNormal alarm for ").append(this.alarmSupport.getSourceOrd()), var2
            );
         }
      }
   }

   private static BAlarmSourceInfo initAlarmSourceInfo() {
      BAlarmSourceInfo alarmSourceInfo = new BAlarmSourceInfo();
      alarmSourceInfo.setSourceName(BFormat.make("%parent.parent.parent.displayName%.%displayName%"));
      alarmSourceInfo.setToOffnormalText(BFormat.make("%lexicon(bacnet:issuerCertAndCrl.crlExpired)%"));
      alarmSourceInfo.setToNormalText(BFormat.make("%lexicon(bacnet:issuerCertAndCrl.crlNotExpired)%"));
      return alarmSourceInfo;
   }

   private static boolean shouldGenerateAlarmOnExpiration() {
      BAuthenticationService authnService = BAuthenticationService.getService();
      BBacnetScAuthenticationScheme[] bacnetSchemes = (BBacnetScAuthenticationScheme[])authnService.getAuthenticationSchemes()
         .getChildren(BBacnetScAuthenticationScheme.class);
      return bacnetSchemes.length == 0 ? false : bacnetSchemes[0].getGenerateAlarmOnCrlExpiration();
   }

   private static File fetchCrlDir() {
      File protectedStationHome = Sys.getProtectedStationHome();
      if (protectedStationHome == null) {
         return null;
      } else {
         try {
            return AccessController.doPrivileged((PrivilegedAction<File>)(() -> {
               File dir = new File(protectedStationHome, "bacnet" + File.separator + "crls");
               if (!dir.exists() && !dir.mkdirs()) {
                  BBacnetScAuthenticator.logger.warning("Failed to retrieve or create directory for BACnet/SC CRLs");
                  return null;
               } else {
                  return dir;
               }
            }));
         } catch (Exception var2) {
            ScLinkLayerUtil.logException(BBacnetScAuthenticator.logger, new StringBuilder("Failed to retrieve or create directory for BACnet/SC CRLs"), var2);
            return null;
         }
      }
   }

   private static final class CrlDirHolder {
      static final File CRL_DIR = BIssuerCertAndCrl.fetchCrlDir();
   }
}
