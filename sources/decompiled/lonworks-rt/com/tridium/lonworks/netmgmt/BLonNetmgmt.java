package com.tridium.lonworks.netmgmt;

import com.tridium.lonworks.BLonRouter;
import com.tridium.lonworks.datatypes.BAppDownloadParameter;
import com.tridium.lonworks.datatypes.BCommissionParameter;
import com.tridium.lonworks.datatypes.BDescriptorTable;
import com.tridium.lonworks.datatypes.BDeviceEntry;
import com.tridium.lonworks.datatypes.BDeviceEntryTable;
import com.tridium.lonworks.datatypes.BDiscoverParameter;
import com.tridium.lonworks.datatypes.BLearnParameter;
import com.tridium.lonworks.datatypes.BLinkEntryTable;
import com.tridium.lonworks.datatypes.BMatchParameter;
import com.tridium.lonworks.datatypes.BRouterEntry;
import com.tridium.lonworks.datatypes.BRouterEntryTable;
import com.tridium.lonworks.datatypes.BServicePinData;
import com.tridium.lonworks.datatypes.BSetServiceTypeParameter;
import com.tridium.lonworks.datatypes.BTagLinkEntry;
import com.tridium.lonworks.datatypes.BTagLinkEntryTable;
import com.tridium.lonworks.datatypes.BUtilitiesCommand;
import com.tridium.lonworks.netmessages.ServicePin;
import com.tridium.lonworks.util.NmUtil;
import com.tridium.lonworks.util.RouterUtil;
import com.tridium.sys.station.Station;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.lonworks.AddressManager;
import javax.baja.lonworks.BLonDevice;
import javax.baja.lonworks.BLonNetwork;
import javax.baja.lonworks.LonException;
import javax.baja.lonworks.datatypes.BAuthenticationKey;
import javax.baja.lonworks.datatypes.BDeviceData;
import javax.baja.lonworks.datatypes.BDomainId;
import javax.baja.lonworks.datatypes.BNeuronId;
import javax.baja.lonworks.datatypes.BSubnetNode;
import javax.baja.naming.BOrd;
import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraActions;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraTopic;
import javax.baja.nre.annotations.NiagaraTopics;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.space.BComponentSpace;
import javax.baja.sys.Action;
import javax.baja.sys.BBoolean;
import javax.baja.sys.BComponent;
import javax.baja.sys.BFacets;
import javax.baja.sys.BIcon;
import javax.baja.sys.BString;
import javax.baja.sys.BValue;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Topic;
import javax.baja.sys.Type;
import javax.baja.units.UnitDatabase;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "deviceDiscoverTable",
      type = "BDeviceEntryTable",
      defaultValue = "new BDeviceEntryTable()",
      flags = 6
   ), @NiagaraProperty(
      name = "routerDiscoverTable",
      type = "BRouterEntryTable",
      defaultValue = "new BRouterEntryTable()",
      flags = 6
   ), @NiagaraProperty(
      name = "domainId",
      type = "BDomainId",
      defaultValue = "BDomainId.DEFAULT"
   ), @NiagaraProperty(
      name = "authenticate",
      type = "boolean",
      defaultValue = "false"
   ), @NiagaraProperty(
      name = "authenticationKey",
      type = "BAuthenticationKey",
      defaultValue = "BAuthenticationKey.DEFAULT"
   ), @NiagaraProperty(
      name = "linkDescriptors",
      type = "BDescriptorTable",
      defaultValue = "new BDescriptorTable()"
   ), @NiagaraProperty(
      name = "nonGroupTimer",
      type = "int",
      defaultValue = "4"
   ), @NiagaraProperty(
      name = "channelPriorities",
      type = "int",
      defaultValue = "0"
   ), @NiagaraProperty(
      name = "debug",
      type = "boolean",
      defaultValue = "false"
   ), @NiagaraProperty(
      name = "verifyNvDir",
      type = "boolean",
      defaultValue = "false"
   ), @NiagaraProperty(
      name = "servicePinWait",
      type = "int",
      defaultValue = "300",
      facets = {@Facet("BFacets.makeInt(UnitDatabase.getUnit(\"second\"))")}
   ), @NiagaraProperty(
      name = "enabled",
      type = "boolean",
      defaultValue = "true",
      flags = 7
   ), @NiagaraProperty(
      name = "tempBridge",
      type = "boolean",
      defaultValue = "false",
      flags = 5
   ), @NiagaraProperty(
      name = "useLonObjects",
      type = "boolean",
      defaultValue = "false"
   ), @NiagaraProperty(
      name = "alwaysInZeroLengthDomain",
      type = "boolean",
      defaultValue = "false"
   )})
