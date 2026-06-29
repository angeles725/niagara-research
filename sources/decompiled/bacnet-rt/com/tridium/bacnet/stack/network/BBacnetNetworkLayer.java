package com.tridium.bacnet.stack.network;

import com.tridium.bacnet.asn.AsnInputStream;
import com.tridium.bacnet.stack.BBacnetStack;
import com.tridium.bacnet.stack.link.BLinkLayerChoice;
import com.tridium.bacnet.stack.network.messages.ApplicationMsg;
import com.tridium.bacnet.stack.network.messages.DisconnectConnectionToNetwork;
import com.tridium.bacnet.stack.network.messages.EstablishConnectionToNetwork;
import com.tridium.bacnet.stack.network.messages.IAmRouterToNetwork;
import com.tridium.bacnet.stack.network.messages.ICouldBeRouterToNetwork;
import com.tridium.bacnet.stack.network.messages.InitializeRoutingTable;
import com.tridium.bacnet.stack.network.messages.InitializeRoutingTableAck;
import com.tridium.bacnet.stack.network.messages.NetworkLayerMsg;
import com.tridium.bacnet.stack.network.messages.NetworkNumberIs;
import com.tridium.bacnet.stack.network.messages.RejectMessageToNetwork;
import com.tridium.bacnet.stack.network.messages.RouterAvailableToNetwork;
import com.tridium.bacnet.stack.network.messages.RouterBusyToNetwork;
import com.tridium.bacnet.stack.network.messages.WhatIsNetworkNumber;
import com.tridium.bacnet.stack.network.messages.WhoIsRouterToNetwork;
import com.tridium.bacnet.stack.transport.ApplicationPdu;
import com.tridium.bacnet.stack.transport.BBacnetTransportLayer;
import com.tridium.bacnet.timers.TimerListener;
import com.tridium.bacnet.timers.Timers;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.bacnet.datatypes.BBacnetAddress;
import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraActions;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.spy.SpyWriter;
import javax.baja.sys.Action;
import javax.baja.sys.BComponent;
import javax.baja.sys.BFacets;
import javax.baja.sys.BRelTime;
import javax.baja.sys.Clock;
import javax.baja.sys.Property;
import javax.baja.sys.SlotCursor;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.units.UnitDatabase;
import javax.baja.util.Lexicon;
import javax.baja.util.QueueFullException;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "routerTable",
      type = "BBacnetRouterTable",
      defaultValue = "new BBacnetRouterTable()"
   ), @NiagaraProperty(
      name = "ipPort",
      type = "BNetworkPort",
      defaultValue = "BNetworkPort.make(1, BLinkLayerChoice.ip)",
      flags = 8
   ), @NiagaraProperty(
      name = "routingEnabled",
      type = "boolean",
      defaultValue = "true"
   ), @NiagaraProperty(
      name = "maintainRoutingEnabled",
      type = "boolean",
      defaultValue = "false"
   ), @NiagaraProperty(
      name = "minimumRouterUpdateTime",
      type = "int",
      defaultValue = "500",
      facets = {@Facet("BFacets.makeInt(UnitDatabase.getUnit(\"millisecond\"))")}
   ), @NiagaraProperty(
      name = "routerDiscoveryTimeout",
      type = "int",
      defaultValue = "5000",
      facets = {@Facet("BFacets.makeInt(UnitDatabase.getUnit(\"millisecond\"))")}
   ), @NiagaraProperty(
      name = "terminationTimeValue",
      type = "int",
      defaultValue = "120",
      facets = {@Facet("BFacets.makeInt(UnitDatabase.getUnit(\"second\"))")}
   )})
@NiagaraActions({@NiagaraAction(
      name = "sendNetworkNumberIs",
      flags = 4
   ), @NiagaraAction(
      name = "whatIsNetworkNumber",
      flags = 4
   )})
public class BBacnetNetworkLayer extends BComponent implements NetworkMsgType, TimerListener {
   public static final Property routerTable = newProperty(0, new BBacnetRouterTable(), null);
   public static final Property ipPort = newProperty(8, BNetworkPort.make(1, BLinkLayerChoice.ip), null);
   public static final Property routingEnabled = newProperty(0, true, null);
   public static final Property maintainRoutingEnabled = newProperty(0, false, null);
   public static final Property minimumRouterUpdateTime = newProperty(0, 500, BFacets.makeInt(UnitDatabase.getUnit("millisecond")));
   public static final Property routerDiscoveryTimeout = newProperty(0, 5000, BFacets.makeInt(UnitDatabase.getUnit("millisecond")));
   public static final Property terminationTimeValue = newProperty(0, 120, BFacets.makeInt(UnitDatabase.getUnit("second")));
   public static final Action sendNetworkNumberIs = newAction(4, null);
   public static final Action whatIsNetworkNumber = newAction(4, null);
   public static final Type TYPE = Sys.loadType(BBacnetNetworkLayer.class);
   static final Logger logger = Logger.getLogger("bacnet.network");
   static final Lexicon lex = Lexicon.make("bacnet");
   public static final int DNET_NOT_USED = -1;
   private BBacnetNetworkLayer.BacnetNetworkQueue myQueue;
   private HashMap<Integer, ArrayList<Integer>> pendingDNETs = new HashMap<>();
   private HashMap<Integer, Long> networkNumberUpdates = new HashMap<>();
   private static final int ALL_PORTS = -1;
   private BNetworkPort[] ports = null;

