package com.tridium.opcUaServer.history;

import com.prosysopc.ua.ServiceException;
import com.prosysopc.ua.StatusException;
import com.prosysopc.ua.nodes.UaNode;
import com.prosysopc.ua.server.HistoryContinuationPoint;
import com.prosysopc.ua.server.HistoryManagerListener;
import com.prosysopc.ua.server.HistoryResult;
import com.prosysopc.ua.server.ServiceContext;
import com.prosysopc.ua.server.nodes.UaObjectNode;
import com.prosysopc.ua.server.nodes.UaVariableNode;
import com.prosysopc.ua.stack.builtintypes.ByteString;
import com.prosysopc.ua.stack.builtintypes.DataValue;
import com.prosysopc.ua.stack.builtintypes.DateTime;
import com.prosysopc.ua.stack.builtintypes.DiagnosticInfo;
import com.prosysopc.ua.stack.builtintypes.NodeId;
import com.prosysopc.ua.stack.builtintypes.StatusCode;
import com.prosysopc.ua.stack.builtintypes.UnsignedInteger;
import com.prosysopc.ua.stack.builtintypes.Variant;
import com.prosysopc.ua.stack.core.AccessLevelType;
import com.prosysopc.ua.stack.core.AggregateConfiguration;
import com.prosysopc.ua.stack.core.EventFilter;
import com.prosysopc.ua.stack.core.EventNotifierType;
import com.prosysopc.ua.stack.core.HistoryData;
import com.prosysopc.ua.stack.core.HistoryEvent;
import com.prosysopc.ua.stack.core.HistoryEventFieldList;
import com.prosysopc.ua.stack.core.HistoryModifiedData;
import com.prosysopc.ua.stack.core.HistoryReadDetails;
import com.prosysopc.ua.stack.core.HistoryReadValueId;
import com.prosysopc.ua.stack.core.HistoryUpdateDetails;
import com.prosysopc.ua.stack.core.HistoryUpdateResult;
import com.prosysopc.ua.stack.core.PerformUpdateType;
import com.prosysopc.ua.stack.core.StatusCodes;
import com.prosysopc.ua.stack.core.TimestampsToReturn;
import com.prosysopc.ua.stack.core.EventNotifierType.Options;
import com.prosysopc.ua.stack.utils.NumericRange;
import com.tridium.opcUaServer.event.EventHistory;
import com.tridium.opcUaServer.util.OpcUaServerUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.history.BHistoryId;
import javax.baja.history.BHistoryRecord;
import javax.baja.history.BHistoryService;
import javax.baja.history.BIHistory;
import javax.baja.history.BTrendRecord;
import javax.baja.history.db.BHistoryDatabase;
import javax.baja.history.db.HistoryDatabaseConnection;
import javax.baja.history.ext.BHistoryExt;
import javax.baja.sys.BAbsTime;
import javax.baja.sys.Cursor;
import javax.baja.sys.Sys;

public class OpcUaHistorian implements HistoryManagerListener {
   private final Map<UaObjectNode, EventHistory> eventHistories = new HashMap<>();
   private final Map<UaVariableNode, ValueHistory> variableHistories = new HashMap<>();
   private final Map<String, BHistoryExt> pointHistories = new HashMap<>();
   BHistoryService service;
   BHistoryDatabase historyDb;
   private HistoryDatabaseConnection dbConnection;
   public static final Logger logger = Logger.getLogger("opcUaServer.historian");

   public void init() {
      this.service = (BHistoryService)Sys.getService(BHistoryService.TYPE);
      this.historyDb = this.service.getDatabase();
      this.dbConnection = this.historyDb.getDbConnection(null);
   }

   public void addEventHistory(UaObjectNode node) {
      EventHistory history = new EventHistory(node);
      Set<Options> eventNotifier = node.getEventNotifier().toSet();
      eventNotifier.add(Options.HistoryRead);
      node.setEventNotifier(EventNotifierType.of(eventNotifier));
      this.eventHistories.put(node, history);
   }

   public void addVariableHistory(UaVariableNode variable, BHistoryExt historyExt) {
      ValueHistory history = new ValueHistory(variable);
      variable.setHistorizing(true);
      AccessLevelType READ_WRITE_HISTORYREAD = AccessLevelType.of(
         new com.prosysopc.ua.stack.core.AccessLevelType.Options[]{
            com.prosysopc.ua.stack.core.AccessLevelType.Options.CurrentRead,
            com.prosysopc.ua.stack.core.AccessLevelType.Options.CurrentWrite,
            com.prosysopc.ua.stack.core.AccessLevelType.Options.HistoryRead
         }
      );
      variable.setAccessLevel(READ_WRITE_HISTORYREAD);
      this.variableHistories.put(variable, history);
      this.pointHistories.put(variable.getNodeId().toString(), historyExt);
   }

   public void removeVariableHistory(UaVariableNode variable) {
      variable.setHistorizing(false);
      this.variableHistories.remove(variable);
      this.pointHistories.remove(variable.getNodeId().toString());
   }

