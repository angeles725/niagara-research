package com.tridium.bacnet.history;

import com.tridium.bacnet.ObjectTypeList;
import com.tridium.bacnet.asn.AsnInputStream;
import com.tridium.bacnet.asn.AsnUtil;
import com.tridium.bacnet.job.BacnetDiscoveryUtil;
import com.tridium.bacnet.stack.BBacnetStack;
import com.tridium.bacnet.stack.DeviceRegistry;
import com.tridium.bacnet.stack.client.BBacnetClientLayer;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.bacnet.BBacnetDevice;
import javax.baja.bacnet.BBacnetNetwork;
import javax.baja.bacnet.BacnetConst;
import javax.baja.bacnet.BacnetException;
import javax.baja.bacnet.datatypes.BBacnetAddress;
import javax.baja.bacnet.datatypes.BBacnetDateTime;
import javax.baja.bacnet.datatypes.BBacnetDeviceObjectPropertyReference;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.util.PropertyInfo;
import javax.baja.control.trigger.BManualTriggerMode;
import javax.baja.data.BIDataValue;
import javax.baja.driver.history.ArchiveException;
import javax.baja.driver.history.BHistoryImport;
import javax.baja.driver.util.BDescriptorState;
import javax.baja.history.BCollectionInterval;
import javax.baja.history.BHistoryConfig;
import javax.baja.history.BHistoryId;
import javax.baja.history.BHistoryRecord;
import javax.baja.history.BHistoryService;
import javax.baja.history.BIHistory;
import javax.baja.history.HistorySpaceConnection;
import javax.baja.history.db.BHistoryDatabase;
import javax.baja.history.db.HistoryDatabaseConnection;
import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.util.ByteArrayUtil;
import javax.baja.sys.Action;
import javax.baja.sys.BAbsTime;
import javax.baja.sys.BComponent;
import javax.baja.sys.BComponentEvent;
import javax.baja.sys.BFacets;
import javax.baja.sys.BIService;
import javax.baja.sys.BLong;
import javax.baja.sys.BRelTime;
import javax.baja.sys.BString;
import javax.baja.sys.BValue;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.ServiceNotFoundException;
import javax.baja.sys.Subscriber;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.timezone.BTimeZone;
import javax.baja.util.BFormat;
import javax.baja.util.BTypeSpec;
import javax.baja.util.IFuture;
import javax.baja.util.Lexicon;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "historyId",
      type = "BHistoryId",
      defaultValue = "BHistoryId.NULL",
      flags = 1,
      override = true
   ), @NiagaraProperty(
      name = "objectId",
      type = "BBacnetObjectIdentifier",
      defaultValue = "BBacnetObjectIdentifier.make(BBacnetObjectType.TREND_LOG)"
   ), @NiagaraProperty(
      name = "localHistoryName",
      type = "String",
      defaultValue = "",
      flags = 1
   ), @NiagaraProperty(
      name = "localHistoryNameFormat",
      type = "BFormat",
      defaultValue = "BFormat.make(\"%name%\")"
   ), @NiagaraProperty(
      name = "referenceTime",
      type = "BBacnetDateTime",
      defaultValue = "new BBacnetDateTime(BAbsTime.make(0, BTimeZone.UTC))"
   ), @NiagaraProperty(
      name = "maxRecordsPerRequest",
      type = "int",
      defaultValue = "0",
      facets = {@Facet("BFacets.makeInt(null, 0, Integer.MAX_VALUE)")}
   ), @NiagaraProperty(
      name = "alwaysRequestByReferenceTime",
      type = "boolean",
      defaultValue = "false"
   ), @NiagaraProperty(
      name = "lastSequenceNumberProcessed",
      type = "long",
      defaultValue = "BLong.make(0)"
   ), @NiagaraProperty(
      name = "maxStartingEvents",
      type = "int",
      defaultValue = "3",
      flags = 4
   ), @NiagaraProperty(
      name = "discoveryHistoryType",
      type = "String",
      defaultValue = "BString.make(UNKNOWN)",
      flags = 5
   )})
