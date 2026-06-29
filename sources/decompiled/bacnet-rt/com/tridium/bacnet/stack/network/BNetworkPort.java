package com.tridium.bacnet.stack.network;

import com.tridium.bacnet.stack.BBacnetMultiPoll;
import com.tridium.bacnet.stack.BBacnetPoll;
import com.tridium.bacnet.stack.BacnetInputStream;
import com.tridium.bacnet.stack.BacnetStackException;
import com.tridium.bacnet.stack.link.BBacnetLinkLayer;
import com.tridium.bacnet.stack.link.BLinkLayerChoice;
import com.tridium.bacnet.stack.link.LinkListener;
import com.tridium.bacnet.stack.link.ethernet.BBacnetEthernetLinkLayer;
import com.tridium.bacnet.stack.link.ip.BBacnetIpLinkLayer;
import com.tridium.bacnet.stack.link.mstp.BBacnetMstpLinkLayer;
import com.tridium.bacnet.stack.link.sc.BScLinkLayer;
import com.tridium.bacnet.stack.network.messages.NetworkNumberIs;
import com.tridium.bacnet.stack.network.wiretap.IncomingWiretap;
import com.tridium.bacnet.stack.network.wiretap.OutgoingWiretap;
import com.tridium.bacnet.stack.network.wiretap.WiretapAware;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.bacnet.BBacnetNetwork;
import javax.baja.bacnet.BacnetConst;
import javax.baja.bacnet.datatypes.BBacnetAddress;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraActions;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.spy.SpyWriter;
import javax.baja.status.BStatus;
import javax.baja.sys.Action;
import javax.baja.sys.BBoolean;
import javax.baja.sys.BComponent;
import javax.baja.sys.BValue;
import javax.baja.sys.BajaRuntimeException;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.util.QueueFullException;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "networkNumber",
      type = "int",
      defaultValue = "-1"
   ), @NiagaraProperty(
      name = "link",
      type = "BBacnetLinkLayer",
      defaultValue = "new BBacnetEthernetLinkLayer()"
   ), @NiagaraProperty(
      name = "status",
      type = "BStatus",
      defaultValue = "BStatus.ok",
      flags = 67
   ), @NiagaraProperty(
      name = "faultCause",
      type = "String",
      defaultValue = "",
      flags = 3
   ), @NiagaraProperty(
      name = "multiPoll",
      type = "boolean",
      defaultValue = "true",
      flags = 4
   ), @NiagaraProperty(
      name = "pollService",
      type = "BBacnetPoll",
      defaultValue = "new BBacnetMultiPoll()"
   ), @NiagaraProperty(
      name = "maxDevices",
      type = "int",
      defaultValue = "Integer.MAX_VALUE",
      flags = 1
   ), @NiagaraProperty(
      name = "enabled",
      type = "boolean",
      defaultValue = "false",
      flags = 64
   ), @NiagaraProperty(
      name = "portId",
      type = "int",
      defaultValue = "-1",
      flags = 65
   ), @NiagaraProperty(
      name = "portInfo",
      type = "String",
      defaultValue = "",
      flags = 1
   )})
@NiagaraActions({@NiagaraAction(
      name = "enable"
   ), @NiagaraAction(
      name = "disable"
   )})
public class BNetworkPort extends BComponent implements LinkListener, BacnetConst, WiretapAware {
   public static final Property networkNumber = newProperty(0, -1, null);
   public static final Property link = newProperty(0, new BBacnetEthernetLinkLayer(), null);
   public static final Property status = newProperty(67, BStatus.ok, null);
   public static final Property faultCause = newProperty(3, "", null);
   public static final Property multiPoll = newProperty(4, true, null);
   public static final Property pollService = newProperty(0, new BBacnetMultiPoll(), null);
   public static final Property maxDevices = newProperty(1, Integer.MAX_VALUE, null);
   public static final Property enabled = newProperty(64, false, null);
   public static final Property portId = newProperty(65, -1, null);
   public static final Property portInfo = newProperty(1, "", null);
   public static final Action enable = newAction(0, null);
   public static final Action disable = newAction(0, null);
   public static final Type TYPE = Sys.loadType(BNetworkPort.class);
   private boolean fatalFault = false;
   private boolean fault = false;
   protected static final Logger logger = Logger.getLogger("bacnet.network");
   private Set<OutgoingWiretap> wiretaps = Collections.synchronizedSet(new HashSet<>());
   private static int portIds = 1;
   private volatile BBacnetAddress address = null;
   private boolean ready = false;
   private int oldDnet = -1;
   private int oldStatus = 0;

