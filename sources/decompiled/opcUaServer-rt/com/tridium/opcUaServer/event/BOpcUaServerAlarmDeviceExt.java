package com.tridium.opcUaServer.event;

import com.prosysopc.ua.StatusException;
import com.prosysopc.ua.server.EventManager;
import com.prosysopc.ua.server.EventManagerListener;
import com.prosysopc.ua.server.MonitoredEventItem;
import com.prosysopc.ua.server.ServiceContext;
import com.prosysopc.ua.server.Subscription;
import com.prosysopc.ua.stack.builtintypes.ByteString;
import com.prosysopc.ua.stack.builtintypes.DateTime;
import com.prosysopc.ua.stack.builtintypes.LocalizedText;
import com.prosysopc.ua.stack.builtintypes.NodeId;
import com.prosysopc.ua.stack.core.EventFilter;
import com.prosysopc.ua.stack.core.EventFilterResult;
import com.prosysopc.ua.stack.core.StatusCodes;
import com.prosysopc.ua.types.opcua.server.AcknowledgeableConditionTypeNode;
import com.prosysopc.ua.types.opcua.server.AlarmConditionTypeNode;
import com.prosysopc.ua.types.opcua.server.ConditionTypeNode;
import com.prosysopc.ua.types.opcua.server.ShelvedStateMachineTypeNode;
import com.tridium.opcUaServer.BOpcUaNamespace;
import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Logger;
import javax.baja.alarm.AlarmDbConnection;
import javax.baja.alarm.BAlarmDatabase;
import javax.baja.alarm.BAlarmRecord;
import javax.baja.alarm.BAlarmService;
import javax.baja.alarm.ext.BAlarmSourceExt;
import javax.baja.control.BControlPoint;
import javax.baja.driver.alarm.BAlarmDeviceExt;
import javax.baja.naming.BOrdList;
import javax.baja.naming.SlotPath;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BBoolean;
import javax.baja.sys.BComponent;
import javax.baja.sys.BObject;
import javax.baja.sys.BValue;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.util.BUuid;

@NiagaraType
public class BOpcUaServerAlarmDeviceExt extends BAlarmDeviceExt implements EventManagerListener {
   public static final Type TYPE = Sys.loadType(BOpcUaServerAlarmDeviceExt.class);
   private int eventId = 0;
   private AlarmDbConnection dbConnection = null;
   private Logger logger = Logger.getLogger("opcUaServer.alarmDeviceExt");

   public Type getType() {
      return TYPE;
   }

   public void started() throws Exception {
      super.started();
   }

   public void init() {
      BAlarmService service = (BAlarmService)Sys.getService(BAlarmService.TYPE);
      BAlarmDatabase alarmDb = service.getAlarmDb();
      this.dbConnection = alarmDb.getDbConnection(null);
      BOpcUaAlarmClass[] opcAlarmClass = (BOpcUaAlarmClass[])service.getChildren(BOpcUaAlarmClass.class);
      this.setAlarmClass(opcAlarmClass[0].getName());
   }

   public boolean isParentLegal(BComponent parent) {
      return parent instanceof BOpcUaNamespace;
   }

   public boolean isChildLegal(BComponent child) {
      return child instanceof BOpcUaAlarmSource;
   }

   public void doRouteAlarm(BAlarmRecord record) throws Exception {
   }

   public BBoolean doAckAlarm(BAlarmRecord record) throws Exception {
      return null;
   }

   public void addAlarmPoint(AlarmConditionTypeNode variable, BControlPoint point) {
      String nodeId = variable.getNodeId().toString();
      String name = SlotPath.escape(nodeId);
      Property property = this.getProperty(name);
      if (property == null) {
         BOpcUaAlarmSource almSrc = BOpcUaAlarmSource.make(variable, point);
         this.add(name, almSrc, 3);
      } else {
         BValue bValue = this.get(property);
         if (bValue instanceof BOpcUaAlarmSource) {
            ((BOpcUaAlarmSource)bValue).updateFrom(variable, point);
         }
      }
   }

   public void removeAlarmPoint(AlarmConditionTypeNode variable) {
      String nodeId = variable.getNodeId().toString();
      String name = SlotPath.escape(nodeId);
      Property property = this.getProperty(name);
      if (property != null) {
         this.remove(property);
      }
   }

   public boolean onAcknowledge(ServiceContext serviceContext, AcknowledgeableConditionTypeNode condition, ByteString eventId, LocalizedText comment) throws StatusException {
      this.logger.fine("Acknowledge: Condition=" + condition + "; EventId=" + this.eventIdToString(eventId) + "; Comment=" + comment);

      try {
         ByteString bytes = EventManager.extractUserEventId(eventId);
         BUuid uuid = BUuid.make(bytes.getValue());
         BAlarmRecord alarmRecord = this.dbConnection.getRecord(uuid);
         if (alarmRecord == null) {
            throw new StatusException(StatusCodes.Bad_EventIdUnknown);
         }

         if (alarmRecord.isAcknowledged()) {
            return true;
         }

         DateTime now = DateTime.currentTime();
         if (Arrays.equals(condition.getEventId().getValue(), bytes.getValue())) {
            condition.setAcked(true, now);
            ByteString userEventId = this.getNextUserEventId();
            condition.addComment(eventId, comment, now, userEventId);
         }

         BOrdList source = alarmRecord.getSource();
         BObject bObject = source.get(0).get();
         if (bObject instanceof BAlarmSourceExt) {
            ((BAlarmSourceExt)bObject).ackAlarm(alarmRecord);
         }
      } catch (IOException var11) {
      }

      return true;
   }