@NiagaraActions({@NiagaraAction(
      name = "discover",
      returnType = "BOrd",
      flags = 4
   ), @NiagaraAction(
      name = "discoverSelect",
      parameterType = "BDiscoverParameter",
      defaultValue = "new BDiscoverParameter()",
      returnType = "BOrd",
      flags = 4
   ), @NiagaraAction(
      name = "learn",
      parameterType = "BLearnParameter",
      defaultValue = "new BLearnParameter()",
      returnType = "BOrd",
      flags = 4
   ), @NiagaraAction(
      name = "match",
      parameterType = "BMatchParameter",
      defaultValue = "new BMatchParameter()",
      returnType = "BBoolean",
      flags = 4
   ), @NiagaraAction(
      name = "commissionDevice",
      parameterType = "BCommissionParameter",
      defaultValue = "new BCommissionParameter()",
      returnType = "BOrd",
      flags = 4
   ), @NiagaraAction(
      name = "replaceDevice",
      parameterType = "BCommissionParameter",
      defaultValue = "new BCommissionParameter()",
      returnType = "BOrd",
      flags = 4
   ), @NiagaraAction(
      name = "commissionRouter",
      parameterType = "BCommissionParameter",
      defaultValue = "new BCommissionParameter()",
      returnType = "BOrd",
      flags = 4
   ), @NiagaraAction(
      name = "replaceRouter",
      parameterType = "BCommissionParameter",
      defaultValue = "new BCommissionParameter()",
      returnType = "BOrd",
      flags = 4
   ), @NiagaraAction(
      name = "setServiceType",
      parameterType = "BSetServiceTypeParameter",
      defaultValue = "new BSetServiceTypeParameter()",
      returnType = "BOrd",
      flags = 4
   ), @NiagaraAction(
      name = "bind",
      returnType = "BOrd",
      flags = 4
   ), @NiagaraAction(
      name = "bindLinkEntry",
      parameterType = "BString",
      defaultValue = "BString.DEFAULT",
      returnType = "BOrd",
      flags = 4
   ), @NiagaraAction(
      name = "bindTagLinkEntry",
      parameterType = "BTagLinkEntry",
      defaultValue = "new BTagLinkEntry()",
      returnType = "BOrd",
      flags = 4
   ), @NiagaraAction(
      name = "bindDevice",
      parameterType = "BLonDevice",
      defaultValue = "new BLonDevice()",
      returnType = "BOrd",
      flags = 4
   ), @NiagaraAction(
      name = "refreshLinkTable",
      flags = 4
   ), @NiagaraAction(
      name = "tempBridgeOn"
   ), @NiagaraAction(
      name = "tempBridgeOff"
   ), @NiagaraAction(
      name = "appDownLoad",
      parameterType = "BAppDownloadParameter",
      defaultValue = "new BAppDownloadParameter()",
      returnType = "BOrd",
      flags = 4
   ), @NiagaraAction(
      name = "deviceForSubnetNodeId",
      parameterType = "BSubnetNode",
      defaultValue = "BSubnetNode.DEFAULT",
      returnType = "BOrd",
      flags = 4
   ), @NiagaraAction(
      name = "deviceForNeuronId",
      parameterType = "BNeuronId",
      defaultValue = "BNeuronId.DEFAULT",
      returnType = "BOrd",
      flags = 4
   ), @NiagaraAction(
      name = "updateRouters",
      returnType = "BOrd"
   ), @NiagaraAction(
      name = "cancelServicePin"
   )})