@NiagaraAction(
   name = "clearRecordsInDevice",
   flags = 128
)
public abstract class BAbstractBacnetHistory extends BHistoryImport implements BacnetConst {
   public static final Lexicon lex = Lexicon.make("bacnet");
   private static final String UNKNOWN = lex.get("historyType.unknown", "unknown");
   public static final Property historyId = newProperty(1, BHistoryId.NULL, null);
   public static final Property objectId = newProperty(0, BBacnetObjectIdentifier.make(20), null);
   public static final Property localHistoryName = newProperty(1, "", null);
   public static final Property localHistoryNameFormat = newProperty(0, BFormat.make("%name%"), null);
   public static final Property referenceTime = newProperty(0, new BBacnetDateTime(BAbsTime.make(0L, BTimeZone.UTC)), null);
   public static final Property maxRecordsPerRequest = newProperty(0, 0, BFacets.makeInt(null, 0, Integer.MAX_VALUE));
   public static final Property alwaysRequestByReferenceTime = newProperty(0, false, null);
   public static final Property lastSequenceNumberProcessed = newProperty(0, BLong.make(0L), null);
   public static final Property maxStartingEvents = newProperty(4, 3, null);
   public static final Property discoveryHistoryType = newProperty(5, BString.make(UNKNOWN), null);
   public static final Action clearRecordsInDevice = newAction(128, null);
   public static final Type TYPE = Sys.loadType(BAbstractBacnetHistory.class);
   public static final Logger logger = Logger.getLogger("bacnet.history");
   protected AsnInputStream asnIn = new AsnInputStream();
   protected boolean bufferReady = false;
   protected boolean formatChecked = false;
   protected boolean overridesConfigured = false;
   protected byte[] prev = null;
   protected String prevNam = null;
   protected boolean oprChange = true;
   private BAbstractBacnetHistory.BacnetHistoryImportSubscriber bacnetHistoryImportSubscriber;

   public BBacnetObjectIdentifier getObjectId() {
      return (BBacnetObjectIdentifier)this.get(objectId);
   }

   public void setObjectId(BBacnetObjectIdentifier v) {
      this.set(objectId, v, null);
   }

   public String getLocalHistoryName() {
      return this.getString(localHistoryName);
   }

   public void setLocalHistoryName(String v) {
      this.setString(localHistoryName, v, null);
   }

   public BFormat getLocalHistoryNameFormat() {
      return (BFormat)this.get(localHistoryNameFormat);
   }

   public void setLocalHistoryNameFormat(BFormat v) {
      this.set(localHistoryNameFormat, v, null);
   }

   public BBacnetDateTime getReferenceTime() {
      return (BBacnetDateTime)this.get(referenceTime);
   }

   public void setReferenceTime(BBacnetDateTime v) {
      this.set(referenceTime, v, null);
   }

   public int getMaxRecordsPerRequest() {
      return this.getInt(maxRecordsPerRequest);
   }

   public void setMaxRecordsPerRequest(int v) {
      this.setInt(maxRecordsPerRequest, v, null);
   }

   public boolean getAlwaysRequestByReferenceTime() {
      return this.getBoolean(alwaysRequestByReferenceTime);
   }

   public void setAlwaysRequestByReferenceTime(boolean v) {
      this.setBoolean(alwaysRequestByReferenceTime, v, null);
   }

   public long getLastSequenceNumberProcessed() {
      return this.getLong(lastSequenceNumberProcessed);
   }

   public void setLastSequenceNumberProcessed(long v) {
      this.setLong(lastSequenceNumberProcessed, v, null);
   }

   public int getMaxStartingEvents() {
      return this.getInt(maxStartingEvents);
   }

   public void setMaxStartingEvents(int v) {
      this.setInt(maxStartingEvents, v, null);
   }

   public String getDiscoveryHistoryType() {
      return this.getString(discoveryHistoryType);
   }

   public void setDiscoveryHistoryType(String v) {
      this.setString(discoveryHistoryType, v, null);
   }

   public void clearRecordsInDevice() {
      this.invoke(clearRecordsInDevice, null, null);
   }

   public Type getType() {
      return TYPE;
   }

   public final BBacnetDevice device() {
      return (BBacnetDevice)this.getDeviceExt().getDevice();
   }

   public final BBacnetHistoryDeviceExt deviceExt() {
      return (BBacnetHistoryDeviceExt)this.getDeviceExt();
   }