   public BBacnetRouterTable getRouterTable() {
      return (BBacnetRouterTable)this.get(routerTable);
   }

   public void setRouterTable(BBacnetRouterTable v) {
      this.set(routerTable, v, null);
   }

   public BNetworkPort getIpPort() {
      return (BNetworkPort)this.get(ipPort);
   }

   public void setIpPort(BNetworkPort v) {
      this.set(ipPort, v, null);
   }

   public boolean getRoutingEnabled() {
      return this.getBoolean(routingEnabled);
   }

   public void setRoutingEnabled(boolean v) {
      this.setBoolean(routingEnabled, v, null);
   }

   public boolean getMaintainRoutingEnabled() {
      return this.getBoolean(maintainRoutingEnabled);
   }

   public void setMaintainRoutingEnabled(boolean v) {
      this.setBoolean(maintainRoutingEnabled, v, null);
   }

   public int getMinimumRouterUpdateTime() {
      return this.getInt(minimumRouterUpdateTime);
   }

   public void setMinimumRouterUpdateTime(int v) {
      this.setInt(minimumRouterUpdateTime, v, null);
   }

   public int getRouterDiscoveryTimeout() {
      return this.getInt(routerDiscoveryTimeout);
   }

   public void setRouterDiscoveryTimeout(int v) {
      this.setInt(routerDiscoveryTimeout, v, null);
   }

   public int getTerminationTimeValue() {
      return this.getInt(terminationTimeValue);
   }

   public void setTerminationTimeValue(int v) {
      this.setInt(terminationTimeValue, v, null);
   }

   public void sendNetworkNumberIs() {
      this.invoke(sendNetworkNumberIs, null, null);
   }

   public void whatIsNetworkNumber() {
      this.invoke(whatIsNetworkNumber, null, null);
   }

   public Type getType() {
      return TYPE;
   }

   public void started() {
      this.myQueue = new BBacnetNetworkLayer.BacnetNetworkQueue(this);
      this.myQueue.start("BnNet");
   }

   public void descendantsStarted() {
      this.initializeAsnInputPool();
   }

   public void stackStopped() {
      this.myQueue.stop();
      this.myQueue = null;
      SlotCursor<Property> sc = this.getProperties();

      while (sc.next(BNetworkPort.class)) {
         ((BNetworkPort)sc.get()).stackStopped();
      }
   }

   public final boolean isParentLegal(BComponent parent) {
      return parent instanceof BBacnetStack;
   }

   public void initializeAsnInputPool() {
      BNetworkPort[] ports = this.getPorts();
      int numThds = 0;

      for (int i = 0; i < ports.length; i++) {
         numThds += ports[i].getPollService().getNumberOfThreads();
      }

      AsnInputStream.setPoolSize(numThds + 5);
   }

   public BBacnetAddress getAddress(int networkNumber) {
      BNetworkPort[] ports = this.getPorts();

      for (int i = 0; i < ports.length; i++) {
         if (ports[i].getNetworkNumber() == networkNumber) {
            return ports[i].getAddress();
         }
      }

      return null;
   }

   public BNetworkPort getPortByNetwork(int networkNumber) {
      BNetworkPort ret = null;
      boolean found = false;
      BNetworkPort[] ports = this.getPorts();

      for (int i = 0; i < ports.length; i++) {
         if (ports[i].getNetworkNumber() == networkNumber) {
            if (found) {
               logger.severe("BACnet network layer misconfiguration detected: two network ports with the same network number!");
               return null;
            }

            ret = ports[i];
            found = true;
         }
      }

      return ret;
   }

   public BNetworkPort getPortByDNET(int dnet) {
      BNetworkPort ret = null;
      BBacnetRouterEntry router = this.getRouter(dnet);
      if (router != null) {
         ret = this.getPortById(router.getPortId());
      }

      return ret;
   }

   public BNetworkPort getPortById(int portId) {
      BNetworkPort[] ports = this.getPorts();

      for (int i = 0; i < ports.length; i++) {
         if (ports[i].getPortId() == portId) {
            return ports[i];
         }
      }

      return null;
   }

   public boolean isDirectlyConnectedNetwork(int networkNumber) {
      BNetworkPort np = this.getPortByNetwork(networkNumber);
      return np != null ? np.getStatus().isOk() : false;
   }

   public boolean isEnabled() {
      this.loadSlots();
      BNetworkPort[] ports = this.getPorts();

      for (int i = 0; i < ports.length; i++) {
         ports[i].loadSlots();
         if (ports[i].getStatus().isOk()) {
            return true;
         }
      }

      return false;
   }

   public void networkReady() {
      BNetworkPort[] ports = this.getPorts();

      for (int i = 0; i < ports.length; i++) {
         ports[i].networkReady();
      }

      if (this.getRoutingEnabled()) {
         this.issueIAmRouterToNetworks();
      }

      this.issueWhoIsRouterToNetwork(-1);
   }

   public int getQueueSize() {
      return this.myQueue.size();
   }