@NiagaraTopics({@NiagaraTopic(
      name = "deviceDiscoveryUpdated",
      eventType = "BDeviceEntryTable"
   ), @NiagaraTopic(
      name = "routerDiscoveryUpdated",
      eventType = "BRouterEntryTable"
   ), @NiagaraTopic(
      name = "devicesUpdated",
      eventType = "BDeviceEntryTable"
   ), @NiagaraTopic(
      name = "routersUpdated",
      eventType = "BRouterEntryTable"
   ), @NiagaraTopic(
      name = "linksUpdated",
      eventType = "BLinkEntryTable"
   ), @NiagaraTopic(
      name = "tagLinksUpdated",
      eventType = "BTagLinkEntryTable"
   ), @NiagaraTopic(
      name = "servicePinReceived",
      eventType = "BServicePinData"
   ), @NiagaraTopic(
      name = "learnComplete"
   )})
public class BLonNetmgmt extends BComponent {
   public static final Property deviceDiscoverTable = newProperty(6, new BDeviceEntryTable(), null);
   public static final Property routerDiscoverTable = newProperty(6, new BRouterEntryTable(), null);
   public static final Property domainId = newProperty(0, BDomainId.DEFAULT, null);
   public static final Property authenticate = newProperty(0, false, null);
   public static final Property authenticationKey = newProperty(0, BAuthenticationKey.DEFAULT, null);
   public static final Property linkDescriptors = newProperty(0, new BDescriptorTable(), null);
   public static final Property nonGroupTimer = newProperty(0, 4, null);
   public static final Property channelPriorities = newProperty(0, 0, null);
   public static final Property debug = newProperty(0, false, null);
   public static final Property verifyNvDir = newProperty(0, false, null);
   public static final Property servicePinWait = newProperty(0, 300, BFacets.makeInt(UnitDatabase.getUnit("second")));
   public static final Property enabled = newProperty(7, true, null);
   public static final Property tempBridge = newProperty(5, false, null);
   public static final Property useLonObjects = newProperty(0, false, null);
   public static final Property alwaysInZeroLengthDomain = newProperty(0, false, null);
   public static final Action discover = newAction(4, null);
   public static final Action discoverSelect = newAction(4, new BDiscoverParameter(), null);
   public static final Action learn = newAction(4, new BLearnParameter(), null);
   public static final Action match = newAction(4, new BMatchParameter(), null);
   public static final Action commissionDevice = newAction(4, new BCommissionParameter(), null);
   public static final Action replaceDevice = newAction(4, new BCommissionParameter(), null);
   public static final Action commissionRouter = newAction(4, new BCommissionParameter(), null);
   public static final Action replaceRouter = newAction(4, new BCommissionParameter(), null);
   public static final Action setServiceType = newAction(4, new BSetServiceTypeParameter(), null);
   public static final Action bind = newAction(4, null);
   public static final Action bindLinkEntry = newAction(4, BString.DEFAULT, null);
   public static final Action bindTagLinkEntry = newAction(4, new BTagLinkEntry(), null);
   public static final Action bindDevice = newAction(4, new BLonDevice(), null);
   public static final Action refreshLinkTable = newAction(4, null);
   public static final Action tempBridgeOn = newAction(0, null);
   public static final Action tempBridgeOff = newAction(0, null);
   public static final Action appDownLoad = newAction(4, new BAppDownloadParameter(), null);
   public static final Action deviceForSubnetNodeId = newAction(4, BSubnetNode.DEFAULT, null);
   public static final Action deviceForNeuronId = newAction(4, BNeuronId.DEFAULT, null);
   public static final Action updateRouters = newAction(0, null);
   public static final Action cancelServicePin = newAction(0, null);
   public static final Topic deviceDiscoveryUpdated = newTopic(0, null);
   public static final Topic routerDiscoveryUpdated = newTopic(0, null);
   public static final Topic devicesUpdated = newTopic(0, null);
   public static final Topic routersUpdated = newTopic(0, null);
   public static final Topic linksUpdated = newTopic(0, null);
   public static final Topic tagLinksUpdated = newTopic(0, null);
   public static final Topic servicePinReceived = newTopic(0, null);
   public static final Topic learnComplete = newTopic(0, null);
   public static final Type TYPE = Sys.loadType(BLonNetmgmt.class);
   private static final BIcon icon = BIcon.std("match.png");

