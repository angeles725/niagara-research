package javax.baja.bacnet;

import com.tridium.bacnet.datatypes.BChangeDeviceIdConfig;
import com.tridium.bacnet.datatypes.BDeviceDiscoveryConfig;
import com.tridium.bacnet.datatypes.BTimeSynchConfig;
import com.tridium.bacnet.datatypes.BWhoHasConfig;
import com.tridium.bacnet.job.BBacnetDiscoverDevicesJob;
import com.tridium.bacnet.job.BChangeDeviceIdJob;
import com.tridium.bacnet.job.BTimeSynchJob;
import com.tridium.bacnet.job.BWhoHasJob;
import com.tridium.bacnet.stack.BBacnetPoll;
import com.tridium.bacnet.stack.BBacnetStack;
import com.tridium.bacnet.stack.client.BBacnetClientLayer;
import com.tridium.bacnet.stack.network.BBacnetNetworkLayer;
import com.tridium.bacnet.stack.network.BNetworkPort;
import com.tridium.bacnet.stack.server.BBacnetServerLayer;
import com.tridium.bacnet.stack.server.cov.BBacnetCovWorker;
import com.tridium.util.ComponentTreeCursor;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.agent.AgentList;
import javax.baja.bacnet.datatypes.BBacnetAddress;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.datatypes.BBacnetOctetString;
import javax.baja.bacnet.enums.BBacnetSegmentation;
import javax.baja.bacnet.export.BLocalBacnetDevice;
import javax.baja.bacnet.io.BBacnetComm;
import javax.baja.bacnet.point.BBacnetTuningPolicy;
import javax.baja.bacnet.point.BBacnetTuningPolicyMap;
import javax.baja.bacnet.util.BBacnetWorker;
import javax.baja.bacnet.util.BIBacnetPollable;
import javax.baja.driver.history.BHistoryNetworkExt;
import javax.baja.driver.loadable.BLoadableNetwork;
import javax.baja.driver.point.BTuningPolicyMap;
import javax.baja.driver.util.BAbstractPollService;
import javax.baja.license.Feature;
import javax.baja.naming.BOrd;
import javax.baja.naming.UnresolvedException;
import javax.baja.nav.BINavNode;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraActions;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.util.Array;
import javax.baja.spy.SpyWriter;
import javax.baja.sys.Action;
import javax.baja.sys.BBoolean;
import javax.baja.sys.BComponent;
import javax.baja.sys.BIService;
import javax.baja.sys.BValue;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.ServiceNotFoundException;
import javax.baja.sys.SlotCursor;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.units.UnitDatabase;
import javax.baja.util.IFuture;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "historyPolicies",
      type = "BHistoryNetworkExt",
      defaultValue = "new BHistoryNetworkExt()"
   ), @NiagaraProperty(
      name = "worker",
      type = "BBacnetWorker",
      defaultValue = "new BBacnetWorker()",
      flags = 4
   ), @NiagaraProperty(
      name = "writeWorker",
      type = "BBacnetWorker",
      defaultValue = "new BBacnetWorker()",
      flags = 4
   ), @NiagaraProperty(
      name = "bacnetComm",
      type = "BBacnetComm",
      defaultValue = "new BBacnetStack()"
   ), @NiagaraProperty(
      name = "localDevice",
      type = "BLocalBacnetDevice",
      defaultValue = "new BLocalBacnetDevice()"
   ), @NiagaraProperty(
      name = "tuningPolicies",
      type = "BTuningPolicyMap",
      defaultValue = "new BBacnetTuningPolicyMap()"
   ), @NiagaraProperty(
      name = "covWorker",
      type = "BBacnetCovWorker",
      defaultValue = "new BBacnetCovWorker()",
      flags = 4
   ), @NiagaraProperty(
      name = "asyncPing",
      type = "boolean",
      defaultValue = "false",
      flags = 4
   )})