   public int getNetworkNumber() {
      return this.getInt(networkNumber);
   }

   public void setNetworkNumber(int v) {
      this.setInt(networkNumber, v, null);
   }

   public BBacnetLinkLayer getLink() {
      return (BBacnetLinkLayer)this.get(link);
   }

   public void setLink(BBacnetLinkLayer v) {
      this.set(link, v, null);
   }

   public BStatus getStatus() {
      return (BStatus)this.get(status);
   }

   public void setStatus(BStatus v) {
      this.set(status, v, null);
   }

   public String getFaultCause() {
      return this.getString(faultCause);
   }

   public void setFaultCause(String v) {
      this.setString(faultCause, v, null);
   }

   public boolean getMultiPoll() {
      return this.getBoolean(multiPoll);
   }

   public void setMultiPoll(boolean v) {
      this.setBoolean(multiPoll, v, null);
   }

   public BBacnetPoll getPollService() {
      return (BBacnetPoll)this.get(pollService);
   }

   public void setPollService(BBacnetPoll v) {
      this.set(pollService, v, null);
   }

   public int getMaxDevices() {
      return this.getInt(maxDevices);
   }

   public void setMaxDevices(int v) {
      this.setInt(maxDevices, v, null);
   }

   public boolean getEnabled() {
      return this.getBoolean(enabled);
   }

   public void setEnabled(boolean v) {
      this.setBoolean(enabled, v, null);
   }

   public int getPortId() {
      return this.getInt(portId);
   }

   public void setPortId(int v) {
      this.setInt(portId, v, null);
   }

   public String getPortInfo() {
      return this.getString(portInfo);
   }

   public void setPortInfo(String v) {
      this.setString(portInfo, v, null);
   }

   public void enable() {
      this.invoke(enable, null, null);
   }

   public void disable() {
      this.invoke(disable, null, null);
   }

   public Type getType() {
      return TYPE;
   }

   public static BNetworkPort make(int networkNumber, BLinkLayerChoice linkLayer) {
      BNetworkPort port = new BNetworkPort();
      port.setNetworkNumber(networkNumber);
      switch (linkLayer.getOrdinal()) {
         case 0:
            port.setLink(new BBacnetIpLinkLayer());
            port.setPortInfo("Annex J IP");
            return port;
         case 1:
            port.setLink(new BBacnetEthernetLinkLayer());
            port.setPortInfo("Ethernet");
            return port;
         case 2:
         case 4:
         case 5:
         case 6:
         default:
            throw new IllegalArgumentException("Unsupported Link Layer Choice!");
         case 3:
            port.setLink(new BBacnetMstpLinkLayer());
            port.setPortInfo("MS/TP");
            return port;
         case 7:
            port.setLink(new BScLinkLayer());
            port.setPortInfo("Secure Connect");
            return port;
      }
   }

   public void doEnable() {
      if (this.isRunning()) {
         if (!this.fatalFault) {
            try {
               this.getLink().linkInit();
               this.getLink().addLinkListener(this);
               this.getLink().linkStart();
               this.network()
                  .updateRouterAddress(this.getNetworkNumber(), this.getNetworkNumber(), this.getLink().getMacAddress(), BRouterStatus.ok, this.getPortInfo());
               this.getPollService().pollStart();
               this.network().issueIAmRouterToNetworks();
               this.sendToLink(null, new NetworkNumberIs(this.getNetworkNumber()));
               BBacnetNetwork.localDevice().sendIAm();
               this.set(enabled, BBoolean.TRUE, noWrite);
               this.ok();
            } catch (BajaRuntimeException var6) {
               logger.log(Level.WARNING, "Exception enabling BACnet network port: " + this + ":" + var6, (Throwable)var6);
               this.fault(var6.getMessage() != null ? var6.getMessage() : "Null Exception Cause");
            } catch (Exception var7) {
               logger.log(Level.WARNING, "Cannot enable BACnet network port: " + this + ":" + var7, (Throwable)var7);
               this.fault(var7.toString());
            } finally {
               this.updateStatus();
            }
         }
      }
   }