   public BDeviceEntryTable getDeviceDiscoverTable() {
      return (BDeviceEntryTable)this.get(deviceDiscoverTable);
   }

   public void setDeviceDiscoverTable(BDeviceEntryTable v) {
      this.set(deviceDiscoverTable, v, null);
   }

   public BRouterEntryTable getRouterDiscoverTable() {
      return (BRouterEntryTable)this.get(routerDiscoverTable);
   }

   public void setRouterDiscoverTable(BRouterEntryTable v) {
      this.set(routerDiscoverTable, v, null);
   }

   public BDomainId getDomainId() {
      return (BDomainId)this.get(domainId);
   }

   public void setDomainId(BDomainId v) {
      this.set(domainId, v, null);
   }

   public boolean getAuthenticate() {
      return this.getBoolean(authenticate);
   }

   public void setAuthenticate(boolean v) {
      this.setBoolean(authenticate, v, null);
   }

   public BAuthenticationKey getAuthenticationKey() {
      return (BAuthenticationKey)this.get(authenticationKey);
   }

   public void setAuthenticationKey(BAuthenticationKey v) {
      this.set(authenticationKey, v, null);
   }

   public BDescriptorTable getLinkDescriptors() {
      return (BDescriptorTable)this.get(linkDescriptors);
   }

   public void setLinkDescriptors(BDescriptorTable v) {
      this.set(linkDescriptors, v, null);
   }

   public int getNonGroupTimer() {
      return this.getInt(nonGroupTimer);
   }

   public void setNonGroupTimer(int v) {
      this.setInt(nonGroupTimer, v, null);
   }

   public int getChannelPriorities() {
      return this.getInt(channelPriorities);
   }

   public void setChannelPriorities(int v) {
      this.setInt(channelPriorities, v, null);
   }

   public boolean getDebug() {
      return this.getBoolean(debug);
   }

   public void setDebug(boolean v) {
      this.setBoolean(debug, v, null);
   }

   public boolean getVerifyNvDir() {
      return this.getBoolean(verifyNvDir);
   }

   public void setVerifyNvDir(boolean v) {
      this.setBoolean(verifyNvDir, v, null);
   }

   public int getServicePinWait() {
      return this.getInt(servicePinWait);
   }

   public void setServicePinWait(int v) {
      this.setInt(servicePinWait, v, null);
   }

   public boolean getEnabled() {
      return this.getBoolean(enabled);
   }

   public void setEnabled(boolean v) {
      this.setBoolean(enabled, v, null);
   }

   public boolean getTempBridge() {
      return this.getBoolean(tempBridge);
   }

   public void setTempBridge(boolean v) {
      this.setBoolean(tempBridge, v, null);
   }

   public boolean getUseLonObjects() {
      return this.getBoolean(useLonObjects);
   }

   public void setUseLonObjects(boolean v) {
      this.setBoolean(useLonObjects, v, null);
   }

   public boolean getAlwaysInZeroLengthDomain() {
      return this.getBoolean(alwaysInZeroLengthDomain);
   }

   public void setAlwaysInZeroLengthDomain(boolean v) {
      this.setBoolean(alwaysInZeroLengthDomain, v, null);
   }

   public BOrd discover() {
      return (BOrd)this.invoke(discover, null, null);
   }

   public BOrd discoverSelect(BDiscoverParameter parameter) {
      return (BOrd)this.invoke(discoverSelect, parameter, null);
   }