   public Object onBeginHistoryRead(
      ServiceContext serviceContext,
      HistoryReadDetails details,
      TimestampsToReturn timestampsToReturn,
      HistoryReadValueId[] nodesToRead,
      HistoryContinuationPoint[] continuationPoints,
      HistoryResult[] results
   ) throws ServiceException {
      for (HistoryReadValueId historyReadValueId : nodesToRead) {
         BHistoryExt historyExt = this.pointHistories.get(historyReadValueId.getNodeId().toString());
         if (historyExt != null) {
            BHistoryId historyId = historyExt.getHistoryConfig().getId();
            if (historyId != null) {
               BIHistory history = this.dbConnection.getHistory(historyId);
               Cursor<BHistoryRecord> histCursor = this.dbConnection.scan(history);
               Throwable var15 = null;

               try {
                  while (histCursor.next()) {
                     BHistoryRecord record = (BHistoryRecord)histCursor.get();
                     if (logger.isLoggable(Level.FINEST)) {
                        logger.finest("record: " + record);
                     }
                  }
               } catch (Throwable var24) {
                  var15 = var24;
                  throw var24;
               } finally {
                  if (histCursor != null) {
                     if (var15 != null) {
                        try {
                           histCursor.close();
                        } catch (Throwable var23) {
                           var15.addSuppressed(var23);
                        }
                     } else {
                        histCursor.close();
                     }
                  }
               }
            }
         }
      }

      return null;
   }

   public Object onBeginHistoryUpdate(
      ServiceContext serviceContext, HistoryUpdateDetails[] details, HistoryUpdateResult[] results, DiagnosticInfo[] diagnosticInfos
   ) throws ServiceException {
      return null;
   }

   public void onDeleteAtTimes(
      ServiceContext serviceContext,
      Object operationContext,
      NodeId nodeId,
      UaNode node,
      DateTime[] reqTimes,
      StatusCode[] operationResults,
      DiagnosticInfo[] operationDiagnostics
   ) throws StatusException {
      ValueHistory history = this.variableHistories.get(node);
      if (history != null) {
         history.deleteAtTimes(reqTimes, operationResults, operationDiagnostics);
      } else {
         throw new StatusException(StatusCodes.Bad_NoData);
      }
   }

   public void onDeleteEvents(
      ServiceContext serviceContext,
      Object operationContext,
      NodeId nodeId,
      UaNode node,
      ByteString[] eventIds,
      StatusCode[] operationResults,
      DiagnosticInfo[] operationDiagnostics
   ) throws StatusException {
      EventHistory history = this.eventHistories.get(node);
      if (history != null) {
         history.deleteEvents(eventIds, operationResults, operationDiagnostics);
      } else {
         throw new StatusException(StatusCodes.Bad_NoData);
      }
   }

   public void onDeleteModified(ServiceContext serviceContext, Object operationContext, NodeId nodeId, UaNode node, DateTime startTime, DateTime endTime) throws StatusException {
      throw new StatusException(StatusCodes.Bad_HistoryOperationUnsupported);
   }

   public void onDeleteRaw(ServiceContext serviceContext, Object operationContext, NodeId nodeId, UaNode node, DateTime startTime, DateTime endTime) throws StatusException {
      throw new StatusException(StatusCodes.Bad_HistoryOperationUnsupported);
   }

   public void onEndHistoryRead(
      ServiceContext serviceContext,
      Object operationContext,
      HistoryReadDetails details,
      TimestampsToReturn timestampsToReturn,
      HistoryReadValueId[] nodesToRead,
      HistoryContinuationPoint[] continuationPoints,
      HistoryResult[] results
   ) throws ServiceException {
   }

   public void onEndHistoryUpdate(
      ServiceContext serviceContext, Object operationContext, HistoryUpdateDetails[] details, HistoryUpdateResult[] results, DiagnosticInfo[] diagnosticInfos
   ) throws ServiceException {
   }

   public Object onReadAtTimes(
      ServiceContext serviceContext,
      Object operationContext,
      TimestampsToReturn timestampsToReturn,
      NodeId nodeId,
      UaNode node,
      Object continuationPoint,
      DateTime[] reqTimes,
      Boolean useSimpleBounds,
      NumericRange indexRange,
      HistoryData historyData
   ) throws StatusException {
      if (logger.isLoggable(Level.FINE)) {
         logger.fine("onReadAtTimes: reqTimes=[" + reqTimes.length + "] " + (reqTimes.length < 20 ? Arrays.toString((Object[])reqTimes) : ""));
      }

      ValueHistory history = this.variableHistories.get(node);
      if (history != null) {
         historyData.setDataValues(history.readAtTimes(reqTimes));
         return null;
      } else {
         throw new StatusException(StatusCodes.Bad_NoData);
      }
   }

