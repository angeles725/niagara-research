package javax.baja.bacnet;

import com.tridium.bacnet.BacUtil;
import com.tridium.bacnet.ObjectTypeList;
import com.tridium.bacnet.asn.AsnUtil;
import com.tridium.bacnet.asn.NErrorType;
import com.tridium.bacnet.asn.NReadAccessSpec;
import com.tridium.bacnet.asn.NReadPropertyResult;
import com.tridium.bacnet.history.BBacnetHistoryDeviceExt;
import com.tridium.bacnet.schedule.BBacnetScheduleDeviceExt;
import com.tridium.bacnet.stack.BBacnetPoll;
import com.tridium.bacnet.stack.BBacnetStack;
import com.tridium.bacnet.stack.DeviceRegistry;
import com.tridium.bacnet.stack.client.BBacnetClientLayer;
import com.tridium.bacnet.stack.link.BBacnetLinkLayer;
import com.tridium.bacnet.stack.link.ethernet.BBacnetEthernetLinkLayer;
import com.tridium.bacnet.stack.link.ip.BBacnetIpLinkLayer;
import com.tridium.bacnet.stack.link.mstp.BBacnetMstpLinkLayer;
import com.tridium.bacnet.stack.link.sc.BScLinkLayer;
import com.tridium.bacnet.stack.network.BNetworkPort;
import com.tridium.bacnet.stack.transport.TransactionException;
import com.tridium.bacnet.stack.transport.TransactionTimeoutException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.bacnet.alarm.BBacnetAlarmDeviceExt;
import javax.baja.bacnet.config.BBacnetConfigDeviceExt;
import javax.baja.bacnet.config.BBacnetDeviceObject;
import javax.baja.bacnet.datatypes.BBacnetAddress;
import javax.baja.bacnet.datatypes.BBacnetBitString;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.datatypes.BBacnetOctetString;
import javax.baja.bacnet.datatypes.BBacnetPropertyReference;
import javax.baja.bacnet.datatypes.BBacnetUnsigned;
import javax.baja.bacnet.device.LatencyRecorder;
import javax.baja.bacnet.device.LatencyRecorderAware;
import javax.baja.bacnet.device.overrides.ApduSizeOverride;
import javax.baja.bacnet.device.overrides.DeviceOverride;
import javax.baja.bacnet.device.overrides.DeviceOverrideAware;
import javax.baja.bacnet.device.overrides.SegmentationOverride;
import javax.baja.bacnet.device.overrides.ServiceOverride;
import javax.baja.bacnet.enums.BBacnetErrorCode;
import javax.baja.bacnet.enums.BBacnetPropertyIdentifier;
import javax.baja.bacnet.enums.BBacnetSegmentation;
import javax.baja.bacnet.enums.BCharacterSetEncoding;
import javax.baja.bacnet.enums.BExtensibleEnumList;
import javax.baja.bacnet.io.AbortException;
import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.io.ErrorException;
import javax.baja.bacnet.io.RejectException;
import javax.baja.bacnet.point.BBacnetPointDeviceExt;
import javax.baja.bacnet.point.BBacnetProxyExt;
import javax.baja.bacnet.point.BBacnetTuningPolicy;
import javax.baja.bacnet.util.BIBacnetPollable;
import javax.baja.bacnet.util.BacnetBitStringUtil;
import javax.baja.bacnet.util.PollList;
import javax.baja.bacnet.util.PollListEntry;
import javax.baja.bacnet.util.PropertyInfo;
import javax.baja.bacnet.virtual.BBacnetVirtualGateway;
import javax.baja.control.BControlPoint;
import javax.baja.driver.history.BHistoryDeviceExt;
import javax.baja.driver.loadable.BDownloadParameters;
import javax.baja.driver.loadable.BLoadableDevice;
import javax.baja.driver.loadable.BUploadParameters;
import javax.baja.driver.schedule.BScheduleDeviceExt;
import javax.baja.driver.util.BPollFrequency;
import javax.baja.naming.BOrd;
import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.util.Array;
import javax.baja.nre.util.TextUtil;
import javax.baja.spy.SpyWriter;
import javax.baja.status.BStatus;
import javax.baja.sys.Action;
import javax.baja.sys.BBoolean;
import javax.baja.sys.BComponent;
import javax.baja.sys.BDynamicEnum;
import javax.baja.sys.BEnum;
import javax.baja.sys.BEnumRange;
import javax.baja.sys.BFacets;
import javax.baja.sys.BObject;
import javax.baja.sys.BValue;
import javax.baja.sys.Clock;
import javax.baja.sys.Context;
import javax.baja.sys.NotRunningException;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.sys.Clock.Ticket;
import javax.baja.util.BTypeSpec;
import javax.baja.util.Lexicon;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "address",
      type = "BBacnetAddress",
      defaultValue = "BBacnetAddress.LOCAL_BROADCAST_ADDRESS",
      flags = 64
   ), @NiagaraProperty(
      name = "points",
      type = "BBacnetPointDeviceExt",
      defaultValue = "new BBacnetPointDeviceExt()"
   ), @NiagaraProperty(
      name = "virtual",
      type = "BBacnetVirtualGateway",
      defaultValue = "new BBacnetVirtualGateway()"
   ), @NiagaraProperty(
      name = "alarms",
      type = "BBacnetAlarmDeviceExt",
      defaultValue = "new BBacnetAlarmDeviceExt()"
   ), @NiagaraProperty(
      name = "schedules",
      type = "BScheduleDeviceExt",
      defaultValue = "new BBacnetScheduleDeviceExt()"
   ), @NiagaraProperty(
      name = "trendLogs",
      type = "BHistoryDeviceExt",
      defaultValue = "new BBacnetHistoryDeviceExt()"
   ), @NiagaraProperty(
      name = "config",
      type = "BBacnetConfigDeviceExt",
      defaultValue = "new BBacnetConfigDeviceExt()"
   ), @NiagaraProperty(
      name = "enumerationList",
      type = "BExtensibleEnumList",
      defaultValue = "new BExtensibleEnumList()"
   ), @NiagaraProperty(
      name = "useCov",
      type = "boolean",
      defaultValue = "false"
   ), @NiagaraProperty(
      name = "useCovProperty",
      type = "boolean",
      defaultValue = "false"
   ), @NiagaraProperty(
      name = "maxCovSubscriptions",
      type = "int",
      defaultValue = "Integer.MAX_VALUE"
   ), @NiagaraProperty(
      name = "covSubscriptions",
      type = "int",
      defaultValue = "0",
      flags = 3
   ), @NiagaraProperty(
      name = "pollFrequency",
      type = "BPollFrequency",
      defaultValue = "BPollFrequency.normal",
      flags = 5
   ), @NiagaraProperty(
      name = "characterSet",
      type = "BCharacterSetEncoding",
      defaultValue = "BCharacterSetEncoding.iso10646_UTF8",
      flags = 1
   ), @NiagaraProperty(
      name = "maxPollTimeouts",
      type = "int",
      defaultValue = "0",
      flags = 4,
      facets = {@Facet(
         name = "BFacets.MIN",
         value = "0"
      )}
   ), @NiagaraProperty(
      name = "disableDeviceOnCovSubscriptionFailure",
      type = "boolean",
      defaultValue = "true",
      flags = 4
   )})