   public BOrd learn(BLearnParameter parameter) {
      return (BOrd)this.invoke(learn, parameter, null);
   }

   public BBoolean match(BMatchParameter parameter) {
      return (BBoolean)this.invoke(match, parameter, null);
   }

   public BOrd commissionDevice(BCommissionParameter parameter) {
      return (BOrd)this.invoke(commissionDevice, parameter, null);
   }

   public BOrd replaceDevice(BCommissionParameter parameter) {
      return (BOrd)this.invoke(replaceDevice, parameter, null);
   }

   public BOrd commissionRouter(BCommissionParameter parameter) {
      return (BOrd)this.invoke(commissionRouter, parameter, null);
   }

   public BOrd replaceRouter(BCommissionParameter parameter) {
      return (BOrd)this.invoke(replaceRouter, parameter, null);
   }

   public BOrd setServiceType(BSetServiceTypeParameter parameter) {
      return (BOrd)this.invoke(setServiceType, parameter, null);
   }

   public BOrd bind() {
      return (BOrd)this.invoke(bind, null, null);
   }

   public BOrd bindLinkEntry(BString parameter) {
      return (BOrd)this.invoke(bindLinkEntry, parameter, null);
   }

   public BOrd bindTagLinkEntry(BTagLinkEntry parameter) {
      return (BOrd)this.invoke(bindTagLinkEntry, parameter, null);
   }

   public BOrd bindDevice(BLonDevice parameter) {
      return (BOrd)this.invoke(bindDevice, parameter, null);
   }

   public void refreshLinkTable() {
      this.invoke(refreshLinkTable, null, null);
   }

   public void tempBridgeOn() {
      this.invoke(tempBridgeOn, null, null);
   }

   public void tempBridgeOff() {
      this.invoke(tempBridgeOff, null, null);
   }

   public BOrd appDownLoad(BAppDownloadParameter parameter) {
      return (BOrd)this.invoke(appDownLoad, parameter, null);
   }

   public BOrd deviceForSubnetNodeId(BSubnetNode parameter) {
      return (BOrd)this.invoke(deviceForSubnetNodeId, parameter, null);
   }

   public BOrd deviceForNeuronId(BNeuronId parameter) {
      return (BOrd)this.invoke(deviceForNeuronId, parameter, null);
   }

   public BOrd updateRouters() {
      return (BOrd)this.invoke(updateRouters, null, null);
   }

   public void cancelServicePin() {
      this.invoke(cancelServicePin, null, null);
   }

   public void fireDeviceDiscoveryUpdated(BDeviceEntryTable event) {
      this.fire(deviceDiscoveryUpdated, event, null);
   }

   public void fireRouterDiscoveryUpdated(BRouterEntryTable event) {
      this.fire(routerDiscoveryUpdated, event, null);
   }

   public void fireDevicesUpdated(BDeviceEntryTable event) {
      this.fire(devicesUpdated, event, null);
   }

   public void fireRoutersUpdated(BRouterEntryTable event) {
      this.fire(routersUpdated, event, null);
   }

   public void fireLinksUpdated(BLinkEntryTable event) {
      this.fire(linksUpdated, event, null);
   }

   public void fireTagLinksUpdated(BTagLinkEntryTable event) {
      this.fire(tagLinksUpdated, event, null);
   }

   public void fireServicePinReceived(BServicePinData event) {
      this.fire(servicePinReceived, event, null);
   }

   public void fireLearnComplete(BValue event) {
      this.fire(learnComplete, event, null);
   }

   public Type getType() {
      return TYPE;
   }

   public boolean isParentLegal(BComponent parent) {
      return !(parent instanceof BLonNetwork) ? false : !parent.getProperties().next(BLonNetmgmt.class);
   }

   public BLonNetwork lonNetwork() {
      return (BLonNetwork)this.getParent();
   }

   public Logger log() {
      return this.lonNetwork().log();
   }

   public final void started() {
      boolean disableNetmgmt = Boolean.getBoolean("lonworks.disableNetmgmt");
      this.setEnabled(!disableNetmgmt);
   }