   public BRelTime getRouterEntryLifetime() {
      BRelTime life = BRelTime.DAY;

      try {
         BRelTime reLife = (BRelTime)this.get("routerEntryLifetime");
         if (reLife != null) {
            life = reLife;
         }
      } catch (Exception var3) {
      }

      if (life.getMinutes() < 5) {
         life = BRelTime.makeMinutes(5);
      }

      return life;
   }

   void receiveNpdu(NetworkPdu npdu) throws QueueFullException {
      this.myQueue.enqueue(npdu);
   }

   protected void process(NetworkPdu npdu) {
      if (npdu != null) {
         if (!this.validateNpdu(npdu)) {
            logger.severe("Received Npdu indicates network misconfiguration!!!\n" + npdu + "\n");
            if (!this.getMaintainRoutingEnabled()) {
               this.setRoutingEnabled(false);
            }
         } else {
            if (npdu.isNetworkLayerMsg()) {
               this.receiveNetworkLayerMessage((NetworkLayerMsg)npdu);
            } else {
               this.receiveAPDU((ApplicationMsg)npdu);
            }
         }
      }
   }

   public void sendRequest(BBacnetAddress destAddress, ApplicationPdu apdu) {
      byte[] macAddress = destAddress.getMacAddress().getBytes();
      int networkNumber = destAddress.getNetworkNumber();
      ApplicationMsg appMsg;
      if (networkNumber != 0 && !this.isDirectlyConnectedNetwork(networkNumber)) {
         appMsg = new ApplicationMsg(destAddress, null, apdu, apdu.getPriority(), apdu.getDataExpectingReply());
         if (networkNumber == 65535) {
            macAddress = null;
         } else {
            BBacnetRouterEntry router = this.getRouter(networkNumber);
            if (router == null) {
               if (logger.isLoggable(Level.FINE)) {
                  logger.fine("Unknown network number: " + networkNumber + ", attempting to discover router...");
               }

               this.issueWhoIsRouterToNetwork(networkNumber);
               int timerId = appMsg.startTimer(this);
               this.addPendingDNET(appMsg.getDNET(), timerId);
               return;
            }

            networkNumber = router.getRouterAddress().getNetworkNumber();
            macAddress = router.getRouterAddress().getMacAddress().getBytes();
         }
      } else {
         appMsg = new ApplicationMsg(null, null, apdu, apdu.getPriority(), apdu.getDataExpectingReply());
      }

      if (logger.isLoggable(Level.FINE)) {
         logger.fine(appMsg.toString());
      }

      this.sendToPort(networkNumber, macAddress, appMsg);
   }

   private void receiveAPDU(ApplicationMsg appMsg) {
      if (appMsg.isSNET()) {
         this.updateRouterAddress(appMsg.getSNET(), appMsg.getSrcNetworkNumber(), appMsg.getSrcMacAddress(), BRouterStatus.ok);
      }

      if (!appMsg.isDNET()) {
         this.transport()
            .receiveIndication(
               appMsg.getSrcAddress(),
               appMsg.getDestAddress(),
               appMsg.getRawAPDU(),
               appMsg.getNetworkPriority(),
               appMsg.getDataExpectingReply(),
               appMsg.isBroadcast()
            );
      } else {
         appMsg.storeApdu();
         if (appMsg.getDNET() == 65535) {
            this.transport()
               .receiveIndication(
                  appMsg.getSrcAddress(),
                  appMsg.getDestAddress(),
                  appMsg.getRawAPDU(),
                  appMsg.getNetworkPriority(),
                  appMsg.getDataExpectingReply(),
                  appMsg.isBroadcast()
               );
         } else {
            appMsg.getRawAPDU().release();
         }

         this.receiveRemoteTraffic(appMsg);
      }
   }

   private void receiveNetworkLayerMessage(NetworkLayerMsg netMsg) {
      if (logger.isLoggable(Level.FINE)) {
         logger.fine("Network layer message received from network " + netMsg.getSrcNetworkNumber() + ":\n " + netMsg);
      }

      switch (netMsg.getMessageType()) {
         case 0:
            this.processWhoIsRouterToNetwork((WhoIsRouterToNetwork)netMsg);
            break;
         case 1:
            this.processIAmRouterToNetwork((IAmRouterToNetwork)netMsg);
            break;
         case 2:
            this.processICouldBeRouterToNetwork((ICouldBeRouterToNetwork)netMsg);
            break;
         case 3:
            this.processRejectMessageToNetwork((RejectMessageToNetwork)netMsg);
            break;
         case 4:
            this.processRouterBusyToNetwork((RouterBusyToNetwork)netMsg);
            break;
         case 5:
            this.processRouterAvailableToNetwork((RouterAvailableToNetwork)netMsg);
            break;
         case 6:
            if (this.isRouterActive()) {
               this.processInitializeRoutingTable((InitializeRoutingTable)netMsg);
            }
            break;
         case 7:
            this.processInitializeRoutingTableAck((InitializeRoutingTableAck)netMsg);
            break;
         case 8:
            if (this.isRouterActive()) {
               this.processEstablishConnectionToNetwork((EstablishConnectionToNetwork)netMsg);
            }
            break;
         case 9:
            if (this.isRouterActive()) {
               this.processDisconnectConnectionToNetwork((DisconnectConnectionToNetwork)netMsg);
            }
            break;
         case 10:
         case 11:
         case 12:
         case 13:
         case 14:
         case 15:
         case 16:
         case 17:
         default:
            this.processProprietaryNetworkMessage(netMsg);
            break;
         case 18:
            this.processWhatIsNetworkNumber((WhatIsNetworkNumber)netMsg);
            break;
         case 19:
            this.processNetworkNumberIs((NetworkNumberIs)netMsg);
      }

      if (netMsg.isDNET()) {
         this.receiveRemoteTraffic(netMsg);
      }
   }

