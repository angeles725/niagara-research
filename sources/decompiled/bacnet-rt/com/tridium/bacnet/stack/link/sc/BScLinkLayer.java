package com.tridium.bacnet.stack.link.sc;

import com.tridium.bacnet.stack.BacnetInputStream;
import com.tridium.bacnet.stack.link.BBacnetLinkLayer;
import com.tridium.bacnet.stack.link.sc.authentication.BBacnetScAuthenticationScheme;
import com.tridium.bacnet.stack.link.sc.authentication.BBacnetScAuthenticator;
import com.tridium.bacnet.stack.link.sc.authentication.BIssuerCertAndCrl;
import com.tridium.bacnet.stack.link.sc.connection.BAbstractScWebSocketInitiator;
import com.tridium.bacnet.stack.link.sc.connection.BDirectAcceptingConnection;
import com.tridium.bacnet.stack.link.sc.connection.BDirectInitiatingConnection;
import com.tridium.bacnet.stack.link.sc.connection.BHubAcceptingConnection;
import com.tridium.bacnet.stack.link.sc.connection.BInitiatingConnection;
import com.tridium.bacnet.stack.link.sc.connection.jetty.BJettyScWebSocketInitiator;
import com.tridium.bacnet.stack.link.sc.message.HeaderOption;
import com.tridium.bacnet.stack.link.sc.message.ProprietaryHeaderOption;
import com.tridium.bacnet.stack.link.sc.message.ScMessageUtil;
import com.tridium.bacnet.stack.link.sc.message.ScNpdu;
import com.tridium.bacnet.stack.link.sc.message.SecurePathHeaderOption;
import com.tridium.bacnet.stack.link.sc.message.UnsupportedHeaderOption;
import com.tridium.bacnet.stack.network.DataAttribute;
import com.tridium.bacnet.stack.network.DataAttributes;
import com.tridium.bacnet.stack.network.NetworkPdu;
import java.security.cert.CRLException;
import java.security.cert.TrustAnchor;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.bacnet.datatypes.BBacnetOctetString;
import javax.baja.naming.BOrd;
import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraActions;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.util.ByteArrayUtil;
import javax.baja.spy.SpyWriter;
import javax.baja.sys.Action;
import javax.baja.sys.BFacets;
import javax.baja.sys.Context;
import javax.baja.sys.LocalizableException;
import javax.baja.sys.LocalizableRuntimeException;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "vmacAddress",
      type = "BBacnetOctetString",
      defaultValue = "BBacnetOctetString.DEFAULT",
      flags = 65
   ), @NiagaraProperty(
      name = "config",
      type = "BScConfiguration",
      defaultValue = "new BScConfiguration()"
   ), @NiagaraProperty(
      name = "credentials",
      type = "BScCredentials",
      defaultValue = "new BScCredentials()",
      facets = {@Facet(
         name = "BFacets.SECURITY",
         value = "true"
      )}
   ), @NiagaraProperty(
      name = "webSocketInitiator",
      type = "BAbstractScWebSocketInitiator",
      defaultValue = "new BJettyScWebSocketInitiator()"
   ), @NiagaraProperty(
      name = "nodeSwitch",
      type = "BNodeSwitch",
      defaultValue = "new BNodeSwitch()"
   ), @NiagaraProperty(
      name = "hubConnector",
      type = "BHubConnector",
      defaultValue = "new BHubConnector()"
   )})
@NiagaraActions({@NiagaraAction(
      name = "changeVmacAddress",
      parameterType = "BBacnetOctetString",
      defaultValue = "BBacnetOctetString.make(new byte[VMAC_LENGTH])",
      flags = 128
   ), @NiagaraAction(
      name = "regenerateVmacAddress",
      flags = 128
   )})
public final class BScLinkLayer extends BBacnetLinkLayer {
   public static final Property vmacAddress = newProperty(65, BBacnetOctetString.DEFAULT, null);
   public static final Property config = newProperty(0, new BScConfiguration(), null);
   public static final Property credentials = newProperty(0, new BScCredentials(), BFacets.make("security", true));
   public static final Property webSocketInitiator = newProperty(0, new BJettyScWebSocketInitiator(), null);
   public static final Property nodeSwitch = newProperty(0, new BNodeSwitch(), null);
   public static final Property hubConnector = newProperty(0, new BHubConnector(), null);
   public static final Action changeVmacAddress = newAction(128, BBacnetOctetString.make(new byte[6]), null);
   public static final Action regenerateVmacAddress = newAction(128, null);
   public static final Type TYPE = Sys.loadType(BScLinkLayer.class);
   private static final BHubAcceptingConnection[] EMPTY_HUB_ACCEPTING_CONNECTION_ARRAY = new BHubAcceptingConnection[0];
   static final Logger logger = Logger.getLogger("bacnet.sc.linkLayer");
   private final AtomicInteger messageId = new AtomicInteger();
   private final AtomicBoolean commStarted = new AtomicBoolean();
   private final Object initiatorRestartLock = new Object();

