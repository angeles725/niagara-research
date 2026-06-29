package com.tridium.bacnet.stack.link.sc.authentication;

import com.tridium.bacnet.stack.link.sc.ScLinkLayerUtil;
import com.tridium.bacnet.stack.network.BNetworkPort;
import com.tridium.util.CompUtil;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.cert.CRLException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509CRL;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.logging.Level;
import javax.baja.bacnet.BBacnetNetwork;
import javax.baja.control.trigger.BDailyTriggerMode;
import javax.baja.control.trigger.BTimeTrigger;
import javax.baja.driver.util.BAbstractDescriptor;
import javax.baja.driver.util.BDescriptorState;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.status.BStatus;
import javax.baja.sys.Action;
import javax.baja.sys.BComponent;
import javax.baja.sys.BTime;
import javax.baja.sys.BValue;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.util.BDaysOfWeekBits;
import javax.baja.util.BIRestrictedComponent;
import javax.baja.util.IFuture;
import javax.baja.util.Invocation;

@NiagaraType
@NiagaraProperty(
   name = "executionTime",
   type = "BTimeTrigger",
   defaultValue = "new BTimeTrigger(BDailyTriggerMode.make(BTime.make(2,0,0), BDaysOfWeekBits.monday))",
   override = true
)
public final class BCrlDescriptor extends BAbstractDescriptor implements BIRestrictedComponent {
   public static final Property executionTime = newProperty(0, new BTimeTrigger(BDailyTriggerMode.make(BTime.make(2, 0, 0), BDaysOfWeekBits.monday)), null);
   public static final Type TYPE = Sys.loadType(BCrlDescriptor.class);
   private static final List<String> CRL_ENCODING_TYPES = Arrays.asList("application/pkix-crl", "application/x-pkcs7-crl");
   private BNetworkPort networkPort;
   private boolean fatalFault;

   public Type getType() {
      return TYPE;
   }

   public void started() throws Exception {
      super.started();
      this.checkFatalFault();
      this.updateStatus();
   }

   public void stopped() throws Exception {
      super.stopped();
      this.networkPort = null;
   }

   public void checkParentForRestrictedComponent(BComponent parent, Context cx) {
      ScLinkLayerUtil.checkParentType(TYPE, this.getParent().getType(), BIssuerCertAndCrl.TYPE);
      ScLinkLayerUtil.checkForDuplicate(this, parent);
   }