   public void started() throws Exception {
      super.started();
      if (Sys.atSteadyState() && this.isEnabled()) {
         this.postConfigureOverrides();
         if (this.getExecutionTime().getTriggerMode() != BManualTriggerMode.DEFAULT) {
            this.execute();
         }
      }

      this.bacnetHistoryImportSubscriber = new BAbstractBacnetHistory.BacnetHistoryImportSubscriber();
      this.bacnetHistoryImportSubscriber.subscribe(this.getConfigOverrides());
   }

   public boolean isEnabled() {
      return super.isEnabled() && this.device().getEnabled() && !this.device().isFault();
   }

   public void atSteadyState() throws Exception {
      super.atSteadyState();
      if (this.isEnabled()) {
         this.postConfigureOverrides();
         if (this.getExecutionTime().getTriggerMode() != BManualTriggerMode.DEFAULT) {
            this.execute();
         }
      }
   }

   public void changed(Property p, Context cx) {
      super.changed(p, cx);
      if (this.isRunning()) {
         if (p.equals(status) && Sys.atSteadyState() && this.isEnabled() && !this.overridesConfigured) {
            this.postConfigureOverrides();
         }

         if (p.getName().equals(BHistoryImport.configOverrides.getName())) {
            this.saveConfigOverride();
         }
      }
   }

   protected BBacnetClientLayer client() {
      return ((BBacnetStack)BBacnetNetwork.bacnet().getBacnetComm()).getClient();
   }

   public void doClearRecordsInDevice() {
      this.doClearDeviceRecords();
   }

   public boolean isBufferReady() {
      return this.bufferReady;
   }

   public void setBufferReady(boolean val) {
      this.bufferReady = val;
   }

   protected IFuture postExecute(Action archiveAction, BValue arg, Context cx) {
      try {
         return BBacnetNetwork.bacnet().postAsync(new BAbstractBacnetHistory.AsyncImport());
      } catch (Exception var5) {
         this.executeFail(var5);
         return null;
      }
   }

   protected void postConfigureOverrides() {
      try {
         if (this.device().isOperational()) {
            BBacnetNetwork.bacnet().postAsync(new Runnable() {
               @Override
               public void run() {
                  BAbstractBacnetHistory.this.configureOverrides();
               }
            });
         }
      } catch (Exception var2) {
         logger.log(Level.SEVERE, "Failed to invoke postConfigureOverrides", (Throwable)var2);
      }
   }

   protected synchronized void configureOverrides() {
      if (!this.overridesConfigured) {
         if (this.device().isOperational()) {
            BComponent overrides = this.getConfigOverrides();
            this.setTimeZone(overrides);
            this.setLogInterval(overrides);
            this.setValueFacets(overrides);
         }

         this.overridesConfigured = true;
      }
   }

   private void setTimeZone(BComponent overrides) {
      BTimeZone timeZone = this.deviceExt().getTimeZone();
      if (timeZone == null) {
         logger.warning("TimeZone determined from TimeZoneDatabase and device values is null, resetting to default!");
         timeZone = BTimeZone.getLocal();
         Property tzprop = overrides.getProperty("timeZone");
         if (tzprop == null) {
            overrides.add("timeZone", timeZone, 0, BFacets.make("fieldEditor", BString.make("driver:TimeZoneSelectionFE")), null);
         } else {
            overrides.set(tzprop, timeZone);
            overrides.setFlags(tzprop, overrides.getFlags(tzprop) & -2);
            overrides.setFacets(tzprop, BFacets.make(overrides.getSlotFacets(tzprop), "fieldEditor", BString.make("driver:TimeZoneSelectionFE")));
         }
      } else if (overrides.getProperty("timeZone") == null) {
         overrides.add("timeZone", timeZone, 0, BFacets.make("fieldEditor", BString.make("driver:TimeZoneSelectionFE")), null);
      }
   }