@NiagaraActions({@NiagaraAction(
      name = "submitDeviceManagerJob",
      parameterType = "BValue",
      defaultValue = "new BDeviceDiscoveryConfig()",
      returnType = "BOrd",
      flags = 4
   ), @NiagaraAction(
      name = "lookupDeviceById",
      parameterType = "BBacnetObjectIdentifier",
      defaultValue = "BBacnetObjectIdentifier.DEFAULT",
      returnType = "BBacnetDevice",
      flags = 4
   ), @NiagaraAction(
      name = "lookupDeviceByAddress",
      parameterType = "BBacnetAddress",
      defaultValue = "BBacnetAddress.DEFAULT",
      returnType = "BBacnetDevice",
      flags = 4
   ), @NiagaraAction(
      name = "lookupDeviceOrdById",
      parameterType = "BBacnetObjectIdentifier",
      defaultValue = "BBacnetObjectIdentifier.DEFAULT",
      returnType = "BOrd",
      flags = 4
   ), @NiagaraAction(
      name = "lookupDeviceOrdByAddress",
      parameterType = "BBacnetAddress",
      defaultValue = "BBacnetAddress.DEFAULT",
      returnType = "BOrd",
      flags = 4
   )})
public class BBacnetNetwork extends BLoadableNetwork implements BacnetConst, BIService {
   public static final Property historyPolicies = newProperty(0, new BHistoryNetworkExt(), null);
   public static final Property worker = newProperty(4, new BBacnetWorker(), null);
   public static final Property writeWorker = newProperty(4, new BBacnetWorker(), null);
   public static final Property bacnetComm = newProperty(0, new BBacnetStack(), null);
   public static final Property localDevice = newProperty(0, new BLocalBacnetDevice(), null);
   public static final Property tuningPolicies = newProperty(0, new BBacnetTuningPolicyMap(), null);
   public static final Property covWorker = newProperty(4, new BBacnetCovWorker(), null);
   public static final Property asyncPing = newProperty(4, false, null);
   public static final Action submitDeviceManagerJob = newAction(4, new BDeviceDiscoveryConfig(), null);
   public static final Action lookupDeviceById = newAction(4, BBacnetObjectIdentifier.DEFAULT, null);
   public static final Action lookupDeviceByAddress = newAction(4, BBacnetAddress.DEFAULT, null);
   public static final Action lookupDeviceOrdById = newAction(4, BBacnetObjectIdentifier.DEFAULT, null);
   public static final Action lookupDeviceOrdByAddress = newAction(4, BBacnetAddress.DEFAULT, null);
   public static final Type TYPE = Sys.loadType(BBacnetNetwork.class);
   private static Type[] serviceTypes = new Type[]{TYPE};
   public static final String UPLOAD_ON_START = "uploadOnStart";
   public static final String WRITE_ON_FACET_CHANGE = "writeOnFacetChange";
   public static final String SHOULD_SUPPORT_FAULTS_MULTI_STATE = "shouldSupportFaultForMultiState";
   public static final String PRIVATE_TRANSFER_RESULT_BLOCK = "privateTransferResultBlockFlag";
   private boolean networkReady = false;
   private BBoolean uploadOnStart = null;
   private static BBacnetNetwork bacnetService = null;
   private static BBacnetNetwork BACNET_NETWORK = null;
   private static BLocalBacnetDevice BACNET_LOCAL_DEVICE = null;
   protected static final Logger log = Logger.getLogger("bacnet");
   private Hashtable<BBacnetObjectIdentifier, BOrd> ordByObjectId = new Hashtable<>();
   private Map<Integer, Map<BBacnetOctetString, BOrd>> ordByAddress = new HashMap<>();

   public BHistoryNetworkExt getHistoryPolicies() {
      return (BHistoryNetworkExt)this.get(historyPolicies);
   }

   public void setHistoryPolicies(BHistoryNetworkExt v) {
      this.set(historyPolicies, v, null);
   }

   public BBacnetWorker getWorker() {
      return (BBacnetWorker)this.get(worker);
   }

   public void setWorker(BBacnetWorker v) {
      this.set(worker, v, null);
   }

   public BBacnetWorker getWriteWorker() {
      return (BBacnetWorker)this.get(writeWorker);
   }

   public void setWriteWorker(BBacnetWorker v) {
      this.set(writeWorker, v, null);
   }

   public BBacnetComm getBacnetComm() {
      return (BBacnetComm)this.get(bacnetComm);
   }

   public void setBacnetComm(BBacnetComm v) {
      this.set(bacnetComm, v, null);
   }

   public BLocalBacnetDevice getLocalDevice() {
      return (BLocalBacnetDevice)this.get(localDevice);
   }

   public void setLocalDevice(BLocalBacnetDevice v) {
      this.set(localDevice, v, null);
   }

   public BTuningPolicyMap getTuningPolicies() {
      return (BTuningPolicyMap)this.get(tuningPolicies);
   }