   public void doDisable() {
      if (this.isRunning()) {
         try {
            this.set(enabled, BBoolean.FALSE, noWrite);
            this.getPollService().pollStop();
            this.network().getRouterTable().removeRouterByPortId(this.getPortId());
            this.getLink().removeLinkListener(this);
            this.getLink().linkStop();
            this.getLink().linkCleanup();
         } catch (Exception var5) {
            logger.warning("Exception disabling BACnet network port: " + this + ":" + var5);
         } finally {
            this.updateStatus();
         }
      }
   }

   public void started() throws Exception {
      super.started();
      this.checkFatalFault();
      Type pollServiceType = this.getPollService().getType();
      if (!this.getMultiPoll() && !this.getLink().getType().is(BBacnetMstpLinkLayer.TYPE)) {
         if (pollServiceType.is(BBacnetMultiPoll.TYPE)) {
            this.setPollService(new BBacnetPoll());
         }
      } else if (!pollServiceType.is(BBacnetMultiPoll.TYPE)) {
         this.setPollService(new BBacnetMultiPoll());
      }

      this.setPortId(portIds++);
      this.oldDnet = this.getNetworkNumber();
      if (Sys.atSteadyState()) {
         this.ready = true;
      }

      if (!this.fatalFault && this.getEnabled()) {
         this.getLink().linkInit();
      }
   }

   public void added(Property p, Context cx) {
      super.added(p, cx);
      Object o = null;
      if ((o = this.get(p)) instanceof IncomingWiretap) {
         this.getLink().addLinkListener((IncomingWiretap)o);
      }

      if (o instanceof OutgoingWiretap) {
         this.wiretaps.add((OutgoingWiretap)o);
      }
   }

   public void removed(Property p, BValue oldValue, Context cx) {
      super.removed(p, oldValue, cx);
      if (oldValue instanceof IncomingWiretap) {
         this.getLink().removeLinkListener((IncomingWiretap)oldValue);
      }

      if (oldValue instanceof OutgoingWiretap) {
         this.wiretaps.remove((OutgoingWiretap)oldValue);
      }
   }

   public void stopped() {
      this.stackStopped();
   }

   public void stackStopped() {
      try {
         if (this.getEnabled()) {
            this.getPollService().pollStop();
            this.network().getRouterTable().removeRouterByPortId(this.getPortId());
            this.getLink().removeLinkListener(this);
            this.getLink().linkStop();
            this.getLink().linkCleanup();
         }
      } catch (Exception var2) {
         logger.log(Level.SEVERE, "Failed to stop NetworkPort", (Throwable)var2);
      }
   }

   public void changed(Property p, Context cx) {
      super.changed(p, cx);
      if (this.isRunning()) {
         if (p.equals(networkNumber)) {
            if (this.getNetworkNumber() == 0) {
               this.fault("Invalid network number:" + this.getNetworkNumber());
            } else if (this.network().getPortByNetwork(this.getNetworkNumber()) != this) {
               logger.severe("Duplicate network number! Resetting to old DNET " + this.oldDnet + "...");
               this.setInt(networkNumber, this.oldDnet, fallback);
            } else {
               this.network().getRouterTable().removeRouterByDnet(this.oldDnet);
               if (this.getEnabled() && this.getNetworkNumber() > 0) {
                  this.network()
                     .updateRouterAddress(
                        this.getNetworkNumber(), this.getNetworkNumber(), this.getLink().getMacAddress(), BRouterStatus.ok, this.getPortInfo()
                     );
               }

               this.oldDnet = this.getNetworkNumber();
               this.ok();
            }
         } else if (p.equals(enabled) && cx != noWrite) {
            if (this.getEnabled()) {
               this.enable();
            } else {
               this.disable();
            }
         }
      }
   }

   public final boolean isParentLegal(BComponent parent) {
      return parent instanceof BBacnetNetworkLayer;
   }

   public void sendToLink(byte[] destAddress, NetworkPdu npdu) {
      if (this.ready && this.getStatus().isValid()) {
         this.getLink().sendRequest(destAddress, npdu);

         for (OutgoingWiretap tap : this.wiretaps) {
            try {
               tap.sendRequest(destAddress, npdu);
            } catch (Exception var6) {
            }
         }
      }
   }

