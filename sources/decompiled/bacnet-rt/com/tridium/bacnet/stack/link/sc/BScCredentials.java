package com.tridium.bacnet.stack.link.sc;

import com.tridium.bacnet.stack.link.sc.authentication.BIssuerCertAndCrl;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.control.trigger.BIntervalTriggerMode;
import javax.baja.control.trigger.BTimeTrigger;
import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.security.BCertificateAliasAndPassword;
import javax.baja.security.BPassword;
import javax.baja.sys.Action;
import javax.baja.sys.BComponent;
import javax.baja.sys.BComponentEvent;
import javax.baja.sys.BFacets;
import javax.baja.sys.BRelTime;
import javax.baja.sys.Context;
import javax.baja.sys.Flags;
import javax.baja.sys.IllegalChildException;
import javax.baja.sys.Property;
import javax.baja.sys.Subscriber;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.util.BIRestrictedComponent;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "operationalCertificate",
      type = "String",
      defaultValue = "",
      facets = {@Facet(
         name = "BFacets.SECURITY",
         value = "true"
      ), @Facet(
         name = "BFacets.FIELD_EDITOR",
         value = "\"workbench:CertificateAliasFE\""
      ), @Facet(
         name = "BFacets.UX_FIELD_EDITOR",
         value = "\"webEditors:CertificateAliasEditor\""
      ), @Facet("BFacets.make(\"purposeId\", \"\")")},
      deprecated = true
   ), @NiagaraProperty(
      name = "operationalCertificateAliasAndPassword",
      type = "BCertificateAliasAndPassword",
      defaultValue = "BCertificateAliasAndPassword.DEFAULT",
      flags = 4,
      facets = {@Facet(
         name = "BFacets.SECURITY",
         value = "true"
      )}
   ), @NiagaraProperty(
      name = "issuerCertificate1",
      type = "BIssuerCertAndCrl",
      defaultValue = "new BIssuerCertAndCrl()",
      facets = {@Facet(
         name = "BFacets.SECURITY",
         value = "true"
      )}
   ), @NiagaraProperty(
      name = "issuerCertificate2",
      type = "BIssuerCertAndCrl",
      defaultValue = "new BIssuerCertAndCrl()",
      facets = {@Facet(
         name = "BFacets.SECURITY",
         value = "true"
      )}
   ), @NiagaraProperty(
      name = "retryTrigger",
      type = "BTimeTrigger",
      defaultValue = "new BTimeTrigger(BIntervalTriggerMode.make(BRelTime.makeHours(12)))"
   )})
@NiagaraAction(
   name = "retryFailedCrlRetrievals"
)
public class BScCredentials extends BComponent implements BIRestrictedComponent {
   @Deprecated
   public static final Property operationalCertificate = newProperty(
      0,
      "",
      BFacets.make(
         BFacets.make(
            BFacets.make(BFacets.make("security", true), BFacets.make("fieldEditor", "workbench:CertificateAliasFE")),
            BFacets.make("uxFieldEditor", "webEditors:CertificateAliasEditor")
         ),
         BFacets.make("purposeId", "")
      )
   );
   public static final Property operationalCertificateAliasAndPassword = newProperty(4, BCertificateAliasAndPassword.DEFAULT, BFacets.make("security", true));
   public static final Property issuerCertificate1 = newProperty(0, new BIssuerCertAndCrl(), BFacets.make("security", true));
   public static final Property issuerCertificate2 = newProperty(0, new BIssuerCertAndCrl(), BFacets.make("security", true));
   public static final Property retryTrigger = newProperty(0, new BTimeTrigger(BIntervalTriggerMode.make(BRelTime.makeHours(12))), null);
   public static final Action retryFailedCrlRetrievals = newAction(0, null);
   public static final Type TYPE = Sys.loadType(BScCredentials.class);
   private static final Logger LOG = Logger.getLogger("bacnet.sc.linkLayer");
   private Subscriber portSubscriber;

   @Deprecated
   public String getOperationalCertificate() {
      return this.getString(operationalCertificate);
   }

   @Deprecated
   public void setOperationalCertificate(String v) {
      this.setString(operationalCertificate, v, null);
   }

   public BCertificateAliasAndPassword getOperationalCertificateAliasAndPassword() {
      return (BCertificateAliasAndPassword)this.get(operationalCertificateAliasAndPassword);
   }

   public void setOperationalCertificateAliasAndPassword(BCertificateAliasAndPassword v) {
      this.set(operationalCertificateAliasAndPassword, v, null);
   }

   public BIssuerCertAndCrl getIssuerCertificate1() {
      return (BIssuerCertAndCrl)this.get(issuerCertificate1);
   }

   public void setIssuerCertificate1(BIssuerCertAndCrl v) {
      this.set(issuerCertificate1, v, null);
   }

   public BIssuerCertAndCrl getIssuerCertificate2() {
      return (BIssuerCertAndCrl)this.get(issuerCertificate2);
   }