   private void receiveRemoteTraffic(NetworkPdu npdu) {
      if (this.getRoutingEnabled()) {
         if (!npdu.isSNET()) {
            npdu.setSADR(npdu.getSrcMacAddress());
            npdu.setSNET(npdu.getSrcNetworkNumber());
         }

         if (this.isDirectlyConnectedNetwork(npdu.getDNET())) {
            logger.fine("Routing message to directly connected network...");
            byte[] macAddress = npdu.getDADR();
            int networkNumber = npdu.getDNET();
            npdu.setDNET(-1);
            this.sendToPort(networkNumber, macAddress, npdu);
         } else if (npdu.getHopCount() != 0) {
            npdu.decrementHopCount();
            if (npdu.getHopCount() != 0) {
               if (npdu.getDNET() == 65535) {
                  BNetworkPort[] ports = this.getPorts();

                  for (int i = 0; i < ports.length; i++) {
                     if (ports[i] != npdu.getSourcePort()) {
                        ports[i].sendToLink(npdu.getDADR(), npdu);
                     }
                  }
               } else {
                  this.sendRemoteTraffic(npdu);
               }
            }
         }
      }
   }

   private void sendRemoteTraffic(NetworkPdu npdu) {
      int dnet = npdu.getDNET();
      if (dnet > 0) {
         BBacnetRouterEntry router = this.getRouter(dnet, true);
         if (logger.isLoggable(Level.FINE)) {
            logger.fine("Sending remote traffic: router=" + router);
         }

         if (router == null) {
            if (logger.isLoggable(Level.FINE)) {
               logger.fine("Unknown network number: " + dnet + ", attempting to discover router...");
            }

            this.issueWhoIsRouterToNetwork(dnet, npdu.getSrcNetworkNumber());
            int timerId = npdu.startTimer(this);
            this.addPendingDNET(dnet, timerId);
         } else if (router.getRouterAddress().getNetworkNumber() != npdu.getSrcNetworkNumber()) {
            switch (router.getRouterStatus().getOrdinal()) {
               case 0:
                  this.sendToPort(
                     router.getRouterAddress().getNetworkNumber(),
                     router.getRouterAddress().getMacAddress().getBytes(),
                     npdu,
                     npdu.getNetworkPriority(),
                     npdu.getDataExpectingReply()
                  );
                  break;
               case 1:
               case 3:
               case 4:
                  this.sendToPort(npdu.getSrcNetworkNumber(), npdu.getSrcMacAddress(), new RejectMessageToNetwork(1, dnet), npdu.getNetworkPriority(), false);
                  break;
               case 2:
                  this.sendToPort(npdu.getSrcNetworkNumber(), npdu.getSrcMacAddress(), new RejectMessageToNetwork(2, dnet), npdu.getNetworkPriority(), false);
                  break;
               default:
                  logger.warning("receiveRemoteTraffic:Unknown router status " + router.getRouterStatus() + " for router " + router);
            }
         }
      }
   }

   private void processWhoIsRouterToNetwork(WhoIsRouterToNetwork msg) {
      if (this.getRoutingEnabled()) {
         int snetNum = msg.getSrcNetworkNumber();
         if (msg.isNetworkNumber()) {
            int dnet = msg.getNetworkNumber();
            if (dnet == 0) {
               return;
            }

            if (this.isDirectlyConnectedNetwork(dnet)) {
               if (dnet != snetNum) {
                  this.sendToPort(snetNum, null, new IAmRouterToNetwork(dnet), msg.getNetworkPriority(), false);
               }
            } else {
               BBacnetRouterEntry router = this.getRouter(dnet, false);
               if (router != null) {
                  if (router.getPortId() != msg.getSourcePort().getPortId()) {
                     this.sendToPort(snetNum, null, new IAmRouterToNetwork(dnet), msg.getNetworkPriority(), false);
                  }
               } else {
                  if (!msg.isSNET()) {
                     msg.setSADR(msg.getSrcMacAddress());
                     msg.setSNET(msg.getSrcNetworkNumber());
                  }

                  BNetworkPort[] ports = this.getPorts();

                  for (int i = 0; i < ports.length; i++) {
                     if (ports[i] != msg.getSourcePort()) {
                        ports[i].sendToLink(null, msg);
                     }
                  }
               }
            }
         } else {
            int[] dnets = this.getRouterTable().getDnets(msg.getSourcePort().getPortId());
            if (dnets != null && dnets.length > 0) {
               this.sendToPort(snetNum, null, new IAmRouterToNetwork(dnets), msg.getNetworkPriority(), false);
            }
         }
      }
   }