   public void doExecute() {
      if (this.isUnoperational()) {
         BBacnetScAuthenticator.logger.fine("CRLDescriptor is unoperational, skipping execute");
         this.setState(BDescriptorState.idle);
      } else {
         this.executeInProgress();
         BIssuerCertAndCrl certAndCrl = (BIssuerCertAndCrl)this.getParent();
         if (certAndCrl == null) {
            BBacnetScAuthenticator.logger.fine("CRLDescriptor is unparented, skipping execute");
            this.executeFail(ScLinkLayerUtil.LEXICON.getText("crlDescriptor.execute.unparented", null));
         } else {
            String distPoints = certAndCrl.getCrlDistributionPointUrls();
            if (distPoints.isEmpty()) {
               BBacnetScAuthenticator.logger.fine("IssuerCertAndCrl has no distribution points configured, skipping execute");
               this.executeFail(ScLinkLayerUtil.LEXICON.getText("crlDescriptor.execute.urlsEmpty", null));
            } else if (certAndCrl.getCrlFilename().isEmpty()) {
               BBacnetScAuthenticator.logger.fine("IssuerCertAndCrl could not generate a CRL filename, skipping execute");
               this.executeFail(ScLinkLayerUtil.LEXICON.getText("crlDescriptor.execute.noFilename", null));
            } else {
               X509CRL crl = null;
               String[] urls = distPoints.split("\n");

               for (String url : urls) {
                  URL distPointUrl;
                  try {
                     distPointUrl = new URL(url.trim());
                  } catch (MalformedURLException var36) {
                     ScLinkLayerUtil.logException(BBacnetScAuthenticator.logger, Level.INFO, new StringBuilder("Could not create URL from ").append(url), var36);
                     continue;
                  }

                  try {
                     HttpURLConnection connection = (HttpURLConnection)distPointUrl.openConnection();
                     connection.setInstanceFollowRedirects(true);
                     connection.connect();
                     int responseCode = connection.getResponseCode();
                     if (responseCode != 200) {
                        BBacnetScAuthenticator.logger.info("Received bad response code " + responseCode + " from " + url + ". Expected 200");
                        continue;
                     }

                     String encodingType = connection.getHeaderField("Content-Type");
                     if (!CRL_ENCODING_TYPES.contains(encodingType.toLowerCase(Locale.ENGLISH))) {
                        BBacnetScAuthenticator.logger
                           .info(
                              "Received bad encoding type '"
                                 + encodingType
                                 + "' from "
                                 + url
                                 + ". Expected 'application/pkix-crl' or 'application/x-pkcs7-crl'"
                           );
                        continue;
                     }

                     try (InputStream inputStream = connection.getInputStream()) {
                        CertificateFactory cf = CertificateFactory.getInstance("X.509");
                        crl = (X509CRL)cf.generateCRL(inputStream);
                        if (BBacnetScAuthenticator.logger.isLoggable(Level.FINE)) {
                           BBacnetScAuthenticator.logger.fine("Found a CRL at " + url);
                        }

                        if (BBacnetScAuthenticator.logger.isLoggable(Level.FINEST)) {
                           BBacnetScAuthenticator.logger.finest("CRL found:\n" + crl.toString());
                        }
                     } catch (CertificateException var33) {
                        ScLinkLayerUtil.logException(
                           BBacnetScAuthenticator.logger, Level.SEVERE, new StringBuilder("Could not find 'X.509' algorithm to parse CRL"), var33
                        );
                        this.executeFail(var33.getLocalizedMessage());
                        return;
                     } catch (CRLException var34) {
                        ScLinkLayerUtil.logException(
                           BBacnetScAuthenticator.logger, Level.INFO, new StringBuilder("Could not parse CRL from ").append(url), var34
                        );
                        continue;
                     }
                  } catch (IOException var35) {
                     ScLinkLayerUtil.logException(BBacnetScAuthenticator.logger, Level.INFO, new StringBuilder("Could not connect to ").append(url), var35);
                     continue;
                  }

                  try {
                     certAndCrl.validateCrl(crl);
                     break;
                  } catch (Exception var30) {
                     ScLinkLayerUtil.logException(
                        BBacnetScAuthenticator.logger, new StringBuilder("Could not validate CRL for ").append(certAndCrl.getName()), var30
                     );
                     crl = null;
                  }
               }

               if (crl == null) {
                  BBacnetScAuthenticator.logger.warning("Could not acquire CRL from distribution points for " + certAndCrl.getName());
                  this.executeFail(ScLinkLayerUtil.LEXICON.getText("crlDescriptor.execute.getFailed", null));
               } else {
                  try {
                     certAndCrl.saveCrl(crl);
                     this.executeOk();
                  } catch (Exception var28) {
                     ScLinkLayerUtil.logException(
                        BBacnetScAuthenticator.logger,
                        new StringBuilder("Could not save CRL for ")
                           .append(certAndCrl.getName())
                           .append(" of SC port ")
                           .append(ScLinkLayerUtil.getScLinkLayer(certAndCrl).getParent().getName()),
                        var28
                     );
                     this.executeFail(ScLinkLayerUtil.LEXICON.getText("crlDescriptor.execute.saveFailed", null));
                  }
               }
            }
         }
      }
   }

   public void updateStatus() {
      int newStatus = this.getStatus().getBits();
      BStatus portStatus = this.networkPort == null ? BStatus.fault : this.networkPort.getStatus();
      if (this.getEnabled() && !portStatus.isDisabled()) {
         newStatus &= -2;
      } else {
         newStatus |= 1;
      }

      if (!this.isFatalFault() && (this.getLastFailure().isNull() || !this.getLastFailure().isAfter(this.getLastSuccess())) && !portStatus.isFault()) {
         newStatus &= -3;
      } else {
         newStatus |= 2;
      }

      if (newStatus != this.getStatus().getBits()) {
         this.setStatus(BStatus.make(newStatus));
      }
   }

   protected IFuture postExecute(Action action, BValue arg, Context cx) {
      BBacnetNetwork bacnetNetwork = BBacnetNetwork.bacnet();
      if (bacnetNetwork != null) {
         return BBacnetNetwork.bacnet().postAsync(new Invocation(this, action, arg, cx));
      } else {
         BBacnetScAuthenticator.logger.warning("Cannot execute " + action.getName() + ". BacnetNetwork was not found.");
         this.executeFail("BacnetNetwork not found");
         return null;
      }
   }

   public boolean isUnoperational() {
      return this.fatalFault || super.isUnoperational();
   }

   public boolean isFatalFault() {
      return this.fatalFault;
   }

   private void checkFatalFault() {
      Optional<BNetworkPort> networkPort = CompUtil.closestAncestor(this.getParent(), BNetworkPort.class);
      if (!networkPort.isPresent()) {
         this.setFaultCause(ScLinkLayerUtil.LEXICON.getText("crlDescriptor.fault.notUnderPort", null));
         this.fatalFault = true;
      } else {
         this.networkPort = networkPort.get();
         if (BIssuerCertAndCrl.getCrlDir() == null) {
            this.setFaultCause(ScLinkLayerUtil.LEXICON.getText("crlDescriptor.fault.couldNotCreateCrlDir", null));
            this.fatalFault = true;
         } else {
            this.fatalFault = false;
            this.setFaultCause("");
         }
      }
   }
}