   public void setIssuerCertificate2(BIssuerCertAndCrl v) {
      this.set(issuerCertificate2, v, null);
   }

   public BTimeTrigger getRetryTrigger() {
      return (BTimeTrigger)this.get(retryTrigger);
   }

   public void setRetryTrigger(BTimeTrigger v) {
      this.set(retryTrigger, v, null);
   }

   public void retryFailedCrlRetrievals() {
      this.invoke(retryFailedCrlRetrievals, null, null);
   }

   public Type getType() {
      return TYPE;
   }

   public final Object fw(int x, Object a, Object b, Object c, Object d) {
      switch (x) {
         case 11:
            this.fwStarted();
         default:
            return super.fw(x, a, b, c, d);
      }
   }

   private void fwStarted() {
      if (!Flags.has(this, operationalCertificate, 268435456)) {
         this.getOperationalCertificateAliasAndPassword().setAlias(this.getOperationalCertificate());
         this.getOperationalCertificateAliasAndPassword().setPassword(BPassword.DEFAULT);
         Flags.add(this, operationalCertificate, null, new int[]{268435461});
      }

      if (!Flags.has(this, operationalCertificateAliasAndPassword, 268435456)) {
         Flags.add(this, operationalCertificateAliasAndPassword, null, new int[]{268435456});
         this.setFlags(operationalCertificateAliasAndPassword, this.getFlags(operationalCertificateAliasAndPassword) & -5);
         this.getOperationalCertificateAliasAndPassword().setFacets(BCertificateAliasAndPassword.alias, BFacets.make("purposeId", ""));
      }
   }

   public void started() throws Exception {
      super.started();
      this.linkTo("retryLink", this.getRetryTrigger(), BTimeTrigger.fireTrigger, retryFailedCrlRetrievals);
      this.portSubscriber = Subscriber.make(new BScCredentials.NetworkPortEventConsumer());
      this.portSubscriber.subscribe(this.getParent().getParent().asComponent());
   }

   public void changed(Property property, Context context) {
      if (this.isRunning()) {
         BScLinkLayer linkLayer = (BScLinkLayer)this.getParent();
         if (!issuerCertificate1.equals(property) && !issuerCertificate2.equals(property)) {
            if (operationalCertificateAliasAndPassword.equals(property)) {
               if (BScLinkLayer.logger.isLoggable(Level.FINE)) {
                  BScLinkLayer.logger
                     .fine(
                        "Operational certificate/password has been changed in SC port " + linkLayer.getParent().getName() + ": restarting web socket initiator"
                     );
               }

               linkLayer.restartWebSocketInitiator();
            }
         } else {
            if (BScLinkLayer.logger.isLoggable(Level.FINE)) {
               BScLinkLayer.logger
                  .fine(
                     property.getName()
                        + " has been changed in SC port "
                        + linkLayer.getParent().getName()
                        + ": signaling that trust anchors have been updated"
                  );
            }

            linkLayer.trustAnchorsUpdated();
         }
      }
   }

   public void stopped() throws Exception {
      super.stopped();
      Property retryLink = this.getProperty("retryLink");
      if (retryLink != null) {
         this.remove(retryLink);
      }

      if (this.portSubscriber != null) {
         this.portSubscriber.unsubscribeAll();
      }
   }

   public boolean isChildLegal(BComponent child) {
      if (child instanceof BIssuerCertAndCrl) {
         try {
            ScLinkLayerUtil.checkForTooMany(child, this, 2);
            return true;
         } catch (Exception var3) {
            ScLinkLayerUtil.logException(LOG, new StringBuilder("Could not add Issuer Cert And CRL to ").append(this.getName()), var3);
            return false;
         }
      } else {
         return true;
      }
   }

   public void doRetryFailedCrlRetrievals() {
      for (BIssuerCertAndCrl certAndCrl : (BIssuerCertAndCrl[])this.getChildren(BIssuerCertAndCrl.class)) {
         if (certAndCrl.getCrlDescriptor().isFault()) {
            certAndCrl.getCrlDescriptor().execute();
         }
      }
   }

   public void checkParentForRestrictedComponent(BComponent parent, Context cx) {
      if (!(parent instanceof BScLinkLayer)) {
         throw new IllegalChildException("baja", "IllegalChildException.parentAndChild", new Object[]{this.getType(), parent.getType()});
      } else {
         ScLinkLayerUtil.checkForDuplicate(this, parent);
      }
   }

   private class NetworkPortEventConsumer implements Consumer<BComponentEvent> {
      private NetworkPortEventConsumer() {
      }

      public void accept(BComponentEvent event) {
         if (event.getId() == 0 && "enabled".equals(event.getSlotName())) {
            BIssuerCertAndCrl[] issuerCertAndCrls = (BIssuerCertAndCrl[])BScCredentials.this.getChildren(BIssuerCertAndCrl.class);

            for (BIssuerCertAndCrl issuerCertAndCrl : issuerCertAndCrls) {
               issuerCertAndCrl.getCrlDescriptor().updateStatus();
            }
         }
      }
   }
}