   private void processIAmRouterToNetwork(IAmRouterToNetwork msg) {
      int[] dnets = msg.getNetworkNumbers();
      boolean msgOK = true;

      for (int i = 0; i < dnets.length; i++) {
         if (!this.checkNetworkConfig(dnets[i], true)) {
            msgOK = false;
         }
      }

      if (!msgOK) {
         if (!this.getMaintainRoutingEnabled()) {
            logger.severe("Disabling Bacnet Router functionality until network configuration is fixed!");
            this.setRoutingEnabled(false);
         }
      } else {
         this.updateRouterAddress(dnets, msg.getSrcNetworkNumber(), msg.getSrcMacAddress(), BRouterStatus.ok);

         for (int ix = 0; ix < dnets.length; ix++) {
            this.processPendingNpdus(dnets[ix]);
         }

         if (this.getRoutingEnabled()) {
            if (dnets.length != 0) {
               BNetworkPort[] ports = this.getPorts();

               for (int ix = 0; ix < ports.length; ix++) {
                  if (ports[ix].getStatus().isOk() && ports[ix].getNetworkNumber() != msg.getSrcNetworkNumber()) {
                     ports[ix].sendToLink(null, msg);
                  }
               }
            }
         }
      }
   }

   private void processICouldBeRouterToNetwork(ICouldBeRouterToNetwork msg) {
      this.updateRouterAddress(msg.getNetworkNumber(), msg.getSrcNetworkNumber(), msg.getSrcMacAddress(), BRouterStatus.routerNotConnected);
   }

   private void processRejectMessageToNetwork(RejectMessageToNetwork msg) {
      int reason = msg.getRejectReason();
      if (reason != 3) {
         logger.severe(msg.toString());
         if (reason == 1) {
            BBacnetRouterEntry re = this.getRouterTable().getRouterByDnet(msg.getRejectedDNET());
            if (re != null) {
               re.setRouterStatus(BRouterStatus.routerUnavailable);
            }
         } else if (reason == 2) {
            BBacnetRouterEntry re = this.getRouterTable().getRouterByDnet(msg.getRejectedDNET());
            if (re != null) {
               re.setRouterStatus(BRouterStatus.routerBusy);
            }
         }
      }
   }

   private void processRouterBusyToNetwork(RouterBusyToNetwork msg) {
      RouterBusyToNetwork mymsg = null;
      if (msg.isAllNetworks()) {
         Vector<BBacnetRouterEntry> v = this.getRouterTable().getRoutersByAddress(msg.getSrcNetworkNumber(), msg.getSrcMacAddress());
         int[] mynets = new int[v.size()];
         if (v.size() > 0) {
            for (int i = 0; i < v.size(); i++) {
               BBacnetRouterEntry re = v.elementAt(i);
               re.setRouterStatus(BRouterStatus.routerBusy);
               re.startBusyTimer();
               mynets[i] = re.getDnet();
            }

            mymsg = new RouterBusyToNetwork(mynets);
         }
      } else {
         int[] dnets = msg.getNetworkNumbers();

         for (int i = 0; i < dnets.length; i++) {
            BBacnetRouterEntry re = this.getRouterTable().getRouterByDnet(dnets[i]);
            if (re == null) {
               this.updateRouterAddress(dnets[i], msg.getSrcNetworkNumber(), msg.getSrcMacAddress(), BRouterStatus.routerBusy);
            } else {
               re.setRouterStatus(BRouterStatus.routerBusy);
               re.startBusyTimer();
            }
         }

         mymsg = new RouterBusyToNetwork(dnets);
      }

      if (this.getRoutingEnabled() && mymsg != null) {
         BNetworkPort[] ports = this.getPorts();

         for (int ix = 0; ix < ports.length; ix++) {
            if (ports[ix].getStatus().isOk() && ports[ix] != msg.getSourcePort()) {
               ports[ix].sendToLink(null, mymsg);
            }
         }
      }
   }

   private void processRouterAvailableToNetwork(RouterAvailableToNetwork msg) {
      RouterAvailableToNetwork mymsg = null;
      if (msg.isAllNetworks()) {
         Vector<BBacnetRouterEntry> v = this.getRouterTable().getRoutersByAddress(msg.getSrcNetworkNumber(), msg.getSrcMacAddress());
         int[] mynets = new int[v.size()];
         if (v.size() > 0) {
            for (int i = 0; i < v.size(); i++) {
               BBacnetRouterEntry re = v.elementAt(i);
               re.setRouterStatus(BRouterStatus.ok);
               re.stopBusyTimer();
               mynets[i] = re.getDnet();
            }

            mymsg = new RouterAvailableToNetwork(mynets);
         }
      } else {
         int[] dnets = msg.getNetworkNumbers();

         for (int i = 0; i < dnets.length; i++) {
            BBacnetRouterEntry re = this.getRouterTable().getRouterByDnet(dnets[i]);
            if (re == null) {
               this.updateRouterAddress(dnets[i], msg.getSrcNetworkNumber(), msg.getSrcMacAddress(), BRouterStatus.ok);
            } else {
               re.setRouterStatus(BRouterStatus.ok);
               re.stopBusyTimer();
            }
         }

         mymsg = new RouterAvailableToNetwork(dnets);
      }

      if (this.getRoutingEnabled() && mymsg != null) {
         BNetworkPort[] ports = this.getPorts();

         for (int ix = 0; ix < ports.length; ix++) {
            if (ports[ix].getStatus().isOk() && ports[ix] != msg.getSourcePort()) {
               ports[ix].sendToLink(null, mymsg);
            }
         }
      }
   }