   public Object onReadEvents(
      ServiceContext serviceContext,
      Object operationContext,
      NodeId nodeId,
      UaNode node,
      Object continuationPoint,
      DateTime startTime,
      DateTime endTime,
      UnsignedInteger numValuesPerNode,
      EventFilter filter,
      HistoryEvent historyEvent
   ) throws StatusException {
      EventHistory history = this.eventHistories.get(node);
      if (history != null) {
         List<HistoryEventFieldList> events = new ArrayList<>();
         int firstIndex = continuationPoint == null ? 0 : (Integer)continuationPoint;
         Integer newContinuationPoint = history.readEvents(startTime, endTime, numValuesPerNode.intValue(), filter, events, firstIndex);
         historyEvent.setEvents(events.toArray(new HistoryEventFieldList[0]));
         return newContinuationPoint;
      } else {
         throw new StatusException(StatusCodes.Bad_NoData);
      }
   }

   public Object onReadModified(
      ServiceContext serviceContext,
      Object operationContext,
      TimestampsToReturn timestampsToReturn,
      NodeId nodeId,
      UaNode node,
      Object continuationPoint,
      DateTime startTime,
      DateTime endTime,
      UnsignedInteger numValuesPerNode,
      NumericRange indexRange,
      HistoryModifiedData historyData
   ) throws StatusException {
      throw new StatusException(StatusCodes.Bad_HistoryOperationUnsupported);
   }

   public Object onReadProcessed(
      ServiceContext serviceContext,
      Object operationContext,
      TimestampsToReturn timestampsToReturn,
      NodeId nodeId,
      UaNode node,
      Object continuationPoint,
      DateTime startTime,
      DateTime endTime,
      Double resampleInterval,
      NodeId aggregateType,
      AggregateConfiguration aggregateConfiguration,
      NumericRange indexRange,
      HistoryData historyData
   ) throws StatusException {
      throw new StatusException(StatusCodes.Bad_HistoryOperationUnsupported);
   }

   public Object onReadRaw(
      ServiceContext serviceContext,
      Object operationContext,
      TimestampsToReturn timestampsToReturn,
      NodeId nodeId,
      UaNode node,
      Object continuationPoint,
      DateTime startTime,
      DateTime endTime,
      UnsignedInteger numValuesPerNode,
      Boolean returnBounds,
      NumericRange indexRange,
      HistoryData historyData
   ) throws StatusException {
      if (logger.isLoggable(Level.FINEST)) {
         logger.finest("onReadRaw: startTime={} endTime={} numValuesPerNode={}" + startTime + ", " + endTime + ", " + numValuesPerNode);
      }

      BHistoryExt historyExt = this.pointHistories.get(nodeId.toString());
      if (historyExt != null) {
         BHistoryId historyId = historyExt.getHistoryConfig().getId();
         List<DataValue> values = new ArrayList<>();
         BAbsTime sTime = OpcUaServerUtil.dateTimeToAbsTime(startTime);
         BAbsTime eTime = OpcUaServerUtil.dateTimeToAbsTime(endTime);
         BIHistory history = this.dbConnection.getHistory(historyId);
         if (history != null) {
            Cursor<BHistoryRecord> histCursor = this.dbConnection.scan(history);
            Throwable var20 = null;

            try {
               while (histCursor.next()) {
                  BHistoryRecord record = (BHistoryRecord)histCursor.get();
                  if (record instanceof BTrendRecord) {
                     BAbsTime timestamp = record.getTimestamp();
                     if (timestamp.isAfter(sTime) && timestamp.isBefore(eTime)) {
                        DataValue histDataValue = OpcUaServerUtil.trendRecordToDataValue((BTrendRecord)record);
                        values.add(histDataValue);
                     }
                  }
               }
            } catch (Throwable var31) {
               var20 = var31;
               throw var31;
            } finally {
               if (histCursor != null) {
                  if (var20 != null) {
                     try {
                        histCursor.close();
                     } catch (Throwable var30) {
                        var20.addSuppressed(var30);
                     }
                  } else {
                     histCursor.close();
                  }
               }
            }

            historyData.setDataValues(values.toArray(new DataValue[0]));
            return null;
         }
      }

      return null;
   }

   public void onUpdateData(
      ServiceContext serviceContext,
      Object operationContext,
      NodeId nodeId,
      UaNode node,
      DataValue[] updateValues,
      PerformUpdateType performInsertReplace,
      StatusCode[] operationResults,
      DiagnosticInfo[] operationDiagnostics
   ) throws StatusException {
      throw new StatusException(StatusCodes.Bad_HistoryOperationUnsupported);
   }

   public void onUpdateEvent(
      ServiceContext serviceContext,
      Object operationContext,
      NodeId nodeId,
      UaNode node,
      Variant[] eventFields,
      EventFilter filter,
      PerformUpdateType performInsertReplace,
      StatusCode[] operationResults,
      DiagnosticInfo[] operationDiagnostics
   ) throws StatusException {
      throw new StatusException(StatusCodes.Bad_HistoryOperationUnsupported);
   }

   public void onUpdateStructureData(
      ServiceContext serviceContext,
      Object operationContext,
      NodeId nodeId,
      UaNode node,
      DataValue[] updateValues,
      PerformUpdateType performUpdateType,
      StatusCode[] operationResults,
      DiagnosticInfo[] operationDiagnostics
   ) throws StatusException {
      throw new StatusException(StatusCodes.Bad_HistoryOperationUnsupported);
   }
}