   public void setTuningPolicies(BTuningPolicyMap v) {
      this.set(tuningPolicies, v, null);
   }

   public BBacnetCovWorker getCovWorker() {
      return (BBacnetCovWorker)this.get(covWorker);
   }

   public void setCovWorker(BBacnetCovWorker v) {
      this.set(covWorker, v, null);
   }

   public boolean getAsyncPing() {
      return this.getBoolean(asyncPing);
   }

   public void setAsyncPing(boolean v) {
      this.setBoolean(asyncPing, v, null);
   }

   public BOrd submitDeviceManagerJob(BValue parameter) {
      return (BOrd)this.invoke(submitDeviceManagerJob, parameter, null);
   }

   public BBacnetDevice lookupDeviceById(BBacnetObjectIdentifier parameter) {
      return (BBacnetDevice)this.invoke(lookupDeviceById, parameter, null);
   }

   public BBacnetDevice lookupDeviceByAddress(BBacnetAddress parameter) {
      return (BBacnetDevice)this.invoke(lookupDeviceByAddress, parameter, null);
   }

   public BOrd lookupDeviceOrdById(BBacnetObjectIdentifier parameter) {
      return (BOrd)this.invoke(lookupDeviceOrdById, parameter, null);
   }

   public BOrd lookupDeviceOrdByAddress(BBacnetAddress parameter) {
      return (BOrd)this.invoke(lookupDeviceOrdByAddress, parameter, null);
   }

   public Type getType() {
      return TYPE;
   }

   public Type[] getServiceTypes() {
      return serviceTypes;
   }

   public final void serviceStarted() {
      bacnetService = this;
      BACNET_NETWORK = null;
      BACNET_LOCAL_DEVICE = null;
      bacnet();
      localDevice();
   }

   public final void serviceStopped() {
      bacnetService = null;
      BACNET_NETWORK = null;
      BACNET_LOCAL_DEVICE = null;
   }

   public boolean isChildLegal(BComponent child) {
      return !(child instanceof BLocalBacnetDevice);
   }

   public void started() throws Exception {
      try {
         super.started();
         if (Sys.getService(TYPE) != this) {
            this.configFail("Duplicate BacnetNetwork");
            throw new IllegalStateException("Only one BacnetNetwork allowed per station!");
         }

         this.setUploadOnStart();
         this.setAndGetWriteOnFacetChange();
         this.setAndGetShouldSupportFaults();
         this.setAndGetPrivateTransferResultBlockFlag();
      } catch (ServiceNotFoundException var2) {
         log.severe("BACnet Network not registered as a service!");
      }
   }

   public void changed(Property property, Context context) {
      super.changed(property, context);
      if (property.isDynamic() && property.getName().equalsIgnoreCase("uploadOnStart")) {
         this.uploadOnStart = this.setUploadOnStart();
      }
   }

   public void descendantsStarted() throws Exception {
      super.descendantsStarted();
      ((BBacnetStack)this.getBacnetComm()).getNetwork().networkReady();
      ((BBacnetStack)this.getBacnetComm()).getServer().iAm();
      this.networkReady = true;
      SlotCursor<Property> c = this.getProperties();

      while (c.next(BBacnetDevice.class)) {
         ((BBacnetDevice)c.get()).networkReady();
      }
   }

   public void descendantsStopped() throws Exception {
      super.descendantsStopped();
      ((BBacnetStack)this.getBacnetComm()).stopStack();
   }

   public Type getDeviceType() {
      return BBacnetDevice.TYPE;
   }

   public Type getDeviceFolderType() {
      return BBacnetDeviceFolder.TYPE;
   }

   public BINavNode[] getNavChildren() {
      BINavNode[] kids = super.getNavChildren();
      Array<BINavNode> acc = new Array(BINavNode.class);
      acc.add(this.getLocalDevice());
      acc.add(this.getBacnetComm());
      acc.add(this.getMonitor());
      acc.add(this.getTuningPolicies());

      for (int i = 0; i < kids.length; i++) {
         acc.add(kids[i]);
      }

      return (BINavNode[])acc.trim();
   }

   public final Feature getLicenseFeature() {
      return Sys.getLicenseManager().getFeature("tridium", "bacnet");
   }

   public final boolean hasServerLicense() {
      return this.getLicenseFeature().getb("export", false);
   }

   public boolean isAws() {
      return false;
   }