   private void processInitializeRoutingTable(InitializeRoutingTable msg) {
      BNetworkPort port = this.getPortByNetwork(msg.getSrcNetworkNumber());
      InitializeRoutingTableAck ack;
      if (msg.isEmpty()) {
         ack = new InitializeRoutingTableAck(this.getRouterTable());
      } else {
         BBacnetRouterEntry[] table = msg.getTable();

         for (int i = 0; i < table.length; i++) {
            if (table[i].getPortId() == 0) {
               this.getRouterTable().removeRouterByDnet(table[i].getDnet());
            } else {
               this.getRouterTable().addRouter(table[i]);
            }
         }

         ack = new InitializeRoutingTableAck();
      }

      if (port != null) {
         port.sendToLink(msg.getSrcMacAddress(), ack);
      }
   }

   private void processInitializeRoutingTableAck(InitializeRoutingTableAck msg) {
      if (logger.isLoggable(Level.FINE)) {
         logger.fine(msg.toString());
      }
   }

   private void processEstablishConnectionToNetwork(EstablishConnectionToNetwork msg) {
      this.sendToPort(msg.getSrcNetworkNumber(), msg.getSrcMacAddress(), new RejectMessageToNetwork(0, msg.getNetworkNumber()), msg.getNetworkPriority(), false);
   }

   private void processDisconnectConnectionToNetwork(DisconnectConnectionToNetwork msg) {
      this.sendToPort(msg.getSrcNetworkNumber(), msg.getSrcMacAddress(), new RejectMessageToNetwork(0, msg.getNetworkNumber()), msg.getNetworkPriority(), false);
   }

   private void processProprietaryNetworkMessage(NetworkLayerMsg msg) {
      if (logger.isLoggable(Level.FINE)) {
         logger.fine("Proprietary Bacnet Network Layer Msg encountered, code = " + msg.getMessageType());
      }

      if (!msg.isDNET()) {
         this.sendToPort(msg.getSrcNetworkNumber(), msg.getSrcMacAddress(), new RejectMessageToNetwork(3, 0));
      }
   }

   public void issueIAmRouterToNetworks() {
      if (this.getRoutingEnabled()) {
         BNetworkPort[] ports = this.getPorts();

         for (int i = 0; i < ports.length; i++) {
            if (ports[i].getStatus().isOk()) {
               int[] dnets = this.getRouterTable().getDnets(ports[i].getPortId());
               if (dnets != null && dnets.length != 0) {
                  ports[i].sendToLink(null, new IAmRouterToNetwork(dnets));
               }
            }
         }
      }
   }

   public void doWhatIsNetworkNumber() {
      this.issueWhatIsNetworkNumber();
   }

   public void doSendNetworkNumberIs() {
      this.issueNetworkNumberIs(-1);
   }

   private void processWhatIsNetworkNumber(WhatIsNetworkNumber netMsg) {
      if (logger.isLoggable(Level.FINE)) {
         logger.fine("Received WhatIsNetworkNumber: " + netMsg);
      }

      this.issueNetworkNumberIs(netMsg.getNetworkNumber());
   }

   private void processNetworkNumberIs(NetworkNumberIs netMsg) {
      if (logger.isLoggable(Level.FINE)) {
         logger.fine("Received networkNumberIs: " + netMsg);
      }

      if (netMsg.getNetworkNumber() != netMsg.getRecvOnNetwork()) {
         logger.warning(lex.getText("networkNumberMismatch", new Object[]{netMsg.getNetworkNumber(), netMsg.getRecvOnNetwork()}));
      }
   }

   private void issueNetworkNumberIs(int networkNumber) {
      BNetworkPort[] ports = this.getPorts();

      for (int i = 0; i < ports.length; i++) {
         int nn = ports[i].getNetworkNumber();
         if (networkNumber == nn || networkNumber == -1) {
            ports[i].sendToLink(null, new NetworkNumberIs(nn));
            if (networkNumber != -1) {
               break;
            }
         }
      }
   }

   private void issueWhatIsNetworkNumber() {
      BNetworkPort[] ports = this.getPorts();

      for (int i = 0; i < ports.length; i++) {
         ports[i].sendToLink(null, new WhatIsNetworkNumber());
      }
   }

   private BNetworkPort[] getPorts() {
      SlotCursor<Property> sc = this.getProperties();
      int i = 0;

      while (sc.next(BNetworkPort.class)) {
         if (this.ports == null || i >= this.ports.length || this.ports[i++] != sc.get()) {
            this.ports = (BNetworkPort[])this.getChildren(BNetworkPort.class);
         }
      }

      return this.ports;
   }

   private boolean isRouterActive() {
      if (this.getRoutingEnabled()) {
         this.loadSlots();
         BNetworkPort[] ports = this.getPorts();
         boolean foundPort = false;

         for (int i = 0; i < ports.length; i++) {
            if (ports[i].getStatus().isOk()) {
               if (foundPort) {
                  return true;
               }

               foundPort = true;
            }
         }
      }

      return false;
   }