   public BBacnetOctetString getVmacAddress() {
      return (BBacnetOctetString)this.get(vmacAddress);
   }

   public void setVmacAddress(BBacnetOctetString v) {
      this.set(vmacAddress, v, null);
   }

   public BScConfiguration getConfig() {
      return (BScConfiguration)this.get(config);
   }

   public void setConfig(BScConfiguration v) {
      this.set(config, v, null);
   }

   public BScCredentials getCredentials() {
      return (BScCredentials)this.get(credentials);
   }

   public void setCredentials(BScCredentials v) {
      this.set(credentials, v, null);
   }

   public BAbstractScWebSocketInitiator getWebSocketInitiator() {
      return (BAbstractScWebSocketInitiator)this.get(webSocketInitiator);
   }

   public void setWebSocketInitiator(BAbstractScWebSocketInitiator v) {
      this.set(webSocketInitiator, v, null);
   }

   public BNodeSwitch getNodeSwitch() {
      return (BNodeSwitch)this.get(nodeSwitch);
   }

   public void setNodeSwitch(BNodeSwitch v) {
      this.set(nodeSwitch, v, null);
   }

   public BHubConnector getHubConnector() {
      return (BHubConnector)this.get(hubConnector);
   }

   public void setHubConnector(BHubConnector v) {
      this.set(hubConnector, v, null);
   }

   public void changeVmacAddress(BBacnetOctetString parameter) {
      this.invoke(changeVmacAddress, parameter, null);
   }

   public void regenerateVmacAddress() {
      this.invoke(regenerateVmacAddress, null, null);
   }

   @Override
   public Type getType() {
      return TYPE;
   }

   public void started() throws Exception {
      super.started();
      if (BBacnetOctetString.DEFAULT.equals(this.getVmacAddress())) {
         this.setVmacAddress(BBacnetOctetString.make(VmacUtil.regenerateVmacAddress()));
      }
   }

   public void stopped() throws Exception {
      super.stopped();
      if (Sys.getStation().isRunning()) {
         try {
            this.getNodeSwitch().doRemoveScUser();
         } catch (Exception var2) {
         }
      }
   }

   public BHubFunction getHubFunction() {
      BHubFunction[] hubs = (BHubFunction[])this.getChildren(BHubFunction.class);
      return hubs.length != 1 ? null : hubs[0];
   }

   BDirectInitiatingConnection[] getDirectInitiatingConnections() {
      return (BDirectInitiatingConnection[])this.getNodeSwitch().getConnections().getChildren(BDirectInitiatingConnection.class);
   }

   BDirectAcceptingConnection[] getDirectAcceptingConnections() {
      return (BDirectAcceptingConnection[])this.getNodeSwitch().getConnections().getChildren(BDirectAcceptingConnection.class);
   }

   BHubAcceptingConnection[] getHubFunctionConnections() {
      BHubFunction hubFunction = this.getHubFunction();
      return hubFunction == null
         ? EMPTY_HUB_ACCEPTING_CONNECTION_ARRAY
         : (BHubAcceptingConnection[])hubFunction.getConnections().getChildren(BHubAcceptingConnection.class);
   }

   void restartWebSocketInitiator() {
      if (this.isCommStarted()) {
         logger.fine("Gracefully disconnecting hub connector and direct initiating connections before restarting the web socket initiator");
         this.getHubConnector().linkCommStop();

         for (BInitiatingConnection initiatingConnection : this.getDirectInitiatingConnections()) {
            initiatingConnection.disconnect();
         }

         synchronized (this.initiatorRestartLock) {
            BAbstractScWebSocketInitiator initiator = this.getWebSocketInitiator();
            logger.fine("Calling linkCommStop on the web socket initiator as part of a restart.");
            initiator.linkCommStop();

            try {
               logger.fine("Calling linkCommStart on the web socket initiator and hub connector as part of a restart.");
               initiator.linkCommStart();
               this.getHubConnector().linkCommStart();
            } catch (Exception var6) {
               ScLinkLayerUtil.logException(logger, new StringBuilder("Failed to restart BACnet/SC link layer web socket initiator"), var6);
            }
         }
      }
   }

   public int getNextMessageId() {
      return this.messageId.updateAndGet(ScLinkLayerUtil::incrementUnsignedShort);
   }

   public long getLocalVmac() {
      long vmac = VmacUtil.octetStringToVmac(this.getVmacAddress());
      VmacUtil.checkIsDeviceVmac(vmac);
      return vmac;
   }