   public boolean onAddComment(ServiceContext serviceContext, ConditionTypeNode condition, ByteString eventId, LocalizedText comment) throws StatusException {
      this.logger.fine("AddComment: Condition=" + condition + "; Event=" + this.eventIdToString(eventId) + "; Comment=" + comment);
      if (!Arrays.equals(eventId.getValue(), condition.getEventId().getValue())) {
         throw new StatusException(StatusCodes.Bad_EventIdUnknown);
      } else {
         ByteString userEventId = this.getNextUserEventId();
         condition.addComment(eventId, comment, DateTime.currentTime(), userEventId);
         return true;
      }
   }

   public void onAfterCreateMonitoredEventItem(ServiceContext serviceContext, Subscription subscription, MonitoredEventItem item) {
      this.logger.fine("onAfterCreateMonitoredEventItem: serviceContext=" + serviceContext + "; subscription=" + subscription + "; item=" + item);
   }

   public void onAfterDeleteMonitoredEventItem(ServiceContext serviceContext, Subscription subscription, MonitoredEventItem item) {
      this.logger.fine("onAfterDeleteMonitoredEventItem: serviceContext=" + serviceContext + "; subscription=" + subscription + "; item=" + item);
   }

   public void onAfterModifyMonitoredEventItem(ServiceContext serviceContext, Subscription subscription, MonitoredEventItem item) {
      this.logger.fine("onAfterModifyMonitoredEventItem: serviceContext=" + serviceContext + "; subscription=" + subscription + "; item=" + item);
   }

   public void onConditionRefresh(ServiceContext serviceContext, Subscription subscription) throws StatusException {
      this.logger.fine("onConditionRefresh: serviceContext=" + serviceContext + "; subscription=" + subscription);
   }

   public void onConditionRefresh2(ServiceContext serviceContext, MonitoredEventItem monitoredEventItem) throws StatusException {
   }

   public boolean onConfirm(ServiceContext serviceContext, AcknowledgeableConditionTypeNode condition, ByteString eventId, LocalizedText comment) throws StatusException {
      this.logger.fine("Confirm: Condition=" + condition + "; EventId=" + this.eventIdToString(eventId) + "; Comment=" + comment);
      if (!Arrays.equals(eventId.getValue(), condition.getEventId().getValue())) {
         throw new StatusException(StatusCodes.Bad_EventIdUnknown);
      } else if (condition.isConfirmed()) {
         throw new StatusException(StatusCodes.Bad_ConditionBranchAlreadyConfirmed);
      } else if (!condition.isAcked()) {
         throw new StatusException("Condition can only be confirmed when it is acknowledged.", StatusCodes.Bad_InvalidState);
      } else {
         if (!(condition instanceof AlarmConditionTypeNode) || !((AlarmConditionTypeNode)condition).isActive()) {
            condition.setRetain(false);
         }

         DateTime now = DateTime.currentTime();
         condition.setConfirmed(true, now);
         ByteString userEventId = this.getNextUserEventId();
         condition.addComment(eventId, comment, now, userEventId);
         return true;
      }
   }

   public void onCreateMonitoredEventItem(ServiceContext serviceContext, NodeId nodeId, EventFilter eventFilter, EventFilterResult filterResult) throws StatusException {
      this.logger.fine("onCreateMonitoredEventItem: " + nodeId);
   }

   public void onDeleteMonitoredEventItem(ServiceContext serviceContext, Subscription subscription, MonitoredEventItem monitoredItem) {
      this.logger.fine("onDeleteMonitoredEventItem: " + monitoredItem.getNodeId());
   }

   public boolean onDisable(ServiceContext serviceContext, ConditionTypeNode condition) throws StatusException {
      this.logger.fine("Disable: Condition=" + condition);
      if (condition.isEnabled()) {
         DateTime now = DateTime.currentTime();
         condition.setEnabled(false, now);
         condition.triggerEvent(now, null, this.getNextUserEventId());
      }

      String name = SlotPath.escape(condition.getNodeId().toString());
      BValue bValue = this.get(name);
      if (bValue != null && bValue instanceof BOpcUaAlarmSource) {
         ((BOpcUaAlarmSource)bValue).updateFrom(condition);
      }

      return true;
   }

   public boolean onEnable(ServiceContext serviceContext, ConditionTypeNode condition) throws StatusException {
      if (!condition.isEnabled()) {
         DateTime now = DateTime.currentTime();
         condition.setEnabled(true, now);
      }

      String name = SlotPath.escape(condition.getNodeId().toString());
      BValue bValue = this.get(name);
      if (bValue != null && bValue instanceof BOpcUaAlarmSource) {
         ((BOpcUaAlarmSource)bValue).updateFrom(condition);
      }

      return true;
   }

   public void onModifyMonitoredEventItem(
      ServiceContext serviceContext, Subscription subscription, MonitoredEventItem monitoredItem, EventFilter eventFilter, EventFilterResult filterResult
   ) throws StatusException {
   }

   public boolean onOneshotShelve(ServiceContext serviceContext, AlarmConditionTypeNode condition, ShelvedStateMachineTypeNode stateMachine) throws StatusException {
      return false;
   }

   public boolean onTimedShelve(ServiceContext serviceContext, AlarmConditionTypeNode condition, ShelvedStateMachineTypeNode stateMachine, double shelvingTime) throws StatusException {
      return false;
   }

   public boolean onUnshelve(ServiceContext serviceContext, AlarmConditionTypeNode condition, ShelvedStateMachineTypeNode stateMachine) throws StatusException {
      return false;
   }

   private String eventIdToString(ByteString eventId) {
      return eventId == null ? "(null)" : Arrays.toString(eventId.getValue());
   }

   private ByteString getNextUserEventId() {
      return ByteString.fromLong(this.eventId++);
   }
}