   private void sendToPort(int networkNumber, byte[] destMacAddress, NetworkPdu npdu, BNetworkPriority priority, boolean dataExpectingReply) {
      npdu.setNetworkPriority(priority);
      npdu.setDataExpectingReply(dataExpectingReply);
      this.sendToPort(networkNumber, destMacAddress, npdu);
   }

   private void sendToPort(int networkNumber, byte[] destMacAddress, NetworkPdu npdu) {
      if (networkNumber != 65535 && networkNumber != 0) {
         BNetworkPort port = this.getPortByNetwork(networkNumber);
         if (port != null) {
            port.sendToLink(destMacAddress, npdu);
         } else {
            logger.warning("Unable to send Npdu to port: unknown networkNumber (" + networkNumber + ")");
         }
      } else {
         BNetworkPort[] ports = this.getPorts();

         for (int i = 0; i < ports.length; i++) {
            ports[i].sendToLink(destMacAddress, npdu);
         }
      }
   }

   private void connectToNetwork(BBacnetRouterEntry router) {
      EstablishConnectionToNetwork msg = new EstablishConnectionToNetwork(router.getDnet(), this.getTerminationTimeValue());
      this.sendToPort(router.getRouterAddress().getNetworkNumber(), router.getRouterAddress().getMacAddress().getBytes(), msg);
   }

   public void issueWhoIsRouterToNetwork(int dnet) {
      this.issueWhoIsRouterToNetwork(dnet, -1);
   }

   private void issueWhoIsRouterToNetwork(int dnet, int excludedDNET) {
      WhoIsRouterToNetwork npdu = new WhoIsRouterToNetwork(dnet);
      npdu.setNetworkPriority(BNetworkPriority.urgent);
      BNetworkPort[] ports = this.getPorts();

      for (int i = 0; i < ports.length; i++) {
         if (ports[i].getNetworkNumber() != excludedDNET) {
            ports[i].sendToLink(null, npdu);
         }
      }
   }

   private void addPendingDNET(int pendingDNET, int timerId) {
      ArrayList<Integer> v = this.pendingDNETs.get(pendingDNET);
      if (v == null) {
         v = new ArrayList<>();
         v.add(timerId);
         this.pendingDNETs.put(pendingDNET, v);
      } else {
         v.add(timerId);
      }
   }

   private void processPendingNpdus(int dnet) {
      ArrayList<Integer> v = this.pendingDNETs.remove(dnet);
      if (v != null) {
         for (int i = 0; i < v.size(); i++) {
            int timerId = v.get(i);
            NetworkPdu npdu = (NetworkPdu)Timers.cancel(timerId);
            if (npdu != null) {
               this.sendRemoteTraffic(npdu);
            }
         }
      }
   }

   private BBacnetRouterEntry getRouter(int networkNumber) {
      return this.getRouter(networkNumber, true);
   }

   private BBacnetRouterEntry getRouter(int networkNumber, boolean establishPTPConnection) {
      BBacnetRouterEntry router = this.getRouterTable().getRouterByDnet(networkNumber);
      if (router != null) {
         if (router.isDisconnected()) {
            if (establishPTPConnection) {
               this.connectToNetwork(router);
               logger.info("Requesting connection establishment to network " + networkNumber + "...");
               return router;
            } else {
               return null;
            }
         } else if (router.isUnavailable()) {
            logger.severe("Router to network " + networkNumber + " is permanently unavailable!!");
            return null;
         } else {
            return router;
         }
      } else {
         return router;
      }
   }

   void updateRouterAddress(int dnet, int networkNumber, byte[] routerMacAddress, BRouterStatus routerStatus) {
      this.updateRouterAddress(dnet, networkNumber, routerMacAddress, routerStatus, null);
   }

   void updateRouterAddress(int dnet, int networkNumber, byte[] routerMacAddress, BRouterStatus routerStatus, String portInfo) {
      if (dnet != networkNumber) {
         BNetworkPort port = this.getPortByNetwork(dnet);
         if (port != null && port.getStatus().isOk()) {
            logger.severe(
               "Bacnet Internetwork misconfiguration detected(5):\nReceived an update to the router table for DNET "
                  + dnet
                  + ", but we are already a router to that DNET!!"
            );
            return;
         }
      }

      BNetworkPort port = this.getPortByNetwork(networkNumber);
      if (port != null) {
         this.getRouterTable().updateRouter(dnet, networkNumber, routerMacAddress, port.getPortId(), portInfo, routerStatus);
      }
   }

   void updateRouterAddress(int[] dnets, int networkNumber, byte[] routerMacAddress, BRouterStatus routerStatus) {
      for (int i = 0; i < dnets.length; i++) {
         this.updateRouterAddress(dnets[i], networkNumber, routerMacAddress, routerStatus, null);
      }
   }