   public void changed(Property prop, Context context) {
      super.changed(prop, context);
      if (this.isRunning() && context != BLonNetwork.lonNoWrite) {
         if (prop == domainId || prop == authenticationKey) {
            try {
               NmUtil.updateDomainTable(this.lonNetwork().getLocalLonDevice(), this.getDomainId(), this.getAuthenticationKey(), false);
            } catch (LonException var4) {
               this.log().log(Level.SEVERE, "Unable to update local domain table ", (Throwable)var4);
            }
         }
      }
   }

   public void doRefreshLinkTable() {
      BComponentSpace cs = this.getComponentSpace();
      if (cs != null && cs == Station.space) {
         Runnable req = new Runnable() {
            @Override
            public void run() {
               BLonNetmgmt.this.asyncRefreshLinkTable();
            }
         };
         this.lonNetwork().postAsync(req);
      }
   }

   private void asyncRefreshLinkTable() {
      try {
         ConnectionTable connTable = new ConnectionTable(this.lonNetwork());
         this.updateLinkTable(connTable);
      } catch (Exception var2) {
         var2.printStackTrace();
      }
   }

   public BOrd doBind() {
      return new BLonBindJob(this, null, null, this.getDebug()).submit(null);
   }

   public BOrd doBindDevice(BLonDevice dev) {
      return new BLonBindJob(this, dev, this.getDebug()).submit(null);
   }

   public BOrd doBindLinkEntry(BString selects) {
      return new BLonBindJob(this, selects, null, this.getDebug()).submit(null);
   }

   public BOrd doBindTagLinkEntry(BTagLinkEntry entry) {
      return new BLonBindJob(this, null, entry, this.getDebug()).submit(null);
   }

   void updateLinkTable(ConnectionTable connTable) {
      BLinkEntryTable lnkTab = connTable.getLonLinkTable();
      this.fireLinksUpdated(lnkTab);
      BTagLinkEntryTable tagTab = connTable.getLonMessageTagTable();
      this.fireTagLinksUpdated(tagTab);
   }

   public void deviceAdded(BLonDevice dev) {
      BDeviceEntry e = this.getDeviceDiscoverTable().findEntry(dev.getDeviceData().getNeuronId());
      if (e != null) {
         this.getDeviceDiscoverTable().removeEntry(e);
      }

      this.fireDeviceDiscoveryUpdated(this.getDeviceDiscoverTable());
   }

   public void routerAdded(BLonRouter dev) {
      BRouterEntry e = this.getRouterDiscoverTable().findEntry(dev.getNearDeviceData().getNeuronId());
      if (e != null) {
         this.getRouterDiscoverTable().removeEntry(e);
      }

      this.fireRouterDiscoveryUpdated(this.getRouterDiscoverTable());
   }

   public void receiveServicePin(ServicePin msg) {
      this.lonNetwork().postAsync(new ProcessServicePin(this, msg));
      this.fireServicePinReceived(new BServicePinData(msg.getNeuronId(), msg.getIdString()));
   }

   public BOrd doDiscover() {
      return new BLonDiscoverJob(this).submit(null);
   }

   public BOrd doDiscoverSelect(BDiscoverParameter param) {
      return new BLonDiscoverJob(this, param).submit(null);
   }

   public BOrd doLearn(BLearnParameter param) {
      return param.isSelectedDevices() ? new BLonLearnLinksJob(this, param).submit(null) : new BLonLearnJob(this, param).submit(null);
   }

   public void doCancelServicePin() {
      this.lonNetwork().netMessageReceiver().cancelServicePin();
   }

   public BOrd doCommissionDevice(BCommissionParameter param) {
      return new BLonCommissionJob(this, param).submit(null);
   }

   public BOrd doCommissionRouter(BCommissionParameter param) {
      return new BLonCommissionRouterJob(this, param).submit(null);
   }

   public BOrd doReplaceDevice(BCommissionParameter param) {
      return new BLonReplaceJob(this, param).submit(null);
   }