   private void setLogInterval(BComponent overrides) {
      try {
         byte[] encodedValue = this.client().readProperty(this.device().getAddress(), this.getObjectId(), 134);
         long logInterval = AsnUtil.fromAsnUnsignedInteger(encodedValue);
         BCollectionInterval interval = BCollectionInterval.IRREGULAR;
         if (logInterval >= 0L) {
            interval = BCollectionInterval.make(BRelTime.make(logInterval * 10L));
         }

         setOrAdd(overrides, "interval", interval);
      } catch (BacnetException var6) {
         logger.info("Unable to read Log_Interval from " + this.getObjectId() + " in " + this.device() + ":" + var6);
      }
   }

   private void setValueFacets(BComponent overrides) {
      BBacnetDeviceObjectPropertyReference dopr = this.getDeviceObjectProp();
      if (dopr != null && hasValueFacets(dopr)) {
         BBacnetObjectIdentifier deviceId = dopr.getDeviceId();
         if (deviceId != null) {
            if (deviceId == BBacnetObjectIdentifier.DEFAULT_DEVICE) {
               deviceId = this.device().getObjectId();
            }

            BBacnetAddress address = DeviceRegistry.getDeviceAddress(deviceId);
            if (address != null) {
               Map<String, BIDataValue> map = BacnetDiscoveryUtil.discoverFacets(dopr.getObjectId(), address);
               if (map != null) {
                  BFacets valueFacets = BFacets.make(map);
                  if (valueFacets != null) {
                     setOrAdd(overrides, "valueFacets", valueFacets);
                  }
               }
            }
         }
      }
   }

   private static boolean hasValueFacets(BBacnetDeviceObjectPropertyReference dopr) {
      if (dopr.getPropertyId() == 85 && dopr.getPropertyArrayIndex() == -1) {
         switch (dopr.getObjectId().getObjectType()) {
            case 0:
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
            case 13:
            case 14:
            case 19:
               return true;
            case 6:
            case 7:
            case 8:
            case 9:
            case 10:
            case 11:
            case 12:
            case 15:
            case 16:
            case 17:
            case 18:
         }
      }

      return false;
   }

   public BHistoryRecord correctTimestamp(BHistoryRecord rec) {
      BTimeZone timeZone = this.deviceExt().getTimeZone();
      if (timeZone == null) {
         return rec;
      } else {
         BAbsTime ts = rec.getTimestamp();
         BRelTime tsOff = BRelTime.make(ts.getTimeZone().getUtcOffset());
         BRelTime tzOff = BRelTime.make(timeZone.getUtcOffset());
         rec.setTimestamp(BAbsTime.make(ts.subtract(tzOff).add(tsOff), timeZone));
         return rec;
      }
   }

   protected BHistoryDatabase getHistoryDb() throws ArchiveException {
      try {
         BHistoryService service = (BHistoryService)Sys.getService(BHistoryService.TYPE);
         return service.getDatabase();
      } catch (ServiceNotFoundException var2) {
         this.executeFail(var2);
         throw new ArchiveException(var2);
      }
   }

   protected long determineNextIndex() throws BacnetException {
      long lastSeq = this.getLastSequenceNumberProcessed();
      long expectedSeq = BacnetTrendLogUtil.incrementSequenceNumber(lastSeq);
      byte[] ba = this.client().readProperty(this.device().getAddress(), this.getObjectId(), 145);
      long totalRecordCount = AsnUtil.fromAsnUnsignedInteger(ba);
      ba = this.client().readProperty(this.device().getAddress(), this.getObjectId(), 141);
      long recordCount = AsnUtil.fromAsnUnsignedInteger(ba);
      if (logger.isLoggable(Level.FINE)) {
         logger.fine(
            "determineNextIndex():lastSeq=" + lastSeq + " recordCount=" + recordCount + " totalRecordCount=" + totalRecordCount + " expectedSeq=" + expectedSeq
         );
      }

      if (recordCount != 0L && lastSeq != totalRecordCount) {
         if (recordCount <= totalRecordCount) {
            long firstIndex = totalRecordCount - recordCount + 1L;
            if (expectedSeq < firstIndex || expectedSeq > totalRecordCount) {
               expectedSeq = firstIndex;
            }
         } else {
            long firstIndex = BacnetTrendLogUtil.MAX_SEQ_NUM - (recordCount - totalRecordCount) + 1L;
            if (expectedSeq < firstIndex && expectedSeq > totalRecordCount) {
               expectedSeq = firstIndex;
            }
         }

         if (logger.isLoggable(Level.FINE)) {
            logger.fine(" --> expectedSeq=" + expectedSeq);
         }

         return expectedSeq;
      } else {
         return -1L;
      }
   }