   public int getMaxBvlcLength() {
      int maxBvlcLength = this.getConfig().getNodeMaxBvlcLength();
      int minBvlcLength = ScMessageUtil.getMinBvlcLength(this.getMaxNpduLength());
      if (maxBvlcLength < minBvlcLength) {
         maxBvlcLength = minBvlcLength;
      }

      if (maxBvlcLength > 65535) {
         maxBvlcLength = 65535;
      }

      return maxBvlcLength;
   }

   public int getMaxNpduLength() {
      int maxNpduLength = this.getConfig().getNodeMaxNpduLength();
      if (maxNpduLength < 74) {
         return 74;
      } else {
         return maxNpduLength > 61327 ? 61327 : maxNpduLength;
      }
   }

   public void doChangeVmacAddress(BBacnetOctetString vmac) {
      if (!VmacUtil.isDeviceVmac(VmacUtil.octetStringToVmac(vmac))) {
         throw new LocalizableRuntimeException("bacnet", "changeVmacAddress.invalidVmac");
      } else {
         this.checkNetworkPortIsDisabled();
         this.setVmacAddress(vmac);
      }
   }

   public void doRegenerateVmacAddress() {
      this.checkNetworkPortIsDisabled();
      byte[] vmac = VmacUtil.regenerateVmacAddress();
      this.setVmacAddress(BBacnetOctetString.make(vmac));
   }

   private void checkNetworkPortIsDisabled() {
      if (this.getNetworkPort().getEnabled()) {
         throw new LocalizableRuntimeException("bacnet", "scLinkLayer.changeVmacAddress.portEnabled");
      }
   }

   public void checkNetworkPortIsEnabled() {
      if (!this.getNetworkPort().getEnabled()) {
         throw new LocalizableRuntimeException("bacnet", "scLinkLayer.portDisabled");
      }
   }

   public BOrd getParentSlotPathOrd() {
      return this.getNetworkPort().getSlotPathOrd();
   }

   public String toString(Context cx) {
      return ScLinkLayerUtil.LEXICON.getText("scLinkLayer.toString", cx, new Object[]{this.getVmacAddress().toString(cx)});
   }

   @Override
   public byte[] getMacAddress() {
      return this.getVmacAddress().getBytes();
   }

   @Override
   public int getMaxAPDULengthAccepted() {
      return this.getMaxNpduLength() - 24;
   }

   @Override
   public void linkCommInit() throws Exception {
      this.checkOperationalCert();
   }

   @Override
   public void linkCommStart() throws Exception {
      this.commStarted.set(true);

      try {
         this.getWebSocketInitiator().linkCommStart();
         BHubFunction hubFunction = this.getHubFunction();
         if (hubFunction != null) {
            hubFunction.linkCommStart();
         }

         this.getNodeSwitch().linkCommStart();
         this.getHubConnector().linkCommStart();
      } catch (Exception var2) {
         this.commStarted.set(false);
         throw var2;
      }
   }

   @Override
   public void linkCommStop() {
      if (this.commStarted.compareAndSet(true, false)) {
         this.getHubConnector().linkCommStop();
         this.getNodeSwitch().linkCommStop();
         BHubFunction hubFunction = this.getHubFunction();
         if (hubFunction != null) {
            hubFunction.linkCommStop();
         }

         this.getWebSocketInitiator().linkCommStop();
      }
   }

   @Override
   public void sendRequest(byte[] destAddress, NetworkPdu npdu) {
      long destinationVmac = destAddress != null && destAddress.length != 0 ? VmacUtil.bytesToVmac(destAddress) : 281474976710655L;
      if (!VmacUtil.isDestinationVmac(destinationVmac)) {
         logger.warning("Invalid BACnet/SC VMAC address [" + ByteArrayUtil.toHexString(destAddress) + ']');
      } else {
         try {
            ScNpdu scNpdu = ScNpdu.make(this.getNextMessageId(), npdu);
            addDataOptions(npdu.getDataAttributes(), scNpdu);
            this.getNodeSwitch().sendMessage(destinationVmac, scNpdu);
         } catch (Exception var6) {
            ScLinkLayerUtil.logException(logger, new StringBuilder("Exception while sending request to device [").append(destinationVmac).append(']'), var6);
         }
      }
   }

   public void rcvIndication(long originatingVmac, long destinationVmac, ScNpdu scNpdu) {
      byte[] rawNpdu = Objects.requireNonNull(scNpdu.getRawNpdu());
      boolean isBroadcast = destinationVmac == 281474976710655L;
      this.rcvIndication(
         VmacUtil.vmacToBytes(originatingVmac),
         isBroadcast ? null : VmacUtil.vmacToBytes(destinationVmac),
         BacnetInputStream.make(rawNpdu, 0, rawNpdu.length),
         isBroadcast,
         makeDataAttributes(scNpdu.getDataOptions())
      );
   }