   public IFuture postAsync(Runnable runnable) {
      return this.getWorker().post(runnable);
   }

   public IFuture postWrite(Runnable runnable) {
      return this.getWriteWorker().post(runnable);
   }

   public BOrd doSubmitDeviceManagerJob(BValue arg, Context cx) {
      if (this.isFatalFault()) {
         return null;
      } else {
         Type t = arg.getType();
         if (t.is(BWhoHasConfig.TYPE)) {
            return new BWhoHasJob(this, (BWhoHasConfig)arg).submit(cx);
         } else if (t.is(BDeviceDiscoveryConfig.TYPE)) {
            return new BBacnetDiscoverDevicesJob(this, (BDeviceDiscoveryConfig)arg).submit(cx);
         } else if (t.is(BTimeSynchConfig.TYPE)) {
            return new BTimeSynchJob(this, (BTimeSynchConfig)arg).submit(cx);
         } else {
            return t.is(BChangeDeviceIdConfig.TYPE) ? new BChangeDeviceIdJob(this, (BChangeDeviceIdConfig)arg).submit(cx) : BOrd.DEFAULT;
         }
      }
   }

   public BBacnetDevice doLookupDeviceById(BBacnetObjectIdentifier objectId) {
      if (objectId == null) {
         return null;
      } else if (objectId.getInstanceNumber() < 0) {
         return null;
      } else {
         synchronized (this) {
            BOrd ord = this.ordByObjectId.get(objectId);
            if (ord == null) {
               return null;
            } else {
               BBacnetDevice var10000;
               try {
                  BBacnetDevice dev = (BBacnetDevice)ord.get(this);
                  var10000 = dev;
               } catch (UnresolvedException var6) {
                  this.ordByObjectId.remove(objectId);
                  return null;
               }

               return var10000;
            }
         }
      }
   }

   public BBacnetDevice doLookupDeviceByAddress(BBacnetAddress address) {
      synchronized (this) {
         BOrd ord = this.doLookupDeviceOrdByAddress(address);
         if (ord != null) {
            BBacnetDevice var10000;
            try {
               BBacnetDevice dev = (BBacnetDevice)ord.get(this);
               var10000 = dev;
            } catch (UnresolvedException var6) {
               this.removeAddress(address);
               return null;
            }

            return var10000;
         } else {
            return null;
         }
      }
   }

   public BOrd doLookupDeviceOrdById(BBacnetObjectIdentifier objectId) {
      if (objectId == null) {
         return null;
      } else if (objectId.getInstanceNumber() < 0) {
         return null;
      } else {
         synchronized (this) {
            return this.ordByObjectId.get(objectId);
         }
      }
   }

   public BOrd doLookupDeviceOrdByAddress(BBacnetAddress address) {
      if (address == null) {
         return null;
      } else if (address.equals(BBacnetAddress.DEFAULT)) {
         return null;
      } else {
         BOrd ord = null;
         synchronized (this) {
            int networkNumber = address.getNetworkNumber();
            Map<BBacnetOctetString, BOrd> network = this.ordByAddress.get(networkNumber);
            if (network != null) {
               ord = network.get(address.getMacAddress());
            }

            return ord;
         }
      }
   }

   public synchronized void registerDevice(BBacnetDevice device) {
      BOrd ordInSession = device.getOrdInSession();
      this.ordByObjectId.put(device.getObjectId(), ordInSession);
      BBacnetAddress address = device.getAddress();
      int networkNumber = address.getNetworkNumber();
      Map<BBacnetOctetString, BOrd> network = this.ordByAddress.get(networkNumber);
      if (network == null) {
         network = new HashMap<>();
         this.ordByAddress.put(networkNumber, network);
      }

      network.put(address.getMacAddress(), ordInSession);
   }

   public synchronized void unregisterDevice(BBacnetDevice device) {
      this.removeFromMaps(device.getOrdInSession());
   }

   public synchronized void updateDevice(BBacnetDevice device) {
      this.unregisterDevice(device);
      this.registerDevice(device);
   }

   @Deprecated
   public BBacnetDevice lookupDevice(BBacnetObjectIdentifier objectId) {
      return this.doLookupDeviceById(objectId);
   }

   @Deprecated
   public BBacnetDevice lookupDevice(BBacnetAddress address) {
      return this.doLookupDeviceByAddress(address);
   }