   private boolean checkNetworkConfig(int sourceNetworkNumber, boolean validateTime) {
      boolean retern = true;
      if (validateTime) {
         long now = Clock.ticks();
         Long lastUpdate = this.networkNumberUpdates.get(sourceNetworkNumber);
         if (lastUpdate != null && now - lastUpdate < this.getMinimumRouterUpdateTime()) {
            logger.severe(
               "Bacnet Network Misconfiguration detected(3): Network number update frequency ("
                  + (now - lastUpdate)
                  + "ms) indicates possible router loop on network "
                  + sourceNetworkNumber
                  + "!!!"
            );
            if (!this.getMaintainRoutingEnabled()) {
               this.getRouterTable().removeRouterByDnet(sourceNetworkNumber);
            }

            retern = false;
         }

         this.networkNumberUpdates.put(sourceNetworkNumber, now);
      }

      BNetworkPort np = this.getPortByNetwork(sourceNetworkNumber);
      if (np != null && np.getStatus().isOk()) {
         logger.severe("Bacnet Network Misconfiguration detected(4): Possible router loop, or duplicate network number " + sourceNetworkNumber);
         if (!this.getMaintainRoutingEnabled()) {
            this.getRouterTable().removeRouterByDnet(sourceNetworkNumber);
         }

         retern = false;
      }

      return retern;
   }

   private boolean validateNpdu(NetworkPdu npdu) {
      BBacnetAddress srcAddr = npdu.getSrcAddress();
      BNetworkPort[] ports = this.getPorts();

      for (int i = 0; i < ports.length; i++) {
         if (ports[i].getStatus().isOk()) {
            byte[] myMac = ports[i].getLink().getMacAddress();
            if (srcAddr.equals(ports[i].getNetworkNumber(), myMac)) {
               logger.severe(
                  "Bacnet Network Misconfiguration detected(1): Received message from network "
                     + npdu.getSrcNetworkNumber()
                     + " sent originally by us on network "
                     + ports[i].getNetworkNumber()
                     + "!!!"
               );
               if (!this.getMaintainRoutingEnabled()) {
                  this.getRouterTable().removeRouterByDnet(npdu.getSrcNetworkNumber());
                  if (npdu.isSNET()) {
                     this.getRouterTable().removeRouterByDnet(npdu.getSNET());
                  }
               }

               return false;
            }
         }
      }

      if (npdu.isSNET()) {
         int snet = npdu.getSNET();

         for (int ix = 0; ix < ports.length; ix++) {
            if (snet == ports[ix].getNetworkNumber() && ports[ix].getStatus().isOk()) {
               logger.severe(
                  "Bacnet Network Misconfiguration detected(2): Received message from network "
                     + npdu.getSrcNetworkNumber()
                     + ", but we are also a router to network "
                     + snet
                     + "!!!"
               );
               if (!this.getMaintainRoutingEnabled()) {
                  this.getRouterTable().removeRouterByDnet(npdu.getSrcNetworkNumber());
                  if (npdu.isSNET()) {
                     this.getRouterTable().removeRouterByDnet(npdu.getSNET());
                  }
               }

               return false;
            }
         }
      }

      return true;
   }

   private BBacnetTransportLayer transport() {
      return ((BBacnetStack)this.getParent()).getTransport();
   }

   @Override
   public long timerExpired(int timerId, Object object) {
      if (object instanceof BBacnetRouterEntry) {
         ((BBacnetRouterEntry)object).setRouterStatus(BRouterStatus.ok);
      } else if (object instanceof NetworkPdu) {
         NetworkPdu npdu = (NetworkPdu)object;
         if (npdu.getSrcMacAddress() != null) {
            logger.warning("Router discovery timer expired for dnet " + npdu.getDNET() + ", sending reject...");
            this.sendToPort(
               npdu.getSrcNetworkNumber(), npdu.getSrcMacAddress(), new RejectMessageToNetwork(1, npdu.getDNET()), npdu.getNetworkPriority(), false
            );
         }

         ArrayList<Integer> v = this.pendingDNETs.get(npdu.getDNET());
         v.remove(Integer.valueOf(timerId));
      }

      return 0L;
   }

   public void spy(SpyWriter out) throws Exception {
      super.spy(out);
      out.startProps();
      out.trTitle("BacnetNetworkLayer", 2);
      out.prop("ticks", "" + Clock.ticks());
      out.prop("->now", Clock.time());
      out.prop("pendingDNETs", this.pendingDNETs.size());

      for (Entry<Integer, ArrayList<Integer>> entry : this.pendingDNETs.entrySet()) {
         ArrayList<Integer> v = entry.getValue();
         int dnet = entry.getKey();
         int sz = v.size();
         out.prop("dnet:" + dnet, "pending NPDUs:" + sz);
         StringBuilder sb = new StringBuilder();

         for (int i = 0; i < sz; i++) {
            sb.append(' ').append(v.get(i));
         }

         out.prop("    timerIds", sb.toString());
      }

      out.prop("networkNumberUpdates", this.networkNumberUpdates.size());

      for (Entry<Integer, Long> entry : this.networkNumberUpdates.entrySet()) {
         out.prop("dnet:" + entry.getKey(), entry.getValue());
      }

      out.prop("myQueue.size", this.myQueue.size());
      out.prop("myQueue.maxSize", this.myQueue.maxSize());
      out.endProps();
   }

   static class BacnetNetworkQueue extends RunnablePrioritizedQueue {
      private BBacnetNetworkLayer network;

      BacnetNetworkQueue(BBacnetNetworkLayer network) {
         this.network = network;
      }

      @Override
      protected void process(PrioritizedQueueEntry entry) {
         this.network.process((NetworkPdu)entry);
      }
   }
}