   private BBacnetDeviceObjectPropertyReference getDeviceObjectPropEx() throws ArchiveException {
      try {
         return this.readDeviceObjectProp();
      } catch (Throwable var2) {
         throw new ArchiveException(var2);
      }
   }

   public final BBacnetDeviceObjectPropertyReference getDeviceObjectProp() {
      try {
         return this.readDeviceObjectProp();
      } catch (BacnetException var2) {
         logger.log(Level.WARNING, "Read Property request for log-device-object-property failed.");
         return null;
      }
   }

   private BBacnetDeviceObjectPropertyReference readDeviceObjectProp() throws BacnetException {
      byte[] ba = this.client().readProperty(this.device().getAddress(), this.getObjectId(), 132);
      this.oprChange = this.prev == null || !ByteArrayUtil.equals(ba, this.prev);
      this.prev = ba;
      this.asnIn.setBuffer(ba);
      BBacnetDeviceObjectPropertyReference dopr = new BBacnetDeviceObjectPropertyReference();
      dopr.readAsn(this.asnIn);
      return dopr;
   }

   protected BIHistory getOrCreateHistory(HistorySpaceConnection conn) {
      String histName = this.getLocalHistoryName();
      BHistoryId id = BHistoryId.make(this.device().getName(), histName);
      if (id == null) {
         throw new ArchiveException("Invalid names for historyId:" + this.device().getName() + "/" + histName);
      } else {
         BIHistory hist = conn.getHistory(id);
         if (hist != null) {
            return hist;
         } else {
            BTypeSpec ts = this.getTypeSpec();
            if (ts == null) {
               throw new ArchiveException("History type cannot be determined from device!");
            } else {
               return this.createHistory(conn, ts, id);
            }
         }
      }
   }

   protected BIHistory createHistory(HistorySpaceConnection conn, BTypeSpec ts, BHistoryId id) {
      BHistoryConfig config = new BHistoryConfig(id, ts);
      config = this.makeLocalConfig(config);
      if (logger.isLoggable(Level.FINE)) {
         logger.fine("Create history from config:" + config);
      }

      conn.createHistory(config);
      id = config.getId();
      this.setHistoryId(id);
      return conn.getHistory(id);
   }

   protected BTypeSpec getTypeSpec() {
      BTypeSpec ts = null;
      if (!this.getDiscoveryHistoryType().equalsIgnoreCase(UNKNOWN)) {
         ts = BTypeSpec.make("bacnet", "Bacnet" + this.getDiscoveryHistoryType() + "TrendRecord");
      }

      if (ts != null) {
         return ts;
      } else {
         try {
            ts = this.getTypeSpec(this.getDeviceObjectPropEx());
         } catch (Exception var4) {
            if (logger.isLoggable(Level.FINE)) {
               logger.fine("Cannot create history for " + this + " from Log_DeviceObjectProperty");
            }
         }

         if (ts != null) {
            return ts;
         } else {
            if (this.getDiscoveryHistoryType().equalsIgnoreCase(UNKNOWN)) {
               try {
                  ts = BacnetTrendLogUtil.findHistoryTypeByRecords(this.device(), this.getObjectId());
               } catch (Exception var3) {
                  if (logger.isLoggable(Level.FINE)) {
                     logger.fine("Cannot get history type spec for " + this + " from records");
                  }
               }
            }

            return ts;
         }
      }
   }

   protected BTypeSpec getTypeSpec(BBacnetDeviceObjectPropertyReference opr) {
      try {
         if (opr.getObjectId().getInstanceNumber() != 4194303) {
            PropertyInfo pi = ObjectTypeList.getInstance().getPropertyInfo(opr.getObjectId().getObjectType(), opr.getPropertyId());
            if (pi != null) {
               return this.asnTagToTypeSpec(pi.getAsnType());
            }
         }

         byte[] ba = this.client().readProperty(this.device().getAddress(), opr.getObjectId(), opr.getPropertyId());
         this.asnIn.setBuffer(ba);
         int tag = this.asnIn.peekTag();
         return this.asnIn.isApplicationTag(tag) ? this.asnTagToTypeSpec(tag) : BTypeSpec.make("bacnet", "BacnetStringTrendRecord");
      } catch (Throwable var4) {
         return null;
      }
   }