   public BOrd doReplaceRouter(BCommissionParameter param) {
      return new BLonCommissionRouterJob(this, param).submit(null);
   }

   public void doTempBridgeOn() {
      Runnable req = new Runnable() {
         @Override
         public void run() {
            BLonNetmgmt.this.setTempBridge(true);
            RouterUtil.setTemporaryBridge(BLonNetmgmt.this.lonNetwork());
         }
      };
      this.lonNetwork().postAsync(req);
   }

   public void doTempBridgeOff() {
      Runnable req = new Runnable() {
         @Override
         public void run() {
            BLonNetmgmt.this.setTempBridge(false);
            RouterUtil.clearTemporaryBridge(BLonNetmgmt.this.lonNetwork());
         }
      };
      this.lonNetwork().postAsync(req);
   }

   public BOrd doAppDownLoad(BAppDownloadParameter param) {
      return new BLonAppDownloadJob(this, param).submit(null);
   }

   public BOrd doSetServiceType(BSetServiceTypeParameter param) {
      return new BLonSetServiceTypeJob(this, param).submit(null);
   }

   public BBoolean doMatch(BMatchParameter param) {
      BLonDevice dbDev = this.lonNetwork().addressManager().getDeviceByAddress(param.getDbDevSubnetNode());
      BDeviceEntry devEntry = param.getDeviceEntry();
      if (dbDev != null && devEntry != null) {
         BDeviceData dd = dbDev.getDeviceData();
         BSubnetNode sn = BSubnetNode.make(devEntry.getSubnet(), devEntry.getNode());
         BLonDevice snDev = this.lonNetwork().addressManager().getDeviceByAddress(sn);
         if (snDev != null && snDev != dbDev) {
            return BBoolean.FALSE;
         } else {
            dd.set(BDeviceData.neuronId, devEntry.getNeuronId(), AddressManager.noDeviceChange);
            dd.set(BDeviceData.subnetNodeId, sn, AddressManager.noDeviceChange);
            dd.set(BDeviceData.nodeState, devEntry.getState(), AddressManager.noDeviceChange);
            dd.set(BDeviceData.programId, devEntry.getProgramId(), AddressManager.noDeviceChange);
            dd.setInt(BDeviceData.channelId, devEntry.getChannelId(), AddressManager.noDeviceChange);
            String n = devEntry.getDevName();
            if (!n.equals(dbDev.getName())) {
               dbDev.getParent().asComponent().rename(dbDev.getPropertyInParent(), n);
            }

            return BBoolean.TRUE;
         }
      } else {
         return BBoolean.TRUE;
      }
   }

   public BOrd doDeviceForSubnetNodeId(BSubnetNode addr) {
      if (this.lonNetwork().addressManager() == null) {
         return null;
      } else {
         BLonDevice dev = this.lonNetwork().addressManager().getDeviceByAddress(addr);
         if (dev != null) {
            return dev.getHandleOrd();
         } else {
            BLonRouter rtr = this.lonNetwork().addressManager().getRouterByAddress(addr);
            return rtr != null ? rtr.getHandleOrd() : null;
         }
      }
   }

   public BOrd doDeviceForNeuronId(BNeuronId nid) {
      if (this.lonNetwork().addressManager() == null) {
         return null;
      } else {
         BLonDevice dev = this.lonNetwork().addressManager().getDeviceByAddress(nid);
         if (dev != null) {
            return dev.getHandleOrd();
         } else {
            BLonRouter rtr = this.lonNetwork().addressManager().getRouterByAddress(nid);
            return rtr != null ? rtr.getHandleOrd() : null;
         }
      }
   }

   public void executeUtilitiesCommand(BUtilitiesCommand cmd, PrintWriter out) {
      LonUtilRequest r = new LonUtilRequest(cmd, this, out);
      r.run();
      out.flush();
   }

   public BOrd doUpdateRouters() {
      return new BLonUpdateRoutersJob(this).submit(null);
   }

   public BIcon getIcon() {
      return icon;
   }
}
