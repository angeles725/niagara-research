package com.tridium.opcUaClient.history;

import com.prosysopc.ua.client.UaClient;
import com.prosysopc.ua.stack.builtintypes.DataValue;
import com.prosysopc.ua.stack.builtintypes.DateTime;
import com.prosysopc.ua.stack.builtintypes.NodeId;
import com.prosysopc.ua.stack.builtintypes.UnsignedInteger;
import com.prosysopc.ua.stack.core.TimestampsToReturn;
import com.tridium.ndriver.util.SfUtil;
import com.tridium.opcUaClient.BOpcUaDevice;
import com.tridium.opcUaClient.BOpcUaNetwork;
import com.tridium.opcUaClient.util.OpcUaClientUtil;
import com.tridium.opcUaCore.enums.BServerState;
import java.util.logging.Logger;
import javax.baja.driver.history.ArchiveException;
import javax.baja.driver.history.BHistoryImport;
import javax.baja.history.BBooleanTrendRecord;
import javax.baja.history.BEnumTrendRecord;
import javax.baja.history.BHistoryConfig;
import javax.baja.history.BHistoryId;
import javax.baja.history.BHistoryService;
import javax.baja.history.BIHistory;
import javax.baja.history.BNumericTrendRecord;
import javax.baja.history.BStringTrendRecord;
import javax.baja.history.BTrendRecord;
import javax.baja.history.db.BHistoryDatabase;
import javax.baja.history.db.HistoryDatabaseConnection;
import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.registry.TypeInfo;
import javax.baja.status.BStatusBoolean;
import javax.baja.status.BStatusEnum;
import javax.baja.status.BStatusNumeric;
import javax.baja.status.BStatusString;
import javax.baja.status.BStatusValue;
import javax.baja.sys.Action;
import javax.baja.sys.BAbsTime;
import javax.baja.sys.BBoolean;
import javax.baja.sys.BDynamicEnum;
import javax.baja.sys.BEnum;
import javax.baja.sys.BFacets;
import javax.baja.sys.BNumber;
import javax.baja.sys.BString;
import javax.baja.sys.BValue;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Slot;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.timezone.BTimeZone;
import javax.baja.util.BTypeSpec;
import javax.baja.util.IFuture;
import javax.baja.util.Invocation;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "localHistoryName",
      type = "String",
      defaultValue = "",
      flags = 64,
      facets = {@Facet("SfUtil.incl()")}
   ), @NiagaraProperty(
      name = "uaNodeName",
      type = "String",
      defaultValue = "",
      flags = 1,
      facets = {@Facet("SfUtil.incl()")}
   ), @NiagaraProperty(
      name = "uaNodeId",
      type = "String",
      defaultValue = "",
      facets = {@Facet("SfUtil.incl()")}
   ), @NiagaraProperty(
      name = "typeSpec",
      type = "String",
      defaultValue = "",
      facets = {@Facet("SfUtil.incl()")}
   ), @NiagaraProperty(
      name = "facets",
      type = "BFacets",
      defaultValue = "BFacets.NULL"
   )})
public class BOpcUaClientHistoryImport extends BHistoryImport {
   public static final Property localHistoryName = newProperty(64, "", SfUtil.incl());
   public static final Property uaNodeName = newProperty(1, "", SfUtil.incl());
   public static final Property uaNodeId = newProperty(0, "", SfUtil.incl());
   public static final Property typeSpec = newProperty(0, "", SfUtil.incl());
   public static final Property facets = newProperty(0, BFacets.NULL, null);
   public static final Type TYPE = Sys.loadType(BOpcUaClientHistoryImport.class);
   private BIHistory pointHistory;
   private HistoryDatabaseConnection dbConnection;
   public static final Logger logger = Logger.getLogger("opcUaClient.history");
   private static final String VALUE_FACETS = "valueFacets";

   public String getLocalHistoryName() {
      return this.getString(localHistoryName);
   }

   public void setLocalHistoryName(String v) {
      this.setString(localHistoryName, v, null);
   }

   public String getUaNodeName() {
      return this.getString(uaNodeName);
   }

   public void setUaNodeName(String v) {
      this.setString(uaNodeName, v, null);
   }

   public String getUaNodeId() {
      return this.getString(uaNodeId);
   }

   public void setUaNodeId(String v) {
      this.setString(uaNodeId, v, null);
   }

   public String getTypeSpec() {
      return this.getString(typeSpec);
   }

   public void setTypeSpec(String v) {
      this.setString(typeSpec, v, null);
   }

   public BFacets getFacets() {
      return (BFacets)this.get(facets);
   }