   public void updateDeviceInfo(
      BBacnetObjectIdentifier objectId, BBacnetAddress address, int maxAPDULengthAccepted, BBacnetSegmentation segmentationSupported, int vendorId
   ) {
      if (log.isLoggable(Level.FINE)) {
         log.fine("Updating device data for {" + objectId + "}");
      }

      BBacnetDevice device = this.doLookupDeviceById(objectId);
      if (device != null) {
         if (log.isLoggable(Level.FINE)) {
            log.fine("Updating device data for " + device.getName() + " {" + objectId + "}");
         }

         device.updateDeviceInfo(
            objectId, address, maxAPDULengthAccepted, segmentationSupported, vendorId, this.network().getPortByNetwork(address.getNetworkNumber())
         );
      }
   }

   public BBacnetDevice[] getDeviceList() {
      Array<BBacnetDevice> ret = new Array(BBacnetDevice.class);
      ComponentTreeCursor c = new ComponentTreeCursor(this, null);

      while (c.next(BBacnetDevice.class)) {
         ret.add((BBacnetDevice)c.get());
      }

      return (BBacnetDevice[])ret.trim();
   }

   public BBacnetObjectIdentifier getObjectId() {
      return this.getLocalDevice().getObjectId();
   }

   public static BBacnetNetwork bacnet() {
      if (BACNET_NETWORK == null) {
         BBacnetNetwork tempBacnetNetwork = null;

         try {
            if (Sys.getStation() == null) {
               tempBacnetNetwork = bacnetService;
            } else {
               tempBacnetNetwork = (BBacnetNetwork)Sys.getService(TYPE);
            }
         } catch (ServiceNotFoundException var2) {
            log.log(Level.SEVERE, "Unable to locate Bacnet Service!", (Throwable)var2);
         }

         BACNET_NETWORK = tempBacnetNetwork;
      }

      return BACNET_NETWORK;
   }

   public static BLocalBacnetDevice localDevice() {
      if (BACNET_LOCAL_DEVICE == null) {
         BLocalBacnetDevice tempLocalDevice = null;

         try {
            if (Sys.getStation() == null) {
               if (bacnetService == null) {
                  throw new ServiceNotFoundException("BacnetNetwork service not initialized!");
               }

               tempLocalDevice = bacnetService.getLocalDevice();
            } else {
               tempLocalDevice = ((BBacnetNetwork)Sys.getService(TYPE)).getLocalDevice();
            }
         } catch (ServiceNotFoundException var2) {
            log.log(Level.SEVERE, "Unable to locate Bacnet Service!", (Throwable)var2);
         }

         BACNET_LOCAL_DEVICE = tempLocalDevice;
      }

      return BACNET_LOCAL_DEVICE;
   }

   public boolean isNetworkReady() {
      return this.networkReady;
   }

   public BAbstractPollService getPollService(BIBacnetPollable pollable) {
      return this.poll(pollable.device().getAddress().getNetworkNumber());
   }

   public void tuningChanged(BBacnetTuningPolicy policy, Context cx) {
      BBacnetDevice[] devices = this.getDeviceList();

      for (int i = 0; i < devices.length; i++) {
         devices[i].tuningChanged(policy, cx);
      }
   }

   public BBoolean uploadOnStart() {
      if (this.uploadOnStart == null) {
         this.uploadOnStart = this.setUploadOnStart();
      }

      return this.uploadOnStart;
   }

   private BBoolean setUploadOnStart() {
      BValue value = this.get("uploadOnStart");
      BBoolean uploadOnStart = BBoolean.TRUE;
      if (value == null) {
         this.add("uploadOnStart", uploadOnStart);
      } else if (value instanceof BBoolean) {
         uploadOnStart = (BBoolean)value;
      }

      return uploadOnStart;
   }

   public BBoolean setAndGetWriteOnFacetChange() {
      BValue value = this.get("writeOnFacetChange");
      BBoolean writeOnFacetChange = BBoolean.TRUE;
      if (value == null) {
         this.add("writeOnFacetChange", writeOnFacetChange, 4);
      } else if (value instanceof BBoolean) {
         writeOnFacetChange = (BBoolean)value;
      }

      return writeOnFacetChange;
   }

   public boolean setAndGetShouldSupportFaults() {
      BValue value = this.get("shouldSupportFaultForMultiState");
      BBoolean toSupportFaults = BBoolean.TRUE;
      if (value == null) {
         this.add("shouldSupportFaultForMultiState", toSupportFaults, 4);
      } else if (value instanceof BBoolean) {
         toSupportFaults = (BBoolean)value;
      }

      return toSupportFaults.getBoolean();
   }