   private BTypeSpec asnTagToTypeSpec(int tag) {
      switch (tag) {
         case 1:
            return BTypeSpec.make("bacnet", "BacnetBooleanTrendRecord");
         case 2:
         case 3:
         case 4:
         case 5:
            return BTypeSpec.make("bacnet", "BacnetNumericTrendRecord");
         case 6:
         case 7:
         case 8:
         case 12:
            return BTypeSpec.make("bacnet", "BacnetStringTrendRecord");
         case 9:
            return BTypeSpec.make("bacnet", "BacnetEnumTrendRecord");
         case 10:
         case 11:
         default:
            return BTypeSpec.make("bacnet", "BacnetStringTrendRecord");
      }
   }

   public void doClearDeviceRecords() {
      try {
         if (!this.device().isServiceSupported("writeProperty")) {
            logger.info("Cannot clear device records:" + lex.getText("serviceNotSupported.writeProperty"));
         } else {
            this.client().writeProperty(this.device().getAddress(), this.getObjectId(), 141, AsnUtil.toAsnUnsigned(0L));
         }
      } catch (Exception var2) {
         logger.log(
            Level.SEVERE,
            "Problem during auto-clear of trend log records (device " + this.device().getAddress() + ", id " + this.getObjectId() + ")",
            (Throwable)var2
         );
      }
   }

   public String verifyLocalNameFormat() {
      String format = this.getLocalHistoryNameFormat().format(this);
      String name = format;
      if (!this.formatChecked) {
         boolean nameDef = localHistoryName.isEquivalentToDefaultValue(this.get(localHistoryName));
         if (!nameDef && !this.getLocalHistoryName().equals(format)) {
            this.setLocalHistoryNameFormat(BFormat.make(this.getLocalHistoryName()));
            name = this.getLocalHistoryName();
         } else {
            this.setLocalHistoryName(format);
         }

         this.formatChecked = true;
      }

      return name;
   }

   private static void setOrAdd(BComponent c, String propName, BValue v) {
      if (c.get(propName) == null) {
         c.add(propName, v);
      } else {
         c.set(propName, v);
      }
   }

   private void saveConfigOverride() {
      BHistoryId id = BHistoryId.make(this.device().getName(), this.getLocalHistoryName());
      BHistoryConfig config = new BHistoryConfig(id, this.getTypeSpec());
      config = this.makeLocalConfig(config);
      Optional<BIService> service = Sys.findService(BHistoryService.TYPE);
      if (service.isPresent()) {
         BHistoryService historyService = (BHistoryService)service.get();
         BHistoryDatabase db = historyService.getDatabase();
         HistoryDatabaseConnection conn = db.getDbConnection(null);
         Throwable var7 = null;

         try {
            conn.reconfigureHistory(config);
         } catch (Throwable var16) {
            var7 = var16;
            throw var16;
         } finally {
            if (conn != null) {
               if (var7 != null) {
                  try {
                     conn.close();
                  } catch (Throwable var15) {
                     var7.addSuppressed(var15);
                  }
               } else {
                  conn.close();
               }
            }
         }
      }
   }

   public void stopped() throws Exception {
      if (null != this.bacnetHistoryImportSubscriber) {
         this.bacnetHistoryImportSubscriber.unsubscribe(this.getConfigOverrides());
      }
   }

   class AsyncImport implements Runnable {
      @Override
      public void run() {
         try {
            BAbstractBacnetHistory.this.doExecute();
         } catch (Throwable var5) {
            BAbstractBacnetHistory.this.executeFail(var5);
         } finally {
            BAbstractBacnetHistory.this.setState(BDescriptorState.idle);
         }
      }
   }

   protected class BacnetHistoryImportSubscriber extends Subscriber {
      public void event(BComponentEvent event) {
         if (event.getId() == 0) {
            BAbstractBacnetHistory.this.saveConfigOverride();
         }
      }
   }
}