   public void setFacets(BFacets v) {
      this.set(facets, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public final BOpcUaDevice device() {
      return (BOpcUaDevice)this.getDeviceExt().getDevice();
   }

   public void started() throws Exception {
      super.started();
      Type rcdType = this.getHistoryRecordType();
      BHistoryId histId = BHistoryId.make(this.getDevice().getName(), this.getDefaultName());
      this.setHistoryId(histId);
      Slot slot = this.getSlot("historyConfig");
      if (slot == null) {
         BHistoryConfig histConfig = new BHistoryConfig(histId, BTypeSpec.make(rcdType));
         histConfig = this.makeLocalConfig(histConfig);

         try {
            this.add("historyConfig", histConfig);
         } catch (Exception var6) {
            logger.severe("Exception while adding history records: " + var6);
         }
      }

      BHistoryService service = (BHistoryService)Sys.getService(BHistoryService.TYPE);
      BHistoryDatabase db = service.getDatabase();
      if (db != null) {
         this.dbConnection = db.getDbConnection(null);
         if (this.dbConnection != null) {
            this.initHistory(histId);
         }
      }
   }

   private void initHistory(BHistoryId histId) {
      try {
         BHistoryConfig histConfig = (BHistoryConfig)this.get("historyConfig");
         histConfig.setTimeZone(BTimeZone.getLocal());
         BFacets f = (BFacets)histConfig.get("valueFacets");
         if (f != null) {
            histConfig.set("valueFacets", this.getFacets());
         } else {
            histConfig.add("valueFacets", this.getFacets());
         }

         if (!this.dbConnection.exists(histId)) {
            this.dbConnection.createHistory(histConfig);
         } else {
            this.dbConnection.reconfigureHistory(histConfig);
         }

         this.pointHistory = this.dbConnection.getHistory(histId);
      } catch (Exception var4) {
         logger.severe("Exception while creating history records: " + var4);
      }
   }

   protected IFuture postExecute(Action action, BValue arg, Context cx) {
      return !this.isRunning() ? null : ((BOpcUaNetwork)this.getNetwork()).postAsync(new Invocation(this, execute, arg, cx));
   }

   public void doExecute() throws ArchiveException {
      BOpcUaDevice device = this.device();
      DateTime startTime = DateTime.MIN_VALUE;
      if (!device.getServerState().equals(BServerState.Running)) {
         this.executeFail("Server not running");
      } else {
         this.executeInProgress();

         try {
            BAbsTime lastTimestamp = this.dbConnection.getLastTimestamp(this.pointHistory);
            startTime = DateTime.fromMillis(
               OpcUaClientUtil.getModifiedStartTime(lastTimestamp, ((BOpcUaDevice)this.getDevice()).getInitialHistoryArchiveFromDate()).getMillis()
            );
         } catch (Exception var10) {
            logger.fine("Exception while getting the lastTimestamp for PointHistory  - " + var10);
         }

         DataValue[] result;
         try {
            UaClient client = device.uaClient;
            NodeId nodeId = NodeId.parseNodeId(this.getUaNodeId());
            BHistoryId historyId = this.pointHistory.getId();
            if (this.dbConnection.getHistory(historyId) == null) {
               this.initHistory(historyId);
            }

            result = OpcUaClientUtil.historyReadRaw(
               client, nodeId, startTime, DateTime.currentTime(), UnsignedInteger.MAX_VALUE, true, null, TimestampsToReturn.Source
            );
         } catch (Exception var11) {
            logger.info("OpcUaClientHistoryImport readhistory exception - " + var11);
            this.executeFail(var11);
            return;
         }

         if (result != null) {
            for (DataValue aResult : result) {
               try {
                  this.appendHistoryDataValue(aResult);
               } catch (Exception var9) {
                  logger.info("Exception while adding history data:" + var9.getMessage());
               }
            }
         }

         this.executeOk();
      }
   }

   private void appendHistoryDataValue(DataValue dataValue) {
      DateTime sourceTimestamp = dataValue.getSourceTimestamp();
      BAbsTime sampleTime = OpcUaClientUtil.dateTimeToAbsTime(sourceTimestamp);
      BTrendRecord histRecord = (BTrendRecord)this.pointHistory.getRecordType().getInstance();
      BStatusValue histValue = OpcUaClientUtil.makeStatusValue(dataValue, histRecord);
      BAbsTime lastTimestamp = this.dbConnection.getLastTimestamp(this.pointHistory);
      if (this.dbConnection.getRecordCount(this.pointHistory) == 0 || sampleTime.isAfter(lastTimestamp)) {
         if (histRecord instanceof BNumericTrendRecord) {
            double value = ((BStatusNumeric)histValue).getValue();
            ((BNumericTrendRecord)histRecord).set(sampleTime, value, histValue.getStatus());
         } else if (histRecord instanceof BBooleanTrendRecord) {
            boolean value = ((BStatusBoolean)histValue).getValue();
            ((BBooleanTrendRecord)histRecord).set(sampleTime, value, histValue.getStatus());
         } else if (histRecord instanceof BEnumTrendRecord) {
            BDynamicEnum value = ((BStatusEnum)histValue).getValue();
            ((BEnumTrendRecord)histRecord).set(sampleTime, value, histValue.getStatus());
         } else {
            if (!(histRecord instanceof BStringTrendRecord)) {
               return;
            }

            String value = ((BStatusString)histValue).getValue();
            ((BStringTrendRecord)histRecord).set(sampleTime, value, histValue.getStatus());
         }

         this.dbConnection.append(this.pointHistory, histRecord);
      }
   }

   private Type getHistoryRecordType() {
      String spec = this.getTypeSpec();
      if (spec.isEmpty()) {
         return null;
      } else {
         TypeInfo typeInfo = BTypeSpec.make(spec).getTypeInfo();
         if (typeInfo.is(BNumber.TYPE)) {
            return BNumericTrendRecord.TYPE;
         } else if (typeInfo.is(BBoolean.TYPE)) {
            return BBooleanTrendRecord.TYPE;
         } else if (typeInfo.is(BEnum.TYPE)) {
            return BEnumTrendRecord.TYPE;
         } else {
            return typeInfo.is(BString.TYPE) ? BStringTrendRecord.TYPE : null;
         }
      }
   }

   private String getDefaultName() {
      return this.getUaNodeName().replace('/', '_');
   }
}