   public boolean setAndGetPrivateTransferResultBlockFlag() {
      BValue value = this.get("privateTransferResultBlockFlag");
      BBoolean privateTransferResultBlockFlag = BBoolean.TRUE;
      if (value == null) {
         this.add("privateTransferResultBlockFlag", privateTransferResultBlockFlag, 4);
      } else if (value instanceof BBoolean) {
         privateTransferResultBlockFlag = (BBoolean)value;
      }

      return privateTransferResultBlockFlag.getBoolean();
   }

   BBacnetClientLayer client() {
      return ((BBacnetStack)this.getBacnetComm()).getClient();
   }

   BBacnetServerLayer server() {
      return ((BBacnetStack)this.getBacnetComm()).getServer();
   }

   BBacnetNetworkLayer network() {
      return ((BBacnetStack)this.getBacnetComm()).getNetwork();
   }

   final BBacnetPoll poll(int networkNumber) {
      BNetworkPort port = this.network().getPortByDNET(networkNumber);
      if (port == null) {
         port = this.network().getIpPort();
      }

      return port.getPollService();
   }

   private void removeAddress(BBacnetAddress address) {
      if (address != null) {
         if (!address.equals(BBacnetAddress.DEFAULT)) {
            synchronized (this) {
               int networkNumber = address.getNetworkNumber();
               Map<BBacnetOctetString, BOrd> network = this.ordByAddress.get(networkNumber);
               if (network != null) {
                  network.remove(address.getMacAddress());
                  if (network.isEmpty()) {
                     this.ordByAddress.remove(networkNumber);
                  }
               }
            }
         }
      }
   }

   private synchronized void removeFromMaps(BOrd ord) {
      this.ordByObjectId.values().remove(ord);
      Iterator<Entry<Integer, Map<BBacnetOctetString, BOrd>>> entries = this.ordByAddress.entrySet().iterator();

      while (entries.hasNext()) {
         Map<BBacnetOctetString, BOrd> network = entries.next().getValue();
         network.values().remove(ord);
         if (network.isEmpty()) {
            entries.remove();
         }
      }
   }

   public AgentList getAgents(Context cx) {
      AgentList agents = super.getAgents(cx);
      agents.remove("driver:DeviceManager");
      agents.toBottom("bacnetEDE:EdeBacnetDeviceManager");
      return agents;
   }

   private int hashtableSize(Hashtable<BBacnetObjectIdentifier, BOrd> t) {
      int vsize = 0;
      int ksize = 0;
      Enumeration<BOrd> ee = t.elements();

      while (ee.hasMoreElements()) {
         vsize++;
         ee.nextElement();
      }

      Enumeration<BBacnetObjectIdentifier> ek = t.keys();

      while (ek.hasMoreElements()) {
         ksize++;
         ek.nextElement();
      }

      if (ksize != vsize) {
         log.warning("HASHTABLE SIZE MISMATCH: ksize=" + ksize + "; vsize=" + vsize);
      }

      return vsize;
   }

   public void spy(SpyWriter out) throws Exception {
      super.spy(out);
      out.startProps();
      out.trTitle("BacnetNetwork", 2);
      out.prop("networkReady", this.networkReady);
      out.prop("bacnetService", bacnetService);
      out.prop("bacnet()", bacnet());
      if (this.isRunning()) {
         BComponent c = Sys.getService(TYPE);
         out.prop("service", c);
      }

      out.prop("this", this);
      synchronized (this) {
         out.trTitle("ordByObjectId reported size:" + this.ordByObjectId.size() + "; actual size:" + this.hashtableSize(this.ordByObjectId), 2);
         Enumeration<BBacnetObjectIdentifier> e = this.ordByObjectId.keys();
         if (this.ordByObjectId.size() < 1000) {
            while (e.hasMoreElements()) {
               BBacnetObjectIdentifier k = e.nextElement();
               out.prop("  " + k, this.ordByObjectId.get(k));
            }
         }

         out.trTitle("ordByAddress network size:" + this.ordByAddress.size(), 2);
      }

      out.endProps();
   }

   public String toString(Context cx) {
      return super.toString(cx) + this.getHandleOrd();
   }

   static {
      UnitDatabase.getDefault();
   }
}