@NiagaraAction(
   name = "macAddressFailed",
   flags = 4
)
public class BBacnetDevice
   extends BLoadableDevice
   implements BacnetConst,
   BIBacnetPollable,
   BIBacnetObjectContainer,
   DeviceOverrideAware,
   LatencyRecorderAware,
   LatencyRecorder {
   public static final Property address = newProperty(64, BBacnetAddress.LOCAL_BROADCAST_ADDRESS, null);
   public static final Property points = newProperty(0, new BBacnetPointDeviceExt(), null);
   public static final Property virtual = newProperty(0, new BBacnetVirtualGateway(), null);
   public static final Property alarms = newProperty(0, new BBacnetAlarmDeviceExt(), null);
   public static final Property schedules = newProperty(0, new BBacnetScheduleDeviceExt(), null);
   public static final Property trendLogs = newProperty(0, new BBacnetHistoryDeviceExt(), null);
   public static final Property config = newProperty(0, new BBacnetConfigDeviceExt(), null);
   public static final Property enumerationList = newProperty(0, new BExtensibleEnumList(), null);
   public static final Property useCov = newProperty(0, false, null);
   public static final Property useCovProperty = newProperty(0, false, null);
   public static final Property maxCovSubscriptions = newProperty(0, Integer.MAX_VALUE, null);
   public static final Property covSubscriptions = newProperty(3, 0, null);
   public static final Property pollFrequency = newProperty(5, BPollFrequency.normal, null);
   public static final Property characterSet = newProperty(1, BCharacterSetEncoding.iso10646_UTF8, null);
   public static final Property maxPollTimeouts = newProperty(4, 0, BFacets.make("min", 0));
   public static final Property disableDeviceOnCovSubscriptionFailure = newProperty(4, true, null);
   public static final Action macAddressFailed = newAction(4, null);
   public static final Type TYPE = Sys.loadType(BBacnetDevice.class);
   protected static final int MINIMUM_COV_SUBSCRIPTION_LIFETIME = 300;
   private static final Lexicon lex = Lexicon.make("bacnet");
   private boolean rpmOk = false;
   private int maxAPDU = 50;
   private BBacnetAddress oldAddress;
   private boolean isPolling;
   private int pollTimeouts = 0;
   private ObjectTypeList vendorObjectTypesList = null;
   private BBoolean isAws = null;
   private Object DEVICE_LOCK = new Object();
   private Ticket staleTicket = null;
   private long lastPingTime = 0L;
   private volatile int failedPings = 0;
   private volatile boolean isFirstPingOk = true;
   private static final int CHECK_ADDRESS_AFTER_PING_FAILS = Integer.getInteger("niagara.bacnet.checkAddressAfterFailedPings", 100);
   public static final Logger log = Logger.getLogger("bacnet.client");
   public static final Logger plog = Logger.getLogger("bacnet.point");
   private static final int MIN_APDU = 50;
   private static final String SKIP_UPLOAD = "skipUpload";
   public static final String IGNORE_SYSTEM_STATUS = "ignoreSystemStatus";
   private List<DeviceOverride> overrides = null;
   private List<LatencyRecorder> recorders = null;

   public BBacnetAddress getAddress() {
      return (BBacnetAddress)this.get(address);
   }

   public void setAddress(BBacnetAddress v) {
      this.set(address, v, null);
   }

   public BBacnetPointDeviceExt getPoints() {
      return (BBacnetPointDeviceExt)this.get(points);
   }

   public void setPoints(BBacnetPointDeviceExt v) {
      this.set(points, v, null);
   }

   public BBacnetVirtualGateway getVirtual() {
      return (BBacnetVirtualGateway)this.get(virtual);
   }

   public void setVirtual(BBacnetVirtualGateway v) {
      this.set(virtual, v, null);
   }

   public BBacnetAlarmDeviceExt getAlarms() {
      return (BBacnetAlarmDeviceExt)this.get(alarms);
   }

   public void setAlarms(BBacnetAlarmDeviceExt v) {
      this.set(alarms, v, null);
   }

   public BScheduleDeviceExt getSchedules() {
      return (BScheduleDeviceExt)this.get(schedules);
   }

   public void setSchedules(BScheduleDeviceExt v) {
      this.set(schedules, v, null);
   }

   public BHistoryDeviceExt getTrendLogs() {
      return (BHistoryDeviceExt)this.get(trendLogs);
   }

   public void setTrendLogs(BHistoryDeviceExt v) {
      this.set(trendLogs, v, null);
   }

   public BBacnetConfigDeviceExt getConfig() {
      return (BBacnetConfigDeviceExt)this.get(config);
   }

   public void setConfig(BBacnetConfigDeviceExt v) {
      this.set(config, v, null);
   }

   public BExtensibleEnumList getEnumerationList() {
      return (BExtensibleEnumList)this.get(enumerationList);
   }

   public void setEnumerationList(BExtensibleEnumList v) {
      this.set(enumerationList, v, null);
   }

   public boolean getUseCov() {
      return this.getBoolean(useCov);
   }

   public void setUseCov(boolean v) {
      this.setBoolean(useCov, v, null);
   }

   public boolean getUseCovProperty() {
      return this.getBoolean(useCovProperty);
   }

   public void setUseCovProperty(boolean v) {
      this.setBoolean(useCovProperty, v, null);
   }

   public int getMaxCovSubscriptions() {
      return this.getInt(maxCovSubscriptions);
   }

   public void setMaxCovSubscriptions(int v) {
      this.setInt(maxCovSubscriptions, v, null);
   }

   public int getCovSubscriptions() {
      return this.getInt(covSubscriptions);
   }

   public void setCovSubscriptions(int v) {
      this.setInt(covSubscriptions, v, null);
   }

   public BPollFrequency getPollFrequency() {
      return (BPollFrequency)this.get(pollFrequency);
   }

   public void setPollFrequency(BPollFrequency v) {
      this.set(pollFrequency, v, null);
   }

   public BCharacterSetEncoding getCharacterSet() {
      return (BCharacterSetEncoding)this.get(characterSet);
   }

   public void setCharacterSet(BCharacterSetEncoding v) {
      this.set(characterSet, v, null);
   }

   public int getMaxPollTimeouts() {
      return this.getInt(maxPollTimeouts);
   }

   public void setMaxPollTimeouts(int v) {
      this.setInt(maxPollTimeouts, v, null);
   }

   public boolean getDisableDeviceOnCovSubscriptionFailure() {
      return this.getBoolean(disableDeviceOnCovSubscriptionFailure);
   }

   public void setDisableDeviceOnCovSubscriptionFailure(boolean v) {
      this.setBoolean(disableDeviceOnCovSubscriptionFailure, v, null);
   }

   public void macAddressFailed() {
      this.invoke(macAddressFailed, null, null);
   }

   public Type getType() {
      return TYPE;
   }

   @Override
   public final BBacnetDevice device() {
      return this;
   }

   @Override
   public final int getPollableType() {
      return 0;
   }

   @Deprecated
   @Override
   public boolean poll() {
      log.warning("BBacnetDevice.poll() is no longer used.");
      return true;
   }

   @Override
   public final void readFail(String failureMsg) {
   }

   @Override
   public final void fromEncodedValue(byte[] encodedValue, BStatus status, Context cx) {
   }

   @Override
   public final PollListEntry[] getPollListEntries() {
      return new PollListEntry[0];
   }

   public String toString(Context context) {
      StringBuilder sb = new StringBuilder();
      sb.append("BacnetDevice {").append(this.getName()).append("}");
      return sb.toString();
   }

   public final BBacnetObjectIdentifier getObjectId() {
      return this.getDeviceObject().getObjectId();
   }

   public final void setObjectId(BBacnetObjectIdentifier objectId, Context cx) {
      this.getDeviceObject().set(BBacnetObject.objectId, objectId, cx);
   }

   public final void objectIdChanged() {
      if (this.getObjectId().isValid()) {
         this.configOk();
         this.network().postAsync(new Runnable() {
            @Override
            public void run() {
               BBacnetDevice.this.checkAddress();
               BBacnetDevice.this.upload(new BUploadParameters(false));
            }
         });
      } else {
         this.configFail("Invalid Device Object ID");
      }
   }

   public final int getMaxAPDULengthAccepted() {
      int overridenMaxApdu = this.getOverridenAdpuSize(this.maxAPDU);
      return Math.max(overridenMaxApdu, 50);
   }

   public final void setMaxAPDULengthAccepted(int maxAPDULengthAccepted, Context cx) {
      this.maxAPDU = maxAPDULengthAccepted;
   }

   public final BBacnetSegmentation getSegmentationSupported() {
      SegmentationOverride segmentationOverride = this.getSegmentationOverride();
      return segmentationOverride == null
         ? this.getDeviceObject().getSegmentationSupported()
         : segmentationOverride.getSegmentationSupported(this.getDeviceObject());
   }

   public final void setSegmentationSupported(BBacnetSegmentation segmentationSupported, Context cx) {
      this.getDeviceObject().set(BBacnetDeviceObject.segmentationSupported, segmentationSupported, cx);
   }

   public final int getMaxSegmentsAccepted() {
      return this.getDeviceObject().getMaxSegmentsAccepted();
   }

   public final int getVendorId() {
      return this.getDeviceObject().getVendorIdentifier().getInt();
   }

   public final void setVendorId(int vendorId, Context cx) {
      this.getDeviceObject().set(BBacnetDeviceObject.vendorIdentifier, BBacnetUnsigned.make(vendorId), cx);
   }

   public boolean isServiceSupported(int serviceId) {
      return this.getServicesSupported(this.getDeviceObject().getProtocolServicesSupported()).getBit(serviceId);
   }

   public boolean isServiceSupported(String serviceName) {
      return this.getServicesSupported(this.getDeviceObject().getProtocolServicesSupported())
         .getBit(BacnetBitStringUtil.getBitIndex("BacnetServicesSupported", serviceName));
   }

   public boolean isObjectTypeSupported(int objectType) {
      return this.getDeviceObject().getProtocolObjectTypesSupported().getBit(objectType);
   }

   public final int getProtocolRevision() {
      return this.getDeviceObject().getProtocolRevision().getInt();
   }

   @Deprecated
   public BValue getObjectListStaleTime() {
      return this.get("objectListStaleTime");
   }

   protected BBacnetNetwork network() {
      return (BBacnetNetwork)this.getNetwork();
   }

   private BBacnetClientLayer client() {
      return ((BBacnetStack)this.network().getBacnetComm()).getClient();
   }

   private BBacnetDeviceObject getDeviceObject() {
      return this.getConfig().getDeviceObject();
   }

   public boolean isAddressValid() {
      BBacnetAddress addr = this.getAddress();
      return addr != null && !addr.getMacAddress().isNull();
   }

   public void checkAddress() {
      try {
         int instanceNumber = this.getObjectId().getInstanceNumber();
         if (instanceNumber >= 0) {
            this.client().whoIs(BBacnetAddress.GLOBAL_BROADCAST_ADDRESS, instanceNumber, instanceNumber);
         }
      } catch (BacnetException var2) {
         if (log.isLoggable(Level.FINE)) {
            log.log(Level.FINE, "BacnetException checking address for " + this + ": " + var2, (Throwable)var2);
         }
      }
   }

   private boolean isAws() {
      if (this.isAws == null) {
         this.isAws = BBoolean.FALSE;

         try {
            BOrd serviceOrd = BOrd.make("service:bacnet:BacnetNetwork");
            BBacnetNetwork bacnet = (BBacnetNetwork)serviceOrd.get(this);
            if (bacnet != null && bacnet.getType().getTypeName().indexOf("Aws") >= 0) {
               this.isAws = BBoolean.TRUE;
            }
         } catch (Exception var3) {
            if (log.isLoggable(Level.FINE)) {
               log.fine("Could not determine AWS status for " + this + ":" + var3);
            }
         }
      }

      return this.isAws.getBoolean();
   }

   public void started() throws Exception {
      super.started();
      if (!this.getObjectId().isValid()) {
         this.configFail("Invalid Device Object ID");
      } else {
         this.checkForDuplicateDeviceId();
         if (!this.isAddressValid()) {
            this.setStatus(BStatus.stale);
            this.checkAddress();
            synchronized (this.DEVICE_LOCK) {
               if (this.staleTicket == null) {
                  this.staleTicket = Clock.schedule(this, this.network().getMonitor().getStartupAlarmDelay(), macAddressFailed, null);
               }
            }
         }

         this.initializeDevice();
         if (log.isLoggable(Level.FINEST)) {
            log.finest(this + " device started execution finish.");
         }
      }
   }

   @Override
   public void updateServicesSupported() {
      this.rpmOk = this.isServiceSupported("readPropertyMultiple");
   }

   private void updateMaxAPDU() {
      this.maxAPDU = this.getDeviceObject().getMaxAPDULengthAccepted().getInt();
      if (this.maxAPDU <= 50 && this.isAddressValid()) {
         int registrySize = DeviceRegistry.getMaxApduLengthSupported(this.getAddress());
         this.maxAPDU = Math.max(this.maxAPDU, registrySize);
      }

      if (this.maxAPDU > 50) {
         this.getConfig().getDeviceObject().set(BBacnetDeviceObject.maxAPDULengthAccepted, BBacnetUnsigned.make(this.maxAPDU), noWrite);
      }
   }

   private void updateSegmentationSupported() {
      BBacnetSegmentation segmentationSupported = this.getDeviceObject().getSegmentationSupported();
      if (segmentationSupported == BBacnetSegmentation.noSegmentation && this.isAddressValid()) {
         BBacnetSegmentation registrySegmentation = DeviceRegistry.getSegmentationSupported(this.getAddress());
         if (registrySegmentation != BBacnetSegmentation.noSegmentation) {
            this.setSegmentationSupported(registrySegmentation, noWrite);
         }
      }
   }

   protected void initializeDevice() {
      this.oldAddress = this.getAddress();
      this.updateServicesSupported();
      this.updateSegmentationSupported();
      this.updateMaxAPDU();
      this.isAws = null;
      BBacnetNetwork.localDevice().addAddressBinding(this);
      DeviceRegistry.update(this);
      BBacnetNetwork.bacnet().registerDevice(this);
      ((BBacnetScheduleDeviceExt)this.getSchedules()).setSupport(this.getProtocolRevision());
      this.network().postAsync(new Runnable() {
         @Override
         public void run() {
            BBacnetDevice.this.getVendorObjectTypesList();
         }
      });
   }

   private void checkForDuplicateDeviceId() {
      BBacnetDevice dup = this.network().doLookupDeviceById(this.getObjectId());
      if (dup != null && dup != this) {
         this.configFail("Duplicate Device Object ID");
         this.setObjectId(BBacnetObjectIdentifier.make(8), null);
      }
   }

   public void descendantsStarted() {
      if (this.network().isNetworkReady()) {
         this.networkReady();
      }
   }

   protected boolean uploadOnStart() {
      BValue skipUpload = this.get("skipUpload");
      return skipUpload == null || skipUpload.equals(BBoolean.FALSE);
   }

   void networkReady() {
      if (this.getObjectId().isValid() && this.getStatus().isValid() && this.uploadOnStart()) {
         if (log.isLoggable(Level.FINE)) {
            log.fine(this + ": sending soft ping to trigger an upload at start");
         }

         this.postSoftPing();
      }
   }

   private void postUpload() {
      this.network().postAsync(new Runnable() {
         @Override
         public void run() {
            try {
               if (!BBacnetDevice.this.isAddressValid()) {
                  BBacnetDevice.this.checkAddress();
               } else if (BBacnetDevice.this.getStatus().isOk()) {
                  BBacnetDevice.this.uploadDevice(new BUploadParameters(), null);
                  if (!BBacnetDevice.this.network().uploadOnStart().getBoolean()) {
                     BacUtil.setOrAdd(BBacnetDevice.this, "skipUpload", BBoolean.make(true), 4, null, null);
                  }
               }
            } catch (Exception var2) {
               BBacnetDevice.plog.log(Level.SEVERE, "Exception occurred in postUpload", (Throwable)var2);
            }
         }
      });
   }

   private void postSoftPing() {
      this.network()
         .postAsync(
            new Runnable() {
               @Override
               public void run() {
                  try {
                     byte[] encoded = ((BBacnetStack)BBacnetNetwork.bacnet().getBacnetComm())
                        .getClient()
                        .readProperty(BBacnetDevice.this.getAddress(), BBacnetDevice.this.getObjectId(), 112);
                     if (encoded == null) {
                        if (BBacnetDevice.log.isLoggable(Level.FINE)) {
                           BBacnetDevice.log.fine(this + ": soft ping returned null");
                        }

                        return;
                     }

                     int systemStatus = AsnUtil.fromAsnEnumerated(encoded);
                     BBacnetDevice.this.pingOk();
                     BBacnetDevice.this.updateSystemStatus(systemStatus);
                  } catch (Exception var3) {
                     if (BBacnetDevice.log.isLoggable(Level.FINE)) {
                        BBacnetDevice.log.log(Level.FINE, this + ": soft ping failed", (Throwable)var3);
                     }
                  }
               }
            }
         );
   }

   public void stopped() throws Exception {
      super.stopped();
      BBacnetNetwork.bacnet().unregisterDevice(this);

      try {
         BBacnetNetwork.localDevice().removeAddressBinding(this);
      } catch (NotRunningException var2) {
      }

      DeviceRegistry.remove(this.getObjectId());
   }

   public void changed(Property p, Context cx) {
      super.changed(p, cx);
      if (this.isRunning() && cx != fallback) {
         if (p.equals(status)) {
            this.getVirtual().updateStatus();
         } else if (p.equals(useCov) || p.equals(useCovProperty)) {
            this.tuningChanged(null, cx);
         } else if (p.equals(address)) {
            BBacnetAddress newAddress = this.getAddress();
            BBacnetNetwork network = BBacnetNetwork.bacnet();
            if (cx != noWrite) {
               if (this.oldAddress != null && newAddress != null) {
                  BBacnetOctetString oldMacAddr = this.oldAddress.getMacAddress();
                  if (oldMacAddr != null) {
                     byte[] oldMacAddrBytes = oldMacAddr.getBytes();
                     if (newAddress.equals(this.oldAddress.getNetworkNumber(), oldMacAddrBytes)) {
                        return;
                     }
                  }
               }

               BBacnetDevice d = network.doLookupDeviceByAddress(newAddress);
               if (d != null && d != this) {
                  log.severe("Duplicate Address:" + newAddress + ", used by " + d.getName() + "!\n  Resetting to old address:" + this.oldAddress);
                  this.set(address, this.oldAddress, fallback);
                  return;
               }

               if (log.isLoggable(Level.FINE)) {
                  log.fine("BacnetDevice " + this.getName() + " Address changed from " + this.oldAddress + " to " + newAddress);
               }

               this.upload(new BUploadParameters(false));
            }

            if (this.oldAddress == null) {
               this.oldAddress = BBacnetAddress.DEFAULT;
            }

            network.getLocalDevice().updateAddressBinding(this.oldAddress, newAddress);
            network.updateDevice(this);
            DeviceRegistry.update(this);
            this.oldAddress = (BBacnetAddress)newAddress.newCopy(true);
         } else if (p.getName().equals("vendorObjectTypesFile")) {
            this.network().postAsync(new Runnable() {
               @Override
               public void run() {
                  BBacnetDevice.this.getVendorObjectTypesList();
               }
            });
         }
      }
   }

   public void subscribed() {
      if (this.isRunning()) {
         if (!this.isDown()) {
            this.getConfig().getDeviceObject().loadSlots();
         }
      }
   }

   public boolean isParentLegal(BComponent parent) {
      return parent instanceof BBacnetNetwork || parent instanceof BBacnetDeviceFolder;
   }

   public Type getNetworkType() {
      return BBacnetNetwork.TYPE;
   }

   public void updateDeviceInfo(
      BBacnetObjectIdentifier objectId,
      BBacnetAddress address,
      int maxAPDULengthAccepted,
      BBacnetSegmentation segmentationSupported,
      int vendorId,
      BNetworkPort port
   ) {
      if (log.isLoggable(Level.FINE)) {
         log.fine("Update Device info: " + this);
      }

      this.maxAPDU = Math.max(50, maxAPDULengthAccepted);
      this.getConfig().getDeviceObject().set(BBacnetDeviceObject.maxAPDULengthAccepted, BBacnetUnsigned.make(this.maxAPDU), noWrite);
      this.setSegmentationSupported(segmentationSupported, noWrite);
      this.setVendorId(vendorId, noWrite);
      BBacnetAddress newAddress = (BBacnetAddress)address.newCopy();
      newAddress.setAddressType(this.getAddress().getAddressType());
      if (port != null && port.getNetworkNumber() == address.getNetworkNumber() && this.getAddress().getAddressType() == 0) {
         BBacnetLinkLayer link = port.getLink();
         if (link instanceof BBacnetIpLinkLayer) {
            newAddress.setAddressType(2);
         } else if (link instanceof BBacnetEthernetLinkLayer) {
            newAddress.setAddressType(1);
         } else if (link instanceof BBacnetMstpLinkLayer) {
            newAddress.setAddressType(3);
         } else if (link instanceof BScLinkLayer) {
            newAddress.setAddressType(4);
         }
      }

      if (!this.getAddress().equals(newAddress.getNetworkNumber(), newAddress.getMacAddress().getBytes())) {
         this.set(BBacnetDevice.address, newAddress, noWrite);
      }

      BStatus status = this.getStatus();
      if (status.isDown()) {
         if (!status.isDisabled()) {
            this.readSystemStatus();
         }
      } else if (!status.isDisabled()) {
         this.pingOk();
      }

      if (status.isStale()) {
         synchronized (this.DEVICE_LOCK) {
            if (this.staleTicket != null) {
               this.staleTicket.cancel();
            }
         }

         this.clearStaleFlag();
         this.updateStatus();
         this.postUpload();
      }

      DeviceRegistry.update(this);
   }

   public void doPing() {
      if (log.isLoggable(Level.FINE)) {
         log.fine("doPing on " + this);
      }

      BStatus status = this.getStatus();
      if ((status.getBits() & 3) == 0) {
         if (this.isAddressValid()) {
            long now = 0L;
            if (!this.isDown() || (now = Clock.ticks()) - this.lastPingTime > this.network().getLocalDevice().getDeviceTimeout()) {
               Runnable ping = new Runnable() {
                  @Override
                  public void run() {
                     long t0 = 0L;
                     if (BBacnetDevice.this.isRecordingLatency()) {
                        t0 = Clock.ticks();
                     }

                     BBacnetDevice.this.readSystemStatus();
                     BBacnetDevice.this.updateStatus();
                     if (BBacnetDevice.this.isDown() && ++BBacnetDevice.this.failedPings % BBacnetDevice.CHECK_ADDRESS_AFTER_PING_FAILS == 0) {
                        BBacnetDevice.this.checkAddress();
                        if (t0 > 0L) {
                           BBacnetDevice.this.recordLatency(Clock.ticks() - t0);
                        }
                     }
                  }
               };
               BBacnetNetwork net = this.network();
               if (net.getAsyncPing() && net.getWorker().hasWorkerPool()) {
                  net.postAsync(ping);
               } else {
                  ping.run();
               }

               this.lastPingTime = now;
            }
         } else {
            this.checkAddress();
         }
      }
   }

   void readSystemStatus() {
      try {
         byte[] encodedStatus = ((BBacnetStack)BBacnetNetwork.bacnet().getBacnetComm()).getClient().readProperty(this.getAddress(), this.getObjectId(), 112);
         if (encodedStatus == null) {
            if (log.isLoggable(Level.FINE)) {
               log.fine(this + ": read System Status returned null");
            }

            this.pingFail(lex.getText("BacnetDevice.ping.null"));
            return;
         }

         int systemStatus = AsnUtil.fromAsnEnumerated(encodedStatus);
         this.pingOk();
         this.updateSystemStatus(systemStatus);
      } catch (TransactionException var3) {
         log.info("TransactionException pinging " + this + ": " + var3);
         this.pingFail(var3.toString());
      } catch (AsnException var4) {
         log.log(Level.WARNING, "Unable to convert encoded System_Status!", (Throwable)var4);
         this.pingFail(var4.toString());
      } catch (BacnetException var5) {
         log.log(Level.WARNING, "BacnetException pinging " + this + ": " + var5, (Throwable)var5);
         this.pingFail(var5.toString());
      }
   }

   private void updateSystemStatus(int systemStatus) {
      if (!this.getEnumerationList().getDeviceStatusRange().isOrdinal(systemStatus)) {
         this.getEnumerationList().addNewDeviceStatus(systemStatus);
      }

      BBacnetDeviceObject deviceObj = this.getConfig().getDeviceObject();
      if (systemStatus != deviceObj.getSystemStatus().getOrdinal()) {
         deviceObj.set(BBacnetDeviceObject.systemStatus, BDynamicEnum.make(systemStatus, this.getEnumerationList().getDeviceStatusRange()), noWrite);
      }
   }

   public void pingOk() {
      super.pingOk();
      this.failedPings = 0;
      if (this.isFirstPingOk && this.uploadOnStart() && this.getStatus().isValid()) {
         if (log.isLoggable(Level.FINE)) {
            log.fine(this + ": uploading on first pingOk");
         }

         this.postUpload();
      }

      this.isFirstPingOk = false;
   }

   public void spy(SpyWriter out) throws Exception {
      super.spy(out);
      out.startProps();
      out.trTitle("BacnetDevice", 2);
      out.prop("rpmOk", this.rpmOk);
      out.prop("oldAddress", this.oldAddress);
      out.prop("isPolling", this.isPolling);
      out.prop("pollTimeouts", this.pollTimeouts);
      out.prop("maxAPDU", this.maxAPDU);
      out.prop("isAws", this.isAws);
      out.endProps();
   }

   public void doUpload(BUploadParameters p, Context cx) throws Exception {
      BStatus status = this.getStatus();
      if ((status.getBits() & 3) != 0) {
         if (log.isLoggable(Level.FINE)) {
            log.fine(this + " is either disabled or fault, device upload unsuccessful.");
         }
      } else {
         this.doPing();
         if (this.getStatus().isValid()) {
            this.uploadDevice(p, cx);
         }
      }
   }

   private void uploadDevice(BUploadParameters p, Context cx) throws Exception {
      this.getDeviceObject().readProperty(BBacnetDeviceObject.protocolServicesSupported);
      this.getConfig().doUpload(p, cx);
      if (this.isServiceSupported("readPropertyMultiple")) {
         this.rpmOk = true;
      } else {
         this.rpmOk = false;
      }

      this.maxAPDU = this.getDeviceObject().getMaxAPDULengthAccepted().getInt();
      boolean subCov = this.isServiceSupported("subscribeCov");
      int flags = this.getFlags(useCov);
      if (!subCov) {
         this.setUseCov(false);
      }

      flags = subCov ? flags & -2 : flags | 1;
      this.setFlags(useCov, flags);
      ((BBacnetScheduleDeviceExt)this.getSchedules()).setSupport(this.getProtocolRevision());
      if (log.isLoggable(Level.FINEST)) {
         log.finest(this + " device upload execution finish.");
      }
   }

   public void doDownload(BDownloadParameters p, Context cx) throws Exception {
      this.getConfig().doDownload(p, cx);
   }

   public boolean isPolling() {
      return this.isPolling;
   }

   public void setIsPolling(boolean isPolling) {
      this.isPolling = isPolling;
   }

   @Deprecated
   public void addPolledPoint(BBacnetProxyExt pt) {
      log.warning("BBacnetDevice.addPolledPoint(Ljavax/baja/bacnet/point/BBacnetProxyExt;) is DEPRECATED!");
   }

   @Deprecated
   public void removePolledPoint(BBacnetProxyExt pt) {
      log.warning("BBacnetDevice.removePolledPoint(Ljavax/baja/bacnet/point/BBacnetProxyExt;) is DEPRECATED!");
   }

   public void tuningChanged(BBacnetTuningPolicy policy, Context cx) {
      BControlPoint[] points = this.getPoints().getPoints();

      for (int i = 0; i < points.length; i++) {
         ((BBacnetProxyExt)points[i].getProxyExt()).tuningChanged(policy, cx);
      }
   }

   @Deprecated
   public boolean poll(BBacnetProxyExt pt) {
      log.warning("BBacnetDevice.poll(Ljavax/baja/bacnet/point/BBacnetProxyExt;) is DEPRECATED!");
      return true;
   }

   public boolean poll(PollList pl) {
      BStatus status = this.getStatus();
      if (this.getEnabled() && !status.isDown() && !status.isFault() && !status.isStale()) {
         if (!this.rpmOk) {
            if (log.isLoggable(Level.FINE)) {
               log.fine("BBacnetDevice#poll: Using readProperty instead of readPropertyMultiple because rpmOk is false; poll list: " + pl.debug());
            }

            this.pollRP(pl);
            return true;
         } else {
            PollListEntry[] entries = pl.getPollEntries();
            int len = entries.length;
            if (len == 0) {
               if (log.isLoggable(Level.FINE)) {
                  log.fine("BBacnetDevice#poll: Poll list has no entries; poll list: " + pl.debug());
               }

               return false;
            } else {
               Array<NReadAccessSpec> specs = new Array(NReadAccessSpec.class);
               int ndx = 0;
               PollListEntry e = entries[ndx++];
               NReadAccessSpec spec = new NReadAccessSpec(e.getObjectId(), e.getPropertyId(), e.getPropertyArrayIndex());
               int curHC = e.getObjectId().hashCode();

               while (ndx < len) {
                  e = entries[ndx++];
                  if (e == null) {
                     if (log.isLoggable(Level.FINE)) {
                        log.fine("BBacnetDevice#poll: Building ReadAccessSpec: Poll list entry is null at index " + (ndx - 1) + "; poll list: " + pl.debug());
                     }
                  } else if (e.getObjectId().getInstanceNumber() >= 0) {
                     if (curHC == e.getObjectId().hashCode()) {
                        spec.addPropertyReference(e.getPropertyId(), e.getPropertyArrayIndex());
                     } else {
                        specs.add(spec);
                        spec = new NReadAccessSpec(e.getObjectId(), e.getPropertyId(), e.getPropertyArrayIndex());
                        curHC = e.getObjectId().hashCode();
                     }
                  } else {
                     log.warning(
                        "BBacnetDevice#poll: Building ReadAccessSpec: Poll list entry at index "
                           + (ndx - 1)
                           + " has an invalid object instance number: "
                           + e.getObjectId().getInstanceNumber()
                           + "; poll list: "
                           + pl.debug()
                     );
                     BIBacnetPollable p = e.getPollable();
                     if (p instanceof BBacnetObject) {
                        BBacnetObject obj = (BBacnetObject)p;
                        obj.setStatus(BStatus.fault);
                        obj.setFaultCause(TextUtil.toFriendly(BBacnetErrorCode.tag(31)));
                     }
                  }
               }

               if (spec.getObjectId().getInstanceNumber() >= 0) {
                  specs.add(spec);
               } else {
                  log.warning(
                     "BBacnetDevice#poll: Building ReadAccessSpec: Poll list entry at index "
                        + (ndx - 1)
                        + " has an invalid object instance number: "
                        + e.getObjectId().getInstanceNumber()
                        + "; poll list: "
                        + pl.debug()
                  );
               }

               if (specs.isEmpty()) {
                  if (log.isLoggable(Level.FINE)) {
                     log.fine("BBacnetDevice#poll: Poll list entries did not result in any read access specs; poll list: " + pl.debug());
                  }

                  return true;
               } else {
                  Iterator resultList = null;

                  try {
                     long t0 = 0L;
                     if (this.isRecordingLatency()) {
                        t0 = Clock.ticks();
                     }

                     resultList = this.client().readPropertyMultiple(this.getAddress(), specs);
                     if (t0 > 0L) {
                        this.recordLatency(Clock.ticks() - t0);
                     }
                  } catch (TransactionException var14) {
                     TransactionException x = var14;
                     if (log.isLoggable(Level.FINE)) {
                        log.log(Level.INFO, "TransactionException in poll() for " + this + "; poll list = " + pl, (Throwable)var14);
                     } else {
                        log.log(Level.INFO, "TransactionException in poll() for " + this + "; exception = " + var14 + "; poll list = " + pl);
                     }

                     this.ping();
                     if (++this.pollTimeouts > this.getMaxPollTimeouts()) {
                        for (int i = 0; i < len; i++) {
                           e = entries[i];
                           if (e != null) {
                              e.getPollable().readFail(x.toString());
                           }
                        }
                     }

                     return false;
                  } catch (ErrorException var15) {
                     ErrorException xx = var15;
                     this.pollTimeouts = 0;

                     for (int ix = 0; ix < len; ix++) {
                        e = entries[ix];
                        if (e != null) {
                           BIBacnetPollable p = e.getPollable();
                           if (p instanceof BBacnetProxyExt) {
                              ((BBacnetProxyExt)p).setLastReadError(xx.getErrorType());
                           }

                           plog.log(
                              Level.SEVERE,
                              "Bacnet Error polling " + pl + "; entry " + e + " in " + this.getName() + " {" + this.getObjectId() + "}: " + xx,
                              (Throwable)xx
                           );
                           e.getPollable().readFail(xx.toString());
                        }
                     }

                     return true;
                  } catch (AbortException var16) {
                     if (log.isLoggable(Level.FINE)) {
                        log.log(Level.INFO, "AbortException in poll() for " + this + "; poll list = " + pl, (Throwable)var16);
                     } else {
                        log.log(Level.INFO, "AbortException in poll() for " + this + "; exception = " + var16 + "; poll list = " + pl);
                     }

                     this.pollTimeouts = 0;
                     if (var16.getAbortReason() == 4 || var16.getAbortReason() == 0) {
                        return false;
                     }
                  } catch (RejectException var17) {
                     if (log.isLoggable(Level.FINE)) {
                        log.log(Level.INFO, "RejectException in poll() for " + this + "; poll list = " + pl, (Throwable)var17);
                     } else {
                        log.log(Level.INFO, "RejectException in poll() for " + this + "; exception = " + var17 + "; poll list = " + pl);
                     }

                     this.pollTimeouts = 0;
                     if (var17.getRejectReason() == 9) {
                        this.rpmOk = false;
                        plog.info("REJECT:Unrecognized service - switch polling to ReadProperty");
                        this.pollRP(pl);
                        return true;
                     }

                     if (var17.getRejectReason() == 1) {
                        this.maxAPDU -= 5;
                        if (this.maxAPDU <= 50) {
                           this.rpmOk = false;
                        }

                        plog.info("REJECT:Buffer overflow - decreasing max APDU size to " + this.maxAPDU);
                        return false;
                     }
                  } catch (BacnetException var18) {
                     this.pollTimeouts = 0;
                     plog.log(
                        Level.SEVERE, "BacnetException polling " + pl + " in " + this.getName() + " {" + this.getObjectId() + "}: " + var18, (Throwable)var18
                     );
                     return false;
                  }

                  if (resultList == null) {
                     if (plog.isLoggable(Level.INFO)) {
                        plog.info("BBacnetDevice#poll: Poll resultList is empty!; poll list = " + pl);
                     }

                     return false;
                  } else {
                     this.pingOk();
                     this.pollTimeouts = 0;
                     ndx = 0;

                     while (resultList.hasNext() && ndx < entries.length) {
                        NReadPropertyResult rpr = (NReadPropertyResult)resultList.next();
                        e = entries[ndx++];
                        if (e == null) {
                           if (log.isLoggable(Level.FINE)) {
                              log.fine("Iterating ReadAccessResults: PLE is null in  slot " + (ndx - 1));
                           }
                        } else {
                           if (!e.getObjectId().equals(rpr.getObjectId())
                              || e.getPropertyId() != rpr.getPropertyId()
                              || e.getPropertyArrayIndex() != rpr.getPropertyArrayIndex()) {
                              plog.info("Mismatch between PollListEntry and ReadPropertyResult:\n  ple=" + e.debugString() + "\n  rpr=" + rpr.debug());
                              return false;
                           }

                           if (rpr.isError()) {
                              if (log.isLoggable(Level.FINE)) {
                                 log.fine(
                                    "BBacnetDevice#poll: Read property error; poll list entry = "
                                       + e.debugString()
                                       + "; read property result = "
                                       + rpr.debug()
                                       + "; poll list = "
                                       + pl.debug()
                                 );
                              }

                              BIBacnetPollable p = e.getPollable();
                              if (p instanceof BBacnetProxyExt) {
                                 ((BBacnetProxyExt)p).setLastReadError(rpr.getPropertyAccessError());
                              }

                              e.getPollable().readFail(NErrorType.toString(rpr.getErrorClass(), rpr.getErrorCode()));
                           } else {
                              e.getPollable().fromEncodedValue(rpr.getPropertyValue(), null, e);
                           }
                        }
                     }

                     return true;
                  }
               }
            }
         }
      } else {
         if (log.isLoggable(Level.FINE)) {
            log.fine(
               "BBacnetDevice#poll: Poll list skipped because device is disabled or has an invalid status; device enabled: "
                  + this.getEnabled()
                  + "; device status: "
                  + status
                  + "; poll list: "
                  + pl.debug()
            );
         }

         return true;
      }
   }

   private void pollRP(PollList pl) {
      PollListEntry[] entries = pl.getPollEntries();
      int len = entries.length;

      for (int i = 0; i < len; i++) {
         PollListEntry e = entries[i];
         if (e == null) {
            if (log.isLoggable(Level.FINE)) {
               log.fine("pollRP: PLE is null in  slot " + i);
            }
         } else {
            byte[] encodedValue = null;

            try {
               long t0 = 0L;
               if (this.isRecordingLatency()) {
                  t0 = Clock.ticks();
               }

               encodedValue = this.client().readProperty(this.getAddress(), e.getObjectId(), e.getPropertyId(), e.getPropertyArrayIndex());
               if (t0 > 0L) {
                  this.recordLatency(Clock.ticks() - t0);
               }

               this.pingOk();
               e.getPollable().fromEncodedValue(encodedValue, null, e);
            } catch (TransactionException var9) {
               e.getPollable().readFail(var9.toString());
               this.ping();
               if (this.getStatus().isDown()) {
                  break;
               }
            } catch (ErrorException var10) {
               BIBacnetPollable p = e.getPollable();
               if (p instanceof BBacnetProxyExt) {
                  ((BBacnetProxyExt)p).setLastReadError(var10.getErrorType());
               }

               plog.log(
                  Level.SEVERE,
                  "Bacnet Error polling PollList " + pl + " entry " + e + " in " + this.getName() + " {" + this.getObjectId() + "}: " + var10,
                  (Throwable)var10
               );
               e.getPollable().readFail(var10.toString());
            } catch (BacnetException var11) {
               plog.log(
                  Level.SEVERE,
                  "BacnetException polling PollList " + pl + " entry " + e + " in " + this.getName() + " {" + this.getObjectId() + "}: " + var11,
                  (Throwable)var11
               );
               e.getPollable().readFail(var11.toString());
            }
         }
      }
   }

   @Deprecated
   public int countPolledPoints() {
      log.warning("BBacnetDevice.countPolledPoints()I is DEPRECATED!");
      return 0;
   }

   public boolean canAddCov() {
      return this.getUseCov() && this.getCovSubscriptions() < this.getMaxCovSubscriptions();
   }

   public boolean canAddCovProperty() {
      return this.getUseCovProperty() && this.getCovSubscriptions() < this.getMaxCovSubscriptions();
   }

   public boolean subscribeCov(BBacnetProxyExt pt) {
      if (!this.device().isOperational()) {
         return false;
      } else {
         boolean covOK = true;
         if (!this.canAddCov()) {
            covOK = false;
         } else {
            int subLife = this.calculateSubcriptionLifetime(pt.getCovSubscriptionLifetime());

            try {
               this.client().subscribeCov(this.getAddress(), 1L, pt.getObjectId(), pt.useConfirmedCov(), subLife);
               this.pingOk();
            } catch (BacnetException var5) {
               if (this.getDisableDeviceOnCovSubscriptionFailure() && var5 instanceof TransactionTimeoutException) {
                  this.device().pingFail(lex.get("bacnetDevice.subscribeCov.failure"));
                  log.severe(this + ": Timeout detected, marking the device down due to no response.");
               }

               plog.log(
                  Level.SEVERE,
                  this + ": BacnetException sending SubscribeCov for " + pt.getObjectId() + " in " + this.getObjectId() + ": " + var5,
                  (Throwable)var5
               );
               covOK = false;
            }
         }

         BBacnetPoll pollService = (BBacnetPoll)this.network().getPollService(pt);
         if (covOK) {
            pollService.unsubscribe(pt);
            if (!pt.isCOV()) {
               this.setCovSubscriptions(this.getCovSubscriptions() + 1);
            }
         } else {
            if (!pt.isPolled()) {
               pollService.subscribe(pt);
            }

            if (pt.isCOV()) {
               this.setCovSubscriptions(this.getCovSubscriptions() - 1);
            }
         }

         pt.setSubState(covOK ? 2 : 4);
         return covOK;
      }
   }

   public boolean subscribeCovProperty(BBacnetProxyExt pt) {
      if (!this.device().isOperational()) {
         return false;
      } else {
         boolean covPropertyOK = true;
         if (!this.canAddCovProperty()) {
            covPropertyOK = false;
         } else {
            int subLife = this.calculateSubcriptionLifetime(pt.getCovPropertySubscriptionLifetime());

            try {
               this.client()
                  .subscribeCovProperty(
                     this.getAddress(),
                     1L,
                     pt.getObjectId(),
                     pt.useConfirmedCovProperty(),
                     subLife,
                     new BBacnetPropertyReference(pt.getPropertyId().getOrdinal(), pt.getPropertyArrayIndex()),
                     pt.getCovPropertyIncrement()
                  );
               this.pingOk();
            } catch (BacnetException var5) {
               if (this.getDisableDeviceOnCovSubscriptionFailure() && var5 instanceof TransactionTimeoutException) {
                  this.device().pingFail(lex.get("bacnetDevice.subscribeCovProperty.failure"));
                  log.severe(this + ": Timeout detected, marking the device down due to no response.");
               }

               plog.log(
                  Level.SEVERE,
                  this + ": BacnetException sending SubscribeCovProperty for " + pt.getObjectId() + " in " + this.getObjectId() + ": " + var5,
                  (Throwable)var5
               );
               covPropertyOK = false;
            }
         }

         BBacnetPoll pollService = (BBacnetPoll)this.network().getPollService(pt);
         if (covPropertyOK) {
            pollService.unsubscribe(pt);
            if (!pt.isCOVProperty()) {
               this.setCovSubscriptions(this.getCovSubscriptions() + 1);
            }

            pt.setSubState(7);
         } else {
            if (!pt.isPolled()) {
               pollService.subscribe(pt);
            }

            if (pt.isCOVProperty()) {
               this.setCovSubscriptions(this.getCovSubscriptions() - 1);
            }

            pt.setSubState(4);
         }

         return covPropertyOK;
      }
   }

   public void unsubscribeCov(BBacnetProxyExt pt) {
      if (!this.device().isOperational()) {
         this.setCovSubscriptions(this.getCovSubscriptions() - 1);
      } else {
         BBacnetObjectIdentifier objectId = pt.getObjectId();

         try {
            this.client().unsubscribeCov(this.getAddress(), 1L, objectId);
            this.setCovSubscriptions(this.getCovSubscriptions() - 1);
         } catch (BacnetException var4) {
            if (this.getDisableDeviceOnCovSubscriptionFailure() && var4 instanceof TransactionTimeoutException) {
               this.device().pingFail(lex.get("bacnetDevice.unsubscribeCov.failure"));
               log.severe(this + ": Timeout detected, marking the device down due to no response.");
            }

            plog.log(
               Level.SEVERE,
               this + ": BacnetException cancelling Cov subscription for " + objectId + " in " + this.getObjectId() + ": " + var4,
               (Throwable)var4
            );
         }
      }
   }

   public void unsubscribeCovProperty(BBacnetProxyExt pt) {
      if (!this.device().isOperational()) {
         this.setCovSubscriptions(this.getCovSubscriptions() - 1);
      } else {
         BBacnetObjectIdentifier objectId = pt.getObjectId();
         BBacnetPropertyReference propertyReference = new BBacnetPropertyReference(pt.getPropertyId().getOrdinal(), pt.getPropertyArrayIndex());

         try {
            this.client().unsubscribeCovProperty(this.getAddress(), 1L, objectId, propertyReference);
            this.setCovSubscriptions(this.getCovSubscriptions() - 1);
         } catch (BacnetException var5) {
            if (this.getDisableDeviceOnCovSubscriptionFailure() && var5 instanceof TransactionTimeoutException) {
               this.device().pingFail(lex.get("bacnetDevice.unsubscribeCovProperty.failure"));
               log.severe(this + ": Timeout detected, marking the device down due to no response.");
            }

            plog.log(
               Level.SEVERE,
               this
                  + ": BacnetException cancelling Cov Property subscription for "
                  + objectId
                  + " in "
                  + this.getObjectId()
                  + " Property Reference  "
                  + propertyReference
                  + ": "
                  + var5,
               (Throwable)var5
            );
         }
      }
   }

   public final PropertyInfo getPropertyInfo(int objectType, int propId) {
      PropertyInfo propInfo = null;

      try {
         propInfo = this.getVendorPropertyInfo(objectType, propId);
      } catch (Exception var6) {
      }

      try {
         if (propInfo == null) {
            propInfo = ObjectTypeList.getInstance().getPropertyInfo(objectType, propId);
         }
      } catch (Exception var5) {
      }

      if (propInfo != null && propInfo.isAws() && !this.isAws()) {
         propInfo = null;
      }

      if (propInfo == null) {
         propInfo = new PropertyInfo(BBacnetPropertyIdentifier.tag(propId), propId, -6);
      }

      return propInfo;
   }

   public int[] getPossibleProperties(BBacnetObjectIdentifier objectId) {
      int revisionId = -1;

      try {
         this.getDeviceObject().readProperty(BBacnetDeviceObject.protocolRevision);
         revisionId = this.getDeviceObject().getProtocolRevision().getInt();
      } catch (Exception var4) {
         log.log(Level.INFO, "Could not read the protocol revision from the device object.", (Throwable)var4);
      }

      int[] possibleProperties = ObjectTypeList.getInstance().getPossibleProperties(objectId, revisionId);
      return this.removePropertyFromArray(possibleProperties, 139);
   }

   private int[] removePropertyFromArray(int[] possibleProperties, int propToRemove) {
      for (int index = 0; index < possibleProperties.length; index++) {
         if (possibleProperties[index] == propToRemove) {
            int[] newPossibleProperties = new int[possibleProperties.length - 1];
            System.arraycopy(possibleProperties, 0, newPossibleProperties, 0, index);
            System.arraycopy(possibleProperties, index + 1, newPossibleProperties, index, possibleProperties.length - index - 1);
            return newPossibleProperties;
         }
      }

      return possibleProperties;
   }

   public int[] getRequiredProperties(BBacnetObjectIdentifier objectId) {
      return ObjectTypeList.getInstance().getRequiredProperties(objectId);
   }

   protected PropertyInfo getVendorPropertyInfo(int objectType, int propertyId) {
      return this.vendorObjectTypesList != null ? this.vendorObjectTypesList.getPropertyInfo(objectType, propertyId) : null;
   }

   private ObjectTypeList getVendorObjectTypesList() {
      BOrd o = BOrd.NULL;
      if (this.vendorObjectTypesList == null) {
         try {
            o = (BOrd)this.get("vendorObjectTypesFile");
            if (o != null && !o.equals(BOrd.NULL)) {
               this.vendorObjectTypesList = ObjectTypeList.make(o);
            }
         } catch (ClassCastException var3) {
            log.log(Level.INFO, "vendorObjectTypesFile must be a BOrd." + var3, (Throwable)var3);
         } catch (Exception var4) {
            log.log(Level.INFO, "Unable to build vendor object types list from file:" + o, (Throwable)var4);
         }
      }

      return this.vendorObjectTypesList;
   }

   public BEnumRange getEnumRange(int objectType, int propertyId) {
      PropertyInfo pi = this.getPropertyInfo(objectType, propertyId);
      if (pi.isEnum()) {
         if (pi.isExtensible()) {
            return this.getEnumerationList().getEnumRange(pi.getType());
         } else {
            BTypeSpec tspec = BTypeSpec.make(pi.getType());
            return ((BEnum)tspec.getInstance()).getRange();
         }
      } else {
         return null;
      }
   }

   public boolean isOperational() {
      BStatus status = this.getStatus();
      if (status == null) {
         status = BStatus.down;
      }

      return !status.isDown() && !status.isDisabled() && !status.isFault();
   }

   @Override
   public BObject lookupBacnetObject(BBacnetObjectIdentifier objectId, int propertyId, int propertyArrayIndex, String domain) {
      if (!this.isRunning()) {
         this.loadSlots();
      }

      if (domain == null || domain.equals("point")) {
         return this.getPoints().lookupBacnetObject(objectId, propertyId, propertyArrayIndex, domain);
      } else if (domain.equals("schedule")) {
         return ((BBacnetScheduleDeviceExt)this.getSchedules()).lookupBacnetObject(objectId, propertyId, propertyArrayIndex, domain);
      } else if (domain.equals("history")) {
         return null;
      } else {
         return domain.equals("config") ? this.getConfig().lookupBacnetObject(objectId, propertyId, propertyArrayIndex, domain) : null;
      }
   }

   public void doMacAddressFailed() {
      if (log.isLoggable(Level.FINE)) {
         log.fine("Cannot resolve mac address for: " + this);
      }

      synchronized (this.DEVICE_LOCK) {
         this.staleTicket = null;
      }

      this.clearStaleFlag();
      this.pingFail("Cannot resolve MAC address");
      this.updateStatus();
   }

   @Override
   public boolean addDeviceOverride(DeviceOverride override) {
      synchronized (this.DEVICE_LOCK) {
         if (this.overrides == null) {
            this.overrides = Collections.synchronizedList(new ArrayList<>());
         }
      }

      return this.overrides.add(override);
   }

   @Override
   public boolean removeDeviceOverride(DeviceOverride override) {
      return this.overrides == null ? false : this.overrides.remove(override);
   }

   private int calculateSubcriptionLifetime(int subLife) {
      subLife *= 60;
      if (subLife < 0 || subLife > 0 && subLife < 300) {
         subLife = 300;
      }

      return subLife;
   }

   private int getOverridenAdpuSize(int claimed) {
      int smallest = claimed;
      if (this.overrides != null) {
         for (DeviceOverride override : this.overrides) {
            if (override instanceof ApduSizeOverride) {
               ApduSizeOverride current = (ApduSizeOverride)override;
               smallest = Math.min(smallest, current.getMaxAPDULengthAccepted(this.getDeviceObject()));
            }
         }
      }

      return smallest;
   }

   private SegmentationOverride getSegmentationOverride() {
      if (this.overrides != null) {
         for (DeviceOverride override : this.overrides) {
            if (override instanceof SegmentationOverride) {
               return (SegmentationOverride)override;
            }
         }
      }

      return null;
   }

   private BBacnetBitString getServicesSupported(BBacnetBitString claimed) {
      BBacnetBitString merged = claimed;
      if (this.overrides != null) {
         for (DeviceOverride override : this.overrides) {
            if (override instanceof ServiceOverride) {
               ServiceOverride sso = (ServiceOverride)override;
               merged = sso.getProtocolServicesSupported(this.getDeviceObject(), merged);
            }
         }
      }

      return merged;
   }

   private void clearStaleFlag() {
      BStatus status = this.getStatus();
      this.setStatus(BStatus.make(status.getBits() & -17, status.getFacets()));
   }

   @Override
   public boolean addLatencyRecorder(LatencyRecorder recorder) {
      if (recorder != null) {
         synchronized (this.DEVICE_LOCK) {
            if (this.recorders == null) {
               this.recorders = Collections.synchronizedList(new ArrayList<>());
            }
         }

         return this.recorders.add(recorder);
      } else {
         return false;
      }
   }

   @Override
   public boolean removeLatencyRecorder(LatencyRecorder recorder) {
      return recorder != null && this.recorders != null ? this.recorders.remove(recorder) : false;
   }

   @Override
   public void recordLatency(long ms) {
      if (this.recorders != null) {
         for (LatencyRecorder r : this.recorders) {
            r.recordLatency(ms);
         }
      }
   }

   @Override
   public boolean isRecordingLatency() {
      if (this.recorders != null) {
         for (LatencyRecorder r : this.recorders) {
            if (r.isRecordingLatency()) {
               return true;
            }
         }
      }

      return false;
   }
}
