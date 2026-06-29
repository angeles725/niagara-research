package com.tridium.opcUaClient.history;

import com.prosysopc.ua.client.UaClient;
import com.prosysopc.ua.stack.builtintypes.DataValue;
import com.prosysopc.ua.stack.builtintypes.DateTime;
import com.prosysopc.ua.stack.builtintypes.NodeId;
import com.prosysopc.ua.stack.builtintypes.UnsignedInteger;
import com.prosysopc.ua.stack.core.TimestampsToReturn;
import com.tridium.opcUaClient.BOpcUaDevice;
import com.tridium.opcUaClient.BOpcUaNetwork;
import com.tridium.opcUaClient.point.BOpcUaClientProxyExt;
import com.tridium.opcUaClient.util.OpcUaClientUtil;
import com.tridium.opcUaCore.enums.BServerState;
import com.tridium.util.ThrowableUtil;
import java.io.IOException;
import java.util.logging.Logger;
import javax.baja.control.BControlPoint;
import javax.baja.control.ext.BAbstractProxyExt;
import javax.baja.control.trigger.BDailyTriggerMode;
import javax.baja.control.trigger.BTimeTrigger;
import javax.baja.driver.BDevice;
import javax.baja.driver.BDeviceNetwork;
import javax.baja.driver.history.BIHistoryPollable;
import javax.baja.driver.point.BProxyExt;
import javax.baja.driver.util.BDescriptorState;
import javax.baja.driver.util.BPollFrequency;
import javax.baja.history.BBooleanTrendRecord;
import javax.baja.history.BEnumTrendRecord;
import javax.baja.history.BHistoryService;
import javax.baja.history.BIHistory;
import javax.baja.history.BIPollableHistorySource;
import javax.baja.history.BNumericTrendRecord;
import javax.baja.history.BStringTrendRecord;
import javax.baja.history.BTrendRecord;
import javax.baja.history.db.BHistoryDatabase;
import javax.baja.history.db.HistoryDatabaseConnection;
import javax.baja.history.ext.BHistoryExt;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.status.BStatus;
import javax.baja.status.BStatusBoolean;
import javax.baja.status.BStatusEnum;
import javax.baja.status.BStatusNumeric;
import javax.baja.status.BStatusString;
import javax.baja.status.BStatusValue;
import javax.baja.sys.Action;
import javax.baja.sys.BAbsTime;
import javax.baja.sys.BDynamicEnum;
import javax.baja.sys.BLink;
import javax.baja.sys.BValue;
import javax.baja.sys.Clock;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.util.IFuture;
import javax.baja.util.Invocation;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "state",
      type = "BDescriptorState",
      defaultValue = "BDescriptorState.idle",
      flags = 1
   ), @NiagaraProperty(
      name = "executionTime",
      type = "BTimeTrigger",
      defaultValue = "new BTimeTrigger(BDailyTriggerMode.make())"
   ), @NiagaraProperty(
      name = "lastAttempt",
      type = "BAbsTime",
      defaultValue = "BAbsTime.NULL",
      flags = 1
   ), @NiagaraProperty(
      name = "lastSuccess",
      type = "BAbsTime",
      defaultValue = "BAbsTime.NULL",
      flags = 1
   ), @NiagaraProperty(
      name = "lastFailure",
      type = "BAbsTime",
      defaultValue = "BAbsTime.NULL",
      flags = 1
   )})
@NiagaraAction(
   name = "execute",
   flags = 16
)
public class BImportHistoryExt extends BHistoryExt implements BIHistoryPollable, BIPollableHistorySource {
   public static final Property state = newProperty(1, BDescriptorState.idle, null);
   public static final Property executionTime = newProperty(0, new BTimeTrigger(BDailyTriggerMode.make()), null);
   public static final Property lastAttempt = newProperty(1, BAbsTime.NULL, null);
   public static final Property lastSuccess = newProperty(1, BAbsTime.NULL, null);
   public static final Property lastFailure = newProperty(1, BAbsTime.NULL, null);
   public static final Action execute = newAction(16, null);
   public static final Type TYPE = Sys.loadType(BImportHistoryExt.class);
   private BIHistory pointHistory;
   private HistoryDatabaseConnection dbConnection;
   private BOpcUaClientProxyExt proxyExt;
   private static Logger logger = Logger.getLogger("opcUaClient.history");

   public BDescriptorState getState() {
      return (BDescriptorState)this.get(state);
   }

   public void setState(BDescriptorState v) {
      this.set(state, v, null);
   }

   public BTimeTrigger getExecutionTime() {
      return (BTimeTrigger)this.get(executionTime);
   }

   public void setExecutionTime(BTimeTrigger v) {
      this.set(executionTime, v, null);
   }

   public BAbsTime getLastAttempt() {
      return (BAbsTime)this.get(lastAttempt);
   }

   public void setLastAttempt(BAbsTime v) {
      this.set(lastAttempt, v, null);
   }

   public BAbsTime getLastSuccess() {
      return (BAbsTime)this.get(lastSuccess);
   }

   public void setLastSuccess(BAbsTime v) {
      this.set(lastSuccess, v, null);
   }

   public BAbsTime getLastFailure() {
      return (BAbsTime)this.get(lastFailure);
   }

   public void setLastFailure(BAbsTime v) {
      this.set(lastFailure, v, null);
   }

   public void execute() {
      this.invoke(execute, null, null);
   }

   public Type getType() {
      return TYPE;
   }

   public void started() throws Exception {
      super.started();
      this.setFlags(activePeriod, this.getFlags(activePeriod) | 4);
      this.setFlags(active, this.getFlags(active) | 4);
      this.add(null, new BLink(this.getExecutionTime().getOrdInSession(), "fireTrigger", "execute", true), 6);
   }