   @Override
   public void rcvIndication(byte[] srcMacAddress, byte[] destMacAddress, BacnetInputStream is, boolean isBroadcast) {
      this.rcvIndication(srcMacAddress, destMacAddress, is, isBroadcast, null);
   }

   @Override
   public void rcvIndication(byte[] srcMacAddress, byte[] destMacAddress, BacnetInputStream is, boolean isBroadcast, DataAttributes dataAttributes) {
      try {
         NetworkPdu npdu = NetworkPdu.parseNetworkBytes(srcMacAddress, destMacAddress, this.getNetworkNumber(), is, isBroadcast);
         if (npdu != null && this.ready && this.getStatus().isValid()) {
            npdu.setSourcePort(this);
            npdu.setDataAttributes(dataAttributes);
            if (logger.isLoggable(Level.FINE)) {
               logger.fine(npdu.toString());
            }

            this.network().receiveNpdu(npdu);
         }
      } catch (BacnetStackException var7) {
         logger.log(Level.SEVERE, "Unable to parse Bacnet Npdu!", (Throwable)var7);
         is.release();
      } catch (QueueFullException var8) {
         logger.log(Level.SEVERE, "Network layer message queue overflow!", (Throwable)var8);
         is.release();
      }
   }

   public BBacnetAddress getAddress() {
      int networkNumber = this.getNetworkNumber();
      byte[] macAddress = this.getLink().getMacAddress();
      if (this.address == null || networkNumber != this.address.getNetworkNumber() || !this.address.macEquals(macAddress)) {
         this.address = new BBacnetAddress(networkNumber, macAddress);
      }

      return this.address;
   }

   void networkReady() {
      this.ready = true;

      try {
         if (!this.fatalFault && this.getEnabled()) {
            this.getLink().addLinkListener(this);
            this.getLink().linkStart();
            this.sendToLink(null, new NetworkNumberIs(this.getNetworkNumber()));
            this.network()
               .updateRouterAddress(this.getNetworkNumber(), this.getNetworkNumber(), this.getLink().getMacAddress(), BRouterStatus.ok, this.getPortInfo());
         }
      } catch (Exception var2) {
         this.fault(var2.toString());
      }

      this.updateStatus();
      if (this.getEnabled()) {
         this.getPollService().pollStart();
      }
   }

   private BBacnetNetworkLayer network() {
      return (BBacnetNetworkLayer)this.getParent();
   }

   public String toString(Context cx) {
      return "NetworkPort: id="
         + this.getPortId()
         + " net="
         + this.getNetworkNumber()
         + (this.getEnabled() ? " enabled" : " disabled")
         + " max="
         + this.getMaxDevices()
         + " link="
         + this.getLink();
   }

   public void spy(SpyWriter out) throws Exception {
      super.spy(out);
      out.startProps();
      out.trTitle("NetworkPort", 2);
      out.prop("portIds (static)", portIds);
      out.prop("ready", this.ready);
      out.prop("oldDnet", this.oldDnet);
      out.prop("fatalFault", this.fatalFault);
      out.prop("oldStatus", this.oldStatus);
      out.endProps();
   }

   public final boolean isFatalFault() {
      return this.fatalFault;
   }

   public final void fault(String cause) {
      this.fault = true;
      if (!this.fatalFault) {
         this.setFaultCause(cause);
         this.updateStatus();
      }
   }

   public final void ok() {
      this.fault = false;
      if (!this.fatalFault) {
         this.setFaultCause("");
         this.updateStatus();
      }
   }

   public final void fatalFault(String faultCause) {
      this.fatalFault = true;
      this.setFaultCause(faultCause);
      this.updateStatus();
   }

   public void updateStatus() {
      int newStatus = this.getStatus().getBits();
      if (!this.getEnabled()) {
         newStatus |= 1;
      } else {
         newStatus &= -2;
      }

      if (!this.fatalFault && !this.fault) {
         newStatus &= -3;
      } else {
         newStatus |= 2;
      }

      if (newStatus != this.oldStatus) {
         this.setStatus(BStatus.make(newStatus));
         this.oldStatus = newStatus;
      }
   }

   private void checkFatalFault() {
      this.getLink().checkFatalFault();
   }
}