   private static DataAttributes makeDataAttributes(List<HeaderOption> dataOptions) {
      if (dataOptions.isEmpty()) {
         return null;
      } else {
         List<DataAttribute> attributes = new ArrayList<>(dataOptions.size());

         for (HeaderOption option : dataOptions) {
            if (option instanceof DataAttribute) {
               attributes.add((DataAttribute)option);
            }
         }

         return new DataAttributes(attributes);
      }
   }

   private static void addDataOptions(DataAttributes dataAttributes, ScNpdu scNpdu) {
      if (dataAttributes == null) {
         addSecurePathOption(scNpdu);
      } else {
         List<DataAttribute> attributes = dataAttributes.getAttributes();
         if (attributes.isEmpty()) {
            addSecurePathOption(scNpdu);
         } else {
            boolean containsSecurePath = false;

            for (DataAttribute attribute : attributes) {
               if (attribute instanceof SecurePathHeaderOption) {
                  scNpdu.addDataOption(SecurePathHeaderOption.make());
                  containsSecurePath = true;
               } else if (attribute instanceof ProprietaryHeaderOption) {
                  scNpdu.addDataOption(ProprietaryHeaderOption.copy((ProprietaryHeaderOption)attribute));
               } else if (attribute instanceof UnsupportedHeaderOption) {
                  scNpdu.addDataOption(UnsupportedHeaderOption.copy((UnsupportedHeaderOption)attribute));
               }
            }

            if (!containsSecurePath) {
               addSecurePathOption(scNpdu);
            }
         }
      }
   }

   private static void addSecurePathOption(ScNpdu scNpdu) {
      if (!scNpdu.getNpdu().isSNET()) {
         for (HeaderOption option : scNpdu.getDataOptions()) {
            if (option instanceof SecurePathHeaderOption) {
               return;
            }
         }

         scNpdu.addDataOption(SecurePathHeaderOption.make());
      }
   }

   private void checkOperationalCert() throws Exception {
      String certAlias = this.getCredentials().getOperationalCertificateAliasAndPassword().getAlias().trim();
      if (certAlias.isEmpty()) {
         throw new LocalizableException("bacnet", "scLinkLayer.noOperationalCertificateSelected");
      }
   }

   public void spy(SpyWriter out) throws Exception {
      if (Sys.isStation()) {
         out.startProps();
         out.trTitle("BACnet/SC Link Layer", 2);
         out.prop("Message ID Counter", this.messageId.get());
         out.endProps();
      }

      super.spy(out);
   }

   public boolean isCommStarted() {
      return this.commStarted.get();
   }

   public boolean hasAssociatedUser() {
      for (BBacnetScAuthenticator scAuthenticator : BBacnetScAuthenticationScheme.findScAuthenticators()) {
         if (scAuthenticator.isAssociatedWithLinkLayer(this)) {
            return true;
         }
      }

      return false;
   }

   public void addTrustAnchors(Set<TrustAnchor> trustAnchors) {
      addIssuingCert(this.getCredentials().getIssuerCertificate1(), trustAnchors);
      addIssuingCert(this.getCredentials().getIssuerCertificate2(), trustAnchors);
   }

   private static void addIssuingCert(BIssuerCertAndCrl certAndCrl, Set<TrustAnchor> trustAnchors) {
      X509Certificate issuingCert = certAndCrl.getIssuerCertificate().getX509Certificate();
      if (issuingCert != null) {
         trustAnchors.add(new TrustAnchor(issuingCert, null));
      }
   }

   public void addCRLs(Set<X509CRL> crls) throws CRLException {
      BScCredentials credentials = this.getCredentials();
      credentials.getIssuerCertificate1().getCrl().ifPresent(crls::add);
      credentials.getIssuerCertificate2().getCrl().ifPresent(crls::add);
   }

   public void trustAnchorsUpdated() {
      if (logger.isLoggable(Level.FINE)) {
         logger.fine("Trust anchors in the SC port " + this.getParent().getName() + " have been updated");
      }

      this.updateWebSocketAcceptorTrustAnchors();
      this.restartWebSocketInitiator();
   }

   private void updateWebSocketAcceptorTrustAnchors() {
      if (this.hasAssociatedUser()) {
         logger.fine("Sending trustAnchorsUpdated to BacnetScAuthenticationScheme");
         BBacnetScAuthenticationScheme.trustAnchorsUpdated();
      }
   }

   public void setTrustAnchorFault(String faultCause) {
      this.getNodeSwitch().setTrustAnchorFault(faultCause);
      BHubFunction hubFunction = this.getHubFunction();
      if (hubFunction != null) {
         hubFunction.setTrustAnchorFault(faultCause);
      }
   }

   public void updateConnectionManagersStatus() {
      this.getNodeSwitch().updateStatus();
      BHubFunction hubFunction = this.getHubFunction();
      if (hubFunction != null) {
         hubFunction.updateStatus();
      }
   }
}