   public void pointChanged(BAbsTime timestamp, BStatusValue out) throws Exception {
   }

   public BDeviceNetwork getNetwork() {
      BAbstractProxyExt abstractProxyExt = this.getParentPoint().getProxyExt();
      if (abstractProxyExt != null && abstractProxyExt instanceof BProxyExt) {
         BProxyExt proxyExt = (BProxyExt)abstractProxyExt;
         return proxyExt.getNetwork();
      } else {
         return null;
      }
   }

   public BDevice getDevice() {
      BAbstractProxyExt abstractProxyExt = this.getParentPoint().getProxyExt();
      if (abstractProxyExt != null && abstractProxyExt instanceof BProxyExt) {
         BProxyExt proxyExt = (BProxyExt)abstractProxyExt;
         return proxyExt.getDevice();
      } else {
         return null;
      }
   }

   public boolean requiresPointSubscription() {
      return false;
   }

   protected void activated(BAbsTime activeStartTime, BAbsTime currentTime, BStatusValue out) throws IOException {
   }

   protected void deactivated(BAbsTime currentTime, BStatusValue out) throws IOException {
   }

   public Type getRecordType() {
      BControlPoint point = this.getParentPoint();
      if (point != null) {
         BStatusValue statusValue = point.getOutStatusValue();
         if (statusValue instanceof BStatusNumeric) {
            return BNumericTrendRecord.TYPE;
         }

         if (statusValue instanceof BStatusBoolean) {
            return BBooleanTrendRecord.TYPE;
         }

         if (statusValue instanceof BStatusEnum) {
            return BEnumTrendRecord.TYPE;
         }

         if (statusValue instanceof BStatusString) {
            return BStringTrendRecord.TYPE;
         }
      }

      return null;
   }

   public IFuture post(Action action, BValue argument, Context cx) {
      BDeviceNetwork network = this.getNetwork();
      return ((BOpcUaNetwork)network).postAsync(new Invocation(this, action, argument, cx));
   }

   public void doExecute() {
      BStatus status = this.getStatus();
      BOpcUaDevice device = (BOpcUaDevice)this.getDevice();
      boolean devDisabled = device.isDisabled() || device.isFault() || device.isDown();
      boolean netwkDisabled = device.getOpcUaClientNetwork().isDisabled()
         || device.getOpcUaClientNetwork().isFault()
         || device.getOpcUaClientNetwork().isDown();
      if (!status.isDisabled() && !status.isDown() && !status.isFault() && !devDisabled && !netwkDisabled) {
         if (!device.getServerState().equals(BServerState.Running)) {
            this.executeFail("Server not running");
         } else {
            BHistoryService service = (BHistoryService)Sys.getService(BHistoryService.TYPE);
            BHistoryDatabase db = service.getDatabase();
            this.dbConnection = db.getDbConnection(null);
            this.pointHistory = this.dbConnection.getHistory(this.getHistoryConfig().getId());
            if (this.proxyExt == null) {
               BAbstractProxyExt absProxyExt = this.getParentPoint().getProxyExt();
               if (!(absProxyExt instanceof BOpcUaClientProxyExt)) {
                  return;
               }

               this.proxyExt = (BOpcUaClientProxyExt)absProxyExt;
            }

            this.executeInProgress();
            this.setLastAttempt(BAbsTime.now());

            try {
               UaClient client = device.uaClient;
               NodeId nodeId = NodeId.parseNodeId(this.proxyExt.getUaNodeId());

               try {
                  DateTime startTime = DateTime.MIN_VALUE;

                  try {
                     BAbsTime lastTimestamp = this.dbConnection.getLastTimestamp(this.pointHistory);
                     startTime = DateTime.fromMillis(
                        OpcUaClientUtil.getModifiedStartTime(lastTimestamp, ((BOpcUaDevice)this.getDevice()).getInitialHistoryArchiveFromDate()).getMillis()
                     );
                  } catch (Exception var15) {
                  }

                  DataValue[] result = OpcUaClientUtil.historyReadRaw(
                     client, nodeId, startTime, DateTime.currentTime(), UnsignedInteger.MAX_VALUE, true, null, TimestampsToReturn.Source
                  );
                  if (result != null) {
                     for (DataValue aResult : result) {
                        this.appendHistoryDataValue(aResult);
                     }

                     this.setLastRecord(this.dbConnection.getLastRecord(this.pointHistory));
                  }
               } catch (Exception var16) {
                  logger.info("Execute " + var16.getMessage());
               }
            } catch (Exception var17) {
               logger.severe("OpcUaClientHistoryImport readhistory exception - " + var17);
               this.executeFail(var17);
               return;
            }

            this.executeOk();
         }
      }
   }

   private void appendHistoryDataValue(DataValue dataValue) {
      if (this.pointHistory != null) {
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
   }

   public void executeOk() {
      this.setFaultCause("");
      this.setLastSuccess(Clock.time());
      this.setState(BDescriptorState.idle);
      this.updateStatus();
   }

   public void executeFail() {
      this.executeFail("");
   }

   public void executeFail(String reason) {
      if (reason == null) {
         reason = "";
      }

      this.setLastFailure(Clock.time());
      this.setFaultCause(reason);
      this.setState(BDescriptorState.idle);
      this.updateStatus();
   }

   public void executeFail(Throwable ex) {
      this.executeFail(ex == null ? "" : ThrowableUtil.dumpToString(ex, 1));
   }

   public void executeInProgress() {
      this.setState(BDescriptorState.inProgress);
   }

   public void poll() {
   }

   public BPollFrequency getPollFrequency() {
      return null;
   }

   public boolean historyPollingEnabled() {
      return false;
   }

   public int updateHistorySubscriptionCount(int change) {
      return 0;
   }
}
